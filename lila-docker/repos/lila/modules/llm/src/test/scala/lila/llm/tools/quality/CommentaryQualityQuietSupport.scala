package lila.llm.tools.quality

import java.nio.file.{ Files, Paths }

import scala.util.control.NonFatal

import play.api.libs.json.{ Format, JsValue, Json }
import lila.llm.tools.review.{ ChronicleActivePlannerSliceRunner, CommentaryPlayerQcSupport }

object CommentaryQualityQuietSupport:

  import CommentaryPlayerQcSupport.{ BookmakerOutputEntry, SliceKind }
  import CommentaryQualitySupport.{ EvalRubric, EvaluationRubricScores, evaluateSelection }

  object Schema:
    val QuietRichVersion = "commentary_quality_quiet_support_quiet_rich.v1"
    val QuietSupportSelectorVersion = "commentary_quality_quiet_support_selector.v1"
    val QuietSupportEvalVersion = "commentary_quality_quiet_support_eval.v1"
    val QuietSupportSummaryVersion = "commentary_quality_quiet_support_summary.v1"

  object Bucket:
    val ProphylaxisRestraint = "prophylaxis_restraint"
    val LongStructuralSqueeze = "long_structural_squeeze"
    val SlowRouteImprovement = "slow_route_improvement"
    val OpeningDeviationAfterStableDevelopment = "opening_deviation_after_stable_development"
    val TransitionHeavyEndgames = "transition_heavy_endgames"
    val PressureMaintenanceWithoutImmediateTactic = "pressure_maintenance_without_immediate_tactic"

    val all = List(
      ProphylaxisRestraint,
      LongStructuralSqueeze,
      SlowRouteImprovement,
      OpeningDeviationAfterStableDevelopment,
      TransitionHeavyEndgames,
      PressureMaintenanceWithoutImmediateTactic
    )

  object Status:
    val Eligible = "eligible"
    val UpstreamBlocked = "upstream_blocked"
    val NonEligible = "non_eligible"

  object Lane:
    val Eligible = "quiet-rich eligible rows"
    val Blocked = "upstream-blocked / non-eligible rows"

  object SourceRole:
    val DirectOwner = "direct_owner"
    val SupportOnly = "support_only"
    val DirectOwnerOrFallback = "direct_owner_or_fallback_only"

  final case class QuietRichRow(
      schemaVersion: String = Schema.QuietRichVersion,
      sampleId: String,
      gameKey: String,
      bucket: String,
      sliceKind: String,
      targetPly: Int,
      playedSan: String,
      commentary: String,
      realShard: Boolean = true,
      status: String,
      lane: String,
      statusReasons: List[String],
      plannerSceneType: Option[String],
      plannerSelectedOwnerFamily: Option[String],
      plannerSelectedOwnerSource: Option[String],
      bookmakerFallbackMode: String,
      quietnessVerified: Boolean,
      quietnessSpreadCp: Option[Int],
      quietnessNotes: List[String],
      directSources: List[String],
      supportSources: List[String]
  )
  object QuietRichRow:
    given Format[QuietRichRow] = Json.format[QuietRichRow]

  final case class QuietRichBucketSummary(
      bucket: String,
      totalRows: Int,
      eligibleRows: Int,
      upstreamBlockedRows: Int,
      nonEligibleRows: Int,
      representativeSampleIds: List[String]
  )
  object QuietRichBucketSummary:
    given Format[QuietRichBucketSummary] = Json.format[QuietRichBucketSummary]

  final case class StrategicSupportSourceInventoryRow(
      source: String,
      quietBucketSuitability: List[String],
      ownerSupportRole: String,
      reusePossible: Boolean,
      notes: String
  )
  object StrategicSupportSourceInventoryRow:
    given Format[StrategicSupportSourceInventoryRow] = Json.format[StrategicSupportSourceInventoryRow]

  final case class ModalityEnvelopeRow(
      status: String,
      verbFamily: String,
      examples: List[String],
      allowedWhen: String
  )
  object ModalityEnvelopeRow:
    given Format[ModalityEnvelopeRow] = Json.format[ModalityEnvelopeRow]

  final case class ModalityRewriteExample(
      bucket: String,
      before: String,
      after: String,
      reason: String
  )
  object ModalityRewriteExample:
    given Format[ModalityRewriteExample] = Json.format[ModalityRewriteExample]

  final case class SurfaceQuietSupportPlan(
      surface: String,
      attachMode: String,
      sentenceBudget: Int,
      allowedSources: List[String],
      safeBecause: String
  )
  object SurfaceQuietSupportPlan:
    given Format[SurfaceQuietSupportPlan] = Json.format[SurfaceQuietSupportPlan]

  final case class EvaluationLaneSummary(
      lane: String,
      rowCount: Int,
      rubricFamilies: List[String],
      sampleIds: List[String]
  )
  object EvaluationLaneSummary:
    given Format[EvaluationLaneSummary] = Json.format[EvaluationLaneSummary]

  final case class BaselineRegressionStatus(
      status: String,
      notes: List[String]
  )
  object BaselineRegressionStatus:
    given Format[BaselineRegressionStatus] = Json.format[BaselineRegressionStatus]

  final case class QuietRichReport(
      schemaVersion: String = Schema.QuietRichVersion,
      realShardSource: String,
      realShardOnly: Boolean,
      bucketSummaries: List[QuietRichBucketSummary],
      rows: List[QuietRichRow],
      sourceInventory: List[StrategicSupportSourceInventoryRow],
      modalityEnvelope: List[ModalityEnvelopeRow],
      strongerVerbExceptions: List[ModalityEnvelopeRow],
      rewriteExamples: List[ModalityRewriteExample],
      quietSupportPlans: List[SurfaceQuietSupportPlan],
      evaluationLanes: List[EvaluationLaneSummary],
      baselineRegressionStatus: BaselineRegressionStatus,
      nextRecommendedMove: String
  )
  object QuietRichReport:
    given Format[QuietRichReport] = Json.format[QuietRichReport]

  private final case class ParsedVariationLead(
      san: Option[String],
      isCapture: Boolean,
      givesCheck: Boolean,
      scoreCp: Option[Int]
  )

  private final case class ParsedSignalDigest(
      deploymentRoute: List[String],
      structuralCue: Option[String],
      practicalVerdict: Option[String],
      openingRelationClaim: Option[String],
      endgameTransitionClaim: Option[String],
      prophylaxisPlan: Option[String],
      prophylaxisThreat: Option[String],
      counterplayScoreDrop: Option[Int]
  )

  private final case class ParsedRawPayload(
      concepts: List[String],
      top: Option[ParsedVariationLead],
      second: Option[ParsedVariationLead],
      spreadCp: Option[Int],
      signal: ParsedSignalDigest
  )

  private final case class QuietnessCheck(
      passed: Boolean,
      reasons: List[String],
      notes: List[String]
  )

  private final case class SourceProfile(
      directSources: List[String],
      supportSources: List[String],
      missingPrerequisites: List[String]
  )

  def buildQuietRichReport(
      bookmakerEntries: List[BookmakerOutputEntry],
      realShardSource: String
  ): QuietRichReport =
    val rawCache =
      bookmakerEntries
        .flatMap(entry => parseRawPayload(entry.rawResponsePath).map(payload => entry.rawResponsePath -> payload))
        .toMap

    val rows =
      bookmakerEntries.flatMap { entry =>
        rawCache.get(entry.rawResponsePath).toList.flatMap { payload =>
          classifyEntry(entry, payload)
        }
      }

    val bucketSummaries =
      Bucket.all.map { bucket =>
        val bucketRows = rows.filter(_.bucket == bucket)
        QuietRichBucketSummary(
          bucket = bucket,
          totalRows = bucketRows.size,
          eligibleRows = bucketRows.count(_.status == Status.Eligible),
          upstreamBlockedRows = bucketRows.count(_.status == Status.UpstreamBlocked),
          nonEligibleRows = bucketRows.count(_.status == Status.NonEligible),
          representativeSampleIds =
            bucketRows
              .sortBy(row => representativeOrdering(row))
              .take(3)
              .map(_.sampleId)
        )
      }

    val evaluationLanes =
      List(
        EvaluationLaneSummary(
          lane = Lane.Eligible,
          rowCount = rows.count(_.status == Status.Eligible),
          rubricFamilies = quietSupportRubrics,
          sampleIds =
            rows
              .filter(_.status == Status.Eligible)
              .map(_.sampleId)
              .distinct
              .sorted
              .take(12)
        ),
        EvaluationLaneSummary(
          lane = Lane.Blocked,
          rowCount = rows.count(_.status != Status.Eligible),
          rubricFamilies = quietSupportRubrics,
          sampleIds =
            rows
              .filter(_.status != Status.Eligible)
              .map(_.sampleId)
              .distinct
              .sorted
              .take(12)
        )
      )

    QuietRichReport(
      realShardSource = realShardSource,
      realShardOnly = true,
      bucketSummaries = bucketSummaries,
      rows = rows.sortBy(row => (row.bucket, representativeOrdering(row), row.sampleId)),
      sourceInventory = sourceInventoryRows,
      modalityEnvelope = modalityEnvelopeRows,
      strongerVerbExceptions = strongerVerbRows,
      rewriteExamples = rewriteExamples,
      quietSupportPlans = quietSupportPlans,
      evaluationLanes = evaluationLanes,
      baselineRegressionStatus =
        BaselineRegressionStatus(
          status = "no_baseline_regression",
          notes = List(
            "Baseline gate remains fixed at move_attribution_correctness >= 4.",
            "Allowance boundary remains unchanged; contrast taxonomy is not reopened.",
            "This appendix changes only test/tooling and audit planning, not legality, ranking, or owner admission."
          )
        ),
      nextRecommendedMove =
        "quiet_support_chronicle_replay: mirror the accepted Bookmaker quiet-support chain into Chronicle replay only for planner-owned claim-only quiet scenes, without reopening owner selection or blocked lanes."
    )

  def renderQuietRichMarkdown(
      report: QuietRichReport
  ): String =
    val bucketTable =
      if report.bucketSummaries.isEmpty then
        "| bucket | total | eligible | upstream-blocked | non-eligible |\n| --- | --- | --- | --- | --- |\n| none | 0 | 0 | 0 | 0 |"
      else
        val rows =
          report.bucketSummaries.map { summary =>
            s"| `${summary.bucket}` | `${summary.totalRows}` | `${summary.eligibleRows}` | `${summary.upstreamBlockedRows}` | `${summary.nonEligibleRows}` |"
          }.mkString("\n")
        s"| bucket | total | eligible | upstream-blocked | non-eligible |\n| --- | --- | --- | --- | --- |\n$rows"

    val representativeSections =
      Bucket.all.map { bucket =>
        val bucketRows =
          report.rows
            .filter(_.bucket == bucket)
            .sortBy(row => representativeOrdering(row))
            .take(3)
        val rendered =
          if bucketRows.isEmpty then "- none"
          else
            bucketRows.map { row =>
              val sources = (row.directSources ++ row.supportSources).distinct.mkString(", ")
              s"- `${row.sampleId}` `${row.playedSan}` status=`${row.status}` slice=`${row.sliceKind}` spread=`${row.quietnessSpreadCp.getOrElse(-1)}` sources=`$sources` text=`${row.commentary}`"
            }.mkString("\n")
        s"### `$bucket`\n$rendered"
      }.mkString("\n\n")

    val sourceTable =
      if report.sourceInventory.isEmpty then
        "| source | role | quiet bucket suitability | reuse | notes |\n| --- | --- | --- | --- | --- |\n| none | none | none | false | no reusable sources were recorded |"
      else
        val rows =
          report.sourceInventory.map { row =>
            s"| `${row.source}` | `${row.ownerSupportRole}` | `${row.quietBucketSuitability.mkString(", ")}` | `${row.reusePossible}` | ${row.notes} |"
          }.mkString("\n")
        s"| source | role | quiet bucket suitability | reuse | notes |\n| --- | --- | --- | --- | --- |\n$rows"

    val modalityTable =
      if report.modalityEnvelope.isEmpty then
        "| status | verb family | examples | allowed when |\n| --- | --- | --- | --- |\n| none | none | none | no envelope recorded |"
      else
        val rows =
          report.modalityEnvelope.map { row =>
            s"| `${row.status}` | `${row.verbFamily}` | `${row.examples.mkString(", ")}` | ${row.allowedWhen} |"
          }.mkString("\n")
        s"| status | verb family | examples | allowed when |\n| --- | --- | --- | --- |\n$rows"

    val strongerTable =
      if report.strongerVerbExceptions.isEmpty then
        "| verb family | examples | allowed when |\n| --- | --- | --- |\n| none | none | no proof-backed exceptions recorded |"
      else
        val rows =
          report.strongerVerbExceptions.map { row =>
            s"| `${row.verbFamily}` | `${row.examples.mkString(", ")}` | ${row.allowedWhen} |"
          }.mkString("\n")
        s"| verb family | examples | allowed when |\n| --- | --- | --- |\n$rows"

    val rewriteBullets =
      report.rewriteExamples.map { example =>
        s"- `${example.bucket}` before=`${example.before}` after=`${example.after}` reason=`${example.reason}`"
      }.mkString("\n")

    val planBullets =
      report.quietSupportPlans.map { plan =>
        s"- `${plan.surface}` mode=`${plan.attachMode}` sentenceBudget=`${plan.sentenceBudget}` sources=`${plan.allowedSources.mkString(", ")}` safeBecause=`${plan.safeBecause}`"
      }.mkString("\n")

    val laneBullets =
      report.evaluationLanes.map { lane =>
        val sampleIds =
          if lane.sampleIds.isEmpty then "none"
          else lane.sampleIds.mkString(", ")
        s"- `${lane.lane}` rowCount=`${lane.rowCount}` rubrics=`${lane.rubricFamilies.mkString(", ")}` sampleIds=`$sampleIds`"
      }.mkString("\n")

    val regressionBullets =
    report.baselineRegressionStatus.notes.map(note => s"- $note").mkString("\n")

    List(
        "# Quiet-Support Appendix",
      "",
      "## 1. quiet-rich scene slice",
      "",
      s"- real shard basis: `${report.realShardOnly}`",
      s"- source: `${report.realShardSource}`",
      "",
      bucketTable,
      "",
      "Representative rows:",
      "",
      representativeSections,
      "",
      "## 2. strategic support source inventory",
      "",
      sourceTable,
      "",
      "## 3. modality envelope",
      "",
      modalityTable,
      "",
        "Proof-backed stronger verbs kept outside the default quiet-support envelope:",
      "",
      strongerTable,
      "",
      "Example rewrites:",
      "",
      rewriteBullets,
      "",
      "## 4. Bookmaker / Chronicle quiet-support plan",
      "",
      planBullets,
      "",
        "## 5. Baseline regression status",
      "",
        s"- status: `${report.baselineRegressionStatus.status}`",
      "",
      regressionBullets,
      "",
      "Evaluation scaffold lanes:",
      "",
      laneBullets,
      "",
      "## 6. next recommended move",
      "",
      s"- `${report.nextRecommendedMove}`"
    ).mkString("\n")

  private def classifyEntry(
      entry: BookmakerOutputEntry,
      payload: ParsedRawPayload
  ): List[QuietRichRow] =
    candidateBuckets(entry, payload).map { bucket =>
      val quietness = quietnessCheck(payload)
      val profile = sourceProfile(bucket, entry, payload)
      val blockedByClosedFamily =
        entry.plannerSelectedOwnerFamily.exists(family =>
          family == "ForcingDefense" || family == "TacticalFailure"
        )
      val statusReasons =
        if !quietness.passed then quietness.reasons
        else if profile.missingPrerequisites.nonEmpty then profile.missingPrerequisites
        else if blockedByClosedFamily then List("closed_owner_family_already_selected")
        else if profile.directSources.isEmpty && profile.supportSources.isEmpty then
          List("no_reusable_move_linked_support")
        else Nil

      val status =
        if !quietness.passed then Status.NonEligible
        else if statusReasons.nonEmpty then Status.UpstreamBlocked
        else Status.Eligible

      QuietRichRow(
        sampleId = entry.sampleId,
        gameKey = entry.gameKey,
        bucket = bucket,
        sliceKind = entry.sliceKind,
        targetPly = entry.targetPly,
        playedSan = entry.playedSan,
        commentary = entry.commentary,
        status = status,
        lane = if status == Status.Eligible then Lane.Eligible else Lane.Blocked,
        statusReasons = statusReasons,
        plannerSceneType = entry.plannerSceneType,
        plannerSelectedOwnerFamily = entry.plannerSelectedOwnerFamily,
        plannerSelectedOwnerSource = entry.plannerSelectedOwnerSource,
        bookmakerFallbackMode = entry.bookmakerFallbackMode,
        quietnessVerified = quietness.passed,
        quietnessSpreadCp = payload.spreadCp,
        quietnessNotes = quietness.notes,
        directSources = profile.directSources,
        supportSources = profile.supportSources
      )
    }

  private def candidateBuckets(
      entry: BookmakerOutputEntry,
      payload: ParsedRawPayload
  ): List[String] =
    List(
      Option.when(entry.sliceKind == SliceKind.LongStructuralSqueeze)(Bucket.LongStructuralSqueeze),
      Option.when(entry.sliceKind == SliceKind.OpeningTransition)(Bucket.OpeningDeviationAfterStableDevelopment),
      Option.when(entry.sliceKind == SliceKind.TransitionHeavyEndgames)(Bucket.TransitionHeavyEndgames),
      Option.when(hasSlowRouteImprovementCandidate(entry, payload))(Bucket.SlowRouteImprovement),
      Option.when(hasPressureMaintenanceCandidate(entry, payload))(Bucket.PressureMaintenanceWithoutImmediateTactic),
      Option.when(hasProphylaxisCandidate(entry, payload))(Bucket.ProphylaxisRestraint)
    ).flatten.distinct

  private def hasSlowRouteImprovementCandidate(
      entry: BookmakerOutputEntry,
      payload: ParsedRawPayload
  ): Boolean =
    (entry.sliceKind == SliceKind.StrategicChoice ||
      entry.sliceKind == SliceKind.LongStructuralSqueeze ||
      entry.sliceKind == SliceKind.TransitionHeavyEndgames ||
      entry.sliceKind == SliceKind.EndgameConversion) &&
    (payload.signal.deploymentRoute.nonEmpty || looksLikePieceImprovement(entry.playedSan))

  private def hasPressureMaintenanceCandidate(
      entry: BookmakerOutputEntry,
      payload: ParsedRawPayload
  ): Boolean =
    (entry.sliceKind == SliceKind.LongStructuralSqueeze ||
      entry.sliceKind == SliceKind.TransitionHeavyEndgames ||
      entry.sliceKind == SliceKind.EndgameConversion) &&
    (
      payload.signal.structuralCue.nonEmpty ||
        payload.signal.practicalVerdict.nonEmpty ||
        payload.signal.deploymentRoute.nonEmpty
    )

  private def hasProphylaxisCandidate(
      entry: BookmakerOutputEntry,
      payload: ParsedRawPayload
  ): Boolean =
    (entry.sliceKind == SliceKind.LongStructuralSqueeze ||
      entry.sliceKind == SliceKind.TransitionHeavyEndgames ||
      entry.sliceKind == SliceKind.EndgameConversion ||
      entry.sliceKind == SliceKind.StrategicChoice) &&
    (
      payload.signal.prophylaxisPlan.nonEmpty ||
        payload.signal.prophylaxisThreat.nonEmpty ||
        payload.signal.counterplayScoreDrop.exists(_ >= 40) ||
        payload.concepts.exists(_.equalsIgnoreCase("Prophylaxis against counterplay"))
    )

  private def quietnessCheck(
      payload: ParsedRawPayload
  ): QuietnessCheck =
    val reasons = List.newBuilder[String]
    val notes = List.newBuilder[String]

    val top = payload.top
    val second = payload.second

    if top.isEmpty || second.isEmpty then reasons += "missing_multi_pv_evidence"

    payload.spreadCp.foreach { spread =>
      notes += s"spread=${spread}cp"
      if spread > 100 then reasons += "multi_pv_spread_above_100cp"
    }

    top.foreach { lead =>
      notes += s"top=${lead.san.getOrElse("unknown")}"
      lead.scoreCp.foreach(score => notes += s"top_eval=${score}cp")
      if lead.givesCheck then reasons += "top_line_starts_with_check"
      if lead.isCapture then reasons += "top_line_starts_with_capture"
      lead.scoreCp.filter(score => math.abs(score) > 250).foreach(_ =>
        reasons += "top_eval_outside_quiet_band"
      )
    }

    second.foreach { lead =>
      notes += s"second=${lead.san.getOrElse("unknown")}"
      if lead.givesCheck then reasons += "second_line_starts_with_check"
      if lead.isCapture then reasons += "second_line_starts_with_capture"
    }

    QuietnessCheck(
      passed = reasons.result().isEmpty,
      reasons = reasons.result(),
      notes = notes.result().distinct
    )

  private def sourceProfile(
      bucket: String,
      entry: BookmakerOutputEntry,
      payload: ParsedRawPayload
  ): SourceProfile =
    val direct = List.newBuilder[String]
    val support = List.newBuilder[String]
    val missing = List.newBuilder[String]

    if hasTrace(entry, "quiet_move_claim") then direct += "QuietMoveIntentBuilder.quiet_move_claim"
    if hasTrace(entry, "source_kind=pv_delta") then direct += "MoveDelta.pv_delta"
    if hasTrace(entry, "source_kind=main_bundle") then direct += "MoveDelta.main_bundle"

    if payload.signal.openingRelationClaim.nonEmpty || hasTrace(entry, "opening_relation_translator") then
      direct += "OpeningRelation.translator"
    else if bucket == Bucket.OpeningDeviationAfterStableDevelopment then
      missing += "opening_relation_translator_missing"

    if hasTrace(entry, "opening_precedent_summary") then support += "OpeningRelation.summary"

    if payload.signal.endgameTransitionClaim.nonEmpty || hasTrace(entry, "endgame_transition_translator") then
      direct += "EndgameTransition.translator"
    else if bucket == Bucket.TransitionHeavyEndgames then
      missing += "endgame_transition_translator_missing"

    if hasTrace(entry, "endgame_theoretical_hint") then support += "EndgameTransition.hint"

    if payload.signal.deploymentRoute.nonEmpty then support += "Digest.route"
    if payload.signal.structuralCue.nonEmpty then support += "Digest.structure"
    if payload.signal.practicalVerdict.nonEmpty then support += "Digest.pressure"

    if payload.signal.prophylaxisPlan.nonEmpty ||
      payload.signal.prophylaxisThreat.nonEmpty ||
      payload.signal.counterplayScoreDrop.exists(_ >= 40)
    then support += "Digest.restriction"
    else if bucket == Bucket.ProphylaxisRestraint then
      missing += "move_linked_restriction_signal_missing"

    if bucket == Bucket.SlowRouteImprovement &&
      !support.result().contains("Digest.route")
    then
      missing += "route_digest_missing"

    if bucket == Bucket.LongStructuralSqueeze &&
      !support.result().exists(source => source == "Digest.structure" || source == "Digest.pressure")
    then
      missing += "structural_support_missing"

    if bucket == Bucket.PressureMaintenanceWithoutImmediateTactic &&
      !support.result().exists(source => source == "Digest.structure" || source == "Digest.pressure")
    then
      missing += "pressure_maintenance_support_missing"

    SourceProfile(
      directSources = direct.result().distinct.sorted,
      supportSources = support.result().distinct.sorted,
      missingPrerequisites = missing.result().distinct.sorted
    )

  private def parseRawPayload(
      rawResponsePath: String
  ): Option[ParsedRawPayload] =
    val path = Paths.get(rawResponsePath)
    if !Files.exists(path) then None
    else
      try
        val js = Json.parse(Files.readString(path))
        val signalDigest = (js \ "signalDigest").toOption.getOrElse(Json.obj())
        val top = variationLead(js, 0)
        val second = variationLead(js, 1)
        val spread =
          for
            first <- top.flatMap(_.scoreCp)
            next <- second.flatMap(_.scoreCp)
          yield math.abs(first - next)
        Some(
          ParsedRawPayload(
            concepts = stringList(js, "concepts"),
            top = top,
            second = second,
            spreadCp = spread,
            signal =
              ParsedSignalDigest(
                deploymentRoute = stringList(signalDigest, "deploymentRoute"),
                structuralCue = stringField(signalDigest, "structuralCue"),
                practicalVerdict = stringField(signalDigest, "practicalVerdict"),
                openingRelationClaim = stringField(signalDigest, "openingRelationClaim"),
                endgameTransitionClaim = stringField(signalDigest, "endgameTransitionClaim"),
                prophylaxisPlan = stringField(signalDigest, "prophylaxisPlan"),
                prophylaxisThreat = stringField(signalDigest, "prophylaxisThreat"),
                counterplayScoreDrop = intField(signalDigest, "counterplayScoreDrop")
              )
          )
        )
      catch
        case NonFatal(_) => None

  private def variationLead(
      root: JsValue,
      index: Int
  ): Option[ParsedVariationLead] =
    (root \ "variations").asOpt[List[JsValue]].flatMap(_.lift(index)).map { variation =>
      val parsedMoves = (variation \ "parsedMoves").asOpt[List[JsValue]].getOrElse(Nil)
      val firstParsed = parsedMoves.headOption
      ParsedVariationLead(
        san = firstParsed.flatMap(js => (js \ "san").asOpt[String]).flatMap(clean),
        isCapture = firstParsed.flatMap(js => (js \ "isCapture").asOpt[Boolean]).getOrElse(false),
        givesCheck = firstParsed.flatMap(js => (js \ "givesCheck").asOpt[Boolean]).getOrElse(false),
        scoreCp = (variation \ "scoreCp").asOpt[Int]
      )
    }

  private def stringList(
      root: JsValue,
      field: String
  ): List[String] =
    (root \ field).asOpt[List[String]].getOrElse(Nil).flatMap(clean)

  private def stringField(
      root: JsValue,
      field: String
  ): Option[String] =
    (root \ field).asOpt[String].flatMap(clean)

  private def intField(
      root: JsValue,
      field: String
  ): Option[Int] =
    (root \ field).asOpt[Int]

  private def looksLikePieceImprovement(
      san: String
  ): Boolean =
    clean(san).exists { value =>
      val normalized = value.trim
      normalized.nonEmpty &&
      !normalized.contains("x") &&
      !normalized.contains("+") &&
      normalized.headOption.exists(ch => "KQRBN".contains(ch))
    }

  private def hasTrace(
      entry: BookmakerOutputEntry,
      needle: String
  ): Boolean =
    (entry.plannerOwnerCandidates ++ entry.plannerAdmittedFamilies ++ entry.plannerProposedFamilyMappings)
      .exists(_.contains(needle))

  private def representativeOrdering(
      row: QuietRichRow
  ): (Int, Int, String, String) =
    val statusPriority =
      row.status match
        case Status.Eligible        => 0
        case Status.UpstreamBlocked => 1
        case Status.NonEligible     => 2
        case _                      => 3
    (
      statusPriority,
      row.quietnessSpreadCp.getOrElse(Int.MaxValue),
      row.sliceKind,
      row.sampleId
    )

  private def clean(
      raw: String
  ): Option[String] =
    Option(raw).map(_.trim).filter(_.nonEmpty)

  private val quietSupportRubrics =
    List(
      EvalRubric.Clarity,
      EvalRubric.MoveAttributionCorrectness,
      EvalRubric.PracticalUsefulness,
      EvalRubric.DryButTruePenalty,
      EvalRubric.OverclaimPenalty
    )

  private val sourceInventoryRows =
    List(
      StrategicSupportSourceInventoryRow(
        source = "MoveDelta.pv_delta",
        quietBucketSuitability =
          List(
            Bucket.LongStructuralSqueeze,
            Bucket.SlowRouteImprovement,
            Bucket.PressureMaintenanceWithoutImmediateTactic,
            Bucket.TransitionHeavyEndgames
          ),
        ownerSupportRole = SourceRole.DirectOwner,
        reusePossible = true,
        notes =
      "Already move-linked and legal in quiet/conversion scenes; the quiet-support lane can reuse it as the primary change anchor without minting a new owner."
      ),
      StrategicSupportSourceInventoryRow(
        source = "OpeningRelation.translator",
        quietBucketSuitability = List(Bucket.OpeningDeviationAfterStableDevelopment),
        ownerSupportRole = SourceRole.DirectOwner,
        reusePossible = true,
        notes =
          "Direct opening-domain owner only when the move-linked translator is present. Raw opening precedent summary stays support-only and is not reopened as a direct owner."
      ),
      StrategicSupportSourceInventoryRow(
        source = "EndgameTransition.translator",
        quietBucketSuitability = List(Bucket.TransitionHeavyEndgames),
        ownerSupportRole = SourceRole.DirectOwner,
        reusePossible = true,
        notes =
          "Direct endgame-domain owner only when the translated transition sentence is present. Raw endgame hints remain support-only."
      ),
      StrategicSupportSourceInventoryRow(
        source = "Digest.route/structure/pressure/restriction",
        quietBucketSuitability =
          List(
            Bucket.ProphylaxisRestraint,
            Bucket.LongStructuralSqueeze,
            Bucket.SlowRouteImprovement,
            Bucket.PressureMaintenanceWithoutImmediateTactic,
            Bucket.TransitionHeavyEndgames
          ),
        ownerSupportRole = SourceRole.SupportOnly,
        reusePossible = true,
        notes =
          "NarrativeSignalDigestBuilder already carries route, structural cue, practical pressure, and restriction-style material. These are support-only and safe for one extra sentence."
      ),
      StrategicSupportSourceInventoryRow(
        source = "planner-owned primary plus existing support material",
        quietBucketSuitability = Bucket.all,
        ownerSupportRole = SourceRole.SupportOnly,
        reusePossible = true,
        notes =
      "The quiet-support lane does not mint a new owner family. It reuses already-admitted quiet/domain owners and only adds bounded support beneath them."
      ),
      StrategicSupportSourceInventoryRow(
        source = "QuietMoveIntentBuilder",
        quietBucketSuitability =
          List(
            Bucket.ProphylaxisRestraint,
            Bucket.SlowRouteImprovement,
            Bucket.TransitionHeavyEndgames
          ),
        ownerSupportRole = SourceRole.DirectOwnerOrFallback,
        reusePossible = true,
        notes =
          "Usable only under the existing certification gate. No latent, pv-coupled, or deferred revival is allowed; otherwise it remains exact-factual fallback only."
      )
    )

  private val modalityEnvelopeRows =
    List(
      ModalityEnvelopeRow(
        status = "allowed",
        verbFamily = "availability",
        examples = List("keeps available", "continues to allow", "keeps the route open"),
        allowedWhen = "Use when the move preserves a route, square, or plan branch without proving that the follow-up is forced."
      ),
      ModalityEnvelopeRow(
        status = "allowed",
        verbFamily = "maintenance",
        examples = List("maintains pressure", "keeps the squeeze coordinated", "holds the pressure"),
        allowedWhen = "Use when route/structure/pressure digest says the move preserves an existing bind rather than creating a new decisive claim."
      ),
      ModalityEnvelopeRow(
        status = "allowed",
        verbFamily = "restriction",
        examples = List("limits", "holds back", "cuts down the immediate counterplay squares"),
        allowedWhen = "Use only when a move-linked restriction digest exists; stay local and avoid implying total shutdown."
      ),
      ModalityEnvelopeRow(
        status = "allowed",
        verbFamily = "transition",
        examples = List("reinforces", "confirms transition", "keeps the technical task in view"),
        allowedWhen = "Use for move-linked opening/endgame translators or clearly move-linked transition support. Do not upgrade raw hints into ownership."
      ),
      ModalityEnvelopeRow(
        status = "discouraged",
        verbFamily = "thesis growth",
        examples = List("builds toward", "leans into", "takes over"),
        allowedWhen = "These often overstate ownership in quiet scenes. Keep them out unless another truth family already owns the claim."
      ),
      ModalityEnvelopeRow(
        status = "forbidden",
        verbFamily = "proof-free strong verbs",
        examples = List("prepares", "launches", "forces", "secures", "neutralizes"),
      allowedWhen = "Forbidden in the default quiet-support envelope because they imply unearned future proof or total control."
      )
    )

  private val strongerVerbRows =
    List(
      ModalityEnvelopeRow(
        status = "proof_backed_only",
        verbFamily = "forcing",
        examples = List("forces", "secures"),
        allowedWhen = "Only when decisive truth already certifies an only-move, tactical refutation, or conversion owner outside the quiet-support path."
      ),
      ModalityEnvelopeRow(
        status = "proof_backed_only",
        verbFamily = "shutdown",
        examples = List("neutralizes", "shuts down"),
        allowedWhen = "Only when move-linked restriction evidence names the concrete resource and the truth layer already treats the move as the owner."
      )
    )

  private val rewriteExamples =
    List(
      ModalityRewriteExample(
        bucket = Bucket.ProphylaxisRestraint,
        before = "This neutralizes Black's queenside play.",
        after = "This limits Black's queenside counterplay right away.",
        reason = "The rewrite keeps the claim local and does not imply a total shutdown."
      ),
      ModalityRewriteExample(
        bucket = Bucket.LongStructuralSqueeze,
        before = "This secures the squeeze.",
        after = "This keeps the squeeze coordinated.",
        reason = "Coordination is support-level truth; securing the whole result is stronger than the proof."
      ),
      ModalityRewriteExample(
        bucket = Bucket.SlowRouteImprovement,
        before = "This prepares the rook lift.",
        after = "This keeps the rook route available for later.",
      reason = "The quiet-support lane can name availability, not an unsupported future plan."
      ),
      ModalityRewriteExample(
        bucket = Bucket.OpeningDeviationAfterStableDevelopment,
        before = "This launches a new opening plan.",
        after = "This keeps the game on a less familiar opening route.",
        reason = "Opening-relation prose should stay route-level unless the translator proves more."
      ),
      ModalityRewriteExample(
        bucket = Bucket.TransitionHeavyEndgames,
        before = "This secures the rook ending.",
        after = "This confirms the transition into a rook ending.",
        reason = "The move can confirm the technical phase shift without claiming that the ending is already secured."
      ),
      ModalityRewriteExample(
        bucket = Bucket.PressureMaintenanceWithoutImmediateTactic,
        before = "This forces White into passivity.",
        after = "This maintains the pressure and keeps White's active options narrower.",
        reason = "Quiet support can describe the pressure boundary, not a forced result."
      )
    )

  private val quietSupportPlans =
    List(
      SurfaceQuietSupportPlan(
        surface = "Bookmaker",
        attachMode = "keep existing main claim, add at most one quiet support sentence",
        sentenceBudget = 1,
        allowedSources =
          List(
            "MoveDelta.pv_delta",
            "OpeningRelation.translator",
            "EndgameTransition.translator",
            "Digest.route",
            "Digest.structure",
            "Digest.pressure",
            "Digest.restriction"
          ),
        safeBecause =
      "Bookmaker already has supportPrimary/supportSecondary slots. The quiet-support lane can fill one of them only when quietness passes and the modality envelope stays inside support-level verbs."
      ),
      SurfaceQuietSupportPlan(
        surface = "Chronicle",
        attachMode = "claim-only replay filler only under the existing planner owner",
        sentenceBudget = 1,
        allowedSources =
          List(
            "MoveDelta.pv_delta",
            "OpeningRelation.translator",
            "EndgameTransition.translator",
            "Digest.route",
            "Digest.structure",
            "Digest.pressure"
          ),
        safeBecause =
      "Chronicle can replay one bounded quiet-support sentence only when the planner-selected owner already stays on MoveDelta/pv_delta and the rendered replay surface would otherwise be claim-only."
      ),
      SurfaceQuietSupportPlan(
        surface = "Active",
        attachMode = "diagnostic_only",
        sentenceBudget = 0,
        allowedSources = Nil,
        safeBecause =
      "The contrast-support lane already cut Active from signoff scope. Reopening attach logic here would mix validator debt into a quiet-support experiment."
      )
    )

  object EligibilityProfile:
    val ActiveBuckets =
      Set(
        Bucket.LongStructuralSqueeze,
        Bucket.SlowRouteImprovement,
        Bucket.PressureMaintenanceWithoutImmediateTactic
      )
    val AllowedSupportSources = Set("Digest.route", "Digest.structure", "Digest.pressure")
    val ForbiddenVerbStems = List("prepare", "launch", "force", "secure", "neutraliz")

  private def beforeQuietSceneEligible(sceneType: Option[String]): Boolean =
    sceneType.forall(scene => scene == "quiet_improvement" || scene == "transition_conversion")

  object QuietSupportLane:
    val BaselineSelected = "quiet-rich eligible / baseline selected rows"
    val RuntimeGatePass = "quiet-rich eligible / runtime gate pass rows"
    val RuntimeGateFail = "quiet-rich eligible / runtime gate fail rows"
    val EligibleStable = "quiet-rich eligible / planner owner-question unchanged rows"
    val EligibleDrift = "quiet-rich eligible / non-target drift rows"

  object IsolationCategory:
    val BaselineSelected = "baseline_selected"
    val RuntimeGatePass = "runtime_gate_pass"
    val SelectorMismatch = "runtime_gate_fail / selector_mismatch"
    val IngressRegression = "runtime_gate_fail / ingress_regression"
    val NonTargetDrift = "non_target_drift"
    val BlockedFallbackSpike = "blocked_fallback_spike"

  final case class QuietSupportSelectorRow(
      schemaVersion: String = Schema.QuietSupportSelectorVersion,
      sampleId: String,
      gameKey: String,
      bucket: String,
      playedSan: String,
      lane: String,
      selected: Boolean,
      selectionReasons: List[String],
      beforeStatus: String,
      beforeFallbackMode: String,
      directSources: List[String],
      supportSources: List[String],
      plannerSceneType: Option[String],
      plannerSelectedOwnerFamily: Option[String],
      plannerSelectedOwnerSource: Option[String],
      commentary: String
  )
  object QuietSupportSelectorRow:
    given Format[QuietSupportSelectorRow] = Json.format[QuietSupportSelectorRow]

  final case class QuietSupportEvalRow(
      schemaVersion: String = Schema.QuietSupportEvalVersion,
      sampleId: String,
      gameKey: String,
      bucket: String,
      playedSan: String,
      lane: String,
      selected: Boolean,
      changed: Boolean,
      selectorDelta: Int,
      beforeStatus: String,
      afterStatus: Option[String],
      beforeFallbackMode: String,
      afterFallbackMode: String,
      fallbackIncrease: Boolean,
      beforeSceneType: Option[String],
      afterSceneType: Option[String],
      beforeSceneReasons: List[String],
      afterSceneReasons: List[String],
      beforeQuestion: Option[String],
      afterQuestion: Option[String],
      beforeOwnerFamily: Option[String],
      afterOwnerFamily: Option[String],
      beforeOwnerSource: Option[String],
      afterOwnerSource: Option[String],
      ownerDivergence: Boolean,
      questionDivergence: Boolean,
      ownerQuestionUnchanged: Boolean,
      changeFamily: String,
      runtimeGateClassification: String,
      isolationClassification: String,
      runtimeGatePassed: Option[Boolean],
      runtimeGateRejectReasons: List[String],
      beforePlannerOwnerCandidates: List[String],
      afterPlannerOwnerCandidates: List[String],
      beforeRawChoiceType: Option[String],
      afterRawChoiceType: Option[String],
      beforeRawDecisionPresent: Option[Boolean],
      afterRawDecisionPresent: Option[Boolean],
      beforeRawDecisionIngressReason: Option[String],
      afterRawDecisionIngressReason: Option[String],
      beforeRawPvDeltaAvailable: Option[Boolean],
      afterRawPvDeltaAvailable: Option[Boolean],
      beforeRawPvDeltaIngressReason: Option[String],
      afterRawPvDeltaIngressReason: Option[String],
      beforeRawPvDeltaResolvedThreatsPresent: Option[Boolean],
      afterRawPvDeltaResolvedThreatsPresent: Option[Boolean],
      beforeRawPvDeltaNewOpportunitiesPresent: Option[Boolean],
      afterRawPvDeltaNewOpportunitiesPresent: Option[Boolean],
      beforeRawPvDeltaPlanAdvancementsPresent: Option[Boolean],
      afterRawPvDeltaPlanAdvancementsPresent: Option[Boolean],
      beforeRawPvDeltaConcessionsPresent: Option[Boolean],
      afterRawPvDeltaConcessionsPresent: Option[Boolean],
      beforeSanitizedDecisionPresent: Option[Boolean],
      afterSanitizedDecisionPresent: Option[Boolean],
      beforeSanitizedDecisionIngressReason: Option[String],
      afterSanitizedDecisionIngressReason: Option[String],
      beforeSanitizedPvDeltaAvailable: Option[Boolean],
      afterSanitizedPvDeltaAvailable: Option[Boolean],
      beforeSanitizedPvDeltaIngressReason: Option[String],
      afterSanitizedPvDeltaIngressReason: Option[String],
      beforeTruthClass: Option[String],
      afterTruthClass: Option[String],
      beforeTruthReasonFamily: Option[String],
      afterTruthReasonFamily: Option[String],
      beforeTruthFailureMode: Option[String],
      afterTruthFailureMode: Option[String],
      beforeTruthChosenMatchesBest: Option[Boolean],
      afterTruthChosenMatchesBest: Option[Boolean],
      beforeTruthOnlyMoveDefense: Option[Boolean],
      afterTruthOnlyMoveDefense: Option[Boolean],
      beforeTruthBenchmarkCriticalMove: Option[Boolean],
      afterTruthBenchmarkCriticalMove: Option[Boolean],
      beforePlannerTacticalFailureSources: List[String],
      afterPlannerTacticalFailureSources: List[String],
      beforePlannerForcingDefenseSources: List[String],
      afterPlannerForcingDefenseSources: List[String],
      beforePlannerMoveDeltaSources: List[String],
      afterPlannerMoveDeltaSources: List[String],
      runtimePvDeltaAvailable: Option[Boolean],
      runtimeSignalDigestAvailable: Option[Boolean],
      runtimeMoveLinkedPvDeltaAnchorAvailable: Option[Boolean],
      quietSupportLiftApplied: Boolean,
      quietSupportRejectReasons: List[String],
      quietSupportCandidateBucket: Option[String],
      quietSupportCandidateSourceKinds: List[String],
      quietSupportCandidateVerbFamily: Option[String],
      quietSupportCandidateText: Option[String],
      strongerVerbLeakage: Boolean,
      strongerVerbLeakageTerms: List[String],
      beforeText: String,
      afterText: String,
      beforeRubric: EvaluationRubricScores,
      afterRubric: EvaluationRubricScores,
      beforeSelectorScore: Int,
      afterSelectorScore: Int,
      upstreamPrimarySubsystem: Option[String],
      upstreamPrimaryCause: Option[String],
      upstreamSupportingSignals: List[String],
      summary: String
  )
  object QuietSupportEvalRow:
    given Format[QuietSupportEvalRow] = Json.format[QuietSupportEvalRow]

  final case class QuietSupportLaneSummary(
      lane: String,
      rowCount: Int,
      selectedCount: Int,
      changedCount: Int,
      fallbackIncreaseCount: Int,
      quietSupportLiftAppliedCount: Int,
      strongerVerbLeakageCount: Int,
      ownerDivergenceCount: Int,
      questionDivergenceCount: Int,
      avgSelectorBefore: Double,
      avgSelectorAfter: Double,
      avgPracticalUsefulnessBefore: Double,
      avgPracticalUsefulnessAfter: Double,
      avgDryButTruePenaltyBefore: Double,
      avgDryButTruePenaltyAfter: Double,
      avgMoveAttributionCorrectnessBefore: Double,
      avgMoveAttributionCorrectnessAfter: Double,
      avgOverclaimPenaltyBefore: Double,
      avgOverclaimPenaltyAfter: Double
  )
  object QuietSupportLaneSummary:
    given Format[QuietSupportLaneSummary] = Json.format[QuietSupportLaneSummary]

  final case class ChronicleMirrorRepresentative(
      bookmakerSampleId: String,
      chronicleSampleId: String,
      bucket: String,
      lane: String,
      playedSan: String,
      quietSupportApplied: Boolean,
      ownerDivergence: Boolean,
      questionDivergence: Boolean,
      strongerVerbLeakageTerms: List[String],
      blockedLaneContamination: Boolean,
      crossSurfaceOwnerDivergence: Boolean,
      beforeNarrative: Option[String],
      afterNarrative: Option[String],
      quietSupportCandidateText: Option[String]
  )
  object ChronicleMirrorRepresentative:
    given Format[ChronicleMirrorRepresentative] = Json.format[ChronicleMirrorRepresentative]

  final case class ChronicleMirrorSummary(
      beforeSource: String,
      afterSource: String,
      selectedQuietRowCount: Int,
      blockedRowCount: Int,
      quietSupportAppliedCount: Int,
      ownerDivergenceCount: Int,
      questionDivergenceCount: Int,
      strongerVerbLeakageCount: Int,
      blockedLaneContaminationCount: Int,
      crossSurfaceOwnerDivergenceCount: Int,
      representatives: List[ChronicleMirrorRepresentative],
      acceptanceNotes: List[String]
  )
  object ChronicleMirrorSummary:
    given Format[ChronicleMirrorSummary] = Json.format[ChronicleMirrorSummary]

  final case class QuietSupportSummary(
      schemaVersion: String = Schema.QuietSupportSummaryVersion,
      beforeSource: String,
      afterSource: String,
      selectorRowCount: Int,
      eligibleSelectedCount: Int,
      baselineSelectedCount: Int,
      runtimeGatePassCount: Int,
      runtimeGateFailCount: Int,
      selectorMismatchCount: Int,
      ingressRegressionCount: Int,
      nonTargetDriftCount: Int,
      blockedFallbackSpikeCount: Int,
      eligibleStableSelectedCount: Int,
      eligibleDriftSelectedCount: Int,
      blockedRowCount: Int,
      laneSummaries: List[QuietSupportLaneSummary],
      acceptanceNotes: List[String],
      chronicleMirror: Option[ChronicleMirrorSummary] = None
  )
  object QuietSupportSummary:
    given Format[QuietSupportSummary] = Json.using[Json.WithDefaultValues].format[QuietSupportSummary]

  private final case class UpstreamCauseTrace(
      primarySubsystem: String,
      primaryCause: String,
      supportingSignals: List[String]
  )

  private final case class ChronicleMirrorRow(
      selector: QuietSupportSelectorRow,
      chronicleSampleId: String,
      quietSupportApplied: Boolean,
      ownerDivergence: Boolean,
      questionDivergence: Boolean,
      strongerVerbLeakageTerms: List[String],
      blockedLaneContamination: Boolean,
      crossSurfaceOwnerDivergence: Boolean,
      beforeNarrative: Option[String],
      afterNarrative: Option[String],
      quietSupportCandidateText: Option[String]
  )

  private final case class QuietSupportTraceView(
      liftApplied: Option[Boolean],
      rejectReasons: List[String],
      runtimeGatePassed: Option[Boolean],
      runtimeGateRejectReasons: List[String],
      runtimeSceneType: Option[String],
      runtimeSelectedOwnerFamily: Option[String],
      runtimeSelectedOwnerSource: Option[String],
      runtimePvDeltaAvailable: Option[Boolean],
      runtimeSignalDigestAvailable: Option[Boolean],
      runtimeMoveLinkedPvDeltaAnchorAvailable: Option[Boolean],
      candidateBucket: Option[String],
      candidateSourceKinds: List[String],
      candidateVerbFamily: Option[String],
      candidateText: Option[String]
  )

  private def quietSupportTraceView(
      entry: BookmakerOutputEntry
  ): QuietSupportTraceView =
    QuietSupportTraceView(
      liftApplied = entry.quietSupportLiftApplied,
      rejectReasons = entry.quietSupportRejectReasons,
      runtimeGatePassed = entry.quietSupportRuntimeGatePassed,
      runtimeGateRejectReasons = entry.quietSupportRuntimeGateRejectReasons,
      runtimeSceneType = entry.quietSupportRuntimeSceneType,
      runtimeSelectedOwnerFamily = entry.quietSupportRuntimeSelectedOwnerFamily,
      runtimeSelectedOwnerSource = entry.quietSupportRuntimeSelectedOwnerSource,
      runtimePvDeltaAvailable = entry.quietSupportRuntimePvDeltaAvailable,
      runtimeSignalDigestAvailable = entry.quietSupportRuntimeSignalDigestAvailable,
      runtimeMoveLinkedPvDeltaAnchorAvailable = entry.quietSupportRuntimeMoveLinkedPvDeltaAnchorAvailable,
      candidateBucket = entry.quietSupportCandidateBucket,
      candidateSourceKinds = entry.quietSupportCandidateSourceKinds,
      candidateVerbFamily = entry.quietSupportCandidateVerbFamily,
      candidateText = entry.quietSupportCandidateText
    )

  private def buildBaselineSelectorRows(
      beforeEntries: List[BookmakerOutputEntry],
      beforeSource: String
  ): List[QuietSupportSelectorRow] =
    val beforeReport = buildQuietRichReport(beforeEntries, beforeSource)
    beforeReport.rows
      .filter(row => EligibilityProfile.ActiveBuckets.contains(row.bucket))
      .map { row =>
        val selectedReasons =
          List(
            Option.when(row.status == Status.Eligible)("baseline_eligible"),
            Option.when(row.bookmakerFallbackMode == "exact_factual")("before_exact_factual"),
            Option.when(row.directSources.contains("MoveDelta.pv_delta"))("move_delta_pv_delta"),
            Option.when(row.supportSources.exists(EligibilityProfile.AllowedSupportSources.contains))("digest_support_whitelisted"),
            Option.when(beforeQuietSceneEligible(row.plannerSceneType))("before_quiet_scene")
          ).flatten
        val selected = selectedReasons.size == 5
        QuietSupportSelectorRow(
          sampleId = row.sampleId,
          gameKey = row.gameKey,
          bucket = row.bucket,
          playedSan = row.playedSan,
          lane = row.lane,
          selected = selected,
          selectionReasons =
            if selected then selectedReasons
            else
              (
                row.statusReasons ++
                  selectedReasons ++
                  Option
                    .when(!beforeQuietSceneEligible(row.plannerSceneType))(
                      s"before_scene_not_quiet:${row.plannerSceneType.getOrElse("none")}"
                    )
                    .toList
              ).distinct,
          beforeStatus = row.status,
          beforeFallbackMode = row.bookmakerFallbackMode,
          directSources = row.directSources,
          supportSources = row.supportSources,
          plannerSceneType = row.plannerSceneType,
          plannerSelectedOwnerFamily = row.plannerSelectedOwnerFamily,
          plannerSelectedOwnerSource = row.plannerSelectedOwnerSource,
          commentary = row.commentary
        )
      }
      .sortBy(row => (row.bucket, row.sampleId))

  def buildQuietSupportSelectorRows(
      beforeEntries: List[BookmakerOutputEntry],
      beforeSource: String
  ): List[QuietSupportSelectorRow] =
    buildBaselineSelectorRows(beforeEntries, beforeSource)

  private def buildBaselineEvaluation(
      beforeEntries: List[BookmakerOutputEntry],
      afterEntries: List[BookmakerOutputEntry],
      beforeSource: String,
      afterSource: String
  ): (List[QuietSupportSelectorRow], List[QuietSupportEvalRow], QuietSupportSummary) =
    val beforeReport = buildQuietRichReport(beforeEntries, beforeSource)
    val afterReport = buildQuietRichReport(afterEntries, afterSource)
    val beforeRows =
      beforeReport.rows
        .filter(row => EligibilityProfile.ActiveBuckets.contains(row.bucket))
        .map(row => rowKey(row.sampleId, row.bucket) -> row)
        .toMap
    val afterRows =
      afterReport.rows
        .filter(row => EligibilityProfile.ActiveBuckets.contains(row.bucket))
        .map(row => rowKey(row.sampleId, row.bucket) -> row)
        .toMap
    val beforeEntriesBySample = beforeEntries.map(entry => entry.sampleId -> entry).toMap
    val afterEntriesBySample = afterEntries.map(entry => entry.sampleId -> entry).toMap
    val selectorRows = buildBaselineSelectorRows(beforeEntries, beforeSource)

    val evalRows =
      selectorRows.flatMap { selector =>
        beforeRows.get(rowKey(selector.sampleId, selector.bucket)).flatMap { beforeRow =>
          beforeEntriesBySample.get(selector.sampleId).map { beforeEntry =>
            val afterEntry = afterEntriesBySample.get(selector.sampleId)
            val afterRow = afterRows.get(rowKey(selector.sampleId, selector.bucket))
            val afterQuietSupport = afterEntry.map(quietSupportTraceView)
            val beforeRubric =
              quietSupportRubric(
                candidateText = beforeEntry.commentary,
                fallbackMode = beforeEntry.bookmakerFallbackMode,
                baselineQuestion = beforeEntry.plannerSelectedQuestion,
                baselineOwnerFamily = beforeEntry.plannerSelectedOwnerFamily,
                baselineOwnerSource = beforeEntry.plannerSelectedOwnerSource,
                currentQuestion = beforeEntry.plannerSelectedQuestion,
                currentOwnerFamily = beforeEntry.plannerSelectedOwnerFamily,
                currentOwnerSource = beforeEntry.plannerSelectedOwnerSource,
                strongerVerbLeakageTerms = Nil
              )
            val leakageTerms =
              strongerVerbLeakageTerms(beforeEntry.commentary, afterEntry.map(_.commentary).getOrElse(""))
            val afterRubric =
              quietSupportRubric(
                candidateText = afterEntry.map(_.commentary).getOrElse(""),
                fallbackMode = afterEntry.map(_.bookmakerFallbackMode).getOrElse("missing_after"),
                baselineQuestion = beforeEntry.plannerSelectedQuestion,
                baselineOwnerFamily = beforeEntry.plannerSelectedOwnerFamily,
                baselineOwnerSource = beforeEntry.plannerSelectedOwnerSource,
                currentQuestion = afterEntry.flatMap(_.plannerSelectedQuestion),
                currentOwnerFamily = afterEntry.flatMap(_.plannerSelectedOwnerFamily),
                currentOwnerSource = afterEntry.flatMap(_.plannerSelectedOwnerSource),
                strongerVerbLeakageTerms = leakageTerms
              )
            val beforeSelection = evaluateSelection(beforeRubric)
            val afterSelection = evaluateSelection(afterRubric)
            val ownerDivergence =
              !sameOpt(beforeEntry.plannerSelectedOwnerFamily, afterEntry.flatMap(_.plannerSelectedOwnerFamily)) ||
                !sameOpt(beforeEntry.plannerSelectedOwnerSource, afterEntry.flatMap(_.plannerSelectedOwnerSource))
            val questionDivergence =
              !sameOpt(beforeEntry.plannerSelectedQuestion, afterEntry.flatMap(_.plannerSelectedQuestion))
            val ownerQuestionUnchanged = !ownerDivergence && !questionDivergence
            val fallbackIncrease =
              fallbackSeverity(afterEntry.map(_.bookmakerFallbackMode).getOrElse("missing_after")) >
                fallbackSeverity(beforeEntry.bookmakerFallbackMode)
            val afterText = afterEntry.map(_.commentary).getOrElse("")
            val runtimeGatePassed = afterQuietSupport.flatMap(_.runtimeGatePassed)
            val runtimeGateRejectReasons =
              afterQuietSupport.map(_.runtimeGateRejectReasons).getOrElse(Nil)
            val runtimeSceneType =
              afterQuietSupport.flatMap(_.runtimeSceneType).orElse(afterEntry.flatMap(_.plannerSceneType))
            val runtimeGateClassification =
              quietSupportRuntimeGateClassification(selector, ownerQuestionUnchanged, runtimeGatePassed)
            val isolationClassification =
              quietSupportIsolationClassification(
                selector = selector,
                beforeEntry = beforeEntry,
                afterEntry = afterEntry,
                ownerQuestionUnchanged = ownerQuestionUnchanged,
                fallbackIncrease = fallbackIncrease
              )
            val upstreamCause =
              quietSupportUpstreamCause(beforeEntry, afterEntry, isolationClassification, fallbackIncrease)
            val changed =
              normalize(beforeEntry.commentary) != normalize(afterText) ||
                beforeEntry.bookmakerFallbackMode != afterEntry.map(_.bookmakerFallbackMode).getOrElse("missing_after")
            val changeFamily =
              quietSupportChangeFamily(selector, ownerQuestionUnchanged)
            val quietSupportLift =
              actualQuietSupportLiftApplied(
                selector = selector,
                beforeEntry = beforeEntry,
                afterEntry = afterEntry,
                afterText = afterText,
                ownerQuestionUnchanged = ownerQuestionUnchanged
              )
            val quietSupportRejectReasons =
              afterQuietSupport.map(_.rejectReasons).getOrElse(Nil)

            QuietSupportEvalRow(
              sampleId = selector.sampleId,
              gameKey = selector.gameKey,
              bucket = selector.bucket,
              playedSan = selector.playedSan,
              lane = selector.lane,
              selected = selector.selected,
              changed = changed,
              selectorDelta = afterSelection.selectorScore - beforeSelection.selectorScore,
              beforeStatus = beforeRow.status,
              afterStatus = afterRow.map(_.status),
              beforeFallbackMode = beforeEntry.bookmakerFallbackMode,
              afterFallbackMode = afterEntry.map(_.bookmakerFallbackMode).getOrElse("missing_after"),
              fallbackIncrease = fallbackIncrease,
              beforeSceneType = beforeEntry.plannerSceneType,
              afterSceneType = runtimeSceneType,
              beforeSceneReasons = beforeEntry.plannerSceneReasons,
              afterSceneReasons = afterEntry.map(_.plannerSceneReasons).getOrElse(Nil),
              beforeQuestion = beforeEntry.plannerSelectedQuestion,
              afterQuestion = afterEntry.flatMap(_.plannerSelectedQuestion),
              beforeOwnerFamily = beforeEntry.plannerSelectedOwnerFamily,
              afterOwnerFamily = afterEntry.flatMap(_.plannerSelectedOwnerFamily),
              beforeOwnerSource = beforeEntry.plannerSelectedOwnerSource,
              afterOwnerSource = afterEntry.flatMap(_.plannerSelectedOwnerSource),
              ownerDivergence = ownerDivergence,
              questionDivergence = questionDivergence,
              ownerQuestionUnchanged = ownerQuestionUnchanged,
              changeFamily = changeFamily,
              runtimeGateClassification = runtimeGateClassification,
              isolationClassification = isolationClassification,
              runtimeGatePassed = runtimeGatePassed,
              runtimeGateRejectReasons = runtimeGateRejectReasons,
              beforePlannerOwnerCandidates = beforeEntry.plannerOwnerCandidates,
              afterPlannerOwnerCandidates = afterEntry.map(_.plannerOwnerCandidates).getOrElse(Nil),
              beforeRawChoiceType = beforeEntry.rawChoiceType,
              afterRawChoiceType = afterEntry.flatMap(_.rawChoiceType),
              beforeRawDecisionPresent = beforeEntry.rawDecisionPresent,
              afterRawDecisionPresent = afterEntry.flatMap(_.rawDecisionPresent),
              beforeRawDecisionIngressReason = beforeEntry.rawDecisionIngressReason,
              afterRawDecisionIngressReason = afterEntry.flatMap(_.rawDecisionIngressReason),
              beforeRawPvDeltaAvailable = beforeEntry.rawPvDeltaAvailable,
              afterRawPvDeltaAvailable = afterEntry.flatMap(_.rawPvDeltaAvailable),
              beforeRawPvDeltaIngressReason = beforeEntry.rawPvDeltaIngressReason,
              afterRawPvDeltaIngressReason = afterEntry.flatMap(_.rawPvDeltaIngressReason),
              beforeRawPvDeltaResolvedThreatsPresent = beforeEntry.rawPvDeltaResolvedThreatsPresent,
              afterRawPvDeltaResolvedThreatsPresent = afterEntry.flatMap(_.rawPvDeltaResolvedThreatsPresent),
              beforeRawPvDeltaNewOpportunitiesPresent = beforeEntry.rawPvDeltaNewOpportunitiesPresent,
              afterRawPvDeltaNewOpportunitiesPresent = afterEntry.flatMap(_.rawPvDeltaNewOpportunitiesPresent),
              beforeRawPvDeltaPlanAdvancementsPresent = beforeEntry.rawPvDeltaPlanAdvancementsPresent,
              afterRawPvDeltaPlanAdvancementsPresent = afterEntry.flatMap(_.rawPvDeltaPlanAdvancementsPresent),
              beforeRawPvDeltaConcessionsPresent = beforeEntry.rawPvDeltaConcessionsPresent,
              afterRawPvDeltaConcessionsPresent = afterEntry.flatMap(_.rawPvDeltaConcessionsPresent),
              beforeSanitizedDecisionPresent = beforeEntry.sanitizedDecisionPresent,
              afterSanitizedDecisionPresent = afterEntry.flatMap(_.sanitizedDecisionPresent),
              beforeSanitizedDecisionIngressReason = beforeEntry.sanitizedDecisionIngressReason,
              afterSanitizedDecisionIngressReason = afterEntry.flatMap(_.sanitizedDecisionIngressReason),
              beforeSanitizedPvDeltaAvailable = beforeEntry.sanitizedPvDeltaAvailable,
              afterSanitizedPvDeltaAvailable = afterEntry.flatMap(_.sanitizedPvDeltaAvailable),
              beforeSanitizedPvDeltaIngressReason = beforeEntry.sanitizedPvDeltaIngressReason,
              afterSanitizedPvDeltaIngressReason = afterEntry.flatMap(_.sanitizedPvDeltaIngressReason),
              beforeTruthClass = beforeEntry.truthClass,
              afterTruthClass = afterEntry.flatMap(_.truthClass),
              beforeTruthReasonFamily = beforeEntry.truthReasonFamily,
              afterTruthReasonFamily = afterEntry.flatMap(_.truthReasonFamily),
              beforeTruthFailureMode = beforeEntry.truthFailureMode,
              afterTruthFailureMode = afterEntry.flatMap(_.truthFailureMode),
              beforeTruthChosenMatchesBest = beforeEntry.truthChosenMatchesBest,
              afterTruthChosenMatchesBest = afterEntry.flatMap(_.truthChosenMatchesBest),
              beforeTruthOnlyMoveDefense = beforeEntry.truthOnlyMoveDefense,
              afterTruthOnlyMoveDefense = afterEntry.flatMap(_.truthOnlyMoveDefense),
              beforeTruthBenchmarkCriticalMove = beforeEntry.truthBenchmarkCriticalMove,
              afterTruthBenchmarkCriticalMove = afterEntry.flatMap(_.truthBenchmarkCriticalMove),
              beforePlannerTacticalFailureSources = beforeEntry.plannerTacticalFailureSources,
              afterPlannerTacticalFailureSources = afterEntry.map(_.plannerTacticalFailureSources).getOrElse(Nil),
              beforePlannerForcingDefenseSources = beforeEntry.plannerForcingDefenseSources,
              afterPlannerForcingDefenseSources = afterEntry.map(_.plannerForcingDefenseSources).getOrElse(Nil),
              beforePlannerMoveDeltaSources = beforeEntry.plannerMoveDeltaSources,
              afterPlannerMoveDeltaSources = afterEntry.map(_.plannerMoveDeltaSources).getOrElse(Nil),
              runtimePvDeltaAvailable = afterQuietSupport.flatMap(_.runtimePvDeltaAvailable),
              runtimeSignalDigestAvailable = afterQuietSupport.flatMap(_.runtimeSignalDigestAvailable),
              runtimeMoveLinkedPvDeltaAnchorAvailable =
                afterQuietSupport.flatMap(_.runtimeMoveLinkedPvDeltaAnchorAvailable),
              quietSupportLiftApplied = quietSupportLift,
              quietSupportRejectReasons = quietSupportRejectReasons,
              quietSupportCandidateBucket = afterQuietSupport.flatMap(_.candidateBucket),
              quietSupportCandidateSourceKinds = afterQuietSupport.map(_.candidateSourceKinds).getOrElse(Nil),
              quietSupportCandidateVerbFamily = afterQuietSupport.flatMap(_.candidateVerbFamily),
              quietSupportCandidateText = afterQuietSupport.flatMap(_.candidateText),
              strongerVerbLeakage = leakageTerms.nonEmpty,
              strongerVerbLeakageTerms = leakageTerms,
              beforeText = beforeEntry.commentary,
              afterText = afterText,
              beforeRubric = beforeRubric,
              afterRubric = afterRubric,
              beforeSelectorScore = beforeSelection.selectorScore,
              afterSelectorScore = afterSelection.selectorScore,
              upstreamPrimarySubsystem = upstreamCause.map(_.primarySubsystem),
              upstreamPrimaryCause = upstreamCause.map(_.primaryCause),
              upstreamSupportingSignals = upstreamCause.map(_.supportingSignals).getOrElse(Nil),
              summary =
                quietSupportSummaryLine(
                  selector = selector,
                  beforeEntry = beforeEntry,
                  afterEntry = afterEntry,
                  beforeSelection = beforeSelection.selectorScore,
                  afterSelection = afterSelection.selectorScore,
                  fallbackIncrease = fallbackIncrease,
                  ownerDivergence = ownerDivergence,
                  questionDivergence = questionDivergence,
                  leakageTerms = leakageTerms,
                  upstreamCause = upstreamCause
                )
            )
          }
        }
      }

    val laneSummaries =
      List(
        buildQuietSupportLaneSummary(Lane.Eligible, evalRows.filter(_.lane == Lane.Eligible)),
        buildQuietSupportLaneSummary(QuietSupportLane.BaselineSelected, evalRows.filter(row => row.lane == Lane.Eligible && row.selected)),
        buildQuietSupportLaneSummary(
          QuietSupportLane.RuntimeGatePass,
          evalRows.filter(_.runtimeGateClassification == QuietSupportLane.RuntimeGatePass)
        ),
        buildQuietSupportLaneSummary(
          QuietSupportLane.RuntimeGateFail,
          evalRows.filter(_.runtimeGateClassification == QuietSupportLane.RuntimeGateFail)
        ),
        buildQuietSupportLaneSummary(
          IsolationCategory.SelectorMismatch,
          evalRows.filter(_.isolationClassification == IsolationCategory.SelectorMismatch)
        ),
        buildQuietSupportLaneSummary(
          IsolationCategory.IngressRegression,
          evalRows.filter(_.isolationClassification == IsolationCategory.IngressRegression)
        ),
        buildQuietSupportLaneSummary(
            IsolationCategory.NonTargetDrift,
            evalRows.filter(_.isolationClassification == IsolationCategory.NonTargetDrift)
        ),
        buildQuietSupportLaneSummary(
          IsolationCategory.BlockedFallbackSpike,
          evalRows.filter(_.isolationClassification == IsolationCategory.BlockedFallbackSpike)
        ),
        buildQuietSupportLaneSummary(QuietSupportLane.EligibleStable, evalRows.filter(_.changeFamily == QuietSupportLane.EligibleStable)),
        buildQuietSupportLaneSummary(QuietSupportLane.EligibleDrift, evalRows.filter(_.changeFamily == QuietSupportLane.EligibleDrift)),
        buildQuietSupportLaneSummary(Lane.Blocked, evalRows.filter(_.lane == Lane.Blocked))
      )
    val eligibleRows = evalRows.filter(row => row.lane == Lane.Eligible && row.selected)
    val baselineSelectedRows = eligibleRows
    val stableEligibleRows = evalRows.filter(_.changeFamily == QuietSupportLane.EligibleStable)
    val runtimeGatePassRows = evalRows.filter(_.runtimeGateClassification == QuietSupportLane.RuntimeGatePass)
    val runtimeGateFailRows = evalRows.filter(_.runtimeGateClassification == QuietSupportLane.RuntimeGateFail)
    val selectorMismatchRows = evalRows.filter(_.isolationClassification == IsolationCategory.SelectorMismatch)
    val ingressRegressionRows = evalRows.filter(_.isolationClassification == IsolationCategory.IngressRegression)
        val nonTargetDriftRows = evalRows.filter(_.isolationClassification == IsolationCategory.NonTargetDrift)
    val blockedFallbackSpikeRows = evalRows.filter(_.isolationClassification == IsolationCategory.BlockedFallbackSpike)
    val driftEligibleRows = evalRows.filter(_.changeFamily == QuietSupportLane.EligibleDrift)
    val blockedRows = evalRows.filter(_.lane == Lane.Blocked)
    val summary =
          QuietSupportSummary(
        beforeSource = beforeSource,
        afterSource = afterSource,
        selectorRowCount = selectorRows.size,
        eligibleSelectedCount = selectorRows.count(row => row.selected && row.lane == Lane.Eligible),
        baselineSelectedCount = baselineSelectedRows.size,
        runtimeGatePassCount = runtimeGatePassRows.size,
        runtimeGateFailCount = runtimeGateFailRows.size,
        selectorMismatchCount = selectorMismatchRows.size,
        ingressRegressionCount = ingressRegressionRows.size,
            nonTargetDriftCount = nonTargetDriftRows.size,
        blockedFallbackSpikeCount = blockedFallbackSpikeRows.size,
        eligibleStableSelectedCount = stableEligibleRows.size,
        eligibleDriftSelectedCount = driftEligibleRows.size,
        blockedRowCount = selectorRows.count(_.lane == Lane.Blocked),
        laneSummaries = laneSummaries,
        acceptanceNotes =
          List(
            s"baseline selected row count = ${baselineSelectedRows.size}",
            acceptanceCountNote(
              label = "runtime gate pass row count",
              value = runtimeGatePassRows.size,
              minimum = 1
            ),
            acceptanceCountNote(
              label = "runtime gate fail row count",
              value = runtimeGateFailRows.size,
              maximum = 0
            ),
            s"selector mismatch row count = ${selectorMismatchRows.size}",
            s"ingress regression row count = ${ingressRegressionRows.size}",
              s"non-target drift row count = ${nonTargetDriftRows.size}",
            s"blocked fallback spike row count = ${blockedFallbackSpikeRows.size}",
            acceptanceNote(
              label = "runtime gate pass practical_usefulness",
              before = averageOf(runtimeGatePassRows)(_.beforeRubric.practicalUsefulness),
              after = averageOf(runtimeGatePassRows)(_.afterRubric.practicalUsefulness),
              target = "up"
            ),
            acceptanceNote(
              label = "runtime gate pass dry_but_true_penalty",
              before = averageOf(runtimeGatePassRows)(_.beforeRubric.dryButTruePenalty),
              after = averageOf(runtimeGatePassRows)(_.afterRubric.dryButTruePenalty),
              target = "down"
            ),
            acceptanceNote(
              label = "runtime gate pass move_attribution_correctness",
              before = averageOf(runtimeGatePassRows)(_.beforeRubric.moveAttributionCorrectness),
              after = averageOf(runtimeGatePassRows)(_.afterRubric.moveAttributionCorrectness),
              target = "hold_or_up"
            ),
            acceptanceNote(
              label = "runtime gate pass overclaim_penalty",
              before = averageOf(runtimeGatePassRows)(_.beforeRubric.overclaimPenalty),
              after = averageOf(runtimeGatePassRows)(_.afterRubric.overclaimPenalty),
              target = "hold_or_down"
            ),
            acceptanceCountNote(
              label = "runtime gate pass actual exact_factual lift count",
      value = runtimeGatePassRows.count(_.quietSupportLiftApplied),
              minimum = 1
            ),
            acceptanceCountNote(
              label = "baseline selected owner/question divergence count",
              value = baselineSelectedRows.count(row => !row.ownerQuestionUnchanged),
              maximum = 0
            ),
            acceptanceCountNote(
              label = "baseline selected stronger verb leakage count",
              value = baselineSelectedRows.count(_.strongerVerbLeakage),
              maximum = 0
            ),
              s"baseline selected non-target drift row count = ${driftEligibleRows.size}",
            acceptanceCountNote(
              label = "baseline selected fallback increase count",
              value = baselineSelectedRows.count(_.fallbackIncrease),
              maximum = 0
            ),
            acceptanceCountNote(
              label = "blocked/non-eligible fallback increase count",
              value = blockedRows.count(_.fallbackIncrease),
              maximum = 0
            ),
            acceptanceCountNote(
              label = "blocked/non-eligible stronger verb leakage count",
              value = blockedRows.count(_.strongerVerbLeakage),
              maximum = 0
            )
          )
      )

    (selectorRows, evalRows.sortBy(row => (row.bucket, row.sampleId)), summary)

  def buildQuietSupportEvaluation(
      beforeEntries: List[BookmakerOutputEntry],
      afterEntries: List[BookmakerOutputEntry],
      beforeSource: String,
      afterSource: String,
      beforeChronicleEntries: List[ChronicleActivePlannerSliceRunner.SliceSurfaceEntry] = Nil,
      afterChronicleEntries: List[ChronicleActivePlannerSliceRunner.SliceSurfaceEntry] = Nil,
      beforeChronicleSource: Option[String] = None,
      afterChronicleSource: Option[String] = None
  ): (List[QuietSupportSelectorRow], List[QuietSupportEvalRow], QuietSupportSummary) =
    val (selectorRows, evalRows, baselineSummary) =
      buildBaselineEvaluation(beforeEntries, afterEntries, beforeSource, afterSource)
    val chronicleMirror =
      Option.when(
        beforeChronicleEntries.nonEmpty ||
          afterChronicleEntries.nonEmpty ||
          beforeChronicleSource.nonEmpty ||
          afterChronicleSource.nonEmpty
      ) {
        buildChronicleMirrorSummary(
          selectorRows = selectorRows,
          afterBookmakerEntries = afterEntries,
          beforeChronicleEntries = beforeChronicleEntries,
          afterChronicleEntries = afterChronicleEntries,
          beforeSource = beforeChronicleSource.getOrElse("chronicle_before_unspecified"),
          afterSource = afterChronicleSource.getOrElse("chronicle_after_unspecified")
        )
      }
    (selectorRows, evalRows, baselineSummary.copy(chronicleMirror = chronicleMirror))

  private def buildChronicleMirrorSummary(
      selectorRows: List[QuietSupportSelectorRow],
      afterBookmakerEntries: List[BookmakerOutputEntry],
      beforeChronicleEntries: List[ChronicleActivePlannerSliceRunner.SliceSurfaceEntry],
      afterChronicleEntries: List[ChronicleActivePlannerSliceRunner.SliceSurfaceEntry],
      beforeSource: String,
      afterSource: String
  ): ChronicleMirrorSummary =
    val afterBookmakerBySample = afterBookmakerEntries.map(entry => entry.sampleId -> entry).toMap
    val beforeChronicleBySample = beforeChronicleEntries.map(entry => entry.sampleId -> entry).toMap
    val afterChronicleBySample = afterChronicleEntries.map(entry => entry.sampleId -> entry).toMap

    val rows =
      selectorRows.map { selector =>
        val chronicleSampleId = chronicleSampleIdFor(selector.sampleId)
        val beforeChronicle = beforeChronicleBySample.get(chronicleSampleId)
        val afterChronicle = afterChronicleBySample.get(chronicleSampleId)
        val bookmakerQuietSupport = afterBookmakerBySample.get(selector.sampleId).map(quietSupportTraceView)
        val replayQuestion = afterChronicle.flatMap(chronicleReplayQuestion)
        val replayOwnerFamily = afterChronicle.flatMap(chronicleReplayOwnerFamily)
        val replayOwnerSource = afterChronicle.flatMap(chronicleReplayOwnerSource)
        val plannerQuestion = afterChronicle.flatMap(chroniclePlannerQuestion)
        val plannerOwnerFamily = afterChronicle.flatMap(chroniclePlannerOwnerFamily)
        val plannerOwnerSource = afterChronicle.flatMap(chroniclePlannerOwnerSource)
        val quietSupportApplied = afterChronicle.exists(_.chronicleReplayQuietSupport.applied)
        val ownerDivergence =
          selector.selected && (
            afterChronicle.isEmpty ||
              !sameOpt(plannerOwnerFamily, replayOwnerFamily) ||
              !sameOpt(plannerOwnerSource, replayOwnerSource)
          )
        val questionDivergence =
          selector.selected && (
            afterChronicle.isEmpty ||
              !sameOpt(plannerQuestion, replayQuestion)
          )
        val beforeNarrative = beforeChronicle.flatMap(chronicleNarrativeText)
        val afterNarrative = afterChronicle.flatMap(chronicleNarrativeText)
        val leakageTerms =
          if selector.selected then strongerVerbLeakageTerms(beforeNarrative.getOrElse(""), afterNarrative.getOrElse(""))
          else Nil
        val replayAsQuietMoveDelta =
          replayOwnerFamily.contains("MoveDelta") &&
            replayOwnerSource.contains("pv_delta")
        val blockedLaneContamination =
          selector.lane == Lane.Blocked && (
            quietSupportApplied ||
              (
                replayAsQuietMoveDelta &&
                  (
                    !sameOpt(plannerOwnerFamily, replayOwnerFamily) ||
                      !sameOpt(plannerOwnerSource, replayOwnerSource)
                  )
              )
          )
        val crossSurfaceOwnerDivergence =
          selector.selected && (
            afterChronicle.isEmpty ||
              !sameOpt(bookmakerQuietSupport.flatMap(_.runtimeSelectedOwnerFamily), replayOwnerFamily) ||
              !sameOpt(bookmakerQuietSupport.flatMap(_.runtimeSelectedOwnerSource), replayOwnerSource)
          )

        ChronicleMirrorRow(
          selector = selector,
          chronicleSampleId = chronicleSampleId,
          quietSupportApplied = quietSupportApplied,
          ownerDivergence = ownerDivergence,
          questionDivergence = questionDivergence,
          strongerVerbLeakageTerms = leakageTerms,
          blockedLaneContamination = blockedLaneContamination,
          crossSurfaceOwnerDivergence = crossSurfaceOwnerDivergence,
          beforeNarrative = beforeNarrative,
          afterNarrative = afterNarrative,
          quietSupportCandidateText =
            afterChronicle.flatMap(entry => entry.chronicleReplayQuietSupport.candidateText)
        )
      }

    val uniqueRows =
      rows
        .groupBy(_.chronicleSampleId)
        .values
        .map(_.sortBy(row => (if row.selector.selected then 0 else 1, row.selector.bucket)).head)
        .toList
        .sortBy(_.chronicleSampleId)
    val selectedQuietRows = uniqueRows.filter(_.selector.selected)
    val blockedRows = uniqueRows.filter(_.selector.lane == Lane.Blocked)
    val highlightedRows =
      (
        selectedQuietRows.filter(_.quietSupportApplied) :::
          blockedRows.filter(_.blockedLaneContamination) :::
          selectedQuietRows.filter(row =>
            row.ownerDivergence || row.questionDivergence || row.crossSurfaceOwnerDivergence ||
              row.strongerVerbLeakageTerms.nonEmpty
          ) :::
          selectedQuietRows.take(3)
      ).distinctBy(_.chronicleSampleId).take(8)

    ChronicleMirrorSummary(
      beforeSource = beforeSource,
      afterSource = afterSource,
      selectedQuietRowCount = selectedQuietRows.size,
      blockedRowCount = blockedRows.size,
      quietSupportAppliedCount = selectedQuietRows.count(_.quietSupportApplied),
      ownerDivergenceCount = selectedQuietRows.count(_.ownerDivergence),
      questionDivergenceCount = selectedQuietRows.count(_.questionDivergence),
      strongerVerbLeakageCount = selectedQuietRows.count(_.strongerVerbLeakageTerms.nonEmpty),
      blockedLaneContaminationCount = blockedRows.count(_.blockedLaneContamination),
      crossSurfaceOwnerDivergenceCount = selectedQuietRows.count(_.crossSurfaceOwnerDivergence),
      representatives =
        highlightedRows.map { row =>
          ChronicleMirrorRepresentative(
            bookmakerSampleId = row.selector.sampleId,
            chronicleSampleId = row.chronicleSampleId,
            bucket = row.selector.bucket,
            lane = row.selector.lane,
            playedSan = row.selector.playedSan,
            quietSupportApplied = row.quietSupportApplied,
            ownerDivergence = row.ownerDivergence,
            questionDivergence = row.questionDivergence,
            strongerVerbLeakageTerms = row.strongerVerbLeakageTerms,
            blockedLaneContamination = row.blockedLaneContamination,
            crossSurfaceOwnerDivergence = row.crossSurfaceOwnerDivergence,
            beforeNarrative = row.beforeNarrative,
            afterNarrative = row.afterNarrative,
            quietSupportCandidateText = row.quietSupportCandidateText
          )
        },
      acceptanceNotes =
        List(
          s"selected quiet row count = ${selectedQuietRows.size}",
          acceptanceCountNote(
            label = "chronicle quiet-support applied count",
            value = selectedQuietRows.count(_.quietSupportApplied),
            minimum = 1
          ),
          acceptanceCountNote(
            label = "chronicle owner divergence count",
            value = selectedQuietRows.count(_.ownerDivergence),
            maximum = 0
          ),
          acceptanceCountNote(
            label = "chronicle question divergence count",
            value = selectedQuietRows.count(_.questionDivergence),
            maximum = 0
          ),
          acceptanceCountNote(
            label = "chronicle stronger-verb leakage count",
            value = selectedQuietRows.count(_.strongerVerbLeakageTerms.nonEmpty),
            maximum = 0
          ),
          acceptanceCountNote(
            label = "chronicle blocked-lane contamination count",
            value = blockedRows.count(_.blockedLaneContamination),
            maximum = 0
          ),
          acceptanceCountNote(
            label = "chronicle cross-surface owner divergence count",
            value = selectedQuietRows.count(_.crossSurfaceOwnerDivergence),
            maximum = 0
          )
        )
    )

  private def renderBaselineSummaryMarkdown(
      summary: QuietSupportSummary,
      selectorRows: List[QuietSupportSelectorRow],
      evalRows: List[QuietSupportEvalRow]
  ): String =
    val selectorBullets =
      if selectorRows.isEmpty then "- none"
      else
        selectorRows.map { row =>
          s"- `${row.sampleId}` san=`${row.playedSan}` bucket=`${row.bucket}` lane=`${row.lane}` selected=`${row.selected}` fallback=`${row.beforeFallbackMode}` reasons=`${row.selectionReasons.mkString(", ")}`"
        }.mkString("\n")

    val evalBullets =
      if evalRows.isEmpty then "- none"
      else
        evalRows.map { row =>
          s"- `${row.sampleId}` san=`${row.playedSan}` bucket=`${row.bucket}` isolation=`${row.isolationClassification}` changeFamily=`${row.changeFamily}` runtimeGate=`${row.runtimeGateClassification}` scene=`${row.beforeSceneType.getOrElse("none")} -> ${row.afterSceneType.getOrElse("none")}` sceneReasons=`${row.beforeSceneReasons.mkString("+")} -> ${row.afterSceneReasons.mkString("+")}` rawChoice=`${row.beforeRawChoiceType.getOrElse("none")} -> ${row.afterRawChoiceType.getOrElse("none")}` rawDecision=`${row.beforeRawDecisionIngressReason.getOrElse("n/a")} -> ${row.afterRawDecisionIngressReason.getOrElse("n/a")}` rawPvDelta=`${row.beforeRawPvDeltaIngressReason.getOrElse("n/a")} -> ${row.afterRawPvDeltaIngressReason.getOrElse("n/a")}` truth=`${row.beforeTruthReasonFamily.getOrElse("none")} -> ${row.afterTruthReasonFamily.getOrElse("none")}` plannerMoveDelta=`${row.beforePlannerMoveDeltaSources.mkString("+")} -> ${row.afterPlannerMoveDeltaSources.mkString("+")}` upstream=`${row.upstreamPrimarySubsystem.getOrElse("none")}:${row.upstreamPrimaryCause.getOrElse("none")}` delta=`${row.selectorDelta}` fallback=`${row.beforeFallbackMode} -> ${row.afterFallbackMode}` lift=`${row.quietSupportLiftApplied}` leakage=`${row.strongerVerbLeakageTerms.mkString(",")}` gateRejects=`${row.runtimeGateRejectReasons.mkString(",")}` rejects=`${row.quietSupportRejectReasons.mkString(",")}` summary=`${row.summary}`"
        }.mkString("\n")

    val laneBullets =
      if summary.laneSummaries.isEmpty then "- none"
      else
        summary.laneSummaries.map { lane =>
          f"- `${lane.lane}` rows=`${lane.rowCount}` selected=`${lane.selectedCount}` changed=`${lane.changedCount}` lift=`${lane.quietSupportLiftAppliedCount}` fallbackIncrease=`${lane.fallbackIncreaseCount}` leakage=`${lane.strongerVerbLeakageCount}` selector=`${lane.avgSelectorBefore}%.2f -> ${lane.avgSelectorAfter}%.2f` practical=`${lane.avgPracticalUsefulnessBefore}%.2f -> ${lane.avgPracticalUsefulnessAfter}%.2f` dry=`${lane.avgDryButTruePenaltyBefore}%.2f -> ${lane.avgDryButTruePenaltyAfter}%.2f`"
        }.mkString("\n")

    val notes =
      if summary.acceptanceNotes.isEmpty then "- none"
      else summary.acceptanceNotes.map(note => s"- $note").mkString("\n")

    val chronicleSection =
      summary.chronicleMirror.map { chronicle =>
        val chronicleNotes =
          if chronicle.acceptanceNotes.isEmpty then "- none"
          else chronicle.acceptanceNotes.map(note => s"- $note").mkString("\n")
        val chronicleRows =
          if chronicle.representatives.isEmpty then "- none"
          else
            chronicle.representatives.map { row =>
              s"- `${row.chronicleSampleId}` san=`${row.playedSan}` bucket=`${row.bucket}` lane=`${row.lane}` applied=`${row.quietSupportApplied}` ownerDivergence=`${row.ownerDivergence}` questionDivergence=`${row.questionDivergence}` crossSurfaceOwnerDivergence=`${row.crossSurfaceOwnerDivergence}` contamination=`${row.blockedLaneContamination}` leakage=`${row.strongerVerbLeakageTerms.mkString(",")}` before=`${row.beforeNarrative.getOrElse("")}` after=`${row.afterNarrative.getOrElse("")}` candidate=`${row.quietSupportCandidateText.getOrElse("")}`"
            }.mkString("\n")
        List(
          "",
          "## Chronicle mirror",
          "",
          s"- before chronicle source: `${chronicle.beforeSource}`",
          s"- after chronicle source: `${chronicle.afterSource}`",
          s"- selected quiet rows: `${chronicle.selectedQuietRowCount}`",
          s"- blocked rows: `${chronicle.blockedRowCount}`",
          s"- quiet-support applied rows: `${chronicle.quietSupportAppliedCount}`",
          s"- owner divergence rows: `${chronicle.ownerDivergenceCount}`",
          s"- question divergence rows: `${chronicle.questionDivergenceCount}`",
          s"- stronger-verb leakage rows: `${chronicle.strongerVerbLeakageCount}`",
          s"- blocked-lane contamination rows: `${chronicle.blockedLaneContaminationCount}`",
          s"- cross-surface owner divergence rows: `${chronicle.crossSurfaceOwnerDivergenceCount}`",
          "",
          "Representative Chronicle rows:",
          "",
          chronicleRows,
          "",
          "Chronicle acceptance notes:",
          "",
          chronicleNotes
        ).mkString("\n")
      }.getOrElse("")

    List(
        "# Quiet-Support Baseline",
      "",
      s"- before source: `${summary.beforeSource}`",
      s"- after source: `${summary.afterSource}`",
      s"- selector rows: `${summary.selectorRowCount}`",
      s"- eligible selected rows: `${summary.eligibleSelectedCount}`",
      s"- baseline selected rows: `${summary.baselineSelectedCount}`",
      s"- runtime gate pass rows: `${summary.runtimeGatePassCount}`",
      s"- runtime gate fail rows: `${summary.runtimeGateFailCount}`",
      s"- selector mismatch rows: `${summary.selectorMismatchCount}`",
      s"- ingress regression rows: `${summary.ingressRegressionCount}`",
      s"- non-target drift rows: `${summary.nonTargetDriftCount}`",
      s"- blocked fallback spike rows: `${summary.blockedFallbackSpikeCount}`",
      s"- eligible stable selected rows: `${summary.eligibleStableSelectedCount}`",
      s"- eligible drift selected rows: `${summary.eligibleDriftSelectedCount}`",
      s"- blocked rows: `${summary.blockedRowCount}`",
      "",
      "## Selector rows",
      "",
      selectorBullets,
      "",
      "## Before / after rows",
      "",
      evalBullets,
      "",
      "## Lane summaries",
      "",
      laneBullets,
      "",
      "## Acceptance notes",
      "",
      notes,
      chronicleSection
    ).mkString("\n")

  def renderQuietSupportSummaryMarkdown(
      summary: QuietSupportSummary,
      selectorRows: List[QuietSupportSelectorRow],
      evalRows: List[QuietSupportEvalRow]
  ): String =
    renderBaselineSummaryMarkdown(summary, selectorRows, evalRows)

  private def buildQuietSupportLaneSummary(
      lane: String,
      rows: List[QuietSupportEvalRow]
  ): QuietSupportLaneSummary =
    QuietSupportLaneSummary(
      lane = lane,
      rowCount = rows.size,
      selectedCount = rows.count(_.selected),
      changedCount = rows.count(_.changed),
      fallbackIncreaseCount = rows.count(_.fallbackIncrease),
      quietSupportLiftAppliedCount = rows.count(_.quietSupportLiftApplied),
      strongerVerbLeakageCount = rows.count(_.strongerVerbLeakage),
      ownerDivergenceCount = rows.count(_.ownerDivergence),
      questionDivergenceCount = rows.count(_.questionDivergence),
      avgSelectorBefore = averageOf(rows)(_.beforeSelectorScore),
      avgSelectorAfter = averageOf(rows)(_.afterSelectorScore),
      avgPracticalUsefulnessBefore = averageOf(rows)(_.beforeRubric.practicalUsefulness),
      avgPracticalUsefulnessAfter = averageOf(rows)(_.afterRubric.practicalUsefulness),
      avgDryButTruePenaltyBefore = averageOf(rows)(_.beforeRubric.dryButTruePenalty),
      avgDryButTruePenaltyAfter = averageOf(rows)(_.afterRubric.dryButTruePenalty),
      avgMoveAttributionCorrectnessBefore = averageOf(rows)(_.beforeRubric.moveAttributionCorrectness),
      avgMoveAttributionCorrectnessAfter = averageOf(rows)(_.afterRubric.moveAttributionCorrectness),
      avgOverclaimPenaltyBefore = averageOf(rows)(_.beforeRubric.overclaimPenalty),
      avgOverclaimPenaltyAfter = averageOf(rows)(_.afterRubric.overclaimPenalty)
    )

  private def quietSupportRubric(
      candidateText: String,
      fallbackMode: String,
      baselineQuestion: Option[String],
      baselineOwnerFamily: Option[String],
      baselineOwnerSource: Option[String],
      currentQuestion: Option[String],
      currentOwnerFamily: Option[String],
      currentOwnerSource: Option[String],
      strongerVerbLeakageTerms: List[String]
  ): EvaluationRubricScores =
    val text = normalize(candidateText)
    val outputPresent = text.nonEmpty
    val supportLift = hasSupportLift(text)
    val concrete = hasConcreteAnchor(text)
    val selectionAligned =
      sameOpt(baselineQuestion, currentQuestion) &&
        sameOpt(baselineOwnerFamily, currentOwnerFamily) &&
        sameOpt(baselineOwnerSource, currentOwnerSource)
    val clarity =
      if !outputPresent then 1
      else if supportLift && concrete then 4
      else if concrete then 3
      else 2
    val moveAttributionCorrectness =
      if !outputPresent then 1
      else if selectionAligned && baselineQuestion.nonEmpty then 5
      else if selectionAligned then 4
      else 2
    val practicalUsefulness =
      if !outputPresent then 0
      else if supportLift && concrete then 4
      else if concrete then 2
      else 1
    val dryButTruePenalty =
      if !outputPresent then 3
      else if supportLift then 0
      else if fallbackSeverity(fallbackMode) >= 1 then 2
      else 1
    val overclaimPenalty =
      Option.when(strongerVerbLeakageTerms.nonEmpty || looksOverclaimy(text))(1).getOrElse(0)

    EvaluationRubricScores(
      clarity = clarity,
      moveAttributionCorrectness = moveAttributionCorrectness,
      contrastUsefulness = 0,
      practicalUsefulness = practicalUsefulness,
      dryButTruePenalty = dryButTruePenalty,
      overclaimPenalty = overclaimPenalty
    )

  private def quietSupportSummaryLine(
      selector: QuietSupportSelectorRow,
      beforeEntry: BookmakerOutputEntry,
      afterEntry: Option[BookmakerOutputEntry],
      beforeSelection: Int,
      afterSelection: Int,
      fallbackIncrease: Boolean,
      ownerDivergence: Boolean,
      questionDivergence: Boolean,
      leakageTerms: List[String],
      upstreamCause: Option[UpstreamCauseTrace]
  ): String =
    val afterQuietSupport = afterEntry.map(quietSupportTraceView)
    List(
      s"lane=${selector.lane}",
      s"selected=${selector.selected}",
      s"runtimeGate=${quietSupportRuntimeGateClassification(selector, !ownerDivergence && !questionDivergence, afterQuietSupport.flatMap(_.runtimeGatePassed))}",
      s"isolation=${quietSupportIsolationClassification(selector, beforeEntry, afterEntry, !ownerDivergence && !questionDivergence, fallbackIncrease)}",
      s"scene=${beforeEntry.plannerSceneType.getOrElse("none")}->${afterQuietSupport.flatMap(_.runtimeSceneType).orElse(afterEntry.flatMap(_.plannerSceneType)).getOrElse("none")}",
      s"rawChoice=${beforeEntry.rawChoiceType.getOrElse("none")}->${afterEntry.flatMap(_.rawChoiceType).getOrElse("none")}",
      s"rawDecision=${beforeEntry.rawDecisionPresent.map(_.toString).getOrElse("n/a")}->${afterEntry.flatMap(_.rawDecisionPresent).map(_.toString).getOrElse("n/a")}",
      s"rawPvDelta=${beforeEntry.rawPvDeltaAvailable.map(_.toString).getOrElse("n/a")}->${afterEntry.flatMap(_.rawPvDeltaAvailable).map(_.toString).getOrElse("n/a")}",
      s"truthReason=${beforeEntry.truthReasonFamily.getOrElse("none")}->${afterEntry.flatMap(_.truthReasonFamily).getOrElse("none")}",
      s"pvDelta=${afterQuietSupport.flatMap(_.runtimePvDeltaAvailable).map(_.toString).getOrElse("n/a")}",
      s"anchor=${afterQuietSupport.flatMap(_.runtimeMoveLinkedPvDeltaAnchorAvailable).map(_.toString).getOrElse("n/a")}",
      s"selector=${beforeSelection}->${afterSelection}",
      s"fallback=${beforeEntry.bookmakerFallbackMode}->${afterEntry.map(_.bookmakerFallbackMode).getOrElse("missing_after")}",
      s"quietSupportLift=${actualQuietSupportLiftApplied(selector, beforeEntry, afterEntry, afterEntry.map(_.commentary).getOrElse(""), !ownerDivergence && !questionDivergence)}",
      s"upstream=${upstreamCause.map(c => s"${c.primarySubsystem}:${c.primaryCause}").getOrElse("none")}",
      s"ownerDivergence=$ownerDivergence",
      s"questionDivergence=$questionDivergence",
      s"fallbackIncrease=$fallbackIncrease",
      s"leakage=${leakageTerms.mkString("+")}"
    ).mkString(" ")

  private def acceptanceNote(
      label: String,
      before: Double,
      after: Double,
      target: String
  ): String =
    val status =
      target match
        case "up" if after > before               => "ok"
        case "down" if after < before             => "ok"
        case "hold_or_up" if after >= before      => "ok"
        case "hold_or_down" if after <= before    => "ok"
        case _                                    => "review"
    f"$label $before%.2f -> $after%.2f [$status]"

  private def acceptanceCountNote(
      label: String,
      value: Int,
      minimum: Int = Int.MinValue,
      maximum: Int = Int.MaxValue
  ): String =
    val status =
      if value >= minimum && value <= maximum then "ok"
      else "review"
    s"$label = $value [$status]"

  private def quietSupportChangeFamily(
      selector: QuietSupportSelectorRow,
      ownerQuestionUnchanged: Boolean
  ): String =
    if selector.lane == Lane.Eligible && selector.selected && ownerQuestionUnchanged then QuietSupportLane.EligibleStable
    else if selector.lane == Lane.Eligible && selector.selected then QuietSupportLane.EligibleDrift
    else selector.lane

  private def quietSupportRuntimeGateClassification(
      selector: QuietSupportSelectorRow,
      ownerQuestionUnchanged: Boolean,
      runtimeGatePassed: Option[Boolean]
  ): String =
    if !(selector.lane == Lane.Eligible && selector.selected) then selector.lane
    else if !ownerQuestionUnchanged then QuietSupportLane.EligibleDrift
    else
      runtimeGatePassed match
        case Some(true)  => QuietSupportLane.RuntimeGatePass
        case Some(false) => QuietSupportLane.RuntimeGateFail
        case None        => QuietSupportLane.BaselineSelected

  private def quietSupportIsolationClassification(
      selector: QuietSupportSelectorRow,
      beforeEntry: BookmakerOutputEntry,
      afterEntry: Option[BookmakerOutputEntry],
      ownerQuestionUnchanged: Boolean,
      fallbackIncrease: Boolean
  ): String =
    val afterQuietSupport = afterEntry.map(quietSupportTraceView)
    val beforeMoveDeltaCandidate =
      beforeEntry.plannerOwnerCandidates.exists(_.contains("MoveDelta:source_kind=pv_delta"))
    val beforeTacticalScene = beforeEntry.plannerSceneType.contains("tactical_failure")
    val afterPlannerOwned = afterEntry.exists(_.bookmakerFallbackMode == "planner_owned")
    val blockedFallbackSpike =
      selector.lane == Lane.Blocked &&
        fallbackIncrease &&
        beforeEntry.bookmakerFallbackMode == "planner_owned" &&
        afterEntry.exists(_.bookmakerFallbackMode == "exact_factual")
    if blockedFallbackSpike then IsolationCategory.BlockedFallbackSpike
    else if selector.lane == Lane.Eligible && selector.selected && !ownerQuestionUnchanged && afterPlannerOwned then
        IsolationCategory.NonTargetDrift
    else if !selector.selected && beforeTacticalScene then
      IsolationCategory.SelectorMismatch
    else if selector.selected && beforeMoveDeltaCandidate && afterQuietSupport.flatMap(_.runtimePvDeltaAvailable).contains(false) then
      IsolationCategory.IngressRegression
    else if selector.selected && afterQuietSupport.flatMap(_.runtimeGatePassed).contains(true) then IsolationCategory.RuntimeGatePass
    else if selector.selected then IsolationCategory.BaselineSelected
    else selector.lane

  private def quietSupportUpstreamCause(
      beforeEntry: BookmakerOutputEntry,
      afterEntry: Option[BookmakerOutputEntry],
      isolationClassification: String,
      fallbackIncrease: Boolean
  ): Option[UpstreamCauseTrace] =
    def signal(label: String, value: String): String = s"$label=$value"
    def afterSignal(label: String, value: Option[String]): String =
      signal(label, value.filter(_.nonEmpty).getOrElse("none"))
    def trace(subsystem: String, cause: String, details: List[String]): Option[UpstreamCauseTrace] =
      Some(UpstreamCauseTrace(subsystem, cause, details.distinct))

    val afterQuietSupport = afterEntry.map(quietSupportTraceView)
    val afterScene = afterQuietSupport.flatMap(_.runtimeSceneType).orElse(afterEntry.flatMap(_.plannerSceneType))
    val afterTruthReason = afterEntry.flatMap(_.truthReasonFamily)
    val afterFailureMode = afterEntry.flatMap(_.truthFailureMode)
    val afterChoiceType = afterEntry.flatMap(_.rawChoiceType)
    val afterDecisionIngress = afterEntry.flatMap(_.rawDecisionIngressReason)
    val afterPvDeltaIngress = afterEntry.flatMap(_.rawPvDeltaIngressReason)
    val afterForcingSources = afterEntry.map(_.plannerForcingDefenseSources).getOrElse(Nil)
    val afterTacticalSources = afterEntry.map(_.plannerTacticalFailureSources).getOrElse(Nil)
    val afterMoveDeltaSources = afterEntry.map(_.plannerMoveDeltaSources).getOrElse(Nil)
    val commonDetails =
      List(
        signal("beforeScene", beforeEntry.plannerSceneType.getOrElse("none")),
        afterSignal("afterScene", afterScene),
        afterSignal("afterTruthReason", afterTruthReason),
        afterSignal("afterFailureMode", afterFailureMode),
        afterSignal("afterChoiceType", afterChoiceType),
        afterSignal("afterDecisionIngress", afterDecisionIngress),
        afterSignal("afterPvDeltaIngress", afterPvDeltaIngress),
        signal("afterForcingSources", afterForcingSources.mkString("+")),
        signal("afterTacticalSources", afterTacticalSources.mkString("+")),
        signal("afterMoveDeltaSources", afterMoveDeltaSources.mkString("+"))
      )

    isolationClassification match
      case IsolationCategory.SelectorMismatch =>
        trace(
          subsystem = "QuietSupportSelector",
          cause =
            if beforeEntry.plannerSceneType.contains("tactical_failure") then
              "baseline_selected_despite_preexisting_tactical_scene"
            else "baseline_selected_despite_runtime_gate_mismatch",
          details =
            commonDetails ++ List(
              signal("beforeSceneReasons", beforeEntry.plannerSceneReasons.mkString("+"))
            )
        )
      case IsolationCategory.IngressRegression =>
        if afterDecisionIngress.contains("style_choice_decision_omitted") then
          trace(
            subsystem = "NarrativeContextBuilder",
            cause = "style_choice_skipped_decision_and_pv_delta",
            details = commonDetails
          )
        else if afterEntry.flatMap(_.rawDecisionPresent).contains(false) then
          trace(
            subsystem = "NarrativeContextBuilder",
            cause = "decision_missing_before_planner",
            details = commonDetails
          )
        else if afterEntry.flatMap(_.rawPvDeltaAvailable).contains(false) then
          trace(
            subsystem = "NarrativeContextBuilder",
            cause = "pv_delta_missing_before_planner",
            details = commonDetails
          )
        else if afterEntry.exists(entry =>
            entry.rawPvDeltaAvailable.contains(true) &&
              entry.sanitizedPvDeltaAvailable.contains(false)
          )
        then
          trace(
            subsystem = "DecisiveTruth",
            cause = "pv_delta_removed_during_truth_sanitization",
            details = commonDetails
          )
        else if afterMoveDeltaSources.isEmpty then
          trace(
            subsystem = "QuestionFirstCommentaryPlanner",
            cause = "move_delta_candidate_not_emitted",
            details = commonDetails
          )
        else
          trace(
            subsystem = "QuestionFirstCommentaryPlanner",
            cause = "scene_reclassified_out_of_quiet_lane",
            details = commonDetails
          )
      case IsolationCategory.NonTargetDrift =>
        if afterForcingSources.contains("threat") then
          trace(
            subsystem = "QuestionFirstCommentaryPlanner",
            cause = "threat_candidate_promoted_forcing_defense",
            details = commonDetails
          )
        else if afterForcingSources.contains("prevented_plan") then
          trace(
            subsystem = "QuestionFirstCommentaryPlanner",
            cause = "prevented_plan_candidate_promoted_forcing_defense",
            details = commonDetails
          )
        else if afterTacticalSources.contains("truth_contract") then
          trace(
            subsystem = "DecisiveTruth",
            cause = "truth_contract_promoted_tactical_failure",
            details = commonDetails
          )
        else if afterForcingSources.contains("truth_contract") || afterEntry.flatMap(_.truthOnlyMoveDefense).contains(true) then
          trace(
            subsystem = "DecisiveTruth",
            cause = "truth_contract_promoted_only_move_defense",
            details = commonDetails
          )
        else
          trace(
            subsystem = "QuestionFirstCommentaryPlanner",
            cause = "owner_question_drift_outside_quiet_support",
            details = commonDetails
          )
      case IsolationCategory.BlockedFallbackSpike =>
        if afterTacticalSources.contains("truth_contract") then
          trace(
            subsystem = "DecisiveTruth",
            cause = "truth_contract_reclassified_blocked_row_as_tactical_failure",
            details = commonDetails :+ signal("fallbackIncrease", fallbackIncrease.toString)
          )
        else if afterMoveDeltaSources.isEmpty && afterEntry.flatMap(_.rawPvDeltaAvailable).contains(false) then
          trace(
            subsystem = "NarrativeContextBuilder",
            cause = "blocked_row_lost_move_delta_ingress",
            details = commonDetails :+ signal("fallbackIncrease", fallbackIncrease.toString)
          )
        else
          trace(
            subsystem = "QuestionFirstCommentaryPlanner",
            cause = "scene_reclassified_and_planner_support_dropped",
            details = commonDetails :+ signal("fallbackIncrease", fallbackIncrease.toString)
          )
      case _ => None

  private def actualQuietSupportLiftApplied(
      selector: QuietSupportSelectorRow,
      beforeEntry: BookmakerOutputEntry,
      afterEntry: Option[BookmakerOutputEntry],
      afterText: String,
      ownerQuestionUnchanged: Boolean
  ): Boolean =
    val stableExactFactualWindow =
      selector.selected &&
        ownerQuestionUnchanged &&
        beforeEntry.bookmakerFallbackMode == "exact_factual" &&
        afterEntry.exists(_.bookmakerFallbackMode == "exact_factual")
    stableExactFactualWindow &&
      afterEntry
        .map(quietSupportTraceView)
        .flatMap(_.liftApplied)
        .getOrElse(
          normalize(beforeEntry.commentary) != normalize(afterText) &&
            hasSupportLift(afterText)
        )

  private def strongerVerbLeakageTerms(
      beforeText: String,
      afterText: String
  ): List[String] =
    val beforeTokens = tokenize(beforeText).toSet
    val addedTokens = tokenize(afterText).filterNot(beforeTokens.contains)
    EligibilityProfile.ForbiddenVerbStems.filter(stem => addedTokens.exists(_.startsWith(stem)))

  private def chronicleSampleIdFor(
      bookmakerSampleId: String
  ): String =
    if bookmakerSampleId.endsWith(":bookmaker") then
      bookmakerSampleId.stripSuffix(":bookmaker") + ":chronicle"
    else bookmakerSampleId

  private def chronicleNarrativeText(
      entry: ChronicleActivePlannerSliceRunner.SliceSurfaceEntry
  ): Option[String] =
    entry.chronicleReplayNarrative
      .orElse(entry.chronicleNarrative)
      .flatMap(text => Option(normalize(text)).filter(_.nonEmpty))

  private def chronicleReplayQuestion(
      entry: ChronicleActivePlannerSliceRunner.SliceSurfaceEntry
  ): Option[String] =
    entry.chronicleReplayPrimaryKind.orElse(entry.chroniclePrimaryKind)

  private def chronicleReplayOwnerFamily(
      entry: ChronicleActivePlannerSliceRunner.SliceSurfaceEntry
  ): Option[String] =
    entry.chronicleReplaySelectedOwnerFamily.orElse(entry.chronicleSelectedOwnerFamily)

  private def chronicleReplayOwnerSource(
      entry: ChronicleActivePlannerSliceRunner.SliceSurfaceEntry
  ): Option[String] =
    entry.chronicleReplaySelectedOwnerSource.orElse(entry.chronicleSelectedOwnerSource)

  private def chroniclePlannerQuestion(
      entry: ChronicleActivePlannerSliceRunner.SliceSurfaceEntry
  ): Option[String] =
    entry.plannerSelectedQuestion.orElse(entry.chroniclePrimaryKind)

  private def chroniclePlannerOwnerFamily(
      entry: ChronicleActivePlannerSliceRunner.SliceSurfaceEntry
  ): Option[String] =
    entry.plannerSelectedOwnerFamily.orElse(entry.chronicleSelectedOwnerFamily)

  private def chroniclePlannerOwnerSource(
      entry: ChronicleActivePlannerSliceRunner.SliceSurfaceEntry
  ): Option[String] =
    entry.plannerSelectedOwnerSource.orElse(entry.chronicleSelectedOwnerSource)

  private def tokenize(
      raw: String
  ): List[String] =
    Option(raw)
      .getOrElse("")
      .toLowerCase
      .split("""[^a-z]+""")
      .toList
      .map(_.trim)
      .filter(_.nonEmpty)

  private def rowKey(
      sampleId: String,
      bucket: String
  ): String =
    s"$sampleId::$bucket"

  private def sameOpt(
      left: Option[String],
      right: Option[String]
  ): Boolean =
    normalize(left.getOrElse("")) == normalize(right.getOrElse(""))

  private def normalize(
      raw: String
  ): String =
    Option(raw).map(_.trim.replaceAll("""\s+""", " ")).getOrElse("")

  private def hasSupportLift(
      text: String
  ): Boolean =
    Option(text).getOrElse("").contains("\n\n") ||
      countSentences(text) > 1 ||
      List("keeps available", "continues to allow", "maintains pressure", "limits", "reinforces", "confirms transition")
        .exists(marker => normalize(text).toLowerCase.contains(marker))

  private def countSentences(
      text: String
  ): Int =
    normalize(text).split("""(?<=[.!?])\s+""").count(_.trim.nonEmpty)

  private def hasConcreteAnchor(
      text: String
  ): Boolean =
    normalize(text).toLowerCase.matches(""".*(\b[a-h][1-8]\b|\b[a-h]-file\b|queenside|kingside|center|counterplay|pressure|route|squeeze|bind|rook|bishop|knight|queen|king).*""")

  private def looksOverclaimy(
      text: String
  ): Boolean =
    List("wins", "winning", "decisive", "forced", "unstoppable", "secured", "neutralized")
      .exists(marker => normalize(text).toLowerCase.contains(marker))

  private def fallbackSeverity(
      mode: String
  ): Int =
    Option(mode).map(_.trim.toLowerCase).getOrElse("") match
      case "planner_owned"               => 0
      case "exact_factual"               => 1
      case "bookmaker_exact_factual"     => 1
      case "missing_after"               => 3
      case value if value.startsWith("omitted") => 3
      case _                             => 2

  private def averageOf[A](
      rows: List[A]
  )(
      value: A => Int
  ): Double =
    if rows.isEmpty then 0.0
    else rows.map(value).sum.toDouble / rows.size.toDouble

