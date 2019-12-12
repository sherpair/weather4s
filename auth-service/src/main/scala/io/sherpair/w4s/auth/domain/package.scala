package io.sherpair.w4s.auth

import java.nio.charset.StandardCharsets.UTF_8

import tsec.passwordhashers.jca.{JCAPasswordPlatform, SCrypt}

package object domain {

  type Crypt = SCrypt
  val Crypt: JCAPasswordPlatform[Crypt] = SCrypt

  type Members = List[Member]
  type Tokens = List[Token]

  lazy val minSecretLen = 8

  lazy val specials = "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~".toSet

  trait SecretValidator {

    val secret: Array[Byte]

    lazy val hasLegalSecret: Boolean = {
      val s = new String(secret, UTF_8)
      s.length >= minSecretLen && verifyIfSecretIsLegal(s) == legal
    }

    lazy val illegalSecret =
      s"Secret must be at least $minSecretLen characters in length and ".concat(
        "contains uppercase and lowercase letters, digits and special characters"
      )

    private lazy val     legal = 0x0F
    private lazy val lowerCase = 0x01
    private lazy val upperCase = 0x02
    private lazy val     digit = 0x04
    private lazy val   special = 0x08

    private def verifyIfSecretIsLegal(secretAsString: String): Int =
      secretAsString.foldLeft(0) { (acc, chr) =>
        if (acc == legal) legal else chr match {
          case c if Character.isLowerCase(c) => acc | lowerCase
          case c if Character.isUpperCase(c) => acc | upperCase
          case c if Character.isDigit(c) => acc | digit
          case c if specials.contains(c) => acc | special
          case _ => acc
        }
      }
  }
}
