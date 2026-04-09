package lila.llm.strategicobject

import chess.Color
import munit.FunSuite
import lila.llm.analysis.TruthVisibilityRole

class QuestionPlannerTest extends FunSuite:

  private val fileDuelFen = "2r3k1/8/8/8/8/8/8/2R3K1 w - - 0 1"
  private val k10Fen = "r2qk2r/1b1nbppp/pp1ppn2/8/2PQ4/BPN2NP1/P3PPBP/R2R2K1 w kq - 2 11"
  private val passerRaceFen = "6k1/2P5/8/8/8/8/5p2/6K1 w - - 0 1"
  private val visibleComparativeTruth =
    PrimitiveExtractionTest.neutralTruthFrame.copy(visibilityRole = TruthVisibilityRole.PrimaryVisible)
  private val visibleComparativeContract =
    PrimitiveExtractionTest.neutralContract.copy(visibilityRole = TruthVisibilityRole.PrimaryVisible)

  test("planner only admits typed stable claims as primary and keeps provisional typed claims as support") {
    val truth = PrimitiveExtractionTest.moveTransitionVisibleTruthFrameFor("c1c8")
    val contract = PrimitiveExtractionTest.moveTransitionVisibleContractFor("c1c8")
    val objects = StrategicObjectSynthesizerTest.objectsForFen(fileDuelFen, truth)
    val deltas = CanonicalStrategicObjectDeltaProjector.project(contract, truth, objects)
    val claims = CanonicalClaimCertification.certify(contract, objects, deltas)
    val planned = CanonicalQuestionPlanner.plan(contract, claims)
    val accessPrimary =
      claims.find(claim =>
        claim.status == ClaimStatus.Certified &&
          claim.deltaScope == StrategicDeltaScope.MoveLocal &&
          objects.exists(obj => obj.id == claim.objectId && obj.family == StrategicObjectFamily.AccessNetwork && obj.owner == Color.White)
      ).getOrElse(fail("expected certified access claim"))
    val shellSupport =
      claims.find(claim =>
        claim.status == ClaimStatus.SupportOnly &&
          objects.exists(obj => obj.id == claim.objectId && obj.family == StrategicObjectFamily.KingSafetyShell && obj.owner == Color.White)
      ).getOrElse(fail("expected support-only shell claim"))

    assertEquals(planned.axis, QuestionAxis.WhyThis, clue("non-bad move-local certified delta should open WhyThis"))
    assert(planned.claimIds.contains(accessPrimary.id), clue("stable typed claim must stay primary-eligible"))
    assert(!planned.supportClaimIds.contains(shellSupport.id), clue("non-matching position-local support must not attach under WhyThis"))
    assert(planned.supportClaimIds.forall(id =>
      claims.find(_.id == id).exists(_.deltaScope == StrategicDeltaScope.MoveLocal)
    ), clue("WhyThis support must stay move-local"))
    assert(accessPrimary.delta.exists(_.moveTransition.exists(_.isTransitionAware)), clue("primary claim must preserve move transition witness"))
    assert(shellSupport.delta.exists(_.scope == StrategicDeltaScope.PositionLocal), clue("support claim must preserve typed delta"))
  }

  test("planner opens WhyNow from certified timing-sensitive move-local delta") {
    val row = deltaRow("passer-complex-move-exact")
    val truth = truthFor(row)
    val contract = contractFor(row)
    val objects = StrategicObjectSynthesizerTest.objectsForFen(row.fen, truth)
    val deltas = CanonicalStrategicObjectDeltaProjector.project(contract, truth, objects)
    val claims = CanonicalClaimCertification.certify(contract, objects, deltas)
    val timingPrimary =
      claims.find(claim =>
        claim.status == ClaimStatus.Certified &&
          claim.deltaScope == StrategicDeltaScope.MoveLocal &&
          claim.primaryTag.exists(
            Set(
              StrategicDeltaTag.BreakAccelerated,
              StrategicDeltaTag.BreakDelayed,
              StrategicDeltaTag.RouteShortened,
              StrategicDeltaTag.PasserAccelerated
            ).contains
          )
      ).getOrElse(fail("expected timing-sensitive certified move-local claim"))
    val planned = CanonicalQuestionPlanner.plan(contract, claims)

    assertEquals(planned.axis, QuestionAxis.WhyNow)
    assert(planned.claimIds.contains(timingPrimary.id), clue("timing-sensitive claim should own WhyNow"))
  }

  test("planner opens WhyNow from certified release-sensitive move-local delta") {
    val row = deltaRow("break-axis-move-exact")
    val truth = truthFor(row)
    val contract = contractFor(row)
    val objects = StrategicObjectSynthesizerTest.objectsForFen(row.fen, truth)
    val deltas = CanonicalStrategicObjectDeltaProjector.project(contract, truth, objects)
    val claims = CanonicalClaimCertification.certify(contract, objects, deltas)
    val releasePrimary =
      claims.find(claim =>
        claim.status == ClaimStatus.Certified &&
          claim.deltaScope == StrategicDeltaScope.MoveLocal &&
          claim.delta.exists(_.moveTransition.exists(_.primitiveKinds.contains(PrimitiveKind.ReleaseCandidate)))
      ).getOrElse(fail("expected release-sensitive certified move-local claim"))
    val planned = CanonicalQuestionPlanner.plan(contract, claims)

    assertEquals(planned.axis, QuestionAxis.WhyNow)
    assert(planned.claimIds.contains(releasePrimary.id), clue("release-sensitive claim should own WhyNow"))
  }

  test("planner does not open WhyNow from support-only timing-sensitive delta alone") {
    val row = deltaRow("passer-complex-move-exact")
    val truth = truthFor(row)
    val contract = contractFor(row)
    val objects = StrategicObjectSynthesizerTest.objectsForFen(row.fen, truth)
    val deltas = CanonicalStrategicObjectDeltaProjector.project(contract, truth, objects)
    val claims = CanonicalClaimCertification.certify(contract, objects, deltas)
    val timingPrimary =
      claims.find(claim =>
        claim.status == ClaimStatus.Certified &&
          claim.deltaScope == StrategicDeltaScope.MoveLocal &&
          claim.primaryTag.exists(
            Set(
              StrategicDeltaTag.BreakAccelerated,
              StrategicDeltaTag.BreakDelayed,
              StrategicDeltaTag.RouteShortened,
              StrategicDeltaTag.PasserAccelerated
            ).contains
          )
      ).getOrElse(fail("expected timing-sensitive certified move-local claim"))
    val supportOnlyClaims = List(timingPrimary.copy(status = ClaimStatus.SupportOnly))
    val planned = CanonicalQuestionPlanner.plan(contract, supportOnlyClaims)

    assertEquals(planned.axis, QuestionAxis.WhatMattersHere)
    assert(planned.claimIds.isEmpty, clue("support-only timing claim must not become primary"))
  }

  test("planner keeps WhatMustBeStopped ahead of WhyNow on bad contracts") {
    val row = deltaRow("passer-complex-move-exact")
    val truth = truthFor(row)
    val goodContract = contractFor(row)
    val badContract = goodContract.copy(truthClass = lila.llm.analysis.DecisiveTruthClass.Mistake)
    val objects = StrategicObjectSynthesizerTest.objectsForFen(row.fen, truth)
    val deltas = CanonicalStrategicObjectDeltaProjector.project(goodContract, truth, objects)
    val claims = CanonicalClaimCertification.certify(goodContract, objects, deltas)
    val planned = CanonicalQuestionPlanner.plan(badContract, claims)

    assertEquals(planned.axis, QuestionAxis.WhatMustBeStopped)
    assert(planned.claimIds.nonEmpty, clue("bad contract should still admit a primary move-local lane"))
  }

  test("planner chooses WhatChanged only from certified comparative delta once move-local typed delta is removed") {
    val truth = PrimitiveExtractionTest.moveTransitionVisibleTruthFrameFor("c1c8")
    val contract = PrimitiveExtractionTest.moveTransitionVisibleContractFor("c1c8")
    val objects = StrategicObjectSynthesizerTest.objectsForFen(fileDuelFen, truth)
    val deltas = CanonicalStrategicObjectDeltaProjector.project(contract, truth, objects)
    val claims = CanonicalClaimCertification.certify(contract, objects, deltas)
    val scopeOnlyClaims =
      claims.map(claim =>
        if claim.deltaScope == StrategicDeltaScope.MoveLocal then claim.copy(delta = None)
        else claim
      )
    val planned = CanonicalQuestionPlanner.plan(contract, scopeOnlyClaims)

    assertEquals(planned.axis, QuestionAxis.WhatChanged, clue("planner must read typed delta, not scope shell"))
    assert(planned.claimIds.forall(id => !scopeOnlyClaims.find(_.id == id).exists(_.deltaScope == StrategicDeltaScope.MoveLocal)))
  }

  test("planner chooses WhatMattersHere from the packet-owned current-position fixed-target probe") {
    val row =
      CurrentPositionFixedTargetProbeTest.rows.find(_.caseType == "exact").getOrElse(
        fail("expected current-position fixed-target exact row")
      )
    val truth = PrimitiveExtractionTest.neutralTruthFrame
    val contract = PrimitiveExtractionTest.neutralContract
    val objects = StrategicObjectSynthesizerTest.objectsForFen(row.fen, truth)
    val deltas = CanonicalStrategicObjectDeltaProjector.project(contract, truth, objects)
    val claims = CanonicalClaimCertification.certify(contract, objects, deltas)
    val planned = CanonicalQuestionPlanner.plan(contract, claims)
    val objectIds = currentPositionTargetObjectIds(row, objects)
    val positionClaims =
      claims.filter(claim =>
        objectIds.contains(claim.objectId) &&
          claim.deltaScope == StrategicDeltaScope.PositionLocal
      )
    val primaryClaim =
      positionClaims.find(_.status == ClaimStatus.Certified).getOrElse(
        fail("expected certified current-position fixed-target claim")
      )

    assertEquals(planned.axis, QuestionAxis.WhatMattersHere)
    assert(positionClaims.nonEmpty, clue("expected at least one certified position-local claim"))
    assert(planned.claimIds.contains(primaryClaim.id), clue("expected packet-owned primary admission"))
    assert(primaryClaim.primaryTag.contains(StrategicDeltaTag.TargetFixed), clue("expected exact fixation"))
  }

  test("planner chooses WhatMattersHere from the packet-owned current-position coordination probe") {
    val row =
      CurrentPositionCoordinationProbeTest.rows.find(_.caseType == "exact").getOrElse(
        fail("expected current-position coordination exact row")
      )
    val truth = PrimitiveExtractionTest.neutralTruthFrame
    val contract = PrimitiveExtractionTest.neutralContract
    val objects = StrategicObjectSynthesizerTest.objectsForFen(row.fen, truth)
    val deltas = CanonicalStrategicObjectDeltaProjector.project(contract, truth, objects)
    val claims = CanonicalClaimCertification.certify(contract, objects, deltas)
    val planned = CanonicalQuestionPlanner.plan(contract, claims)
    val objectIds = currentPositionCoordinationObjectIds(row, objects)
    val positionClaims =
      claims.filter(claim =>
        objectIds.contains(claim.objectId) &&
          claim.deltaScope == StrategicDeltaScope.PositionLocal
      )
    val primaryClaim =
      positionClaims.find(claim =>
        claim.status == ClaimStatus.Certified &&
          claim.primaryTag.contains(StrategicDeltaTag.CoordinationImproved)
      ).getOrElse(
        fail("expected certified current-position coordination claim")
      )

    assertEquals(planned.axis, QuestionAxis.WhatMattersHere)
    assert(positionClaims.nonEmpty, clue("expected at least one certified coordination position-local claim"))
    assert(planned.claimIds.contains(primaryClaim.id), clue("expected packet-owned coordination primary admission"))
  }

  test("whatmattershere support remains slice-bounded between coordination and fixed-target probes") {
    val truth = PrimitiveExtractionTest.neutralTruthFrame
    val contract = PrimitiveExtractionTest.neutralContract
    val coordinationRow =
      CurrentPositionCoordinationProbeTest.rows.find(_.caseType == "exact").getOrElse(
        fail("expected current-position coordination exact row")
      )
    val fixedTargetRow =
      CurrentPositionFixedTargetProbeTest.rows.find(_.caseType == "exact").getOrElse(
        fail("expected current-position fixed-target exact row")
      )

    val coordinationObjects = StrategicObjectSynthesizerTest.objectsForFen(coordinationRow.fen, truth)
    val coordinationDeltas = CanonicalStrategicObjectDeltaProjector.project(contract, truth, coordinationObjects)
    val coordinationClaims = CanonicalClaimCertification.certify(contract, coordinationObjects, coordinationDeltas)
    val coordinationIds = currentPositionCoordinationObjectIds(coordinationRow, coordinationObjects)
    val coordinationPrimary =
      coordinationClaims.find(claim =>
        coordinationIds.contains(claim.objectId) &&
          claim.deltaScope == StrategicDeltaScope.PositionLocal &&
          claim.status == ClaimStatus.Certified &&
          claim.primaryTag.contains(StrategicDeltaTag.CoordinationImproved)
      ).getOrElse(
        fail("expected certified coordination primary claim")
      )

    val fixedTargetObjects = StrategicObjectSynthesizerTest.objectsForFen(fixedTargetRow.fen, truth)
    val fixedTargetDeltas = CanonicalStrategicObjectDeltaProjector.project(contract, truth, fixedTargetObjects)
    val fixedTargetClaims = CanonicalClaimCertification.certify(contract, fixedTargetObjects, fixedTargetDeltas)
    val fixedTargetIds = currentPositionTargetObjectIds(fixedTargetRow, fixedTargetObjects)
    val fixedTargetSupport =
      fixedTargetClaims.find(claim =>
        fixedTargetIds.contains(claim.objectId) &&
          claim.deltaScope == StrategicDeltaScope.PositionLocal &&
          claim.status == ClaimStatus.Certified &&
          claim.primaryTag.contains(StrategicDeltaTag.TargetFixed)
      ).map(claim =>
        claim.copy(
          id = s"${claim.id}:support-slice-check",
          status = ClaimStatus.SupportOnly
        )
      ).getOrElse(
        fail("expected fixed-target support candidate")
      )
    val coordinationSupport =
      coordinationPrimary.copy(
        id = s"${coordinationPrimary.id}:support-slice-check",
        status = ClaimStatus.SupportOnly
      )

    val planned = CanonicalQuestionPlanner.plan(
      contract,
      List(coordinationPrimary, coordinationSupport, fixedTargetSupport)
    )

    assertEquals(planned.axis, QuestionAxis.WhatMattersHere)
    assertEquals(planned.claimIds, List(coordinationPrimary.id))
    assertEquals(planned.supportClaimIds, List(coordinationSupport.id))
    assert(!planned.supportClaimIds.contains(fixedTargetSupport.id), clue("cross-slice fixed-target support must remain closed under coordination primary"))
  }

  test("planner keeps WhatChanged ahead of the packet-owned current-position probe on mixed boards") {
    val row =
      CurrentPositionFixedTargetProbeTest.rows.find(_.caseType == "exact").getOrElse(
        fail("expected current-position fixed-target exact row")
      )
    val truth = visibleComparativeTruth
    val contract = visibleComparativeContract
    val objects = StrategicObjectSynthesizerTest.objectsForFen(row.fen, truth)
    val deltas = CanonicalStrategicObjectDeltaProjector.project(contract, truth, objects)
    val claims = CanonicalClaimCertification.certify(contract, objects, deltas)
    val planned = CanonicalQuestionPlanner.plan(contract, claims)
    val objectIds = currentPositionTargetObjectIds(row, objects)
    val positionClaims =
      claims.filter(claim =>
        objectIds.contains(claim.objectId) &&
          claim.deltaScope == StrategicDeltaScope.PositionLocal
      )
    val comparativePrimary =
      claims.find(claim =>
        claim.status == ClaimStatus.Certified &&
          claim.deltaScope == StrategicDeltaScope.Comparative &&
          claim.delta.exists(_.family == StrategicObjectFamily.FixedTargetComplex) &&
          objects.exists(obj =>
            obj.id == claim.objectId &&
              obj.family == StrategicObjectFamily.FixedTargetComplex &&
              obj.owner == Color.White
          )
      ).getOrElse(fail("expected certified comparative fixed-target claim"))

    assert(positionClaims.nonEmpty, clue("expected mixed-board current-position fixed-target claim"))
    assertEquals(planned.axis, QuestionAxis.WhatChanged)
    assert(planned.claimIds.contains(comparativePrimary.id), clue("comparative lane must keep precedence"))
    assert(!planned.claimIds.exists(positionClaims.map(_.id).toSet.contains), clue("packet-owned probe must not suppress WhatChanged"))
  }

  test("planner chooses WhatMustBeStopped only from certified move-local delta on bad contracts") {
    val truth = PrimitiveExtractionTest.moveTransitionVisibleTruthFrameFor("c1c8")
    val goodContract = PrimitiveExtractionTest.moveTransitionVisibleContractFor("c1c8")
    val badContract = goodContract.copy(truthClass = lila.llm.analysis.DecisiveTruthClass.Mistake)
    val objects = StrategicObjectSynthesizerTest.objectsForFen(fileDuelFen, truth)
    val deltas = CanonicalStrategicObjectDeltaProjector.project(goodContract, truth, objects)
    val claims = CanonicalClaimCertification.certify(goodContract, objects, deltas)
    val planned = CanonicalQuestionPlanner.plan(badContract, claims)

    assertEquals(planned.axis, QuestionAxis.WhatMustBeStopped)
    assert(planned.claimIds.nonEmpty, clue("expected certified move-local primary on bad contract"))
    assert(planned.claimIds.forall(id =>
      claims.find(_.id == id).exists(_.deltaScope == StrategicDeltaScope.MoveLocal)
    ))
  }

  test("whatchanged admits only the packet-owned shared-target comparative support slice") {
    val truth = visibleComparativeTruth
    val contract = visibleComparativeContract
    val objects = StrategicObjectSynthesizerTest.objectsForFen(k10Fen, truth)
    val deltas = CanonicalStrategicObjectDeltaProjector.project(contract, truth, objects)
    val claims = CanonicalClaimCertification.certify(contract, objects, deltas)
    val fixedTargetPrimary =
      claims.find(claim =>
        claim.status == ClaimStatus.Certified &&
          claim.deltaScope == StrategicDeltaScope.Comparative &&
          objects.exists(obj =>
            obj.id == claim.objectId &&
              obj.family == StrategicObjectFamily.FixedTargetComplex &&
              obj.owner == Color.White
          )
      ).getOrElse(fail("expected certified fixed-target comparative primary"))
    val restrictionSupport =
      claims.find(claim =>
        claim.status == ClaimStatus.SupportOnly &&
          claim.deltaScope == StrategicDeltaScope.Comparative &&
          objects.exists(obj =>
            obj.id == claim.objectId &&
              obj.family == StrategicObjectFamily.RestrictionShell &&
              obj.owner == Color.White
          )
      ).getOrElse(fail("expected restriction-shell comparative support"))
    val spaceClampSupport =
      claims.find(claim =>
        claim.status == ClaimStatus.SupportOnly &&
          claim.deltaScope == StrategicDeltaScope.Comparative &&
          objects.exists(obj =>
            obj.id == claim.objectId &&
              obj.family == StrategicObjectFamily.SpaceClamp &&
              obj.owner == Color.White
          )
      ).getOrElse(fail("expected space-clamp comparative support candidate"))
    val planned = CanonicalQuestionPlanner.plan(contract, claims)

    assertEquals(planned.axis, QuestionAxis.WhatChanged)
    assert(planned.claimIds.contains(fixedTargetPrimary.id), clue("fixed-target comparative should stay primary"))
    assertEquals(planned.supportClaimIds, List(restrictionSupport.id))
    assert(!planned.supportClaimIds.contains(spaceClampSupport.id), clue("non-packet comparative support must stay closed"))
  }

  test("shallow comparative rows stay planner-none even when whatchanged has certified primary claims") {
    val row = deltaRow("development-comparative-near-miss")
    val truth = truthFor(row)
    val contract = contractFor(row)
    val objects = StrategicObjectSynthesizerTest.objectsForFen(row.fen, truth)
    val deltas = CanonicalStrategicObjectDeltaProjector.project(contract, truth, objects)
    val claims = CanonicalClaimCertification.certify(contract, objects, deltas)
    val shallowComparative =
      claims.find(claim =>
        claim.status == ClaimStatus.SupportOnly &&
          claim.deltaScope == StrategicDeltaScope.Comparative &&
          claim.objectId == "DevelopmentCoordinationState-white-mixed-c1"
      ).getOrElse(fail("expected support-only shallow comparative claim"))
    val planned = CanonicalQuestionPlanner.plan(contract, claims)

    assertEquals(planned.axis, QuestionAxis.WhatChanged)
    assert(planned.claimIds.nonEmpty, clue("expected certified comparative primary claims on the board"))
    assert(!planned.supportClaimIds.contains(shallowComparative.id), clue("shallow comparative should stay planner-none"))
    assert(planned.supportClaimIds.isEmpty, clue("generic shallow comparative support must stay closed"))
  }

  test("Tier-1 provisional comparative near-miss rows stay planner-none family by family") {
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
      val planned = CanonicalQuestionPlanner.plan(contract, claims)
      val comparativeIds =
        claims.collect {
          case claim
              if claim.deltaScope == StrategicDeltaScope.Comparative &&
                claim.status == ClaimStatus.SupportOnly &&
                objects.exists(obj =>
                  obj.id == claim.objectId &&
                    obj.family == StrategicObjectDeltaProjectorTest.parseFamily(row.family) &&
                    obj.owner == StrategicObjectDeltaProjectorTest.parseColor(row.owner)
                ) =>
            claim.id
        }.toSet

      assert(comparativeIds.nonEmpty, clue(s"${row.id}: expected support-only shallow comparative claim"))
      assertEquals(
        planned.supportClaimIds.filter(comparativeIds.contains),
        Nil,
        clue(s"${row.id}: provisional shallow comparative should stay planner-none, planned=$planned")
      )
    }
  }

  test("planner leaves deferred families out of primary and support admission") {
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
    val planned = CanonicalQuestionPlanner.plan(contract, claims)
    val deferredIds =
      claims.filter(_.readiness == StrategicObjectReadiness.DeferredForDelta).map(_.id).toSet

    assert(deferredIds.nonEmpty, clue("expected deferred claims"))
    assert(planned.claimIds.forall(id => !deferredIds.contains(id)), clue("deferred claims must not become primary"))
    assert(planned.supportClaimIds.forall(id => !deferredIds.contains(id)), clue("deferred claims must not become support"))
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

  private def currentPositionTargetObjectIds(
      row: CurrentPositionFixedTargetProbeTest.CurrentPositionFixedTargetRow,
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

  private def objectSquares(
      obj: StrategicObject
  ): List[chess.Square] =
    (
      obj.locus.allSquares ++
        obj.anchors.flatMap(_.squares) ++
        obj.anchors.flatMap(_.route.toList.flatMap(_.allSquares))
    ).distinct.sortBy(_.key)

  private def currentPositionCoordinationObjectIds(
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
          anchor.forall(objectSquares(obj).contains)
      )
      .map(_.id)
      .toSet
