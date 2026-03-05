package lila.llm.analysis

import munit.FunSuite
import lila.llm.{ GameNarrativeMoment, StrategyPack, StrategyPieceRoute, StrategySidePlan }

class StrategicBranchSelectorTest extends FunSuite:

  private def moment(
      ply: Int,
      momentType: String,
      transitionType: Option[String] = None,
      wpaSwing: Option[Double] = None,
      strategyPack: Option[StrategyPack] = None
  ): GameNarrativeMoment =
    GameNarrativeMoment(
      momentId = s"ply_$ply",
      ply = ply,
      moveNumber = (ply + 1) / 2,
      side = if ply % 2 == 1 then "white" else "black",
      moveClassification = None,
      momentType = momentType,
      fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w - - 0 1",
      narrative = "Narrative",
      concepts = Nil,
      variations = Nil,
      cpBefore = 0,
      cpAfter = 0,
      mateBefore = None,
      mateAfter = None,
      wpaSwing = wpaSwing,
      strategicSalience = None,
      transitionType = transitionType,
      transitionConfidence = None,
      activePlan = None,
      topEngineMove = None,
      collapse = None,
      strategyPack = strategyPack
    )

  private val richPack = StrategyPack(
    sideToMove = "white",
    plans = List(StrategySidePlan("white", "long", "Kingside pressure")),
    pieceRoutes = List(StrategyPieceRoute("N", "d2", List("d2", "f1", "e3"), "king defense", 0.8)),
    longTermFocus = List("Dark-square control")
  )

  test("selector accepts configured moment types and transition types") {
    val moments = List(
      moment(ply = 11, momentType = "TensionPeak"),
      moment(ply = 22, momentType = "Quiet", transitionType = Some("ForcedPivot")),
      moment(ply = 35, momentType = "Quiet")
    )

    val selected = StrategicBranchSelector.select(moments).map(_.ply).toSet
    assertEquals(selected, Set(11, 22))
  }

  test("selector caps strategic branches to 8 moments") {
    val moments =
      (1 to 12).toList.map { i =>
        moment(
          ply = i,
          momentType = "SustainedPressure",
          wpaSwing = Some(i.toDouble),
          strategyPack = Some(richPack)
        )
      }

    val selected = StrategicBranchSelector.select(moments)
    assertEquals(selected.size, 8)
  }

  test("selector does not backfill when candidates are fewer than five") {
    val moments = List(
      moment(ply = 9, momentType = "OpeningTheoryEnds"),
      moment(ply = 18, momentType = "Quiet", transitionType = Some("Opportunistic")),
      moment(ply = 27, momentType = "Equalization"),
      moment(ply = 30, momentType = "Quiet"),
      moment(ply = 33, momentType = "Quiet"),
      moment(ply = 36, momentType = "Quiet")
    )

    val selected = StrategicBranchSelector.select(moments).map(_.ply)
    assertEquals(selected, List(9, 27, 18))
  }

