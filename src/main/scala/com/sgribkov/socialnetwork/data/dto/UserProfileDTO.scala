package com.sgribkov.socialnetwork.data.dto

import cats.data.{Validated, ValidatedNec}
import com.sgribkov.socialnetwork.data.entities.{User, UserIdentity, UserInterests}
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import cats.implicits.catsSyntaxTuple6Semigroupal
import com.sgribkov.socialnetwork.data.Error.DtoDataError
import zio.Task

final case class UserProfileDTO(firstName: String,
                                lastName: String,
                                age: Int,
                                gender: String,
                                city: String,
                                interests: UserInterests
                               ) { self =>

  private def checkCapitalLetter(str: String, field: String): Validated[String, String] =
    if (str.matches("^[A-Z].*"))
      Validated.Valid(str)
    else
      Validated.Invalid(s"$field must starts with a capital letter.")

  private def checkGender(gender: String): Validated[String, String] =
    gender match {
      case "M" | "Male" => Validated.Valid("Male")
      case "F" | "Female" => Validated.Valid("Female")
      case "U" | "Unknown" => Validated.Valid("Unknown")
      case _ => Validated.Invalid(s"Field 'gender' can contain only these values: M / Male, F / Female, U / Unknown.")
    }

  private def checkAge(age: Int): Validated[String, Int] =
    if (age >= 0 && age <= 200)
      Validated.Valid(age)
    else
      Validated.Invalid(s"Age must be between 0 and 200.")

  private def validate: ValidatedNec[String, UserProfileDTO] = (
    checkCapitalLetter(self.firstName, "First name").toValidatedNec,
    checkCapitalLetter(self.lastName, "Last name").toValidatedNec,
    checkAge(self.age).toValidatedNec,
    checkGender(self.gender).toValidatedNec,
    checkCapitalLetter(self.city, "City").toValidatedNec,
    Validated.Valid(self.interests).toValidatedNec
    ).mapN {
    (first, last, age, gen, city, ints) =>
      UserProfileDTO(first, last, age, gen, city, ints)
    }

  def toUser(id: UserIdentity): Task[User] = validate.fold(
    err => Task.fail(DtoDataError(err.toChain.toList.mkString(" "))),
    dto => Task.succeed(
      User(
        id.userId,
        id.userLogin,
        dto.firstName,
        dto.lastName,
        dto.age,
        dto.gender,
        dto.city,
        dto.interests
      )
    )
  )
}

object UserProfileDTO {
  implicit val codec: Codec[UserProfileDTO] = deriveCodec
}


