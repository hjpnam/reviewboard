package com.hjpnam.reviewboard.syntax

import zio.ZIO
import zio.test.*

extension [R, E, A](zio: ZIO[R, E, A])
  def assert(assertion: Assertion[A]): ZIO[R, E, TestResult] =
    assertZIO(zio)(assertion)

  def assert(predicate: (=> A) => Boolean, name: String = "test assertion"): ZIO[R, E, TestResult] =
    assert(Assertion.assertion(name)(predicate))
