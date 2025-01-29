package com.hjpnam.reviewboard.http.controller

import com.hjpnam.reviewboard.domain.data.Company
import com.hjpnam.reviewboard.http.endpoint.CompanyEndpoint
import com.hjpnam.reviewboard.http.syntax.*
import com.hjpnam.reviewboard.service.CompanyService
import sttp.tapir.ztapir.{ZServerEndpoint, given}
import zio.{URIO, ZIO}

class CompanyController private (companyService: CompanyService)
    extends BaseController,
      CompanyEndpoint:

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

  val allFilters = allFiltersEndpoint.zServerLogic[Any] { _ =>
    companyService.allFilters.mapToHttpError
  }

  override val routes: List[ZServerEndpoint[Any, Any]] =
    create :: getAll :: allFilters :: getById :: Nil

object CompanyController:
  val makeZIO: URIO[CompanyService, CompanyController] =
    for companyService <- ZIO.service[CompanyService]
    yield new CompanyController(companyService)
