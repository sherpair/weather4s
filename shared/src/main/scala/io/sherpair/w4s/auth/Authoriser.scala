package io.sherpair.w4s.auth

import cats.data.Kleisli
import cats.effect.{Blocker, ContextShift => CS, Sync}
import cats.syntax.applicative._
import cats.syntax.either._
import cats.syntax.functor._
import cats.syntax.try_._
import io.circe.parser.decode
import io.sherpair.w4s.config.Configuration
import io.sherpair.w4s.domain.{ClaimContent, DataForAuthorisation, Logger}
import org.http4s.Message
import org.http4s.server.AuthMiddleware
import pdi.jwt.{JwtCirce, JwtClaim, JwtOptions}
import pdi.jwt.algorithms.JwtRSAAlgorithm
import pdi.jwt.exceptions.JwtException

abstract class MessageValidator[F[_]: Sync](audience: Audience, dfa: DataForAuthorisation) {

  private def decodeToken(token: String): F[Either[String, ClaimContent]] =
    JwtCirce.decodeAll(token, dfa.publicKey, dfa.jwtAlgorithms, JwtOptions.DEFAULT)
      .map(tokenAsTuple => validateClaim(tokenAsTuple._2))
      .recover { case _: JwtException => notAuthorized }
      .liftTo[F]

  private lazy val missingToken = "Authorization token is missing".asLeft[ClaimContent].pure[F]

  private lazy val notAuthorized = "Not authorized".asLeft[ClaimContent]

  private def validateClaim(claim: JwtClaim): Either[String, ClaimContent] =
    if (claim.isValid(Claims.iss, audience)) {
      decode[ClaimContent](claim.content).fold(_ => notAuthorized, content => content.asRight[String])
    } else notAuthorized

  def validateMessage(message: Message[F]): F[Either[String, ClaimContent]] =
    retrieveBearerTokenFromMessage(message).fold(missingToken)(decodeToken(_))
}

class AuthGenerator[F[_]: Sync](audience: Audience, dfa: DataForAuthorisation) extends MessageValidator(audience, dfa) {

  val gen: Authoriser[F] = AuthMiddleware(validateRequest, onFailure)

  private def validateRequest: AuthResult[F, ClaimContent] = Kleisli(validateMessage(_))
}

object Authoriser {

  def apply[F[_]: CS: Logger: Sync](
      audience: Audience, jwtAlgorithm: JwtRSAAlgorithm)(
      implicit B: Blocker, C: Configuration
  ): F[Authoriser[F]] =
    loadPublicRsaKey.map { publicKey =>
      new AuthGenerator[F](audience, DataForAuthorisation(jwtAlgorithm, publicKey)).gen
    }
}
