package io.sherpair.w4s.auth.app

import java.nio.charset.StandardCharsets.UTF_8

import cats.effect.IO
import cats.syntax.applicative._
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.option._
import io.sherpair.w4s.FakeAuth
import io.sherpair.w4s.auth.{Authoriser, MemberFixtures}
import io.sherpair.w4s.auth.config.MaybePostman
import io.sherpair.w4s.auth.domain.{Member, MemberRequest, Members, Token, UpdateRequest}
import io.sherpair.w4s.auth.repository.{Repository, RepositoryMemberOps}
import io.sherpair.w4s.auth.repository.doobie.DoobieRepository
import org.http4s.{EntityEncoder, Request, Response, Status}
import org.http4s.Uri.unsafeFromString
import org.http4s.circe._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.http4s.syntax.kleisli._

class MemberAppSpec extends AuthenticatorSpec with MemberFixtures with FakeAuth with Http4sDsl[IO] {

  implicit val memberRequestEncoder: EntityEncoder[IO, MemberRequest] = jsonEncoderOf[IO, MemberRequest]
  implicit val updateRequestEncoder: EntityEncoder[IO, UpdateRequest] = jsonEncoderOf[IO, UpdateRequest]

  "GET -> /auth/member/{id}" should {
    "return a member given an existing member-id" in  {
      val signupRequest = genSignupRequest

      val response = DoobieRepository[IO].use { implicit R =>
        R.memberRepositoryOps >>= { implicit RM =>
          RM.empty >> RM.insert(signupRequest) >>= { member =>
            withMemberAppRoutes(
              withMasterAuth[IO],
              Request[IO](GET, unsafeFromString(s"${aC.root}/member/${member.id}"))
            )
          }
        }
      }.unsafeRunSync

      response.status shouldBe Status.Ok

      val member = response.as[Member].unsafeRunSync
      member.accountId shouldBe signupRequest.accountId
      member.email shouldBe signupRequest.email
    }
  }

  "GET -> /auth/member/{id}" should {
    "return a NotFound status code for a non-existing member-id" in  {
      val response = DoobieRepository[IO].use { implicit R =>
        R.memberRepositoryOps >>= { implicit RM =>
          RM.empty >>
            withMemberAppRoutes(
              withMasterAuth[IO],
              Request[IO](GET, unsafeFromString(s"${aC.root}/member/${fakeId}"))
            )
        }
      }.unsafeRunSync

      response.status shouldBe Status.NotFound
    }
  }

/*
  "GET -> /auth/members" should {
    "return a list of all existing members" in  {
      val signupRequest = genSignupRequest

      val response = DoobieRepository[IO].use { implicit R =>
        R.memberRepositoryOps >>= { implicit RM =>
          RM.empty >>
            RM.insert(signupRequest) >>
              RM.insert(genSignupRequest) >>
                RM.insert(genSignupRequest) >>
                  withMemberAppRoutes(
                    withMasterAuth[IO],
                    Request[IO](GET, unsafeFromString(s"${aC.root}/members"))
                  )
        }
      }.unsafeRunSync

      response.status shouldBe Status.Ok

      import io.circe.Json
      import io.circe.jawn.CirceSupportParser
      import jawnfs2._
      import org.typelevel.jawn.RawFacade

      implicit val facade: RawFacade[Json] = CirceSupportParser.facade

      val streamOfMembers = response.body.chunks.parseJsonStream.map(_.as[Member]).rethrow

      val members = streamOfMembers.compile.toList.unsafeRunSync
      members.size shouldBe 3
      members.find(_.accountId == signupRequest.accountId) shouldBe signupRequest
    }
  }

  "GET -> /auth/members" should {
    "return an empty list when there are no members" in  {
      val response = DoobieRepository[IO].use { implicit R =>
        R.memberRepositoryOps >>= { implicit RM =>
          RM.empty >>
            withMemberAppRoutes(
              withMasterAuth[IO],
              Request[IO](GET, unsafeFromString(s"${aC.root}/members"))
            )
        }
      }.unsafeRunSync

      response.status shouldBe Status.Ok
      response.as[Members].unsafeRunSync.size shouldBe 0
    }
  }
*/

  "PUT -> /auth/member/{id}" should {
    "update an existing active member and return NoContent" in  {
      val expAccountId = alpha

      val (response, memberO) = DoobieRepository[IO].use { implicit R =>
        R.memberRepositoryOps >>= { implicit RM =>
          RM.empty >> RM.insert(genSignupRequest) >>= { m =>
            RM.enable(m.id) >>
              withMemberAppRoutes(
                withMemberAuth[IO](m.id),
                Request[IO](PUT, unsafeFromString(s"${aC.root}/member/${m.id}")).withEntity(
                  UpdateRequest(expAccountId, m.firstName, m.lastName, m.geoId, m.country)
                )
              ) >>= {
              response => (IO(response), RM.find(m.id)).tupled
            }
          }
        }
      }.unsafeRunSync

      response.status shouldBe Status.NoContent

      val updateMember = memberO.get
      updateMember.accountId shouldBe expAccountId
    }
  }

  "PUT -> /auth/member/{id}" should {
    "return NotFound when trying to update an existing inactive member" in  {
      val response = DoobieRepository[IO].use { implicit R =>
        R.memberRepositoryOps >>= { implicit RM =>
          RM.empty >> RM.insert(genSignupRequest) >>= { m =>
            withMemberAppRoutes(
              withMemberAuth[IO](m.id),
              Request[IO](PUT, unsafeFromString(s"${aC.root}/member/${m.id}")).withEntity(
                UpdateRequest(alpha, m.firstName, m.lastName, m.geoId, m.country)
              )
            )
          }
        }
      }.unsafeRunSync

      response.status shouldBe Status.NotFound
    }
  }

