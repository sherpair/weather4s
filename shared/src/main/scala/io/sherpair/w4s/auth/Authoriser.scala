package io.sherpair.w4s.auth

import cats.data.Kleisli
import cats.effect.Sync
import cats.syntax.applicative._
import cats.syntax.either._
import cats.syntax.try_._
import io.circe.parser.decode
import io.sherpair.w4s.domain.{AuthData, ClaimContent}
import org.http4s.{AuthScheme, Request}
import org.http4s.Credentials.Token
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Authorization
import org.http4s.server.AuthMiddleware
import pdi.jwt.{Jwt, JwtClaim, JwtOptions}
import pdi.jwt.exceptions.JwtException

class Authoriser[F[_]: Sync](
    authData: AuthData, audience: Audience, isAuthorised: ClaimContent => Boolean
) extends Http4sDsl[F] {

  val authorise: Auth[F] = AuthMiddleware(validateRequest, onFailure)

  private def decodeToken(token: String): F[Either[String, ClaimContent]] =
    Jwt.decodeAll(token, authData.publicKey, authData.jwtAlgorithms, JwtOptions.DEFAULT)
      .map(tokenAsTuple => validateClaim(tokenAsTuple._2))
      .recover { case _: JwtException => notAuthorized }
      .liftTo[F]

  private lazy val missingToken = "Authorization token is missing".asLeft[ClaimContent].pure[F]

  private lazy val notAuthorized = "Not authorized".asLeft[ClaimContent]

  private def retrieveToken(request: Request[F]): Option[String] =
    request.headers.get(Authorization).collect {
      case Authorization(Token(AuthScheme.Bearer, token)) => token
    }

  def validateClaim(claim: JwtClaim): Either[String, ClaimContent] =
    if (claim.isValid(Claims.iss, audience)) {
      decode[ClaimContent](claim.content).fold(
        _ => notAuthorized,
        content => if (isAuthorised(content)) content.asRight[String] else notAuthorized
      )
    } else notAuthorized

  private def validateRequest: AuthResult[F] = Kleisli(validateToken(_))

  private def validateToken(request: Request[F]): F[Either[String, ClaimContent]] =
    retrieveToken(request).fold(missingToken)(decodeToken(_))
}

object Authoriser {

  def apply[F[_]: Sync](
      authData: AuthData, audience: Audience, isAuthorised: ClaimContent => Boolean = _ => true
  ): Auth[F] =
    new Authoriser[F](authData, audience, isAuthorised).authorise
}
