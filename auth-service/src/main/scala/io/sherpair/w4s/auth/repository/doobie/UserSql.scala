package io.sherpair.w4s.auth.repository.doobie

import java.time.Instant

import cats.effect.Sync
import doobie.{FC, Query0, Update0}
import doobie.free.connection.ConnectionIO
import doobie.postgres.implicits.pgEnumStringOpt
import doobie.postgres.sqlstate.class23.UNIQUE_VIOLATION
import doobie.syntax.applicativeerror._
import doobie.syntax.string._
import doobie.util.Meta
import doobie.util.fragment.Fragment.const
import io.sherpair.w4s.auth.config.AuthConfig
import io.sherpair.w4s.auth.domain.{SignupRequest, UpdateRequest, User}
import io.sherpair.w4s.domain.{Role, W4sError}
import io.sherpair.w4s.domain.Role.Member
import tsec.passwordhashers.PasswordHash

private[doobie] class UserSql[F[_]: Sync](implicit C: AuthConfig) {

  implicit val roleMeta: Meta[Role] = pgEnumStringOpt("role", Role.withNameOption, _.entryName)

  val fields = fr"account_id, first_name, last_name, email, geoId, country, active, role, created_at"

  def changeSecretSql[A](user: User, secret: PasswordHash[A]): ConnectionIO[Int] =
    sql"""UPDATE users SET secret = ${secret.toString} WHERE id = ${user.id} AND active = TRUE""".update.run

  /* Test-only */
  val countSql: Query0[Int] = sql"""SELECT COUNT(*) FROM users""".query

  def deleteSql(id: Long): Update0 = sql"""DELETE FROM users WHERE id = $id""".update

  def deleteSql(fieldId: String, fieldVal: String): Update0 =
    (fr"DELETE FROM users where " ++ const(fieldId) ++ fr"= $fieldVal").update

  def disableSql(id: Long): Update0 =
    sql"""UPDATE users SET active = FALSE WHERE id = ${id}""".update

  /* Test-only */
  val emptySql: Update0 = sql"""TRUNCATE TABLE users CASCADE""".update

  def enableSql(id: Long): Update0 =
    sql"""UPDATE users SET active = TRUE WHERE id = ${id}""".update

  def error(method: String, accountId: String, email: String): W4sError =
    W4sError(s"(${method}) accountId(${accountId}) and/or email(${email}) already exist")

    def findSql(id: Long): Query0[User] =
    (fr"SELECT" ++ fields ++ fr"FROM users" ++ fr"WHERE id = $id").query[User]

  def findSql(fieldId: String, fieldVal: String): Query0[User] =
    (fr"SELECT" ++ fields ++ fr"FROM users WHERE" ++ const(fieldId) ++ fr"= $fieldVal").query[User]

  def findForSigninSql(fieldId: String, fieldVal: String): Query0[(User, String)] =
    (fr"SELECT" ++ fields ++ fr", secret" ++ fr"FROM users WHERE" ++ const(fieldId) ++ fr"= $fieldVal")
      .query[(User, String)]

  def insertSql[A](sr: SignupRequest, secret: PasswordHash[A]): ConnectionIO[User] =
    insertStmt(sr, secret)
      .attemptSomeSqlState {
        case UNIQUE_VIOLATION => error("insert", sr.accountId, sr.email)
      }
      .flatMap {
        case Left(error) => FC.raiseError(error)
        case Right(result) => FC.pure(user(result._1, sr, result._2))
      }

  def insertStmt[A](sr: SignupRequest, secret: PasswordHash[A]): ConnectionIO[(Long, Instant)] =
    sql"""
      INSERT INTO users (account_id, first_name, last_name, email, geoId, country, secret)
      VALUES (
        ${sr.accountId}, ${sr.firstName}, ${sr.lastName},
        ${sr.email}, ${sr.geoId}, ${sr.country}, ${sr.secret}
      )
    """.update.withUniqueGeneratedKeys[(Long, Instant)]("id", "created_at")

  val listSql: Query0[User] = (fr"SELECT" ++ fields ++ fr"FROM users").query[User]

  def subsetSql(order: String, limit: Long, offset: Long): Query0[User] =
    (fr"SELECT" ++ fields ++ fr"FROM users" ++ fr"ORDER BY $order LIMIT $limit OFFSET $offset").query[User]

  def updateSql(id: Long, ur: UpdateRequest): ConnectionIO[Int] =
    updateStmt(id, ur)
      .attemptSomeSqlState {
        case UNIQUE_VIOLATION => error("update", ur.accountId, ur.email)
      }
      .flatMap {
        case Left(error) => FC.raiseError(error)
        case Right(result) => FC.pure(result)
      }

  def updateStmt(id: Long, ur: UpdateRequest): ConnectionIO[Int] =
    sql"""
      UPDATE users SET
        account_id = ${ur.accountId},
        first_name = ${ur.firstName},
        last_name = ${ur.lastName},
        email = ${ur.email},
        geoId = ${ur.geoId},
        country = ${ur.country},
      WHERE id = ${id}
        AND active = TRUE
    """.update.run

  private def user(id: Long, sr: SignupRequest, created_at: Instant) =
    new User(
      id, sr.accountId, sr.firstName, sr.lastName, sr.email, sr.geoId, sr.country, false, Member, created_at
    )
}
