package io.sherpair.w4s

import java.io.InputStream
import java.time.{Instant, LocalDate, LocalDateTime, ZoneOffset}
import java.time.format.DateTimeFormatter
import java.util.concurrent.{Executors, ThreadFactory}
import java.util.concurrent.atomic.AtomicLong

import scala.util.Try

import cats.effect.{Blocker, ContextShift => CS, Resource, Sync}
import cats.syntax.apply._
import fs2.io.readInputStream
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import io.circe.{Json, Printer}

package object domain {

  type BulkErrors = List[BulkError]

  type Logger[F[_]] = SelfAwareStructuredLogger[F]

  val epoch: LocalDateTime = LocalDateTime.ofEpochSecond(0L, 0, ZoneOffset.UTC)
  val epochAsLong = 0L

  val isoPattern = "yyyy-MM-dd HH:mm:ss"
  val isoFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern(isoPattern)

  val jsonPrinter: Json => String = Printer.noSpaces.copy(dropNullValues = true).print

  val leftUnit = Left(unit)
  val rightUnit = Right(unit)

  val unit: Unit = ()

  val noBulkErrors = List.empty[BulkError]

  val now: Long = Instant.now.toEpochMilli  // UTC
  val startOfTheDay: Long = toMillis(LocalDate.now.atStartOfDay)

  def fromIsoDate(date: String): Long = Try(toMillis(LocalDate.parse(date).atStartOfDay)).getOrElse(epochAsLong)
  def fromIsoDateTime(date: String): Long = Try(toMillis(LocalDateTime.parse(date))).getOrElse(epochAsLong)
  def toDate(millis: Long): LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneOffset.UTC)
  def toIsoDate(millis: Long): String = toDate(millis).format(isoFormatter)
  def toMillis(date: LocalDateTime): Long = date.toInstant(ZoneOffset.UTC).toEpochMilli

  def loadResource[F[_]: CS: Sync](resource: String)(implicit B: Blocker, L: Logger[F]): F[Array[Byte]] =
    L.info(s"Loading resource(${resource})") *>
      readInputStream(istream(resource), 4096, B).compile.to(Array)

  private val threadSequence: AtomicLong = new AtomicLong(0L)

  def blockerForIOtasks[F[_]: Sync]: Resource[F, Blocker] = blockerForIOtasks[F](1)

  def blockerForIOtasks[F[_]: Sync](nThreads: Int): Resource[F, Blocker] = {
    val tf: ThreadFactory =
      (r: Runnable) => {
        val thread = new Thread(r, s"io-task-blocker-${threadSequence.getAndIncrement}")
        thread.setDaemon(false)
        thread
      }

    val es = Sync[F].delay(nThreads match {
      case 0 => Executors.newCachedThreadPool(tf)
      case 1 => Executors.newSingleThreadExecutor(tf)
      case n => Executors.newFixedThreadPool(n, tf)
    })

    Blocker.fromExecutorService(es)
  }

  private def istream[F[_]](resource: String)(implicit S: Sync[F]): F[InputStream] =
    S.delay(
      Option(getClass.getResourceAsStream(resource)).fold(
        throw new IllegalArgumentException(s"The resource(${resource}) cannot be found")
      )(identity)
    )
}
