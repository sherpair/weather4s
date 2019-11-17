package io.sherpair.w4s

import cats.{Applicative, Monad}
import cats.data.Kleisli
import cats.effect.IO
import cats.syntax.applicative._
import cats.syntax.either._
import io.sherpair.w4s.auth.{jwtAlgorithm, loadPublicRsaKey, onFailure, Auth, AuthResult}
import io.sherpair.w4s.config.Configuration
import io.sherpair.w4s.domain.{ClaimContent, DataForAuthorisation, Role}
import io.sherpair.w4s.domain.Role.{Master, Member}
import org.http4s.server.AuthMiddleware

trait FakeAuth extends Fixtures {

  def withDataForAuthorisation(implicit C: Configuration): DataForAuthorisation = {
    val t = (for {
      jwtAlgorithm <- jwtAlgorithm[IO]
      publicKey <- loadPublicRsaKey[IO]
    }
    yield (jwtAlgorithm, publicKey)).unsafeRunSync

    DataForAuthorisation(t._1, t._2)
  }

  def withMasterAuth[F[_]: Monad]: Auth[F] = AuthMiddleware(validateFakeRequest(fakeId, Master), onFailure)

  def withMemberAuth[F[_]: Monad]: Auth[F] = AuthMiddleware(validateFakeRequest(fakeId, Member), onFailure)

  def withMemberAuth[F[_]: Monad](id: Long): Auth[F] = AuthMiddleware(validateFakeRequest(id, Member), onFailure)

  private def genClaimContent(id: Long, role: Role): ClaimContent =
    ClaimContent(id, alpha, alpha, alpha, numStr, oneElementFrom(countries), role, pastInstant)

  private def validateFakeRequest[F[_]: Applicative](id: Long, role: Role): AuthResult[F] =
    Kleisli(_ => genClaimContent(id, role).asRight[String].pure[F])
}
