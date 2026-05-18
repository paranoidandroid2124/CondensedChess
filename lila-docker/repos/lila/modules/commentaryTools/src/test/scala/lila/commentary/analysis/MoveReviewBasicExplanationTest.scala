package lila.commentary.analysis

import lila.commentary.*
import lila.commentary.model.*
import munit.FunSuite

final class MoveReviewBasicExplanationTest extends FunSuite:

  private val italianBeforeBc4 =
    "r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3"

  private val italianOpening = OpeningReference(
    eco = Some("C50"),
    name = Some("Italian Game"),
    totalGames = 420000,
    topMoves = List(
      ExplorerMove("f1c4", "Bc4", 210000, 93000, 52000, 65000, 2460),
      ExplorerMove("f1b5", "Bb5", 180000, 78000, 50000, 52000, 2480)
    ),
    sampleGames = Nil
  )

  private def openingRef(name: String, eco: String, move: String, san: String): OpeningReference =
    OpeningReference(
      eco = Some(eco),
      name = Some(name),
      totalGames = 250000,
      topMoves = List(ExplorerMove(move, san, 120000, 52000, 31000, 37000, 2450)),
      sampleGames = Nil
    )

  private def openingCtx(
      fen: String,
      playedMove: String,
      playedSan: String,
      opening: OpeningReference,
      ply: Int,
      phaseReason: String
  ): NarrativeContext =
    NarrativeContext(
      fen = fen,
      header = ContextHeader("Opening", "Normal", "StyleChoice", "Low", "ExplainPlan"),
      ply = ply,
      playedMove = Some(playedMove),
      playedSan = Some(playedSan),
      summary = NarrativeSummary(opening.name.getOrElse("opening plan"), None, "StyleChoice", "Maintain", "0.00"),
      threats = ThreatTable(Nil, Nil),
      pawnPlay = PawnPlayTable(false, None, "Low", "Maintain", "Quiet", "Background", None, false, "quiet"),
      plans = PlanTable(
        top5 = List(
          PlanRow(
            rank = 1,
            name = opening.name.getOrElse("opening plan"),
            score = 0.62,
            evidence = List(s"played $playedSan in ${opening.name.getOrElse("the opening")}"),
            confidence = ConfidenceLevel.Heuristic
          )
        ),
        suppressed = Nil
      ),
      delta = None,
      phase = PhaseContext("Opening", phaseReason),
      candidates = List(
        CandidateInfo(
          move = playedSan,
          uci = Some(playedMove),
          annotation = "",
          planAlignment = opening.name.getOrElse("opening plan"),
          tacticalAlert = None,
          practicalDifficulty = "clean",
          whyNot = None
        )
      ),
      openingEvent = Some(OpeningEvent.Intro(opening.eco.getOrElse(""), opening.name.getOrElse("Opening"), phaseReason, List(playedSan))),
      openingData = Some(opening),
      renderMode = NarrativeRenderMode.Bookmaker
    )

  private def generalCtx(
      fen: String,
      playedMove: String,
      playedSan: String,
      phase: String = "Middlegame",
      ply: Int = 20,
      phaseReason: String
  ): NarrativeContext =
    NarrativeContext(
      fen = fen,
      header = ContextHeader(phase, "Normal", "StyleChoice", "Low", "ExplainPlan"),
      ply = ply,
      playedMove = Some(playedMove),
      playedSan = Some(playedSan),
      summary = NarrativeSummary(phaseReason, None, "StyleChoice", "Maintain", "0.00"),
      threats = ThreatTable(Nil, Nil),
      pawnPlay = PawnPlayTable(false, None, "Low", "Maintain", "Quiet", "Background", None, false, "quiet"),
      plans = PlanTable(Nil, Nil),
      delta = None,
      phase = PhaseContext(phase, phaseReason),
      candidates = List(
        CandidateInfo(
          move = playedSan,
          uci = Some(playedMove),
          annotation = "",
          planAlignment = phaseReason,
          tacticalAlert = None,
          practicalDifficulty = "clean",
          whyNot = None
        )
      ),
      openingEvent = None,
      openingData = None,
      renderMode = NarrativeRenderMode.Bookmaker
    )

  private def italianCtx: NarrativeContext =
    openingCtx(
      fen = italianBeforeBc4,
      playedMove = "f1c4",
      playedSan = "Bc4",
      opening = italianOpening,
      ply = 5,
      phaseReason = "Italian Game development"
    )

  private def refsWithShortLine: MoveReviewRefs =
    val afterBc4 = NarrativeUtils.uciListToFen(italianBeforeBc4, List("f1c4"))
    val afterNf6 = NarrativeUtils.uciListToFen(italianBeforeBc4, List("f1c4", "g8f6"))
    val afterD3 = NarrativeUtils.uciListToFen(italianBeforeBc4, List("f1c4", "g8f6", "d2d3"))
    MoveReviewRefs(
      startFen = italianBeforeBc4,
      startPly = 5,
      variations = List(
        MoveReviewVariationRef(
          lineId = "line_01",
          scoreCp = 18,
          mate = None,
          depth = 16,
          moves = List(
            MoveReviewMoveRef("line_01_m01", "Bc4", "f1c4", afterBc4, 5, 3, Some("3.")),
            MoveReviewMoveRef("line_01_m02", "Nf6", "g8f6", afterNf6, 6, 3, Some("...")),
            MoveReviewMoveRef("line_01_m03", "d3", "d2d3", afterD3, 7, 4, Some("4."))
          )
        )
      )
    )

  private def refsForLine(startFen: String, ucis: List[String], sans: List[String], lineId: String = "line_01"): MoveReviewRefs =
    val fens = ucis.indices.toList.map(idx => NarrativeUtils.uciListToFen(startFen, ucis.take(idx + 1)))
    MoveReviewRefs(
      startFen = startFen,
      startPly = NarrativeUtils.plyFromFen(startFen).map(_ + 1).getOrElse(1),
      variations = List(
        MoveReviewVariationRef(
          lineId = lineId,
          scoreCp = 16,
          mate = None,
          depth = 16,
          moves =
            ucis.zip(sans).zipWithIndex.map { case ((uci, san), idx) =>
              val ply = NarrativeUtils.plyFromFen(startFen).map(_ + 1 + idx).getOrElse(idx + 1)
              MoveReviewMoveRef(
                refId = s"${lineId}_m${idx + 1}",
                san = san,
                uci = uci,
                fenAfter = fens(idx),
                ply = ply,
                moveNo = (ply + 1) / 2,
                marker = Some(if ply % 2 == 1 then s"${(ply + 1) / 2}." else s"${(ply + 1) / 2}...")
              )
            }
        )
      )
    )

  private def variationForLine(startFen: String, ucis: List[String], sans: List[String], lineId: String): MoveReviewVariationRef =
    val fens = ucis.indices.toList.map(idx => NarrativeUtils.uciListToFen(startFen, ucis.take(idx + 1)))
    MoveReviewVariationRef(
      lineId = lineId,
      scoreCp = 16,
      mate = None,
      depth = 16,
      moves =
        ucis.zip(sans).zipWithIndex.map { case ((uci, san), idx) =>
          val ply = NarrativeUtils.plyFromFen(startFen).map(_ + 1 + idx).getOrElse(idx + 1)
          MoveReviewMoveRef(
            refId = s"${lineId}_m${idx + 1}",
            san = san,
            uci = uci,
            fenAfter = fens(idx),
            ply = ply,
            moveNo = (ply + 1) / 2,
            marker = Some(if ply % 2 == 1 then s"${(ply + 1) / 2}." else s"${(ply + 1) / 2}...")
          )
        }
    )

  private def catalogPattern(
      fen: String,
      playedMove: String,
      playedSan: String,
      openingName: String,
      refs: Option[MoveReviewRefs],
      ply: Int = 5
  ): Option[String] =
    val ctx =
      openingCtx(
        fen = fen,
        playedMove = playedMove,
        playedSan = playedSan,
        opening = openingRef(openingName, "A00", playedMove, playedSan),
        ply = ply,
        phaseReason = s"$openingName catalog pattern"
      )
    for
      facts <- MoveReviewBoardFacts.current(ctx)
      reasons = MoveReviewBoardFacts.primitiveTags(ctx, facts)
      lineFacts = MoveReviewPvFacts.firstCoupled(ctx.fen, facts.uci, refs)
      idea <- OpeningIdeaCatalog.matchIdea(ctx, facts, reasons, lineFacts)
    yield idea.pattern

  test("Italian Bc4 gets a concrete move-review explanation with a short PV line") {
    val explanation = BasicMoveExplanationBuilder.build(italianCtx, Some(refsWithShortLine))

    assert(explanation.nonEmpty, clue("expected an Italian basic explanation"))
    val moveReview = explanation.get
    assert(moveReview.title.contains("Bc4"), clue(moveReview.title))
    assert(moveReview.prose.contains("Italian"), clue(moveReview.prose))
    assert(moveReview.prose.contains("f7"), clue(moveReview.prose))
    assert(moveReview.prose.contains("d5"), clue(moveReview.prose))
    assert(moveReview.reasonTags.contains("opening_idea"), clue(moveReview.reasonTags))
    assert(moveReview.reasonTags.contains("develops_piece"), clue(moveReview.reasonTags))
    assert(moveReview.reasonTags.contains("targets_f7_or_f2"), clue(moveReview.reasonTags))
    assert(moveReview.reasonTags.contains("controls_center"), clue(moveReview.reasonTags))
    assertEquals(moveReview.shortLine.map(_.san), Some(List("Bc4", "Nf6", "d3")), clue(moveReview.shortLine))
    assertEquals(moveReview.pvInterpretation.map(_.linePurpose), Some("quiet_development"), clue(moveReview.pvInterpretation))
    assertEquals(moveReview.pvInterpretation.map(_.tension), Some("tension_maintained"), clue(moveReview.pvInterpretation))
    assertEquals(moveReview.pvInterpretation.flatMap(_.opponentReplyMeaning), Some("attacks_center_pawn"), clue(moveReview.pvInterpretation))
    assert(moveReview.pvInterpretation.exists(_.learningPoint.contains("d3")), clue(moveReview.pvInterpretation))
    assert(moveReview.pvInterpretation.exists(_.learningPoint.contains("e4")), clue(moveReview.pvInterpretation))
    assert(moveReview.prose.contains("Nf6"), clue(moveReview.prose))
    assert(moveReview.prose.contains("d3"), clue(moveReview.prose))
    assert(moveReview.prose.contains("e4"), clue(moveReview.prose))
  }

  test("Italian Bc4 without PV still explains the opening purpose and omits shortLine") {
    val explanation = BasicMoveExplanationBuilder.build(italianCtx, None)

    assert(explanation.nonEmpty, clue("expected an Italian basic explanation without PV"))
    val moveReview = explanation.get
    assert(moveReview.prose.contains("Italian"), clue(moveReview.prose))
    assert(moveReview.prose.contains("f7"), clue(moveReview.prose))
    assert(moveReview.prose.contains("d5"), clue(moveReview.prose))
    assertEquals(moveReview.shortLine, None, clue(moveReview))
    assertEquals(moveReview.pvInterpretation, None, clue(moveReview))
  }

  test("Italian catalog does not claim f7 pressure when the board path is blocked") {
    val blockedF7 =
      "r1bqkbnr/pppp1ppp/2n1p3/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 0 3"
    val explanation =
      BasicMoveExplanationBuilder.build(
        openingCtx(
          fen = blockedF7,
          playedMove = "f1c4",
          playedSan = "Bc4",
          opening = italianOpening,
          ply = 5,
          phaseReason = "Italian label with blocked diagonal"
        ),
        None
      ).get

    assert(!explanation.reasonTags.contains("opening_idea"), clue(explanation))
    assert(!explanation.prose.contains("f7"), clue(explanation.prose))
  }

  test("Ruy Lopez Bb5 separates the opening idea from a generic developing move") {
    val explanation =
      BasicMoveExplanationBuilder.build(
        openingCtx(
          fen = italianBeforeBc4,
          playedMove = "f1b5",
          playedSan = "Bb5",
          opening = openingRef("Ruy Lopez", "C60", "f1b5", "Bb5"),
          ply = 5,
          phaseReason = "Ruy Lopez pressure on the e5 defender"
        ),
        None
      ).get

    assert(explanation.title.contains("e5"), clue(explanation.title))
    assert(explanation.prose.contains("c6 knight"), clue(explanation.prose))
    assert(explanation.prose.contains("e5"), clue(explanation.prose))
    assert(explanation.reasonTags.contains("opening_idea"), clue(explanation.reasonTags))
    assert(explanation.reasonTags.contains("develops_piece"), clue(explanation.reasonTags))
  }

  test("Queen's Gambit c4 explains the d5 tension instead of only naming a pawn move") {
    val explanation =
      BasicMoveExplanationBuilder.build(
        openingCtx(
          fen = "rnbqkbnr/ppp1pppp/8/3p4/3P4/8/PPP1PPPP/RNBQKBNR w KQkq - 0 2",
          playedMove = "c2c4",
          playedSan = "c4",
          opening = openingRef("Queen's Gambit", "D06", "c2c4", "c4"),
          ply = 3,
          phaseReason = "Queen's Gambit central tension"
        ),
        None
      ).get

    assert(explanation.title.contains("d5"), clue(explanation.title))
    assert(explanation.prose.contains("d5 pawn"), clue(explanation.prose))
    assert(explanation.prose.contains("center"), clue(explanation.prose))
    assert(explanation.reasonTags.contains("controls_center"), clue(explanation.reasonTags))
  }

  test("Sicilian c5 explains the asymmetric d4 challenge") {
    val explanation =
      BasicMoveExplanationBuilder.build(
        openingCtx(
          fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1",
          playedMove = "c7c5",
          playedSan = "c5",
          opening = openingRef("Sicilian Defense", "B20", "c7c5", "c5"),
          ply = 2,
          phaseReason = "Sicilian flank challenge"
        ),
        None
      ).get

    assert(explanation.title.contains("d4"), clue(explanation.title))
    assert(explanation.prose.contains("asymmetrical"), clue(explanation.prose))
    assert(explanation.prose.contains("extra space"), clue(explanation.prose))
    assert(explanation.reasonTags.contains("opening_idea"), clue(explanation.reasonTags))
  }

  test("castling gets a king-safety explanation inside the same basic lane") {
    val explanation =
      BasicMoveExplanationBuilder.build(
        openingCtx(
          fen = "r1bqkb1r/pppp1ppp/2n2n2/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4",
          playedMove = "e1g1",
          playedSan = "O-O",
          opening = italianOpening,
          ply = 7,
          phaseReason = "Italian Game king safety"
        ),
        None
      ).get

    assert(explanation.title.toLowerCase.contains("king safety"), clue(explanation.title))
    assert(explanation.prose.toLowerCase.contains("king safety"), clue(explanation.prose))
    assert(explanation.reasonTags.contains("king_safety"), clue(explanation.reasonTags))
  }

  test("Italian Bc4 Nf6 d4 explains the delayed central break") {
    val explanation =
      BasicMoveExplanationBuilder.build(
        italianCtx,
        Some(refsForLine(italianBeforeBc4, List("f1c4", "g8f6", "d2d4"), List("Bc4", "Nf6", "d4")))
      ).get

    assertEquals(explanation.pvInterpretation.map(_.linePurpose), Some("center_break_setup"), clue(explanation.pvInterpretation))
    assertEquals(explanation.pvInterpretation.map(_.tension), Some("delayed_center_break"), clue(explanation.pvInterpretation))
    assert(explanation.pvInterpretation.exists(_.learningPoint.contains("d4")), clue(explanation.pvInterpretation))
    assert(explanation.prose.contains("d4"), clue(explanation.prose))
  }

  test("Ruy Lopez Bb5 a6 Ba4 reads the reply as asking for a bishop commitment") {
    val ctx =
      openingCtx(
        fen = italianBeforeBc4,
        playedMove = "f1b5",
        playedSan = "Bb5",
        opening = openingRef("Ruy Lopez", "C60", "f1b5", "Bb5"),
        ply = 5,
        phaseReason = "Ruy Lopez pressure on the e5 defender"
      )
    val explanation =
      BasicMoveExplanationBuilder.build(
        ctx,
        Some(refsForLine(italianBeforeBc4, List("f1b5", "a7a6", "b5a4"), List("Bb5", "a6", "Ba4")))
      ).get

    assertEquals(explanation.pvInterpretation.flatMap(_.opponentReplyMeaning), Some("asks_piece_commitment"), clue(explanation.pvInterpretation))
    assert(explanation.pvInterpretation.exists(_.confirms.contains("opening_idea")), clue(explanation.pvInterpretation))
    assert(explanation.pvInterpretation.exists(_.learningPoint.contains("e5")), clue(explanation.pvInterpretation))
    assert(explanation.pvInterpretation.exists(_.learningPoint.contains("c6")), clue(explanation.pvInterpretation))
  }

  test("Queen's Gambit c4 e6 Nc3 keeps d5 tension in the PV interpretation") {
    val fen = "rnbqkbnr/ppp1pppp/8/3p4/3P4/8/PPP1PPPP/RNBQKBNR w KQkq - 0 2"
    val explanation =
      BasicMoveExplanationBuilder.build(
        openingCtx(
          fen = fen,
          playedMove = "c2c4",
          playedSan = "c4",
          opening = openingRef("Queen's Gambit", "D06", "c2c4", "c4"),
          ply = 3,
          phaseReason = "Queen's Gambit central tension"
        ),
        Some(refsForLine(fen, List("c2c4", "e7e6", "b1c3"), List("c4", "e6", "Nc3")))
      ).get

    assert(Set("challenge_center", "center_break_setup").contains(explanation.pvInterpretation.map(_.linePurpose).getOrElse("")), clue(explanation.pvInterpretation))
    assert(explanation.pvInterpretation.exists(_.learningPoint.contains("d5")), clue(explanation.pvInterpretation))
  }

  test("Sicilian c5 Nf3 d6 explains the d4 question without opening broad strategy") {
    val fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1"
    val explanation =
      BasicMoveExplanationBuilder.build(
        openingCtx(
          fen = fen,
          playedMove = "c7c5",
          playedSan = "c5",
          opening = openingRef("Sicilian Defense", "B20", "c7c5", "c5"),
          ply = 2,
          phaseReason = "Sicilian flank challenge"
        ),
        Some(refsForLine(fen, List("c7c5", "g1f3", "d7d6"), List("c5", "Nf3", "d6")))
      ).get

    assertEquals(explanation.pvInterpretation.map(_.linePurpose), Some("challenge_center"), clue(explanation.pvInterpretation))
    assertEquals(explanation.pvInterpretation.map(_.tension), Some("tension_maintained"), clue(explanation.pvInterpretation))
    assert(explanation.pvInterpretation.exists(_.learningPoint.contains("d4")), clue(explanation.pvInterpretation))
  }

  test("castling PV explains king safety before the center opens") {
    val fen = "r1bqkb1r/pppp1ppp/2n2n2/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4"
    val explanation =
      BasicMoveExplanationBuilder.build(
        openingCtx(
          fen = fen,
          playedMove = "e1g1",
          playedSan = "O-O",
          opening = italianOpening,
          ply = 7,
          phaseReason = "Italian Game king safety"
        ),
        Some(refsForLine(fen, List("e1g1", "f8c5", "d2d3"), List("O-O", "Bc5", "d3")))
      ).get

    assertEquals(explanation.pvInterpretation.map(_.linePurpose), Some("king_safety_first"), clue(explanation.pvInterpretation))
    assert(explanation.pvInterpretation.exists(_.learningPoint.toLowerCase.contains("king safety")), clue(explanation.pvInterpretation))
    assert(explanation.pvInterpretation.exists(_.learningPoint.toLowerCase.contains("center")), clue(explanation.pvInterpretation))
  }

  test("opening catalog tranche patterns are requirement-backed") {
    val sicilianAfterNf3 = "rnbqkbnr/pp1ppppp/8/2p5/4P3/5N2/PPPP1PPP/RNBQKB1R b KQkq - 1 2"
    val frenchCenter = "rnbqkbnr/ppp2ppp/4p3/3p4/3PP3/8/PPP2PPP/RNBQKBNR w KQkq - 0 3"
    val caroCenter = "rnbqkbnr/pp2pppp/2p5/3p4/3PP3/8/PPP2PPP/RNBQKBNR w KQkq - 0 3"
    val cases = List(
      ("Italian quiet", italianBeforeBc4, "f1c4", "Bc4", "Italian Game", Some(refsForLine(italianBeforeBc4, List("f1c4", "g8f6", "d2d3"), List("Bc4", "Nf6", "d3"))), "quiet_d3_setup"),
      ("Italian break", italianBeforeBc4, "f1c4", "Bc4", "Italian Game", Some(refsForLine(italianBeforeBc4, List("f1c4", "g8f6", "d2d4"), List("Bc4", "Nf6", "d4"))), "c3_d4_center_break"),
      ("Italian general", italianBeforeBc4, "f1c4", "Bc4", "Italian Game", None, "bishop_c4_f7_d5"),
      ("Ruy pressure", italianBeforeBc4, "f1b5", "Bb5", "Ruy Lopez", None, "bishop_b5_c6_e5"),
      ("Ruy a6", italianBeforeBc4, "f1b5", "Bb5", "Ruy Lopez", Some(refsForLine(italianBeforeBc4, List("f1b5", "a7a6", "b5a4"), List("Bb5", "a6", "Ba4"))), "a6_Ba4_maintains_pressure"),
      ("Ruy castle", "r1bqkb1r/pppp1ppp/2n2n2/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4", "e1g1", "O-O", "Ruy Lopez", None, "Nf6_OO_king_safety_before_center"),
      ("Queen c4", "rnbqkbnr/ppp1pppp/8/3p4/3P4/8/PPP1PPPP/RNBQKBNR w KQkq - 0 2", "c2c4", "c4", "Queen's Gambit", None, "c4_d5_tension"),
      ("Queen tension PV", "rnbqkbnr/ppp1pppp/8/3p4/3P4/8/PPP1PPPP/RNBQKBNR w KQkq - 0 2", "c2c4", "c4", "Queen's Gambit", Some(refsForLine("rnbqkbnr/ppp1pppp/8/3p4/3P4/8/PPP1PPPP/RNBQKBNR w KQkq - 0 2", List("c2c4", "e7e6", "b1c3"), List("c4", "e6", "Nc3"))), "exchange_vs_tension"),
      ("Queen develop", "rnbqkbnr/ppp1pppp/8/3p4/2PP4/8/PP2PPPP/RNBQKBNR w KQkq - 0 3", "b1c3", "Nc3", "Queen's Gambit", None, "Nc3_Nf3_e3_development_around_d5"),
      ("Sicilian c5", "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1", "c7c5", "c5", "Sicilian Defense", None, "c5_d4_counter_center"),
      ("Sicilian Nf3", "rnbqkbnr/pp1ppppp/8/2p5/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2", "g1f3", "Nf3", "Sicilian Defense", None, "Nf3_prepares_d4"),
      ("Sicilian d4", "rnbqkbnr/pp1ppppp/8/2p5/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 1 2", "d2d4", "d4", "Sicilian Defense", Some(refsForLine("rnbqkbnr/pp1ppppp/8/2p5/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 1 2", List("d2d4", "c5d4", "f3d4"), List("d4", "cxd4", "Nxd4"))), "d4_cxd4_Nxd4_open_sicilian"),
      ("Sicilian d6", sicilianAfterNf3, "d7d6", "d6", "Sicilian Defense", None, "d6_center_choice"),
      ("Sicilian Nc6", sicilianAfterNf3, "b8c6", "Nc6", "Sicilian Defense", None, "Nc6_center_choice"),
      ("Sicilian e6", sicilianAfterNf3, "e7e6", "e6", "Sicilian Defense", None, "e6_center_choice"),
      ("French e6", "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1", "e7e6", "e6", "French Defense", None, "e6_d5_chain"),
      ("French advance", frenchCenter, "e4e5", "e5", "French Defense", None, "advance_structure"),
      ("French exchange", frenchCenter, "e4d5", "exd5", "French Defense", None, "exchange_structure"),
      ("Caro c6", "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1", "c7c6", "c6", "Caro-Kann Defense", None, "c6_d5_support"),
      ("Caro d5", "rnbqkbnr/pp1ppppp/2p5/8/3PP3/8/PPP2PPP/RNBQKBNR b KQkq - 0 2", "d7d5", "d5", "Caro-Kann Defense", None, "solid_center"),
      ("Caro exchange", caroCenter, "e4d5", "exd5", "Caro-Kann Defense", None, "exchange_structure")
    )

    cases.foreach { case (label, fen, uci, san, opening, refs, expected) =>
      assertEquals(catalogPattern(fen, uci, san, opening, refs), Some(expected), clue(label))
      assertNotEquals(catalogPattern(fen, uci, san, "Unrelated Opening", refs), Some(expected), clue(label))
    }
    assertNotEquals(
      catalogPattern(italianBeforeBc4, "f1c4", "Bc4", "Italian Game", None),
      Some("quiet_d3_setup")
    )
  }

  test("PV interpretation is omitted when the line is not coupled to the played move") {
    val explanation =
      BasicMoveExplanationBuilder.build(
        italianCtx,
        Some(refsForLine(italianBeforeBc4, List("f1b5", "g8f6", "d2d3"), List("Bb5", "Nf6", "d3")))
      ).get

    assertEquals(explanation.pvInterpretation, None, clue(explanation))
    assertEquals(explanation.shortLine.map(_.san), Some(List("Bb5", "Nf6", "d3")), clue(explanation.shortLine))
  }

  test("PV interpretation is omitted for empty or missing-fen lines without blocking the basic lane") {
    val emptyLine =
      MoveReviewRefs(
        startFen = italianBeforeBc4,
        startPly = 5,
        variations = List(MoveReviewVariationRef("line_empty", 12, None, 16, Nil))
      )
    val emptyExplanation = BasicMoveExplanationBuilder.build(italianCtx, Some(emptyLine)).get

    assertEquals(emptyExplanation.pvInterpretation, None, clue(emptyExplanation))
    assertEquals(emptyExplanation.shortLine, None, clue(emptyExplanation))

    val lineWithMissingFen =
      refsForLine(italianBeforeBc4, List("f1c4", "g8f6", "d2d3"), List("Bc4", "Nf6", "d3"))
    val missingFen =
      lineWithMissingFen.copy(
        variations = lineWithMissingFen.variations.map { line =>
          line.copy(moves = line.moves.updated(0, line.moves.head.copy(fenAfter = "")))
        }
      )
    val missingFenExplanation = BasicMoveExplanationBuilder.build(italianCtx, Some(missingFen)).get

    assertEquals(missingFenExplanation.pvInterpretation, None, clue(missingFenExplanation))
    assertEquals(missingFenExplanation.shortLine.map(_.san), Some(List("Bc4", "Nf6", "d3")), clue(missingFenExplanation.shortLine))
  }

  test("PV interpretation is omitted for mismatched fenAfter or mismatched refs startFen while shortLine stays visible") {
    val corruptedMove =
      refsWithShortLine.variations.head.moves(1).copy(fenAfter = refsWithShortLine.variations.head.moves.head.fenAfter)
    val mismatchedFenRefs =
      refsWithShortLine.copy(
        variations = List(
          refsWithShortLine.variations.head.copy(
            moves = refsWithShortLine.variations.head.moves.updated(1, corruptedMove)
          )
        )
      )
    val mismatchedStartRefs =
      refsWithShortLine.copy(startFen = refsWithShortLine.variations.head.moves.head.fenAfter)

    val corruptedExplanation = BasicMoveExplanationBuilder.build(italianCtx, Some(mismatchedFenRefs)).get
    val startMismatchExplanation = BasicMoveExplanationBuilder.build(italianCtx, Some(mismatchedStartRefs)).get

    assertEquals(corruptedExplanation.pvInterpretation, None, clue(corruptedExplanation))
    assertEquals(corruptedExplanation.shortLine.map(_.san), Some(List("Bc4", "Nf6", "d3")), clue(corruptedExplanation.shortLine))
    assertEquals(startMismatchExplanation.pvInterpretation, None, clue(startMismatchExplanation))
    assertEquals(startMismatchExplanation.shortLine.map(_.san), Some(List("Bc4", "Nf6", "d3")), clue(startMismatchExplanation.shortLine))
  }

  test("illegal current move creates no basic move explanation") {
    val illegal =
      BasicMoveExplanationBuilder.build(
        openingCtx(
          fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
          playedMove = "e2e5",
          playedSan = "e5",
          opening = openingRef("King's Pawn Game", "C20", "e2e5", "e5"),
          ply = 1,
          phaseReason = "illegal pawn jump"
        ),
        None
      )

    assertEquals(illegal, None, clue(illegal))
  }

  test("PV-backed middlegame pawn capture explains recapture tension outside opening") {
    val fen = "4k3/8/2p5/3p4/4P3/8/8/4K3 w - - 0 1"
    val maybeExplanation =
      BasicMoveExplanationBuilder.build(
        generalCtx(fen, "e4d5", "exd5", phaseReason = "capture tension"),
        Some(refsForLine(fen, List("e4d5", "c6d5", "e1d2"), List("exd5", "cxd5", "Kd2")))
      )
    assert(maybeExplanation.nonEmpty, clue(maybeExplanation))
    val explanation = maybeExplanation.get

    assertEquals(explanation.pvInterpretation.map(_.linePurpose), Some("resolve_capture_tension"), clue(explanation))
    assert(explanation.pvInterpretation.exists(_.confirms.contains("capture_tension")), clue(explanation.pvInterpretation))
    assert(explanation.pvInterpretation.exists(_.learningPoint.contains("cxd5")), clue(explanation.pvInterpretation))
    assert(explanation.prose.contains("recapture"), clue(explanation.prose))
  }

  test("PV-backed direct threat explains the opponent answer outside opening") {
    val fen = "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2"
    val maybeExplanation =
      BasicMoveExplanationBuilder.build(
        generalCtx(fen, "d1h5", "Qh5", phaseReason = "direct queen target"),
        Some(refsForLine(fen, List("d1h5", "g7g6", "h5e5"), List("Qh5", "g6", "Qxe5+")))
      )
    assert(maybeExplanation.nonEmpty, clue(maybeExplanation))
    val explanation = maybeExplanation.get

    assertEquals(explanation.pvInterpretation.map(_.linePurpose), Some("answer_direct_threat"), clue(explanation))
    assert(explanation.pvInterpretation.exists(_.confirms.contains("direct_threat")), clue(explanation.pvInterpretation))
    assert(explanation.pvInterpretation.exists(_.learningPoint.contains("Qh5")), clue(explanation.pvInterpretation))
    assert(explanation.pvInterpretation.exists(_.learningPoint.contains("g6")), clue(explanation.pvInterpretation))
  }

  test("PV-backed piece exchange clarifies the exchange outside opening") {
    val fen = "4k3/8/4p3/3n4/8/2N5/3P4/4K3 w - - 0 1"
    val maybeExplanation =
      BasicMoveExplanationBuilder.build(
        generalCtx(fen, "c3d5", "Nxd5", phaseReason = "piece exchange clarification"),
        Some(refsForLine(fen, List("c3d5", "e6d5", "d2d4"), List("Nxd5", "exd5", "d4")))
      )
    assert(maybeExplanation.nonEmpty, clue(maybeExplanation))
    val explanation = maybeExplanation.get

    assertEquals(explanation.pvInterpretation.map(_.linePurpose), Some("clarify_exchange"), clue(explanation))
    assert(explanation.pvInterpretation.exists(_.confirms.contains("exchange_clarified")), clue(explanation.pvInterpretation))
    assert(explanation.pvInterpretation.exists(_.learningPoint.contains("exd5")), clue(explanation.pvInterpretation))
  }

  test("PV-backed endgame king move explains activity outside opening") {
    val fen = "8/8/8/8/8/8/4P3/4K2k w - - 0 1"
    val maybeExplanation =
      BasicMoveExplanationBuilder.build(
        generalCtx(fen, "e1d2", "Kd2", phase = "Endgame", ply = 60, phaseReason = "king and pawn activity"),
        Some(refsForLine(fen, List("e1d2", "h1g2", "e2e4"), List("Kd2", "Kg2", "e4")))
      )
    assert(maybeExplanation.nonEmpty, clue(maybeExplanation))
    val explanation = maybeExplanation.get

    assertEquals(explanation.pvInterpretation.map(_.linePurpose), Some("improve_endgame_activity"), clue(explanation))
    assertEquals(explanation.source, "endgame_idea", clue(explanation))
    assert(explanation.pvInterpretation.exists(_.confirms.contains("endgame_activity")), clue(explanation.pvInterpretation))
    assert(explanation.pvInterpretation.exists(_.learningPoint.toLowerCase.contains("king activity")), clue(explanation.pvInterpretation))
    assert(explanation.pvInterpretation.exists(_.learningPoint.contains("e4")), clue(explanation.pvInterpretation))
  }

  test("PV-backed passed-pawn support is admitted as an exact endgame catalog idea") {
    val fen = "8/8/8/8/4P3/8/4K3/7k w - - 0 1"
    val explanation =
      BasicMoveExplanationBuilder.build(
        generalCtx(fen, "e2d3", "Kd3", phase = "Endgame", ply = 60, phaseReason = "king supports passer"),
        Some(refsForLine(fen, List("e2d3", "h1g2", "e4e5"), List("Kd3", "Kg2", "e5")))
      ).get

    assertEquals(explanation.source, "endgame_idea", clue(explanation))
    assert(explanation.title.toLowerCase.contains("passed pawn"), clue(explanation.title))
    assert(explanation.pvInterpretation.exists(_.confirms.contains("passed_pawn_support")), clue(explanation.pvInterpretation))
  }

  test("PV-backed rook-behind-passer is admitted as an exact endgame catalog idea") {
    val fen = "7k/8/8/P7/8/8/4K3/7R w - - 0 1"
    val explanation =
      BasicMoveExplanationBuilder.build(
        generalCtx(fen, "h1a1", "Ra1", phase = "Endgame", ply = 70, phaseReason = "rook supports passer"),
        Some(refsForLine(fen, List("h1a1", "h8g7", "a5a6"), List("Ra1", "Kg7", "a6")))
      ).get

    assertEquals(explanation.source, "endgame_idea", clue(explanation))
    assert(explanation.title.toLowerCase.contains("rook"), clue(explanation.title))
    assert(explanation.pvInterpretation.exists(_.confirms.contains("rook_behind_passer")), clue(explanation.pvInterpretation))
  }

  test("outside opening basic lane stays closed without UCI-coupled PV semantics") {
    val fen = "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2"

    assertEquals(
      BasicMoveExplanationBuilder.build(generalCtx(fen, "d1h5", "Qh5", phaseReason = "direct queen target"), None),
      None
    )
    assertEquals(
      BasicMoveExplanationBuilder.build(
        generalCtx(fen, "d1h5", "Qh5", phaseReason = "direct queen target"),
        Some(refsForLine(fen, List("d1e2", "g8f6"), List("Qh5", "Nf6")))
      ),
      None
    )
  }

  test("outside opening direct threat needs a PV reply that addresses the moved piece") {
    val fen = "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2"
    val explanation =
      BasicMoveExplanationBuilder.build(
        generalCtx(fen, "d1h5", "Qh5", phaseReason = "direct queen target"),
        Some(refsForLine(fen, List("d1h5", "b8c6", "g1f3"), List("Qh5", "Nc6", "Nf3")))
      )

    assertEquals(explanation, None, clue(explanation))
  }

  test("outside opening endgame activity needs a continuation that shows the activity") {
    val fen = "8/8/8/8/8/8/4P3/4K2k w - - 0 1"
    val explanation =
      BasicMoveExplanationBuilder.build(
        generalCtx(fen, "e1d2", "Kd2", phase = "Endgame", ply = 60, phaseReason = "king and pawn activity"),
        Some(refsForLine(fen, List("e1d2", "h1g2"), List("Kd2", "Kg2")))
      )

    assertEquals(explanation, None, clue(explanation))
  }

  test("board facts do not infer a missing source piece from SAN") {
    val fenWithMissingQueen = "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNB1KBNR w KQkq - 0 2"
    val explanation =
      BasicMoveExplanationBuilder.build(
        generalCtx(fenWithMissingQueen, "d1h5", "Qh5", phaseReason = "direct queen target"),
        Some(refsForLine(fenWithMissingQueen, List("d1h5", "g7g6", "h5e5"), List("Qh5", "g6", "Qxe5+")))
      )

    assertEquals(explanation, None, clue(explanation))
  }

  test("shortLine follows the same coupled PV line as pvInterpretation") {
    val fen = "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2"
    val refs =
      MoveReviewRefs(
        startFen = fen,
        startPly = 3,
        variations = List(
          variationForLine(fen, List("g1f3", "g8f6"), List("Nf3", "Nf6"), "line_uncoupled"),
          variationForLine(fen, List("d1h5", "g7g6", "h5e5"), List("Qh5", "g6", "Qxe5+"), "line_coupled")
        )
      )
    val explanation =
      BasicMoveExplanationBuilder.build(
        generalCtx(fen, "d1h5", "Qh5", phaseReason = "direct queen target"),
        Some(refs)
      ).get

    assertEquals(explanation.pvInterpretation.flatMap(_.supportedByLineId), Some("line_coupled"), clue(explanation.pvInterpretation))
    assertEquals(explanation.shortLine.flatMap(_.lineId), Some("line_coupled"), clue(explanation.shortLine))
    assertEquals(explanation.shortLine.map(_.san), Some(List("Qh5", "g6", "Qxe5+")), clue(explanation.shortLine))
  }

  test("basic move explanation is admitted before exact factual fallback") {
    val outline = BookStyleRenderer.validatedOutline(italianCtx)
    val slots = BookmakerPolishSlotsBuilder.buildOrFallback(
      italianCtx,
      outline,
      refs = Some(refsWithShortLine),
      strategyPack = None,
      truthContract = None
    )

    assert(slots.claim.contains("Italian"), clue(slots.claim))
    assertEquals(slots.sourceKind, "basic_move_explanation", clue(slots))
    assert(slots.claim.contains("f7"), clue(slots.claim))
    assert(slots.claim.contains("d5"), clue(slots.claim))
    assertNotEquals(
      BookmakerProseContract.stripMoveHeader(slots.claim),
      "This puts the bishop on c4.",
      clue(slots.claim)
    )
  }
