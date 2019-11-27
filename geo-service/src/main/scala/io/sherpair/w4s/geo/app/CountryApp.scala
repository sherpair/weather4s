package io.sherpair.w4s.geo.app

import cats.effect.ConcurrentEffect
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.functor._
import fs2.Stream
import io.circe.Json
import io.circe.syntax.EncoderOps
import io.sherpair.w4s.auth.retrieveBearerTokenFromMessage
import io.sherpair.w4s.domain.{ClaimContent, Country, CountryCount, Logger}
import io.sherpair.w4s.geo.cache.CacheRef
import io.sherpair.w4s.geo.config.GeoConfig
import io.sherpair.w4s.geo.http.Loader
import io.sherpair.w4s.http.JsonMT
import io.sherpair.w4s.types.Countries
import org.http4s.{AuthedRoutes, EntityEncoder, Request, Response}
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.dsl.Http4sDsl

class CountryApp[F[_]](
    cacheRef: CacheRef[F], client: Client[F])(
    implicit C: GeoConfig, CE: ConcurrentEffect[F], L: Logger[F]
) extends Http4sDsl[F] {

  implicit val countryEncoder: EntityEncoder[F, Country] = jsonEncoderOf[F, Country]
  implicit val countryCountEncoder = jsonEncoderOf[F, CountryCount]

  lazy val loaderUrl =
    s"""
      ${C.loaderData.plainHttp.fold("https")(if (_) "http" else "https")}://
      ${C.loaderData.host.joined}
      ${C.loaderData.segment}
    """.replaceAll("\\s", "")

  val routes: AuthedRoutes[ClaimContent, F] =
    AuthedRoutes.of[ClaimContent, F] {
      case GET -> Root / "countries" as _ => Ok(count)
      case GET -> Root / "countries" / "available" as _ => Ok(availableCountries, JsonMT)
      case GET -> Root / "countries" / "not-available-yet" as _ => Ok(countriesNotAvailableYet, JsonMT)
      case GET -> Root / "country" / id as _ => findCountry(id) >>= { _.fold(unknown(id))(Ok(_)) }

      case GET -> Root / "localities" / id as _ => findCountry(id) >>= {
        _.fold(unknown(id))(country => Ok(country.localities.toString))
      }

      case request @ PUT -> Root / "country" / id as _ => addCountry(id, request.req)
    }

  private def addCountry(id: String, request: Request[F]): F[Response[F]] =
    CE.delay(id.length == 2).ifM(cacheRef.countryByCode(id.toLowerCase), cacheRef.countryByName(id)) >>= {
      maybeCountry => cacheRef.countriesNotAvailableYet >>= {
        addCountryToEngineIfNotAvailableYet(_, id, maybeCountry, request)
      }
    }

  private def addCountryToEngineIfNotAvailableYet(
      countriesNotAvailableYet: Countries, id: String, maybeCountry: Option[Country], request: Request[F]
  ): F[Response[F]] =
    maybeCountry.map(country => countriesNotAvailableYet.find(_.code == country.code) match {
      case Some(country) =>
        retrieveBearerTokenFromMessage(request).fold(missingToken(country.code)) {
          Loader(client, country, loaderUrl, _)
        }

      case _ => Conflict("Country already available")
    }).getOrElse(unknown(id))

  private def availableCountries: Stream[F, String] =
    Stream.eval(cacheRef.availableCountries.map(_.asJson.noSpaces))

  private def count: F[Json] = cacheRef.countryCount.map(_.asJson)

  private def countriesNotAvailableYet: Stream[F, String] =
    Stream.eval(cacheRef.countriesNotAvailableYet.map(_.asJson.noSpaces))

  private def findCountry(id: String): F[Option[Country]] =
    if (id.length == 2) cacheRef.countryByCode(id.toLowerCase) else cacheRef.countryByName(id)

  private def missingToken(country: String): F[Response[F]] =
    L.error(s"addCountry(${country} request. Token is missing!!") *> InternalServerError()

  private def unknown(id: String): F[Response[F]] = NotFound(s"Country(${id}) is not known")
}
