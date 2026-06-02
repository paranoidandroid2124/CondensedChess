package lila.commentary.analysis

import chess.*

object PlanMoveEvidenceSupport:

  def orientedPosition(pos: Position, color: Color): Position =
    if pos.color == color then pos else pos.withColor(color)

  def advancesTowardPromotion(mv: Move, color: Color): Boolean =
    if color.white then mv.dest.rank.value > mv.orig.rank.value
    else mv.dest.rank.value < mv.orig.rank.value

  def promotionDistance(square: Square, color: Color): Int =
    if color.white then Rank.Eighth.value - square.rank.value
    else square.rank.value - Rank.First.value

  def passedPawns(board: Board, color: Color): List[Square] =
    board.byPiece(color, Pawn).squares.filter(isPassedPawn(board, _, color)).toList

  def isPassedPawn(board: Board, pawnSq: Square, color: Color): Boolean =
    board.pieceAt(pawnSq).exists(piece => piece.color == color && piece.role == Pawn) && {
      val enemyPawns = board.pawns & board.byColor(!color)
      !enemyPawns.squares.exists { opp =>
        val fileOk = (opp.file.value - pawnSq.file.value).abs <= 1
        val ahead = if color.white then opp.rank.value > pawnSq.rank.value else opp.rank.value < pawnSq.rank.value
        fileOk && ahead
      }
    }

  def isProtectedByPawn(board: Board, pawnSq: Square, color: Color): Boolean =
    board.attackers(pawnSq, color).intersects(board.byPiece(color, Pawn))

  def createsEnemyPawnContact(board: Board, pawnSq: Square, color: Color): Boolean =
    val enemyPawns = board.pawns & board.byColor(!color)
    val attacksEnemyPawn = (pawnSq.pawnAttacks(color) & enemyPawns).nonEmpty
    val frontEnemyPawn =
      forwardSquare(pawnSq, color).exists(sq => board.pieceAt(sq).contains(Piece(!color, Pawn)))
    attacksEnemyPawn || frontEnemyPawn

  def forwardSquare(square: Square, color: Color): Option[Square] =
    Square.at(square.file.value, square.rank.value + (if color.white then 1 else -1))

  def kingAlignedWithRookPawn(board: Board, color: Color, file: File): Boolean =
    board.kingPosOf(color).exists { king =>
      if file == File.H then king.file.value >= File.F.value
      else if file == File.A then king.file.value <= File.C.value
      else false
    }

  def rolePromotionChar(role: Role): Char =
    role match
      case Queen  => 'q'
      case Rook   => 'r'
      case Bishop => 'b'
      case Knight => 'n'
      case _      => 'q'

  def pieceValue(role: Role): Int =
    role match
      case Pawn   => 1
      case Knight => 3
      case Bishop => 3
      case Rook   => 5
      case Queen  => 9
      case King   => 100

  def materialBalance(board: Board, color: Color): Int =
    materialValue(board, color) - materialValue(board, !color)

  def materialValue(board: Board, color: Color): Int =
    Role.all.filter(_ != King).map(role => board.byPiece(color, role).count * pieceValue(role)).sum

  def pieceMobility(board: Board, piece: Piece, square: Square): Int =
    val targets =
      piece.role match
        case Pawn =>
          val direction = if piece.color.white then 1 else -1
          val forward1 = Square.at(square.file.value, square.rank.value + direction)
          val f1 = forward1.filterNot(sq => board.pieceAt(sq).isDefined).map(squareMask).getOrElse(Bitboard.empty)
          val captures = square.pawnAttacks(piece.color) & board.byColor(!piece.color)
          f1 | captures
        case Knight => square.knightAttacks
        case Bishop => square.bishopAttacks(board.occupied)
        case Rook   => square.rookAttacks(board.occupied)
        case Queen  => square.queenAttacks(board.occupied)
        case King   => square.kingAttacks
    (targets & ~board.byColor(piece.color)).count

  def attackedEnemyPieces(board: Board, piece: Piece, square: Square): List[Square] =
    val attacks =
      piece.role match
        case Pawn   => square.pawnAttacks(piece.color)
        case Knight => square.knightAttacks
        case Bishop => square.bishopAttacks(board.occupied)
        case Rook   => square.rookAttacks(board.occupied)
        case Queen  => square.queenAttacks(board.occupied)
        case King   => square.kingAttacks
    (attacks & board.byColor(!piece.color)).squares.toList

  def isLightSquare(square: Square): Boolean =
    (square.file.value + square.rank.value) % 2 == 0

  private def squareMask(square: Square): Bitboard =
    square.file.bb & square.rank.bb

  def isRookPawnFile(file: File): Boolean =
    file == File.A || file == File.H

  def isFlankFile(file: File): Boolean =
    file == File.A || file == File.B || file == File.G || file == File.H

  def isCentralFile(file: File): Boolean =
    MovePredicates.isCentralFile(file)

  def isCentralSquare(square: Square): Boolean =
    Set(File.C, File.D, File.E, File.F).contains(square.file) &&
      Set(Rank.Third, Rank.Fourth, Rank.Fifth, Rank.Sixth).contains(square.rank)

  def isOpenOrSemiOpenFileFor(board: Board, file: File, color: Color): Boolean =
    val mask = Bitboard.file(file)
    val ownPawns = board.pawns & board.byColor(color) & mask
    ownPawns.isEmpty

  def isOutpostSquare(board: Board, sq: Square, color: Color): Boolean =
    val supportedByPawn = board.attackers(sq, color).intersects(board.byPiece(color, Pawn))
    val attackedByEnemyPawn = board.attackers(sq, !color).intersects(board.byPiece(!color, Pawn))
    supportedByPawn && !attackedByEnemyPawn

  def projectedMobility(board: Board, piece: Piece, from: Square, to: Square): Int =
    val occupiedAfter = board.occupied & ~squareMask(from)
    val friendlyAfter = board.byColor(piece.color) & ~squareMask(from)
    val targets =
      piece.role match
        case Pawn =>
          val direction = if piece.color.white then 1 else -1
          val forward1 = Square.at(to.file.value, to.rank.value + direction)
          val f1 = forward1.filterNot(sq => sq == from || board.pieceAt(sq).isDefined).map(squareMask).getOrElse(Bitboard.empty)
          val captures = to.pawnAttacks(piece.color) & board.byColor(!piece.color) & ~squareMask(from)
          f1 | captures
        case Knight => to.knightAttacks
        case Bishop => to.bishopAttacks(occupiedAfter)
        case Rook   => to.rookAttacks(occupiedAfter)
        case Queen  => to.queenAttacks(occupiedAfter)
        case King   => to.kingAttacks
    (targets & ~friendlyAfter).count

  def isOpenFile(board: Board, file: File): Boolean =
    (board.pawns & Bitboard.file(file)).isEmpty

  def isSemiOpenFileFor(board: Board, file: File, color: Color): Boolean =
    val mask = Bitboard.file(file)
    val ours = board.pawns & board.byColor(color) & mask
    val theirs = board.pawns & board.byColor(!color) & mask
    ours.isEmpty && theirs.nonEmpty

  def sideName(color: Color): String =
    if color.white then "white" else "black"

  def pieceToken(role: Role): String =
    role match
      case Pawn   => "P"
      case Knight => "N"
      case Bishop => "B"
      case Rook   => "R"
      case Queen  => "Q"
      case King   => "K"

  def hasPiece(board: Board, color: Color, square: Square, role: Role): Boolean =
    board.pieceAt(square).exists(piece => piece.color == color && piece.role == role)

  def pawnAt(board: Board, color: Color, square: Square): Boolean =
    hasPiece(board, color, square, Pawn)

  def chebyshev(a: Square, b: Square): Int =
    math.max((a.file.value - b.file.value).abs, (a.rank.value - b.rank.value).abs)

  def diagonalClear(board: Board, from: Square, to: Square): Boolean =
    val fileStep = math.signum(to.file.value - from.file.value)
    val rankStep = math.signum(to.rank.value - from.rank.value)
    val fileDiff = (to.file.value - from.file.value).abs
    val rankDiff = (to.rank.value - from.rank.value).abs
    if fileDiff != rankDiff || fileDiff == 0 then false
    else
      (1 until fileDiff).forall { offset =>
        Square
          .at(from.file.value + offset * fileStep, from.rank.value + offset * rankStep)
          .forall(board.pieceAt(_).isEmpty)
      }

  def mostCommon(values: List[String]): Option[String] =
    values.groupBy(identity).toList.sortBy { case (value, grouped) => (-grouped.size, value) }.headOption.map(_._1)

  def relativeRank(square: Square, color: Color): Int =
    if color.white then square.rank.value + 1 else 8 - square.rank.value

  def fileDistance(a: File, b: File): Int =
    (a.value - b.value).abs

  def isRookPawn(square: Square): Boolean =
    isRookPawnFile(square.file)

  def isFlankPawn(square: Square): Boolean =
    isFlankFile(square.file)

  def isCornerRegion(square: Square): Boolean =
    (square.file == File.A || square.file == File.B || square.file == File.G || square.file == File.H) &&
      (square.rank == Rank.First || square.rank == Rank.Second || square.rank == Rank.Seventh || square.rank == Rank.Eighth)

  def isRookBehindPawn(board: Board, color: Color, pawn: Square): Boolean =
    board.byPiece(color, Rook).squares.exists { rook =>
      rook.file == pawn.file &&
        (if color.white then rook.rank.value < pawn.rank.value else rook.rank.value > pawn.rank.value)
    }

  def promotionSquare(pawn: Square, color: Color): Option[Square] =
    Square.at(pawn.file.value, if color.white then 7 else 0)

  def isLikelyPawnMove(move: String): Boolean =
    Option(move).getOrElse("").trim.matches("""^[a-h](?:x[a-h])?[1-8](?:=[QRBN])?[+#]?$""")

  def isPieceMove(move: String): Boolean =
    Option(move).getOrElse("").trim.headOption.exists(ch => "KQRBN".contains(ch))

  def isMinorDevelopmentMove(move: String): Boolean =
    move.matches("""^[NB](?!x).*[a-h][1-8]$""")

  def isCenterReactionMove(move: String): Boolean =
    move.matches("""^(c|d|e|f)[3-6]$""") ||
      List("cxd4", "cxd5", "dxc4", "dxe4", "exd4", "exd5", "fxe4", "fxe5").contains(move)

  def isFianchettoMove(move: String): Boolean =
    Set("g3", "b3", "g6", "b6", "Bg2", "Bb2", "Bg7", "Bb7").contains(move)

  def isQueenMove(move: String): Boolean =
    move.startsWith("Q")

  def isCastleMove(move: String): Boolean =
    move == "O-O" || move == "O-O-O"

  def isBreakPreparationMove(move: String): Boolean =
    Set(
      "c3", "d3", "f3", "a3", "h3", "Qc2", "Qe2", "Be2", "Bd3", "Re1",
      "c6", "d6", "f6", "a6", "h6", "Qc7", "Qe7", "Be7", "Bd6", "Re8",
      "b4", "b5", "g4", "g5", "f4", "f5"
    ).contains(move)

  def scoreDevelopmentLogic(moves: List[String]): Int =
    val minorMoves = moves.count(isMinorDevelopmentMove)
    minorMoves + Option.when(moves.exists(isCastleMove))(1).getOrElse(0) - Option.when(moves.exists(isQueenMove))(1).getOrElse(0)

  def scoreCenterReaction(moves: List[String]): Int =
    val centerMoves = moves.count(isCenterReactionMove)
    centerMoves + Option.when(moves.exists(_.contains("x")))(1).getOrElse(0)

  def scoreFianchettoSupport(moves: List[String]): Int =
    val fianchettoMoves = moves.count(isFianchettoMove)
    fianchettoMoves * 2 + Option.when(moves.exists(isMinorDevelopmentMove))(1).getOrElse(0)

  def scoreQueenExposure(moves: List[String]): Int =
    val queenMoves = moves.count(isQueenMove)
    queenMoves * 2 + Option.when(moves.exists(m => m.startsWith("Q") && m.contains("x")))(1).getOrElse(0)

  def scoreCastleRace(moves: List[String]): Int =
    val castles = moves.count(isCastleMove)
    val flankCommitments = moves.count(m => Set("g4", "g5", "h4", "h5", "a4", "a5", "b4", "b5").contains(m))
    castles * 2 + Option.when(castles >= 1 && flankCommitments >= 1)(1).getOrElse(0)

  def scoreBreakPreparation(moves: List[String]): Int =
    val prepMoves = moves.count(isBreakPreparationMove)
    prepMoves * 2 + Option.when(moves.exists(isCenterReactionMove))(1).getOrElse(0)

  def pieceValueCp(role: Role): Int =
    pieceValue(role) * 100

  def materialCp(board: Board, color: Color): Int =
    Role.all.filter(_ != King).map(role => board.byPiece(color, role).count * pieceValueCp(role)).sum

  def materialDiffCp(board: Board): Int =
    materialCp(board, Color.White) - materialCp(board, Color.Black)

  def sideMaterialDeficitCp(board: Board, side: Color): Int =
    val diff = materialDiffCp(board)
    if side.white then math.max(0, -diff) else math.max(0, diff)

  def forwardDirection(side: Color): Int =
    if side.white then 1 else -1

  def squareAt(key: String): Option[Square] =
    Square.all.find(_.key == key)

  def fileOf(square: String): Option[Char] =
    square.headOption.filter(file => file >= 'a' && file <= 'h')

  def rankOf(square: String): Option[Int] =
    square.lift(1).flatMap(char => Option.when(char >= '1' && char <= '8')(char.asDigit))

  def adjacentSquares(square: String): List[String] =
    for
      file <- fileOf(square).toList
      rank <- rankOf(square).toList
      df <- List(-1, 0, 1)
      dr <- List(-1, 0, 1)
      if df != 0 || dr != 0
      nextFile = (file + df).toChar
      nextRank = rank + dr
      if nextFile >= 'a' && nextFile <= 'h' && nextRank >= 1 && nextRank <= 8
    yield s"$nextFile$nextRank"



