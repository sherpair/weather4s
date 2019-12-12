package io.sherpair.w4s.auth.domain

import io.circe.{Decoder, Encoder}
import io.circe.derivation.{deriveDecoder, deriveEncoder}

final case class SignupRequest(
  accountId: String,
  firstName: String,
  lastName: String,
  email: String,
  geoId: String,
  country: String,      // Country code,
  secret: Array[Byte]
) extends SecretValidator

object SignupRequest {

  implicit val decoder: Decoder[SignupRequest] = deriveDecoder[SignupRequest]
  implicit val encoder: Encoder[SignupRequest] = deriveEncoder[SignupRequest]
}
