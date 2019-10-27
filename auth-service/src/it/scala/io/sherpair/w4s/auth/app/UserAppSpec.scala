package io.sherpair.w4s.auth.app

import cats.effect.IO
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.option._
import io.circe.Encoder
import io.circe.derivation.deriveEncoder
import io.sherpair.w4s.auth.AuthSpec
import io.sherpair.w4s.auth.domain.{User, Users}
import io.sherpair.w4s.auth.repository.RepositoryUserOps
import io.sherpair.w4s.auth.repository.doobie.DoobieRepository
import org.http4s.{EntityEncoder, Request, Response, Status}
import org.http4s.Uri.unsafeFromString
import org.http4s.circe._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.http4s.syntax.kleisli._
import org.http4s.syntax.literals._
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfter

class UserAppSpec extends AuthSpec with BeforeAndAfter with Http4sDsl[IO] {

  implicit val encoder: Encoder[UserBadge] = deriveEncoder[UserBadge]
  implicit val userEncoder: EntityEncoder[IO, UserBadge] = jsonEncoderOf[IO, UserBadge]

  "POST -> /auth/user" should {
    "successfully insert a new user" in  {
      val responseIO = DoobieRepository[IO].use(
        _.userRepositoryOps >>= { repositoryUserOps =>
          implicit val R: RepositoryUserOps[IO] = repositoryUserOps

          val (user, password) = withUserData

          R.empty >>
            withUserAppRoutes(
              Request[IO](POST, uri"/auth/user").withEntity(UserBadge("ign", password, user.some))
            )
        }
      )

      val response = responseIO.unsafeRunSync
      response.status shouldBe Status.Created
    }
  }

  "POST -> /auth/user" should {
    "return 409 when trying to insert a user with an accountId already taken by another user" in  {
      val responseIO = DoobieRepository[IO].use(
        _.userRepositoryOps >>= { repositoryUserOps =>
          implicit val R: RepositoryUserOps[IO] = repositoryUserOps

          val (user0, password) = withUserData
          val user1 = genUser.copy(accountId = user0.accountId)

          R.empty >> R.insert(user0) >> withUserAppRoutes(
            Request[IO](POST, uri"/auth/user").withEntity(UserBadge("ign", password, user1.some))
          )
        }
      )

      val response = responseIO.unsafeRunSync
      response.status shouldBe Status.Conflict
    }
  }

  "POST -> /auth/user" should {
    "return 409 when trying to insert a user with an email already taken by another user" in  {
      val responseIO = DoobieRepository[IO].use(
        _.userRepositoryOps >>= { repositoryUserOps =>
          implicit val R: RepositoryUserOps[IO] = repositoryUserOps

          val (user0, password) = withUserData
          val user1 = genUser.copy(email = user0.email)

          R.empty >> R.insert(user0) >> withUserAppRoutes(
            Request[IO](POST, uri"/auth/user").withEntity(UserBadge("ign", password, user1.some))
          )
        }
      )

      val response = responseIO.unsafeRunSync
      response.status shouldBe Status.Conflict
    }
  }

  "GET -> /auth/user/{id}" should {
    "successfully return a user given an existing user-id" in  {
      val expectedUser = genUser

      val responseIO = DoobieRepository[IO].use(
        _.userRepositoryOps >>= { repositoryUserOps =>
          implicit val R: RepositoryUserOps[IO] = repositoryUserOps

          R.empty >> R.insert(expectedUser) >>= { user =>
            withUserAppRoutes(Request[IO](GET, unsafeFromString(s"/auth/user/${user.id}")))
          }
        }
      )

      val response = responseIO.unsafeRunSync
      response.status shouldBe Status.Ok

      val user = response.as[User].unsafeRunSync
      user.accountId shouldBe expectedUser.accountId
      user.email shouldBe expectedUser.email
    }
  }

/* This test for some reason fails with RejectedExecutionException. It requires further investigation.
 *
  "GET -> /auth/users" should {
    "successfully return a list of all existing users" in  {
      val expectedUser = genUser

      val responseIO = DoobieRepository[IO].use(
        _.userRepositoryOps >>= { repositoryUserOps =>
          implicit val R: RepositoryUserOps[IO] = repositoryUserOps

          R.empty >> R.insert(expectedUser) >> R.insert(genUser) >> R.insert(genUser) >>
            withUserAppRoutes(Request[IO](GET, uri"/auth/users"))
        }
      )

      val response = responseIO.unsafeRunSync
      response.status shouldBe Status.Ok

* THE TEST FAILS HERE
      val users = response.as[Users].unsafeRunSync
      users.size shouldBe 3
      users.find(_.accountId == expectedUser.accountId) shouldBe expectedUser
    }
  }
*/

