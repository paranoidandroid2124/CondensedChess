package lila.llm.strategicobject

import chess.Square
import munit.FunSuite
import play.api.libs.json.*

import lila.llm.analysis.{ DecisiveTruthContract, MoveTruthFrame, TruthVisibilityRole }

import scala.io.Source

class ComparativeSupportAdmissionTest extends FunSuite:

  import ComparativeSupportAdmissionTest.*

  test("packet comparative-support corpus covers exact negative contrastive and near-miss rows") {
    assertEquals(rows.map(_.caseType).toSet, Set("exact", "negative", "contrastive", "near_miss"))
  }

  rows.foreach { row =>
    test(s"comparative support expectation ${row.id}") {
      val truth = truthFor(row)
      val contract = contractFor(row)
      val objects = StrategicObjectSynthesizerTest.objectsForFen(row.fen, truth)
      val deltas = CanonicalStrategicObjectDeltaProjector.project(contract, truth, objects)
      val claims = CanonicalClaimCertification.certify(contract, objects, deltas)
      val planned = CanonicalQuestionPlanner.plan(contract, claims)

      val primary = primaryClaim(row, objects, claims).getOrElse(
        fail(s"${row.id}: expected fixed-target comparative primary")
      )
      val support = supportClaim(row, objects, claims)

      assertEquals(planned.axis, QuestionAxis.WhatChanged)
      assert(planned.claimIds.contains(primary.id), clue(s"${row.id}: primary claim lost"))

      row.expectation match
        case "support" =>
          val supportClaimValue = support.getOrElse(fail(s"${row.id}: expected support claim"))
          assertEquals(supportClaimValue.status, ClaimStatus.SupportOnly)
          assertEquals(
            planned.supportClaimIds,
            List(supportClaimValue.id),
            clue(
              s"${row.id}: support=${supportClaimValue.id} supportObjects=${supportClaimValue.supportingObjectIds} primary=${primary.id} planned=$planned"
            )
          )
        case "none" =>
          assert(support.forall(claim => !planned.supportClaimIds.contains(claim.id)), clue(s"${row.id}: unexpected support admission"))
        case other =>
          fail(s"${row.id}: unsupported expectation=$other")
    }
  }

object ComparativeSupportAdmissionTest:

  final case class ComparativeSupportRow(
      id: String,
      caseType: String,
      source: String,
      fen: String,
      contractMode: String,
      primaryFamily: String,
      primaryOwner: String,
      primaryAnchor: String,
      supportFamily: String,
      supportOwner: String,
      supportAnchor: String,
      expectation: String
  )

  private given Reads[ComparativeSupportRow] = Json.reads[ComparativeSupportRow]

  val rows: List[ComparativeSupportRow] =
    Source
      .fromResource("strategic-object-corpus/comparative-support-expectations.jsonl")
      .getLines()
      .toList
      .map(_.trim)
      .filter(_.nonEmpty)
      .zipWithIndex
      .map { case (line, idx) =>
        Json.parse(line).validate[ComparativeSupportRow].asEither match
          case Right(row) => row
          case Left(err)  => throw new IllegalArgumentException(s"invalid comparative support row ${idx + 1}: $err")
      }

  private val visibleComparativeTruth: MoveTruthFrame =
    PrimitiveExtractionTest.neutralTruthFrame.copy(visibilityRole = TruthVisibilityRole.PrimaryVisible)

  private val visibleComparativeContract: DecisiveTruthContract =
    PrimitiveExtractionTest.neutralContract.copy(visibilityRole = TruthVisibilityRole.PrimaryVisible)

  def truthFor(
      row: ComparativeSupportRow
  ): MoveTruthFrame =
    row.contractMode match
      case "visible_no_transition" => visibleComparativeTruth
      case other                    => throw new IllegalArgumentException(s"${row.id}: unsupported contractMode=$other")

  def contractFor(
      row: ComparativeSupportRow
  ): DecisiveTruthContract =
    row.contractMode match
      case "visible_no_transition" => visibleComparativeContract
      case other                    => throw new IllegalArgumentException(s"${row.id}: unsupported contractMode=$other")

  def primaryClaim(
      row: ComparativeSupportRow,
      objects: List[StrategicObject],
      claims: List[CertifiedClaim]
  ): Option[CertifiedClaim] =
    claimFor(
      family = StrategicObjectSynthesizerTest.parseFamily(row.primaryFamily),
      owner = StrategicObjectSynthesizerTest.parseColor(row.primaryOwner),
      anchor = StrategicObjectSynthesizerTest.parseSquare(row.primaryAnchor).getOrElse(
        throw new IllegalArgumentException(s"${row.id}: invalid primary anchor ${row.primaryAnchor}")
      ),
      objects = objects,
      claims = claims
    ).find(_.status == ClaimStatus.Certified)

  def supportClaim(
      row: ComparativeSupportRow,
      objects: List[StrategicObject],
      claims: List[CertifiedClaim]
  ): Option[CertifiedClaim] =
    claimFor(
      family = StrategicObjectSynthesizerTest.parseFamily(row.supportFamily),
      owner = StrategicObjectSynthesizerTest.parseColor(row.supportOwner),
      anchor = StrategicObjectSynthesizerTest.parseSquare(row.supportAnchor).getOrElse(
        throw new IllegalArgumentException(s"${row.id}: invalid support anchor ${row.supportAnchor}")
      ),
      objects = objects,
      claims = claims
    ).headOption

  private def claimFor(
      family: StrategicObjectFamily,
      owner: chess.Color,
      anchor: Square,
      objects: List[StrategicObject],
      claims: List[CertifiedClaim]
  ): List[CertifiedClaim] =
    val matchingObjectIds =
      objects
        .filter(obj =>
          obj.family == family &&
            obj.owner == owner &&
            objectSquares(obj).contains(anchor)
        )
        .map(_.id)
        .toSet

    claims.filter(claim =>
      matchingObjectIds.contains(claim.objectId) &&
        claim.deltaScope == StrategicDeltaScope.Comparative
    )

  private def objectSquares(
      obj: StrategicObject
  ): List[Square] =
    (
      obj.locus.allSquares ++
        obj.anchors.flatMap(_.squares) ++
        obj.anchors.flatMap(_.route.toList.flatMap(_.allSquares))
    ).distinct.sortBy(_.key)
