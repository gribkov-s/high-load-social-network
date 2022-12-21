package com.sgribkov.socialnetwork.system.dbtransactor

import org.flywaydb.core.Flyway
import org.flywaydb.core.api.Location
import org.flywaydb.core.api.configuration.ClassicConfiguration
import com.sgribkov.socialnetwork.system.config._
import zio.logging.{Logging, log}
import zio.{Has, RIO, ZIO}


object Migration {

  private val cpLocation = new Location("classpath:src/main/resources/db/migration")
  private val fsLocation = new Location("filesystem:src/main/resources/db/migration")

  val migrate: RIO[Has[DatabaseConfig] with Logging, Unit] =
    Config.dbConfig
      .flatMap { cfg =>
        ZIO.effect {
          val config = new ClassicConfiguration()
          config.setDataSource(cfg.url, cfg.user, cfg.password)
          config.setLocations(cpLocation, fsLocation)
          val newFlyway = new Flyway(config)
          newFlyway.migrate()
        }.unit
      }
      .tapError(err => log.error(s"Error migrating database: $err."))

}
