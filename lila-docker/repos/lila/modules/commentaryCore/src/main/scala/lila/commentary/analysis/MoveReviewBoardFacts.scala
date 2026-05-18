package lila.commentary.analysis

import lila.commentary.model.NarrativeContext

private[commentary] object MoveReviewBoardFacts:

  val CenterSquares: Set[String] = Set("d4", "e4", "d5", "e5")
  private val HighValuePieces = Set('q', 'r', 'k')

  final case class Coord(file: Int, rank: Int):
    def key: String = s"${('a' + file).toChar}$rank"
    def +(delta: (Int, Int)): Option[Coord] =
      val next = Coord(file + delta._1, rank + delta._2)
      Option.when(next.file >= 0 && next.file <= 7 && next.rank >= 1 && next.rank <= 8)(next)

  final case class MoveFacts(
      uci: String,
      san: String,
      from: Coord,
      to: Coord,
      piece: Char,
      side: Char,
      before: Map[String, Char],
      afterFen: String,
      afterBoard: Map[String, Char]
  ):
    def fromKey: String = from.key
    def toKey: String = to.key
    def role: Char = piece.toLower
    def isWhite: Boolean = side == 'w'
    def isPawn: Boolean = role == 'p'
    def isMinor: Boolean = role == 'n' || role == 'b'
    def isKing: Boolean = role == 'k'
    def capturedPiece: Option[Char] = before.get(toKey).filter(_.isUpper != piece.isUpper)
    def isCapture: Boolean = san.contains("x") || capturedPiece.nonEmpty
    def isCastle: Boolean =
      san.startsWith("O-O") || Set("e1g1", "e1c1", "e8g8", "e8c8").contains(uci.toLowerCase)
    def after: Map[String, Char] = afterBoard
    def attackedEnemyPieces: Set[(String, Char)] =
      after.collect {
        case (square, target) if target.isUpper != piece.isUpper && attacks(square) => square -> target.toLower
      }.toSet
    def attacks(targetSquare: String): Boolean =
      MoveReviewBoardFacts.attacks(role, isWhite, to, targetSquare, after)

  def current(ctx: NarrativeContext): Option[MoveFacts] =
    for
      uci <- ctx.playedMove.map(_.trim).filter(_.matches("^[a-h][1-8][a-h][1-8][qrbn]?$"))
      san <- ctx.playedSan.map(_.trim).filter(_.nonEmpty)
      shape <- from(ctx.fen, uci, san)
    yield shape

  def from(fen: String, uci: String, san: String): Option[MoveFacts] =
    for
      from <- coord(uci.take(2))
      to <- coord(uci.slice(2, 4))
      before = boardFromFen(fen)
      piece <- before.get(from.key)
      side <- sideToMove(fen)
      afterFen <- MoveReviewPvChainValidator.legalFenAfter(fen, uci)
      afterBoard = boardFromFen(afterFen)
    yield MoveFacts(uci.toLowerCase, san, from, to, piece, side, before, afterFen, afterBoard)

  def primitiveTags(ctx: NarrativeContext, facts: MoveFacts): Set[String] =
    val tags = scala.collection.mutable.LinkedHashSet.empty[String]
    if developsPiece(facts) then tags += "develops_piece"
    if controlsCenter(facts) then tags += "controls_center"
    if improvesKingSafety(facts) then tags += "king_safety"
    if winsTempo(facts) then tags += "tempo"
    if targetsF7OrF2(facts) then tags += "targets_f7_or_f2"
    if opensLine(facts) then tags += "opens_line"
    if defendsCenterPawn(facts) then tags += "defends_center_pawn"
    if createsBasicThreat(facts) then tags += "creates_basic_threat"
    if isEndgameTechnique(ctx, facts) then tags += "endgame_technique"
    tags.toSet

  def boardFromFen(fen: String): Map[String, Char] =
    val placement = Option(fen).getOrElse("").takeWhile(_ != ' ')
    val board = scala.collection.mutable.Map.empty[String, Char]
    val ranks = placement.split('/').toList
    ranks.zipWithIndex.foreach { case (row, rowIdx) =>
      var file = 0
      val rank = 8 - rowIdx
      row.foreach {
        case digit if digit.isDigit => file += digit.asDigit
        case piece =>
          if file >= 0 && file <= 7 then board += (Coord(file, rank).key -> piece)
          file += 1
      }
    }
    board.toMap

  def sideToMove(fen: String): Option[Char] =
    Option(fen).getOrElse("").split("\\s+").lift(1).flatMap(_.headOption).filter(ch => ch == 'w' || ch == 'b')

  def coord(square: String): Option[Coord] =
    Option(square).filter(_.matches("^[a-h][1-8]$")).map { sq =>
      Coord(sq.charAt(0) - 'a', sq.charAt(1).asDigit)
    }

  def attacks(role: Char, isWhite: Boolean, from: Coord, targetSquare: String, occupancy: Map[String, Char]): Boolean =
    coord(targetSquare).exists { target =>
      val df = target.file - from.file
      val dr = target.rank - from.rank
      role.toLower match
        case 'p' =>
          val direction = if isWhite then 1 else -1
          math.abs(df) == 1 && dr == direction
        case 'n' =>
          Set((1, 2), (2, 1), (2, -1), (1, -2), (-1, -2), (-2, -1), (-2, 1), (-1, 2)).contains((df, dr))
        case 'b' =>
          math.abs(df) == math.abs(dr) && clearRay(from, target, occupancy)
        case 'r' =>
          (df == 0 || dr == 0) && clearRay(from, target, occupancy)
        case 'q' =>
          (math.abs(df) == math.abs(dr) || df == 0 || dr == 0) && clearRay(from, target, occupancy)
        case 'k' =>
          math.abs(df) <= 1 && math.abs(dr) <= 1
        case _ => false
    }

  private def developsPiece(facts: MoveFacts): Boolean =
    facts.isMinor &&
      ((facts.isWhite && facts.from.rank == 1) || (!facts.isWhite && facts.from.rank == 8)) &&
      !facts.isCapture

  private def controlsCenter(facts: MoveFacts): Boolean =
    CenterSquares.contains(facts.toKey) ||
      CenterSquares.exists(square => facts.attacks(square))

  private def improvesKingSafety(facts: MoveFacts): Boolean =
    facts.isCastle || (facts.isKing && (facts.toKey == "g1" || facts.toKey == "c1" || facts.toKey == "g8" || facts.toKey == "c8"))

  private def winsTempo(facts: MoveFacts): Boolean =
    facts.isCapture || facts.attackedEnemyPieces.exists { case (_, piece) => HighValuePieces.contains(piece) }

  private def targetsF7OrF2(facts: MoveFacts): Boolean =
    val target = if facts.isWhite then "f7" else "f2"
    facts.attacks(target)

  private def opensLine(facts: MoveFacts): Boolean =
    facts.isPawn &&
      ((facts.from.file == 2 && facts.to.file == 2) ||
        (facts.from.file == 3 && facts.to.file == 3) ||
        (facts.from.file == 4 && facts.to.file == 4)) &&
      math.abs(facts.to.rank - facts.from.rank) >= 1

  private def defendsCenterPawn(facts: MoveFacts): Boolean =
    val ownPieces = facts.after.filter { case (_, piece) => piece.isUpper == facts.piece.isUpper }
    ownPieces.exists { case (square, piece) =>
      piece.toLower == 'p' && CenterSquares.contains(square) && facts.attacks(square)
    }

  private def createsBasicThreat(facts: MoveFacts): Boolean =
    facts.attackedEnemyPieces.nonEmpty && !facts.isCapture

  private def isEndgameTechnique(ctx: NarrativeContext, facts: MoveFacts): Boolean =
    ctx.phase.current.equalsIgnoreCase("Endgame") && (facts.isKing || facts.isPawn || facts.role == 'r')

  private def clearRay(from: Coord, to: Coord, occupancy: Map[String, Char]): Boolean =
    val df = Integer.signum(to.file - from.file)
    val dr = Integer.signum(to.rank - from.rank)
    var current = from + (df, dr)
    while current.exists(c => c != to) do
      if current.exists(c => occupancy.contains(c.key)) then return false
      current = current.flatMap(_ + (df, dr))
    current.contains(to)
