package io.sherpair.w4s.auth.app

import cats.effect.Sync
import cats.syntax.applicativeError._
import cats.syntax.functor._
import io.circe.Json
import io.circe.syntax._
import io.sherpair.w4s.auth.config.AuthConfig
import io.sherpair.w4s.auth.repository.Repository
import org.http4s.HttpRoutes
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

class Monitoring[F[_]: Sync](implicit C: AuthConfig, repository: Repository[F]) extends Http4sDsl[F] {

  val attempts = 10

  def routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "health" => Ok(healthCheck)
  }

  private def healthCheck: F[Json] = {
    for {
      (attempts, status) <- repository
        .healthCheck(C.healthAttemptsDB, C.healthIntervalDB)
        .handleErrorWith {
          error => Sync[F].delay((C.healthAttemptsDB, error.getMessage))
        }
    } yield Map(
      "attempts" -> attempts.toString,
      "database" -> status
    ).asJson
  }
}
