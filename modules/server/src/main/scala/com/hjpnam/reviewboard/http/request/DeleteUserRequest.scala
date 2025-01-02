package com.hjpnam.reviewboard.http.request

import zio.json.JsonCodec

final case class DeleteUserRequest(email: String, password: String) derives JsonCodec
