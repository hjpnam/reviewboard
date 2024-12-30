package com.hjpnam.reviewboard.domain.data

import zio.json.{DeriveJsonCodec, JsonCodec}

final case class User(
    id: Long,
    email: String,
    hashedPassword: String
):
  def toUserID: UserID = UserID(id, email)

final case class UserID(
    id: Long,
    email: String
)
