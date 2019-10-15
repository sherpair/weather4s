package io.sherpair.w4s.config

case class Host(address: String, port: Int) {

  val joined: String = s"${address}:${port}"
}