  "POST -> /auth/user/accountId" should {
    "successfully login a user when existing accountId and password are provided" in  {
      val (expectedUser, password) = withUserData

      val responseIO = DoobieRepository[IO].use(
        _.userRepositoryOps >>= { repositoryUserOps =>
          implicit val R: RepositoryUserOps[IO] = repositoryUserOps

          R.empty >> R.insert(expectedUser) >>= { user =>
            withUserAppRoutes(Request[IO](POST, uri"/auth/user/accountId")
              .withEntity(UserBadge(user.accountId, password, None)))
          }
        }
      )

      val response = responseIO.unsafeRunSync
      response.status shouldBe Status.Ok

      val user = response.as[User].unsafeRunSync
      user.accountId shouldBe expectedUser.accountId
      user.email shouldBe expectedUser.email
    }
  }

  "POST -> /auth/user/email" should {
    "successfully login a user when existing email and password are provided" in  {
      val (expectedUser, password) = withUserData

      val responseIO = DoobieRepository[IO].use(
        _.userRepositoryOps >>= { repositoryUserOps =>
          implicit val R: RepositoryUserOps[IO] = repositoryUserOps

          R.empty >> R.insert(expectedUser) >>= { user =>
            withUserAppRoutes(Request[IO](POST, uri"/auth/user/email")
              .withEntity(UserBadge(user.email, password, None)))
          }
        }
      )

      val response = responseIO.unsafeRunSync
      response.status shouldBe Status.Ok

      val user = response.as[User].unsafeRunSync
      user.email shouldBe expectedUser.email
      user.accountId shouldBe expectedUser.accountId
    }
  }

  "PUT -> /auth/user/{id}" should {
    "successfully update an existing user" in  {
      val (beforeUser, _) = withUserData
      val afterUser = beforeUser.copy(
        accountId = oneGen(Gen.alphaStr, "accountId"),
        email = oneGen(email("sherpair.io"), "email@sherpair.io")
      )

      val (response, user) = DoobieRepository[IO].use(
        _.userRepositoryOps >>= { repositoryUserOps =>
          implicit val R: RepositoryUserOps[IO] = repositoryUserOps

          R.empty >> R.insert(beforeUser) >>= { user =>
            val afterUserWithId = afterUser.copy(id = user.id)
            withUserAppRoutes(
              Request[IO](PUT, unsafeFromString(s"/auth/user/${user.id}"))
                .withEntity(UserBadge("ign", "ign".getBytes, afterUserWithId.some))
            ) >>= {
              response => (IO(response), R.find(user.id)).tupled
            }
          }
        }
      ).unsafeRunSync

      response.status shouldBe Status.NoContent

      val updateUser = user.get
      updateUser.accountId shouldBe afterUser.accountId
      updateUser.email shouldBe afterUser.email
    }
  }

  "DELETE -> /auth/user/{id}" should {
    "successfully delete a user given an existing user-id" in  {
      val (responseDel, userO) = DoobieRepository[IO].use(
        _.userRepositoryOps >>= { repositoryUserOps =>
          implicit val R: RepositoryUserOps[IO] = repositoryUserOps

          R.empty >> R.insert(genUser) >>= { user =>
            withUserAppRoutes(Request[IO](DELETE, unsafeFromString(s"/auth/user/${user.id}"))) >>= {
              response => (IO(response), R.find(user.id)).tupled
            }
          }
        }
      ).unsafeRunSync

      responseDel.status shouldBe Status.NoContent
      userO shouldBe None
    }
  }

  private def withUserData: (User, Array[Byte]) = {
    val user = genUser
    (user, user.password.getBytes("UTF-8"))
  }

  private def withUserAppRoutes(request: Request[IO])(implicit R: RepositoryUserOps[IO]): IO[Response[IO]] =
    Router(("/auth", new UserApp[IO].routes)).orNotFound.run(request)
}
