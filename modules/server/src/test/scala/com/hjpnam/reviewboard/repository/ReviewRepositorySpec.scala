package com.hjpnam.reviewboard.repository

import com.hjpnam.reviewboard.domain.data.Review
import com.hjpnam.reviewboard.syntax.*
import com.hjpnam.reviewboard.util.Gen
import zio.*
import zio.test.*

import java.time.Instant

object ReviewRepositorySpec extends ZIOSpecDefault, RepositorySpec, Gen:
  private val testInstant = Instant.now()

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
    created = testInstant,
    updated = testInstant
  )

  private def genReview(): Review = Review(
    id = 1L,
    companyId = genLong(),
    userId = genLong(),
    management = genRating(),
    culture = genRating(),
    salaries = genRating(),
    benefits = genRating(),
    wouldRecommend = genRating(),
    review = genString(20),
    created = testInstant,
    updated = testInstant
  )

  override val initScriptPath: String = "sql/review.sql"

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("ReviewRepositorySpec")(
      test("create a review") {
        val program = for
          repo   <- ZIO.service[ReviewRepository]
          review <- repo.create(testReview)
        yield review

        program.assert { case Review(_, companyId, userId, management, _, _, _, _, review, _, _) =>
          companyId == testReview.companyId && userId == testReview.userId && management == testReview.management && review == testReview.review
        }
      },
      test("update a review") {
        val program = for
          repo   <- ZIO.service[ReviewRepository]
          review <- repo.create(testReview)
          updatedReview <- repo.update(
            review.id,
            r => r.copy(management = 2, updated = testInstant.plusSeconds(10))
          )
          fetchedById <- repo.getById(review.id)
        yield updatedReview -> fetchedById

        program.assert {
          case (review, Some(fetchedReview)) => review == fetchedReview
          case _                             => false
        }
      },
      test("delete a review") {
        val program = for
          repo        <- ZIO.service[ReviewRepository]
          review      <- repo.create(testReview)
          _           <- repo.delete(review.id)
          fetchedById <- repo.getById(review.id)
        yield fetchedById

        program.assert(_.isEmpty)
      },
      test("get review by ID") {
        val program = for
          repo        <- ZIO.service[ReviewRepository]
          review      <- repo.create(testReview)
          fetchedById <- repo.getById(review.id)
        yield review -> fetchedById

        program.assert {
          case (review, Some(fetchedReview)) =>
            review.id == fetchedReview.id && review.companyId == fetchedReview.companyId
          case _ => false
        }
      },
      test("get reviews by user ID") {
        val testUserId = 2L
        val program = for
          repo <- ZIO.service[ReviewRepository]
          reviewsForCompany <- ZIO.foreach(1 to 10)(_ =>
            repo.create(genReview().copy(userId = testUserId))
          )
          fetchedByUserId <- repo.getByUserId(testUserId)
        yield reviewsForCompany -> fetchedByUserId

        program.map { case (reviews, fetchedReviews) =>
          zio.test.assert(fetchedReviews)(Assertion.hasSameElements(reviews))
        }
      },
      test("get reviews by company ID") {
        val testCompanyId = 2L
        val program = for
          repo <- ZIO.service[ReviewRepository]
          reviewsForCompany <- ZIO.foreach(1 to 10)(_ =>
            repo.create(genReview().copy(companyId = testCompanyId))
          )
          fetchedByCompanyId <- repo.getByCompanyId(testCompanyId)
        yield (reviewsForCompany, fetchedByCompanyId)

        program.map { case (reviews, fetchedReviews) =>
          zio.test.assert(fetchedReviews)(Assertion.hasSameElements(reviews))
        }
      },
      test("get all reviews") {
        val program = for
          repo              <- ZIO.service[ReviewRepository]
          reviewsForCompany <- ZIO.foreach(1 to 10)(_ => repo.create(genReview()))
          fetchedReviews    <- repo.get
        yield reviewsForCompany -> fetchedReviews

        program.map { case (reviews, fetchedReviews) =>
          zio.test.assert(fetchedReviews)(Assertion.hasSameElements(reviews))
        }
      }
    ).provide(ReviewRepository.live, dataSourceLayer, Repository.quillLayer, Scope.default)
