package io.sherpair.w4s.geo.cache

import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.syntax.functor._
import io.sherpair.w4s.domain.{epochAsLong, leftUnit, rightUnit, Country, CountryCount}
import io.sherpair.w4s.types.Countries

case class Cache(
  lastCacheRenewal: Long, countries: Countries, cacheHandlerStopFlag: Either[Unit, Unit] = leftUnit
)

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
        override def availableCountries: F[Countries] = ref.get.map(_.countries.filter(_.updated > epochAsLong))
        override def cacheHandlerStopFlag: F[Either[Unit, Unit]] = ref.get.map(_.cacheHandlerStopFlag)
        override def cacheRenewal(cache: Cache): F[Unit] = ref.set(cache)
        override def countriesNotAvailableYet: F[Countries] = ref.get.map(_.countries.filter(_.updated <= epochAsLong))
        override def countryByCode(code: String): F[Option[Country]] = {
          val codeUC = code.toLowerCase
          ref.get.map(_.countries.find(_.code == codeUC))
        }
        override def countryByName(name: String): F[Option[Country]] = {
          val nameLC = name.toLowerCase
          ref.get.map(_.countries.find(_.name.toLowerCase == nameLC))
        }
        override def countryCount: F[CountryCount] = ref.get.map(cache => CountryCount(cache.countries))
        override def lastCacheRenewal: F[Long] = ref.get.map(_.lastCacheRenewal)
        override def stopCacheHandler: F[Unit] = ref.update(_.copy(cacheHandlerStopFlag = rightUnit))
      }
    }
}
