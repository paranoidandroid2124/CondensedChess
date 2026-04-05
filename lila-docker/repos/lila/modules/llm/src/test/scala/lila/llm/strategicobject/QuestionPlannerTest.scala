package lila.llm.strategicobject

import chess.Color
import munit.FunSuite

class QuestionPlannerTest extends FunSuite:

  private val fileDuelFen = "2r3k1/8/8/8/8/8/8/2R3K1 w - - 0 1"
  private val passerRaceFen = "6k1/2P5/8/8/8/8/5p2/6K1 w - - 0 1"

  test("planner only admits stable claims as primary and keeps provisional claims as support") {
    val objects = StrategicObjectSynthesizerTest.objectsForFen(fileDuelFen)
    val deltas = CanonicalStrategicObjectDeltaProjector.project(PrimitiveExtractionTest.neutralContract, objects)
    val claims = CanonicalClaimCertification.certify(PrimitiveExtractionTest.neutralContract, objects, deltas)
    val planned = CanonicalQuestionPlanner.plan(PrimitiveExtractionTest.neutralContract, claims)
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

    assert(planned.claimIds.contains(accessPrimary.id), clue("stable claim must stay primary-eligible"))
    assert(planned.supportClaimIds.contains(shellSupport.id), clue("provisional claim must stay support-only"))
  }

  test("planner leaves deferred families out of primary and support admission") {
    val objects = StrategicObjectSynthesizerTest.objectsForFen(passerRaceFen)
    val deltas = CanonicalStrategicObjectDeltaProjector.project(PrimitiveExtractionTest.neutralContract, objects)
    val claims = CanonicalClaimCertification.certify(PrimitiveExtractionTest.neutralContract, objects, deltas)
    val planned = CanonicalQuestionPlanner.plan(PrimitiveExtractionTest.neutralContract, claims)
    val deferredIds =
      claims.filter(_.readiness == StrategicObjectReadiness.DeferredForDelta).map(_.id).toSet

    assert(deferredIds.nonEmpty, clue("expected deferred claims"))
    assert(planned.claimIds.forall(id => !deferredIds.contains(id)), clue("deferred claims must not become primary"))
    assert(planned.supportClaimIds.forall(id => !deferredIds.contains(id)), clue("deferred claims must not become support"))
  }
