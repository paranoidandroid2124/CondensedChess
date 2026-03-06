package lila.llm.analysis

import chess.Color
import lila.llm.analysis.strategic.{ EndgameAnalyzerImpl, EndgamePatternOracle }
import lila.llm.model.strategic.{ RookEndgamePattern, TheoreticalOutcomeHint }
import munit.FunSuite

class EndgamePatternOracleRegressionTest extends FunSuite:

  private val analyzer = new EndgameAnalyzerImpl()

  private def evaluate(fen: String, color: Color) =
    val board = chess.format.Fen.read(chess.variant.Standard, chess.format.Fen.Full(fen)).get.board
    val core = analyzer.analyze(board, color).get
    EndgamePatternOracle
      .detect(board = board, color = color, coreFeature = core)
      .map(EndgamePatternOracle.applyPattern(core, _))
      .getOrElse(core)

  test("TarraschDefenseActive requires rook behind the pawn by rank") {
    val behindFen = "7k/8/R2PK3/8/8/8/8/3r4 w - - 0 1"
    val frontFen = "7k/3r4/R2PK3/8/8/8/8/8 w - - 0 1"

    val behind = evaluate(behindFen, Color.White)
    val front = evaluate(frontFen, Color.White)

    assertEquals(behind.primaryPattern, Some("TarraschDefenseActive"))
    assertEquals(behind.rookEndgamePattern, RookEndgamePattern.TarraschDefenseActive)
    assertNotEquals(front.primaryPattern, Some("TarraschDefenseActive"))
  }

  test("PassiveRookDefense can be detected with opponent rook present (R+P vs R)") {
    val fen = "6k1/8/8/8/8/3p4/3r2K1/R7 b - - 0 1"
    val feature = evaluate(fen, Color.Black)

    assertEquals(feature.primaryPattern, Some("PassiveRookDefense"))
    assertEquals(feature.rookEndgamePattern, RookEndgamePattern.PassiveRookDefense)
  }

  test("QueenVsAdvancedPawn uses attacking-king distance for win/draw split") {
    val drawFen = "8/PqK5/8/8/8/8/8/7k w - - 0 1"
    val winFen = "8/PqK5/k7/8/8/8/8/8 w - - 0 1"

    val drawCase = evaluate(drawFen, Color.White)
    val winCase = evaluate(winFen, Color.White)

    assertEquals(drawCase.primaryPattern, Some("QueenVsAdvancedPawn"))
    assertEquals(drawCase.theoreticalOutcomeHint, TheoreticalOutcomeHint.Draw)
    assertEquals(winCase.primaryPattern, Some("QueenVsAdvancedPawn"))
    assertEquals(winCase.theoreticalOutcomeHint, TheoreticalOutcomeHint.Win)
  }

  test("SameColoredBishopsBlockade requires same-colored bishops and bishop-color blocking pawns") {
    val positiveFen = "7k/2p3b1/8/2P5/8/4B3/6K1/8 w - - 0 1"
    val negativeFen = "7k/6b1/2p5/2P5/8/4B3/6K1/8 w - - 0 1"

    val positive = evaluate(positiveFen, Color.White)
    val negative = evaluate(negativeFen, Color.White)

    assertEquals(positive.primaryPattern, Some("SameColoredBishopsBlockade"))
    assertEquals(positive.theoreticalOutcomeHint, TheoreticalOutcomeHint.Draw)
    assertNotEquals(negative.primaryPattern, Some("SameColoredBishopsBlockade"))
  }
