package lila.commentary.witness.u

import chess.{ Bishop, Bitboard, Board, Color, File, King, Knight, Pawn, Piece, Position, Queen, Rank, Role, Rook, Square }
import chess.variant

import lila.commentary.root.RootAtomRegistry
import lila.commentary.root.RootAtomRegistry.*
import lila.commentary.root.{ RootStateVector }
import lila.commentary.witness.*

private[commentary] final case class PawnAdvance(
    from: Square,
    to: Square,
    steps: Int,
    path: Vector[Square]
)

private[commentary] final case class UExtractionContext(rootState: RootStateVector):

  val board: WitnessBoardView = WitnessBoardView.fromRoot(rootState)

  lazy val sideToMove: Option[Color] =
    singleActiveIndex(SchemaId.SideToMove).map: index =>
      canonicalColors(index - requireSchema(SchemaId.SideToMove).start)

  lazy val castlingRightsMask: Option[Int] =
    singleActiveIndex(SchemaId.CastlingRights).map(index => index - requireSchema(SchemaId.CastlingRights).start)

  lazy val enPassantLabel: Option[String] =
    singleActiveIndex(SchemaId.EnPassantState).flatMap(RootAtomRegistry.enPassantState)

  private lazy val legalPositionsByColor: Map[Color, Position] =
    canonicalColors.map: color =>
      color -> Position(board.toBoard, variant.Standard, color)
    .toMap

  def hasPieceOn(color: Color, role: Role, square: Square): Boolean =
    rootState.contains(pieceOnIndex(color, role, square))

  def pieceOnRootIndex(color: Color, role: Role, square: Square): Option[Int] =
    Option.when(hasPieceOn(color, role, square))(pieceOnIndex(color, role, square))

  def hasColorSquare(schemaId: String, color: Color, square: Square): Boolean =
    rootState.contains(colorSquareIndex(schemaId, color, square))

  def colorSquareRootIndex(schemaId: String, color: Color, square: Square): Option[Int] =
    Option.when(hasColorSquare(schemaId, color, square))(colorSquareIndex(schemaId, color, square))

  def hasColorPawnSquare(schemaId: String, color: Color, square: Square): Boolean =
    colorPawnSquareIndex(schemaId, color, square).exists(rootState.contains)

  def colorPawnSquareRootIndex(schemaId: String, color: Color, square: Square): Option[Int] =
    colorPawnSquareIndex(schemaId, color, square).filter(rootState.contains)

  def hasColorFile(schemaId: String, color: Color, file: File): Boolean =
    rootState.contains(colorFileIndex(schemaId, color, file))

  def colorFileRootIndex(schemaId: String, color: Color, file: File): Option[Int] =
    Option.when(hasColorFile(schemaId, color, file))(colorFileIndex(schemaId, color, file))

  def hasNeutralSquare(schemaId: String, square: Square): Boolean =
    rootState.contains(neutralSquareIndex(schemaId, square))

  def neutralSquareRootIndex(schemaId: String, square: Square): Option[Int] =
    Option.when(hasNeutralSquare(schemaId, square))(neutralSquareIndex(schemaId, square))

  def hasNeutralFile(schemaId: String, file: File): Boolean =
    rootState.contains(neutralFileIndex(schemaId, file))

  def neutralFileRootIndex(schemaId: String, file: File): Option[Int] =
    Option.when(hasNeutralFile(schemaId, file))(neutralFileIndex(schemaId, file))

  def pieceAt(square: Square): Option[Piece] =
    board.pieceAt(square)

  def activePieceSquares(color: Color, role: Role): Vector[Square] =
    board.squaresOf(color, role)

  def activeColorSquares(schemaId: String, color: Color): Vector[Square] =
    canonicalSquares.filter(square => hasColorSquare(schemaId, color, square))

  def activeColorPawnSquares(schemaId: String, color: Color): Vector[Square] =
    canonicalPawnSquares.filter(square => hasColorPawnSquare(schemaId, color, square))

  def activeColorFiles(schemaId: String, color: Color): Vector[File] =
    canonicalFiles.filter(file => hasColorFile(schemaId, color, file))

  def activeNeutralSquares(schemaId: String): Vector[Square] =
    canonicalSquares.filter(square => hasNeutralSquare(schemaId, square))

  def activeNeutralFiles(schemaId: String): Vector[File] =
    canonicalFiles.filter(file => hasNeutralFile(schemaId, file))

  def weakSquaresFor(color: Color): Vector[Square] =
    activeColorSquares(SchemaId.WeakSquare, color)

  def outpostSquaresFor(color: Color): Vector[Square] =
    activeColorSquares(SchemaId.OutpostSquare, color)

  def loosePieceSquaresFor(color: Color): Vector[Square] =
    activeColorSquares(SchemaId.LoosePiece, color)

  def pinnedPieceSquaresFor(color: Color): Vector[Square] =
    activeColorSquares(SchemaId.PinnedPiece, color)

  def overloadedPieceSquaresFor(color: Color): Vector[Square] =
    activeColorSquares(SchemaId.OverloadedPiece, color)

  def trappedPieceSquaresFor(color: Color): Vector[Square] =
    activeColorSquares(SchemaId.TrappedPiece, color)

  def xrayTargetSquaresFor(color: Color): Vector[Square] =
    activeColorSquares(SchemaId.XrayTarget, color)

  def kingRingSquaresFor(color: Color): Vector[Square] =
    activeColorSquares(SchemaId.KingRingSquare, color)

  def forwardSquare(color: Color, from: Square, steps: Int = 1): Option[Square] =
    Square.at(from.file.value, from.rank.value + (if color.white then steps else -steps))

  def backwardSquare(color: Color, from: Square, steps: Int = 1): Option[Square] =
    forwardSquare(!color, from, steps)

  def pawnAdvancesFrom(color: Color, from: Square): Vector[PawnAdvance] =
    Option.when(hasPieceOn(color, Pawn, from)):
      legalPositionsByColor(color).generateMovesAt(from).flatMap: move =>
        val rankDelta =
          if color.white then move.dest.rank.value - from.rank.value
          else from.rank.value - move.dest.rank.value

        Option.when(
          move.piece.role == Pawn &&
            move.orig == from &&
            move.dest.file == from.file &&
            move.promotion.isEmpty &&
            !move.captures &&
            (rankDelta == 1 || rankDelta == 2)
        ):
          val path =
            if rankDelta == 1 then Vector(move.dest)
            else Vector(forwardSquare(color, from).get, move.dest)

          PawnAdvance(
            from = from,
            to = move.dest,
            steps = rankDelta,
            path = path
          )
      .sortBy(advance => (advance.steps, advance.to.value)).toVector
    .getOrElse(Vector.empty)

  private def singleActiveIndex(schemaId: String): Option[Int] =
    val active = rootState.activeIndicesForSchema(schemaId)
    require(
      active.size <= 1,
      s"Malformed one-hot root state for $schemaId: ${active.mkString(", ")}"
    )
    active.headOption

