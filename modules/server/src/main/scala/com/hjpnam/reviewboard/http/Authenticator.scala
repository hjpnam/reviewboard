package com.hjpnam.reviewboard.http

import com.hjpnam.reviewboard.domain.data.UserID
import com.hjpnam.reviewboard.domain.error.HttpError
import com.hjpnam.reviewboard.http.syntax.*
import com.hjpnam.reviewboard.service.JWTService
import sttp.tapir.Endpoint
import sttp.tapir.ztapir.*

trait Authenticator:
  def jwtService: JWTService

  extension [INPUT, OUTPUT, C](e: Endpoint[String, INPUT, HttpError, OUTPUT, C])
    def authenticate: ZPartialServerEndpoint[Any, String, UserID, INPUT, HttpError, OUTPUT, C] =
      e.zServerSecurityLogic[Any, UserID](token => jwtService.verifyToken(token).mapToHttpError)
