package com.sgribkov.socialnetwork.repository

import com.sgribkov.socialnetwork.data.entities.{UserAuth, UserId, UserLogin, UserPassword}
import zio.{Has, RIO, Task}

package object userauth {

  type UserAuthRepo = Has[UserAuthRepo.Service]

  object UserAuthRepo {

    trait Service {
      def checkUserAuth(login: UserLogin, password: UserPassword): Task[Option[UserId]]
      def insert(userAuth: UserAuth): Task[Boolean]
      def update(id: UserId, password: UserPassword): Task[Boolean]
      def deactivate(id: UserId): Task[Boolean]
    }

    def checkUserAuth(login: UserLogin, password: UserPassword): RIO[UserAuthRepo, Option[UserId]] =
      RIO.accessM(_.get.checkUserAuth(login, password))

    def insert(userAuth: UserAuth): RIO[UserAuthRepo, Boolean] =
      RIO.accessM(_.get.insert(userAuth))

    def update(id: UserId, password: UserPassword): RIO[UserAuthRepo, Boolean] =
      RIO.accessM(_.get.update(id, password))

    def deactivate(id: UserId): RIO[UserAuthRepo, Boolean] =
      RIO.accessM(_.get.deactivate(id))
  }

}
