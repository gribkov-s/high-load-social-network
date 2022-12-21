package com.sgribkov.socialnetwork.data.entities

import com.sgribkov.socialnetwork.data.dto.UserAuthDTO

final case class UserAuth(id: UserId, login: UserLogin, password: UserPassword) {
  def toDTO: UserAuthDTO = UserAuthDTO(login, password)
  def toIdentity: UserIdentity = UserIdentity(id, login)
}
