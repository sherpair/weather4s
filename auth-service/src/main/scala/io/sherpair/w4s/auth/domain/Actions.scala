package io.sherpair.w4s.auth.domain

import enumeratum.{CirceEnum, Enum, EnumEntry}

sealed trait AuthAction extends EnumEntry
object AuthAction extends Enum[AuthAction] with CirceEnum[AuthAction] {

  val values = findValues

  case object ActivationExpired extends AuthAction
  case object ResetSecret extends AuthAction
  case object Signin extends AuthAction
}

sealed trait UserAction extends EnumEntry
object UserAction extends Enum[UserAction] with CirceEnum[UserAction] {

  val values = findValues

  case object UserDelete extends UserAction
  case object UserUpdate extends UserAction
}
