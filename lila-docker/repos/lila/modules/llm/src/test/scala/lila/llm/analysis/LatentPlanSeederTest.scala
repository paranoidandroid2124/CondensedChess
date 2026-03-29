package lila.llm.analysis

import chess.{ File, Knight, Queen, Rook, Square }
import lila.llm.model.authoring.{ LineType, MovePattern }
import munit.FunSuite

class LatentPlanSeederTest extends FunSuite:

  private def ctx(fen: String, isWhiteToMove: Boolean) =
    IntegratedContext(
      evalCp = 0,
      isWhiteToMove = isWhiteToMove,
      positionKey = Some(fen),
      features = PositionAnalyzer.extractFeatures(fen, 1)
    )

  test("dynamic knight-outpost seed is no longer blocked by legacy scoring") {
    val beforeFen = "4k3/8/8/8/4P3/2N5/8/4K3 w - - 0 1"

    val seeds = LatentPlanSeeder.seedForMoveAnnotation(
      fenBefore = beforeFen,
      playedUci = "e1f1",
      ply = 1,
      ctx = ctx(beforeFen, isWhiteToMove = true)
    )

    val outpost = seeds.find(_.seedId == "KnightOutpost_Route").getOrElse(fail("missing KnightOutpost_Route"))
    assert(outpost.candidateMoves.contains(MovePattern.PieceTo(Knight, Square.D5)), clue(outpost.candidateMoves))
  }

  test("open-file doubling seed surfaces when heavy pieces already own the file") {
    val beforeFen = "4k3/8/8/8/3R4/8/3R1Q2/4K3 w - - 0 1"

    val seeds = LatentPlanSeeder.seedForMoveAnnotation(
      fenBefore = beforeFen,
      playedUci = "f2f1",
      ply = 1,
      ctx = ctx(beforeFen, isWhiteToMove = true)
    )

    val doubling = seeds.find(_.seedId == "OpenFile_Doubling").getOrElse(fail("missing OpenFile_Doubling"))
    assert(
      doubling.candidateMoves.exists {
        case MovePattern.BatteryFormation(Rook, Rook, LineType.FileLine(File.D))   => true
        case MovePattern.BatteryFormation(Rook, Queen, LineType.FileLine(File.D)) => true
        case _                                                                     => false
      },
      clue(doubling.candidateMoves)
    )
  }
