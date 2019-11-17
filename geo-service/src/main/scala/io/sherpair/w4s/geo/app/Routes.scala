package io.sherpair.w4s.geo.app

import cats.effect.{ConcurrentEffect => CE, Resource}
import io.sherpair.w4s.auth.{jwtAlgorithm, loadPublicRsaKey, Authoriser, Claims}
import io.sherpair.w4s.domain.{DataForAuthorisation, Logger}
import io.sherpair.w4s.domain.Role.Master
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

      routes <- Resource.liftF(CE[F].delay {

        val dfa = DataForAuthorisation(jwtAlgorithm, publicKey)
        val masterAuthoriser = Authoriser[F](dfa, Claims.audGeo, _.role == Master)
        val memberAuthoriser = Authoriser(dfa, Claims.audGeo)

        Seq(
          new CountryApp[F](memberAuthoriser, cacheRef, client).routes,
          new Monitoring[F](masterAuthoriser).routes,
          new SuggestApp[F](cacheRef, engineOps).routes
        )
      })
    }
    yield routes
}
