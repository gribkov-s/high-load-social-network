package com.sgribkov.socialnetwork.system.config

case class DbMigrationConfig(url: String,
                             driver: String,
                             user: String,
                             password: String,
                             classpath: String,
                             filesystem: String
                            )
