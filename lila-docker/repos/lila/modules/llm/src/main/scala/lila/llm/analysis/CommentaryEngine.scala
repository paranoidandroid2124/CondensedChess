package lila.llm.analysis

import _root_.chess.*
import _root_.chess.format.Uci
import lila.llm.{ GameChronicleMoment, LlmConfig, LlmLevel, NarrativeSignalDigest }
import lila.llm.analysis.structure.{ PawnStructureClassifier, PlanAlignmentScorer, StructuralPlaybook }
import lila.llm.model.*
import lila.llm.model.authoring.{ NarrativeOutline, OutlineBeat, OutlineBeatKind }
import lila.llm.model.strategic.{ VariationLine, PlanContinuity, StrategicSalience }
import lila.llm.analysis.L3.*
import lila.llm.analysis.PositionAnalyzer
import lila.llm.analysis.ThemeTaxonomy.ThemeResolver
import lila.llm.analysis.DecisiveTruth.toContract
import scala.annotation.unused
// import scala.util.{ Either, Left, Right } // Removed as it caused warnings

/**
 * The central hub for chess position analysis. 
 * Orchestrates motifs, plans, and position nature characterization.
 */
case class PositionAssessment(
    pos: Position,
    motifs: List[Motif],
    plans: PlanScoringResult,
    nature: PositionNature,
    phase: GamePhase
)

