package io.sherpair.w4s.domain

import cats.effect.Sync
import cats.syntax.applicative._
import io.circe.{Decoder, Encoder, Json}
import io.circe.derivation.{deriveDecoder, deriveEncoder}
import io.circe.syntax._

case class GeoPoint(lat: Double, lon: Double)

object GeoPoint {

  // scalastyle:off magic.number
  def apply(fields: Array[String]): Option[GeoPoint] =
    fields(5).trim.toDoubleOption.fold(
      //  alternatenames is empty
      for { lat <- fields(3).trim.toDoubleOption; lon <- fields(4).trim.toDoubleOption } yield new GeoPoint(lat, lon)
    )(
      lon => fields(4).trim.toDoubleOption.map(new GeoPoint(_, lon))
    )
  // scalastyle:on magic.number
}

case class Locality(
  geoId: String,
  name: String,
  asciiOnly: String,
  locality: Option[GeoPoint],
  tz: String,
  updated: Long = epochAsLong
)

object Locality {

  // Assumes that the array's length was validated by the call site!
  def apply(fields: Array[String]): Locality =
    new Locality(
      geoId = fields(0).trim,
      name = fields(1).trim,
      asciiOnly = fields(2).trim,
      locality = GeoPoint(fields),
      tz = fields(fields.length - 2).trim,
      updated = fromIsoDate(fields(fields.length - 1).trim)
    )

  val mapping: String =
    """{
      | "mappings": {
      |   "properties": {
      |      "geoId":     { "type": "text" },
      |      "name":      { "type": "completion" },
      |      "plainName": { "type": "completion" },
      |      "locality":  { "type": "geo_point" },
      |      "tz":        { "type": "text" },
      |      "updated":   { "type": "long" }
      |    }
      |  }
      |}""".stripMargin

  implicit val geoDecoder: Decoder[GeoPoint] = deriveDecoder[GeoPoint]
  implicit val geoEncoder: Encoder[GeoPoint] = deriveEncoder[GeoPoint]

  implicit val decoder: Decoder[Locality] = deriveDecoder[Locality]
  implicit val encoder: Encoder[Locality] = deriveEncoder[Locality]

  def encodeToJson[F[_]: Sync](localities: Localities): F[Json] = localities.asJson.pure[F]

  implicit class GeoPointConverter(locality: String) {
    def toGeoPoint: GeoPoint =
      locality.split("'") match {
        case Array(latitude, longitude) => GeoPoint(latitude.toDouble, longitude.toDouble)
      }
  }
}
