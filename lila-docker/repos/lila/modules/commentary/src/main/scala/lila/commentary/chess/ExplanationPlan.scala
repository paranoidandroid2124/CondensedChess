package lila.commentary.chess

private[commentary] enum ExplanationClaim:
  case CanWinPiece
  case PieceCanBeTakenWithGain
  case CaptureLeavesMaterialGain

  def key: String =
    this match
      case CanWinPiece               => "can_win_piece"
      case PieceCanBeTakenWithGain   => "piece_can_be_taken_with_gain"
      case CaptureLeavesMaterialGain => "capture_leaves_material_gain"

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

private[commentary] enum ExplanationStrength:
  case Bounded

  def key: String =
    this match
      case Bounded => "bounded"

private[commentary] enum ExplanationRelation:
  case SameFamilyLowerRank
  case AlternativeHangingCandidate
  case CappedSameStory
  case BlockedByEngineRefute

  def key: String =
    this match
      case SameFamilyLowerRank         => "same_family_lower_rank"
      case AlternativeHangingCandidate => "alternative_hanging_candidate"
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

  private def allowedClaim(verdict: Verdict) =
    if verdict.role == Role.Lead && verdict.leadAllowed && !verdict.engineStrengthLimited then
      Some(ExplanationClaim.CanWinPiece)
    else None

  private def relations(verdict: Verdict) =
    val roleRelation =
      verdict.role match
        case Role.Support => Vector(ExplanationRelation.SameFamilyLowerRank)
        case Role.Context => Vector(ExplanationRelation.AlternativeHangingCandidate)
        case Role.Blocked if verdict.engineCheckStatus.contains(EngineCheckStatus.Refutes) =>
          Vector(ExplanationRelation.BlockedByEngineRefute)
        case _ => Vector.empty
    if verdict.engineStrengthLimited then roleRelation :+ ExplanationRelation.CappedSameStory
    else roleRelation

  def fromSelected(verdict: Verdict): Option[ExplanationPlan] =
    val story = verdict.story
    for
      target <- story.target
      anchor <- story.anchor
      route <- story.route
      if verdict.selected
      if story.scene == Scene.Tactic
      if story.tactic.contains(Tactic.Hanging)
      if story.side == Side.White || story.side == Side.Black
    yield ExplanationPlan(
      role = verdict.role,
      scene = story.scene,
      tactic = story.tactic,
      side = story.side,
      target = Some(target),
      anchor = Some(anchor),
      route = Some(route),
      allowedClaim = allowedClaim(verdict),
      evidenceLine = Some(route),
      strength = ExplanationStrength.Bounded,
      forbiddenWording =
        if verdict.engineStrengthLimited then
          HangingForbiddenWording :+ ForbiddenWording.StrongWording
        else HangingForbiddenWording,
      relations = relations(verdict),
      debugOnly = verdict.role == Role.Blocked,
      supportContextLinks = Vector.empty
    )
