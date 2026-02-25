package lila.llm

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path }

import scala.concurrent.{ Await, ExecutionContext, ExecutionContextExecutor }
import scala.concurrent.duration.*
import scala.sys.process.*

import akka.actor.ActorSystem
import lila.llm.analysis.{ NarrativeUtils, OpeningExplorerClient }
import lila.llm.model.strategic.VariationLine
import munit.FunSuite
import play.api.libs.json.*
import play.api.libs.ws.ahc.StandaloneAhcWSClient
import play.shaded.ahc.org.asynchttpclient.DefaultAsyncHttpClient

class LiveWasmLlmPolishE2ETest extends FunSuite:

  override val munitTimeout: FiniteDuration = 15.minutes

  given ExecutionContextExecutor = ExecutionContext.global

  private case class Scenario(
      id: String,
      fen: String,
      phase: String,
      ply: Int,
      opening: Option[String]
  )

  private case class Outcome(
      id: String,
      sourceMode: String,
      validationReasons: List[String],
      polishReasons: List[String]
  )

  private val jsonStartMarker = "---JSON_START---"
  private val jsonEndMarker = "---JSON_END---"

  private def boolEnv(name: String): Boolean =
    sys.env
      .get(name)
      .map(_.trim.toLowerCase)
      .exists(v => v == "1" || v == "true" || v == "yes" || v == "on")

  private def runNodeWasmVariations(
      fens: List[String],
      depth: Int,
      multiPv: Int
  ): Map[String, List[VariationLine]] =
    val uniqueFens = fens.distinct.filter(_.nonEmpty)
    if uniqueFens.isEmpty then Map.empty
    else
      val scriptPath = Path.of("run_stockfish_wasm.js")
      assert(Files.exists(scriptPath), s"Missing helper script: $scriptPath")

      val nodeCheckOut = new StringBuilder()
      val nodeCheckErr = new StringBuilder()
      val nodeExit =
        Process(Seq("node", "--version"), Path.of(".").toFile).!(
          ProcessLogger(
            out => nodeCheckOut.append(out).append('\n'),
            err => nodeCheckErr.append(err).append('\n')
          )
        )
      assert(
        nodeExit == 0,
        s"Node.js is required for Stockfish WASM helper. stderr=${nodeCheckErr.toString.take(300)}"
      )

      val inputPath = Files.createTempFile("llm-live-wasm-input-", ".json")
      val payload = Json.obj(
        "fens" -> uniqueFens,
        "depth" -> depth,
        "multiPv" -> multiPv
      )
      val stdout = new StringBuilder()
      val stderr = new StringBuilder()

      try
        Files.writeString(inputPath, Json.stringify(payload), StandardCharsets.UTF_8)
        val command = Seq("node", "run_stockfish_wasm.js", "--input", inputPath.toString)
        val exit =
          Process(command, Path.of(".").toFile).!(
            ProcessLogger(
              out => stdout.append(out).append('\n'),
              err => stderr.append(err).append('\n')
            )
          )
        assert(exit == 0, s"Stockfish WASM helper failed: exit=$exit stderr=${stderr.toString.take(1500)}")

        val raw = extractJsonPayload(stdout.toString)
        val root = Json.parse(raw).as[JsObject]
        root.value.view.mapValues(parseVariationList).toMap
      finally
        Files.deleteIfExists(inputPath)

  private def extractJsonPayload(output: String): String =
    val start = output.indexOf(jsonStartMarker)
    val end = output.indexOf(jsonEndMarker)
    if start < 0 || end < 0 || end <= start then
      fail(s"Could not parse Stockfish JSON payload.\nOutput:\n${output.take(2000)}")
    output.substring(start + jsonStartMarker.length, end).trim

  private def parseVariationList(js: JsValue): List[VariationLine] =
    js.asOpt[List[JsValue]].getOrElse(Nil).flatMap { row =>
      val moves = (row \ "moves").asOpt[List[String]].getOrElse(Nil).map(_.trim).filter(_.nonEmpty)
      Option.when(moves.nonEmpty) {
        VariationLine(
          moves = moves,
          scoreCp = (row \ "scoreCp").asOpt[Int].getOrElse(0),
          mate = (row \ "mate").asOpt[Int],
          depth = (row \ "depth").asOpt[Int].getOrElse(0)
        )
      }
    }

  test("live wasm + llm polish preserves SAN/eval/variation structure") {
    if !boolEnv("LLM_LIVE_E2E") then
      assert(true) // Opt-in: set LLM_LIVE_E2E=1 to run live external-call E2E.
    else
      val provider = LlmProviderConfig.fromEnv
      val openAiConfigured = sys.env.get("OPENAI_API_KEY").exists(_.trim.nonEmpty)
      val geminiConfigured = sys.env.get("GEMINI_API_KEY").exists(_.trim.nonEmpty)

      assert(
        provider.provider == "openai" || provider.provider == "gemini",
        s"LLM_PROVIDER must be openai or gemini, got `${provider.provider}`"
      )
      if provider.provider == "openai" then
        assert(openAiConfigured, "OPENAI_API_KEY is required when LLM_PROVIDER=openai")
      if provider.provider == "gemini" then
        assert(geminiConfigured, "GEMINI_API_KEY is required when LLM_PROVIDER=gemini")

      val scenarios = List(
        Scenario(
          id = "ruy_exchange_mid",
          fen = "r1bqk2r/pppn1ppp/2p5/2b1p3/4P3/3P1N2/PPP2PPP/RNBQ1RK1 w kq - 2 1",
          phase = "middlegame",
          ply = 17,
          opening = Some("Ruy Lopez, Exchange Variation")
        ),
        Scenario(
          id = "queen_pawn_mid",
          fen = "r2q1rk1/p4ppp/1p2pn2/8/2PP4/5N2/P4PPP/R2Q1RK1 w - - 0 14",
          phase = "middlegame",
          ply = 28,
          opening = Some("Queen's Gambit Declined")
        ),
        Scenario(
          id = "rook_endgame",
          fen = "8/8/8/4k3/8/8/4P3/4K3 w - - 0 1",
          phase = "endgame",
          ply = 55,
          opening = None
        )
      )

      val varsByFen = runNodeWasmVariations(scenarios.map(_.fen), depth = 10, multiPv = 4)
      scenarios.foreach { s =>
        assert(varsByFen.get(s.fen).exists(_.nonEmpty), s"[${s.id}] No WASM MultiPV lines generated")
      }

      given ActorSystem = ActorSystem("llm-live-wasm-polish-e2e")
      val ws = new StandaloneAhcWSClient(new DefaultAsyncHttpClient())

      try
        val providerForLive = provider.copy(
          polishGateThreshold = 2.0, // force actual polish call for this live E2E
          premiumOnly = false
        )
        val api = LlmApi(
          openingExplorer = OpeningExplorerClient(ws),
          geminiClient = GeminiClient(ws, GeminiConfig.fromEnv),
          openAiClient = OpenAiClient(ws, OpenAiConfig.fromEnv),
          commentaryCache = CommentaryCache(),
          llmConfig = LlmConfig.fromEnv,
          providerConfig = providerForLive
        )

        val outcomes = scenarios.map { s =>
          val vars = varsByFen(s.fen).take(4)
          val playedMove =
            vars.flatMap(_.moves.headOption).headOption.getOrElse(fail(s"[${s.id}] Empty first-move in variations"))

          val ruleResult =
            Await
              .result(
                api.bookmakerCommentPosition(
                  fen = s.fen,
                  lastMove = Some(playedMove),
                  eval = None,
                  variations = Some(vars),
                  probeResults = None,
                  openingData = None,
                  afterFen = None,
                  afterEval = None,
                  afterVariations = None,
                  opening = s.opening,
                  phase = s.phase,
                  ply = s.ply,
                  prevStateToken = None,
                  allowLlmPolish = false,
                  lang = "en"
                ),
                60.seconds
              )
              .getOrElse(fail(s"[${s.id}] Rule response was empty"))

          val llmResult =
            Await
              .result(
                api.bookmakerCommentPosition(
                  fen = s.fen,
                  lastMove = Some(playedMove),
                  eval = None,
                  variations = Some(vars),
                  probeResults = None,
                  openingData = None,
                  afterFen = None,
                  afterEval = None,
                  afterVariations = None,
                  opening = s.opening,
                  phase = s.phase,
                  ply = s.ply,
                  prevStateToken = None,
                  allowLlmPolish = true,
                  lang = "en"
                ),
                90.seconds
              )
              .getOrElse(fail(s"[${s.id}] LLM response was empty"))

          val allowedSans =
            llmResult.response.variations.flatMap(v => NarrativeUtils.uciListToSan(s.fen, v.moves))
          val validation = PolishValidation.validatePolishedCommentary(
            polished = llmResult.response.commentary,
            original = ruleResult.response.commentary,
            allowedSans = allowedSans
          )
          val metaReasons = llmResult.response.polishMeta.map(_.validationReasons).getOrElse(Nil)

          Outcome(
            id = s.id,
            sourceMode = llmResult.response.sourceMode,
            validationReasons = validation.reasons,
            polishReasons = metaReasons
          )
        }

        val nonPolished = outcomes.filterNot(_.sourceMode == "llm_polished")
        val invalid = outcomes.filter(_.validationReasons.nonEmpty)
        val metaRejected = outcomes.filter(_.polishReasons.nonEmpty)
        val details = outcomes.map { o =>
          val v = if o.validationReasons.nonEmpty then o.validationReasons.mkString("|") else "-"
          val m = if o.polishReasons.nonEmpty then o.polishReasons.mkString("|") else "-"
          s"${o.id}:${o.sourceMode}:validation=$v:meta=$m"
        }

        assert(
          nonPolished.isEmpty,
          clues(nonPolished.map(o => s"${o.id}:${o.sourceMode}"), details)
        )
        assert(
          invalid.isEmpty,
          clues(invalid.map(o => s"${o.id}:${o.validationReasons.mkString(",")}"), details)
        )
        assert(
          metaRejected.isEmpty,
          clues(metaRejected.map(o => s"${o.id}:${o.polishReasons.mkString(",")}"), details)
        )
      finally
        ws.close()
        summon[ActorSystem].terminate()
  }
