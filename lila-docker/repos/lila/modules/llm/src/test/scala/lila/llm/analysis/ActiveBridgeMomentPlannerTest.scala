package lila.llm.analysis

import munit.FunSuite
import lila.llm.*

class ActiveBridgeMomentPlannerTest extends FunSuite:

  private def moment(
      ply: Int,
      side: String = "white",
      subplanId: String = "minority_attack_fixation",
      theme: String = "Minority attack",
      structure: String = "Carlsbad",
      transitionType: Option[String] = None,
      decision: String = "keep building the same attack"
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
      selectionKind = "key",
      selectionLabel = Some("Key Moment"),
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
      activePlan = Some(ActivePlanRef(theme, Some(subplanId), Some("Execution"), Some(0.78))),
      topEngineMove = None,
      collapse = None,
      strategyPack = Some(
        StrategyPack(
          sideToMove = side,
          plans = List(StrategySidePlan(side, "long", theme)),
          pieceRoutes = List(StrategyPieceRoute(side, "R", "a1", List("a1", "b1", "b3"), "queenside pressure", 0.78)),
          longTermFocus = List("queenside pressure")
        )
      ),
      signalDigest = Some(
        NarrativeSignalDigest(
          structureProfile = Some(structure),
          structuralCue = Some(s"$structure structure"),
          deploymentPurpose = Some("queenside pressure"),
          decision = Some(decision)
        )
      )
    )

  private def conversionMoment(
      ply: Int,
      structure: String = "Carlsbad"
  ): GameChronicleMoment =
    moment(
      ply = ply,
      subplanId = "invasion_transition",
      theme = "Invasion transition",
      structure = structure,
      decision = "simplify into the winning endgame",
      transitionType = Some("NaturalShift")
    )

  test("planCandidatePlies respects top-3 thread scope and 36-ply cap") {
    val allThreads =
      StrategicBranchSelector.rankThreads(
        List(
          moment(11, subplanId = "minority_attack_fixation", theme = "Minority attack", structure = "Carlsbad"),
          moment(19, subplanId = "minority_attack_fixation", theme = "Minority attack", structure = "Carlsbad"),
          moment(41, subplanId = "rook_pawn_march", theme = "Rook-pawn march", structure = "Locked center"),
          moment(49, subplanId = "rook_pawn_march", theme = "Rook-pawn march", structure = "Locked center"),
          moment(71, subplanId = "outpost_entrenchment", theme = "Outpost entrenchment", structure = "Hedgehog"),
          moment(79, subplanId = "outpost_entrenchment", theme = "Outpost entrenchment", structure = "Hedgehog"),
          conversionMoment(101, structure = "Opposite bishops"),
          conversionMoment(109, structure = "Opposite bishops")
        )
      )

    val planned =
      ActiveBridgeMomentPlanner.planCandidatePlies(
        rankedThreads = allThreads,
        anchorPlies = allThreads.flatMap(_.moments.map(_.moment.ply)).toSet,
        totalPlies = 140
      )

    assertEquals(planned.size, 3)
    assert(planned.values.forall(_.size <= 14))
    assert(planned.values.flatten.toList.distinct.size <= 36)
  }

  test("selectBridges fills missing stages from enriched thread moments") {
    val anchors =
      List(
        moment(11, decision = "seed the queenside pressure"),
        moment(31, transitionType = Some("NaturalShift"), decision = "switch into direct invasion")
      )
    val baseThreads = StrategicBranchSelector.rankThreads(anchors)
    val enriched =
      anchors ++ List(
        moment(19, decision = "keep building the same minority attack").copy(
          momentType = "StrategicBridge",
          selectionKind = "thread_bridge",
          selectionLabel = Some("Campaign Bridge")
        ),
        moment(39, transitionType = Some("NaturalShift"), decision = "simplify into the winning endgame").copy(
          momentType = "StrategicBridge",
          selectionKind = "thread_bridge",
          selectionLabel = Some("Campaign Bridge")
        )
      )

    val bridges =
      ActiveBridgeMomentPlanner.selectBridges(
        rankedThreads = baseThreads,
        enrichedMoments = enriched,
        anchorPlies = anchors.map(_.ply).toSet
      )

    assertEquals(bridges.map(_.ply), List(19, 39))
    assert(bridges.exists(_.reason.contains("fills build stage")))
    assert(bridges.exists(_.reason.contains("fills convert stage")))
  }

  test("selectBridges ignores incompatible structure and opposite-side candidates") {
    val anchors = List(moment(11), moment(27, transitionType = Some("NaturalShift"), decision = "switch into direct invasion"))
    val baseThreads = StrategicBranchSelector.rankThreads(anchors)
    val enriched =
      anchors ++ List(
        moment(19, structure = "Isolated Queen Pawn").copy(
          momentType = "StrategicBridge",
          selectionKind = "thread_bridge",
          selectionLabel = Some("Campaign Bridge")
        ),
        moment(21, side = "black").copy(
          momentType = "StrategicBridge",
          selectionKind = "thread_bridge",
          selectionLabel = Some("Campaign Bridge")
        )
      )

    val bridges =
      ActiveBridgeMomentPlanner.selectBridges(
        rankedThreads = baseThreads,
        enrichedMoments = enriched,
        anchorPlies = anchors.map(_.ply).toSet
      )

    assertEquals(bridges, Nil)
  }

  test("selectBridges adds a continuity bridge when stages are already covered") {
    val anchors =
      List(
        moment(11, decision = "seed the queenside pressure"),
        moment(19, decision = "keep building the same minority attack"),
        moment(27, transitionType = Some("NaturalShift"), decision = "switch files"),
        moment(43, transitionType = Some("NaturalShift"), decision = "simplify into the winning endgame")
      )
    val baseThreads = StrategicBranchSelector.rankThreads(anchors)
    val enriched =
      anchors ++ List(
        moment(33, decision = "keep the same bind without changing the plan").copy(
          momentType = "StrategicBridge",
          selectionKind = "thread_bridge",
          selectionLabel = Some("Campaign Bridge")
        )
      )

    val bridges =
      ActiveBridgeMomentPlanner.selectBridges(
        rankedThreads = baseThreads,
        enrichedMoments = enriched,
        anchorPlies = anchors.map(_.ply).toSet
      )

    assertEquals(bridges.map(_.ply), List(33))
    assert(bridges.head.reason.contains("bridges long gap"))
  }
