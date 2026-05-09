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
  case SkewersPieceToPiece
  case AdvancesPassedPawn
  case StopsPassedPawnNextAdvance
  case ChallengesPawnDirectly
  case CapturesPawn
  case CreatesPassedPawn
  case OpensFile
  case CreatesPromotionThreat
  case PromotesPawn

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
      case SkewersPieceToPiece => "skewers_piece_to_piece"
      case AdvancesPassedPawn => "advances_passed_pawn"
      case StopsPassedPawnNextAdvance => "stops_pawn_advance"
      case ChallengesPawnDirectly => "challenges_pawn"
      case CapturesPawn => "captures_rival_pawn"
      case CreatesPassedPawn => "creates_passed_pawn"
      case OpensFile => "opens_file"
      case CreatesPromotionThreat => "threatens_promotion_next"
      case PromotesPawn => "promotes_pawn"

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
  case BestMove
  case OnlyMove
  case EngineSays
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
  case WinsMaterialByFork
  case WinsQueen
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
  case WeakensStructure
  case CreatesWeakness
  case WinsSpace

  def key: String =
    this match
      case FreePiece => "free_piece"
      case Blunder => "blunder"
      case Winning => "winning"
      case Decisive => "decisive"
      case Forced => "forced"
      case BestMove => "best_move"
      case OnlyMove => "only_move"
      case EngineSays => "engine_says"
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
      case WinsMaterialByFork => "wins_material_by_fork"
      case WinsQueen => "wins_queen"
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
      case WeakensStructure => "weakens_structure"
      case CreatesWeakness => "creates_weakness"
      case WinsSpace => "wins_space"

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
        ForbiddenWording.WinsQueen,
        ForbiddenWording.DecisiveFork,
        ForbiddenWording.ForcedWin
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
  private val SkewerForbiddenWording =
    LineDefenderForbiddenWording
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

  private def promotionThreatAllowedClaim(verdict: Verdict) =
    Option.when(verdict.role == Role.Lead && verdict.leadAllowed && !verdict.engineStrengthLimited)(
      ExplanationClaim.CreatesPromotionThreat
    )

  private def promotionAllowedClaim(verdict: Verdict) =
    Option.when(verdict.role == Role.Lead && verdict.leadAllowed && !verdict.engineStrengthLimited)(
      ExplanationClaim.PromotesPawn
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

  private def skewerCanPlan(verdict: Verdict) =
    verdict.selected &&
      verdict.role == Role.Lead &&
      verdict.leadAllowed &&
      !verdict.engineStrengthLimited &&
      !verdict.engineCheckStatus.contains(EngineCheckStatus.Refutes)

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
    if story.scene == Scene.Material then fromSelectedMaterial(verdict, story)
    else if story.scene == Scene.Defense then fromSelectedDefense(verdict, story)
    else if story.tactic.contains(Tactic.DiscoveredAttack) then fromSelectedDiscoveredAttack(verdict, story)
    else if story.tactic.contains(Tactic.Pin) then fromSelectedPin(verdict, story)
    else if story.tactic.contains(Tactic.RemoveGuard) then fromSelectedRemoveGuard(verdict, story)
    else if story.tactic.contains(Tactic.Skewer) then fromSelectedSkewer(verdict, story)
    else if story.scene == Scene.PawnAdvance then fromSelectedPawnAdvance(verdict, story)
    else if story.scene == Scene.PawnStop then fromSelectedPawnStop(verdict, story)
    else if story.scene == Scene.PawnBreak then fromSelectedPawnBreak(verdict, story)
    else if story.scene == Scene.PawnCapture then fromSelectedPawnCapture(verdict, story)
    else if story.scene == Scene.PassedPawnCreated then fromSelectedPassedPawnCreated(verdict, story)
    else if story.scene == Scene.FileOpened then fromSelectedFileOpened(verdict, story)
    else if story.scene == Scene.PromotionThreat then fromSelectedPromotionThreat(verdict, story)
    else if story.scene == Scene.Promotion then fromSelectedPromotion(verdict, story)
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
      if tactic == Tactic.Hanging || tactic == Tactic.Fork
      if tactic != Tactic.Fork || story.secondaryTarget.nonEmpty
      if story.side == Side.White || story.side == Side.Black
    yield ExplanationPlan(
      role = verdict.role,
      scene = story.scene,
      tactic = Some(tactic),
      side = story.side,
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
