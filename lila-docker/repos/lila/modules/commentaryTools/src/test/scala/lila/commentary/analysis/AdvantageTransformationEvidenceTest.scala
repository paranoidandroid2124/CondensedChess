package lila.commentary.analysis

import chess.{ Pawn, Rook, Square, White }
import chess.format.Fen
import chess.variant.Standard
import munit.FunSuite

final class AdvantageTransformationEvidenceTest extends FunSuite:

  private def position(fen: String) =
    Fen.read(Standard, Fen.Full(fen)).getOrElse(fail(s"invalid FEN: $fen"))

  test("passer conversion candidates advance an existing passed pawn") {
    val pos = position("4k3/8/8/4P3/8/8/8/4K3 w - - 0 1")
    val moves =
      AdvantageTransformationEvidence.movesForSubplan(
        pos,
        PlanTaxonomy.PlanKind.PasserConversion,
        pos.legalMoves.toList
      )

    assert(moves.nonEmpty, clue(pos.legalMoves.toList.map(_.toUci.uci)))
    assert(moves.forall(_.piece.role == Pawn), clue(moves.map(_.toUci.uci)))
    assert(moves.exists(mv => mv.orig == Square.E5 && mv.dest == Square.E6), clue(moves.map(_.toUci.uci)))
  }

  test("passed pawn manufacture candidates must create a real passer") {
    val pos = position("4k3/8/3p4/4P3/8/8/8/4K3 w - - 0 1")
    val moves =
      AdvantageTransformationEvidence.movesForSubplan(
        pos,
        PlanTaxonomy.PlanKind.PassedPawnManufacture,
        pos.legalMoves.toList
      )

    assert(moves.exists(mv => mv.orig == Square.E5 && mv.dest == Square.D6), clue(moves.map(_.toUci.uci)))
    assert(
      moves.forall(mv => PlanMoveEvidenceSupport.isPassedPawn(mv.after.board, mv.dest, chess.Color.White)),
      clue(moves.map(_.toUci.uci))
    )
  }

  test("ordinary pawn advances are not passed pawn manufacture") {
    val pos = position("4k3/8/3p1p2/8/4P3/8/8/4K3 w - - 0 1")
    val moves =
      AdvantageTransformationEvidence.movesForSubplan(
        pos,
        PlanTaxonomy.PlanKind.PassedPawnManufacture,
        pos.legalMoves.toList
      )

    assertEquals(moves, Nil)
  }

  test("simplification-conversion candidates require a material edge and concrete trade") {
    val pos = position("r3k3/8/8/8/8/8/8/RQ2K3 w - - 0 1")
    val moves =
      AdvantageTransformationEvidence.movesForSubplan(
        pos,
        PlanTaxonomy.PlanKind.SimplificationConversion,
        pos.legalMoves.toList
      )

    assert(moves.exists(mv => mv.piece.role == Rook && mv.orig == Square.A1 && mv.dest == Square.A8), clue(moves.map(_.toUci.uci)))
  }

  test("simplification-conversion candidates fail closed without a material edge") {
    val pos = position("r3k3/8/8/8/8/8/8/R3K3 w - - 0 1")
    val moves =
      AdvantageTransformationEvidence.movesForSubplan(
        pos,
        PlanTaxonomy.PlanKind.SimplificationConversion,
        pos.legalMoves.toList
      )

    assertEquals(moves, Nil)
  }

  test("invasion-transition candidates are heavy-piece moves to invasion ranks") {
    val pos = position("4k3/8/8/8/8/8/8/R3K3 w - - 0 1")
    val moves =
      AdvantageTransformationEvidence.movesForSubplan(
        pos,
        PlanTaxonomy.PlanKind.InvasionTransition,
        pos.legalMoves.toList
      )

    assert(moves.exists(mv => mv.piece.role == Rook && mv.orig == Square.A1 && mv.dest == Square.A8), clue(moves.map(_.toUci.uci)))
    assert(moves.forall(mv => mv.dest.rank == chess.Rank.Seventh || mv.dest.rank == chess.Rank.Eighth), clue(moves.map(_.toUci.uci)))
  }

  test("invasion-transition candidates do not emit generic rook moves") {
    val pos = position("4k3/8/8/8/8/8/8/R3K3 w - - 0 1")
    val moves =
      AdvantageTransformationEvidence.movesForSubplan(
        pos,
        PlanTaxonomy.PlanKind.InvasionTransition,
        pos.legalMoves.toList
      )

    assert(!moves.exists(_.dest == Square.B1), clue(moves.map(_.toUci.uci)))
  }

  test("invasion-transition candidates require check or a non-king resource target") {
    val materialOnly = position("8/8/7k/8/8/8/8/RN2K3 w - - 0 1")
    val resourceTarget = position("7k/1r6/8/8/8/8/8/R3K3 w - - 0 1")

    val materialOnlyMoves =
      AdvantageTransformationEvidence.movesForSubplan(
        materialOnly,
        PlanTaxonomy.PlanKind.InvasionTransition,
        materialOnly.legalMoves.toList
      )
    val resourceMoves =
      AdvantageTransformationEvidence.movesForSubplan(
        resourceTarget,
        PlanTaxonomy.PlanKind.InvasionTransition,
        resourceTarget.legalMoves.toList
      )

    assertEquals(materialOnlyMoves, Nil)
    assert(resourceMoves.exists(mv => mv.piece.role == Rook && mv.orig == Square.A1 && mv.dest == Square.A7), clue(resourceMoves.map(_.toUci.uci)))
  }

  test("opposite-bishops conversion candidates require opposite bishops and passer progress") {
    val pos = position("4k1b1/8/8/4P3/8/8/8/2B1K3 w - - 0 1")
    val moves =
      AdvantageTransformationEvidence.movesForSubplan(
        pos,
        PlanTaxonomy.PlanKind.OppositeBishopsConversion,
        pos.legalMoves.toList
      )

    assert(moves.exists(mv => mv.piece.role == Pawn && mv.orig == Square.E5 && mv.dest == Square.E6), clue(moves.map(_.toUci.uci)))
  }

  test("opposite-bishops conversion fails closed without opposite bishops") {
    val pos = position("4k3/8/8/4P3/8/8/8/4K3 w - - 0 1")
    val moves =
      AdvantageTransformationEvidence.movesForSubplan(
        pos,
        PlanTaxonomy.PlanKind.OppositeBishopsConversion,
        pos.legalMoves.toList
      )

    assertEquals(moves, Nil)
  }

  test("invasion-transition hypotheses explain the transformed asset in player-facing language") {
    val fen = "7k/1r6/8/8/8/8/8/R3K3 w - - 0 1"
    val hypothesis =
      AdvantageTransformationEvidence
        .planHypotheses(fen, White)
        .find(_.subplanId.contains(PlanTaxonomy.PlanKind.InvasionTransition.id))
        .getOrElse(fail("missing invasion-transition hypothesis"))
    val rendered =
      (List(hypothesis.planName) ++ hypothesis.preconditions ++ hypothesis.executionSteps).mkString(" ").toLowerCase

    assert(rendered.contains("a1-a7 is a concrete invasion move"), clue(rendered))
    assert(rendered.contains("a7 is an invasion square for the rook"), clue(rendered))
    assert(rendered.contains("the invasion attacks b7"), clue(rendered))
    assert(rendered.contains("place the rook on a7 with a1-a7"), clue(rendered))
    assert(!rendered.contains("candidate move"), clue(rendered))
    assert(!rendered.contains("a1a7"), clue(rendered))
    assert(!rendered.contains("heavy_piece_invasion_square"), clue(rendered))
    assert(!rendered.contains("attacks_resources:"), clue(rendered))
  }

  test("passed-pawn manufacture hypotheses describe conversion without raw reason tokens") {
    val fen = "4k3/8/3p4/4P3/8/8/8/4K3 w - - 0 1"
    val hypothesis =
      AdvantageTransformationEvidence
        .planHypotheses(fen, White)
        .find(_.subplanId.contains(PlanTaxonomy.PlanKind.PassedPawnManufacture.id))
        .getOrElse(fail("missing passed-pawn manufacture hypothesis"))
    val rendered =
      (List(hypothesis.planName) ++ hypothesis.preconditions ++ hypothesis.executionSteps).mkString(" ").toLowerCase

    assert(rendered.contains("e5-d6 is a concrete passed-pawn creation move"), clue(rendered))
    assert(rendered.contains("e5-d6 creates a passed pawn"), clue(rendered))
    assert(rendered.contains("use e5-d6 to create the passer"), clue(rendered))
    assert(!rendered.contains("candidate move"), clue(rendered))
    assert(!rendered.contains("e5d6"), clue(rendered))
    assert(!rendered.contains("creates_passed_pawn"), clue(rendered))
    assert(!rendered.contains("passer_count:"), clue(rendered))
  }

  test("plan-first proposals can emit typed invasion-transition subplans") {
    val fen = "4k3/8/8/8/8/8/8/R3K3 w - - 0 1"
    val ctx = IntegratedContext(
      evalCp = 0,
      isWhiteToMove = true,
      positionKey = Some(fen),
      features = PositionAnalyzer.extractFeatures(fen, 1)
    )

    val hypotheses =
      PlanProposalEngine.propose(
        fen = fen,
        ply = 42,
        ctx = ctx,
        maxItems = 8
      )

    assert(
      hypotheses.exists(_.subplanId.contains(PlanTaxonomy.PlanKind.InvasionTransition.id)),
      clue(hypotheses.map(h => h.planId -> h.subplanId -> h.evidenceSources))
    )
  }

  test("plan-first proposals can emit typed pawn conversion subplans") {
    val fen = "4k3/8/3p4/4P3/8/8/8/4K3 w - - 0 1"
    val ctx = IntegratedContext(
      evalCp = 0,
      isWhiteToMove = true,
      positionKey = Some(fen),
      features = PositionAnalyzer.extractFeatures(fen, 1)
    )

    val hypotheses =
      PlanProposalEngine.propose(
        fen = fen,
        ply = 40,
        ctx = ctx,
        maxItems = 8
      )

    assert(
      hypotheses.exists(_.subplanId.contains(PlanTaxonomy.PlanKind.PassedPawnManufacture.id)),
      clue(hypotheses.map(h => h.planId -> h.subplanId -> h.evidenceSources))
    )
  }
