package com.hjpnam.reviewboard.http

import com.hjpnam.reviewboard.http.controller.{
  BaseController,
  CompanyController,
  HealthController,
  ReviewController,
  UserController
}

object HttpApi:
  def gatherRoutes(controllers: List[BaseController]) =
    controllers.flatMap(_.routes)

  def makeControllers =
    for
      healthController  <- HealthController.makeZIO
      companyController <- CompanyController.makeZIO
      reviewController  <- ReviewController.makeZIO
      userController    <- UserController.makeZIO
    yield healthController :: companyController :: reviewController :: userController :: Nil

  val endpointsZIO = makeControllers.map(gatherRoutes)
