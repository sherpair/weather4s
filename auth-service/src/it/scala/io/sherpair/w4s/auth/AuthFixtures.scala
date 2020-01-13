package io.sherpair.w4s.auth

import java.nio.charset.StandardCharsets.UTF_8

import io.sherpair.w4s.Fixtures
import io.sherpair.w4s.auth.domain.{specials, Crypt, Kind, Member, SignupRequest, Token}
import org.scalacheck.Gen
import tsec.common.SecureRandomId
import tsec.passwordhashers.PasswordHash

trait MemberFixtures extends Fixtures {

  def fakeSecret: PasswordHash[Crypt] = PasswordHash[Crypt](genSecretAsString)

  def genSecretAsString: String =
    (1 to 4).foldLeft("") { (acc, _) =>
      acc + oneGen(Gen.alphaLowerChar) + oneGen(Gen.alphaUpperChar) + oneGen(Gen.numChar) + oneGen(Gen.oneOf(specials))
    }

  def genSecret: Array[Byte] = genSecretAsString.getBytes(UTF_8)

  def genMember(active: Boolean = true): Member = new Member(
    fakeId, alphaNum, alphaNum, alphaNum, email("sherpair.io"),
    numStr, oneElementFrom(countries), active
  )

  def genSignupRequest: SignupRequest = {
    val m: Member = genMember()
    SignupRequest(
      m.accountId, m.firstName, m.lastName, m.email, m.geoId, m.country, genSecret
    )
  }
}

trait TokenFixtures extends Fixtures with MemberFixtures {

  def genToken(member: Member, kind: Kind = Kind.Activation): Token =
    Token(fakeId, SecureRandomId.Interactive.generate, member.id, kind, futureInstant, pastInstant)
}
