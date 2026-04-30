package lila.commentary.diagnostic

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path, Paths }

import chess.{ FullMoveNumber, Pawn }
import chess.format.{ Fen, Uci }
import chess.variant
import play.api.libs.json.*

final case class LowerDiagnosticMaterializedTransitionProbeSummary(
    totalRows: Int,
    uniqueGamePlyMoveRows: Int,
    legalFromCurrentFenRows: Int,
    illegalFromCurrentFenRows: Int,
    missingPlayedUciRows: Int,
    invalidCurrentFenRows: Int,
    existingPgnPathRows: Int,
    missingPgnPathRows: Int,
    countsByIllegalReason: Map[String, Int]
)

final case class LowerDiagnosticMaterializedTransitionProbeRow(
    rowId: String,
    gameKey: Option[String],
    ply: Int,
    currentFen: String,
    beforeFenCandidate: Option[String],
    playedUci: Option[String],
    afterFenCandidate: Option[String],
    replayState: String,
    reason: String,
    pgnPath: Option[String],
    pgnPathExists: Boolean
)

final case class LowerDiagnosticMaterializedTransitionProbeReport(
    summary: LowerDiagnosticMaterializedTransitionProbeSummary,
    rows: Vector[LowerDiagnosticMaterializedTransitionProbeRow]
)

object LowerDiagnosticMaterializedTransitionProbeReport:

  def fromRows(rows: Vector[LowerDiagnosticLargeCorpus.Row]): LowerDiagnosticMaterializedTransitionProbeReport =
    val probeRows = rows.map(probeRow)
    val uniqueKeys = rows.map(row =>
      (
        row.metadata.getOrElse("gameKey", row.id),
        row.input.ply,
        row.metadata.getOrElse("playedUci", row.input.playedMove.getOrElse(""))
      )
    ).distinct.size
    LowerDiagnosticMaterializedTransitionProbeReport(
      summary = LowerDiagnosticMaterializedTransitionProbeSummary(
        totalRows = probeRows.size,
        uniqueGamePlyMoveRows = uniqueKeys,
        legalFromCurrentFenRows = probeRows.count(_.replayState == "legal_from_current_fen"),
        illegalFromCurrentFenRows = probeRows.count(_.replayState == "illegal_from_current_fen"),
        missingPlayedUciRows = probeRows.count(_.replayState == "missing_played_uci"),
        invalidCurrentFenRows = probeRows.count(_.replayState == "invalid_current_fen"),
        existingPgnPathRows = probeRows.count(_.pgnPathExists),
        missingPgnPathRows = probeRows.count(row => row.pgnPath.nonEmpty && !row.pgnPathExists),
        countsByIllegalReason = countLabels(probeRows.filter(_.replayState == "illegal_from_current_fen").map(_.reason))
      ),
      rows = probeRows.sortBy(row => (stateRank(row.replayState), row.reason, row.rowId))
    )

  private def probeRow(row: LowerDiagnosticLargeCorpus.Row): LowerDiagnosticMaterializedTransitionProbeRow =
    val playedUci = row.metadata.get("playedUci").orElse(row.input.playedMove)
    val pgnPath = row.metadata.get("pgnPath")
    val pgnPathExists = pgnPath.exists(path => Files.exists(Paths.get(path)))
    val replay = playedUci match
      case None =>
        ("missing_played_uci", "missing_played_uci", None)
      case Some(uci) =>
        replayFrom(row.input.currentFen, uci)
    LowerDiagnosticMaterializedTransitionProbeRow(
      rowId = row.id,
      gameKey = row.metadata.get("gameKey"),
      ply = row.input.ply,
      currentFen = row.input.currentFen,
      beforeFenCandidate = Option.when(replay._1 == "legal_from_current_fen")(row.input.currentFen),
      playedUci = playedUci,
      afterFenCandidate = replay._3,
      replayState = replay._1,
      reason = replay._2,
      pgnPath = pgnPath,
      pgnPathExists = pgnPathExists
    )

  private def replayFrom(currentFen: String, playedUci: String): (String, String, Option[String]) =
    Uci(playedUci) match
      case Some(move: Uci.Move) =>
        Fen.read(variant.Standard, Fen.Full.clean(currentFen)) match
          case None => ("invalid_current_fen", "invalid_current_fen", None)
          case Some(position) =>
            position.move(move) match
              case Left(reason) => ("illegal_from_current_fen", reason.toString, None)
              case Right(done) =>
                LowerDiagnosticMaterializedTransitionFen.afterFen(currentFen, position, move, done) match
                  case Right(afterFen) => ("legal_from_current_fen", "legal_replay_from_current_fen", Some(afterFen))
                  case Left(reason)    => ("illegal_from_current_fen", reason, None)
      case Some(_) => ("illegal_from_current_fen", "unsupported_non_move_uci", None)
      case None    => ("illegal_from_current_fen", "invalid_uci", None)

  private def stateRank(state: String): Int =
    state match
      case "illegal_from_current_fen" => 1
      case "invalid_current_fen"      => 2
      case "missing_played_uci"       => 3
      case "legal_from_current_fen"   => 4
      case _                          => 9

  private def countLabels(values: Vector[String]): Map[String, Int] =
    values.groupMapReduce(identity)(_ => 1)(_ + _).toVector.sortBy(_._1).toMap.withDefaultValue(0)

