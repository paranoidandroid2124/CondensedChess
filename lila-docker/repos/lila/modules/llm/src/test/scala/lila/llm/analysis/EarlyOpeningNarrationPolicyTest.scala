package lila.llm.analysis

import munit.FunSuite
import lila.llm.model.*
import lila.llm.model.authoring.*
import lila.llm.model.authoring.SeedFamily

class EarlyOpeningNarrationPolicyTest extends FunSuite:

  private val CompactBeatKinds = Set(
    OutlineBeatKind.MoveHeader,
    OutlineBeatKind.Context,
    OutlineBeatKind.MainMove,
    OutlineBeatKind.OpeningTheory
  )

  private def sentenceCount(text: String): Int =
    Option(text)
      .map(_.trim)
      .filter(_.nonEmpty)
      .map(_.split("(?<=[.!?])\\s+").toList.count(_.trim.nonEmpty))
      .getOrElse(0)

  private def earlyOpeningContext(
      variantKey: String = EarlyOpeningNarrationPolicy.StandardVariant,
      openingEvent: OpeningEvent = OpeningEvent.Intro("E04", "Catalan", "queenside pressure", List("d4", "Nf3", "g3")),
      ply: Int = 8,
      header: ContextHeader = ContextHeader("Opening", "Normal", "NarrowChoice", "Medium", "ExplainPlan"),
      renderMode: NarrativeRenderMode = NarrativeRenderMode.Bookmaker
  ): NarrativeContext =
    NarrativeContext(
      fen = "r1bqkbnr/pppp1ppp/2n5/4p3/3PP3/5N2/PPP2PPP/RNBQKB1R b KQkq - 2 3",
      header = header,
      ply = ply,
      playedMove = Some("g1f3"),
      playedSan = Some("Nf3"),
      summary = NarrativeSummary("Development lead", Some("...Bb4+"), "NarrowChoice", "Maintain", "+0.10"),
      threats = ThreatTable(Nil, Nil),
      pawnPlay = PawnPlayTable(false, None, "Low", "Maintain", "Quiet", "Background", None, false, "quiet"),
      plans = PlanTable(
        List(
          PlanRow(
            rank = 1,
            name = "Queenside pressure",
            score = 0.62,
            evidence = List("light-squared pressure"),
            confidence = ConfidenceLevel.Heuristic
          )
        ),
        Nil
      ),
      delta = None,
      phase = PhaseContext("Opening", "Development phase"),
      candidates = List(
        CandidateInfo(
          move = "Bb5",
          uci = Some("f1b5"),
          annotation = "!",
          planAlignment = "Development",
          tacticalAlert = None,
          practicalDifficulty = "clean",
          whyNot = None
        )
      ),
      latentPlans = List(
        LatentPlanNarrative(
          seedId = "latent_c4_break",
          planName = "c4 break",
          viabilityScore = 0.58,
          whyAbsentFromTopMultiPv = "it still needs full development first"
        )
      ),
      whyAbsentFromTopMultiPV = List("the c4 break still needs full development first"),
      authorQuestions = List(
        AuthorQuestion(
          id = "latent_plan",
          kind = AuthorQuestionKind.LatentPlan,
          priority = 1,
          question = "What slower plan is waiting in the position?",
          latentPlan = Some(
            LatentPlanInfo(
              seedId = "latent_c4_break",
              seedFamily = SeedFamily.Pawn,
              narrative = NarrativeTemplate("A delayed c4 break becomes playable once development is complete.")
            )
          )
        )
      ),
      openingEvent = Some(openingEvent),
      renderMode = renderMode,
      variantKey = variantKey
    )

  test("missing variant defaults to standard") {
    assertEquals(EarlyOpeningNarrationPolicy.normalizeVariantKey(None), EarlyOpeningNarrationPolicy.StandardVariant)
    assertEquals(EarlyOpeningNarrationPolicy.normalizeVariantKey(Some("unknown")), EarlyOpeningNarrationPolicy.StandardVariant)
  }

  test("standard intro-only opening collapses bookmaker outline and prose") {
    val ctx = earlyOpeningContext()

    assert(EarlyOpeningNarrationPolicy.collapsedEarlyOpening(ctx), clue(ctx))

    val outline = BookStyleRenderer.validatedOutline(ctx)
    assert(outline.beats.nonEmpty, clue(outline))
    assert(outline.beats.forall(beat => CompactBeatKinds.contains(beat.kind)), clue(outline.beats.map(_.kind)))
    assert(!outline.beats.exists(_.kind == OutlineBeatKind.WrapUp), clue(outline.beats.map(_.kind)))
    assert(!outline.beats.exists(_.kind == OutlineBeatKind.ConditionalPlan), clue(outline.beats.map(_.kind)))

    val prose = BookStyleRenderer.render(ctx)
    assert(sentenceCount(prose) <= EarlyOpeningNarrationPolicy.MaxCollapsedSentences, clue(prose))
    assert(!prose.contains("Ranked stack:"), clue(prose))
  }

  test("collapsed standard opening keeps game-arc narrative compact and neutral") {
    val ctx = earlyOpeningContext(renderMode = NarrativeRenderMode.FullGame)
    val moment =
      KeyMoment(
        ply = ctx.ply,
        momentType = "OpeningSetup",
        score = 0,
        description = "Development frame",
        selectionKind = "opening"
      )

    val parts = CommentaryEngine.buildHybridNarrativeParts(ctx, moment)
    val (rendered, _) = CommentaryEngine.renderHybridMomentNarrative(ctx, moment, prepared = Some(parts))

    assert(EarlyOpeningNarrationPolicy.collapsedEarlyOpening(ctx), clue(ctx))
    assertEquals(parts.criticalBranch, None)
    assert(sentenceCount(rendered) <= EarlyOpeningNarrationPolicy.MaxCollapsedSentences, clue(rendered))
    assert(!rendered.contains("Critical branch:"), clue(rendered))
  }

  test("chess960 does not trigger the standard early-opening collapse") {
    val ctx = earlyOpeningContext(variantKey = EarlyOpeningNarrationPolicy.Chess960Variant)

    assert(!EarlyOpeningNarrationPolicy.collapsedEarlyOpening(ctx), clue(ctx))

    val outline = BookStyleRenderer.validatedOutline(ctx)
    assert(outline.beats.exists(beat => !CompactBeatKinds.contains(beat.kind)), clue(outline.beats.map(_.kind)))
  }

  test("meaningful opening events bypass the collapse") {
    val ctx = earlyOpeningContext(openingEvent = OpeningEvent.OutOfBook("h3", List("Nf3", "Nc3"), 8))

    assert(!EarlyOpeningNarrationPolicy.collapsedEarlyOpening(ctx), clue(ctx))
  }

  test("forced or critical tactical states bypass the collapse") {
    val ctx = earlyOpeningContext(header = ContextHeader("Opening", "Critical", "OnlyMove", "High", "ExplainDefense"))

    assert(!EarlyOpeningNarrationPolicy.collapsedEarlyOpening(ctx), clue(ctx))
  }

  test("openings after ply ten keep the richer narrative path") {
    val ctx = earlyOpeningContext(ply = 12)

    assert(!EarlyOpeningNarrationPolicy.collapsedEarlyOpening(ctx), clue(ctx))
  }
