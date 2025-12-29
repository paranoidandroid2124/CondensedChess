package lila.api

import lila.common.Bus
import lila.core.perm.Granter
import lila.user.UserDelete
import lila.db.dsl.{ *, given }

/* Chesstory: Simplified account termination (analysis-only system)
 * Removed: playban, plan, push, round, chat dependencies
 */
final class AccountTermination(
    userRepo: lila.user.UserRepo,
    rankingApi: lila.user.RankingApi,
    securityStore: lila.security.SessionStore,
    email: lila.mailer.AutomaticEmail,
    tokenApi: lila.oauth.AccessTokenApi,
    gameRepo: lila.game.GameRepo,
    analysisRepo: lila.analyse.AnalysisRepo
)(using Executor, Scheduler, akka.stream.Materializer):

  def disable(u: User, forever: Boolean)(using me: Me): Funit = for
    _ <- isEssential(u.id).so:
      fufail[Unit](s"Cannot disable essential account ${u.username}")
    selfClose = me.is(u)
    modClose = !selfClose && Granter(_.CloseAccount)
    tos = u.marks.dirty || modClose
    _ <- userRepo.disable(u, keepEmail = tos, forever = forever)
    _ <- rankingApi.remove(u.id)
    _ <- securityStore.closeAllSessionsOf(u.id)
    _ <- selfClose.so(tokenApi.revokeAllByUser(u.id))
  yield Bus.pub(lila.core.security.CloseAccount(u.id))

  def scheduleDelete(u: User)(using Me): Funit = for
    _ <- disable(u, forever = false)
    _ <- email.delete(u)
    _ <- userRepo.delete.schedule(u.id, UserDelete(nowInstant).some)
  yield ()

  lila.common.LilaScheduler.variableDelay(
    "accountTermination.delete",
    delay = prev => _.Delay(if prev.isDefined then 1.second else 10.seconds),
    timeout = _.AtMost(1.minute),
    initialDelay = _.Delay(111.seconds)
  ):
    userRepo.delete.findNextScheduled.flatMapz: user =>
      if user.enabled.yes
      then userRepo.delete.schedule(user.id, none).inject(none)
      else doDeleteNow(user).inject(user.some)

  private def doDeleteNow(u: User): Funit = for
    _ <- isEssential(u.id).so:
      fufail[Unit](s"Cannot delete essential account ${u.username}")
    tos = u.marks.dirty
    _ = logger.info(s"Deleting user ${u.username} tos=$tos")
    _ <- if tos then userRepo.delete.nowWithTosViolation(u) else userRepo.delete.nowFully(u)
    singlePlayerGameIds <- gameRepo.deleteAllSinglePlayerOf(u.id)
    _ <- analysisRepo.remove(singlePlayerGameIds)
    _ <- tokenApi.revokeAllByUser(u.id)
    _ <- u.marks.clean.so:
      securityStore.deleteAllSessionsOf(u.id)
  yield
    Bus.pub(lila.core.user.UserDelete(u))

  private val isEssential: Set[UserId] =
    Set(
      UserId.lichess,
      UserId.ai
    )
