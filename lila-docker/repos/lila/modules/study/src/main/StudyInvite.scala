package lila.study

import lila.core.perm.Granter
import lila.db.dsl.{ *, given }

// Chesstory: Removed notify, relation, and prefApi - analysis-only system
final private class StudyInvite(
    studyRepo: StudyRepo,
    userApi: lila.core.user.UserApi
)(using Executor, lila.core.config.RateLimit):

  private val maxMembers = 30

  def apply(
      byUserId: UserId,
      study: Study,
      invitedUsername: UserStr,
      getIsPresent: UserId => Fu[Boolean]
  ): Fu[User] = for
    _ <- (study.nbMembers >= maxMembers).so(fufail[Unit](s"Max study members reached: $maxMembers"))
    inviter <- userApi.me(byUserId).orFail("No such inviter")
    _ <- (!study.isOwner(inviter) && !Granter.ofUser(_.StudyAdmin)(inviter)).so:
      fufail[Unit]("Only the study owner can invite")
    invited <-
      userApi
        .enabledById(invitedUsername)
        .map(_.filterNot(u => UserId.lichess.is(u) && !Granter.ofUser(_.StudyAdmin)(inviter)))
        .orFail("No such invited")
    _ <- study.members.contains(invited).so(fufail[Unit]("Already a member"))
    _ <- studyRepo.addMember(study, StudyMember.make(invited))
  yield invited

  def becomeAdmin(me: MyId)(study: Study): Funit =
    studyRepo.coll:
      _.update
        .one(
          $id(study.id),
          $set(s"members.${me}" -> $doc("role" -> "w", "admin" -> true)) ++
            $addToSet("uids" -> me)
        )
        .void
