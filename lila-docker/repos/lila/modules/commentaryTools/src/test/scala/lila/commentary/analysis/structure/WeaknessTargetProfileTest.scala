package lila.commentary.analysis.structure

import chess.Color
import chess.format.Fen
import chess.variant.Standard
import munit.FunSuite

final class WeaknessTargetProfileTest extends FunSuite:

  private def board(fen: String) =
    Fen.read(Standard, Fen.Full(fen)).map(_.board).getOrElse(fail(s"invalid FEN: $fen"))

  test("detects dynamic isolated pawn targets from board truth") {
    val fen = "4k3/8/8/4p3/8/8/8/4K3 w - - 0 1"
    val targets = WeaknessTargetProfile.fromFenForMover(fen)

    assertEquals(targets.map(_.targetSquare), List("e5"))
    assertEquals(targets.head.kind, WeaknessTargetProfile.IsolatedPawn)
    assertEquals(targets.head.weakSide, Color.Black)
    assert(targets.head.evidenceTerms.contains("weakness_target:e5"), clues(targets))
  }

  test("detects backward pawn targets through PositionAnalyzer primitive") {
    val fen = "4k3/8/4p3/3p4/3P4/8/8/4K3 w - - 0 1"
    val targets = WeaknessTargetProfile.targetsForPressure(board(fen), Color.White)

    assert(targets.exists(target =>
      target.targetSquare == "e6" &&
        target.kind == WeaknessTargetProfile.BackwardPawn &&
        target.weakSide == Color.Black
    ), clues(targets))
  }

  test("target hints accept only exact square target evidence") {
    assertEquals(
      WeaknessTargetProfile.targetHintSquares(
        List(
          "target:e5",
          "weakness_target:d6",
          "fixed_target:c6",
          "coordinated_target:f5",
          "target_fixing:e4",
          "enemy_weak_square:d5",
          "weak_complex:c4",
          "target:e5:queen",
          "target:d5_extra"
        )
      ),
      List("e5", "d6", "c6", "f5", "e4", "d5", "c4")
    )
  }

  test("promotes isolated queen pawn over generic isolated pawn on the same square") {
    val fen = "4k3/8/8/8/3P4/8/8/4K3 b - - 0 1"
    val targets = WeaknessTargetProfile.fromFenForMover(fen)

    assertEquals(targets.map(_.targetSquare), List("d4"))
    assertEquals(targets.head.kind, WeaknessTargetProfile.IQP)
    assertEquals(targets.head.weakSide, Color.White)
  }

  test("classifies a target as liquidated when the defender pushes it away on the line") {
    val fen = "4k3/8/8/4p3/8/8/8/4K3 w - - 0 1"

    assertEquals(
      WeaknessTargetProfile.lineOutcomeFromFen(fen, List("e1e2", "e5e4"), "e5").map(_.status),
      Some(WeaknessTargetProfile.LiquidatedByDefense)
    )
  }

  test("classifies a disappearing target as resolved when the pressure side captures it") {
    val fen = "4k3/8/8/4p3/2N5/8/8/4K3 w - - 0 1"

    assertEquals(
      WeaknessTargetProfile.lineOutcomeFromFen(fen, List("c4e5"), "e5").map(_.status),
      Some(WeaknessTargetProfile.ResolvedByPressure)
    )
  }

  test("does not trust a resulting FEN when the PV cannot replay to it") {
    val fen = "4k3/8/8/4p3/8/8/8/4K3 w - - 0 1"
    val staleResultingFen = "4k3/8/8/8/4p3/8/8/4K3 b - - 0 1"

    assertEquals(
      WeaknessTargetProfile.lineOutcomeFromFen(
        fen = fen,
        moves = List("e1e9"),
        targetSquare = "e5",
        resultingFen = Some(staleResultingFen)
      ),
      None
    )
  }
