package lila.llm.analysis.structure

import munit.FunSuite
import chess.Color
import lila.llm.analysis.L3.BreakAnalyzer
import lila.llm.model.{ Plan, PlanMatch }
import lila.llm.model.structure.{ AlignmentBand, CenterState, StructureId, StructureProfile }

class PlanAlignmentScorerTest extends FunSuite:

  private def pm(plan: Plan, score: Double = 0.9): PlanMatch =
    PlanMatch(plan, score, Nil)

  test("scores aligned carlsbad plan as on-book") {
    val profile = StructureProfile(
      primary = StructureId.Carlsbad,
      confidence = 0.91,
      alternatives = List(StructureId.LockedCenter),
      centerState = CenterState.Locked,
      evidenceCodes = List("CARLSBAD_CORE")
    )
    val entry = StructuralPlaybook.lookup(StructureId.Carlsbad).getOrElse(fail("missing playbook"))
    val pawn = BreakAnalyzer.noPawnPlay.copy(minorityAttack = true, pawnBreakReady = true)

    val alignment = PlanAlignmentScorer.score(
      structureProfile = profile,
      playbookEntry = entry,
      topPlans = List(pm(Plan.MinorityAttack(Color.White))),
      motifs = Nil,
      pawnAnalysis = Some(pawn),
      sideToMove = Color.White
    )

    assertEquals(alignment.band, AlignmentBand.OnBook)
    assert(alignment.score >= 70, clues(alignment))
    assert(alignment.reasonCodes.contains("PA_MATCH"), clues(alignment))
    assert(alignment.reasonWeights.getOrElse("PA_MATCH", 0.0) > 0.0, clues(alignment.reasonWeights))
    assert(alignment.narrativeIntent.exists(_.nonEmpty), clues(alignment))
    assert(alignment.narrativeRisk.exists(_.nonEmpty), clues(alignment))
  }

  test("applies anti-plan penalty on counter-plan primary") {
    val profile = StructureProfile(
      primary = StructureId.Carlsbad,
      confidence = 0.86,
      alternatives = Nil,
      centerState = CenterState.Locked,
      evidenceCodes = List("CARLSBAD_CORE")
    )
    val entry = StructuralPlaybook.lookup(StructureId.Carlsbad).getOrElse(fail("missing playbook"))

    val alignment = PlanAlignmentScorer.score(
      structureProfile = profile,
      playbookEntry = entry,
      topPlans = List(pm(Plan.PawnStorm(Color.White, "kingside"))),
      motifs = Nil,
      pawnAnalysis = Some(BreakAnalyzer.noPawnPlay),
      sideToMove = Color.White
    )

    assertEquals(alignment.band, AlignmentBand.OffPlan)
    assert(alignment.reasonCodes.contains("ANTI_PLAN"), clues(alignment))
    assert(alignment.reasonWeights.getOrElse("ANTI_PLAN", 0.0) > 0.0, clues(alignment.reasonWeights))
  }

  test("returns unknown band when structure confidence is low") {
    val profile = StructureProfile(
      primary = StructureId.MaroczyBind,
      confidence = 0.55,
      alternatives = Nil,
      centerState = CenterState.Fluid,
      evidenceCodes = List("LOW_CONF")
    )
    val entry = StructuralPlaybook.lookup(StructureId.MaroczyBind).getOrElse(fail("missing playbook"))

    val alignment = PlanAlignmentScorer.score(
      structureProfile = profile,
      playbookEntry = entry,
      topPlans = List(pm(Plan.SpaceAdvantage(Color.White))),
      motifs = Nil,
      pawnAnalysis = Some(BreakAnalyzer.noPawnPlay),
      sideToMove = Color.White
    )

    assertEquals(alignment.band, AlignmentBand.Unknown)
    assert(alignment.reasonCodes.contains("LOW_CONF"), clues(alignment))
    assert(alignment.reasonWeights.getOrElse("LOW_CONF", 0.0) > 0.0, clues(alignment.reasonWeights))
  }

  test("does not apply anti-plan penalty when top plan confidence is too low") {
    val profile = StructureProfile(
      primary = StructureId.Carlsbad,
      confidence = 0.90,
      alternatives = Nil,
      centerState = CenterState.Locked,
      evidenceCodes = List("REQ_D_PAWN_TENSION")
    )
    val entry = StructuralPlaybook.lookup(StructureId.Carlsbad).getOrElse(fail("missing playbook"))

    val alignment = PlanAlignmentScorer.score(
      structureProfile = profile,
      playbookEntry = entry,
      topPlans = List(pm(Plan.PawnStorm(Color.White, "kingside"), score = 0.30)),
      motifs = Nil,
      pawnAnalysis = Some(BreakAnalyzer.noPawnPlay),
      sideToMove = Color.White
    )

    assert(!alignment.reasonCodes.contains("ANTI_PLAN"), clues(alignment))
    assert(alignment.reasonWeights.getOrElse("ANTI_PLAN", 0.0) == 0.0, clues(alignment.reasonWeights))
  }
