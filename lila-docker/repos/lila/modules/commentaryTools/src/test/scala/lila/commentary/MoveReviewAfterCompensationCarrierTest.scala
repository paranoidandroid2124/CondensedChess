package lila.commentary

import munit.FunSuite
import play.api.libs.ws.StandaloneWSClient

import scala.concurrent.{ Await, ExecutionContext }
import scala.concurrent.duration.Duration

import lila.commentary.model.strategic.VariationLine

class MoveReviewAfterCompensationCarrierTest extends FunSuite:

  given Executor = ExecutionContext.global

  private def api(): CommentaryApi =
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
        promptCacheKeyPrefix = "moveReview:test",
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

    CommentaryApi(
      openingExplorer = null,
      geminiClient = gemini,
      openAiClient = openAi,
      commentaryCache = CommentaryCache(),
      commentaryConfig = CommentaryConfig.fromEnv,
      providerConfig = AiProviderConfig.fromEnv.copy(provider = "none")
    )

  private def directMoveReview(
      fen: String,
      playedMove: String,
      before: List[VariationLine],
      afterFen: String,
      after: List[VariationLine],
      cp: Int,
      ply: Int,
      phase: String
  ): MoveReviewResult =
    Await
      .result(
        api().moveReviewPosition(
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
          allowAiPolish = false,
          lang = "en",
          planTier = PlanTier.Pro
        ),
        Duration(90, "seconds")
      )
      .getOrElse(fail(s"empty moveReview response for $fen"))

  private def assertCompensationCarrierClosed(result: MoveReviewResult): Unit =
    val digest = result.response.signalDigest.getOrElse(fail("missing signal digest"))
    assertEquals(digest.compensation, None, clue(digest))
    assertEquals(digest.compensationVectors, Nil, clue(digest))
    assertEquals(digest.investedMaterial, None, clue(digest))

  test("CAT02 moveReview path keeps unverified after-move compensation carrier closed") {
    val result =
      directMoveReview(
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

    assertCompensationCarrierClosed(result)
  }

  test("QID02 moveReview path keeps unverified after-move compensation carrier closed") {
    val result =
      directMoveReview(
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

    assertCompensationCarrierClosed(result)
  }
