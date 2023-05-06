package com.sgribkov.socialnetwork.repository

import com.sgribkov.socialnetwork.data.entities.UserId
import com.sgribkov.socialnetwork.data.entities.post._
import zio.{Has, RIO, Task}

package object userpost {

  type UserPostRepo = Has[UserPostRepo.Service]

  object UserPostRepo {

    trait Service {
      def getPosts(user: UserId): fs2.Stream[Task, PostMessage]
      def getPost(user: UserId, postId: PostId): Task[Option[PostMessage]]
      def getPostsForFeed(subscriber: UserId, feedLen: Int): Task[List[PostMessage]]
      def insert(post: PostMessage): Task[Boolean]
      def update(user: UserId, postId: PostId, content: PostMsgBody): Task[Boolean]
      def delete(user: UserId, postId: PostId): Task[Boolean]
    }

    def getPosts(user: UserId): RIO[UserPostRepo, fs2.Stream[Task, PostMessage]] =
      RIO.access(_.get.getPosts(user))

    def getPost(user: UserId, postId: PostId): RIO[UserPostRepo, Option[PostMessage]] =
      RIO.accessM(_.get.getPost(user, postId))

    def getPostsForFeed(subscriber: UserId, feedLen: Int): RIO[UserPostRepo, List[PostMessage]] =
      RIO.accessM(_.get.getPostsForFeed(subscriber, feedLen))

    def insert(post: PostMessage): RIO[UserPostRepo,  Boolean] =
      RIO.accessM(_.get.insert(post))

    def update(user: UserId, postId: PostId, content: PostMsgBody): RIO[UserPostRepo,  Boolean] =
      RIO.accessM(_.get.update(user, postId, content))

    def delete(user: UserId, postId: PostId): RIO[UserPostRepo,  Boolean] =
      RIO.accessM(_.get.delete(user, postId))
  }
}
