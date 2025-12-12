package chess
package variant

import cats.Eq
import cats.syntax.all.*
import chess.format.Fen

import scala.annotation.nowarn

abstract class Variant(val name: String):

  def initialPieces: Map[Square, Piece]
  def initialBoard: Board
  def initialPosition: Position = Position(initialBoard, this, White)

  inline def standard: Boolean = this == Standard
  inline def chess960: Boolean = false
  inline def fromPosition: Boolean = false
  inline def kingOfTheHill: Boolean = false
  inline def threeCheck: Boolean = false
  inline def antichess: Boolean = false
  inline def atomic: Boolean = false
  inline def horde: Boolean = false
  inline def racingKings: Boolean = false
  inline def crazyhouse: Boolean = false

  def allowsCastling: Boolean = !castles.isEmpty

  def makeUnmovedRooks(rooks: Bitboard): UnmovedRooks =
    if allowsCastling then UnmovedRooks(rooks) else UnmovedRooks.none

  protected val backRank: Vector[Role] =
    Vector(Rook, Knight, Bishop, Queen, King, Bishop, Knight, Rook)

  def castles: Castles = Castles.init

  val initialFen: Fen.Full = Fen.Full.initial

  def isValidPromotion(promotion: Option[PromotableRole]): Boolean =
    promotion.forall {
      case Queen | Rook | Knight | Bishop => true
      case _ => false
    }

  def validMoves(position: Position): List[Move]

  def validMovesAt(position: Position, square: Square): List[Move] =
    import position.us
    position.pieceAt(square).fold(Nil) { piece =>
      if piece.color != position.color then Nil
      else
        val targets = ~us
        val bb = square.bb
        piece.role match
          case Pawn => position.genEnPassant(us & bb) ++ position.genPawn(bb, targets)
          case Knight => position.genKnight(us & bb, targets)
          case Bishop => position.genBishop(us & bb, targets)
          case Rook => position.genRook(us & bb, targets)
          case Queen => position.genQueen(us & bb, targets)
          case King => position.genKingAt(targets, square)
    }

  def pieceThreatened(board: Board, by: Color, to: Square): Boolean =
    board.attacks(to, by)

  def kingThreatened(board: Board, color: Color): Check =
    board.isCheck(color)

  def checkWhite(board: Board): Check = kingThreatened(board, White)
  def checkBlack(board: Board): Check = kingThreatened(board, Black)

  def checkColor(board: Board): Option[Color] =
    checkWhite(board).yes.option(White).orElse(checkBlack(board).yes.option(Black))

  def kingSafety(m: Move): Boolean =
    kingThreatened(m.afterWithoutHistory.board, m.color).no

  def castleCheckSafeSquare(board: Board, kingTo: Square, color: Color, occupied: Bitboard): Boolean =
    board.attackers(kingTo, !color, occupied).isEmpty

  def move(
      position: Position,
      from: Square,
      to: Square,
      promotion: Option[PromotableRole]
  ): Either[ErrorStr, Move] =

    inline def findMove(m: Move): Boolean =
      m.dest == to && m.promotion == promotion.orElse(Option.when(isPromotion(m))(Queen))

    inline def isPromotion(m: Move): Boolean =
      m.piece.is(Pawn) && m.dest.rank == m.piece.color.lastRank

    validMovesAt(position, from)
      .find(findMove)
      .toRight(ErrorStr(s"Piece on ${from.key} cannot move to ${to.key}"))

  def drop(@nowarn position: Position, role: Role, square: Square): Either[ErrorStr, Drop] =
    ErrorStr(s"$this variant cannot drop $role $square").asLeft

  def staleMate(position: Position): Boolean = position.check.no && position.legalMoves.isEmpty

  def checkmate(position: Position): Boolean = position.check.yes && position.legalMoves.isEmpty

  def winner(position: Position): Option[Color] =
    if position.checkMate || specialEnd(position) then Option(!position.color) else None

  def specialEnd(position: Position): Boolean = false

  def specialDraw(position: Position): Boolean = false

  def autoDraw(position: Position): Boolean =
    isInsufficientMaterial(position) || fiftyMoves(position.history) || position.history.fivefoldRepetition

  def materialImbalance(board: Board): Int =
    board.fold(0): (acc, color, role) =>
      Role
        .valueOf(role)
        .fold(acc): value =>
          acc + value * color.fold(1, -1)

  def isInsufficientMaterial(position: Position): Boolean = InsufficientMatingMaterial(position.board)

  def opponentHasInsufficientMaterial(position: Position): Boolean =
    InsufficientMatingMaterial(position.board, !position.color)

  def playerHasInsufficientMaterial(position: Position): Boolean =
    opponentHasInsufficientMaterial(position.withColor(!position.color))

  def fiftyMoves(history: History): Boolean =
    history.halfMoveClock >= HalfMoveClock(100)

  def isIrreversible(move: Move): Boolean =
    move.piece.is(Pawn) || move.captures || move.promotes || move.castles

  protected def pawnsOnPromotionRank(board: Board, color: Color): Boolean =
    board.byPiece(color, Pawn).intersects(Bitboard.rank(color.promotablePawnRank))

  protected def pawnsOnBackRank(board: Board, color: Color): Boolean =
    board.byPiece(color, Pawn).intersects(Bitboard.rank(color.backRank))

  protected def validSide(position: Position, strict: Boolean)(color: Color): Boolean =
    position.byPiece(color, King).count == 1 &&
      (!strict || { position.byPiece(color, Pawn).count <= 8 && position.byColor(color).count <= 16 }) &&
      !pawnsOnPromotionRank(position.board, color) &&
      !pawnsOnBackRank(position.board, color)

  def valid(position: Position, strict: Boolean): Boolean =
    Color.all.forall(validSide(position, strict))

  val promotableRoles: List[PromotableRole] = List(Queen, Rook, Bishop, Knight)

  override def toString = s"Variant($name)"

  override def equals(that: Any): Boolean = this eq that.asInstanceOf[AnyRef]

  override def hashCode: Int = name.hashCode

object Variant:

  given Eq[Variant] = Eq.fromUniversalEquals

  val default: Variant = Standard

  def byName(name: String): Option[Variant] =
    Option.when(name.equalsIgnoreCase("standard") || name.equalsIgnoreCase("chess"))(Standard)
