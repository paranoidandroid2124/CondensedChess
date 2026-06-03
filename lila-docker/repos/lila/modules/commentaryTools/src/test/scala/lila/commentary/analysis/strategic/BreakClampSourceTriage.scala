package lila.commentary.analysis.strategic

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path, Paths }

import lila.commentary.tools.claim.SourceReview
import lila.commentary.tools.claim.SourceWitnessCatalog

object BreakClampSourceTriage:

  val DefaultSourceUrl = "https://www.pgnmentor.com/openings/ModernBenoni6e4.zip"

  final case class TriageConfig(
      scanConfig: BreakClampCandidateScanner.ScanConfig = BreakClampCandidateScanner.ScanConfig(),
      maxCandidates: Int = 24,
      admitLimit: Int = 1,
      sourceUrl: String = DefaultSourceUrl
  )

  final case class RankedCandidate(
      rank: Int,
      score: Int,
      scoreReasons: List[String],
      row: BreakClampCandidateScanner.ScanRow,
      source: SourceWitnessCatalog.SourceCandidate
  )

  final case class TriageRow(
      ranked: RankedCandidate,
      review: SourceReview.Observation
  ):
    def tsv: String =
      val row = ranked.row
      List(
        ranked.rank.toString,
        ranked.score.toString,
        ranked.scoreReasons.mkString(";"),
        row.gameId,
        row.label,
        row.ply.toString,
        row.fen,
        row.playedUci,
        row.engineGate,
        row.routeId,
        row.routeToken,
        row.destinationToken,
        row.transformRisk,
        row.transformVerdicts,
        row.suggestedSourceId,
        row.suggestedPlyRange,
        review.verdict,
        review.diagnosis,
        review.admissionBlockers,
        review.ownerFailureCodes,
        review.mainProofSource,
        review.mainClaimScope,
        review.packetSummary,
        review.contractId,
        review.contractStatus,
        review.contractFailures,
        review.release,
        review.primary,
        review.moveReview,
        row.selectedEnginePv.mkString(" ")
      ).map(clean).mkString("\t")

  final case class TriageReport(
      scan: BreakClampCandidateScanner.ScanReport,
      ranked: List[RankedCandidate],
      rows: List[TriageRow],
      admitLimit: Int
  ):
    def admittedRows: List[TriageRow] =
      rows.filter(_.review.verdict == SourceReview.Verdict.AdmitAuthorityRow).take(admitLimit)

    def tsv: String =
      (Header :: rows.map(_.tsv)).mkString("\n") + "\n"

    def markdown: String =
      val admitted = admittedRows
      val lines =
        List(
          "# Break Clamp Source Triage",
          "",
          s"summary=games=${scan.totalGames}, scannedPlies=${scan.scannedPlies}, routeEvidencePlies=${scan.routeEvidencePlies}, cleanCandidates=${scan.cleanRows.size}, consideredCandidates=${ranked.size}, triageRows=${rows.size}, admittedRows=${admitted.size}",
          "",
          "## Promotable Rows",
          ""
        ) ++ rowsMarkdown(admitted) ++
          List(
            "",
            "## Triage Rows",
            ""
          ) ++ rowsMarkdown(rows)
      lines.mkString("\n") + "\n"

  def triage(
      games: List[BreakClampCandidateScanner.SourceGame],
      scannerEngine: Option[BreakClampCandidateScanner.Engine],
      reviewEngine: Option[SourceReview.SourceReviewEngine],
      config: TriageConfig = TriageConfig()
  ): TriageReport =
    val scan = BreakClampCandidateScanner.scan(games, scannerEngine, config.scanConfig)
    val gamesById = games.map(game => game.id -> game).toMap
    val ranked = rankCandidates(scan.rows, gamesById, config.sourceUrl, config.maxCandidates)
    val observations =
      SourceReview.observationsForSources(
        ranked.map(_.source),
        reviewEngine,
        depth = config.scanConfig.depth,
        multiPv = config.scanConfig.multiPv
      )
    TriageReport(scan, ranked, ranked.zip(observations).map(TriageRow(_, _)), config.admitLimit)

  private[commentary] def rankCandidates(
      rows: List[BreakClampCandidateScanner.ScanRow],
      games: Map[String, BreakClampCandidateScanner.SourceGame],
      sourceUrl: String,
      maxCandidates: Int
  ): List[RankedCandidate] =
    val scored =
      rows
        .filter(row => row.classification == BreakClampCandidateScanner.Classification.CleanRouteClamp)
        .filter(row =>
          row.engineGate == BreakClampCandidateScanner.EngineGate.SourceMoveTopPv ||
            row.engineGate == BreakClampCandidateScanner.EngineGate.SourceMoveNearTopMultiPv
        )
        .flatMap(row => games.get(row.gameId).map(game => scoredCandidate(row, game, sourceUrl)))

    scored
      .groupBy(candidate => (candidate.row.gameId, candidate.row.ply))
      .values
      .map(_.minBy(candidate => (-candidate.score, engineRank(candidate.row.engineGate), candidate.row.routeId)))
      .toList
      .sortBy(candidate => (-candidate.score, engineRank(candidate.row.engineGate), candidate.row.gameId, candidate.row.ply, candidate.row.routeId))
      .take(maxCandidates)
      .zipWithIndex
      .map { case (candidate, idx) => candidate.copy(rank = idx + 1) }

  private[commentary] def sourceCandidateFor(
      row: BreakClampCandidateScanner.ScanRow,
      game: BreakClampCandidateScanner.SourceGame,
      sourceUrl: String
  ): SourceWitnessCatalog.SourceCandidate =
    SourceWitnessCatalog.SourceCandidate(
      id = row.suggestedSourceId,
      gameName = game.label,
      sourceUrl = sourceUrl,
      pgn = game.pgn,
      candidatePlyRange = SourceWitnessCatalog.CandidatePlyRange(row.ply, row.ply),
      reviewGroup = "A:break_prevention",
      intendedVerdict = SourceReview.Verdict.ScreenOnly,
      validationNote =
        s"Transient clean route-clamp triage candidate: exact ply ${row.ply} ${row.playedUci} denies ${row.routeToken}; discovery only until fixed SourceReview admission."
    )

  def writeArtifacts(report: TriageReport): (Path, Path) =
    val dir = Paths.get("tmp")
    Files.createDirectories(dir)
    val tsvPath = dir.resolve("strategic_break_clamp_source_triage.tsv")
    val mdPath = dir.resolve("strategic_break_clamp_source_triage.md")
    Files.writeString(tsvPath, report.tsv, StandardCharsets.UTF_8)
    Files.writeString(mdPath, report.markdown, StandardCharsets.UTF_8)
    (tsvPath, mdPath)

  private val Header =
    List(
      "rank",
      "score",
      "scoreReasons",
      "gameId",
      "label",
      "ply",
      "fen",
      "playedUci",
      "engineGate",
      "routeId",
      "routeToken",
      "destinationToken",
      "transformRisk",
      "transformVerdicts",
      "suggestedSourceId",
      "suggestedPlyRange",
      "sourceReviewVerdict",
      "diagnosis",
      "blockers",
      "ownerFailureCodes",
      "mainProofSource",
      "mainClaimScope",
      "packetSummary",
      "contract",
      "contractStatus",
      "contractFailures",
      "release",
      "surfaceText",
      "moveReviewSurface",
      "selectedEnginePv"
    ).mkString("\t")

  private val PreferredBlackDestinations =
    Set("...a5", "...b5", "...c5", "...d5", "...e5", "...f5")

  private def scoredCandidate(
      row: BreakClampCandidateScanner.ScanRow,
      game: BreakClampCandidateScanner.SourceGame,
      sourceUrl: String
  ): RankedCandidate =
    val reasons = scala.collection.mutable.ListBuffer.empty[String]
    var score = 100

    if row.engineGate == BreakClampCandidateScanner.EngineGate.SourceMoveTopPv then
      score += 40
      reasons += "engine_top_pv"
    else
      score += 20
      reasons += "engine_near_top"

    val isBlack = row.routeId.startsWith("black:")
    val isWhite = row.routeId.startsWith("white:")
    val isQuiet = row.routeId.endsWith(":quiet_push")
    val isCapture = row.routeId.endsWith(":capture_break")
    val isPreferredDestination = PreferredBlackDestinations(row.destinationToken)
    val isOpeningBoilerplate =
      row.ply <= 12 || row.playedUci == "d4d5" || row.routeId.contains(":d7-d5:")

    if isBlack then
      score += 30
      reasons += "black_route"
    if isQuiet then
      score += 20
      reasons += "quiet_push"
    if isPreferredDestination then
      score += 30
      reasons += "preferred_destination"
    if isBlack && isQuiet && isPreferredDestination then
      score += 50
      reasons += "black_quiet_named_break"
    if isCapture then
      score -= 25
      reasons += "capture_route_downranked"
    if isWhite then
      score -= 40
      reasons += "white_route_downranked"
    if isOpeningBoilerplate then
      score -= 60
      reasons += "opening_boilerplate_downranked"

    RankedCandidate(
      rank = 0,
      score = score,
      scoreReasons = reasons.toList,
      row = row,
      source = sourceCandidateFor(row, game, sourceUrl)
    )

  private def engineRank(engineGate: String): Int =
    engineGate match
      case BreakClampCandidateScanner.EngineGate.SourceMoveTopPv          => 0
      case BreakClampCandidateScanner.EngineGate.SourceMoveNearTopMultiPv => 1
      case _                                                              => 2

  private def rowsMarkdown(rows: List[TriageRow]): List[String] =
    if rows.isEmpty then List("_none_")
    else
      rows.flatMap { triageRow =>
        val row = triageRow.ranked.row
        val review = triageRow.review
        List(
          s"- rank=${triageRow.ranked.rank}, score=${triageRow.ranked.score}, id=${row.suggestedSourceId}, game=${row.gameId}, ply=${row.ply}, route=${row.routeToken}, destination=${row.destinationToken}, verdict=${review.verdict}, blockers=${review.admissionBlockers}, source=${review.mainProofSource}, packet=${review.packetSummary}, surface=${review.primary}"
        )
      }

  private def clean(value: String): String =
    value.replace('\t', ' ').replace('\n', ' ').trim
