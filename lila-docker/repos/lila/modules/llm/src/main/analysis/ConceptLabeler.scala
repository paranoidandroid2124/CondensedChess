package lila.llm

import lila.llm.model.*

import _root_.chess.*

/**
 * Unified Concept Labeler
 * 
 * Combines:
 * - ConceptLabeler: Strategic/structural/tactical tagging
 * - PlanMatcher: High-level plan identification (absorbed as PlanTag enum)
 * 
 * This is the single source of truth for semantic position labeling.
 */
enum StructureTag:
  case IqpWhite, IqpBlack
  case HangingPawnsWhite, HangingPawnsBlack
  case MinorityAttackCandidate
  case CentralBreakAvailable, CentralBreakSuccess, CentralBreakBad
  case SpaceAdvantageWhite, SpaceAdvantageBlack
  case KingExposedWhite, KingExposedBlack

enum PlanTag:
  // Attack plans
  case KingsideAttackGood, KingsideAttackBad, KingsideAttackPremature
  case QueensideAttackGood, QueensideAttackBad
  case PawnStormGood, PawnStormBad
  case DirectMateAvailable
  // Positional plans
  case CentralControlGood, CentralControlBad
  case PieceActivationGood, PieceActivationBad
  case RookActivationGood
  // Structural plans
  case PassedPawnPushGood
  // Endgame plans
  case KingActivationGood, KingActivationBad
  case PromotionThreat
  // Defensive plans
  case DefensiveConsolidation, PerpetualCheckAvailable
  case PawnBreak(square: String) // e.g. "c5"

  def toSnakeCase: String = this match
    case KingsideAttackGood => "kingside_attack_good"
    case KingsideAttackBad => "kingside_attack_bad"
    case KingsideAttackPremature => "kingside_attack_premature"
    case QueensideAttackGood => "queenside_attack_good"
    case QueensideAttackBad => "queenside_attack_bad"
    case PawnStormGood => "pawn_storm_good"
    case PawnStormBad => "pawn_storm_bad"
    case DirectMateAvailable => "direct_mate_available"
    case CentralControlGood => "central_control_good"
    case CentralControlBad => "central_control_bad"
    case PieceActivationGood => "piece_activation_good"
    case PieceActivationBad => "piece_activation_bad"
    case RookActivationGood => "rook_activation_good"
    case PassedPawnPushGood => "passed_pawn_push_good"
    case KingActivationGood => "king_activation_good"
    case KingActivationBad => "king_activation_bad"
    case PromotionThreat => "promotion_threat"
    case DefensiveConsolidation => "defensive_consolidation"
    case PerpetualCheckAvailable => "perpetual_check_available"
    case PawnBreak(s) => s"pawn_break_$s"

enum TacticTag:
  case GreekGiftSound, GreekGiftUnsound
  case BackRankMatePattern
  case TacticalPatternMiss
  case ForkSound, ForkMiss
  case PinSound, PinMiss
  case SkewerSound
  case DiscoveredAttackSound
  case DecoySound, DeflectionSound, ClearanceSound
  case OverloadingSound, InterferenceSound

  def toSnakeCase: String = this match
    case GreekGiftSound => "greek_gift_sound"
    case GreekGiftUnsound => "greek_gift_unsound"
    case BackRankMatePattern => "back_rank_mate_pattern"
    case TacticalPatternMiss => "tactical_pattern_miss"
    case ForkSound => "fork_sound"
    case ForkMiss => "fork_miss"
    case PinSound => "pin_sound"
    case PinMiss => "pin_miss"
    case SkewerSound => "skewer_sound"
    case DiscoveredAttackSound => "discovered_attack_sound"
    case DecoySound => "decoy_sound"
    case DeflectionSound => "deflection_sound"
    case ClearanceSound => "clearance_sound"
    case OverloadingSound => "overloading_sound"
    case InterferenceSound => "interference_sound"

