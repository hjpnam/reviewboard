package com.hjpnam.reviewboard.http.controllers

import sttp.tapir.ztapir.ZServerEndpoint

trait BaseController:
  val routes: List[ZServerEndpoint[Any, Any]]
