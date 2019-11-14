package io.sherpair.w4s.auth

import io.sherpair.w4s.Fixtures
import io.sherpair.w4s.auth.domain.{Member, SignupRequest, Token}
import tsec.common.SecureRandomId
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.SCrypt

trait MemberFixtures extends Fixtures {

  def genMember(active: Boolean = true): Member = new Member(
    fakeId, alphaNum, alphaNum, alphaNum, email("sherpair.io"),
    numStr, oneElementFrom(countries), active
  )

  def genSecret: PasswordHash[SCrypt] = PasswordHash[SCrypt](unicodeStr(16))

  def genSignupRequest: SignupRequest = {
    val m: Member = genMember()
    SignupRequest(m.accountId, m.firstName, m.lastName, m.email, m.geoId, m.country, Array.empty)
  }
}

trait TokenFixtures extends Fixtures with MemberFixtures {

  def genToken(member: Member): Token =
    Token(fakeId, SecureRandomId.Interactive.generate, member.id, futureInstant, pastInstant)
}
