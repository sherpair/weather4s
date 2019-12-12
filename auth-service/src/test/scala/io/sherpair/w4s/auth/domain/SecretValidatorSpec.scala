package io.sherpair.w4s.auth.domain

import java.nio.charset.StandardCharsets.UTF_8

import io.sherpair.w4s.Fixtures
import org.scalatest.funsuite.AnyFunSuite

class SecretValidatorSpec extends AnyFunSuite with Fixtures {

  test("hasLegalSecret should be asserted for a valid secret") {
    assert(MemberRequest("anAccountId", "q3W2$validSecret".getBytes(UTF_8)).hasLegalSecret)
  }

  test("hasLegalSecret should NOT be asserted for a too short secret") {
    assertResult(false)(MemberRequest("anAccountId", "q3W2$v".getBytes(UTF_8)).hasLegalSecret)
  }

  test("hasLegalSecret should NOT be asserted for a secret without digits") {
    assertResult(false)(MemberRequest("anAccountId", "qW$invalidSecret".getBytes(UTF_8)).hasLegalSecret)
  }

  test("hasLegalSecret should NOT be asserted for a secret without uppercase letters") {
    assertResult(false)(MemberRequest("anAccountId", "qw$2invalidsecret".getBytes(UTF_8)).hasLegalSecret)
  }

  test("hasLegalSecret should NOT be asserted for a secret without lowercase letters") {
    assertResult(false)(MemberRequest("anAccountId", "QW$2INVALIDSECRET".getBytes(UTF_8)).hasLegalSecret)
  }
}
