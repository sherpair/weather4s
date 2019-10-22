package io.sherpair.w4s.auth.repository.doobie

import cats.effect.IO
import io.sherpair.w4s.auth.SqlSpec

class UserSqlSpec extends SqlSpec {

  val doobieSql = new UserSql[IO]
  import doobieSql._

  "Syntax of UserSql statements" should {
    "be type checked" in {
      userGen.sample.map { user =>
        check(insertStmt(user))
        check(findSql(user.id))
        check(updateStmt(user))
        check(deleteSql(user.id))
      }

      check(deleteSql("email", "test@sherpair.io"))
      check(listSql)
      check(loginSql("account_id", "aNickName", "aPassword"))
      check(subsetSql("country", 100L, 0L))
    }
  }
}
