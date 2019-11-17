package io.sherpair.w4s.auth

import java.nio.charset.StandardCharsets.UTF_8

import io.sherpair.w4s.Fixtures
import io.sherpair.w4s.auth.domain.{Crypt, Member, SignupRequest, Token}
import tsec.common.SecureRandomId
import tsec.passwordhashers.PasswordHash

trait MemberFixtures extends Fixtures {

  def fakeSecret: PasswordHash[Crypt] = PasswordHash[Crypt](unicodeStr(16))

  def genMember(active: Boolean = true): Member = new Member(
    fakeId, alphaNum, alphaNum, alphaNum, email("sherpair.io"),
    numStr, oneElementFrom(countries), active
  )

  def genSignupRequest: SignupRequest = {
    val m: Member = genMember()
    SignupRequest(
      m.accountId, m.firstName, m.lastName, m.email, m.geoId, m.country, unicodeStr(16).getBytes(UTF_8)
    )
  }
}

trait TokenFixtures extends Fixtures with MemberFixtures {

  def genToken(member: Member): Token =
    Token(fakeId, SecureRandomId.Interactive.generate, member.id, futureInstant, pastInstant)
}
