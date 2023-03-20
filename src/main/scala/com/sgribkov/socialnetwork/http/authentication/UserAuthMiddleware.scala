package com.sgribkov.socialnetwork.http.authentication

import cats.data.{Kleisli, OptionT}
import com.sgribkov.socialnetwork.AppEnvironment
import com.sgribkov.socialnetwork.data.entities.UserIdentity
import org.http4s.{AuthedRoutes, Request, server}
import org.http4s.dsl.Http4sDsl
import org.http4s.server.AuthMiddleware
import zio.RIO
import zio.interop.catz.monadErrorInstance


trait UserAuthMiddleware {

  type UserAuthTask[A] = RIO[AppEnvironment, A]

  val dsl: Http4sDsl[UserAuthTask] = Http4sDsl[UserAuthTask]
  import dsl._

  val authHeaders = new UserAuthHeaders[AppEnvironment] {}

  def authUser: Kleisli[UserAuthTask, Request[UserAuthTask], Either[String, UserIdentity]] = {
    Kleisli(request =>
      authHeaders.getToken(request).map { e => {
        e.left.map(_.toString)
      }}
    )
  }

  val onFailure: AuthedRoutes[String, UserAuthTask] = Kleisli(
    authedReq => OptionT.liftF {
      Forbidden(authedReq.context) //authedReq.authInfo
    }
  )

  val authMiddleware: server.AuthMiddleware[UserAuthTask, UserIdentity] = AuthMiddleware(authUser, onFailure)
}
