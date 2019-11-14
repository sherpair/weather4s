package io.sherpair.w4s.auth.repository.doobie

import cats.effect.Sync
import doobie.syntax.connectionio._
import doobie.syntax.stream._
import doobie.util.transactor.Transactor
import fs2.Stream
import io.sherpair.w4s.auth.config.AuthConfig
import io.sherpair.w4s.auth.domain.{Member, SignupRequest, UpdateRequest}
import io.sherpair.w4s.auth.repository.RepositoryMemberOps
import tsec.passwordhashers.PasswordHash

private[doobie] class DoobieRepositoryMemberOps[F[_]](
    tx: Transactor[F])(implicit C: AuthConfig, S: Sync[F]
) extends RepositoryMemberOps[F] {

  val memberSql = new MemberSql
  import memberSql._

  /* Test-only */
  override def count: F[Long] = countSql.unique.transact(tx)

  override def delete(fieldId: String, fieldVal: String): F[Int] =
    deleteSql(fieldId, fieldVal).run.map[Int](identity).transact(tx)

  override def delete(id: Long): F[Int] = deleteSql(id).run.map[Int](identity).transact(tx)

  override def disable(id: Long): F[Int] = disableSql(id).run.map[Int](identity).transact(tx)

  /* Test-only */
  override def empty: F[Int] = emptySql.run.map[Int](identity).transact(tx)

  override def enable(id: Long): F[Int] = enableSql(id).run.map[Int](identity).transact(tx)

  override def find(id: Long): F[Option[Member]] = findSql(id).option.transact(tx)

  override def find(fieldId: String, fieldVal: String): F[Option[Member]] =
    findSql(fieldId, fieldVal).option.transact(tx)

  override def findForSignin(fieldId: String, fieldVal: String): F[Option[(Member, String)]] =
    findForSigninSql(fieldId, fieldVal).option.transact(tx)

  override def insert[A](signupRequest: SignupRequest, secret: PasswordHash[A]): F[Member] =
    insertSql(signupRequest, secret).transact(tx)

  override def list: Stream[F, Member] = listSql.stream.transact(tx)

  override def subset(order: String, limit: Long, offset: Long): Stream[F, Member] =
    subsetSql(order, offset, limit).stream.transact(tx)

  override def update(id: Long, updateRequest: UpdateRequest): F[Int] =
    updateSql(id, updateRequest).transact(tx)
}

object DoobieRepositoryMemberOps {

  def apply[F[_]: Sync](tx: Transactor[F])(implicit C: AuthConfig): F[RepositoryMemberOps[F]] =
    Sync[F].delay(new DoobieRepositoryMemberOps[F](tx))
}
