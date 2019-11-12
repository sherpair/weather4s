package io.sherpair.w4s.auth.repository.doobie

import scala.concurrent.duration._

import cats.effect.IO
import io.sherpair.w4s.auth.{SqlSpec, TokenFixtures, UserFixtures}
import tsec.common.SecureRandomId

class TokenSqlSpec extends SqlSpec with TokenFixtures with UserFixtures {

  val tokenSql = new TokenSql[IO]
  import tokenSql._

  "Syntax of TokenSql statements" should {
    "be type checked" in {

      val token = genToken
      check(insertStmt(token))
      check(findSql(token.id))
      check(updateStmt(token))
      check(deleteSql(token.id))

      check(deleteIfOlderThanSql(2 hours, genUser()))

      check(countSql)
      check(emptySql)
      check(findSql(SecureRandomId.Interactive.generate))
      check(listSql)
      check(subsetSql("country", 100L, 0L))
    }
  }
}
