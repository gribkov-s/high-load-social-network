package com.sgribkov.socialnetwork.services.dialog.service

import com.sgribkov.socialnetwork.data.Error._
import com.sgribkov.socialnetwork.data.entities.dialog.{DialogId, DialogMessage, DialogUser}
import com.sgribkov.socialnetwork.data.entities.UserIdentity
import com.sgribkov.socialnetwork.repository.userdialog.UserDialogRepo
import fs2.concurrent.Queue
import zio.CanFail.canFailAmbiguous1
import zio._


class DialogServiceLive(users: Ref[Map[DialogId, DialogUser]],
                        repo: UserDialogRepo.Service
                       ) extends DialogService.Service {

  override def createUser(dialogId: DialogId, user: UserIdentity, out: Queue[Task, DialogMessage]): Task[Unit] = {
    for {
      dialogUser <- ZIO.succeed(DialogUser(user, out))
      _ <- users.update(_ + (dialogId -> dialogUser))
    } yield dialogUser
  }

  override def handleUserMsg(dialogId: DialogId,
                             sender: UserIdentity,
                             delivered: Boolean,
                             content: String
                            ): Task[DialogMessage] =
    for {
      msg <- ZIO.succeed(DialogMessage.from(dialogId.normalize, sender, delivered, content))
      _ <- repo.insert(msg).flatMap {
        case true  => UIO.unit
        case false => Task.fail(CanNotSaveMessage(dialogId, sender.userLogin))
      }
      _ <- broadcast(dialogId, msg)
    } yield msg

  override def checkReceiver(dialogId: DialogId): Task[Boolean] =
    for {
      usrs  <- users.get
      hasRcv = usrs.contains(dialogId.reverse)
    } yield hasRcv

  override def remove(dialogId: DialogId): Task[Unit] =
    for {
      _ <- users.modify(du => (dialogId, du - dialogId))
    } yield ()

  override def getDialogMessages(dialogId: DialogId): DialogMessageStream =
    repo.getDialogMessages(dialogId.normalize)

  override def delivered(dialogId: DialogId, receiver: UserIdentity): Task[Unit] = {
    for {
      _ <- repo.updateDelivered(dialogId.normalize, receiver)
    } yield ()
  }

  private def zioUnit: ZIO[Any, Throwable, Unit] = ZIO.unit

  private def broadcast(dialogId: DialogId, msg: DialogMessage): Task[Unit] =
    for {
      usrs  <- users.get
      _ <- usrs.foldLeft(zioUnit) {
        case (acc, (d, u)) =>
          if (d == dialogId || d == dialogId.reverse)
            acc *> u.out.enqueue1(msg)
          else
            acc
      }
    } yield ()
}
