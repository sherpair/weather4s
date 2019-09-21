package io.sherpair.w4s.geo.engine

import cats.effect.SyncIO
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.noop.NoOpLogger
import io.sherpair.w4s.domain.{epochAsLong, unit, BulkError, Country}
import io.sherpair.w4s.domain.Country.indexName
import io.sherpair.w4s.engine.{Engine, EngineIndex}
import io.sherpair.w4s.engine.memory.MemoryEngine
import io.sherpair.w4s.geo.{countryUnderTest, BaseSpec, ImplicitsSyncIO}

class EngineOpsCountriesSpec extends BaseSpec {

  "createIndexIfNotExists" when {
    "the \"countries\" index does not exist yet in the engine" should {
      "successfully load and decode the list of countries from a json resource" in new ImplicitsSyncIO {
        val countries = for {
          countriesFromResource <- engineOps.engineOpsCountries.createIndexIfNotExists
          // Also checking the countries are successfully saved in the engine
          countriesFromEngine <- engineOps.engineOpsCountries.loadCountries
        }
        yield (countriesFromResource, countriesFromEngine)

        val (countriesFromResource, countriesFromEngine) = countries.unsafeRunSync

        countriesFromResource.size shouldBe countriesFromEngine.size
        countriesFromResource.size shouldBe Country.numberOfCountries

        countriesFromResource should
          (contain(Country("AF", "Afghanistan", epochAsLong)) and contain(countryUnderTest))

        countriesFromEngine should
          (contain(Country("AF", "Afghanistan", epochAsLong)) and contain(countryUnderTest))
      }
    }

    "the \"countries\" index does already exist in the engine" should {
      "successfully load and decode the list of countries from the engine" in new ImplicitsSyncIO {
        val countries = for {
          countriesFromResource <- engineOps.engineOpsCountries.firstLoadOfCountriesFromResource
          countriesFromEngine <- engineOps.engineOpsCountries.firstLoadOfCountriesFromEngine
        }
        yield (countriesFromResource, countriesFromEngine)

        val (countriesFromResource, countriesFromEngine) = countries.unsafeRunSync

        countriesFromResource.size shouldBe countriesFromEngine.size
        countriesFromResource.size shouldBe Country.numberOfCountries

        countriesFromResource should
          (contain (Country("AF", "Afghanistan", epochAsLong)) and contain (countryUnderTest))

        countriesFromEngine should
          (contain (Country("AF", "Afghanistan", epochAsLong)) and contain (countryUnderTest))
      }
    }

    "the \"countries\" index does already exist in the engine" should {
      "throw an exception if it does not contain the expected data" in new ImplicitsSyncIO {
        val exc = intercept[IllegalArgumentException] {
          (for {
            _ <- engine.createIndex(indexName)
            _ <- engine.indexExists(indexName)
            _ <- engineOps.engineOpsCountries.createIndexIfNotExists
          } yield unit).unsafeRunSync
        }

        exc.getMessage shouldBe s"requirement failed: ${Country.requirement}"
      }
    }

    "the \"countries\" index does not exist yet in the engine" should {
      "throw an exception if errors are detected while saving data to the index" in {
        implicit val logger: Logger[SyncIO] = NoOpLogger.impl[SyncIO]

        implicit val engine: Engine[SyncIO] = MemoryEngine.applyWithFailingSaveAll[SyncIO](
          List(BulkError(countryUnderTest.code, s"Error while saving ${countryUnderTest.name}"))
        )

        val engineOps: EngineOps[SyncIO] = EngineOps[SyncIO]("clusterName")

        val exc = intercept[IllegalArgumentException] {
          (for {
            _ <- engineOps.engineOpsCountries.createIndexIfNotExists
          } yield unit).unsafeRunSync
        }

        exc.getMessage should startWith (s"requirement failed: ${EngineIndex.bulkErrorMessage}")
      }
    }
  }
}
