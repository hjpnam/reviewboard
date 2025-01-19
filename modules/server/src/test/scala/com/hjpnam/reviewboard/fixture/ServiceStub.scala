package com.hjpnam.reviewboard.fixture

import com.auth0.jwt.exceptions.JWTVerificationException
import com.hjpnam.reviewboard.domain.data.{Review, User, UserID, UserToken}
import com.hjpnam.reviewboard.http.request.CreateReviewRequest
import com.hjpnam.reviewboard.service.{JWTService, ReviewService, UserService}
import zio.{Task, ZIO, ZLayer}

trait ServiceStub:
  this: TestObject =>

  val userServiceStub = ZLayer.succeed(new UserService {
    override def registerUser(email: String, password: String): Task[User] = ZIO.succeed(testUser)

    override def verifyPassword(email: String, password: String): Task[Boolean] =
      ZIO.succeed(email == testUser.email)

    override def updatePassword(
        email: String,
        oldPassword: String,
        newPassword: String
    ): Task[User] = ZIO.succeed(testUser.copy(email = email, hashedPassword = newPassword))

    override def deleteUser(email: String, password: String): Task[User] = ZIO.succeed(testUser)

    override def generateToken(email: String, password: String): Task[Option[UserToken]] = ZIO.when(
      email == testUser.email && password == testUser.hashedPassword
    )(ZIO.succeed(UserToken(testUser.email, "test-token", 3600L)))

    override def recoverPassword(email: String, token: String, newPassword: String): Task[Boolean] =
      ???

    override def sendPasswordRecoveryToken(email: String): Task[Unit] = ???
  })

  val testAuthToken = "valid-token"

  val jwtServiceStub = ZLayer.succeed(new JWTService {
    override def createToken(user: User): Task[UserToken] = ???

    override def verifyToken(token: String): Task[UserID] =
      ZIO.cond(
        token == testAuthToken,
        UserID(testUser.id, testUser.email),
        JWTVerificationException("invalid token")
      )
  })

  val serviceStub = ZLayer.succeed(new ReviewService:
    override def create(createRequest: CreateReviewRequest, userId: Long): Task[Review] =
      ZIO.succeed(testReview)

    override def update(id: Long, updateRequest: CreateReviewRequest): Task[Review] =
      ZIO.succeed(updateRequest.toReview(id, -1L, now))

    override def delete(id: Long): Task[Review] = ZIO.succeed(testReview)

    override def getById(id: Long): Task[Option[Review]] =
      ZIO.succeed(Option.when(id == 1L)(testReview))

    override def getByCompanyId(companyId: Long): Task[List[Review]] = ZIO.succeed {
      if companyId == 1L then testReview :: Nil
      else Nil
    }

    override def getByUserId(userId: Long): Task[List[Review]] = ZIO.succeed {
      if userId == 1L then testReview :: Nil
      else Nil
    }

    override def getAll: Task[List[Review]] = ZIO.succeed(testReview :: Nil)
  )
