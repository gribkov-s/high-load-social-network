package com.sgribkov.socialnetwork.data.entities.dialog

import io.circe.{Decoder, Encoder}

final case class DialogMsgId(value: String) extends AnyVal

object DialogMsgId {
  implicit val encoder: Encoder[DialogMsgId] = Encoder.encodeString.contramap(_.value)
  implicit val decoder: Decoder[DialogMsgId] = Decoder.decodeString.map(DialogMsgId.apply)
}
