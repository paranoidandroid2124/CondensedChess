package lila.llm.analysis

import _root_.chess.*
import _root_.chess.format.Uci
import lila.llm.model.*
import lila.llm.model.strategic.{FullGameNarrative, MomentNarrative, GameMetadata, VariationLine}
import lila.llm.analysis.L3.*
import lila.llm.analysis.PositionAnalyzer
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

enum GamePhase:
  case Opening, Middlegame, Endgame

object CommentaryEngine:

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
      pv: List[String],
      opening: Option[String] = None,
      phase: Option[String] = None
  ): Option[PositionAssessment] =
    _root_.chess.format.Fen.read(_root_.chess.variant.Standard, _root_.chess.format.Fen.Full(fen)).flatMap { initialPos =>
      PositionAnalyzer.extractFeatures(fen, 1).map { features =>
        val motifs = MoveAnalyzer.tokenizePv(fen, pv)
        
        val lastPos = pv.foldLeft(initialPos) { (p, uciStr) =>
          Uci(uciStr).collect { case m: Uci.Move => m }.flatMap(p.move(_).toOption).map(_.after).getOrElse(p)
        }

        val nature = PositionCharacterizer.characterize(lastPos)
        val currentPhase = determinePhase(lastPos)
        
        // L3 Phase 1: Classification
        val dummyPv = List(PvLine(pv, 0, None, 0))
        val classification = PositionClassifier.classify(
          features = features,
          multiPv = dummyPv,
          currentEval = 0,
          tacticalMotifsCount = motifs.size
        )

        // L3 Phase 2: Threat Analysis (Dual Perspective)
        val ownThreats = ThreatAnalyzer.analyze(
           motifs = motifs,
           multiPv = dummyPv,
           phase1 = classification,
           sideToMove = lastPos.color.name
        )
        val oppThreats = ThreatAnalyzer.analyze(
           motifs = motifs,
           multiPv = dummyPv,
           phase1 = classification,
           sideToMove = (!lastPos.color).name
        )
        
        // L3 Phase 3: Break & Pawn Play (Dual Perspective)
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
          openingName = opening,
          isWhiteToMove = lastPos.color == White,
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
      opening: Option[String],
      phase: Option[String],
      ply: Int,
      prevMove: Option[String]
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
        opening = opening,
        phase = phase,
        ply = ply,
        prevMove = prevMove
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
      prevPlanSequence: Option[PlanSequence] = None,
      prevAnalysis: Option[ExtendedAnalysisData] = None
  ): Option[ExtendedAnalysisData] =
    _root_.chess.format.Fen.read(_root_.chess.variant.Standard, _root_.chess.format.Fen.Full(fen)).flatMap { initialPos =>
      PositionAnalyzer.extractFeatures(fen, 1).map { features =>
        val mainPv = variations.headOption.map(_.moves).getOrElse(Nil)
        val motifs = MoveAnalyzer.tokenizePv(fen, mainPv)
        
        val lastPos = mainPv.foldLeft(initialPos) { (p, uciStr) =>
          Uci(uciStr).collect { case m: Uci.Move => m }.flatMap(p.move(_).toOption).map(_.after).getOrElse(p)
        }

        val nature = PositionCharacterizer.characterize(lastPos)
        val currentPhase = determinePhase(lastPos)
        
        // L3 Phase 1: Classification
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

        // L3 Phase 2: Threat Analysis (Dual Perspective)
        val ownThreats = ThreatAnalyzer.analyze(
           motifs = motifs,
           multiPv = multiPv,
           phase1 = classification,
           sideToMove = initialPos.color.name
        )
        val oppThreats = ThreatAnalyzer.analyze(
           motifs = motifs,
           multiPv = multiPv,
           phase1 = classification,
           sideToMove = (!initialPos.color).name
        )

        // L3 Phase 3: Break & Pawn Play & Threat Integration for PlanMatcher
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

        // FIX: Extract evalCp properly from variations head
        val evalCp = variations.headOption.map(_.scoreCp).getOrElse(0)

        // Update counterThreatBetter assessments
        val (finalUs, finalThem) = (ownThreats, oppThreats) match {
          case (toThem, toUs) =>
            val ourMax = toThem.threats.map(_.lossIfIgnoredCp).maxOption.getOrElse(0)
            val theirMax = toUs.threats.map(_.lossIfIgnoredCp).maxOption.getOrElse(0)
            val counterBetter = ourMax > theirMax + 100 && theirMax < 500 // We have better counter, and their threat isn't overwhelming
            (toThem.copy(counterThreatBetter = counterBetter, defense = toThem.defense.copy(counterIsBetter = counterBetter)), 
             toUs.copy(counterThreatBetter = counterBetter, defense = toUs.defense.copy(counterIsBetter = counterBetter)))
        }

        val ctx = IntegratedContext(
          evalCp = evalCp,
          classification = Some(classification),
          pawnAnalysis = Some(pawnAnalysis),
          opponentPawnAnalysis = Some(oppPawnAnalysis),
          threatsToUs = Some(finalThem),
          threatsToThem = Some(finalUs),
          openingName = opening,
          isWhiteToMove = initialPos.color == White, // Corrected: use FEN side-to-move for probes
          features = Some(features),  // A5: L1 Snapshot injection
          initialPos = Some(initialPos)
        )
        
        val planScoring = PlanMatcher.matchPlans(motifs, ctx, initialPos.color)
        val activePlans = PlanMatcher.toActivePlans(planScoring.topPlans, planScoring.compatibilityEvents)

        // PHASE 5: Transition Logic
        val currentSequence = TransitionAnalyzer.analyze(
          currentPlans = activePlans,
          previousPlan = prevPlanSequence.map(_.currentPlans.primary.plan),
          previousMomentum = prevPlanSequence.map(_.momentum).getOrElse(0.0),
          planHistory = prevPlanSequence.map(_.planHistory).getOrElse(Nil),
          ctx = ctx
        )

        val baseData = BaseAnalysisData(
          nature = nature,
          motifs = motifs,
          plans = planScoring.topPlans,
          planSequence = Some(currentSequence)
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
          playedMove = playedMove
        ).copy(
          planSequence = Some(currentSequence),
          tacticalThreatToUs = ctx.tacticalThreatToUs,
          tacticalThreatToThem = ctx.tacticalThreatToThem,
          evalCp = evalCp,
          isWhiteToMove = initialPos.color == White,
          phase = ctx.phase,
          integratedContext = Some(ctx)  // Fix 1: Preserve full IntegratedContext
        )
      }
    }

  def analyzeGame(
      pgn: String,
      evals: Map[Int, List[VariationLine]],
      initialFen: Option[String] = None
  ): List[ExtendedAnalysisData] = {
     lila.llm.PgnAnalysisHelper.extractPlyData(pgn) match {
       case scala.util.Right(plyDataList) =>
         val plyMap = plyDataList.map(p => p.ply -> p).toMap
         val startFen = initialFen.getOrElse("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")

           val moveEvals = plyDataList.map { p =>
             val vars = evals.getOrElse(p.ply, Nil)
             val bestV = vars.headOption
             lila.llm.MoveEval(
               ply = p.ply,
               cp = bestV.map(_.scoreCp).getOrElse(0),
               mate = bestV.flatMap(_.mate),
               variations = vars.map(v => lila.llm.AnalysisVariation(v.moves, v.scoreCp, v.mate))
             )
           }

           val keyMoments = GameNarrativeOrchestrator.selectKeyMoments(moveEvals)
           val keyPlies = keyMoments.map(_.ply).toSet

            val (results, _, _) = plyDataList.foldLeft((List.empty[ExtendedAnalysisData], Option.empty[PlanSequence], Option.empty[ExtendedAnalysisData])) {
              case ((acc, prevSeq, prevAnalysis), plyData) =>
                val ply = plyData.ply
                if (keyPlies.contains(ply)) {
                  val variations = evals.getOrElse(ply, Nil)
                  val prevFen = if (ply == 1) startFen else plyMap.get(ply - 1).map(_.fen).getOrElse(startFen)
                  val ctxMove = if (ply > 1) plyMap.get(ply - 1).map(_.playedUci) else None

                  val analysis = assessExtended(
                    fen = prevFen,
                    variations = variations,
                    playedMove = Some(plyData.playedUci),
                    ply = ply,
                    prevMove = ctxMove,
                    prevPlanSequence = prevSeq,
                    prevAnalysis = prevAnalysis
                  )
                  
                  analysis match {
                    case Some(data) => (acc :+ data, data.planSequence, Some(data))
                    case None => (acc, prevSeq, prevAnalysis)
                  }
                } else (acc, prevSeq, prevAnalysis)
            }
            results
       case scala.util.Left(_) => 
         Nil
     }
  }

  def generateFullGameNarrative(
      pgn: String,
      evals: Map[Int, List[VariationLine]],
      providedMetadata: Option[GameMetadata] = None
  ): FullGameNarrative = {
     
     val metadata = providedMetadata.getOrElse(extractMetadata(pgn))
     
     lila.llm.PgnAnalysisHelper.extractPlyData(pgn) match {
       case scala.util.Right(plyDataList) =>
          // FIX 2: Single moveEvals construction (used for key moments AND analysis)
          val moveEvals = plyDataList.map { p =>
             val vars = evals.getOrElse(p.ply, Nil)
             val bestV = vars.headOption
             lila.llm.MoveEval(
               ply = p.ply,
               cp = bestV.map(_.scoreCp).getOrElse(0),
               mate = bestV.flatMap(_.mate),
               variations = vars.map(v => lila.llm.AnalysisVariation(v.moves, v.scoreCp, v.mate))
             )
           }
           
          // FIX 2: Single call to selectKeyMoments
          val keyMoments = GameNarrativeOrchestrator.selectKeyMoments(moveEvals)
          
          // Analyze only key moment plies
          val keyMomentsWithData = analyzeGame(pgn, evals)
          
          // FIX 3: Defensive ply-based pairing instead of zip
          val dataByPly = keyMomentsWithData.map(d => d.ply -> d).toMap
          
          val (momentNarratives, _) = keyMoments.foldLeft((List.empty[MomentNarrative], Option.empty[ExtendedAnalysisData])) {
            case ((acc, prevAnalysis), moment) =>
              dataByPly.get(moment.ply) match {
                case Some(data) =>
                  // build NarrativeContext using current data and prevAnalysis
                  val ctx = NarrativeContextBuilder.build(data, data.toContext, prevAnalysis)
                  val fullText = lila.llm.NarrativeGenerator.describeHierarchical(ctx)
                  
                  val momentNarrative = MomentNarrative(
                    ply = moment.ply,
                    momentType = moment.momentType,
                    narrative = fullText,
                    analysisData = data
                  )
                  (acc :+ momentNarrative, Some(data))
                case None => (acc, prevAnalysis)
              }
          }
           
          val gameIntro = lila.llm.NarrativeTemplates.gameIntro(
            metadata.white, metadata.black, metadata.event, metadata.date, metadata.result
          )
          
          // FIX 4: Themes fallback to conceptSummary if plans are empty
          val planThemes = keyMomentsWithData.flatMap(_.plans.map(_.plan.name)).distinct
          val allThemes = if (planThemes.nonEmpty) planThemes.take(3) 
                          else keyMomentsWithData.flatMap(_.conceptSummary).distinct.take(3)
           
          val conclusion = lila.llm.NarrativeTemplates.gameConclusion(
            winner = if (metadata.result.startsWith("1-0")) Some(metadata.white) 
                     else if (metadata.result.startsWith("0-1")) Some(metadata.black) 
                     else None,
            themes = allThemes
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
