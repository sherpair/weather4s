package io.sherpair.w4s.auth.repository.doobie

import scala.concurrent.duration.FiniteDuration

import cats.effect.Sync
import cats.syntax.functor._
import doobie.syntax.connectionio._
import doobie.syntax.stream._
import doobie.util.transactor.Transactor
import fs2.Stream
import io.sherpair.w4s.auth.domain.{Member, Token}
import io.sherpair.w4s.auth.repository.RepositoryTokenOps
import tsec.common.SecureRandomId

private[doobie] class DoobieRepositoryTokenOps[F[_]: Sync](tx: Transactor[F]) extends RepositoryTokenOps[F] {

  val tokenSql = new TokenSql
  import tokenSql._

  /* Test-only */
  override def count: F[Long] = countSql.unique.transact(tx)

  override def delete(id: Long): F[Int] = deleteSql(id).run.map[Int](identity).transact(tx)

  override def delete(tokenId: SecureRandomId): F[Unit] = deleteSql(tokenId).run.void.transact(tx)

  override def deleteIfOlderThan(rateLimit: FiniteDuration, member: Member): F[Boolean] =
    deleteIfOlderThanSql(rateLimit, member).run.map[Boolean](_ > 0).transact(tx)

  /* Test-only */
  override def empty: F[Int] = emptySql.run.map[Int](identity).transact(tx)

  override def find(id: Long): F[Option[Token]] = findSql(id).option.transact(tx)

  override def find(tokenId: SecureRandomId): F[Option[Token]] = findSql(tokenId).option.transact(tx)

  override def insert(token: Token): F[Token] = insertSql(token).transact(tx)

  def list: Stream[F, Token] = listSql.stream.transact(tx)

  def subset(order: String, limit: Long, offset: Long): Stream[F, Token] =
    subsetSql(order, offset, limit).stream.transact(tx)
}

object DoobieRepositoryTokenOps {

  def apply[F[_]: Sync](tx: Transactor[F]): F[RepositoryTokenOps[F]] =
    Sync[F].delay(new DoobieRepositoryTokenOps[F](tx))
}


