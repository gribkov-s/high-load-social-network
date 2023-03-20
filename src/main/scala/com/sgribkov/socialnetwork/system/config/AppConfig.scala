package com.sgribkov.socialnetwork.system.config

final case class AppConfig(server: HttpServerConfig,
                           database: DatabaseConfig,
                           migrations: DbMigrations
                          )

