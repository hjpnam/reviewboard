package com.hjpnam.reviewboard.http.request

import com.hjpnam.reviewboard.domain.data.Review
import zio.json.{DeriveJsonCodec, JsonCodec}
import java.time.Instant

case class CreateReviewRequest(
    companyId: Long,
    userId: Long,
    management: Int,
    culture: Int,
    salaries: Int,
    benefits: Int,
    wouldRecommend: Int,
    review: String
):
  def toReview(id: Long, timestamp: Instant): Review =
    Review(
      id,
      companyId,
      userId,
      management,
      culture,
      salaries,
      benefits,
      wouldRecommend,
      review,
      timestamp,
      timestamp
    )

object CreateReviewRequest:
  given codec: JsonCodec[CreateReviewRequest] = DeriveJsonCodec.gen[CreateReviewRequest]
