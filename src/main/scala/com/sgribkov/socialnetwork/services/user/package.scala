package com.sgribkov.socialnetwork.services

import com.sgribkov.socialnetwork.data.Error._
import com.sgribkov.socialnetwork.data.dto.{PasswordChangeDTO, UserProfileDTO, UserRegDTO}
import com.sgribkov.socialnetwork.data.entities.{User, UserFriendship, UserId, UserIdentity, UserLogin}
import com.sgribkov.socialnetwork.repository.userauth.UserAuthRepo
import com.sgribkov.socialnetwork.repository.userfriendship.UserFriendshipRepo
import com.sgribkov.socialnetwork.repository.userprofile.UserProfileRepo
import zio.{Has, RIO, Task, UIO, ULayer, ZIO, ZLayer}
import zio.random.Random


package object user {

  type UserService = Has[UserService.Service]

  object UserService {

    trait Service {
      def findUserProfile(login: UserLogin): RIO[UserProfileRepo, User]
      def registerUser(userData: UserRegDTO): RIO[UserProfileRepo with UserAuthRepo with Random, Unit]
      def updateUserProfile(identity: UserIdentity, userData: UserProfileDTO): RIO[UserProfileRepo, Unit]
      def changeUserPassword(userId: UserId, pwds: PasswordChangeDTO): RIO[UserAuthRepo, Unit]
      def deleteUser(id: UserId): RIO[UserProfileRepo with UserAuthRepo with UserFriendshipRepo, Unit]
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

      override def findUserProfile(login: UserLogin): RIO[UserProfileRepo, User] =
        UserProfileRepo
          .findByLogin(login)
          .flatMap(u => Task.require(UserNotFound(login))(Task.succeed(u)))

      override def registerUser(userData: UserRegDTO): RIO[UserProfileRepo with UserAuthRepo with Random, Unit] =
        for {
          uuid <- zio.random.nextUUID
          id = UserId(uuid.toString)
          auth <- userData.auth.toUserAuth(id)
          identity = auth.toIdentity
          user <- userData.profile.toUser(identity)
          _ <- checkBooleanResult(UserAuthRepo.insert(auth), UserAlreadyExists(userData.auth.login))
          _ <- checkBooleanResult(UserProfileRepo.insert(user), UserAlreadyExists(userData.auth.login))
        } yield ()

      override def updateUserProfile(identity: UserIdentity, userData: UserProfileDTO): RIO[UserProfileRepo, Unit] =
        for {
          user <- userData.toUser(identity)
          _ <- checkBooleanResult(UserProfileRepo.update(user), CanNotUpdateUserProfile)
        } yield ()

      override def changeUserPassword(userId: UserId, pwds: PasswordChangeDTO): RIO[UserAuthRepo, Unit] = {
        checkBooleanResult(
          UserAuthRepo.update(userId, pwds.password1),
          UserPasswordError("Enter another password.")
        )
      }

      override def deleteUser(id: UserId): RIO[UserProfileRepo with UserAuthRepo with UserFriendshipRepo, Unit] =
          checkBooleanResult(UserProfileRepo.delete(id), CanNotDeleteUserData("profile")) *>
            checkBooleanResult(UserAuthRepo.deactivate(id), CanNotDeleteUserData("auth"))

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

    val live: ULayer[UserService] = ZLayer.succeed(new Impl)


    def findUserProfile(userLogin: UserLogin): RIO[UserService with UserProfileRepo, User] =
      RIO.accessM(_.get.findUserProfile(userLogin))

    def registerUser(userData: UserRegDTO): RIO[UserService with UserAuthRepo with Random with UserProfileRepo, Unit] =
      RIO.accessM(_.get.registerUser(userData))

    def updateUserProfile(identity: UserIdentity, userData: UserProfileDTO): RIO[UserService with UserProfileRepo, Unit] =
      RIO.accessM(_.get.updateUserProfile(identity, userData))

    def changeUserPassword(userId: UserId, pwds: PasswordChangeDTO): RIO[UserService with UserAuthRepo, Unit] =
      RIO.accessM(_.get.changeUserPassword(userId, pwds))

    def deleteUser(id: UserId): RIO[UserService with UserProfileRepo with UserAuthRepo with UserFriendshipRepo, Unit] =
      RIO.accessM(_.get.deleteUser(id))

    def getFriends(login: UserLogin): RIO[UserService with UserFriendshipRepo, List[UserLogin]] =
      RIO.accessM(_.get.getFriends(login))

    def follow(user: UserIdentity, friendLogin: UserLogin): RIO[UserService with UserFriendshipRepo with UserProfileRepo, Unit] =
      RIO.accessM(_.get.follow(user, friendLogin))

    def unfollow(user: UserIdentity, friendLogin: UserLogin): RIO[UserService with UserFriendshipRepo with UserProfileRepo, Unit] =
      RIO.accessM(_.get.unfollow(user, friendLogin))
  }
}
