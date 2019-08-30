package io.sherpair.geo.cache

import cats.Monad
import cats.effect.{Concurrent, Fiber, Resource, Sync, Timer}
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.chrisdavenport.log4cats.Logger
import io.sherpair.geo.config.Configuration
import io.sherpair.geo.domain.{unit, Country, Meta}
import io.sherpair.geo.engine.EngineOps

class CacheHandler[F[_]: Logger: Sync: Timer] private (cacheRef: CacheRef[F])(
    implicit config: Configuration,
    engineOps: EngineOps[F]
) {

  def start: F[Unit] =
    Monad[F].tailRecM[Unit, Unit](unit) { _ =>
      for {
        _ <- Timer[F].sleep(config.cacheHandlerInterval)
        _ <- checkIfCacheRenewalIsNeeded
      } yield Left(unit)
    }

  private def checkIfCacheRenewalIsNeeded: F[Unit] =
    for {
      meta <- engineOps.loadMeta
      lastCacheRenewal <- cacheRef.lastCacheRenewal
      _ <- checkIfCacheRenewalIsNeeded(meta, lastCacheRenewal)
    } yield unit

  private def checkIfCacheRenewalIsNeeded(meta: Meta, lastCacheRenewal: Long): F[Unit] =
    if (meta.lastEngineUpdate <= lastCacheRenewal) Sync[F].unit
    else cacheRenewal(meta)

  private def cacheRenewal(meta: Meta): F[Unit] =
    for {
      countries <- engineOps.loadCountries
      _ <- cacheRef.cacheRenewal(Cache(meta.lastEngineUpdate, countries))
      _ <- meta.logLastEngineUpdate
      _ <- Country.logCountOfCountries[F](countries)
    } yield unit
}

object CacheHandler {

  def describe[F[_]: Concurrent: EngineOps: Logger: Sync: Timer](
      cacheRef: CacheRef[F]
  )(implicit C: Configuration): Resource[F, Fiber[F, Unit]] =
    Resource.liftF(Concurrent[F].start[Unit](new CacheHandler[F](cacheRef).start))
}
