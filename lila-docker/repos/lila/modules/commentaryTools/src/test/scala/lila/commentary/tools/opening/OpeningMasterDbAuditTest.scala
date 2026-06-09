package lila.commentary.tools.opening

import munit.FunSuite

class OpeningMasterDbAuditTest extends FunSuite:

  test("extractYear matches valid 4-digit years and extracts them") {
    assertEquals(OpeningMasterDbAudit.extractYear("2015"), Some("2015"))
    assertEquals(OpeningMasterDbAudit.extractYear("2020-05-12"), Some("2020"))
    assertEquals(OpeningMasterDbAudit.extractYear("since 1952"), Some("1952"))
    assertEquals(OpeningMasterDbAudit.extractYear("  1984  "), Some("1984"))
    assertEquals(OpeningMasterDbAudit.extractYear("invalid"), None)
    assertEquals(OpeningMasterDbAudit.extractYear("123"), None)
    assertEquals(OpeningMasterDbAudit.extractYear("12345"), None)
  }

  test("mastersRequest filters query parameters with clean years") {
    val row =
      OpeningPoolAudit.AuditedRow(
        lineNo = 1,
        eco = "A00",
        name = "Uncommon Opening",
        pgn = "1. a3",
        endpointKey = Some("a3_endpoint"),
        uciPlay = List("a2a3"),
        plyCount = 1,
        issues = Nil
      )

    val req1 =
      OpeningMasterDbAudit.mastersRequest(
        row = row,
        mode = OpeningMasterDbAudit.QueryMode.Fen,
        since = Some("2015-06-01"),
        until = Some("2020")
      )

    // Expected query params: fen=a3_endpoint+0+1&moves=0&topGames=3&since=2015&until=2020
    assert(req1.url.contains("since=2015"))
    assert(req1.url.contains("until=2020"))

    val req2 =
      OpeningMasterDbAudit.mastersRequest(
        row = row,
        mode = OpeningMasterDbAudit.QueryMode.Fen,
        since = Some("invalid-date"),
        until = None
      )

    // Expected query params should omit since parameter entirely if it's invalid
    assert(!req2.url.contains("since"))
    assert(!req2.url.contains("until"))
  }
