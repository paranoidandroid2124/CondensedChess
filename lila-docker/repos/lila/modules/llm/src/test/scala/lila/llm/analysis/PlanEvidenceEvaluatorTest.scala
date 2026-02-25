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

  test("partition promotes engine-coupled plan to main and keeps unevidenced idea latent") {
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

    assertEquals(partition.mainPlans.map(_.planId), List("CentralControl"))
    assert(partition.latentPlans.exists(_.seedId == "kingside_rook_pawn_march"))
    assert(partition.whyAbsentFromTopMultiPV.nonEmpty)
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
    assertEquals(partition.latentPlans, Nil)
    assert(
      partition.whyAbsentFromTopMultiPV.exists(_.toLowerCase.contains("refutation")),
      clue(partition.whyAbsentFromTopMultiPV)
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
    assertEquals(partition.latentPlans.map(_.seedId), List("kingside_rook_pawn_march"))
    assert(
      partition.whyAbsentFromTopMultiPV.exists(_.contains("discarded")),
      clue(partition.whyAbsentFromTopMultiPV)
    )
  }
