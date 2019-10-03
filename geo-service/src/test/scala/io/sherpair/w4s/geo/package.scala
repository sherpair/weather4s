package io.sherpair.w4s

import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration._

import cats.effect.{ContextShift, IO, SyncIO, Timer}
import io.chrisdavenport.log4cats.noop.NoOpLogger
import io.sherpair.w4s.config.{Cluster, Engine => EngineConfig, GlobalLock, HealthCheck, Host, Http, Service}
import io.sherpair.w4s.domain.{epochAsLong, Country, Logger}
import io.sherpair.w4s.engine.{Engine, EngineIndex}
import io.sherpair.w4s.engine.memory.MemoryEngine
import io.sherpair.w4s.geo.cache.CacheRef
import io.sherpair.w4s.geo.config.GeoConfig
import io.sherpair.w4s.geo.engine.EngineOps
import org.scalatest.{Matchers, OptionValues, PrivateMethodTester, WordSpec}

package object geo {

  abstract class BaseSpec
    extends WordSpec
      with Matchers
      with OptionValues
      with PrivateMethodTester {

    val countryUnderTest = Country("zw", "Zimbabwe", 0L, epochAsLong)

    val port = 8081
    val host = Host("localhost", port)

    implicit val configuration: GeoConfig = GeoConfig(
      cacheHandlerInterval = 1 second,
      EngineConfig(
        Cluster("clusterName"),
        host,
        EngineIndex.defaultWindowSize,
        GlobalLock(3, 1 second, true),
        HealthCheck(4, 1 second)
      ),
      Http(host), Http(host),
      Service("geo")
    )
  }

  trait ImplicitsIO {
    implicit val cs: ContextShift[IO] = IO.contextShift(global)
    implicit val timer: Timer[IO] = IO.timer(global)
    implicit val logger: Logger[IO] = NoOpLogger.impl[IO]
    implicit val engine: Engine[IO] = MemoryEngine[IO]

    def withBaseResources: IO[(CacheRef[IO], EngineOps[IO])] = {
      for {
        implicit0(engineOps: EngineOps[IO]) <- EngineOps[IO]("clusterName")
        countriesCache <- engineOps.init

        // The cache should already contain all known countries with the property "updated" always set to "epoch".
        cacheRef <- CacheRef[IO](countriesCache)
      }
      yield (cacheRef -> engineOps)
    }
  }

  trait ImplicitsSyncIO {
    implicit val logger: Logger[SyncIO] = NoOpLogger.impl[SyncIO]
    implicit val engine: Engine[SyncIO] = MemoryEngine[SyncIO]

    val engineOpsF: SyncIO[EngineOps[SyncIO]] = EngineOps[SyncIO]("clusterName")
  }
}
