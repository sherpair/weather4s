package io.sherpair.w4s.geo.app

import cats.effect.{Resource, ConcurrentEffect => CE}
import io.sherpair.w4s.auth.{jwtAlgorithm, loadPublicRsaKey}
import io.sherpair.w4s.domain.{AuthData, Logger}
import io.sherpair.w4s.engine.Engine
import io.sherpair.w4s.geo.cache.CacheRef
import io.sherpair.w4s.geo.config.GeoConfig
import io.sherpair.w4s.geo.engine.EngineOps
import org.http4s.HttpRoutes
import org.http4s.client.Client

object Routes {

  def apply[F[_]: CE](
      cacheRef: CacheRef[F], client: Client[F], engineOps: EngineOps[F])(
      implicit C: GeoConfig, E: Engine[F], L: Logger[F]
  ): Resource[F, Seq[HttpRoutes[F]]] =
    for {
      jwtAlgorithm <- Resource.liftF(jwtAlgorithm)

      publicKey <- Resource.liftF(loadPublicRsaKey)
      authData = AuthData(jwtAlgorithm, publicKey)

      routes <- Resource.liftF(CE[F].delay(
        Seq(
          new CountryApp[F](authData, cacheRef, client).routes,
          new Monitoring[F](authData).routes,
          new SuggestApp[F](cacheRef, engineOps).routes,
        )
      ))
    }
    yield routes
}
