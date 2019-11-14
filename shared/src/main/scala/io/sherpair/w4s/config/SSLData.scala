package io.sherpair.w4s.config

import io.circe.{Decoder, Encoder}
import io.circe.derivation.{deriveDecoder, deriveEncoder}

case class SSLData(
  algorithm: String, keyStore: String, randomAlgorithm: String, secret: String, `type`: String
)

object SSLData {

  implicit val decoder: Decoder[SSLData] = deriveDecoder[SSLData]
  implicit val encoder: Encoder[SSLData] = deriveEncoder[SSLData]
}
