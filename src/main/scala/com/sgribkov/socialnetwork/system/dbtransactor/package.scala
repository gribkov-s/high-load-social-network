package com.sgribkov.socialnetwork.system

import scala.concurrent.ExecutionContext
import cats.effect.Blocker
import doobie.hikari.HikariTransactor
import doobie.util.transactor.Transactor
import zio.blocking.Blocking
import com.sgribkov.socialnetwork.system.config.{DatabaseConfig, _}
import zio.interop.catz._
import zio.logging.{Logging, log}
import zio.{Has, Managed, Task, URIO, ZIO, ZLayer, ZManaged, blocking}

package object dbtransactor {

  type DBTransactor = Has[Transactor[Task]]

  object DBTransactor {
    private def makeTransactor(conf: DatabaseConfig,
                               connectEC: ExecutionContext,
                               transactEC: ExecutionContext
                              ): Managed[Throwable, Transactor[Task]] =
      HikariTransactor
        .newHikariTransactor[Task](
          conf.driver,
          conf.url,
          conf.user,
          conf.password,
          connectEC,
          Blocker.liftExecutionContext(transactEC)
        )
        .toManagedZIO

    val managed: ZManaged[Has[DatabaseConfig] with Blocking with Logging, Throwable, Transactor[Task]] =
      for {
        config <- Config.dbConfig.toManaged_
        connectEC <- ZIO.descriptor.map(_.executor.asEC).toManaged_
        blockingEC <- blocking.blocking(ZIO.descriptor.map(_.executor.asEC)).toManaged_
        _ <- log.info("Initializing DB Transactor and creating connection pool.").toManaged_
        transactor <- makeTransactor(config, connectEC, blockingEC)
      } yield transactor

    val managedWithMigration: ZManaged[Has[DatabaseConfig] with Has[DbMigrations] with Logging with Blocking, Throwable, Transactor[Task]] =
      Migrations.migrate.toManaged_ *> managed

    val live: ZLayer[Has[DatabaseConfig] with Has[DbMigrations] with Logging with Blocking, Throwable, DBTransactor] =
      ZLayer.fromManaged(managedWithMigration)

    val transactor: URIO[DBTransactor, Transactor[Task]] = ZIO.service
  }

}
