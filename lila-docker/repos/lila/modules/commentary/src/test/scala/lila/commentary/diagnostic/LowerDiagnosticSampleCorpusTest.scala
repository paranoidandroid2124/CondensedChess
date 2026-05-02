package lila.commentary.diagnostic

class LowerDiagnosticSampleCorpusTest extends munit.FunSuite:

  private val rows = LowerDiagnosticSampleCorpus.loadAll()

  test("lower diagnostic sample has exact anti-cases and positive move-local cases"):
    assert(rows.exists(_.expectedTacticalVerdict == "standing_tactical_only"))
    assert(rows.exists(_.expectedTacticalVerdict == "move_local_tactical_claim"))
    assert(rows.exists(_.expectedTacticalVerdict == "no_tactical_root"))

  rows.foreach: row =>
    test(s"lower diagnostic trace classifies ${row.id}"):
      val trace = LowerLayerDiagnostic.trace(row.input)

      assertEquals(trace.identity.nodeId, row.nodeId)
      assertEquals(trace.identity.ply, row.ply)
      assertEquals(trace.tacticalVerdict, row.expectedTacticalVerdict)
      assertEquals(trace.preRendererVerdict, row.expectedPreRendererVerdict)
      row.expectedBreaks.foreach: expectedBreak =>
        assert(trace.breaks.contains(expectedBreak), clues(trace))

  test("lower diagnostic summary exposes current pre-renderer breakpoints"):
    val summary = LowerDiagnosticSummary.from(rows.map(row => LowerLayerDiagnostic.trace(row.input)))

    assertEquals(summary.total, rows.size)
    assertEquals(summary.tacticalVerdicts("standing_tactical_only"), 4)
    assertEquals(summary.tacticalVerdicts("move_local_tactical_claim"), 2)
    assertEquals(summary.tacticalVerdicts("no_tactical_root"), 2)
    assertEquals(summary.breakCounts("admission:standing_tactical_only"), 4)
    assertEquals(summary.breakCounts("selection:no_move_local_tactical_lead"), 5)
    assertEquals(summary.breakCounts("selection:no_lead"), 1)
