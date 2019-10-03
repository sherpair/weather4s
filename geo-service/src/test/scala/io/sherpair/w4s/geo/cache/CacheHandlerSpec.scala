package io.sherpair.w4s.geo.cache

import java.time.Instant

import scala.concurrent.duration._

import cats.effect.IO
import io.sherpair.w4s.domain.Meta
import io.sherpair.w4s.geo.{BaseSpec, ImplicitsIO}

class CacheHandlerSpec extends BaseSpec {

  "CacheHandler" when {
    "one country's localities are loaded into the engine on user's behalf" should {
      "trigger a cache renewal" in new ImplicitsIO {

        val timeTheUpdateTookPlace = Instant.now.toEpochMilli
        val expectedCountry = countryUnderTest.copy(updated = timeTheUpdateTookPlace)

        val maybeCountry =
          for {
            resources <- withBaseResources
            cacheRef <- IO.pure(resources._1)
            engineOps <- IO.pure(resources._2)

            // Ok. Now let's update the 2 indexes handled by the Geo service. The document/record in the
            // "countries" index for the country under test (ZW), and thereafter the unique document in
            // the "meta" index, which informs the CacheHandler when the engine gets updated.
            // It's indeed the update of the "meta" index to trigger the cache renewal by the CacheHandler.
            _ <- engineOps.engineOpsCountries.upsert(expectedCountry)
            _ <- engineOps.engineOpsMeta.upsert(Meta(Instant.now.toEpochMilli))

            // The CacheHandler should stop straight after the 1st iteration.
            _ <- cacheRef.stopCacheHandler

            // Starting the CacheHandler, which should update the cache.
            _ <- CacheHandler[IO](cacheRef, engineOps, 1000 millisecond)

            // Retrieve the country under test
            maybeCountry <- cacheRef.countryByCode(expectedCountry.code)
          }
          yield maybeCountry

        maybeCountry.unsafeRunSync shouldBe Some(expectedCountry)
      }
    }
  }
}
