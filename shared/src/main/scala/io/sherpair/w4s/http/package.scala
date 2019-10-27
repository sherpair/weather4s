package io.sherpair.w4s

import javax.net.ssl.SSLContext

import fs2.Stream
import io.circe.Encoder
import io.circe.syntax.EncoderOps
import io.sherpair.w4s.config.Host
import org.http4s.MediaType
import org.http4s.headers.`Content-Type`

package object http {

  val MT: `Content-Type` = `Content-Type`(MediaType.application.json)

  case class SSLData(host: Host, context: SSLContext)

  def arrayOf[F[_], T](stream: Stream[F, T])(implicit encoder: Encoder[T]): Stream[F, String] =
    Stream("[") ++ stream.map(_.asJson.noSpaces).intersperse(",") ++ Stream("]")
}
