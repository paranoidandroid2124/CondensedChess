package lila.commentary.chess

private[commentary] enum ExplanationClaim:
  case CanWinPiece
  case PieceCanBeTakenWithGain
  case CaptureLeavesMaterialGain
  case ForksTwoTargets
  case AttacksTwoTargets
  case MaterialBalanceChanges
  case LineLeavesMaterialGain
  case ExchangeLeavesSideAhead
  case DefendsPiece
  case PreventsMaterialLoss
  case ProtectsTarget
  case RevealsAttackOnPiece
  case PinsPiece
  case RemovesDefender
  case OverloadsDefender
  case DeflectsDefender
  case SkewersPieceToPiece
  case AdvancesPassedPawn
  case StopsPassedPawnNextAdvance
  case ChallengesPawnDirectly
  case CapturesPawn
  case CreatesPassedPawn
  case OpensFile
  case BlocksPawn
  case CreatesPromotionThreat
  case PromotesPawn
  case GivesCheck
  case EscapesCheck
  case Checkmates
  case Stalemates
  case AttacksQueen
  case AttacksLoosePiece
  case TrapsPiece
  case DecoysPiece

  def key: String =
    this match
      case CanWinPiece => "can_win_piece"
      case PieceCanBeTakenWithGain => "piece_can_be_taken_with_gain"
      case CaptureLeavesMaterialGain => "capture_leaves_material_gain"
      case ForksTwoTargets => "forks_two_targets"
      case AttacksTwoTargets => "attacks_two_targets"
      case MaterialBalanceChanges => "material_balance_changes"
      case LineLeavesMaterialGain => "line_leaves_material_gain"
      case ExchangeLeavesSideAhead => "exchange_leaves_side_ahead"
      case DefendsPiece => "defends_piece"
      case PreventsMaterialLoss => "prevents_material_loss"
      case ProtectsTarget => "protects_target"
      case RevealsAttackOnPiece => "reveals_attack_on_piece"
      case PinsPiece => "pins_piece"
      case RemovesDefender => "removes_defender"
      case OverloadsDefender => "overloads_defender"
      case DeflectsDefender => "deflects_defender"
      case SkewersPieceToPiece => "skewers_piece_to_piece"
      case AdvancesPassedPawn => "advances_passed_pawn"
      case StopsPassedPawnNextAdvance => "stops_pawn_advance"
      case ChallengesPawnDirectly => "challenges_pawn"
      case CapturesPawn => "captures_rival_pawn"
      case CreatesPassedPawn => "creates_passed_pawn"
      case OpensFile => "opens_file"
      case BlocksPawn => "blocks_pawn"
      case CreatesPromotionThreat => "threatens_promotion_next"
      case PromotesPawn => "promotes_pawn"
      case GivesCheck => "gives_check"
      case EscapesCheck => "escapes_check"
      case Checkmates => "checkmates"
      case Stalemates => "stalemates"
      case AttacksQueen => "attacks_queen"
      case AttacksLoosePiece => "attacks_loose_piece"
      case TrapsPiece => "traps_piece"
      case DecoysPiece => "decoys_piece"

private[commentary] object ExplanationClaim:
  val HangingAllowed: Vector[ExplanationClaim] =
    Vector(
      ExplanationClaim.CanWinPiece,
      ExplanationClaim.PieceCanBeTakenWithGain,
      ExplanationClaim.CaptureLeavesMaterialGain
    )

  val HangingForbiddenKeys: Vector[String] =
    Vector(
      "free_piece",
      "blunder",
      "winning_tactic",
      "decisive_tactic",
      "forced_win",
      "best_move",
      "no_counterplay",
      "engine_approved"
    )

  val ForkAllowed: Vector[ExplanationClaim] =
    Vector(
      ExplanationClaim.ForksTwoTargets,
      ExplanationClaim.AttacksTwoTargets
    )

  val ForkForbiddenKeys: Vector[String] =
    Vector(
      "wins_material_by_fork",
      "wins_queen",
      "decisive_fork",
      "forced_win",
      "best_move",
      "no_counterplay"
    )

  val MaterialAllowed: Vector[ExplanationClaim] =
    Vector(
      ExplanationClaim.MaterialBalanceChanges,
      ExplanationClaim.LineLeavesMaterialGain,
      ExplanationClaim.ExchangeLeavesSideAhead
    )

  val MaterialForbiddenKeys: Vector[String] =
    Vector(
      "winning_position",
      "decisive_advantage",
      "conversion",
      "blunder",
      "best_move",
      "forced_win",
      "no_counterplay",
      "line_tactic_identity"
    )

  val DefenseAllowed: Vector[ExplanationClaim] =
    Vector(
      ExplanationClaim.DefendsPiece,
      ExplanationClaim.PreventsMaterialLoss,
      ExplanationClaim.ProtectsTarget
    )

  val DefenseForbiddenKeys: Vector[String] =
    Vector(
      "only_move",
      "best_defense",
      "refutes_attack",
      "stops_counterplay",
      "solves_position",
      "king_safe",
      "mate_defense",
      "no_counterplay"
    )

  val DiscoveredAttackAllowed: Vector[ExplanationClaim] =
    Vector(
      ExplanationClaim.RevealsAttackOnPiece
    )

  val DiscoveredAttackForbiddenKeys: Vector[String] =
    Vector(
      "wins_material",
      "pins_piece",
      "skewers_piece",
      "creates_pressure",
      "takes_initiative",
      "mate_threat",
      "best_move",
      "forced",
      "decisive"
    )

  val PinAllowed: Vector[ExplanationClaim] =
    Vector(
      ExplanationClaim.PinsPiece
    )

  val PinForbiddenKeys: Vector[String] =
    Vector(
      "wins_material",
      "king_unsafe",
      "mate_threat",
      "best_move",
      "only_move",
      "forced",
      "decisive",
      "creates_pressure",
      "takes_initiative",
      "cannot_move"
    )

  val RemoveGuardAllowed: Vector[ExplanationClaim] =
    Vector(
      ExplanationClaim.RemovesDefender
    )

  val OverloadAllowed: Vector[ExplanationClaim] =
    Vector(
      ExplanationClaim.OverloadsDefender
    )

  val DeflectAllowed: Vector[ExplanationClaim] =
    Vector(
      ExplanationClaim.DeflectsDefender
    )

  val RemoveGuardForbiddenKeys: Vector[String] =
    Vector(
      "wins_material",
      "target_is_hanging",
      "no_defense",
      "refutes_defense",
      "best_move",
      "only_move",
      "forced",
      "decisive",
      "creates_pressure",
      "takes_initiative"
    )

  val SkewerAllowed: Vector[ExplanationClaim] =
    Vector(
      ExplanationClaim.SkewersPieceToPiece
    )

  val QueenHitAllowed: Vector[ExplanationClaim] =
    Vector(
      ExplanationClaim.AttacksQueen
    )

  val LooseAllowed: Vector[ExplanationClaim] =
    Vector(
      ExplanationClaim.AttacksLoosePiece
    )

  val TrapAllowed: Vector[ExplanationClaim] =
    Vector(
      ExplanationClaim.TrapsPiece
    )

  val DecoyAllowed: Vector[ExplanationClaim] =
    Vector(
      ExplanationClaim.DecoysPiece
    )

  val QueenHitForbiddenKeys: Vector[String] =
    Vector(
      "wins_queen",
      "traps_queen",
      "gains_tempo",
      "wins_material",
      "best_move",
      "only_move",
      "forced_move",
      "decisive",
      "winning"
    )

  val LooseForbiddenKeys: Vector[String] =
    Vector(
      "hanging_piece",
      "wins_piece",
      "wins_material",
      "attacks_queen",
      "removes_defender",
      "gains_tempo",
      "creates_pressure",
      "best_move",
      "only_move",
      "forced_move",
      "decisive",
      "winning"
    )

  val TrapForbiddenKeys: Vector[String] =
    Vector(
      "wins_piece",
      "wins_material",
      "forced",
      "only_move",
      "best_move",
      "no_escape",
      "cannot_be_saved",
      "no_counterplay",
      "queen_trap",
      "free_piece"
    )

  val DecoyForbiddenKeys: Vector[String] =
    Vector(
      "why_it_matters",
      "traps_piece",
      "wins_piece",
      "wins_material",
      "forced_reply",
      "forced",
      "only_move",
      "best_move",
      "cannot_refuse",
      "no_escape",
      "no_counterplay",
      "deflects_defender",
      "removes_defender",
      "overloads_defender",
      "raw_reply_line"
    )

  val DeflectForbiddenKeys: Vector[String] =
    Vector(
      "wins_material",
      "wins_piece",
      "forced",
      "forced_reply",
      "only_move",
      "best_move",
      "no_defense",
      "no_counterplay",
      "decoy",
      "removes_defender",
      "overloads_defender",
      "traps_piece"
    )

  val SkewerForbiddenKeys: Vector[String] =
    Vector(
      "wins_material",
      "wins_rear_piece",
      "front_piece_must_move",
      "best_move",
      "only_move",
      "forced",
      "decisive",
      "king_unsafe",
      "mate_threat",
      "creates_pressure",
      "takes_initiative"
    )

  val PawnAdvanceAllowed: Vector[ExplanationClaim] =
    Vector(
      ExplanationClaim.AdvancesPassedPawn
    )

  val PawnAdvanceForbiddenKeys: Vector[String] =
    Vector(
      "promotion_threat",
      "unstoppable_pawn",
      "wins_endgame",
      "converts_advantage",
      "queens",
      "promotes_next",
      "clear_path",
      "cannot_be_stopped",
      "pawn_race",
      "passed_pawn_strategy",
      "best_move",
      "only_move",
      "forced",
      "decisive",
      "creates_pressure",
      "takes_initiative"
    )

  val PawnStopAllowed: Vector[ExplanationClaim] =
    Vector(
      ExplanationClaim.StopsPassedPawnNextAdvance
    )

  val PawnBreakAllowed: Vector[ExplanationClaim] =
    Vector(
      ExplanationClaim.ChallengesPawnDirectly
    )

  val PawnCaptureAllowed: Vector[ExplanationClaim] =
    Vector(
      ExplanationClaim.CapturesPawn
    )

  val PassedPawnCreatedAllowed: Vector[ExplanationClaim] =
    Vector(
      ExplanationClaim.CreatesPassedPawn
    )

  val FileOpenedAllowed: Vector[ExplanationClaim] =
    Vector(
      ExplanationClaim.OpensFile
    )

  val PawnBlockAllowed: Vector[ExplanationClaim] =
    Vector(
      ExplanationClaim.BlocksPawn
    )

  val PassedPawnCreatedForbiddenKeys: Vector[String] =
    Vector(
      "unstoppable_pawn",
      "promotion_threat",
      "will_promote",
      "actual_promotion",
      "wins_endgame",
      "converts_advantage",
      "pawn_race",
      "breaks_through",
      "creates_pressure",
      "takes_initiative",
      "best_move",
      "only_move",
      "forced",
      "tablebase_win",
      "no_counterplay"
    )

  val FileOpenedForbiddenKeys: Vector[String] =
    Vector(
      "controls_file",
      "uses_open_file",
      "rook_activity",
      "creates_pressure",
      "takes_initiative",
      "weakens_structure",
      "creates_weakness",
      "breaks_through",
      "wins_space",
      "wins_pawn",
      "wins_material",
      "creates_passed_pawn",
      "promotion_threat",
      "converts_advantage",
      "best_move",
      "only_move",
      "forced"
    )

  val PawnCaptureForbiddenKeys: Vector[String] =
    Vector(
      "wins_pawn",
      "wins_material",
      "creates_passed_pawn",
      "opens_file",
      "weakens_structure",
      "breaks_through",
      "creates_pressure",
      "takes_initiative",
      "best_move",
      "only_move",
      "forced"
    )

  val PawnBreakForbiddenKeys: Vector[String] =
    Vector(
      "opens_position",
      "breaks_through",
      "creates_passed_pawn",
      "weakens_structure",
      "wins_space",
      "best_move",
      "only_move",
      "forced",
      "decisive",
      "creates_pressure",
      "takes_initiative",
      "converts_advantage"
    )

  val PawnBlockForbiddenKeys: Vector[String] =
    Vector(
      "stops_passed_pawn",
      "stops_pawn_advance",
      "challenges_pawn",
      "captures_rival_pawn",
      "creates_passed_pawn",
      "opens_file",
      "wins_pawn",
      "wins_material",
      "weakens_structure",
      "fixes_pawn",
      "creates_blockade",
      "restricts_opponent",
      "creates_pressure",
      "takes_initiative",
      "best_move",
      "only_move",
      "forced"
    )

  val PromotionThreatAllowed: Vector[ExplanationClaim] =
    Vector(
      ExplanationClaim.CreatesPromotionThreat
    )

  val PromotionThreatForbiddenKeys: Vector[String] =
    Vector(
      "unstoppable_pawn",
      "will_promote",
      "cannot_be_stopped",
      "wins_endgame",
      "converts_advantage",
      "best_move",
      "only_move",
      "forced",
      "tablebase_win",
      "no_counterplay"
    )

  val PromotionAllowed: Vector[ExplanationClaim] =
    Vector(
      ExplanationClaim.PromotesPawn
    )

  val CheckGivenAllowed: Vector[ExplanationClaim] =
    Vector(
      ExplanationClaim.GivesCheck
    )

  val CheckGivenForbiddenKeys: Vector[String] =
    Vector(
      "mate_threat",
      "checkmate",
      "king_safety",
      "king_unsafe",
      "attack",
      "attacks_king",
      "creates_attack",
      "creates_pressure",
      "takes_initiative",
      "forced",
      "forces_reply",
      "best_move",
      "only_move",
      "winning",
      "decisive",
      "no_counterplay"
    )

  val CheckEscapedAllowed: Vector[ExplanationClaim] =
    Vector(
      ExplanationClaim.EscapesCheck
    )

  val CheckmateAllowed: Vector[ExplanationClaim] =
    Vector(
      ExplanationClaim.Checkmates
    )

  val StalemateAllowed: Vector[ExplanationClaim] =
    Vector(
      ExplanationClaim.Stalemates
    )

  val CheckEscapedForbiddenKeys: Vector[String] =
    Vector(
      "king_escapes_check",
      "blocks_check",
      "captures_checker",
      "gives_check",
      "mate_threat",
      "checkmate",
      "avoids_mate",
      "king_safety",
      "king_safe",
      "safe_king",
      "king_unsafe",
      "unsafe_king",
      "defense_success",
      "refutes_attack",
      "defends_position",
      "attack",
      "creates_attack",
      "pressure",
      "creates_pressure",
      "initiative",
      "takes_initiative",
      "engine_says_this_escapes_check",
      "eval_number",
      "force",
      "forced",
      "forced_move",
      "forced_escape",
      "forces_reply",
      "best_move",
      "only_move",
      "winning",
      "winning_escape",
      "decisive",
      "decisive_escape",
      "checkmate_defense",
      "raw_pv",
      "no_counterplay"
    )

  val CheckmateForbiddenKeys: Vector[String] =
    Vector(
      "gives_check",
      "escapes_check",
      "mate_threat",
      "mate_in_one",
      "mate_in_n",
      "forced_mate",
      "best_move",
      "only_move",
      "winning",
      "decisive",
      "no_counterplay",
      "king_unsafe",
      "attacks_king",
      "creates_attack",
      "creates_pressure",
      "takes_initiative",
      "engine_says_mate"
    )

  val StalemateForbiddenKeys: Vector[String] =
    Vector(
      "checkmates",
      "gives_check",
      "escapes_check",
      "draws_game",
      "saves_game",
      "throws_win",
      "blunder",
      "tablebase_draw",
      "engine_says_draw",
      "best_move",
      "only_move",
      "forced",
      "winning",
      "losing",
      "decisive",
      "no_counterplay"
    )

  val PromotionForbiddenKeys: Vector[String] =
    Vector(
      "wins_endgame",
      "converts_advantage",
      "decisive",
      "best_move",
      "only_move",
      "forced_win",
      "tablebase_win",
      "unstoppable_pawn",
      "material_gain"
    )

  val PawnStopForbiddenKeys: Vector[String] =
    Vector(
      "stops_promotion",
      "permanently_stops_pawn",
      "draws_endgame",
      "best_defense",
      "only_move",
      "tablebase_draw",
      "wins_endgame",
      "converts_advantage",
      "forced"
    )

