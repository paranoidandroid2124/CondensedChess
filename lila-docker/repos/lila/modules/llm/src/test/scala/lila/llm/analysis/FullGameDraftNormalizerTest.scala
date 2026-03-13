package lila.llm.analysis

import munit.FunSuite
import lila.llm.model.*
import lila.llm.model.authoring.*

class FullGameDraftNormalizerTest extends FunSuite:

  test("render latent plan text interpolates side labels and seed id") {
    val text =
      FullGameDraftNormalizer.renderLatentPlanText(
        template = "If {them} is slow, {us} can begin with {seed}.",
        fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
        seedId = "PawnStorm_Kingside"
      )

    assertEquals(text, "If Black is slow, White can begin with a kingside pawn storm.")
  }

  test("normalize proseifies full-game meta labels") {
    val raw =
      "Idea: Build pressure. Primary route is c-file occupation. Ranked stack: 1. c-file occupation (0.82). Preconditions: rook access. Evidence: Structural support is present. Signals: open file, rook access. Refutation/Hold: The plan still needs the center to stay closed."

    val normalized = FullGameDraftNormalizer.normalize(raw)

    assert(!normalized.contains("Idea:"))
    assert(!normalized.contains("Ranked stack:"))
    assert(!normalized.contains("Signals:"))
    assert(!normalized.contains("Refutation/Hold:"))
    assert(normalized.contains("The leading route is c-file occupation."))
    assert(normalized.contains("Related candidates still cluster around 1. c-file occupation (0.82)."))
    assert(normalized.contains("This works best when rook access."))
    assert(normalized.contains("The clearest signs are open file, rook access."))
    assert(normalized.contains("The plan still needs the center to stay closed."))
  }

  test("render draft translates evidence labels into prose-only wrap-up text") {
    val ctx =
      NarrativeContext(
        fen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1",
        header = ContextHeader("Middlegame", "Normal", "NarrowChoice", "Medium", "ExplainPlan"),
        ply = 24,
        summary = NarrativeSummary("Restriction first", None, "NarrowChoice", "Maintain", "0.00"),
        threats = ThreatTable(Nil, Nil),
        pawnPlay = PawnPlayTable(false, None, "Low", "Maintain", "Quiet", "Background", None, false, "quiet"),
        plans = PlanTable(Nil, Nil),
        delta = None,
        phase = PhaseContext("Middlegame", "Balanced middlegame"),
        candidates = Nil,
        mainStrategicPlans = List(
          PlanHypothesis(
            planId = "break_prevention",
            planName = "Break prevention",
            rank = 1,
            score = 0.82,
            preconditions = List("rook access stays available", "them king remains a kingside target"),
            executionSteps = Nil,
            failureModes = List("opponent blocks with. 7-pawn push"),
            viability = PlanViability(0.82, "high", "stable"),
            refutation = Some("the center opens too early"),
            evidenceSources = List(
              "theme:restriction_prophylaxis",
              "subplan:break_prevention",
              "seed:pawnstorm_kingside"
            ),
            themeL1 = "restriction_prophylaxis",
            subplanId = Some("break_prevention")
          )
        ),
        renderMode = NarrativeRenderMode.FullGame
      )

    val draft = FullGameDraftNormalizer.normalize(BookStyleRenderer.renderDraft(ctx))

    assert(!draft.contains("theme:"))
    assert(!draft.contains("subplan:"))
    assert(!draft.contains("seed:"))
    assert(draft.contains("restrict opponent counterplay before expansion"))
    assert(draft.contains("deny opponent central break timing"))
    assert(draft.contains("gain flank space with rook-pawn advance"))
    assert(draft.contains("This works best when"))
    assert(draft.contains("the enemy king remains a kingside target"))
    assert(draft.contains("the opponent blocks with 7-pawn push"))
  }

  test("leak hits report both placeholder and meta-label leaks") {
    val hits =
      FullGameDraftNormalizer.leakHits(
        "Idea: If {them} is slow, {us} can work toward {seed}."
      )

    assert(hits.contains("Idea:"))
    assert(hits.contains("{them}"))
    assert(hits.contains("{us}"))
    assert(hits.contains("{seed}"))
  }

  test("placeholder detection ignores natural key theme prose but catches raw labels") {
    val proseHits = UserFacingSignalSanitizer.placeholderHits("Key theme: **PawnStorm Kingside**.")
    val rawHits =
      UserFacingSignalSanitizer.placeholderHits(
        "The draft still exposes theme:piece_redeployment and support:engine_hypothesis."
      )

    assertEquals(proseHits, Nil)
    assert(rawHits.contains("raw_label"))
  }
end FullGameDraftNormalizerTest
