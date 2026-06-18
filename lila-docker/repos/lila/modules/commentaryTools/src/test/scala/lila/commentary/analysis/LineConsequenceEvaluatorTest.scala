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

  test("checked line wording does not upgrade a check into a forced claim") {
    val fen = "1Qb1k3/2r2r1p/7p/8/8/3P2P1/PPP3P1/RN5K w - - 0 24"
    val ucis = List("b8b5", "e8d8", "b5d5", "d8e7", "b1c3", "f7f1", "a1f1")
    val ctx = context(fen, "b8b5", "Qb5+", List(VariationLine(ucis, scoreCp = 646, depth = 10)))
    val evidence =
      LineConsequenceEvaluator
        .fromEngine(ctx)
        .headOption
        .getOrElse(fail("missing line consequence"))

    assertEquals(evidence.kind, LineConsequenceKind.ForcingCheckSequence)
    assert(evidence.consequence.contains("gives check"), clue(evidence.consequence))
    assert(!evidence.consequence.toLowerCase.contains("forcing"), clue(evidence.consequence))
    assert(!evidence.consequence.toLowerCase.contains("forced"), clue(evidence.consequence))
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

  test("moveReviewCandidate can use a concrete reviewed capture line behind a one-ply preview") {
    val fen = "rnbqk2r/pppp1ppp/5n2/2b1N3/2B1P3/8/PPPP1PPP/RNBQK2R b KQkq - 0 4"
    val previewUcis = List("f6e4")
    val proofUcis = List("f6e4", "c4f7", "e8f8", "d2d4", "e4f2", "d1h5", "f2h1", "c1g5")
    val preview = refs(fen, "preview", previewUcis, List("Nxe4")).variations.head
    val proof =
      refs(fen, "proof", proofUcis, List("Nxe4", "Bxf7+", "Kf8", "d4", "Nxf2", "Qh5", "Nxh1", "Bg5"))
        .variations
        .head
    val combined =
      MoveReviewRefs(
        startFen = fen,
        startPly = NarrativeUtils.plyFromFen(fen).getOrElse(1),
        variations = List(preview, proof)
      )
    val ctx = context(fen, "f6e4", "Nxe4", List(VariationLine(proofUcis, scoreCp = 239, depth = 10)))
    val reviewed =
      LineConsequenceEvaluator
        .reviewedMoveSurfaceCandidate(ctx, Some(combined))
        .getOrElse(fail("missing reviewed capture line consequence"))

    assertEquals(reviewed.lineId, Some("proof"))
    assertEquals(reviewed.kind, LineConsequenceKind.ExchangeSequence)
    assertEquals(reviewed.uciMoves.headOption, Some("f6e4"))
    assertEquals(reviewed.triggerSan, Some("Nxe4"))

    val evidence =
      LineConsequenceEvaluator
        .moveReviewCandidate(ctx, Some(combined), truthContract = None)
        .getOrElse(fail("missing reviewed capture line consequence"))

    assertEquals(evidence.lineId, Some("proof"))
    assertEquals(evidence.kind, LineConsequenceKind.ExchangeSequence)
    assertEquals(evidence.uciMoves.headOption, Some("f6e4"))
    assertEquals(evidence.triggerSan, Some("Nxe4"))
  }

  test("moveReviewCandidate can prefer a played-first delayed pawn capture behind an unowned surface line") {
    val fen = "rnbqkb1r/pp2pppp/2p2n2/8/P1pP4/2N2N2/1P2PPPP/R1BQKB1R b KQkq - 0 5"
    val unownedUcis = List("e7e6", "e2e3", "c6c5", "f1c4", "c5d4", "e3d4", "f8b4", "e1g1", "e8g8")
    val playedUcis = List("c8f5", "e2e3", "e7e6", "f1c4", "f8d6", "d1e2", "e8g8", "e1g1")
    val unowned =
      refs(fen, "line_01", unownedUcis, List("e6", "e3", "c5", "Bxc4", "cxd4", "exd4", "Bb4", "O-O", "O-O"))
        .variations
        .head
    val played =
      refs(fen, "line_02", playedUcis, List("Bf5", "e3", "e6", "Bxc4", "Bd6", "Qe2", "O-O", "O-O"))
        .variations
        .head
    val combined =
      MoveReviewRefs(
        startFen = fen,
        startPly = NarrativeUtils.plyFromFen(fen).getOrElse(1),
        variations = List(unowned, played)
      )
    val ctx = context(fen, "c8f5", "Bf5", List(VariationLine(playedUcis, scoreCp = 35, depth = 10)))
    val tacticalTruth =
      DecisiveTruthContract(
        playedMove = Some("c8f5"),
        verifiedBestMove = Some("e7e6"),
        truthClass = DecisiveTruthClass.Acceptable,
        cpLoss = 21,
        swingSeverity = 0,
        reasonFamily = DecisiveReasonKind.TacticalRefutation,
        allowConcreteBenchmark = false,
        chosenMatchesBest = false,
        compensationAllowed = false,
        truthPhase = None,
        ownershipRole = TruthOwnershipRole.NoneRole,
        visibilityRole = TruthVisibilityRole.PrimaryVisible,
        surfaceMode = TruthSurfaceMode.Neutral,
        exemplarRole = TruthExemplarRole.NonExemplar,
        surfacedMoveOwnsTruth = false,
        verifiedPayoffAnchor = None,
        compensationProseAllowed = false,
        benchmarkProseAllowed = false,
        investmentTruthChainKey = None,
        maintenanceExemplarCandidate = false,
        benchmarkCriticalMove = false,
        failureMode = FailureInterpretationMode.NoClearPlan,
        failureIntentConfidence = 0.0,
        failureIntentAnchor = None,
        failureInterpretationAllowed = false
      )

    val reviewed =
      LineConsequenceEvaluator
        .reviewedMoveSurfaceCandidate(ctx, Some(combined))
        .getOrElse(fail("missing reviewed delayed capture line consequence"))

    assertEquals(reviewed.lineId, Some("line_02"))
    assertEquals(reviewed.kind, LineConsequenceKind.DelayedPawnCapture)
    assertEquals(reviewed.uciMoves.headOption, Some("c8f5"))
    assertEquals(reviewed.triggerSan, Some("Bxc4"))
    assert(reviewed.playerSentence.contains("delayed pawn capture"), clue(reviewed))

    val evidence =
      LineConsequenceEvaluator
        .moveReviewCandidate(ctx, Some(combined), truthContract = Some(tacticalTruth))
        .getOrElse(fail("missing move-review delayed capture line consequence"))

    assertEquals(evidence.lineId, Some("line_02"))
    assertEquals(evidence.kind, LineConsequenceKind.DelayedPawnCapture)
    assert(VariationNarrativeBuilder.build(ctx, evidence).exists(_.contains("pawn capture arrives after the first move")), clue(evidence))
  }

  test("actual e3 can bind the later Bxc4 pawn recovery as a reviewed line consequence") {
    val fen = "rn1qkb1r/pp2pppp/2p2n2/5b2/P1pP4/2N2N2/1P2PPPP/R1BQKB1R w KQkq - 1 6"
    val bestUcis = List("f3h4", "d8d7", "h4f5", "d7f5", "e2e3", "e7e5", "f1c4")
    val alternateUcis =
      List("f3e5", "b8d7", "e5c4", "d7b6", "c4e5", "b6d7", "e5d7", "d8d7", "f2f3", "e7e5", "d4e5", "d7d1", "e1d1")
    val playedUcis = List("e2e3", "e7e6", "f1c4", "f8b4", "e1g1", "e8g8", "d1b3")
    val combined =
      MoveReviewRefs(
        startFen = fen,
        startPly = NarrativeUtils.plyFromFen(fen).getOrElse(1),
        variations =
          List(
            refs(fen, "line_01", bestUcis, List("Nh4", "Qd7", "Nxf5", "Qxf5", "e3", "e5", "Bxc4")).variations.head,
            refs(
              fen,
              "line_02",
              alternateUcis,
              List("Ne5", "Nbd7", "Nxc4", "Nb6", "Ne5", "Nbd7", "Nxd7", "Qxd7", "f3", "e5", "dxe5", "Qxd1+", "Kxd1")
            ).variations.head,
            refs(fen, "line_03", playedUcis, List("e3", "e6", "Bxc4", "Bb4", "O-O", "O-O", "Qb3")).variations.head
          )
      )
    val quietTruth =
      DecisiveTruthContract(
        playedMove = Some("e2e3"),
        verifiedBestMove = Some("f3h4"),
        truthClass = DecisiveTruthClass.Acceptable,
        cpLoss = 14,
        swingSeverity = 0,
        reasonFamily = DecisiveReasonKind.QuietTechnicalMove,
        allowConcreteBenchmark = false,
        chosenMatchesBest = false,
        compensationAllowed = false,
        truthPhase = None,
        ownershipRole = TruthOwnershipRole.NoneRole,
        visibilityRole = TruthVisibilityRole.PrimaryVisible,
        surfaceMode = TruthSurfaceMode.Neutral,
        exemplarRole = TruthExemplarRole.NonExemplar,
        surfacedMoveOwnsTruth = false,
        verifiedPayoffAnchor = None,
        compensationProseAllowed = false,
        benchmarkProseAllowed = false,
        investmentTruthChainKey = None,
        maintenanceExemplarCandidate = false,
        benchmarkCriticalMove = false,
        failureMode = FailureInterpretationMode.NoClearPlan,
        failureIntentConfidence = 0.0,
        failureIntentAnchor = None,
        failureInterpretationAllowed = false
      )
    val ctx = context(fen, "e2e3", "e3", List(VariationLine(playedUcis, scoreCp = 27, depth = 10)))

    val reviewed =
      LineConsequenceEvaluator
        .reviewedMoveSurfaceCandidate(ctx, Some(combined))
        .getOrElse(fail("missing actual e3 reviewed line consequence"))

    assertEquals(reviewed.lineId, Some("line_03"))
    assertEquals(reviewed.kind, LineConsequenceKind.DelayedPawnCapture)
    assertEquals(reviewed.triggerSan, Some("Bxc4"))

    val evidence =
      LineConsequenceEvaluator
        .moveReviewCandidate(ctx, Some(combined), truthContract = Some(quietTruth))
        .getOrElse(fail("missing actual e3 delayed pawn capture candidate"))

    assertEquals(evidence.lineId, Some("line_03"))
    assertEquals(evidence.kind, LineConsequenceKind.DelayedPawnCapture)
    assertEquals(evidence.triggerSan, Some("Bxc4"))
  }

  test("classifies an immediate opponent pawn capture after a quiet reviewed move") {
    val fen = "3q1rk1/2p2ppp/1p1p1n2/B3p3/4P3/5N2/2P2PPP/1R3QK1 w - - 0 18"
    val ucis = List("a5e1", "f6e4", "f1c4", "e4c5")
    val refsValue = refs(fen, "line_04", ucis, List("Be1", "Nxe4", "Qc4", "Nc5"))
    val ctx = context(fen, "a5e1", "Be1", List(VariationLine(ucis, scoreCp = 107, depth = 18)))

    val reviewed =
      LineConsequenceEvaluator
        .reviewedMoveSurfaceCandidate(ctx, Some(refsValue))
        .getOrElse(fail("missing reviewed immediate pawn capture"))

    assertEquals(reviewed.kind, LineConsequenceKind.ImmediateOpponentPawnCapture)
    assertEquals(reviewed.triggerSan, Some("Nxe4"))
    assert(reviewed.playerSentence.contains("immediate pawn capture"), clue(reviewed))
    assert(VariationNarrativeBuilder.build(ctx, reviewed).exists(_.contains("Black answers with Nxe4")), clue(reviewed))
  }

  test("classifies immediate opponent king-zone target pressure after a quiet reviewed move") {
    val fen = "r1bq1rk1/ppp2ppp/1bnp1n2/4p3/4P3/2NP2P1/PPP1NPBP/R1BQ1RK1 w - - 2 8"
    val ucis = List("g1h1", "f6g4", "d1e1", "c6b4")
    val refsValue = refs(fen, "line_04", ucis, List("Kh1", "Ng4", "Qe1", "Nb4"))
    val ctx = context(fen, "g1h1", "Kh1", List(VariationLine(ucis, scoreCp = -42, depth = 10)))

    val reviewed =
      LineConsequenceEvaluator
        .reviewedMoveSurfaceCandidate(ctx, Some(refsValue))
        .getOrElse(fail("missing reviewed immediate target pressure"))

    assertEquals(reviewed.kind, LineConsequenceKind.ImmediateOpponentTargetPressure)
    assertEquals(reviewed.triggerSan, Some("Ng4"))
    assertEquals(reviewed.targetDetails.map(_.square).sorted, List("f2", "h2"))
    assert(reviewed.playerSentence.contains("immediate target pressure"), clue(reviewed))
    assert(VariationNarrativeBuilder.build(ctx, reviewed).exists(_.contains("Black answers with Ng4")), clue(reviewed))

    assertEquals(LineConsequenceEvaluator.surfaceCandidate(ctx, Some(refsValue)), None)
    assertEquals(LineConsequenceEvaluator.fromRefsForRoleComparison(ctx, Some(refsValue)), Nil)
    assertEquals(LineConsequenceEvaluator.moveReviewCandidate(ctx, Some(refsValue), truthContract = None), None)

    val tacticalTruth =
      DecisiveTruthContract(
        playedMove = Some("g1h1"),
        verifiedBestMove = Some("c3a4"),
        truthClass = DecisiveTruthClass.Inaccuracy,
        cpLoss = 88,
        swingSeverity = 1,
        reasonFamily = DecisiveReasonKind.TacticalRefutation,
        allowConcreteBenchmark = false,
        chosenMatchesBest = false,
        compensationAllowed = false,
        truthPhase = None,
        ownershipRole = TruthOwnershipRole.NoneRole,
        visibilityRole = TruthVisibilityRole.PrimaryVisible,
        surfaceMode = TruthSurfaceMode.Neutral,
        exemplarRole = TruthExemplarRole.NonExemplar,
        surfacedMoveOwnsTruth = false,
        verifiedPayoffAnchor = None,
        compensationProseAllowed = false,
        benchmarkProseAllowed = false,
        investmentTruthChainKey = None,
        maintenanceExemplarCandidate = false,
        benchmarkCriticalMove = false,
        failureMode = FailureInterpretationMode.TacticalRefutation,
        failureIntentConfidence = 0.0,
        failureIntentAnchor = None,
        failureInterpretationAllowed = false
      )
    val evidence =
      LineConsequenceEvaluator
        .moveReviewCandidate(ctx, Some(refsValue), truthContract = Some(tacticalTruth))
        .getOrElse(fail("missing gated immediate target pressure"))

    assertEquals(evidence.kind, LineConsequenceKind.ImmediateOpponentTargetPressure)
    assertEquals(evidence.lineId, Some("line_04"))
  }

  test("classifies played-move advanced pawn target pressure only for move-review admission") {
    val fen = "2r3k1/p4p1p/1qp3p1/2R5/3pr3/1PQ3P1/P4P1P/5BK1 w - - 0 23"
    val ucis = List("c3c4", "b6d8", "c4a6", "c8c7", "a6a5")
    val refsValue = refs(fen, "line_04", ucis, List("Qc4", "Qd8", "Qa6", "Rc7", "Qa5"))
    val ctx = context(fen, "c3c4", "Qc4", List(VariationLine(ucis, scoreCp = 4, depth = 10)))

    val reviewed =
      LineConsequenceEvaluator
        .reviewedMoveSurfaceCandidate(ctx, Some(refsValue))
        .getOrElse(fail("missing reviewed played-move target pressure"))

    assertEquals(reviewed.kind, LineConsequenceKind.PlayedMoveTargetPressure)
    assertEquals(reviewed.triggerSan, Some("Qc4"))
    assertEquals(reviewed.targetDetails.map(_.square), List("d4"))
    assert(reviewed.targetDetails.exists(detail =>
      detail.kind == "advanced_pawn_target_pressure" &&
        detail.role == "pawn" &&
        detail.attacker.contains("c4") &&
        detail.side.contains("black")
    ), clue(reviewed.targetDetails))
    assert(VariationNarrativeBuilder.build(ctx, reviewed).exists(_.contains("Qc4 puts pressure on the pawn on d4")), clue(reviewed))
    assert(VariationNarrativeBuilder.build(ctx, reviewed).exists(_.contains("same queen stays active with Qa6")), clue(reviewed))
    assertEquals(VariationNarrativeBuilder.build(ctx, reviewed.copy(targetDetails = Nil)), None)
    assertEquals(VariationNarrativeBuilder.build(ctx, reviewed.copy(uciMoves = reviewed.uciMoves.take(2), sanMoves = reviewed.sanMoves.take(2))), None)

    assertEquals(LineConsequenceEvaluator.surfaceCandidate(ctx, Some(refsValue)), None)
    assertEquals(LineConsequenceEvaluator.fromRefsForRoleComparison(ctx, Some(refsValue)), Nil)
    assertEquals(LineConsequenceEvaluator.moveReviewCandidate(ctx, Some(refsValue), truthContract = None), None)
    assertEquals(
      LineConsequenceEvaluator.moveReviewCandidate(ctx, Some(refsValue), truthContract = None, reviewedMoveOwnerCertified = true),
      None
    )

    val quietTruth =
      DecisiveTruthContract(
        playedMove = Some("c3c4"),
        verifiedBestMove = Some("c3c4"),
        truthClass = DecisiveTruthClass.Acceptable,
        cpLoss = 4,
        swingSeverity = 0,
        reasonFamily = DecisiveReasonKind.InvestmentSacrifice,
        allowConcreteBenchmark = false,
        chosenMatchesBest = false,
        compensationAllowed = false,
        truthPhase = None,
        ownershipRole = TruthOwnershipRole.NoneRole,
        visibilityRole = TruthVisibilityRole.PrimaryVisible,
        surfaceMode = TruthSurfaceMode.Neutral,
        exemplarRole = TruthExemplarRole.NonExemplar,
        surfacedMoveOwnsTruth = false,
        verifiedPayoffAnchor = None,
        compensationProseAllowed = false,
        benchmarkProseAllowed = false,
        investmentTruthChainKey = None,
        maintenanceExemplarCandidate = false,
        benchmarkCriticalMove = false,
        failureMode = FailureInterpretationMode.NoClearPlan,
        failureIntentConfidence = 0.0,
        failureIntentAnchor = None,
        failureInterpretationAllowed = false
      )
    val evidence =
      LineConsequenceEvaluator
        .moveReviewCandidate(ctx, Some(refsValue), truthContract = Some(quietTruth))
        .getOrElse(fail("missing gated played-move target pressure"))

    assertEquals(evidence.kind, LineConsequenceKind.PlayedMoveTargetPressure)
    assertEquals(evidence.lineId, Some("line_04"))
  }

  test("classifies origin-square clearance when the checked line reuses the played move's origin") {
    val fen = "2r2rk1/pp3ppp/3qpn2/n2p1b2/3P4/2P2N1P/PP1NBPP1/R2QR1K1 w - - 6 13"
    val ucis = List("d2f1", "d6b6", "d1c1", "a5c6", "c1d2")
    val refsValue = refs(fen, "line_04", ucis, List("Nf1", "Qb6", "Qc1", "Nc6", "Qd2"))
    val ctx = context(fen, "d2f1", "Nf1", List(VariationLine(ucis, scoreCp = 28, depth = 10)))

    val reviewed =
      LineConsequenceEvaluator
        .reviewedMoveSurfaceCandidate(ctx, Some(refsValue))
        .getOrElse(fail("missing reviewed origin-square clearance"))

    assertEquals(reviewed.kind, LineConsequenceKind.OriginSquareClearance)
    assertEquals(reviewed.triggerSan, Some("Qd2"))
    assert(reviewed.playerSentence.contains("square cleared by Nf1"), clue(reviewed))
    assert(VariationNarrativeBuilder.build(ctx, reviewed).exists(_.contains("Nf1 clears d2")), clue(reviewed))
    assert(VariationNarrativeBuilder.build(ctx, reviewed).exists(_.contains("Qd2 later uses that square")), clue(reviewed))

    val evidence =
      LineConsequenceEvaluator
        .moveReviewCandidate(ctx, Some(refsValue), truthContract = None)
        .getOrElse(fail("missing move-review origin-square clearance"))

    assertEquals(evidence.kind, LineConsequenceKind.OriginSquareClearance)
    assertEquals(evidence.lineId, Some("line_04"))
  }

  test("classifies sliding path through the played move origin as origin-square clearance") {
    val fen = "r2q1rk1/ppp2ppp/1bnp1n2/8/4PBb1/2NP2P1/PPP1N1BP/R2Q1R1K b - - 0 10"
    val ucis = List("d8d7", "d1d2", "a8e8", "a2a4", "f6h5")
    val refsValue = refs(fen, "line_01", ucis, List("Qd7", "Qd2", "Rae8", "a4", "Nh5"))
    val ctx = context(fen, "d8d7", "Qd7", List(VariationLine(ucis, scoreCp = 0, depth = 10)))

    val evidence =
      LineConsequenceEvaluator
        .reviewedMoveSurfaceCandidate(ctx, Some(refsValue))
        .getOrElse(fail("missing sliding-path origin-square clearance"))

    assertEquals(evidence.kind, LineConsequenceKind.OriginSquareClearance)
    assertEquals(evidence.triggerSan, Some("Rae8"))
    assert(VariationNarrativeBuilder.build(ctx, evidence).exists(_.contains("Qd7 clears d8")), clue(evidence))
    assert(VariationNarrativeBuilder.build(ctx, evidence).exists(_.contains("Rae8 later uses that square")), clue(evidence))
  }

  test("does not classify a same-piece return as origin-square clearance") {
    val fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    val ucis = List("g1f3", "g8f6", "f3g1")
    val refsValue = refs(fen, "return", ucis, List("Nf3", "Nf6", "Ng1"))
    val ctx = context(fen, "g1f3", "Nf3", List(VariationLine(ucis, scoreCp = 0, depth = 10)))
    val evidence =
      LineConsequenceEvaluator
        .fromRefs(ctx, Some(refsValue))
        .headOption
        .getOrElse(fail("missing replayed line"))

    assertEquals(evidence.kind, LineConsequenceKind.PreviewOnly)
  }

  test("moveReviewCandidate can use a played-first exchange branch for a quiet best move behind a preview") {
    val fen = "r3k2r/pp1bbpp1/1qn1p2p/2ppPn2/5B2/3P1N1P/PPPNBPP1/R1Q1K2R b KQkq - 0 11"
    val previewUcis = List("a8c8")
    val proofUcis =
      List("a8c8", "c2c3", "g7g5", "f4h2", "h6h5", "g2g4", "h5g4", "h3g4", "f5h4", "f3h4", "g5h4")
    val preview = refs(fen, "line_01", previewUcis, List("Rc8")).variations.head
    val proof =
      refs(
        fen,
        "line_04",
        proofUcis,
        List("Rc8", "c3", "g5", "Bh2", "h5", "g4", "hxg4", "hxg4", "Nh4", "Nxh4", "gxh4")
      ).variations.head
    val combined =
      MoveReviewRefs(
        startFen = fen,
        startPly = NarrativeUtils.plyFromFen(fen).getOrElse(1),
        variations = List(preview, proof)
      )
    val ctx = context(fen, "a8c8", "Rc8", List(VariationLine(previewUcis, scoreCp = -244, depth = 10)))
    val quietBestTruth =
      DecisiveTruthContract(
        playedMove = Some("a8c8"),
        verifiedBestMove = Some("a8c8"),
        truthClass = DecisiveTruthClass.Best,
        cpLoss = 0,
        swingSeverity = 0,
        reasonFamily = DecisiveReasonKind.QuietTechnicalMove,
        allowConcreteBenchmark = false,
        chosenMatchesBest = true,
        compensationAllowed = false,
        truthPhase = None,
        ownershipRole = TruthOwnershipRole.NoneRole,
        visibilityRole = TruthVisibilityRole.PrimaryVisible,
        surfaceMode = TruthSurfaceMode.Neutral,
        exemplarRole = TruthExemplarRole.NonExemplar,
        surfacedMoveOwnsTruth = false,
        verifiedPayoffAnchor = None,
        compensationProseAllowed = false,
        benchmarkProseAllowed = false,
        investmentTruthChainKey = None,
        maintenanceExemplarCandidate = false,
        benchmarkCriticalMove = false,
        failureMode = FailureInterpretationMode.NoClearPlan,
        failureIntentConfidence = 0.0,
        failureIntentAnchor = None,
        failureInterpretationAllowed = false
      )

    val evidence =
      LineConsequenceEvaluator
        .moveReviewCandidate(ctx, Some(combined), truthContract = Some(quietBestTruth))
        .getOrElse(fail("missing quiet-best branch consequence"))

    assertEquals(evidence.lineId, Some("line_04"))
    assertEquals(evidence.kind, LineConsequenceKind.ExchangeSequence)
    assertEquals(evidence.uciMoves.headOption, Some("a8c8"))
    assertEquals(evidence.triggerSan, Some("hxg4"))
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

  test("does not classify the actual Nb5 branch as passer creation when the ref tail immediately removes it") {
    val fen = "rnbqk1nr/pp2ppbp/6p1/3P4/5B2/2N2N2/PP3PPP/R2QKB1R w KQkq - 3 10"
    val ucis = List("c3b5", "b8a6", "f1c4", "g8f6", "d5d6", "e8g8", "e1g1", "e7d6", "f4d6")
    val sans = List("Nb5", "Na6", "Bc4", "Nf6", "d6", "O-O", "O-O", "exd6", "Bxd6")
    val ctx = context(fen, "c3b5", "Nb5", List(VariationLine(ucis, scoreCp = 169, depth = 10)))
    val evidence =
      LineConsequenceEvaluator
        .fromRefs(ctx, Some(refs(fen, "actual-nb5", ucis, sans)), maxPly = 8)
        .headOption
        .getOrElse(fail("missing Nb5 line consequence"))

    assertEquals(evidence.kind, LineConsequenceKind.PreviewOnly)
    assert(!evidence.playerSentence.toLowerCase.contains("passed pawn"), clue(evidence))
    assert(VariationNarrativeBuilder.build(ctx, evidence).forall(!_.toLowerCase.contains("passed pawn")), clue(evidence))
  }

  test("classifies an advanced passed-pawn push as a bounded promotion cue") {
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
    assert(evidence.playerSentence.contains("passed-pawn advance near promotion"), clue(evidence))
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
