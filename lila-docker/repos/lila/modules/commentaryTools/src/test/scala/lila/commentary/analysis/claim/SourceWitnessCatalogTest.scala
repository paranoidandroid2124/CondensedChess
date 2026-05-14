package lila.commentary.analysis.claim

import lila.commentary.tools.claim.*

import lila.commentary.analysis.*
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
        "source-boleslavsky-nezhmetdinov-1950-static-weakness-fixation",
        "source-maderna-palermo-1955-static-weakness-fixation",
        "source-aronian-andreikin-2014-defender-trade",
        "source-capablanca-golombek-1939",
        "source-capablanca-golombek-1939-iqp-inducement",
        "source-botvinnik-vidmar-1936",
        "source-botvinnik-vidmar-1936-iqp-multipv-screen",
        "source-botvinnik-vidmar-1936-iqp-opening-inducement",
        "source-kramnik-anand-2001",
        "source-kramnik-anand-2001-iqp-opening-inducement",
        "source-tartakower-capablanca-1924",
        "source-alekhine-bogoljubow-1936-iqp-inducement",
        "source-salov-ljubojevic-1992-simplification-window",
        "source-najdorf-sergeant-1939-iqp-inducement",
        "source-carlsen-anand-2014-g6"
      )
    )
    assertEquals(rows.map(_.id).distinct.size, rows.size)
    assert(rows.forall(_.sourceUrl.startsWith("https://")), clues(rows.map(_.sourceUrl)))
    assert(rows.forall(_.gameName.nonEmpty), clues(rows))
    assert(rows.forall(_.family.nonEmpty), clues(rows))
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
    val botvinnik = byId("source-botvinnik-vidmar-1936-iqp-multipv-screen")
    val botvinnikOpening = byId("source-botvinnik-vidmar-1936-iqp-opening-inducement")
    val kramnikOpening = byId("source-kramnik-anand-2001-iqp-opening-inducement")
    val alekhine = byId("source-alekhine-bogoljubow-1936-iqp-inducement")
    val najdorf = byId("source-najdorf-sergeant-1939-iqp-inducement")
    val salovSimplification = byId("source-salov-ljubojevic-1992-simplification-window")
    val boleslavskyStaticWeakness = byId("source-boleslavsky-nezhmetdinov-1950-static-weakness-fixation")
    val madernaStaticWeakness = byId("source-maderna-palermo-1955-static-weakness-fixation")
    val aronianDefenderTrade = byId("source-aronian-andreikin-2014-defender-trade")

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

    val botvinnikWindow =
      PgnAnalysisHelper
        .extractPlyDataStrict(botvinnik.pgn)
        .fold(err => fail(s"${botvinnik.id} PGN did not parse strictly: $err"), identity)
        .filter(ply => botvinnik.candidatePlyRange.contains(ply.ply))
    assertEquals(botvinnikWindow.map(_.ply), List(31))
    assertEquals(botvinnikWindow.head.fen, "r2q1rk1/pp2bppp/4pn2/3bN1B1/1n1P4/1BN4Q/PP3PPP/3R1RK1 w - - 11 16")
    assertEquals(botvinnikWindow.head.playedUci, "c3d5")

    val botvinnikOpeningWindow =
      PgnAnalysisHelper
        .extractPlyDataStrict(botvinnikOpening.pgn)
        .fold(err => fail(s"${botvinnikOpening.id} PGN did not parse strictly: $err"), identity)
        .filter(ply => botvinnikOpening.candidatePlyRange.contains(ply.ply))
    assertEquals(botvinnikOpeningWindow.map(_.ply), List(16))
    assertEquals(botvinnikOpeningWindow.head.fen, "r1bq1rk1/pp1nbppp/4pn2/2pp2B1/2PP4/2NBPN2/PP3PPP/R2Q1RK1 b - - 1 8")
    assertEquals(botvinnikOpeningWindow.head.playedUci, "c5d4")

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
  }
