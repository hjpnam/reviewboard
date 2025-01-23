package com.hjpnam.reviewboard.domain.data

case class PasswordRecoveryToken(email: String, token: String, expiration: Long)
