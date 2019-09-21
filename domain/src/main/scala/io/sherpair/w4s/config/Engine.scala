package io.sherpair.w4s.config

import scala.concurrent.duration.FiniteDuration

case class Cluster(name: String)
case class Engine(cluster: Cluster, host: Host, defaultWindowSize: Int, globalLock: GlobalLock, healthCheck: HealthCheck)
case class GlobalLock(attempts: Int, interval: FiniteDuration, goAheadEvenIfNotAcquired: Boolean)
case class HealthCheck(attempts: Int, interval: FiniteDuration)

object Engine {
  def clusterName(engine: Engine): String = engine.cluster.name

  def defaultWindowSize(engine: Engine): Int = engine.defaultWindowSize

  def healthAttempts(engine: Engine): Int = engine.healthCheck.attempts
  def healthInterval(engine: Engine): FiniteDuration = engine.healthCheck.interval

  def lockAttempts(engine: Engine): Int = engine.globalLock.attempts
  def lockGoAhead(engine: Engine): Boolean = engine.globalLock.goAheadEvenIfNotAcquired
  def lockInterval(engine: Engine): FiniteDuration = engine.globalLock.interval
}
