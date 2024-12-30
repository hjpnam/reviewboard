package com.hjpnam.reviewboard.http.controller

import com.hjpnam.reviewboard.domain.data.Company
import com.hjpnam.reviewboard.domain.error.HttpError
import com.hjpnam.reviewboard.http.endpoint.CompanyEndpoint
import com.hjpnam.reviewboard.service.CompanyService
import sttp.tapir.ztapir.*
import sttp.tapir.server.ServerEndpoint
import zio.*
import com.hjpnam.reviewboard.http.controller.syntax.*

class CompanyController private (companyService: CompanyService)
    extends BaseController
    with CompanyEndpoint:

  val create =
    createEndpoint.zServerLogic[Any](req => companyService.create(req).mapToHttpError)

  val getAll = getAllEndpoint.zServerLogic[Any](_ => companyService.getAll.mapToHttpError)

  val getById = getByIdEndpoint.zServerLogic[Any] { id =>
    ZIO
      .attempt(id.toLong)
      .flatMap(companyService.getById)
      .catchSome { case _: NumberFormatException =>
        companyService.getBySlug(id)
      }
      .mapToHttpError
  }

  override val routes: List[ZServerEndpoint[Any, Any]] = create :: getAll :: getById :: Nil

object CompanyController:
  val makeZIO: URIO[CompanyService, CompanyController] =
    for companyService <- ZIO.service[CompanyService]
    yield new CompanyController(companyService)
