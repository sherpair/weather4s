package io.sherpair.w4s.domain

import java.time.Instant

import io.circe.{Decoder, Encoder}
import io.circe.derivation.{deriveDecoder, deriveEncoder}

case class ClaimContent(
  id: Long,
  accountId: String,
  firstName: String,
  lastName: String,
  geoId: String,
  country: String,
  role: Role,
  createdAt: Instant
)

object ClaimContent {

  implicit val decoder: Decoder[ClaimContent] = deriveDecoder[ClaimContent]
  implicit val encoder: Encoder[ClaimContent] = deriveEncoder[ClaimContent]
}
