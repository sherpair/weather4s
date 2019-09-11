package io.sherpair.geo.app

import cats.effect.IO
import io.sherpair.geo.{countryUnderTest, BaseSpec, ImplicitsIO}
import io.sherpair.geo.cache.CacheRef
import io.sherpair.geo.domain.{Country, CountryCount}
import io.sherpair.geo.engine.EngineOps
import org.http4s.{EntityDecoder, Request, Response, Status}
import org.http4s.Method.GET
import org.http4s.Uri.unsafeFromString
import org.http4s.circe._
import org.http4s.implicits._
import org.scalatest.Assertion

class CountryAppSpec extends BaseSpec with ImplicitsIO {

  def withCountryAppRoutes(resources: IO[(CacheRef[IO], EngineOps[IO])], request: Request[IO])(
      test: Response[IO] => Assertion
  ): Assertion = {
    val response = (for {
      (cacheRef: CacheRef[IO], _) <- resources
      response <- new CountryApp[IO](cacheRef).routes.orNotFound.run(request)
    }
    yield response).unsafeRunSync

    test(response)
  }

  "GET -> /countries" should {
    "return the number of total, available and not-available-yet countries" in {
      withBaseResources {
        withCountryAppRoutes(_, Request[IO](GET, uri"/countries")) { response: Response[IO] =>

          implicit val countryCountDecoder: EntityDecoder[IO, CountryCount] = jsonOf[IO, CountryCount]

          response.status shouldBe Status.Ok

          // val body = response.body.compile.toVector.unsafeRunSync.map(_.toChar).mkString
          response.as[CountryCount].unsafeRunSync should have(
            Symbol("total")(Country.numberOfCountries),
            Symbol("available")(0),
            Symbol("notAvailableYet")(Country.numberOfCountries)
          )
        }
      }
    }
  }

  "GET -> /country / {id}" should {
    "return the requested country" in {
      val expectedCode = countryUnderTest.code
      val expectedName = countryUnderTest.name

      withBaseResources {
        withCountryAppRoutes(_, Request[IO](GET, unsafeFromString(s"/country/${expectedCode}"))) { response: Response[IO] =>

          implicit val countryDecoder: EntityDecoder[IO, Country] = jsonOf[IO, Country]

          response.status shouldBe Status.Ok

          response.as[Country].unsafeRunSync should have(
            Symbol("code")(expectedCode),
            Symbol("name")(expectedName)
          )
        }
      }
    }
  }

  "GET -> /country / {id}" should {
    "return \"NotFound\" if the request concerns an unknown country" in {
      withBaseResources {
        withCountryAppRoutes(_, Request[IO](GET, uri"/country/unknown")) { response: Response[IO] =>

          response.status shouldBe Status.NotFound
        }
      }
    }
  }
}
