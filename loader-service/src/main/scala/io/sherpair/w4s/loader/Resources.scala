package io.sherpair.w4s.loader

import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration._

import cats.effect.{ConcurrentEffect, ContextShift, Resource, Timer}
import fs2.concurrent.Queue
import io.sherpair.w4s.domain.{Country, Logger}
import io.sherpair.w4s.engine.Engine
import io.sherpair.w4s.loader.app.{Loader, Routes}
import io.sherpair.w4s.loader.config.LoaderConfig
import io.sherpair.w4s.loader.domain.LoaderContext
import io.sherpair.w4s.loader.engine.EngineOps
import io.sherpair.w4s.loader.http.LoaderServer
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.server.Server

object Resources {

  def apply[F[_]: ConcurrentEffect: ContextShift: Engine: Logger: Timer](
    implicit C: LoaderConfig, CL: LoaderContext[F]
  ): Resource[F, Server[F]] =
    for {
      implicit0(engineOps: EngineOps[F]) <- Resource.liftF(EngineOps[F](C.clusterName))
      _ <- Resource.make(engineOps.init)(_ => engineOps.close)
      countryQueue <- Resource.liftF(Queue.boundedNoneTerminated[F, Country](C.maxEnqueuedCountries))
      client <- blazeClient
      loaderFiber <- Loader(client, countryQueue)
      routes <- Routes[F](countryQueue, loaderFiber)
      server <- LoaderServer[F](C.httpLoader.host, routes)
    }
    yield server

    private def blazeClient[F[_]: ConcurrentEffect]: Resource[F, Client[F]] =
      BlazeClientBuilder[F](global)
        .withConnectTimeout(30 seconds)
        .withRequestTimeout(30 seconds)
        .resource
}
