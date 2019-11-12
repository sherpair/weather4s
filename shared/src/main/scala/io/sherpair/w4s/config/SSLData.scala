package io.sherpair.w4s.config

case class SSLData(
  algorithm: String, keyStore: String, randomAlgorithm: String, secret: String, `type`: String
)
