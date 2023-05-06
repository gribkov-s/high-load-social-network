package com.sgribkov.socialnetwork.repository.userpostsending

import com.sgribkov.socialnetwork.data.Error.DatabaseError
import com.sgribkov.socialnetwork.data.entities.UserId
import com.sgribkov.socialnetwork.data.entities.post.{PostId, PostSending}
import com.sgribkov.socialnetwork.repository.userpostsending.UserPostSendingSQL.Queries
import com.sgribkov.socialnetwork.system.dbtransactor.DBTransactor
import doobie.implicits._
import doobie.util.transactor.Transactor
import zio.{Task, ZLayer}
import doobie.{ConnectionIO, Query0, Update, Update0}
import doobie.implicits.javasql.TimestampMeta
import zio.interop.catz._


class UserPostSendingSQL(trx: Transactor[Task]) extends UserPostSendingRepo.Service {

  override def insert(sending: List[PostSending]): Task[Boolean] =
    Queries
      .insert(sending)
      .transact(trx)
      .mapBoth(
        err => DatabaseError(err.getMessage),
        rows => if (rows < sending.length) false else true
      )

  override def getSubscribers(postId: PostId): Task[List[UserId]] =
    Queries
      .getSubscribers(postId)
      .to[List]
      .transact(trx)
      .mapError(err => DatabaseError(err.getMessage))
}

object UserPostSendingSQL {

  object Queries {

    def insert(sending: List[PostSending]): ConnectionIO[Int] = {
      val sql =
      """
         INSERT INTO user_post_sending
         VALUES (?, ?, ?, ?)
      """
      Update[PostSending](sql).updateMany(sending)
    }

    def getSubscribers(postId: PostId): Query0[UserId] =
      sql"""
         SELECT subscriber_id
         FROM   user_post_sending
         WHERE  post_id = ${postId}
         """.query[UserId]
  }

  val live: ZLayer[DBTransactor, Throwable, UserPostSendingRepo] =
    ZLayer.fromEffect(
      DBTransactor.transactor.map(new UserPostSendingSQL(_))
    )
}
