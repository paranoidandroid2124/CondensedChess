package lila.commentary.analysis.claim

import lila.commentary.analysis.*
import lila.commentary.model.authoring.AuthorQuestionKind
import munit.FunSuite

class ClaimAuthorityResolverBoundaryTest extends FunSuite:

  test("authority decision has one public admission ladder with no extra roles") {
    assertEquals(
      ClaimAuthorityTier.values.toList,
      List(
        ClaimAuthorityTier.CertifiedOwner,
        ClaimAuthorityTier.SupportedLocal,
        ClaimAuthorityTier.DiagnosticOnly,
        ClaimAuthorityTier.Suppressed
      )
    )
  }

  test("authority decision admits only non-suppressed strategic claim tiers") {
    assert(ClaimAuthorityDecision(ClaimAuthorityTier.CertifiedOwner).admitted)
    assert(ClaimAuthorityDecision(ClaimAuthorityTier.SupportedLocal).admitted)
    assert(!ClaimAuthorityDecision(ClaimAuthorityTier.DiagnosticOnly).admitted)
    assert(!ClaimAuthorityDecision(ClaimAuthorityTier.Suppressed).admitted)
  }

  test("authority decision exposes failure codes while preserving tactical veto compatibility") {
    val decision =
      ClaimAuthorityDecision(
        tier = ClaimAuthorityTier.Suppressed,
        failureCodes = List("contract:deferred", "truth_contract_tactical_refutation")
      )

    assertEquals(decision.failureCodes, List("contract:deferred", "truth_contract_tactical_refutation"))
    assertEquals(decision.vetoReasons, List("truth_contract_tactical_refutation"))
  }

  test("missing tactical context suppresses supported position probe plans") {
    val decision =
      ClaimAuthorityResolver.shouldTacticalVetoPlan(
        ctx = None,
        inputs = minimalInputs(),
        truthContract = None,
        plan = supportedPositionProbePlan()
      )

    assertEquals(decision.map(_.tier), Some(ClaimAuthorityTier.Suppressed))
    assert(decision.exists(_.vetoReasons.contains("tactical_context_missing")))
  }

  test("supported local surface preserves proper opening-family capitalization") {
    val surface =
      ClaimAuthorityResolver.supportedLocalSurface(
        "Carlsbad structure is solid."
      )

    assertEquals(surface, "A key idea is that Carlsbad structure is solid.")
  }

  private def minimalInputs(): QuestionPlannerInputs =
    QuestionPlannerInputs(
      mainBundle = None,
      quietIntent = None,
      decisionFrame = CertifiedDecisionFrame(),
      decisionComparison = None,
      alternativeNarrative = None,
      truthMode = PlayerFacingTruthMode.Strategic,
      preventedPlansNow = Nil,
      pvDelta = None,
      counterfactual = None,
      practicalAssessment = None,
      opponentThreats = Nil,
      forcingThreats = Nil,
      evidenceByQuestionId = Map.empty,
      candidateEvidenceLines = Nil,
      evidenceBackedPlans = Nil,
      opponentPlan = None,
      factualFallback = None
    )

  private def supportedPositionProbePlan(): QuestionPlan =
    QuestionPlan(
      questionId = "probe",
      questionKind = AuthorQuestionKind.WhatMattersHere,
      priority = 1,
      claim = "c6 is the fixed target.",
      evidence = None,
      contrast = None,
      consequence = None,
      fallbackMode = QuestionPlanFallbackMode.PlannerOwned,
      strengthTier = QuestionPlanStrengthTier.Moderate,
      sourceKinds = List(PlanTaxonomy.PlanKind.BackwardPawnTargeting.id),
      admissibilityReasons = List("strategic_claim_supported_local"),
      plannerOwnerKind = PlannerOwnerKind.PositionProbe,
      plannerSource = PlanTaxonomy.PlanKind.BackwardPawnTargeting.id
    )

  test("supported local surface preserves piece-name capitalization") {
    val kingSurface = ClaimAuthorityResolver.supportedLocalSurface("King safety depends on the dark squares.")
    val queenSurface = ClaimAuthorityResolver.supportedLocalSurface("Queen activity is the main resource.")
    val rookSurface = ClaimAuthorityResolver.supportedLocalSurface("Rook pressure keeps the file tied down.")

    assertEquals(kingSurface, "A key idea is that King safety depends on the dark squares.")
    assertEquals(queenSurface, "A key idea is that Queen activity is the main resource.")
    assertEquals(rookSurface, "A key idea is that Rook pressure keeps the file tied down.")
  }

  test("supported local surface does not lowercase the first core character") {
    val surface = ClaimAuthorityResolver.supportedLocalSurface("This exchange moves into the queenless branch.")

    assertEquals(surface, "A key idea is that This exchange moves into the queenless branch.")
  }