private[commentary] final case class WitnessBoardView private (pieceMap: Map[Square, Piece]):

  val occupied: Bitboard = Bitboard(pieceMap.keys.toList)

  def toBoard: Board =
    Board.fromMap(pieceMap)

  private val squaresByColor: Map[Color, Vector[Square]] =
    canonicalColors
      .map: color =>
        color -> pieceMap.iterator
          .collect { case (square, piece) if piece.color == color => square }
          .toVector
          .sortBy(_.value)
      .toMap

  private val squaresByColorAndRole: Map[(Color, Role), Vector[Square]] =
    (for
      color <- canonicalColors
      role <- canonicalRoles
    yield (color, role) -> pieceMap.iterator
      .collect { case (square, piece) if piece.color == color && piece.role == role => square }
      .toVector
      .sortBy(_.value)).toMap

  def pieceAt(square: Square): Option[Piece] = pieceMap.get(square)

  def without(square: Square): WitnessBoardView =
    WitnessBoardView(pieceMap - square)

  def squaresOf(color: Color): Vector[Square] =
    squaresByColor.getOrElse(color, Vector.empty)

  def squaresOf(color: Color, role: Role): Vector[Square] =
    squaresByColorAndRole.getOrElse((color, role), Vector.empty)

  def kingSquare(color: Color): Option[Square] =
    squaresOf(color, King).headOption

  def rayToEdge(source: Square, direction: WitnessDirection): Vector[Square] =
    direction.raySquaresFrom(source)

  def rayUntilBlocked(source: Square, direction: WitnessDirection): Vector[Square] =
    val builder = Vector.newBuilder[Square]
    var next = direction.step(source)
    var blocked = false
    while next.nonEmpty && !blocked do
      val square = next.get
      builder += square
      blocked = pieceMap.contains(square)
      next = if blocked then None else direction.step(square)
    builder.result()

  def clearRay(source: Square, direction: WitnessDirection): Vector[Square] =
    rayUntilBlocked(source, direction).takeWhile(square => pieceAt(square).isEmpty)

  def firstBlocker(source: Square, direction: WitnessDirection): Option[Square] =
    rayUntilBlocked(source, direction).find(square => pieceAt(square).nonEmpty)

  def raySquaresBetween(source: Square, target: Square): Vector[Square] =
    WitnessDirection
      .between(source, target)
      .map: direction =>
        val builder = Vector.newBuilder[Square]
        var next = direction.step(source)
        while next.nonEmpty && next.get != target do
          builder += next.get
          next = direction.step(next.get)
        builder.result()
      .getOrElse(Vector.empty)

  def occupiedBetween(source: Square, target: Square): Vector[Square] =
    raySquaresBetween(source, target).filter(square => pieceAt(square).nonEmpty)

  def singleBlockerBetween(source: Square, target: Square): Option[Square] =
    occupiedBetween(source, target) match
      case Vector(blocker) => Some(blocker)
      case _ => None

  def attacksFrom(square: Square): Bitboard =
    pieceAt(square).fold(Bitboard.empty)(piece => attackMaskFrom(square, piece))

  def attacksSquare(origin: Square, target: Square): Boolean =
    attacksFrom(origin).contains(target)

  def attackersOf(target: Square, color: Color): Vector[Square] =
    squaresOf(color).filter(origin => attacksSquare(origin, target))

  def attackCountOn(target: Square, color: Color): Int =
    attackersOf(target, color).size

  def sliderDirections(role: Role): Vector[WitnessDirection] =
    role match
      case Bishop => WitnessDirection.diagonal
      case Rook => WitnessDirection.orthogonal
      case Queen => WitnessDirection.slider
      case _ => Vector.empty

  def testableDirectionsFrom(source: Square, role: Role): Vector[WitnessDirection] =
    sliderDirections(role).filter(_.step(source).nonEmpty)

  def openTestableDirectionsFrom(source: Square, role: Role): Vector[WitnessDirection] =
    testableDirectionsFrom(source, role).filter: direction =>
      direction.step(source).forall(square => pieceAt(square).isEmpty)

  def rayMobilityByDirection(source: Square, role: Role): Map[WitnessDirection, Int] =
    testableDirectionsFrom(source, role).map: direction =>
      direction -> clearRay(source, direction).size
    .toMap

  def pieceValue(role: Role): Int =
    role match
      case Pawn => 1
      case Knight | Bishop => 3
      case Rook => 5
      case Queen => 9
      case King => 100

  private def attackMaskFrom(square: Square, piece: Piece): Bitboard =
    piece.role match
      case Pawn => square.pawnAttacks(piece.color)
      case Knight => square.knightAttacks
      case Bishop => square.bishopAttacks(occupied)
      case Rook => square.rookAttacks(occupied)
      case Queen => square.queenAttacks(occupied)
      case King => square.kingAttacks

private[commentary] object WitnessBoardView:

  def fromRoot(rootState: RootStateVector): WitnessBoardView =
    val placements =
      for
        color <- canonicalColors
        role <- canonicalRoles
        square <- canonicalSquares
        if rootState.contains(pieceOnIndex(color, role, square))
      yield square -> Piece(color, role)

    val duplicateSquares =
      placements.groupBy(_._1).collect { case (square, values) if values.size > 1 => square.key }.toVector.sorted

    require(
      duplicateSquares.isEmpty,
      s"Duplicate piece_on roots found for squares: ${duplicateSquares.mkString(", ")}"
    )

    WitnessBoardView(placements.toMap)
