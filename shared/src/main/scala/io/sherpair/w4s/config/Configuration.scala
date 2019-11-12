package io.sherpair.w4s.config

trait Configuration {

  def authToken: AuthToken
  def httpPoolSize: Int
  def host: Host
  def root: String
  def service: Service
  def sslData: SSLData
}
