package io.sherpair.w4s.auth.domain

import enumeratum.{CirceEnum, Enum, EnumEntry}

sealed abstract class EmailType(val reason: String, val template: String) extends EnumEntry
object EmailType extends Enum[EmailType] with CirceEnum[EmailType] {

  val values = findValues

  case object Activation extends EmailType("Account activation", "activation.html")
  case object ResetSecret extends EmailType("Reset secret", "reset-secret.html")
}
