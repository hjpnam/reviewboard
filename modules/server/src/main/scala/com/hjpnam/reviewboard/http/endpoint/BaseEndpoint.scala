package com.hjpnam.reviewboard.http.endpoint

import com.hjpnam.reviewboard.domain.error.*
import sttp.model.StatusCode
import sttp.tapir.Endpoint
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.ztapir.*

trait BaseEndpoint:
  val baseEndpoint: Endpoint[Unit, Unit, HttpError, Unit, Any] = endpoint
    .errorOut(statusCode and plainBody[String])
    .mapErrorOut(HttpError.decode)(HttpError.encode)
