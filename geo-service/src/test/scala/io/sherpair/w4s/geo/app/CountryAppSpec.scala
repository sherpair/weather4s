package io.sherpair.w4s.geo.app

import cats.data.Kleisli
import cats.effect.{ConcurrentEffect, IO}
import io.sherpair.w4s.domain.{Country, CountryCount, Logger}
import io.sherpair.w4s.geo.{BaseSpec, ImplicitsIO}
import io.sherpair.w4s.geo.cache.CacheRef
import io.sherpair.w4s.geo.engine.EngineOps
import org.http4s.{EntityDecoder, Request, Response, Status}
import org.http4s.Method.GET
import org.http4s.Uri.unsafeFromString
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.implicits._

class CountryAppSpec extends BaseSpec {

  def withCountryAppRoutes(
    resourcesF: IO[(CacheRef[IO], EngineOps[IO])], request: Request[IO]
  )(implicit CE: ConcurrentEffect[IO], L: Logger[IO]): IO[Response[IO]] = {

    // For the time being... not used with the tests we have have at this time
    val client: Client[IO] = Client.fromHttpApp[IO](Kleisli.pure(Response[IO](Status.NoContent)))

    for {
      resources <- resourcesF
      cacheRef <- IO.pure(resources._1)
      response <- new CountryApp[IO](cacheRef, client).routes.orNotFound.run(request)
    } yield response
  }

  "GET -> /countries" should {
    "return the number of total, available and not-available-yet countries" in new ImplicitsIO {
      val responseIO = withCountryAppRoutes(withBaseResources, Request[IO](GET, uri"/countries"))

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

      val responseIO = withCountryAppRoutes(
        withBaseResources, Request[IO](GET, unsafeFromString(s"/country/${expectedCode}"))
      )

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
      val responseIO = withCountryAppRoutes(withBaseResources, Request[IO](GET, uri"/country/unknown"))

      val response = responseIO.unsafeRunSync
      response.status shouldBe Status.NotFound
    }
  }
}
