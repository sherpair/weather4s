package io.sherpair.w4s.auth.generator

import java.nio.file.{Files, Paths}
import java.security.KeyPairGenerator
import java.security.interfaces.{RSAPrivateKey, RSAPublicKey}
import java.util.Base64

import cats.effect.{ExitCode, IO, IOApp, Sync}
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.sherpair.w4s.auth.config.AuthConfig

class AsymmetricKeyGenerator[F[_]](C: AuthConfig)(implicit S: Sync[F]) {

  implicit val clock = java.time.Clock.systemUTC()

  val lineSeparator = Array[Byte](10)

  val privateDir = "auth-service/src/main/resources/auth"
  val privatePem = s"$privateDir/private.pem"
  val privateDer = s"$privateDir/private.der"

  val publicDir = "shared/src/main/resources/auth"
  val publicPem = s"$publicDir/public.pem"
  val publicDer = s"$publicDir/public.der"

  val generateAsymmetricKeys: F[(RSAPrivateKey, RSAPublicKey)] =
    S.delay {
      val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
      keyPairGenerator.initialize(C.authToken.rsaKeyStrength)
      val keyPair = keyPairGenerator.generateKeyPair
      (keyPair.getPrivate.asInstanceOf[RSAPrivateKey], keyPair.getPublic.asInstanceOf[RSAPublicKey])
    }

  def hd(n: String, d: String): Array[Byte] = s"${n}-----${d} KEY-----\n".getBytes("UTF-8")

  /* As of today, plain Java Security can only load files in Der format!! Pem files won't be actually used */
  def writeKeysToPemFile(privateKey: RSAPrivateKey, publicKey: RSAPublicKey): F[Unit] =
    S.delay {
      val encoder = Base64.getMimeEncoder(64, lineSeparator)

      val privateBytes = hd("", "BEGIN RSA PRIVATE") ++ encoder.encode(privateKey.getEncoded) ++ hd("\n", "END RSA PRIVATE")
      Files.write(Paths.get(privatePem), privateBytes)

      val publicBytes = hd("", "BEGIN RSA PUBLIC") ++ encoder.encode(publicKey.getEncoded) ++ hd("\n", "END RSA PUBLIC")
      Files.write(Paths.get(publicPem), publicBytes)
    }.void

  def writeKeysToDerFile(privateKey: RSAPrivateKey, publicKey: RSAPublicKey): F[Unit] =
    S.delay {
      Files.write(Paths.get(privateDer), privateKey.getEncoded)
      Files.write(Paths.get(publicDer), publicKey.getEncoded)
    }.void

  def app: F[Unit] =
    for {
      keysToFiles <- generateAsymmetricKeys

      _ <- writeKeysToPemFile(keysToFiles._1, keysToFiles._2)
      _ <- writeKeysToDerFile(keysToFiles._1, keysToFiles._2)
    }
    yield ()
}

object AsymmetricKeyGenerator extends IOApp {
  def run(args: List[String]): IO[ExitCode] =
    new AsymmetricKeyGenerator[IO](AuthConfig()).app *> IO.pure(ExitCode.Success)
}
