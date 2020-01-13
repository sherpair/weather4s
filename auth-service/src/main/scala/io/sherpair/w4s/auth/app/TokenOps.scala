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
import io.sherpair.w4s.auth.domain.{EmailType, Kind, Member, Token}
import io.sherpair.w4s.auth.repository.RepositoryTokenOps
import io.sherpair.w4s.domain.{unit, Logger}
import org.http4s.Response
import tsec.common.SecureRandomId

class TokenOps[F[_]](
    auth: Authenticator[F], postman: MaybePostman)(
    implicit C: AuthConfig, L: Logger[F], R: RepositoryTokenOps[F], S: Sync[F]
) {

  def addTokensToResponse(member: Member, responseF: F[Response[F]]): F[Response[F]] =
    create(member, Kind.Refresh, C.token.refreshLife) >>= {
      auth.addTokensToResponse(responseF, member, _)
    }

  def delete(token: Token): F[Unit] =
    R.delete(token.tokenId).handleErrorWith {
      L.error(_)(s"While trying to delete a token for Member(${token.memberId})")
    }

  def deleteIfOlderThan(rateLimit: FiniteDuration, member: Member, kind: Kind): F[Boolean] =
    R.deleteIfOlderThan(rateLimit, member, kind)

  def retrieve(tokenId: SecureRandomId, kind: Kind): F[Option[Token]] =
    R.find(tokenId, kind) >>= {
      _.fold(none[Token].pure[F]) { token =>
        S.delay(if (token.expiryDate.isBefore(Instant.now)) none[Token] else token.some)
      }
    }

  def send(member: Member, kind: Kind, emailType: EmailType): F[Unit] =
    for {
      token <- create(member, kind, C.token.activationLife)
      _ <- S.delay(postman.sendEmail(token, member, emailType))
    }
    yield unit

  private def create(member: Member, kind: Kind, duration: FiniteDuration): F[Token] =
    for {
      now <- S.delay(Instant.now())
      expiryDate = now.plusSeconds(duration.toSeconds)
      token <- R.insert(Token(SecureRandomId.Strong.generate, member.id, kind, expiryDate))
    }
    yield token
}

object TokenOps {

  def apply[F[_]: Sync](
      auth: Authenticator[F], postman: MaybePostman)(implicit C: AuthConfig, L: Logger[F], R: RepositoryTokenOps[F]
  ): TokenOps[F] =
    new TokenOps[F](auth, postman)
}
