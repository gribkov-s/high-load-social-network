package com.sgribkov.socialnetwork.http.api

import com.sgribkov.socialnetwork.data.dto.UserRegDTO
import com.sgribkov.socialnetwork.repository.userauth.UserAuthRepo
import com.sgribkov.socialnetwork.repository.userprofile.UserProfileRepo
import com.sgribkov.socialnetwork.services.auth.UserAuthService
import com.sgribkov.socialnetwork.services.user.UserService
import io.circe.{Decoder, Encoder}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, EntityEncoder, HttpRoutes}
import zio.RIO
import zio.interop.catz.taskConcurrentInstance
import zio.random.Random


class UserRegAPI[R <: UserService with
                      UserProfileRepo with
                      UserAuthRepo with
                      Random] {

  type UserRegTask[A] =  RIO[R, A]

  val dsl = Http4sDsl[UserRegTask]
  import dsl._

  implicit def jsonDecoder[A](implicit decoder: Decoder[A]): EntityDecoder[UserRegTask, A] =
    jsonOf[UserRegTask, A]

  implicit def jsonEncoder[A](implicit decoder: Encoder[A]): EntityEncoder[UserRegTask, A] =
    jsonEncoderOf[UserRegTask, A]

  val routes: HttpRoutes[UserRegTask] = HttpRoutes.of[UserRegTask] {

    case reqUser @ POST -> Root / "registration" =>
      reqUser.decode[UserRegDTO] { register =>
        UserService.registerUser(register).foldM(
          err => BadRequest(err.getMessage),
          result => Ok(s"User ${register.auth.login.value} was registered successfully")
        )
      }
  }

}
