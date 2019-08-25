package io.sherpair.geo.infrastructure

import cats.effect.{ConcurrentEffect, ContextShift, Fiber, Resource, Timer}
import io.chrisdavenport.log4cats.Logger
import io.sherpair.geo.cache.{Cache, CacheHandler, CacheRef}
import io.sherpair.geo.config.Configuration
import io.sherpair.geo.engine.{Engine, EngineOps}
import org.http4s.server.Server

class Resources[F[_]: ConcurrentEffect: ContextShift: Engine: Logger: Timer](implicit C: Configuration) {

  def describe: Resource[F, (Cache, Fiber[F, Unit], Server[F])] =
    for {
      implicit0(engineOps: EngineOps[F]) <- EngineOps[F]
      res1 <- Resource.make(engineOps.init)(_ => engineOps.close)
      cacheRef <- CacheRef[F](res1)
      res2 <- CacheHandler.describe[F](cacheRef)
      res3 <- GeoServer.describe[F](cacheRef)
    } yield (res1, res2, res3)
}
