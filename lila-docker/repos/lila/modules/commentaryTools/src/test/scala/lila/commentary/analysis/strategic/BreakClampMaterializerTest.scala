package lila.commentary.analysis.strategic

import chess.Color
import chess.format.Fen
import chess.variant.Standard
import lila.commentary.model.FactScope
import lila.commentary.model.strategic.VariationLine
import munit.FunSuite

class BreakClampMaterializerTest extends FunSuite:

  test("materializes a route-shaped named break when the played move occupies the break destination") {
    val plans =
      materialize(
        fen = "4k3/8/8/1p6/8/2P5/1P6/4K3 w - - 0 1",
        line = List("b2b4", "e8e7", "e1e2", "e7e6")
      )

    val breakPlan = plans.find(_.breakNeutralized.contains("...b5-b4"))
    assert(breakPlan.nonEmpty, clues(plans))
    assert(breakPlan.flatMap(_.deniedResourceClass).contains("break"), clues(breakPlan))
    assert(breakPlan.flatMap(_.deniedEntryScope).contains("file"), clues(breakPlan))
    assert(breakPlan.map(_.sourceScope).contains(FactScope.Now), clues(breakPlan))
    assert(breakPlan.map(_.counterplayScoreDrop).contains(0), clues(breakPlan))
    assert(breakPlan.exists(_.mobilityDelta < 0), clues(breakPlan))
    assert(breakPlan.map(_.deniedSquares.map(_.key)).contains(List("b4")), clues(breakPlan))
  }

  test("materializes the opponent route instead of the played destination in opening center clamps") {
    val plans =
      BreakClampMaterializer.materialize(
        fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1",
        board = Fen.read(Standard, Fen.Full("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1"))
          .map(_.board)
          .getOrElse(fail("bad opening FEN")),
        color = Color.Black,
        mainLine = VariationLine(moves = List("e7e5", "g1f3", "b8c6", "d2d4"), scoreCp = 0, depth = 16)
      )

    assert(plans.exists(_.breakNeutralized.contains("e4-e5")), clues(plans))
    assert(!plans.exists(_.breakNeutralized.contains("e5")), clues(plans))
  }

  test("marks a route clamp as a pinned break pawn when Bf4 pins the d6 pawn") {
    val fen = "1k1rr3/pp3ppp/3p1b2/1qp2Q2/4P3/2P1BP2/PP4PP/2KR3R w - - 3 17"
    val evidence =
      routeEvidence(fen = fen, line = List("e3f4"))
        .find(_.routeId == "black:d6-d5:quiet_push")
        .getOrElse(fail("missing d6-d5 route evidence"))

    assert(evidence.mechanismTerms.contains("break_clamp_mechanism:pinned_pawn"), clues(evidence))
    assert(evidence.mechanismTerms.contains("pinned_break_pawn:d6"), clues(evidence))
    assert(evidence.mechanismTerms.contains("pin_attacker:f4"), clues(evidence))
    assert(evidence.mechanismTerms.contains("pin_king:b8"), clues(evidence))
  }

  test("marks a route clamp as a pinned break pawn when Qa4 pins the c6 pawn") {
    val fen = "rn1qk2r/pp3ppp/2p1pn2/P4b2/1bpP4/2N1PN2/1P3PPP/R1BQKB1R w KQkq - 1 8"
    val evidence =
      routeEvidence(fen = fen, line = List("d1a4"))
        .find(_.routeId == "black:c6-c5:quiet_push")
        .getOrElse(fail("missing c6-c5 route evidence"))

    assert(evidence.mechanismTerms.contains("break_clamp_mechanism:pinned_pawn"), clues(evidence))
    assert(evidence.mechanismTerms.contains("pinned_break_pawn:c6"), clues(evidence))
    assert(evidence.mechanismTerms.contains("pin_attacker:a4"), clues(evidence))
    assert(evidence.mechanismTerms.contains("pin_king:e8"), clues(evidence))
  }

  test("classifies a same-destination capture as a transform risk rather than route restoration") {
    val fen = "4k3/8/8/1pp5/8/2P5/1P6/4K3 w - - 0 1"
    val line = List("b2b4", "e8e7", "e1e2", "e7e6")
    val board = Fen.read(Standard, Fen.Full(fen)).map(_.board).getOrElse(fail(s"bad FEN: $fen"))
    val evidence =
      BreakClampMaterializer.routeEvidence(
        fen = fen,
        board = board,
        color = Color.White,
        mainLine = VariationLine(moves = line, scoreCp = 0, depth = 16)
      )

    val route =
      evidence
        .find(_.routeId == "black:b5-b4:quiet_push")
        .getOrElse(fail(s"missing b5-b4 route evidence: $evidence"))
    assertEquals(route.token, "...b5-b4")
    assertEquals(route.transformRisk, BreakClampMaterializer.BreakTransformRisk.CaptureTransform)
    assertEquals(route.transformRoutes, List("black:c5-b4:capture_break"))

    val breakPlan = materialize(fen = fen, line = line).find(_.breakNeutralized.contains("...b5-b4"))
    assert(breakPlan.nonEmpty, clues(materialize(fen = fen, line = line)))
    assert(breakPlan.exists(_.mobilityDelta < 0), clues(breakPlan))
  }

  test("classifies a same-destination capture without immediate recapture as unanswered") {
    val fen = "4k3/8/8/1pp5/8/8/1P6/4K3 w - - 0 1"
    val route =
      routeEvidence(fen = fen, line = List("b2b4", "e8e7"))
        .find(_.routeId == "black:b5-b4:quiet_push")
        .getOrElse(fail("missing b5-b4 route evidence"))

    assertEquals(route.transformAssessments.map(_.routeId), List("black:c5-b4:capture_break"))
    assertEquals(
      route.transformAssessments.map(_.shape),
      List(BreakClampMaterializer.BreakTransformShape.SameDestinationCapture)
    )
    assertEquals(
      route.transformAssessments.map(_.verdict),
      List(BreakClampMaterializer.BreakTransformVerdict.UnansweredCapture)
    )
    assertEquals(route.transformAssessments.flatMap(_.immediateRecapture), Nil)
  }

  test("classifies a recapturable same-destination capture as unproven rather than authority") {
    val fen = "4k3/8/8/1pp5/8/2P5/1P6/4K3 w - - 0 1"
    val route =
      routeEvidence(fen = fen, line = List("b2b4", "e8e7"))
        .find(_.routeId == "black:b5-b4:quiet_push")
        .getOrElse(fail("missing b5-b4 route evidence"))

    val assessment = route.transformAssessments.headOption.getOrElse(fail("missing transform assessment"))
    assertEquals(assessment.routeId, "black:c5-b4:capture_break")
    assertEquals(assessment.captureDestinationIsPlayedDestination, true)
    assertEquals(assessment.immediateRecapture, Some("c3b4"))
    assertEquals(assessment.releaseRoutesAfterRecapture, Nil)
    assertEquals(assessment.verdict, BreakClampMaterializer.BreakTransformVerdict.RecaptureAvailableUnproven)
  }

  test("classifies a branch-proven same-destination capture recapture as harmless") {
    val fen = "4k3/8/8/1pp5/8/2P5/1P6/4K3 w - - 0 1"
    val route =
      routeEvidence(fen = fen, line = List("b2b4", "c5b4", "c3b4", "e8e7"))
        .find(_.routeId == "black:b5-b4:quiet_push")
        .getOrElse(fail("missing b5-b4 route evidence"))

    val assessment = route.transformAssessments.headOption.getOrElse(fail("missing transform assessment"))
    assertEquals(assessment.routeId, "black:c5-b4:capture_break")
    assertEquals(assessment.shape, BreakClampMaterializer.BreakTransformShape.SameDestinationCapture)
    assertEquals(assessment.captureDestinationIsPlayedDestination, true)
    assertEquals(assessment.immediateRecapture, Some("c3b4"))
    assertEquals(assessment.releaseRoutesAfterRecapture, Nil)
    assertEquals(assessment.verdict.toString, "RecaptureProvenHarmless")
  }

  test("classifies recapture that leaves another same-destination break as still releasing") {
    val fen = "4k3/8/8/ppp5/8/2P5/1P6/4K3 w - - 0 1"
    val route =
      routeEvidence(fen = fen, line = List("b2b4", "e8e7"))
        .find(_.routeId == "black:b5-b4:quiet_push")
        .getOrElse(fail("missing b5-b4 route evidence"))

    assertEquals(route.transformAssessments.map(_.routeId), List("black:a5-b4:capture_break", "black:c5-b4:capture_break"))
    assertEquals(
      route.transformAssessments.map(_.verdict).distinct,
      List(BreakClampMaterializer.BreakTransformVerdict.RecaptureStillReleases)
    )
    assert(route.transformAssessments.forall(_.immediateRecapture.contains("c3b4")), clues(route.transformAssessments))
    assert(route.transformAssessments.forall(_.releaseRoutesAfterRecapture.nonEmpty), clues(route.transformAssessments))
  }

  test("does not materialize when no legal null-turn break exists before the move") {
    val plans =
      materialize(
        fen = "4k3/8/8/8/8/8/4P3/4K3 w - - 0 1",
        line = List("e2e3")
      )

    assertEquals(plans, Nil)
  }

  test("does not materialize when the same break remains legal after the move") {
    val plans =
      materialize(
        fen = "4k3/8/8/1p6/P7/8/7P/4K3 w - - 0 1",
        line = List("h2h3")
      )

    assertEquals(plans, Nil)
  }

  test("does not materialize when the same break returns in the first four plies") {
    val plans =
      materialize(
        fen = "4k3/8/8/np6/8/2P5/1P6/4K3 w - - 0 1",
        line = List("b2b4", "e8e7", "b4a5", "b5b4")
      )

    assertEquals(plans.find(_.breakNeutralized.contains("...b4")), None, clues(plans))
  }

  test("does not materialize from a capture, check, or promotion first move") {
    val plans =
      materialize(
        fen = "4k3/8/8/1p6/P7/8/8/4K3 w - - 0 1",
        line = List("a4b5")
      )

    assertEquals(plans, Nil)
  }

  test("does not materialize a pawn capture that only attacks the moved piece on its origin square") {
    val fen = "4k3/8/5n2/4P3/8/8/8/4K3 b - - 0 1"
    val plans =
      BreakClampMaterializer.materialize(
        fen = fen,
        board = Fen.read(Standard, Fen.Full(fen)).map(_.board).getOrElse(fail(s"bad FEN: $fen")),
        color = Color.Black,
        mainLine = VariationLine(moves = List("f6d7", "e1e2", "e8e7", "e2e3"), scoreCp = 0, depth = 16)
      )

    assert(!plans.exists(_.breakNeutralized.contains("f6")), clues(plans))
  }

  test("prophylaxis analyzer appends materialized clamp plans without changing its public contract") {
    val fen = "4k3/8/8/1p6/8/2P5/1P6/4K3 w - - 0 1"
    val plans =
      ProphylaxisAnalyzerImpl().analyze(
        fen = fen,
        board = Fen.read(Standard, Fen.Full(fen)).map(_.board).getOrElse(fail(s"bad FEN: $fen")),
        color = Color.White,
        mainLine = VariationLine(moves = List("b2b4", "e8e7", "e1e2", "e7e6"), scoreCp = 0, depth = 16),
        threatLine = None
      )

    val breakPlan = plans.find(_.breakNeutralized.contains("...b5-b4"))
    assert(breakPlan.nonEmpty, clues(plans))
    assert(breakPlan.map(_.sourceScope).contains(FactScope.Now), clues(breakPlan))
  }

  test("prophylaxis analyzer extracts break file from underscore square plan ids") {
    val fen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1"
    val plans =
      ProphylaxisAnalyzerImpl().analyze(
        fen = fen,
        board = Fen.read(Standard, Fen.Full(fen)).map(_.board).getOrElse(fail(s"bad FEN: $fen")),
        color = Color.White,
        mainLine = VariationLine(moves = Nil, scoreCp = 140, depth = 16),
        threatLine = Some(VariationLine(moves = Nil, scoreCp = 0, depth = 16)),
        explicitPlanId = Some("neutralize_f5_break")
      )

    val plan =
      plans.find(_.planId == "neutralize_f5_break").getOrElse(fail(s"missing explicit plan: $plans"))

    assertEquals(plan.breakNeutralized, Some("f"), clues(plan))
    assertEquals(plan.deniedEntryScope, Some("file"), clues(plan))
  }

  private def materialize(fen: String, line: List[String]) =
    BreakClampMaterializer.materialize(
      fen = fen,
      board = Fen.read(Standard, Fen.Full(fen)).map(_.board).getOrElse(fail(s"bad FEN: $fen")),
      color = Color.White,
      mainLine = VariationLine(moves = line, scoreCp = 0, depth = 16)
    )

  private def routeEvidence(fen: String, line: List[String]) =
    BreakClampMaterializer.routeEvidence(
      fen = fen,
      board = Fen.read(Standard, Fen.Full(fen)).map(_.board).getOrElse(fail(s"bad FEN: $fen")),
      color = Color.White,
      mainLine = VariationLine(moves = line, scoreCp = 0, depth = 16)
    )
