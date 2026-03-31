package lila.llm.analysis

import munit.FunSuite

import lila.llm.{ NarrativeSignalDigest, StrategyPack }
import lila.llm.analysis.practical.ContrastiveSupportAdmissibility
import lila.llm.analysis.render.QuietStrategicSupportComposer
import lila.llm.model.authoring.{ AuthorQuestionKind, NarrativeOutline, OutlineBeat, OutlineBeatKind }

class GameChronicleCompressionPolicyTest extends FunSuite:

  private val emptyParts =
    CommentaryEngine.HybridNarrativeParts(
      lead = "Lead",
      defaultBridge = "Bridge",
      criticalBranch = None,
      body = "Body",
      primaryPlan = None,
      focusedOutline = NarrativeOutline(beats = Nil),
      phase = "Middlegame",
      tacticalPressure = false,
      cpWhite = Some(20),
      bead = 1
    )

  private def partsFor(
      fixture: BookmakerProseGoldenFixtures.PlannerRuntimeFixture,
      extraBeats: List[OutlineBeat] = Nil
  ) =
    emptyParts.copy(
      focusedOutline =
        NarrativeOutline(
          beats =
            fixture.ctx.authorEvidence.headOption.flatMap(_.branches.headOption).map { branch =>
              OutlineBeat(
                kind = OutlineBeatKind.Evidence,
                text = s"Line: a) ${branch.line}",
                branchScoped = true
              )
            }.toList ++ extraBeats
        )
    )

  private def render(fixture: BookmakerProseGoldenFixtures.PlannerRuntimeFixture) =
    GameChronicleCompressionPolicy.render(
      ctx = fixture.ctx,
      parts = partsFor(fixture),
      strategyPack = fixture.strategyPack,
      truthContract = fixture.truthContract
    )

  private def plannerInputs(
      openingRelationClaim: Option[String] = None,
      endgameTransitionClaim: Option[String] = None
  ) =
    QuestionPlannerInputs(
      mainBundle = None,
      quietIntent = None,
      decisionFrame = CertifiedDecisionFrame(),
      decisionComparison = None,
      alternativeNarrative = None,
      truthMode = PlayerFacingTruthMode.Strategic,
      preventedPlansNow = Nil,
      pvDelta = None,
      counterfactual = None,
      practicalAssessment = None,
      opponentThreats = Nil,
      forcingThreats = Nil,
      evidenceByQuestionId = Map.empty,
      candidateEvidenceLines = List("14...Rc8 15.Re1 Qc7"),
      evidenceBackedPlans = Nil,
      opponentPlan = None,
      factualFallback = None,
      openingRelationClaim = openingRelationClaim,
      endgameTransitionClaim = endgameTransitionClaim
    )

  private def plannerPlan(
      questionId: String,
      kind: AuthorQuestionKind,
      claim: String,
      ownerFamily: OwnerFamily,
      ownerSource: String,
      contrast: Option[String]
  ) =
    QuestionPlan(
      questionId = questionId,
      questionKind = kind,
      priority = 100,
      claim = claim,
      evidence = None,
      contrast = contrast,
      consequence = None,
      fallbackMode = QuestionPlanFallbackMode.PlannerOwned,
      strengthTier = QuestionPlanStrengthTier.Moderate,
      sourceKinds = List(ownerSource),
      admissibilityReasons = List("test"),
      ownerFamily = ownerFamily,
      ownerSource = ownerSource
    )

  private val quietSupportGateTrace =
    QuietStrategicSupportComposer.QuietStrategicSupportGateTrace(
      sceneType = "quiet_improvement",
      selectedOwnerFamily = Some("MoveDelta"),
      selectedOwnerSource = Some("pv_delta"),
      pvDeltaAvailable = true,
      signalDigestAvailable = true,
      openingRelationClaimPresent = false,
      endgameTransitionClaimPresent = false,
      moveLinkedPvDeltaAnchorAvailable = true,
      rejectReasons = Nil
    )

  private def quietSupportTrace(
      text: String
  ) =
    QuietStrategicSupportComposer.QuietStrategicSupportTrace(
      emitted = true,
      line =
        Some(
          QuietStrategicSupportComposer.QuietStrategicSupportLine(
            text = text,
            bucket = QuietStrategicSupportComposer.Bucket.LongStructuralSqueeze,
            sourceKinds = List("MoveDelta.pv_delta", "Digest.structure"),
            verbFamily = QuietStrategicSupportComposer.VerbFamily.Reinforces
          )
        ),
      rejectReasons = Nil,
      gatePassed = true,
      gate = quietSupportGateTrace
    )

  private def phaseAQuietSupportPack(
      deploymentRoute: List[String]
  ): StrategyPack =
    StrategyPack(
      sideToMove = "white",
      signalDigest =
        Some(
          NarrativeSignalDigest(
            deploymentRoute = deploymentRoute
          )
        )
    )

  private def renderPlanSurface(
      fixture: BookmakerProseGoldenFixtures.PlannerRuntimeFixture,
      primary: QuestionPlan,
      secondary: Option[QuestionPlan] = None,
      quietTrace: QuietStrategicSupportComposer.QuietStrategicSupportTrace = quietSupportTrace(
        "This reinforces the fluid center."
      )
  ) =
    GameChronicleCompressionPolicy.renderPlanSurface(
      fixture.ctx,
      GameChronicleCompressionPolicy.ChronicleRenderSurface(
        primary = primary,
        secondary = secondary,
        contrastTrace = ContrastiveSupportAdmissibility.ContrastSupportTrace(),
        quietSupportTrace = quietTrace
      ),
      beatEvidence = Nil
    )

  test("chronicle positive planner fixtures surface planner-owned claims") {
    val positives =
      BookmakerProseGoldenFixtures.plannerRuntimeFixtures.filter(_.expectation == BookmakerProseGoldenFixtures.PlannerFixtureExpectation.Positive)

    positives.foreach { fixture =>
      val rendered = render(fixture).getOrElse(fail(s"expected chronicle render for ${fixture.id}"))
      fixture.expectedClaimFragment.foreach(fragment =>
        assert(rendered.toLowerCase.contains(fragment.toLowerCase), clue(s"${fixture.id}: $rendered"))
      )
      fixture.expectedFallbackClaim.foreach(fallback =>
        assert(!rendered.contains(fallback), clue(s"${fixture.id}: $rendered"))
      )
    }
  }

  test("chronicle negative and fallback fixtures fail closed to compact factual prose") {
    val failClosed =
      BookmakerProseGoldenFixtures.plannerRuntimeFixtures.filter(fixture =>
        fixture.expectedPrimaryKind.isEmpty && fixture.expectedFallbackClaim.nonEmpty
      )

    failClosed.foreach { fixture =>
      val rendered = render(fixture).getOrElse(fail(s"expected factual fallback for ${fixture.id}"))
      assertEquals(rendered, fixture.expectedFallbackClaim.get, clue(fixture.id))
    }
  }

  test("chronicle can surface a safe top-2 swap when race framing demotes to stopping counterplay") {
    val fixture =
      BookmakerProseGoldenFixtures.plannerRuntimeFixtures.find(_.id == "whose_plan_is_faster_negative")
        .getOrElse(fail("missing race-negative fixture"))

    val rendered = render(fixture).getOrElse(fail("expected chronicle render"))

    assert(rendered.toLowerCase.contains("stop"), clue(rendered))
    assert(!rendered.contains("This puts the rook on c3."), clue(rendered))
  }

  test("chronicle surfaces certified race framing only with supporting sentence material") {
    val fixture =
      BookmakerProseGoldenFixtures.plannerRuntimeFixtures.find(_.id == "whose_plan_is_faster_positive")
        .getOrElse(fail("missing race-positive fixture"))

    val rendered = render(fixture).getOrElse(fail("expected race chronicle render"))

    assert(rendered.toLowerCase.contains("queenside counterplay"), clue(rendered))
    assert(rendered.toLowerCase.contains("kingside") || rendered.toLowerCase.contains("23...rc8"), clue(rendered))
    assert(!rendered.contains("This puts the rook on c3."), clue(rendered))
  }

  test("chronicle only uses branch-scoped evidence when the cited line is anchored") {
    val fixture =
      BookmakerProseGoldenFixtures.plannerRuntimeFixtures.find(_.id == "what_changed_positive")
        .getOrElse(fail("missing what-changed fixture"))
    val branchyParts =
      partsFor(
        fixture,
        extraBeats =
          List(
            OutlineBeat(
              kind = OutlineBeatKind.Alternatives,
              text = "Branch leak marker without an inline line.",
              branchScoped = true
            )
          )
      )

    val rendered =
      GameChronicleCompressionPolicy
        .render(fixture.ctx, branchyParts, fixture.strategyPack, fixture.truthContract)
        .getOrElse(fail("expected chronicle render"))

    assert(!rendered.contains("Branch leak marker"), clue(rendered))
    assert(rendered.toLowerCase.contains("back-rank counterplay"), clue(rendered))
  }

  test("chronicle omits instead of salvaging raw bundle prose when there is no admitted plan or factual fallback") {
    val fixture =
      BookmakerProseGoldenFixtures.plannerRuntimeFixtures.find(_.id == "why_this_negative")
        .getOrElse(fail("missing negative fixture"))
    val noFallbackCtx =
      fixture.ctx.copy(
        playedMove = None,
        playedSan = None,
        authorQuestions = Nil
      )

    val rendered =
      GameChronicleCompressionPolicy.render(
        ctx = noFallbackCtx,
        parts = emptyParts,
        strategyPack = fixture.strategyPack,
        truthContract = fixture.truthContract
      )

    assertEquals(rendered, None)
  }

  test("chronicle keeps opening relation WhyThis ahead of a higher-priority WhatChanged swap") {
    val rankedPlans =
      RankedQuestionPlans(
        primary =
          Some(
            plannerPlan(
              questionId = "q_open_why",
              kind = AuthorQuestionKind.WhyThis,
              claim = "The move bends away from the usual opening script.",
              ownerFamily = OwnerFamily.OpeningRelation,
              ownerSource = "opening_relation_translator",
              contrast = Some("The move matters because it changes which opening script the position follows.")
            )
          ),
        secondary =
          Some(
            plannerPlan(
              questionId = "q_open_change",
              kind = AuthorQuestionKind.WhatChanged,
              claim = "The opening relationship changes immediately.",
              ownerFamily = OwnerFamily.OpeningRelation,
              ownerSource = "opening_relation_translator",
              contrast = Some("Before the move, the opening was still following the more familiar setup.")
            )
          ),
        rejected = Nil
      )

    val selected =
      GameChronicleCompressionPolicy
        .selectPlannerSurface(rankedPlans, plannerInputs(openingRelationClaim = Some("opening relation")))
        .getOrElse(fail("missing chronicle selection"))

    assertEquals(selected.primary.questionKind, AuthorQuestionKind.WhyThis)
    assertEquals(selected.secondary.map(_.questionKind), Some(AuthorQuestionKind.WhatChanged))
  }

  test("chronicle keeps threat-owned WhatMustBeStopped primary ahead of a WhyNow replay swap") {
    val rankedPlans =
      RankedQuestionPlans(
        primary =
          Some(
            plannerPlan(
              questionId = "q_stop",
              kind = AuthorQuestionKind.WhatMustBeStopped,
              claim = "This has to stop the threat before it lands.",
              ownerFamily = OwnerFamily.ForcingDefense,
              ownerSource = "threat",
              contrast = Some("If White drifts, the reply is forced.")
            )
          ),
        secondary =
          Some(
            plannerPlan(
              questionId = "q_why_now",
              kind = AuthorQuestionKind.WhyNow,
              claim = "The move has to happen now.",
              ownerFamily = OwnerFamily.ForcingDefense,
              ownerSource = "threat",
              contrast = Some("If delayed, the threat becomes concrete.")
            )
          ),
        rejected = Nil
      )

    val selected =
      GameChronicleCompressionPolicy
        .selectPlannerSurface(rankedPlans, plannerInputs())
        .getOrElse(fail("missing chronicle selection"))

    assertEquals(selected.primary.questionKind, AuthorQuestionKind.WhatMustBeStopped)
    assertEquals(selected.secondary.map(_.questionKind), Some(AuthorQuestionKind.WhyNow))
  }

  test("chronicle attaches quiet support only for claim-only MoveDelta pv_delta replay rows") {
    val fixture =
      BookmakerProseGoldenFixtures.plannerRuntimeFixtures.find(_.id == "what_changed_positive")
        .getOrElse(fail("missing what-changed fixture"))
    val artifact =
      renderPlanSurface(
        fixture,
        primary =
          plannerPlan(
            questionId = "q_quiet",
            kind = AuthorQuestionKind.WhatChanged,
            claim = "The move tightens the queenside bind.",
            ownerFamily = OwnerFamily.MoveDelta,
            ownerSource = "pv_delta",
            contrast = None
          )
      ).getOrElse(fail("expected chronicle render artifact"))

    assert(artifact.narrative.contains("The move tightens the queenside bind."), clue(artifact))
    assert(artifact.narrative.contains("This reinforces the fluid center."), clue(artifact))
    assertEquals(artifact.quietSupportTrace.applied, true, clue(artifact))
  }

  test("chronicle factual fallback stays claim-only even when quiet support is available") {
    val ctx =
      BookmakerProseGoldenFixtures.openFileFight.ctx.copy(
        authorQuestions = Nil,
        authorEvidence = Nil
      )
    val artifact =
      GameChronicleCompressionPolicy
        .renderWithTrace(
          ctx = ctx,
          parts = emptyParts,
          strategyPack =
            Some(
              phaseAQuietSupportPack(
                deploymentRoute = List("c3", "g3")
              )
            ),
          truthContract = None
        )
        .getOrElse(fail("expected chronicle factual fallback artifact"))

    assertEquals(artifact.narrative, "This puts the rook on c3.", clue(artifact))
    assertEquals(artifact.quietSupportTrace.applied, false, clue(artifact.quietSupportTrace))
    assertEquals(artifact.quietSupportTrace.composerTrace.emitted, true, clue(artifact.quietSupportTrace))
    assertEquals(
      artifact.quietSupportTrace.composerTrace.line.map(_.bucket),
      Some(QuietStrategicSupportComposer.Bucket.SlowRouteImprovement),
      clue(artifact.quietSupportTrace)
    )
    assert(
      artifact.quietSupportTrace.rejectReasons.contains("chronicle_exact_factual_support_blocked"),
      clue(artifact.quietSupportTrace)
    )
  }

  test("chronicle does not add quiet support when planner support is already present") {
    val fixture =
      BookmakerProseGoldenFixtures.plannerRuntimeFixtures.find(_.id == "what_changed_positive")
        .getOrElse(fail("missing what-changed fixture"))
    val artifact =
      renderPlanSurface(
        fixture,
        primary =
          plannerPlan(
            questionId = "q_supported",
            kind = AuthorQuestionKind.WhatChanged,
            claim = "The move tightens the queenside bind.",
            ownerFamily = OwnerFamily.MoveDelta,
            ownerSource = "pv_delta",
            contrast = Some("The move keeps the c-file pressure coordinated.")
          )
      ).getOrElse(fail("expected chronicle render artifact"))

    assert(!artifact.narrative.contains("This reinforces the fluid center."), clue(artifact))
    assertEquals(artifact.quietSupportTrace.applied, false, clue(artifact))
    assert(
      artifact.quietSupportTrace.rejectReasons.contains("surface_support_already_present"),
      clue(artifact.quietSupportTrace)
    )
  }

  test("chronicle does not reinterpret non-MoveDelta rows as quiet support scenes") {
    val fixture =
      BookmakerProseGoldenFixtures.plannerRuntimeFixtures.find(_.id == "what_changed_positive")
        .getOrElse(fail("missing what-changed fixture"))
    val artifact =
      renderPlanSurface(
        fixture,
        primary =
          plannerPlan(
            questionId = "q_forcing",
            kind = AuthorQuestionKind.WhyNow,
            claim = "The move has to happen now.",
            ownerFamily = OwnerFamily.ForcingDefense,
            ownerSource = "threat",
            contrast = None
          )
      ).getOrElse(fail("expected chronicle render artifact"))

    assert(!artifact.narrative.contains("This reinforces the fluid center."), clue(artifact))
    assertEquals(artifact.quietSupportTrace.applied, false, clue(artifact))
    assert(
      artifact.quietSupportTrace.rejectReasons.contains("surface_primary_not_movedelta_pv_delta"),
      clue(artifact.quietSupportTrace)
    )
  }
