package lila.commentary.analysis

import munit.FunSuite
import lila.commentary.model.*
import lila.commentary.model.authoring.*

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

  private def certifiedResult(request: ProbeRequest, result: ProbeResult): ProbeResult =
    result.copy(
      fen = result.fen.orElse(Some(request.fen)),
      purpose = result.purpose.orElse(request.purpose),
      probedMove = result.probedMove.orElse(request.candidateMove).orElse(request.moves.headOption),
      depth = result.depth.orElse(Option.when(request.depth > 0)(request.depth))
    )

  private def validatedResults(request: ProbeRequest, result: ProbeResult): List[ProbeResult] =
    PlanEvidenceEvaluator
      .validateProbeResults(
        rawResults = List(certifiedResult(request, result)),
        probeRequests = List(request)
      )
      .validResults

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

  test("partition keeps pv-coupled plans out of selected main plans without supportive probe evidence") {
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
        validatedProbeResults = validatedResults(request, result),
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
        validatedProbeResults = validatedResults(request, result),
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

  test("partition does not attach a probe to another plan by partial name match") {
    val exactPlan =
      hypothesis(
        id = "RookLift",
        name = "Rook lift",
        score = 0.74,
        sources = List("theme:piece_redeployment")
      )
    val substringPlan =
      hypothesis(
        id = "Rook",
        name = "Rook",
        score = 0.72,
        sources = List("theme:piece_redeployment")
      )
    val request = ProbeRequest(
      id = "probe_rook_lift",
      fen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1",
      moves = List("h1h3"),
      depth = 18,
      purpose = Some("theme_plan_validation"),
      planName = Some("Rook lift"),
      requiredSignals = List("replyPvs", "futureSnapshot")
    )
    val result = ProbeResult(
      id = "probe_rook_lift",
      evalCp = 20,
      bestReplyPv = List("h8h6", "h3g3"),
      replyPvs = Some(List(List("h8h6", "h3g3"))),
      deltaVsBaseline = 5,
      keyMotifs = List("rook_lift"),
      purpose = Some("theme_plan_validation"),
      futureSnapshot = Some(
        FutureSnapshot(
          resolvedThreatKinds = Nil,
          newThreatKinds = Nil,
          targetsDelta = TargetsDelta(Nil, Nil, List("g3"), Nil),
          planBlockersRemoved = List("rook_path_clear"),
          planPrereqsMet = List("rook_lift_ready")
        )
      )
    )

    val partition =
      PlanEvidenceEvaluator.partition(
        hypotheses = List(exactPlan, substringPlan),
        probeRequests = List(request),
        validatedProbeResults = validatedResults(request, result),
        rulePlanIds = Set.empty,
        isWhiteToMove = true,
        droppedProbeCount = 0
      )

    val exactEntry = partition.diagnosticSidecar.entries.find(_.planId == "RookLift").getOrElse(fail("missing exact"))
    val substringEntry = partition.diagnosticSidecar.entries.find(_.planId == "Rook").getOrElse(fail("missing substring"))
    assertEquals(exactEntry.userFacingEligibility, "probe_backed")
    assertEquals(substringEntry.userFacingEligibility, "deferred")
    assertEquals(substringEntry.supportProbeIds, Nil)
  }

  test("partition rejects probes whose explicit plan and seed bindings conflict") {
    val plan =
      hypothesis(
        id = "PawnStorm",
        name = "Rook-pawn march to gain flank space",
        score = 0.73,
        sources = List("latent_seed:kingside_rook_pawn_march")
      )
    val request = ProbeRequest(
      id = "probe_conflicting_binding",
      fen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1",
      moves = List("h2h4"),
      depth = 18,
      purpose = Some("free_tempo_branches"),
      planId = Some("PawnStorm"),
      seedId = Some("queenside_minority_attack"),
      requiredSignals = List("replyPvs", "futureSnapshot")
    )
    val result = ProbeResult(
      id = "probe_conflicting_binding",
      evalCp = 20,
      bestReplyPv = List("h7h6", "h4h5"),
      replyPvs = Some(List(List("h7h6", "h4h5"))),
      deltaVsBaseline = 5,
      keyMotifs = List("free_tempo_trajectory"),
      purpose = Some("free_tempo_branches"),
      futureSnapshot = Some(
        FutureSnapshot(
          resolvedThreatKinds = Nil,
          newThreatKinds = Nil,
          targetsDelta = TargetsDelta(Nil, Nil, List("h5"), Nil),
          planBlockersRemoved = Nil,
          planPrereqsMet = List("rook_pawn_march_ready")
        )
      )
    )

    val partition =
      PlanEvidenceEvaluator.partition(
        hypotheses = List(plan),
        probeRequests = List(request),
        validatedProbeResults = validatedResults(request, result),
        rulePlanIds = Set.empty,
        isWhiteToMove = true,
        droppedProbeCount = 0
      )

    val entry = partition.diagnosticSidecar.entries.find(_.planId == "PawnStorm").getOrElse(fail("missing entry"))
    assertEquals(entry.userFacingEligibility, "deferred")
    assertEquals(entry.supportProbeIds, Nil)
  }

  test("partition treats non-refuted refutation probes as clearance, not support") {
    val plan =
      hypothesis(
        id = "PawnStorm",
        name = "Rook-pawn march to gain flank space",
        score = 0.73,
        sources = List("latent_seed:kingside_rook_pawn_march")
      )
    val request = ProbeRequest(
      id = "probe_clearance",
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
      id = "probe_clearance",
      evalCp = 20,
      bestReplyPv = List("h7h6", "h4h5"),
      replyPvs = Some(List(List("h7h6", "h4h5"))),
      deltaVsBaseline = 5,
      keyMotifs = List("latent_plan_refutation"),
      purpose = Some("latent_plan_refutation"),
      l1Delta = Some(L1DeltaSnapshot(0, 0, 0, 0, 1, None)),
      futureSnapshot = Some(
        FutureSnapshot(
          resolvedThreatKinds = Nil,
          newThreatKinds = Nil,
          targetsDelta = TargetsDelta(Nil, Nil, List("h5"), Nil),
          planBlockersRemoved = Nil,
          planPrereqsMet = Nil
        )
      )
    )

    val partition =
      PlanEvidenceEvaluator.partition(
        hypotheses = List(plan),
        probeRequests = List(request),
        validatedProbeResults = validatedResults(request, result),
        rulePlanIds = Set.empty,
        isWhiteToMove = true,
        droppedProbeCount = 0
      )

    assertEquals(partition.mainPlans, Nil)
    val entry = partition.diagnosticSidecar.entries.find(_.planId == "PawnStorm").getOrElse(fail("missing entry"))
    assertEquals(entry.userFacingEligibility, "deferred")
    assertEquals(entry.supportProbeIds, Nil)
    assertEquals(entry.refuteProbeIds, Nil)
  }

  test("partition does not refute from an unknown-purpose probe even if a result is supplied") {
    val plan =
      hypothesis(
        id = "PawnStorm",
        name = "Rook-pawn march to gain flank space",
        score = 0.73,
        sources = List("latent_seed:kingside_rook_pawn_march")
      )
    val request = ProbeRequest(
      id = "probe_unknown_refute",
      fen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1",
      moves = List("h2h4"),
      depth = 20,
      purpose = Some("unknown_probe"),
      planId = Some("PawnStorm"),
      seedId = Some("kingside_rook_pawn_march"),
      requiredSignals = List("replyPvs"),
      maxCpLoss = Some(120)
    )
    val result = ProbeResult(
      id = "probe_unknown_refute",
      evalCp = -240,
      bestReplyPv = List("h7h5", "g2g4"),
      replyPvs = Some(List(List("h7h5", "g2g4"))),
      deltaVsBaseline = -260,
      keyMotifs = Nil,
      purpose = Some("unknown_probe")
    )

    val partition =
      PlanEvidenceEvaluator.partition(
        hypotheses = List(plan),
        probeRequests = List(request),
        validatedProbeResults = List(certifiedResult(request, result)),
        rulePlanIds = Set.empty,
        isWhiteToMove = true,
        droppedProbeCount = 0
      )

    assertEquals(partition.mainPlans, Nil)
    val entry = partition.diagnosticSidecar.entries.find(_.planId == "PawnStorm").getOrElse(fail("missing entry"))
    assertEquals(entry.userFacingEligibility, "deferred")
    assertEquals(entry.refuteProbeIds, Nil)
  }

  test("partition promotes board-valid probes even when bookkeeping metadata drifts") {
    val backedPlan =
      hypothesis(
        id = "PieceActivation",
        name = "Piece activation",
        score = 0.74,
        sources = List("support:engine_hypothesis", "theme:piece_redeployment")
      )
    val request = ProbeRequest(
      id = "probe_soft_support",
      fen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1",
      moves = List("g1f3"),
      depth = 18,
      purpose = Some("theme_plan_validation"),
      objective = Some("validate_plan_presence"),
      planId = Some("PieceActivation"),
      requiredSignals = List("replyPvs", "futureSnapshot"),
      candidateMove = Some("g1f3"),
      depthFloor = Some(18),
      variationHash = Some("expected-hash"),
      engineConfigFingerprint = Some("wasm_stockfish:depth=18:multipv=1")
    )
    val result = ProbeResult(
      id = "probe_soft_support",
      fen = Some("4k3/8/8/8/8/8/8/4K3 w - - 0 1"),
      evalCp = 24,
      bestReplyPv = List("g8f6", "e2e4"),
      replyPvs = Some(List(List("g8f6", "e2e4"))),
      deltaVsBaseline = 8,
      keyMotifs = List("piece_activation"),
      purpose = Some("free_tempo_branches"),
      objective = Some("validate_latent_plan"),
      probedMove = Some("g1f3"),
      depth = Some(18),
      futureSnapshot = Some(
        FutureSnapshot(
          resolvedThreatKinds = List("Development"),
          newThreatKinds = Nil,
          targetsDelta = TargetsDelta(Nil, Nil, List("e4"), Nil),
          planBlockersRemoved = List("development_complete"),
          planPrereqsMet = List("piece_activation_ready")
        )
      ),
      variationHash = Some("other-hash"),
      engineConfigFingerprint = Some("wasm_stockfish:depth=18:multipv=2")
    )
    val validation =
      PlanEvidenceEvaluator.validateProbeResults(
        rawResults = List(result),
        probeRequests = List(request)
      )

    val partition =
      PlanEvidenceEvaluator.partition(
        hypotheses = List(backedPlan),
        probeRequests = List(request),
        validatedProbeResults = validation.validResults,
        rulePlanIds = Set("pieceactivation"),
        isWhiteToMove = true,
        droppedProbeCount = validation.droppedCount,
        droppedProbeReasons = validation.droppedReasons,
        invalidByRequestId = validation.invalidByRequestId,
        softByRequestId = validation.softByRequestId
      )

    assertEquals(validation.droppedCount, 0)
    assertEquals(partition.mainPlans.map(_.planId), List("PieceActivation"), clue(partition.mainPlans))
    assertEquals(
      partition.selectedMainEvaluatedPlans.map(_.hypothesis.planId),
      List("PieceActivation"),
      clue(partition.selectedMainEvaluatedPlans)
    )
    assert(
      partition.selectedMainEvaluatedPlans.exists(ep =>
        ep.userFacingEligibility == PlanEvidenceEvaluator.UserFacingPlanEligibility.ProbeBacked &&
          ep.supportProbeIds.contains("probe_soft_support")
      ),
      clue(partition.selectedMainEvaluatedPlans)
    )
    val entry = partition.diagnosticSidecar.entries.find(_.planId == "PieceActivation").getOrElse(fail("missing entry"))
    assert(entry.reasonCodes.contains("purpose_mismatch"), clue(entry.reasonCodes))
    assert(entry.reasonCodes.contains("engine_config_mismatch"), clue(entry.reasonCodes))
  }

  test("partition records alternative dominance without treating it as refutation") {
    val strongerSibling =
      hypothesis(
        id = "PieceActivationMain",
        name = "Piece activation main route",
        score = 0.86,
        sources = List("support:engine_hypothesis", "theme:piece_redeployment")
      )
    val supportedSibling =
      hypothesis(
        id = "PieceActivationSide",
        name = "Piece activation side route",
        score = 0.68,
        sources = List("theme:piece_redeployment")
      )
    val request = ProbeRequest(
      id = "probe_side_support",
      fen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1",
      moves = List("g1f3"),
      depth = 18,
      purpose = Some("theme_plan_validation"),
      planId = Some("PieceActivationSide"),
      requiredSignals = List("replyPvs", "futureSnapshot")
    )
    val result = ProbeResult(
      id = "probe_side_support",
      evalCp = 18,
      bestReplyPv = List("g8f6", "e2e4"),
      replyPvs = Some(List(List("g8f6", "e2e4"))),
      deltaVsBaseline = 6,
      keyMotifs = List("piece_activation"),
      purpose = Some("theme_plan_validation"),
      futureSnapshot = Some(
        FutureSnapshot(
          resolvedThreatKinds = Nil,
          newThreatKinds = Nil,
          targetsDelta = TargetsDelta(Nil, Nil, List("e4"), Nil),
          planBlockersRemoved = List("development_complete"),
          planPrereqsMet = List("piece_activation_ready")
        )
      )
    )

    val partition =
      PlanEvidenceEvaluator.partition(
        hypotheses = List(strongerSibling, supportedSibling),
        probeRequests = List(request),
        validatedProbeResults = validatedResults(request, result),
        rulePlanIds = Set("pieceactivationmain"),
        isWhiteToMove = true,
        droppedProbeCount = 0
      )

    val sideEntry = partition.diagnosticSidecar.entries.find(_.planId == "PieceActivationSide").getOrElse(fail("missing side entry"))
    assertEquals(sideEntry.userFacingEligibility, "probe_backed")
    assertEquals(sideEntry.status, "playable_evidence_backed")
    assert(sideEntry.alternativeDominance)
    assert(sideEntry.reasonCodes.contains("alternative_dominance"))
    assert(!sideEntry.reasonCodes.contains("probe_refuted"))
    assertEquals(partition.selectedMainEvaluatedPlans.map(_.hypothesis.planId), List("PieceActivationSide"))
  }

  test("strategic plan evidence view projects typed selected and blocked plans") {
    val selectedPlan =
      hypothesis(
        id = "PieceActivation",
        name = "Piece activation",
        score = 0.78,
        sources = List("support:engine_hypothesis")
      )
    val deferredPlan =
      hypothesis(
        id = "PawnStorm",
        name = "Pawn storm",
        score = 0.64,
        sources = List("latent_seed:kingside_rook_pawn_march")
      )
    val request = ProbeRequest(
      id = "probe_view_support",
      fen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1",
      moves = List("g1f3"),
      depth = 20,
      purpose = Some("theme_plan_validation"),
      planId = Some("PieceActivation"),
      requiredSignals = List("replyPvs", "futureSnapshot")
    )
    val result = ProbeResult(
      id = "probe_view_support",
      evalCp = 24,
      bestReplyPv = List("g8f6", "e2e4"),
      replyPvs = Some(List(List("g8f6", "e2e4"))),
      deltaVsBaseline = 8,
      keyMotifs = List("piece_activation"),
      futureSnapshot = Some(
        FutureSnapshot(
          resolvedThreatKinds = Nil,
          newThreatKinds = Nil,
          targetsDelta = TargetsDelta(Nil, Nil, List("e4"), Nil),
          planBlockersRemoved = List("development_complete"),
          planPrereqsMet = List("piece_activation_ready")
        )
      )
    )

    val partition =
      PlanEvidenceEvaluator.partition(
        hypotheses = List(selectedPlan, deferredPlan),
        probeRequests = List(request),
        validatedProbeResults = validatedResults(request, result),
        rulePlanIds = Set("pieceactivation"),
        isWhiteToMove = true,
        droppedProbeCount = 0
      )
    val view = PlanEvidenceEvaluator.StrategicPlanEvidenceView.from(partition)

    assertEquals(view.selectedPlans.map(_.hypothesis.planId), List("PieceActivation"))
    assertEquals(view.probeBackedPlans.map(_.hypothesis.planId), List("PieceActivation"))
    assertEquals(view.deferredPlans.map(_.hypothesis.planId), List("PawnStorm"))
    assert(view.hasProbeBacked)
    assertEquals(view.bestEvidenceFor("PieceActivation", None).map(_.userFacingEligibility), Some(PlanEvidenceEvaluator.UserFacingPlanEligibility.ProbeBacked))
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
        validatedProbeResults = validatedResults(request, result),
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
        validatedProbeResults =
          validatedResults(downgradedRequest, downgradedResult) ++
            validatedResults(blockedRequest, blockedResult),
        rulePlanIds = Set.empty,
        isWhiteToMove = true,
        droppedProbeCount = 0
      )

    assert(partition.diagnosticSidecar.blockedStrongClaims >= 1)
    assert(partition.diagnosticSidecar.downgradedWeakClaims >= 1)
    assert(partition.diagnosticSidecar.quantifierFailures >= 1)
    assert(partition.diagnosticSidecar.stabilityFailures >= 1)
  }

  test("partition keeps structural-only plans out of selected main plans when probe-backed is missing") {
    val structuralPlan =
      hypothesis(
        id = "StructuralBind",
        name = "Structural bind plan",
        score = 0.82,
        sources = List("support:engine_hypothesis", "structural_state:pawn_duo")
      )
    val structuralRequest = ProbeRequest(
      id = "probe_structural_bind",
      fen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1",
      moves = List("e2e4"),
      depth = 20,
      planId = Some("StructuralBind"),
      requiredSignals = List("replyPvs", "futureSnapshot")
    )

    val partition =
      PlanEvidenceEvaluator.partition(
        hypotheses = List(structuralPlan),
        probeRequests = List(structuralRequest),
        validatedProbeResults = Nil,
        rulePlanIds = Set.empty,
        isWhiteToMove = true,
        droppedProbeCount = 0
      )

    assertEquals(partition.mainPlans, Nil)
    val evaluated = partition.evaluated.headOption.getOrElse(fail("missing structural plan"))
    assertEquals(evaluated.status, PlanEvidenceEvaluator.PlanEvidenceStatus.PlayableStructuralOnly)
    assertEquals(evaluated.userFacingEligibility, PlanEvidenceEvaluator.UserFacingPlanEligibility.StructuralOnly)
    assert(
      partition.diagnosticSidecar.entries.exists(entry =>
        entry.planId == "StructuralBind" &&
          entry.status == "playable_structural_only" &&
          entry.userFacingEligibility == "structural_only"
      ),
      clue(partition.diagnosticSidecar)
    )
  }
