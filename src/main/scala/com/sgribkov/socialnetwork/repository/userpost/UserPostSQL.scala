package com.sgribkov.socialnetwork.repository.userpost

import com.sgribkov.socialnetwork.data.Error.DatabaseError
import com.sgribkov.socialnetwork.data.entities.UserId
import com.sgribkov.socialnetwork.data.entities.post.{PostId, PostMessage, PostMsgBody}
import com.sgribkov.socialnetwork.repository.userpost.UserPostSQL.Queries
import com.sgribkov.socialnetwork.system.dbtransactor.DBTransactor
import doobie.implicits._
import doobie.util.transactor.Transactor
import zio.{Task, ZLayer}
import doobie.{Query0, Update0}
import doobie.implicits.javasql.TimestampMeta
import zio.interop.catz._


class UserPostSQL(trx: Transactor[Task]) extends UserPostRepo.Service {

  override def getPosts(user: UserId): fs2.Stream[Task, PostMessage] =
    Queries
      .getPosts(user)
      .stream
      .transact(trx)

  override def getPost(user: UserId, postId: PostId): Task[Option[PostMessage]] =
    Queries
      .getPost(user, postId)
      .option
      .transact(trx)
      .mapError(err => DatabaseError(err.getMessage))

  override def getPostsForFeed(subscriber: UserId, feedLen: Int): Task[List[PostMessage]] =
    Queries
      .getPostsForFeed(subscriber, feedLen)
      .to[List]
      .transact(trx)
      .mapError(err => DatabaseError(err.getMessage))

  override def insert(post: PostMessage): Task[Boolean] =
    Queries
      .insert(post)
      .run
      .transact(trx)
      .mapBoth(
        err => DatabaseError(err.getMessage),
        rows => if (rows == 0) false else true
      )

  override def update(user: UserId, postId: PostId, content: PostMsgBody): Task[Boolean] =
    Queries
      .update(user, postId, content)
      .run
      .transact(trx)
      .mapBoth(
        err => DatabaseError(err.getMessage),
        rows => if (rows == 0) false else true
      )

  override def delete(user: UserId, postId: PostId): Task[Boolean] =
    Queries
      .delete(user, postId)
      .run
      .transact(trx)
      .mapBoth(
        err => DatabaseError(err.getMessage),
        rows => if (rows == 0) false else true
      )
}

object UserPostSQL {

  object Queries {

    def getPosts(user: UserId): Query0[PostMessage] =
      sql"""
           SELECT post_id,
                  post_time,
                  publisher_id,
                  publisher_login,
                  body
           FROM   user_post
           WHERE  publisher_id = ${user}
           ORDER BY post_time ASC
           """.query[PostMessage]

    def getPost(user: UserId, postId: PostId): Query0[PostMessage] =
      sql"""
           SELECT post_id,
                  post_time,
                  publisher_id,
                  publisher_login,
                  body
           FROM   user_post
           WHERE  post_id = ${postId} AND
                  publisher_id = $user
           """.query[PostMessage]

    def getPostsForFeed(subscriber: UserId, feedLen: Int): Query0[PostMessage] =
      sql"""
           WITH last_posts AS (
              SELECT post_id
              FROM   user_post_sending
              WHERE  subscriber_id = ${subscriber}
              ORDER BY post_time DESC
              LIMIT $feedLen
           )

           SELECT post_id,
                  post_time,
                  publisher_id,
                  publisher_login,
                  body
           FROM   user_post
           WHERE  post_id IN
           (
              SELECT  post_id
              FROM    last_posts
           )
           """.query[PostMessage]

    def insert(post: PostMessage): Update0 =
      sql"""
           INSERT INTO user_post
           VALUES
           (
              ${post.postId},
              ${post.postTime},
              ${post.publisherId},
              ${post.publisherLogin},
              ${post.content}
           )
         """.update

    def update(user: UserId, postId: PostId, content: PostMsgBody): Update0 =
      sql"""
           UPDATE user_post
           SET body = $content
           WHERE  post_id = $postId AND
                  publisher_id = $user
         """.update

    def delete(user: UserId, postId: PostId): Update0 =
      sql"""
           DELETE FROM user_post
           WHERE  post_id = $postId AND
                  publisher_id = $user
           """.update
  }

  val live: ZLayer[DBTransactor, Throwable, UserPostRepo] =
    ZLayer.fromEffect(
      DBTransactor.transactor.map(new UserPostSQL(_))
    )
}
