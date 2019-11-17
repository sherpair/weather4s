package io.sherpair.w4s.auth

import cats.data.Kleisli
import cats.effect.Sync
import cats.syntax.applicative._
import cats.syntax.either._
import cats.syntax.try_._
import io.circe.parser.decode
import io.sherpair.w4s.domain.{ClaimContent, DataForAuthorisation}
import org.http4s.{AuthScheme, Message}
import org.http4s.Credentials.Token
import org.http4s.headers.Authorization
import org.http4s.server.AuthMiddleware
import pdi.jwt.{Jwt, JwtCirce, JwtClaim, JwtOptions}
import pdi.jwt.exceptions.JwtException

abstract class MessageValidator[F[_]: Sync](
    dfa: DataForAuthorisation, audience: Audience, isAuthorised: ClaimContent => Boolean
) {

   private def decodeToken(token: String): F[Either[String, ClaimContent]] = {
     JwtCirce.decodeAll(token, dfa.publicKey, dfa.jwtAlgorithms, JwtOptions.DEFAULT)
     // Jwt.decodeAll(token, dfa.publicKey, dfa.jwtAlgorithms, JwtOptions.DEFAULT)
       .map(tokenAsTuple => validateClaim(tokenAsTuple._2))
       .recover { case _: JwtException => notAuthorized }
       .liftTo[F]
   }

  private lazy val missingToken = "Authorization token is missing".asLeft[ClaimContent].pure[F]

  private lazy val notAuthorized = "Not authorized".asLeft[ClaimContent]

  private def retrieveToken(message: Message[F]): Option[String] =
    message.headers.get(Authorization).collect {
      case Authorization(Token(AuthScheme.Bearer, token)) => token
    }

  private def validateClaim(claim: JwtClaim): Either[String, ClaimContent] =
    if (claim.isValid(Claims.iss, audience)) {
      decode[ClaimContent](claim.content).fold(
        _ => notAuthorized,
        content => if (isAuthorised(content)) content.asRight[String] else notAuthorized
      )
    } else notAuthorized

  def validateMessage(message: Message[F]): F[Either[String, ClaimContent]] =
    retrieveToken(message).fold(missingToken)(decodeToken(_))
}

class Authoriser[F[_]: Sync](
    dfa: DataForAuthorisation, audience: Audience, isAuthorised: ClaimContent => Boolean
) extends MessageValidator(dfa, audience, isAuthorised) {

  val authorise: Auth[F] = AuthMiddleware(validateRequest, onFailure)

  private def validateRequest: AuthResult[F] = Kleisli(validateMessage(_))
}

object Authoriser {

  def apply[F[_]: Sync](
      dfa: DataForAuthorisation, audience: Audience, isAuthorised: ClaimContent => Boolean = _ => true
  ): Auth[F] =
    new Authoriser[F](dfa, audience, isAuthorised).authorise
}
