package io.sherpair.w4s.geo.app

import cats.effect.ConcurrentEffect
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.circe.Json
import io.circe.syntax.EncoderOps
import io.sherpair.w4s.domain.{Countries, Country, CountryCount, Logger}
import io.sherpair.w4s.geo.cache.CacheRef
import io.sherpair.w4s.geo.config.GeoConfig
import io.sherpair.w4s.geo.http.Loader
import org.http4s.{EntityEncoder, HttpRoutes, Response}
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.dsl.Http4sDsl

class CountryApp[F[_]: ConcurrentEffect](
    cacheRef: CacheRef[F], client: Client[F])(implicit C: GeoConfig, L: Logger[F]) extends Http4sDsl[F] {

  implicit val countryEncoder: EntityEncoder[F, Country] = jsonEncoderOf[F, Country]
  implicit val countryCountEncoder = jsonEncoderOf[F, CountryCount]

  def routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "countries" => Ok(count)
    case GET -> Root / "countries" / "available" => Ok(availableCountries)
    case GET -> Root / "countries" / "not-available-yet" => Ok(countriesNotAvailableYet)
    case GET -> Root / "country" / id => findCountry(id) >>= { _.fold(NotFound())(Ok(_)) }
    case PUT -> Root / "country" / id => addCountry(id)

    case GET -> Root / "localities" / id => findCountry(id) >>= {
      _.fold(NotFound())(country => Ok(country.localities.toString))
    }
  }

  private def addCountry(id: String): F[Response[F]] = {
    val response = for {
      maybeCountry <- if (id.length == 2) cacheRef.countryByCode(id.toLowerCase) else cacheRef.countryByName(id)
      response <- cacheRef.countriesNotAvailableYet.map(addCountryToEngineIfNotAvailableYet(_, maybeCountry))
    } yield response

    ConcurrentEffect[F].flatten(response)
  }

  private def addCountryToEngineIfNotAvailableYet(
      countriesNotAvailableYet: Countries, maybeCountry: Option[Country]
  ): F[Response[F]] =
    maybeCountry.map(country => countriesNotAvailableYet.find(_.code == country.code) match {
      case Some(country) => Loader(client, country, countryEncoder.toEntity(country).body)
      case _ => Conflict("Country already available")
    }).getOrElse(NotFound())

  private def availableCountries: F[Json] = cacheRef.availableCountries.map(countries => countries.asJson)

  private def count: F[Json] = cacheRef.countryCount.map(_.asJson)

  private def countriesNotAvailableYet: F[Json] =
    cacheRef.countriesNotAvailableYet.map(countries => countries.asJson)

  private def findCountry(id: String): F[Option[Country]] =
    if (id.length == 2) cacheRef.countryByCode(id.toLowerCase) else cacheRef.countryByName(id)
}
