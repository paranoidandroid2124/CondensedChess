package lila.commentary.validation

import chess.{ Position, Queen, Rook, Square as ChessSquare }
import chess.format.Fen
import chess.variant

import scala.collection.mutable

import lila.commentary.strategic.StrategicObjectExtractor
import lila.commentary.witness.u.UExtractionContext

class CertificationCorpusSemanticSanityTest extends munit.FunSuite:

  private val MaxRenewalStates = 512
  private val rows = CertificationExpectationCorpus.loadAll().map(row => row.id -> row).toMap

  private final case class Square(file: Int, rank: Int):
    def key: String = s"${('a' + file).toChar}${rank + 1}"

  private def boardOf(id: String): Map[String, Char] =
    val fen = rows.getOrElse(id, fail(s"Missing certification row $id")).fen
    parseFenBoard(fen)

  private def parseFenBoard(fen: String): Map[String, Char] =
    val boardPart = fen.takeWhile(_ != ' ')
    val ranks = boardPart.split('/').toVector
    require(ranks.size == 8, s"Unexpected board part in $fen")
    ranks.zipWithIndex.flatMap: (rankPart, rankIndexFromTop) =>
      var file = 0
      val rank = 7 - rankIndexFromTop
      rankPart.flatMap:
        case digit if digit.isDigit =>
          file += digit.asDigit
          None
        case piece =>
          val square = Square(file, rank).key
          file += 1
          Some(square -> piece)
    .toMap

  private def square(key: String): Square =
    Square(key(0) - 'a', key(1) - '1')

  private def clearBetween(from: String, to: String, board: Map[String, Char]): Boolean =
    val start = square(from)
    val end = square(to)
    val fileDelta = end.file - start.file
    val rankDelta = end.rank - start.rank
    val stepFile =
      if fileDelta == 0 then 0 else if fileDelta > 0 then 1 else -1
    val stepRank =
      if rankDelta == 0 then 0 else if rankDelta > 0 then 1 else -1
    val aligned =
      fileDelta == 0 || rankDelta == 0 || math.abs(fileDelta) == math.abs(rankDelta)
    require(aligned, s"$from -> $to is not a straight or diagonal line")
    Iterator
      .iterate(Square(start.file + stepFile, start.rank + stepRank)) { current =>
        Square(current.file + stepFile, current.rank + stepRank)
      }
      .takeWhile(current => current.file != end.file || current.rank != end.rank)
      .forall(current => !board.contains(current.key))

  private def rookAttacks(from: String, to: String, board: Map[String, Char]): Boolean =
    val start = square(from)
    val end = square(to)
    (start.file == end.file || start.rank == end.rank) && clearBetween(from, to, board)

  private def kingAttacks(from: String, to: String): Boolean =
    val start = square(from)
    val end = square(to)
    math.abs(start.file - end.file) <= 1 && math.abs(start.rank - end.rank) <= 1

  private def hasRenewableHeavyPieceCheckingCycle(id: String): Boolean =
    val fen = rows.getOrElse(id, fail(s"Missing certification row $id")).fen
    val extraction =
      StrategicObjectExtractor.fromFen(Fen.Full.clean(fen)).fold(message => fail(message), identity)
    val lowLevel = UExtractionContext(extraction.rootState)
    val sideToMove =
      lowLevel.sideToMove.getOrElse(fail(s"Missing side-to-move for $id"))
    val position = Position(lowLevel.board.toBoard, variant.Standard, sideToMove)

    forcedRenewalFrom(position, Set.empty, mutable.Map.empty)

  private def hasHeavyPieceCheckingMove(id: String): Boolean =
    val fen = rows.getOrElse(id, fail(s"Missing certification row $id")).fen
    val extraction =
      StrategicObjectExtractor.fromFen(Fen.Full.clean(fen)).fold(message => fail(message), identity)
    val lowLevel = UExtractionContext(extraction.rootState)
    val sideToMove =
      lowLevel.sideToMove.getOrElse(fail(s"Missing side-to-move for $id"))
    val position = Position(lowLevel.board.toBoard, variant.Standard, sideToMove)

    checkingMoves(legalMoves(position), position.board.pieceAt).nonEmpty

  private def legalMoves(position: Position): Vector[chess.Move] =
    ChessSquare.all
      .filter(square => position.pieceAt(square).exists(_.color == position.color))
      .flatMap(position.generateMovesAt)
      .sortBy(move => (move.orig.value, move.dest.value))
      .toVector

  private def checkingMoves(
      moves: Vector[chess.Move],
      pieceAt: chess.Square => Option[chess.Piece]
  ): Vector[chess.Move] =
    moves.filter: move =>
      pieceAt(move.orig).exists(piece =>
        (piece.role == Queen || piece.role == Rook) &&
          move.after.check.yes &&
          legalMoves(move.after.position).nonEmpty
      )

  private def forcedRenewalFrom(
      position: Position,
      visiting: Set[String],
      memo: mutable.Map[String, Boolean]
  ): Boolean =
    val key = positionKey(position)
    if visiting.contains(key) then true
    else if memo.size > MaxRenewalStates then false
    else
      memo.get(key) match
        case Some(result) => result
        case None =>
          val nextVisiting = visiting + key
          val result =
            checkingMoves(legalMoves(position), position.board.pieceAt).exists: move =>
              val defenderReplies = legalMoves(move.after.position)
              defenderReplies.nonEmpty &&
                defenderReplies.forall(reply =>
                  forcedRenewalFrom(reply.after.position, nextVisiting, memo)
                )
          memo.update(key, result)
          result

  private def positionKey(position: Position): String =
    val pieces =
      ChessSquare.all
        .flatMap(square => position.pieceAt(square).map(piece => s"${square.key}:${piece.color}:${piece.role}"))
        .mkString(",")
    val enPassant =
      position.enPassantSquare.map(_.key).getOrElse("-")
    s"$pieces|${position.color}|${position.history.castles.value}|$enPassant"

  test("material-harvest nasty and best-defense rows stay on defended or equal-trade boards"):
    val nastyBoard = boardOf("cert-material-harvest-nasty-negative")
    assertEquals(nastyBoard("d5"), 'r')
    assertEquals(nastyBoard("e6"), 'k')
    assertEquals(nastyBoard("d4"), 'R')
    assert(rookAttacks("d4", "d5", nastyBoard))
    assert(kingAttacks("e6", "d5"))

    val supportBoard = boardOf("cert-material-harvest-best-defense")
    assertEquals(supportBoard("d5"), 'n')
    assertEquals(supportBoard("e8"), 'k')
    assertEquals(supportBoard("d4"), 'R')
    assert(rookAttacks("d4", "d5", supportBoard))
    assert(!kingAttacks("e8", "d5"))

  test("winning-endgame best-defense row keeps the passer supported and out of immediate capture"):
    val board = boardOf("cert-winning-endgame-best-defense")
    assertEquals(board("f8"), 'k')
    assertEquals(board("f6"), 'K')
    assertEquals(board("e6"), 'P')
    assert(!kingAttacks("f8", "e6"))
    assert(kingAttacks("f6", "e6"))

  test("perpetual exact row keeps a heavy-piece checking move whose every defender reply renews the check"):
    assert(hasRenewableHeavyPieceCheckingCycle("cert-perpetual-check-holding-exact"))

  test("perpetual near-miss row keeps a heavy-piece check but loses the renewal loop on at least one defender reply"):
    assert(hasHeavyPieceCheckingMove("cert-perpetual-check-holding-near-miss"))
    assert(!hasRenewableHeavyPieceCheckingCycle("cert-perpetual-check-holding-near-miss"))

  test("perpetual nasty-negative row keeps a check but every defender reply breaks the renewal loop"):
    assert(hasHeavyPieceCheckingMove("cert-perpetual-check-holding-nasty-negative"))
    assert(!hasRenewableHeavyPieceCheckingCycle("cert-perpetual-check-holding-nasty-negative"))

  test("perpetual best-defense row keeps the renewable cycle while evidence remains deferred"):
    val deferredBoard = boardOf("cert-perpetual-check-holding-best-defense")
    assertEquals(deferredBoard("f8"), 'r')
    assertEquals(deferredBoard("h8"), 'k')
    assertEquals(deferredBoard("g7"), 'R')
    assertEquals(deferredBoard("g6"), 'K')
    assert(hasRenewableHeavyPieceCheckingCycle("cert-perpetual-check-holding-best-defense"))
