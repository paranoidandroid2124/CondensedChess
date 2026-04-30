package lila.commentary.diagnostic

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path, Paths }

import chess.{ FullMoveNumber, Move, Pawn, Position, Replay }
import chess.format.Fen
import chess.format.pgn.{ Parser, PgnStr }
import play.api.libs.json.*

import scala.jdk.CollectionConverters.*

final case class LowerDiagnosticSourceVerificationSummary(
    totalRows: Int,
    uniqueTransitions: Int,
    verifiedTransitions: Int,
    unverifiedTransitions: Int,
    rowAmplification: Int,
    countsByState: Map[String, Int]
)

final case class LowerDiagnosticSourceVerificationRow(
    transitionKey: String,
    sourceRowIds: Vector[String],
    duplicateRows: Int,
    gameKey: Option[String],
    pgnPath: Option[String],
    ply: Int,
    playedMove: Option[String],
    sourceState: String,
    reason: String,
    materializedBeforeFen: Option[String],
    materializedCurrentFen: String,
    sourceBeforeFen: Option[String],
    sourceCurrentFen: Option[String],
    sourcePlayedMove: Option[String],
    nextAction: String
)

final case class LowerDiagnosticSourceVerificationReport(
    summary: LowerDiagnosticSourceVerificationSummary,
    rows: Vector[LowerDiagnosticSourceVerificationRow]
)

object LowerDiagnosticSourceVerificationReport:

  def fromRows(
      rows: Vector[LowerDiagnosticLargeCorpus.Row],
      pgnRoots: Vector[Path] = Vector.empty
  ): LowerDiagnosticSourceVerificationReport =
    val verificationRows = rows.groupBy(transitionKey).toVector.map((_, grouped) => verifyGroup(grouped, pgnRoots)).sortBy(_.transitionKey)
    LowerDiagnosticSourceVerificationReport(
      summary = LowerDiagnosticSourceVerificationSummary(
        totalRows = rows.size,
        uniqueTransitions = verificationRows.size,
        verifiedTransitions = verificationRows.count(_.sourceState == "verified"),
        unverifiedTransitions = verificationRows.count(_.sourceState != "verified"),
        rowAmplification = rows.size - verificationRows.size,
        countsByState = countLabels(verificationRows.map(_.sourceState))
      ),
      rows = verificationRows
    )

  private def verifyGroup(
      rows: Vector[LowerDiagnosticLargeCorpus.Row],
      pgnRoots: Vector[Path]
  ): LowerDiagnosticSourceVerificationRow =
    val first = rows.minBy(_.id)
    val pgnPath = first.metadata.get("pgnPath")
    val sourceRowIds = rows.map(_.id).distinct.sorted
    val base = Base(first, sourceRowIds, rows.size, pgnPath)
    duplicateConflict(rows) match
      case Some((state, reason)) =>
        row(base, state, reason, None)
      case None =>
        resolvePgnPath(pgnPath, pgnRoots) match
          case None =>
            row(base, "missing_pgn_path", "row has no pgnPath metadata", None)
          case Some(path) if !Files.exists(Paths.get(path)) =>
            row(base, "missing_pgn_file", "pgnPath does not exist in the current worktree", None)
          case Some(path) =>
            val pgn = Files.readString(Paths.get(path), StandardCharsets.UTF_8)
            LowerDiagnosticPgnReplay.snapshots(pgn) match
              case Left(reason) => row(base, "pgn_parse_failed", reason, None)
              case Right(snaps) =>
                snaps.find(_.ply == first.input.ply) match
                  case None => row(base, "pgn_ply_missing", "source PGN does not contain the requested ply", None)
                  case Some(source) =>
                    compareToSource(base, source)

  private final case class Base(
      row: LowerDiagnosticLargeCorpus.Row,
      sourceRowIds: Vector[String],
      duplicateRows: Int,
      pgnPath: Option[String]
  )

  private def compareToSource(
      base: Base,
      source: LowerDiagnosticPgnReplay.Snapshot
  ): LowerDiagnosticSourceVerificationRow =
    val materialized = base.row.input
    if materialized.beforeFen.map(cleanFen).contains(cleanFen(source.beforeFen)) &&
      materialized.playedMove.contains(source.playedMove) &&
      cleanFen(materialized.currentFen) == cleanFen(source.currentFen)
    then row(base, "verified", "source PGN node matches beforeFen, playedMove, and currentFen", Some(source))
    else if !materialized.beforeFen.map(cleanFen).contains(cleanFen(source.beforeFen)) then
      row(base, "before_fen_mismatch", "source PGN before FEN differs from materialized beforeFen", Some(source))
    else if !materialized.playedMove.contains(source.playedMove) then
      row(base, "played_move_mismatch", "source PGN played move differs from materialized playedMove", Some(source))
    else
      row(base, "current_fen_mismatch", "source PGN after FEN differs from materialized currentFen", Some(source))

  private def row(
      base: Base,
      state: String,
      reason: String,
      source: Option[LowerDiagnosticPgnReplay.Snapshot]
  ): LowerDiagnosticSourceVerificationRow =
    LowerDiagnosticSourceVerificationRow(
      transitionKey = transitionKey(base.row),
      sourceRowIds = base.sourceRowIds,
      duplicateRows = base.duplicateRows,
      gameKey = base.row.metadata.get("gameKey"),
      pgnPath = base.pgnPath,
      ply = base.row.input.ply,
      playedMove = base.row.input.playedMove,
      sourceState = state,
      reason = reason,
      materializedBeforeFen = base.row.input.beforeFen,
      materializedCurrentFen = base.row.input.currentFen,
      sourceBeforeFen = source.map(_.beforeFen),
      sourceCurrentFen = source.map(_.currentFen),
      sourcePlayedMove = source.map(_.playedMove),
      nextAction = nextAction(state)
    )

  private def transitionKey(row: LowerDiagnosticLargeCorpus.Row): String =
    val gameKey = row.metadata.getOrElse("gameKey", row.metadata.getOrElse("sourceId", row.id))
    val move = row.input.playedMove.orElse(row.metadata.get("playedUci")).getOrElse("none")
    s"$gameKey|ply:${row.input.ply}|move:$move"

  private def nextAction(state: String): String =
    state match
      case "verified" =>
        "Eligible for source-verified unique-transition audit."
      case "missing_pgn_path" | "missing_pgn_file" =>
        "Recover the source PGN path or regenerate this corpus from available PGN before source-authority acceptance."
      case "pgn_parse_failed" | "pgn_ply_missing" =>
        "Fix source parsing or gameKey/ply mapping before using this transition as source-verified evidence."
      case _ =>
        "Treat this transition as source-conflicted; do not use it for move-causal admission until the mismatch is resolved."

  private def cleanFen(fen: String): String = (Fen.Full.clean(fen): Fen.Full).value

  private def countLabels(values: Vector[String]): Map[String, Int] =
    values.groupMapReduce(identity)(_ => 1)(_ + _).toVector.sortBy(_._1).toMap.withDefaultValue(0)

  private def duplicateConflict(rows: Vector[LowerDiagnosticLargeCorpus.Row]): Option[(String, String)] =
    val checks = Vector(
      ("pgn_path_conflict", "pgnPath", rows.map(_.metadata.get("pgnPath").getOrElse("<missing>"))),
      ("before_fen_conflict", "beforeFen", rows.map(_.input.beforeFen.getOrElse("<missing>"))),
      ("current_fen_conflict", "currentFen", rows.map(_.input.currentFen)),
      ("played_move_conflict", "playedMove", rows.map(_.input.playedMove.getOrElse("<missing>")))
    )
    checks.collectFirst:
      case (state, label, values) if values.distinct.size > 1 =>
        state -> s"duplicate transitionKey contains conflicting $label values; source authority cannot be assigned to the group"

  private def resolvePgnPath(rawPath: Option[String], roots: Vector[Path]): Option[String] =
    rawPath.map: path =>
      val original = Paths.get(path)
      if Files.exists(original) then original.toString
      else
        roots
          .iterator
          .flatMap(root => suffixes(original).map(suffix => root.resolve(suffix)))
          .find(Files.exists(_))
          .map(_.toString)
          .getOrElse(path)

  private def suffixes(path: Path): Vector[Path] =
    val names = path.iterator().asScala.map(_.toString).toVector
    names.indices.toVector.map(index => Paths.get(names.drop(index).mkString(java.io.File.separator)))

