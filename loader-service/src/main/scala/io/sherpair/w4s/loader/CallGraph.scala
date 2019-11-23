package io.sherpair.w4s.loader

import cats.effect.{Blocker, ConcurrentEffect => CE, ContextShift => CS, Resource, Timer}
import fs2.concurrent.Queue
import io.sherpair.w4s.domain.{blockerForIOtasks, Country, Logger}
import io.sherpair.w4s.engine.Engine
import io.sherpair.w4s.http.{blazeClient, maybeWithSSLContext, withClientMiddleware, HttpServer}
import io.sherpair.w4s.loader.app.{Loader, Routes}
import io.sherpair.w4s.loader.config.LoaderConfig
import io.sherpair.w4s.loader.engine.EngineOps
import org.http4s.server.Server

object CallGraph {

  type CallGraphRes[F[_]] = Resource[F, Server[F]]


  def apply[F[_]: CE: CS: Logger: Timer](engineR: Resource[F, Engine[F]])(implicit C: LoaderConfig): CallGraphRes[F] =
    for {
      implicit0(engine: Engine[F]) <- engineR
      implicit0(engineOps: EngineOps[F]) <- Resource.liftF(EngineOps[F](C.clusterName))
      _ <- Resource.make(engineOps.init)(_ => engineOps.close)

      implicit0(blocker: Blocker) <- blockerForIOtasks

      sslContextO <- maybeWithSSLContext
      clientNoMW <- blazeClient(sslContextO)
      client <- withClientMiddleware(clientNoMW)

      countryQueue <- Resource.liftF(Queue.boundedNoneTerminated[F, Country](C.maxEnqueuedCountries))
      loaderFiber <- Loader(client, countryQueue)

      routes <- Routes(countryQueue, loaderFiber)
      server <- HttpServer(routes, sslContextO)
    }
    yield server
}
