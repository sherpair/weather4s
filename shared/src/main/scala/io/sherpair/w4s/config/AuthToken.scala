package io.sherpair.w4s.config

import scala.concurrent.duration.FiniteDuration

case class AuthToken(duration: FiniteDuration, publicKey: String, rsaKeyAlgorithm: String, rsaKeyStrength: Int)
