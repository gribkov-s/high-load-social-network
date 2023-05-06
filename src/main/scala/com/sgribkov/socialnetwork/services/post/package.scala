package com.sgribkov.socialnetwork.services

import com.sgribkov.socialnetwork.data.entities.{UserId, UserIdentity}
import com.sgribkov.socialnetwork.data.entities.post.{PostId, PostMessage, PostMsgBody}
import com.sgribkov.socialnetwork.repository.userpostfeed.UserPostFeedRepo
import com.sgribkov.socialnetwork.repository.userfriendship.UserFriendshipRepo
import com.sgribkov.socialnetwork.repository.userpost.UserPostRepo
import com.sgribkov.socialnetwork.repository.userpostsending.UserPostSendingRepo
import com.sgribkov.socialnetwork.system.config.{Config, PostServiceConfig}
import zio.interop.catz.taskConcurrentInstance
import zio.{Has, RIO, Task, ZLayer}

package object post {

  type PostService = Has[PostService.Service]
  type PostMessageStream = fs2.Stream[Task, PostMessage]

  object PostService {

    trait Service {
      def getUserPosts(user: UserId): RIO[UserPostRepo, PostMessageStream]
      def getUserFeedInval(user: UserId): RIO[UserPostFeedRepo with UserPostRepo, List[String]]
      def refreshUserFeed(user: UserId): RIO[UserPostFeedRepo with UserPostRepo, List[String]]
      def createPost(publisher: UserIdentity, content: PostMsgBody): RIO[UserPostRepo with
                                                                     UserPostSendingRepo with
                                                                     UserPostFeedRepo with
                                                                     UserFriendshipRepo,
                                                                     Unit]
      def updatePost(user: UserId, postId: PostId, content: PostMsgBody): RIO[UserPostRepo with
                                                                              UserPostSendingRepo with
                                                                              UserPostFeedRepo,
                                                                              Unit]
      def deletePost(user: UserId, postId: PostId): RIO[UserPostRepo with
                                                        UserPostSendingRepo with
                                                        UserPostFeedRepo,
                                                        Unit]
    }


    val live: ZLayer[Any with Has[PostServiceConfig], Throwable, PostService] =
      ZLayer.fromEffect(
        for {
          cnf <- Config.postService
        } yield new PostServiceLive(cnf)
      )

    def getUserPosts(user: UserId): RIO[PostService with UserPostRepo, PostMessageStream] =
      RIO.accessM(_.get.getUserPosts(user))

    def getUserFeedInval(user: UserId): RIO[PostService with UserPostFeedRepo with UserPostRepo, List[String]] =
      RIO.accessM(_.get.getUserFeedInval(user))

    def refreshUserFeed(user: UserId): RIO[PostService with UserPostFeedRepo with UserPostRepo, List[String]] =
      RIO.accessM(_.get.refreshUserFeed(user))

    def createPost(publisher: UserIdentity,
                   content: PostMsgBody
                  ): RIO[PostService with
                         UserPostRepo with
                         UserPostSendingRepo with
                         UserPostFeedRepo with
                         UserFriendshipRepo,
                         Unit] =
      RIO.accessM(_.get.createPost(publisher, content))

    def updatePost(user: UserId, postId: PostId, content: PostMsgBody): RIO[PostService with
                                                              UserPostRepo with
                                                              UserPostSendingRepo with
                                                              UserPostFeedRepo,
                                                              Unit] =
      RIO.accessM(_.get.updatePost(user, postId, content))

    def deletePost(user: UserId, postId: PostId): RIO[PostService with
                                        UserPostRepo with
                                        UserPostSendingRepo with
                                        UserPostFeedRepo,
                                        Unit] =
      RIO.accessM(_.get.deletePost(user, postId))

  }

}
