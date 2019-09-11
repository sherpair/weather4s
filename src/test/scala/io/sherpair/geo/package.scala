package io.sherpair

import scala.concurrent.ExecutionContext.global

import cats.effect.{IO, SyncIO, Timer}
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.noop.NoOpLogger
import io.sherpair.geo.cache.CacheRef
import io.sherpair.geo.domain.{epochAsLong, Country}
import io.sherpair.geo.engine.EngineOps
import io.sherpair.geo.engine.memory.MemoryEngine
import org.scalatest.{Assertion, Matchers, OptionValues, PrivateMethodTester, WordSpec}

package object geo {

  abstract class BaseSpec
    extends WordSpec
      with Matchers
      with OptionValues
      with PrivateMethodTester

  trait ImplicitsIO {
    implicit val timer: Timer[IO] = IO.timer(global)
    implicit val logger: Logger[IO] = NoOpLogger.impl[IO]
    implicit val memoryEngine: MemoryEngine[IO] = MemoryEngine[IO].unsafeRunSync

    def withBaseResources(test: IO[(CacheRef[IO], EngineOps[IO])] => Assertion): Assertion = {
      val resources: IO[(CacheRef[IO], EngineOps[IO])] =
        for {
          implicit0(engineOps: EngineOps[IO]) <- EngineOps[IO]("clusterName")
          countriesCache <- engineOps.init

          // The cache should already contain all known countries with the property "updated" always set to "epoch".
          cacheRef <- CacheRef[IO](countriesCache)
        }
          yield (cacheRef, engineOps)

      test(resources)
    }
  }

  trait ImplicitsSyncIO {
    implicit val logger: Logger[SyncIO] = NoOpLogger.impl[SyncIO]
    val memoryEngine: MemoryEngine[SyncIO] = MemoryEngine[SyncIO].unsafeRunSync
  }

  val countryUnderTest = Country("ZW", "Zimbabwe", epochAsLong)
}
