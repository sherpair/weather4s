package io.sherpair.geo.app

import cats.effect.Sync
import io.sherpair.geo.cache.CacheRef
import io.sherpair.geo.engine.Engine
import org.http4s.HttpRoutes

class Application[F[_]: Sync](cacheRef: CacheRef[F])(implicit engine: Engine[F]) {

  def routes: F[Seq[HttpRoutes[F]]] =
    Sync[F].delay(
      Seq(
        new CountryApp[F](cacheRef).routes,
        new Monitoring[F].routes
      )
    )
}
