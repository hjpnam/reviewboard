package com.hjpnam.reviewboard.http

import com.hjpnam.reviewboard.http.controller.{BaseController, CompanyController, HealthController}

object HttpApi:
  def gatherRoutes(controllers: List[BaseController]) =
    controllers.flatMap(_.routes)

  def makeControllers =
    for
      healthController  <- HealthController.makeZIO
      companyController <- CompanyController.makeZIO
    yield healthController :: companyController :: Nil

  val endpointsZIO = makeControllers.map(gatherRoutes)
