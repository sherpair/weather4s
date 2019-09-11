package io.sherpair.geo.cache

import java.time.Instant

import scala.concurrent.duration._

import cats.effect.IO
import io.sherpair.geo.{countryUnderTest, BaseSpec, ImplicitsIO}
import io.sherpair.geo.domain.Meta
import io.sherpair.geo.engine.EngineOps

class CacheHandlerSpec extends BaseSpec with ImplicitsIO {

  "CacheHandler" when {
    "one country's locations are loaded into the engine on user's behalf" should {
      "trigger a cache renewal" in {

        withBaseResources { resources: IO[(CacheRef[IO], EngineOps[IO])] =>

          val timeTheUpdateTookPlace = Instant.now.toEpochMilli
          val expectedCountry = countryUnderTest.copy(updated = timeTheUpdateTookPlace)

          val maybeCountry =
            for {
              (cacheRef: CacheRef[IO], engineOps: EngineOps[IO]) <- resources

              // Ok. Now let's update the 2 indexes handled by the Geo service. The document/record in the
              // "countries" index for the country under test (ZW), and thereafter the unique document in
              // the "meta" index, which informs the CacheHandler when the engine gets updated.
              // It's indeed the update of the "meta" index to trigger the cache renewal by the CacheHandler.
              _ <- engineOps.engineOpsCountries.upsert(expectedCountry)
              _ <- engineOps.engineOpsMeta.upsert(Meta(Instant.now.toEpochMilli))

              // The CacheHandler should stop straight after the 1st iteration.
              _ <- cacheRef.stopCacheHandler

              // Starting the CacheHandler, which should update the cache.
              _ <- CacheHandler.describe[IO](cacheRef, engineOps, 1 millisecond)

              // Retrieve the country under test
              maybeCountry <- cacheRef.countryByCode(expectedCountry.code)
            }
            yield maybeCountry

          maybeCountry.unsafeRunSync shouldBe Some(expectedCountry)
        }
      }
    }
  }
}
