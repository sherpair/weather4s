package io.sherpair.w4s.config

import scala.concurrent.duration.FiniteDuration

case class HealthCheck(attempts: Int, interval: FiniteDuration)
