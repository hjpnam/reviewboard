package com.hjpnam.reviewboard.repository

import com.hjpnam.reviewboard.domain.data.User
import com.hjpnam.reviewboard.syntax.*
import com.hjpnam.reviewboard.util.Gen
import zio.*
import zio.test.*

object UserRepositorySpec extends ZIOSpecDefault, RepositorySpec, Gen:
  override val initScriptPath = "sql/user.sql"

  private def genUser: User =
    User(
      id = genLong(),
      email = s"${genString(5)}@${genString(5)}.com",
      hashedPassword = genString(8)
    )

  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("UserRepositorySpec")(
      test("create a user") {
        val testUser = genUser
        val program = for
          repo <- ZIO.service[UserRepository]
          user <- repo.create(testUser)
        yield user

        program.assert { case User(_, email, pw) =>
          email == testUser.email &&
          pw == testUser.hashedPassword
        }
      },
      test("get a user by ID") {
        val testUser = genUser
        val program = for
          repo        <- ZIO.service[UserRepository]
          user        <- repo.create(testUser)
          fetchedById <- repo.getById(user.id)
        yield user -> fetchedById

        program.assert {
          case (user, Some(fetchedUser)) => user == fetchedUser
          case _                         => false
        }
      },
      test("get a user by email") {
        val testUser = genUser
        val program = for
          repo           <- ZIO.service[UserRepository]
          user           <- repo.create(testUser)
          fetchedByEmail <- repo.getByEmail(user.email)
        yield user -> fetchedByEmail

        program.assert {
          case (user, Some(fetchedUser)) => user == fetchedUser
          case _                         => false
        }
      },
      test("delete a user") {
        val testUser = genUser
        val program = for
          repo        <- ZIO.service[UserRepository]
          user        <- repo.create(testUser)
          _           <- repo.delete(user.id)
          fetchedById <- repo.getById(user.id)
        yield fetchedById

        program.assert(_.isEmpty)
      }
    ).provide(UserRepository.live, dataSourceLayer, Repository.quillLayer, Scope.default)
