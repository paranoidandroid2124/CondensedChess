package lila.llm.tools.quality

import play.api.libs.json.{ Format, Json }
import lila.llm.tools.review.{ ChronicleActivePlannerSliceRunner, CommentaryPlayerQcSupport }

object CommentaryQualityContrastSupport:

  import CommentaryPlayerQcSupport.*
  import CommentaryQualitySupport.*

  object ContrastSchema:
    val SelectorVersion = "commentary_quality_contrast_selector.v1"
    val EvalVersion = "commentary_quality_contrast_eval.v1"
    val SummaryVersion = "commentary_quality_contrast_summary.v1"

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

  final case class ContrastSelectorRow(
      schemaVersion: String = ContrastSchema.SelectorVersion,
      sampleId: String,
      gameKey: String,
      surface: String,
      sliceKind: String,
      targetPly: Int,
      playedSan: String,
      plannerQuestion: Option[String],
      plannerOwnerFamily: Option[String],
      bookmakerFallbackMode: String,
      afterBookmakerFallbackMode: Option[String] = None,
      upstreamTaxonomy: Option[String],
      upstreamAllowedByDesign: Boolean,
      upstreamAllowanceTag: Option[String],
      bookmakerPairBlockers: List[String] = Nil,
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
      plannerOwnerFamily: Option[String],
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
      bookmakerPairBlockerCounts: Map[String, Int],
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
      summary: ContrastSummary
  )

  def buildContrastReport(
      beforeEntries: List[BookmakerOutputEntry],
      afterEntries: List[BookmakerOutputEntry],
      surfaceEntries: List[ChronicleActivePlannerSliceRunner.SliceSurfaceEntry],
      parityReport: SamePlyParityReport
  ): Either[String, ContrastReport] =
    val beforeById = beforeEntries.map(entry => entry.sampleId -> entry).toMap
    val afterById = afterEntries.map(entry => entry.sampleId -> entry).toMap
    val missingAfter = beforeEntries.filterNot(entry => afterById.contains(entry.sampleId)).map(_.sampleId)

    if missingAfter.nonEmpty then Left(s"missing contrast after rows: ${missingAfter.mkString(", ")}")
    else
      val parityIndex = parityReport.rows.map(row => parityKey(row.gameKey, row.targetPly, row.sliceKind) -> row).toMap
      val beforeThresholdBySampleId =
        buildBookmakerThresholdRows(beforeEntries, surfaceEntries, parityReport).map(row => row.sampleId -> row).toMap
      val afterThresholdBySampleId =
        buildBookmakerThresholdRows(afterEntries, surfaceEntries, parityReport).map(row => row.sampleId -> row).toMap
      val surfaceThresholdReport =
        buildSurfaceThresholdReport(beforeEntries, surfaceEntries, parityReport)
      val whyRows =
        beforeEntries.filter(entry =>
          entry.plannerPrimaryKind.contains("WhyThis") || entry.plannerPrimaryKind.contains("WhyNow")
        )

      val selectorRows = whyRows.map { before =>
        val after = afterById(before.sampleId)
        val parityRow = parityIndex.get(parityKey(before.gameKey, before.targetPly, before.sliceKind))
        val upstreamTaxonomy = parityRow.map(_.primaryTaxonomy)
        val allowanceTag = rowSurfaceOnlyAllowanceTag(parityRow)
        val allowedByDesign = rowAllowedByDesign(parityRow)
        val upstreamPairBlockers = bookmakerPairBlockers(parityRow, MismatchLayer.Upstream)
        val replayPairBlockers = bookmakerPairBlockers(parityRow, MismatchLayer.Replay)
        val pairBlockers = upstreamPairBlockers ++ replayPairBlockers
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
          else if upstreamPairBlockers.nonEmpty then SelectionStatus.UpstreamBlocked
          else if replayPairBlockers.nonEmpty then SelectionStatus.ReplayBlocked
          else if passedBaselineKeepGate && after.bookmakerFallbackMode == "exact_factual" then
            SelectionStatus.AfterFallbackBlocked
          else if passedBaselineKeepGate then SelectionStatus.Eligible
          else SelectionStatus.BaselineBlocked
        val selectionReason =
          selectionStatus match
            case SelectionStatus.Eligible         => "upstream_allowed_and_baseline_keep_gate_passed"
            case SelectionStatus.UpstreamBlocked  => pairBlockers.headOption.getOrElse(upstreamTaxonomy.getOrElse(MismatchTaxonomy.UpstreamLayerMismatch))
            case SelectionStatus.ReplayBlocked    => replayPairBlockers.headOption.getOrElse(MismatchTaxonomy.ReplayLayerRewrite)
            case SelectionStatus.AfterFallbackBlocked =>
              "after_bookmaker_exact_factual"
            case SelectionStatus.BaselineBlocked  => "baseline_keep_gate_failed"
            case SelectionStatus.QuestionFiltered => "question_outside_scope"
            case SelectionStatus.NoPrimary        => "no_planner_primary"

        ContrastSelectorRow(
          sampleId = before.sampleId,
          gameKey = before.gameKey,
          surface = SurfaceName.Bookmaker,
          sliceKind = before.sliceKind,
          targetPly = before.targetPly,
          playedSan = before.playedSan,
          plannerQuestion = before.plannerPrimaryKind,
          plannerOwnerFamily = before.plannerSelectedOwnerFamily,
          bookmakerFallbackMode = before.bookmakerFallbackMode,
          afterBookmakerFallbackMode = Some(after.bookmakerFallbackMode),
          upstreamTaxonomy = upstreamTaxonomy,
          upstreamAllowedByDesign = allowedByDesign,
          upstreamAllowanceTag = allowanceTag,
          bookmakerPairBlockers = pairBlockers,
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
        val evalRows =
          selectorRows
            .filter(row =>
              row.selectionStatus == SelectionStatus.Eligible || row.selectionStatus == SelectionStatus.BaselineBlocked
            )
            .map { row =>
              val beforeThreshold = beforeThresholdBySampleId(row.sampleId)
              val afterThreshold = afterThresholdBySampleId(row.sampleId)
              val afterEntry = afterById(row.sampleId)
              val finalVerdict =
                if row.selectionStatus == SelectionStatus.Eligible && afterEntry.contrast_admissible.contains(true) &&
                    afterThreshold.selection.thresholdVerdict == EvalVerdict.Keep
                then FinalVerdict.Keep
                else if row.selectionStatus == SelectionStatus.Eligible && afterEntry.contrast_admissible.contains(false)
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
                plannerOwnerFamily = row.plannerOwnerFamily,
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
        val chronicleSummary =
          surfaceThresholdReport.summary.surfaceSummaries.getOrElse(
            SurfaceName.Chronicle,
            SurfaceThresholdSurfaceSummary(0, 0, 0, 0, 0, 0, 0, 0, 0)
          )
        val activeSummary =
          surfaceThresholdReport.summary.surfaceSummaries.getOrElse(
            SurfaceName.Active,
            SurfaceThresholdSurfaceSummary(0, 0, 0, 0, 0, 0, 0, 0, 0)
          )

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
              evalSampleIds.count(sampleId => beforeById(sampleId).bookmakerFallbackMode == "exact_factual"),
            afterFallbackCount =
              evalSampleIds.count(sampleId => afterById(sampleId).bookmakerFallbackMode == "exact_factual"),
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
            bookmakerPairBlockerCounts = countValues(selectorRows.flatMap(_.bookmakerPairBlockers)),
            upstreamPairBlockerCounts =
              countValues(
                selectorRows
                  .filter(_.selectionStatus == SelectionStatus.UpstreamBlocked)
                  .flatMap(_.bookmakerPairBlockers)
              ),
            replayPairBlockerCounts =
              countValues(
                selectorRows
                  .filter(_.selectionStatus == SelectionStatus.ReplayBlocked)
                  .flatMap(_.bookmakerPairBlockers)
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
            crossSurfaceStabilityStatus =
              s"chronicle keep=${chronicleSummary.keepCount} review=${chronicleSummary.reviewCount} gateFail=${chronicleSummary.gateFailCount}; active keep=${activeSummary.keepCount} review=${activeSummary.reviewCount} gateFail=${activeSummary.gateFailCount}; verdict_disagreement=${surfaceThresholdReport.summary.crossSurfaceVerdictDisagreementCount}"
          )

        Right(ContrastReport(selectorRows = selectorRows.sortBy(_.sampleId), evalRows = evalRows.sortBy(_.sampleId), summary = summary))

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
       |- Bookmaker pair blockers: ${renderCountMap(report.summary.bookmakerPairBlockerCounts)}
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
        s"- `${row.sampleId}` question=`${row.plannerQuestion.getOrElse("none")}` owner=`${row.plannerOwnerFamily.getOrElse("-")}` status=`${row.selectionStatus}` reason=`${row.selectionReason}` pairBlockers=`${row.bookmakerPairBlockers.mkString("|")}` contrast=`${row.contrast_source_kind.getOrElse("-")}` reject=`${row.contrast_reject_reason.getOrElse("-")}` changed=`${row.changed}`"
      }.mkString("\n")

  private def renderEvalRows(rows: List[ContrastEvalRow]): String =
    if rows.isEmpty then "- none"
    else
      rows.map { row =>
        s"- `${row.sampleId}` verdict=`${row.finalVerdict}` rawNet `${row.beforeRubric.netScore} -> ${row.afterRubric.netScore}` selector `${row.beforeSelection.selectorScore} -> ${row.afterSelection.selectorScore}` contrast=`${row.contrast_source_kind.getOrElse("-")}` reject=`${row.contrast_reject_reason.getOrElse("-")}` before=`${row.beforeText}` after=`${row.afterText}`"
      }.mkString("\n")

  private def blankLike(text: String): Boolean =
    val cleaned = Option(text).getOrElse("").replaceAll("""\s+""", " ").trim
    val words = cleaned.split("\\s+").count(_.nonEmpty)
    words <= 11 &&
      !lila.llm.analysis.LineScopedCitation.hasInlineCitation(cleaned) &&
      !lila.llm.analysis.LiveNarrativeCompressionCore.hasConcreteAnchor(cleaned)

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

  private def bookmakerPairBlockers(
      parityRow: Option[SamePlyParityRow],
      layer: String
  ): List[String] =
    parityRow.toList.flatMap(_.pairwiseMismatches).collect {
      case mismatch
          if !mismatch.allowedByDesign &&
            mismatch.layer == layer &&
            (mismatch.leftSurface == SurfaceName.Bookmaker || mismatch.rightSurface == SurfaceName.Bookmaker) =>
        s"${mismatch.leftSurface}/${mismatch.rightSurface}:${mismatch.taxonomy}"
    }
