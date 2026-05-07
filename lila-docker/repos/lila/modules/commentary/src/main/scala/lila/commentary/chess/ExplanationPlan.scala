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
      case DefendsPiece              => "defends_piece"
      case PreventsMaterialLoss      => "prevents_material_loss"
      case ProtectsTarget            => "protects_target"

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
  case BestDefense
  case RefutesAttack
  case StopsCounterplay
  case SolvesPosition
  case KingSafe
  case MateDefense

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
      case BestDefense        => "best_defense"
      case RefutesAttack      => "refutes_attack"
      case StopsCounterplay   => "stops_counterplay"
      case SolvesPosition     => "solves_position"
      case KingSafe           => "king_safe"
      case MateDefense        => "mate_defense"

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
    ForbiddenWording.Basic :+ ForbiddenWording.ForcedWin
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

  private def defenseAllowedClaim(verdict: Verdict) =
    Option.when(verdict.role == Role.Lead && verdict.leadAllowed && !verdict.engineStrengthLimited)(
      ExplanationClaim.DefendsPiece
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
