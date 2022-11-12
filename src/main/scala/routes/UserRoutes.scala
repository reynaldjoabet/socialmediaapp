package routes

import org.http4s.dsl.Http4sDsl
import org.http4s._

import cats.effect.kernel.Async
import services.UserService
import cats.implicits._
import org.http4s.implicits._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.circe.CirceEntityDecoder._
import api._
import error._
import services._
import domain.User
import db.Doobie._
import org.http4s.ResponseCookie
import java.util.UUID

final case class UserRoutes[F[_]: Async](userService: UserService[F]) extends Http4sDsl[F] {

  import org.http4s.server.Router

  private val prefix = "api/users"

  private val routes = HttpRoutes.of[F] {

    case req @ POST -> Root / "login" =>
      req
        .as[LoginUser]
        .flatMap { loginUser =>
          userService
            .findUserByUsername(loginUser.username)
            .flatMap {
              case Some(user) =>
                if (HashingService.checkPassword(user.password, loginUser.password))
                  Ok(user).map(
                    _.addCookie(
                      ResponseCookie(
                        "sessionID",
                        UUID.randomUUID().toString(),
                        domain = Some("localhost"),
                        path = Some("/"),
                      )
                    )
                  )
                else
                  Forbidden(InvalidUsernamePassword("Invalid username or password"))

              case None => Forbidden(UserNotFound("User does not exist"))

            }
        }
        .handleErrorWith(e => InternalServerError(e.toString()))

    case req @ POST -> Root / "register" =>
      req
        .as[RegisterUser]
        .flatMap { registerUser =>
          userService
            .findUserByUsername(registerUser.username)
            .flatMap {
              case Some(user) => Conflict(UserExists("User already exists"))

              case None =>
                userService
                  .saveUser(
                    registerUser.username,
                    registerUser.email,
                    HashingService.hashPassword(registerUser.password),
                    registerUser.name,
                    registerUser.coverPicture,
                    registerUser.profilePicture,
                    registerUser.city,
                    registerUser.website,
                  )
                  .flatMap(_ => Created())

            }
        }
        .handleErrorWith(e => InternalServerError(e.toString()))

  }

  val router: HttpRoutes[F] = Router(prefix -> routes)
}

object UserRoutes {

  def make[F[_]: Async]() = UserRoutes[F](UserService(xa))
}
