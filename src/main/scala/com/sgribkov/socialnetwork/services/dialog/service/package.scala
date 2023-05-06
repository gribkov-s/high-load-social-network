package com.sgribkov.socialnetwork.services.dialog

import com.sgribkov.socialnetwork.data.entities.dialog.{DialogId, DialogMessage, DialogUser}
import com.sgribkov.socialnetwork.data.entities.UserIdentity
import com.sgribkov.socialnetwork.repository.userdialog.UserDialogRepo
import fs2.concurrent.Queue
import zio._
import zio.interop.catz.taskConcurrentInstance


package object service {

  type DialogService = Has[DialogService.Service]
  type DialogMessageStream = fs2.Stream[Task, DialogMessage]

  object DialogService {

    trait Service {
      def createUser(dialogId: DialogId, user: UserIdentity, out: Queue[Task[*], DialogMessage]): Task[Unit]
      def handleUserMsg(dialogId: DialogId, sender: UserIdentity, delivered: Boolean, content: String): Task[DialogMessage]
      def checkReceiver(dialogId: DialogId): Task[Boolean]
      def remove(dialogId: DialogId): Task[Unit]
      def getDialogMessages(dialogId: DialogId): DialogMessageStream
      def delivered(dialogId: DialogId, receiver: UserIdentity): Task[Unit]
    }

    val live: ZLayer[Any with Has[UserDialogRepo.Service], Throwable, Has[Service]] =
      ZLayer.fromServiceM[UserDialogRepo.Service, Any, Throwable, DialogService.Service] { repo =>
        for {
          users <- Ref.make(Map[DialogId, DialogUser]())
        } yield new DialogServiceLive(users, repo)
      }

    def createUser(dialogId: DialogId, user: UserIdentity, out: Queue[Task[*], DialogMessage]): ZIO[DialogService, Throwable, Unit] =
      ZIO.accessM[DialogService](_.get.createUser(dialogId, user, out))

    def handleUserMsg(dialogId: DialogId, sender: UserIdentity, delivered: Boolean, content: String): ZIO[DialogService, Throwable, DialogMessage] =
      ZIO.accessM[DialogService](_.get.handleUserMsg(dialogId, sender, delivered, content))

    def checkReceiver(dialogId: DialogId): ZIO[DialogService, Throwable, Boolean] =
      ZIO.accessM[DialogService](_.get.checkReceiver(dialogId))

    def remove(dialogId: DialogId): ZIO[DialogService, Throwable, Unit] =
      ZIO.accessM[DialogService](_.get.remove(dialogId))

    def getDialogMessages(dialogId: DialogId): ZIO[DialogService, Throwable, DialogMessageStream] =
      ZIO.access[DialogService](_.get.getDialogMessages(dialogId))

    def delivered(dialogId: DialogId, receiver: UserIdentity): ZIO[DialogService, Throwable, Unit] =
      ZIO.accessM[DialogService](_.get.delivered(dialogId, receiver))
  }
}
