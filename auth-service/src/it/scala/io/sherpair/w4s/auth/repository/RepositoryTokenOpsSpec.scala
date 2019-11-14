package io.sherpair.w4s.auth.repository

import cats.effect.IO
import cats.syntax.flatMap._
import io.sherpair.w4s.auth.TokenFixtures
import io.sherpair.w4s.auth.domain.Token
import io.sherpair.w4s.auth.repository.doobie.DoobieRepository
import io.sherpair.w4s.auth.repository.doobie.TransactorSpec

class RepositoryTokenOpsSpec extends TransactorSpec with TokenFixtures {

  "An \"empty\" op (truncate)" should {
    "remove all existing tokens" in  {
      val count = DoobieRepository[IO].use { doobieRepository =>
        doobieRepository.memberRepositoryOps >>= { repositoryMemberOps =>
          doobieRepository.tokenRepositoryOps >>= { repositoryTokenOps =>
            val secret = genSecret

            def insert: IO[Token] =
              repositoryMemberOps.insert(genSignupRequest, secret) >>= { member =>
                repositoryTokenOps.insert(genToken(member))
              }

            insert >> insert >>
              repositoryTokenOps.empty >>
                insert >> insert >> insert >>
                  repositoryTokenOps.count
          }
        }
      }

      count.unsafeRunSync shouldBe 3
    }
  }
}
