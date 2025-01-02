package com.hjpnam.reviewboard.http.controller

import com.hjpnam.reviewboard.http.endpoint.ReviewEndpoint
import com.hjpnam.reviewboard.service.ReviewService
import com.hjpnam.reviewboard.http.syntax.*
import sttp.tapir.ztapir.{ZServerEndpoint, given}
import zio.{URIO, ZIO}

class ReviewController private (reviewService: ReviewService)
    extends BaseController
    with ReviewEndpoint:

  val create = createEndpoint.zServerLogic[Any](req => reviewService.create(req).mapToHttpError)
  val getById = getByIdEndpoint.zServerLogic[Any] { id =>
    ZIO
      .attempt(id.toLong)
      .flatMap(reviewService.getById)
      .mapToHttpError
  }
  val getAll = getAllEndpoint.zServerLogic[Any] { _ => reviewService.getAll.mapToHttpError }

  override val routes: List[ZServerEndpoint[Any, Any]] =
    create :: getById :: getAll :: Nil

object ReviewController:
  def makeZIO: URIO[ReviewService, ReviewController] =
    for reviewService <- ZIO.service[ReviewService]
    yield new ReviewController(reviewService)