object LowerDiagnosticPgnReplay:

  final case class Snapshot(
      ply: Int,
      beforeFen: String,
      playedMove: String,
      currentFen: String
  )

  def snapshots(pgn: String): Either[String, Vector[Snapshot]] =
    Parser.mainline(PgnStr(pgn)) match
      case Left(err) => Left(err.value)
      case Right(parsed) =>
        val game = parsed.toGame
        val replay = Replay.makeReplay(game, parsed.moves)
        val moves = replay.replay.chronoMoves
        val beforePositions = game.position +: moves.dropRight(1).map(_.after)
        val initialHalfMove = fenClock(Fen.write(game.position, FullMoveNumber(beforeFullMove(game.ply.value + 1))).value).map(_.halfMove)
        initialHalfMove.flatMap: half =>
          moves.zip(beforePositions).zipWithIndex
            .foldLeft[Either[String, (Vector[Snapshot], Int)]](Right(Vector.empty[Snapshot] -> half)):
              case (acc, ((move: Move, before), index)) =>
                acc.flatMap: (snapshots, beforeHalfMove) =>
                  val uci = move.toUci
                  val ply = game.ply.value + index + 1
                  val beforeFen = fenWithClocks(before, beforeFullMove(ply), beforeHalfMove)
                  val currentFen = LowerDiagnosticFenSupport.afterFen(beforeFen, before, uci, move).getOrElse(
                    Fen.write(move.after, FullMoveNumber(afterFullMove(beforeFullMove(ply), before))).value
                  )
                  val afterHalfMove =
                    if before.board.pieceAt(move.orig).exists(_.role == Pawn) || move.capture.nonEmpty then 0
                    else beforeHalfMove + 1
                  Right((snapshots :+ Snapshot(ply, beforeFen, uci.uci, currentFen), afterHalfMove))
              case (_, _) => Left("source PGN contains unsupported drop")
            .map(_._1)

  private final case class FenClock(halfMove: Int)

  private def fenClock(fen: String): Either[String, FenClock] =
    fen.split("\\s+").toList match
      case _ :: _ :: _ :: _ :: halfMove :: _ :: Nil =>
        halfMove.toIntOption.map(FenClock(_)).toRight("invalid FEN halfmove clock")
      case _ => Left("full FEN must include halfmove clock")

  private def fenWithClocks(position: Position, fullMove: Int, halfMove: Int): String =
    replaceClocks(Fen.write(position, FullMoveNumber(fullMove)).value, halfMove, fullMove)

  private def beforeFullMove(ply: Int): Int =
    math.max(1, (ply + 1) / 2)

  private def afterFullMove(beforeFullMove: Int, before: Position): Int =
    if before.color == chess.Color.Black then beforeFullMove + 1 else beforeFullMove

  private def replaceClocks(fen: String, halfMove: Int, fullMove: Int): String =
    fen.split("\\s+").toVector match
      case Vector(board, side, castling, enPassant, _, _) =>
        Vector(board, side, castling, enPassant, halfMove.toString, fullMove.toString).mkString(" ")
      case _ => fen

