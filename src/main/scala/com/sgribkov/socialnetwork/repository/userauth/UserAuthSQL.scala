package com.sgribkov.socialnetwork.repository.userauth

import com.sgribkov.socialnetwork.data.Error._
import com.sgribkov.socialnetwork.data.entities.{UserAuth, UserId, UserLogin, UserPassword}
import com.sgribkov.socialnetwork.repository.userauth.UserAuthSQL.Queries
import com.sgribkov.socialnetwork.system.dbtransactor.DBTransactor
import doobie.implicits._
import doobie.util.transactor.Transactor
import doobie.{Query0, Update0}
import zio._
import zio.interop.catz._


class UserAuthSQL(trx: Transactor[Task]) extends UserAuthRepo.Service {

  override def checkUserAuth(login: UserLogin, password: UserPassword): Task[Option[UserId]] = {
    Queries
      .checkUserAuth(login, password)
      .option
      .transact(trx)
      .mapError(err => DatabaseError(err.getMessage))
  }

  override def insert(userAuth: UserAuth): Task[Boolean] =
    Queries
      .insert(userAuth)
      .run
      .transact(trx)
      .mapBoth(
        err => DatabaseError(err.getMessage),
        rows => if (rows == 0) false else true
      )

  override def update(id: UserId, password: UserPassword): Task[Boolean] =
    Queries
      .update(id, password)
      .run
      .transact(trx)
      .mapBoth(
        err => DatabaseError(err.getMessage),
        rows => if (rows == 0) false else true
      )

  override def deactivate(id: UserId): Task[Boolean] =
    Queries
      .deactivate(id)
      .run
      .transact(trx)
      .mapBoth(
        err => DatabaseError(err.getMessage),
        rows => if (rows == 0) false else true
      )
}

object UserAuthSQL {

  object Queries {

    def checkUserAuth(login: UserLogin, password: UserPassword): Query0[UserId] =
      sql"""
           SELECT user_id
           FROM user_auth
           WHERE login = ${login.value} AND password = ${password.encrypt}
           """.query[UserId]

    def insert(userAuth: UserAuth): Update0 =
      sql"""
           INSERT IGNORE INTO user_auth
           VALUES (
              ${userAuth.id.value},
              ${userAuth.login.value},
              ${userAuth.password.encrypt}
           )
         """.update

    def update(id: UserId, password: UserPassword): Update0 =
      sql"""
           UPDATE user_auth
           SET password = ${password.encrypt}
           WHERE user_id = ${id.value} AND password <> ${password.encrypt}
         """.update

    def deactivate(id: UserId): Update0 =
      sql"""
           UPDATE user_auth
           SET password = ''
           WHERE user_id = ${id.value}
           """.update
  }

  val live: ZLayer[DBTransactor, Throwable, UserAuthRepo] =
    ZLayer.fromEffect(
      DBTransactor.transactor.map(new UserAuthSQL(_))
    )
}
