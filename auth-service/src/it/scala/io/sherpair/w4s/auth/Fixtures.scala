package io.sherpair.w4s.auth

import java.time.Instant

import cats.{FlatMap, Id}
import cats.syntax.either._
import io.sherpair.w4s.auth.domain.{Token, User}
import org.scalacheck.Gen
import tsec.common.SecureRandomId

trait Fixtures {

  val attempts = 100  // Arbitrary

  def failedGen[T]: Either[Int, T] =
    throw new RuntimeException(s"scalacheck.Gen.sample failed after ${attempts} attempts!!")

  /* Gen[T].sample might fail (and returning None) */
  def oneGen[T](gen: Gen[T]): T =
    FlatMap[Id].tailRecM[Int, T](attempts) { attempt =>
      gen.sample.fold(if (attempt == 0) failedGen[T] else (attempt - 1).asLeft[T])(_.asRight[Int])
    }

  val fiveMinutes: Long = 1000L * 60L * 5L  // Arbitrary 5 mins in the future or in the past

  def alpha: String = oneGen(Gen.alphaStr.suchThat(_.nonEmpty))

  def alphaNum: String = oneGen(Gen.alphaNumStr.suchThat(_.nonEmpty))

  def id: Long = oneGen(Gen.posNum[Long])

  def numStr: String = oneGen(Gen.numStr.suchThat(_.nonEmpty))

  def oneElementFrom[T](xseq: IndexedSeq[T]): T = oneGen(Gen.pick(1, xseq)).head

  def futureInstant: Instant = Instant.ofEpochMilli(oneGen(Gen.chooseNum(fiveMinutes, Long.MaxValue)))
  def pastInstant: Instant = Instant.ofEpochMilli(oneGen(Gen.chooseNum(Long.MinValue, -fiveMinutes)))

  val unicodeSeq: IndexedSeq[Char] = (Char.MinValue to Char.MaxValue).filter(Character.isDefined)
  val unicodeChar: Gen[Char] = Gen.oneOf(unicodeSeq)

  def email(domain: String): String = s"${alpha}@${domain}"
  def unicodeStr(len: Int): String = oneGen(Gen.listOfN(len, unicodeChar)).mkString
}

trait TokenFixtures extends Fixtures {

  def genToken: Token = new Token(id, SecureRandomId.Interactive.generate, id, futureInstant, pastInstant)
}

trait UserFixtures extends Fixtures {

  lazy val countries = IndexedSeq(
    "Bahamas", "Bahrain", "Bangladesh", "Barbados", "Belarus", "Belgium",
    "Belize", "Benin", "Bermuda", "Bhutan", "Bolivia", "Botswana", "Brazil",
    "Bulgaria", "Burkina Faso", "Burundi", "Cameroon", "Canada", "Cape Verde"
  )

  def genUser(active: Boolean = true): User = new User(
    id, alphaNum, alphaNum, alphaNum, email("sherpair.io"),
    numStr, oneElementFrom(countries), unicodeStr(16), active
  )
}
