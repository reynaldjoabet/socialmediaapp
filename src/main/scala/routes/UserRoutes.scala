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
import doobie.util.transactor._

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
                        path = Some("/")
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
        .as[CreateUser]
        .flatMap { createUser =>
          userService
            .findUserByUsername(createUser.username)
            .flatMap {
              case Some(user) => Conflict(UserExists("User already exists"))

              case None =>
                userService
                  .saveUser(
                    createUser.username,
                    createUser.email,
                    HashingService.hashPassword(createUser.password),
                    createUser.name,
                    createUser.coverPicture,
                    createUser.profilePicture,
                    createUser.city,
                    createUser.website
                  )
                  .flatMap(_ => Created())

            }
        }
        .handleErrorWith(e => InternalServerError(e.toString()))

  }

  val router: HttpRoutes[F] = Router(prefix -> routes)
}

object UserRoutes {

  def make[F[_]: Async](): UserRoutes[F] = UserRoutes[F](UserService(xa))

  def make[F[_]: Async](transactor: Transactor[F]): UserRoutes[F] = UserRoutes[F](
    UserService(transactor)
  )

}
