package io.sherpair.w4s

import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration._

import cats.effect.{ContextShift, IO, Timer}
import io.chrisdavenport.log4cats.noop.NoOpLogger
import io.sherpair.w4s.config.{
  Cluster, Configuration, Engine => EngineConfig, GlobalLock, HealthCheck, Host, Service, Suggestions
}
import io.sherpair.w4s.domain.{Country, Logger}
import io.sherpair.w4s.domain.Analyzer.{english, stop}
import io.sherpair.w4s.engine.{Engine, EngineIndex}
import io.sherpair.w4s.engine.memory.MemoryEngine
import io.sherpair.w4s.geo.cache.CacheRef
import io.sherpair.w4s.geo.config.{GeoConfig, SSLGeo}
import io.sherpair.w4s.geo.engine.EngineOps
import org.scalatest.{Matchers, OptionValues, PrivateMethodTester, WordSpec}

package object geo {

  abstract class BaseSpec
    extends WordSpec
      with Matchers
      with OptionValues
      with PrivateMethodTester {

    val countryUnderTest = Country("zw", "Zimbabwe", english)

    val port = 8082
    val host = Host("localhost", port)
    val maxSuggestions = 10

    implicit val configuration: GeoConfig = GeoConfig(
      cacheHandlerInterval = 1 second,
      EngineConfig(
        Cluster("w4sCluster"),
        EngineIndex.defaultWindowSize,
        GlobalLock(3, 1 second, true),
        HealthCheck(4, 1 second),
        host
      ),
      host, host, host,
      httpPoolSize = 2,
      Service("Geo"),
      SSLGeo(
        "SunX509", host, "ssl/weather4s.p12", "w4s123456", "NativePRNGNonBlocking", "PKCS12"
      ),
      Suggestions(stop, 1, maxSuggestions)
    )
  }

  trait ImplicitsIO {
    implicit val cs: ContextShift[IO] = IO.contextShift(global)
    implicit val timer: Timer[IO] = IO.timer(global)
    implicit val logger: Logger[IO] = NoOpLogger.impl[IO]
    implicit val engine: Engine[IO] = MemoryEngine[IO]

    def withBaseResources(implicit C: Configuration): IO[(CacheRef[IO], EngineOps[IO])] = {
      for {
        implicit0(engineOps: EngineOps[IO]) <- EngineOps[IO]("clusterName")
        countriesCache <- engineOps.init

        // The cache should already contain all known countries with the property "updated" always set to "epoch".
        cacheRef <- CacheRef[IO](countriesCache)
      }
      yield (cacheRef -> engineOps)
    }
  }

  trait ImplicitsOpsIO {
    implicit val cs: ContextShift[IO] = IO.contextShift(global)
    implicit val timer: Timer[IO] = IO.timer(global)
    implicit val logger: Logger[IO] = NoOpLogger.impl[IO]
    implicit val engine: Engine[IO] = MemoryEngine[IO]

    def engineOpsF(implicit C: Configuration): IO[EngineOps[IO]] =
      EngineOps[IO]("clusterName")
  }

//  trait ImplicitsSyncIO {
//    implicit val logger: Logger[SyncIO] = NoOpLogger.impl[SyncIO]
//    implicit val engine: Engine[SyncIO] = MemoryEngine[SyncIO]
//
//    val engineOpsF: SyncIO[EngineOps[SyncIO]] = EngineOps[SyncIO]("clusterName")
//  }
}
