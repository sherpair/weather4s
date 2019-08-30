package io.sherpair.geo.config

import scala.concurrent.duration.FiniteDuration

// Needed.
import pureconfig.generic.auto._

case class Host(address: String, port: Int)

case class Http(host: Host)

case class Cluster(name: String)
case class GlobalLock(attempts: Int, interval: FiniteDuration, goAheadEvenIfNotAcquired: Boolean)
case class ElasticSearch(cluster: Cluster, host: Host, defaultWindowSize: Int, globalLock: GlobalLock)

case class Configuration(cacheHandlerInterval: FiniteDuration, elasticSearch: ElasticSearch, http: Http)

object Configuration {
  def apply(): Configuration = pureconfig.loadConfigOrThrow[Configuration]

  def clusterName(config: Configuration): String = config.elasticSearch.cluster.name

  def defaultWindowSize(config: Configuration): Int = config.elasticSearch.defaultWindowSize

  def lockAttempts(config: Configuration): Int = config.elasticSearch.globalLock.attempts
  def lockGoAhead(config: Configuration): Boolean = config.elasticSearch.globalLock.goAheadEvenIfNotAcquired
  def lockInterval(config: Configuration): FiniteDuration = config.elasticSearch.globalLock.interval
}
