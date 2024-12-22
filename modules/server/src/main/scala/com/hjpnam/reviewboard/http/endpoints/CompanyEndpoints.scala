package com.hjpnam.reviewboard.http.endpoints

import com.hjpnam.reviewboard.domain.data.Company
import com.hjpnam.reviewboard.http.requests.CreateCompanyRequest
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.ztapir.*

trait CompanyEndpoints:
  val createEndpoint = endpoint
    .tag("companies")
    .name("create")
    .description("create listing for a company")
    .in("companies")
    .post
    .in(jsonBody[CreateCompanyRequest])
    .out(jsonBody[Company])

  val getAllEndpoint = endpoint
    .tag("companies")
    .name("getAll")
    .description("get all company listings")
    .in("companies")
    .get
    .out(jsonBody[List[Company]])

  val getByIdEndpoint = endpoint
    .tag("companies")
    .name("getById")
    .description("get company listing by ID")
    .in("companies" / path[String]("id"))
    .get
    .out(jsonBody[Option[Company]])
