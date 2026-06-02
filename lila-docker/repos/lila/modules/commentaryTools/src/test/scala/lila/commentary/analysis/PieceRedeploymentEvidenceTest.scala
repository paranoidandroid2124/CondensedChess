package lila.commentary.analysis

import chess.{ Bishop, Queen, Rook, Square, White }
import chess.format.Fen
import chess.variant.Standard
import munit.FunSuite

final class PieceRedeploymentEvidenceTest extends FunSuite:

  private def position(fen: String) =
    Fen.read(Standard, Fen.Full(fen)).getOrElse(fail(s"invalid FEN: $fen"))

  test("bishop reanchor probe candidates are actual bishop improvement moves") {
    val pos = position("4k3/8/8/8/8/8/8/2B1K3 w - - 0 1")
    val moves =
      PieceRedeploymentEvidence.movesForSubplan(
        pos,
        PlanTaxonomy.PlanKind.BishopReanchor,
        pos.legalMoves.toList
      )

    assert(moves.nonEmpty, clue(pos.legalMoves.toList.map(_.toUci.uci)))
    assert(moves.forall(_.piece.role == Bishop), clue(moves.map(_.toUci.uci)))
    assert(moves.exists(_.dest == Square.E3), clue(moves.map(_.toUci.uci)))
  }

  test("rook file-transfer candidates require a rook and an open or improved file") {
    val noRook = position("4k3/8/8/8/8/8/8/2B1K3 w - - 0 1")
    val quietRook = position("4k3/8/8/8/8/8/8/R3K3 w - - 0 1")

    val noRookMoves =
      PieceRedeploymentEvidence.movesForSubplan(
        noRook,
        PlanTaxonomy.PlanKind.RookFileTransfer,
        noRook.legalMoves.toList
      )
    val rookMoves =
      PieceRedeploymentEvidence.movesForSubplan(
        quietRook,
        PlanTaxonomy.PlanKind.RookFileTransfer,
        quietRook.legalMoves.toList
      )

    assertEquals(noRookMoves, Nil)
    assert(rookMoves.nonEmpty, clue(quietRook.legalMoves.toList.map(_.toUci.uci)))
    assert(rookMoves.forall(_.piece.role == Rook), clue(rookMoves.map(_.toUci.uci)))
  }

  test("rook file-transfer candidates do not relabel same-file rook moves") {
    val pos = position("4k3/8/8/8/8/8/8/R3K3 w - - 0 1")
    val moves =
      PieceRedeploymentEvidence.movesForSubplan(
        pos,
        PlanTaxonomy.PlanKind.RookFileTransfer,
        pos.legalMoves.toList
      )

    assert(moves.nonEmpty, clue(pos.legalMoves.toList.map(_.toUci.uci)))
    assert(moves.forall(mv => mv.orig.file != mv.dest.file), clue(moves.map(_.toUci.uci)))
  }

  test("open-file pressure candidates require a heavy-piece file target") {
    val pos = position("6k1/8/2p5/8/8/8/8/4R1K1 w - - 0 1")
    val moves =
      PieceRedeploymentEvidence.movesForSubplan(
        pos,
        PlanTaxonomy.PlanKind.OpenFilePressure,
        pos.legalMoves.toList
      )

    assert(moves.exists(mv => mv.piece.role == Rook && mv.orig == Square.E1 && mv.dest == Square.C1), clue(moves.map(_.toUci.uci)))
  }

  test("open-file pressure does not emit generic heavy-piece shuffles without a file target") {
    val pos = position("7k/8/8/8/8/8/8/4R1K1 w - - 0 1")
    val moves =
      PieceRedeploymentEvidence.movesForSubplan(
        pos,
        PlanTaxonomy.PlanKind.OpenFilePressure,
        pos.legalMoves.toList
      )

    assertEquals(moves, Nil)
  }

  test("open-file pressure does not treat a blocked same-file target as pressure") {
    val pos = position("6k1/8/2p5/8/8/2N5/8/4R1K1 w - - 0 1")
    val moves =
      PieceRedeploymentEvidence.movesForSubplan(
        pos,
        PlanTaxonomy.PlanKind.OpenFilePressure,
        pos.legalMoves.toList
      )

    assert(!moves.exists(mv => mv.orig == Square.E1 && mv.dest == Square.C1), clue(moves.map(_.toUci.uci)))
  }

  test("worst-piece improvement does not relabel active queen centralization") {
    val pos = position("4k3/8/8/8/3Q4/8/8/4K3 w - - 0 1")
    val moves =
      PieceRedeploymentEvidence.movesForSubplan(
        pos,
        PlanTaxonomy.PlanKind.WorstPieceImprovement,
        pos.legalMoves.toList
      )

    assertEquals(moves, Nil)
    assert(pos.legalMoves.toList.exists(mv => mv.piece.role == Queen && mv.dest == Square.E4))
  }

  test("middlegame plan-first proposals can emit typed piece-redeployment subplans") {
    val fen = "4k3/8/8/8/8/8/8/1N2K3 w - - 0 1"
    val ctx = IntegratedContext(
      evalCp = 0,
      isWhiteToMove = true,
      positionKey = Some(fen),
      features = PositionAnalyzer.extractFeatures(fen, 1)
    )

    val hypotheses =
      PlanProposalEngine.propose(
        fen = fen,
        ply = 30,
        ctx = ctx,
        maxItems = 5
      )

    assert(
      hypotheses.exists(_.subplanId.contains(PlanTaxonomy.PlanKind.WorstPieceImprovement.id)),
      clue(hypotheses.map(h => h.planId -> h.subplanId -> h.evidenceSources))
    )
  }

  test("plan-first proposals can emit typed open-file pressure subplans") {
    val fen = "6k1/8/2p5/8/8/8/8/4R1K1 w - - 0 1"
    val ctx = IntegratedContext(
      evalCp = 0,
      isWhiteToMove = true,
      positionKey = Some(fen),
      features = PositionAnalyzer.extractFeatures(fen, 1)
    )

    val hypotheses =
      PlanProposalEngine.propose(
        fen = fen,
        ply = 30,
        ctx = ctx,
        maxItems = 5
      )
    val openFile =
      hypotheses.find(_.subplanId.contains(PlanTaxonomy.PlanKind.OpenFilePressure.id))

    assert(
      openFile.nonEmpty,
      clue(hypotheses.map(h => h.planId -> h.subplanId -> h.evidenceSources))
    )
    assert(
      openFile.exists(_.evidenceSources.exists(_.contains("file_targets:c6"))),
      clue(openFile.map(_.evidenceSources))
    )
  }

  test("piece redeployment hypotheses explain why in player-facing language") {
    val fen = "6k1/8/2p5/8/8/8/8/4R1K1 w - - 0 1"
    val hypothesis =
      PieceRedeploymentEvidence
        .planHypotheses(fen, White)
        .find(_.subplanId.contains(PlanTaxonomy.PlanKind.OpenFilePressure.id))
        .getOrElse(fail("missing open-file pressure hypothesis"))
    val rendered =
      (List(hypothesis.planName) ++ hypothesis.preconditions ++ hypothesis.executionSteps).mkString(" ").toLowerCase

    assert(rendered.contains("c-file"), clue(rendered))
    assert(rendered.contains("against c6"), clue(rendered))
    assert(rendered.contains("place the rook on c1"), clue(hypothesis.executionSteps))
    assert(!rendered.contains("open_or_semi_open_file"), clue(rendered))
    assert(!rendered.contains("file_targets:"), clue(rendered))
    assert(!rendered.contains("mobility_gain:"), clue(rendered))
  }
