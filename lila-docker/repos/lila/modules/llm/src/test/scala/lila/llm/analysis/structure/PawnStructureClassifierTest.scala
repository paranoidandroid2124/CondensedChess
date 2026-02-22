package lila.llm.analysis.structure

import chess.Color
import chess.format.Fen
import chess.variant.Standard
import lila.llm.analysis.PositionAnalyzer
import lila.llm.model.structure.StructureId
import munit.FunSuite

class PawnStructureClassifierTest extends FunSuite:

  private def classify(fen: String, side: Color = Color.White, minConfidence: Double = 0.72, minMargin: Double = 0.10) =
    val features = PositionAnalyzer.extractFeatures(fen, plyCount = 1).getOrElse(fail(s"features unavailable: $fen"))
    val board = Fen.read(Standard, Fen.Full(fen)).map(_.board).getOrElse(fail(s"invalid fen: $fen"))
    PawnStructureClassifier.classify(features, board, side, minConfidence = minConfidence, minMargin = minMargin)

  private val positives = List(
    StructureId.Carlsbad -> "4k3/1p6/2p5/3p4/3P4/4P3/8/4K3 w - - 1 101",
    StructureId.IQPWhite -> "4k3/8/8/8/3P4/8/8/4K3 w - - 1 115",
    StructureId.IQPBlack -> "4k3/8/8/3p4/8/8/8/4K3 w - - 1 129",
    StructureId.HangingPawnsWhite -> "4k3/8/8/8/2PP4/8/8/4K3 w - - 1 143",
    StructureId.HangingPawnsBlack -> "4k3/8/8/2pp4/8/8/8/4K3 w - - 1 157",
    StructureId.FrenchAdvanceChain -> "4k3/8/4p3/3pP3/3P4/8/8/4K3 w - - 1 171",
    StructureId.NajdorfScheveningenCenter -> "4k3/8/3pp3/2p5/4P3/8/8/4K3 w - - 1 185",
    StructureId.BenoniCenter -> "4k3/8/1p1p4/2pP4/4P3/8/8/4K3 w - - 1 199",
    StructureId.KIDLockedCenter -> "4k3/8/3p2p1/4p3/3PP3/8/8/4K3 w - - 1 213",
    StructureId.SlavCaroTriangle -> "4k3/8/2p1p3/3p4/8/8/8/4K3 w - - 1 227",
    StructureId.MaroczyBind -> "4k3/8/1p1p4/2p5/2P1P3/8/8/4K3 w - - 1 241",
    StructureId.Hedgehog -> "4k3/8/pp1pp3/8/8/8/8/4K3 w - - 1 255",
    StructureId.FianchettoShell -> "4k3/8/8/8/8/6P1/6B1/4K3 w - - 1 269",
    StructureId.Stonewall -> "4k3/8/8/8/3P1P2/2P1P3/8/4K3 w - - 1 283",
    StructureId.OpenCenter -> "4k3/8/8/8/2P2P2/8/8/4K3 w - - 1 297",
    StructureId.LockedCenter -> "4k3/8/8/3pp3/3PPPP1/8/8/4K3 w - - 1 311",
    StructureId.FluidCenter -> "4k3/8/4p3/3p4/4P3/8/8/4K3 w - - 1 325",
    StructureId.SymmetricCenter -> "4k3/8/4p3/8/4P3/8/8/4K3 w - - 1 339"
  )

  positives.foreach { case (expected, fen) =>
    test(s"classifies ${expected.toString} positive sample") {
      val result = classify(fen)
      assertEquals(result.primary, expected, clues(result))
      assert(result.confidence >= 0.72, clues(result))
    }
  }

  test("resolves IQP ownership independently of side-to-move") {
    val fen = "4k3/8/8/8/3P4/8/8/4K3 b - - 1 1"
    val result = classify(fen, side = Color.Black)
    assertEquals(result.primary, StructureId.IQPWhite, clues(result))
  }

  test("resolves hanging ownership independently of side-to-move") {
    val fen = "4k3/8/8/8/2PP4/8/8/4K3 b - - 1 1"
    val result = classify(fen, side = Color.Black)
    assertEquals(result.primary, StructureId.HangingPawnsWhite, clues(result))
  }

  test("fails closed to unknown on explicitly unknown goldset sample") {
    val fen = "4k3/8/2p1p3/3p4/3P4/4P3/8/4K3 w - - 1 353"
    val result = classify(fen)
    assertEquals(result.primary, StructureId.Unknown)
    assert(result.evidenceCodes.exists(c => c == "LOW_CONF" || c == "LOW_MARGIN" || c == "REQ_MISS" || c == "BLK_CONFLICT"), clues(result))
  }

  test("fails closed on ambiguous start position under strict thresholds") {
    val fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    val result = classify(fen, minConfidence = 1.01, minMargin = 0.35)
    assertEquals(result.primary, StructureId.Unknown)
    assert(result.evidenceCodes.exists(c => c == "LOW_CONF" || c == "LOW_MARGIN"), clues(result))
  }
