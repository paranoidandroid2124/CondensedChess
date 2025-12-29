package lila.user

import chess.PlayerTitle
import lila.core.user.{ Me, User, RoleDbKey }
import lila.core.userId.*
import lila.core.email.EmailAddress
import lila.db.dsl.*

final class UserApi(userRepo: UserRepo)(using
    ec: Executor
) extends lila.core.user.UserApi:

  export userRepo.{
    byId,
    byIds,
    me,
    isEnabled,
    userIdsWithRoles,
    filterExists
  }

  def email(id: UserId): Fu[Option[EmailAddress]] =
    userRepo.byId(id).map(_.map(u => EmailAddress(u.id.value + "@example.com"))) // Dummy email logic for now if needed, or implement properly

  def accountAge(id: UserId): Fu[scalalib.model.Days] =
    userRepo.byId(id).map:
      _.fold(scalalib.model.Days(0)): u =>
        scalalib.model.Days(java.time.Duration.between(u.createdAt, java.time.Instant.now()).toDays().toInt)

  def isCreatedSince(id: UserId, since: java.time.Instant): Fu[Boolean] =
    userRepo.byId(id).map(_.exists(_.createdAt.isAfter(since)))

  def byIdAs[U: BSONDocumentReader](id: String, proj: Bdoc): Fu[Option[U]] =
    userRepo.coll.one[U]($id(id), proj)
