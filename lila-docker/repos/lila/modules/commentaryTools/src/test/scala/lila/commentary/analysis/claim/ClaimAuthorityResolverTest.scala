package lila.commentary.analysis.claim

import lila.commentary.analysis.*
import lila.commentary.analysis.semantic.StrategicObservationIds.{ ProofFamilyId, ProofSourceId }
import lila.commentary.model.*
import lila.commentary.model.authoring.AuthorQuestionKind
import munit.FunSuite

final class ClaimAuthorityResolverTest extends FunSuite:

  private val Source = PlayerFacingTruthModePolicy.DefenderTradeProofSource
  private val Family = ProofFamilyId.fromPlanKind(PlanTaxonomy.PlanKind.DefenderTrade).get.wireKey

  test("move-delta SupportedLocal admission does not depend on matching prose") {
    val decision =
      ClaimAuthorityResolver.planAuthorityDecision(
        ctx = Some(baseContext),
        inputs = inputs(sourceKind = Source, claimText = "This removes a defender on the local branch."),
        truthContract = Some(neutralTruthContract),
        plan = plan(plannerSource = Source, claim = "A different template says the trade changes the defender.")
      )

    assertEquals(decision.map(_.tier), Some(ClaimAuthorityTier.SupportedLocal), clue(decision))
  }

  test("move-delta SupportedLocal admission requires tactical-veto context") {
    val decision =
      ClaimAuthorityResolver.planAuthorityDecision(
        ctx = None,
        inputs = inputs(sourceKind = Source, claimText = "This removes a defender on the local branch."),
        truthContract = None,
        plan = plan(plannerSource = Source, claim = "This removes a defender on the local branch.")
      )

    assertEquals(decision.map(_.tier), Some(ClaimAuthorityTier.Suppressed), clue(decision))
    assert(decision.exists(_.vetoReasons.contains("tactical_context_missing")), clue(decision))
    assert(decision.exists(_.vetoReasons.contains("truth_contract_missing")), clue(decision))
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

    val genericExchange =
      ClaimAuthorityResolver.planAuthorityDecision(
        ctx = None,
        inputs =
          inputs(
            sourceKind = ProofSourceId.ExchangeForcingDelta.wireKey,
            claimText = "This removes a defender on the local branch."
          ),
        truthContract = None,
        plan =
          plan(
            plannerSource = ProofSourceId.ExchangeForcingDelta.wireKey,
            claim = "This removes a defender on the local branch."
          )
      )

    assertEquals(genericExchange, None)
  }

  private def inputs(sourceKind: String, claimText: String): QuestionPlannerInputs =
    QuestionPlannerInputs(
      mainBundle = Some(MainPathClaimBundle(Some(mainClaim(sourceKind, claimText)), None)),
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

  private def mainClaim(sourceKind: String, claimText: String): MainPathScopedClaim =
    MainPathScopedClaim(
      scope = PlayerFacingClaimScope.MoveLocal,
      mode = PlayerFacingTruthMode.Strategic,
      deltaClass = Some(PlayerFacingMoveDeltaClass.ExchangeForcing),
      claimText = claimText,
      anchorTerms = List("defender_trade"),
      evidenceLines = List("the local branch trades the defender"),
      sourceKind = sourceKind,
      tacticalOwnership = None,
      packet = Some(packet(sourceKind))
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
      sameBranchState = PlayerFacingSameBranchState.Proven,
      persistence = PlayerFacingClaimPersistence.Stable,
      proofPathWitness =
        PlayerFacingProofPathWitness(
          ownerSeedTerms =
            List("defender_trade_branch", "defender:c5", "exchange_square:d4", "defended_target:e5"),
          continuationTerms = List("trade_defender"),
          structureTransitionTerms =
            List("defender_trade_branch", "defender:c5", "exchange_square:d4", "defended_target:e5"),
          exactSliceProof = Some(PlayerFacingExactSliceProof.DefenderTrade("c5", "d4", "e5"))
        ),
      fallbackMode = PlayerFacingClaimFallbackMode.WeakMain
    )

  private def baseContext: NarrativeContext =
    NarrativeContext(
      fen = "4k3/p7/8/8/8/8/7P/2R1K3 w - - 0 1",
      header = ContextHeader("Middlegame", "Normal", "NarrowChoice", "Medium", "ExplainPlan"),
      ply = 24,
      playedMove = Some("c4d5"),
      playedSan = Some("Nxd5"),
      summary = NarrativeSummary("Exchange", None, "NarrowChoice", "Maintain", "0.00"),
      threats = ThreatTable(Nil, Nil),
      pawnPlay = PawnPlayTable(false, None, "Low", "Maintain", "Quiet", "Background", None, false, "quiet"),
      plans = PlanTable(Nil, Nil),
      delta = None,
      phase = PhaseContext("Middlegame", "Balanced middlegame"),
      candidates = Nil,
      renderMode = NarrativeRenderMode.MoveReview
    )

  private def neutralTruthContract: DecisiveTruthContract =
    DecisiveTruthContract(
      playedMove = Some("c4d5"),
      verifiedBestMove = Some("c4d5"),
      truthClass = DecisiveTruthClass.Acceptable,
      cpLoss = 0,
      swingSeverity = 0,
      reasonFamily = DecisiveReasonKind.QuietTechnicalMove,
      allowConcreteBenchmark = false,
      chosenMatchesBest = true,
      compensationAllowed = false,
      truthPhase = None,
      ownershipRole = TruthOwnershipRole.NoneRole,
      visibilityRole = TruthVisibilityRole.SupportingVisible,
      surfaceMode = TruthSurfaceMode.Neutral,
      exemplarRole = TruthExemplarRole.NonExemplar,
      surfacedMoveOwnsTruth = false,
      verifiedPayoffAnchor = None,
      compensationProseAllowed = true,
      benchmarkProseAllowed = false,
      investmentTruthChainKey = None,
      maintenanceExemplarCandidate = false,
      failureMode = FailureInterpretationMode.NoClearPlan,
      failureIntentConfidence = 0.0,
      failureIntentAnchor = None,
      failureInterpretationAllowed = false
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
