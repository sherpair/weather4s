package io.sherpair.w4s.geo.engine

import cats.effect.IO
import cats.syntax.option._
import io.sherpair.w4s.domain.{epochAsLong, unit, Meta}
import io.sherpair.w4s.domain.Meta.indexName
import io.sherpair.w4s.geo.{GeoSpec, IOengine}

class EngineOpsMetaSpec extends GeoSpec {

  "createIndexIfNotExists" when {
    "the \"meta\" index does not exist yet in the engine" should {
      "successfully initialise the Meta object" in new IOengine {
        val metas = for {
          engineOps <- EngineOps[IO](C.clusterName)
          metaFromInitialise <- engineOps.engineOpsMeta.createIndexIfNotExists
          // Also checking the Meta object is successfully saved in the engine
          metaFromEngine <- engineOps.loadMeta
        }
        yield (metaFromInitialise, metaFromEngine)

        val (metaFromInitialise, metaFromEngine) = metas.unsafeRunSync
        metaFromInitialise shouldBe Meta(epochAsLong)
        metaFromEngine shouldBe Meta(epochAsLong).some
      }
    }

    "the \"meta\" index does already exist in the engine" should {
      "successfully load the Meta object from the engine" in new IOengine {
        val metas = for {
          engineOps <- EngineOps[IO](C.clusterName)
          metaFromInitialise <- engineOps.engineOpsMeta.initialiseMeta
          metaFromEngine <- engineOps.engineOpsMeta.firstMetaLoad
        }
        yield (metaFromInitialise, metaFromEngine)

        val (metaFromInitialise, metaFromEngine) = metas.unsafeRunSync
        metaFromInitialise shouldBe Meta(epochAsLong)
        metaFromEngine shouldBe Meta(epochAsLong)
      }
    }

    "the \"meta\" index does already exist in the engine" should {
      "throw an exception if it does not contain the expected Meta object" in new IOengine {
        val exc = intercept[IllegalArgumentException] {
          (for {
            _ <- engine.createIndex(indexName)
            _ <- engine.indexExists(indexName)
            engineOps <- EngineOps[IO](C.clusterName)
            _ <- engineOps.engineOpsMeta.createIndexIfNotExists
          } yield unit).unsafeRunSync
        }

        exc.getMessage shouldBe s"requirement failed: ${Meta.requirement}"
      }
    }
  }
}
