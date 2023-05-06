package com.sgribkov.socialnetwork.data.entities.dialog

import com.sgribkov.socialnetwork.data.entities.UserLogin
import scala.math.abs

final case class DialogId(value: String) extends AnyVal {

  def normalize: DialogId = {
    val id = value.split(":").sorted.mkString(":")
    DialogId(id)
  }

  def reverse: DialogId = {
    val id = value.split(":").reverse.mkString(":")
    DialogId(id)
  }

  def getVBucket: Int = abs(this.normalize.hashCode()) % 256
}

object DialogId {

  def from(login1: UserLogin, login2: UserLogin): DialogId = {
    val id = login1.value + ":" + login2.value
    DialogId(id)
  }
}
