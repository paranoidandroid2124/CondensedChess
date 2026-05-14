package lila.commentary.analysis

import munit.FunSuite

class ExactBoardEngineProofTest extends FunSuite:

  private final case class HeavyPieceCase(
      id: String,
      fen: String,
      expectedTopLine: List[String],
      expectedFeatures: Set[String] = Set.empty
  )

  private val heavyPieceCases =
    List(
      HeavyPieceCase(
        id = "queen_infiltration_shell",
        fen = "2rq1rk1/pp3ppp/2n1pn2/3p4/3P4/2P1PN2/PPQ2PPP/2R2RK1 w - - 0 24",
        expectedTopLine = List("c3c4", "a7a6", "c4d5", "d8d5", "a2a3", "d5d7", "c2b3")
      ),
      HeavyPieceCase(
        id = "rook_lift_switch",
        fen = "2rq1rk1/pp3ppp/2n1pn2/3p4/3P2P1/2P1P3/PPQ2PBP/2RR2K1 w - - 0 24",
        expectedTopLine = List("h2h3", "c6a5", "c2e2", "a7a6", "b2b3", "a5c6", "c3c4", "h7h6", "c4d5", "e6d5")
      ),
      HeavyPieceCase(
        id = "perpetual_check_escape",
        fen = "r4rk1/5ppp/8/8/7q/8/2Q3P1/R5K1 b - - 0 1",
        expectedTopLine = List("a8a1", "c2b1", "a1b1"),
        expectedFeatures = Set("forcing_checks", "rook_lift")
      ),
      HeavyPieceCase(
        id = "off_sector_break_release",
        fen = "2rq1rk1/pp3ppp/2n1pn2/3p4/3P4/2P2N2/PPQ2PPP/2RR2K1 w - - 0 24",
        expectedTopLine = List("c3c4", "c8b8", "f3e5", "c6e7", "c4c5", "e7c6", "e5c6", "b7c6"),
        expectedFeatures = Set("rook_lift")
      ),
      HeavyPieceCase(
        id = "pressure_only_waiting_move",
        fen = "2rq1rk1/pp3ppp/2n1pn2/3p4/3P4/2P1P3/PPQ2PPP/2RR2K1 w - - 0 24",
        expectedTopLine = List("g2g3", "b7b5", "g1g2", "e6e5", "d1e1", "e5d4", "e3d4"),
        expectedFeatures = Set("rook_lift")
      ),
      HeavyPieceCase(
        id = "stitched_heavy_piece_bundle",
        fen = "2rq1rk1/pp3ppp/2n1pn2/3p4/3P2P1/2P1PN2/PPQ2P1P/2RR2K1 w - - 0 24",
        expectedTopLine = List("g1h1", "f6g4", "d1g1", "g4f6", "f3g5", "h7h6"),
        expectedFeatures = Set("rook_lift")
      )
    )

  private val RouteFen =
    "2r2rk1/pp3pp1/2n1p2p/3p4/1p1P1P2/P1P1PN1P/1P4P1/2R2RK1 w - - 0 24"

  test("fixed-depth engine verification reproduces exact best-path roots for heavy-piece fixtures") {
    ExactBoardEngineVerifier.resolvedEnginePath() match
      case None =>
        println("Skipping exact-board engine verification: STOCKFISH_BIN / AI_ACTIVE_CORPUS_ENGINE_PATH not set and local fallback missing.")
      case Some(enginePath) =>
        heavyPieceCases.foreach { engineCase =>
          val analysis =
            ExactBoardEngineVerifier
              .analyze(engineCase.fen)
              .getOrElse(fail(s"expected engine analysis for ${engineCase.id} at $enginePath"))
          val topLine =
            analysis.lines.headOption.getOrElse(fail(s"expected top PV for ${engineCase.id}"))
          val replay =
            HeavyPieceLocalBindValidation
              .replayBranchLine(engineCase.fen, topLine.moves)
              .getOrElse(fail(s"expected top-line replay for ${engineCase.id}"))

          assertEquals(analysis.bestMove, engineCase.expectedTopLine.headOption, clues(engineCase.id, analysis))
          assertEquals(topLine.moves, engineCase.expectedTopLine, clues(engineCase.id, analysis))
          assert(topLine.depth >= 12, clues(engineCase.id, analysis))
          assertEquals(replay.complete, true, clues(engineCase.id, topLine, replay))
          assertEquals(replay.features.toSet, engineCase.expectedFeatures, clues(engineCase.id, topLine, replay))
        }
  }

  test("route-chain candidate fails root-best while the exact a5 intermediate survives after the played move") {
    val playedMove = "a3b4"
    val expectedDefensePrefix = List("a7a5", "b4a5", "c6a5", "f3e5")

    ExactBoardEngineVerifier.resolvedEnginePath() match
      case None =>
        println("Skipping route-chain engine verification: STOCKFISH_BIN / AI_ACTIVE_CORPUS_ENGINE_PATH not set and local fallback missing.")
      case Some(enginePath) =>
        val rootAnalysis =
          ExactBoardEngineVerifier
            .analyze(RouteFen, depth = 18, multiPv = 5)
            .getOrElse(fail(s"expected root analysis at $enginePath"))
        val analysis =
          ExactBoardEngineVerifier
            .analyze(RouteFen, depth = 18, multiPv = 5, moves = List(playedMove))
            .getOrElse(fail(s"expected engine analysis at $enginePath"))
        val matchingLine =
          analysis.lines.headOption
            .filter(line => line.moves.take(expectedDefensePrefix.size) == expectedDefensePrefix)
            .getOrElse(fail(s"expected exact route-chain best-defense prefix in MultiPV at $enginePath: $analysis"))
        val replayLine = playedMove :: matchingLine.moves
        val replay =
          HeavyPieceLocalBindValidation
            .replayBranchLine(RouteFen, replayLine)
            .getOrElse(fail("expected exact branch replay"))
        val expectedReplayPrefix = playedMove :: expectedDefensePrefix

        assert(rootAnalysis.bestMove.nonEmpty, clues(enginePath, rootAnalysis))
        assertEquals(rootAnalysis.bestMove.contains(playedMove), false, clues(enginePath, rootAnalysis))
        assertEquals(analysis.bestMove, matchingLine.moves.headOption, clues(enginePath, analysis))
        assert(matchingLine.depth >= 16, clues(matchingLine))
        assertEquals(replay.complete, true, clues(replay))
        assertEquals(replay.replayedUci.take(expectedReplayPrefix.size), expectedReplayPrefix, clues(replay))
        assert(replay.replayedUci.contains("f3e5"), clues(replay))
        assert(replay.replayedUci.contains("a5c4"), clues(replay))
  }

  test("route-network exact B6b reroute-denial branch remains visible in MultiPV") {
    val expectedRoutePrefix = List("a3b4", "a7a5", "b4a5", "c6a5", "f3e5")

    ExactBoardEngineVerifier.resolvedEnginePath() match
      case None =>
        println("Skipping route-network engine verification: STOCKFISH_BIN / AI_ACTIVE_CORPUS_ENGINE_PATH not set and local fallback missing.")
      case Some(enginePath) =>
        val analysis =
          ExactBoardEngineVerifier
            .analyze(RouteFen)
            .getOrElse(fail(s"expected engine analysis at $enginePath"))
        val matchingLine =
          analysis.lines.find(_.moves.take(expectedRoutePrefix.size) == expectedRoutePrefix).getOrElse(
            fail(s"expected exact reroute-denial prefix in MultiPV at $enginePath: $analysis")
          )
        val replay =
          HeavyPieceLocalBindValidation
            .replayBranchLine(RouteFen, matchingLine.moves)
            .getOrElse(fail("expected exact branch replay"))

        assert(analysis.bestMove.nonEmpty, clues(enginePath, analysis))
        assert(matchingLine.scoreCp >= 50, clues(matchingLine))
        assert(matchingLine.depth >= 12, clues(matchingLine))
        assertEquals(replay.complete, true, clues(replay))
        assertEquals(replay.replayedUci.take(expectedRoutePrefix.size), expectedRoutePrefix, clues(replay))
        assert(replay.replayedUci.contains("a7a5"), clues(replay))
        assert(replay.replayedUci.contains("b4a5"), clues(replay))
  }
