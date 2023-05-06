package com.sgribkov.socialnetwork.http.api

import com.sgribkov.socialnetwork.data.entities.{UserIdentity, UserLogin}
import com.sgribkov.socialnetwork.data.entities.dialog.DialogId
import com.sgribkov.socialnetwork.repository.userdialog.UserDialogRepo
import com.sgribkov.socialnetwork.services.dialog.builder.DialogFlowBuilder
import io.circe.{Decoder, Encoder}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.{AuthedRoutes, EntityDecoder, EntityEncoder}
import zio.{RIO, ZIO}
import zio.interop.catz.taskConcurrentInstance
import org.http4s.server.websocket.WebSocketBuilder
import org.http4s.implicits.http4sLiteralsSyntax


class UserDialogAPI[R <: UserDialogRepo with DialogFlowBuilder] {

  type UserDialogTask[A] =  RIO[R, A]

  val dsl = Http4sDsl[UserDialogTask]
  import dsl._

  implicit def jsonDecoder[A](implicit decoder: Decoder[A]): EntityDecoder[UserDialogTask, A] =
    jsonOf[UserDialogTask, A]

  implicit def jsonEncoder[A](implicit decoder: Encoder[A]): EntityEncoder[UserDialogTask, A] =
    jsonEncoderOf[UserDialogTask, A]


  val routes: AuthedRoutes[UserIdentity, UserDialogTask] = AuthedRoutes.of[UserIdentity, UserDialogTask] {

    case GET -> Root / "dialog" / destination as user =>
      for {
        dialogId <- ZIO.succeed(DialogId.from(user.userLogin, UserLogin(destination)))
        flowData <- DialogFlowBuilder.build[R](dialogId, user)
        ws <- WebSocketBuilder[UserDialogTask].build(
          flowData.out,
          flowData.in,
          onClose = flowData.onClose
        )
      } yield ws
  }
}
