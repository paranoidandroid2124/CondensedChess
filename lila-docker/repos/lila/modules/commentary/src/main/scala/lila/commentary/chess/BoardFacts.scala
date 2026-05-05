package lila.commentary.chess

import java.util.{ Collections, WeakHashMap }

import chess.format.Fen
import chess.{
  Bishop,
  Bitboard,
  Board,
  Color,
  King,
  Knight,
  Pawn,
  Position,
  Queen,
  Role,
  Rook,
  Square as ChessSquare
}
import lila.commentary.root.{ RootAtomRegistry, RootExtractor, RootStateVector }

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
      snapshotPly >= 0 &&
      hashLo == 0 &&
      hashHi == 0

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
      diff == white.value - black.value &&
      imbalance == 0

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

final class BoardFacts private (
    val root: RootStateVector,
    val sideToMove: Side,
    val header: BoardHeader,
    val sideLegal: Moves,
    val rivalLegal: Moves,
    val control: Control,
    val material: Material,
    val pawns: Pawns,
    val pieces: Vector[Piece]
):
  require(
    sideToMove == Side.White || sideToMove == Side.Black,
    "BoardFacts sideToMove must be White or Black"
  )
  private[commentary] def sameBoardReady: Boolean =
    BoardFacts.sameBoardReady(this)

