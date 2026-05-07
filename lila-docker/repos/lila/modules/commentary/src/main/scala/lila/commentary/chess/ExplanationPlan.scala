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

  def key: String =
    this match
      case CanWinPiece               => "can_win_piece"
      case PieceCanBeTakenWithGain   => "piece_can_be_taken_with_gain"
      case CaptureLeavesMaterialGain => "capture_leaves_material_gain"
      case ForksTwoTargets           => "forks_two_targets"
      case AttacksTwoTargets         => "attacks_two_targets"
      case MaterialBalanceChanges    => "material_balance_changes"
      case LineLeavesMaterialGain    => "line_leaves_material_gain"
      case ExchangeLeavesSideAhead   => "exchange_leaves_side_ahead"

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
      "no_counterplay"
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
      case SameFamilyLowerRank         => "same_family_lower_rank"
      case AlternativeHangingCandidate => "alternative_hanging_candidate"
      case AlternativeForkCandidate    => "alternative_fork_candidate"
      case CappedSameStory             => "capped_same_story"
      case BlockedByEngineRefute       => "blocked_by_engine_refute"

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
  case Outpost
  case StrategicKey
  case Conversion
  case MateNet
  case StrongWording
  case WinsMaterialByFork
  case WinsQueen
  case DecisiveFork
  case ForcedWin

  def key: String =
    this match
      case FreePiece      => "free_piece"
      case Blunder        => "blunder"
      case Winning        => "winning"
      case Decisive       => "decisive"
      case Forced         => "forced"
      case BestMove       => "best_move"
      case OnlyMove       => "only_move"
      case EngineSays     => "engine_says"
      case NoCounterplay  => "no_counterplay"
      case KingUnsafe     => "king_unsafe"
      case FileControl    => "file_control"
      case Outpost        => "outpost"
      case StrategicKey   => "strategic_key"
      case Conversion     => "conversion"
      case MateNet        => "mate_net"
      case StrongWording  => "strong_wording"
      case WinsMaterialByFork => "wins_material_by_fork"
      case WinsQueen          => "wins_queen"
      case DecisiveFork       => "decisive_fork"
      case ForcedWin          => "forced_win"

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
      route: Option[Line]
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
    ForbiddenWording.Basic :+ ForbiddenWording.ForcedWin

  private def tacticAllowedClaim(verdict: Verdict, tactic: Tactic) =
    if verdict.role == Role.Lead && verdict.leadAllowed && !verdict.engineStrengthLimited then
      tactic match
        case Tactic.Hanging => Some(ExplanationClaim.CanWinPiece)
        case Tactic.Fork    => Some(ExplanationClaim.ForksTwoTargets)
        case _              => None
    else None

  private def materialAllowedClaim(verdict: Verdict) =
    Option.when(verdict.role == Role.Lead && verdict.leadAllowed && !verdict.engineStrengthLimited)(
      ExplanationClaim.MaterialBalanceChanges
    )

  private def forbiddenWording(verdict: Verdict, tactic: Tactic) =
    val base =
      tactic match
        case Tactic.Fork => ForkForbiddenWording
        case _           => HangingForbiddenWording
    if verdict.engineStrengthLimited then base :+ ForbiddenWording.StrongWording
    else base

  private def materialForbiddenWording(verdict: Verdict) =
    if verdict.engineStrengthLimited then MaterialForbiddenWording :+ ForbiddenWording.StrongWording
    else MaterialForbiddenWording

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

  def fromSelected(verdict: Verdict): Option[ExplanationPlan] =
    val story = verdict.story
    if story.scene == Scene.Material then fromSelectedMaterial(verdict, story)
    else fromSelectedTactic(verdict, story)

  private def fromSelectedTactic(verdict: Verdict, story: Story): Option[ExplanationPlan] =
    for
      tactic <- story.tactic
      target <- story.target
      anchor <- story.anchor
      route <- story.route
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
      secondaryTarget = story.secondaryTarget,
      allowedClaim = tacticAllowedClaim(verdict, tactic),
      evidenceLine = Some(route),
      strength = ExplanationStrength.Bounded,
      forbiddenWording = forbiddenWording(verdict, tactic),
      relations = relations(verdict, tactic),
      debugOnly = verdict.role == Role.Blocked,
      supportContextLinks = Vector.empty
    )

  private def fromSelectedMaterial(verdict: Verdict, story: Story): Option[ExplanationPlan] =
    for
      target <- story.target
      anchor <- story.anchor
      route <- story.route
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
      secondaryTarget = None,
      allowedClaim = materialAllowedClaim(verdict),
      evidenceLine = Some(route),
      strength = ExplanationStrength.Bounded,
      forbiddenWording = materialForbiddenWording(verdict),
      relations = materialRelations(verdict),
      debugOnly = verdict.role == Role.Blocked,
      supportContextLinks = Vector.empty
    )
