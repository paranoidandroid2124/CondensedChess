package lila.llm.analysis

import munit.FunSuite

class NamedRouteNetworkBindEngineVerificationTest extends FunSuite:

  private val PositiveFen =
    "2r2rk1/pp3pp1/2n1p2p/3p4/1p1P1P2/P1P1PN1P/1P4P1/2R2RK1 w - - 0 24"

  private val ExpectedRoutePrefix =
    List(
      "a3b4",
      "a7a5",
      "b4a5",
      "c6a5",
      "f3e5"
    )

  test("fixed-depth engine verification keeps the exact B6b reroute-denial branch visible in MultiPV") {
    HeavyPieceLocalBindEngineVerifier.resolvedEnginePath() match
      case None =>
        println("Skipping named route-network engine verification: STOCKFISH_BIN / LLM_ACTIVE_CORPUS_ENGINE_PATH not set and local fallback missing.")
      case Some(enginePath) =>
        val analysis =
          HeavyPieceLocalBindEngineVerifier
            .analyze(PositiveFen)
            .getOrElse(fail(s"expected engine analysis at $enginePath"))
        val matchingLine =
          analysis.lines.find(_.moves.take(ExpectedRoutePrefix.size) == ExpectedRoutePrefix).getOrElse(
            fail(s"expected exact reroute-denial prefix in MultiPV at $enginePath: $analysis")
          )
        val replay =
          HeavyPieceLocalBindValidation
            .replayBranchLine(PositiveFen, matchingLine.moves)
            .getOrElse(fail("expected exact branch replay"))

        assert(analysis.bestMove.nonEmpty, clues(enginePath, analysis))
        assert(matchingLine.scoreCp >= 50, clues(matchingLine))
        assert(matchingLine.depth >= 12, clues(matchingLine))
        assertEquals(replay.complete, true, clues(replay))
        assertEquals(replay.replayedUci.take(ExpectedRoutePrefix.size), ExpectedRoutePrefix, clues(replay))
        assert(replay.replayedUci.contains("a7a5"), clues(replay))
        assert(replay.replayedUci.contains("b4a5"), clues(replay))
  }
