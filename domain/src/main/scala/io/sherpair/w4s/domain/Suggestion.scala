package io.sherpair.w4s.domain

import io.circe.{Decoder, Encoder, HCursor}
import io.circe.derivation.{deriveDecoder, deriveEncoder}

case class Name(input: String, weight: Int)

object Name {
  implicit val nameDecoder: Decoder[Name] = deriveDecoder[Name]
  implicit val nameEncoder: Encoder[Name] = deriveEncoder[Name]
}

case class Suggestion(name: Name, coord: GeoPoint, tz: String)

object Suggestion {

  implicit val suggestionDecoder: Decoder[Suggestion] =
    (hCursor: HCursor) =>
      for {
        name <- hCursor.get[Name]("name")
        coord <- hCursor.get[List[Double]]("coord")
        tz <- hCursor.get[String]("tz")
      }
      yield Suggestion(name, GeoPoint(lat = coord.last, lon = coord.head), tz)

  implicit val suggestionEncoder: Encoder[Suggestion] = deriveEncoder[Suggestion]
}

case class SuggestionMeta(
  text: String,
  _index: String,
  _type: String,
  _id: String,
  _score: Double,
  _source: Suggestion
)

object SuggestionMeta {

  implicit val metaDecoder: Decoder[SuggestionMeta] = deriveDecoder[SuggestionMeta]
}
