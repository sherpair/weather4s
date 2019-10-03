package io.sherpair.w4s.loader.http

import cats.effect.{Concurrent, ConcurrentEffect, ContextShift, Resource, Timer}
import io.sherpair.w4s.config.Host
import org.http4s.{HttpApp, HttpRoutes}
import org.http4s.server.{Router, Server}
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.{CORS, Logger}
import org.http4s.syntax.kleisli._

object LoaderServer {

  def apply[F[_]: ConcurrentEffect: ContextShift: Timer](host: Host, routes: Seq[HttpRoutes[F]]): Resource[F, Server[F]] =
    BlazeServerBuilder[F]
      .bindHttp(host.port, host.address)
      // .enableHttp2(true)  // HTTP/2 support requires TLS
      .withHttpApp(withMiddleware[F](routes))
      .resource

  private def withMiddleware[F[_]: Concurrent](routes: Seq[HttpRoutes[F]]): HttpApp[F] =
    Logger.httpApp(true, true)(CORS(Router(routes.map(("/loader", _)): _*)).orNotFound)
}
