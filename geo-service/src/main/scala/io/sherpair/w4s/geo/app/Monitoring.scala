package io.sherpair.w4s.geo.app

import cats.effect.Sync
import cats.syntax.functor._
import io.circe.Json
import io.circe.syntax._
import io.sherpair.w4s.config.Engine.healthAttempts
import io.sherpair.w4s.engine.Engine
import io.sherpair.w4s.geo.config.Configuration
import org.http4s.HttpRoutes
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

class Monitoring[F[_]: Sync](implicit C: Configuration, E: Engine[F]) extends Http4sDsl[F] {

  def routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "health" => Ok(healthCheck)
  }

  private def healthCheck: F[Json] = {
    for {
      (attempts, status) <- Sync[F].handleErrorWith[(Int, String)](E.healthCheck) {
        error => Sync[F].delay((healthAttempts(C.engine), error.getMessage))
      }
    } yield Map(
      "attempts" -> attempts.toString,
      "engine" -> status
    ).asJson
  }
}
