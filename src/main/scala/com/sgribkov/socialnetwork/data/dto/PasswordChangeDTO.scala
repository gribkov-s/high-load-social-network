package com.sgribkov.socialnetwork.data.dto

import com.sgribkov.socialnetwork.data.entities.UserPassword
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import cats.data.{Validated, ValidatedNec}
import com.sgribkov.socialnetwork.data.Error._
import cats.implicits.catsSyntaxTuple2Semigroupal

final case class PasswordChangeDTO(password1: UserPassword,
                                   password2: UserPassword
                                  ) {self =>

  private def compare(dto: PasswordChangeDTO): Validated[UserPasswordError, PasswordChangeDTO] =
    if (dto.password1 == dto.password2)
      Validated.Valid(dto)
    else
      Validated.Invalid(UserPasswordError("Passwords do not match."))

  private def checkLength(dto: PasswordChangeDTO): ValidatedNec[String, PasswordChangeDTO] = (
    dto.password1.validate.toValidatedNec,
    dto.password2.validate.toValidatedNec
    ).mapN{(pwd1, pwd2) => PasswordChangeDTO(pwd1, pwd2)}

  def validate: Validated[UserPasswordError, PasswordChangeDTO] = {
    val validatedFirst = checkLength(self)
    validatedFirst.fold(
      err => Validated.Invalid(UserPasswordError(err.head)),
      pcd => compare(pcd)
    )
  }
}

object PasswordChangeDTO {
  implicit val codec: Codec[PasswordChangeDTO] = deriveCodec
}

