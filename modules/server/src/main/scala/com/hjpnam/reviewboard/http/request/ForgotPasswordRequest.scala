package com.hjpnam.reviewboard.http.request

import zio.json.JsonCodec

case class ForgotPasswordRequest(email: String) derives JsonCodec
