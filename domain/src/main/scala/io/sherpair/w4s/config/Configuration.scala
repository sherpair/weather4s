package io.sherpair.w4s.config

import scala.concurrent.duration.FiniteDuration

trait Configuration {

  val clusterName: String = engine.cluster.name

  val defaultWindowSize: Int = engine.defaultWindowSize

  val healthAttempts: Int = engine.healthCheck.attempts
  val healthInterval: FiniteDuration = engine.healthCheck.interval

  val lockAttempts: Int = engine.globalLock.attempts
  val lockGoAhead: Boolean = engine.globalLock.goAheadEvenIfNotAcquired
  val lockInterval: FiniteDuration = engine.globalLock.interval

  def engine: Engine
  def service: Service
}
