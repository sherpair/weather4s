package io.sherpair.geo

import cats.effect.{ConcurrentEffect, ContextShift, Fiber, Resource, Timer}
import cats.effect.syntax.concurrent._
import io.chrisdavenport.log4cats.Logger
import io.sherpair.geo.app.Application
import io.sherpair.geo.cache.{Cache, CacheHandler, CacheRef}
import io.sherpair.geo.config.Configuration
import io.sherpair.geo.config.Configuration.clusterName
import io.sherpair.geo.engine.{Engine, EngineOps}
import io.sherpair.geo.http.GeoServer

class Resources[F[_]: ConcurrentEffect: ContextShift: Engine: Logger: Timer] {

  def describe(implicit config: Configuration): Resource[F, (Cache, Fiber[F, Unit], Fiber[F, Unit])] =
    for {
      implicit0(engineOps: EngineOps[F]) <- Resource.liftF(EngineOps[F](clusterName(config)))
      countriesCache <- Resource.make(engineOps.init)(_ => engineOps.close)
      cacheRef <- Resource.make(CacheRef[F](countriesCache))(_.stopCacheHandler)
      cacheHandlerFiber <- Resource.liftF(CacheHandler.describe[F](cacheRef, engineOps, config.cacheHandlerInterval).start)
      routes <- Resource.liftF(new Application[F](cacheRef).routes)
      httpServerFiber <- Resource.liftF(GeoServer.describe[F](config.http.host, routes))
    } yield (countriesCache, cacheHandlerFiber, httpServerFiber)
}
