package lila.commentary.analysis.strategic

import lila.commentary.model.strategic.VariationLine
import lila.commentary.tools.claim.SourceReview
import lila.commentary.tools.claim.SourceWitnessCatalog

final class BreakClampSourceTriageTest extends munit.FunSuite:

  private val cleanPgn =
    """[Event "Clean clamp"]
      |[Site "?"]
      |[Date "2026.01.01"]
      |[Round "?"]
      |[White "White"]
      |[Black "Black"]
      |[Result "*"]
      |[FEN "4k3/8/8/1p6/8/2P5/1P6/4K3 w - - 0 1"]
      |[SetUp "1"]
      |
      |1. b4 Ke7 2. Ke2 Ke6 *
      |""".stripMargin

  private def sourceGame(id: String = "game-clean", label: String = "Clean clamp") =
    BreakClampCandidateScanner.SourceGame(id = id, label = label, pgn = cleanPgn)

  private def cleanRow(
      gameId: String = "game-clean",
      ply: Int = 1,
      routeId: String = "black:a6-a5:quiet_push",
      routeToken: String = "...a6-a5",
      destinationToken: String = "...a5",
      playedUci: String = "b2b4",
      engineGate: String = BreakClampCandidateScanner.EngineGate.SourceMoveTopPv
  ) =
    BreakClampCandidateScanner.ScanRow(
      gameId = gameId,
      label = "Clean clamp",
      ply = ply,
      fen = "4k3/8/8/1p6/8/2P5/1P6/4K3 w - - 0 1",
      playedUci = playedUci,
      engineGate = engineGate,
      routeId = routeId,
      routeToken = routeToken,
      destinationToken = destinationToken,
      transformRisk = "None",
      transformRoutes = "",
      transformVerdicts = "",
      blockers = "",
      selectedEnginePv = List("b2b4", "e8e7", "e1e2", "e7e6"),
      suggestedSourceId = s"source-$gameId-break-prevention-ply-$ply",
      suggestedPlyRange = s"$ply-$ply",
      classification = BreakClampCandidateScanner.Classification.CleanRouteClamp
    )

  private final class FixedScannerEngine(lines: List[VariationLine])
      extends BreakClampCandidateScanner.Engine:
    override def newGame(): Unit = ()
    override def analyze(fen: String, depth: Int, multiPv: Int): List[VariationLine] = lines

  private final class FixedReviewEngine(linesByFen: Map[String, List[VariationLine]])
      extends SourceReview.SourceReviewEngine:
    override def newGame(): Unit = ()
    override def analyze(fen: String, depth: Int, multiPv: Int): List[VariationLine] =
      linesByFen.getOrElse(fen, Nil)

  test("ranks black quiet named-break clean rows above opening, capture, and white-route noise") {
    val games = List(sourceGame()).map(game => game.id -> game).toMap
    val rows = List(
      cleanRow(
        routeId = "black:d7-d5:quiet_push",
        routeToken = "...d7-d5",
        destinationToken = "...d5",
        playedUci = "d4d5",
        ply = 5
      ),
      cleanRow(
        routeId = "black:c5-d4:capture_break",
        routeToken = "...c5-d4",
        destinationToken = "...d4",
        playedUci = "b2b4",
        ply = 23
      ),
      cleanRow(
        routeId = "white:g2-g4:quiet_push",
        routeToken = "g2-g4",
        destinationToken = "g4",
        playedUci = "a7a5",
        ply = 24
      ),
      cleanRow(ply = 31)
    )

    val ranked =
      BreakClampSourceTriage.rankCandidates(rows, games, sourceUrl = "https://example.invalid/source.pgn", maxCandidates = 10)

    assertEquals(ranked.head.row.routeId, "black:a6-a5:quiet_push")
    assert(ranked.head.scoreReasons.contains("black_quiet_named_break"), clues(ranked.head.scoreReasons))
  }

  test("deduplicates multiple clean routes from the same game and ply") {
    val games = List(sourceGame()).map(game => game.id -> game).toMap
    val rows = List(
      cleanRow(routeId = "black:c5-d4:capture_break", routeToken = "...c5-d4", destinationToken = "...d4", ply = 17),
      cleanRow(routeId = "black:a6-a5:quiet_push", routeToken = "...a6-a5", destinationToken = "...a5", ply = 17)
    )

    val ranked =
      BreakClampSourceTriage.rankCandidates(rows, games, sourceUrl = "https://example.invalid/source.pgn", maxCandidates = 10)

    assertEquals(ranked.size, 1)
    assertEquals(ranked.head.row.routeId, "black:a6-a5:quiet_push")
  }

  test("builds a transient A:break_prevention source candidate with an exact one-ply range") {
    val row = cleanRow(ply = 19)
    val candidate =
      BreakClampSourceTriage.sourceCandidateFor(row, sourceGame(), sourceUrl = "https://example.invalid/source.pgn")

    assertEquals(candidate.id, "source-game-clean-break-prevention-ply-19")
    assertEquals(candidate.reviewGroup, "A:break_prevention")
    assertEquals(candidate.candidatePlyRange.start, 19)
    assertEquals(candidate.candidatePlyRange.end, 19)
    assertEquals(candidate.sourceUrl, "https://example.invalid/source.pgn")
  }

  test("dry-runs a transient admitted row through SourceReview as neutralize_key_break") {
    val catalogRow =
      SourceWitnessCatalog.all.find(_.id == "source-pfleger-maalouf-1961-a6-a5-break-prevention").get
    val transient = catalogRow.copy(id = "source-transient-pfleger-a6-a5-break-prevention")
    val fen = "r2qr1k1/1p3pb1/pn1p1npp/2pP4/P3P3/2NQ1N2/1P1B1PPP/R3R1K1 w - - 0 17"
    val pv =
      "a4a5 b6d7 c3a4 f6g4 d2c3 g4e5 f3e5 g7e5 c3e5 d7e5 d3g3 d8g5 a4b6 g5g3 h2g3 a8d8 f2f4 e5d3 e1e3 d3b4 a1d1 b4c2"
    val observation = SourceReview
      .observationsForSources(
        sources = List(transient),
        engine = Some(FixedReviewEngine(Map(fen -> List(VariationLine(pv.split(" ").toList, scoreCp = 20, depth = 16))))),
        depth = 16,
        multiPv = 3
      )
      .head

    assertEquals(observation.verdict, SourceReview.Verdict.AdmitAuthorityRow)
    assertEquals(observation.mainProofSource, "counterplay_axis_suppression")
    assert(observation.packetSummary.contains("proof_family=neutralize_key_break"), clues(observation.packetSummary))
    assertEquals(observation.primary, "A local reading is that this keeps ...a5 from coming right away.")
    assertEquals(observation.primary, observation.bookmaker)
    assertEquals(observation.primary, observation.chronicle)
  }

  test("reports non-admitted triage rows with their exact SourceReview blockers") {
    val scanLine = VariationLine(List("b2b4", "e8e7", "e1e2", "e7e6"), scoreCp = 10, depth = 12)
    val report = BreakClampSourceTriage.triage(
      games = List(sourceGame()),
      scannerEngine = Some(FixedScannerEngine(List(scanLine))),
      reviewEngine = None,
      config = BreakClampSourceTriage.TriageConfig(maxCandidates = 4)
    )

    assertEquals(report.rows.size, 1)
    assertEquals(report.rows.head.review.admissionBlockers, "engine:missing")
    assert(report.markdown.contains("engine:missing"), clues(report.markdown))
  }