enum MistakeTag:
  case TacticalMiss
  case PrematurePawnPush
  case PassiveMove
  case MissedCentralBreak
  case Greed
  case Fear
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
  case OppositionGood, OppositionBad
  case Zugzwang

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
  case RestrictedBishop(side: Color)
  case StrongKnight(side: Color)
  case DynamicPosition
  case DryPosition
  case DrawishPosition
  case SpaceAdvantage(side: Color)
  case KingSafetyCrisis(side: Color)
  case TacticalComplexity
  case MaterialImbalance
  case BishopPairAdvantage(side: Color)
  case BadBishop(side: Color)
  case GoodBishop(side: Color)

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
    case DynamicPosition => "dynamic_position"
    case DryPosition => "dry_position"
    case DrawishPosition => "drawish_position"
    case SpaceAdvantage(c) => s"${c.name.toLowerCase}_space_advantage"
    case KingSafetyCrisis(c) => s"${c.name.toLowerCase}_king_safety_crisis"
    case TacticalComplexity => "tactical_complexity"
    case MaterialImbalance => "material_imbalance"
    case BishopPairAdvantage(c) => s"${c.name.toLowerCase}_bishop_pair_advantage"
    case BadBishop(c) => s"${c.name.toLowerCase}_bad_bishop"
    case GoodBishop(c) => s"${c.name.toLowerCase}_good_bishop"

case class ConceptLabels(
    structureTags: List[StructureTag] = Nil,
    planTags: List[PlanTag] = Nil,
    tacticTags: List[TacticTag] = Nil,
    mistakeTags: List[MistakeTag] = Nil,
    endgameTags: List[EndgameTag] = Nil,
    positionalTags: List[PositionalTag] = Nil,
    missedPatternTypes: List[String] = Nil
):
  def allTags: List[String] =
    structureTags.map(_.toString.toLowerCase.replace("$", "_")) ++
      planTags.map(_.toSnakeCase) ++
      tacticTags.map(_.toSnakeCase) ++
      mistakeTags.map(_.toSnakeCase) ++
      endgameTags.map(_.toString.toLowerCase.replace("$", "_")) ++
      positionalTags.map(_.toSnakeCase)

/**
 * Unified Concept Labeler
 * 
 * Combines:
 * - ConceptLabeler: Strategic/structural/tactical tagging
 * - PlanMatcher: High-level plan identification (absorbed as PlanTag enum)
 * 
 * This is the single source of truth for semantic position labeling.
 */
