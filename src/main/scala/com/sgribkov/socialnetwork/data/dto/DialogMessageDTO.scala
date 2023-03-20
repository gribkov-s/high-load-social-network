package com.sgribkov.socialnetwork.data.dto

import java.sql.Timestamp

import com.sgribkov.socialnetwork.data.entities.dialog.DialogMsgBody
import com.sgribkov.socialnetwork.data.entities.UserLogin
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec


final case class DialogMessageDTO(time: String,
                                  sender: UserLogin,
                                  content: DialogMsgBody
                                 )

object DialogMessageDTO {
  implicit val codec: Codec[DialogMessageDTO] = deriveCodec
}


