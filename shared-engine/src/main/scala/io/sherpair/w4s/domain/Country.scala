package io.sherpair.w4s.domain

import cats.effect.Sync
import cats.syntax.applicative._
import io.circe.{Decoder, Encoder, HCursor}
import io.circe.derivation.{deriveDecoder, deriveEncoder}
import io.circe.jawn.decode
import io.sherpair.w4s.config.Config4e
import io.sherpair.w4s.domain.Analyzer.stop
import io.sherpair.w4s.types.Countries

case class Country(
  code: String,
  name: String,
  analyzer: Analyzer,
  localities: Long,
  updated: Long
)

object Country {
  // The engine requires lowercase index names, and w4s uses the country code as index name
  def apply(code: String, name: String, analyzer: Analyzer = stop): Country =
    new Country(code.toLowerCase, name, analyzer, 0, epochAsLong)

  val indexName = "countries"

  val countryUnderLoadOrUpdate = epochAsLong - 1L

  val numberOfCountries = 245
  val requirement = s"Something wrong happened!! Countries should be at least ${numberOfCountries}"

  implicit val decoder: Decoder[Country] = deriveDecoder[Country]
  implicit val encoder: Encoder[Country] = deriveEncoder[Country]

  def decodeFromJson[F[_]: Sync](json: String)(implicit C: Config4e): F[Countries] = {
    def resolveAnalyzer(country: String, name: String): Analyzer =
      Analyzer.withNameOption(name.trim.toLowerCase) match {
        case Some(analyzer) => analyzer
        case _ => throw W4sError(s"The analyzer(${name}) for Country(${country}) is unknown!!")  // Fatal error!!
      }

    implicit val decoder: Decoder[Country] =
      (hCursor: HCursor) =>
        for {
          code <- hCursor.get[String]("code")
          name <- hCursor.get[String]("name")
          analyzer <- hCursor.getOrElse[String]("analyzer")(C.defaultAnalyzer.entryName)
        }
        // Fatal error (with NoSuchElementException) if the Analyzer is unknown.
        yield Country(code.toLowerCase, name, resolveAnalyzer(name, analyzer))

    decode[Countries](json) match {
      case Left(error) => Sync[F].raiseError(error)
      case Right(countries) => countries.pure[F]
    }
  }
}
