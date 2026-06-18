package lila.commentary.tools.quality

import munit.FunSuite
import lila.commentary.analysis.QuestionPlanFallbackMode
import lila.commentary.tools.review.CommentaryPlayerQcSupport

class CommentaryQualityContrastSupportTest extends FunSuite:

  import CommentaryPlayerQcSupport.*
  import CommentaryQualityContrastSupport.*

  private final case class LocalFactSpec(
      family: String,
      authority: String,
      producer: Option[String],
      evidenceRefs: List[String]
  )

  private def localFactPayload(family: String, authority: String, evidenceRefs: String*): Option[LocalFactSpec] =
    localFactPayload(family, authority, evidenceRefs.toList)

  private def localFactPayload(family: String, authority: String, evidenceRefs: List[String]): Option[LocalFactSpec] =
    Some(LocalFactSpec(family = family, authority = authority, producer = None, evidenceRefs = evidenceRefs))

  private def producedLocalFactPayload(
      family: String,
      authority: String,
      producer: String,
      evidenceRefs: String*
  ): Option[LocalFactSpec] =
    producedLocalFactPayload(family, authority, producer, evidenceRefs.toList)

  private def producedLocalFactPayload(
      family: String,
      authority: String,
      producer: String,
      evidenceRefs: List[String]
  ): Option[LocalFactSpec] =
    Some(LocalFactSpec(family = family, authority = authority, producer = Some(producer), evidenceRefs = evidenceRefs))

  private def moveReviewEntry(
      sampleId: String,
      commentary: String,
      primaryKind: Option[String],
      selectedQuestion: Option[String],
      selectedOwnerKind: Option[String],
      selectedSource: Option[String],
      fallbackMode: String = MoveReviewFallbackMode.PlannerOwned,
      fen: String = "fen",
      playedSan: String = "Re1",
      playedUci: String = "e2e4",
      cacheHit: Boolean = false,
      contrastAdmissible: Option[Boolean] = Some(true),
      contrastRejectReason: Option[String] = None,
      plannerSceneType: Option[String] = Some("forcing_defense"),
      plannerDroppedOwners: List[String] = Nil,
      truthClass: Option[String] = Some("Best"),
      truthReasonFamily: Option[String] = None,
      truthFailureMode: Option[String] = None,
      truthOnlyMoveDefense: Option[Boolean] = None,
      truthBenchmarkCriticalMove: Option[Boolean] = None,
      localFact: Option[LocalFactSpec] = None,
      localFactFamilies: List[String] = Nil,
      localFactAuthorities: List[String] = Nil,
      localFactProducers: List[String] = Nil,
      localFactEvidenceRefs: List[String] = Nil,
      supportedLocalAdmittedFamilies: List[String] = Nil,
      causalEvidenceSources: List[String] = Nil,
      causalQuestion: Option[String] = None,
      causalRelations: List[String] = Nil,
      supportRows: List[SupportRow] = Nil,
      advancedRows: List[SupportRow] = Nil,
      causalSupportEmbedded: Option[Boolean] = None,
      causalLocalFactGuardrails: List[String] = Nil
  ): MoveReviewOutputEntry =
    MoveReviewOutputEntry(
      sampleId = sampleId,
      gameKey = "g1",
      sliceKind = "strategic_choice",
      targetPly = 14,
      fen = fen,
      playedSan = playedSan,
      playedUci = playedUci,
      opening = None,
      commentary = commentary,
      supportRows = supportRows,
      advancedRows = advancedRows,
      sourceMode = "rule",
      model = None,
      rawResponsePath = "raw",
      variationCount = 1,
      cacheHit = cacheHit,
      plannerPrimaryKind = primaryKind,
      plannerPrimaryFallbackMode = primaryKind.map(_ => QuestionPlanFallbackMode.PlannerOwned.toString),
      plannerSecondaryKind = None,
      plannerSecondarySurfaced = false,
      moveReviewFallbackMode = fallbackMode,
      plannerSceneType = plannerSceneType,
      plannerDroppedOwners = plannerDroppedOwners,
      plannerSelectedQuestion = selectedQuestion,
      plannerSelectedOwnerKind = selectedOwnerKind,
      plannerSelectedSource = selectedSource,
      surfaceReplayOutcome =
        Some(MoveReviewFallbackMode.replayOutcome(fallbackMode)),
      contrast_admissible = contrastAdmissible,
      contrast_reject_reason = contrastRejectReason,
      truthClass = truthClass,
      truthReasonFamily = truthReasonFamily,
      truthFailureMode = truthFailureMode,
      truthOnlyMoveDefense = truthOnlyMoveDefense,
      truthBenchmarkCriticalMove = truthBenchmarkCriticalMove,
      supportedLocalAdmittedFamilies = supportedLocalAdmittedFamilies,
      moveReviewLocalFactFamilies = localFact.map(fact => List(fact.family)).getOrElse(localFactFamilies),
      moveReviewLocalFactAuthorities = localFact.map(fact => List(fact.authority)).getOrElse(localFactAuthorities),
      moveReviewLocalFactProducers = localFact.map(_.producer.toList).getOrElse(localFactProducers),
      moveReviewLocalFactEvidenceRefs = localFact.map(_.evidenceRefs).getOrElse(localFactEvidenceRefs),
      moveReviewCausalClaimEvidenceSources = causalEvidenceSources,
      moveReviewCausalClaimQuestion = causalQuestion,
      moveReviewCausalClaimRelations = causalRelations,
      moveReviewCausalClaimSupportEmbedded = causalSupportEmbedded,
      moveReviewCausalClaimLocalFactGuardrails = causalLocalFactGuardrails
    )

  private def contrastReport(
      beforeEntries: List[MoveReviewOutputEntry],
      afterEntries: List[MoveReviewOutputEntry],
      codeCost: Option[CodeCostSummary] = None,
      clue: String = "expected contrast report"
  ): ContrastReport =
    CommentaryQualityContrastSupport
      .buildContrastReport(beforeEntries = beforeEntries, afterEntries = afterEntries, codeCost = codeCost)
      .getOrElse(fail(clue))

  private def gatePair(
      before: MoveReviewOutputEntry,
      after: MoveReviewOutputEntry,
      codeCost: Option[CodeCostSummary] = None,
      clue: String = "expected contrast report"
  ): (MoveReviewQualityGateRow, MoveReviewQualityGateSummary) =
    val report = contrastReport(List(before), List(after), codeCost, clue)
    val gateRow = report.moveReviewGateRows.headOption.getOrElse(fail("missing gate row"))
    val gate = report.moveReviewGate.getOrElse(fail("missing gate summary"))
    gateRow -> gate

  private def sameRowGate(row: MoveReviewOutputEntry): MoveReviewQualityGateRow =
    contrastReport(List(row), List(row)).moveReviewGateRows.headOption.getOrElse(fail("missing gate row"))

  private def defaultCodeCost(
      added: Int,
      deleted: Int = 0,
      helperCount: Int = 0,
      reused: List[String] = List("CommentaryQualityContrastSupport")
  ): Option[CodeCostSummary] =
    Some(CodeCostSummary(netLineAdded = added, netLineDeleted = deleted, newHelperCount = Some(helperCount), reusedExistingHelperNames = reused))

  private def assertGatePass(
      gate: MoveReviewQualityGateSummary,
      wrongTierDelta: Option[Int] = None,
      wrongFamilyDelta: Option[Int] = None,
      harmfulOverclaimDelta: Option[Int] = None,
      genericFallbackDelta: Option[Int] = None,
      exactEvidenceSurfaceDelta: Option[Int] = None,
      evidenceBoundSurfaceDelta: Option[Int] = None
  ): Unit =
    assertEquals(gate.finalStatus, MoveReviewGateStatus.Pass)
    wrongTierDelta.foreach(expected => assertEquals(gate.wrongTierRiskDelta, expected))
    wrongFamilyDelta.foreach(expected => assertEquals(gate.wrongFamilyRiskDelta, expected))
    harmfulOverclaimDelta.foreach(expected => assertEquals(gate.harmfulOverclaimDelta, expected))
    genericFallbackDelta.foreach(expected => assertEquals(gate.genericFallbackDelta, expected))
    exactEvidenceSurfaceDelta.foreach(expected => assertEquals(gate.exactEvidenceSurfaceDelta, expected))
    evidenceBoundSurfaceDelta.foreach(expected => assertEquals(gate.evidenceBoundSurfaceDelta, expected))

  test("after moveReview exact factual rows are blocked out of eligible contrast gain metrics") {
    val sampleId = "g1:strategic_choice:14:moveReview"
    val before =
      moveReviewEntry(
        sampleId = sampleId,
        commentary = "14... Re1: This keeps the e-file under control before the reply lands.",
        primaryKind = Some("WhyNow"),
        selectedQuestion = Some("WhyNow"),
        selectedOwnerKind = Some("ForcingDefense"),
        selectedSource = Some("truth_contract")
      )
    val after =
      moveReviewEntry(
        sampleId = sampleId,
        commentary = "14... Re1: This puts the rook on e1.",
        primaryKind = None,
        selectedQuestion = None,
        selectedOwnerKind = None,
        selectedSource = None,
        fallbackMode = MoveReviewFallbackMode.ExactFactual,
        contrastAdmissible = Some(false)
      )

    val report = contrastReport(List(before), List(after))

    val row = report.selectorRows.headOption.getOrElse(fail("missing selector row"))
    assertEquals(row.selectionStatus, SelectionStatus.AfterFallbackBlocked, clues(row))
    assertEquals(row.selectionReason, "after_move_review_exact_factual")
    assertEquals(report.evalRows, Nil)
    assertEquals(report.summary.contrastEligibleRows, 0)
    assertEquals(report.summary.afterFallbackCount, 0)
    assertEquals(report.summary.afterFallbackBlockedRows, 1)
    assertEquals(report.summary.degradedCount, 0)
    assertEquals(report.summary.baselineRegressionStatus, "no_baseline_regression")
  }

  test("contrast eval reviews exact typed local facts instead of rejecting them for missing contrast slot") {
    val sampleId = "2026_03_01_deflauzano_ly3257_sample_237:long_structural_squeeze:26:moveReview"
    val commentary =
      "13... c4: c4 attacks the bishop on d3. The PV keeps the pressure on the d3 bishop local to c4: Bc2 moves that bishop from d3. Short line: c4 Bc2 Bc5 Kh1 f5."
    val targetPressureRefs =
      List(
        "evidence_source:typed_local_fact",
        "evidence_line_binding:pv_coupled",
        "typed_local_fact_source:canonical_fact",
        "typed_local_fact_family:pressure",
        "typed_local_fact_producer:target_pressure",
        "fact_kind:target_piece",
        "fact_scope:now",
        "fact_square:d3",
        "fact_square:c4",
        "fact_role:bishop",
        "fact_source:post_move_static"
      )
    def row(contrastAdmissible: Option[Boolean], rejectReason: Option[String]) =
      moveReviewEntry(
        sampleId = sampleId,
        commentary = commentary,
        primaryKind = Some("WhyThis"),
        selectedQuestion = Some("WhyThis"),
        selectedOwnerKind = Some("MoveDelta"),
        selectedSource = Some("canonical_fact"),
        localFact = producedLocalFactPayload("pressure", "canonical_fact", "target_pressure", targetPressureRefs),
        causalEvidenceSources = List("branch_line", "typed_local_fact"),
        supportRows = List(SupportRow("Checked line", "Short line: c4 Bc2 Bc5 Kh1 f5.")),
        contrastAdmissible = contrastAdmissible,
        contrastRejectReason = rejectReason
      )

    val report =
      contrastReport(
        beforeEntries = List(row(Some(true), None)),
        afterEntries = List(row(Some(false), Some("missing_concrete_consequence")))
      )
    val eval = report.evalRows.headOption.getOrElse(fail("missing eval row"))

    assertEquals(eval.selectionStatus, SelectionStatus.Eligible, clues(eval))
    assertEquals(eval.finalVerdict, FinalVerdict.Review, clues(eval))
    assertEquals(report.summary.eligibleRejectCount, 0, clues(report.summary))
    assertEquals(report.summary.eligibleReviewCount, 1, clues(report.summary))
  }

  test("moveReview regression gate passes when field-backed overclaim risk drops and exact evidence surfaces") {
    val sampleId = "g1:prophylaxis:5:moveReview"
    val before =
      moveReviewEntry(
        sampleId = sampleId,
        commentary =
          "3. d4: The timing matters now because this also plays the d2-d4 break. Only the played move still keeps the position together now.",
        primaryKind = Some("WhyNow"),
        selectedQuestion = Some("WhyNow"),
        selectedOwnerKind = Some("ForcingDefense"),
        selectedSource = Some("only_move_defense"),
        truthReasonFamily = Some("OnlyMoveDefense"),
        truthOnlyMoveDefense = Some(true),
        truthBenchmarkCriticalMove = Some(true),
        localFact = localFactPayload("timing", "forced_reply", "evidence_source:delayed_only_move"),
        causalEvidenceSources = List("delayed_only_move")
      )
    val after =
      moveReviewEntry(
        sampleId = sampleId,
        commentary = "3. d4: On the checked line, this also plays the d2-d4 break at this moment. Short line: d4 Bf5 Nf3 e6 c3.",
        primaryKind = None,
        selectedQuestion = None,
        selectedOwnerKind = None,
        selectedSource = None,
        fallbackMode = MoveReviewFallbackMode.ExactFactual,
        truthReasonFamily = Some("QuietTechnicalMove"),
        truthOnlyMoveDefense = Some(false),
        truthBenchmarkCriticalMove = Some(false),
        localFact = producedLocalFactPayload(
            "timing",
            "certified_strategy",
            "certified_strategy_delta",
            "typed_local_fact_source:certified_strategy_support",
            "proof_family:central_break_timing",
            "proof_source:central_break_timing"
        ),
        supportedLocalAdmittedFamilies = List("central_break_timing"),
        causalEvidenceSources = List("branch_line", "typed_local_fact"),
        supportRows =
          List(
            SupportRow(
              label = "Central break",
              text = "On the checked line, this also plays the d2-d4 break at this moment."
            )
          )
      )

    val (_, gate) = gatePair(before, after, defaultCodeCost(added = 4, deleted = 2))
    assertEquals(gate.role, MoveReviewGateRole.RegressionBlockingGate)
    assertEquals(gate.finalStatus, MoveReviewGateStatus.Pass)
    assertEquals(gate.harmfulOverclaimDelta, -1)
    assertEquals(gate.wrongFamilyRiskDelta, -1)
    assertEquals(gate.genericFallbackDelta, 0)
    assertEquals(gate.exactEvidenceSurfaceDelta, 1)
    assertEquals(gate.evidenceBoundSurfaceDelta, 1)
    assertEquals(gate.sourceIdentityStatus, "same_position_cache_identity")
    assertEquals(gate.codeCost.map(_.netLineDelta), Some(2))
    assert(gate.blockingReasons.isEmpty, clues(gate))
  }

  test("moveReview regression gate does not count truth-only-move after certified plan-support rebound") {
    val sampleId = "g1:tactical_turn:8:moveReview"
    val before =
      moveReviewEntry(
        sampleId = sampleId,
        commentary =
          "4... d6: The timing matters now because other moves allow the position to slip away. Only the played move still keeps the position together now.",
        primaryKind = Some("WhyNow"),
        selectedQuestion = Some("WhyNow"),
        selectedOwnerKind = Some("ForcingDefense"),
        selectedSource = Some("only_move_defense"),
        truthReasonFamily = Some("OnlyMoveDefense"),
        truthOnlyMoveDefense = Some(true),
        truthBenchmarkCriticalMove = Some(true),
        localFact = producedLocalFactPayload(
            "timing",
            "forced_reply",
            "forced_reply",
            "evidence_source:delayed_only_move",
            "contrast_anchor:d6"
        ),
        causalEvidenceSources = List("delayed_only_move", "timing_witness", "typed_local_fact")
      )
    val after =
      moveReviewEntry(
        sampleId = sampleId,
        commentary =
          "4... d6: d6 is tied to checked plan support around d-file. The PV keeps the plan-support detail bounded: d6 is the move under review, the checked continuation begins with Qxf7+.",
        primaryKind = None,
        selectedQuestion = None,
        selectedOwnerKind = None,
        selectedSource = None,
        fallbackMode = MoveReviewFallbackMode.ExactFactual,
        truthReasonFamily = Some("OnlyMoveDefense"),
        truthOnlyMoveDefense = Some(true),
        truthBenchmarkCriticalMove = Some(true),
        localFact = producedLocalFactPayload(
            "plan_support",
            "certified_strategy",
            "certified_strategy_delta",
            "evidence_source:typed_local_fact",
            "evidence_subject:played_move",
            "evidence_line_binding:pv_coupled",
            "typed_local_fact_source:practical_position_support",
            "typed_local_fact_family:plan_support",
            "typed_local_fact_producer:certified_strategy_delta",
            "anchor:d-file",
            "strategy_ref:source:pawn_break_motif"
        ),
        causalEvidenceSources = List("branch_line", "typed_local_fact"),
        causalQuestion = Some("WhyThis"),
        causalRelations = List("played_move_consequence"),
        supportRows =
          List(
            SupportRow(
              label = "Checked line",
              text = "The PV keeps the plan-support detail bounded: d6 is the move under review, the checked continuation begins with Qxf7+."
            )
          )
      )

    val (gateRow, gate) = gatePair(before, after, defaultCodeCost(added = 8, deleted = 2))
    assert(gateRow.beforeReasons.contains("truth_only_move_defense"), clues(gateRow))
    assert(!gateRow.afterReasons.contains("truth_only_move_defense"), clues(gateRow))
    assert(!gateRow.afterHarmfulOverclaimRisk, clues(gateRow))

    assertEquals(gate.harmfulOverclaimDelta, -1)
    assertEquals(gate.finalStatus, MoveReviewGateStatus.Pass)
  }

  test("moveReview regression gate does not count bounded non-unique reply loss as harmful forced reply") {
    val sampleId = "g1:opening_transition:10:moveReview"
    val row =
      moveReviewEntry(
        sampleId = sampleId,
        commentary =
          "5... e6: This addresses the opponent's material threat against the pawn on d5 on the checked line. If delayed, one defensive reply has to answer the material threat on d5.",
        primaryKind = Some("WhatMustBeStopped"),
        selectedQuestion = Some("WhatMustBeStopped"),
        selectedOwnerKind = Some("ForcingDefense"),
        selectedSource = Some("threat"),
        playedSan = "e6",
        playedUci = "e7e6",
        truthReasonFamily = Some("QuietTechnicalMove"),
        truthOnlyMoveDefense = Some(false),
        truthBenchmarkCriticalMove = Some(false),
        localFact = producedLocalFactPayload(
            "defense",
            "forced_reply",
            "forced_reply",
            "evidence_source:explicit_reply_loss",
            "evidence_subject:line_or_reply",
            "contrast_anchor:e6",
            "reply_anchor:e6",
            "reply_uci:e7e6",
            "reply_defense_count:3",
            "loss_if_ignored_cp:144",
            "turns_to_impact:1",
            "threat_kind:material",
            "threat_square:d5"
        ),
        causalEvidenceSources = List("explicit_reply_loss", "branch_line", "timing_witness"),
        causalQuestion = Some("WhatMustBeStopped"),
        causalRelations = List("defensive_resource"),
        causalLocalFactGuardrails =
          List(
            "local_fact_guardrail:explicit_reply_loss",
            "local_fact_guardrail:forced_reply_non_unique",
            "local_fact_guardrail:reply_defense_count:3",
            "local_fact_guardrail:surface_forced=false",
            "local_fact_guardrail:surface_line=replayed"
          ),
        supportRows = List(SupportRow("Checked line", "Short line: e6 h3 Bh5 Qb3 Qc7. Refs: e6 h3 Bh5 Qb3 Qc7."))
      )

    val gateRow = sameRowGate(row)
    assert(!gateRow.afterReasons.contains("local_fact_forced_reply"), clues(gateRow))
    assert(!gateRow.afterHarmfulOverclaimRisk, clues(gateRow))
    assertEquals(contrastReport(List(row), List(row)).moveReviewGate.map(_.harmfulOverclaimAfter), Some(0))
  }

  test("moveReview regression gate treats bounded non-unique reply loss diluted by practical restraint as wrong tier") {
    val sampleId = "2026_03_01_grobich_alfapenton_sample_494:opening_transition:10:moveReview"
    val base =
      moveReviewEntry(
        sampleId = sampleId,
        commentary =
          "5... e6: This addresses the opponent's material threat against the pawn on d5 on the checked line. If delayed, one defensive reply has to answer the material threat on d5.",
        primaryKind = Some("WhatMustBeStopped"),
        selectedQuestion = Some("WhatMustBeStopped"),
        selectedOwnerKind = Some("ForcingDefense"),
        selectedSource = Some("threat"),
        fen = "rn1qkb1r/pp2pppp/2p2n2/3p4/2PP2b1/2N1PN2/PP3PPP/R1BQKB1R b KQkq - 0 5",
        playedSan = "e6",
        playedUci = "e7e6",
        plannerSceneType = Some("forcing_defense"),
        truthReasonFamily = Some("QuietTechnicalMove"),
        truthOnlyMoveDefense = Some(false),
        truthBenchmarkCriticalMove = Some(false),
        localFact = producedLocalFactPayload(
            "defense",
            "forced_reply",
            "forced_reply",
            "evidence_source:explicit_reply_loss",
            "evidence_subject:line_or_reply",
            "contrast_anchor:e6",
            "reply_anchor:e6",
            "reply_uci:e7e6",
            "reply_defense_count:3",
            "loss_if_ignored_cp:144",
            "turns_to_impact:1",
            "threat_kind:material",
            "threat_square:d5"
        ),
        causalEvidenceSources = List("explicit_reply_loss", "branch_line", "timing_witness"),
        causalQuestion = Some("WhatMustBeStopped"),
        causalRelations = List("defensive_resource"),
        causalLocalFactGuardrails =
          List(
            "local_fact_guardrail:explicit_reply_loss",
            "local_fact_guardrail:forced_reply_non_unique",
            "local_fact_guardrail:reply_defense_count:3",
            "local_fact_guardrail:surface_forced=false",
            "local_fact_guardrail:surface_line=replayed"
          ),
        supportRows =
          List(
            SupportRow("Central challenge", "The move challenges the center through e7-e6."),
            SupportRow("Checked line", "Short line: e6 h3 Bh5 Qb3 Qc7. Refs: e6 h3 Bh5 Qb3 Qc7.")
          )
      )
    val before =
      base.copy(
        advancedRows =
          List(
            SupportRow("Practical restraint", "The current structure gives a practical brake on the opponent's c-file counterbreak."),
            SupportRow(
              "Engine path",
              "The checked line reaches an exchange sequence after cxd5. The decision is about the resulting structure: a semi-open c-file for White Refs: e6 h3 Bh5 Qb3."
            )
          )
      )
    val after =
      base.copy(
        advancedRows =
          List(
            SupportRow(
              "Engine path",
              "The checked line reaches an exchange sequence after cxd5. The decision is about the resulting structure: a semi-open c-file for White Refs: e6 h3 Bh5 Qb3."
            )
          )
      )

    val (gateRow, gate) =
      gatePair(
        before,
        after,
        defaultCodeCost(added = 10, deleted = 1, reused = List("MoveReviewPlayerPayloadBuilder.currentLocalFactRequiresExactSurface"))
      )
    assert(gateRow.beforeReasons.contains("bounded_non_unique_reply_loss_primary_diluted_by_broad_practical_surface"), clues(gateRow))
    assert(!gateRow.afterReasons.contains("bounded_non_unique_reply_loss_primary_diluted_by_broad_practical_surface"), clues(gateRow))

    assertGatePass(
      gate,
      wrongTierDelta = Some(-1),
      harmfulOverclaimDelta = Some(0),
      genericFallbackDelta = Some(0),
      exactEvidenceSurfaceDelta = Some(0),
      evidenceBoundSurfaceDelta = Some(0)
    )
  }

  test("moveReview regression gate fails when generic fallback replaces exact evidence without quality gain") {
    val sampleId = "g1:prophylaxis:6:moveReview"
    val before =
      moveReviewEntry(
        sampleId = sampleId,
        commentary = "6. Nf3: On the checked line, this keeps the knight tied to the e5 outpost.",
        primaryKind = None,
        selectedQuestion = None,
        selectedOwnerKind = None,
        selectedSource = None,
        fallbackMode = MoveReviewFallbackMode.ExactFactual,
        truthReasonFamily = Some("QuietTechnicalMove"),
        localFact = localFactPayload("pressure", "certified_strategy", "proof_family:outpost", "proof_source:outpost"),
        supportedLocalAdmittedFamilies = List("outpost"),
        causalEvidenceSources = List("typed_local_fact"),
        supportRows = List(SupportRow("Outpost", "On the checked line, this keeps the knight tied to the e5 outpost."))
      )
    val after =
      moveReviewEntry(
        sampleId = sampleId,
        commentary = "6. Nf3: This moves the knight from g1 to f3.",
        primaryKind = None,
        selectedQuestion = None,
        selectedOwnerKind = None,
        selectedSource = None,
        fallbackMode = MoveReviewFallbackMode.ExactFactual,
        truthReasonFamily = Some("QuietTechnicalMove")
      )

    val report = contrastReport(List(before), List(after), defaultCodeCost(added = 12))
    val gate = report.moveReviewGate.getOrElse(fail("missing gate summary"))
    assertEquals(gate.finalStatus, MoveReviewGateStatus.Fail)
    assertEquals(gate.genericFallbackDelta, 1)
    assertEquals(gate.exactEvidenceSurfaceDelta, -1)
    assertEquals(gate.evidenceBoundSurfaceDelta, -1)
    assertEquals(gate.evidenceBoundSurfaceBefore, report.moveReviewGateRows.count(_.beforeEvidenceBoundSurface))
    assertEquals(gate.evidenceBoundSurfaceAfter, report.moveReviewGateRows.count(_.afterEvidenceBoundSurface))
    assert(gate.blockingReasons.contains("exact_evidence_surface_decreased"), clues(gate))
    assert(gate.blockingReasons.contains("evidence_bound_surface_delta_negative"), clues(gate))
    assert(gate.blockingReasons.contains("generic_fallback_increased_without_exact_evidence_gain"), clues(gate))
    assert(gate.blockingReasons.contains("net_code_growth_without_gate_metric_gain"), clues(gate))
  }

  test("moveReview regression gate treats dropped line-consequence primary as wrong family") {
    val sampleId = "g1:prophylaxis:12:moveReview"
    val before =
      moveReviewEntry(
        sampleId = sampleId,
        commentary =
          "6... d5: d5 in this opening context matches a checked center cue. Short line: d5 cxd5 cxd5 O-O Ne4.",
        primaryKind = None,
        selectedQuestion = None,
        selectedOwnerKind = None,
        selectedSource = None,
        fallbackMode = MoveReviewFallbackMode.ExactFactual,
        plannerSceneType = Some("line_consequence"),
        plannerDroppedOwners =
          List(
            "LineConsequence:line_consequence:WhatChanged:PrimaryAllowed+admission_missing_owner_candidate+line_consequence_primary_in_line_scene+primary_admission_without_surviving_plan+pv_line_consequence+scene=line_consequence"
        ),
        truthReasonFamily = Some("QuietTechnicalMove"),
        localFact = producedLocalFactPayload(
            "opening_goal",
            "opening_goal_evidence",
            "opening_goal",
            "typed_local_fact_source:opening_goal",
            "typed_local_fact_family:opening_goal",
            "evidence_line_binding:pv_coupled"
        ),
        causalEvidenceSources = List("branch_line", "typed_local_fact"),
        supportRows = List(SupportRow("Checked line", "Short line: d5 cxd5 cxd5 O-O Ne4."))
      )
    val after =
      moveReviewEntry(
        sampleId = sampleId,
        commentary =
          "6... d5: On the checked line, this times the central break with d5, changing the pawn structure before counterplay settles.",
        primaryKind = Some("WhatChanged"),
        selectedQuestion = Some("WhatChanged"),
        selectedOwnerKind = Some("LineConsequence"),
        selectedSource = Some("line_consequence"),
        plannerSceneType = Some("line_consequence"),
        truthReasonFamily = Some("QuietTechnicalMove"),
        localFact = producedLocalFactPayload(
            "line_consequence",
            "pv_coupled_line",
            "line_consequence",
            "evidence_source:line_consequence_surface",
            "evidence_line_binding:pv_coupled",
            "line_consequence_kind:central_break_timing"
        ),
        causalEvidenceSources = List("branch_line", "line_consequence_surface"),
        supportRows = List(SupportRow("Central break", "On the checked line, this times the central break after d5."))
      )

    val (gateRow, gate) = gatePair(before, after, defaultCodeCost(added = 12))
    assert(gateRow.beforeReasons.contains("line_consequence_primary_dropped_to_other_family"), clues(gateRow))
    assert(!gateRow.afterWrongFamilyRisk, clues(gateRow))

    assertGatePass(gate, wrongFamilyDelta = Some(-1))
  }

  test("moveReview regression gate treats strategy-pack king pressure without concrete attack lane as wrong family") {
    val sampleId = "g1:practical_simplification:20:moveReview"
    val before =
      moveReviewEntry(
        sampleId = sampleId,
        commentary =
          "10... Bb4: Bb4 is tied to checked king pressure around e7. Short line: Bb4 Qh5 Nc6 a3 d5.",
        primaryKind = Some("WhyThis"),
        selectedQuestion = Some("WhyThis"),
        selectedOwnerKind = Some("MoveDelta"),
        selectedSource = Some("practical_position_support"),
        truthReasonFamily = Some("QuietTechnicalMove"),
        localFact = producedLocalFactPayload(
            "pressure",
            "certified_strategy",
            "certified_strategy_delta",
            "typed_local_fact_source:practical_position_support",
            "typed_local_fact_family:pressure",
            "typed_local_fact_producer:certified_strategy_delta",
            "strategic_idea_kind:king_attack_build_up",
            "anchor:e7",
            "strategy_ref:source:king_ring_pressure"
        ),
        causalEvidenceSources = List("branch_line", "typed_local_fact"),
        supportRows = List(SupportRow("Pin pressure", "The checked line pins the pawn on d2 to the king on e1."))
      )
    val after =
      moveReviewEntry(
        sampleId = sampleId,
        commentary =
          "10... Bb4: The checked line pins the pawn on d2 to the king on e1. Short line: Bb4 Qh5 Nc6 a3 d5.",
        primaryKind = Some("WhyThis"),
        selectedQuestion = Some("WhyThis"),
        selectedOwnerKind = Some("MoveDelta"),
        selectedSource = Some("relation_witness"),
        truthReasonFamily = Some("QuietTechnicalMove"),
        localFact = producedLocalFactPayload(
            "attack",
            "pv_coupled_line",
            "relation_witness",
            "typed_local_fact_source:relation_witness",
            "typed_local_fact_family:attack",
            "typed_local_fact_producer:relation_witness",
            "relation_kind:pin",
            "relation_surface:line_geometry",
            "evidence_line_binding:pv_coupled"
        ),
        causalEvidenceSources = List("branch_line", "typed_local_fact"),
        supportRows = List(SupportRow("Pin pressure", "The checked line pins the pawn on d2 to the king on e1."))
      )

    val (gateRow, gate) = gatePair(before, after, defaultCodeCost(added = 12))
    assert(gateRow.beforeReasons.contains("strategy_pack_king_attack_without_concrete_surface"), clues(gateRow))
    assert(!gateRow.afterWrongFamilyRisk, clues(gateRow))

    assertGatePass(gate, wrongFamilyDelta = Some(-1))
    assert(!gate.blockingReasons.contains("net_code_growth_without_gate_metric_gain"), clues(gate))
  }

  test("moveReview regression gate treats canonical pin primary diluted by x-ray support as wrong tier") {
    val sampleId = "2026_03_01_don_kisot_t_clevertacticbtfaill_sample_616:opening_transition:10:moveReview"
    val pinRefs =
      List(
        "evidence_source:typed_local_fact",
        "evidence_subject:line_or_reply",
        "evidence_line_binding:pv_coupled",
        "typed_local_fact_source:canonical_fact",
        "typed_local_fact_family:threat",
        "typed_local_fact_producer:tactical_motif",
        "tactical_kind:pin",
        "motif_owner:current_move",
        "fact_kind:pin",
        "fact_scope:candidate_pv",
        "fact_square:g4",
        "fact_square:f3",
        "fact_square:d1",
        "attacker_role:bishop",
        "pinned_role:knight",
        "behind_role:queen",
        "motif_square:g4",
        "motif_square:f3",
        "motif_square:d1"
      )
    val before =
      moveReviewEntry(
        sampleId = sampleId,
        commentary =
          "5... Bg4: Bg4 pins the f3 knight to the d1 queen. The PV keeps the pin of the f3 knight to the d1 queen local to Bg4: the checked continuation begins with Be2. Short line: Bg4 Be2 Bxf3 Bxf3 Nf6.",
        primaryKind = Some("WhyThis"),
        selectedQuestion = Some("WhyThis"),
        selectedOwnerKind = Some("ConcreteTactical"),
        selectedSource = Some("canonical_fact"),
        plannerSceneType = Some("concrete_tactical"),
        truthReasonFamily = Some("QuietTechnicalMove"),
        localFact = producedLocalFactPayload("threat", "canonical_fact", "tactical_motif", pinRefs),
        causalEvidenceSources = List("branch_line", "typed_local_fact"),
        supportRows =
          List(
            SupportRow("X-ray pressure", "The checked line sets x-ray pressure from the bishop on g4 through f3 toward d1. Refs: Bg4."),
            SupportRow("Checked line", "Short line: Bg4 Be2 Bxf3 Bxf3 Nf6. Refs: Bg4 Be2 Bxf3 Bxf3 Nf6.")
          )
      )
    val after =
      moveReviewEntry(
        sampleId = sampleId,
        commentary = before.commentary,
        primaryKind = Some("WhyThis"),
        selectedQuestion = Some("WhyThis"),
        selectedOwnerKind = Some("ConcreteTactical"),
        selectedSource = Some("canonical_fact"),
        plannerSceneType = Some("concrete_tactical"),
        truthReasonFamily = Some("QuietTechnicalMove"),
        localFact = producedLocalFactPayload("threat", "canonical_fact", "tactical_motif", pinRefs),
        causalEvidenceSources = List("branch_line", "typed_local_fact"),
        supportRows =
          List(
            SupportRow("Checked line", "Short line: Bg4 Be2 Bxf3 Bxf3 Nf6. Refs: Bg4 Be2 Bxf3 Bxf3 Nf6.")
          )
      )

    val (gateRow, gate) = gatePair(before, after, defaultCodeCost(added = 12))
    assert(gateRow.changed, clues(gateRow))
    assert(gateRow.beforeReasons.contains("canonical_pin_primary_diluted_by_xray_support"), clues(gateRow))
    assert(!gateRow.afterWrongTierRisk, clues(gateRow))

    assertGatePass(gate, wrongTierDelta = Some(-1), exactEvidenceSurfaceDelta = Some(0), evidenceBoundSurfaceDelta = Some(0))
    assert(!gate.blockingReasons.contains("net_code_growth_without_gate_metric_gain"), clues(gate))
  }

  test("moveReview regression gate treats relation-witness primary diluted by weaker relation or unresolved author-defense surfaces as wrong tier") {
    val sampleId = "2026_03_01_dragorz_magnuscr44_sample_261:practical_simplification:20:moveReview"
    val relationRefs =
      List(
        "evidence_source:typed_local_fact",
        "evidence_subject:line_or_reply",
        "evidence_line_binding:pv_coupled",
        "typed_local_fact_source:relation_witness",
        "typed_local_fact_family:threat",
        "typed_local_fact_authority:pv_coupled_line",
        "typed_local_fact_producer:relation_witness",
        "relation_kind:overload",
        "relation_source:overload_relation",
        "relation_observation:overload_semantic",
        "overload_relation_witness",
        "relation_fact:defender:f3",
        "relation_fact:duties:d2|e1",
        "relation_fact:attacker:a5",
        "line_move:d8a5"
      )
    val baseCommentary =
      "10... Qa5+: The checked line overloads the defender on f3 across d2 and e1. The PV keeps the overload relation local to Qa5+: the checked continuation begins with Bd2. Short line: Qa5+ Bd2 Qd8 Qa4 Bd7."
    def row(supportRows: List[SupportRow], advancedRows: List[SupportRow]) =
      moveReviewEntry(
        sampleId = sampleId,
        commentary = baseCommentary,
        primaryKind = Some("WhyThis"),
        selectedQuestion = Some("WhyThis"),
        selectedOwnerKind = Some("ConcreteTactical"),
        selectedSource = Some("relation_witness"),
        fen = "rnbqk1nr/pp2ppbp/6p1/1N1P4/5B2/5N2/PP3PPP/R2QKB1R b KQkq - 4 10",
        playedSan = "Qa5+",
        playedUci = "d8a5",
        plannerSceneType = Some("concrete_tactical"),
        truthReasonFamily = Some("QuietTechnicalMove"),
        localFact = producedLocalFactPayload("threat", "pv_coupled_line", "relation_witness", relationRefs),
        causalEvidenceSources = List("branch_line", "relation_witness"),
        causalRelations = List("overload"),
        causalLocalFactGuardrails = List("local_fact_guardrail:relation_witness_typed_details"),
        supportRows = supportRows,
        advancedRows = advancedRows
      )
    val before =
      row(
        supportRows =
          List(
            SupportRow("X-ray pressure", "The checked line sets x-ray pressure from the queen on a5 through a2 toward a1. Refs: Qa5+."),
            SupportRow("Checked line", "Short line: Qa5+ Bd2 Qd8 Qa4 Bd7. Refs: Qa5+ Bd2 Qd8 Qa4 Bd7.")
          ),
        advancedRows =
          List(
            SupportRow("Line relation", "The line relation is pin geometry from a5 through a2 toward a1. Target: a2."),
            SupportRow("Practical line", "The queen already has a practical c-file post.")
          )
      )
    val after =
      row(
        supportRows =
          List(
            SupportRow("Checked line", "Short line: Qa5+ Bd2 Qd8 Qa4 Bd7. Refs: Qa5+ Bd2 Qd8 Qa4 Bd7.")
          ),
        advancedRows = Nil
      )

    val (gateRow, gate) = gatePair(before, after, defaultCodeCost(added = 12))
    assert(gateRow.changed, clues(gateRow))
    assert(gateRow.beforeReasons.contains("relation_witness_primary_diluted_by_weaker_relation_surface"), clues(gateRow))
    assert(!gateRow.afterWrongTierRisk, clues(gateRow))

    assertGatePass(gate, wrongTierDelta = Some(-1), exactEvidenceSurfaceDelta = Some(0), evidenceBoundSurfaceDelta = Some(0))
    assert(!gate.blockingReasons.contains("net_code_growth_without_gate_metric_gain"), clues(gate))

    val authorPromptBefore =
      row(
        supportRows =
          List(
            SupportRow("Checked line", "Short line: Qa5+ Bd2 Qd8 Qa4 Bd7. Refs: Qa5+ Bd2 Qd8 Qa4 Bd7.")
          ),
        advancedRows =
          List(
            SupportRow(
              "What Must Be Stopped",
              "What is the defensive task here — can Black meet the threat with Qa5+?; There is immediate pressure: if ignored, king safety can become critical on the next move."
            )
          )
      )
    val (authorPromptGateRow, _) =
      gatePair(authorPromptBefore, after, defaultCodeCost(added = 8), clue = "expected author prompt contrast report")
    assert(authorPromptGateRow.beforeReasons.contains("relation_witness_primary_diluted_by_unresolved_author_defense_prompt"), clues(authorPromptGateRow))
    assert(!authorPromptGateRow.afterWrongTierRisk, clues(authorPromptGateRow))
  }

  test("moveReview regression gate treats line-occupation primary diluted by weaker relation or practical surfaces as wrong tier") {
    val sampleId = "2026_03_01_classictraining2024_oden9_sample_52:endgame_conversion:50:moveReview"
    val lineOccupationRefs =
      List(
        "evidence_source:typed_local_fact",
        "evidence_subject:played_move",
        "evidence_line_binding:pv_coupled",
        "typed_local_fact_source:practical_position_support",
        "typed_local_fact_family:pressure",
        "typed_local_fact_authority:certified_strategy",
        "typed_local_fact_producer:certified_strategy_delta",
        "strategic_idea_kind:line_occupation",
        "strategic_readiness:ready",
        "anchor:d5",
        "strategy_ref:source:route_line_access",
        "strategy_ref:open_file_d",
        "strategy_ref:source:open_file_control",
        "strategy_ref:source:directional_line_access",
        "strategy_ref:directional_line_access_shape",
        "strategy_ref:source:line_control_features",
        "strategy_ref:line_control_shape",
        "focus_square:d5",
        "focus_file:d",
        "line_occupation_file:d",
        "line_occupation_status:open"
      )
    val baseCommentary =
      "25... Qd5: Qd5 puts the queen on the open d-file. The PV keeps the open d-file occupation local to Qd5: the checked continuation begins with Qe7. Short line: Qd5 Qe7 Qd4 Qb7 Re8."
    def row(advancedRows: List[SupportRow]) =
      moveReviewEntry(
        sampleId = sampleId,
        commentary = baseCommentary,
        primaryKind = Some("WhyThis"),
        selectedQuestion = Some("WhyThis"),
        selectedOwnerKind = Some("MoveDelta"),
        selectedSource = Some("practical_position_support"),
        fen = "2r3k1/p4p1p/2p3p1/2q5/4Q3/1P4P1/P4P1P/5BK1 b - - 0 25",
        playedSan = "Qd5",
        playedUci = "c5d5",
        plannerSceneType = Some("quiet_improvement"),
        truthReasonFamily = Some("QuietTechnicalMove"),
        localFact = producedLocalFactPayload("pressure", "certified_strategy", "certified_strategy_delta", lineOccupationRefs),
        causalEvidenceSources = List("branch_line", "typed_local_fact"),
        causalRelations = List("played_move_consequence"),
        supportRows =
          List(
            SupportRow("File entry", "The checked line places the queen on the open d-file. Refs: Qd5."),
            SupportRow("Checked line", "Short line: Qd5 Qe7 Qd4 Qb7 Re8. Refs: Qd5 Qe7 Qd4 Qb7 Re8.")
          ),
        advancedRows = advancedRows
      )
    val before =
      row(
        List(
          SupportRow("Tactical relation", "overload pressure on e4 across d3 and d4. Target: d3."),
          SupportRow("Practical line", "The open d-file gives a practical major-piece line cue.")
        )
      )
    val after = row(Nil)

    val (gateRow, gate) = gatePair(before, after, defaultCodeCost(added = 12))
    assert(gateRow.changed, clues(gateRow))
    assert(gateRow.beforeReasons.contains("line_occupation_primary_diluted_by_weaker_relation_or_practical_surface"), clues(gateRow))
    assert(!gateRow.afterWrongTierRisk, clues(gateRow))

    assertGatePass(gate, wrongTierDelta = Some(-1), exactEvidenceSurfaceDelta = Some(0), evidenceBoundSurfaceDelta = Some(0))
    assert(!gate.blockingReasons.contains("net_code_growth_without_gate_metric_gain"), clues(gate))
  }

  test("moveReview regression gate treats line-consequence primary diluted by practical line as wrong tier") {
    val sampleId = "2026_03_01_cb_sai_1_tamogna_sample_29:long_structural_squeeze:35:moveReview"
    val lineConsequenceRefs =
      List(
        "evidence_source:line_consequence_surface",
        "evidence_subject:played_move",
        "evidence_line_binding:pv_coupled",
        "line_consequence_kind:immediate_opponent_pawn_capture",
        "line_consequence_release:surface_candidate",
        "line_consequence_line_id:line_02",
        "line_consequence_trigger_san:Nxe4"
      )
    val commentary =
      "18. Be1: On the checked line 18. Be1 Nxe4 19. Qc4 Nc5 20. Ra1 Re8, Black answers with Nxe4, immediately taking the pawn on e4. One checked line is a) Be1 Nxe4 Qc4 Nc5 Ra1."
    def row(advancedRows: List[SupportRow]) =
      moveReviewEntry(
        sampleId = sampleId,
        commentary = commentary,
        primaryKind = Some("WhatChanged"),
        selectedQuestion = Some("WhatChanged"),
        selectedOwnerKind = Some("MoveDelta"),
        selectedSource = Some("pv_coupled_plan_support"),
        fen = "3q1rk1/2p2ppp/1p1p1n2/B3p3/4P3/5N2/2P2PPP/1R3QK1 w - - 0 18",
        playedSan = "Be1",
        playedUci = "a5e1",
        plannerSceneType = Some("quiet_improvement"),
        truthClass = Some("Acceptable"),
        truthReasonFamily = Some("QuietTechnicalMove"),
        truthFailureMode = Some("NoClearPlan"),
        localFact = producedLocalFactPayload("line_consequence", "pv_coupled_line", "line_consequence", lineConsequenceRefs),
        causalEvidenceSources = List("branch_line", "line_consequence_surface", "pv_coupled_plan_support"),
        causalQuestion = Some("WhatChanged"),
        causalRelations = List("played_move_consequence", "change_consequence"),
        supportRows =
          List(
            SupportRow("Checked line", "Short line: Be1 Nxe4 Qc4 Nc5 Ra1. Refs: Be1 Nxe4 Qc4 Nc5 Ra1.")
          ),
        advancedRows = advancedRows
      )
    val before =
      row(
        List(
          SupportRow("Practical line", "The rook already has a practical a-file post."),
          SupportRow("Engine path", "The checked line meets the move with an immediate pawn capture after Nxe4.")
        )
      )
    val after =
      row(
        List(
          SupportRow("Engine path", "The checked line meets the move with an immediate pawn capture after Nxe4.")
        )
      )

    val (gateRow, gate) = gatePair(before, after, defaultCodeCost(added = 12))
    assert(gateRow.changed, clues(gateRow))
    assert(gateRow.beforeReasons.contains("line_consequence_primary_diluted_by_broad_practical_surface"), clues(gateRow))
    assert(!gateRow.afterWrongTierRisk, clues(gateRow))

    assertGatePass(gate, wrongTierDelta = Some(-1), exactEvidenceSurfaceDelta = Some(0), evidenceBoundSurfaceDelta = Some(0))
  }

  test("moveReview regression gate treats line-consequence primary diluted by tactical relation as wrong tier") {
    val sampleId = "2026_03_01_ferbil_kasavoeboe_sample_620:practical_simplification:20:moveReview"
    val lineConsequenceRefs =
      List(
        "evidence_source:line_consequence_surface",
        "evidence_subject:played_move",
        "evidence_line_binding:pv_coupled",
        "line_consequence_kind:exchange_sequence",
        "line_consequence_release:surface_candidate",
        "line_consequence_line_id:line_01",
        "line_consequence_trigger_san:Nxc3"
      )
    val commentary =
      "10... Nd5: On the checked line 10... Nd5 11. O-O Nxc3 12. Bd3 Nxd1, this exchange sequence starts when Nxc3 captures the pawn on c3, settling which pieces and pawns remain."
    def row(advancedRows: List[SupportRow]) =
      moveReviewEntry(
        sampleId = sampleId,
        commentary = commentary,
        primaryKind = Some("WhatChanged"),
        selectedQuestion = Some("WhatChanged"),
        selectedOwnerKind = Some("LineConsequence"),
        selectedSource = Some("line_consequence"),
        fen = "r1b2rk1/pppp1ppp/2n2n2/6B1/4q3/2P2N1P/P1P1BPP1/R2QK2R b KQ - 1 10",
        playedSan = "Nd5",
        playedUci = "f6d5",
        plannerSceneType = Some("line_consequence"),
        truthClass = Some("Acceptable"),
        truthReasonFamily = Some("QuietTechnicalMove"),
        truthFailureMode = Some("NoClearPlan"),
        localFact = producedLocalFactPayload("line_consequence", "pv_coupled_line", "line_consequence", lineConsequenceRefs),
        causalEvidenceSources = List("branch_line", "line_consequence_surface"),
        causalQuestion = Some("WhatChanged"),
        causalRelations = List("played_move_consequence", "change_consequence"),
        supportRows =
          List(
            SupportRow("Checked line", "Short line: Nd5 O-O Nxc3 Bd3 Nxd1. Refs: Nd5 O-O Nxc3 Bd3 Nxd1.")
          ),
        advancedRows = advancedRows
      )
    val before =
      row(
        List(
          SupportRow("Tactical relation", "The tactical relation is hanging piece pressure from d5 on c3. Target: c3."),
          SupportRow("Engine path", "The checked line reaches an exchange sequence after Nxc3.")
        )
      )
    val after =
      row(
        List(
          SupportRow("Engine path", "The checked line reaches an exchange sequence after Nxc3.")
        )
      )

    val (gateRow, gate) = gatePair(before, after, defaultCodeCost(added = 12))
    assert(gateRow.changed, clues(gateRow))
    assert(gateRow.beforeReasons.contains("line_consequence_primary_diluted_by_weaker_relation_surface"), clues(gateRow))
    assert(!gateRow.afterWrongTierRisk, clues(gateRow))

    assertGatePass(gate, wrongTierDelta = Some(-1), exactEvidenceSurfaceDelta = Some(0), evidenceBoundSurfaceDelta = Some(0))
  }

  test("moveReview regression gate treats alternative-comparison primary diluted by practical space as wrong tier") {
    val sampleId = "2026_03_01_a1234abdoo01234_epicalychess_sample_158:endgame_conversion:35:moveReview"
    val alternativeComparisonRefs =
      List(
        "evidence_source:role_aware_line_consequence",
        "evidence_subject:played_move",
        "evidence_line_binding:pv_coupled",
        "comparison_source:role_aware_line_consequence",
        "branch_role:engine_best",
        "branch_role:played",
        "engine_best_move:Ne3",
        "played_move:c3",
        "compared_move:c3",
        "cp_loss:55",
        "engine_best:line_consequence_kind:capture_structure_transition",
        "engine_best:line_consequence_release:replay_backed_internal",
        "played:line_consequence_kind:preview_only",
        "played:line_consequence_release:replay_backed_internal"
      )
    val commentary =
      "18. c3: Ne3 reaches a capture leaving Black with an isolated pawn on h4 on the engine-best branch; c3 stays on the played branch without that concrete capture."
    def row(advancedRows: List[SupportRow]) =
      moveReviewEntry(
        sampleId = sampleId,
        commentary = commentary,
        primaryKind = Some("WhatChanged"),
        selectedQuestion = Some("WhatChanged"),
        selectedOwnerKind = Some("AlternativeComparison"),
        selectedSource = Some("decision_comparison"),
        fen = "7r/pppr3p/2n1kp2/3Np3/4P3/P4P2/1PP3PP/2R1K2R w K - 0 18",
        playedSan = "c3",
        playedUci = "c2c3",
        plannerSceneType = Some("alternative_comparison"),
        truthClass = Some("Inaccuracy"),
        truthReasonFamily = Some("QuietTechnicalMove"),
        truthFailureMode = Some("NoClearPlan"),
        localFact = producedLocalFactPayload("line_consequence", "alternative_comparison", "alternative_comparison", alternativeComparisonRefs),
        causalEvidenceSources = List("role_aware_line_consequence"),
        causalQuestion = Some("WhatChanged"),
        causalRelations = List("alternative_contrast"),
        supportRows =
          List(
            SupportRow("Checked line", "Short line: c3 Na5 Ke2 Nc4 Rc2. Refs: c3 Na5 Ke2 Nc4 Rc2."),
            SupportRow("Decision point", "played c3, engine looked at Ne3, compared c3, gap 55cp slight. Refs: Ne3 Rhd8 Rf1 h5 c3 Na5.")
          ),
        advancedRows = advancedRows
      )
    val before =
      row(
        List(
          SupportRow("Practical space", "The pawn chain gives a practical flank-space cue."),
          SupportRow("Engine path", "The checked line continues c3 Na5 Ke2 Nc4.")
        )
      )
    val after =
      row(
        List(
          SupportRow("Engine path", "The checked line continues c3 Na5 Ke2 Nc4.")
        )
      )

    val (gateRow, gate) = gatePair(before, after, defaultCodeCost(added = 12))
    assert(gateRow.changed, clues(gateRow))
    assert(gateRow.beforeReasons.contains("alternative_comparison_primary_diluted_by_broad_practical_surface"), clues(gateRow))
    assert(!gateRow.afterWrongTierRisk, clues(gateRow))

    assertGatePass(gate, wrongTierDelta = Some(-1), exactEvidenceSurfaceDelta = Some(0), evidenceBoundSurfaceDelta = Some(0))
  }

  test("moveReview regression gate treats fork-entry defense primary diluted by broad practical surface as wrong tier") {
    val sampleId = "2026_03_01_bicibici3_starfihs_sample_734:strategic_choice:18:moveReview"
    val forkEntryRefs =
      List(
        "evidence_source:typed_local_fact",
        "evidence_subject:line_or_reply",
        "evidence_line_binding:pv_coupled",
        "typed_local_fact_source:canonical_fact",
        "typed_local_fact_family:defense",
        "typed_local_fact_authority:canonical_fact",
        "typed_local_fact_producer:fork_entry_defense",
        "fact_source:post_move_fork_entry_defense",
        "fork_entry_square:c7",
        "fork_attacker:b5",
        "fork_defender_square:a6",
        "fork_target:e8:king",
        "fork_target:a8:rook"
      )
    val baseCommentary =
      "9... Na6: Na6 covers c7 against the knight fork from b5, where the knight would hit king on e8 and rook on a8. The PV keeps the defensive detail bounded: Na6 is the move under review, the checked continuation begins with Be3."
    def row(advancedRows: List[SupportRow]) =
      moveReviewEntry(
        sampleId = sampleId,
        commentary = baseCommentary,
        primaryKind = Some("WhyThis"),
        selectedQuestion = Some("WhyThis"),
        selectedOwnerKind = Some("ForcingDefense"),
        selectedSource = Some("canonical_fact"),
        fen = "rnb1kb1r/pp3p1p/4pp2/1N6/8/8/PPP2PPP/R1B1KB1R b KQkq - 1 9",
        playedSan = "Na6",
        playedUci = "b8a6",
        plannerSceneType = Some("forcing_defense"),
        truthClass = Some("Inaccuracy"),
        truthReasonFamily = Some("QuietTechnicalMove"),
        truthBenchmarkCriticalMove = Some(false),
        localFact = producedLocalFactPayload("defense", "canonical_fact", "fork_entry_defense", forkEntryRefs),
        causalEvidenceSources = List("typed_local_fact"),
        causalRelations = List("played_move_consequence"),
        causalLocalFactGuardrails =
          List(
            "local_fact_guardrail:fork_entry_square_defended_by_played_move",
            "local_fact_guardrail:post_move_static_fork_entry_defense",
            "local_fact_guardrail:hypothetical_knight_fork_targets_king_and_major"
          ),
        supportRows =
          List(
            SupportRow("Checked line", "Short line: Na6 Be3 Bd7 O-O-O f5. Refs: Na6 Be3 Bd7 O-O-O f5.")
          ),
        advancedRows = advancedRows
      )
    val before =
      row(
        List(
          SupportRow("Practical line", "The semi-open g-file gives a practical major-piece line cue.")
        )
      )
    val after = row(Nil)

    val (gateRow, gate) = gatePair(before, after, defaultCodeCost(added = 12))
    assert(gateRow.changed, clues(gateRow))
    assert(gateRow.beforeReasons.contains("fork_entry_defense_primary_diluted_by_broad_practical_surface"), clues(gateRow))
    assert(!gateRow.afterWrongTierRisk, clues(gateRow))

    assertGatePass(gate, wrongTierDelta = Some(-1), exactEvidenceSurfaceDelta = Some(0), evidenceBoundSurfaceDelta = Some(0))
    assert(!gate.blockingReasons.contains("net_code_growth_without_gate_metric_gain"), clues(gate))
  }

  test("moveReview regression gate treats broad practical advanced row under exact tactical primary as wrong tier") {
    val sampleId = "2026_03_01_don_kisot_t_clevertacticbtfaill_sample_616:opening_transition:10:moveReview"
    val pinRefs =
      List(
        "evidence_source:typed_local_fact",
        "evidence_line_binding:pv_coupled",
        "typed_local_fact_source:canonical_fact",
        "typed_local_fact_family:threat",
        "typed_local_fact_producer:tactical_motif",
        "tactical_kind:pin",
        "motif_owner:current_move",
        "behind_role:queen"
      )
    val baseCommentary =
      "5... Bg4: Bg4 pins the f3 knight to the d1 queen. The PV keeps the pin of the f3 knight to the d1 queen local to Bg4: the checked continuation begins with Be2. Short line: Bg4 Be2 Bxf3 Bxf3 Nf6."
    def row(advancedRows: List[SupportRow]) =
      moveReviewEntry(
        sampleId = sampleId,
        commentary = baseCommentary,
        primaryKind = Some("WhyThis"),
        selectedQuestion = Some("WhyThis"),
        selectedOwnerKind = Some("ConcreteTactical"),
        selectedSource = Some("canonical_fact"),
        plannerSceneType = Some("concrete_tactical"),
        truthReasonFamily = Some("QuietTechnicalMove"),
        localFact = producedLocalFactPayload("threat", "canonical_fact", "tactical_motif", pinRefs),
        causalEvidenceSources = List("branch_line", "typed_local_fact"),
        supportRows =
          List(
            SupportRow("Checked line", "Short line: Bg4 Be2 Bxf3 Bxf3 Nf6. Refs: Bg4 Be2 Bxf3 Bxf3 Nf6.")
          ),
        advancedRows = advancedRows
      )
    val before =
      row(List(SupportRow("Practical restraint", "The current structure gives a practical brake on the opponent's g-file break.")))
    val after = row(Nil)

    val (gateRow, gate) = gatePair(before, after, defaultCodeCost(added = 10))
    assert(gateRow.changed, clues(gateRow))
    assert(gateRow.beforeReasons.contains("canonical_tactical_primary_diluted_by_broad_practical_advanced"), clues(gateRow))
    assert(!gateRow.afterWrongTierRisk, clues(gateRow))

    assertGatePass(gate, wrongTierDelta = Some(-1), exactEvidenceSurfaceDelta = Some(0), evidenceBoundSurfaceDelta = Some(0))
  }

  test("moveReview regression gate treats broad practical advanced row under exact target-pressure primary as wrong tier") {
    val sampleId = "2026_03_01_deflauzano_ly3257_sample_237:long_structural_squeeze:26:moveReview"
    val targetPressureRefs =
      List(
        "evidence_source:typed_local_fact",
        "evidence_line_binding:pv_coupled",
        "typed_local_fact_source:canonical_fact",
        "typed_local_fact_family:pressure",
        "typed_local_fact_producer:target_pressure",
        "fact_kind:target_piece",
        "fact_scope:now",
        "fact_square:d3",
        "fact_square:c4",
        "fact_role:bishop",
        "fact_source:post_move_static"
      )
    val targetPressureGuardrails =
      List(
        "local_fact_guardrail:target_fact_attacked_by_played_move",
        "local_fact_guardrail:pv_coupled",
        "local_fact_guardrail:post_move_static_target"
      )
    val baseCommentary =
      "13... c4: c4 attacks the bishop on d3. The PV keeps the pressure on the d3 bishop local to c4: Bc2 moves that bishop from d3. Short line: c4 Bc2 Bc5 Kh1 f5."
    def row(advancedRows: List[SupportRow]) =
      moveReviewEntry(
        sampleId = sampleId,
        commentary = baseCommentary,
        primaryKind = Some("WhyThis"),
        selectedQuestion = Some("WhyThis"),
        selectedOwnerKind = Some("MoveDelta"),
        selectedSource = Some("canonical_fact"),
        plannerSceneType = Some("quiet_improvement"),
        truthReasonFamily = Some("QuietTechnicalMove"),
        localFact = producedLocalFactPayload("pressure", "canonical_fact", "target_pressure", targetPressureRefs),
        causalEvidenceSources = List("branch_line", "typed_local_fact"),
        causalLocalFactGuardrails = targetPressureGuardrails,
        supportRows =
          List(
            SupportRow("Checked line", "Short line: c4 Bc2 Bc5 Kh1 f5. Refs: c4 Bc2 Bc5 Kh1 f5.")
          ),
        advancedRows = advancedRows
      )
    val before =
      row(
        List(
          SupportRow("Practical break", "The French Advance chain gives Black a practical ...f6 break cue."),
          SupportRow("Practical restraint", "The current structure gives a practical brake on the opponent's a-file break.")
        )
      )
    val after = row(Nil)

    val (gateRow, gate) = gatePair(before, after, defaultCodeCost(added = 10))
    assert(gateRow.changed, clues(gateRow))
    assert(gateRow.beforeReasons.contains("canonical_target_pressure_primary_diluted_by_broad_practical_advanced"), clues(gateRow))
    assert(!gateRow.afterWrongTierRisk, clues(gateRow))

    assertGatePass(gate, wrongTierDelta = Some(-1), exactEvidenceSurfaceDelta = Some(0), evidenceBoundSurfaceDelta = Some(0))
  }

  test("moveReview regression gate treats mover-self discovered attack support as wrong family") {
    val sampleId = "2026_03_01_haru808_eliminagrupos_sample_266:long_structural_squeeze:38:moveReview"
    val lineConsequenceRefs =
      List(
        "evidence_source:line_consequence_surface",
        "evidence_subject:played_move",
        "evidence_line_binding:pv_coupled",
        "line_consequence_kind:exchange_sequence",
        "line_consequence_release:surface_candidate",
        "line_consequence_uci:g4d7",
        "line_consequence_uci:g2e4",
        "line_consequence_uci:e8e4",
        "line_consequence_uci:d3e4",
        "line_consequence_structure_kind:backward_pawn",
        "line_consequence_structure_square:e4"
      )
    val commentary =
      "19... Bd7: On the checked line 19... Bd7 20. Be4 Rxe4 21. dxe4 f6 22. Kh2 Qxb2, this exchange sequence trades the rook for the bishop on e4, leaving White with a backward pawn target on e4. One checked line is a) Bd7 Be4 Rxe4 dxe4 f6."
    val before =
      moveReviewEntry(
        sampleId = sampleId,
        commentary = commentary,
        primaryKind = Some("WhatChanged"),
        selectedQuestion = Some("WhatChanged"),
        selectedOwnerKind = Some("LineConsequence"),
        selectedSource = Some("line_consequence"),
        fen = "4rrk1/ppp2ppp/1b1p4/1q1P4/6b1/2PP3P/PP1B2B1/R6K b - - 0 19",
        playedSan = "Bd7",
        playedUci = "g4d7",
        plannerSceneType = Some("line_consequence"),
        truthReasonFamily = Some("QuietTechnicalMove"),
        localFact = producedLocalFactPayload("line_consequence", "pv_coupled_line", "line_consequence", lineConsequenceRefs),
        causalEvidenceSources = List("branch_line", "line_consequence_surface"),
        supportRows =
          List(
            SupportRow("Discovered attack", "The checked line clears g4, revealing a bishop attack from d7 on h3. Refs: Bd7."),
            SupportRow("Checked line", "Short line: Bd7 Be4 Rxe4 dxe4 f6. Refs: Bd7 Be4 Rxe4 dxe4 f6.")
          )
      )
    val after =
      moveReviewEntry(
        sampleId = sampleId,
        commentary = commentary,
        primaryKind = Some("WhatChanged"),
        selectedQuestion = Some("WhatChanged"),
        selectedOwnerKind = Some("LineConsequence"),
        selectedSource = Some("line_consequence"),
        fen = "4rrk1/ppp2ppp/1b1p4/1q1P4/6b1/2PP3P/PP1B2B1/R6K b - - 0 19",
        playedSan = "Bd7",
        playedUci = "g4d7",
        plannerSceneType = Some("line_consequence"),
        truthReasonFamily = Some("QuietTechnicalMove"),
        localFact = producedLocalFactPayload("line_consequence", "pv_coupled_line", "line_consequence", lineConsequenceRefs),
        causalEvidenceSources = List("branch_line", "line_consequence_surface"),
        supportRows =
          List(
            SupportRow("Checked line", "Short line: Bd7 Be4 Rxe4 dxe4 f6. Refs: Bd7 Be4 Rxe4 dxe4 f6.")
          )
      )

    val (gateRow, gate) = gatePair(before, after, defaultCodeCost(added = 12))
    assert(gateRow.changed, clues(gateRow))
    assert(gateRow.beforeReasons.contains("line_consequence_primary_diluted_by_mover_self_discovered_attack"), clues(gateRow))
    assert(!gateRow.afterWrongFamilyRisk, clues(gateRow))

    assertGatePass(gate, wrongFamilyDelta = Some(-1), exactEvidenceSurfaceDelta = Some(0), evidenceBoundSurfaceDelta = Some(0))
  }

  test("moveReview regression gate treats quiet pawn-recovery demoted to discovered-attack family as wrong family") {
    val sampleId = "2026_03_01_econpower_leonkiller77_sample_959:prophylaxis:11:moveReview"
    val before =
      moveReviewEntry(
        sampleId = sampleId,
        commentary =
          "6. e3: e3 opens a discovered attack from the f1 bishop toward the c4 pawn. The PV keeps the discovered attack from the f1 bishop toward the c4 pawn local to e3: the checked continuation begins with e6. Short line: e3 e6 Bxc4 Bb4 O-O.",
        primaryKind = Some("WhyThis"),
        selectedQuestion = Some("WhyThis"),
        selectedOwnerKind = Some("ConcreteTactical"),
        selectedSource = Some("canonical_fact"),
        fen = "rn1qkb1r/pp2pppp/2p2n2/5b2/P1pP4/2N2N2/1P2PPPP/R1BQKB1R w KQkq - 1 6",
        playedSan = "e3",
        playedUci = "e2e3",
        plannerSceneType = Some("concrete_tactical"),
        truthClass = Some("Acceptable"),
        truthReasonFamily = Some("QuietTechnicalMove"),
        truthFailureMode = Some("NoClearPlan"),
        localFact =
          producedLocalFactPayload(
              "threat",
              "canonical_fact",
              "tactical_motif",
              "typed_local_fact_source:canonical_fact",
              "typed_local_fact_family:threat",
              "typed_local_fact_authority:canonical_fact",
                "typed_local_fact_producer:tactical_motif",
              "tactical_kind:discovered_attack",
              "motif_owner:current_move",
              "motif_piece_square:f1",
              "motif_target_square:c4"
          ),
        causalEvidenceSources = List("branch_line", "typed_local_fact"),
        causalQuestion = Some("WhyThis"),
        causalRelations = List("played_move_consequence"),
        supportRows =
          List(
            SupportRow("Checked line", "Short line: e3 e6 Bxc4 Bb4 O-O. Refs: e3 e6 Bxc4 Bb4 O-O.")
          )
      )
    val after =
      moveReviewEntry(
        sampleId = sampleId,
        commentary =
          "6. e3: On the checked line 6. e3 e6 7. Bxc4 Bb4 8. O-O O-O 9. Qb3, the line reaches Bxc4, so the pawn capture arrives after the first move. One checked line is a) e3 e6 Bxc4 Bb4 O-O.",
        primaryKind = Some("WhatChanged"),
        selectedQuestion = Some("WhatChanged"),
        selectedOwnerKind = Some("LineConsequence"),
        selectedSource = Some("line_consequence"),
        fen = "rn1qkb1r/pp2pppp/2p2n2/5b2/P1pP4/2N2N2/1P2PPPP/R1BQKB1R w KQkq - 1 6",
        playedSan = "e3",
        playedUci = "e2e3",
        plannerSceneType = Some("line_consequence"),
        truthClass = Some("Acceptable"),
        truthReasonFamily = Some("QuietTechnicalMove"),
        truthFailureMode = Some("NoClearPlan"),
        localFact =
          producedLocalFactPayload(
              "line_consequence",
              "pv_coupled_line",
              "line_consequence",
              "evidence_source:line_consequence_surface",
              "evidence_subject:played_move",
              "evidence_line_binding:pv_coupled",
              "line_consequence_kind:delayed_pawn_capture",
              "line_consequence_release:surface_candidate",
              "line_consequence_line_id:line_03",
              "line_consequence_trigger_san:Bxc4"
          ),
        causalEvidenceSources = List("branch_line", "line_consequence_surface", "typed_local_fact"),
        causalQuestion = Some("WhatChanged"),
        causalRelations = List("played_move_consequence", "change_consequence"),
        supportRows =
          List(
            SupportRow("Checked line", "One checked line is a) e3 e6 Bxc4 Bb4 O-O. Refs: e3 e6 Bxc4 Bb4 O-O.")
          )
      )

    val (gateRow, gate) = gatePair(before, after, defaultCodeCost(added = 18, deleted = 4, helperCount = 1))
    assert(gateRow.changed, clues(gateRow))
    assert(gateRow.beforeReasons.contains("quiet_pawn_recovery_demoted_to_discovered_attack_family"), clues(gateRow))
    assert(!gateRow.afterWrongFamilyRisk, clues(gateRow))

    assertGatePass(gate, wrongFamilyDelta = Some(-1), exactEvidenceSurfaceDelta = Some(0), evidenceBoundSurfaceDelta = Some(0))
  }

  test("moveReview regression gate treats plan-support surface diluted by transient passed-pawn line consequence as wrong family") {
    val sampleId = "g1:opening_transition:19:moveReview"
    val before =
      moveReviewEntry(
        sampleId = sampleId,
        commentary =
          "10. Nb5: The checked line from Nb5 keeps Development and central control connected to d6 as a practical plan. On the checked line 10. Nb5 Na6 11. Bc4 Nf6 12. d6 O-O 13. O-O exd6, the pawn reaches d6 as a passed pawn, giving the line a concrete passer cue.",
        primaryKind = Some("WhatChanged"),
        selectedQuestion = Some("WhatChanged"),
        selectedOwnerKind = Some("MoveDelta"),
        selectedSource = Some("pv_coupled_plan_support"),
        playedSan = "Nb5",
        playedUci = "c3b5",
        plannerSceneType = Some("opening_relation"),
        truthClass = Some("Acceptable"),
        truthReasonFamily = Some("QuietTechnicalMove"),
        localFact =
          producedLocalFactPayload(
              "plan_support",
              "pv_coupled_line",
              "certified_strategy_delta",
              "evidence_source:pv_coupled_plan_support",
              "evidence_line_binding:pv_coupled",
              "plan_anchor_matched:true",
              "branch_line_first_san:Nb5"
          ),
        causalEvidenceSources = List("branch_line", "pv_coupled_plan_support"),
        supportRows =
          List(
            SupportRow(
              "Engine path",
              "The checked line creates a passed pawn after exd6. That matters because the line leaves a concrete passed-pawn cue to track"
            )
          )
      )
    val after =
      before.copy(
        commentary =
          "10. Nb5: The checked line from Nb5 keeps Development and central control connected to d6 as a practical plan.",
        supportRows = List(SupportRow("Checked line", "Short line: Nb5 Na6 Bc4 Nf6 d6."))
      )

    val (gateRow, gate) =
      gatePair(before, after, defaultCodeCost(added = 8, deleted = 2, reused = List("LineConsequenceEvaluator")))
    assert(gateRow.beforeReasons.contains("plan_support_surface_diluted_by_passed_pawn_line_consequence"), clues(gateRow))
    assert(!gateRow.afterReasons.contains("plan_support_surface_diluted_by_passed_pawn_line_consequence"), clues(gateRow))

    assertGatePass(gate, wrongFamilyDelta = Some(-1), exactEvidenceSurfaceDelta = Some(0), evidenceBoundSurfaceDelta = Some(0))
  }

  test("moveReview regression gate treats pv-coupled plan-support primary diluted by practical line as wrong tier") {
    val sampleId = "2026_03_01_chesscleff842_kingfritz_sample_377:long_structural_squeeze:30:moveReview"
    val localFactEvidenceRefs =
      List(
        "evidence_source:pv_coupled_plan_support",
        "evidence_subject:played_move",
        "evidence_line_binding:pv_coupled",
        "plan_name:Prophylaxis against counterplay",
        "played_san:Be6",
        "branch_line_meaningful:true",
        "plan_anchor_matched:true",
        "branch_line_first_san:Be6",
        "plan_anchor_line:Further probe work still targets Prophylaxis against counterplay through Rae8 an",
        "plan_anchor_token:Rae8",
        "plan_anchor_token:Rfe8",
        "plan_anchor_matched_token:Rfe8"
      )
    val before =
      moveReviewEntry(
        sampleId = sampleId,
        commentary =
          "15... Be6: The checked line from Be6 keeps Prophylaxis against counterplay connected to Rfe8 as a practical plan. On the checked line 15... Be6 16. b3 Rfe8 17. Qf2 b6 18. Rfd1 h5 19. d4, the pawn advances to d4, modifying the central pawn structure.",
        primaryKind = Some("WhatChanged"),
        selectedQuestion = Some("WhatChanged"),
        selectedOwnerKind = Some("MoveDelta"),
        selectedSource = Some("pv_coupled_plan_support"),
        fen = "r4rk1/pp3ppp/3q1n2/2p5/2P3b1/3PBPN1/PP2Q1PP/R4RK1 b - - 0 15",
        playedSan = "Be6",
        playedUci = "g4e6",
        plannerSceneType = Some("quiet_improvement"),
        truthClass = Some("Best"),
        truthReasonFamily = Some("InvestmentSacrifice"),
        localFact = producedLocalFactPayload("plan_support", "pv_coupled_line", "certified_strategy_delta", localFactEvidenceRefs),
        causalEvidenceSources = List("branch_line", "pv_coupled_plan_support"),
        causalQuestion = Some("WhatChanged"),
        causalRelations = List("played_move_consequence", "change_consequence"),
        supportRows =
          List(
            SupportRow("Checked line", "Short line: Be6 b3 Rfe8 Qf2 b6. Refs: Be6 b3 Rfe8 Qf2 b6.")
          ),
        advancedRows =
          List(
            SupportRow("Practical line", "The queen already has a practical e-file post."),
            SupportRow(
              "Engine path",
              "The checked line includes a central pawn advance after d4. The local result is a changed central pawn structure Refs: Be6 b3 Rfe8 Qf2."
            )
          )
      )
    val after =
      before.copy(
        advancedRows =
          List(
            SupportRow(
              "Engine path",
              "The checked line includes a central pawn advance after d4. The local result is a changed central pawn structure Refs: Be6 b3 Rfe8 Qf2."
            )
          )
      )

    val (gateRow, gate) =
      gatePair(
        before,
        after,
        defaultCodeCost(added = 8, deleted = 1, reused = List("MoveReviewPlayerPayloadBuilder.currentLocalFactRequiresExactSurface"))
      )
    assert(gateRow.beforeReasons.contains("plan_support_primary_diluted_by_broad_practical_surface"), clues(gateRow))
    assert(!gateRow.afterReasons.contains("plan_support_primary_diluted_by_broad_practical_surface"), clues(gateRow))

    assertGatePass(gate, wrongTierDelta = Some(-1), exactEvidenceSurfaceDelta = Some(0), evidenceBoundSurfaceDelta = Some(0))
  }

  test("moveReview regression gate counts canonical endgame local fact surface as exact evidence") {
    val sampleId = "g1:question_why_now:82:moveReview"
    val row =
      moveReviewEntry(
        sampleId = sampleId,
        commentary =
          "41... Kd5: Kd5 sets diagonal king opposition from d5 against the king on f3. Short line: Kd5 Rh1 Rh7 Rg1 Ke6.",
        primaryKind = None,
        selectedQuestion = None,
        selectedOwnerKind = None,
        selectedSource = None,
        fallbackMode = MoveReviewFallbackMode.ExactFactual,
        truthReasonFamily = Some("QuietTechnicalMove"),
        localFact =
          producedLocalFactPayload(
              "endgame",
              "canonical_fact",
              "endgame_fact",
              "endgame_fact:opposition",
              "king_square:f3",
              "enemy_king_square:d5",
              "opposition_type:Diagonal"
          ),
        supportRows =
          List(
            SupportRow(
              "Checked line",
              "The line keeps the diagonal opposition detail local to Kd5. Short line: Kd5 Rh1 Rh7 Rg1 Ke6."
            )
          )
      )

    val gateRow = sameRowGate(row)
    assertEquals(gateRow.beforeExactEvidenceSurface, true, clues(gateRow))
    assertEquals(gateRow.beforeEvidenceBoundSurface, true, clues(gateRow))
    assertEquals(gateRow.afterExactEvidenceSurface, true, clues(gateRow))
    assertEquals(gateRow.afterEvidenceBoundSurface, true, clues(gateRow))
    assertEquals(gateRow.beforeReasons, List("exact_evidence_surface", "evidence_bound_surface"))
    assertEquals(gateRow.afterReasons, List("exact_evidence_surface", "evidence_bound_surface"))
  }

  test("moveReview regression gate counts pv-coupled capture local fact surface as exact evidence") {
    val sampleId = "2026_03_01_bicibici3_starfihs_sample_734:transition_heavy_endgames:28:moveReview"
    val row =
      moveReviewEntry(
        sampleId = sampleId,
        commentary =
          "14... Bxg2: Bxg2 captures the pawn on g2. The checked line keeps the capture of the g2 pawn local to Bxg2: the checked continuation begins with Rhe1.",
        primaryKind = None,
        selectedQuestion = None,
        selectedOwnerKind = None,
        selectedSource = None,
        fallbackMode = MoveReviewFallbackMode.ExactFactual,
        fen = "r3k2r/p4p1p/p1bBpp2/8/8/8/PPP2PPP/2KR3R b kq - 1 14",
        playedSan = "Bxg2",
        playedUci = "c6g2",
        truthClass = Some("CompensatedInvestment"),
        truthReasonFamily = Some("InvestmentSacrifice"),
        localFact = producedLocalFactPayload(
          "capture",
          "pv_coupled_line",
          "capture_sequence",
          "capture_motif",
          "captured_square:g2",
          "captured_role:pawn"
        ),
        supportRows =
          List(
            SupportRow(
              "Checked line",
              "The checked line keeps the capture of the g2 pawn local to Bxg2: the checked continuation begins with Rhe1. Short line: Bxg2 Rhe1 Rc8 Re3 Rg8."
            )
          )
      )

    val gateRow = sameRowGate(row)
    assertEquals(gateRow.beforeExactEvidenceSurface, true, clues(gateRow))
    assertEquals(gateRow.beforeEvidenceBoundSurface, true, clues(gateRow))
    assertEquals(gateRow.afterExactEvidenceSurface, true, clues(gateRow))
    assertEquals(gateRow.afterEvidenceBoundSurface, true, clues(gateRow))
    assertEquals(gateRow.beforeReasons, List("exact_evidence_surface", "evidence_bound_surface"))
    assertEquals(gateRow.afterReasons, List("exact_evidence_surface", "evidence_bound_surface"))
  }

  test("moveReview regression gate treats bounded only-move line consequence as harmful reduction") {
    val sampleId = "g1:prophylaxis:8:moveReview"
    val before =
      moveReviewEntry(
        sampleId = sampleId,
        commentary =
          "8... Bg6: The timing matters now because other moves allow the position to slip away. Only the played move still keeps the position together now.",
        primaryKind = Some("WhyNow"),
        selectedQuestion = Some("WhyNow"),
        selectedOwnerKind = Some("ForcingDefense"),
        selectedSource = Some("only_move_defense"),
        truthReasonFamily = Some("OnlyMoveDefense"),
        truthOnlyMoveDefense = Some(true),
        truthBenchmarkCriticalMove = Some(true),
        localFact = producedLocalFactPayload("timing", "forced_reply", "forced_reply", "evidence_source:delayed_only_move"),
        causalEvidenceSources = List("delayed_only_move", "branch_line", "line_consequence_surface")
      )
    val after =
      moveReviewEntry(
        sampleId = sampleId,
        commentary =
          "8... Bg6: The timing matters now because the checked line reaches an exchange sequence after gxh5. The decision is about the resulting structure: White with an isolated pawn on h4.",
        primaryKind = Some("WhyNow"),
        selectedQuestion = Some("WhyNow"),
        selectedOwnerKind = Some("ForcingDefense"),
        selectedSource = Some("only_move_defense"),
        truthReasonFamily = Some("OnlyMoveDefense"),
        truthOnlyMoveDefense = Some(true),
        truthBenchmarkCriticalMove = Some(true),
        localFact = producedLocalFactPayload(
            "line_consequence",
            "pv_coupled_line",
            "line_consequence",
            "evidence_source:line_consequence_surface",
            "evidence_line_binding:pv_coupled",
            "line_consequence_kind:exchange_sequence",
            "line_consequence_trigger_san:gxh5"
        ),
        causalEvidenceSources = List("delayed_only_move", "branch_line", "line_consequence_surface", "timing_witness"),
        supportRows = List(SupportRow("Checked line", "Short line: Bg6 h4 h5 gxh5 Bxh5.")),
        causalSupportEmbedded = Some(true)
      )

    val (_, gate) = gatePair(before, after, defaultCodeCost(added = 12))
    assertEquals(gate.finalStatus, MoveReviewGateStatus.Pass)
    assertEquals(gate.harmfulOverclaimDelta, -1)
    assertEquals(gate.genericFallbackDelta, 0)
    assertEquals(gate.exactEvidenceSurfaceDelta, 1)
    assertEquals(gate.evidenceBoundSurfaceDelta, 1)
  }

  test("moveReview regression gate fails when before and after rows do not share source identity") {
    val sampleId = "g1:prophylaxis:7:moveReview"
    val before =
      moveReviewEntry(
        sampleId = sampleId,
        commentary = "7. Re1: The rook move is evaluated from this position.",
        primaryKind = None,
        selectedQuestion = None,
        selectedOwnerKind = None,
        selectedSource = None,
        fen = "8/8/8/8/8/8/8/K6k w - - 0 1"
      )
    val after =
      moveReviewEntry(
        sampleId = sampleId,
        commentary = "7. Re1: The rook move is evaluated from this position.",
        primaryKind = None,
        selectedQuestion = None,
        selectedOwnerKind = None,
        selectedSource = None,
        fen = "8/8/8/8/8/8/8/K6k b - - 0 1"
      )

    val (_, gate) = gatePair(before, after)
    assertEquals(gate.finalStatus, MoveReviewGateStatus.Fail)
    assertEquals(gate.sourceIdentityStatus, "source_identity_mismatch")
    assertEquals(gate.sourceIdentityMismatchCount, 1)
    assert(gate.sourceIdentityMismatches.contains(s"$sampleId:fen"), clues(gate))
    assert(gate.blockingReasons.contains("source_identity_mismatch"), clues(gate))
  }
