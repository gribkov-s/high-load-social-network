package com.sgribkov.socialnetwork.http.api

import com.sgribkov.socialnetwork.data.dto.{PasswordChangeDTO, UserProfileDTO}
import com.sgribkov.socialnetwork.data.entities.{UserIdentity, UserLogin}
import com.sgribkov.socialnetwork.repository.userauth.UserAuthRepo
import com.sgribkov.socialnetwork.repository.userfriendship.UserFriendshipRepo
import com.sgribkov.socialnetwork.repository.userprofile.UserProfileRepo
import com.sgribkov.socialnetwork.services.friendship.FriendshipService
import com.sgribkov.socialnetwork.services.user.UserService
import io.circe.{Decoder, Encoder}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.{AuthedRoutes, EntityDecoder, EntityEncoder}
import zio.RIO
import zio.interop.catz.taskConcurrentInstance
import zio.random.Random


class UserFriendshipAPI[R <: FriendshipService with
                        UserProfileRepo with
                        UserFriendshipRepo] {

  type FriendshipTask[A] =  RIO[R, A]

  val dsl = Http4sDsl[FriendshipTask]
  import dsl._

  implicit def jsonDecoder[A](implicit decoder: Decoder[A]): EntityDecoder[FriendshipTask, A] =
    jsonOf[FriendshipTask, A]

  implicit def jsonEncoder[A](implicit decoder: Encoder[A]): EntityEncoder[FriendshipTask, A] =
    jsonEncoderOf[FriendshipTask, A]

  val routes: AuthedRoutes[UserIdentity, FriendshipTask] = AuthedRoutes.of[UserIdentity, FriendshipTask] {

    case GET -> Root / "friends" as userIdentity =>
      FriendshipService.getFriends(userIdentity.userLogin).foldM(
        _ => NotFound(s"Not found user ${userIdentity.userLogin.value}."),
        friends => Ok(friends)
      )

    case GET -> Root / login / "friends" as userIdentity =>
      FriendshipService.getFriends(UserLogin(login)).foldM(
        _ => NotFound(s"Not found user $login."),
        friends => Ok(friends)
      )

    case POST -> Root / login / "follow" as userIdentity => {
      val friendLogin = UserLogin(login)
      FriendshipService.follow(userIdentity, friendLogin).foldM(
          err => BadRequest(err.getMessage),
          result => Ok(s"User $login was followed.")
        )
      }

    case DELETE -> Root / login / "unfollow" as userIdentity => {
      val friendLogin = UserLogin(login)
      FriendshipService.unfollow(userIdentity, friendLogin).foldM(
        err => BadRequest(err.getMessage),
        _ => Ok(s"User $login was unfollowed.")
      )
    }
  }
}
