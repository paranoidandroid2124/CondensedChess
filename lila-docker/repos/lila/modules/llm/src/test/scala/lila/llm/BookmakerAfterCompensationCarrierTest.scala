package lila.llm

import munit.FunSuite
import play.api.libs.ws.StandaloneWSClient

import scala.concurrent.{ Await, ExecutionContext }
import scala.concurrent.duration.Duration

import lila.llm.model.strategic.VariationLine

class BookmakerAfterCompensationCarrierTest extends FunSuite:

  given Executor = ExecutionContext.global

  private def api(): LlmApi =
    val openAi = OpenAiClient(
      ws = null.asInstanceOf[StandaloneWSClient],
      config = OpenAiConfig(
        apiKey = "",
        endpoint = "https://api.openai.com/v1/chat/completions",
        modelSync = "gpt-5-mini",
        modelFallback = "gpt-4.1-mini",
        modelAsync = "gpt-5-mini",
        modelProSync = "gpt-5-mini",
        modelProFallback = "gpt-5-mini",
        modelProAsync = "gpt-5-mini",
        modelActiveSync = "gpt-5-mini",
        modelActiveFallback = "gpt-4.1-mini",
        modelActiveAsync = "gpt-5-mini",
        reasoningEffortActive = "none",
        promptCacheKeyPrefix = "bookmaker:test",
        enabled = false,
        temperature = 0.2,
        maxOutputTokens = 256,
        requestTimeoutSeconds = 30
      )
    )
    val gemini = GeminiClient(
      ws = null.asInstanceOf[StandaloneWSClient],
      config = GeminiConfig(
        apiKey = "",
        model = "gemini-2.5-flash-latest",
        modelActive = "gemini-3-flash-preview",
        enabled = false,
        temperature = 0.4,
        maxOutputTokens = 256,
        contextCacheTtlMinutes = 60,
        requestTimeoutSeconds = 30
      )
    )

    LlmApi(
      openingExplorer = null,
      geminiClient = gemini,
      openAiClient = openAi,
      commentaryCache = CommentaryCache(),
      llmConfig = LlmConfig.fromEnv,
      providerConfig = LlmProviderConfig.fromEnv.copy(provider = "none")
    )

  private def directBookmaker(
      fen: String,
      playedMove: String,
      before: List[VariationLine],
      afterFen: String,
      after: List[VariationLine],
      cp: Int,
      ply: Int,
      phase: String
  ): BookmakerResult =
    Await
      .result(
        api().bookmakerCommentPosition(
          fen = fen,
          lastMove = Some(playedMove),
          eval = Some(EvalData(cp = cp, mate = None, pv = Some(before.head.moves))),
          variations = Some(before),
          probeResults = None,
          openingData = None,
          afterFen = Some(afterFen),
          afterEval = Some(EvalData(cp = cp, mate = None, pv = Some(after.head.moves))),
          afterVariations = Some(after),
          opening = None,
          phase = phase,
          ply = ply,
          prevStateToken = None,
          prevEndgameStateToken = None,
          allowLlmPolish = false,
          lang = "en",
          planTier = PlanTier.Pro,
          llmLevel = LlmLevel.Active
        ),
        Duration(90, "seconds")
      )
      .getOrElse(fail(s"empty bookmaker response for $fen"))

  private def assertCompensationCarrier(result: BookmakerResult): Unit =
    val digest = result.response.signalDigest.getOrElse(fail("missing signal digest"))
    assert(digest.compensation.exists(_.trim.nonEmpty), clue(digest))
    assert(digest.compensationVectors.exists(_.trim.nonEmpty), clue(digest))
    assert(digest.investedMaterial.exists(_ > 0), clue(digest))
    val commentary = Option(result.response.commentary).getOrElse("").toLowerCase
    assert(
      commentary.contains("compensation") || commentary.contains("initiative"),
      clue(result.response.commentary)
    )

  test("CAT02 bookmaker path keeps after-move compensation carrier alive") {
    val result =
      directBookmaker(
        fen = "r3kb1r/2R2p1p/4p1p1/p2qP3/3p4/P4PP1/1P1Q2KP/R7 w kq - 0 23",
        playedMove = "d2f4",
        before = List(
          VariationLine(
            moves = List("d2f4", "f8e7", "c7e7", "e8e7", "f4f6"),
            scoreCp = -228,
            depth = 10
          ),
          VariationLine(
            moves = List("d2c2", "f8e7", "c2a4", "e8f8"),
            scoreCp = -316,
            depth = 10
          )
        ),
        afterFen = "r3kb1r/2R2p1p/4p1p1/p2qP3/3p1Q2/P4PP1/1P4KP/R7 b kq - 1 23",
        after = List(
          VariationLine(
            moves = List("d4d3"),
            scoreCp = -228,
            depth = 10
          )
        ),
        cp = -228,
        ply = 45,
        phase = "endgame"
      )

    assertCompensationCarrier(result)
  }

  test("QID02 bookmaker path keeps after-move compensation carrier alive") {
    val result =
      directBookmaker(
        fen = "5rk1/p1r3p1/1p2p2p/1q1nP3/2p1Q2P/P1P3P1/1P2RPK1/2B4R b - - 2 26",
        playedMove = "c7f7",
        before = List(
          VariationLine(
            moves = List("c7f7"),
            scoreCp = -61,
            depth = 10
          ),
          VariationLine(
            moves = List("c7d7", "e2d2", "b5c6"),
            scoreCp = -11,
            depth = 10
          )
        ),
        afterFen = "5rk1/p4rp1/1p2p2p/1q1nP3/2p1Q2P/P1P3P1/1P2RPK1/2B4R w - - 3 27",
        after = List(
          VariationLine(
            moves = List("b2b3"),
            scoreCp = -61,
            depth = 10
          )
        ),
        cp = -61,
        ply = 52,
        phase = "endgame"
      )

    assertCompensationCarrier(result)
  }
