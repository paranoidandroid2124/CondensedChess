package lila.llm.analysis

import munit.FunSuite
import play.api.libs.json.Json
import _root_.chess.Color
import lila.llm.model.{ Plan, PlanMatch, PlanSequenceSummary, TransitionType }
import lila.llm.model.strategic.PlanContinuity

class PlanStateTrackerTest extends FunSuite:

  private def pm(plan: Plan, score: Double = 0.8): PlanMatch =
    PlanMatch(plan, score, Nil)

  test("PlanStateTracker should initialize with empty primary/secondary state") {
    val tracker = PlanStateTracker.empty
    assertEquals(tracker.getContinuity(Color.White), None)
    assertEquals(tracker.getSecondaryContinuity(Color.White), None)
    assertEquals(tracker.getContinuity(Color.Black), None)
    assertEquals(tracker.getSecondaryContinuity(Color.Black), None)
  }

  test("PlanStateTracker should record primary and secondary plans with planId") {
    val tracker = PlanStateTracker.empty
    val next = tracker.update(
      movingColor = Color.White,
      ply = 1,
      primaryPlan = Some(pm(Plan.KingsideAttack(Color.White))),
      secondaryPlan = Some(pm(Plan.CentralControl(Color.White), 0.6)),
      sequence = Some(PlanSequenceSummary(TransitionType.Opening, 0.5))
    )

    val primary = next.getContinuity(Color.White)
    val secondary = next.getSecondaryContinuity(Color.White)
    assertEquals(primary.map(_.planName), Some("Kingside Attack"))
    assertEquals(primary.flatMap(_.planId), Some("KingsideAttack"))
    assertEquals(primary.map(_.consecutivePlies), Some(1))
    assertEquals(secondary.map(_.planName), Some("Central Control"))
    assertEquals(secondary.flatMap(_.planId), Some("CentralControl"))
    assertEquals(next.getColorState(Color.White).lastPly, Some(1))
  }

  test("PlanStateTracker should increment continuity when same primary returns on next ply") {
    val t1 = PlanStateTracker.empty.update(
      movingColor = Color.White,
      ply = 1,
      primaryPlan = Some(pm(Plan.KingsideAttack(Color.White))),
      secondaryPlan = Some(pm(Plan.CentralControl(Color.White), 0.6))
    )
    val t2 = t1.update(
      movingColor = Color.White,
      ply = 3,
      primaryPlan = Some(pm(Plan.KingsideAttack(Color.White), 0.9)),
      secondaryPlan = Some(pm(Plan.CentralControl(Color.White), 0.5))
    )

    val primary = t2.getContinuity(Color.White).getOrElse(fail("missing primary continuity"))
    assertEquals(primary.planName, "Kingside Attack")
    assertEquals(primary.consecutivePlies, 2)
    assertEquals(primary.startingPly, 1)
  }

  test("PlanStateTracker should be idempotent on same-ply refined updates") {
    val t1 = PlanStateTracker.empty.update(
      movingColor = Color.White,
      ply = 9,
      primaryPlan = Some(pm(Plan.CentralControl(Color.White))),
      secondaryPlan = Some(pm(Plan.PieceActivation(Color.White), 0.6))
    )
    val t2 = t1.update(
      movingColor = Color.White,
      ply = 9,
      primaryPlan = Some(pm(Plan.CentralControl(Color.White), 0.95)),
      secondaryPlan = Some(pm(Plan.PieceActivation(Color.White), 0.65))
    )

    val primary = t2.getContinuity(Color.White).getOrElse(fail("missing primary"))
    val secondary = t2.getSecondaryContinuity(Color.White).getOrElse(fail("missing secondary"))
    assertEquals(primary.consecutivePlies, 1)
    assertEquals(secondary.consecutivePlies, 1)
  }

  test("PlanStateTracker should carry continuity when previous primary is demoted to secondary") {
    val t1 = PlanStateTracker.empty.update(
      movingColor = Color.White,
      ply = 5,
      primaryPlan = Some(pm(Plan.KingsideAttack(Color.White))),
      secondaryPlan = Some(pm(Plan.CentralControl(Color.White), 0.6))
    )
    val t2 = t1.update(
      movingColor = Color.White,
      ply = 7,
      primaryPlan = Some(pm(Plan.CentralControl(Color.White), 0.9)),
      secondaryPlan = Some(pm(Plan.KingsideAttack(Color.White), 0.7))
    )

    val secondary = t2.getSecondaryContinuity(Color.White).getOrElse(fail("missing secondary"))
    assertEquals(secondary.planName, "Kingside Attack")
    assertEquals(secondary.consecutivePlies, 2)
    assertEquals(secondary.startingPly, 5)
  }

  test("PlanStateTracker v2 codec round-trips both colors") {
    val tracker =
      PlanStateTracker.empty
        .update(
          movingColor = Color.White,
          ply = 3,
          primaryPlan = Some(pm(Plan.KingsideAttack(Color.White))),
          secondaryPlan = Some(pm(Plan.CentralControl(Color.White), 0.6)),
          sequence = Some(PlanSequenceSummary(TransitionType.Opening, 0.5))
        )
        .update(
          movingColor = Color.Black,
          ply = 4,
          primaryPlan = Some(pm(Plan.DefensiveConsolidation(Color.Black), 0.7)),
          secondaryPlan = None,
          sequence = Some(PlanSequenceSummary(TransitionType.ForcedPivot, 0.3))
        )

    val encoded = Json.toJson(tracker)
    assertEquals((encoded \ "version").asOpt[Int], Some(2))
    val decoded = Json.parse(Json.stringify(encoded)).as[PlanStateTracker]
    assertEquals(decoded, tracker)
  }

  test("PlanStateTracker should read legacy v1 history format (white only)") {
    val js = Json.obj(
      "history" -> Json.obj(
        "white" -> Json.obj(
          "planName" -> "Kingside Attack",
          "consecutivePlies" -> 2,
          "startingPly" -> 5
        )
      )
    )

    val decoded = js.as[PlanStateTracker]
    assertEquals(
      decoded.getContinuity(Color.White),
      Some(PlanContinuity("Kingside Attack", None, 2, 5))
    )
    assertEquals(decoded.getContinuity(Color.Black), None)
  }

  test("PlanStateTracker should read legacy v1 history format (both colors)") {
    val js = Json.obj(
      "history" -> Json.obj(
        "white" -> Json.obj(
          "planName" -> "Central Control",
          "consecutivePlies" -> 3,
          "startingPly" -> 1
        ),
        "black" -> Json.obj(
          "planName" -> "Defensive Consolidation",
          "consecutivePlies" -> 2,
          "startingPly" -> 2
        )
      )
    )
    val decoded = js.as[PlanStateTracker]
    assertEquals(decoded.getContinuity(Color.White).map(_.planName), Some("Central Control"))
    assertEquals(decoded.getContinuity(Color.Black).map(_.planName), Some("Defensive Consolidation"))
  }

  test("PlanStateTracker should default missing history keys to empty state") {
    val decoded = Json.obj().as[PlanStateTracker]
    assertEquals(decoded.getContinuity(Color.White), None)
    assertEquals(decoded.getContinuity(Color.Black), None)
    assertEquals(decoded.getColorState(Color.White).lastPly, None)
  }
