package lila.commentary.analysis

import chess.{ Pawn, Square, White }
import chess.format.Fen
import chess.variant.Standard
import munit.FunSuite

final class PawnBreakEvidenceTest extends FunSuite:

  private def position(fen: String) =
    Fen.read(Standard, Fen.Full(fen)).getOrElse(fail(s"invalid FEN: $fen"))

  test("central break candidates are concrete pawn contacts or captures") {
    val pos = position("4k3/8/8/3p4/4P3/8/8/4K3 w - - 0 1")
    val moves =
      PawnBreakEvidence.movesForSubplan(
        pos,
        PlanTaxonomy.PlanKind.CentralBreakTiming,
        pos.legalMoves.toList
      )

    assert(moves.nonEmpty, clue(pos.legalMoves.toList.map(_.toUci.uci)))
    assert(moves.forall(_.piece.role == Pawn), clue(moves.map(_.toUci.uci)))
    assert(moves.exists(mv => mv.orig == Square.E4 && mv.dest == Square.D5), clue(moves.map(_.toUci.uci)))
  }

  test("quiet central pawn moves without contact are not central breaks") {
    val pos = position("4k3/8/8/8/8/8/4P3/4K3 w - - 0 1")
    val moves =
      PawnBreakEvidence.movesForSubplan(
        pos,
        PlanTaxonomy.PlanKind.CentralBreakTiming,
        pos.legalMoves.toList
      )

    assertEquals(moves, Nil)
  }

  test("tension-maintenance candidates preserve existing central pawn tension") {
    val pos = position("4k3/8/8/3p4/4P3/5N2/8/4K3 w - - 0 1")
    val moves =
      PawnBreakEvidence.movesForSubplan(
        pos,
        PlanTaxonomy.PlanKind.TensionMaintenance,
        pos.legalMoves.toList
      )

    assert(moves.nonEmpty, clue(pos.legalMoves.toList.map(_.toUci.uci)))
    assert(moves.forall(mv => mv.piece.role != Pawn && !mv.captures), clue(moves.map(_.toUci.uci)))
    assert(!moves.exists(mv => mv.orig == Square.E4 && mv.dest == Square.D5), clue(moves.map(_.toUci.uci)))
  }

  test("tension-maintenance candidates fail closed when no pawn tension exists") {
    val pos = position("4k3/8/8/8/4P3/5N2/8/4K3 w - - 0 1")
    val moves =
      PawnBreakEvidence.movesForSubplan(
        pos,
        PlanTaxonomy.PlanKind.TensionMaintenance,
        pos.legalMoves.toList
      )

    assertEquals(moves, Nil)
  }

  test("tension-maintenance candidates do not emit arbitrary quiet shuffles") {
    val pos = position("4k3/8/8/3p4/4P3/8/8/4K2R w - - 0 1")
    val moves =
      PawnBreakEvidence.movesForSubplan(
        pos,
        PlanTaxonomy.PlanKind.TensionMaintenance,
        pos.legalMoves.toList
      )

    assertEquals(moves, Nil)
  }

  test("wing break candidates are concrete flank pawn contacts or captures") {
    val pos = position("4k3/8/8/6p1/7P/8/8/4K3 w - - 0 1")
    val moves =
      PawnBreakEvidence.movesForSubplan(
        pos,
        PlanTaxonomy.PlanKind.WingBreakTiming,
        pos.legalMoves.toList
      )

    assert(moves.exists(mv => mv.orig == Square.H4 && mv.dest == Square.G5), clue(moves.map(_.toUci.uci)))
  }

  test("wing break hypotheses keep the pawn-break taxonomy theme") {
    val fen = "4k3/8/8/6p1/7P/8/8/4K3 w - - 0 1"
    val hypotheses = PawnBreakEvidence.planHypotheses(fen, White)
    val wing =
      hypotheses.find(_.subplanId.contains(PlanTaxonomy.PlanKind.WingBreakTiming.id))

    assert(wing.nonEmpty, clue(hypotheses.map(h => h.subplanId -> h.evidenceSources)))
    assertEquals(wing.map(_.themeL1), Some(PlanTaxonomy.PlanTheme.PawnBreakPreparation.id))
    assert(
      wing.exists(_.evidenceSources.contains("theme:pawn_break_preparation")),
      clue(wing.map(_.evidenceSources))
    )
    assert(
      !wing.exists(_.evidenceSources.contains("theme:flank_infrastructure")),
      clue(wing.map(_.evidenceSources))
    )
  }

  test("central break hypotheses explain timing in player-facing language") {
    val fen = "4k3/8/8/3p4/4P3/8/8/4K3 w - - 0 1"
    val hypothesis =
      PawnBreakEvidence
        .planHypotheses(fen, White)
        .find(_.subplanId.contains(PlanTaxonomy.PlanKind.CentralBreakTiming.id))
        .getOrElse(fail("missing central-break hypothesis"))
    val rendered =
      (List(hypothesis.planName) ++ hypothesis.preconditions ++ hypothesis.executionSteps).mkString(" ").toLowerCase

    assert(rendered.contains("e4-d5 is a concrete central pawn break"), clue(rendered))
    assert(rendered.contains("play e4-d5 as the concrete central break"), clue(rendered))
    assert(!rendered.contains("pawn break move"), clue(rendered))
    assert(!rendered.contains("e4d5"), clue(rendered))
    assert(!rendered.contains("pawn_break"), clue(rendered))
    assert(!rendered.contains("captures_tension_pawn"), clue(rendered))
  }

  test("tension-maintenance hypotheses explain held tension without raw reason tokens") {
    val fen = "4k3/8/8/3p4/4P3/5N2/8/4K3 w - - 0 1"
    val hypothesis =
      PawnBreakEvidence
        .planHypotheses(fen, White)
        .find(_.subplanId.contains(PlanTaxonomy.PlanKind.TensionMaintenance.id))
        .getOrElse(fail("missing tension-maintenance hypothesis"))
    val rendered =
      (List(hypothesis.planName) ++ hypothesis.preconditions ++ hypothesis.executionSteps).mkString(" ").toLowerCase

    assert(rendered.contains("keeps the existing central pawn contact"), clue(rendered))
    assert(rendered.contains("hold the center with"), clue(rendered))
    assert(!rendered.contains("candidate move"), clue(rendered))
    assert(!rendered.contains("central_tension_preserved"), clue(rendered))
    assert(!rendered.contains("minor_piece_centralizes"), clue(rendered))
    assert(!rendered.contains("mobility_gain:"), clue(rendered))
  }

  test("plan-first proposals can emit typed central break subplans") {
    val fen = "4k3/8/8/3p4/4P3/8/8/4K3 w - - 0 1"
    val ctx = IntegratedContext(
      evalCp = 0,
      isWhiteToMove = true,
      positionKey = Some(fen),
      features = PositionAnalyzer.extractFeatures(fen, 1)
    )

    val hypotheses =
      PlanProposalEngine.propose(
        fen = fen,
        ply = 28,
        ctx = ctx,
        maxItems = 10
      )

    assert(
      hypotheses.exists(_.subplanId.contains(PlanTaxonomy.PlanKind.CentralBreakTiming.id)),
      clue(hypotheses.map(h => h.planId -> h.subplanId -> h.evidenceSources))
    )
  }

  test("plan-first proposals can emit typed tension-maintenance subplans") {
    val fen = "4k3/8/8/3p4/4P3/5N2/8/4K3 w - - 0 1"
    val ctx = IntegratedContext(
      evalCp = 0,
      isWhiteToMove = true,
      positionKey = Some(fen),
      features = PositionAnalyzer.extractFeatures(fen, 1)
    )

    val hypotheses =
      PlanProposalEngine.propose(
        fen = fen,
        ply = 28,
        ctx = ctx,
        maxItems = 10
      )

    assert(
      hypotheses.exists(_.subplanId.contains(PlanTaxonomy.PlanKind.TensionMaintenance.id)),
      clue(hypotheses.map(h => h.planId -> h.subplanId -> h.evidenceSources))
    )
  }

  test("plan-first proposals can emit typed wing-break subplans") {
    val fen = "4k3/8/8/6p1/7P/8/8/4K3 w - - 0 1"
    val ctx = IntegratedContext(
      evalCp = 0,
      isWhiteToMove = true,
      positionKey = Some(fen),
      features = PositionAnalyzer.extractFeatures(fen, 1)
    )

    val hypotheses =
      PlanProposalEngine.propose(
        fen = fen,
        ply = 28,
        ctx = ctx,
        maxItems = 10
      )

    assert(
      hypotheses.exists(h =>
        h.subplanId.contains(PlanTaxonomy.PlanKind.WingBreakTiming.id) &&
          h.themeL1 == PlanTaxonomy.PlanTheme.PawnBreakPreparation.id
      ),
      clue(hypotheses.map(h => h.planId -> h.subplanId -> h.themeL1 -> h.evidenceSources))
    )
  }
