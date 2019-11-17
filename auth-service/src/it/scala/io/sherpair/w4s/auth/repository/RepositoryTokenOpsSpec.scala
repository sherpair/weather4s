package io.sherpair.w4s.auth.repository

import cats.effect.IO
import cats.syntax.flatMap._
import io.sherpair.w4s.auth.TokenFixtures
import io.sherpair.w4s.auth.domain.Token
import io.sherpair.w4s.auth.repository.doobie.DoobieRepository
import io.sherpair.w4s.auth.repository.doobie.TransactorSpec
import org.postgresql.util.PSQLException

class RepositoryTokenOpsSpec extends TransactorSpec with TokenFixtures {

  "An \"empty\" op (truncate)" should {
    "remove all existing tokens" in  {
      val count = DoobieRepository[IO].use { doobieRepository =>
        doobieRepository.memberRepositoryOps >>= { R =>
          doobieRepository.tokenRepositoryOps >>= { RT =>

            def insert: IO[Token] =
              R.insert(genSignupRequest) >>= { member =>
                RT.insert(genToken(member))
              }

            insert >> insert >>
              RT.empty >>
              insert >> insert >> insert >>
              RT.count
          }
        }
      }

      count.unsafeRunSync shouldBe 3
    }
  }

  "An \"insert token\" op" should {
    "raise an error if the token is referencing a non-existing member" in  {
      val exc = intercept[PSQLException] {
        DoobieRepository[IO].use { doobieRepository =>
          doobieRepository.tokenRepositoryOps >>= { R =>
            R.insert(genToken(genMember()))
          }
        }.unsafeRunSync
      }

      exc.getMessage.toLowerCase should include("foreign key constraint")
    }
  }
}
