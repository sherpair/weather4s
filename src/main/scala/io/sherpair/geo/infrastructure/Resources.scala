package io.sherpair.geo.infrastructure

import cats.effect.{ConcurrentEffect, ContextShift, Resource, Timer}
import io.chrisdavenport.log4cats.Logger
import io.sherpair.geo.cache.{Cache, CacheRef}
import io.sherpair.geo.config.Configuration
import io.sherpair.geo.engine.{Engine, EngineOps}
import org.http4s.server.Server

class Resources[F[_]: ConcurrentEffect: ContextShift: Engine: Logger: Timer](implicit C: Configuration) {

  def describe: Resource[F, (Cache, Server[F])] = {
    val engineOps = new EngineOps[F]

    for {
      res1 <- Resource.make(engineOps.init)(_ => engineOps.close)
      cacheRef <- Resource.liftF(CacheRef.create(res1))
      res2 <- GeoServer.describe[F](cacheRef)
    } yield (res1, res2)
  }
}
