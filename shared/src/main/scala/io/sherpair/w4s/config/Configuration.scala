package io.sherpair.w4s.config

trait Configuration {

  def authToken: AuthToken
  def httpPoolSize: Int
  def host: Host
  def plainHttp: Option[Boolean]
  def root: String
  def service: Service
  def sslData: SSLData
}
