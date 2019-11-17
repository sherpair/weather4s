package io.sherpair.w4s.auth

import java.util.concurrent.Executors

import scala.concurrent.ExecutionContext

import cats.effect.{ContextShift, IO, Timer}
import io.chrisdavenport.log4cats.noop.NoOpLogger
import io.sherpair.w4s.auth.domain.Member
import io.sherpair.w4s.domain.Logger
import org.http4s.EntityEncoder
import org.http4s.circe.jsonEncoderOf
import org.scalatest.{EitherValues, Matchers, OptionValues, PrivateMethodTester, WordSpec}

trait AuthSpec
  extends WordSpec
    with Matchers
    with EitherValues
    with OptionValues
    with PrivateMethodTester {

  implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())
  implicit val cs: ContextShift[IO] = IO.contextShift(ec)
  implicit val timer: Timer[IO] = IO.timer(ec)
  implicit val logger: Logger[IO] = NoOpLogger.impl[IO]

  implicit val memberEncoder: EntityEncoder[IO, Member] = jsonEncoderOf[IO, Member]
}
