package lila.commentary.tools.quality

import java.nio.file.{ Path, Paths }
import java.util.regex.Pattern

import play.api.libs.json.*
import lila.commentary.tools.review.CommentaryPlayerQcSupport

object MoveReviewLocalFactualFallbackCorpusAudit:

  import CommentaryPlayerQcSupport.*

  private val ExactFactualMode = "exact_factual"
  private val PlannerOwnedMode = "planner_owned"
  private val ForbiddenTerms =
    List("compensation", "pressure", "route", "plan", "simplifying", "exchange benefit", "advantage", "winning", "wins")

  final case class ForbiddenHit(sampleId: String, term: String, commentary: String)
  object ForbiddenHit:
    given Format[ForbiddenHit] = Json.format[ForbiddenHit]

  final case class Summary(
      beforeExactFactualRows: Int,
      afterExactFactualRows: Int,
      beforePlannerOwnedRows: Int,
      afterPlannerOwnedRows: Int,
      beforeLiteralCaptureFloorCount: Int,
      afterLiteralCaptureFloorCount: Int,
      beforeRoleAnchoredCaptureCount: Int,
      afterRoleAnchoredCaptureCount: Int,
      beforeEnPassantCount: Int,
      afterEnPassantCount: Int,
      beforePromotionCount: Int,
      afterPromotionCount: Int,
      beforePromotionCaptureCount: Int,
      afterPromotionCaptureCount: Int,
      beforeMaterialSupportCount: Int,
      afterMaterialSupportCount: Int,
      beforeTacticalMotifSupportCount: Int,
      afterTacticalMotifSupportCount: Int,
      beforeCoupledPvSupportCount: Int,
      afterCoupledPvSupportCount: Int,
      modeChangedSampleIds: List[String],
      missingAfterSampleIds: List[String],
      forbiddenHits: List[ForbiddenHit],
      acceptanceStatus: String
  )
  object Summary:
    given Format[Summary] = Json.format[Summary]

  final case class Report(summary: Summary)
  object Report:
    given Format[Report] = Json.format[Report]

  final case class Config(
      beforePath: Path = Paths.get("C:\\Codes\\CondensedChess\\tmp\\commentary-player-qc\\runs\\move_review_local_factual_v2\\before_outputs.jsonl"),
      afterPath: Path = Paths.get("C:\\Codes\\CondensedChess\\tmp\\commentary-player-qc\\runs\\move_review_local_factual_v2\\after_outputs.jsonl"),
      summaryJsonPath: Path = Paths.get("C:\\Codes\\CondensedChess\\tmp\\commentary-player-qc\\runs\\move_review_local_factual_v2\\local_factual_v2_audit.json"),
      summaryMarkdownPath: Path = Paths.get("C:\\Codes\\CondensedChess\\tmp\\commentary-player-qc\\runs\\move_review_local_factual_v2\\local_factual_v2_audit.md")
  )

  def build(before: List[MoveReviewOutputEntry], after: List[MoveReviewOutputEntry]): Report =
    val beforeCounts = Counts.from(before)
    val afterCounts = Counts.from(after)
    val beforeBySampleId = before.map(entry => entry.sampleId -> entry).toMap
    val afterBySampleId = after.map(entry => entry.sampleId -> entry).toMap
    val missingAfter =
      beforeBySampleId.keySet.diff(afterBySampleId.keySet).toList.sorted
    val modeChanged =
      beforeBySampleId.keySet
        .intersect(afterBySampleId.keySet)
        .toList
        .filter(sampleId => beforeBySampleId(sampleId).moveReviewFallbackMode != afterBySampleId(sampleId).moveReviewFallbackMode)
        .sorted
    val forbidden =
      after
        .filter(_.moveReviewFallbackMode == ExactFactualMode)
        .filter(isLocalFactualFallbackLike)
        .flatMap(forbiddenHits)
        .sortBy(hit => hit.sampleId -> hit.term)
    val acceptanceStatus =
      if modeChanged.nonEmpty ||
          missingAfter.nonEmpty ||
          forbidden.nonEmpty ||
          afterCounts.literalCaptureFloorCount > beforeCounts.literalCaptureFloorCount
      then "rejected"
      else if afterCounts.exactFactualRows == 0 then "coverage_insufficient"
      else "accepted"

    Report(
      Summary(
        beforeExactFactualRows = beforeCounts.exactFactualRows,
        afterExactFactualRows = afterCounts.exactFactualRows,
        beforePlannerOwnedRows = beforeCounts.plannerOwnedRows,
        afterPlannerOwnedRows = afterCounts.plannerOwnedRows,
        beforeLiteralCaptureFloorCount = beforeCounts.literalCaptureFloorCount,
        afterLiteralCaptureFloorCount = afterCounts.literalCaptureFloorCount,
        beforeRoleAnchoredCaptureCount = beforeCounts.roleAnchoredCaptureCount,
        afterRoleAnchoredCaptureCount = afterCounts.roleAnchoredCaptureCount,
        beforeEnPassantCount = beforeCounts.enPassantCount,
        afterEnPassantCount = afterCounts.enPassantCount,
        beforePromotionCount = beforeCounts.promotionCount,
        afterPromotionCount = afterCounts.promotionCount,
        beforePromotionCaptureCount = beforeCounts.promotionCaptureCount,
        afterPromotionCaptureCount = afterCounts.promotionCaptureCount,
        beforeMaterialSupportCount = beforeCounts.materialSupportCount,
        afterMaterialSupportCount = afterCounts.materialSupportCount,
        beforeTacticalMotifSupportCount = beforeCounts.tacticalMotifSupportCount,
        afterTacticalMotifSupportCount = afterCounts.tacticalMotifSupportCount,
        beforeCoupledPvSupportCount = beforeCounts.coupledPvSupportCount,
        afterCoupledPvSupportCount = afterCounts.coupledPvSupportCount,
        modeChangedSampleIds = modeChanged,
        missingAfterSampleIds = missingAfter,
        forbiddenHits = forbidden,
        acceptanceStatus = acceptanceStatus
      )
    )

  def main(args: Array[String]): Unit =
    val config = parseConfig(args.toList)
    val before =
      readJsonLines[MoveReviewOutputEntry](config.beforePath) match
        case Right(value) => value
        case Left(err) =>
          System.err.println(s"[move-review-local-factual-audit] failed to read before outputs `${config.beforePath}`: $err")
          sys.exit(1)
    val after =
      readJsonLines[MoveReviewOutputEntry](config.afterPath) match
        case Right(value) => value
        case Left(err) =>
          System.err.println(s"[move-review-local-factual-audit] failed to read after outputs `${config.afterPath}`: $err")
          sys.exit(1)
    val report = build(before, after)
    writeJson(config.summaryJsonPath, Json.toJson(report))
    writeText(config.summaryMarkdownPath, renderMarkdown(report))
    println(
      s"[move-review-local-factual-audit] ${report.summary.acceptanceStatus}; wrote `${config.summaryJsonPath}` and `${config.summaryMarkdownPath}`"
    )

  private final case class Counts(
      exactFactualRows: Int,
      plannerOwnedRows: Int,
      literalCaptureFloorCount: Int,
      roleAnchoredCaptureCount: Int,
      enPassantCount: Int,
      promotionCount: Int,
      promotionCaptureCount: Int,
      materialSupportCount: Int,
      tacticalMotifSupportCount: Int,
      coupledPvSupportCount: Int
  )

  private object Counts:
    def from(entries: List[MoveReviewOutputEntry]): Counts =
      val exactRows = entries.filter(_.moveReviewFallbackMode == ExactFactualMode)
      Counts(
        exactFactualRows = exactRows.size,
        plannerOwnedRows = entries.count(_.moveReviewFallbackMode == PlannerOwnedMode),
        literalCaptureFloorCount = exactRows.count(isLiteralCaptureFloor),
        roleAnchoredCaptureCount = exactRows.count(isRoleAnchoredCapture),
        enPassantCount = exactRows.count(entry => contains(entry, "en passant")),
        promotionCount = exactRows.count(entry => contains(entry, "promotes to a")),
        promotionCaptureCount = exactRows.count(entry => contains(entry, "captures the") && contains(entry, "promotes to a")),
        materialSupportCount = exactRows.count(entry => contains(entry, "The local material change is")),
        tacticalMotifSupportCount = exactRows.count(isTacticalMotifSupport),
        coupledPvSupportCount = exactRows.count(entry => contains(entry, "The checked line begins"))
      )

  private def parseConfig(args: List[String]): Config =
    Config(
      beforePath = args.headOption.map(Paths.get(_)).getOrElse(Config().beforePath),
      afterPath = args.lift(1).map(Paths.get(_)).getOrElse(Config().afterPath),
      summaryJsonPath = args.lift(2).map(Paths.get(_)).getOrElse(Config().summaryJsonPath),
      summaryMarkdownPath = args.lift(3).map(Paths.get(_)).getOrElse(Config().summaryMarkdownPath)
    )

  private def forbiddenHits(entry: MoveReviewOutputEntry): List[ForbiddenHit] =
    ForbiddenTerms.flatMap { term =>
      val pattern = Pattern.compile(s"(?i)\\b${Pattern.quote(term)}\\b")
      Option.when(pattern.matcher(entry.commentary).find())(ForbiddenHit(entry.sampleId, term, entry.commentary))
    }

  private def isLiteralCaptureFloor(entry: MoveReviewOutputEntry): Boolean =
    firstClaimSentence(entry.commentary).equalsIgnoreCase("This captures.")

  private def isLocalFactualFallbackLike(entry: MoveReviewOutputEntry): Boolean =
    firstClaimSentence(entry.commentary).startsWith("This ")

  private def isRoleAnchoredCapture(entry: MoveReviewOutputEntry): Boolean =
    val pattern = Pattern.compile("""(?i)\bThis captures the (pawn|knight|bishop|rook|queen)\b""")
    pattern.matcher(entry.commentary).find()

  private def isTacticalMotifSupport(entry: MoveReviewOutputEntry): Boolean =
    contains(entry, "It also gives check") ||
      contains(entry, "It also gives checkmate") ||
      contains(entry, "It also creates a fork") ||
      contains(entry, "It also creates a pin") ||
      contains(entry, "It also creates a skewer")

  private def contains(entry: MoveReviewOutputEntry, needle: String): Boolean =
    entry.commentary.toLowerCase.contains(needle.toLowerCase)

  private def firstClaimSentence(commentary: String): String =
    val withoutHeader =
      Option(commentary)
        .getOrElse("")
        .trim
        .replaceFirst("""(?s)^\d+\.(?:\.\.)?\s+[^:]+:\s*""", "")
        .trim
    withoutHeader.linesIterator.nextOption.getOrElse(withoutHeader).trim

  private def renderMarkdown(report: Report): String =
    val s = report.summary
    val forbidden =
      if s.forbiddenHits.isEmpty then "- forbidden hits: 0"
      else
        s.forbiddenHits
          .map(hit => s"- forbidden hit: `${hit.sampleId}` term `${hit.term}`")
          .mkString("\n")
    s"""# MoveReview Local Factual Fallback Corpus Audit
       |
       |- acceptance: `${s.acceptanceStatus}`
       |- exact-factual rows: before ${s.beforeExactFactualRows}, after ${s.afterExactFactualRows}
       |- planner-owned rows: before ${s.beforePlannerOwnedRows}, after ${s.afterPlannerOwnedRows}
       |- literal `This captures.` floors: before ${s.beforeLiteralCaptureFloorCount}, after ${s.afterLiteralCaptureFloorCount}
       |- role-anchored captures: before ${s.beforeRoleAnchoredCaptureCount}, after ${s.afterRoleAnchoredCaptureCount}
       |- en passant: before ${s.beforeEnPassantCount}, after ${s.afterEnPassantCount}
       |- promotion: before ${s.beforePromotionCount}, after ${s.afterPromotionCount}
       |- promotion-capture: before ${s.beforePromotionCaptureCount}, after ${s.afterPromotionCaptureCount}
       |- material support: before ${s.beforeMaterialSupportCount}, after ${s.afterMaterialSupportCount}
       |- tactical motif support: before ${s.beforeTacticalMotifSupportCount}, after ${s.afterTacticalMotifSupportCount}
       |- coupled PV support: before ${s.beforeCoupledPvSupportCount}, after ${s.afterCoupledPvSupportCount}
       |- mode-changed samples: ${if s.modeChangedSampleIds.isEmpty then "0" else s.modeChangedSampleIds.mkString(", ")}
       |- missing after samples: ${if s.missingAfterSampleIds.isEmpty then "0" else s.missingAfterSampleIds.mkString(", ")}
       |$forbidden
       |""".stripMargin
