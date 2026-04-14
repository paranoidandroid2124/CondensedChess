package lila.llm.strategicobject

import chess.Square
import munit.FunSuite
import play.api.libs.json.*

import scala.io.Source

class FavorableSimplificationAdmissionTest extends FunSuite:

  import FavorableSimplificationAdmissionTest.*

  test("packet favorable-simplification corpus covers exact negative contrastive and near-miss rows") {
    assertEquals(rows.map(_.caseType).toSet, Set("exact", "negative", "contrastive", "near_miss", "nasty_negative"))
  }

  test("trade-invariant primary descriptor centralizes packet-owned and general primary reasons") {
    val packetDescriptor =
      TradeInvariantPrimaryDescriptor.describe(
        owner = chess.Color.White,
        exchangeSquares = List(Square.E6),
        invariantSquares = List(Square.E6, Square.D5),
        preservedFamilies = Set(StrategicObjectFamily.FixedTargetComplex),
        features = Set(TradeInvariantFeature.FixedTargetAnchor)
      )
    val breakDescriptor =
      TradeInvariantPrimaryDescriptor.describe(
        owner = chess.Color.White,
        exchangeSquares = List(Square.D5),
        invariantSquares = List(Square.D5, Square.E6, Square.C6),
        preservedFamilies = Set(
          StrategicObjectFamily.AccessNetwork,
          StrategicObjectFamily.FixedTargetComplex,
          StrategicObjectFamily.BreakAxis
        ),
        features = Set(
          TradeInvariantFeature.AccessAnchor,
          TradeInvariantFeature.FixedTargetAnchor,
          TradeInvariantFeature.BreakAnchor
        )
      )
    val nonPrimaryDescriptor =
      TradeInvariantPrimaryDescriptor.describe(
        owner = chess.Color.White,
        exchangeSquares = List(Square.E4),
        invariantSquares = List(Square.E4, Square.D5, Square.C6),
        preservedFamilies = Set(
          StrategicObjectFamily.AccessNetwork,
          StrategicObjectFamily.FixedTargetComplex
        ),
        features = Set(
          TradeInvariantFeature.AccessAnchor,
          TradeInvariantFeature.FixedTargetAnchor
        )
      )

    assert(packetDescriptor.primaryEligible, clue(packetDescriptor))
    assert(packetDescriptor.packetPrimaryEligible, clue(packetDescriptor))
    assertEquals(
      packetDescriptor.primaryReason,
      Some(TradeInvariantPrimaryReason.PacketOwnedFixedTargetSlice)
    )

    assert(breakDescriptor.primaryEligible, clue(breakDescriptor))
    assert(!breakDescriptor.packetPrimaryEligible, clue(breakDescriptor))
    assertEquals(
      breakDescriptor.primaryReason,
      Some(TradeInvariantPrimaryReason.BreakBackedInvariant)
    )
    assertEquals(
      breakDescriptor.primaryRelevantPreservedFamilies,
      Set(
        StrategicObjectFamily.FixedTargetComplex,
        StrategicObjectFamily.BreakAxis
      )
    )

    assert(!nonPrimaryDescriptor.primaryEligible, clue(nonPrimaryDescriptor))
    assert(!nonPrimaryDescriptor.packetPrimaryEligible, clue(nonPrimaryDescriptor))
    assertEquals(nonPrimaryDescriptor.primaryReason, None)
  }

  test("access-backed persistence does not satisfy the primary trade-invariant witness boundary by itself") {
    val exchangeSquares = List(Square.E5)
    val invariantSquares = List(Square.E5)
    val preservedFamilies = Set(StrategicObjectFamily.AccessNetwork)
    val features = Set(TradeInvariantFeature.AccessAnchor)

    assertEquals(
      TradeInvariantPersistenceBoundary.persistenceWitnessCount(
        exchangeSquares,
        invariantSquares,
        preservedFamilies,
        features
      ),
      0
    )
    assert(
      !TradeInvariantPersistenceBoundary.eligibleForPrimarySimplification(
        exchangeSquares,
        invariantSquares,
        preservedFamilies,
        features
      )
    )
  }

  test("access-only fixed-target simplification needs either queen exchange or deep defender removal") {
    val exchangeSquares = List(Square.E4)
    val invariantSquares = List(Square.E4, Square.D5, Square.C6)
    val preservedFamilies = Set(
      StrategicObjectFamily.AccessNetwork,
      StrategicObjectFamily.FixedTargetComplex
    )

    assert(
      !TradeInvariantPersistenceBoundary.eligibleForPrimarySimplification(
        exchangeSquares,
        invariantSquares,
        preservedFamilies,
        Set(
          TradeInvariantFeature.AccessAnchor,
          TradeInvariantFeature.FixedTargetAnchor
        )
      )
    )
    assert(
      TradeInvariantPersistenceBoundary.eligibleForPrimarySimplification(
        exchangeSquares,
        List(Square.E4, Square.D5, Square.C6, Square.B7, Square.A8, Square.H4),
        preservedFamilies,
        Set(
          TradeInvariantFeature.AccessAnchor,
          TradeInvariantFeature.FixedTargetAnchor,
          TradeInvariantFeature.QueenExchange
        )
      )
    )
    assert(
      TradeInvariantPersistenceBoundary.eligibleForPrimarySimplification(
        List(Square.D7),
        List(Square.D7, Square.E6, Square.C6, Square.B5, Square.A4, Square.H5),
        preservedFamilies,
        Set(
          TradeInvariantFeature.AccessAnchor,
          TradeInvariantFeature.FixedTargetAnchor,
          TradeInvariantFeature.DeepDefenderRemoval
        )
      )
    )
    assert(
      TradeInvariantPersistenceBoundary.eligibleForPrimarySimplification(
        List(Square.E6),
        List(Square.E6, Square.D5, Square.C4, Square.B4, Square.A5, Square.H5),
        preservedFamilies,
        Set(
          TradeInvariantFeature.AccessAnchor,
          TradeInvariantFeature.FixedTargetAnchor,
          TradeInvariantFeature.ReleaseOverlap
        )
      )
    )
    assert(
      !TradeInvariantPersistenceBoundary.eligibleForPrimarySimplification(
        List(Square.E4),
        List(Square.E4, Square.D5, Square.C6, Square.B7),
        preservedFamilies,
        Set(
          TradeInvariantFeature.AccessAnchor,
          TradeInvariantFeature.FixedTargetAnchor,
          TradeInvariantFeature.QueenExchange
        )
      )
    )
  }

  test("exchange cascade does not stay primary even with break-backed persistence") {
    assert(
      !TradeInvariantPersistenceBoundary.eligibleForPrimarySimplification(
        exchangeSquares = List(Square.D5),
        invariantSquares = List(Square.D5, Square.E6, Square.C6),
        preservedFamilies = Set(
          StrategicObjectFamily.AccessNetwork,
          StrategicObjectFamily.FixedTargetComplex,
          StrategicObjectFamily.BreakAxis
        ),
        features = Set(
          TradeInvariantFeature.AccessAnchor,
          TradeInvariantFeature.FixedTargetAnchor,
          TradeInvariantFeature.BreakAnchor,
          TradeInvariantFeature.ReleaseOverlap,
          TradeInvariantFeature.ExchangeCascade
        )
      )
    )
    assert(
      !TradeInvariantPersistenceBoundary.eligibleForPrimarySimplification(
        exchangeSquares = List(Square.D5, Square.E6, Square.C6),
        invariantSquares = List(Square.D5, Square.E6, Square.C6, Square.C5),
        preservedFamilies = Set(
          StrategicObjectFamily.AccessNetwork,
          StrategicObjectFamily.FixedTargetComplex,
          StrategicObjectFamily.BreakAxis
        ),
        features = Set(
          TradeInvariantFeature.AccessAnchor,
          TradeInvariantFeature.FixedTargetAnchor,
          TradeInvariantFeature.BreakAnchor,
          TradeInvariantFeature.ReleaseOverlap
        )
      )
    )
  }

  rows.foreach { row =>
    test(s"favorable simplification expectation ${row.id}") {
      val truth = truthFor(row)
      val contract = contractFor(row)
      val objects = StrategicObjectSynthesizerTest.objectsForFen(row.fen, truth)
      val deltas = CanonicalStrategicObjectDeltaProjector.project(contract, truth, objects)
      val claims = CanonicalClaimCertification.certify(contract, objects, deltas)
      val planned = CanonicalQuestionPlanner.plan(contract, claims)

      val objectIds = tradeInvariantObjectIds(row, objects)
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
      val plannerAdmission = admission(planned, moveLocalClaims)
      val localizedStage = localization(planned, objectIds, moveLocalDeltas, moveLocalClaims)
      val debugSummary =
        s"${row.id}: plannerAdmission=$plannerAdmission localization=$localizedStage planned=$planned objectIds=${objectIds.toList.sorted} moveLocalDeltas=${moveLocalDeltas.map(delta => s"${delta.primaryTag}:${delta.objectId}")} moveLocalClaims=${moveLocalClaims.map(claim => s"${claim.status}:${claim.id}")}"

      assertEquals(plannerAdmission, row.plannerAdmission, clue(debugSummary))
      assertEquals(localizedStage, row.localization, clue(debugSummary))

      row.expectation match
        case "primary" =>
          val primaryClaim =
            moveLocalClaims.find(claim =>
              claim.delta.exists(TradeInvariantSimplificationSlice.isPacketOwnedPrimarySimplificationDelta)
            ).getOrElse(
              fail(s"${row.id}: expected packet-owned primary simplification claim")
            )

          assert(objectIds.nonEmpty, clue(s"${row.id}: expected TradeInvariant object"))
          assert(moveLocalDeltas.nonEmpty, clue(s"${row.id}: expected move-local trade-invariant delta"))
          assert(moveLocalDeltas.forall(_.primaryTag == StrategicDeltaTag.TradePreserved), clue(s"${row.id}: expected bounded favorable-simplification delta tag"))
          assert(primaryClaim.primaryTag.contains(StrategicDeltaTag.TradePreserved), clue(s"${row.id}: expected trade-preserved certification"))
          assertEquals(planned.axis, QuestionAxis.WhyThis)
          assertEquals(planned.claimIds, List(primaryClaim.id), clue(s"${row.id}: expected isolated primary admission"))
        case "none" =>
          assert(moveLocalDeltas.isEmpty, clue(s"${row.id}: non-slice board must not emit a move-local TradeInvariant delta"))
          assert(moveLocalClaims.isEmpty, clue(s"${row.id}: expected no move-local TradeInvariant claim"))
        case other =>
          fail(s"${row.id}: unsupported expectation=$other")
    }
  }

