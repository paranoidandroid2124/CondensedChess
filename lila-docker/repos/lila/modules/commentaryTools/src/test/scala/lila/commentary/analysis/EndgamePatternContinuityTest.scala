package lila.commentary.analysis

import munit.FunSuite
import lila.commentary.model.*
import lila.commentary.model.strategic.EndgamePatternState

class EndgamePatternContinuityTest extends FunSuite:

  // From goldset Lucena positive.
  private val lucenaFen = "2K5/2P1k3/8/8/8/8/7r/R7 w - - 0 1"
  // From goldset Philidor positive: represents a barrier-rank rook setup after Lucena is lost.
  private val lucenaBrokenFen = "8/4k3/r7/4PK2/8/8/8/R7 b - - 0 1"
  private val philidorFen = "8/4k3/r7/4PK2/8/8/8/R7 b - - 0 1"
  private val philidorBrokenFen = "8/4k3/8/4PK2/8/8/8/R3r3 b - - 0 1"
  private val vancuraFen = "6k1/7R/PKr5/8/8/8/8/8 w - - 0 1"
  private val vancuraBrokenFen = "6k1/2r4R/1KP5/8/8/8/8/8 b - - 0 1"
  private val passiveRookFenBlackToMove = "6k1/8/8/8/8/3p4/3r2K1/R7 b - - 0 1"
  private val passiveRookFenWhiteToMove = "6k1/8/8/8/8/3p4/3r2K1/R7 w - - 0 1"
  private val syntheticFen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1"

  private def analyzeAt(
      fen: String,
      ply: Int,
      prevState: Option[EndgamePatternState]
  ): (ExtendedAnalysisData, Option[EndgamePatternState]) =
    val dataOpt = CommentaryEngine.assessExtended(
      fen = fen,
      variations = Nil,
      playedMove = None,
      opening = None,
      phase = Some("endgame"),
      ply = ply,
      prevMove = None,
      prevEndgameState = prevState,
      probeResults = Nil
    )
    assert(dataOpt.nonEmpty, s"assessExtended should return data at ply=$ply")
    val data = dataOpt.get
    val nextState = EndgamePatternState.evolve(prevState, data.endgameFeatures, ply)
    (data, nextState)

  private def syntheticContext(
      pattern: Option[String],
      patternAge: Int = 0,
      transition: Option[String] = None,
      outcome: String = "Draw",
      fen: String
  ): NarrativeContext =
    NarrativeContext(
      fen = fen,
      header = ContextHeader("Endgame", "Normal", "StyleChoice", "Low", "ExplainConvert"),
      ply = 80,
      summary = NarrativeSummary("Endgame Technique", None, "StyleChoice", "Maintain", "0.00"),
      threats = ThreatTable(Nil, Nil),
      pawnPlay = PawnPlayTable(false, None, "Low", "Maintain", "Quiet", "Background", None, false, "quiet"),
      plans = PlanTable(Nil, Nil),
      delta = None,
      phase = PhaseContext("Endgame", "Technical ending"),
      candidates = Nil,
      semantic = Some(
        SemanticSection(
          structuralWeaknesses = Nil,
          pieceActivity = Nil,
          positionalFeatures = Nil,
          compensation = None,
          endgameFeatures = Some(
            EndgameInfo(
              hasOpposition = false,
              isZugzwang = false,
              keySquaresControlled = Nil,
              theoreticalOutcomeHint = outcome,
              confidence = 0.85,
              primaryPattern = pattern,
              patternAge = patternAge,
              transition = transition
            )
          ),
          practicalAssessment = None,
          preventedPlans = Nil,
          conceptSummary = Nil
        )
      )
    )

  private def goldsetFen(pattern: String, positive: Boolean): Option[String] =
    EndgamePatternGoldsetSupport.rows
      .find(_.pattern.equalsIgnoreCase(pattern))
      .flatMap(row =>
        row.cases
          .find(c => (c.expectedLabel == row.pattern) == positive)
          .map(_.fen)
      )

  test("Lucena continuity ages across ply gaps and emits dissolution transition") {
    val (m38, s38) = analyzeAt(lucenaFen, 38, None)
    assertEquals(m38.endgamePatternAge, 0)
    assertEquals(m38.endgameFeatures.flatMap(_.primaryPattern), Some("Lucena"))
    assertEquals(m38.endgameFeatures.map(_.theoreticalOutcomeHint.toString), Some("Win"))
    assertEquals(m38.endgameTransition, None)
    assert(!m38.conceptSummary.exists(_.toLowerCase.contains("lucena")), clue(m38.conceptSummary))

    val (m40, s40) = analyzeAt(lucenaFen, 40, s38)
    assertEquals(m40.endgamePatternAge, 2)
    assertEquals(m40.endgameFeatures.flatMap(_.primaryPattern), Some("Lucena"))
    assertEquals(m40.endgameTransition, None)

    val (m44, s44) = analyzeAt(lucenaFen, 44, s40)
    assertEquals(m44.endgamePatternAge, 6)
    assertEquals(m44.endgameFeatures.flatMap(_.primaryPattern), Some("Lucena"))
    assertEquals(m44.endgameTransition, None)

    val (m48, _) = analyzeAt(lucenaBrokenFen, 48, s44)
    assertEquals(m48.endgamePatternAge, 0)
    assertEquals(m48.endgameFeatures.flatMap(_.primaryPattern), Some("PhilidorDefense"))
    assertEquals(m48.endgameFeatures.map(_.theoreticalOutcomeHint.toString), Some("Draw"))
    assertEquals(m48.endgameTransition, Some("Lucena(Unclear) → PhilidorDefense(Unclear)"))
  }

  test("Book prose surfaces sustained endgame patterns and transitions") {
    val (m38, s38) = analyzeAt(lucenaFen, 38, None)
    val (m40, s40) = analyzeAt(lucenaFen, 40, s38)
    val (m44, s44) = analyzeAt(lucenaFen, 44, s40)
    val (m48, _) = analyzeAt(lucenaBrokenFen, 48, s44)

    val sustainedCtx = NarrativeContextBuilder.build(m40, m40.toContext, Some(m38))
    val sustainedEndgame = sustainedCtx.semantic.flatMap(_.endgameFeatures).getOrElse(fail("missing sustained endgame semantic"))
    assertEquals(sustainedEndgame.primaryPattern, Some("Lucena"))
    assertEquals(sustainedEndgame.patternAge, 2)
    assertEquals(sustainedEndgame.transition, None)
    val (sustainedOutline, _) = NarrativeOutlineBuilder.build(sustainedCtx, new TraceRecorder())
    val sustainedOutlineText = sustainedOutline.beats.map(_.text).mkString("\n")
    assert(
      sustainedOutlineText.contains("bridge-building rook-pawn shape has stayed visible for 2 plies"),
      s"expected sustained rook-pawn continuity outline text, got: $sustainedOutlineText"
    )
    val sustainedText = BookStyleRenderer.render(sustainedCtx)
    assert(
      sustainedText.contains("bridge-building rook-pawn shape has stayed visible for 2 plies"),
      s"expected sustained rook-pawn continuity text, got: $sustainedText"
    )
    assert(
      sustainedText.contains("keeping that endgame context in view"),
      s"expected sustained endgame-context text, got: $sustainedText"
    )

    val transitionCtx = NarrativeContextBuilder.build(m48, m48.toContext, Some(m44))
    val transitionEndgame = transitionCtx.semantic.flatMap(_.endgameFeatures).getOrElse(fail("missing transition endgame semantic"))
    assertEquals(transitionEndgame.primaryPattern, Some("PhilidorDefense"))
    assertEquals(transitionEndgame.transition, Some("Lucena(Unclear) → PhilidorDefense(Unclear)"))
    val (transitionOutline, _) = NarrativeOutlineBuilder.build(transitionCtx, new TraceRecorder())
    val transitionOutlineText = transitionOutline.beats.map(_.text).mkString("\n")
    assert(
      transitionOutlineText.contains("shifted from bridge-building rook-pawn shape to barrier-rank rook defense"),
      s"expected rook-pawn -> barrier-rank outline transition text, got: $transitionOutlineText"
    )
    val transitionText = BookStyleRenderer.render(transitionCtx)
    assert(
      transitionText.contains("shifted from bridge-building rook-pawn shape to barrier-rank rook defense"),
      s"expected rook-pawn -> barrier-rank transition text, got: $transitionText"
    )
    assert(
      transitionText.contains("endgame structure has shifted from bridge-building rook-pawn shape to barrier-rank rook defense"),
      s"expected transition structure text, got: $transitionText"
    )
    assert(
      transitionText.contains("bridge-building rook-pawn geometry has loosened because the stronger king is no longer beside the promotion square"),
      s"expected rook-pawn causal loss text, got: $transitionText"
    )
    assert(
      transitionText.contains("barrier-rank rook defense is now visible because the rook still guards the barrier rank"),
      s"expected barrier-rank causal hold text, got: $transitionText"
    )
  }

  test("Book prose surfaces rook-endgame breakdown causes without theorem labels") {
    val (philidorStable, philidorState) = analyzeAt(philidorFen, 60, None)
    assertEquals(philidorStable.endgameFeatures.flatMap(_.primaryPattern), Some("PhilidorDefense"))
    val (philidorBroken, _) = analyzeAt(philidorBrokenFen, 62, philidorState)
    assert(philidorBroken.endgameTransition.exists(_.startsWith("PhilidorDefense(Unclear) → none(")))

    val philidorCtx = NarrativeContextBuilder.build(philidorBroken, philidorBroken.toContext, Some(philidorStable))
    val philidorText = BookStyleRenderer.render(philidorCtx)
    assert(
      philidorText.contains("barrier-rank rook defense has loosened because the defending rook has left the barrier rank"),
      s"expected barrier-rank loss-cause text, got: $philidorText"
    )

    val (vancuraStable, vancuraState) = analyzeAt(vancuraFen, 70, None)
    assertEquals(vancuraStable.endgameFeatures.flatMap(_.primaryPattern), Some("VancuraDefense"))
    val (vancuraBroken, _) = analyzeAt(vancuraBrokenFen, 72, vancuraState)
    assert(vancuraBroken.endgameTransition.exists(_.startsWith("VancuraDefense(Unclear) → none(")))

    val vancuraCtx = NarrativeContextBuilder.build(vancuraBroken, vancuraBroken.toContext, Some(vancuraStable))
    val vancuraText = BookStyleRenderer.render(vancuraCtx)
    assert(
      vancuraText.contains("side-checking rook defense has loosened because") &&
        (vancuraText.contains("pawn's rank") || vancuraText.contains("side-checking formation")),
      s"expected side-checking loss-cause text, got: $vancuraText"
    )
  }

  test("objective endgame patterns keep continuity across side-to-move flips") {
    val (m70, s70) = analyzeAt(passiveRookFenBlackToMove, 70, None)
    assertEquals(m70.endgameFeatures.flatMap(_.primaryPattern), Some("PassiveRookDefense"))
    assertEquals(m70.endgamePatternAge, 0)

    val (m71, _) = analyzeAt(passiveRookFenWhiteToMove, 71, s70)
    assertEquals(m71.endgameFeatures.flatMap(_.primaryPattern), Some("PassiveRookDefense"))
    assertEquals(m71.endgamePatternAge, 1)
    assertEquals(m71.endgameTransition, None)
  }

  test("Remaining DB patterns emit draft hold and loss causality prose") {
    val holdCases = List(
      "WrongRookPawnWrongBishopFortress" -> "promotion corner",
      "OutsidePasserDecoy" -> "remote passer",
      "ConnectedPassers" -> "advance together",
      "KeySquaresOppositionBreakthrough" -> "critical entry squares",
      "TriangulationZugzwang" -> "spare king tempo",
      "BreakthroughSacrifice" -> "points at a passer",
      "Shouldering" -> "off the pawn's route",
      "RetiManeuver" -> "chase the passer",
      "ShortSideDefense" -> "checking room on the short side",
      "OppositeColoredBishopsDraw" -> "different color complex",
      "GoodBishopRookPawnConversion" -> "bishop matches the promotion corner",
      "KnightBlockadeRookPawnDraw" -> "covers the promotion square",
      "QueenVsAdvancedPawn" -> "checking distance against the advanced pawn",
      "TarraschDefenseActive" -> "active behind the pawn",
      "PassiveRookDefense" -> "behind the pawn",
      "RookAndBishopVsRookDraw" -> "checking distance or corner geometry",
      "SameColoredBishopsBlockade" -> "shared color complex"
    )

    val lossCases = List(
      "WrongRookPawnWrongBishopFortress" -> "promotion corner",
      "OutsidePasserDecoy" -> "drags the enemy king away",
      "ConnectedPassers" -> "advancing together",
      "KeySquaresOppositionBreakthrough" -> "entry squares",
      "TriangulationZugzwang" -> "spare king tempo",
      "BreakthroughSacrifice" -> "passer-route cue",
      "Shouldering" -> "pushed off the pawn's path",
      "RetiManeuver" -> "combine pursuit of the passer",
      "ShortSideDefense" -> "checking distance",
      "OppositeColoredBishopsDraw" -> "bishop's color complex",
      "GoodBishopRookPawnConversion" -> "promotion corner",
      "KnightBlockadeRookPawnDraw" -> "promotion square",
      "QueenVsAdvancedPawn" -> "advanced pawn",
      "TarraschDefenseActive" -> "checking from behind the pawn",
      "PassiveRookDefense" -> "behind the pawn",
      "RookAndBishopVsRookDraw" -> "checking distance or corner geometry",
      "SameColoredBishopsBlockade" -> "shared color complex"
    )

    holdCases.foreach { case (pattern, anchor) =>
      val ctx =
        syntheticContext(
          pattern = Some(pattern),
          patternAge = 4,
          fen = goldsetFen(pattern, positive = true).getOrElse(syntheticFen)
        )
      val text = BookStyleRenderer.renderDraft(ctx)
      assert(
        text.contains("because") && text.toLowerCase.contains(anchor.toLowerCase),
        s"expected hold causality for $pattern, got: $text"
      )
    }

    lossCases.foreach { case (pattern, anchor) =>
      val ctx = syntheticContext(
        pattern = None,
        transition = Some(s"$pattern(Unclear) → none(Unclear)"),
        fen = goldsetFen(pattern, positive = false).getOrElse(syntheticFen)
      )
      val text = BookStyleRenderer.renderDraft(ctx)
      assert(
        text.contains("because") && text.toLowerCase.contains(anchor.toLowerCase),
        s"expected loss causality for $pattern, got: $text"
      )
    }
  }
