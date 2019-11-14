package io.sherpair.w4s.auth.repository

import scala.concurrent.duration.FiniteDuration

import io.sherpair.w4s.auth.domain.{Member, Token}
import tsec.common.SecureRandomId

trait RepositoryTokenOps[F[_]] extends RepositoryOps[F, Long, Token] {

  def delete(tokenId: SecureRandomId): F[Unit]

  def deleteIfOlderThan(rateLimit: FiniteDuration, member: Member): F[Boolean]

  def find(tokenId: SecureRandomId): F[Option[Token]]

  def insert(token: Token): F[Token]
}
