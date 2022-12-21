package com.sgribkov.socialnetwork.repository

import com.sgribkov.socialnetwork.data.entities.{UserFriendship, UserId, UserLogin}
import zio.{Has, RIO, Task}

package object userfriendship {

  type UserFriendshipRepo = Has[UserFriendshipRepo.Service]

  object UserFriendshipRepo {

    trait Service {
      def getFriendsByLogin(login: UserLogin): Task[List[UserLogin]]
      def insert(friendship: UserFriendship): Task[Boolean]
      def unfollow(id: UserId, friendId: UserId): Task[Boolean]
    }

    def getFriendsByLogin(login: UserLogin): RIO[UserFriendshipRepo, List[UserLogin]] =
      RIO.accessM(_.get.getFriendsByLogin(login))

    def insert(friendship: UserFriendship): RIO[UserFriendshipRepo, Boolean] =
      RIO.accessM(_.get.insert(friendship))

    def unfollow(id: UserId, friendId: UserId): RIO[UserFriendshipRepo, Boolean] =
      RIO.accessM(_.get.unfollow(id, friendId))
  }
}
