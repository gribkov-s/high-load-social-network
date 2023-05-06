package com.sgribkov.socialnetwork.services.dialog.builder

import cats.implicits._
import com.sgribkov.socialnetwork.data.entities.UserIdentity
import com.sgribkov.socialnetwork.data.entities.dialog.{DialogId, DialogMessage}
import com.sgribkov.socialnetwork.services.dialog.builder.DialogFlowBuilder.DialogClientFlow
import com.sgribkov.socialnetwork.services.dialog.service._
import fs2.Pipe
import fs2.concurrent.Queue
import io.circe.syntax._
import org.http4s.websocket.WebSocketFrame
import zio._
import zio.interop.catz._


class DialogFlowBuilderLive(dialogService: DialogService.Service) extends DialogFlowBuilder.Service {

  override def build[R](dialogId: DialogId, user: UserIdentity): RIO[R, DialogClientFlow[R]] =
    for {
      in <- Queue.unbounded[RIO[R, *], WebSocketFrame]
      inEndChannel <- Queue.unbounded[Task[*], Option[Unit]]
      out <- Queue.unbounded[Task[*], DialogMessage]
      _ <- dialogService.createUser(dialogId, user, out)

      qinLogic = in.dequeue
        .through(handleMsg[R](dialogId, user))
        .merge(inEndChannel.dequeue)
        .unNoneTerminate

      fb <- (
        for {
          res <- qinLogic.compile.drain
        } yield ()
        ).forkDaemon

      savedMessages = dialogService.getDialogMessages(dialogId)
        .map(m => WebSocketFrame.Text(m.toDTO.asJson.toString))
      outStream = savedMessages <+> out.dequeue.map(m => WebSocketFrame.Text(m.toDTO.asJson.toString))
      onClose = for {
        //_ <- fb.await
        _ <- inEndChannel.enqueue1(none)
        _ <- dialogService.remove(dialogId)
      } yield ()
      _ <- dialogService.delivered(dialogId, user)
    } yield DialogClientFlow(outStream, in.enqueue, onClose)

  private def handleMsg[R](dialogId: DialogId,
                           userId: UserIdentity
                          ): Pipe[RIO[R, *], WebSocketFrame, Option[Unit]] =
    _.collect {
      case WebSocketFrame.Text(msg, _) => msg
    }.evalMap(msg =>
      for {
        delivered <- dialogService.checkReceiver(dialogId)
        _ <- dialogService.handleUserMsg(dialogId, userId, delivered, msg)
      } yield msg
    )
      .dropWhile(_ => true)
      .map(_ => ().some)
}
