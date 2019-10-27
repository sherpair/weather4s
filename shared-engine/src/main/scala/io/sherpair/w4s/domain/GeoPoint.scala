package io.sherpair.w4s.domain

import io.circe.{Decoder, Encoder}
import io.circe.derivation.{deriveDecoder, deriveEncoder}

case class GeoPoint(lat: Double, lon: Double) {

  val forLocality: String = s"${this.lon},${this.lat}"
}

// scalastyle:off magic.number
object GeoPoint {

  def apply(fields: Array[String]): GeoPoint =
    fields(5).trim.toDoubleOption.fold(
      //  alternatenames is empty
      for { lat <- fields(3).trim.toDoubleOption; lon <- fields(4).trim.toDoubleOption } yield new GeoPoint(lat, lon)
    )(
      lon => fields(4).trim.toDoubleOption.map(new GeoPoint(_, lon))
    ).getOrElse(GeoPoint(0.0, 0.0))

  implicit val geoDecoder: Decoder[GeoPoint] = deriveDecoder[GeoPoint]
  implicit val geoEncoder: Encoder[GeoPoint] = deriveEncoder[GeoPoint]

//  implicit class GeoPointConverter(locality: String) {
//    def toGeoPoint: GeoPoint =
//      locality.split("'") match {
//        case Array(latitude, longitude) => GeoPoint(latitude.toDouble, longitude.toDouble)
//      }
//  }
}
// scalastyle:on magic.number
