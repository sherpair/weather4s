package io.sherpair.w4s.geo.http

import cats.effect.{Concurrent, ConcurrentEffect, ContextShift, Fiber, Resource, Timer}
import cats.effect.syntax.concurrent._
import cats.syntax.functor._
import io.sherpair.w4s.config.Host
import org.http4s.{HttpApp, HttpRoutes}
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.{CORS, Logger}
import org.http4s.syntax.kleisli._

object GeoServer {

  def apply[F[_]: ConcurrentEffect: ContextShift: Timer](host: Host, routes: Seq[HttpRoutes[F]]): Resource[F, Fiber[F, Unit]] =
    Resource.liftF(
      BlazeServerBuilder[F]
        .bindHttp(host.port, host.address)
        // .enableHttp2(true)  // HTTP/2 support requires TLS
        .withHttpApp(withMiddleware[F](routes))
        .serve.compile.drain
        .map(identity).start
    )

  private def withMiddleware[F[_]: Concurrent](routes: Seq[HttpRoutes[F]]): HttpApp[F] =
    Logger.httpApp(true, true)(CORS(Router(routes.map(("/geo", _)): _*)).orNotFound)
}
