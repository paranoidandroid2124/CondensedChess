package lila.llm.strategicobject

import chess.Square
import munit.FunSuite

class SharedTargetContinuityCertificationTest extends FunSuite:

  private val packetTargetSquare: Square =
    Square.fromKey("d6").getOrElse(
      fail("missing packet target square d6")
    )

  private val packetWitness: CertifiedBoundaryWitness =
    CertifiedBoundaryWitness.SharedTargetContinuity(packetTargetSquare)

  test("packet-owned d6 continuity witness is certified across WhatMattersHere, WhyThis, and WhatChanged") {
    val positionRow =
      CurrentPositionFixedTargetProbeTest.rows.find(_.id == "current-position-fixed-target-d6-exact").getOrElse(
        fail("expected packet-owned d6 current-position row")
      )
    val positionClaim =
      runNeutralPipeline(positionRow.fen)
        .find(claim =>
          claim.status == ClaimStatus.Certified &&
            claim.deltaScope == StrategicDeltaScope.PositionLocal &&
            SharedTargetContinuityBoundary.hasPacketContinuity(claim)
        ).getOrElse(
          fail("expected packet-owned d6 current-position continuity claim")
        )

    val fixationRow =
      TargetFixationAdmissionTest.rows.find(_.id == "target-fixation-exact").getOrElse(
        fail("expected packet-owned target-fixation row")
      )
    val fixationClaim =
      runTargetFixationPipeline(fixationRow)
        .find(claim =>
          claim.status == ClaimStatus.Certified &&
            claim.deltaScope == StrategicDeltaScope.MoveLocal &&
            SharedTargetContinuityBoundary.hasPacketContinuity(claim)
        ).getOrElse(
          fail("expected packet-owned target-fixation continuity claim")
        )

    val comparativeRow =
      ComparativeSupportAdmissionTest.rows.find(_.id == "shared-target-support-exact").getOrElse(
        fail("expected packet-owned comparative exact row")
      )
    val comparativeClaims = runComparativePipeline(comparativeRow)
    val comparativeObjects = runComparativeObjects(comparativeRow)
    val comparativeSupport =
      ComparativeSupportAdmissionTest.supportClaim(
        comparativeRow,
        comparativeObjects,
        comparativeClaims
      ).getOrElse(
        fail("expected packet-owned comparative support claim")
      )
    val comparativePrimary =
      ComparativeSupportAdmissionTest.primaryClaim(
        comparativeRow,
        comparativeObjects,
        comparativeClaims
      ).getOrElse(
        fail("expected packet-owned comparative primary claim")
      )

    val continuityClaims =
      List(positionClaim, fixationClaim, comparativePrimary, comparativeSupport)

    assert(
      SharedTargetContinuityBoundary.hasPacketContinuity(comparativePrimary),
      clue(s"expected comparative primary continuity witness, got ${claimDebug(comparativePrimary)}")
    )
    assert(
      SharedTargetContinuityBoundary.hasPacketContinuity(comparativeSupport),
      clue(s"expected comparative support continuity witness, got ${claimDebug(comparativeSupport)}")
    )
    assertEquals(
      comparativeSupport.supportingObjectIds.toSet,
      Set(
        "DefenderDependencyNetwork-white-kingside-f3-fgh"
      )
    )
    assertEquals(continuityClaims.map(_.boundaryWitnesses).toSet, Set(Set(packetWitness)))
    assert(SharedTargetContinuityBoundary.sharesPacketContinuity(comparativePrimary, comparativeSupport))
  }

  test("planner admits shared-target comparative support only from the certified d6 continuity boundary") {
    val row =
      ComparativeSupportAdmissionTest.rows.find(_.id == "shared-target-support-exact").getOrElse(
        fail("expected packet-owned exact comparative row")
      )
    val contract = ComparativeSupportAdmissionTest.contractFor(row)
    val objects = runComparativeObjects(row)
    val claims = runComparativePipeline(row)
    val primary =
      ComparativeSupportAdmissionTest.primaryClaim(row, objects, claims).getOrElse(
        fail("expected packet-owned primary claim for shared-target-support-exact")
      )
    val support =
      ComparativeSupportAdmissionTest.supportClaim(row, objects, claims).getOrElse(
        fail("expected packet-owned support claim for shared-target-support-exact")
      )
    val planned = CanonicalQuestionPlanner.plan(contract, claims)
    val strippedPlanned =
      CanonicalQuestionPlanner.plan(
        contract,
        claims.map(claim =>
          if claim.id == primary.id || claim.id == support.id then claim.copy(boundaryWitnesses = Set.empty)
          else claim
        )
      )

    assert(
      SharedTargetContinuityBoundary.hasPacketContinuity(primary),
      clue(s"expected packet-owned primary continuity witness, got ${claimDebug(primary)}")
    )
    assert(
      SharedTargetContinuityBoundary.hasPacketContinuity(support),
      clue(s"expected packet-owned support continuity witness, got ${claimDebug(support)}")
    )
    assertEquals(planned.supportClaimIds, List(support.id), clue("expected support via certified continuity"))
    assertEquals(strippedPlanned.supportClaimIds, Nil, clue("stripping continuity witness must close support"))
  }

  test("non-packet white comparative pair stays outside the continuity witness") {
    val row =
      ComparativeSupportAdmissionTest.rows.find(_.id == "shared-target-support-contrastive").getOrElse(
        fail("expected contrastive comparative row")
      )
    val contract = ComparativeSupportAdmissionTest.contractFor(row)
    val objects = runComparativeObjects(row)
    val claims = runComparativePipeline(row)
    val primary =
      ComparativeSupportAdmissionTest.primaryClaim(row, objects, claims).getOrElse(
        fail("expected contrastive primary claim")
      )
    val support =
      ComparativeSupportAdmissionTest.supportClaim(row, objects, claims).getOrElse(
        fail("expected contrastive support claim")
      )
    val planned = CanonicalQuestionPlanner.plan(contract, claims)

    assert(
      !SharedTargetContinuityBoundary.hasPacketContinuity(primary),
      clue(s"contrastive row must not receive packet continuity, got ${claimDebug(primary)}")
    )
    assert(
      !SharedTargetContinuityBoundary.hasPacketContinuity(support),
      clue(s"contrastive support must not receive packet continuity, got ${claimDebug(support)}")
    )
    assertEquals(planned.supportClaimIds, Nil, clue("contrastive row must not attach support through the witness boundary"))
  }

  test("preserved c6 probe does not receive the packet-owned d6 continuity witness") {
    val row =
      CurrentPositionFixedTargetProbeTest.rows.find(_.id == "current-position-fixed-target-exact").getOrElse(
        fail("expected preserved c6 current-position row")
      )
    val claims = runNeutralPipeline(row.fen)
    val fixedTargetClaims =
      claims.filter(claim =>
        claim.deltaScope == StrategicDeltaScope.PositionLocal &&
          claim.primaryTag.contains(StrategicDeltaTag.TargetFixed)
      )

    assert(fixedTargetClaims.nonEmpty, clue("expected preserved c6 fixed-target claim"))
    assert(fixedTargetClaims.forall(!SharedTargetContinuityBoundary.hasPacketContinuity(_)))
  }

  test("near-miss and wrong-support comparative rows remain outside the packet-owned continuity boundary") {
    val nearMissRow =
      ComparativeSupportAdmissionTest.rows.find(_.id == "shared-target-support-near-miss").getOrElse(
        fail("expected comparative near-miss row")
      )
    val nearMissClaims = runComparativePipeline(nearMissRow)
    val nearMissComparativeClaims =
      nearMissClaims.filter(_.deltaScope == StrategicDeltaScope.Comparative)

    val wrongSupportRow =
      ComparativeSupportAdmissionTest.rows.find(_.id == "shared-target-support-negative").getOrElse(
        fail("expected wrong-support comparative row")
      )
    val wrongSupportObjects = runComparativeObjects(wrongSupportRow)
    val wrongSupportClaims = runComparativePipeline(wrongSupportRow)
    val wrongSupportCandidate =
      ComparativeSupportAdmissionTest.supportClaim(wrongSupportRow, wrongSupportObjects, wrongSupportClaims).getOrElse(
        fail("expected wrong-support comparative candidate")
      )

    assert(nearMissComparativeClaims.nonEmpty, clue("expected near-miss comparative claims"))
    assert(nearMissComparativeClaims.forall(!SharedTargetContinuityBoundary.hasPacketContinuity(_)))
    assert(!SharedTargetContinuityBoundary.hasPacketContinuity(wrongSupportCandidate))
  }

  private def runNeutralPipeline(
      fen: String
  ): List[CertifiedClaim] =
    val truth = PrimitiveExtractionTest.neutralTruthFrame
    val contract = PrimitiveExtractionTest.neutralContract
    val objects = StrategicObjectSynthesizerTest.objectsForFen(fen, truth)
    val deltas = CanonicalStrategicObjectDeltaProjector.project(contract, truth, objects)
    CanonicalClaimCertification.certify(contract, objects, deltas)

  private def runTargetFixationPipeline(
      row: TargetFixationAdmissionTest.TargetFixationRow
  ): List[CertifiedClaim] =
    val truth = TargetFixationAdmissionTest.truthFor(row)
    val contract = TargetFixationAdmissionTest.contractFor(row)
    val objects = StrategicObjectSynthesizerTest.objectsForFen(row.fen, truth)
    val deltas = CanonicalStrategicObjectDeltaProjector.project(contract, truth, objects)
    CanonicalClaimCertification.certify(contract, objects, deltas)

  private def runComparativePipeline(
      row: ComparativeSupportAdmissionTest.ComparativeSupportRow
  ): List[CertifiedClaim] =
    val truth = ComparativeSupportAdmissionTest.truthFor(row)
    val contract = ComparativeSupportAdmissionTest.contractFor(row)
    val objects = runComparativeObjects(row)
    val deltas = CanonicalStrategicObjectDeltaProjector.project(contract, truth, objects)
    CanonicalClaimCertification.certify(contract, objects, deltas)

  private def runComparativeObjects(
      row: ComparativeSupportAdmissionTest.ComparativeSupportRow
  ): List[StrategicObject] =
    val truth = ComparativeSupportAdmissionTest.truthFor(row)
    StrategicObjectSynthesizerTest.objectsForFen(row.fen, truth)

  private def claimDebug(
      claim: CertifiedClaim
  ): String =
    claim.delta match
      case Some(delta) =>
        s"id=${claim.id} status=${claim.status} witnesses=${claim.boundaryWitnesses} profile=${delta.profile} projection=${delta.projection} changed=${delta.changedAnchors.flatMap(_.squares).map(_.key)} support=${delta.supportingObjectIds}"
      case None =>
        s"id=${claim.id} status=${claim.status} witnesses=${claim.boundaryWitnesses} delta=none"
