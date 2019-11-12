package io.sherpair.w4s.auth.repository.doobie

import cats.effect.Sync
import doobie.syntax.connectionio._
import doobie.syntax.stream._
import doobie.util.transactor.Transactor
import fs2.Stream
import io.sherpair.w4s.auth.config.AuthConfig
import io.sherpair.w4s.auth.domain.{SignupRequest, UpdateRequest, User}
import io.sherpair.w4s.auth.repository.RepositoryUserOps
import tsec.passwordhashers.PasswordHash

private[doobie] class DoobieRepositoryUserOps[F[_]](
    tx: Transactor[F])(implicit C: AuthConfig, S: Sync[F]
) extends RepositoryUserOps[F] {

  val userSql = new UserSql
  import userSql._

  /* Test-only */
  override def count: F[Int] = countSql.unique.transact(tx)

  override def delete(fieldId: String, fieldVal: String): F[Int] =
    deleteSql(fieldId, fieldVal).run.map[Int](identity).transact(tx)

  override def delete(id: Long): F[Int] = deleteSql(id).run.map[Int](identity).transact(tx)

  override def disable(id: Long): F[Int] = disableSql(id).run.map[Int](identity).transact(tx)

  /* Test-only */
  override def empty: F[Int] = emptySql.run.map[Int](identity).transact(tx)

  override def enable(id: Long): F[Int] = enableSql(id).run.map[Int](identity).transact(tx)

  override def find(id: Long): F[Option[User]] = findSql(id).option.transact(tx)

  override def find(fieldId: String, fieldVal: String): F[Option[User]] =
    findSql(fieldId, fieldVal).option.transact(tx)

  override def findForSignin(fieldId: String, fieldVal: String): F[Option[(User, String)]] =
    findForSigninSql(fieldId, fieldVal).option.transact(tx)

  override def insert[A](signupRequest: SignupRequest, secret: PasswordHash[A]): F[User] =
    insertSql(signupRequest, secret).transact(tx)

  override def list: Stream[F, User] = listSql.stream.transact(tx)

  override def subset(order: String, limit: Long, offset: Long): Stream[F, User] =
    subsetSql(order, offset, limit).stream.transact(tx)

  override def update(id: Long, updateRequest: UpdateRequest): F[Int] =
    updateSql(id, updateRequest).transact(tx)
}

object DoobieRepositoryUserOps {

  def apply[F[_]: Sync](tx: Transactor[F])(implicit C: AuthConfig): F[RepositoryUserOps[F]] =
    Sync[F].delay(new DoobieRepositoryUserOps[F](tx))
}
