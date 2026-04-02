package lila.llm.analysis

import munit.FunSuite

class HeavyPieceLocalBindEngineVerificationTest extends FunSuite:

  private final case class EngineCase(
      id: String,
      fen: String,
      expectedTopLine: List[String],
      expectedFeatures: Set[String] = Set.empty
  )

  private val exactCases =
    List(
      EngineCase(
        id = "queen_infiltration_shell",
        fen = "2rq1rk1/pp3ppp/2n1pn2/3p4/3P4/2P1PN2/PPQ2PPP/2R2RK1 w - - 0 24",
        expectedTopLine = List("c3c4", "a7a6", "c4d5", "d8d5", "a2a3", "d5d7", "c2b3")
      ),
      EngineCase(
        id = "rook_lift_switch",
        fen = "2rq1rk1/pp3ppp/2n1pn2/3p4/3P2P1/2P1P3/PPQ2PBP/2RR2K1 w - - 0 24",
        expectedTopLine = List("h2h3", "c6a5", "c2e2", "a7a6", "b2b3", "a5c6", "c3c4", "h7h6", "c4d5", "e6d5")
      ),
      EngineCase(
        id = "perpetual_check_escape",
        fen = "r4rk1/5ppp/8/8/7q/8/2Q3P1/R5K1 b - - 0 1",
        expectedTopLine = List("a8a1", "c2b1", "a1b1"),
        expectedFeatures = Set("forcing_checks", "rook_lift")
      ),
      EngineCase(
        id = "off_sector_break_release",
        fen = "2rq1rk1/pp3ppp/2n1pn2/3p4/3P4/2P2N2/PPQ2PPP/2RR2K1 w - - 0 24",
        expectedTopLine = List("c3c4", "c8b8", "f3e5", "c6e7", "c4c5", "e7c6", "e5c6", "b7c6"),
        expectedFeatures = Set("rook_lift")
      ),
      EngineCase(
        id = "pressure_only_waiting_move",
        fen = "2rq1rk1/pp3ppp/2n1pn2/3p4/3P4/2P1P3/PPQ2PPP/2RR2K1 w - - 0 24",
        expectedTopLine = List("g2g3", "b7b5", "g1g2", "e6e5", "d1e1", "e5d4", "e3d4"),
        expectedFeatures = Set("rook_lift")
      ),
      EngineCase(
        id = "stitched_heavy_piece_bundle",
        fen = "2rq1rk1/pp3ppp/2n1pn2/3p4/3P2P1/2P1PN2/PPQ2P1P/2RR2K1 w - - 0 24",
        expectedTopLine = List("g1h1", "f6g4", "d1g1", "g4f6", "f3g5", "h7h6"),
        expectedFeatures = Set("rook_lift")
      )
    )

  test("fixed-depth engine verification reproduces exact best-path roots for heavy-piece negative fixtures") {
    HeavyPieceLocalBindEngineVerifier.resolvedEnginePath() match
      case None =>
        println("Skipping heavy-piece engine verification: STOCKFISH_BIN / LLM_ACTIVE_CORPUS_ENGINE_PATH not set and local fallback missing.")
      case Some(enginePath) =>
        exactCases.foreach { engineCase =>
          val analysis =
            HeavyPieceLocalBindEngineVerifier
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
