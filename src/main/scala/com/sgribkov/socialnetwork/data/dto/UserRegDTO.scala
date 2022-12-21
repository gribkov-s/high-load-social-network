package com.sgribkov.socialnetwork.data.dto

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

final case class UserRegDTO(auth: UserAuthDTO, profile: UserProfileDTO)

object UserRegDTO {
  implicit val codec: Codec[UserRegDTO] = deriveCodec
}
