package io.sherpair.w4s.auth.repository

import cats.effect.IO
import cats.syntax.flatMap._
import io.sherpair.w4s.auth.MemberFixtures
import io.sherpair.w4s.auth.repository.doobie.{DoobieRepository, TransactorSpec}


class RepositoryMemberOpsSpec extends TransactorSpec with MemberFixtures {

  "An \"empty\" op (truncate)" should {
    "remove all existing members" in  {
      val count = DoobieRepository[IO].use(
        _.memberRepositoryOps >>= { R =>

          R.insert(genSignupRequest) >>
            R.insert(genSignupRequest) >>
              R.empty >>
                R.insert(genSignupRequest) >>
                  R.insert(genSignupRequest) >>
                    R.insert(genSignupRequest) >>
                      R.count
        }
      )

      count.unsafeRunSync shouldBe 3
    }
  }
}
