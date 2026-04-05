package lila.llm.strategicobject

import chess.{ Color, File, Role, Square }

sealed trait Primitive:
  def owner: Color

enum BreakMode:
  case Push
  case Capture

final case class TargetSquare(
    owner: Color,
    square: Square,
    targetOwner: Color,
    occupant: Option[Role],
    attackerCount: Int,
    defenderCount: Int,
    fixed: Boolean
) extends Primitive

final case class BreakCandidate(
    owner: Color,
    sourceSquare: Square,
    file: File,
    breakSquare: Square,
    mode: BreakMode,
    targetSquares: List[Square],
    supportCount: Int,
    resistanceCount: Int
) extends Primitive

final case class RouteContestSeed(
    owner: Color,
    square: Square,
    lane: File,
    carrierSquares: List[Square],
    supportingRoles: Set[Role],
    attackerCount: Int,
    defenderCount: Int
) extends Primitive

final case class ExchangeSquare(
    owner: Color,
    square: Square,
    targetOwner: Color,
    occupant: Role,
    attackerCount: Int,
    defenderCount: Int
) extends Primitive

final case class AccessRoute(
    owner: Color,
    file: File,
    carrierSquares: List[Square],
    roles: Set[Role]
) extends Primitive

final case class DefendedResource(
    owner: Color,
    square: Square,
    role: Role,
    attackerCount: Int,
    defenderCount: Int
) extends Primitive

enum PieceRoleIssueKind:
  case BadBishop
  case TrappedPiece

final case class PieceRoleIssue(
    owner: Color,
    square: Square,
    role: Role,
    issue: PieceRoleIssueKind
) extends Primitive

enum CriticalSquareKind:
  case Outpost
  case PromotionSquare
  case BreakContact

final case class CriticalSquare(
    owner: Color,
    square: Square,
    kind: CriticalSquareKind,
    pressure: Int
) extends Primitive

final case class PasserSeed(
    owner: Color,
    square: Square,
    protectedByPawn: Boolean,
    relativeRank: Int
) extends Primitive

final case class DiagonalLaneSeed(
    owner: Color,
    origin: Square,
    target: Square,
    role: Role,
    attackerCount: Int,
    defenderCount: Int
) extends Primitive

final case class LiftCorridorSeed(
    owner: Color,
    origin: Square,
    liftSquare: Square,
    target: Square,
    attackerCount: Int,
    defenderCount: Int
) extends Primitive

final case class KnightRouteSeed(
    owner: Color,
    origin: Square,
    via: Square,
    target: Square,
    attackerCount: Int,
    defenderCount: Int
) extends Primitive

final case class RedeploymentPathSeed(
    owner: Color,
    origin: Square,
    via: Square,
    target: Square,
    role: Role,
    mobilityGain: Int
) extends Primitive

final case class LeverContactSeed(
    owner: Color,
    from: Square,
    target: Square,
    supportCount: Int,
    resistanceCount: Int,
    flank: Boolean
) extends Primitive

final case class HookContactSeed(
    owner: Color,
    from: Square,
    createSquare: Square,
    target: Square,
    supportCount: Int,
    resistanceCount: Int
) extends Primitive

final case class CounterplayResourceSeed(
    owner: Color,
    square: Square,
    role: Role,
    pressureSquares: List[Square],
    supportCount: Int,
    attackerCount: Int
) extends Primitive

final case class TensionContactSeed(
    owner: Color,
    from: Square,
    target: Square,
    supportCount: Int,
    resistanceCount: Int,
    maintainable: Boolean,
    flank: Boolean
) extends Primitive

enum ReleaseCandidateKind:
  case Capture
  case Exchange
  case Break

final case class ReleaseCandidate(
    owner: Color,
    from: Square,
    target: Square,
    kind: ReleaseCandidateKind,
    supportCount: Int,
    resistanceCount: Int
) extends Primitive

