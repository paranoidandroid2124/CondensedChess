package lila.commentary.analysis

import chess.{ Pawn, Rook, Square, White }
import chess.format.Fen
import chess.variant.Standard
import munit.FunSuite

final class FlankInfrastructureEvidenceTest extends FunSuite:

  private def position(fen: String) =
    Fen.read(Standard, Fen.Full(fen)).getOrElse(fail(s"invalid FEN: $fen"))

  test("rook-pawn march candidates are rook-pawn advances, not generic flank pawn moves") {
    val pos = position("4k3/8/8/8/8/8/1P5P/6K1 w - - 0 1")
    val moves =
      FlankInfrastructureEvidence.movesForSubplan(
        pos,
        PlanTaxonomy.PlanKind.RookPawnMarch,
        pos.legalMoves.toList
      )

    assert(moves.nonEmpty, clue(pos.legalMoves.toList.map(_.toUci.uci)))
    assert(moves.forall(_.piece.role == Pawn), clue(moves.map(_.toUci.uci)))
    assert(moves.forall(_.orig.file == chess.File.H), clue(moves.map(_.toUci.uci)))
    assert(!moves.exists(_.orig == Square.B2), clue(moves.map(_.toUci.uci)))
  }

  test("rook-pawn march does not emit unaligned rook-pawn pushes without hook contact") {
    val pos = position("4k3/8/8/8/8/8/P7/4K3 w - - 0 1")
    val moves =
      FlankInfrastructureEvidence.movesForSubplan(
        pos,
        PlanTaxonomy.PlanKind.RookPawnMarch,
        pos.legalMoves.toList
      )

    assertEquals(moves, Nil)
  }

  test("hook creation candidates require actual enemy-pawn contact after the move") {
    val hook = position("4k3/8/8/7p/8/7P/8/6K1 w - - 0 1")
    val noHook = position("4k3/8/8/8/8/7P/8/6K1 w - - 0 1")

    val hookMoves =
      FlankInfrastructureEvidence.movesForSubplan(
        hook,
        PlanTaxonomy.PlanKind.HookCreation,
        hook.legalMoves.toList
      )
    val noHookMoves =
      FlankInfrastructureEvidence.movesForSubplan(
        noHook,
        PlanTaxonomy.PlanKind.HookCreation,
        noHook.legalMoves.toList
      )

    assert(hookMoves.exists(mv => mv.orig == Square.H3 && mv.dest == Square.H4), clue(hookMoves.map(_.toUci.uci)))
    assertEquals(noHookMoves, Nil)
  }

  test("rook-lift scaffold candidates are concrete lift-rank rook moves") {
    val pos = position("6k1/8/8/8/8/8/8/6KR w - - 0 1")
    val moves =
      FlankInfrastructureEvidence.movesForSubplan(
        pos,
        PlanTaxonomy.PlanKind.RookLiftScaffold,
        pos.legalMoves.toList
      )

    assert(moves.nonEmpty, clue(pos.legalMoves.toList.map(_.toUci.uci)))
    assert(moves.forall(mv => mv.piece.role == Rook && mv.orig == Square.H1), clue(moves.map(_.toUci.uci)))
    assert(moves.exists(mv => mv.dest == Square.H3 || mv.dest == Square.H4), clue(moves.map(_.toUci.uci)))
  }

  test("rook-lift scaffold does not emit generic rook moves without a lift rank") {
    val pos = position("4k3/8/8/8/8/8/8/R3K3 w - - 0 1")
    val moves =
      FlankInfrastructureEvidence.movesForSubplan(
        pos,
        PlanTaxonomy.PlanKind.RookLiftScaffold,
        pos.legalMoves.toList
      )

    assert(!moves.exists(_.dest == Square.B1), clue(moves.map(_.toUci.uci)))
  }

  test("rook-lift scaffold needs king-file pressure, check, or a resource target") {
    val pos = position("4k3/8/8/8/8/8/8/6KR w - - 0 1")
    val moves =
      FlankInfrastructureEvidence.movesForSubplan(
        pos,
        PlanTaxonomy.PlanKind.RookLiftScaffold,
        pos.legalMoves.toList
      )

    assertEquals(moves, Nil)
  }

  test("plan-first proposals can emit typed hook-creation subplans") {
    val fen = "4k3/8/8/7p/8/7P/8/6K1 w - - 0 1"
    val ctx = IntegratedContext(
      evalCp = 0,
      isWhiteToMove = true,
      positionKey = Some(fen),
      features = PositionAnalyzer.extractFeatures(fen, 1)
    )

    val hypotheses =
      PlanProposalEngine.propose(
        fen = fen,
        ply = 34,
        ctx = ctx,
        maxItems = 5
      )

    assert(
      hypotheses.exists(_.subplanId.contains(PlanTaxonomy.PlanKind.HookCreation.id)),
      clue(hypotheses.map(h => h.planId -> h.subplanId -> h.evidenceSources))
    )
  }

  test("plan-first proposals can emit typed rook-pawn march subplans") {
    val fen = "4k3/8/8/8/8/8/7P/6K1 w - - 0 1"
    val ctx = IntegratedContext(
      evalCp = 0,
      isWhiteToMove = true,
      positionKey = Some(fen),
      features = PositionAnalyzer.extractFeatures(fen, 1)
    )

    val hypotheses =
      PlanProposalEngine.propose(
        fen = fen,
        ply = 34,
        ctx = ctx,
        maxItems = 8
      )

    assert(
      hypotheses.exists(_.subplanId.contains(PlanTaxonomy.PlanKind.RookPawnMarch.id)),
      clue(hypotheses.map(h => h.planId -> h.subplanId -> h.evidenceSources))
    )
  }

  test("hook-creation hypotheses explain flank contact in player-facing language") {
    val fen = "4k3/8/8/7p/8/7P/8/6K1 w - - 0 1"
    val hypothesis =
      FlankInfrastructureEvidence
        .planHypotheses(fen, White)
        .find(_.subplanId.contains(PlanTaxonomy.PlanKind.HookCreation.id))
        .getOrElse(fail("missing hook-creation hypothesis"))
    val rendered =
      (List(hypothesis.planName) ++ hypothesis.preconditions ++ hypothesis.executionSteps).mkString(" ").toLowerCase

    assert(rendered.contains("h3-h4 creates contact on the h-file"), clue(rendered))
    assert(rendered.contains("play h3-h4 while the center stays controlled"), clue(rendered))
    assert(!rendered.contains("flank pawn move"), clue(rendered))
    assert(!rendered.contains("h3h4"), clue(rendered))
    assert(!rendered.contains("creates_hook_contact"), clue(rendered))
    assert(!rendered.contains("king_flank_aligned"), clue(rendered))
  }

  test("rook-lift hypotheses explain lift-rank support without raw reason tokens") {
    val fen = "6k1/8/8/8/8/8/8/6KR w - - 0 1"
    val hypothesis =
      FlankInfrastructureEvidence
        .planHypotheses(fen, White)
        .find(_.subplanId.contains(PlanTaxonomy.PlanKind.RookLiftScaffold.id))
        .getOrElse(fail("missing rook-lift hypothesis"))
    val rendered =
      (List(hypothesis.planName) ++ hypothesis.preconditions ++ hypothesis.executionSteps).mkString(" ").toLowerCase

    assert(rendered.contains("reaches a lift rank"), clue(rendered))
    assert(rendered.contains("lift the rook with"), clue(rendered))
    assert(!rendered.contains("rook lift move"), clue(rendered))
    assert(!rendered.contains("rook_lift_rank"), clue(rendered))
    assert(!rendered.contains("enemy_king_flank_file"), clue(rendered))
    assert(!rendered.contains("mobility_gain:"), clue(rendered))
  }

  test("plan-first proposals can emit typed rook-lift scaffold subplans") {
    val fen = "6k1/8/8/8/8/8/8/6KR w - - 0 1"
    val ctx = IntegratedContext(
      evalCp = 0,
      isWhiteToMove = true,
      positionKey = Some(fen),
      features = PositionAnalyzer.extractFeatures(fen, 1)
    )

    val hypotheses =
      PlanProposalEngine.propose(
        fen = fen,
        ply = 34,
        ctx = ctx,
        maxItems = 8
      )
    val lift =
      hypotheses.find(_.subplanId.contains(PlanTaxonomy.PlanKind.RookLiftScaffold.id))

    assert(
      lift.nonEmpty,
      clue(hypotheses.map(h => h.planId -> h.subplanId -> h.evidenceSources))
    )
    assert(
      lift.exists(_.evidenceSources.contains("rook_lift_rank")),
      clue(lift.map(_.evidenceSources))
    )
    assert(
      !lift.exists(_.executionSteps.exists(_.contains("flank pawn"))),
      clue(lift.map(_.executionSteps))
    )
  }
