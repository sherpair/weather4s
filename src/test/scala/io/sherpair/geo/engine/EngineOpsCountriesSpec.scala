package io.sherpair.geo.engine

import cats.effect.SyncIO
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.noop.NoOpLogger
import io.sherpair.geo.{countryUnderTest, BaseSpec, ImplicitsSyncIO}
import io.sherpair.geo.domain.{epochAsLong, Countries, Country}
import io.sherpair.geo.engine.EngineCountry.indexName
import io.sherpair.geo.engine.memory.MemoryEngine

class EngineOpsCountriesSpec extends BaseSpec {

  "createIndexIfNotExists" when {
    "the \"countries\" index does not exist yet in the engine" should {
      "successfully load and decode the list of countries from a json resource" in new ImplicitsSyncIO {
        val countries: Countries = new EngineOpsCountries(memoryEngine).createIndexIfNotExists.unsafeRunSync
        countries.size shouldBe Country.numberOfCountries
        countries should
          (contain (Country("AF", "Afghanistan", epochAsLong)) and contain (countryUnderTest))
      }
    }

    "the \"countries\" index does exist already in the engine" should {
      "successfully load and decode the list of countries from the engine" in new ImplicitsSyncIO {
        val engineOpsCountries = new EngineOpsCountries(memoryEngine)
        val firstLoadOfCountriesFromResource = PrivateMethod[SyncIO[Countries]](Symbol("firstLoadOfCountriesFromResource"))
        (engineOpsCountries invokePrivate firstLoadOfCountriesFromResource()).unsafeRunSync

        val countries: Countries = engineOpsCountries.createIndexIfNotExists.unsafeRunSync
        countries.size shouldBe (Country.numberOfCountries << 1) // Each country is indexed both by code and name in CacheRef
        countries should
          (contain (Country("AF", "Afghanistan", epochAsLong)) and contain (countryUnderTest))
      }
    }

    "the \"countries\" index does exist yet in the engine" should {
      "throw an exception if it does not contain the expected data" in new ImplicitsSyncIO {
        (for {
          _ <- memoryEngine.createIndex(indexName)
          res <- memoryEngine.indexExists(indexName)
        } yield res).unsafeRunSync shouldBe true

        val exc = intercept[IllegalArgumentException] {
          new EngineOpsCountries(memoryEngine).createIndexIfNotExists.unsafeRunSync
        }
        exc.getMessage shouldBe s"requirement failed: ${Country.requirement}"
      }
    }

    "the \"countries\" index does not exist yet in the engine" should {
      "throw an exception if errors are detected while saving data to the index" in {
        implicit val logger: Logger[SyncIO] = NoOpLogger.impl[SyncIO]
        val memoryEngine: MemoryEngine[SyncIO] = MemoryEngine.applyWithFailingSaveAll[SyncIO].unsafeRunSync

        val engineOpsCountries = new EngineOpsCountries(memoryEngine)

        val exc = intercept[IllegalArgumentException] {
          engineOpsCountries.createIndexIfNotExists.unsafeRunSync
        }
        exc.getMessage should startWith (s"requirement failed: ${engineOpsCountries.bulkErrorMessage}")
      }
    }
  }
}
