package io.sherpair.w4s.geo

import scala.concurrent.ExecutionContext.global

import cats.effect.{ConcurrentEffect => CE, ContextShift => CS, Fiber, Resource, Timer}
import io.sherpair.w4s.domain.Logger
import io.sherpair.w4s.engine.Engine
import io.sherpair.w4s.geo.app.Routes
import io.sherpair.w4s.geo.cache.{Cache, CacheHandler, CacheRef}
import io.sherpair.w4s.geo.config.GeoConfig
import io.sherpair.w4s.geo.engine.EngineOps
import io.sherpair.w4s.http.{maybeWithSSLContext, HttpServer}
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.server.Server

object CallGraph {

  type CallGraphRes[F[_]] = Resource[F, (Cache, Fiber[F, Unit], Server[F])]

  def apply[F[_]: CE: CS: Logger: Timer](engineR: Resource[F, Engine[F]])(implicit C: GeoConfig): CallGraphRes[F] =
    for {
      implicit0(engine: Engine[F]) <- engineR
      implicit0(engineOps: EngineOps[F]) <- Resource.liftF(EngineOps[F](C.clusterName))
      countriesCache <- Resource.make(engineOps.init)(_ => engineOps.close)

      cacheRef <- Resource.make(CacheRef[F](countriesCache))(_.stopCacheHandler)
      cacheHandlerFiber <- Resource.liftF(CacheHandler[F](cacheRef, engineOps, C.cacheHandlerInterval))

      client <- BlazeClientBuilder[F](global).resource

      routes <- Routes[F](cacheRef, client, engineOps)
      sslContextO <- maybeWithSSLContext[F]
      server <- HttpServer[F](routes, sslContextO)
    }
    yield (countriesCache, cacheHandlerFiber, server)
}
