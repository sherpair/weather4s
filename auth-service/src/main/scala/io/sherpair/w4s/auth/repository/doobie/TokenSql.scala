package io.sherpair.w4s.auth.repository.doobie

import java.time.Instant

import scala.concurrent.duration.FiniteDuration

import cats.effect.Sync
import doobie.{FC, Query0, Update0}
import doobie.free.connection.ConnectionIO
import doobie.postgres.sqlstate.class23.UNIQUE_VIOLATION
import doobie.syntax.applicativeerror._
import doobie.syntax.string._
import doobie.util.Meta
import io.sherpair.w4s.auth.config.AuthConfig
import io.sherpair.w4s.auth.domain.{Token, User}
import io.sherpair.w4s.domain.W4sError
import tsec.common.SecureRandomId

private[doobie] class TokenSql[F[_]: Sync](implicit C: AuthConfig) {

  implicit val sridMeta: Meta[SecureRandomId] = Meta[String].timap(_.asInstanceOf[SecureRandomId])(_.toString)

  /* Test-only */
  val countSql: Query0[Int] = sql"""SELECT COUNT(*) FROM tokens""".query

  def deleteIfOlderThanSql(rateLimit: FiniteDuration, user: User): Update0 =
    sql"""
      | DELETE FROM tokens
      | WHERE user_id = ${user.id}
      | AND created_at < ${Instant.now.minusMillis(rateLimit.toMillis)}""".update

  def deleteSql(id: Long): Update0 = sql"""DELETE FROM tokens WHERE id = $id""".update

  def deleteSql(tokenId: SecureRandomId): Update0 = sql"""DELETE FROM tokens WHERE token_id = $tokenId""".update

  /* Test-only */
  val emptySql: Update0 = sql"""TRUNCATE TABLE tokens""".update

  def error(method: String, token: Token): W4sError =
    W4sError(s"(${method}) token(${token.id}) already exists (User is ${token.userId})")

  def findSql(id: Long): Query0[Token] = sql"""SELECT * tokens WHERE id = $id""".query[Token]

  def findSql(tokenId: SecureRandomId): Query0[Token] =
    sql"""SELECT * FROM tokens WHERE token_id = $tokenId""".query[Token]

  def insertSql(token: Token): ConnectionIO[Token] =
    insertStmt(token)
      .attemptSomeSqlState {
        case UNIQUE_VIOLATION => error("insert", token)
      }
      .flatMap {
        case Left(error) => FC.raiseError(error)
        case Right(result) => FC.pure(token.copy(id = result._1, createdAt = result._2))
      }

  def insertStmt(token: Token): ConnectionIO[(Long, Instant)] =
    sql"""
      | INSERT INTO tokens (token_id, user_id, expiry_date)
      | VALUES (${token.tokenId}, ${token.userId}, ${token.expiryDate})"""
    .update.withUniqueGeneratedKeys[(Long, Instant)]("id", "created_at")

  val listSql: Query0[Token] = sql"""SELECT * FROM tokens""".query[Token]

  def subsetSql(order: String, limit: Long, offset: Long): Query0[Token] =
  sql"""SELECT * FROM tokens ORDER BY $order LIMIT $limit OFFSET $offset""".query[Token]
}
