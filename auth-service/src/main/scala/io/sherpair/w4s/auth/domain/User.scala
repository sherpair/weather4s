package io.sherpair.w4s.auth.domain

import java.time.Instant

import io.circe.{Decoder, Encoder}
import io.circe.derivation.{deriveDecoder, deriveEncoder}
import io.circe.syntax._
import io.sherpair.w4s.domain.{ClaimContent, Role}

case class User(
  id: Long = -1L,
  accountId: String,
  firstName: String,
  lastName: String,
  email: String,
  geoId: String,
  country: String,   // Country code,
  active: Boolean = false,
  role: Role = Role.Member,
  createdAt: Instant = Instant.now
) {

  lazy val claimContent: String =
    new ClaimContent(id, accountId, firstName, lastName, geoId, country, role, createdAt).asJson.noSpaces
}

object User {

  implicit val decoder: Decoder[User] = deriveDecoder[User]
  implicit val encoder: Encoder[User] = deriveEncoder[User]
}
