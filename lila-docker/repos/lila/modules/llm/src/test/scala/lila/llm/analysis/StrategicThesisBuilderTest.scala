package lila.llm.analysis

import munit.FunSuite
import lila.llm.*
import lila.llm.model.*
import lila.llm.model.authoring.*

class StrategicThesisBuilderTest extends FunSuite:

  private def baseContext: NarrativeContext =
    NarrativeContext(
      fen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1",
      header = ContextHeader("Middlegame", "Normal", "NarrowChoice", "Medium", "ExplainPlan"),
      ply = 24,
      playedMove = Some("h2h4"),
      playedSan = Some("h4"),
      summary = NarrativeSummary("Kingside expansion", None, "NarrowChoice", "Maintain", "0.00"),
      threats = ThreatTable(Nil, Nil),
      pawnPlay = PawnPlayTable(false, None, "Low", "Maintain", "Quiet", "Background", None, false, "quiet"),
      plans = PlanTable(
        top5 = List(
          PlanRow(
            rank = 1,
            name = "Kingside expansion",
            score = 0.82,
            evidence = List("space on the kingside"),
            confidence = ConfidenceLevel.Heuristic
          )
        ),
        suppressed = Nil
      ),
      delta = None,
      phase = PhaseContext("Middlegame", "Balanced middlegame"),
      candidates = Nil,
      renderMode = NarrativeRenderMode.Bookmaker
    )

  private def paragraphs(text: String): List[String] =
    Option(text).getOrElse("")
      .split("""\n\s*\n""")
      .map(_.trim)
      .filter(_.nonEmpty)
      .toList

  private def planExperiment(
      planId: String,
      subplanId: Option[String] = None,
      evidenceTier: String = "evidence_backed",
      moveOrderSensitive: Boolean = false
  ): StrategicPlanExperiment =
    StrategicPlanExperiment(
      planId = planId,
      subplanId = subplanId,
      evidenceTier = evidenceTier,
      moveOrderSensitive = moveOrderSensitive
    )

  private def surfaceDrivenPack(
      compensation: Option[String] = None,
      compensationVectors: List[String] = Nil,
      investedMaterial: Option[Int] = None
  ): StrategyPack =
    StrategyPack(
      sideToMove = "white",
      strategicIdeas = List(
        StrategyIdeaSignal(
          ideaId = "idea_attack_g7",
          ownerSide = "white",
          kind = StrategicIdeaKind.KingAttackBuildUp,
          group = StrategicIdeaGroup.InteractionAndTransformation,
          readiness = StrategicIdeaReadiness.Build,
          focusSquares = List("g7"),
          focusZone = Some("kingside"),
          beneficiaryPieces = List("Q", "R"),
          confidence = 0.91
        )
      ),
      pieceRoutes = List(
        StrategyPieceRoute(
          ownerSide = "white",
          piece = "R",
          from = "c3",
          route = List("c3", "g3"),
          purpose = "rook lift",
          strategicFit = 0.88,
          tacticalSafety = 0.74,
          surfaceConfidence = 0.82,
          surfaceMode = RouteSurfaceMode.Toward
        )
      ),
      directionalTargets = List(
        StrategyDirectionalTarget(
          targetId = "target_g7",
          ownerSide = "white",
          piece = "Q",
          from = "d1",
          targetSquare = "g7",
          readiness = DirectionalTargetReadiness.Build,
          strategicReasons = List("mating net")
        )
      ),
      longTermFocus = List("keep pressure on g7"),
      signalDigest = Some(
        NarrativeSignalDigest(
          compensation = compensation,
          compensationVectors = compensationVectors,
          investedMaterial = investedMaterial,
          dominantIdeaKind = Some(StrategicIdeaKind.KingAttackBuildUp),
          dominantIdeaGroup = Some(StrategicIdeaGroup.InteractionAndTransformation),
          dominantIdeaReadiness = Some(StrategicIdeaReadiness.Build),
          dominantIdeaFocus = Some("g7")
        )
      )
    )

  private def quietCompensationPack: StrategyPack =
    StrategyPack(
      sideToMove = "black",
      strategicIdeas = List(
        StrategyIdeaSignal(
          ideaId = "idea_bfile_fixation",
          ownerSide = "black",
          kind = StrategicIdeaKind.TargetFixing,
          group = "slow_structural",
          readiness = StrategicIdeaReadiness.Build,
          focusSquares = List("b2", "b3"),
          focusFiles = List("b"),
          focusZone = Some("queenside"),
          beneficiaryPieces = List("R"),
          confidence = 0.88
        )
      ),
      pieceRoutes = List(
        StrategyPieceRoute(
          ownerSide = "black",
          piece = "R",
          from = "a8",
          route = List("a8", "b8", "b4"),
          purpose = "queenside pressure",
          strategicFit = 0.84,
          tacticalSafety = 0.78,
          surfaceConfidence = 0.80,
          surfaceMode = RouteSurfaceMode.Toward
        )
      ),
      longTermFocus = List("fix the queenside targets before recovering the pawn"),
      signalDigest = Some(
        NarrativeSignalDigest(
          compensation = Some("return vector through line pressure and delayed recovery"),
          compensationVectors = List("Line Pressure (0.7)", "Delayed Recovery (0.6)"),
          investedMaterial = Some(100),
          dominantIdeaKind = Some(StrategicIdeaKind.TargetFixing),
          dominantIdeaGroup = Some("slow_structural"),
          dominantIdeaReadiness = Some(StrategicIdeaReadiness.Build),
          dominantIdeaFocus = Some("b2, b3")
        )
      )
    )

  test("maintenance phase sanitized pack does not surface compensation thesis") {
    val ctx =
      baseContext.copy(
        fen = "r5k1/8/8/8/1b6/8/8/3R2K1 w - - 0 1",
        playedMove = Some("g1h2"),
        playedSan = Some("Kh2")
      )
    val pack =
      StrategyPack(
        sideToMove = "white",
        longTermFocus = List("central file pressure"),
        signalDigest =
          Some(
            NarrativeSignalDigest(
              compensation = Some("central file pressure"),
              compensationVectors = List("central files"),
              investedMaterial = Some(100),
              dominantIdeaFocus = Some("pressure on e6")
            )
          )
      )
    val contract = DecisiveTruth.derive(
      ctx = ctx,
      cpBefore = Some(40),
      cpAfter = Some(40),
      strategyPack = Some(pack),
      comparisonOverride =
        Some(
          DecisionComparison(
            chosenMove = Some("Kh2"),
            engineBestMove = Some("Kh2"),
            engineBestScoreCp = Some(40),
            engineBestPv = List("Kh2"),
            cpLossVsChosen = Some(0),
            deferredMove = None,
            deferredReason = None,
            deferredSource = None,
            evidence = None,
            practicalAlternative = false,
            chosenMatchesBest = true
          )
        )
    )
    val sanitizedCtx = DecisiveTruth.sanitizeContext(ctx, contract)
    val sanitizedPack = DecisiveTruth.sanitizeStrategyPack(Some(pack), contract)
    val thesis = StrategicThesisBuilder.build(sanitizedCtx, sanitizedPack)

    assertEquals(contract.truthPhase, Some(InvestmentTruthPhase.CompensationMaintenance))
    assertEquals(contract.compensationProseAllowed, false)
    assert(thesis.exists(_.lens != StrategicLens.Compensation), clue(thesis))
  }

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

  private def weakCompensationPack: StrategyPack =
    StrategyPack(
      sideToMove = "white",
      signalDigest = Some(
        NarrativeSignalDigest(
          compensation = Some("initiative against the king"),
          compensationVectors = List("Initiative (0.6)"),
          investedMaterial = Some(100)
        )
      )
    )

  private def duplicateCompensationPack: StrategyPack =
    StrategyPack(
      sideToMove = "black",
      strategicIdeas = List(
        StrategyIdeaSignal(
          ideaId = "idea_line_pressure",
          ownerSide = "black",
          kind = StrategicIdeaKind.LineOccupation,
          group = "slow_structural",
          readiness = StrategicIdeaReadiness.Build,
          focusSquares = List("d3"),
          focusFiles = List("d"),
          focusZone = Some("center"),
          beneficiaryPieces = List("Q", "R"),
          confidence = 0.84
        )
      ),
      pieceRoutes = List(
        StrategyPieceRoute(
          ownerSide = "black",
          piece = "Q",
          from = "d8",
          route = List("d8", "d6", "d3"),
          purpose = "central pressure",
          strategicFit = 0.81,
          tacticalSafety = 0.74,
          surfaceConfidence = 0.78,
          surfaceMode = RouteSurfaceMode.Toward
        )
      ),
      signalDigest = Some(
        NarrativeSignalDigest(
          compensation = Some("pressure on d3"),
          compensationVectors = List("Pressure on d3 (0.6)", "Delayed Recovery (0.4)"),
          investedMaterial = Some(100),
          dominantIdeaKind = Some(StrategicIdeaKind.LineOccupation),
          dominantIdeaGroup = Some("slow_structural"),
          dominantIdeaReadiness = Some(StrategicIdeaReadiness.Build),
          dominantIdeaFocus = Some("d3")
        )
      )
    )

  private def targetDisplayNormalizationPack: StrategyPack =
    StrategyPack(
      sideToMove = "white",
      strategicIdeas = List(
        StrategyIdeaSignal(
          ideaId = "idea_center_line",
          ownerSide = "white",
          kind = StrategicIdeaKind.LineOccupation,
          group = "slow_structural",
          readiness = StrategicIdeaReadiness.Build,
          focusSquares = List("d4", "e4"),
          focusFiles = List("d", "e"),
          focusZone = Some("center"),
          beneficiaryPieces = List("Q", "R"),
          confidence = 0.83
        )
      ),
      pieceRoutes = List(
        StrategyPieceRoute(
          ownerSide = "white",
          piece = "Q",
          from = "d1",
          route = List("d1", "d5"),
          purpose = "file pressure",
          strategicFit = 0.8,
          tacticalSafety = 0.76,
          surfaceConfidence = 0.79,
          surfaceMode = RouteSurfaceMode.Toward
        )
      ),
      pieceMoveRefs = List(
        StrategyPieceMoveRef(
          ownerSide = "white",
          piece = "B",
          from = "e3",
          target = "d4",
          idea = "contest the pawn on d4",
          evidence = List("target_pawn")
        )
      ),
      directionalTargets = List(
        StrategyDirectionalTarget(
          targetId = "target_d4",
          ownerSide = "white",
          piece = "Q",
          from = "d1",
          targetSquare = "d4",
          readiness = DirectionalTargetReadiness.Build,
          strategicReasons = List("weak pawn")
        )
      ),
      longTermFocus = List("keep the fixed central targets under pressure before recovering material"),
      signalDigest = Some(
        NarrativeSignalDigest(
          compensation = Some("return vector through line pressure and delayed recovery"),
          compensationVectors = List("Line Pressure (0.7)", "Delayed Recovery (0.6)", "Fixed Targets (0.5)"),
          investedMaterial = Some(100),
          dominantIdeaKind = Some(StrategicIdeaKind.LineOccupation),
          dominantIdeaGroup = Some("slow_structural"),
          dominantIdeaReadiness = Some(StrategicIdeaReadiness.Build),
          dominantIdeaFocus = Some("d4, e4")
        )
      )
    )

  private def kingsideDisplayNormalizationPack: StrategyPack =
    StrategyPack(
      sideToMove = "white",
      strategicIdeas = List(
        StrategyIdeaSignal(
          ideaId = "idea_center_line_support",
          ownerSide = "white",
          kind = StrategicIdeaKind.LineOccupation,
          group = "slow_structural",
          readiness = StrategicIdeaReadiness.Build,
          focusSquares = List("d4", "e5"),
          focusFiles = List("d", "e"),
          focusZone = Some("center"),
          beneficiaryPieces = List("R", "Q"),
          confidence = 0.81
        )
      ),
      pieceRoutes = List(
        StrategyPieceRoute(
          ownerSide = "white",
          piece = "R",
          from = "d1",
          route = List("d1", "f1", "f5"),
          purpose = "kingside clamp",
          strategicFit = 0.82,
          tacticalSafety = 0.75,
          surfaceConfidence = 0.8,
          surfaceMode = RouteSurfaceMode.Toward
        )
      ),
      longTermFocus = List("keep the line pressure durable before recovering material"),
      signalDigest = Some(
        NarrativeSignalDigest(
          compensation = Some("return vector through initiative and line pressure"),
          compensationVectors = List("Line Pressure (0.7)", "Delayed Recovery (0.5)"),
          investedMaterial = Some(100),
          dominantIdeaKind = Some(StrategicIdeaKind.LineOccupation),
          dominantIdeaGroup = Some("slow_structural"),
          dominantIdeaReadiness = Some(StrategicIdeaReadiness.Build),
          dominantIdeaFocus = Some("d4, e5")
        )
      )
    )

  private def focusTargetDisplayNormalizationPack: StrategyPack =
    StrategyPack(
      sideToMove = "black",
      strategicIdeas = List(
        StrategyIdeaSignal(
          ideaId = "idea_center_file_shell",
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
          piece = "R",
          from = "h8",
          route = List("h8", "d8", "d3"),
          purpose = "open-file occupation",
          strategicFit = 0.8,
          tacticalSafety = 0.75,
          surfaceConfidence = 0.78,
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
      directionalTargets = List(
        StrategyDirectionalTarget(
          targetId = "target_d4",
          ownerSide = "black",
          piece = "R",
          from = "e8",
          targetSquare = "d4",
          readiness = DirectionalTargetReadiness.Build,
          strategicReasons = List("line access", "central foothold")
        )
      ),
      longTermFocus = List("keep the central targets fixed before recovering material"),
      signalDigest = Some(
        NarrativeSignalDigest(
          compensation = Some("return vector through line pressure and delayed recovery"),
          compensationVectors = List("Line Pressure (0.6)", "Delayed Recovery (0.6)", "Fixed Targets (0.4)"),
          investedMaterial = Some(100),
          dominantIdeaKind = Some(StrategicIdeaKind.LineOccupation),
          dominantIdeaGroup = Some("slow_structural"),
          dominantIdeaReadiness = Some(StrategicIdeaReadiness.Build),
          dominantIdeaFocus = Some("d4, e5")
        )
      )
    )

  private def transitionRescueDisplayNormalizationPack: StrategyPack =
    StrategyPack(
      sideToMove = "black",
      strategicIdeas = List(
        StrategyIdeaSignal(
          ideaId = "idea_trade_then_pressure",
          ownerSide = "black",
          kind = StrategicIdeaKind.FavorableTradeOrTransformation,
          group = "slow_structural",
          readiness = StrategicIdeaReadiness.Build,
          focusSquares = List("e3", "e4"),
          focusFiles = List("d", "e"),
          focusZone = Some("center"),
          beneficiaryPieces = List("Q", "R"),
          confidence = 0.79
        )
      ),
      directionalTargets = List(
        StrategyDirectionalTarget(
          targetId = "target_d3",
          ownerSide = "black",
          piece = "Q",
          from = "f5",
          targetSquare = "d3",
          readiness = DirectionalTargetReadiness.Build,
          strategicReasons = List("central foothold", "line access")
        ),
        StrategyDirectionalTarget(
          targetId = "target_d6",
          ownerSide = "black",
          piece = "R",
          from = "c8",
          targetSquare = "d6",
          readiness = DirectionalTargetReadiness.Build,
          strategicReasons = List("central foothold", "line access")
        )
      ),
      longTermFocus = List("keep the transition under control while delaying recovery"),
      signalDigest = Some(
        NarrativeSignalDigest(
          compensation = Some("return vector through delayed recovery"),
          compensationVectors = List("Delayed Recovery (0.5)", "Return Vector (0.5)"),
          investedMaterial = Some(100),
          dominantIdeaKind = Some(StrategicIdeaKind.FavorableTradeOrTransformation),
          dominantIdeaGroup = Some("slow_structural"),
          dominantIdeaReadiness = Some(StrategicIdeaReadiness.Build),
          dominantIdeaFocus = Some("e3, e4")
        )
      )
    )

  private def lateTargetRescueDisplayNormalizationPack: StrategyPack =
    StrategyPack(
      sideToMove = "white",
      strategicIdeas = List(
        StrategyIdeaSignal(
          ideaId = "idea_queenside_attack_shell",
          ownerSide = "white",
          kind = StrategicIdeaKind.KingAttackBuildUp,
          group = "dynamic_attack",
          readiness = StrategicIdeaReadiness.Build,
          focusSquares = List("f6"),
          focusFiles = List("f"),
          focusZone = Some("kingside"),
          beneficiaryPieces = List("Q", "R"),
          confidence = 0.83
        )
      ),
      pieceRoutes = List(
        StrategyPieceRoute(
          ownerSide = "white",
          piece = "R",
          from = "a1",
          route = List("a1", "a3", "d3"),
          purpose = "kingside clamp",
          strategicFit = 0.81,
          tacticalSafety = 0.76,
          surfaceConfidence = 0.78,
          surfaceMode = RouteSurfaceMode.Toward
        )
      ),
      pieceMoveRefs = List(
        StrategyPieceMoveRef(
          ownerSide = "white",
          piece = "N",
          from = "c4",
          target = "b3",
          idea = "contest the pawn on b3",
          evidence = List("target_pawn")
        )
      ),
      longTermFocus = List("keeping the queenside files under pressure before cashing out"),
      evidence = List("dominant_thesis: durable queenside file pressure"),
      signalDigest = Some(
        NarrativeSignalDigest(
          strategicStack = List("1. Attacking fixed Pawn (0.80)"),
          latentPlan = Some("Attacking fixed Pawn"),
          decisionComparison = Some(
            DecisionComparisonDigest(
              evidence = Some("Further probe work still targets Attacking fixed Pawn through b3 and f3.")
            )
          ),
          compensation = Some("return vector through initiative and line pressure"),
          compensationVectors = List("Line Pressure (0.7)", "Delayed Recovery (0.6)"),
          investedMaterial = Some(100),
          dominantIdeaKind = Some(StrategicIdeaKind.KingAttackBuildUp),
          dominantIdeaGroup = Some("dynamic_attack"),
          dominantIdeaReadiness = Some(StrategicIdeaReadiness.Build),
          dominantIdeaFocus = Some("f6")
        )
      )
    )

  private def centeredLateTargetDisplayNormalizationPack: StrategyPack =
    StrategyPack(
      sideToMove = "white",
      strategicIdeas = List(
        StrategyIdeaSignal(
          ideaId = "idea_center_attack_shell",
          ownerSide = "white",
          kind = StrategicIdeaKind.KingAttackBuildUp,
          group = "dynamic_attack",
          readiness = StrategicIdeaReadiness.Build,
          focusSquares = List("c4", "c1", "g8"),
          focusFiles = List("c", "g"),
          focusZone = Some("kingside"),
          beneficiaryPieces = List("Q", "R"),
          confidence = 0.82
        )
      ),
      pieceRoutes = List(
        StrategyPieceRoute(
          ownerSide = "white",
          piece = "R",
          from = "d1",
          route = List("d1", "f1", "f3", "g3"),
          purpose = "open-file occupation",
          strategicFit = 0.82,
          tacticalSafety = 0.77,
          surfaceConfidence = 0.8,
          surfaceMode = RouteSurfaceMode.Toward
        )
      ),
      pieceMoveRefs = List(
        StrategyPieceMoveRef(
          ownerSide = "white",
          piece = "B",
          from = "g2",
          target = "d4",
          idea = "contest the pawn on d4",
          evidence = List("target_pawn")
        ),
        StrategyPieceMoveRef(
          ownerSide = "white",
          piece = "N",
          from = "f3",
          target = "e5",
          idea = "contest the pawn on e5",
          evidence = List("target_pawn")
        )
      ),
      directionalTargets = List(
        StrategyDirectionalTarget(
          targetId = "target_d3",
          ownerSide = "white",
          piece = "R",
          from = "f3",
          targetSquare = "d3",
          readiness = DirectionalTargetReadiness.Build,
          strategicReasons = List("line access", "central foothold")
        ),
        StrategyDirectionalTarget(
          targetId = "target_e5",
          ownerSide = "white",
          piece = "N",
          from = "f3",
          targetSquare = "e5",
          readiness = DirectionalTargetReadiness.Build,
          strategicReasons = List("line access", "central foothold")
        )
      ),
      longTermFocus = List("keeping the queenside files under pressure before cashing out"),
      evidence = List("dominant_thesis: durable queenside file pressure"),
      signalDigest = Some(
        NarrativeSignalDigest(
          strategicStack = List("1. Attacking fixed Pawn (0.78)"),
          latentPlan = Some("Attacking fixed Pawn"),
          decisionComparison = Some(
            DecisionComparisonDigest(
              evidence = Some("Further probe work still targets Attacking fixed Pawn through d4 and e5.")
            )
          ),
          compensation = Some("return vector through initiative and line pressure"),
          compensationVectors = List("Line Pressure (0.7)", "Delayed Recovery (0.6)"),
          investedMaterial = Some(100),
          dominantIdeaKind = Some(StrategicIdeaKind.KingAttackBuildUp),
          dominantIdeaGroup = Some("dynamic_attack"),
          dominantIdeaReadiness = Some(StrategicIdeaReadiness.Build),
          dominantIdeaFocus = Some("c4, c1, g8")
        )
      )
    )

  private def rookAccessLineOccupationLockPack: StrategyPack =
    StrategyPack(
      sideToMove = "white",
      strategicIdeas = List(
        StrategyIdeaSignal(
          ideaId = "idea_line_pressure_shell",
          ownerSide = "white",
          kind = StrategicIdeaKind.LineOccupation,
          group = "slow_structural",
          readiness = StrategicIdeaReadiness.Build,
          focusSquares = List("f1", "c6", "d3", "d6"),
          focusFiles = List("c", "d", "f"),
          focusZone = Some("queenside"),
          beneficiaryPieces = List("R", "B"),
          confidence = 0.81
        )
      ),
      pieceRoutes = List(
        StrategyPieceRoute(
          ownerSide = "white",
          piece = "B",
          from = "d4",
          route = List("d4", "c5"),
          purpose = "kingside pressure",
          strategicFit = 0.8,
          tacticalSafety = 0.76,
          surfaceConfidence = 0.79,
          surfaceMode = RouteSurfaceMode.Toward
        )
      ),
      pieceMoveRefs = List(
        StrategyPieceMoveRef(
          ownerSide = "white",
          piece = "B",
          from = "d4",
          target = "c6",
          idea = "contest the pawn on c6",
          evidence = List("target_pawn")
        ),
        StrategyPieceMoveRef(
          ownerSide = "white",
          piece = "B",
          from = "d4",
          target = "d6",
          idea = "contest the pawn on d6",
          evidence = List("target_pawn")
        )
      ),
      directionalTargets = List(
        StrategyDirectionalTarget(
          targetId = "target_c6",
          ownerSide = "white",
          piece = "R",
          from = "d4",
          targetSquare = "c6",
          readiness = DirectionalTargetReadiness.Build,
          strategicReasons = List("supports attacking fixed pawn", "central foothold", "line access")
        )
      ),
      longTermFocus = List("objective: work toward making c6 available for the rook"),
      evidence = List("isolated pawn can be fixed", "backward pawn can be fixed"),
      signalDigest = Some(
        NarrativeSignalDigest(
          strategicStack = List("1. immediate tactical gain Counterplay (0.75)", "2. Attacking fixed Pawn (0.78)"),
          latentPlan = Some("Rook-pawn march to gain flank space"),
          decisionComparison = Some(
            DecisionComparisonDigest(
              evidence = Some("Further probe work still targets Rook-pawn march to gain flank space through Rg3.")
            )
          ),
          compensation = Some("return vector through initiative and line pressure"),
          compensationVectors = List("Initiative (0.6)", "Line Pressure (0.6)"),
          investedMaterial = Some(100),
          dominantIdeaKind = Some(StrategicIdeaKind.LineOccupation),
          dominantIdeaGroup = Some("slow_structural"),
          dominantIdeaReadiness = Some(StrategicIdeaReadiness.Build),
          dominantIdeaFocus = Some("f1, c6, d3, d6")
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

  test("compensation lens drives bookmaker prose for long-term investment motif") {
    val ctx = baseContext.copy(
      semantic = Some(
        SemanticSection(
          structuralWeaknesses = Nil,
          pieceActivity = Nil,
          positionalFeatures = Nil,
          compensation = Some(
            CompensationInfo(
              investedMaterial = 120,
              returnVector = Map("Attack on King" -> 1.2, "Space Advantage" -> 0.8),
              expiryPly = None,
              conversionPlan = "Mating Attack"
            )
          ),
          endgameFeatures = None,
          practicalAssessment = None,
          preventedPlans = Nil,
          conceptSummary = Nil
        )
      ),
      whyAbsentFromTopMultiPV = List("the direct recapture loses the initiative"),
      authorEvidence = List(
        QuestionEvidence(
          questionId = "q1",
          purpose = "free_tempo_branches",
          branches = List(EvidenceBranch("...Qe7", "Qe7 h5 Rh6", Some(68), None, Some(20), Some("probe-1")))
        )
      )
    )

    val thesis = StrategicThesisBuilder.build(ctx).getOrElse(fail("missing truth-first thesis"))
    assertNotEquals(thesis.lens, StrategicLens.Compensation)
    val claimLow = thesis.claim.toLowerCase
    assert(claimLow.nonEmpty, clue(thesis.claim))
    assert(!claimLow.contains("gives up material"), clue(thesis.claim))
    assert(!claimLow.contains("winning it back"), clue(thesis.claim))
    assert(!claimLow.contains("material can wait"), clue(thesis.claim))
  }

  test("strategy-pack compensation fallback survives without semantic compensation") {
    val thesis =
      StrategicThesisBuilder
        .build(baseContext, Some(surfaceDrivenPack(compensation = Some("initiative against the king"), investedMaterial = Some(180))))
        .getOrElse(fail("missing compensation thesis from strategy pack"))

    assertEquals(thesis.lens, StrategicLens.Compensation)
    val claimLow = thesis.claim.toLowerCase
    assert(
      claimLow.contains("material can wait") ||
      claimLow.contains("recover the material") ||
        claimLow.contains("gives up material") ||
        claimLow.contains("down material"),
      clue(thesis.claim)
    )
    assert(claimLow.contains("initiative") || claimLow.contains("pressure") || claimLow.contains("attack"), clue(thesis.claim))
    assert(
      claimLow.contains("g7") ||
        claimLow.contains("recover the material") ||
        claimLow.contains("winning it back") ||
        claimLow.contains("against the king") ||
        claimLow.contains("can head for"),
      clue(thesis.claim)
    )
    assert(
      claimLow.contains("initiative against the king") ||
        thesis.support.exists(text => {
          val low = text.toLowerCase
          low.contains("initiative") || low.contains("winning the material back") || low.contains("favorable exchanges")
        })
    )
    assert(thesis.support.headOption.exists(_.nonEmpty), clue(thesis.support))
  }

  test("strategy-pack compensation vectors drive thesis wording without semantic compensation") {
    val thesis =
      StrategicThesisBuilder
        .build(
          baseContext,
          Some(
            surfaceDrivenPack(
              compensationVectors = List("Initiative (0.7)", "Line Pressure (0.6)", "Delayed Recovery (0.5)"),
              investedMaterial = Some(200)
            )
          )
        )
        .getOrElse(fail("missing compensation thesis from digest vectors"))

    assertEquals(thesis.lens, StrategicLens.Compensation)
    val claimLow = thesis.claim.toLowerCase
    val rendered = (thesis.claim :: thesis.support).mkString(" ").toLowerCase
    assert(
      claimLow.contains("material can wait") ||
      claimLow.contains("recover the material") ||
        claimLow.contains("gives up material") ||
        claimLow.contains("down material"),
      clue(thesis.claim)
    )
    assert(
      rendered.contains("initiative") ||
        rendered.contains("pressure") ||
        rendered.contains("open lines") ||
        rendered.contains("open files") ||
        rendered.contains("targets"),
      clue(rendered)
    )
    assert(
      rendered.contains("g7") ||
        rendered.contains("recover the material") ||
        rendered.contains("winning it back") ||
        rendered.contains("open lines") ||
        rendered.contains("open files") ||
        rendered.contains("targets") ||
        rendered.contains("can head for"),
      clue(rendered)
    )
    assert(
      thesis.support.exists(text => {
        val low = text.toLowerCase
        low.contains("open files") ||
          low.contains("open lines") ||
          low.contains("pressure along the open files") ||
          low.contains("dragged back to the king") ||
          low.contains("defenders keep getting dragged back")
      }),
      clue(thesis.support)
    )
    assert(
      thesis.support.exists(text =>
        text.toLowerCase.contains("winning the material back") ||
          text.toLowerCase.contains("favorable exchanges") ||
          text.toLowerCase.contains("durable") ||
          text.toLowerCase.contains("under control") ||
          text.toLowerCase.contains("open lines") ||
          text.toLowerCase.contains("before recovering the material") ||
          text.toLowerCase.contains("keep the compensation alive")
      ),
      clue(thesis.support)
    )
  }

  test("recapture-neutralized current compensation does not leak into bookmaker prose") {
    val ctx = baseContext.copy(
      fen = "3r3k/2pq3p/1p3pr1/p1p1p2Q/P1P1PB2/3P1P2/2P3P1/R4R1K b - - 0 30",
      playedMove = Some("e5f4"),
      playedSan = Some("exf4"),
      summary = NarrativeSummary("Recapture", None, "NarrowChoice", "Maintain", "-0.49"),
      semantic = Some(
        SemanticSection(
          structuralWeaknesses = Nil,
          pieceActivity = Nil,
          positionalFeatures = Nil,
          compensation = Some(
            CompensationInfo(
              investedMaterial = 300,
              returnVector = Map(
                "Initiative" -> 0.6,
                "Line Pressure" -> 0.6,
                "Delayed Recovery" -> 0.4
              ),
              expiryPly = None,
              conversionPlan = "return vector through initiative and line pressure"
            )
          ),
          endgameFeatures = None,
          practicalAssessment = None,
          preventedPlans = Nil,
          conceptSummary = Nil
        )
      )
    )

    val thesis = StrategicThesisBuilder.build(ctx)
    assert(thesis.forall(_.lens != StrategicLens.Compensation), clue(thesis))

    val prose = BookStyleRenderer.render(ctx).toLowerCase
    assert(!prose.contains("compensation investment"), clue(prose))
    assert(!prose.contains("the move keeps the compensation alive"), clue(prose))
    assert(!prose.contains("cash out through"), clue(prose))
  }

  test("quiet positional compensation keeps subtype-specific file pressure language") {
    val thesis =
      StrategicThesisBuilder
        .build(baseContext, Some(quietCompensationPack))
        .getOrElse(fail("missing quiet compensation thesis"))

    assertEquals(thesis.lens, StrategicLens.Compensation)
    assert(
      thesis.claim.toLowerCase.contains("queenside targets under pressure") ||
        thesis.claim.toLowerCase.contains("queenside targets") ||
        thesis.claim.toLowerCase.contains("queenside file pressure"),
      clue(thesis.claim)
    )
    assert(!thesis.claim.toLowerCase.contains("attack on king"))
    assert(
      thesis.support.exists(text =>
        text.toLowerCase.contains("file pressure") ||
          text.toLowerCase.contains("open lines") ||
          text.toLowerCase.contains("targets")
      ),
      clue(thesis.support)
    )
    assert(
      thesis.support.exists(text =>
        text.toLowerCase.contains("winning the material back") ||
          text.toLowerCase.contains("targets") ||
          text.toLowerCase.contains("favorable exchanges")
      ),
      clue(thesis.support)
    )
  }

  test("Benko-like compensation stays queenside and target-led despite central support squares") {
    val surface = StrategyPackSurface.from(Some(benkoLikeCompensationPack))
    assertEquals(surface.compensationSubtype.map(_.pressureTheater), Some("queenside"))
    assertEquals(surface.compensationSubtype.map(_.pressureMode), Some("target_fixing"))
    assertEquals(
      StrategyPackSurface.compensationSubtypeLabel(surface),
      Some("queenside/target_fixing/intentionally_deferred/durable_pressure")
    )
    assert(surface.normalizationActive)
    assert(surface.normalizationConfidence >= 6)
    assertEquals(surface.dominantIdeaText, Some("fixed queenside targets"))
    assert(surface.executionText.exists(_.toLowerCase.contains("queenside targets")), clue(surface.executionText))
    assert(!surface.executionText.exists(_.toLowerCase.contains("kingside clamp")), clue(surface.executionText))
    assert(
      surface.focusText.exists(text =>
        text.toLowerCase.contains("queenside targets") || text.toLowerCase.contains("queenside files")
      ),
      clue(surface.focusText)
    )

    val thesis =
      StrategicThesisBuilder
        .build(baseContext, Some(benkoLikeCompensationPack))
        .getOrElse(fail("missing Benko-like compensation thesis"))

    assert(
      thesis.claim.toLowerCase.contains("queenside targets under pressure") ||
        thesis.claim.toLowerCase.contains("queenside targets"),
      clue(thesis.claim)
    )
    assert(!thesis.claim.toLowerCase.contains("kingside clamp"), clue(thesis.claim))
    assert(
      thesis.support.exists(text =>
        text.toLowerCase.contains("queenside targets") || text.toLowerCase.contains("queenside file pressure")
      ),
      clue(thesis.support)
    )
  }

  test("display subtype promotes target-fixing when fixed-target anchors dominate the raw line pressure shell") {
    val surface = StrategyPackSurface.from(Some(targetDisplayNormalizationPack))

    assertEquals(surface.compensationSubtype.map(_.pressureMode), Some("line_occupation"))
    assertEquals(
      StrategyPackSurface.compensationSubtypeLabel(surface),
      Some("center/target_fixing/intentionally_deferred/durable_pressure")
    )
    assertEquals(surface.dominantIdeaText, Some("fixed central targets"))
    assert(
      surface.executionText.exists(_.toLowerCase.contains("fixed central targets")),
      clue(surface.executionText)
    )
    assert(
      CompensationDisplayPhrasing.compensationWhyNowText(surface).exists(_.toLowerCase.contains("central targets")),
      clue(CompensationDisplayPhrasing.compensationWhyNowText(surface))
    )
  }

  test("display subtype promotes kingside theater when the pressure route lands on the flank") {
    val surface = StrategyPackSurface.from(Some(kingsideDisplayNormalizationPack))

    assertEquals(surface.compensationSubtype.map(_.pressureTheater), Some("center"))
    assertEquals(
      StrategyPackSurface.compensationSubtypeLabel(surface),
      Some("kingside/line_occupation/intentionally_deferred/durable_pressure")
    )
    assertEquals(surface.dominantIdeaText, Some("durable pressure along the files"))
    assert(
      surface.executionText.exists(_.toLowerCase.contains("f5")),
      clue(surface.executionText)
    )
    assert(
      CompensationDisplayPhrasing.compensationWhyNowText(surface).exists(text => {
        val low = text.toLowerCase
        (low.contains("gives up material") && low.contains("head for f5")) ||
        (low.contains("gives up material") && low.contains("open files")) ||
        low.contains("recover the material")
      }),
      clue(CompensationDisplayPhrasing.compensationWhyNowText(surface))
    )
  }

  test("display subtype prefers target-fixing when the deferred focus explicitly fixes central targets") {
    val surface = StrategyPackSurface.from(Some(focusTargetDisplayNormalizationPack))

    assertEquals(
      StrategyPackSurface.compensationSubtypeLabel(surface),
      Some("center/target_fixing/intentionally_deferred/durable_pressure")
    )
    assertEquals(surface.dominantIdeaText, Some("fixed central targets"))
    assert(
      surface.executionText.exists(_.toLowerCase.contains("fixed central targets")),
      clue(surface.executionText)
    )
  }

  test("late fixed-target hints keep the raw attack-led fallback when rescue conditions are not met") {
    val surface = StrategyPackSurface.from(Some(lateTargetRescueDisplayNormalizationPack))

    assertEquals(
      StrategyPackSurface.compensationSubtypeLabel(surface),
      Some("queenside/target_fixing/intentionally_deferred/durable_pressure")
    )
    assertEquals(surface.displaySubtypeSource, "raw_fallback")
    assertEquals(surface.dominantIdeaText, Some("pressure on f6"))
    assert(!surface.executionText.exists(_.toLowerCase.contains("fixed queenside targets")), clue(surface.executionText))
  }

  test("target-fixing theater follows fixed-target anchors instead of inherited queenside file shell text") {
    val surface = StrategyPackSurface.from(Some(centeredLateTargetDisplayNormalizationPack))

    assert(
      StrategyPackSurface.compensationSubtypeLabel(surface).exists(_.startsWith("center/")),
      clue(StrategyPackSurface.compensationSubtypeLabel(surface))
    )
    assertEquals(surface.displaySubtypeSource, "raw_fallback")
    assert(
      surface.dominantIdeaText.exists(text =>
        text.toLowerCase.contains("pressure on c4") || text.toLowerCase.contains("pressure on c1")
      ),
      clue(surface.dominantIdeaText)
    )
    assert(
      surface.executionText.exists(text =>
        text.toLowerCase.contains("g3") || text.toLowerCase.contains("open file occupation")
      ),
      clue(surface.executionText)
    )
  }

  test("rook-access line shells stay line-occupation despite late fixed-pawn hints") {
    val surface = StrategyPackSurface.from(Some(rookAccessLineOccupationLockPack))

    assertEquals(
      StrategyPackSurface.compensationSubtypeLabel(surface),
      Some("queenside/line_occupation/intentionally_deferred/durable_pressure")
    )
    assertEquals(surface.dominantIdeaText, Some("queenside file pressure"))
    assert(
      surface.executionText.exists(_.toLowerCase.contains("queenside files")),
      clue(surface.executionText)
    )
  }

  test("transition-only raw subtype is rescued into durable central line pressure when structural anchors persist") {
    val surface = StrategyPackSurface.from(Some(transitionRescueDisplayNormalizationPack))

    assertEquals(surface.compensationSubtype.map(_.pressureMode), Some("conversion_window"))
    assertEquals(surface.compensationSubtype.map(_.stabilityClass), Some("transition_only"))
    assertEquals(
      StrategyPackSurface.compensationSubtypeLabel(surface),
      Some("center/line_occupation/intentionally_deferred/durable_pressure")
    )
    assert(surface.normalizationActive, clue(surface.displayNormalization))
    assertEquals(surface.dominantIdeaText, Some("central file pressure"))
    assert(
      surface.executionText.exists(_.toLowerCase.contains("central files")),
      clue(surface.executionText)
    )
  }

  test("attack-led kingside shells can still adopt deferred display subtype when the investment is explicitly held") {
    val surface = StrategyPackSurface.from(Some(attackRecoveryDisplayNormalizationPack))

    assert(surface.compensationSubtype.nonEmpty, clue(surface.compensationSubtype))
    assertEquals(
      StrategyPackSurface.compensationSubtypeLabel(surface),
      Some("kingside/line_occupation/intentionally_deferred/durable_pressure")
    )
    assert(surface.normalizationActive, clue(surface.displayNormalization))
    assert(
      surface.executionText.exists(_.toLowerCase.contains("f3")),
      clue(surface.executionText)
    )
    assert(
      CompensationDisplayPhrasing.compensationWhyNowText(surface).exists(text => {
        val low = text.toLowerCase
        low.contains("material can wait") ||
        low.contains("recover the material") ||
        low.contains("gives up material") ||
        low.contains("winning it back")
      }),
      clue(CompensationDisplayPhrasing.compensationWhyNowText(surface))
    )
  }

  test("attack-led compensation keeps raw kingside attack wording when normalization confidence is low") {
    val surface = StrategyPackSurface.from(BookmakerProseGoldenFixtures.exchangeSacrifice.strategyPack)

    assert(!surface.normalizationActive, clue(surface.displayNormalization))
    assert(
      surface.dominantIdeaText.exists(text =>
        text.toLowerCase.contains("pressure on") || text.toLowerCase.contains("attacking chances")
      ),
      clue(surface.dominantIdeaText)
    )
    assert(
      surface.executionText.exists(text =>
        text.toLowerCase.contains("h5") || text.toLowerCase.contains("mate threats")
      ),
      clue(surface.executionText)
    )
  }

  test("prophylaxis lens leads when counterplay denial is the key point") {
    val ctx = baseContext.copy(
      semantic = Some(
        SemanticSection(
          structuralWeaknesses = Nil,
          pieceActivity = Nil,
          positionalFeatures = Nil,
          compensation = None,
          endgameFeatures = None,
          practicalAssessment = None,
          preventedPlans = List(
            PreventedPlanInfo(
              planId = "Queenside Counterplay",
              deniedSquares = Nil,
              breakNeutralized = Some("c5"),
              mobilityDelta = 0,
              counterplayScoreDrop = 140,
              preventedThreatType = Some("counterplay")
            )
          ),
          conceptSummary = Nil
        )
      )
    )

    val thesis = StrategicThesisBuilder.build(ctx).getOrElse(fail("missing prophylaxis thesis"))
    assertEquals(thesis.lens, StrategicLens.Prophylaxis)
    assert(
      thesis.claim.toLowerCase.contains("stopping ...c5") ||
        thesis.claim.toLowerCase.contains("slowing down"),
      clue(thesis.claim)
    )

    val prose = BookStyleRenderer.render(ctx)
    val paras = paragraphs(prose)
    assertEquals(paras.size, 2)
    assert(
      paras.head.toLowerCase.contains("...c5") ||
        paras.head.toLowerCase.contains("slowing down") ||
        paras.head.toLowerCase.contains("opponent out of"),
      clue(paras.head)
    )
    assert(paras(1).toLowerCase.contains("...c5") || paras(1).toLowerCase.contains("entry"))
    assert(!paras(1).contains("Kingside expansion"))
  }

  test("threat-line prophylaxis stays out of the main thesis and keeps SAN citation in prose") {
    val ctx = baseContext.copy(
      semantic = Some(
        SemanticSection(
          structuralWeaknesses = Nil,
          pieceActivity = Nil,
          positionalFeatures = Nil,
          compensation = None,
          endgameFeatures = None,
          practicalAssessment = None,
          preventedPlans = List(
            PreventedPlanInfo(
              planId = "Queenside Counterplay",
              deniedSquares = Nil,
              breakNeutralized = Some("c5"),
              mobilityDelta = 0,
              counterplayScoreDrop = 140,
              preventedThreatType = Some("counterplay"),
              sourceScope = FactScope.ThreatLine,
              citationLine = Some("12...Bf5 13.Nc3 13...Qa5")
            )
          ),
          conceptSummary = Nil
        )
      )
    )

    val thesis = StrategicThesisBuilder.build(ctx)
    assert(!thesis.exists(_.lens == StrategicLens.Prophylaxis), clue(thesis))

    val prose = BookStyleRenderer.render(ctx).toLowerCase
    assert(prose.contains("after 12...bf5 13.nc3 13...qa5"))
    assert(!prose.contains("cutting out counterplay"))
  }

  test("structure lens names the structure and plan fit instead of flattening it") {
    val ctx = baseContext.copy(
      playedMove = Some("a1b1"),
      playedSan = Some("Rb1"),
      semantic = Some(
        SemanticSection(
          structuralWeaknesses = Nil,
          pieceActivity = List(
            PieceActivityInfo(
              piece = "Rook",
              square = "a1",
              mobilityScore = 0.40,
              isTrapped = false,
              isBadBishop = false,
              keyRoutes = List("b1", "b3"),
              coordinationLinks = List("b4")
            )
          ),
          positionalFeatures = Nil,
          compensation = None,
          endgameFeatures = None,
          practicalAssessment = None,
          preventedPlans = Nil,
          conceptSummary = Nil,
          structureProfile = Some(
            StructureProfileInfo(
              primary = "Carlsbad",
              confidence = 0.84,
              alternatives = Nil,
              centerState = "Locked",
              evidenceCodes = List("MAJORITY")
            )
          ),
          planAlignment = Some(
            PlanAlignmentInfo(
              score = 61,
              band = "Playable",
              matchedPlanIds = List("minority_attack"),
              missingPlanIds = List("central_break"),
              reasonCodes = List("PRECOND_MISS"),
              narrativeIntent = Some("play around queenside pressure"),
              narrativeRisk = Some("counterplay if move order slips")
            )
          )
        )
      ),
      mainStrategicPlans = List(
        PlanHypothesis(
          planId = "minority_attack",
          planName = "Minority Attack",
          rank = 1,
          score = 0.88,
          preconditions = Nil,
          executionSteps = Nil,
          failureModes = Nil,
          viability = PlanViability(0.82, "high", "slow"),
          themeL1 = "minority_attack"
        )
      )
    )

    val thesis = StrategicThesisBuilder.build(ctx).getOrElse(fail("missing structure thesis"))
    assertEquals(thesis.lens, StrategicLens.Structure)
    assert(thesis.claim.contains("Carlsbad"))
    assert(thesis.claim.toLowerCase.contains("minority attack"))
    assert(thesis.claim.toLowerCase.contains("rook"))
    assert(thesis.claim.toLowerCase.contains("b-file"))
    assert(thesis.support.exists(_.toLowerCase.contains("queenside pressure")))
    assert(thesis.support.exists(_.toLowerCase.contains("starts that route immediately")))

    val prose = BookStyleRenderer.render(ctx)
    val paras = paragraphs(prose)
    assertEquals(paras.size, 3)
    assert(paras.head.contains("Carlsbad"))
    assert(paras(1).toLowerCase.contains("queenside pressure"))
    assert(paras(1).toLowerCase.contains("starts that route immediately"))
    assert(paras(2).toLowerCase.contains("move order"))
  }

  test("off-plan structure keeps deployment as caution instead of the main claim") {
    val ctx = baseContext.copy(
      playedMove = Some("a1b1"),
      playedSan = Some("Rb1"),
      semantic = Some(
        SemanticSection(
          structuralWeaknesses = Nil,
          pieceActivity = List(
            PieceActivityInfo(
              piece = "Rook",
              square = "a1",
              mobilityScore = 0.38,
              isTrapped = false,
              isBadBishop = false,
              keyRoutes = List("b1", "b3"),
              coordinationLinks = List("b4")
            )
          ),
          positionalFeatures = Nil,
          compensation = None,
          endgameFeatures = None,
          practicalAssessment = None,
          preventedPlans = Nil,
          conceptSummary = Nil,
          structureProfile = Some(
            StructureProfileInfo(
              primary = "Carlsbad",
              confidence = 0.84,
              alternatives = Nil,
              centerState = "Locked",
              evidenceCodes = List("MAJORITY")
            )
          ),
          planAlignment = Some(
            PlanAlignmentInfo(
              score = 34,
              band = "OffPlan",
              matchedPlanIds = Nil,
              missingPlanIds = List("minority_attack"),
              reasonCodes = List("ANTI_PLAN"),
              narrativeIntent = Some("play around queenside pressure"),
              narrativeRisk = Some("the move order fights the structure")
            )
          )
        )
      ),
      mainStrategicPlans = List(
        PlanHypothesis(
          planId = "minority_attack",
          planName = "Minority Attack",
          rank = 1,
          score = 0.88,
          preconditions = Nil,
          executionSteps = Nil,
          failureModes = Nil,
          viability = PlanViability(0.82, "high", "slow"),
          themeL1 = "minority_attack"
        )
      )
    )

    val thesis = StrategicThesisBuilder.build(ctx).getOrElse(fail("missing structure thesis"))
    assertEquals(thesis.lens, StrategicLens.Structure)
    assert(thesis.claim.contains("Carlsbad"))
    assert(!thesis.claim.toLowerCase.contains("rook belongs on the b-file"))
    assert(thesis.support.exists(_.toLowerCase.contains("still wants the b-file")))
  }

  test("structure lens treats pv-coupled main plans as conditional and avoids naming them in the claim") {
    val ctx = baseContext.copy(
      playedMove = Some("a1b1"),
      playedSan = Some("Rb1"),
      semantic = Some(
        SemanticSection(
          structuralWeaknesses = Nil,
          pieceActivity = List(
            PieceActivityInfo(
              piece = "Rook",
              square = "a1",
              mobilityScore = 0.40,
              isTrapped = false,
              isBadBishop = false,
              keyRoutes = List("b1", "b3"),
              coordinationLinks = List("b4")
            )
          ),
          positionalFeatures = Nil,
          compensation = None,
          endgameFeatures = None,
          practicalAssessment = None,
          preventedPlans = Nil,
          conceptSummary = Nil,
          structureProfile = Some(
            StructureProfileInfo(
              primary = "Carlsbad",
              confidence = 0.84,
              alternatives = Nil,
              centerState = "Locked",
              evidenceCodes = List("MAJORITY")
            )
          ),
          planAlignment = Some(
            PlanAlignmentInfo(
              score = 61,
              band = "Playable",
              matchedPlanIds = List("minority_attack"),
              missingPlanIds = Nil,
              reasonCodes = List("PA_MATCH"),
              narrativeIntent = Some("queenside pressure"),
              narrativeRisk = Some("counterplay if move order slips")
            )
          )
        )
      ),
      mainStrategicPlans = List(
        PlanHypothesis(
          planId = "minority_attack",
          planName = "Minority Attack",
          rank = 1,
          score = 0.88,
          preconditions = Nil,
          executionSteps = Nil,
          failureModes = Nil,
          viability = PlanViability(0.82, "high", "slow"),
          themeL1 = "minority_attack"
        )
      ),
      strategicPlanExperiments = List(
        planExperiment(
          planId = "minority_attack",
          evidenceTier = "pv_coupled",
          moveOrderSensitive = true
        )
      )
    )

    val thesis = StrategicThesisBuilder.build(ctx).getOrElse(fail("missing conditional structure thesis"))
    assertEquals(thesis.lens, StrategicLens.Structure)
    assert(thesis.claim.contains("Carlsbad"), clue(thesis.claim))
    assert(thesis.claim.toLowerCase.contains("queenside pressure"), clue(thesis.claim))
    assert(!thesis.claim.toLowerCase.contains("minority attack"), clue(thesis.claim))
    assert(!thesis.claim.toLowerCase.contains("kingside expansion"), clue(thesis.claim))
  }

  test("decision lens makes the chosen route and deferred alternative explicit") {
    val ctx = baseContext.copy(
      decision = Some(
        DecisionRationale(
          focalPoint = Some(TargetSquare("g7")),
          logicSummary = "Resolve back-rank mate -> create pressure on g7",
          delta = PVDelta(
            resolvedThreats = List("back-rank mate"),
            newOpportunities = List("g7"),
            planAdvancements = List("Met: rook lift"),
            concessions = List("dark-square drift")
          ),
          confidence = ConfidenceLevel.Probe
        )
      ),
      whyAbsentFromTopMultiPV = List("the immediate 'g4' push loses 220 cp"),
      authorEvidence = List(
        QuestionEvidence(
          questionId = "q2",
          purpose = "latent_plan_refutation",
          branches = List(EvidenceBranch("...Qf6", "Qf6 g4 Qxd4", Some(-220), None, Some(22), Some("probe-2")))
        )
      )
    )

    val thesis = StrategicThesisBuilder.build(ctx).getOrElse(fail("missing decision thesis"))
    assertEquals(thesis.lens, StrategicLens.Decision)
    assert(!thesis.claim.toLowerCase.contains("the key decision is to choose"))
    assert(!thesis.claim.toLowerCase.contains("the whole decision turns on"))

    val prose = BookStyleRenderer.render(ctx)
    val paras = paragraphs(prose)
    assertEquals(paras.size, 3)
    assert(paras.head.toLowerCase.contains("because"))
    assert(paras(1).toLowerCase.contains("resolving back-rank mate"))
    assert(paras(2).contains("Probe evidence"))
    assert(paras(2).toLowerCase.contains("immediate"))
  }

  test("decision lens uses strategy-pack thesis before generic route scaffolding when available") {
    val ctx = baseContext.copy(
      decision = Some(
        DecisionRationale(
          focalPoint = Some(TargetSquare("g7")),
          logicSummary = "contest the c-file -> switch the rook to g3 -> pressure g7",
          delta = PVDelta(
            resolvedThreats = List("back-rank mate"),
            newOpportunities = List("g7"),
            planAdvancements = List("Met: rook lift"),
            concessions = List("queenside simplification")
          ),
          confidence = ConfidenceLevel.Probe
        )
      ),
      whyAbsentFromTopMultiPV = List("the immediate 'Qh5' thrust lets Black trade queens and kill the attack"),
      authorEvidence = List(
        QuestionEvidence(
          questionId = "q-open-file",
          purpose = "latent_plan_refutation",
          branches = List(
            EvidenceBranch("...Rc8", "Rc8 Rc3 Rg6", Some(42), None, Some(23), Some("probe-open-file"))
          )
        )
      )
    )

    val thesis =
      StrategicThesisBuilder
        .build(ctx, Some(surfaceDrivenPack()))
        .getOrElse(fail("missing strategy-pack-backed decision thesis"))

    assertEquals(thesis.lens, StrategicLens.Decision)
    assert(!thesis.claim.contains("The key decision is to choose"))
    assert(!thesis.claim.toLowerCase.contains("rather than drifting into"), clue(thesis.claim))
    assert(!thesis.claim.toLowerCase.contains("focused on"), clue(thesis.claim))
    assert(thesis.claim.toLowerCase.contains("g7"))
    assert(!thesis.claim.toLowerCase.contains("execution"))
    assert(
      thesis.support.exists(text =>
        text.toLowerCase.contains("a concrete target is") ||
          text.toLowerCase.contains("a likely follow-up is") ||
          text.toLowerCase.contains("can head for")
      ),
      clue(thesis.support)
    )
    assert(thesis.support.exists(_.toLowerCase.contains("stays secondary because")))
    assert(!thesis.support.exists(_.toLowerCase.contains("the whole decision turns on")))
  }

  test("compensation-flavored decision surface avoids whole-decision fallback and uses compensation lexicon") {
    val ctx = baseContext.copy(
      decision = Some(
        DecisionRationale(
          focalPoint = Some(TargetSquare("g7")),
          logicSummary = "switch the rook and keep the attack alive",
          delta = PVDelta(
            resolvedThreats = List("trade into a worse ending"),
            newOpportunities = List("g7"),
            planAdvancements = List("Met: line pressure"),
            concessions = Nil
          ),
          confidence = ConfidenceLevel.Probe
        )
      ),
      whyAbsentFromTopMultiPV = List("the direct recapture kills the attack"),
      authorEvidence = List(
        QuestionEvidence(
          questionId = "q-comp-surface",
          purpose = "latent_plan_refutation",
          branches = List(EvidenceBranch("...Qe7", "Qe7 h5 Rh6", Some(68), None, Some(21), Some("probe-comp-surface")))
        )
      )
    )

    val thesis =
      StrategicThesisBuilder
        .build(ctx, Some(surfaceDrivenPack(compensation = Some("initiative against the king"), investedMaterial = Some(180))))
        .getOrElse(fail("missing compensation-flavored decision thesis"))

    val claimLow = thesis.claim.toLowerCase
    assert(!claimLow.contains("the whole decision turns on"))
    assert(claimLow.contains("compensation") || claimLow.contains("initiative") || claimLow.contains("pressure") || claimLow.contains("attack"))
    assert(thesis.support.exists(text => {
      val low = text.toLowerCase
      low.contains("winning the material back") ||
        low.contains("compensation") ||
        low.contains("initiative") ||
        low.contains("favorable exchanges") ||
        low.contains("attack") ||
        low.contains("this works only while") ||
        low.contains("head for") ||
        low.contains("pressure on")
    }))
  }

  test("compensation thesis support uses salvageable coach-style sentences") {
    val thesis =
      StrategicThesisBuilder
        .build(baseContext, Some(benkoLikeCompensationPack))
        .getOrElse(fail("missing compensation thesis"))

    val rendered = (thesis.claim :: thesis.support).mkString(" ").toLowerCase
    assert(!rendered.contains("while aiming for the knight can head for"), clue(rendered))
    assert(!rendered.contains("via queen toward"), clue(rendered))
    assert(!rendered.contains("the play still runs through"), clue(rendered))
    assert(!rendered.contains("pressure keeps building through"), clue(rendered))
    assert(
      rendered.contains("material can wait") ||
        rendered.contains("winning the material back") ||
        rendered.contains("gives up material"),
      clue(rendered)
    )
  }

  test("weak compensation shell reframes to ordinary move-purpose instead of forcing compensation prose") {
    val ctx = baseContext.copy(
      decision = Some(
        DecisionRationale(
          focalPoint = Some(TargetSquare("g7")),
          logicSummary = "keep the kingside pressure coordinated",
          delta = PVDelta(
            resolvedThreats = List("trade into a worse ending"),
            newOpportunities = List("g7"),
            planAdvancements = Nil,
            concessions = Nil
          ),
          confidence = ConfidenceLevel.Probe
        )
      )
    )

    val thesis =
      StrategicThesisBuilder
        .build(ctx, Some(weakCompensationPack))
        .getOrElse(fail("missing reframed thesis"))

    assertNotEquals(thesis.lens, StrategicLens.Compensation)
    val rendered = (thesis.claim :: thesis.support).mkString(" ").toLowerCase
    assert(!rendered.contains("material can wait"), clue(rendered))
    assert(!rendered.contains("winning the material back"), clue(rendered))
    assert(!rendered.contains("gives up material"), clue(rendered))
  }

  test("compensation thesis drops repeated recovery wording when claim and support say the same thing") {
    val thesis =
      StrategicThesisBuilder
        .build(baseContext, Some(duplicateCompensationPack))
        .getOrElse(fail("missing duplicate-compensation thesis"))

    val rendered = (thesis.claim :: thesis.support).mkString(" ").toLowerCase
    assertEquals(rendered.split("winning the material back can wait because", -1).length - 1, 0, clue(rendered))
    assertEquals(rendered.split("bringing the queen to d3", -1).length - 1, 0, clue(rendered))
    assert(rendered.contains("pressure on d3"), clue(rendered))
  }

  test("practical lens foregrounds workload drivers over tiny eval edges") {
    val ctx = baseContext.copy(
      semantic = Some(
        SemanticSection(
          structuralWeaknesses = Nil,
          pieceActivity = Nil,
          positionalFeatures = Nil,
          compensation = None,
          endgameFeatures = None,
          practicalAssessment = Some(
            PracticalInfo(
              engineScore = 18,
              practicalScore = 74.0,
              verdict = "Comfortable",
              biasFactors = List(
                PracticalBiasInfo("Mobility", "Diff: 1.8", 36.0),
                PracticalBiasInfo("Forgiveness", "2 safe moves", -18.0)
              )
            )
          ),
          preventedPlans = Nil,
          conceptSummary = Nil
        )
      )
    )

    val thesis = StrategicThesisBuilder.build(ctx).getOrElse(fail("missing practical thesis"))
    assertEquals(thesis.lens, StrategicLens.Practical)
    assert(thesis.claim.toLowerCase.contains("easier to handle"), clue(thesis.claim))

    val prose = BookStyleRenderer.render(ctx)
    val paras = paragraphs(prose)
    assertEquals(paras.size, 2)
    assert(paras.head.toLowerCase.contains("easier to handle"))
    assert(
      paras(1).toLowerCase.contains("pieces have more room") ||
        paras(1).toLowerCase.contains("safe follow-up moves"),
      clue(paras(1))
    )
    assert(!paras(1).toLowerCase.contains("forgiveness"))
  }

  test("opening lens keeps opening identity tied to the strategic purpose") {
    val ctx = baseContext.copy(
      openingData = Some(
        OpeningReference(
          eco = Some("E04"),
          name = Some("Catalan"),
          totalGames = 42,
          topMoves = Nil,
          sampleGames = Nil
        )
      ),
      openingEvent = Some(OpeningEvent.Intro("E04", "Catalan", "queenside pressure", Nil)),
      mainStrategicPlans = List(
        PlanHypothesis(
          planId = "pressure_c_file",
          planName = "Queenside Pressure",
          rank = 1,
          score = 0.79,
          preconditions = Nil,
          executionSteps = Nil,
          failureModes = Nil,
          viability = PlanViability(0.74, "medium", "slow"),
          themeL1 = "open_file"
        )
      )
    )

    val thesis = StrategicThesisBuilder.build(ctx).getOrElse(fail("missing opening thesis"))
    assertEquals(thesis.lens, StrategicLens.Opening)
    assert(thesis.claim.contains("Catalan"))
    assert(thesis.claim.toLowerCase.contains("queenside pressure"))

    val prose = BookStyleRenderer.render(ctx)
    val paras = paragraphs(prose)
    assertEquals(paras.size, 2)
    assert(paras.head.contains("Catalan"))
    assert(paras(1).toLowerCase.contains("queenside pressure"))
    assert(paras(1).toLowerCase.contains("territory"))
  }

  test("opening lens falls back to opening themes instead of raw top5 plan names") {
    val ctx = baseContext.copy(
      openingData = Some(
        OpeningReference(
          eco = Some("E04"),
          name = Some("Catalan"),
          totalGames = 42,
          topMoves = Nil,
          sampleGames = Nil
        )
      ),
      openingEvent = Some(OpeningEvent.Intro("E04", "Catalan", "queenside pressure", Nil)),
      mainStrategicPlans = Nil
    )

    val thesis = StrategicThesisBuilder.build(ctx).getOrElse(fail("missing opening-theme fallback thesis"))
    assertEquals(thesis.lens, StrategicLens.Opening)
    assert(thesis.claim.contains("Catalan"), clue(thesis.claim))
    assert(thesis.claim.toLowerCase.contains("queenside pressure"), clue(thesis.claim))
    assert(thesis.claim.toLowerCase.contains("keeps the game inside"), clue(thesis.claim))
    assert(!thesis.claim.toLowerCase.contains("kingside expansion"), clue(thesis.claim))
  }

  test("opening lens can cite a representative player game and strategic branch") {
    val ctx = baseContext.copy(
      playedMove = Some("b2b3"),
      playedSan = Some("b3"),
      openingData = Some(
        OpeningReference(
          eco = Some("E04"),
          name = Some("Catalan"),
          totalGames = 120,
          topMoves = List(
            ExplorerMove("b2b3", "b3", 38, 16, 10, 12, 2520),
            ExplorerMove("d4c5", "dxc5", 31, 13, 8, 10, 2510)
          ),
          sampleGames = List(
            ExplorerGame(
              id = "game1",
              winner = Some(chess.White),
              white = ExplorerPlayer("Kramnik, Vladimir", 2810),
              black = ExplorerPlayer("Anand, Viswanathan", 2791),
              year = 2008,
              month = 10,
              event = Some("WCh"),
              pgn = Some("9. b3 Qe7 10. Bb2 Rd8 11. Rc1")
            )
          )
        )
      ),
      openingEvent = Some(OpeningEvent.BranchPoint(List("Qc2", "b3", "dxc5"), "Main line shifts", Some("lichess.org/game1"))),
      mainStrategicPlans = List(
        PlanHypothesis(
          planId = "pressure_c_file",
          planName = "Queenside Pressure",
          rank = 1,
          score = 0.82,
          preconditions = Nil,
          executionSteps = Nil,
          failureModes = Nil,
          viability = PlanViability(0.76, "medium", "slow"),
          themeL1 = "open_file"
        )
      )
    )

    val thesis = StrategicThesisBuilder.build(ctx).getOrElse(fail("missing opening thesis"))
    assertEquals(thesis.lens, StrategicLens.Opening)
    assert(thesis.support.exists(_.contains("Vladimir Kramnik-Viswanathan Anand")))
    assert(thesis.support.exists(_.toLowerCase.contains("queenside pressure branch")))
    assert(thesis.support.exists(_.toLowerCase.contains("keeps the game inside")))

    val prose = BookStyleRenderer.render(ctx)
    val paras = paragraphs(prose)
    assert(paras.head.contains("Catalan"))
    assert(paras(1).contains("Vladimir Kramnik-Viswanathan Anand"))
    assert(paras(1).toLowerCase.contains("queenside pressure branch"))
    assert(paras(1).toLowerCase.contains("keeps the game inside"))
  }