object ConceptLabeler:

  // ============================================================
  // CONSTANTS
  // ============================================================

  val SUCCESS_THRESHOLD_CP = 60
  val FAILURE_THRESHOLD_CP = -60
  val TACTIC_WIN_THRESHOLD_CP = 150
  val BLUNDER_THRESHOLD_CP = 150

  // ============================================================
  // MAIN LABELING ENTRY POINT
  // ============================================================

  def labelPosition(
      features: PositionFeatures,
      evalBefore: Int,
      evalAfter: Int,
      bestEval: Int,
      pos: Position,
      motifs: List[Motif] = Nil,
  ): ConceptLabels =
    val structure = labelStructure(features)
    val plans = labelPlans(features, evalBefore, evalAfter, bestEval)
    val tactics = labelTactics(evalBefore, evalAfter, bestEval, motifs)
    val mistakes = labelMistakes(features, evalBefore, evalAfter, bestEval)
    val endgame = labelEndgame(features, evalBefore, bestEval)
    val positional = labelPositional(pos, pos.color, features)

    ConceptLabels(
      structureTags = structure,
      planTags = plans,
      tacticTags = tactics,
      mistakeTags = mistakes,
      endgameTags = endgame,
      positionalTags = positional
    )

  // ============================================================
  // SUB-LABELERS
  // ============================================================

  private def labelStructure(features: PositionFeatures): List[StructureTag] =
    val tags = List.newBuilder[StructureTag]
    val pawns = features.pawns

    if pawns.whiteIQP then tags += StructureTag.IqpWhite
    if pawns.blackIQP then tags += StructureTag.IqpBlack
    if pawns.whiteHangingPawns then tags += StructureTag.HangingPawnsWhite
    if pawns.blackHangingPawns then tags += StructureTag.HangingPawnsBlack

    val ks = features.kingSafety
    if ks.whiteKingExposedFiles >= 2 then tags += StructureTag.KingExposedWhite
    if ks.blackKingExposedFiles >= 2 then tags += StructureTag.KingExposedBlack

    tags.result().distinct

  private def labelPlans(
      features: PositionFeatures,
      evalBefore: Int,
      evalAfter: Int,
  ): List[PlanTag] =
    val tags = List.newBuilder[PlanTag]
    val phase = features.materialPhase.phase
    val nature = features.nature

    if nature.natureType == NatureType.Static then
      tags += PlanTag.CentralControlGood

    if phase == "endgame" then
      tags += PlanTag.KingActivationGood

    if features.pawns.whitePassedPawns > 0 || features.pawns.blackPassedPawns > 0 then
      if phase == "endgame" then tags += PlanTag.PromotionThreat

    val evalGain = evalAfter - evalBefore
    if evalGain > SUCCESS_THRESHOLD_CP then
      if nature.natureType == NatureType.Chaos then
        tags += PlanTag.KingsideAttackGood
      else
        tags += PlanTag.PieceActivationGood

    // Pawn Breaks
    detectPawnBreaks(features.pawns, features.sideToMove).foreach { sq =>
      tags += PlanTag.PawnBreak(sq)
    }

    tags.result().distinct

  private def labelTactics(
      evalAfter: Int,
      bestEval: Int,
      motifs: List[Motif]
  ): List[TacticTag] =
    val tags = List.newBuilder[TacticTag]
    val deltaToBest = bestEval - evalAfter

    // Logic based on detected motifs
    motifs.foreach {
      case _: PinMotif => tags += TacticTag.PinSound
      case _: ForkMotif => tags += TacticTag.ForkSound
      case _: Skewer => tags += TacticTag.SkewerSound
      case _: DiscoveredAttack => tags += TacticTag.DiscoveredAttackSound
      case _: BackRankMate => tags += TacticTag.BackRankMatePattern
      case _: Overloading => tags += TacticTag.OverloadingSound
      case _: Interference => tags += TacticTag.InterferenceSound
      case _ => ()
    }

    if deltaToBest > BLUNDER_THRESHOLD_CP && motifs.nonEmpty then
      tags += TacticTag.TacticalPatternMiss

    tags.result().distinct

  private def labelMistakes(
      evalBefore: Int,
      evalAfter: Int,
      bestEval: Int
  ): List[MistakeTag] =
    val tags = List.newBuilder[MistakeTag]
    val deltaToBest = bestEval - evalAfter
    val evalDrop = evalBefore - evalAfter

    if deltaToBest > BLUNDER_THRESHOLD_CP then
      tags += MistakeTag.TacticalMiss
      if evalDrop > 50 then
        tags += MistakeTag.PassiveMove

    if evalBefore > 100 && evalDrop > 100 then
      tags += MistakeTag.Fear

    tags.result().distinct

  private def labelEndgame(
      features: PositionFeatures,
      evalBefore: Int, // Eval of current position (parent)
      bestEval: Int // Best eval available from this position
  ): List[EndgameTag] =
    val tags = List.newBuilder[EndgameTag]
    val phase = features.materialPhase.phase

    if phase == "endgame" then
      // Note: RookBehindPassedPawnObeyed should only be tagged if a rook is actually behind a passed pawn.
      // This is a placeholder; real logic would check rook position relative to passed pawns.
      // Removed automatic tagging to avoid false positives.
      
      // Zugzwang Heuristic:
      // If the best move available is significantly worse than the current static evaluation,
      // it implies compulsion to move is detrimental.
      // Note: evalBefore is usually from the Move (previous). But here we assume it's current.
      // If evalBefore (static/parent) ~ 0.00 and bestEval (search) < -100, and it's endgame.
      if (evalBefore > -50 && bestEval < -150) then
        tags += EndgameTag.Zugzwang
      else if (evalBefore > 200 && bestEval < 50) then // Winning to Drawish/Losing?
        tags += EndgameTag.Zugzwang

    tags.result().distinct

  // ============================================================
  // POSITIONAL LABELING (Board-based)
  // ============================================================

  def labelPositional(
      position: Position,
      perspective: Color,
      features: PositionFeatures
  ): List[PositionalTag] =
    val board = position.board
    val tags = List.newBuilder[PositionalTag]
    val oppColor = !perspective

    detectOutposts(board, perspective).foreach { sq =>
      tags += PositionalTag.Outpost(sq.key, perspective)
    }

    detectWeakSquares(board, oppColor).take(2).foreach { sq =>
      tags += PositionalTag.WeakSquare(sq.key, oppColor)
    }

    detectAbuseOfOpenFiles(board, perspective).foreach { file =>
      tags += PositionalTag.OpenFile(file.char.toString, perspective)
    }

    if isBackRankWeak(board, oppColor) then
      tags += PositionalTag.WeakBackRank(oppColor)

    detectLoosePieces(board, oppColor).take(2).foreach { sq =>
      tags += PositionalTag.LoosePiece(sq.key, oppColor)
    }

    val (wBishops, bBishops) = (
      (board.bishops & board.white).count,
      (board.bishops & board.black).count
    )
    if perspective == Color.White && wBishops >= 2 && bBishops < 2 then
      tags += PositionalTag.BishopPairAdvantage(perspective)
    else if perspective == Color.Black && bBishops >= 2 && wBishops < 2 then
      tags += PositionalTag.BishopPairAdvantage(perspective)

    detectBishopQuality(board, perspective).foreach(tags += _)

    if rookOnSeventh(board, perspective) then
      tags += PositionalTag.RookOnSeventh(perspective)
    
    if oppositeColorBishops(board) then
      tags += PositionalTag.OppositeColorBishops

    features.nature.natureType match
      case NatureType.Dynamic | NatureType.Chaos =>
        tags += PositionalTag.DynamicPosition
      case NatureType.Static =>
        tags += PositionalTag.DryPosition
      case _ => ()

    tags.result().distinct

  // ============================================================
  // GEOMETRIC HELPERS
  // ============================================================

  private def detectOutposts(board: Board, color: Color): List[Square] =
    val knights = board.knights & board.byColor(color)
    val pawns = board.pawns & board.byColor(color)
    val oppPawns = board.pawns & board.byColor(!color)
    
    knights.squares.filter { sq =>
      val supported = (sq.pawnAttacks(!color) & pawns).nonEmpty
      val oppPawnAttackers = sq.pawnAttacks(color) & oppPawns
      val isSafeFromPawn = oppPawnAttackers.isEmpty
      
      val isAdvanced = if color == Color.White then sq.rank.value >= 3 && sq.rank.value <= 5
                       else sq.rank.value >= 2 && sq.rank.value <= 4
                       
      supported && isSafeFromPawn && isAdvanced
    }

  private def detectWeakSquares(board: Board, color: Color): List[Square] =
    val pawns = board.pawns & board.byColor(color)
    val rank34 = if color == Color.White then Rank.Third.bb | Rank.Fourth.bb else Rank.Sixth.bb | Rank.Fifth.bb
    val relevantSquares = rank34 & ~pawns
    
    relevantSquares.squares.filter { sq =>
      val protectedByPawn = (sq.pawnAttacks(!color) & pawns).nonEmpty
      !protectedByPawn
    }.take(3)

  private def detectAbuseOfOpenFiles(board: Board, color: Color): List[File] =
    val rooks = board.rooks & board.byColor(color)
    val pawns = board.pawns
    
    rooks.squares.map(_.file).distinct.filter { file =>
      val fileBb = file.bb
      val filePawns = pawns & fileBb
      val ourPawns = filePawns & board.byColor(color)
      ourPawns.isEmpty
    }

  private def isBackRankWeak(board: Board, color: Color): Boolean =
    val king = board.kingPosOf(color)
    val rank = if color == Color.White then Rank.First else Rank.Eighth
    king.exists { k =>
       k.rank == rank && 
       (board.rooks & board.byColor(!color) & rank.bb).nonEmpty
    }

  private def detectLoosePieces(board: Board, color: Color): List[Square] =
    val ourPieces = (board.byColor(color) ^ board.kings) & ~board.pawns
    
    ourPieces.squares.filter { sq =>
      val attackers = board.attackers(sq, !color)
      val defenders = board.attackers(sq, color)
      attackers.nonEmpty && defenders.isEmpty
    }

  private def rookOnSeventh(board: Board, color: Color): Boolean =
    val rooks = board.rooks & board.byColor(color)
    rooks.squares.exists { r =>
      if color == Color.White then r.rank == Rank.Seventh
      else r.rank == Rank.Second
    }

  private def oppositeColorBishops(board: Board): Boolean =
    val whiteBishops = (board.bishops & board.white).squares
    val blackBishops = (board.bishops & board.black).squares
    if whiteBishops.size == 1 && blackBishops.size == 1 then
      whiteBishops.head.isLight != blackBishops.head.isLight
    else false

  private def detectBishopQuality(board: Board, color: Color): List[PositionalTag] =
    val bishops = board.bishops & board.byColor(color)
    val pawns = board.pawns & board.byColor(color)
    val tags = List.newBuilder[PositionalTag]
    
    bishops.squares.foreach { bSq =>
      val bishopIsLight = bSq.isLight
      val sameColorPawns = pawns.squares.count(_.isLight == bishopIsLight)
      val totalPawns = pawns.count
      
      if totalPawns >= 4 && sameColorPawns > (totalPawns / 2) then
        tags += PositionalTag.BadBishop(color)
      else if totalPawns >= 4 && sameColorPawns <= 1 then
        tags += PositionalTag.GoodBishop(color)
    }
    tags.result().distinct

  private def detectPawnBreaks(pawns: PawnStructureFeatures, color: String): List[String] =
    // Placeholder - in real implementation this would analyze potential pawn moves
    // from a move generator or structure lookup.
    // For now, returning empty to allow compilation.
    Nil
