package lila.llm.tools.strategicobject

import chess.{ File, Square }
import play.api.libs.json.*

import lila.llm.analysis.{ DecisiveTruthContract, MoveTruthFrame }
import lila.llm.strategicobject.*

object StrategicObjectExplanationTraceSupport:

  final case class ProjectionTrace(
      kind: Option[String],
      primaryTag: Option[String],
      standing: Option[String],
      focalAnchorCount: Option[Int],
      metricCount: Int
  )
  object ProjectionTrace:
    given Writes[ProjectionTrace] = Json.writes[ProjectionTrace]

  final case class WitnessTrace(
      kind: Option[String],
      transitionAware: Boolean,
      familyAware: Boolean,
      exactCounterpartWitness: Boolean,
      matchedSquares: List[String],
      matchedFiles: List[String],
      primitiveKinds: List[String],
      relationWitnesses: List[String],
      counterpartWitnessKinds: List[String],
      counterpartObjectIds: List[String]
  )
  object WitnessTrace:
    given Writes[WitnessTrace] = Json.writes[WitnessTrace]

  final case class CertificationTrace(
      status: Option[String],
      claimId: Option[String],
      supportingObjectIds: List[String]
  )
  object CertificationTrace:
    given Writes[CertificationTrace] = Json.writes[CertificationTrace]

  final case class PlannerTrace(
      axis: String,
      admission: String,
      primaryClaimIds: List[String],
      supportClaimIds: List[String]
  )
  object PlannerTrace:
    given Writes[PlannerTrace] = Json.writes[PlannerTrace]

  final case class EvidenceTrace(
      changedAnchorSquares: List[String],
      evidenceAnchorSquares: List[String],
      contestedSquares: List[String],
      lanes: List[String]
  )
  object EvidenceTrace:
    given Writes[EvidenceTrace] = Json.writes[EvidenceTrace]

  final case class LocalizationTrace(
      localizedStage: String,
      objectMatchCount: Int,
      deltaMatchCount: Int,
      claimMatchCount: Int
  )
  object LocalizationTrace:
    given Writes[LocalizationTrace] = Json.writes[LocalizationTrace]

  final case class ExplanationTraceRow(
      rowId: String,
      caseType: String,
      expectation: String,
      source: String,
      fen: String,
      playedMove: Option[String],
      truthCase: Option[String],
      family: String,
      owner: String,
      scope: String,
      anchor: Option[String],
      objectId: Option[String],
      readiness: Option[String],
      projection: ProjectionTrace,
      witness: WitnessTrace,
      certification: CertificationTrace,
      planner: PlannerTrace,
      evidence: EvidenceTrace,
      localization: LocalizationTrace
  )
  object ExplanationTraceRow:
    given Writes[ExplanationTraceRow] = Json.writes[ExplanationTraceRow]

  def traceRows: List[ExplanationTraceRow] =
    StrategicObjectDeltaProjectorTest.rows.map(traceRow)

  def renderJsonl(rows: List[ExplanationTraceRow]): String =
    rows.map(row => Json.stringify(Json.toJson(row))).mkString("", "\n", "\n")

  def traceRow(
      row: StrategicObjectDeltaProjectorTest.DeltaExpectationRow
  ): ExplanationTraceRow =
    val truth = truthFor(row)
    val contract = contractFor(row)
    val objects = StrategicObjectSynthesizerTest.objectsForFen(row.fen, truth)
    val deltas = CanonicalStrategicObjectDeltaProjector.project(contract, truth, objects)
    val claims = CanonicalClaimCertification.certify(contract, objects, deltas)
    val planned = CanonicalQuestionPlanner.plan(contract, claims)

    val matchedObjects = objectMatches(row, objects)
    val matchedDeltas = deltaMatches(row, deltas)
    val matchedClaims = claimMatches(row, matchedObjects, claims)

    val traceObject = matchedObjects.sortBy(_.id).headOption
    val traceDelta = matchedDeltas.sortBy(_.objectId).headOption
    val traceClaim = matchedClaims.sortBy(_.id).headOption

    ExplanationTraceRow(
      rowId = row.id,
      caseType = row.caseType,
      expectation = row.expectation,
      source = row.source,
      fen = row.fen,
      playedMove = row.playedMove,
      truthCase = row.truthCase,
      family = row.family,
      owner = row.owner,
      scope = row.scope,
      anchor = row.anchor,
      objectId = traceObject.map(_.id),
      readiness = traceObject.map(_.readiness.toString),
      projection = projectionTrace(traceDelta),
      witness = witnessTrace(traceDelta),
      certification = certificationTrace(traceClaim),
      planner = plannerTrace(planned, matchedClaims),
      evidence = evidenceTrace(traceDelta),
      localization = localizationTrace(planned, matchedObjects, matchedDeltas, matchedClaims)
    )

  private def truthFor(
      row: StrategicObjectDeltaProjectorTest.DeltaExpectationRow
  ): MoveTruthFrame =
    if isVisibleTruthCase(row.truthCase) then
      row.playedMove match
        case Some(playedMove) => PrimitiveExtractionTest.moveTransitionVisibleTruthFrameFor(playedMove)
        case None             => PrimitiveExtractionTest.moveTransitionVisibleTruthFrame
    else
      row.playedMove match
        case Some(playedMove) => PrimitiveExtractionTest.moveTransitionTruthFrameFor(playedMove)
        case None             => PrimitiveExtractionTest.moveTransitionTruthFrame

  private def contractFor(
      row: StrategicObjectDeltaProjectorTest.DeltaExpectationRow
  ): DecisiveTruthContract =
    if isVisibleTruthCase(row.truthCase) then
      row.playedMove match
        case Some(playedMove) => PrimitiveExtractionTest.moveTransitionVisibleContractFor(playedMove)
        case None             => PrimitiveExtractionTest.moveTransitionVisibleContract
    else
      row.playedMove match
        case Some(playedMove) => PrimitiveExtractionTest.moveTransitionContractFor(playedMove)
        case None             => PrimitiveExtractionTest.moveTransitionContract

  private def isVisibleTruthCase(
      truthCase: Option[String]
  ): Boolean =
    truthCase.contains("primary_visible") || truthCase.contains("move_transition_visible")

  private def objectMatches(
      row: StrategicObjectDeltaProjectorTest.DeltaExpectationRow,
      objects: List[StrategicObject]
  ): List[StrategicObject] =
    val family = StrategicObjectSynthesizerTest.parseFamily(row.family)
    val owner = StrategicObjectSynthesizerTest.parseColor(row.owner)
    val anchor = row.anchor.flatMap(StrategicObjectSynthesizerTest.parseSquare)

    objects.filter { obj =>
      obj.family == family &&
      obj.owner == owner &&
      anchor.forall(objectSquares(obj).contains)
    }

  private def deltaMatches(
      row: StrategicObjectDeltaProjectorTest.DeltaExpectationRow,
      deltas: List[StrategicObjectDelta]
  ): List[StrategicObjectDelta] =
    val family = StrategicObjectSynthesizerTest.parseFamily(row.family)
    val owner = StrategicObjectSynthesizerTest.parseColor(row.owner)
    val scope = StrategicObjectDeltaProjectorTest.parseScope(row.scope)
    val anchor = row.anchor.flatMap(StrategicObjectSynthesizerTest.parseSquare)

    deltas.filter { delta =>
      delta.family == family &&
      delta.owner == owner &&
      delta.scope == scope &&
      anchor.forall(deltaSquares(delta).contains)
    }

  private def claimMatches(
      row: StrategicObjectDeltaProjectorTest.DeltaExpectationRow,
      matchedObjects: List[StrategicObject],
      claims: List[CertifiedClaim]
  ): List[CertifiedClaim] =
    val scope = StrategicObjectDeltaProjectorTest.parseScope(row.scope)
    val matchedObjectIds = matchedObjects.map(_.id).toSet

    claims.filter(claim =>
      matchedObjectIds.contains(claim.objectId) &&
        claim.deltaScope == scope
    )

  private def projectionTrace(
      delta: Option[StrategicObjectDelta]
  ): ProjectionTrace =
    delta match
      case Some(found) =>
        found.projection match
          case StrategicDeltaProjection.MoveLocal(change, _) =>
            ProjectionTrace(
              kind = Some("MoveLocal"),
              primaryTag = Some(change.toString),
              standing = None,
              focalAnchorCount = None,
              metricCount = 0
            )
          case StrategicDeltaProjection.PositionLocal(state, focalAnchorCount) =>
            ProjectionTrace(
              kind = Some("PositionLocal"),
              primaryTag = Some(state.toString),
              standing = None,
              focalAnchorCount = Some(focalAnchorCount),
              metricCount = 0
            )
          case StrategicDeltaProjection.Comparative(contrast, balance, _, _, profile) =>
            ProjectionTrace(
              kind = Some("Comparative"),
              primaryTag = Some(contrast.toString),
              standing = Some(balance.standing.toString),
              focalAnchorCount = None,
              metricCount = profile.metrics.size
            )
      case None =>
        ProjectionTrace(None, None, None, None, 0)

  private def witnessTrace(
      delta: Option[StrategicObjectDelta]
  ): WitnessTrace =
    delta match
      case Some(found) =>
        found.projection match
          case StrategicDeltaProjection.MoveLocal(_, witness) =>
            WitnessTrace(
              kind = Some("MoveLocal"),
              transitionAware = witness.isTransitionAware,
              familyAware = false,
              exactCounterpartWitness = false,
              matchedSquares = showSquares(witness.matchedSquares),
              matchedFiles = showFiles(witness.matchedFiles),
              primitiveKinds = witness.primitiveKinds.toList.map(_.toString).sorted,
              relationWitnesses = witness.relationWitnesses.toList.map(_.toString).sorted,
              counterpartWitnessKinds = Nil,
              counterpartObjectIds = Nil
            )
          case StrategicDeltaProjection.PositionLocal(_, _) =>
            WitnessTrace(
              kind = Some("PositionLocal"),
              transitionAware = false,
              familyAware = false,
              exactCounterpartWitness = false,
              matchedSquares = Nil,
              matchedFiles = Nil,
              primitiveKinds = Nil,
              relationWitnesses = Nil,
              counterpartWitnessKinds = Nil,
              counterpartObjectIds = Nil
            )
          case StrategicDeltaProjection.Comparative(_, _, witness, counterpartObjectIds, _) =>
            WitnessTrace(
              kind = Some("Comparative"),
              transitionAware = false,
              familyAware = witness.isFamilyAware,
              exactCounterpartWitness = witness.hasExactCounterpartWitness,
              matchedSquares = showSquares(witness.matchedSquares),
              matchedFiles = showFiles(witness.matchedFiles),
              primitiveKinds = witness.rivalPrimitiveKinds.toList.map(_.toString).sorted,
              relationWitnesses = witness.relationWitnesses.toList.map(_.toString).sorted,
              counterpartWitnessKinds = witness.counterpartWitnessKinds.toList.map(_.toString).sorted,
              counterpartObjectIds = counterpartObjectIds.sorted
            )
      case None =>
        WitnessTrace(None, false, false, false, Nil, Nil, Nil, Nil, Nil, Nil)

  private def certificationTrace(
      claim: Option[CertifiedClaim]
  ): CertificationTrace =
    CertificationTrace(
      status = claim.map(_.status.toString),
      claimId = claim.map(_.id),
      supportingObjectIds = claim.toList.flatMap(_.supportingObjectIds).distinct.sorted
    )

  private def plannerTrace(
      planned: PlannedQuestion,
      matchedClaims: List[CertifiedClaim]
  ): PlannerTrace =
    val matchedClaimIds = matchedClaims.map(_.id).toSet
    val admission =
      if planned.claimIds.exists(matchedClaimIds.contains) then "primary"
      else if planned.supportClaimIds.exists(matchedClaimIds.contains) then "support"
      else "none"

    PlannerTrace(
      axis = planned.axis.toString,
      admission = admission,
      primaryClaimIds = planned.claimIds.sorted,
      supportClaimIds = planned.supportClaimIds.sorted
    )

  private def evidenceTrace(
      delta: Option[StrategicObjectDelta]
  ): EvidenceTrace =
    EvidenceTrace(
      changedAnchorSquares = delta.toList.flatMap(_.changedAnchors).flatMap(_.squares).distinct.sortBy(_.key).map(_.key),
      evidenceAnchorSquares = delta.toList.flatMap(_.evidenceRefs).flatMap(_.anchorSquares).distinct.sortBy(_.key).map(_.key),
      contestedSquares = delta.toList.flatMap(_.evidenceRefs).flatMap(_.contestedSquares).distinct.sortBy(_.key).map(_.key),
      lanes = delta.toList.flatMap(_.evidenceRefs).flatMap(_.lane).distinct.sortBy(_.char.toString).map(_.char.toString)
    )

  private def localizationTrace(
      planned: PlannedQuestion,
      matchedObjects: List[StrategicObject],
      matchedDeltas: List[StrategicObjectDelta],
      matchedClaims: List[CertifiedClaim]
  ): LocalizationTrace =
    val matchedClaimIds = matchedClaims.map(_.id).toSet
    val stage =
      if planned.claimIds.exists(matchedClaimIds.contains) then "planner_primary"
      else if planned.supportClaimIds.exists(matchedClaimIds.contains) then "planner_support"
      else if matchedClaims.exists(_.status == ClaimStatus.Certified) then "planner_none"
      else if matchedClaims.nonEmpty then "certification"
      else if matchedDeltas.nonEmpty then "delta"
      else if matchedObjects.nonEmpty then "object"
      else "absent"

    LocalizationTrace(
      localizedStage = stage,
      objectMatchCount = matchedObjects.size,
      deltaMatchCount = matchedDeltas.size,
      claimMatchCount = matchedClaims.size
    )

  private def objectSquares(
      obj: StrategicObject
  ): List[Square] =
    (
      obj.locus.allSquares ++
        obj.anchors.flatMap(_.squares) ++
        obj.anchors.flatMap(_.route.toList.flatMap(_.allSquares))
    ).distinct.sortBy(_.key)

  private def deltaSquares(
      delta: StrategicObjectDelta
  ): List[Square] =
    (
      delta.changedAnchors.flatMap(_.squares) ++
        delta.changedAnchors.flatMap(_.route.toList.flatMap(_.allSquares)) ++
        delta.evidenceRefs.flatMap(_.anchorSquares) ++
        delta.evidenceRefs.flatMap(_.contestedSquares)
    ).distinct.sortBy(_.key)

  private def showSquares(
      squares: List[Square]
  ): List[String] =
    squares.distinct.sortBy(_.key).map(_.key)

  private def showFiles(
      files: List[File]
  ): List[String] =
    files.distinct.sortBy(_.char.toString).map(_.char.toString)
