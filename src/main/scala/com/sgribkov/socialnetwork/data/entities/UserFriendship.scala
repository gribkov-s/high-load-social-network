package com.sgribkov.socialnetwork.data.entities

final case class UserFriendship(userId: UserId,
                                userLogin: UserLogin,
                                friendId: UserId,
                                friendLogin: UserLogin
                               )
