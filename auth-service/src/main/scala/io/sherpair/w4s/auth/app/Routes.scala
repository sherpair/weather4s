package io.sherpair.w4s.auth.app

import java.nio.file.{Files, Paths}
import java.security.{KeyFactory, PrivateKey}
import java.security.spec.PKCS8EncodedKeySpec

import cats.Applicative
import cats.effect.{ConcurrentEffect => CE, Resource, Sync}
import io.sherpair.w4s.auth.{jwtAlgorithm, loadPublicRsaKey}
import io.sherpair.w4s.auth.config.AuthConfig
import io.sherpair.w4s.auth.domain.User
import io.sherpair.w4s.auth.repository.{Repository, RepositoryTokenOps, RepositoryUserOps}
import io.sherpair.w4s.domain.{AuthData, Logger}
import org.http4s.{EntityEncoder, HttpRoutes}
import org.http4s.circe.jsonEncoderOf
import tsec.passwordhashers.jca.JCAPasswordPlatform

object Routes {

  def apply[F[_]: CE, A](
      jca: JCAPasswordPlatform[A])(implicit C: AuthConfig, L: Logger[F], R: Repository[F]
  ): Resource[F, Seq[HttpRoutes[F]]] =
    for {
      implicit0(repositoryTokenOps: RepositoryTokenOps[F]) <- Resource.liftF(R.tokenRepositoryOps)
      implicit0(repositoryUserOps: RepositoryUserOps[F]) <- Resource.liftF(R.userRepositoryOps)

      jwtAlgorithm <- Resource.liftF(jwtAlgorithm)

      publicKey <- Resource.liftF(loadPublicRsaKey)
      authData = AuthData(jwtAlgorithm, publicKey)

      privateKey <- Resource.liftF(loadPrivateRsaKey)
      authenticator = Authenticator[F](jwtAlgorithm, privateKey)

      routes <- Resource.liftF(CE[F].delay(
        Seq(
          new AuthApp[F, A](authenticator, jca).routes,
          new Monitoring[F](authData).routes,
          new UserApp[F](authData).routes
        )
      ))
    }
    yield routes

  implicit def userEncoder[F[_]: Applicative]: EntityEncoder[F, User] = jsonEncoderOf

  def loadPrivateRsaKey[F[_] : Sync](implicit C: AuthConfig): F[PrivateKey] =
    Sync[F].delay {
      val privateBytes = Files.readAllBytes(Paths.get(getClass.getResource(C.privateKey).toURI))
      val privateKeySpec = new PKCS8EncodedKeySpec(privateBytes)

      val keyFactory = KeyFactory.getInstance("RSA");
      keyFactory.generatePrivate(privateKeySpec)
    }
}