private[commentary] enum ExplanationStrength:
  case Bounded

  def key: String =
    this match
      case Bounded => "bounded"

private[commentary] enum ExplanationRelation:
  case SameFamilyLowerRank
  case AlternativeHangingCandidate
  case AlternativeForkCandidate
  case CappedSameStory
  case BlockedByEngineRefute

  def key: String =
    this match
      case SameFamilyLowerRank => "same_family_lower_rank"
      case AlternativeHangingCandidate => "alternative_hanging_candidate"
      case AlternativeForkCandidate => "alternative_fork_candidate"
      case CappedSameStory => "capped_same_story"
      case BlockedByEngineRefute => "blocked_by_engine_refute"

private[commentary] enum ForbiddenWording:
  case FreePiece
  case Blunder
  case Winning
  case Decisive
  case Forced
  case ForcedMove
  case BestMove
  case OnlyMove
  case EngineSays
  case EvalNumber
  case RawPv
  case NoCounterplay
  case KingUnsafe
  case FileControl
  case RookActivity
  case Outpost
  case StrategicKey
  case Conversion
  case MateNet
  case StrongWording
  case WinsPawn
  case PawnForkWins
  case WinsMaterialByFork
  case WinsQueen
  case TrapsQueen
  case QueenIsLost
  case GainsTempo
  case DecisiveFork
  case ForcedWin
  case BestDefense
  case RefutesAttack
  case StopsCounterplay
  case SolvesPosition
  case KingSafe
  case MateDefense
  case WinsMaterial
  case PinsPiece
  case SkewersPiece
  case CreatesPressure
  case TakesInitiative
  case MateThreat
  case CannotMove
  case TargetIsHanging
  case HangingPiece
  case WinsPiece
  case NoDefense
  case RefutesDefense
  case LeavesUndefended
  case NoDefenderRemains
  case RemovesDefender
  case LineTacticIdentity
  case WinsRearPiece
  case FrontPieceMustMove
  case PromotionThreat
  case ActualPromotion
  case UnstoppablePawn
  case WinningEndgame
  case ConvertsAdvantage
  case PromotionStop
  case PermanentStop
  case DrawsEndgame
  case TablebaseDraw
  case TablebaseWin
  case MaterialGain
  case ConversionStopped
  case KingRoute
  case Opposition
  case PawnRace
  case PassedPawnStrategy
  case AdvancesPassedPawn
  case OpensPosition
  case OpensFile
  case ControlsFile
  case UsesOpenFile
  case BreaksThrough
  case CreatesPassedPawn
  case PawnCaptureEvent
  case StopsPassedPawn
  case StopsPawnAdvance
  case ChallengesPawn
  case FixesPawn
  case CreatesBlockade
  case RestrictsOpponent
  case WeakensStructure
  case CreatesWeakness
  case WinsSpace
  case Checkmate
  case KingSafety
  case Attack
  case AttacksKing
  case CreatesAttack
  case AttacksQueen
  case GivesCheck
  case EscapesCheck
  case ForcesReply
  case MateInOne
  case MateInN
  case ForcedMate
  case EngineSaysMate
  case DrawsGame
  case SavesGame
  case ThrowsWin
  case EngineSaysDraw
  case Losing
  case KingMovedOutOfCheck
  case CheckBlocked
  case CheckingPieceCaptured
  case AvoidsMate
  case SafeKing
  case DefenseSuccess
  case NoEscape
  case CannotBeSaved
  case CannotRefuse
  case QueenTrap
  case Decoy
  case DeflectsDefender
  case OverloadsDefender
  case TrapsPiece
  case RawReplyLine
  case WhyItMatters

  def key: String =
    this match
      case FreePiece => "free_piece"
      case Blunder => "blunder"
      case Winning => "winning"
      case Decisive => "decisive"
      case Forced => "forced"
      case ForcedMove => "forced_move"
      case BestMove => "best_move"
      case OnlyMove => "only_move"
      case EngineSays => "engine_says"
      case EvalNumber => "eval_number"
      case RawPv => "raw_pv"
      case NoCounterplay => "no_counterplay"
      case KingUnsafe => "king_unsafe"
      case FileControl => "file_control"
      case RookActivity => "rook_activity"
      case Outpost => "outpost"
      case StrategicKey => "strategic_key"
      case Conversion => "conversion"
      case MateNet => "mate_net"
      case StrongWording => "strong_wording"
      case WinsPawn => "wins_pawn"
      case PawnForkWins => "pawn_fork_wins"
      case WinsMaterialByFork => "wins_material_by_fork"
      case WinsQueen => "wins_queen"
      case TrapsQueen => "traps_queen"
      case QueenIsLost => "queen_lost"
      case GainsTempo => "gains_tempo"
      case DecisiveFork => "decisive_fork"
      case ForcedWin => "forced_win"
      case BestDefense => "best_defense"
      case RefutesAttack => "refutes_attack"
      case StopsCounterplay => "stops_counterplay"
      case SolvesPosition => "solves_position"
      case KingSafe => "king_safe"
      case MateDefense => "mate_defense"
      case WinsMaterial => "wins_material"
      case PinsPiece => "pins_piece"
      case SkewersPiece => "skewers_piece"
      case CreatesPressure => "creates_pressure"
      case TakesInitiative => "takes_initiative"
      case MateThreat => "mate_threat"
      case CannotMove => "cannot_move"
      case TargetIsHanging => "target_is_hanging"
      case HangingPiece => "hanging_piece"
      case WinsPiece => "wins_piece"
      case NoDefense => "no_defense"
      case RefutesDefense => "refutes_defense"
      case LeavesUndefended => "leaves_undefended"
      case NoDefenderRemains => "no_defender_remains"
      case RemovesDefender => "removes_defender"
      case LineTacticIdentity => "line_tactic_identity"
      case WinsRearPiece => "wins_rear_piece"
      case FrontPieceMustMove => "front_piece_must_move"
      case PromotionThreat => "promotion_threat"
      case ActualPromotion => "actual_promotion"
      case UnstoppablePawn => "unstoppable_pawn"
      case WinningEndgame => "wins_endgame"
      case ConvertsAdvantage => "converts_advantage"
      case PromotionStop => "stops_promotion"
      case PermanentStop => "permanently_stops_pawn"
      case DrawsEndgame => "draws_endgame"
      case TablebaseDraw => "tablebase_draw"
      case TablebaseWin => "tablebase_win"
      case MaterialGain => "material_gain"
      case ConversionStopped => "conversion_stopped"
      case KingRoute => "king_route"
      case Opposition => "opposition"
      case PawnRace => "pawn_race"
      case PassedPawnStrategy => "passed_pawn_strategy"
      case AdvancesPassedPawn => "advances_passed_pawn"
      case OpensPosition => "opens_position"
      case OpensFile => "opens_file"
      case ControlsFile => "controls_file"
      case UsesOpenFile => "uses_open_file"
      case BreaksThrough => "breaks_through"
      case CreatesPassedPawn => "creates_passed_pawn"
      case PawnCaptureEvent => "captures_rival_pawn"
      case StopsPassedPawn => "stops_passed_pawn"
      case StopsPawnAdvance => "stops_pawn_advance"
      case ChallengesPawn => "challenges_pawn"
      case FixesPawn => "fixes_pawn"
      case CreatesBlockade => "creates_blockade"
      case RestrictsOpponent => "restricts_opponent"
      case WeakensStructure => "weakens_structure"
      case CreatesWeakness => "creates_weakness"
      case WinsSpace => "wins_space"
      case Checkmate => "checkmate"
      case KingSafety => "king_safety"
      case Attack => "attack"
      case AttacksKing => "attacks_king"
      case CreatesAttack => "creates_attack"
      case AttacksQueen => "attacks_queen"
      case GivesCheck => "gives_check"
      case EscapesCheck => "escapes_check"
      case ForcesReply => "forces_reply"
      case MateInOne => "mate_in_one"
      case MateInN => "mate_in_n"
      case ForcedMate => "forced_mate"
      case EngineSaysMate => "engine_says_mate"
      case DrawsGame => "draws_game"
      case SavesGame => "saves_game"
      case ThrowsWin => "throws_win"
      case EngineSaysDraw => "engine_says_draw"
      case Losing => "losing"
      case KingMovedOutOfCheck => "king_moved_out_of_check"
      case CheckBlocked => "blocks_check"
      case CheckingPieceCaptured => "captures_checker"
      case AvoidsMate => "avoids_mate"
      case SafeKing => "safe_king"
      case DefenseSuccess => "defense_success"
      case NoEscape => "no_escape"
      case CannotBeSaved => "cannot_be_saved"
      case CannotRefuse => "cannot_refuse"
      case QueenTrap => "queen_trap"
      case Decoy => "decoy"
      case DeflectsDefender => "deflects_defender"
      case OverloadsDefender => "overloads_defender"
      case TrapsPiece => "traps_piece"
      case RawReplyLine => "raw_reply_line"
      case WhyItMatters => "why_it_matters"

private[commentary] object ForbiddenWording:
  val Basic: Vector[ForbiddenWording] =
    Vector(
      ForbiddenWording.FreePiece,
      ForbiddenWording.Blunder,
      ForbiddenWording.Winning,
      ForbiddenWording.Decisive,
      ForbiddenWording.Forced,
      ForbiddenWording.BestMove,
      ForbiddenWording.OnlyMove,
      ForbiddenWording.EngineSays,
      ForbiddenWording.NoCounterplay,
      ForbiddenWording.KingUnsafe,
      ForbiddenWording.FileControl,
      ForbiddenWording.Outpost,
      ForbiddenWording.StrategicKey,
      ForbiddenWording.Conversion,
      ForbiddenWording.MateNet
    )

private[commentary] final case class ExplanationPlan(
    role: Role,
    scene: Scene,
    tactic: Option[Tactic],
    side: Side,
    rival: Side,
    target: Option[Square],
    anchor: Option[Square],
    route: Option[Line],
    routeSan: Option[String],
    secondaryTarget: Option[Square],
    allowedClaim: Option[ExplanationClaim],
    evidenceLine: Option[Line],
    strength: ExplanationStrength,
    forbiddenWording: Vector[ForbiddenWording],
    relations: Vector[ExplanationRelation],
    debugOnly: Boolean,
    supportContextLinks: Vector[ExplanationPlan.Link]
)

