package io.sherpair.w4s

import java.time.{Instant, LocalDate, LocalDateTime, ZoneOffset}
import java.time.format.DateTimeFormatter

import scala.util.Try

import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import io.circe.{Json, Printer}

package object domain {

  type Countries = List[Country]
  type Localities = List[Locality]
  type Suggestions = List[Suggestion]

  type Logger[F[_]] = SelfAwareStructuredLogger[F]

  val epoch: LocalDateTime = LocalDateTime.ofEpochSecond(0L, 0, ZoneOffset.UTC)
  val epochAsLong = 0L

  val isoPattern = "yyyy-MM-dd HH:mm:ss"
  val isoFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern(isoPattern)

  val jsonPrinter: Json => String = Printer.noSpaces.copy(dropNullValues = true).print

  val leftUnit = Left(unit)
  val rightUnit = Right(unit)

  val unit: Unit = ()

  val now: Long = toMillis(LocalDateTime.now)
  val startOfTheDay: Long = toMillis(LocalDate.now.atStartOfDay)

  def fromIsoDate(date: String): Long = Try(toMillis(LocalDate.parse(date).atStartOfDay)).getOrElse(epochAsLong)
  def fromIsoDateTime(date: String): Long = Try(toMillis(LocalDateTime.parse(date))).getOrElse(epochAsLong)
  def toDate(millis: Long): LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneOffset.UTC)
  def toIsoDate(millis: Long): String = toDate(millis).format(isoFormatter)
  def toMillis(date: LocalDateTime): Long = date.toInstant(ZoneOffset.UTC).toEpochMilli
}
