package com.hjpnam.reviewboard.http.endpoint

import com.hjpnam.reviewboard.domain.data.Company
import com.hjpnam.reviewboard.http.request.CreateCompanyRequest
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.ztapir.*

trait CompanyEndpoint extends BaseEndpoint:
  val createEndpoint =
    baseEndpoint
      .tag("companies")
      .name("create")
      .description("create listing for a company")
      .in("companies")
      .post
      .in(jsonBody[CreateCompanyRequest])
      .out(jsonBody[Company])

  val getAllEndpoint = baseEndpoint
    .tag("companies")
    .name("getAll")
    .description("get all company listings")
    .in("companies")
    .get
    .out(jsonBody[List[Company]])

  val getByIdEndpoint = baseEndpoint
    .tag("companies")
    .name("getById")
    .description("get company listing by ID")
    .in("companies" / path[String]("id"))
    .get
    .out(jsonBody[Option[Company]])
