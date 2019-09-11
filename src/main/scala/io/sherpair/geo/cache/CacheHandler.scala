package io.sherpair.geo.cache

import scala.concurrent.duration.FiniteDuration

import cats.effect.{Sync, Timer}
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.chrisdavenport.log4cats.Logger
import io.sherpair.geo.domain.{epochAsLong, unit, Countries, Country, Meta}
import io.sherpair.geo.engine.EngineOps

class CacheHandler[F[_]: Sync: Timer] private(cacheRef: CacheRef[F], engineOps: EngineOps[F], cacheHandlerInterval: FiniteDuration)(
  implicit L: Logger[F]
) {

  def start: F[Unit] =
    L.info("Starting CacheHandler") *>
      Sync[F].tailRecM[Unit, Unit](unit) { _ =>
        for {
          _ <- Timer[F].sleep(cacheHandlerInterval)
          _ <- checkIfCacheRenewalIsNeeded
          cacheHandlerStopFlag <- cacheRef.cacheHandlerStopFlag
        } yield cacheHandlerStopFlag
      }

  private def checkIfCacheRenewalIsNeeded: F[Unit] =
    for {
      maybeMeta <- engineOps.loadMeta
      lastCacheRenewal <- cacheRef.lastCacheRenewal
      _ <- checkIfCacheRenewalIsNeeded(maybeMeta, lastCacheRenewal)
    } yield unit

  private def checkIfCacheRenewalIsNeeded(maybeMeta: Option[Meta], lastCacheRenewal: Long): F[Unit] =
    maybeMeta.fold(L.error("Meta record not found!! Cannot do a cache renewal")) { meta =>
      if (meta.lastEngineUpdate <= lastCacheRenewal) Sync[F].unit
      else cacheRenewal(meta)
    }

  private def cacheRenewal(meta: Meta): F[Unit] =
    for {
      countries <- engineOps.loadCountries
      cacheHandlerStopFlag <- cacheRef.cacheHandlerStopFlag
      _ <- cacheRef.cacheRenewal(Cache(meta.lastEngineUpdate, countries, cacheHandlerStopFlag))
      _ <- meta.logLastEngineUpdate
      _ <- logCountOfCountries(countries)
    } yield unit

  private def logCountOfCountries(countries: Countries): F[Unit] = {
    val size = countries.size
    if (size < Country.numberOfCountries) L.error(Country.requirement)
    val loadedFromUser = countries.count(_.updated != epochAsLong)
    L.info(s"Countries(${size}):  uploaded(${loadedFromUser}),  not-uploaded-yet(${size - loadedFromUser})")
  }
}

object CacheHandler {

  def describe[F[_]: Logger: Sync: Timer](cacheRef: CacheRef[F], engineOps: EngineOps[F], cacheHandlerInterval: FiniteDuration): F[Unit] =
    new CacheHandler[F](cacheRef, engineOps, cacheHandlerInterval).start
}
