package com.hjpnam.reviewboard.http.endpoint

import com.hjpnam.reviewboard.domain.data.{Company, CompanyFilter}
import com.hjpnam.reviewboard.http.request.CreateCompanyRequest
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.jsonBody

trait CompanyEndpoint extends BaseEndpoint:
  val createEndpoint =
    baseEndpoint
      .tag("company")
      .name("create")
      .description("create listing for a company")
      .in("company")
      .post
      .in(jsonBody[CreateCompanyRequest])
      .out(jsonBody[Company])

  val getAllEndpoint = baseEndpoint
    .tag("company")
    .name("getAll")
    .description("get all company listings")
    .in("company")
    .get
    .out(jsonBody[List[Company]])

  val getByIdEndpoint = baseEndpoint
    .tag("company")
    .name("getById")
    .description("get company listing by ID")
    .in("company" / path[String]("id"))
    .get
    .out(jsonBody[Option[Company]])

  val allFiltersEndpoint = baseEndpoint
    .tag("company")
    .name("allFilters")
    .description("Get all possible search filters")
    .in("company" / "filters")
    .get
    .out(jsonBody[CompanyFilter])
