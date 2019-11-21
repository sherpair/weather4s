package io.sherpair.w4s.auth.app

import java.security.PrivateKey

import cats.effect.{Blocker, IO}
import cats.syntax.flatMap._
import cats.syntax.option._
import io.sherpair.w4s.auth.app.Routes.loadPrivateRsaKey
import io.sherpair.w4s.auth.config.MaybePostman
import io.sherpair.w4s.auth.domain.{EmailType, Member, Token}
import io.sherpair.w4s.auth.jwtAlgorithm
import io.sherpair.w4s.auth.repository.{Repository, RepositoryMemberOps}
import io.sherpair.w4s.auth.repository.doobie.TransactorSpec
import io.sherpair.w4s.config.Configuration
import org.http4s.{Request, Response}
import org.http4s.server.Router
import org.http4s.syntax.kleisli._
import pdi.jwt.algorithms.JwtRSAAlgorithm

trait AuthenticatorSpec extends TransactorSpec {

  def withAuthAppRoutes(
      request: Request[IO], postman: MaybePostman = withoutPostman)(
      implicit R: Repository[IO], RM: RepositoryMemberOps[IO]
  ): IO[Response[IO]] = {
    R.tokenRepositoryOps >>= { implicit RT =>
      val (jwtAlgorithm, privateKey) = withDataForAuthenticator
      val authenticator = Authenticator[IO](jwtAlgorithm, postman, privateKey)
      val routes = new AuthApp[IO](authenticator).routes
      Router((aC.root, routes)).orNotFound.run(request)
    }
  }

  def withDataForAuthenticator: (JwtRSAAlgorithm, PrivateKey) = {
    implicit val blocker = Blocker.liftExecutionContext(ec)
    (for {
      jwtAlgorithm <- jwtAlgorithm[IO]
      privateKey <- loadPrivateRsaKey[IO]
    }
    yield (jwtAlgorithm, privateKey)).unsafeRunSync
  }

  val withoutPostman: MaybePostman = new MaybePostman {}

  abstract class PostmanFixture(implicit C: Configuration) extends MaybePostman {

    val expectedToken: Token => Unit

    override def sendEmail(token: Token, member: Member, emailType: EmailType): Option[String] = {
      expectedToken(token)
      url(emailType.segment, token.tokenId).some
    }
  }
}
