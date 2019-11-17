package io.sherpair.w4s.auth.app

import cats.effect.IO
import cats.syntax.flatMap._
import cats.syntax.option._
import io.sherpair.w4s.FakeAuth
import io.sherpair.w4s.auth.MemberFixtures
import io.sherpair.w4s.auth.config.MaybePostman
import io.sherpair.w4s.auth.domain.{EmailType, Member, SignupRequest, Token}
import io.sherpair.w4s.auth.domain.EmailType.Activation
import io.sherpair.w4s.auth.repository.doobie.DoobieRepository
import org.http4s.{EntityEncoder, Request, Status}
import org.http4s.Uri.unsafeFromString
import org.http4s.circe._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.dsl.Http4sDsl

class SignupAppSpec extends AuthenticatorSpec with MemberFixtures with FakeAuth with Http4sDsl[IO] {

  implicit val signupRequestEncoder: EntityEncoder[IO, SignupRequest] = jsonEncoderOf[IO, SignupRequest]

  "POST -> /auth/signup" should {
    "return 400 when the request is malformed" in  {
      val response = DoobieRepository[IO].use { implicit DR =>
        DR.memberRepositoryOps >>= { implicit RM =>

          withAuthAppRoutes(
            withoutPostman,
            Request[IO](POST, unsafeFromString(s"${aC.root}/signup"))
          )
        }
      }.unsafeRunSync

      response.status shouldBe Status.BadRequest
    }
  }

  "POST -> /auth/signup" should {
    "return 409 when trying to insert a member with an accountId already taken by another member" in  {
      val response = DoobieRepository[IO].use { implicit DR =>
        DR.memberRepositoryOps >>= { implicit RM =>

          val signupRequest0 = genSignupRequest
          val signupRequest1 = genSignupRequest.copy(accountId = signupRequest0.accountId)

          RM.empty >>
            RM.insert(signupRequest0) >>
            withAuthAppRoutes(
              withoutPostman,
              Request[IO](POST, unsafeFromString(s"${aC.root}/signup")).withEntity(signupRequest1)
            )
        }
      }.unsafeRunSync

      response.status shouldBe Status.Conflict
    }
  }

  "POST -> /auth/signup" should {
    "return 409 when trying to insert a member with an email already taken by another member" in  {
      val response = DoobieRepository[IO].use { implicit DR =>
        DR.memberRepositoryOps >>= { implicit RM =>

          val signupRequest0 = genSignupRequest
          val signupRequest1 = genSignupRequest.copy(email = signupRequest0.email)

          RM.empty >>
            RM.insert(signupRequest0) >>
            withAuthAppRoutes(
              withoutPostman,
              Request[IO](POST, unsafeFromString(s"${aC.root}/signup")).withEntity(signupRequest1)
            )
        }
      }.unsafeRunSync

      response.status shouldBe Status.Conflict
    }
  }

  "POST -> /auth/signup" should {
    "insert a new member but set as inactive" in  {
      val response = DoobieRepository[IO].use { implicit DR =>
        DR.memberRepositoryOps >>= { implicit RM =>
          RM.empty >>
            withAuthAppRoutes(
              withoutPostman,
              Request[IO](POST, unsafeFromString(s"${aC.root}/signup")).withEntity(genSignupRequest)
            )
        }
      }.unsafeRunSync

      response.status shouldBe Status.Created
      val member = response.as[Member].unsafeRunSync
      member.active shouldBe false
    }
  }

  "POST -> /auth/signup" should {
    "insert a new member and send an activation-token email" in  {
      // scalastyle:off
      var actualUrl: Option[String] = None
      // scalastyle:on
      val postman: MaybePostman = new MaybePostman {
        override def sendEmail(token: Token, member: Member, emailType: EmailType): Option[String] = {
          actualUrl = url(emailType.segment, token.tokenId).some
          actualUrl
        }
      }

      val expectedUrl = s"${postman.path}/${Activation.segment}"

      val response = DoobieRepository[IO].use { implicit DR =>
        DR.memberRepositoryOps >>= { implicit RM =>
          RM.empty >>
            withAuthAppRoutes(
              postman,
              Request[IO](POST, unsafeFromString(s"${aC.root}/signup")).withEntity(genSignupRequest)
            )
        }
      }.unsafeRunSync

      response.status shouldBe Status.Created;
      actualUrl.value should startWith(expectedUrl)
    }
  }

  "GET -> /auth/account-activation" should {
    "return 404 when the resource url is incomplete" in  {
      val response = DoobieRepository[IO].use { implicit DR =>
        DR.memberRepositoryOps >>= { implicit RM =>

          withAuthAppRoutes(
            withoutPostman,
            Request[IO](GET, unsafeFromString(s"${aC.root}/${Activation.segment}/"))
          )
        }
      }.unsafeRunSync

      response.status shouldBe Status.NotFound
    }
  }

  "GET -> /auth/account-activation" should {
    "return 404 when the endpoint is reached by the wrong method" in  {
      val response = DoobieRepository[IO].use { implicit DR =>
        DR.memberRepositoryOps >>= { implicit RM =>

          withAuthAppRoutes(
            withoutPostman,
            Request[IO](POST, unsafeFromString(s"${aC.root}/${Activation.segment}/1234567890"))
          )
        }
      }.unsafeRunSync

      response.status shouldBe Status.NotFound
    }
  }

  "GET -> /auth/account-activation" should {
    "return 404 when trying to activate an account with a non-existing token" in  {
      val response = DoobieRepository[IO].use { implicit DR =>
        DR.memberRepositoryOps >>= { implicit RM =>
          RM.empty >>
            withAuthAppRoutes(
              withoutPostman,
              Request[IO](GET, unsafeFromString(s"${aC.root}/${Activation.segment}/1234567890"))
            )
        }
      }.unsafeRunSync

      response.status shouldBe Status.NotFound
    }
  }

  "GET -> /auth/account-activation" should {
    "insert a new member and set as active once the provided token is sent back to the activation endpoint" in  {
      // scalastyle:off
      var actualToken: Option[Token] = None
      // scalastyle:on
      val postman: MaybePostman = new MaybePostman {
        override def sendEmail(token: Token, member: Member, emailType: EmailType): Option[String] = {
          actualToken = token.some
          url(emailType.segment, token.tokenId).some
        }
      }

      val (member, resp1, resp2) = DoobieRepository[IO].use { implicit DR =>
        DR.memberRepositoryOps >>= { implicit RM =>

          for {
            _ <- RM.empty
            resp1 <- withAuthAppRoutes(postman,
              Request[IO](POST, unsafeFromString(s"${aC.root}/signup")).withEntity(genSignupRequest)
            )
            resp2 <- withAuthAppRoutes(postman,
              Request[IO](GET, unsafeFromString(s"${aC.root}/${Activation.segment}/${actualToken.value.tokenId}"))
            )
            member <- RM.find(actualToken.value.memberId)
          }
            yield (member, resp1, resp2)
        }
      }.unsafeRunSync

      resp1.status shouldBe Status.Created
      resp2.status shouldBe Status.Ok
      member.value.active shouldBe true
    }
  }
}
