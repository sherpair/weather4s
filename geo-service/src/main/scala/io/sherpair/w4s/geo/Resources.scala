package io.sherpair.w4s.geo

import java.security.{KeyStore, SecureRandom}
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManager}

import scala.concurrent.ExecutionContext.global

import cats.effect.{ConcurrentEffect => CE, ContextShift => CS, Fiber, Resource, Timer}
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.option._
import io.sherpair.w4s.domain.Logger
import io.sherpair.w4s.engine.Engine
import io.sherpair.w4s.geo.app.Routes
import io.sherpair.w4s.geo.cache.{Cache, CacheHandler, CacheRef}
import io.sherpair.w4s.geo.config.{GeoConfig, SSLGeo}
import io.sherpair.w4s.geo.engine.EngineOps
import io.sherpair.w4s.http.{HttpServer, SSLData}
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.server.Server

object Resources {

  type CallGraphRes[F[_]] = Resource[F, (Cache, Fiber[F, Unit], Server[F])]

  def apply[F[_]: CE: CS: Timer](
      engineR: Resource[F, Engine[F]])(implicit C: GeoConfig, L: Logger[F]
  ): CallGraphRes[F] =
    for {
      implicit0(engine: Engine[F]) <- engineR
      implicit0(engineOps: EngineOps[F]) <- Resource.liftF(EngineOps[F](C.clusterName))
      countriesCache <- Resource.make(engineOps.init)(_ => engineOps.close)
      cacheRef <- Resource.make(CacheRef[F](countriesCache))(_.stopCacheHandler)
      cacheHandlerFiber <- Resource.liftF(CacheHandler[F](cacheRef, engineOps, C.cacheHandlerInterval))
      client <- BlazeClientBuilder[F](global).resource
      routes <- Routes[F](cacheRef, client, engineOps)
      sslData <- Resource.liftF(withHttps.ifM(withSSLData(C.sslGeo), none[SSLData].pure[F]))
      server <- HttpServer[F](C.hostGeo, C.httpPoolSize, "/geo", routes, sslData)
    }
    yield (countriesCache, cacheHandlerFiber, server)

  private def withHttps[F[_]: CE]: F[Boolean] =
    CE[F].delay(sys.env.get("W4S_GEO_PLAIN_HTTP").fold(true)(_.toLowerCase != "true"))

  private def withSSLData[F[_]: CE](sslGeo: SSLGeo): F[Option[SSLData]] =
    Resource
      .fromAutoCloseable(CE[F].delay(getClass.getResourceAsStream(s"/${sslGeo.keyStore}")))
      .use { is =>
        val keyStore = KeyStore.getInstance(sslGeo.`type`)
        val keyManagerFactory = KeyManagerFactory.getInstance(sslGeo.algorithm)
        keyStore.load(is, sslGeo.password.toCharArray)
        keyManagerFactory.init(keyStore, sslGeo.password.toCharArray)

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(
          keyManagerFactory.getKeyManagers,
          Array.empty[TrustManager],
          SecureRandom.getInstance(sslGeo.randomAlgorithm)
        )

        CE[F].delay(SSLData(sslGeo.host, sslContext).some)
    }
}
