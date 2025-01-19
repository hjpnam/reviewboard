package com.hjpnam.reviewboard.http.controller

import com.hjpnam.reviewboard.domain.data.Review
import com.hjpnam.reviewboard.fixture.{ServiceStub, TestObject}
import com.hjpnam.reviewboard.http.controller.util.BackendStub
import com.hjpnam.reviewboard.http.request.CreateReviewRequest
import com.hjpnam.reviewboard.service.{JWTService, ReviewService}
import sttp.client3.*
import sttp.tapir.server.ServerEndpoint
import zio.*
import zio.json.*
import zio.test.*
import zio.test.Assertion.*

object ReviewControllerSpec extends ZIOSpecDefault, BackendStub, TestObject, ServiceStub:

  private val controllerBackendStubZIO: (
      ReviewController => List[ServerEndpoint[Any, Task]]
  ) => ZIO[JWTService & ReviewService, Nothing, SttpBackend[Task, Nothing]] = backendStubZIO(
    ReviewController.makeZIO
  )

  override def spec: Spec[TestEnvironment with Scope, Any] =
    import com.hjpnam.reviewboard.util.RichSttpBackend.*

    suite("ReviewControllerSpec")(
      test("POST /review") {
        for
          backendStub <- controllerBackendStubZIO(_.create :: Nil)
          response <- backendStub.postAuth[Review]("review", testCreateReviewRequest, testAuthToken)
        yield assert(response)(isRight(equalTo(testReview)))
      },
      test("GET /review") {
        for
          backendStub <- controllerBackendStubZIO(_.getAll :: Nil)
          response    <- backendStub.getAuth[List[Review]]("review", testAuthToken)
        yield assert(response)(isRight(hasSameElements(testReview :: Nil)))
      },
      test("GET /review/:id for existing review") {
        for
          backendStub <- controllerBackendStubZIO(_.getById :: Nil)
          response    <- backendStub.getAuth[Review]("/review/1", testAuthToken)
        yield assert(response)(isRight(equalTo(testReview)))
      },
      test("GET /review/:id for non-existing review") {
        for
          backendStub <- controllerBackendStubZIO(_.getById :: Nil)
          response    <- backendStub.getAuth[Review]("/review/2", testAuthToken)
        yield assert(response)(isLeft)
      },
      test("GET /review/company/:companyId") {
        for
          backendStub <- controllerBackendStubZIO(_.getByCompanyId :: Nil)
          response    <- backendStub.getAuth[List[Review]]("/review/company/1", testAuthToken)
        yield assert(response)(isRight(hasSameElements(testReview :: Nil)))
      },
      test("GET /review/company/:companyId for company without reviews") {
        for
          backendStub <- controllerBackendStubZIO(_.getByCompanyId :: Nil)
          response    <- backendStub.getAuth[List[Review]]("/review/company/2", testAuthToken)
        yield assert(response)(isRight(isEmpty))
      },
      test("GET /review/user/:userId") {
        for
          backendStub <- controllerBackendStubZIO(_.getByUserId :: Nil)
          response    <- backendStub.getAuth[List[Review]]("/review/user/1", testAuthToken)
        yield assert(response)(isRight(hasSameElements(testReview :: Nil)))
      },
      test("GET /review/user/:userId for user without reviews") {
        for
          backendStub <- controllerBackendStubZIO(_.getByUserId :: Nil)
          response    <- backendStub.getAuth[List[Review]]("/review/user/2", testAuthToken)
        yield assert(response)(isRight(isEmpty))
      }
    ).provide(serviceStub, jwtServiceStub)
