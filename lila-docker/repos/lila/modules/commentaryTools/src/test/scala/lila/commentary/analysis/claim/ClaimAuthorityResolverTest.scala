package lila.commentary.analysis.claim

import lila.commentary.analysis.*
import lila.commentary.analysis.semantic.StrategicObservationIds.ProofFamilyId
import lila.commentary.model.authoring.AuthorQuestionKind
import munit.FunSuite

final class ClaimAuthorityResolverTest extends FunSuite:

  private val Source = PlayerFacingTruthModePolicy.DefenderTradeProofSource
  private val Family = ProofFamilyId.fromPlanKind(PlanTaxonomy.PlanKind.DefenderTrade).get.wireKey

  test("move-delta SupportedLocal admission does not depend on matching prose") {
    val decision =
      ClaimAuthorityResolver.planAuthorityDecision(
        ctx = None,
        inputs = inputs(sourceKind = Source, claimText = "This removes a defender on the local branch."),
        truthContract = None,
        plan = plan(plannerSource = Source, claim = "A different template says the trade changes the defender.")
      )

    assertEquals(decision.map(_.tier), Some(ClaimAuthorityTier.SupportedLocal), clue(decision))
  }

  test("relation transformation SupportedLocal admission requires a proven stable branch") {
    val unstablePacket =
      packet(Source).copy(sameBranchState = PlayerFacingSameBranchState.Missing)
    val decision =
      ClaimAuthorityResolver.planAuthorityDecision(
        ctx = None,
        inputs =
          inputs(
            sourceKind = Source,
            claimText = "This removes a defender on the local branch.",
            packetOverride = Some(unstablePacket)
          ),
        truthContract = None,
        plan = plan(plannerSource = Source, claim = "This removes a defender on the local branch.")
      )

    assertEquals(decision, None)
  }

  test("move-delta SupportedLocal admission still requires the structured source to match") {
    val decision =
      ClaimAuthorityResolver.planAuthorityDecision(
        ctx = None,
        inputs = inputs(sourceKind = Source, claimText = "This removes a defender on the local branch."),
        truthContract = None,
        plan = plan(plannerSource = "other_source", claim = "This removes a defender on the local branch.")
      )

    assertEquals(decision, None)
  }

  private def inputs(
      sourceKind: String,
      claimText: String,
      packetOverride: Option[PlayerFacingClaimPacket] = None
  ): QuestionPlannerInputs =
    QuestionPlannerInputs(
      mainBundle = Some(MainPathClaimBundle(Some(mainClaim(sourceKind, claimText, packetOverride)), None)),
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

  private def mainClaim(
      sourceKind: String,
      claimText: String,
      packetOverride: Option[PlayerFacingClaimPacket]
  ): MainPathScopedClaim =
    MainPathScopedClaim(
      scope = PlayerFacingClaimScope.MoveLocal,
      mode = PlayerFacingTruthMode.Strategic,
      deltaClass = Some(PlayerFacingMoveDeltaClass.ExchangeForcing),
      claimText = claimText,
      anchorTerms = List("defender_trade"),
      evidenceLines = List("the local branch trades the defender"),
      sourceKind = sourceKind,
      tacticalOwnership = None,
      packet = Some(packetOverride.getOrElse(packet(sourceKind)))
    )

  private def packet(proofSource: String): PlayerFacingClaimPacket =
    PlayerFacingClaimPacket(
      claimGate =
        PlanEvidenceEvaluator.ClaimCertification(
          certificateStatus = PlayerFacingCertificateStatus.Valid,
          quantifier = PlayerFacingClaimQuantifier.BestResponse,
          attributionGrade = PlayerFacingClaimAttributionGrade.AnchoredButShared,
          stabilityGrade = PlayerFacingClaimStabilityGrade.Stable,
          provenanceClass = PlayerFacingClaimProvenanceClass.ProbeBacked
        ),
      proofSource = proofSource,
      proofFamily = Family,
      scope = PlayerFacingPacketScope.MoveLocal,
      triggerKind = PlanTaxonomy.PlanKind.DefenderTrade.id,
      anchorTerms = List("defender_trade"),
      bestDefenseMove = Some("c4d5"),
      bestDefenseBranchKey = Some("c4d5|e6d5"),
      sameBranchState = PlayerFacingSameBranchState.Proven,
      persistence = PlayerFacingClaimPersistence.Stable,
      proofPathWitness =
        PlayerFacingProofPathWitness(
          ownerSeedTerms = List("defender_trade"),
          continuationTerms = List("trade_defender"),
          structureTransitionTerms = List("defender_removed"),
          exactSliceProof = Some(PlayerFacingExactSliceProof.DefenderTrade("e6", "d5", "c4"))
        ),
      fallbackMode = PlayerFacingClaimFallbackMode.WeakMain
    )

  private def plan(plannerSource: String, claim: String): QuestionPlan =
    QuestionPlan(
      questionId = "q_move_delta",
      questionKind = AuthorQuestionKind.WhatChanged,
      priority = 100,
      claim = claim,
      evidence = None,
      contrast = None,
      consequence = None,
      fallbackMode = QuestionPlanFallbackMode.PlannerOwned,
      strengthTier = QuestionPlanStrengthTier.Moderate,
      sourceKinds = List(plannerSource),
      admissibilityReasons = Nil,
      plannerOwnerKind = PlannerOwnerKind.MoveDelta,
      plannerSource = plannerSource
    )
