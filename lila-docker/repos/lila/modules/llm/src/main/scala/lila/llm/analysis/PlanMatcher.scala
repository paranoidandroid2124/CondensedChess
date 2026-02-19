package lila.llm.analysis

import chess.*
import lila.llm.model.*
import lila.llm.model.Motif.*
import lila.llm.analysis.L3.{PawnPlayAnalysis, PositionClassification, ThreatAnalysis, RiskLevel}
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
    features: Option[PositionFeatures] = None,
    initialPos: Option[Position] = None
) {
  def evalFor(color: Color): Int = if (color == White) evalCp else -evalCp

  /** Derive phase from classification or fallback to "middlegame" */
  def phase: String = phaseEnum match {
    case lila.llm.analysis.L3.GamePhaseType.Opening    => "opening"
    case lila.llm.analysis.L3.GamePhaseType.Middlegame => "middlegame"
    case lila.llm.analysis.L3.GamePhaseType.Endgame    => "endgame"
  }

  def phaseEnum: lila.llm.analysis.L3.GamePhaseType = classification
    .map(_.gamePhase.phaseType)
    .getOrElse(lila.llm.analysis.L3.GamePhaseType.Middlegame)
  // NUMERIC-BASED THREAT ASSESSMENT (replaces label-based)
  
  /** Tactical threat TO US: 1-2 moves, >= 200cp loss (mate, forced tactic) */
  def tacticalThreatToUs: Boolean = threatsToUs.exists(_.threats.exists(t => 
    t.turnsToImpact <= 2 && t.lossIfIgnoredCp >= 200
  ))
  
  /** Strategic threat TO US: 3-5 moves, >= 100cp loss (structure, passer, file) */
  def strategicThreatToUs: Boolean = threatsToUs.exists(_.threats.exists(t =>
    t.turnsToImpact <= 5 && t.lossIfIgnoredCp >= Thresholds.SIGNIFICANT_THREAT_CP && !tacticalThreatToUs
  ))
  
  /** Tactical threat TO THEM: 1-2 moves, >= 200cp (we have forcing attack) */
  def tacticalThreatToThem: Boolean = threatsToThem.exists(_.threats.exists(t =>
    t.turnsToImpact <= 2 && t.lossIfIgnoredCp >= 200
  ))
  
  /** Strategic threat TO THEM: 3-5 moves, >= 100cp (we have positional pressure) */
  def strategicThreatToThem: Boolean = threatsToThem.exists(_.threats.exists(t =>
    t.turnsToImpact <= 5 && t.lossIfIgnoredCp >= Thresholds.SIGNIFICANT_THREAT_CP && !tacticalThreatToThem
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
    compatibilityEvents: List[CompatibilityEvent] = Nil // Compatibility trace
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
      scoreSacrifice(motifs, ctx, side),
      scoreMinorPieceManeuver(motifs, ctx, side),
      scoreFileControl(motifs, ctx, side)
    )

    val rawPlans = allScorers.flatten
    val (compatiblePlans, events) = applyCompatWithEvents(rawPlans, ctx, side)
    val sortedPlans = compatiblePlans.sortBy(-_.score)

    // Fail-closed fallback: always provide at least one plan so renderers and callers
    // don't need to handle "no plan" as a special case.
    val plans =
      if (sortedPlans.nonEmpty) sortedPlans
      else
        val fallbackPlan =
          ctx.phaseEnum match
            case lila.llm.analysis.L3.GamePhaseType.Opening => Plan.PieceActivation(side)
            case lila.llm.analysis.L3.GamePhaseType.Endgame => Plan.KingActivation(side)
            case _                                          => Plan.CentralControl(side)
        List(PlanMatch(fallbackPlan, 0.25, Nil, Nil, Nil, Nil))

    val confidence = plans.headOption.map(_.score).getOrElse(0.0)

    PlanScoringResult(
      topPlans = plans.take(5),
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
    if (hasBlockade && hasSimplification && ctx.phaseEnum == lila.llm.analysis.L3.GamePhaseType.Endgame && ctx.evalFor(side) > 100) {
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
    if (hasSpace && hasProphylaxis && ctx.phaseEnum == lila.llm.analysis.L3.GamePhaseType.Middlegame) {
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

    // P1 FIX: Flank attacks require more evidence in the opening
    if (relevantMotifs.isEmpty || (ctx.phase == "opening" && relevantMotifs.size < 2)) None
    else
      val baseScore = relevantMotifs.size * 0.2
      val phaseMultiplier = ctx.phaseEnum match
        case lila.llm.analysis.L3.GamePhaseType.Opening    => 0.7
        case lila.llm.analysis.L3.GamePhaseType.Middlegame => 1.4
        case lila.llm.analysis.L3.GamePhaseType.Endgame    => 0.5

      val evidence = relevantMotifs.take(3).map: m =>
        EvidenceAtom(m, 0.2, s"Kingside pressure: ${m.move.getOrElse("")}")

      val supports = scala.collection.mutable.ListBuffer[String]()
      val blockers = scala.collection.mutable.ListBuffer[String]()
      val missing = scala.collection.mutable.ListBuffer[String]()

      ctx.features.foreach { f =>
        val enemyKing = if (side == White) f.kingSafety.blackKingExposedFiles else f.kingSafety.whiteKingExposedFiles
        val ourAttackers = if (side == White) f.kingSafety.whiteAttackersCount else f.kingSafety.blackAttackersCount
        val enemyShield = if (side == White) f.kingSafety.blackKingShield else f.kingSafety.whiteKingShield

        if (enemyKing > 0) supports += "Opponent king exposed"
        if (ourAttackers >= 2) supports += s"$ourAttackers pieces near king zone"
        
        if (enemyShield >= 2) blockers += "Strong defensive pawn shield"
        if (f.centralSpace.pawnTensionCount > 0) blockers += "Unstable center"
        
        if (ourAttackers < 2) missing += "Need more pieces in the attack zone"
        if (f.centralSpace.lockedCenter == false && f.centralSpace.openCenter == false) missing += "Center should be stabilized"
      }

      Some(PlanMatch(
        Plan.KingsideAttack(side), 
        baseScore * phaseMultiplier, 
        evidence,
        supports.toList,
        blockers.toList,
        missing.toList
      ))

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

    // P1 FIX: Flank attacks require more evidence in the opening
    if (relevantMotifs.isEmpty || (ctx.phaseEnum == lila.llm.analysis.L3.GamePhaseType.Opening && relevantMotifs.size < 2)) None
    else
      val baseScore = relevantMotifs.size * 0.25
      val phaseMultiplier = ctx.phaseEnum match
        case lila.llm.analysis.L3.GamePhaseType.Middlegame => 1.2
        case lila.llm.analysis.L3.GamePhaseType.Endgame    => 0.8
        case _                                             => 1.0

      val evidence = relevantMotifs.take(3).map: m =>
        EvidenceAtom(m, 0.25, s"Queenside expansion: ${m.move.getOrElse("")}")

      val supports = scala.collection.mutable.ListBuffer[String]()
      val blockers = scala.collection.mutable.ListBuffer[String]()
      val missing = scala.collection.mutable.ListBuffer[String]()

      ctx.features.foreach { f =>
        val ourPawns = if (side == White) f.pawns.whitePawnCount else f.pawns.blackPawnCount
        val mobility = if (side == White) f.activity.whitePseudoMobility else f.activity.blackPseudoMobility

        if (mobility > 30) supports += "High overall piece mobility"
        if (f.imbalance.whiteBishopPair && side == White) supports += "Bishop pair favors open play"

        if (f.centralSpace.lockedCenter) blockers += "Locked center limits flank activity"
        
        if (f.lineControl.openFilesCount == 0) missing += "Need to open files for major pieces"
      }

      Some(PlanMatch(
        Plan.QueensideAttack(side), 
        baseScore * phaseMultiplier, 
        evidence,
        supports.toList,
        blockers.toList,
        missing.toList
      ))

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
    if (ctx.phaseEnum == lila.llm.analysis.L3.GamePhaseType.Opening) return None

    val evidence = stormPawns.take(3).map: m =>
      EvidenceAtom(m, 0.3, s"Pawn storm: ${m.move.getOrElse("")}")

    val supports = scala.collection.mutable.ListBuffer[String]()
    val blockers = scala.collection.mutable.ListBuffer[String]()
    val missing = scala.collection.mutable.ListBuffer[String]()

    ctx.features.foreach { f =>
      val enemyKingSide = if (side == White) f.kingSafety.blackCastledSide == "short" else f.kingSafety.whiteCastledSide == "short"
      if (stormSide == "kingside" && enemyKingSide) supports += "Attacking castled king"
      
      if (f.centralSpace.pawnTensionCount > 0) blockers += "Counterplay in center exists"
      
      if (stormPawns.size < 3) missing += "Need more pawns to commit to the storm"
    }

    Some(PlanMatch(
      Plan.PawnStorm(side, stormSide), 
      stormPawns.size * 0.3, 
      evidence,
      supports.toList,
      blockers.toList,
      missing.toList
    ))

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
      
      val supports = List("Continuous checking sequence possible")
      val blockers = List("Opponent king may find a safe square")
      
      Some(PlanMatch(Plan.PerpetualCheck(side), 1.0, evidence, supports, blockers, Nil))
    else None

  private def scoreDirectMate(
      motifs: List[Motif],
      ctx: IntegratedContext,
      side: Color
  ): Option[PlanMatch] =
    val checks = motifs.collect { case m: Motif.Check => m }
    // P1 FIX: Detect if immediate mate is present in threats
    // We check threatsToThem because DirectMate is an attacking plan for 'side'
    val hasImmediateMate = ctx.threatsToThem.exists(_.threats.exists(t => 
      t.kind == lila.llm.analysis.L3.ThreatKind.Mate && t.turnsToImpact <= 2
    ))
    val evalForSide = ctx.evalFor(side)

    if (hasImmediateMate || (checks.nonEmpty && evalForSide > 500))
      val evidence = checks.take(3).map: m =>
        EvidenceAtom(m, 1.0, s"Mating attack: ${m.move.getOrElse("")}")

      val supports = scala.collection.mutable.ListBuffer[String]()
      val blockers = scala.collection.mutable.ListBuffer[String]()
      val missing = scala.collection.mutable.ListBuffer[String]()

      ctx.features.foreach { f =>
        val ourAttackers = if (side == White) f.kingSafety.whiteAttackersCount else f.kingSafety.blackAttackersCount
        val enemyEscapes = if (side == White) f.kingSafety.blackEscapeSquares else f.kingSafety.whiteEscapeSquares
        
        if (ourAttackers >= 3) supports += "Sufficient attacking force near king"
        if (enemyEscapes == 0) supports += "Opponent king has limited mobility"
        
        if (enemyEscapes > 2) blockers += "Opponent king has multiple escape routes"
        if (f.centralSpace.pawnTensionCount > 0) blockers += "Unresolved central tension may grant counterplay"

        if (ourAttackers < 3) missing += "Need to bring more pieces into the attack"
      }

      // Strong boost for forced mate
      val finalScore = if (hasImmediateMate) 5.0 else 1.5

      Some(PlanMatch(Plan.DirectMate(side), finalScore, evidence, supports.toList, blockers.toList, missing.toList))
    else None

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
      val phaseMultiplier = ctx.phaseEnum match
        case lila.llm.analysis.L3.GamePhaseType.Opening => 1.5
        case _                                          => 0.8

      val evidence = relevantMotifs.take(3).map: m =>
        EvidenceAtom(m, 0.3, s"Central influence: ${m.move.getOrElse("")}")

      val supports = scala.collection.mutable.ListBuffer[String]()
      val blockers = scala.collection.mutable.ListBuffer[String]()
      val missing = scala.collection.mutable.ListBuffer[String]()

      ctx.features.foreach { f =>
        val ourControl = if (side == White) f.centralSpace.whiteCenterControl else f.centralSpace.blackCenterControl
        val ourPawns = if (side == White) f.centralSpace.whiteCentralPawns else f.centralSpace.blackCentralPawns
        val ourMobility = if (side == White) f.activity.whiteMinorPieceMobility else f.activity.blackMinorPieceMobility
        val devLag = if (side == White) f.activity.whiteDevelopmentLag else f.activity.blackDevelopmentLag

        if (ourControl >= 3) supports += "Strong central square control"
        if (ourPawns >= 2) supports += "Solid central pawn structure"
        
        if (f.centralSpace.pawnTensionCount > 0) blockers += "Central tension requires resolution"
        if (f.centralSpace.lockedCenter) blockers += "Center is locked"
        
        if (devLag > 0) missing += "Completion of development needed"
        if (ourMobility < 5) missing += "Piece mobility in center is restricted"
      }

      Some(PlanMatch(
        Plan.CentralControl(side), 
        baseScore * phaseMultiplier, 
        evidence,
        supports.toList,
        blockers.toList,
        missing.toList
      ))

  private def scorePieceActivation(
      motifs: List[Motif],
      ctx: IntegratedContext,
      side: Color
  ): Option[PlanMatch] =
    val relevantMotifs = motifs.filter:
      case _: Fianchetto     => true
      case _: Outpost        => true
      case _: Centralization => true
      case _: PieceLift      => true
      case _: Pin            => true
      case _: Maneuver       => true
      case _                 => false

    val minEvidence = if ctx.phaseEnum == lila.llm.analysis.L3.GamePhaseType.Opening then 1 else 2
    if (relevantMotifs.size < minEvidence) None
    else
      val baseScore = relevantMotifs.size * 0.25
      val evidence = relevantMotifs.take(3).map: m =>
        EvidenceAtom(m, 0.25, s"Piece development: ${m.move.getOrElse("")}")

      val supports = scala.collection.mutable.ListBuffer[String]()
      val blockers = scala.collection.mutable.ListBuffer[String]()
      val missing = scala.collection.mutable.ListBuffer[String]()

      ctx.features.foreach { f =>
        val lag = if (side == White) f.activity.whiteDevelopmentLag else f.activity.blackDevelopmentLag
        val mobility = if (side == White) f.activity.whitePseudoMobility else f.activity.blackPseudoMobility
        
        if (mobility < 25) supports += "Significant room for activity improvement"
        if (lag > 0) supports += s"$lag pieces waiting to be developed"
        
        if (f.centralSpace.lockedCenter) blockers += "Closed center limits piece maneuvering"
        
        if (lag > 0) missing += "Back-rank pieces need to find active roles"
      }

      Some(PlanMatch(Plan.PieceActivation(side), baseScore, evidence, supports.toList, blockers.toList, missing.toList))

  private def scoreRookActivation(
      motifs: List[Motif],
      ctx: IntegratedContext,
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

      val supports = scala.collection.mutable.ListBuffer[String]()
      val blockers = scala.collection.mutable.ListBuffer[String]()
      val missing = scala.collection.mutable.ListBuffer[String]()

      ctx.features.foreach { f =>
        val ourRooks = if (side == White) f.imbalance.whiteRooks else f.imbalance.blackRooks
        val openFiles = f.lineControl.openFilesCount
        
        if (openFiles > 0) supports += s"$openFiles open files available for rooks"
        if (ourRooks >= 2) supports += "Opportunity for rook doubling"
        
        if (openFiles == 0) blockers += "Lack of open files for rook activity"
        
        if (openFiles == 0) missing += "Need to open files via pawn breaks"
      }

      Some(PlanMatch(Plan.RookActivation(side), rookMotifs.size * 0.4, evidence, supports.toList, blockers.toList, missing.toList))

  private def scorePassedPawnPush(
      motifs: List[Motif],
      ctx: IntegratedContext,
      side: Color
  ): Option[PlanMatch] =
    // Filter by side color to avoid counting opponent's pawn advances
    val pawnMotifs = motifs.collect { 
      case m: PawnAdvance if m.color == side && m.relativeTo >= 5 => m
      case m: PassedPawnPush if m.color == side => m
      case m: PawnPromotion if m.color == side => m
    }

    val isAdvanced = pawnMotifs.exists {
      case m: PassedPawnPush => Motif.relativeRank(m.toRank, side) >= 6
      case m: PawnAdvance => m.relativeTo >= 6
      case m: PawnPromotion => true
      case _ => false
    }

    if (pawnMotifs.isEmpty || (ctx.phaseEnum != lila.llm.analysis.L3.GamePhaseType.Endgame && !isAdvanced)) None
    else
      val evidence = pawnMotifs.take(2).map: m =>
        EvidenceAtom(m, 0.5, s"Passed pawn advance: ${m.move.getOrElse("")}")

      val supports = scala.collection.mutable.ListBuffer[String]()
      val blockers = scala.collection.mutable.ListBuffer[String]()
      val missing = scala.collection.mutable.ListBuffer[String]()

      ctx.features.foreach { f =>
        val ourPassers = if (side == White) f.pawns.whitePassedPawns else f.pawns.blackPassedPawns
        val ourPasserRank = if (side == White) f.pawns.whitePassedPawnRank else f.pawns.blackPassedPawnRank
        val ourRookOn7th = if (side == White) f.lineControl.whiteRookOn7th else f.lineControl.blackRookOn7th

        if (ourPassers > 0) supports += s"$ourPassers passed pawns"
        if (ourPasserRank >= 6) supports += "Highly advanced passed pawn"
        if (ourRookOn7th) supports += "Rook on 7th rank supports advance"

        if (ctx.pawnAnalysis.exists(_.blockadeSquare.isDefined)) blockers += "Passed pawn is currently blockaded"
        
        if (ctx.phaseEnum != lila.llm.analysis.L3.GamePhaseType.Endgame) missing += "Plan is most effective in endgame"
        if (ourPassers == 0) missing += "Need to create a passed pawn first"
        if (f.kingSafety.whiteCastledSide == "none" && side == White) missing += "King activation needed"
      }
      
      // Additional gating: suppress if no passers and no advanced pawns
      val ourPassers = ctx.features.map(f => if (side == White) f.pawns.whitePassedPawns else f.pawns.blackPassedPawns).getOrElse(0)
      val finalScore = (0.8 + (if (isAdvanced) 0.5 else 0.0) + (if (ctx.phaseEnum == lila.llm.analysis.L3.GamePhaseType.Endgame) 0.4 else 0.0)) * (if (ourPassers > 0) 1.0 else 0.3)

      Some(PlanMatch(
        Plan.PassedPawnPush(side), 
        finalScore, 
        evidence,
        supports.toList,
        blockers.toList,
        missing.toList
      ))

  private def scoreKingActivation(
      motifs: List[Motif],
      ctx: IntegratedContext,
      side: Color
  ): Option[PlanMatch] =
    val kingMoves = motifs.collect:
      case m: KingStep if m.stepType == KingStepType.Activation => m

    if (ctx.phaseEnum == lila.llm.analysis.L3.GamePhaseType.Endgame && kingMoves.nonEmpty)
      val evidence = kingMoves.take(2).map: m =>
        EvidenceAtom(m, 0.4, s"King march: ${m.move.getOrElse("")}")
      
      val supports = List("Endgame phase permits king activity", "King can support passed pawns")
      val blockers = List("Remaining enemy major pieces posed a threat")
      
      Some(PlanMatch(Plan.KingActivation(side), 0.7 + kingMoves.size * 0.1, evidence, supports, blockers, Nil))
    else None

  private def scorePromotion(
      motifs: List[Motif],
      ctx: IntegratedContext,
      side: Color
  ): Option[PlanMatch] =
    val promoMotifs = motifs.collect { case m: PawnPromotion => m }

    if (promoMotifs.nonEmpty)
      val evidence = promoMotifs.map: m =>
        EvidenceAtom(m, 1.0, s"Promotion: ${m.move.getOrElse("")}")
      
      val supports = List("Pawn has reached the penultimate rank", "Clear path to promotion")
      val blockers = List("Opponent pieces blockading the square")
      val missing = List("Need to deflect the blockader")
      
      Some(PlanMatch(Plan.Promotion(side), 1.2, evidence, supports, blockers, missing))
    else None

  private def scoreDefensiveConsolidation(
      motifs: List[Motif],
      ctx: IntegratedContext,
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

      val supports = scala.collection.mutable.ListBuffer[String]()
      val blockers = scala.collection.mutable.ListBuffer[String]()
      val missing = scala.collection.mutable.ListBuffer[String]()

      ctx.features.foreach { f =>
        val ourKingExposed = if (side == White) f.kingSafety.whiteKingExposedFiles else f.kingSafety.blackKingExposedFiles
        val ourBackRankWeak = if (side == White) f.kingSafety.whiteBackRankWeakness else f.kingSafety.blackBackRankWeakness

        if (ourKingExposed == 0) supports += "Solid pawn wall around king"
        if (!ourBackRankWeak) supports += "Healthy back rank"

        if (ourKingExposed > 1) blockers += "Significant king exposure"
        if (ourBackRankWeak) blockers += "Vulnerable back rank"

        if (ctx.evalFor(side) < -100) missing += "Material deficit makes consolidation difficult"
      }

      Some(PlanMatch(
        Plan.DefensiveConsolidation(side), 
        defensiveMotifs.size * 0.3, 
        evidence,
        supports.toList,
        blockers.toList,
        missing.toList
      ))

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
       
       val supports = List("Threat identified early", "Resources available for defense")
       val blockers = List("Defensive move may concede initiative")
       
       Some(PlanMatch(Plan.Prophylaxis(side, "Attack"), 0.8, evidence, supports, blockers, Nil))
     else if (ctx.evalFor(side) < -50 && defensiveMoves.nonEmpty)
       // Fallback for general defensive posture
       val evidence = defensiveMoves.map(m => EvidenceAtom(m, 0.4, "Defensive repositioning"))
       Some(PlanMatch(Plan.Prophylaxis(side, "Attack"), 0.5, evidence, List("General defensive posture"), Nil, Nil))
     else None

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
      val supports = List("Winning material advantage", "Reduced complexity increases win probability")
      val blockers = List("Exchanging active pieces for passive ones")
      Some(PlanMatch(Plan.Simplification(side), 1.0 + exchanges.size * 0.2 + reliefBonus, evidence, supports, blockers, Nil))
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
    
    // P1 FIX: Only trade queens if both sides HAVE queens
    val countQueens = ctx.features.map(f => f.imbalance.whiteQueens + f.imbalance.blackQueens).getOrElse(0)
    
    if (queenTrades.nonEmpty && (winning || drawish) && !attackOpportunityAtRisk && countQueens >= 2)
      val reason = if (winning) "Simplification (Winning)" else "Transition to Draw"
      val evidence = queenTrades.map(m => EvidenceAtom(m, 1.0, s"Queen exchange: $reason"))
      val supports = List("Neutralizes opponent's dynamic potential", "Solidifies structural advantage")
      val blockers = List("Losing own attacking potential")
      Some(PlanMatch(Plan.QueenTrade(side), 1.2, evidence, supports, blockers, Nil))
    else None

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
      val supports = List("Halts passed pawn advance", "Blockading piece is safe (Outpost/Centralized)")
      val blockers = List("Blockading piece is tied down to defense")
      val missing = if (!hasDetailedEvidence) List("Need to secure the blockade square") else Nil

      Some(PlanMatch(Plan.Blockade(side, blockadeSquareStr), baseScore, finalEvidence, supports, blockers, missing))
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
       val supports = List("Creating weaknesses in opponent's pawn structure", "Queenside space advantage")
       val blockers = List("Opening lines for opponent's pieces")
       val missing = List("Need to advance terminal pawn (a or b file)")
       Some(PlanMatch(Plan.MinorityAttack(side), 0.85, evidence, supports, blockers, missing))
    else None

  private def scorePawnChain(motifs: List[Motif], ctx: IntegratedContext, side: Color): Option[PlanMatch] =
    val chainMotifs = motifs.collect { case m: PawnChain => m }
    if (chainMotifs.nonEmpty)
      val evidence = chainMotifs.map(m => EvidenceAtom(m, 0.4, "Pawn chain structure"))
      val supports = List("Solid pawn structure", "Long-term positional stability")
      val blockers = List("Inflexible pawns can become targets")
      Some(PlanMatch(Plan.PawnChain(side), 0.5 + chainMotifs.size * 0.1, evidence, supports, blockers, Nil))
    else None

  private def scoreSpaceAdvantage(motifs: List[Motif], ctx: IntegratedContext, side: Color): Option[PlanMatch] =
    val spaceMotifs = motifs.collect { case m: SpaceAdvantage => m }
    if (spaceMotifs.nonEmpty)
      val evidence = spaceMotifs.map(m => EvidenceAtom(m, 0.7, "Space advantage"))
      val supports = List("Greater piece mobility", "Easier maneuvering between flanks")
      val blockers = List("Overextended pawns can be weak")
      val missing = if (ctx.features.exists(_.activity.whiteDevelopmentLag > 0)) List("Completion of development to utilize space") else Nil
      Some(PlanMatch(Plan.SpaceAdvantage(side), 0.7, evidence, supports, blockers, missing))
    else None
    
  private def scoreZugzwang(motifs: List[Motif], ctx: IntegratedContext, side: Color): Option[PlanMatch] =
    val zugMotifs = motifs.collect { case m: Zugzwang => m }
    if (zugMotifs.nonEmpty && ctx.phaseEnum == lila.llm.analysis.L3.GamePhaseType.Endgame)
       val evidence = zugMotifs.map(m => EvidenceAtom(m, 1.0, "Zugzwang pattern"))
       val supports = List("Opponent has exhausted useful moves", "Dominant piece placement")
       val blockers = List("Stalemate risks if not careful")
       Some(PlanMatch(Plan.Zugzwang(side), 1.3, evidence, supports, blockers, Nil))
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
      
      val supports = scala.collection.mutable.ListBuffer[String]()
      val blockers = scala.collection.mutable.ListBuffer[String]()
      val missing = scala.collection.mutable.ListBuffer[String]()
      
      val kingExposure = riskProfile.map(_.kingExposureSum).getOrElse(0)
      if (kingExposure > 5) supports += "Exposed opponent king justifies investment"
      if (evalBonus > 0) supports += "Strong overall position allows for material sacrifice"
      
      if (eval < 0) blockers += "Material deficit makes sacrifice risky"
      
      if (highRiskBonus > 0) missing += "Careful calculation of subsequent lines required"

      Some(PlanMatch(Plan.Sacrifice(side, sacPiece), baseScore.min(2.0), evidence, supports.toList, blockers.toList, missing.toList))
    else None

  private def scoreMinorPieceManeuver(
      motifs: List[Motif],
      ctx: IntegratedContext,
      side: Color
  ): Option[PlanMatch] =
    val maneuverMotifs = motifs.collect { case m: Centralization => m }
    if (maneuverMotifs.nonEmpty)
      val targetSq = maneuverMotifs.headOption.map(_.square.key).getOrElse("weak square")
      val evidence = maneuverMotifs
        .take(1)
        .map(m => EvidenceAtom(m, 0.4, s"Minor piece maneuver: ${m.move.getOrElse("")}"))
      val supports = List("Improved piece coordination", "Targeting weak squares")
      val blockers = List("Maneuver takes multiple turns")
      Some(PlanMatch(Plan.MinorPieceManeuver(side, targetSq), 0.5, evidence, supports, blockers, Nil))
    else None

  private def scoreFileControl(
      motifs: List[Motif],
      ctx: IntegratedContext,
      side: Color
  ): Option[PlanMatch] =
    val fileMotifs = motifs.collect { case m: OpenFileControl => m }
    
    // P1 FIX: Open file control requires ROOKS, not just a Queen or empty file
    val hasRooks = ctx.features.exists(f => (side == White && f.imbalance.whiteRooks > 0) || (side == Black && f.imbalance.blackRooks > 0))
    
    if (fileMotifs.nonEmpty && hasRooks)
      val evidence = fileMotifs.map(m => EvidenceAtom(m, 0.6, s"Control of ${m.file} file"))
      val supports = List("Infiltration path for major pieces", "Restricts opponent's lateral mobility")
      val blockers = List("File can be contested by opponent rooks")
      val missing = Nil // Missing field handled by hasRooks check now
      Some(PlanMatch(Plan.FileControl(side, "Open"), 0.8, evidence, supports, blockers, missing))
    else None

  extension (f: File)
    def isKingside: Boolean = f == File.F || f == File.G || f == File.H
    def isQueenside: Boolean = f == File.A || f == File.B || f == File.C
    def isCentral: Boolean = f == File.D || f == File.E
