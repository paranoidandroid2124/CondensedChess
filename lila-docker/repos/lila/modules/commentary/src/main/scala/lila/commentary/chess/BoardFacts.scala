package lila.commentary.chess

import lila.commentary.root.RootStateVector

final case class Square private (index: Int):
  def bit: Long = 1L << index
  def file: Int = index % 8
  def rank: Int = index / 8

object Square:
  def fromIndex(index: Int): Square =
    require(index >= 0 && index < 64, "Square index must be 0..63")
    new Square(index)

  def apply(file: Char, rank: Int): Square =
    val f = file.toLower - 'a'
    require(f >= 0 && f < 8, "Square file must be a..h")
    require(rank >= 1 && rank <= 8, "Square rank must be 1..8")
    fromIndex((rank - 1) * 8 + f)

enum Man:
  case Pawn
  case Knight
  case Bishop
  case Rook
  case Queen
  case King

final case class Piece(side: Side, man: Man, square: Square):
  require(side == Side.White || side == Side.Black, "Piece side must be White or Black")

final case class Line(from: Square, to: Square)

final case class BoardHeader(
    known: Boolean = false,
    plyFromStart: Int = 0,
    phaseTotal: Int = 0,
    phaseNonPawn: Int = 0,
    halfmoveClock: Int = 0,
    fullmoveNumber: Int = 0,
    castlingMask: Int = 0,
    epSquare: Option[Square] = None,
    inCheckMask: Int = 0,
    snapshotPly: Int = 0,
    hashLo: Int = 0,
    hashHi: Int = 0
):
  def sane: Boolean =
    known &&
      plyFromStart >= 0 &&
      phaseTotal >= 0 &&
      phaseNonPawn >= 0 &&
      halfmoveClock >= 0 &&
      fullmoveNumber >= 1 &&
      castlingMask >= 0 &&
      castlingMask <= 15 &&
      inCheckMask >= 0 &&
      inCheckMask <= 3 &&
      snapshotPly >= 0

final case class Moves(
    known: Boolean = false,
    lines: Vector[Line] = Vector.empty,
    destinationUnion: Long = 0L,
    moveCount: Int = 0,
    captureCount: Int = 0,
    checkCount: Int = 0
):
  def legalDestinationUnion: Long =
    lines.foldLeft(destinationUnion): (mask, line) =>
      mask | line.to.bit

  def sane: Boolean =
    val destinationsMatchMoveCount =
      if moveCount == 0 then destinationUnion == 0L && lines.isEmpty
      else destinationUnion != 0L || lines.nonEmpty
    known &&
      moveCount >= 0 &&
      captureCount >= 0 &&
      checkCount >= 0 &&
      captureCount <= moveCount &&
      checkCount <= moveCount &&
      destinationsMatchMoveCount

final case class ControlSide(
    space: Int = 0,
    controlledSquares: Int = 0,
    attackedTwice: Int = 0,
    attackedSquares: Long = 0L,
    controlledMask: Long = 0L
):
  def sane: Boolean =
    space >= 0 &&
      controlledSquares >= 0 &&
      controlledSquares <= 64 &&
      attackedTwice >= 0 &&
      attackedTwice <= 64

final case class Control(
    known: Boolean = false,
    white: ControlSide = ControlSide(),
    black: ControlSide = ControlSide(),
    contestedSquares: Int = 0,
    spaceDiff: Int = 0
):
  def sane: Boolean =
    known &&
      white.sane &&
      black.sane &&
      contestedSquares >= 0 &&
      contestedSquares <= 64 &&
      spaceDiff == white.space - black.space

final case class Pieces(
    pawns: Int = 0,
    knights: Int = 0,
    bishops: Int = 0,
    rooks: Int = 0,
    queens: Int = 0,
    kings: Int = 0,
    value: Int = 0
):
  def sane: Boolean =
    Vector(pawns, knights, bishops, rooks, queens, kings, value).forall(_ >= 0)

final case class Material(
    known: Boolean = false,
    white: Pieces = Pieces(),
    black: Pieces = Pieces(),
    diff: Int = 0,
    imbalance: Int = 0
):
  def sane: Boolean =
    known &&
      white.sane &&
      black.sane &&
      white.kings == 1 &&
      black.kings == 1 &&
      diff == white.value - black.value

final case class PawnSide(
    fileCounts: Int = 0,
    isolated: Int = 0,
    backward: Int = 0,
    doubledFiles: Int = 0,
    passed: Int = 0,
    candidatePassers: Int = 0,
    protectedPassers: Int = 0,
    fixed: Int = 0,
    chainBases: Int = 0,
    levers: Int = 0,
    breakChances: Int = 0,
    blockaded: Int = 0,
    bestPromotionDistance: Int = 0,
    support: Int = 0,
    risk: Int = 0,
    structure: Int = 0
):
  def scalars: Vector[Int] = Vector(
    fileCounts,
    isolated,
    backward,
    doubledFiles,
    passed,
    candidatePassers,
    protectedPassers,
    fixed,
    chainBases,
    levers,
    breakChances,
    blockaded,
    bestPromotionDistance,
    support,
    risk,
    structure
  )

  def sane: Boolean =
    scalars.forall(_ >= 0)

final case class Pawns(
    known: Boolean = false,
    white: PawnSide = PawnSide(),
    black: PawnSide = PawnSide()
):
  def sane: Boolean =
    known && white.sane && black.sane

final case class BoardFacts(
    root: RootStateVector,
    sideToMove: Side,
    header: BoardHeader,
    sideLegal: Moves,
    rivalLegal: Moves,
    control: Control,
    material: Material,
    pawns: Pawns,
    pieces: Vector[Piece] = Vector.empty
):
  require(
    sideToMove == Side.White || sideToMove == Side.Black,
    "BoardFacts sideToMove must be White or Black"
  )
