package com.sgribkov.socialnetwork.data.entities.dialog

import com.sgribkov.socialnetwork.data.entities.UserIdentity
import fs2.concurrent.Queue
import zio.{Task, ZIO}

case class DialogUser(user: UserIdentity, out: Queue[Task[*], DialogMessage]) {
  def identify(id: UserIdentity): Boolean = user == id
}

