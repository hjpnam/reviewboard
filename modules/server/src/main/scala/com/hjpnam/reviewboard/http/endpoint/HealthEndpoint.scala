package com.hjpnam.reviewboard.http.endpoint

import sttp.tapir.ztapir.*

trait HealthEndpoint:
  val healthCheck = endpoint
    .tag("health")
    .name("health")
    .description("health check")
    .get
    .in("health")
    .out(plainBody[String])
