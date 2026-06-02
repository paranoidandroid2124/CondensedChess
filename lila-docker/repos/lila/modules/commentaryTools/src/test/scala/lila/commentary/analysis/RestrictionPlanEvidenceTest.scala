package lila.commentary.analysis

import chess.{ Knight, Pawn, Square, White }
import chess.format.Fen
import chess.variant.Standard
import munit.FunSuite

final class RestrictionPlanEvidenceTest extends FunSuite:

  private def position(fen: String) =
    Fen.read(Standard, Fen.Full(fen)).getOrElse(fail(s"invalid FEN: $fen"))

  test("key-square denial candidates occupy a square the opponent could use") {
    val pos = position("4k3/8/8/5n2/8/5N2/8/4K3 w - - 0 1")
    val moves =
      RestrictionPlanEvidence.movesForSubplan(
        pos,
        PlanTaxonomy.PlanKind.KeySquareDenial,
        pos.legalMoves.toList
      )

    assert(moves.exists(mv => mv.piece.role == Knight && mv.orig == Square.F3 && mv.dest == Square.D4), clue(moves.map(_.toUci.uci)))
  }

  test("break-prevention candidates reduce concrete opponent pawn breaks") {
    val pos = position("4k3/8/8/2p5/3P4/8/8/4K3 w - - 0 1")
    val moves =
      RestrictionPlanEvidence.movesForSubplan(
        pos,
        PlanTaxonomy.PlanKind.BreakPrevention,
        pos.legalMoves.toList
      )

    assert(moves.exists(mv => mv.piece.role == Pawn && mv.orig == Square.D4 && mv.dest == Square.D5), clue(moves.map(_.toUci.uci)))
  }

  test("restriction hypotheses explain why the candidate is good in player-facing language") {
    val fen = "4k3/8/8/2p5/3P4/8/8/4K3 w - - 0 1"
    val hypothesis =
      RestrictionPlanEvidence
        .planHypotheses(fen, White)
        .find(_.subplanId.contains(PlanTaxonomy.PlanKind.BreakPrevention.id))
        .getOrElse(fail("missing break-prevention hypothesis"))
    val rendered =
      (List(hypothesis.planName) ++ hypothesis.preconditions ++ hypothesis.executionSteps).mkString(" ").toLowerCase

    assert(hypothesis.planName.matches(""".*[a-h][1-8]-[a-h][1-8].*"""), clue(hypothesis.planName))
    assert(rendered.contains("cuts opponent break choices"), clue(hypothesis.preconditions))
    assert(rendered.contains("before the counter-break becomes easy"), clue(hypothesis.executionSteps))
    assert(!rendered.contains("candidate move"), clue(rendered))
    assert(!rendered.contains("opponent_breaks:"), clue(rendered))
    assert(!rendered.contains("central_control_gain"), clue(rendered))
  }

  test("flank clamp candidates require actual enemy-pawn contact") {
    val contact = position("4k3/8/8/7p/8/7P/8/6K1 w - - 0 1")
    val noContact = position("4k3/8/8/8/8/7P/8/6K1 w - - 0 1")

    val contactMoves =
      RestrictionPlanEvidence.movesForSubplan(
        contact,
        PlanTaxonomy.PlanKind.FlankClamp,
        contact.legalMoves.toList
      )
    val noContactMoves =
      RestrictionPlanEvidence.movesForSubplan(
        noContact,
        PlanTaxonomy.PlanKind.FlankClamp,
        noContact.legalMoves.toList
      )

    assert(contactMoves.exists(mv => mv.orig == Square.H3 && mv.dest == Square.H4), clue(contactMoves.map(_.toUci.uci)))
    assertEquals(noContactMoves, Nil)
  }

  test("central-space bind candidates need a board-backed central-control gain") {
    val pos = position("4k3/8/8/8/8/8/4P3/4K3 w - - 0 1")
    val moves =
      RestrictionPlanEvidence.movesForSubplan(
        pos,
        PlanTaxonomy.PlanKind.CentralSpaceBind,
        pos.legalMoves.toList
      )

    assert(moves.exists(mv => mv.piece.role == Pawn && mv.orig == Square.E2 && mv.dest == Square.E4), clue(moves.map(_.toUci.uci)))
  }

  test("mobility-suppression candidates are quiet restriction moves") {
    val pos = position("4k3/8/7b/8/8/8/4P3/4K3 w - - 0 1")
    val moves =
      RestrictionPlanEvidence.movesForSubplan(
        pos,
        PlanTaxonomy.PlanKind.MobilitySuppression,
        pos.legalMoves.toList
      )

    assert(moves.nonEmpty, clue(pos.legalMoves.toList.map(_.toUci.uci)))
    assert(moves.forall(!_.captures), clue(moves.map(_.toUci.uci)))
    assert(moves.exists(mv => mv.piece.role == Pawn && mv.orig == Square.E2 && mv.dest == Square.E3), clue(moves.map(_.toUci.uci)))
  }

  test("mobility suppression does not relabel material captures") {
    val pos = position("4k3/8/8/5q2/8/6N1/8/4K3 w - - 0 1")
    val moves =
      RestrictionPlanEvidence.movesForSubplan(
        pos,
        PlanTaxonomy.PlanKind.MobilitySuppression,
        pos.legalMoves.toList
      )

    assert(!moves.exists(_.captures), clue(moves.map(_.toUci.uci)))
  }

  test("move-level restriction does not duplicate specific candidates as generic prophylaxis") {
    val fen = "4k3/8/8/5n2/8/5N2/8/4K3 w - - 0 1"
    val pos = position(fen)
    val genericMoves =
      RestrictionPlanEvidence.movesForSubplan(
        pos,
        PlanTaxonomy.PlanKind.ProphylaxisRestraint,
        pos.legalMoves.toList
      )
    val hypotheses = RestrictionPlanEvidence.planHypotheses(fen, White)

    assertEquals(genericMoves, Nil)
    assert(hypotheses.exists(_.subplanId.contains(PlanTaxonomy.PlanKind.KeySquareDenial.id)))
    assert(
      !hypotheses.exists(_.subplanId.contains(PlanTaxonomy.PlanKind.ProphylaxisRestraint.id)),
      clue(hypotheses.map(h => h.subplanId -> h.evidenceSources))
    )
  }

  test("plan-first proposals can emit typed restriction subplans") {
    val fen = "4k3/8/8/5n2/8/5N2/8/4K3 w - - 0 1"
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
        maxItems = 5
      )

    assert(
      hypotheses.exists(_.subplanId.contains(PlanTaxonomy.PlanKind.KeySquareDenial.id)),
      clue(hypotheses.map(h => h.planId -> h.subplanId -> h.evidenceSources))
    )
  }

  test("plan-first proposals can emit typed mobility-suppression subplans") {
    val fen = "4k3/8/7b/8/8/8/4P3/4K3 w - - 0 1"
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
      hypotheses.exists(_.subplanId.contains(PlanTaxonomy.PlanKind.MobilitySuppression.id)),
      clue(hypotheses.map(h => h.planId -> h.subplanId -> h.evidenceSources))
    )
  }
