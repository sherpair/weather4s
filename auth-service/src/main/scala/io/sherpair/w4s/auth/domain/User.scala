package io.sherpair.w4s.auth.domain

import java.time.Instant

import io.circe.{Decoder, Encoder}
import io.circe.derivation.{deriveDecoder, deriveEncoder}

case class User(
  override val id: Long = -1L,
  accountId: String,
  firstName: String,
  lastName: String,
  email: String,
  password: String,
  geoId: String,
  country: String,   // Country code
  override val createdAt: Instant = Instant.now
)
extends Record[Long]

object User {

  implicit val decoder: Decoder[User] = deriveDecoder[User]
  implicit val encoder: Encoder[User] = deriveEncoder[User]
}