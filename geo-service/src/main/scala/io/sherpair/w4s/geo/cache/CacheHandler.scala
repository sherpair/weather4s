package io.sherpair.w4s.geo.cache

import scala.concurrent.duration.FiniteDuration

import cats.effect.{Concurrent, Fiber, Sync, Timer}
import cats.syntax.applicative._
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.sherpair.w4s.domain.{epochAsLong, unit, Country, Logger, Meta}
import io.sherpair.w4s.geo.engine.EngineOps
import io.sherpair.w4s.types.Countries

class CacheHandler[F[_]: Sync: Timer] (
    cacheRef: CacheRef[F], engineOps: EngineOps[F], cacheHandlerInterval: FiniteDuration)(implicit L: Logger[F]
) {

  def start: F[Unit] =
    L.info("Starting CacheHandler") *>
      Sync[F].tailRecM[Unit, Unit](unit) { _ =>
        Timer[F].sleep(cacheHandlerInterval) >> checkIfCacheRenewalIsNeeded >> cacheRef.cacheHandlerStopFlag
      }

  private def checkIfCacheRenewalIsNeeded: F[Unit] =
    for {
      maybeMeta <- engineOps.loadMeta
      lastCacheRenewal <- cacheRef.lastCacheRenewal
      _ <- checkIfCacheRenewalIsNeeded(maybeMeta, lastCacheRenewal)
    } yield unit

  private def checkIfCacheRenewalIsNeeded(maybeMeta: Option[Meta], lastCacheRenewal: Long): F[Unit] =
    maybeMeta.fold(L.error("Meta record not found!! Cannot do a cache renewal")) { meta =>
      cacheRenewal(meta).whenA(meta.lastEngineUpdate > lastCacheRenewal)
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
    val available = countries.count(_.updated > epochAsLong)
    L.info(s"Countries(${size}):  available(${available}),  not-available-yet(${size - available})")
  }
}

object CacheHandler {

  def apply[F[_]: Concurrent: Timer](
    cacheRef: CacheRef[F], engineOps: EngineOps[F], cacheHandlerInterval: FiniteDuration)(implicit L: Logger[F]
  ): F[Fiber[F, Unit]] =
    Concurrent[F].start(new CacheHandler[F](cacheRef, engineOps, cacheHandlerInterval).start)
}
