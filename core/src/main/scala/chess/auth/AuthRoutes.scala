package chess.auth

import cats.effect.*
import cats.syntax.all.*
import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.server.AuthMiddleware
import chess.db.DbUser
import ujson.Value

object AuthRoutes:
  def routes(authService: AuthService, middleware: AuthMiddleware[IO, DbUser]): HttpRoutes[IO] =
    val publicRoutes = HttpRoutes.of[IO] {
      case req @ POST -> Root / "register" =>
        IO.println("DEBUG: HIT /auth/register") >>
        req.as[String].flatMap { body =>
          IO.println(s"DEBUG: Register Body (len=${body.length}): $body") >>
          val jsonTry = scala.util.Try(ujson.read(body))
          
          jsonTry match {
            case scala.util.Failure(e) =>
              IO.println(s"ERROR: JSON Parse Failed: ${e.getMessage}") >>
              BadRequest(ujson.Obj("error" -> s"JSON Parse Error: ${e.getMessage}").render())
            
            case scala.util.Success(json) =>
              val email = json("email").str
              val password = json("password").str
              
              authService.register(email, password).flatMap {
                case Right(user) => 
                   Ok(ujson.Obj(
                     "id" -> user.id.toString,
                     "email" -> user.email,
                     "tier" -> user.tier
                   ).render())
                case Left(err) => BadRequest(ujson.Obj("error" -> err).render())
              }
          }
        }.handleErrorWith { e =>
          IO.println(s"Auth/Register Error: ${e.getMessage}") >> 
          IO(e.printStackTrace()) >>
          BadRequest(ujson.Obj("error" -> s"Server Error: ${e.getMessage}").render())
        }

      case req @ POST -> Root / "login" =>
        IO.println("DEBUG: HIT /auth/login") >>
        req.as[String].flatMap { body =>
          IO.println(s"DEBUG: Login Body (len=${body.length}): $body") >>
          val jsonTry = scala.util.Try(ujson.read(body))

          jsonTry match {
            case scala.util.Failure(e) =>
              IO.println(s"ERROR: Login JSON Parse Failed: ${e.getMessage}") >>
              BadRequest(ujson.Obj("error" -> s"JSON Parse Error: ${e.getMessage}").render())
            
            case scala.util.Success(json) =>
              val email = json("email").str
              val password = json("password").str
              
              authService.login(email, password).flatMap {
                case Right(res) =>
                   Ok(ujson.Obj(
                     "token" -> res.token,
                     "user" -> ujson.Obj(
                        "id" -> res.user.id.toString,
                        "email" -> res.user.email,
                        "tier" -> res.user.tier
                     )
                   ).render())
                case Left(err) => Forbidden(ujson.Obj("error" -> err).render())
              }
          }
        }.handleErrorWith { e =>
          IO.println(s"Auth/Login Error: ${e.getMessage}") >> 
          IO(e.printStackTrace()) >>
          BadRequest(ujson.Obj("error" -> s"Server Error: ${e.getMessage}").render())
        }

      case req @ POST -> Root / "login" / "google" =>
        IO.println("DEBUG: HIT /auth/login/google") >>
        req.as[String].flatMap { body =>
          IO.println(s"DEBUG: Google Login Body (len=${body.length}): $body") >>
          val jsonTry = scala.util.Try(ujson.read(body))

          jsonTry match {
            case scala.util.Failure(e) =>
              IO.println(s"ERROR: Google Login JSON Parse Failed: ${e.getMessage}") >>
              BadRequest(ujson.Obj("error" -> s"JSON Parse Error: ${e.getMessage}").render())

            case scala.util.Success(json) =>
              val idToken = json("idToken").str
              
              authService.loginOrRegisterGoogle(idToken).flatMap {
                case Right(res) =>
                   Ok(ujson.Obj(
                     "token" -> res.token,
                     "user" -> ujson.Obj(
                        "id" -> res.user.id.toString,
                        "email" -> res.user.email,
                        "tier" -> res.user.tier
                     )
                   ).render())
                case Left(err) => Forbidden(ujson.Obj("error" -> err).render())
              }
          }
        }.handleErrorWith { e =>
          IO.println(s"Auth/Login Error: ${e.getMessage}") >> 
          IO(e.printStackTrace()) >>
          BadRequest(ujson.Obj("error" -> s"Server Error: ${e.getMessage}").render())
        }
    }

    val privateRoutes: AuthedRoutes[DbUser, IO] = AuthedRoutes.of {
      case GET -> Root / "me" as user =>
        Ok(ujson.Obj(
           "id" -> user.id.toString,
           "email" -> user.email,
           "tier" -> user.tier,
           "createdAt" -> user.createdAt.toString
        ).render())
    }

    // Mount public at / (e.g. /auth/register)
    // Mount private at / (e.g. /auth/me) protected by middleware
    publicRoutes <+> middleware(privateRoutes)

