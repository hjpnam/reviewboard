package com.hjpnam.reviewboard.service

import com.hjpnam.reviewboard.domain.data.Review
import com.hjpnam.reviewboard.http.request.CreateReviewRequest
import com.hjpnam.reviewboard.repository.ReviewRepository
import com.hjpnam.reviewboard.syntax.*
import zio.*
import zio.test.*

object ReviewServiceSpec extends ZIOSpecDefault:
  private val service = ZIO.serviceWithZIO[ReviewService]

  private val reviewRepositoryStub: ULayer[ReviewRepository] = ZLayer.succeed(new ReviewRepository {
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

  private val testCreateRequest = CreateReviewRequest(
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
    suite("ReviewServiceSpec")(
      test("create a review") {
        val program =
          for created <- service(_.create(testCreateRequest))
          yield created

        program.assert(review =>
          review.companyId == testCreateRequest.companyId &&
            review.management == testCreateRequest.management &&
            review.review == testCreateRequest.review
        )
      },
      test("update a review") {
        val program = for
          created     <- service(_.create(testCreateRequest))
          _           <- service(_.update(created.id, testCreateRequest.copy(management = 2)))
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
          created     <- service(_.create(testCreateRequest))
          _           <- service(_.delete(created.id))
          fetchedById <- service(_.getById(created.id))
        yield fetchedById

        program.assert(_.isEmpty)
      },
      test("get a review by id") {
        val program = for
          created     <- service(_.create(testCreateRequest))
          fetchedById <- service(_.getById(created.id))
        yield created -> fetchedById

        program.assert {
          case (review, Some(fetchedReview)) => review == fetchedReview
          case _                             => false
        }
      },
      test("get reviews by user ID") {
        val program = for
          review1 <- service(_.create(testCreateRequest))
          review2 <- service(_.create(testCreateRequest))
          reviews <- service(_.getByUserId(testCreateRequest.userId))
        yield (review1, review2, reviews)

        program.map { case (review1, review2, reviews) =>
          zio.test.assert(reviews)(Assertion.hasSameElements(review1 :: review2 :: Nil))
        }
      },
      test("get reviews by company ID") {
        val program = for
          review1 <- service(_.create(testCreateRequest))
          review2 <- service(_.create(testCreateRequest))
          reviews <- service(_.getByUserId(testCreateRequest.companyId))
        yield (review1, review2, reviews)

        program.map { case (review1, review2, reviews) =>
          zio.test.assert(reviews)(Assertion.hasSameElements(review1 :: review2 :: Nil))
        }
      },
      test("get all reviews") {
        val program = for
          review1 <- service(_.create(testCreateRequest))
          review2 <- service(_.create(testCreateRequest))
          reviews <- service(_.getAll)
        yield (review1, review2, reviews)

        program.map { case (review1, review2, reviews) =>
          zio.test.assert(reviews)(Assertion.hasSameElements(review1 :: review2 :: Nil))
        }
      }
    ).provide(ReviewService.live, reviewRepositoryStub)
