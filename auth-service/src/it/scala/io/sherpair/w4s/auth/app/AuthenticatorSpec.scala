package io.sherpair.w4s.auth.app

import java.security.PrivateKey

import cats.effect.IO
import cats.syntax.flatMap._
import io.sherpair.w4s.auth.app.Routes.loadPrivateRsaKey
import io.sherpair.w4s.auth.config.MaybePostman
import io.sherpair.w4s.auth.jwtAlgorithm
import io.sherpair.w4s.auth.repository.{Repository, RepositoryMemberOps}
import io.sherpair.w4s.auth.repository.doobie.TransactorSpec
import org.http4s.{Request, Response}
import org.http4s.server.Router
import org.http4s.syntax.kleisli._
import pdi.jwt.algorithms.JwtRSAAlgorithm

trait AuthenticatorSpec extends TransactorSpec {

  val withoutPostman: MaybePostman = new MaybePostman {}

  def withAuthAppRoutes(
    postman: MaybePostman, request: Request[IO])(implicit R: Repository[IO], RM: RepositoryMemberOps[IO]
  ): IO[Response[IO]] = {
    R.tokenRepositoryOps >>= { implicit RT =>
      val (jwtAlgorithm, privateKey) = withDataForAuthenticator
      val authenticator = Authenticator[IO](jwtAlgorithm, postman, privateKey)
      val routes = new AuthApp[IO](authenticator).routes
      Router((aC.root, routes)).orNotFound.run(request)
    }
  }

  def withDataForAuthenticator: (JwtRSAAlgorithm, PrivateKey) =
    (for {
      jwtAlgorithm <- jwtAlgorithm[IO]
      privateKey <- loadPrivateRsaKey[IO]
    }
    yield (jwtAlgorithm, privateKey)).unsafeRunSync
}
