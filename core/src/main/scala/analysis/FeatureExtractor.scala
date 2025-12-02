package chess
package analysis

import chess.opening.OpeningDb

/** 경량 피처 추출기: 엔진 없이 보드 상태만으로 계산 가능한 구조/기물 기반 지표들. */
object FeatureExtractor:

  final case class SideFeatures(
      pawnIslands: Int,
      isolatedPawns: Int,
      doubledPawns: Int,
      passedPawns: Int,
      rookOpenFiles: Int,
      rookSemiOpenFiles: Int,
      bishopPair: Boolean,
      kingRingPressure: Int,
      spaceControl: Int
  )

  final case class Snapshot(
      ply: Ply,
      sideToMove: Color,
      white: SideFeatures,
      black: SideFeatures,
      oppositeColorBishops: Boolean,
      openingAtPly: Option[chess.opening.Opening.AtPly]
  )

  def snapshot(position: Position, ply: Ply, sans: Iterable[format.pgn.SanStr]): Snapshot =
    val whiteFeatures = sideFeatures(position, Color.White)
    val blackFeatures = sideFeatures(position, Color.Black)
    Snapshot(
      ply = ply,
      sideToMove = position.color,
      white = whiteFeatures,
      black = blackFeatures,
      oppositeColorBishops = hasOppositeColorBishops(position.board),
      openingAtPly = OpeningDb.search(sans)
    )

  def sideFeatures(position: Position, color: Color): SideFeatures =
    val board = position.board
    val pawns = board.pawns & board.byColor(color)
    val oppPawns = board.pawns & board.byColor(!color)
    val pawnCountsByFile = countsByFile(pawns)
    val islands = pawnIslands(pawnCountsByFile)
    val isolated = isolatedPawns(pawnCountsByFile)
    val doubled = doubledPawns(pawnCountsByFile)
    val passed = passedPawns(color, pawns, oppPawns)
    val (openFiles, semiOpenFiles) = openAndSemiOpenFiles(pawns, oppPawns)
    val bishopPair = board.count(color, Bishop) >= 2
    val kingRingPressure = kingRingThreats(position, color)
    val spaceControl = spaceScore(position.board, color)
    SideFeatures(
      pawnIslands = islands,
      isolatedPawns = isolated,
      doubledPawns = doubled,
      passedPawns = passed,
      rookOpenFiles = openFiles,
      rookSemiOpenFiles = semiOpenFiles,
      bishopPair = bishopPair,
      kingRingPressure = kingRingPressure,
      spaceControl = spaceControl
    )

  private def countsByFile(pawns: Bitboard): Map[Int, Int] =
    pawns.squares.groupBy(_.file.value).view.mapValues(_.size).toMap

  private def pawnIslands(countsByFile: Map[Int, Int]): Int =
    val files = countsByFile.keys.toList.sorted
    files.foldLeft((0, -2)) { case ((islands, prev), file) =>
      val newIslands = if file != prev + 1 then islands + 1 else islands
      newIslands -> file
    }._1

  private def isolatedPawns(countsByFile: Map[Int, Int]): Int =
    countsByFile.iterator.count { case (file, count) =>
      val left = countsByFile.getOrElse(file - 1, 0)
      val right = countsByFile.getOrElse(file + 1, 0)
      count > 0 && left == 0 && right == 0
    }

  private def doubledPawns(countsByFile: Map[Int, Int]): Int =
    countsByFile.values.foldLeft(0) { (acc, c) =>
      if c > 1 then acc + c else acc
    }

  private def passedPawns(color: Color, pawns: Bitboard, oppPawns: Bitboard): Int =
    val oppSquares = oppPawns.squares
    pawns.squares.count { pawn =>
      val pawnFile = pawn.file.value
      val pawnRank = pawn.rank.value
      val blocking = oppSquares.exists { opp =>
        val fileOk = (opp.file.value - pawnFile).abs <= 1
        val ahead =
          if color == Color.White then opp.rank.value > pawnRank
          else opp.rank.value < pawnRank
        fileOk && ahead
      }
      !blocking
    }

  private def openAndSemiOpenFiles(pawns: Bitboard, oppPawns: Bitboard): (Int, Int) =
    val friendly = pawns
    val enemy = oppPawns
    File.all.foldLeft((0, 0)) { case ((open, semiOpen), file) =>
      val mask = file.bb
      val friendlyOnFile = friendly.intersects(mask)
      val enemyOnFile = enemy.intersects(mask)
      if !friendlyOnFile && !enemyOnFile then (open + 1, semiOpen)
      else if !friendlyOnFile && enemyOnFile then (open, semiOpen + 1)
      else (open, semiOpen)
    }

  private def kingRingThreats(position: Position, color: Color): Int =
    position.kingPosOf(color).fold(0) { kingSq =>
      val ring = kingSq.kingAttacks | kingSq.bb
      val attackers =
        ring.fold(Bitboard.empty)((acc, sq) => acc | position.attackers(sq, !color))
      attackers.count
    }

  private def spaceScore(board: Board, color: Color): Int =
    val attacked = attackedSquares(board, color)
    attacked.squares.count { sq =>
      if color == Color.White then sq.rank.value >= Rank.Fourth.value
      else sq.rank.value <= Rank.Fifth.value
    }

  private def attackedSquares(board: Board, color: Color): Bitboard =
    Square.all.foldLeft(Bitboard.empty) { (acc, sq) =>
      if board.attackers(sq, color).nonEmpty then acc | sq.bb else acc
    }

  def hasOppositeColorBishops(board: Board): Boolean =
    val whiteBishops = (board.bishops & board.white).squares
    val blackBishops = (board.bishops & board.black).squares
    if whiteBishops.size == 1 && blackBishops.size == 1 then
      whiteBishops.head.isLight != blackBishops.head.isLight
    else false
