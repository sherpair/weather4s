package io.sherpair.w4s.auth.domain

import enumeratum._

sealed trait Kind extends EnumEntry

object Kind extends Enum[Kind] {

  case object Activation extends Kind
  case object ChangeEMail extends Kind
  case object Refresh extends Kind

  override val values: IndexedSeq[Kind] = findValues
}
