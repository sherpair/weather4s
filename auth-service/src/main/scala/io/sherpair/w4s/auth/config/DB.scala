package io.sherpair.w4s.auth.config

import io.sherpair.w4s.config.{HealthCheck, Host}

case class DB(
  connectionPool: Int,
  driver: String,
  healthCheck: HealthCheck,
  host: Host,
  name: String,
  password: Array[Byte],
  user: String
) {

  val url: String = s"jdbc:postgresql://${host.joined}/${name}"
}
