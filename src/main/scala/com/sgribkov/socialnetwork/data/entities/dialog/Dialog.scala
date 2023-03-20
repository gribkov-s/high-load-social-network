package com.sgribkov.socialnetwork.data.entities.dialog

import com.sgribkov.socialnetwork.data.entities.UserIdentity
import fs2.concurrent.Queue
import zio.{Task, ZIO}
import zio.interop.catz.taskConcurrentInstance

final case class Dialog(user1: UserIdentity,
                        user2: UserIdentity,
                        out: Queue[Task[*], DialogMessage]
                       ) {
  def getReceiver(sender: UserIdentity): Option[UserIdentity] = {
    val recUser = if (user1 != sender) user1 else user2
    if (recUser == UserIdentity.DEFAULT) None else Some(recUser)
  }

  def join(joinUser: UserIdentity): Dialog = {
    if (this.user1 == UserIdentity.DEFAULT)
      this.copy(user1 = joinUser)
    else
      this.copy(user2 = joinUser)
  }

  def broadcast(msg: DialogMessage): Task[Unit] = out.enqueue1(msg)
}

object Dialog {
  def from(user: UserIdentity, out: Queue[Task, DialogMessage]): Dialog = {
    val defaultUser = UserIdentity.DEFAULT
    Dialog(user, defaultUser, out)
  }

  def from(out: Queue[Task, DialogMessage]): Dialog = {
      val defaultUser = UserIdentity.DEFAULT
      Dialog(defaultUser, defaultUser, out)
    }
}
