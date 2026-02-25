package lila.llm.analysis

import chess.Color
import lila.llm.model.{ Plan, PlanMatch, PlanSequenceSummary, TransitionType }
import lila.llm.model.strategic.PlanLifecyclePhase
import munit.FunSuite
import play.api.libs.json.Json

class PlanStateTrackerV3Test extends FunSuite:

  private val centralControl = PlanMatch(
    plan = Plan.CentralControl(Color.White),
    score = 0.8,
    evidence = Nil
  )

  test("phase escalates to Fruition on sustained continuity") {
    val t1 = PlanStateTracker.empty.update(
      movingColor = Color.White,
      ply = 10,
      primaryPlan = Some(centralControl),
      secondaryPlan = None
    )
    val t2 = t1.update(
      movingColor = Color.White,
      ply = 11,
      primaryPlan = Some(centralControl),
      secondaryPlan = None
    )
    val t3 = t2.update(
      movingColor = Color.White,
      ply = 12,
      primaryPlan = Some(centralControl),
      secondaryPlan = None
    )

    val continuity = t3.getContinuity(Color.White).get
    assertEquals(continuity.phase, PlanLifecyclePhase.Fruition)
    assert(continuity.commitmentScore >= 0.65)
  }

  test("forced pivot marks primary continuity as aborted") {
    val seq = PlanSequenceSummary(
      transitionType = TransitionType.ForcedPivot,
      momentum = 0.25,
      primaryPlanId = Some(centralControl.plan.id.toString),
      primaryPlanName = Some(centralControl.plan.name)
    )
    val tracker = PlanStateTracker.empty.update(
      movingColor = Color.White,
      ply = 20,
      primaryPlan = Some(centralControl),
      secondaryPlan = None,
      sequence = Some(seq)
    )

    val continuity = tracker.getContinuity(Color.White).get
    assertEquals(continuity.phase, PlanLifecyclePhase.Aborted)
    assertEquals(continuity.abortedReason, Some("forced pivot"))
  }

  test("json writer emits v3 and reader accepts legacy v1 continuity nodes") {
    val tracker = PlanStateTracker.empty.update(
      movingColor = Color.White,
      ply = 5,
      primaryPlan = Some(centralControl),
      secondaryPlan = None
    )
    val json = Json.toJson(tracker)
    assertEquals((json \ "version").as[Int], 3)

    val legacy = Json.obj(
      "history" -> Json.obj(
        "white" -> Json.obj(
          "planName" -> "Central Control",
          "planId" -> "CentralControl",
          "consecutivePlies" -> 2,
          "startingPly" -> 3
        ),
        "black" -> Json.obj()
      )
    )
    val decoded = legacy.validate[PlanStateTracker].asOpt.get
    val whitePrimary = decoded.getColorState(Color.White).primary.get
    assertEquals(whitePrimary.planName, "Central Control")
    assertEquals(whitePrimary.phase, PlanLifecyclePhase.Preparation)
  }
