package controllers

import play.api.mvc.*
import lila.app.*
import lila.analyse.CondensedJsonView
import lila.core.game.Pov
import lila.tree.Root

final class UserAnalysis(
    env: Env
) extends LilaController(env)
    with lila.web.TheftPrevention:

  def index = Open:
    val pov = makePov(None, chess.variant.Standard)
    val root = Root.default(pov.game.variant)
    val data = CondensedJsonView(pov, root, ctx.pref)
    Ok.page(views.analyse.ui.userAnalysis(data, pov))

  def parseArg(arg: String) = Open:
    val (key, fenStr) = arg.indexOf('/') match
      case -1 => (arg, None)
      case i  => (arg.take(i), Some(arg.drop(i + 1)))

    val variant = chess.variant.Variant.orDefault(chess.variant.Variant.LilaKey(key))
    val fen: Option[chess.format.Fen.Full] = fenStr
      .map(_.replace("_", " "))
      .flatMap(lila.common.String.decodeUriPath)
      .filter(_.trim.nonEmpty)
      .map(chess.format.Fen.Full.clean)

    val pov = makePov(fen, variant)
    val root = fen.fold(Root.default(variant))(f => Root.default(variant).copy(fen = f))

    val data = CondensedJsonView(pov, root, ctx.pref)
    Ok.page(views.analyse.ui.userAnalysis(data, pov))

  def pgn(pgnString: String) = Open:
    pgnString.length
    val pov = makePov(None, chess.variant.Standard)
    val root = Root.default(pov.game.variant)
    val data = CondensedJsonView(pov, root, ctx.pref)
    Ok.page(views.analyse.ui.userAnalysis(data, pov))

  def embed = Anon:
    Ok("Analysis Board Embed").toFuccess

  private[controllers] def makePov(fen: Option[chess.format.Fen.Full], variant: chess.variant.Variant): Pov =
    Pov(lila.core.game.Game.make(variant, fen), chess.Color.White)
