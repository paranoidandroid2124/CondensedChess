package lila.llm.analysis

import munit.FunSuite

import lila.llm.*
class CompensationDisplaySubtypeResolverTest extends FunSuite:

  private def benkoLikeCompensationPack: StrategyPack =
    StrategyPack(
      sideToMove = "black",
      strategicIdeas = List(
        StrategyIdeaSignal(
          ideaId = "idea_benko_line",
          ownerSide = "black",
          kind = StrategicIdeaKind.LineOccupation,
          group = "slow_structural",
          readiness = StrategicIdeaReadiness.Build,
          focusSquares = List("b2", "c4", "d4"),
          focusFiles = List("b", "c", "d"),
          focusZone = Some("queenside"),
          beneficiaryPieces = List("R", "Q"),
          confidence = 0.86
        ),
        StrategyIdeaSignal(
          ideaId = "idea_benko_targets",
          ownerSide = "black",
          kind = StrategicIdeaKind.TargetFixing,
          group = "slow_structural",
          readiness = StrategicIdeaReadiness.Build,
          focusSquares = List("b2", "a6"),
          focusFiles = List("a", "b"),
          focusZone = Some("queenside"),
          beneficiaryPieces = List("R"),
          confidence = 0.79
        )
      ),
      pieceRoutes = List(
        StrategyPieceRoute(
          ownerSide = "black",
          piece = "R",
          from = "a8",
          route = List("a8", "d8", "d3"),
          purpose = "kingside clamp",
          strategicFit = 0.82,
          tacticalSafety = 0.77,
          surfaceConfidence = 0.79,
          surfaceMode = RouteSurfaceMode.Toward
        )
      ),
      pieceMoveRefs = List(
        StrategyPieceMoveRef(
          ownerSide = "black",
          piece = "Q",
          from = "d8",
          target = "b6",
          idea = "fix the queenside targets"
        )
      ),
      directionalTargets = List(
        StrategyDirectionalTarget(
          targetId = "target_b2",
          ownerSide = "black",
          piece = "R",
          from = "d8",
          targetSquare = "b2",
          readiness = DirectionalTargetReadiness.Build,
          strategicReasons = List("backward pawn")
        )
      ),
      longTermFocus = List("fix the queenside targets before recovering the pawn"),
      signalDigest = Some(
        NarrativeSignalDigest(
          compensation = Some("return vector through line pressure and delayed recovery"),
          compensationVectors = List("Line Pressure (0.7)", "Delayed Recovery (0.6)", "Fixed Targets (0.5)"),
          investedMaterial = Some(100),
          dominantIdeaKind = Some(StrategicIdeaKind.LineOccupation),
          dominantIdeaGroup = Some("slow_structural"),
          dominantIdeaReadiness = Some(StrategicIdeaReadiness.Build),
          dominantIdeaFocus = Some("b2, c4, d4")
        )
      )
    )

  private def attackRecoveryDisplayNormalizationPack: StrategyPack =
    StrategyPack(
      sideToMove = "white",
      strategicIdeas = List(
        StrategyIdeaSignal(
          ideaId = "idea_attack_shell",
          ownerSide = "white",
          kind = StrategicIdeaKind.KingAttackBuildUp,
          group = "dynamic_attack",
          readiness = StrategicIdeaReadiness.Build,
          focusSquares = List("f2", "f4", "e6"),
          focusFiles = List("f", "g", "h"),
          focusZone = Some("kingside"),
          beneficiaryPieces = List("Q", "R"),
          confidence = 0.84
        )
      ),
      directionalTargets = List(
        StrategyDirectionalTarget(
          targetId = "target_f3",
          ownerSide = "white",
          piece = "R",
          from = "h3",
          targetSquare = "f3",
          readiness = DirectionalTargetReadiness.Build,
          strategicReasons = List("line access", "improves piece placement")
        ),
        StrategyDirectionalTarget(
          targetId = "target_f2",
          ownerSide = "white",
          piece = "Q",
          from = "g4",
          targetSquare = "f2",
          readiness = DirectionalTargetReadiness.Build,
          strategicReasons = List("supports immediate tactical gain counterplay", "pressure on a fixed weakness")
        )
      ),
      longTermFocus = List("cash out only after the kingside pressure stays durable"),
      signalDigest = Some(
        NarrativeSignalDigest(
          compensation = Some("return vector through initiative and line pressure"),
          compensationVectors = List("Initiative (0.6)", "Line Pressure (0.6)", "Delayed Recovery (0.4)"),
          investedMaterial = Some(100),
          dominantIdeaKind = Some(StrategicIdeaKind.KingAttackBuildUp),
          dominantIdeaGroup = Some("dynamic_attack"),
          dominantIdeaReadiness = Some(StrategicIdeaReadiness.Build),
          dominantIdeaFocus = Some("f2, f4, e6")
        )
      )
    )

  private def centeredTargetPayoffPack: StrategyPack =
    StrategyPack(
      sideToMove = "black",
      strategicIdeas = List(
        StrategyIdeaSignal(
          ideaId = "idea_center_shell",
          ownerSide = "black",
          kind = StrategicIdeaKind.LineOccupation,
          group = "slow_structural",
          readiness = StrategicIdeaReadiness.Build,
          focusSquares = List("d4", "e5"),
          focusFiles = List("d", "e"),
          focusZone = Some("center"),
          beneficiaryPieces = List("R", "N"),
          confidence = 0.81
        )
      ),
      pieceRoutes = List(
        StrategyPieceRoute(
          ownerSide = "black",
          piece = "R",
          from = "e8",
          route = List("e8", "d8", "d4"),
          purpose = "open-file occupation",
          strategicFit = 0.82,
          tacticalSafety = 0.76,
          surfaceConfidence = 0.79,
          surfaceMode = RouteSurfaceMode.Toward
        ),
        StrategyPieceRoute(
          ownerSide = "black",
          piece = "Q",
          from = "h4",
          route = List("h4", "g4", "f4"),
          purpose = "kingside clamp",
          strategicFit = 0.74,
          tacticalSafety = 0.7,
          surfaceConfidence = 0.72,
          surfaceMode = RouteSurfaceMode.Toward
        )
      ),
      pieceMoveRefs = List(
        StrategyPieceMoveRef(
          ownerSide = "black",
          piece = "N",
          from = "f7",
          target = "e5",
          idea = "contest the pawn on e5",
          evidence = List("target_pawn")
        ),
        StrategyPieceMoveRef(
          ownerSide = "black",
          piece = "N",
          from = "f6",
          target = "d4",
          idea = "contest the pawn on d4",
          evidence = List("target_pawn")
        )
      ),
      longTermFocus = List("keep the fixed central targets under pressure before recovering material"),
      signalDigest = Some(
        NarrativeSignalDigest(
          compensation = Some("return vector through line pressure and delayed recovery"),
          compensationVectors = List("Line Pressure (0.6)", "Delayed Recovery (0.6)", "Fixed Targets (0.4)"),
          investedMaterial = Some(100),
          dominantIdeaKind = Some(StrategicIdeaKind.LineOccupation),
          dominantIdeaGroup = Some("slow_structural"),
          dominantIdeaReadiness = Some(StrategicIdeaReadiness.Build),
          dominantIdeaFocus = Some("d4, e5"),
          decisionComparison = Some(
            DecisionComparisonDigest(
              evidence = Some("Further probe work can still route through g4 and h4 before the pressure lands.")
            )
          )
        )
      )
    )

  private def lockedTargetFixingPack: StrategyPack =
    centeredTargetPayoffPack.copy(
      strategicIdeas =
        centeredTargetPayoffPack.strategicIdeas :+
          StrategyIdeaSignal(
            ideaId = "idea_center_fix",
            ownerSide = "black",
            kind = StrategicIdeaKind.TargetFixing,
            group = "slow_structural",
            readiness = StrategicIdeaReadiness.Build,
            focusSquares = List("d4", "e5"),
            focusFiles = List("d", "e"),
            focusZone = Some("center"),
            beneficiaryPieces = List("N", "Q"),
            confidence = 0.84
          ),
      longTermFocus = List("keep the fixed central targets under pressure before recovering the pawn"),
      signalDigest = centeredTargetPayoffPack.signalDigest.map(_.copy(
        compensationVectors = List("Line Pressure (0.5)", "Delayed Recovery (0.7)", "Fixed Targets (0.7)"),
        dominantIdeaKind = Some(StrategicIdeaKind.TargetFixing),
        dominantIdeaFocus = Some("d4, e5"),
        decisionComparison = Some(
          DecisionComparisonDigest(
            evidence = Some("The point is not to switch theaters yet; keep the fixed central targets tied down first.")
          )
        )
      ))
    )

  private def weakCompensationShellPack: StrategyPack =
    StrategyPack(
      sideToMove = "white",
      strategicIdeas = List(
        StrategyIdeaSignal(
          ideaId = "idea_shell",
          ownerSide = "white",
          kind = StrategicIdeaKind.KingAttackBuildUp,
          group = "dynamic_attack",
          readiness = StrategicIdeaReadiness.Build,
          focusSquares = List("g7"),
          focusZone = Some("kingside"),
          beneficiaryPieces = List("Q"),
          confidence = 0.76
        )
      ),
      signalDigest = Some(
        NarrativeSignalDigest(
          compensation = Some("initiative against the king"),
          compensationVectors = List("Initiative (0.6)"),
          investedMaterial = Some(100),
          dominantIdeaKind = Some(StrategicIdeaKind.KingAttackBuildUp),
          dominantIdeaGroup = Some("dynamic_attack"),
          dominantIdeaReadiness = Some(StrategicIdeaReadiness.Build),
          dominantIdeaFocus = Some("g7")
        )
      )
    )

  test("resolver prefers payoff subtype for fixed-target compensation shell") {
    val surface = StrategyPackSurface.from(Some(benkoLikeCompensationPack))
    val rawSubtype = surface.compensationSubtype.getOrElse(fail("missing raw subtype"))

    val resolution = StrategyPackSurface.CompensationDisplaySubtypeResolver.resolve(surface, rawSubtype)

    assertEquals(resolution.displaySubtypeSource, "payoff")
    assertEquals(resolution.selectedDisplaySubtype.map(_.pressureTheater), Some("queenside"))
    assertEquals(resolution.selectedDisplaySubtype.map(_.pressureMode), Some("target_fixing"))
    assert(resolution.normalizationActive, clue(resolution))
    assert(resolution.payoffConfidence >= resolution.pathConfidence, clue(resolution))
  }

  test("resolver keeps raw fallback for attack-led shell") {
    val surface = StrategyPackSurface.from(Some(attackRecoveryDisplayNormalizationPack))
    val rawSubtype = surface.compensationSubtype.getOrElse(fail("missing raw subtype"))

    val resolution = StrategyPackSurface.CompensationDisplaySubtypeResolver.resolve(surface, rawSubtype)

    assertEquals(resolution.displaySubtypeSource, "path")
    assertEquals(resolution.selectedDisplaySubtype.map(_.pressureTheater), Some("kingside"))
    assertEquals(resolution.selectedDisplaySubtype.map(_.pressureMode), Some("line_occupation"))
  }

  test("resolver keeps the central target path when durable target anchors outweigh payoff drift") {
    val surface = StrategyPackSurface.from(Some(centeredTargetPayoffPack))
    val rawSubtype = surface.compensationSubtype.getOrElse(fail("missing raw subtype"))

    val resolution = StrategyPackSurface.CompensationDisplaySubtypeResolver.resolve(surface, rawSubtype)

    assertEquals(resolution.displaySubtypeSource, "path")
    assertEquals(resolution.selectedDisplaySubtype.map(_.pressureTheater), Some("center"))
    assertEquals(resolution.selectedDisplaySubtype.map(_.pressureMode), Some("target_fixing"))
    assert(resolution.normalizationActive, clue(resolution))
  }

  test("resolver keeps the durable target-fixing path when concrete anchors lock the theater") {
    val surface = StrategyPackSurface.from(Some(lockedTargetFixingPack))
    val rawSubtype = surface.compensationSubtype.getOrElse(fail("missing raw subtype"))

    val resolution = StrategyPackSurface.CompensationDisplaySubtypeResolver.resolve(surface, rawSubtype)

    assertEquals(resolution.displaySubtypeSource, "path")
    assertEquals(resolution.selectedDisplaySubtype.map(_.pressureTheater), Some("center"))
    assertEquals(resolution.selectedDisplaySubtype.map(_.pressureMode), Some("target_fixing"))
    assert(resolution.pathConfidence >= 4, clue(resolution))
  }

  test("resolver leaves weak compensation shells in raw fallback") {
    val surface = StrategyPackSurface.from(Some(weakCompensationShellPack))
    val rawSubtype = surface.compensationSubtype.getOrElse(fail("missing raw subtype"))

    val resolution = StrategyPackSurface.CompensationDisplaySubtypeResolver.resolve(surface, rawSubtype)

    assertEquals(resolution.displaySubtypeSource, "raw_fallback")
    assertEquals(resolution.selectedDisplaySubtype, None)
    assert(!resolution.normalizationActive, clue(resolution))
  }