object LowerDiagnosticMaterializedTransitionProbeJson:

  def summaryJson(summary: LowerDiagnosticMaterializedTransitionProbeSummary): JsObject =
    Json.obj(
      "totalRows" -> summary.totalRows,
      "uniqueGamePlyMoveRows" -> summary.uniqueGamePlyMoveRows,
      "legalFromCurrentFenRows" -> summary.legalFromCurrentFenRows,
      "illegalFromCurrentFenRows" -> summary.illegalFromCurrentFenRows,
      "missingPlayedUciRows" -> summary.missingPlayedUciRows,
      "invalidCurrentFenRows" -> summary.invalidCurrentFenRows,
      "existingPgnPathRows" -> summary.existingPgnPathRows,
      "missingPgnPathRows" -> summary.missingPgnPathRows,
      "countsByIllegalReason" -> summary.countsByIllegalReason
    )

  def rowJson(row: LowerDiagnosticMaterializedTransitionProbeRow): JsObject =
    Json.obj(
      "rowId" -> row.rowId,
      "gameKey" -> row.gameKey,
      "ply" -> row.ply,
      "currentFen" -> row.currentFen,
      "beforeFenCandidate" -> row.beforeFenCandidate,
      "playedUci" -> row.playedUci,
      "afterFenCandidate" -> row.afterFenCandidate,
      "replayState" -> row.replayState,
      "reason" -> row.reason,
      "pgnPath" -> row.pgnPath,
      "pgnPathExists" -> row.pgnPathExists
    )

object LowerDiagnosticMaterializedTransitionProbeRunner:

  private val defaultOutputDir = Paths.get("tmp/commentary-diagnostic/lower-layer/materialized300/transition-probe")

  def main(args: Array[String]): Unit =
    val options = RunnerOptions.parse(args.toVector)
    val rows =
      options.inputPath match
        case Some(path) => LowerDiagnosticLargeCorpus.loadExternalRows(path, options.limit)
        case None       => LowerDiagnosticLargeCorpus.loadTrackedRows()
    write(LowerDiagnosticMaterializedTransitionProbeReport.fromRows(rows), options.outputDir)

  def write(report: LowerDiagnosticMaterializedTransitionProbeReport, outputDir: Path): Unit =
    Files.createDirectories(outputDir)
    Files.writeString(
      outputDir.resolve("transition-probe-summary.json"),
      Json.prettyPrint(LowerDiagnosticMaterializedTransitionProbeJson.summaryJson(report.summary)) + System.lineSeparator(),
      StandardCharsets.UTF_8
    )
    Files.writeString(
      outputDir.resolve("transition-probe-rows.jsonl"),
      report.rows.map(LowerDiagnosticMaterializedTransitionProbeJson.rowJson).map(Json.stringify).mkString("", System.lineSeparator(), System.lineSeparator()),
      StandardCharsets.UTF_8
    )

  private final case class RunnerOptions(
      outputDir: Path,
      inputPath: Option[Path],
      limit: Option[Int]
  )

  private object RunnerOptions:
    def parse(args: Vector[String]): RunnerOptions =
      def valueAfter(flag: String): Option[String] =
        args.sliding(2).collectFirst { case Vector(`flag`, value) => value }
      RunnerOptions(
        outputDir = valueAfter("--out").map(Paths.get(_)).getOrElse(defaultOutputDir),
        inputPath = valueAfter("--input").map(Paths.get(_)),
        limit = valueAfter("--limit").flatMap(_.toIntOption)
      )

final case class LowerDiagnosticMaterializedTransitionEnrichmentSummary(
    totalRows: Int,
    enrichedRows: Int,
    failedRows: Int,
    countsByState: Map[String, Int],
    outputJsonl: String
)

final case class LowerDiagnosticMaterializedTransitionEnrichmentFailure(
    rowId: String,
    state: String,
    reason: String,
    fen: Option[String],
    playedUci: Option[String],
    ply: Option[Int]
)

