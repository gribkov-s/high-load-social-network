package com.sgribkov.socialnetwork.repository

import com.sgribkov.socialnetwork.data.entities.UserId
import com.sgribkov.socialnetwork.data.entities.post.PostMessage
import zio.{Has, RIO, Task, ZLayer}
import cats.implicits._
import zio.interop.catz._
import com.sgribkov.socialnetwork.system.config.{CacheConfig, Config}


package object userpostfeed {

  type UserPostFeedRepo = Has[UserPostFeedRepo.Service]

  object UserPostFeedRepo {

    trait Service {
      def insert(subscriber: UserId, post: PostMessage, reduceFeedTo: Int): Task[Boolean]
      def delete(subscriber: UserId, post: PostMessage): Task[Boolean]
      def getPostFeed(subscriber: UserId, postFeedLength: Int): Task[List[String]]
      def checkSubscriber(subscriber: UserId): Task[Boolean]
      def clearFeed(subscriber: UserId): Task[Boolean]
    }

    val live: ZLayer[Any with Has[CacheConfig], Throwable, UserPostFeedRepo] =
      ZLayer.fromEffect(
        for {
          cnf <- Config.cacheConfig
        } yield new UserPostFeedCache(cnf)
      )

    def insert(subscriber: UserId, post: PostMessage, reduceFeedTo: Int = 0): RIO[UserPostFeedRepo, Boolean] =
      RIO.accessM(_.get.insert(subscriber, post, reduceFeedTo))

    def delete(subscriber: UserId, post: PostMessage): RIO[UserPostFeedRepo, Boolean] =
      RIO.accessM(_.get.delete(subscriber, post))

    def getPostFeed(subscriber: UserId, postFeedLength: Int): RIO[UserPostFeedRepo, List[String]] =
      RIO.accessM(_.get.getPostFeed(subscriber, postFeedLength))

    def checkSubscriber(subscriber: UserId): RIO[UserPostFeedRepo, Boolean] =
      RIO.accessM(_.get.checkSubscriber(subscriber))

    def clearFeed(subscriber: UserId): RIO[UserPostFeedRepo, Boolean] =
      RIO.accessM(_.get.clearFeed(subscriber))
  }
}
