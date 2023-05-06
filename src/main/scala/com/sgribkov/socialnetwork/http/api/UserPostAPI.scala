package com.sgribkov.socialnetwork.http.api

import com.sgribkov.socialnetwork.repository.userfriendship.UserFriendshipRepo
import com.sgribkov.socialnetwork.repository.userpost.UserPostRepo
import com.sgribkov.socialnetwork.repository.userpostsending.UserPostSendingRepo
import com.sgribkov.socialnetwork.services.post.PostService
import zio.RIO
import io.circe.{Decoder, Encoder}
import org.http4s.{AuthedRoutes, EntityDecoder, EntityEncoder}
import org.http4s.circe._
import zio.interop.catz.taskConcurrentInstance
import org.http4s.dsl.Http4sDsl
import cats.implicits.toSemigroupKOps
import com.sgribkov.socialnetwork.data.dto.PostMessageDTO
import com.sgribkov.socialnetwork.data.entities.UserIdentity
import com.sgribkov.socialnetwork.data.entities.post._
import com.sgribkov.socialnetwork.repository.userpostfeed.UserPostFeedRepo


class UserPostAPI[R <: PostService with
                       UserPostRepo with
                       UserPostSendingRepo with
                       UserPostFeedRepo with
                       UserFriendshipRepo] {

  type PostTask[A] =  RIO[R, A]
  type PostMessageStream = fs2.Stream[PostTask, PostMessage]

  val dsl = Http4sDsl[PostTask]
  import dsl._

  implicit def jsonDecoder[A](implicit decoder: Decoder[A]): EntityDecoder[PostTask, A] =
    jsonOf[PostTask, A]

  implicit def jsonEncoder[A](implicit decoder: Encoder[A]): EntityEncoder[PostTask, A] =
    jsonEncoderOf[PostTask, A]

  val routes: AuthedRoutes[UserIdentity, PostTask] = AuthedRoutes.of[UserIdentity, PostTask] {

    case GET -> Root / "posts" / "published" as userIdentity =>
      val pipeline: PostTask[PostMessageStream] =
        PostService.getUserPosts(userIdentity.userId)
      for {
        stream <- pipeline
        dto <- Ok(stream.map(PostMessageDTO.from))
      } yield dto

    case GET -> Root / "posts" / "feed" as userIdentity =>
      PostService.getUserFeedInval(userIdentity.userId).foldM(
        _ => NotFound(s"Not found feed for user ${userIdentity.userLogin.value}."),
        feed => Ok(feed)
      )

    case GET -> Root / "posts" / "feed" / "refresh" as userIdentity =>
      PostService.refreshUserFeed(userIdentity.userId).foldM(
        _ => NotFound(s"Can not refresh feed for user ${userIdentity.userLogin.value}."),
        feed => Ok(feed)
      )

    case reqPost @ POST -> Root / "posts" / "publish" as userIdentity =>
      reqPost.req.decode[PostMsgBody] { text =>
        PostService.createPost(userIdentity, text).foldM(
          err => BadRequest(err.getMessage),
          _ => Ok(s"Post was published successfully.")
        )
      }

    case reqPost @ PUT -> Root / "posts" / postId as userIdentity =>
      val pId = PostId(postId)
      reqPost.req.decode[PostMsgBody] { text =>
        PostService.updatePost(userIdentity.userId, pId, text).foldM(
          err => BadRequest(err.getMessage),
          _ => Ok(s"Post was updated successfully.")
        )
      }

    case DELETE -> Root / "posts" / postId as userIdentity =>
      val pId = PostId(postId)
      PostService.deletePost(userIdentity.userId, pId).foldM(
        err => BadRequest(err.getMessage),
        _ => Ok(s"Post was deleted successfully.")
      )
  }
}
