package lila.commentary.analysis

import _root_.chess.*
import _root_.chess.format.Uci
import lila.commentary.CommentaryConfig
import lila.commentary.analysis.structure.{ PawnStructureClassifier, PlanAlignmentScorer, StructuralPlaybook }
import lila.commentary.model.*
import lila.commentary.model.strategic.{ VariationLine, PlanContinuity, StrategicSalience }
import lila.commentary.analysis.L3.*
import lila.commentary.analysis.PositionAnalyzer
import lila.commentary.analysis.PlanTaxonomy.ThemeResolver

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
  private val commentaryConfig = CommentaryConfig.fromEnv


  private def themeMaxShareFromHypotheses(
      hypotheses: List[lila.commentary.model.authoring.PlanHypothesis]
  ): Double =
    val themes = hypotheses.map(ThemeResolver.fromHypothesis).map(_.id).filter(_.nonEmpty)
    if themes.isEmpty then 1.0
    else
      val counts = themes.groupBy(identity).view.mapValues(_.size).toMap
      counts.values.maxOption.getOrElse(0).toDouble / themes.size.toDouble

  // Lazily instantiated Analyzers (stateless)
  private val prophylaxisAnalyzer = new lila.commentary.analysis.strategic.ProphylaxisAnalyzerImpl()
  private val activityAnalyzer = new lila.commentary.analysis.strategic.ActivityAnalyzerImpl()
  private val structureAnalyzer = new lila.commentary.analysis.strategic.StructureAnalyzerImpl()
  private val endgameAnalyzer = new lila.commentary.analysis.strategic.EndgameAnalyzerImpl()
  private val practicalityScorer = new lila.commentary.analysis.strategic.PracticalityScorerImpl()
  
  private val semanticExtractor = new lila.commentary.analysis.StrategicFeatureExtractorImpl(
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
   * Formats the assessment into a sparse text representation for the AI prompt.
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
      val resultingFen =
        val normalizedPv = pv.map(MoveReviewPvLine.normalizeUci)
        Option.when(normalizedPv.nonEmpty && normalizedPv.forall(MoveReviewExchangeAnalyzer.isUciMove))(normalizedPv)
          .flatMap(
            _.foldLeft(Option(fen))((current, move) =>
              current.flatMap(MoveReviewPvLine.legalFenAfter(_, move))
            )
          )
      val mainVariation = VariationLine(
        moves = pv,
        scoreCp = 0, 
        mate = None,
        resultingFen = resultingFen,
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
      prevEndgameState: Option[lila.commentary.model.strategic.EndgamePatternState] = None
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
        val structureEvalStartNs = Option.when(commentaryConfig.shouldEvaluateStructureKb)(System.nanoTime())
        val structureProfile =
          Option.when(commentaryConfig.shouldEvaluateStructureKb) {
            PawnStructureClassifier.classify(
              features = features,
              board = initialPos.board,
              sideToMove = initialPos.color,
              minConfidence = commentaryConfig.structKbMinConfidence
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
            ctx = ctx,
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
