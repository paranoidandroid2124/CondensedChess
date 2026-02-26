package lila.llm.tools

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path, Paths }

import scala.concurrent.{ Await, ExecutionContext }
import scala.concurrent.duration.*
import scala.util.control.NonFatal

import akka.actor.ActorSystem
import play.api.libs.ws.ahc.StandaloneAhcWSClient
import play.api.libs.functional.syntax.*
import play.api.libs.json.*
import play.shaded.ahc.org.asynchttpclient.DefaultAsyncHttpClient

import lila.llm.analysis.{ BookStyleRenderer, CommentaryEngine, NarrativeContextBuilder, NarrativeOutlineBuilder, NarrativeUtils, OpeningExplorerClient, PlanEvidenceEvaluator, TraceRecorder }
import lila.llm.model.*
import lila.llm.model.authoring.OutlineBeatKind
import lila.llm.model.strategic.VariationLine
import lila.llm.analysis.strategic.EndgameAnalyzerImpl

/**
 * Offline harness for comparing the current rule-based Bookmaker prose to book-style excerpts.
 *
 * Usage (from `lila-docker/repos/lila`):
 *   sbt "llm/runMain lila.llm.tools.BookCommentaryCorpusRunner"
 *   sbt "llm/runMain lila.llm.tools.BookCommentaryCorpusRunner modules/llm/docs/BookCommentaryCorpus.json modules/llm/docs/BookCommentaryCorpusReport.md"
 */