private object LowerDiagnosticFenSupport:

  def afterFen(
      beforeFen: String,
      beforePosition: Position,
      playedMove: chess.format.Uci.Move,
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

object LowerDiagnosticSourceVerificationJson:

  def summaryJson(summary: LowerDiagnosticSourceVerificationSummary): JsObject =
    Json.obj(
      "totalRows" -> summary.totalRows,
      "uniqueTransitions" -> summary.uniqueTransitions,
      "verifiedTransitions" -> summary.verifiedTransitions,
      "unverifiedTransitions" -> summary.unverifiedTransitions,
      "rowAmplification" -> summary.rowAmplification,
      "countsByState" -> summary.countsByState
    )

  def rowJson(row: LowerDiagnosticSourceVerificationRow): JsObject =
    Json.obj(
      "transitionKey" -> row.transitionKey,
      "sourceRowIds" -> row.sourceRowIds,
      "duplicateRows" -> row.duplicateRows,
      "gameKey" -> row.gameKey,
      "pgnPath" -> row.pgnPath,
      "ply" -> row.ply,
      "playedMove" -> row.playedMove,
      "sourceState" -> row.sourceState,
      "reason" -> row.reason,
      "materializedBeforeFen" -> row.materializedBeforeFen,
      "materializedCurrentFen" -> row.materializedCurrentFen,
      "sourceBeforeFen" -> row.sourceBeforeFen,
      "sourceCurrentFen" -> row.sourceCurrentFen,
      "sourcePlayedMove" -> row.sourcePlayedMove,
      "nextAction" -> row.nextAction
    )

object LowerDiagnosticSourceVerificationRunner:

  private val defaultOutputDir = Paths.get("tmp/commentary-diagnostic/lower-layer/source-verification")

  def main(args: Array[String]): Unit =
    val options = RunnerOptions.parse(args.toVector)
    val rows =
      options.inputPath match
        case Some(path) => LowerDiagnosticLargeCorpus.loadExternalRows(path, options.limit)
        case None       => LowerDiagnosticLargeCorpus.loadTrackedRows()
    write(LowerDiagnosticSourceVerificationReport.fromRows(rows, options.pgnRoots), options.outputDir)

  def write(report: LowerDiagnosticSourceVerificationReport, outputDir: Path): Unit =
    Files.createDirectories(outputDir)
    Files.writeString(
      outputDir.resolve("source-verification-summary.json"),
      Json.prettyPrint(LowerDiagnosticSourceVerificationJson.summaryJson(report.summary)) + System.lineSeparator(),
      StandardCharsets.UTF_8
    )
    Files.writeString(
      outputDir.resolve("source-verification-transitions.jsonl"),
      report.rows.map(LowerDiagnosticSourceVerificationJson.rowJson).map(Json.stringify).mkString("", System.lineSeparator(), System.lineSeparator()),
      StandardCharsets.UTF_8
    )

  private final case class RunnerOptions(outputDir: Path, inputPath: Option[Path], limit: Option[Int], pgnRoots: Vector[Path])

  private object RunnerOptions:
    def parse(args: Vector[String]): RunnerOptions =
      def valueAfter(flag: String): Option[String] =
        args.sliding(2).collectFirst { case Vector(`flag`, value) => value }
      RunnerOptions(
        outputDir = valueAfter("--out").map(Paths.get(_)).getOrElse(defaultOutputDir),
        inputPath = valueAfter("--input").map(Paths.get(_)),
        limit = valueAfter("--limit").flatMap(_.toIntOption),
        pgnRoots = args.zipWithIndex.collect {
          case ("--pgn-root", index) if args.lift(index + 1).nonEmpty => Paths.get(args(index + 1))
        }
      )
