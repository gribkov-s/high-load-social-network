package com.sgribkov.socialnetwork.repository

import com.sgribkov.socialnetwork.data.entities._
import com.sgribkov.socialnetwork.data.entities.dialog.{DialogId, DialogMessage}
import zio.{Has, RIO, Task}
import zio.interop.catz._

package object userdialog {

  type UserDialogRepo = Has[UserDialogRepo.Service]

  object UserDialogRepo {

    trait Service {
      def getDialogMessages(dialog: DialogId): fs2.Stream[Task, DialogMessage]
      def insert(msg: DialogMessage): Task[Boolean]
      def updateDelivered(dialog: DialogId, user: UserIdentity): Task[Boolean]
    }

    def getDialogMessages(dialog: DialogId): RIO[UserDialogRepo, fs2.Stream[Task, DialogMessage]] =
      RIO.access(_.get.getDialogMessages(dialog))

    def insert(msg: DialogMessage): RIO[UserDialogRepo, Boolean] =
      RIO.accessM(_.get.insert(msg))

    def updateDelivered(dialog: DialogId, user: UserIdentity): RIO[UserDialogRepo, Boolean] =
      RIO.accessM(_.get.updateDelivered(dialog, user))
  }

}
