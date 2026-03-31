package lila.llm.analysis

import munit.FunSuite

import lila.llm.{ NarrativeSignalDigest, StrategyPack }
import lila.llm.analysis.render.QuietStrategicSupportComposer
import lila.llm.model.NarrativeContext

class QuietStrategicSupportComposerTest extends FunSuite:

  private def baseCtx: NarrativeContext =
    BookmakerProseGoldenFixtures.openFileFight.ctx.copy(
      authorQuestions = Nil,
      authorEvidence = Nil
    )

  private def plannerState(
      ctx: NarrativeContext,
      strategyPack: Option[StrategyPack]
  ): (QuestionPlannerInputs, RankedQuestionPlans) =
    val inputs =
      QuestionPlannerInputsBuilder.build(ctx, strategyPack, truthContract = None)
    val rankedPlans =
      QuestionFirstCommentaryPlanner.plan(ctx, inputs, truthContract = None)
    (inputs, rankedPlans)

  private def quietPack(
      deploymentRoute: List[String] = Nil,
      structuralCue: Option[String] = None,
      practicalVerdict: Option[String] = None
  ): StrategyPack =
    StrategyPack(
      sideToMove = "white",
      signalDigest =
        Some(
          NarrativeSignalDigest(
            deploymentRoute = deploymentRoute,
            structuralCue = structuralCue,
            practicalVerdict = practicalVerdict
          )
        )
    )

  private def assertNoForbiddenVerb(text: String): Unit =
    val lowered = text.toLowerCase
    List("prepare", "launch", "force", "secure", "neutraliz").foreach { stem =>
      assert(!lowered.matches(s""".*\\b${stem}\\w*\\b.*"""), clues(text, stem))
    }

  test("eligible pv_delta plus route digest yields a bounded slow-route sentence") {
    val strategyPack = Some(quietPack(deploymentRoute = List("c3", "g3")))
    val (inputs, rankedPlans) = plannerState(baseCtx, strategyPack)

    val line =
      QuietStrategicSupportComposer
        .compose(baseCtx, inputs, rankedPlans, strategyPack)
        .getOrElse(fail("expected a quiet support line"))

    assertEquals(line.bucket, QuietStrategicSupportComposer.Bucket.SlowRouteImprovement, clues(line))
    assertEquals(line.verbFamily, QuietStrategicSupportComposer.VerbFamily.KeepsAvailable, clues(line))
    assert(line.sourceKinds.contains("MoveDelta.pv_delta"), clues(line))
    assert(line.sourceKinds.contains("Digest.route"), clues(line))
    assert(line.text.toLowerCase.contains("available"), clues(line))
    assertNoForbiddenVerb(line.text)
  }

  test("eligible pv_delta plus structure digest yields a bounded squeeze sentence") {
    val strategyPack = Some(quietPack(structuralCue = Some("queenside bind")))
    val (inputs, rankedPlans) = plannerState(baseCtx, strategyPack)

    val line =
      QuietStrategicSupportComposer
        .compose(baseCtx, inputs, rankedPlans, strategyPack)
        .getOrElse(fail("expected a structural quiet support line"))

    assertEquals(line.bucket, QuietStrategicSupportComposer.Bucket.LongStructuralSqueeze, clues(line))
    assertEquals(line.verbFamily, QuietStrategicSupportComposer.VerbFamily.Reinforces, clues(line))
    assert(line.sourceKinds.contains("MoveDelta.pv_delta"), clues(line))
    assert(line.sourceKinds.contains("Digest.structure"), clues(line))
    assert(line.text.toLowerCase.contains("reinforces"), clues(line))
    assertNoForbiddenVerb(line.text)
  }

  test("eligible pv_delta plus pressure digest yields a bounded pressure sentence") {
    val strategyPack =
      Some(
        quietPack(
          practicalVerdict = Some("pressure remains on the c-file")
        )
      )
    val (inputs, rankedPlans) = plannerState(baseCtx, strategyPack)

    val line =
      QuietStrategicSupportComposer
        .compose(baseCtx, inputs, rankedPlans, strategyPack)
        .getOrElse(fail("expected a pressure quiet support line"))

    assertEquals(
      line.bucket,
      QuietStrategicSupportComposer.Bucket.PressureMaintenanceWithoutImmediateTactic,
      clues(line)
    )
    assert(line.text.toLowerCase.contains("maintains pressure"), clues(line))
    assert(line.sourceKinds.contains("Digest.pressure"), clues(line))
    assertNoForbiddenVerb(line.text)
  }

  test("excluded opening or endgame domain claims never emit support") {
    val strategyPack = Some(quietPack(deploymentRoute = List("c3", "g3")))
    val (inputs, rankedPlans) = plannerState(baseCtx, strategyPack)

    val openingBlocked =
      QuietStrategicSupportComposer.compose(
        baseCtx,
        inputs.copy(openingRelationClaim = Some("This keeps the game on a sideline.")),
        rankedPlans,
        strategyPack
      )
    val endgameBlocked =
      QuietStrategicSupportComposer.compose(
        baseCtx,
        inputs.copy(endgameTransitionClaim = Some("This confirms the rook ending.")),
        rankedPlans,
        strategyPack
      )

    assertEquals(openingBlocked, None)
    assertEquals(endgameBlocked, None)
  }

  test("missing digest support returns none") {
    val strategyPack = Some(quietPack())
    val (inputs, rankedPlans) = plannerState(baseCtx, strategyPack)

    val line =
      QuietStrategicSupportComposer.compose(baseCtx, inputs, rankedPlans, strategyPack)

    assertEquals(line, None)
  }

  test("diagnose reports why no quiet support line was emitted") {
    val strategyPack = Some(quietPack())
    val (inputs, rankedPlans) = plannerState(baseCtx, strategyPack)

    val trace =
      QuietStrategicSupportComposer.diagnose(baseCtx, inputs, rankedPlans, strategyPack)

    assertEquals(trace.emitted, false, clues(trace))
    assertEquals(trace.line, None, clues(trace))
    assertEquals(trace.gatePassed, true, clues(trace))
    assertEquals(trace.gate.sceneType, SceneType.TransitionConversion.wireName, clues(trace))
    assertEquals(trace.gate.pvDeltaAvailable, true, clues(trace))
    assertEquals(trace.gate.moveLinkedPvDeltaAnchorAvailable, true, clues(trace))
    assert(trace.rejectReasons.contains("digest_route_missing"), clues(trace))
    assert(trace.rejectReasons.contains("digest_structure_missing"), clues(trace))
    assert(trace.rejectReasons.contains("digest_pressure_missing"), clues(trace))
  }
