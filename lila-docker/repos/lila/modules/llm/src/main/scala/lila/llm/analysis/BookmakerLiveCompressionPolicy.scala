package lila.llm.analysis

import lila.llm.{ BookmakerRefsV1, StrategyPack }
import lila.llm.model.*
import lila.llm.model.authoring.{ AuthorQuestionKind, NarrativeOutline }
import scala.annotation.unused

private[llm] object BookmakerLiveCompressionPolicy:

  private val movePurposeMarkers = List(
    "keep",
    "keeps",
    "prepare",
    "prepares",
    "improve",
    "improves",
    "activate",
    "activates",
    "support",
    "supports",
    "restrain",
    "restrains",
    "castle",
    "castles",
    "conversion",
    "counterplay"
  )

  def systemLanguageBanList: List[String] = LiveNarrativeCompressionCore.systemLanguageBanList

  def systemLanguageHits(raw: String): List[String] =
    LiveNarrativeCompressionCore.systemLanguageHits(raw)

  def buildSlotsOrFallback(
      ctx: NarrativeContext,
      outline: NarrativeOutline,
      refs: Option[BookmakerRefsV1],
      strategyPack: Option[StrategyPack],
      truthContract: Option[DecisiveTruthContract] = None
  ): BookmakerPolishSlots =
    buildSlots(ctx, outline, refs, strategyPack, truthContract)
      .orElse(exactFactualFallbackSlots(ctx))
      .getOrElse(omittedSlots)

  private[analysis] def cleanNarrativeSentence(raw: String, ctx: NarrativeContext): Option[String] =
    cleanSentence(raw, ctx)

  private[llm] def candidateEvidenceLines(
      refs: Option[BookmakerRefsV1],
      ctx: NarrativeContext
  ): List[String] =
    variationGuardrail(refs)
      .flatMap(cleanSentence(_, ctx))
      .toList

  def buildSlots(
      ctx: NarrativeContext,
      @unused outline: NarrativeOutline,
      refs: Option[BookmakerRefsV1],
      strategyPack: Option[StrategyPack],
      truthContract: Option[DecisiveTruthContract] = None
  ): Option[BookmakerPolishSlots] =
    val candidateEvidence = candidateEvidenceLines(refs, ctx)
    val plannerInputs =
      QuestionPlannerInputsBuilder.build(ctx, strategyPack, truthContract, candidateEvidence)
    val rankedPlans = QuestionFirstCommentaryPlanner.plan(ctx, plannerInputs, truthContract)
    slotsFromPlanner(ctx, plannerInputs, rankedPlans)

  private case class PlannerSlotDraft(
      questionKind: AuthorQuestionKind,
      lens: StrategicLens,
      claim: String,
      supportPrimary: Option[String],
      supportSecondary: Option[String],
      tension: Option[String],
      evidenceHook: Option[String],
      coda: Option[String]
  )

  private def slotsFromPlanner(
      ctx: NarrativeContext,
      inputs: QuestionPlannerInputs,
      rankedPlans: RankedQuestionPlans
  ): Option[BookmakerPolishSlots] =
    rankedPlans.primary
      .flatMap(primary => plannerDraft(ctx, primary, rankedPlans.secondary, inputs))
      .flatMap(draft => finalizePlannerSlots(ctx, draft))

  private def plannerDraft(
      ctx: NarrativeContext,
      primary: QuestionPlan,
      secondary: Option[QuestionPlan],
      inputs: QuestionPlannerInputs
  ): Option[PlannerSlotDraft] =
    val secondarySupport = secondarySupportText(primary, secondary, ctx)
    val primaryEvidence = plannerEvidenceHook(primary.evidence, ctx)
    val primaryConsequence = plannerConsequence(primary.consequence, ctx)
    val timingTension = plannerTimingTension(primary.questionKind, inputs, ctx)

    primary.questionKind match
      case AuthorQuestionKind.WhyThis =>
        Some(
          PlannerSlotDraft(
            questionKind = AuthorQuestionKind.WhyThis,
            lens = plannerLens(primary, inputs),
            claim = primary.claim,
            supportPrimary = primary.contrast,
            supportSecondary = secondarySupport,
            tension = None,
            evidenceHook = primaryEvidence,
            coda = primaryConsequence
          )
        )
      case AuthorQuestionKind.WhyNow =>
        Some(
          PlannerSlotDraft(
            questionKind = AuthorQuestionKind.WhyNow,
            lens = StrategicLens.Decision,
            claim = primary.claim,
            supportPrimary = primary.contrast,
            supportSecondary = secondarySupport.filterNot(text => primary.contrast.exists(sameSentence(_, text))),
            tension = timingTension,
            evidenceHook = primaryEvidence,
            coda = primaryConsequence
          )
        )
      case AuthorQuestionKind.WhatChanged =>
        Some(
          PlannerSlotDraft(
            questionKind = AuthorQuestionKind.WhatChanged,
            lens = plannerLens(primary, inputs),
            claim = primary.claim,
            supportPrimary = primary.contrast,
            supportSecondary = secondarySupport,
            tension = None,
            evidenceHook = primaryEvidence,
            coda = primaryConsequence
          )
        )
      case AuthorQuestionKind.WhatMustBeStopped =>
        Some(
          PlannerSlotDraft(
            questionKind = AuthorQuestionKind.WhatMustBeStopped,
            lens =
              if primary.sourceKinds.exists(kind => kind.contains("threat")) &&
                inputs.truthMode == PlayerFacingTruthMode.Tactical
              then StrategicLens.Decision
              else StrategicLens.Prophylaxis,
            claim = primary.claim,
            supportPrimary = primary.contrast,
            supportSecondary = secondarySupport,
            tension = timingTension,
            evidenceHook = primaryEvidence,
            coda = primaryConsequence
          )
        )
      case AuthorQuestionKind.WhosePlanIsFaster =>
        Some(
          PlannerSlotDraft(
            questionKind = AuthorQuestionKind.WhosePlanIsFaster,
            lens = StrategicLens.Decision,
            claim = primary.claim,
            supportPrimary = inputs.decisionFrame.battlefront.map(_.sentence).orElse(primary.contrast),
            supportSecondary = secondarySupport,
            tension = timingTension,
            evidenceHook = primaryEvidence,
            coda = primaryConsequence
          )
        )

  private def finalizePlannerSlots(
      ctx: NarrativeContext,
      draft: PlannerSlotDraft
  ): Option[BookmakerPolishSlots] =
    val cleanedClaim = sanitizePlannerClaim(draft, ctx)
    cleanedClaim.flatMap { claim =>
      val supportPrimary = sanitizeDistinctText(draft.supportPrimary, ctx, claim)
      val supportSecondary =
        sanitizeDistinctText(draft.supportSecondary, ctx, claim)
          .filter(text => !supportPrimary.exists(sameSentence(_, text)))
      val tension =
        sanitizeDistinctText(draft.tension, ctx, claim)
          .filter(text =>
            !supportPrimary.exists(sameSentence(_, text)) &&
              !supportSecondary.exists(sameSentence(_, text))
          )
      val evidenceHook =
        sanitizeDistinctEvidence(draft.evidenceHook, ctx, claim)
          .filter(text =>
            !supportPrimary.exists(sameSentence(_, text)) &&
              !supportSecondary.exists(sameSentence(_, text)) &&
              !tension.exists(sameSentence(_, text))
          )
      val coda =
        sanitizeDistinctText(draft.coda, ctx, claim)
          .filter(text =>
            !supportPrimary.exists(sameSentence(_, text)) &&
              !supportSecondary.exists(sameSentence(_, text)) &&
              !tension.exists(sameSentence(_, text)) &&
              !evidenceHook.exists(sameSentence(_, text))
          )

      Option.when(hasPlannerSupport(supportPrimary, supportSecondary, tension, evidenceHook, coda)) {
        val supportLines = List(supportPrimary, supportSecondary).flatten
        val slots =
          BookmakerPolishSlots(
            lens = draft.lens,
            claim = prefixMoveHeader(ctx, claim),
            supportPrimary = supportPrimary,
            supportSecondary = supportSecondary,
            tension = tension,
            evidenceHook = evidenceHook,
            coda = coda,
            factGuardrails = (supportLines ++ tension.toList ++ evidenceHook.toList ++ coda.toList).distinct,
            paragraphPlan = plannerParagraphPlan(supportLines, tension, evidenceHook, coda)
          )
        Option.when(bookmakerContractSafe(slots))(slots)
      }.flatten
    }

  private def sanitizePlannerClaim(
      draft: PlannerSlotDraft,
      ctx: NarrativeContext
  ): Option[String] =
    cleanSentence(draft.claim, ctx)
      .orElse {
        Option.when(draft.questionKind == AuthorQuestionKind.WhosePlanIsFaster) {
          relaxedCertifiedRaceSentence(draft.claim, ctx)
        }.flatten
      }

  private def plannerLens(
      primary: QuestionPlan,
      inputs: QuestionPlannerInputs
  ): StrategicLens =
    inputs.mainBundle.flatMap(_.mainClaim).map(_.lens)
      .orElse(inputs.quietIntent.map(_.lens))
      .getOrElse {
        primary.questionKind match
          case AuthorQuestionKind.WhatMustBeStopped => StrategicLens.Prophylaxis
          case AuthorQuestionKind.WhosePlanIsFaster => StrategicLens.Decision
          case AuthorQuestionKind.WhyNow            => StrategicLens.Decision
          case _                                    => StrategicLens.Decision
      }

  private def plannerEvidenceHook(
      evidence: Option[QuestionPlanEvidence],
      ctx: NarrativeContext
  ): Option[String] =
    evidence
      .filter(e =>
        e.branchScoped ||
          e.text.linesIterator.exists(line => LineScopedCitation.hasConcreteSanLine(line)) ||
          e.text.linesIterator.exists(line => line.trim.matches("""^[a-z]\)\s+.*"""))
      )
      .flatMap(_.text.linesIterator.map(_.trim).find(_.nonEmpty))
      .flatMap(cleanSentence(_, ctx))

  private def plannerConsequence(
      consequence: Option[QuestionPlanConsequence],
      ctx: NarrativeContext
  ): Option[String] =
    consequence
      .filter(_.certified)
      .map(_.text)
      .flatMap(cleanSentence(_, ctx))

  private def plannerTimingTension(
      kind: AuthorQuestionKind,
      inputs: QuestionPlannerInputs,
      ctx: NarrativeContext
  ): Option[String] =
    Option.when(
      kind == AuthorQuestionKind.WhyNow || kind == AuthorQuestionKind.WhosePlanIsFaster || kind == AuthorQuestionKind.WhatMustBeStopped
    ) {
      inputs.decisionFrame.urgency
        .map(_.sentence)
        .orElse(
          inputs.practicalAssessment.flatMap(_.biasFactors.headOption.flatMap { bias =>
            LiveNarrativeCompressionCore
              .renderPracticalBiasPlayer(bias.factor, bias.description)
              .map(text => s"Practically, $text.")
          })
        )
    }.flatten
      .flatMap(cleanSentence(_, ctx))
      .filter(isConcreteTimingTension)

  private def secondarySupportText(
      primary: QuestionPlan,
      secondary: Option[QuestionPlan],
      ctx: NarrativeContext
  ): Option[String] =
    secondary
      .flatMap(plan =>
        plan.contrast
          .orElse(plan.consequence.filter(_.certified).map(_.text))
      )
      .flatMap(cleanSentence(_, ctx))
      .filter(text => !sameSentence(primary.claim, text))

  private def sanitizeDistinctText(
      textOpt: Option[String],
      ctx: NarrativeContext,
      claim: String
  ): Option[String] =
    textOpt
      .flatMap(cleanSentence(_, ctx))
      .filter(text => !sameSentence(text, claim))

  private def sanitizeDistinctEvidence(
      textOpt: Option[String],
      ctx: NarrativeContext,
      claim: String
  ): Option[String] =
    textOpt
      .flatMap { text =>
        Option.when(
          text.matches("""^[a-z]\)\s+.*""") || LineScopedCitation.hasConcreteSanLine(text)
        )(text)
      }
      .flatMap(cleanSentence(_, ctx))
      .filter(text => !sameSentence(text, claim))

  private def hasPlannerSupport(
      supportPrimary: Option[String],
      supportSecondary: Option[String],
      tension: Option[String],
      evidenceHook: Option[String],
      coda: Option[String]
  ): Boolean =
    List(supportPrimary, supportSecondary, tension, evidenceHook, coda).flatten.exists(_.trim.nonEmpty)

  private def bookmakerContractSafe(slots: BookmakerPolishSlots): Boolean =
    val prose = LiveNarrativeCompressionCore.deterministicProse(slots)
    val evaluation = BookmakerProseContract.evaluate(prose, slots)
    prose.trim.nonEmpty &&
    evaluation.claimLikeFirstParagraph &&
    evaluation.paragraphBudgetOk &&
    evaluation.placeholderHits.isEmpty

  private def isConcreteTimingTension(text: String): Boolean =
    val low = Option(text).getOrElse("").toLowerCase
    val timingMarker =
      low.contains("now") || low.contains("immediate") || low.contains("threat") || low.contains("break") || low.contains("counterplay") || low.contains("window")
    val concreteAnchor =
      LiveNarrativeCompressionCore.hasConcreteAnchor(text) ||
        low.contains("threat") ||
        low.contains("break") ||
        low.contains("counterplay")
    timingMarker && concreteAnchor &&
      !low.matches("""the timing matters now\.?""")

  private[analysis] def relaxedCertifiedRaceSentence(raw: String, ctx: NarrativeContext): Option[String] =
    normalized(raw)
      .map(BookmakerSlotSanitizer.sanitizeUserText)
      .map(LiveNarrativeCompressionCore.rewritePlayerLanguage)
      .flatMap(normalized)
      .map(LiveNarrativeCompressionCore.trimLeadScaffold)
      .flatMap(normalized)
      .filter(text => systemLanguageHits(text).isEmpty)
      .filter(text => LiveNarrativeCompressionCore.playerLanguageHits(text).isEmpty)
      .filter(text => !containsNonBackedPlanName(text, ctx))
      .filter(text => namedPlanAllowed(text, ctx))
      .filterNot(LiveNarrativeCompressionCore.isLowValueNarrativeSentence)

  private def plannerParagraphPlan(
      supportLines: List[String],
      tension: Option[String],
      evidenceHook: Option[String],
      coda: Option[String]
  ): List[String] =
    if supportLines.isEmpty && tension.isEmpty && evidenceHook.isEmpty && coda.isEmpty then List("p1=claim")
    else
      val p2 =
        if supportLines.nonEmpty then Some("p2=support_chain")
        else if evidenceHook.nonEmpty then Some("p2=cited_line")
        else Some("p2=practical_nuance")
      val p3 =
        if supportLines.nonEmpty && (tension.nonEmpty || evidenceHook.nonEmpty || coda.nonEmpty) then Some("p3=tension_or_evidence")
        else None
      List(Some("p1=claim"), p2, p3).flatten

  private def exactFactualFallbackSlots(
      ctx: NarrativeContext
  ): Option[BookmakerPolishSlots] =
    QuietMoveIntentBuilder.exactFactualSentence(ctx)
      .flatMap(cleanSentence(_, ctx))
      .map { factual =>
        BookmakerPolishSlots(
          lens = StrategicLens.Decision,
          claim = prefixMoveHeader(ctx, factual),
          supportPrimary = None,
          supportSecondary = None,
          tension = None,
          evidenceHook = None,
          coda = None,
          factGuardrails = Nil,
          paragraphPlan = List("p1=claim")
        )
      }

  private def omittedSlots: BookmakerPolishSlots =
    BookmakerPolishSlots(
      lens = StrategicLens.Decision,
      claim = "",
      supportPrimary = None,
      supportSecondary = None,
      tension = None,
      evidenceHook = None,
      coda = None,
      factGuardrails = Nil,
      paragraphPlan = List("p1=claim")
    )

  private def cleanSentence(raw: String, ctx: NarrativeContext): Option[String] =
    normalized(raw)
      .map(BookmakerSlotSanitizer.sanitizeUserText)
      .map(LiveNarrativeCompressionCore.rewritePlayerLanguage)
      .flatMap(normalized)
      .map(LiveNarrativeCompressionCore.trimLeadScaffold)
      .flatMap(normalized)
      .filter(text => systemLanguageHits(text).isEmpty)
      .filter(text => LiveNarrativeCompressionCore.playerLanguageHits(text).isEmpty)
      .filter(LiveNarrativeCompressionCore.keepPlayerFacingSentence)
      .filterNot(LiveNarrativeCompressionCore.isLowValueNarrativeSentence)
      .filter(text => !containsNonBackedPlanName(text, ctx))
      .filter(text => namedPlanAllowed(text, ctx))
      .filterNot(_.equalsIgnoreCase("Concrete support is still limited."))

  private def containsNonBackedPlanName(text: String, ctx: NarrativeContext): Boolean =
    val low = text.toLowerCase
    val backed = StrategicNarrativePlanSupport.evidenceBackedPlanNames(ctx).flatMap(normalized).toSet
    val otherPlanNames =
      (
        ctx.mainStrategicPlans.map(_.planName) ++
          ctx.plans.top5.map(_.name)
      ).flatMap(normalized).distinct
    otherPlanNames.exists(name =>
      !backed.contains(name) &&
        name.split("\\s+").length >= 2 &&
        low.contains(name)
    )

  private def namedPlanAllowed(text: String, ctx: NarrativeContext): Boolean =
    val low = text.toLowerCase
    val backed = StrategicNarrativePlanSupport.evidenceBackedPlanNames(ctx).flatMap(normalized).distinct
    val containsBackedPlan = backed.exists(low.contains)
    !containsNonBackedPlanName(text, ctx) &&
      (!containsBackedPlan || movePurposeMarkers.exists(low.contains))

  private def prefixMoveHeader(ctx: NarrativeContext, claim: String): String =
    if Option(claim).exists(_.matches("""^\d+\.(?:\.\.)?\s+[^:]+:\s*.*""")) then claim
    else
      val moveHeader =
        for
          san <- ctx.playedSan.filter(_.trim.nonEmpty)
        yield
          val moveNum = (ctx.ply + 1) / 2
          val prefix = if ctx.ply % 2 == 1 then s"$moveNum." else s"$moveNum..."
          s"$prefix $san:"
      moveHeader.map(h => s"$h $claim").getOrElse(claim)

  private def variationGuardrail(refs: Option[BookmakerRefsV1]): Option[String] =
    refs.flatMap(_.variations.headOption).flatMap { variation =>
      val preview =
        variation.moves
          .take(3)
          .map(_.san.trim)
          .filter(_.nonEmpty)
          .mkString(" ")
          .trim
      Option.when(preview.nonEmpty) {
        val eval = formatVariationScore(variation.scoreCp, variation.mate)
        s"Line: a) $preview$eval."
      }
    }

  private def formatVariationScore(scoreCp: Int, mate: Option[Int]): String =
    mate match
      case Some(m) if m > 0 => s" (mate in $m)"
      case Some(m) if m < 0 => s" (mated in ${Math.abs(m)})"
      case Some(_)          => ""
      case None =>
        val sign = if scoreCp >= 0 then "+" else ""
        f" ($sign${scoreCp.toDouble / 100}%.1f)"

  private def normalized(raw: String): Option[String] =
    Option(raw)
      .map(_.trim)
      .filter(_.nonEmpty)
      .map(_.replaceAll("""\s+""", " ").trim)

  private def sameSentence(left: String, right: String): Boolean =
    val normalizedLeft = normalized(left).map(_.toLowerCase).getOrElse("")
    val normalizedRight = normalized(right).map(_.toLowerCase).getOrElse("")
    normalizedLeft.nonEmpty && normalizedLeft == normalizedRight
