package com.hjpnam.reviewboard.http

import com.hjpnam.reviewboard.http.controller.{BaseController, CompanyController, HealthController}
import sttp.tapir.ztapir.ZServerEndpoint
import zio.UIO

object HttpApi:
  def gatherRoutes(controllers: List[BaseController]): List[ZServerEndpoint[Any, Any]] =
    controllers.flatMap(_.routes)

  def makeControllers: UIO[List[BaseController]] =
    for
      healthController  <- HealthController.makeZIO
      companyController <- CompanyController.makeZIO
    yield healthController :: companyController :: Nil

  val endpointsZIO: UIO[List[ZServerEndpoint[Any, Any]]] = makeControllers.map(gatherRoutes)
