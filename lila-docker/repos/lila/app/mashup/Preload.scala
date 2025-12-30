package lila.app
package mashup

import lila.user.{ LightUserApi, Me }

// Chesstory: Minimal Preload for analysis-only system
// Removed: playban, timeline, lobbyApi, roundProxy, feed, perfsRepo
final class Preload(
    lightUserApi: LightUserApi
)(using Executor):

  import Preload.*

  // Analysis-only: no lobby homepage needed
  def apply()(using ctx: Context): Fu[Homepage] = 
    fuccess(Homepage(ctx.me.map(_.userId)))

  // No current game tracking in analysis-only mode
  def currentGameMyTurn(using me: Me): Fu[Option[CurrentGame]] = fuccess(None)

object Preload:

  // Minimal Homepage - analysis system doesn't need lobby data
  case class Homepage(
      me: Option[UserId]
  )

  case class CurrentGame(pov: Pov, opponent: String)
