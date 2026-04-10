package lila.llm.strategicobject

import chess.Square
import munit.FunSuite

class ThinShellCertifiedRendererBoundaryTest extends FunSuite:

  test("P9-A01 exact comparative support survives as thin-shell primary/support only") {
    val row =
      ComparativeSupportAdmissionTest.rows.find(_.id == "shared-target-support-exact").getOrElse(
        fail("expected comparative exact row")
      )
    val truth = ComparativeSupportAdmissionTest.truthFor(row)
    val contract = ComparativeSupportAdmissionTest.contractFor(row)
    val (objects, _, claims, planned) = runPipeline(row.fen, truth, contract)
    val primary =
      ComparativeSupportAdmissionTest.primaryClaim(row, objects, claims).getOrElse(
        fail("expected comparative primary claim")
      )
    val support =
      ComparativeSupportAdmissionTest.supportClaim(row, objects, claims).getOrElse(
        fail("expected comparative support claim")
      )

    assertEquals(planned.axis, QuestionAxis.WhatChanged)
    assert(planned.claimIds.contains(primary.id), clue("expected comparative primary admission"))
    assertEquals(support.status, ClaimStatus.SupportOnly)
    assertEquals(planned.supportClaimIds, List(support.id))
    assertThinShellMirror(planned, claims)
  }

  test("comparative near-miss support stays closed at shell") {
    val row =
      ComparativeSupportAdmissionTest.rows.find(_.id == "shared-target-support-near-miss").getOrElse(
        fail("expected comparative near-miss row")
      )
    val truth = ComparativeSupportAdmissionTest.truthFor(row)
    val contract = ComparativeSupportAdmissionTest.contractFor(row)
    val (objects, _, claims, planned) = runPipeline(row.fen, truth, contract)
    val support = ComparativeSupportAdmissionTest.supportClaim(row, objects, claims)

    assertEquals(row.expectation, "none")
    support.foreach(claim => assert(!planned.supportClaimIds.contains(claim.id), clue("near-miss support must stay closed")))
    assertThinShellMirror(planned, claims)
  }

  test("P9-A02 exact target fixation survives as thin-shell primary only") {
    val row =
      TargetFixationAdmissionTest.rows.find(_.id == "target-fixation-exact").getOrElse(
        fail("expected target-fixation exact row")
      )
    val truth = TargetFixationAdmissionTest.truthFor(row)
    val contract = TargetFixationAdmissionTest.contractFor(row)
    val (objects, _, claims, planned) = runPipeline(row.fen, truth, contract)
    val objectIds = objectIdsFor(row.family, row.owner, Some(row.anchor), objects)
    val moveLocalClaims = claimsFor(objectIds, StrategicDeltaScope.MoveLocal, claims)

    assertEquals(row.expectation, "primary")
    assertEquals(admission(planned, moveLocalClaims), row.plannerAdmission)
    assert(moveLocalClaims.exists(_.status == ClaimStatus.Certified), clue("expected certified move-local fixation claim"))
    assertThinShellMirror(planned, claims)
  }

  test("target-fixation near-miss stays planner-none at shell") {
    val row =
      TargetFixationAdmissionTest.rows.find(_.id == "target-fixation-near-miss").getOrElse(
        fail("expected target-fixation near-miss row")
      )
    val truth = TargetFixationAdmissionTest.truthFor(row)
    val contract = TargetFixationAdmissionTest.contractFor(row)
    val (objects, _, claims, planned) = runPipeline(row.fen, truth, contract)
    val objectIds = objectIdsFor(row.family, row.owner, Some(row.anchor), objects)
    val moveLocalClaims = claimsFor(objectIds, StrategicDeltaScope.MoveLocal, claims)

    assertEquals(row.expectation, "none")
    assertEquals(admission(planned, moveLocalClaims), row.plannerAdmission)
    assert(moveLocalClaims.isEmpty, clue("near-miss should not emit move-local fixation claims"))
    assertThinShellMirror(planned, claims)
  }

  test("P9-A03 exact favorable simplification survives as thin-shell primary only") {
    val row =
      FavorableSimplificationAdmissionTest.rows.find(_.id == "bounded-favorable-simplification-exact").getOrElse(
        fail("expected favorable-simplification exact row")
      )
    val truth = FavorableSimplificationAdmissionTest.truthFor(row)
    val contract = FavorableSimplificationAdmissionTest.contractFor(row)
    val (objects, _, claims, planned) = runPipeline(row.fen, truth, contract)
    val objectIds = objectIdsFor(row.family, row.owner, row.anchor, objects)
    val moveLocalClaims = claimsFor(objectIds, StrategicDeltaScope.MoveLocal, claims)
    val primary =
      moveLocalClaims.find(claim => claim.delta.exists(TradeInvariantSimplificationSlice.isPacketOwnedPrimarySimplificationDelta)).getOrElse(
        fail("expected packet-owned primary simplification claim")
      )

    assertEquals(row.expectation, "primary")
    assertEquals(admission(planned, moveLocalClaims), row.plannerAdmission)
    assert(moveLocalClaims.exists(_.status == ClaimStatus.Certified), clue("expected certified move-local simplification claim"))
    assertEquals(planned.claimIds, List(primary.id), clue("thin shell must isolate the primary simplification claim"))
    assertThinShellMirror(planned, claims)
  }

  test("favorable-simplification near-miss stays planner-none at shell") {
    val row =
      FavorableSimplificationAdmissionTest.rows.find(_.id == "bounded-favorable-simplification-near-miss").getOrElse(
        fail("expected favorable-simplification near-miss row")
      )
    val truth = FavorableSimplificationAdmissionTest.truthFor(row)
    val contract = FavorableSimplificationAdmissionTest.contractFor(row)
    val (objects, _, claims, planned) = runPipeline(row.fen, truth, contract)
    val objectIds = objectIdsFor(row.family, row.owner, row.anchor, objects)
    val moveLocalClaims = claimsFor(objectIds, StrategicDeltaScope.MoveLocal, claims)

    assertEquals(row.expectation, "none")
    assertEquals(admission(planned, moveLocalClaims), row.plannerAdmission)
    assert(moveLocalClaims.isEmpty, clue("near-miss should not emit move-local simplification claims"))
    assertThinShellMirror(planned, claims)
  }

  test("P9-A04 exact current-position fixed-target survives as thin-shell primary only") {
    val row =
      CurrentPositionFixedTargetProbeTest.rows.find(_.id == "current-position-fixed-target-exact").getOrElse(
        fail("expected current-position fixed-target exact row")
      )
    val (objects, _, claims, planned) = runNeutralPipeline(row.fen)
    val objectIds = objectIdsFor(row.family, row.owner, row.anchor, objects)
    val positionClaims = claimsFor(objectIds, StrategicDeltaScope.PositionLocal, claims)

    assertEquals(row.expectation, "primary")
    assertEquals(admission(planned, positionClaims), row.plannerAdmission)
    assert(positionClaims.exists(_.status == ClaimStatus.Certified), clue("expected certified position-local fixed-target claim"))
    assertThinShellMirror(planned, claims)
  }

  test("P9-A04b packet-owned d6 current-position fixed-target survives as thin-shell primary only") {
    val row =
      CurrentPositionFixedTargetProbeTest.rows.find(_.id == "current-position-fixed-target-d6-exact").getOrElse(
        fail("expected packet-owned d6 current-position fixed-target exact row")
      )
    val (objects, _, claims, planned) = runNeutralPipeline(row.fen)
    val objectIds = objectIdsFor(row.family, row.owner, row.anchor, objects)
    val positionClaims = claimsFor(objectIds, StrategicDeltaScope.PositionLocal, claims)

    assertEquals(row.expectation, "primary")
    assertEquals(admission(planned, positionClaims), row.plannerAdmission)
    assert(positionClaims.exists(_.status == ClaimStatus.Certified), clue("expected certified d6 position-local fixed-target claim"))
    assertThinShellMirror(planned, claims)
  }

  test("K03A fixed-target negative stays closed at shell") {
    val row =
      CurrentPositionFixedTargetProbeTest.rows.find(_.id == "current-position-fixed-target-negative").getOrElse(
        fail("expected current-position fixed-target negative row")
      )
    val (objects, _, claims, planned) = runNeutralPipeline(row.fen)
    val objectIds = objectIdsFor(row.family, row.owner, row.anchor, objects)
    val positionClaims = claimsFor(objectIds, StrategicDeltaScope.PositionLocal, claims)

    assertEquals(row.expectation, "none")
    assertEquals(admission(planned, positionClaims), row.plannerAdmission)
    assert(positionClaims.forall(claim => !planned.claimIds.contains(claim.id) && !planned.supportClaimIds.contains(claim.id)))
    assertThinShellMirror(planned, claims)
  }

  test("P9-A05 exact current-position coordination survives as thin-shell primary only") {
    val row =
      CurrentPositionCoordinationProbeTest.rows.find(_.id == "current-position-coordination-probe-exact").getOrElse(
        fail("expected current-position coordination exact row")
      )
    val (objects, _, claims, planned) = runNeutralPipeline(row.fen)
    val objectIds = objectIdsFor(row.family, row.owner, row.anchor, objects)
    val positionClaims = claimsFor(objectIds, StrategicDeltaScope.PositionLocal, claims)

    assertEquals(row.expectation, "primary")
    assertEquals(admission(planned, positionClaims), row.plannerAdmission)
    assert(
      positionClaims.exists(claim =>
        claim.status == ClaimStatus.Certified &&
          claim.primaryTag.contains(StrategicDeltaTag.CoordinationImproved)
      ),
      clue("expected certified coordination claim")
    )
    assertThinShellMirror(planned, claims)
  }

  test("K09E coordination near-miss stays closed at shell") {
    val row =
      CurrentPositionCoordinationProbeTest.rows.find(_.id == "current-position-coordination-probe-near-miss").getOrElse(
        fail("expected current-position coordination near-miss row")
      )
    val (objects, _, claims, planned) = runNeutralPipeline(row.fen)
    val objectIds = objectIdsFor(row.family, row.owner, row.anchor, objects)
    val positionClaims = claimsFor(objectIds, StrategicDeltaScope.PositionLocal, claims)

    assertEquals(row.expectation, "none")
    assertEquals(admission(planned, positionClaims), row.plannerAdmission)
    assert(positionClaims.forall(claim => !planned.claimIds.contains(claim.id) && !planned.supportClaimIds.contains(claim.id)))
    assertThinShellMirror(planned, claims)
  }

  test("single-active-piece mirage nasty-negative stays closed at shell") {
    val row =
      CurrentPositionCoordinationProbeTest.rows.find(_.id == "current-position-coordination-probe-nasty-negative").getOrElse(
        fail("expected current-position coordination nasty-negative row")
      )
    val (objects, _, claims, planned) = runNeutralPipeline(row.fen)
    val objectIds = objectIdsFor(row.family, row.owner, row.anchor, objects)
    val positionClaims = claimsFor(objectIds, StrategicDeltaScope.PositionLocal, claims)

    assertEquals(row.caseType, "nasty_negative")
    assertEquals(row.expectation, "none")
    assertEquals(admission(planned, positionClaims), row.plannerAdmission)
    assert(positionClaims.forall(claim => !planned.claimIds.contains(claim.id) && !planned.supportClaimIds.contains(claim.id)))
    assertThinShellMirror(planned, claims)
  }

  private def runPipeline(
      fen: String,
      truth: lila.llm.analysis.MoveTruthFrame,
      contract: lila.llm.analysis.DecisiveTruthContract
  ): (List[StrategicObject], List[StrategicObjectDelta], List[CertifiedClaim], PlannedQuestion) =
    val objects = StrategicObjectSynthesizerTest.objectsForFen(fen, truth)
    val deltas = CanonicalStrategicObjectDeltaProjector.project(contract, truth, objects)
    val claims = CanonicalClaimCertification.certify(contract, objects, deltas)
    val planned = CanonicalQuestionPlanner.plan(contract, claims)
    (objects, deltas, claims, planned)

  private def runNeutralPipeline(
      fen: String
  ): (List[StrategicObject], List[StrategicObjectDelta], List[CertifiedClaim], PlannedQuestion) =
    runPipeline(fen, PrimitiveExtractionTest.neutralTruthFrame, PrimitiveExtractionTest.neutralContract)

  private def objectIdsFor(
      familyName: String,
      ownerName: String,
      anchorKey: Option[String],
      objects: List[StrategicObject]
  ): Set[String] =
    val family = StrategicObjectSynthesizerTest.parseFamily(familyName)
    val owner = StrategicObjectSynthesizerTest.parseColor(ownerName)
    val anchor = anchorKey.flatMap(StrategicObjectSynthesizerTest.parseSquare)

    objects
      .filter(obj =>
        obj.family == family &&
          obj.owner == owner &&
          anchor.forall(objectSquares(obj).contains)
      )
      .map(_.id)
      .toSet

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

  private def assertThinShellMirror(
      planned: PlannedQuestion,
      claims: List[CertifiedClaim]
  ): Unit =
    val rendered = CanonicalThinShellRenderer.render(planned, claims)
    assertEquals(rendered.claimIds, planned.claimIds, clue("thin shell must mirror planner primary ids"))
    assertEquals(rendered.supportClaimIds, planned.supportClaimIds, clue("thin shell must mirror planner support ids"))

  private def objectSquares(
      obj: StrategicObject
  ): List[Square] =
    (
      obj.locus.allSquares ++
        obj.anchors.flatMap(_.squares) ++
        obj.anchors.flatMap(_.route.toList.flatMap(_.allSquares))
    ).distinct.sortBy(_.key)
