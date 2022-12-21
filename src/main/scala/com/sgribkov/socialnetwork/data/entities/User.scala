package com.sgribkov.socialnetwork.data.entities

import com.sgribkov.socialnetwork.data.dto.UserProfileDTO
import doobie.Read

final case class User(id: UserId,
                      login: UserLogin,
                      firstName: String,
                      lastName: String,
                      age: Int,
                      gender: String,
                      city: String,
                      interests: UserInterests,
                     ) {
  def toDTO: UserProfileDTO =
    UserProfileDTO(firstName, lastName, age, gender, city, interests)
}

object User {
  type UserRec = (String, String, String, String, Int, String, String, String)
  implicit val userRead: Read[User] = Read[UserRec].map {
    case (id, login, firstName, lastName, age, gender, city, interests) =>
      User(
        UserId(id),
        UserLogin(login),
        firstName,
        lastName,
        age,
        gender,
        city,
        UserInterests.fromJsonStr(interests)
      )
  }
}
