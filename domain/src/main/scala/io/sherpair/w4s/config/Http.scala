package io.sherpair.w4s.config

case class Host(address: String, port: Int)

case class Http(host: Host)

object Http {
  def host(http: Http): String = s"${http.host.address}:${http.host.port}"
}
