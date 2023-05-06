package com.sgribkov.socialnetwork.data

import com.sgribkov.socialnetwork.data.entities.{UserId, UserLogin}
import com.sgribkov.socialnetwork.data.entities.dialog.DialogId
import com.sgribkov.socialnetwork.data.entities.post.PostId

import scala.util.control.NoStackTrace

sealed trait Error extends Throwable with NoStackTrace

object Error {

  final case class DatabaseError(error: String)
    extends Throwable(s"Error accessing repository layer: $error.") with Error

  final case class CacheError(error: String)
    extends Throwable(s"Error accessing repository layer: $error.") with Error

  final case class UserNotFound(userLogin: UserLogin)
    extends Throwable(s"User with login ${userLogin.value} was not found.") with Error

  final case class UserAuthFailed(userLogin: UserLogin)
    extends Throwable(s"Can not authenticate user with login ${userLogin.value}.") with Error

  final case object AuthBadFormat
    extends Throwable(s"Bad authentication format.") with Error

  final case class UserAlreadyExists(userLogin: UserLogin)
    extends Throwable(s"User ${userLogin.value} already exists.") with Error

  final case class FriendAlreadyExists(friendLogin: UserLogin)
    extends Throwable(s"Friend ${friendLogin.value} already exists.") with Error

  final case class FriendNotExists(friendLogin: UserLogin)
    extends Throwable(s"Friend ${friendLogin.value} not exists.") with Error

  final case object CanNotUpdateUserProfile
    extends Throwable(s"Can not update you profile.") with Error

  final case class CanNotDeleteUserData(data: String)
    extends Throwable(s"Can not delete you $data.") with Error

  final case class UserPasswordError(msg: String) extends Error {
    override def getMessage: String = msg
  }

  final case class UserLoginError(msg: String) extends Error {
    override def getMessage: String = msg
  }

  final case class DtoDataError(msg: String) extends Error {
    override def getMessage: String = msg
  }

  final case class CanNotSaveMessage(dialogId: DialogId, sender: UserLogin)
    extends Throwable(s"Can not save message id dialog ${dialogId.value} from ${sender.value}.") with Error

  final case class PostNotFound(postId: PostId)
    extends Throwable(s"Post ${postId.value} not exists.") with Error

  final case class CanNotPublishPost(postId: PostId, storage: String)
    extends Throwable(s"Can not publish post ${postId.value} in $storage.") with Error

  final case class CanNotUpdatePost(postId: PostId, storage: String)
    extends Throwable(s"Can not update post ${postId.value} in $storage.") with Error

  final case class CanNotDeletePost(postId: PostId, storage: String)
    extends Throwable(s"Can not delete post ${postId.value} in $storage.") with Error

  //final case class CanNotGetPostFeed(user: UserId)
    //extends Throwable(s"Can not get post feed for user ${user.value}.") with Error

  final case class CanNotInvalidatePostFeed(user: UserId)
    extends Throwable(s"Can not invalidate post feed for user ${user.value}.") with Error

}
