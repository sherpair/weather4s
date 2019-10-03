package io.sherpair.w4s.domain

import cats.effect.Sync
import cats.syntax.applicative._
import io.circe.{Decoder, Encoder, HCursor}
import io.circe.derivation.{deriveDecoder, deriveEncoder}
import io.circe.jawn.decode

case class Country(code: String, name: String, localities: Long = 0L, updated: Long = epochAsLong)

case class CountryCount(total: Int, available: Int, notAvailableYet: Int)

object CountryCount {
  def apply(countries: Countries): CountryCount = {
    val loadFromUser = countries.count(_.updated > epochAsLong)
    CountryCount(countries.size, loadFromUser, countries.size - loadFromUser)
  }

  implicit val decoder: Decoder[CountryCount] = deriveDecoder[CountryCount]
  implicit val encoder: Encoder[CountryCount] = deriveEncoder[CountryCount]
}

object Country {
  // The engine requires lowercase index names, and w4s uses the country code as index name
  def apply(code: String, name: String): Country = new Country(code.toLowerCase, name, 0, epochAsLong)

  val indexName = "countries"

  val countryUnderLoadOrUpdate = epochAsLong - 1L

  val numberOfCountries = 245
  val requirement = s"Something wrong happened!! Countries should be at least ${numberOfCountries}"

  implicit val decoder: Decoder[Country] = deriveDecoder[Country]
  implicit val encoder: Encoder[Country] = deriveEncoder[Country]

  def decodeFromJson[F[_]: Sync](json: String): F[Countries] = {
    implicit val decoder: Decoder[Country] =
      (hCursor: HCursor) =>
        for {
          code <- hCursor.get[String]("code")
          name <- hCursor.get[String]("name")
        } yield Country(code.toLowerCase, name, 0L, epochAsLong)

    decode[Countries](json) match {
      case Left(error) => Sync[F].raiseError(error)
      case Right(countries) => countries.pure[F]
    }
  }
}
