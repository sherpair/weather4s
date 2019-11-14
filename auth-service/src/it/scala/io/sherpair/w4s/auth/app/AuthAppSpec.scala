package io.sherpair.w4s.auth.app

import cats.effect.IO
import cats.syntax.apply._
import cats.syntax.flatMap._
import io.sherpair.w4s.FakeAuth
import io.sherpair.w4s.auth.MemberFixtures
import io.sherpair.w4s.auth.domain.{Member, UpdateRequest}
import io.sherpair.w4s.auth.repository.RepositoryMemberOps
import io.sherpair.w4s.auth.repository.doobie.{DoobieRepository, TransactorSpec}
import org.http4s.{EntityEncoder, Request, Response, Status}
import org.http4s.Uri.unsafeFromString
import org.http4s.circe._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.http4s.syntax.kleisli._

class AuthAppSpec extends TransactorSpec with MemberFixtures with FakeAuth with Http4sDsl[IO] {

  implicit val memberEncoder: EntityEncoder[IO, Member] = jsonEncoderOf[IO, Member]
  implicit val updateRequestEncoder: EntityEncoder[IO, UpdateRequest] = jsonEncoderOf[IO, UpdateRequest]
/*

  "POST -> /auth/member" should {
    "successfully insert a new member" in  {
      val responseIO = DoobieRepository[IO].use(
        _.memberRepositoryOps >>= { repositoryMemberOps =>
          implicit val R: RepositoryMemberOps[IO] = repositoryMemberOps

          val (member, secret) = withMemberData

          R.empty >>
            withMemberAppRoutes(
              Request[IO](POST, unsafeFromString(s"${C.root}/member").withEntity(MemberBadge("ign", secret, member.some))
            )
        }
      )

      val response = responseIO.unsafeRunSync
      response.status shouldBe Status.Created
    }
  }

  "POST -> /auth/member" should {
    "return 409 when trying to insert a member with an accountId already taken by another member" in  {
      val responseIO = DoobieRepository[IO].use(
        _.memberRepositoryOps >>= { repositoryMemberOps =>
          implicit val R: RepositoryMemberOps[IO] = repositoryMemberOps

          val (member0, secret) = withMemberData
          val member1 = genMember.copy(accountId = member0.accountId)

          R.empty >> R.insert(member0) >> withMemberAppRoutes(
            Request[IO](POST, unsafeFromString(s"${C.root}/member").withEntity(MemberBadge("ign", secret, member1.some))
          )
        }
      )

      val response = responseIO.unsafeRunSync
      response.status shouldBe Status.Conflict
    }
  }

  "POST -> /auth/member" should {
    "return 409 when trying to insert a member with an email already taken by another member" in  {
      val responseIO = DoobieRepository[IO].use(
        _.memberRepositoryOps >>= { repositoryMemberOps =>
          implicit val R: RepositoryMemberOps[IO] = repositoryMemberOps

          val (member0, secret) = withMemberData
          val member1 = genMember.copy(email = member0.email)

          R.empty >> R.insert(member0) >> withMemberAppRoutes(
            Request[IO](POST, unsafeFromString(s"${C.root}/member").withEntity(MemberBadge("ign", secret, member1.some))
          )
        }
      )

      val response = responseIO.unsafeRunSync
      response.status shouldBe Status.Conflict
    }
  }

  "POST -> /auth/member/accountId" should {
    "successfully login a member when existing accountId and secret are provided" in  {
      val (expectedMember, secret) = withMemberData

      val responseIO = DoobieRepository[IO].use(
        _.memberRepositoryOps >>= { repositoryMemberOps =>
          implicit val R: RepositoryMemberOps[IO] = repositoryMemberOps

          R.empty >> R.insert(expectedMember) >>= { member =>
            withMemberAppRoutes(Request[IO](POST, unsafeFromString(s"${C.root}/member/accountId")
              .withEntity(MemberBadge(member.accountId, secret, None)))
          }
        }
      )

      val response = responseIO.unsafeRunSync
      response.status shouldBe Status.Ok

      val member = response.as[Member].unsafeRunSync
      member.accountId shouldBe expectedMember.accountId
      member.email shouldBe expectedMember.email
    }
  }

  "POST -> /auth/member/email" should {
    "successfully login a member when existing email and secret are provided" in  {
      val (expectedMember, secret) = withMemberData

      val responseIO = DoobieRepository[IO].use(
        _.memberRepositoryOps >>= { repositoryMemberOps =>
          implicit val R: RepositoryMemberOps[IO] = repositoryMemberOps

          R.empty >> R.insert(expectedMember) >>= { member =>
            withMemberAppRoutes(Request[IO](POST, unsafeFromString(s"${C.root}/member/email")
              .withEntity(MemberBadge(member.email, secret, None)))
          }
        }
      )

      val response = responseIO.unsafeRunSync
      response.status shouldBe Status.Ok

      val member = response.as[Member].unsafeRunSync
      member.email shouldBe expectedMember.email
      member.accountId shouldBe expectedMember.accountId
    }
  }
*/
}
