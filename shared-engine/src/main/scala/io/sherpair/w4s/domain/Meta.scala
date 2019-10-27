package io.sherpair.w4s.domain

import io.circe.{Decoder, Encoder}
import io.circe.derivation._

case class Meta(lastEngineUpdate: Long = epochAsLong) {

  def logLastEngineUpdate[F[_]](implicit L: Logger[F]): F[Unit] =
    L.info(s"Last Engine update at(${toIsoDate(lastEngineUpdate)})")
}

object Meta {

  val id = "0"
  val indexName = "meta"

  val requirement = "Meta index exists but no Meta record found ??"

  implicit val decoder: Decoder[Meta] = deriveDecoder[Meta]
  implicit val encoder: Encoder[Meta] = deriveEncoder[Meta]
}
