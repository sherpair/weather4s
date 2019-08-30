package io.sherpair.geo.domain

import cats.effect.Sync
import cats.syntax.applicative._
import io.chrisdavenport.log4cats.Logger
import io.circe.{Decoder, Encoder, HCursor}
import io.circe.derivation.{deriveDecoder, deriveEncoder}
import io.circe.parser.decode

case class Country(code: String, name: String, updated: Long = epochAsLong)

case class CountryCount(total: Int, available: Int, notAvailableYet: Int)

object CountryCount {
  def apply(countries: Countries): CountryCount = {
    val loadFromUser = countries.count(_.updated != epochAsLong)
    CountryCount(countries.size, loadFromUser, countries.size - loadFromUser)
  }
}

object Country {
  def apply(code: String, name: String): Country = new Country(code, name, epochAsLong)

  implicit val decoder: Decoder[Country] = deriveDecoder[Country]
  implicit val encoder: Encoder[Country] = deriveEncoder[Country]

  def decodeFromJson[F[_]: Sync](json: String): F[Countries] = {
    implicit val decoder: Decoder[Country] =
      (hCursor: HCursor) =>
        for {
          code <- hCursor.get[String]("code")
          name <- hCursor.get[String]("name")
        } yield Country(code, name, epochAsLong)

    decode[Countries](json) match {
      case Left(error) => Sync[F].raiseError(error)
      case Right(countries) => countries.pure[F]
    }
  }

  def logCountOfCountries[F[_]: Logger](countries: Countries): F[Unit] = {
    val size = countries.size
    val loadedFromUser = countries.count(_.updated != epochAsLong)
    Logger[F].info(s"Countries(${size}):  uploaded(${loadedFromUser}),  not-uploaded-yet(${size - loadedFromUser})")
  }
}
