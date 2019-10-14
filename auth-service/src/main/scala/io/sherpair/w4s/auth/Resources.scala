package io.sherpair.w4s.auth

import cats.effect.{ConcurrentEffect => CE, ContextShift => CS, Resource, Timer}
import io.sherpair.w4s.auth.app.Routes
import io.sherpair.w4s.auth.config.AuthConfig
import io.sherpair.w4s.auth.repository.Repository
import io.sherpair.w4s.domain.Logger
import io.sherpair.w4s.http.HttpServer
import org.http4s.server.Server

object Resources {

  type CallGraphRes[F[_]] = Resource[F, Server[F]]

  def apply[F[_]: CE: CS: Logger: Timer](reposR: Resource[F, Repository[F]])(implicit C: AuthConfig): CallGraphRes[F] =
    for {
      implicit0(repository: Repository[F]) <- reposR
      _ <- repository.init
      routes <- Routes[F]
      server <- HttpServer[F](C.httpAuth.host, "/auth", routes)
    }
    yield server
}
