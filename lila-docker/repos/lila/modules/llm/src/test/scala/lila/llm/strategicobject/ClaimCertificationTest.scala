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
