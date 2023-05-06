package com.sgribkov.socialnetwork.data.entities

import java.util.UUID

final case class UserIdentity(userId: UserId, userLogin: UserLogin)

object UserIdentity {
  private val defaultId = new UUID(0, 0).toString
  val DEFAULT: UserIdentity = UserIdentity(UserId(defaultId), UserLogin("unknown"))
}
