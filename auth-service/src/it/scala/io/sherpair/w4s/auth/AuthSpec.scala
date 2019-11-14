package io.sherpair.w4s.auth

import javax.mail.Message

import scala.concurrent.ExecutionContext.global

import cats.effect.{ContextShift, IO, Timer}
import cats.syntax.option._
import io.chrisdavenport.log4cats.noop.NoOpLogger
import io.sherpair.w4s.auth.config.{Smtp, SmtpEnv, Transporter}
import io.sherpair.w4s.domain.{unit, Logger}
import org.scalatest.{EitherValues, Matchers, OptionValues, PrivateMethodTester, WordSpec}

trait AuthSpec
  extends WordSpec
    with Matchers
    with EitherValues
    with OptionValues
    with PrivateMethodTester {

  implicit val cs: ContextShift[IO] = IO.contextShift(global)
  implicit val timer: Timer[IO] = IO.timer(global)
  implicit val logger: Logger[IO] = NoOpLogger.impl[IO]

  val fakeSmtp = Smtp(SmtpEnv("smtp.gmail.com", "587", "firstName.lastName@gmail.com", "onePassword",
    new Transporter {
      override def send(message: Message): Unit = unit
    }
  )).some
}
