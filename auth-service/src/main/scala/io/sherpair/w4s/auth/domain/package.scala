package io.sherpair.w4s.auth

import tsec.passwordhashers.jca.{JCAPasswordPlatform, SCrypt}

package object domain {

  type Crypt = SCrypt
  val Crypt: JCAPasswordPlatform[Crypt] = SCrypt

  type Members = List[Member]
  type Tokens = List[Token]
}
