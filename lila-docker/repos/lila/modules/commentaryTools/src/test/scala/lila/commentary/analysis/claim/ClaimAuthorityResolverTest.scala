package lila.commentary.analysis.claim

import lila.commentary.analysis.*
import lila.commentary.analysis.semantic.StrategicObservationIds.{ ProofFamilyId, ProofSourceId }
import lila.commentary.model.*
import lila.commentary.model.authoring.AuthorQuestionKind
import lila.commentary.model.strategic.{ EngineEvidence, VariationLine }
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

  test("move-delta SupportedLocal admission requires proof-contract witnesses") {
    val decision =
      ClaimAuthorityResolver.supportedLocalMoveDeltaPacketDecision(
        ctx = Some(baseContext),
        inputs = inputs(sourceKind = Source, claimText = "This removes a defender on the local branch."),
        truthContract = Some(neutralTruthContract),
        packet = packet(Source, exactProof = false)
    )

    assertEquals(decision.tier, ClaimAuthorityTier.Suppressed, clue(decision))
    assert(decision.failureCodes.contains("witness:exact_slice_missing"), clue(decision))
  }

  test("promoted local facts prefer exact proof family over coarse move-delta class") {
    val source = ProofSourceId.LocalFileEntryBind.wireKey
    val claim =
      MainPathScopedClaim(
        scope = PlayerFacingClaimScope.MoveLocal,
        mode = PlayerFacingTruthMode.Strategic,
        deltaClass = Some(PlayerFacingMoveDeltaClass.NewAccess),
        claimText = "The file-entry bind keeps c6 under pressure.",
        anchorTerms = List("c-file", "c6"),
        evidenceLines = List("the local file-entry branch holds c6"),
        sourceKind = source,
        tacticalOwnership = None,
        packet = Some(localFilePacket)
      )
    val admissions =
      ClaimAuthorityResolver.promotedLocalFactAdmissions(
        ctx = Some(baseContext),
        inputs = inputs(claim),
        truthContract = Some(neutralTruthContract),
        plan = plan(plannerSource = source, claim = claim.claimText)
      )

    assertEquals(admissions.map(_.family), List(MoveReviewLocalFact.Family.Pressure), clue(admissions))
    assertEquals(admissions.map(_.source), List(MoveReviewLocalFact.Source.PvCoupledLine), clue(admissions))
    assertEquals(
      admissions.map(_.localFactCandidate.producer),
      List(MoveReviewLocalFact.Producer.CertifiedStrategyDelta),
      clue(admissions)
    )
  }

  test("promoted local facts carry central-break timing packets through existing admission") {
    val ctx = centralBreakContext
    val witness =
      CentralBreakTimingWitness
        .exact(ctx)
        .getOrElse(fail(s"missing central-break witness: ${CentralBreakTimingWitness.diagnose(ctx)}"))
    val source = CentralBreakTimingWitness.ProofSource
    val packet = centralBreakPacket(witness)
    val claim =
      MainPathScopedClaim(
        scope = PlayerFacingClaimScope.MoveLocal,
        mode = PlayerFacingTruthMode.Strategic,
        deltaClass = Some(PlayerFacingMoveDeltaClass.PlanAdvance),
        claimText = s"${witness.breakToken} is the checked central break timing point.",
        anchorTerms = witness.ownerSeedTerms,
        evidenceLines = witness.structureTransitionTerms,
        sourceKind = source,
        tacticalOwnership = None,
        packet = Some(packet)
      )
    val timingPlan =
      plan(plannerSource = source, claim = claim.claimText).copy(
        plannerOwnerKind = PlannerOwnerKind.DecisionTiming,
        sourceKinds = List(source)
      )
    val admissions =
      ClaimAuthorityResolver.promotedLocalFactAdmissions(
        ctx = Some(ctx),
        inputs = inputs(claim),
        truthContract = Some(neutralTruthContract.copy(playedMove = Some(witness.breakMove), verifiedBestMove = Some(witness.breakMove))),
        plan = timingPlan
      )

    assertEquals(admissions.map(_.family), List(MoveReviewLocalFact.Family.Timing), clue(admissions))
    assertEquals(admissions.map(_.source), List(MoveReviewLocalFact.Source.PvCoupledLine), clue(admissions))
    assertEquals(
      admissions.map(_.localFactCandidate.producer),
      List(MoveReviewLocalFact.Producer.CertifiedStrategyDelta),
      clue(admissions)
    )
    assert(admissions.exists(_.evidenceRefs.contains(s"proof_source:${CentralBreakTimingWitness.ProofSource}")), clue(admissions))
    assert(admissions.exists(_.guardrails.contains("promoted_family:timing")), clue(admissions))
  }

  private def inputs(sourceKind: String, claimText: String): QuestionPlannerInputs =
    inputs(mainClaim(sourceKind, claimText))

  private def inputs(claim: MainPathScopedClaim): QuestionPlannerInputs =
    QuestionPlannerInputs(
      mainBundle = Some(MainPathClaimBundle(Some(claim), None)),
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

  private def packet(proofSource: String, exactProof: Boolean = true): PlayerFacingClaimPacket =
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
          exactSliceProof = Option.when(exactProof)(PlayerFacingExactSliceProof.DefenderTrade("c5", "d4", "e5"))
        ),
      fallbackMode = PlayerFacingClaimFallbackMode.WeakMain
    )

  private def localFilePacket: PlayerFacingClaimPacket =
    PlayerFacingClaimPacket(
      claimGate =
        PlanEvidenceEvaluator.ClaimCertification(
          certificateStatus = PlayerFacingCertificateStatus.Valid,
          quantifier = PlayerFacingClaimQuantifier.BestResponse,
          attributionGrade = PlayerFacingClaimAttributionGrade.AnchoredButShared,
          stabilityGrade = PlayerFacingClaimStabilityGrade.Stable,
          provenanceClass = PlayerFacingClaimProvenanceClass.ProbeBacked
        ),
      proofSource = ProofSourceId.LocalFileEntryBind.wireKey,
      proofFamily = ProofFamilyId.HalfOpenFilePressure.wireKey,
      scope = PlayerFacingPacketScope.MoveLocal,
      triggerKind = "file_entry_denial",
      anchorTerms = List("c-file", "c6"),
      bestDefenseMove = Some("c7c6"),
      bestDefenseBranchKey = Some("d4c6|c7c6"),
      sameBranchState = PlayerFacingSameBranchState.Proven,
      persistence = PlayerFacingClaimPersistence.Stable,
      proofPathWitness =
        PlayerFacingProofPathWitness(
          ownerSeedTerms = List("c-file", "c6"),
          continuationTerms = List("local_file_entry_bind", "c-file", "c6", "d4c6", "c7c6"),
          structureTransitionTerms = List("file-entry:c-file:c6"),
          exactSliceProof = Some(PlayerFacingExactSliceProof.LocalFileEntryBind("c-file", "c6"))
        ),
      fallbackMode = PlayerFacingClaimFallbackMode.WeakMain
    )

  private def centralBreakContext: NarrativeContext =
    baseContext.copy(
      fen = "rnbqk2r/pp2bppp/4pn2/2p5/2BP4/4PN2/PP3PPP/RNBQ1RK1 w kq - 0 8",
      ply = 15,
      playedMove = Some("d4d5"),
      playedSan = Some("d5"),
      engineEvidence =
        Some(
          EngineEvidence(
            depth = 18,
            variations =
              List(
                VariationLine(List("d4d5", "e6d5", "c4d5"), scoreCp = 72, depth = 18),
                VariationLine(List("b1c3", "e8g8", "d4d5"), scoreCp = 28, depth = 18)
              )
          )
        )
    )

  private def centralBreakPacket(witness: CentralBreakTimingWitness.Witness): PlayerFacingClaimPacket =
    PlayerFacingClaimPacket(
      claimGate =
        PlanEvidenceEvaluator.ClaimCertification(
          certificateStatus = PlayerFacingCertificateStatus.Valid,
          quantifier = PlayerFacingClaimQuantifier.BestResponse,
          attributionGrade = PlayerFacingClaimAttributionGrade.AnchoredButShared,
          stabilityGrade = PlayerFacingClaimStabilityGrade.Stable,
          provenanceClass = PlayerFacingClaimProvenanceClass.ProbeBacked
        ),
      proofSource = CentralBreakTimingWitness.ProofSource,
      proofFamily = CentralBreakTimingWitness.ProofFamily,
      scope = PlayerFacingPacketScope.MoveLocal,
      triggerKind = PlanTaxonomy.PlanKind.CentralBreakTiming.id,
      anchorTerms = witness.ownerSeedTerms,
      bestDefenseMove = Some(witness.breakMove),
      sameBranchState = PlayerFacingSameBranchState.Proven,
      persistence = PlayerFacingClaimPersistence.Stable,
      proofPathWitness =
        PlayerFacingProofPathWitness(
          ownerSeedTerms = witness.ownerSeedTerms,
          continuationTerms = witness.structureTransitionTerms,
          structureTransitionTerms = witness.structureTransitionTerms,
          exactSliceProof =
            Some(
              PlayerFacingExactSliceProof.CentralBreakTiming(
                breakMove = witness.breakMove,
                breakSquare = witness.breakSquare,
                breakToken = witness.breakToken
              )
            )
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
