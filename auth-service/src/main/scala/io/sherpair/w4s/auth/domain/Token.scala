package io.sherpair.w4s.auth.domain

import java.time.Instant

/*
import io.circe.{Decoder, Encoder}
import io.circe.derivation.{deriveDecoder, deriveEncoder}
*/
import tsec.common.SecureRandomId

case class Token(
  id: Long = -1L,
  tokenId: SecureRandomId,
  userId: Long,
  expiryDate: Instant,
  createdAt: Instant = Instant.now
)

object Token {

  def apply(tokenId: SecureRandomId, userId: Long, expiryDate: Instant): Token =
    new Token(tokenId = tokenId, userId = userId, expiryDate = expiryDate)

/*
  implicit val decoder: Decoder[ActivationToken] = deriveDecoder[ActivationToken]
  implicit val encoder: Encoder[ActivationToken] = deriveEncoder[ActivationToken]
*/
}
