package lila.llm.analysis

import chess.Square
import munit.FunSuite
import lila.llm.model.*
import lila.llm.model.authoring.*
import lila.llm.model.strategic.PreventedPlan

class CounterplayAxisSuppressionCertificationTest extends FunSuite:

  private val QueenlessLateMiddlegameFen =
    "2r2rk1/pp3pp1/2n1p2p/3p4/3P1P2/2P1PN1P/PP4P1/2R2RK1 w - - 0 23"
  private val HeavyPieceMiddlegameFen =
    "r1bq1rk1/pp3ppp/2n1pn2/2bp4/3P4/2N1PN2/PPQ2PPP/R1B2RK1 w - - 0 12"
  private val PureEndgameFen =
    "8/5pk1/3b2p1/3P4/5P2/6P1/5BK1/8 w - - 0 45"

  private def restrictionHypothesis(
      id: String = "named_break_suppression",
      name: String = "Clamp the ...c5 break",
      subplanId: String = ThemeTaxonomy.SubplanId.BreakPrevention.id,
      executionSteps: List[String] = List("Keep the ...c5 break closed before expanding.")
  ): PlanHypothesis =
    PlanHypothesis(
      planId = id,
      planName = name,
      rank = 1,
      score = 0.79,
      preconditions = Nil,
      executionSteps = executionSteps,
      failureModes = List("If the clamp slips, counterplay comes back."),
      viability = PlanViability(score = 0.79, label = "high", risk = "B2b unit"),
      refutation = None,
      evidenceSources = List("theme:restriction_prophylaxis"),
      themeL1 = ThemeTaxonomy.ThemeL1.RestrictionProphylaxis.id,
      subplanId = Some(subplanId)
    )

  private def evaluatedRestrictionPlan(
      supportProbeIds: List[String],
      subplanId: String = ThemeTaxonomy.SubplanId.BreakPrevention.id,
      hypothesis: PlanHypothesis = restrictionHypothesis(),
      claimCertification: PlanEvidenceEvaluator.ClaimCertification =
        PlanEvidenceEvaluator.ClaimCertification(
          certificateStatus = PlayerFacingCertificateStatus.Valid,
          quantifier = PlayerFacingClaimQuantifier.BestResponse,
          modalityTier = PlayerFacingClaimModalityTier.Advances,
          attributionGrade = PlayerFacingClaimAttributionGrade.Distinctive,
          stabilityGrade = PlayerFacingClaimStabilityGrade.Stable,
          provenanceClass = PlayerFacingClaimProvenanceClass.ProbeBacked,
          ontologyFamily = PlayerFacingClaimOntologyFamily.LongTermRestraint
        ),
      status: PlanEvidenceEvaluator.PlanEvidenceStatus =
        PlanEvidenceEvaluator.PlanEvidenceStatus.PlayableEvidenceBacked,
      missingSignals: List[String] = Nil,
      pvCoupled: Boolean = false
  ): PlanEvidenceEvaluator.EvaluatedPlan =
    PlanEvidenceEvaluator.EvaluatedPlan(
      hypothesis = hypothesis.copy(subplanId = Some(subplanId)),
      status = status,
      userFacingEligibility = PlanEvidenceEvaluator.UserFacingPlanEligibility.ProbeBacked,
      reason = "B2b unit",
      supportProbeIds = supportProbeIds,
      refuteProbeIds = Nil,
      missingSignals = missingSignals,
      pvCoupled = pvCoupled,
      themeL1 = ThemeTaxonomy.ThemeL1.RestrictionProphylaxis.id,
      subplanId = Some(subplanId),
      claimCertification = claimCertification
    )

  private def supportProbe(
      id: String,
      purpose: String = ThemePlanProbePurpose.LongTermRestraintValidation,
      bestReplyPv: List[String] = List("f8e8", "c1c8"),
      replyPvs: Option[List[List[String]]] =
        Some(
          List(
            List("f8e8", "c1c8"),
            List("a7a5", "g2g4")
          )
        ),
      collapseReason: Option[String] = None,
      futureSnapshot: Option[FutureSnapshot] =
        Some(
          FutureSnapshot(
            resolvedThreatKinds = List("Counterplay"),
            newThreatKinds = Nil,
            targetsDelta = TargetsDelta(Nil, Nil, Nil, List("c5")),
            planBlockersRemoved = List("...c5 break denied"),
            planPrereqsMet = List("queenside counterplay stays muted")
          )
        ),
      keyMotifs: List[String] = List("...c5 break denied", "counterplay restrained")
  ): ProbeResult =
    ProbeResult(
      id = id,
      evalCp = 185,
      bestReplyPv = bestReplyPv,
      replyPvs = replyPvs,
      deltaVsBaseline = 12,
      keyMotifs = keyMotifs,
      purpose = Some(purpose),
      l1Delta =
        Some(
          L1DeltaSnapshot(
            materialDelta = 0,
            kingSafetyDelta = 0,
            centerControlDelta = 1,
            openFilesDelta = 0,
            mobilityDelta = -2,
            collapseReason = collapseReason
          )
        ),
      futureSnapshot = futureSnapshot
    )

  private def preventedPlan(
      breakNeutralized: Option[String] = Some("...c5"),
      deniedSquare: String = "c5",
      deniedResourceClass: Option[String] = Some("break"),
      counterplayScoreDrop: Int = 140,
      breakNeutralizationStrength: Option[Int] = Some(82),
      defensiveSufficiency: Option[Int] = Some(78)
  ): PreventedPlan =
    PreventedPlan(
      planId = "deny_counterplay",
      deniedSquares = List(Square.fromKey(deniedSquare).get),
      breakNeutralized = breakNeutralized,
      mobilityDelta = -2,
      counterplayScoreDrop = counterplayScoreDrop,
      preventedThreatType = Some("counterplay"),
      deniedResourceClass = deniedResourceClass,
      breakNeutralizationStrength = breakNeutralizationStrength,
      defensiveSufficiency = defensiveSufficiency,
      sourceScope = FactScope.Now
    )

  private def certification(
      plan: PlanEvidenceEvaluator.EvaluatedPlan,
      probes: List[ProbeResult],
      preventedPlans: List[PreventedPlan] = List(preventedPlan()),
      evalCp: Int = 185,
      phase: String = "middlegame",
      ply: Int = 28,
      fen: String = QueenlessLateMiddlegameFen
  ): CounterplayAxisSuppressionCertification.Contract =
    CounterplayAxisSuppressionCertification
      .evaluate(
        plan = plan,
        probeResultsById = probes.map(result => result.id -> result).toMap,
        preventedPlans = preventedPlans,
        evalCp = evalCp,
        isWhiteToMove = true,
        phase = phase,
        ply = ply,
        fen = fen
      )
      .getOrElse(fail("expected counterplay-axis suppression contract"))

  test("true_named_break_suppression passes certification for a late-middlegame clamp") {
    val cert =
      certification(
        plan = evaluatedRestrictionPlan(List("probe_true")),
        probes = List(supportProbe(id = "probe_true"))
      )

    assert(cert.certified, clue(cert))
    assertEquals(cert.claimScope, "break_axis")
    assertEquals(cert.squeezeArchetype, "prophylactic_clamp")
    assertEquals(cert.restrictionEvidence.namedAxis, "...c5")
    assertEquals(cert.defenderResources, List("f8e8", "a7a5"))
    assertEquals(cert.bestDefenseFound, Some("f8e8"))
    assert(cert.routePersistence.axisStillSuppressed, clue(cert))
    assertEquals(cert.counterplayReinflationRisk, "bounded_axis_only")
  }

  test("hidden_freeing_break fails when more than one live axis survives") {
    val cert =
      certification(
        plan = evaluatedRestrictionPlan(List("probe_hidden_break")),
        probes = List(supportProbe(id = "probe_hidden_break")),
        preventedPlans =
          List(
            preventedPlan(breakNeutralized = Some("...c5"), deniedSquare = "c5"),
            preventedPlan(breakNeutralized = Some("...e5"), deniedSquare = "e5")
          )
      )

    assert(!cert.certified, clue(cert))
    assert(cert.failsIf.contains("hidden_freeing_break"), clue(cert))
    assertEquals(cert.freeingBreaksRemaining, List("...e5"), clue(cert))
  }

  test("hidden_tactical_release fails when best defense revives forcing play") {
    val cert =
      certification(
        plan = evaluatedRestrictionPlan(List("probe_tactical_release")),
        probes =
          List(
            supportProbe(
              id = "probe_tactical_release",
              futureSnapshot =
                Some(
                  FutureSnapshot(
                    resolvedThreatKinds = List("Counterplay"),
                    newThreatKinds = List("Perpetual"),
                    targetsDelta = TargetsDelta(List("g2"), Nil, Nil, List("c5")),
                    planBlockersRemoved = List("...c5 break denied"),
                    planPrereqsMet = List("queenside counterplay stays muted")
                  )
                ),
              keyMotifs = List("...c5 break denied", "exchange sac resource")
            )
          )
      )

    assert(!cert.certified, clue(cert))
    assert(cert.failsIf.contains("hidden_tactical_release"), clue(cert))
    assert(cert.tacticalReleasesRemaining.nonEmpty, clue(cert))
  }

  test("move_order_fragile_clamp fails when best defense collapses the shell") {
    val cert =
      certification(
        plan = evaluatedRestrictionPlan(List("probe_fragile")),
        probes =
          List(
            supportProbe(
              id = "probe_fragile",
              collapseReason = Some("wrong order lets the c-file open")
            )
          )
      )

    assert(!cert.certified, clue(cert))
    assert(cert.moveOrderFragility.fragile, clue(cert))
    assert(cert.failsIf.contains("move_order_fragility"), clue(cert))
  }

  test("pv_restatement_only fails without restriction validation evidence") {
    val cert =
      certification(
        plan = evaluatedRestrictionPlan(List("probe_pv_only")),
        probes =
          List(
            supportProbe(
              id = "probe_pv_only",
              purpose = ThemePlanProbePurpose.ThemePlanValidation
            )
          )
      )

    assert(!cert.certified, clue(cert))
    assert(cert.failsIf.contains("pv_restatement_only"), clue(cert))
  }

  test("waiting_move_only fails when no measurable restriction delta exists") {
    val cert =
      certification(
        plan = evaluatedRestrictionPlan(List("probe_waiting")),
        probes =
          List(
            supportProbe(
              id = "probe_waiting",
              futureSnapshot =
                Some(
                  FutureSnapshot(
                    resolvedThreatKinds = Nil,
                    newThreatKinds = Nil,
                    targetsDelta = TargetsDelta(Nil, Nil, Nil, Nil),
                    planBlockersRemoved = Nil,
                    planPrereqsMet = Nil
                  )
                )
            )
          ),
        preventedPlans =
          List(
            preventedPlan(
              counterplayScoreDrop = 30,
              breakNeutralizationStrength = Some(40),
              defensiveSufficiency = Some(35)
            )
          )
      )

    assert(!cert.certified, clue(cert))
    assert(cert.failsIf.contains("waiting_move_disguised_as_plan"), clue(cert))
  }

  test("local_overreach_shell fails outside the narrow late-middlegame clearly-better slice") {
    val cert =
      certification(
        plan = evaluatedRestrictionPlan(List("probe_overreach")),
        probes = List(supportProbe(id = "probe_overreach")),
        evalCp = 115,
        fen = HeavyPieceMiddlegameFen,
        ply = 18
      )

    assert(!cert.certified, clue(cert))
    assert(cert.failsIf.contains("local_to_global_overreach"), clue(cert))
  }

  test("surface_reinflation fails when the shell language overclaims global no-counterplay") {
    val cert =
      certification(
        plan =
          evaluatedRestrictionPlan(
            List("probe_reinflate"),
            hypothesis =
              restrictionHypothesis(
                name = "Leave Black with no counterplay forever",
                executionSteps = List("Completely shut Black down and win by force.")
              )
          ),
        probes = List(supportProbe(id = "probe_reinflate"))
      )

    assert(!cert.certified, clue(cert))
    assert(cert.failsIf.contains("surface_reinflation"), clue(cert))
  }

  test("pure_endgame_shell fails closed even when the same local clamp exists") {
    val cert =
      certification(
        plan = evaluatedRestrictionPlan(List("probe_endgame")),
        probes = List(supportProbe(id = "probe_endgame")),
        phase = "endgame",
        ply = 90,
        fen = PureEndgameFen
      )

    assert(!cert.certified, clue(cert))
    assert(cert.failsIf.contains("local_to_global_overreach"), clue(cert))
  }
