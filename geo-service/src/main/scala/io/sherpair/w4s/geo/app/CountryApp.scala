package io.sherpair.w4s.geo.app

import cats.effect.ConcurrentEffect
import cats.syntax.flatMap._
import cats.syntax.functor._
import fs2.Stream
import io.circe.Json
import io.circe.syntax.EncoderOps
import io.sherpair.w4s.domain.{Country, CountryCount, Logger}
import io.sherpair.w4s.geo.cache.CacheRef
import io.sherpair.w4s.geo.config.GeoConfig
import io.sherpair.w4s.geo.http.Loader
import io.sherpair.w4s.http.MT
import io.sherpair.w4s.types.Countries
import org.http4s.{EntityEncoder, HttpRoutes, Response}
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.dsl.Http4sDsl

class CountryApp[F[_]](
    cacheRef: CacheRef[F], client: Client[F])(implicit C: GeoConfig, CE: ConcurrentEffect[F], L: Logger[F]
) extends Http4sDsl[F] {

  implicit val countryEncoder: EntityEncoder[F, Country] = jsonEncoderOf[F, Country]
  implicit val countryCountEncoder = jsonEncoderOf[F, CountryCount]

  def routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "countries" => Ok(count)
    case GET -> Root / "countries" / "available" => Ok(availableCountries, MT)
    case GET -> Root / "countries" / "not-available-yet" => Ok(countriesNotAvailableYet, MT)
    case GET -> Root / "country" / id => findCountry(id) >>= { _.fold(unknown(id))(Ok(_)) }
    case PUT -> Root / "country" / id => addCountry(id)

    case GET -> Root / "localities" / id => findCountry(id) >>= {
      _.fold(unknown(id))(country => Ok(country.localities.toString))
    }
  }

  private def addCountry(id: String): F[Response[F]] =
    CE.delay(id.length == 2).ifM(cacheRef.countryByCode(id.toLowerCase), cacheRef.countryByName(id)) >>= {
      maybeCountry => cacheRef.countriesNotAvailableYet >>= {
        addCountryToEngineIfNotAvailableYet(_, maybeCountry, id)
      }
    }

  private def addCountryToEngineIfNotAvailableYet(
      countriesNotAvailableYet: Countries, maybeCountry: Option[Country], id: String
  ): F[Response[F]] =
    maybeCountry.map(country => countriesNotAvailableYet.find(_.code == country.code) match {
      case Some(country) => Loader(client, country, countryEncoder.toEntity(country).body)
      case _ => Conflict("Country already available")
    }).getOrElse(unknown(id))

  private def availableCountries: Stream[F, String] =
    Stream.eval(cacheRef.availableCountries.map(_.asJson.noSpaces))

  private def count: F[Json] = cacheRef.countryCount.map(_.asJson)

  private def countriesNotAvailableYet: Stream[F, String] =
    Stream.eval(cacheRef.countriesNotAvailableYet.map(_.asJson.noSpaces))

  private def findCountry(id: String): F[Option[Country]] =
    if (id.length == 2) cacheRef.countryByCode(id.toLowerCase) else cacheRef.countryByName(id)

  private def unknown(id: String): F[Response[F]] = NotFound(s"Country(${id}) is not known")
}
