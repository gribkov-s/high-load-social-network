package com.sgribkov.socialnetwork.http

import org.http4s.HttpApp
import org.http4s.server.blaze.BlazeServerBuilder
import zio.interop.catz._
import zio.interop.catz.implicits._
import zio.{RIO, ZIO}
import com.sgribkov.socialnetwork.system.config._
import com.sgribkov.socialnetwork.http.api._
import org.http4s.server.Router
import cats.effect.{ExitCode => CatsExitCode}
import com.sgribkov.socialnetwork.AppEnvironment
import com.sgribkov.socialnetwork.http.authentication.UserAuthMiddleware
import org.http4s.implicits._
import cats.implicits.toSemigroupKOps


object Server extends UserAuthMiddleware {

  type ServerTask[A] = RIO[AppEnvironment, A]

  private val userRegRoutes = new UserRegAPI[AppEnvironment].routes
  private val userRoutes = new UserAPI[AppEnvironment].routes
  private val dialogRoutes = new UserDialogAPI[AppEnvironment].routes

  private val appRoutes =
    userRegRoutes <+>
      authMiddleware(userRoutes) <+>
        authMiddleware(dialogRoutes)

  private def httpApp(uri: String): HttpApp[ServerTask] =
    Router[ServerTask](uri -> appRoutes).orNotFound

  val run: ZIO[AppEnvironment, Throwable, Unit] = for {
    config <- Config.httpServerConfig
    server <- ZIO.runtime[AppEnvironment].flatMap{ implicit rts =>
      val ec = rts.platform.executor.asEC
      BlazeServerBuilder[ServerTask](ec)
        .bindHttp(config.port, config.host)
        .withHttpApp(httpApp(config.baseUrl))
        .withWebSockets(true)
        .serve
        .compile[ServerTask, ServerTask, CatsExitCode]
        .drain
    }
  } yield server

}
