package io.sherpair.w4s.auth.app

import cats.effect.Sync
import io.sherpair.w4s.auth.config.AuthConfig
import io.sherpair.w4s.domain.{Country, Logger}
import org.http4s.{EntityDecoder, HttpRoutes}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

class AuthApp[F[_]: Sync](implicit C: AuthConfig, L: Logger[F]) extends Http4sDsl[F] {

  implicit val countryEncoder: EntityDecoder[F, Country] = jsonOf[F, Country]

  def routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case request@PUT -> Root / "country" / id => NoContent()
  }
}
