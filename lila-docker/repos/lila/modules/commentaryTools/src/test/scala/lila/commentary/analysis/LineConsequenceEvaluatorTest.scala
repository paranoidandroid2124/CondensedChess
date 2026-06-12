package lila.commentary.analysis

import lila.commentary.*
import lila.commentary.model.*
import lila.commentary.model.strategic.{ EngineEvidence, VariationLine }
import munit.FunSuite

final class LineConsequenceEvaluatorTest extends FunSuite:

  private val ExchangeFen =
    "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2"
  private val DirectBreakFen =
    "rnbqk2r/pp2bppp/4pn2/2p5/2BP4/4PN2/PP3PPP/RNBQ1RK1 w kq - 0 8"

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
      summary = NarrativeSummary("Line consequence", None, "NarrowChoice", "Maintain", "0.00"),
      threats = ThreatTable(Nil, Nil),
      pawnPlay = PawnPlayTable(false, None, "Low", "Maintain", "Quiet", "Background", None, false, "quiet"),
      plans = PlanTable(Nil, Nil),
      delta = None,
      phase = PhaseContext("Middlegame", "Line consequence"),
      candidates = Nil,
      engineEvidence = Some(EngineEvidence(depth = 20, variations = lines)),
      renderMode = NarrativeRenderMode.MoveReview
    )

  private def refs(startFen: String, lineId: String, ucis: List[String], sans: List[String]): MoveReviewRefs =
    val fens = ucis.indices.toList.map(idx => NarrativeUtils.uciListToFen(startFen, ucis.take(idx + 1)))
    MoveReviewRefs(
      startFen = startFen,
      startPly = NarrativeUtils.plyFromFen(startFen).getOrElse(1),
      variations =
        List(
          MoveReviewVariationRef(
            lineId = lineId,
            scoreCp = 42,
            mate = None,
            depth = 20,
            moves =
              ucis.zip(sans).zipWithIndex.map { case ((uci, san), idx) =>
                val ply = NarrativeUtils.plyFromFen(startFen).map(_ + 1 + idx).getOrElse(idx + 1)
                MoveReviewMoveRef(
                  refId = s"$lineId-${idx + 1}",
                  san = san,
                  uci = uci,
                  fenAfter = fens(idx),
                  ply = ply,
                  moveNo = (ply + 1) / 2,
                  marker = None
                )
              }
          )
        )
    )

  test("classifies a FEN-replayed exchange sequence as surface-safe") {
    val ucis = List("g1f3", "b8c6", "f1b5", "a7a6", "b5c6", "d7c6")
    val ctx = context(ExchangeFen, "g1f3", "Nf3", List(VariationLine(ucis, scoreCp = 42, depth = 20)))
    val evidence =
      LineConsequenceEvaluator
        .fromRefs(ctx, Some(refs(ExchangeFen, "exchange", ucis, List("Nf3", "Nc6", "Bb5", "a6", "Bxc6", "dxc6"))))
        .headOption
        .getOrElse(fail("missing line consequence"))

    assertEquals(evidence.kind, LineConsequenceKind.ExchangeSequence)
    assertEquals(evidence.release, LineConsequenceRelease.SurfaceCandidate)
    assertEquals(evidence.windowPly, 6)
    assertEquals(evidence.triggerSan, Some("Bxc6"))
    assert(evidence.consequence.toLowerCase.contains("exchange sequence"), clue(evidence))
    assertEquals(evidence.rejectReasons, Nil)
  }

  test("surfaceCandidate does not prefer a later consequence owned by another move") {
    val previewUcis = List("g1f3", "b8c6")
    val exchangeUcis = List("g1f3", "b8c6", "f1b5", "a7a6", "b5c6", "d7c6")
    val preview = refs(ExchangeFen, "preview", previewUcis, List("Nf3", "Nc6")).variations.head
    val exchange =
      refs(ExchangeFen, "exchange", exchangeUcis, List("Nf3", "Nc6", "Bb5", "a6", "Bxc6", "dxc6")).variations.head
    val combined =
      MoveReviewRefs(
        startFen = ExchangeFen,
        startPly = NarrativeUtils.plyFromFen(ExchangeFen).getOrElse(1),
        variations = List(preview, exchange)
      )
    val ctx = context(ExchangeFen, "g1f3", "Nf3", List(VariationLine(exchangeUcis, scoreCp = 42, depth = 20)))
    val evidence =
      LineConsequenceEvaluator
        .surfaceCandidate(ctx, Some(combined))
        .getOrElse(fail("missing preferred line consequence"))

    assertEquals(evidence.lineId, Some("preview"))
    assertEquals(evidence.kind, LineConsequenceKind.PreviewOnly)
  }

  test("reviewedMoveSurfaceCandidate can select a concrete played-first ref line for scoped line-consequence callers") {
    val fen = "r2q1rk1/1b2bppp/1p2p1n1/p1ppP3/5P2/2PBP1B1/PP1NQ1PP/R4RK1 b - - 0 13"
    val alternativeUcis = List("a5a4", "a2a3", "f7f5", "e5f6", "e7f6")
    val playedUcis = List("c5c4", "d3c2", "e7c5", "g1h1", "f7f5", "e5f6", "d8f6")
    val alternative =
      refs(fen, "alternative", alternativeUcis, List("a4", "a3", "f5", "exf6", "Bxf6")).variations.head
    val played =
      refs(fen, "played", playedUcis, List("c4", "Bc2", "Bc5", "Kh1", "f5", "exf6", "Qxf6")).variations.head
    val combined =
      MoveReviewRefs(
        startFen = fen,
        startPly = NarrativeUtils.plyFromFen(fen).getOrElse(1),
        variations = List(alternative, played)
      )
    val ctx = context(fen, "c5c4", "c4", List(VariationLine(playedUcis, scoreCp = 91, depth = 10)))

    val reviewed =
      LineConsequenceEvaluator
        .reviewedMoveSurfaceCandidate(ctx, Some(combined))
        .getOrElse(fail("missing reviewed-move line consequence"))

    assertEquals(reviewed.lineId, Some("played"))
    assertEquals(reviewed.kind, LineConsequenceKind.ExchangeSequence)
    assertEquals(reviewed.uciMoves.headOption, Some("c5c4"))
    assertEquals(reviewed.triggerSan, Some("exf6"))
  }

  test("surfaceCandidate prefers a concrete consequence triggered by the reviewed move") {
    val fen = "7k/8/4pn2/3p4/4P3/8/8/4K3 w - - 0 1"
    val previewUcis = List("e4d5", "h8g8")
    val exchangeUcis = List("e4d5", "f6d5")
    val preview = refs(fen, "preview", previewUcis, List("exd5", "Kg8")).variations.head
    val exchange = refs(fen, "exchange", exchangeUcis, List("exd5", "Nxd5")).variations.head
    val combined =
      MoveReviewRefs(
        startFen = fen,
        startPly = NarrativeUtils.plyFromFen(fen).getOrElse(1),
        variations = List(preview, exchange)
      )
    val ctx = context(fen, "e4d5", "exd5", List(VariationLine(exchangeUcis, scoreCp = 42, depth = 20)))
    val evidence =
      LineConsequenceEvaluator
        .surfaceCandidate(ctx, Some(combined))
        .getOrElse(fail("missing owned line consequence"))

    assertEquals(evidence.lineId, Some("exchange"))
    assertEquals(evidence.kind, LineConsequenceKind.ExchangeSequence)
    assertEquals(evidence.release, LineConsequenceRelease.SurfaceCandidate)
  }

  test("keeps illegal or mismatched ref lines diagnostic-only") {
    val ucis = List("g1f3", "b8c6", "f1b5")
    val corrupted =
      refs(ExchangeFen, "bad", ucis, List("Nf3", "Nc6", "Bb5")).copy(
        variations =
          refs(ExchangeFen, "bad", ucis, List("Nf3", "Nc6", "Bb5")).variations.map { variation =>
            variation.copy(moves = variation.moves.updated(1, variation.moves(1).copy(fenAfter = variation.moves.head.fenAfter)))
          }
      )
    val ctx = context(ExchangeFen, "g1f3", "Nf3", List(VariationLine(ucis, scoreCp = 42, depth = 20)))
    val evidence = LineConsequenceEvaluator.fromRefs(ctx, Some(corrupted)).headOption.getOrElse(fail("missing diagnostic"))

    assertEquals(evidence.release, LineConsequenceRelease.DiagnosticOnly)
    assert(evidence.rejectReasons.contains("line_consequence:ref_replay_failed"), clue(evidence))
  }

  test("uses the existing central-break witness before surfacing central-break timing") {
    val ucis = List("d4d5", "e6d5", "c4d5")
    val ctx = context(DirectBreakFen, "d4d5", "d5", List(VariationLine(ucis, scoreCp = 72, depth = 18)))
    val evidence =
      LineConsequenceEvaluator
        .fromRefs(ctx, Some(refs(DirectBreakFen, "break", ucis, List("d5", "exd5", "cxd5"))))
        .headOption
        .getOrElse(fail("missing central-break evidence"))

    assertEquals(evidence.kind, LineConsequenceKind.CentralBreakTiming)
    assertEquals(evidence.release, LineConsequenceRelease.SurfaceCandidate)
    assert(evidence.whyItMatters.exists(_.contains("central break")), clue(evidence))
  }

  test("observes a central pawn advance without upgrading it to central-break timing") {
    val fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    val ucis = List("g1f3", "d7d5")
    val ctx = context(fen, "g1f3", "Nf3", List(VariationLine(ucis, scoreCp = 18, depth = 18)))
    val evidence =
      LineConsequenceEvaluator
        .fromRefs(ctx, Some(refs(fen, "central-observed", ucis, List("Nf3", "d5"))))
        .headOption
        .getOrElse(fail("missing central pawn observation"))

    assertEquals(evidence.kind, LineConsequenceKind.CentralPawnAdvance)
    assertEquals(evidence.release, LineConsequenceRelease.SurfaceCandidate)
    assert(evidence.consequence.contains("central pawn advance"), clue(evidence))
    assert(!evidence.playerSentence.contains("times the central break"), clue(evidence))
  }

  test("classifies a replayed pawn capture that creates a passed pawn") {
    val fen = "4k3/8/1p6/2P5/8/8/8/4K3 w - - 0 1"
    val ucis = List("c5b6")
    val ctx = context(fen, "c5b6", "cxb6", List(VariationLine(ucis, scoreCp = 84, depth = 18)))
    val evidence =
      LineConsequenceEvaluator
        .fromRefs(ctx, Some(refs(fen, "passed-pawn", ucis, List("cxb6"))))
        .headOption
        .getOrElse(fail("missing passed-pawn evidence"))

    assertEquals(evidence.kind, LineConsequenceKind.PassedPawnCreation)
    assertEquals(evidence.release, LineConsequenceRelease.SurfaceCandidate)
    assertEquals(evidence.triggerSan, Some("cxb6"))
    assert(evidence.playerSentence.contains("passed pawn"), clue(evidence))
    assert(VariationNarrativeBuilder.build(ctx, evidence).exists(_.contains("passed pawn")), clue(evidence))
  }

  test("classifies an advanced passed-pawn push as a promotion race") {
    val fen = "4k3/8/1P6/8/8/8/8/4K3 w - - 0 1"
    val ucis = List("b6b7")
    val ctx = context(fen, "b6b7", "b7", List(VariationLine(ucis, scoreCp = 150, depth = 18)))
    val evidence =
      LineConsequenceEvaluator
        .fromRefs(ctx, Some(refs(fen, "promotion-race", ucis, List("b7"))))
        .headOption
        .getOrElse(fail("missing promotion-race evidence"))

    assertEquals(evidence.kind, LineConsequenceKind.PromotionRace)
    assertEquals(evidence.release, LineConsequenceRelease.SurfaceCandidate)
    assertEquals(evidence.triggerSan, Some("b7"))
    assert(evidence.playerSentence.contains("promotion race"), clue(evidence))
    assert(VariationNarrativeBuilder.build(ctx, evidence).exists(_.contains("promotion")), clue(evidence))
  }

  test("unreplayed engine-only variation evidence remains diagnostic-only") {
    val ucis = List("g1f3", "a1a8")
    val ctx = context(ExchangeFen, "g1f3", "Nf3", List(VariationLine(ucis, scoreCp = 42, depth = 20)))
    val evidence = LineConsequenceEvaluator.fromEngine(ctx).headOption.getOrElse(fail("missing engine fallback"))

    assertEquals(evidence.release, LineConsequenceRelease.DiagnosticOnly)
    assert(evidence.rejectReasons.contains("line_consequence:engine_only"), clue(evidence))
  }

  test("legal engine replay is available as internal narrative evidence but not public surface evidence") {
    val ucis = List("g1f3", "b8c6", "f1b5", "a7a6", "b5c6", "d7c6")
    val ctx = context(ExchangeFen, "g1f3", "Nf3", List(VariationLine(ucis, scoreCp = 42, depth = 20)))
    val evidence = LineConsequenceEvaluator.fromEngine(ctx).headOption.getOrElse(fail("missing engine fallback"))

    assertEquals(evidence.kind, LineConsequenceKind.ExchangeSequence)
    assertEquals(evidence.release, LineConsequenceRelease.ReplayBackedInternal)
    assert(evidence.rejectReasons.contains("line_consequence:engine_only"), clue(evidence))
    assertEquals(LineConsequenceEvaluator.surfaceCandidate(ctx, refs = None), None)
    assertEquals(LineConsequenceEvaluator.narrativeCandidate(ctx, refs = None).map(_.release), Some(LineConsequenceRelease.ReplayBackedInternal))
  }

  test("engine-only line consequence can use a concrete legal prefix without trusting a stale tail") {
    val ucis = List("g1f3", "b8c6", "f1b5", "a7a6", "b5c6", "d7c6", "a1a8")
    val ctx = context(ExchangeFen, "g1f3", "Nf3", List(VariationLine(ucis, scoreCp = 42, depth = 20)))
    val evidence =
      LineConsequenceEvaluator
        .fromEngine(ctx, maxPly = 7)
        .headOption
        .getOrElse(fail("missing engine prefix evidence"))

    assertEquals(evidence.kind, LineConsequenceKind.ExchangeSequence)
    assertEquals(evidence.release, LineConsequenceRelease.ReplayBackedInternal)
    assertEquals(evidence.uciMoves, ucis.take(6))
    assertEquals(evidence.windowPly, 6)
  }

  test("engine-only stale tail cannot promote a preview prefix through mate metadata") {
    val ucis = List("g1f3", "a1a8")
    val ctx = context(ExchangeFen, "g1f3", "Nf3", List(VariationLine(ucis, scoreCp = 42, mate = Some(1), depth = 20)))
    val evidence =
      LineConsequenceEvaluator
        .fromEngine(ctx, maxPly = 2)
        .headOption
        .getOrElse(fail("missing engine diagnostic evidence"))

    assertEquals(evidence.kind, LineConsequenceKind.PreviewOnly)
    assertEquals(evidence.release, LineConsequenceRelease.DiagnosticOnly)
    assert(evidence.rejectReasons.contains("line_consequence:engine_replay_failed"), clue(evidence))
  }

  test("internal narrative candidate can inspect an eight-ply replay window") {
    val fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    val ucis = List("g1f3", "g8f6", "g2g3", "g7g6", "f1g2", "f8g7", "e1g1", "e8g8")
    val ctx = context(fen, "g1f3", "Nf3", List(VariationLine(ucis, scoreCp = 12, depth = 18)))
    val evidence =
      LineConsequenceEvaluator
        .narrativeCandidate(ctx, refs = None)
        .getOrElse(fail("missing internal narrative candidate"))

    assertEquals(evidence.windowPly, 8)
    assertEquals(evidence.uciMoves.length, 8)
  }
