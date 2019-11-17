package io.sherpair.w4s.auth.app

import java.security.PrivateKey
import java.time.Instant

import scala.concurrent.duration.FiniteDuration

import cats.effect.Sync
import cats.syntax.applicative._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.option._
import io.sherpair.w4s.auth.{clock, Claims}
import io.sherpair.w4s.auth.config.{AuthConfig, MaybePostman}
import io.sherpair.w4s.auth.domain.{EmailType, Member, Token}
import io.sherpair.w4s.auth.repository.RepositoryTokenOps
import io.sherpair.w4s.domain.{unit, Logger}
import org.http4s.{AuthScheme, Credentials, Response}
import org.http4s.headers.Authorization
import pdi.jwt.{Jwt, JwtClaim, JwtTime}
import pdi.jwt.algorithms.JwtRSAAlgorithm
import tsec.common.SecureRandomId

class Authenticator[F[_]](
    jwtAlgorithm: JwtRSAAlgorithm, postman: MaybePostman, privateKey: PrivateKey)(
    implicit C: AuthConfig, L: Logger[F], R: RepositoryTokenOps[F], S: Sync[F]
) {

  def addJwtToAuthorizationHeader(responseF: F[Response[F]], member: Member): F[Response[F]] =
    for {
      response <- responseF
      jwt <- S.delay(Jwt.encode(claims(member), privateKey, jwtAlgorithm))
    }
    yield response.putHeaders(Authorization(Credentials.Token(AuthScheme.Bearer, jwt)))

  def deleteToken(token: Token): F[Unit] =
    R.delete(token.tokenId).handleErrorWith {
      L.error(_)(s"While trying to delete a token for Member(${token.memberId})")
    }

  def deleteTokenIfOlderThan(rateLimit: FiniteDuration, member: Member): F[Boolean] =
    R.deleteIfOlderThan(rateLimit, member)

  def retrieveToken(tokenId: SecureRandomId): F[Option[Token]] =
    R.find(tokenId) >>= {
      _.fold(none[Token].pure[F]) { token =>
        S.delay(if (token.expiryDate.isBefore(Instant.now)) none[Token] else token.some)
      }
    }

  def sendToken(member: Member, emailType: EmailType): F[Unit] =
    for {
      now <- S.delay(Instant.now())
      expiryDate = now.plusSeconds(C.token.duration.toSeconds)
      token <- R.insert(Token(SecureRandomId.Strong.generate, member.id, expiryDate))
      _ <- S.delay(postman.sendEmail(token, member, emailType))
    }
    yield unit

  private def claims(member: Member): JwtClaim = {
    val now = JwtTime.nowSeconds
    JwtClaim(
      member.claimContent, Claims.issO, member.id.toString.some, Claims.audO,
      (now + C.authToken.duration.toSeconds).some, now.some, now.some
    )
  }
}

object Authenticator {

  def apply[F[_]: Sync](
      jwtAlgorithm: JwtRSAAlgorithm, postman: MaybePostman, privateKey: PrivateKey)(
      implicit C: AuthConfig, L: Logger[F], R: RepositoryTokenOps[F]
  ): Authenticator[F] =
    new Authenticator[F](jwtAlgorithm, postman, privateKey)
}

