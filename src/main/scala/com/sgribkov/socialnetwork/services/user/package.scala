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
      def deleteUser(id: UserId): RIO[UserProfileRepo with UserAuthRepo, Unit]
    }

    class Impl extends Service {

      private def checkBooleanResult[R](rio: RIO[R, Boolean], err: Throwable): ZIO[R, Throwable, Unit] =
        rio.flatMap {
          case true => UIO.unit
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

      override def deleteUser(id: UserId): RIO[UserProfileRepo with UserAuthRepo, Unit] =
        checkBooleanResult(UserProfileRepo.delete(id), CanNotDeleteUserData("profile")) *>
          checkBooleanResult(UserAuthRepo.deactivate(id), CanNotDeleteUserData("auth"))
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

    def deleteUser(id: UserId): RIO[UserService with UserProfileRepo with UserAuthRepo, Unit] =
      RIO.accessM(_.get.deleteUser(id))
  }
}
