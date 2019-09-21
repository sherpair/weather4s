package io.sherpair.w4s.geo

import cats.effect.{ConcurrentEffect, ContextShift, Fiber, Resource, Timer}
import cats.effect.syntax.concurrent._
import io.chrisdavenport.log4cats.Logger
import io.sherpair.w4s.config.Engine.clusterName
import io.sherpair.w4s.engine.Engine
import io.sherpair.w4s.geo.app.Application
import io.sherpair.w4s.geo.cache.{Cache, CacheHandler, CacheRef}
import io.sherpair.w4s.geo.config.Configuration
import io.sherpair.w4s.geo.engine.EngineOps
import io.sherpair.w4s.geo.http.GeoServer

class Resources[F[_]: ContextShift: Engine: Logger: Timer](implicit CE: ConcurrentEffect[F]) {

  def describe(implicit C: Configuration): Resource[F, (Cache, Fiber[F, Unit], Fiber[F, Unit])] =
    for {
      implicit0(engineOps: EngineOps[F]) <- Resource.liftF(CE.delay(EngineOps[F](clusterName(C.engine))))
      countriesCache <- Resource.make(engineOps.init)(_ => engineOps.close)
      cacheRef <- Resource.make(CacheRef[F](countriesCache))(_.stopCacheHandler)
      cacheHandlerFiber <- Resource.liftF(CacheHandler.describe[F](cacheRef, engineOps, C.cacheHandlerInterval).start)
      routes <- Resource.liftF(new Application[F](cacheRef).routes)
      httpServerFiber <- Resource.liftF(GeoServer.describe[F](C.httpGeo.host, routes))
    } yield (countriesCache, cacheHandlerFiber, httpServerFiber)
}
