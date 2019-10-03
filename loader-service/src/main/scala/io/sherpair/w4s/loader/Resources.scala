package io.sherpair.w4s.loader

import java.util.concurrent.{Executors, ExecutorService, ThreadFactory}

import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration._

import cats.effect.{Blocker, ConcurrentEffect, ContextShift, Resource, Timer}
import fs2.concurrent.Queue
import io.sherpair.w4s.domain.{Country, Logger}
import io.sherpair.w4s.engine.Engine
import io.sherpair.w4s.loader.app.{Loader, Routes}
import io.sherpair.w4s.loader.config.LoaderConfig
import io.sherpair.w4s.loader.engine.EngineOps
import io.sherpair.w4s.loader.http.LoaderServer
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.server.Server

object Resources {

  def apply[F[_]: ConcurrentEffect: ContextShift: Engine: Logger: Timer](implicit C: LoaderConfig): Resource[F, Server[F]] =
    for {
      implicit0(engineOps: EngineOps[F]) <- Resource.liftF(EngineOps[F](C.clusterName))
      _ <- Resource.make(engineOps.init)(_ => engineOps.close)
      countryQueue <- Resource.liftF(Queue.boundedNoneTerminated[F, Country](C.maxEnqueuedCountries))
      blocker <- blocker[F]
      client <- blazeClient
      loaderFiber <- Loader(blocker, client, countryQueue)
      routes <- Routes[F](countryQueue, loaderFiber)
      server <- LoaderServer[F](C.httpLoader.host, routes)
    }
    yield server

    private def blazeClient[F[_]: ConcurrentEffect]: Resource[F, Client[F]] =
      BlazeClientBuilder[F](global)
        .withConnectTimeout(30 seconds)
        .withRequestTimeout(30 seconds)
        .resource

    private def blocker[F[_]: ConcurrentEffect]: Resource[F, Blocker] = {
      val es: ExecutorService = Executors.newSingleThreadExecutor(
        new ThreadFactory {
          def newThread(r: Runnable) = {
            val thread = new Thread(r, "loader-blocker")
            thread.setDaemon(false)
            thread
          }
        }
      )

      Blocker.fromExecutorService(ConcurrentEffect[F].delay(es))
    }
}