object BookCommentaryCorpusRunner:
  private val AllowedRepeatedFivegramViolations = 20


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

  final case class EndgameExpect(
      oppositionType: Option[String],
      ruleOfSquare: Option[String],
      triangulationAvailable: Option[Boolean],
      rookEndgamePattern: Option[String],
      zugzwangLikely: Option[Boolean]
  )
  object EndgameExpect:
    given Reads[EndgameExpect] = Json.reads[EndgameExpect]

  final case class EndgameGoldRow(
      fen: String,
      color: String,
      expect: EndgameExpect
  )
  object EndgameGoldRow:
    given Reads[EndgameGoldRow] = Json.reads[EndgameGoldRow]

  final case class EndgameGoldMetrics(
      rows: Int,
      labeledChecks: Int,
      matchedChecks: Int,
      mismatchedChecks: Int,
      conceptF1: Double,
      oracleLatencyP95Ms: Double
  )

  final case class EndgameSnapshot(
      oppositionType: String,
      ruleOfSquare: String,
      triangulationAvailable: Boolean,
      rookEndgamePattern: String,
      zugzwangLikelihood: Double,
      isZugzwang: Boolean,
      outcomeHint: String,
      confidence: Double,
      primaryPattern: Option[String]
  )

  final case class EndgameGateStatus(
      conceptF1: Option[Double],
      contradictionRate: Double,
      oracleLatencyP95Ms: Option[Double]
  ):
    def passed: Boolean =
      conceptF1.forall(_ >= 0.85) &&
        contradictionRate < 0.02 &&
        oracleLatencyP95Ms.forall(_ <= 40.0)

  final case class Metrics(chars: Int, paragraphs: Int)

  final case class QualityMetrics(
      lexicalDiversity: Double,
      sentenceCount: Int,
      uniqueSentenceRatio: Double,
      duplicateSentenceCount: Int,
      repeatedTrigramTypes: Int,
      repeatedFourgramTypes: Int,
      maxNgramRepeat: Int,
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

  final case class StrategicMetrics(
      planRecallAt3: Double,
      planRecallAt3Legacy: Double,
      planRecallAt3HypothesisFirst: Double,
      latentPrecision: Double,
      pvCouplingRatio: Double,
      evidenceReasonCoverage: Double,
      hypothesisProbeHitRate: Double,
      contractDropRate: Double,
      legacyFenMissingRate: Double
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
      endgame: Option[EndgameSnapshot],
      openingPrecedentEligible: Boolean,
      openingPrecedentObserved: Boolean,
      semanticConcepts: List[String],
      strategic: Option[StrategicMetrics],
      analysisLatencyMs: Option[Long],
      failures: List[String]
  ):
    def passed: Boolean = failures.isEmpty

  final case class BalancedGateStatus(
      totalCases: Int,
      eligiblePrecedentCases: Int,
      precedentCoveredCases: Int,
      requiredPrecedentCases: Int,
      repeatedFivegramViolations: Int,
      allowedFivegramViolations: Int
  ):
    def passed: Boolean =
      precedentCoveredCases >= requiredPrecedentCases &&
        repeatedFivegramViolations <= allowedFivegramViolations

  def main(args: Array[String]): Unit =
    val strictQuality     = args.contains("--strict-quality")
    val noOpeningExplorer = args.contains("--no-opening-explorer")
    val noEndgameGoldset  = args.contains("--no-endgame-goldset")
    val endgameGoldsetPath = optionValue(args.toList, "--endgame-goldset")
      .getOrElse("modules/llm/src/test/resources/endgame_goldset_v1.jsonl")
    val positional        = positionalArgs(args.toList)
    val corpusPath        = positional.headOption.getOrElse("modules/llm/docs/BookCommentaryCorpus.json")
    val outPath           = positional.lift(1).getOrElse("modules/llm/docs/BookCommentaryCorpusReport.md")

    given ExecutionContext = ExecutionContext.global
    val actorSystem        = ActorSystem("book-commentary-corpus-runner")
    given ActorSystem      = actorSystem

    val wsClientOpt =
      if noOpeningExplorer then None
      else Some(new StandaloneAhcWSClient(new DefaultAsyncHttpClient()))
    val openingExplorerOpt = wsClientOpt.map(ws => OpeningExplorerClient(ws))

    val fetchOpeningRef: String => Option[OpeningReference] = fen =>
      openingExplorerOpt.flatMap { explorer =>
        try Await.result(explorer.fetchMasters(fen), 4.seconds)
        catch
          case NonFatal(e) =>
            System.err.println(
              s"[corpus] Advisory: Opening Explorer fetch failed for `${fen.take(32)}...`: ${e.getMessage}"
            )
            None
      }

    try
      val corpus =
        readJsonFile(Paths.get(corpusPath)).flatMap(_.validate[Corpus].asEither.left.map(_.toString)) match
          case Left(err) =>
            System.err.println(s"[corpus] Failed to read/parse `$corpusPath`:\n$err")
            sys.exit(2)
          case Right(c) => c

      val endgameGoldMetrics =
        if noEndgameGoldset then None
        else
          evaluateEndgameGoldset(Paths.get(endgameGoldsetPath)) match
            case Left(err) =>
              System.err.println(s"[corpus] Advisory: failed to evaluate endgame goldset `$endgameGoldsetPath`: $err")
              None
            case Right(metrics) => Some(metrics)

      val results = corpus.cases.map(runCase(_, fetchOpeningRef))
      val report  = renderReport(corpusPath, results, endgameGoldMetrics)
      writeText(Paths.get(outPath), report)

      val failed        = results.count(!_.passed)
      val failedQuality = results.count(_.qualityFindings.nonEmpty)
      val advisories    = results.map(_.advisoryFindings.size).sum
      val total         = results.size
      val gate          = balancedGate(results)
      val endgameGate   = endgameGateStatus(results, endgameGoldMetrics)
      val strictOk      = advisories == 0 && gate.passed && failedQuality == 0 && endgameGate.passed

      if failed == 0 && failedQuality == 0 && (!strictQuality || strictOk) then
        println(s"[corpus] ✅ All $total cases satisfied expectations. Wrote `$outPath`.")
        if advisories > 0 then
          println(
            s"[corpus] ⚠ Non-blocking advisories: $advisories (run with --strict-quality to fail on advisories)."
          )
        if !gate.passed then
          println(
            s"[corpus] ⚠ Balanced gate not met: precedentCoverage=${gate.precedentCoveredCases}/${gate.eligiblePrecedentCases} (target >= ${gate.requiredPrecedentCases}), repeated5gram=${gate.repeatedFivegramViolations} (target <= ${gate.allowedFivegramViolations})."
          )
        if !endgameGate.passed then
          println(
            f"[corpus] ⚠ Endgame gate not met: conceptF1=${endgameGate.conceptF1.getOrElse(0.0)}%.3f (target>=0.85), contradictionRate=${endgameGate.contradictionRate}%.3f (target<0.02), oracleP95Ms=${endgameGate.oracleLatencyP95Ms.getOrElse(0.0)}%.3f (target<=40)."
          )
      else
        if failed > 0 then
          System.err.println(s"[corpus] ❌ $failed/$total cases failed hard expectations. Wrote `$outPath`.")
        if failedQuality > 0 then
          System.err.println(
            s"[corpus] ❌ $failedQuality/$total cases failed quality criteria. Wrote `$outPath`."
          )
        if strictQuality && (advisories > 0 || !gate.passed || !endgameGate.passed) then
          System.err.println(
            s"[corpus] ❌ Strict quality mode: non-blocking issues detected (advisories=$advisories, gate=${if gate.passed then "PASS" else "FAIL"}, endgameGate=${if endgameGate.passed then "PASS" else "FAIL"}). Wrote `$outPath`."
          )
        sys.exit(1)
    finally
      wsClientOpt.foreach(_.close())
      actorSystem.terminate()

  private def runCase(
      c: CorpusCase,
      fetchOpeningRef: String => Option[OpeningReference]
  ): CaseResult =
    val fenOpt = c.analysisFen.orElse:
      for
        start <- c.startFen
        pre   <- c.preMovesUci
      yield NarrativeUtils.uciListToFen(start, pre)

    val baseFailures =
      List(
        Option.when(fenOpt.isEmpty)(
          "Missing `analysisFen` (provide `analysisFen` or `startFen` + `preMovesUci`)."
        ),
        Option.when(c.variations.isEmpty)("Missing `variations` (need at least 1 PV line).")
      ).flatten

    if baseFailures.nonEmpty then
      return CaseResult(
        c = c,
        fen = fenOpt,
        prose = None,
        metrics = None,
        quality = None,
        qualityFindings = Nil,
        advisoryFindings = Nil,
        outline = None,
        endgame = None,
        openingPrecedentEligible = false,
        openingPrecedentObserved = false,
        semanticConcepts = Nil,
        strategic = None,
        analysisLatencyMs = None,
        failures = baseFailures
      )

    val fen        = fenOpt.get
    val openingRef = c.openingRef.orElse(fetchOpeningRef(fen))

    val analysisStartNs = System.nanoTime()
    val dataOpt =
      try
        CommentaryEngine.assessExtended(
          fen = fen,
          variations = c.variations,
          playedMove = Some(c.playedMove),
          opening = c.opening,
          phase = Some(c.phase),
          ply = c.ply,
          prevMove = Some(c.playedMove),
          probeResults = c.probeResults.getOrElse(Nil)
        )
      catch
        case NonFatal(e) =>
          System.err.println(s"[case:${c.id}] Exception: ${e.getMessage}")
          None
    val analysisLatencyMs = ((System.nanoTime() - analysisStartNs) / 1000000L).max(0L)

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
          endgame = None,
          openingPrecedentEligible = false,
          openingPrecedentObserved = false,
          semanticConcepts = Nil,
          strategic = None,
          analysisLatencyMs = Some(analysisLatencyMs),
          failures = List("Analysis failed (invalid FEN, illegal PV, or internal exception).")
        )
      case Some(data) =>
        val ctx =
          NarrativeContextBuilder.build(
            data = data,
            ctx = data.toContext,
            prevAnalysis = None,
            probeResults = c.probeResults.getOrElse(Nil),
            openingRef = openingRef,
            prevOpeningRef = None,
            openingBudget = OpeningEventBudget(),
            afterAnalysis = None
          )

        // Inject virtual motifs for corpus validation
        val enrichedCtx = ctx.copy(
          semantic = Some(
            ctx.semantic
              .getOrElse(lila.llm.model.SemanticSection(Nil, Nil, Nil, None, None, None, Nil, Nil))
              .copy(
                conceptSummary =
                  (ctx.semantic.map(_.conceptSummary).getOrElse(Nil) ++ c.virtualMotifs.getOrElse(Nil)).distinct
              )
          )
        )

        val prose            = BookStyleRenderer.render(enrichedCtx)
        val metrics          = computeMetrics(prose)
        val quality          = computeQualityMetrics(prose, fen, c.variations)
        val qualityFindings  = qualityFindingsFrom(quality)
        val advisoryFindings = advisoryFindingsFrom(quality)

        val outlineOpt =
          try
            val rec            = new TraceRecorder()
            val (outline, _)   = NarrativeOutlineBuilder.build(enrichedCtx, rec)
            val evidenceBeat   = outline.beats.find(_.kind == OutlineBeatKind.Evidence)
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
          catch case NonFatal(_) => None

        val expectationFailures = c.expect.toList.flatMap(checkExpectations(prose, metrics, outlineOpt, _))
        val openingPrecedentEligible =
          c.phase.equalsIgnoreCase("opening") &&
            ctx.openingEvent.exists(isCoreOpeningEvent) &&
            openingRef.exists(_.sampleGames.nonEmpty)
        val openingPrecedentObserved = openingPrecedentEligible && precedentMentionCount(prose) > 0
        val semanticConcepts =
          enrichedCtx.semantic
            .map(_.conceptSummary.map(_.trim).filter(_.nonEmpty).distinct)
            .getOrElse(Nil)
        val strategicMetrics = computeStrategicMetrics(data, enrichedCtx, c.probeResults.getOrElse(Nil))

        CaseResult(
          c = c,
          fen = Some(fen),
          prose = Some(prose),
          metrics = Some(metrics),
          quality = Some(quality),
          qualityFindings = qualityFindings,
          advisoryFindings = advisoryFindings,
          outline = outlineOpt,
          endgame = data.endgameFeatures.map { eg =>
            EndgameSnapshot(
              oppositionType = eg.oppositionType.toString,
              ruleOfSquare = eg.ruleOfSquare.toString,
              triangulationAvailable = eg.triangulationAvailable,
              rookEndgamePattern = eg.rookEndgamePattern.toString,
              zugzwangLikelihood = eg.zugzwangLikelihood,
              isZugzwang = eg.isZugzwang,
              outcomeHint = eg.theoreticalOutcomeHint.toString,
              confidence = eg.confidence,
              primaryPattern = eg.primaryPattern
            )
          },
          openingPrecedentEligible = openingPrecedentEligible,
          openingPrecedentObserved = openingPrecedentObserved,
          semanticConcepts = semanticConcepts,
          strategic = Some(strategicMetrics),
          analysisLatencyMs = Some(analysisLatencyMs),
          failures = expectationFailures
        )

  private def computeMetrics(prose: String): Metrics =
    val paras = prose.split("\n\n").iterator.map(_.trim).count(_.nonEmpty)
    Metrics(chars = prose.length, paragraphs = paras)

  private def computeStrategicMetrics(
      data: ExtendedAnalysisData,
      ctx: NarrativeContext,
      rawProbeResults: List[ProbeResult]
  ): StrategicMetrics =
    val topRuleId = data.plans.headOption.map(_.plan.id.toString)
    val topRuleIds = data.plans.take(3).map(_.plan.id.toString).toSet
    val topHypothesisId = ctx.mainStrategicPlans.headOption.map(_.planId)
    val planRecallAt3Legacy =
      if topHypothesisId.isDefined && topRuleId == topHypothesisId then 1.0 else 0.0
    val planRecallAt3HypothesisFirst =
      if topHypothesisId.exists(topRuleIds.contains) then 1.0 else 0.0
    val planRecallAt3 = planRecallAt3Legacy

    val latentPrecision =
      if ctx.latentPlans.isEmpty then 1.0
      else
        val supported = ctx.latentPlans.count { lp =>
          ctx.authorEvidence.exists { ev =>
            val p = ev.purpose.toLowerCase
            (p.contains("free_tempo") || p.contains("latent_plan")) && ev.branches.nonEmpty &&
              ev.branches.exists(b => Option(b.line).exists(_.toLowerCase.contains(lp.seedId.toLowerCase.take(6))))
          }
        }
        supported.toDouble / ctx.latentPlans.size.toDouble

    val pvCouplingRatio =
      if ctx.whyAbsentFromTopMultiPV.nonEmpty then 0.0 else 1.0

    val evidenceReasonCoverage =
      if ctx.whyAbsentFromTopMultiPV.isEmpty then 1.0
      else if ctx.absentReasonSource == "evidence" then 1.0
      else 0.0

    val rootProbeResultsRaw = rawProbeResults.filter(pr => pr.fen.forall(_ == data.fen))
    val validation = PlanEvidenceEvaluator.validateProbeResults(rootProbeResultsRaw, ctx.probeRequests)
    val hypothesisRequests = ctx.probeRequests.filter(isHypothesisProbeRequest)
    val hypothesisProbeHitRate =
      if hypothesisRequests.isEmpty then 1.0
      else
        val validIds = validation.validResults.map(_.id).toSet
        hypothesisRequests.count(req => validIds.contains(req.id)).toDouble / hypothesisRequests.size.toDouble

    val contractDropRate =
      if rootProbeResultsRaw.isEmpty then 0.0
      else validation.droppedCount.toDouble / rootProbeResultsRaw.size.toDouble

    val legacyFenMissingRate =
      if rawProbeResults.isEmpty then 0.0
      else rawProbeResults.count(_.fen.isEmpty).toDouble / rawProbeResults.size.toDouble

    StrategicMetrics(
      planRecallAt3 = planRecallAt3,
      planRecallAt3Legacy = planRecallAt3Legacy,
      planRecallAt3HypothesisFirst = planRecallAt3HypothesisFirst,
      latentPrecision = latentPrecision,
      pvCouplingRatio = pvCouplingRatio,
      evidenceReasonCoverage = evidenceReasonCoverage,
      hypothesisProbeHitRate = hypothesisProbeHitRate,
      contractDropRate = contractDropRate,
      legacyFenMissingRate = legacyFenMissingRate
    )

  private def isHypothesisProbeRequest(req: ProbeRequest): Boolean =
    req.seedId.exists(_.trim.nonEmpty) ||
      req.purpose.exists { purpose =>
        purpose == "free_tempo_branches" ||
        purpose == "latent_plan_immediate" ||
        purpose == "latent_plan_refutation"
      }

  private val moveTokenRegex  = """(?i)\b(?:[KQRBN]?[a-h]?[1-8]?x?[a-h][1-8](?:=[QRBN])?[+#]?|O-O(?:-O)?)\b""".r
  private val scoreTokenRegex = """(?i)(?:\([+\-]\d+\.\d\)|\bmate\b|\bcp\b|#[0-9]+)""".r
  private val tokenRegex      = """[A-Za-z][A-Za-z0-9'\-]{1,}""".r
  private val sentenceSplitRegex = """(?<=[\.\!\?])\s+|\n+""".r
  private val openingPrecedentRegex =
    """(?i)\bin\s+[A-Za-z][A-Za-z\.\-'\s]+-[A-Za-z][A-Za-z\.\-'\s]+\s+\(\d{4}""".r
  private val oppositionMentionRegex =
    """(?i)\b(?:direct|distant|diagonal|king)\s+opposition\b""".r

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

  private val regressionTemplatePhrases = List(
    "is playable in practice, but it asks for careful handling",
    "remains usable, but the downside is that",
    "engine-wise **",
    "this sits around the",
    "is accurate and consistent with the main plan",
    "is fully sound and matches the position's demands",
    "is a workable practical choice, but it leaves little room for imprecision"
  )

  private def computeQualityMetrics(prose: String, fen: String, vars: List[VariationLine]): QualityMetrics =
    val lower = prose.toLowerCase

    val tokens = tokenRegex.findAllIn(lower).toList
    val uniqueTokens = tokens.distinct
    val lexicalDiversity =
      if tokens.isEmpty then 0.0
      else uniqueTokens.size.toDouble / tokens.size.toDouble

    val trigramCounts         = ngramCounts(tokens, 3)
    val fourgramCounts        = ngramCounts(tokens, 4)
    val repeatedTrigramTypes  = trigramCounts.count(_._2 >= 3)
    val repeatedFourgramTypes = fourgramCounts.count(_._2 >= 2)
    val maxNgramRepeat        = (trigramCounts.valuesIterator ++ fourgramCounts.valuesIterator).foldLeft(0)(Math.max)

    val sentences           = extractSentences(prose)
    val sentenceNorm        = sentences.map(normalizeSentence).filter(_.length >= 8)
    val uniqueSentenceCount = sentenceNorm.distinct.size
    val sentenceCount       = sentenceNorm.size
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

    val moveTokenCount  = moveTokenRegex.findAllIn(prose).length
    val scoreTokenCount = scoreTokenRegex.findAllIn(prose).length

    val anchors =
      vars.take(3).flatMap(v => NarrativeUtils.uciListToSan(fen, v.moves).headOption).map(_.toLowerCase)
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
      if repeatedTrigramTypes >= 4 then s -= 10
      else if repeatedTrigramTypes >= 2 then s -= 6
      if repeatedFourgramTypes >= 2 then s -= 10
      else if repeatedFourgramTypes >= 1 then s -= 5
      if maxNgramRepeat >= 4 then s -= 6
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
      repeatedTrigramTypes = repeatedTrigramTypes,
      repeatedFourgramTypes = repeatedFourgramTypes,
      maxNgramRepeat = maxNgramRepeat,
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
      Option.when(q.repeatedTrigramTypes >= 4)(
        s"High repeated trigram patterns: ${q.repeatedTrigramTypes}"
      ),
      Option.when(q.repeatedFourgramTypes >= 2)(
        s"High repeated four-gram patterns: ${q.repeatedFourgramTypes}"
      ),
      Option.when(q.mateToneConflictHits > 0)(s"Mate-tone conflict: ${q.mateToneConflictHits}"),
      Option.when(q.variationAnchorCoverage < 0.50)(
        f"Low variation anchor coverage: ${q.variationAnchorCoverage}%.2f"
      ),
      Option.when(q.moveTokenCount < 6)(s"Low move-token density: ${q.moveTokenCount}"),
      Option.when(q.qualityScore < 70)(s"Low quality score: ${q.qualityScore}/100")
    ).flatten

  private def advisoryFindingsFrom(q: QualityMetrics): List[String] =
    List(
      Option.when(q.qualityScore < 90)(s"Advisory: quality score below target (90): ${q.qualityScore}"),
      Option.when(q.boilerplateHits == 1)(s"Advisory: boilerplate phrase present (1)"),
      Option.when(q.repeatedTrigramTypes >= 2 && q.repeatedTrigramTypes < 4)(
        s"Advisory: repeated trigram templates present (${q.repeatedTrigramTypes})"
      ),
      Option.when(q.repeatedFourgramTypes == 1)(
        s"Advisory: repeated four-gram templates present (1)"
      ),
      Option.when(q.lexicalDiversity < 0.65)(f"Advisory: lexical diversity soft-low: ${q.lexicalDiversity}%.2f")
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

  private def precedentMentionCount(prose: String): Int =
    openingPrecedentRegex.findAllMatchIn(prose).length

  private def isCoreOpeningEvent(event: OpeningEvent): Boolean = event match
    case OpeningEvent.BranchPoint(_, _, _) => true
    case OpeningEvent.OutOfBook(_, _, _)   => true
    case OpeningEvent.TheoryEnds(_, _)     => true
    case OpeningEvent.Novelty(_, _, _, _)  => true
    case OpeningEvent.Intro(_, _, _, _)    => false

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

    val effectiveMustNotContain = (e.mustNotContain ++ regressionTemplatePhrases).distinct

    val mustNotContainFails =
      effectiveMustNotContain.flatMap { token =>
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

  private def balancedGate(results: List[CaseResult]): BalancedGateStatus =
    val totalCases = results.size
    val eligiblePrecedentCases = results.count(_.openingPrecedentEligible)
    val precedentCoveredCases = results.count(_.openingPrecedentObserved)
    val requiredPrecedentCases =
      if eligiblePrecedentCases <= 0 then 0
      else Math.ceil(eligiblePrecedentCases.toDouble * 0.8).toInt
    val repeatedFivegramViolations =
      repeatedNgramsAcrossCases(results, n = 5, minCases = 4).size

    BalancedGateStatus(
      totalCases = totalCases,
      eligiblePrecedentCases = eligiblePrecedentCases,
      precedentCoveredCases = precedentCoveredCases,
      requiredPrecedentCases = requiredPrecedentCases,
      repeatedFivegramViolations = repeatedFivegramViolations,
      allowedFivegramViolations = AllowedRepeatedFivegramViolations
    )

  private def renderReport(
      corpusPath: String,
      results: List[CaseResult],
      endgameGoldMetrics: Option[EndgameGoldMetrics]
  ): String =
    val sb = new StringBuilder()

    val total             = results.size
    val failed            = results.count(!_.passed)
    val passed            = total - failed
    val qualityList       = results.flatMap(_.quality)
    val avgQuality        = average(qualityList.map(_.qualityScore.toDouble))
    val avgLexical        = average(qualityList.map(_.lexicalDiversity))
    val avgAnchorCoverage = average(qualityList.map(_.variationAnchorCoverage))
    val strategicList     = results.flatMap(_.strategic)
    val avgPlanRecallAt3  = average(strategicList.map(_.planRecallAt3))
    val avgPlanRecallAt3Legacy = average(strategicList.map(_.planRecallAt3Legacy))
    val avgPlanRecallAt3HypothesisFirst = average(strategicList.map(_.planRecallAt3HypothesisFirst))
    val avgLatentPrecision = average(strategicList.map(_.latentPrecision))
    val avgPvCouplingRatio = average(strategicList.map(_.pvCouplingRatio))
    val avgEvidenceReasonCoverage = average(strategicList.map(_.evidenceReasonCoverage))
    val avgHypothesisProbeHitRate = average(strategicList.map(_.hypothesisProbeHitRate))
    val avgContractDropRate = average(strategicList.map(_.contractDropRate))
    val avgLegacyFenMissingRate = average(strategicList.map(_.legacyFenMissingRate))
    val lowQualityCases   = results.filter(_.quality.exists(_.qualityScore < 70))
    val advisoryCases     = results.filter(_.advisoryFindings.nonEmpty)
    val advisoryCount     = results.map(_.advisoryFindings.size).sum
    val precedentMentions = results.flatMap(_.prose).map(precedentMentionCount).sum
    val precedentCases    = results.count(_.openingPrecedentObserved)
    val gate              = balancedGate(results)
    val endgameCases      = results.count(_.endgame.isDefined)
    val endgameNarrativeCases = results.count(r => r.endgame.isDefined && r.prose.isDefined)
    val contradictionCount = endgameContradictionCount(results)
    val contradictionRate =
      if endgameNarrativeCases == 0 then 0.0
      else contradictionCount.toDouble / endgameNarrativeCases.toDouble
    val analysisLatencyP95 = percentileLong(results.flatMap(_.analysisLatencyMs), 0.95)
    val endgameGate = endgameGateStatus(results, endgameGoldMetrics)

    val crossCaseSentenceMap = repeatedSentencesAcrossCases(results)
    val topRepeatedSentences = crossCaseSentenceMap.toList.sortBy((_, c) => -c).take(12)
    val crossCaseFivegramMap = repeatedNgramsAcrossCases(results, n = 5, minCases = 4)
    val topRepeatedFivegrams = crossCaseFivegramMap.toList.sortBy((_, c) => -c).take(16)

    sb.append("# Book Commentary Corpus Report\n\n")
    sb.append(s"- Corpus: `$corpusPath`\n")
    sb.append(s"- Results: $passed/$total passed\n\n")
    if qualityList.nonEmpty then
      sb.append("## Quality Summary\n\n")
      sb.append(f"- Avg quality score: $avgQuality%.1f / 100\n")
      sb.append(f"- Avg lexical diversity: $avgLexical%.3f\n")
      sb.append(f"- Avg variation-anchor coverage: $avgAnchorCoverage%.3f\n")
      sb.append(f"- Strategic metric PlanRecall@3(active): $avgPlanRecallAt3%.3f\n")
      sb.append(f"- Strategic metric PlanRecall@3(legacy top-rule match): $avgPlanRecallAt3Legacy%.3f\n")
      sb.append(f"- Strategic metric PlanRecall@3(hypothesis-first): $avgPlanRecallAt3HypothesisFirst%.3f\n")
      sb.append(f"- Strategic metric LatentPrecision: $avgLatentPrecision%.3f\n")
      sb.append(f"- Strategic metric PV-coupling ratio: $avgPvCouplingRatio%.3f\n")
      sb.append(f"- Strategic metric EvidenceReasonCoverage: $avgEvidenceReasonCoverage%.3f\n")
      sb.append(f"- Strategic metric HypothesisProbeHitRate: $avgHypothesisProbeHitRate%.3f\n")
      sb.append(f"- Strategic metric ContractDropRate: $avgContractDropRate%.3f\n")
      sb.append(f"- Strategic metric LegacyFenMissingRate: $avgLegacyFenMissingRate%.3f\n")
      sb.append(
        s"- Opening precedent mentions: $precedentMentions across $precedentCases/${gate.eligiblePrecedentCases} eligible cases\n"
      )
      sb.append(
        s"- Balanced gate: ${if gate.passed then "PASS" else "FAIL"} (repeated 5-gram [4+ cases]=${gate.repeatedFivegramViolations}, target<=${gate.allowedFivegramViolations}; precedent coverage=${gate.precedentCoveredCases}/${gate.eligiblePrecedentCases}, target>=${gate.requiredPrecedentCases})\n"
      )
      sb.append(
        f"- Endgame gate: ${if endgameGate.passed then "PASS" else "FAIL"} (F1=${endgameGate.conceptF1.getOrElse(0.0)}%.3f target>=0.85, contradictionRate=${endgameGate.contradictionRate}%.3f target<0.02, oracleP95Ms=${endgameGate.oracleLatencyP95Ms.getOrElse(0.0)}%.3f target<=40)\n"
      )
      sb.append(s"- Endgame cases: $endgameCases/$total (narrative-checked: $endgameNarrativeCases)\n")
      sb.append(f"- Endgame contradiction count: $contradictionCount (rate=$contradictionRate%.3f)\n")
      sb.append(f"- Analysis latency p95 (all corpus cases): $analysisLatencyP95%.3f ms\n")
      endgameGoldMetrics.foreach { gm =>
        sb.append(
          f"- Endgame goldset: rows=${gm.rows}, checks=${gm.labeledChecks}, matched=${gm.matchedChecks}, mismatched=${gm.mismatchedChecks}, conceptF1=${gm.conceptF1}%.3f, oracleP95Ms=${gm.oracleLatencyP95Ms}%.3f\n"
        )
      }
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
      if topRepeatedFivegrams.isEmpty then sb.append("- No 5-gram pattern repeated across 4+ cases.\n\n")
      else
        sb.append("### Repeated 5-gram Patterns\n\n")
        topRepeatedFivegrams.foreach { (s, n) =>
          sb.append(s"""- [$n cases] "$s"\n""")
        }
        sb.append("\n")

    results.foreach { r =>
      sb.append(s"## ${r.c.id}: ${r.c.title}\n\n")
      sb.append(s"- `ply`: ${r.c.ply}\n")
      sb.append(s"- `playedMove`: `${r.c.playedMove}`\n")
      r.fen.foreach(f => sb.append(s"- `analysisFen`: `$f`\n"))
      r.metrics.foreach { m => sb.append(s"- Metrics: ${m.chars} chars, ${m.paragraphs} paragraphs\n") }
      r.analysisLatencyMs.foreach(ms => sb.append(s"- Analysis latency: ${ms} ms\n"))
      r.prose.foreach(p => sb.append(s"- Precedent mentions: ${precedentMentionCount(p)}\n"))
      sb.append(
        s"- Opening precedent: eligible=${r.openingPrecedentEligible}, observed=${r.openingPrecedentObserved}\n"
      )
      if r.semanticConcepts.nonEmpty then
        sb.append(s"- Semantic concepts: ${r.semanticConcepts.mkString(", ")}\n")
      r.endgame.foreach { eg =>
        sb.append(
          f"- Endgame oracle: opposition=${eg.oppositionType}, ruleOfSquare=${eg.ruleOfSquare}, triangulation=${eg.triangulationAvailable}, rookPattern=${eg.rookEndgamePattern}, zug=${eg.zugzwangLikelihood}%.3f, outcome=${eg.outcomeHint}, conf=${eg.confidence}%.3f, pattern=${eg.primaryPattern.getOrElse("None")}\n"
        )
      }
      r.quality.foreach { q =>
        sb.append(
          f"- Quality: score=${q.qualityScore}%d/100, lexical=${q.lexicalDiversity}%.3f, uniqueSent=${q.uniqueSentenceRatio}%.3f, anchorCoverage=${q.variationAnchorCoverage}%.3f\n"
        )
        sb.append(
          s"- Quality details: sentences=${q.sentenceCount}, dup=${q.duplicateSentenceCount}, triRepeat=${q.repeatedTrigramTypes}, fourRepeat=${q.repeatedFourgramTypes}, maxNgramRepeat=${q.maxNgramRepeat}, boilerplate=${q.boilerplateHits}, mateToneConflict=${q.mateToneConflictHits}, moveTokens=${q.moveTokenCount}, scoreTokens=${q.scoreTokenCount}\n"
        )
      }
      r.strategic.foreach { s =>
        sb.append(
          f"- Strategic: PlanRecall@3=${s.planRecallAt3}%.3f, PlanRecall@3Legacy=${s.planRecallAt3Legacy}%.3f, PlanRecall@3HypothesisFirst=${s.planRecallAt3HypothesisFirst}%.3f, LatentPrecision=${s.latentPrecision}%.3f, PV-coupling=${s.pvCouplingRatio}%.3f, EvidenceReasonCoverage=${s.evidenceReasonCoverage}%.3f, HypothesisProbeHitRate=${s.hypothesisProbeHitRate}%.3f, ContractDropRate=${s.contractDropRate}%.3f, LegacyFenMissingRate=${s.legacyFenMissingRate}%.3f\n"
        )
      }
      if r.qualityFindings.nonEmpty then
        sb.append("- Quality findings:\n")
        r.qualityFindings.foreach(f => sb.append(s"  - $f\n"))
      if r.advisoryFindings.nonEmpty then
        sb.append("- Advisory findings:\n")
        r.advisoryFindings.foreach(f => sb.append(s"  - $f\n"))
      r.outline.foreach { o =>
        val kinds = if o.kinds.nonEmpty then o.kinds.mkString(", ") else "-"
        sb.append(s"- Outline: ${o.beats} beats ($kinds)\n")
        o.evidence.foreach { e =>
          val p = e.purpose.getOrElse("-")
          sb.append(s"- Evidence: purpose=$p, branches=${e.branches}\n")
        }
      }

      if r.failures.nonEmpty then
        sb.append("- Failures:\n")
        r.failures.foreach(f => sb.append(s"  - $f\n"))

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

  private def repeatedNgramsAcrossCases(results: List[CaseResult], n: Int, minCases: Int): Map[String, Int] =
    val perCaseNgramSets = results.flatMap(_.prose).map { prose =>
      val tokens = tokenRegex.findAllIn(prose.toLowerCase).map(_.trim).filter(_.length >= 3).toList
      ngrams(tokens, n).toSet
    }
    perCaseNgramSets
      .flatten
      .groupBy(identity)
      .view
      .mapValues(_.size)
      .filter(_._2 >= minCases)
      .toMap

  private def ngramCounts(tokens: List[String], n: Int): Map[String, Int] =
    ngrams(tokens, n).groupBy(identity).view.mapValues(_.size).toMap

  private def ngrams(tokens: List[String], n: Int): List[String] =
    if n <= 0 || tokens.lengthCompare(n) < 0 then Nil
    else tokens.sliding(n).map(_.mkString(" ")).toList

  private def average(xs: List[Double]): Double =
    if xs.isEmpty then 0.0
    else xs.sum / xs.size

  private def optionValue(args: List[String], flag: String): Option[String] =
    args
      .sliding(2)
      .collectFirst { case List(f, v) if f == flag => v }

  private def positionalArgs(args: List[String]): List[String] =
    val flagsWithValue = Set("--endgame-goldset")
    val out = scala.collection.mutable.ListBuffer[String]()
    var i = 0
    while i < args.size do
      val token = args(i)
      if flagsWithValue.contains(token) then i += 2
      else if token.startsWith("--") then i += 1
      else
        out += token
        i += 1
    out.toList

  private def percentileLong(values: List[Long], p: Double): Double =
    if values.isEmpty then 0.0
    else
      val sorted = values.sorted
      val idx = math.ceil(p * sorted.size.toDouble).toInt - 1
      sorted(idx.max(0).min(sorted.size - 1)).toDouble

  private def endgameContradictionCount(results: List[CaseResult]): Int =
    results.count { r =>
      (r.prose, r.endgame) match
        case (Some(prose), Some(eg)) =>
          val low = prose.toLowerCase
          val patternAllowsZugMention =
            eg.primaryPattern.exists { p =>
              val normalized = p.toLowerCase
              normalized.contains("triangulationzugzwang") || normalized.contains("shouldering")
            }
          val semanticAllowsZugMention =
            r.semanticConcepts.exists(_.toLowerCase.contains("zugzwang"))
          val zugMentioned = low.contains("zugzwang")
          val oppositionMentioned = oppositionMentionRegex.findFirstIn(prose).nonEmpty
          val zugMismatch =
            zugMentioned &&
              !patternAllowsZugMention &&
              !semanticAllowsZugMention &&
              !eg.isZugzwang &&
              eg.zugzwangLikelihood < 0.65
          val oppMismatch = oppositionMentioned && eg.oppositionType.equalsIgnoreCase("None")
          val ruleMismatch = low.contains("rule of the square") && eg.ruleOfSquare.equalsIgnoreCase("NA")
          val winMismatch =
            (low.contains("winning") || low.contains("clearly winning")) &&
              eg.outcomeHint.equalsIgnoreCase("Draw") &&
              eg.confidence >= 0.70
          zugMismatch || oppMismatch || ruleMismatch || winMismatch
        case _ => false
    }

  private def endgameGateStatus(
      results: List[CaseResult],
      goldMetrics: Option[EndgameGoldMetrics]
  ): EndgameGateStatus =
    val narrativeCases = results.count(r => r.endgame.isDefined && r.prose.isDefined)
    val contradictions = endgameContradictionCount(results)
    val contradictionRate =
      if narrativeCases == 0 then 0.0
      else contradictions.toDouble / narrativeCases.toDouble
    EndgameGateStatus(
      conceptF1 = goldMetrics.map(_.conceptF1),
      contradictionRate = contradictionRate,
      oracleLatencyP95Ms = goldMetrics.map(_.oracleLatencyP95Ms)
    )

  private[llm] def evaluateBalancedGateForTest(results: List[CaseResult]): BalancedGateStatus =
    balancedGate(results)

  private[llm] def countEndgameContradictionsForTest(results: List[CaseResult]): Int =
    endgameContradictionCount(results)

  private def evaluateEndgameGoldset(path: Path): Either[String, EndgameGoldMetrics] =
    if !Files.exists(path) then Left("file does not exist")
    else
      val linesEither =
        try Right(Files.readAllLines(path, StandardCharsets.UTF_8).toArray.toList.map(_.toString))
        catch case NonFatal(e) => Left(e.getMessage)

      linesEither.flatMap { lines =>
        val cleanLines = lines.map(_.trim).filter(l => l.nonEmpty && !l.startsWith("#"))

        val validatedRowsEither =
          cleanLines.zipWithIndex.foldLeft[Either[String, List[(EndgameGoldRow, _root_.chess.Color, chess.Board)]]](Right(Nil)) {
            case (Left(err), _) => Left(err)
            case (Right(acc), (line, idx)) =>
              Json.parse(line).validate[EndgameGoldRow] match
                case JsError(errs) =>
                  Left(s"invalid jsonl row at line ${idx + 1}: ${errs.toString}")
                case JsSuccess(row, _) =>
                  val colorOpt =
                    if row.color.equalsIgnoreCase("white") then Some(_root_.chess.Color.White)
                    else if row.color.equalsIgnoreCase("black") then Some(_root_.chess.Color.Black)
                    else None
                  colorOpt match
                    case None => Left(s"invalid color `${row.color}` at line ${idx + 1}")
                    case Some(color) =>
                      chess.format.Fen.read(chess.variant.Standard, chess.format.Fen.Full(row.fen)).map(_.board) match
                        case None => Left(s"invalid FEN at line ${idx + 1}: ${row.fen}")
                        case Some(board) => Right((row, color, board) :: acc)
          }

        validatedRowsEither.map { validatedRowsRev =>
          val validatedRows = validatedRowsRev.reverse
          val analyzer = new EndgameAnalyzerImpl()
          var checks = 0
          var matched = 0
          var mismatched = 0
          val latenciesMs = scala.collection.mutable.ListBuffer[Long]()

          def normalized(s: String): String = s.trim.toLowerCase
          def compare(check: Option[Boolean]): Unit =
            check.foreach { ok =>
              checks += 1
              if ok then matched += 1 else mismatched += 1
            }

          validatedRows.foreach { case (row, color, board) =>
            val started = System.nanoTime()
            val featureOpt = analyzer.analyze(board, color)
            latenciesMs += ((System.nanoTime() - started) / 1000000L).max(0L)

            compare(row.expect.oppositionType.map { expected =>
              featureOpt.exists(f => normalized(f.oppositionType.toString) == normalized(expected))
            })
            compare(row.expect.ruleOfSquare.map { expected =>
              featureOpt.exists(f => normalized(f.ruleOfSquare.toString) == normalized(expected))
            })
            compare(row.expect.triangulationAvailable.map { expected =>
              featureOpt.exists(_.triangulationAvailable == expected)
            })
            compare(row.expect.rookEndgamePattern.map { expected =>
              featureOpt.exists(f => normalized(f.rookEndgamePattern.toString) == normalized(expected))
            })
            compare(row.expect.zugzwangLikely.map { expected =>
              featureOpt.exists { f =>
                val predicted = f.isZugzwang || f.zugzwangLikelihood >= 0.65
                predicted == expected
              }
            })
          }

          EndgameGoldMetrics(
            rows = validatedRows.size,
            labeledChecks = checks,
            matchedChecks = matched,
            mismatchedChecks = mismatched,
            conceptF1 = if checks == 0 then 0.0 else matched.toDouble / checks.toDouble,
            oracleLatencyP95Ms = percentileLong(latenciesMs.toList, 0.95)
          )
        }
      }

  private def readJsonFile(path: Path): Either[String, JsValue] =
    try
      val raw = Files.readString(path, StandardCharsets.UTF_8)
      Right(Json.parse(raw))
    catch case NonFatal(e) => Left(e.toString)

  private def writeText(path: Path, text: String): Unit =
    Files.writeString(path, text, StandardCharsets.UTF_8)
