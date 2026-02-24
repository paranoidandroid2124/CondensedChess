package lila.llm.tools

import scala.concurrent.{ Await, ExecutionContext }

import akka.actor.ActorSystem

import play.api.libs.ws.ahc.StandaloneAhcWSClient
import play.shaded.ahc.org.asynchttpclient.DefaultAsyncHttpClient

import lila.llm.*
import lila.llm.analysis.OpeningExplorerClient
import lila.llm.model.OpeningReference
import lila.llm.model.strategic.VariationLine
import play.api.libs.json.{ Json, Writes }

/** Audit helper to inspect per-ply bookmaker commentary and rough token risk.
  *
  * Usage:
  *   sbt "llm/runMain lila.llm.tools.PlyCommentaryAudit"
  *   sbt "llm/runMain lila.llm.tools.PlyCommentaryAudit \"1. d4 Nf6 2. c4 e6 3. Nf3 d5 4. g3 *\""
  *   sbt "llm/runMain lila.llm.tools.PlyCommentaryAudit --opening \"Sicilian Defense\" \"1. e4 c5 2. Nf3 d6 3. d4 cxd4 *\""
  *   sbt "llm/runMain lila.llm.tools.PlyCommentaryAudit --allow-llm --opening \"Catalan Opening\" \"1. d4 Nf6 2. c4 e6 3. Nf3 d5 4. g3 *\""
  */
