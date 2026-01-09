package lila.llm.analysis

import lila.llm.model._
import lila.llm.model.strategic._
import chess.format.Fen
import chess.format.Uci
import chess.variant.Standard

class A9NarrativeOutputTest extends munit.FunSuite {

  test("Manual: Print rendered opening commentary for a Sample Game") {
    val pgn = "1. e4 c5 2. Nf3 d6 3. d4 cxd4 4. Nxd4 Nf6 5. Nc3 a6 6. g4"
    
    // Mock Opening Data
    val najdorfRef = OpeningReference(
      eco = Some("B99"),
      name = Some("Sicilian Najdorf"),
      totalGames = 10000,
      topMoves = List(
        ExplorerMove("e2e4", "e4", 5000, 2000, 1500, 1500, 2650),
        ExplorerMove("a2a3", "a3", 100, 40, 30, 30, 2500)
      ),
      sampleGames = Nil
    )

    // Generate accurate FENs using Utils
    val startFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    val fenAfter1 = lila.llm.analysis.NarrativeUtils.uciListToFen(startFen, List("e2e4", "c7c5"))
    val najdorfUci = List("e2e4", "c7c5", "g1f3", "d7d6", "d2d4", "c5d4", "f3d4", "g8f6", "b1c3", "a7a6")
    val fenBefore6 = lila.llm.analysis.NarrativeUtils.uciListToFen(startFen, najdorfUci)

    // Maps FEN to OpeningReference
    val openingRefs = Map(
      startFen -> najdorfRef,
      fenAfter1 -> najdorfRef,
      fenBefore6 -> najdorfRef
    )

    // Add some "blunder" eval to trigger key moments
    val evals = Map(
      11 -> List(VariationLine(moves = List("g2g4"), scoreCp = -300, mate = None, tags = Nil)) // 6. g4 is a mistake in our mock
    )

    // Generate Full Narrative
    val narrative = CommentaryEngine.generateFullGameNarrative(
      pgn = pgn,
      evals = evals,
      openingRefsByFen = openingRefs
    )

    val output = new java.lang.StringBuilder()
    output.append("\n" + "=" * 50 + "\n")
    output.append("--- GAME INTRO ---\n")
    output.append(narrative.gameIntro + "\n")
    
    output.append("\n--- KEY MOMENT NARRATIVES ---\n")
    narrative.keyMomentNarratives.foreach { moment =>
      output.append(s"\n[Ply ${moment.ply}] (${moment.momentType})\n")
      output.append("-" * 20 + "\n")
      output.append(moment.narrative + "\n")
    }
    
    output.append("\n--- CONCLUSION ---\n")
    output.append(narrative.conclusion + "\n")
    output.append("=" * 50 + "\n")

    // Write to a file instead of println to ensure capture
    java.nio.file.Files.writeString(
      java.nio.file.Path.of("a9_narrative_output.txt"),
      output.toString
    )
    println(output.toString)
  }

