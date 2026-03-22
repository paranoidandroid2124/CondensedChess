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
  ): GameArc = {
     
     val metadata = providedMetadata.getOrElse(extractMetadata(pgn))
     
     lila.llm.PgnAnalysisHelper.extractPlyData(pgn) match {
        case scala.util.Right(plyDataList) =>
           val moveEvals = buildMoveEvals(plyDataList, evals)
           val moveEvalsByPly = moveEvals.map(ev => ev.ply -> ev).toMap
           val anchorMoments = selectAnchorMoments(moveEvals, plyDataList, openingRefsByFen)
           val anchorPlies = anchorMoments.map(_.ply).toSet

           val preliminaryData = analyzeSelectedPlies(plyDataList, evals, anchorPlies)
           val preliminaryDataByPly = preliminaryData.map(d => d.ply -> d).toMap
           val preliminaryMoments =
             buildPreviewResponseMoments(anchorMoments, preliminaryDataByPly, moveEvals, openingRefsByFen, variantKey)
           val rankedThreads = StrategicBranchSelector.rankThreads(preliminaryMoments).take(3)
           val candidatePlan =
             ActiveBridgeMomentPlanner.planCandidatePlies(
               rankedThreads = rankedThreads,
               anchorPlies = anchorPlies,
               totalPlies = plyDataList.lastOption.map(_.ply).getOrElse(0)
             )
           val candidateMoments =
             candidatePlan.values.flatten.toSet.toList.sorted.map(ply => bridgeCandidateMoment(ply, moveEvalsByPly))

           val internalMoments =
             if rankedThreads.isEmpty || candidateMoments.isEmpty then anchorMoments
             else
               val enrichedData =
                 analyzeSelectedPlies(
                   plyDataList,
                   evals,
                   anchorPlies ++ candidateMoments.map(_.ply)
                 )
               val enrichedDataByPly = enrichedData.map(d => d.ply -> d).toMap
               val enrichedPreview =
                 buildPreviewResponseMoments(
                   (anchorMoments ++ candidateMoments).sortBy(_.ply).distinctBy(_.ply),
                   enrichedDataByPly,
                   moveEvals,
                   openingRefsByFen,
                   variantKey
                 )
               val selectedBridges =
                 ActiveBridgeMomentPlanner.selectBridges(
                   rankedThreads = rankedThreads,
                   enrichedMoments = enrichedPreview,
                   anchorPlies = anchorPlies
                 )
               if selectedBridges.isEmpty then anchorMoments
               else
                 (anchorMoments ++ selectedBridges.map(plan => bridgeMomentFromPlan(plan, moveEvalsByPly)))
                   .sortBy(_.ply)
                   .distinctBy(_.ply)

           val finalPlies = internalMoments.map(_.ply).toSet
           val finalData =
             if finalPlies == anchorPlies then preliminaryData
             else analyzeSelectedPlies(plyDataList, evals, finalPlies)
           val dataByPly = finalData.map(d => d.ply -> d).toMap
           val internalMomentNarratives =
             buildMomentNarratives(internalMoments, dataByPly, moveEvals, openingRefsByFen, probeResultsByPly, variantKey)
           val projection = StrategicBranchSelector.buildSelection(internalMomentNarratives.map(GameChronicleMoment.fromArcMoment))
           val visibleMomentPlies = projection.selectedMoments.map(_.ply).toSet
           val activeNotePlies = projection.activeNoteMoments.map(_.ply).toSet
           val momentNarratives =
             internalMomentNarratives
               .filter(moment => visibleMomentPlies.contains(moment.ply))
               .sortBy(_.ply)
               .map { moment =>
                 moment.copy(
                   strategicBranch = activeNotePlies.contains(moment.ply),
                   strategicThread = projection.threadRefsByPly.get(moment.ply)
                 )
               }

           val totalPlies = plyDataList.lastOption.map(_.ply).getOrElse(0)
           val blundersCount =
             momentNarratives.count(moment =>
               moment.momentType == "AdvantageSwing" && moment.moveClassification.contains("Blunder")
             )
           val missedWinsCount =
             momentNarratives.count(moment =>
               moment.momentType == "AdvantageSwing" && moment.moveClassification.contains("MissedWin")
             )

           val gameIntro = lila.llm.analysis.NarrativeLexicon.gameIntro(
             metadata.white, metadata.black, metadata.event, metadata.date, metadata.result,
             totalPlies = totalPlies,
             keyMomentsCount = momentNarratives.size
           )

           val visibleData = momentNarratives.flatMap(moment => dataByPly.get(moment.ply))
           val planThemes = visibleData.flatMap(_.plans.map(_.plan.name)).distinct
           val allThemes = if (planThemes.nonEmpty) planThemes.take(3)
                           else visibleData.flatMap(_.conceptSummary).distinct.take(3)

           val conclusion = lila.llm.analysis.NarrativeLexicon.gameConclusion(
             winner = if (metadata.result.startsWith("1-0")) Some(metadata.white)
                      else if (metadata.result.startsWith("0-1")) Some(metadata.black)
                      else None,
             themes = allThemes,
             blunders = blundersCount,
             missedWins = missedWinsCount
           )

           GameArc(
             gameIntro = gameIntro,
             keyMomentNarratives = momentNarratives,
             conclusion = conclusion,
             overallThemes = allThemes,
             internalMomentCount = internalMomentNarratives.size,
             strategicThreads = projection.threads
           )
       case scala.util.Left(err) =>
          GameArc(
            gameIntro = "Analysis Failed",
            keyMomentNarratives = Nil,
            conclusion = s"Could not parse game: $err",
            overallThemes = Nil,
            internalMomentCount = 0,
            strategicThreads = Nil
         )
      }
  }

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

  private def buildPreviewResponseMoments(
      moments: List[KeyMoment],
      dataByPly: Map[Int, ExtendedAnalysisData],
      moveEvals: List[lila.llm.MoveEval],
      openingRefsByFen: Map[String, OpeningReference],
      variantKey: String
  ): List[GameChronicleMoment] =
    buildMomentNarratives(moments, dataByPly, moveEvals, openingRefsByFen, variantKey = variantKey)
      .map(GameChronicleMoment.fromArcMoment)

  private def buildMomentNarratives(
      moments: List[KeyMoment],
      dataByPly: Map[Int, ExtendedAnalysisData],
      moveEvals: List[lila.llm.MoveEval],
      openingRefsByFen: Map[String, OpeningReference],
      probeResultsByPly: Map[Int, List[ProbeResult]] = Map.empty,
      variantKey: String
  ): List[GameArcMoment] =
    val draftedMoments =
      moments
        .sortBy(_.ply)
        .foldLeft(
          (
            List.empty[(GameArcMoment, Option[FullGameEvidenceSurfacePolicy.InternalProbeCandidate])],
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
              val ctx = NarrativeContextBuilder.build(
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
              val strategyPack = StrategyPackBuilder.build(data, ctx)
              val authoringSurface = AuthoringEvidenceSummaryBuilder.build(ctx)
              val preparedNarrative = buildHybridNarrativeParts(ctx, moment)
              val signalDigest =
                buildMomentSignalDigest(ctx, preparedNarrative.focusedOutline, strategyPack, authoringSurface.headline)
              val (fullText, focusedOutline) =
                renderHybridMomentNarrative(
                  ctx,
                  moment,
                  strategyPack = strategyPack,
                  signalDigest = signalDigest,
                  prepared = Some(preparedNarrative)
                )

              val (classification, narrativeEvent) = moment.momentType match
                case "Blunder"                            => (Some("Blunder"), "AdvantageSwing")
                case "MissedWin"                          => (Some("MissedWin"), "AdvantageSwing")
                case "Equalization"                       => (None, "Equalization")
                case "SustainedPressure"                  => (None, "SustainedPressure")
                case "TensionPeak"                        => (None, "TensionPeak")
                case "MateFound" | "MateLost" | "MateShift" => (None, "MatePivot")
                case other                                => (None, other)

              val collapseData =
                if moment.momentType == "Blunder" || moment.momentType == "SustainedPressure" then
                  CausalCollapseAnalyzer.analyze(moment.ply, moveEvals, data)
                else None
              val evidenceEligible =
                evidenceMomentsUsed < FullGameEvidenceSurfacePolicy.MaxMoments &&
                  FullGameEvidenceSurfacePolicy.eligible(moment.momentType, ctx, focusedOutline)
              val internalProbeCandidate =
                FullGameEvidenceSurfacePolicy.internalCandidate(
                  momentType = moment.momentType,
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
                topEngineMove = data.alternatives.headOption.map { alt =>
                  lila.llm.EngineAlternative(
                    uci = alt.moves.headOption.getOrElse(""),
                    san = None,
                    cpAfterAlt = Some(alt.scoreCp),
                    cpLossVsPlayed = Some(cpLossForSideToMove(data.fen, alt.scoreCp, moment.cpAfter)),
                    pv = alt.moves
                  )
                },
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
                whyAbsentFromTopMultiPV = ctx.whyAbsentFromTopMultiPV.take(3)
              )

              val nextRef = ctx.openingData.orElse(prevRef)
              val nextEvidenceCount =
                evidenceMomentsUsed + (if surfacedEvidence.nonEmpty then 1 else 0)
              (acc :+ (momentNarrative -> internalProbeCandidate), Some(data), ctx.updatedBudget, nextRef, nextEvidenceCount)
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
      moment.copy(probeRefinementRequests = probeRefinementRequests)
    }

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
      moment: KeyMoment
  ): HybridNarrativeParts = {
      val bead = Math.abs(ctx.hashCode) ^ (moment.ply * 0x9e3779b9)
      val phase = ctx.phase.current
      val collapsedEarlyOpening = EarlyOpeningNarrationPolicy.collapsedEarlyOpening(ctx)
      val plan = topStrategicPlanName(ctx)
      val cpWhite = ctx.engineEvidence.flatMap(_.best).map(_.scoreCp)
      val rankedVars = rankEngineVariationsForFen(ctx.fen, ctx.engineEvidence.toList.flatMap(_.variations))
      val tacticalPressure =
        ctx.threats.toUs.exists(t => t.lossIfIgnoredCp >= 80 || t.kind.toLowerCase.contains("mate")) ||
          ctx.header.criticality.toLowerCase.contains("high") ||
          ctx.candidates.exists(c => c.tags.contains(CandidateTag.Sharp) || c.tags.contains(CandidateTag.TacticalGamble))

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
        Option.when(!collapsedEarlyOpening)(buildCriticalBranchNarrative(ctx, rankedVars, bead)).flatten
      val validatedOutline = BookStyleRenderer.validatedOutline(ctx)
      val focusedOutline = focusMomentOutline(validatedOutline, criticalBranch.nonEmpty, Some(ctx))

      val bookBody = FullGameDraftNormalizer.normalize(BookStyleRenderer.renderValidatedOutline(validatedOutline, ctx)).trim
      val focusedBody = FullGameDraftNormalizer.normalize(BookStyleRenderer.renderValidatedOutline(focusedOutline, ctx)).trim
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
      @unused signalDigest: Option[NarrativeSignalDigest] = None
  ): String =
    StrategicThesisBuilder.build(ctx, strategyPack)
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
    prepared: Option[HybridNarrativeParts] = None
  ): (String, NarrativeOutline) = {
    val parts = prepared.getOrElse(buildHybridNarrativeParts(ctx, moment))
    StandardCommentaryClaimPolicy.noEventNote(ctx) match
      case Some(note) => (note, parts.focusedOutline)
      case None =>
        val rendered = GameChronicleCompressionPolicy.render(ctx, parts).getOrElse("")
        (
          EarlyOpeningNarrationPolicy.clampNarrative(
            ctx,
            StandardCommentaryClaimPolicy.finalizeProse(
              ctx,
              FullGameDraftNormalizer.normalize(rendered).trim
            )
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
    bead: Int
  ): Option[String] =
    if rankedVars.size < 2 then None
    else
      val best = rankedVars.head
      val playedBranch = rankedVars.find(v => variationMatchesPlayed(ctx, v))
      val pivot = playedBranch.filterNot(_ == best).orElse(rankedVars.lift(1))
      pivot.flatMap { alt =>
        val bestMove = variationFirstMoveSan(ctx.fen, best).getOrElse("the engine first move")
        val altMove = variationFirstMoveSan(ctx.fen, alt).getOrElse("the alternative move")
        val altIsPlayed = variationMatchesPlayed(ctx, alt)
        val cpLoss = cpLossForSideToMove(ctx.fen, best.effectiveScore, alt.effectiveScore)
        val gapPawns = f"${cpLoss.toDouble / 100}%.1f"
        val bestCitation = LineScopedCitation.strategicCitation(ctx.fen, ctx.ply + 1, best)
        val altCitation = LineScopedCitation.strategicCitation(ctx.fen, ctx.ply + 1, alt)
        for
          bestLine <- bestCitation
          altLine <- altCitation
          bestSentence <- LineScopedCitation.afterClause(
            bestLine,
            if cpLoss <= 20 then
              s"**$bestMove** stays only slightly ahead of **$altMove**"
            else if altIsPlayed && cpLoss >= 120 then
              s"**$bestMove** keeps about a $gapPawns-pawn edge over the played **$altMove**"
            else
              s"**$bestMove** keeps about a $gapPawns-pawn edge over **$altMove**"
          )
          altSentence <- LineScopedCitation.afterClause(
            altLine,
            if altIsPlayed then
              s"the played branch gives up the cleaner continuation"
            else s"the alternative branch remains secondary"
          )
        yield s"$bestSentence $altSentence"
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
    authoringEvidence: Option[String]
  ): Option[NarrativeSignalDigest] =
    val base =
      NarrativeSignalDigestBuilder.buildWithAuthoringEvidence(
        ctx,
        preservedSignalsOverride = Some(NarrativeSignalDigestBuilder.preservedSignalsFromOutline(ctx, focusedOutline)),
        authoringEvidence = authoringEvidence
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
