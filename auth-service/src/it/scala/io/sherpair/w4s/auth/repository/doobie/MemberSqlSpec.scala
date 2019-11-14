package io.sherpair.w4s.auth.repository.doobie

import cats.effect.IO
import io.sherpair.w4s.auth.MemberFixtures
import io.sherpair.w4s.auth.domain.{Member, SignupRequest, UpdateRequest}

class MemberSqlSpec extends TransactorSpec with MemberFixtures {

  val memberSql = new MemberSql[IO]
  import memberSql._

  "Syntax of MemberSql statements" should {
    "be type checked" in {

      val m: Member = genMember()
      val sr = SignupRequest(m.accountId, m.firstName, m.lastName, m.email, m.geoId, m.country, Array.empty)
      check(insertStmt(sr, genSecret))
      check(findSql(m.id))

      val ur = UpdateRequest(m.accountId, m.firstName, m.lastName, m.email, m.geoId, m.country)
      check(updateStmt(m.id, ur))

      check(deleteSql(m.id))

      check(countSql)
      check(deleteSql("email", "test@sherpair.io"))
      check(emptySql)
      check(findSql("account_id", "aNickName"))
      check(listSql)
      check(subsetSql("country", 100L, 0L))
    }
  }
}
