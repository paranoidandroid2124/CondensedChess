package lila.commentary.tools.quality

import play.api.libs.json.{ Format, Json }
import lila.commentary.tools.review.CommentaryPlayerQcSupport

object CommentaryQualityContrastSupport:

  import CommentaryPlayerQcSupport.*
  import CommentaryQualitySupport.*

  object ContrastSchema:
    val SelectorVersion = "commentary_quality_contrast_selector.v1"
    val EvalVersion = "commentary_quality_contrast_eval.v1"
    val SummaryVersion = "commentary_quality_contrast_summary.v1"
    val MoveReviewGateRowVersion = "move_review_regression_gate_row.v2"
    val MoveReviewGateSummaryVersion = "move_review_regression_gate_summary.v2"

  object SelectionStatus:
    val Eligible = "eligible"
    val UpstreamBlocked = "upstream_blocked"
    val ReplayBlocked = "replay_blocked"
    val AfterFallbackBlocked = "after_fallback_blocked"
    val BaselineBlocked = "baseline_blocked"
    val QuestionFiltered = "question_filtered"
    val NoPrimary = "no_primary"

  object FinalVerdict:
    val Keep = "keep"
    val Review = "review"
    val Reject = "reject"

  object MoveReviewGateRole:
    val RegressionBlockingGate = "regression_blocking_gate"

  object MoveReviewGateStatus:
    val Pass = "pass"
    val Fail = "fail"

  final case class CodeCostSummary(
      netLineAdded: Int,
      netLineDeleted: Int,
      newFileCount: Option[Int] = None,
      newHelperCount: Option[Int] = None,
      reusedExistingHelperNames: List[String] = Nil,
      notes: List[String] = Nil
  ):
    def netLineDelta: Int = netLineAdded - netLineDeleted

  object CodeCostSummary:
    given Format[CodeCostSummary] = Json.format[CodeCostSummary]

  final case class MoveReviewQualityGateRow(
      schemaVersion: String = ContrastSchema.MoveReviewGateRowVersion,
      sampleId: String,
      gameKey: String,
      sliceKind: String,
      targetPly: Int,
      playedSan: String,
      beforeHarmfulOverclaimRisk: Boolean,
      afterHarmfulOverclaimRisk: Boolean,
      beforeWrongFamilyRisk: Boolean,
      afterWrongFamilyRisk: Boolean,
      beforeWrongTierRisk: Boolean,
      afterWrongTierRisk: Boolean,
      beforeGenericFallback: Boolean,
      afterGenericFallback: Boolean,
      beforeExactEvidenceSurface: Boolean,
      afterExactEvidenceSurface: Boolean,
      beforeEvidenceBoundSurface: Boolean,
      afterEvidenceBoundSurface: Boolean,
      beforeCriticalTruthRow: Boolean,
      afterCriticalTruthRow: Boolean,
      beforeCriticalSurfacePresent: Boolean,
      afterCriticalSurfacePresent: Boolean,
      beforeCriticalSurfaceMissing: Boolean,
      afterCriticalSurfaceMissing: Boolean,
      beforeCriticalSurfaceGeneric: Boolean,
      afterCriticalSurfaceGeneric: Boolean,
      beforeCriticalSurfaceWrongFamily: Boolean,
      afterCriticalSurfaceWrongFamily: Boolean,
      beforeCriticalSurfaceOverstrong: Boolean,
      afterCriticalSurfaceOverstrong: Boolean,
      changed: Boolean,
      beforeReasons: List[String],
      afterReasons: List[String],
      beforeText: String,
      afterText: String
  )
  object MoveReviewQualityGateRow:
    given Format[MoveReviewQualityGateRow] = Json.format[MoveReviewQualityGateRow]

  final case class MoveReviewQualityGateSummary(
      schemaVersion: String = ContrastSchema.MoveReviewGateSummaryVersion,
      role: String = MoveReviewGateRole.RegressionBlockingGate,
      finalStatus: String,
      sampleCount: Int,
      changedCount: Int,
      harmfulOverclaimBefore: Int,
      harmfulOverclaimAfter: Int,
      harmfulOverclaimDelta: Int,
      wrongFamilyRiskBefore: Int,
      wrongFamilyRiskAfter: Int,
      wrongFamilyRiskDelta: Int,
      wrongTierRiskBefore: Int,
      wrongTierRiskAfter: Int,
      wrongTierRiskDelta: Int,
      genericFallbackBefore: Int,
      genericFallbackAfter: Int,
      genericFallbackDelta: Int,
      exactEvidenceSurfaceBefore: Int,
      exactEvidenceSurfaceAfter: Int,
      exactEvidenceSurfaceDelta: Int,
      evidenceBoundSurfaceBefore: Int,
      evidenceBoundSurfaceAfter: Int,
      evidenceBoundSurfaceDelta: Int,
      criticalTruthRowsBefore: Int,
      criticalTruthRowsAfter: Int,
      criticalTruthRowsDelta: Int,
      criticalSurfacePresentBefore: Int,
      criticalSurfacePresentAfter: Int,
      criticalSurfacePresentDelta: Int,
      criticalSurfaceMissingBefore: Int,
      criticalSurfaceMissingAfter: Int,
      criticalSurfaceMissingDelta: Int,
      criticalSurfaceGenericBefore: Int,
      criticalSurfaceGenericAfter: Int,
      criticalSurfaceGenericDelta: Int,
      criticalSurfaceWrongFamilyBefore: Int,
      criticalSurfaceWrongFamilyAfter: Int,
      criticalSurfaceWrongFamilyDelta: Int,
      criticalSurfaceOverstrongBefore: Int,
      criticalSurfaceOverstrongAfter: Int,
      criticalSurfaceOverstrongDelta: Int,
      sourceIdentityStatus: String,
      sourceIdentityMismatchCount: Int,
      sourceIdentityMismatches: List[String],
      sourceDigestDriftCount: Int,
      sourceDigestDrifts: List[String],
      blockingReasons: List[String],
      codeCost: Option[CodeCostSummary],
      metricBoundary: String =
        "Regression/blocking gate only; this does not claim to be a final commentary-quality evaluator.",
      evidenceBoundSurfaceNote: String =
        "evidence_bound_surface_delta is a proxy for retained field-backed surface evidence, not an automatic useful-explanation score.",
      criticalSurfaceNote: String =
        "critical_surface_* metrics are field-backed regression proxies for critical-truth rows, not a human chess verdict."
  )
  object MoveReviewQualityGateSummary:
    given Format[MoveReviewQualityGateSummary] = Json.format[MoveReviewQualityGateSummary]

  final case class ContrastSelectorRow(
      schemaVersion: String = ContrastSchema.SelectorVersion,
      sampleId: String,
      gameKey: String,
      surface: String,
      sliceKind: String,
      targetPly: Int,
      playedSan: String,
      plannerQuestion: Option[String],
      plannerProofFamily: Option[String],
      moveReviewFallbackMode: String,
      afterMoveReviewFallbackMode: Option[String] = None,
      upstreamTaxonomy: Option[String],
      upstreamAllowedByDesign: Boolean,
      upstreamAllowanceTag: Option[String],
      moveReviewPairBlockers: List[String] = Nil,
      selectionStatus: String,
      selectionReason: String,
      baselineSelectorScore: Option[Int] = None,
      baselineMoveAttributionCorrectness: Option[Int] = None,
      baselineOverclaimPenalty: Option[Int] = None,
      contrast_source_kind: Option[String] = None,
      contrast_anchor: Option[String] = None,
      contrast_consequence: Option[String] = None,
      contrast_admissible: Option[Boolean] = None,
      contrast_reject_reason: Option[String] = None,
      beforeText: String,
      afterText: String,
      changed: Boolean
  )
  object ContrastSelectorRow:
    given Format[ContrastSelectorRow] = Json.format[ContrastSelectorRow]

  final case class ContrastEvalRow(
      schemaVersion: String = ContrastSchema.EvalVersion,
      comparisonKey: String,
      sampleId: String,
      gameKey: String,
      surface: String,
      sliceKind: String,
      targetPly: Int,
      plannerQuestion: String,
      plannerProofFamily: Option[String],
      selectionStatus: String,
      selectionReason: String,
      upstreamTaxonomy: Option[String],
      upstreamAllowanceTag: Option[String],
      allowedByDesign: Boolean,
      contrast_source_kind: Option[String],
      contrast_anchor: Option[String],
      contrast_consequence: Option[String],
      contrast_admissible: Boolean,
      contrast_reject_reason: Option[String],
      beforeText: String,
      afterText: String,
      beforeRubric: EvaluationRubricScores,
      afterRubric: EvaluationRubricScores,
      beforeSelection: EvaluationSelection,
      afterSelection: EvaluationSelection,
      rawNetDelta: Int,
      selectorDelta: Int,
      finalVerdict: String,
      summary: String,
      flags: List[String] = Nil
  )
  object ContrastEvalRow:
    given Format[ContrastEvalRow] = Json.format[ContrastEvalRow]

  final case class ContrastSummary(
      schemaVersion: String = ContrastSchema.SummaryVersion,
      totalWhyRows: Int,
      contrastEligibleRows: Int,
      upstreamBlockedRows: Int,
      replayBlockedRows: Int,
      baselineBlockedRows: Int,
      questionFilteredRows: Int,
      noPrimaryRows: Int,
      eligibleKeepCount: Int,
      eligibleReviewCount: Int,
      eligibleRejectCount: Int,
      shadowKeepCount: Int,
      shadowReviewCount: Int,
      shadowRejectCount: Int,
      beforeBlankLikeCount: Int,
      afterBlankLikeCount: Int,
      beforeFallbackCount: Int,
      afterFallbackCount: Int,
      afterFallbackBlockedRows: Int = 0,
      changedCount: Int,
      unchangedCount: Int,
      degradedCount: Int,
      selectorStatusCounts: Map[String, Int],
      upstreamTaxonomyCounts: Map[String, Int],
      moveReviewPairBlockerCounts: Map[String, Int],
      upstreamPairBlockerCounts: Map[String, Int],
      replayPairBlockerCounts: Map[String, Int],
      contrastRejectReasonCounts: Map[String, Int],
      contrastUsefulnessDelta: Double,
      overclaimPenaltyDelta: Double,
      dryButTruePenaltyDelta: Double,
      moveAttributionDelta: Double,
      baselineRegressionStatus: String,
      crossSurfaceStabilityStatus: String
  )
  object ContrastSummary:
    given Format[ContrastSummary] = Json.format[ContrastSummary]

  final case class ContrastReport(
      selectorRows: List[ContrastSelectorRow],
      evalRows: List[ContrastEvalRow],
      summary: ContrastSummary,
      moveReviewGateRows: List[MoveReviewQualityGateRow] = Nil,
      moveReviewGate: Option[MoveReviewQualityGateSummary] = None
  )

  def buildContrastReport(
      beforeEntries: List[MoveReviewOutputEntry],
      afterEntries: List[MoveReviewOutputEntry],
      codeCost: Option[CodeCostSummary] = None
  ): Either[String, ContrastReport] =
    val beforeById = beforeEntries.map(entry => entry.sampleId -> entry).toMap
    val afterById = afterEntries.map(entry => entry.sampleId -> entry).toMap
    val missingAfter = beforeEntries.filterNot(entry => afterById.contains(entry.sampleId)).map(_.sampleId)

    if missingAfter.nonEmpty then Left(s"missing contrast after rows: ${missingAfter.mkString(", ")}")
    else
      val beforeThresholdBySampleId =
        beforeEntries.map(entry => entry.sampleId -> buildMoveReviewThresholdRow(entry)).toMap
      val afterThresholdBySampleId =
        afterEntries.map(entry => entry.sampleId -> buildMoveReviewThresholdRow(entry)).toMap
      val whyRows =
        beforeEntries.filter(entry =>
          entry.plannerPrimaryKind.contains("WhyThis") || entry.plannerPrimaryKind.contains("WhyNow")
        )

      val selectorRows = whyRows.map { before =>
        val after = afterById(before.sampleId)
        val upstreamTaxonomy = None
        val allowanceTag = None
        val allowedByDesign = true
        val pairBlockers = Nil
        val beforeThreshold = beforeThresholdBySampleId.get(before.sampleId)
        val beforeSelection = beforeThreshold.map(_.selection)
        val passedBaselineKeepGate =
          beforeSelection.exists(selection =>
            selection.moveAttributionGatePassed &&
              selection.selectorScore >= EvalThresholds.KeepSelectorMin &&
              beforeThreshold.exists(_.rubric.overclaimPenalty <= EvalThresholds.MaxOverclaimPenaltyForKeep)
          )
        val selectionStatus =
          if before.plannerPrimaryKind.isEmpty then SelectionStatus.NoPrimary
          else if !(before.plannerPrimaryKind.contains("WhyThis") || before.plannerPrimaryKind.contains("WhyNow")) then
            SelectionStatus.QuestionFiltered
          else if passedBaselineKeepGate && MoveReviewFallbackMode.isExactFactual(after.moveReviewFallbackMode) then
            SelectionStatus.AfterFallbackBlocked
          else if passedBaselineKeepGate then SelectionStatus.Eligible
          else SelectionStatus.BaselineBlocked
        val selectionReason =
          selectionStatus match
            case SelectionStatus.Eligible         => "upstream_allowed_and_baseline_keep_gate_passed"
            case SelectionStatus.AfterFallbackBlocked =>
              "after_move_review_exact_factual"
            case SelectionStatus.BaselineBlocked  => "baseline_keep_gate_failed"
            case SelectionStatus.QuestionFiltered => "question_outside_scope"
            case SelectionStatus.NoPrimary        => "no_planner_primary"
            case _                                => "blocked"

        ContrastSelectorRow(
          sampleId = before.sampleId,
          gameKey = before.gameKey,
          surface = SurfaceName.MoveReview,
          sliceKind = before.sliceKind,
          targetPly = before.targetPly,
          playedSan = before.playedSan,
          plannerQuestion = before.plannerPrimaryKind,
          plannerProofFamily = before.plannerSelectedOwnerKind,
          moveReviewFallbackMode = before.moveReviewFallbackMode,
          afterMoveReviewFallbackMode = Some(after.moveReviewFallbackMode),
          upstreamTaxonomy = upstreamTaxonomy,
          upstreamAllowedByDesign = allowedByDesign,
          upstreamAllowanceTag = allowanceTag,
          moveReviewPairBlockers = pairBlockers,
          selectionStatus = selectionStatus,
          selectionReason = selectionReason,
          baselineSelectorScore = beforeSelection.map(_.selectorScore),
          baselineMoveAttributionCorrectness = beforeThreshold.map(_.rubric.moveAttributionCorrectness),
          baselineOverclaimPenalty = beforeThreshold.map(_.rubric.overclaimPenalty),
          contrast_source_kind = after.contrast_source_kind,
          contrast_anchor = after.contrast_anchor,
          contrast_consequence = after.contrast_consequence,
          contrast_admissible = after.contrast_admissible,
          contrast_reject_reason = after.contrast_reject_reason,
          beforeText = before.commentary,
          afterText = after.commentary,
          changed = before.commentary != after.commentary
        )
      }

      val missingThresholdRows =
        selectorRows
          .filter(row =>
              row.selectionStatus == SelectionStatus.Eligible ||
              row.selectionStatus == SelectionStatus.BaselineBlocked
          )
          .filterNot(row =>
            beforeThresholdBySampleId.contains(row.sampleId) && afterThresholdBySampleId.contains(row.sampleId)
          )
          .map(_.sampleId)

      if missingThresholdRows.nonEmpty then
        Left(s"missing baseline threshold rows for contrast selector rows: ${missingThresholdRows.mkString(", ")}")
      else
        val gateRows =
          buildMoveReviewQualityGateRows(beforeEntries, afterById).sortBy(_.sampleId)
        val gateRowsBySampleId = gateRows.map(row => row.sampleId -> row).toMap
        val evalRows =
          selectorRows
            .filter(row =>
              row.selectionStatus == SelectionStatus.Eligible || row.selectionStatus == SelectionStatus.BaselineBlocked
            )
            .map { row =>
              val beforeThreshold = beforeThresholdBySampleId(row.sampleId)
              val afterThreshold = afterThresholdBySampleId(row.sampleId)
              val afterEntry = afterById(row.sampleId)
              val gateRow = gateRowsBySampleId(row.sampleId)
              val contrastRejected =
                row.selectionStatus == SelectionStatus.Eligible && afterEntry.contrast_admissible.contains(false)
              val contrastRejectBlocks =
                contrastRejected &&
                  (
                    !gateRow.afterExactEvidenceSurface ||
                      hasForcedOrAlternativeEvidence(afterEntry)
                  )
              val finalVerdict =
                if row.selectionStatus == SelectionStatus.Eligible && afterEntry.contrast_admissible.contains(true) &&
                    afterThreshold.selection.thresholdVerdict == EvalVerdict.Keep
                then FinalVerdict.Keep
                else if contrastRejectBlocks
                then FinalVerdict.Reject
                else FinalVerdict.Review

              ContrastEvalRow(
                comparisonKey = s"${row.gameKey}:${row.sliceKind}:${row.targetPly}",
                sampleId = row.sampleId,
                gameKey = row.gameKey,
                surface = row.surface,
                sliceKind = row.sliceKind,
                targetPly = row.targetPly,
                plannerQuestion = row.plannerQuestion.getOrElse("none"),
                plannerProofFamily = row.plannerProofFamily,
                selectionStatus = row.selectionStatus,
                selectionReason = row.selectionReason,
                upstreamTaxonomy = row.upstreamTaxonomy,
                upstreamAllowanceTag = row.upstreamAllowanceTag,
                allowedByDesign = row.upstreamAllowedByDesign,
                contrast_source_kind = row.contrast_source_kind,
                contrast_anchor = row.contrast_anchor,
                contrast_consequence = row.contrast_consequence,
                contrast_admissible = row.contrast_admissible.getOrElse(false),
                contrast_reject_reason = row.contrast_reject_reason,
                beforeText = row.beforeText,
                afterText = row.afterText,
                beforeRubric = beforeThreshold.rubric,
                afterRubric = afterThreshold.rubric,
                beforeSelection = beforeThreshold.selection,
                afterSelection = afterThreshold.selection,
                rawNetDelta = afterThreshold.rubric.netScore - beforeThreshold.rubric.netScore,
                selectorDelta = afterThreshold.selection.selectorScore - beforeThreshold.selection.selectorScore,
                finalVerdict = finalVerdict,
                summary =
                  if row.selectionStatus == SelectionStatus.Eligible then
                    "Baseline keep row evaluated under contrast support-slot constraints."
                  else
                    "Baseline shadow row stayed below keep before contrast selection.",
                flags = (beforeThreshold.flags ++ afterThreshold.flags).distinct
              )
            }

        val selectorStatusCounts = countValues(selectorRows.map(_.selectionStatus))
        val eligibleEvalRows = evalRows.filter(_.selectionStatus == SelectionStatus.Eligible)
        val shadowEvalRows = evalRows.filter(_.selectionStatus == SelectionStatus.BaselineBlocked)
        val evalSampleIds = evalRows.map(_.sampleId).toSet

        val summary =
          ContrastSummary(
            totalWhyRows = whyRows.size,
            contrastEligibleRows = selectorStatusCounts.getOrElse(SelectionStatus.Eligible, 0),
            upstreamBlockedRows = selectorStatusCounts.getOrElse(SelectionStatus.UpstreamBlocked, 0),
            replayBlockedRows = selectorStatusCounts.getOrElse(SelectionStatus.ReplayBlocked, 0),
            baselineBlockedRows = selectorStatusCounts.getOrElse(SelectionStatus.BaselineBlocked, 0),
            questionFilteredRows = selectorStatusCounts.getOrElse(SelectionStatus.QuestionFiltered, 0),
            noPrimaryRows = selectorStatusCounts.getOrElse(SelectionStatus.NoPrimary, 0),
            eligibleKeepCount = eligibleEvalRows.count(_.finalVerdict == FinalVerdict.Keep),
            eligibleReviewCount = eligibleEvalRows.count(_.finalVerdict == FinalVerdict.Review),
            eligibleRejectCount = eligibleEvalRows.count(_.finalVerdict == FinalVerdict.Reject),
            shadowKeepCount = shadowEvalRows.count(_.finalVerdict == FinalVerdict.Keep),
            shadowReviewCount = shadowEvalRows.count(_.finalVerdict == FinalVerdict.Review),
            shadowRejectCount = shadowEvalRows.count(_.finalVerdict == FinalVerdict.Reject),
            beforeBlankLikeCount = selectorRows.count(row => blankLike(row.beforeText)),
            afterBlankLikeCount = selectorRows.count(row => blankLike(row.afterText)),
            beforeFallbackCount =
              evalSampleIds.count(sampleId => MoveReviewFallbackMode.isExactFactual(beforeById(sampleId).moveReviewFallbackMode)),
            afterFallbackCount =
              evalSampleIds.count(sampleId => MoveReviewFallbackMode.isExactFactual(afterById(sampleId).moveReviewFallbackMode)),
            afterFallbackBlockedRows =
              selectorRows.count(_.selectionStatus == SelectionStatus.AfterFallbackBlocked),
            changedCount = selectorRows.count(_.changed),
            unchangedCount = selectorRows.count(row => !row.changed),
            degradedCount =
              evalRows.count(row =>
                row.afterSelection.selectorScore < row.beforeSelection.selectorScore ||
                  row.afterRubric.overclaimPenalty > row.beforeRubric.overclaimPenalty
              ),
            selectorStatusCounts = selectorStatusCounts,
            upstreamTaxonomyCounts =
              countValues(
                selectorRows.collect {
                  case row if row.selectionStatus == SelectionStatus.UpstreamBlocked => row.upstreamTaxonomy
                }.flatten
              ),
            moveReviewPairBlockerCounts = countValues(selectorRows.flatMap(_.moveReviewPairBlockers)),
            upstreamPairBlockerCounts =
              countValues(
                selectorRows
                  .filter(_.selectionStatus == SelectionStatus.UpstreamBlocked)
                  .flatMap(_.moveReviewPairBlockers)
              ),
            replayPairBlockerCounts =
              countValues(
                selectorRows
                  .filter(_.selectionStatus == SelectionStatus.ReplayBlocked)
                  .flatMap(_.moveReviewPairBlockers)
              ),
            contrastRejectReasonCounts = countValues(selectorRows.flatMap(_.contrast_reject_reason)),
            contrastUsefulnessDelta =
              averageDelta(evalRows)(row => row.afterRubric.contrastUsefulness - row.beforeRubric.contrastUsefulness),
            overclaimPenaltyDelta =
              averageDelta(evalRows)(row => row.afterRubric.overclaimPenalty - row.beforeRubric.overclaimPenalty),
            dryButTruePenaltyDelta =
              averageDelta(evalRows)(row => row.afterRubric.dryButTruePenalty - row.beforeRubric.dryButTruePenalty),
            moveAttributionDelta =
              averageDelta(evalRows)(row =>
                row.afterRubric.moveAttributionCorrectness - row.beforeRubric.moveAttributionCorrectness
              ),
            baselineRegressionStatus =
              if evalRows.exists(row => row.afterSelection.selectorScore < row.beforeSelection.selectorScore)
              then "regression_detected"
              else "no_baseline_regression",
            crossSurfaceStabilityStatus = "stable"
          )

        val sourceIdentityMismatches =
          buildMoveReviewSourceIdentityMismatches(beforeEntries, afterEntries, afterById)
        val sourceDigestDrifts =
          buildMoveReviewSourceDigestDrifts(beforeEntries, afterById)
        val gateSummary =
          buildMoveReviewQualityGateSummary(gateRows, codeCost, sourceIdentityMismatches, sourceDigestDrifts)
        Right(
          ContrastReport(
            selectorRows = selectorRows.sortBy(_.sampleId),
            evalRows = evalRows.sortBy(_.sampleId),
            summary = summary,
            moveReviewGateRows = gateRows,
            moveReviewGate = Some(gateSummary)
          )
        )

  def renderContrastMarkdown(report: ContrastReport): String =
    val eligible = renderRows(report.selectorRows.filter(_.selectionStatus == SelectionStatus.Eligible))
    val upstreamBlocked = renderRows(report.selectorRows.filter(_.selectionStatus == SelectionStatus.UpstreamBlocked))
    val replayBlocked = renderRows(report.selectorRows.filter(_.selectionStatus == SelectionStatus.ReplayBlocked))
    val afterFallbackBlocked =
      renderRows(report.selectorRows.filter(_.selectionStatus == SelectionStatus.AfterFallbackBlocked))
    val baselineBlocked = renderEvalRows(report.evalRows.filter(_.selectionStatus == SelectionStatus.BaselineBlocked))
    val eligibleEval = renderEvalRows(report.evalRows.filter(_.selectionStatus == SelectionStatus.Eligible))

    s"""# Commentary Quality Contrast Report
       |
       |- Selector status counts: ${renderCountMap(report.summary.selectorStatusCounts)}
       |- Upstream taxonomy counts: ${renderCountMap(report.summary.upstreamTaxonomyCounts)}
       |- MoveReview pair blockers: ${renderCountMap(report.summary.moveReviewPairBlockerCounts)}
       |- Upstream pair blockers: ${renderCountMap(report.summary.upstreamPairBlockerCounts)}
       |- Replay pair blockers: ${renderCountMap(report.summary.replayPairBlockerCounts)}
       |- Contrast reject reasons: ${renderCountMap(report.summary.contrastRejectReasonCounts)}
       |- Eligible keep / review / reject: `keep=${report.summary.eligibleKeepCount}` `review=${report.summary.eligibleReviewCount}` `reject=${report.summary.eligibleRejectCount}`
       |- Shadow keep / review / reject: `keep=${report.summary.shadowKeepCount}` `review=${report.summary.shadowReviewCount}` `reject=${report.summary.shadowRejectCount}`
       |- Blank-like before / after: `${report.summary.beforeBlankLikeCount} / ${report.summary.afterBlankLikeCount}`
       |- Fallback before / after: `${report.summary.beforeFallbackCount} / ${report.summary.afterFallbackCount}` (`blocked=${report.summary.afterFallbackBlockedRows}`)
       |- Changed / unchanged / degraded: `${report.summary.changedCount} / ${report.summary.unchangedCount} / ${report.summary.degradedCount}`
       |- Metric deltas: `contrast_usefulness=${formatDouble(report.summary.contrastUsefulnessDelta)}` `overclaim_penalty=${formatDouble(report.summary.overclaimPenaltyDelta)}` `dry_but_true_penalty=${formatDouble(report.summary.dryButTruePenaltyDelta)}` `move_attribution_correctness=${formatDouble(report.summary.moveAttributionDelta)}`
       |- Baseline regression: `${report.summary.baselineRegressionStatus}`
       |- Cross-surface stability: `${report.summary.crossSurfaceStabilityStatus}`
       |
       |## MoveReview Regression Gate
       |
       |${renderMoveReviewGate(report.moveReviewGate)}
       |
       |## Contrast-eligible rows
       |
       |$eligible
       |
       |## Upstream-blocked rows
       |
       |$upstreamBlocked
       |
       |## Replay-blocked rows
       |
       |$replayBlocked
       |
       |## After-fallback blocked rows
       |
       |$afterFallbackBlocked
       |
       |## Baseline-blocked shadow rows
       |
       |$baselineBlocked
       |
       |## Eligible before / after
       |
       |$eligibleEval
       |""".stripMargin

  private def renderRows(rows: List[ContrastSelectorRow]): String =
    if rows.isEmpty then "- none"
    else
      rows.map { row =>
        s"- `${row.sampleId}` question=`${row.plannerQuestion.getOrElse("none")}` owner=`${row.plannerProofFamily.getOrElse("-")}` status=`${row.selectionStatus}` reason=`${row.selectionReason}` pairBlockers=`${row.moveReviewPairBlockers.mkString("|")}` contrast=`${row.contrast_source_kind.getOrElse("-")}` reject=`${row.contrast_reject_reason.getOrElse("-")}` changed=`${row.changed}`"
      }.mkString("\n")

  private def renderEvalRows(rows: List[ContrastEvalRow]): String =
    if rows.isEmpty then "- none"
    else
      rows.map { row =>
        s"- `${row.sampleId}` verdict=`${row.finalVerdict}` rawNet `${row.beforeRubric.netScore} -> ${row.afterRubric.netScore}` selector `${row.beforeSelection.selectorScore} -> ${row.afterSelection.selectorScore}` contrast=`${row.contrast_source_kind.getOrElse("-")}` reject=`${row.contrast_reject_reason.getOrElse("-")}` before=`${row.beforeText}` after=`${row.afterText}`"
      }.mkString("\n")

  def renderMoveReviewGate(summary: Option[MoveReviewQualityGateSummary]): String =
    summary match
      case None => "- gate not generated"
      case Some(gate) =>
        val codeCost =
          gate.codeCost
            .map(cost =>
              s"`+${cost.netLineAdded}/-${cost.netLineDeleted}` net=`${cost.netLineDelta}` newFiles=`${cost.newFileCount.map(_.toString).getOrElse("unknown")}` newHelpers=`${cost.newHelperCount.map(_.toString).getOrElse("unknown")}` reusedHelpers=`${if cost.reusedExistingHelperNames.isEmpty then "-" else cost.reusedExistingHelperNames.mkString(",")}`"
            )
            .getOrElse("`unknown`")
        s"""- Role: `${gate.role}`
           |- Status: `${gate.finalStatus}`
           |- Boundary: ${gate.metricBoundary}
           |- Harmful overclaim before / after / delta: `${gate.harmfulOverclaimBefore} / ${gate.harmfulOverclaimAfter} / ${gate.harmfulOverclaimDelta}`
           |- Wrong-family risk before / after / delta: `${gate.wrongFamilyRiskBefore} / ${gate.wrongFamilyRiskAfter} / ${gate.wrongFamilyRiskDelta}`
           |- Wrong-tier risk before / after / delta: `${gate.wrongTierRiskBefore} / ${gate.wrongTierRiskAfter} / ${gate.wrongTierRiskDelta}`
           |- Generic fallback before / after / delta: `${gate.genericFallbackBefore} / ${gate.genericFallbackAfter} / ${gate.genericFallbackDelta}`
           |- Exact evidence surface before / after / delta: `${gate.exactEvidenceSurfaceBefore} / ${gate.exactEvidenceSurfaceAfter} / ${gate.exactEvidenceSurfaceDelta}`
           |- Evidence-bound surface before / after / delta: `${gate.evidenceBoundSurfaceBefore} / ${gate.evidenceBoundSurfaceAfter} / ${gate.evidenceBoundSurfaceDelta}`
           |- Critical truth rows before / after / delta: `${gate.criticalTruthRowsBefore} / ${gate.criticalTruthRowsAfter} / ${gate.criticalTruthRowsDelta}`
           |- Critical surface present before / after / delta: `${gate.criticalSurfacePresentBefore} / ${gate.criticalSurfacePresentAfter} / ${gate.criticalSurfacePresentDelta}`
           |- Critical surface missing before / after / delta: `${gate.criticalSurfaceMissingBefore} / ${gate.criticalSurfaceMissingAfter} / ${gate.criticalSurfaceMissingDelta}`
           |- Critical surface generic before / after / delta: `${gate.criticalSurfaceGenericBefore} / ${gate.criticalSurfaceGenericAfter} / ${gate.criticalSurfaceGenericDelta}`
           |- Critical surface wrong-family before / after / delta: `${gate.criticalSurfaceWrongFamilyBefore} / ${gate.criticalSurfaceWrongFamilyAfter} / ${gate.criticalSurfaceWrongFamilyDelta}`
           |- Critical surface overstrong before / after / delta: `${gate.criticalSurfaceOverstrongBefore} / ${gate.criticalSurfaceOverstrongAfter} / ${gate.criticalSurfaceOverstrongDelta}`
           |- Source identity: `${gate.sourceIdentityStatus}` mismatches=`${gate.sourceIdentityMismatchCount}` ${gate.sourceIdentityMismatches.take(5).mkString("; ")}
           |- Source digest drift: `${gate.sourceDigestDriftCount}` ${gate.sourceDigestDrifts.take(5).mkString("; ")}
           |- Blocking reasons: `${if gate.blockingReasons.isEmpty then "none" else gate.blockingReasons.mkString(",")}`
           |- Code cost: $codeCost""".stripMargin

  private def buildMoveReviewQualityGateRows(
      beforeEntries: List[MoveReviewOutputEntry],
      afterById: Map[String, MoveReviewOutputEntry]
  ): List[MoveReviewQualityGateRow] =
    beforeEntries.flatMap { before =>
      afterById.get(before.sampleId).map { after =>
        val beforeSignals = moveReviewGateSignals(before)
        val afterSignals = moveReviewGateSignals(after)
        val beforeSurfaceText =
          moveReviewSurfaceText(before, "\n")
        val afterSurfaceText =
          moveReviewSurfaceText(after, "\n")
        MoveReviewQualityGateRow(
          sampleId = before.sampleId,
          gameKey = before.gameKey,
          sliceKind = before.sliceKind,
          targetPly = before.targetPly,
          playedSan = before.playedSan,
          beforeHarmfulOverclaimRisk = beforeSignals.harmfulOverclaimRisk,
          afterHarmfulOverclaimRisk = afterSignals.harmfulOverclaimRisk,
          beforeWrongFamilyRisk = beforeSignals.wrongFamilyRisk,
          afterWrongFamilyRisk = afterSignals.wrongFamilyRisk,
          beforeWrongTierRisk = beforeSignals.wrongTierRisk,
          afterWrongTierRisk = afterSignals.wrongTierRisk,
          beforeGenericFallback = beforeSignals.genericFallback,
          afterGenericFallback = afterSignals.genericFallback,
          beforeExactEvidenceSurface = beforeSignals.exactEvidenceSurface,
          afterExactEvidenceSurface = afterSignals.exactEvidenceSurface,
          beforeEvidenceBoundSurface = beforeSignals.evidenceBoundSurface,
          afterEvidenceBoundSurface = afterSignals.evidenceBoundSurface,
          beforeCriticalTruthRow = beforeSignals.criticalTruthRow,
          afterCriticalTruthRow = afterSignals.criticalTruthRow,
          beforeCriticalSurfacePresent = beforeSignals.criticalSurfacePresent,
          afterCriticalSurfacePresent = afterSignals.criticalSurfacePresent,
          beforeCriticalSurfaceMissing = beforeSignals.criticalSurfaceMissing,
          afterCriticalSurfaceMissing = afterSignals.criticalSurfaceMissing,
          beforeCriticalSurfaceGeneric = beforeSignals.criticalSurfaceGeneric,
          afterCriticalSurfaceGeneric = afterSignals.criticalSurfaceGeneric,
          beforeCriticalSurfaceWrongFamily = beforeSignals.criticalSurfaceWrongFamily,
          afterCriticalSurfaceWrongFamily = afterSignals.criticalSurfaceWrongFamily,
          beforeCriticalSurfaceOverstrong = beforeSignals.criticalSurfaceOverstrong,
          afterCriticalSurfaceOverstrong = afterSignals.criticalSurfaceOverstrong,
          changed = beforeSurfaceText != afterSurfaceText,
          beforeReasons = beforeSignals.reasons,
          afterReasons = afterSignals.reasons,
          beforeText = beforeSurfaceText,
          afterText = afterSurfaceText
        )
      }
    }

  private final case class MoveReviewGateSignals(
      harmfulOverclaimRisk: Boolean,
      wrongFamilyRisk: Boolean,
      wrongTierRisk: Boolean,
      genericFallback: Boolean,
      exactEvidenceSurface: Boolean,
      evidenceBoundSurface: Boolean,
      criticalTruthRow: Boolean,
      criticalSurfacePresent: Boolean,
      criticalSurfaceMissing: Boolean,
      criticalSurfaceGeneric: Boolean,
      criticalSurfaceWrongFamily: Boolean,
      criticalSurfaceOverstrong: Boolean,
      reasons: List[String]
  )

  private final case class MoveReviewGateMetric(before: Int, after: Int):
    val delta: Int = after - before

  private def moveReviewGateMetric(
      rows: List[MoveReviewQualityGateRow]
  )(
      before: MoveReviewQualityGateRow => Boolean,
      after: MoveReviewQualityGateRow => Boolean
  ): MoveReviewGateMetric =
    MoveReviewGateMetric(rows.count(before), rows.count(after))

  private def moveReviewGateSignals(entry: MoveReviewOutputEntry): MoveReviewGateSignals =
    val harmfulReasons = harmfulOverclaimReasons(entry)
    val familyReasons = wrongFamilyReasons(entry)
    val tierReasons = wrongTierReasons(entry)
    val genericFallback = genericFallbackRisk(entry)
    val exactEvidenceSurface = exactEvidenceSurfacePresent(entry, genericFallback)
    val evidenceBoundSurface =
      exactEvidenceSurface && !blankLike(entry.commentary)
    val criticalTruthRow = criticalTruth(entry)
    val criticalCuePresent = criticalSurfaceCuePresent(entry)
    val criticalSurfacePresent =
      criticalTruthRow && criticalSurfaceEvidenceAvailable(entry) && criticalCuePresent
    val criticalSurfaceGeneric =
      criticalTruthRow && genericFallback && !criticalCuePresent
    val criticalSurfaceMissing =
      criticalTruthRow && !criticalSurfacePresent && !criticalSurfaceGeneric
    val criticalSurfaceWrongFamily =
      criticalTruthRow && familyReasons.nonEmpty
    val criticalSurfaceOverstrong =
      criticalTruthRow && criticalSurfaceOverstrongRisk(entry)
    val reasons =
      harmfulReasons ++
        familyReasons ++
        tierReasons ++
        Option.when(genericFallback)("generic_fallback").toList ++
        Option.when(exactEvidenceSurface)("exact_evidence_surface").toList ++
        Option.when(evidenceBoundSurface)("evidence_bound_surface").toList ++
        Option.when(criticalTruthRow)("critical_truth_row").toList ++
        Option.when(criticalSurfacePresent)("critical_surface_present").toList ++
        Option.when(criticalSurfaceMissing)("critical_surface_missing").toList ++
        Option.when(criticalSurfaceGeneric)("critical_surface_generic").toList ++
        Option.when(criticalSurfaceWrongFamily)("critical_surface_wrong_family").toList ++
        Option.when(criticalSurfaceOverstrong)("critical_surface_overstrong").toList
    MoveReviewGateSignals(
      harmfulOverclaimRisk = harmfulReasons.nonEmpty,
      wrongFamilyRisk = familyReasons.nonEmpty,
      wrongTierRisk = tierReasons.nonEmpty,
      genericFallback = genericFallback,
      exactEvidenceSurface = exactEvidenceSurface,
      evidenceBoundSurface = evidenceBoundSurface,
      criticalTruthRow = criticalTruthRow,
      criticalSurfacePresent = criticalSurfacePresent,
      criticalSurfaceMissing = criticalSurfaceMissing,
      criticalSurfaceGeneric = criticalSurfaceGeneric,
      criticalSurfaceWrongFamily = criticalSurfaceWrongFamily,
      criticalSurfaceOverstrong = criticalSurfaceOverstrong,
      reasons = reasons.distinct
    )

  private def buildMoveReviewQualityGateSummary(
      rows: List[MoveReviewQualityGateRow],
      codeCost: Option[CodeCostSummary],
      sourceIdentityMismatches: List[String],
      sourceDigestDrifts: List[String]
  ): MoveReviewQualityGateSummary =
    val harmful = moveReviewGateMetric(rows)(_.beforeHarmfulOverclaimRisk, _.afterHarmfulOverclaimRisk)
    val wrongFamily = moveReviewGateMetric(rows)(_.beforeWrongFamilyRisk, _.afterWrongFamilyRisk)
    val wrongTier = moveReviewGateMetric(rows)(_.beforeWrongTierRisk, _.afterWrongTierRisk)
    val generic = moveReviewGateMetric(rows)(_.beforeGenericFallback, _.afterGenericFallback)
    val exact = moveReviewGateMetric(rows)(_.beforeExactEvidenceSurface, _.afterExactEvidenceSurface)
    val evidence = moveReviewGateMetric(rows)(_.beforeEvidenceBoundSurface, _.afterEvidenceBoundSurface)
    val criticalTruth = moveReviewGateMetric(rows)(_.beforeCriticalTruthRow, _.afterCriticalTruthRow)
    val criticalPresent = moveReviewGateMetric(rows)(_.beforeCriticalSurfacePresent, _.afterCriticalSurfacePresent)
    val criticalMissing = moveReviewGateMetric(rows)(_.beforeCriticalSurfaceMissing, _.afterCriticalSurfaceMissing)
    val criticalGeneric = moveReviewGateMetric(rows)(_.beforeCriticalSurfaceGeneric, _.afterCriticalSurfaceGeneric)
    val criticalWrongFamily = moveReviewGateMetric(rows)(_.beforeCriticalSurfaceWrongFamily, _.afterCriticalSurfaceWrongFamily)
    val criticalOverstrong = moveReviewGateMetric(rows)(_.beforeCriticalSurfaceOverstrong, _.afterCriticalSurfaceOverstrong)
    val metricGain =
      (-harmful.delta).max(0) +
        (-wrongFamily.delta).max(0) +
        (-wrongTier.delta).max(0) +
        (-generic.delta).max(0) +
        exact.delta.max(0) +
        evidence.delta.max(0) +
        criticalTruth.delta.max(0) +
        criticalPresent.delta.max(0) +
        (-criticalMissing.delta).max(0) +
        (-criticalGeneric.delta).max(0) +
        (-criticalWrongFamily.delta).max(0) +
        (-criticalOverstrong.delta).max(0)
    val blockingReasons =
      List(
        Option.when(sourceIdentityMismatches.nonEmpty)("source_identity_mismatch"),
        Option.when(harmful.delta > 0)("harmful_overclaim_increased"),
        Option.when(wrongFamily.delta > 0)("wrong_family_risk_increased"),
        Option.when(wrongTier.delta > 0)("wrong_tier_risk_increased"),
        Option.when(exact.delta < 0)("exact_evidence_surface_decreased"),
        Option.when(evidence.delta < 0)("evidence_bound_surface_delta_negative"),
        Option.when(generic.delta > 0 && exact.delta <= 0)("generic_fallback_increased_without_exact_evidence_gain"),
        Option.when(criticalTruth.delta < 0)("critical_truth_rows_decreased"),
        Option.when(criticalPresent.delta < 0)("critical_surface_present_decreased"),
        Option.when(criticalMissing.delta > 0)("critical_surface_missing_increased"),
        Option.when(criticalGeneric.delta > 0)("critical_surface_generic_increased"),
        Option.when(criticalWrongFamily.delta > 0)("critical_surface_wrong_family_increased"),
        Option.when(criticalOverstrong.delta > 0)("critical_surface_overstrong_increased"),
        Option.when(codeCost.exists(_.netLineDelta > 0) && metricGain == 0)("net_code_growth_without_gate_metric_gain")
      ).flatten
    MoveReviewQualityGateSummary(
      finalStatus = if blockingReasons.isEmpty then MoveReviewGateStatus.Pass else MoveReviewGateStatus.Fail,
      sampleCount = rows.size,
      changedCount = rows.count(_.changed),
      harmfulOverclaimBefore = harmful.before,
      harmfulOverclaimAfter = harmful.after,
      harmfulOverclaimDelta = harmful.delta,
      wrongFamilyRiskBefore = wrongFamily.before,
      wrongFamilyRiskAfter = wrongFamily.after,
      wrongFamilyRiskDelta = wrongFamily.delta,
      wrongTierRiskBefore = wrongTier.before,
      wrongTierRiskAfter = wrongTier.after,
      wrongTierRiskDelta = wrongTier.delta,
      genericFallbackBefore = generic.before,
      genericFallbackAfter = generic.after,
      genericFallbackDelta = generic.delta,
      exactEvidenceSurfaceBefore = exact.before,
      exactEvidenceSurfaceAfter = exact.after,
      exactEvidenceSurfaceDelta = exact.delta,
      evidenceBoundSurfaceBefore = evidence.before,
      evidenceBoundSurfaceAfter = evidence.after,
      evidenceBoundSurfaceDelta = evidence.delta,
      criticalTruthRowsBefore = criticalTruth.before,
      criticalTruthRowsAfter = criticalTruth.after,
      criticalTruthRowsDelta = criticalTruth.delta,
      criticalSurfacePresentBefore = criticalPresent.before,
      criticalSurfacePresentAfter = criticalPresent.after,
      criticalSurfacePresentDelta = criticalPresent.delta,
      criticalSurfaceMissingBefore = criticalMissing.before,
      criticalSurfaceMissingAfter = criticalMissing.after,
      criticalSurfaceMissingDelta = criticalMissing.delta,
      criticalSurfaceGenericBefore = criticalGeneric.before,
      criticalSurfaceGenericAfter = criticalGeneric.after,
      criticalSurfaceGenericDelta = criticalGeneric.delta,
      criticalSurfaceWrongFamilyBefore = criticalWrongFamily.before,
      criticalSurfaceWrongFamilyAfter = criticalWrongFamily.after,
      criticalSurfaceWrongFamilyDelta = criticalWrongFamily.delta,
      criticalSurfaceOverstrongBefore = criticalOverstrong.before,
      criticalSurfaceOverstrongAfter = criticalOverstrong.after,
      criticalSurfaceOverstrongDelta = criticalOverstrong.delta,
      sourceIdentityStatus =
        if sourceIdentityMismatches.isEmpty then "same_position_cache_identity" else "source_identity_mismatch",
      sourceIdentityMismatchCount = sourceIdentityMismatches.size,
      sourceIdentityMismatches = sourceIdentityMismatches.take(50),
      sourceDigestDriftCount = sourceDigestDrifts.size,
      sourceDigestDrifts = sourceDigestDrifts.take(50),
      blockingReasons = blockingReasons,
      codeCost = codeCost
    )

  private def buildMoveReviewSourceIdentityMismatches(
      beforeEntries: List[MoveReviewOutputEntry],
      afterEntries: List[MoveReviewOutputEntry],
      afterById: Map[String, MoveReviewOutputEntry]
  ): List[String] =
    val beforeIds = beforeEntries.map(_.sampleId).toSet
    val extraAfter =
      afterEntries
        .filterNot(entry => beforeIds.contains(entry.sampleId))
        .map(entry => s"${entry.sampleId}:extra_after_row")
    val paired =
      beforeEntries.flatMap { before =>
        afterById.get(before.sampleId).toList.flatMap { after =>
          List(
            Option.when(before.gameKey != after.gameKey)("gameKey"),
            Option.when(before.sliceKind != after.sliceKind)("sliceKind"),
            Option.when(before.targetPly != after.targetPly)("targetPly"),
            Option.when(before.fen != after.fen)("fen"),
            Option.when(before.playedSan != after.playedSan)("playedSan"),
            Option.when(before.playedUci != after.playedUci)("playedUci"),
            Option.when(before.cacheHit != after.cacheHit)("cacheHit")
          ).flatten.map(field => s"${before.sampleId}:$field")
        }
      }
    (extraAfter ++ paired).distinct.sorted

  private def buildMoveReviewSourceDigestDrifts(
      beforeEntries: List[MoveReviewOutputEntry],
      afterById: Map[String, MoveReviewOutputEntry]
  ): List[String] =
    beforeEntries.flatMap { before =>
      afterById.get(before.sampleId).toList.flatMap { after =>
        val digest =
          List(
            Option.when(before.moveReviewSnapshotDigestHash != after.moveReviewSnapshotDigestHash)("moveReviewSnapshotDigestHash"),
            Option.when(before.moveReviewCarryDigestHash != after.moveReviewCarryDigestHash)("moveReviewCarryDigestHash"),
            Option.when(before.moveReviewAugmentationDigestHash != after.moveReviewAugmentationDigestHash)(
              "moveReviewAugmentationDigestHash"
            ),
            Option.when(before.moveReviewBundleDigestHash != after.moveReviewBundleDigestHash)("moveReviewBundleDigestHash")
          ).flatten
        digest.map(field => s"${before.sampleId}:$field")
      }
    }.distinct.sorted

  private def harmfulOverclaimReasons(entry: MoveReviewOutputEntry): List[String] =
    val boundedOnlyMoveLineConsequence = boundedOnlyMoveLineConsequenceSurface(entry)
    val refs = normalizedEvidenceRefs(entry)
    val localFactGuardrails = entry.moveReviewCausalClaimLocalFactGuardrails.map(normalized)
    val nonUniqueReplyCount =
      refs.exists(ref =>
        ref
          .stripPrefix("reply_defense_count:")
          .toIntOption
          .exists(_ > 1)
      )
    val visibleOnlyMoveLanguage =
      entry.commentary.contains("Only the played move still keeps the position together now") ||
        normalized(entry.commentary).contains("other moves allow the position to slip away")
    val visibleForcedReplyLanguage =
      normalized(entry.commentary).contains("the forced reply") ||
        normalized(entry.commentary).contains("the reply becomes forced") ||
        normalized(entry.commentary).contains("only defensive reply")
    val boundedNonUniqueReplyLossSurface =
      hasLocalFactAuthority(entry, "forced_reply") &&
        refs.exists(_ == "evidence_source:explicit_reply_loss") &&
        localFactGuardrails.exists(_.endsWith("forced_reply_non_unique")) &&
        localFactGuardrails.exists(_.endsWith("surface_forced=false")) &&
        nonUniqueReplyCount &&
        refs.exists(_.startsWith("loss_if_ignored_cp:")) &&
        refs.exists(_.startsWith("threat_kind:")) &&
        refs.exists(_.startsWith("threat_square:")) &&
        !visibleOnlyMoveLanguage &&
        !visibleForcedReplyLanguage
    val certifiedPlanSupportRebound =
      entry.moveReviewCausalClaimQuestion.exists(question => normalized(question) == "whythis") &&
        entry.moveReviewCausalClaimRelations.exists(relation => normalized(relation) == "played_move_consequence") &&
        hasLocalFact(entry, "plan_support", "certified_strategy", "certified_strategy_delta") &&
        entry.moveReviewCausalClaimEvidenceSources.exists(source => normalized(source) == "typed_local_fact") &&
        refs.exists(_ == "typed_local_fact_source:practical_position_support") &&
        refs.exists(_ == "typed_local_fact_family:plan_support") &&
        refs.exists(_ == "typed_local_fact_producer:certified_strategy_delta") &&
        refs.exists(_ == "evidence_line_binding:pv_coupled") &&
        !visibleOnlyMoveLanguage
    val unboundedOnlyMoveSurface =
      !boundedOnlyMoveLineConsequence && !certifiedPlanSupportRebound
    List(
      Option.when(entry.truthOnlyMoveDefense.contains(true) && unboundedOnlyMoveSurface)("truth_only_move_defense"),
      Option.when(
        hasLocalFactAuthority(entry, "forced_reply") &&
          !boundedNonUniqueReplyLossSurface
      )("local_fact_forced_reply"),
      Option.when(
        entry.plannerSelectedOwnerKind.contains("ForcingDefense") &&
          entry.plannerSelectedSource.exists(normalized(_) == "only_move_defense") &&
          unboundedOnlyMoveSurface
      )("planner_only_move_forcing_defense"),
      Option.when(entry.contrast_source_kind.exists(normalized(_) == "delayed_only_move") && unboundedOnlyMoveSurface)(
        "contrast_delayed_only_move"
      ),
      Option.when(entry.commentary.contains("Only the played move still keeps the position together now"))(
        "only_move_phrase_secondary"
      )
    ).flatten

  private def boundedOnlyMoveLineConsequenceSurface(entry: MoveReviewOutputEntry): Boolean =
    val refs = normalizedEvidenceRefs(entry)
    entry.truthOnlyMoveDefense.contains(true) &&
      entry.truthBenchmarkCriticalMove.contains(true) &&
      MoveReviewFallbackMode.isPlannerOwned(entry.moveReviewFallbackMode) &&
      entry.moveReviewCausalClaimSupportEmbedded.contains(true) &&
      entry.moveReviewCausalClaimEvidenceSources.exists(source => normalized(source) == "line_consequence_surface") &&
      hasLocalFact(entry, "line_consequence", "pv_coupled_line", "line_consequence") &&
      refs.exists(_.startsWith("line_consequence_kind:")) &&
      refs.exists(_ == "evidence_line_binding:pv_coupled") &&
      !entry.commentary.contains("Only the played move still keeps the position together now")

  private def wrongFamilyReasons(entry: MoveReviewOutputEntry): List[String] =
    val lowerEvidence = normalizedEvidenceRefs(entry)
    val lineConsequencePrimaryDroppedWithoutOwner =
      entry.plannerSceneType.exists(scene => normalized(scene) == "line_consequence") &&
        entry.plannerDroppedOwners.exists { owner =>
          val low = normalized(owner)
          low.startsWith("lineconsequence:") &&
          low.contains("primaryallowed") &&
          low.contains("admission_missing_owner_candidate") &&
          low.contains("primary_admission_without_surviving_plan") &&
          low.contains("pv_line_consequence")
        }
    val lineConsequenceOwnsSurface =
      entry.plannerSelectedOwnerKind.exists(owner => normalized(owner) == "lineconsequence") ||
        entry.plannerSelectedSource.exists(source => normalized(source) == "line_consequence") ||
        hasLocalFactFamily(entry, "line_consequence") ||
        hasLocalFactProducer(entry, "line_consequence")
    val strategyPackKingAttackWithoutConcreteSurface =
      hasLocalFact(entry, "pressure", "certified_strategy", "certified_strategy_delta") &&
        lowerEvidence.exists(ref => ref == "typed_local_fact_source:practical_position_support") &&
        lowerEvidence.exists(ref => ref == "strategic_idea_kind:king_attack_build_up") &&
        !lowerEvidence.exists(ref => ref == "attack_lane_board_attack" || ref.startsWith("attack_lane_square:"))
    val lineConsequencePrimary = lineConsequencePrimarySurface(entry, lowerEvidence)
    val playedDest = Option(entry.playedUci).map(normalized).filter(_.length >= 4).map(_.substring(2, 4))
    val moverSelfDiscoveredAttackSurface =
      playedDest.exists(dest =>
        surfaceRows(entry).exists { row =>
          normalized(row.label) == "discovered attack" &&
            normalized(row.text).contains(s" from $dest ")
        }
      )
    val planSupportPrimary =
      entry.plannerSelectedSource.exists(source => normalized(source) == "pv_coupled_plan_support") &&
        hasLocalFact(entry, "plan_support", "pv_coupled_line", "certified_strategy_delta")
    val visibleSurfaceText =
      normalized(
        (
          entry.commentary ::
            (surfaceRowText(entry) ++ entry.plannerCandidateEvidenceLines)
        ).mkString(" ")
      )
    val passedPawnLineConsequenceSurface =
      planSupportPrimary &&
        visibleSurfaceText.contains("checked line") &&
        (
          visibleSurfaceText.contains("creates a passed pawn") ||
            visibleSurfaceText.contains("as a passed pawn") ||
            visibleSurfaceText.contains("passed-pawn cue") ||
            visibleSurfaceText.contains("concrete passer cue")
        ) &&
        !hasLocalFactFamily(entry, "line_consequence")
    val quietPawnRecoveryDemotedToDiscoveredAttack =
      entry.truthReasonFamily.contains("QuietTechnicalMove") &&
        entry.truthFailureMode.contains("NoClearPlan") &&
        !lineConsequenceOwnsSurface &&
        entry.plannerSelectedOwnerKind.exists(owner => normalized(owner) == "concretetactical") &&
        entry.plannerSelectedSource.exists(source => normalized(source) == "canonical_fact") &&
        hasLocalFact(entry, "threat", "canonical_fact", "tactical_motif") &&
        lowerEvidence.exists(_ == "typed_local_fact_source:canonical_fact") &&
        lowerEvidence.exists(_ == "typed_local_fact_producer:tactical_motif") &&
        lowerEvidence.exists(_ == "tactical_kind:discovered_attack") &&
        lowerEvidence.exists(_ == "motif_owner:current_move") &&
        visibleSurfaceText.contains("discovered attack") &&
        visibleSurfaceText.contains("checked line") &&
        visibleSurfaceText.contains("pawn")
    List(
      Option.when(
        entry.truthClass.contains("Best") &&
          entry.truthReasonFamily.contains("TacticalRefutation") &&
          !entry.truthBenchmarkCriticalMove.contains(true)
      )("best_noncritical_tactical_refutation"),
      Option.when(
        entry.truthReasonFamily.contains("OnlyMoveDefense") &&
          !entry.truthBenchmarkCriticalMove.contains(true) &&
          !hasForcedOrAlternativeEvidence(entry)
      )("only_move_defense_without_verified_uniqueness"),
      Option.when(
        hasLocalFactFamily(entry, "timing") &&
          hasLocalFactAuthority(entry, "forced_reply") &&
          !lowerEvidence.exists(ref => ref.contains("proof_") || ref.contains("typed_local_fact") || ref.contains("branch_line"))
      )("timing_family_forced_reply_without_exact_evidence"),
      Option.when(lineConsequencePrimaryDroppedWithoutOwner && !lineConsequenceOwnsSurface)(
        "line_consequence_primary_dropped_to_other_family"
      ),
      Option.when(strategyPackKingAttackWithoutConcreteSurface)(
        "strategy_pack_king_attack_without_concrete_surface"
      ),
      Option.when(lineConsequencePrimary && moverSelfDiscoveredAttackSurface)(
        "line_consequence_primary_diluted_by_mover_self_discovered_attack"
      ),
      Option.when(passedPawnLineConsequenceSurface)(
        "plan_support_surface_diluted_by_passed_pawn_line_consequence"
      ),
      Option.when(quietPawnRecoveryDemotedToDiscoveredAttack)(
        "quiet_pawn_recovery_demoted_to_discovered_attack_family"
      )
    ).flatten

  private def wrongTierReasons(entry: MoveReviewOutputEntry): List[String] =
    val lowerEvidence = normalizedEvidenceRefs(entry)
    val lowerLocalFactGuardrails = entry.moveReviewCausalClaimLocalFactGuardrails.map(normalized)
    val canonicalRelativePinPrimary =
      hasLocalFact(entry, "threat", "canonical_fact", "tactical_motif") &&
        lowerEvidence.exists(_ == "tactical_kind:pin") &&
        lowerEvidence.exists(_ == "motif_owner:current_move") &&
        lowerEvidence.exists(_.startsWith("behind_role:")) &&
        !lowerEvidence.exists(_ == "behind_role:king")
    val xraySupportSurface =
      surfaceRows(entry).exists { row =>
        val label = normalized(row.label)
        val text = normalized(row.text)
        label == "x-ray pressure" || text.contains("x-ray pressure")
      }
    val canonicalTacticalPrimary =
      entry.plannerSelectedOwnerKind.contains("ConcreteTactical") &&
        entry.plannerSelectedSource.exists(source => normalized(source) == "canonical_fact") &&
        hasLocalFact(entry, "threat", "canonical_fact", "tactical_motif") &&
        lowerEvidence.exists(_ == "typed_local_fact_source:canonical_fact") &&
        lowerEvidence.exists(_ == "typed_local_fact_producer:tactical_motif") &&
        lowerEvidence.exists(_.startsWith("tactical_kind:"))
    val canonicalTargetPressurePrimary =
      entry.plannerSelectedSource.exists(source => normalized(source) == "canonical_fact") &&
        hasLocalFact(entry, "pressure", "canonical_fact", "target_pressure") &&
        lowerEvidence.exists(_ == "typed_local_fact_source:canonical_fact") &&
        lowerEvidence.exists(_ == "typed_local_fact_producer:target_pressure") &&
        lowerEvidence.exists(ref => ref == "fact_kind:target_piece" || ref == "fact_kind:hanging_piece") &&
        lowerEvidence.exists(_.startsWith("fact_square:")) &&
        lowerLocalFactGuardrails.exists(guardrail =>
          guardrail == "target_fact_attacked_by_played_move" ||
            guardrail == "local_fact_guardrail:target_fact_attacked_by_played_move"
        )
    val relationWitnessPrimary =
      entry.plannerSelectedOwnerKind.contains("ConcreteTactical") &&
        entry.plannerSelectedSource.exists(source => normalized(source) == "relation_witness") &&
        hasLocalFact(entry, "threat", "pv_coupled_line", "relation_witness") &&
        lowerEvidence.exists(_ == "typed_local_fact_source:relation_witness") &&
        lowerEvidence.exists(_ == "typed_local_fact_producer:relation_witness") &&
        lowerEvidence.exists(_ == "evidence_line_binding:pv_coupled") &&
        lowerEvidence.exists(_.startsWith("relation_kind:")) &&
        (
          lowerEvidence.exists(_.endsWith("_relation_witness")) ||
            lowerLocalFactGuardrails.exists(guardrail =>
              guardrail == "relation_witness_typed_details" ||
                guardrail == "local_fact_guardrail:relation_witness_typed_details"
            )
        )
    val lineConsequencePrimary = lineConsequencePrimarySurface(entry, lowerEvidence)
    val alternativeComparisonPrimary =
      entry.plannerSelectedOwnerKind.exists(owner => normalized(owner) == "alternativecomparison") &&
        entry.plannerSelectedSource.exists(source => normalized(source) == "decision_comparison") &&
        hasLocalFact(entry, "line_consequence", "alternative_comparison", "alternative_comparison") &&
        lowerEvidence.exists(_ == "evidence_source:role_aware_line_consequence") &&
        lowerEvidence.exists(_ == "comparison_source:role_aware_line_consequence") &&
        lowerEvidence.exists(_ == "branch_role:engine_best") &&
        lowerEvidence.exists(_ == "branch_role:played") &&
        lowerEvidence.exists(_.startsWith("engine_best:line_consequence_kind:")) &&
        lowerEvidence.exists(_.startsWith("played:line_consequence_kind:"))
    val boundedNonUniqueReplyLossPrimary =
      entry.plannerSelectedOwnerKind.exists(owner => normalized(owner) == "forcingdefense") &&
        entry.plannerSelectedSource.exists(source => normalized(source) == "threat") &&
        !entry.truthOnlyMoveDefense.contains(true) &&
        hasLocalFact(entry, "defense", "forced_reply", "forced_reply") &&
        lowerEvidence.exists(_ == "evidence_source:explicit_reply_loss") &&
        lowerEvidence.exists(ref =>
          ref
            .stripPrefix("reply_defense_count:")
            .toIntOption
            .exists(_ > 1)
        ) &&
        lowerEvidence.exists(_.startsWith("loss_if_ignored_cp:")) &&
        lowerEvidence.exists(_.startsWith("threat_kind:")) &&
        lowerEvidence.exists(_.startsWith("threat_square:")) &&
        lowerLocalFactGuardrails.exists(_.endsWith("forced_reply_non_unique")) &&
        lowerLocalFactGuardrails.exists(_.endsWith("surface_forced=false"))
    val planSupportPrimary =
      entry.plannerSelectedOwnerKind.exists(owner => normalized(owner) == "movedelta") &&
        entry.plannerSelectedSource.exists(source => normalized(source) == "pv_coupled_plan_support") &&
        hasLocalFact(entry, "plan_support", "pv_coupled_line", "certified_strategy_delta") &&
        lowerEvidence.exists(_ == "evidence_source:pv_coupled_plan_support") &&
        lowerEvidence.exists(_ == "evidence_line_binding:pv_coupled") &&
        lowerEvidence.exists(_.startsWith("plan_name:")) &&
        lowerEvidence.exists(_ == "branch_line_meaningful:true") &&
        lowerEvidence.exists(_ == "plan_anchor_matched:true") &&
        lowerEvidence.exists(_.startsWith("branch_line_first_san:")) &&
        lowerEvidence.exists(_.startsWith("plan_anchor_matched_token:"))
    val lineOccupationPrimary =
      entry.plannerSelectedOwnerKind.exists(owner => normalized(owner) == "movedelta") &&
        entry.plannerSelectedSource.exists(source => normalized(source) == "practical_position_support") &&
        hasLocalFact(entry, "pressure", "certified_strategy", "certified_strategy_delta") &&
        lowerEvidence.exists(_ == "typed_local_fact_source:practical_position_support") &&
        lowerEvidence.exists(_ == "typed_local_fact_producer:certified_strategy_delta") &&
        lowerEvidence.exists(_ == "evidence_line_binding:pv_coupled") &&
        lowerEvidence.exists(_ == "strategic_idea_kind:line_occupation") &&
        lowerEvidence.exists(_.startsWith("anchor:")) &&
        lowerEvidence.exists(_.startsWith("line_occupation_file:")) &&
        lowerEvidence.exists(ref => ref == "line_occupation_status:open" || ref == "line_occupation_status:semi_open")
    val forkEntryDefensePrimary =
      entry.plannerSelectedOwnerKind.exists(owner => normalized(owner) == "forcingdefense") &&
        entry.plannerSelectedSource.exists(source => normalized(source) == "canonical_fact") &&
        hasLocalFact(entry, "defense", "canonical_fact", "fork_entry_defense") &&
        lowerEvidence.exists(_ == "typed_local_fact_source:canonical_fact") &&
        lowerEvidence.exists(_ == "typed_local_fact_producer:fork_entry_defense") &&
        lowerEvidence.exists(_ == "evidence_line_binding:pv_coupled") &&
        lowerEvidence.exists(_.startsWith("fork_entry_square:")) &&
        lowerEvidence.exists(_.startsWith("fork_attacker:")) &&
        lowerEvidence.exists(_.startsWith("fork_defender_square:")) &&
        lowerEvidence.exists(_.startsWith("fork_target:")) &&
        lowerLocalFactGuardrails.exists(guardrail =>
          guardrail == "fork_entry_square_defended_by_played_move" ||
            guardrail == "local_fact_guardrail:fork_entry_square_defended_by_played_move"
        )
    val broadPracticalAdvancedSurface =
      entry.advancedRows.exists(row => normalized(row.label).startsWith("practical "))
    val weakerLineConsequenceRelationSurface =
      surfaceRows(entry).exists { row =>
        val label = normalized(row.label)
        label == "tactical relation" || label == "line relation"
      }
    val weakerRelationSurface =
      surfaceRows(entry).exists { row =>
        val label = normalized(row.label)
        val text = normalized(row.text)
        label == "x-ray pressure" ||
          label == "line relation" ||
          label.startsWith("practical ") ||
          text.contains("x-ray pressure") ||
          text.contains("pin geometry")
      }
    val unresolvedAuthorDefensePrompt =
      entry.advancedRows.exists { row =>
        val label = normalized(row.label)
        val text = normalized(row.text)
        label == "what must be stopped" &&
          text.contains("defensive task") &&
          text.contains("meet the threat") &&
        text.contains("if ignored")
      }
    val weakerLineOccupationSurface =
      surfaceRows(entry).exists { row =>
        val label = normalized(row.label)
        label == "tactical relation" ||
          label == "line relation" ||
          label.startsWith("practical ")
      }
    List(
      Option.when(
        entry.plannerSelectedOwnerKind.contains("ForcingDefense") &&
          entry.supportedLocalAdmittedFamilies.nonEmpty &&
          !hasForcedOrAlternativeEvidence(entry)
      )("supported_local_absorbed_by_forcing_owner"),
      Option.when(
        MoveReviewFallbackMode.isPlannerOwned(entry.moveReviewFallbackMode) &&
          hasLocalFactAuthority(entry, "forced_reply") &&
          entry.supportedLocalAdmittedFamilies.nonEmpty
      )("forced_reply_planner_owned_over_supported_local"),
      Option.when(canonicalRelativePinPrimary && xraySupportSurface)(
        "canonical_pin_primary_diluted_by_xray_support"
      ),
      Option.when(canonicalTacticalPrimary && broadPracticalAdvancedSurface)(
        "canonical_tactical_primary_diluted_by_broad_practical_advanced"
      ),
      Option.when(canonicalTargetPressurePrimary && broadPracticalAdvancedSurface)(
        "canonical_target_pressure_primary_diluted_by_broad_practical_advanced"
      ),
      Option.when(relationWitnessPrimary && weakerRelationSurface)(
        "relation_witness_primary_diluted_by_weaker_relation_surface"
      ),
      Option.when(relationWitnessPrimary && unresolvedAuthorDefensePrompt)(
        "relation_witness_primary_diluted_by_unresolved_author_defense_prompt"
      ),
      Option.when(lineOccupationPrimary && weakerLineOccupationSurface)(
        "line_occupation_primary_diluted_by_weaker_relation_or_practical_surface"
      ),
      Option.when(lineConsequencePrimary && broadPracticalAdvancedSurface)(
        "line_consequence_primary_diluted_by_broad_practical_surface"
      ),
      Option.when(lineConsequencePrimary && weakerLineConsequenceRelationSurface)(
        "line_consequence_primary_diluted_by_weaker_relation_surface"
      ),
      Option.when(alternativeComparisonPrimary && broadPracticalAdvancedSurface)(
        "alternative_comparison_primary_diluted_by_broad_practical_surface"
      ),
      Option.when(boundedNonUniqueReplyLossPrimary && broadPracticalAdvancedSurface)(
        "bounded_non_unique_reply_loss_primary_diluted_by_broad_practical_surface"
      ),
      Option.when(planSupportPrimary && broadPracticalAdvancedSurface)(
        "plan_support_primary_diluted_by_broad_practical_surface"
      ),
      Option.when(forkEntryDefensePrimary && broadPracticalAdvancedSurface)(
        "fork_entry_defense_primary_diluted_by_broad_practical_surface"
      )
    ).flatten

  private def genericFallbackRisk(entry: MoveReviewOutputEntry): Boolean =
    val low = normalized(entry.commentary)
    MoveReviewFallbackMode.isExactFactual(entry.moveReviewFallbackMode) &&
      (
        low.contains(": this moves ") ||
          low.contains(": this puts ") ||
          low.matches(""".*: this moves the [a-z]+ from [a-h][1-8] to [a-h][1-8]\.?(\s+the checked line begins.*)?""")
      ) &&
      !hasExactEvidenceRefs(entry)

  private def criticalTruth(entry: MoveReviewOutputEntry): Boolean =
    entry.truthClass.exists { value =>
      Set("blunder", "mistake", "inaccuracy", "missedwin").contains(normalizedKey(value))
    }

  private def criticalSurfaceEvidenceAvailable(entry: MoveReviewOutputEntry): Boolean =
    hasExactEvidenceRefs(entry) ||
      entry.truthReasonFamily.exists(_.trim.nonEmpty) ||
      entry.truthFailureMode.exists(_.trim.nonEmpty) ||
      entry.moveReviewLocalFactEvidenceRefs.nonEmpty ||
      entry.moveReviewCausalClaimEvidenceSources.nonEmpty ||
      entry.moveReviewCausalClaimLocalFactEvidenceRefs.nonEmpty ||
      entry.plannerConcreteTacticalSources.nonEmpty ||
      entry.plannerLineConsequenceSources.nonEmpty ||
      entry.plannerAlternativeComparisonSources.nonEmpty ||
      entry.plannerMoveDeltaSources.nonEmpty

  private def criticalSurfaceCuePresent(entry: MoveReviewOutputEntry): Boolean =
    val rowText =
      normalized(
        surfaceRowText(entry).mkString(" ")
      )
    val allText = normalized((entry.commentary :: List(rowText)).mkString(" "))
    val hasBadMoveTerm =
      List("blunder", "mistake", "inaccuracy", "missed win", "misses a win").exists(allText.contains)
    val hasCriticalConsequence =
      List("loses", "drops", "allows", "gives up", "concession", "refutation", "punish", "worse").exists(
        allText.contains
      )
    val hasDecisionGap =
      rowText.contains("gap ") &&
        (rowText.contains("played ") || rowText.contains("engine looked at ") || rowText.contains("compared "))
    hasBadMoveTerm || hasCriticalConsequence || hasDecisionGap

  private def criticalSurfaceOverstrongRisk(entry: MoveReviewOutputEntry): Boolean =
    val truth = entry.truthClass.map(normalizedKey)
    val text = normalized((entry.commentary :: surfaceRows(entry).map(_.text)).mkString(" "))
    truth match
      case Some("inaccuracy") =>
        text.contains("blunder") ||
          text.contains("missed win") ||
          text.contains("misses a win") ||
          text.contains("clear mistake") ||
          text.contains("serious mistake")
      case Some("mistake") =>
        text.contains("blunder") ||
          text.contains("missed win") ||
          text.contains("misses a win")
      case _ => false

  private def exactEvidenceSurfacePresent(entry: MoveReviewOutputEntry, genericFallback: Boolean): Boolean =
    val hasSurfaceRow =
      surfaceRows(entry).exists(row => row.text.trim.nonEmpty)
    hasSurfaceRow && hasExactEvidenceRefs(entry) && !genericFallback

  private def hasExactEvidenceRefs(entry: MoveReviewOutputEntry): Boolean =
    val canonicalEndgameFact =
      hasLocalFact(entry, "endgame", "canonical_fact", "endgame_fact")
    val refs = normalizedEvidenceRefs(entry)
    val pvCoupledCaptureFact =
      hasLocalFact(entry, "capture", "pv_coupled_line", "capture_sequence") &&
        refs.exists(_ == "capture_motif") &&
        refs.exists(_.startsWith("captured_square:")) &&
        refs.exists(_.startsWith("captured_role:"))
    pvCoupledCaptureFact ||
      refs.exists { low =>
        low.startsWith("proof_family:") ||
        low.startsWith("proof_source:") ||
        low.startsWith("typed_local_fact_source:") ||
        low == "branch_line" ||
        low == "typed_local_fact" ||
        low.startsWith("evidence_line_binding:") ||
        (canonicalEndgameFact && low.startsWith("endgame_fact:"))
      }

  private def hasForcedOrAlternativeEvidence(entry: MoveReviewOutputEntry): Boolean =
    entry.truthBenchmarkCriticalMove.contains(true) ||
      evidenceRefs(entry).exists { ref =>
        val low = normalized(ref)
        low.contains("forced") ||
        low.contains("only_move") ||
        low.contains("alternative") ||
        low.contains("role_aware")
      } ||
      entry.contrast_source_kind.exists(source => normalized(source).contains("only_move"))

  private def hasLocalFact(entry: MoveReviewOutputEntry, family: String, authority: String, producer: String): Boolean =
    hasLocalFactFamily(entry, family) &&
      hasLocalFactAuthority(entry, authority) &&
      hasLocalFactProducer(entry, producer)

  private def hasLocalFactFamily(entry: MoveReviewOutputEntry, family: String): Boolean =
    entry.moveReviewLocalFactFamilies.exists(value => normalized(value) == family)

  private def hasLocalFactAuthority(entry: MoveReviewOutputEntry, authority: String): Boolean =
    entry.moveReviewLocalFactAuthorities.exists(value => normalized(value) == authority)

  private def hasLocalFactProducer(entry: MoveReviewOutputEntry, producer: String): Boolean =
    entry.moveReviewLocalFactProducers.exists(value => normalized(value) == producer)

  private def lineConsequencePrimarySurface(entry: MoveReviewOutputEntry, lowerEvidence: List[String]): Boolean =
    hasLocalFact(entry, "line_consequence", "pv_coupled_line", "line_consequence") &&
      lowerEvidence.exists(_ == "evidence_source:line_consequence_surface") &&
      lowerEvidence.exists(_ == "evidence_line_binding:pv_coupled") &&
      lowerEvidence.exists(_ == "line_consequence_release:surface_candidate") &&
      lowerEvidence.exists(_.startsWith("line_consequence_kind:")) &&
      !lowerEvidence.exists(_ == "line_consequence_kind:preview_only")

  private def evidenceRefs(entry: MoveReviewOutputEntry): List[String] =
    entry.moveReviewLocalFactEvidenceRefs ++
      entry.moveReviewCausalClaimEvidenceSources ++
      entry.moveReviewCausalClaimLocalFactEvidenceRefs ++
      entry.plannerMoveDeltaSources ++
      entry.plannerForcingDefenseSources ++
      entry.plannerAlternativeComparisonSources

  private def normalizedEvidenceRefs(entry: MoveReviewOutputEntry): List[String] =
    evidenceRefs(entry).map(normalized)

  private def moveReviewSurfaceText(entry: MoveReviewOutputEntry, separator: String): String =
    (entry.commentary :: surfaceRowText(entry)).mkString(separator)

  private def surfaceRowText(entry: MoveReviewOutputEntry): List[String] =
    surfaceRows(entry).map(row => s"${row.label}: ${row.text}")

  private def surfaceRows(entry: MoveReviewOutputEntry): List[SupportRow] =
    entry.supportRows ++ entry.advancedRows

  private def blankLike(text: String): Boolean =
    val cleaned = Option(text).getOrElse("").replaceAll("""\s+""", " ").trim
    val words = cleaned.split("\\s+").count(_.nonEmpty)
    words <= 11 &&
      !lila.commentary.analysis.LineScopedCitation.hasInlineCitation(cleaned) &&
      !lila.commentary.analysis.LiveNarrativeCompressionCore.hasConcreteAnchor(cleaned)

  private def averageDelta(rows: List[ContrastEvalRow])(f: ContrastEvalRow => Int): Double =
    if rows.isEmpty then 0.0
    else rows.map(f).sum.toDouble / rows.size.toDouble

  private def countValues(values: List[String]): Map[String, Int] =
    values.groupMapReduce(identity)(_ => 1)(_ + _).toList.sortBy(_._1).toMap

  private def renderCountMap(values: Map[String, Int]): String =
    if values.isEmpty then "{}"
    else values.toList.sortBy { case (key, _) => key }.map { case (key, count) => s"`$key=$count`" }.mkString("{ ", ", ", " }")

  private def formatDouble(value: Double): String =
    f"$value%.2f"

  private def normalized(raw: String): String =
    Option(raw).getOrElse("").trim.toLowerCase

  private def normalizedKey(raw: String): String =
    normalized(raw).replace("_", "").replace("-", "").replace(" ", "")
