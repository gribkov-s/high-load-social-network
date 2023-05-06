package com.sgribkov.socialnetwork.data.entities.post

import com.sgribkov.socialnetwork.data.entities.{UserId, UserLogin}
import java.sql.Timestamp
import doobie.implicits.javasql.TimestampMeta
import doobie.Read

case class PostSending(postId: PostId,
                       postTime: Timestamp,
                       publisherId: UserId,
                       subscriberId: UserId
                      )

object PostSending {

  type PostSendingRec = (String, Timestamp, String, String)

  implicit val dialogMsgRead: Read[PostSending] = Read[PostSendingRec].map {
    case (postId, postTime, pubId, subId) =>
      PostSending(
        PostId(postId),
        postTime,
        UserId(pubId),
        UserId(subId)
      )
  }

}
