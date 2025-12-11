package chess.auth

import cats.data.{Kleisli, OptionT}
import cats.effect.IO
import org.http4s.*
import org.http4s.server.AuthMiddleware
import org.http4s.dsl.io.*
import org.http4s.headers.Authorization
import chess.db.{UserRepo, DbUser}
import doobie.Transactor
import doobie.implicits.* 

object AuthMiddlewareVals:
  def apply(authService: AuthService, xa: Transactor[IO]): AuthMiddleware[IO, DbUser] =
    val authUser: Kleisli[IO, Request[IO], Either[String, DbUser]] = Kleisli { req =>
      val tokenOpt = req.headers.get[Authorization].collect {
        case Authorization(Credentials.Token(AuthScheme.Bearer, token)) => token
      }
      
      tokenOpt match
        case Some(token) =>
          authService.verify(token).flatMap {
            case Some(userId) =>
               UserRepo.findById(userId).transact(xa).map {
                 case Some(user) => Right(user)
                 case None => Left("User not found")
               }
            case None => IO.pure(Left("Invalid token"))
          }
        case None => IO.pure(Left("Missing token"))
    }

    val onFailure: AuthedRoutes[String, IO] = Kleisli { req =>
      OptionT.liftF(Forbidden(ujson.Obj("error" -> req.context).render()))
    }

    AuthMiddleware(authUser, onFailure)

  def optionalAuthMiddleware(authService: AuthService, xa: Transactor[IO], apiKey: String): AuthMiddleware[IO, Option[DbUser]] =
    val authLogic: Kleisli[IO, Request[IO], Either[String, Option[DbUser]]] = Kleisli { req =>
      // 1. Check JWT
      val tokenOpt = req.headers.get[Authorization].collect {
        case Authorization(Credentials.Token(AuthScheme.Bearer, token)) => token
      }

      tokenOpt match
        case Some(token) =>
          authService.verify(token).flatMap {
            case Some(userId) =>
               UserRepo.findById(userId).transact(xa).map {
                 case Some(user) => Right(Some(user))
                 case None => Left("User not found")
               }
            case None => IO.pure(Left("Invalid token"))
          }
        case None =>
          // 2. Check API Key
          val keyOpt = req.headers.get(org.typelevel.ci.CIString("x-api-key")).map(_.head.value)
          keyOpt match
             case Some(key) if key == apiKey => IO.pure(Right(None))
             case Some(_) => IO.pure(Left("Invalid API Key"))
             case None => IO.pure(Left("Missing Credentials"))
    }

    val onFailureOptions: AuthedRoutes[String, IO] = Kleisli { req =>
      OptionT.liftF(Forbidden(ujson.Obj("error" -> req.context).render()))
    }

    AuthMiddleware(authLogic, onFailureOptions)
