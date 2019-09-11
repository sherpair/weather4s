package io.sherpair.geo.app

import cats.effect.Sync
import cats.syntax.applicative._
import io.sherpair.geo.cache.CacheRef
import io.sherpair.geo.engine.Engine
import org.http4s.HttpRoutes

class Application[F[_]: Sync](cacheRef: CacheRef[F])(implicit engine: Engine[F]) {

  def routes: F[Seq[HttpRoutes[F]]] =
    Seq(
      new CountryApp[F](cacheRef).routes,
      new Monitoring[F].routes
    ).pure[F]
}
