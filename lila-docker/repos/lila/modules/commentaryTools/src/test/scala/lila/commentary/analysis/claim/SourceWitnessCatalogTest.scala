package lila.commentary.analysis.claim

import lila.commentary.tools.claim.*

import lila.commentary.PgnAnalysisHelper
import munit.FunSuite

class SourceWitnessCatalogTest extends FunSuite:

  test("source-backed B/C candidates are fixed and fully described") {
    val rows = SourceWitnessCatalog.all
    assertEquals(
      rows.map(_.id),
      List(
        "source-evans-opsahl-1950",
        "source-evans-opsahl-1950-iqp-inducement",
        "source-karpov-unzicker-1974-break-prevention",
        "source-karpov-andersson-1975-hedgehog-break-screen",
        "source-karpov-andersson-1975-iqp-inducement",
        "source-lokvenc-czerniak-1952-b6-b5-break-prevention",
        "source-maderna-palermo-1955-a6-a5-break-prevention",
        "source-maderna-palermo-1955-central-break-timing",
        "source-maderna-palermo-1955-central-break-prep-review",
        "source-camara-bazan-1960-b7-b5-break-prevention",
        "source-camara-bazan-1960-d5-color-complex-squeeze",
        "source-sliwa-gromek-1960-a6-a5-break-prevention",
        "source-luckis-bielicki-1961-a6-a5-break-prevention",
        "source-pfleger-maalouf-1961-a6-a5-break-prevention",
        "source-pfleger-maalouf-1961-d5-color-complex-squeeze",
        "source-polugaevsky-giorgadze-1956-c5-c4-break-prevention",
        "source-boleslavsky-nezhmetdinov-1950-static-weakness-fixation",
        "source-maderna-palermo-1955-static-weakness-fixation",
        "source-aronian-andreikin-2014-defender-trade",
        "source-bad-piece-liquidation-pilot",
        "source-capablanca-golombek-1939",
        "source-capablanca-golombek-1939-bad-piece-liquidation",
        "source-capablanca-golombek-1939-iqp-inducement",
        "source-botvinnik-vidmar-1936",
        "source-botvinnik-vidmar-1936-iqp-multipv-screen",
        "source-botvinnik-vidmar-1936-simplification-window",
        "source-botvinnik-vidmar-1936-iqp-opening-inducement",
        "source-botvinnik-vidmar-1936-flank-clamp",
        "source-botvinnik-vidmar-1936-e4-color-complex-squeeze",
        "source-kramnik-anand-2001",
        "source-kramnik-anand-2001-iqp-opening-inducement",
        "source-tartakower-capablanca-1924",
        "source-alekhine-bogoljubow-1936-iqp-inducement",
        "source-salov-ljubojevic-1992-simplification-window",
        "source-najdorf-sergeant-1939-iqp-inducement",
        "source-carlsen-anand-2014-g6",
        "source-carlsen-anand-2014-g6-queen-trade-completion"
      )
    )
    assertEquals(rows.map(_.id).distinct.size, rows.size)
    assert(rows.forall(_.sourceUrl.startsWith("https://")), clues(rows.map(_.sourceUrl)))
    assert(rows.forall(_.gameName.nonEmpty), clues(rows))
    assert(rows.forall(_.reviewGroup.nonEmpty), clues(rows))
    assert(rows.forall(_.intendedVerdict.nonEmpty), clues(rows))
    assert(rows.forall(_.validationNote.nonEmpty), clues(rows))
  }

  test("source PGNs parse strictly and candidate windows resolve to exact FEN rows") {
    SourceWitnessCatalog.all.foreach { row =>
      val plyData =
        PgnAnalysisHelper
          .extractPlyDataStrict(row.pgn)
          .fold(err => fail(s"${row.id} PGN did not parse strictly: $err"), identity)
      val window =
        plyData.filter(ply => row.candidatePlyRange.contains(ply.ply))

      assert(window.nonEmpty, clues(row.id, row.candidatePlyRange, plyData.map(_.ply)))
      assert(window.exists(_.fen.trim.nonEmpty), clues(row.id, window))
      assert(window.exists(_.playedUci.trim.nonEmpty), clues(row.id, window))
    }
  }

  test("natural IQP admission rows resolve to the exact expected ply") {
    val byId = SourceWitnessCatalog.all.map(row => row.id -> row).toMap
    val evans = byId("source-evans-opsahl-1950-iqp-inducement")
    val capablanca = byId("source-capablanca-golombek-1939-iqp-inducement")
    val capablancaBadPiece = byId("source-capablanca-golombek-1939-bad-piece-liquidation")
    val botvinnik = byId("source-botvinnik-vidmar-1936-iqp-multipv-screen")
    val botvinnikSimplification = byId("source-botvinnik-vidmar-1936-simplification-window")
    val botvinnikOpening = byId("source-botvinnik-vidmar-1936-iqp-opening-inducement")
    val botvinnikFlankClamp = byId("source-botvinnik-vidmar-1936-flank-clamp")
    val botvinnikE4ColorComplex = byId("source-botvinnik-vidmar-1936-e4-color-complex-squeeze")
    val kramnikOpening = byId("source-kramnik-anand-2001-iqp-opening-inducement")
    val alekhine = byId("source-alekhine-bogoljubow-1936-iqp-inducement")
    val najdorf = byId("source-najdorf-sergeant-1939-iqp-inducement")
    val carlsenQueenTrade = byId("source-carlsen-anand-2014-g6")
    val carlsenQueenTradeCompletion = byId("source-carlsen-anand-2014-g6-queen-trade-completion")
    val salovSimplification = byId("source-salov-ljubojevic-1992-simplification-window")
    val boleslavskyStaticWeakness = byId("source-boleslavsky-nezhmetdinov-1950-static-weakness-fixation")
    val madernaStaticWeakness = byId("source-maderna-palermo-1955-static-weakness-fixation")
    val aronianDefenderTrade = byId("source-aronian-andreikin-2014-defender-trade")
    val badPieceLiquidation = byId("source-bad-piece-liquidation-pilot")
    val karpovUnzickerBreakPrevention = byId("source-karpov-unzicker-1974-break-prevention")
    val karpovAnderssonBreakScreen = byId("source-karpov-andersson-1975-hedgehog-break-screen")
    val karpovAnderssonIqp = byId("source-karpov-andersson-1975-iqp-inducement")
    val lokvencBreakPrevention = byId("source-lokvenc-czerniak-1952-b6-b5-break-prevention")
    val madernaBreakPrevention = byId("source-maderna-palermo-1955-a6-a5-break-prevention")
    val madernaCentralBreak = byId("source-maderna-palermo-1955-central-break-timing")
    val madernaCentralBreakPrep = byId("source-maderna-palermo-1955-central-break-prep-review")
    val camaraBreakPrevention = byId("source-camara-bazan-1960-b7-b5-break-prevention")
    val camaraD5ColorComplex = byId("source-camara-bazan-1960-d5-color-complex-squeeze")
    val sliwaBreakPrevention = byId("source-sliwa-gromek-1960-a6-a5-break-prevention")
    val luckisBreakPrevention = byId("source-luckis-bielicki-1961-a6-a5-break-prevention")
    val pflegerBreakPrevention = byId("source-pfleger-maalouf-1961-a6-a5-break-prevention")
    val pflegerD5ColorComplex = byId("source-pfleger-maalouf-1961-d5-color-complex-squeeze")
    val polugaevskyBreakPrevention = byId("source-polugaevsky-giorgadze-1956-c5-c4-break-prevention")

    val evansWindow =
      PgnAnalysisHelper
        .extractPlyDataStrict(evans.pgn)
        .fold(err => fail(s"${evans.id} PGN did not parse strictly: $err"), identity)
        .filter(ply => evans.candidatePlyRange.contains(ply.ply))
    assertEquals(evansWindow.map(_.ply), List(33))
    assertEquals(evansWindow.head.fen, "r3rnk1/1p3ppp/p1p5/3p2q1/PP1P2b1/2QBP3/3N1PPP/1R3RK1 w - - 3 17")
    assertEquals(evansWindow.head.playedUci, "f1c1")

    val capablancaWindow =
      PgnAnalysisHelper
        .extractPlyDataStrict(capablanca.pgn)
        .fold(err => fail(s"${capablanca.id} PGN did not parse strictly: $err"), identity)
        .filter(ply => capablanca.candidatePlyRange.contains(ply.ply))
    assertEquals(capablancaWindow.map(_.ply), List(45))
    assertEquals(capablancaWindow.head.fen, "r3r1k1/pp3pn1/2pq2pp/3p4/NP1P4/3QP2P/P4PP1/1RR3K1 w - - 0 23")
    assertEquals(capablancaWindow.head.playedUci, "b4b5")

    val capablancaBadPieceWindow =
      PgnAnalysisHelper
        .extractPlyDataStrict(capablancaBadPiece.pgn)
        .fold(err => fail(s"${capablancaBadPiece.id} PGN did not parse strictly: $err"), identity)
        .filter(ply => capablancaBadPiece.candidatePlyRange.contains(ply.ply))
    assertEquals(capablancaBadPieceWindow.map(_.ply), List(43))
    assertEquals(
      capablancaBadPieceWindow.head.fen,
      "r2qr1k1/pp3pn1/2pb2pp/3pB3/NP1P4/3QP2P/P4PP1/1RR3K1 w - - 1 22"
    )
    assertEquals(capablancaBadPieceWindow.head.playedUci, "e5d6")

    val karpovAnderssonIqpWindow =
      PgnAnalysisHelper
        .extractPlyDataStrict(karpovAnderssonIqp.pgn)
        .fold(err => fail(s"${karpovAnderssonIqp.id} PGN did not parse strictly: $err"), identity)
        .filter(ply => karpovAnderssonIqp.candidatePlyRange.contains(ply.ply))
    assertEquals(karpovAnderssonIqpWindow.map(_.ply), List(49))
    assertEquals(karpovAnderssonIqpWindow.head.fen, "bq1rrbk1/3n1pp1/pp2pn1p/3p4/2P1P3/P1N1BP2/1P1NBQPP/2RR3K w - - 0 25")
    assertEquals(karpovAnderssonIqpWindow.head.playedUci, "c4d5")

    val botvinnikWindow =
      PgnAnalysisHelper
        .extractPlyDataStrict(botvinnik.pgn)
        .fold(err => fail(s"${botvinnik.id} PGN did not parse strictly: $err"), identity)
        .filter(ply => botvinnik.candidatePlyRange.contains(ply.ply))
    assertEquals(botvinnikWindow.map(_.ply), List(31))
    assertEquals(botvinnikWindow.head.fen, "r2q1rk1/pp2bppp/4pn2/3bN1B1/1n1P4/1BN4Q/PP3PPP/3R1RK1 w - - 11 16")
    assertEquals(botvinnikWindow.head.playedUci, "c3d5")

    val botvinnikSimplificationWindow =
      PgnAnalysisHelper
        .extractPlyDataStrict(botvinnikSimplification.pgn)
        .fold(err => fail(s"${botvinnikSimplification.id} PGN did not parse strictly: $err"), identity)
        .filter(ply => botvinnikSimplification.candidatePlyRange.contains(ply.ply))
    assertEquals(botvinnikSimplificationWindow.map(_.ply), List(31))
    assertEquals(
      botvinnikSimplificationWindow.head.fen,
      "r2q1rk1/pp2bppp/4pn2/3bN1B1/1n1P4/1BN4Q/PP3PPP/3R1RK1 w - - 11 16"
    )
    assertEquals(botvinnikSimplificationWindow.head.playedUci, "c3d5")

    val botvinnikOpeningWindow =
      PgnAnalysisHelper
        .extractPlyDataStrict(botvinnikOpening.pgn)
        .fold(err => fail(s"${botvinnikOpening.id} PGN did not parse strictly: $err"), identity)
        .filter(ply => botvinnikOpening.candidatePlyRange.contains(ply.ply))
    assertEquals(botvinnikOpeningWindow.map(_.ply), List(16))
    assertEquals(botvinnikOpeningWindow.head.fen, "r1bq1rk1/pp1nbppp/4pn2/2pp2B1/2PP4/2NBPN2/PP3PPP/R2Q1RK1 b - - 1 8")
    assertEquals(botvinnikOpeningWindow.head.playedUci, "c5d4")

    val botvinnikFlankClampWindow =
      PgnAnalysisHelper
        .extractPlyDataStrict(botvinnikFlankClamp.pgn)
        .fold(err => fail(s"${botvinnikFlankClamp.id} PGN did not parse strictly: $err"), identity)
        .filter(ply => botvinnikFlankClamp.candidatePlyRange.contains(ply.ply))
    assertEquals(botvinnikFlankClampWindow.map(_.ply), List(25))
    assertEquals(
      botvinnikFlankClampWindow.head.fen,
      "r2q1rk1/pp1bbppp/4pn2/3n2B1/3P4/1BNQ1N2/PP3PPP/R4RK1 w - - 5 13"
    )
    assertEquals(botvinnikFlankClampWindow.head.playedUci, "f3e5")

    val botvinnikE4ColorComplexWindow =
      PgnAnalysisHelper
        .extractPlyDataStrict(botvinnikE4ColorComplex.pgn)
        .fold(err => fail(s"${botvinnikE4ColorComplex.id} PGN did not parse strictly: $err"), identity)
        .filter(ply => botvinnikE4ColorComplex.candidatePlyRange.contains(ply.ply))
    assertEquals(botvinnikE4ColorComplexWindow.map(_.ply), List(30))
    assertEquals(
      botvinnikE4ColorComplexWindow.head.fen,
      "r2q1rk1/pp2bppp/2b1pn2/4N1B1/1n1P4/1BN4Q/PP3PPP/3R1RK1 b - - 10 15"
    )
    assertEquals(botvinnikE4ColorComplexWindow.head.playedUci, "c6d5")

    val kramnikOpeningWindow =
      PgnAnalysisHelper
        .extractPlyDataStrict(kramnikOpening.pgn)
        .fold(err => fail(s"${kramnikOpening.id} PGN did not parse strictly: $err"), identity)
        .filter(ply => kramnikOpening.candidatePlyRange.contains(ply.ply))
    assertEquals(kramnikOpeningWindow.map(_.ply), List(14))
    assertEquals(kramnikOpeningWindow.head.fen, "rnbqkb1r/1p3ppp/p3pn2/2p5/3P4/1B2PN2/PP3PPP/RNBQ1RK1 b kq - 1 7")
    assertEquals(kramnikOpeningWindow.head.playedUci, "c5d4")

    val alekhineWindow =
      PgnAnalysisHelper
        .extractPlyDataStrict(alekhine.pgn)
        .fold(err => fail(s"${alekhine.id} PGN did not parse strictly: $err"), identity)
        .filter(ply => alekhine.candidatePlyRange.contains(ply.ply))
    assertEquals(alekhineWindow.map(_.ply), List(20))
    assertEquals(alekhineWindow.head.fen, "rnb1k2r/pp3ppp/4p3/2pqP3/PbpPn3/2N2N2/1PQ1BPPP/R1B2RK1 b kq - 1 10")
    assertEquals(alekhineWindow.head.playedUci, "e4c3")

    val najdorfWindow =
      PgnAnalysisHelper
        .extractPlyDataStrict(najdorf.pgn)
        .fold(err => fail(s"${najdorf.id} PGN did not parse strictly: $err"), identity)
        .filter(ply => najdorf.candidatePlyRange.contains(ply.ply))
    assertEquals(najdorfWindow.map(_.ply), List(23))
    assertEquals(najdorfWindow.head.fen, "r1b2rk1/pp2qppp/4p3/2nn4/3N4/2N1P3/PPQ2PPP/3RKB1R w K - 0 12")
    assertEquals(najdorfWindow.head.playedUci, "c3d5")

    val carlsenQueenTradeWindow =
      PgnAnalysisHelper
        .extractPlyDataStrict(carlsenQueenTrade.pgn)
        .fold(err => fail(s"${carlsenQueenTrade.id} PGN did not parse strictly: $err"), identity)
        .filter(ply => carlsenQueenTrade.candidatePlyRange.contains(ply.ply))
    assertEquals(carlsenQueenTradeWindow.map(_.ply), List(15))
    assertEquals(
      carlsenQueenTradeWindow.head.fen,
      "r1bqk2r/1p1p1ppp/p1n1pn2/8/1bPNP3/2NQ4/PP3PPP/R1B1KB1R w KQkq - 5 8"
    )
    assertEquals(carlsenQueenTradeWindow.head.playedUci, "d4c6")

    val carlsenQueenTradeCompletionWindow =
      PgnAnalysisHelper
        .extractPlyDataStrict(carlsenQueenTradeCompletion.pgn)
        .fold(err => fail(s"${carlsenQueenTradeCompletion.id} PGN did not parse strictly: $err"), identity)
        .filter(ply => carlsenQueenTradeCompletion.candidatePlyRange.contains(ply.ply))
    assertEquals(carlsenQueenTradeCompletionWindow.map(_.ply), List(17))
    assertEquals(
      carlsenQueenTradeCompletionWindow.head.fen,
      "r1bqk2r/1p3ppp/p1p1pn2/8/1bP1P3/2NQ4/PP3PPP/R1B1KB1R w KQkq - 0 9"
    )
    assertEquals(carlsenQueenTradeCompletionWindow.head.playedUci, "d3d8")

    val salovSimplificationWindow =
      PgnAnalysisHelper
        .extractPlyDataStrict(salovSimplification.pgn)
        .fold(err => fail(s"${salovSimplification.id} PGN did not parse strictly: $err"), identity)
        .filter(ply => salovSimplification.candidatePlyRange.contains(ply.ply))
    assertEquals(salovSimplificationWindow.map(_.ply), List(71))
    assertEquals(salovSimplificationWindow.head.fen, "7k/p4qp1/8/1Q1pR3/3P1P2/2r3P1/7P/6K1 w - - 0 36")
    assertEquals(salovSimplificationWindow.head.playedUci, "b5d5")

    val boleslavskyStaticWeaknessWindow =
      PgnAnalysisHelper
        .extractPlyDataStrict(boleslavskyStaticWeakness.pgn)
        .fold(err => fail(s"${boleslavskyStaticWeakness.id} PGN did not parse strictly: $err"), identity)
        .filter(ply => boleslavskyStaticWeakness.candidatePlyRange.contains(ply.ply))
    assertEquals(boleslavskyStaticWeaknessWindow.map(_.ply), List(19))
    assertEquals(boleslavskyStaticWeaknessWindow.head.fen, "rnbqr1k1/pp3pbp/3p1np1/2pP4/4P3/2N2N2/PP2BPPP/R1BQ1RK1 w - - 6 10")
    assertEquals(boleslavskyStaticWeaknessWindow.head.playedUci, "f3d2")

    val madernaStaticWeaknessWindow =
      PgnAnalysisHelper
        .extractPlyDataStrict(madernaStaticWeakness.pgn)
        .fold(err => fail(s"${madernaStaticWeakness.id} PGN did not parse strictly: $err"), identity)
        .filter(ply => madernaStaticWeakness.candidatePlyRange.contains(ply.ply))
    assertEquals(madernaStaticWeaknessWindow.map(_.ply), List(17))
    assertEquals(madernaStaticWeaknessWindow.head.fen, "rnbq1rk1/pp3pbp/3p1np1/2pP4/4P3/2N2N2/PP2BPPP/R1BQK2R w KQ - 4 9")
    assertEquals(madernaStaticWeaknessWindow.head.playedUci, "f3d2")

    val aronianDefenderTradeWindow =
      PgnAnalysisHelper
        .extractPlyDataStrict(aronianDefenderTrade.pgn)
        .fold(err => fail(s"${aronianDefenderTrade.id} PGN did not parse strictly: $err"), identity)
        .filter(ply => aronianDefenderTrade.candidatePlyRange.contains(ply.ply))
    assertEquals(aronianDefenderTradeWindow.map(_.ply), List(33))
    assertEquals(aronianDefenderTradeWindow.head.playedUci, "c1a3")
    assertEquals(
      aronianDefenderTradeWindow.head.fen,
      "3k1b1r/p2b1ppp/1n3n2/4p3/8/1R4P1/P1QPqPBP/2B2RK1 w - - 0 17"
    )

    val badPieceLiquidationWindow =
      PgnAnalysisHelper
        .extractPlyDataStrict(badPieceLiquidation.pgn)
        .fold(err => fail(s"${badPieceLiquidation.id} PGN did not parse strictly: $err"), identity)
        .filter(ply => badPieceLiquidation.candidatePlyRange.contains(ply.ply))
    assertEquals(badPieceLiquidationWindow.map(_.ply), List(1))
    assertEquals(badPieceLiquidationWindow.head.playedUci, "c1a3")
    assertEquals(
      badPieceLiquidationWindow.head.fen,
      "5b2/4k1pp/8/8/3P4/1R2P3/P4PPP/2B3K1 w - - 0 1"
    )

    val karpovUnzickerWindow =
      PgnAnalysisHelper
        .extractPlyDataStrict(karpovUnzickerBreakPrevention.pgn)
        .fold(err => fail(s"${karpovUnzickerBreakPrevention.id} PGN did not parse strictly: $err"), identity)
        .filter(ply => karpovUnzickerBreakPrevention.candidatePlyRange.contains(ply.ply))
    assertEquals(karpovUnzickerWindow.map(_.ply), (31 to 47).toList)
    assertEquals(karpovUnzickerWindow.head.fen, "1rbn1rk1/2q1bppp/3p1n2/1ppPp3/4P3/2P2N1P/1PBN1PP1/R1BQR1K1 w - - 0 16")
    assertEquals(karpovUnzickerWindow.head.playedUci, "b2b4")
    assertEquals(karpovUnzickerWindow.last.fen, "r1rq1bk1/1n1b1p1p/3p1np1/1p1Pp3/1Pp1P3/2P1BNNP/R2Q1PP1/1B2R1K1 w - - 2 24")
    assertEquals(karpovUnzickerWindow.last.playedUci, "e3a7")

    val karpovAnderssonWindow =
      PgnAnalysisHelper
        .extractPlyDataStrict(karpovAnderssonBreakScreen.pgn)
        .fold(err => fail(s"${karpovAnderssonBreakScreen.id} PGN did not parse strictly: $err"), identity)
        .filter(ply => karpovAnderssonBreakScreen.candidatePlyRange.contains(ply.ply))
    assertEquals(karpovAnderssonWindow.map(_.ply), (47 to 50).toList)
    assertEquals(karpovAnderssonWindow.head.fen, "bq1rrbk1/3n1pp1/pp1ppn1p/8/2P1P3/2N1BP2/PP1NBQPP/2RR3K w - - 6 24")
    assertEquals(karpovAnderssonWindow.head.playedUci, "a2a3")
    assertEquals(karpovAnderssonWindow.last.fen, "bq1rrbk1/3n1pp1/pp2pn1p/3P4/4P3/P1N1BP2/1P1NBQPP/2RR3K b - - 0 25")
    assertEquals(karpovAnderssonWindow.last.playedUci, "e6d5")

    val lokvencBreakWindow =
      PgnAnalysisHelper
        .extractPlyDataStrict(lokvencBreakPrevention.pgn)
        .fold(err => fail(s"${lokvencBreakPrevention.id} PGN did not parse strictly: $err"), identity)
        .filter(ply => lokvencBreakPrevention.candidatePlyRange.contains(ply.ply))
    assertEquals(lokvencBreakWindow.map(_.ply), List(23))
    assertEquals(lokvencBreakWindow.head.fen, "r1bqr1k1/p4pbp/np1p1np1/2pP4/4P3/2N2N2/PPQ1BPPP/R1B1R1K1 w - - 2 12")
    assertEquals(lokvencBreakWindow.head.playedUci, "e2b5")

    val madernaBreakWindow =
      PgnAnalysisHelper
        .extractPlyDataStrict(madernaBreakPrevention.pgn)
        .fold(err => fail(s"${madernaBreakPrevention.id} PGN did not parse strictly: $err"), identity)
        .filter(ply => madernaBreakPrevention.candidatePlyRange.contains(ply.ply))
    assertEquals(madernaBreakWindow.map(_.ply), List(29))
    assertEquals(madernaBreakWindow.head.fen, "1rbqr1k1/1p1n1pbp/pn1p2p1/2pP4/P3PP2/2N2B2/1P1N2PP/R1BQR1K1 w - - 5 15")
    assertEquals(madernaBreakWindow.head.playedUci, "a4a5")

    val madernaCentralBreakWindow =
      PgnAnalysisHelper
        .extractPlyDataStrict(madernaCentralBreak.pgn)
        .fold(err => fail(s"${madernaCentralBreak.id} PGN did not parse strictly: $err"), identity)
        .filter(ply => madernaCentralBreak.candidatePlyRange.contains(ply.ply))
    assertEquals(madernaCentralBreakWindow.map(_.ply), List(33))
    assertEquals(madernaCentralBreakWindow.head.fen, "nrb1r1k1/1pqn1pbp/p2p2p1/P1pP4/2N1PP2/2N2B2/1P4PP/R1BQR1K1 w - - 3 17")
    assertEquals(madernaCentralBreakWindow.head.playedUci, "e4e5")

    val madernaCentralBreakPrepWindow =
      PgnAnalysisHelper
        .extractPlyDataStrict(madernaCentralBreakPrep.pgn)
        .fold(err => fail(s"${madernaCentralBreakPrep.id} PGN did not parse strictly: $err"), identity)
        .filter(ply => madernaCentralBreakPrep.candidatePlyRange.contains(ply.ply))
    assertEquals(madernaCentralBreakPrepWindow.map(_.ply), List(31))
    assertEquals(madernaCentralBreakPrepWindow.head.fen, "nrbqr1k1/1p1n1pbp/p2p2p1/P1pP4/4PP2/2N2B2/1P1N2PP/R1BQR1K1 w - - 1 16")
    assertEquals(madernaCentralBreakPrepWindow.head.playedUci, "d2c4")

    val camaraBreakWindow =
      PgnAnalysisHelper
        .extractPlyDataStrict(camaraBreakPrevention.pgn)
        .fold(err => fail(s"${camaraBreakPrevention.id} PGN did not parse strictly: $err"), identity)
        .filter(ply => camaraBreakPrevention.candidatePlyRange.contains(ply.ply))
    assertEquals(camaraBreakWindow.map(_.ply), List(27))
    assertEquals(camaraBreakWindow.head.fen, "1rbqr1k1/pp1n1pbp/3p2p1/2pP4/1n2PP2/2NB3P/PP2N1P1/R1BQ1R1K w - - 3 14")
    assertEquals(camaraBreakWindow.head.playedUci, "d3b5")

    val camaraD5ColorComplexWindow =
      PgnAnalysisHelper
        .extractPlyDataStrict(camaraD5ColorComplex.pgn)
        .fold(err => fail(s"${camaraD5ColorComplex.id} PGN did not parse strictly: $err"), identity)
        .filter(ply => camaraD5ColorComplex.candidatePlyRange.contains(ply.ply))
    assertEquals(camaraD5ColorComplexWindow.map(_.ply), List(27))
    assertEquals(
      camaraD5ColorComplexWindow.head.fen,
      "1rbqr1k1/pp1n1pbp/3p2p1/2pP4/1n2PP2/2NB3P/PP2N1P1/R1BQ1R1K w - - 3 14"
    )
    assertEquals(camaraD5ColorComplexWindow.head.playedUci, "d3b5")

    val sliwaBreakWindow =
      PgnAnalysisHelper
        .extractPlyDataStrict(sliwaBreakPrevention.pgn)
        .fold(err => fail(s"${sliwaBreakPrevention.id} PGN did not parse strictly: $err"), identity)
        .filter(ply => sliwaBreakPrevention.candidatePlyRange.contains(ply.ply))
    assertEquals(sliwaBreakWindow.map(_.ply), List(55))
    assertEquals(sliwaBreakWindow.head.fen, "1r1r3k/1p1q1pbp/pn1p2p1/2pP4/Pn2PP2/NQ4PP/1P3B2/3RRBK1 w - - 2 28")
    assertEquals(sliwaBreakWindow.head.playedUci, "a4a5")

    val luckisBreakWindow =
      PgnAnalysisHelper
        .extractPlyDataStrict(luckisBreakPrevention.pgn)
        .fold(err => fail(s"${luckisBreakPrevention.id} PGN did not parse strictly: $err"), identity)
        .filter(ply => luckisBreakPrevention.candidatePlyRange.contains(ply.ply))
    assertEquals(luckisBreakWindow.map(_.ply), List(33))
    assertEquals(luckisBreakWindow.head.fen, "2r1r1k1/1pqn1pbp/p2p1np1/3P4/P1p1PB2/2N4P/1PQ1BPP1/R3R1K1 w - - 2 17")
    assertEquals(luckisBreakWindow.head.playedUci, "a4a5")

    val pflegerBreakWindow =
      PgnAnalysisHelper
        .extractPlyDataStrict(pflegerBreakPrevention.pgn)
        .fold(err => fail(s"${pflegerBreakPrevention.id} PGN did not parse strictly: $err"), identity)
        .filter(ply => pflegerBreakPrevention.candidatePlyRange.contains(ply.ply))
    assertEquals(pflegerBreakWindow.map(_.ply), List(33))
    assertEquals(pflegerBreakWindow.head.fen, "r2qr1k1/1p3pb1/pn1p1npp/2pP4/P3P3/2NQ1N2/1P1B1PPP/R3R1K1 w - - 0 17")
    assertEquals(pflegerBreakWindow.head.playedUci, "a4a5")

    val pflegerD5ColorComplexWindow =
      PgnAnalysisHelper
        .extractPlyDataStrict(pflegerD5ColorComplex.pgn)
        .fold(err => fail(s"${pflegerD5ColorComplex.id} PGN did not parse strictly: $err"), identity)
        .filter(ply => pflegerD5ColorComplex.candidatePlyRange.contains(ply.ply))
    assertEquals(pflegerD5ColorComplexWindow.map(_.ply), List(33))
    assertEquals(
      pflegerD5ColorComplexWindow.head.fen,
      "r2qr1k1/1p3pb1/pn1p1npp/2pP4/P3P3/2NQ1N2/1P1B1PPP/R3R1K1 w - - 0 17"
    )
    assertEquals(pflegerD5ColorComplexWindow.head.playedUci, "a4a5")

    val polugaevskyBreakWindow =
      PgnAnalysisHelper
        .extractPlyDataStrict(polugaevskyBreakPrevention.pgn)
        .fold(err => fail(s"${polugaevskyBreakPrevention.id} PGN did not parse strictly: $err"), identity)
        .filter(ply => polugaevskyBreakPrevention.candidatePlyRange.contains(ply.ply))
    assertEquals(polugaevskyBreakWindow.map(_.ply), List(23))
    assertEquals(polugaevskyBreakWindow.head.fen, "rnbqnrk1/5ppp/pp1p1b2/2pP4/P3P3/2N5/1P1NBPPP/R1BQ1RK1 w - - 0 12")
    assertEquals(polugaevskyBreakWindow.head.playedUci, "d2c4")
  }
