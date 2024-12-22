package com.hjpnam.reviewboard.http.controller

import com.hjpnam.reviewboard.domain.data.Company
import com.hjpnam.reviewboard.http.endpoint.CompanyEndpoints
import com.hjpnam.reviewboard.service.CompanyService
import sttp.tapir.*
import sttp.tapir.server.ServerEndpoint
import zio.*

import scala.collection.mutable

class CompanyController private (companyService: CompanyService)
    extends BaseController
    with CompanyEndpoints:

  val create = createEndpoint.serverLogicSuccess[Task](companyService.create)

  val getAll = getAllEndpoint.serverLogicSuccess[Task](_ => companyService.getAll)

  val getById = getByIdEndpoint.serverLogicSuccess[Task] { id =>
    ZIO
      .attempt(id.toLong)
      .flatMap(companyService.getById)
      .catchSome { case _: NumberFormatException =>
        companyService.getBySlug(id)
      }
  }

  override val routes: List[ServerEndpoint[Any, Task]] = create :: getAll :: getById :: Nil

object CompanyController:
  val makeZIO: URIO[CompanyService, CompanyController] =
    for companyService <- ZIO.service[CompanyService]
    yield new CompanyController(companyService)
