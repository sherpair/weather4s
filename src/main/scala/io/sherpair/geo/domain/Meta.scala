package io.sherpair.geo.domain

import io.chrisdavenport.log4cats.Logger
import io.circe.{Decoder, Encoder}
import io.circe.derivation._

case class Meta(lastEngineUpdate: Long = epochAsLong) extends AnyVal {

  def logLastEngineUpdate[F[_]: Logger]: F[Unit] =
    Logger[F].info(s"Last Engine update at(${toIsoDate(lastEngineUpdate)})")
}

object Meta {

  val requirement = "Meta index exists but no Meta record found ??"

  implicit val decoder: Decoder[Meta] = deriveDecoder[Meta]
  implicit val encoder: Encoder[Meta] = deriveEncoder[Meta]
}