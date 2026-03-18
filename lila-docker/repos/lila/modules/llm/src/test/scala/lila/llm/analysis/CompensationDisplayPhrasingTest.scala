package lila.llm.analysis

import munit.FunSuite

import lila.llm.*
class CompensationDisplayPhrasingTest extends FunSuite:

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

  test("phrasing builder keeps normalized payoff wording together") {
    val surface = StrategyPackSurface.from(Some(benkoLikeCompensationPack))
    val rawSubtype = surface.compensationSubtype.getOrElse(fail("missing raw subtype"))
    val resolution = StrategyPackSurface.CompensationDisplaySubtypeResolver.resolve(surface, rawSubtype)

    val normalization =
      StrategyPackSurface.CompensationDisplayPhrasing.buildDisplayNormalization(surface, rawSubtype, resolution)

    assertEquals(normalization.displaySubtypeSource, "payoff")
    assertEquals(normalization.normalizedDominantIdeaText, Some("fixed queenside targets"))
    assert(normalization.normalizedExecutionText.exists(_.contains("fixed queenside targets")), clue(normalization))
    assert(
      normalization.normalizedLongTermFocusText.exists(_.contains("queenside targets under pressure")),
      clue(normalization)
    )
    assertEquals(normalization.normalizedCompensationLead, Some("fixed queenside targets and long queenside pressure"))
  }

  test("phrasing helper keeps subtype-specific support wording") {
    val surface = StrategyPackSurface.from(Some(benkoLikeCompensationPack))

    val support = StrategyPackSurface.CompensationDisplayPhrasing.compensationSupportText(surface)

    assert(support.exists(_.contains("fixed queenside targets")), clue(support))
    assert(support.exists(_.contains("file pressure")), clue(support))
  }
