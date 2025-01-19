package com.hjpnam.reviewboard.service

import com.hjpnam.reviewboard.domain.data.Review
import com.hjpnam.reviewboard.fixture.{RepoStub, TestObject}
import com.hjpnam.reviewboard.http.request.CreateReviewRequest
import com.hjpnam.reviewboard.repository.ReviewRepository
import com.hjpnam.reviewboard.syntax.*
import zio.*
import zio.test.*

object ReviewServiceSpec extends ZIOSpecDefault, TestObject, RepoStub:
  private val service = ZIO.serviceWithZIO[ReviewService]

  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("ReviewServiceSpec")(
      test("create a review") {
        val program =
          for created <- service(_.create(testCreateReviewRequest, 1L))
          yield created

        program.assert(review =>
          review.companyId == testCreateReviewRequest.companyId &&
            review.management == testCreateReviewRequest.management &&
            review.review == testCreateReviewRequest.review
        )
      },
      test("update a review") {
        val program = for
          created     <- service(_.create(testCreateReviewRequest, 1L))
          _           <- service(_.update(created.id, testCreateReviewRequest.copy(management = 2)))
          fetchedById <- service(_.getById(created.id))
        yield created -> fetchedById

        program.assert {
          case (review, Some(fetchedReview)) =>
            review.id == fetchedReview.id &&
            review.companyId == fetchedReview.companyId &&
            fetchedReview.management == 2
          case _ => false
        }
      },
      test("delete a review") {
        val program = for
          created     <- service(_.create(testCreateReviewRequest, 1L))
          _           <- service(_.delete(created.id))
          fetchedById <- service(_.getById(created.id))
        yield fetchedById

        program.assert(_.isEmpty)
      },
      test("get a review by id") {
        val program = for
          created     <- service(_.create(testCreateReviewRequest, 1L))
          fetchedById <- service(_.getById(created.id))
        yield created -> fetchedById

        program.assert {
          case (review, Some(fetchedReview)) => review == fetchedReview
          case _                             => false
        }
      },
      test("get reviews by user ID") {
        val program = for
          review1 <- service(_.create(testCreateReviewRequest, 1L))
          review2 <- service(_.create(testCreateReviewRequest, 1L))
          reviews <- service(_.getByUserId(1L))
        yield (review1, review2, reviews)

        program.map { case (review1, review2, reviews) =>
          zio.test.assert(reviews)(Assertion.hasSameElements(review1 :: review2 :: Nil))
        }
      },
      test("get reviews by company ID") {
        val program = for
          review1 <- service(_.create(testCreateReviewRequest, 1L))
          review2 <- service(_.create(testCreateReviewRequest, 1L))
          reviews <- service(_.getByCompanyId(testCreateReviewRequest.companyId))
        yield (review1, review2, reviews)

        program.map { case (review1, review2, reviews) =>
          zio.test.assert(reviews)(Assertion.hasSameElements(review1 :: review2 :: Nil))
        }
      },
      test("get all reviews") {
        val program = for
          review1 <- service(_.create(testCreateReviewRequest, 1L))
          review2 <- service(_.create(testCreateReviewRequest, 1L))
          reviews <- service(_.getAll)
        yield (review1, review2, reviews)

        program.map { case (review1, review2, reviews) =>
          zio.test.assert(reviews)(Assertion.hasSameElements(review1 :: review2 :: Nil))
        }
      }
    ).provide(ReviewService.live, reviewRepositoryStub)
