package io.sherpair.w4s

import cats.{Applicative, Monad}
import cats.data.Kleisli
import cats.effect.{Blocker, ContextShift, IO}
import cats.syntax.applicative._
import cats.syntax.either._
import io.sherpair.w4s.auth.{jwtAlgorithm, loadPublicRsaKey, onFailure, Authoriser, AuthResult}
import io.sherpair.w4s.config.Configuration
import io.sherpair.w4s.domain.{ClaimContent, DataForAuthorisation, Logger, Role}
import io.sherpair.w4s.domain.Role.{Master, Member}
import org.http4s.server.AuthMiddleware

trait FakeAuth extends Fixtures {

  def withDataForAuthorisation(
      B: Blocker)(implicit C: Configuration, CS: ContextShift[IO], L: Logger[IO]
  ): DataForAuthorisation = {
    implicit val blocker = B
    val t = (for {
      jwtAlgorithm <- jwtAlgorithm[IO]
      publicKey <- loadPublicRsaKey[IO]
    }
    yield (jwtAlgorithm, publicKey)).unsafeRunSync

    DataForAuthorisation(t._1, t._2)
  }

  def withMasterAuth[F[_]: Monad]: Authoriser[F] =
    AuthMiddleware(validateFakeRequest(fakeId, Master), onFailure)

  def withMemberAuth[F[_]: Monad]: Authoriser[F] =
    AuthMiddleware(validateFakeRequest(fakeId, Member), onFailure)

  def withMemberAuth[F[_]: Monad](id: Long): Authoriser[F] =
    AuthMiddleware(validateFakeRequest(id, Member), onFailure)

  private def genClaimContent(id: Long, role: Role): ClaimContent =
    ClaimContent(id, alpha, alpha, alpha, numStr, oneElementFrom(countries), role, pastInstant)

  private def validateFakeRequest[F[_]: Applicative](id: Long, role: Role): AuthResult[F, ClaimContent] =
    Kleisli(_ => genClaimContent(id, role).asRight[String].pure[F])
}
