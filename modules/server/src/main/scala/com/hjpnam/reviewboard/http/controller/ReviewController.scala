package com.hjpnam.reviewboard.http.controller

import com.hjpnam.reviewboard.http.Authenticator
import com.hjpnam.reviewboard.http.endpoint.ReviewEndpoint
import com.hjpnam.reviewboard.http.syntax.mapToHttpError
import com.hjpnam.reviewboard.service.{JWTService, ReviewService}
import sttp.tapir.ztapir.*
import zio.ZIO

class ReviewController private (reviewService: ReviewService, val jwtService: JWTService)
    extends BaseController,
      ReviewEndpoint,
      Authenticator:

  val create = createEndpoint.authenticate
    .serverLogic[Any](userId => req => reviewService.create(req, userId.id).mapToHttpError)

  val getById =
    getByIdEndpoint.authenticate.serverLogic[Any](_ =>
      id => reviewService.getById(id).mapToHttpError
    )

  val getByCompanyId = getByCompanyIdEndpoint.authenticate.serverLogic[Any](_ =>
    companyId => reviewService.getByCompanyId(companyId).mapToHttpError
  )

  val getByUserId = getByUserIdEndpoint.authenticate.serverLogic[Any](_ =>
    userId => reviewService.getByUserId(userId).mapToHttpError
  )

  val getAll = getAllEndpoint.authenticate.serverLogic[Any] { _ => _ =>
    reviewService.getAll.mapToHttpError
  }

  override val routes: List[ZServerEndpoint[Any, Any]] =
    create :: getById :: getByCompanyId :: getByUserId :: getAll :: Nil

object ReviewController:
  def makeZIO: ZIO[JWTService & ReviewService, Nothing, ReviewController] =
    for
      reviewService <- ZIO.service[ReviewService]
      jwtService    <- ZIO.service[JWTService]
    yield new ReviewController(reviewService, jwtService)
