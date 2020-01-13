package io.sherpair.w4s.auth.domain

import enumeratum.{CirceEnum, Enum, EnumEntry}

sealed trait AuthAction extends EnumEntry
object AuthAction extends Enum[AuthAction] with CirceEnum[AuthAction] {

  val values = findValues

  case object ActivationExpired extends AuthAction
  case object ChangeEMailExpired extends AuthAction
  case object Signin extends AuthAction
}

sealed trait MemberAction extends EnumEntry
object MemberAction extends Enum[MemberAction] with CirceEnum[MemberAction] {

  val values = findValues

  case object ChangeEmail extends MemberAction
  case object ChangeSecret extends MemberAction
  case object MemberDelete extends MemberAction
  case object MemberUpdate extends MemberAction
}
