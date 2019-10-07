package io.sherpair.w4s.geo

import scala.concurrent.ExecutionContext.global

import cats.effect.{ConcurrentEffect, ContextShift, Fiber, Resource, Timer}
import io.sherpair.w4s.domain.Logger
import io.sherpair.w4s.engine.Engine
import io.sherpair.w4s.geo.app.Routes
import io.sherpair.w4s.geo.cache.{Cache, CacheHandler, CacheRef}
import io.sherpair.w4s.geo.config.GeoConfig
import io.sherpair.w4s.geo.engine.EngineOps
import io.sherpair.w4s.geo.http.GeoServer
import org.http4s.client.blaze.BlazeClientBuilder

object Resources {

  def apply[F[_]: ContextShift: Engine: Logger: Timer](
      implicit C: GeoConfig, CE: ConcurrentEffect[F]): Resource[F, (Cache, Fiber[F, Unit], Fiber[F, Unit])] =
    for {
      implicit0(engineOps: EngineOps[F]) <- Resource.liftF(EngineOps[F](C.clusterName))
      countriesCache <- Resource.make(engineOps.init)(_ => engineOps.close)
      cacheRef <- Resource.make(CacheRef[F](countriesCache))(_.stopCacheHandler)
      cacheHandlerFiber <- Resource.liftF(CacheHandler[F](cacheRef, engineOps, C.cacheHandlerInterval))
      client <- BlazeClientBuilder[F](global).resource
      routes <- Routes[F](cacheRef, client, engineOps)
      httpServerFiber <- GeoServer[F](C.httpGeo.host, routes)
    }
    yield (countriesCache, cacheHandlerFiber, httpServerFiber)
}
