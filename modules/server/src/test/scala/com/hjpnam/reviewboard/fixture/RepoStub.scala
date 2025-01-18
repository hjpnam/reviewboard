package com.hjpnam.reviewboard.fixture

import com.hjpnam.reviewboard.domain.data.User
import com.hjpnam.reviewboard.repository.UserRepository
import zio.{Task, ZIO, ZLayer}

import scala.collection.mutable

trait RepoStub:
  val testEmail = "test@email.com"
  val testUser = User(
    1L,
    testEmail,
    "1000:da70ffa630dc4793f0e4a64a8ca1aa1ae5892a29da5ce97d:2d1538385faf4154231d6fae95e09306efd841b32e9eb0bc"
  )

  val stubUserRepoLayer = ZLayer.succeed(
    new UserRepository:
      val db = mutable.Map(1L -> testUser)
      override def create(user: User): Task[User] = ZIO.succeed {
        db += (user.id -> user)
        user
      }

      override def update(id: Long, op: User => User): Task[User] = ZIO.succeed {
        val newUser = op(db(id))
        db += (newUser.id -> newUser)
        newUser
      }

      override def getById(id: Long): Task[Option[User]] = ZIO.succeed(db.get(id))

      override def getByEmail(email: String): Task[Option[User]] =
        ZIO.succeed(db.values.find(_.email == email))

      override def delete(id: Long): Task[User] = ZIO.succeed {
        val user = db(id)
        db -= id
        user
      }
  )
