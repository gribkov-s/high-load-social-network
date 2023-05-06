package com.sgribkov.socialnetwork.repository

import com.sgribkov.socialnetwork.data.entities.UserId
import com.sgribkov.socialnetwork.data.entities.post._
import zio.{Has, RIO, Task}

package object userpostsending {

  type UserPostSendingRepo = Has[UserPostSendingRepo.Service]

  object UserPostSendingRepo {

    trait Service {
      def insert(sending: List[PostSending]): Task[Boolean]
      def getSubscribers(postId: PostId): Task[List[UserId]]
    }

    def insert(sending: List[PostSending]): RIO[UserPostSendingRepo,  Boolean] =
      RIO.accessM(_.get.insert(sending))

    def getSubscribers(postId: PostId): RIO[UserPostSendingRepo, List[UserId]] =
      RIO.accessM(_.get.getSubscribers(postId))
  }
}
