package com.sgribkov.socialnetwork.system.dbtransactor

import com.sgribkov.socialnetwork.system.config._
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.Location
import org.flywaydb.core.api.configuration.ClassicConfiguration
import zio.CanFail.canFailAmbiguous1
import zio.logging.{Logging, log}
import zio.{Has, RIO, ZIO}


object Migrations {

  private def migrateAll(migrations: DbMigrations) = {
    migrations.params.map { p =>
      ZIO.effect {
        val config = new ClassicConfiguration()
        val cpLocation = new Location("classpath:" + p.classpath)
        val fsLocation = new Location("filesystem:" + p.filesystem)
        config.setDataSource(p.url, p.user, p.password)
        config.setLocations(cpLocation, fsLocation)
        val newFlyway = new Flyway(config)
        newFlyway.migrate()
      }.unit
    }
  }

  val migrate: ZIO[Has[DbMigrations] with Logging, Throwable, Unit] =
    Config.dbMigrations
      .flatMap { cfg =>
        ZIO.collectAll_(migrateAll(cfg))
      }
      .tapError(err => log.error(s"Error migrating database: $err."))


}
