package lila.commentary.root

class RootBroadAuditContractTest extends munit.FunSuite:

  private val rows = RootExpectationCorpus.loadAll()

  private def normalizeSnapshot(value: String): String =
    value.replace("\r\n", "\n").stripSuffix("\n")

  test("root coverage matrix snapshot stays synchronized with the renderer"):
    val rendered = normalizeSnapshot(RootCoverageMatrix.render(rows))
    val artifact = normalizeSnapshot(RootCoverageMatrix.loadArtifact())

    assertEquals(rendered, artifact)
