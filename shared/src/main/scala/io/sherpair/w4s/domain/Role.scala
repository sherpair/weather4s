package io.sherpair.w4s.domain

import enumeratum._
import io.circe.{Decoder, Encoder}

sealed trait Role extends EnumEntry

object Role extends Enum[Role] {

  case object Master extends Role
  case object Member extends Role

  override val values: IndexedSeq[Role] = findValues

  implicit val roleDecoder: Decoder[Role] = enumeratum.Circe.decoder(this)
  implicit val roleEncoder: Encoder[Role] = enumeratum.Circe.encoder(this)
}
