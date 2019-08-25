package io.sherpair.geo.cache

import cats.Monad
import cats.effect.{Concurrent, Fiber, Resource, Sync, Timer}
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.chrisdavenport.log4cats.Logger
import io.sherpair.geo.config.Configuration
import io.sherpair.geo.domain.{unit, Country, EngineMeta}
import io.sherpair.geo.engine.EngineOps

class CacheHandler[F[_]: Logger: Sync: Timer](cacheRef: CacheRef[F])(implicit config: Configuration, engineOps: EngineOps[F]) {

  def start: F[Unit] =
    Monad[F].tailRecM[Unit, Unit](unit) { _ =>
      for {
        _ <- Timer[F].sleep(config.cacheHandlerInterval)
        _ <- checkIfCacheRenewalIsNeeded
      } yield Left(unit)
    }

  private def checkIfCacheRenewalIsNeeded: F[Unit] =
    for {
      engineMeta <- engineOps.loadEngineMeta
      lastCacheRenewal <- cacheRef.lastCacheRenewal
      _ <- checkIfCacheRenewalIsNeeded(engineMeta, lastCacheRenewal)
    } yield Left(unit)

  private def checkIfCacheRenewalIsNeeded(engineMeta: EngineMeta, lastCacheRenewal: Long): F[Unit] =
    if (engineMeta.lastEngineUpdate <= lastCacheRenewal) Sync[F].unit
    else cacheRenewal(engineMeta.lastEngineUpdate)

  private def cacheRenewal(lastEngineUpdate: Long): F[Unit] =
    for {
      countries <- engineOps.loadCountries
      _ <- cacheRef.cacheRenewal(Cache(lastEngineUpdate, countries))
      _ <- EngineMeta.logLastEngineUpdate(lastEngineUpdate)
      _ <- Country.logCountOfCountries[F](countries)
    } yield unit
}

object CacheHandler {

  def describe[F[_]: Concurrent: EngineOps: Logger: Sync: Timer](
      cacheRef: CacheRef[F]
  )(implicit C: Configuration): Resource[F, Fiber[F, Unit]] =
    Resource.liftF(Concurrent[F].start[Unit](new CacheHandler[F](cacheRef).start))
}
