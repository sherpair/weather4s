package io.sherpair.w4s.loader

import java.util.concurrent.{Executors, ExecutorService, ThreadFactory}

import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration._

import cats.effect.{Blocker, ConcurrentEffect => CE, ContextShift => CS, Resource, Timer}
import fs2.concurrent.Queue
import io.sherpair.w4s.domain.{Country, Logger}
import io.sherpair.w4s.engine.Engine
import io.sherpair.w4s.http.HttpServer
import io.sherpair.w4s.loader.app.{Loader, Routes}
import io.sherpair.w4s.loader.config.LoaderConfig
import io.sherpair.w4s.loader.engine.EngineOps
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.server.Server

object Resources {

  type CallGraphRes[F[_]] = Resource[F, Server[F]]

  def apply[F[_]: CE: CS: Timer](engineR: Resource[F, Engine[F]])(implicit C: LoaderConfig, L: Logger[F]): CallGraphRes[F] =
    for {
      implicit0(engine: Engine[F]) <- engineR
      implicit0(engineOps: EngineOps[F]) <- Resource.liftF(EngineOps[F](C.clusterName))
      _ <- Resource.make(engineOps.init)(_ => engineOps.close)
      countryQueue <- Resource.liftF(Queue.boundedNoneTerminated[F, Country](C.maxEnqueuedCountries))
      blocker <- blocker[F]
      client <- blazeClient
      loaderFiber <- Loader(blocker, client, countryQueue)
      routes <- Routes[F](countryQueue, loaderFiber)
      server <- HttpServer[F](C.hostLoader, C.httpPoolSize, "/loader", routes)
    }
    yield server

    private def blazeClient[F[_]: CE]: Resource[F, Client[F]] =
      BlazeClientBuilder[F](global)
        .withConnectTimeout(30 seconds)
        .withRequestTimeout(30 seconds)
        .resource

    private def blocker[F[_]: CE]: Resource[F, Blocker] = {
      val es: ExecutorService = Executors.newSingleThreadExecutor(
        new ThreadFactory {
          def newThread(r: Runnable) = {
            val thread = new Thread(r, "loader-blocker")
            thread.setDaemon(false)
            thread
          }
        }
      )

      Blocker.fromExecutorService(CE[F].delay(es))
    }
}
