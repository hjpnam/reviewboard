package com.hjpnam.reviewboard.http.controller

import com.hjpnam.reviewboard.http.endpoint.{ReviewEndpoint, SecureBaseEndpoint}
import com.hjpnam.reviewboard.service.{JWTService, ReviewService}
import com.hjpnam.reviewboard.http.syntax.*
import sttp.tapir.ztapir.{ZServerEndpoint, given}
import zio.{URIO, ZIO}

class ReviewController private (reviewService: ReviewService, jwtService: JWTService)
    extends BaseController
    with ReviewEndpoint
    with SecureBaseEndpoint(jwtService):

  val create = createEndpoint.serverLogic[Any](userId =>
    req => reviewService.create(req, userId.id).mapToHttpError
  )

  val getById =
    getByIdEndpoint.serverLogic[Any](_ => id => reviewService.getById(id).mapToHttpError)

  val getByCompanyId = getByCompanyIdEndpoint.serverLogic[Any](_ =>
    companyId => reviewService.getByCompanyId(companyId).mapToHttpError
  )

  val getByUserId = getByUserIdEndpoint.serverLogic[Any](_ =>
    userId => reviewService.getByUserId(userId).mapToHttpError
  )

  val getAll = getAllEndpoint.serverLogic[Any] { _ => _ => reviewService.getAll.mapToHttpError }

  override val routes: List[ZServerEndpoint[Any, Any]] =
    create :: getById :: getByCompanyId :: getByUserId :: getAll :: Nil

object ReviewController:
  def makeZIO: ZIO[JWTService & ReviewService, Nothing, ReviewController] =
    for
      reviewService <- ZIO.service[ReviewService]
      jwtService    <- ZIO.service[JWTService]
    yield new ReviewController(reviewService, jwtService)
