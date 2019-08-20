package io.sherpair.geo.config

import scala.concurrent.duration.FiniteDuration

import pureconfig.generic.auto._

case class Host(address: String, port: Int)

case class Http(host: Host)

case class Cluster(name: String)
case class GlobalLock(attempts: Int, interval: FiniteDuration)
case class ElasticSearch(cluster: Cluster, host: Host, globalLock: GlobalLock)

case class Configuration(elasticSearch: ElasticSearch, http: Http)

object Configuration {
  def apply(): Configuration = pureconfig.loadConfigOrThrow[Configuration]
}
