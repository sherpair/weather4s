package io.sherpair.w4s.auth.app

import java.nio.file.{Files, Paths}
import java.security.{KeyFactory, PrivateKey}
import java.security.spec.PKCS8EncodedKeySpec

import cats.Applicative
import cats.effect.{ConcurrentEffect => CE, Resource, Sync}
import io.sherpair.w4s.auth.{jwtAlgorithm, loadPublicRsaKey, Authoriser, Claims}
import io.sherpair.w4s.auth.config.AuthConfig
import io.sherpair.w4s.auth.domain.Member
import io.sherpair.w4s.auth.repository.{Repository, RepositoryMemberOps, RepositoryTokenOps}
import io.sherpair.w4s.domain.{AuthData, Logger}
import io.sherpair.w4s.domain.Role.Master
import org.http4s.{EntityEncoder, HttpRoutes}
import org.http4s.circe.jsonEncoderOf
import tsec.passwordhashers.jca.JCAPasswordPlatform

object Routes {

  def apply[F[_]: CE, A](
      jca: JCAPasswordPlatform[A])(implicit C: AuthConfig, L: Logger[F], R: Repository[F]
  ): Resource[F, Seq[HttpRoutes[F]]] =
    for {
      implicit0(repositoryMemberOps: RepositoryMemberOps[F]) <- Resource.liftF(R.memberRepositoryOps)
      implicit0(repositoryTokenOps: RepositoryTokenOps[F]) <- Resource.liftF(R.tokenRepositoryOps)

      jwtAlgorithm <- Resource.liftF(jwtAlgorithm)

      publicKey <- Resource.liftF(loadPublicRsaKey)

      privateKey <- Resource.liftF(loadPrivateRsaKey)
      authenticator = Authenticator[F](jwtAlgorithm, privateKey)

      routes <- Resource.liftF(CE[F].delay {

        val authData = AuthData(jwtAlgorithm, publicKey)
        val masterAuthoriser = Authoriser[F](authData, Claims.audAuth, _.role == Master)
        val memberAuthoriser = Authoriser(authData, Claims.audAuth)

        Seq(
          new AuthApp[F, A](authenticator, jca).routes,
          new MemberApp[F](masterAuthoriser, memberAuthoriser).routes,
          new Monitoring[F](masterAuthoriser).routes
        )
      })
    }
    yield routes

  implicit def memberEncoder[F[_]: Applicative]: EntityEncoder[F, Member] = jsonEncoderOf

  def loadPrivateRsaKey[F[_] : Sync](implicit C: AuthConfig): F[PrivateKey] =
    Sync[F].delay {
      val privateBytes = Files.readAllBytes(Paths.get(getClass.getResource(C.privateKey).toURI))
      val privateKeySpec = new PKCS8EncodedKeySpec(privateBytes)

      val keyFactory = KeyFactory.getInstance("RSA");
      keyFactory.generatePrivate(privateKeySpec)
    }
}
