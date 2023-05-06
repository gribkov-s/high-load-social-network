package com.sgribkov.socialnetwork.services

import com.sgribkov.socialnetwork.data.Error._
import com.sgribkov.socialnetwork.data.entities.{UserFriendship, UserId, UserIdentity, UserLogin}
import com.sgribkov.socialnetwork.repository.userfriendship.UserFriendshipRepo
import com.sgribkov.socialnetwork.repository.userprofile.UserProfileRepo
import zio.{Has, RIO, Task, UIO, ULayer, ZIO, ZLayer}


package object friendship {

  type FriendshipService = Has[FriendshipService.Service]

  object FriendshipService {

    trait Service {
      def getFriends(login: UserLogin): RIO[UserFriendshipRepo, List[UserLogin]]
      def follow(user: UserIdentity, friendLogin: UserLogin): RIO[UserFriendshipRepo with UserProfileRepo, Unit]
      def unfollow(user: UserIdentity, friendLogin: UserLogin): RIO[UserFriendshipRepo with UserProfileRepo, Unit]
    }

    class Impl extends Service {

      private def checkBooleanResult[R](rio: RIO[R, Boolean], err: Throwable): ZIO[R, Throwable, Unit] =
        rio.flatMap {
          case true  => UIO.unit
          case false => Task.fail(err)
        }

      override def getFriends(login: UserLogin): RIO[UserFriendshipRepo, List[UserLogin]] =
        UserFriendshipRepo
          .getFriendsByLogin(login)

      override def follow(user: UserIdentity, friendLogin: UserLogin): RIO[UserFriendshipRepo with UserProfileRepo, Unit] =
        for {
          _ <- UserFriendshipRepo.getFriendsByLogin(friendLogin).flatMap {
            case friends if friends.contains(user.userLogin) => Task.fail(FriendAlreadyExists(friendLogin))
            case _ => UIO.unit
          } ///
          friendId <- UserProfileRepo.getUserId(friendLogin)
            .mapBoth(_ => UserNotFound(friendLogin), id => UserId(id.get))
          friendship = UserFriendship(user.userId, user.userLogin, friendId, friendLogin)
          _ <- checkBooleanResult(UserFriendshipRepo.insert(friendship), FriendAlreadyExists(friendLogin))
        } yield ()

      override def unfollow(user: UserIdentity, friendLogin: UserLogin): RIO[UserFriendshipRepo with UserProfileRepo, Unit] =
        for {
          _ <- UserFriendshipRepo.getFriendsByLogin(friendLogin).flatMap {
            case friends if friends.contains(user.userLogin) => UIO.unit
            case _ => Task.fail(FriendNotExists(friendLogin))
          } ///
          friendId <- UserProfileRepo.getUserId(friendLogin)
            .mapBoth(_ => UserNotFound(friendLogin), id => UserId(id.get))
          _ <- checkBooleanResult(UserFriendshipRepo.unfollow(user.userId, friendId), FriendNotExists(friendLogin))
        } yield ()
    }

    val live: ULayer[FriendshipService] = ZLayer.succeed(new Impl)

    def getFriends(login: UserLogin): RIO[FriendshipService with UserFriendshipRepo, List[UserLogin]] =
      RIO.accessM(_.get.getFriends(login))

    def follow(user: UserIdentity, friendLogin: UserLogin): RIO[FriendshipService with UserFriendshipRepo with UserProfileRepo, Unit] =
      RIO.accessM(_.get.follow(user, friendLogin))

    def unfollow(user: UserIdentity, friendLogin: UserLogin): RIO[FriendshipService with UserFriendshipRepo with UserProfileRepo, Unit] =
      RIO.accessM(_.get.unfollow(user, friendLogin))
  }
}
