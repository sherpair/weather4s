package io.sherpair.w4s.http

import cats.effect.{Concurrent, ConcurrentEffect, ContextShift, Resource, Timer}
import io.sherpair.w4s.config.Host
import org.http4s.{HttpApp, HttpRoutes}
import org.http4s.server.{Router, Server}
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.{CORS, Logger}
import org.http4s.syntax.kleisli._

object HttpServer {

  def apply[F[_]: ConcurrentEffect: ContextShift: Timer](
      host: Host, httpPoolSize: Int, root: String, routes: Seq[HttpRoutes[F]], sslDataO: Option[SSLData] = None
  ): Resource[F, Server[F]] = {

    val blazeBuilder: BlazeServerBuilder[F] = sslDataO.fold {
      BlazeServerBuilder[F].
        bindHttp(host.port, host.address)
    } { sslData =>
      BlazeServerBuilder[F]
        .bindHttp(sslData.host.port, sslData.host.address)
        .enableHttp2(true)
        .withSSLContext(sslData.context)
    }

    blazeBuilder
      .withConnectorPoolSize(httpPoolSize)
      .withHttpApp(withMiddleware[F](root, routes))
      .resource
  }

  private def withMiddleware[F[_]: Concurrent](root: String, routes: Seq[HttpRoutes[F]]): HttpApp[F] =
    Logger.httpApp(true, true)(CORS(Router(routes.map((root, _)): _*)).orNotFound)
}

/*
   To start the Blaze Server concurrently and return the fiber (as a Resource)...
    Resource.liftF(
      BlazeServerBuilder[F]
        .bindHttp(host.port, host.address)
        .withHttpApp(withMiddleware[F](root, routes))
        .serve.compile.drain
        .map(identity).start
    )
 */
