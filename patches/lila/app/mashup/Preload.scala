package lila.app
package mashup

import play.api.libs.json.*

import lila.core.game.Game
import lila.core.perf.UserWithPerfs
import lila.playban.TempBan
import lila.timeline.Entry
import lila.user.{ LightUserApi, Me, User }

// Simplified Preload - many modules deleted for analysis-focused app
final class Preload(
    gameRepo: lila.game.GameRepo,
    perfsRepo: lila.user.UserPerfsRepo,
    timelineApi: lila.timeline.EntryApi,
    lobbyApi: lila.api.LobbyApi,
    playbanApi: lila.playban.PlaybanApi,
    lightUserApi: LightUserApi,
    roundProxy: lila.round.GameProxyRepo,
    getLastUpdates: lila.feed.Feed.GetLastUpdates
)(using Executor):

  import Preload.*

  def apply()(using ctx: Context): Fu[Homepage] = for
    withPerfs <- ctx.user.traverse(perfsRepo.withPerfs)
    given Option[UserWithPerfs] = withPerfs
    (data, entries) <- lobbyApi.get
      .mon(_.lobby.segment("lobbyApi"))
      .zip((ctx.userId.so(timelineApi.userEntries)).mon(_.lobby.segment("timeline")))
    playban <- ctx.userId.so(playbanApi.currentBan).mon(_.lobby.segment("playban"))
    blindGames <- ctx.blind.so(ctx.me).so(roundProxy.urgentGames)
    _ <- lightUserApi.preloadMany(entries.flatMap(_.userIds).toList)
  yield Homepage(
    data,
    entries,
    playban,
    blindGames,
    getLastUpdates(),
    withPerfs
  )

  def currentGameMyTurn(using me: Me): Fu[Option[CurrentGame]] =
    gameRepo
      .playingRealtimeNoAi(me)
      .flatMap:
        _.map { roundProxy.pov(_, me) }.parallel.dmap(_.flatten)
      .flatMap:
        currentGameMyTurn(_, lightUserApi.sync)

  private def currentGameMyTurn(povs: List[Pov], lightUser: lila.core.LightUser.GetterSync)(using
      me: Me
  ): Fu[Option[CurrentGame]] =
    ~povs.collectFirst:
      case p1 if p1.game.nonAi && p1.game.hasClock && p1.isMyTurn =>
        roundProxy.pov(p1.gameId, me).dmap(_ | p1).map { pov =>
          val opponent = lila.game.Namer.playerTextBlocking(pov.opponent)(using lightUser)
          CurrentGame(pov = pov, opponent = opponent).some
        }

object Preload:

  // Simplified Homepage - many fields removed (tournament, swiss, event, simul, streamer, puzzle, ublog, relay)
  case class Homepage(
      data: JsObject,
      userTimeline: Vector[Entry],
      playban: Option[TempBan],
      blindGames: List[Pov],
      lastUpdates: List[lila.feed.Feed.Update],
      me: Option[UserWithPerfs]
  )

  case class CurrentGame(pov: Pov, opponent: String)
