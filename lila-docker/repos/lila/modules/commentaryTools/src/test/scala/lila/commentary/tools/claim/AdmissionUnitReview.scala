package lila.commentary.tools.claim

import java.nio.charset.StandardCharsets
import java.nio.charset.MalformedInputException
import java.nio.file.{ Files, Path, Paths }

import play.api.libs.json.*

import lila.commentary.PgnAnalysisHelper
import lila.commentary.analysis.PlanTaxonomy

object AdmissionUnitReview:

  final case class SourceGame(
      id: String,
      label: String,
      pgn: String,
      sourceUrl: String,
      plyRange: Option[(Int, Int)] = None
  )

  final case class AdmissionConfig(
      planKind: String = "prophylaxis_restraint",
      depth: Int = 16,
      multiPv: Int = 3,
      nearTopCp: Int = 50,
      offset: Int = 0,
      limit: Option[Int] = None,
      maxCandidates: Int = 24,
      admitLimit: Int = 1,
      sourceUrl: String = "https://www.pgnmentor.com/openings/ModernBenoni6e4.zip"
  )

  final case class RankedObservation(
      rank: Int,
      score: Int,
      scoreReasons: List[String],
      observation: SourceReview.Observation
  )

  final case class AdmissionRow(
      ranked: RankedObservation
  ):
    def observation: SourceReview.Observation = ranked.observation

    def tsv: String =
      val obs = observation
      List(
        ranked.rank.toString,
        ranked.score.toString,
        ranked.scoreReasons.mkString(";"),
        obs.source.id,
        obs.source.gameName,
        obs.source.reviewGroup,
        obs.verdict,
        obs.diagnosis,
        obs.admissionBlockers,
        obs.ownerFailureCodes,
        obs.engineAgreement,
        obs.plannerOwnership,
        obs.surfaceGate,
        obs.mainProofSource,
        obs.mainClaimScope,
        obs.packetSummary,
        obs.contractId,
        obs.contractStatus,
        obs.contractFailures,
        obs.release,
        obs.taxonomy,
        obs.ply.fold("-")(_.toString),
        obs.fen.getOrElse("-"),
        obs.playedUci.getOrElse("-"),
        obs.enginePv.mkString(" "),
        obs.primary,
        obs.moveReview,
        obs.reason
      ).map(clean).mkString("\t")

  final case class AdmissionReport(
      unitSpec: AdmissionUnitCatalog.AdmissionUnitSpec,
      rows: List[AdmissionRow],
      admitLimit: Int
  ):
    def admittedRows: List[AdmissionRow] =
      rows
        .filter(row => exactAdmissionUnitSupported(row.observation, unitSpec))
        .take(admitLimit)

    def tsv: String =
      (Header :: rows.map(_.tsv)).mkString("\n") + "\n"

    def markdown: String =
      val admitted = admittedRows
      val blockerCounts =
        rows
          .flatMap(_.observation.admissionBlockers.split(";").toList.map(_.trim).filter(_.nonEmpty).filterNot(_ == "none"))
          .groupBy(identity)
          .view
          .mapValues(_.size)
          .toList
          .sortBy(_._1)
      val lines =
        List(
          "# Strategic Admission Unit Review",
          "",
          s"planKind=${unitSpec.planKindId}",
          s"proofSource=${unitSpec.proofSource}",
          s"proofFamily=${unitSpec.proofFamily}",
          s"summary=triageRows=${rows.size}, admittedRows=${admitted.size}, admitLimit=$admitLimit",
          s"Admission blockers: ${if blockerCounts.isEmpty then "none" else blockerCounts.map { case (k, v) => s"$k=$v" }.mkString(", ")}",
          "",
          "## Admitted Rows",
          ""
        ) ++ rowsMarkdown(admitted) ++
          List(
            "",
            "## Reviewed Rows",
            ""
          ) ++ rowsMarkdown(rows)
      lines.mkString("\n") + "\n"

  private final case class JsonCorpusGame(id: String, label: String, pgn: String)

  private object JsonCorpusGame:
    given Reads[JsonCorpusGame] = Reads { json =>
      for
        id <- (json \ "id").validate[String]
        pgn <- (json \ "pgn").validate[String]
      yield
        JsonCorpusGame(
          id = id,
          label =
            (json \ "label").asOpt[String]
              .orElse((json \ "gameName").asOpt[String])
              .getOrElse(id),
          pgn = pgn
        )
    }

  def specForPlanKind(
      planKind: String
  ): Option[AdmissionUnitCatalog.AdmissionUnitSpec] =
    AdmissionUnitCatalog.admissionUnits.find(_.planKindId == planKind)

  def admit(
      games: List[SourceGame],
      engine: Option[SourceReview.SourceReviewEngine],
      config: AdmissionConfig = AdmissionConfig()
  ): AdmissionReport =
    val sources = candidateSources(games, config)
    val observations =
      SourceReview.observationsForSources(
        sources = sources,
        engine = engine,
        depth = config.depth,
        multiPv = config.multiPv
      )
    reportFromObservations(observations, config)

  private[commentary] def reportFromObservations(
      observations: List[SourceReview.Observation],
      config: AdmissionConfig
  ): AdmissionReport =
    val contract =
      specForPlanKind(config.planKind)
        .getOrElse(sys.error(s"missing admission-unit spec: ${config.planKind}"))
    AdmissionReport(
      unitSpec = contract,
      rows = rankObservations(observations, config).map(AdmissionRow(_)),
      admitLimit = config.admitLimit
    )

  private[commentary] def rankObservations(
      observations: List[SourceReview.Observation],
      config: AdmissionConfig
  ): List[RankedObservation] =
    val contract =
      specForPlanKind(config.planKind)
        .getOrElse(sys.error(s"missing admission-unit spec: ${config.planKind}"))
    observations
      .map(observation => scoredObservation(observation, contract))
      .sortBy(row =>
        (
          -row.score,
          engineRank(row.observation.engineAgreement),
          row.observation.source.id,
          row.observation.ply.getOrElse(Int.MaxValue)
        )
      )
      .take(config.maxCandidates)
      .zipWithIndex
      .map { case (row, idx) => row.copy(rank = idx + 1) }

  private[commentary] def sourceCandidateFor(
      game: SourceGame,
      ply: PgnAnalysisHelper.PlyData,
      planKind: String
  ): SourceWitnessCatalog.SourceCandidate =
    SourceWitnessCatalog.SourceCandidate(
      id = s"source-${slug(game.id)}-${planKind.replace('_', '-')}-ply-${ply.ply}",
      gameName = game.label,
      sourceUrl = game.sourceUrl,
      pgn = game.pgn,
      candidatePlyRange = SourceWitnessCatalog.CandidatePlyRange(ply.ply, ply.ply),
      reviewGroup = reviewGroupForPlanKind(planKind),
      intendedVerdict = SourceReview.Verdict.ScreenOnly,
      validationNote =
        s"Transient $planKind admission-unit candidate: exact ply ${ply.ply} ${ply.playedMove} (${ply.playedUci}); discovery only until fixed SourceReview admission."
    )

  def readPgnFile(path: Path, id: String, label: String, sourceUrl: String): List[SourceGame] =
    val raw = readPgnText(path)
    val games = splitPgnGames(raw)
    if games.size <= 1 then
      List(SourceGame(id = id, label = label, pgn = raw.trim, sourceUrl = sourceUrl))
    else
      games.zipWithIndex.map { case (pgn, idx) =>
        val headers = parseHeaders(pgn)
        val white = headers.getOrElse("White", "White")
        val black = headers.getOrElse("Black", "Black")
        val event = headers.getOrElse("Event", label)
        val gameNo = idx + 1
        SourceGame(
          id = f"${slug(id)}-$gameNo%05d",
          label = s"$label #$gameNo: $white-$black, $event",
          pgn = pgn,
          sourceUrl = sourceUrl
        )
      }

  def readCorpus(path: Path, sourceUrl: String): List[SourceGame] =
    val json = Json.parse(Files.readString(path, StandardCharsets.UTF_8))
    val games =
      (json \ "games").validate[List[JsonCorpusGame]].asOpt
        .orElse(
          json.validate[List[JsValue]].asOpt.map { corpora =>
            corpora.flatMap(corpus => (corpus \ "games").validate[List[JsonCorpusGame]].asOpt.getOrElse(Nil))
          }
        )
        .getOrElse(throw IllegalArgumentException(s"unsupported corpus JSON shape: $path"))
    games.map(game => SourceGame(game.id, game.label, game.pgn, sourceUrl)).distinctBy(_.id)

  def writeArtifacts(report: AdmissionReport): (Path, Path) =
    val dir = Paths.get("tmp")
    Files.createDirectories(dir)
    val tsvPath = dir.resolve("strategic_admission_unit_review.tsv")
    val mdPath = dir.resolve("strategic_admission_unit_review.md")
    Files.writeString(tsvPath, report.tsv, StandardCharsets.UTF_8)
    Files.writeString(mdPath, report.markdown, StandardCharsets.UTF_8)
    (tsvPath, mdPath)

  private def candidateSources(
      games: List[SourceGame],
      config: AdmissionConfig
  ): List[SourceWitnessCatalog.SourceCandidate] =
    val selected =
    config.limit match
      case Some(limit) => games.drop(config.offset).take(limit)
      case None        => games.drop(config.offset)
    selected.iterator.flatMap { game =>
      PgnAnalysisHelper.extractPlyDataStrict(game.pgn).toOption.iterator.flatMap { plyData =>
        plyData
          .iterator
          .filter(ply =>
            game.plyRange.forall { case (start, end) => ply.ply >= start && ply.ply <= end } &&
              candidatePreScreen(ply, plyData.size, config.planKind)
          )
          .map(sourceCandidateFor(game, _, config.planKind))
      }
    }.take(config.maxCandidates).toList

  private def reviewGroupForPlanKind(planKind: String): String =
    specForPlanKind(planKind).map(_.sourceReviewGroup).getOrElse(s"A:$planKind")

  private def candidatePreScreen(
      ply: PgnAnalysisHelper.PlyData,
      totalPlies: Int,
      planKind: String
  ): Boolean =
    if tacticalTransitionPlanKinds.contains(planKind) then tacticalTransitionPreScreen(ply, totalPlies)
    else prophylaxisPreScreen(ply, totalPlies)

  private val tacticalTransitionPlanKinds =
    Set(
      PlanTaxonomy.PlanKind.IQPInducement.id,
      PlanTaxonomy.PlanKind.SimplificationWindow.id,
      PlanTaxonomy.PlanKind.DefenderTrade.id,
      PlanTaxonomy.PlanKind.QueenTradeShield.id,
      PlanTaxonomy.PlanKind.BadPieceLiquidation.id
    )

  private def prophylaxisPreScreen(
      ply: PgnAnalysisHelper.PlyData,
      totalPlies: Int
  ): Boolean =
    totalPlies - ply.ply >= 2 &&
      ply.playedUci.length >= 4 &&
      !ply.playedMove.contains("x") &&
      !ply.playedMove.contains("+") &&
      !ply.playedMove.contains("#") &&
      !ply.playedMove.contains("=") &&
      !ply.playedMove.startsWith("O-O")

  private def tacticalTransitionPreScreen(
      ply: PgnAnalysisHelper.PlyData,
      totalPlies: Int
  ): Boolean =
    totalPlies - ply.ply >= 1 &&
      ply.playedUci.length >= 4 &&
      !ply.playedMove.contains("#") &&
      !ply.playedMove.contains("=") &&
      !ply.playedMove.startsWith("O-O")

  private def scoredObservation(
      observation: SourceReview.Observation,
      contract: AdmissionUnitCatalog.AdmissionUnitSpec
  ): RankedObservation =
    val reasons = scala.collection.mutable.ListBuffer.empty[String]
    var score = 100
    if observation.engineAgreement == "top_pv_matches_played" then
      score += 40
      reasons += "engine_top_pv"
    else if observation.engineAgreement.startsWith("near_top_multipv_contains_played") then
      score += 20
      reasons += "engine_near_top"
    else if observation.engineAgreement == "engine_missing" then
      score -= 80
      reasons += "engine_missing"
    else
      score -= 60
      reasons += "engine_blocked"

    if exactAdmissionUnitSupported(observation, contract) then
      score += 160
      reasons += "exact_admission_unit_authority"
    if observation.admissionBlockers.contains(s"proof:${contract.planKindId}_contract_mismatch") then
      score -= 50
      reasons += "contract_mismatch"
    if observation.admissionBlockers.contains("tactical:first") then
      score -= 120
      reasons += "tactical_first"
    if observation.ply.exists(_ <= 12) then
      score -= 25
      reasons += "opening_downranked"
    if observation.admissionBlockers == "none" then
      score += 30
      reasons += "no_admission_blockers"

    RankedObservation(0, score, reasons.toList, observation)

  private def exactAdmissionUnitSupported(
      observation: SourceReview.Observation,
      contract: AdmissionUnitCatalog.AdmissionUnitSpec
  ): Boolean =
    val authorityGateMatches =
      observation.release match
        case "CertifiedOwner" => observation.surfaceGate == "certified_owner_surface_not_blocking"
        case "SupportedLocal" => observation.surfaceGate == "supported_local_surface_passed"
        case _                => false
    observation.verdict == SourceReview.Verdict.AdmitAuthorityRow &&
      contract.surfaceAuthorityTiers.contains(observation.release) &&
      observation.mainProofSource == contract.proofSource &&
      observation.packetSummary.contains(s"proof_family=${contract.proofFamily}") &&
      observation.contractId.contains(contract.proofFamily) &&
      observation.contractStatus == "Releasable" &&
      authorityGateMatches

  private def engineRank(engineAgreement: String): Int =
    if engineAgreement == "top_pv_matches_played" then 0
    else if engineAgreement.startsWith("near_top_multipv_contains_played") then 1
    else 2

  private def rowsMarkdown(rows: List[AdmissionRow]): List[String] =
    if rows.isEmpty then List("_none_")
    else
      rows.map { row =>
        val obs = row.observation
        s"- rank=${row.ranked.rank}, score=${row.ranked.score}, id=${obs.source.id}, reviewGroup=${obs.source.reviewGroup}, ply=${obs.ply.getOrElse("-")}, verdict=${obs.verdict}, blockers=${obs.admissionBlockers}, proofSource=${obs.mainProofSource}, packet=${clean(obs.packetSummary)}, surface=${clean(obs.primary)}"
      }

  private val Header =
    List(
      "rank",
      "score",
      "scoreReasons",
      "id",
      "gameName",
      "reviewGroup",
      "verdict",
      "diagnosis",
      "blockers",
      "ownerFailureCodes",
      "engineAgreement",
      "plannerOwnership",
      "surfaceGate",
      "mainProofSource",
      "mainClaimScope",
      "packetSummary",
      "contract",
      "contractStatus",
      "contractFailures",
      "release",
      "taxonomy",
      "ply",
      "fen",
      "playedUci",
      "enginePv",
      "primary",
      "moveReview",
      "reason"
    ).mkString("\t")

  private val HeaderPattern = """(?m)^\[(\w+)\s+"(.*)"\]\s*$""".r

  private def splitPgnGames(raw: String): List[String] =
    val normalized = Option(raw).getOrElse("").replace("\r\n", "\n").replace('\r', '\n').trim
    if normalized.isEmpty then Nil
    else
      """(?m)(?=^\[Event\s+")""".r
        .split(normalized)
        .toList
        .map(_.trim)
        .filter(chunk => chunk.nonEmpty && chunk.startsWith("[Event "))

  private def parseHeaders(pgn: String): Map[String, String] =
    HeaderPattern
      .findAllMatchIn(Option(pgn).getOrElse(""))
      .map(m => m.group(1).trim -> m.group(2).trim)
      .toMap

  private def readPgnText(path: Path): String =
    try Files.readString(path, StandardCharsets.UTF_8)
    catch
      case _: MalformedInputException =>
        Files.readString(path, StandardCharsets.ISO_8859_1)

  private def slug(raw: String): String =
    raw.toLowerCase
      .replaceAll("[^a-z0-9]+", "-")
      .replaceAll("(^-+|-+$)", "")

  private def clean(value: String): String =
    value.replace('\t', ' ').replace('\n', ' ').trim
