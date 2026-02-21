package lila.api

import lila.common.Bus
import scala.annotation.unused

/* Chesstory: Simplified account termination (analysis-only system)
 * Removed: playban, plan, push, round, chat, ranking dependencies
 * Most functionality is stubbed out for the minimal analysis backend
 */
final class AccountTermination(
    @unused userRepo: lila.user.UserRepo,
    securityStore: lila.security.SessionStore
)(using Executor):

  def disable(u: User, @unused forever: Boolean)(using @unused me: Me): Funit = for
    _ <- isEssential(u.id).so:
      fufail[Unit](s"Cannot disable essential account ${u.username}")
    _ <- securityStore.closeAllSessionsOf(u.id)
  yield Bus.pub(lila.core.security.CloseAccount(u.id))

  def scheduleDelete(u: User)(using Me): Funit = for
    _ <- disable(u, forever = false)
  yield ()

  private val isEssential: Set[UserId] =
    Set(
      UserId.lichess,
      UserId.ai
    )
