package com.sgribkov.socialnetwork.repository.userfriendship

import com.sgribkov.socialnetwork.data.Error._
import com.sgribkov.socialnetwork.data.entities.{UserFriendship, UserId, UserLogin}
import com.sgribkov.socialnetwork.repository.userfriendship.UserFriendshipSQL.Queries
import com.sgribkov.socialnetwork.system.dbtransactor.DBTransactor
import doobie.implicits._
import doobie.util.transactor.Transactor
import doobie.{Query0, Update0}
import zio._
import zio.interop.catz._


class UserFriendshipSQL(trx: Transactor[Task]) extends UserFriendshipRepo.Service {

  override def getFriendsByLogin(login: UserLogin): Task[List[UserLogin]] = {
    println(login)
    Queries
      .getFriendsByLogin(login)
      .to[List]
      .transact(trx)
      .mapError(err => DatabaseError(err.getMessage))
  }

  override def insert(friendship: UserFriendship): Task[Boolean] =
    Queries
      .insert(friendship)
      .run
      .transact(trx)
      .mapBoth(
        err => DatabaseError(err.getMessage),
        rows => if (rows == 0) false else true
      )

  override def unfollow(id: UserId, friendId: UserId): Task[Boolean] =
    Queries
      .unfollow(id, friendId)
      .run
      .transact(trx)
      .mapBoth(
        err => DatabaseError(err.getMessage),
        rows => if (rows == 0) false else true
      )
}

object UserFriendshipSQL {

  object Queries {

    def getFriendsByLogin(login: UserLogin): Query0[UserLogin] =
      sql"""
             SELECT friend_login
             FROM user_friendship
             WHERE login = ${login.value}
           UNION
             SELECT login
             FROM user_friendship
             WHERE friend_login = ${login.value}
           """.query[UserLogin]

    def insert(friendship: UserFriendship): Update0 =
      sql"""
           INSERT IGNORE INTO user_friendship
           VALUES
           (
              ${friendship.userId.value},
              ${friendship.userLogin.value},
              ${friendship.friendId.value},
              ${friendship.friendLogin.value}
           )
         """.update

    def unfollow(id: UserId, friendId: UserId): Update0 =
      sql"""
           DELETE FROM user_friendship
           WHERE
            (user_id = ${id.value} AND friend_id = ${friendId.value})
           OR
            (user_id = ${friendId.value} AND friend_id = ${id.value})
           """.update
  }

  val live: ZLayer[DBTransactor, Throwable, UserFriendshipRepo] =
    ZLayer.fromEffect(
      DBTransactor.transactor.map(new UserFriendshipSQL(_))
    )
}


