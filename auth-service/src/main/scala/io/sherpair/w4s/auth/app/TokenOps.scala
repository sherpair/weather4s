package io.sherpair.w4s.auth.app

import java.time.Instant

import scala.concurrent.duration.FiniteDuration

import cats.effect.Sync
import cats.syntax.applicative._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.option._
import io.sherpair.w4s.auth.config.{AuthConfig, MaybePostman}
import io.sherpair.w4s.auth.domain.{EmailType, Member, Token}
import io.sherpair.w4s.auth.repository.RepositoryTokenOps
import io.sherpair.w4s.domain.{unit, Logger}
import tsec.common.SecureRandomId

class TokenOps[F[_]](
    postman: MaybePostman)(implicit C: AuthConfig, L: Logger[F], R: RepositoryTokenOps[F], S: Sync[F]
) {

  def delete(token: Token): F[Unit] =
    R.delete(token.tokenId).handleErrorWith {
      L.error(_)(s"While trying to delete a token for Member(${token.memberId})")
    }

  def deleteIfOlderThan(rateLimit: FiniteDuration, member: Member): F[Boolean] =
    R.deleteIfOlderThan(rateLimit, member)

  def retrieve(tokenId: SecureRandomId): F[Option[Token]] =
    R.find(tokenId) >>= {
      _.fold(none[Token].pure[F]) { token =>
        S.delay(if (token.expiryDate.isBefore(Instant.now)) none[Token] else token.some)
      }
    }

  def send(member: Member, emailType: EmailType): F[Unit] =
    for {
      now <- S.delay(Instant.now())
      expiryDate = now.plusSeconds(C.token.duration.toSeconds)
      token <- R.insert(Token(SecureRandomId.Strong.generate, member.id, expiryDate))
      _ <- S.delay(postman.sendEmail(token, member, emailType))
    }
    yield unit
}

object TokenOps {

  def apply[F[_]: Sync](
      postman: MaybePostman)(implicit C: AuthConfig, L: Logger[F], R: RepositoryTokenOps[F]
  ): TokenOps[F] =
    new TokenOps[F](postman)
}
