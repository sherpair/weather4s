package io.sherpair.w4s

import java.time.Instant

import cats.{FlatMap, Id}
import cats.syntax.either._
import io.sherpair.w4s.domain.now
import org.scalacheck.Gen

trait Fixtures {

  val attempts = 100  // Arbitrary

  lazy val countries = IndexedSeq(
    "AF","AX","AL","DZ","AS","AD","AO","AI","AQ","AG","AR","AM","AW","AU","AT","AZ","BS","BH","BD","BB","BY","BE",
    "BZ","BJ","BM","BT","BO","BA","BW","BV","BR","IO","BN","BG","BF","BI","KH","CM","CA","CV","KY","CF","TD","CL",
    "CN","CX","CC","CO","KM","CD","CG","CK","CR","CI","HR","CU","CY","CZ","KP","DK","DJ","DM","DO","EC","EG","SV",
    "GQ","ER","EE","SZ","ET","FK","FO","FJ","FI","FR","GF","PF","TF","GA","GM","GE","DE","GH","GI","GR","GL","GD",
    "GP","GU","GT","GG","GW","GN","GY","HT","HM","VA","HN","HK","HU","IS","IN","ID","IR","IQ","IE","IM","IL","IT",
    "JM","JP","JE","JO","KZ","KE","KI","KR","XK","KW","KG","LA","LV","LB","LS","LR","LY","LI","LT","LU","MO","MK",
    "MG","MW","MY","MV","ML","MT","MH","MQ","MR","MU","YT","MX","FM","MD","MC","MN","ME","MS","MA","MZ","MM","NA",
    "NR","NP","AN","NL","NC","NZ","NI","NE","NG","NU","NF","MP","NO","OM","PK","PW","PS","PA","PG","PY","PE","PH",
    "PN","PL","PT","PR","QA","RE","RO","RU","RW","SH","KN","LC","PM","VC","WS","SM","ST","SA","SN","RS","SC","SL",
    "SG","SK","SI","SB","SO","ZA","GS","ES","LK","SD","SR","SJ","SE","CH","SY","TW","TJ","TZ","TH","TL","TG","TK",
    "TO","TT","TN","TR","TM","TC","TV","UG","UA","AE","GB","UM","US","UY","UZ","VU","VE","VN","VG","VI","WF","EH",
    "YE","ZM","ZW"
  )

  def failedGen[T]: Either[Int, T] =
    throw new RuntimeException(s"scalacheck.Gen.sample failed after ${attempts} attempts!!")

  /* Gen[T].sample might fail (and returning None) */
  def oneGen[T](gen: => Gen[T]): T =
    FlatMap[Id].tailRecM[Int, T](attempts) { attempt =>
      gen.sample.fold(if (attempt == 0) failedGen[T] else (attempt - 1).asLeft[T])(_.asRight[Int])
    }

  val fiveMinutes: Long = 1000L * 60L * 5L  // Arbitrary 5 mins in the future or in the past

  def alpha: String = oneGen(Gen.alphaStr.suchThat(_.nonEmpty))

  def alphaNum: String = oneGen(Gen.alphaNumStr.suchThat(_.nonEmpty))

  def fakeId: Long = oneGen(Gen.posNum[Long])

  def numStr: String = oneGen(Gen.numStr.suchThat(_.nonEmpty))

  def oneElementFrom[T](xseq: IndexedSeq[T]): T = oneGen(Gen.pick(1, xseq)).head

  val UTC_2050_12_31_23_59_59 = 2556143999000L

  def futureInstant: Instant = Instant.ofEpochMilli(oneGen(Gen.chooseNum(now + fiveMinutes, UTC_2050_12_31_23_59_59)))
  def pastInstant: Instant = Instant.ofEpochMilli(oneGen(Gen.chooseNum(0L, now - fiveMinutes)))

  val unicodeSeq: IndexedSeq[Char] = (Char.MinValue to Char.MaxValue).filter(Character.isDefined)
  val unicodeChar: Gen[Char] = Gen.oneOf(unicodeSeq)

  def email(domain: String): String = s"${alpha}@${domain}"
  def unicodeStr(len: Int): String = oneGen(Gen.listOfN(len, unicodeChar)).mkString
}
