package io.sherpair.w4s.auth.repository.doobie

import cats.effect.Sync
import cats.syntax.applicative._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import cats.syntax.option._
import doobie.syntax.connectionio._
import doobie.syntax.stream._
import doobie.util.invariant.UnexpectedEnd
import doobie.util.transactor.Transactor
import fs2.Stream
import io.sherpair.w4s.auth.domain.{Crypt, Member, SignupRequest, UpdateRequest}
import io.sherpair.w4s.auth.repository.RepositoryMemberOps

private[doobie] class DoobieRepositoryMemberOps[F[_]](
    tx: Transactor[F])(implicit S: Sync[F]
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

  override def findWithSecret(fieldId: String, fieldVal: String): F[Option[(Member, String)]] =
    findWithSecretSql(fieldId, fieldVal).option.transact(tx)

  override def insert(signupRequest: SignupRequest): F[Member] =
    Crypt.hashpw(signupRequest.secret) >>= {
      insertSql(signupRequest, _).transact(tx)
    }

  override def list: Stream[F, Member] = listSql.stream.transact(tx)

  override def subset(order: String, limit: Long, offset: Long): Stream[F, Member] =
    subsetSql(order, offset, limit).stream.transact(tx)

  override def update(id: Long, email: String): F[Option[Member]] =
    updateEmailSql(id, email)
      .transact(tx)
      .recoverWith {
        case UnexpectedEnd => none[Member].pure[F]
      }

  override def update(id: Long, secret: Array[Byte]): F[Int] =
    Crypt.hashpw(secret) >>= {
      updateSecretSql(id, _).run
        .transact(tx)
        .recoverWith {
          case UnexpectedEnd => 0.pure[F]
        }
    }

  override def update(id: Long, updateRequest: UpdateRequest): F[Option[Member]] =
    updateSql(id, updateRequest)
      .transact(tx)
      .recoverWith {
        case UnexpectedEnd => none[Member].pure[F]
      }
}

object DoobieRepositoryMemberOps {

  def apply[F[_]: Sync](tx: Transactor[F]): F[RepositoryMemberOps[F]] =
    Sync[F].delay(new DoobieRepositoryMemberOps[F](tx))
}
