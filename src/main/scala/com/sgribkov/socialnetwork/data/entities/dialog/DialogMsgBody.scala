package com.sgribkov.socialnetwork.data.entities.dialog

import io.circe.{Decoder, Encoder}

final case class DialogMsgBody(value: String) extends AnyVal

object DialogMsgBody {
  implicit val encoder: Encoder[DialogMsgBody] = Encoder.encodeString.contramap(_.value)
  implicit val decoder: Decoder[DialogMsgBody] = Decoder.decodeString.map(DialogMsgBody.apply)
}


