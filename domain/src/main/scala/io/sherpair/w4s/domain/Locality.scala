package io.sherpair.w4s.domain

import java.lang.{StringBuilder => JStringBuilder}

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
      name = filter(fields(1).trim),
      asciiOnly = filter(fields(2).trim),
      coord = GeoPoint(fields),
      tz = fields(fields.length - 2).trim,
      population = fields(fields.length - 5).trim.toIntOption.getOrElse(0),
      updated = fromIsoDate(fields(fields.length - 1).trim)
    )

  // scalastyle:off
  private def filter(s: String): String = {
    val len = s.length
    val sb = new JStringBuilder(len)
    var i = 0
    while (i < len) {
      s.charAt(i) match {
        case '"' => sb.append("\\\"")
        case '\\' => sb.append("\\\\")
        case chr => sb.append(chr)
      }
      i += 1
    }

    if (len == sb.length) s else sb.toString
  }
  // scalastyle:on magic.number

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
