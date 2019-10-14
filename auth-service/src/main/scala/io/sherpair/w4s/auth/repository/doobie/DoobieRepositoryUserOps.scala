package io.sherpair.w4s.auth.repository.doobie

import cats.effect.Sync
import doobie.syntax.connectionio._
import doobie.util.transactor.Transactor
import io.sherpair.w4s.auth.config.AuthConfig
import io.sherpair.w4s.auth.domain.User
import io.sherpair.w4s.auth.repository.{
  RepositoryUserOps,
  Result, ResultList, ResultOption, ResultRecord, ResultStream
}

private[doobie] class DoobieRepositoryUserOps[F[_]](
    tx: Transactor[F])(implicit C: AuthConfig, S: Sync[F]
) extends RepositoryUserOps[F] {

  val doobieSql = new UserSql
  import doobieSql._

  override def delete(id: Long): Result[F, Int] =
    S.delay(deleteSql(id).run.map[Int](identity))

  override def delete(fieldId: String, fieldVal: String): F[Int] =
    deleteSql(fieldId, fieldVal).run.map[Int](identity).transact(tx)

  override def deleteX(id: Long): F[Int] =
    deleteSql(id).run.map[Int](identity).transact(tx)

  override def empty: Result[F, Int] =
    S.delay(emptySql.run.map[Int](identity))

  override def emptyX: F[Int] =
    emptySql.run.map[Int](identity).transact(tx)

  override def find(id: Long): ResultOption[F, Long, User] =
    S.delay(findSql(id).option)

  override def findX(id: Long): F[Option[User]] =
    findSql(id).option.transact(tx)

  override def insert(record: User): ResultRecord[F, Long, User] =
    S.delay(insertSql(record))

  override def insertX(record: User): F[User] =
    insertSql(record).transact(tx)

  override def list: ResultStream[Long, User] = listSql.stream

  override def login(fieldId: String, fieldVal: String, password: String): F[Option[User]] =
    loginSql(fieldId, fieldVal, password).option.transact(tx)

  override def subset(order: String, limit: Int, offset: Int): ResultList[F, Long, User] =
    S.delay(subsetSql(order, offset, limit).stream.compile.toList)

  override def subsetX(order: String, limit: Int, offset: Int): F[List[User]] =
    subsetSql(order, offset, limit).stream.compile.toList.transact(tx)

  override def update(record: User): Result[F, Int] =
    S.delay(updateSql(record))

  override def updateX(record: User): F[Int] =
    updateSql(record).transact(tx)
}

object DoobieRepositoryUserOps {

  def apply[F[_]: Sync](tx: Transactor[F])(implicit C: AuthConfig): F[RepositoryUserOps[F]] =
    Sync[F].delay(new DoobieRepositoryUserOps[F](tx))
}
