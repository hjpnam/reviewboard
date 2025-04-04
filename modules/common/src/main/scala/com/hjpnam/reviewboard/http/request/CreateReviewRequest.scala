package com.hjpnam.reviewboard.http.request

import com.hjpnam.reviewboard.domain.data.Review
import io.github.arainko.ducktape.{into, Field}
import zio.json.JsonCodec

import java.time.Instant

case class CreateReviewRequest(
    companyId: Long,
    management: Int,
    culture: Int,
    salaries: Int,
    benefits: Int,
    wouldRecommend: Int,
    review: String
) derives JsonCodec:
  def toReview(id: Long, userId: Long, timestamp: Instant): Review =
    this
      .into[Review]
      .transform(
        Field.const(_.id, id),
        Field.const(_.userId, userId),
        Field.const(_.created, timestamp),
        Field.const(_.updated, timestamp)
      )

object CreateReviewRequest:
  def fromReview(review: Review): CreateReviewRequest = review.into[CreateReviewRequest].transform()
