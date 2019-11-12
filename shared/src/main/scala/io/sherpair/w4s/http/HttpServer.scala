package io.sherpair.w4s.http

import javax.net.ssl.SSLContext

import cats.effect.{Concurrent, ConcurrentEffect, ContextShift, Resource, Timer}
import io.sherpair.w4s.config.Configuration
import org.http4s.{HttpApp, HttpRoutes}
import org.http4s.server.{Router, Server}
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.{CORS, Logger}
import org.http4s.syntax.kleisli._

object HttpServer {

  def apply[F[_]: ConcurrentEffect: ContextShift: Timer](
      routes: Seq[HttpRoutes[F]], sslContextO: Option[SSLContext] = None)(implicit C: Configuration
  ): Resource[F, Server[F]] = {

    val blazeBuilder: BlazeServerBuilder[F] = sslContextO.fold {
      BlazeServerBuilder[F].bindHttp(C.host.port, C.host.address)
    } { sslContext =>
      BlazeServerBuilder[F].bindHttp(C.host.port, C.host.address).withSSLContext(sslContext).enableHttp2(true)
    }

    blazeBuilder
      .withConnectorPoolSize(C.httpPoolSize)
      .withHttpApp(withMiddleware[F](C.root, routes))
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