package io.sherpair.w4s.auth.domain

case class UniqueViolation(message: String) extends Exception

object UniqueViolation {
  def apply(message: String): UniqueViolation = new UniqueViolation(message)
}
