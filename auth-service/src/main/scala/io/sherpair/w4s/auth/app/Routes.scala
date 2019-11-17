package io.sherpair.w4s.auth.app

import java.nio.file.{Files, Paths}
import java.security.{KeyFactory, PrivateKey}
import java.security.spec.PKCS8EncodedKeySpec

import scala.io.Source.fromResource

import cats.Applicative
import cats.effect.{ConcurrentEffect => CE, Resource, Sync}
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.sherpair.w4s.auth.{jwtAlgorithm, loadPublicRsaKey, Authoriser, Claims}
import io.sherpair.w4s.auth.config.{AuthConfig, MaybePostman, Postman, Smtp}
import io.sherpair.w4s.auth.domain.EmailType.{Activation, ResetSecret}
import io.sherpair.w4s.auth.domain.Member
import io.sherpair.w4s.auth.repository.{Repository, RepositoryMemberOps, RepositoryTokenOps}
import io.sherpair.w4s.domain.{DataForAuthorisation, Logger}
import io.sherpair.w4s.domain.Role.Master
import org.http4s.{EntityEncoder, HttpRoutes}
import org.http4s.circe.jsonEncoderOf

object Routes {

  def apply[F[_]: CE](implicit C: AuthConfig, L: Logger[F], R: Repository[F]): Resource[F, Seq[HttpRoutes[F]]] =
    for {
      implicit0(repositoryMemberOps: RepositoryMemberOps[F]) <- Resource.liftF(R.memberRepositoryOps)
      implicit0(repositoryTokenOps: RepositoryTokenOps[F]) <- Resource.liftF(R.tokenRepositoryOps)

      jwtAlgorithm <- Resource.liftF(jwtAlgorithm)

      publicKey <- Resource.liftF(loadPublicRsaKey)

      postman <- Resource.liftF(withPostman)
      privateKey <- Resource.liftF(loadPrivateRsaKey)
      authenticator = Authenticator[F](jwtAlgorithm, postman, privateKey)

      routes <- Resource.liftF(CE[F].delay {

        val dfa = DataForAuthorisation(jwtAlgorithm, publicKey)
        val masterAuthoriser = Authoriser[F](dfa, Claims.audAuth, _.role == Master)
        val memberAuthoriser = Authoriser(dfa, Claims.audAuth)

        Seq(
          new AuthApp[F](authenticator).routes,
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

  private def loadTemplate[F[_]](template: String)(implicit S: Sync[F]): F[String] =
    Resource
      .fromAutoCloseable(S.delay(fromResource(s"templates/${template}")))
      .use(_.mkString.pure[F])

  private def withPostman[F[_]](implicit C: AuthConfig, S: Sync[F]): F[MaybePostman] =
    for {
      activationTemplate <- loadTemplate[F](Activation.template)
      resetSecretTemplate <- loadTemplate[F](ResetSecret.template)
      templateMap <- S.delay(Map[String, String](
        Activation.reason -> activationTemplate,
        ResetSecret.reason -> resetSecretTemplate
      ))
    }
    yield new Postman(Smtp(), templateMap)
}
