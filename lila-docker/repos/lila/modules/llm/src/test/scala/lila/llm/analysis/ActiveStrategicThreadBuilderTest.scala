package lila.llm.analysis

import munit.FunSuite
import lila.llm.*

class ActiveStrategicThreadBuilderTest extends FunSuite:

  private def moment(
      ply: Int,
      side: String = "white",
      activePlan: Option[ActivePlanRef],
      strategyPack: Option[StrategyPack] = None,
      signalDigest: Option[NarrativeSignalDigest] = None,
      transitionType: Option[String] = None
  ): GameChronicleMoment =
    GameChronicleMoment(
      momentId = s"ply_$ply",
      ply = ply,
      moveNumber = (ply + 1) / 2,
      side = side,
      moveClassification = None,
      momentType = "SustainedPressure",
      fen = "r2q1rk1/pp2bppp/2n1pn2/2pp4/3P4/2P1PN2/PPBNBPPP/R2Q1RK1 w - - 0 11",
      narrative = "Narrative",
      concepts = List("space"),
      variations = Nil,
      cpBefore = 0,
      cpAfter = 0,
      mateBefore = None,
      mateAfter = None,
      wpaSwing = Some(0.08),
      strategicSalience = Some("High"),
      transitionType = transitionType,
      transitionConfidence = None,
      activePlan = activePlan,
      topEngineMove = None,
      collapse = None,
      strategyPack = strategyPack,
      signalDigest = signalDigest
    )

  private def minorityMoment(ply: Int, structure: String, side: String = "white"): GameChronicleMoment =
    moment(
      ply = ply,
      side = side,
      activePlan = Some(ActivePlanRef("Minority attack", Some("minority_attack_fixation"), Some("Execution"), Some(0.78))),
      signalDigest = Some(
        NarrativeSignalDigest(
          structureProfile = Some(structure),
          structuralCue = Some(s"$structure structure"),
          deploymentPurpose = Some("queenside pressure")
        )
      )
    )

  test("build merges same theme and continuity into one thread") {
    val result =
      ActiveStrategicThreadBuilder.build(
        List(
          minorityMoment(11, "Carlsbad"),
          minorityMoment(19, "Carlsbad"),
          minorityMoment(27, "Carlsbad", side = "white")
        )
      )

    assertEquals(result.threads.size, 1)
    assertEquals(result.threads.head.themeKey, "minority_attack")
    assert(result.threadRefsByPly.keySet.contains(11))
    assert(result.threadRefsByPly.keySet.contains(19))
    assert(result.threadRefsByPly.keySet.contains(27))
  }

  test("build splits threads when the structure profile changes") {
    val result =
      ActiveStrategicThreadBuilder.build(
        List(
          minorityMoment(11, "Carlsbad"),
          minorityMoment(19, "Carlsbad"),
          minorityMoment(31, "Isolated Queen Pawn"),
          minorityMoment(39, "Isolated Queen Pawn")
        )
      )

    assertEquals(result.threads.size, 2)
    assertEquals(result.threads.map(_.seedPly), List(11, 31))
  }

  test("build splits on opposite ownership and classifies seed build convert stages") {
    val whiteConversion =
      StrategyPack(
        sideToMove = "white",
        plans = List(StrategySidePlan("white", "long", "Opposite bishops conversion")),
        pieceRoutes = List(StrategyPieceRoute("white", "K", "g2", List("g2", "f3", "e4"), "central route", 0.66))
      )
    val result =
      ActiveStrategicThreadBuilder.build(
        List(
          moment(
            ply = 41,
            activePlan = Some(ActivePlanRef("Opposite bishops conversion", Some("opposite_bishops_conversion"), Some("Execution"), Some(0.72))),
            strategyPack = Some(whiteConversion),
            signalDigest = Some(NarrativeSignalDigest(decision = Some("Start converting toward the endgame.")))
          ),
          moment(
            ply = 49,
            activePlan = Some(ActivePlanRef("Opposite bishops conversion", Some("opposite_bishops_conversion"), Some("Execution"), Some(0.84))),
            strategyPack = Some(whiteConversion),
            signalDigest = Some(NarrativeSignalDigest(decision = Some("Keep the same conversion under control.")))
          ),
          moment(
            ply = 57,
            activePlan = Some(ActivePlanRef("Opposite bishops conversion", Some("opposite_bishops_conversion"), Some("Execution"), Some(0.90))),
            strategyPack = Some(whiteConversion),
            signalDigest = Some(NarrativeSignalDigest(decision = Some("Simplify into the winning opposite-coloured bishops ending."))),
            transitionType = Some("NaturalShift")
          ),
          minorityMoment(65, "Carlsbad", side = "black"),
          minorityMoment(73, "Carlsbad", side = "black")
        )
      )

    assertEquals(result.threads.size, 2)
    assertEquals(result.threadRefsByPly(41).stageKey, "seed")
    assertEquals(result.threadRefsByPly(49).stageKey, "build")
    assertEquals(result.threadRefsByPly(57).stageKey, "convert")
    assertEquals(result.threads.head.themeKey, "opposite_bishops_conversion")
  }
