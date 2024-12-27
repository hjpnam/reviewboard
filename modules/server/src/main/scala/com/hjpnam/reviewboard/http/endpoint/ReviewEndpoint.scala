package com.hjpnam.reviewboard.http.endpoint

import com.hjpnam.reviewboard.domain.data.Review
import com.hjpnam.reviewboard.http.request.CreateReviewRequest
import sttp.tapir.*
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.generic.auto.*

trait ReviewEndpoint:
  val createEndpoint = endpoint
    .tag("review")
    .name("create")
    .description("create a review for a company")
    .in("review")
    .post
    .in(jsonBody[CreateReviewRequest])
    .out(jsonBody[Review])

  val getAllEndpoint = endpoint
    .tag("review")
    .name("getAll")
    .description("get all company reviews")
    .in("review")
    .get
    .out(jsonBody[List[Review]])

  val getByIdEndpoint = endpoint
    .tag("review")
    .name("getById")
    .description("get company review by ID")
    .in("review" / path[String]("id"))
    .get
    .out(jsonBody[Option[Review]])
