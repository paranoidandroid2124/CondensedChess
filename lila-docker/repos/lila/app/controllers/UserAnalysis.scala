package controllers

import play.api.mvc.*
import lila.app.*

final class UserAnalysis(
    env: Env
) extends LilaController(env)
    with lila.web.TheftPrevention:

  def index = Open:
    Ok.page(AnalysePgnPipeline.page())

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

    Ok.page(AnalysePgnPipeline.page(variant = variant, fen = fen))

  def pgn(pgnString: String) = Open:
    val decodedPgn = lila.common.String.decodeUriPath(pgnString).getOrElse(pgnString)
    AnalysePgnPipeline.normalizedInlinePgn(decodedPgn).fold[Fu[Result]](
      BadRequest("Empty PGN payload.").toFuccess
    ): inlinePgn =>
      Ok.page(AnalysePgnPipeline.page(inlinePgn = Some(inlinePgn)))

  def embed = Anon:
    Ok("Analysis Board Embed").toFuccess
