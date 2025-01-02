package com.hjpnam.reviewboard.http.controller

import com.hjpnam.reviewboard.domain.data.Review
import com.hjpnam.reviewboard.http.controller.util.BackendStub
import com.hjpnam.reviewboard.http.request.CreateReviewRequest
import com.hjpnam.reviewboard.service.ReviewService
import com.hjpnam.reviewboard.syntax.*
import sttp.client3.*
import sttp.tapir.server.ServerEndpoint
import zio.*
import zio.json.*
import zio.test.*

import java.time.Instant

object ReviewControllerSpec extends ZIOSpecDefault, BackendStub:

  private val now = Instant.now()

  private val testReview = Review(
    id = 1L,
    companyId = 1L,
    userId = 1L,
    management = 1,
    culture = 1,
    salaries = 1,
    benefits = 1,
    wouldRecommend = 1,
    review = "lorem ipsum",
    created = now,
    updated = now
  )

  private val serviceStub = ZLayer.succeed(new ReviewService {
    override def create(createRequest: CreateReviewRequest): Task[Review] = ZIO.succeed(testReview)

    override def update(id: Long, updateRequest: CreateReviewRequest): Task[Review] =
      ZIO.succeed(updateRequest.toReview(id, Instant.now()))

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
  })

  private val controllerBackendStubZIO: (
      ReviewController => List[ServerEndpoint[Any, Task]]
  ) => ZIO[ReviewService, Nothing, SttpBackend[Task, Any]] = backendStubZIO(
    ReviewController.makeZIO
  )

  private val testCreateReviewRequest = CreateReviewRequest(
    companyId = 1L,
    userId = 1L,
    management = 1,
    culture = 1,
    salaries = 1,
    benefits = 1,
    wouldRecommend = 1,
    review = "lorem ipsum"
  )

  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("ReviewControllerSpec")(
      test("POST /review") {
        val program = for
          backendStub <- controllerBackendStubZIO(_.create :: Nil)
          response <- basicRequest
            .post(uri"/review")
            .body(testCreateReviewRequest.toJson)
            .send(backendStub)
        yield response.body

        program.assert(_ => true)
      },
      test("GET /review") {
        val program = for
          backendStub <- controllerBackendStubZIO(_.getAll :: Nil)
          response <- basicRequest
            .get(uri"/review")
            .send(backendStub)
        yield response.body

        program.map { respEither =>
          val review = respEither.flatMap(_.fromJson[List[Review]])
          zio.test.assert(review)(Assertion.isRight(Assertion.hasSameElements(testReview :: Nil)))
        }
      },
      test("GET /review/:id for existing review") {
        val program = for
          backendStub <- controllerBackendStubZIO(_.getById :: Nil)
          response <- basicRequest
            .get(uri"/review/1")
            .send(backendStub)
        yield response.body

        program.assert(
          _.toOption
            .flatMap(_.fromJson[Review].toOption)
            .contains(testReview)
        )
      },
      test("GET /review/:id for non-existing review") {
        val program = for
          backendStub <- controllerBackendStubZIO(_.getById :: Nil)
          response <- basicRequest
            .get(uri"/review/2")
            .send(backendStub)
        yield response.body

        program.assert(
          _.toOption
            .flatMap(_.fromJson[Review].toOption)
            .isEmpty
        )
      }
    ).provide(serviceStub)
