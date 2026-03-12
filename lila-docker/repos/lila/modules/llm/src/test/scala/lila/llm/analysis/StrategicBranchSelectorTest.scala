package lila.llm.analysis

import munit.FunSuite
import lila.llm.{ ActivePlanRef, GameNarrativeMoment, NarrativeSignalDigest, StrategyPack, StrategyPieceRoute, StrategySidePlan }

class StrategicBranchSelectorTest extends FunSuite:

  private def moment(
      ply: Int,
      momentType: String,
      moveClassification: Option[String] = None,
      transitionType: Option[String] = None,
      selectionKind: String = "key",
      selectionLabel: Option[String] = Some("Key Moment"),
      wpaSwing: Option[Double] = None,
      strategyPack: Option[StrategyPack] = None
  ): GameNarrativeMoment =
    GameNarrativeMoment(
      momentId = s"ply_$ply",
      ply = ply,
      moveNumber = (ply + 1) / 2,
      side = if ply % 2 == 1 then "white" else "black",
      moveClassification = moveClassification,
      momentType = momentType,
      fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w - - 0 1",
      narrative = "Narrative",
      selectionKind = selectionKind,
      selectionLabel = selectionLabel,
      concepts = Nil,
      variations = Nil,
      cpBefore = 0,
      cpAfter = 0,
      mateBefore = None,
      mateAfter = None,
      wpaSwing = wpaSwing,
      strategicSalience = Some("High"),
      transitionType = transitionType,
      transitionConfidence = None,
      activePlan = None,
      topEngineMove = None,
      collapse = None,
      strategyPack = strategyPack
    )

  private def threadedMoment(
      ply: Int,
      subplanId: String = "minority_attack_fixation",
      theme: String = "Minority attack",
      structure: String = "Carlsbad",
      transitionType: Option[String] = None,
      decision: String = "keep building the same minority attack",
      selectionKind: String = "key"
  ): GameNarrativeMoment =
    moment(
      ply = ply,
      momentType = if selectionKind == "thread_bridge" then "StrategicBridge" else "SustainedPressure",
      transitionType = transitionType,
      selectionKind = selectionKind,
      selectionLabel = Some(if selectionKind == "thread_bridge" then "Campaign Bridge" else "Key Moment"),
      wpaSwing = Some(6),
      strategyPack = Some(
        StrategyPack(
          sideToMove = "white",
          plans = List(StrategySidePlan("white", "long", theme)),
          pieceRoutes = List(StrategyPieceRoute("white", "R", "a1", List("a1", "b1", "b3"), "queenside pressure", 0.78)),
          longTermFocus = List("queenside pressure")
        )
      )
    ).copy(
      activePlan = Some(ActivePlanRef(theme, Some(subplanId), Some("Execution"), Some(0.8))),
      signalDigest = Some(
        NarrativeSignalDigest(
          structureProfile = Some(structure),
          structuralCue = Some(s"$structure structure"),
          deploymentPurpose = Some("queenside pressure"),
          decision = Some(decision)
        )
      )
    )

  test("selector keeps only top 3 threads visible and caps active-note targets at 8") {
    val selection =
      StrategicBranchSelector.buildSelection(
        List(
          threadedMoment(11),
          threadedMoment(19),
          threadedMoment(27, transitionType = Some("NaturalShift"), decision = "convert the first thread"),
          threadedMoment(61),
          threadedMoment(69),
          threadedMoment(77, transitionType = Some("NaturalShift"), decision = "convert the second thread"),
          threadedMoment(111),
          threadedMoment(119),
          threadedMoment(127, transitionType = Some("NaturalShift"), decision = "convert the third thread"),
          threadedMoment(161),
          threadedMoment(169),
          threadedMoment(177, transitionType = Some("NaturalShift"), decision = "convert the hidden fourth thread")
        )
      )

    assertEquals(selection.threads.size, 3)
    assertEquals(selection.selectedMoments.size, 9)
    assertEquals(selection.activeNoteMoments.size, 8)
    assert(!selection.selectedMoments.exists(_.ply >= 160))
  }

  test("selector fills spare visible slots with blunder, missed win, mate, then opening branch events") {
    val selection =
      StrategicBranchSelector.buildSelection(
        List(
          threadedMoment(11),
          threadedMoment(19),
          threadedMoment(27, transitionType = Some("NaturalShift"), decision = "convert thread one"),
          threadedMoment(61),
          threadedMoment(69),
          threadedMoment(77, transitionType = Some("NaturalShift"), decision = "convert thread two"),
          threadedMoment(111),
          threadedMoment(119),
          threadedMoment(127, transitionType = Some("NaturalShift"), decision = "convert thread three"),
          moment(130, "AdvantageSwing", moveClassification = Some("Blunder")),
          moment(132, "AdvantageSwing", moveClassification = Some("MissedWin")),
          moment(134, "MatePivot"),
          moment(136, "OpeningNovelty", selectionKind = "opening", selectionLabel = Some("Opening Event")),
          moment(138, "OpeningIntro", selectionKind = "opening", selectionLabel = Some("Opening Event"))
        )
      )

    val plies = selection.selectedMoments.map(_.ply)
    assertEquals(selection.selectedMoments.size, 12)
    assert(plies.contains(130))
    assert(plies.contains(132))
    assert(plies.contains(134))
    assert(!plies.contains(136))
    assert(!plies.contains(138))
  }

  test("bridge moments are visible only when they occupy a representative stage slot") {
    val selection =
      StrategicBranchSelector.buildSelection(
        List(
          threadedMoment(11, decision = "seed thread one"),
          threadedMoment(19, decision = "build via bridge", selectionKind = "thread_bridge"),
          threadedMoment(29, transitionType = Some("NaturalShift"), decision = "convert thread one"),
          threadedMoment(61, decision = "seed thread two"),
          threadedMoment(69, decision = "standard build thread two"),
          threadedMoment(73, decision = "non representative bridge", selectionKind = "thread_bridge"),
          threadedMoment(81, transitionType = Some("NaturalShift"), decision = "convert thread two")
        )
      )

    val plies = selection.selectedMoments.map(_.ply)
    assert(plies.contains(19))
    assert(!plies.contains(73))
  }

  test("selector falls back to core tactical and opening branch events when no threads exist") {
    val selection =
      StrategicBranchSelector.buildSelection(
        List(
          moment(9, "OpeningIntro", selectionKind = "opening", selectionLabel = Some("Opening Event")),
          moment(14, "OpeningTheoryEnds", selectionKind = "opening", selectionLabel = Some("Opening Event")),
          moment(18, "AdvantageSwing", moveClassification = Some("Blunder")),
          moment(22, "MatePivot"),
          moment(30, "SustainedPressure")
        )
      )

    assertEquals(selection.threads, Nil)
    assertEquals(selection.activeNoteMoments, Nil)
    assertEquals(selection.selectedMoments.map(_.ply), List(14, 18, 22))
  }
