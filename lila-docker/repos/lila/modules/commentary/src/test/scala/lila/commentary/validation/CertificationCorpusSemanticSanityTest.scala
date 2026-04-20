package lila.commentary.validation

class CertificationCorpusSemanticSanityTest extends munit.FunSuite:

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

  private def queenAttacks(from: String, to: String, board: Map[String, Char]): Boolean =
    val start = square(from)
    val end = square(to)
    val straight = start.file == end.file || start.rank == end.rank
    val diagonal = math.abs(start.file - end.file) == math.abs(start.rank - end.rank)
    (straight || diagonal) && clearBetween(from, to, board)

  private def kingAttacks(from: String, to: String): Boolean =
    val start = square(from)
    val end = square(to)
    math.abs(start.file - end.file) <= 1 && math.abs(start.rank - end.rank) <= 1

  test("material-harvest nasty and best-defense rows stay on defended or equal-trade boards"):
    val nastyBoard = boardOf("cert-material-harvest-nasty-negative")
    assertEquals(nastyBoard("d5"), 'r')
    assertEquals(nastyBoard("e6"), 'k')
    assertEquals(nastyBoard("d4"), 'R')
    assert(rookAttacks("d4", "d5", nastyBoard))
    assert(kingAttacks("e6", "d5"))

    val supportBoard = boardOf("cert-material-harvest-best-defense")
    assertEquals(supportBoard("d5"), 'n')
    assertEquals(supportBoard("e6"), 'k')
    assertEquals(supportBoard("d4"), 'R')
    assert(rookAttacks("d4", "d5", supportBoard))
    assert(kingAttacks("e6", "d5"))

  test("winning-endgame best-defense row keeps the passer out of immediate capture"):
    val board = boardOf("cert-winning-endgame-best-defense")
    assertEquals(board("e7"), 'k')
    assertEquals(board("d5"), 'K')
    assertEquals(board("c6"), 'P')
    assert(!kingAttacks("e7", "c6"))
    assert(kingAttacks("d5", "c6"))

  test("perpetual rows show a present check and an explicit follow-up checking geometry"):
    val exactBoard = boardOf("cert-perpetual-check-holding-exact")
    assertEquals(exactBoard("h7"), 'k')
    assertEquals(exactBoard("c2"), 'Q')
    assert(queenAttacks("c2", "h7", exactBoard))
    assert(queenAttacks("h7", "g8", exactBoard - "h7"))
    assert(queenAttacks("c8", "h8", exactBoard))

    val deferredBoard = boardOf("cert-perpetual-check-holding-best-defense")
    assertEquals(deferredBoard("e8"), 'r')
    assertEquals(deferredBoard("h7"), 'k')
    assertEquals(deferredBoard("c2"), 'Q')
    assert(queenAttacks("c2", "h7", deferredBoard))
