package io.sherpair.w4s.auth.app

import cats.effect.IO
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

  val validator = new MessageValidator[IO](withDataForAuthorisation, Claims.audAuth, _ => true) {}

  "POST -> /auth/signin" should {
    "return 400 when the request is malformed" in  {
      val response = DoobieRepository[IO].use { implicit DR =>
        DR.memberRepositoryOps >>= { implicit RM =>
          withAuthAppRoutes(
            withoutPostman,
            Request[IO](POST, unsafeFromString(s"${aC.root}/signin"))
          )
        }
      }.unsafeRunSync

      response.status shouldBe Status.BadRequest
    }
  }

  "POST -> /auth/signin" should {
    "return 404 when the endpoint is reached by the wrong method" in  {
      val response = DoobieRepository[IO].use { implicit DR =>
        DR.memberRepositoryOps >>= { implicit RM =>
          withAuthAppRoutes(
            withoutPostman,
            Request[IO](GET, unsafeFromString(s"${aC.root}/signin"))
          )
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
              withoutPostman,
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
              withoutPostman,
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
              withoutPostman,
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
    "add to the response's Authorization header the active member's data when accountId and secret match" in  {
      val signupRequest = genSignupRequest

      // The original secret is set to zeroes by PasswordHash after the hashing.
      val theSecret = signupRequest.secret.clone

      val (response, claimContent) = DoobieRepository[IO].use { implicit DR =>
        DR.memberRepositoryOps >>= { implicit RM =>
          RM.empty >>
            RM.insert(signupRequest) >>= { member =>
              RM.enable(member.id) >>
                withAuthAppRoutes(
                  withoutPostman,
                  Request[IO](POST, unsafeFromString(s"${aC.root}/signin"))
                    .withEntity(MemberRequest(member.accountId, theSecret))
                ) >>= { response: Response[IO] =>
                  val claimContent =
                    if (response.status == Status.Ok) validator.validateMessage(response)
                    else "Not Ok".asLeft[ClaimContent].pure[IO]

                  IO(response, claimContent)
                }
          }
        }
      }.unsafeRunSync

      response.status shouldBe Status.Ok
      claimContent.unsafeRunSync shouldBe Symbol("right")
    }
  }
}
