package io.sherpair.w4s.auth.app

import cats.effect.Sync
import cats.syntax.applicative._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.circe.syntax._
import io.sherpair.w4s.auth.config.AuthConfig
import io.sherpair.w4s.auth.masterOnly
import io.sherpair.w4s.auth.repository.Repository
import io.sherpair.w4s.domain.ClaimContent
import org.http4s.{AuthedRoutes, Response}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

class Monitoring[F[_]: Sync](implicit C: AuthConfig, R: Repository[F]) extends Http4sDsl[F] {

  val attempts = 10

  private val masterOnlyRoutes: AuthedRoutes[ClaimContent, F] =
    AuthedRoutes.of[ClaimContent, F] {
      case GET -> Root / "health" as cC => masterOnly(cC, healthCheck)
    }

  val routes = masterOnlyRoutes

  private def healthCheck: F[Response[F]] =
    for {
      (attempts, status) <-
        R.healthCheck(C.healthAttemptsDB, C.healthIntervalDB)
          .handleErrorWith(error => (C.healthAttemptsDB, error.getMessage).pure[F])

      response <- Ok(Map("attempts" -> attempts.toString, "database" -> status).asJson)
    }
    yield response
}
