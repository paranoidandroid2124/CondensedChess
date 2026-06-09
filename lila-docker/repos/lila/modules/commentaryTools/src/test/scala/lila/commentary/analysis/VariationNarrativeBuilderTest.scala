package lila.commentary.analysis

import lila.commentary.model.*
import lila.commentary.model.strategic.{ EngineEvidence, VariationLine }
import munit.FunSuite

final class VariationNarrativeBuilderTest extends FunSuite:

  private val ExchangeFen =
    "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2"

  private def context(
      fen: String,
      playedMove: String,
      playedSan: String,
      lines: List[VariationLine]
  ): NarrativeContext =
    NarrativeContext(
      fen = fen,
      header = ContextHeader("Middlegame", "Normal", "NarrowChoice", "Medium", "ExplainPlan"),
      ply = NarrativeUtils.plyFromFen(fen).getOrElse(1),
      playedMove = Some(playedMove),
      playedSan = Some(playedSan),
      summary = NarrativeSummary("Variation narrative", None, "NarrowChoice", "Maintain", "0.00"),
      threats = ThreatTable(Nil, Nil),
      pawnPlay = PawnPlayTable(false, None, "Low", "Maintain", "Quiet", "Background", None, false, "quiet"),
      plans = PlanTable(Nil, Nil),
      delta = None,
      phase = PhaseContext("Middlegame", "Line consequence"),
      candidates = Nil,
      engineEvidence = Some(EngineEvidence(depth = 20, variations = lines)),
      renderMode = NarrativeRenderMode.MoveReview
    )

  test("generates exchange sequence narrative") {
    val ucis = List("g1f3", "b8c6", "f1b5", "a7a6", "b5c6", "d7c6")
    val ctx = context(ExchangeFen, "g1f3", "Nf3", List(VariationLine(ucis, scoreCp = 42, depth = 20)))
    val consequence = LineConsequenceEvaluator.fromEngine(ctx).head
    val narrative = VariationNarrativeBuilder.build(ctx, consequence).getOrElse(fail("failed to build narrative"))

    assert(narrative.contains("trades the bishop for the knight on c6"), clue(narrative))
    assert(narrative.contains("2. Nf3 Nc6 3. Bb5 a6 4. Bxc6 dxc6"), clue(narrative)) // move numbering (ExchangeFen starts at ply 2, which is move 2. Nf3)
  }

  test("exchange sequence narrative highlights a queen trade after a pawn-capture prelude") {
    val fen = "rnbqkbnr/ppp2ppp/3p4/4P3/4P3/8/PPP2PPP/RNBQKBNR b KQkq - 0 3"
    val ucis = List("d6e5", "d1d8", "e8d8", "b1c3")
    val ctx = context(fen, "d6e5", "dxe5", List(VariationLine(ucis, scoreCp = 24, depth = 12)))
    val consequence = LineConsequenceEvaluator.fromEngine(ctx).head
    val narrative = VariationNarrativeBuilder.build(ctx, consequence).getOrElse(fail("failed to build narrative"))

    assert(narrative.contains("queen trade on d8"), clue(narrative))
    assert(!narrative.contains("trades the pawn for the pawn"), clue(narrative))
  }

  test("generates forcing check sequence narrative") {
    val fen = "rnbqkb1r/pppp1ppp/5n2/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3"
    val ucis = List("f3e5", "b8c6", "e5c6", "d7c6", "d2d3", "f8c5", "f1e2", "f6g4", "e2g4", "d8h4")
    val ctx = context(fen, "f3e5", "Nxe5", List(VariationLine(ucis, scoreCp = 15, depth = 20)))
    
    val consequence = LineConsequenceEvidence(
      lineId = None,
      sanMoves = List("Nxe5", "Nc6", "Nxc6", "dxc6", "d3", "Bc5", "Be2", "Ng4", "Bxg4", "Qh4"),
      uciMoves = ucis,
      scoreCp = Some(15),
      mate = Some(2), 
      depth = Some(20),
      windowPly = 10,
      kind = LineConsequenceKind.ForcingCheckSequence,
      triggerSan = Some("Qh4"),
      consequence = "",
      whyItMatters = None,
      release = LineConsequenceRelease.SurfaceCandidate,
      rejectReasons = Nil
    )
    val narrative = VariationNarrativeBuilder.build(ctx, consequence).getOrElse(fail("failed to build narrative"))
    assert(narrative.contains("forcing"), clue(narrative))
    assert(narrative.contains("checks the king"), clue(narrative))
  }

  test("preview-only narrative uses SAN preview without move-number-only fragments") {
    val fen = "7r/pppr3p/2n1kp2/3Np3/4P3/P4P2/1PP3PP/2R1K2R w K - 0 18"
    val ucis = List("c2c3")
    val ctx = context(fen, "c2c3", "c3", List(VariationLine(ucis, scoreCp = 57, depth = 8)))
    val consequence =
      LineConsequenceEvidence(
        lineId = Some("line_preview"),
        sanMoves = List("c3"),
        uciMoves = ucis,
        scoreCp = Some(57),
        mate = None,
        depth = Some(8),
        windowPly = 1,
        kind = LineConsequenceKind.PreviewOnly,
        triggerSan = Some("c3"),
        consequence = "",
        whyItMatters = None,
        release = LineConsequenceRelease.SurfaceCandidate,
        rejectReasons = Nil
      )

    val narrative = VariationNarrativeBuilder.build(ctx, consequence).getOrElse(fail("failed to build preview narrative"))

    assertEquals(narrative, "The checked line continues c3.")
    assert(!narrative.contains("18."), clue(narrative))
  }
