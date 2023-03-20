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

  private val unauthenticated = IO.succeed(Left(AuthBadFormat))

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
}
