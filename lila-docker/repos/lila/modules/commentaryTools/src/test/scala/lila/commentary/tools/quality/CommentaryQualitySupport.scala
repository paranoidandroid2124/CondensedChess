package lila.commentary.tools.quality

import java.security.MessageDigest

import play.api.libs.json.{ Format, JsNull, JsObject, JsValue, Json, Writes }

import lila.commentary.*
import lila.commentary.analysis.DecisiveTruthContract
import lila.commentary.model.authoring.{ AuthorQuestion, QuestionEvidence }
import lila.commentary.tools.review.CommentaryPlayerQcSupport

object CommentaryQualitySupport:

  object SurfaceName:
    val MoveReview = "moveReview"

  object MismatchTaxonomy:
    val BundleMissing = "bundle_missing"
    val CarryMismatch = "carry_mismatch"
    val SnapshotSkew = "snapshot_skew"
    val SurfaceOnlyAugmentation = "surface_only_augmentation"
    val ReplayLayerRewrite = "replay_layer_rewrite"
    val UpstreamLayerMismatch = "upstream_layer_mismatch"

    val precedence = List(
      BundleMissing,
      SnapshotSkew,
      CarryMismatch,
      SurfaceOnlyAugmentation,
      ReplayLayerRewrite,
      UpstreamLayerMismatch
    )

  object MismatchLayer:
    val Upstream = "upstream"
    val Replay = "replay"

  object EvalSchema:
    val Version = "commentary_quality_eval.v1"
    val SummaryVersion = "commentary_quality_eval_summary.v1"
    val JudgePromptVersion = "commentary_quality_judge_prompt.v1"
    val SamePlyParityVersion = "commentary_quality_same_ply_parity.v1"
    val SurfaceThresholdVersion = "commentary_quality_surface_threshold.v1"
    val SurfaceThresholdSummaryVersion = "commentary_quality_surface_threshold_summary.v1"

  object EvalRubric:
    val Clarity = "clarity"
    val MoveAttributionCorrectness = "move_attribution_correctness"
    val ContrastUsefulness = "contrast_usefulness"
    val PracticalUsefulness = "practical_usefulness"
    val DryButTruePenalty = "dry_but_true_penalty"
    val OverclaimPenalty = "overclaim_penalty"

    val all = List(
      Clarity,
      MoveAttributionCorrectness,
      ContrastUsefulness,
      PracticalUsefulness,
      DryButTruePenalty,
      OverclaimPenalty
    )

  object EvalVerdict:
    val Keep = "keep"
    val Review = "review"

  object EvalThresholds:
    val MoveAttributionGateMin = 4
    val KeepSelectorMin = 12
    val MaxOverclaimPenaltyForKeep = 1

  object SurfaceOnlyAugmentationAllowance:
    val ReviewRequired = "review_required"

  final case class SurfaceDigestHashes(
      snapshotDigestHash: Option[String] = None,
      carryDigestHash: Option[String] = None,
      augmentationDigestHash: Option[String] = None,
      bundleDigestHash: Option[String] = None
  )
  object SurfaceDigestHashes:
    given Format[SurfaceDigestHashes] = Json.format[SurfaceDigestHashes]

  final case class SurfaceParitySnapshot(
      sampleId: String,
      gameKey: String,
      surface: String,
      sliceKind: String,
      targetPly: Int,
      playedSan: String,
      selectedQuestion: Option[String],
      selectedOwnerKind: Option[String],
      selectedSource: Option[String],
      replayOutcome: Option[String],
      digests: SurfaceDigestHashes
  )
  object SurfaceParitySnapshot:
    given Format[SurfaceParitySnapshot] = Json.format[SurfaceParitySnapshot]

  final case class PairwiseParityMismatch(
      leftSurface: String,
      rightSurface: String,
      taxonomy: String,
      layer: String,
      reasons: List[String],
      allowanceTag: Option[String] = None,
      allowedByDesign: Boolean = false
  )
  object PairwiseParityMismatch:
    given Format[PairwiseParityMismatch] = Json.format[PairwiseParityMismatch]

  final case class SamePlyParityRow(
      gameKey: String,
      targetPly: Int,
      sliceKind: String,
      playedSan: String,
      primaryTaxonomy: String,
      primaryLayer: String,
      pairwiseMismatches: List[PairwiseParityMismatch],
      surfaces: List[SurfaceParitySnapshot]
  )
  object SamePlyParityRow:
    given Format[SamePlyParityRow] = Json.format[SamePlyParityRow]

  final case class SamePlyParitySummary(
      groupedPlies: Int,
      mismatchedPlies: Int,
      taxonomyCounts: Map[String, Int],
      layerCounts: Map[String, Int],
      surfaceOnlyAugmentationAllowanceCounts: Map[String, Int] = Map.empty
  )
  object SamePlyParitySummary:
    given Format[SamePlyParitySummary] = Json.format[SamePlyParitySummary]

  final case class SamePlyParityReport(
      schemaVersion: String = EvalSchema.SamePlyParityVersion,
      summary: SamePlyParitySummary,
      rows: List[SamePlyParityRow]
  )
  object SamePlyParityReport:
    given Format[SamePlyParityReport] = Json.format[SamePlyParityReport]

  final case class EvaluationRubricScores(
      clarity: Int,
      moveAttributionCorrectness: Int,
      contrastUsefulness: Int,
      practicalUsefulness: Int,
      dryButTruePenalty: Int,
      overclaimPenalty: Int
  ):
    def netScore: Int =
      clarity + moveAttributionCorrectness + contrastUsefulness + practicalUsefulness - dryButTruePenalty - overclaimPenalty

    def deltas(other: EvaluationRubricScores): Map[String, Int] =
      Map(
        EvalRubric.Clarity -> (clarity - other.clarity),
        EvalRubric.MoveAttributionCorrectness -> (moveAttributionCorrectness - other.moveAttributionCorrectness),
        EvalRubric.ContrastUsefulness -> (contrastUsefulness - other.contrastUsefulness),
        EvalRubric.PracticalUsefulness -> (practicalUsefulness - other.practicalUsefulness),
        EvalRubric.DryButTruePenalty -> (dryButTruePenalty - other.dryButTruePenalty),
        EvalRubric.OverclaimPenalty -> (overclaimPenalty - other.overclaimPenalty)
      )
  object EvaluationRubricScores:
    given Format[EvaluationRubricScores] = Json.format[EvaluationRubricScores]

  final case class EvaluationSelection(
      rawNetScore: Int,
      selectorScore: Int,
      moveAttributionGatePassed: Boolean,
      usefulnessCredit: Int,
      usefulnessRewardBlocked: Boolean,
      thresholdVerdict: String
  )
  object EvaluationSelection:
    given Format[EvaluationSelection] = Json.format[EvaluationSelection]

  final case class CommentaryQualityEvalRecord(
      schemaVersion: String = EvalSchema.Version,
      comparisonKey: String,
      sampleId: String,
      gameKey: String,
      surface: String,
      sliceKind: String,
      targetPly: Int,
      candidateLabel: String,
      truthAnchor: String,
      candidateText: String,
      upstreamTaxonomy: Option[String],
      bundleDigestHash: Option[String],
      rubric: EvaluationRubricScores,
      selection: EvaluationSelection,
      overallVerdict: String,
      summary: String,
      evidenceNotes: List[String] = Nil,
      flags: List[String] = Nil
  )
  object CommentaryQualityEvalRecord:
    given Format[CommentaryQualityEvalRecord] = Json.format[CommentaryQualityEvalRecord]

  final case class CommentaryQualityEvalSummary(
      schemaVersion: String = EvalSchema.SummaryVersion,
      comparisonKey: String,
      sampleId: String,
      gameKey: String,
      surface: String,
      sliceKind: String,
      targetPly: Int,
      beforeLabel: String,
      afterLabel: String,
      beforeNetScore: Int,
      afterNetScore: Int,
      beforeSelectorScore: Int,
      afterSelectorScore: Int,
      beforeUsefulnessCredit: Int,
      afterUsefulnessCredit: Int,
      beforeAttributionGatePassed: Boolean,
      afterAttributionGatePassed: Boolean,
      beforeThresholdVerdict: String,
      afterThresholdVerdict: String,
      netDelta: Int,
      rubricDeltas: Map[String, Int],
      improved: Boolean,
      headline: String
  )
  object CommentaryQualityEvalSummary:
    given Format[CommentaryQualityEvalSummary] = Json.format[CommentaryQualityEvalSummary]

  final case class SurfaceThresholdRow(
      schemaVersion: String = EvalSchema.SurfaceThresholdVersion,
      sampleId: String,
      gameKey: String,
      surface: String,
      sliceKind: String,
      targetPly: Int,
      playedSan: String,
      candidateText: String,
      surfaceOutcome: String,
      plannerQuestion: Option[String],
      plannerProofFamily: Option[String],
      plannerProofSource: Option[String],
      surfaceQuestion: Option[String],
      surfaceProofFamily: Option[String],
      surfaceProofSource: Option[String],
      upstreamTaxonomy: Option[String],
      upstreamAllowedByDesign: Boolean,
      upstreamAllowanceTag: Option[String],
      plannerQuestionAligned: Boolean,
      plannerOwnerAligned: Boolean,
      crossSurfaceQuestionDisagreement: Boolean,
      crossSurfaceOwnerDisagreement: Boolean,
      blankLike: Boolean,
      rubric: EvaluationRubricScores,
      selection: EvaluationSelection,
      evidenceNotes: List[String] = Nil,
      flags: List[String] = Nil
  )
  object SurfaceThresholdRow:
    given Format[SurfaceThresholdRow] = Json.format[SurfaceThresholdRow]

  final case class SurfaceThresholdSurfaceSummary(
      totalRows: Int,
      keepCount: Int,
      reviewCount: Int,
      gateFailCount: Int,
      usefulnessBlockedCount: Int,
      disagreementCount: Int,
      blankLikeCount: Int,
      plannerOwnedLikeCount: Int,
      fallbackLikeCount: Int
  )
  object SurfaceThresholdSurfaceSummary:
    given Format[SurfaceThresholdSurfaceSummary] = Json.format[SurfaceThresholdSurfaceSummary]

  final case class SurfaceThresholdSummary(
      schemaVersion: String = EvalSchema.SurfaceThresholdSummaryVersion,
      totalRows: Int,
      surfaceSummaries: Map[String, SurfaceThresholdSurfaceSummary],
      crossSurfaceVerdictDisagreementCount: Int,
      crossSurfaceGateDisagreementCount: Int,
      crossSurfaceQuestionDisagreementCount: Int,
      crossSurfaceOwnerDisagreementCount: Int
  )
  object SurfaceThresholdSummary:
    given Format[SurfaceThresholdSummary] = Json.format[SurfaceThresholdSummary]

  final case class SurfaceThresholdReport(
      summary: SurfaceThresholdSummary,
      rows: List[SurfaceThresholdRow]
  )
  object SurfaceThresholdReport:
    given Format[SurfaceThresholdReport] = Json.format[SurfaceThresholdReport]

  private final case class RealEvalSeedSpec(
      comparisonKey: String,
      sampleId: String,
      truthAnchor: String,
      afterText: String,
      beforeRubric: EvaluationRubricScores,
      afterRubric: EvaluationRubricScores,
      beforeSummary: String,
      afterSummary: String,
      beforeEvidenceNotes: List[String] = Nil,
      afterEvidenceNotes: List[String] = Nil,
      beforeFlags: List[String] = Nil,
      afterFlags: List[String] = Nil
  )

  def moveReviewDigests(
      snapshot: CommentaryPlayerQcSupport.SliceSnapshot
  ): SurfaceDigestHashes =
    sharedReplayDigests(snapshot)
  private def sharedReplayDigests(
      snapshot: CommentaryPlayerQcSupport.SliceSnapshot,
      augmentationJson: JsValue = JsNull
  ): SurfaceDigestHashes =
    val snapshotJson =
      Json.obj(
        "fen" -> snapshot.plyData.fen,
        "ply" -> snapshot.plyData.ply,
        "phase" -> snapshot.phase,
        "evalBeforeCp" -> snapshot.evalBeforeCp,
        "evalAfterCp" -> snapshot.evalAfterCp,
        "signalDigest" -> jsonOrNull(snapshot.signalDigest),
        "strategyPack" -> jsonOrNull(snapshot.strategyPack),
        "truthContract" -> truthContractJson(snapshot.truthContract)
      )
    val carryJson =
      Json.obj(
        "authorQuestions" -> snapshot.ctx.authorQuestions.map(authorQuestionJson),
        "authorEvidence" -> snapshot.ctx.authorEvidence.map(questionEvidenceJson),
        "mainStrategicPlans" -> snapshot.ctx.mainStrategicPlans.map(planHypothesisJson),
        "opponentPlan" -> snapshot.ctx.opponentPlan.map(_.name),
        "openingRelationClaim" -> snapshot.signalDigest.flatMap(_.openingRelationClaim),
        "endgameTransitionClaim" -> snapshot.signalDigest.flatMap(_.endgameTransitionClaim)
      )
    digestSet(snapshotJson, carryJson, augmentationJson)
  def moveReviewParitySnapshot(
      entry: CommentaryPlayerQcSupport.MoveReviewOutputEntry
  ): SurfaceParitySnapshot =
    SurfaceParitySnapshot(
      sampleId = entry.sampleId,
      gameKey = entry.gameKey,
      surface = SurfaceName.MoveReview,
      sliceKind = entry.sliceKind,
      targetPly = entry.targetPly,
      playedSan = entry.playedSan,
      selectedQuestion = entry.plannerSelectedQuestion,
      selectedOwnerKind = entry.plannerSelectedOwnerKind,
      selectedSource = entry.plannerSelectedSource,
      replayOutcome = entry.surfaceReplayOutcome,
      digests =
        SurfaceDigestHashes(
          snapshotDigestHash = entry.moveReviewSnapshotDigestHash,
          carryDigestHash = entry.moveReviewCarryDigestHash,
          augmentationDigestHash = entry.moveReviewAugmentationDigestHash,
          bundleDigestHash = entry.moveReviewBundleDigestHash
        )
    )



  def buildSamePlyParityReport(
      surfaces: List[SurfaceParitySnapshot]
  ): SamePlyParityReport =
    val rows =
      surfaces
        .groupBy(surface => s"${surface.gameKey}:${surface.targetPly}:${surface.sliceKind}")
        .values
        .toList
        .flatMap { group =>
          val sorted = group.sortBy(_.surface)
          val mismatches = pairwise(sorted)
          Option.when(mismatches.nonEmpty) {
            val ordered = mismatches.sortBy(mismatch => mismatchRank(mismatch.taxonomy))
            val head = selectPrimaryMismatch(ordered)
            SamePlyParityRow(
              gameKey = sorted.head.gameKey,
              targetPly = sorted.head.targetPly,
              sliceKind = sorted.head.sliceKind,
              playedSan = sorted.head.playedSan,
              primaryTaxonomy = head.taxonomy,
              primaryLayer = head.layer,
              pairwiseMismatches = ordered,
              surfaces = sorted
            )
          }
        }
        .sortBy(row => (row.gameKey, row.targetPly, mismatchRank(row.primaryTaxonomy)))

    SamePlyParityReport(
      summary =
        SamePlyParitySummary(
          groupedPlies = surfaces.groupBy(surface => s"${surface.gameKey}:${surface.targetPly}:${surface.sliceKind}").size,
          mismatchedPlies = rows.size,
          taxonomyCounts = countValues(rows.map(_.primaryTaxonomy)),
          layerCounts = countValues(rows.map(_.primaryLayer)),
          surfaceOnlyAugmentationAllowanceCounts =
            countValues(
              rows.flatMap(_.pairwiseMismatches).collect {
                case mismatch if mismatch.taxonomy == MismatchTaxonomy.SurfaceOnlyAugmentation =>
                  mismatch.allowanceTag.getOrElse(SurfaceOnlyAugmentationAllowance.ReviewRequired)
              }
            )
        ),
      rows = rows
    )

  def renderSamePlyParityMarkdown(
      report: SamePlyParityReport
  ): String =
    val rows =
      if report.rows.isEmpty then "- none"
      else
        report.rows.map { row =>
          val pairwise =
            row.pairwiseMismatches
              .map(mismatch =>
                s"${mismatch.leftSurface}/${mismatch.rightSurface}:${mismatch.taxonomy}:${mismatch.reasons.mkString(",")}"
              )
              .mkString(" | ")
          s"- `${row.gameKey}:${row.targetPly}` `${row.sliceKind}` `${row.primaryTaxonomy}` layer=`${row.primaryLayer}` move=`${row.playedSan}` pairwise=`$pairwise`"
        }.mkString("\n")
    s"""# Commentary Quality Same-Ply Parity Report
       |
       |- Grouped plies: `${report.summary.groupedPlies}`
       |- Mismatched plies: `${report.summary.mismatchedPlies}`
       |- Taxonomy counts: ${renderCountMap(report.summary.taxonomyCounts)}
       |- Layer counts: ${renderCountMap(report.summary.layerCounts)}
       |- Surface-only augmentation allowance counts: ${renderCountMap(report.summary.surfaceOnlyAugmentationAllowanceCounts)}
       |
       |## Representative mismatches
       |
       |$rows
       |""".stripMargin

  def renderJudgePrompt(
      evalInput: CommentaryQualityEvalRecord,
      baseline: Option[CommentaryQualityEvalRecord] = None
  ): String =
    val baselineBlock =
      baseline.map { value =>
        s"""Baseline candidate:
           |- label: ${value.candidateLabel}
           |- text: ${value.candidateText}
           |""".stripMargin
      }.getOrElse("")

    s"""You are the internal Chesstory commentary-quality judge for `${EvalSchema.JudgePromptVersion}`.
       |This is an internal filter, not a replacement for human evaluation.
       |Respect truth-first constraints: reward usefulness only when the move attribution stays correct, and penalize vivid overclaim.
       |
       |Return exactly one JSON object with these keys:
       |- `clarity` integer 1-5
       |- `move_attribution_correctness` integer 1-5
       |- `contrast_usefulness` integer 1-5
       |- `practical_usefulness` integer 1-5
       |- `dry_but_true_penalty` integer 0-3
       |- `overclaim_penalty` integer 0-3
       |- `overallVerdict` short string
       |- `summary` one sentence
       |- `evidenceNotes` array of short strings
       |- `flags` array of short strings
       |
       |Rubric:
       |- `clarity`: can a strong club player immediately tell what changed and why the prose is saying it.
       |- `move_attribution_correctness`: does the text correctly attribute the claim to the played move rather than to a state that already existed.
       |- `contrast_usefulness`: does contrast sharpen the point without inventing a stronger line than the truth layer certified.
       |- `practical_usefulness`: does the text help a human understand what to watch, avoid, or continue.
       |- `dry_but_true_penalty`: apply when the text is technically true but too inert to help a player.
       |- `overclaim_penalty`: apply when the text overstates certainty, names unsupported intent, or implies a benchmark the truth layer did not certify.
       |
       |Selector thresholds:
       |- move-attribution gate passes at `move_attribution_correctness >= ${EvalThresholds.MoveAttributionGateMin}`
       |- usefulness credit is zeroed when the move-attribution gate fails
       |- keep threshold requires selector score `>= ${EvalThresholds.KeepSelectorMin}` and `overclaim_penalty <= ${EvalThresholds.MaxOverclaimPenaltyForKeep}`
       |
       |Candidate metadata:
       |- surface: ${evalInput.surface}
       |- sliceKind: ${evalInput.sliceKind}
       |- ply: ${evalInput.targetPly}
       |- label: ${evalInput.candidateLabel}
       |- truthAnchor: ${evalInput.truthAnchor}
       |- upstreamTaxonomy: ${evalInput.upstreamTaxonomy.getOrElse("aligned")}
       |- bundleDigestHash: ${evalInput.bundleDigestHash.getOrElse("-")}
       |
       |Candidate text:
       |${evalInput.candidateText}
       |
       |$baselineBlock""".stripMargin

  def buildComparisonSummary(
      before: CommentaryQualityEvalRecord,
      after: CommentaryQualityEvalRecord
  ): CommentaryQualityEvalSummary =
    val beforeNet = before.rubric.netScore
    val afterNet = after.rubric.netScore
    val delta = afterNet - beforeNet
    CommentaryQualityEvalSummary(
      comparisonKey = after.comparisonKey,
      sampleId = after.sampleId,
      gameKey = after.gameKey,
      surface = after.surface,
      sliceKind = after.sliceKind,
      targetPly = after.targetPly,
      beforeLabel = before.candidateLabel,
      afterLabel = after.candidateLabel,
      beforeNetScore = beforeNet,
      afterNetScore = afterNet,
      beforeSelectorScore = before.selection.selectorScore,
      afterSelectorScore = after.selection.selectorScore,
      beforeUsefulnessCredit = before.selection.usefulnessCredit,
      afterUsefulnessCredit = after.selection.usefulnessCredit,
      beforeAttributionGatePassed = before.selection.moveAttributionGatePassed,
      afterAttributionGatePassed = after.selection.moveAttributionGatePassed,
      beforeThresholdVerdict = before.selection.thresholdVerdict,
      afterThresholdVerdict = after.selection.thresholdVerdict,
      netDelta = delta,
      rubricDeltas = after.rubric.deltas(before.rubric),
      improved = delta > 0,
      headline =
        if after.selection.usefulnessRewardBlocked then
          s"${after.surface} stayed in review because usefulness credit was blocked by move attribution."
        else if delta > 0 then s"${after.surface} improved by $delta net rubric points."
        else if delta < 0 then s"${after.surface} regressed by ${-delta} net rubric points."
          else s"${after.surface} stayed flat on the commentary-quality rubric."
    )

  def renderEvaluationSummaryMarkdown(
      records: List[CommentaryQualityEvalRecord],
      summaries: List[CommentaryQualityEvalSummary]
  ): String =
    val afterCandidates = records.filter(_.candidateLabel == "after")
    val verdictCounts = countValues(afterCandidates.map(_.selection.thresholdVerdict))
    val blockedCount = afterCandidates.count(_.selection.usefulnessRewardBlocked)
    val renderedRecords =
      if records.isEmpty then "- none"
      else
        records.map { record =>
          s"- `${record.sampleId}` `${record.surface}` `${record.candidateLabel}` rawNet=`${record.selection.rawNetScore}` selector=`${record.selection.selectorScore}` gate=`${record.selection.moveAttributionGatePassed}` usefulnessCredit=`${record.selection.usefulnessCredit}` verdict=`${record.selection.thresholdVerdict}` taxonomy=`${record.upstreamTaxonomy.getOrElse("aligned")}`"
        }.mkString("\n")
    val renderedSummaries =
      if summaries.isEmpty then "- none"
      else
        summaries.map { summary =>
          s"- `${summary.sampleId}` `${summary.beforeLabel} -> ${summary.afterLabel}` rawNet `${summary.beforeNetScore} -> ${summary.afterNetScore}` selector `${summary.beforeSelectorScore} -> ${summary.afterSelectorScore}` verdict `${summary.beforeThresholdVerdict} -> ${summary.afterThresholdVerdict}` headline=`${summary.headline}`"
        }.mkString("\n")

    s"""# Commentary Quality Metrics Scaffold
       |
       |- Schema version: `${EvalSchema.Version}`
       |- Summary version: `${EvalSchema.SummaryVersion}`
       |- Rubrics: `${EvalRubric.all.mkString(", ")}`
       |- Thresholds: gate=`move_attribution_correctness >= ${EvalThresholds.MoveAttributionGateMin}`, keep selector=`${EvalThresholds.KeepSelectorMin}+`, max overclaim for keep=`${EvalThresholds.MaxOverclaimPenaltyForKeep}`
       |- After verdict counts: ${renderCountMap(verdictCounts)}
       |- Usefulness reward blocked count: `$blockedCount`
       |
       |## Evaluation rows
       |
       |$renderedRecords
       |
       |## Before / after summaries
       |
       |$renderedSummaries
       |""".stripMargin

  def sampleEvaluationSlice(): (List[CommentaryQualityEvalRecord], List[CommentaryQualityEvalSummary]) =
    val before =
      makeEvaluationRecord(
          comparisonKey = "commentary_quality_demo_move_review_42",
        sampleId = "demo-game:long_structural_squeeze:42:moveReview",
        gameKey = "demo-game",
        surface = SurfaceName.MoveReview,
        sliceKind = "long_structural_squeeze",
        targetPly = 42,
        candidateLabel = "before",
        truthAnchor = "Re1 improved White's e4 break and reduced Black's c-file counterplay window.",
        candidateText = "White improves the position and keeps the pressure.",
        upstreamTaxonomy = Some(MismatchTaxonomy.SnapshotSkew),
        bundleDigestHash = Some("bundle-before"),
        rubric =
          EvaluationRubricScores(
            clarity = 2,
            moveAttributionCorrectness = 3,
            contrastUsefulness = 1,
            practicalUsefulness = 2,
            dryButTruePenalty = 1,
            overclaimPenalty = 1
          ),
        summary = "The move is pointed at the right side of the board, but the prose is generic and barely move-owned.",
        evidenceNotes = List("No concrete mention of e4 or the c-file timing window."),
        flags = List("generic_main_claim")
      )
    val after =
      makeEvaluationRecord(
        comparisonKey = before.comparisonKey,
        sampleId = before.sampleId,
        gameKey = before.gameKey,
        surface = before.surface,
        sliceKind = before.sliceKind,
        targetPly = before.targetPly,
        candidateLabel = "after",
        truthAnchor = before.truthAnchor,
        candidateText = "Re1 clears the rook off a1 so White can push e4 under better cover, and Black's c-file counterplay arrives too slowly afterward.",
        upstreamTaxonomy = Some(MismatchTaxonomy.SnapshotSkew),
        bundleDigestHash = Some("bundle-after"),
        rubric =
          EvaluationRubricScores(
            clarity = 4,
            moveAttributionCorrectness = 5,
            contrastUsefulness = 4,
            practicalUsefulness = 4,
            dryButTruePenalty = 0,
            overclaimPenalty = 0
          ),
        summary = "The move is explicitly owned, the practical race is visible, and the prose stays inside the truth anchor.",
        evidenceNotes = List("Names e4 as the move-linked gain.", "Explains the c-file race without inventing a best-line benchmark."),
        flags = Nil
      )
    val summary = buildComparisonSummary(before, after)
    (List(before, after), List(summary))

  def buildRealEvaluationSeedSlice(
      moveReviewEntries: List[CommentaryPlayerQcSupport.MoveReviewOutputEntry]
  ): Either[String, (List[CommentaryQualityEvalRecord], List[CommentaryQualityEvalSummary])] =
    val moveReviewBySampleId = moveReviewEntries.map(entry => entry.sampleId -> entry).toMap
    val missing = realEvalSeedSpecs.filterNot(spec => moveReviewBySampleId.contains(spec.sampleId)).map(_.sampleId)

    if missing.nonEmpty then Left(s"missing moveReview seed rows: ${missing.mkString(", ")}")
    else
      val resolved =
        realEvalSeedSpecs.map { spec =>
          val entry = moveReviewBySampleId(spec.sampleId)
          val taxonomy = None
          val before =
            makeEvaluationRecord(
              comparisonKey = spec.comparisonKey,
              sampleId = entry.sampleId,
              gameKey = entry.gameKey,
              surface = SurfaceName.MoveReview,
              sliceKind = entry.sliceKind,
              targetPly = entry.targetPly,
              candidateLabel = "before",
              truthAnchor = spec.truthAnchor,
              candidateText = entry.commentary,
              upstreamTaxonomy = taxonomy,
              bundleDigestHash = entry.moveReviewBundleDigestHash,
              rubric = spec.beforeRubric,
              summary = spec.beforeSummary,
              evidenceNotes = spec.beforeEvidenceNotes,
              flags = spec.beforeFlags
            )
          val after =
            makeEvaluationRecord(
              comparisonKey = spec.comparisonKey,
              sampleId = entry.sampleId,
              gameKey = entry.gameKey,
              surface = SurfaceName.MoveReview,
              sliceKind = entry.sliceKind,
              targetPly = entry.targetPly,
              candidateLabel = "after",
              truthAnchor = spec.truthAnchor,
              candidateText = spec.afterText,
              upstreamTaxonomy = taxonomy,
              bundleDigestHash = entry.moveReviewBundleDigestHash,
              rubric = spec.afterRubric,
              summary = spec.afterSummary,
              evidenceNotes = spec.afterEvidenceNotes,
              flags = spec.afterFlags
            )
          before -> after
        }
      Right(resolved.flatMap(pair => List(pair._1, pair._2)) -> resolved.map(buildComparisonSummary))

  def buildMoveReviewThresholdRow(
      entry: CommentaryPlayerQcSupport.MoveReviewOutputEntry
  ): SurfaceThresholdRow =
    val candidateText = normalizeText(entry.commentary)
    val upstreamAllowanceTag = None
    val questionAligned = alignedOption(entry.plannerSelectedQuestion, entry.plannerSelectedQuestion)
    val ownerAligned =
      alignedOption(entry.plannerSelectedOwnerKind, entry.plannerSelectedOwnerKind) &&
        alignedOption(entry.plannerSelectedSource, entry.plannerSelectedSource)
    val rubric =
      thresholdRubric(
        candidateText = candidateText,
        blankLike = blankLikeText(candidateText),
        plannerQuestion = entry.plannerSelectedQuestion,
        plannerProofFamily = entry.plannerSelectedOwnerKind,
        plannerProofSource = entry.plannerSelectedSource,
        surfaceQuestion = entry.plannerSelectedQuestion,
        surfaceProofFamily = entry.plannerSelectedOwnerKind,
        surfaceProofSource = entry.plannerSelectedSource,
        surfaceOutcome = entry.surfaceReplayOutcome.getOrElse(entry.moveReviewFallbackMode),
        exactFactual = entry.moveReviewFallbackMode == "exact_factual"
      )
    SurfaceThresholdRow(
      sampleId = entry.sampleId,
      gameKey = entry.gameKey,
      surface = SurfaceName.MoveReview,
      sliceKind = entry.sliceKind,
      targetPly = entry.targetPly,
      playedSan = entry.playedSan,
      candidateText = candidateText,
      surfaceOutcome = entry.surfaceReplayOutcome.getOrElse(entry.moveReviewFallbackMode),
      plannerQuestion = entry.plannerSelectedQuestion,
      plannerProofFamily = entry.plannerSelectedOwnerKind,
      plannerProofSource = entry.plannerSelectedSource,
      surfaceQuestion = entry.plannerSelectedQuestion,
      surfaceProofFamily = entry.plannerSelectedOwnerKind,
      surfaceProofSource = entry.plannerSelectedSource,
      upstreamTaxonomy = None,
      upstreamAllowedByDesign = true,
      upstreamAllowanceTag = upstreamAllowanceTag,
      plannerQuestionAligned = questionAligned,
      plannerOwnerAligned = ownerAligned,
      crossSurfaceQuestionDisagreement = false,
      crossSurfaceOwnerDisagreement = false,
      blankLike = blankLikeText(candidateText),
      rubric = rubric,
      selection = evaluateSelection(rubric),
      evidenceNotes =
        List(
          Option.when(entry.moveReviewFallbackMode == "exact_factual")("exact_factual_surface"),
          Option.when(entry.moveReviewFallbackMode == "planner_owned")("planner_owned_surface")
        ).flatten,
      flags =
        List(
          Option.when(entry.moveReviewFallbackMode == "exact_factual")("fallback_like"),
          Option.when(blankLikeText(candidateText))("blank_like")
        ).flatten
    )

  private def thresholdRubric(
      candidateText: String,
      blankLike: Boolean,
      plannerQuestion: Option[String],
      plannerProofFamily: Option[String],
      plannerProofSource: Option[String],
      surfaceQuestion: Option[String],
      surfaceProofFamily: Option[String],
      surfaceProofSource: Option[String],
      surfaceOutcome: String,
      exactFactual: Boolean
  ): EvaluationRubricScores =
    val actualOutputPresent = candidateText.nonEmpty
    val concrete = hasConcreteAnchorOrAction(candidateText)
    val citationOnly =
      lila.commentary.analysis.LineScopedCitation.hasInlineCitation(candidateText) && !concrete
    val genericTimingShell =
      normalizeText(candidateText).toLowerCase.contains("other moves allow the position to slip away")
    val plannerHasSelection =
      plannerQuestion.nonEmpty || plannerProofFamily.nonEmpty || plannerProofSource.nonEmpty
    val questionAligned = alignedOption(plannerQuestion, surfaceQuestion)
    val ownerAligned =
      alignedOption(plannerProofFamily, surfaceProofFamily) &&
        alignedOption(plannerProofSource, surfaceProofSource)
    val clarity =
      if !actualOutputPresent then 1
      else if blankLike then 2
      else if concrete && !citationOnly && !genericTimingShell then 4
      else if citationOnly then 2
      else 3
    val moveAttributionCorrectness =
      if !plannerHasSelection then
        if surfaceQuestion.isEmpty && surfaceProofFamily.isEmpty && surfaceProofSource.isEmpty then 4 else 3
      else if questionAligned && ownerAligned && actualOutputPresent then 5
      else if questionAligned && ownerAligned then 3
      else if !actualOutputPresent then 2
      else 2
    val contrastUsefulness =
      if !actualOutputPresent then 0
      else if plannerQuestion.exists(kind => kind == "WhyThis" || kind == "WhyNow") then
        if concrete && !genericTimingShell then 3 else 1
      else 0
    val practicalUsefulness =
      if !actualOutputPresent then 0
      else if concrete && !citationOnly then 4
      else if blankLike then 1
      else 2
    val dryButTruePenalty =
      if !actualOutputPresent then 3
      else if blankLike then 2
      else if exactFactual || genericTimingShell || citationOnly || isFallbackLike(surfaceOutcome) then 1
      else 0
    val overclaimPenalty =
      Option.when(looksOverclaimy(candidateText) && !concrete)(1).getOrElse(0)

    EvaluationRubricScores(
      clarity = clarity,
      moveAttributionCorrectness = moveAttributionCorrectness,
      contrastUsefulness = contrastUsefulness,
      practicalUsefulness = practicalUsefulness,
      dryButTruePenalty = dryButTruePenalty,
      overclaimPenalty = overclaimPenalty
    )

  def rowAllowedByDesign(
      parityRow: Option[SamePlyParityRow]
  ): Boolean =
    parityRow.exists { row =>
      row.pairwiseMismatches.nonEmpty &&
      row.pairwiseMismatches.forall(mismatch =>
        mismatch.taxonomy == MismatchTaxonomy.SurfaceOnlyAugmentation && mismatch.allowedByDesign
      )
    }

  def rowSurfaceOnlyAllowanceTag(
      parityRow: Option[SamePlyParityRow]
  ): Option[String] =
    parityRow.flatMap { row =>
      Option.when(rowAllowedByDesign(Some(row))) {
        row.pairwiseMismatches.flatMap(_.allowanceTag).distinct.mkString("+")
      }
    }

  def parityKey(gameKey: String, ply: Int, sliceKind: String): String =
    s"$gameKey:$ply:$sliceKind"



  private def alignedOption(left: Option[String], right: Option[String]): Boolean =
    (left, right) match
      case (None, None)       => true
      case (Some(a), Some(b)) => a == b
      case _                  => false

  private def blankLikeText(text: String): Boolean =
    val cleaned = normalizeText(text)
    val words = cleaned.split("\\s+").count(_.nonEmpty)
    words <= 11 &&
      !lila.commentary.analysis.LineScopedCitation.hasInlineCitation(cleaned) &&
      !lila.commentary.analysis.LiveNarrativeCompressionCore.hasConcreteAnchor(cleaned)

  private def looksOverclaimy(text: String): Boolean =
    val normalized = normalizeText(text).toLowerCase
    List(
      "dictating the whole middlegame",
      "nothing active",
      "wins by force",
      "completely winning",
      "simply winning"
    ).exists(normalized.contains)

  private def isFallbackLike(surfaceOutcome: String): Boolean =
    Set("factual_fallback", "exact_factual", "move_review_exact_factual")
      .contains(surfaceOutcome)

  private def normalizeText(raw: String): String =
    Option(raw).getOrElse("").replaceAll("\\s+", " ").trim

  private def pairwise(
      surfaces: List[SurfaceParitySnapshot]
  ): List[PairwiseParityMismatch] =
    surfaces.combinations(2).toList.flatMap {
      case List(left, right) => classifyPair(left, right)
      case _                 => None
    }

  private def classifyPair(
      left: SurfaceParitySnapshot,
      right: SurfaceParitySnapshot
  ): Option[PairwiseParityMismatch] =
    val snapshotEqual = equalDigest(left.digests.snapshotDigestHash, right.digests.snapshotDigestHash)
    val carryEqual = equalDigest(left.digests.carryDigestHash, right.digests.carryDigestHash)
    val augmentationEqual = equalDigest(left.digests.augmentationDigestHash, right.digests.augmentationDigestHash)
    val bundleMissing =
      left.digests.bundleDigestHash.forall(_.trim.isEmpty) || right.digests.bundleDigestHash.forall(_.trim.isEmpty)
    val replayTupleEqual =
      left.selectedQuestion == right.selectedQuestion &&
        left.selectedOwnerKind == right.selectedOwnerKind &&
        left.selectedSource == right.selectedSource &&
        normalizedReplayOutcome(left.replayOutcome) == normalizedReplayOutcome(right.replayOutcome)

    if bundleMissing then
      Some(
        PairwiseParityMismatch(
          leftSurface = left.surface,
          rightSurface = right.surface,
          taxonomy = MismatchTaxonomy.BundleMissing,
          layer = MismatchLayer.Upstream,
          reasons = List("one_surface_missing_bundle_digest")
        )
      )
    else if !snapshotEqual && carryEqual && augmentationEqual then
      Some(
        PairwiseParityMismatch(
          leftSurface = left.surface,
          rightSurface = right.surface,
          taxonomy = MismatchTaxonomy.SnapshotSkew,
          layer = MismatchLayer.Upstream,
          reasons = List("snapshot_digest_diverged", "carry_digest_aligned")
        )
      )
    else if snapshotEqual && !carryEqual && augmentationEqual then
      Some(
        PairwiseParityMismatch(
          leftSurface = left.surface,
          rightSurface = right.surface,
          taxonomy = MismatchTaxonomy.CarryMismatch,
          layer = MismatchLayer.Upstream,
          reasons = List("carry_digest_diverged", "snapshot_digest_aligned")
        )
      )
    else if snapshotEqual && carryEqual && !augmentationEqual then
      Some(
        PairwiseParityMismatch(
          leftSurface = left.surface,
          rightSurface = right.surface,
          taxonomy = MismatchTaxonomy.SurfaceOnlyAugmentation,
          layer = MismatchLayer.Upstream,
          reasons =
            List(
              "surface_augmentation_digest_diverged",
              if replayTupleEqual then "selection_aligned" else "selection_shift_after_augmentation"
            ),
          allowanceTag = None,
          allowedByDesign = false
        )
      )
    else if snapshotEqual && carryEqual && augmentationEqual && !replayTupleEqual then
      Some(
        PairwiseParityMismatch(
          leftSurface = left.surface,
          rightSurface = right.surface,
          taxonomy = MismatchTaxonomy.ReplayLayerRewrite,
          layer = MismatchLayer.Replay,
          reasons = List("digest_hashes_aligned", "selection_or_replay_outcome_diverged")
        )
      )
    else if !snapshotEqual || !carryEqual || !augmentationEqual then
      Some(
        PairwiseParityMismatch(
          leftSurface = left.surface,
          rightSurface = right.surface,
          taxonomy = MismatchTaxonomy.UpstreamLayerMismatch,
          layer = MismatchLayer.Upstream,
          reasons =
            List(
              Option.when(!snapshotEqual)("snapshot_digest_diverged"),
              Option.when(!carryEqual)("carry_digest_diverged"),
              Option.when(!augmentationEqual)("surface_augmentation_digest_diverged")
            ).flatten
        )
      )
    else None

  private def mismatchRank(
      taxonomy: String
  ): Int =
    MismatchTaxonomy.precedence.indexOf(taxonomy) match
      case -1 => Int.MaxValue
      case v  => v

  private def makeEvaluationRecord(
      comparisonKey: String,
      sampleId: String,
      gameKey: String,
      surface: String,
      sliceKind: String,
      targetPly: Int,
      candidateLabel: String,
      truthAnchor: String,
      candidateText: String,
      upstreamTaxonomy: Option[String],
      bundleDigestHash: Option[String],
      rubric: EvaluationRubricScores,
      summary: String,
      evidenceNotes: List[String],
      flags: List[String]
  ): CommentaryQualityEvalRecord =
    val selection = evaluateSelection(rubric)
    CommentaryQualityEvalRecord(
      comparisonKey = comparisonKey,
      sampleId = sampleId,
      gameKey = gameKey,
      surface = surface,
      sliceKind = sliceKind,
      targetPly = targetPly,
      candidateLabel = candidateLabel,
      truthAnchor = truthAnchor,
      candidateText = candidateText,
      upstreamTaxonomy = upstreamTaxonomy,
      bundleDigestHash = bundleDigestHash,
      rubric = rubric,
      selection = selection,
      overallVerdict = selection.thresholdVerdict,
      summary = summary,
      evidenceNotes = evidenceNotes,
      flags = flags
    )

  def evaluateSelection(
      rubric: EvaluationRubricScores
  ): EvaluationSelection =
    val gatePassed = rubric.moveAttributionCorrectness >= EvalThresholds.MoveAttributionGateMin
    val usefulnessCredit = if gatePassed then rubric.contrastUsefulness + rubric.practicalUsefulness else 0
    val usefulnessRewardBlocked =
      !gatePassed && (rubric.contrastUsefulness > 0 || rubric.practicalUsefulness > 0)
    val selectorScore =
      rubric.clarity +
        rubric.moveAttributionCorrectness +
        usefulnessCredit -
        rubric.dryButTruePenalty -
        rubric.overclaimPenalty
    val thresholdVerdict =
      if gatePassed &&
          selectorScore >= EvalThresholds.KeepSelectorMin &&
          rubric.overclaimPenalty <= EvalThresholds.MaxOverclaimPenaltyForKeep
      then EvalVerdict.Keep
      else EvalVerdict.Review

    EvaluationSelection(
      rawNetScore = rubric.netScore,
      selectorScore = selectorScore,
      moveAttributionGatePassed = gatePassed,
      usefulnessCredit = usefulnessCredit,
      usefulnessRewardBlocked = usefulnessRewardBlocked,
      thresholdVerdict = thresholdVerdict
      )

  private def selectPrimaryMismatch(
      mismatches: List[PairwiseParityMismatch]
  ): PairwiseParityMismatch =
    val disallowed = mismatches.filterNot(_.allowedByDesign)
    val pool = if disallowed.nonEmpty then disallowed else mismatches
    pool.sortBy(mismatch => mismatchRank(mismatch.taxonomy)).head

  private def realEvalSeedSpecs: List[RealEvalSeedSpec] =
    List(
      RealEvalSeedSpec(
        comparisonKey =
          "2024_03_03_4_1_bochnicka_vladimir_krivoborodov_egor_lichess_broadcast_master_classical_67:strategic_choice:14",
        sampleId =
          "2024_03_03_4_1_bochnicka_vladimir_krivoborodov_egor_lichess_broadcast_master_classical_67:strategic_choice:14:moveReview",
        truthAnchor =
          "cxd4 had to come before White got a cleaner recapture; Black keeps the center from opening on White's terms.",
        afterText =
          "cxd4 resolves the center before White untangles. Black cashes the tension on time here; if he waits, White gets the cleaner recapture and the position starts following White's version instead.",
        beforeRubric =
          EvaluationRubricScores(
            clarity = 2,
            moveAttributionCorrectness = 4,
            contrastUsefulness = 2,
            practicalUsefulness = 2,
            dryButTruePenalty = 1,
            overclaimPenalty = 0
          ),
        afterRubric =
          EvaluationRubricScores(
            clarity = 4,
            moveAttributionCorrectness = 5,
            contrastUsefulness = 4,
            practicalUsefulness = 4,
            dryButTruePenalty = 0,
            overclaimPenalty = 0
          ),
        beforeSummary =
          "The current prose is mostly true, but it leaves the center-tension point too generic to clear the keep threshold.",
        afterSummary =
          "The revised line stays move-owned and makes the timing race legible enough to keep.",
        beforeEvidenceNotes = List("Mentions timing, but not what cxd4 prevents."),
        afterEvidenceNotes = List("Names the central release as the move-owned point.", "Keeps the contrast tied to White's cleaner recapture instead of an invented benchmark."),
        beforeFlags = List("generic_timing_shell")
      ),
      RealEvalSeedSpec(
        comparisonKey =
          "2024_03_03_4_1_bochnicka_vladimir_krivoborodov_egor_lichess_broadcast_master_classical_67:practical_simplification:20",
        sampleId =
          "2024_03_03_4_1_bochnicka_vladimir_krivoborodov_egor_lichess_broadcast_master_classical_67:practical_simplification:20:moveReview",
        truthAnchor =
          "Be7 is mainly a development move to e7 here; it does not by itself seize the whole initiative or solve every queenside question.",
        afterText =
          "Be7 already takes over the queenside race and leaves White with nothing active to do, so Black is simply dictating the whole middlegame now.",
        beforeRubric =
          EvaluationRubricScores(
            clarity = 1,
            moveAttributionCorrectness = 4,
            contrastUsefulness = 1,
            practicalUsefulness = 1,
            dryButTruePenalty = 3,
            overclaimPenalty = 0
          ),
        afterRubric =
          EvaluationRubricScores(
            clarity = 4,
            moveAttributionCorrectness = 2,
            contrastUsefulness = 4,
            practicalUsefulness = 4,
            dryButTruePenalty = 0,
            overclaimPenalty = 2
          ),
        beforeSummary =
          "The baseline is exact but too inert, so it stays in review for being dry without helping the reader.",
        afterSummary =
          "The rewrite sounds useful, but it over-attributes the position to Be7; usefulness credit is blocked and the row stays in review.",
        beforeEvidenceNotes = List("Only states the destination square."),
        afterEvidenceNotes = List("Claims a full initiative swing that the truth anchor does not certify."),
        beforeFlags = List("dry_but_true"),
        afterFlags = List("usefulness_blocked_by_move_attribution_gate", "overclaim_risk")
      ),
      RealEvalSeedSpec(
        comparisonKey =
          "2024_03_03_4_1_bochnicka_vladimir_krivoborodov_egor_lichess_broadcast_master_classical_67:long_structural_squeeze:54",
        sampleId =
          "2024_03_03_4_1_bochnicka_vladimir_krivoborodov_egor_lichess_broadcast_master_classical_67:long_structural_squeeze:54:moveReview",
        truthAnchor =
          "Ke7 is a timing move inside the squeeze: Black improves coordination before the rook ending loosens, so White does not get the freer version of the position.",
        afterText =
          "Ke7 is a timing move, not a flourish. Black centralizes the king before the rook ending loosens, so the squeeze stays coordinated and White does not get the freer version of the endgame.",
        beforeRubric =
          EvaluationRubricScores(
            clarity = 2,
            moveAttributionCorrectness = 3,
            contrastUsefulness = 1,
            practicalUsefulness = 2,
            dryButTruePenalty = 2,
            overclaimPenalty = 0
          ),
        afterRubric =
          EvaluationRubricScores(
            clarity = 4,
            moveAttributionCorrectness = 4,
            contrastUsefulness = 3,
            practicalUsefulness = 4,
            dryButTruePenalty = 0,
            overclaimPenalty = 0
          ),
        beforeSummary =
          "The baseline hints at timing, but it never states what Ke7 preserves, so it stays below keep.",
        afterSummary =
          "The revised sentence makes the squeeze legible without pretending Ke7 wins by force, so it clears keep.",
        beforeEvidenceNotes = List("States urgency without naming the coordination gain."),
        afterEvidenceNotes = List("Explains the king-centralization timing in the endgame transition."),
        beforeFlags = List("generic_timing_shell")
      )
    )

  private def digestSet(
      snapshotJson: JsValue,
      carryJson: JsValue,
      augmentationJson: JsValue,
      preserveSnapshot: Option[String] = None,
      preserveCarry: Option[String] = None
  ): SurfaceDigestHashes =
    val snapshotHash = preserveSnapshot.orElse(hashJson(snapshotJson))
    val carryHash = preserveCarry.orElse(hashJson(carryJson))
    val augmentationHash = hashJson(augmentationJson)
    val bundleHash =
      hashJson(
        Json.obj(
          "snapshotDigestHash" -> snapshotHash,
          "carryDigestHash" -> carryHash,
          "augmentationDigestHash" -> augmentationHash
        )
      )
    SurfaceDigestHashes(
      snapshotDigestHash = snapshotHash,
      carryDigestHash = carryHash,
      augmentationDigestHash = augmentationHash,
      bundleDigestHash = bundleHash
    )

  private def hashJson(
      js: JsValue
  ): Option[String] =
    Option.when(js != JsNull) {
      val payload = Json.stringify(js)
      val digest = MessageDigest.getInstance("SHA-256").digest(payload.getBytes("UTF-8"))
      digest.map("%02x".format(_)).mkString
    }

  private def equalDigest(
      left: Option[String],
      right: Option[String]
  ): Boolean =
    (left, right) match
      case (Some(a), Some(b)) => a == b
      case (None, None)       => true
      case _                  => false

  private def normalizedReplayOutcome(
      outcome: Option[String]
  ): Option[String] =
    outcome.map {
      case "move_review_planner_owned" => "planner_owned"
      case "move_review_exact_factual" => "factual_fallback"
      case other                     => other
    }

  private def jsonOrNull[A: Writes](
      value: Option[A]
  ): JsValue =
    value.map(Json.toJson(_)).getOrElse(JsNull)

  private def hasConcreteAnchorOrAction(
      text: String
  ): Boolean =
    lila.commentary.analysis.LiveNarrativeCompressionCore.hasConcreteAnchor(text) ||
      normalizeText(text).toLowerCase.split("""[^a-z0-9]+""").exists(token =>
        token.nonEmpty && Set(
          "break",
          "reply",
          "recapture",
          "threat",
          "counterplay",
          "mate",
          "wins",
          "loses",
          "drops",
          "fork",
          "pin",
          "trade"
        ).contains(token)
      )

  private def truthContractJson(
      truthContract: Option[DecisiveTruthContract]
  ): JsValue =
    truthContract
      .map { contract =>
        Json.obj(
          "playedMove" -> contract.playedMove,
          "verifiedBestMove" -> contract.verifiedBestMove,
          "truthClass" -> contract.truthClass.toString,
          "cpLoss" -> contract.cpLoss,
          "swingSeverity" -> contract.swingSeverity,
          "reasonFamily" -> contract.reasonFamily.toString,
          "truthPhase" -> contract.truthPhase.map(_.toString),
          "ownershipRole" -> contract.ownershipRole.toString,
          "visibilityRole" -> contract.visibilityRole.toString,
          "surfaceMode" -> contract.surfaceMode.toString,
          "exemplarRole" -> contract.exemplarRole.toString,
          "surfacedMoveOwnsTruth" -> contract.surfacedMoveOwnsTruth,
          "verifiedPayoffAnchor" -> contract.verifiedPayoffAnchor,
          "compensationProseAllowed" -> contract.compensationProseAllowed,
          "benchmarkProseAllowed" -> contract.benchmarkProseAllowed,
          "investmentTruthChainKey" -> contract.investmentTruthChainKey
        )
      }
      .getOrElse(JsNull)

  private def authorQuestionJson(
      question: AuthorQuestion
  ): JsObject =
    Json.obj(
      "id" -> question.id,
      "kind" -> question.kind.toString,
      "priority" -> question.priority,
      "question" -> question.question,
      "anchors" -> question.anchors,
      "evidencePurposes" -> question.evidencePurposes
    )

  private def questionEvidenceJson(
      evidence: QuestionEvidence
  ): JsObject =
    Json.obj(
      "questionId" -> evidence.questionId,
      "purpose" -> evidence.purpose,
      "branches" -> evidence.branches.map(branch => Json.obj("keyMove" -> branch.keyMove, "line" -> branch.line))
    )

  private def planHypothesisJson(
      plan: lila.commentary.model.authoring.PlanHypothesis
  ): JsObject =
    Json.obj(
      "planId" -> plan.planId,
      "planName" -> plan.planName,
      "rank" -> plan.rank,
      "score" -> plan.score
    )

  private def renderCountMap(
      values: Map[String, Int]
  ): String =
    if values.isEmpty then "{}"
    else values.toList.sortBy(_._1).map { case (key, count) => s"$key=$count" }.mkString("{", ", ", "}")

  private def countValues(
      values: List[String]
  ): Map[String, Int] =
    values.groupBy(identity).view.mapValues(_.size).toMap
