package com.sgribkov.socialnetwork.services

import com.sgribkov.socialnetwork.data.dto.UserAuthDTO
import com.sgribkov.socialnetwork.data.Error._
import com.sgribkov.socialnetwork.data.entities.UserIdentity
import com.sgribkov.socialnetwork.repository.userauth.UserAuthRepo
import zio.{Has, RIO, Task, ULayer, ZIO, ZLayer}

package object auth {

  type UserAuthService = Has[UserAuthService.Service]

  object UserAuthService {

    trait Service {
      def authenticate(authData: UserAuthDTO): RIO[UserAuthRepo, UserIdentity]
    }

    class Impl extends Service {
      override def authenticate(authData: UserAuthDTO): RIO[UserAuthRepo, UserIdentity] =
        UserAuthRepo.checkUserAuth(authData.login, authData.password).flatMap {
          case None  => Task.fail(UserAuthFailed(authData.login))
          case Some(id) => ZIO.succeed(UserIdentity(id, authData.login))
        }
    }

    val live: ULayer[UserAuthService] = ZLayer.succeed(new Impl)

    def authenticate(authData: UserAuthDTO): RIO[UserAuthService with UserAuthRepo, UserIdentity] =
      RIO.accessM(_.get.authenticate(authData))
  }
}
