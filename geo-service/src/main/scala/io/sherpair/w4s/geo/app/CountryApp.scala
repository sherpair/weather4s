package io.sherpair.w4s.geo.app

import cats.effect.{ConcurrentEffect, Sync}
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.chrisdavenport.log4cats.Logger
import io.circe.Json
import io.circe.syntax.EncoderOps
import io.sherpair.w4s.domain.{Countries, Country, CountryCount}
import io.sherpair.w4s.geo.cache.CacheRef
import io.sherpair.w4s.geo.config.Configuration
import io.sherpair.w4s.geo.http.Loader
import org.http4s.{HttpRoutes, Response}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

class CountryApp[F[_]: ConcurrentEffect: Logger](cacheRef: CacheRef[F])(implicit C: Configuration) extends Http4sDsl[F] {

  implicit val countryEncoder = jsonEncoderOf[F, Country]
  implicit val countryCountEncoder = jsonEncoderOf[F, CountryCount]

  def routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "countries" => Ok(count)
    case GET -> Root / "countries" / "available" => Ok(availableCountries)
    case GET -> Root / "countries" / "not-available-yet" => Ok(countriesNotAvailableYet)
    case GET -> Root / "country" / id => findCountry(id).flatMap(_.fold(NotFound())(Ok(_)))
    case PUT -> Root / "country" / id => addCountry(id)
  }

  private def addCountry(id: String): F[Response[F]] = {
    val res = for {
      maybeCountry <- if (id.length == 2) cacheRef.countryByCode(id) else cacheRef.countryByName(id)
      response <- cacheRef.countriesNotAvailableYet.map(addCountryToEngineIfNotAvailableYet(_, maybeCountry))
    } yield response

    Sync[F].flatten(res)
  }

  private def addCountryToEngineIfNotAvailableYet(
      countriesNotAvailableYet: Countries, maybeCountry: Option[Country]
  ): F[Response[F]] =
    maybeCountry.map(country => countriesNotAvailableYet.find(_.code == country.code) match {
      case Some(country) => Loader(country)
      case _ => Conflict("Country already available")
    }).getOrElse(NotFound())

  private def availableCountries: F[Json] = cacheRef.availableCountries.map(countries => countries.asJson)

  private def count: F[Json] = cacheRef.countryCount.map(_.asJson)

  private def countriesNotAvailableYet: F[Json] =
    cacheRef.countriesNotAvailableYet.map(countries => countries.asJson)

  private def findCountry(id: String): F[Option[Country]] =
    for {
      country <-
        if (id.length == 2) cacheRef.countryByCode(id)
        else cacheRef.countryByName(id)
    } yield country
}
