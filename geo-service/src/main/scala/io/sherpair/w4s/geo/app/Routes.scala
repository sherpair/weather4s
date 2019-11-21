package io.sherpair.w4s.geo.app

import cats.effect.{Blocker, ConcurrentEffect => CE, ContextShift => CS, Resource}
import cats.syntax.semigroupk._
import io.sherpair.w4s.auth.{jwtAlgorithm, Authoriser, Claims}
import io.sherpair.w4s.domain.{blockerForIOtasks, Logger}
import io.sherpair.w4s.engine.Engine
import io.sherpair.w4s.geo.cache.CacheRef
import io.sherpair.w4s.geo.config.GeoConfig
import io.sherpair.w4s.geo.engine.EngineOps
import org.http4s.HttpRoutes
import org.http4s.client.Client

object Routes {

  def apply[F[_]: CE: CS: Logger](
      cacheRef: CacheRef[F], client: Client[F], engineOps: EngineOps[F])(
      implicit C: GeoConfig, E: Engine[F]
  ): Resource[F, HttpRoutes[F]] =
    for {
      implicit0(blocker: Blocker) <- blockerForIOtasks
      jwtAlgorithm <- Resource.liftF(jwtAlgorithm)
      authoriser <- Resource.liftF(Authoriser[F](Claims.audAuth, jwtAlgorithm))

      routes <- Resource.liftF(CE[F].delay {
        // For performance reason no authoriser for the "suggest" endpoint.
        // Still, to benchmark with and without when the frontend is ready.
        new SuggestApp[F](cacheRef, engineOps).routes <+>
        authoriser(new CountryApp[F](cacheRef, client).routes) <+>
        authoriser(new Monitoring[F].routes)
      })
    }
    yield routes
}
