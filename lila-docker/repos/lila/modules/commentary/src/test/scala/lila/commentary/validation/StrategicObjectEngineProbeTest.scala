package lila.commentary.validation

class StrategicObjectEngineProbeTest extends munit.FunSuite:

  private val objectRows = ObjectExpectationCorpus.loadAll().map(row => row.id -> row).toMap
  private val probeRows =
    EngineProbeExpectationCorpus.loadAll().filter(_.resolvedLayer == "object")

  test("Object 7 corpus positions survive a local Stockfish tactical smoke check"):
    assert(java.nio.file.Files.isRegularFile(StockfishProbe.enginePath))
    assertEquals(probeRows.map(_.id).sorted, ObjectExpectationCorpus.loadAll().map(_.id).sorted)

    probeRows.foreach: probeRow =>
      val objectRow = objectRows.getOrElse(probeRow.id, fail(s"Missing object row for ${probeRow.id}"))
      val probe = StockfishProbe.probeFen(objectRow.fen)
      assert(probe.bestMove.nonEmpty, s"${probeRow.id} returned no best move")
      assert(probe.cp.nonEmpty || probe.mate.nonEmpty, s"${probeRow.id} returned no score line")
      assert(
        probe.mate.forall(mate => math.abs(mate) > probeRow.maxMatePly),
        s"${probeRow.id} collapses into a too-short mate according to Stockfish: ${probe.rawInfo.getOrElse("")}"
      )
      probeRow.maxAbsCp.foreach: maxAbsCp =>
        assert(
          probe.cp.forall(cp => math.abs(cp) <= maxAbsCp),
          s"${probeRow.id} exceeds the eval sanity bound $maxAbsCp: ${probe.rawInfo.getOrElse("")}"
        )
