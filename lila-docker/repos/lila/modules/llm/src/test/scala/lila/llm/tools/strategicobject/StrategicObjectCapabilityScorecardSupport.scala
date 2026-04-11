package lila.llm.tools.strategicobject

import chess.Square
import play.api.libs.json.*

import lila.llm.strategicobject.*

object StrategicObjectCapabilityScorecardSupport:

  final case class CapabilityEvidenceRow(
      rowId: String,
      evidenceSource: String,
      source: String,
      caseType: String,
      family: String,
      owner: String,
      scope: Option[String],
      axis: Option[String],
      expectedPresence: Boolean,
      expectedAdmission: Option[String],
      expectedStage: Option[String],
      observedAdmission: String,
      observedStage: String,
      certificationStatus: Option[String],
      objectCount: Int,
      deltaCount: Int,
      claimCount: Int,
      satisfied: Boolean
  )
  object CapabilityEvidenceRow:
    given Writes[CapabilityEvidenceRow] = Json.writes[CapabilityEvidenceRow]

  final case class Maturity(
      level: Int,
      key: String,
      description: String
  )
  object Maturity:
    given Writes[Maturity] = Json.writes[Maturity]

  final case class ScopeSummary(
      scope: String,
      totalRows: Int,
      positiveRows: Int,
      exactRows: Int,
      primaryRows: Int,
      supportRows: Int,
      negativeLeakRows: Int,
      highestStage: String
  )
  object ScopeSummary:
    given Writes[ScopeSummary] = Json.writes[ScopeSummary]

  final case class AxisSummary(
      axis: String,
      totalRows: Int,
      positiveRows: Int,
      primaryRows: Int,
      supportRows: Int,
      negativeLeakRows: Int,
      highestStage: String
  )
  object AxisSummary:
    given Writes[AxisSummary] = Json.writes[AxisSummary]

  final case class FamilySummary(
      family: String,
      maturity: Maturity,
      objectStage: String,
      deliveryStage: String,
      totalRows: Int,
      positiveRows: Int,
      exactPositiveRows: Int,
      satisfiedRows: Int,
      negativeLeakRows: Int,
      primaryRows: Int,
      supportRows: Int,
      distinctPrimarySources: Int,
      evidenceSources: List[String],
      stageCounts: Map[String, Int],
      certificationCounts: Map[String, Int],
      byScope: List[ScopeSummary],
      byAxis: List[AxisSummary]
  )
  object FamilySummary:
    given Writes[FamilySummary] = Json.writes[FamilySummary]

  final case class MaturityBucket(
      level: Int,
      key: String,
      familyCount: Int,
      families: List[String]
  )
  object MaturityBucket:
    given Writes[MaturityBucket] = Json.writes[MaturityBucket]

  final case class SystemSummary(
      totalRows: Int,
      familyCount: Int,
      positiveRows: Int,
      negativeLeakRows: Int,
      maturityBuckets: List[MaturityBucket]
  )
  object SystemSummary:
    given Writes[SystemSummary] = Json.writes[SystemSummary]

  final case class CapabilityScorecard(
      schema: String,
      system: SystemSummary,
      families: List[FamilySummary]
  )
  object CapabilityScorecard:
    given Writes[CapabilityScorecard] = Json.writes[CapabilityScorecard]

  val schema: String = "chesstory.strategicObject.capabilityScorecard.v1"

  def scorecard(
      rows: List[CapabilityEvidenceRow] = evidenceRows,
      families: Set[String] = Set.empty
  ): CapabilityScorecard =
    val filtered =
      if families.isEmpty then rows
      else rows.filter(row => families.contains(row.family))
    val familySummaries =
      filtered
        .groupBy(_.family)
        .toList
        .sortBy(_._1)
        .map { case (family, familyRows) =>
          summarizeFamily(family, familyRows)
        }
    CapabilityScorecard(
      schema = schema,
      system = summarizeSystem(filtered, familySummaries),
      families = familySummaries
    )

  def renderJsonl(
      rows: List[CapabilityEvidenceRow]
  ): String =
    rows.map(row => Json.stringify(Json.toJson(row))).mkString("", "\n", "\n")

  private val absentStage = "absent"
  private val objectStage = "object"
  private val deltaStage = "delta"
  private val claimStage = "claim"
  private val certificationStage = "certification"
  private val plannerNoneStage = "planner_none"
  private val plannerSupportStage = "planner_support"
  private val plannerPrimaryStage = "planner_primary"

  private val stageRank: Map[String, Int] =
    Map(
      absentStage -> 0,
      objectStage -> 1,
      deltaStage -> 2,
      claimStage -> 3,
      certificationStage -> 4,
      plannerNoneStage -> 5,
      plannerSupportStage -> 6,
      plannerPrimaryStage -> 7
    )

  private val supportStages: Set[String] =
    Set(certificationStage, plannerNoneStage, plannerSupportStage)

  private val repeatablePrimaryThreshold: Int = 3
  private val repeatablePrimarySourcesThreshold: Int = 2
  private val releasePrimaryThreshold: Int = 5

  val evidenceRows: List[CapabilityEvidenceRow] =
    objectEvidenceRows ++
      traceEvidenceRows ++
      targetFixationEvidenceRows ++
      favorableSimplificationEvidenceRows ++
      currentPositionFixedTargetEvidenceRows ++
      currentPositionCoordinationEvidenceRows

  private def summarizeFamily(
      family: String,
      rows: List[CapabilityEvidenceRow]
  ): FamilySummary =
    val positiveRows = rows.filter(_.expectedPresence)
    val exactPositiveRows = positiveRows.count(_.caseType == "exact")
    val negativeLeakRows = rows.count(isNegativeLeak)
    val primaryRows = positiveRows.count(_.observedAdmission == "primary")
    val supportRows = positiveRows.count(isSupportRow)
    val distinctPrimarySources =
      positiveRows
        .filter(_.observedAdmission == "primary")
        .map(_.source)
        .distinct
        .size
    val objectStageValue =
      highestStage(
        rows.filter(_.evidenceSource == "object").filter(_.expectedPresence).map(_.observedStage)
      )
    val deliveryStageValue =
      highestStage(
        rows.filter(_.evidenceSource != "object").filter(_.expectedPresence).map(_.observedStage)
      )
    val maturity =
      classifyMaturity(
        objectStageObserved = objectStageValue,
        deliveryStageObserved = deliveryStageValue,
        primaryRows = primaryRows,
        distinctPrimarySources = distinctPrimarySources,
        negativeLeakRows = negativeLeakRows
      )

    FamilySummary(
      family = family,
      maturity = maturity,
      objectStage = objectStageValue,
      deliveryStage = deliveryStageValue,
      totalRows = rows.size,
      positiveRows = positiveRows.size,
      exactPositiveRows = exactPositiveRows,
      satisfiedRows = rows.count(_.satisfied),
      negativeLeakRows = negativeLeakRows,
      primaryRows = primaryRows,
      supportRows = supportRows,
      distinctPrimarySources = distinctPrimarySources,
      evidenceSources = rows.map(_.evidenceSource).distinct.sorted,
      stageCounts = countBy(rows.map(_.observedStage)),
      certificationCounts = countBy(rows.flatMap(_.certificationStatus)),
      byScope =
        rows
          .flatMap(_.scope)
          .distinct
          .sorted
          .map(scope => summarizeScope(scope, rows.filter(_.scope.contains(scope)))),
      byAxis =
        rows
          .flatMap(_.axis)
          .distinct
          .sorted
          .map(axis => summarizeAxis(axis, rows.filter(_.axis.contains(axis))))
    )

  private def summarizeScope(
      scope: String,
      rows: List[CapabilityEvidenceRow]
  ): ScopeSummary =
    val positiveRows = rows.filter(_.expectedPresence)
    ScopeSummary(
      scope = scope,
      totalRows = rows.size,
      positiveRows = positiveRows.size,
      exactRows = positiveRows.count(_.caseType == "exact"),
      primaryRows = positiveRows.count(_.observedAdmission == "primary"),
      supportRows = positiveRows.count(isSupportRow),
      negativeLeakRows = rows.count(isNegativeLeak),
      highestStage = highestStage(positiveRows.map(_.observedStage))
    )

  private def summarizeAxis(
      axis: String,
      rows: List[CapabilityEvidenceRow]
  ): AxisSummary =
    val positiveRows = rows.filter(_.expectedPresence)
    AxisSummary(
      axis = axis,
      totalRows = rows.size,
      positiveRows = positiveRows.size,
      primaryRows = positiveRows.count(_.observedAdmission == "primary"),
      supportRows = positiveRows.count(isSupportRow),
      negativeLeakRows = rows.count(isNegativeLeak),
      highestStage = highestStage(positiveRows.map(_.observedStage))
    )

  private def summarizeSystem(
      rows: List[CapabilityEvidenceRow],
      familySummaries: List[FamilySummary]
  ): SystemSummary =
    val maturityBuckets =
      familySummaries
        .groupBy(summary => (summary.maturity.level, summary.maturity.key))
        .toList
        .sortBy(_._1._1)
        .map { case ((level, key), familiesAtLevel) =>
          MaturityBucket(
            level = level,
            key = key,
            familyCount = familiesAtLevel.size,
            families = familiesAtLevel.map(_.family).sorted
          )
        }

    SystemSummary(
      totalRows = rows.size,
      familyCount = familySummaries.size,
      positiveRows = rows.count(_.expectedPresence),
      negativeLeakRows = rows.count(isNegativeLeak),
      maturityBuckets = maturityBuckets
    )

  private def classifyMaturity(
      objectStageObserved: String,
      deliveryStageObserved: String,
      primaryRows: Int,
      distinctPrimarySources: Int,
      negativeLeakRows: Int
  ): Maturity =
    if primaryRows >= releasePrimaryThreshold &&
      distinctPrimarySources >= repeatablePrimarySourcesThreshold &&
      negativeLeakRows == 0
    then
      Maturity(
        level = 5,
        key = "release_candidate",
        description = "multiple primary slices survive with clean negative control"
      )
    else if primaryRows >= repeatablePrimaryThreshold &&
      distinctPrimarySources >= repeatablePrimarySourcesThreshold &&
      negativeLeakRows == 0
    then
      Maturity(
        level = 4,
        key = "repeatable_primary",
        description = "multiple primary slices survive repeatedly on the current corpus"
      )
    else if primaryRows > 0 then
      Maturity(
        level = 3,
        key = "bounded_primary",
        description = "at least one bounded slice reaches planner primary"
      )
    else if stageRank.getOrElse(deliveryStageObserved, 0) >= stageRank(certificationStage) then
      Maturity(
        level = 2,
        key = "support_only",
        description = "delivery evidence reaches claim/certification but not planner primary"
      )
    else if stageRank.getOrElse(objectStageObserved, 0) >= stageRank(objectStage) then
      Maturity(
        level = 1,
        key = "object_only",
        description = "object evidence exists but delivery remains closed"
      )
    else
      Maturity(
        level = 0,
        key = "absent_or_deferred",
        description = "no positive evidence survives object or delivery stages"
      )

  private def highestStage(
      stages: Iterable[String]
  ): String =
    stages.toList match
      case Nil => absentStage
      case values =>
        values.maxBy(stage => stageRank.getOrElse(stage, 0))

  private def isSupportRow(
      row: CapabilityEvidenceRow
  ): Boolean =
    supportStages.contains(row.observedStage) ||
      row.certificationStatus.contains(ClaimStatus.SupportOnly.toString) ||
      row.observedAdmission == "support"

  private def isNegativeLeak(
      row: CapabilityEvidenceRow
  ): Boolean =
    !row.expectedPresence && row.observedStage != absentStage

  private def countBy(
      values: Iterable[String]
  ): Map[String, Int] =
    values
      .filter(value => value != null)
      .groupBy(identity)
      .view
      .mapValues(_.size)
      .toMap
      .toList
      .sortBy(_._1)
      .toMap

  private def objectEvidenceRows: List[CapabilityEvidenceRow] =
    StrategicObjectSynthesizerTest.rows.map { row =>
      val objects = StrategicObjectSynthesizerTest.objectsForRow(row)
      val matched = StrategicObjectSynthesizerTest.findMatches(row, objects)
      val observedStage = if matched.nonEmpty then objectStage else absentStage
      val expectedStage = if row.expectation == "present" then Some(objectStage) else Some(absentStage)

      CapabilityEvidenceRow(
        rowId = row.id,
        evidenceSource = "object",
        source = row.source,
        caseType = row.caseType,
        family = row.family,
        owner = row.owner,
        scope = None,
        axis = None,
        expectedPresence = row.expectation == "present",
        expectedAdmission = None,
        expectedStage = expectedStage,
        observedAdmission = "none",
        observedStage = observedStage,
        certificationStatus = None,
        objectCount = matched.size,
        deltaCount = 0,
        claimCount = 0,
        satisfied = expectedStage.contains(observedStage)
      )
    }

  private def traceEvidenceRows: List[CapabilityEvidenceRow] =
    StrategicObjectExplanationTraceSupport.traceRows.map { row =>
      CapabilityEvidenceRow(
        rowId = row.rowId,
        evidenceSource = "trace",
        source = row.source,
        caseType = row.caseType,
        family = row.family,
        owner = row.owner,
        scope = Some(row.scope),
        axis = Some(row.planner.axis),
        expectedPresence = row.expectation == "present",
        expectedAdmission = row.traceExpectation.plannerAdmission,
        expectedStage = row.traceExpectation.localizationStage,
        observedAdmission = row.planner.admission,
        observedStage = row.localization.localizedStage,
        certificationStatus = row.certification.status,
        objectCount = row.localization.objectMatchCount,
        deltaCount = row.localization.deltaMatchCount,
        claimCount = row.localization.claimMatchCount,
        satisfied = row.traceExpectationMatch.satisfied
      )
    }

  private def targetFixationEvidenceRows: List[CapabilityEvidenceRow] =
    TargetFixationAdmissionTest.rows.map { row =>
      val truth = TargetFixationAdmissionTest.truthFor(row)
      val contract = TargetFixationAdmissionTest.contractFor(row)
      val objects = StrategicObjectSynthesizerTest.objectsForFen(row.fen, truth)
      val deltas = CanonicalStrategicObjectDeltaProjector.project(contract, truth, objects)
      val claims = CanonicalClaimCertification.certify(contract, objects, deltas)
      val planned = CanonicalQuestionPlanner.plan(contract, claims)

      val objectIds = matchingObjectIds(row.family, row.owner, Some(row.anchor), objects)
      val moveLocalDeltas = deltasFor(objectIds, StrategicDeltaScope.MoveLocal, deltas)
      val moveLocalClaims = claimsFor(objectIds, StrategicDeltaScope.MoveLocal, claims)
      val observedAdmission = admission(planned, moveLocalClaims)
      val observedStage = localization(planned, objectIds, moveLocalDeltas, moveLocalClaims)

      CapabilityEvidenceRow(
        rowId = row.id,
        evidenceSource = "packet_target_fixation",
        source = row.source,
        caseType = row.caseType,
        family = row.family,
        owner = row.owner,
        scope = Some("move_local"),
        axis = Some(planned.axis.toString),
        expectedPresence = row.expectation == "primary",
        expectedAdmission = Some(row.plannerAdmission),
        expectedStage = Some(row.localization),
        observedAdmission = observedAdmission,
        observedStage = observedStage,
        certificationStatus = preferredClaimStatus(moveLocalClaims),
        objectCount = objectIds.size,
        deltaCount = moveLocalDeltas.size,
        claimCount = moveLocalClaims.size,
        satisfied = observedAdmission == row.plannerAdmission && observedStage == row.localization
      )
    }

  private def favorableSimplificationEvidenceRows: List[CapabilityEvidenceRow] =
    FavorableSimplificationAdmissionTest.rows.map { row =>
      val truth = FavorableSimplificationAdmissionTest.truthFor(row)
      val contract = FavorableSimplificationAdmissionTest.contractFor(row)
      val objects = StrategicObjectSynthesizerTest.objectsForFen(row.fen, truth)
      val deltas = CanonicalStrategicObjectDeltaProjector.project(contract, truth, objects)
      val claims = CanonicalClaimCertification.certify(contract, objects, deltas)
      val planned = CanonicalQuestionPlanner.plan(contract, claims)

      val objectIds = matchingObjectIds(row.family, row.owner, row.anchor, objects)
      val moveLocalDeltas = deltasFor(objectIds, StrategicDeltaScope.MoveLocal, deltas)
      val moveLocalClaims = claimsFor(objectIds, StrategicDeltaScope.MoveLocal, claims)
      val observedAdmission = admission(planned, moveLocalClaims)
      val observedStage = localization(planned, objectIds, moveLocalDeltas, moveLocalClaims)

      CapabilityEvidenceRow(
        rowId = row.id,
        evidenceSource = "packet_favorable_simplification",
        source = row.source,
        caseType = row.caseType,
        family = row.family,
        owner = row.owner,
        scope = Some("move_local"),
        axis = Some(planned.axis.toString),
        expectedPresence = row.expectation == "primary",
        expectedAdmission = Some(row.plannerAdmission),
        expectedStage = Some(row.localization),
        observedAdmission = observedAdmission,
        observedStage = observedStage,
        certificationStatus = preferredClaimStatus(moveLocalClaims),
        objectCount = objectIds.size,
        deltaCount = moveLocalDeltas.size,
        claimCount = moveLocalClaims.size,
        satisfied = observedAdmission == row.plannerAdmission && observedStage == row.localization
      )
    }

  private def currentPositionFixedTargetEvidenceRows: List[CapabilityEvidenceRow] =
    CurrentPositionFixedTargetProbeTest.rows.map { row =>
      val truth = PrimitiveExtractionTest.neutralTruthFrame
      val contract = PrimitiveExtractionTest.neutralContract
      val objects = StrategicObjectSynthesizerTest.objectsForFen(row.fen, truth)
      val deltas = CanonicalStrategicObjectDeltaProjector.project(contract, truth, objects)
      val claims = CanonicalClaimCertification.certify(contract, objects, deltas)
      val planned = CanonicalQuestionPlanner.plan(contract, claims)

      val objectIds = matchingObjectIds(row.family, row.owner, row.anchor, objects)
      val positionDeltas = deltasFor(objectIds, StrategicDeltaScope.PositionLocal, deltas)
      val positionClaims = claimsFor(objectIds, StrategicDeltaScope.PositionLocal, claims)
      val observedAdmission = admission(planned, positionClaims)
      val observedStage = localization(planned, objectIds, positionDeltas, positionClaims)

      CapabilityEvidenceRow(
        rowId = row.id,
        evidenceSource = "packet_current_position_fixed_target",
        source = row.source,
        caseType = row.caseType,
        family = row.family,
        owner = row.owner,
        scope = Some("position_local"),
        axis = Some(planned.axis.toString),
        expectedPresence = row.expectation == "primary",
        expectedAdmission = Some(row.plannerAdmission),
        expectedStage = None,
        observedAdmission = observedAdmission,
        observedStage = observedStage,
        certificationStatus = preferredClaimStatus(positionClaims),
        objectCount = objectIds.size,
        deltaCount = positionDeltas.size,
        claimCount = positionClaims.size,
        satisfied = observedAdmission == row.plannerAdmission
      )
    }

  private def currentPositionCoordinationEvidenceRows: List[CapabilityEvidenceRow] =
    CurrentPositionCoordinationProbeTest.rows.map { row =>
      val truth = PrimitiveExtractionTest.neutralTruthFrame
      val contract = PrimitiveExtractionTest.neutralContract
      val objects = StrategicObjectSynthesizerTest.objectsForFen(row.fen, truth)
      val deltas = CanonicalStrategicObjectDeltaProjector.project(contract, truth, objects)
      val claims = CanonicalClaimCertification.certify(contract, objects, deltas)
      val planned = CanonicalQuestionPlanner.plan(contract, claims)

      val objectIds = matchingObjectIds(row.family, row.owner, row.anchor, objects)
      val positionDeltas = deltasFor(objectIds, StrategicDeltaScope.PositionLocal, deltas)
      val positionClaims = claimsFor(objectIds, StrategicDeltaScope.PositionLocal, claims)
      val observedAdmission = admission(planned, positionClaims)
      val observedStage = localization(planned, objectIds, positionDeltas, positionClaims)

      CapabilityEvidenceRow(
        rowId = row.id,
        evidenceSource = "packet_current_position_coordination",
        source = row.source,
        caseType = row.caseType,
        family = row.family,
        owner = row.owner,
        scope = Some("position_local"),
        axis = Some(planned.axis.toString),
        expectedPresence = row.expectation == "primary",
        expectedAdmission = Some(row.plannerAdmission),
        expectedStage = None,
        observedAdmission = observedAdmission,
        observedStage = observedStage,
        certificationStatus = preferredClaimStatus(positionClaims),
        objectCount = objectIds.size,
        deltaCount = positionDeltas.size,
        claimCount = positionClaims.size,
        satisfied = observedAdmission == row.plannerAdmission
      )
    }

  private def matchingObjectIds(
      family: String,
      owner: String,
      anchor: Option[String],
      objects: List[StrategicObject]
  ): Set[String] =
    val parsedFamily = StrategicObjectSynthesizerTest.parseFamily(family)
    val parsedOwner = StrategicObjectSynthesizerTest.parseColor(owner)
    val parsedAnchor = anchor.flatMap(StrategicObjectSynthesizerTest.parseSquare)

    objects
      .filter(obj =>
        obj.family == parsedFamily &&
          obj.owner == parsedOwner &&
          parsedAnchor.forall(objectSquares(obj).contains)
      )
      .map(_.id)
      .toSet

  private def objectSquares(
      obj: StrategicObject
  ): List[Square] =
    (
      obj.locus.allSquares ++
        obj.anchors.flatMap(_.squares) ++
        obj.anchors.flatMap(_.route.toList.flatMap(_.allSquares))
    ).distinct.sortBy(_.key)

  private def deltasFor(
      objectIds: Set[String],
      scope: StrategicDeltaScope,
      deltas: List[StrategicObjectDelta]
  ): List[StrategicObjectDelta] =
    deltas.filter(delta =>
      objectIds.contains(delta.objectId) &&
        delta.scope == scope
    )

  private def claimsFor(
      objectIds: Set[String],
      scope: StrategicDeltaScope,
      claims: List[CertifiedClaim]
  ): List[CertifiedClaim] =
    claims.filter(claim =>
      objectIds.contains(claim.objectId) &&
        claim.deltaScope == scope
    )

  private def admission(
      planned: PlannedQuestion,
      matchedClaims: List[CertifiedClaim]
  ): String =
    val matchedClaimIds = matchedClaims.map(_.id).toSet

    if planned.claimIds.exists(matchedClaimIds.contains) then "primary"
    else if planned.supportClaimIds.exists(matchedClaimIds.contains) then "support"
    else "none"

  private def localization(
      planned: PlannedQuestion,
      objectIds: Set[String],
      matchedDeltas: List[StrategicObjectDelta],
      matchedClaims: List[CertifiedClaim]
  ): String =
    val matchedClaimIds = matchedClaims.map(_.id).toSet

    if planned.claimIds.exists(matchedClaimIds.contains) then plannerPrimaryStage
    else if planned.supportClaimIds.exists(matchedClaimIds.contains) then plannerSupportStage
    else if matchedClaims.exists(_.status == ClaimStatus.Certified) then plannerNoneStage
    else if matchedClaims.nonEmpty then certificationStage
    else if matchedDeltas.nonEmpty then deltaStage
    else if objectIds.nonEmpty then objectStage
    else absentStage

  private def preferredClaimStatus(
      claims: List[CertifiedClaim]
  ): Option[String] =
    claims.sortBy(claim => claimStatusRank(claim.status)).lastOption.map(_.status.toString)

  private def claimStatusRank(
      status: ClaimStatus
  ): Int =
    status match
      case ClaimStatus.Deferred    => 0
      case ClaimStatus.Rejected    => 0
      case ClaimStatus.SupportOnly => 1
      case ClaimStatus.Certified   => 2
