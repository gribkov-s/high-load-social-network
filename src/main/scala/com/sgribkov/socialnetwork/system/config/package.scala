package com.sgribkov.socialnetwork.system

import scala.jdk.CollectionConverters._
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import zio.logging.{Logging, log}
import zio.{Task, _}

package object config {

  type Config = Has[DatabaseConfig] with Has[HttpServerConfig] with Has[DbMigrations]

  object Config {

    private val basePath = "high-load-social-network"
    private val source = ConfigSource.default.at(basePath)

    private val buildEnv: Task[String] =
      Task.effect {
        System
          .getenv()
          .asScala
          .map(v => s"${v._1} = ${v._2}")
          .mkString("\n", "\n", "")
      }

    private def logEnv(ex: Throwable): ZIO[Logging, Throwable, Unit] =
      for {
        env <- buildEnv
        _ <- log.error(s"Loading configuration failed with the following environment variables: $env.")
        _ <- log.error(s"Error thrown was $ex.")
      } yield ()

    val live: ZLayer[Logging, Throwable, Config] = ZLayer.fromEffectMany(
      Task
        .effect(source.loadOrThrow[AppConfig])
        .map(c => Has(c.database) ++ Has(c.server) ++ Has(c.migrations))
        .tapBoth(err => logEnv(err), c => log.info(s"Loaded configuration $c successfully."))
    )

    val dbConfig: URIO[Has[DatabaseConfig], DatabaseConfig] = ZIO.service
    val httpServerConfig: URIO[Has[HttpServerConfig], HttpServerConfig] = ZIO.service
    val dbMigrations: URIO[Has[DbMigrations], DbMigrations] = ZIO.service
  }
}
