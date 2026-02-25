package controllers

import lila.app.*
import lila.analyse.CondensedJsonView
import lila.core.game.Pov
import lila.tree.Root
import lila.ui.Page
import play.api.libs.json.JsObject

object AnalysePgnPipeline:

  // Shared "analyse PGN" entrypoint used by all controller-side import flows.
  private val maxInlinePgnChars = 200000

  final case class RenderPayload(
      data: JsObject,
      pov: Pov,
      inlinePgn: Option[String]
  )

  def normalizedInlinePgn(rawPgn: String): Option[String] =
    Option(rawPgn).map(_.trim).filter(_.nonEmpty).map: pgn =>
      if pgn.length > maxInlinePgnChars then pgn.take(maxInlinePgnChars) else pgn

  def buildPayload(
      variant: chess.variant.Variant = chess.variant.Standard,
      fen: Option[chess.format.Fen.Full] = None,
      inlinePgn: Option[String] = None
  )(using ctx: Context): RenderPayload =
    val pov = Pov(lila.core.game.Game.make(variant, fen), chess.Color.White)
    val root = fen.fold(Root.default(variant))(f => Root.default(variant).copy(fen = f))
    val data = CondensedJsonView(pov, root, ctx.pref)
    RenderPayload(
      data = data,
      pov = pov,
      inlinePgn = inlinePgn.flatMap(normalizedInlinePgn)
    )

  def page(
      variant: chess.variant.Variant = chess.variant.Standard,
      fen: Option[chess.format.Fen.Full] = None,
      inlinePgn: Option[String] = None
  )(using ctx: Context): Page =
    val payload = buildPayload(variant = variant, fen = fen, inlinePgn = inlinePgn)
    views.analyse.ui.userAnalysis(payload.data, payload.pov, inlinePgn = payload.inlinePgn)
