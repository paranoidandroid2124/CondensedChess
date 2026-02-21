package lila.security

import lila.core.email.NormalizedEmailAddress
import lila.core.security.{ ClearPassword, HashedPassword }
import lila.db.dsl.{ *, given }
import lila.core.lilaism.Core.*
import lila.user.{ AuthData, BSONFields as F, UserRepo }
import lila.user.AuthData.given
import lila.user.BSONHandlers.given
import lila.core.user.*
import lila.core.userId.UserId

case class PasswordAndToken(password: ClearPassword, token: Option[TotpToken])
type CredentialCheck = ClearPassword => Boolean

case class LoginCandidate(user: User, check: CredentialCheck, isBlanked: Boolean, must2fa: Boolean = false):
  import LoginCandidate.*
  import lila.core.user.TotpSecret.verify
  def apply(p: PasswordAndToken): Result =
    val res =
      if user.totpSecret.isEmpty && must2fa then Result.Must2fa
      else if check(p.password) then
        user.totpSecret.fold[Result](Result.Success(user)): tp =>
          p.token.fold[Result](Result.MissingTotpToken): token =>
            if tp.verify(token) then Result.Success(user) else Result.InvalidTotpToken
      else if isBlanked then Result.BlankedPassword
      else Result.InvalidUsernameOrPassword
    // lila.mon.user.auth.count(res.success).increment()
    res
  def option(p: PasswordAndToken): Option[User] = apply(p).toOption

object LoginCandidate:
  enum Result(val toOption: Option[User]):
    def success = toOption.isDefined
    case Success(user: User) extends Result(Some(user))
    case InvalidUsernameOrPassword extends Result(Option.empty)
    case Must2fa extends Result(Option.empty)
    case BlankedPassword extends Result(Option.empty)
    case WeakPassword extends Result(Option.empty)
    case MissingTotpToken extends Result(Option.empty)
    case InvalidTotpToken extends Result(Option.empty)

final class Authenticator(
    passHasher: PasswordHasher,
    userRepo: UserRepo
)(using Executor)
    extends lila.core.security.Authenticator:

  def passEnc(p: ClearPassword): HashedPassword = passHasher.hash(p)

  def compare(auth: AuthData, p: ClearPassword): Boolean =
    passHasher.check(auth.bpass, p)

  def authenticateById(id: UserId, passwordAndToken: PasswordAndToken): Fu[Option[User]] =
    loginCandidateById(id) flatMap {
      case None => Future.successful(None)
      case Some(c) => Future.successful(c.option(passwordAndToken))
    }

  def authenticateByEmail(
      email: NormalizedEmailAddress,
      passwordAndToken: PasswordAndToken
  ): Fu[Option[User]] =
    loginCandidateByEmail(email) flatMap {
      case None => Future.successful(None)
      case Some(c) => Future.successful(c.option(passwordAndToken))
    }

  def loginCandidate(using me: Me): Fu[LoginCandidate] =
    loginCandidate($id(me.userId)).map { 
      _ | LoginCandidate(me, _ => false, false) 
    }

  def loginCandidateById(id: UserId): Fu[Option[LoginCandidate]] =
    loginCandidate($id(id))

  def loginCandidateByEmail(email: NormalizedEmailAddress): Fu[Option[LoginCandidate]] =
    loginCandidate($doc(F.email -> email))

  def setPassword(id: UserId, p: ClearPassword): Funit =
    userRepo.coll.update.one($id(id), $set(F.bpass -> passEnc(p))).void

  private def authWithBenefits(auth: AuthData)(p: ClearPassword): Boolean =
    compare(auth, p)

  private def loginCandidate(select: Bdoc): Fu[Option[LoginCandidate]] =
    userRepo.coll.find(select).one[Bdoc].map { doc =>
      for
        user <- doc.flatMap(_.asOpt[User])
        auth <- doc.flatMap(_.asOpt[AuthData])
      yield LoginCandidate(
        user = user,
        check = authWithBenefits(auth),
        isBlanked = false // Simplified
      )
    }
