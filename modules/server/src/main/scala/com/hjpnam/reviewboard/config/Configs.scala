package com.hjpnam.reviewboard.config

import com.typesafe.config.ConfigFactory
import zio.config.magnolia.{deriveConfig, DeriveConfig}
import zio.config.typesafe.FromConfigSourceTypesafe
import zio.{ConfigProvider, Tag, TaskLayer, ZLayer}

object Configs:
  def makeLayer[A](
      path: String
  )(using desc: DeriveConfig[A], tag: Tag[A]): TaskLayer[A] =
    ZLayer {
      for
        configProvider <- ConfigProvider.fromTypesafeConfigZIO(ConfigFactory.load().getConfig(path))
        config         <- configProvider.load(deriveConfig[A])
      yield config
    }
