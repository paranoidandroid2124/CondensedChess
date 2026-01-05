package lila.llm.analysis

import chess.*
import lila.llm.model.*
import lila.llm.model.Motif.*
import lila.llm.analysis.L3.{PawnPlayAnalysis, PositionClassification, ThreatAnalysis, ThreatSeverity, RiskLevel}
import chess.Color.White

case class IntegratedContext(
    evalCp: Int, // Centipawns, ALWAYS White POV
    classification: Option[PositionClassification] = None,
    pawnAnalysis: Option[PawnPlayAnalysis] = None,          // Side to move
    opponentPawnAnalysis: Option[PawnPlayAnalysis] = None,  // Opponent
    // THREAT POV CLARIFICATION:
    threatsToUs: Option[ThreatAnalysis] = None,   // Threats OPPONENT makes TO US (defensive concern)
    threatsToThem: Option[ThreatAnalysis] = None, // Threats WE make TO THEM (attacking opportunity)
    openingName: Option[String] = None,
    isWhiteToMove: Boolean,
    // A5: L1 features for narrative layer
    features: Option[PositionFeatures] = None,
    initialPos: Option[Position] = None
) {
  def evalFor(color: Color): Int = if (color == White) evalCp else -evalCp

  /** Derive phase from classification or fallback to "middlegame" */
  def phase: String = classification.map(_.gamePhase.phaseType match {
    case lila.llm.analysis.L3.GamePhaseType.Opening => "opening"
    case lila.llm.analysis.L3.GamePhaseType.Middlegame => "middlegame"
    case lila.llm.analysis.L3.GamePhaseType.Endgame => "endgame"
  }).getOrElse("middlegame")

  // ============================================================
  // NUMERIC-BASED THREAT ASSESSMENT (replaces label-based)
  // ============================================================
  
  /** Tactical threat TO US: 1-2 moves, >= 200cp loss (mate, forced tactic) */
  def tacticalThreatToUs: Boolean = threatsToUs.exists(_.threats.exists(t => 
    t.turnsToImpact <= 2 && t.lossIfIgnoredCp >= 200
  ))
  
  /** Strategic threat TO US: 3-5 moves, >= 100cp loss (structure, passer, file) */
  def strategicThreatToUs: Boolean = threatsToUs.exists(_.threats.exists(t =>
    t.turnsToImpact <= 5 && t.lossIfIgnoredCp >= 100 && !tacticalThreatToUs
  ))
  
  /** Tactical threat TO THEM: 1-2 moves, >= 200cp (we have forcing attack) */
  def tacticalThreatToThem: Boolean = threatsToThem.exists(_.threats.exists(t =>
    t.turnsToImpact <= 2 && t.lossIfIgnoredCp >= 200
  ))
  
  /** Strategic threat TO THEM: 3-5 moves, >= 100cp (we have positional pressure) */
  def strategicThreatToThem: Boolean = threatsToThem.exists(_.threats.exists(t =>
    t.turnsToImpact <= 5 && t.lossIfIgnoredCp >= 100 && !tacticalThreatToThem
  ))
  
  /** Max loss if we ignore threats TO US */
  def maxThreatLossToUs: Int = threatsToUs.map(_.threats.map(_.lossIfIgnoredCp).maxOption.getOrElse(0)).getOrElse(0)
  
  /** Max loss if opponent ignores our threats */
  def maxThreatLossToThem: Int = threatsToThem.map(_.threats.map(_.lossIfIgnoredCp).maxOption.getOrElse(0)).getOrElse(0)
  
  // Legacy helpers (use numeric versions above for new code)
  def underDefensivePressure: Boolean = tacticalThreatToUs || strategicThreatToUs
  def holdingAttackingThreats: Boolean = tacticalThreatToThem || strategicThreatToThem
  def simplificationReliefPossible: Boolean = underDefensivePressure
  def attackingOpportunityAtRisk: Boolean = holdingAttackingThreats
}

/**
 * Matches detected Motifs to high-level strategic Plans.
 * Uses scoring with phase gating and motif category weights.
 * Applies Plan Compatibility Matrix for conflict/synergy resolution.
 */
