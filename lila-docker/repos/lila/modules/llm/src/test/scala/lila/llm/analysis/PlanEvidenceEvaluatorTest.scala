package lila.llm.analysis

import munit.FunSuite
import lila.llm.model.*
import lila.llm.model.authoring.*

class PlanEvidenceEvaluatorTest extends FunSuite:

  private def hypothesis(
      id: String,
      name: String,
      score: Double,
      sources: List[String]
  ): PlanHypothesis =
    PlanHypothesis(
      planId = id,
      planName = name,
      rank = 0,
      score = score,
      preconditions = Nil,
      executionSteps = Nil,
      failureModes = Nil,
      viability = PlanViability(score = score, label = "medium", risk = "test"),
      refutation = None,
      evidenceSources = sources
    )

  test("validateProbeResults drops contract-violating probe payloads") {
    val request = ProbeRequest(
      id = "probe_missing",
      fen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1",
      moves = List("h2h4"),
      depth = 18,
      purpose = Some("free_tempo_branches"),
      requiredSignals = List("replyPvs", "futureSnapshot")
    )
    val result = ProbeResult(
      id = "probe_missing",
      evalCp = 12,
      bestReplyPv = List("h7h5"),
      deltaVsBaseline = -5,
      keyMotifs = List("free_tempo_trajectory"),
      purpose = Some("free_tempo_branches")
    )

    val validation =
      PlanEvidenceEvaluator.validateProbeResults(
        rawResults = List(result),
        probeRequests = List(request)
      )

    assertEquals(validation.validResults, Nil)
    assertEquals(validation.droppedCount, 1)
    assert(validation.invalidByRequestId.getOrElse("probe_missing", Nil).contains("futureSnapshot"))
  }

  test("partition keeps pv-coupled plans out of user-facing main plans without supportive probe evidence") {
    val enginePlan =
      hypothesis(
        id = "CentralControl",
        name = "Central control with flexible break",
        score = 0.74,
        sources = List("support:engine_hypothesis")
      )
    val latentPlan =
      hypothesis(
        id = "PawnStorm",
        name = "Rook-pawn march to gain flank space",
        score = 0.68,
        sources = List("latent_seed:kingside_rook_pawn_march")
      )

    val partition =
      PlanEvidenceEvaluator.partition(
        hypotheses = List(enginePlan, latentPlan),
        probeRequests = Nil,
        validatedProbeResults = Nil,
        rulePlanIds = Set("centralcontrol"),
        isWhiteToMove = true,
        droppedProbeCount = 0
      )

    assertEquals(partition.mainPlans, Nil)
    assert(
      partition.diagnosticSidecar.entries.exists(entry =>
        entry.planId == "CentralControl" &&
          entry.userFacingEligibility == "pv_coupled_only"
      ),
      clue(partition.diagnosticSidecar)
    )
  }

  test("partition marks plan as refuted when validated refutation probe exceeds cp-loss bound") {
    val plan =
      hypothesis(
        id = "PawnStorm",
        name = "Rook-pawn march to gain flank space",
        score = 0.73,
        sources = List("latent_seed:kingside_rook_pawn_march")
      )
    val request = ProbeRequest(
      id = "probe_refute",
      fen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1",
      moves = List("h2h4"),
      depth = 20,
      purpose = Some("latent_plan_refutation"),
      planId = Some("PawnStorm"),
      seedId = Some("kingside_rook_pawn_march"),
      requiredSignals = List("replyPvs", "keyMotifs", "l1Delta", "futureSnapshot"),
      maxCpLoss = Some(120)
    )
    val result = ProbeResult(
      id = "probe_refute",
      evalCp = -180,
      bestReplyPv = List("h7h5", "g2g4"),
      replyPvs = Some(List(List("h7h5", "g2g4"))),
      deltaVsBaseline = -220,
      keyMotifs = List("latent_plan_refutation"),
      purpose = Some("latent_plan_refutation"),
      l1Delta = Some(
        L1DeltaSnapshot(
          materialDelta = 0,
          kingSafetyDelta = -2,
          centerControlDelta = -1,
          openFilesDelta = 0,
          mobilityDelta = -2,
          collapseReason = Some("king safety collapses")
        )
      ),
      futureSnapshot = Some(
        FutureSnapshot(
          resolvedThreatKinds = Nil,
          newThreatKinds = List("KingSafety"),
          targetsDelta = TargetsDelta(Nil, Nil, Nil, Nil),
          planBlockersRemoved = Nil,
          planPrereqsMet = Nil
        )
      )
    )

    val partition =
      PlanEvidenceEvaluator.partition(
        hypotheses = List(plan),
        probeRequests = List(request),
        validatedProbeResults = List(result),
        rulePlanIds = Set.empty,
        isWhiteToMove = true,
        droppedProbeCount = 0
    )

    assertEquals(partition.mainPlans, Nil)
    assert(
      partition.diagnosticSidecar.entries.exists(entry =>
        entry.planId == "PawnStorm" &&
          entry.userFacingEligibility == "refuted" &&
          entry.refuteProbeIds.contains("probe_refute")
      ),
      clue(partition.diagnosticSidecar)
    )
  }

  test("partition defers plan when matched probes failed validation contract") {
    val plan =
      hypothesis(
        id = "PawnStorm",
        name = "Rook-pawn march to gain flank space",
        score = 0.66,
        sources = List("latent_seed:kingside_rook_pawn_march")
      )
    val request = ProbeRequest(
      id = "probe_deferred",
      fen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1",
      moves = List("h2h4"),
      depth = 18,
      purpose = Some("free_tempo_branches"),
      planId = Some("PawnStorm"),
      seedId = Some("kingside_rook_pawn_march"),
      requiredSignals = List("replyPvs", "futureSnapshot")
    )

    val partition =
      PlanEvidenceEvaluator.partition(
        hypotheses = List(plan),
        probeRequests = List(request),
        validatedProbeResults = Nil,
        rulePlanIds = Set.empty,
        isWhiteToMove = true,
        droppedProbeCount = 1,
        invalidByRequestId = Map("probe_deferred" -> List("futureSnapshot"))
      )

    assertEquals(partition.mainPlans, Nil)
    assertEquals(partition.diagnosticSidecar.droppedProbeCount, 1)
    assert(
      partition.diagnosticSidecar.entries.exists(entry =>
        entry.planId == "PawnStorm" &&
          entry.userFacingEligibility == "deferred" &&
          entry.missingSignals.contains("futureSnapshot")
      ),
      clue(partition.diagnosticSidecar)
    )
  }

  test("partition exposes only validated supportive plans to user-facing main plans") {
    val backedPlan =
      hypothesis(
        id = "PieceActivation",
        name = "Piece activation",
        score = 0.74,
        sources = List("support:engine_hypothesis")
      )
    val request = ProbeRequest(
      id = "probe_support",
      fen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1",
      moves = List("e2e4"),
      depth = 18,
      purpose = Some("theme_plan_validation"),
      planId = Some("PieceActivation"),
      requiredSignals = List("replyPvs", "futureSnapshot")
    )
    val result = ProbeResult(
      id = "probe_support",
      evalCp = 24,
      bestReplyPv = List("e7e5", "g1f3"),
      replyPvs = Some(List(List("e7e5", "g1f3"))),
      deltaVsBaseline = 8,
      keyMotifs = List("piece_activation"),
      purpose = Some("theme_plan_validation"),
      futureSnapshot = Some(
        FutureSnapshot(
          resolvedThreatKinds = List("Development"),
          newThreatKinds = Nil,
          targetsDelta = TargetsDelta(Nil, Nil, Nil, Nil),
          planBlockersRemoved = List("development_complete"),
          planPrereqsMet = List("piece_activation_ready")
        )
      )
    )
    val deferredPlan =
      hypothesis(
        id = "PawnStorm2",
        name = "Kingside pawn storm",
        score = 0.66,
        sources = List("latent_seed:kingside_rook_pawn_march")
      )

    val partition =
      PlanEvidenceEvaluator.partition(
        hypotheses = List(backedPlan, deferredPlan),
        probeRequests = List(request),
        validatedProbeResults = List(result),
        rulePlanIds = Set("pieceactivation"),
        isWhiteToMove = true,
        droppedProbeCount = 0
      )

    assertEquals(partition.mainPlans.map(_.planId), List("PieceActivation"))
    assert(
      partition.mainPlans.headOption.exists(_.evidenceSources.contains("probe_backed:validated_support")),
      clue(partition.mainPlans)
    )
    assert(
      partition.diagnosticSidecar.entries.exists(entry =>
        entry.planId == "PawnStorm2" &&
          entry.userFacingEligibility == "deferred"
      ),
      clue(partition.diagnosticSidecar)
    )
  }

  test("partition records certification metadata for probe-backed plans in diagnostic sidecar") {
    val backedPlan =
      hypothesis(
        id = "PieceActivation",
        name = "Piece activation",
        score = 0.78,
        sources = List("support:engine_hypothesis", "theme:piece_redeployment")
      )
    val request = ProbeRequest(
      id = "probe_support_cert",
      fen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1",
      moves = List("g1f3"),
      depth = 20,
      purpose = Some("theme_plan_validation"),
      objective = Some("validate_theme_plan"),
      planId = Some("PieceActivation"),
      requiredSignals = List("replyPvs", "futureSnapshot"),
      candidateMove = Some("g1f3"),
      depthFloor = Some(20),
      variationHash = Some("piece-activation-hash"),
      engineConfigFingerprint = Some("wasm_stockfish:depth=20:multipv=2")
    )
    val result = ProbeResult(
      id = "probe_support_cert",
      fen = Some("4k3/8/8/8/8/8/8/4K3 w - - 0 1"),
      evalCp = 26,
      bestReplyPv = List("g8f6", "e2e4"),
      replyPvs = Some(List(List("g8f6", "e2e4"), List("e7e5", "g1f3"))),
      deltaVsBaseline = 12,
      keyMotifs = List("piece_activation"),
      purpose = Some("theme_plan_validation"),
      objective = Some("validate_theme_plan"),
      probedMove = Some("g1f3"),
      depth = Some(20),
      futureSnapshot = Some(
        FutureSnapshot(
          resolvedThreatKinds = List("Development"),
          newThreatKinds = Nil,
          targetsDelta = TargetsDelta(Nil, Nil, List("e4"), Nil),
          planBlockersRemoved = List("development_complete"),
          planPrereqsMet = List("piece_activation_ready")
        )
      ),
      variationHash = Some("piece-activation-hash"),
      engineConfigFingerprint = Some("wasm_stockfish:depth=20:multipv=2")
    )

    val partition =
      PlanEvidenceEvaluator.partition(
        hypotheses = List(backedPlan),
        probeRequests = List(request),
        validatedProbeResults = List(result),
        rulePlanIds = Set("pieceactivation"),
        isWhiteToMove = true,
        droppedProbeCount = 0
      )

    val entry =
      partition.diagnosticSidecar.entries.find(_.planId == "PieceActivation").getOrElse(fail("missing diagnostic entry"))
    assertEquals(entry.certificateStatus, "valid")
    assertEquals(entry.quantifier, "universal")
    assertEquals(entry.modalityTier, "advances")
    assertEquals(entry.provenanceClass, "probe_backed")
    assertEquals(entry.ontologyFamily, "plan_advance")
  }

  test("partition summarizes blocked and downgraded claim-certification outcomes in diagnostic sidecar") {
    val downgradedPlan =
      hypothesis(
        id = "ExchangeWindow",
        name = "Favorable exchange window",
        score = 0.76,
        sources = List("theme:favorable_exchange")
      )
    val siblingExchangePlan =
      hypothesis(
        id = "ExchangeShield",
        name = "Queen trade shield",
        score = 0.74,
        sources = List("theme:favorable_exchange")
      )
    val blockedPlan =
      hypothesis(
        id = "PieceCoordination",
        name = "Piece coordination",
        score = 0.72,
        sources = List("theme:piece_redeployment")
      )

    val downgradedRequest = ProbeRequest(
      id = "probe_downgraded",
      fen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1",
      moves = List("g1f3"),
      depth = 20,
      purpose = Some("theme_plan_validation"),
      objective = Some("validate_plan_presence"),
      planId = Some("ExchangeWindow"),
      requiredSignals = List("replyPvs")
    )
    val blockedRequest = ProbeRequest(
      id = "probe_blocked",
      fen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1",
      moves = List("b1c3"),
      depth = 20,
      purpose = Some("theme_plan_validation"),
      objective = Some("validate_plan_presence"),
      planId = Some("PieceCoordination"),
      requiredSignals = List("keyMotifs")
    )

    val downgradedResult = ProbeResult(
      id = "probe_downgraded",
      fen = Some("4k3/8/8/8/8/8/8/4K3 w - - 0 1"),
      evalCp = 22,
      bestReplyPv = List("g8f6", "f3e5"),
      replyPvs = Some(List(List("g8f6", "f3e5"))),
      deltaVsBaseline = 8,
      keyMotifs = List("favorable_exchange"),
      purpose = Some("theme_plan_validation"),
      objective = Some("validate_plan_presence"),
      probedMove = Some("g1f3"),
      depth = Some(20)
    )
    val blockedResult = ProbeResult(
      id = "probe_blocked",
      fen = Some("4k3/8/8/8/8/8/8/4K3 w - - 0 1"),
      evalCp = 18,
      bestReplyPv = Nil,
      deltaVsBaseline = 4,
      keyMotifs = List("piece_redeployment"),
      purpose = Some("theme_plan_validation"),
      objective = Some("validate_plan_presence"),
      probedMove = Some("b1c3"),
      depth = Some(20)
    )

    val partition =
      PlanEvidenceEvaluator.partition(
        hypotheses = List(downgradedPlan, siblingExchangePlan, blockedPlan),
        probeRequests = List(downgradedRequest, blockedRequest),
        validatedProbeResults = List(downgradedResult, blockedResult),
        rulePlanIds = Set.empty,
        isWhiteToMove = true,
        droppedProbeCount = 0
      )

    assert(partition.diagnosticSidecar.blockedStrongClaims >= 1)
    assert(partition.diagnosticSidecar.downgradedWeakClaims >= 1)
    assert(partition.diagnosticSidecar.quantifierFailures >= 1)
    assert(partition.diagnosticSidecar.stabilityFailures >= 1)
  }
