package lila.commentary.tools.quality

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path, Paths }

import play.api.libs.json.*
import scala.jdk.CollectionConverters.*

import _root_.chess.format.{ Fen, Uci }
import _root_.chess.variant.Standard
import lila.commentary.tools.review.CommentaryPlayerQcSupport

object MoveReviewEvidenceCoverageAudit:

  import CommentaryPlayerQcSupport.*

  private val ExactFactualFallback = "exact_factual_fallback"
  private val CoverageInsufficient = "coverage_insufficient"
  private val CoverageComplete = "complete"
  private val EvalGapWithoutConcreteDescriptor = "eval_gap_without_concrete_descriptor"

  final case class RankedCandidate(label: String, count: Int, sampleIds: List[String])
  object RankedCandidate:
    given Format[RankedCandidate] = Json.format[RankedCandidate]

  final case class Summary(
      totalRows: Int,
      coverageStatus: String,
      sourceKindCounts: Map[String, Int],
      exactFactualRows: Int,
      basicEvidenceStatusCounts: Map[String, Int],
      basicEvidenceRejectReasonCounts: Map[String, Int],
      supportedLocalCandidateFamilyCounts: Map[String, Int],
      supportedLocalAdmittedFamilyCounts: Map[String, Int],
      supportedLocalRejectReasonCounts: Map[String, Int],
      supportedLocalRejectBucketCounts: Map[String, Int],
      counterplayBreakRowCount: Int,
      counterplayBreakNamedTokenRowCount: Int,
      counterplayBreakGenericFallbackCount: Int,
      counterplayBreakPlayedMoveCollisionCount: Int,
      centralBreakRowCount: Int,
      centralBreakNamedTokenRowCount: Int,
      centralBreakGenericFallbackCount: Int,
      centralBreakDiagonalCaptureVisibleCount: Int,
      evalOnlyDescriptorGapSampleIds: List[String],
      basicExpansionCandidateSampleIds: List[String],
      supportedLocalRuntimeCandidateSampleIds: List[String],
      supportedLocalEvidenceGapSampleIds: List[String],
      runtimeExpansionCandidates: List[RankedCandidate],
      evidenceGapCandidates: List[RankedCandidate]
  )
  object Summary:
    given Format[Summary] = Json.format[Summary]

  final case class Report(summary: Summary)
  object Report:
    given Format[Report] = Json.format[Report]

  final case class Config(
      inputPath: Path = CommentaryPlayerQcSupport.DefaultMoveReviewRunDir.resolve("move_review_outputs.jsonl"),
      summaryJsonPath: Path = CommentaryPlayerQcSupport.DefaultMoveReviewRunDir.resolve("move_review_evidence_coverage_audit.json"),
      summaryMarkdownPath: Path = CommentaryPlayerQcSupport.DefaultMoveReviewRunDir.resolve("move_review_evidence_coverage_audit.md")
  )

  def build(entries: List[MoveReviewOutputEntry]): Report =
    val coveredEntries =
      entries.filter(entry => entry.moveReviewSourceKind.nonEmpty && entry.basicEvidenceStatus.nonEmpty)
    val coverageStatus =
      if entries.isEmpty || coveredEntries.size != entries.size then CoverageInsufficient
      else CoverageComplete
    val sourceCounts = countValues(coveredEntries.flatMap(_.moveReviewSourceKind))
    val exactRows =
      coveredEntries.filter(_.moveReviewSourceKind.contains(ExactFactualFallback))
    val basicCandidates =
      exactRows.filter(basicRuntimeExpansionCandidate).sortBy(_.sampleId)
    val evalOnlyDescriptorGaps =
      exactRows.filter(evalOnlyDescriptorGap).sortBy(_.sampleId)
    val basicCandidateIds = basicCandidates.map(_.sampleId)
    val basicCandidateSet = basicCandidateIds.toSet
    val supportedRuntimeCandidates =
      exactRows
        .filterNot(entry => basicCandidateSet.contains(entry.sampleId))
        .filter(entry => entry.supportedLocalCandidateFamilies.nonEmpty && entry.supportedLocalAdmittedFamilies.nonEmpty)
        .sortBy(_.sampleId)
    val supportedRuntimeSet = supportedRuntimeCandidates.map(_.sampleId).toSet
    val supportedEvidenceGaps =
      exactRows
        .filterNot(entry => basicCandidateSet.contains(entry.sampleId))
        .filterNot(entry => supportedRuntimeSet.contains(entry.sampleId))
        .filter(entry =>
          entry.supportedLocalCandidateFamilies.nonEmpty &&
            entry.supportedLocalAdmittedFamilies.isEmpty &&
            entry.supportedLocalRejectReasons.nonEmpty
        )
        .sortBy(_.sampleId)
    val counterplayBreakRows =
      coveredEntries.flatMap(entry =>
        entry.supportRows
          .filter(row => row.label == "Counterplay break")
          .map(row => entry -> row)
      )
    val counterplayBreakGenericFallbacks =
      counterplayBreakRows.filter { case (_, row) => genericCounterplayBreakText(row.text) }
    val counterplayBreakPlayedMoveCollisions =
      counterplayBreakRows.filter { case (entry, row) => counterplayBreakPlayedMoveCollision(entry, row.text) }
    val counterplayBreakNamedTokenRows =
      counterplayBreakRows.filter { case (entry, row) =>
        counterplayBreakSurfaceToken(row.text).nonEmpty &&
          !genericCounterplayBreakText(row.text) &&
          !counterplayBreakPlayedMoveCollision(entry, row.text)
      }
    val centralBreakRows =
      coveredEntries.flatMap(entry =>
        entry.supportRows
          .filter(row => row.label == "Central break")
          .map(row => entry -> row)
      )
    val centralBreakGenericFallbacks =
      centralBreakRows.filter { case (_, row) => genericCentralBreakText(row.text) }
    val centralBreakDiagonalCaptures =
      centralBreakRows.filter { case (_, row) =>
        centralBreakSurfaceToken(row.text).exists(centralBreakDiagonalCaptureToken)
      }
    val centralBreakNamedTokenRows =
      centralBreakRows.filter { case (_, row) =>
        centralBreakSurfaceToken(row.text).exists(coreCentralBreakRouteToken) &&
          !genericCentralBreakText(row.text)
      }

    Report(
      Summary(
        totalRows = entries.size,
        coverageStatus = coverageStatus,
        sourceKindCounts = sourceCounts,
        exactFactualRows = sourceCounts.getOrElse(ExactFactualFallback, 0),
        basicEvidenceStatusCounts = countValues(coveredEntries.flatMap(_.basicEvidenceStatus)),
        basicEvidenceRejectReasonCounts = countValues(coveredEntries.flatMap(_.basicEvidenceRejectReasons)),
        supportedLocalCandidateFamilyCounts = countValues(coveredEntries.flatMap(_.supportedLocalCandidateFamilies)),
        supportedLocalAdmittedFamilyCounts = countValues(coveredEntries.flatMap(_.supportedLocalAdmittedFamilies)),
        supportedLocalRejectReasonCounts = countValues(coveredEntries.flatMap(_.supportedLocalRejectReasons)),
        supportedLocalRejectBucketCounts =
          countValues(coveredEntries.flatMap(_.supportedLocalRejectReasons).map(supportedLocalRejectBucket)),
        counterplayBreakRowCount = counterplayBreakRows.size,
        counterplayBreakNamedTokenRowCount = counterplayBreakNamedTokenRows.size,
        counterplayBreakGenericFallbackCount = counterplayBreakGenericFallbacks.size,
        counterplayBreakPlayedMoveCollisionCount = counterplayBreakPlayedMoveCollisions.size,
        centralBreakRowCount = centralBreakRows.size,
        centralBreakNamedTokenRowCount = centralBreakNamedTokenRows.size,
        centralBreakGenericFallbackCount = centralBreakGenericFallbacks.size,
        centralBreakDiagonalCaptureVisibleCount = centralBreakDiagonalCaptures.size,
        evalOnlyDescriptorGapSampleIds = evalOnlyDescriptorGaps.map(_.sampleId),
        basicExpansionCandidateSampleIds = basicCandidateIds,
        supportedLocalRuntimeCandidateSampleIds = supportedRuntimeCandidates.map(_.sampleId),
        supportedLocalEvidenceGapSampleIds = supportedEvidenceGaps.map(_.sampleId),
        runtimeExpansionCandidates =
          rankedCandidates(
            basicCandidates.flatMap(entry => entry.basicEvidenceRejectReasons.map(reason => s"basic:$reason").map(_ -> entry.sampleId)) ++
              supportedRuntimeCandidates.flatMap(entry =>
                entry.supportedLocalAdmittedFamilies.map(family => s"supportedLocal:$family" -> entry.sampleId)
              )
          ),
        evidenceGapCandidates =
          rankedCandidates(
            exactRows.filterNot(entry => basicCandidateSet.contains(entry.sampleId)).flatMap(entry =>
              entry.basicEvidenceRejectReasons
                .filter(hardBasicEvidenceGap)
                .map(reason => s"basic:$reason" -> entry.sampleId)
            ) ++
              supportedEvidenceGaps.flatMap(entry =>
                entry.supportedLocalRejectReasons.map(reason => s"supportedLocal:${dropFamilyPrefix(reason)}" -> entry.sampleId)
              )
          )
      )
    )

  def main(args: Array[String]): Unit =
    val config = parseConfig(args.toList)
    val entries =
      readJsonLines[MoveReviewOutputEntry](config.inputPath) match
        case Right(value) => value
        case Left(err) =>
          System.err.println(s"[move-review-evidence-coverage-audit] failed to read `${config.inputPath}`: $err")
          sys.exit(1)
    val report = build(entries)
    writeJson(config.summaryJsonPath, Json.toJson(report))
    writeText(config.summaryMarkdownPath, renderMarkdown(report))
    println(
      s"[move-review-evidence-coverage-audit] ${report.summary.coverageStatus}; wrote `${config.summaryJsonPath}` and `${config.summaryMarkdownPath}`"
    )

  private def parseConfig(args: List[String]): Config =
    Config(
      inputPath = args.headOption.map(Paths.get(_)).getOrElse(Config().inputPath),
      summaryJsonPath = args.lift(1).map(Paths.get(_)).getOrElse(Config().summaryJsonPath),
      summaryMarkdownPath = args.lift(2).map(Paths.get(_)).getOrElse(Config().summaryMarkdownPath)
    )

  private def basicRuntimeExpansionCandidate(entry: MoveReviewOutputEntry): Boolean =
    entry.basicEvidenceStatus.contains("blocked") &&
      !evalOnlyDescriptorGap(entry) &&
      (
        entry.basicEvidenceRejectReasons.contains("after_pv_projection_would_admit_basic") ||
          entry.basicEvidenceRejectReasons.contains("before_pv_not_seeded_by_played_move") ||
          (
            entry.basicEvidenceRejectReasons.contains("no_descriptor_rule_matched") &&
              !entry.basicEvidenceRejectReasons.exists(hardBasicEvidenceGap)
          )
      )

  private def hardBasicEvidenceGap(reason: String): Boolean =
    reason == "missing_current_move" ||
      reason == "missing_coupled_pv_line" ||
      reason == "after_pv_projection_replay_failed" ||
      reason == "coupled_pv_replay_failed"

  private def evalOnlyDescriptorGap(entry: MoveReviewOutputEntry): Boolean =
    entry.basicEvidenceRejectReasons.contains(EvalGapWithoutConcreteDescriptor)

  private def countValues(values: Iterable[String]): Map[String, Int] =
    values
      .filter(_.trim.nonEmpty)
      .groupBy(identity)
      .view
      .mapValues(_.size)
      .toMap

  private def rankedCandidates(values: Iterable[(String, String)]): List[RankedCandidate] =
    values
      .filter { case (label, sampleId) => label.trim.nonEmpty && sampleId.trim.nonEmpty }
      .groupBy(_._1)
      .view
      .map { case (label, rows) =>
        RankedCandidate(
          label = label,
          count = rows.map(_._2).toSet.size,
          sampleIds = rows.map(_._2).toSet.toList.sorted.take(20)
        )
      }
      .toList
      .sortBy(candidate => (-candidate.count, candidate.label))

  private def supportedLocalRejectBucket(reason: String): String =
    val code = dropFamilyPrefix(reason)
    if code.startsWith("contract:source") then "source"
    else if code.startsWith("surface:") then "surface"
    else if code.startsWith("contract:scope") || code.startsWith("policy:") then "surface"
    else if code.startsWith("contract:") then "contract"
    else if code.startsWith("witness:") || code.startsWith("rival:") then "witness"
    else if code.startsWith("planner_owner:") then "planner_owner"
    else "other"

  private def dropFamilyPrefix(reason: String): String =
    reason.indexOf(':') match
      case idx if idx >= 0 && idx + 1 < reason.length => reason.substring(idx + 1)
      case _                                          => reason

  private def genericCounterplayBreakText(text: String): Boolean =
    val low = text.trim.toLowerCase
    low.contains("from coming right away") ||
      low.contains("material threat before it lands")

  private def counterplayBreakSurfaceToken(text: String): Option[String] =
    val pattern =
      """(?i)\bstops\s+the\s+((?:\.\.\.)?[a-h][1-8](?:-[a-h][1-8])?)\s+break\s+before\s+it\s+appears\b""".r
    pattern.findFirstMatchIn(text).map(_.group(1).toLowerCase)

  private def genericCentralBreakText(text: String): Boolean =
    val low = text.trim.toLowerCase
    low.contains("local reading") ||
      low.contains("strategic point") ||
      low.contains("central_break_timing") ||
      """(?i)\b(?:\.\.\.)?[de]-break\b""".r.findFirstIn(text).nonEmpty

  private def centralBreakSurfaceToken(text: String): Option[String] =
    val pattern =
      """(?i)\b(?:also\s+plays|also\s+leaves|uses|keeps)\s+the\s+((?:\.\.\.)?[de][1-8]-[de][1-8])\s+break\b""".r
    pattern.findFirstMatchIn(text).map(_.group(1).toLowerCase)

  private def coreCentralBreakRouteToken(token: String): Boolean =
    routeSquares(token).exists { case (from, to) =>
      from.take(1) == to.take(1) &&
        Set("d", "e").contains(from.take(1)) &&
        Set("d4", "e4", "d5", "e5").contains(to)
    }

  private def centralBreakDiagonalCaptureToken(token: String): Boolean =
    routeSquares(token).exists { case (from, to) =>
      Set("d", "e").contains(from.take(1)) &&
        Set("d", "e").contains(to.take(1)) &&
        from.take(1) != to.take(1)
    }

  private def routeSquares(token: String): Option[(String, String)] =
    val core = token.stripPrefix("...")
    core.split("-", 2).toList match
      case from :: to :: Nil
          if from.matches("""[a-h][1-8]""") && to.matches("""[a-h][1-8]""") =>
        Some(from -> to)
      case _ => None

  private def counterplayBreakPlayedMoveCollision(
      entry: MoveReviewOutputEntry,
      text: String
  ): Boolean =
    counterplayBreakSurfaceToken(text).flatMap(singleSquareToken).exists { square =>
      legalPlayedTargetSquare(entry).contains(square)
    }

  private def singleSquareToken(token: String): Option[String] =
    val core = token.stripPrefix("...")
    Option.when(core.matches("""[a-h][1-8]"""))(core)

  private def legalPlayedTargetSquare(entry: MoveReviewOutputEntry): Option[String] =
    for
      position <- Fen.read(Standard, Fen.Full(entry.fen))
      uci <- Option(entry.playedUci).map(_.trim.toLowerCase).filter(_.nonEmpty)
      move <- Uci(uci).collect { case move: Uci.Move => move }.flatMap(position.move(_).toOption)
    yield move.dest.key

  private def renderMarkdown(report: Report): String =
    val s = report.summary
    s"""# MoveReview Evidence Coverage Audit
       |
       |- coverage: `${s.coverageStatus}`
       |- rows: ${s.totalRows}
       |- source kinds: ${renderCounts(s.sourceKindCounts)}
       |- exact factual rows: ${s.exactFactualRows}
       |- basic status: ${renderCounts(s.basicEvidenceStatusCounts)}
       |- basic reject reasons: ${renderCounts(s.basicEvidenceRejectReasonCounts)}
       |- SupportedLocal candidate families: ${renderCounts(s.supportedLocalCandidateFamilyCounts)}
       |- SupportedLocal admitted families: ${renderCounts(s.supportedLocalAdmittedFamilyCounts)}
       |- SupportedLocal reject buckets: ${renderCounts(s.supportedLocalRejectBucketCounts)}
       |- Counterplay break rows: ${s.counterplayBreakRowCount}
       |- Counterplay break named-token rows: ${s.counterplayBreakNamedTokenRowCount}
       |- Counterplay break generic fallback rows: ${s.counterplayBreakGenericFallbackCount}
       |- Counterplay break played-move collision rows: ${s.counterplayBreakPlayedMoveCollisionCount}
       |- Central break rows: ${s.centralBreakRowCount}
       |- Central break named-token rows: ${s.centralBreakNamedTokenRowCount}
       |- Central break generic fallback rows: ${s.centralBreakGenericFallbackCount}
       |- Central break diagonal-capture visible rows: ${s.centralBreakDiagonalCaptureVisibleCount}
       |- eval-only fail-closed rows: ${renderIds(s.evalOnlyDescriptorGapSampleIds)}
       |- basic expansion candidates: ${renderIds(s.basicExpansionCandidateSampleIds)}
       |- SupportedLocal runtime candidates: ${renderIds(s.supportedLocalRuntimeCandidateSampleIds)}
       |- SupportedLocal evidence gaps: ${renderIds(s.supportedLocalEvidenceGapSampleIds)}
       |
       |## Runtime Expansion Candidates
       |${renderRanked(s.runtimeExpansionCandidates)}
       |
       |## Evidence Gap Candidates
       |${renderRanked(s.evidenceGapCandidates)}
       |""".stripMargin

  private def renderCounts(counts: Map[String, Int]): String =
    if counts.isEmpty then "none"
    else counts.toList.sortBy { case (key, value) => (-value, key) }.map { case (key, value) => s"`$key`=$value" }.mkString(", ")

  private def renderIds(ids: List[String]): String =
    if ids.isEmpty then "none" else ids.take(20).mkString(", ")

  private def renderRanked(candidates: List[RankedCandidate]): String =
    if candidates.isEmpty then "- none"
    else
      candidates.take(20).map { candidate =>
        s"- `${candidate.label}`: ${candidate.count} (${candidate.sampleIds.take(5).mkString(", ")})"
      }.mkString("\n")

  private def readJsonLines[A: Reads](path: Path): Either[String, List[A]] =
    if !Files.exists(path) then Left("file does not exist")
    else
      val parsed =
        Files.readAllLines(path, StandardCharsets.UTF_8).asScala.toList.zipWithIndex
          .filter { case (line, _) => line.trim.nonEmpty }
          .map { case (line, idx) =>
            Json.parse(line).validate[A].asEither.left.map(err => s"line ${idx + 1}: $err")
          }
      parsed.collectFirst { case Left(err) => err } match
        case Some(err) => Left(err)
        case None      => Right(parsed.collect { case Right(value) => value })

  private def writeJson(path: Path, value: JsValue): Unit =
    Option(path.getParent).foreach(Files.createDirectories(_))
    Files.writeString(path, Json.prettyPrint(value), StandardCharsets.UTF_8)

  private def writeText(path: Path, text: String): Unit =
    Option(path.getParent).foreach(Files.createDirectories(_))
    Files.writeString(path, text, StandardCharsets.UTF_8)
