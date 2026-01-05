package lila.llm.analysis

import lila.llm.model.{ Motif, ExtendedAnalysisData }
import lila.llm.model.strategic.{ VariationLine, VariationTag, PreventedPlan, PositionalTag, EndgameFeature }
import lila.llm.analysis.strategic._
import chess.format.Fen
import chess.variant.Standard

class StrategicFeatureExtractorImpl(
    prophylaxisAnalyzer: ProphylaxisAnalyzer,
    activityAnalyzer: ActivityAnalyzer,
    structureAnalyzer: StructureAnalyzer,
    endgameAnalyzer: EndgameAnalyzer,
    practicalityScorer: PracticalityScorer
) extends StrategicFeatureExtractor {

  def extract(
      fen: String,
      metadata: AnalysisMetadata,
      baseData: BaseAnalysisData,
      vars: List[lila.llm.model.strategic.VariationLine],
      playedMove: Option[String]
  ): ExtendedAnalysisData = {

    val board = Fen.read(Standard, Fen.Full(fen)).map(_.board).getOrElse(chess.Board.empty)
    val color = metadata.color
    
    // ===== DEBT 1: CP Contract Validation (see docs/CpContract.md) =====
    // All CP must be White absolute perspective. Mate scores map to high values.
    assert(
      vars.forall(v => v.scoreCp.abs < 10000 || v.mate.isDefined), 
      s"CP score out of expected range - verify White perspective contract. Got: ${vars.map(_.scoreCp)}"
    )

    // ===== FIX 1: Inject played line into vars if missing =====
    val varsWithPlayed = playedMove match {
      case Some(move) if !vars.exists(_.moves.headOption.contains(move)) =>
        // Create synthetic line for the played move (estimate score as worst of existing or -100)
        val worstScore = vars.map(_.effectiveScore).minOption.getOrElse(0) - 50
        val playedLine = VariationLine(
          moves = List(move),
          scoreCp = worstScore,
          mate = None,
          tags = Nil
        )
        vars :+ playedLine
      case _ => vars
    }

    // ===== FIX 4: Sort vars by effectiveScore with COLOR NORMALIZATION =====
    // For White: higher score is better, sort descending (-score)
    // For Black: lower score is better (more negative), sort ascending (score)
    val sortedVars = varsWithPlayed.sortBy(v => if (color.white) -v.effectiveScore else v.effectiveScore)
    val defaultVar = VariationLine(moves = Nil, scoreCp = 0, mate = None, tags = Nil)
    val bestVar = sortedVars.headOption.getOrElse(defaultVar)
    
    // ===== FIX 8: Perspective Correction & Score Unification =====
    // Use normalized score logic for consistent scaling
    def normalizeScore(v: VariationLine): Int = v.mate match {
      case Some(m) if m > 0 => 2000 + (20 - m.min(20)) * 100  // Positive mate: 2000-4000
      case Some(m) if m < 0 => -2000 - (20 - (-m).min(20)) * 100 // Negative mate: -2000 to -4000
      case _ => v.scoreCp
    }
    
    // Create Normalized Variations for Comparisons
    val normalizedBestScore = normalizeScore(bestVar)
    val normalizedBestVar = bestVar.copy(scoreCp = normalizedBestScore, mate = None)
    val relativeScore = if (color.white) normalizedBestScore else -normalizedBestScore

    // ===== FIX 3: Real Threat Detection with Motif-based planId =====
    val detectedThreats = lila.llm.analysis.MoveAnalyzer.detectThreats(fen, color)
    val (threatLineRaw, threatPlanId) = if (detectedThreats.nonEmpty) {
      // Derive planId from actual motifs
      val planId = detectedThreats.collectFirst {
        case _: Motif.Check => "Checkmate Threat"
        case m: Motif.Capture if m.captureType == Motif.CaptureType.Winning => "Material Loss"
        case _: Motif.Fork => "Fork Threat"
        case _: Motif.Pin => "Pin Threat"
      }.getOrElse("Tactical Threat")
      
      val threatValue = detectedThreats.map {
        case _: Motif.Check => 100
        case _: Motif.Fork => 300
        case m: Motif.Capture if m.captureType == Motif.CaptureType.Winning => 200
        case _ => 100
      }.max
      
      // Calculate normalized threat score
      val threatScore = normalizedBestScore - (if (color.white) threatValue else -threatValue)
      
      (Some(VariationLine(
        moves = detectedThreats.flatMap(_.move).take(3),
        scoreCp = threatScore,
        mate = None,
        tags = Nil
      )), Some(planId))
    } else (None, None)

    // Wire to ProphylaxisAnalyzer with normalized inputs and explicit plan ID
    // Note: inputs are now normalized, so analyzer's scoreDiff will be correct
    val preventedPlans = prophylaxisAnalyzer.analyze(board, color, normalizedBestVar, threatLineRaw, threatPlanId)

    val pieceActivity = activityAnalyzer.analyze(board, color)
    val structuralWeaknesses = structureAnalyzer.analyze(board)
    val positionalFeatures = structureAnalyzer.detectPositionalFeatures(board, color)
    val compensation = structureAnalyzer.analyzeCompensation(board, color)
    val endgameFeatures = endgameAnalyzer.analyze(board, color)

    // Score Practicality with normalized score
    val practicalAssessment = practicalityScorer.score(
      board, 
      color, 
      relativeScore, 
      sortedVars, 
      pieceActivity, 
      structuralWeaknesses, 
      endgameFeatures
    )

    // ===== FIX 1 continued: Counterfactual with injected played line =====
    val counterfactual = playedMove.flatMap { move =>
      val userLine = sortedVars.find(_.moves.headOption.contains(move))
      
      userLine.flatMap { ul =>
        if (bestVar.moves.headOption != Some(move)) {
          // FIX 2: Use normalized cpLoss (normalization logic reused)
          val normalizedUserScore = normalizeScore(ul)
          val cpLoss = (if (color.white) normalizedBestScore - normalizedUserScore 
                        else normalizedUserScore - normalizedBestScore).abs
          
          Some(lila.llm.analysis.CounterfactualAnalyzer.createMatchNormalized(
            fen = fen,
            userMove = move,
            bestMove = bestVar.moves.headOption.getOrElse("?"),
            userLine = ul,
            bestLine = bestVar,
            cpLoss = cpLoss
          ))
        } else None
      }
    }

    // ===== FIX 4 continued: Tag AFTER sorting =====
    // Generate Extended Analysis Data
    val analyzedVars = lila.llm.analysis.MoveAnalyzer.analyzeVariations(fen, sortedVars, threatLineRaw)
    val bestScoreNorm = analyzedVars.headOption.map(v => normalizeScore(v)).getOrElse(0)
    
    val enrichedAlternatives = analyzedVars.map { v =>
      val vScoreNorm = normalizeScore(v)
      val diff = (if (color.white) bestScoreNorm - vScoreNorm else vScoreNorm - bestScoreNorm).abs
      val extraTags = if (diff > 300) List(VariationTag.Blunder)
                      else if (diff > 100) List(VariationTag.Mistake)
                      else if (diff < 20) List(VariationTag.Excellent)
                      else Nil
      v.copy(tags = (v.tags ++ extraTags).distinct)
    }.take(3)

    // ===== NEW: Deep Analysis of Top Candidates (Multi-PV) =====
    val candidates = sortedVars.take(3).map { candidateVar =>
      val candScore = normalizeScore(candidateVar)
      val candMotifs = lila.llm.analysis.MoveAnalyzer.tokenizePv(fen, candidateVar.moves)
      
      // FIX: Normalize candidate before passing to prophylaxis to match threatLineRaw scale
      val normalizedCandidateVar = candidateVar.copy(scoreCp = candScore, mate = None)
      
      // Does this candidate prevent the threat?
      val candPrevented = prophylaxisAnalyzer.analyze(board, color, normalizedCandidateVar, threatLineRaw, threatPlanId)
      
      // Future context using actual motifs for richer differentiation
      val moveStr = candidateVar.moves.headOption.getOrElse("")
      val futureContext = {
        // Check for tactical motifs first
        val hasFork = candMotifs.exists(_.isInstanceOf[Motif.Fork])
        val hasPin = candMotifs.exists(_.isInstanceOf[Motif.Pin])
        val hasCheck = candMotifs.exists(_.isInstanceOf[Motif.Check])
        val hasCapture = candMotifs.exists(_.isInstanceOf[Motif.Capture])
        
        if (hasCheck) "Sharp attack with check"
        else if (hasFork) "Tactical shot (fork)"
        else if (hasPin) "Positional pressure (pin)"
        else if (moveStr.contains("+")) "Initiating an attack"
        else if (hasCapture || moveStr.contains("x")) "Forcing exchanges"
        else if (candPrevented.nonEmpty) "Prophylactic defense"
        else "Quiet maneuvering"
      }

      lila.llm.model.strategic.AnalyzedCandidate(
        move = moveStr,
        score = candScore,
        motifs = candMotifs,
        prophylaxisResults = candPrevented,
        futureContext = futureContext,
        line = candidateVar
      )
    }

    // Assemble Data
    ExtendedAnalysisData(
      fen = fen,
      nature = baseData.nature,
      motifs = baseData.motifs,
      plans = baseData.plans,
      preventedPlans = preventedPlans,
      pieceActivity = pieceActivity,
      structuralWeaknesses = structuralWeaknesses,
      positionalFeatures = positionalFeatures,
      compensation = compensation,
      endgameFeatures = endgameFeatures,
      practicalAssessment = Some(practicalAssessment),
      alternatives = enrichedAlternatives,
      candidates = candidates,
      counterfactual = counterfactual,
      // DEBT 4: Populate concept summary from detected features
      conceptSummary = deriveConceptSummary(baseData.nature, baseData.plans, positionalFeatures, endgameFeatures),
      prevMove = metadata.prevMove,
      ply = metadata.ply,
      evalCp = if (color.white) bestScoreNorm else -bestScoreNorm, // Use normalized score from variations
      isWhiteToMove = color.white,
      phase = baseData.nature.natureType.toString.toLowerCase,
      planSequence = baseData.planSequence
    )
  }
  
  // DEBT 4: Derive high-level strategic concepts from features
  private def deriveConceptSummary(
      nature: PositionNature,
      plans: List[lila.llm.model.PlanMatch],
      positionalFeatures: List[PositionalTag],
      endgameFeatures: Option[EndgameFeature]
  ): List[String] = {
    val concepts = scala.collection.mutable.ListBuffer[String]()
    
    // From Nature
    nature.natureType match {
      case lila.llm.analysis.NatureType.Static => concepts += "Positional battle"
      case lila.llm.analysis.NatureType.Dynamic => concepts += "Dynamic play"
      case lila.llm.analysis.NatureType.Chaos => concepts += "Tactical complexity"
      case lila.llm.analysis.NatureType.Transition => concepts += "Transitional phase"
    }

    // From Plans (Top 2 distinct Plans)
    plans.sortBy(-_.score).map(_.plan.name).distinct.take(2).foreach { planName =>
      concepts += planName
    }
    
    // From Positional Features
    positionalFeatures.foreach {
      case PositionalTag.Outpost(_, color) => concepts += s"${color.name} outpost advantage"
      case PositionalTag.OpenFile(_, color) => concepts += s"${color.name} file control"
      case PositionalTag.LoosePiece(_, color) => concepts += s"${color.name} has hanging piece"
      case PositionalTag.WeakBackRank(color) => concepts += s"${color.name} back rank weakness"
      case PositionalTag.RookOnSeventh(color) => concepts += s"${color.name} rook on 7th"
      case PositionalTag.BishopPairAdvantage(color) => concepts += s"${color.name} bishop pair"
      case PositionalTag.SpaceAdvantage(color) => concepts += s"${color.name} space control"
      case PositionalTag.OppositeColorBishops => concepts += "Opposite-color bishops"
      case PositionalTag.KingStuckCenter(color) => concepts += s"${color.name} king unsafe"
      case PositionalTag.ConnectedRooks(color) => concepts += s"${color.name} connected rooks"
      case _ => ()
    }
    
    // From Endgame
    endgameFeatures.foreach { eg =>
      if (eg.isZugzwang) concepts += "Zugzwang"
      if (eg.hasOpposition) concepts += "King opposition"
      if (eg.keySquaresControlled.nonEmpty) concepts += "Key square control"
    }
    
    concepts.distinct.toList.take(5)
  }
}
