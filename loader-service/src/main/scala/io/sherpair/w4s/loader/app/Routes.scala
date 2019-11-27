package io.sherpair.w4s.loader.app

import cats.effect.{Blocker, ConcurrentEffect => CE, ContextShift => CS, Fiber, Resource}
import cats.syntax.semigroupk._
import fs2.concurrent.NoneTerminatedQueue
import io.sherpair.w4s.auth.{jwtAlgorithm, Authoriser, Claims}
import io.sherpair.w4s.domain.{Country, Logger}
import io.sherpair.w4s.engine.Engine
import io.sherpair.w4s.http.ApiApp
import io.sherpair.w4s.loader.config.LoaderConfig
import io.sherpair.w4s.loader.engine.EngineOps
import org.http4s.HttpRoutes

object Routes {

  def apply[F[_]: CE: CS: Logger](
      countryQueue: NoneTerminatedQueue[F, Country], loaderFiber: Fiber[F, Unit])(
      implicit B: Blocker, C: LoaderConfig, engine: Engine[F], engineOps: EngineOps[F]
  ): Resource[F, HttpRoutes[F]] =
    for {
      jwtAlgorithm <- Resource.liftF(jwtAlgorithm)
      authoriser <- Resource.liftF(Authoriser[F](Claims.audAuth, jwtAlgorithm))

      routes <- Resource.liftF(CE[F].delay {
        new ApiApp[F].routes <+>
        authoriser(new CountryApp[F](countryQueue, loaderFiber).routes) <+>
        authoriser(new Monitoring[F].routes)
      })
    }
    yield routes
}
