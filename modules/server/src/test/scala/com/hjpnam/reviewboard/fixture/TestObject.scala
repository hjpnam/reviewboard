package com.hjpnam.reviewboard.fixture

import com.hjpnam.reviewboard.domain.data.{Review, User}
import com.hjpnam.reviewboard.http.request.CreateReviewRequest

import java.time.Instant

trait TestObject:
  val testEmail = "test@email.com"

  val testUser = User(
    1L,
    testEmail,
    "1000:da70ffa630dc4793f0e4a64a8ca1aa1ae5892a29da5ce97d:2d1538385faf4154231d6fae95e09306efd841b32e9eb0bc"
  )

  val now = Instant.now

  val testCreateReviewRequest = CreateReviewRequest(
    companyId = 1L,
    management = 1,
    culture = 1,
    salaries = 1,
    benefits = 1,
    wouldRecommend = 1,
    review = "lorem ipsum"
  )

  val testReview = Review(
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