private[commentary] object ExplanationPlan:
  final case class Link(
      role: Role,
      scene: Scene,
      tactic: Option[Tactic],
      side: Side,
      target: Option[Square],
      route: Option[Line],
      routeSan: Option[String]
  )

  private val HangingForbiddenWording = ForbiddenWording.Basic
  private val ForkForbiddenWording =
    ForbiddenWording.Basic ++
      Vector(
        ForbiddenWording.WinsMaterialByFork,
        ForbiddenWording.WinsMaterial,
        ForbiddenWording.WinsPawn,
        ForbiddenWording.PawnForkWins,
        ForbiddenWording.EvalNumber,
        ForbiddenWording.RawPv,
        ForbiddenWording.WinsQueen,
        ForbiddenWording.DecisiveFork,
        ForbiddenWording.ForcedWin,
        ForbiddenWording.Winning
      )
  private val MaterialForbiddenWording =
    ForbiddenWording.Basic ++
      Vector(
        ForbiddenWording.ForcedWin,
        ForbiddenWording.LineTacticIdentity
      )
  private val DefenseForbiddenWording =
    ForbiddenWording.Basic ++
      Vector(
        ForbiddenWording.BestDefense,
        ForbiddenWording.RefutesAttack,
        ForbiddenWording.StopsCounterplay,
        ForbiddenWording.SolvesPosition,
        ForbiddenWording.KingSafe,
        ForbiddenWording.MateDefense
      )
  private val LineDefenderForbiddenWording =
    Vector(
      ForbiddenWording.WinsMaterial,
      ForbiddenWording.Winning,
      ForbiddenWording.Decisive,
      ForbiddenWording.BestMove,
      ForbiddenWording.OnlyMove,
      ForbiddenWording.Forced,
      ForbiddenWording.CannotMove,
      ForbiddenWording.NoDefense,
      ForbiddenWording.FrontPieceMustMove,
      ForbiddenWording.WinsRearPiece,
      ForbiddenWording.CreatesPressure,
      ForbiddenWording.TakesInitiative,
      ForbiddenWording.MateThreat,
      ForbiddenWording.KingUnsafe
    )
  private val DiscoveredAttackForbiddenWording =
    (
      LineDefenderForbiddenWording ++
        Vector(
          ForbiddenWording.PinsPiece,
          ForbiddenWording.SkewersPiece
        )
    ).distinct
  private val PinForbiddenWording =
    LineDefenderForbiddenWording
  private val RemoveGuardForbiddenWording =
    (
      LineDefenderForbiddenWording ++
        Vector(
          ForbiddenWording.TargetIsHanging,
          ForbiddenWording.LeavesUndefended,
          ForbiddenWording.NoDefenderRemains,
          ForbiddenWording.RefutesDefense
        )
    ).distinct
  private val OverloadForbiddenWording =
    (
      LineDefenderForbiddenWording ++
        Vector(
          ForbiddenWording.HangingPiece,
          ForbiddenWording.TargetIsHanging,
          ForbiddenWording.RemovesDefender,
          ForbiddenWording.WinsPiece,
          ForbiddenWording.WinsMaterial,
          ForbiddenWording.GainsTempo,
          ForbiddenWording.ForcedMove
        )
    ).distinct
  private val DeflectForbiddenWording =
    (
      LineDefenderForbiddenWording ++
        Vector(
          ForbiddenWording.WinsPiece,
          ForbiddenWording.WinsMaterial,
          ForbiddenWording.ForcesReply,
          ForbiddenWording.Decoy,
          ForbiddenWording.RemovesDefender,
          ForbiddenWording.OverloadsDefender,
          ForbiddenWording.TrapsPiece
        )
    ).distinct
  private val SkewerForbiddenWording =
    LineDefenderForbiddenWording
  private val QueenHitForbiddenWording =
    (
      ForbiddenWording.Basic ++
        Vector(
          ForbiddenWording.WinsQueen,
          ForbiddenWording.TrapsQueen,
          ForbiddenWording.QueenIsLost,
          ForbiddenWording.GainsTempo,
          ForbiddenWording.WinsMaterial,
          ForbiddenWording.ForcedMove
        )
    ).distinct
  private val LooseForbiddenWording =
    (
      ForbiddenWording.Basic ++
        Vector(
          ForbiddenWording.HangingPiece,
          ForbiddenWording.TargetIsHanging,
          ForbiddenWording.WinsPiece,
          ForbiddenWording.WinsMaterial,
          ForbiddenWording.AttacksQueen,
          ForbiddenWording.RemovesDefender,
          ForbiddenWording.GainsTempo,
          ForbiddenWording.CreatesPressure,
          ForbiddenWording.ForcedMove
        )
    ).distinct
  private val TrapForbiddenWording =
    (
      ForbiddenWording.Basic ++
        Vector(
          ForbiddenWording.WinsPiece,
          ForbiddenWording.WinsMaterial,
          ForbiddenWording.NoEscape,
          ForbiddenWording.CannotBeSaved,
          ForbiddenWording.QueenTrap
        )
    ).distinct
  private val DecoyForbiddenWording =
    (
      ForbiddenWording.Basic ++
        Vector(
          ForbiddenWording.WhyItMatters,
          ForbiddenWording.TrapsPiece,
          ForbiddenWording.WinsPiece,
          ForbiddenWording.WinsMaterial,
          ForbiddenWording.ForcesReply,
          ForbiddenWording.CannotRefuse,
          ForbiddenWording.NoEscape,
          ForbiddenWording.DeflectsDefender,
          ForbiddenWording.RemovesDefender,
          ForbiddenWording.OverloadsDefender,
          ForbiddenWording.RawReplyLine
        )
    ).distinct
  private val PawnInteractionForbiddenWording =
    ForbiddenWording.Basic ++
      Vector(
        ForbiddenWording.PromotionThreat,
        ForbiddenWording.UnstoppablePawn,
        ForbiddenWording.WinningEndgame,
        ForbiddenWording.ConvertsAdvantage,
        ForbiddenWording.DrawsEndgame,
        ForbiddenWording.TablebaseDraw,
        ForbiddenWording.PromotionStop,
        ForbiddenWording.PermanentStop,
        ForbiddenWording.Decisive,
        ForbiddenWording.CreatesPressure,
        ForbiddenWording.TakesInitiative,
        ForbiddenWording.PawnRace,
        ForbiddenWording.KingRoute,
        ForbiddenWording.Opposition,
        ForbiddenWording.PassedPawnStrategy,
        ForbiddenWording.RookActivity,
        ForbiddenWording.WeakensStructure,
        ForbiddenWording.CreatesWeakness,
        ForbiddenWording.BreaksThrough,
        ForbiddenWording.WinsSpace,
        ForbiddenWording.WinsPawn,
        ForbiddenWording.WinsMaterial
      )
  private val PawnAdvanceForbiddenWording =
    PawnInteractionForbiddenWording
  private val PawnStopForbiddenWording =
    (
      PawnInteractionForbiddenWording ++
        Vector(
          ForbiddenWording.BestDefense
        )
    ).distinct
  private val PawnBreakForbiddenWording =
    (
      PawnInteractionForbiddenWording ++
        Vector(
          ForbiddenWording.OpensPosition,
          ForbiddenWording.OpensFile,
          ForbiddenWording.BreaksThrough,
          ForbiddenWording.CreatesPassedPawn,
          ForbiddenWording.WeakensStructure,
          ForbiddenWording.WinsSpace,
          ForbiddenWording.WinsPawn,
          ForbiddenWording.WinsMaterial,
          ForbiddenWording.NoCounterplay,
          ForbiddenWording.MaterialGain
        )
    ).distinct
  private val PawnCaptureForbiddenWording =
    (
      PawnInteractionForbiddenWording ++
        Vector(
          ForbiddenWording.OpensPosition,
          ForbiddenWording.OpensFile,
          ForbiddenWording.BreaksThrough,
          ForbiddenWording.CreatesPassedPawn,
          ForbiddenWording.WeakensStructure,
          ForbiddenWording.WinsSpace,
          ForbiddenWording.NoCounterplay,
          ForbiddenWording.MaterialGain,
          ForbiddenWording.WinsPawn,
          ForbiddenWording.WinsMaterial,
          ForbiddenWording.FileControl
        )
    ).distinct
  private val PassedPawnCreatedForbiddenWording =
    (
      PawnInteractionForbiddenWording.filterNot(_ == ForbiddenWording.PassedPawnStrategy) ++
        Vector(
          ForbiddenWording.AdvancesPassedPawn,
          ForbiddenWording.ActualPromotion,
          ForbiddenWording.OpensPosition,
          ForbiddenWording.OpensFile,
          ForbiddenWording.BreaksThrough,
          ForbiddenWording.PawnCaptureEvent,
          ForbiddenWording.WeakensStructure,
          ForbiddenWording.WinsSpace,
          ForbiddenWording.NoCounterplay,
          ForbiddenWording.MaterialGain,
          ForbiddenWording.WinsPawn,
          ForbiddenWording.WinsMaterial,
          ForbiddenWording.TablebaseWin
        )
    ).distinct
  private val FileOpenedForbiddenWording =
    (
      PawnInteractionForbiddenWording.filterNot(_ == ForbiddenWording.FileControl) ++
        Vector(
          ForbiddenWording.ControlsFile,
          ForbiddenWording.UsesOpenFile,
          ForbiddenWording.RookActivity,
          ForbiddenWording.WeakensStructure,
          ForbiddenWording.CreatesWeakness,
          ForbiddenWording.BreaksThrough,
          ForbiddenWording.WinsSpace,
          ForbiddenWording.WinsPawn,
          ForbiddenWording.WinsMaterial,
          ForbiddenWording.MaterialGain,
          ForbiddenWording.CreatesPassedPawn,
          ForbiddenWording.PromotionThreat,
          ForbiddenWording.ActualPromotion,
          ForbiddenWording.ConvertsAdvantage,
          ForbiddenWording.NoCounterplay
        )
    ).distinct
  private val PawnBlockForbiddenWording =
    (
      PawnInteractionForbiddenWording ++
        Vector(
          ForbiddenWording.StopsPassedPawn,
          ForbiddenWording.StopsPawnAdvance,
          ForbiddenWording.ChallengesPawn,
          ForbiddenWording.PawnCaptureEvent,
          ForbiddenWording.CreatesPassedPawn,
          ForbiddenWording.OpensFile,
          ForbiddenWording.WinsPawn,
          ForbiddenWording.WinsMaterial,
          ForbiddenWording.WeakensStructure,
          ForbiddenWording.FixesPawn,
          ForbiddenWording.CreatesBlockade,
          ForbiddenWording.RestrictsOpponent,
          ForbiddenWording.CreatesPressure,
          ForbiddenWording.TakesInitiative,
          ForbiddenWording.BestMove,
          ForbiddenWording.OnlyMove,
          ForbiddenWording.Forced
        )
    ).distinct
  private val PromotionThreatForbiddenWording =
    ForbiddenWording.Basic ++
      Vector(
        ForbiddenWording.ActualPromotion,
        ForbiddenWording.UnstoppablePawn,
        ForbiddenWording.WinningEndgame,
        ForbiddenWording.ConvertsAdvantage,
        ForbiddenWording.DrawsEndgame,
        ForbiddenWording.TablebaseDraw,
        ForbiddenWording.PromotionStop,
        ForbiddenWording.PermanentStop,
        ForbiddenWording.Decisive,
        ForbiddenWording.NoCounterplay,
        ForbiddenWording.PawnRace,
        ForbiddenWording.KingRoute,
        ForbiddenWording.Opposition,
        ForbiddenWording.PassedPawnStrategy,
        ForbiddenWording.RookActivity,
        ForbiddenWording.WeakensStructure,
        ForbiddenWording.CreatesWeakness,
        ForbiddenWording.BreaksThrough,
        ForbiddenWording.WinsSpace,
        ForbiddenWording.WinsPawn,
        ForbiddenWording.WinsMaterial
      )
  private val PromotionForbiddenWording =
    ForbiddenWording.Basic ++
      Vector(
        ForbiddenWording.WinningEndgame,
        ForbiddenWording.ConvertsAdvantage,
        ForbiddenWording.ForcedWin,
        ForbiddenWording.TablebaseWin,
        ForbiddenWording.UnstoppablePawn,
        ForbiddenWording.MaterialGain
      )
  private val CheckGivenForbiddenWording =
    Vector(
      ForbiddenWording.MateThreat,
      ForbiddenWording.Checkmate,
      ForbiddenWording.KingSafety,
      ForbiddenWording.KingSafe,
      ForbiddenWording.SafeKing,
      ForbiddenWording.KingUnsafe,
      ForbiddenWording.Attack,
      ForbiddenWording.AttacksKing,
      ForbiddenWording.CreatesAttack,
      ForbiddenWording.CreatesPressure,
      ForbiddenWording.TakesInitiative,
      ForbiddenWording.Forced,
      ForbiddenWording.ForcesReply,
      ForbiddenWording.BestMove,
      ForbiddenWording.OnlyMove,
      ForbiddenWording.Winning,
      ForbiddenWording.Decisive,
      ForbiddenWording.NoCounterplay,
      ForbiddenWording.EngineSays,
      ForbiddenWording.EngineSaysMate
    )
  private val CheckEscapedForbiddenWording =
    Vector(
      ForbiddenWording.KingMovedOutOfCheck,
      ForbiddenWording.CheckBlocked,
      ForbiddenWording.CheckingPieceCaptured,
      ForbiddenWording.GivesCheck,
      ForbiddenWording.MateThreat,
      ForbiddenWording.Checkmate,
      ForbiddenWording.AvoidsMate,
      ForbiddenWording.KingSafety,
      ForbiddenWording.KingSafe,
      ForbiddenWording.SafeKing,
      ForbiddenWording.KingUnsafe,
      ForbiddenWording.BestDefense,
      ForbiddenWording.DefenseSuccess,
      ForbiddenWording.RefutesAttack,
      ForbiddenWording.Attack,
      ForbiddenWording.CreatesAttack,
      ForbiddenWording.CreatesPressure,
      ForbiddenWording.TakesInitiative,
      ForbiddenWording.Forced,
      ForbiddenWording.ForcesReply,
      ForbiddenWording.BestMove,
      ForbiddenWording.OnlyMove,
      ForbiddenWording.Winning,
      ForbiddenWording.Decisive,
      ForbiddenWording.NoCounterplay,
      ForbiddenWording.EngineSays,
      ForbiddenWording.EngineSaysMate
    )
  private val CheckmateForbiddenWording =
    Vector(
      ForbiddenWording.GivesCheck,
      ForbiddenWording.EscapesCheck,
      ForbiddenWording.MateThreat,
      ForbiddenWording.MateInOne,
      ForbiddenWording.MateInN,
      ForbiddenWording.ForcedMate,
      ForbiddenWording.BestMove,
      ForbiddenWording.OnlyMove,
      ForbiddenWording.Winning,
      ForbiddenWording.Decisive,
      ForbiddenWording.NoCounterplay,
      ForbiddenWording.KingSafety,
      ForbiddenWording.KingSafe,
      ForbiddenWording.SafeKing,
      ForbiddenWording.KingUnsafe,
      ForbiddenWording.Attack,
      ForbiddenWording.AttacksKing,
      ForbiddenWording.CreatesAttack,
      ForbiddenWording.CreatesPressure,
      ForbiddenWording.TakesInitiative,
      ForbiddenWording.EngineSays,
      ForbiddenWording.EngineSaysMate
    )
  private val StalemateForbiddenWording =
    Vector(
      ForbiddenWording.Checkmate,
      ForbiddenWording.GivesCheck,
      ForbiddenWording.EscapesCheck,
      ForbiddenWording.DrawsGame,
      ForbiddenWording.SavesGame,
      ForbiddenWording.ThrowsWin,
      ForbiddenWording.Blunder,
      ForbiddenWording.TablebaseDraw,
      ForbiddenWording.EngineSaysDraw,
      ForbiddenWording.BestMove,
      ForbiddenWording.OnlyMove,
      ForbiddenWording.Forced,
      ForbiddenWording.Winning,
      ForbiddenWording.Losing,
      ForbiddenWording.Decisive,
      ForbiddenWording.NoCounterplay
    )

  private def tacticAllowedClaim(verdict: Verdict, tactic: Tactic) =
    if verdict.role == Role.Lead && verdict.leadAllowed && !verdict.engineStrengthLimited then
      tactic match
        case Tactic.Hanging => Some(ExplanationClaim.CanWinPiece)
        case Tactic.Fork => Some(ExplanationClaim.ForksTwoTargets)
        case _ => None
    else None

  private def materialAllowedClaim(verdict: Verdict) =
    Option.when(verdict.role == Role.Lead && verdict.leadAllowed && !verdict.engineStrengthLimited)(
      ExplanationClaim.MaterialBalanceChanges
    )

  private def defenseAllowedClaim(verdict: Verdict) =
    Option.when(verdict.role == Role.Lead && verdict.leadAllowed && !verdict.engineStrengthLimited)(
      ExplanationClaim.DefendsPiece
    )

  private def pawnAdvanceAllowedClaim(verdict: Verdict) =
    Option.when(verdict.role == Role.Lead && verdict.leadAllowed && !verdict.engineStrengthLimited)(
      ExplanationClaim.AdvancesPassedPawn
    )

  private def pawnStopAllowedClaim(verdict: Verdict) =
    Option.when(verdict.role == Role.Lead && verdict.leadAllowed && !verdict.engineStrengthLimited)(
      ExplanationClaim.StopsPassedPawnNextAdvance
    )

  private def pawnBreakAllowedClaim(verdict: Verdict) =
    Option.when(verdict.role == Role.Lead && verdict.leadAllowed && !verdict.engineStrengthLimited)(
      ExplanationClaim.ChallengesPawnDirectly
    )

  private def pawnCaptureAllowedClaim(verdict: Verdict) =
    Option.when(verdict.role == Role.Lead && verdict.leadAllowed && !verdict.engineStrengthLimited)(
      ExplanationClaim.CapturesPawn
    )

  private def passedPawnCreatedAllowedClaim(verdict: Verdict) =
    Option.when(verdict.role == Role.Lead && verdict.leadAllowed && !verdict.engineStrengthLimited)(
      ExplanationClaim.CreatesPassedPawn
    )

  private def fileOpenedAllowedClaim(verdict: Verdict) =
    Option.when(verdict.role == Role.Lead && verdict.leadAllowed && !verdict.engineStrengthLimited)(
      ExplanationClaim.OpensFile
    )

  private def pawnBlockAllowedClaim(verdict: Verdict) =
    Option.when(verdict.role == Role.Lead && verdict.leadAllowed && !verdict.engineStrengthLimited)(
      ExplanationClaim.BlocksPawn
    )

  private def promotionThreatAllowedClaim(verdict: Verdict) =
    Option.when(verdict.role == Role.Lead && verdict.leadAllowed && !verdict.engineStrengthLimited)(
      ExplanationClaim.CreatesPromotionThreat
    )

  private def promotionAllowedClaim(verdict: Verdict) =
    Option.when(verdict.role == Role.Lead && verdict.leadAllowed && !verdict.engineStrengthLimited)(
      ExplanationClaim.PromotesPawn
    )

  private def checkGivenAllowedClaim(verdict: Verdict) =
    Option.when(verdict.role == Role.Lead && verdict.leadAllowed && !verdict.engineStrengthLimited)(
      ExplanationClaim.GivesCheck
    )

  private def checkEscapedAllowedClaim(verdict: Verdict) =
    Option.when(verdict.role == Role.Lead && verdict.leadAllowed && !verdict.engineStrengthLimited)(
      ExplanationClaim.EscapesCheck
    )

  private def checkmateAllowedClaim(verdict: Verdict) =
    Option.when(verdict.role == Role.Lead && verdict.leadAllowed && !verdict.engineStrengthLimited)(
      ExplanationClaim.Checkmates
    )

  private def stalemateAllowedClaim(verdict: Verdict) =
    Option.when(verdict.role == Role.Lead && verdict.leadAllowed && !verdict.engineStrengthLimited)(
      ExplanationClaim.Stalemates
    )

  private def forbiddenWording(verdict: Verdict, tactic: Tactic) =
    val base =
      tactic match
        case Tactic.Fork => ForkForbiddenWording
        case _ => HangingForbiddenWording
    if verdict.engineStrengthLimited then base :+ ForbiddenWording.StrongWording
    else base

  private def discoveredAttackAllowedClaim(verdict: Verdict) =
    Option.when(verdict.role == Role.Lead && verdict.leadAllowed && !verdict.engineStrengthLimited)(
      ExplanationClaim.RevealsAttackOnPiece
    )

  private def discoveredAttackCanPlan(verdict: Verdict) =
    verdict.selected &&
      verdict.role == Role.Lead &&
      verdict.leadAllowed &&
      !verdict.engineStrengthLimited &&
      !verdict.engineCheckStatus.contains(EngineCheckStatus.Refutes)

  private def pinCanPlan(verdict: Verdict) =
    verdict.selected &&
      verdict.role == Role.Lead &&
      verdict.leadAllowed &&
      !verdict.engineStrengthLimited &&
      !verdict.engineCheckStatus.contains(EngineCheckStatus.Refutes)

  private def removeGuardCanPlan(verdict: Verdict) =
    verdict.selected &&
      verdict.role == Role.Lead &&
      verdict.leadAllowed &&
      !verdict.engineStrengthLimited &&
      !verdict.engineCheckStatus.contains(EngineCheckStatus.Refutes)

  private def overloadCanPlan(verdict: Verdict) =
    verdict.selected &&
      verdict.role == Role.Lead &&
      verdict.leadAllowed &&
      !verdict.engineStrengthLimited &&
      !verdict.engineCheckStatus.contains(EngineCheckStatus.Refutes)

  private def deflectCanPlan(verdict: Verdict) =
    verdict.selected &&
      verdict.role == Role.Lead &&
      verdict.leadAllowed &&
      !verdict.engineStrengthLimited &&
      verdict.engineCheckStatus.forall(_ == EngineCheckStatus.Supports)

  private def skewerCanPlan(verdict: Verdict) =
    verdict.selected &&
      verdict.role == Role.Lead &&
      verdict.leadAllowed &&
      !verdict.engineStrengthLimited &&
      !verdict.engineCheckStatus.contains(EngineCheckStatus.Refutes)

  private def queenHitCanPlan(verdict: Verdict) =
    verdict.selected &&
      verdict.role == Role.Lead &&
      verdict.leadAllowed &&
      !verdict.engineStrengthLimited &&
      !verdict.engineCheckStatus.contains(EngineCheckStatus.Refutes)

  private def looseCanPlan(verdict: Verdict) =
    verdict.selected &&
      verdict.role == Role.Lead &&
      verdict.leadAllowed &&
      !verdict.engineStrengthLimited &&
      !verdict.engineCheckStatus.contains(EngineCheckStatus.Refutes)

  private def trapCanPlan(verdict: Verdict) =
    verdict.selected &&
      verdict.role == Role.Lead &&
      verdict.leadAllowed &&
      !verdict.engineStrengthLimited &&
      verdict.engineCheckStatus.forall(_ == EngineCheckStatus.Supports)

  private def decoyCanPlan(verdict: Verdict) =
    verdict.selected &&
      verdict.role == Role.Lead &&
      verdict.leadAllowed &&
      !verdict.engineStrengthLimited &&
      verdict.engineCheckStatus.forall(_ == EngineCheckStatus.Supports)

  private def isDecoyShaped(story: Story) =
    story.writer.contains(StoryWriter.TacticDecoy) ||
      story.tactic.contains(Tactic.Decoy) ||
      story.decoyProof.nonEmpty

  private def decoyPlanStory(story: Story) =
    story.writer.contains(StoryWriter.TacticDecoy) &&
      story.scene == Scene.Tactic &&
      story.tactic.contains(Tactic.Decoy) &&
      story.plan.isEmpty &&
      story.captureResult.isEmpty &&
      story.threatProof.isEmpty &&
      story.defenseProof.isEmpty &&
      story.multiTargetProof.isEmpty &&
      story.lineProof.isEmpty &&
      story.pinProof.isEmpty &&
      story.removeGuardProof.isEmpty &&
      story.overloadProof.isEmpty &&
      story.deflectProof.isEmpty &&
      story.skewerProof.isEmpty &&
      story.queenHitProof.isEmpty &&
      story.loosePieceProof.isEmpty &&
      story.trapProof.isEmpty &&
      story.fileOpenedProof.isEmpty &&
      story.secondaryTarget.isEmpty &&
      story.proofFailures.isEmpty &&
      story.decoyProof.exists(decoyProofBindsPlan(story, _))

  private def decoyProofBindsPlan(story: Story, proof: DecoyProof) =
    proof.complete &&
      proof.sameBoardProof &&
      proof.legalSideMove &&
      proof.legalRivalReply &&
      proof.replyByNamedPiece &&
      proof.rivalPieceRemainsAfterReply &&
      proof.replyLandsOnDecoySquare &&
      proof.decoySquareEqualsLandingSquare &&
      proof.completeTrapFollowUpProof &&
      proof.trapFollowUpBindsSamePieceAndSquare &&
      proof.completeStoryProof &&
      proof.noEngineEvidenceUsed &&
      proof.side == story.side &&
      proof.rivalSide == story.rival &&
      proof.sideMove.exists(move => story.route.contains(move)) &&
      proof.rivalReply.nonEmpty &&
      proof.decoySquare == story.target &&
      proof.landingSquare == story.target &&
      proof.namedPieceAfterReply.exists(piece =>
        story.rival == piece.side &&
          story.target.contains(piece.square)
      ) &&
      proof.afterSideMoveBoard.exists(piece =>
        story.side == piece.side &&
          story.anchor.contains(piece.square) &&
          proof.sideMove.exists(_.to == piece.square)
      )

  private def isTrapShaped(story: Story) =
    story.writer.contains(StoryWriter.TacticTrap) ||
      story.tactic.contains(Tactic.Trap) ||
      story.trapProof.nonEmpty

  private def trapPlanStory(story: Story) =
    story.writer.contains(StoryWriter.TacticTrap) &&
      story.scene == Scene.Tactic &&
      story.tactic.contains(Tactic.Trap) &&
      story.plan.isEmpty &&
      story.captureResult.isEmpty &&
      story.threatProof.isEmpty &&
      story.defenseProof.isEmpty &&
      story.multiTargetProof.isEmpty &&
      story.lineProof.isEmpty &&
      story.pinProof.isEmpty &&
      story.removeGuardProof.isEmpty &&
      story.overloadProof.isEmpty &&
      story.skewerProof.isEmpty &&
      story.queenHitProof.isEmpty &&
      story.loosePieceProof.isEmpty &&
      story.fileOpenedProof.isEmpty &&
      story.secondaryTarget.isEmpty &&
      story.proofFailures.isEmpty &&
      story.trapProof.exists(trapProofBindsPlan(story, _))

  private def trapProofBindsPlan(story: Story, proof: TrapProof) =
    proof.complete &&
      proof.sameBoardProof &&
      proof.legalMove &&
      proof.nonCapturingMove &&
      proof.exactAfterBoardReplay &&
      proof.completeStoryProof &&
      proof.targetRivalMinor &&
      proof.targetDefendedByRivalSide &&
      proof.afterBoardTargetAttackedByMovingSide &&
      proof.targetHasLegalMoves &&
      proof.everyTargetMoveUnsafe &&
      proof.routeDoesNotGiveCheck &&
      proof.routeDoesNotGiveMate &&
      proof.routeDoesNotPromote &&
      proof.noEngineEvidenceUsed &&
      proof.attackingSide == story.side &&
      proof.rivalSide == story.rival &&
      proof.trapMove.exists(move => story.route.contains(move)) &&
      proof.targetPieceSquareAfter == story.target &&
      proof.anchorSquareAfter == story.anchor &&
      proof.targetPieceAfter.exists(piece =>
        story.rival == piece.side &&
          (piece.man == Man.Knight || piece.man == Man.Bishop) &&
          proof.targetPieceSquareAfter.contains(piece.square)
      ) &&
      proof.movingPieceAfter.exists(piece =>
        story.side == piece.side &&
          proof.anchorSquareAfter.contains(piece.square) &&
          story.anchor.contains(piece.square)
      )

  private def isOverloadShaped(story: Story) =
    story.writer.contains(StoryWriter.TacticOverload) ||
      story.tactic.contains(Tactic.Overload) ||
      story.overloadProof.nonEmpty

  private def overloadPlanStory(story: Story) =
    story.writer.contains(StoryWriter.TacticOverload) &&
      story.scene == Scene.Tactic &&
      story.tactic.contains(Tactic.Overload) &&
      story.plan.isEmpty &&
      story.captureResult.isEmpty &&
      story.threatProof.isEmpty &&
      story.defenseProof.isEmpty &&
      story.multiTargetProof.isEmpty &&
      story.lineProof.isEmpty &&
      story.pinProof.isEmpty &&
      story.removeGuardProof.isEmpty &&
      story.skewerProof.isEmpty &&
      story.queenHitProof.isEmpty &&
      story.loosePieceProof.isEmpty &&
      story.fileOpenedProof.isEmpty &&
      story.proofFailures.isEmpty &&
      story.overloadProof.exists(overloadProofBindsPlan(story, _))

  private def overloadProofBindsPlan(story: Story, proof: OverloadProof) =
    proof.complete &&
      proof.proofComplete &&
      proof.sameBoardProof &&
      proof.legalMove &&
      proof.sameBoardLegalReplay &&
      proof.defenderDuty.exists(_.complete) &&
      proof.dualDefenderDuty.exists(_.complete) &&
      proof.overloadTest.exists(_.complete) &&
      proof.cannotSatisfyBoth.exists(_.complete) &&
      proof.noReplyPreservesBothDutyTargets &&
      proof.side == story.side &&
      proof.rivalSide == story.rival &&
      proof.route.exists(move => story.route.contains(move)) &&
      proof.target.exists(piece => story.target.contains(piece.square) && piece.side == story.rival) &&
      proof.secondaryTarget.exists(piece => story.secondaryTarget.contains(piece.square) && piece.side == story.rival) &&
      proof.anchor.exists(piece => story.anchor.contains(piece.square) && piece.side == story.rival)

  private def isDeflectShaped(story: Story) =
    story.writer.contains(StoryWriter.TacticDeflect) ||
      story.tactic.contains(Tactic.Deflect) ||
      story.deflectProof.nonEmpty

  private def deflectPlanStory(story: Story) =
    story.writer.contains(StoryWriter.TacticDeflect) &&
      story.scene == Scene.Tactic &&
      story.tactic.contains(Tactic.Deflect) &&
      story.plan.isEmpty &&
      story.captureResult.isEmpty &&
      story.threatProof.isEmpty &&
      story.defenseProof.isEmpty &&
      story.multiTargetProof.isEmpty &&
      story.lineProof.isEmpty &&
      story.pinProof.isEmpty &&
      story.removeGuardProof.isEmpty &&
      story.overloadProof.isEmpty &&
      story.skewerProof.isEmpty &&
      story.queenHitProof.isEmpty &&
      story.loosePieceProof.isEmpty &&
      story.trapProof.isEmpty &&
      story.fileOpenedProof.isEmpty &&
      story.secondaryTarget.isEmpty &&
      story.proofFailures.isEmpty &&
      story.deflectProof.exists(deflectProofBindsPlan(story, _))

  private def deflectProofBindsPlan(story: Story, proof: DeflectProof) =
    proof.complete &&
      proof.sameBoardProof &&
      proof.legalSideMove &&
      proof.legalRivalReply &&
      proof.replyByNamedDefender &&
      proof.defenderGuardedTargetBeforeReply &&
      proof.defenderNoLongerGuardsTargetAfterReply &&
      proof.targetRemainsAfterReply &&
      proof.defenderRemainsAfterReply &&
      proof.sideMoveDoesNotCaptureDefender &&
      proof.completeStoryProof &&
      proof.noEngineEvidenceUsed &&
      proof.side == story.side &&
      proof.rivalSide == story.rival &&
      proof.sideMove.exists(move => story.route.contains(move)) &&
      proof.targetBeforeReply.exists(piece =>
        story.rival == piece.side &&
          story.target.contains(piece.square) &&
          piece.man != Man.King
      ) &&
      proof.targetAfterReply.exists(piece =>
        story.rival == piece.side &&
          story.target.contains(piece.square) &&
          piece.man != Man.King
      ) &&
      proof.defenderAfterReply.exists(piece =>
        story.rival == piece.side &&
          story.anchor.contains(piece.square)
      )

  private def isLooseShaped(story: Story) =
    story.writer.contains(StoryWriter.TacticLoose) ||
      story.tactic.contains(Tactic.Loose) ||
      story.loosePieceProof.nonEmpty

  private def loosePlanStory(story: Story) =
    story.writer.contains(StoryWriter.TacticLoose) &&
      story.scene == Scene.Tactic &&
      story.tactic.contains(Tactic.Loose) &&
      story.plan.isEmpty &&
      story.captureResult.isEmpty &&
      story.threatProof.isEmpty &&
      story.defenseProof.isEmpty &&
      story.multiTargetProof.isEmpty &&
      story.lineProof.isEmpty &&
      story.pinProof.isEmpty &&
      story.removeGuardProof.isEmpty &&
      story.skewerProof.isEmpty &&
      story.queenHitProof.isEmpty &&
      story.fileOpenedProof.isEmpty &&
      story.secondaryTarget.isEmpty &&
      story.proofFailures.isEmpty &&
      story.loosePieceProof.exists(loosePieceProofBindsPlan(story, _))

  private def loosePieceProofBindsPlan(story: Story, proof: LoosePieceProof) =
    proof.complete &&
      proof.sameBoardProof &&
      proof.legalMove &&
      proof.exactAfterBoardReplay &&
      proof.targetRivalOwned &&
      proof.targetNonKing &&
      proof.afterBoardTargetAttackedByMovingSide &&
      proof.rivalLegalDefendersAfter.isEmpty &&
      proof.rivalSideHasNoLegalDefenderOfTarget &&
      proof.looseAttackProducedOrRevealedByLegalMove &&
      proof.attackingSide == story.side &&
      proof.rivalSide == story.rival &&
      proof.attackMove.exists(move => story.route.contains(move)) &&
      proof.targetPieceSquareAfter == story.target &&
      proof.attackingPieceSquareAfter == story.anchor &&
      proof.targetPieceAfter.exists(piece => story.rival == piece.side && piece.man != Man.King)

  private def isQueenHitShaped(story: Story) =
    story.writer.contains(StoryWriter.TacticQueenHit) ||
      story.tactic.contains(Tactic.QueenHit) ||
      story.queenHitProof.nonEmpty

  private def queenHitPlanStory(story: Story) =
    story.writer.contains(StoryWriter.TacticQueenHit) &&
      story.scene == Scene.Tactic &&
      story.tactic.contains(Tactic.QueenHit) &&
      story.plan.isEmpty &&
      story.captureResult.isEmpty &&
      story.threatProof.isEmpty &&
      story.defenseProof.isEmpty &&
      story.multiTargetProof.isEmpty &&
      story.lineProof.isEmpty &&
      story.pinProof.isEmpty &&
      story.removeGuardProof.isEmpty &&
      story.skewerProof.isEmpty &&
      story.loosePieceProof.isEmpty &&
      story.fileOpenedProof.isEmpty &&
      story.secondaryTarget.isEmpty &&
      story.proofFailures.isEmpty &&
      story.queenHitProof.exists(queenHitProofBindsPlan(story, _))

  private def queenHitProofBindsPlan(story: Story, proof: QueenHitProof) =
    proof.complete &&
      proof.sameBoardProof &&
      proof.legalMove &&
      proof.exactAfterBoardReplay &&
      proof.rivalQueenExistsAfter &&
      proof.afterBoardQueenAttackedByMovingSide &&
      proof.queenHitProducedOrRevealedByLegalMove &&
      proof.attackingSide == story.side &&
      proof.rivalSide == story.rival &&
      proof.attackMove.exists(move => story.route.contains(move)) &&
      proof.rivalQueenSquareAfter == story.target &&
      proof.attackingPieceSquareAfter == story.anchor

  private def pawnAdvanceCanPlan(verdict: Verdict) =
    verdict.selected &&
      verdict.role == Role.Lead &&
      verdict.leadAllowed &&
      !verdict.engineStrengthLimited &&
      !verdict.engineCheckStatus.contains(EngineCheckStatus.Refutes)

  private def pawnStopCanPlan(verdict: Verdict) =
    verdict.selected &&
      verdict.role == Role.Lead &&
      verdict.leadAllowed &&
      !verdict.engineStrengthLimited &&
      !verdict.engineCheckStatus.contains(EngineCheckStatus.Refutes)

  private def pawnBreakCanPlan(verdict: Verdict) =
    verdict.selected &&
      verdict.role == Role.Lead &&
      verdict.leadAllowed &&
      !verdict.engineStrengthLimited &&
      !verdict.engineCheckStatus.contains(EngineCheckStatus.Refutes)

  private def pawnCaptureCanPlan(verdict: Verdict) =
    verdict.selected &&
      verdict.role == Role.Lead &&
      verdict.leadAllowed &&
      !verdict.engineStrengthLimited &&
      !verdict.engineCheckStatus.contains(EngineCheckStatus.Refutes)

  private def passedPawnCreatedCanPlan(verdict: Verdict) =
    verdict.selected &&
      verdict.role == Role.Lead &&
      verdict.leadAllowed &&
      !verdict.engineStrengthLimited &&
      !verdict.engineCheckStatus.contains(EngineCheckStatus.Refutes)

  private def fileOpenedCanPlan(verdict: Verdict) =
    verdict.selected &&
      verdict.role == Role.Lead &&
      verdict.leadAllowed &&
      !verdict.engineStrengthLimited &&
      !verdict.engineCheckStatus.contains(EngineCheckStatus.Refutes)

  private def pawnBlockCanPlan(verdict: Verdict) =
    verdict.selected &&
      verdict.role == Role.Lead &&
      verdict.leadAllowed &&
      !verdict.engineStrengthLimited &&
      !verdict.engineCheckStatus.contains(EngineCheckStatus.Refutes)

  private def promotionThreatCanPlan(verdict: Verdict) =
    verdict.selected &&
      verdict.role == Role.Lead &&
      verdict.leadAllowed &&
      !verdict.engineStrengthLimited &&
      !verdict.engineCheckStatus.contains(EngineCheckStatus.Refutes)

  private def promotionCanPlan(verdict: Verdict) =
    verdict.selected &&
      verdict.role == Role.Lead &&
      verdict.leadAllowed &&
      !verdict.engineStrengthLimited &&
      !verdict.engineCheckStatus.contains(EngineCheckStatus.Refutes)

  private def checkGivenCanPlan(verdict: Verdict) =
    verdict.selected &&
      verdict.role == Role.Lead &&
      verdict.leadAllowed &&
      !verdict.engineStrengthLimited &&
      verdict.engineCheckStatus.forall(_ == EngineCheckStatus.Supports)

  private def checkEscapedCanPlan(verdict: Verdict) =
    verdict.selected &&
      verdict.role == Role.Lead &&
      verdict.leadAllowed &&
      !verdict.engineStrengthLimited &&
      verdict.engineCheckStatus.forall(_ == EngineCheckStatus.Supports)

  private def checkmateCanPlan(verdict: Verdict) =
    verdict.selected &&
      verdict.role == Role.Lead &&
      verdict.leadAllowed &&
      !verdict.engineStrengthLimited &&
      verdict.engineCheckStatus.forall(_ == EngineCheckStatus.Supports)

  private def stalemateCanPlan(verdict: Verdict) =
    verdict.selected &&
      verdict.role == Role.Lead &&
      verdict.leadAllowed &&
      !verdict.engineStrengthLimited &&
      verdict.engineCheckStatus.forall(_ == EngineCheckStatus.Supports)

  private def materialForbiddenWording(verdict: Verdict) =
    if verdict.engineStrengthLimited then MaterialForbiddenWording :+ ForbiddenWording.StrongWording
    else MaterialForbiddenWording

  private def defenseForbiddenWording(verdict: Verdict) =
    if verdict.engineStrengthLimited then DefenseForbiddenWording :+ ForbiddenWording.StrongWording
    else DefenseForbiddenWording

  private def relations(verdict: Verdict, tactic: Tactic) =
    val roleRelation =
      verdict.role match
        case Role.Support => Vector(ExplanationRelation.SameFamilyLowerRank)
        case Role.Context if tactic == Tactic.Fork =>
          Vector(ExplanationRelation.AlternativeForkCandidate)
        case Role.Context => Vector(ExplanationRelation.AlternativeHangingCandidate)
        case Role.Blocked if verdict.engineCheckStatus.contains(EngineCheckStatus.Refutes) =>
          Vector(ExplanationRelation.BlockedByEngineRefute)
        case _ => Vector.empty
    if verdict.engineStrengthLimited then roleRelation :+ ExplanationRelation.CappedSameStory
    else roleRelation

  private def materialRelations(verdict: Verdict) =
    val roleRelation =
      verdict.role match
        case Role.Support => Vector(ExplanationRelation.SameFamilyLowerRank)
        case Role.Blocked if verdict.engineCheckStatus.contains(EngineCheckStatus.Refutes) =>
          Vector(ExplanationRelation.BlockedByEngineRefute)
        case _ => Vector.empty
    if verdict.engineStrengthLimited then roleRelation :+ ExplanationRelation.CappedSameStory
    else roleRelation

  private def defenseRelations(verdict: Verdict) =
    val roleRelation =
      verdict.role match
        case Role.Support => Vector(ExplanationRelation.SameFamilyLowerRank)
        case Role.Blocked if verdict.engineCheckStatus.contains(EngineCheckStatus.Refutes) =>
          Vector(ExplanationRelation.BlockedByEngineRefute)
        case _ => Vector.empty
    if verdict.engineStrengthLimited then roleRelation :+ ExplanationRelation.CappedSameStory
    else roleRelation

  def fromSelected(verdict: Verdict): Option[ExplanationPlan] =
    val story = verdict.story
    if isLooseShaped(story) then fromSelectedLoose(verdict, story)
    else if isQueenHitShaped(story) then fromSelectedQueenHit(verdict, story)
    else if isDecoyShaped(story) then fromSelectedDecoy(verdict, story)
    else if isTrapShaped(story) then fromSelectedTrap(verdict, story)
    else if isDeflectShaped(story) then fromSelectedDeflect(verdict, story)
    else if story.scene == Scene.Material then fromSelectedMaterial(verdict, story)
    else if story.scene == Scene.Defense then fromSelectedDefense(verdict, story)
    else if story.tactic.contains(Tactic.DiscoveredAttack) then fromSelectedDiscoveredAttack(verdict, story)
    else if story.tactic.contains(Tactic.Pin) then fromSelectedPin(verdict, story)
    else if story.tactic.contains(Tactic.RemoveGuard) then fromSelectedRemoveGuard(verdict, story)
    else if isOverloadShaped(story) then fromSelectedOverload(verdict, story)
    else if story.tactic.contains(Tactic.Skewer) then fromSelectedSkewer(verdict, story)
    else if story.scene == Scene.PawnAdvance then fromSelectedPawnAdvance(verdict, story)
    else if story.scene == Scene.PawnStop then fromSelectedPawnStop(verdict, story)
    else if story.scene == Scene.PawnBreak then fromSelectedPawnBreak(verdict, story)
    else if story.scene == Scene.PawnCapture then fromSelectedPawnCapture(verdict, story)
    else if story.scene == Scene.PassedPawnCreated then fromSelectedPassedPawnCreated(verdict, story)
    else if story.scene == Scene.FileOpened then fromSelectedFileOpened(verdict, story)
    else if story.scene == Scene.PawnBlock then fromSelectedPawnBlock(verdict, story)
    else if story.scene == Scene.PromotionThreat then fromSelectedPromotionThreat(verdict, story)
    else if story.scene == Scene.Promotion then fromSelectedPromotion(verdict, story)
    else if story.scene == Scene.CheckGiven then fromSelectedCheckGiven(verdict, story)
    else if story.scene == Scene.CheckEscaped then fromSelectedCheckEscaped(verdict, story)
    else if story.scene == Scene.Checkmate then fromSelectedCheckmate(verdict, story)
    else if story.scene == Scene.Stalemate then fromSelectedStalemate(verdict, story)
    else fromSelectedTactic(verdict, story)

  private def fromSelectedTactic(verdict: Verdict, story: Story): Option[ExplanationPlan] =
    for
      tactic <- story.tactic
      target <- story.target
      anchor <- story.anchor
      route <- story.route
      routeSan <- story.routeSan
      if verdict.selected
      if story.scene == Scene.Tactic
      if tactic == Tactic.Hanging || (tactic == Tactic.Fork && forkPlanAllowed(verdict, story, route))
      if story.side == Side.White || story.side == Side.Black
    yield ExplanationPlan(
      role = verdict.role,
      scene = story.scene,
      tactic = Some(tactic),
      side = story.side,
      rival = story.rival,
      target = Some(target),
      anchor = Some(anchor),
      route = Some(route),
      routeSan = Some(routeSan),
      secondaryTarget = story.secondaryTarget,
      allowedClaim = tacticAllowedClaim(verdict, tactic),
      evidenceLine = Some(route),
      strength = ExplanationStrength.Bounded,
      forbiddenWording = forbiddenWording(verdict, tactic),
      relations = relations(verdict, tactic),
      debugOnly = verdict.role == Role.Blocked,
      supportContextLinks = Vector.empty
    )

  private def forkPlanAllowed(verdict: Verdict, story: Story, route: Line): Boolean =
    verdict.role == Role.Lead &&
      verdict.leadAllowed &&
      !verdict.engineStrengthLimited &&
      !verdict.engineCheckStatus.contains(EngineCheckStatus.Refutes) &&
      story.writer.contains(StoryWriter.TacticFork) &&
      story.proofFailures.isEmpty &&
      story.multiTargetProof.exists: proof =>
        proof.complete &&
          proof.sameBoardProof &&
          proof.forkMove.contains(route) &&
          proof.attackerAfterMove.exists(piece => story.anchor.contains(piece.square)) &&
          proof.targetA.exists(piece => story.target.contains(piece.square) && piece.side == story.rival && piece.man != Man.King) &&
          proof.targetB.exists(piece => story.secondaryTarget.contains(piece.square) && piece.side == story.rival && piece.man != Man.King) &&
          story.target.exists(proof.attackedTargetSquaresAfterMove.contains) &&
          story.secondaryTarget.exists(proof.attackedTargetSquaresAfterMove.contains) &&
          proof.replyMap.nonEmpty

  private def fromSelectedLoose(verdict: Verdict, story: Story): Option[ExplanationPlan] =
    for
      target <- story.target
      anchor <- story.anchor
      route <- story.route
      routeSan <- story.routeSan
      if looseCanPlan(verdict)
      if loosePlanStory(story)
      if story.side == Side.White || story.side == Side.Black
    yield ExplanationPlan(
      role = verdict.role,
      scene = story.scene,
      tactic = Some(Tactic.Loose),
      side = story.side,
      rival = story.rival,
      target = Some(target),
      anchor = Some(anchor),
      route = Some(route),
      routeSan = Some(routeSan),
      secondaryTarget = None,
      allowedClaim = Some(ExplanationClaim.AttacksLoosePiece),
      evidenceLine = Some(route),
      strength = ExplanationStrength.Bounded,
      forbiddenWording = LooseForbiddenWording,
      relations = Vector.empty,
      debugOnly = false,
      supportContextLinks = Vector.empty
    )

  private def fromSelectedQueenHit(verdict: Verdict, story: Story): Option[ExplanationPlan] =
    for
      target <- story.target
      anchor <- story.anchor
      route <- story.route
      routeSan <- story.routeSan
      if queenHitCanPlan(verdict)
      if queenHitPlanStory(story)
      if story.side == Side.White || story.side == Side.Black
    yield ExplanationPlan(
      role = verdict.role,
      scene = story.scene,
      tactic = Some(Tactic.QueenHit),
      side = story.side,
      rival = story.rival,
      target = Some(target),
      anchor = Some(anchor),
      route = Some(route),
      routeSan = Some(routeSan),
      secondaryTarget = None,
      allowedClaim = Some(ExplanationClaim.AttacksQueen),
      evidenceLine = Some(route),
      strength = ExplanationStrength.Bounded,
      forbiddenWording = QueenHitForbiddenWording,
      relations = Vector.empty,
      debugOnly = false,
      supportContextLinks = Vector.empty
    )

  private def fromSelectedTrap(verdict: Verdict, story: Story): Option[ExplanationPlan] =
    for
      target <- story.target
      anchor <- story.anchor
      route <- story.route
      routeSan <- story.routeSan
      if trapCanPlan(verdict)
      if trapPlanStory(story)
      if story.side == Side.White || story.side == Side.Black
    yield ExplanationPlan(
      role = verdict.role,
      scene = story.scene,
      tactic = Some(Tactic.Trap),
      side = story.side,
      rival = story.rival,
      target = Some(target),
      anchor = Some(anchor),
      route = Some(route),
      routeSan = Some(routeSan),
      secondaryTarget = None,
      allowedClaim = Some(ExplanationClaim.TrapsPiece),
      evidenceLine = Some(route),
      strength = ExplanationStrength.Bounded,
      forbiddenWording = TrapForbiddenWording,
      relations = Vector.empty,
      debugOnly = false,
      supportContextLinks = Vector.empty
    )

  private def fromSelectedDecoy(verdict: Verdict, story: Story): Option[ExplanationPlan] =
    for
      target <- story.target
      anchor <- story.anchor
      route <- story.route
      routeSan <- story.routeSan
      if decoyCanPlan(verdict)
      if decoyPlanStory(story)
      if story.side == Side.White || story.side == Side.Black
    yield ExplanationPlan(
      role = verdict.role,
      scene = story.scene,
      tactic = Some(Tactic.Decoy),
      side = story.side,
      rival = story.rival,
      target = Some(target),
      anchor = Some(anchor),
      route = Some(route),
      routeSan = Some(routeSan),
      secondaryTarget = None,
      allowedClaim = Some(ExplanationClaim.DecoysPiece),
      evidenceLine = Some(route),
      strength = ExplanationStrength.Bounded,
      forbiddenWording = DecoyForbiddenWording,
      relations = Vector.empty,
      debugOnly = false,
      supportContextLinks = Vector.empty
    )

  private def fromSelectedDeflect(verdict: Verdict, story: Story): Option[ExplanationPlan] =
    for
      target <- story.target
      anchor <- story.anchor
      route <- story.route
      routeSan <- story.routeSan
      if deflectCanPlan(verdict)
      if deflectPlanStory(story)
      if story.side == Side.White || story.side == Side.Black
    yield ExplanationPlan(
      role = verdict.role,
      scene = story.scene,
      tactic = Some(Tactic.Deflect),
      side = story.side,
      rival = story.rival,
      target = Some(target),
      anchor = Some(anchor),
      route = Some(route),
      routeSan = Some(routeSan),
      secondaryTarget = None,
      allowedClaim = Some(ExplanationClaim.DeflectsDefender),
      evidenceLine = Some(route),
      strength = ExplanationStrength.Bounded,
      forbiddenWording = DeflectForbiddenWording,
      relations = Vector.empty,
      debugOnly = false,
      supportContextLinks = Vector.empty
    )

  private def fromSelectedDiscoveredAttack(verdict: Verdict, story: Story): Option[ExplanationPlan] =
    for
      target <- story.target
      anchor <- story.anchor
      route <- story.route
      routeSan <- story.routeSan
      if discoveredAttackCanPlan(verdict)
      if story.scene == Scene.Tactic
      if story.tactic.contains(Tactic.DiscoveredAttack)
      if story.secondaryTarget.isEmpty
      if story.side == Side.White || story.side == Side.Black
    yield ExplanationPlan(
      role = verdict.role,
      scene = story.scene,
      tactic = Some(Tactic.DiscoveredAttack),
      side = story.side,
      rival = story.rival,
      target = Some(target),
      anchor = Some(anchor),
      route = Some(route),
      routeSan = Some(routeSan),
      secondaryTarget = None,
      allowedClaim = discoveredAttackAllowedClaim(verdict),
      evidenceLine = Some(route),
      strength = ExplanationStrength.Bounded,
      forbiddenWording = DiscoveredAttackForbiddenWording,
      relations = Vector.empty,
      debugOnly = false,
      supportContextLinks = Vector.empty
    )

  private def fromSelectedPin(verdict: Verdict, story: Story): Option[ExplanationPlan] =
    for
      target <- story.target
      anchor <- story.anchor
      route <- story.route
      routeSan <- story.routeSan
      if pinCanPlan(verdict)
      if story.scene == Scene.Tactic
      if story.tactic.contains(Tactic.Pin)
      if story.writer.contains(StoryWriter.TacticPin)
      if story.secondaryTarget.isEmpty
      if story.side == Side.White || story.side == Side.Black
    yield ExplanationPlan(
      role = verdict.role,
      scene = story.scene,
      tactic = Some(Tactic.Pin),
      side = story.side,
      rival = story.rival,
      target = Some(target),
      anchor = Some(anchor),
      route = Some(route),
      routeSan = Some(routeSan),
      secondaryTarget = None,
      allowedClaim = Some(ExplanationClaim.PinsPiece),
      evidenceLine = Some(route),
      strength = ExplanationStrength.Bounded,
      forbiddenWording = PinForbiddenWording,
      relations = Vector.empty,
      debugOnly = false,
      supportContextLinks = Vector.empty
    )

  private def fromSelectedRemoveGuard(verdict: Verdict, story: Story): Option[ExplanationPlan] =
    for
      target <- story.target
      anchor <- story.anchor
      route <- story.route
      routeSan <- story.routeSan
      if removeGuardCanPlan(verdict)
      if story.scene == Scene.Tactic
      if story.tactic.contains(Tactic.RemoveGuard)
      if story.writer.contains(StoryWriter.TacticRemoveGuard)
      if story.secondaryTarget.isEmpty
      if story.side == Side.White || story.side == Side.Black
    yield ExplanationPlan(
      role = verdict.role,
      scene = story.scene,
      tactic = Some(Tactic.RemoveGuard),
      side = story.side,
      rival = story.rival,
      target = Some(target),
      anchor = Some(anchor),
      route = Some(route),
      routeSan = Some(routeSan),
      secondaryTarget = None,
      allowedClaim = Some(ExplanationClaim.RemovesDefender),
      evidenceLine = Some(route),
      strength = ExplanationStrength.Bounded,
      forbiddenWording = RemoveGuardForbiddenWording,
      relations = Vector.empty,
      debugOnly = false,
      supportContextLinks = Vector.empty
    )

  private def fromSelectedOverload(verdict: Verdict, story: Story): Option[ExplanationPlan] =
    for
      target <- story.target
      anchor <- story.anchor
      route <- story.route
      routeSan <- story.routeSan
      secondaryTarget <- story.secondaryTarget
      if overloadCanPlan(verdict)
      if overloadPlanStory(story)
      if story.side == Side.White || story.side == Side.Black
    yield ExplanationPlan(
      role = verdict.role,
      scene = story.scene,
      tactic = Some(Tactic.Overload),
      side = story.side,
      rival = story.rival,
      target = Some(target),
      anchor = Some(anchor),
      route = Some(route),
      routeSan = Some(routeSan),
      secondaryTarget = Some(secondaryTarget),
      allowedClaim = Some(ExplanationClaim.OverloadsDefender),
      evidenceLine = Some(route),
      strength = ExplanationStrength.Bounded,
      forbiddenWording = OverloadForbiddenWording,
      relations = Vector.empty,
      debugOnly = false,
      supportContextLinks = Vector.empty
    )

  private def fromSelectedSkewer(verdict: Verdict, story: Story): Option[ExplanationPlan] =
    for
      target <- story.target
      anchor <- story.anchor
      route <- story.route
      routeSan <- story.routeSan
      secondaryTarget <- story.secondaryTarget
      if skewerCanPlan(verdict)
      if story.scene == Scene.Tactic
      if story.tactic.contains(Tactic.Skewer)
      if story.writer.contains(StoryWriter.TacticSkewer)
      if story.side == Side.White || story.side == Side.Black
    yield ExplanationPlan(
      role = verdict.role,
      scene = story.scene,
      tactic = Some(Tactic.Skewer),
      side = story.side,
      rival = story.rival,
      target = Some(target),
      anchor = Some(anchor),
      route = Some(route),
      routeSan = Some(routeSan),
      secondaryTarget = Some(secondaryTarget),
      allowedClaim = Some(ExplanationClaim.SkewersPieceToPiece),
      evidenceLine = Some(route),
      strength = ExplanationStrength.Bounded,
      forbiddenWording = SkewerForbiddenWording,
      relations = Vector.empty,
      debugOnly = false,
      supportContextLinks = Vector.empty
    )

  private def fromSelectedMaterial(verdict: Verdict, story: Story): Option[ExplanationPlan] =
    for
      target <- story.target
      anchor <- story.anchor
      route <- story.route
      routeSan <- story.routeSan
      if verdict.selected
      if story.scene == Scene.Material
      if story.tactic.isEmpty
      if story.side == Side.White || story.side == Side.Black
    yield ExplanationPlan(
      role = verdict.role,
      scene = story.scene,
      tactic = None,
      side = story.side,
      rival = story.rival,
      target = Some(target),
      anchor = Some(anchor),
      route = Some(route),
      routeSan = Some(routeSan),
      secondaryTarget = None,
      allowedClaim = materialAllowedClaim(verdict),
      evidenceLine = Some(route),
      strength = ExplanationStrength.Bounded,
      forbiddenWording = materialForbiddenWording(verdict),
      relations = materialRelations(verdict),
      debugOnly = verdict.role == Role.Blocked,
      supportContextLinks = Vector.empty
    )

  private def fromSelectedPawnAdvance(verdict: Verdict, story: Story): Option[ExplanationPlan] =
    for
      target <- story.target
      anchor <- story.anchor
      route <- story.route
      routeSan <- story.routeSan
      if pawnAdvanceCanPlan(verdict)
      if story.scene == Scene.PawnAdvance
      if story.tactic.isEmpty
      if story.plan.isEmpty
      if story.writer.contains(StoryWriter.ScenePawnAdvance)
      if story.secondaryTarget.isEmpty
      if story.side == Side.White || story.side == Side.Black
    yield ExplanationPlan(
      role = verdict.role,
      scene = story.scene,
      tactic = None,
      side = story.side,
      rival = story.rival,
      target = Some(target),
      anchor = Some(anchor),
      route = Some(route),
      routeSan = Some(routeSan),
      secondaryTarget = None,
      allowedClaim = pawnAdvanceAllowedClaim(verdict),
      evidenceLine = Some(route),
      strength = ExplanationStrength.Bounded,
      forbiddenWording = PawnAdvanceForbiddenWording,
      relations = Vector.empty,
      debugOnly = false,
      supportContextLinks = Vector.empty
    )

  private def fromSelectedPawnStop(verdict: Verdict, story: Story): Option[ExplanationPlan] =
    for
      target <- story.target
      anchor <- story.anchor
      route <- story.route
      routeSan <- story.routeSan
      if pawnStopCanPlan(verdict)
      if story.scene == Scene.PawnStop
      if story.tactic.isEmpty
      if story.plan.isEmpty
      if story.writer.contains(StoryWriter.ScenePawnStop)
      if story.secondaryTarget.isEmpty
      if story.side == Side.White || story.side == Side.Black
    yield ExplanationPlan(
      role = verdict.role,
      scene = story.scene,
      tactic = None,
      side = story.side,
      rival = story.rival,
      target = Some(target),
      anchor = Some(anchor),
      route = Some(route),
      routeSan = Some(routeSan),
      secondaryTarget = None,
      allowedClaim = pawnStopAllowedClaim(verdict),
      evidenceLine = Some(route),
      strength = ExplanationStrength.Bounded,
      forbiddenWording = PawnStopForbiddenWording,
      relations = Vector.empty,
      debugOnly = false,
      supportContextLinks = Vector.empty
    )

  private def fromSelectedPawnBreak(verdict: Verdict, story: Story): Option[ExplanationPlan] =
    for
      target <- story.target
      anchor <- story.anchor
      route <- story.route
      routeSan <- story.routeSan
      if pawnBreakCanPlan(verdict)
      if story.scene == Scene.PawnBreak
      if story.tactic.isEmpty
      if story.plan.isEmpty
      if story.writer.contains(StoryWriter.ScenePawnBreak)
      if story.secondaryTarget.isEmpty
      if story.side == Side.White || story.side == Side.Black
    yield ExplanationPlan(
      role = verdict.role,
      scene = story.scene,
      tactic = None,
      side = story.side,
      rival = story.rival,
      target = Some(target),
      anchor = Some(anchor),
      route = Some(route),
      routeSan = Some(routeSan),
      secondaryTarget = None,
      allowedClaim = pawnBreakAllowedClaim(verdict),
      evidenceLine = Some(route),
      strength = ExplanationStrength.Bounded,
      forbiddenWording = PawnBreakForbiddenWording,
      relations = Vector.empty,
      debugOnly = false,
      supportContextLinks = Vector.empty
    )

  private def fromSelectedPawnCapture(verdict: Verdict, story: Story): Option[ExplanationPlan] =
    for
      target <- story.target
      anchor <- story.anchor
      route <- story.route
      routeSan <- story.routeSan
      if pawnCaptureCanPlan(verdict)
      if story.scene == Scene.PawnCapture
      if story.tactic.isEmpty
      if story.plan.isEmpty
      if story.writer.contains(StoryWriter.ScenePawnCapture)
      if story.secondaryTarget.isEmpty
      if story.side == Side.White || story.side == Side.Black
    yield ExplanationPlan(
      role = verdict.role,
      scene = story.scene,
      tactic = None,
      side = story.side,
      rival = story.rival,
      target = Some(target),
      anchor = Some(anchor),
      route = Some(route),
      routeSan = Some(routeSan),
      secondaryTarget = None,
      allowedClaim = pawnCaptureAllowedClaim(verdict),
      evidenceLine = Some(route),
      strength = ExplanationStrength.Bounded,
      forbiddenWording = PawnCaptureForbiddenWording,
      relations = Vector.empty,
      debugOnly = false,
      supportContextLinks = Vector.empty
    )

  private def fromSelectedPassedPawnCreated(verdict: Verdict, story: Story): Option[ExplanationPlan] =
    for
      target <- story.target
      anchor <- story.anchor
      route <- story.route
      routeSan <- story.routeSan
      if passedPawnCreatedCanPlan(verdict)
      if story.scene == Scene.PassedPawnCreated
      if story.tactic.isEmpty
      if story.plan.isEmpty
      if story.writer.contains(StoryWriter.ScenePassedPawnCreated)
      if story.secondaryTarget.isEmpty
      if story.side == Side.White || story.side == Side.Black
    yield ExplanationPlan(
      role = verdict.role,
      scene = story.scene,
      tactic = None,
      side = story.side,
      rival = story.rival,
      target = Some(target),
      anchor = Some(anchor),
      route = Some(route),
      routeSan = Some(routeSan),
      secondaryTarget = None,
      allowedClaim = passedPawnCreatedAllowedClaim(verdict),
      evidenceLine = Some(route),
      strength = ExplanationStrength.Bounded,
      forbiddenWording = PassedPawnCreatedForbiddenWording,
      relations = Vector.empty,
      debugOnly = false,
      supportContextLinks = Vector.empty
    )

  private def fromSelectedFileOpened(verdict: Verdict, story: Story): Option[ExplanationPlan] =
    for
      target <- story.target
      anchor <- story.anchor
      route <- story.route
      routeSan <- story.routeSan
      if fileOpenedCanPlan(verdict)
      if story.scene == Scene.FileOpened
      if story.tactic.isEmpty
      if story.plan.isEmpty
      if story.writer.contains(StoryWriter.SceneFileOpened)
      if story.secondaryTarget.isEmpty
      if story.side == Side.White || story.side == Side.Black
    yield ExplanationPlan(
      role = verdict.role,
      scene = story.scene,
      tactic = None,
      side = story.side,
      rival = story.rival,
      target = Some(target),
      anchor = Some(anchor),
      route = Some(route),
      routeSan = Some(routeSan),
      secondaryTarget = None,
      allowedClaim = fileOpenedAllowedClaim(verdict),
      evidenceLine = Some(route),
      strength = ExplanationStrength.Bounded,
      forbiddenWording = FileOpenedForbiddenWording,
      relations = Vector.empty,
      debugOnly = false,
      supportContextLinks = Vector.empty
    )

  private def fromSelectedPawnBlock(verdict: Verdict, story: Story): Option[ExplanationPlan] =
    for
      target <- story.target
      anchor <- story.anchor
      route <- story.route
      routeSan <- story.routeSan
      if pawnBlockCanPlan(verdict)
      if story.scene == Scene.PawnBlock
      if story.tactic.isEmpty
      if story.plan.isEmpty
      if story.writer.contains(StoryWriter.ScenePawnBlock)
      if story.secondaryTarget.isEmpty
      if story.side == Side.White || story.side == Side.Black
    yield ExplanationPlan(
      role = verdict.role,
      scene = story.scene,
      tactic = None,
      side = story.side,
      rival = story.rival,
      target = Some(target),
      anchor = Some(anchor),
      route = Some(route),
      routeSan = Some(routeSan),
      secondaryTarget = None,
      allowedClaim = pawnBlockAllowedClaim(verdict),
      evidenceLine = Some(route),
      strength = ExplanationStrength.Bounded,
      forbiddenWording = PawnBlockForbiddenWording,
      relations = Vector.empty,
      debugOnly = false,
      supportContextLinks = Vector.empty
    )

  private def fromSelectedPromotionThreat(verdict: Verdict, story: Story): Option[ExplanationPlan] =
    for
      target <- story.target
      anchor <- story.anchor
      route <- story.route
      routeSan <- story.routeSan
      if promotionThreatCanPlan(verdict)
      if story.scene == Scene.PromotionThreat
      if story.tactic.isEmpty
      if story.plan.isEmpty
      if story.writer.contains(StoryWriter.ScenePromotionThreat)
      if story.secondaryTarget.isEmpty
      if story.side == Side.White || story.side == Side.Black
    yield ExplanationPlan(
      role = verdict.role,
      scene = story.scene,
      tactic = None,
      side = story.side,
      rival = story.rival,
      target = Some(target),
      anchor = Some(anchor),
      route = Some(route),
      routeSan = Some(routeSan),
      secondaryTarget = None,
      allowedClaim = promotionThreatAllowedClaim(verdict),
      evidenceLine = Some(route),
      strength = ExplanationStrength.Bounded,
      forbiddenWording = PromotionThreatForbiddenWording,
      relations = Vector.empty,
      debugOnly = false,
      supportContextLinks = Vector.empty
    )

  private def fromSelectedPromotion(verdict: Verdict, story: Story): Option[ExplanationPlan] =
    for
      target <- story.target
      anchor <- story.anchor
      route <- story.route
      routeSan <- story.routeSan
      if promotionCanPlan(verdict)
      if story.scene == Scene.Promotion
      if story.tactic.isEmpty
      if story.plan.isEmpty
      if story.writer.contains(StoryWriter.ScenePromotion)
      if story.secondaryTarget.isEmpty
      if story.side == Side.White || story.side == Side.Black
    yield ExplanationPlan(
      role = verdict.role,
      scene = story.scene,
      tactic = None,
      side = story.side,
      rival = story.rival,
      target = Some(target),
      anchor = Some(anchor),
      route = Some(route),
      routeSan = Some(routeSan),
      secondaryTarget = None,
      allowedClaim = promotionAllowedClaim(verdict),
      evidenceLine = Some(route),
      strength = ExplanationStrength.Bounded,
      forbiddenWording = PromotionForbiddenWording,
      relations = Vector.empty,
      debugOnly = false,
      supportContextLinks = Vector.empty
    )

  private def fromSelectedCheckGiven(verdict: Verdict, story: Story): Option[ExplanationPlan] =
    for
      target <- story.target
      anchor <- story.anchor
      route <- story.route
      routeSan <- story.routeSan
      proof <- story.checkGivenProof
      if checkGivenCanPlan(verdict)
      if story.scene == Scene.CheckGiven
      if story.tactic.isEmpty
      if story.plan.isEmpty
      if story.writer.contains(StoryWriter.SceneCheckGiven)
      if story.secondaryTarget.isEmpty
      if cleanCheckGivenSidecars(story)
      if story.proofFailures.isEmpty
      if story.side == Side.White || story.side == Side.Black
      if checkGivenProofBindsPlanStory(story, proof)
    yield ExplanationPlan(
      role = verdict.role,
      scene = story.scene,
      tactic = None,
      side = story.side,
      rival = story.rival,
      target = Some(target),
      anchor = Some(anchor),
      route = Some(route),
      routeSan = Some(routeSan),
      secondaryTarget = None,
      allowedClaim = checkGivenAllowedClaim(verdict),
      evidenceLine = Some(route),
      strength = ExplanationStrength.Bounded,
      forbiddenWording = CheckGivenForbiddenWording,
      relations = Vector.empty,
      debugOnly = false,
      supportContextLinks = Vector.empty
    )

  private def fromSelectedCheckEscaped(verdict: Verdict, story: Story): Option[ExplanationPlan] =
    for
      target <- story.target
      anchor <- story.anchor
      route <- story.route
      routeSan <- story.routeSan
      proof <- story.checkEscapedProof
      if checkEscapedCanPlan(verdict)
      if story.scene == Scene.CheckEscaped
      if story.tactic.isEmpty
      if story.plan.isEmpty
      if story.writer.contains(StoryWriter.SceneCheckEscaped)
      if story.secondaryTarget.isEmpty
      if cleanCheckEscapedSidecars(story)
      if story.proofFailures.isEmpty
      if story.side == Side.White || story.side == Side.Black
      if checkEscapedProofBindsPlanStory(story, proof)
    yield ExplanationPlan(
      role = verdict.role,
      scene = story.scene,
      tactic = None,
      side = story.side,
      rival = story.rival,
      target = Some(target),
      anchor = Some(anchor),
      route = Some(route),
      routeSan = Some(routeSan),
      secondaryTarget = None,
      allowedClaim = checkEscapedAllowedClaim(verdict),
      evidenceLine = Some(route),
      strength = ExplanationStrength.Bounded,
      forbiddenWording = CheckEscapedForbiddenWording,
      relations = Vector.empty,
      debugOnly = false,
      supportContextLinks = Vector.empty
    )

  private def fromSelectedCheckmate(verdict: Verdict, story: Story): Option[ExplanationPlan] =
    for
      target <- story.target
      anchor <- story.anchor
      route <- story.route
      routeSan <- story.routeSan
      proof <- story.checkmateProof
      if checkmateCanPlan(verdict)
      if story.scene == Scene.Checkmate
      if story.tactic.isEmpty
      if story.plan.isEmpty
      if story.writer.contains(StoryWriter.SceneCheckmate)
      if story.secondaryTarget.isEmpty
      if cleanCheckmateSidecars(story)
      if story.proofFailures.isEmpty
      if story.side == Side.White || story.side == Side.Black
      if checkmateProofBindsPlanStory(story, proof)
    yield ExplanationPlan(
      role = verdict.role,
      scene = story.scene,
      tactic = None,
      side = story.side,
      rival = story.rival,
      target = Some(target),
      anchor = Some(anchor),
      route = Some(route),
      routeSan = Some(routeSan),
      secondaryTarget = None,
      allowedClaim = checkmateAllowedClaim(verdict),
      evidenceLine = Some(route),
      strength = ExplanationStrength.Bounded,
      forbiddenWording = CheckmateForbiddenWording,
      relations = Vector.empty,
      debugOnly = false,
      supportContextLinks = Vector.empty
    )

  private def fromSelectedStalemate(verdict: Verdict, story: Story): Option[ExplanationPlan] =
    for
      target <- story.target
      anchor <- story.anchor
      route <- story.route
      routeSan <- story.routeSan
      proof <- story.stalemateProof
      if stalemateCanPlan(verdict)
      if story.scene == Scene.Stalemate
      if story.tactic.isEmpty
      if story.plan.isEmpty
      if story.writer.contains(StoryWriter.SceneStalemate)
      if story.secondaryTarget.isEmpty
      if cleanStalemateSidecars(story)
      if story.proofFailures.isEmpty
      if story.side == Side.White || story.side == Side.Black
      if stalemateProofBindsPlanStory(story, proof)
    yield ExplanationPlan(
      role = verdict.role,
      scene = story.scene,
      tactic = None,
      side = story.side,
      rival = story.rival,
      target = Some(target),
      anchor = Some(anchor),
      route = Some(route),
      routeSan = Some(routeSan),
      secondaryTarget = None,
      allowedClaim = stalemateAllowedClaim(verdict),
      evidenceLine = Some(route),
      strength = ExplanationStrength.Bounded,
      forbiddenWording = StalemateForbiddenWording,
      relations = Vector.empty,
      debugOnly = false,
      supportContextLinks = Vector.empty
    )

  private def fromSelectedDefense(verdict: Verdict, story: Story): Option[ExplanationPlan] =
    for
      target <- story.target
      anchor <- story.anchor
      route <- story.route
      routeSan <- story.routeSan
      if verdict.selected
      if story.scene == Scene.Defense
      if story.tactic.isEmpty
      if story.plan.isEmpty
      if story.side == Side.White || story.side == Side.Black
    yield ExplanationPlan(
      role = verdict.role,
      scene = story.scene,
      tactic = None,
      side = story.side,
      rival = story.rival,
      target = Some(target),
      anchor = Some(anchor),
      route = Some(route),
      routeSan = Some(routeSan),
      secondaryTarget = None,
      allowedClaim = defenseAllowedClaim(verdict),
      evidenceLine = Some(route),
      strength = ExplanationStrength.Bounded,
      forbiddenWording = defenseForbiddenWording(verdict),
      relations = defenseRelations(verdict),
      debugOnly = verdict.role == Role.Blocked,
      supportContextLinks = Vector.empty
    )

  private def cleanCheckGivenSidecars(story: Story): Boolean =
    story.captureResult.isEmpty &&
      story.multiTargetProof.isEmpty &&
      story.threatProof.isEmpty &&
      story.defenseProof.isEmpty &&
      story.lineProof.isEmpty &&
      story.pinProof.isEmpty &&
      story.removeGuardProof.isEmpty &&
      story.skewerProof.isEmpty &&
      story.pawnAdvanceProof.isEmpty &&
      story.pawnStopProof.isEmpty &&
      story.pawnBreakProof.isEmpty &&
      story.pawnBlockProof.isEmpty &&
      story.promotionThreatProof.isEmpty &&
      story.promotionProof.isEmpty &&
      story.pawnCaptureProof.isEmpty &&
      story.passedPawnCreatedProof.isEmpty &&
      story.fileOpenedProof.isEmpty &&
      story.openedFile.isEmpty &&
      story.checkEscapedProof.isEmpty

  private def checkGivenProofBindsPlanStory(story: Story, proof: CheckGivenProof): Boolean =
    proof.complete &&
      proof.sameBoardProof &&
      proof.legalMove &&
      proof.exactAfterBoardReplay &&
      proof.afterBoardRivalKingInCheck &&
      proof.checkProducedByLegalMove &&
      proof.checkingSide == story.side &&
      proof.rivalSide == story.rival &&
      proof.checkMove.exists(move => story.route.contains(move)) &&
      proof.originSquare.exists(square => story.anchor.contains(square)) &&
      proof.rivalKingSquareAfter.exists(square => story.target.contains(square))

  private def cleanCheckEscapedSidecars(story: Story): Boolean =
    story.captureResult.isEmpty &&
      story.multiTargetProof.isEmpty &&
      story.threatProof.isEmpty &&
      story.defenseProof.isEmpty &&
      story.lineProof.isEmpty &&
      story.pinProof.isEmpty &&
      story.removeGuardProof.isEmpty &&
      story.skewerProof.isEmpty &&
      story.pawnAdvanceProof.isEmpty &&
      story.pawnStopProof.isEmpty &&
      story.pawnBreakProof.isEmpty &&
      story.pawnBlockProof.isEmpty &&
      story.promotionThreatProof.isEmpty &&
      story.promotionProof.isEmpty &&
      story.pawnCaptureProof.isEmpty &&
      story.passedPawnCreatedProof.isEmpty &&
      story.fileOpenedProof.isEmpty &&
      story.openedFile.isEmpty &&
      story.checkGivenProof.isEmpty

  private def checkEscapedProofBindsPlanStory(story: Story, proof: CheckEscapedProof): Boolean =
    proof.complete &&
      proof.sameBoardProof &&
      proof.legalMove &&
      proof.exactBeforeBoardState &&
      proof.exactAfterBoardReplay &&
      proof.beforeBoardSideKingInCheck &&
      proof.afterBoardSideKingNotInCheck &&
      proof.checkEscapedByLegalMove &&
      proof.escapingSide == story.side &&
      proof.rivalSide == story.rival &&
      proof.escapeMove.exists(move => story.route.contains(move)) &&
      proof.beforeKingSquare.nonEmpty &&
      proof.originSquare.exists(square => story.anchor.contains(square)) &&
      proof.afterKingSquare.exists(square => story.target.contains(square))

  private def cleanCheckmateSidecars(story: Story): Boolean =
    story.captureResult.isEmpty &&
      story.multiTargetProof.isEmpty &&
      story.threatProof.isEmpty &&
      story.defenseProof.isEmpty &&
      story.lineProof.isEmpty &&
      story.pinProof.isEmpty &&
      story.removeGuardProof.isEmpty &&
      story.skewerProof.isEmpty &&
      story.pawnAdvanceProof.isEmpty &&
      story.pawnStopProof.isEmpty &&
      story.pawnBreakProof.isEmpty &&
      story.pawnBlockProof.isEmpty &&
      story.promotionThreatProof.isEmpty &&
      story.promotionProof.isEmpty &&
      story.pawnCaptureProof.isEmpty &&
      story.passedPawnCreatedProof.isEmpty &&
      story.fileOpenedProof.isEmpty &&
      story.openedFile.isEmpty &&
      story.checkGivenProof.isEmpty &&
      story.checkEscapedProof.isEmpty

  private def checkmateProofBindsPlanStory(story: Story, proof: CheckmateProof): Boolean =
    proof.complete &&
      proof.sameBoardProof &&
      proof.legalMove &&
      proof.exactAfterBoardReplay &&
      proof.afterBoardRivalKingInCheck &&
      proof.afterBoardRivalSideHasNoLegalEscape &&
      proof.checkmateProducedByLegalMove &&
      proof.matingSide == story.side &&
      proof.rivalSide == story.rival &&
      proof.mateMove.exists(move => story.route.contains(move)) &&
      proof.originSquare.exists(square => story.anchor.contains(square)) &&
      proof.rivalKingSquareAfter.exists(square => story.target.contains(square))

  private def cleanStalemateSidecars(story: Story): Boolean =
    story.captureResult.isEmpty &&
      story.multiTargetProof.isEmpty &&
      story.threatProof.isEmpty &&
      story.defenseProof.isEmpty &&
      story.lineProof.isEmpty &&
      story.pinProof.isEmpty &&
      story.removeGuardProof.isEmpty &&
      story.skewerProof.isEmpty &&
      story.pawnAdvanceProof.isEmpty &&
      story.pawnStopProof.isEmpty &&
      story.pawnBreakProof.isEmpty &&
      story.pawnBlockProof.isEmpty &&
      story.promotionThreatProof.isEmpty &&
      story.promotionProof.isEmpty &&
      story.pawnCaptureProof.isEmpty &&
      story.passedPawnCreatedProof.isEmpty &&
      story.fileOpenedProof.isEmpty &&
      story.openedFile.isEmpty &&
      story.checkGivenProof.isEmpty &&
      story.checkEscapedProof.isEmpty &&
      story.checkmateProof.isEmpty

  private def stalemateProofBindsPlanStory(story: Story, proof: StalemateProof): Boolean =
    proof.complete &&
      proof.sameBoardProof &&
      proof.legalMove &&
      proof.exactAfterBoardReplay &&
      proof.afterBoardRivalSideNotInCheck &&
      proof.afterBoardRivalSideHasNoLegalMoves &&
      proof.stalemateProducedByLegalMove &&
      proof.stalematingSide == story.side &&
      proof.rivalSide == story.rival &&
      proof.stalemateMove.exists(move => story.route.contains(move)) &&
      proof.originSquare.exists(square => story.anchor.contains(square)) &&
      proof.rivalKingSquareAfter.exists(square => story.target.contains(square))
