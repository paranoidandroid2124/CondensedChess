package lila.llm.analysis

import munit.FunSuite

import lila.llm.model.authoring.{ NarrativeOutline, OutlineBeat, OutlineBeatKind }

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
      BookmakerProseGoldenFixtures.plannerRuntimeFixtures.find(_.id == "why_now_positive")
        .getOrElse(fail("missing why-now fixture"))
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
    assert(rendered.toLowerCase.contains("now"), clue(rendered))
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
