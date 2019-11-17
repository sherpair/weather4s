package io.sherpair.w4s.auth.app

import cats.effect.IO
import cats.syntax.apply._
import cats.syntax.flatMap._
import io.sherpair.w4s.FakeAuth
import io.sherpair.w4s.auth.MemberFixtures
import io.sherpair.w4s.auth.domain.{Member, Members, UpdateRequest}
import io.sherpair.w4s.auth.repository.RepositoryMemberOps
import io.sherpair.w4s.auth.repository.doobie.{DoobieRepository, TransactorSpec}
import org.http4s.{EntityEncoder, Request, Response, Status}
import org.http4s.Uri.unsafeFromString
import org.http4s.circe._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.http4s.syntax.kleisli._

class MemberAppSpec extends TransactorSpec with MemberFixtures with FakeAuth with Http4sDsl[IO] {

  implicit val updateRequestEncoder: EntityEncoder[IO, UpdateRequest] = jsonEncoderOf[IO, UpdateRequest]

  "GET -> /auth/member/{id}" should {
    "return a member given an existing member-id" in  {
      val signupRequest = genSignupRequest

      val response = DoobieRepository[IO].use {
        _.memberRepositoryOps >>= { implicit R =>
          R.empty >> R.insert(signupRequest) >>= { member =>
            withMemberAppRoutes(Request[IO](GET, unsafeFromString(s"${aC.root}/member/${member.id}")))
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
      val response = DoobieRepository[IO].use {
        _.memberRepositoryOps >>= { implicit R =>
          R.empty >>
            withMemberAppRoutes(Request[IO](GET, unsafeFromString(s"${aC.root}/member/${fakeId}")))
        }
      }.unsafeRunSync

      response.status shouldBe Status.NotFound
    }
  }

/*
  "GET -> /auth/members" should {
    "return a list of all existing members" in  {
      val signupRequest = genSignupRequest

      val response = DoobieRepository[IO].use {
        _.memberRepositoryOps >>= { implicit R =>
          R.empty >>
            R.insert(signupRequest) >>
              R.insert(genSignupRequest) >>
                R.insert(genSignupRequest) >>
                  withMemberAppRoutes(Request[IO](GET, unsafeFromString(s"${aC.root}/members")))
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
      val response = DoobieRepository[IO].use {
        _.memberRepositoryOps >>= { implicit R =>
          R.empty >>
            withMemberAppRoutes(Request[IO](GET, unsafeFromString(s"${aC.root}/members")))
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
      val expEmail = email("sherpair.io")

      val (response, member) = DoobieRepository[IO].use {
        _.memberRepositoryOps >>= { implicit R =>
          R.empty >> R.insert(genSignupRequest) >>= { m =>
            R.enable(m.id) >>
              withMemberAppRoutes(
                m.id,
                Request[IO](PUT, unsafeFromString(s"${aC.root}/member/${m.id}")).withEntity(
                  UpdateRequest(expAccountId, m.firstName, m.lastName, expEmail, m.geoId, m.country)
                )
              ) >>= {
                response => (IO(response), R.find(m.id)).tupled
              }
          }
        }
      }.unsafeRunSync

      response.status shouldBe Status.NoContent

      val updateMember = member.get
      updateMember.accountId shouldBe expAccountId
      updateMember.email shouldBe expEmail
    }
  }

  "PUT -> /auth/member/{id}" should {
    "return NotFound when trying to update an existing inactive member, " in  {
      val response = DoobieRepository[IO].use {
        _.memberRepositoryOps >>= { implicit R =>
          R.empty >> R.insert(genSignupRequest) >>= { m =>
            withMemberAppRoutes(
              m.id,
              Request[IO](PUT, unsafeFromString(s"${aC.root}/member/${m.id}")).withEntity(
                UpdateRequest(alpha, m.firstName, m.lastName, email("sherpair.io"), m.geoId, m.country)
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
      val (response, memberO) = DoobieRepository[IO].use {
        _.memberRepositoryOps >>= { implicit R =>
          R.empty >> R.insert(genSignupRequest) >>= { member =>
            withMemberAppRoutes(
              member.id,
              Request[IO](DELETE, unsafeFromString(s"${aC.root}/member/${member.id}"))
            ) >>= {
              response => (IO(response), R.find(member.id)).tupled
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
      val response = DoobieRepository[IO].use {
        _.memberRepositoryOps >>= { implicit R =>
          val memberId = fakeId
          R.empty >>
            withMemberAppRoutes(
              memberId,
              Request[IO](DELETE, unsafeFromString(s"${aC.root}/member/${memberId}"))
            )
        }
      }.unsafeRunSync

      response.status shouldBe Status.NotFound
    }
  }

  private def withMemberAppRoutes(
    request: Request[IO])(implicit R: RepositoryMemberOps[IO]
  ): IO[Response[IO]] =
    Router((aC.root, new MemberApp[IO](withMasterAuth, withMemberAuth).routes)).orNotFound.run(request)

  private def withMemberAppRoutes(
      id: Long, request: Request[IO])(implicit R: RepositoryMemberOps[IO]
  ): IO[Response[IO]] =
    Router((aC.root, new MemberApp[IO](withMasterAuth, withMemberAuth(id)).routes)).orNotFound.run(request)
}
