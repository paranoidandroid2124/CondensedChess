package lila.commentary.tools.opening

import play.api.libs.json.*

import java.net.URI
import java.net.http.{ HttpClient, HttpRequest, HttpResponse }
import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path, Paths, StandardOpenOption }
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*
import scala.util.control.NonFatal

object OpeningMasterDbAuditRunner:

  final case class Config(
      openingsPath: Path = Paths.get("modules/commentaryCore/src/main/resources/lila/commentary/openings/openings.tsv"),
      outPath: Path = Paths.get("modules/commentaryTools/target/opening-masterdb-audit.json"),
      mode: OpeningMasterDbAudit.QueryMode = OpeningMasterDbAudit.QueryMode.Fen,
      live: Boolean = false,
      skipRows: Int = 0,
      maxRows: Option[Int] = None,
      onlyIssues: Boolean = false,
      statusFilter: Set[String] = Set.empty,
      sleepMs: Int = 250,
      requestTimeoutSeconds: Int = 20,
      tokenEnv: String = "LICHESS_TOKEN",
      evidenceCachePath: Option[Path] = None,
      masterDbBaseUrl: String = OpeningMasterDbAudit.DefaultMastersBaseUrl,
      writeEvidenceCachePath: Option[Path] = None,
      mastersSince: Option[String] = None,
      mastersUntil: Option[String] = None
  )

  final case class Report(
      source: String,
      live: Boolean,
      queryMode: String,
      querySince: Option[String],
      queryUntil: Option[String],
      rowCount: Int,
      selectedSkipRows: Int,
      selectedRowLimit: Option[Int],
      selectedRowCount: Int,
      issueRowCount: Int,
      duplicateEndpointGroupCount: Int,
      masterBackedRowCount: Int,
      masterFetchErrorCount: Int,
      provenanceStatusCounts: Map[String, Int],
      rows: List[ReportRow]
  )

  final case class ReportRow(
      rowId: String,
      lineNo: Int,
      eco: String,
      name: String,
      endpointKey: Option[String],
      plyCount: Int,
      issueTags: List[String],
      duplicateEndpointSize: Int,
      masterRequestUrl: String,
      masterTotalGames: Option[Int],
      masterOpeningEco: Option[String],
      masterOpeningName: Option[String],
      masterTopGameIds: List[String],
      masterEvidenceSource: Option[String],
      masterEvidenceError: Option[String],
      provenanceStatus: String
  )

  private final case class CachedEvidence(evidence: OpeningMasterDbAudit.MastersEvidence, source: String)
  private final case class LiveLookup(
      evidence: Option[OpeningMasterDbAudit.MastersEvidence],
      rawResponse: Option[String],
      error: Option[String]
  )

  given Writes[ReportRow] = Json.writes[ReportRow]
  given Writes[Report] = Json.writes[Report]

  def main(args: Array[String]): Unit =
    val config = parseConfig(args.toList)
    val report =
      buildReport(config) match
        case Right(value) => value
        case Left(error) =>
          System.err.println(s"[opening-masterdb-audit] $error")
          sys.exit(1)
    Files.createDirectories(config.outPath.getParent)
    Files.writeString(config.outPath, Json.prettyPrint(Json.toJson(report)) + "\n", StandardCharsets.UTF_8)
    println(
      s"[opening-masterdb-audit] wrote ${config.outPath} rows=${report.rowCount} " +
        s"issues=${report.issueRowCount} duplicateGroups=${report.duplicateEndpointGroupCount} " +
        s"masterBacked=${report.masterBackedRowCount} fetchErrors=${report.masterFetchErrorCount} live=${report.live}"
    )

  def buildReport(config: Config): Either[String, Report] =
    val token = sys.env.get(config.tokenEnv).filter(_.nonEmpty)
    buildReport(config, token, url => HttpMasterFetcher(token, config.requestTimeoutSeconds).fetch(url))

  def buildReport(
      config: Config,
      token: Option[String],
      fetchMaster: String => Either[String, String]
  ): Either[String, Report] =
    if !Files.exists(config.openingsPath) then Left(s"missing openings TSV: ${config.openingsPath}")
    else
      val audited =
        OpeningPoolAudit.auditLines(Files.readAllLines(config.openingsPath, StandardCharsets.UTF_8).asScala)
      val duplicateSizeByEndpoint =
        OpeningPoolAudit
          .duplicateEndpointGroups(audited)
          .flatMap(group => group.rows.map(row => row.stableId -> group.rows.size))
          .toMap
      val selected =
        audited
          .filter(row => !config.onlyIssues || row.issues.nonEmpty || duplicateSizeByEndpoint.contains(row.stableId))
          .sortBy(_.lineNo)
          .drop(config.skipRows.max(0))
          .take(config.maxRows.getOrElse(Int.MaxValue))
      val cachedEvidence =
        config.evidenceCachePath
          .map(loadEvidenceCache)
          .getOrElse(Right(Map.empty[String, CachedEvidence])) match
          case Left(error) => return Left(error)
          case Right(value) => value
      if config.live && token.isEmpty then
        Left(s"--live requires OAuth token in environment variable ${config.tokenEnv}")
      else
        prepareEvidenceCache(config.writeEvidenceCachePath) match
          case Left(error) => return Left(error)
          case Right(_)    =>
        val reportRows =
          selected.map { row =>
            val request =
              OpeningMasterDbAudit.mastersRequest(
                row,
                config.mode,
                config.masterDbBaseUrl,
                since = config.mastersSince,
                until = config.mastersUntil
              )
            val cacheHit = cachedEvidence.get(row.stableId).orElse(cachedEvidence.get(row.legacyStableId))
            val liveLookup =
              if config.live && cacheHit.isEmpty then
                val fetched = fetchMaster(request.url)
                if config.sleepMs > 0 then Thread.sleep(config.sleepMs.millis.toMillis)
                fetched match
                  case Left(error) => LiveLookup(None, None, Some(stableError(error, "master-fetch-error")))
                  case Right(raw) =>
                    OpeningMasterDbAudit.parseMastersResponse(raw) match
                      case Left(error) => LiveLookup(None, Some(raw), Some(s"parse-error: ${stableError(error, "unknown")}"))
                      case Right(evidence) =>
                        config.writeEvidenceCachePath.foreach { _ =>
                          appendEvidenceCache(
                            config.writeEvidenceCachePath,
                            Json.stringify(
                              Json.obj(
                                "rowId" -> row.stableId,
                                "requestUrl" -> request.url,
                                "mode" -> request.mode.toString,
                                "since" -> config.mastersSince,
                                "until" -> config.mastersUntil,
                                "rawResponse" -> raw
                              )
                            )
                          )
                        }
                        LiveLookup(Some(evidence), Some(raw), None)
              else LiveLookup(None, None, None)
            val evidence = cacheHit.map(_.evidence).orElse(liveLookup.evidence)
            val evidenceSource = cacheHit.map(_.source).orElse(Option.when(liveLookup.evidence.nonEmpty)("live"))
            val duplicateSize = duplicateSizeByEndpoint.getOrElse(row.stableId, 1)
            ReportRow(
              rowId = row.stableId,
              lineNo = row.lineNo,
              eco = row.eco,
              name = row.name,
              endpointKey = row.endpointKey,
              plyCount = row.plyCount,
              issueTags = row.issues,
              duplicateEndpointSize = duplicateSize,
              masterRequestUrl = request.url,
              masterTotalGames = evidence.map(_.totalGames),
              masterOpeningEco = evidence.flatMap(_.openingEco),
              masterOpeningName = evidence.flatMap(_.openingName),
              masterTopGameIds = evidence.map(_.topGameIds).getOrElse(Nil),
              masterEvidenceSource = evidenceSource,
              masterEvidenceError = liveLookup.error,
              provenanceStatus = provenanceStatus(row, duplicateSize, evidence, liveLookup.error)
            )
          }
        val statusCounts =
          reportRows.groupMapReduce(_.provenanceStatus)(_ => 1)(_ + _)
        val filteredReportRows =
          if config.statusFilter.isEmpty then reportRows
          else reportRows.filter(row => config.statusFilter.contains(row.provenanceStatus))
        Right(
          Report(
            source = config.openingsPath.toString,
            live = config.live,
            queryMode = config.mode.toString,
            querySince = config.mastersSince,
            queryUntil = config.mastersUntil,
            rowCount = audited.size,
            selectedSkipRows = config.skipRows.max(0),
            selectedRowLimit = config.maxRows,
            selectedRowCount = reportRows.size,
            issueRowCount = audited.count(_.issues.nonEmpty),
            duplicateEndpointGroupCount = OpeningPoolAudit.duplicateEndpointGroups(audited).size,
            masterBackedRowCount = reportRows.count(_.provenanceStatus == "master-backed"),
            masterFetchErrorCount = reportRows.count(_.masterEvidenceError.nonEmpty),
            provenanceStatusCounts = statusCounts,
            rows = filteredReportRows
          )
        )

  private def provenanceStatus(
      row: OpeningPoolAudit.AuditedRow,
      duplicateSize: Int,
      evidence: Option[OpeningMasterDbAudit.MastersEvidence],
      evidenceError: Option[String]
  ): String =
    if row.issues.nonEmpty then "quarantine"
    else if duplicateSize > 1 then "transposition-duplicate"
    else if evidenceError.nonEmpty then "master-fetch-error"
    else
      evidence match
        case Some(value) if value.isMasterBacked => "master-backed"
        case Some(_)                             => "not-found-in-masters"
        case None                                => "unverified"

  private final case class HttpMasterFetcher(token: Option[String], requestTimeoutSeconds: Int):
    private val client = HttpClient.newHttpClient()
    def fetch(url: String): Either[String, String] =
      try
        val builder =
          HttpRequest
            .newBuilder(URI.create(url))
            .timeout(java.time.Duration.ofSeconds(requestTimeoutSeconds.max(1).toLong))
            .header("Accept", "application/json")
        token.foreach(value => builder.header("Authorization", s"Bearer $value"))
        val request = builder.GET().build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
        if response.statusCode() >= 200 && response.statusCode() < 300 then Right(response.body())
        else Left(s"HTTP ${response.statusCode()} from $url")
      catch case NonFatal(e) => Left(stableError(e.getMessage, e.getClass.getSimpleName))

  private def loadEvidenceCache(path: Path): Either[String, Map[String, CachedEvidence]] =
    if !Files.exists(path) then Left(s"missing evidence cache: $path")
    else
      try
        val entries =
          Files
            .readAllLines(path, StandardCharsets.UTF_8)
            .asScala
            .toList
            .zipWithIndex
            .flatMap { case (line, idx) =>
              val raw = line.trim
              if raw.isEmpty then None
              else
                val js = Json.parse(raw)
                val rowId = (js \ "rowId").asOpt[String].getOrElse("")
                val response =
                  (js \ "response")
                    .asOpt[JsValue]
                    .map(Json.stringify)
                    .orElse((js \ "rawResponse").asOpt[String])
                    .getOrElse("")
                if rowId.isEmpty || response.isEmpty then
                  throw IllegalArgumentException(s"$path line ${idx + 1}: expected rowId and response/rawResponse")
                OpeningMasterDbAudit.parseMastersResponse(response) match
                  case Right(evidence) => Some(rowId -> CachedEvidence(evidence, "cache"))
                  case Left(error)     => throw IllegalArgumentException(s"$path line ${idx + 1}: $error")
            }
        Right(entries.toMap)
      catch case NonFatal(e) => Left(e.getMessage)

  private def prepareEvidenceCache(path: Option[Path]): Either[String, Unit] =
    path match
      case None => Right(())
      case Some(value) =>
        try
          Option(value.getParent).foreach(Files.createDirectories(_))
          Files.deleteIfExists(value)
          Files.createFile(value)
          Right(())
        catch case NonFatal(e) => Left(stableError(e.getMessage, e.getClass.getSimpleName))

  private def appendEvidenceCache(path: Option[Path], row: String): Unit =
    path.foreach { value =>
      Files.writeString(
        value,
        row + "\n",
        StandardCharsets.UTF_8,
        StandardOpenOption.CREATE,
        StandardOpenOption.APPEND
      )
    }

  private def stableError(value: String, fallback: String): String =
    Option(value).map(_.trim).filter(_.nonEmpty).getOrElse(fallback)

  private def parseConfig(args: List[String]): Config =
    def loop(rest: List[String], config: Config): Config =
      rest match
        case Nil => config
        case "--openings" :: value :: tail => loop(tail, config.copy(openingsPath = Paths.get(value)))
        case "--out" :: value :: tail      => loop(tail, config.copy(outPath = Paths.get(value)))
        case "--skip-rows" :: value :: tail => loop(tail, config.copy(skipRows = value.toIntOption.getOrElse(config.skipRows)))
        case "--max-rows" :: value :: tail => loop(tail, config.copy(maxRows = value.toIntOption))
        case "--only-status" :: value :: tail =>
          loop(tail, config.copy(statusFilter = value.split(",").map(_.trim).filter(_.nonEmpty).toSet))
        case "--sleep-ms" :: value :: tail => loop(tail, config.copy(sleepMs = value.toIntOption.getOrElse(config.sleepMs)))
        case "--request-timeout-seconds" :: value :: tail =>
          loop(tail, config.copy(requestTimeoutSeconds = value.toIntOption.getOrElse(config.requestTimeoutSeconds)))
        case "--token-env" :: value :: tail => loop(tail, config.copy(tokenEnv = value))
        case "--evidence-cache" :: value :: tail => loop(tail, config.copy(evidenceCachePath = Some(Paths.get(value))))
        case "--write-evidence-cache" :: value :: tail =>
          loop(tail, config.copy(writeEvidenceCachePath = Some(Paths.get(value))))
        case "--base-url" :: value :: tail => loop(tail, config.copy(masterDbBaseUrl = value))
        case "--since" :: value :: tail    => loop(tail, config.copy(mastersSince = Some(value)))
        case "--until" :: value :: tail    => loop(tail, config.copy(mastersUntil = Some(value)))
        case "--live" :: tail              => loop(tail, config.copy(live = true))
        case "--only-issues" :: tail       => loop(tail, config.copy(onlyIssues = true))
        case "--mode" :: "play" :: tail    => loop(tail, config.copy(mode = OpeningMasterDbAudit.QueryMode.Play))
        case "--mode" :: "fen" :: tail     => loop(tail, config.copy(mode = OpeningMasterDbAudit.QueryMode.Fen))
        case _ :: tail                     => loop(tail, config)
    loop(args, Config())
