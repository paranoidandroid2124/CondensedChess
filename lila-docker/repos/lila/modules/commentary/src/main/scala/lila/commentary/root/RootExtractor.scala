package lila.commentary.root

import chess.format.Fen
import chess.variant
import chess.{ Bishop, Bitboard, Board, ByColor, Color, File, King, Knight, Pawn, Position, Queen, Rank, Role, Rook, Square }

object RootExtractor:

  import RootAtomRegistry.*

  private[commentary] final case class RootWithPosition(position: Position, root: RootStateVector)

  val implementedSchemas: Set[String] = all.map(_.id).toSet

  def fromFen(fen: Fen.Full): Either[String, RootStateVector] =
    readPosition(fen).map(position => RootAnalyzer(position).extract())

  // Synthetic root corpora may use exact-board positions that are not legal
  // reachable game states. External FEN input should go through the strict
  // fail-closed boundary instead.
  def fromFenFailClosed(fen: Fen.Full): Either[String, RootStateVector] =
    fromFenWithPositionFailClosed(fen).map(_.root)

  private[commentary] def fromFenWithPositionFailClosed(fen: Fen.Full): Either[String, RootWithPosition] =
    for
      position <- readPosition(fen)
      _ <- validateStrictPosition(position, fen.value)
      _ <- validateFenFields(fen, position)
    yield RootWithPosition(position, RootAnalyzer(position).extract())

  private[commentary] def fromPositionFailClosed(position: Position): Either[String, RootWithPosition] =
    validateStrictPosition(position, Fen.write(position).value)
      .map(_ => RootWithPosition(position, RootAnalyzer(position).extract()))

  private def readPosition(fen: Fen.Full): Either[String, Position] =
    Fen
      .read(variant.Standard, fen)
      .toRight(s"Invalid standard FEN: $fen")

  private def validateStrictPosition(position: Position, label: String): Either[String, Unit] =
    for
      _ <- Either.cond(
        variant.Standard.valid(position, strict = true),
        (),
        s"Illegal standard position shape: $label"
      )
      _ <- Either.cond(
        kingsAreSeparated(position),
        (),
        s"Illegal standard position (adjacent kings): $label"
      )
      _ <- Either.cond(
        sideThatJustMovedIsSafe(position),
        (),
        s"Illegal standard position (side that just moved remains in check): $label"
      )
    yield ()

  private def validateFenFields(fen: Fen.Full, position: Position): Either[String, Unit] =
    for
      _ <- validateCastlingField(fen, position)
      _ <- validateEnPassantField(fen, position)
    yield ()

  private def kingsAreSeparated(position: Position): Boolean =
    (for
      whiteKing <- position.board.kingPosOf(Color.White)
      blackKing <- position.board.kingPosOf(Color.Black)
    yield !whiteKing.kingAttacks.contains(blackKing)).getOrElse(false)

  private def sideThatJustMovedIsSafe(position: Position): Boolean =
    position.withColor(!position.color).check.no

  private def validateCastlingField(fen: Fen.Full, position: Position): Either[String, Unit] =
    rawCastlingField(fen) match
      case "-" => Right(())
      case raw =>
        val rights = raw.toVector
        for
          _ <- Either.cond(
            rights.distinct.size == rights.size && rights.forall(ch => "KQkq".contains(ch)),
            (),
            s"Invalid castling-rights field: $fen"
          )
          _ <- rights.foldLeft[Either[String, Unit]](Right(())):
            case (acc, right) =>
              acc.flatMap: _ =>
                Either.cond(
                  castlingRightMatchesBoard(position.board, right),
                  (),
                  s"Illegal castling-rights state ($right): $fen"
                )
        yield ()

  private def castlingRightMatchesBoard(board: Board, right: Char): Boolean =
    right match
      case 'K' => boardHas(board, Color.White, King, Square(File.E, Rank.First)) && boardHas(board, Color.White, Rook, Square(File.H, Rank.First))
      case 'Q' => boardHas(board, Color.White, King, Square(File.E, Rank.First)) && boardHas(board, Color.White, Rook, Square(File.A, Rank.First))
      case 'k' => boardHas(board, Color.Black, King, Square(File.E, Rank.Eighth)) && boardHas(board, Color.Black, Rook, Square(File.H, Rank.Eighth))
      case 'q' => boardHas(board, Color.Black, King, Square(File.E, Rank.Eighth)) && boardHas(board, Color.Black, Rook, Square(File.A, Rank.Eighth))
      case _ => false

  private def validateEnPassantField(fen: Fen.Full, position: Position): Either[String, Unit] =
    rawEnPassantField(fen) match
      case "-" => Right(())
      case raw =>
        Square
          .fromKey(raw)
          .toRight(s"Invalid en-passant square: $fen")
          .flatMap: square =>
            val sixthRank = position.color.fold(Rank.Sixth, Rank.Third)
            val seventhRank = position.color.fold(Rank.Seventh, Rank.Second)
            val fifthRank = position.color.fold(Rank.Fifth, Rank.Fourth)
            val origin = Square(square.file, seventhRank)
            val destination = Square(square.file, fifthRank)
            for
              _ <- Either.cond(
                square.rank == sixthRank,
                (),
                s"Illegal en-passant rank: $fen"
              )
              _ <- Either.cond(
                position.pieceAt(square).isEmpty,
                (),
                s"Illegal en-passant target occupancy: $fen"
              )
              _ <- Either.cond(
                position.pieceAt(origin).isEmpty,
                (),
                s"Illegal en-passant origin occupancy: $fen"
              )
              _ <- Either.cond(
                position.pieceAt(destination).exists(piece => piece.color == !position.color && piece.role == Pawn),
                (),
                s"Illegal en-passant history state: $fen"
              )
            yield ()

  private def boardHas(board: Board, color: Color, role: Role, square: Square): Boolean =
    board.pieceAt(square).exists(piece => piece.color == color && piece.role == role)

  private def rawCastlingField(fen: Fen.Full): String =
    fen.value.split(' ').lift(2).getOrElse("-")

  private def rawEnPassantField(fen: Fen.Full): String =
    fen.value.split(' ').lift(3).getOrElse("-")

  private final class RootAnalyzer(position: Position):

    private val board = position.board
    private val pieceMap = board.pieceMap
    private val pawnsByColor = ByColor(color => board.byPiece(color, Pawn))
    private val controlledByMask = ByColor(color => unionAttacks(color))
    private val pawnControlledByMask = ByColor(color => unionAttacks(color, roles = Set(Pawn)))
    private val pieceControlledByMask = ByColor(color => unionAttacks(color, roles = Set(Knight, Bishop, Rook, Queen)))
    private val contestedMask = controlledByMask(Color.White) & controlledByMask(Color.Black)
    private val openFileMask = canonicalFiles.foldLeft(0): (mask, file) =>
      if (board.pawns & Bitboard.file(file)).isEmpty then mask | (1 << file.value) else mask
    private val halfOpenFileMask = ByColor(color => canonicalFiles.foldLeft(0): (mask, file) =>
      if isHalfOpenFile(color, file) then mask | (1 << file.value) else mask
    )
    private val kingRingMask = ByColor(color => kingRingSquares(color))
    private val weakSquareMask = ByColor(color => bitboardFromSquares(canonicalSquares.filter(isWeakSquare(color, _))))
    private val outpostSquareMask = ByColor(color => bitboardFromSquares(canonicalSquares.filter(isOutpostSquare(color, _))))
    private val isolatedPawnMask = ByColor(color => bitboardFromSquares(pawnSquaresOf(color).filter(isIsolatedPawn(color, _))))
    private val backwardPawnMask = ByColor(color => bitboardFromSquares(pawnSquaresOf(color).filter(isBackwardPawn(color, _))))
    private val doubledFileMask = ByColor(color => canonicalFiles.foldLeft(0): (mask, file) =>
      if isDoubledFile(color, file) then mask | (1 << file.value) else mask
    )
    private val passedPawnMask = ByColor(color => bitboardFromSquares(pawnSquaresOf(color).filter(isPassedPawn(color, _))))
    private val candidatePasserMask = ByColor(color => bitboardFromSquares(pawnSquaresOf(color).filter(isCandidatePasser(color, _))))
    private val fixedPawnMask = ByColor(color => bitboardFromSquares(pawnSquaresOf(color).filter(isFixedPawn(color, _))))
    private val loosePieceMask = ByColor(color => bitboardFromSquares(pieceSquaresOf(color).filter(isLoosePiece(color, _))))
    private val pinnedPieceMask = ByColor(color => bitboardFromSquares(pieceSquaresOf(color).filter(isPinnedPiece(color, _))))
    private val overloadedPieceMask = ByColor(color => bitboardFromSquares(pieceSquaresOf(color).filter(isOverloadedPiece(color, _))))
    private val trappedPieceMask = ByColor(color => bitboardFromSquares(pieceSquaresOf(color).filter(isTrappedPiece(color, _))))
    private val xrayTargetMask = ByColor(color => bitboardFromSquares(canonicalSquares.filter(isXrayTarget(color, _))))
    private val leverAvailableMask = ByColor(color => bitboardFromSquares(pawnSquaresOf(color).filter(hasImmediatePawnLever(color, _))))
    private val kingShelterHoleMask = ByColor(color => bitboardFromSquares(canonicalSquares.filter(isKingShelterHole(color, _))))

    def extract(): RootStateVector =
      val builder = RootStateVector.builder

      extractPieceOn(builder)

      setColorSquareMask(SchemaId.ControlledBy, controlledByMask, builder)
      setColorSquareMask(SchemaId.PawnControlledBy, pawnControlledByMask, builder)
      setNeutralSquareMask(SchemaId.Contested, contestedMask, builder)
      setNeutralFileMask(SchemaId.OpenFile, openFileMask, builder)
      setColorFileMask(SchemaId.HalfOpenFile, halfOpenFileMask, builder)
      setColorSquareMask(SchemaId.KingRingSquare, kingRingMask, builder)

      setColorSquareMask(SchemaId.WeakSquare, weakSquareMask, builder)
      setColorSquareMask(SchemaId.OutpostSquare, outpostSquareMask, builder)
      setColorPawnSquareMask(SchemaId.IsolatedPawn, isolatedPawnMask, builder)
      setColorPawnSquareMask(SchemaId.BackwardPawn, backwardPawnMask, builder)
      setColorFileMask(SchemaId.DoubledFile, doubledFileMask, builder)
      setColorPawnSquareMask(SchemaId.PassedPawn, passedPawnMask, builder)
      setColorPawnSquareMask(SchemaId.CandidatePasser, candidatePasserMask, builder)
      setColorPawnSquareMask(SchemaId.FixedPawn, fixedPawnMask, builder)

      setColorSquareMask(SchemaId.LoosePiece, loosePieceMask, builder)
      setColorSquareMask(SchemaId.PinnedPiece, pinnedPieceMask, builder)
      setColorSquareMask(SchemaId.OverloadedPiece, overloadedPieceMask, builder)
      setColorSquareMask(SchemaId.TrappedPiece, trappedPieceMask, builder)
      setColorSquareMask(SchemaId.XrayTarget, xrayTargetMask, builder)
      setColorPawnSquareMask(SchemaId.LeverAvailable, leverAvailableMask, builder)
      setColorSquareMask(SchemaId.KingShelterHole, kingShelterHoleMask, builder)

      extractAuxState(builder)

      builder.result()

    private def extractPieceOn(builder: RootStateVector.Builder): Unit =
      position.foreach: (color, role, square) =>
        builder.set(pieceOnIndex(color, role, square))

    private def setColorSquareMask(
        schemaId: String,
        maskByColor: ByColor[Bitboard],
        builder: RootStateVector.Builder
    ): Unit =
      canonicalColors.foreach: color =>
        maskByColor(color).foreach: square =>
          builder.set(colorSquareIndex(schemaId, color, square))

    private def setColorPawnSquareMask(
        schemaId: String,
        maskByColor: ByColor[Bitboard],
        builder: RootStateVector.Builder
    ): Unit =
      canonicalColors.foreach: color =>
        maskByColor(color).foreach: square =>
          colorPawnSquareIndex(schemaId, color, square).foreach(builder.set)

    private def setNeutralSquareMask(schemaId: String, mask: Bitboard, builder: RootStateVector.Builder): Unit =
      mask.foreach(square => builder.set(neutralSquareIndex(schemaId, square)))

    private def setColorFileMask(
        schemaId: String,
        maskByColor: ByColor[Int],
        builder: RootStateVector.Builder
    ): Unit =
      canonicalColors.foreach: color =>
        canonicalFiles.foreach: file =>
          if (maskByColor(color) & (1 << file.value)) != 0 then
            builder.set(colorFileIndex(schemaId, color, file))

    private def setNeutralFileMask(schemaId: String, mask8: Int, builder: RootStateVector.Builder): Unit =
      canonicalFiles.foreach: file =>
        if (mask8 & (1 << file.value)) != 0 then
          builder.set(neutralFileIndex(schemaId, file))

    private def extractAuxState(builder: RootStateVector.Builder): Unit =
      builder.set(sideToMoveIndex(position.color))
      builder.set(castlingRightsIndex(castlingRightsMask))
      builder.set(enPassantStateIndex)

    private def castlingRightsMask: Int =
      val castles = position.history.castles
      (if castles.whiteKingSide then 1 else 0) |
        (if castles.whiteQueenSide then 2 else 0) |
        (if castles.blackKingSide then 4 else 0) |
        (if castles.blackQueenSide then 8 else 0)

    private def enPassantStateIndex: Int =
      position.enPassantSquare match
        case Some(square) => enPassantIndex(position.color, square.file)
        case None => enPassantNoneIndex

    private def pawnSquaresOf(color: Color): Vector[Square] =
      pawnsByColor(color).squares.toVector

    private def pieceSquaresOf(color: Color): Vector[Square] =
      board.byColor(color).squares.toVector

    private def bitboardFromSquares(squares: Iterable[Square]): Bitboard =
      Bitboard(squares.toList)

    private def unionAttacks(color: Color, roles: Set[Role] = Set(Pawn, Knight, Bishop, Rook, Queen, King)): Bitboard =
      pieceMap.foldLeft(Bitboard.empty):
        case (mask, (square, piece)) if piece.color == color && roles.contains(piece.role) =>
          mask | attackMaskFrom(square, piece)
        case (mask, _) => mask

    private def attackMaskFrom(square: Square, piece: chess.Piece): Bitboard =
      piece.role match
        case Pawn => square.pawnAttacks(piece.color)
        case Knight => square.knightAttacks
        case Bishop => square.bishopAttacks(board.occupied)
        case Rook => square.rookAttacks(board.occupied)
        case Queen => square.queenAttacks(board.occupied)
        case King => square.kingAttacks

    private def kingRingSquares(color: Color): Bitboard =
      board.kingPosOf(color).map(_.kingAttacks).getOrElse(Bitboard.empty)

    private def isHalfOpenFile(color: Color, file: File): Boolean =
      (pawnsByColor(color) & Bitboard.file(file)).isEmpty &&
        (pawnsByColor(!color) & Bitboard.file(file)).nonEmpty

    private def isDoubledFile(color: Color, file: File): Boolean =
      (pawnsByColor(color) & Bitboard.file(file)).count >= 2

    private def isIsolatedPawn(color: Color, square: Square): Boolean =
      pieceIs(square, color, Pawn) &&
        adjacentFiles(square.file).forall(file => (pawnsByColor(color) & Bitboard.file(file)).isEmpty)

    private def isPassedPawn(color: Color, square: Square): Boolean =
      pieceIs(square, color, Pawn) &&
        forwardConePawnsRelativeTo(color, !color, square, includeOwnFile = true).isEmpty

    private def isCandidatePasser(color: Color, square: Square): Boolean =
      pieceIs(square, color, Pawn) &&
        square.nextRank(color).exists(front => board.pieceAt(front).isEmpty) &&
        !isPassedPawn(color, square) &&
        candidatePasserSupportPawns(color, square).size >=
          forwardConePawnsRelativeTo(color, !color, square, includeOwnFile = true).size

    private def isFixedPawn(color: Color, square: Square): Boolean =
      pieceIs(square, color, Pawn) &&
        square.nextRank(color).exists(stop => pieceIs(stop, !color, Pawn))

    private def isBackwardPawn(color: Color, square: Square): Boolean =
      pieceIs(square, color, Pawn) &&
        hasAdjacentFriendlyPawn(color, square) &&
        square.nextRank(color).exists: front =>
          !hasStructuralPawnSupport(color, front) &&
            frontDeniedForBackwardPawn(color, front) &&
            isHalfOpenFile(!color, square.file)

    private def isWeakSquare(color: Color, square: Square): Boolean =
      isInEnemyTerritory(color, square) &&
        position.pieceAt(square).forall(piece => piece.color == color && piece.role != Pawn && piece.role != King) &&
        !enemyPawnCanChallengeSquare(color, square) &&
        canExploitWithPieces(color, square)

    private def isOutpostSquare(color: Color, square: Square): Boolean =
      isInEnemyTerritory(color, square) &&
        isWeakSquare(color, square) &&
        hasStructuralPawnSupport(color, square)

    private def isLoosePiece(color: Color, square: Square): Boolean =
      position.pieceAt(square).exists: piece =>
        piece.color == color && piece.role != King &&
          opponentBestExchangeNet(board, square, !color) > 0

    private def isPinnedPiece(color: Color, square: Square): Boolean =
      position.pieceAt(square).exists: piece =>
        piece.color == color && piece.role != King &&
          (isAbsolutelyPinned(color, square) || isRelativelyPinned(color, square, piece))

    private def isOverloadedPiece(color: Color, square: Square): Boolean =
      position.pieceAt(square).exists: piece =>
        piece.color == color && piece.role != King && countCriticalDefensiveTargets(color, square, piece) >= 2

    private def isTrappedPiece(color: Color, square: Square): Boolean =
      position.pieceAt(square).exists: piece =>
        if piece.color != color || piece.role == King || piece.role == Pawn || board.attackers(square, !color).isEmpty then false
        else
          val legalMoves = position.withColor(color).generateMovesAt(square)
          legalMoves.forall(move => opponentBestExchangeNet(move.after.board, move.dest, !color) > 0)

    private def isXrayTarget(color: Color, square: Square): Boolean =
      position.pieceAt(square).exists: target =>
        target.color == !color && highValueXrayTarget(target.role) &&
          sliderSquaresOf(color).exists: sliderSquare =>
            position.pieceAt(sliderSquare).exists: slider =>
              slider.color == color &&
                sliderCanUseLine(slider.role, sliderSquare, square) &&
                (Bitboard.between(sliderSquare, square) & board.occupied).count == 1

    private def highValueXrayTarget(role: Role): Boolean =
      role == Rook || role == Queen

    private def isKingShelterHole(beneficiary: Color, square: Square): Boolean =
      val defender = !beneficiary
      homeShelterMask(defender).contains(square) &&
        !pieceIs(square, defender, Pawn) &&
        !pawnControlledByMask(defender).contains(square) &&
        hasPieceAttackOrAccess(beneficiary, square)

    private def hasImmediatePawnLever(color: Color, square: Square): Boolean =
      position.withColor(color).generateMovesAt(square).exists: move =>
        isForwardPawnPush(move.orig, move.dest, color, move.promotion.isEmpty, !move.captures) &&
          arrivalAttacksEnemyPawn(color, move.dest)

    private def isForwardPawnPush(
        origin: Square,
        destination: Square,
        color: Color,
        noPromotion: Boolean,
        notCapture: Boolean
    ): Boolean =
      val delta = color.fold(destination.value - origin.value, origin.value - destination.value)
      noPromotion && notCapture && origin.file == destination.file && (delta == 8 || delta == 16)

    private def arrivalAttacksEnemyPawn(color: Color, arrival: Square): Boolean =
      arrival.pawnAttacks(color).exists(target => pieceIs(target, !color, Pawn))

    private def canExploitWithPieces(color: Color, square: Square): Boolean =
      position.pieceAt(square).exists(piece => piece.color == color && piece.role != Pawn && piece.role != King) ||
        (board.pieceAt(square).isEmpty && pieceControlledByMask(color).contains(square))

    private def hasPieceAttackOrAccess(color: Color, square: Square): Boolean =
      position.pieceAt(square).exists(piece => piece.color == color && piece.role != Pawn && piece.role != King) ||
        pieceControlledByMask(color).contains(square)

    private def isInEnemyTerritory(color: Color, square: Square): Boolean =
      color.fold(square.rank >= Rank.Fifth, square.rank <= Rank.Fourth)

    private def hasStructuralPawnSupport(color: Color, target: Square): Boolean =
      pawnAttackSources(target, color).exists: supportSquare =>
        pawnsOnFile(color, supportSquare.file).exists(origin => canPawnStructurallyReach(origin, supportSquare, color))

    private def hasAdjacentFriendlyPawn(color: Color, square: Square): Boolean =
      adjacentFiles(square.file).exists(file => (pawnsByColor(color) & Bitboard.file(file)).nonEmpty)

    private def candidatePasserSupportPawns(color: Color, square: Square): Vector[Square] =
      val adjacent = adjacentFiles(square.file)
      val relevantFiles = adjacent + square.file
      pawnsByColor(color).filter: pawn =>
        pawn != square &&
          relevantFiles.contains(pawn.file) &&
          (
            isAheadOf(color, pawn, square) ||
              (adjacent.contains(pawn.file) && pawn.rank == square.rank)
          )
      .toVector

    private def frontDeniedForBackwardPawn(color: Color, front: Square): Boolean =
      board.pieceAt(front) match
        case Some(piece) => piece.color == !color
        case None =>
          pawnControlledByMask(!color).contains(front) &&
            !pawnControlledByMask(color).contains(front)

    private def enemyPawnCanChallengeSquare(beneficiary: Color, target: Square): Boolean =
      val enemy = !beneficiary
      pawnAttackSources(target, enemy).exists: source =>
        pawnsOnFile(enemy, source.file).exists(origin => canPawnStructurallyReach(origin, source, enemy))

    private def pawnAttackSources(target: Square, attackerColor: Color): Vector[Square] =
      target.pawnAttacks(!attackerColor).squares.toVector

    private def pawnsOnFile(color: Color, file: File): Vector[Square] =
      pawnsByColor(color).filter(_.file == file).toVector

    private def canPawnStructurallyReach(origin: Square, target: Square, color: Color): Boolean =
      if origin.file != target.file then false
      else
        val distance = forwardDistance(color, origin, target)
        if distance < 0 then false
        else if distance == 0 then pieceIs(origin, color, Pawn)
        else
          board.pieceAt(target).isEmpty &&
            squaresBetweenOnFile(origin, target).forall(square => board.pieceAt(square).isEmpty)

    private def forwardDistance(color: Color, from: Square, to: Square): Int =
      color.fold(to.rank.value - from.rank.value, from.rank.value - to.rank.value)

    private def squaresBetweenOnFile(from: Square, to: Square): Vector[Square] =
      if from.file != to.file then Vector.empty
      else
        val step = if to.rank.value > from.rank.value then 1 else -1
        ((from.rank.value + step) to to.rank.value by step)
          .map(rankValue => Square(from.file, Rank(rankValue).get))
          .toVector

    private def forwardConePawnsRelativeTo(
        forwardColor: Color,
        pawnsColor: Color,
        square: Square,
        includeOwnFile: Boolean
    ): Vector[Square] =
      val files = if includeOwnFile then adjacentAndOwnFiles(square.file) else adjacentFiles(square.file)
      pawnsByColor(pawnsColor).filter(pawn => files.contains(pawn.file) && isAheadOf(forwardColor, pawn, square)).toVector

    private def adjacentFiles(file: File): Set[File] =
      Set(file.value - 1, file.value + 1).flatMap(File(_))

    private def adjacentAndOwnFiles(file: File): Set[File] =
      adjacentFiles(file) + file

    private def isAheadOf(color: Color, candidate: Square, reference: Square): Boolean =
      color.fold(candidate.rank > reference.rank, candidate.rank < reference.rank)

    private def pieceIs(square: Square, color: Color, role: Role): Boolean =
      board.pieceAt(square).exists(piece => piece.color == color && piece.role == role)

    private def sliderSquaresOf(color: Color): Vector[Square] =
      pieceMap.collect:
        case (square, piece) if piece.color == color && Set(Bishop, Rook, Queen).contains(piece.role) => square
      .toVector

    private def sliderCanUseLine(role: Role, from: Square, to: Square): Boolean =
      role match
        case Bishop => from.onSameDiagonal(to)
        case Rook => from.onSameLine(to)
        case Queen => from.onSameDiagonal(to) || from.onSameLine(to)
        case _ => false

    private def isAbsolutelyPinned(color: Color, square: Square): Boolean =
      board.kingPosOf(color).exists(king => board.sliderBlockers(king, color).contains(square))

    private def isRelativelyPinned(color: Color, square: Square, piece: chess.Piece): Boolean =
      sliderSquaresOf(!color).exists: sniperSquare =>
        position.pieceAt(sniperSquare).exists: sniper =>
          sliderCanUseLine(sniper.role, sniperSquare, square) &&
            moreValuableFriendlyAnchors(color, square, piece).exists: anchor =>
              sliderCanUseLine(sniper.role, sniperSquare, anchor) &&
                hasSingleBlockerBetween(anchor, sniperSquare, square)

    private def moreValuableFriendlyAnchors(color: Color, square: Square, piece: chess.Piece): Vector[Square] =
      pieceMap.collect:
        case (anchor, anchorPiece)
            if anchor != square &&
              anchorPiece.color == color &&
              anchorPiece.role != King &&
              pieceValue(anchorPiece.role) > pieceValue(piece.role) =>
          anchor
      .toVector

    private def hasSingleBlockerBetween(anchor: Square, sniper: Square, blocker: Square): Boolean =
      val blockers = Bitboard.between(anchor, sniper) & board.occupied
      blockers.count == 1 && blockers.contains(blocker)

    private def homeShelterMask(color: Color): Bitboard =
      board.kingPosOf(color).fold(Bitboard.empty): king =>
        val inHomeShelterRegime = color.fold(king.rank <= Rank.Second, king.rank >= Rank.Seventh)
        if !inHomeShelterRegime then Bitboard.empty
        else
          Bitboard(
            for
              rankStep <- List(1, 2)
              fileOffset <- List(-1, 0, 1)
              square <- Square.at(
                king.file.value + fileOffset,
                king.rank.value + color.fold(rankStep, -rankStep)
              )
            yield square
          )

    private def countCriticalDefensiveTargets(color: Color, square: Square, piece: chess.Piece): Int =
      val boardWithoutDefender = board.discard(square)
      attackMaskFrom(square, piece)
        .filter: target =>
          board.pieceAt(target).exists(targetPiece => targetPiece.color == color && targetPiece.role != King) &&
            boardWithoutDefender.attackers(target, !color).nonEmpty &&
            boardWithoutDefender.attackers(target, color).count < boardWithoutDefender.attackers(target, !color).count
        .size

    private def opponentBestExchangeNet(boardState: Board, square: Square, attacker: Color): Int =
      boardState.pieceAt(square).fold(0): occupant =>
        legalAttackers(boardState, square, attacker)
          .map: origin =>
            val afterCapture = boardState.taking(origin, square).get
            pieceValue(occupant.role) - opponentBestExchangeNet(afterCapture, square, !attacker)
          .foldLeft(0)(math.max)

    private def legalAttackers(boardState: Board, square: Square, attacker: Color): Vector[Square] =
      val attackerPosition = Position(boardState, variant.Standard, attacker)
      boardState
        .attackers(square, attacker)
        .filter(origin => attackerPosition.generateMovesAt(origin).exists(_.dest == square))
        .toVector

    private def pieceValue(role: Role): Int = role match
      case Pawn => 100
      case Knight | Bishop => 300
      case Rook => 500
      case Queen => 900
      case King => 10_000
