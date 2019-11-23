package io.sherpair.w4s

import java.security.{KeyStore, SecureRandom}
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}

import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration._

import cats.effect.{ConcurrentEffect => CE, Resource, Sync}
import cats.syntax.applicative._
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.option._
import fs2.Stream
import io.circe.Encoder
import io.circe.syntax.EncoderOps
import io.sherpair.w4s.config.{Configuration, SSLData}
import io.sherpair.w4s.domain.Logger
import org.http4s.MediaType
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.client.middleware.{RequestLogger, ResponseLogger}
import org.http4s.headers.`Content-Type`

package object http {

  val MT: `Content-Type` = `Content-Type`(MediaType.application.json)

  def arrayOf[F[_], T](stream: Stream[F, T])(implicit encoder: Encoder[T]): Stream[F, String] =
    Stream("[") ++ stream.map(_.asJson.noSpaces).intersperse(",") ++ Stream("]")

  def blazeClient[F[_]: CE](sslContextO: Option[SSLContext]): Resource[F, Client[F]] =
    BlazeClientBuilder(global)
      .withConnectTimeout(40 seconds)
      .withRequestTimeout(40 seconds)
      .withSslContextOption(sslContextO).resource

  def maybeWithSSLContext[F[_]](implicit C: Configuration, L: Logger[F], S: Sync[F]): Resource[F, Option[SSLContext]] =
    Resource.liftF {
      S.delay(C.plainHttp.fold(true)(!_))
        .ifM(
          L.info(s"Loading https keystore(${C.sslData.keyStore})") *> withSSLContext(C.sslData),
          L.info(s"Plain HTTP (no HTTPS!!) for ${C.service}") *> S.pure(none[SSLContext])
        )
    }

  def withClientMiddleware[F[_]: CE](client: Client[F])(implicit L: Logger[F]): Resource[F, Client[F]] =
    Resource.liftF(
      L.isDebugEnabled.ifM(
        CE[F].delay {
          RequestLogger(logHeaders = true, logBody = true, redactHeadersWhen = _ => false)(
            ResponseLogger(logHeaders = true, logBody = true, redactHeadersWhen = _ => false)(client)
          )
        }, client.pure[F]
      )
    )

  private def withSSLContext[F[_]](sslData: SSLData)(implicit S: Sync[F]): F[Option[SSLContext]] =
    Resource
      .fromAutoCloseable(S.delay(getClass.getResourceAsStream(sslData.keyStore)))
      .use { kis =>
        Resource
          .fromAutoCloseable(S.delay(getClass.getResourceAsStream(sslData.trustStore)))
          .use { tis =>
            val keyStore = KeyStore.getInstance(sslData.`type`)
            val keyManagerFactory = KeyManagerFactory.getInstance(sslData.algorithm)
            keyStore.load(kis, sslData.secret.toCharArray)
            keyManagerFactory.init(keyStore, sslData.secret.toCharArray)

            val trustStore = KeyStore.getInstance("JKS")
            trustStore.load(tis, sslData.secret.toCharArray)
            val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm)
            trustManagerFactory.init(trustStore)

            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(
              keyManagerFactory.getKeyManagers,
              trustManagerFactory.getTrustManagers,
              SecureRandom.getInstance(sslData.randomAlgorithm)
            )

            S.pure(sslContext.some)
          }
      }
}
