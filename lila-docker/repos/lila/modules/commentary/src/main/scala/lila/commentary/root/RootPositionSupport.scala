package lila.commentary.root

import chess.format.Uci
import chess.variant
import chess.{ Board, Castles, Color, File, History, Piece, Position, Rank, Role, Square, UnmovedRooks }

private[commentary] object RootPositionSupport:

  import RootAtomRegistry.*

  def exactPosition(rootState: RootStateVector): Either[String, Position] =
    sideToMove(rootState).flatMap(positionFor(rootState, _))

  def positionFor(
      rootState: RootStateVector,
      color: Color
  ): Either[String, Position] =
    for
      board <- boardFromRoot(rootState)
      rootColor <- sideToMove(rootState)
      rightsMask <- castlingRightsMask(rootState)
      lastMove <- enPassantLastMove(rootState)
      _ <- validateAuxStateBoardCoherence(board, rootColor, rightsMask, lastMove)
    yield Position(
      board = board,
      history = History(
        lastMove = lastMove,
        castles = castlesFromMask(rightsMask),
        unmovedRooks = unmovedRooksFromMask(rightsMask),
        crazyData = None
      ),
      variant = variant.Standard,
      color = color
    )

  def sideToMove(rootState: RootStateVector): Either[String, Color] =
    singleActiveIndex(rootState, SchemaId.SideToMove).map: index =>
      canonicalColors(index - requireSchema(SchemaId.SideToMove).start)

  private def castlingRightsMask(rootState: RootStateVector): Either[String, Int] =
    singleActiveIndex(rootState, SchemaId.CastlingRights).map: index =>
      index - requireSchema(SchemaId.CastlingRights).start

  private def enPassantLastMove(
      rootState: RootStateVector
  ): Either[String, Option[Uci.Move]] =
    singleActiveIndex(rootState, SchemaId.EnPassantState).map: index =>
      val schema = requireSchema(SchemaId.EnPassantState)
      val local = index - schema.start
      if local == 0 then None
      else
        val capturerColor = canonicalColors((local - 1) / 8)
        val file = canonicalFiles((local - 1) % 8)
        val originRank = if capturerColor.white then Rank.Seventh else Rank.Second
        val destinationRank = if capturerColor.white then Rank.Fifth else Rank.Fourth
        Some(Uci.Move(Square(file, originRank), Square(file, destinationRank), None))

  private def boardFromRoot(rootState: RootStateVector): Either[String, Board] =
    val placements =
      for
        color <- canonicalColors
        role <- canonicalRoles
        square <- canonicalSquares
        if rootState.contains(pieceOnIndex(color, role, square))
      yield square -> Piece(color, role)

    val duplicateSquares =
      placements
        .groupBy(_._1)
        .collect { case (square, values) if values.size > 1 => square.key }
        .toVector
        .sorted

    Either.cond(
      duplicateSquares.isEmpty,
      Board.fromMap(placements.toMap),
      s"Duplicate piece_on roots found for squares: ${duplicateSquares.mkString(", ")}"
    )

  private def validateAuxStateBoardCoherence(
      board: Board,
      colorToMove: Color,
      rightsMask: Int,
      lastMove: Option[Uci.Move]
  ): Either[String, Unit] =
    for
      _ <- validateCastlingRightsBoardCoherence(board, rightsMask)
      _ <- validateEnPassantBoardCoherence(board, colorToMove, lastMove)
    yield ()

  private def castlesFromMask(mask: Int): Castles =
    Castles(
      whiteKingSide = (mask & 1) != 0,
      whiteQueenSide = (mask & 2) != 0,
      blackKingSide = (mask & 4) != 0,
      blackQueenSide = (mask & 8) != 0
    )

  private def unmovedRooksFromMask(mask: Int): UnmovedRooks =
    UnmovedRooks(
      Vector(
        Option.when((mask & 1) != 0)(Square.H1),
        Option.when((mask & 2) != 0)(Square.A1),
        Option.when((mask & 4) != 0)(Square.H8),
        Option.when((mask & 8) != 0)(Square.A8)
      ).flatten
    )

  private def singleActiveIndex(
      rootState: RootStateVector,
      schemaId: String
  ): Either[String, Int] =
    val active = rootState.activeIndicesForSchema(schemaId)
    Either.cond(
      active.size == 1,
      active.headOption.getOrElse(-1),
      s"Exact root state requires exactly one active $schemaId bit, found ${active.mkString(", ")}"
    )

  private def validateCastlingRightsBoardCoherence(
      board: Board,
      rightsMask: Int
  ): Either[String, Unit] =
    Vector(
      Option.when((rightsMask & 1) != 0)('K'),
      Option.when((rightsMask & 2) != 0)('Q'),
      Option.when((rightsMask & 4) != 0)('k'),
      Option.when((rightsMask & 8) != 0)('q')
    ).flatten
      .foldLeft[Either[String, Unit]](Right(())):
        case (acc, right) =>
          acc.flatMap: _ =>
            Either.cond(
              castlingRightMatchesBoard(board, right),
              (),
              s"Illegal castling-rights state ($right)"
            )

  private def validateEnPassantBoardCoherence(
      board: Board,
      colorToMove: Color,
      lastMove: Option[Uci.Move]
  ): Either[String, Unit] =
    lastMove match
      case None => Right(())
      case Some(move) =>
        val targetRank = if colorToMove.white then Rank.Sixth else Rank.Third
        val destinationRank = if colorToMove.white then Rank.Fifth else Rank.Fourth
        val targetSquare = Square(move.dest.file, targetRank)
        for
          _ <- Either.cond(
            move.orig.rank == (if colorToMove.white then Rank.Seventh else Rank.Second) &&
              move.dest.rank == destinationRank,
            (),
            "Illegal en-passant history state"
          )
          _ <- Either.cond(
            board.pieceAt(targetSquare).isEmpty,
            (),
            "Illegal en-passant target occupancy"
          )
          _ <- Either.cond(
            board.pieceAt(move.orig).isEmpty,
            (),
            "Illegal en-passant origin occupancy"
          )
          _ <- Either.cond(
            board.pieceAt(move.dest).exists(piece => piece.color == !colorToMove && piece.role == chess.Pawn),
            (),
            "Illegal en-passant history state"
          )
        yield ()

  private def castlingRightMatchesBoard(board: Board, right: Char): Boolean =
    right match
      case 'K' =>
        boardHas(board, Color.White, chess.King, Square(File.E, Rank.First)) &&
          boardHas(board, Color.White, chess.Rook, Square.H1)
      case 'Q' =>
        boardHas(board, Color.White, chess.King, Square(File.E, Rank.First)) &&
          boardHas(board, Color.White, chess.Rook, Square.A1)
      case 'k' =>
        boardHas(board, Color.Black, chess.King, Square(File.E, Rank.Eighth)) &&
          boardHas(board, Color.Black, chess.Rook, Square.H8)
      case 'q' =>
        boardHas(board, Color.Black, chess.King, Square(File.E, Rank.Eighth)) &&
          boardHas(board, Color.Black, chess.Rook, Square.A8)
      case _ => false

  private def boardHas(board: Board, color: Color, role: Role, square: Square): Boolean =
    board.pieceAt(square).contains(Piece(color, role))
