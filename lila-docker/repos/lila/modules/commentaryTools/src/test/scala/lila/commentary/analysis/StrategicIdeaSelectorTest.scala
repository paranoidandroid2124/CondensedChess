package lila.commentary.analysis

import chess.{ Bishop, Board, Color, File, Knight, Queen, Rook, Square }
import chess.format.Fen
import chess.variant.Standard
import lila.commentary.*
import lila.commentary.analysis.L3.{
  ChoiceTopologyResult,
  ChoiceTopologyType,
  CriticalityResult,
  CriticalityType,
  DrawBiasResult,
  GamePhaseResult,
  GamePhaseType,
  NatureResult,
  NatureType,
  PassedPawnUrgency,
  PawnPlayAnalysis,
  PositionClassification,
  RiskLevel,
  RiskProfileResult,
  SimplifyBiasResult,
  TaskModeResult,
  TaskModeType,
  TensionPolicy
}
import lila.commentary.model.{ Motif, PawnPlayTable, Plan, PlanMatch, StrategicPlanExperiment }
import lila.commentary.model.strategic.{ EndgameFeature, EndgameOppositionType, PositionalTag, PreventedPlan, WeakComplex }
import lila.commentary.model.structure.{ CenterState, StructureId, StructureProfile }
import munit.FunSuite

