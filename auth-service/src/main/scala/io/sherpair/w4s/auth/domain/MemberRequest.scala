package io.sherpair.w4s.auth.domain

import io.circe.{Decoder, Encoder}
import io.circe.derivation.{deriveDecoder, deriveEncoder}

final case class MemberRequest(accountId: String, secret: Array[Byte]) extends SecretValidator

object MemberRequest {

  implicit val decoder: Decoder[MemberRequest] = deriveDecoder[MemberRequest]
  implicit val encoder: Encoder[MemberRequest] = deriveEncoder[MemberRequest]
}