  test("Manual: Render diverse FEN snapshots (Opening/Middlegame/Endgame)") {
    case class Scenario(
        name: String,
        expectedPhase: Option[String],
        fen: String,
        ply: Int,
        variations: List[VariationLine] = Nil,
        openingRef: Option[OpeningReference] = None,
        playedMove: Option[String] = None
    )

    def defaultVariations(fen: String): (List[VariationLine], Option[String]) = {
      Fen.read(Standard, Fen.Full(fen)) match {
        case None => (List(VariationLine(moves = Nil, scoreCp = 0, mate = None, depth = 0)), None)
        case Some(pos) =>
          val legal = pos.legalMoves.toList.sortBy(_.toUci.uci)
          legal.headOption match {
            case None => (List(VariationLine(moves = Nil, scoreCp = 0, mate = None, depth = 0)), None)
            case Some(best) =>
              val moves = legal.take(3).map(_.toUci.uci)
              val vars = moves.zipWithIndex.map { case (uci, idx) =>
                VariationLine(moves = List(uci), scoreCp = -idx * 10, mate = None, depth = 10)
              }
              (vars, Some(best.toUci.uci))
          }
      }
    }

    def isSquareKey(s: String): Boolean =
      s.length == 2 && s.charAt(0) >= 'a' && s.charAt(0) <= 'h' && s.charAt(1) >= '1' && s.charAt(1) <= '8'

    def isFileKey(s: String): Boolean = s.length == 1 && s.charAt(0) >= 'a' && s.charAt(0) <= 'h'

    def validate(
        fen: String,
        playedMove: Option[String],
        data: ExtendedAnalysisData,
        ctx: NarrativeContext,
        rendered: String
    ): List[String] = {
      val issues = scala.collection.mutable.ListBuffer.empty[String]

      val posOpt = Fen.read(Standard, Fen.Full(fen))
      val legalUci = posOpt.map(_.legalMoves.toList.map(_.toUci.uci).toSet).getOrElse(Set.empty[String])
      val pawnCount = posOpt.map(_.board.pawns.count).getOrElse(0)

      // Threat squares should be valid coordinates when present
      (ctx.threats.toUs ++ ctx.threats.toThem).flatMap(_.square).foreach { sq =>
        if (!isSquareKey(sq)) issues += s"Invalid ThreatRow.square: '$sq'"
      }

      // Critical semantic invariant: no pawns => do not invent pawn-structure weaknesses
      if (pawnCount == 0) {
        if (data.structuralWeaknesses.nonEmpty) {
          issues += s"Pawnless board: structuralWeaknesses present (${data.structuralWeaknesses.size})"
        }

        val pawnDerivedPositional = data.positionalFeatures.exists {
          case PositionalTag.WeakSquare(_, _) => true
          case PositionalTag.Outpost(_, _) => true
          case _ => false
        }
        if (pawnDerivedPositional) {
          issues += "Pawnless board: pawn-derived positionalFeatures present (WeakSquare/Outpost)"
        }
      }

      // Targets should not carry numeric/invalid coordinates
      ctx.meta.foreach { meta =>
        (meta.targets.tactical ++ meta.targets.strategic).foreach { entry =>
          entry.ref match {
            case TargetSquare(key) if !isSquareKey(key) =>
              issues += s"Invalid TargetSquare: '$key' (${entry.reason})"
            case TargetFile(file) if !isFileKey(file) =>
              issues += s"Invalid TargetFile: '$file' (${entry.reason})"
            case TargetPiece(role, square) if !isSquareKey(square) =>
              issues += s"Invalid TargetPiece square: '$role@$square' (${entry.reason})"
            case _ => ()
          }
        }
      }

      // Candidate SAN must not claim check unless the move gives check
      ctx.candidates.foreach { c =>
        (for {
          uciStr <- c.uci
          pos <- posOpt
          uci <- Uci(uciStr)
          move <- uci match {
            case m: Uci.Move => pos.move(m).toOption
            case _: Uci.Drop => None
          }
        } yield {
          val sanClaimsCheck = c.move.contains("+") || c.move.contains("#")
          val isCheck = move.after.check.yes
          if (sanClaimsCheck != isCheck) {
            issues += s"Check marker mismatch: uci=$uciStr san='${c.move}' check=$isCheck"
          }
        }).getOrElse(())
      }

      // Threats "toThem" should not suggest our own move as their defense
      playedMove.foreach { pm =>
        ctx.threats.toThem.flatMap(_.bestDefense).foreach { d =>
          if (d == pm) issues += s"Suspicious: TO THEM bestDefense equals playedMove ($pm)"
        }
      }

      // Best defense moves should not be illegal gibberish (when provided as UCI)
      ctx.threats.toUs.flatMap(_.bestDefense).foreach { d =>
        if (Uci(d).isDefined && !legalUci.contains(d)) {
          issues += s"TO US bestDefense is not legal in current FEN: '$d'"
        }
      }

      // Duplicate threats in narrative output is usually a wiring/dedup issue
      def dups(xs: List[String]): List[String] =
        xs.groupBy(identity).collect { case (k, vs) if vs.size > 1 => k }.toList

      val dupToUs = dups(ctx.threats.toUs.map(_.toNarrative))
      if (dupToUs.nonEmpty) issues += s"Duplicate TO US threats: ${dupToUs.mkString(" | ")}"

      val dupToThem = dups(ctx.threats.toThem.map(_.toNarrative))
      if (dupToThem.nonEmpty) issues += s"Duplicate TO THEM threats: ${dupToThem.mkString(" | ")}"

      // WeakComplex "holes" exploding across the entire board is suspicious
      data.structuralWeaknesses.foreach { wc =>
        if (wc.squares.size >= 16) {
          issues += s"WeakComplex too large: ${wc.squares.size} squares (cause='${wc.cause}')"
        }
      }

      // Text-level smell: avoid '?' squares in threat summary
      if (rendered.contains(" on ?")) issues += "Rendered narrative contains '?' square placeholder"

      // 1) Candidate 비어있음 금지
      if (rendered.contains("\na) !") || rendered.contains("\nb) !") || rendered.contains("\nc) !")) {
        issues += "Empty/invalid candidate move rendering"
      }

      // 2) Mate override 검사
      val hasMate = rendered.contains("#")
      if (hasMate && rendered.contains("Tension: Ignore (quiet)")) {
        issues += "Mate present but tension marked quiet"
      }

      // 3) Endgame pawn scarcity에서 holes/weakcomplex 금지
      if (rendered.contains("=== CONTEXT [Endgame") && rendered.contains("Holes) at") && pawnCount <= 1) {
        issues += s"Pawn-scarce endgame ($pawnCount pawns) still emitting Holes list"
      }

      // 4) Rook 부재에서 Open File Control 1순위 금지
      val hasRooks = posOpt.exists(p => (p.board.rooks & p.board.occupied).nonEmpty)
      if (!hasRooks && rendered.contains("Primary Plan: Suggested Plan: Open File Control")) {
        issues += "Open File Control suggested without rooks"
      }

      // 5) 초반 오프닝에서 'Under Pressure' 과잉
      if (fen == "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1" && rendered.contains("Practical Assessment: Under Pressure")) {
        issues += "Starting position incorrectly flagged as under pressure"
      }

      issues.toList
    }

    // ------------------------------
    // Opening refs (mocked)
    // ------------------------------
    val najdorfIntroRef = OpeningReference(
      eco = Some("B99"),
      name = Some("Sicilian Najdorf"),
      totalGames = 10000,
      topMoves = List(
        ExplorerMove("e2e4", "e4", 5000, 2000, 1500, 1500, 2650),
        ExplorerMove("d2d4", "d4", 4000, 1600, 1200, 1200, 2620),
        ExplorerMove("g1f3", "Nf3", 900, 380, 260, 260, 2580)
      ),
      sampleGames = Nil
    )

    val najdorfMove6Ref = OpeningReference(
      eco = Some("B99"),
      name = Some("Sicilian Najdorf"),
      totalGames = 5000,
      topMoves = List(
        ExplorerMove("c1e3", "Be3", 1400, 560, 420, 420, 2700),
        ExplorerMove("c1g5", "Bg5", 1200, 480, 360, 360, 2680),
        ExplorerMove("f2f3", "f3", 900, 360, 270, 270, 2660),
        ExplorerMove("f1c4", "Bc4", 700, 280, 210, 210, 2650),
        ExplorerMove("h2h3", "h3", 500, 200, 150, 150, 2620)
      ),
      sampleGames = Nil
    )

    // ------------------------------
    // FENs (generated where possible)
    // ------------------------------
    val startFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

    // Najdorf after 1.e4 c5 2.Nf3 d6 3.d4 cxd4 4.Nxd4 Nf6 5.Nc3 a6 (white to move)
    val najdorfUci = List("e2e4", "c7c5", "g1f3", "d7d6", "d2d4", "c5d4", "f3d4", "g8f6", "b1c3", "a7a6")
    val najdorfFenBefore6 = lila.llm.analysis.NarrativeUtils.uciListToFen(startFen, najdorfUci)

    // Ruy Lopez setup after 1.e4 e5 2.Nf3 Nc6 (white to play Bb5)
    val ruyLopezUci = List("e2e4", "e7e5", "g1f3", "b8c6")
    val ruyLopezFenBeforeBb5 = lila.llm.analysis.NarrativeUtils.uciListToFen(startFen, ruyLopezUci)

    // Middlegame with open files (d/e)
    val openFilesFen = "r3k2r/ppp2ppp/2n2n2/3b4/3B4/2N2N2/PPP2PPP/R3K2R w KQkq - 0 10"

    // Endgame snapshots
    val kpEndgameFen = "8/8/8/4k3/8/8/4P3/4K3 w - - 0 1"
    val passedPawnEndgameFen = "8/3k4/3P4/8/8/8/8/4K3 w - - 0 1"

    val baseScenarios = List(
      Scenario(
        name = "OpeningIntro (mocked Najdorf at start position)",
        expectedPhase = Some("Opening"),
        fen = startFen,
        ply = 1,
        openingRef = Some(najdorfIntroRef)
      ),
      Scenario(
        name = "OpeningOutOfBook (Najdorf 6.g4)",
        expectedPhase = Some("Opening"),
        fen = najdorfFenBefore6,
        ply = 11,
        openingRef = Some(najdorfMove6Ref),
        playedMove = Some("g2g4"),
        variations = List(VariationLine(moves = List("g2g4"), scoreCp = -50, mate = None, depth = 18))
      ),
      Scenario(
        name = "OpeningPin motif (Ruy Lopez Bb5)",
        expectedPhase = Some("Opening"),
        fen = ruyLopezFenBeforeBb5,
        ply = 5,
        playedMove = Some("f1b5"),
        variations = List(VariationLine(moves = List("f1b5"), scoreCp = 0, mate = None, depth = 12))
      ),
      Scenario(
        name = "Middlegame (open files d/e)",
        expectedPhase = Some("Middlegame"),
        fen = openFilesFen,
        ply = 20
      ),
      Scenario(
        name = "Endgame (K+P vs K)",
        expectedPhase = Some("Endgame"),
        fen = kpEndgameFen,
        ply = 60
      ),
      Scenario(
        name = "Endgame (advanced passed pawn)",
        expectedPhase = Some("Endgame"),
        fen = passedPawnEndgameFen,
        ply = 70,
        variations = List(VariationLine(moves = Nil, scoreCp = 200, mate = None, depth = 0))
      )
    )

    // ------------------------------
    // Bulk coverage: reusing vetted verification-suite FENs
    // ------------------------------
    val coverageScenarios = List(
      // L1 structure + safety
      Scenario(
        name = "L1: IQP (isolated queen pawn)",
        expectedPhase = None,
        fen = "rnbqk2r/pp2bppp/4pn2/8/3P4/2N2N2/PP3PPP/R1BQ1RK1 w KQkq - 0 8",
        ply = 15
      ),
      Scenario(
        name = "L1: Hanging Pawns",
        expectedPhase = None,
        fen = "r2q1rk1/p4ppp/1p2pn2/8/2PP4/5N2/P4PPP/R2Q1RK1 w - - 0 14",
        ply = 27
      ),
      Scenario(
        name = "L1: Backward Pawn (d6)",
        expectedPhase = None,
        fen = "r1bqk2r/1p2bppp/p1np1n2/2p1p3/2P1P3/2N2N2/PP1PBPPP/R1BQ1RK1 w kq - 0 8",
        ply = 15
      ),
      Scenario(
        name = "L1: Locked Center (d4/e4 vs d5/e5)",
        expectedPhase = None,
        fen = "rnbqkbnr/ppp2ppp/8/3pp3/3PP3/8/PPP2PPP/RNBQKBNR w KQkq - 0 2",
        ply = 3
      ),
      Scenario(
        name = "L1: King Safety (early attack)",
        expectedPhase = None,
        fen = "rnb1k2r/pppp1ppp/5n2/4p3/2B1P2q/5N2/PPPP1bPP/RNBQK2R w KQkq - 2 5",
        ply = 9
      ),
      Scenario(
        name = "L1: Line Control (Alekhine's gun)",
        expectedPhase = None,
        fen = "2r2rk1/pp1b1ppp/1q2pn2/3p4/3P4/2N2N2/PP1QPPPP/R2R2K1 w - - 0 1",
        ply = 21
      ),
      Scenario(
        name = "L1: Opposite Castling Race",
        expectedPhase = None,
        fen = "r1r3k1/pp1bppbp/3p1np1/8/3NPPP1/2N1B3/PPPQ3P/2KR3R b - - 0 12",
        ply = 23
      ),
      Scenario(
        name = "L1: Fortress / Blockade",
        expectedPhase = None,
        fen = "r1bq1rk1/2p1bppp/p1np1n2/1p2p3/4P3/1BP2N2/PP1P1PPP/RNBQR1K1 w - - 0 9",
        ply = 17
      ),

      // L2 positional/tactical motifs (some with forced moves)
      Scenario(
        name = "L2: Open file control",
        expectedPhase = None,
        fen = "r4rk1/ppp2ppp/2n5/4p3/8/2NR1N2/PPP2PPP/3R2K1 w - - 0 1",
        ply = 21
      ),
      Scenario(
        name = "L2: Battery (Q+R aligned)",
        expectedPhase = None,
        fen = "3k4/8/8/8/8/8/3R4/3Q3K w - - 0 1",
        ply = 45
      ),
      Scenario(
        name = "L2: Weak back rank",
        expectedPhase = None,
        fen = "1R4k1/5ppp/8/8/8/8/8/6K1 w - - 0 1",
        ply = 55
      ),
      Scenario(
        name = "L2: Discovered attack (bishop move e5f6)",
        expectedPhase = Some("Endgame"),
        fen = "4k3/8/8/4B3/8/8/4R3/4K3 w - - 0 1",
        ply = 51,
        playedMove = Some("e5f6"),
        variations = List(VariationLine(moves = List("e5f6"), scoreCp = 500, mate = None, depth = 10))
      ),
      Scenario(
        name = "L2: Pin (Bg5)",
        expectedPhase = None,
        fen = "rnbqk2r/pppp1ppp/4pn2/8/1bPP4/2N5/PP2PPPP/R1BQKBNR w KQkq - 0 1",
        ply = 7,
        playedMove = Some("c1g5"),
        variations = List(VariationLine(moves = List("c1g5"), scoreCp = 80, mate = None, depth = 10))
      ),
      Scenario(
        name = "L2: Fork (Ne5d7)",
        expectedPhase = None,
        fen = "r1bqkb1r/pppp1ppp/2n2n2/4N3/4P3/8/PPPP1PPP/RNBQKB1R w KQkq - 0 1",
        ply = 9,
        playedMove = Some("e5d7"),
        variations = List(VariationLine(moves = List("e5d7"), scoreCp = 120, mate = None, depth = 10))
      ),
      Scenario(
        name = "L2: Clearance (Rd4h4)",
        expectedPhase = Some("Endgame"),
        fen = "3k4/8/8/8/3R4/8/8/3Q3K w - - 0 1",
        ply = 61,
        playedMove = Some("d4h4"),
        variations = List(VariationLine(moves = List("d4h4"), scoreCp = 300, mate = None, depth = 10))
      ),

      Scenario(
        name = "L1: Passed pawn on 7th (a7)",
        expectedPhase = Some("Endgame"),
        fen = "8/P7/8/2k5/8/5b2/4K3/8 w - - 0 1",
        ply = 65
      ),

      Scenario(
        name = "L3: Chaos tactical storm",
        expectedPhase = None,
        fen = "r1bq1rk1/pp3ppp/2n1pn2/3p4/3P4/2NBPN2/PP3PPP/R1BQ1RK1 w - - 0 9",
        ply = 19
      ),

      Scenario(
        name = "Tactic: Forced mate pattern (Re8#)",
        expectedPhase = Some("Endgame"),
        fen = "6k1/5ppp/8/8/8/8/5PPP/4R1K1 w - - 0 1",
        ply = 41,
        playedMove = Some("e1e8"),
        variations = List(VariationLine(moves = List("e1e8"), scoreCp = 0, mate = Some(1), depth = 20))
      ),

      Scenario(
        name = "NEGATIVE: Battery no target",
        expectedPhase = Some("Endgame"),
        fen = "8/8/8/k7/8/8/3R4/3Q3K w - - 0 1",
        ply = 47
      ),

      // Negative / edge cases
      Scenario(
        name = "Edge: Pawnless board (should not invent weak squares)",
        expectedPhase = Some("Endgame"),
        fen = "8/8/8/8/8/8/8/4K3 w - - 0 1",
        ply = 80
      ),
      Scenario(
        name = "Edge: Symmetric queen endgame",
        expectedPhase = Some("Endgame"),
        fen = "6k1/5ppp/8/3q4/3Q4/8/5P1P/6K1 w - - 0 1",
        ply = 90
      )
    )

    val scenarios = baseScenarios ++ coverageScenarios

    val output = new java.lang.StringBuilder()
    output.append("\n" + "=" * 50 + "\n")
    output.append(s"--- FEN COVERAGE (${scenarios.size} scenarios) ---\n")

    var issueCount = 0
    val issueSummary = scala.collection.mutable.ListBuffer.empty[(String, List[String])]

    scenarios.foreach { s =>
      val (autoVars, autoMove) = defaultVariations(s.fen)
      val variations = if (s.variations.nonEmpty) s.variations else autoVars
      val playedMove = s.playedMove.orElse(autoMove).orElse(variations.headOption.flatMap(_.moves.headOption))

      val dataOpt = CommentaryEngine.assessExtended(
        fen = s.fen,
        variations = variations,
        playedMove = playedMove,
        ply = s.ply,
        prevMove = playedMove
      )

      val data = dataOpt.getOrElse(fail(s"Failed to analyze scenario: ${s.name}"))

      val narrativeCtx = NarrativeContextBuilder.build(
        data = data,
        ctx = data.toContext,
        prevAnalysis = None,
        probeResults = Nil,
        openingRef = s.openingRef
      )

      val text = lila.llm.NarrativeGenerator.describeHierarchical(narrativeCtx)
      val issues = validate(s.fen, playedMove, data, narrativeCtx, text)
      issueCount += issues.size
      issueSummary += ((s.name, issues))

      // Basic sanity assertions: pipeline produces a full structured narrative
      s.expectedPhase.foreach { phase =>
        assertEquals(narrativeCtx.header.phase, phase, s"Phase mismatch for ${s.name}")
      }
      assert(text.contains("=== SUMMARY ==="), s"SUMMARY section missing for ${s.name}")

      if (s.openingRef.isDefined) {
        assert(text.contains("=== OPENING EVENT (MASTERS) ==="), s"OPENING EVENT section missing for ${s.name}")
      } else {
        assert(!text.contains("=== OPENING EVENT (MASTERS) ==="), s"OPENING EVENT should be absent for ${s.name}")
      }

      // Strong deterministic check: open file detection in a known middlegame FEN
      if (s.name.startsWith("Middlegame (open files")) {
        val openFiles = narrativeCtx.snapshots.head.openFiles
        assert(openFiles.contains("d"), s"d-file should be open. Got: $openFiles")
        assert(openFiles.contains("e"), s"e-file should be open. Got: $openFiles")
      }

      output.append(s"\n[${s.name}] ply=${s.ply}\n")
      output.append(s"FEN: ${s.fen}\n")
      playedMove.foreach(m => output.append(s"Played: $m\n"))
      output.append("-" * 20 + "\n")
      if (issues.nonEmpty) {
        output.append("!!! ISSUES DETECTED !!!\n")
        issues.foreach(i => output.append(s"- $i\n"))
        output.append("-" * 20 + "\n")
      }
      output.append(text + "\n")
    }

    output.append("\n" + "=" * 50 + "\n")
    output.append(s"TOTAL ISSUES: $issueCount\n")

    val failing = issueSummary.filter(_._2.nonEmpty).toList.sortBy { case (_, issues) => -issues.size }
    if (failing.nonEmpty) {
      output.append("\n--- ISSUE SUMMARY ---\n")
      failing.foreach { case (name, issues) =>
        output.append(s"* $name: ${issues.size}\n")
        issues.take(6).foreach(i => output.append(s"  - $i\n"))
        if (issues.size > 6) output.append(s"  - (+${issues.size - 6} more)\n")
      }
    }

    java.nio.file.Files.writeString(
      java.nio.file.Path.of("fen_narrative_output.txt"),
      output.toString
    )

    println(output.toString)
  }
}
