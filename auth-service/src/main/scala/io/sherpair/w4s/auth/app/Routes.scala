package io.sherpair.w4s.auth.app

import java.nio.charset.StandardCharsets.UTF_8
import java.security.{KeyFactory, PrivateKey}
import java.security.spec.PKCS8EncodedKeySpec

import cats.Applicative
import cats.effect.{Blocker, ConcurrentEffect => CE, ContextShift => CS, Resource, Sync}
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.semigroupk._
import io.sherpair.w4s.auth.{jwtAlgorithm, Authoriser, Claims}
import io.sherpair.w4s.auth.config.{AuthConfig, MaybePostman, Postman, Smtp}
import io.sherpair.w4s.auth.domain.EmailType.{Activation, ResetSecret}
import io.sherpair.w4s.auth.domain.Member
import io.sherpair.w4s.auth.repository.{Repository, RepositoryMemberOps, RepositoryTokenOps}
import io.sherpair.w4s.domain.{blockerForIOtasks, loadResource, Logger}
import io.sherpair.w4s.http.ApiApp
import org.http4s.{EntityEncoder, HttpRoutes}
import org.http4s.circe.jsonEncoderOf

object Routes {

  def apply[F[_]: CE: CS: Logger](implicit C: AuthConfig, R: Repository[F]): Resource[F, HttpRoutes[F]] =
    for {
      implicit0(repositoryMemberOps: RepositoryMemberOps[F]) <- Resource.liftF(R.memberRepositoryOps)
      implicit0(repositoryTokenOps: RepositoryTokenOps[F]) <- Resource.liftF(R.tokenRepositoryOps)

      implicit0(blocker: Blocker) <- blockerForIOtasks(2)
      postman <- Resource.liftF(withPostman)
      privateKey <- Resource.liftF(loadPrivateRsaKey)

      jwtAlgorithm <- Resource.liftF(jwtAlgorithm)

      authoriser <- Resource.liftF(Authoriser[F](Claims.audAuth, jwtAlgorithm))

      routes <- Resource.liftF(CE[F].delay {
        val authenticator = Authenticator[F](jwtAlgorithm, postman, privateKey)
        new ApiApp[F].routes <+>
        new AuthApp[F](authenticator).routes <+>
        authoriser(new MemberApp[F](authenticator).routes) <+>
        authoriser(new Monitoring[F].routes)
      })
    }
    yield routes

  implicit def memberEncoder[F[_]: Applicative]: EntityEncoder[F, Member] = jsonEncoderOf

  def loadPrivateRsaKey[F[_]: CS: Logger: Sync](implicit B: Blocker, C: AuthConfig): F[PrivateKey] =
    loadResource(C.privateKey).map { privateKeyBytes =>
      val privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes)

      val keyFactory = KeyFactory.getInstance("RSA");
      keyFactory.generatePrivate(privateKeySpec)
    }

  private def withPostman[F[_]: CS](implicit B: Blocker, C: AuthConfig, L: Logger[F], S: Sync[F]): F[MaybePostman] =
    for {
      activationTemplate <- loadResource(s"/templates/${Activation.template}")
      resetSecretTemplate <- loadResource(s"/templates/${ResetSecret.template}")
      templateMap <- S.delay(Map[String, String](
        Activation.reason -> new String(activationTemplate, UTF_8),
        ResetSecret.reason -> new String(resetSecretTemplate, UTF_8)
      ))
      postman <- S.delay(new Postman(Smtp(), templateMap))
      _ <- L.info(s"Emails will be sent from the Postman to '${postman.emailRoot}'")
    }
    yield postman
}
