package lila.commentary.analysis

import _root_.chess.{ Bishop, Pawn, Square }
import munit.FunSuite
import lila.commentary.model.*

class StandardCommentaryClaimPolicyTest extends FunSuite:

  private def sentenceCount(text: String): Int =
    Option(text)
      .map(_.trim)
      .filter(_.nonEmpty)
      .map(_.split("(?<=[.!?])\\s+").count(_.trim.nonEmpty))
      .getOrElse(0)

  private def quietOpeningCtx(
      renderMode: NarrativeRenderMode = NarrativeRenderMode.MoveReview,
      variantKey: String = EarlyOpeningNarrationPolicy.StandardVariant
  ): NarrativeContext =
    NarrativeContext(
      fen = "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2",
      header = ContextHeader("Opening", "Normal", "StyleChoice", "Low", "ExplainPlan"),
      ply = 4,
      playedMove = Some("e7e5"),
      playedSan = Some("e5"),
      summary = NarrativeSummary("Development", None, "StyleChoice", "Maintain", "0.00"),
      threats = ThreatTable(Nil, Nil),
      pawnPlay = PawnPlayTable(false, None, "Low", "Maintain", "Quiet", "Background", None, false, "quiet"),
      plans = PlanTable(
        List(
          PlanRow(
            rank = 1,
            name = "Central Control",
            score = 0.55,
            evidence = List("space"),
            confidence = ConfidenceLevel.Heuristic
          )
        ),
        Nil
      ),
      delta = None,
      phase = PhaseContext("Opening", "Normal development"),
      candidates = List(
        CandidateInfo(
          move = "Nf3",
          uci = Some("g1f3"),
          annotation = "",
          planAlignment = "Development",
          tacticalAlert = None,
          practicalDifficulty = "clean",
          whyNot = None
        )
      ),
      openingEvent = Some(OpeningEvent.Intro("C20", "Open Game", "central development", List("e4", "e5", "Nf3"))),
      facts = List(Fact.HangingPiece(Square.E5, Pawn, List(Square.E4), Nil, FactScope.Now)),
      renderMode = renderMode,
      variantKey = variantKey
    )

  test("quiet standard opening collapses to a no-event note in moveReview") {
    val moveReviewCtx = quietOpeningCtx()
    val moveReview = BookStyleRenderer.render(moveReviewCtx)

    assertEquals(
      moveReview,
      "This is still standard opening development, and no major imbalance has hardened yet."
    )
    assertEquals(sentenceCount(moveReview), 1, clue(moveReview))
    assert(!moveReview.toLowerCase.contains("hanging"), clue(moveReview))
    assert(!moveReview.toLowerCase.contains("underdefended"), clue(moveReview))
  }

  test("quiet standard opening suppresses central-pawn hanging wording") {
    val ctx = quietOpeningCtx()
    val text =
      NarrativeLexicon
        .getFactStatement(0, Fact.HangingPiece(Square.E5, Pawn, List(Square.E4), Nil, FactScope.Now), ctx)
        .trim

    assertEquals(text, "")
  }

  test("forcing standard positions still allow strong hanging wording when the liability is concrete") {
    val ctx =
      quietOpeningCtx().copy(
        header = ContextHeader("Opening", "Critical", "OnlyMove", "High", "ExplainDefense"),
        threats = ThreatTable(
          List(ThreatRow("Material", "US", Some("f3"), 250, 1, Some("Nd2"), 1, insufficientData = false)),
          Nil
        ),
        facts = List(Fact.HangingPiece(Square.F3, Bishop, List(Square.E4), Nil, FactScope.Now))
      )
    val text =
      NarrativeLexicon
        .getFactStatement(0, Fact.HangingPiece(Square.F3, Bishop, List(Square.E4), Nil, FactScope.Now), ctx)
        .toLowerCase

    assert(
      text.contains("hanging") || text.contains("underdefended") || text.contains("keep an eye"),
      clue(text)
    )
  }

  test("opponent plan row alone does not prevent quiet standard no-event note") {
    val ctx =
      quietOpeningCtx().copy(
        header = ContextHeader("Middlegame", "Normal", "StyleChoice", "Low", "ExplainPlan"),
        phase = PhaseContext("Middlegame", "Quiet position"),
        openingEvent = None,
        opponentPlan =
          Some(
            PlanRow(
              rank = 1,
              name = "Queenside counterplay",
              score = 0.72,
              evidence = List("pressure on the c-file"),
              confidence = ConfidenceLevel.Heuristic
            )
          )
      )

    val note = StandardCommentaryClaimPolicy.noEventNote(ctx)

    assert(note.exists(_.contains("not much to claim")), clue(note))
    assert(!StandardCommentaryClaimPolicy.allowsAmberTier(ctx), clue(note))
  }

  test("quiet chess960 opening does not auto-collapse to the standard no-event note") {
    val ctx = quietOpeningCtx(variantKey = EarlyOpeningNarrationPolicy.Chess960Variant)
    val prose = BookStyleRenderer.render(ctx)

    assertNotEquals(
      prose,
      "This is still standard opening development, and no major imbalance has hardened yet."
    )
  }
