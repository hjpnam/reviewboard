package com.hjpnam.reviewboard.domain.data

import zio.json.{DeriveJsonCodec, JsonCodec}
import io.github.arainko.ducktape.into

final case class User(
    id: Long,
    email: String,
    hashedPassword: String
):
  def toUserID: UserID = this.into[UserID].transform()

final case class UserID(
    id: Long,
    email: String
)
