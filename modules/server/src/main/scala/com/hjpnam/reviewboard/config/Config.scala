package com.hjpnam.reviewboard.config

import zio.config.magnolia.{deriveConfig, DeriveConfig}
import zio.config.typesafe.FromConfigSourceTypesafe
import zio.{ConfigProvider, RLayer, Tag, ZLayer}

object Config:
  def makeConfigLayer[A](path: String)(using desc: DeriveConfig[A], tag: Tag[A]): RLayer[Any, A] =
    ZLayer {
      for
        configProvider <- ConfigProvider.fromHoconFilePathZIO(path)
        config         <- configProvider.load(deriveConfig[A])
      yield config
    }
