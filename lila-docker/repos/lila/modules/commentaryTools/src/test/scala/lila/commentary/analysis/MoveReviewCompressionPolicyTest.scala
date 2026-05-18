package lila.commentary.analysis

import lila.commentary.model.*
import munit.FunSuite

final class MoveReviewCompressionPolicyTest extends FunSuite:

  private def quietH3Ctx: NarrativeContext =
    NarrativeContext(
      fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
      header = ContextHeader("Opening", "Normal", "StyleChoice", "Low", "ExplainPlan"),
      ply = 1,
      playedMove = Some("h2h3"),
      playedSan = Some("h3"),
      summary = NarrativeSummary("quiet move", None, "StyleChoice", "Maintain", "0.00"),
      threats = ThreatTable(Nil, Nil),
      pawnPlay = PawnPlayTable(false, None, "Low", "Maintain", "Quiet", "Background", None, false, "quiet"),
      plans = PlanTable(Nil, Nil),
      delta = None,
      phase = PhaseContext("Opening", "quiet opening move"),
      candidates = Nil,
      renderMode = NarrativeRenderMode.Bookmaker
    )

  test("basic lane stays closed when no primitive is safe and exact factual fallback remains") {
    val ctx = quietH3Ctx
    val outline = BookStyleRenderer.validatedOutline(ctx)
    val explanation = BasicMoveExplanationBuilder.build(ctx, None)
    val slots =
      BookmakerPolishSlotsBuilder.buildOrFallback(
        ctx,
        outline,
        refs = None,
        strategyPack = None,
        truthContract = None
      )

    assertEquals(explanation, None, clues(explanation))
    assertEquals(
      BookmakerProseContract.stripMoveHeader(slots.claim),
      "This is a pawn move to h3.",
      clues(slots)
    )
    assertEquals(slots.paragraphPlan, List("p1=claim"), clues(slots))
  }

  test("existing planner-positive fixture still outranks the new basic lane") {
    val fixture =
      BookmakerProseGoldenFixtures.plannerRuntimeFixtures.find(_.expectedClaimFragment.nonEmpty).get
    val outline =
      BookStyleRenderer.validatedOutline(
        fixture.ctx,
        strategyPack = fixture.strategyPack,
        truthContract = fixture.truthContract
      )
    val slots =
      BookmakerPolishSlotsBuilder.buildOrFallback(
        fixture.ctx,
        outline,
        refs = None,
        strategyPack = fixture.strategyPack,
        truthContract = fixture.truthContract
      )
    val claim = BookmakerProseContract.stripMoveHeader(slots.claim).toLowerCase

    assertNotEquals(slots.sourceKind, "basic_move_explanation", clues(fixture.id, slots))
    assert(
      claim.contains(fixture.expectedClaimFragment.get.toLowerCase),
      clues(fixture.id, claim, slots)
    )
    assertNotEquals(slots.paragraphPlan, List("p1=claim"), clues(fixture.id, slots))
  }
