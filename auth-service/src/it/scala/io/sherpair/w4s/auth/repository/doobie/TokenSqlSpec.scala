package io.sherpair.w4s.auth.repository.doobie

import scala.concurrent.duration._

import cats.effect.IO
import io.sherpair.w4s.auth.{MemberFixtures, TokenFixtures}
import tsec.common.SecureRandomId

class TokenSqlSpec extends TransactorSpec with TokenFixtures with MemberFixtures {

  val tokenSql = new TokenSql[IO]
  import tokenSql._

  "Syntax of TokenSql statements" should {
    "be type checked" in {

      val token = genToken(genMember())
      check(insertStmt(token))
      check(findSql(token.id))
      check(deleteSql(token.id))

      check(deleteIfOlderThanSql(2 hours, genMember()))

      check(countSql)
      check(emptySql)
      check(findSql(SecureRandomId.Interactive.generate))
      check(listSql)
      check(subsetSql("country", 100L, 0L))
    }
  }
}

