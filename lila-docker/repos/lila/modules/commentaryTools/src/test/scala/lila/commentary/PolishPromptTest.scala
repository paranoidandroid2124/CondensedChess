package lila.commentary

import munit.FunSuite
import lila.commentary.analysis.{ BookStyleRenderer, MoveReviewPolishSlotsBuilder, MoveReviewProseGoldenFixtures }
import lila.commentary.model.*
import lila.commentary.model.authoring.{ NarrativeOutline, OutlineBeat, OutlineBeatKind }

class PolishPromptTest extends FunSuite:

  test("buildPolishPrompt uses a short structure reminder") {
    val prompt = PolishPrompt.buildPolishPrompt(
      prose = "White improves the knight and keeps pressure on e5.\n\nThe move also prepares f4.",
      phase = "middlegame",
      evalDelta = Some(18),
      concepts = List("space", "piece activity"),
      fen = "r2q1rk1/pp2bppp/2n1pn2/2pp4/3P4/2P1PN2/PPBNBPPP/R2Q1RK1 w - - 0 11",
      openingName = Some("Queen's Gambit Declined"),
      momentType = None
    )

    assert(prompt.contains("Structure reminder: return plain prose only"))
    assert(prompt.contains("do not emit UI section titles"))
    assert(prompt.contains("2-3 short paragraphs and 2-4 total sentences"))
    assert(prompt.contains("Context Mode: Isolated Move"))
    assert(prompt.indexOf("## CONTEXT") < prompt.indexOf("## DRAFT COMMENTARY"))
    assert(!prompt.contains("## MOVE_REVIEW PROSE CONTRACT"))
  }

  test("system prompt preserves the dominant strategic claim and causal chain") {
    assert(PolishPrompt.systemPrompt.contains("dominant strategic claim"))
    assert(PolishPrompt.systemPrompt.contains("cause -> effect chain"))
    assert(PolishPrompt.systemPrompt.contains("whole-game anchor nouns"))
    assert(PolishPrompt.estimatedSystemTokens <= 1500)
  }

  test("buildRepairPrompt keeps a short repair reminder") {
    val prompt = PolishPrompt.buildRepairPrompt(
      originalProse = "White centralizes the rook.\n\nThat keeps the pressure alive.",
      rejectedPolish = "Strategic Signals: White centralizes the rook.",
      phase = "middlegame",
      evalDelta = Some(-35),
      concepts = List("rook activity"),
      fen = "2r2rk1/pp3ppp/2n1pn2/2pp4/3P4/2P1PN2/PP1NBPPP/R1BQ1RK1 w - - 0 10",
      openingName = Some("Catalan"),
      allowedSans = List("Rc1", "Rfd1")
    )

    assert(prompt.contains("Structure reminder: return plain prose only"))
    assert(prompt.contains("do not emit UI section titles"))
    assert(prompt.contains("Repair REJECTED_POLISH into a strict-valid final commentary."))
    assert(prompt.contains("2-3 short paragraphs and 2-4 total sentences"))
    assert(prompt.indexOf("## CONTEXT") < prompt.indexOf("## ORIGINAL_DRAFT"))
    assert(!prompt.contains("## MOVE_REVIEW PROSE CONTRACT"))
  }

  test("slot-mode moveReview prompt bans system language and keeps slot budget fixed") {
    val fixture = MoveReviewProseGoldenFixtures.openFileFight
    val outline = BookStyleRenderer.validatedOutline(fixture.ctx)
    val slots =
      MoveReviewPolishSlotsBuilder.buildOrFallback(
        fixture.ctx,
        outline,
        refs = None,
        strategyPack = fixture.strategyPack
      )
    val prompt = PolishPrompt.buildPolishPrompt(
      prose = slots.claim,
      phase = "middlegame",
      evalDelta = Some(12),
      concepts = List("coordination"),
      fen = "r2q1rk1/p4ppp/1p2pn2/8/2PP4/5N2/P4PPP/R2Q1RK1 w - - 0 14",
      openingName = Some("Queen's Gambit Declined"),
      moveReviewSlots = Some(slots)
    )

    assert(prompt.contains("Do not introduce a new topic beyond the slots."))
    assert(prompt.contains("Do not turn sidecar metadata into prose."))
    assert(prompt.contains("Avoid these internal-system phrases:"))
    assert(prompt.contains("strategic stack"))
    if slots.paragraphPlan == List("p1=claim") then
      assert(prompt.contains("Keep a single brief paragraph built from the slot claim only."))
    else
      assert(prompt.contains("Do not add a fourth paragraph."))
  }

  test("slot-mode compact moveReview prompt allows a single brief paragraph") {
    val ctx =
      NarrativeContext(
        fen = "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2",
        header = ContextHeader("Opening", "Normal", "NarrowChoice", "Low", "ExplainPlan"),
        ply = 2,
        playedMove = Some("e7e5"),
        playedSan = Some("e5"),
        summary = NarrativeSummary("Open Game development", None, "NarrowChoice", "Maintain", "="),
        threats = ThreatTable(Nil, Nil),
        pawnPlay = PawnPlayTable(false, None, "Low", "Maintain", "Quiet", "Background", None, false, "quiet"),
        plans = PlanTable(Nil, Nil),
        delta = None,
        phase = PhaseContext("Opening", "Early opening development"),
        candidates = List(
          CandidateInfo(
            move = "e5",
            annotation = "",
            planAlignment = "Open Game development",
            tacticalAlert = None,
            practicalDifficulty = "clean",
            whyNot = None
          )
        ),
        openingEvent = Some(OpeningEvent.Intro("C20", "Open Game", "central development", List("e4", "e5"))),
        openingData = Some(OpeningReference(Some("C20"), Some("Open Game"), 0, Nil, Nil)),
        renderMode = NarrativeRenderMode.MoveReview,
        variantKey = "standard"
      )
    val outline =
      NarrativeOutline(
        beats = List(
          OutlineBeat(kind = OutlineBeatKind.Context, text = "This is still normal Open Game development, and no major imbalance has hardened yet."),
          OutlineBeat(kind = OutlineBeatKind.MainMove, text = "The move keeps the center balanced.")
        )
      )
    val slots = MoveReviewPolishSlotsBuilder.buildOrFallback(ctx, outline, refs = None)
    val prompt = PolishPrompt.buildPolishPrompt(
      prose = slots.claim,
      phase = "opening",
      evalDelta = None,
      concepts = Nil,
      fen = "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2",
      openingName = Some("Open Game"),
      moveReviewSlots = Some(slots)
    )

    assert(prompt.contains("keep one brief paragraph with 1-2 sentences"))
    assert(prompt.contains("Keep a single brief paragraph built from the slot claim only."))
  }

  test("buildSegmentRepairPrompt keeps lock-anchor instructions narrow") {
    val prompt = PolishPrompt.buildSegmentRepairPrompt(
      originalSegment = "White improves [[LK_001]] before [[LK_002]].",
      rejectedPolish = "White improves the pieces quickly.",
      phase = "middlegame",
      evalDelta = Some(12),
      concepts = List("coordination"),
      fen = "2r2rk1/pp3ppp/2n1pn2/2pp4/3P4/2P1PN2/PP1NBPPP/R1BQ1RK1 w - - 0 10",
      openingName = Some("Catalan")
    )

    assert(prompt.contains("Repair REJECTED_POLISH into a strict-valid commentary segment."))
    assert(prompt.contains("Lock anchors like [[LK_001]] must be preserved exactly"))
    assert(prompt.contains("## ORIGINAL_SEGMENT"))
    assert(prompt.contains("## REJECTED_POLISH"))
  }

  test("buildPolishPrompt omits empty optional context fields") {
    val prompt = PolishPrompt.buildPolishPrompt(
      prose = "White keeps the position under control.",
      phase = "opening",
      evalDelta = None,
      concepts = Nil,
      fen = "",
      openingName = None,
      nature = None,
      tension = None,
      salience = None,
      momentType = Some("Game Intro")
    )

    assert(prompt.contains("Phase: opening | Eval Δ: N/A"))
    assert(prompt.contains("Context Mode: Key Moment (Game Intro) - Part of Full Game Review"))
    assert(!prompt.contains("Opening:"))
    assert(!prompt.contains("Concepts:"))
    assert(!prompt.contains("FEN:"))
    assert(!prompt.contains("Salience:"))
    assert(!prompt.contains("Nature:"))
    assert(!prompt.contains("Tension:"))
  }

  test("buildPolishPrompt and buildRepairPrompt format and include factFragments") {
    val fixture = MoveReviewProseGoldenFixtures.openFileFight
    val outline = BookStyleRenderer.validatedOutline(fixture.ctx)
    val baseSlots = MoveReviewPolishSlotsBuilder.buildOrFallback(
      fixture.ctx,
      outline,
      refs = None,
      strategyPack = fixture.strategyPack
    )
    val slots = baseSlots.copy(
      claim = "12. Bc4 in the Italian Game develops through Development Logic.",
      factFragments = Some(List(
        FactFragment.OpeningGoalFragment(
          san = "Bc4",
          openingName = Some("Italian Game"),
          goalName = "Development Logic",
          supportedEvidence = List("Nf6", "d3")
        )
      ))
    )

    val prompt = PolishPrompt.buildPolishPrompt(
      prose = slots.claim,
      phase = "opening",
      evalDelta = None,
      concepts = Nil,
      fen = "",
      openingName = Some("Italian Game"),
      moveReviewSlots = Some(slots)
    )

    assert(prompt.contains("## DRY FACTS"))
    assert(prompt.contains("- OpeningGoal | san: Bc4 | opening: Italian Game | goal: Development Logic | supported: Nf6, d3"))

    val repairPrompt = PolishPrompt.buildRepairPrompt(
      originalProse = slots.claim,
      rejectedPolish = "Italian Game is cool.",
      phase = "opening",
      evalDelta = None,
      concepts = Nil,
      fen = "",
      openingName = Some("Italian Game"),
      moveReviewSlots = Some(slots)
    )

    assert(repairPrompt.contains("## DRY FACTS"))
    assert(repairPrompt.contains("- OpeningGoal | san: Bc4 | opening: Italian Game | goal: Development Logic | supported: Nf6, d3"))
  }
