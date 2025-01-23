package com.hjpnam.reviewboard.http.endpoint

import com.hjpnam.reviewboard.domain.data.Review
import com.hjpnam.reviewboard.domain.error.HttpError
import com.hjpnam.reviewboard.http.request.CreateReviewRequest
import sttp.tapir.*
import sttp.tapir.generic.auto.given
import sttp.tapir.json.zio.jsonBody

trait ReviewEndpoint extends BaseEndpoint:
  val createEndpoint: Endpoint[String, CreateReviewRequest, HttpError, Review, Any] =
    secureBaseEndpoint
      .tag("review")
      .name("create")
      .description("create a review for a company")
      .in("review")
      .post
      .in(jsonBody[CreateReviewRequest])
      .out(jsonBody[Review])

  val getAllEndpoint = secureBaseEndpoint
    .tag("review")
    .name("getAll")
    .description("get all company reviews")
    .in("review")
    .get
    .out(jsonBody[List[Review]])

  val getByIdEndpoint = secureBaseEndpoint
    .tag("review")
    .name("getById")
    .description("get company review by ID")
    .in("review" / path[Long]("id"))
    .get
    .out(jsonBody[Option[Review]])

  val getByCompanyIdEndpoint = secureBaseEndpoint
    .tag("review")
    .name("getByCompanyId")
    .description("get reviews for a company")
    .in("review" / "company" / path[Long]("companyId"))
    .get
    .out(jsonBody[List[Review]])

  val getByUserIdEndpoint = secureBaseEndpoint
    .tag("review")
    .name("getByUserId")
    .description("get reviews written by a user")
    .in("review" / "user" / path[Long]("userId"))
    .get
    .out(jsonBody[List[Review]])