object LowerDiagnosticMaterializedTransitionEnrichment:

  final case class Result(
      summary: LowerDiagnosticMaterializedTransitionEnrichmentSummary,
      enrichedRows: Vector[JsObject],
      failures: Vector[LowerDiagnosticMaterializedTransitionEnrichmentFailure]
  )

  private final case class Enriched(value: JsObject, state: String)

  def enrich(inputPath: Path, outputJsonl: Path, limit: Option[Int] = None): Result =
    val rawRows = readRows(inputPath, limit)
    val enriched = rawRows.map(enrichRow)
    val successes = enriched.collect { case Right(row) => row }
    val failures = enriched.collect { case Left(failure) => failure }
    Result(
      summary = LowerDiagnosticMaterializedTransitionEnrichmentSummary(
        totalRows = rawRows.size,
        enrichedRows = successes.size,
        failedRows = failures.size,
        countsByState = countLabels(successes.map(_.state) ++ failures.map(_.state)),
        outputJsonl = outputJsonl.toString
      ),
      enrichedRows = successes.map(_.value),
      failures = failures
    )

  private def readRows(inputPath: Path, limit: Option[Int]): Vector[JsObject] =
    val source = scala.io.Source.fromFile(inputPath.toFile, "UTF-8")
    try
      val lines = source.getLines().filter(_.trim.nonEmpty)
      limit.fold(lines)(lines.take).zipWithIndex.map: (line, index) =>
        Json.parse(line) match
          case obj: JsObject => obj
          case other =>
            throw IllegalArgumentException(s"${inputPath.getFileName} row ${index + 1} is not a JSON object: $other")
      .toVector
    finally source.close()

  private def enrichRow(obj: JsObject): Either[LowerDiagnosticMaterializedTransitionEnrichmentFailure, Enriched] =
    val rowId = stringAt(obj, "sampleId").orElse(stringAt(obj, "id")).getOrElse("unknown")
    val fen = stringAt(obj, "fen").orElse(stringAt(obj, "currentFen"))
    val playedUci = stringAt(obj, "playedUci").orElse(stringAt(obj, "playedMove"))
    val ply = intAt(obj, "ply")
    (fen, playedUci, ply) match
      case (Some(beforeFen), Some(uci), Some(_)) =>
        replay(beforeFen, uci) match
          case Right(afterFen) =>
            Right(
              Enriched(
                obj ++ Json.obj(
                  "decisionFen" -> beforeFen,
                  "beforeFen" -> beforeFen,
                  "playedMove" -> uci,
                  "currentFen" -> afterFen,
                  "transitionEnrichment" -> Json.obj(
                    "state" -> "enriched_from_current_fen",
                    "beforeFenSource" -> "fen",
                    "playedMoveSource" -> "playedUci",
                    "afterFenSource" -> "legal_replay",
                    "originalFenWasDecisionFen" -> true
                  )
                ),
                state = "enriched_from_current_fen"
              )
            )
          case Left(reason) =>
            Left(failure(rowId, "failed_replay", reason, fen, playedUci, ply))
      case _ =>
        Left(failure(rowId, "missing_required_field", "fen, playedUci, and ply are required", fen, playedUci, ply))

  private def replay(beforeFen: String, playedUci: String): Either[String, String] =
    Uci(playedUci) match
      case Some(move: Uci.Move) =>
        Fen.read(variant.Standard, Fen.Full.clean(beforeFen)) match
          case None => Left("invalid_current_fen")
          case Some(position) =>
            position.move(move) match
              case Left(reason) => Left(reason.toString)
              case Right(done)  => LowerDiagnosticMaterializedTransitionFen.afterFen(beforeFen, position, move, done)
      case Some(_) => Left("unsupported_non_move_uci")
      case None    => Left("invalid_uci")

  private def failure(
      rowId: String,
      state: String,
      reason: String,
      fen: Option[String],
      playedUci: Option[String],
      ply: Option[Int]
  ): LowerDiagnosticMaterializedTransitionEnrichmentFailure =
    LowerDiagnosticMaterializedTransitionEnrichmentFailure(rowId, state, reason, fen, playedUci, ply)

  private def stringAt(obj: JsObject, field: String): Option[String] =
    (obj \ field).asOpt[String].filter(_.trim.nonEmpty)

  private def intAt(obj: JsObject, field: String): Option[Int] =
    (obj \ field).asOpt[Int]

  private def countLabels(values: Vector[String]): Map[String, Int] =
    values.groupMapReduce(identity)(_ => 1)(_ + _).toVector.sortBy(_._1).toMap.withDefaultValue(0)

