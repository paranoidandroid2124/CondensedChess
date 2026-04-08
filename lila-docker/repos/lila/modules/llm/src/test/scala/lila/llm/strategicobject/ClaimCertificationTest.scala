package lila.llm.strategicobject

import chess.Color
import munit.FunSuite

class ClaimCertificationTest extends FunSuite:

  private val fileDuelFen = "2r3k1/8/8/8/8/8/8/2R3K1 w - - 0 1"
  private val passerRaceFen = "6k1/2P5/8/8/8/8/5p2/6K1 w - - 0 1"

  test("claim certification preserves typed deltas for stable and provisional claims") {
    val truth = PrimitiveExtractionTest.moveTransitionVisibleTruthFrameFor("c1c8")
    val contract = PrimitiveExtractionTest.moveTransitionVisibleContractFor("c1c8")
    val objects = StrategicObjectSynthesizerTest.objectsForFen(fileDuelFen, truth)
    val deltas = CanonicalStrategicObjectDeltaProjector.project(contract, truth, objects)
    val claims = CanonicalClaimCertification.certify(contract, objects, deltas)
    val access = objects.find(obj => obj.family == StrategicObjectFamily.AccessNetwork && obj.owner == Color.White).getOrElse(
      fail("expected white access network")
    )
    val shell = objects.find(obj => obj.family == StrategicObjectFamily.KingSafetyShell && obj.owner == Color.White).getOrElse(
      fail("expected white king-safety shell")
    )
    val accessClaim = claims.find(claim => claim.objectId == access.id && claim.deltaScope == StrategicDeltaScope.MoveLocal).getOrElse(
      fail("expected stable access claim")
    )
    val shellClaim = claims.find(claim => claim.objectId == shell.id && claim.deltaScope == StrategicDeltaScope.PositionLocal).getOrElse(
      fail("expected provisional shell claim")
    )

    assertEquals(accessClaim.readiness, StrategicObjectReadiness.Stable)
    assertEquals(accessClaim.status, ClaimStatus.Certified)
    assert(accessClaim.delta.nonEmpty, clue("stable claim must keep typed delta"))
    assert(accessClaim.primaryTag.nonEmpty, clue("stable claim must keep primary tag"))
    assertEquals(shellClaim.readiness, StrategicObjectReadiness.Provisional)
    assertEquals(shellClaim.status, ClaimStatus.SupportOnly)
    assert(shellClaim.delta.nonEmpty, clue("provisional claim must keep typed delta"))
  }

  test("stable move-local delta without anchored witness is demoted to support-only") {
    val truth = PrimitiveExtractionTest.moveTransitionVisibleTruthFrameFor("c1c8")
    val contract = PrimitiveExtractionTest.moveTransitionVisibleContractFor("c1c8")
    val objects = StrategicObjectSynthesizerTest.objectsForFen(fileDuelFen, truth)
    val strongDelta =
      CanonicalStrategicObjectDeltaProjector
        .project(contract, truth, objects)
        .find(delta =>
          delta.family == StrategicObjectFamily.AccessNetwork &&
            delta.owner == Color.White &&
            delta.scope == StrategicDeltaScope.MoveLocal
        )
        .getOrElse(fail("expected white access move-local delta"))
    val weakenedDelta =
      strongDelta.copy(
        projection = strongDelta.projection match
          case StrategicDeltaProjection.MoveLocal(change, witness) =>
            StrategicDeltaProjection.MoveLocal(
              change,
              witness.copy(
                relationWitnesses = Set(StrategicRelationOperator.Enables),
                primitiveKinds = Set.empty
              )
            )
          case other =>
            fail(s"expected move-local projection, got $other")
      )
    val claim =
      CanonicalClaimCertification
        .certify(contract, objects, List(weakenedDelta))
        .find(_.objectId == weakenedDelta.objectId)
        .getOrElse(fail("expected demoted access claim"))

    assertEquals(claim.status, ClaimStatus.SupportOnly)
    assert(claim.delta.nonEmpty, clue("support-only claim should keep typed delta for traceability"))
  }

  test("stable comparative certification demotes shallow metric burden but keeps strong contrast certified") {
    val row = deltaRow("fixed-target-comparative-contrastive")
    val truth = truthFor(row)
    val contract = contractFor(row)
    val objects = StrategicObjectSynthesizerTest.objectsForFen(row.fen, truth)
    val strongDelta =
      CanonicalStrategicObjectDeltaProjector
        .project(contract, truth, objects)
        .find(delta =>
          delta.family == StrategicObjectFamily.FixedTargetComplex &&
            delta.owner == Color.White &&
            delta.scope == StrategicDeltaScope.Comparative
        )
        .getOrElse(fail("expected strong fixed-target comparative delta"))
    val strongClaim =
      CanonicalClaimCertification
        .certify(contract, objects, List(strongDelta))
        .find(_.objectId == strongDelta.objectId)
        .getOrElse(fail("expected certified fixed-target claim"))
    val weakenedDelta =
      strongDelta.copy(
        projection = strongDelta.projection match
          case StrategicDeltaProjection.Comparative(change, balance, witness, counterpartObjectIds, profile) =>
            StrategicDeltaProjection.Comparative(
              change,
              balance,
              witness,
              counterpartObjectIds,
              profile.copy(metrics = profile.metrics.take(1))
            )
          case other =>
            fail(s"expected comparative projection, got $other")
      )
    val weakenedClaim =
      CanonicalClaimCertification
        .certify(contract, objects, List(weakenedDelta))
        .find(_.objectId == weakenedDelta.objectId)
        .getOrElse(fail("expected weakened fixed-target claim"))

    assertEquals(strongClaim.status, ClaimStatus.Certified)
    assertEquals(weakenedClaim.status, ClaimStatus.SupportOnly)
  }

  test("insufficient exact-board support defers instead of overclaiming") {
    val truth = PrimitiveExtractionTest.moveTransitionVisibleTruthFrameFor("c1c8")
    val contract = PrimitiveExtractionTest.moveTransitionVisibleContractFor("c1c8")
    val objects = StrategicObjectSynthesizerTest.objectsForFen(fileDuelFen, truth)
    val strongDelta =
      CanonicalStrategicObjectDeltaProjector
        .project(contract, truth, objects)
        .find(delta =>
          delta.family == StrategicObjectFamily.AccessNetwork &&
            delta.owner == Color.White &&
            delta.scope == StrategicDeltaScope.MoveLocal
        )
        .getOrElse(fail("expected white access move-local delta"))
    val unsupportedDelta = strongDelta.copy(changedAnchors = Nil, evidenceRefs = Nil)
    val claim =
      CanonicalClaimCertification
        .certify(contract, objects, List(unsupportedDelta))
        .find(_.objectId == unsupportedDelta.objectId)
        .getOrElse(fail("expected deferred access claim"))

    assertEquals(claim.status, ClaimStatus.Deferred)
    assert(claim.delta.nonEmpty, clue("deferred weak delta should remain auditable"))
  }

  test("deferred families remain claims without promoted typed delta") {
    val truth = PrimitiveExtractionTest.moveTransitionTruthFrame
    val contract = PrimitiveExtractionTest.moveTransitionContract
    val objects = StrategicObjectSynthesizerTest.objectsForFen(passerRaceFen, truth)
    val deltas =
      CanonicalStrategicObjectDeltaProjector.project(
        contract,
        truth,
        objects
      )
    val claims = CanonicalClaimCertification.certify(contract, objects, deltas)
    val planRace = objects.find(obj => obj.family == StrategicObjectFamily.PlanRace && obj.owner == Color.White).getOrElse(
      fail("expected white plan race")
    )
    val deferredClaim = claims.find(_.objectId == planRace.id).getOrElse(fail("expected deferred plan-race claim"))

    assertEquals(deferredClaim.readiness, StrategicObjectReadiness.DeferredForDelta)
    assertEquals(deferredClaim.status, ClaimStatus.Deferred)
    assert(deferredClaim.delta.isEmpty, clue("deferred claim must not materialize a direct typed delta"))
  }

  private def deltaRow(
      id: String
  ): StrategicObjectDeltaProjectorTest.DeltaExpectationRow =
    StrategicObjectDeltaProjectorTest.rows.find(_.id == id).getOrElse(
      fail(s"expected delta row $id")
    )

  private def truthFor(
      row: StrategicObjectDeltaProjectorTest.DeltaExpectationRow
  ): lila.llm.analysis.MoveTruthFrame =
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
  ): lila.llm.analysis.DecisiveTruthContract =
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
