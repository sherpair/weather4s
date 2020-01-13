package io.sherpair.w4s.auth.app

import java.security.PrivateKey

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.option._
import io.sherpair.w4s.auth.{clock, Claims}
import io.sherpair.w4s.auth.config.AuthConfig
import io.sherpair.w4s.auth.domain.{Member, Token}
import org.http4s.{AuthScheme, Credentials, HttpDate, Response, ResponseCookie}
import org.http4s.headers.Authorization
import pdi.jwt.{Jwt, JwtClaim, JwtTime}
import pdi.jwt.algorithms.JwtRSAAlgorithm

class Authenticator[F[_]](jwtAlgorithm: JwtRSAAlgorithm, privateKey: PrivateKey)(implicit C: AuthConfig, S: Sync[F]) {

  def addTokensToResponse(responseF: F[Response[F]], member: Member, refreshToken: Token): F[Response[F]] =
    for {
      response <- responseF
      jwt <- S.delay(Jwt.encode(claims(member), privateKey, jwtAlgorithm))
      cookie <- setCookieWithRefreshToken(refreshToken)
    }
    yield response
      .putHeaders(Authorization(Credentials.Token(AuthScheme.Bearer, jwt)))
      .addCookie(cookie)

  private def claims(member: Member): JwtClaim = {
    val now = JwtTime.nowSeconds
    JwtClaim(
      member.claimContent, Claims.issO, member.id.toString.some, Claims.audO,
      (now + C.authToken.duration.toSeconds).some, now.some, now.some
    )
  }

  private def setCookieWithRefreshToken(refreshToken: Token): F[ResponseCookie] =
    S.delay(ResponseCookie(
      "refresh-token", refreshToken.tokenId,
      expires = Some(HttpDate.unsafeFromInstant(refreshToken.expiryDate)),
      httpOnly = true, secure = true
    ))
}

object Authenticator {

  def apply[F[_]: Sync](
      jwtAlgorithm: JwtRSAAlgorithm, privateKey: PrivateKey)(implicit C: AuthConfig
  ): Authenticator[F] =
    new Authenticator[F](jwtAlgorithm, privateKey)
}

