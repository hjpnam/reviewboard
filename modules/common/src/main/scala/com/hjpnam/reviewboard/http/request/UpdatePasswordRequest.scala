package com.hjpnam.reviewboard.http.request

import zio.json.JsonCodec

final case class UpdatePasswordRequest(oldPassword: String, newPassword: String) derives JsonCodec
