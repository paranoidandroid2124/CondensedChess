package lila.llm.analysis

import munit.FunSuite

import lila.llm.*

class StrategicIdeaSelectorTest extends FunSuite:

  private def plan(name: String) =
    StrategySidePlan(
      side = "white",
      horizon = "long",
      planName = name
    )

  private def pack(
      plans: List[String] = Nil,
      digest: Option[NarrativeSignalDigest] = None,
      directionalTargets: List[StrategyDirectionalTarget] = Nil,
      routes: List[StrategyPieceRoute] = Nil,
      moveRefs: List[StrategyPieceMoveRef] = Nil
  ): StrategyPack =
    StrategyPack(
      sideToMove = "white",
      plans = plans.map(plan),
      pieceRoutes = routes,
      pieceMoveRefs = moveRefs,
      directionalTargets = directionalTargets,
      signalDigest = digest
    )

  private def selected(pack: StrategyPack): List[StrategyIdeaSignal] =
    StrategicIdeaSelector.select(pack)

  test("selects pawn_break from explicit plan cue") {
    val idea = selected(pack(plans = List("Prepare the c5 break"))).head
    assertEquals(idea.kind, StrategicIdeaKind.PawnBreak)
  }

  test("selects space_gain_or_restriction from explicit plan cue") {
    val idea = selected(pack(plans = List("Build kingside clamp and space gain"))).head
    assertEquals(idea.kind, StrategicIdeaKind.SpaceGainOrRestriction)
  }

  test("selects target_fixing from explicit plan cue") {
    val idea = selected(pack(plans = List("Fix the e5 square before switching play"))).head
    assertEquals(idea.kind, StrategicIdeaKind.TargetFixing)
  }

  test("selects line_occupation from route cue") {
    val idea = selected(
      pack(
        routes = List(
          StrategyPieceRoute(
            ownerSide = "white",
            piece = "R",
            from = "a1",
            route = List("a1", "b1", "b3"),
            purpose = "open-file occupation",
            strategicFit = 0.81,
            tacticalSafety = 0.76,
            surfaceConfidence = 0.76,
            surfaceMode = RouteSurfaceMode.Toward
          )
        )
      )
    ).head
    assertEquals(idea.kind, StrategicIdeaKind.LineOccupation)
  }

  test("selects outpost_creation_or_occupation from directional target cue") {
    val idea = selected(
      pack(
        directionalTargets = List(
          StrategyDirectionalTarget(
            targetId = "target_1",
            ownerSide = "white",
            piece = "N",
            from = "c3",
            targetSquare = "e4",
            readiness = DirectionalTargetReadiness.Build,
            strategicReasons = List("outpost pressure")
          )
        )
      )
    ).head
    assertEquals(idea.kind, StrategicIdeaKind.OutpostCreationOrOccupation)
  }

  test("selects minor_piece_imbalance_exploitation from explicit plan cue") {
    val idea = selected(pack(plans = List("Exploit the bad bishop versus the knight"))).head
    assertEquals(idea.kind, StrategicIdeaKind.MinorPieceImbalanceExploitation)
  }

  test("selects prophylaxis from digest cue") {
    val idea = selected(
      pack(
        digest = Some(NarrativeSignalDigest(prophylaxisPlan = Some("Prevent ...f5 expansion")))
      )
    ).head
    assertEquals(idea.kind, StrategicIdeaKind.Prophylaxis)
  }

  test("selects king_attack_build_up from explicit plan cue") {
    val idea = selected(pack(plans = List("Organize the kingside attack build-up"))).head
    assertEquals(idea.kind, StrategicIdeaKind.KingAttackBuildUp)
  }

  test("selects favorable_trade_or_transformation from move-ref cue") {
    val idea = selected(
      pack(
        moveRefs = List(
          StrategyPieceMoveRef(
            ownerSide = "white",
            piece = "B",
            from = "g2",
            target = "c6",
            idea = "favorable exchange on c6"
          )
        )
      )
    ).head
    assertEquals(idea.kind, StrategicIdeaKind.FavorableTradeOrTransformation)
  }

  test("selects counterplay_suppression from counterplay cue") {
    val idea = selected(
      pack(
        digest = Some(NarrativeSignalDigest(opponentPlan = Some("queenside counterplay")))
      )
    ).head
    assertEquals(idea.kind, StrategicIdeaKind.CounterplaySuppression)
  }

  test("structural_change ideas map into the structural group") {
    val idea = selected(pack(plans = List("Prepare the c5 break"))).head
    assertEquals(idea.group, StrategicIdeaGroup.StructuralChange)
  }

  test("piece_and_line_management ideas map into the piece-and-line group") {
    val idea = selected(
      pack(
        directionalTargets = List(
          StrategyDirectionalTarget(
            targetId = "target_2",
            ownerSide = "white",
            piece = "N",
            from = "c3",
            targetSquare = "e4",
            readiness = DirectionalTargetReadiness.Build,
            strategicReasons = List("outpost pressure")
          )
        )
      )
    ).head
    assertEquals(idea.group, StrategicIdeaGroup.PieceAndLineManagement)
  }

  test("interaction_and_transformation ideas map into the interaction group") {
    val idea = selected(
      pack(
        digest = Some(NarrativeSignalDigest(prophylaxisPlan = Some("Prevent ...f5 expansion")))
      )
    ).head
    assertEquals(idea.group, StrategicIdeaGroup.InteractionAndTransformation)
  }

  test("secondary idea is allowed only across groups within the score window") {
    val ideas = selected(
      pack(
        plans = List("Build kingside clamp and space gain"),
        directionalTargets = List(
          StrategyDirectionalTarget(
            targetId = "target_3",
            ownerSide = "white",
            piece = "R",
            from = "a1",
            targetSquare = "b1",
            readiness = DirectionalTargetReadiness.Build,
            strategicReasons = List("line access")
          )
        )
      )
    )

    assertEquals(ideas.head.kind, StrategicIdeaKind.SpaceGainOrRestriction)
    assertEquals(ideas.drop(1).headOption.map(_.kind), Some(StrategicIdeaKind.LineOccupation))
  }