object BoardFacts:

  private val sameBoardFacts =
    Collections.synchronizedMap(new WeakHashMap[BoardFacts, java.lang.Boolean])

  private def markSameBoard(facts: BoardFacts): BoardFacts =
    sameBoardFacts.put(facts, java.lang.Boolean.TRUE)
    facts

  private[commentary] def sameBoardReady(facts: BoardFacts): Boolean =
    sameBoardFacts.containsKey(facts)

  def fromFen(fenInput: Fen.Full | String): Either[String, BoardFacts] =
    val fen = Fen.Full(fenInput.toString)
    RootExtractor
      .fromFenWithPositionFailClosed(fen)
      .flatMap: rooted =>
        fenHeader(fen).map(headerParts => fromRooted(rooted, headerParts))

  private[commentary] def untrusted(
      root: RootStateVector,
      sideToMove: Side,
      header: BoardHeader,
      sideLegal: Moves,
      rivalLegal: Moves,
      control: Control,
      material: Material,
      pawns: Pawns,
      pieces: Vector[Piece] = Vector.empty
  ): BoardFacts =
    BoardFacts(
      root = root,
      sideToMove = sideToMove,
      header = header,
      sideLegal = sideLegal,
      rivalLegal = rivalLegal,
      control = control,
      material = material,
      pawns = pawns,
      pieces = pieces
    )

  private[commentary] def fromPosition(position: Position, fullmoveNumber: Int): Either[String, BoardFacts] =
    Either
      .cond(fullmoveNumber >= 1, fullmoveNumber, s"Invalid fullmove number: $fullmoveNumber")
      .flatMap: validFullmoveNumber =>
        RootExtractor
          .fromPositionFailClosed(position)
          .map: rooted =>
            fromRooted(
              rooted,
              HeaderParts(
                halfmoveClock = position.history.halfMoveClock.value,
                fullmoveNumber = validFullmoveNumber,
                castlingMask = castlingMask(position),
                epSquare = position.enPassantSquare.map(square => Square.fromIndex(square.value))
              )
            )

  private final case class HeaderParts(
      halfmoveClock: Int,
      fullmoveNumber: Int,
      castlingMask: Int,
      epSquare: Option[Square]
  )

  private def fromRooted(rooted: RootExtractor.RootWithPosition, headerParts: HeaderParts): BoardFacts =
    val position = rooted.position
    val ply = plyFromStart(headerParts.fullmoveNumber, position.color)
    markSameBoard(BoardFacts(
      root = rooted.root,
      sideToMove = sideFromColor(position.color),
      header = BoardHeader(
        known = true,
        plyFromStart = ply,
        phaseTotal = phaseTotal(position.board),
        phaseNonPawn = phaseNonPawn(position.board),
        halfmoveClock = headerParts.halfmoveClock,
        fullmoveNumber = headerParts.fullmoveNumber,
        castlingMask = headerParts.castlingMask,
        epSquare = headerParts.epSquare,
        inCheckMask = inCheckMask(position),
        snapshotPly = ply,
        hashLo = 0,
        hashHi = 0
      ),
      sideLegal = movesFor(position),
      rivalLegal = movesFor(position.withColor(!position.color)),
      control = control(position),
      material = material(position.board),
      pawns = pawns(position.board, rooted.root),
      pieces = pieces(position.board)
    ))

  private def fenHeader(fen: Fen.Full): Either[String, HeaderParts] =
    val parts = fen.value.split(' ')
    for
      halfmove <- parseNonNegative(parts.lift(4), "halfmove clock", fen)
      fullmove <- parsePositive(parts.lift(5), "fullmove number", fen)
      castling <- RootAtomRegistry
        .castlingRightsMask(parts.lift(2).getOrElse("-"))
        .toRight(s"Invalid castling-rights field: $fen")
      epSquare <- parseEpSquare(parts.lift(3).getOrElse("-"), fen)
    yield HeaderParts(
      halfmoveClock = halfmove,
      fullmoveNumber = fullmove,
      castlingMask = castling,
      epSquare = epSquare
    )

  private def parseNonNegative(raw: Option[String], field: String, fen: Fen.Full): Either[String, Int] =
    raw
      .flatMap(value => value.toIntOption)
      .filter(_ >= 0)
      .toRight(s"Invalid $field: $fen")

  private def parsePositive(raw: Option[String], field: String, fen: Fen.Full): Either[String, Int] =
    parseNonNegative(raw, field, fen).flatMap: value =>
      Either.cond(value >= 1, value, s"Invalid $field: $fen")

  private def parseEpSquare(raw: String, fen: Fen.Full): Either[String, Option[Square]] =
    if raw == "-" then Right(None)
    else
      ChessSquare
        .fromKey(raw)
        .map(square => Some(Square.fromIndex(square.value)))
        .toRight(s"Invalid en-passant square: $fen")

  private def movesFor(position: Position): Moves =
    val legal = position.legalMoves.toVector
    Moves(
      known = true,
      lines = legal.map(move => Line(Square.fromIndex(move.orig.value), Square.fromIndex(move.dest.value))),
      moveCount = legal.size,
      captureCount = legal.count(move => move.capture.isDefined || move.enpassant),
      checkCount = legal.count(move => move.after.check.yes)
    )

  private def control(position: Position): Control =
    val whiteAttacks = attackMasks(position.board, Color.White)
    val blackAttacks = attackMasks(position.board, Color.Black)
    val whiteUnion = whiteAttacks.foldLeft(Bitboard.empty)(_ | _)
    val blackUnion = blackAttacks.foldLeft(Bitboard.empty)(_ | _)
    val occupied = position.board.occupied.value
    val whiteSpace = space(whiteUnion.value, blackUnion.value, occupied, Color.White)
    val blackSpace = space(blackUnion.value, whiteUnion.value, occupied, Color.Black)
    Control(
      known = true,
      white = ControlSide(
        space = whiteSpace,
        controlledSquares = Bitboard.count(whiteUnion),
        attackedTwice = attackedTwice(whiteAttacks),
        attackedSquares = whiteUnion.value,
        controlledMask = whiteUnion.value
      ),
      black = ControlSide(
        space = blackSpace,
        controlledSquares = Bitboard.count(blackUnion),
        attackedTwice = attackedTwice(blackAttacks),
        attackedSquares = blackUnion.value,
        controlledMask = blackUnion.value
      ),
      contestedSquares = Bitboard.count(whiteUnion & blackUnion),
      spaceDiff = whiteSpace - blackSpace
    )

  private def space(controlled: Long, opposed: Long, occupied: Long, color: Color): Int =
    java.lang.Long.bitCount(spaceZoneMask(color) & controlled & ~opposed & ~occupied)

  private def spaceZoneMask(color: Color): Long =
    val ranks = if color.white then 3 to 5 else 2 to 4
    (for
      rank <- ranks
      file <- 2 to 5
    yield 1L << (rank * 8 + file)).foldLeft(0L)(_ | _)

  private def attackMasks(board: Board, color: Color): Vector[Bitboard] =
    board.pieceMap.iterator
      .collect:
        case (square, piece) if piece.color == color => attackMaskFrom(board, square, piece)
      .toVector

  private def attackMaskFrom(board: Board, square: ChessSquare, piece: chess.Piece): Bitboard =
    piece.role match
      case Pawn => square.pawnAttacks(piece.color)
      case Knight => square.knightAttacks
      case Bishop => square.bishopAttacks(board.occupied)
      case Rook => square.rookAttacks(board.occupied)
      case Queen => square.queenAttacks(board.occupied)
      case King => square.kingAttacks

  private def attackedTwice(masks: Vector[Bitboard]): Int =
    val counts = Array.fill(64)(0)
    masks.foreach: mask =>
      Bitboard.foreach(mask): square =>
        counts(square.value) += 1
    counts.count(_ >= 2)

  private def material(board: Board): Material =
    val white = piecesFor(board, Color.White)
    val black = piecesFor(board, Color.Black)
    Material(
      known = true,
      white = white,
      black = black,
      diff = white.value - black.value,
      imbalance = 0
    )

  private def piecesFor(board: Board, color: Color): Pieces =
    val pawns = board.count(color, Pawn)
    val knights = board.count(color, Knight)
    val bishops = board.count(color, Bishop)
    val rooks = board.count(color, Rook)
    val queens = board.count(color, Queen)
    val kings = board.count(color, King)
    Pieces(
      pawns = pawns,
      knights = knights,
      bishops = bishops,
      rooks = rooks,
      queens = queens,
      kings = kings,
      value = pawns * 100 + knights * 320 + bishops * 330 + rooks * 500 + queens * 900
    )

  private def pawns(board: Board, root: RootStateVector): Pawns =
    Pawns(
      known = true,
      white = pawnSide(board, root, Color.White),
      black = pawnSide(board, root, Color.Black)
    )

  private def pawnSide(board: Board, root: RootStateVector, color: Color): PawnSide =
    val pawnSquares = Bitboard.squares(board.byPiece(color, Pawn)).toVector
    PawnSide(
      fileCounts = packedPawnFileCounts(pawnSquares),
      isolated = rootPawnCount(root, RootAtomRegistry.SchemaId.IsolatedPawn, color),
      backward = rootPawnCount(root, RootAtomRegistry.SchemaId.BackwardPawn, color),
      doubledFiles =
        root.fileMask8(RootAtomRegistry.SchemaId.DoubledFile, Some(color)).fold(0)(Integer.bitCount),
      passed = rootPawnCount(root, RootAtomRegistry.SchemaId.PassedPawn, color),
      candidatePassers = rootPawnCount(root, RootAtomRegistry.SchemaId.CandidatePasser, color),
      protectedPassers = 0,
      fixed = rootPawnCount(root, RootAtomRegistry.SchemaId.FixedPawn, color),
      chainBases = 0,
      levers = rootPawnCount(root, RootAtomRegistry.SchemaId.LeverAvailable, color),
      breakChances = 0,
      blockaded = 0,
      bestPromotionDistance = bestPromotionDistance(pawnSquares, color),
      support = 0,
      risk = 0,
      structure = 0
    )

  private def rootPawnCount(root: RootStateVector, schemaId: String, color: Color): Int =
    java.lang.Long.bitCount(root.squareMask64(schemaId, Some(color)).getOrElse(0L))

  private def packedPawnFileCounts(pawnSquares: Vector[ChessSquare]): Int =
    pawnSquares.foldLeft(0): (packed, square) =>
      val shift = square.file.value * 4
      val count = ((packed >>> shift) & 0xf) + 1
      (packed & ~(0xf << shift)) | (count << shift)

  private def bestPromotionDistance(pawnSquares: Vector[ChessSquare], color: Color): Int =
    if pawnSquares.isEmpty then 0
    else
      pawnSquares
        .map: square =>
          if color.white then 7 - square.rank.value else square.rank.value
        .min

  private def pieces(board: Board): Vector[Piece] =
    board.pieceMap.iterator
      .map:
        case (square, piece) =>
          Piece(sideFromColor(piece.color), manFromRole(piece.role), Square.fromIndex(square.value))
      .toVector

  private def plyFromStart(fullmoveNumber: Int, color: Color): Int =
    (fullmoveNumber - 1) * 2 + (if color.black then 1 else 0)

  private def inCheckMask(position: Position): Int =
    (if position.isCheck(Color.White).yes then 1 else 0) |
      (if position.isCheck(Color.Black).yes then 2 else 0)

  private def castlingMask(position: Position): Int =
    val castles = position.history.castles
    (if castles.whiteKingSide then 1 else 0) |
      (if castles.whiteQueenSide then 2 else 0) |
      (if castles.blackKingSide then 4 else 0) |
      (if castles.blackQueenSide then 8 else 0)

  private def phaseTotal(board: Board): Int =
    RootAtomRegistry.canonicalColors
      .map: color =>
        board.count(color, Knight) + board.count(color, Bishop) +
          board.count(color, Rook) * 2 + board.count(color, Queen) * 4
      .sum

  private def phaseNonPawn(board: Board): Int =
    RootAtomRegistry.canonicalColors
      .map: color =>
        board.count(color, Knight) + board.count(color, Bishop) + board
          .count(color, Rook) + board.count(color, Queen)
      .sum

  private def sideFromColor(color: Color): Side =
    if color.white then Side.White else Side.Black

  private def manFromRole(role: Role): Man =
    role match
      case Pawn => Man.Pawn
      case Knight => Man.Knight
      case Bishop => Man.Bishop
      case Rook => Man.Rook
      case Queen => Man.Queen
      case King => Man.King
