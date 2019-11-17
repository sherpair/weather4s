package io.sherpair.w4s.loader.app

import cats.effect.{ConcurrentEffect => CE, Fiber, Resource}
import fs2.concurrent.NoneTerminatedQueue
import io.sherpair.w4s.auth.{jwtAlgorithm, loadPublicRsaKey, Authoriser, Claims}
import io.sherpair.w4s.domain.{Country, DataForAuthorisation, Logger}
import io.sherpair.w4s.domain.Role.Master
import io.sherpair.w4s.engine.Engine
import io.sherpair.w4s.loader.config.LoaderConfig
import io.sherpair.w4s.loader.engine.EngineOps
import org.http4s.HttpRoutes

object Routes {

  def apply[F[_]: CE](
      countryQueue: NoneTerminatedQueue[F, Country], loaderFiber: Fiber[F, Unit])(
      implicit C: LoaderConfig, engine: Engine[F], engineOps: EngineOps[F], L: Logger[F]
  ): Resource[F, Seq[HttpRoutes[F]]] =
    for {
      jwtAlgorithm <- Resource.liftF(jwtAlgorithm)

      publicKey <- Resource.liftF(loadPublicRsaKey)

      routes <- Resource.liftF(CE[F].delay {

        val dfa = DataForAuthorisation(jwtAlgorithm, publicKey)
        val masterAuthoriser = Authoriser[F](dfa, Claims.audLoader, _.role == Master)

        Seq(
          new CountryApp[F](masterAuthoriser, countryQueue, loaderFiber).routes,
          new Monitoring[F](masterAuthoriser).routes
        )
      })
    }
    yield routes
}
