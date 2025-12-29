package views.game

import lila.app.UiEnv.*

val ui = lila.game.ui.GameUi(helpers)
export ui.mini

def sides(
    pov: Pov,
    initialFen: Option[chess.format.Fen.Full],
    tour: Option[Any],  // tournament module removed
    cross: Option[lila.game.Crosstable.WithMatchup],
    simul: Option[Any],  // simul module removed
    userTv: Option[User] = None,
    bookmarked: Boolean
)(using ctx: Context) =
  div(
    side.meta(pov, initialFen, none, none, userTv, bookmarked = bookmarked),
    cross.map: c =>
      div(cls := "crosstable")(ui.crosstable(ctx.userId.foldLeft(c)(_.fromPov(_)), pov.gameId.some))
  )

def widgets(
    games: Seq[Game],
    notes: Map[GameId, String] = Map(),
    user: Option[User] = None,
    ownerLink: Boolean = false
)(using ctx: lila.ui.Context): Frag =
  games.map: g =>
    ui.widgets(g, notes.get(g.id), user, ownerLink)(emptyFrag)  // tournament/simul/swiss links removed
