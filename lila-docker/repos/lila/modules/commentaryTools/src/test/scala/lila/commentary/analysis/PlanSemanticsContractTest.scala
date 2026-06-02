package lila.commentary.analysis

import lila.commentary.StrategicIdeaKind
import lila.commentary.analysis.semantic.StrategicObservationIds.ProofFamilyId
import munit.FunSuite

final class PlanSemanticsContractTest extends FunSuite:

  import PlanTaxonomy.PlanKind
  import PlanTaxonomy.PlanTheme

  test("every PlanKind has a semantics contract") {
    val missing =
      PlanKind.values.toList.filter(kind => PlanSemanticsContract.forKind(kind).isEmpty)

    assertEquals(missing.map(_.id), Nil)
  }

  test("contract keeps opening and tactical subplans out of strategic support") {
    assertEquals(
      PlanSemanticsContract.strategicIdeaKinds(PlanKind.OpeningDevelopment),
      Set.empty[String]
    )
    assertEquals(
      PlanSemanticsContract.strategicIdeaKinds(PlanKind.ForcingTacticalShot),
      Set.empty[String]
    )
  }

  test("contract owns strategic idea mapping for representative subplans") {
    assert(
      PlanSemanticsContract
        .strategicIdeaKinds(PlanKind.BreakPrevention)
        .contains(StrategicIdeaKind.Prophylaxis)
    )
    assert(
      PlanSemanticsContract
        .strategicIdeaKinds(PlanKind.BreakPrevention)
        .contains(StrategicIdeaKind.CounterplaySuppression)
    )
    assertEquals(
      PlanSemanticsContract.strategicIdeaKinds(PlanKind.CentralBreakTiming),
      Set(StrategicIdeaKind.PawnBreak)
    )
    assertEquals(
      PlanSemanticsContract.strategicIdeaKinds(PlanKind.DefenderTrade),
      Set(StrategicIdeaKind.FavorableTradeOrTransformation)
    )
  }

  test("fallback is unsafe for tactical, opening-only, and unknown themes") {
    assertEquals(
      PlanSemanticsContract.fallbackPolicy(PlanTheme.ImmediateTacticalGain).mayEmitStrategicFallback,
      false
    )
    assertEquals(
      PlanSemanticsContract.fallbackPolicy(PlanTheme.OpeningPrinciples).mayEmitStrategicFallback,
      false
    )
    assertEquals(
      PlanSemanticsContract.fallbackPolicy(PlanTheme.Unknown).mayEmitStrategicFallback,
      false
    )
  }

  test("semantics contract preserves current probe purpose selection") {
    PlanKind.values.foreach { kind =>
      assertEquals(
        PlanSemanticsContract.forKind(kind).map(_.probePurpose),
        Some(ThemePlanProbePurpose.purposeForSubplan(kind))
      )
    }
  }

  test("semantics contract owns proof-family and trigger-kind projection") {
    assertEquals(
      PlanSemanticsContract.proofFamily(PlanKind.BreakPrevention),
      ProofFamilyId.NeutralizeKeyBreak.wireKey
    )
    assertEquals(
      PlanSemanticsContract.proofFamily(PlanKind.OpenFilePressure),
      ProofFamilyId.HalfOpenFilePressure.wireKey
    )
    assertEquals(
      PlanSemanticsContract.triggerKind(PlanKind.DefenderTrade),
      ProofFamilyId.TradeKeyDefender.wireKey
    )
    assertEquals(
      PlanSemanticsContract.triggerKind(PlanKind.ProphylaxisRestraint),
      ProofFamilyId.CounterplayRestraint.wireKey
    )
  }

  test("theme-only projection remains broad and does not create a default subplan proof") {
    assertEquals(
      PlanSemanticsContract.proofFamily(PlanTheme.PieceRedeployment, None),
      PlanTheme.PieceRedeployment.id
    )
    assertEquals(
      PlanSemanticsContract.triggerKind(PlanTheme.PieceRedeployment, None),
      PlanTheme.PieceRedeployment.id
    )
  }
