package io.sherpair.w4s.auth.repository.doobie

import cats.effect.Sync
import doobie.syntax.connectionio._
import doobie.syntax.stream._
import doobie.util.transactor.Transactor
import fs2.Stream
import io.sherpair.w4s.auth.config.AuthConfig
import io.sherpair.w4s.auth.domain.User
import io.sherpair.w4s.auth.repository.RepositoryUserOps

private[doobie] class DoobieRepositoryUserOps[F[_]](
    tx: Transactor[F])(implicit C: AuthConfig, S: Sync[F]
) extends RepositoryUserOps[F] {

  val doobieSql = new UserSql
  import doobieSql._

  /* Test-only */
  override def count: F[Int] = countSql.unique.transact(tx)

  override def delete(id: Long): F[Int] =
    deleteSql(id).run.map[Int](identity).transact(tx)

  override def delete(fieldId: String, fieldVal: String): F[Int] =
    deleteSql(fieldId, fieldVal).run.map[Int](identity).transact(tx)

  override def empty: F[Int] =
    emptySql.run.map[Int](identity).transact(tx)

  override def find(id: Long): F[Option[User]] =
    findSql(id).option.transact(tx)

  override def insert(record: User): F[User] =
    insertSql(record).transact(tx)

  override def list: Stream[F, User] = listSql.stream.transact(tx)

  override def login(fieldId: String, fieldVal: String, password: String): F[Option[User]] =
    loginSql(fieldId, fieldVal, password).option.transact(tx)

  override def subset(order: String, limit: Long, offset: Long): Stream[F, User] =
    subsetSql(order, offset, limit).stream.transact(tx)

  override def update(record: User): F[Int] =
    updateSql(record).transact(tx)
}

object DoobieRepositoryUserOps {

  def apply[F[_]: Sync](tx: Transactor[F])(implicit C: AuthConfig): F[RepositoryUserOps[F]] =
    Sync[F].delay(new DoobieRepositoryUserOps[F](tx))
}
