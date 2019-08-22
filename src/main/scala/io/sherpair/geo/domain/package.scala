package io.sherpair.geo

import java.time.{Instant, LocalDateTime, ZoneOffset}
import java.time.format.DateTimeFormatter

package object domain {

  type Countries = List[Country]

  val isoPattern = "yyyy-MM-dd HH:mm:ss"
  val isoFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern(isoPattern)

  val epoch: LocalDateTime = LocalDateTime.ofEpochSecond(0L, 0, ZoneOffset.UTC)
  val epochAsLong = 0L

  def toDate(millis: Long): LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneOffset.UTC)
  def toMillis(date: LocalDateTime): Long = date.toInstant(ZoneOffset.UTC).toEpochMilli
}
