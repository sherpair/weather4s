package io.sherpair.w4s.config

import scala.concurrent.duration.FiniteDuration

case class Cluster(name: String)
case class Engine(cluster: Cluster, host: Host, defaultWindowSize: Int, globalLock: GlobalLock, healthCheck: HealthCheck)
case class GlobalLock(attempts: Int, interval: FiniteDuration, goAheadEvenIfNotAcquired: Boolean)
case class HealthCheck(attempts: Int, interval: FiniteDuration)

