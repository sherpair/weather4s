package io.sherpair.w4s.auth.repository.doobie

import java.time.Instant

import scala.concurrent.duration.FiniteDuration

import cats.effect.Sync
import doobie.{FC, Query0, Update0}
import doobie.free.connection.ConnectionIO
import doobie.implicits.legacy.instant._
import doobie.postgres.sqlstate.class23.UNIQUE_VIOLATION
import doobie.syntax.applicativeerror._
import doobie.syntax.string._
import doobie.util.meta.Meta
import io.sherpair.w4s.auth.domain.{Member, Token, UniqueViolation}
import tsec.common.SecureRandomId

private[doobie] class TokenSql[F[_]: Sync] {

  implicit val sridMeta: Meta[SecureRandomId] =
    Meta[String].timap(_.asInstanceOf[SecureRandomId])(_.toString)

  /* Test-only */
  val countSql: Query0[Long] = sql"""SELECT COUNT(*) FROM tokens""".query

  def deleteIfOlderThanSql(rateLimit: FiniteDuration, member: Member): Update0 =
    sql"""
      DELETE FROM tokens
      WHERE member_id = ${member.id}
      AND created_at < ${Instant.now.minusMillis(rateLimit.toMillis)}
    """.update

  def deleteSql(id: Long): Update0 = sql"""DELETE FROM tokens WHERE id = $id""".update

  def deleteSql(tokenId: SecureRandomId): Update0 =
    sql"""DELETE FROM tokens WHERE token_id = $tokenId""".update

  /* Test-only */
  val emptySql: Update0 = sql"""TRUNCATE TABLE tokens""".update

  def error(token: Token): UniqueViolation =
    UniqueViolation(s"token(${token.id}) already used for Member(${token.memberId})")

  def findSql(id: Long): Query0[Token] = sql"""SELECT * FROM tokens WHERE id = $id""".query[Token]

  def findSql(tokenId: SecureRandomId): Query0[Token] =
    sql"""SELECT * FROM tokens WHERE token_id = $tokenId""".query[Token]

  def insertSql(token: Token): ConnectionIO[Token] =
    insertStmt(token)
      .withUniqueGeneratedKeys[(Long, Instant)]("id", "created_at")
      .attemptSomeSqlState {
        case UNIQUE_VIOLATION => error(token)
      }
      .flatMap {
        case Left(error) => FC.raiseError(error)
        case Right(result) => FC.pure(token.copy(id = result._1, createdAt = result._2))
      }

  def insertStmt(token: Token): Update0 =
    sql"""
      INSERT INTO tokens (token_id, member_id, expiry_date)
      VALUES (${token.tokenId}, ${token.memberId}, ${token.expiryDate})
    """
    .update

  val listSql: Query0[Token] = sql"""SELECT * FROM tokens""".query[Token]

  def subsetSql(order: String, limit: Long, offset: Long): Query0[Token] =
  sql"""SELECT * FROM tokens ORDER BY $order LIMIT $limit OFFSET $offset""".query[Token]
}
