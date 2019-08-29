package io.sherpair.geo.http

import cats.effect.{Concurrent, ConcurrentEffect, ContextShift, Resource, Timer}
import io.sherpair.geo.config.Configuration
import io.sherpair.geo.engine.EngineOps
import org.http4s.{HttpApp, HttpRoutes}
import org.http4s.server.{Router, Server}
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.{CORS, Logger}
import org.http4s.syntax.kleisli._

object GeoServer {

  def describe[F[_]: ConcurrentEffect: ContextShift: Timer](
      routes: Seq[HttpRoutes[F]]
  )(implicit config: Configuration, engineOps: EngineOps[F]): Resource[F, Server[F]] =
    for {
      server <- BlazeServerBuilder[F]
        .bindHttp(config.http.host.port, config.http.host.address)
        .withHttpApp(withMiddleware[F](routes))
        .resource
    } yield server

  private def withMiddleware[F[_]: Concurrent](routes: Seq[HttpRoutes[F]]): HttpApp[F] =
    Logger.httpApp(true, true)(CORS(Router(routes.map(("/", _)): _*)).orNotFound)
}
