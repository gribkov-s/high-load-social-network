package com.sgribkov.socialnetwork.data.entities.dialog

import java.sql.Timestamp

import com.sgribkov.socialnetwork.data.dto.DialogMessageDTO
import doobie.Read
import doobie.implicits.javasql.TimestampMeta
import java.util.UUID.randomUUID
import com.sgribkov.socialnetwork.data.entities.{UserId, UserIdentity, UserLogin}


case class DialogMessage(messageId: DialogMsgId,
                         vBucket: Int,
                         dialogId: DialogId,
                         messageTime: Timestamp,
                         senderId: UserId,
                         senderLogin: UserLogin,
                         delivered: Boolean,
                         content: DialogMsgBody
                        ) {
  def toDTO: DialogMessageDTO =
    DialogMessageDTO(messageTime.toString, senderLogin, content)
}

object DialogMessage {

  def from(dialogId: DialogId,
           sender: UserIdentity,
           delivered: Boolean,
           content: String
          ): DialogMessage = {

    val msgId = DialogMsgId(randomUUID.toString)
    val vBucket = dialogId.getVBucket
    val time = new Timestamp(System.currentTimeMillis())
    val body = DialogMsgBody(content)

    DialogMessage(
      msgId,
      vBucket,
      dialogId,
      time,
      sender.userId,
      sender.userLogin,
      delivered,
      body
    )
  }

  type DialogMessageRec = (String, Int, String, Timestamp, String, String, Boolean, String)

  implicit val userRead: Read[DialogMessage] = Read[DialogMessageRec].map {
    case (msgId, vBucket, dId, msgTime, sndId, sndLogin, delivered, body) =>
      DialogMessage(
        DialogMsgId(msgId),
        vBucket,
        DialogId(dId),
        msgTime,
        UserId(sndId),
        UserLogin(sndLogin),
        delivered,
        DialogMsgBody(body)
      )
  }

}
