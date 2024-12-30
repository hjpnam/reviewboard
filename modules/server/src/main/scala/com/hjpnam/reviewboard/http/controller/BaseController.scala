package com.hjpnam.reviewboard.http.controller

import sttp.tapir.ztapir.ZServerEndpoint

trait BaseController:
  val routes: List[ZServerEndpoint[Any, Any]]
