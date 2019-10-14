package io.sherpair.w4s.auth.repository.doobie

import java.time.Instant

import cats.effect.Sync
import doobie.{FC, Query0, Update0}
import doobie.free.connection.ConnectionIO
import doobie.postgres.sqlstate.class23.UNIQUE_VIOLATION
import doobie.syntax.applicativeerror._
import doobie.syntax.string._
import doobie.util.fragment.Fragment.{const => fr}
import io.sherpair.w4s.auth.config.AuthConfig
import io.sherpair.w4s.auth.domain.User
import io.sherpair.w4s.domain.W4sError

private[doobie] class UserSql[F[_]: Sync](implicit C: AuthConfig) extends DoobieSql[Long, User] {

  override def deleteSql(id: Long): Update0 = sql"""DELETE FROM users WHERE id = $id""".update

  override def deleteSql(fieldId: String, fieldVal: String): Update0 =
    (fr"DELETE FROM users where " ++ fr(fieldId) ++ fr"= $fieldVal").update

  override val emptySql: Update0 = sql"""TRUNCATE TABLE users""".update

  override def findSql(id: Long): Query0[User] = sql"""
      SELECT id, account_id, first_name, last_name, email, password, geoId, country, created_at
      FROM users
      WHERE id = $id
    """.query

  override def insertSql(user: User): ConnectionIO[User] = {
    val stmt = sql"""
      INSERT INTO users (account_id, first_name, last_name, email, password, geoId, country)
      VALUES (
        ${user.accountId}, ${user.firstName}, ${user.lastName},
        ${user.email}, crypt(${user.password}, gen_salt('bf', 8)), ${user.geoId}, ${user.country}
      )
    """
    tryUpsert[(Long, Instant), User](
      stmt.update.withUniqueGeneratedKeys[(Long, Instant)]("id", "created_at"),
      user, "insert",
      values => user.copy(id = values._1, createdAt = values._2)
    )
  }

  override val listSql: Query0[User] = sql"""
      SELECT id, account_id, first_name, last_name, email, password, geoId, country, created_at
      FROM users
    """.query

  override def loginSql(fieldId: String, fieldVal: String, password: String): Query0[User] =
    (fr"SELECT id, account_id, first_name, last_name, email, password, geoId, country, created_at" ++
     fr"FROM users WHERE" ++
     fr(fieldId) ++ fr"= $fieldVal" ++
     fr"AND password = crypt(${password}, password)"
    )
    .query[User]

  override def subsetSql(order: String, limit: Int, offset: Int): Query0[User] = sql"""
      SELECT id, account_id, first_name, last_name, email, password, geoId, country, created_at
      FROM users
      ORDER BY $order LIMIT $limit OFFSET $offset
    """.query

  def updateSql(user: User): ConnectionIO[Int] = {
    val stmt = sql"""
      UPDATE users SET
        account_id = ${user.accountId},
        first_name = ${user.firstName},
        last_name = ${user.lastName},
        email = ${user.email},
        password = crypt(${user.password}, gen_salt('bf', 8)),
        geoId = ${user.geoId},
        country = ${user.country}
      WHERE id = ${user.id}
    """
    tryUpsert[Int, Int](stmt.update.run, user, "update", identity)
  }

  private def error(method: String, user: User): W4sError =
    W4sError(s"(${method}) accountId(${user.accountId}) and/or email(${user.email}) already exist")

  private def tryUpsert[A, B](cIO: ConnectionIO[A], user: User, method: String, f: A => B): ConnectionIO[B] =
    cIO.attemptSomeSqlState {
      case UNIQUE_VIOLATION => error(method, user)
    }
    .flatMap {
      case Left(error) => FC.raiseError[B](error)
      case Right(result) => FC.pure(f(result))
    }
}
