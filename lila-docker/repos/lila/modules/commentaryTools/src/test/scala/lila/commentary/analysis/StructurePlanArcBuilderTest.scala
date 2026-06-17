package lila.commentary.analysis

import munit.FunSuite
import lila.commentary.model.*
import lila.commentary.model.authoring.*

class StructurePlanArcBuilderTest extends FunSuite:

  private def baseContext(
      playedMove: String,
      playedSan: String,
      mainPlan: String,
      structure: StructureProfileInfo,
      alignment: PlanAlignmentInfo,
      pieceActivity: List[PieceActivityInfo],
      preventedPlans: List[PreventedPlanInfo] = Nil,
      fen: String = "4k3/8/8/8/8/8/8/4K3 w - - 0 1"
  ): NarrativeContext =
    NarrativeContext(
      fen = fen,
      header = ContextHeader("Middlegame", "Normal", "NarrowChoice", "Medium", "ExplainPlan"),
      ply = 24,
      playedMove = Some(playedMove),
      playedSan = Some(playedSan),
      summary = NarrativeSummary(mainPlan, None, "NarrowChoice", "Maintain", "0.00"),
      threats = ThreatTable(Nil, Nil),
      pawnPlay = PawnPlayTable(false, None, "Low", "Maintain", "Quiet", "Background", None, false, "quiet"),
      plans = PlanTable(
        top5 = List(PlanRow(1, mainPlan, 0.82, List(s"supports $mainPlan"), confidence = ConfidenceLevel.Heuristic)),
        suppressed = Nil
      ),
      delta = None,
      phase = PhaseContext("Middlegame", "Balanced middlegame"),
      candidates = Nil,
      semantic = Some(
        SemanticSection(
          structuralWeaknesses = Nil,
          pieceActivity = pieceActivity,
          positionalFeatures = Nil,
          compensation = None,
          endgameFeatures = None,
          practicalAssessment = None,
          preventedPlans = preventedPlans,
          conceptSummary = Nil,
          structureProfile = Some(structure),
          planAlignment = Some(alignment)
        )
      ),
      mainStrategicPlans = List(
        PlanHypothesis(
          planId = mainPlan.toLowerCase.replace(' ', '_'),
          planName = mainPlan,
          rank = 1,
          score = 0.86,
          preconditions = Nil,
          executionSteps = Nil,
          failureModes = Nil,
          viability = PlanViability(0.81, "high", "slow burn")
        )
      ),
      renderMode = NarrativeRenderMode.MoveReview
    )

  test("carlsbad minority attack chooses rook deployment and immediate move contribution") {
    val ctx = baseContext(
      playedMove = "a1b1",
      playedSan = "Rb1",
      mainPlan = "Minority Attack",
      structure = StructureProfileInfo("Carlsbad", 0.87, Nil, "Locked", List("MAJORITY")),
      alignment = PlanAlignmentInfo(
        score = 74,
        band = "Playable",
        matchedPlanIds = List("minority_attack"),
        missingPlanIds = Nil,
        reasonCodes = List("PA_MATCH"),
        narrativeIntent = Some("build queenside pressure"),
        narrativeRisk = Some("the center can open if the pawn break is rushed")
      ),
      pieceActivity = List(
        PieceActivityInfo("Rook", "a1", 0.40, false, false, List("b1", "b3"), List("b4"))
      ),
      fen = "4k3/8/8/8/8/8/8/R3K3 w - - 0 1"
    )

    val arc = StructurePlanArcBuilder.build(ctx).getOrElse(fail("missing structure arc"))
    assertEquals(arc.structureLabel, "Carlsbad")
    assertEquals(arc.planLabel, "Minority Attack")
    assertEquals(arc.primaryDeployment.piece, "R")
    assertEquals(arc.primaryDeployment.destination, "b-file")
    assert(StructurePlanArcBuilder.proseEligible(arc))
    assertEquals(arc.moveContribution, "This move starts that route immediately.")
  }

  test("route contribution does not trust UCI text when the played move cannot replay") {
    val ctx = baseContext(
      playedMove = "a1b1",
      playedSan = "Rb1",
      mainPlan = "Minority Attack",
      structure = StructureProfileInfo("Carlsbad", 0.87, Nil, "Locked", List("MAJORITY")),
      alignment = PlanAlignmentInfo(
        score = 74,
        band = "Playable",
        matchedPlanIds = List("minority_attack"),
        missingPlanIds = Nil,
        reasonCodes = List("PA_MATCH"),
        narrativeIntent = Some("build queenside pressure"),
        narrativeRisk = Some("the center can open if the pawn break is rushed")
      ),
      pieceActivity = List(
        PieceActivityInfo("Rook", "a1", 0.40, false, false, List("b1", "b3"), List("b4"))
      )
    )

    val arc = StructurePlanArcBuilder.build(ctx).getOrElse(fail("missing structure arc"))
    assertEquals(
      arc.moveContribution,
      "The structure-plan evidence keeps that route tied to minority attack."
    )
    assert(!arc.moveContribution.toLowerCase.contains("easier"), clue(arc.moveContribution))
  }

  test("iqp activation selects piece activation before the break") {
    val ctx = baseContext(
      playedMove = "d2f3",
      playedSan = "Nf3",
      mainPlan = "Central Break",
      structure = StructureProfileInfo("IQP", 0.83, Nil, "Open", List("IQP")),
      alignment = PlanAlignmentInfo(
        score = 79,
        band = "OnBook",
        matchedPlanIds = List("central_break"),
        missingPlanIds = Nil,
        reasonCodes = List("PA_MATCH"),
        narrativeIntent = Some("prepare the central break"),
        narrativeRisk = Some("the break fails if White is still underdeveloped")
      ),
      pieceActivity = List(
        PieceActivityInfo("Knight", "d2", 0.33, false, false, List("f3", "e5"), List("e5"))
      )
    )

    val arc = StructurePlanArcBuilder.build(ctx).getOrElse(fail("missing structure arc"))
    assertEquals(arc.primaryDeployment.piece, "N")
    assert(arc.primaryDeployment.purpose.toLowerCase.contains("before the break"))
    assert(StructurePlanArcBuilder.supportPrimaryText(arc).toLowerCase.contains("break"))
  }

  test("hedgehog route carries prophylaxis support") {
    val ctx = baseContext(
      playedMove = "c1d2",
      playedSan = "Bd2",
      mainPlan = "Counterplay Restraint",
      structure = StructureProfileInfo("Hedgehog", 0.78, Nil, "Closed", List("HEDGEHOG")),
      alignment = PlanAlignmentInfo(
        score = 68,
        band = "Playable",
        matchedPlanIds = List("counterplay_restraint"),
        missingPlanIds = Nil,
        reasonCodes = List("PA_MATCH"),
        narrativeIntent = Some("hold the structure and restrain counterplay"),
        narrativeRisk = Some("premature pawn breaks loosen the shell")
      ),
      pieceActivity = List(
        PieceActivityInfo("Bishop", "c1", 0.28, false, false, List("d2", "e3"), List("e3", "c5"))
      ),
      preventedPlans = List(
        PreventedPlanInfo("...b5 break", Nil, Some("b5"), 0, 120, Some("counterplay"))
      )
    )

    val arc = StructurePlanArcBuilder.build(ctx).getOrElse(fail("missing structure arc"))
    assert(arc.primaryDeployment.purpose.toLowerCase.contains("counterplay"))
    assert(arc.prophylaxisSupport.exists(_.toLowerCase.contains("cuts out counterplay")))
  }

  test("french structure supports exact reroute chains for high confidence cues") {
    val ctx = baseContext(
      playedMove = "d2f1",
      playedSan = "Nf1",
      mainPlan = "Restrict the Entrenched Knight",
      structure = StructureProfileInfo("French Chain", 0.80, Nil, "Closed", List("ENTRENCHED")),
      alignment = PlanAlignmentInfo(
        score = 70,
        band = "Playable",
        matchedPlanIds = List("restriction_play"),
        missingPlanIds = Nil,
        reasonCodes = List("PA_MATCH"),
        narrativeIntent = Some("reroute toward e3 and g4"),
        narrativeRisk = Some("Black can free the game with ...c5")
      ),
      pieceActivity = List(
        PieceActivityInfo("Knight", "d2", 0.24, false, false, List("f1", "e3", "g4"), List("e3", "g4", "h6"))
      )
    )

    val arc = StructurePlanArcBuilder.build(ctx).getOrElse(fail("missing structure arc"))
    assert(arc.primaryDeployment.confidence >= StructurePlanArcBuilder.ExactRouteCutoff, clues(arc.primaryDeployment))
    assert(StructurePlanArcBuilder.useExactRoute(arc.primaryDeployment))
    assertEquals(arc.primaryDeployment.surfaceMode, lila.commentary.RouteSurfaceMode.Exact)
    assert(StructurePlanArcBuilder.claimText(arc).contains("French Chain"))
    assert(StructurePlanArcBuilder.claimText(arc).toLowerCase.contains("knight"))
  }

  test("queen deployment never surfaces as exact path") {
    val ctx = baseContext(
      playedMove = "d1d3",
      playedSan = "Qd3",
      mainPlan = "Whole-board pressure",
      structure = StructureProfileInfo("Semi-Open Center", 0.82, Nil, "Semi-Open", List("SPACE")),
      alignment = PlanAlignmentInfo(
        score = 81,
        band = "OnBook",
        matchedPlanIds = List("whole_board_pressure"),
        missingPlanIds = Nil,
        reasonCodes = List("PA_MATCH"),
        narrativeIntent = Some("centralize the queen before switching wings"),
        narrativeRisk = Some("entry squares can be overextended")
      ),
      pieceActivity = List(
        PieceActivityInfo("Queen", "d1", 0.12, false, false, List("d3", "c4", "c5"), List("c5"))
      )
    )

    val arc = StructurePlanArcBuilder.build(ctx).getOrElse(fail("missing structure arc"))
    assertEquals(arc.primaryDeployment.surfaceMode, lila.commentary.RouteSurfaceMode.Toward)
    assert(!StructurePlanArcBuilder.useExactRoute(arc.primaryDeployment))
  }

  test("off-plan structures without matched typed plan stay out of structure arc") {
    val ctx = baseContext(
      playedMove = "a1b1",
      playedSan = "Rb1",
      mainPlan = "Minority Attack",
      structure = StructureProfileInfo("Carlsbad", 0.87, Nil, "Locked", List("MAJORITY")),
      alignment = PlanAlignmentInfo(
        score = 34,
        band = "OffPlan",
        matchedPlanIds = Nil,
        missingPlanIds = List("minority_attack"),
        reasonCodes = List("ANTI_PLAN"),
        narrativeIntent = Some("build queenside pressure"),
        narrativeRisk = Some("the move order fights the usual minority-attack timing")
      ),
      pieceActivity = List(
        PieceActivityInfo("Rook", "a1", 0.38, false, false, List("b1", "b3"), List("b4"))
      )
    )

    assertEquals(StructurePlanArcBuilder.build(ctx), None)
  }
