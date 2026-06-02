package lila.commentary.tools.review

import java.nio.file.{ Files, Path, Paths }

import play.api.libs.json.*

import scala.collection.mutable

import lila.commentary.analysis.UserFacingSignalSanitizer

object CommentaryPlayerReviewQueueBuilder:

  import CommentaryPlayerQcSupport.*

  final case class Config(
      manifestPath: Path = DefaultManifestDir.resolve("slice_manifest.jsonl"),
      moveReviewOutputsPath: Path = DefaultMoveReviewRunDir.resolve("move_review_outputs.jsonl"),
      chronicleReportPath: Path = DefaultChronicleRunDir.resolve("report.json"),
      outPath: Path = DefaultReviewDir.resolve("review_queue.jsonl"),
      summaryPath: Path = DefaultReportDir.resolve("review_queue_summary.json"),
      auditSetPath: Option[Path] = None,
      fullReview: Boolean = false
  )

  def main(args: Array[String]): Unit =
    val config = parseConfig(args.toList)
    val (queue, summary) =
      config.auditSetPath match
        case Some(path) => buildAuditQueue(config, path)
        case None       => buildLegacyQueue(config)

    writeJsonLines(config.outPath, queue)
    writeJson(config.summaryPath, Json.toJson(summary))
    val mandatory = queue.count(entry => config.fullReview || isMandatoryReview(entry.sliceKind, entry.flags))
    println(s"[player-qc-queue] wrote `${config.outPath}` (reviewed=${queue.size}, mandatory=$mandatory)")

  private[tools] def buildAuditQueue(config: Config, auditSetPath: Path): (List[ReviewQueueEntry], ChronicleQueueReport) =
    val auditSet =
      readAuditSet(auditSetPath) match
        case Right(value) => value
        case Left(err) =>
          System.err.println(s"[player-qc-queue] failed to read audit set `${auditSetPath}`: $err")
          sys.exit(1)

    val reportCache = mutable.Map.empty[Path, RunReport]
    val queue =
      auditSet.games.flatMap { entry =>
        val reportPath = Paths.get(entry.reportPath)
        val runReport = reportCache.getOrElseUpdate(reportPath, readRunReportOrExit(reportPath))
        val game =
          runReport.games.find(_.id == entry.gameId).getOrElse {
            System.err.println(
              s"[player-qc-queue] audit set game `${entry.gameId}` missing from report `${entry.reportPath}`"
            )
            sys.exit(1)
          }
        val rawDir = Paths.get(entry.rawDir)
        val focusRows = game.focusMoments.flatMap(moment => buildAuditMomentEntries(entry, moment, rawDir, config.fullReview))
        focusRows
      }

    val moveReviewCount = queue.count(_.surface == ReviewSurface.MoveReview)
    val mandatory = queue.count(entry => config.fullReview || isMandatoryReview(entry.sliceKind, entry.flags))
    val summary =
      ChronicleQueueReport(
        version = 1,
        generatedAt = java.time.Instant.now().toString,
        moveReviewOutputCount = moveReviewCount,
        chronicleMomentCount = 0,
        wholeGameReviewCount = 0,
        mandatoryReviewCount = mandatory,
        sampledReviewCount = queue.size - mandatory,
        reviewedCount = queue.size,
        fullReview = config.fullReview,
        auditSetGameCount = auditSet.games.size
      )

    (queue, summary)

  private def buildLegacyQueue(config: Config): (List[ReviewQueueEntry], ChronicleQueueReport) =
    val manifest =
      readJsonLines[SliceManifestEntry](config.manifestPath) match
        case Right(value) => value
        case Left(err) =>
          System.err.println(s"[player-qc-queue] failed to read manifest `${config.manifestPath}`: $err")
          sys.exit(1)
    val moveReview =
      readJsonLines[MoveReviewOutputEntry](config.moveReviewOutputsPath) match
        case Right(value) => value.map(output => output.sampleId -> output).toMap
        case Left(err) =>
          System.err.println(s"[player-qc-queue] failed to read moveReview outputs `${config.moveReviewOutputsPath}`: $err")
          sys.exit(1)

    val queue =
      manifest.flatMap { entry =>
        entry.surface match
          case ReviewSurface.MoveReview =>
            moveReview.get(entry.sampleId).flatMap { output =>
              val flags = reviewFlags(output.commentary, output.supportRows, output.advancedRows, output.sliceKind)
              val include = config.fullReview || isMandatoryReview(output.sliceKind, flags) || sampleByHash(output.sampleId)
              Option.when(include) {
                ReviewQueueEntry(
                  sampleId = output.sampleId,
                  gameId = output.gameKey,
                  surface = ReviewSurface.MoveReview,
                  reviewKind = ReviewKind.MoveReviewFocus,
                  sliceKind = output.sliceKind,
                  fen = output.fen,
                  playedSan = output.playedSan,
                  mainProse = output.commentary,
                  supportRows = flattenRows(output.supportRows),
                  advancedRows = flattenRows(output.advancedRows),
                  flags = flags
                )
              }
            }
          case _ => None
      }

    val mandatory = queue.count(entry => config.fullReview || isMandatoryReview(entry.sliceKind, entry.flags))
    val summary =
      ChronicleQueueReport(
        version = 1,
        generatedAt = java.time.Instant.now().toString,
        moveReviewOutputCount = moveReview.size,
        chronicleMomentCount = 0,
        wholeGameReviewCount = 0,
        mandatoryReviewCount = mandatory,
        sampledReviewCount = queue.size - mandatory,
        reviewedCount = queue.size,
        fullReview = config.fullReview,
        auditSetGameCount = 0
      )

    (queue, summary)

  private def buildAuditMomentEntries(
      entry: AuditSetEntry,
      moment: FocusMomentReport,
      rawDir: Path,
      fullReview: Boolean
  ): List[ReviewQueueEntry] =
    val moveReviewRowPayload = moveReviewPayload(entry.gameId, moment, rawDir)
    val moveReviewSampleId = s"${entry.auditId}:${moment.ply}:moveReview"
    val moveReviewFlags =
      reviewFlags(
        moveReviewRowPayload.commentary,
        moveReviewRowPayload.supportRows,
        moveReviewRowPayload.advancedRows,
        WholeGameSliceKind.MoveReviewFocus
      )
    val moveReviewEntry =
      Option.when(fullReview || isMandatoryReview(WholeGameSliceKind.MoveReviewFocus, moveReviewFlags) || sampleByHash(moveReviewSampleId)) {
        ReviewQueueEntry(
          sampleId = moveReviewSampleId,
          auditId = Some(entry.auditId),
          gameId = entry.gameId,
          surface = ReviewSurface.MoveReview,
          reviewKind = ReviewKind.MoveReviewFocus,
          sliceKind = WholeGameSliceKind.MoveReviewFocus,
          tier = Some(entry.tier),
          openingFamily = Some(entry.openingFamily),
          label = Some(entry.label),
          pairedSampleId = None,
          fen = momentFen(rawDir, entry.gameId, moment.ply).getOrElse(""),
          playedSan = "",
          mainProse = moveReviewRowPayload.commentary,
          supportRows = flattenRows(moveReviewRowPayload.supportRows),
          advancedRows = flattenRows(moveReviewRowPayload.advancedRows),
          flags = moveReviewFlags
        )
      }

    moveReviewEntry.toList

  private final case class MoveReviewPayload(
      commentary: String,
      supportRows: List[SupportRow],
      advancedRows: List[SupportRow]
  )

  private def moveReviewPayload(gameId: String, moment: FocusMomentReport, rawDir: Path): MoveReviewPayload =
    val rawMoveReviewPath = rawDir.resolve(s"${gameId}.ply_${moment.ply}.move_review.json")
    if !Files.exists(rawMoveReviewPath) then
      MoveReviewPayload(
        commentary = moment.moveReviewCommentary,
        supportRows = Nil,
        advancedRows = Nil
      )
    else
      val js = Json.parse(Files.readString(rawMoveReviewPath))
      val commentary = (js \ "commentary").asOpt[String].getOrElse(moment.moveReviewCommentary)
      val (support, advanced) = moveReviewRowsFromJson(js)
      MoveReviewPayload(commentary = commentary, supportRows = support, advancedRows = advanced)

  private def moveReviewRowsFromJson(js: JsValue): (List[SupportRow], List[SupportRow]) =
    moveReviewPlayerSurfaceRowsFromJson(js).getOrElse((Nil, Nil))

  private def moveReviewPlayerSurfaceRowsFromJson(js: JsValue): Option[(List[SupportRow], List[SupportRow])] =
    val surface = js \ "moveReviewPlayerSurface"
    val schema = (surface \ "schema").asOpt[String]
    Option.when(schema.exists(value =>
      value == "chesstory.move_review.player_surface.v1" ||
        value == "chesstory.move_review.player_surface.v2"
    )) {
      val support =
        rowsFromJson(surface \ "summaryRows")
      val advanced =
        rowsFromJson(surface \ "advancedRows") ++
          rowsFromJson(surface \ "probeRows") ++
          authorRowsFromJson(surface \ "authorRows")
      (support.distinct, advanced.distinct)
    }

  private def rowsFromJson(value: JsLookupResult): List[SupportRow] =
    value.asOpt[List[JsObject]].getOrElse(Nil).flatMap { row =>
      for
        label <- (row \ "label").asOpt[String].map(_.trim).filter(_.nonEmpty)
        text <- (row \ "text").asOpt[String].flatMap(sanitizeSurfaceText)
      yield SupportRow(label, text)
    }

  private def authorRowsFromJson(value: JsLookupResult): List[SupportRow] =
    value.asOpt[List[JsObject]].getOrElse(Nil).flatMap { row =>
      val label = (row \ "title").asOpt[String].map(_.trim).filter(_.nonEmpty).getOrElse("Author check")
      val branchText =
        (row \ "branches").asOpt[List[JsObject]].getOrElse(Nil).flatMap { branch =>
          val branchLabel = (branch \ "label").asOpt[String].map(_.trim).filter(_.nonEmpty)
          (branch \ "text").asOpt[String].flatMap(sanitizeSurfaceText).map { text =>
            branchLabel.fold(text)(value => s"$value: $text")
          }
        }
      val text =
        (List((row \ "question").asOpt[String], (row \ "why").asOpt[String]).flatten ++ branchText)
          .flatMap(sanitizeSurfaceText)
          .distinct
          .mkString("; ")
      Option.when(text.nonEmpty)(SupportRow(label, text))
    }

  private def sanitizeSurfaceText(raw: String): Option[String] =
    Option(raw)
      .map(UserFacingSignalSanitizer.sanitize)
      .map(_.trim)
      .filter(_.nonEmpty)

  private def momentFen(rawDir: Path, gameId: String, ply: Int): Option[String] =
    val rawGamePath = rawDir.resolve(s"${gameId}.game_arc.json")
    if !Files.exists(rawGamePath) then None
    else
      val js = Json.parse(Files.readString(rawGamePath))
      (js \ "moments")
        .asOpt[List[JsObject]]
        .getOrElse(Nil)
        .find(moment => (moment \ "ply").asOpt[Int].contains(ply))
        .flatMap(moment => (moment \ "fen").asOpt[String])

  private def readAuditSet(path: Path): Either[String, AuditSetManifest] =
    try
      Json.parse(Files.readString(path)).validate[AuditSetManifest].asEither.left.map(_.toString)
    catch case err: Exception => Left(err.getMessage)

  private def readRunReportOrExit(path: Path): RunReport =
    Json
      .parse(Files.readString(path))
      .validate[RunReport]
      .asEither match
      case Right(value) => value
      case Left(err) =>
        System.err.println(s"[player-qc-queue] failed to parse chronicle report `${path}`: $err")
        sys.exit(1)

  private def parseConfig(args: List[String]): Config =
    @annotation.tailrec
    def loop(
        rest: List[String],
        positional: List[String],
        auditSetPath: Option[Path],
        fullReview: Boolean
    ): (List[String], Option[Path], Boolean) =
      rest match
        case "--audit-set" :: value :: tail => loop(tail, positional, Some(Paths.get(value)), fullReview)
        case "--full-review" :: tail        => loop(tail, positional, auditSetPath, true)
        case "--no-sampling" :: tail        => loop(tail, positional, auditSetPath, true)
        case head :: tail if head.startsWith("--") =>
          loop(tail, positional, auditSetPath, fullReview)
        case head :: tail =>
          loop(tail, positional :+ head, auditSetPath, fullReview)
        case Nil => (positional, auditSetPath, fullReview)

    val (positional, auditSetPath, fullReview) = loop(args, Nil, None, false)
    auditSetPath match
      case Some(path) =>
        Config(
          outPath = positional.headOption.map(Paths.get(_)).getOrElse(DefaultReviewDir.resolve("review_queue.jsonl")),
          summaryPath = positional.lift(1).map(Paths.get(_)).getOrElse(DefaultReportDir.resolve("review_queue_summary.json")),
          auditSetPath = Some(path),
          fullReview = fullReview
        )
      case None =>
        Config(
          manifestPath = positional.headOption.map(Paths.get(_)).getOrElse(DefaultManifestDir.resolve("slice_manifest.jsonl")),
          moveReviewOutputsPath =
            positional.lift(1).map(Paths.get(_)).getOrElse(DefaultMoveReviewRunDir.resolve("move_review_outputs.jsonl")),
          chronicleReportPath =
            positional.lift(2).map(Paths.get(_)).getOrElse(DefaultChronicleRunDir.resolve("report.json")),
          outPath = positional.lift(3).map(Paths.get(_)).getOrElse(DefaultReviewDir.resolve("review_queue.jsonl")),
          summaryPath = positional.lift(4).map(Paths.get(_)).getOrElse(DefaultReportDir.resolve("review_queue_summary.json")),
          auditSetPath = None,
          fullReview = fullReview
        )
