package com.hjpnam.reviewboard.http.response

import zio.json.JsonCodec

final case class UserResponse(email: String) derives JsonCodec
