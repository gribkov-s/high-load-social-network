package com.sgribkov.socialnetwork.http.api

import zio.RIO
import io.circe.{Decoder, Encoder}
import org.http4s.{AuthedRoutes, EntityDecoder, EntityEncoder}
import org.http4s.circe._
import zio.interop.catz.taskConcurrentInstance
import org.http4s.dsl.Http4sDsl
import com.sgribkov.socialnetwork.services.user.UserService
import com.sgribkov.socialnetwork.data.dto.{PasswordChangeDTO, UserProfileDTO}
import com.sgribkov.socialnetwork.data.entities.{UserIdentity, UserLogin}
import com.sgribkov.socialnetwork.repository.userauth.UserAuthRepo
import com.sgribkov.socialnetwork.repository.userfriendship.UserFriendshipRepo
import com.sgribkov.socialnetwork.repository.userprofile.UserProfileRepo
import com.sgribkov.socialnetwork.services.auth.UserAuthService
import zio.random.Random


class UserAPI[R <: UserService with UserAuthService with UserProfileRepo with UserAuthRepo with UserFriendshipRepo with Random] {

  type UserTask[A] =  RIO[R, A]

  val dsl = Http4sDsl[UserTask]
  import dsl._

  implicit def jsonDecoder[A](implicit decoder: Decoder[A]): EntityDecoder[UserTask, A] =
    jsonOf[UserTask, A]

  implicit def jsonEncoder[A](implicit decoder: Encoder[A]): EntityEncoder[UserTask, A] =
    jsonEncoderOf[UserTask, A]

  val routes: AuthedRoutes[UserIdentity, UserTask] = AuthedRoutes.of[UserIdentity, UserTask] {

    case GET -> Root / "profile" as userIdentity =>
      UserService.findUserProfile(userIdentity.userLogin).foldM(
        _ => NotFound(s"Not found user ${userIdentity.userLogin.value}."),
        user => Ok(user.toDTO)
      )

    case reqUser @ PUT -> Root / "profile" / "update" as userIdentity =>
      reqUser.req.decode[UserProfileDTO] { user =>
        UserService.updateUserProfile(userIdentity, user).foldM(
          err => BadRequest(err.getMessage),
          _ => Ok(s"Profile was updated successfully.")
        )
      }

    case reqPwds @ PUT -> Root / "profile" / "change-password" as userIdentity =>
      reqPwds.req.decode[PasswordChangeDTO] { pwds =>
        pwds.validate.fold(
          errVld => BadRequest(errVld.getMessage),
          resultVld =>
            UserService.changeUserPassword(userIdentity.userId, resultVld).foldM(
              err => BadRequest(err.getMessage),
              _ => Ok("Password was changed successfully.")
            )
        )
      }

    case DELETE -> Root / "profile" / "delete" as userIdentity =>
      UserService.deleteUser(userIdentity.userId).foldM(
        err => BadRequest(err.getMessage),
        _ => Ok(s"User ${userIdentity.userLogin.value} was deleted successfully.")
      )

    case GET -> Root / "friends" as userIdentity =>
      UserService.getFriends(userIdentity.userLogin).foldM(
        _ => NotFound(s"Not found user ${userIdentity.userLogin.value}."),
        friends => Ok(friends)
      )

    case GET -> Root / login as userIdentity =>
      UserService.findUserProfile(UserLogin(login)).foldM(
        _ => NotFound(s"Not found user $login."),
        user => Ok(user.toDTO)
      )

    case GET -> Root / login / "friends" as userIdentity =>
      UserService.getFriends(UserLogin(login)).foldM(
        _ => NotFound(s"Not found user $login."),
        friends => Ok(friends)
      )

    case POST -> Root / login / "follow" as userIdentity => {
      val friendLogin = UserLogin(login)
        UserService.follow(userIdentity, friendLogin).foldM(
          err => BadRequest(err.getMessage),
          result => Ok(s"User $login was followed.")
        )
      }

    case DELETE -> Root / login / "unfollow" as userIdentity => {
      val friendLogin = UserLogin(login)
      UserService.unfollow(userIdentity, friendLogin).foldM(
        err => BadRequest(err.getMessage),
        _ => Ok(s"User $login was unfollowed.")
      )
    }
  }
}
