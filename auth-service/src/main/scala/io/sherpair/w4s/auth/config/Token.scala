package io.sherpair.w4s.auth.config

import scala.concurrent.duration.FiniteDuration

case class Token(
  duration: FiniteDuration,

  // Must elapse before the member can request an email sending
  // with a new token. For activation, reset secret, ...
  rateLimit: FiniteDuration
)
