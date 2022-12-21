package com.sgribkov.socialnetwork.repository

import com.sgribkov.socialnetwork.data.entities.{User, UserId, UserLogin}
import zio.{Has, RIO, Task}

package object userprofile {

  type UserProfileRepo = Has[UserProfileRepo.Service]

  object UserProfileRepo {

    trait Service {
      def getUserId(login: UserLogin): Task[Option[String]]
      def findByLogin(login: UserLogin): Task[Option[User]]
      def insert(user: User): Task[Boolean]
      def update(user: User): Task[Boolean]
      def delete(id: UserId): Task[Boolean]
    }

    def getUserId(login: UserLogin): RIO[UserProfileRepo, Option[String]] =
      RIO.accessM(_.get.getUserId(login))

    def findByLogin(login: UserLogin): RIO[UserProfileRepo, Option[User]] =
      RIO.accessM(_.get.findByLogin(login))

    def insert(user: User): RIO[UserProfileRepo, Boolean]=
      RIO.accessM(_.get.insert(user))

    def update(user: User): RIO[UserProfileRepo, Boolean] =
      RIO.accessM(_.get.update(user))

    def delete(id: UserId): RIO[UserProfileRepo, Boolean] =
      RIO.accessM(_.get.delete(id))
  }

}
