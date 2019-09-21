package io.sherpair.w4s.geo.app

import cats.effect.ConcurrentEffect
import cats.syntax.applicative._
import io.chrisdavenport.log4cats.Logger
import io.sherpair.w4s.engine.Engine
import io.sherpair.w4s.geo.cache.CacheRef
import io.sherpair.w4s.geo.config.Configuration
import org.http4s.HttpRoutes

class Application[F[_]: ConcurrentEffect: Engine: Logger](cacheRef: CacheRef[F])(implicit C: Configuration) {

  def routes: F[Seq[HttpRoutes[F]]] =
    Seq(
      new CountryApp[F](cacheRef).routes,
      new Monitoring[F].routes
    ).pure[F]
}
