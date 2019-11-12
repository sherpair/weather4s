package io.sherpair.w4s.auth.domain

import io.circe.{Decoder, Encoder}
import io.circe.derivation.{deriveDecoder, deriveEncoder}

case class UserRequest(accountId: String, bytes: Array[Byte])

object UserRequest {

  implicit val decoder: Decoder[UserRequest] = deriveDecoder[UserRequest]
  implicit val encoder: Encoder[UserRequest] = deriveEncoder[UserRequest]
}
