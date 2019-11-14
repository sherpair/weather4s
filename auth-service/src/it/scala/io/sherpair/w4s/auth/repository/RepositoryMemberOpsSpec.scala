package io.sherpair.w4s.auth.repository

import cats.effect.IO
import cats.syntax.flatMap._
import io.sherpair.w4s.auth.MemberFixtures
import io.sherpair.w4s.auth.repository.doobie.{DoobieRepository, TransactorSpec}


class RepositoryMemberOpsSpec extends TransactorSpec with MemberFixtures {

  "An \"empty\" op (truncate)" should {
    "remove all existing members" in  {
      val count = DoobieRepository[IO].use(
        _.memberRepositoryOps >>= { repositoryMemberOps =>
          val secret = genSecret

          val R: RepositoryMemberOps[IO] = repositoryMemberOps

          R.insert(genSignupRequest, secret) >>
            R.insert(genSignupRequest, secret) >>
              R.empty >>
                R.insert(genSignupRequest, secret) >>
                  R.insert(genSignupRequest, secret) >>
                    R.insert(genSignupRequest, secret) >>
                      R.count
        }
      )

      count.unsafeRunSync shouldBe 3
    }
  }
}
