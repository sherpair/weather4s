package io.sherpair.w4s.auth.repository.doobie

import cats.effect.IO
import io.sherpair.w4s.auth.{SqlSpec, UserFixtures}

class UserSqlSpec extends SqlSpec with UserFixtures {

  val userSql = new UserSql[IO]
  import userSql._

  "Syntax of UserSql statements" should {
    "be type checked" in {

      val user = genUser()
      check(insertStmt(user))
      check(findSql(user.id))
      check(updateStmt(user))
      check(deleteSql(user.id))

      check(countSql)
      check(deleteSql("email", "test@sherpair.io"))
      check(emptySql)
      check(findSql("account_id", "aNickName"))
      check(listSql)
      check(subsetSql("country", 100L, 0L))
    }
  }
}