class StrategicIdeaSelectorTest extends FunSuite:

  private def selectFromFen(
      fen: String,
      phase: String = "middlegame"
  ): List[StrategyIdeaSignal] =
    val board =
      Fen.read(Standard, Fen.Full(fen)).map(_.board).getOrElse(fail(s"invalid FEN: $fen"))
    val data =
      CommentaryEngine
        .assessExtended(
          fen = fen,
          variations = List(lila.commentary.model.strategic.VariationLine(Nil, 0, depth = 0)),
          phase = Some(phase),
          ply = 24
        )
        .getOrElse(fail(s"analysis missing for $fen"))
    val ctx = NarrativeContextBuilder.build(data, data.toContext, None)
    val semantic = StrategicIdeaSemanticContext.from(data, ctx, Some(board))
    val pack = StrategyPackBuilder.build(data, ctx).getOrElse(fail(s"strategy pack missing for $fen"))
    StrategicIdeaSelector.select(pack, semantic)

  private def noisyPack(): StrategyPack =
    StrategyPack(
      sideToMove = "white",
      plans = List(
        StrategySidePlan(
          side = "white",
          horizon = "long",
          planName = "Build kingside clamp and space gain"
        )
      ),
      longTermFocus = List("misleading long-term focus about exchange play"),
      signalDigest = Some(
        NarrativeSignalDigest(
          structuralCue = Some("misleading space gain text"),
          latentPlan = Some("misleading favorable exchange text"),
          decision = Some("misleading prophylaxis text"),
          prophylaxisPlan = Some("misleading prevent text"),
          opponentPlan = Some("misleading counterplay text")
        )
      )
    )

  private def boardFromFen(fen: String): Board =
    Fen.read(Standard, Fen.Full(fen)).map(_.board).getOrElse(fail(s"invalid FEN: $fen"))

  private def planMatch(plan: Plan, score: Double): PlanMatch =
    PlanMatch(plan = plan, score = score, evidence = Nil)

  private def compensationFeatures(
      fen: String,
      materialDiff: Int,
      whiteDevelopmentLag: Int,
      blackDevelopmentLag: Int,
      whiteAttackersCount: Int = 0,
      blackAttackersCount: Int = 0,
      blackKingRingAttacked: Int = 0,
      blackKingExposedFiles: Int = 0,
      blackCastledSide: String = "none",
      whiteSemiOpenFiles: Int,
      openFilesCount: Int,
      spaceDiff: Int,
      phase: String = "opening"
  ): PositionFeatures =
    PositionFeatures.empty.copy(
      fen = fen,
      sideToMove = "white",
      activity = PositionFeatures.empty.activity.copy(
        whiteDevelopmentLag = whiteDevelopmentLag,
        blackDevelopmentLag = blackDevelopmentLag
      ),
      kingSafety = PositionFeatures.empty.kingSafety.copy(
        whiteCastledSide = "short",
        blackCastledSide = blackCastledSide,
        whiteAttackersCount = whiteAttackersCount,
        blackAttackersCount = blackAttackersCount,
        blackKingRingAttacked = blackKingRingAttacked,
        blackKingExposedFiles = blackKingExposedFiles
      ),
      materialPhase = PositionFeatures.empty.materialPhase.copy(
        whiteMaterial = 37,
        blackMaterial = 37 - materialDiff,
        materialDiff = materialDiff,
        phase = phase
      ),
      lineControl = PositionFeatures.empty.lineControl.copy(
        openFilesCount = openFilesCount,
        whiteSemiOpenFiles = whiteSemiOpenFiles
      ),
      centralSpace = PositionFeatures.empty.centralSpace.copy(spaceDiff = spaceDiff)
    )

  test("typed selector ignores text-only ambiguity without typed evidence") {
    val pack = noisyPack()

    val typed = StrategicIdeaSelector.select(pack, StrategicIdeaSemanticContext.empty("white"))

    assertEquals(typed, Nil)
  }

  test("color-complex clamp remains selector support without proof authority") {
    val semantic =
      StrategicIdeaSemanticContext
        .empty("white")
        .copy(
          strategicState = Some(StrategicStateFeatures.empty.copy(whiteColorComplexClamp = true)),
          positionalFeatures = List(
            PositionalTag.ColorComplexWeakness(Color.Black, "dark", List(Square.F6, Square.H6, Square.G7))
          )
        )

    val ideas = StrategicIdeaSelector.select(StrategyPack(sideToMove = "white"), semantic)
    val spaceIdea = ideas.find(_.kind == StrategicIdeaKind.SpaceGainOrRestriction)

    assert(spaceIdea.exists(_.evidenceRefs.contains("source:color_complex_clamp")), clues(ideas))
    assert(spaceIdea.exists(_.evidenceRefs.contains("enemy_color_complex_weakness")), clues(ideas))
    assert(spaceIdea.exists(_.evidenceRefs.contains("color_complex_dark")), clues(ideas))
    assert(spaceIdea.exists(_.focusSquares == List("f6", "h6", "g7")), clues(ideas))
    assert(spaceIdea.exists(_.focusZone.contains("dark-square complex")), clues(ideas))
    assert(ideas.flatMap(_.evidenceRefs).forall(ref => !ref.startsWith("proof:")), clues(ideas))
  }

  test("space producer keeps central and mobility shape facts for support surface") {
    val centralFeatures =
      PositionFeatures.empty.copy(
        centralSpace = PositionFeatures.empty.centralSpace.copy(spaceDiff = 3)
      )
    val centralIdeas =
      StrategicIdeaSelector.select(
        StrategyPack(sideToMove = "white"),
        StrategicIdeaSemanticContext(sideToMove = "white", positionFeatures = Some(centralFeatures))
      )
    val centralIdea =
      centralIdeas.find(_.evidenceRefs.contains("source:central_space_edge")).getOrElse(fail(clue(centralIdeas).toString))

    assert(centralIdea.evidenceRefs.contains("central_space_edge_shape"), clue(centralIdea.evidenceRefs))
    assert(centralIdea.evidenceRefs.contains("central_space_diff_3"), clue(centralIdea.evidenceRefs))
    assertEquals(centralIdea.focusZone, Some("center"))

    val mobilityFeatures =
      PositionFeatures.empty.copy(
        activity = PositionFeatures.empty.activity.copy(whiteLowMobilityPieces = 0, blackLowMobilityPieces = 2)
      )
    val mobilityIdeas =
      StrategicIdeaSelector.select(
        StrategyPack(sideToMove = "white"),
        StrategicIdeaSemanticContext(sideToMove = "white", positionFeatures = Some(mobilityFeatures))
      )
    val mobilityIdea =
      mobilityIdeas.find(_.evidenceRefs.contains("source:mobility_restriction")).getOrElse(fail(clue(mobilityIdeas).toString))

    assert(mobilityIdea.evidenceRefs.contains("mobility_restriction_shape"), clue(mobilityIdea.evidenceRefs))
    assert(mobilityIdea.evidenceRefs.contains("mobility_restriction_gap_2"), clue(mobilityIdea.evidenceRefs))
    assert(mobilityIdea.evidenceRefs.contains("enemy_low_mobility_pieces_2"), clue(mobilityIdea.evidenceRefs))
    assert(mobilityIdea.evidenceRefs.contains("own_low_mobility_pieces_0"), clue(mobilityIdea.evidenceRefs))
    assertEquals(mobilityIdea.focusZone, Some("center"))

    val iqpFen = "8/8/8/8/3P4/8/8/4K2k w - - 0 1"
    val iqpIdeas =
      StrategicIdeaSelector.select(
        StrategyPack(sideToMove = "white"),
        StrategicIdeaSemanticContext(
          sideToMove = "white",
          board = Some(boardFromFen(iqpFen)),
          fen = iqpFen,
          structureProfile = Some(
            StructureProfile(
              primary = StructureId.IQPWhite,
              confidence = 0.92,
              alternatives = Nil,
              centerState = CenterState.Open,
              evidenceCodes = List("structure_iqp_white")
            )
          )
        )
      )
    val iqpIdea =
      iqpIdeas.find(_.evidenceRefs.contains("source:iqp_central_presence")).getOrElse(fail(clue(iqpIdeas).toString))

    assert(iqpIdea.evidenceRefs.contains("structure_iqp_white"), clue(iqpIdea.evidenceRefs))
    assert(iqpIdea.evidenceRefs.contains("iqp_central_presence_shape"), clue(iqpIdea.evidenceRefs))
    assertEquals(iqpIdea.focusSquares, List("d4"))

    val maroczyIdeas =
      StrategicIdeaSelector.select(
        StrategyPack(sideToMove = "white"),
        StrategicIdeaSemanticContext(
          sideToMove = "white",
          structureProfile = Some(
            StructureProfile(
              primary = StructureId.MaroczyBind,
              confidence = 0.92,
              alternatives = Nil,
              centerState = CenterState.Open,
              evidenceCodes = List("structure_maroczy_bind")
            )
          )
        )
      )
    val maroczyIdea =
      maroczyIdeas.find(_.evidenceRefs.contains("source:maroczy_bind_profile")).getOrElse(fail(clue(maroczyIdeas).toString))

    assert(maroczyIdea.evidenceRefs.contains("structure_maroczy_bind"), clue(maroczyIdea.evidenceRefs))
    assertEquals(maroczyIdea.focusZone, Some("center"))
  }

  test("minor-piece producer keeps shape facts for support surface") {
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        positionalFeatures = List(
          PositionalTag.BishopPairAdvantage(Color.White),
          PositionalTag.BadBishop(Color.Black),
          PositionalTag.OppositeColorBishops
        )
      )
    val ideas = StrategicIdeaSelector.select(StrategyPack(sideToMove = "white"), semantic)
    val minorIdea =
      ideas.find(_.kind == StrategicIdeaKind.MinorPieceImbalanceExploitation).getOrElse(fail(clue(ideas).toString))

    assert(minorIdea.evidenceRefs.contains("source:bishop_pair_advantage"), clue(minorIdea.evidenceRefs))
    assert(minorIdea.evidenceRefs.contains("bishop_pair_advantage_shape"), clue(minorIdea.evidenceRefs))
    assert(minorIdea.evidenceRefs.contains("source:enemy_bad_bishop"), clue(minorIdea.evidenceRefs))
    assert(minorIdea.evidenceRefs.contains("enemy_bad_bishop_shape"), clue(minorIdea.evidenceRefs))
    assert(minorIdea.evidenceRefs.contains("source:opposite_color_bishops"), clue(minorIdea.evidenceRefs))
    assert(minorIdea.evidenceRefs.contains("opposite_color_bishops_shape"), clue(minorIdea.evidenceRefs))
  }

  test("good-bishop count edge keeps paired shape facts for support surface") {
    val features =
      PositionFeatures.empty.copy(
        imbalance = PositionFeatures.empty.imbalance.copy(
          whiteBishops = 2,
          blackBishops = 1
        )
      )
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        positionalFeatures = List(PositionalTag.GoodBishop(Color.White)),
        positionFeatures = Some(features)
      )
    val ideas = StrategicIdeaSelector.select(StrategyPack(sideToMove = "white"), semantic)
    val minorIdea =
      ideas.find(_.kind == StrategicIdeaKind.MinorPieceImbalanceExploitation).getOrElse(fail(clue(ideas).toString))

    assert(minorIdea.evidenceRefs.contains("source:good_bishop"), clue(minorIdea.evidenceRefs))
    assert(minorIdea.evidenceRefs.contains("good_bishop_shape"), clue(minorIdea.evidenceRefs))
    assert(minorIdea.evidenceRefs.contains("source:minor_piece_count_imbalance"), clue(minorIdea.evidenceRefs))
    assert(minorIdea.evidenceRefs.contains("minor_piece_count_imbalance_shape"), clue(minorIdea.evidenceRefs))
    assert(minorIdea.evidenceRefs.contains("good_bishop_count_edge"), clue(minorIdea.evidenceRefs))
  }

  test("centralization motif keeps piece-improvement witnesses for support surface") {
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        motifs = List(Motif.Centralization(Knight, Square.E5, Color.White, plyIndex = 0, move = Some("Ne5")))
      )

    val ideas = StrategicIdeaSelector.select(StrategyPack(sideToMove = "white"), semantic)
    val minorIdea =
      ideas.find(_.evidenceRefs.contains("source:piece_centralization_motif")).getOrElse(fail(clue(ideas).toString))

    assertEquals(minorIdea.kind, StrategicIdeaKind.MinorPieceImbalanceExploitation)
    assert(minorIdea.focusSquares.contains("e5"), clue(minorIdea))
    assert(minorIdea.beneficiaryPieces.contains("N"), clue(minorIdea))
    assert(minorIdea.evidenceRefs.contains("piece_centralization_shape"), clue(minorIdea.evidenceRefs))
    assert(minorIdea.evidenceRefs.contains("centralized_piece_e5"), clue(minorIdea.evidenceRefs))
  }

  test("maneuver motif keeps minor-piece improvement witnesses for support surface") {
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        motifs = List(Motif.Maneuver(Knight, "rerouting", Color.White, plyIndex = 0, move = Some("Nd2")))
      )

    val ideas = StrategicIdeaSelector.select(StrategyPack(sideToMove = "white"), semantic)
    val minorIdea =
      ideas.find(_.evidenceRefs.contains("source:piece_maneuver_motif")).getOrElse(fail(clue(ideas).toString))

    assertEquals(minorIdea.kind, StrategicIdeaKind.MinorPieceImbalanceExploitation)
    assertEquals(minorIdea.focusSquares, List("d2"), clue(minorIdea))
    assert(minorIdea.beneficiaryPieces.contains("N"), clue(minorIdea))
    assert(minorIdea.evidenceRefs.contains("piece_maneuver_shape"), clue(minorIdea.evidenceRefs))
    assert(minorIdea.evidenceRefs.contains("piece_maneuver_rerouting"), clue(minorIdea.evidenceRefs))
    assert(minorIdea.evidenceRefs.contains("piece_maneuver_square_d2"), clue(minorIdea.evidenceRefs))
  }

  test("knight-vs-bishop motif keeps minor-piece witnesses for support surface") {
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        motifs = List(Motif.KnightVsBishop(Color.White, isKnightBetter = true, plyIndex = 0, move = None))
      )

    val ideas = StrategicIdeaSelector.select(StrategyPack(sideToMove = "white"), semantic)
    val minorIdea =
      ideas.find(_.evidenceRefs.contains("source:knight_vs_bishop_motif")).getOrElse(fail(clue(ideas).toString))

    assertEquals(minorIdea.kind, StrategicIdeaKind.MinorPieceImbalanceExploitation)
    assert(minorIdea.beneficiaryPieces.contains("N"), clue(minorIdea))
    assert(minorIdea.evidenceRefs.contains("knight_vs_bishop_motif_shape"), clue(minorIdea.evidenceRefs))
    assert(minorIdea.evidenceRefs.contains("knight_preferred_over_bishop"), clue(minorIdea.evidenceRefs))
  }

  test("space advantage motif keeps central-space witnesses for support surface") {
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        motifs = List(Motif.SpaceAdvantage(Color.White, pawnDelta = 3, plyIndex = 0, move = None))
      )

    val ideas = StrategicIdeaSelector.select(StrategyPack(sideToMove = "white"), semantic)
    val spaceIdea =
      ideas.find(_.evidenceRefs.contains("source:space_advantage_motif")).getOrElse(fail(clue(ideas).toString))

    assertEquals(spaceIdea.kind, StrategicIdeaKind.SpaceGainOrRestriction)
    assertEquals(spaceIdea.focusZone, Some("center"))
    assert(spaceIdea.evidenceRefs.contains("space_advantage_motif_shape"), clue(spaceIdea.evidenceRefs))
    assert(spaceIdea.evidenceRefs.contains("space_pawn_delta_3"), clue(spaceIdea.evidenceRefs))
  }

  test("central pawn-advance motif keeps file and rank witnesses for support surface") {
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        motifs = List(Motif.PawnAdvance(File.D, fromRank = 2, toRank = 4, Color.White, plyIndex = 0, move = Some("d4")))
      )

    val ideas = StrategicIdeaSelector.select(StrategyPack(sideToMove = "white"), semantic)
    val spaceIdea =
      ideas.find(_.evidenceRefs.contains("source:central_pawn_advance_motif")).getOrElse(fail(clue(ideas).toString))

    assertEquals(spaceIdea.kind, StrategicIdeaKind.SpaceGainOrRestriction)
    assert(spaceIdea.focusFiles.contains("d"), clue(spaceIdea))
    assertEquals(spaceIdea.focusZone, Some("center"))
    assert(spaceIdea.evidenceRefs.contains("central_pawn_advance_shape"), clue(spaceIdea.evidenceRefs))
    assert(spaceIdea.evidenceRefs.contains("central_pawn_file_d"), clue(spaceIdea.evidenceRefs))
    assert(spaceIdea.evidenceRefs.contains("central_pawn_to_rank_4"), clue(spaceIdea.evidenceRefs))
  }

  test("flank pawn-advance motif keeps file and rank witnesses for support surface") {
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        motifs = List(Motif.PawnAdvance(File.G, fromRank = 2, toRank = 4, Color.White, plyIndex = 0, move = Some("g4")))
      )

    val ideas = StrategicIdeaSelector.select(StrategyPack(sideToMove = "white"), semantic)
    val attackIdea =
      ideas.find(_.evidenceRefs.contains("source:flank_pawn_advance_motif")).getOrElse(fail(clue(ideas).toString))

    assertEquals(attackIdea.kind, StrategicIdeaKind.KingAttackBuildUp)
    assert(attackIdea.focusFiles.contains("g"), clue(attackIdea))
    assertEquals(attackIdea.focusZone, Some("kingside"))
    assert(attackIdea.evidenceRefs.contains("flank_pawn_advance_shape"), clue(attackIdea.evidenceRefs))
    assert(attackIdea.evidenceRefs.contains("flank_pawn_file_g"), clue(attackIdea.evidenceRefs))
    assert(attackIdea.evidenceRefs.contains("flank_pawn_to_rank_4"), clue(attackIdea.evidenceRefs))
  }

  test("pawn-chain motif keeps flank-space witnesses for support surface") {
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        motifs = List(Motif.PawnChain(File.G, File.H, Color.White, plyIndex = 0, move = None))
      )

    val ideas = StrategicIdeaSelector.select(StrategyPack(sideToMove = "white"), semantic)
    val spaceIdea =
      ideas.find(_.evidenceRefs.contains("source:pawn_chain_space_motif")).getOrElse(fail(clue(ideas).toString))

    assertEquals(spaceIdea.kind, StrategicIdeaKind.SpaceGainOrRestriction)
    assert(spaceIdea.focusFiles.contains("g"), clue(spaceIdea))
    assert(spaceIdea.focusFiles.contains("h"), clue(spaceIdea))
    assertEquals(spaceIdea.focusZone, Some("kingside"))
    assert(spaceIdea.evidenceRefs.contains("pawn_chain_space_shape"), clue(spaceIdea.evidenceRefs))
    assert(spaceIdea.evidenceRefs.contains("pawn_chain_g_h"), clue(spaceIdea.evidenceRefs))
  }

  test("refuted experiment blocks matching king-attack idea") {
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        positionalFeatures = List(PositionalTag.MateNet(Color.White)),
        strategicPlanExperiments = List(
          StrategicPlanExperiment(
            planId = "flank_attack",
            themeL1 = PlanTaxonomy.PlanTheme.FlankInfrastructure.id,
            subplanId = Some(PlanTaxonomy.PlanKind.HookCreation.id),
            evidenceTier = "refuted",
            refuteProbeCount = 1,
            moveOrderSensitive = true,
            experimentConfidence = 0.08
          )
        )
      )

    val ideas = StrategicIdeaSelector.select(StrategyPack(sideToMove = "white"), semantic)

    assertEquals(ideas, Nil)
  }

  test("pv-coupled move-order-sensitive experiment demotes but does not erase slow space idea") {
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        positionalFeatures = List(PositionalTag.SpaceAdvantage(Color.White)),
        strategicPlanExperiments = List(
          StrategicPlanExperiment(
            planId = "space_clamp",
            themeL1 = PlanTaxonomy.PlanTheme.SpaceClamp.id,
            subplanId = Some(PlanTaxonomy.PlanKind.CentralSpaceBind.id),
            evidenceTier = "pv_coupled",
            moveOrderSensitive = true,
            experimentConfidence = 0.52
          )
        )
      )

    val ideas = StrategicIdeaSelector.select(StrategyPack(sideToMove = "white"), semantic)

    assertEquals(ideas.headOption.map(_.kind), Some(StrategicIdeaKind.SpaceGainOrRestriction))
    assert(ideas.headOption.exists(_.evidenceRefs.contains("experiment:space_clamp")))
  }

  test("evidence-backed experiment adds experiment refs to surviving outpost idea") {
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        positionalFeatures = List(PositionalTag.Outpost(Square.E5, Color.White)),
        strategicPlanExperiments = List(
          StrategicPlanExperiment(
            planId = "outpost_plan",
            themeL1 = PlanTaxonomy.PlanTheme.PieceRedeployment.id,
            subplanId = Some(PlanTaxonomy.PlanKind.OutpostEntrenchment.id),
            evidenceTier = "evidence_backed",
            supportProbeCount = 1,
            bestReplyStable = true,
            futureSnapshotAligned = true,
            experimentConfidence = 0.91
          )
        )
      )

    val ideas = StrategicIdeaSelector.select(StrategyPack(sideToMove = "white"), semantic)

    assertEquals(ideas.headOption.map(_.kind), Some(StrategicIdeaKind.OutpostCreationOrOccupation))
    assert(ideas.headOption.exists(_.evidenceRefs.exists(_ == "experiment:piece_redeployment")))
    assert(ideas.headOption.exists(_.evidenceRefs.exists(_ == "experiment_subplan:outpost_entrenchment")))
  }

  test("family-first staging keeps concrete pawn break ahead of slow structural noise") {
    val ideas =
      selectFromFen("r2qk2r/pp1bbppp/2n1p3/3pPn2/NP1P4/P2B1N2/1B3PPP/R2QK2R b KQkq - 8 12")

    assertEquals(ideas.headOption.map(_.kind), Some(StrategicIdeaKind.PawnBreak))
  }

  test("family-first staging keeps IQP trade-down windows in the conversion family") {
    val ideas =
      selectFromFen("r1bqr1k1/pp2bpp1/2n2n1p/3p4/3N4/2N1B1P1/PP2PPBP/R2Q1RK1 w - - 2 12")

    assertEquals(ideas.headOption.map(_.kind), Some(StrategicIdeaKind.FavorableTradeOrTransformation))
  }

  test("exchange availability bridge keeps IQP structure witness for support surface") {
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        classification =
          Some(
            PositionClassification(
              nature = NatureResult(NatureType.Static, tensionScore = 0, openFilesCount = 0, mobilityDiff = 0, lockedCenter = false),
              criticality = CriticalityResult(CriticalityType.Normal, evalDeltaCp = 0, mateDistance = None, forcingMovesInPv = 0),
              choiceTopology = ChoiceTopologyResult(ChoiceTopologyType.StyleChoice, pv1Eval = 80, pv2Eval = 50, pv3Eval = None, gapPv1ToPv2 = 0.30, spreadTop3 = 0.30, pv2FailureMode = None),
              gamePhase = GamePhaseResult(GamePhaseType.Middlegame, totalMaterial = 54, queensOnBoard = true, minorPiecesCount = 4),
              simplifyBias = SimplifyBiasResult(isSimplificationWindow = false, evalAdvantage = 80, isEndgameNear = false, exchangeAvailable = true),
              drawBias = DrawBiasResult(isDrawish = false, materialSymmetry = false, oppositeColorBishops = false, fortressLikely = false, insufficientMaterial = false),
              riskProfile = RiskProfileResult(RiskLevel.Low, evalVolatility = 10, tacticalMotifsCount = 0, kingExposureSum = 0),
              taskMode = TaskModeResult(TaskModeType.ExplainPlan, primaryDriver = "strategic_maneuvering")
            )
          ),
        structureProfile =
          Some(
            StructureProfile(
              primary = StructureId.IQPBlack,
              confidence = 0.92,
              alternatives = Nil,
              centerState = CenterState.Open,
              evidenceCodes = List("structure_iqp_black")
            )
          )
      )

    val ideas = StrategicIdeaSelector.select(StrategyPack(sideToMove = "white"), semantic)
    val tradeIdea =
      ideas.find(_.evidenceRefs.contains("source:exchange_availability_bridge")).getOrElse(fail(clue(ideas).toString))

    assertEquals(tradeIdea.kind, StrategicIdeaKind.FavorableTradeOrTransformation)
    assert(tradeIdea.evidenceRefs.contains("structure_iqp_black"), clue(tradeIdea.evidenceRefs))
    assert(tradeIdea.evidenceRefs.contains("source:iqp_simplification_profile"), clue(tradeIdea.evidenceRefs))
  }

  test("rook endgame motifs keep conversion witnesses for support surface") {
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        board = Some(boardFromFen("4k3/8/4P3/8/8/4R3/8/4K3 w - - 0 1")),
        motifs =
          List(
            Motif.RookBehindPassedPawn(File.E, Color.White, plyIndex = 0, move = Some("Re3")),
            Motif.KingCutOff("Rank", 6, Color.White, plyIndex = 0, move = None)
          )
      )

    val ideas = StrategicIdeaSelector.select(StrategyPack(sideToMove = "white"), semantic)
    val conversionIdea =
      ideas.find(_.evidenceRefs.contains("source:rook_endgame_pattern")).getOrElse(fail(clue(ideas).toString))

    assertEquals(conversionIdea.kind, StrategicIdeaKind.FavorableTradeOrTransformation)
    assert(conversionIdea.focusSquares.contains("e6"), clue(conversionIdea))
    assert(conversionIdea.focusFiles.contains("e"), clue(conversionIdea))
    assert(conversionIdea.evidenceRefs.contains("rook_endgame_pattern_shape"), clue(conversionIdea.evidenceRefs))
    assert(conversionIdea.evidenceRefs.contains("rook_behind_passed_pawn"), clue(conversionIdea.evidenceRefs))
    assert(conversionIdea.evidenceRefs.contains("rook_behind_passer_square_e6"), clue(conversionIdea.evidenceRefs))
    assert(conversionIdea.evidenceRefs.contains("king_cut_off"), clue(conversionIdea.evidenceRefs))
    assertEquals(StrategicIdeaSelector.playerFacingIdeaText(conversionIdea), "rook-endgame cue")
  }

  test("opposition motif keeps endgame-technique witnesses for support surface") {
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        phase = "endgame",
        board = Some(boardFromFen("8/8/4k3/8/4K3/8/8/8 w - - 0 1")),
        motifs =
          List(
            Motif.Opposition(
              opponentKingSquare = Square.E6,
              ownKingSquare = Square.E4,
              oppType = Motif.OppositionType.Direct,
              color = Color.White,
              plyIndex = 0,
              move = None
            )
          )
      )

    val ideas = StrategicIdeaSelector.select(StrategyPack(sideToMove = "white"), semantic)
    val conversionIdea =
      ideas.find(_.evidenceRefs.contains("source:endgame_technique_motif")).getOrElse(fail(clue(ideas).toString))

    assertEquals(conversionIdea.kind, StrategicIdeaKind.FavorableTradeOrTransformation)
    assert(conversionIdea.focusSquares.contains("e4"), clue(conversionIdea))
    assert(conversionIdea.focusSquares.contains("e6"), clue(conversionIdea))
    assertEquals(conversionIdea.focusZone, Some("endgame"))
    assert(conversionIdea.evidenceRefs.contains("endgame_technique_shape"), clue(conversionIdea.evidenceRefs))
    assert(conversionIdea.evidenceRefs.contains("opposition_direct"), clue(conversionIdea.evidenceRefs))
    assertEquals(StrategicIdeaSelector.playerFacingIdeaText(conversionIdea), "direct-opposition cue")
  }

  test("opposition geometry in a real middlegame position does not become endgame technique") {
    val fen = "r1bqk2r/pppp1ppp/5n2/4N1B1/4P3/3P4/P1P1KPPP/Q6R b kq - 0 10"
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "black",
        fen = fen,
        phase = "middlegame",
        board = Some(boardFromFen(fen)),
        motifs =
          List(
            Motif.Opposition(
              opponentKingSquare = Square.E2,
              ownKingSquare = Square.E8,
              oppType = Motif.OppositionType.Distant,
              color = Color.Black,
              plyIndex = 20,
              move = None
            )
          )
      )

    val ideas = StrategicIdeaSelector.select(StrategyPack(sideToMove = "black"), semantic)

    assert(
      !ideas.exists(_.evidenceRefs.contains("source:endgame_technique_motif")),
      clue(ideas)
    )
  }

  test("endgame phase label without endgame material does not make opposition geometry authoritative") {
    val fen = "2r3k1/p4p1p/2p3p1/2q5/4Q3/1P4P1/P4P1P/5BK1 b - - 0 25"
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "black",
        fen = fen,
        phase = "endgame",
        board = Some(boardFromFen(fen)),
        endgameFeatures =
          Some(
            EndgameFeature(
              hasOpposition = true,
              isZugzwang = false,
              keySquaresControlled = Nil,
              oppositionType = EndgameOppositionType.Distant,
              confidence = 0.30
            )
          ),
        motifs =
          List(
            Motif.Opposition(
              opponentKingSquare = Square.G1,
              ownKingSquare = Square.G7,
              oppType = Motif.OppositionType.Distant,
              color = Color.Black,
              plyIndex = 50,
              move = None
            )
          )
      )

    val ideas = StrategicIdeaSelector.select(StrategyPack(sideToMove = "black"), semantic)

    assert(
      !ideas.exists(_.evidenceRefs.contains("source:endgame_technique_motif")),
      clue(ideas)
    )
  }

  test("zugzwang motif keeps endgame-technique witnesses for support surface") {
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        phase = "endgame",
        board = Some(boardFromFen("6k1/8/8/8/8/8/8/4K3 w - - 0 1")),
        motifs = List(Motif.Zugzwang(Color.Black, plyIndex = 0, move = None))
      )

    val ideas = StrategicIdeaSelector.select(StrategyPack(sideToMove = "white"), semantic)
    val conversionIdea =
      ideas.find(_.evidenceRefs.contains("source:endgame_technique_motif")).getOrElse(fail(clue(ideas).toString))

    assertEquals(conversionIdea.kind, StrategicIdeaKind.FavorableTradeOrTransformation)
    assertEquals(conversionIdea.focusZone, Some("endgame"))
    assert(conversionIdea.evidenceRefs.contains("endgame_technique_shape"), clue(conversionIdea.evidenceRefs))
    assert(conversionIdea.evidenceRefs.contains("zugzwang_shape"), clue(conversionIdea.evidenceRefs))
    assertEquals(StrategicIdeaSelector.playerFacingIdeaText(conversionIdea), "zugzwang cue")
  }

  test("active king-step motif keeps endgame-technique witnesses for support surface") {
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        phase = "endgame",
        board = Some(boardFromFen("6k1/8/8/8/8/8/8/4K3 w - - 0 1")),
        motifs = List(Motif.KingStep(Motif.KingStepType.Activation, Color.White, plyIndex = 0, move = Some("Ke4")))
      )

    val ideas = StrategicIdeaSelector.select(StrategyPack(sideToMove = "white"), semantic)
    val conversionIdea =
      ideas.find(_.evidenceRefs.contains("source:endgame_technique_motif")).getOrElse(fail(clue(ideas).toString))

    assertEquals(conversionIdea.kind, StrategicIdeaKind.FavorableTradeOrTransformation)
    assert(conversionIdea.focusSquares.contains("e4"), clue(conversionIdea))
    assertEquals(conversionIdea.focusZone, Some("endgame"))
    assert(conversionIdea.evidenceRefs.contains("endgame_technique_shape"), clue(conversionIdea.evidenceRefs))
    assert(conversionIdea.evidenceRefs.contains("king_activity_shape"), clue(conversionIdea.evidenceRefs))
    assert(conversionIdea.evidenceRefs.contains("king_activity_square_e4"), clue(conversionIdea.evidenceRefs))
    assertEquals(StrategicIdeaSelector.playerFacingIdeaText(conversionIdea), "king-activity cue")
  }

  test("passed-pawn motifs keep conversion witnesses for support surface") {
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        motifs =
          List(
            Motif.PassedPawn(File.E, 6, Color.White, isProtected = true, plyIndex = 0, move = None)
          )
      )

    val ideas = StrategicIdeaSelector.select(StrategyPack(sideToMove = "white"), semantic)
    val conversionIdea =
      ideas.find(_.evidenceRefs.contains("source:passed_pawn_conversion_motif")).getOrElse(fail(clue(ideas).toString))

    assertEquals(conversionIdea.kind, StrategicIdeaKind.FavorableTradeOrTransformation)
    assert(conversionIdea.focusSquares.contains("e6"), clue(conversionIdea))
    assert(conversionIdea.focusFiles.contains("e"), clue(conversionIdea))
    assert(conversionIdea.evidenceRefs.contains("passed_pawn_conversion_shape"), clue(conversionIdea.evidenceRefs))
    assert(conversionIdea.evidenceRefs.contains("passed_pawn_e6"), clue(conversionIdea.evidenceRefs))
    assert(conversionIdea.evidenceRefs.contains("protected_passed_pawn"), clue(conversionIdea.evidenceRefs))
    assertEquals(StrategicIdeaSelector.playerFacingIdeaText(conversionIdea), "passed-pawn cue around e6")
  }

  test("back-rank passer in real forcing-defense row does not become conversion motif") {
    val fen = "r3k2r/p4p1p/p2Bp3/5p2/3Rb3/8/PPP2P1P/2K3R1 w kq - 0 18"
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        fen = fen,
        phase = "middlegame",
        board = Some(boardFromFen(fen)),
        motifs =
          List(
            Motif.PassedPawn(File.C, 2, Color.White, isProtected = false, plyIndex = 35, move = None)
          )
      )

    val ideas = StrategicIdeaSelector.select(StrategyPack(sideToMove = "white"), semantic)

    assert(
      !ideas.exists(_.evidenceRefs.contains("source:passed_pawn_conversion_motif")),
      clue(ideas)
    )
  }

  test("central passer in real file-pressure row does not become conversion motif") {
    val fen = "rn2rbk1/3q1pp1/3p3p/1p1P1b1n/p2N4/P4P1P/BP1N1BP1/2RQ1RK1 b - - 4 21"
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "black",
        fen = fen,
        phase = "middlegame",
        board = Some(boardFromFen(fen)),
        motifs =
          List(
            Motif.PassedPawn(File.D, 4, Color.Black, isProtected = false, plyIndex = 42, move = None)
          )
      )

    val ideas = StrategicIdeaSelector.select(StrategyPack(sideToMove = "black"), semantic)

    assert(
      !ideas.exists(_.evidenceRefs.contains("source:passed_pawn_conversion_motif")),
      clue(ideas)
    )
  }

  test("passed-pawn push motif keeps conversion witnesses for support surface") {
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        motifs =
          List(
            Motif.PassedPawnPush(File.C, 7, Color.White, plyIndex = 0, move = Some("c7"))
          )
      )

    val ideas = StrategicIdeaSelector.select(StrategyPack(sideToMove = "white"), semantic)
    val conversionIdea =
      ideas.find(_.evidenceRefs.contains("source:passed_pawn_conversion_motif")).getOrElse(fail(clue(ideas).toString))

    assertEquals(conversionIdea.kind, StrategicIdeaKind.FavorableTradeOrTransformation)
    assert(conversionIdea.focusSquares.contains("c7"), clue(conversionIdea))
    assert(conversionIdea.focusFiles.contains("c"), clue(conversionIdea))
    assert(conversionIdea.evidenceRefs.contains("passed_pawn_conversion_shape"), clue(conversionIdea.evidenceRefs))
    assert(conversionIdea.evidenceRefs.contains("passed_pawn_c7"), clue(conversionIdea.evidenceRefs))
    assert(conversionIdea.evidenceRefs.contains("passed_pawn_push"), clue(conversionIdea.evidenceRefs))
    assert(conversionIdea.evidenceRefs.contains("advanced_passed_pawn"), clue(conversionIdea.evidenceRefs))
    assertEquals(StrategicIdeaSelector.playerFacingIdeaText(conversionIdea), "passed-pawn cue around c7")
  }

  test("pawn-promotion motif keeps conversion witnesses for support surface") {
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        motifs =
          List(
            Motif.PawnPromotion(File.A, Queen, Color.White, plyIndex = 0, move = Some("a8=Q"))
          )
      )

    val ideas = StrategicIdeaSelector.select(StrategyPack(sideToMove = "white"), semantic)
    val conversionIdea =
      ideas.find(_.evidenceRefs.contains("source:passed_pawn_conversion_motif")).getOrElse(fail(clue(ideas).toString))

    assertEquals(conversionIdea.kind, StrategicIdeaKind.FavorableTradeOrTransformation)
    assert(conversionIdea.focusSquares.contains("a8"), clue(conversionIdea))
    assert(conversionIdea.focusFiles.contains("a"), clue(conversionIdea))
    assert(conversionIdea.evidenceRefs.contains("passed_pawn_conversion_shape"), clue(conversionIdea.evidenceRefs))
    assert(conversionIdea.evidenceRefs.contains("passed_pawn_a8"), clue(conversionIdea.evidenceRefs))
    assert(conversionIdea.evidenceRefs.contains("pawn_promotion"), clue(conversionIdea.evidenceRefs))
    assert(conversionIdea.evidenceRefs.contains("promotion_piece_q"), clue(conversionIdea.evidenceRefs))
    assertEquals(StrategicIdeaSelector.playerFacingIdeaText(conversionIdea), "promotion cue on a8")
  }

  test("static pawn weakness motifs keep target-fixing witnesses for support surface") {
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        motifs =
          List(
            Motif.IsolatedPawn(File.D, 5, Color.Black, plyIndex = 0, move = None),
            Motif.BackwardPawn(File.E, 6, Color.Black, plyIndex = 0, move = None)
          )
      )

    val ideas = StrategicIdeaSelector.select(StrategyPack(sideToMove = "white"), semantic)
    val pressureIdea =
      ideas.find(_.evidenceRefs.contains("source:weak_complex_fixation")).getOrElse(fail(clue(ideas).toString))

    assertEquals(pressureIdea.kind, StrategicIdeaKind.TargetFixing)
    assert(pressureIdea.focusSquares.contains("d5"), clue(pressureIdea))
    assert(pressureIdea.focusSquares.contains("e6"), clue(pressureIdea))
    assert(pressureIdea.evidenceRefs.contains("weak_complex_isolated_pawn"), clue(pressureIdea.evidenceRefs))
    assert(pressureIdea.evidenceRefs.contains("weak_complex_backward_pawn"), clue(pressureIdea.evidenceRefs))
  }

  test("doubled-pawn motif keeps file-pressure witnesses for support surface") {
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        motifs = List(Motif.DoubledPawns(File.D, Color.Black, plyIndex = 0, move = None))
      )

    val ideas = StrategicIdeaSelector.select(StrategyPack(sideToMove = "white"), semantic)
    val pressureIdea =
      ideas.find(_.evidenceRefs.contains("source:doubled_pawn_pressure_motif")).getOrElse(fail(clue(ideas).toString))

    assertEquals(pressureIdea.kind, StrategicIdeaKind.TargetFixing)
    assert(pressureIdea.focusFiles.contains("d"), clue(pressureIdea))
    assertEquals(pressureIdea.focusZone, Some("center"))
    assert(pressureIdea.evidenceRefs.contains("doubled_pawn_pressure_shape"), clue(pressureIdea.evidenceRefs))
    assert(pressureIdea.evidenceRefs.contains("doubled_pawn_file_d"), clue(pressureIdea.evidenceRefs))
  }

  test("directional target fixation survives when the target is structural rather than tag-only") {
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        structuralWeaknesses = List(
          WeakComplex(
            color = Color.Black,
            squares = List(Square.D6),
            isOutpost = false,
            cause = "Backward pawn on d6"
          )
        )
      )
    val pack =
      StrategyPack(
        sideToMove = "white",
        directionalTargets = List(
          StrategyDirectionalTarget(
            targetId = "d6_target",
            ownerSide = "white",
            piece = "N",
            from = "f5",
            targetSquare = "d6",
            readiness = DirectionalTargetReadiness.Build,
            strategicReasons = List("keep the d6 weakness fixed"),
            evidence = List("probe")
          )
        )
      )

    val ideas = StrategicIdeaSelector.select(pack, semantic)

    assertEquals(ideas.headOption.map(_.kind), Some(StrategicIdeaKind.TargetFixing))
    assert(ideas.headOption.exists(_.focusSquares.contains("d6")))
    assert(ideas.headOption.exists(_.evidenceRefs.contains("source:directional_target_fixation")))
  }

  test("directional rook line access keeps concrete file witness for support surface") {
    val fen = "r4rk1/2p2ppp/p1n5/1p1p4/3P4/5N2/PPQ2PPP/2R2RK1 w - - 0 1"
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        board = Some(boardFromFen(fen))
      )
    val pack =
      StrategyPack(
        sideToMove = "white",
        directionalTargets = List(
          StrategyDirectionalTarget(
            targetId = "rook_c_file_target",
            ownerSide = "white",
            piece = "R",
            from = "c1",
            targetSquare = "c4",
            readiness = DirectionalTargetReadiness.Build,
            strategicReasons = List("line access")
          )
        )
      )

    val ideas = StrategicIdeaSelector.select(pack, semantic)
    val lineIdea = ideas.find(_.kind == StrategicIdeaKind.LineOccupation).getOrElse(fail(clue(ideas).toString))

    assert(lineIdea.focusSquares.contains("c4"), clue(lineIdea))
    assert(lineIdea.focusFiles.contains("c"), clue(lineIdea))
    assert(lineIdea.evidenceRefs.contains("source:directional_line_access"), clue(lineIdea.evidenceRefs))
    assert(lineIdea.evidenceRefs.contains("directional_line_access_shape"), clue(lineIdea.evidenceRefs))
    assert(lineIdea.evidenceRefs.exists(ref => ref == "open_file_c" || ref == "semi_open_file_c"), clue(lineIdea.evidenceRefs))
  }

  test("open-file motif reuses line-control witnesses for support surface") {
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        motifs = List(Motif.OpenFileControl(File.D, Color.White, plyIndex = 0, move = None))
      )

    val ideas = StrategicIdeaSelector.select(StrategyPack(sideToMove = "white"), semantic)
    val lineIdea =
      ideas.find(_.evidenceRefs.contains("source:open_file_control")).getOrElse(fail(clue(ideas).toString))

    assertEquals(lineIdea.kind, StrategicIdeaKind.LineOccupation)
    assert(lineIdea.focusFiles.contains("d"), clue(lineIdea))
    assert(lineIdea.beneficiaryPieces.exists(piece => piece == "R" || piece == "Q"), clue(lineIdea))
    assert(lineIdea.evidenceRefs.contains("open_file_d"), clue(lineIdea.evidenceRefs))
  }

  test("semi-open-file motif keeps line-control witnesses for support surface") {
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        motifs = List(Motif.SemiOpenFileControl(File.C, Color.White, plyIndex = 0, move = None))
      )

    val ideas = StrategicIdeaSelector.select(StrategyPack(sideToMove = "white"), semantic)
    val lineIdea =
      ideas.find(_.evidenceRefs.contains("source:semi_open_file_control")).getOrElse(fail(clue(ideas).toString))

    assertEquals(lineIdea.kind, StrategicIdeaKind.LineOccupation)
    assert(lineIdea.focusFiles.contains("c"), clue(lineIdea))
    assert(lineIdea.beneficiaryPieces.exists(piece => piece == "R" || piece == "Q"), clue(lineIdea))
    assert(lineIdea.evidenceRefs.contains("semi_open_file_c"), clue(lineIdea.evidenceRefs))
  }

  test("doubled-rooks motif keeps file witness for support surface") {
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        motifs = List(Motif.DoubledPieces(Rook, File.C, Color.White, plyIndex = 0, move = None))
      )

    val ideas = StrategicIdeaSelector.select(StrategyPack(sideToMove = "white"), semantic)
    val lineIdea =
      ideas.find(_.evidenceRefs.contains("source:doubled_rooks")).getOrElse(fail(clue(ideas).toString))

    assertEquals(lineIdea.kind, StrategicIdeaKind.LineOccupation)
    assert(lineIdea.focusFiles.contains("c"), clue(lineIdea))
    assert(lineIdea.beneficiaryPieces.contains("R"), clue(lineIdea))
    assert(lineIdea.evidenceRefs.contains("doubled_rooks_c"), clue(lineIdea.evidenceRefs))
  }

  test("line-control features keep exact route file witness for support surface") {
    val fen = "r4rk1/2p2ppp/p1n5/1p1p4/3P4/5N2/PPQ2PPP/2R2RK1 w - - 0 1"
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        board = Some(boardFromFen(fen)),
        positionFeatures =
          Some(
            PositionFeatures.empty.copy(
              fen = fen,
              sideToMove = "white",
              lineControl = PositionFeatures.empty.lineControl.copy(whiteSemiOpenFiles = 1)
            )
          )
      )
    val pack =
      StrategyPack(
        sideToMove = "white",
        pieceRoutes = List(
          StrategyPieceRoute(
            ownerSide = "white",
            piece = "R",
            from = "c1",
            route = List("c4"),
            purpose = "use the c-file",
            strategicFit = 0.80,
            tacticalSafety = 0.80,
            surfaceConfidence = 0.80,
            surfaceMode = RouteSurfaceMode.Exact
          )
        )
      )

    val ideas = StrategicIdeaSelector.select(pack, semantic)
    val lineIdea = ideas.find(_.kind == StrategicIdeaKind.LineOccupation).getOrElse(fail(clue(ideas).toString))

    assert(lineIdea.focusSquares.contains("c4"), clue(lineIdea))
    assert(lineIdea.focusFiles.contains("c"), clue(lineIdea))
    assert(lineIdea.evidenceRefs.contains("source:line_control_features"), clue(lineIdea.evidenceRefs))
    assert(lineIdea.evidenceRefs.contains("line_control_shape"), clue(lineIdea.evidenceRefs))
    assert(lineIdea.evidenceRefs.contains("source:route_line_access"), clue(lineIdea.evidenceRefs))
    assert(lineIdea.evidenceRefs.contains("route_surface_exact"), clue(lineIdea.evidenceRefs))
    assert(lineIdea.evidenceRefs.exists(ref => ref == "open_file_c" || ref == "semi_open_file_c"), clue(lineIdea.evidenceRefs))
  }

  test("line-occupation producer keeps rook coordination shape facts for support surface") {
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        positionalFeatures = List(
          PositionalTag.RookOnSeventh(Color.White),
          PositionalTag.ConnectedRooks(Color.White)
        )
      )

    val ideas = StrategicIdeaSelector.select(StrategyPack(sideToMove = "white"), semantic)
    val refs = ideas.flatMap(_.evidenceRefs)

    assert(refs.contains("source:rook_on_seventh"), clue(refs))
    assert(refs.contains("rook_on_seventh_shape"), clue(refs))
    assert(refs.contains("source:connected_rooks"), clue(refs))
    assert(refs.contains("connected_rooks_shape"), clue(refs))
  }

  test("seventh-rank invasion motif reuses rook-on-seventh witnesses for support surface") {
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        motifs = List(Motif.SeventhRankInvasion(Color.White, plyIndex = 0, move = None))
      )

    val ideas = StrategicIdeaSelector.select(StrategyPack(sideToMove = "white"), semantic)
    val lineIdea =
      ideas.find(_.evidenceRefs.contains("source:rook_on_seventh")).getOrElse(fail(clue(ideas).toString))

    assertEquals(lineIdea.kind, StrategicIdeaKind.LineOccupation)
    assertEquals(lineIdea.focusZone, Some("back rank"))
    assert(lineIdea.beneficiaryPieces.contains("R"), clue(lineIdea))
    assert(lineIdea.evidenceRefs.contains("rook_on_seventh_shape"), clue(lineIdea.evidenceRefs))
  }

  test("outpost motif reuses outpost-tag witnesses for support surface") {
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        motifs = List(Motif.Outpost(Knight, Square.D5, Color.White, plyIndex = 0, move = Some("Nd5")))
      )

    val ideas = StrategicIdeaSelector.select(StrategyPack(sideToMove = "white"), semantic)
    val outpostIdea =
      ideas.find(_.evidenceRefs.contains("source:outpost_tag")).getOrElse(fail(clue(ideas).toString))

    assertEquals(outpostIdea.kind, StrategicIdeaKind.OutpostCreationOrOccupation)
    assert(outpostIdea.focusSquares.contains("d5"), clue(outpostIdea))
    assert(outpostIdea.beneficiaryPieces.contains("N"), clue(outpostIdea))
    assert(outpostIdea.evidenceRefs.contains("outpost_d5"), clue(outpostIdea.evidenceRefs))
    assertEquals(StrategicIdeaSelector.playerFacingIdeaText(outpostIdea), "an outpost on d5")
  }

  test("directional outpost access keeps typed square witness for support surface") {
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        positionalFeatures = List(PositionalTag.Outpost(Square.D5, Color.White))
      )
    val pack =
      StrategyPack(
        sideToMove = "white",
        directionalTargets = List(
          StrategyDirectionalTarget(
            targetId = "knight_d5_target",
            ownerSide = "white",
            piece = "N",
            from = "f4",
            targetSquare = "d5",
            readiness = DirectionalTargetReadiness.Build,
            strategicReasons = List("outpost pressure")
          )
        )
      )

    val ideas = StrategicIdeaSelector.select(pack, semantic)
    val outpostIdea =
      ideas.find(_.kind == StrategicIdeaKind.OutpostCreationOrOccupation).getOrElse(fail(clue(ideas).toString))

    assert(outpostIdea.focusSquares.contains("d5"), clue(outpostIdea))
    assert(outpostIdea.beneficiaryPieces.contains("N"), clue(outpostIdea))
    assert(outpostIdea.evidenceRefs.contains("source:directional_outpost_access"), clue(outpostIdea.evidenceRefs))
    assert(outpostIdea.evidenceRefs.contains("directional_outpost_access_shape"), clue(outpostIdea.evidenceRefs))
    assertEquals(StrategicIdeaSelector.playerFacingIdeaText(outpostIdea), "minor-piece outpost cue around d5")
  }

  test("exact route outpost access keeps typed square witness for support surface") {
    val fen = "4k3/8/8/8/5N2/8/8/4K3 w - - 0 1"
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        board = Some(boardFromFen(fen)),
        positionalFeatures = List(PositionalTag.Outpost(Square.D5, Color.White))
      )
    val pack =
      StrategyPack(
        sideToMove = "white",
        pieceRoutes = List(
          StrategyPieceRoute(
            ownerSide = "white",
            piece = "N",
            from = "f4",
            route = List("d5"),
            purpose = "occupy the outpost",
            strategicFit = 0.82,
            tacticalSafety = 0.80,
            surfaceConfidence = 0.82,
            surfaceMode = RouteSurfaceMode.Exact
          )
        )
      )

    val ideas = StrategicIdeaSelector.select(pack, semantic)
    val outpostIdea =
      ideas.find(_.kind == StrategicIdeaKind.OutpostCreationOrOccupation).getOrElse(fail(clue(ideas).toString))

    assert(outpostIdea.focusSquares.contains("d5"), clue(outpostIdea))
    assert(outpostIdea.beneficiaryPieces.contains("N"), clue(outpostIdea))
    assert(outpostIdea.evidenceRefs.contains("source:route_outpost_access"), clue(outpostIdea.evidenceRefs))
    assert(outpostIdea.evidenceRefs.contains("route_outpost_access_shape"), clue(outpostIdea.evidenceRefs))
    assert(outpostIdea.evidenceRefs.contains("route_surface_exact"), clue(outpostIdea.evidenceRefs))
    assertEquals(StrategicIdeaSelector.playerFacingIdeaText(outpostIdea), "minor-piece outpost access around d5")
  }

  test("minority-attack semantic support carries queenside target squares without release-family source") {
    val fen = "4k3/pp3ppp/2p5/3p4/1P1P4/4P3/P4PPP/4K3 w - - 0 1"
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        board = Some(boardFromFen(fen)),
        fen = fen,
        structuralWeaknesses = List(
          WeakComplex(
            color = Color.Black,
            squares = List(Square.C6),
            isOutpost = false,
            cause = "Minority-attack target on c6"
          )
        ),
        structureProfile = Some(
          StructureProfile(
            primary = StructureId.Carlsbad,
            confidence = 0.98,
            alternatives = Nil,
            centerState = CenterState.Locked,
            evidenceCodes = List("structure_carlsbad")
          )
        )
      )
    val pack = StrategyPack(sideToMove = "white")

    val ideas = StrategicIdeaSelector.select(pack, semantic)

    assertEquals(ideas.headOption.map(_.kind), Some(StrategicIdeaKind.TargetFixing))
    assert(ideas.headOption.exists(_.focusSquares.contains("c6")))
    assert(ideas.headOption.exists(_.evidenceRefs.contains("source:minority_attack_semantic")))
    assert(ideas.headOption.exists(_.evidenceRefs.contains("target_pressure_semantic")), clues(ideas.headOption.map(_.evidenceRefs)))
    assert(!ideas.headOption.exists(_.evidenceRefs.contains("source:target_pressure_semantic")))
    assert(!ideas.headOption.exists(_.evidenceRefs.contains("source:minority_attack_fixation")))
  }

  test("minority-attack support keeps flank fact when semantic observation is absent") {
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        positionalFeatures = List(PositionalTag.MinorityAttack(Color.White, "queenside")),
        structuralWeaknesses = List(
          WeakComplex(
            color = Color.Black,
            squares = List(Square.C6),
            isOutpost = false,
            cause = "Minority-attack target on c6"
          )
        )
      )
    val pack = StrategyPack(sideToMove = "white")

    val ideas = StrategicIdeaSelector.select(pack, semantic)

    assertEquals(ideas.headOption.map(_.kind), Some(StrategicIdeaKind.TargetFixing))
    assert(ideas.headOption.exists(_.focusSquares.contains("c6")), clue(ideas.headOption))
    assert(ideas.headOption.exists(_.evidenceRefs.contains("source:minority_attack_support")), clue(ideas.headOption.map(_.evidenceRefs)))
    assert(ideas.headOption.exists(_.evidenceRefs.contains("minority_attack_support_queenside")), clue(ideas.headOption.map(_.evidenceRefs)))
  }

  test("motif battery keeps axis and square witnesses for support surface") {
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        motifs =
          List(
            Motif.Battery(
              front = Bishop,
              back = Queen,
              axis = Motif.BatteryAxis.Diagonal,
              color = Color.White,
              plyIndex = 0,
              move = None,
              frontSq = Some(Square.D3),
              backSq = Some(Square.F3)
            )
          )
      )

    val ideas = StrategicIdeaSelector.select(StrategyPack(sideToMove = "white"), semantic)
    val attackIdea =
      ideas.find(_.evidenceRefs.contains("source:motif_battery")).getOrElse(fail(clue(ideas).toString))

    assertEquals(attackIdea.kind, StrategicIdeaKind.KingAttackBuildUp)
    assert(attackIdea.focusSquares.contains("d3"), clue(attackIdea))
    assert(attackIdea.focusSquares.contains("f3"), clue(attackIdea))
    assert(attackIdea.beneficiaryPieces.contains("B"), clue(attackIdea))
    assert(attackIdea.beneficiaryPieces.contains("Q"), clue(attackIdea))
    assert(attackIdea.evidenceRefs.contains("battery_axis_diagonal"), clue(attackIdea.evidenceRefs))
  }

  test("motif rook lift keeps file and rook witnesses for support surface") {
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        motifs = List(Motif.RookLift(File.F, fromRank = 1, toRank = 3, color = Color.White, plyIndex = 0, move = Some("Rf3")))
      )

    val ideas = StrategicIdeaSelector.select(StrategyPack(sideToMove = "white"), semantic)
    val attackIdea =
      ideas.find(_.evidenceRefs.contains("source:motif_rook_lift")).getOrElse(fail(clue(ideas).toString))

    assertEquals(attackIdea.kind, StrategicIdeaKind.KingAttackBuildUp)
    assert(attackIdea.focusFiles.contains("f"), clue(attackIdea))
    assert(attackIdea.beneficiaryPieces.contains("R"), clue(attackIdea))
    assertEquals(attackIdea.focusZone, Some("kingside"))
  }

  test("motif piece lift keeps piece and motif witnesses for support surface") {
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        board = Some(boardFromFen("6k1/8/8/8/8/8/8/4K3 w - - 0 1")),
        motifs = List(Motif.PieceLift(Knight, fromRank = 2, toRank = 5, color = Color.White, plyIndex = 0, move = Some("Nf5")))
      )

    val ideas = StrategicIdeaSelector.select(StrategyPack(sideToMove = "white"), semantic)
    val attackIdea =
      ideas.find(_.evidenceRefs.contains("source:motif_piece_lift")).getOrElse(fail(clue(ideas).toString))

    assertEquals(attackIdea.kind, StrategicIdeaKind.KingAttackBuildUp)
    assertEquals(attackIdea.focusSquares, List("f5"), clue(attackIdea))
    assert(attackIdea.beneficiaryPieces.contains("N"), clue(attackIdea))
    assert(attackIdea.evidenceRefs.contains("motif_piece_lift_shape"), clue(attackIdea.evidenceRefs))
    assert(attackIdea.evidenceRefs.contains("piece_lift_square_f5"), clue(attackIdea.evidenceRefs))
    assertEquals(attackIdea.focusZone, Some("kingside"))
  }

  test("motif check pressure keeps check-type witness for support surface") {
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        board = Some(boardFromFen("6k1/8/8/8/8/8/8/4K3 w - - 0 1")),
        motifs = List(Motif.Check(Queen, Square.H7, Motif.CheckType.Normal, Color.White, plyIndex = 0, move = Some("Qh7+")))
      )

    val ideas = StrategicIdeaSelector.select(StrategyPack(sideToMove = "white"), semantic)
    val attackIdea =
      ideas.find(_.evidenceRefs.contains("source:motif_check_pressure")).getOrElse(fail(clue(ideas).toString))

    assertEquals(attackIdea.kind, StrategicIdeaKind.KingAttackBuildUp)
    assert(attackIdea.focusSquares.contains("h7"), clue(attackIdea))
    assert(attackIdea.beneficiaryPieces.contains("Q"), clue(attackIdea))
    assert(attackIdea.evidenceRefs.contains("check_type_normal"), clue(attackIdea.evidenceRefs))
    assertEquals(attackIdea.focusZone, Some("kingside"))
  }

  test("fianchetto motif keeps side and bishop witnesses for support surface") {
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        board = Some(boardFromFen("6k1/8/8/8/8/8/6B1/4K3 w - - 0 1")),
        motifs = List(Motif.Fianchetto(Motif.FianchettoSide.Kingside, Color.White, plyIndex = 0, move = Some("Bg2")))
      )

    val ideas = StrategicIdeaSelector.select(StrategyPack(sideToMove = "white"), semantic)
    val supportIdea =
      ideas.find(_.evidenceRefs.contains("source:fianchetto_motif")).getOrElse(fail(clue(ideas).toString))

    assertEquals(supportIdea.kind, StrategicIdeaKind.KingAttackBuildUp)
    assert(supportIdea.beneficiaryPieces.contains("B"), clue(supportIdea))
    assertEquals(supportIdea.focusZone, Some("kingside"))
    assert(supportIdea.evidenceRefs.contains("fianchetto_motif_shape"), clue(supportIdea.evidenceRefs))
    assert(supportIdea.evidenceRefs.contains("fianchetto_side_kingside"), clue(supportIdea.evidenceRefs))
  }

  test("generic initiative motif does not become king-attack support by itself") {
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        board = Some(boardFromFen("6k1/8/8/8/8/8/8/4K3 w - - 0 1")),
        motifs = List(Motif.Initiative(Color.White, score = 12, plyIndex = 0, move = Some("Qh5")))
      )

    val ideas = StrategicIdeaSelector.select(StrategyPack(sideToMove = "white"), semantic)

    assert(!ideas.exists(_.evidenceRefs.contains("source:initiative_motif")), clue(ideas))
  }

  test("weak-back-rank motif keeps enemy-back-rank witnesses for support surface") {
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        board = Some(boardFromFen("6k1/6pp/8/8/8/8/8/4K3 w - - 0 1")),
        motifs = List(Motif.WeakBackRank(Color.Black, plyIndex = 0, move = None))
      )

    val ideas = StrategicIdeaSelector.select(StrategyPack(sideToMove = "white"), semantic)
    val attackIdea =
      ideas.find(_.evidenceRefs.contains("source:enemy_weak_back_rank")).getOrElse(fail(clue(ideas).toString))

    assertEquals(attackIdea.kind, StrategicIdeaKind.KingAttackBuildUp)
    assertEquals(attackIdea.focusZone, Some("kingside"))
    assert(attackIdea.evidenceRefs.contains("enemy_weak_back_rank_shape"), clue(attackIdea.evidenceRefs))
  }

  test("compensation development lead promotes king-attack build-up over broad space") {
    val fen = "r4rk1/ppp2ppp/2n5/3p4/3P4/2NB1Q2/PPP2PPP/R1B2RK1 w - - 0 1"
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        board = Some(boardFromFen(fen)),
        positionalFeatures = List(PositionalTag.SpaceAdvantage(Color.White)),
        plans = List(
          planMatch(Plan.OpeningDevelopment(Color.White), 0.82),
          planMatch(Plan.KingsideAttack(Color.White), 0.79),
          planMatch(Plan.SpaceAdvantage(Color.White), 0.77)
        ),
        motifs = List(
          Motif.Battery(
            front = Bishop,
            back = Queen,
            axis = Motif.BatteryAxis.Diagonal,
            color = Color.White,
            plyIndex = 0,
            move = None,
            frontSq = Some(Square.D3),
            backSq = Some(Square.F3)
          )
        ),
        positionFeatures = Some(
          compensationFeatures(
            fen = fen,
            materialDiff = -2,
            whiteDevelopmentLag = 0,
            blackDevelopmentLag = 3,
            whiteAttackersCount = 2,
            blackKingRingAttacked = 2,
            blackKingExposedFiles = 1,
            whiteSemiOpenFiles = 1,
            openFilesCount = 1,
            spaceDiff = 2
          )
        ),
        phase = "opening"
      )
    val pack =
      StrategyPack(
        sideToMove = "white",
        pieceRoutes = List(
          StrategyPieceRoute(
            ownerSide = "white",
            piece = "B",
            from = "d3",
            route = List("h7"),
            purpose = "keep the king under pressure",
            strategicFit = 0.86,
            tacticalSafety = 0.76,
            surfaceConfidence = 0.86,
            surfaceMode = RouteSurfaceMode.Exact
          )
        )
      )

    val ideas = StrategicIdeaSelector.select(pack, semantic)

    assertEquals(ideas.headOption.map(_.kind), Some(StrategicIdeaKind.KingAttackBuildUp))
    assert(ideas.headOption.exists(_.evidenceRefs.contains("source:compensation_development_lead")))
    assert(ideas.headOption.exists(_.evidenceRefs.contains("source:compensation_diagonal_battery")), clues(ideas.headOption.map(_.evidenceRefs)))
    assert(ideas.headOption.exists(_.evidenceRefs.contains("material_deficit_compensation")), clues(ideas.headOption.map(_.evidenceRefs)))
    assert(ideas.headOption.exists(_.evidenceRefs.contains("battery_axis_diagonal")), clues(ideas.headOption.map(_.evidenceRefs)))
    assert(ideas.headOption.exists(_.evidenceRefs.contains("compensation_battery_square_d3")), clues(ideas.headOption.map(_.evidenceRefs)))
    assert(ideas.headOption.exists(_.evidenceRefs.contains("compensation_battery_square_f3")), clues(ideas.headOption.map(_.evidenceRefs)))
  }

  test("exact route attack lane keeps typed endpoint witness for support surface") {
    val fen = "r4rk1/ppp2ppp/8/8/8/3B4/PPP2PPP/R5K1 w - - 0 1"
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        board = Some(boardFromFen(fen)),
        phase = "middlegame"
      )
    val pack =
      StrategyPack(
        sideToMove = "white",
        pieceRoutes = List(
          StrategyPieceRoute(
            ownerSide = "white",
            piece = "B",
            from = "d3",
            route = List("h7"),
            purpose = "attack the king",
            strategicFit = 0.84,
            tacticalSafety = 0.80,
            surfaceConfidence = 0.84,
            surfaceMode = RouteSurfaceMode.Exact
          )
        )
      )

    val ideas = StrategicIdeaSelector.select(pack, semantic)

    assertEquals(ideas.headOption.map(_.kind), Some(StrategicIdeaKind.KingAttackBuildUp))
    assert(ideas.headOption.exists(_.evidenceRefs.contains("source:route_attack_lane")), clues(ideas.headOption.map(_.evidenceRefs)))
    assert(ideas.headOption.exists(_.evidenceRefs.contains("route_attack_lane_shape")), clues(ideas.headOption.map(_.evidenceRefs)))
    assert(ideas.headOption.exists(_.evidenceRefs.contains("attack_lane_board_attack")), clues(ideas.headOption.map(_.evidenceRefs)))
    assert(ideas.headOption.exists(_.focusSquares.contains("h7")), clue(ideas.headOption))
    assertEquals(ideas.headOption.flatMap(_.focusZone), Some("kingside"))
  }

  test("directional attack lane keeps typed endpoint witness for support surface") {
    val fen = "r4rk1/ppp2ppp/8/8/8/3B4/PPP2PPP/R5K1 w - - 0 1"
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        board = Some(boardFromFen(fen)),
        phase = "middlegame"
      )
    val pack =
      StrategyPack(
        sideToMove = "white",
        directionalTargets = List(
          StrategyDirectionalTarget(
            targetId = "queen_h7_target",
            ownerSide = "white",
            piece = "Q",
            from = "d3",
            targetSquare = "h7",
            readiness = DirectionalTargetReadiness.Build,
            strategicReasons = List("attack the king")
          )
        )
      )

    val ideas = StrategicIdeaSelector.select(pack, semantic)

    assertEquals(ideas.headOption.map(_.kind), Some(StrategicIdeaKind.KingAttackBuildUp))
    assert(ideas.headOption.exists(_.evidenceRefs.contains("source:directional_attack_lane")), clues(ideas.headOption.map(_.evidenceRefs)))
    assert(ideas.headOption.exists(_.evidenceRefs.contains("directional_attack_lane_shape")), clues(ideas.headOption.map(_.evidenceRefs)))
    assert(ideas.headOption.exists(_.evidenceRefs.contains("attack_lane_board_attack")), clues(ideas.headOption.map(_.evidenceRefs)))
    assert(ideas.headOption.exists(_.focusSquares.contains("h7")), clue(ideas.headOption))
    assertEquals(ideas.headOption.flatMap(_.focusZone), Some("kingside"))
  }

  test("route and directional target near an uncastled king require a board-proved attack lane") {
    val fen = "4k3/8/8/8/4n3/8/8/4K3 b - - 0 1"
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "black",
        board = Some(boardFromFen(fen)),
        phase = "middlegame"
      )
    val pack =
      StrategyPack(
        sideToMove = "black",
        pieceRoutes = List(
          StrategyPieceRoute(
            ownerSide = "black",
            piece = "N",
            from = "e4",
            route = List("c3"),
            purpose = "centralization route",
            strategicFit = 0.80,
            tacticalSafety = 0.80,
            surfaceConfidence = 0.82,
            surfaceMode = RouteSurfaceMode.Exact
          )
        ),
        directionalTargets = List(
          StrategyDirectionalTarget(
            targetId = "black_knight_c3_target",
            ownerSide = "black",
            piece = "N",
            from = "e4",
            targetSquare = "c3",
            readiness = DirectionalTargetReadiness.Build,
            strategicReasons = List("central foothold")
          )
        )
      )

    val ideas = StrategicIdeaSelector.select(pack, semantic)
    val refs = ideas.flatMap(_.evidenceRefs)

    assert(!refs.contains("source:route_attack_lane"), clues(ideas))
    assert(!refs.contains("source:directional_attack_lane"), clues(ideas))
    assert(!refs.contains("attack_lane_board_attack"), clues(ideas))
  }

  test("pawn analysis break ready keeps typed file witness for support surface") {
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        pawnAnalysis = Some(
          PawnPlayAnalysis(
            pawnBreakReady = true,
            breakFile = Some("d"),
            breakImpact = 220,
            advanceOrCapture = true,
            passedPawnUrgency = PassedPawnUrgency.Background,
            passerBlockade = false,
            blockadeSquare = None,
            blockadeRole = None,
            pusherSupport = false,
            minorityAttack = false,
            counterBreak = false,
            tensionPolicy = TensionPolicy.Release,
            tensionSquares = List("d4", "e5"),
            primaryDriver = "break_ready",
            notes = "d-file break is ready"
          )
        )
      )
    val ideas = StrategicIdeaSelector.select(StrategyPack(sideToMove = "white"), semantic)
    val breakIdea = ideas.find(_.kind == StrategicIdeaKind.PawnBreak).getOrElse(fail(clue(ideas).toString))

    assert(breakIdea.evidenceRefs.contains("source:pawn_analysis_break_ready"), clue(breakIdea.evidenceRefs))
    assert(breakIdea.evidenceRefs.contains("pawn_analysis_break_ready_shape"), clue(breakIdea.evidenceRefs))
    assert(breakIdea.focusFiles.contains("d"), clue(breakIdea))
  }

  test("pawn-break motif keeps file witnesses for support surface") {
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        motifs = List(Motif.PawnBreak(File.D, File.E, Color.White, plyIndex = 0, move = Some("dxe5")))
      )

    val ideas = StrategicIdeaSelector.select(StrategyPack(sideToMove = "white"), semantic)
    val breakIdea =
      ideas.find(_.evidenceRefs.contains("source:pawn_break_motif")).getOrElse(fail(clue(ideas).toString))

    assertEquals(breakIdea.kind, StrategicIdeaKind.PawnBreak)
    assert(breakIdea.focusFiles.contains("d"), clue(breakIdea))
    assert(breakIdea.focusFiles.contains("e"), clue(breakIdea))
    assertEquals(breakIdea.focusZone, Some("center"))
    assert(breakIdea.evidenceRefs.contains("pawn_break_motif_shape"), clue(breakIdea.evidenceRefs))
    assert(breakIdea.evidenceRefs.contains("break_file_d"), clue(breakIdea.evidenceRefs))
    assert(breakIdea.evidenceRefs.contains("break_target_file_e"), clue(breakIdea.evidenceRefs))
  }

  test("central break tension keeps typed file witness for support surface") {
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        pawnPlay = Some(
          PawnPlayTable(
            breakReady = false,
            breakFile = Some("d"),
            breakImpact = "Medium",
            tensionPolicy = "Maintain",
            tensionReason = "central tension around d-file break",
            passedPawnUrgency = "Background",
            passerBlockade = None,
            counterBreak = false,
            primaryDriver = "central_tension"
          )
        ),
        positionFeatures = Some(
          PositionFeatures.empty.copy(
            centralSpace = PositionFeatures.empty.centralSpace.copy(
              lockedCenter = true,
              pawnTensionCount = 1
            )
          )
        )
      )
    val ideas = StrategicIdeaSelector.select(StrategyPack(sideToMove = "white"), semantic)
    val breakIdea = ideas.find(_.kind == StrategicIdeaKind.PawnBreak).getOrElse(fail(clue(ideas).toString))

    assert(breakIdea.evidenceRefs.contains("source:central_break_tension"), clue(breakIdea.evidenceRefs))
    assert(breakIdea.evidenceRefs.contains("locked_center"), clue(breakIdea.evidenceRefs))
    assert(breakIdea.focusFiles.contains("d"), clue(breakIdea))
  }

  test("direct central break collision does not produce counterplay-suppression evidence") {
    val fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1"
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "black",
        fen = fen,
        playedMove = Some("e7e5"),
        board = Some(boardFromFen(fen)),
        preventedPlans = List(
          PreventedPlan(
            planId = "SameDestinationE5",
            deniedSquares = List(Square.E5),
            breakNeutralized = Some("e4-e5"),
            mobilityDelta = -2,
            counterplayScoreDrop = 140,
            deniedResourceClass = Some("break")
          )
        )
      )

    val ideas = StrategicIdeaSelector.select(StrategyPack(sideToMove = "black"), semantic)

    assert(!ideas.exists(_.kind == StrategicIdeaKind.CounterplaySuppression), clue(ideas))
    assert(!ideas.exists(_.evidenceRefs.contains("source:counterplay_suppression")), clue(ideas))
  }

  test("blockade motif keeps passer-restraint witnesses for support surface") {
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        motifs = List(Motif.Blockade(Knight, Square.D5, Square.D4, Color.White, plyIndex = 0, move = Some("Nd5")))
      )

    val ideas = StrategicIdeaSelector.select(StrategyPack(sideToMove = "white"), semantic)
    val restraintIdea =
      ideas.find(_.evidenceRefs.contains("source:passer_blockade_motif")).getOrElse(fail(clue(ideas).toString))

    assertEquals(restraintIdea.kind, StrategicIdeaKind.CounterplaySuppression)
    assert(restraintIdea.focusSquares.contains("d5"), clue(restraintIdea))
    assert(restraintIdea.focusSquares.contains("d4"), clue(restraintIdea))
    assert(restraintIdea.focusFiles.contains("d"), clue(restraintIdea))
    assert(restraintIdea.beneficiaryPieces.contains("N"), clue(restraintIdea))
    assert(restraintIdea.evidenceRefs.contains("passer_blockade_shape"), clue(restraintIdea.evidenceRefs))
    assert(restraintIdea.evidenceRefs.contains("blockade_square_d5"), clue(restraintIdea.evidenceRefs))
    assert(restraintIdea.evidenceRefs.contains("blockaded_pawn_d4"), clue(restraintIdea.evidenceRefs))
  }

  test("compensation open lines keep line occupation ahead of generic space") {
    val fen = "r4rk1/2p2ppp/p1n5/1p1p4/3P4/5N2/PPQ2PPP/2R2RK1 w - - 0 1"
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        board = Some(boardFromFen(fen)),
        positionalFeatures = List(PositionalTag.SpaceAdvantage(Color.White)),
        plans = List(
          planMatch(Plan.OpeningDevelopment(Color.White), 0.80),
          planMatch(Plan.FileControl(Color.White, "c-file"), 0.78),
          planMatch(Plan.PieceActivation(Color.White), 0.75)
        ),
        positionFeatures = Some(
          compensationFeatures(
            fen = fen,
            materialDiff = -1,
            whiteDevelopmentLag = 0,
            blackDevelopmentLag = 2,
            blackCastledSide = "short",
            whiteSemiOpenFiles = 1,
            openFilesCount = 1,
            spaceDiff = 2
          )
        ),
        phase = "opening"
      )
    val pack =
      StrategyPack(
        sideToMove = "white",
        pieceRoutes = List(
          StrategyPieceRoute(
            ownerSide = "white",
            piece = "R",
            from = "c1",
            route = List("c7"),
            purpose = "occupy the c-file before recovering material",
            strategicFit = 0.84,
            tacticalSafety = 0.80,
            surfaceConfidence = 0.84,
            surfaceMode = RouteSurfaceMode.Exact
          )
        )
      )

    val ideas = StrategicIdeaSelector.select(pack, semantic)

    assertEquals(ideas.headOption.map(_.kind), Some(StrategicIdeaKind.LineOccupation))
    assert(ideas.headOption.exists(_.evidenceRefs.exists(ref =>
      ref == "source:compensation_open_lines" || ref == "source:delayed_recovery_window"
    )))
    assert(ideas.headOption.exists(_.focusFiles.nonEmpty), clue(ideas.headOption))
    assert(ideas.headOption.exists(_.evidenceRefs.exists(ref =>
      ref.startsWith("open_file_") || ref.startsWith("semi_open_file_") || ref == "seventh_rank_entry"
    )))
    assert(ideas.headOption.exists(_.evidenceRefs.contains("compensation_open_lines_shape")))
  }

  test("compensation target fixation keeps Open Catalan pressure target-led") {
    val ideas =
      selectFromFen(
        "1rbqk2r/1pp1bppp/p3pn2/n7/P1pP4/4P1P1/1P1N1PBP/RNBQ1RK1 w k - 3 10",
        phase = "opening"
      )

    assertEquals(ideas.headOption.map(_.kind), Some(StrategicIdeaKind.TargetFixing))
    assert(ideas.headOption.exists(_.evidenceRefs.contains("source:compensation_target_fixation")))
  }

  test("quiet compensation line pressure stays ahead of attack family when king window is weak") {
    val fen = "r1b2rk1/1pp2ppp/p1n5/3p4/1P1P4/2P2N2/P1Q2PPP/2R2RK1 w - - 0 1"
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        board = Some(boardFromFen(fen)),
        plans = List(
          planMatch(Plan.KingsideAttack(Color.White), 0.79),
          planMatch(Plan.FileControl(Color.White, "c-file"), 0.81),
          planMatch(Plan.PieceActivation(Color.White), 0.74)
        ),
        positionFeatures = Some(
          compensationFeatures(
            fen = fen,
            materialDiff = -1,
            whiteDevelopmentLag = 0,
            blackDevelopmentLag = 1,
            blackCastledSide = "short",
            whiteSemiOpenFiles = 1,
            openFilesCount = 1,
            spaceDiff = 1,
            phase = "middlegame"
          )
        ),
        phase = "middlegame"
      )
    val pack =
      StrategyPack(
        sideToMove = "white",
        pieceRoutes = List(
          StrategyPieceRoute(
            ownerSide = "white",
            piece = "R",
            from = "c1",
            route = List("c1", "c5"),
            purpose = "occupy the c-file before recovering material",
            strategicFit = 0.82,
            tacticalSafety = 0.79,
            surfaceConfidence = 0.83,
            surfaceMode = RouteSurfaceMode.Exact
          )
        )
      )

    val ideas = StrategicIdeaSelector.select(pack, semantic)

    assertEquals(ideas.headOption.map(_.kind), Some(StrategicIdeaKind.LineOccupation))
    assert(ideas.headOption.forall(_.kind != StrategicIdeaKind.KingAttackBuildUp))
  }

  test("compensation counterplay denial produces suppression carrier instead of drifting to attack") {
    val fen = "r2q1rk1/1p2bppp/p1n1pn2/2pp4/3P4/1PN1PN2/PBQ2PPP/2RR2K1 w - - 0 1"
    val semantic =
      StrategicIdeaSemanticContext(
        sideToMove = "white",
        board = Some(boardFromFen(fen)),
        preventedPlans = List(
          PreventedPlan(
            planId = "DenyB5",
            deniedSquares = List(Square.B5, Square.C4),
            breakNeutralized = Some("b5"),
            mobilityDelta = -2,
            counterplayScoreDrop = 140,
            deniedResourceClass = Some("break")
          )
        ),
        plans = List(
          planMatch(Plan.Prophylaxis(Color.White, "b5 break"), 0.80),
          planMatch(Plan.FileControl(Color.White, "b-file"), 0.78)
        ),
        positionFeatures = Some(
          compensationFeatures(
            fen = fen,
            materialDiff = -1,
            whiteDevelopmentLag = 0,
            blackDevelopmentLag = 1,
            blackCastledSide = "short",
            whiteSemiOpenFiles = 1,
            openFilesCount = 1,
            spaceDiff = 1,
            phase = "middlegame"
          )
        ),
        phase = "middlegame"
      )
    val pack =
      StrategyPack(
        sideToMove = "white",
        pieceRoutes = List(
          StrategyPieceRoute(
            ownerSide = "white",
            piece = "R",
            from = "d1",
            route = List("d1", "b1", "b5"),
            purpose = "queenside pressure",
            strategicFit = 0.80,
            tacticalSafety = 0.76,
            surfaceConfidence = 0.80,
            surfaceMode = RouteSurfaceMode.Toward
          )
        )
      )

    val ideas = StrategicIdeaSelector.select(pack, semantic)
    val suppressionIdea =
      ideas.find(_.evidenceRefs.contains("source:compensation_counterplay_denial")).getOrElse(fail(clue(ideas).toString))

    assert(ideas.exists(signal => signal.kind == StrategicIdeaKind.CounterplaySuppression))
    assert(suppressionIdea.evidenceRefs.contains("material_deficit_compensation"), clue(suppressionIdea.evidenceRefs))
    assert(suppressionIdea.evidenceRefs.contains("break_neutralized"), clue(suppressionIdea.evidenceRefs))
    assert(suppressionIdea.focusFiles.contains("b"), clue(suppressionIdea))
    assert(ideas.headOption.forall(_.kind != StrategicIdeaKind.KingAttackBuildUp))
  }
