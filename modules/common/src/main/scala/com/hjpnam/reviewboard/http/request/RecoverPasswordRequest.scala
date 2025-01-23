package com.hjpnam.reviewboard.http.request

import zio.json.JsonCodec

case class RecoverPasswordRequest(
    email: String,
    token: String,
    newPassword: String
) derives JsonCodec
