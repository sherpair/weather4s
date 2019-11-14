package io.sherpair.w4s

import java.nio.file.{Files, Paths}
import java.security.{KeyFactory, PublicKey}
import java.security.spec.X509EncodedKeySpec
import java.time.Clock

import cats.Applicative
import cats.data.{Kleisli, OptionT}
import cats.effect.Sync
import cats.syntax.option._
import io.sherpair.w4s.config.Configuration
import io.sherpair.w4s.domain.{ClaimContent, W4sError}
import org.http4s.{AuthedRoutes, Request}
import org.http4s.dsl.Http4sDsl
import org.http4s.server.AuthMiddleware
import pdi.jwt.JwtAlgorithm.{RS256, RS384, RS512}
import pdi.jwt.JwtAlgorithm
import pdi.jwt.algorithms.JwtRSAAlgorithm

package object auth {

  type Audience = String
  type Auth[F[_]] = AuthMiddleware[F, ClaimContent]
  type AuthResult[F[_]] = Kleisli[F, Request[F], Either[String, ClaimContent]]

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

  def jwtAlgorithm[F[_]](implicit C: Configuration, S: Sync[F]): F[JwtRSAAlgorithm] =
    JwtAlgorithm.fromString(C.authToken.rsaKeyAlgorithm) match {
      case RS256 if C.authToken.rsaKeyStrength == 2048 => S.pure(RS256)
      case RS384 if C.authToken.rsaKeyStrength == 3072 => S.pure(RS384)
      case RS512 if C.authToken.rsaKeyStrength == 4096 => S.pure(RS512)
      case _ => S.raiseError[JwtRSAAlgorithm](W4sError(
        "Invalid RSA-Keys pair config. Can only be (2048,RS256) OR (3072,RS384) OR (4096,RS512). Aborting...")
      )
    }

  def loadPublicRsaKey[F[_]](implicit C: Configuration, S: Sync[F]): F[PublicKey] =
    S.delay {
      val publicBytes = Files.readAllBytes(Paths.get(getClass.getResource(C.authToken.publicKey).toURI))
      val publicKeySpec = new X509EncodedKeySpec(publicBytes)

      val keyFactory = KeyFactory.getInstance("RSA");
      keyFactory.generatePublic(publicKeySpec)
    }

  def onFailure[F[_]: Applicative]: AuthedRoutes[String, F] = {
    val http4sDsl = Http4sDsl[F]
    import http4sDsl._

    Kleisli {
      request => OptionT.liftF(Forbidden(request.authInfo))
    }
  }
}
