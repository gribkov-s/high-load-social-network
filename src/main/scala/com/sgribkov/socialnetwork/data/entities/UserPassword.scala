package com.sgribkov.socialnetwork.data.entities

import java.security.MessageDigest
import cats.data.Validated
import io.circe.{Decoder, Encoder}

final case class UserPassword(value: String) extends AnyVal {

  def validate: Validated[String, UserPassword] =
    if (value.length >= 5)
      Validated.Valid(UserPassword(value))
    else
      Validated.Invalid("Password length must be at least 5 characters.")

  def encrypt: String =
    MessageDigest.getInstance("MD5")
      .digest(value.getBytes)
      .map("%02X".format(_)).mkString
}

object UserPassword {
  implicit val encoder: Encoder[UserPassword] = Encoder.encodeString.contramap(_.value)
  implicit val decoder: Decoder[UserPassword] = Decoder.decodeString.map(UserPassword.apply)
}


