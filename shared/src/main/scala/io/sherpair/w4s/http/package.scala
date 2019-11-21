package io.sherpair.w4s

import java.security.{KeyStore, SecureRandom}
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManager}

import cats.effect.{Resource, Sync}
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.option._
import fs2.Stream
import io.circe.Encoder
import io.circe.syntax.EncoderOps
import io.sherpair.w4s.config.{Configuration, SSLData}
import io.sherpair.w4s.domain.Logger
import org.http4s.MediaType
import org.http4s.headers.`Content-Type`

package object http {

  val MT: `Content-Type` = `Content-Type`(MediaType.application.json)

  def arrayOf[F[_], T](stream: Stream[F, T])(implicit encoder: Encoder[T]): Stream[F, String] =
    Stream("[") ++ stream.map(_.asJson.noSpaces).intersperse(",") ++ Stream("]")

  def maybeWithSSLContext[F[_]](
      implicit C: Configuration, L: Logger[F], S: Sync[F]
  ): Resource[F, Option[SSLContext]] =
    Resource.liftF {
      S.delay(C.plainHttp.fold(true)(!_))
        .ifM(
          L.info(s"Loading https keystore(${C.sslData.keyStore})") *> withSSLContext(C.sslData),
          L.info(s"Plain HTTP (no HTTPS!!) for ${C.service}") *> S.pure(none[SSLContext])
        )
    }

  private def withSSLContext[F[_]](sslData: SSLData)(implicit S: Sync[F]): F[Option[SSLContext]] =
    Resource
      .fromAutoCloseable(S.delay(getClass.getResourceAsStream(sslData.keyStore)))
      .use { is =>
        val keyStore = KeyStore.getInstance(sslData.`type`)
        val keyManagerFactory = KeyManagerFactory.getInstance(sslData.algorithm)
        keyStore.load(is, sslData.secret.toCharArray)
        keyManagerFactory.init(keyStore, sslData.secret.toCharArray)

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(
          keyManagerFactory.getKeyManagers,
          Array.empty[TrustManager],
          SecureRandom.getInstance(sslData.randomAlgorithm)
        )

        S.pure(sslContext.some)
      }
}
