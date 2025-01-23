package com.hjpnam.reviewboard.http.endpoint

import com.hjpnam.reviewboard.domain.error.HttpError
import sttp.model.StatusCode
import sttp.tapir.*

trait BaseEndpoint:
  val baseEndpoint = endpoint
    .errorOut(statusCode and plainBody[String])
    .mapErrorOut(HttpError.decode)(HttpError.encode)

  val secureBaseEndpoint = baseEndpoint
    .securityIn(auth.bearer[String]())
