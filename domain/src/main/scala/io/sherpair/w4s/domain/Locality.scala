package io.sherpair.w4s.domain

import io.circe.{Decoder, Encoder}
import io.circe.derivation.{deriveDecoder, deriveEncoder}

case class Locality(
  geoId: String,
  name: String,
  asciiOnly: String,
  coord: GeoPoint,
  tz: String,
  population: Int,
  updated: Long = epochAsLong
) {

  val toJson: String =
    s"""{
       | "geoId":"${this.geoId}",
       | "name": { "input":"${this.name}", "weight":${this.population} },
       | "asciiOnly": { "input":"${this.asciiOnly}", "weight":${this.population} },
       | "coord":[${this.coord.forLocality}],
       | "tz":"${this.tz}",
       | "population":${this.population},
       | "updated":${this.updated}
       }""".stripMargin
}

object Locality {

  // Assumes that the array's length was validated by the call site!
  def apply(fields: Array[String]): Locality =
    new Locality(
      geoId = fields(0).trim,
      name = normalize(fields(1).trim),
      asciiOnly = normalize(fields(2).trim),
      coord = GeoPoint(fields),
      tz = fields(fields.length - 2).trim,
      population = fields(fields.length - 5).trim.toIntOption.getOrElse(0),
      updated = fromIsoDate(fields(fields.length - 1).trim)
    )

  private def normalize(s: String): String = {
    val len = s.length
    val builder = new java.lang.StringBuilder(len)
    var ix = 0
    while (ix < len) {
      s.charAt(ix) match {
        case '"' => builder.append("\\\"")
        case '\\' => builder.append("\\\\")
        case chr => builder.append(chr)
      }
      ix += 1
    }

    if (len == builder.length) s else builder.toString
  }

  implicit val decoder: Decoder[Locality] = deriveDecoder[Locality]
  implicit val encoder: Encoder[Locality] = deriveEncoder[Locality]

  val mapping: String =
    """{
      | "mappings": {
      |   "properties": {
      |     "geoId":      { "type": "text" },
      |     "name":       { "type": "completion" },
      |     "asciiOnly":  { "type": "completion" },
      |     "coord":      { "type": "geo_point" },
      |     "tz":         { "type": "text" },
      |     "population": { "type": "integer" },
      |     "updated":    { "type": "long" }
      |    }
      |  }
      |}""".stripMargin
}
