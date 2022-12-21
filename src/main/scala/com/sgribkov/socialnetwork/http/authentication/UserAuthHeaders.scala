package com.sgribkov.socialnetwork.http.authentication

import com.sgribkov.socialnetwork.data.Error.AuthBadFormat
import com.sgribkov.socialnetwork.data.dto.UserAuthDTO
import com.sgribkov.socialnetwork.data.entities.UserIdentity
import com.sgribkov.socialnetwork.repository.userauth.UserAuthRepo
import com.sgribkov.socialnetwork.services.auth.UserAuthService
import org.http4s.{AuthScheme, Credentials, Request}
import org.http4s.headers.Authorization
import zio.{IO, RIO, ZIO}


trait UserAuthHeaders[R <: UserAuthService with UserAuthRepo] {

  type UserAuthHTask[T] = RIO[R, T]

  private val unauthenticated =
    IO.succeed(Left(AuthBadFormat))

  def getToken(req: Request[UserAuthHTask]): UserAuthHTask[Either[Throwable, UserIdentity]] = {

    val base64StrOpt =
      req.headers
        .get(Authorization)
        .flatMap { t =>
          t.credentials match {
            case Credentials.Token(scheme, token)
              if scheme == AuthScheme.Basic => Some(token)
            case _ => None
          }
        }

    base64StrOpt.map { base64Str =>
      val authData = UserAuthDTO.fromBase64(base64Str)
      val tok =
        UserAuthService.authenticate(authData)
      tok.either
    }.getOrElse(unauthenticated)
  }

  /*def getToken(req: Request[UserAuthHTask]): UserAuthHTask[Either[Throwable, AuthToken]] = {
    val userNamePasswordOpt: Option[Array[String]] = {
      for {
        auth <- req.headers.get(Authorization).map(_.value)
        asSplit = auth.split(" ")
        if asSplit.size == 2
      } yield asSplit
    }
    userNamePasswordOpt.map { asSplit =>
      val login = UserLogin(asSplit(0))
      val pwd = UserPassword(asSplit(1))
      val authData = UserAuthDTO(login, pwd)
      println(authData)
      val tok =
        UserAuthService.authenticate(authData)
      tok.either
    }.getOrElse(unauthenticated)
  }*/
}
