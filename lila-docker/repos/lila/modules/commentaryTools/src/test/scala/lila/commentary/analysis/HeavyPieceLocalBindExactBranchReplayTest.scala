package lila.commentary.analysis

import munit.FunSuite

class HeavyPieceLocalBindExactBranchReplayTest extends FunSuite:

  private val QueenInfiltrationFen =
    "2rq1rk1/pp3ppp/2n1pn2/3p4/3P4/2P1PN2/PPQ2PPP/2R2RK1 w - - 0 24"
  private val WrongTurnQueenInfiltrationFen =
    "2rq1rk1/pp3ppp/2n1pn2/3p4/3P4/2P1PN2/PPQ2PPP/2R2RK1 b - - 0 24"
  private val RookLiftFen =
    "2rq1rk1/pp3ppp/2n1pn2/3p4/3P2P1/2P1P3/PPQ2PBP/2RR2K1 w - - 0 24"
  private val PerpetualFen =
    "r4rk1/5ppp/8/8/7q/8/2Q3P1/R5K1 b - - 0 1"
  private val ExchangeSacFen =
    "2rq1rk1/pp3ppp/4pn2/3p4/3P4/4PN2/PP1Q1PPP/2B2RK1 w - - 0 24"
  private val BenignRookCaptureFen =
    "6k1/p7/8/8/R7/8/8/6K1 w - - 0 1"
  private val QueenCentralizationFen =
    "6k1/8/8/8/8/3Q4/8/6K1 w - - 0 1"
  private val BackRankRookShuffleFen =
    "5rk1/8/8/8/8/8/8/6K1 b - - 0 1"

  test("exact branch replay derives queen infiltration from a legal UCI line") {
    val replay =
      HeavyPieceLocalBindValidation
        .replayBranchLine(
          QueenInfiltrationFen,
          List("b2b3", "d8e7", "c3c4", "e7a3")
        )
        .getOrElse(fail("expected replay"))

    assertEquals(replay.complete, true, clues(replay))
    assert(replay.features.contains("queen_infiltration"), clues(replay))
    assertEquals(replay.stopReason, None, clues(replay))
  }

  test("exact branch replay derives rook lift from a legal heavy-piece line") {
    val replay =
      HeavyPieceLocalBindValidation
        .replayBranchLine(
          RookLiftFen,
          List("d1d2", "c6e7", "g2f3", "f8e8")
        )
        .getOrElse(fail("expected replay"))

    assertEquals(replay.complete, true, clues(replay))
    assert(replay.features.contains("rook_lift"), clues(replay))
  }

  test("exact branch replay derives repeated heavy-piece checks from the line itself") {
    val replay =
      HeavyPieceLocalBindValidation
        .replayBranchLine(
          PerpetualFen,
          List("h4e1", "g1h2", "e1h4", "h2g1", "h4e1")
        )
        .getOrElse(fail("expected replay"))

    assertEquals(replay.complete, true, clues(replay))
    assert(replay.features.contains("forcing_checks"), clues(replay))
    assert(replay.features.contains("perpetual_check"), clues(replay))
  }

  test("exact branch replay derives exchange-sac release from capture plus recapture") {
    val replay =
      HeavyPieceLocalBindValidation
        .replayBranchLine(
          ExchangeSacFen,
          List("h2h3", "c8c1", "f1c1")
        )
        .getOrElse(fail("expected replay"))

    assertEquals(replay.complete, true, clues(replay))
    assert(replay.features.contains("exchange_sac_release"), clues(replay))
  }

  test("exact branch replay does not treat an unrecaptured rook capture as exchange sacrifice") {
    val replay =
      HeavyPieceLocalBindValidation
        .replayBranchLine(
          BenignRookCaptureFen,
          List("a4a7", "g8f8", "g1f1")
        )
        .getOrElse(fail("expected replay"))

    assertEquals(replay.complete, true, clues(replay))
    assert(!replay.features.contains("exchange_sac_release"), clues(replay))
  }

  test("exact branch replay does not treat queen centralization as infiltration release") {
    val replay =
      HeavyPieceLocalBindValidation
        .replayBranchLine(
          QueenCentralizationFen,
          List("d3d5", "g8f8", "g1f1")
        )
        .getOrElse(fail("expected replay"))

    assertEquals(replay.complete, true, clues(replay))
    assert(!replay.features.contains("queen_infiltration"), clues(replay))
    assert(!replay.features.contains("forcing_checks"), clues(replay))
  }

  test("exact branch replay does not treat a back-rank rook shuffle as rook lift") {
    val replay =
      HeavyPieceLocalBindValidation
        .replayBranchLine(
          BackRankRookShuffleFen,
          List("f8e8", "g1f1", "g8f7")
        )
        .getOrElse(fail("expected replay"))

    assertEquals(replay.complete, true, clues(replay))
    assert(!replay.features.contains("rook_lift"), clues(replay))
  }

  test("short exact branch does not count as release proof") {
    val replay =
      HeavyPieceLocalBindValidation
        .replayBranchLine(
          RookLiftFen,
          List("c2e2", "c6e7")
        )
        .getOrElse(fail("expected replay"))

    assertEquals(replay.complete, true, clues(replay))
    assertEquals(replay.stopReason, None, clues(replay))
    assertEquals(replay.features, Nil, clues(replay))
  }

  test("illegal tail after a legal release prefix fails closed") {
    val replay =
      HeavyPieceLocalBindValidation
        .replayBranchLine(
          RookLiftFen,
          List("c2e2", "c6e7", "zzzz")
        )
        .getOrElse(fail("expected replay"))

    assertEquals(replay.complete, false, clues(replay))
    assert(replay.stopReason.exists(_.startsWith("invalid_uci:")), clues(replay))
    assertEquals(replay.features, Nil, clues(replay))
  }

  test("illegal heavy-piece paraphrase does not count as exact release proof") {
    val replay =
      HeavyPieceLocalBindValidation
        .replayBranchLine(
          QueenInfiltrationFen,
          List("d8h4", "g2g3", "h4h3")
        )
        .getOrElse(fail("expected replay"))

    assertEquals(replay.complete, false, clues(replay))
    assert(replay.stopReason.exists(_.startsWith("illegal_move:")), clues(replay))
    assertEquals(replay.features, Nil, clues(replay))
  }

  test("wrong-base FEN fails closed even when the UCI line is real on the source position") {
    val replay =
      HeavyPieceLocalBindValidation
        .replayBranchLine(
          WrongTurnQueenInfiltrationFen,
          List("b2b3", "d8e7", "c3c4", "e7a3")
        )
        .getOrElse(fail("expected replay"))

    assertEquals(replay.complete, false, clues(replay))
    assert(replay.stopReason.exists(_.startsWith("illegal_move:")), clues(replay))
    assertEquals(replay.features, Nil, clues(replay))
  }
