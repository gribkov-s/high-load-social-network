package com.sgribkov.socialnetwork.data.dto

import java.text.SimpleDateFormat

import com.sgribkov.socialnetwork.data.entities.UserLogin
import com.sgribkov.socialnetwork.data.entities.post.{PostMessage, PostMsgBody}
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec


final case class PostMessageDTO(time: String,
                                content: PostMsgBody
                               )

object PostMessageDTO {

  implicit val codec: Codec[PostMessageDTO] = deriveCodec

  def from(post: PostMessage): PostMessageDTO = {
    val formattedTime =
      new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(post.postTime)
    PostMessageDTO(formattedTime, post.content)
  }
}




