package io.sherpair.w4s.auth.domain

import java.time.Instant

import tsec.common.SecureRandomId

case class Token(
  id: Long = -1L,
  tokenId: SecureRandomId,
  memberId: Long,
  kind: Kind,
  expiryDate: Instant,
  createdAt: Instant = Instant.now
)

object Token {

  def apply(tokenId: SecureRandomId, memberId: Long, kind: Kind, expiryDate: Instant): Token =
    new Token(tokenId = tokenId, memberId = memberId, kind = kind, expiryDate = expiryDate)
}
