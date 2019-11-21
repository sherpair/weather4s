package io.sherpair.w4s.auth.app

import cats.effect.{Blocker, IO}
import cats.syntax.applicative._
import cats.syntax.either._
import cats.syntax.flatMap._
import io.sherpair.w4s.FakeAuth
import io.sherpair.w4s.auth.{Claims, MemberFixtures, MessageValidator}
import io.sherpair.w4s.auth.domain.MemberRequest
import io.sherpair.w4s.auth.repository.doobie.DoobieRepository
import io.sherpair.w4s.domain.ClaimContent
import org.http4s.{EntityEncoder, Request, Response, Status}
import org.http4s.Uri.unsafeFromString
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.scalatest.EitherValues

class SigninAppSpec
  extends AuthenticatorSpec
    with EitherValues
    with FakeAuth
    with MemberFixtures
    with Http4sDsl[IO] {

  implicit val memberRequestEncoder: EntityEncoder[IO, MemberRequest] = jsonEncoderOf[IO, MemberRequest]

  "POST -> /auth/signin" should {
    "return 400 when the request is malformed" in  {
      val response = DoobieRepository[IO].use { implicit DR =>
        DR.memberRepositoryOps >>= { implicit RM =>
          withAuthAppRoutes(Request[IO](POST, unsafeFromString(s"${aC.root}/signin")))
        }
      }.unsafeRunSync

      response.status shouldBe Status.BadRequest
    }
  }

  "POST -> /auth/signin" should {
    "return 404 when the endpoint is reached by the wrong method" in  {
      val response = DoobieRepository[IO].use { implicit DR =>
        DR.memberRepositoryOps >>= { implicit RM =>
          withAuthAppRoutes(Request[IO](GET, unsafeFromString(s"${aC.root}/signin")))
        }
      }.unsafeRunSync

      response.status shouldBe Status.NotFound
    }
  }

  "POST -> /auth/signin" should {
    "return 404 when no member has correspondence with the provided account" in  {
      val response = DoobieRepository[IO].use { implicit DR =>
        DR.memberRepositoryOps >>= { implicit RM =>
          RM.empty >>
            withAuthAppRoutes(
              Request[IO](GET, unsafeFromString(s"${aC.root}/signin"))
                .withEntity(MemberRequest("anAccountId", "aPassword".getBytes))
            )
        }
      }.unsafeRunSync

      response.status shouldBe Status.NotFound
    }
  }

  "POST -> /auth/signin" should {
    "return 404 when the member's secret does not match the provided secret" in  {
      val response = DoobieRepository[IO].use { implicit DR =>
        DR.memberRepositoryOps >>= { implicit RM =>
          RM.empty >>
            RM.insert(genSignupRequest) >>= { member =>
            withAuthAppRoutes(
              Request[IO](POST, unsafeFromString(s"${aC.root}/signin"))
                .withEntity(MemberRequest(member.accountId, "aPassword".getBytes))
            )
          }
        }
      }.unsafeRunSync

      response.status shouldBe Status.NotFound
    }
  }

  "POST -> /auth/signin" should {
    "return 403 when the member has correspondence with the provided credentials but still inactive" in  {
      val signupRequest = genSignupRequest

      // The original secret is set to zeroes by PasswordHash after the hashing.
      val theSecret = signupRequest.secret.clone

      val response = DoobieRepository[IO].use { implicit DR =>
        DR.memberRepositoryOps >>= { implicit RM =>
          RM.empty >>
            RM.insert(signupRequest) >>= { member =>
            withAuthAppRoutes(
              Request[IO](POST, unsafeFromString(s"${aC.root}/signin"))
                .withEntity(MemberRequest(member.accountId, theSecret))
            )
          }
        }
      }.unsafeRunSync

      response.status shouldBe Status.Forbidden
    }
  }

  "POST -> /auth/signin" should {
    "return 204 and add the Authorization header (with active member's data) when accountId and secret match" in  {
      val signupRequest = genSignupRequest

      // The original secret is set to zeroes by PasswordHash after the hashing.
      val theSecret = signupRequest.secret.clone

      val (response, claimContent) = DoobieRepository[IO].use { implicit DR =>
        DR.memberRepositoryOps >>= { implicit RM =>
          RM.empty >>
            RM.insert(signupRequest) >>= { member =>
              RM.enable(member.id) >>
                withAuthAppRoutes(
                  Request[IO](POST, unsafeFromString(s"${aC.root}/signin"))
                    .withEntity(MemberRequest(member.accountId, theSecret))
                ) >>= { response: Response[IO] =>
                  val claimContent =
                    if (response.status == Status.NoContent) validator(response)
                    else "Not Ok".asLeft[ClaimContent].pure[IO]

                  IO(response, claimContent)
                }
          }
        }
      }.unsafeRunSync

      response.status shouldBe Status.NoContent
      claimContent.unsafeRunSync shouldBe Symbol("right")
    }
  }

  private def validator(response: Response[IO]): IO[Either[String, ClaimContent]] =
    new MessageValidator[IO](
      Claims.audAuth,
      withDataForAuthorisation(Blocker.liftExecutionContext(ec))
    ) {}
    .validateMessage(response)
}
