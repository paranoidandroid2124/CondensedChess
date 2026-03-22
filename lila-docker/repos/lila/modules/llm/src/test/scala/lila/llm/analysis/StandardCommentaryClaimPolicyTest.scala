package lila.llm.analysis

import _root_.chess.{ Bishop, Pawn, Square }
import munit.FunSuite
import lila.llm.model.*

class StandardCommentaryClaimPolicyTest extends FunSuite:

  private def sentenceCount(text: String): Int =
    Option(text)
      .map(_.trim)
      .filter(_.nonEmpty)
      .map(_.split("(?<=[.!?])\\s+").count(_.trim.nonEmpty))
      .getOrElse(0)

  private def quietOpeningCtx(
      renderMode: NarrativeRenderMode = NarrativeRenderMode.Bookmaker,
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

  test("quiet standard opening collapses to a shared no-event note across bookmaker and chronicle") {
    val bookmakerCtx = quietOpeningCtx()
    val bookmaker = BookStyleRenderer.render(bookmakerCtx)

    assertEquals(
      bookmaker,
      "This is still standard opening development, and no major imbalance has hardened yet."
    )
    assertEquals(sentenceCount(bookmaker), 1, clue(bookmaker))
    assert(!bookmaker.toLowerCase.contains("hanging"), clue(bookmaker))
    assert(!bookmaker.toLowerCase.contains("underdefended"), clue(bookmaker))

    val chronicleCtx = quietOpeningCtx(renderMode = NarrativeRenderMode.FullGame)
    val moment =
      KeyMoment(
        ply = chronicleCtx.ply,
        momentType = "OpeningSetup",
        score = 0,
        description = "Quiet opening setup"
      )
    val (chronicle, _) = CommentaryEngine.renderHybridMomentNarrative(chronicleCtx, moment)

    assertEquals(chronicle, bookmaker)
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

  test("quiet chess960 opening does not auto-collapse to the standard no-event note") {
    val ctx = quietOpeningCtx(variantKey = EarlyOpeningNarrationPolicy.Chess960Variant)
    val prose = BookStyleRenderer.render(ctx)

    assertNotEquals(
      prose,
      "This is still standard opening development, and no major imbalance has hardened yet."
    )
  }