object LowerDiagnosticMaterializedTransitionEnrichmentJson:

  def summaryJson(summary: LowerDiagnosticMaterializedTransitionEnrichmentSummary): JsObject =
    Json.obj(
      "totalRows" -> summary.totalRows,
      "enrichedRows" -> summary.enrichedRows,
      "failedRows" -> summary.failedRows,
      "countsByState" -> summary.countsByState,
      "outputJsonl" -> summary.outputJsonl
    )

  def failureJson(failure: LowerDiagnosticMaterializedTransitionEnrichmentFailure): JsObject =
    Json.obj(
      "rowId" -> failure.rowId,
      "state" -> failure.state,
      "reason" -> failure.reason,
      "fen" -> failure.fen,
      "playedUci" -> failure.playedUci,
      "ply" -> failure.ply
    )

object LowerDiagnosticMaterializedTransitionEnrichmentRunner:

  private val defaultOutputDir = Paths.get("tmp/commentary-diagnostic/lower-layer/materialized300/enriched")

  def main(args: Array[String]): Unit =
    val options = RunnerOptions.parse(args.toVector)
    val outputJsonl = options.outputDir.resolve("materialized-transition-enriched.jsonl")
    val result = LowerDiagnosticMaterializedTransitionEnrichment.enrich(options.inputPath, outputJsonl, options.limit)
    write(result, options.outputDir, outputJsonl)

  def write(
      result: LowerDiagnosticMaterializedTransitionEnrichment.Result,
      outputDir: Path,
      outputJsonl: Path
  ): Unit =
    Files.createDirectories(outputDir)
    Files.writeString(
      outputJsonl,
      result.enrichedRows.map(Json.stringify).mkString("", System.lineSeparator(), System.lineSeparator()),
      StandardCharsets.UTF_8
    )
    Files.writeString(
      outputDir.resolve("enrichment-summary.json"),
      Json.prettyPrint(LowerDiagnosticMaterializedTransitionEnrichmentJson.summaryJson(result.summary)) + System.lineSeparator(),
      StandardCharsets.UTF_8
    )
      Files.writeString(
      outputDir.resolve("enrichment-failures.jsonl"),
      result.failures.map(LowerDiagnosticMaterializedTransitionEnrichmentJson.failureJson).map(Json.stringify).mkString("", System.lineSeparator(), System.lineSeparator()),
      StandardCharsets.UTF_8
    )

  private final case class RunnerOptions(
      inputPath: Path,
      outputDir: Path,
      limit: Option[Int]
  )

  private object RunnerOptions:
    def parse(args: Vector[String]): RunnerOptions =
      def valueAfter(flag: String): Option[String] =
        args.sliding(2).collectFirst { case Vector(`flag`, value) => value }
      RunnerOptions(
        inputPath = valueAfter("--input")
          .map(Paths.get(_))
          .getOrElse(throw IllegalArgumentException("--input is required")),
        outputDir = valueAfter("--out").map(Paths.get(_)).getOrElse(defaultOutputDir),
        limit = valueAfter("--limit").flatMap(_.toIntOption)
      )

private object LowerDiagnosticMaterializedTransitionFen:

  def afterFen(
      beforeFen: String,
      beforePosition: chess.Position,
      playedMove: Uci.Move,
      done: chess.Move
  ): Either[String, String] =
    for
      beforeClock <- fenClock(Fen.Full.clean(beforeFen))
      movingPiece <- beforePosition.board.pieceAt(playedMove.orig).toRight("missing transition moving piece")
      expectedHalfMove = if movingPiece.role == Pawn || done.capture.nonEmpty then 0 else beforeClock.halfMove + 1
      expectedFullMove = if beforeClock.sideToMove == "b" then beforeClock.fullMove + 1 else beforeClock.fullMove
      generated = Fen.write(done.after.position, FullMoveNumber(expectedFullMove)).value
      corrected <- replaceClocks(generated, expectedHalfMove, expectedFullMove)
    yield corrected

  private final case class FenClock(sideToMove: String, halfMove: Int, fullMove: Int)

  private def fenClock(fen: Fen.Full): Either[String, FenClock] =
    fen.value.split("\\s+").toList match
      case _ :: side :: _ :: _ :: halfMove :: fullMove :: Nil =>
        for
          half <- halfMove.toIntOption.toRight("invalid FEN halfmove clock")
          full <- fullMove.toIntOption.toRight("invalid FEN fullmove number")
        yield FenClock(side, half, full)
      case _ => Left("full FEN must include side, castling, en-passant, halfmove, and fullmove fields")

  private def replaceClocks(generatedFen: String, halfMove: Int, fullMove: Int): Either[String, String] =
    generatedFen.split("\\s+").toVector match
      case Vector(board, side, castling, enPassant, _, _) =>
        Right(Vector(board, side, castling, enPassant, halfMove.toString, fullMove.toString).mkString(" "))
      case _ => Left("generated after FEN is not a full FEN")
