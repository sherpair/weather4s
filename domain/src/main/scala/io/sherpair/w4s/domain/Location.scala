package io.sherpair.w4s.domain

import scala.util.Success

import cats.effect.Sync
import cats.syntax.applicative._
import com.sksamuel.elastic4s.{Hit, HitReader, Indexable}
import io.circe.{Encoder, Json}
import io.circe.derivation.deriveEncoder
import io.circe.syntax._

case class GeoPoint(latitude: Double, longitude: Double)

case class Location(geoId: Int, name: String, location: GeoPoint, tz: String, updated: Long = epochAsLong)

object Location {

  val indexName: String = "locations"

  val mapping: String =
    """{
      | "mappings": {
      |   "properties": {
      |      "geoId":    { "type": "integer" },
      |      "name":     { "type": "completion" },
      |      "location": { "type": "geo_point" },
      |      "tz":       { "type": "text" },
      |      "updated":  { "type": "long" }
      |    }
      |  }
      |}""".stripMargin

  implicit val geoEncoder: Encoder.AsObject[GeoPoint] = deriveEncoder[GeoPoint]

  implicit val locationEncoder: Encoder.AsObject[Location] = deriveEncoder[Location]

  implicit val indexable: Indexable[Location] = (location: Location) =>
    s"""{
      | "code":     "${location.geoId}",
      | "name":     "${location.name}",
      | "location": "${location.location}",
      | "tz":       "${location.tz}",
      | "update":   "${location.updated}"
      |}""".stripMargin

  implicit val HitReader: HitReader[Location] = (hit: Hit) =>
    Success(
      new Location(
        hit.sourceField("geoId").toString.toInt,
        hit.sourceField("name").toString,
        hit.sourceField("location").toString.toGeoPoint,
        hit.sourceField("tz").toString,
        hit.sourceField("updated").toString.toLong
      )
    )

  def encodeToJson[F[_]: Sync](locations: Locations): F[Json] = locations.asJson.pure[F]

  implicit class GeoPointConverter(location: String) {
    def toGeoPoint: GeoPoint =
      location.split("'") match {
        case Array(latitude, longitude) => GeoPoint(latitude.toDouble, longitude.toDouble)
      }
  }
}
