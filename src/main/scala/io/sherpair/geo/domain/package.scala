package io.sherpair.geo

import java.time.{Instant, LocalDateTime, ZoneOffset}
import java.time.format.DateTimeFormatter

package object domain {

  type Countries = List[Country]
  type Locations = List[Location]

  val isoPattern = "yyyy-MM-dd HH:mm:ss"
  val isoFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern(isoPattern)

  val epoch: LocalDateTime = LocalDateTime.ofEpochSecond(0L, 0, ZoneOffset.UTC)
  val epochAsLong = 0L

  def toDate(millis: Long): LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneOffset.UTC)
  def toIsoDate(millis: Long): String = toDate(millis).format(isoFormatter)
  def toMillis(date: LocalDateTime): Long = date.toInstant(ZoneOffset.UTC).toEpochMilli

  def unit: Unit = ()
}