object PlyCommentaryAudit:

  private case class CliArgs(
      allowLlm: Boolean,
      openingName: String,
      pgn: String,
      openAiModelOverride: Option[String],
      openAiFallbackOverride: Option[String]
  )

  private case class AuditRow(
      ply: Int,
      san: String,
      uci: String,
      fenBefore: String,
      variationUci: List[String],
      commentary: String,
      chars: Int,
      words: Int,
      estTokens: Int,
      over160: Boolean,
      sourceMode: String,
      model: Option[String],
      cacheHit: Boolean
  )

  private object AuditRow:
    given Writes[AuditRow] = Json.writes[AuditRow]

  def main(args: Array[String]): Unit =
    given Executor = ExecutionContext.global
    given ActorSystem = ActorSystem("ply-commentary-audit")

    val cli = parseArgs(args.toList)
    val allowLlm = cli.allowLlm
    val openingName = cli.openingName
    val rawPgnArg = cli.pgn
    val userPgn =
      if rawPgnArg.nonEmpty then rawPgnArg
      else "1. d4 Nf6 2. c4 e6 3. Nf3 d5 4. g3 *"
    val fallbackPgn = "1. d4 Nf6 2. c4 e6 3. Nf3 d5 4. g3 *"

    val (pgnUsed, plyData) = PgnAnalysisHelper.extractPlyData(userPgn) match
      case Right(rows) =>
        println(s"[audit] parsed input PGN successfully (${rows.size} plies)")
        userPgn -> rows
      case Left(err) =>
        println(s"[audit] input PGN parse failed: $err")
        println(s"[audit] falling back to legal Catalan setup: $fallbackPgn")
        PgnAnalysisHelper.extractPlyData(fallbackPgn) match
          case Right(rows) => fallbackPgn -> rows
          case Left(err2) =>
            System.err.println(s"[audit] fallback PGN parse also failed: $err2")
            sys.exit(2)

    println(s"[audit] using PGN: $pgnUsed")
    println(s"[audit] opening label: $openingName")
    println(s"[audit] allowLlmPolish: $allowLlm")

    val ws = new StandaloneAhcWSClient(new DefaultAsyncHttpClient())
    try
      val explorer = OpeningExplorerClient(ws)
      val geminiConfig =
        if allowLlm then GeminiConfig.fromEnv
        else GeminiConfig(
          apiKey = "",
          model = "disabled",
          enabled = false,
          temperature = 0.4,
          maxOutputTokens = 256,
          contextCacheTtlMinutes = 60,
          requestTimeoutSeconds = 30
        )
      val geminiClient = GeminiClient(ws, geminiConfig)
      val openAiConfig =
        if allowLlm then
          val baseOpenAi = OpenAiConfig.fromEnv
          baseOpenAi.copy(
            modelSync = cli.openAiModelOverride.getOrElse(baseOpenAi.modelSync),
            modelFallback = cli.openAiFallbackOverride.getOrElse(baseOpenAi.modelFallback)
          )
        else OpenAiConfig(
          apiKey = "",
          endpoint = "https://api.openai.com/v1/chat/completions",
          modelSync = "disabled",
          modelFallback = "disabled",
          modelAsync = "disabled",
          promptCacheKeyPrefix = "bookmaker:polish:v1",
          enabled = false,
          temperature = 0.2,
          maxOutputTokens = 256,
          requestTimeoutSeconds = 30
        )
      val openAiClient = OpenAiClient(ws, openAiConfig)
      val cache = CommentaryCache()
      val providerConfig =
        if allowLlm then LlmProviderConfig.fromEnv
        else LlmProviderConfig(
          provider = "none",
          promptVersion = "v1",
          polishGateThreshold = 0.90,
          premiumOnly = true,
          defaultLang = "en"
        )
      val api = LlmApi(
        openingExplorer = explorer,
        geminiClient = geminiClient,
        openAiClient = openAiClient,
        commentaryCache = cache,
        llmConfig = LlmConfig.fromEnv,
        providerConfig = providerConfig
      )
      println(s"[audit] provider=${providerConfig.provider}, openaiEnabled=${openAiConfig.enabled}, geminiEnabled=${geminiConfig.enabled}")
      val fallbackModel =
        if openAiConfig.modelFallback.trim.nonEmpty then openAiConfig.modelFallback.trim else "(disabled)"
      println(s"[audit] openai.syncModel=${openAiConfig.modelSync}, openai.fallbackModel=$fallbackModel")
      if allowLlm && !openAiConfig.enabled && !geminiConfig.enabled then
        println("[audit] --allow-llm is on, but no provider API key is configured. Falling back to rule-only responses.")
      if allowLlm && providerConfig.provider == "none" then
        println("[audit] --allow-llm is on, but LLM_PROVIDER=none. Set LLM_PROVIDER=openai (or gemini) to enable polishing.")

      // Pass a minimal opening reference to bypass explorer network dependence in this audit.
      val openingRef = OpeningReference(
        eco = Some("E00"),
        name = Some(openingName),
        totalGames = 0,
        topMoves = Nil,
        sampleGames = Nil
      )

      var prevStateToken: Option[lila.llm.analysis.PlanStateTracker] = None
      val rows = scala.collection.mutable.ListBuffer.empty[AuditRow]

      plyData.foreach { pd =>
        val tailMoves = plyData.dropWhile(_.ply < pd.ply).map(_.playedUci).take(6)
        val vars = List(
          VariationLine(
            moves = tailMoves,
            scoreCp = 0,
            mate = None,
            depth = 18
          )
        )

        val resOpt = Await.result(
          api.bookmakerCommentPosition(
            fen = pd.fen,
            lastMove = Some(pd.playedUci),
            eval = None,
            variations = Some(vars),
            probeResults = None,
            openingData = Some(openingRef),
            afterFen = None,
            afterEval = None,
            afterVariations = None,
            opening = Some(openingName),
            phase = "opening",
            ply = pd.ply,
            prevStateToken = prevStateToken,
            allowLlmPolish = allowLlm,
            lang = "en"
          ),
          20.seconds
        )

        val result = resOpt.getOrElse {
          BookmakerResult(
            response = CommentResponse(
              commentary = "(no commentary)",
              concepts = Nil,
              variations = Nil
            ),
            cacheHit = false
          )
        }
        val response = result.response
        prevStateToken = response.planStateToken

        val commentary = response.commentary.trim
        val words = commentary.split("\\s+").count(_.nonEmpty)
        val chars = commentary.length
        val estTokens = math.ceil(chars.toDouble / 4.0).toInt

        rows += AuditRow(
          ply = pd.ply,
          san = pd.playedMove,
          uci = pd.playedUci,
          fenBefore = pd.fen,
          variationUci = tailMoves,
          commentary = commentary,
          chars = chars,
          words = words,
          estTokens = estTokens,
          over160 = estTokens > 160,
          sourceMode = response.sourceMode,
          model = response.model,
          cacheHit = result.cacheHit
        )
      }

      println()
      println("=== PLY COMMENTARY AUDIT ===")
      rows.foreach { r =>
        println()
        println(s"[ply ${r.ply}] ${r.san} (${r.uci})")
        println(s"chars=${r.chars}, words=${r.words}, estTokens=${r.estTokens}, over160=${r.over160}, sourceMode=${r.sourceMode}, model=${r.model.getOrElse("-")}, cacheHit=${r.cacheHit}")
        println(r.commentary)
      }

      val summary = Json.obj(
        "pgnUsed" -> pgnUsed,
        "opening" -> openingName,
        "allowLlmPolish" -> allowLlm,
        "provider" -> providerConfig.provider,
        "rows" -> rows.toList,
        "maxEstTokens" -> rows.map(_.estTokens).maxOption.getOrElse(0),
        "avgEstTokens" ->
          (if rows.isEmpty then 0.0 else rows.map(_.estTokens).sum.toDouble / rows.size.toDouble),
        "over160Count" -> rows.count(_.over160),
        "llmCount" -> rows.count(_.sourceMode.startsWith("llm")),
        "fallbackRuleCount" -> rows.count(_.sourceMode.startsWith("fallback_rule")),
        "cacheHitCount" -> rows.count(_.cacheHit),
        "plyCount" -> rows.size
      )
      println()
      println("=== JSON SUMMARY ===")
      println(Json.prettyPrint(summary))
    finally
      ws.close()
      summon[ActorSystem].terminate()

  private def parseArgs(args: List[String]): CliArgs =
    def normalizeModelOverride(value: String): Option[String] =
      Option(value).map(_.trim).filter(_.nonEmpty)

    def normalizeFallbackOverride(value: String): Option[String] =
      Option(value).map(_.trim).filter(_.nonEmpty).map { v =>
        v.toLowerCase match
          case "none" | "off" | "disabled" | "-" => ""
          case _                                  => v
      }

    @annotation.tailrec
    def loop(
        rest: List[String],
        allowLlm: Boolean,
        opening: Option[String],
        pgnParts: List[String],
        modelOverride: Option[String],
        fallbackOverride: Option[String]
    ): CliArgs =
      rest match
        case "--allow-llm" :: tail =>
          loop(
            tail,
            allowLlm = true,
            opening = opening,
            pgnParts = pgnParts,
            modelOverride = modelOverride,
            fallbackOverride = fallbackOverride
          )
        case "--opening" :: value :: tail =>
          loop(
            tail,
            allowLlm = allowLlm,
            opening = Some(value.trim).filter(_.nonEmpty),
            pgnParts = pgnParts,
            modelOverride = modelOverride,
            fallbackOverride = fallbackOverride
          )
        case "--model" :: value :: tail =>
          loop(
            tail,
            allowLlm = allowLlm,
            opening = opening,
            pgnParts = pgnParts,
            modelOverride = normalizeModelOverride(value),
            fallbackOverride = fallbackOverride
          )
        case "--fallback-model" :: value :: tail =>
          loop(
            tail,
            allowLlm = allowLlm,
            opening = opening,
            pgnParts = pgnParts,
            modelOverride = modelOverride,
            fallbackOverride = normalizeFallbackOverride(value)
          )
        case token :: tail =>
          loop(
            tail,
            allowLlm = allowLlm,
            opening = opening,
            pgnParts = pgnParts :+ token,
            modelOverride = modelOverride,
            fallbackOverride = fallbackOverride
          )
        case Nil =>
          val openingName = opening.getOrElse("Catalan Opening")
          CliArgs(
            allowLlm = allowLlm,
            openingName = openingName,
            pgn = pgnParts.mkString(" ").trim,
            openAiModelOverride = modelOverride,
            openAiFallbackOverride = fallbackOverride
          )

    loop(
      args,
      allowLlm = false,
      opening = None,
      pgnParts = Nil,
      modelOverride = None,
      fallbackOverride = None
    )
