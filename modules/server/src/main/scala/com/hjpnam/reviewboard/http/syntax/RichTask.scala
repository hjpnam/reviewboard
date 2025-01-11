package com.hjpnam.reviewboard.http.syntax

import com.hjpnam.reviewboard.domain.error.HttpError
import zio.{Task, ZIO}

extension [R, A](task: Task[A])
  def mapToHttpError: ZIO[R, HttpError, A] = task.mapError(HttpError.apply)
