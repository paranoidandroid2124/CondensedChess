package lila.commentary.analysis

import _root_.chess.{ Bishop, Color, Knight, Pawn, Queen, Rook, Square }
import lila.commentary.model.*
import munit.FunSuite

final class CommentaryFactSurfaceTest extends FunSuite:

  private def quietOpeningCtx(
      facts: List[Fact] = Nil,
      renderMode: NarrativeRenderMode = NarrativeRenderMode.Bookmaker
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
      plans = PlanTable(Nil, Nil),
      delta = None,
      phase = PhaseContext("Opening", "Normal development"),
      candidates = Nil,
      openingEvent = Some(OpeningEvent.Intro("C20", "Open Game", "central development", List("e4", "e5", "Nf3"))),
      facts = facts,
      renderMode = renderMode,
      variantKey = EarlyOpeningNarrationPolicy.StandardVariant
    )

  private def forcingCtx(facts: List[Fact]): NarrativeContext =
    quietOpeningCtx(facts).copy(
      header = ContextHeader("Opening", "Critical", "OnlyMove", "High", "ExplainDefense"),
      threats = ThreatTable(
        List(ThreatRow("Material", "US", Some("f3"), 250, 1, Some("Nd2"), 1, insufficientData = false)),
        Nil
      )
    )

  test("tactical facts expose one surface for tags statement branch reason and consequence") {
    val fork = Fact.Fork(Square.F5, Knight, List(Square.E7 -> Rook, Square.H4 -> Queen), FactScope.Now)
    val pin = Fact.Pin(Square.B4, Bishop, Square.C3, Knight, Square.E1, _root_.chess.King, isAbsolute = true, FactScope.Now)
    val ctx = forcingCtx(List(fork, pin))

    assertEquals(CommentaryFactSurface.tags(fork), List("fork"))
    assertEquals(CommentaryFactSurface.tags(pin), List("pin"))
    assert(CommentaryFactSurface.statement(0, fork, ctx).exists(_.toLowerCase.contains("fork")), clue(fork))
    assert(CommentaryFactSurface.statement(0, pin, ctx).exists(_.toLowerCase.contains("pin")), clue(pin))
    assert(CommentaryFactSurface.branchReason(pin).exists(_.toLowerCase.contains("pin pressure")), clue(pin))
    assert(CommentaryFactSurface.consequenceBody(fork).exists(_.toLowerCase.contains("fork")), clue(fork))
    assert(CommentaryFactSurface.consequenceBody(pin).exists(_.toLowerCase.contains("pin")), clue(pin))
  }

  test("hanging and target facts keep standard trust suppression centralized") {
    val hanging = Fact.HangingPiece(Square.E5, Pawn, List(Square.E4), Nil, FactScope.Now)
    val target = Fact.TargetPiece(Square.E5, Pawn, List(Square.H5), Nil, FactScope.Now)
    val quiet = quietOpeningCtx(List(hanging, target))
    val forcing = forcingCtx(List(Fact.HangingPiece(Square.F3, Bishop, List(Square.E4), Nil, FactScope.Now)))

    assertEquals(CommentaryFactSurface.statement(0, hanging, quiet), None)
    assertEquals(CommentaryFactSurface.statement(0, target, quiet), None)
    assert(
      CommentaryFactSurface
        .statement(0, forcing.facts.head, forcing)
        .exists(text => text.toLowerCase.contains("hanging") || text.toLowerCase.contains("underdefended")),
      clue(forcing.facts.head)
    )
    assertEquals(CommentaryFactSurface.tags(hanging), List("hanging_piece"))
    assertEquals(CommentaryFactSurface.tags(target), List("direct_threat"))
  }

  test("strategic and endgame facts share descriptor tags and wording") {
    val weak = Fact.WeakSquare(Square.D5, Color.Black, "no pawn defense", FactScope.Now)
    val kingActivity = Fact.KingActivity(Square.D4, mobility = 5, proximityToCenter = 1, FactScope.Now)
    val ctx = forcingCtx(List(weak, kingActivity))

    assertEquals(CommentaryFactSurface.tags(weak), List("weak_square"))
    assertEquals(CommentaryFactSurface.tags(kingActivity), List("king_activity"))
    assert(CommentaryFactSurface.statement(0, weak, ctx).exists(_.toLowerCase.contains("d5")), clue(weak))
    assert(CommentaryFactSurface.statement(0, kingActivity, ctx).exists(_.toLowerCase.contains("king")), clue(kingActivity))
    assert(CommentaryFactSurface.branchReason(weak).exists(_.toLowerCase.contains("d5")), clue(weak))
    assert(CommentaryFactSurface.consequenceBody(weak).exists(_.toLowerCase.contains("d5")), clue(weak))
    assert(CommentaryFactSurface.issueConsequence(weak).exists(_.startsWith("Consequence:")), clue(weak))
  }
