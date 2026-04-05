package lila.llm.strategicobject

import chess.Color
import munit.FunSuite

class ClaimCertificationTest extends FunSuite:

  private val fileDuelFen = "2r3k1/8/8/8/8/8/8/2R3K1 w - - 0 1"
  private val passerRaceFen = "6k1/2P5/8/8/8/8/5p2/6K1 w - - 0 1"

  test("claim certification promotes stable objects and demotes provisional ones to support-only") {
    val objects = StrategicObjectSynthesizerTest.objectsForFen(fileDuelFen)
    val deltas = CanonicalStrategicObjectDeltaProjector.project(PrimitiveExtractionTest.neutralContract, objects)
    val claims = CanonicalClaimCertification.certify(PrimitiveExtractionTest.neutralContract, objects, deltas)
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
    assertEquals(shellClaim.readiness, StrategicObjectReadiness.Provisional)
    assertEquals(shellClaim.status, ClaimStatus.SupportOnly)
  }

  test("deferred families are certified only as deferred non-primary claims") {
    val objects = StrategicObjectSynthesizerTest.objectsForFen(passerRaceFen)
    val deltas = CanonicalStrategicObjectDeltaProjector.project(PrimitiveExtractionTest.neutralContract, objects)
    val claims = CanonicalClaimCertification.certify(PrimitiveExtractionTest.neutralContract, objects, deltas)
    val planRace = objects.find(obj => obj.family == StrategicObjectFamily.PlanRace && obj.owner == Color.White).getOrElse(
      fail("expected white plan race")
    )
    val deferredClaim = claims.find(_.objectId == planRace.id).getOrElse(fail("expected deferred plan-race claim"))

    assertEquals(deferredClaim.readiness, StrategicObjectReadiness.DeferredForDelta)
    assertEquals(deferredClaim.status, ClaimStatus.Deferred)
  }
