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

    assert(planned.claimIds.contains(accessPrimary.id), clue("stable typed claim must stay primary-eligible"))
    assert(planned.supportClaimIds.contains(shellSupport.id), clue("provisional typed claim must stay support-only"))
    assert(accessPrimary.delta.exists(_.moveTransition.exists(_.isTransitionAware)), clue("primary claim must preserve move transition witness"))
    assert(shellSupport.delta.exists(_.scope == StrategicDeltaScope.PositionLocal), clue("support claim must preserve typed delta"))
  }

  test("planner does not choose move-local axis from scope-only shells once typed delta is removed") {
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
