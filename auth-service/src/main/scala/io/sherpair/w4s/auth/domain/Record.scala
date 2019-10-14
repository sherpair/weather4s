package io.sherpair.w4s.auth.domain

import java.time.Instant

trait Record[K] {
  val id: K
  val createdAt: Instant
}
