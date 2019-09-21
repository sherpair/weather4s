package io.sherpair.w4s.geo.app

import cats.effect.{ConcurrentEffect, IO}
import io.chrisdavenport.log4cats.Logger
import io.sherpair.w4s.domain.{Country, CountryCount}
import io.sherpair.w4s.geo.{countryUnderTest, BaseSpec, ImplicitsIO}
import io.sherpair.w4s.geo.cache.CacheRef
import org.http4s.{EntityDecoder, Request, Response, Status}
import org.http4s.Method.GET
import org.http4s.Uri.unsafeFromString
import org.http4s.circe._
import org.http4s.implicits._

class CountryAppSpec extends BaseSpec {

  def withCountryAppRoutes(
    cacheRef: IO[CacheRef[IO]], request: Request[IO]
  )(implicit CE: ConcurrentEffect[IO], L: Logger[IO]): IO[Response[IO]] =
    for {
      cacheRef <- cacheRef
      response <- new CountryApp[IO](cacheRef).routes.orNotFound.run(request)
    } yield response

  "GET -> /countries" should {
    "return the number of total, available and not-available-yet countries" in new ImplicitsIO {
      val (cacheRef, _) = withBaseResources
      val responseIO = withCountryAppRoutes(cacheRef, Request[IO](GET, uri"/countries"))

      implicit val countryCountDecoder: EntityDecoder[IO, CountryCount] = jsonOf[IO, CountryCount]

      val response = responseIO.unsafeRunSync
      response.status shouldBe Status.Ok

      // val body = response.body.compile.toVector.unsafeRunSync.map(_.toChar).mkString
      response.as[CountryCount].unsafeRunSync should have(
        Symbol("total")(Country.numberOfCountries),
        Symbol("available")(0),
        Symbol("notAvailableYet")(Country.numberOfCountries)
      )
    }
  }

  "GET -> /country / {id}" should {
    "return the requested country" in new ImplicitsIO {
      val expectedCode = countryUnderTest.code
      val expectedName = countryUnderTest.name

      val (cacheRef, _) = withBaseResources
      val responseIO = withCountryAppRoutes(cacheRef, Request[IO](GET, unsafeFromString(s"/country/${expectedCode}")))

      implicit val countryDecoder: EntityDecoder[IO, Country] = jsonOf[IO, Country]

      val response = responseIO.unsafeRunSync
      response.status shouldBe Status.Ok

      response.as[Country].unsafeRunSync should have(
        Symbol("code")(expectedCode),
        Symbol("name")(expectedName)
      )
    }
  }

  "GET -> /country / {id}" should {
    "return \"NotFound\" if the request concerns an unknown country" in new ImplicitsIO {
      val (cacheRef, _) = withBaseResources
      val responseIO = withCountryAppRoutes(cacheRef, Request[IO](GET, uri"/country/unknown"))

      val response = responseIO.unsafeRunSync
      response.status shouldBe Status.NotFound
    }
  }
}