object FavorableSimplificationAdmissionTest:

  final case class FavorableSimplificationRow(
      id: String,
      caseType: String,
      source: String,
      fen: String,
      playedMove: String,
      truthCase: String,
      family: String,
      owner: String,
      anchor: Option[String],
      expectation: String,
      plannerAdmission: String,
      localization: String,
      axis: Option[String]
  )

  private given Reads[FavorableSimplificationRow] = Json.reads[FavorableSimplificationRow]

  val rows: List[FavorableSimplificationRow] =
    Source
      .fromResource("strategic-object-corpus/favorable-simplification-expectations.jsonl")
      .getLines()
      .toList
      .map(_.trim)
      .filter(_.nonEmpty)
      .zipWithIndex
      .map { case (line, idx) =>
        Json.parse(line).validate[FavorableSimplificationRow].asEither match
          case Right(row) => row
          case Left(err)  => throw new IllegalArgumentException(s"invalid favorable simplification row ${idx + 1}: $err")
      }

  def truthFor(
      row: FavorableSimplificationRow
  ): lila.llm.analysis.MoveTruthFrame =
    row.truthCase match
      case "move_transition_visible" =>
        PrimitiveExtractionTest.moveTransitionVisibleTruthFrameFor(row.playedMove)
      case other =>
        throw new IllegalArgumentException(s"${row.id}: unsupported truthCase=$other")

  def contractFor(
      row: FavorableSimplificationRow
  ): lila.llm.analysis.DecisiveTruthContract =
    row.truthCase match
      case "move_transition_visible" =>
        PrimitiveExtractionTest.moveTransitionVisibleContractFor(row.playedMove)
      case other =>
        throw new IllegalArgumentException(s"${row.id}: unsupported truthCase=$other")

  private def tradeInvariantObjectIds(
      row: FavorableSimplificationRow,
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
