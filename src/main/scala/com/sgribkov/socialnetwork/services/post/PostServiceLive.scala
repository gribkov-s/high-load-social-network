package com.sgribkov.socialnetwork.services.post

import com.sgribkov.socialnetwork.data.Error._
import com.sgribkov.socialnetwork.data.entities.{UserId, UserIdentity}
import com.sgribkov.socialnetwork.data.entities.post.{PostId, PostMessage, PostMsgBody}
import com.sgribkov.socialnetwork.repository.userfriendship.UserFriendshipRepo
import com.sgribkov.socialnetwork.repository.userpost.UserPostRepo
import com.sgribkov.socialnetwork.repository.userpostfeed.UserPostFeedRepo
import com.sgribkov.socialnetwork.repository.userpostsending.UserPostSendingRepo
import com.sgribkov.socialnetwork.system.config.PostServiceConfig
import zio.{RIO, Task, UIO, ZIO}


class PostServiceLive(config: PostServiceConfig) extends PostService.Service {

  private def checkBooleanResult[R](rio: RIO[R, Boolean], err: Throwable): ZIO[R, Throwable, Unit] =
    rio.flatMap {
      case true => UIO.unit
      case false => Task.fail(err)
    }

  override def getUserPosts(user: UserId): RIO[UserPostRepo, PostMessageStream] =
    UserPostRepo.getPosts(user)

  override def getUserFeedInval(user: UserId): RIO[UserPostFeedRepo with UserPostRepo, List[String]] = {

    val invalidateAndGet =
      for {
        posts <- UserPostRepo.getPostsForFeed(user, config.postFeedLength)
        _ <- ZIO.foreachPar(posts)(post =>
          checkBooleanResult(
            UserPostFeedRepo.insert(user, post),
            CanNotInvalidatePostFeed(user)
          )
        )
        feedInval <- UserPostFeedRepo.getPostFeed(user, config.postFeedLength)
      } yield feedInval

    val get =
      UserPostFeedRepo.getPostFeed(user, config.postFeedLength)

    UserPostFeedRepo.checkSubscriber(user).flatMap {
      case true => get
      case false => invalidateAndGet
    }
  }

  def refreshUserFeed(user: UserId): RIO[UserPostFeedRepo with UserPostRepo, List[String]] = {
    for {
      _ <- checkBooleanResult(
        UserPostFeedRepo.clearFeed(user),
        CanNotInvalidatePostFeed(user)
      )
      posts <- UserPostRepo.getPostsForFeed(user, config.postFeedLength)
      _ <- ZIO.foreachPar(posts)(post =>
        checkBooleanResult(
          UserPostFeedRepo.insert(user, post),
          CanNotInvalidatePostFeed(user)
        )
      )
      feedInval <- UserPostFeedRepo.getPostFeed(user, config.postFeedLength)
    } yield feedInval
  }

  override def createPost(publisher: UserIdentity,
                          content: PostMsgBody
                         ): RIO[UserPostRepo with
                                UserPostSendingRepo with
                                UserPostFeedRepo with
                                UserFriendshipRepo,
                                Unit] =
    for {
      post <- ZIO.succeed(PostMessage.from(publisher, content))
      sending <- UserFriendshipRepo.getFriendsById(publisher.userId).mapEffect(_.map(post.toSending))
      _ <- checkBooleanResult(
        UserPostRepo.insert(post),
        CanNotPublishPost(post.postId, "user_post")
      )
      _ <- checkBooleanResult(
        UserPostSendingRepo.insert(sending),
        CanNotPublishPost(post.postId, "user_post_sending")
      )
      subs = sending.map(_.subscriberId)
      _ <- ZIO.foreachPar(subs)(sub =>
        checkBooleanResult(
          UserPostFeedRepo.insert(sub, post, config.postFeedLength),
          CanNotPublishPost(post.postId, s"feed cache for key ${sub.value}")
        )
      )
    } yield ()

  override def updatePost(user: UserId, postId: PostId, content: PostMsgBody): RIO[UserPostRepo with
                                                                                   UserPostSendingRepo with
                                                                                   UserPostFeedRepo,
                                                                                   Unit] =
    for {
      postOld <- UserPostRepo.getPost(user, postId)
        .flatMap(u => Task.require(PostNotFound(postId))(Task.succeed(u)))
      postNew = postOld.copy(content = content)
      subs <- UserPostSendingRepo.getSubscribers(postId)
      _ <- checkBooleanResult(
        UserPostRepo.update(user, postId, content),
        CanNotUpdatePost(postNew.postId, "user_post")
      )
      _ <- ZIO.foreachPar(subs)( sub =>
        UserPostFeedRepo.delete(sub, postOld) *>  UserPostFeedRepo.insert(sub, postNew)
      )
    } yield ()

  override def deletePost(user: UserId, postId: PostId): RIO[UserPostRepo with
                                                             UserPostSendingRepo with
                                                             UserPostFeedRepo,
                                                             Unit] =
    for {
      postOld <- UserPostRepo.getPost(user, postId)
        .flatMap(u => Task.require(PostNotFound(postId))(Task.succeed(u)))
      subs <- UserPostSendingRepo.getSubscribers(postId)
      _ <- checkBooleanResult(
        UserPostRepo.delete(user, postId),
        CanNotDeletePost(postOld.postId, "user_post")
      )
      _ <- ZIO.foreachPar(subs)( sub => UserPostFeedRepo.delete(sub, postOld))
    } yield ()

}
