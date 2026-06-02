package lila.commentary.analysis

import chess.{ Knight, Square, White }
import chess.format.Fen
import chess.variant.Standard
import munit.FunSuite

final class WeaknessFixationEvidenceTest extends FunSuite:

  private def position(fen: String) =
    Fen.read(Standard, Fen.Full(fen)).getOrElse(fail(s"invalid FEN: $fen"))

  test("backward-pawn targeting candidates are anchored to a real weakness target") {
    val pos = position("4k3/8/4p3/3p2N1/3P4/8/8/4K3 w - - 0 1")
    val moves =
      WeaknessFixationEvidence.movesForSubplan(
        pos,
        PlanTaxonomy.PlanKind.BackwardPawnTargeting,
        pos.legalMoves.toList
      )

    assert(moves.exists(mv => mv.piece.role == Knight && mv.orig == Square.G5 && mv.dest == Square.E6), clue(moves.map(_.toUci.uci)))
  }

  test("static weakness candidates can capture an isolated target") {
    val pos = position("4k3/8/8/4p3/2N5/8/8/4K3 w - - 0 1")
    val moves =
      WeaknessFixationEvidence.movesForSubplan(
        pos,
        PlanTaxonomy.PlanKind.StaticWeaknessFixation,
        pos.legalMoves.toList
      )

    assert(moves.exists(mv => mv.orig == Square.C4 && mv.dest == Square.E5), clue(moves.map(_.toUci.uci)))
  }

  test("iqp candidates are anchored to an actual isolated queen pawn") {
    val pos = position("4k3/8/8/5n2/3P4/8/8/4K3 b - - 0 1")
    val moves =
      WeaknessFixationEvidence.movesForSubplan(
        pos,
        PlanTaxonomy.PlanKind.IQPInducement,
        pos.legalMoves.toList
      )

    assert(moves.exists(mv => mv.orig == Square.F5 && mv.dest == Square.D4), clue(moves.map(_.toUci.uci)))
  }

  test("weakness subplans do not emit moves when no weakness target exists") {
    val pos = position("4k3/8/8/8/8/8/1P6/4K3 w - - 0 1")
    val moves =
      WeaknessFixationEvidence.movesForSubplan(
        pos,
        PlanTaxonomy.PlanKind.StaticWeaknessFixation,
        pos.legalMoves.toList
      )

    assertEquals(moves, Nil)
  }

  test("minority-attack fixation uses semantic-ready primary break as the probe move") {
    val pos = position("4k3/pp3ppp/2p5/3p4/1P1P4/4P3/P4PPP/4K3 w - - 0 1")
    val moves =
      WeaknessFixationEvidence.movesForSubplan(
        pos,
        PlanTaxonomy.PlanKind.MinorityAttackFixation,
        pos.legalMoves.toList
      )

    assert(moves.exists(mv => mv.orig == Square.B4 && mv.dest == Square.B5), clue(moves.map(_.toUci.uci)))
  }

  test("minority-attack fixation uses the current prep move when the break needs staging") {
    val pos = position("4k3/pp3ppp/2p5/3p4/3P4/1P2P3/P4PPP/4K3 w - - 0 1")
    val moves =
      WeaknessFixationEvidence.movesForSubplan(
        pos,
        PlanTaxonomy.PlanKind.MinorityAttackFixation,
        pos.legalMoves.toList
      )

    assert(moves.exists(mv => mv.orig == Square.B3 && mv.dest == Square.B4), clue(moves.map(_.toUci.uci)))
  }

  test("minority-attack fixation fails closed without a semantic-ready targetable majority") {
    val pos = position("4k3/ppp2ppp/8/8/1P6/4P3/P4PPP/4K3 w - - 0 1")
    val moves =
      WeaknessFixationEvidence.movesForSubplan(
        pos,
        PlanTaxonomy.PlanKind.MinorityAttackFixation,
        pos.legalMoves.toList
      )

    assertEquals(moves, Nil)
  }

  test("structural weakness creation does not promote to minority attack without semantic-ready support") {
    val fen = "4k3/ppp2ppp/8/8/1P6/4P3/P4PPP/4K3 w - - 0 1"
    val candidates = WeaknessFixationEvidence.candidatesFromFen(fen, White)

    assert(
      !candidates.exists(_.kind == PlanTaxonomy.PlanKind.MinorityAttackFixation),
      clue(candidates.map(c => (c.kind.id, c.targetSquare, c.moveUci, c.reasons)))
    )
  }

  test("weakness hypotheses explain target pressure in player-facing language") {
    val fen = "4k3/8/4p3/3p2N1/3P4/8/8/4K3 w - - 0 1"
    val hypothesis =
      WeaknessFixationEvidence
        .planHypotheses(fen, White)
        .find(_.subplanId.contains(PlanTaxonomy.PlanKind.BackwardPawnTargeting.id))
        .getOrElse(fail("missing backward-pawn hypothesis"))
    val rendered =
      (List(hypothesis.planName) ++ hypothesis.preconditions ++ hypothesis.executionSteps).mkString(" ").toLowerCase

    assert(rendered.contains("e6 is a backward pawn target"), clue(rendered))
    assert(rendered.contains("g5-e6 removes the target pawn directly"), clue(rendered))
    assert(rendered.contains("take the target on e6 with g5-e6"), clue(rendered))
    assert(!rendered.contains("candidate move"), clue(rendered))
    assert(!rendered.contains("g5e6"), clue(rendered))
    assert(!rendered.contains("weakness_target:"), clue(rendered))
    assert(!rendered.contains("pressure_delta:"), clue(rendered))
  }

  test("minority-attack hypotheses explain staged target creation without raw semantic tokens") {
    val fen = "4k3/pp3ppp/2p5/3p4/3P4/1P2P3/P4PPP/4K3 w - - 0 1"
    val hypothesis =
      WeaknessFixationEvidence
        .planHypotheses(fen, White)
        .find(_.subplanId.contains(PlanTaxonomy.PlanKind.MinorityAttackFixation.id))
        .getOrElse(fail("missing minority-attack hypothesis"))
    val rendered =
      (List(hypothesis.planName) ++ hypothesis.preconditions ++ hypothesis.executionSteps).mkString(" ").toLowerCase

    assert(rendered.contains("minority-attack target"), clue(rendered))
    assert(rendered.contains("stage b3-b4"), clue(rendered))
    assert(!rendered.contains("minority_attack_semantic"), clue(rendered))
    assert(!rendered.contains("primary_break:"), clue(rendered))
    assert(!rendered.contains("prep_move:"), clue(rendered))
    assert(!rendered.contains("target_after:"), clue(rendered))
  }

  test("plan-first proposals can emit typed backward-pawn targeting subplans") {
    val fen = "4k3/8/4p3/3p2N1/3P4/8/8/4K3 w - - 0 1"
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
      hypotheses.exists(_.subplanId.contains(PlanTaxonomy.PlanKind.BackwardPawnTargeting.id)),
      clue(hypotheses.map(h => h.planId -> h.subplanId -> h.evidenceSources))
    )
  }

  test("plan-first proposals can emit support-only minority-attack fixation subplans") {
    val fen = "4k3/pp3ppp/2p5/3p4/1P1P4/4P3/P4PPP/4K3 w - - 0 1"
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
      hypotheses.exists(h =>
        h.subplanId.contains(PlanTaxonomy.PlanKind.MinorityAttackFixation.id) &&
          h.evidenceSources.exists(_.contains("minority_attack_semantic"))
      ),
      clue(hypotheses.map(h => h.planId -> h.subplanId -> h.evidenceSources))
    )
  }
