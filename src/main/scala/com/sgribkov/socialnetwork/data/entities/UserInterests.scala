package com.sgribkov.socialnetwork.data.entities

import io.circe.parser.decode
import io.circe.{Decoder, Encoder}

case class UserInterests(interests: List[String]) extends AnyVal {
  def toJsonStr: String = {
    interests.map(v => s"${'"'}$v${'"'}").mkString("[", ",", "]")
  }
}

object UserInterests {
  def fromJsonStr(str: String): UserInterests = {
    val l =
      decode[List[String]](str)
        .fold(_ => List[String](), l => l)

    UserInterests(l)
  }
  implicit val encoder: Encoder[UserInterests] = Encoder.encodeList[String].contramap(_.interests)
  implicit val decoder: Decoder[UserInterests] = Decoder.decodeList[String].map(UserInterests.apply)
}
