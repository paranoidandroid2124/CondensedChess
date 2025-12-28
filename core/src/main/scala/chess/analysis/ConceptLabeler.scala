package chess
package analysis

import chess.analysis.FeatureExtractor.PositionFeatures
import chess.analysis.AnalysisTypes.ExperimentResult

object ConceptLabeler:

  // --- Tag Definitions ---

  enum StructureTag:
    case IqpWhite, IqpBlack, HangingPawnsWhite, HangingPawnsBlack
    case MinorityAttackCandidate
    case CentralBreakAvailable, CentralBreakSuccess, CentralBreakBad
    // Positional
    case SpaceAdvantageWhite, SpaceAdvantageBlack
    case KingExposedWhite, KingExposedBlack
    case BadBishopWhite, BadBishopBlack

  enum PlanTag:
    case CentralBreakGood, CentralBreakBad, CentralBreakRefuted, CentralBreakPremature
    case QueensideMajorityGood, QueensideMajorityBad
    case KingsideAttackGood, KingsideAttackBad, KingsideAttackRefuted, KingsideAttackPremature
    case PieceImprovementGood, PieceImprovementBad
    case RookLiftGood, RookLiftBad

  enum TacticTag:
    case GreekGiftSound, GreekGiftUnsound
    case BackRankMatePattern
    case TacticalPatternMiss
    case ForkSound
    case PinSound
    case SkewerSound
    case DiscoveredAttackSound

  enum MistakeTag:
    case TacticalMiss
    case PrematurePawnPush
    case PassiveMove
    case MissedCentralBreak
    case Greed // Took material but lost game
    case Fear // Passive defense when attack was available
    case PositionalTradeError
    case IgnoredThreat 

    def toSnakeCase: String = this match
      case TacticalMiss => "tactical_miss"
      case PrematurePawnPush => "premature_pawn_push"
      case PassiveMove => "passive_move"
      case MissedCentralBreak => "missed_central_break"
      case Greed => "greedy"
      case Fear => "fear"
      case PositionalTradeError => "positional_trade_error"
      case IgnoredThreat => "ignored_threat"

  enum EndgameTag:
    case KingActivityIgnored, KingActivityGood
    case RookBehindPassedPawnObeyed, RookBehindPassedPawnIgnored
    case WrongBishopDraw

  enum PositionalTag:
    case OpenFile(file: String, side: Color)
    case WeakSquare(square: String, side: Color)
    case Outpost(square: String, side: Color)
    case WeakBackRank(side: Color)
    case LoosePiece(square: String, side: Color)
    case KingStuckCenter(side: Color)
    case RookOnSeventh(side: Color)
    case PawnStorm(side: Color)
    case OppositeColorBishops
    case KingAttackReady(side: Color)
    // Concept-based
    case RestrictedBishop(side: Color)
    case StrongKnight(side: Color)
    case ColorComplexWeakness(side: Color)
    case LockedPosition
    case FortressDefense(side: Color)
    case DynamicPosition
    case DryPosition
    case DrawishPosition
    case ActiveRooks(side: Color)
    case SpaceAdvantage(side: Color)
    case KingSafetyCrisis(side: Color)
    case ConversionDifficulty(side: Color)
    case HighBlunderRisk
    case TacticalComplexity
    case MaterialImbalance
    case LongTermCompensation(side: Color)
    case EngineOnlyMove(side: Color)
    case ComfortablePosition(side: Color)
    case UnpleasantPosition(side: Color)
    case BishopPairAdvantage(side: Color)

    def toSnakeCase: String = this match
      case OpenFile(f, c) => s"${c.name.toLowerCase}_open_${f}_file"
      case WeakSquare(sq, c) => s"${c.name.toLowerCase}_weak_$sq"
      case Outpost(sq, c) => s"${c.name.toLowerCase}_outpost_$sq"
      case WeakBackRank(c) => s"${c.name.toLowerCase}_weak_back_rank"
      case LoosePiece(sq, c) => s"${c.name.toLowerCase}_loose_piece_$sq"
      case KingStuckCenter(c) => s"${c.name.toLowerCase}_king_stuck_center"
      case RookOnSeventh(c) => s"${c.name.toLowerCase}_rook_on_seventh"
      case PawnStorm(c) => s"${c.name.toLowerCase}_pawn_storm"
      case OppositeColorBishops => "opposite_color_bishops"
      case KingAttackReady(c) => s"${c.name.toLowerCase}_king_attack_ready"
      case RestrictedBishop(c) => s"${c.name.toLowerCase}_restricted_bishop"
      case StrongKnight(c) => s"${c.name.toLowerCase}_strong_knight"
      case ColorComplexWeakness(c) => s"${c.name.toLowerCase}_color_complex_weakness"
      case LockedPosition => "locked_position"
      case FortressDefense(c) => s"${c.name.toLowerCase}_fortress_defense"
      case DynamicPosition => "dynamic_position"
      case DryPosition => "dry_position"
      case DrawishPosition => "drawish_position"
      case ActiveRooks(c) => s"${c.name.toLowerCase}_active_rooks"
      case SpaceAdvantage(c) => s"${c.name.toLowerCase}_space_advantage"
      case KingSafetyCrisis(c) => s"${c.name.toLowerCase}_king_safety_crisis"
      case ConversionDifficulty(c) => s"${c.name.toLowerCase}_conversion_difficulty"
      case HighBlunderRisk => "high_blunder_risk"
      case TacticalComplexity => "tactical_complexity"
      case MaterialImbalance => "material_imbalance"
      case LongTermCompensation(c) => s"${c.name.toLowerCase}_long_term_compensation"
      case EngineOnlyMove(c) => s"${c.name.toLowerCase}_engine_only_move"
      case ComfortablePosition(c) => s"${c.name.toLowerCase}_comfortable_position"
      case UnpleasantPosition(c) => s"${c.name.toLowerCase}_unpleasant_position"
      case BishopPairAdvantage(c) => s"${c.name.toLowerCase}_bishop_pair_advantage"

  enum TransitionTag:
    case EndgameTransition
    case TacticalToPositional
    case FortressStructure // Renamed from FortressBuilding
    case ComfortToUnpleasant
    case PositionalSacrifice
    case KingExposed
    case ConversionDifficulty

    def toSnakeCase: String = this match
      case EndgameTransition => "endgame_transition"
      case TacticalToPositional => "tactical_to_positional"
      case FortressStructure => "fortress_building"
      case ComfortToUnpleasant => "comfort_to_unpleasant"
      case PositionalSacrifice => "positional_sacrifice"
      case KingExposed => "king_exposed"
      case ConversionDifficulty => "conversion_difficulty"

  case class ConceptLabels(
    structureTags: List[StructureTag],
    planTags: List[PlanTag],
    tacticTags: List[TacticTag],
    mistakeTags: List[MistakeTag],
    endgameTags: List[EndgameTag],
    positionalTags: List[PositionalTag] = Nil,
    transitionTags: List[TransitionTag] = Nil,
    missedPatternTypes: List[String] = Nil  // e.g., ["Fork", "Pin"] when TacticalPatternMiss
  )

  // --- Constants ---
  val SUCCESS_THRESHOLD_CP = 60
  val FAILURE_THRESHOLD_CP = -60
  val TACTIC_WIN_THRESHOLD_CP = 150
  val BLUNDER_THRESHOLD_CP = 150

  // --- Main Labeler ---

  def labelAll(
      featuresBefore: PositionFeatures,
      featuresAfter: PositionFeatures,
      movePlayedUci: String,
      experiments: List[ExperimentResult],
      baselineEval: Int,
      evalAfterPlayed: Int,
      bestEval: Int,
      positionalTags: List[PositionalTag] = Nil
  ): ConceptLabels =

    val structure = labelStructure(featuresBefore, featuresAfter, experiments, baselineEval)
    val plans = labelPlans(featuresBefore, experiments, baselineEval)
    val (tactics, missedPatterns) = labelTactics(featuresBefore, experiments, baselineEval, movePlayedUci, bestEval, evalAfterPlayed)
    val mistakes = labelMistakes(movePlayedUci, featuresBefore, featuresAfter, experiments, baselineEval, evalAfterPlayed, bestEval)
    val endgame = labelEndgame(featuresBefore, featuresAfter, movePlayedUci)
    
    // Transition
    val transitions = TransitionTagger.label(featuresBefore, featuresAfter, baselineEval, evalAfterPlayed, bestEval)

    ConceptLabels(structure, plans, tactics, mistakes, endgame, positionalTags, transitions, missedPatterns)


  // --- Sub-Labelers ---

  private def getScore(ex: ExperimentResult): Int =
    // Helper to get CP score from ExperimentResult (EngineEval)
    // Assuming best line
    ex.eval.lines.headOption.flatMap(l => l.cp.orElse(l.mate.map(m => if m > 0 then 10000 - m else -10000 + m))).getOrElse(0)

  // Helper to check candidate type from metadata
  private def isCandidate(ex: ExperimentResult, tpe: String): Boolean =
    ex.metadata.get("candidateType").contains(tpe) || ex.metadata.get("candidateType").contains(tpe.toString)

  def labelStructure(
      fBefore: PositionFeatures, 
      fAfter: PositionFeatures, 
      exps: List[ExperimentResult],
      baselineEval: Int
  ): List[StructureTag] =
    val tags = List.newBuilder[StructureTag]

    if fBefore.pawns.whiteIQP then tags += StructureTag.IqpWhite
    if fBefore.pawns.blackIQP then tags += StructureTag.IqpBlack
    if fBefore.pawns.whiteHangingPawns then tags += StructureTag.HangingPawnsWhite
    if fBefore.pawns.blackHangingPawns then tags += StructureTag.HangingPawnsBlack
    
    if fBefore.pawns.whiteMinorityAttackReady || fBefore.pawns.blackMinorityAttackReady then
    if fBefore.pawns.whiteMinorityAttackReady || fBefore.pawns.blackMinorityAttackReady then
      tags += StructureTag.MinorityAttackCandidate
      
    // Geometry/BadBishop check removed - GeometryFeatures was deleted

    // Space Advantage: Compare central space control
    val spaceThreshold = 3
    val spaceDiff = fBefore.space.whiteCentralSpace - fBefore.space.blackCentralSpace
    if spaceDiff >= spaceThreshold then tags += StructureTag.SpaceAdvantageWhite
    if spaceDiff <= -spaceThreshold then tags += StructureTag.SpaceAdvantageBlack

    // King Exposed: Check exposed files
    if fBefore.kingSafety.whiteKingExposedFiles >= 2 then tags += StructureTag.KingExposedWhite
    if fBefore.kingSafety.blackKingExposedFiles >= 2 then tags += StructureTag.KingExposedBlack

    val breaks = exps.filter(e => isCandidate(e, "CentralBreak"))
    if breaks.nonEmpty then
      tags += StructureTag.CentralBreakAvailable
      breaks.foreach { b =>
        val delta = getScore(b) - baselineEval
        if delta > SUCCESS_THRESHOLD_CP then tags += StructureTag.CentralBreakSuccess
        if delta < FAILURE_THRESHOLD_CP then tags += StructureTag.CentralBreakBad
      }

    tags.result().distinct

  def labelPlans(
      fBefore: PositionFeatures, 
      exps: List[ExperimentResult], 
      baselineEval: Int
  ): List[PlanTag] =
    val tags = List.newBuilder[PlanTag]

    exps.foreach { ex =>
      val delta = getScore(ex) - baselineEval
      val cType = ex.metadata.get("candidateType").getOrElse("")
      
      val isRefuted = delta < -200 // Big drop = tactical refutation
      
      cType match
        case "CentralBreak" =>
          if delta > SUCCESS_THRESHOLD_CP then tags += PlanTag.CentralBreakGood
          else if isRefuted then tags += PlanTag.CentralBreakRefuted
          else if delta < FAILURE_THRESHOLD_CP then tags += PlanTag.CentralBreakPremature
        case "QueensideMajority" =>
          if delta > SUCCESS_THRESHOLD_CP then tags += PlanTag.QueensideMajorityGood
          else if delta < FAILURE_THRESHOLD_CP then tags += PlanTag.QueensideMajorityBad
        case "KingsidePawnStorm" =>
          if delta > SUCCESS_THRESHOLD_CP then tags += PlanTag.KingsideAttackGood
          else if isRefuted then tags += PlanTag.KingsideAttackRefuted
          else if delta < FAILURE_THRESHOLD_CP then tags += PlanTag.KingsideAttackPremature
        case "PieceImprovement" =>
          if delta > SUCCESS_THRESHOLD_CP then tags += PlanTag.PieceImprovementGood
        case "RookLift" =>
          if delta > SUCCESS_THRESHOLD_CP then tags += PlanTag.RookLiftGood
        case _ => 
    }

    tags.result().distinct

  def labelTactics(
      fBefore: PositionFeatures, 
      exps: List[ExperimentResult], 
      baselineEval: Int,
      movePlayed: String,
      bestEval: Int,
      evalAfterPlayed: Int
  ): (List[TacticTag], List[String]) =  // Returns (tacticTags, missedPatternTypes)
    val tags = List.newBuilder[TacticTag]
    
    // Sound Tactics found in experiments
    exps.filter(e => isCandidate(e, "SacrificeProbe")).foreach { ex =>
      val delta = getScore(ex) - baselineEval
      val move = ex.move.getOrElse("")
      if (move.startsWith("h7") && fBefore.kingSafety.blackKingShieldPawns > 0) ||
         (move.contains("h7") || move.contains("h2")) then
           if delta > TACTIC_WIN_THRESHOLD_CP then tags += TacticTag.GreekGiftSound
           else if delta < FAILURE_THRESHOLD_CP then tags += TacticTag.GreekGiftUnsound
    }

    // Back Rank Mate Pattern
    // Heuristic: Mate in < 5, and king on 1st/8th rank, and own pawns blocking?
    // This is hard to detect purely from numeric features without board state.
    // We can check if 'mate' is present in bestEval and king is on back rank.
    if exps.exists(e => e.eval.lines.headOption.exists(_.mate.exists(m => m.abs <= 5))) then
       val kingRank = if bestEval > 0 then fBefore.kingSafety.blackBackRankWeak else fBefore.kingSafety.whiteBackRankWeak
       // Actually 'blackBackRankWeak' is boolean. We need rank index.
       // Current features don't expose raw King Rank directly.
       // We can infer back rank if 'whiteBackRankWeak' is true? No.
       // We need to extend FeatureExtractor or use available proxy.
       // 'whiteBackRankWeak' logic: kSq.rank == Rank.First && kingShield == 0.
       // If so, king is on back rank.
       val isBackRank = if bestEval > 0 then fBefore.kingSafety.blackBackRankWeak else fBefore.kingSafety.whiteBackRankWeak
       
       if isBackRank && (fBefore.kingSafety.whiteKingShieldPawns > 0 || fBefore.kingSafety.blackKingShieldPawns > 0) then 
          tags += TacticTag.BackRankMatePattern

    val deltaToBest = bestEval - evalAfterPlayed
    
    // Collect missed tactical patterns
    val missedPatterns = List.newBuilder[String]
    if deltaToBest > BLUNDER_THRESHOLD_CP then
       tags += TacticTag.TacticalPatternMiss
       // Check which sound tactics were available but not played
       if exps.exists(e => isCandidate(e, "Fork") && (getScore(e) - baselineEval) > TACTIC_WIN_THRESHOLD_CP) then
         missedPatterns += "Fork"
       if exps.exists(e => isCandidate(e, "Pin") && (getScore(e) - baselineEval) > TACTIC_WIN_THRESHOLD_CP) then
         missedPatterns += "Pin"
       if exps.exists(e => isCandidate(e, "DiscoveredAttack") && (getScore(e) - baselineEval) > TACTIC_WIN_THRESHOLD_CP) then
         missedPatterns += "DiscoveredAttack"
       if exps.exists(e => isCandidate(e, "Skewer") && (getScore(e) - baselineEval) > TACTIC_WIN_THRESHOLD_CP) then
         missedPatterns += "Skewer"
       if exps.exists(e => isCandidate(e, "SacrificeProbe") && (getScore(e) - baselineEval) > TACTIC_WIN_THRESHOLD_CP) then
         missedPatterns += "Sacrifice"

    // Fork/Pin/Discovered Detection (using MoveGenerator's candidateType)
    exps.filter(e => isCandidate(e, "Fork")).foreach { ex =>
      val delta = getScore(ex) - baselineEval
      if delta > TACTIC_WIN_THRESHOLD_CP then tags += TacticTag.ForkSound
    }
    exps.filter(e => isCandidate(e, "Pin")).foreach { ex =>
      val delta = getScore(ex) - baselineEval
      if delta > TACTIC_WIN_THRESHOLD_CP then tags += TacticTag.PinSound
    }
    exps.filter(e => isCandidate(e, "DiscoveredAttack")).foreach { ex =>
      val delta = getScore(ex) - baselineEval
      if delta > TACTIC_WIN_THRESHOLD_CP then tags += TacticTag.DiscoveredAttackSound
    }
    exps.filter(e => isCandidate(e, "Skewer")).foreach { ex =>
      val delta = getScore(ex) - baselineEval
      if delta > TACTIC_WIN_THRESHOLD_CP then tags += TacticTag.SkewerSound
    }

    (tags.result().distinct, missedPatterns.result())

  def labelMistakes(
      movePlayed: String,
      fBefore: PositionFeatures,
      fAfter: PositionFeatures,
      exps: List[ExperimentResult],
      baselineEval: Int,
      evalAfterPlayed: Int,
      bestEval: Int
  ): List[MistakeTag] =
    val tags = List.newBuilder[MistakeTag]
    val deltaToBest = bestEval - evalAfterPlayed

    val goodPlans = exps.filter(e => (getScore(e) - baselineEval) > SUCCESS_THRESHOLD_CP)
    
    if deltaToBest > 100 then
       if goodPlans.nonEmpty && !goodPlans.exists(_.move.contains(movePlayed)) then
          val isBreak = movePlayed.contains("d4d5") || movePlayed.contains("e4e5")
          if !isBreak && goodPlans.exists(e => isCandidate(e, "CentralBreak")) then
             tags += MistakeTag.MissedCentralBreak
    
    if deltaToBest > BLUNDER_THRESHOLD_CP then
      tags += MistakeTag.TacticalMiss
      if goodPlans.nonEmpty && !goodPlans.exists(_.move.contains(movePlayed)) && !tags.result().contains(MistakeTag.MissedCentralBreak) then
         tags += MistakeTag.PassiveMove

      // Greed Detection
      // Use logic: Material increased, but position lost
      val side = fBefore.sideToMove // "white" or "black"
      val matDiff = if side == "white" 
        then fAfter.materialPhase.whiteMaterial - fBefore.materialPhase.whiteMaterial
        else fAfter.materialPhase.blackMaterial - fBefore.materialPhase.blackMaterial
      
      if matDiff > 0 then
         tags += MistakeTag.Greed

      val (exposedBefore, exposedAfter) = if side == "white"
         then (fBefore.kingSafety.whiteKingExposedFiles, fAfter.kingSafety.whiteKingExposedFiles)
         else (fBefore.kingSafety.blackKingExposedFiles, fAfter.kingSafety.blackKingExposedFiles)

      if exposedAfter > exposedBefore then
         tags += MistakeTag.PrematurePawnPush

      // Fear Detection: Good attack was available but played passive defense
      val attackAvailable = exps.exists(e => 
         isCandidate(e, "KingsidePawnStorm") && (getScore(e) - baselineEval) > SUCCESS_THRESHOLD_CP
      )
      val evalWorsened = evalAfterPlayed < baselineEval - 50
      if attackAvailable && evalWorsened then
         tags += MistakeTag.Fear

      // Ignored Threat
      // If we made a mistake/blunder and were under pressure
      if deltaToBest > 100 then
         val pressure = if side == "white"
            then fBefore.kingSafety.whiteKingRingEnemyPieces
            else fBefore.kingSafety.blackKingRingEnemyPieces
         
         val underPressure = pressure >= 3 // Threshold for pressure
         
         if underPressure then 
            tags += MistakeTag.IgnoredThreat

         // Positional Trade Error
         // Lost material advantage without tactical justification
         val minorsBefore = if side == "white" then fBefore.materialPhase.whiteMinorPieces else fBefore.materialPhase.blackMinorPieces
         val minorsAfter = if side == "white" then fAfter.materialPhase.whiteMinorPieces else fAfter.materialPhase.blackMinorPieces

         if minorsBefore > minorsAfter then
            tags += MistakeTag.PositionalTradeError

    tags.result().distinct

  def labelEndgame(
      fBefore: PositionFeatures,
      fAfter: PositionFeatures,
      movePlayed: String
  ): List[EndgameTag] =
    val tags = List.newBuilder[EndgameTag]
    
    // 1. King Activity (KingActivityIgnored)
    // If we are in endgame, and King moves AWAY from center or action when it should be active?
    // Or if opponent has active king and we don't?
    val phase = fBefore.materialPhase.phase
    if phase == "endgame" then
       val side = fBefore.sideToMove // "white" or "black"
       val (myDistBef, myDistAft) = if side == "white" 
         then (fBefore.kingSafety.whiteKingCenterDistance, fAfter.kingSafety.whiteKingCenterDistance)
         else (fBefore.kingSafety.blackKingCenterDistance, fAfter.kingSafety.blackKingCenterDistance)
         
       // King moved away from center in pure pawn ending?
       val isPawnEnding = fBefore.materialPhase.whiteMajorPieces == 0 && fBefore.materialPhase.whiteMinorPieces == 0 &&
                          fBefore.materialPhase.blackMajorPieces == 0 && fBefore.materialPhase.blackMinorPieces == 0
       
       if isPawnEnding && myDistAft > myDistBef + 1.0 then // significantly away
          tags += EndgameTag.KingActivityIgnored
       else if isPawnEnding && myDistAft < myDistBef - 0.5 then
          tags += EndgameTag.KingActivityGood

    // 2. Rook Behind Passed Pawn
    // Use features: whiteRooksBehindPassedPawns
    // If we have passed pawns and rooks behind them -> Good
    // If we have passed pawns and rooks NOT behind them -> Bad?
    // Heuristic: If passed pawns > 0.
    val pPassed = if fBefore.sideToMove == "white" then fBefore.pawns.whitePassedPawns else fBefore.pawns.blackPassedPawns
    val rBehind = if fBefore.sideToMove == "white" then fBefore.coordination.whiteRooksBehindPassedPawns else fBefore.coordination.blackRooksBehindPassedPawns
    
    if pPassed > 0 && rBehind > 0 then
       tags += EndgameTag.RookBehindPassedPawnObeyed
    else if pPassed > 0 && rBehind == 0 && (if fBefore.sideToMove == "white" then fBefore.activity.whiteRookMobility > 0 else fBefore.activity.blackRookMobility > 0) then
       // Has rooks, has passed pawns, but none behind? Maybe ignored.
       tags += EndgameTag.RookBehindPassedPawnIgnored


    // WrongBishopDraw check removed - GeometryFeatures was deleted

    tags.result().distinct

  object TransitionTagger:
    def label(
        fBefore: PositionFeatures,
        fAfter: PositionFeatures,
        evalBefore: Int,
        evalAfter: Int,
        bestEval: Int
    ): List[TransitionTag] =
      val tags = List.newBuilder[TransitionTag]
      val phaseBefore = fBefore.materialPhase.phase
      val phaseAfter = fAfter.materialPhase.phase
      
      if phaseBefore != "endgame" && phaseAfter == "endgame" then
        tags += TransitionTag.EndgameTransition
      
      val kingSafetyDelta = (fAfter.kingSafety.whiteKingExposedFiles + fAfter.kingSafety.blackKingExposedFiles) -
                            (fBefore.kingSafety.whiteKingExposedFiles + fBefore.kingSafety.blackKingExposedFiles)
      if kingSafetyDelta >= 2 then
        tags += TransitionTag.KingExposed
      
      val mp = fBefore.materialPhase
      val materialBefore = mp.whiteMajorPieces + mp.blackMajorPieces + mp.whiteMinorPieces + mp.blackMinorPieces
      val mpAfter = fAfter.materialPhase
      val materialAfter = mpAfter.whiteMajorPieces + mpAfter.blackMajorPieces + mpAfter.whiteMinorPieces + mpAfter.blackMinorPieces
      val materialDrop = materialBefore - materialAfter
      if materialDrop >= 3 && evalAfter >= evalBefore - 50 then
        tags += TransitionTag.PositionalSacrifice
      
      val winPctBefore = 50.0 + evalBefore / 10.0
      val evalDrop = evalBefore - evalAfter
      if winPctBefore >= 70 && evalDrop >= 150 then
        tags += TransitionTag.ConversionDifficulty
      
      if evalBefore <= -150 && evalAfter.abs <= 50 then
        tags += TransitionTag.FortressStructure
      
      if materialDrop >= 3 && fAfter.materialPhase.phase == "endgame" then
        tags += TransitionTag.TacticalToPositional
      
      if evalBefore >= 50 && evalDrop >= 200 then
        tags += TransitionTag.ComfortToUnpleasant
      
      tags.result().distinct

  // --- Positional Logic (Migrated from SemanticTagger) ---

  def labelPositional(
      position: Position,
      perspective: Color,
      ply: Ply,
      self: FeatureExtractor.SideFeatures,
      opp: FeatureExtractor.SideFeatures,
      concepts: Option[ConceptScorer.Scores] = None,
      winPct: Double = 50.0
  ): List[PositionalTag] =
    val board = position.board
    val tags = List.newBuilder[PositionalTag]
    val plyNumber = ply.value
    val oppColor = !perspective
    val oppKing = board.kingPosOf(oppColor)
    val p = perspective.toString.toLowerCase

    if hasOpenFileThreat(board, oppKing, File.H, perspective) then tags += PositionalTag.OpenFile("h", perspective)
    if hasOpenFileThreat(board, oppKing, File.G, perspective) then tags += PositionalTag.OpenFile("g", perspective)

    weakFHomeTag(board, oppColor, File.F, Rank.Seventh, s"${oppColor.fold("black", "white")}_weak_f7").foreach(s => tags += PositionalTag.WeakSquare("f7", oppColor))
    weakFHomeTag(board, oppColor, File.F, Rank.Second, s"${oppColor.fold("black", "white")}_weak_f2").foreach(s => tags += PositionalTag.WeakSquare("f2", oppColor))

    strongOutpost(board, perspective).foreach(s => tags += PositionalTag.Outpost(s.replace("outpost_", ""), perspective))

    if backRankWeak(board, perspective) then tags += PositionalTag.WeakBackRank(perspective)
    looseMinor(board, perspective).foreach(s => tags += PositionalTag.LoosePiece(s.replace("loose_piece_", ""), perspective))

    if kingStuckCenter(position, perspective, plyNumber) then tags += PositionalTag.KingStuckCenter(perspective)
    if rookOnSeventh(board, perspective) then tags += PositionalTag.RookOnSeventh(perspective)

    val boardPawnStorm = pawnStormAgainstCastledKing(board, perspective, oppKing)
    if boardPawnStorm then tags += PositionalTag.PawnStorm(perspective)

    if opp.bishopPair && !self.bishopPair && FeatureExtractor.hasOppositeColorBishops(board) then
      tags += PositionalTag.OppositeColorBishops

    if self.kingRingPressure >= 4 && opp.kingRingPressure <= 1 then tags += PositionalTag.KingAttackReady(perspective)

    concepts.foreach { c =>
      if c.badBishop >= 0.6 then tags += PositionalTag.RestrictedBishop(perspective)
      if c.goodKnight >= 0.6 then
        if hasCentralKnightOutpost(board, perspective) then tags += PositionalTag.Outpost("central", perspective) else tags += PositionalTag.StrongKnight(perspective)
      if c.colorComplex >= 0.5 then tags += PositionalTag.ColorComplexWeakness(perspective)
      
      val endgame = isEndgame(board)
      if c.fortress >= 0.6 && !endgame && plyNumber > 20 then tags += PositionalTag.LockedPosition
      
      if c.fortress >= 0.6 && endgame then
        val myMat = materialScore(board, perspective)
        val oppMat = materialScore(board, oppColor)
        if myMat <= oppMat - 1.0 && c.drawish >= 0.5 then tags += PositionalTag.FortressDefense(perspective)
 
      val dynamicTagged =
        if c.dynamic >= 0.7 && c.dry < 0.5 then
          tags += PositionalTag.DynamicPosition
          true
        else false
      if !dynamicTagged && c.dry >= 0.6 && (plyNumber > 16 || isEndgame(board)) then tags += PositionalTag.DryPosition
      if c.drawish >= 0.7 && winPct >= 35.0 && winPct <= 65.0 then tags += PositionalTag.DrawishPosition
      if c.pawnStorm >= 0.6 && boardPawnStorm then tags += PositionalTag.PawnStorm(perspective)
      
      val spaceAdvantage = self.spaceControl.toDouble / (self.spaceControl + opp.spaceControl + 1.0)
      if spaceAdvantage >= 0.65 then tags += PositionalTag.SpaceAdvantage(perspective)
      if c.rookActivity >= 0.6 then tags += PositionalTag.ActiveRooks(perspective)
      
      val crisis = c.kingSafety >= 0.6 && (c.pawnStorm >= 0.5 || c.rookActivity >= 0.6)
      if crisis then tags += PositionalTag.KingSafetyCrisis(perspective)
      
      if c.conversionDifficulty >= 0.5 && winPct >= 60.0 then tags += PositionalTag.ConversionDifficulty(perspective)
      if c.blunderRisk >= 0.6 then tags += PositionalTag.HighBlunderRisk
      if c.tacticalDepth >= 0.6 then tags += PositionalTag.TacticalComplexity
      if c.imbalanced >= 0.6 then tags += PositionalTag.MaterialImbalance
      if c.alphaZeroStyle >= 0.6 then tags += PositionalTag.LongTermCompensation(perspective)
      if c.engineLike >= 0.6 then tags += PositionalTag.EngineOnlyMove(perspective)
      if c.comfortable >= 0.7 then tags += PositionalTag.ComfortablePosition(perspective)
      if c.unpleasant >= 0.6 && winPct <= 45.0 then tags += PositionalTag.UnpleasantPosition(perspective)
      
      if self.bishopPair && !opp.bishopPair && c.dry <= 0.4 then tags += PositionalTag.BishopPairAdvantage(perspective)
    }

    tags.result().distinct

  private def hasOpenFileThreat(board: Board, kingOpt: Option[Square], file: File, perspective: Color): Boolean =
    kingOpt.exists { kingSq =>
      val kingNearFile = (kingSq.file.value - file.value).abs <= 1
      val myMajorOnFile = hasMajorOnFile(board, perspective, file)
      val openOrSemiOpen =
        val friendly = board.pawns & board.byColor(perspective)
        val enemy = board.pawns & board.byColor(!perspective)
        val mask = file.bb
        val friendlyOnFile = friendly.intersects(mask)
        val enemyOnFile = enemy.intersects(mask)
        (!friendlyOnFile && !enemyOnFile) || (!friendlyOnFile && enemyOnFile)
      kingNearFile && myMajorOnFile && openOrSemiOpen
    }

  private def hasMajorOnFile(board: Board, color: Color, file: File): Boolean =
    val majors = (board.rooks | board.queens) & board.byColor(color)
    majors.squares.exists(_.file == file)

  private def weakFHomeTag(board: Board, color: Color, file: File, rank: Rank, tag: String): Option[String] =
    if isEndgame(board) then None
    else if !kingInFHomeSector(board, color) then None
    else
      val homeSq = Square(file, rank)
      val opp = !color
      val attackers = board.attackers(homeSq, opp)
      val defenders = board.attackers(homeSq, color)
      val attackersCount = attackers.count
      val defendersCount = defenders.count
      val strongAttackerExists = attackers.exists { sq => board.roleAt(sq).exists(r => r == Queen || r == Rook) }
      val oppMajorsExist = hasMajors(board, opp)

      board.pieceAt(homeSq) match
        case Some(Piece(Pawn, `color`)) =>
          if attackersCount >= 2 && attackersCount > defendersCount && strongAttackerExists then Some(tag) else None
        case _ =>
          if oppMajorsExist && attackersCount >= 1 && strongAttackerExists then Some(tag) else None

  private def kingInFHomeSector(board: Board, color: Color): Boolean =
    board.kingPosOf(color).exists { k =>
      color match
        case Color.White => k.rank.value <= Rank.Second.value && k.file.value >= File.E.value && k.file.value <= File.G.value
        case Color.Black => k.rank.value >= Rank.Seventh.value && k.file.value >= File.E.value && k.file.value <= File.G.value
    }

  private def hasMajors(board: Board, color: Color): Boolean =
    ((board.queens & board.byColor(color)).nonEmpty) || ((board.rooks & board.byColor(color)).nonEmpty)

  private def hasCentralKnightOutpost(board: Board, color: Color): Boolean =
    val knights = board.knights & board.byColor(color)
    val centralFiles = Set(File.D, File.E)
    val centralRanks = if color == Color.White then Set(Rank.Fourth, Rank.Fifth, Rank.Sixth) else Set(Rank.Fifth, Rank.Fourth, Rank.Third)
    knights.squares.exists(k => centralFiles.contains(k.file) && centralRanks.contains(k.rank))

  private def strongOutpost(board: Board, color: Color): Option[String] =
    val squares = if color == Color.White then List(Square.D4, Square.E4, Square.F4, Square.D5, Square.E5, Square.F5) else List(Square.D5, Square.E5, Square.F5, Square.D4, Square.E4, Square.F4)
    squares.collectFirst { case sq if isOutpost(board, color, sq) => s"outpost_${sq.key.toLowerCase}" }

  private def isOutpost(board: Board, color: Color, sq: Square): Boolean =
    val knights = board.knights & board.byColor(color)
    val pawnSupport = (board.pawns & board.byColor(color)).squares.exists { p => p.pawnAttacks(color).contains(sq) }
    val enemyPawns = board.pawns & board.byColor(!color)
    val enemyChasers = enemyPawns.squares.exists { p => p.pawnAttacks(!color).contains(sq) }
    knights.contains(sq) && pawnSupport && !enemyChasers

  private def backRankWeak(board: Board, color: Color): Boolean =
    board.kingPosOf(color).exists { k =>
      val backRank = if color == Color.White then Rank.First else Rank.Eighth
      if k.rank != backRank then false
      else if isEndgame(board) then false
      else
        val oppMajors = (board.rooks | board.queens) & board.byColor(!color)
        if oppMajors.isEmpty then false
        else
          val shieldRank = if color == Color.White then Rank.Second else Rank.Seventh
          val shieldFiles = if k.file.value <= File.D.value then List(File.B, File.C, File.D) else List(File.F, File.G, File.H)
          val pawns = board.pawns & board.byColor(color)
          shieldFiles.count(f => pawns.contains(Square(f, shieldRank))) < 2
    }

  private def looseMinor(board: Board, color: Color): Option[String] =
    val defenders = board.attackers(_, color)
    val attackers = board.attackers(_, !color)
    val minors = (board.bishops | board.knights) & board.byColor(color)
    minors.squares.collectFirst { case sq if attackers(sq).nonEmpty && defenders(sq).isEmpty => s"loose_piece_${sq.key.toLowerCase}" }

  private def kingStuckCenter(position: Position, color: Color, ply: Int): Boolean =
    val board = position.board
    val endgame = isEndgame(board)
    val anyQueen = board.queens.nonEmpty
    val canCastle = position.castles.can(color)
    board.kingPosOf(color).exists { k =>
      val centerFiles = Set(File.D, File.E)
      val centerRankOk = if color == Color.White then k.rank.value <= Rank.Third.value else k.rank.value >= Rank.Sixth.value
      ply > 12 && !endgame && anyQueen && canCastle && centerFiles.contains(k.file) && centerRankOk
    }

  private def rookOnSeventh(board: Board, color: Color): Boolean =
    val rooks = board.rooks & board.byColor(color)
    rooks.squares.exists { r => if color == Color.White then r.rank == Rank.Seventh else r.rank == Rank.Second }

  private def pawnStormAgainstCastledKing(board: Board, color: Color, oppKingOpt: Option[Square]): Boolean =
    if isEndgame(board) || board.queens.isEmpty then false
    else
      oppKingOpt.exists { king =>
        val isShortCastle = (king == Square.G1 && color == Color.Black) || (king == Square.G8 && color == Color.White)
        val isLongCastle = (king == Square.C1 && color == Color.Black) || (king == Square.C8 && color == Color.White)
        val myPawns = board.pawns & board.byColor(color)
        val advancedThreshold = if color == Color.White then Rank.Fourth.value else Rank.Fifth.value
        val candidatePawns = myPawns.squares.filter { s =>
          val kingside = s.file.value >= File.F.value
          val queenside = s.file.value <= File.C.value
          val advanced = if color == Color.White then s.rank.value >= advancedThreshold else s.rank.value <= advancedThreshold
          (isShortCastle && kingside && advanced) || (isLongCastle && queenside && advanced)
        }
        val closeToKing = candidatePawns.exists { s => (s.file.value - king.file.value).abs + (s.rank.value - king.rank.value).abs <= 3 }
        (isShortCastle || isLongCastle) && candidatePawns.size >= 2 && closeToKing
      }

  private def isEndgame(board: Board): Boolean =
    val whitePieces = board.byColor(Color.White).count
    val blackPieces = board.byColor(Color.Black).count
    whitePieces <= 6 || blackPieces <= 6 || (board.queens.isEmpty && whitePieces <= 8 && blackPieces <= 8)

  private def materialScore(board: Board, color: Color): Double =
    val pawns = (board.pawns & board.byColor(color)).count * 1.0
    val knights = (board.knights & board.byColor(color)).count * 3.0
    val bishops = (board.bishops & board.byColor(color)).count * 3.0
    val rooks = (board.rooks & board.byColor(color)).count * 5.0
    val queens = (board.queens & board.byColor(color)).count * 9.0
    pawns + knights + bishops + rooks + queens
