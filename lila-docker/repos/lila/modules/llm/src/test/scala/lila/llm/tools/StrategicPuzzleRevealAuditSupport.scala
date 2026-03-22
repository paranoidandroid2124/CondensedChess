package lila.llm.tools

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path }
import java.time.Instant

import scala.jdk.CollectionConverters.*

import play.api.libs.json.*

import lila.llm.analysis.{ CommentaryPayloadNormalizer, LiveNarrativeCompressionCore }
import lila.strategicPuzzle.StrategicPuzzle.{ StrategicPuzzleDoc, TerminalReveal }

object StrategicPuzzleRevealAuditSupport:

  final case class RevealIssue(code: String, detail: String)
  object RevealIssue:
    given Writes[RevealIssue] = Json.writes[RevealIssue]

  final case class RevealAuditRow(
      puzzleId: String,
      terminalId: String,
      outcome: String,
      opening: Option[String],
      dominantFamily: Option[String],
      anchor: Option[String],
      lineSan: List[String],
      title: String,
      summary: String,
      commentary: String,
      issueCodes: List[String],
      issues: List[RevealIssue]
  )
  object RevealAuditRow:
    given Writes[RevealAuditRow] = Json.writes[RevealAuditRow]

  final case class RevealAuditSummary(
      totalPuzzles: Int,
      totalTerminals: Int,
      flaggedTerminals: Int,
      cleanTerminals: Int,
      issueCounts: Map[String, Int]
  )
  object RevealAuditSummary:
    given Writes[RevealAuditSummary] = Json.writes[RevealAuditSummary]

  final case class RevealAuditReport(
      version: Int = 1,
      generatedAt: String,
      inputPath: String,
      summary: RevealAuditSummary,
      rows: List[RevealAuditRow]
  )
  object RevealAuditReport:
    given Writes[RevealAuditReport] = Json.writes[RevealAuditReport]

  private val provenanceBlurFragments = List(
    "engine",
    "stockfish",
    "best move",
    "best line",
    "best continuation",
    "engine preference",
    "engine line order",
    "playablebypv",
    "strict evidence mode",
    "current evidence threshold",
    "engine-coupled continuation",
    "confirmation is still pending",
    "probe evidence",
    "supported by the current engine line",
    "a concrete line is"
  )

  private val strategicFlatteningFragments = List(
    "representative continuation",
    "no deeper node survived the filter",
    "keeps the strategic thread alive",
    "featured training line",
    "stored route",
    "stored continuation"
  )

  private val falseClaimRiskFragments = List(
    "stops the enemy ideas",
    "opens the position",
    "holds a practical initiative",
    "has the easier side to press with",
    "can keep up mild pressure",
    "is a bit better",
    "keeps initiative more stable"
  )

  def readDocs(path: Path): List[StrategicPuzzleDoc] =
    Files
      .readAllLines(path, StandardCharsets.UTF_8)
      .asScala
      .toList
      .map(_.trim)
      .filter(_.nonEmpty)
      .map(line => Json.parse(line).as[StrategicPuzzleDoc])

  def auditDocs(docs: List[StrategicPuzzleDoc], inputPath: Path): RevealAuditReport =
    val rows =
      docs.flatMap { doc =>
        doc.runtimeShell.toList.flatMap(_.terminals.map(terminal => auditTerminal(doc, terminal)))
      }
    val issueCounts =
      rows
        .flatMap(_.issueCodes)
        .groupBy(identity)
        .view
        .mapValues(_.size)
        .toMap
    val flagged = rows.count(_.issueCodes.nonEmpty)
    RevealAuditReport(
      generatedAt = Instant.now().toString,
      inputPath = inputPath.toAbsolutePath.normalize.toString,
      summary = RevealAuditSummary(
        totalPuzzles = docs.size,
        totalTerminals = rows.size,
        flaggedTerminals = flagged,
        cleanTerminals = rows.size - flagged,
        issueCounts = issueCounts
      ),
      rows = rows
    )

  def auditTerminal(doc: StrategicPuzzleDoc, terminal: TerminalReveal): RevealAuditRow =
    val title = normalizePlain(terminal.title)
    val summary = normalizePlain(terminal.summary)
    val commentary = CommentaryPayloadNormalizer.normalize(Option(terminal.commentary).getOrElse("")).trim
    val lead = firstParagraph(commentary).getOrElse(summary)
    val combined = List(title, summary, lead, commentary).filter(_.nonEmpty).mkString("\n")
    val issues = scala.collection.mutable.ListBuffer.empty[RevealIssue]

    val provenanceHits =
      (LiveNarrativeCompressionCore.systemLanguageHits(combined) ++ provenanceBlurHits(combined)).distinct
    if provenanceHits.nonEmpty then
      issues += RevealIssue(
        code = "provenance_blur",
        detail = s"Reveal text leaks engine/probe/grading language: ${provenanceHits.mkString(", ")}."
      )

    val anchorText = List(summary, lead).filter(_.nonEmpty).mkString(" ")
    if LiveNarrativeCompressionCore.requiresConcreteAnchor(anchorText) && !LiveNarrativeCompressionCore.hasConcreteAnchor(anchorText) then
      issues += RevealIssue(
        code = "missing_anchor",
        detail = "Reveal text talks about a plan or pressure without naming a square, file, break, exchange, or concrete piece target."
      )

    val falseClaimHits = falseClaimRiskHits(summary) ++ falseClaimRiskHits(lead)
    if falseClaimHits.nonEmpty then
      issues += RevealIssue(
        code = "false_claim_risk",
        detail = s"Reveal states a concrete effect without a stable anchor: ${falseClaimHits.distinct.mkString(", ")}."
      )

    val flatteningHits =
      (LiveNarrativeCompressionCore.playerLanguageHits(combined) ++ strategicFlatteningHits(title, summary, lead, commentary)).distinct
    if flatteningHits.nonEmpty then
      issues += RevealIssue(
        code = "strategic_flattening",
        detail = s"Reveal buries the teachable plan in generic or workflow text: ${flatteningHits.mkString(", ")}."
      )

    RevealAuditRow(
      puzzleId = doc.id,
      terminalId = terminal.id,
      outcome = terminal.outcome,
      opening = terminal.opening.orElse(doc.source.opening),
      dominantFamily = terminal.dominantFamilyKey.orElse(doc.dominantFamily.map(_.key)),
      anchor = terminal.anchor.orElse(doc.dominantFamily.map(_.anchor)),
      lineSan = terminal.lineSan,
      title = title,
      summary = summary,
      commentary = commentary,
      issueCodes = issues.toList.map(_.code).distinct,
      issues = issues.toList
    )

  def renderMarkdown(report: RevealAuditReport): String =
    val sb = new StringBuilder()
    sb.append("# Strategic Puzzle Reveal Audit\n\n")
    sb.append(s"- Generated: `${report.generatedAt}`\n")
    sb.append(s"- Input: `${report.inputPath}`\n")
    sb.append(s"- Puzzles: `${report.summary.totalPuzzles}`\n")
    sb.append(s"- Terminals audited: `${report.summary.totalTerminals}`\n")
    sb.append(s"- Flagged terminals: `${report.summary.flaggedTerminals}`\n")
    sb.append(s"- Clean terminals: `${report.summary.cleanTerminals}`\n")
    sb.append(s"- Issue counts: ${renderMap(report.summary.issueCounts)}\n\n")
    val flaggedRows = report.rows.filter(_.issueCodes.nonEmpty)
    if flaggedRows.nonEmpty then
      sb.append("## Flagged Reveals\n\n")
      flaggedRows.foreach { row =>
        sb.append(s"### ${row.puzzleId} / ${row.terminalId}\n\n")
        sb.append(s"- Outcome: `${row.outcome}`\n")
        sb.append(s"- Opening: `${row.opening.getOrElse("-")}`\n")
        sb.append(s"- Dominant family: `${row.dominantFamily.getOrElse("-")}`\n")
        sb.append(s"- Anchor: `${row.anchor.getOrElse("-")}`\n")
        sb.append(s"- Line: `${row.lineSan.mkString(" ")}`\n")
        sb.append(s"- Issue codes: `${row.issueCodes.mkString(", ")}`\n")
        row.issues.foreach(issue => sb.append(s"- ${issue.code}: ${issue.detail}\n"))
        sb.append("\n#### Summary\n\n```text\n")
        sb.append(row.summary.trim)
        sb.append("\n```\n\n")
        sb.append("#### Commentary\n\n```text\n")
        sb.append(row.commentary.trim)
        sb.append("\n```\n\n")
      }
    sb.toString()

  private def normalizePlain(raw: String): String =
    Option(raw).getOrElse("").replaceAll("""\s+""", " ").trim

  private def firstParagraph(raw: String): Option[String] =
    Option(raw)
      .map(_.split("""\n\s*\n""").toList.map(_.trim).find(_.nonEmpty).getOrElse("").trim)
      .filter(_.nonEmpty)

  private def provenanceBlurHits(raw: String): List[String] =
    val low = Option(raw).getOrElse("").toLowerCase
    val evalNotationHits =
      Option.when("""\([+-]?\d+(?:\.\d+)?\)""".r.findFirstIn(low).nonEmpty)("inline_eval")
        .toList
    provenanceBlurFragments.filter(low.contains) ++ evalNotationHits

  private def falseClaimRiskHits(raw: String): List[String] =
    splitSentences(raw).flatMap { sentence =>
      val low = sentence.toLowerCase
      val effectHit = falseClaimRiskFragments.find(low.contains)
      val sideEvalHit =
        Option.when(
          !LiveNarrativeCompressionCore.hasConcreteAnchor(sentence) &&
            (low.contains("white ") || low.contains("black ")) &&
            List("better", "initiative", "pressure").exists(low.contains)
        )("side_eval_without_anchor")
      List(effectHit, sideEvalHit).flatten
    }.distinct

  private def strategicFlatteningHits(
      title: String,
      summary: String,
      lead: String,
      commentary: String
  ): List[String] =
    val low = List(title, summary, lead, commentary).mkString("\n").toLowerCase
    val hits = scala.collection.mutable.ListBuffer.empty[String]
    hits ++= strategicFlatteningFragments.filter(low.contains)
    if splitSentences(summary).size > 2 || wordCount(summary) > 45 then hits += "summary_buries_plan"
    if lead.nonEmpty && wordCount(lead) > 70 then hits += "lead_buries_plan"
    hits.toList.distinct

  private def splitSentences(raw: String): List[String] =
    Option(raw)
      .getOrElse("")
      .split("""(?<=[.!?])\s+""")
      .toList
      .map(_.trim)
      .filter(_.nonEmpty)

  private def wordCount(raw: String): Int =
    Option(raw).getOrElse("").split("""\s+""").count(_.nonEmpty)

  private def renderMap(values: Map[String, Int]): String =
    if values.isEmpty then "-"
    else values.toList.sortBy(_._1).map { case (key, value) => s"`$key=$value`" }.mkString(", ")
