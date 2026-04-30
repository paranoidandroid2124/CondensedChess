package lila.commentary.diagnostic

class KingAttackProjectionImpactReportTest extends munit.FunSuite:

  test("king attack projection impact blocks missing actual projection evidence"):
    val attackShape =
      row(
        id = "attack-shape",
        fen = "6k1/6pp/8/8/3B4/8/6R1/6K1 w - - 0 1"
      )
    val quiet =
      row(
        id = "quiet",
        fen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1"
      )

    val report = KingAttackProjectionImpactReport.fromRows(Vector(attackShape, quiet))

    assertEquals(report.summary.total, 2)
    assertEquals(report.summary.uniqueFenTotal, 2)
    assertEquals(report.summary.actualBlockedMissingProjectionEvidenceRows, 2)
    assertEquals(report.summary.probeAdmittedRowsByBand.getOrElse("S03", 0), 0, clues(report.summary))
    assertEquals(report.summary.probeAdmittedUniqueFenByBand.getOrElse("S03", 0), 0, clues(report.summary))
    assertEquals(report.summary.probeAdmittedRowsByBand.getOrElse("S01", 0), 0, clues(report.summary))
    assertEquals(report.summary.probeAdmittedRowsByBand.getOrElse("S02", 0), 1, clues(report.summary))

    val attackRow = report.rows.find(_.rowId == "external_candidate:attack-shape").getOrElse(fail("missing attack row"))
    assertEquals(attackRow.actualStatus, "blocked_missing_projection_evidence")
    assert(
      attackRow.probeAdmissions.exists(admission => admission.band == "S02" && admission.owner == "white" && admission.admitted),
      clues(attackRow.probeAdmissions)
    )

    val admittedFenLedger = KingAttackProjectionImpactReport.probeAdmittedFenLedger(report.rows)
    assertEquals(admittedFenLedger.size, 1)
    assertEquals(admittedFenLedger.head.rowCount, 1)
    assertEquals(admittedFenLedger.head.admittedBands, Vector("S02"))
    assertEquals(admittedFenLedger.head.auditConclusion, "diagnostic_probe_admitted_not_actual_public_admission")

  test("king attack projection probe rejects audited geometry-only king attack candidates"):
    val rows =
      Vector(
        row("s02-public-candidate", "5rk1/pn5n/1p1p2p1/2pPrb1p/P1P1p2q/1NP1B2P/3Q1PP1/2RBR1K1 b - - 2 29"),
        row("s02-back-rank-tension", "1r2r1k1/3p1pp1/7p/4q3/PP1R4/8/1Q3PPP/5RK1 b - - 0 29"),
        row("s02-wrong-theater", "3r1rk1/pp2ppbp/1qn3p1/3R1b2/Q7/2P3P1/PP2PPBP/R1B1N1K1 b - - 2 13"),
        row("s02-opening-shape", "r1br2k1/1pq2ppp/p1n1pn2/2b5/P1B1P3/2N2N1P/1P2QPP1/R1B1R1K1 b - - 2 13"),
        row("s03-queenless-activity", "5rk1/1p2n1pp/2n5/1p1b4/r5P1/P3P3/1P1BB2N/2R2RK1 b - - 2 21"),
        row("s03-second-rank-activity", "6k1/5pp1/b6p/4p3/1N4n1/1P2P1P1/3r1PBP/2R3K1 b - - 0 29"),
        row("s03-opening-geometry", "r4rk1/1pqnnp2/2p3pp/p2p1b2/N2P4/3BPN1P/PPQ2PP1/1RR3K1 b - - 7 17")
      )

    val report = KingAttackProjectionImpactReport.fromRows(rows)
    val admittedById =
      report.rows.map(row => row.rowId -> row.probeAdmissions.filter(_.admitted).map(_.band).sorted).toMap

    assertEquals(admittedById("external_candidate:s02-public-candidate"), Vector.empty)
    assertEquals(admittedById("external_candidate:s02-back-rank-tension"), Vector.empty)
    assertEquals(admittedById("external_candidate:s02-wrong-theater"), Vector.empty)
    assertEquals(admittedById("external_candidate:s02-opening-shape"), Vector.empty)
    assertEquals(admittedById("external_candidate:s03-queenless-activity"), Vector.empty)
    assertEquals(admittedById("external_candidate:s03-second-rank-activity"), Vector.empty)
    assertEquals(admittedById("external_candidate:s03-opening-geometry"), Vector.empty)

  private def row(id: String, fen: String): LowerDiagnosticLargeCorpus.Row =
    LowerDiagnosticLargeCorpus.Row(
      id = s"external_candidate:$id",
      sourceFile = "synthetic-materialized.jsonl",
      sourceKind = "external_candidate",
      sourceSchema = None,
      caseType = None,
      expectation = None,
      input = LowerLayerDiagnostic.Input(
        id = s"external_candidate:$id",
        currentFen = fen,
        beforeFen = None,
        playedMove = None,
        nodeId = s"diag-$id",
        ply = 0
      ),
      metadata = Map.empty
    )
