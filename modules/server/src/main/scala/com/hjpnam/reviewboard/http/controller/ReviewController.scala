package com.hjpnam.reviewboard.http.controller

import com.hjpnam.reviewboard.http.endpoint.ReviewEndpoint
import com.hjpnam.reviewboard.service.ReviewService
import sttp.tapir.server.ServerEndpoint
import zio.{Task, URIO, ZIO}

class ReviewController private (reviewService: ReviewService)
    extends BaseController
    with ReviewEndpoint:

  val create = createEndpoint.serverLogicSuccess[Task](reviewService.create)
  val getById = getByIdEndpoint.serverLogicSuccess[Task] { id =>
    ZIO
      .attempt(id.toLong)
      .flatMap(reviewService.getById)
  }
  val getAll = getAllEndpoint.serverLogicSuccess[Task] { _ => reviewService.getAll }

  override val routes: List[ServerEndpoint[Any, Task]] =
    create :: getById :: getAll :: Nil

object ReviewController:
  def makeZIO: URIO[ReviewService, ReviewController] =
    for reviewService <- ZIO.service[ReviewService]
    yield new ReviewController(reviewService)