object CommentaryEngine:
  private val llmConfig = LlmConfig.fromEnv

  private[llm] final case class TruthBoundArcMoment(
      moment: GameArcMoment,
      truthFrame: MoveTruthFrame,
      rawStrategyPack: Option[lila.llm.StrategyPack],
      truthContract: DecisiveTruthContract
  )

  private[llm] final case class DiagnosticWitness(
      ply: Int,
      momentType: Option[String] = None,
      label: Option[String] = None
  )

  private[llm] final case class TruthTraceMoment(
      ply: Int,
      momentType: String,
      selectionKind: String,
      selectionLabel: Option[String],
      selectionReason: Option[String],
      traceSource: String,
      anchorMoment: Boolean,
      bridgeCandidate: Boolean,
      selectedBridge: Boolean,
      finalInternal: Boolean,
      visibleMoment: Boolean,
      activeNoteMoment: Boolean,
      wholeGamePromoted: Boolean,
      strategicThreadId: Option[String],
      moveClassification: Option[String],
      playedMove: Option[String],
      verifiedBestMove: Option[String],
      truthClass: String,
      truthPhase: Option[String],
      reasonFamily: String,
      ownershipRole: String,
      visibilityRole: String,
      surfaceMode: String,
      exemplarRole: String,
      surfacedMoveOwnsTruth: Boolean,
      compensationAllowed: Boolean,
      compensationProseAllowed: Boolean,
      benchmarkProseAllowed: Boolean,
      verifiedPayoffAnchor: Option[String],
      chainKey: Option[String],
      moveQualityVerdict: String,
      cpLoss: Int,
      swingSeverity: Int,
      severityBand: String,
      investedMaterialCp: Option[Int],
      beforeDeficit: Int,
      afterDeficit: Int,
      deficitDelta: Int,
      movingPieceValue: Int,
      capturedPieceValue: Int,
      sacrificeKind: Option[String],
      valueDownCapture: Boolean,
      increasesDeficit: Boolean,
      recoversDeficit: Boolean,
      overinvestment: Boolean,
      uncompensatedLoss: Boolean,
      forcedRecovery: Boolean,
      createsFreshInvestment: Boolean,
      maintainsInvestment: Boolean,
      convertsInvestment: Boolean,
      durablePressure: Boolean,
      currentMoveEvidence: Boolean,
      currentConcreteCarrier: Boolean,
      freshCommitmentCandidate: Boolean,
      ownerEligible: Boolean,
      legacyVisibleOnly: Boolean,
      maintenanceExemplarCandidate: Boolean,
      evidenceProvenance: List[String],
      failureMode: String,
      failureIntentConfidence: Double,
      failureIntentAnchor: Option[String],
      failureInterpretationAllowed: Boolean,
      rawDominantIdea: Option[String],
      rawSecondaryIdea: Option[String],
      rawExecution: Option[String],
      rawObjective: Option[String],
      rawFocus: Option[String],
      rawLongTermFocus: List[String],
      rawDirectionalTargets: List[String],
      rawPieceRoutes: List[String],
      rawPieceMoveRefs: List[String],
      rawCompensationSummary: Option[String],
      rawCompensationVectors: List[String],
      rawInvestedMaterial: Option[Int],
      sanitizedDominantIdea: Option[String],
      sanitizedSecondaryIdea: Option[String],
      sanitizedExecution: Option[String],
      sanitizedObjective: Option[String],
      sanitizedFocus: Option[String]
  )

  private[llm] final case class GameArcDiagnostic(
      arc: GameArc,
      anchorPlies: List[Int],
      candidateBridgePlies: List[Int],
      selectedBridgePlies: List[Int],
      finalInternalPlies: List[Int],
      visibleMomentPlies: List[Int],
      activeNotePlies: List[Int],
      promotedWholeGamePly: Option[Int],
      canonicalTraceMoments: List[TruthTraceMoment],
      witnessTraceMoments: List[TruthTraceMoment]
  )

  private[llm] case class WholeGameConclusionSupport(
      mainContest: Option[String] = None,
      decisiveShift: Option[String] = None,
      payoff: Option[String] = None
  )

  private case class WholeGameAnchor(
      text: String,
      fullSentence: Boolean
  )

  private def themeMaxShareFromHypotheses(
      hypotheses: List[lila.llm.model.authoring.PlanHypothesis]
  ): Double =
    val themes = hypotheses.map(ThemeResolver.fromHypothesis).map(_.id).filter(_.nonEmpty)
    if themes.isEmpty then 1.0
    else
      val counts = themes.groupBy(identity).view.mapValues(_.size).toMap
      counts.values.maxOption.getOrElse(0).toDouble / themes.size.toDouble

  // Lazily instantiated Analyzers (stateless)
  private val prophylaxisAnalyzer = new lila.llm.analysis.strategic.ProphylaxisAnalyzerImpl()
  private val activityAnalyzer = new lila.llm.analysis.strategic.ActivityAnalyzerImpl()
  private val structureAnalyzer = new lila.llm.analysis.strategic.StructureAnalyzerImpl()
  private val endgameAnalyzer = new lila.llm.analysis.strategic.EndgameAnalyzerImpl()
  private val practicalityScorer = new lila.llm.analysis.strategic.PracticalityScorerImpl()
  
  private val semanticExtractor = new lila.llm.analysis.StrategicFeatureExtractorImpl(
    prophylaxisAnalyzer,
    activityAnalyzer,
    structureAnalyzer,
    endgameAnalyzer,
    practicalityScorer
  )

  def assess(
      fen: String,
      pv: List[String]
  ): Option[PositionAssessment] =
    _root_.chess.format.Fen.read(_root_.chess.variant.Standard, _root_.chess.format.Fen.Full(fen)).flatMap { initialPos =>
      PositionAnalyzer.extractFeatures(fen, 1).map { features =>
        val motifs = MoveAnalyzer.tokenizePv(fen, pv)
        
        val lastPos = pv.foldLeft(initialPos) { (p, uciStr) =>
          Uci(uciStr).collect { case m: Uci.Move => m }.flatMap(p.move(_).toOption).map(_.after).getOrElse(p)
        }

        val lastFen = _root_.chess.format.Fen.write(lastPos).value
        val nature =
          PositionCharacterizer.characterize(
            pos = lastPos,
            features = Nil,
            evalCp = Some(0), // No PV evaluation available in assess()
            material = lastFen.takeWhile(_ != ' ')
          )
        val currentPhase = determinePhase(lastPos)
        val dummyPv = List(PvLine(pv, 0, None, 0))
        val classification = PositionClassifier.classify(
          features = features,
          multiPv = dummyPv,
          currentEval = 0,
          tacticalMotifsCount = motifs.size
        )
        val ownThreats = ThreatAnalyzer.analyze(
           fen = fen,
           motifs = motifs,
           multiPv = dummyPv,
           phase1 = classification,
           sideToMove = lastPos.color.name
        )
        val oppThreats = ThreatAnalyzer.analyze(
           fen = fen,
           motifs = motifs,
           multiPv = dummyPv,
           phase1 = classification,
           sideToMove = (!lastPos.color).name
        )
        val pawnAnalysis = BreakAnalyzer.analyze(
          features = features,
          motifs = motifs,
          phase1 = classification,
          sideToMove = lastPos.color.name
        )
        val oppPawnAnalysis = BreakAnalyzer.analyze(
          features = features,
          motifs = motifs,
          phase1 = classification,
          sideToMove = (!lastPos.color).name
        )
        
        val ctx = IntegratedContext(
          evalCp = 0, // General assess (no PV evaluation yet)
          classification = Some(classification),
          pawnAnalysis = Some(pawnAnalysis),
          opponentPawnAnalysis = Some(oppPawnAnalysis),
          threatsToUs = Some(ownThreats),
          threatsToThem = Some(oppThreats),
          openingName = None,
          isWhiteToMove = lastPos.color == White,
          positionKey = Some(fen),
          features = Some(features),
          initialPos = Some(initialPos)
        )
        
        val planScoring = PlanMatcher.matchPlans(motifs, ctx, lastPos.color)

        PositionAssessment(
          pos = lastPos,
          motifs = motifs,
          plans = planScoring,
          nature = nature,
          phase = currentPhase
        )
      }
    }

  private def determinePhase(pos: Position): GamePhase =
    val pieces = pos.board.occupied.count
    if (pieces > 28) GamePhase.Opening
    else if (pieces < 12) GamePhase.Endgame
    else GamePhase.Middlegame

  private def evolveContinuity(
      prev: Option[PlanContinuity],
      currentPrimary: PlanMatch,
      ply: Int
  ): Option[PlanContinuity] =
    val currentKey = currentPrimary.plan.id.toString.toLowerCase
    prev match
      case Some(p) if p.planId.map(_.toLowerCase).contains(currentKey) =>
        Some(
          p.copy(
            planName = currentPrimary.plan.name,
            planId = Some(currentPrimary.plan.id.toString),
            consecutivePlies = p.consecutivePlies + 1
          )
        )
      case Some(p) if p.planName.equalsIgnoreCase(currentPrimary.plan.name) =>
        Some(
          p.copy(
            planName = currentPrimary.plan.name,
            planId = Some(currentPrimary.plan.id.toString),
            consecutivePlies = p.consecutivePlies + 1
          )
        )
      case _ =>
        Some(
          PlanContinuity(
            planName = currentPrimary.plan.name,
            planId = Some(currentPrimary.plan.id.toString),
            consecutivePlies = 1,
            startingPly = ply
          )
        )

  /**
   * Formats the assessment into a sparse text representation for the LLM prompt.
   */
  def formatSparse(assessment: PositionAssessment): String =
    val p = assessment.plans
    val n = assessment.nature
    
    val planText = p.topPlans.map { m =>
      s"- Plan: ${m.plan.name} (score: ${"%.2f".format(m.score)})"
    }.mkString("\n")

    s"""NATURE: ${n.natureType} (tension=${"%.2f".format(n.tension)}, stability=${"%.2f".format(n.stability)})
       |${n.description}
       |
       |TOP STRATEGIES:
       |$planText
       |
       |OBSERVED MOTIFS:
       |${assessment.motifs.take(8).map(m => s"- ${m.getClass.getSimpleName}: ${m.move.getOrElse("?")}").mkString("\n")}
       |""".stripMargin

  // Legacy overload for backward compatibility
  @scala.annotation.targetName("assessExtendedLegacy")
  def assessExtended(
      fen: String,
      pv: List[String],
      ply: Int,
      prevMove: Option[String],
      probeResults: List[ProbeResult]
  ): Option[ExtendedAnalysisData] = {
      val mainVariation = VariationLine(
        moves = pv,
        scoreCp = 0, 
        mate = None,
        resultingFen = Some(NarrativeUtils.uciListToFen(fen, pv)),
        tags = Nil
      )
      assessExtended(
        fen = fen,
        variations = List(mainVariation),
        playedMove = None,
        opening = None,
        phase = None,
        ply = ply,
        prevMove = prevMove,
        probeResults = probeResults
      )
  }

  def assessExtended(
      fen: String,
      variations: List[VariationLine],
      playedMove: Option[String] = None,
      opening: Option[String] = None,
      phase: Option[String] = None,
      ply: Int = 0,
      prevMove: Option[String] = None,
      prevPlanContinuity: Option[PlanContinuity] = None,
      probeResults: List[ProbeResult] = Nil,
      evalDeltaCp: Option[Int] = None,
      prevEndgameState: Option[lila.llm.model.strategic.EndgamePatternState] = None
  ): Option[ExtendedAnalysisData] =
    _root_.chess.format.Fen.read(_root_.chess.variant.Standard, _root_.chess.format.Fen.Full(fen)).flatMap { initialPos =>
      PositionAnalyzer.extractFeatures(fen, 1).map { features =>
        val mainPv = variations.headOption.map(_.moves).getOrElse(Nil)
        val motifs = MoveAnalyzer.tokenizePv(fen, mainPv)
        
        val lastPos = mainPv.foldLeft(initialPos) { (p, uciStr) =>
          Uci(uciStr).collect { case m: Uci.Move => m }.flatMap(p.move(_).toOption).map(_.after).getOrElse(p)
        }

        val lastFen = _root_.chess.format.Fen.write(lastPos).value
        val bestScore = variations.headOption.map(_.scoreCp).getOrElse(0)
        val nature =
          PositionCharacterizer.characterize(
            pos = lastPos,
            features = Nil,
            evalCp = Some(bestScore), // Best PV score
            material = lastFen.takeWhile(_ != ' ')
          )
        val _currentPhase = determinePhase(lastPos)
        // Convert VariationLine to PvLine
        val multiPv = variations.map { v =>
          PvLine(v.moves, v.scoreCp, v.mate, v.depth)
        }
        val classification = PositionClassifier.classify(
          features = features,
          multiPv = multiPv,
          currentEval = variations.headOption.map(_.scoreCp).getOrElse(0),
          tacticalMotifsCount = motifs.size
        )
        val ownThreats = ThreatAnalyzer.analyze(
           fen = fen,
           motifs = motifs,
           multiPv = multiPv,
           phase1 = classification,
           sideToMove = initialPos.color.name
        )
        val oppThreats = ThreatAnalyzer.analyze(
           fen = fen,
           motifs = motifs,
           multiPv = multiPv,
           phase1 = classification,
           sideToMove = (!initialPos.color).name
        )
        val pawnAnalysis = BreakAnalyzer.analyze(
          features = features,
          motifs = motifs,
          phase1 = classification,
          sideToMove = initialPos.color.name
        )
        
        val oppPawnAnalysis = BreakAnalyzer.analyze(
          features = features,
          motifs = motifs,
          phase1 = classification,
          sideToMove = (!initialPos.color).name
        )
        val evalCp = variations.headOption.map(_.scoreCp).getOrElse(0)
        val structureEvalStartNs = Option.when(llmConfig.shouldEvaluateStructureKb)(System.nanoTime())
        val structureProfile =
          Option.when(llmConfig.shouldEvaluateStructureKb) {
            PawnStructureClassifier.classify(
              features = features,
              board = initialPos.board,
              sideToMove = initialPos.color,
              minConfidence = llmConfig.structKbMinConfidence
            )
          }

        // Update counterThreatBetter assessments
        val (finalUs, finalThem) = (ownThreats, oppThreats) match {
          case (toThem, toUs) =>
            val ourMax = toThem.threats.map(_.lossIfIgnoredCp).maxOption.getOrElse(0)
            val theirMax = toUs.threats.map(_.lossIfIgnoredCp).maxOption.getOrElse(0)
            val counterBetter = ourMax > theirMax + 100 && theirMax < 500 // We have better counter, and their threat isn't overwhelming
            (toThem.copy(counterThreatBetter = counterBetter, defense = toThem.defense.copy(counterIsBetter = counterBetter)), 
             toUs.copy(counterThreatBetter = counterBetter, defense = toUs.defense.copy(counterIsBetter = counterBetter)))
        }

        val baseCtx = IntegratedContext(
          evalCp = evalCp,
          classification = Some(classification),
          pawnAnalysis = Some(pawnAnalysis),
          opponentPawnAnalysis = Some(oppPawnAnalysis),
          threatsToUs = Some(finalThem),
          threatsToThem = Some(finalUs),
          openingName = opening,
          isWhiteToMove = initialPos.color == White, // Corrected: use FEN side-to-move for probes
          positionKey = Some(fen),
          features = Some(features), // L1 Snapshot injection
          initialPos = Some(initialPos),
          structureProfile = structureProfile
        )
        
        val planScoring = PlanMatcher.matchPlans(motifs, baseCtx, initialPos.color)
        val planAlignment =
          structureProfile.flatMap { profile =>
            StructuralPlaybook.lookup(profile.primary).map { entry =>
              PlanAlignmentScorer.score(
                structureProfile = profile,
                playbookEntry = entry,
                topPlans = planScoring.topPlans,
                motifs = motifs,
                pawnAnalysis = Some(pawnAnalysis),
                sideToMove = initialPos.color
              )
            }.orElse(Some(PlanAlignmentScorer.unknown(planScoring.topPlans)))
          }
        val structureEvalLatencyMs = structureEvalStartNs.map { started =>
          ((System.nanoTime() - started) / 1000000L).max(0L)
        }
        val ctx = baseCtx.copy(planAlignment = planAlignment)
        val planFirstHypotheses =
          PlanProposalEngine.propose(
            fen = fen,
            ply = ply,
            ctx = ctx,
            maxItems = 5
          )
        val engineHypotheses =
          HypothesisGenerator.generate(
            planScoring = planScoring,
            ctx = ctx,
            maxItems = 5
          )
        val planHypotheses =
          PlanProposalEngine.mergePlanFirstWithEngine(
            planFirst = planFirstHypotheses,
            engineHypotheses = engineHypotheses,
            maxItems = 3
          )
        val salienceThemeShare =
          themeMaxShareFromHypotheses(
            (planFirstHypotheses ++ engineHypotheses)
              .distinctBy(h => s"${h.planId.trim.toLowerCase}|${h.subplanId.getOrElse("").trim.toLowerCase}")
          )
        val activePlans = PlanMatcher.toActivePlans(planScoring.topPlans, planScoring.compatibilityEvents)
        val currentSequence = TransitionAnalyzer.analyze(
          currentPlans = activePlans,
          continuityOpt = prevPlanContinuity,
          ctx = ctx
        )

        val currentContinuity = planScoring.topPlans.headOption.flatMap(tp => evolveContinuity(prevPlanContinuity, tp, ply))
        val baseData = BaseAnalysisData(
          nature = nature,
          motifs = motifs,
          plans = planScoring.topPlans
        )
        
        val salience = StrategicSalience.calculate(
          transitionType = currentSequence.transitionType,
          consecutivePlies = currentContinuity.map(_.consecutivePlies).getOrElse(1),
          evalDeltaCp = evalDeltaCp.getOrElse(0),
          themeMaxShare = salienceThemeShare
        )

        val meta = AnalysisMetadata(
          color = initialPos.color,
          ply = ply,
          prevMove = prevMove
        )
        // val currentPhaseName = _currentPhase.toString()

        semanticExtractor.extract(
          fen = fen,
          metadata = meta,
          baseData = baseData,
          vars = variations,
          playedMove = playedMove,
          probeResults = probeResults,
          prevEndgameState = prevEndgameState
        ).copy(
          tacticalThreatToUs = ctx.tacticalThreatToUs,
          tacticalThreatToThem = ctx.tacticalThreatToThem,
          evalCp = evalCp,
          isWhiteToMove = initialPos.color == White,
          phase = phase.getOrElse(_currentPhase.toString),
          structureProfile = structureProfile,
          structureEvalLatencyMs = structureEvalLatencyMs,
          planAlignment = planAlignment,
          planHypotheses = planHypotheses,
          strategicSalience = salience,
          integratedContext = Some(ctx)
        )
      }
    }

  def analyzeGame(
      pgn: String,
      evals: Map[Int, List[VariationLine]],
      extraPlies: Set[Int] = Set.empty
  ): List[ExtendedAnalysisData] = {
     lila.llm.PgnAnalysisHelper.extractPlyData(pgn) match {
       case scala.util.Right(plyDataList) =>
         val moveEvals = buildMoveEvals(plyDataList, evals)
         val keyPlies: Set[Int] = GameNarrativeOrchestrator.selectKeyMoments(moveEvals).map(_.ply).toSet ++ extraPlies
         analyzeSelectedPlies(plyDataList, evals, keyPlies)
       case scala.util.Left(_) => 
         Nil
     }
  }

  def generateGameArc(
      pgn: String,
      evals: Map[Int, List[VariationLine]],
      providedMetadata: Option[GameMetadata] = None,
      openingRefsByFen: Map[String, OpeningReference] = Map.empty,
      probeResultsByPly: Map[Int, List[ProbeResult]] = Map.empty,
      variantKey: String = EarlyOpeningNarrationPolicy.StandardVariant,
      @unused llmLevel: String = LlmLevel.Polish
  ): GameArc =
    generateGameArcDiagnostic(
      pgn = pgn,
      evals = evals,
      providedMetadata = providedMetadata,
      openingRefsByFen = openingRefsByFen,
      probeResultsByPly = probeResultsByPly,
      variantKey = variantKey
    ).arc

  private[llm] def generateGameArcDiagnostic(
      pgn: String,
      evals: Map[Int, List[VariationLine]],
      providedMetadata: Option[GameMetadata] = None,
      openingRefsByFen: Map[String, OpeningReference] = Map.empty,
      probeResultsByPly: Map[Int, List[ProbeResult]] = Map.empty,
      variantKey: String = EarlyOpeningNarrationPolicy.StandardVariant,
      diagnosticWitnesses: List[DiagnosticWitness] = Nil
  ): GameArcDiagnostic =
    val metadata = providedMetadata.getOrElse(extractMetadata(pgn))

    lila.llm.PgnAnalysisHelper.extractPlyData(pgn) match
      case scala.util.Right(plyDataList) =>
        val moveEvals = buildMoveEvals(plyDataList, evals)
        val moveEvalsByPly = moveEvals.map(ev => ev.ply -> ev).toMap
        val anchorMoments = selectAnchorMoments(moveEvals, plyDataList, openingRefsByFen)
        val anchorPlies = anchorMoments.map(_.ply).toSet

        val preliminaryData = analyzeSelectedPlies(plyDataList, evals, anchorPlies)
        val preliminaryDataByPly = preliminaryData.map(d => d.ply -> d).toMap
        val preliminaryBounds =
          buildPreviewTruthBounds(anchorMoments, preliminaryDataByPly, moveEvals, openingRefsByFen, variantKey)
        val preliminaryMoments =
          preliminaryBounds.map(bound => GameChronicleMoment.fromArcMoment(bound.moment))
        val rankedThreads = StrategicBranchSelector.rankThreads(preliminaryMoments).take(3)
        val candidatePlan =
          ActiveBridgeMomentPlanner.planCandidatePlies(
            rankedThreads = rankedThreads,
            anchorPlies = anchorPlies,
            totalPlies = plyDataList.lastOption.map(_.ply).getOrElse(0)
          )
        val candidateMoments =
          candidatePlan.values.flatten.toSet.toList.sorted.map(ply => bridgeCandidateMoment(ply, moveEvalsByPly))
        val candidateBridgePlies = candidateMoments.map(_.ply).toSet

        val (internalMoments, selectedBridgePlies) =
          if rankedThreads.isEmpty || candidateMoments.isEmpty then (anchorMoments, Set.empty[Int])
          else
            val enrichedData =
              analyzeSelectedPlies(
                plyDataList,
                evals,
                anchorPlies ++ candidateBridgePlies
              )
            val enrichedDataByPly = enrichedData.map(d => d.ply -> d).toMap
            val enrichedBounds =
              buildPreviewTruthBounds(
                (anchorMoments ++ candidateMoments).sortBy(_.ply).distinctBy(_.ply),
                enrichedDataByPly,
                moveEvals,
                openingRefsByFen,
                variantKey
              )
            val enrichedPreview =
              enrichedBounds.map(bound => GameChronicleMoment.fromArcMoment(bound.moment))
            val selectedBridges =
              ActiveBridgeMomentPlanner.selectBridges(
                rankedThreads = rankedThreads,
                enrichedMoments = enrichedPreview,
                anchorPlies = anchorPlies
              )
            val selectedBridgePlies = selectedBridges.map(_.ply).toSet
            val rescuedBridgePlies =
              selectCanonicalRescueBridgePlies(
                bounds = enrichedBounds,
                anchorPlies = anchorPlies,
                selectedBridgePlies = selectedBridgePlies
              )
            val rescuedBridgeMoments =
              rescuedBridgePlies.toList.sorted.flatMap { ply =>
                enrichedBounds.find(_.moment.ply == ply).map(bound => canonicalRescueBridgeMoment(bound, moveEvalsByPly))
              }
            val bridgeMoments =
              selectedBridges.map(plan => bridgeMomentFromPlan(plan, moveEvalsByPly)) ++ rescuedBridgeMoments
            if bridgeMoments.isEmpty then (anchorMoments, Set.empty[Int])
            else
              (
                mergeCanonicalInternalMoments(anchorMoments ++ bridgeMoments),
                selectedBridgePlies ++ rescuedBridgePlies
              )

        val finalPlies = internalMoments.map(_.ply).toSet
        val finalData =
          if finalPlies == anchorPlies then preliminaryData
          else analyzeSelectedPlies(plyDataList, evals, finalPlies)
        val dataByPly = finalData.map(d => d.ply -> d).toMap
        val internalMomentNarratives =
          buildMomentNarratives(
            internalMoments,
            dataByPly,
            moveEvals,
            openingRefsByFen,
            probeResultsByPly,
            variantKey
          )
        val truthContractsByPly =
          internalMomentNarratives.map(bound => bound.moment.ply -> bound.truthContract).toMap
        val projection =
          StrategicBranchSelector.buildSelection(
            internalMomentNarratives.map(bound => GameChronicleMoment.fromArcMoment(bound.moment)),
            truthContractsByPly
          )
        val visibleMomentPlies = projection.selectedMoments.map(_.ply).toSet
        val promotedWholeGamePly =
          selectWholeGamePromotionPly(
            internalMomentNarratives.map(_.moment),
            visibleMomentPlies,
            truthContractsByPly
          )
        val finalVisibleMomentPlies = visibleMomentPlies ++ promotedWholeGamePly.toSet
        val activeNotePlies = projection.activeNoteMoments.map(_.ply).toSet
        val momentNarratives =
          internalMomentNarratives
            .filter(bound => finalVisibleMomentPlies.contains(bound.moment.ply))
            .sortBy(_.moment.ply)
            .map { bound =>
              bound.moment.copy(
                strategicBranch = activeNotePlies.contains(bound.moment.ply),
                strategicThread = projection.threadRefsByPly.get(bound.moment.ply)
              )
            }

        val totalPlies = plyDataList.lastOption.map(_.ply).getOrElse(0)
        val blundersCount =
          momentNarratives.count(moment => wholeGameClassification(moment, truthContractsByPly) == "blunder")
        val missedWinsCount =
          momentNarratives.count(moment => wholeGameClassification(moment, truthContractsByPly) == "missedwin")

        val gameIntro = lila.llm.analysis.NarrativeLexicon.gameIntro(
          metadata.white, metadata.black, metadata.event, metadata.date, metadata.result,
          totalPlies = totalPlies,
          keyMomentsCount = momentNarratives.size
        )

        val visibleData = momentNarratives.flatMap(moment => dataByPly.get(moment.ply))
        val planThemes = visibleData.flatMap(_.plans.map(_.plan.name)).distinct
        val allThemes =
          if planThemes.nonEmpty then planThemes.take(3)
          else visibleData.flatMap(_.conceptSummary).distinct.take(3)
        val conclusionSupport =
          buildWholeGameConclusionSupport(
            moments = momentNarratives,
            strategicThreads = projection.threads,
            themes = allThemes,
            truthContractsByPly = truthContractsByPly.filter { case (ply, _) => finalVisibleMomentPlies.contains(ply) }
          )

        val conclusion = lila.llm.analysis.NarrativeLexicon.gameConclusion(
          winner = if (metadata.result.startsWith("1-0")) Some(metadata.white)
                   else if (metadata.result.startsWith("0-1")) Some(metadata.black)
                   else None,
          themes = allThemes,
          blunders = blundersCount,
          missedWins = missedWinsCount,
          mainContest = conclusionSupport.mainContest,
          decisiveShift = conclusionSupport.decisiveShift,
          payoff = conclusionSupport.payoff
        )

        val arc =
          GameArc(
            gameIntro = gameIntro,
            keyMomentNarratives = momentNarratives,
            conclusion = conclusion,
            overallThemes = allThemes,
            internalMomentCount = internalMomentNarratives.size,
            strategicThreads = projection.threads
          )

        val canonicalTraceMoments =
          buildTruthTraceMoments(
            bounds = internalMomentNarratives,
            anchorPlies = anchorPlies,
            candidateBridgePlies = candidateBridgePlies,
            selectedBridgePlies = selectedBridgePlies,
            finalVisibleMomentPlies = finalVisibleMomentPlies,
            activeNotePlies = activeNotePlies,
            promotedWholeGamePly = promotedWholeGamePly,
            threadRefsByPly = projection.threadRefsByPly,
            traceSource = "canonical_internal"
          )

        val witnessOnlyMoments =
          diagnosticWitnesses
            .sortBy(_.ply)
            .distinctBy(_.ply)
            .filterNot(witness => finalPlies.contains(witness.ply))
            .flatMap(witness => diagnosticWitnessMoment(witness, moveEvalsByPly))
        val witnessTraceMoments =
          if witnessOnlyMoments.isEmpty then Nil
          else
            val witnessTracePlies = finalPlies ++ witnessOnlyMoments.map(_.ply)
            val witnessData =
              if witnessTracePlies == finalPlies then finalData
              else analyzeSelectedPlies(plyDataList, evals, witnessTracePlies)
            val witnessDataByPly = witnessData.map(d => d.ply -> d).toMap
            val witnessBounds =
              buildMomentNarratives(
                (internalMoments ++ witnessOnlyMoments).sortBy(_.ply).distinctBy(_.ply),
                witnessDataByPly,
                moveEvals,
                openingRefsByFen,
                probeResultsByPly,
                variantKey
              ).filter(_.moment.selectionKind == "diagnostic_witness")
            buildTruthTraceMoments(
              bounds = witnessBounds,
              anchorPlies = anchorPlies,
              candidateBridgePlies = candidateBridgePlies,
              selectedBridgePlies = selectedBridgePlies,
              finalVisibleMomentPlies = Set.empty,
              activeNotePlies = Set.empty,
              promotedWholeGamePly = None,
              threadRefsByPly = projection.threadRefsByPly,
              traceSource = "diagnostic_witness"
            )

        GameArcDiagnostic(
          arc = arc,
          anchorPlies = anchorPlies.toList.sorted,
          candidateBridgePlies = candidateBridgePlies.toList.sorted,
          selectedBridgePlies = selectedBridgePlies.toList.sorted,
          finalInternalPlies = finalPlies.toList.sorted,
          visibleMomentPlies = finalVisibleMomentPlies.toList.sorted,
          activeNotePlies = activeNotePlies.toList.sorted,
          promotedWholeGamePly = promotedWholeGamePly,
          canonicalTraceMoments = canonicalTraceMoments,
          witnessTraceMoments = witnessTraceMoments
        )
      case scala.util.Left(err) =>
        GameArcDiagnostic(
          arc = failedGameArc(err),
          anchorPlies = Nil,
          candidateBridgePlies = Nil,
          selectedBridgePlies = Nil,
          finalInternalPlies = Nil,
          visibleMomentPlies = Nil,
          activeNotePlies = Nil,
          promotedWholeGamePly = None,
          canonicalTraceMoments = Nil,
          witnessTraceMoments = Nil
        )

  private def failedGameArc(err: Any): GameArc =
    GameArc(
      gameIntro = "Analysis Failed",
      keyMomentNarratives = Nil,
      conclusion = s"Could not parse game: $err",
      overallThemes = Nil,
      internalMomentCount = 0,
      strategicThreads = Nil
    )

  private def diagnosticWitnessMoment(
      witness: DiagnosticWitness,
      moveEvalsByPly: Map[Int, lila.llm.MoveEval]
  ): Option[KeyMoment] =
    val current = moveEvalsByPly.get(witness.ply)
    val previous = moveEvalsByPly.get(witness.ply - 1)
    current.map { currentEval =>
      KeyMoment(
        ply = witness.ply,
        momentType = witness.momentType.filter(_.trim.nonEmpty).getOrElse("TensionPeak"),
        score = currentEval.cp,
        description = witness.label.filter(_.trim.nonEmpty).getOrElse("Diagnostic witness"),
        cpBefore = previous.map(_.cp).getOrElse(currentEval.cp),
        cpAfter = currentEval.cp,
        mateBefore = previous.flatMap(_.mate),
        mateAfter = currentEval.mate,
        selectionKind = "diagnostic_witness",
        selectionLabel = Some("Diagnostic Witness"),
        selectionReason = witness.label.filter(_.trim.nonEmpty)
      )
    }

  private def buildTruthTraceMoments(
      bounds: List[TruthBoundArcMoment],
      anchorPlies: Set[Int],
      candidateBridgePlies: Set[Int],
      selectedBridgePlies: Set[Int],
      finalVisibleMomentPlies: Set[Int],
      activeNotePlies: Set[Int],
      promotedWholeGamePly: Option[Int],
      threadRefsByPly: Map[Int, lila.llm.ActiveStrategicThreadRef],
      traceSource: String
  ): List[TruthTraceMoment] =
    bounds.sortBy(_.moment.ply).map { bound =>
      val moment = bound.moment
      val frame = bound.truthFrame
      val contract = bound.truthContract
      val projection = DecisiveTruth.momentProjection(moment, Some(contract))
      val rawSurface = StrategyPackSurface.from(bound.rawStrategyPack)
      val sanitizedSurface = StrategyPackSurface.from(moment.strategyPack)
      val material = frame.materialEconomics
      val ownership = frame.strategicOwnership
      val failure = frame.failureInterpretation
      TruthTraceMoment(
        ply = moment.ply,
        momentType = moment.momentType,
        selectionKind = moment.selectionKind,
        selectionLabel = moment.selectionLabel,
        selectionReason = moment.selectionReason,
        traceSource = traceSource,
        anchorMoment = anchorPlies.contains(moment.ply),
        bridgeCandidate = candidateBridgePlies.contains(moment.ply),
        selectedBridge = selectedBridgePlies.contains(moment.ply),
        finalInternal = traceSource == "canonical_internal",
        visibleMoment = finalVisibleMomentPlies.contains(moment.ply),
        activeNoteMoment = activeNotePlies.contains(moment.ply),
        wholeGamePromoted = promotedWholeGamePly.contains(moment.ply),
        strategicThreadId = threadRefsByPly.get(moment.ply).map(_.threadId),
        moveClassification = moment.moveClassification,
        playedMove = frame.playedMove,
        verifiedBestMove = frame.verifiedBestMove,
        truthClass = frame.truthClass.toString,
        truthPhase = contract.truthPhase.map(_.toString),
        reasonFamily = contract.reasonFamily.toString,
        ownershipRole = projection.ownershipRole.toString,
        visibilityRole = projection.visibilityRole.toString,
        surfaceMode = projection.surfaceMode.toString,
        exemplarRole = projection.exemplarRole.toString,
        surfacedMoveOwnsTruth = projection.surfacedMoveOwnsTruth,
        compensationAllowed = contract.compensationAllowed,
        compensationProseAllowed = contract.compensationProseAllowed,
        benchmarkProseAllowed = projection.benchmarkProseAllowed,
        verifiedPayoffAnchor = projection.verifiedPayoffAnchor,
        chainKey = projection.chainKey,
        moveQualityVerdict = frame.moveQuality.verdict.toString,
        cpLoss = frame.moveQuality.cpLoss,
        swingSeverity = frame.moveQuality.swingSeverity,
        severityBand = frame.moveQuality.severityBand,
        investedMaterialCp = material.investedMaterialCp,
        beforeDeficit = material.beforeDeficit,
        afterDeficit = material.afterDeficit,
        deficitDelta = material.deficitDelta,
        movingPieceValue = material.movingPieceValue,
        capturedPieceValue = material.capturedPieceValue,
        sacrificeKind = material.sacrificeKind,
        valueDownCapture = material.valueDownCapture,
        increasesDeficit = material.increasesDeficit,
        recoversDeficit = material.recoversDeficit,
        overinvestment = material.overinvestment,
        uncompensatedLoss = material.uncompensatedLoss,
        forcedRecovery = material.forcedRecovery,
        createsFreshInvestment = ownership.createsFreshInvestment,
        maintainsInvestment = ownership.maintainsInvestment,
        convertsInvestment = ownership.convertsInvestment,
        durablePressure = ownership.durablePressure,
        currentMoveEvidence = ownership.currentMoveEvidence,
        currentConcreteCarrier = ownership.currentConcreteCarrier,
        freshCommitmentCandidate = ownership.freshCommitmentCandidate,
        ownerEligible = ownership.ownerEligible,
        legacyVisibleOnly = ownership.legacyVisibleOnly,
        maintenanceExemplarCandidate = ownership.maintenanceExemplarCandidate,
        evidenceProvenance = ownership.evidenceProvenance.toList.map(_.toString).sorted,
        failureMode = failure.failureMode.toString,
        failureIntentConfidence = failure.intentConfidence,
        failureIntentAnchor = failure.intentAnchor,
        failureInterpretationAllowed = failure.interpretationAllowed,
        rawDominantIdea = rawSurface.rawDominantIdeaText,
        rawSecondaryIdea = rawSurface.rawSecondaryIdeaText,
        rawExecution = rawSurface.rawExecutionText,
        rawObjective = rawSurface.rawObjectiveText,
        rawFocus = rawSurface.rawFocusText,
        rawLongTermFocus = bound.rawStrategyPack.toList.flatMap(_.longTermFocus.map(_.trim).filter(_.nonEmpty)).distinct,
        rawDirectionalTargets = bound.rawStrategyPack.toList.flatMap(_.directionalTargets.map(renderDirectionalTarget)).distinct,
        rawPieceRoutes = bound.rawStrategyPack.toList.flatMap(_.pieceRoutes.map(renderPieceRoute)).distinct,
        rawPieceMoveRefs = bound.rawStrategyPack.toList.flatMap(_.pieceMoveRefs.map(renderPieceMoveRef)).distinct,
        rawCompensationSummary = rawSurface.compensationSummary,
        rawCompensationVectors = rawSurface.compensationVectors,
        rawInvestedMaterial = rawSurface.investedMaterial,
        sanitizedDominantIdea = sanitizedSurface.dominantIdeaText,
        sanitizedSecondaryIdea = sanitizedSurface.secondaryIdeaText,
        sanitizedExecution = sanitizedSurface.executionText,
        sanitizedObjective = sanitizedSurface.objectiveText,
        sanitizedFocus = sanitizedSurface.focusText
      )
    }

  private def renderPieceRoute(route: lila.llm.StrategyPieceRoute): String =
    val path = (route.from :: route.route).filter(_.trim.nonEmpty).mkString("->")
    s"${route.ownerSide}:${route.piece}:$path:${route.purpose}"

  private def renderPieceMoveRef(moveRef: lila.llm.StrategyPieceMoveRef): String =
    s"${moveRef.ownerSide}:${moveRef.piece}:${moveRef.from}->${moveRef.target}:${moveRef.idea}"

  private def renderDirectionalTarget(target: lila.llm.StrategyDirectionalTarget): String =
    val reason =
      target.strategicReasons.map(_.trim).find(_.nonEmpty)
        .orElse(target.prerequisites.map(_.trim).find(_.nonEmpty))
        .getOrElse(target.readiness)
    s"${target.ownerSide}:${target.piece}:${target.from}->${target.targetSquare}:$reason"

  private def buildMoveEvals(
      plyDataList: List[lila.llm.PgnAnalysisHelper.PlyData],
      evals: Map[Int, List[VariationLine]]
  ): List[lila.llm.MoveEval] =
    plyDataList.map { p =>
      val vars = evals.getOrElse(p.ply, Nil)
      val bestV = vars.headOption
      lila.llm.MoveEval(
        ply = p.ply,
        cp = bestV.map(_.scoreCp).getOrElse(0),
        mate = bestV.flatMap(_.mate),
        variations = vars
      )
    }

  private def selectAnchorMoments(
      moveEvals: List[lila.llm.MoveEval],
      plyDataList: List[lila.llm.PgnAnalysisHelper.PlyData],
      openingRefsByFen: Map[String, OpeningReference]
  ): List[KeyMoment] =
    val keyMoments = GameNarrativeOrchestrator.selectKeyMoments(moveEvals)
    val keyPlies = keyMoments.map(_.ply).toSet
    val openingMoments = detectOpeningEventMoments(plyDataList, openingRefsByFen)
    (keyMoments ++ openingMoments.filterNot(m => keyPlies.contains(m.ply)))
      .sortBy(_.ply)
      .distinctBy(_.ply)

  private def analyzeSelectedPlies(
      plyDataList: List[lila.llm.PgnAnalysisHelper.PlyData],
      evals: Map[Int, List[VariationLine]],
      selectedPlies: Set[Int]
  ): List[ExtendedAnalysisData] =
    val (results, _, _) =
      plyDataList
        .filter(p => selectedPlies.contains(p.ply))
        .sortBy(_.ply)
        .foldLeft(
          (List.empty[ExtendedAnalysisData], PlanStateTracker.empty, Option.empty[lila.llm.model.strategic.EndgamePatternState])
        ) { case ((acc, planTracker, prevEgState), p) =>
            val playedUci = p.playedUci
            val vars = evals.getOrElse(p.ply, Nil)
            val ply = p.ply

            if vars.nonEmpty then
              val isWhiteTurn = p.fen.contains(" w ")
              val movingColor = if isWhiteTurn then _root_.chess.Color.White else _root_.chess.Color.Black

              val analysis = assessExtended(
                fen = p.fen,
                variations = vars,
                playedMove = Some(playedUci),
                opening = None,
                phase = None,
                ply = ply,
                prevMove = Some(playedUci),
                prevPlanContinuity = planTracker.getContinuity(movingColor),
                prevEndgameState = prevEgState
              )

              analysis match
                case Some(data) =>
                  val nextTracker = planTracker.update(
                    movingColor = movingColor,
                    ply = ply,
                    primaryPlan = data.plans.headOption,
                    secondaryPlan = data.plans.lift(1),
                    sequence = data.planSequence
                  )
                  val nextEgState =
                    lila.llm.model.strategic.EndgamePatternState.evolve(prevEgState, data.endgameFeatures, ply)
                  (acc :+ data.copy(planContinuity = nextTracker.getContinuity(movingColor)), nextTracker, nextEgState)
                case None =>
                  (acc, planTracker, prevEgState)
            else
              (acc, planTracker, prevEgState)
        }
    results

  private def buildPreviewTruthBounds(
      moments: List[KeyMoment],
      dataByPly: Map[Int, ExtendedAnalysisData],
      moveEvals: List[lila.llm.MoveEval],
      openingRefsByFen: Map[String, OpeningReference],
      variantKey: String
  ): List[TruthBoundArcMoment] =
    buildMomentNarratives(moments, dataByPly, moveEvals, openingRefsByFen, variantKey = variantKey)

  private[llm] def selectCanonicalRescueBridgePlies(
      bounds: List[TruthBoundArcMoment],
      anchorPlies: Set[Int],
      selectedBridgePlies: Set[Int]
  ): Set[Int] =
    bounds
      .filter(bound =>
        bound.moment.selectionKind == "thread_bridge" &&
          !anchorPlies.contains(bound.moment.ply) &&
          !selectedBridgePlies.contains(bound.moment.ply) &&
          canonicalRescueBridgeCandidate(bound.moment, bound.truthContract)
      )
      .map(_.moment.ply)
      .toSet

  private[llm] def canonicalRescueBridgeCandidate(
      moment: GameArcMoment,
      contract: DecisiveTruthContract
  ): Boolean =
    val cpSwing = math.abs(moment.cpAfter.getOrElse(0) - moment.cpBefore.getOrElse(0))
    val mateShift =
      (moment.mateBefore, moment.mateAfter) match
        case (None, Some(_))                                   => true
        case (Some(_), None)                                   => true
        case (Some(before), Some(after)) if math.abs(before - after) >= 3 => true
        case _                                                 => false
    val catastrophicBadMove =
      (contract.truthClass == DecisiveTruthClass.Blunder || contract.truthClass == DecisiveTruthClass.MissedWin) &&
        (contract.cpLoss >= 280 || cpSwing >= 280 || mateShift)
    val severeOnlyMoveFailure =
      contract.failureMode == FailureInterpretationMode.OnlyMoveFailure &&
        contract.cpLoss >= 200
    catastrophicBadMove || severeOnlyMoveFailure || mateShift

  private def canonicalRescueBridgeMoment(
      bound: TruthBoundArcMoment,
      moveEvalsByPly: Map[Int, lila.llm.MoveEval]
  ): KeyMoment =
    val base = bridgeCandidateMoment(bound.moment.ply, moveEvalsByPly)
    val decisiveMomentType =
      bound.truthContract.truthClass match
        case DecisiveTruthClass.Blunder   => "Blunder"
        case DecisiveTruthClass.MissedWin => "MissedWin"
        case _                            => base.momentType
    base.copy(
      momentType = decisiveMomentType,
      selectionReason = Some("Canonical bridge rescue")
    )

  private[llm] def mergeCanonicalInternalMoments(
      moments: List[KeyMoment]
  ): List[KeyMoment] =
    moments
      .groupBy(_.ply)
      .values
      .map(_.toList.sortBy(canonicalInternalMomentOrdering).head)
      .toList
      .sortBy(_.ply)

  private def canonicalInternalMomentOrdering(
      moment: KeyMoment
  ): (Int, Int, Int, Int) =
    val priority =
      moment.momentType match
        case "MissedWin"         => 0
        case "Blunder"           => 1
        case "MateLost"          => 2
        case "MateFound"         => 3
        case "MateShift"         => 4
        case "Equalization"      => 5
        case "SustainedPressure" => 6
        case "TensionPeak"       => 7
        case "StrategicBridge"   => 8
        case _                   => 9
    val selectionPenalty = if moment.selectionKind == "key" then 0 else 1
    val severity = -math.abs(moment.cpAfter - moment.cpBefore)
    (priority, selectionPenalty, severity, moment.ply)

  private def buildMomentNarratives(
      moments: List[KeyMoment],
      dataByPly: Map[Int, ExtendedAnalysisData],
      moveEvals: List[lila.llm.MoveEval],
      openingRefsByFen: Map[String, OpeningReference],
      probeResultsByPly: Map[Int, List[ProbeResult]] = Map.empty,
      variantKey: String
  ): List[TruthBoundArcMoment] =
    val draftedMoments =
      moments
        .sortBy(_.ply)
        .foldLeft(
          (
            List.empty[(TruthBoundArcMoment, Option[FullGameEvidenceSurfacePolicy.InternalProbeCandidate])],
            Option.empty[ExtendedAnalysisData],
            OpeningEventBudget(),
            Option.empty[OpeningReference],
            0
          )
        ) {
        case ((acc, prevAnalysis, budget, prevRef, evidenceMomentsUsed), moment) =>
          dataByPly.get(moment.ply) match
            case Some(data) =>
              val momentProbeResults = probeResultsByPly.getOrElse(moment.ply, Nil)
              val rawCtx = NarrativeContextBuilder.build(
                data,
                data.toContext,
                prevAnalysis,
                momentProbeResults,
                openingRefsByFen.get(data.fen),
                prevRef,
                budget,
                renderMode = NarrativeRenderMode.FullGame,
                variantKey = variantKey
              )
              val rawComparison = DecisionComparisonBuilder.build(rawCtx)
              val rawStrategyPack = StrategyPackBuilder.build(data, rawCtx)
              val truthFrame =
                DecisiveTruth.deriveFrame(
                  ctx = rawCtx,
                  momentType = Some(moment.momentType),
                  transitionType = data.planSequence.map(_.transitionType.toString),
                  cpBefore = Some(moment.cpBefore),
                  cpAfter = Some(moment.cpAfter),
                  strategyPack = rawStrategyPack,
                  comparisonOverride = rawComparison
                )
              val truthContract = truthFrame.toContract
              val ctx = DecisiveTruth.sanitizeContext(rawCtx, truthContract)
              val strategyPack =
                DecisiveTruth.sanitizeStrategyPack(
                  rawStrategyPack,
                  truthContract
                )
              val authoringSurface = AuthoringEvidenceSummaryBuilder.build(ctx)
              val preparedNarrative = buildHybridNarrativeParts(ctx, moment, Some(truthContract))
              val signalDigest =
                buildMomentSignalDigest(
                  ctx,
                  preparedNarrative.focusedOutline,
                  strategyPack,
                  authoringSurface.headline,
                  truthContract = Some(truthContract),
                  decisionComparisonOverride = rawComparison
                )
              val (fullText, focusedOutline) =
                renderHybridMomentNarrative(
                  ctx,
                  moment,
                  strategyPack = strategyPack,
                  signalDigest = signalDigest,
                  prepared = Some(preparedNarrative),
                  truthContract = Some(truthContract)
                )

              val classification = truthContract.moveClassificationLabel
              val narrativeEvent = truthContract.narrativeEvent(moment.momentType)

              val collapseData =
                if truthContract.isBad || narrativeEvent == "SustainedPressure" then
                  CausalCollapseAnalyzer.analyze(moment.ply, moveEvals, data)
                else None
              val evidenceEligible =
                evidenceMomentsUsed < FullGameEvidenceSurfacePolicy.MaxMoments &&
                  FullGameEvidenceSurfacePolicy.eligible(narrativeEvent, ctx, focusedOutline)
              val internalProbeCandidate =
                FullGameEvidenceSurfacePolicy.internalCandidate(
                  momentType = narrativeEvent,
                  selectionKind = moment.selectionKind,
                  strategicSalienceHigh = data.strategicSalience == StrategicSalience.High,
                  ctx = ctx,
                  outline = focusedOutline,
                  strategyPack = strategyPack
                )
              val surfacedEvidence =
                FullGameEvidenceSurfacePolicy.payload(
                  eligible = evidenceEligible,
                  probeRequests = ctx.probeRequests,
                  authorQuestions = authoringSurface.questions,
                  authorEvidence = authoringSurface.evidence
                )
              val surfacedPlans = ctx.mainStrategicPlans.take(3)

              val momentNarrative = GameArcMoment(
                ply = moment.ply,
                momentType = narrativeEvent,
                narrative = fullText,
                analysisData = data,
                selectionKind = moment.selectionKind,
                selectionLabel = moment.selectionLabel,
                selectionReason = moment.selectionReason,
                moveClassification = classification,
                cpBefore = Some(moment.cpBefore),
                cpAfter = Some(moment.cpAfter),
                mateBefore = moment.mateBefore,
                mateAfter = moment.mateAfter,
                wpaSwing = moment.wpaSwing,
                transitionType = data.planSequence.map(_.transitionType.toString),
                transitionConfidence = None,
                activePlan = data.planSequence.flatMap(seq =>
                  seq.primaryPlanName.map(name => lila.llm.ActivePlanRef(
                    themeL1 = name,
                    subplanId = seq.primaryPlanId,
                    phase = Some("Execution"),
                    commitmentScore = Some(seq.momentum)
                  ))
                ),
                topEngineMove =
                  buildTruthBoundTopEngineMove(
                    data = data,
                    comparison = rawComparison,
                    truthContract = truthContract,
                    playedCpAfter = moment.cpAfter
                  ),
                collapse = collapseData,
                strategyPack = strategyPack,
                signalDigest = signalDigest,
                probeRequests = surfacedEvidence.probeRequests,
                probeRefinementRequests = Nil,
                authorQuestions = surfacedEvidence.authorQuestions,
                authorEvidence = surfacedEvidence.authorEvidence,
                mainStrategicPlans = surfacedPlans,
                strategicPlanExperiments =
                  ctx.strategicPlanExperiments.filter(experiment =>
                    surfacedPlans.exists(plan =>
                      plan.planId.equalsIgnoreCase(experiment.planId) &&
                        plan.subplanId.getOrElse("").equalsIgnoreCase(experiment.subplanId.getOrElse(""))
                    )
                  ),
                latentPlans = ctx.latentPlans.take(2),
                whyAbsentFromTopMultiPV = ctx.whyAbsentFromTopMultiPV.take(3),
                truthPhase = truthContract.truthPhase.map(_.toString),
                surfacedMoveOwnsTruth = truthContract.surfacedMoveOwnsTruth,
                verifiedPayoffAnchor = truthContract.verifiedPayoffAnchor,
                compensationProseAllowed = truthContract.compensationProseAllowed,
                benchmarkProseAllowed = truthContract.benchmarkProseAllowed,
                investmentTruthChainKey = truthContract.investmentTruthChainKey
              )

              val nextRef = ctx.openingData.orElse(prevRef)
              val nextEvidenceCount =
                evidenceMomentsUsed + (if surfacedEvidence.nonEmpty then 1 else 0)
              val truthBoundMoment =
                TruthBoundArcMoment(
                  moment = momentNarrative,
                  truthFrame = truthFrame,
                  rawStrategyPack = rawStrategyPack,
                  truthContract = truthContract
                )
              (acc :+ (truthBoundMoment -> internalProbeCandidate), Some(data), ctx.updatedBudget, nextRef, nextEvidenceCount)
            case None =>
              (acc, prevAnalysis, budget, prevRef, evidenceMomentsUsed)
      }._1

    val internalProbePlies =
      FullGameEvidenceSurfacePolicy
        .selectInternalProbeMoments(draftedMoments.flatMap(_._2))
        .toSet

    draftedMoments.map { case (moment, candidateOpt) =>
      val probeRefinementRequests =
        candidateOpt.filter(candidate => internalProbePlies.contains(candidate.ply)).map(_.probeRequests).getOrElse(Nil)
      moment.copy(moment = moment.moment.copy(probeRefinementRequests = probeRefinementRequests))
    }

  private[llm] def selectWholeGamePromotionPly(
      internalMoments: List[GameArcMoment],
      visibleMomentPlies: Set[Int],
      truthContractsByPly: Map[Int, DecisiveTruthContract] = Map.empty
  ): Option[Int] =
    val visibleMoments = internalMoments.filter(moment => visibleMomentPlies.contains(moment.ply))
    val visibleHasDecisiveShift =
      visibleMoments.exists(moment => wholeGameMomentPriority(moment, truthContractsByPly) >= 90)
    if visibleHasDecisiveShift then None
    else
      internalMoments
        .filterNot(moment => visibleMomentPlies.contains(moment.ply))
        .map(moment => (moment, wholeGameMomentPriority(moment, truthContractsByPly), wholeGameMomentSwing(moment)))
        .filter { case (_, priority, _) => priority >= 90 }
        .sortBy { case (moment, priority, swing) => (-priority, -swing, moment.ply) }
        .headOption
        .map(_._1.ply)

  private[llm] def buildWholeGameConclusionSupport(
      moments: List[GameArcMoment],
      strategicThreads: List[lila.llm.ActiveStrategicThread],
      themes: List[String],
      truthContractsByPly: Map[Int, DecisiveTruthContract] = Map.empty
  ): WholeGameConclusionSupport =
    val decisiveMoment = selectWholeGameDecisiveMoment(moments, truthContractsByPly)
    val payoffMoment =
      decisiveMoment
        .filter(moment => wholeGameOwnsTruth(moment, truthContractsByPly) && isPunishOrConversionMoment(moment, truthContractsByPly))
        .orElse(
          moments
            .filter(moment => wholeGameOwnsTruth(moment, truthContractsByPly) && isPunishOrConversionMoment(moment, truthContractsByPly))
            .sortBy(moment => (-wholeGameMomentPriority(moment, truthContractsByPly), -wholeGameMomentSwing(moment), moment.ply))
            .headOption
        ).orElse(
          moments
            .filter(moment => isPunishOrConversionMoment(moment, truthContractsByPly))
            .sortBy(moment => (-wholeGameMomentPriority(moment, truthContractsByPly), -wholeGameMomentSwing(moment), moment.ply))
            .headOption
        )

    WholeGameConclusionSupport(
      mainContest = buildWholeGameMainContest(strategicThreads, themes),
      decisiveShift = decisiveMoment.flatMap(moment => buildWholeGameDecisiveShiftSentence(moment, truthContractsByPly)),
      payoff = payoffMoment.flatMap(moment => buildWholeGamePayoffSentence(moment, truthContractsByPly))
    )

  private def selectWholeGameDecisiveMoment(
      moments: List[GameArcMoment],
      truthContractsByPly: Map[Int, DecisiveTruthContract] = Map.empty
  ): Option[GameArcMoment] =
    moments
      .map(moment => (moment, wholeGameMomentPriority(moment, truthContractsByPly), wholeGameMomentSwing(moment)))
      .filter { case (_, priority, _) => priority >= 70 }
      .sortBy { case (moment, priority, swing) =>
        (if wholeGameOwnsTruth(moment, truthContractsByPly) then 0 else 1, -priority, -swing, moment.ply)
      }
      .headOption
      .map(_._1)

  private def buildWholeGameMainContest(
      strategicThreads: List[lila.llm.ActiveStrategicThread],
      themes: List[String]
  ): Option[String] =
    val threadBySide =
      strategicThreads
        .groupBy(thread => normalizedWholeGameText(thread.side).getOrElse(""))
        .view
        .mapValues(_.sortBy(thread => (-thread.continuityScore, thread.seedPly)).headOption)
        .toMap
    val whitePlan = threadBySide.get("white").flatten.flatMap(thread => wholeGameThreadPlanLabel(thread))
    val blackPlan = threadBySide.get("black").flatten.flatMap(thread => wholeGameThreadPlanLabel(thread))

    (whitePlan, blackPlan) match
      case (Some(white), Some(black)) if !sameWholeGameText(white, black) =>
        Some(s"White was mainly playing for $white, while Black was mainly playing for $black.")
      case (Some(white), _) =>
        Some(s"White's clearest long-term plan was $white.")
      case (_, Some(black)) =>
        Some(s"Black's clearest long-term plan was $black.")
      case _ =>
        themes.headOption
          .flatMap(text => cleanedWholeGameAnchor(text))
          .map(theme => s"The long strategic fight revolved around $theme.")

  private def buildWholeGameDecisiveShiftSentence(
      moment: GameArcMoment,
      truthContractsByPly: Map[Int, DecisiveTruthContract] = Map.empty
  ): Option[String] =
    extractWholeGameAnchor(moment, truthContractsByPly).flatMap { anchor =>
      if anchor.fullSentence then
        Option.when(wholeGameSentenceCarriesShift(anchor.text, moment, truthContractsByPly))(ensureWholeGameSentence(anchor.text))
          .orElse(
            extractWholeGameStructuredAnchor(moment, truthContractsByPly)
              .flatMap(text => renderWholeGameShiftSentence(text, moment, truthContractsByPly))
          )
      else
        renderWholeGameShiftSentence(anchor.text, moment, truthContractsByPly)
    }

  private def buildWholeGamePayoffSentence(
      moment: GameArcMoment,
      truthContractsByPly: Map[Int, DecisiveTruthContract] = Map.empty
  ): Option[String] =
    extractWholeGameAnchor(moment, truthContractsByPly).flatMap { anchor =>
      if anchor.fullSentence then
        Option.when(wholeGameSentenceCarriesPayoff(anchor.text, moment))(ensureWholeGameSentence(anchor.text))
          .orElse(
            extractWholeGameStructuredAnchor(moment, truthContractsByPly)
              .flatMap(text => renderWholeGamePayoffSentence(text, moment, truthContractsByPly))
          )
      else
        renderWholeGamePayoffSentence(anchor.text, moment, truthContractsByPly)
    }

  private def extractWholeGameAnchor(
      moment: GameArcMoment,
      truthContractsByPly: Map[Int, DecisiveTruthContract] = Map.empty
  ): Option[WholeGameAnchor] =
    extractWholeGameNarrativeSentence(moment)
      .orElse(
        extractWholeGameStructuredAnchor(moment, truthContractsByPly).map(text => WholeGameAnchor(text, fullSentence = false))
      )

  private def renderWholeGameShiftSentence(
      rawAnchorText: String,
      moment: GameArcMoment,
      truthContractsByPly: Map[Int, DecisiveTruthContract] = Map.empty
  ): Option[String] =
    val anchorText = rawAnchorText.trim.stripSuffix(".")
    Option.when(anchorText.nonEmpty) {
      val transition = normalizedWholeGameText(moment.transitionType.getOrElse("")).getOrElse("")
      val ownershipRole = wholeGameOwnershipRole(moment, truthContractsByPly)
      val surfaceMode = wholeGameSurfaceMode(moment, truthContractsByPly)
      val classification = wholeGameClassification(moment, truthContractsByPly)
      val shiftLead =
        if ownershipRole == TruthOwnershipRole.ConversionOwner || surfaceMode == TruthSurfaceMode.ConversionExplain then
          "The decisive shift came through"
        else if ownershipRole == TruthOwnershipRole.BlunderOwner then
          "The decisive shift came through"
        else if classification == "missedwin" then
          "The decisive shift came through"
        else if ownershipRole == TruthOwnershipRole.CommitmentOwner then "The decisive shift came through"
        else if normalizedWholeGameText(moment.momentType).exists(_.contains("sustainedpressure")) then
          "The long strategic pressure finally became concrete through"
        else
          "The turning point came through"
      s"$shiftLead $anchorText."
    }

  private def renderWholeGamePayoffSentence(
      rawAnchorText: String,
      moment: GameArcMoment,
      truthContractsByPly: Map[Int, DecisiveTruthContract] = Map.empty
  ): Option[String] =
    val anchorText = rawAnchorText.trim.stripSuffix(".")
    Option.when(anchorText.nonEmpty) {
      wholeGameClassification(moment, truthContractsByPly) match
        case "blunder" =>
          s"The punishment story ran through $anchorText."
        case "missedwin" =>
          s"The winning route was $anchorText."
        case _ =>
          "The conversion route ran through " + anchorText + "."
    }.filter { _ =>
      isPunishOrConversionMoment(moment, truthContractsByPly)
    }

  private def extractWholeGameStructuredAnchor(
      moment: GameArcMoment,
      truthContractsByPly: Map[Int, DecisiveTruthContract] = Map.empty
  ): Option[String] =
    wholeGameSupportTexts(moment, truthContractsByPly).flatMap(normalizeWholeGameSupportAnchor).headOption
      .orElse(extractWholeGameDirectionalTargetAnchor(moment, truthContractsByPly))
      .orElse(extractWholeGameMoveRefAnchor(moment, truthContractsByPly))
      .orElse(extractWholeGameRouteAnchor(moment, truthContractsByPly))
      .orElse(
        List(
          moment.analysisData.plans.headOption.map(_.plan.name),
          moment.activePlan.map(_.themeL1)
        ).flatten.flatMap(normalizeWholeGameSupportAnchor).headOption
      )

  private def extractWholeGameNarrativeSentence(
      moment: GameArcMoment
  ): Option[WholeGameAnchor] =
    splitNarrativeSentences(moment.narrative)
      .map(sentence => ensureWholeGameSentence(LiveNarrativeCompressionCore.rewritePlayerLanguage(sentence).trim))
      .filter(_.nonEmpty)
      .filter(sentence => LiveNarrativeCompressionCore.systemLanguageHits(sentence).isEmpty)
      .filterNot(sentence => LiveNarrativeCompressionCore.isLowValueNarrativeSentence(sentence))
      .filterNot(sentence => bodyAlreadyFramesMoment(sentence))
      .filterNot(sentence => wholeGameSentenceHasRoughAnchor(sentence))
      .filterNot(sentence => wholeGameNarrativeSentenceHasMetaLeakage(sentence))
      .find(sentence => wholeGameNarrativeSentenceEligible(sentence, moment))
      .map(sentence => WholeGameAnchor(sentence, fullSentence = true))

  private def wholeGameSupportTexts(
      moment: GameArcMoment,
      truthContractsByPly: Map[Int, DecisiveTruthContract] = Map.empty
  ): List[String] =
    List(
      truthContractsByPly.get(moment.ply).flatMap(_.verifiedPayoffAnchor).orElse(moment.verifiedPayoffAnchor),
      moment.signalDigest.flatMap(_.dominantIdeaFocus),
      moment.signalDigest.flatMap(_.strategicFlow),
      Option.when(wholeGameSurfaceMode(moment, truthContractsByPly) == TruthSurfaceMode.InvestmentExplain)(
        moment.signalDigest.flatMap(_.compensation)
      ).flatten,
      moment.signalDigest.flatMap(_.practicalVerdict),
      moment.strategyPack.flatMap(_.signalDigest.flatMap(_.dominantIdeaFocus)),
      moment.strategyPack.flatMap(_.signalDigest.flatMap(_.strategicFlow)),
      moment.strategyPack.flatMap(_.longTermFocus.headOption)
    ).flatten ++
      moment.strategyPack.toList.flatMap(_.directionalTargets.flatMap(_.strategicReasons))

  private def normalizeWholeGameSupportAnchor(
      raw: String
  ): Option[String] =
    cleanedWholeGameAnchor(raw).flatMap { text =>
      Option.when(
        !wholeGameAnchorLooksTooRough(text) &&
          !wholeGameSupportAnchorIsGenericFragment(text) &&
          (looksStrategicWholeGameAnchor(text) || LiveNarrativeCompressionCore.hasConcreteAnchor(text) || wholeGameAnchorHasStrategicNoun(text))
      )(text)
    }

  private def extractWholeGameDirectionalTargetAnchor(
      moment: GameArcMoment,
      truthContractsByPly: Map[Int, DecisiveTruthContract] = Map.empty
  ): Option[String] =
    val targets = moment.strategyPack.toList.flatMap(_.directionalTargets)
    val supportTexts = wholeGameSupportTexts(moment, truthContractsByPly)
    val targetSquares = targets.flatMap(target => normalizedWholeGameSquare(target.targetSquare))
    val theaters = targetSquares.flatMap(wholeGameTheaterFromSquare).distinct

    targets.flatMap(_.strategicReasons).flatMap(normalizeWholeGameSupportAnchor).headOption
      .orElse {
        if supportTexts.exists(wholeGameTextSuggestsFilePressure) then
          targetSquares.headOption.map(wholeGameFilePressurePhrase)
        else if supportTexts.exists(wholeGameTextSuggestsTargetPressure) && theaters.size == 1 && targetSquares.size >= 2 then
          Some(wholeGameTargetPhrase(theaters.head))
        else
          targetSquares.headOption.map(wholeGamePressurePhrase)
      }

  private def extractWholeGameMoveRefAnchor(
      moment: GameArcMoment,
      truthContractsByPly: Map[Int, DecisiveTruthContract] = Map.empty
  ): Option[String] =
    val moveRefs = moment.strategyPack.toList.flatMap(_.pieceMoveRefs)
    moveRefs.view.flatMap { moveRef =>
      val explicitSupport =
        (List(Some(moveRef.idea)).flatten ++ moveRef.evidence).flatMap(normalizeWholeGameSupportAnchor).headOption
      explicitSupport
        .orElse {
          val target = normalizedWholeGameSquare(moveRef.target)
          if wholeGameTextSuggestsFilePressure(moveRef.idea) || moveRef.evidence.exists(wholeGameTextSuggestsFilePressure) then
            target.map(wholeGameFilePressurePhrase)
          else if wholeGameTextSuggestsTargetPressure(moveRef.idea) || moveRef.evidence.exists(wholeGameTextSuggestsTargetPressure) then
            target.map(wholeGamePressurePhrase)
          else None
        }
        .toList
    }.headOption

  private def extractWholeGameRouteAnchor(
      moment: GameArcMoment,
      truthContractsByPly: Map[Int, DecisiveTruthContract] = Map.empty
  ): Option[String] =
    val routes = moment.strategyPack.toList.flatMap(_.pieceRoutes)
    routes.view.flatMap { route =>
      normalizeWholeGameSupportAnchor(route.purpose)
        .orElse {
          val target = route.route.lastOption.flatMap(normalizedWholeGameSquare)
          if wholeGameTextSuggestsFilePressure(route.purpose) then target.map(wholeGameFilePressurePhrase)
          else if wholeGameTextSuggestsTargetPressure(route.purpose) then target.map(wholeGamePressurePhrase)
          else None
        }
        .toList
    }.headOption

  private def wholeGameThreadPlanLabel(
      thread: lila.llm.ActiveStrategicThread
  ): Option[String] =
    val summary = Option(thread.summary).map(_.trim).filter(_.nonEmpty)
    val corePlan =
      summary.flatMap { text =>
        """(?i)\bcore plan:\s*([^.;]+)""".r.findFirstMatchIn(text).map(_.group(1).trim)
      }
    corePlan
      .orElse(Option(thread.themeLabel).map(_.trim).filter(_.nonEmpty))
      .flatMap(cleanedWholeGameAnchor)

  private def cleanedWholeGameAnchor(
      raw: String
  ): Option[String] =
    Option(raw)
      .map(LiveNarrativeCompressionCore.rewritePlayerLanguage)
      .map(_.replace("**", "").trim)
      .map(_.stripSuffix(".").trim)
      .filter(text => text.nonEmpty && LiveNarrativeCompressionCore.systemLanguageHits(text).isEmpty)

  private def normalizedWholeGameText(
      raw: String
  ): Option[String] =
    Option(raw).map(_.trim).filter(_.nonEmpty).map(_.toLowerCase)

  private def normalizedWholeGameSquare(
      raw: String
  ): Option[String] =
    normalizedWholeGameText(raw).filter(_.matches("^[a-h][1-8]$"))

  private def sameWholeGameText(a: String, b: String): Boolean =
    normalizeNarrativeFingerprint(a) == normalizeNarrativeFingerprint(b)

  private def looksStrategicWholeGameAnchor(
      text: String
  ): Boolean =
    val low = normalizeNarrativeFingerprint(text)
    List("pressure", "target", "break", "counterplay", "exchange", "file", "files", "control", "clamp", "pawn", "king", "outpost", "promotion", "race", "initiative").exists(low.contains)

  private def wholeGameAnchorHasStrategicNoun(
      text: String
  ): Boolean =
    val low = normalizeNarrativeFingerprint(text)
    List("pressure", "target", "targets", "file", "files", "control", "clamp", "break", "counterplay", "promotion", "race", "conversion", "initiative").exists(low.contains)

  private def wholeGameAnchorLooksTooRough(
      text: String
  ): Boolean =
    wholeGameSquareBundleOnly(text) || wholeGameBareTheaterOnly(text) || wholeGamePieceRouteOnly(text)

  private def wholeGameSquareBundleOnly(
      text: String
  ): Boolean =
    normalizeNarrativeFingerprint(text).matches("^[a-h][1-8](?:\\s+[a-h][1-8])*$")

  private def wholeGameBareTheaterOnly(
      text: String
  ): Boolean =
    Set("kingside", "the kingside", "queenside", "the queenside", "center", "the center").contains(
      normalizeNarrativeFingerprint(text)
    )

  private def wholeGamePieceRouteOnly(
      text: String
  ): Boolean =
    normalizeNarrativeFingerprint(text).matches("^(rook|queen|bishop|knight|king|pawn)\\s+(to|route toward)\\s+[a-h][1-8]$")

  private def wholeGameSentenceHasRoughAnchor(
      sentence: String
  ): Boolean =
    val low = Option(sentence).map(_.trim.toLowerCase.stripSuffix(".")).getOrElse("")
    low.matches(".*\\b(came through|ran through|winning route was|route ran through)\\s+[a-h][1-8](?:\\s*,\\s*[a-h][1-8])*$") ||
      low.matches(".*\\b(came through|ran through|winning route was|route ran through)\\s+(the\\s+)?(kingside|queenside|center)$") ||
      low.matches(".*\\b(came through|ran through|winning route was|route ran through)\\s+(rook|queen|bishop|knight|king|pawn)\\s+(to|route toward)\\s+[a-h][1-8]$")

  private def wholeGameNarrativeSentenceEligible(
      sentence: String,
      moment: GameArcMoment
  ): Boolean =
    wholeGameSentenceCarriesExplicitShift(sentence) || wholeGameSentenceCarriesPayoff(sentence, moment)

  private def wholeGameSentenceCarriesExplicitShift(
      sentence: String
  ): Boolean =
    val low = normalizeNarrativeFingerprint(sentence)
    low.contains("turning point") ||
      low.contains("decisive shift") ||
      (low.contains("turned into") && (
        low.contains("promotion") ||
          low.contains("conversion") ||
          low.contains("race") ||
          low.contains("endgame")
      )) ||
      low.contains("became concrete") ||
      low.contains("finally became concrete") ||
      low.contains("strategic clarity increases") ||
      low.contains("technical details matter more") ||
      low.contains("tactical dust settles")

  private def wholeGameSupportAnchorIsGenericFragment(
      text: String
  ): Boolean =
    val low = normalizeNarrativeFingerprint(text)
    low == "under pressure" ||
      low.startsWith("supports ") ||
      low.startsWith("continues ") ||
      low.startsWith("white continues ") ||
      low.startsWith("black continues ") ||
      low.startsWith("white keeps ") ||
      low.startsWith("black keeps ") ||
      low.startsWith("improving ") ||
      low.startsWith("preparing ") ||
      low.startsWith("using ") ||
      low.startsWith("attacking ") ||
      low.startsWith("development and ") ||
      low.startsWith("immediate ") ||
      low.startsWith("tactical ") ||
      low.startsWith("prophylaxis ") ||
      low.startsWith("simplifying ") ||
      low.startsWith("rook pawn march ")

  private def wholeGameNarrativeSentenceHasMetaLeakage(
      sentence: String
  ): Boolean =
    val low = normalizeNarrativeFingerprint(sentence)
    low.contains("sampled principal lines") ||
      low.contains("principal lines") ||
      low.contains("engine reference")

  private def wholeGameTextSuggestsFilePressure(
      raw: String
  ): Boolean =
    normalizedWholeGameText(raw).exists(text =>
      text.contains("file") || text.contains("line occupation") || text.contains("open-file occupation") || text.contains("open file")
    )

  private def wholeGameTextSuggestsTargetPressure(
      raw: String
  ): Boolean =
    normalizedWholeGameText(raw).exists(text =>
      text.contains("target") ||
        text.contains("fixed pawn") ||
        text.contains("weak pawn") ||
        text.contains("pressure on") ||
        text.contains("pressure against") ||
        text.contains("clamp") ||
        text.contains("bind")
    )

  private def wholeGameTheaterFromSquare(
      square: String
  ): Option[String] =
    normalizedWholeGameSquare(square).map(_.head).flatMap {
      case 'a' | 'b' | 'c' => Some("queenside")
      case 'd' | 'e'       => Some("center")
      case 'f' | 'g' | 'h' => Some("kingside")
      case _               => None
    }

  private def wholeGameFilePressurePhrase(
      square: String
  ): String =
    s"${square.head}-file control"

  private def wholeGamePressurePhrase(
      square: String
  ): String =
    s"pressure on $square"

  private def wholeGameTargetPhrase(
      theater: String
  ): String =
    theater match
      case "center" => "central targets"
      case other    => s"$other targets"

  private def wholeGameTruthContract(
      moment: GameArcMoment,
      truthContractsByPly: Map[Int, DecisiveTruthContract]
  ): Option[DecisiveTruthContract] =
    truthContractsByPly.get(moment.ply)

  private def wholeGameTruthProjection(
      moment: GameArcMoment,
      truthContractsByPly: Map[Int, DecisiveTruthContract]
  ): MomentTruthProjection =
    DecisiveTruth.momentProjection(moment, wholeGameTruthContract(moment, truthContractsByPly))

  private def chronicleTruthProjection(
      moment: GameChronicleMoment,
      truthContractsByPly: Map[Int, DecisiveTruthContract]
  ): MomentTruthProjection =
    DecisiveTruth.momentProjection(moment, truthContractsByPly.get(moment.ply))

  private def wholeGameClassification(
      moment: GameArcMoment,
      truthContractsByPly: Map[Int, DecisiveTruthContract]
  ): String =
    wholeGameTruthProjection(moment, truthContractsByPly).classificationKey

  private def wholeGameOwnershipRole(
      moment: GameArcMoment,
      truthContractsByPly: Map[Int, DecisiveTruthContract]
  ): TruthOwnershipRole =
    wholeGameTruthProjection(moment, truthContractsByPly).ownershipRole

  private def wholeGameVisibilityRole(
      moment: GameArcMoment,
      truthContractsByPly: Map[Int, DecisiveTruthContract]
  ): TruthVisibilityRole =
    wholeGameTruthProjection(moment, truthContractsByPly).visibilityRole

  private def wholeGameSurfaceMode(
      moment: GameArcMoment,
      truthContractsByPly: Map[Int, DecisiveTruthContract]
  ): TruthSurfaceMode =
    wholeGameTruthProjection(moment, truthContractsByPly).surfaceMode

  private def wholeGameOwnsTruth(
      moment: GameArcMoment,
      truthContractsByPly: Map[Int, DecisiveTruthContract]
  ): Boolean =
    wholeGameTruthProjection(moment, truthContractsByPly).surfacedMoveOwnsTruth

  private def wholeGameMomentPriority(
      moment: GameArcMoment,
      truthContractsByPly: Map[Int, DecisiveTruthContract] = Map.empty
  ): Int =
    MomentTruthSemantics
      .arc(moment, wholeGameTruthContract(moment, truthContractsByPly))
      .canonicalPrioritySeed

  private def wholeGameMomentSwing(
      moment: GameArcMoment
  ): Int =
    math.abs(moment.cpAfter.getOrElse(0) - moment.cpBefore.getOrElse(0))

  private def isPunishOrConversionMoment(
      moment: GameArcMoment,
      truthContractsByPly: Map[Int, DecisiveTruthContract] = Map.empty
  ): Boolean =
    MomentTruthSemantics
      .arc(moment, wholeGameTruthContract(moment, truthContractsByPly))
      .punishOrConversion

  private def ensureWholeGameSentence(
      text: String
  ): String =
    val trimmed = Option(text).getOrElse("").trim
    if trimmed.isEmpty then trimmed
    else if trimmed.endsWith(".") || trimmed.endsWith("!") || trimmed.endsWith("?") then trimmed
    else s"$trimmed."

  private def wholeGameSentenceCarriesShift(
      sentence: String,
      moment: GameArcMoment,
      truthContractsByPly: Map[Int, DecisiveTruthContract] = Map.empty
  ): Boolean =
    val low = normalizeNarrativeFingerprint(sentence)
    val classification = wholeGameClassification(moment, truthContractsByPly)
    val ownershipRole = wholeGameOwnershipRole(moment, truthContractsByPly)
    val transition = normalizedWholeGameText(moment.transitionType.getOrElse("")).getOrElse("")
    low.contains("turning point") ||
      low.contains("decisive shift") ||
      low.contains("promotion") ||
      low.contains("exchange") ||
      low.contains("initiative") ||
      low.contains("pressure") ||
      ownershipRole == TruthOwnershipRole.CommitmentOwner ||
      ownershipRole == TruthOwnershipRole.ConversionOwner ||
      ownershipRole == TruthOwnershipRole.BlunderOwner ||
      classification == "blunder" ||
      classification == "missedwin" ||
      transition.contains("promotion") ||
      transition.contains("exchange") ||
      transition.contains("convert") ||
      transition.contains("simplif")

  private def wholeGameSentenceCarriesPayoff(
      sentence: String,
      moment: GameArcMoment
  ): Boolean =
    val low = normalizeNarrativeFingerprint(sentence)
    low.contains("punish") ||
      low.contains("missed win") ||
      low.contains("winning route") ||
      low.contains("converted") ||
      low.contains("conversion")

  private def bridgeCandidateMoment(
      ply: Int,
      moveEvalsByPly: Map[Int, lila.llm.MoveEval]
  ): KeyMoment =
    val current = moveEvalsByPly.get(ply)
    val previous = moveEvalsByPly.get(ply - 1)
    KeyMoment(
      ply = ply,
      momentType = "StrategicBridge",
      score = current.map(_.cp).getOrElse(0),
      description = "Campaign bridge",
      cpBefore = previous.map(_.cp).getOrElse(current.map(_.cp).getOrElse(0)),
      cpAfter = current.map(_.cp).getOrElse(0),
      mateBefore = previous.flatMap(_.mate),
      mateAfter = current.flatMap(_.mate),
      selectionKind = "thread_bridge",
      selectionLabel = Some("Campaign Bridge")
    )

  private def bridgeMomentFromPlan(
      plan: ActiveBridgeMomentPlanner.PlannedBridge,
      moveEvalsByPly: Map[Int, lila.llm.MoveEval]
  ): KeyMoment =
    bridgeCandidateMoment(plan.ply, moveEvalsByPly).copy(
      selectionReason = Some(plan.reason)
    )

  private[llm] case class HybridNarrativeParts(
      lead: String,
      defaultBridge: String,
      criticalBranch: Option[String],
      body: String,
      primaryPlan: Option[String],
      focusedOutline: NarrativeOutline,
      phase: String,
      tacticalPressure: Boolean,
      cpWhite: Option[Int],
      bead: Int
  )

  private[llm] def buildHybridNarrativeParts(
      ctx: NarrativeContext,
      moment: KeyMoment,
      truthContract: Option[DecisiveTruthContract] = None
  ): HybridNarrativeParts = {
      val bead = Math.abs(ctx.hashCode) ^ (moment.ply * 0x9e3779b9)
      val phase = ctx.phase.current
      val collapsedEarlyOpening = EarlyOpeningNarrationPolicy.collapsedEarlyOpening(ctx, truthContract)
      val plan = topStrategicPlanName(ctx)
      val cpWhite = ctx.engineEvidence.flatMap(_.best).map(_.scoreCp)
      val rankedVars = rankEngineVariationsForFen(ctx.fen, ctx.engineEvidence.toList.flatMap(_.variations))
      val tacticalPressure = TacticalTensionPolicy.isTacticallyTense(ctx, truthContract)

      val lead = NarrativeLexicon.momentBlockLead(
        bead = bead ^ 0x11f1f1f1,
        phase = phase,
        momentType = moment.momentType,
        ply = moment.ply
      )
      val bridge = NarrativeLexicon.hybridBridge(
        bead = bead ^ 0x27d4eb2f,
        phase = phase,
        primaryPlan = plan,
        tacticalPressure = tacticalPressure,
        cpWhite = cpWhite,
        ply = ctx.ply
      )
      val criticalBranch =
        Option.when(!collapsedEarlyOpening)(buildCriticalBranchNarrative(ctx, rankedVars, bead, truthContract)).flatten
      val validatedOutline = BookStyleRenderer.validatedOutline(ctx, truthContract)
      val focusedOutline = focusMomentOutline(validatedOutline, criticalBranch.nonEmpty, Some(ctx))

      val bookBody =
        FullGameDraftNormalizer.normalize(BookStyleRenderer.renderValidatedOutline(validatedOutline, ctx, truthContract)).trim
      val focusedBody =
        FullGameDraftNormalizer.normalize(BookStyleRenderer.renderValidatedOutline(focusedOutline, ctx, truthContract)).trim
      val bodyBase =
        if focusedBody.nonEmpty then
          if collapsedEarlyOpening then focusMomentBody(focusedBody, keepParagraphs = 2)
          else focusedBody
        else
          focusMomentBody(
            bookBody,
            keepParagraphs =
              if collapsedEarlyOpening then 2
              else if criticalBranch.nonEmpty then 3
              else 4
          )
      HybridNarrativeParts(
        lead = lead,
        defaultBridge = bridge,
        criticalBranch = criticalBranch,
        body = bodyBase,
        primaryPlan = plan,
        focusedOutline = focusedOutline,
        phase = phase,
        tacticalPressure = tacticalPressure,
        cpWhite = cpWhite,
        bead = bead
      )
  }

  private[llm] def buildHybridNarrativeBridge(
      ctx: NarrativeContext,
      parts: HybridNarrativeParts,
      strategyPack: Option[lila.llm.StrategyPack] = None,
      @unused signalDigest: Option[NarrativeSignalDigest] = None,
      truthContract: Option[DecisiveTruthContract] = None
  ): String =
    StrategicThesisBuilder.build(ctx, strategyPack, truthContract)
      .map(_.claim.trim)
      .filter(_.nonEmpty)
      .filterNot(sentenceAlreadyInBody(_, parts.body))
      .orElse(outlineBridgeCandidate(parts.focusedOutline).filterNot(sentenceAlreadyInBody(_, parts.body)))
      .getOrElse(parts.defaultBridge)

  private[llm] def renderHybridMomentNarrative(
    ctx: NarrativeContext,
    moment: KeyMoment,
    strategyPack: Option[lila.llm.StrategyPack] = None,
    signalDigest: Option[NarrativeSignalDigest] = None,
    prepared: Option[HybridNarrativeParts] = None,
    truthContract: Option[DecisiveTruthContract] = None
  ): (String, NarrativeOutline) = {
    val parts = prepared.getOrElse(buildHybridNarrativeParts(ctx, moment, truthContract))
    StandardCommentaryClaimPolicy.noEventNote(ctx, truthContract) match
      case Some(note) => (note, parts.focusedOutline)
      case None =>
        val rendered = GameChronicleCompressionPolicy.render(ctx, parts).getOrElse("")
        (
          EarlyOpeningNarrationPolicy.clampNarrative(
            ctx,
            StandardCommentaryClaimPolicy.finalizeProse(
              ctx,
              FullGameDraftNormalizer.normalize(rendered).trim,
              truthContract
            ),
            truthContract
          ),
          parts.focusedOutline
        )
  }

  private[llm] def assembleHybridNarrativeDraft(
      lead: String,
      bridge: String,
      criticalBranch: Option[String],
      body: String,
      primaryPlan: Option[String],
      suppressPreface: Boolean = false
  ): String =
    val trimmedBody = trimHybridBodyRepetition(body, primaryPlan)
    val includeLead = !suppressPreface && trimmedBody.nonEmpty && !bodyAlreadyFramesMoment(trimmedBody)
    val includeBridge = !suppressPreface && trimmedBody.nonEmpty && !bodyAlreadyCarriesPrimaryThesis(trimmedBody, primaryPlan)
    val preface =
      List(
        Option.when(includeLead)(lead).getOrElse(""),
        Option.when(includeBridge)(bridge).getOrElse("")
      ).map(_.trim).filter(_.nonEmpty).mkString(" ")
    val parts = List(preface, criticalBranch.getOrElse(""), trimmedBody).map(_.trim).filter(_.nonEmpty)
    val renderedRaw =
      if parts.nonEmpty then parts.mkString("\n\n")
      else if trimmedBody.nonEmpty then trimmedBody
      else preface
    FullGameDraftNormalizer.normalize(renderedRaw)

  private[llm] def trimHybridBodyRepetition(
      body: String,
      primaryPlan: Option[String]
  ): String =
    val sentences = splitNarrativeSentences(body)
    val kept = scala.collection.mutable.ListBuffer.empty[String]
    val seenFingerprints = scala.collection.mutable.Set.empty[String]
    var keptPlanThesis = false

    sentences.foreach { sentence =>
      val trimmed = sentence.trim
      if trimmed.nonEmpty then
        val fingerprint = normalizeNarrativeFingerprint(trimmed)
        val thesisSentence = isPlanThesisSentence(trimmed, primaryPlan)
        val dropAsDuplicate = fingerprint.nonEmpty && seenFingerprints.contains(fingerprint)
        val dropAsMeta =
          isLowValueHybridMetaSentence(trimmed) ||
            (keptPlanThesis && isGenericPhaseRestatement(trimmed)) ||
            (keptPlanThesis && thesisSentence)
        if !dropAsDuplicate && !dropAsMeta then
          kept += trimmed
          if fingerprint.nonEmpty then seenFingerprints += fingerprint
          if thesisSentence then keptPlanThesis = true
    }

    val deduped = kept.mkString(" ").trim
    if deduped.nonEmpty then deduped else body.trim

  private def splitNarrativeSentences(text: String): List[String] =
    Option(text)
      .map(_.trim)
      .filter(_.nonEmpty)
      .map(_.split("(?<=[.!?])\\s+").toList)
      .getOrElse(Nil)

  private def normalizeNarrativeFingerprint(text: String): String =
    Option(text)
      .getOrElse("")
      .replace("**", "")
      .replaceAll("""[^\p{L}\p{N}\s]""", " ")
      .replaceAll("""\s+""", " ")
      .trim
      .toLowerCase

  private def bodyAlreadyFramesMoment(body: String): Boolean =
    val low = normalizeNarrativeFingerprint(body)
    low.startsWith("this opening block") ||
      low.startsWith("this middlegame block") ||
      low.startsWith("this endgame block") ||
      low.startsWith("around ply") ||
      low.startsWith("in the opening block") ||
      low.startsWith("in the middlegame block") ||
      low.startsWith("in the endgame block")

  private def bodyAlreadyCarriesPrimaryThesis(
      body: String,
      primaryPlan: Option[String]
  ): Boolean =
    val low = normalizeNarrativeFingerprint(body)
    val planMentioned =
      primaryPlan.exists { plan =>
        val normalizedPlan = normalizeNarrativeFingerprint(plan)
        normalizedPlan.nonEmpty && low.contains(normalizedPlan)
      }
    planMentioned && (
      low.contains("strategically this phase rewards a coherent plan around") ||
        low.contains("key theme") ||
        low.contains("the strategic stack still favors") ||
        low.contains("the opponent s main counterplan is")
    )

  private def isPlanThesisSentence(
      sentence: String,
      primaryPlan: Option[String]
  ): Boolean =
    val low = normalizeNarrativeFingerprint(sentence)
    val planMentioned =
      primaryPlan.exists { plan =>
        val normalizedPlan = normalizeNarrativeFingerprint(plan)
        normalizedPlan.nonEmpty && low.contains(normalizedPlan)
      }
    planMentioned && (
      low.contains("strategically this phase rewards a coherent plan around") ||
        low.contains("key theme") ||
        low.contains("the strategic stack still favors") ||
        low.contains("the leading route is") ||
        low.contains("the backup strategic stack is")
    )

  private def isGenericPhaseRestatement(sentence: String): Boolean =
    val low = normalizeNarrativeFingerprint(sentence)
    low.startsWith("this opening phase rewards") ||
      low.startsWith("this middlegame phase rewards") ||
      low.startsWith("this endgame phase rewards") ||
      low.startsWith("piece coordination and king safety both matter") ||
      low.startsWith("the middlegame has fully started") ||
      low.startsWith("the game enters a phase of technical consolidation")

  private def isLowValueHybridMetaSentence(sentence: String): Boolean =
    val low = normalizeNarrativeFingerprint(sentence)
    low.startsWith("the strategic stack still favors") ||
      low.startsWith("the leading route is") ||
      low.startsWith("the backup strategic stack is") ||
      low.startsWith("the main signals are") ||
      low.startsWith("evidence must show") ||
      low.startsWith("initial board read") ||
      low.startsWith("clearest read is that") ||
      low.startsWith("validation evidence specifically") ||
      low.startsWith("another key pillar is that") ||
      low.startsWith("in practical terms the split should appear")

  private def isStrategicDistributionBeat(beat: OutlineBeat): Boolean =
    beat.conceptIds.contains("strategic_distribution_first") ||
      beat.conceptIds.contains("plan_evidence_three_stage")

  private def isLowValueChronicleWrapUpBeat(beat: OutlineBeat): Boolean =
    beat.kind == OutlineBeatKind.WrapUp &&
      (
        isStrategicDistributionBeat(beat) ||
          isLowValueHybridMetaSentence(beat.text)
      )

  private[analysis] def focusMomentOutline(
    outline: NarrativeOutline,
    hasCriticalBranch: Boolean,
    ctxOpt: Option[NarrativeContext] = None
  ): NarrativeOutline =
    val maxBeats = if hasCriticalBranch then 5 else 6
    val candidates =
      outline.beats.zipWithIndex.filter { case (beat, _) =>
        beat.kind != OutlineBeatKind.MoveHeader
      }
    val filtered =
      candidates.filterNot { case (beat, _) =>
        isStrategicDistributionBeat(beat) ||
        (
          beat.kind == OutlineBeatKind.WrapUp &&
            (
              isLowValueChronicleWrapUpBeat(beat) ||
                (beat.focusPriority < 75 && isGenericWrapupParagraph(beat.text))
            )
        )
      }
    val supportKinds = Set(OutlineBeatKind.Evidence, OutlineBeatKind.Alternatives)
    val supportByKind =
      filtered
        .filter { case (beat, _) => supportKinds.contains(beat.kind) }
        .groupBy(_._1.kind)
        .view
        .mapValues(_.sortBy(_._2))
        .toMap
    val rankedBaseBeats =
      filtered.filterNot { case (beat, _) => supportKinds.contains(beat.kind) }
    val essentials = rankedBaseBeats.filter(_._1.fullGameEssential)
    val essentialSlots =
      essentials.sortBy { case (beat, idx) => (-beat.focusPriority, idx) }
    val selectedIndices = scala.collection.mutable.Set.empty[Int]
    def canKeepBeat(beat: OutlineBeat): Boolean =
      ctxOpt match
        case Some(ctx) => BranchScopedSentencePolicy.keepBeat(ctx, beat)
        case None =>
          !beat.branchScoped ||
            (BranchScopedSentencePolicy.hasUsableCitation(beat) && !LineScopedCitation.hasSourceLabelOnly(beat.text))
    def trySelect(entry: (OutlineBeat, Int)): Unit =
      val (beat, idx) = entry
      if !selectedIndices.contains(idx) && canKeepBeat(beat) then
        val requiredSupport =
          beat.supportKinds.flatMap(kind => supportByKind.getOrElse(kind, Nil).headOption)
        val supportIndices = requiredSupport.map(_._2).filterNot(selectedIndices.contains)
        val supportAvailable = requiredSupport.forall { case (supportBeat, _) => canKeepBeat(supportBeat) }
        val needed = 1 + supportIndices.size
        if supportAvailable && selectedIndices.size + needed <= maxBeats then
          selectedIndices += idx
          supportIndices.foreach(selectedIndices += _)
    essentialSlots.foreach(trySelect)
    rankedBaseBeats
      .filterNot { case (_, idx) => selectedIndices.contains(idx) }
      .sortBy { case (beat, idx) => (-beat.focusPriority, idx) }
      .foreach(trySelect)
    val selectedBeats =
      filtered
        .filter { case (_, idx) => selectedIndices.contains(idx) }
        .sortBy(_._2)
        .map(_._1)
    NarrativeOutline(selectedBeats, outline.diagnostics)

  private def topStrategicPlanName(ctx: NarrativeContext): Option[String] =
    StrategicNarrativePlanSupport.evidenceBackedLeadingPlanName(ctx)

  private def outlineBridgeCandidate(outline: NarrativeOutline): Option[String] =
    val preferredKinds = List(
      OutlineBeatKind.Context,
      OutlineBeatKind.DecisionPoint,
      OutlineBeatKind.MainMove,
      OutlineBeatKind.ConditionalPlan,
      OutlineBeatKind.OpeningTheory,
      OutlineBeatKind.TeachingPoint,
      OutlineBeatKind.WrapUp
    )
    preferredKinds.iterator
      .flatMap(kind => outline.beats.filter(beat => beat.kind == kind && !isStrategicDistributionBeat(beat)).iterator)
      .flatMap(beat => splitNarrativeSentences(beat.text).iterator)
      .map(_.trim)
      .find(sentence => sentence.nonEmpty && !isLowValueHybridMetaSentence(sentence) && !isGenericWrapupParagraph(sentence))

  private def sentenceAlreadyInBody(sentence: String, body: String): Boolean =
    val sentenceFingerprint = normalizeNarrativeFingerprint(sentence)
    val bodyFingerprint = normalizeNarrativeFingerprint(body)
    sentenceFingerprint.nonEmpty && bodyFingerprint.contains(sentenceFingerprint)

  private def rankEngineVariationsForFen(
    fen: String,
    vars: List[VariationLine]
  ): List[VariationLine] =
    if fenSideToMoveIsWhite(fen) then vars.sortBy(v => -v.effectiveScore)
    else vars.sortBy(_.effectiveScore)

  private def fenSideToMoveIsWhite(fen: String): Boolean =
    Option(fen).getOrElse("").trim.split("\\s+").drop(1).headOption.contains("w")

  private def cpLossForSideToMove(fen: String, bestScore: Int, playedScore: Int): Int =
    if fenSideToMoveIsWhite(fen) then (bestScore - playedScore).max(0)
    else (playedScore - bestScore).max(0)

  private def buildTruthBoundTopEngineMove(
      data: ExtendedAnalysisData,
      comparison: Option[DecisionComparison],
      truthContract: DecisiveTruthContract,
      playedCpAfter: Int
  ): Option[lila.llm.EngineAlternative] =
    val verifiedBest = truthContract.benchmarkMove.orElse(truthContract.verifiedBestMove)
    val matchedAlt =
      verifiedBest.flatMap(bestMove =>
        data.alternatives.find(alt =>
          alt.moves.headOption.exists(move => DecisiveTruth.sameMoveToken(move, bestMove)) ||
            alt.ourMove.map(_.san).exists(move => DecisiveTruth.sameMoveToken(move, bestMove))
        )
      ).orElse(data.alternatives.headOption.filter(_ => truthContract.allowConcreteBenchmark))
    verifiedBest.flatMap { bestMove =>
      matchedAlt.map { alt =>
        lila.llm.EngineAlternative(
          uci = alt.moves.headOption.getOrElse(bestMove),
          san = Some(bestMove),
          cpAfterAlt = Some(alt.scoreCp),
          cpLossVsPlayed = Option.when(truthContract.cpLoss > 0)(truthContract.cpLoss)
            .orElse(Some(cpLossForSideToMove(data.fen, alt.scoreCp, playedCpAfter))),
          pv = alt.moves
        )
      }.orElse {
        Option.when(truthContract.allowConcreteBenchmark || truthContract.chosenMatchesBest) {
          lila.llm.EngineAlternative(
            uci = comparison.flatMap(_.engineBestPv.headOption).getOrElse(bestMove),
            san = Some(bestMove),
            cpAfterAlt = comparison.flatMap(_.engineBestScoreCp),
            cpLossVsPlayed = Option.when(truthContract.cpLoss > 0)(truthContract.cpLoss),
            pv = comparison.map(_.engineBestPv).getOrElse(Nil)
          )
        }
      }
    }

  private def variationFirstMoveSan(fen: String, v: VariationLine): Option[String] =
    v.ourMove.map(_.san).map(_.trim).filter(_.nonEmpty).orElse {
      v.moves.headOption
        .map(m => NarrativeUtils.uciToSanOrFormat(fen, m).trim)
        .filter(_.nonEmpty)
    }

  private def variationMatchesPlayed(ctx: NarrativeContext, v: VariationLine): Boolean =
    val playedUci = ctx.playedMove.map(NarrativeUtils.normalizeUciMove).filter(_.nonEmpty)
    val playedSan = ctx.playedSan.map(_.trim.toLowerCase).filter(_.nonEmpty)
    val byUci =
      playedUci.exists(u => v.moves.headOption.exists(m => NarrativeUtils.uciEquivalent(m, u)))
    val bySan =
      playedSan.exists { san =>
        variationFirstMoveSan(ctx.fen, v).exists(_.trim.toLowerCase == san)
      }
    byUci || bySan

  private def variationLinePreview(fen: String, v: VariationLine): Option[String] =
    if v.parsedMoves.nonEmpty then
      Option.when(v.parsedMoves.nonEmpty)(v.parsedMoves.take(4).map(_.san).mkString(" ").trim).filter(_.nonEmpty)
    else
      val tokens = v.moves.take(4).zipWithIndex.map { case (m, idx) =>
        if idx == 0 then NarrativeUtils.uciToSanOrFormat(fen, m)
        else NarrativeUtils.formatUciAsSan(m)
      }
      Option.when(tokens.nonEmpty)(tokens.mkString(" ").trim).filter(_.nonEmpty)

  private def buildCriticalBranchNarrative(
    ctx: NarrativeContext,
    rankedVars: List[VariationLine],
    bead: Int,
    truthContract: Option[DecisiveTruthContract] = None
  ): Option[String] =
    if rankedVars.size < 2 then None
    else
      val contract =
        truthContract.getOrElse(
          DecisiveTruth.derive(ctx, comparisonOverride = DecisionComparisonBuilder.build(ctx))
        )
      if contract.chosenMatchesBest || !contract.allowConcreteBenchmark then None
      else
        val best =
          contract.verifiedBestMove.flatMap(bestMove =>
            rankedVars.find(variation =>
              variationFirstMoveSan(ctx.fen, variation).exists(move => DecisiveTruth.sameMoveToken(move, bestMove)) ||
                variation.moves.headOption.exists(move => DecisiveTruth.sameMoveToken(move, bestMove))
            )
          ).orElse(rankedVars.headOption)
        val playedBranch = rankedVars.find(v => variationMatchesPlayed(ctx, v))
        best.flatMap { bestVar =>
          playedBranch.flatMap { alt =>
            val bestMove = contract.benchmarkMove.orElse(contract.verifiedBestMove).getOrElse("the engine first move")
            val altMove = variationFirstMoveSan(ctx.fen, alt).getOrElse("the played move")
            val cpLoss = contract.cpLoss
            val gapPawns = f"${cpLoss.toDouble / 100}%.1f"
            val bestCitation = LineScopedCitation.strategicCitation(ctx.fen, ctx.ply + 1, bestVar)
            val altCitation = LineScopedCitation.strategicCitation(ctx.fen, ctx.ply + 1, alt)
            for
              bestLine <- bestCitation
              altLine <- altCitation
              bestSentence <- LineScopedCitation.afterClause(
                bestLine,
                if cpLoss <= 20 then
                  s"**$bestMove** stays only slightly ahead of the played **$altMove**"
                else
                  s"**$bestMove** keeps about a $gapPawns-pawn edge over the played **$altMove**"
              )
              altSentence <- LineScopedCitation.afterClause(
                altLine,
                "the played branch gives up the cleaner continuation"
              )
            yield s"$bestSentence $altSentence"
          }
        }

  private def focusMomentBody(body: String, keepParagraphs: Int): String =
    val paras = Option(body).getOrElse("")
      .split("""\n\s*\n""")
      .map(_.trim)
      .filter(_.nonEmpty)
      .toList
    val filtered = paras.filterNot(isGenericWrapupParagraph)
    filtered.take(keepParagraphs.max(1)).mkString("\n\n")

  private def buildMomentSignalDigest(
    ctx: NarrativeContext,
    focusedOutline: NarrativeOutline,
    strategyPack: Option[lila.llm.StrategyPack],
    authoringEvidence: Option[String],
    truthContract: Option[DecisiveTruthContract] = None,
    decisionComparisonOverride: Option[DecisionComparison] = None
  ): Option[NarrativeSignalDigest] =
    val sanitizedComparison =
      truthContract
        .flatMap(contract => DecisiveTruth.sanitizeDecisionComparison(decisionComparisonOverride, contract).map(_.toDigest))
        .orElse(decisionComparisonOverride.map(_.toDigest))
    val base =
      NarrativeSignalDigestBuilder.buildWithAuthoringEvidence(
        ctx,
        preservedSignalsOverride = Some(NarrativeSignalDigestBuilder.preservedSignalsFromOutline(ctx, focusedOutline)),
        authoringEvidence = authoringEvidence,
        decisionComparisonOverride = sanitizedComparison,
        allowCompensationSignals = truthContract.forall(_.compensationProseAllowed)
      )
    (base, strategyPack.flatMap(_.signalDigest)) match
      case (Some(digest), Some(packDigest)) =>
        Some(
          digest.copy(
            dominantIdeaKind = packDigest.dominantIdeaKind,
            dominantIdeaGroup = packDigest.dominantIdeaGroup,
            dominantIdeaReadiness = packDigest.dominantIdeaReadiness,
            dominantIdeaFocus = packDigest.dominantIdeaFocus,
            secondaryIdeaKind = packDigest.secondaryIdeaKind,
            secondaryIdeaGroup = packDigest.secondaryIdeaGroup,
            secondaryIdeaFocus = packDigest.secondaryIdeaFocus
          )
        )
      case (None, Some(packDigest)) if packDigest.dominantIdeaKind.isDefined =>
        Some(
          NarrativeSignalDigest(
            dominantIdeaKind = packDigest.dominantIdeaKind,
            dominantIdeaGroup = packDigest.dominantIdeaGroup,
            dominantIdeaReadiness = packDigest.dominantIdeaReadiness,
            dominantIdeaFocus = packDigest.dominantIdeaFocus,
            secondaryIdeaKind = packDigest.secondaryIdeaKind,
            secondaryIdeaGroup = packDigest.secondaryIdeaGroup,
            secondaryIdeaFocus = packDigest.secondaryIdeaFocus
          )
        )
      case _ =>
        base

  private def isGenericWrapupParagraph(p: String): Boolean =
    val low = Option(p).getOrElse("").trim.toLowerCase
    low.nonEmpty && List(
      "it is easy to misstep",
      "the position stays tense",
      "one careless tempo",
      "practical terms",
      "the practical chances are still shared",
      "the position remains dynamically balanced",
      "defensive precision is more important than active-looking moves",
      "the game remains balanced, and precision will decide the result"
    ).exists(low.contains)

  private def branchReasonClause(ctx: NarrativeContext, bestMove: String): Option[String] =
    val urgentThreat =
      ctx.threats.toUs
        .sortBy(t => -t.lossIfIgnoredCp)
        .headOption
        .filter(t => t.lossIfIgnoredCp >= 80 || t.kind.toLowerCase.contains("mate"))

    urgentThreat.map { t =>
      val kind = t.kind.toLowerCase
      val square = t.square.map(_.trim).filter(_.nonEmpty).map(s => s" on $s").getOrElse("")
      NarrativeLexicon.pick(Math.abs((ctx.fen + bestMove + kind).hashCode), List(
        s"The cleaner branch also addresses the $kind threat$square more directly.",
        s"Strategically, **$bestMove** limits the $kind pressure$square before it grows.",
        s"This branch is preferable because it meets the $kind idea$square with less concession."
      ))
    }.orElse {
      ctx.facts.iterator.flatMap(fact => StandardCommentaryClaimPolicy.branchReasonFromFact(ctx, fact)).take(1).toList.headOption
    }.orElse {
      ctx.pawnPlay.breakFile.map { f =>
        val file = f.trim
        val fileLabel = if file.toLowerCase.contains("file") then file else s"$file-file"
        s"It also preserves better control of the $fileLabel break."
      }
    }

  private def detectOpeningEventMoments(
      plyDataList: List[lila.llm.PgnAnalysisHelper.PlyData],
      openingRefsByFen: Map[String, OpeningReference]
  ): List[KeyMoment] = {
    val maxScanPly = 20
    val moments = scala.collection.mutable.ListBuffer.empty[KeyMoment]
    var budget = OpeningEventBudget()
    var prevRef: Option[OpeningReference] = None

    val it = plyDataList.iterator
    while (it.hasNext && !budget.theoryEnded) {
      val plyData = it.next()
      if (plyData.ply > maxScanPly) {
        return moments.toList
      }

      val refOpt = openingRefsByFen.get(plyData.fen)

      val eventOpt = OpeningEventDetector.detect(
        ply = plyData.ply,
        playedMove = Some(plyData.playedUci),
        fen = plyData.fen,
        ref = refOpt,
        budget = budget,
        cpLoss = None,
        hasConstructiveEvidence = false,
        prevRef = prevRef
      )

      eventOpt.foreach { ev =>
        val momentType = ev match {
          case OpeningEvent.Intro(_, _, _, _) => "OpeningIntro"
          case OpeningEvent.BranchPoint(_, _, _) => "OpeningBranchPoint"
          case OpeningEvent.OutOfBook(_, _, _) => "OpeningOutOfBook"
          case OpeningEvent.TheoryEnds(_, _) => "OpeningTheoryEnds"
          case OpeningEvent.Novelty(_, _, _, _) => "OpeningNovelty"
        }

        moments += KeyMoment(
          ply = plyData.ply,
          momentType = momentType,
          score = 0,
          description = "Opening reference",
          selectionKind = "opening",
          selectionLabel = Some("Opening Event")
        )

        budget = ev match {
          case OpeningEvent.Intro(_, _, _, _) => budget.afterIntro
          case OpeningEvent.TheoryEnds(_, _) => budget.afterTheoryEnds
          case _ => budget.afterEvent
        }
      }

      if (eventOpt.isEmpty) {
        budget = budget.updatePly(plyData.ply)
      }

      // Preserve last known ref so BranchPoint/TheoryEnds can trigger even if a later fetch is missing.
      prevRef = refOpt.orElse(prevRef)
    }

    moments.toList
  }

  private def extractMetadata(pgn: String): GameMetadata = {
    import chess.format.pgn.{ Parser, PgnStr }
    Parser.full(PgnStr(pgn)).toOption.map { parsed =>
      val tags = parsed.tags.value
      def get(name: String) = tags.find(_.name.name == name).map(_.value).getOrElse("Unknown")
      
      GameMetadata(
        white = get("White"),
        black = get("Black"),
        event = get("Event"),
        date = get("Date"),
        result = get("Result") match {
           case "Unknown" => "*" 
           case r => r
        }
      )
    }.getOrElse(GameMetadata("Unknown", "Unknown", "Unknown", "Unknown", "*"))
  }
