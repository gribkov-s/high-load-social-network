package com.sgribkov

import com.sgribkov.socialnetwork.repository.userauth.{UserAuthRepo, UserAuthSQL}
import com.sgribkov.socialnetwork.repository.userfriendship.{UserFriendshipRepo, UserFriendshipSQL}
import com.sgribkov.socialnetwork.repository.userprofile.{UserProfileRepo, UserProfileSQL}
import com.sgribkov.socialnetwork.repository.userdialog.{UserDialogRepo, UserDialogSQL}
import com.sgribkov.socialnetwork.services.auth.UserAuthService
import com.sgribkov.socialnetwork.services.dialog.builder.DialogFlowBuilder
import com.sgribkov.socialnetwork.services.dialog.service.DialogService
import com.sgribkov.socialnetwork.services.user.UserService
import com.sgribkov.socialnetwork.system.config.Config
import com.sgribkov.socialnetwork.system.dbtransactor.DBTransactor
import com.sgribkov.socialnetwork.system.logging.Logger
import zio.{Has, ULayer, ZEnv, ZLayer}
import zio.blocking.Blocking
import zio.logging.Logging
import zio.random.Random

package object socialnetwork {

  type AppEnvironment =
    Logging with Config with
      UserProfileRepo with UserAuthRepo with UserFriendshipRepo with UserDialogRepo with
        UserService with UserAuthService with DialogService with DialogFlowBuilder with
          Random with Blocking

  val logger: ULayer[Logging] = Logger.live
  val config: ZLayer[Any, Throwable, Config] = logger >>> Config.live
  val transactor: ZLayer[Any, Throwable, DBTransactor] = logger ++ Blocking.live ++ config >>> DBTransactor.live

  val userProfileRepo: ZLayer[Any, Throwable, UserProfileRepo] = transactor >>> UserProfileSQL.live
  val userAuthRepo: ZLayer[Any, Throwable, UserAuthRepo] = transactor >>> UserAuthSQL.live
  val userFriendshipRepo: ZLayer[Any, Throwable, UserFriendshipRepo] = transactor >>> UserFriendshipSQL.live
  val userDialogRepo: ZLayer[Any, Throwable, UserDialogRepo] = transactor >>> UserDialogSQL.live

  val userService: ULayer[UserService] = UserService.live
  val authService: ULayer[UserAuthService] = UserAuthService.live
  val dialogService: ZLayer[Any, Throwable, DialogService] = userDialogRepo >>> DialogService.live
  val dialogFlowBuilder: ZLayer[Any, Throwable, DialogFlowBuilder] = dialogService >>> DialogFlowBuilder.live

  val appLayers =
    logger >+>
      (
        config ++
        userProfileRepo ++ userAuthRepo ++ userFriendshipRepo ++ userDialogRepo ++
        userService ++ dialogService ++ dialogFlowBuilder ++
        authService
      )

}
