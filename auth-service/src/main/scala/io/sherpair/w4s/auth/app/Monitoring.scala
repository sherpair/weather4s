package io.sherpair.w4s.auth.app

import cats.effect.Sync
import cats.syntax.applicative._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.circe.syntax._
import io.sherpair.w4s.auth.{Authoriser, Claims}
import io.sherpair.w4s.auth.config.AuthConfig
import io.sherpair.w4s.auth.repository.Repository
import io.sherpair.w4s.domain.{AuthData, ClaimContent}
import io.sherpair.w4s.domain.Role.Master
import org.http4s.{AuthedRoutes, Response}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

class Monitoring[F[_]: Sync](authData: AuthData)(implicit C: AuthConfig, R: Repository[F]) extends Http4sDsl[F] {

  val attempts = 10

  val masterOnlyRoutes: AuthedRoutes[ClaimContent, F] =
    AuthedRoutes.of[ClaimContent, F] {
      case GET -> Root / "health" as _ => healthCheck
    }

  val masterAuthoriser = Authoriser[F](authData, Claims.audAuth, _.role == Master)

  val routes = masterAuthoriser(masterOnlyRoutes)

  private def healthCheck: F[Response[F]] =
    for {
      (attempts, status) <-
        R.healthCheck(C.healthAttemptsDB, C.healthIntervalDB)
          .handleErrorWith(error => (C.healthAttemptsDB, error.getMessage).pure[F])

      response <- Ok(Map("attempts" -> attempts.toString, "database" -> status).asJson)
    }
    yield response
}
