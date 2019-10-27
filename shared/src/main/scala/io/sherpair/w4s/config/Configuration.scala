package io.sherpair.w4s.config

import scala.concurrent.duration.FiniteDuration

trait Configuration {

  def httpPoolSize: Int
  def service: Service
}
