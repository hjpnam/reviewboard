package com.hjpnam.reviewboard.config

final case class EmailConfig(
    host: String,
    port: Int,
    user: String,
    password: String
)
