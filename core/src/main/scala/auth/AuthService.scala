package chess.auth

import cats.effect.*
import chess.db.UserRepo
import doobie.implicits.*
import doobie.Transactor
import org.mindrot.jbcrypt.BCrypt
import tsec.jws.mac.JWTMac
import tsec.mac.jca.HMACSHA256
import tsec.jwt.JWTClaims
import java.time.Instant
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.net.URI

case class RegisteredUser(id: java.util.UUID, email: String, tier: String)
case class LoginResult(token: String, user: RegisteredUser)

class AuthService(xa: Transactor[IO], jwtSecret: String):
  
  // JWT Key
  private val macKey = HMACSHA256.unsafeBuildKey(jwtSecret.getBytes)
  private val httpClient = HttpClient.newHttpClient()

  def register(email: String, password: String): IO[Either[String, RegisteredUser]] =
    UserRepo.findByEmail(email).transact(xa).flatMap {
      case Some(_) => IO.pure(Left("Email already registered"))
      case None =>
         for
           hash <- IO.blocking(BCrypt.hashpw(password, BCrypt.gensalt()))
           user <- UserRepo.create(email, hash, "BETA").transact(xa)
         yield Right(RegisteredUser(user.id, user.email, user.tier))
    }

  private def generateToken(user: chess.db.DbUser): IO[Either[String, LoginResult]] =
    val claims = JWTClaims(
      subject = Some(user.id.toString),
      expiration = Some(Instant.now.plusSeconds(3600 * 24)) // 1 day
    )
    JWTMac.buildToString[IO, HMACSHA256](claims, macKey).map { token =>
      Right(LoginResult(token, RegisteredUser(user.id, user.email, user.tier)))
    }

  def login(email: String, password: String): IO[Either[String, LoginResult]] =
     UserRepo.findByEmail(email).transact(xa).flatMap {
       case None => IO.pure(Left("Invalid credentials"))
       case Some(user) =>
         IO.blocking(BCrypt.checkpw(password, user.passwordHash)).flatMap {
           case false => IO.pure(Left("Invalid credentials"))
           case true => generateToken(user)
         }
     }

  def verify(token: String): IO[Option[java.util.UUID]] =
    JWTMac.verifyAndParse[IO, HMACSHA256](token, macKey).map { jwt =>
      jwt.body.subject.flatMap(s => scala.util.Try(java.util.UUID.fromString(s)).toOption)
    }.handleError(_ => None)

  // --- Google Auth ---

  def verifyGoogleToken(idToken: String): IO[Option[String]] = IO.blocking {
    val request = HttpRequest.newBuilder()
      .uri(URI.create(s"https://oauth2.googleapis.com/tokeninfo?id_token=$idToken"))
      .GET()
      .build()
    val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    if (response.statusCode() == 200) {
      val json = ujson.read(response.body())
      json.obj.get("email").map(_.str)
    } else None
  }.handleErrorWith(_ => IO.pure(None))

  def loginOrRegisterGoogle(idToken: String): IO[Either[String, LoginResult]] =
    verifyGoogleToken(idToken).flatMap {
      case None => IO.pure(Left("Invalid Google Token"))
      case Some(email) =>
         UserRepo.findByEmail(email).transact(xa).flatMap {
           case Some(user) => generateToken(user)
           case None =>
             val dummyPass = java.util.UUID.randomUUID().toString
             for
               hash <- IO.blocking(BCrypt.hashpw(dummyPass, BCrypt.gensalt()))
               user <- UserRepo.create(email, hash, "BETA").transact(xa)
               res <- generateToken(user)
             yield res
         }
    }

object AuthService:
  def make(xa: Transactor[IO], jwtSecret: String): AuthService = new AuthService(xa, jwtSecret)
