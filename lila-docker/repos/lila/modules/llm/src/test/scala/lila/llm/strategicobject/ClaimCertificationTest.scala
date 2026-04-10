package lila.llm.strategicobject

import chess.Square
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
    val unsupportedClaim =
      CanonicalClaimCertification
        .certify(
          contract,
          objects,
          List(
            strongDelta.copy(
              changedAnchors = Nil,
              evidenceRefs = Nil
            )
          )
        )
        .find(_.objectId == strongDelta.objectId)
        .getOrElse(fail("expected deferred fixed-target claim"))
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
    assertEquals(unsupportedClaim.status, ClaimStatus.Deferred)
    assertEquals(weakenedClaim.status, ClaimStatus.SupportOnly)
  }

  test("packet-owned shared-target comparative near-miss stays support-only or deferred at certification") {
    val exactRow =
      ComparativeSupportAdmissionTest.rows.find(_.id == "shared-target-support-exact").getOrElse(
        fail("expected comparative exact row")
      )
    val contrastiveRow =
      ComparativeSupportAdmissionTest.rows.find(_.id == "shared-target-support-contrastive").getOrElse(
        fail("expected comparative contrastive row")
      )
    val nearMissRow =
      ComparativeSupportAdmissionTest.rows.find(_.id == "shared-target-support-near-miss").getOrElse(
        fail("expected comparative near-miss row")
      )

    val exactTruth = ComparativeSupportAdmissionTest.truthFor(exactRow)
    val exactContract = ComparativeSupportAdmissionTest.contractFor(exactRow)
    val exactObjects = StrategicObjectSynthesizerTest.objectsForFen(exactRow.fen, exactTruth)
    val exactDeltas = CanonicalStrategicObjectDeltaProjector.project(exactContract, exactTruth, exactObjects)
    val exactClaims = CanonicalClaimCertification.certify(exactContract, exactObjects, exactDeltas)
    val exactPrimary =
      ComparativeSupportAdmissionTest.primaryClaim(exactRow, exactObjects, exactClaims).getOrElse(
        fail("expected exact comparative primary claim")
      )
    val exactSupport =
      ComparativeSupportAdmissionTest.supportClaim(exactRow, exactObjects, exactClaims).getOrElse(
        fail("expected exact comparative support claim")
      )

    assertEquals(exactPrimary.status, ClaimStatus.Certified)
    assertEquals(exactSupport.status, ClaimStatus.SupportOnly)
    assertEquals(exactPrimary.deltaScope, StrategicDeltaScope.Comparative)
    assertEquals(exactSupport.deltaScope, StrategicDeltaScope.Comparative)
    assert(SharedTargetContinuityBoundary.hasPacketContinuity(exactPrimary), clue("exact comparative primary must carry the certified continuity witness"))
    assert(SharedTargetContinuityBoundary.hasPacketContinuity(exactSupport), clue("exact comparative support must carry the certified continuity witness"))

    val contrastiveTruth = ComparativeSupportAdmissionTest.truthFor(contrastiveRow)
    val contrastiveContract = ComparativeSupportAdmissionTest.contractFor(contrastiveRow)
    val contrastiveObjects = StrategicObjectSynthesizerTest.objectsForFen(contrastiveRow.fen, contrastiveTruth)
    val contrastiveDeltas = CanonicalStrategicObjectDeltaProjector.project(contrastiveContract, contrastiveTruth, contrastiveObjects)
    val contrastiveClaims = CanonicalClaimCertification.certify(contrastiveContract, contrastiveObjects, contrastiveDeltas)
    val contrastivePrimary =
      ComparativeSupportAdmissionTest.primaryClaim(contrastiveRow, contrastiveObjects, contrastiveClaims).getOrElse(
        fail("expected contrastive comparative primary claim")
      )
    val contrastiveSupport =
      ComparativeSupportAdmissionTest.supportClaim(contrastiveRow, contrastiveObjects, contrastiveClaims).getOrElse(
        fail("expected contrastive comparative support claim")
      )

    assert(!SharedTargetContinuityBoundary.hasPacketContinuity(contrastivePrimary), clue("contrastive comparative primary must stay outside the certified continuity witness"))
    assert(!SharedTargetContinuityBoundary.hasPacketContinuity(contrastiveSupport), clue("contrastive comparative support must stay outside the certified continuity witness"))

    val nearTruth = ComparativeSupportAdmissionTest.truthFor(nearMissRow)
    val nearContract = ComparativeSupportAdmissionTest.contractFor(nearMissRow)
    val nearObjects = StrategicObjectSynthesizerTest.objectsForFen(nearMissRow.fen, nearTruth)
    val nearDeltas = CanonicalStrategicObjectDeltaProjector.project(nearContract, nearTruth, nearObjects)
    val nearClaims = CanonicalClaimCertification.certify(nearContract, nearObjects, nearDeltas)
    val nearSupport =
      ComparativeSupportAdmissionTest.supportClaim(nearMissRow, nearObjects, nearClaims).getOrElse(
        fail("expected near-miss comparative claim")
      )
    val nearSummary =
      nearClaims
        .filter(_.deltaScope == StrategicDeltaScope.Comparative)
        .map(claim =>
          s"${claim.objectId}:${claim.status}:${claim.primaryTag}:${claim.delta.flatMap(_.comparativeProfile).map(_.metrics.size)}"
        )
        .mkString(", ")

    assert(nearClaims.exists(_.deltaScope == StrategicDeltaScope.Comparative), clue("expected comparative delta for near-miss row"))
    assert(
      nearSupport.status == ClaimStatus.SupportOnly || nearSupport.status == ClaimStatus.Deferred,
      clue(s"near-miss comparative must stay support-only or deferred, got ${nearSupport.status}, claims=[$nearSummary]")
    )
    assert(!SharedTargetContinuityBoundary.hasPacketContinuity(nearSupport), clue("near-miss comparative support must stay outside the continuity boundary"))
  }

  test("Tier-1 provisional comparative near-miss rows stay support-only and localize at certification") {
    val provisionalFamilies =
      StrategicObjectFamily.directDeltaOwners.filter(family =>
        StrategicObjectFamilyContract.forFamily(family).defaultReadiness == StrategicObjectReadiness.Provisional
      )
    val shallowRows =
      StrategicObjectDeltaProjectorTest.rows.filter(row =>
        row.caseType == "near_miss" &&
          row.expectation == "present" &&
          row.plannerExpectation.contains("none") &&
          row.localizationExpectation.contains("certification") &&
          row.scope == "comparative" &&
          provisionalFamilies.contains(StrategicObjectDeltaProjectorTest.parseFamily(row.family))
      )

    assertEquals(shallowRows.map(_.family).toSet, provisionalFamilies.map(_.toString).toSet)
    shallowRows.foreach { row =>
      val truth = truthFor(row)
      val contract = contractFor(row)
      val objects = StrategicObjectSynthesizerTest.objectsForFen(row.fen, truth)
      val deltas = CanonicalStrategicObjectDeltaProjector.project(contract, truth, objects)
      val claims = CanonicalClaimCertification.certify(contract, objects, deltas)
      val objectIds =
        objects
          .filter(obj =>
            obj.family == StrategicObjectDeltaProjectorTest.parseFamily(row.family) &&
              obj.owner == StrategicObjectDeltaProjectorTest.parseColor(row.owner)
          )
          .map(_.id)
          .toSet
      val comparativeClaims =
        claims.filter(claim =>
          objectIds.contains(claim.objectId) &&
            claim.deltaScope == StrategicDeltaScope.Comparative
        )

      assert(comparativeClaims.nonEmpty, clue(s"${row.id}: expected comparative claim"))
      assert(
        comparativeClaims.forall(_.status == ClaimStatus.SupportOnly),
        clue(s"${row.id}: shallow provisional comparative should stay support-only, got $comparativeClaims")
      )
    }
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

  test("coordination probe certification promotes only the packet-owned exact current-position slice") {
    val truth = PrimitiveExtractionTest.neutralTruthFrame
    val contract = PrimitiveExtractionTest.neutralContract
    val exactRow =
      CurrentPositionCoordinationProbeTest.rows.find(_.caseType == "exact").getOrElse(
        fail("expected current-position coordination exact row")
      )
    val exactObjects = StrategicObjectSynthesizerTest.objectsForFen(exactRow.fen, truth)
    val exactDeltas = CanonicalStrategicObjectDeltaProjector.project(contract, truth, exactObjects)
    val exactClaims = CanonicalClaimCertification.certify(contract, exactObjects, exactDeltas)
    val exactObjectIds = coordinationTargetObjectIds(exactRow, exactObjects)
    val exactPositionClaims =
      exactClaims.filter(claim =>
        exactObjectIds.contains(claim.objectId) &&
          claim.deltaScope == StrategicDeltaScope.PositionLocal
      )
    val exactPrimary =
      exactPositionClaims.find(claim =>
        claim.status == ClaimStatus.Certified &&
          claim.primaryTag.contains(StrategicDeltaTag.CoordinationImproved)
      )

    assert(exactObjectIds.nonEmpty, clue("expected at least one exact coordination object"))
    assert(exactPrimary.nonEmpty, clue(s"expected certified coordination probe claim, got $exactPositionClaims"))

    val closedRows = CurrentPositionCoordinationProbeTest.rows.filter(_.caseType != "exact")
    closedRows.foreach { row =>
      val objects = StrategicObjectSynthesizerTest.objectsForFen(row.fen, truth)
      val deltas = CanonicalStrategicObjectDeltaProjector.project(contract, truth, objects)
      val claims = CanonicalClaimCertification.certify(contract, objects, deltas)
      val objectIds = coordinationTargetObjectIds(row, objects)
      val positionClaims =
        claims.filter(claim =>
          objectIds.contains(claim.objectId) &&
            claim.deltaScope == StrategicDeltaScope.PositionLocal
        )
      val certifiedCoordination = positionClaims.filter(claim =>
        claim.status == ClaimStatus.Certified &&
          claim.primaryTag.contains(StrategicDeltaTag.CoordinationImproved)
      )
      assertEquals(
        certifiedCoordination,
        Nil,
        clue(s"${row.id}: expected closed coordination slice, claims=$positionClaims")
      )
    }
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

  private def coordinationTargetObjectIds(
      row: CurrentPositionCoordinationProbeTest.CurrentPositionCoordinationProbeRow,
      objects: List[StrategicObject]
  ): Set[String] =
    val family = StrategicObjectSynthesizerTest.parseFamily(row.family)
    val owner = StrategicObjectSynthesizerTest.parseColor(row.owner)
    val anchor = row.anchor.flatMap(StrategicObjectSynthesizerTest.parseSquare)

    objects
      .filter(obj =>
        obj.family == family &&
          obj.owner == owner &&
          anchor.forall(coordinationObjectSquares(obj).contains)
      )
      .map(_.id)
      .toSet

  private def coordinationObjectSquares(
      obj: StrategicObject
  ): List[Square] =
    (
      obj.locus.allSquares ++
        obj.anchors.flatMap(_.squares) ++
        obj.anchors.flatMap(_.route.toList.flatMap(_.allSquares))
    ).distinct.sortBy(_.key)
