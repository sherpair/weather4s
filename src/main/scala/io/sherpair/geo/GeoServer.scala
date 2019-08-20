package io.sherpair.geo

import cats.effect.{ConcurrentEffect, ContextShift, Resource, Timer}
import io.sherpair.geo.config.Configuration
import org.http4s.server.Server
import org.http4s.server.blaze.BlazeServerBuilder

object GeoServer {

  def describe[F[_]: ConcurrentEffect](implicit config: Configuration, T: Timer[F], C: ContextShift[F]): Resource[F, Server[F]] =
    for {
      server <- BlazeServerBuilder[F]
        .bindHttp(config.http.host.port, config.http.host.address)
        .resource
    } yield server
}
