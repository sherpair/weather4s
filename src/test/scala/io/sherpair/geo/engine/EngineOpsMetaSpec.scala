package io.sherpair.geo.engine

import cats.effect.SyncIO
import io.sherpair.geo.{BaseSpec, ImplicitsSyncIO}
import io.sherpair.geo.domain.{epochAsLong, Meta}
import io.sherpair.geo.engine.EngineMeta.indexName

class EngineOpsMetaSpec extends BaseSpec {

  "createIndexIfNotExists" when {
    "the \"meta\" index does not exist yet in the engine" should {
      "successfully initialise the Meta object" in new ImplicitsSyncIO {
        val meta: Meta = new EngineOpsMeta(memoryEngine).createIndexIfNotExists.unsafeRunSync
        meta shouldBe Meta(epochAsLong)
      }
    }

    "the \"meta\" index does exist already in the engine" should {
      "successfully load the Meta object from the engine" in new ImplicitsSyncIO {
        val engineOpsMeta = new EngineOpsMeta(memoryEngine)
        val initialiseMeta = PrivateMethod[SyncIO[Meta]](Symbol("initialiseMeta"))
        (engineOpsMeta invokePrivate initialiseMeta()).unsafeRunSync

        val meta: Meta = engineOpsMeta.createIndexIfNotExists.unsafeRunSync
        meta shouldBe Meta(epochAsLong)
      }
    }

    "the \"meta\" index does exist yet in the engine" should {
      "throw an exception if it does not contain the expected Meta object" in new ImplicitsSyncIO {
        (for {
          _ <- memoryEngine.createIndex(indexName)
          res <- memoryEngine.indexExists(indexName)
        } yield res).unsafeRunSync shouldBe true

        val exc = intercept[IllegalArgumentException] {
          new EngineOpsMeta(memoryEngine).createIndexIfNotExists.unsafeRunSync
        }
        exc.getMessage shouldBe s"requirement failed: ${Meta.requirement}"
      }
    }
  }
}
