package lila.llm.strategicobject

import chess.Square
import munit.FunSuite
import play.api.libs.json.*

import scala.io.Source

class CurrentPositionFixedTargetProbeTest extends FunSuite:

  import CurrentPositionFixedTargetProbeTest.*

  test("packet current-position fixed-target corpus covers exact negative and near-miss rows") {
    assertEquals(rows.map(_.caseType).toSet, Set("exact", "negative", "near_miss"))
  }

  rows.foreach { row =>
    test(s"current-position fixed-target expectation ${row.id}") {
      val truth = PrimitiveExtractionTest.neutralTruthFrame
      val contract = PrimitiveExtractionTest.neutralContract
      val objects = StrategicObjectSynthesizerTest.objectsForFen(row.fen, truth)
      val deltas = CanonicalStrategicObjectDeltaProjector.project(contract, truth, objects)
      val claims = CanonicalClaimCertification.certify(contract, objects, deltas)
      val planned = CanonicalQuestionPlanner.plan(contract, claims)

      val objectIds = targetObjectIds(row, objects)
      val positionClaims =
        claims.filter(claim =>
          objectIds.contains(claim.objectId) &&
            claim.deltaScope == StrategicDeltaScope.PositionLocal
        )
      val primaryClaims = positionClaims.filter(_.status == ClaimStatus.Certified)
      val plannerAdmission = admission(planned, positionClaims)
      val localizedStage = localization(planned, objectIds, deltas, positionClaims)
      val debugSummary =
        s"${row.id}: plannerAdmission=$plannerAdmission localization=$localizedStage planned=$planned objectIds=${objectIds.toList.sorted} positionClaims=${positionClaims.map(claim =>
            s"${claim.status}:${claim.id}:${claim.primaryTag}:support=${claim.supportingObjectIds.sorted.mkString("[", ",", "]")}:delta=${claim.delta
                .map(delta => s"${delta.family}/${delta.scope}/${delta.primaryTag}/${delta.changedAnchors.size}/${delta.evidenceRefs.size}")
                .getOrElse("none")}"
          )}"

      assert(objectIds.nonEmpty || row.anchor.isEmpty, clue(s"${row.id}: expected at least one target object or an intentionally broad near-miss slice"))
      assertEquals(plannerAdmission, row.plannerAdmission, clue(debugSummary))

      row.expectation match
        case "primary" =>
          val primaryClaim =
            primaryClaims.find(_.primaryTag.contains(StrategicDeltaTag.TargetFixed)).getOrElse(
              fail(s"${row.id}: expected certified fixed-target position-local claim")
            )

          assertEquals(planned.axis, QuestionAxis.WhatMattersHere, clue(debugSummary))
          assert(planned.claimIds.contains(primaryClaim.id), clue(s"${row.id}: expected planner primary admission"))
          assert(positionClaims.nonEmpty, clue(s"${row.id}: expected position-local fixed-target claim"))
          assert(primaryClaim.delta.exists(_.family == StrategicObjectFamily.FixedTargetComplex), clue(s"${row.id}: expected FixedTargetComplex claim"))
          assert(primaryClaim.delta.exists(_.scope == StrategicDeltaScope.PositionLocal), clue(s"${row.id}: expected position-local claim"))
        case "none" =>
          assert(positionClaims.forall(claim => !planned.claimIds.contains(claim.id)), clue(s"${row.id}: closed slice must not become primary"))
          assert(positionClaims.forall(claim => !planned.supportClaimIds.contains(claim.id)), clue(s"${row.id}: closed slice must not become support"))
          assert(positionClaims.forall(_.primaryTag != StrategicDeltaTag.TargetFixed), clue(s"${row.id}: closed slice must not certify exact fixation"))
        case other =>
          fail(s"${row.id}: unsupported expectation=$other")
    }
  }

object CurrentPositionFixedTargetProbeTest:

  final case class CurrentPositionFixedTargetRow(
      id: String,
      caseType: String,
      source: String,
      fen: String,
      family: String,
      owner: String,
      anchor: Option[String],
      expectation: String,
      plannerAdmission: String
  )

  private given Reads[CurrentPositionFixedTargetRow] = Json.reads[CurrentPositionFixedTargetRow]

  val rows: List[CurrentPositionFixedTargetRow] =
    Source
      .fromResource("strategic-object-corpus/current-position-fixed-target-expectations.jsonl")
      .getLines()
      .toList
      .map(_.trim)
      .filter(_.nonEmpty)
      .zipWithIndex
      .map { case (line, idx) =>
        Json.parse(line).validate[CurrentPositionFixedTargetRow].asEither match
          case Right(row) => row
          case Left(err)  => throw new IllegalArgumentException(s"invalid current-position fixed-target row ${idx + 1}: $err")
      }

  private def targetObjectIds(
      row: CurrentPositionFixedTargetRow,
      objects: List[StrategicObject]
  ): Set[String] =
    val family = StrategicObjectSynthesizerTest.parseFamily(row.family)
    val owner = StrategicObjectSynthesizerTest.parseColor(row.owner)
    val anchor = row.anchor.flatMap(StrategicObjectSynthesizerTest.parseSquare)

    objects
      .filter(obj =>
        obj.family == family &&
          obj.owner == owner &&
          anchor.forall(objectSquares(obj).contains)
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
    else if matchedClaims.exists(_.status == ClaimStatus.Certified) then "certification"
    else if matchedClaims.nonEmpty then "claim"
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
