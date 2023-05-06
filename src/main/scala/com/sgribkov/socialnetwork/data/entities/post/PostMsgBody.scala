package com.sgribkov.socialnetwork.data.entities.post

import io.circe.{Decoder, Encoder}

final case class PostMsgBody(text: String) extends AnyVal

object PostMsgBody {
  implicit val encoder: Encoder[PostMsgBody] = Encoder.encodeString.contramap(_.text)
  implicit val decoder: Decoder[PostMsgBody] = Decoder.decodeString.map(PostMsgBody.apply)
}
