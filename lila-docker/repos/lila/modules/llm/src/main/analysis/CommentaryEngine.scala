package lila.llm.analysis

import _root_.chess.*
import _root_.chess.format.Uci
import lila.llm.model.*
import lila.llm.model.strategic.{FullGameNarrative, MomentNarrative, GameMetadata, VariationLine}
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
    _root_.chess.format.Fen.read(_root_.chess.variant.Standard, _root_.chess.format.Fen.Full(fen)).map { initialPos =>
      val motifs = MoveAnalyzer.tokenizePv(fen, pv)
      
      val lastPos = pv.foldLeft(initialPos) { (p, uciStr) =>
        Uci(uciStr).collect { case m: Uci.Move => m }.flatMap(p.move(_).toOption).map(_.after).getOrElse(p)
      }

      val nature = PositionCharacterizer.characterize(lastPos)
      val currentPhase = determinePhase(lastPos)
      
      val ctx = IntegratedContext(
        eval = 0.0,
        phase = phase.getOrElse(currentPhase.toString.toLowerCase),
        openingName = opening
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
      prevMove: Option[String] = None
  ): Option[ExtendedAnalysisData] =
    _root_.chess.format.Fen.read(_root_.chess.variant.Standard, _root_.chess.format.Fen.Full(fen)).map { initialPos =>
      
      val mainPv = variations.headOption.map(_.moves).getOrElse(Nil)
      val motifs = MoveAnalyzer.tokenizePv(fen, mainPv)
      
      val lastPos = mainPv.foldLeft(initialPos) { (p, uciStr) =>
        Uci(uciStr).collect { case m: Uci.Move => m }.flatMap(p.move(_).toOption).map(_.after).getOrElse(p)
      }

      val nature = PositionCharacterizer.characterize(lastPos)
      val currentPhase = determinePhase(lastPos)
      
      val ctx = IntegratedContext(
        eval = 0.0,
        phase = phase.getOrElse(currentPhase.toString.toLowerCase),
        openingName = opening
      )
      
      val planScoring = PlanMatcher.matchPlans(motifs, ctx, lastPos.color)

      val baseData = BaseAnalysisData(
        nature = nature,
        motifs = motifs,
        plans = planScoring.topPlans
      )
      
      val meta = AnalysisMetadata(
        color = lastPos.color,
        ply = ply,
        prevMove = prevMove
      )

      semanticExtractor.extract(
        fen = fen,
        metadata = meta,
        baseData = baseData,
        vars = variations,
        playedMove = playedMove
      )
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

           plyDataList.flatMap { plyData =>
             val ply = plyData.ply
             
             if (keyPlies.contains(ply)) {
               val variations = evals.getOrElse(ply, Nil)
               val prevFen = if (ply == 1) startFen else plyMap.get(ply - 1).map(_.fen).getOrElse(startFen)
               val ctxMove = if (ply > 1) plyMap.get(ply - 1).map(_.playedUci) else None

               assessExtended(
                 fen = prevFen,
                 variations = variations,
                 playedMove = Some(plyData.playedUci),
                 ply = ply,
                 prevMove = ctxMove
               )
             } else None
           }
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
          
          val momentNarratives = keyMoments.flatMap { moment =>
            dataByPly.get(moment.ply).map { data =>
              // FIX 1: Include describeExtended for rich semantic context
              val extended = lila.llm.NarrativeGenerator.describeExtended(data)
              val bookStyle = lila.llm.NarrativeGenerator.describeCandidatesBookStyle(
                candidates = data.candidates,
                fen = data.fen,
                ply = moment.ply
              )
              
              // Full narrative = extended analysis + book-style candidate breakdown
              val fullText = s"$extended\n\n$bookStyle"
              
              MomentNarrative(
                ply = moment.ply,
                momentType = moment.momentType,
                narrative = fullText,
                analysisData = data
              )
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
