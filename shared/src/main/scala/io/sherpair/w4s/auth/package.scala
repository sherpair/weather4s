package io.sherpair.w4s

import java.security.{KeyFactory, PublicKey}
import java.security.spec.X509EncodedKeySpec
import java.time.Clock

import cats.Applicative
import cats.data.{Kleisli, OptionT}
import cats.effect.{Blocker, ContextShift => CS, Sync}
import cats.syntax.functor._
import cats.syntax.option._
import io.sherpair.w4s.config.Configuration
import io.sherpair.w4s.domain.{loadResource, ClaimContent, Logger, W4sError}
import io.sherpair.w4s.domain.Role.Master
import org.http4s.{AuthedRoutes, AuthScheme, Credentials, Message, Request, Response}
import org.http4s.Credentials.Token
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Authorization
import org.http4s.server.AuthMiddleware
import pdi.jwt.JwtAlgorithm.{RS256, RS384, RS512}
import pdi.jwt.JwtAlgorithm
import pdi.jwt.algorithms.JwtRSAAlgorithm

package object auth {

  type Audience = String
  type Auth[F[_], T] = AuthMiddleware[F, T]
  type AuthResult[F[_], T] = Kleisli[F, Request[F], Either[String, T]]
  type Authoriser[F[_]] = AuthMiddleware[F, ClaimContent]

  implicit lazy val clock = Clock.systemUTC()

  object Claims {

    lazy val audAuth: Audience = "https://sherpair.io/weather4s/auth"
    lazy val audGeo: Audience = "https://sherpair.io/weather4s/geo"
    lazy val audLoader: Audience = "https://sherpair.io/weather4s/loader"

    lazy val aud = Set(audAuth, audGeo, audLoader)
    lazy val iss = audAuth

    lazy val audO = aud.some
    lazy val issO = iss.some
  }

  def addBearerTokenToRequest[F[_]](request: Request[F], token: String): Request[F] =
    request.putHeaders(Authorization(Credentials.Token(AuthScheme.Bearer, token)))

  def jwtAlgorithm[F[_]](implicit C: Configuration, S: Sync[F]): F[JwtRSAAlgorithm] =
    JwtAlgorithm.fromString(C.authToken.rsaKeyAlgorithm) match {
      case RS256 if C.authToken.rsaKeyStrength == 2048 => S.pure(RS256)
      case RS384 if C.authToken.rsaKeyStrength == 3072 => S.pure(RS384)
      case RS512 if C.authToken.rsaKeyStrength == 4096 => S.pure(RS512)
      case _ => S.raiseError[JwtRSAAlgorithm](W4sError(
        "Invalid RSA-Keys pair config. Can only be (2048,RS256) OR (3072,RS384) OR (4096,RS512). Aborting...")
      )
    }

  def loadPublicRsaKey[F[_]: CS: Logger: Sync](implicit B: Blocker, C: Configuration): F[PublicKey] =
    loadResource(C.authToken.publicKey).map { publicKeyBytes =>
      val publicKeySpec = new X509EncodedKeySpec(publicKeyBytes)

      val keyFactory = KeyFactory.getInstance("RSA");
      keyFactory.generatePublic(publicKeySpec)
    }

  def masterOnly[F[_]: Sync](claimContent: ClaimContent, f: => F[Response[F]]): F[Response[F]] = {
    val http4sDsl = Http4sDsl[F]
    import http4sDsl._

    if (claimContent.role == Master) f else Forbidden("Not authorized")
  }

  def onFailure[F[_]: Applicative]: AuthedRoutes[String, F] = {
    val http4sDsl = Http4sDsl[F]
    import http4sDsl._

    Kleisli {
      request => OptionT.liftF(Forbidden(request.authInfo))
    }
  }

  def retrieveBearerTokenFromMessage[F[_]](message: Message[F]): Option[String] =
    message.headers.get(Authorization).collect {
      case Authorization(Token(AuthScheme.Bearer, token)) => token
    }
}
