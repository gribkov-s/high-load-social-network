package com.sgribkov.socialnetwork.services.dialog

import com.sgribkov.socialnetwork.data.entities.dialog.DialogId
import com.sgribkov.socialnetwork.data.entities.{UserIdentity, UserLogin}
import com.sgribkov.socialnetwork.services.dialog.builder.DialogFlowBuilderLive
import com.sgribkov.socialnetwork.services.dialog.service.DialogService
import fs2.{Pipe, Stream}
import org.http4s.websocket.WebSocketFrame
import zio._


package object builder {

  type DialogFlowBuilder = Has[DialogFlowBuilder.Service]

  object DialogFlowBuilder {

    case class DialogClientFlow[R](out: Stream[Task[*], WebSocketFrame],
                                   in: Pipe[RIO[R, *], WebSocketFrame, Unit],
                                   onClose: Task[Unit]
                                  )

    trait Service {
      def build[R](dialogId: DialogId, user: UserIdentity): RIO[R, DialogClientFlow[R]]
   }

    val live: ZLayer[DialogService, Nothing, DialogFlowBuilder] =
      ZLayer.fromService[DialogService.Service, DialogFlowBuilder.Service] { dialogService =>
        new DialogFlowBuilderLive(dialogService)
      }

    def build[R](dialogId: DialogId, user: UserIdentity): ZIO[R with DialogFlowBuilder, Throwable, DialogFlowBuilder.DialogClientFlow[R]] =
      ZIO.accessM[R with DialogFlowBuilder](_.get.build[R](dialogId, user))
  }
}