  "POST -> /auth/email/{id}" should {
    "update an existing active member's email, set to inactive, send email and return NoContent" in  {
      // scalastyle:off
      var actualToken: Option[Token] = None
      // scalastyle:on
      val postman = new PostmanFixture {
        override val expectedToken = token => actualToken = token.some
      }

      val expectedEmail = email("sherpair.io")

      val (response, memberO) = DoobieRepository[IO].use { implicit R =>
        R.memberRepositoryOps >>= { implicit RM =>
          RM.empty >> RM.insert(genSignupRequest) >>= { m =>
            RM.enable(m.id) >>
              withMemberAppRoutes(
                withMemberAuth[IO](m.id),
                Request[IO](POST, unsafeFromString(s"${aC.root}/email/${m.id}")).withEntity(
                  MemberRequest(expectedEmail, Array.empty)
                ),
                postman
              ) >>= {
              response => (IO(response), RM.find(m.id)).tupled
            }
          }
        }
      }.unsafeRunSync

      response.status shouldBe Status.NoContent

      val updateMember = memberO.get
      updateMember.email shouldBe expectedEmail
      updateMember.active shouldBe false
      actualToken should not be None
    }
  }

  "POST -> /auth/email/{id}" should {
    "return NotFound when trying to update an existing inactive member's email" in  {
      val response = DoobieRepository[IO].use { implicit R =>
        R.memberRepositoryOps >>= { implicit RM =>
          RM.empty >> RM.insert(genSignupRequest) >>= { m =>
            withMemberAppRoutes(
              withMemberAuth[IO](m.id),
              Request[IO](POST, unsafeFromString(s"${aC.root}/email/${m.id}")).withEntity(
                MemberRequest(email("sherpair.io"), Array.empty)
              )
            )
          }
        }
      }.unsafeRunSync

      response.status shouldBe Status.NotFound
    }
  }

  "POST -> /auth/secret/{id}" should {
    "update an existing active member's secret and return NoContent after sign in with the new secret" in  {
      val expectedSecret = unicodeStr(16).getBytes(UTF_8)

      val response = DoobieRepository[IO].use { implicit R =>
        R.memberRepositoryOps >>= { implicit RM =>
          RM.empty >> RM.insert(genSignupRequest) >>= { m =>
            RM.enable(m.id) >>
              withMemberAppRoutes(
                withMemberAuth[IO](m.id),
                Request[IO](POST, unsafeFromString(s"${aC.root}/secret/${m.id}")).withEntity(
                  MemberRequest("", expectedSecret)
                )
              ) >>= { response =>
                if (response.status != Status.NoContent) response.pure[IO]
                else withAuthAppRoutes(
                  Request[IO](POST, unsafeFromString(s"${aC.root}/signin"))
                    .withEntity(MemberRequest(m.accountId, expectedSecret))
                )
            }
          }
        }
      }.unsafeRunSync

      response.status shouldBe Status.NoContent
    }
  }

  "POST -> /auth/secret/{id}" should {
    "return NotFound when trying to update an existing inactive member's secret" in  {
      val response = DoobieRepository[IO].use { implicit R =>
        R.memberRepositoryOps >>= { implicit RM =>
          RM.empty >> RM.insert(genSignupRequest) >>= { m =>
            withMemberAppRoutes(
              withMemberAuth[IO](m.id),
              Request[IO](POST, unsafeFromString(s"${aC.root}/secret/${m.id}")).withEntity(
                MemberRequest("", unicodeStr(16).getBytes(UTF_8))
              )
            )
          }
        }
      }.unsafeRunSync

      response.status shouldBe Status.NotFound
    }
  }

  "DELETE -> /auth/member/{id}" should {
    "delete a member given an existing member-id and return NoContent" in  {
      val (response, memberO) = DoobieRepository[IO].use { implicit R =>
        R.memberRepositoryOps >>= { implicit RM =>
          RM.empty >> RM.insert(genSignupRequest) >>= { member =>
            withMemberAppRoutes(
              withMemberAuth[IO](member.id),
              Request[IO](DELETE, unsafeFromString(s"${aC.root}/member/${member.id}"))
            ) >>= {
              response => (IO(response), RM.find(member.id)).tupled
            }
          }
        }
      }.unsafeRunSync

      response.status shouldBe Status.NoContent
      memberO shouldBe None
    }
  }

  "DELETE -> /auth/member/{id}" should {
    "return NotFound when trying to delete a non-existing member-id" in  {
      val response = DoobieRepository[IO].use { implicit R =>
        R.memberRepositoryOps >>= { implicit RM =>
          val memberId = fakeId
          RM.empty >>
            withMemberAppRoutes(
              withMemberAuth[IO](memberId),
              Request[IO](DELETE, unsafeFromString(s"${aC.root}/member/${memberId}"))
            )
        }
      }.unsafeRunSync

      response.status shouldBe Status.NotFound
    }
  }

  private def withMemberAppRoutes(
      authoriser: Authoriser[IO], request: Request[IO], postman: MaybePostman = withoutPostman)(
      implicit R: Repository[IO], RM: RepositoryMemberOps[IO]
  ): IO[Response[IO]] = {
    R.tokenRepositoryOps >>= { implicit RT =>
      val (jwtAlgorithm, privateKey) = withDataForAuthenticator
      val authenticator = Authenticator[IO](jwtAlgorithm, postman, privateKey)
      Router((aC.root, authoriser(new MemberApp[IO](authenticator).routes))).orNotFound.run(request)
    }
  }
}
