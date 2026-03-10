package lila.llm.analysis

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path }

import scala.concurrent.{ Await, ExecutionContext }
import scala.util.control.NonFatal

import akka.actor.ActorSystem
import lila.llm.*
import lila.llm.model.*
import lila.llm.model.strategic.VariationLine
import play.api.libs.functional.syntax.*
import play.api.libs.json.*
import play.api.libs.ws.ahc.StandaloneAhcWSClient
import play.shaded.ahc.org.asynchttpclient.DefaultAsyncHttpClient

object CoreCommentaryCloseoutQaRunner:

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
      probeResults: Option[List[ProbeResult]],
      virtualMotifs: List[String]
  )

  object CorpusCase:
    given Reads[CorpusCase] =
      (
        (JsPath \ "id").read[String] and
          (JsPath \ "title").read[String] and
          (JsPath \ "analysisFen").readNullable[String] and
          (JsPath \ "startFen").readNullable[String] and
          (JsPath \ "preMovesUci").readNullable[List[String]] and
          (JsPath \ "ply").read[Int] and
          (JsPath \ "playedMove").read[String] and
          (JsPath \ "opening").readNullable[String] and
          (JsPath \ "phase").read[String] and
          (JsPath \ "openingRef").readNullable[OpeningReference] and
          (JsPath \ "variations").readWithDefault[List[VariationLine]](Nil) and
          (JsPath \ "probeResults").readNullable[List[ProbeResult]] and
          (JsPath \ "virtualMotifs").readWithDefault[List[String]](Nil)
      )(CorpusCase.apply)

  final case class Corpus(version: Int, cases: List[CorpusCase])
  object Corpus:
    given Reads[Corpus] =
      (
        (JsPath \ "version").read[Int] and
          (JsPath \ "cases").read[List[CorpusCase]]
      )(Corpus.apply)

  private final case class DirectPolishMetrics(
      provider: String,
      model: Option[String],
      promptTokens: Option[Int],
      cachedTokens: Option[Int],
      completionTokens: Option[Int],
      estimatedCostUsd: Option[Double],
      rawPolished: String,
      finalPolished: String,
      softRepairApplied: Boolean,
      softRepairMaterialApplied: Boolean,
      softRepairActions: List[String],
      softRepairMaterialActions: List[String]
  )

  private final case class ActiveNoteMetrics(
      provider: String,
      model: Option[String],
      promptTokens: Option[Int],
      cachedTokens: Option[Int],
      completionTokens: Option[Int],
      estimatedCostUsd: Option[Double],
      rawNote: String,
      finalNote: String,
      repairApplied: Boolean
  )

  private final case class FullGameParityMetrics(
      focusedCommentary: String,
      thesisPreserved: Boolean,
      alternativeObserved: Boolean,
      structureObserved: Boolean,
      evidenceSurfaceVisible: Boolean
  )

  private final case class QaCaseResult(
      c: CorpusCase,
      ctx: NarrativeContext,
      dominantClaim: String,
      ruleCommentary: String,
      finalCommentary: String,
      evaluation: BookmakerProseContract.Evaluation,
      rawEvaluation: Option[BookmakerProseContract.Evaluation],
      directPolish: Option[DirectPolishMetrics],
      strategyPack: Option[StrategyPack],
      signalDigest: Option[NarrativeSignalDigest],
      fullGame: FullGameParityMetrics,
      activeNote: Option[ActiveNoteMetrics],
      openingEligible: Boolean,
      openingObserved: Boolean,
      structureEligible: Boolean,
      structureObserved: Boolean,
      alternativeEligible: Boolean,
      alternativeObserved: Boolean,
      endgameEligible: Boolean,
      endgameObserved: Boolean,
      thesisAgreementObserved: Boolean
  )

  private val StopWords = Set(
    "this", "that", "with", "from", "into", "where", "because", "belongs", "calls",
    "move", "plan", "long", "term", "structure", "keeps", "stays", "there", "their",
    "your", "have", "will", "after", "before", "than", "then", "here", "when", "line",
    "branch", "side", "piece", "pieces", "route", "routes", "through", "around"
  )

  def main(args: Array[String]): Unit =
    val corpusPath = args.headOption.getOrElse("modules/llm/docs/BookCommentaryCorpus_refreshed.json")
    val outPath = args.lift(1).getOrElse("modules/llm/docs/CoreCommentaryCloseoutQaReport.md")
    given Executor = ExecutionContext.global
    given ActorSystem = ActorSystem("core-commentary-closeout-qa")

    val ws = new StandaloneAhcWSClient(new DefaultAsyncHttpClient())
    val openingExplorer = OpeningExplorerClient(ws)
    try
      val liveProviderAvailable =
        sys.env.get("LLM_PROVIDER").exists(v => v == "openai" || v == "gemini") &&
          (sys.env.get("OPENAI_API_KEY").exists(_.trim.nonEmpty) || sys.env.get("GEMINI_API_KEY").exists(_.trim.nonEmpty))

      val openAi = OpenAiClient(ws, OpenAiConfig.fromEnv)
      val gemini = GeminiClient(ws, GeminiConfig.fromEnv)
      val corpus = Json.parse(Files.readString(Path.of(corpusPath), StandardCharsets.UTF_8)).as[Corpus]
      val corpusCases = corpus.cases.take(50)
      val corpusResults = corpusCases.flatMap(runCase(_, openingExplorer, openAi, gemini, liveProviderAvailable))
      val structureSupplements = structureFixtureCases.flatMap(runFixtureCase(_, openAi, gemini, liveProviderAvailable))
      val results = corpusResults ++ structureSupplements
      val report =
        renderReport(
          corpusPath = corpusPath,
          results = results,
          liveProviderAvailable = liveProviderAvailable,
          corpusCount = corpusCases.size,
          structureSupplementCount = structureSupplements.size
        )
      Files.writeString(Path.of(outPath), report, StandardCharsets.UTF_8)
      println(s"[closeout-qa] wrote $outPath")
    finally
      ws.close()
      summon[ActorSystem].terminate()

  private def runCase(
      c: CorpusCase,
      openingExplorer: OpeningExplorerClient,
      openAi: OpenAiClient,
      gemini: GeminiClient,
      liveProviderAvailable: Boolean
  ): Option[QaCaseResult] =
    val fenOpt =
      c.analysisFen.orElse {
        for
          start <- c.startFen
          pre <- c.preMovesUci
        yield NarrativeUtils.uciListToFen(start, pre)
      }
    fenOpt.flatMap { fen =>
      val openingRef = c.openingRef.orElse(fetchOpeningRef(openingExplorer, fen, c.phase))
      CommentaryEngine.assessExtended(
        fen = fen,
        variations = c.variations,
        playedMove = Some(c.playedMove),
        opening = c.opening,
        phase = Some(c.phase),
        ply = c.ply,
        prevMove = Some(c.playedMove),
        probeResults = c.probeResults.getOrElse(Nil)
      ).flatMap { data =>
        val bookmakerCtx =
          NarrativeContextBuilder.build(
            data = data,
            ctx = data.toContext,
            prevAnalysis = None,
            probeResults = c.probeResults.getOrElse(Nil),
            openingRef = openingRef,
            prevOpeningRef = None,
            openingBudget = OpeningEventBudget(),
            afterAnalysis = None,
            renderMode = NarrativeRenderMode.Bookmaker
          )
        val outline = BookStyleRenderer.validatedOutline(bookmakerCtx)
        BookmakerPolishSlotsBuilder.build(bookmakerCtx, outline, refs = None).map { slots =>
          val dominantClaim = StrategicThesisBuilder.build(bookmakerCtx).map(_.claim).getOrElse(slots.claim)
          val ruleCommentary = BookStyleRenderer.renderValidatedOutline(outline, bookmakerCtx)
          val directPolish =
            if liveProviderAvailable then runDirectPolish(openAi, gemini, bookmakerCtx, ruleCommentary, slots)
            else None
          val finalCommentary =
            directPolish.map(_.finalPolished).getOrElse(BookmakerSoftRepair.repair(ruleCommentary, slots).text)
          val evaluation = BookmakerProseContract.evaluate(finalCommentary, slots)
          val rawEvaluation = directPolish.map(dp => BookmakerProseContract.evaluate(dp.rawPolished, slots))

          val signalDigest = NarrativeSignalDigestBuilder.build(bookmakerCtx)
          val strategyPack = StrategyPackBuilder.build(data, bookmakerCtx)
          val alternative = AlternativeNarrativeSupport.build(bookmakerCtx)
          val structureArc = StructurePlanArcBuilder.build(bookmakerCtx)
          val precedent = OpeningPrecedentBranching.representative(bookmakerCtx, openingRef, requireFocus = true)
          val fullGame = buildFullGameParity(data, c, openingRef, dominantClaim, alternative, structureArc)
          val activeNote =
            if liveProviderAvailable then runActiveNote(openAi, gemini, c, bookmakerCtx, finalCommentary, strategyPack)
            else None

          val openingEligible =
            c.phase.equalsIgnoreCase("opening") &&
              !CriticalAnnotationPolicy.shouldPrioritizeClaim(bookmakerCtx) &&
              (bookmakerCtx.openingEvent.exists {
                case OpeningEvent.BranchPoint(_, _, _) | OpeningEvent.OutOfBook(_, _, _) |
                    OpeningEvent.TheoryEnds(_, _) | OpeningEvent.Novelty(_, _, _, _) =>
                  true
                case _ => false
              } || bookmakerCtx.openingData.flatMap(_.name).exists(_.trim.nonEmpty)) &&
              precedent.isDefined

          val openingObserved =
            openingEligible &&
              mentionsOpeningBranch(finalCommentary, bookmakerCtx.openingData.flatMap(_.name), precedent)

          val structureEligible =
            structureArc.exists(arc =>
              arc.alignmentBand.exists(b => Set("onbook", "playable").contains(b.trim.toLowerCase)) &&
                arc.primaryDeployment.confidence >= 0.62
            )

          val structureObserved =
            structureEligible &&
              mentionsStructureDeployment(finalCommentary, structureArc, signalDigest)

          val alternativeEligible = alternative.isDefined
          val alternativeObserved =
            alternative.exists(alt => AlternativeNarrativeSupport.observedIn(finalCommentary, alt))

          val endgameEligible =
            bookmakerCtx.semantic.flatMap(_.endgameFeatures).exists(endgameEligibleInfo)
          val endgameObserved =
            bookmakerCtx.semantic.flatMap(_.endgameFeatures).exists(info =>
              mentionsEndgame(finalCommentary, info, c.virtualMotifs) ||
                mentionsEndgame(fullGame.focusedCommentary, info, c.virtualMotifs)
            )

          val thesisAgreementObserved =
            activeNote.exists(note =>
              mentionsDominantThesis(finalCommentary, dominantClaim, structureArc, signalDigest, precedent) &&
                mentionsDominantThesis(note.finalNote, dominantClaim, structureArc, signalDigest, precedent)
            )

          QaCaseResult(
            c = c,
            ctx = bookmakerCtx,
            dominantClaim = dominantClaim,
            ruleCommentary = ruleCommentary,
            finalCommentary = finalCommentary,
            evaluation = evaluation,
            rawEvaluation = rawEvaluation,
            directPolish = directPolish,
            strategyPack = strategyPack,
            signalDigest = signalDigest,
            fullGame = fullGame,
            activeNote = activeNote,
            openingEligible = openingEligible,
            openingObserved = openingObserved,
            structureEligible = structureEligible,
            structureObserved = structureObserved,
            alternativeEligible = alternativeEligible,
            alternativeObserved = alternativeObserved,
            endgameEligible = endgameEligible,
            endgameObserved = endgameObserved,
            thesisAgreementObserved = thesisAgreementObserved
          )
        }
      }
    }

  private def runFixtureCase(
      fixture: BookmakerProseGoldenFixtures.Fixture,
      openAi: OpenAiClient,
      gemini: GeminiClient,
      liveProviderAvailable: Boolean
  ): Option[QaCaseResult] =
    val c =
      CorpusCase(
        id = s"fixture_${fixture.id}",
        title = s"${fixture.title} (structure supplement)",
        analysisFen = Some(fixture.ctx.fen),
        startFen = None,
        preMovesUci = None,
        ply = fixture.ctx.ply,
        playedMove = fixture.ctx.playedMove.getOrElse(""),
        opening = fixture.ctx.openingData.flatMap(_.name),
        phase = fixture.ctx.phase.current,
        openingRef = None,
        variations = Nil,
        probeResults = None,
        virtualMotifs = fixture.ctx.semantic.map(_.conceptSummary).getOrElse(Nil)
      )
    val ctx = fixture.ctx.copy(renderMode = NarrativeRenderMode.Bookmaker)
    val outline = BookStyleRenderer.validatedOutline(ctx)
    BookmakerPolishSlotsBuilder.build(ctx, outline, refs = None).map { slots =>
      val dominantClaim = StrategicThesisBuilder.build(ctx).map(_.claim).getOrElse(slots.claim)
      val ruleCommentary = BookStyleRenderer.renderValidatedOutline(outline, ctx)
      val directPolish =
        if liveProviderAvailable then runDirectPolish(openAi, gemini, ctx, ruleCommentary, slots)
        else None
      val finalCommentary =
        directPolish.map(_.finalPolished).getOrElse(BookmakerSoftRepair.repair(ruleCommentary, slots).text)
      val evaluation = BookmakerProseContract.evaluate(finalCommentary, slots)
      val rawEvaluation = directPolish.map(dp => BookmakerProseContract.evaluate(dp.rawPolished, slots))
      val signalDigest = NarrativeSignalDigestBuilder.build(ctx)
      val synthetic = syntheticData(ctx)
      val strategyPack = StrategyPackBuilder.build(synthetic, ctx)
      val alternative = AlternativeNarrativeSupport.build(ctx)
      val structureArc = StructurePlanArcBuilder.build(ctx)
      val fullGame = buildFullGameParity(synthetic, c, None, dominantClaim, alternative, structureArc)
      val activeNote =
        if liveProviderAvailable then runActiveNote(openAi, gemini, c, ctx, finalCommentary, strategyPack)
        else None

      val structureEligible =
        structureArc.exists(arc =>
          arc.alignmentBand.exists(b => Set("onbook", "playable").contains(b.trim.toLowerCase)) &&
            arc.primaryDeployment.confidence >= 0.62
        )
      val structureObserved =
        structureEligible &&
          mentionsStructureDeployment(finalCommentary, structureArc, signalDigest)
      val thesisAgreementObserved =
        activeNote.exists(note =>
          mentionsDominantThesis(finalCommentary, dominantClaim, structureArc, signalDigest, None) &&
            mentionsDominantThesis(note.finalNote, dominantClaim, structureArc, signalDigest, None)
        )

      QaCaseResult(
        c = c,
        ctx = ctx,
        dominantClaim = dominantClaim,
        ruleCommentary = ruleCommentary,
        finalCommentary = finalCommentary,
        evaluation = evaluation,
        rawEvaluation = rawEvaluation,
        directPolish = directPolish,
        strategyPack = strategyPack,
        signalDigest = signalDigest,
        fullGame = fullGame,
        activeNote = activeNote,
        openingEligible = false,
        openingObserved = false,
        structureEligible = structureEligible,
        structureObserved = structureObserved,
        alternativeEligible = alternative.isDefined,
        alternativeObserved = alternative.exists(alt => AlternativeNarrativeSupport.observedIn(finalCommentary, alt)),
        endgameEligible = false,
        endgameObserved = false,
        thesisAgreementObserved = thesisAgreementObserved
      )
    }

  private def buildFullGameParity(
      data: ExtendedAnalysisData,
      c: CorpusCase,
      openingRef: Option[OpeningReference],
      dominantClaim: String,
      alternative: Option[AlternativeNarrative],
      structureArc: Option[StructurePlanArc]
  ): FullGameParityMetrics =
    val fullGameCtx =
      NarrativeContextBuilder.build(
        data = data,
        ctx = data.toContext,
        prevAnalysis = None,
        probeResults = c.probeResults.getOrElse(Nil),
        openingRef = openingRef,
        prevOpeningRef = None,
        openingBudget = OpeningEventBudget(),
        afterAnalysis = None,
        renderMode = NarrativeRenderMode.FullGame
      )
    val rec = new TraceRecorder()
    val (outline, diag) = NarrativeOutlineBuilder.build(fullGameCtx, rec)
    val validated = NarrativeOutlineValidator.validate(outline, diag, rec, Some(fullGameCtx))
    val focused = CommentaryEngine.focusMomentOutline(validated, hasCriticalBranch = alternative.isDefined)
    val focusedCommentary = BookStyleRenderer.renderValidatedOutline(focused, fullGameCtx)
    val evidenceEligible = FullGameEvidenceSurfacePolicy.eligible("Strategic Moment", fullGameCtx, validated)
    val evidencePayload = FullGameEvidenceSurfacePolicy.payload(
      eligible = evidenceEligible,
      probeRequests = fullGameCtx.probeRequests,
      authorQuestions = AuthoringEvidenceSummaryBuilder.summarizeQuestions(fullGameCtx),
      authorEvidence = AuthoringEvidenceSummaryBuilder.summarizeEvidence(fullGameCtx)
    )
    val digest = NarrativeSignalDigestBuilder.build(fullGameCtx)
    val precedent = fullGameCtx.openingData.flatMap(ref => OpeningPrecedentBranching.representative(fullGameCtx, Some(ref), requireFocus = true))

    FullGameParityMetrics(
      focusedCommentary = focusedCommentary,
      thesisPreserved = mentionsDominantThesis(focusedCommentary, dominantClaim, structureArc, digest, precedent),
      alternativeObserved = alternative.exists(alt => AlternativeNarrativeSupport.observedIn(focusedCommentary, alt)),
      structureObserved = mentionsStructureDeployment(focusedCommentary, structureArc, digest),
      evidenceSurfaceVisible = evidencePayload.nonEmpty
    )

  private def runDirectPolish(
      openAi: OpenAiClient,
      gemini: GeminiClient,
      ctx: NarrativeContext,
      ruleCommentary: String,
      slots: BookmakerPolishSlots
  ): Option[DirectPolishMetrics] =
    val phase = normalizePhase(ctx.phase.current)
    val concepts = ctx.semantic.map(_.conceptSummary).getOrElse(Nil)
    sys.env.get("LLM_PROVIDER").map(_.trim.toLowerCase) match
      case Some("openai") =>
        Await.result(
          openAi.polishSync(
            prose = ruleCommentary,
            phase = phase,
            evalDelta = None,
            concepts = concepts,
            fen = ctx.fen,
            openingName = ctx.openingData.flatMap(_.name),
            bookmakerSlots = Some(slots)
          ),
          120.seconds
        ).map { result =>
          val normalized = unwrapCommentaryPayload(result.commentary)
          val repaired = BookmakerSoftRepair.repair(normalized, slots)
          DirectPolishMetrics(
            provider = "openai",
            model = Some(result.model),
            promptTokens = result.promptTokens,
            cachedTokens = result.cachedTokens,
            completionTokens = result.completionTokens,
            estimatedCostUsd = result.estimatedCostUsd,
            rawPolished = normalized,
            finalPolished = repaired.text,
            softRepairApplied = repaired.applied,
            softRepairMaterialApplied = repaired.materialApplied,
            softRepairActions = repaired.actions,
            softRepairMaterialActions = repaired.materialActions
          )
        }
      case Some("gemini") =>
        Await.result(
          gemini.polish(
            prose = ruleCommentary,
            phase = phase,
            evalDelta = None,
            concepts = concepts,
            fen = ctx.fen,
            openingName = ctx.openingData.flatMap(_.name),
            bookmakerSlots = Some(slots)
          ),
          120.seconds
        ).map { result =>
          val normalized = unwrapCommentaryPayload(result)
          val repaired = BookmakerSoftRepair.repair(normalized, slots)
          DirectPolishMetrics(
            provider = "gemini",
            model = sys.env.get("GEMINI_MODEL").filter(_.trim.nonEmpty),
            promptTokens = None,
            cachedTokens = None,
            completionTokens = None,
            estimatedCostUsd = None,
            rawPolished = normalized,
            finalPolished = repaired.text,
            softRepairApplied = repaired.applied,
            softRepairMaterialApplied = repaired.materialApplied,
            softRepairActions = repaired.actions,
            softRepairMaterialActions = repaired.materialActions
          )
        }
      case _ => None

  private def runActiveNote(
      openAi: OpenAiClient,
      gemini: GeminiClient,
      c: CorpusCase,
      ctx: NarrativeContext,
      baseNarrative: String,
      strategyPack: Option[StrategyPack]
  ): Option[ActiveNoteMetrics] =
    strategyPack.flatMap { pack =>
      val routeRefs = buildRouteRefs(pack)
      val moveRefs = buildMoveRefs(ctx.fen, c.variations)
      val phase = normalizePhase(ctx.phase.current)
      val momentType = StrategicThesisBuilder.build(ctx).map(_.lens.toString).getOrElse("Strategic Moment")
      val concepts = ctx.semantic.map(_.conceptSummary).getOrElse(Nil).take(8)
      sys.env.get("LLM_PROVIDER").map(_.trim.toLowerCase) match
        case Some("openai") =>
          val initial =
            Await.result(
              openAi.activeStrategicNoteSync(
                baseNarrative = baseNarrative,
                phase = phase,
                momentType = momentType,
                concepts = concepts,
                fen = ctx.fen,
                strategyPack = Some(pack),
                routeRefs = routeRefs,
                moveRefs = moveRefs
              ),
              120.seconds
            )
          initial.map { note =>
            val reasons = activeRepairReasons(note.commentary, ctx, pack)
            val repaired =
              if reasons.isEmpty then note
              else
                Await.result(
                  openAi.repairActiveStrategicNoteSync(
                    baseNarrative = baseNarrative,
                    rejectedNote = note.commentary,
                    failureReasons = reasons,
                    phase = phase,
                    momentType = momentType,
                    concepts = concepts,
                    fen = ctx.fen,
                    strategyPack = Some(pack),
                    routeRefs = routeRefs,
                    moveRefs = moveRefs
                  ),
                  120.seconds
                ).getOrElse(note)
            ActiveNoteMetrics(
              provider = "openai",
              model = Some(repaired.model),
              promptTokens = repaired.promptTokens,
              cachedTokens = repaired.cachedTokens,
              completionTokens = repaired.completionTokens,
              estimatedCostUsd = repaired.estimatedCostUsd,
              rawNote = note.commentary,
              finalNote = repaired.commentary,
              repairApplied = reasons.nonEmpty
            )
          }
        case Some("gemini") =>
          val initial =
            Await.result(
              gemini.activeStrategicNote(
                baseNarrative = baseNarrative,
                phase = phase,
                momentType = momentType,
                concepts = concepts,
                fen = ctx.fen,
                strategyPack = Some(pack),
                routeRefs = routeRefs,
                moveRefs = moveRefs
              ),
              120.seconds
            )
          initial.map { note =>
            val reasons = activeRepairReasons(note, ctx, pack)
            val repaired =
              if reasons.isEmpty then note
              else
                Await.result(
                  gemini.repairActiveStrategicNote(
                    baseNarrative = baseNarrative,
                    rejectedNote = note,
                    failureReasons = reasons,
                    phase = phase,
                    momentType = momentType,
                    concepts = concepts,
                    fen = ctx.fen,
                    strategyPack = Some(pack),
                    routeRefs = routeRefs,
                    moveRefs = moveRefs
                  ),
                  120.seconds
                ).getOrElse(note)
            ActiveNoteMetrics(
              provider = "gemini",
              model = sys.env.get("GEMINI_MODEL").filter(_.trim.nonEmpty),
              promptTokens = None,
              cachedTokens = None,
              completionTokens = None,
              estimatedCostUsd = None,
              rawNote = note,
              finalNote = repaired,
              repairApplied = reasons.nonEmpty
            )
          }
        case _ => None
    }

  private def activeRepairReasons(note: String, ctx: NarrativeContext, pack: StrategyPack): List[String] =
    val reasons = scala.collection.mutable.ListBuffer.empty[String]
    if !CommentaryOpsSignals.activeThesisAgreement(
        baseNarrative = BookStyleRenderer.renderValidatedOutline(BookStyleRenderer.validatedOutline(ctx), ctx),
        activeNote = note,
        digest = pack.signalDigest
      ).agreed
    then reasons += "dominant_thesis_not_preserved"
    val sentenceCount = note.split("""(?<=[.!?])\s+""").count(_.trim.nonEmpty)
    if sentenceCount < 2 || sentenceCount > 4 then reasons += "sentence_budget"
    reasons.toList

  private def renderReport(
      corpusPath: String,
      results: List[QaCaseResult],
      liveProviderAvailable: Boolean,
      corpusCount: Int,
      structureSupplementCount: Int
  ): String =
    val total = results.size
    val claimRate = ratio(results.count(_.evaluation.claimLikeFirstParagraph), total)
    val paragraphRate = ratio(results.count(_.evaluation.paragraphBudgetOk), total)
    val placeholderOk = results.count(_.evaluation.placeholderHits.isEmpty)
    val genericOk = results.count(_.evaluation.genericHits.isEmpty)
    val directPolish = results.flatMap(_.directPolish)
    val softRepairRate = ratio(directPolish.count(_.softRepairApplied), directPolish.size)
    val materialSoftRepairRate = ratio(directPolish.count(_.softRepairMaterialApplied), directPolish.size)
    val fallbackRate = if liveProviderAvailable then ratio(total - directPolish.size, total) else 0.0
    val avgCostUsd =
      if directPolish.flatMap(_.estimatedCostUsd).nonEmpty then
        Some(directPolish.flatMap(_.estimatedCostUsd).sum / directPolish.flatMap(_.estimatedCostUsd).size.toDouble)
      else None
    val openingEligible = results.count(_.openingEligible)
    val structureEligible = results.count(_.structureEligible)
    val alternativeEligible = results.count(_.alternativeEligible)
    val endgameEligible = results.count(_.endgameEligible)
    val activeEligible = results.count(_.activeNote.nonEmpty)
    val openingRate = ratio(results.count(r => r.openingEligible && r.openingObserved), openingEligible)
    val structureRate = ratio(results.count(r => r.structureEligible && r.structureObserved), structureEligible)
    val alternativeRate = ratio(results.count(r => r.alternativeEligible && r.alternativeObserved), alternativeEligible)
    val endgameRate = ratio(results.count(r => r.endgameEligible && r.endgameObserved), endgameEligible)
    val agreementRate = ratio(results.count(_.thesisAgreementObserved), activeEligible)
    val fullGameThesisRate = ratio(results.count(_.fullGame.thesisPreserved), total)
    val fullGameAlternativeRate = ratio(results.count(r => !r.alternativeEligible || r.fullGame.alternativeObserved), total)
    val fullGameStructureRate = ratio(results.count(r => !r.structureEligible || r.fullGame.structureObserved), total)
    val evidenceVisibleCases = results.count(_.fullGame.evidenceSurfaceVisible)

    def gate(label: String, actual: String, passed: Boolean): String =
      s"| $label | $actual | ${if passed then "PASS" else "FAIL"} |\n"

    val sb = new StringBuilder()
    sb.append("# Core Commentary Closeout QA Report\n\n")
    sb.append(s"- Snapshot date: 2026-03-08\n")
    sb.append(s"- Corpus source: `$corpusPath`\n")
    sb.append(s"- Corpus cases: $corpusCount\n")
    sb.append(s"- Structure supplements: $structureSupplementCount fixture-backed route-aware cases\n")
    sb.append(s"- Sample count: $total\n")
    sb.append(s"- Live provider available: $liveProviderAvailable\n")
    sb.append("- Scope: Bookmaker polished prose, Active strategic note parity, and focused full-game parity.\n\n")

    sb.append("## Completion Gates\n\n")
    sb.append("| Gate | Actual | Status |\n")
    sb.append("| --- | --- | --- |\n")
    sb.append(gate("Bookmaker claim_like_first_paragraph >= 95%", f"${claimRate * 100}%.1f%%", claimRate >= 0.95))
    sb.append(gate("Bookmaker paragraph_budget_2_4 >= 95%", f"${paragraphRate * 100}%.1f%%", paragraphRate >= 0.95))
    sb.append(gate("placeholder_leakage = 0", s"${total - placeholderOk} hits", placeholderOk == total))
    sb.append(gate("generic_flattening_hits <= 5%", s"${total - genericOk}/$total", ratio(total - genericOk, total) <= 0.05))
    sb.append(gate("soft_repair_applied_rate <= 35% (material)", if directPolish.nonEmpty then f"${materialSoftRepairRate * 100}%.1f%%" else "n/a", directPolish.nonEmpty && materialSoftRepairRate <= 0.35))
    sb.append(gate("polish_fallback_rate <= 2%", f"${fallbackRate * 100}%.1f%%", !liveProviderAvailable || fallbackRate <= 0.02))
    sb.append(gate("opening_branch_rate >= 80%", f"${openingRate * 100}%.1f%% ($openingEligible eligible)", openingEligible == 0 || openingRate >= 0.80))
    sb.append(gate("structure_deployment_rate >= 85%", f"${structureRate * 100}%.1f%% ($structureEligible eligible)", structureEligible == 0 || structureRate >= 0.85))
    sb.append(gate("alternative_mention_rate >= 75%", f"${alternativeRate * 100}%.1f%% ($alternativeEligible eligible)", alternativeEligible == 0 || alternativeRate >= 0.75))
    sb.append(gate("endgame_continuity_rate >= 90%", f"${endgameRate * 100}%.1f%% ($endgameEligible eligible)", endgameEligible == 0 || endgameRate >= 0.90))
    sb.append(gate("Bookmaker_Active_thesis_agreement_rate >= 85%", f"${agreementRate * 100}%.1f%% ($activeEligible active)", activeEligible == 0 || agreementRate >= 0.85))
    sb.append("\n")

    sb.append("## Aggregate Metrics\n\n")
    sb.append(f"- claim_like_first_paragraph: $claimRate%.3f\n")
    sb.append(f"- paragraph_budget_2_4: $paragraphRate%.3f\n")
    sb.append(f"- placeholder_leakage: ${(total - placeholderOk).toDouble / total}%.3f\n")
    sb.append(f"- generic_flattening_hits: ${(total - genericOk).toDouble / total}%.3f\n")
    sb.append(s"- soft_repair_applied_rate: ${if directPolish.nonEmpty then f"$softRepairRate%.3f" else "n/a"}\n")
    sb.append(s"- soft_repair_material_rate: ${if directPolish.nonEmpty then f"$materialSoftRepairRate%.3f" else "n/a"}\n")
    sb.append(f"- polish_fallback_rate: $fallbackRate%.3f\n")
    sb.append(f"- opening_branch_rate: $openingRate%.3f\n")
    sb.append(f"- structure_deployment_rate: $structureRate%.3f\n")
    sb.append(f"- alternative_mention_rate: $alternativeRate%.3f\n")
    sb.append(f"- endgame_continuity_rate: $endgameRate%.3f\n")
    sb.append(f"- Bookmaker_Active_thesis_agreement_rate: $agreementRate%.3f\n")
    sb.append(f"- fullgame_thesis_preservation_rate: $fullGameThesisRate%.3f\n")
    sb.append(f"- fullgame_alternative_parity_rate: $fullGameAlternativeRate%.3f\n")
    sb.append(f"- fullgame_structure_parity_rate: $fullGameStructureRate%.3f\n")
    sb.append(s"- fullgame_evidence_surface_visible_cases: $evidenceVisibleCases\n")
    sb.append(s"- avg_estimated_cost_usd: ${avgCostUsd.map(v => f"$v%.6f").getOrElse("n/a")}\n")
    if directPolish.nonEmpty then
      val actionHistogram =
        directPolish.flatMap(_.softRepairActions).groupMapReduce(identity)(_ => 1)(_ + _).toList.sortBy { case (action, count) => (-count, action) }
      val materialActionHistogram =
        directPolish.flatMap(_.softRepairMaterialActions).groupMapReduce(identity)(_ => 1)(_ + _).toList.sortBy { case (action, count) => (-count, action) }
      sb.append("\n## Soft Repair Actions\n\n")
      sb.append(s"- Any repair actions: ${if actionHistogram.isEmpty then "none" else actionHistogram.map((action, count) => s"$action=$count").mkString(", ")}\n")
      sb.append(s"- Material repair actions: ${if materialActionHistogram.isEmpty then "none" else materialActionHistogram.map((action, count) => s"$action=$count").mkString(", ")}\n\n")
    else sb.append("\n")

    sb.append("## Sampled Review\n\n")
    results.take(5).foreach { result =>
      sb.append(s"### ${result.c.id}: ${result.c.title}\n\n")
      sb.append(s"- Dominant thesis: ${result.dominantClaim}\n")
      sb.append(s"- Bookmaker final: `${excerpt(result.finalCommentary)}`\n")
      sb.append(s"- Full-game focused: `${excerpt(result.fullGame.focusedCommentary)}`\n")
      sb.append(s"- Active note: `${result.activeNote.map(n => excerpt(n.finalNote)).getOrElse("n/a")}`\n")
      result.signalDigest.foreach { digest =>
        val deployment =
          digest.deploymentPiece.map { piece =>
            val route =
              if digest.deploymentRoute.nonEmpty then digest.deploymentRoute.mkString("-")
              else "toward target square"
            s"$piece $route"
          }
        sb.append(s"- Strategic Signals / deployment: `${deployment.getOrElse("n/a")}`\n")
      }
      sb.append("\n")
    }

    val failingCases =
      results.filter { r =>
        !r.evaluation.claimLikeFirstParagraph ||
        !r.evaluation.paragraphBudgetOk ||
        r.evaluation.placeholderHits.nonEmpty ||
        r.evaluation.genericHits.nonEmpty ||
        (r.openingEligible && !r.openingObserved) ||
        (r.structureEligible && !r.structureObserved) ||
        (r.alternativeEligible && !r.alternativeObserved) ||
        (r.endgameEligible && !r.endgameObserved) ||
        (r.activeNote.nonEmpty && !r.thesisAgreementObserved)
      }

    sb.append("## Cases Needing Follow-up\n\n")
    if failingCases.isEmpty then
      sb.append("- None.\n")
    else
      failingCases.take(12).foreach { result =>
        val reasons = List(
          Option.when(!result.evaluation.claimLikeFirstParagraph)("claim"),
          Option.when(!result.evaluation.paragraphBudgetOk)("paragraph_budget"),
          Option.when(result.evaluation.placeholderHits.nonEmpty)("placeholder"),
          Option.when(result.evaluation.genericHits.nonEmpty)("generic"),
          Option.when(result.openingEligible && !result.openingObserved)("opening_branch"),
          Option.when(result.structureEligible && !result.structureObserved)("structure_deployment"),
          Option.when(result.alternativeEligible && !result.alternativeObserved)("alternative"),
          Option.when(result.endgameEligible && !result.endgameObserved)("endgame"),
          Option.when(result.activeNote.nonEmpty && !result.thesisAgreementObserved)("active_agreement")
        ).flatten.mkString(", ")
        sb.append(s"- `${result.c.id}`: $reasons\n")
      }
    sb.toString

  private def fetchOpeningRef(
      openingExplorer: OpeningExplorerClient,
      fen: String,
      phase: String
  ): Option[OpeningReference] =
    if !phase.equalsIgnoreCase("opening") then None
    else
      try Await.result(openingExplorer.fetchMasters(fen), 4.seconds)
      catch case NonFatal(_) => None

  private def normalizePhase(phase: String): String =
    Option(phase).map(_.trim.toLowerCase).filter(_.nonEmpty).getOrElse("middlegame")

  private def buildRouteRefs(pack: StrategyPack): List[ActiveStrategicRouteRef] =
    val routeRegex = "^[a-h][1-8]$".r
    pack.pieceRoutes.zipWithIndex.flatMap { case (route, idx) =>
      val squares = route.route.map(_.trim.toLowerCase).filter(routeRegex.matches).distinct
      Option.when(squares.size >= 2)(
        ActiveStrategicRouteRef(
          routeId = s"route_${idx + 1}",
          piece = route.piece,
          route = squares,
          purpose = route.purpose,
          confidence = route.confidence
        )
      )
    }.take(3)

  private def buildMoveRefs(fen: String, variations: List[VariationLine]): List[ActiveStrategicMoveRef] =
    variations.headOption.flatMap(_.moves.headOption).map(_.trim.toLowerCase)
      .filter(_.matches("^[a-h][1-8][a-h][1-8][qrbn]?$"))
      .toList
      .map { uci =>
        ActiveStrategicMoveRef(
          label = "Principal line",
          source = "top_variation",
          uci = uci,
          san = NarrativeUtils.uciToSan(fen, uci),
          fenAfter = Some(NarrativeUtils.uciListToFen(fen, List(uci)))
        )
      }

  private def mentionsDominantThesis(
      text: String,
      dominantClaim: String,
      structureArc: Option[StructurePlanArc],
      digest: Option[NarrativeSignalDigest],
      precedent: Option[OpeningBranchPrecedent]
  ): Boolean =
    val low = normalize(text)
    val claimTokens = significantTokens(dominantClaim)
    val claimHit =
      claimTokens.nonEmpty &&
        claimTokens.count(low.contains) >= math.min(3, claimTokens.size.max(1))
    val structureHit =
      structureArc.exists { arc =>
        val structureTokens = significantTokens(arc.structureLabel)
        val planTokens = significantTokens(arc.planLabel)
        val routeTokens = arc.primaryDeployment.route.map(_.toLowerCase)
        structureTokens.exists(low.contains) &&
        (planTokens.exists(low.contains) || routeTokens.exists(low.contains))
      }
    val openingHit =
      precedent.exists { p =>
        significantTokens(p.branchLabel).exists(low.contains) &&
          playerSurnames(p.gameDescriptor).exists(low.contains)
      }
    val digestHit =
      digest.exists { d =>
        d.deploymentPurpose.exists(p => significantTokens(p).exists(low.contains)) ||
        d.opening.exists(o => significantTokens(o).exists(low.contains))
      }
    claimHit || structureHit || openingHit || digestHit

  private def mentionsOpeningBranch(
      text: String,
      openingName: Option[String],
      precedent: Option[OpeningBranchPrecedent]
  ): Boolean =
    val low = normalize(text)
    val openingHit = openingName.exists(name => significantTokens(name).exists(low.contains))
    val precedentHit =
      precedent.exists { p =>
        val playerHit = playerSurnames(p.gameDescriptor).exists(low.contains)
        val branchHit = significantTokens(p.branchLabel).count(low.contains) >= 1 || low.contains("branch")
        val routeHit =
          normalize(p.routePreview)
            .split("\\s+")
            .toList
            .filter(token => token.nonEmpty && token.length >= 2)
            .exists(low.contains)
        (playerHit && (branchHit || routeHit)) || (openingHit && (branchHit || routeHit))
      }
    precedentHit || (openingHit && low.contains("branch"))

  private def mentionsStructureDeployment(
      text: String,
      structureArc: Option[StructurePlanArc],
      digest: Option[NarrativeSignalDigest]
  ): Boolean =
    val low = normalize(text)
    val arcHit =
      structureArc.exists { arc =>
        val structureHit = significantTokens(arc.structureLabel).exists(low.contains)
        val planHit = significantTokens(arc.planLabel).exists(low.contains)
        val routeHit =
          arc.primaryDeployment.route.nonEmpty &&
            arc.primaryDeployment.route.map(_.toLowerCase).exists(low.contains)
        val destinationHit = normalize(arc.primaryDeployment.destination).nonEmpty && low.contains(normalize(arc.primaryDeployment.destination))
        val contributionHit = significantTokens(arc.moveContribution).exists(low.contains)
        structureHit && planHit && (routeHit || destinationHit) && contributionHit
      }
    val digestHit =
      digest.exists { d =>
        d.deploymentPiece.exists(piece => low.contains(piece.toLowerCase)) &&
          d.deploymentPurpose.exists(p => significantTokens(p).exists(low.contains)) &&
          d.deploymentContribution.exists(c => significantTokens(c).exists(low.contains))
      }
    arcHit || digestHit

  private def mentionsEndgame(text: String, info: EndgameInfo, virtualMotifs: List[String] = Nil): Boolean =
    val low = normalize(text)
    val tokens =
      List(
        Option.when(info.oppositionType != "None")(info.oppositionType),
        Option.when(info.ruleOfSquare != "NA")(info.ruleOfSquare),
        Option.when(info.rookEndgamePattern != "None")(info.rookEndgamePattern),
        info.primaryPattern,
        info.transition,
        Option.when(info.isZugzwang)("zugzwang"),
        Option.when(info.hasOpposition)("opposition"),
        Option.when(info.triangulationAvailable)("triangulation"),
        Option.when(info.theoreticalOutcomeHint.trim.nonEmpty && !info.theoreticalOutcomeHint.equalsIgnoreCase("Unclear"))(info.theoreticalOutcomeHint)
      ).flatten.flatMap(significantTokens)
    val motifTokens =
      virtualMotifs.flatMap {
        case motif if motif.equalsIgnoreCase("passed_pawn") =>
          List("passed pawn", "outside passer", "passer", "queening race")
        case motif if motif.equalsIgnoreCase("rook_endgame") =>
          List("rook endgame", "rook ending")
        case motif if motif.equalsIgnoreCase("opposition") =>
          List("opposition")
        case motif if motif.equalsIgnoreCase("zugzwang") =>
          List("zugzwang")
        case motif =>
          significantTokens(motif.replace('_', ' '))
      }.distinct
    val observedTokens = (tokens ++ motifTokens).distinct
    observedTokens.nonEmpty && observedTokens.exists(token => low.contains(normalize(token)))

  private def endgameEligibleInfo(info: EndgameInfo): Boolean =
    info.primaryPattern.nonEmpty ||
      info.rookEndgamePattern != "None" ||
      info.oppositionType != "None" ||
      info.ruleOfSquare != "NA" ||
      info.transition.nonEmpty

  private def ratio(numerator: Int, denominator: Int): Double =
    if denominator <= 0 then 0.0 else numerator.toDouble / denominator.toDouble

  private def unwrapCommentaryPayload(text: String): String =
    CommentaryPayloadNormalizer.normalize(text)

  private def syntheticData(ctx: NarrativeContext): ExtendedAnalysisData =
    ExtendedAnalysisData(
      fen = ctx.fen,
      nature = PositionNature(NatureType.Dynamic, 0.5, 0.5, "closeout synthetic"),
      motifs = Nil,
      plans = Nil,
      preventedPlans = Nil,
      pieceActivity = Nil,
      structuralWeaknesses = Nil,
      positionalFeatures = Nil,
      compensation = None,
      endgameFeatures = None,
      practicalAssessment = None,
      alternatives = Nil,
      candidates = Nil,
      counterfactual = None,
      conceptSummary = ctx.semantic.map(_.conceptSummary).getOrElse(Nil),
      prevMove = ctx.playedMove,
      ply = ctx.ply,
      evalCp = 0,
      isWhiteToMove = ctx.ply % 2 == 0
    )

  private val structureFixtureCases: List[BookmakerProseGoldenFixtures.Fixture] =
    BookmakerProseGoldenFixtures.all.filter { fixture =>
      fixture.expectedLens == StrategicLens.Structure &&
      StructurePlanArcBuilder.build(fixture.ctx).exists(StructurePlanArcBuilder.proseEligible)
    }

  private def normalize(raw: String): String =
    Option(raw).getOrElse("").trim.toLowerCase.replaceAll("""[^a-z0-9\s\-]""", " ").replaceAll("\\s+", " ")

  private def significantTokens(raw: String): List[String] =
    normalize(raw)
      .split("\\s+")
      .toList
      .filter(token => token.nonEmpty && token.length > 2 && !StopWords.contains(token))
      .distinct

  private def playerSurnames(gameDescriptor: String): List[String] =
    normalize(gameDescriptor)
      .split("""[^a-z0-9]+""")
      .toList
      .filter(token => token.nonEmpty && token.length > 3 && !token.forall(_.isDigit))
      .take(4)

  private def excerpt(text: String, maxChars: Int = 180): String =
    val cleaned = Option(text).getOrElse("").replaceAll("\\s+", " ").trim
    if cleaned.length <= maxChars then cleaned else cleaned.take(maxChars - 1) + "…"
