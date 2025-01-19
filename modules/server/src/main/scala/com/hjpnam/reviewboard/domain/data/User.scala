package com.hjpnam.reviewboard.domain.data

import io.github.arainko.ducktape.into
import zio.json.{DeriveJsonCodec, JsonCodec}

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
