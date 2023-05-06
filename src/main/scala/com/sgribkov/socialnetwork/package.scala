package com.sgribkov

import com.sgribkov.socialnetwork.repository.userpostfeed.{UserPostFeedCache, UserPostFeedRepo}
import com.sgribkov.socialnetwork.repository.userauth.{UserAuthRepo, UserAuthSQL}
import com.sgribkov.socialnetwork.repository.userfriendship.{UserFriendshipRepo, UserFriendshipSQL}
import com.sgribkov.socialnetwork.repository.userprofile.{UserProfileRepo, UserProfileSQL}
import com.sgribkov.socialnetwork.repository.userdialog.{UserDialogRepo, UserDialogSQL}
import com.sgribkov.socialnetwork.repository.userpost.{UserPostRepo, UserPostSQL}
import com.sgribkov.socialnetwork.repository.userpostsending.{UserPostSendingRepo, UserPostSendingSQL}
import com.sgribkov.socialnetwork.services.auth.UserAuthService
import com.sgribkov.socialnetwork.services.dialog.builder.DialogFlowBuilder
import com.sgribkov.socialnetwork.services.dialog.service.DialogService
import com.sgribkov.socialnetwork.services.friendship.FriendshipService
import com.sgribkov.socialnetwork.services.post.PostService
import com.sgribkov.socialnetwork.services.user.UserService
import com.sgribkov.socialnetwork.system.config.Config
import com.sgribkov.socialnetwork.system.dbtransactor.DBTransactor
import com.sgribkov.socialnetwork.system.logging.Logger
import zio.{Has, ULayer, ZLayer}
import zio.blocking.Blocking
import zio.logging.Logging
import zio.random.Random

package object socialnetwork {

  type AppEnvironment =
    Logging with Config with
      UserProfileRepo with UserAuthRepo with UserFriendshipRepo with UserDialogRepo with UserPostRepo with UserPostSendingRepo with UserPostFeedRepo with
        UserService with UserAuthService with FriendshipService with DialogService with DialogFlowBuilder with PostService with
          Random with Blocking

  val logger: ULayer[Logging] = Logger.live
  val config: ZLayer[Any, Throwable, Config] = logger >>> Config.live
  val transactor: ZLayer[Any, Throwable, DBTransactor] = logger ++ Blocking.live ++ config >>> DBTransactor.live

  val userProfileRepo: ZLayer[Any, Throwable, UserProfileRepo] = transactor >>> UserProfileSQL.live
  val userAuthRepo: ZLayer[Any, Throwable, UserAuthRepo] = transactor >>> UserAuthSQL.live
  val userFriendshipRepo: ZLayer[Any, Throwable, UserFriendshipRepo] = transactor >>> UserFriendshipSQL.live
  val userDialogRepo: ZLayer[Any, Throwable, UserDialogRepo] = transactor >>> UserDialogSQL.live
  val userPostRepo: ZLayer[Any, Throwable, UserPostRepo] = transactor >>> UserPostSQL.live
  val userPostSendingRepo: ZLayer[Any, Throwable, UserPostSendingRepo] = transactor >>> UserPostSendingSQL.live
  val userPostFeedRepo: ZLayer[Any, Throwable, UserPostFeedRepo] = config >>> UserPostFeedRepo.live

  val userService: ULayer[UserService] = UserService.live
  val authService: ULayer[UserAuthService] = UserAuthService.live
  val friendshipService: ULayer[FriendshipService] = FriendshipService.live
  val dialogService: ZLayer[Any, Throwable, DialogService] = userDialogRepo >>> DialogService.live
  val dialogFlowBuilder: ZLayer[Any, Throwable, DialogFlowBuilder] = dialogService >>> DialogFlowBuilder.live
  val postService: ZLayer[Any, Throwable, PostService] = config >>> PostService.live

  val appLayers =
    logger >+>
      (
        config ++
        userProfileRepo ++
          userAuthRepo ++
            userFriendshipRepo ++
              userDialogRepo ++
                userPostRepo ++
                  userPostSendingRepo ++
                    userPostFeedRepo ++
        userService ++
          friendshipService ++
            dialogService ++
              dialogFlowBuilder ++
                postService ++
        authService
      )

}
