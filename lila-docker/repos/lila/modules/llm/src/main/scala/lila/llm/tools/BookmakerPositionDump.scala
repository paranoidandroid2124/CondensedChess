package lila.llm.tools

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path }

import scala.concurrent.{ Await, ExecutionContext }
import scala.sys.process.*

import akka.actor.ActorSystem
import lila.llm.*
import lila.llm.analysis.{ NarrativeUtils, OpeningExplorerClient }
import lila.llm.model.strategic.VariationLine
import play.api.libs.json.*
import play.api.libs.ws.ahc.StandaloneAhcWSClient
import play.shaded.ahc.org.asynchttpclient.DefaultAsyncHttpClient

/** Dumps one live position response (rule vs llm) as JSON.
  *
  * Usage:
  *   sbt "llm/runMain lila.llm.tools.BookmakerPositionDump"
  */
object BookmakerPositionDump:

  private val jsonStartMarker = "---JSON_START---"
  private val jsonEndMarker = "---JSON_END---"

  def main(args: Array[String]): Unit =
    given Executor = ExecutionContext.global
    given ActorSystem = ActorSystem("bookmaker-position-dump")

    val fen = "r2q1rk1/p4ppp/1p2pn2/8/2PP4/5N2/P4PPP/R2Q1RK1 w - - 0 14"
    val phase = "middlegame"
    val ply = 28
    val opening = Some("Queen's Gambit Declined")
    val depth = 12
    val multiPv = 4

    val ws = new StandaloneAhcWSClient(new DefaultAsyncHttpClient())
    try
      val vars = runNodeWasmVariations(fen = fen, depth = depth, multiPv = multiPv)
      if vars.isEmpty then
        System.err.println("[dump] no WASM variations produced")
        sys.exit(2)

      val playedMove = vars.flatMap(_.moves.headOption).headOption.getOrElse {
        System.err.println("[dump] no played move in WASM variations")
        sys.exit(2)
      }

      val api = mkApi(ws, LlmProviderConfig.fromEnv.copy(provider = "none"))
      val llmApi = mkApi(ws, LlmProviderConfig.fromEnv.copy(polishGateThreshold = 2.0, premiumOnly = false))

      val rule = Await.result(
        api.bookmakerCommentPosition(
          fen = fen,
          lastMove = Some(playedMove),
          eval = None,
          variations = Some(vars),
          probeResults = None,
          openingData = None,
          afterFen = None,
          afterEval = None,
          afterVariations = None,
          opening = opening,
          phase = phase,
          ply = ply,
          prevStateToken = None,
          allowLlmPolish = false,
          lang = "en"
        ),
        90.seconds
      ).getOrElse {
        System.err.println("[dump] empty rule response")
        sys.exit(2)
      }

      val llm = Await.result(
        llmApi.bookmakerCommentPosition(
          fen = fen,
          lastMove = Some(playedMove),
          eval = None,
          variations = Some(vars),
          probeResults = None,
          openingData = None,
          afterFen = None,
          afterEval = None,
          afterVariations = None,
          opening = opening,
          phase = phase,
          ply = ply,
          prevStateToken = None,
          allowLlmPolish = true,
          lang = "en"
        ),
        120.seconds
      ).getOrElse {
        System.err.println("[dump] empty llm response")
        sys.exit(2)
      }

      val allowedSans =
        llm.response.variations.flatMap(v => NarrativeUtils.uciListToSan(fen, v.moves))
      val validation = PolishValidation.validatePolishedCommentary(
        polished = llm.response.commentary,
        original = rule.response.commentary,
        allowedSans = allowedSans
      )

      val payload = Json.obj(
        "position" -> Json.obj(
          "fen" -> fen,
          "phase" -> phase,
          "ply" -> ply,
          "opening" -> opening
        ),
        "wasm" -> Json.obj(
          "depth" -> depth,
          "multiPv" -> multiPv,
          "playedMoveUci" -> playedMove,
          "variationCount" -> vars.size,
          "variations" -> Json.toJson(vars)
        ),
        "ruleResponse" -> Json.toJson(rule.response),
        "llmResponse" -> Json.toJson(llm.response),
        "polishValidation" -> Json.obj(
          "isValid" -> validation.isValid,
          "reasons" -> validation.reasons
        )
      )

      println(Json.prettyPrint(payload))
    finally
      ws.close()
      summon[ActorSystem].terminate()

  private def mkApi(ws: StandaloneAhcWSClient, providerConfig: LlmProviderConfig)(using Executor): LlmApi =
    LlmApi(
      openingExplorer = OpeningExplorerClient(ws),
      geminiClient = GeminiClient(ws, GeminiConfig.fromEnv),
      openAiClient = OpenAiClient(ws, OpenAiConfig.fromEnv),
      commentaryCache = CommentaryCache(),
      llmConfig = LlmConfig.fromEnv,
      providerConfig = providerConfig
    )

  private def runNodeWasmVariations(
      fen: String,
      depth: Int,
      multiPv: Int
  ): List[VariationLine] =
    val scriptPath = Path.of("run_stockfish_wasm.js")
    if !Files.exists(scriptPath) then
      System.err.println(s"[dump] missing helper script: $scriptPath")
      sys.exit(2)

    val inputPath = Files.createTempFile("bookmaker-position-dump-", ".json")
    val stdout = new StringBuilder()
    val stderr = new StringBuilder()
    val payload = Json.obj(
      "fens" -> List(fen),
      "depth" -> depth,
      "multiPv" -> multiPv
    )

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
      if exit != 0 then
        System.err.println(s"[dump] WASM helper failed: exit=$exit stderr=${stderr.toString.take(1000)}")
        sys.exit(2)

      val raw = extractJsonPayload(stdout.toString)
      val root = Json.parse(raw).as[JsObject]
      root.value.headOption match
        case Some((_, js)) => parseVariationList(js)
        case None          => Nil
    finally
      Files.deleteIfExists(inputPath)

  private def extractJsonPayload(output: String): String =
    val start = output.indexOf(jsonStartMarker)
    val end = output.indexOf(jsonEndMarker)
    if start < 0 || end < 0 || end <= start then
      System.err.println(s"[dump] cannot parse wasm JSON payload:\n${output.take(1500)}")
      sys.exit(2)
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
