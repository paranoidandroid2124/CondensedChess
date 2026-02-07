package lila.llm.tools

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path, Paths }

import scala.util.control.NonFatal

import play.api.libs.functional.syntax.*
import play.api.libs.json.*

import lila.llm.analysis.{ BookStyleRenderer, CommentaryEngine, NarrativeContextBuilder, NarrativeOutlineBuilder, NarrativeUtils, TraceRecorder }
import lila.llm.model.{ OpeningReference, ProbeResult }
import lila.llm.model.authoring.OutlineBeatKind
import lila.llm.model.strategic.VariationLine

/**
 * Offline harness for comparing the current rule-based Bookmaker prose to book-style excerpts.
 *
 * Usage (from `lila-docker/repos/lila`):
 *   sbt "llm/runMain lila.llm.tools.BookCommentaryCorpusRunner"
 *   sbt "llm/runMain lila.llm.tools.BookCommentaryCorpusRunner modules/llm/docs/BookCommentaryCorpus.json modules/llm/docs/BookCommentaryCorpusReport.md"
 */
object BookCommentaryCorpusRunner:

  final case class CorpusExpect(
      minChars: Option[Int],
      minParagraphs: Option[Int],
      mustContain: List[String],
      mustNotContain: List[String],
      minBeats: Option[Int],
      mustHaveBeats: List[String],
      mustNotHaveBeats: List[String],
      minEvidenceBranches: Option[Int],
      mustHaveEvidencePurposes: List[String]
  )

  object CorpusExpect:
    given Reads[CorpusExpect] =
      (
        (JsPath \ "minChars").readNullable[Int] and
          (JsPath \ "minParagraphs").readNullable[Int] and
          (JsPath \ "mustContain").readWithDefault[List[String]](Nil) and
          (JsPath \ "mustNotContain").readWithDefault[List[String]](Nil) and
          (JsPath \ "minBeats").readNullable[Int] and
          (JsPath \ "mustHaveBeats").readWithDefault[List[String]](Nil) and
          (JsPath \ "mustNotHaveBeats").readWithDefault[List[String]](Nil) and
          (JsPath \ "minEvidenceBranches").readNullable[Int] and
          (JsPath \ "mustHaveEvidencePurposes").readWithDefault[List[String]](Nil)
      )(CorpusExpect.apply)

  final case class CorpusCase(
      id: String,
      title: String,
      analysisFen: Option[String],
      startFen: Option[String],
      preMovesUci: Option[List[String]],
      ply: Int,
      playedMove: String,
      opening: Option[String],
      phase: String,
      openingRef: Option[OpeningReference],
      variations: List[VariationLine],
      referenceExcerpt: Option[String],
      expect: Option[CorpusExpect],
      probeResults: Option[List[ProbeResult]],
      virtualMotifs: Option[List[String]]
  )

  object CorpusCase:
    given Reads[CorpusCase] = Json.reads[CorpusCase]

  final case class Corpus(
      version: Int,
      language: Option[String],
      cases: List[CorpusCase]
  )

  object Corpus:
    given Reads[Corpus] = Json.reads[Corpus]

  final case class Metrics(chars: Int, paragraphs: Int)

  final case class QualityMetrics(
      lexicalDiversity: Double,
      sentenceCount: Int,
      uniqueSentenceRatio: Double,
      duplicateSentenceCount: Int,
      boilerplateHits: Int,
      mateToneConflictHits: Int,
      moveTokenCount: Int,
      scoreTokenCount: Int,
      variationAnchorCoverage: Double,
      qualityScore: Int
  )

  final case class OutlineMetrics(
      beats: Int,
      kinds: List[String],
      evidence: Option[EvidenceMetrics]
  )

  final case class EvidenceMetrics(
      purpose: Option[String],
      branches: Int
  )

  final case class CaseResult(
      c: CorpusCase,
      fen: Option[String],
      prose: Option[String],
      metrics: Option[Metrics],
      quality: Option[QualityMetrics],
      qualityFindings: List[String],
      advisoryFindings: List[String],
      outline: Option[OutlineMetrics],
      failures: List[String]
  ):
    def passed: Boolean = failures.isEmpty

  def main(args: Array[String]): Unit =
    val strictQuality = args.contains("--strict-quality")
    val positional = args.filterNot(_.startsWith("--"))
    val corpusPath = positional.headOption.getOrElse("modules/llm/docs/BookCommentaryCorpus.json")
    val outPath = positional.lift(1).getOrElse("modules/llm/docs/BookCommentaryCorpusReport.md")

    val corpus =
      readJsonFile(Paths.get(corpusPath)).flatMap(_.validate[Corpus].asEither.left.map(_.toString)) match
        case Left(err) =>
          System.err.println(s"[corpus] Failed to read/parse `$corpusPath`:\n$err")
          sys.exit(2)
        case Right(c) => c

    val results = corpus.cases.map(runCase)
    val report = renderReport(corpusPath, results)
    writeText(Paths.get(outPath), report)

    val failed = results.count(!_.passed)
    val advisories = results.map(_.advisoryFindings.size).sum
    val total = results.size
    if (failed == 0 && (!strictQuality || advisories == 0))
      println(s"[corpus] ✅ All $total cases satisfied expectations. Wrote `$outPath`.")
      if (advisories > 0)
        println(s"[corpus] ⚠ Non-blocking advisories: $advisories (run with --strict-quality to fail on advisories).")
    else
      if (failed > 0)
        System.err.println(s"[corpus] ❌ $failed/$total cases failed expectations. Wrote `$outPath`.")
      else
        System.err.println(s"[corpus] ❌ Strict quality mode: advisory findings detected ($advisories). Wrote `$outPath`.")
      sys.exit(1)

  private def runCase(c: CorpusCase): CaseResult =
    val fenOpt = c.analysisFen.orElse:
      for
        start <- c.startFen
        pre <- c.preMovesUci
      yield NarrativeUtils.uciListToFen(start, pre)

    val baseFailures =
      List(
        Option.when(fenOpt.isEmpty)(
          "Missing `analysisFen` (provide `analysisFen` or `startFen` + `preMovesUci`)."
        ),
        Option.when(c.variations.isEmpty)("Missing `variations` (need at least 1 PV line).")
      ).flatten

    if (baseFailures.nonEmpty) return CaseResult(c, fenOpt, None, None, None, Nil, Nil, None, baseFailures)

    val fen = fenOpt.get

    val dataOpt =
      try
        CommentaryEngine.assessExtended(
          fen = fen,
          variations = c.variations,
          playedMove = Some(c.playedMove),
          opening = c.opening,
          phase = Some(c.phase),
          ply = c.ply,
          prevMove = Some(c.playedMove)
        )
      catch
        case NonFatal(e) =>
          System.err.println(s"[case:${c.id}] Exception: ${e.getMessage}")
          None

    dataOpt match
      case None =>
        CaseResult(
          c = c,
          fen = Some(fen),
          prose = None,
          metrics = None,
          quality = None,
          qualityFindings = Nil,
          advisoryFindings = Nil,
          outline = None,
          failures = List("Analysis failed (invalid FEN, illegal PV, or internal exception).")
        )
      case Some(data) =>
        val ctx =
          NarrativeContextBuilder.build(
            data = data,
            ctx = data.toContext,
            probeResults = c.probeResults.getOrElse(Nil),
            openingRef = c.openingRef
          )
        
        // Inject virtual motifs for corpus validation
        val enrichedCtx = ctx.copy(
          semantic = Some(ctx.semantic.getOrElse(lila.llm.model.SemanticSection(Nil, Nil, Nil, None, None, None, Nil, Nil)).copy(
            conceptSummary = (ctx.semantic.map(_.conceptSummary).getOrElse(Nil) ++ c.virtualMotifs.getOrElse(Nil)).distinct
          ))
        )

        val prose = BookStyleRenderer.render(enrichedCtx)
        val metrics = computeMetrics(prose)
        val quality = computeQualityMetrics(prose, fen, c.variations)
        val qualityFindings = qualityFindingsFrom(quality)
        val advisoryFindings = advisoryFindingsFrom(quality)

        val outlineOpt =
          try
            val rec = new TraceRecorder()
            val (outline, _) = NarrativeOutlineBuilder.build(enrichedCtx, rec)
            val evidenceBeat = outline.beats.find(_.kind == OutlineBeatKind.Evidence)
            val evidenceBranches =
              evidenceBeat
                .map(_.text.linesIterator.map(_.trim).count(_.matches("^[a-z]\\)\\s+.+$")))
                .getOrElse(0)
            val evidencePurpose = evidenceBeat.flatMap(_.evidencePurposes.headOption)

            Some(
              OutlineMetrics(
                beats = outline.beats.size,
                kinds = outline.beats.map(_.kind.toString).distinct,
                evidence = evidenceBeat.map(_ => EvidenceMetrics(purpose = evidencePurpose, branches = evidenceBranches))
              )
            )
          catch
            case NonFatal(_) => None

        val expectationFailures = c.expect.toList.flatMap(checkExpectations(prose, metrics, outlineOpt, _))

        CaseResult(
          c = c,
          fen = Some(fen),
          prose = Some(prose),
          metrics = Some(metrics),
          quality = Some(quality),
          qualityFindings = qualityFindings,
          advisoryFindings = advisoryFindings,
          outline = outlineOpt,
          failures = expectationFailures
        )

  private def computeMetrics(prose: String): Metrics =
    val paras = prose.split("\n\n").iterator.map(_.trim).count(_.nonEmpty)
    Metrics(chars = prose.length, paragraphs = paras)

  private val moveTokenRegex =
    """(?i)\b(?:[KQRBN]?[a-h]?[1-8]?x?[a-h][1-8](?:=[QRBN])?[+#]?|O-O(?:-O)?)\b""".r
  private val scoreTokenRegex = """(?i)(?:\([+\-]\d+\.\d\)|\bmate\b|\bcp\b|#[0-9]+)""".r
  private val tokenRegex = """[A-Za-z][A-Za-z0-9'\-]{1,}""".r
  private val sentenceSplitRegex = """(?<=[\.\!\?])\s+|\n+""".r

  private val boilerplateLexicon = List(
    "is another good option",
    "is also playable here",
    "deserves attention",
    "both sides have their chances",
    "the game remains tense",
    "the position is roughly equal",
    "in practical terms, this is comfortable to play",
    "accuracy is required to hold the balance",
    "the plan is clear:",
    "both sides are still coordinating pieces",
    "usually reduces tactical noise and favors precise maneuvering",
    "often steers the position toward a structured endgame battle",
    "sufficient positional compensation provides sufficient compensation for the material"
  )

  private def computeQualityMetrics(prose: String, fen: String, vars: List[VariationLine]): QualityMetrics =
    val lower = prose.toLowerCase

    val tokens = tokenRegex.findAllIn(lower).toList
    val uniqueTokens = tokens.distinct
    val lexicalDiversity =
      if tokens.isEmpty then 0.0
      else uniqueTokens.size.toDouble / tokens.size.toDouble

    val sentences = extractSentences(prose)
    val sentenceNorm = sentences.map(normalizeSentence).filter(_.length >= 8)
    val uniqueSentenceCount = sentenceNorm.distinct.size
    val sentenceCount = sentenceNorm.size
    val uniqueSentenceRatio =
      if sentenceCount == 0 then 1.0
      else uniqueSentenceCount.toDouble / sentenceCount.toDouble
    val duplicateSentenceCount = (sentenceCount - uniqueSentenceCount).max(0)

    val boilerplateHits =
      boilerplateLexicon.map(p => occurrencesOf(lower, p)).sum

    val hasMateMove = """\*\*[^*\n]*#\*\*""".r.findFirstIn(prose).nonEmpty
    val mateToneConflictHits =
      if !hasMateMove then 0
      else
        List(
          "structured endgame battle",
          "calmer technical phase",
          "structured technical struggle"
        ).map(p => occurrencesOf(lower, p)).sum

    val moveTokenCount = moveTokenRegex.findAllIn(prose).length
    val scoreTokenCount = scoreTokenRegex.findAllIn(prose).length

    val anchors = vars.take(3).flatMap(v => NarrativeUtils.uciListToSan(fen, v.moves).headOption).map(_.toLowerCase)
    val covered = anchors.count(a => lower.contains(a))
    val variationAnchorCoverage =
      if anchors.isEmpty then 1.0
      else covered.toDouble / anchors.size.toDouble

    val qualityScore = {
      var s = 100
      if lexicalDiversity < 0.34 then s -= 20
      else if lexicalDiversity < 0.42 then s -= 10
      if uniqueSentenceRatio < 0.70 then s -= 20
      else if uniqueSentenceRatio < 0.82 then s -= 10
      s -= math.min(24, boilerplateHits * 8)
      s -= math.min(20, mateToneConflictHits * 10)
      if variationAnchorCoverage < 0.34 then s -= 20
      else if variationAnchorCoverage < 0.67 then s -= 10
      if moveTokenCount < 6 then s -= 10
      s.max(0).min(100)
    }

    QualityMetrics(
      lexicalDiversity = lexicalDiversity,
      sentenceCount = sentenceCount,
      uniqueSentenceRatio = uniqueSentenceRatio,
      duplicateSentenceCount = duplicateSentenceCount,
      boilerplateHits = boilerplateHits,
      mateToneConflictHits = mateToneConflictHits,
      moveTokenCount = moveTokenCount,
      scoreTokenCount = scoreTokenCount,
      variationAnchorCoverage = variationAnchorCoverage,
      qualityScore = qualityScore
    )

  private def qualityFindingsFrom(q: QualityMetrics): List[String] =
    List(
      Option.when(q.lexicalDiversity < 0.34)(f"Low lexical diversity: ${q.lexicalDiversity}%.2f"),
      Option.when(q.uniqueSentenceRatio < 0.75)(f"Low sentence uniqueness: ${q.uniqueSentenceRatio}%.2f"),
      Option.when(q.boilerplateHits >= 2)(s"Boilerplate phrase hits: ${q.boilerplateHits}"),
      Option.when(q.mateToneConflictHits > 0)(s"Mate-tone conflict hits: ${q.mateToneConflictHits}"),
      Option.when(q.variationAnchorCoverage < 0.50)(f"Low variation anchor coverage: ${q.variationAnchorCoverage}%.2f"),
      Option.when(q.moveTokenCount < 6)(s"Low move-token density: ${q.moveTokenCount}"),
      Option.when(q.qualityScore < 70)(s"Low quality score: ${q.qualityScore}/100")
    ).flatten

  private def advisoryFindingsFrom(q: QualityMetrics): List[String] =
    List(
      Option.when(q.qualityScore < 90)(s"Advisory: quality score below target (90): ${q.qualityScore}"),
      Option.when(q.boilerplateHits >= 1)(s"Advisory: boilerplate phrase present (${q.boilerplateHits})"),
      Option.when(q.mateToneConflictHits > 0)(s"Advisory: mate-tone conflict present (${q.mateToneConflictHits})"),
      Option.when(q.lexicalDiversity < 0.70)(f"Advisory: lexical diversity soft-low: ${q.lexicalDiversity}%.2f")
    ).flatten

  private def extractSentences(text: String): List[String] =
    sentenceSplitRegex
      .split(text.replace("\r\n", "\n"))
      .toList
      .map(_.trim)
      .filter(_.nonEmpty)

  private def normalizeSentence(s: String): String =
    s.toLowerCase
      .replaceAll("""[*`_>\[\]\(\)]""", " ")
      .replaceAll("""[^a-z0-9\s]""", " ")
      .replaceAll("""\s+""", " ")
      .trim

  private def occurrencesOf(haystack: String, needle: String): Int =
    if needle.isEmpty then 0
    else haystack.sliding(needle.length).count(_ == needle)

  private def checkExpectations(
      prose: String,
      metrics: Metrics,
      outline: Option[OutlineMetrics],
      e: CorpusExpect
  ): List[String] =
    val text = prose.toLowerCase

    val minCharsFail =
      e.minChars.filter(metrics.chars < _).map(n => s"minChars failed: expected >= $n, got ${metrics.chars}")

    val minParasFail =
      e.minParagraphs
        .filter(metrics.paragraphs < _)
        .map(n => s"minParagraphs failed: expected >= $n, got ${metrics.paragraphs}")

    val mustContainFails =
      e.mustContain.flatMap { token =>
        Option.unless(text.contains(token.toLowerCase))(s"""mustContain failed: "$token"""")
      }

    val mustNotContainFails =
      e.mustNotContain.flatMap { token =>
        Option.when(text.contains(token.toLowerCase))(s"""mustNotContain failed: "$token"""")
      }

    val minBeatsFail =
      e.minBeats.flatMap { n =>
        outline match
          case None    => Some(s"minBeats failed: expected >= $n, got <no outline>")
          case Some(o) => Option.when(o.beats < n)(s"minBeats failed: expected >= $n, got ${o.beats}")
      }

    val mustHaveBeatsFail =
      e.mustHaveBeats.flatMap { beat =>
        outline match
          case None => Some(s"""mustHaveBeats failed: "$beat" (no outline)""")
          case Some(o) =>
            Option.unless(o.kinds.exists(_.equalsIgnoreCase(beat)))(s"""mustHaveBeats failed: "$beat"""")
      }

    val mustNotHaveBeatsFail =
      e.mustNotHaveBeats.flatMap { beat =>
        outline match
          case None => Nil
          case Some(o) =>
            Option.when(o.kinds.exists(_.equalsIgnoreCase(beat)))(s"""mustNotHaveBeats failed: "$beat"""").toList
      }

    val minEvidenceBranchesFail =
      e.minEvidenceBranches.flatMap { n =>
        outline match
          case None => Some(s"minEvidenceBranches failed: expected >= $n, got <no outline>")
          case Some(o) =>
            val got = o.evidence.map(_.branches).getOrElse(0)
            Option.when(got < n)(s"minEvidenceBranches failed: expected >= $n, got $got")
      }

    val mustHaveEvidencePurposesFail =
      e.mustHaveEvidencePurposes.flatMap { p =>
        outline match
          case None => Some(s"""mustHaveEvidencePurposes failed: "$p" (no outline)""")
          case Some(o) =>
            val got = o.evidence.flatMap(_.purpose).getOrElse("")
            Option.unless(got.equalsIgnoreCase(p))(s"""mustHaveEvidencePurposes failed: "$p" (got "$got")""")
      }

    List(minCharsFail, minParasFail, minBeatsFail, minEvidenceBranchesFail).flatten ++
      mustContainFails ++ mustNotContainFails ++ mustHaveBeatsFail ++ mustNotHaveBeatsFail ++ mustHaveEvidencePurposesFail

  private def renderReport(corpusPath: String, results: List[CaseResult]): String =
    val sb = new StringBuilder()

    val total = results.size
    val failed = results.count(!_.passed)
    val passed = total - failed
    val qualityList = results.flatMap(_.quality)
    val avgQuality = average(qualityList.map(_.qualityScore.toDouble))
    val avgLexical = average(qualityList.map(_.lexicalDiversity))
    val avgAnchorCoverage = average(qualityList.map(_.variationAnchorCoverage))
    val lowQualityCases = results.filter(_.quality.exists(_.qualityScore < 70))
    val advisoryCases = results.filter(_.advisoryFindings.nonEmpty)
    val advisoryCount = results.map(_.advisoryFindings.size).sum

    val crossCaseSentenceMap = repeatedSentencesAcrossCases(results)
    val topRepeatedSentences = crossCaseSentenceMap.toList.sortBy((_, c) => -c).take(12)

    sb.append("# Book Commentary Corpus Report\n\n")
    sb.append(s"- Corpus: `$corpusPath`\n")
    sb.append(s"- Results: $passed/$total passed\n\n")
    if qualityList.nonEmpty then
      sb.append("## Quality Summary\n\n")
      sb.append(f"- Avg quality score: $avgQuality%.1f / 100\n")
      sb.append(f"- Avg lexical diversity: $avgLexical%.3f\n")
      sb.append(f"- Avg variation-anchor coverage: $avgAnchorCoverage%.3f\n")
      sb.append(s"- Low-quality cases (<70): ${lowQualityCases.size}\n")
      if lowQualityCases.nonEmpty then
        sb.append("- Case IDs: ")
        sb.append(lowQualityCases.map(_.c.id).mkString(", "))
        sb.append("\n")
      sb.append(s"- Advisory findings (non-blocking): $advisoryCount across ${advisoryCases.size} cases\n")
      sb.append("\n")

      sb.append("## Cross-Case Repetition\n\n")
      if topRepeatedSentences.isEmpty then sb.append("- No sentence repeated across 3+ cases.\n\n")
      else
        topRepeatedSentences.foreach { (s, n) =>
          sb.append(s"""- [$n cases] "$s"\n""")
        }
        sb.append("\n")

    results.foreach { r =>
      sb.append(s"## ${r.c.id}: ${r.c.title}\n\n")
      sb.append(s"- `ply`: ${r.c.ply}\n")
      sb.append(s"- `playedMove`: `${r.c.playedMove}`\n")
      r.fen.foreach(f => sb.append(s"- `analysisFen`: `$f`\n"))
      r.metrics.foreach { m => sb.append(s"- Metrics: ${m.chars} chars, ${m.paragraphs} paragraphs\n") }
      r.quality.foreach { q =>
        sb.append(
          f"- Quality: score=${q.qualityScore}%d/100, lexical=${q.lexicalDiversity}%.3f, uniqueSent=${q.uniqueSentenceRatio}%.3f, anchorCoverage=${q.variationAnchorCoverage}%.3f\n"
        )
        sb.append(s"- Quality details: sentences=${q.sentenceCount}, dup=${q.duplicateSentenceCount}, boilerplate=${q.boilerplateHits}, mateToneConflict=${q.mateToneConflictHits}, moveTokens=${q.moveTokenCount}, scoreTokens=${q.scoreTokenCount}\n")
      }
      if r.qualityFindings.nonEmpty then
        sb.append("- Quality findings:\n")
        r.qualityFindings.foreach(f => sb.append(s"  - $f\n"))
      if r.advisoryFindings.nonEmpty then
        sb.append("- Advisory findings:\n")
        r.advisoryFindings.foreach(f => sb.append(s"  - $f\n"))
      r.outline.foreach { o =>
        val kinds = if (o.kinds.nonEmpty) o.kinds.mkString(", ") else "-"
        sb.append(s"- Outline: ${o.beats} beats ($kinds)\n")
        o.evidence.foreach { e =>
          val p = e.purpose.getOrElse("-")
          sb.append(s"- Evidence: purpose=$p, branches=${e.branches}\n")
        }
      }

      if (r.failures.nonEmpty) {
        sb.append("- Failures:\n")
        r.failures.foreach(f => sb.append(s"  - $f\n"))
      }

      r.c.referenceExcerpt.foreach { ex =>
        sb.append("\n**Reference excerpt (for human comparison)**\n\n")
        sb.append("```text\n")
        sb.append(ex.trim)
        sb.append("\n```\n")
      }

      sb.append("\n**Generated commentary**\n\n")
      r.prose match
        case None => sb.append("_<no output>_\n\n")
        case Some(p) =>
          sb.append("```text\n")
          sb.append(p.trim)
          sb.append("\n```\n\n")
    }

    sb.toString

  private def repeatedSentencesAcrossCases(results: List[CaseResult]): Map[String, Int] =
    val perCaseSentenceSets = results.flatMap(_.prose).map { prose =>
      extractSentences(prose)
        .map(normalizeSentence)
        .filter(_.length >= 24)
        .toSet
    }
    perCaseSentenceSets
      .flatten
      .groupBy(identity)
      .view
      .mapValues(_.size)
      .filter(_._2 >= 3)
      .toMap

  private def average(xs: List[Double]): Double =
    if xs.isEmpty then 0.0
    else xs.sum / xs.size

  private def readJsonFile(path: Path): Either[String, JsValue] =
    try
      val raw = Files.readString(path, StandardCharsets.UTF_8)
      Right(Json.parse(raw))
    catch
      case NonFatal(e) => Left(e.toString)

  private def writeText(path: Path, text: String): Unit =
    Files.writeString(path, text, StandardCharsets.UTF_8)
