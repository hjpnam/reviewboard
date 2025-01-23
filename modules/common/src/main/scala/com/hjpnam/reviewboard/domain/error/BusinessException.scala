package com.hjpnam.reviewboard.domain.error

sealed abstract class BusinessException(message: String) extends Throwable(message)

final case class ObjectNotFound(message: String) extends BusinessException(message)
final case class Unauthorized(message: String)   extends BusinessException(message)
