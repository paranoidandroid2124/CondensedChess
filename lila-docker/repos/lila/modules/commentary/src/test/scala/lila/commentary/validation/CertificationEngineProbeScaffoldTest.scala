package lila.commentary.validation

class CertificationEngineProbeScaffoldTest extends munit.FunSuite:

  private val certificationRows =
    CertificationExpectationCorpus.loadAll().map(row => row.id -> row).toMap
  private val probeRows =
    EngineProbeExpectationCorpus.loadAll().filter(_.resolvedLayer == "certification")

  test("certification scaffold engine bundles cover every certification row"):
    assert(java.nio.file.Files.isRegularFile(StockfishProbe.enginePath))
    assertEquals(
      probeRows.map(_.id).sorted,
      CertificationExpectationCorpus.loadAll().map(_.id).sorted
    )

  probeRows.foreach: probeRow =>
    test(s"certification scaffold position stays inside frozen engine/probe budgets for ${probeRow.id}"):
      val certRow =
        certificationRows.getOrElse(probeRow.id, fail(s"Missing certification row for ${probeRow.id}"))
      val probe = StockfishProbe.probeFen(certRow.fen)
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
