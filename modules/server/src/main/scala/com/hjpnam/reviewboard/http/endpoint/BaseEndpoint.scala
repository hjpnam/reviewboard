package com.hjpnam.reviewboard.http.endpoint

import com.hjpnam.reviewboard.domain.data.UserID
import com.hjpnam.reviewboard.domain.error.*
import com.hjpnam.reviewboard.http.syntax.*
import com.hjpnam.reviewboard.service.JWTService
import sttp.model.StatusCode
import sttp.tapir.ztapir.*

trait BaseEndpoint:
  val baseEndpoint = endpoint
    .errorOut(statusCode and plainBody[String])
    .mapErrorOut(HttpError.decode)(HttpError.encode)

trait SecureBaseEndpoint(jwtService: JWTService):
  this: BaseEndpoint =>

  val secureBaseEndpoint = baseEndpoint
    .securityIn(auth.bearer[String]())
    .zServerSecurityLogic[Any, UserID](token => jwtService.verifyToken(token).mapToHttpError)
