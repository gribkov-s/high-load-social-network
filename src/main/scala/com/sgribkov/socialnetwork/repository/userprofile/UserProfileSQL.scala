package com.sgribkov.socialnetwork.repository.userprofile

import com.sgribkov.socialnetwork.data.Error._
import com.sgribkov.socialnetwork.data.entities.{User, UserId, UserLogin}
import com.sgribkov.socialnetwork.repository.userprofile.UserProfileSQL.Queries
import com.sgribkov.socialnetwork.system.dbtransactor.DBTransactor
import doobie.implicits._
import doobie.util.transactor.Transactor
import doobie.{Query0, Update0}
import zio._
import zio.interop.catz._


final class UserProfileSQL(trx: Transactor[Task]) extends UserProfileRepo.Service {

  override def getUserId(login: UserLogin): Task[Option[String]] =
    Queries
      .getUserId(login)
      .option
      .transact(trx)
      .mapError(err => DatabaseError(err.getMessage))

  override def findByLogin(login: UserLogin): Task[Option[User]] =
    Queries
      .findByLogin(login)
      .option
      .transact(trx)
      .mapError(err => DatabaseError(err.getMessage))

  override def insert(user: User): Task[Boolean] =
    Queries
      .insert(user)
      .run
      .transact(trx)
      .mapBoth(
        err => DatabaseError(err.getMessage),
        rows => if (rows == 0) false else true
      )

  override def update(user: User): Task[Boolean] =
    Queries
      .update(user)
      .run
      .transact(trx)
      .mapBoth(
        err => DatabaseError(err.getMessage),
        rows => if (rows == 0) false else true
      )

  override def delete(id: UserId): Task[Boolean] =
    Queries
      .delete(id)
      .run
      .transact(trx)
      .mapBoth(
        err => DatabaseError(err.getMessage),
        rows => if (rows == 0) false else true
      )
}

object UserProfileSQL {

  object Queries {

    def getUserId(login: UserLogin): Query0[String] =
      sql"""
           SELECT user_id
           FROM user_profile
           WHERE login = ${login.value}
           """.query[String]

    def findByLogin(userLogin: UserLogin): Query0[User] = {
      sql"""
           SELECT user_id,
                  login,
                  first_name,
                  last_name,
                  age,
                  gender,
                  city,
                  interests
           FROM user_profile
           WHERE login = ${userLogin.value}
           """.query[User]
    }

    def insert(user: User): Update0 =
      sql"""
           INSERT IGNORE INTO user_profile
           VALUES (
              ${user.id.value},
              ${user.login.value},
              ${user.firstName},
              ${user.lastName},
              ${user.age},
              ${user.gender},
              ${user.city},
              ${user.interests.toJsonStr}
           )
         """.update

    def update(user: User): Update0 =
      sql"""
           UPDATE user_profile
           SET first_name = ${user.firstName},
               last_name = ${user.lastName},
               age = ${user.age},
               gender = ${user.gender},
               city = ${user.city},
               interests = ${user.interests.toJsonStr}
           WHERE user_id = ${user.id.value}
         """.update

    def delete(id: UserId): Update0 =
      sql"""
           DELETE FROM user_profile
           WHERE user_id = ${id.value}
           """.update
  }

  val live: ZLayer[DBTransactor, Throwable, UserProfileRepo] =
    ZLayer.fromEffect(
      DBTransactor.transactor.map(new UserProfileSQL(_))
    )
}
