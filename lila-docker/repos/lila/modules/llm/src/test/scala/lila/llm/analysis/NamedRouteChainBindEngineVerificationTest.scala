package lila.llm.analysis

import munit.FunSuite

class NamedRouteChainBindEngineVerificationTest extends FunSuite:

  private val PositiveFen =
    "2r2rk1/pp3pp1/2n1p2p/3p4/1p1P1P2/P1P1PN1P/1P4P1/2R2RK1 w - - 0 24"

  private val PlayedMove = "a3b4"

  private val ExpectedDefensePrefix =
    List(
      "a7a5",
      "b4a5",
      "c6a5",
      "f3e5"
    )

  test("route-chain candidate fails the root-best gate even though the exact a5 intermediate survives after the played move") {
    HeavyPieceLocalBindEngineVerifier.resolvedEnginePath() match
      case None =>
        println("Skipping named route-chain engine verification: STOCKFISH_BIN / LLM_ACTIVE_CORPUS_ENGINE_PATH not set and local fallback missing.")
      case Some(enginePath) =>
        val rootAnalysis =
          HeavyPieceLocalBindEngineVerifier
            .analyze(PositiveFen, depth = 18, multiPv = 5)
            .getOrElse(fail(s"expected root analysis at $enginePath"))
        val analysis =
          HeavyPieceLocalBindEngineVerifier
            .analyze(PositiveFen, depth = 18, multiPv = 5, moves = List(PlayedMove))
            .getOrElse(fail(s"expected engine analysis at $enginePath"))
        val matchingLine =
          analysis.lines.headOption
            .filter(line => line.moves.take(ExpectedDefensePrefix.size) == ExpectedDefensePrefix)
            .getOrElse(
              fail(s"expected exact route-chain best-defense prefix in MultiPV at $enginePath: $analysis")
            )
        val replayLine = PlayedMove :: matchingLine.moves
        val replay =
          HeavyPieceLocalBindValidation
            .replayBranchLine(PositiveFen, replayLine)
            .getOrElse(fail("expected exact branch replay"))
        val expectedReplayPrefix = PlayedMove :: ExpectedDefensePrefix

        assert(rootAnalysis.bestMove.nonEmpty, clues(enginePath, rootAnalysis))
        assertEquals(rootAnalysis.bestMove.contains(PlayedMove), false, clues(enginePath, rootAnalysis))
        assertEquals(analysis.bestMove, matchingLine.moves.headOption, clues(enginePath, analysis))
        assert(matchingLine.depth >= 16, clues(matchingLine))
        assertEquals(replay.complete, true, clues(replay))
        assertEquals(replay.replayedUci.take(expectedReplayPrefix.size), expectedReplayPrefix, clues(replay))
        assert(replay.replayedUci.contains("f3e5"), clues(replay))
        assert(replay.replayedUci.contains("a5c4"), clues(replay))
  }
