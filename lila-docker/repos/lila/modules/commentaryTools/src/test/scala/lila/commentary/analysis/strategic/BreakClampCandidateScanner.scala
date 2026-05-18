package lila.commentary.analysis.strategic

import chess.format.Fen
import chess.Color
import chess.variant.Standard
import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path, Paths }

import play.api.libs.json.*

import lila.commentary.PgnAnalysisHelper
import lila.commentary.model.strategic.VariationLine

object BreakClampCandidateScanner:

  object Classification:
    val CleanRouteClamp = "clean_route_clamp"
    val Blocked = "blocked"

  object EngineGate:
    val EngineMissing = "engine_missing"
    val SourceMoveTopPv = "source_move_top_pv"
    val SourceMoveNearTopMultiPv = "source_move_near_top_multipv"
    val SourceMoveMultiPvOnly = "source_move_multipv_only"
    val SourceMoveAbsentFromMultiPv = "source_move_absent_from_multipv"
    val PvAvailableNoFirstMove = "pv_available_no_first_move"

  final case class SourceGame(
      id: String,
      label: String,
      pgn: String,
      plyRange: Option[(Int, Int)] = None,
      focusColor: Option[Color] = None
  )

  trait Engine:
    def newGame(): Unit
    def analyze(fen: String, depth: Int, multiPv: Int): List[VariationLine]

  final case class ScanConfig(
      depth: Int = 12,
      multiPv: Int = 3,
      nearTopCp: Int = 50,
      offset: Int = 0,
      limit: Option[Int] = None
  )

  final case class ScanRow(
      gameId: String,
      label: String,
      ply: Int,
      fen: String,
      playedUci: String,
      engineGate: String,
      routeId: String,
      routeToken: String,
      destinationToken: String,
      transformRisk: String,
      transformRoutes: String,
      transformVerdicts: String,
      blockers: String,
      selectedEnginePv: List[String],
      suggestedSourceId: String,
      suggestedPlyRange: String,
      classification: String
  ):
    def tsv: String =
      List(
        gameId,
        label,
        ply.toString,
        fen,
        playedUci,
        engineGate,
        routeId,
        routeToken,
        destinationToken,
        transformRisk,
        transformRoutes,
        transformVerdicts,
        blockers,
        selectedEnginePv.mkString(" "),
        suggestedSourceId,
        suggestedPlyRange,
        classification
      ).map(clean).mkString("\t")

  final case class ScanReport(
      totalGames: Int,
      scannedPlies: Int,
      routeEvidencePlies: Int,
      rows: List[ScanRow]
  ):
    def cleanRows: List[ScanRow] =
      rows.filter(_.classification == Classification.CleanRouteClamp)

    def transformBlockedRows: List[ScanRow] =
      rows.filter(row => row.transformRisk == BreakClampMaterializer.BreakTransformRisk.CaptureTransform.toString)

    def engineBlockedRows: List[ScanRow] =
      rows.filter(_.blockers.split(";").exists(_.startsWith("engine:")))

  private final case class GameScan(scannedPlies: Int, routeEvidencePlies: Int, rows: List[ScanRow])

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

  private val Header =
    List(
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
      "transformRoutes",
      "transformVerdicts",
      "blockers",
      "selectedEnginePv",
      "suggestedSourceId",
      "suggestedPlyRange",
      "classification"
    ).mkString("\t")

  def scan(
      games: List[SourceGame],
      engine: Option[Engine],
      config: ScanConfig = ScanConfig()
  ): ScanReport =
    val selectedGames =
      config.limit match
        case Some(limit) => games.drop(config.offset).take(limit)
        case None        => games.drop(config.offset)
    val scans = selectedGames.map(scanGame(_, engine, config))
    ScanReport(
      totalGames = selectedGames.size,
      scannedPlies = scans.map(_.scannedPlies).sum,
      routeEvidencePlies = scans.map(_.routeEvidencePlies).sum,
      rows = scans.flatMap(_.rows).sortBy(row => (classificationRank(row), engineRank(row.engineGate), row.gameId, row.ply, row.routeId))
    )

  def readPgnFile(path: Path, id: String, label: String): SourceGame =
    SourceGame(id = id, label = label, pgn = Files.readString(path, StandardCharsets.UTF_8).trim)

  def readCorpus(path: Path): List[SourceGame] =
    val json = Json.parse(Files.readString(path, StandardCharsets.UTF_8))
    val games =
      (json \ "games").validate[List[JsonCorpusGame]].asOpt
        .orElse(
          json.validate[List[JsValue]].asOpt.map { corpora =>
            corpora.flatMap(corpus => (corpus \ "games").validate[List[JsonCorpusGame]].asOpt.getOrElse(Nil))
          }
        )
        .getOrElse(throw IllegalArgumentException(s"unsupported corpus JSON shape: $path"))
    games.map(game => SourceGame(game.id, game.label, game.pgn)).distinctBy(_.id)

  def tsv(report: ScanReport): String =
    (Header :: report.rows.map(_.tsv)).mkString("\n") + "\n"

  def markdown(report: ScanReport): String =
    val transformBlockers = report.transformBlockedRows.size
    val engineBlockers = report.engineBlockedRows.size
    val lines =
      List(
        "# Clean Break Clamp Candidate Scan",
        "",
        s"summary=games=${report.totalGames}, scannedPlies=${report.scannedPlies}, routeEvidencePlies=${report.routeEvidencePlies}, cleanCandidates=${report.cleanRows.size}, transformBlockers=$transformBlockers, engineBlockers=$engineBlockers",
        "",
        "## Clean Candidates",
        ""
      ) ++ rowsMarkdown(report.cleanRows) ++
        List(
          "",
          "## Transform-Blocked Diagnostics",
          ""
        ) ++ rowsMarkdown(report.transformBlockedRows) ++
        List(
          "",
          "## Other Diagnostics",
          ""
        ) ++ rowsMarkdown(report.rows.diff(report.cleanRows).diff(report.transformBlockedRows))
    lines.mkString("\n") + "\n"

  def writeArtifacts(report: ScanReport): (Path, Path) =
    val matrix = Paths.get("tmp", "strategic_break_clamp_candidate_scan.tsv")
    val review = Paths.get("tmp", "strategic_break_clamp_candidate_scan.md")
    Files.createDirectories(matrix.getParent)
    Files.writeString(matrix, tsv(report), StandardCharsets.UTF_8)
    Files.writeString(review, markdown(report), StandardCharsets.UTF_8)
    (matrix, review)

  private def scanGame(
      game: SourceGame,
      engine: Option[Engine],
      config: ScanConfig
  ): GameScan =
    PgnAnalysisHelper.extractPlyDataStrict(game.pgn) match
      case Left(_) => GameScan(0, 0, Nil)
      case Right(plyData) =>
        val scopedPlyData =
          plyData.filter { ply =>
            game.plyRange.forall { case (start, end) => ply.ply >= start && ply.ply <= end } &&
              game.focusColor.forall(_ == ply.color)
          }
        val rowsByPly =
          scopedPlyData.map(ply => scanPly(game, ply, engine, config))
        GameScan(
          scannedPlies = scopedPlyData.size,
          routeEvidencePlies = rowsByPly.count(_.nonEmpty),
          rows = rowsByPly.flatten
        )

  private def scanPly(
      game: SourceGame,
      ply: PgnAnalysisHelper.PlyData,
      engine: Option[Engine],
      config: ScanConfig
  ): List[ScanRow] =
    val onePlyEvidence = routeEvidenceFor(ply, VariationLine(List(ply.playedUci), scoreCp = 0, depth = config.depth))
    if onePlyEvidence.isEmpty then Nil
    else
      val engineLines =
        engine.toList.flatMap { e =>
          e.newGame()
          e.analyze(ply.fen, config.depth, config.multiPv)
        }
      val gate = engineGate(ply.playedUci, engineLines, config.nearTopCp)
      val selectedLine = selectedEngineLine(ply.playedUci, engineLines, gate)
      val branchTooShort =
        enginePasses(gate) && selectedLine.forall(_.moves.size < 4)
      val routeEvidence =
        if enginePasses(gate) && !branchTooShort then
          selectedLine.toList.flatMap(line => routeEvidenceFor(ply, line))
        else onePlyEvidence
      val selectedPv = selectedLine.map(_.moves).getOrElse(Nil)
      routeEvidence.map(rowForEvidence(game, ply, gate, selectedPv, branchTooShort, _))

  private def routeEvidenceFor(
      ply: PgnAnalysisHelper.PlyData,
      line: VariationLine
  ): List[BreakClampMaterializer.BreakRouteEvidence] =
    Fen.read(Standard, Fen.Full(ply.fen)).toList.flatMap { position =>
      BreakClampMaterializer.routeEvidence(
        fen = ply.fen,
        board = position.board,
        color = ply.color,
        mainLine = line
      )
    }

  private def rowForEvidence(
      game: SourceGame,
      ply: PgnAnalysisHelper.PlyData,
      engineGate: String,
      selectedPv: List[String],
      branchTooShort: Boolean,
      evidence: BreakClampMaterializer.BreakRouteEvidence
  ): ScanRow =
    val blockers =
      engineBlocker(engineGate)
        .orElse(Option.when(branchTooShort)("branch:too_short"))
        .getOrElse(routeBlocker(evidence))
    val clean =
      blockers == "none" &&
        enginePasses(engineGate) &&
        evidence.transformRisk == BreakClampMaterializer.BreakTransformRisk.None &&
        evidence.transformRoutes.isEmpty &&
        evidence.transformAssessments.isEmpty
    ScanRow(
      gameId = game.id,
      label = game.label,
      ply = ply.ply,
      fen = ply.fen,
      playedUci = ply.playedUci,
      engineGate = engineGate,
      routeId = evidence.routeId,
      routeToken = evidence.token,
      destinationToken = evidence.destinationToken,
      transformRisk = evidence.transformRisk.toString,
      transformRoutes = if evidence.transformRoutes.isEmpty then "-" else evidence.transformRoutes.mkString("+"),
      transformVerdicts = transformVerdicts(evidence),
      blockers = blockers,
      selectedEnginePv = selectedPv,
      suggestedSourceId = suggestedSourceId(game.id, ply.ply),
      suggestedPlyRange = s"${ply.ply}-${ply.ply}",
      classification = if clean then Classification.CleanRouteClamp else Classification.Blocked
    )

  private def engineGate(playedUci: String, engineLines: List[VariationLine], nearTopCp: Int): String =
    if engineLines.isEmpty then EngineGate.EngineMissing
    else
      val firstMoves = engineLines.flatMap(_.moves.headOption)
      firstMoves.headOption match
        case Some(top) if top == playedUci => EngineGate.SourceMoveTopPv
        case Some(_) if firstMoves.contains(playedUci) && nearTopGapCp(playedUci, engineLines).exists(_ <= nearTopCp) =>
          EngineGate.SourceMoveNearTopMultiPv
        case Some(_) if firstMoves.contains(playedUci) => EngineGate.SourceMoveMultiPvOnly
        case Some(_)                                  => EngineGate.SourceMoveAbsentFromMultiPv
        case None                                     => EngineGate.PvAvailableNoFirstMove

  private def selectedEngineLine(
      playedUci: String,
      engineLines: List[VariationLine],
      gate: String
  ): Option[VariationLine] =
    gate match
      case EngineGate.SourceMoveTopPv          => engineLines.headOption
      case EngineGate.SourceMoveNearTopMultiPv => engineLines.find(_.moves.headOption.contains(playedUci))
      case EngineGate.SourceMoveMultiPvOnly    => engineLines.find(_.moves.headOption.contains(playedUci))
      case _                                   => engineLines.headOption

  private def nearTopGapCp(playedUci: String, engineLines: List[VariationLine]): Option[Int] =
    for
      top <- engineLines.headOption
      played <- engineLines.find(_.moves.headOption.contains(playedUci))
    yield math.abs(top.effectiveScore - played.effectiveScore)

  private def enginePasses(gate: String): Boolean =
    gate == EngineGate.SourceMoveTopPv || gate == EngineGate.SourceMoveNearTopMultiPv

  private def engineBlocker(gate: String): Option[String] =
    gate match
      case EngineGate.SourceMoveTopPv          => None
      case EngineGate.SourceMoveNearTopMultiPv => None
      case EngineGate.SourceMoveMultiPvOnly    => Some("engine:source_move_multipv_only")
      case EngineGate.SourceMoveAbsentFromMultiPv => Some("engine:source_move_absent_from_multipv")
      case EngineGate.EngineMissing               => Some("engine:missing")
      case EngineGate.PvAvailableNoFirstMove      => Some("engine:pv_available_no_first_move")
      case other                                 => Some(s"engine:$other")

  private def routeBlocker(evidence: BreakClampMaterializer.BreakRouteEvidence): String =
    evidence.transformRisk match
      case BreakClampMaterializer.BreakTransformRisk.None => "none"
      case BreakClampMaterializer.BreakTransformRisk.SameRouteRestored =>
        "owner:break_prevention_same_route_restored"
      case BreakClampMaterializer.BreakTransformRisk.BranchUnstable =>
        "owner:break_prevention_branch_unstable"
      case BreakClampMaterializer.BreakTransformRisk.CaptureTransform =>
        val blockers =
          evidence.transformAssessments
            .map(_.verdict)
            .distinct
            .map {
              case BreakClampMaterializer.BreakTransformVerdict.UnansweredCapture =>
                "owner:break_prevention_capture_transform_unanswered"
              case BreakClampMaterializer.BreakTransformVerdict.RecaptureAvailableUnproven =>
                "owner:break_prevention_capture_transform_recapture_unproven"
              case BreakClampMaterializer.BreakTransformVerdict.RecaptureStillReleases =>
                "owner:break_prevention_capture_transform_recapture_still_releases"
              case BreakClampMaterializer.BreakTransformVerdict.RecaptureProvenHarmless =>
                "none"
            }
        val activeBlockers = blockers.filterNot(_ == "none")
        if activeBlockers.isEmpty && blockers.nonEmpty then "none"
        else if activeBlockers.isEmpty then "owner:break_prevention_capture_transform_risk"
        else activeBlockers.mkString(";")

  private def transformVerdicts(evidence: BreakClampMaterializer.BreakRouteEvidence): String =
    val verdicts = evidence.transformAssessments.map(_.verdict.toString).distinct
    if verdicts.isEmpty then "-" else verdicts.mkString("+")

  private def suggestedSourceId(gameId: String, ply: Int): String =
    s"source-${slug(gameId)}-ply-$ply-break-prevention"

  private def slug(raw: String): String =
    raw.toLowerCase
      .replaceAll("[^a-z0-9]+", "-")
      .replaceAll("^-+", "")
      .replaceAll("-+$", "")

  private def rowsMarkdown(rows: List[ScanRow]): List[String] =
    if rows.isEmpty then List("- none")
    else
      rows.map { row =>
        s"- ${row.gameId}: ply=${row.ply} move=${row.playedUci} engine=${row.engineGate} route=${row.routeId} token=${row.routeToken} risk=${row.transformRisk} verdicts=${row.transformVerdicts} blockers=${row.blockers} suggest=${row.suggestedSourceId} range=${row.suggestedPlyRange}"
      }

  private def classificationRank(row: ScanRow): Int =
    if row.classification == Classification.CleanRouteClamp then 0
    else if row.transformRisk == BreakClampMaterializer.BreakTransformRisk.CaptureTransform.toString then 1
    else 2

  private def engineRank(gate: String): Int =
    gate match
      case EngineGate.SourceMoveTopPv          => 0
      case EngineGate.SourceMoveNearTopMultiPv => 1
      case _                                   => 2

  private def clean(raw: String): String =
    Option(raw).getOrElse("").replaceAll("\\s+", " ").trim
