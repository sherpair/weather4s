package io.sherpair.geo.cache

import cats.effect.{Resource, Sync}
import cats.effect.concurrent.Ref
import cats.syntax.functor._
import io.sherpair.geo.domain.{epochAsLong, Countries, Country, CountryCount}

case class Cache(lastCacheRenewal: Long, countries: Countries)

trait CacheRef[F[_]] {
  def availableCountries: F[Countries]
  def cacheRenewal(cache: Cache): F[Unit]
  def countriesNotAvailableYet: F[Countries]
  def countryByCode(code: String): F[Option[Country]]
  def countryByName(name: String): F[Option[Country]]
  def countryCount: F[CountryCount]
  def lastCacheRenewal: F[Long]
}

object CacheRef {

  def apply[F[_]: Sync](cache: Cache): Resource[F, CacheRef[F]] =
    Resource.liftF(
      Ref.of[F, Cache](cache).map { ref: Ref[F, Cache] =>
        new CacheRef[F] {
          def availableCountries: F[Countries] = ref.get.map(_.countries.filter(_.updated != epochAsLong))
          def cacheRenewal(cache: Cache): F[Unit] = ref.set(cache)
          def countriesNotAvailableYet: F[Countries] = ref.get.map(_.countries.filter(_.updated == epochAsLong))
          def countryByCode(code: String): F[Option[Country]] = {
            val codeUC = code.toUpperCase
            ref.get.map(_.countries.find(_.code == codeUC))
          }
          def countryByName(name: String): F[Option[Country]] = {
            val nameLC = name
            ref.get.map(_.countries.find(_.name.toLowerCase == nameLC))
          }
          def countryCount: F[CountryCount] = ref.get.map(cache => CountryCount(cache.countries))
          def lastCacheRenewal: F[Long] = ref.get.map(_.lastCacheRenewal)
        }
      }
    )
}
