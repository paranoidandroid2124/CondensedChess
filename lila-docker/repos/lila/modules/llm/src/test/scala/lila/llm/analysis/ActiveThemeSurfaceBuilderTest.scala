package lila.llm.analysis

import munit.FunSuite
import lila.llm.*

class ActiveThemeSurfaceBuilderTest extends FunSuite:

  private def moment(
      ply: Int = 21,
      activePlan: Option[ActivePlanRef] = None,
      strategyPack: Option[StrategyPack] = None,
      signalDigest: Option[NarrativeSignalDigest] = None,
      authorEvidence: List[AuthorEvidenceSummary] = Nil
  ): GameNarrativeMoment =
    GameNarrativeMoment(
      momentId = s"ply_$ply",
      ply = ply,
      moveNumber = (ply + 1) / 2,
      side = "white",
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
      wpaSwing = None,
      strategicSalience = Some("High"),
      transitionType = None,
      transitionConfidence = None,
      activePlan = activePlan,
      topEngineMove = None,
      collapse = None,
      strategyPack = strategyPack,
      signalDigest = signalDigest,
      authorEvidence = authorEvidence
    )

  test("build picks whole-board play from multi-zone pressure and transfer cues") {
    val pack =
      StrategyPack(
        sideToMove = "white",
        plans = List(StrategySidePlan("white", "long", "Queenside pressure before kingside switch")),
        pieceRoutes = List(
          StrategyPieceRoute("white", "N", "d2", List("d2", "f1", "g3"), "kingside pressure", 0.84)
        ),
        longTermFocus = List("Fix queenside targets first, then switch to the kingside.")
      )

    val surface = ActiveThemeSurfaceBuilder.build(moment(strategyPack = Some(pack))).getOrElse(fail("missing surface"))
    assertEquals(surface.themeKey, "whole_board_play")
  }

  test("build picks opposite-coloured bishops conversion only with conversion evidence") {
    val digest =
      NarrativeSignalDigest(
        decision = Some("White can simplify into an opposite-coloured bishops ending."),
        strategicFlow = Some("The conversion hinges on color-complex penetration and a passer.")
      )
    val surface =
      ActiveThemeSurfaceBuilder.build(
        moment(
          activePlan = Some(ActivePlanRef("Opposite bishops conversion", Some("opposite_bishops_conversion"), Some("Execution"), Some(0.82))),
          signalDigest = Some(digest)
        )
      ).getOrElse(fail("missing surface"))
    assertEquals(surface.themeKey, "opposite_bishops_conversion")
  }

  test("build picks active-passive exchange from defender-trade suppression cues") {
    val digest =
      NarrativeSignalDigest(
        opponentPlan = Some("Black wants queenside counterplay"),
        prophylaxisThreat = Some("the only active counterplay is ...c5"),
        decision = Some("A defender trade leaves Black with only passive pieces.")
      )
    val surface =
      ActiveThemeSurfaceBuilder.build(
        moment(
          activePlan = Some(ActivePlanRef("Defender trade", Some("defender_trade"), Some("Execution"), Some(0.74))),
          signalDigest = Some(digest)
        )
      ).getOrElse(fail("missing surface"))
    assertEquals(surface.themeKey, "active_passive_exchange")
  }

  test("build picks rook-lift attack from rook route via third rank") {
    val pack =
      StrategyPack(
        sideToMove = "white",
        pieceRoutes = List(
          StrategyPieceRoute("white", "R", "h1", List("h1", "h3", "g3"), "kingside pressure", 0.88)
        )
      )
    val surface = ActiveThemeSurfaceBuilder.build(moment(strategyPack = Some(pack))).getOrElse(fail("missing surface"))
    assertEquals(surface.themeKey, "rook_lift_attack")
  }
