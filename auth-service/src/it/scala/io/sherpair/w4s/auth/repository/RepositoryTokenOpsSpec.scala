package io.sherpair.w4s.auth.repository

import cats.effect.IO
import cats.syntax.flatMap._
import io.sherpair.w4s.auth.{AuthSpec, TokenFixtures}
import io.sherpair.w4s.auth.repository.doobie.DoobieRepository
import org.scalatest.BeforeAndAfter

class RepositoryTokenOpsSpec extends AuthSpec with BeforeAndAfter with TokenFixtures {

  "An \"empty Token repository\" op" should {
    "work as expected" in  {
      val count = DoobieRepository[IO].use(
        _.tokenRepositoryOps >>= { repositoryTokenOps =>
          implicit val R: RepositoryTokenOps[IO] = repositoryTokenOps

          R.insert(genToken) >> R.insert(genToken) >>
          R.empty >>
          R.insert(genToken) >> R.insert(genToken) >> R.insert(genToken) >>
          R.count
        }
      )

      count.unsafeRunSync shouldBe 3
    }
  }
}
