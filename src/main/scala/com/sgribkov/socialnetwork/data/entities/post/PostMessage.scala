package com.sgribkov.socialnetwork.data.entities.post

import com.sgribkov.socialnetwork.data.entities.{UserId, UserIdentity, UserLogin}
import java.sql.Timestamp
import java.util.UUID.randomUUID
import doobie.implicits.javasql.TimestampMeta
import doobie.Read
import java.util.UUID.fromString


case class PostMessage(postId: PostId,
                       postTime: Timestamp,
                       publisherId: UserId,
                       publisherLogin: UserLogin,
                       content: PostMsgBody
                      ) {

  def toSending(subscriberId: UserId): PostSending =
    PostSending(postId, postTime, publisherId, subscriberId)

  def getTimeDouble: Double = postTime.getTime.toDouble
}

object PostMessage {

  def from(publisher: UserIdentity,
           content: PostMsgBody
          ): PostMessage = {

    val defaultUUID = fromString("00000000-0000-0000-0000-000000000000")

    val postId =
      if (content.text == "test")
        PostId(defaultUUID.toString)
      else
        PostId(randomUUID.toString)

    val time = new Timestamp(System.currentTimeMillis())

    PostMessage(
      postId,
      time,
      publisher.userId,
      publisher.userLogin,
      content
    )
  }

  type PostMessageRec = (String, Timestamp, String, String, String)

  implicit val dialogMsgRead: Read[PostMessage] = Read[PostMessageRec].map {
    case (postId, postTime, pubId, pubLogin, body) =>
      PostMessage(
        PostId(postId),
        postTime,
        UserId(pubId),
        UserLogin(pubLogin),
        PostMsgBody(body)
      )
  }

}
