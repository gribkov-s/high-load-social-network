package com.sgribkov.socialnetwork.data.dto

import java.nio.charset.StandardCharsets
import java.util.Base64

import cats.data.ValidatedNec
import com.sgribkov.socialnetwork.data.entities.{UserAuth, UserId, UserLogin, UserPassword}
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import cats.implicits.catsSyntaxTuple2Semigroupal
import com.sgribkov.socialnetwork.data.Error.DtoDataError
import zio.Task

final case class UserAuthDTO(login: UserLogin, password: UserPassword) { self =>

  private def validate: ValidatedNec[String, UserAuthDTO] = (
    self.login.validate.toValidatedNec,
    self.password.validate.toValidatedNec
    ).mapN{(login, pwd) => UserAuthDTO(login, pwd)}

  def toUserAuth(id: UserId): Task[UserAuth] = validate.fold(
    err => Task.fail(DtoDataError(err.toChain.toList.mkString(" "))),
    dto => Task.succeed(UserAuth(id, dto.login, dto.password))
  )
}

object UserAuthDTO {

  implicit val codec: Codec[UserAuthDTO] = deriveCodec

  def fromBase64(b64Str: String): UserAuthDTO = {
    val decoded = Base64.getDecoder.decode(b64Str)
    val str = new String(decoded, StandardCharsets.UTF_8)
    val splitted = str.split(":")
    val login = UserLogin(splitted.head)
    val pwd = UserPassword(splitted.last)
    UserAuthDTO(login, pwd)
  }
}
