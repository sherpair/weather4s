package io.sherpair.geo.cache

import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.syntax.either._
import cats.syntax.functor._
import io.sherpair.geo.domain.{epochAsLong, unit, Countries, Country, CountryCount}

case class Cache(lastCacheRenewal: Long, countries: Countries, cacheHandlerStopFlag: Either[Unit, Unit] = unit.asLeft[Unit])

trait CacheRef[F[_]] {
  def availableCountries: F[Countries]
  def cacheHandlerStopFlag: F[Either[Unit, Unit]]
  def cacheRenewal(cache: Cache): F[Unit]
  def countriesNotAvailableYet: F[Countries]
  def countryByCode(code: String): F[Option[Country]]
  def countryByName(name: String): F[Option[Country]]
  def countryCount: F[CountryCount]
  def lastCacheRenewal: F[Long]
  def stopCacheHandler: F[Unit]
}

object CacheRef {

  def apply[F[_]: Sync](cache: Cache): F[CacheRef[F]] =
    Ref.of[F, Cache](cache).map { ref: Ref[F, Cache] =>
      new CacheRef[F] {
        def availableCountries: F[Countries] = ref.get.map(_.countries.filter(_.updated != epochAsLong))
        def cacheHandlerStopFlag: F[Either[Unit, Unit]] = ref.get.map(_.cacheHandlerStopFlag)
        def cacheRenewal(cache: Cache): F[Unit] = ref.set(cache)
        def countriesNotAvailableYet: F[Countries] = ref.get.map(_.countries.filter(_.updated == epochAsLong))
        def countryByCode(code: String): F[Option[Country]] = {
          val codeUC = code.toUpperCase
          ref.get.map(_.countries.find(_.code == codeUC))
        }
        def countryByName(name: String): F[Option[Country]] = {
          val nameLC = name.toLowerCase
          ref.get.map(_.countries.find(_.name.toLowerCase == nameLC))
        }
        def countryCount: F[CountryCount] = ref.get.map(cache => CountryCount(cache.countries))
        def lastCacheRenewal: F[Long] = ref.get.map(_.lastCacheRenewal)
        def stopCacheHandler: F[Unit] = ref.update(_.copy(cacheHandlerStopFlag = unit.asRight[Unit]))
      }
    }
}
