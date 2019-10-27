package io.sherpair.w4s.loader.app

import cats.effect.Sync
import cats.syntax.applicativeError._
import cats.syntax.functor._
import io.circe.Json
import io.circe.syntax._
import io.sherpair.w4s.config.Config4e
import io.sherpair.w4s.engine.Engine
import org.http4s.HttpRoutes
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

class Monitoring[F[_]: Sync](implicit C: Config4e, E: Engine[F]) extends Http4sDsl[F] {

  def routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "health" => Ok(healthCheck)
  }

  private def healthCheck: F[Json] = {
    for {
      (attempts, status) <- E.healthCheck.handleErrorWith {
        error => Sync[F].delay((C.healthAttemptsES, error.getMessage))
      }
    } yield Map(
      "attempts" -> attempts.toString,
      "engine" -> status
    ).asJson
  }
}
