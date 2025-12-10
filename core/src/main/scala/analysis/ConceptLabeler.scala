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

  enum EndgameTag:
    case KingActivityIgnored, KingActivityGood
    case RookBehindPassedPawnObeyed, RookBehindPassedPawnIgnored
    case WrongBishopDraw

  enum TransitionTag:
    case EndgameTransition
    case TacticalToPositional
    case FortressStructure // Renamed from FortressBuilding
    case ComfortToUnpleasant
    case PositionalSacrifice
    case KingExposed
    case ConversionDifficulty

  case class ConceptLabels(
    structureTags: List[StructureTag],
    planTags: List[PlanTag],
    tacticTags: List[TacticTag],
    mistakeTags: List[MistakeTag],
    endgameTags: List[EndgameTag],
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
      bestEval: Int
  ): ConceptLabels =

    val structure = labelStructure(featuresBefore, featuresAfter, experiments, baselineEval)
    val plans = labelPlans(featuresBefore, experiments, baselineEval)
    val (tactics, missedPatterns) = labelTactics(featuresBefore, experiments, baselineEval, movePlayedUci, bestEval, evalAfterPlayed)
    val mistakes = labelMistakes(movePlayedUci, featuresBefore, featuresAfter, experiments, baselineEval, evalAfterPlayed, bestEval)
    val endgame = labelEndgame(featuresBefore, featuresAfter, movePlayedUci)
    val transitions = TransitionTagger.label(featuresBefore, featuresAfter, baselineEval, evalAfterPlayed, bestEval)

    ConceptLabels(structure, plans, tactics, mistakes, endgame, transitions, missedPatterns)


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
      
    // Geometry Features: BadBishop
    if fBefore.geometry.whiteWrongBishop then tags += StructureTag.BadBishopWhite
    if fBefore.geometry.blackWrongBishop then tags += StructureTag.BadBishopBlack

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

      if fAfter.kingSafety.whiteKingExposedFiles > fBefore.kingSafety.whiteKingExposedFiles then
         tags += MistakeTag.PrematurePawnPush

      // Fear Detection: Good attack was available but played passive defense
      val attackAvailable = exps.exists(e => 
         isCandidate(e, "KingsidePawnStorm") && (getScore(e) - baselineEval) > SUCCESS_THRESHOLD_CP
      )
      val evalWorsened = evalAfterPlayed < baselineEval - 50
      if attackAvailable && evalWorsened then
         tags += MistakeTag.Fear

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
       val kingDistBefore = fBefore.kingSafety.whiteKingCenterDistance // assuming white context for now, need side?
       // Wait, we need side perspective. Features are absolute white/black.
       // We need to know who moved.
       // 'movePlayed' is UCI. We don't have side info easily here unless we parse FEN or look at sideToMove in features?
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

    // 3. Wrong Corner Color (WrongBishopDraw)
    if (if fBefore.sideToMove == "white" then fBefore.geometry.whiteWrongBishop else fBefore.geometry.blackWrongBishop) then
       tags += EndgameTag.WrongBishopDraw

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
