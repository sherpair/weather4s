package io.sherpair.geo.cache

import cats.effect.{Resource, Sync}
import cats.effect.concurrent.Ref
import cats.syntax.functor._
import io.sherpair.geo.domain.{epochAsLong, Countries}

case class Cache(lastCacheRenewal: Long, countries: Countries)

trait CacheRef[F[_]] {
  def cacheRenewal(cache: Cache): F[Unit]
  def findAvailableCountries: F[Countries]
  def findCountriesNotAvailableYet: F[Countries]
  def lastCacheRenewal: F[Long]
}

object CacheRef {

  def apply[F[_]: Sync](cache: Cache): Resource[F, CacheRef[F]] =
    Resource.liftF(
      Ref.of[F, Cache](cache).map { ref: Ref[F, Cache] =>
        new CacheRef[F] {
          def cacheRenewal(cache: Cache): F[Unit] = ref.set(cache)
          def findAvailableCountries: F[Countries] = ref.get.map(_.countries.filter(_.updated != epochAsLong))
          def findCountriesNotAvailableYet: F[Countries] = ref.get.map(_.countries.filter(_.updated == epochAsLong))
          def lastCacheRenewal: F[Long] = ref.get.map(_.lastCacheRenewal)
        }
      }
    )
}
