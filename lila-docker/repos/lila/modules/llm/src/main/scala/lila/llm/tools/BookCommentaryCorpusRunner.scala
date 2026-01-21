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
      outline: Option[OutlineMetrics],
      failures: List[String]
  ):
    def passed: Boolean = failures.isEmpty

  def main(args: Array[String]): Unit =
    val corpusPath = args.headOption.getOrElse("modules/llm/docs/BookCommentaryCorpus.json")
    val outPath = args.lift(1).getOrElse("modules/llm/docs/BookCommentaryCorpusReport.md")

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
    val total = results.size
    if (failed == 0) println(s"[corpus] ✅ All $total cases satisfied expectations. Wrote `$outPath`.")
    else
      System.err.println(s"[corpus] ❌ $failed/$total cases failed expectations. Wrote `$outPath`.")
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

    if (baseFailures.nonEmpty) return CaseResult(c, fenOpt, None, None, None, baseFailures)

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

        CaseResult(c, Some(fen), Some(prose), Some(metrics), outlineOpt, expectationFailures)

  private def computeMetrics(prose: String): Metrics =
    val paras = prose.split("\n\n").iterator.map(_.trim).count(_.nonEmpty)
    Metrics(chars = prose.length, paragraphs = paras)

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

    sb.append("# Book Commentary Corpus Report\n\n")
    sb.append(s"- Corpus: `$corpusPath`\n")
    sb.append(s"- Results: $passed/$total passed\n\n")

    results.foreach { r =>
      sb.append(s"## ${r.c.id}: ${r.c.title}\n\n")
      sb.append(s"- `ply`: ${r.c.ply}\n")
      sb.append(s"- `playedMove`: `${r.c.playedMove}`\n")
      r.fen.foreach(f => sb.append(s"- `analysisFen`: `$f`\n"))
      r.metrics.foreach { m => sb.append(s"- Metrics: ${m.chars} chars, ${m.paragraphs} paragraphs\n") }
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

  private def readJsonFile(path: Path): Either[String, JsValue] =
    try
      val raw = Files.readString(path, StandardCharsets.UTF_8)
      Right(Json.parse(raw))
    catch
      case NonFatal(e) => Left(e.toString)

  private def writeText(path: Path, text: String): Unit =
    Files.writeString(path, text, StandardCharsets.UTF_8)
