package com.hjpnam.reviewboard.domain.error

import com.hjpnam.reviewboard.domain.error.*
import sttp.model.StatusCode

final case class HttpError(
    statusCode: StatusCode,
    message: String
) extends Throwable(s"$statusCode: $message")

object HttpError:
  def decode(tuple: (StatusCode, String)): HttpError     = HttpError(tuple._1, tuple._2)
  def encode(httpError: HttpError): (StatusCode, String) = httpError.statusCode -> httpError.message

  def apply(throwable: Throwable): HttpError =
    val statusCode = throwable match
      case _: ObjectNotFound => StatusCode.NotFound
      case _: Unauthorized   => StatusCode.Unauthorized
      case _                 => StatusCode.InternalServerError

    HttpError(statusCode, throwable.getMessage)
