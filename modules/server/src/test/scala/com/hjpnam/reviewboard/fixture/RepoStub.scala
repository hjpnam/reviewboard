package com.hjpnam.reviewboard.fixture

import com.hjpnam.reviewboard.domain.data.{Review, User}
import com.hjpnam.reviewboard.repository.{ReviewRepository, UserRepository}
import zio.{Task, ULayer, ZIO, ZLayer}

import scala.collection.mutable

trait RepoStub:
  this: TestObject =>
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

  val reviewRepositoryStub: ULayer[ReviewRepository] = ZLayer.succeed(new ReviewRepository {
    var db = Map.empty[Long, Review]

    override def create(review: Review): Task[Review] = ZIO.attempt {
      val newKey    = db.keys.maxOption.fold(1L)(_ + 1)
      val newReview = review.copy(id = newKey)
      db = db + (newKey -> newReview)
      newReview
    }

    override def update(id: Long, op: Review => Review): Task[Review] = ZIO.attempt {
      val review  = db(id)
      val updated = op(review)
      db = db.updated(id, updated)
      updated
    }

    override def delete(id: Long): Task[Review] = ZIO.attempt {
      val deleted = db(id)
      db = db - id
      deleted
    }

    override def getById(id: Long): Task[Option[Review]] = ZIO.attempt {
      db.get(id)
    }

    override def getByUserId(userId: Long): Task[List[Review]] = ZIO.attempt {
      db.values.filter(_.userId == userId).toList
    }

    override def getByCompanyId(companyId: Long): Task[List[Review]] = ZIO.attempt {
      db.values.filter(_.companyId == companyId).toList
    }

    override def get: Task[List[Review]] = ZIO.attempt {
      db.values.toList
    }
  })
