package com.sgribkov.socialnetwork.data.entities

import io.circe.{Decoder, Encoder}
import cats.data.Validated

final case class UserLogin(value: String) extends AnyVal {
  def validate: Validated[String, UserLogin] =
    if (value.matches("^[a-z]+[a-z0-9]{4,}$"))
      Validated.Valid(UserLogin(value))
    else
      Validated.Invalid(
        "Login must be in lower case, " +
        "starts with letters, " +
        "contains only letters and digits " +
        "and its length must be at least 5 characters"
      )
}

object UserLogin {
  implicit val encoder: Encoder[UserLogin] = Encoder.encodeString.contramap(_.value)
  implicit val decoder: Decoder[UserLogin] = Decoder.decodeString.map(UserLogin.apply)
}