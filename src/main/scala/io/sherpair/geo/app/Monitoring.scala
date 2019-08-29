package io.sherpair.geo.app

import cats.effect.Sync
import cats.syntax.functor._
import io.circe.Json
import io.circe.syntax._
import io.sherpair.geo.engine.Engine
import org.http4s.HttpRoutes
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

class Monitoring[F[_]: Sync](implicit engine: Engine[F]) extends Http4sDsl[F] {

  def routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "health" => Ok(healthCheck)
  }

  private def healthCheck: F[Json] =
    for {
      engine <- engine.healthCheck
    } yield Map(
      "engine" -> engine
    ).asJson
}
