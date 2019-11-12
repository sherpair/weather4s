package io.sherpair.w4s.auth.config

import scala.concurrent.duration.FiniteDuration

case class Token(
  duration: FiniteDuration,

  // Must elapse before the user can request an email sending
  // with a new one token. For activation, reset password, ...
  rateLimit: FiniteDuration
)
