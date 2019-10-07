package io.sherpair.w4s.domain

import io.circe.{Decoder, Encoder}
import io.circe.derivation.{deriveDecoder, deriveEncoder}

case class CountryCount(total: Int, available: Int, notAvailableYet: Int)

object CountryCount {
  def apply(countries: Countries): CountryCount = {
    val loadFromUser = countries.count(_.updated > epochAsLong)
    CountryCount(countries.size, loadFromUser, countries.size - loadFromUser)
  }

  implicit val decoder: Decoder[CountryCount] = deriveDecoder[CountryCount]
  implicit val encoder: Encoder[CountryCount] = deriveEncoder[CountryCount]
}
