package lila.llm.strategicobject

import chess.Square
import munit.FunSuite
import play.api.libs.json.*

import scala.io.Source

class TargetFixationAdmissionTest extends FunSuite:

  import TargetFixationAdmissionTest.*

  test("packet target-fixation corpus covers exact negative contrastive and near-miss rows") {
    assertEquals(rows.map(_.caseType).toSet, Set("exact", "negative", "contrastive", "near_miss"))
  }

  rows.foreach { row =>
    test(s"target fixation expectation ${row.id}") {
      val truth = truthFor(row)
      val contract = contractFor(row)
      val objects = StrategicObjectSynthesizerTest.objectsForFen(row.fen, truth)
      val deltas = CanonicalStrategicObjectDeltaProjector.project(contract, truth, objects)
      val claims = CanonicalClaimCertification.certify(contract, objects, deltas)
      val planned = CanonicalQuestionPlanner.plan(contract, claims)

      val objectIds = targetObjectIds(row, objects)
      val moveLocalDeltas =
        deltas.filter(delta =>
          objectIds.contains(delta.objectId) &&
            delta.scope == StrategicDeltaScope.MoveLocal
        )
      val moveLocalClaims =
        claims.filter(claim =>
          objectIds.contains(claim.objectId) &&
            claim.deltaScope == StrategicDeltaScope.MoveLocal
        )
      val witnesses =
        moveLocalDeltas.flatMap {
          case StrategicObjectDelta(_, _, _, _, _, StrategicDeltaProjection.MoveLocal(_, witness), _, _, _, _) =>
            Some(witness)
          case _ =>
            None
        }
      val plannerAdmission = admission(planned, moveLocalClaims)
      val localizedStage = localization(planned, objectIds, moveLocalDeltas, moveLocalClaims)
      val deltaSummary = moveLocalDeltas.map(delta => s"${delta.primaryTag}:${delta.objectId}")
      val claimSummary = moveLocalClaims.map(claim => s"${claim.status}:${claim.id}")
      val witnessSummary = witnesses.map(_.matchedSquares.map(_.key))
      val debugSummary =
        s"${row.id}: plannerAdmission=$plannerAdmission localization=$localizedStage planned=$planned moveLocalDeltas=$deltaSummary moveLocalClaims=$claimSummary witnesses=$witnessSummary"

      assert(objectIds.nonEmpty, clue(s"${row.id}: expected fixed-target object at ${row.anchor}"))
      assert(
        plannerAdmission == row.plannerAdmission,
        clue(debugSummary)
      )
      assert(
        localizedStage == row.localization,
        clue(debugSummary)
      )

      row.expectation match
        case "primary" =>
          val primaryClaim =
            moveLocalClaims.find(_.status == ClaimStatus.Certified).getOrElse(
              fail(s"${row.id}: expected certified move-local fixed-target claim")
            )
          val fixationWitness =
            row.fixationWitness.flatMap(StrategicObjectSynthesizerTest.parseSquare).getOrElse(
              fail(s"${row.id}: expected fixation witness square")
            )

          assert(moveLocalDeltas.nonEmpty, clue(s"${row.id}: expected move-local fixed-target delta"))
          assert(moveLocalDeltas.forall(_.primaryTag == StrategicDeltaTag.TargetFixed), clue(s"${row.id}: expected exact fixation, not generic target pressure"))
          assert(primaryClaim.primaryTag.contains(StrategicDeltaTag.TargetFixed), clue(s"${row.id}: expected target-fixed certification"))
          assert(
            witnesses.exists(_.matchedSquares.contains(fixationWitness)),
            clue(s"${row.id}: expected move-local witness to include fixation square ${fixationWitness.key}, not only the target square")
          )
          assertEquals(planned.axis, QuestionAxis.WhyThis)
          assert(planned.claimIds.contains(primaryClaim.id), clue(s"${row.id}: expected planner primary admission"))
        case "none" =>
          assert(moveLocalDeltas.isEmpty, clue(s"${row.id}: pressure-only or non-fixation picture must not emit a move-local fixed-target delta"))
          assert(moveLocalClaims.isEmpty, clue(s"${row.id}: expected no move-local fixed-target claim"))
        case other =>
          fail(s"${row.id}: unsupported expectation=$other")
    }
  }

object TargetFixationAdmissionTest:

  final case class TargetFixationRow(
      id: String,
      caseType: String,
      source: String,
      fen: String,
      playedMove: String,
      truthCase: String,
      family: String,
      owner: String,
      anchor: String,
      fixationWitness: Option[String],
      expectation: String,
      plannerAdmission: String,
      localization: String,
      axis: Option[String]
  )

  private given Reads[TargetFixationRow] = Json.reads[TargetFixationRow]

  val rows: List[TargetFixationRow] =
    Source
      .fromResource("strategic-object-corpus/target-fixation-expectations.jsonl")
      .getLines()
      .toList
      .map(_.trim)
      .filter(_.nonEmpty)
      .zipWithIndex
      .map { case (line, idx) =>
        Json.parse(line).validate[TargetFixationRow].asEither match
          case Right(row) => row
          case Left(err)  => throw new IllegalArgumentException(s"invalid target fixation row ${idx + 1}: $err")
      }

  def truthFor(
      row: TargetFixationRow
  ): lila.llm.analysis.MoveTruthFrame =
    row.truthCase match
      case "move_transition" =>
        PrimitiveExtractionTest.moveTransitionTruthFrameFor(row.playedMove)
      case "move_transition_visible" =>
        PrimitiveExtractionTest.moveTransitionVisibleTruthFrameFor(row.playedMove)
      case other =>
        throw new IllegalArgumentException(s"${row.id}: unsupported truthCase=$other")

  def contractFor(
      row: TargetFixationRow
  ): lila.llm.analysis.DecisiveTruthContract =
    row.truthCase match
      case "move_transition" =>
        PrimitiveExtractionTest.moveTransitionContractFor(row.playedMove)
      case "move_transition_visible" =>
        PrimitiveExtractionTest.moveTransitionVisibleContractFor(row.playedMove)
      case other =>
        throw new IllegalArgumentException(s"${row.id}: unsupported truthCase=$other")

  private def targetObjectIds(
      row: TargetFixationRow,
      objects: List[StrategicObject]
  ): Set[String] =
    val family = StrategicObjectSynthesizerTest.parseFamily(row.family)
    val owner = StrategicObjectSynthesizerTest.parseColor(row.owner)
    val anchor =
      StrategicObjectSynthesizerTest.parseSquare(row.anchor).getOrElse(
        throw new IllegalArgumentException(s"${row.id}: invalid anchor ${row.anchor}")
      )

    objects
      .filter(obj =>
        obj.family == family &&
          obj.owner == owner &&
          objectSquares(obj).contains(anchor)
      )
      .map(_.id)
      .toSet

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

    if planned.claimIds.exists(matchedClaimIds.contains) then "planner_primary"
    else if planned.supportClaimIds.exists(matchedClaimIds.contains) then "planner_support"
    else if matchedClaims.exists(_.status == ClaimStatus.Certified) then "planner_none"
    else if matchedClaims.nonEmpty then "certification"
    else if matchedDeltas.nonEmpty then "delta"
    else if objectIds.nonEmpty then "object"
    else "absent"

  private def objectSquares(
      obj: StrategicObject
  ): List[Square] =
    (
      obj.locus.allSquares ++
        obj.anchors.flatMap(_.squares) ++
        obj.anchors.flatMap(_.route.toList.flatMap(_.allSquares))
    ).distinct.sortBy(_.key)
