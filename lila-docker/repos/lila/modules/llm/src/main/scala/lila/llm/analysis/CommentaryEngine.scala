package lila.llm.analysis

import _root_.chess.*
import _root_.chess.format.Uci
import lila.llm.{ LlmConfig, LlmLevel, NarrativeSignalDigest }
import lila.llm.analysis.structure.{ PawnStructureClassifier, PlanAlignmentScorer, StructuralPlaybook }
import lila.llm.model.*
import lila.llm.model.authoring.{ NarrativeOutline, OutlineBeatKind }
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
  private val LegacyStrategicFallbackText = boolEnv("LLM_LEGACY_STRATEGIC_TEXT_FALLBACK", default = false)

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
         val moveEvals = plyDataList.map { p =>
           val vars = evals.getOrElse(p.ply, Nil)
           val bestV = vars.headOption
           lila.llm.MoveEval(
             ply = p.ply,
             cp = bestV.map(_.scoreCp).getOrElse(0),
             mate = bestV.flatMap(_.mate),
             variations = vars
           )
         }

         val keyMoments = GameNarrativeOrchestrator.selectKeyMoments(moveEvals)
         val keyPlies: Set[Int] = keyMoments.map(_.ply).toSet ++ extraPlies

         // Track both Full Data, Plan Sequence Tracker, and previous Analysis Data
         val (results, _, _) =
           plyDataList
             .filter(p => keyPlies.contains(p.ply)) // Only analyze key plies
             .sortBy(_.ply)
              .foldLeft(
                (List.empty[ExtendedAnalysisData], PlanStateTracker.empty, Option.empty[lila.llm.model.strategic.EndgamePatternState])
              ) { case ((acc, planTracker, prevEgState), p) =>
                  val playedUci = p.playedUci
                  val vars = evals.getOrElse(p.ply, Nil)
                  val ply = p.ply

                  if (vars.nonEmpty) {
                    // Determine the side to move for this ply (FEN has the currently moving side)
                    val isWhiteTurn = p.fen.contains(" w ")
                    val movingColor = if (isWhiteTurn) _root_.chess.Color.White else _root_.chess.Color.Black

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

                    analysis match {
                      case Some(data) => 
                        // Update Tracker with the plans found in this ply
                        val nextTracker = planTracker.update(
                          movingColor = movingColor,
                          ply = ply,
                          primaryPlan = data.plans.headOption,
                          secondaryPlan = data.plans.lift(1),
                          sequence = data.planSequence
                        )
                        val nextEgState = lila.llm.model.strategic.EndgamePatternState.evolve(prevEgState, data.endgameFeatures, ply)
                        (acc :+ data.copy(planContinuity = nextTracker.getContinuity(movingColor)), nextTracker, nextEgState)
                      case None => 
                        (acc, planTracker, prevEgState)
                    }
                  } else {
                    (acc, planTracker, prevEgState)
                  }
         }
         results
       case scala.util.Left(_) => 
         Nil
     }
  }

  def generateFullGameNarrative(
      pgn: String,
      evals: Map[Int, List[VariationLine]],
      providedMetadata: Option[GameMetadata] = None,
      openingRefsByFen: Map[String, OpeningReference] = Map.empty,
      @unused llmLevel: String = LlmLevel.Polish
  ): FullGameNarrative = {
     
     val metadata = providedMetadata.getOrElse(extractMetadata(pgn))
     
     lila.llm.PgnAnalysisHelper.extractPlyData(pgn) match {
       case scala.util.Right(plyDataList) =>
          val moveEvals = plyDataList.map { p =>
             val vars = evals.getOrElse(p.ply, Nil)
             val bestV = vars.headOption
             lila.llm.MoveEval(
               ply = p.ply,
               cp = bestV.map(_.scoreCp).getOrElse(0),
               mate = bestV.flatMap(_.mate),
               variations = vars
             )
           }
          val keyMoments = GameNarrativeOrchestrator.selectKeyMoments(moveEvals)
          val keyMomentPlies = keyMoments.map(_.ply).toSet
          val openingMoments = detectOpeningEventMoments(plyDataList, openingRefsByFen)
          val extraOpeningMoments = openingMoments.filterNot(m => keyMomentPlies.contains(m.ply))
          val extraOpeningPlies = extraOpeningMoments.map(_.ply).toSet

          val allMoments = (keyMoments ++ extraOpeningMoments)
            .sortBy(_.ply)
            .distinctBy(_.ply)
          
          // Analyze only key moment plies
          val keyMomentsWithData = analyzeGame(pgn, evals, extraPlies = extraOpeningPlies)
          val dataByPly = keyMomentsWithData.map(d => d.ply -> d).toMap
          val (momentNarratives, _, _, _, _) = allMoments.foldLeft(
            (List.empty[MomentNarrative], Option.empty[ExtendedAnalysisData], OpeningEventBudget(), Option.empty[OpeningReference], 0)
          ) {
            case ((acc, prevAnalysis, budget, prevRef, evidenceMomentsUsed), moment) =>
              dataByPly.get(moment.ply) match {
                case Some(data) =>
                  // Build NarrativeContext with A9 budget and prevRef
                  val ctx = NarrativeContextBuilder.build(
                    data, 
                    data.toContext, 
                    prevAnalysis,
                    Nil,  // probeResults
                    openingRefsByFen.get(data.fen),
                    prevRef,
                    budget,
                    renderMode = NarrativeRenderMode.FullGame
                  )
                  val (fullText, focusedOutline) = renderHybridMomentNarrative(ctx, moment)
                  
                  val (classification, narrativeEvent) = moment.momentType match {
                    case "Blunder" => (Some("Blunder"), "AdvantageSwing")
                    case "MissedWin" => (Some("MissedWin"), "AdvantageSwing")
                    case "Equalization" => (None, "Equalization")
                    case "SustainedPressure" => (None, "SustainedPressure")
                    case "TensionPeak" => (None, "TensionPeak")
                    case "MateFound" | "MateLost" | "MateShift" => (None, "MatePivot")
                    case other => (None, other)
                  }

                  val collapseData = if (moment.momentType == "Blunder" || moment.momentType == "SustainedPressure") {
                    CausalCollapseAnalyzer.analyze(moment.ply, moveEvals, data)
                  } else None
                  val strategyPack = StrategyPackBuilder.build(data, ctx)
                  val signalDigest = buildMomentSignalDigest(ctx, focusedOutline)
                  val authorQuestions = AuthoringEvidenceSummaryBuilder.summarizeQuestions(ctx)
                  val authorEvidence = AuthoringEvidenceSummaryBuilder.summarizeEvidence(ctx)
                  val evidenceEligible =
                    evidenceMomentsUsed < FullGameEvidenceSurfacePolicy.MaxMoments &&
                      FullGameEvidenceSurfacePolicy.eligible(moment.momentType, ctx, focusedOutline)
                  val surfacedEvidence =
                    FullGameEvidenceSurfacePolicy.payload(
                      eligible = evidenceEligible,
                      probeRequests = ctx.probeRequests,
                      authorQuestions = authorQuestions,
                      authorEvidence = authorEvidence
                    )

                  val momentNarrative = MomentNarrative(
                    ply = moment.ply,
                    momentType = narrativeEvent,
                    narrative = fullText,
                    analysisData = data,
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
                    authorQuestions = surfacedEvidence.authorQuestions,
                    authorEvidence = surfacedEvidence.authorEvidence,
                    mainStrategicPlans = ctx.mainStrategicPlans.take(3),
                    latentPlans = ctx.latentPlans.take(2),
                    whyAbsentFromTopMultiPV = ctx.whyAbsentFromTopMultiPV.take(3)
                  )
                  
                  // Update budget from ctx.updatedBudget for next iteration
                  val nextRef = ctx.openingData.orElse(prevRef)
                  val nextEvidenceCount =
                    evidenceMomentsUsed + (if surfacedEvidence.nonEmpty then 1 else 0)
                  (acc :+ momentNarrative, Some(data), ctx.updatedBudget, nextRef, nextEvidenceCount)
                case None => (acc, prevAnalysis, budget, prevRef, evidenceMomentsUsed)
              }
          }
           
          
          val totalPlies = plyDataList.lastOption.map(_.ply).getOrElse(0)
          val blundersCount = allMoments.count(_.momentType == "Blunder")
          val missedWinsCount = allMoments.count(_.momentType == "MissedWin")

          val gameIntro = lila.llm.analysis.NarrativeLexicon.gameIntro(
            metadata.white, metadata.black, metadata.event, metadata.date, metadata.result,
            totalPlies = totalPlies,
            keyMomentsCount = allMoments.size
          )
          
          val planThemes = keyMomentsWithData.flatMap(_.plans.map(_.plan.name)).distinct
          val allThemes = if (planThemes.nonEmpty) planThemes.take(3) 
                          else keyMomentsWithData.flatMap(_.conceptSummary).distinct.take(3)
           
          val conclusion = lila.llm.analysis.NarrativeLexicon.gameConclusion(
            winner = if (metadata.result.startsWith("1-0")) Some(metadata.white) 
                     else if (metadata.result.startsWith("0-1")) Some(metadata.black) 
                     else None,
            themes = allThemes,
            blunders = blundersCount,
            missedWins = missedWinsCount
          )
          
          FullGameNarrative(gameIntro, momentNarratives, conclusion, allThemes)
       case scala.util.Left(err) =>
          FullGameNarrative(
            gameIntro = "Analysis Failed",
            keyMomentNarratives = Nil,
            conclusion = s"Could not parse game: $err",
            overallThemes = Nil
          )
      }
  }

  private def renderHybridMomentNarrative(
    ctx: NarrativeContext,
    moment: KeyMoment
  ): (String, NarrativeOutline) = {
    val bead = Math.abs(ctx.hashCode) ^ (moment.ply * 0x9e3779b9)
    val phase = ctx.phase.current
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
    val criticalBranch = buildCriticalBranchNarrative(ctx, rankedVars, bead)
    val validatedOutline = BookStyleRenderer.validatedOutline(ctx)
    val focusedOutline = focusMomentOutline(validatedOutline, criticalBranch.nonEmpty)

    val bookBody = BookStyleRenderer.renderValidatedOutline(validatedOutline, ctx).trim
    val focusedBody = BookStyleRenderer.renderValidatedOutline(focusedOutline, ctx).trim
    val fallback = lila.llm.NarrativeGenerator.describeHierarchical(ctx).trim
    val body0 = if bookBody.nonEmpty then bookBody else fallback
    val body =
      if focusedBody.nonEmpty then focusedBody
      else focusMomentBody(body0, keepParagraphs = if criticalBranch.nonEmpty then 3 else 4)
    val preface = List(lead, bridge).map(_.trim).filter(_.nonEmpty).mkString(" ")

    val parts = List(preface, criticalBranch.getOrElse(""), body).map(_.trim).filter(_.nonEmpty)
    val rendered =
      if parts.nonEmpty then parts.mkString("\n\n")
      else if body.nonEmpty then body
      else preface
    (rendered, focusedOutline)
  }

  private[analysis] def focusMomentOutline(
    outline: NarrativeOutline,
    hasCriticalBranch: Boolean
  ): NarrativeOutline =
    val maxBeats = if hasCriticalBranch then 5 else 6
    val candidates =
      outline.beats.zipWithIndex.filter { case (beat, _) =>
        beat.kind != OutlineBeatKind.MoveHeader &&
          beat.kind != OutlineBeatKind.Evidence &&
          beat.kind != OutlineBeatKind.Alternatives
      }
    val filtered =
      candidates.filterNot { case (beat, _) =>
        beat.kind == OutlineBeatKind.WrapUp &&
          beat.focusPriority < 75 &&
          isGenericWrapupParagraph(beat.text)
      }
    val essentials = filtered.filter(_._1.fullGameEssential)
    val essentialSlots =
      if essentials.size <= maxBeats then essentials
      else essentials.sortBy { case (beat, idx) => (-beat.focusPriority, idx) }.take(maxBeats)
    val selectedIndices = scala.collection.mutable.Set.empty[Int]
    essentialSlots.foreach { case (_, idx) => selectedIndices += idx }
    val remainingSlots = (maxBeats - essentialSlots.size).max(0)
    filtered
      .filterNot { case (_, idx) => selectedIndices.contains(idx) }
      .sortBy { case (beat, idx) => (-beat.focusPriority, idx) }
      .take(remainingSlots)
      .foreach { case (_, idx) => selectedIndices += idx }
    val selectedBeats =
      filtered
        .filter { case (_, idx) => selectedIndices.contains(idx) }
        .sortBy(_._2)
        .map(_._1)
    NarrativeOutline(selectedBeats, outline.diagnostics)

  private def topStrategicPlanName(ctx: NarrativeContext): Option[String] =
    ctx.mainStrategicPlans.headOption.map(_.planName).orElse {
      if LegacyStrategicFallbackText then ctx.plans.top5.headOption.map(_.name)
      else None
    }

  private def boolEnv(name: String, default: Boolean): Boolean =
    sys.env
      .get(name)
      .map(_.trim.toLowerCase)
      .flatMap {
        case "1" | "true" | "yes" | "on"  => Some(true)
        case "0" | "false" | "no" | "off" => Some(false)
        case _                              => None
      }
      .getOrElse(default)

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
        val bestLine = variationLinePreview(ctx.fen, best).map(_.stripPrefix(bestMove).trim).filter(_.nonEmpty)
        val altLine = variationLinePreview(ctx.fen, alt).map(_.stripPrefix(altMove).trim).filter(_.nonEmpty)
        val intro =
          NarrativeLexicon.pick(bead ^ 0x4f1bbcdc, List(
            "Critical branch:",
            "Critical branch focus:",
            "Main branch contrast:"
          ))
        val contrast =
          if cpLoss <= 20 then
            NarrativeLexicon.pick(bead ^ 0x2c1b3c6d, List(
              s"Both moves are of comparable strength, with **$bestMove** being the engine's slight preference.",
              s"The evaluation difference is minimal here; both **$bestMove** and **$altMove** are practically sound options."
            ))
          else if altIsPlayed && cpLoss >= 120 then
            NarrativeLexicon.pick(bead ^ 0x2c1b3c6d, List(
              s"The played move **$altMove** concedes heavily; **$bestMove** was superior by about $gapPawns pawns.",
              s"After the game move **$altMove**, engine preference shifts strongly to **$bestMove** (about $gapPawns pawns)."
            ))
          else
            NarrativeLexicon.pick(bead ^ 0x2c1b3c6d, List(
              s"Engine preference is clear: **$bestMove** over **$altMove** by about $gapPawns pawns.",
              s"Compared with **$altMove**, **$bestMove** holds roughly a $gapPawns-pawn edge."
            ))
        val bestSeq = bestLine.map(l => s" Primary engine line: **$bestMove** $l.").getOrElse(s" Primary engine line starts with **$bestMove**.")
        val altLabel = if altIsPlayed then "Played branch" else "Alternative branch"
        val altSeq = altLine.map(l => s" $altLabel: **$altMove** $l.").getOrElse(s" $altLabel starts with **$altMove**.")
        val reason = branchReasonClause(ctx, bestMove).map(r => s" $r").getOrElse("")
        Some(s"$intro $contrast$bestSeq$altSeq$reason")
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
    focusedOutline: NarrativeOutline
  ): Option[NarrativeSignalDigest] =
    NarrativeSignalDigestBuilder.build(
      ctx,
      preservedSignalsOverride = Some(NarrativeSignalDigestBuilder.preservedSignalsFromOutline(ctx, focusedOutline))
    )

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
      ctx.facts.collectFirst {
        case Fact.HangingPiece(square, role, _, _, _) =>
          s"It also keeps the ${role.toString.toLowerCase} on ${square.key} from becoming a tactical liability."
        case Fact.Pin(_, _, pinned, pinnedRole, _, _, _, _) =>
          s"It reduces the pin pressure against the ${pinnedRole.toString.toLowerCase} on ${pinned.key}."
        case Fact.WeakSquare(square, _, _, _) =>
          s"It prevents long-term weakening around ${square.key}."
      }
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
          description = "Opening reference"
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
