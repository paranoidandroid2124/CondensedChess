package controllers

import lila.app.*
import lila.analyse.ImportHistory
import play.api.libs.json.{ JsObject, Json }
import play.api.mvc.*

final class UserAnalysis(
    env: Env
) extends LilaController(env)
    with lila.web.TheftPrevention:

  def index = Open:
    renderPage()

  def help = Open:
    fuccess(Ok.snip(views.analyse.ui.keyboardHelp))

  def parseArg(arg: String) = Open:
    val (key, fenStr) = arg.indexOf('/') match
      case -1 => (arg, None)
      case i  => (arg.take(i), Some(arg.drop(i + 1)))

    chess.variant.Variant(chess.variant.Variant.LilaKey(key)).fold(
      Redirect(routes.UserAnalysis.index).toFuccess
    ): variant =>
      val rawFen: Option[chess.format.Fen.Full] = fenStr
        .map(_.replace("_", " "))
        .flatMap(lila.common.String.decodeUriPath)
        .filter(_.trim.nonEmpty)
        .map(chess.format.Fen.Full.clean)
      val fen = rawFen.filter(fen => chess.format.Fen.read(variant, fen).isDefined)
      val positionQuery = Option.when(variant.chess960 && rawFen.isEmpty)(ctx.req.getQueryString("position")).flatten

      if rawFen.isDefined && fen.isEmpty then Redirect(routes.UserAnalysis.parseArg(variant.key.value)).toFuccess
      else if positionQuery.isDefined then
        Chess960Analysis
          .canonicalArgForPosition(positionQuery)
          .fold(Redirect(routes.UserAnalysis.parseArg(variant.key.value)).toFuccess): canonicalArg =>
            Redirect(routes.UserAnalysis.parseArg(canonicalArg)).toFuccess
      else renderPage(variant = variant, fen = fen)

  def pgn(pgnString: String) = Open:
    val decodedPgn = lila.common.String.decodeUriPath(pgnString).getOrElse(pgnString)
    AnalysePgnPipeline.normalizedInlinePgn(decodedPgn).fold[Fu[Result]](
      BadRequest("Empty PGN payload.").toFuccess
    ): inlinePgn =>
      renderPage(inlinePgn = Some(inlinePgn))

  def imported(id: String) = Auth { ctx ?=> me ?=>
    env.analyse.importHistory.openAnalysis(id, me.userId).flatMap:
      case Some(entry) => renderPage(inlinePgn = Some(entry.normalizedPgn), currentImportId = entry._id.some)
      case None        => notFound
  }

  def embed = Anon:
    val payload = AnalysePgnPipeline.buildPayload()
    fuccess:
      InEmbedContext:
        Ok.snip(
          views.analyse.embed.userAnalysis(
            payload.data,
            bookmaker = payload.pov.game.variant.standard || payload.pov.game.variant.chess960,
            inlinePgn = payload.inlinePgn
          )
        )

  private def renderPage(
      variant: chess.variant.Variant = chess.variant.Standard,
      fen: Option[chess.format.Fen.Full] = None,
      inlinePgn: Option[String] = None,
      currentImportId: Option[String] = None
  )(using ctx: Context): Fu[Result] =
    importHistoryJson(currentImportId).flatMap: history =>
      Ok.page(AnalysePgnPipeline.page(variant = variant, fen = fen, inlinePgn = inlinePgn, importHistory = history))

  private def importHistoryJson(currentImportId: Option[String])(using ctx: Context): Fu[Option[JsObject]] =
    ctx.me.fold(fuccess(none[JsObject])): me =>
      env.analyse.importHistory.recentSummary(me.userId).map: summary =>
        val accounts = summary.accounts.map: account =>
          Json.obj(
            "provider" -> account.provider,
            "providerLabel" -> providerLabel(account.provider),
            "username" -> account.username,
            "href" -> accountUrl(account),
            "analysisCount" -> account.analysisCount,
            "activityAt" -> account.activityAt.toString,
            "lastAnalysedAt" -> account.lastAnalysedAt.map(_.toString)
          )
        val analyses = summary.analyses.map: entry =>
          Json.obj(
            "id" -> entry._id,
            "title" -> entry.title,
            "provider" -> entry.provider,
            "providerLabel" -> entry.provider.map(providerLabel),
            "username" -> entry.username,
            "href" -> routes.UserAnalysis.imported(entry._id).url,
            "openedAt" -> entry.lastOpenedAt.toString,
            "sourceType" -> entry.sourceType,
            "result" -> entry.result,
            "speed" -> entry.speed,
            "playedAtLabel" -> entry.playedAtLabel,
            "white" -> entry.white,
            "black" -> entry.black,
            "variant" -> entry.variant,
            "opening" -> entry.opening
          )
        Json.obj(
          "currentAnalysisId" -> currentImportId,
          "recentAccounts" -> accounts,
          "recentAnalyses" -> analyses
        ).some

  private def accountUrl(account: ImportHistory.Account) =
    account.provider match
      case ImportHistory.providerChessCom => routes.Importer.importFromChessCom(account.username).url
      case _                              => routes.Importer.importFromLichess(account.username).url

  private def providerLabel(provider: String) =
    provider match
      case ImportHistory.providerChessCom => "Chess.com"
      case _                              => "Lichess"
