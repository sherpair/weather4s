package io.sherpair.w4s.loader.app

import cats.effect.{ConcurrentEffect, Fiber, Resource}
import cats.syntax.applicative._
import fs2.concurrent.NoneTerminatedQueue
import io.sherpair.w4s.domain.{Country, Logger}
import io.sherpair.w4s.engine.Engine
import io.sherpair.w4s.loader.config.LoaderConfig
import io.sherpair.w4s.loader.engine.EngineOps
import org.http4s.HttpRoutes

object Routes {

  def apply[F[_]: ConcurrentEffect: Engine: EngineOps: Logger](
      countryQueue: NoneTerminatedQueue[F, Country], loaderFiber: Fiber[F, Unit])(implicit C: LoaderConfig
  ): Resource[F, Seq[HttpRoutes[F]]] =
    Resource.liftF(
      Seq(
        new CountryApp[F](countryQueue, loaderFiber).routes,
        new Monitoring[F].routes
      ).pure[F]
    )
}
