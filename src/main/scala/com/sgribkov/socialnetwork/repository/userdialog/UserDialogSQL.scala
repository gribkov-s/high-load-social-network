package com.sgribkov.socialnetwork.repository.userdialog

import cats.Eq
import com.sgribkov.socialnetwork.data.Error._
import com.sgribkov.socialnetwork.data.entities.dialog.{DialogId, DialogMessage}
import com.sgribkov.socialnetwork.data.entities.{UserId, UserIdentity, UserLogin}
import com.sgribkov.socialnetwork.repository.userdialog.UserDialogSQL.Queries
import com.sgribkov.socialnetwork.system.dbtransactor.DBTransactor
import doobie.implicits._
import doobie.util.transactor.Transactor
import doobie.{Query0, Update0}
import zio._
import doobie.implicits.javasql.TimestampMeta
import zio.interop.catz._

class UserDialogSQL(trx: Transactor[Task]) extends UserDialogRepo.Service {

  override def getDialogMessages(dialog: DialogId): fs2.Stream[Task, DialogMessage] =
    Queries
      .getDialogMessages(dialog)
      .stream
      .transact(trx)

  override def insert(msg: DialogMessage): Task[Boolean] =
    Queries
      .insert(msg)
      .run
      .transact(trx)
      .mapBoth(
        err => DatabaseError(err.getMessage),
        rows => if (rows == 0) false else true
      )

  override def updateDelivered(dialog: DialogId, user: UserIdentity): Task[Boolean] =
    Queries
      .updateDelivered(dialog, user)
      .run
      .transact(trx)
      .mapBoth(
        err => DatabaseError(err.getMessage),
        rows => if (rows == 0) false else true
      )
}

object UserDialogSQL {

  object Queries {

    def getDialogMessages(dialog: DialogId): Query0[DialogMessage] =
      sql"""
           SELECT message_id,
                  v_bucket,
                  dialog_id,
                  message_time,
                  sender_id,
                  sender_login,
                  delivered,
                  body
           FROM   user_dialog
           WHERE  v_bucket = ${dialog.getVBucket} AND
                  dialog_id = ${dialog.value}
           ORDER BY message_time ASC
           """.query[DialogMessage]

    def insert(msg: DialogMessage): Update0 =
      sql"""
           INSERT INTO user_dialog
           VALUES
           (
              ${msg.messageId.value},
              ${msg.dialogId.getVBucket},
              ${msg.dialogId.value},
              ${msg.messageTime},
              ${msg.senderId.value},
              ${msg.senderLogin.value},
              ${msg.delivered},
              ${msg.content}
           )
         """.update

    def updateDelivered(dialog: DialogId, user: UserIdentity): Update0 =
      sql"""
           UPDATE user_dialog
           SET delivered = TRUE
           WHERE  v_bucket = ${dialog.getVBucket} AND
                  dialog_id = ${dialog.value} AND
                  sender_id <> ${user.userLogin.value} AND
                  delivered = FALSE
         """.update
  }

  val live: ZLayer[DBTransactor, Throwable, UserDialogRepo] =
    ZLayer.fromEffect(
      DBTransactor.transactor.map(new UserDialogSQL(_))
    )
}