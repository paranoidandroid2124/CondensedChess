package lila.commentary.analysis

import lila.commentary.*
import munit.FunSuite

final class MoveReviewPvLineTest extends FunSuite:

  private def line(startFen: String, ucis: List[String], sans: List[String], lineId: String = "line_01"): MoveReviewVariationRef =
    val fens = ucis.indices.toList.map(idx => NarrativeUtils.uciListToFen(startFen, ucis.take(idx + 1)))
    MoveReviewVariationRef(
      lineId = lineId,
      scoreCp = 12,
      mate = None,
      depth = 16,
      moves = ucis.zip(sans).zipWithIndex.map { case ((uci, san), idx) =>
        val ply = NarrativeUtils.plyFromFen(startFen).map(_ + 1 + idx).getOrElse(idx + 1)
        MoveReviewMoveRef(
          refId = s"${lineId}_m${idx + 1}",
          san = san,
          uci = uci,
          fenAfter = fens(idx),
          ply = ply,
          moveNo = (ply + 1) / 2,
          marker = None
        )
      }
    )

  test("validates a legal PV chain and exposes first coupled line facts") {
    val fen = "r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3"
    val pv = line(fen, List("f1c4", "g8f6", "d2d3"), List("Bc4", "Nf6", "d3"))
    val refs = MoveReviewRefs(startFen = fen, startPly = 5, variations = List(pv))

    val validated = MoveReviewPvLine.validatedLine(fen, pv, "f1c4")
    val coupled = MoveReviewPvLine.firstCoupled(fen, "f1c4", Some(refs))

    assert(validated.nonEmpty, clue(validated))
    assertEquals(validated.map(_.line.lineId), Some("line_01"), clue(validated))
    assertEquals(validated.map(_.moves.map(_.uci)), Some(List("f1c4", "g8f6", "d2d3")), clue(validated))
    assertEquals(coupled.map(_.first.uci), Some("f1c4"), clue(coupled))
    assertEquals(coupled.flatMap(_.reply).map(_.uci), Some("g8f6"), clue(coupled))
    assertEquals(coupled.flatMap(_.continuation).map(_.uci), Some("d2d3"), clue(coupled))
  }

  test("rejects illegal, wrong-side, mismatched-FEN, missing-FEN, unordered, and mismatched-start lines") {
    val fen = "r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3"
    val pv = line(fen, List("f1c4", "g8f6", "d2d3"), List("Bc4", "Nf6", "d3"))
    val illegal = line(fen, List("f1c4", "g8g6"), List("Bc4", "Ng6"))
    val wrongSide = line(fen, List("g8f6", "f1c4"), List("Nf6", "Bc4"))
    val corrupted = pv.copy(moves = pv.moves.updated(1, pv.moves(1).copy(fenAfter = pv.moves.head.fenAfter)))
    val missingFen = pv.copy(moves = pv.moves.updated(0, pv.moves.head.copy(fenAfter = "")))
    val unordered = pv.copy(moves = pv.moves.updated(1, pv.moves(1).copy(ply = pv.moves.head.ply)))
    val otherStart = "r1bqkbnr/pppp1ppp/2n5/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R b KQkq - 3 3"

    assertEquals(MoveReviewPvLine.validatedLine(fen, illegal, "f1c4"), None)
    assertEquals(MoveReviewPvLine.validatedLine(fen, wrongSide, "g8f6"), None)
    assertEquals(MoveReviewPvLine.validatedLine(fen, corrupted, "f1c4"), None)
    assertEquals(MoveReviewPvLine.validatedLine(fen, missingFen, "f1c4"), None)
    assertEquals(MoveReviewPvLine.validatedLine(fen, unordered, "f1c4"), None)
    assertEquals(MoveReviewPvLine.validatedLine(otherStart, pv, "f1c4"), None)
  }

  test("keeps shortLine assembly separate from semantic validation") {
    val fen = "r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3"
    val pv = line(fen, List("f1c4", "g8f6", "d2d3"), List("Bc4", "Nf6", "d3"), "main")
    val refs = MoveReviewRefs(startFen = fen, startPly = 5, variations = List(pv))

    val short = MoveReviewPvLine.shortLine(Some(refs), Some("main"))

    assertEquals(short.map(_.san), Some(List("Bc4", "Nf6", "d3")), clue(short))
    assertEquals(short.map(_.uci), Some(List("f1c4", "g8f6", "d2d3")), clue(short))
    assertEquals(short.flatMap(_.lineId), Some("main"), clue(short))
  }

  test("keeps shortLine visible when invalid PV is barred from semantic coupling") {
    val fen = "r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3"
    val invalid = line(fen, List("f1c4", "g8g6"), List("Bc4", "Ng6"), "invalid")
    val refs = MoveReviewRefs(startFen = fen, startPly = 5, variations = List(invalid))

    assertEquals(MoveReviewPvLine.firstCoupled(fen, "f1c4", Some(refs)), None)
    assertEquals(MoveReviewPvLine.shortLine(Some(refs), Some("invalid")).map(_.san), Some(List("Bc4", "Ng6")))
  }

  test("validates castling, promotion, and en-passant legal replay") {
    val castleFen = "r1bqkb1r/pppp1ppp/2n2n2/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4"
    val castle = line(castleFen, List("e1g1", "f8c5"), List("O-O", "Bc5"), "castle")
    assert(MoveReviewPvLine.validatedLine(castleFen, castle, "e1g1").nonEmpty)

    val promotionFen = "4k3/P7/8/8/8/8/8/4K3 w - - 0 1"
    val promotion = line(promotionFen, List("a7a8q", "e8f7"), List("a8=Q", "Kf7"), "promotion")
    assert(MoveReviewPvLine.validatedLine(promotionFen, promotion, "a7a8q").nonEmpty)

    val epFen = "rnbqkbnr/pp2pppp/8/2ppP3/8/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 3"
    val enPassant = line(epFen, List("e5d6", "g8f6"), List("exd6", "Nf6"), "en_passant")
    assert(MoveReviewPvLine.validatedLine(epFen, enPassant, "e5d6").nonEmpty)
  }
