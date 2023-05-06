package com.sgribkov.socialnetwork.data.dto

import java.text.SimpleDateFormat

import com.sgribkov.socialnetwork.data.entities.UserLogin
import com.sgribkov.socialnetwork.data.entities.post.{PostMessage, PostMsgBody}
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec


final case class PostFeedMessageDTO(time: String,
                                    publisher: UserLogin,
                                    content: PostMsgBody
                                   )

object PostFeedMessageDTO {

  implicit val codec: Codec[PostFeedMessageDTO] = deriveCodec

  def from(post: PostMessage) = {
    val formattedTime =
      new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(post.postTime)
    PostFeedMessageDTO(formattedTime, post.publisherLogin, post.content)
  }
}






