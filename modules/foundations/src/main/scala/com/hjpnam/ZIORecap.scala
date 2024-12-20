package com.hjpnam

import zio.{Scope, ZIO, ZIOAppArgs, ZIOAppDefault}

object ZIORecap extends ZIOAppDefault:
  override def run = ZIO.succeed(Console.println("hello world"))
