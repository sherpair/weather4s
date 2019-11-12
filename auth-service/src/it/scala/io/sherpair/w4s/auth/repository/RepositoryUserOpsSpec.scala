package io.sherpair.w4s.auth.repository

import cats.effect.IO
import cats.syntax.flatMap._
import io.sherpair.w4s.auth.{AuthSpec, UserFixtures}
import io.sherpair.w4s.auth.repository.doobie.DoobieRepository
import org.scalatest.BeforeAndAfter

class RepositoryUserOpsSpec extends AuthSpec with BeforeAndAfter with UserFixtures {

  "An \"empty User repository\" op" should {
    "work as expected" in  {
      val count = DoobieRepository[IO].use(
        _.userRepositoryOps >>= { repositoryUserOps =>
          implicit val R: RepositoryUserOps[IO] = repositoryUserOps

          R.insert(genUser()) >> R.insert(genUser()) >>
          R.empty >>
          R.insert(genUser()) >> R.insert(genUser()) >> R.insert(genUser()) >>
          R.count
        }
      )

      count.unsafeRunSync shouldBe 3
    }
  }
}
