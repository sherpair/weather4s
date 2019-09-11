package io.sherpair.geo.engine.memory

import cats.ApplicativeError
import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.syntax.applicative._
import cats.syntax.apply._
import cats.syntax.functor._
import io.sherpair.geo.countryUnderTest
import io.sherpair.geo.domain.{BulkError, Countries, Country, GeoError}
import io.sherpair.geo.engine.EngineCountry

class MemoryEngineCountry[F[_]](ref: Ref[F, Map[String, Country]])(
  implicit ae: ApplicativeError[F, Throwable]
) extends EngineCountry[F] {

  // This method should be never called, as countries are always retrieved via cache.
  // Only used to retrieve the Meta record.
  override def getById(id: String): F[Option[Country]] =
    ae.raiseError(GeoError(s"Bug in CountryEngineIndex: getById should never be called"))

  override def loadAll(sortBy: Option[Seq[String]], windowSize: Int): F[Countries] = ref.get.map(_.values.toList)

  override def saveAll(countries: Countries): F[List[BulkError]] =
    ref.set(countries.foldLeft(Map[String, Country]()) { (map, country) =>
      map + (country.code -> country) + (country.name -> country)
    }) *> List.empty.pure

  override def upsert(country: Country): F[String] =
    ref.update(_ + (country.code -> country) + (country.name -> country)) *> "OK".pure

  private[memory] def refCount: F[Long] = ref.get.map(_.size.toLong)
}

object MemoryEngineCountry {
  def apply[F[_]: Sync]: F[MemoryEngineCountry[F]] =
    Ref.of[F, Map[String, Country]](Map.empty).map(new MemoryEngineCountry[F](_))
}

object MemoryEngineCountryWithFailingSaveAll {
  def apply[F[_]: Sync]: F[MemoryEngineCountry[F]] =
    Ref.of[F, Map[String, Country]](Map.empty).map(new MemoryEngineCountry[F](_) {
      override def saveAll(countries: Countries): F[List[BulkError]] =
        Sync[F].delay(List(BulkError(countryUnderTest.code, s"Error while saving ${countryUnderTest.name}")))
    })
}
