package lila.llm.tools.strategicobject

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path }

import chess.Square
import play.api.libs.json.*

import scala.jdk.CollectionConverters.*

import lila.llm.analysis.DecisiveTruth.toContract
import lila.llm.strategicobject.*

object AccessNetworkCandidateBridgeContractRunner:

  final case class Config(
      inputPath: Path,
      outputAuditPath: Path,
      outputDir: Path
  )

  final case class AuditRouteEvidence(
      origin: String,
      via: List[String],
      target: String,
      allSquares: List[String]
  )

  final case class AuditBoardTruthEvidence(
      objectId: String,
      owner: String,
      rolesDetailed: List[String],
      contestedSquares: List[String],
      route: Option[AuditRouteEvidence]
  )

  final case class AuditTraceSelection(
      sampleIsAnchor: Boolean,
      sampleIsCandidateBridge: Boolean,
      sampleIsSelectedBridge: Boolean,
      sampleIsVisibleMoment: Boolean,
      sampleIsActiveNote: Boolean
  )

  final case class AuditRow(
      sampleId: String,
      fen: String,
      playedUci: String,
      boardTruthEvidence: AuditBoardTruthEvidence,
      traceSelection: AuditTraceSelection,
      surfacedOutcome: Option[String]
  )

  final case class ResidualAssessmentRow(
      residualClaimId: String,
      residualFamily: String,
      residualStatus: String,
      residualSpecificityClass: Option[String],
      broadOverlapBefore: Boolean,
      traceAdmittedSlice: Boolean,
      routeWitnessRetained: Boolean,
      contestedTargetRetained: Boolean,
      admittedAfter: Boolean
  )
  object ResidualAssessmentRow:
    private val generatedWrites = Json.writes[ResidualAssessmentRow]

    given Writes[ResidualAssessmentRow] with
      def writes(row: ResidualAssessmentRow): JsValue =
        generatedWrites.writes(row).as[JsObject] ++ Json.obj(
          "residualSpecificityClass" ->
            row.residualSpecificityClass.fold[JsValue](JsNull)(JsString(_))
        )

  final case class RowDiff(
      sampleId: String,
      beforeClass: String,
      afterClass: String,
      matchedAccessClaimId: Option[String],
      matchedAccessObjectId: Option[String],
      boundaryExplainedBefore: Boolean,
      promotedByThreeGate: Boolean,
      controlRetained: Boolean,
      beforeResidualClaimIds: List[String],
      afterResidualClaimIds: List[String],
      residualAssessments: List[ResidualAssessmentRow],
      note: Option[String]
  )
  object RowDiff:
    private val generatedWrites = Json.writes[RowDiff]

    given Writes[RowDiff] with
      def writes(row: RowDiff): JsValue =
        generatedWrites.writes(row).as[JsObject] ++ Json.obj(
          "matchedAccessClaimId" ->
            row.matchedAccessClaimId.fold[JsValue](JsNull)(JsString(_)),
          "matchedAccessObjectId" ->
            row.matchedAccessObjectId.fold[JsValue](JsNull)(JsString(_)),
          "note" -> row.note.fold[JsValue](JsNull)(JsString(_))
        )

  final case class BeforeAfterClassSummary(
      tranche: String,
      inputPath: String,
      totalRows: Int,
      beforeCounts: Map[String, Int],
      afterCounts: Map[String, Int],
      promotedSampleIds: List[String],
      unexplainedAnchorNoCandidateSampleIds: List[String]
  )
  object BeforeAfterClassSummary:
    given writes: OWrites[BeforeAfterClassSummary] = Json.writes[BeforeAfterClassSummary]

  final case class SameBatchAggregateCompare(
      tranche: String,
      totalRows: Int,
      matchedAccessClaimCount: Int,
      anchorNoCandidateCount: Int,
      anchorNoCandidateBroadBoundaryCount: Int,
      anchorNoCandidateThreeGateCount: Int,
      anchorNoCandidatePromotedCount: Int,
      anchorNoCandidateUnexplainedCount: Int,
      offTraceRetainedCount: Int,
      candidateNotSelectedRetainedCount: Int,
      candidateSelectedButSuppressedRetainedCount: Int,
      recoveredWithoutSampleBridgeRetainedCount: Int,
      controlDriftCount: Int
  )
  object SameBatchAggregateCompare:
    given writes: OWrites[SameBatchAggregateCompare] = Json.writes[SameBatchAggregateCompare]

  private val TrancheName = "AccessNetwork candidate-bridge contract hardening"

  def main(args: Array[String]): Unit =
    parseArgs(args.toList) match
      case Left(err) =>
        System.err.println(s"[access-network-candidate-bridge-contract] $err")
        sys.exit(2)
      case Right(config) =>
        run(config)

  private def run(
      config: Config
  ): Unit =
    val rows = loadRows(config.inputPath, config.outputAuditPath)
    val diffs = rows.map(analyzeRow)

    val summary: BeforeAfterClassSummary =
      BeforeAfterClassSummary(
        tranche = TrancheName,
        inputPath = config.inputPath.toString,
        totalRows = diffs.size,
        beforeCounts = countBy(diffs.map(_.beforeClass)),
        afterCounts = countBy(diffs.map(_.afterClass)),
        promotedSampleIds = diffs.filter(_.promotedByThreeGate).map(_.sampleId),
        unexplainedAnchorNoCandidateSampleIds =
          diffs.filter(diff =>
            diff.beforeClass == "anchor_no_candidate" &&
              !diff.boundaryExplainedBefore
          ).map(_.sampleId)
      )

    val aggregate: SameBatchAggregateCompare =
      SameBatchAggregateCompare(
        tranche = TrancheName,
        totalRows = diffs.size,
        matchedAccessClaimCount = diffs.count(_.matchedAccessClaimId.nonEmpty),
        anchorNoCandidateCount = diffs.count(_.beforeClass == "anchor_no_candidate"),
        anchorNoCandidateBroadBoundaryCount =
          diffs.count(diff =>
            diff.beforeClass == "anchor_no_candidate" &&
              diff.boundaryExplainedBefore
          ),
        anchorNoCandidateThreeGateCount =
          diffs.count(diff =>
            diff.beforeClass == "anchor_no_candidate" &&
              diff.afterResidualClaimIds.nonEmpty
          ),
        anchorNoCandidatePromotedCount =
          diffs.count(diff =>
            diff.beforeClass == "anchor_no_candidate" &&
              diff.promotedByThreeGate
          ),
        anchorNoCandidateUnexplainedCount =
          diffs.count(diff =>
            diff.beforeClass == "anchor_no_candidate" &&
              !diff.boundaryExplainedBefore
          ),
        offTraceRetainedCount =
          diffs.count(diff =>
            diff.beforeClass == "off_trace" &&
              diff.afterClass == "off_trace"
          ),
        candidateNotSelectedRetainedCount =
          diffs.count(diff =>
            diff.beforeClass == "candidate_not_selected" &&
              diff.afterClass == "candidate_not_selected"
          ),
        candidateSelectedButSuppressedRetainedCount =
          diffs.count(diff =>
            diff.beforeClass == "candidate_selected_but_suppressed" &&
              diff.afterClass == "candidate_selected_but_suppressed"
          ),
        recoveredWithoutSampleBridgeRetainedCount =
          diffs.count(diff =>
            diff.beforeClass == "recovered_without_sample_bridge" &&
              diff.afterClass == "recovered_without_sample_bridge"
          ),
        controlDriftCount =
          diffs.count(diff =>
            diff.beforeClass != "anchor_no_candidate" &&
              diff.beforeClass != diff.afterClass
          )
      )

    val summaryPath =
      config.outputDir.resolve("access_network_candidate_bridge_contract.before_after_class_summary.json")
    val diffPath =
      config.outputDir.resolve("access_network_candidate_bridge_contract.row_by_row_diff.jsonl")
    val aggregatePath =
      config.outputDir.resolve("access_network_candidate_bridge_contract.same_batch_aggregate_compare.json")

    writeText(summaryPath, Json.prettyPrint(summon[Writes[BeforeAfterClassSummary]].writes(summary)))
    writeText(
      diffPath,
      diffs.map(diff => Json.stringify(summon[Writes[RowDiff]].writes(diff))).mkString("", "\n", "\n")
    )
    writeText(
      aggregatePath,
      Json.prettyPrint(summon[Writes[SameBatchAggregateCompare]].writes(aggregate))
    )

    println(s"[access-network-candidate-bridge-contract] wrote summary to $summaryPath")
    println(s"[access-network-candidate-bridge-contract] wrote row diff to $diffPath")
    println(s"[access-network-candidate-bridge-contract] wrote aggregate compare to $aggregatePath")

  private def analyzeRow(
      row: AuditRow
  ): RowDiff =
    val beforeClass = classifyRow(row)
    val claims = runtimeClaims(row.fen, row.playedUci)
    val accessClaim = matchedAccessClaim(row, claims)
    val residualAssessments =
      accessClaim.toList.flatMap(access =>
        claims
          .filter(other =>
            other.id != access.id &&
              other.plannerMetadata.residualSpecificityClass.nonEmpty &&
              other.deltaScope == access.deltaScope &&
              other.delta.map(_.owner) == access.delta.map(_.owner)
          )
          .sortBy(_.id)
          .map(residual =>
            val afterAssessment =
              AccessNetworkBridgeAdmissionBoundary.assess(access, residual)
            ResidualAssessmentRow(
              residualClaimId = residual.id,
              residualFamily = residual.delta.map(_.family.toString).getOrElse("unknown"),
              residualStatus = residual.status.toString,
              residualSpecificityClass = residual.plannerMetadata.residualSpecificityClass.map(_.toString),
              broadOverlapBefore = legacyBroadOverlap(access, residual),
              traceAdmittedSlice = afterAssessment.traceAdmittedSlice,
              routeWitnessRetained = afterAssessment.routeWitnessRetained,
              contestedTargetRetained = afterAssessment.contestedTargetRetained,
              admittedAfter = afterAssessment.admitted
            )
          )
      )

    val beforeResidualClaimIds =
      residualAssessments.filter(_.broadOverlapBefore).map(_.residualClaimId)
    val afterResidualClaimIds =
      residualAssessments.filter(_.admittedAfter).map(_.residualClaimId)

    val promotedByThreeGate =
      beforeClass == "anchor_no_candidate" &&
        beforeResidualClaimIds.nonEmpty &&
        afterResidualClaimIds.isEmpty

    val afterClass =
      if promotedByThreeGate then "promoted_to_candidate"
      else beforeClass

    val controlRetained =
      beforeClass != "anchor_no_candidate" &&
        beforeClass == afterClass

    val note =
      accessClaim match
        case None =>
          Some(s"runtime did not reproduce board-truth AccessNetwork objectId ${row.boardTruthEvidence.objectId}")
        case Some(_) if beforeClass == "anchor_no_candidate" && beforeResidualClaimIds.isEmpty =>
          Some("current planner-side broad overlap boundary does not explain this anchor_no_candidate row")
        case _ =>
          None

    RowDiff(
      sampleId = row.sampleId,
      beforeClass = beforeClass,
      afterClass = afterClass,
      matchedAccessClaimId = accessClaim.map(_.id),
      matchedAccessObjectId = accessClaim.map(_.objectId),
      boundaryExplainedBefore = beforeResidualClaimIds.nonEmpty,
      promotedByThreeGate = promotedByThreeGate,
      controlRetained = controlRetained,
      beforeResidualClaimIds = beforeResidualClaimIds,
      afterResidualClaimIds = afterResidualClaimIds,
      residualAssessments = residualAssessments,
      note = note
    )

  private def loadRows(
      universePath: Path,
      outputAuditPath: Path
  ): List[AuditRow] =
    val outputAuditBySampleId =
      Files
        .readAllLines(outputAuditPath, StandardCharsets.UTF_8)
        .asScala
        .toList
        .map(_.trim)
        .filter(_.nonEmpty)
        .map(parseOutputAuditRow)
        .map(row => row.sampleId -> row)
        .toMap

    Files
      .readAllLines(universePath, StandardCharsets.UTF_8)
      .asScala
      .toList
      .map(_.trim)
      .filter(_.nonEmpty)
      .map(parseUniverseRow)
      .map { row =>
        val outputAuditRow =
          outputAuditBySampleId.getOrElse(
            row.sampleId,
            throw new IllegalArgumentException(
              s"missing output audit row for sample ${row.sampleId} in $outputAuditPath"
            )
          )
        row.copy(
          boardTruthEvidence = outputAuditRow.boardTruthEvidence,
          surfacedOutcome = outputAuditRow.surfacedOutcome
        )
      }

  private def parseOutputAuditRow(
      line: String
  ): AuditRow =
    val js = Json.parse(line).as[JsObject]
    AuditRow(
      sampleId = requiredString(js, "sampleId"),
      fen = "",
      playedUci = "",
      boardTruthEvidence = parseBoardTruthEvidence((js \ "boardTruthEvidence").as[JsObject]),
      traceSelection =
        AuditTraceSelection(
          sampleIsAnchor = false,
          sampleIsCandidateBridge = false,
          sampleIsSelectedBridge = false,
          sampleIsVisibleMoment = false,
          sampleIsActiveNote = false
        ),
      surfacedOutcome = (js \ "surfacedSuppressedDistorted").asOpt[String]
    )

  private def parseUniverseRow(
      line: String
  ): AuditRow =
    val js = Json.parse(line).as[JsObject]
    AuditRow(
      sampleId = requiredString(js, "sampleId"),
      fen = requiredString(js, "fen"),
      playedUci = requiredString(js, "playedUci"),
      boardTruthEvidence =
        AuditBoardTruthEvidence(
          objectId = "",
          owner = "",
          rolesDetailed = Nil,
          contestedSquares = Nil,
          route = None
        ),
      traceSelection = parseTraceSelection((js \ "traceSelection").as[JsObject]),
      surfacedOutcome = None
    )

  private def parseBoardTruthEvidence(
      js: JsObject
  ): AuditBoardTruthEvidence =
    AuditBoardTruthEvidence(
      objectId = requiredString(js, "objectId"),
      owner = requiredString(js, "owner"),
      rolesDetailed = stringList(js, "rolesDetailed"),
      contestedSquares = stringList(js, "contestedSquares"),
      route =
        (js \ "route").asOpt[JsObject].map(routeJs =>
          AuditRouteEvidence(
            origin = requiredString(routeJs, "origin"),
            via = stringList(routeJs, "via"),
            target = requiredString(routeJs, "target"),
            allSquares = stringList(routeJs, "allSquares")
          )
        )
    )

  private def parseTraceSelection(
      js: JsObject
  ): AuditTraceSelection =
    AuditTraceSelection(
      sampleIsAnchor = requiredBoolean(js, "sampleIsAnchor"),
      sampleIsCandidateBridge = requiredBoolean(js, "sampleIsCandidateBridge"),
      sampleIsSelectedBridge = requiredBoolean(js, "sampleIsSelectedBridge"),
      sampleIsVisibleMoment = requiredBoolean(js, "sampleIsVisibleMoment"),
      sampleIsActiveNote = requiredBoolean(js, "sampleIsActiveNote")
    )

  private def classifyRow(
      row: AuditRow
  ): String =
    val trace = row.traceSelection
    if !trace.sampleIsAnchor &&
        !trace.sampleIsCandidateBridge &&
        !trace.sampleIsSelectedBridge &&
        !trace.sampleIsVisibleMoment &&
        !trace.sampleIsActiveNote
    then "off_trace"
    else if trace.sampleIsSelectedBridge && row.surfacedOutcome.contains("suppressed") then
      "candidate_selected_but_suppressed"
    else if trace.sampleIsCandidateBridge && !trace.sampleIsSelectedBridge then "candidate_not_selected"
    else if !trace.sampleIsCandidateBridge && row.surfacedOutcome.contains("surfaced") then
      "recovered_without_sample_bridge"
    else if trace.sampleIsAnchor && !trace.sampleIsCandidateBridge then "anchor_no_candidate"
    else "other"

  private def runtimeClaims(
      fen: String,
      playedUci: String
  ): List[CertifiedClaim] =
    val truth = PrimitiveExtractionTest.moveTransitionVisibleTruthFrameFor(playedUci)
    val contract = truth.toContract
    val objects = StrategicObjectSynthesizerTest.objectsForFen(fen, truth)
    val deltas = CanonicalStrategicObjectDeltaProjector.project(contract, truth, objects)
    CanonicalClaimCertification.certify(contract, objects, deltas)

  private def matchedAccessClaim(
      row: AuditRow,
      claims: List[CertifiedClaim]
  ): Option[CertifiedClaim] =
    val exactObjectIdMatch =
      claims.find(claim =>
        claim.objectId == row.boardTruthEvidence.objectId &&
          claim.deltaScope == StrategicDeltaScope.MoveLocal &&
          claim.delta.exists(_.family == StrategicObjectFamily.AccessNetwork)
      )

    exactObjectIdMatch.orElse {
      claims.find(claim =>
        claim.deltaScope == StrategicDeltaScope.MoveLocal &&
          claim.delta.exists(_.family == StrategicObjectFamily.AccessNetwork) &&
          accessClaimMatchesBoardTruth(claim, row.boardTruthEvidence)
      )
    }

  private def accessClaimMatchesBoardTruth(
      claim: CertifiedClaim,
      evidence: AuditBoardTruthEvidence
  ): Boolean =
    claim.delta.exists {
      case StrategicObjectDelta(
            _,
            StrategicObjectFamily.AccessNetwork,
            owner,
            StrategicDeltaScope.MoveLocal,
            StrategicObjectProfile.AccessNetwork(_, route, roles, contestedSquares),
            _,
            changedAnchors,
            _,
            _,
            evidenceRefs
          ) =>
        val runtimeRouteSquares =
          route
            .orElse(changedAnchors.flatMap(_.route).headOption)
            .toList
            .flatMap(_.allSquares.map(_.key))
            .distinct
            .sorted
        val runtimeContestedSquares =
          (contestedSquares ++ evidenceRefs.flatMap(_.contestedSquares)).map(_.key).distinct.sorted
        val runtimeRoles =
          roles.map {
            case chess.Bishop => "bishop"
            case chess.Knight => "knight"
            case chess.Rook   => "rook"
            case chess.Queen  => "queen"
            case chess.King   => "king"
            case chess.Pawn   => "pawn"
          }.toList.sorted

        colorName(owner) == evidence.owner &&
          runtimeContestedSquares == evidence.contestedSquares.sorted &&
          evidence.route.forall(routeEvidence => runtimeRouteSquares == routeEvidence.allSquares.sorted) &&
          (evidence.rolesDetailed.isEmpty || runtimeRoles == evidence.rolesDetailed.sorted)
      case _ =>
        false
    }

  private def legacyBroadOverlap(
      accessClaim: CertifiedClaim,
      other: CertifiedClaim
  ): Boolean =
    accessClaim.id != other.id &&
      other.plannerMetadata.residualSpecificityClass.nonEmpty &&
      accessClaim.delta.map(_.owner) == other.delta.map(_.owner) &&
      accessClaim.deltaScope == other.deltaScope &&
      (
        claimAnchorKeys(accessClaim).intersect(claimAnchorKeys(other)).nonEmpty ||
          claimEvidenceSquares(accessClaim).intersect(claimEvidenceSquares(other)).nonEmpty ||
          claimEvidenceFiles(accessClaim).intersect(claimEvidenceFiles(other)).nonEmpty ||
          claimSupportIds(accessClaim).contains(other.objectId) ||
          claimSupportIds(other).contains(accessClaim.objectId) ||
          claimSupportIds(accessClaim).intersect(claimSupportIds(other)).nonEmpty
      )

  private def claimAnchorKeys(
      claim: CertifiedClaim
  ): Set[String] =
    claim.delta.toSet.flatMap(_.changedAnchors.flatMap(_.squares.map(_.key)))

  private def claimEvidenceSquares(
      claim: CertifiedClaim
  ): Set[String] =
    claim.delta.toSet.flatMap(
      _.evidenceRefs.flatMap(ref => ref.anchorSquares.map(_.key) ++ ref.contestedSquares.map(_.key))
    )

  private def claimEvidenceFiles(
      claim: CertifiedClaim
  ): Set[String] =
    claim.delta.toSet.flatMap(_.evidenceRefs.flatMap(_.lane.map(_.char.toString)))

  private def claimSupportIds(
      claim: CertifiedClaim
  ): Set[String] =
    claim.supportingObjectIds.toSet ++ claim.delta.toSet.flatMap(_.supportingObjectIds)

  private def requiredString(
      js: JsObject,
      key: String
  ): String =
    (js \ key).asOpt[String].getOrElse(
      throw new IllegalArgumentException(s"missing string field: $key")
    )

  private def requiredBoolean(
      js: JsObject,
      key: String
  ): Boolean =
    (js \ key).asOpt[Boolean].getOrElse(
      throw new IllegalArgumentException(s"missing boolean field: $key")
    )

  private def stringList(
      js: JsObject,
      key: String
  ): List[String] =
    (js \ key).asOpt[List[String]].getOrElse(Nil)

  private def colorName(
      owner: chess.Color
  ): String =
    if owner.white then "White" else "Black"

  private def countBy(
      values: List[String]
  ): Map[String, Int] =
    values.groupBy(identity).view.mapValues(_.size).toMap.toList.sortBy(_._1).toMap

  private def parseArgs(
      args: List[String]
  ): Either[String, Config] =
    val defaultInput =
      Path
        .of(
          "C:\\Codes\\CondensedChess\\tmp\\strategic_object\\reports\\access_network_commentary_leverage_audit_20260416\\top20.audit-merged.jsonl"
        )
        .toAbsolutePath
        .normalize
    val defaultOutputAudit =
      defaultInput.getParent.resolve("access_network_output_audit.jsonl")
    val defaultOutputDir = defaultInput.getParent

    def loop(rest: List[String], current: Config): Either[String, Config] =
      rest match
        case Nil => Right(current)
        case head :: tail if head.startsWith("--input=") =>
          loop(tail, current.copy(inputPath = Path.of(head.stripPrefix("--input=")).toAbsolutePath.normalize))
        case "--input" :: value :: tail =>
          loop(tail, current.copy(inputPath = Path.of(value).toAbsolutePath.normalize))
        case head :: tail if head.startsWith("--output-dir=") =>
          loop(tail, current.copy(outputDir = Path.of(head.stripPrefix("--output-dir=")).toAbsolutePath.normalize))
        case "--output-dir" :: value :: tail =>
          loop(tail, current.copy(outputDir = Path.of(value).toAbsolutePath.normalize))
        case head :: tail if head.startsWith("--output-audit=") =>
          loop(
            tail,
            current.copy(outputAuditPath = Path.of(head.stripPrefix("--output-audit=")).toAbsolutePath.normalize)
          )
        case "--output-audit" :: value :: tail =>
          loop(
            tail,
            current.copy(outputAuditPath = Path.of(value).toAbsolutePath.normalize)
          )
        case unknown :: _ =>
          Left(s"unknown argument: $unknown")

    loop(
      args,
      Config(
        inputPath = defaultInput,
        outputAuditPath = defaultOutputAudit,
        outputDir = defaultOutputDir
      )
    )

  private def writeText(
      path: Path,
      value: String
  ): Unit =
    Option(path.getParent).foreach(parent => Files.createDirectories(parent))
    Files.writeString(path, value, StandardCharsets.UTF_8)
