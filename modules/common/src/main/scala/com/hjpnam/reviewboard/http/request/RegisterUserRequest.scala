package com.hjpnam.reviewboard.http.request

import zio.json.JsonCodec

final case class RegisterUserRequest(email: String, password: String) derives JsonCodec
