package io.sherpair.w4s.auth

import cats.effect.{ConcurrentEffect => CE, ContextShift => CS, Resource, Timer}
import io.sherpair.w4s.auth.app.Routes
import io.sherpair.w4s.auth.config.AuthConfig
import io.sherpair.w4s.auth.repository.Repository
import io.sherpair.w4s.domain.Logger
import io.sherpair.w4s.http.{maybeWithSSLContext, HttpServer}
import org.http4s.server.Server
import tsec.passwordhashers.jca.JCAPasswordPlatform

object CallGraph {

  type CallGraphRes[F[_]] = Resource[F, Server[F]]

  def apply[F[_]: CE: CS: Timer, A](
    jca: JCAPasswordPlatform[A], repo: Resource[F, Repository[F]])(implicit C: AuthConfig, L: Logger[F]
  ): CallGraphRes[F] =
    for {
      implicit0(repository: Repository[F]) <- repo
      _ <- Resource.liftF(repository.init)

      routes <- Routes[F, A](jca)
      sslContextO <- maybeWithSSLContext[F]
      server <- HttpServer[F](routes, sslContextO)
    }
    yield server
}