object PlanMatcher:

  /** Normalized output with primary/secondary/suppressed plans + compatibility trace */
  case class ActivePlans(
    primary: PlanMatch,
    secondary: Option[PlanMatch],
    suppressed: List[PlanMatch],
    allPlans: List[PlanMatch],            // Pre-compatibility list for debugging
    compatibilityEvents: List[CompatibilityEvent] = Nil  // A4: Compatibility trace
  )

  def matchPlans(
      motifs: List[Motif],
      ctx: IntegratedContext,
      side: Color
  ): PlanScoringResult =
    val allScorers = List(
      // Attack plans
      scoreKingsideAttack(motifs, ctx, side),
      scoreQueensideAttack(motifs, ctx, side),
      scorePawnStorm(motifs, ctx, side),
      scorePerpetualCheck(motifs, ctx, side),
      scoreDirectMate(motifs, ctx, side),
      // Positional plans
      scoreCentralControl(motifs, ctx, side),
      scorePieceActivation(motifs, ctx, side),
      scoreRookActivation(motifs, ctx, side),
      // Transition
      scoreSimplification(motifs, ctx, side),
      scoreQueenTrade(motifs, ctx, side),
      // Structural plans
      scorePassedPawnPush(motifs, ctx, side),
      scoreBlockade(motifs, ctx, side),
      scorePawnChain(motifs, ctx, side),
      scoreMinorityAttack(motifs, ctx, side),
      // Endgame plans
      scoreKingActivation(motifs, ctx, side),
      scorePromotion(motifs, ctx, side),
      scoreZugzwang(motifs, ctx, side),
      // Defensive plans
      scoreDefensiveConsolidation(motifs, ctx, side),
      scoreProphylaxis(motifs, ctx, side),
      // Misc
      scoreSpaceAdvantage(motifs, ctx, side),
      scoreSacrifice(motifs, ctx, side)
    )

    val rawPlans = allScorers.flatten
    val (compatiblePlans, events) = applyCompatWithEvents(rawPlans, ctx, side)
    val sortedPlans = compatiblePlans.sortBy(-_.score)
    val confidence = sortedPlans.headOption.map(_.score).getOrElse(0.0)

    PlanScoringResult(
      topPlans = sortedPlans.take(5),
      confidence = confidence,
      phase = ctx.phase,
      compatibilityEvents = events
    )

  /**
   * Convert sorted plan list to ActivePlans structure.
   * Used by TransitionAnalyzer for plan sequence tracking.
   */
  def toActivePlans(sortedPlans: List[PlanMatch], events: List[CompatibilityEvent] = Nil): ActivePlans = {
    val primary = sortedPlans.headOption.getOrElse(
      PlanMatch(Plan.CentralControl(Color.White), 0.0, Nil)
    )
    val secondary = sortedPlans.lift(1)
    val suppressThreshold = primary.score * 0.5
    val suppressed = sortedPlans.drop(2).filter(_.score < suppressThreshold)
    
    ActivePlans(
      primary = primary,
      secondary = secondary,
      suppressed = suppressed,
      allPlans = sortedPlans,
      compatibilityEvents = events
    )
  }

  // ============================================================
  // PLAN COMPATIBILITY MATRIX
  // ============================================================
  
  /**
   * A4: Apply compatibility with event tracking.
   * Returns (adjusted plans, events log).
   */
  def applyCompatWithEvents(
    plans: List[PlanMatch], 
    ctx: IntegratedContext, 
    side: Color
  ): (List[PlanMatch], List[CompatibilityEvent]) = {
    import scala.collection.mutable.ListBuffer
    val events = ListBuffer[CompatibilityEvent]()
    
    // Build mutable map for tracking: planName -> current score
    val scoreMap = scala.collection.mutable.Map[String, Double]()
    var result = plans.map { p =>
      scoreMap(p.plan.name) = p.score
      p.copy()
    }
    
    def logAdjustment(plan: PlanMatch, newScore: Double, reason: String): Unit = {
      val origScore = scoreMap(plan.plan.name)
      val delta = newScore - origScore
      if (delta != 0.0) {
        val eventType = if (newScore <= 0) "removed" else if (delta > 0) "boosted" else "downweight"
        events += CompatibilityEvent(plan.plan.name, origScore, newScore, delta, reason, eventType)
        scoreMap(plan.plan.name) = newScore
      }
    }
    
    // --- CONFLICT RULES (⟂ = mutual suppression) ---
    
    // Attack ⟂ Simplification
    if (ctx.tacticalThreatToThem) {
      result = result.map {
        case p if p.plan.isInstanceOf[Plan.Simplification] =>
          val newScore = p.score - 0.5
          logAdjustment(p, newScore, "conflict: attack ⟂ simplification")
          p.copy(score = newScore)
        case p if p.plan.isInstanceOf[Plan.QueenTrade] =>
          val newScore = p.score - 0.4
          logAdjustment(p, newScore, "conflict: attack ⟂ queen trade")
          p.copy(score = newScore)
        case p => p
      }
    }
    
    // Prophylaxis ⟂ Sacrifice
    if (ctx.tacticalThreatToUs) {
      result = result.map {
        case p if p.plan.isInstanceOf[Plan.Sacrifice] =>
          val newScore = p.score - 0.3
          logAdjustment(p, newScore, "conflict: threat ⟂ sacrifice")
          p.copy(score = newScore)
        case p => p
      }
    }
    
    // Defensive ⟂ Attack
    val hasDefensive = result.exists(p => p.plan.isInstanceOf[Plan.DefensiveConsolidation] || p.plan.isInstanceOf[Plan.Prophylaxis])
    val hasAttack = result.exists(p => p.plan.isInstanceOf[Plan.KingsideAttack] || p.plan.isInstanceOf[Plan.QueensideAttack])
    if (hasDefensive && hasAttack) {
      val defScore = result.filter(p => p.plan.isInstanceOf[Plan.DefensiveConsolidation] || p.plan.isInstanceOf[Plan.Prophylaxis]).map(_.score).maxOption.getOrElse(0.0)
      val attScore = result.filter(p => p.plan.isInstanceOf[Plan.KingsideAttack] || p.plan.isInstanceOf[Plan.QueensideAttack]).map(_.score).maxOption.getOrElse(0.0)
      if (defScore > attScore) {
        result = result.map {
          case p if p.plan.isInstanceOf[Plan.KingsideAttack] || p.plan.isInstanceOf[Plan.QueensideAttack] =>
            val newScore = p.score * 0.5
            logAdjustment(p, newScore, "conflict: defense > attack")
            p.copy(score = newScore)
          case p => p
        }
      } else {
        result = result.map {
          case p if p.plan.isInstanceOf[Plan.DefensiveConsolidation] || p.plan.isInstanceOf[Plan.Prophylaxis] =>
            val newScore = p.score * 0.5
            logAdjustment(p, newScore, "conflict: attack > defense")
            p.copy(score = newScore)
          case p => p
        }
      }
    }
    
    // --- SYNERGY RULES (↔ = mutual boost) ---
    
    val hasBlockade = result.exists(_.plan.isInstanceOf[Plan.Blockade])
    val hasSimplification = result.exists(_.plan.isInstanceOf[Plan.Simplification])
    if (hasBlockade && hasSimplification && ctx.phase == "endgame" && ctx.evalFor(side) > 100) {
      result = result.map {
        case p if p.plan.isInstanceOf[Plan.Blockade] =>
          val newScore = p.score + 0.2
          logAdjustment(p, newScore, "synergy: blockade ↔ simplification (endgame)")
          p.copy(score = newScore)
        case p if p.plan.isInstanceOf[Plan.Simplification] =>
          val newScore = p.score + 0.2
          logAdjustment(p, newScore, "synergy: blockade ↔ simplification (endgame)")
          p.copy(score = newScore)
        case p => p
      }
    }
    
    val hasSpace = result.exists(_.plan.isInstanceOf[Plan.SpaceAdvantage])
    val hasProphylaxis = result.exists(_.plan.isInstanceOf[Plan.Prophylaxis])
    if (hasSpace && hasProphylaxis && ctx.phase == "middlegame") {
      result = result.map {
        case p if p.plan.isInstanceOf[Plan.Prophylaxis] =>
          val newScore = p.score + 0.1
          logAdjustment(p, newScore, "synergy: space ↔ prophylaxis")
          p.copy(score = newScore)
        case p => p
      }
    }
    
    // Filter and log removed plans
    val (kept, removed) = result.partition(_.score > 0)
    removed.foreach { p =>
      // Already logged as "removed" when score went <= 0
    }
    
    (kept, events.toList)
  }

  // Legacy wrapper for backwards compatibility
  private def applyCompatibility(plans: List[PlanMatch], ctx: IntegratedContext, side: Color): List[PlanMatch] = {
    applyCompatWithEvents(plans, ctx, side)._1
  }

  // ============================================================
  // ATTACK PLAN SCORING
  // ============================================================

  private def scoreKingsideAttack(
      motifs: List[Motif],
      ctx: IntegratedContext,
      side: Color
  ): Option[PlanMatch] =
    val relevantMotifs = motifs.filter:
      case m: PawnAdvance   => m.file.isKingside
      case m: RookLift      => m.file.isKingside
      case m: Motif.Check   => m.targetSquare.file.isKingside
      case _: PieceLift     => true
      case m: Fork          => m.targets.contains(King)
      case _                => false

    if (relevantMotifs.isEmpty) None
    else
      val baseScore = relevantMotifs.size * 0.2
      val phaseMultiplier = ctx.phase match
        case "opening"    => 0.7
        case "middlegame" => 1.4
        case "endgame"    => 0.5
        case _            => 1.0

      val evidence = relevantMotifs.take(3).map: m =>
        EvidenceAtom(m, 0.2, s"Kingside pressure: ${m.move.getOrElse("")}")

      Some(PlanMatch(Plan.KingsideAttack(side), baseScore * phaseMultiplier, evidence))

  private def scoreQueensideAttack(
      motifs: List[Motif],
      ctx: IntegratedContext,
      side: Color
  ): Option[PlanMatch] =
    val relevantMotifs = motifs.filter:
      case m: PawnAdvance => m.file.isQueenside
      case m: PawnBreak   => m.targetFile.isQueenside
      case m: RookLift    => m.file.isQueenside
      case _              => false

    if (relevantMotifs.isEmpty) None
    else
      val baseScore = relevantMotifs.size * 0.25
      val phaseMultiplier = ctx.phase match
        case "middlegame" => 1.2
        case "endgame"    => 0.8
        case _            => 1.0

      val evidence = relevantMotifs.take(3).map: m =>
        EvidenceAtom(m, 0.25, s"Queenside expansion: ${m.move.getOrElse("")}")

      Some(PlanMatch(Plan.QueensideAttack(side), baseScore * phaseMultiplier, evidence))

  private def scorePawnStorm(
      motifs: List[Motif],
      ctx: IntegratedContext,
      side: Color
  ): Option[PlanMatch] =
    val pawnAdvances = motifs.collect { case m: PawnAdvance => m }
    val kingsidePawns = pawnAdvances.filter(_.file.isKingside)
    val queensidePawns = pawnAdvances.filter(_.file.isQueenside)

    val (stormSide, stormPawns) = 
      if (kingsidePawns.size >= 2) ("kingside", kingsidePawns)
      else if (queensidePawns.size >= 2) ("queenside", queensidePawns)
      else return None

    // Only in middlegame/endgame
    if (ctx.phase == "opening") return None

    val evidence = stormPawns.take(3).map: m =>
      EvidenceAtom(m, 0.3, s"Pawn storm: ${m.move.getOrElse("")}")

    Some(PlanMatch(Plan.PawnStorm(side, stormSide), stormPawns.size * 0.3, evidence))

  private def scorePerpetualCheck(
      motifs: List[Motif],
      ctx: IntegratedContext,
      side: Color
  ): Option[PlanMatch] =
    val checks = motifs.collect { case m: Motif.Check => m }
    // Need 3+ checks and eval near 0 (drawish) to suggest perpetual
    if (checks.size >= 3 && ctx.evalCp.abs < 50)
      val evidence = checks.take(3).map: m =>
        EvidenceAtom(m, 1.0, s"Repetitive check: ${m.move.getOrElse("")}")
      Some(PlanMatch(Plan.PerpetualCheck(side), 1.0, evidence))
    else None

  private def scoreDirectMate(
      motifs: List[Motif],
      ctx: IntegratedContext,
      side: Color
  ): Option[PlanMatch] =
    val checks = motifs.collect { case m: Motif.Check => m }
    // If eval is very high and multiple checks, likely mating sequence
    if (checks.nonEmpty && ctx.evalFor(side) > 500)
      val evidence = checks.take(3).map: m =>
        EvidenceAtom(m, 1.0, s"Mating attack: ${m.move.getOrElse("")}")
      Some(PlanMatch(Plan.DirectMate(side), 1.5, evidence))
    else None

  // ============================================================
  // POSITIONAL PLAN SCORING
  // ============================================================

  private def scoreCentralControl(
      motifs: List[Motif],
      ctx: IntegratedContext,
      side: Color
  ): Option[PlanMatch] =
    val relevantMotifs = motifs.filter:
      case m: PawnAdvance    => m.file.isCentral
      case _: Centralization => true
      case _: Outpost        => true
      case _                 => false

    if (relevantMotifs.isEmpty) None
    else
      val baseScore = relevantMotifs.size * 0.3
      val phaseMultiplier = ctx.phase match
        case "opening" => 1.5
        case _         => 0.8

      val evidence = relevantMotifs.take(3).map: m =>
        EvidenceAtom(m, 0.3, s"Central influence: ${m.move.getOrElse("")}")

      Some(PlanMatch(Plan.CentralControl(side), baseScore * phaseMultiplier, evidence))

  private def scorePieceActivation(
      motifs: List[Motif],
      _ctx: IntegratedContext,
      side: Color
  ): Option[PlanMatch] =
    val relevantMotifs = motifs.filter:
      case _: Fianchetto     => true
      case _: Outpost        => true
      case _: Centralization => true
      case _: PieceLift      => true
      case _                 => false

    if (relevantMotifs.size < 2) None
    else
      val baseScore = relevantMotifs.size * 0.25
      val evidence = relevantMotifs.take(3).map: m =>
        EvidenceAtom(m, 0.25, s"Piece development: ${m.move.getOrElse("")}")

      Some(PlanMatch(Plan.PieceActivation(side), baseScore, evidence))

  private def scoreRookActivation(
      motifs: List[Motif],
      _ctx: IntegratedContext,
      side: Color
  ): Option[PlanMatch] =
    val rookMotifs = motifs.collect { 
      case m: RookLift => m 
      case m: DoubledPieces if m.role == Rook => m
    }

    if (rookMotifs.isEmpty) None
    else
      val evidence = rookMotifs.take(2).map: m =>
        EvidenceAtom(m, 0.4, s"Rook activation: ${m.move.getOrElse("")}")

      Some(PlanMatch(Plan.RookActivation(side), rookMotifs.size * 0.4, evidence))

  // ============================================================
  // STRUCTURAL PLAN SCORING
  // ============================================================

  private def scorePassedPawnPush(
      motifs: List[Motif],
      ctx: IntegratedContext,
      side: Color
  ): Option[PlanMatch] =
    val pawnMotifs = motifs.collect { 
      case m: PawnAdvance if m.relativeTo >= 5 => m
      case m: PassedPawnPush => m
      case m: PawnPromotion => m
    }

    val isAdvanced = pawnMotifs.exists {
      case m: PassedPawnPush => Motif.relativeRank(m.toRank, side) >= 6
      case m: PawnAdvance => m.relativeTo >= 6
      case m: PawnPromotion => true
      case _ => false
    }

    if (pawnMotifs.isEmpty || (ctx.phase != "endgame" && !isAdvanced)) None
    else
      val evidence = pawnMotifs.take(2).map: m =>
        EvidenceAtom(m, 0.5, s"Passed pawn advance: ${m.move.getOrElse("")}")

      Some(PlanMatch(Plan.PassedPawnPush(side), 0.8 + pawnMotifs.size * 0.1, evidence))

  // ============================================================
  // ENDGAME PLAN SCORING
  // ============================================================

  private def scoreKingActivation(
      motifs: List[Motif],
      ctx: IntegratedContext,
      side: Color
  ): Option[PlanMatch] =
    val kingMoves = motifs.collect:
      case m: KingStep if m.stepType == KingStepType.Activation => m

    if (ctx.phase == "endgame" && kingMoves.nonEmpty)
      val evidence = kingMoves.take(2).map: m =>
        EvidenceAtom(m, 0.4, s"King march: ${m.move.getOrElse("")}")
      Some(PlanMatch(Plan.KingActivation(side), 0.7 + kingMoves.size * 0.1, evidence))
    else None

  private def scorePromotion(
      motifs: List[Motif],
      _ctx: IntegratedContext,
      side: Color
  ): Option[PlanMatch] =
    val promoMotifs = motifs.collect { case m: PawnPromotion => m }

    if (promoMotifs.nonEmpty)
      val evidence = promoMotifs.map: m =>
        EvidenceAtom(m, 1.0, s"Promotion: ${m.move.getOrElse("")}")
      Some(PlanMatch(Plan.Promotion(side), 1.2, evidence))
    else None

  // ============================================================
  // DEFENSIVE PLAN SCORING
  // ============================================================

  private def scoreDefensiveConsolidation(
      motifs: List[Motif],
      _ctx: IntegratedContext,
      side: Color
  ): Option[PlanMatch] =
    val defensiveMotifs = motifs.filter:
      case m: KingStep if m.stepType == KingStepType.ToCorner => true
      case m: KingStep if m.stepType == KingStepType.Evasion => true
      case _: Castling => true
      case m: Capture if m.captureType == CaptureType.Exchange => true
      case _ => false

    if (defensiveMotifs.size < 2) None
    else
      val evidence = defensiveMotifs.take(2).map: m =>
        EvidenceAtom(m, 0.3, s"Defensive move: ${m.move.getOrElse("")}")

      Some(PlanMatch(Plan.DefensiveConsolidation(side), defensiveMotifs.size * 0.3, evidence))

  private def scoreProphylaxis(
      motifs: List[Motif],
      ctx: IntegratedContext,
      side: Color
  ): Option[PlanMatch] =
     // DESIGN: Prophylaxis uses threatsToUs (threats opponent makes TO US)
     // because it represents defensive preparation against incoming threats.
     val needProphylaxis = ctx.threatsToUs.exists(_.defense.prophylaxisNeeded)
     
     val defensiveMoves = motifs.collect {
       case m: KingStep if m.stepType == KingStepType.ToCorner => m
       case m: KingStep if m.stepType == KingStepType.Evasion => m
       case m: RookLift => m 
     }
     
     if (needProphylaxis && defensiveMoves.nonEmpty)
       val topThreat = ctx.threatsToUs.flatMap(_.threats.sortBy(_.lossIfIgnoredCp).reverse.headOption)
       val threatDesc = topThreat.map(t => s"neutralize ${t.kind.toString.toLowerCase} threat within ${t.turnsToImpact} turns").getOrElse("neutralize upcoming threat")
       val evidence = defensiveMoves.map(m => EvidenceAtom(m, 0.6, s"Prophylactic maneuver to $threatDesc"))
       Some(PlanMatch(Plan.Prophylaxis(side, "Attack"), 0.8, evidence))
     else if (ctx.evalFor(side) < -50 && defensiveMoves.nonEmpty)
       // Fallback for general defensive posture
       val evidence = defensiveMoves.map(m => EvidenceAtom(m, 0.4, "Defensive repositioning"))
       Some(PlanMatch(Plan.Prophylaxis(side, "Attack"), 0.5, evidence))
     else None

  // ============================================================
  // TRANSITION PLAN SCORING
  // ============================================================

  private def scoreSimplification(
      motifs: List[Motif],
      ctx: IntegratedContext,
      side: Color
  ): Option[PlanMatch] =
    val simplifyBias = ctx.classification.exists(_.simplifyBias.shouldSimplify)
    val winning = ctx.evalFor(side) > 200 // +2.0 pawn advantage
    
    // CHESS-CORRECT GATING:
    // - BLOCK if we hold attacking threats (simplification wastes attack)
    // - ALLOW (even encourage) if we're under defensive pressure (simplification relieves pressure)
    val attackOpportunityAtRisk = ctx.attackingOpportunityAtRisk
    val reliefBonus = if (ctx.simplificationReliefPossible) 0.3 else 0.0
    
    val exchanges = motifs.collect { case m: Capture if m.captureType == CaptureType.Exchange => m }
    
    if ((simplifyBias || winning) && !attackOpportunityAtRisk && exchanges.nonEmpty)
      val evidence = exchanges.map(m => EvidenceAtom(m, 0.8, s"Simplifying exchange to capitalize on +${ctx.evalFor(side)}cp lead"))
      Some(PlanMatch(Plan.Simplification(side), 1.0 + exchanges.size * 0.2 + reliefBonus, evidence))
    else None

  private def scoreQueenTrade(
      motifs: List[Motif],
      ctx: IntegratedContext,
      side: Color
  ): Option[PlanMatch] =
    val queenTrades = motifs.collect { case m: Capture if m.captured == Queen => m }
    val drawish = ctx.classification.exists(_.drawBias.tendsToDraw)
    val winning = ctx.evalFor(side) > 150 
    
    // CHESS-CORRECT: Don't trade queens if we hold attacking threats (mate threats, etc.)
    val attackOpportunityAtRisk = ctx.attackingOpportunityAtRisk
    
    if (queenTrades.nonEmpty && (winning || drawish) && !attackOpportunityAtRisk)
      val reason = if (winning) "Simplification (Winning)" else "Transition to Draw"
      val evidence = queenTrades.map(m => EvidenceAtom(m, 1.0, s"Queen exchange: $reason"))
      Some(PlanMatch(Plan.QueenTrade(side), 1.2, evidence))
    else None

  // ============================================================
  // ADDITIONAL SCORERS (Blockade, Minority, Space, Zugzwang, Sacrifice)
  // ============================================================

  private def scoreBlockade(
      motifs: List[Motif],
      ctx: IntegratedContext,
      side: Color
  ): Option[PlanMatch] =
    // Blockade is a plan for the DEFENDER.
    // We suggest it if the OPPONENT has a passed pawn that is blockaded.
    val isOpponentPasserBlocked = ctx.opponentPawnAnalysis.exists((a: PawnPlayAnalysis) => a.passerBlockade)
    
    if (isOpponentPasserBlocked)
      val pAnalysis = ctx.opponentPawnAnalysis
      val blockadeSq = pAnalysis.flatMap(_.blockadeSquare).map(_.key).getOrElse("passer")
      val blockadePiece = pAnalysis.flatMap(_.blockadeRole).map(_.name).getOrElse("piece")
      
      // Motif-based evidence (high-quality narrative)
      val motifEvidence = motifs.collect {
        case m: Centralization => EvidenceAtom(m, 0.7, s"Blockading ${m.piece.name} on ${m.square.key} (Centralized)")
        case m: Outpost => EvidenceAtom(m, 0.8, s"Establishing outpost for ${m.piece.name} on ${m.square.key} in front of passer")
      }
      
      val blockadeSquareStr = pAnalysis.flatMap(_.blockadeSquare).map(_.key).getOrElse {
        motifs.collectFirst {
          case m: Centralization => m.square.key
          case m: Outpost => m.square.key
        }.getOrElse("passer")
      }
      
      // Phase 3 data-based evidence (specific)
      val phase3Evidence = if (pAnalysis.exists(a => a.blockadeSquare.isDefined && a.blockadeRole.isDefined)) {
        List(EvidenceAtom(Zugzwang(side, 0, None), 0.5, s"Passed pawn successfully blockaded by $blockadePiece on $blockadeSq"))
      } else {
        // Generic evidence when Phase 3 data is missing
        List(EvidenceAtom(Zugzwang(side, 0, None), 0.3, "Passed pawn advance halted"))
      }
      
      val finalEvidence = (motifEvidence ++ phase3Evidence).distinct
      
      // EVIDENCE-BASED SCORE: Lower score without detailed evidence (defensive design)
      val hasDetailedEvidence = pAnalysis.exists(a => a.blockadeSquare.isDefined && a.blockadeRole.isDefined)
      val baseScore = if (hasDetailedEvidence) 0.9 else 0.7
      
      Some(PlanMatch(Plan.Blockade(side, blockadeSquareStr), baseScore, finalEvidence))
    else None

  private def scoreMinorityAttack(
      motifs: List[Motif],
      ctx: IntegratedContext,
      side: Color
  ): Option[PlanMatch] =
    if (ctx.pawnAnalysis.exists((a: PawnPlayAnalysis) => a.minorityAttack || a.counterBreak))
       val evidence = motifs.collect { 
         case m: PawnAdvance if m.file.isQueenside => 
           EvidenceAtom(m, 0.6, s"Queenside pawn push on ${m.file.toString}-file (Minority Attack strategy)")
       }
       Some(PlanMatch(Plan.MinorityAttack(side), 0.85, evidence))
    else None

  private def scorePawnChain(motifs: List[Motif], ctx: IntegratedContext, side: Color): Option[PlanMatch] =
    val chainMotifs = motifs.collect { case m: PawnChain => m }
    if (chainMotifs.nonEmpty)
      val evidence = chainMotifs.map(m => EvidenceAtom(m, 0.4, "Pawn chain structure"))
      Some(PlanMatch(Plan.PawnChain(side), 0.5 + chainMotifs.size * 0.1, evidence))
    else None

  private def scoreSpaceAdvantage(motifs: List[Motif], ctx: IntegratedContext, side: Color): Option[PlanMatch] =
    val spaceMotifs = motifs.collect { case m: SpaceAdvantage => m }
    if (spaceMotifs.nonEmpty)
      val evidence = spaceMotifs.map(m => EvidenceAtom(m, 0.7, "Space advantage"))
      Some(PlanMatch(Plan.SpaceAdvantage(side), 0.7, evidence))
    else None
    
  private def scoreZugzwang(motifs: List[Motif], ctx: IntegratedContext, side: Color): Option[PlanMatch] =
    val zugMotifs = motifs.collect { case m: Zugzwang => m }
    if (zugMotifs.nonEmpty && ctx.phase == "endgame")
       val evidence = zugMotifs.map(m => EvidenceAtom(m, 1.0, "Zugzwang pattern"))
       Some(PlanMatch(Plan.Zugzwang(side), 1.3, evidence))
    else None

  private def scoreSacrifice(motifs: List[Motif], ctx: IntegratedContext, side: Color): Option[PlanMatch] =
    val sacrifices = motifs.collect {
      case m: Capture if m.captureType == CaptureType.Sacrifice || m.captureType == CaptureType.ExchangeSacrifice => m
    }
    
    if (sacrifices.nonEmpty)
      val eval = ctx.evalFor(side)
      // Suppress sacrifice in clearly losing positions
      if (eval < -300) then return None
      
      // Eval-based scaling: more aggressive in winning positions
      val evalBonus = if (eval >= 200) 0.3 else if (eval > 0) 0.1 else 0.0
      
      val riskProfile = ctx.classification.map(_.riskProfile)
      val highRiskBonus = if (riskProfile.exists(_.riskLevel == RiskLevel.High)) 0.3 else 0.0
      // NOTE: kingExposureSum measures OPPONENT's king exposure (favorable for attacking sacrifice)
      // If this were OUR king exposure, it would be a liability, not a bonus
      val opponentKingExposedBonus = if (riskProfile.exists(_.kingExposureSum > 5)) 0.2 else 0.0
      
      // Base: attacking 1.0, defensive 0.6
      val isAttacking = eval > -150
      val baseScore = (if (isAttacking) 1.0 else 0.6) + evalBonus + highRiskBonus + opponentKingExposedBonus
      
      val evidence = sacrifices.map(m => EvidenceAtom(m, 1.2, s"Tactical ${m.captureType.toString} to achieve strategic objective"))
      val sacPiece = sacrifices.headOption.map(_.piece.name).getOrElse("Piece")
      Some(PlanMatch(Plan.Sacrifice(side, sacPiece), baseScore.min(2.0), evidence))
    else None

  // ============================================================
  // FILE CLASSIFICATION HELPERS
  // ============================================================

  extension (f: File)
    def isKingside: Boolean = f == File.F || f == File.G || f == File.H
    def isQueenside: Boolean = f == File.A || f == File.B || f == File.C
    def isCentral: Boolean = f == File.D || f == File.E
