package io.sherpair.w4s.config

import scala.concurrent.duration.FiniteDuration

import io.sherpair.w4s.domain.Analyzer

trait Config4e extends Configuration {

  val clusterName: String = engine.cluster.name

  val defaultAnalyzer: Analyzer = suggestions.analyzer

  val defaultWindowSize: Int = engine.defaultWindowSize

  val healthAttemptsES: Int = engine.healthCheck.attempts
  val healthIntervalES: FiniteDuration = engine.healthCheck.interval

  val lockAttempts: Int = engine.globalLock.attempts
  val lockGoAhead: Boolean = engine.globalLock.goAheadEvenIfNotAcquired
  val lockInterval: FiniteDuration = engine.globalLock.interval

  def engine: Engine
  def suggestions: Suggestions
}
