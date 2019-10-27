package io.sherpair.w4s.geo.engine

import scala.concurrent.ExecutionContext.global

import cats.effect.{ContextShift, IO}
import io.sherpair.w4s.domain.{unit, BulkError, BulkErrors, Country, W4sError}
import io.sherpair.w4s.domain.Analyzer.brazilian
import io.sherpair.w4s.domain.Country.indexName
import io.sherpair.w4s.engine.{Engine, EngineIndex}
import io.sherpair.w4s.engine.memory.{DataSuggesters, MemoryEngine}
import io.sherpair.w4s.geo.{GeoSpec, IOengine}

class EngineOpsCountriesSpec extends GeoSpec {

  val Brazil = Country("BR", "Brazil", brazilian)

  "createIndexIfNotExists" when {
    "the \"countries\" index does not exist yet in the engine" should {
      "successfully load and decode the list of countries from a json resource" in new IOengine {
        val countries = for {
          engineOps <- EngineOps[IO](C.clusterName)
          countriesFromResource <- engineOps.engineOpsCountries.createIndexIfNotExists
          // Also checking the countries are successfully saved in the engine
          countriesFromEngine <- engineOps.loadCountries
        }
        yield (countriesFromResource, countriesFromEngine)

        val (countriesFromResource, countriesFromEngine) = countries.unsafeRunSync

        countriesFromResource.size shouldBe countriesFromEngine.size
        countriesFromResource.size shouldBe Country.numberOfCountries

        countriesFromResource should (contain(Brazil) and contain(countryUnderTest))

        countriesFromEngine should (contain(Brazil) and contain(countryUnderTest))
      }
    }

    "the \"countries\" index does already exist in the engine" should {
      "successfully load and decode the list of countries from the engine" in new IOengine {
        val countries = for {
          engineOps <- EngineOps[IO](C.clusterName)
          countriesFromResource <- engineOps.engineOpsCountries.firstLoadOfCountriesFromResource
          countriesFromEngine <- engineOps.engineOpsCountries.firstLoadOfCountriesFromEngine
        }
        yield (countriesFromResource, countriesFromEngine)

        val (countriesFromResource, countriesFromEngine) = countries.unsafeRunSync

        countriesFromResource.size shouldBe countriesFromEngine.size
        countriesFromResource.size shouldBe Country.numberOfCountries

        countriesFromResource should (contain (Brazil) and contain (countryUnderTest))

        countriesFromEngine should (contain (Brazil) and contain (countryUnderTest))
      }
    }

    "the \"countries\" index does already exist in the engine" should {
      "throw an exception if it does not contain the expected data" in new IOengine {
        val exc = intercept[W4sError] {
          (for {
            _ <- engine.createIndex(indexName)
            _ <- engine.indexExists(indexName)
            engineOps <- EngineOps[IO](C.clusterName)
            _ <- engineOps.engineOpsCountries.createIndexIfNotExists
          } yield unit).unsafeRunSync
        }

        exc.getMessage shouldBe s"${C.service}: ${Country.requirement}"
      }
    }

    "the \"countries\" index does not exist yet in the engine" should {
      "throw an exception if errors are detected while saving data to the index" in {

        implicit val resultForSaveAll: BulkErrors =
          List(BulkError(countryUnderTest.code, s"Error while saving ${countryUnderTest.name}"))

        implicit val cs: ContextShift[IO] = IO.contextShift(global)

        implicit val engine: Engine[IO] =
          MemoryEngine[IO](Map.empty[String, DataSuggesters]).unsafeRunSync

        val exc = intercept[W4sError] {
          (for {
            engineOps <- EngineOps[IO](C.clusterName)
            _ <- engineOps.engineOpsCountries.createIndexIfNotExists
          } yield unit).unsafeRunSync
        }

        exc.getMessage should startWith (s"${C.service}: ${EngineIndex.bulkErrorMessage}")
      }
    }
  }
}
