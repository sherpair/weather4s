package io.sherpair.w4s.auth.domain

import io.circe.{Decoder, Encoder}
import io.circe.derivation.{deriveDecoder, deriveEncoder}

case class UpdateRequest(
  accountId: String,
  firstName: String,
  lastName: String,
  geoId: String,
  country: String,   // Country code,
)

object UpdateRequest {

  implicit val decoder: Decoder[UpdateRequest] = deriveDecoder[UpdateRequest]
  implicit val encoder: Encoder[UpdateRequest] = deriveEncoder[UpdateRequest]
}

