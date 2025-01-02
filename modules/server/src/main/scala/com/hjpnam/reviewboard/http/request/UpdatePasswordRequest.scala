package com.hjpnam.reviewboard.http.request

import zio.json.JsonCodec

final case class UpdatePasswordRequest(email: String, oldPassword: String, newPassword: String)
    derives JsonCodec
