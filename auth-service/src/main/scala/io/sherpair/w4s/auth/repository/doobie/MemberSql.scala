package io.sherpair.w4s.auth.repository.doobie

import java.time.Instant

import cats.effect.Sync
import cats.syntax.option._
import doobie.{FC, Query0, Update0}
import doobie.free.connection.ConnectionIO
import doobie.postgres.implicits.pgEnumStringOpt
import doobie.postgres.sqlstate.class23.UNIQUE_VIOLATION
import doobie.syntax.applicativeerror._
import doobie.syntax.string._
import doobie.util.Meta
import doobie.util.fragment.Fragment.const
import io.sherpair.w4s.auth.domain.{Crypt, Member, SignupRequest, UniqueViolation, UpdateRequest}
import io.sherpair.w4s.domain.Role
import tsec.passwordhashers.PasswordHash

private[doobie] class MemberSql[F[_]: Sync] {

  implicit val hashMeta: Meta[PasswordHash[Crypt]] = PasswordHash.subst[Crypt](implicitly[Meta[String]])
  implicit val roleMeta: Meta[Role] = pgEnumStringOpt("role", Role.withNameOption, _.entryName)

  val member = IndexedSeq(
    "id", "account_id", "first_name", "last_name", "email", "geo_id", "country", "active", "role", "created_at"
  )

  val fields = fr"id, account_id, first_name, last_name, email, geo_id, country, active, role, created_at"

  /* Test-only */
  val countSql: Query0[Long] = sql"""SELECT COUNT(*) FROM members""".query

  def deleteSql(id: Long): Update0 = sql"""DELETE FROM members WHERE id = $id""".update

  def deleteSql(fieldId: String, fieldVal: String): Update0 =
    (fr"DELETE FROM members where " ++ const(fieldId) ++ fr"= $fieldVal").update

  def disableSql(id: Long): Update0 =
    sql"""UPDATE members SET active = FALSE WHERE id = ${id}""".update

  /* Test-only */
  val emptySql: Update0 = sql"""TRUNCATE TABLE members CASCADE""".update

  def enableSql(id: Long): Update0 =
    sql"""UPDATE members SET active = TRUE WHERE id = ${id}""".update

    def findSql(id: Long): Query0[Member] =
    (fr"SELECT" ++ fields ++ fr"FROM members" ++ fr"WHERE id = $id").query[Member]

  def findSql(fieldId: String, fieldVal: String): Query0[Member] =
    (fr"SELECT" ++ fields ++ fr"FROM members WHERE" ++ const(fieldId) ++ fr"= $fieldVal").query[Member]

  def findWithSecretSql(fieldId: String, fieldVal: String): Query0[(Member, String)] =
    (fr"SELECT" ++ fields ++ fr", secret" ++ fr"FROM members WHERE" ++ const(fieldId) ++ fr"= $fieldVal")
      .query[(Member, String)]

  def insertSql(sr: SignupRequest, secret: PasswordHash[Crypt]): ConnectionIO[Member] =
    insertStmt(sr, secret)
      .withUniqueGeneratedKeys[(Long, Instant)]("id", "created_at")
      .attemptSomeSqlState {
        case UNIQUE_VIOLATION =>
          UniqueViolation(s"(insert) accountId(${sr.accountId}) and/or email(${sr.email}) already taken")
      }
      .flatMap {
        case Left(error) => FC.raiseError(error)
        case Right(result) => FC.pure(member(result._1, sr, result._2))
      }

  def insertStmt(sr: SignupRequest, secret: PasswordHash[Crypt]): Update0 =
    sql"""
      INSERT INTO members (account_id, first_name, last_name, email, geo_id, country, secret)
      VALUES (
        ${sr.accountId}, ${sr.firstName}, ${sr.lastName},
        ${sr.email}, ${sr.geoId}, ${sr.country}, ${secret}
      )
    """.update

  val listSql: Query0[Member] = (fr"SELECT" ++ fields ++ fr"FROM members").query[Member]

  def subsetSql(order: String, limit: Long, offset: Long): Query0[Member] =
    (fr"SELECT" ++ fields ++ fr"FROM members" ++ fr"ORDER BY $order LIMIT $limit OFFSET $offset").query[Member]

  def updateEmailSql(id: Long, email: String): ConnectionIO[Option[Member]] =
    updateEmailStmt(id, email)
      .withUniqueGeneratedKeys[Member](member: _*)
      .attemptSomeSqlState {
        case UNIQUE_VIOLATION => UniqueViolation(s"email(${email}) already taken")
      }
      .flatMap {
        case Left(error) => FC.raiseError(error)
        case Right(result) => FC.pure(result.some)
      }

  def updateEmailStmt(id: Long, email: String): Update0 =
    sql"""UPDATE members SET email = ${email}, active = FALSE WHERE id = ${id} AND active = TRUE"""
      .update

  def updateSecretSql(id: Long, secret: PasswordHash[Crypt]): Update0 =
    sql"""UPDATE members SET secret = ${secret} WHERE id = ${id} AND active = TRUE""".update

  def updateSql(id: Long, ur: UpdateRequest): ConnectionIO[Option[Member]] =
    updateStmt(id, ur)
    .withUniqueGeneratedKeys[Member](member: _*)
    .attemptSomeSqlState {
      case UNIQUE_VIOLATION => UniqueViolation(s"accountId(${ur.accountId}) already taken")
    }
    .flatMap {
      case Left(error) => FC.raiseError(error)
      case Right(result) => FC.pure(result.some)
    }

  def updateStmt(id: Long, ur: UpdateRequest): Update0 =
    sql"""
      UPDATE members SET
        account_id = ${ur.accountId},
        first_name = ${ur.firstName},
        last_name = ${ur.lastName},
        geo_id = ${ur.geoId},
        country = ${ur.country}
      WHERE id = ${id}
        AND active = TRUE
    """.update

  private def member(id: Long, sr: SignupRequest, created_at: Instant) =
    new Member(
      id, sr.accountId, sr.firstName, sr.lastName, sr.email, sr.geoId, sr.country, false, Role.Member, created_at
    )
}
