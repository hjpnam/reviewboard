package com.hjpnam.reviewboard.domain.data

import zio.json.{DeriveJsonCodec, JsonCodec}

final case class User(
    id: Long,
    email: String,
    hashedPassword: String
)

object User:
  given codec: JsonCodec[User] = DeriveJsonCodec.gen[User]
