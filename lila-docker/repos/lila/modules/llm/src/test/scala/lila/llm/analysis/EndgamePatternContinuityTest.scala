package lila.llm.analysis

import munit.FunSuite
import lila.llm.model.*
import lila.llm.model.strategic.EndgamePatternState

class EndgamePatternContinuityTest extends FunSuite:

  // From goldset Lucena positive.
  private val lucenaFen = "2K5/2P1k3/8/8/8/8/7r/R7 w - - 0 1"
  // From goldset Philidor positive: represents a drawn defensive setup after Lucena is lost.
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
      outcome: String = "Draw"
  ): NarrativeContext =
    NarrativeContext(
      fen = syntheticFen,
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

  test("Lucena continuity ages across ply gaps and emits dissolution transition") {
    val (m38, s38) = analyzeAt(lucenaFen, 38, None)
    assertEquals(m38.endgamePatternAge, 0)
    assertEquals(m38.endgameFeatures.flatMap(_.primaryPattern), Some("Lucena"))
    assertEquals(m38.endgameFeatures.map(_.theoreticalOutcomeHint.toString), Some("Win"))
    assertEquals(m38.endgameTransition, None)

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
    assertEquals(m48.endgameTransition, Some("Lucena(Win) → PhilidorDefense(Draw)"))
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
      sustainedOutlineText.contains("Lucena structure has held for 2 plies"),
      s"expected sustained Lucena continuity outline text, got: $sustainedOutlineText"
    )
    val sustainedText = BookStyleRenderer.render(sustainedCtx)
    assert(
      sustainedText.contains("Lucena structure has held for 2 plies"),
      s"expected sustained Lucena continuity text, got: $sustainedText"
    )
    assert(
      sustainedText.contains("winning method remains in force"),
      s"expected sustained winning-task text, got: $sustainedText"
    )

    val transitionCtx = NarrativeContextBuilder.build(m48, m48.toContext, Some(m44))
    val transitionEndgame = transitionCtx.semantic.flatMap(_.endgameFeatures).getOrElse(fail("missing transition endgame semantic"))
    assertEquals(transitionEndgame.primaryPattern, Some("PhilidorDefense"))
    assertEquals(transitionEndgame.transition, Some("Lucena(Win) → PhilidorDefense(Draw)"))
    val (transitionOutline, _) = NarrativeOutlineBuilder.build(transitionCtx, new TraceRecorder())
    val transitionOutlineText = transitionOutline.beats.map(_.text).mkString("\n")
    assert(
      transitionOutlineText.contains("shifted from Lucena to Philidor Defense"),
      s"expected Lucena -> Philidor outline transition text, got: $transitionOutlineText"
    )
    val transitionText = BookStyleRenderer.render(transitionCtx)
    assert(
      transitionText.contains("shifted from Lucena to Philidor Defense"),
      s"expected Lucena -> Philidor transition text, got: $transitionText"
    )
    assert(
      transitionText.contains("winning method into a drawing setup"),
      s"expected transition task text, got: $transitionText"
    )
    assert(
      transitionText.contains("Lucena has broken down because the stronger king is no longer beside the promotion square"),
      s"expected Lucena causal loss text, got: $transitionText"
    )
    assert(
      transitionText.contains("Philidor now holds because the rook still guards the barrier rank"),
      s"expected Philidor causal hold text, got: $transitionText"
    )
  }

  test("Book prose surfaces Philidor and Vancura breakdown causes") {
    val (philidorStable, philidorState) = analyzeAt(philidorFen, 60, None)
    assertEquals(philidorStable.endgameFeatures.flatMap(_.primaryPattern), Some("PhilidorDefense"))
    val (philidorBroken, _) = analyzeAt(philidorBrokenFen, 62, philidorState)
    assert(philidorBroken.endgameTransition.exists(_.startsWith("PhilidorDefense(Draw) → none(")))

    val philidorCtx = NarrativeContextBuilder.build(philidorBroken, philidorBroken.toContext, Some(philidorStable))
    val philidorText = BookStyleRenderer.render(philidorCtx)
    assert(
      philidorText.contains("Philidor no longer holds because the defending rook has left the barrier rank"),
      s"expected Philidor loss-cause text, got: $philidorText"
    )

    val (vancuraStable, vancuraState) = analyzeAt(vancuraFen, 70, None)
    assertEquals(vancuraStable.endgameFeatures.flatMap(_.primaryPattern), Some("VancuraDefense"))
    val (vancuraBroken, _) = analyzeAt(vancuraBrokenFen, 72, vancuraState)
    assert(vancuraBroken.endgameTransition.exists(_.startsWith("VancuraDefense(Draw) → none(")))

    val vancuraCtx = NarrativeContextBuilder.build(vancuraBroken, vancuraBroken.toContext, Some(vancuraStable))
    val vancuraText = BookStyleRenderer.render(vancuraCtx)
    assert(
      vancuraText.contains("Vancura no longer holds because") &&
        (vancuraText.contains("pawn's rank") || vancuraText.contains("side-checking formation")),
      s"expected Vancura loss-cause text, got: $vancuraText"
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

  test("Remaining DB patterns emit hold and loss causality prose") {
    val holdCases = List(
      "WrongRookPawnWrongBishopFortress" -> "promotion corner",
      "OutsidePasserDecoy" -> "remote passer",
      "ConnectedPassers" -> "advance together",
      "KeySquaresOppositionBreakthrough" -> "critical entry squares",
      "TriangulationZugzwang" -> "spare king tempo",
      "BreakthroughSacrifice" -> "forced passer",
      "Shouldering" -> "shoved off the pawn's route",
      "RetiManeuver" -> "chase the passer",
      "ShortSideDefense" -> "checking room on the short side",
      "OppositeColoredBishopsDraw" -> "different color complexes",
      "GoodBishopRookPawnConversion" -> "bishop matches the promotion corner",
      "KnightBlockadeRookPawnDraw" -> "covers the promotion square",
      "QueenVsAdvancedPawn" -> "advanced pawn contained by checking distance",
      "TarraschDefenseActive" -> "active behind the pawn",
      "PassiveRookDefense" -> "behind the pawn",
      "RookAndBishopVsRookDraw" -> "known safe setup",
      "SameColoredBishopsBlockade" -> "shared color complex"
    )

    val lossCases = List(
      "WrongRookPawnWrongBishopFortress" -> "promotion corner",
      "OutsidePasserDecoy" -> "drags the enemy king away",
      "ConnectedPassers" -> "advancing together",
      "KeySquaresOppositionBreakthrough" -> "entry squares",
      "TriangulationZugzwang" -> "triangulation tempo",
      "BreakthroughSacrifice" -> "force open a passer",
      "Shouldering" -> "pushed off the pawn's path",
      "RetiManeuver" -> "combine pursuit of the passer",
      "ShortSideDefense" -> "checking distance",
      "OppositeColoredBishopsDraw" -> "bishop's color complex",
      "GoodBishopRookPawnConversion" -> "promotion corner",
      "KnightBlockadeRookPawnDraw" -> "promotion square",
      "QueenVsAdvancedPawn" -> "advanced pawn",
      "TarraschDefenseActive" -> "checking from behind the pawn",
      "PassiveRookDefense" -> "behind the pawn",
      "RookAndBishopVsRookDraw" -> "safe checking or corner setup",
      "SameColoredBishopsBlockade" -> "shared color complex"
    )

    holdCases.foreach { case (pattern, anchor) =>
      val ctx = syntheticContext(pattern = Some(pattern), patternAge = 4)
      val text = BookStyleRenderer.render(ctx)
      assert(
        text.contains("because") && text.toLowerCase.contains(anchor.toLowerCase),
        s"expected hold causality for $pattern, got: $text"
      )
    }

    lossCases.foreach { case (pattern, anchor) =>
      val ctx = syntheticContext(
        pattern = None,
        transition = Some(s"$pattern(Draw) → none(Unclear)")
      )
      val text = BookStyleRenderer.render(ctx)
      assert(
        text.contains("because") && text.toLowerCase.contains(anchor.toLowerCase),
        s"expected loss causality for $pattern, got: $text"
      )
    }
  }
