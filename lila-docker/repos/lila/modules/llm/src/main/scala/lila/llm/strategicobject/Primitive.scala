package lila.llm.strategicobject

import chess.{ Color, File, Role, Square }

sealed trait Primitive:
  def owner: Color

final case class TargetSquare(
    owner: Color,
    square: Square,
    targetOwner: Color,
    occupant: Option[Role],
    attackerCount: Int,
    defenderCount: Int,
    fixed: Boolean
) extends Primitive

final case class BreakAxis(
    owner: Color,
    file: File,
    breakSquare: Square,
    targetSquares: List[Square]
) extends Primitive

final case class EntrySquare(
    owner: Color,
    square: Square,
    lane: File,
    supportingRoles: Set[Role]
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
    roles: Set[Role],
    entrySquares: List[Square]
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

final case class PrimitiveBank(
    targetSquares: List[TargetSquare] = Nil,
    breakAxes: List[BreakAxis] = Nil,
    entrySquares: List[EntrySquare] = Nil,
    exchangeSquares: List[ExchangeSquare] = Nil,
    accessRoutes: List[AccessRoute] = Nil,
    defendedResources: List[DefendedResource] = Nil,
    pieceRoleIssues: List[PieceRoleIssue] = Nil,
    criticalSquares: List[CriticalSquare] = Nil,
    passerSeeds: List[PasserSeed] = Nil
):

  def all: List[Primitive] =
    targetSquares ++
      breakAxes ++
      entrySquares ++
      exchangeSquares ++
      accessRoutes ++
      defendedResources ++
      pieceRoleIssues ++
      criticalSquares ++
      passerSeeds

  def normalized: PrimitiveBank =
    copy(
      targetSquares = targetSquares.distinct.sortBy(t => (colorIndex(t.owner), t.square.key)),
      breakAxes = breakAxes.distinct.sortBy(a => (colorIndex(a.owner), a.file.char.toString)),
      entrySquares = entrySquares.distinct.sortBy(e => (colorIndex(e.owner), e.square.key)),
      exchangeSquares = exchangeSquares.distinct.sortBy(e => (colorIndex(e.owner), e.square.key)),
      accessRoutes = accessRoutes.distinct.sortBy(r => (colorIndex(r.owner), r.file.char.toString)),
      defendedResources = defendedResources.distinct.sortBy(r => (colorIndex(r.owner), r.square.key)),
      pieceRoleIssues = pieceRoleIssues.distinct.sortBy(i => (colorIndex(i.owner), i.square.key, i.issue.toString)),
      criticalSquares = criticalSquares.distinct.sortBy(c => (colorIndex(c.owner), c.square.key, c.kind.toString)),
      passerSeeds = passerSeeds.distinct.sortBy(p => (colorIndex(p.owner), p.square.key))
    )

  def hasTarget(owner: Color, square: Square): Boolean =
    targetSquares.exists(p => p.owner == owner && p.square == square)

  def hasBreakAxis(owner: Color, file: File): Boolean =
    breakAxes.exists(p => p.owner == owner && p.file == file)

  def hasEntrySquare(owner: Color, square: Square): Boolean =
    entrySquares.exists(p => p.owner == owner && p.square == square)

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

private def colorIndex(color: Color): Int =
  if color.white then 0 else 1