final case class PrimitiveBank(
    targetSquares: List[TargetSquare] = Nil,
    breakCandidates: List[BreakCandidate] = Nil,
    routeContestSeeds: List[RouteContestSeed] = Nil,
    exchangeSquares: List[ExchangeSquare] = Nil,
    accessRoutes: List[AccessRoute] = Nil,
    defendedResources: List[DefendedResource] = Nil,
    pieceRoleIssues: List[PieceRoleIssue] = Nil,
    criticalSquares: List[CriticalSquare] = Nil,
    passerSeeds: List[PasserSeed] = Nil,
    diagonalLaneSeeds: List[DiagonalLaneSeed] = Nil,
    liftCorridorSeeds: List[LiftCorridorSeed] = Nil,
    knightRouteSeeds: List[KnightRouteSeed] = Nil,
    redeploymentPathSeeds: List[RedeploymentPathSeed] = Nil,
    leverContactSeeds: List[LeverContactSeed] = Nil,
    hookContactSeeds: List[HookContactSeed] = Nil,
    counterplayResourceSeeds: List[CounterplayResourceSeed] = Nil,
    tensionContactSeeds: List[TensionContactSeed] = Nil,
    releaseCandidates: List[ReleaseCandidate] = Nil
):

  def all: List[Primitive] =
    targetSquares ++
      breakCandidates ++
      routeContestSeeds ++
      exchangeSquares ++
      accessRoutes ++
      defendedResources ++
      pieceRoleIssues ++
      criticalSquares ++
      passerSeeds ++
      diagonalLaneSeeds ++
      liftCorridorSeeds ++
      knightRouteSeeds ++
      redeploymentPathSeeds ++
      leverContactSeeds ++
      hookContactSeeds ++
      counterplayResourceSeeds ++
      tensionContactSeeds ++
      releaseCandidates

  def normalized: PrimitiveBank =
    copy(
      targetSquares = targetSquares.distinct.sortBy(t => (colorIndex(t.owner), t.square.key)),
      breakCandidates =
        breakCandidates.distinct.sortBy(a => (colorIndex(a.owner), a.file.char.toString, a.sourceSquare.key, a.breakSquare.key)),
      routeContestSeeds =
        routeContestSeeds.distinct.sortBy(e => (colorIndex(e.owner), e.lane.char.toString, e.square.key)),
      exchangeSquares = exchangeSquares.distinct.sortBy(e => (colorIndex(e.owner), e.square.key)),
      accessRoutes = accessRoutes.distinct.sortBy(r => (colorIndex(r.owner), r.file.char.toString)),
      defendedResources = defendedResources.distinct.sortBy(r => (colorIndex(r.owner), r.square.key)),
      pieceRoleIssues = pieceRoleIssues.distinct.sortBy(i => (colorIndex(i.owner), i.square.key, i.issue.toString)),
      criticalSquares = criticalSquares.distinct.sortBy(c => (colorIndex(c.owner), c.square.key, c.kind.toString)),
      passerSeeds = passerSeeds.distinct.sortBy(p => (colorIndex(p.owner), p.square.key)),
      diagonalLaneSeeds = diagonalLaneSeeds.distinct.sortBy(p => (colorIndex(p.owner), p.origin.key, p.target.key)),
      liftCorridorSeeds = liftCorridorSeeds.distinct.sortBy(p => (colorIndex(p.owner), p.origin.key, p.liftSquare.key, p.target.key)),
      knightRouteSeeds = knightRouteSeeds.distinct.sortBy(p => (colorIndex(p.owner), p.origin.key, p.via.key, p.target.key)),
      redeploymentPathSeeds =
        redeploymentPathSeeds.distinct.sortBy(p => (colorIndex(p.owner), p.origin.key, p.via.key, p.target.key)),
      leverContactSeeds = leverContactSeeds.distinct.sortBy(p => (colorIndex(p.owner), p.from.key, p.target.key)),
      hookContactSeeds = hookContactSeeds.distinct.sortBy(p => (colorIndex(p.owner), p.from.key, p.target.key)),
      counterplayResourceSeeds =
        counterplayResourceSeeds.distinct.sortBy(p => (colorIndex(p.owner), p.square.key, p.role.toString)),
      tensionContactSeeds = tensionContactSeeds.distinct.sortBy(p => (colorIndex(p.owner), p.from.key, p.target.key)),
      releaseCandidates = releaseCandidates.distinct.sortBy(p => (colorIndex(p.owner), p.from.key, p.target.key, p.kind.toString))
    )

  def hasTarget(owner: Color, square: Square): Boolean =
    targetSquares.exists(p => p.owner == owner && p.square == square)

  def hasBreakCandidate(owner: Color, file: File): Boolean =
    breakCandidates.exists(p => p.owner == owner && p.file == file)

  def hasRouteContestSeed(owner: Color, square: Square): Boolean =
    routeContestSeeds.exists(p => p.owner == owner && p.square == square)

  def hasExchangeSquare(owner: Color, square: Square): Boolean =
    exchangeSquares.exists(p => p.owner == owner && p.square == square)

  def hasAccessRoute(owner: Color, file: File): Boolean =
    accessRoutes.exists(p => p.owner == owner && p.file == file)

  def hasDefendedResource(owner: Color, square: Square): Boolean =
    defendedResources.exists(p => p.owner == owner && p.square == square)

  def hasPieceRoleIssue(
      owner: Color,
      square: Square,
      issue: PieceRoleIssueKind
  ): Boolean =
    pieceRoleIssues.exists(p => p.owner == owner && p.square == square && p.issue == issue)

  def hasCriticalSquare(
      owner: Color,
      square: Square,
      kind: CriticalSquareKind
  ): Boolean =
    criticalSquares.exists(p => p.owner == owner && p.square == square && p.kind == kind)

  def hasPasserSeed(owner: Color, square: Square): Boolean =
    passerSeeds.exists(p => p.owner == owner && p.square == square)

  def hasBreakCandidateAt(owner: Color, sourceSquare: Square, breakSquare: Square): Boolean =
    breakCandidates.exists(p => p.owner == owner && p.sourceSquare == sourceSquare && p.breakSquare == breakSquare)

  def hasDiagonalLaneSeed(owner: Color, origin: Square, target: Square): Boolean =
    diagonalLaneSeeds.exists(p => p.owner == owner && p.origin == origin && p.target == target)

  def hasLiftCorridorSeed(owner: Color, origin: Square, target: Square): Boolean =
    liftCorridorSeeds.exists(p => p.owner == owner && p.origin == origin && p.target == target)

  def hasKnightRouteSeed(owner: Color, origin: Square, target: Square): Boolean =
    knightRouteSeeds.exists(p => p.owner == owner && p.origin == origin && p.target == target)

  def hasRedeploymentPathSeed(owner: Color, origin: Square, target: Square): Boolean =
    redeploymentPathSeeds.exists(p => p.owner == owner && p.origin == origin && p.target == target)

  def hasLeverContactSeed(owner: Color, from: Square, target: Square): Boolean =
    leverContactSeeds.exists(p => p.owner == owner && p.from == from && p.target == target)

  def hasHookContactSeed(owner: Color, from: Square, target: Square): Boolean =
    hookContactSeeds.exists(p => p.owner == owner && p.from == from && p.target == target)

  def hasCounterplayResourceSeed(owner: Color, square: Square): Boolean =
    counterplayResourceSeeds.exists(p => p.owner == owner && p.square == square)

  def hasTensionContactSeed(owner: Color, from: Square, target: Square): Boolean =
    tensionContactSeeds.exists(p => p.owner == owner && p.from == from && p.target == target)

  def hasReleaseCandidate(owner: Color, from: Square, target: Square): Boolean =
    releaseCandidates.exists(p => p.owner == owner && p.from == from && p.target == target)

private def colorIndex(color: Color): Int =
  if color.white then 0 else 1
