package lila.llm.strategicobject

import chess.Color
import munit.FunSuite

class QuestionPlannerTest extends FunSuite:

  private val fileDuelFen = "2r3k1/8/8/8/8/8/8/2R3K1 w - - 0 1"
  private val passerRaceFen = "6k1/2P5/8/8/8/8/5p2/6K1 w - - 0 1"

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

  test("planner chooses WhatMattersHere from certified position-local delta when change lanes are absent") {
    val truth = PrimitiveExtractionTest.moveTransitionVisibleTruthFrameFor("c1c8")
    val contract = PrimitiveExtractionTest.moveTransitionVisibleContractFor("c1c8")
    val objects = StrategicObjectSynthesizerTest.objectsForFen(fileDuelFen, truth)
    val deltas = CanonicalStrategicObjectDeltaProjector.project(contract, truth, objects)
    val claims = CanonicalClaimCertification.certify(contract, objects, deltas)
    val positionOnlyClaims =
      claims.map { claim =>
        claim.delta match
          case Some(delta) if delta.scope != StrategicDeltaScope.PositionLocal =>
            claim.copy(delta = None)
          case _ =>
            claim
      }
    val planned = CanonicalQuestionPlanner.plan(contract, positionOnlyClaims)

    assertEquals(planned.axis, QuestionAxis.WhatMattersHere)
    assert(planned.claimIds.nonEmpty, clue("expected at least one certified position-local claim"))
    assert(planned.claimIds.forall(id =>
      positionOnlyClaims.find(_.id == id).exists(_.deltaScope == StrategicDeltaScope.PositionLocal)
    ))
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

  test("support-only claims attach only when their typed delta matches the admitted axis") {
    val truth = PrimitiveExtractionTest.moveTransitionVisibleTruthFrameFor("c1c8")
    val contract = PrimitiveExtractionTest.moveTransitionVisibleContractFor("c1c8")
    val objects = StrategicObjectSynthesizerTest.objectsForFen(fileDuelFen, truth)
    val deltas = CanonicalStrategicObjectDeltaProjector.project(contract, truth, objects)
    val claims = CanonicalClaimCertification.certify(contract, objects, deltas)
    val shellSupport =
      claims.find(claim =>
        claim.status == ClaimStatus.SupportOnly &&
          claim.deltaScope == StrategicDeltaScope.PositionLocal
      ).getOrElse(fail("expected position-local support claim"))
    val comparativePrimary =
      claims.find(claim =>
        claim.status == ClaimStatus.Certified &&
          claim.deltaScope == StrategicDeltaScope.Comparative
      ).getOrElse(fail("expected certified comparative claim"))
    val comparativeSupportId = comparativePrimary.id
    val comparativeLaneClaims =
      claims.map { claim =>
        claim.delta match
          case Some(delta) if delta.scope == StrategicDeltaScope.MoveLocal =>
            claim.copy(delta = None)
          case _ if claim.id == comparativeSupportId =>
            claim.copy(status = ClaimStatus.SupportOnly)
          case _ =>
            claim
      }
    val planned = CanonicalQuestionPlanner.plan(contract, comparativeLaneClaims)

    assertEquals(planned.axis, QuestionAxis.WhatChanged)
    assert(planned.claimIds.forall(id =>
      comparativeLaneClaims.find(_.id == id).exists(_.deltaScope == StrategicDeltaScope.Comparative)
    ))
    assert(planned.supportClaimIds.contains(comparativeSupportId), clue("matching comparative support should attach"))
    assert(!planned.supportClaimIds.contains(shellSupport.id), clue("non-matching position-local support must stay out of WhatChanged"))
    assert(planned.supportClaimIds.forall(id =>
      comparativeLaneClaims.find(_.id == id).exists(_.deltaScope == StrategicDeltaScope.Comparative)
    ), clue("support must stay on the admitted axis"))
  }

  test("support-only shallow comparative claims do not open a primary question axis by themselves") {
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
    val planned = CanonicalQuestionPlanner.plan(contract, List(shallowComparative))

    assertEquals(planned.axis, QuestionAxis.WhatMattersHere)
    assert(planned.claimIds.isEmpty, clue("support-only shallow comparative should not own a primary axis"))
    assert(planned.supportClaimIds.isEmpty, clue("support-only shallow comparative should not attach without a primary axis"))
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
