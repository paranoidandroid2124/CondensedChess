package lila.commentary.analysis

import chess.Color
import lila.commentary.model.{ Plan, PlanMatch, PlanSequenceSummary, TransitionType }
import lila.commentary.model.strategic.{ EndgamePatternState, PlanContinuity, PlanLifecyclePhase, TheoreticalOutcomeHint }
import munit.FunSuite
import play.api.libs.json.Json
import java.util.Locale

class PlanStateTrackerTest extends FunSuite:

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

  test("json writer emits the current format and reader accepts legacy v1 continuity nodes") {
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

  test("fingerprints use locale-independent two-decimal formatting") {
    val original = Locale.getDefault
    Locale.setDefault(Locale.GERMANY)
    try
      val continuity =
        PlanContinuity(
          planName = "Central Control",
          planId = Some("central_control"),
          consecutivePlies = 2,
          startingPly = 9,
          phase = PlanLifecyclePhase.Execution,
          commitmentScore = 0.35
        )
      val colorState =
        PlanStateTracker.ColorPlanState(
          primary = Some(continuity),
          lastTransition = Some(PlanStateTracker.TransitionSnapshot(TransitionType.Continuation, 0.75)),
          lastPly = Some(12)
        )
      val endgame =
        EndgamePatternState(
          activePattern = Some("rook_activity"),
          patternAge = 2,
          outcomeHint = TheoreticalOutcomeHint.Draw,
          prevKingActivityDelta = 1,
          prevConfidence = 0.67,
          lastPly = 44
        )

      assert(continuity.build_fingerprint.contains(":0.35:"), clue(continuity.build_fingerprint))
      assert(colorState.build_fingerprint.contains("Continuation:0.75"), clue(colorState.build_fingerprint))
      assert(endgame.build_fingerprint.contains(":0.67:"), clue(endgame.build_fingerprint))
    finally Locale.setDefault(original)
  }
