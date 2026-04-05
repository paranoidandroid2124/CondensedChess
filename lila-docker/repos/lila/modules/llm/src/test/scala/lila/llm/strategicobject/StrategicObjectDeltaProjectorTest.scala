package lila.llm.strategicobject

import chess.Color
import lila.llm.analysis.TruthVisibilityRole
import munit.FunSuite

class StrategicObjectDeltaProjectorTest extends FunSuite:

  private val fileDuelFen = "2r3k1/8/8/8/8/8/8/2R3K1 w - - 0 1"
  private val passerRaceFen = "6k1/2P5/8/8/8/8/5p2/6K1 w - - 0 1"

  test("stable families emit move-local, position-local, and comparative deltas") {
    val objects = StrategicObjectSynthesizerTest.objectsForFen(fileDuelFen)
    val deltas = CanonicalStrategicObjectDeltaProjector.project(PrimitiveExtractionTest.neutralContract, objects)
    val access =
      objects.find(obj => obj.family == StrategicObjectFamily.AccessNetwork && obj.owner == Color.White).getOrElse(
        fail("expected white access network")
      )

    assertEquals(access.readiness, StrategicObjectReadiness.Stable)
    assertEquals(
      deltas.filter(_.objectId == access.id).map(_.scope).toSet,
      Set(StrategicDeltaScope.MoveLocal, StrategicDeltaScope.PositionLocal, StrategicDeltaScope.Comparative)
    )
  }

  test("provisional families are position-local first and only add comparative under visible truth") {
    val objects = StrategicObjectSynthesizerTest.objectsForFen(fileDuelFen)
    val shell =
      objects.find(obj => obj.family == StrategicObjectFamily.KingSafetyShell && obj.owner == Color.White).getOrElse(
        fail("expected white king-safety shell")
      )
    val neutralScopes =
      CanonicalStrategicObjectDeltaProjector
        .project(PrimitiveExtractionTest.neutralContract, objects)
        .filter(_.objectId == shell.id)
        .map(_.scope)
        .toSet
    val visibleContract =
      PrimitiveExtractionTest.neutralContract.copy(visibilityRole = TruthVisibilityRole.PrimaryVisible)
    val visibleScopes =
      CanonicalStrategicObjectDeltaProjector
        .project(visibleContract, objects)
        .filter(_.objectId == shell.id)
        .map(_.scope)
        .toSet

    assertEquals(shell.readiness, StrategicObjectReadiness.Provisional)
    assertEquals(neutralScopes, Set(StrategicDeltaScope.PositionLocal))
    assertEquals(visibleScopes, Set(StrategicDeltaScope.PositionLocal, StrategicDeltaScope.Comparative))
  }

  test("deferred families stay in the object graph but emit no deltas") {
    val objects = StrategicObjectSynthesizerTest.objectsForFen(passerRaceFen)
    val deltas = CanonicalStrategicObjectDeltaProjector.project(PrimitiveExtractionTest.neutralContract, objects)
    val planRace =
      objects.find(obj => obj.family == StrategicObjectFamily.PlanRace && obj.owner == Color.White).getOrElse(
        fail("expected white plan race")
      )

    assertEquals(planRace.readiness, StrategicObjectReadiness.DeferredForDelta)
    assert(!deltas.exists(_.objectId == planRace.id), clue("deferred family must not project deltas"))
  }
