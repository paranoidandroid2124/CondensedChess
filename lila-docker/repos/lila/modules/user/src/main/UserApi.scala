package lila.user

import lila.core.user.User
import lila.core.userId.*
import lila.core.email.EmailAddress

final class UserApi(userRepo: UserRepo)(using
    ec: Executor
) extends lila.core.user.UserApi:

  export userRepo.{
    byId,
    byIds,
    me,
    isEnabled,
    enable,
    disable,
    delete,
    updateUsername,
    updateEmail,
    userIdsWithRoles,
    filterExists
  }

  def email(id: UserId): Fu[Option[EmailAddress]] =
    userRepo.byId(id).map(_.flatMap(_.email))

  def accountAge(id: UserId): Fu[scalalib.model.Days] =
    userRepo.byId(id).map:
      _.fold(scalalib.model.Days(0)): u =>
        scalalib.model.Days(java.time.Duration.between(u.createdAt, java.time.Instant.now()).toDays().toInt)

  def isCreatedSince(id: UserId, since: java.time.Instant): Fu[Boolean] =
    userRepo.byId(id).map(_.exists(_.createdAt.isAfter(since)))

  def enabledById[U: UserIdOf](u: U): Fu[Option[User]] =
    userRepo.byId(u).map(_.filter(_.enabled.yes))
