package com.hjpnam.reviewboard.domain.data

import zio.json.{DeriveJsonCodec, JsonCodec}

import java.time.Instant

final case class Review(
    id: Long,
    companyId: Long,
    userId: Long,
    management: Int,
    culture: Int,
    salaries: Int,
    benefits: Int,
    wouldRecommend: Int,
    review: String,
    created: Instant,
    updated: Instant
) derives JsonCodec

object Review:
  def empty(companyId: Long): Review = Review(
    -1L,
    companyId,
    -1L,
    5,
    5,
    5,
    5,
    5,
    "",
    Instant.now(),
    Instant.now()
  )
