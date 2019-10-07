package io.sherpair.w4s.config

import scala.concurrent.duration.FiniteDuration

case class Cluster(name: String)

case class Engine(
  cluster: Cluster,
  defaultWindowSize: Int,
  globalLock: GlobalLock,
  healthCheck: HealthCheck,
  host: Host
)

case class GlobalLock(attempts: Int, interval: FiniteDuration, goAheadEvenIfNotAcquired: Boolean)
case class HealthCheck(attempts: Int, interval: FiniteDuration)

