package lila.security

import lila.common.Executor
import lila.core.lilaism.Core.*
import lila.db.dsl.{ *, given }
import lila.core.userId.*
import lila.core.security.{ ClearPassword, HashedPassword }
import lila.core.user.{ User, UserEnabled }
import lila.user.{ UserRepo, BSONFields as F }
import lila.user.BSONHandlers.given

object Signup:
  enum Result:
    case AllSet(user: User)
    case RateLimited
    case ForbiddenNetwork
    case MissingCaptcha
    case Bad(err: play.api.data.Form[?])

final class Signup(
    userRepo: UserRepo,
    authenticator: Authenticator
)(using Executor):

  def apply(username: UserName, password: ClearPassword): Fu[User] =
    val id = username.id
    val user = User(
      id = id,
      username = username,
      enabled = UserEnabled(true),
      roles = Nil,
      createdAt = java.time.Instant.now()
    )
    val hash = authenticator.passEnc(password)
    val doc = lila.user.BSONHandlers.userBSONWriter.writeTry(user).get ++ $doc(
      F.bpass -> hash.bytes
    )
    userRepo.coll.insert.one(doc) >> Future.successful(user)

  def website(blind: Boolean): Fu[Signup.Result] =
    // Simplified: always return rate limited or mock result for now if not implemented
    Future.successful(Signup.Result.MissingCaptcha)

  def mobile(apiVersion: lila.core.net.ApiVersion): Fu[Signup.Result] =
    Future.successful(Signup.Result.MissingCaptcha)
