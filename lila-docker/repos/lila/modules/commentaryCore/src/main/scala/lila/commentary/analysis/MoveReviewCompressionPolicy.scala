package lila.commentary.analysis

import lila.commentary.{ MoveReviewExplanation, MoveReviewRefs, StrategyPack }
import lila.commentary.analysis.practical.ContrastiveSupportAdmissibility
import lila.commentary.analysis.render.QuietStrategicSupportComposer
import lila.commentary.model.*
import lila.commentary.model.authoring.{ AuthorQuestionKind, NarrativeOutline }
import scala.annotation.unused

private[commentary] object MoveReviewCompressionPolicy:

  private[commentary] final case class PlannerRenderSelection(
      primary: QuestionPlan,
      secondary: Option[QuestionPlan],
      contrastTrace: ContrastiveSupportAdmissibility.ContrastSupportTrace
  )

  private[commentary] final case class ExactFactualQuietSupportTrace(
      factualSentence: Option[String],
      composerTrace: QuietStrategicSupportComposer.QuietStrategicSupportTrace,
      liftApplied: Boolean,
      rejectReasons: List[String]
  )

  private final case class PlannerRuntime(
      inputs: QuestionPlannerInputs,
      rankedPlans: RankedQuestionPlans
  )

  private final case class ExactFactualFallbackResult(
      finalSlots: MoveReviewPolishSlots,
      trace: ExactFactualQuietSupportTrace
  )

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
      @unused outline: NarrativeOutline,
      refs: Option[MoveReviewRefs],
      strategyPack: Option[StrategyPack],
      truthContract: Option[DecisiveTruthContract] = None
  ): MoveReviewPolishSlots =
    val plannerRuntime =
      plannerInputsRuntime(ctx, refs, strategyPack, truthContract)
    slotsFromPlanner(ctx, plannerRuntime.inputs, plannerRuntime.rankedPlans, truthContract)
      .orElse(basicMoveExplanationSlots(ctx, refs, truthContract, strategyPack))
      .orElse(exactFactualFallbackSlots(ctx, plannerRuntime, refs, strategyPack))
      .getOrElse(omittedSlots)

  private[commentary] def buildSlotsOrFallbackFromPlannerRuntime(
      ctx: NarrativeContext,
      inputs: QuestionPlannerInputs,
      rankedPlans: RankedQuestionPlans,
      strategyPack: Option[StrategyPack],
      truthContract: Option[DecisiveTruthContract] = None
  ): MoveReviewPolishSlots =
    val plannerRuntime =
      PlannerRuntime(
        inputs = inputs,
        rankedPlans = rankedPlans
      )
    slotsFromPlanner(ctx, inputs, rankedPlans, truthContract)
      .orElse(exactFactualFallbackSlots(ctx, plannerRuntime, refs = None, strategyPack))
      .getOrElse(omittedSlots)

  private[analysis] def cleanNarrativeSentence(raw: String, ctx: NarrativeContext): Option[String] =
    cleanSentence(raw, ctx)

  private[commentary] def candidateEvidenceLines(
      refs: Option[MoveReviewRefs],
      ctx: NarrativeContext
  ): List[String] =
    variationGuardrail(refs)
      .flatMap(cleanSentence(_, ctx))
      .toList

  def buildSlots(
      ctx: NarrativeContext,
      @unused outline: NarrativeOutline,
      refs: Option[MoveReviewRefs],
      strategyPack: Option[StrategyPack],
      truthContract: Option[DecisiveTruthContract] = None
  ): Option[MoveReviewPolishSlots] =
    val plannerRuntime =
      plannerInputsRuntime(ctx, refs, strategyPack, truthContract)
    slotsFromPlanner(ctx, plannerRuntime.inputs, plannerRuntime.rankedPlans, truthContract)

  private[commentary] def exactFactualQuietSupportTrace(
      ctx: NarrativeContext,
      refs: Option[MoveReviewRefs],
      strategyPack: Option[StrategyPack],
      truthContract: Option[DecisiveTruthContract] = None
  ): ExactFactualQuietSupportTrace =
    val plannerRuntime =
      plannerInputsRuntime(ctx, refs, strategyPack, truthContract)
    val composerTrace =
      QuietStrategicSupportComposer.diagnose(
        ctx,
        plannerRuntime.inputs,
        plannerRuntime.rankedPlans,
        strategyPack
      )
    exactFactualFallbackResult(ctx, plannerRuntime, refs, strategyPack)
      .map(_.trace)
      .getOrElse(
        ExactFactualQuietSupportTrace(
          factualSentence = None,
          composerTrace = composerTrace,
          liftApplied = false,
          rejectReasons = List("exact_factual_sentence_missing")
        )
      )

  private def plannerInputsRuntime(
      ctx: NarrativeContext,
      refs: Option[MoveReviewRefs],
      strategyPack: Option[StrategyPack],
      truthContract: Option[DecisiveTruthContract]
  ): PlannerRuntime =
    val candidateEvidence = candidateEvidenceLines(refs, ctx)
    val plannerInputs =
      QuestionPlannerInputsBuilder.build(ctx, strategyPack, truthContract, candidateEvidence)
    val rankedPlans = QuestionFirstCommentaryPlanner.plan(ctx, plannerInputs, truthContract)
    PlannerRuntime(
      inputs = plannerInputs,
      rankedPlans = rankedPlans
    )

  private case class PlannerSlotDraft(
      questionKind: AuthorQuestionKind,
      lens: StrategicLens,
      claim: String,
      supportPrimary: Option[String],
      supportSecondary: Option[String],
      tension: Option[String],
      evidenceHook: Option[String],
      coda: Option[String],
      claimOnlyAllowed: Boolean
  )

  private def slotsFromPlanner(
      ctx: NarrativeContext,
      inputs: QuestionPlannerInputs,
      rankedPlans: RankedQuestionPlans,
      truthContract: Option[DecisiveTruthContract]
  ): Option[MoveReviewPolishSlots] =
    renderSelection(inputs, rankedPlans, truthContract)
      .flatMap(selection =>
        plannerDraft(
          ctx,
          selection.primary,
          selection.secondary,
          inputs,
          selection.contrastTrace
        )
      )
      .flatMap(draft => finalizePlannerSlots(ctx, draft))

  private[commentary] def renderSelection(
      inputs: QuestionPlannerInputs,
      rankedPlans: RankedQuestionPlans,
      truthContract: Option[DecisiveTruthContract]
  ): Option[PlannerRenderSelection] =
    rankedPlans.primary.flatMap { primary =>
      val primaryTrace =
        ContrastiveSupportAdmissibility.decide(primary, inputs, truthContract)
      val eligibleSecondary =
        rankedPlans.secondary
          .filterNot(replayClosedNamedRouteNetwork)
          .filter(shouldPreferWhyNowSecondary(primary, _, primaryTrace))
      if replayClosedNamedRouteNetwork(primary) then
        eligibleSecondary.map { secondary =>
          PlannerRenderSelection(
            primary = secondary,
            secondary = None,
            contrastTrace = ContrastiveSupportAdmissibility.decide(secondary, inputs, truthContract)
          )
        }
      else
        Some(
          eligibleSecondary
            .map { secondary =>
              PlannerRenderSelection(
                primary = secondary,
                secondary = Some(primary),
                contrastTrace = ContrastiveSupportAdmissibility.decide(secondary, inputs, truthContract)
              )
            }
            .getOrElse(
              PlannerRenderSelection(
                primary = primary,
                secondary = rankedPlans.secondary.filterNot(replayClosedNamedRouteNetwork),
                contrastTrace = primaryTrace
              )
            )
        )
    }

  private def replayClosedNamedRouteNetwork(
      plan: QuestionPlan
  ): Boolean =
    plan.plannerSource == RouteNetworkBindProof.ProofSource ||
      plan.sourceKinds.contains(RouteNetworkBindProof.ProofSource)

  private def shouldPreferWhyNowSecondary(
      primary: QuestionPlan,
      secondary: QuestionPlan,
      primaryTrace: ContrastiveSupportAdmissibility.ContrastSupportTrace
  ): Boolean =
    primary.questionKind == AuthorQuestionKind.WhatMustBeStopped &&
      secondary.questionKind == AuthorQuestionKind.WhyNow &&
      primary.plannerOwnerKind == PlannerOwnerKind.ForcingDefense &&
      secondary.plannerOwnerKind == PlannerOwnerKind.ForcingDefense &&
      primaryTrace.contrast_reject_reason.contains(
        ContrastiveSupportAdmissibility.RejectReason.QuestionOutsideScope
      )

  private def plannerDraft(
      ctx: NarrativeContext,
      primary: QuestionPlan,
      secondary: Option[QuestionPlan],
      inputs: QuestionPlannerInputs,
      contrastTrace: ContrastiveSupportAdmissibility.ContrastSupportTrace
  ): Option[PlannerSlotDraft] =
    val admissibleContrast = contrastTrace.effectiveSupport(primary.contrast)
    val secondarySupport = secondarySupportText(primary, secondary, ctx)
    val primaryEvidence = plannerEvidenceHook(primary.evidence, ctx)
    val primaryConsequence = plannerConsequence(primary.consequence, ctx)
    val timingTension = plannerTimingTension(primary.questionKind, inputs, ctx)
    val claimOnlyAllowed = supportedLocalSurfaceOnly(primary)

    primary.questionKind match
      case AuthorQuestionKind.WhatMattersHere =>
        Some(
          PlannerSlotDraft(
            questionKind = AuthorQuestionKind.WhatMattersHere,
            lens = plannerLens(primary, inputs),
            claim = primary.claim,
            supportPrimary = admissibleContrast,
            supportSecondary = secondarySupport,
            tension = None,
            evidenceHook = primaryEvidence,
            coda = primaryConsequence,
            claimOnlyAllowed = claimOnlyAllowed
          )
        )
      case AuthorQuestionKind.WhyThis =>
        Some(
          PlannerSlotDraft(
            questionKind = AuthorQuestionKind.WhyThis,
            lens = plannerLens(primary, inputs),
            claim = primary.claim,
            supportPrimary = admissibleContrast,
            supportSecondary = secondarySupport,
            tension = None,
            evidenceHook = primaryEvidence,
            coda = primaryConsequence,
            claimOnlyAllowed = claimOnlyAllowed
          )
        )
      case AuthorQuestionKind.WhyNow =>
        Some(
          PlannerSlotDraft(
            questionKind = AuthorQuestionKind.WhyNow,
            lens = StrategicLens.Decision,
            claim = primary.claim,
            supportPrimary = admissibleContrast,
            supportSecondary = secondarySupport.filterNot(text => admissibleContrast.exists(sameSentence(_, text))),
            tension = timingTension,
            evidenceHook = primaryEvidence,
            coda = primaryConsequence,
            claimOnlyAllowed = claimOnlyAllowed
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
            coda = primaryConsequence,
            claimOnlyAllowed = claimOnlyAllowed
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
            coda = primaryConsequence,
            claimOnlyAllowed = claimOnlyAllowed
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
            coda = primaryConsequence,
            claimOnlyAllowed = claimOnlyAllowed
          )
        )

  private def finalizePlannerSlots(
      ctx: NarrativeContext,
      draft: PlannerSlotDraft
  ): Option[MoveReviewPolishSlots] =
    val cleanedClaim = sanitizePlannerClaim(draft, ctx)
    cleanedClaim.flatMap { claim =>
      if draft.claimOnlyAllowed then
        Some(
          MoveReviewPolishSlots(
            lens = draft.lens,
            claim = claim,
            supportPrimary = None,
            supportSecondary = None,
            tension = None,
            evidenceHook = None,
            coda = None,
            factGuardrails = Nil,
            paragraphPlan = List("p1=claim")
          )
        )
      else {
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
            MoveReviewPolishSlots(
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
          Option.when(moveReviewContractSafe(slots))(slots)
        }.flatten
      }
    }

  private def supportedLocalSurfaceOnly(primary: QuestionPlan): Boolean =
    primary.admissibilityReasons.contains("strategic_claim_supported_local")

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
          case AuthorQuestionKind.WhatMattersHere => StrategicLens.Structure
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

  private def moveReviewContractSafe(slots: MoveReviewPolishSlots): Boolean =
    val prose = LiveNarrativeCompressionCore.deterministicProse(slots)
    val evaluation = MoveReviewProseContract.evaluate(prose, slots)
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
      .map(MoveReviewSlotSanitizer.sanitizeUserText)
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
      ctx: NarrativeContext,
      plannerRuntime: PlannerRuntime,
      refs: Option[MoveReviewRefs],
      strategyPack: Option[StrategyPack]
  ): Option[MoveReviewPolishSlots] =
    exactFactualFallbackResult(ctx, plannerRuntime, refs, strategyPack).map(_.finalSlots)

  private def basicMoveExplanationSlots(
      ctx: NarrativeContext,
      refs: Option[MoveReviewRefs],
      truthContract: Option[DecisiveTruthContract],
      strategyPack: Option[StrategyPack]
  ): Option[MoveReviewPolishSlots] =
    MoveReviewExplanationBuilder.build(ctx, refs, truthContract, strategyPack).flatMap { explanation =>
      cleanSentence(explanation.prose, ctx).map { claim =>
        val support =
          explanation.shortLine.flatMap { line =>
            val preview = line.san.take(5).map(_.trim).filter(_.nonEmpty).mkString(" ")
            Option.when(preview.nonEmpty)(s"Short line: $preview.")
          }
        val localFactGuardrails =
          List(
            Some(s"MoveReview title draft: ${explanation.title}"),
            Some(s"MoveReview source: ${explanation.source}"),
            reviewTagValue(explanation, "review_intent").map(intent => s"MoveReview review intent: $intent"),
            reviewTagValue(explanation, "character_band").map(band => s"MoveReview character band: $band"),
            reviewTagValue(explanation, "line_proof").map(proof => s"MoveReview line proof: $proof"),
            reviewTagValue(explanation, "line_subject").map(subject => s"MoveReview PV subject: $subject"),
            Option.when(explanation.reasonTags.nonEmpty)(s"MoveReview reason tags: ${explanation.reasonTags.mkString(", ")}"),
            explanation.pvInterpretation.map(interpretation => s"PV line purpose: ${interpretation.linePurpose}"),
            explanation.pvInterpretation.map(interpretation => s"PV confirms: ${interpretation.confirms.mkString(", ")}"),
            explanation.pvInterpretation.map(interpretation => s"PV tension: ${interpretation.tension}"),
            explanation.pvInterpretation.flatMap(_.opponentReplyMeaning).map(meaning => s"PV opponent reply: $meaning"),
            explanation.pvInterpretation.map(interpretation => s"PV learning point: ${interpretation.learningPoint}")
          ).flatten
        MoveReviewPolishSlots(
          lens = StrategicLens.Decision,
          claim = prefixMoveHeader(ctx, claim),
          supportPrimary = support,
          supportSecondary = None,
          tension = None,
          evidenceHook = None,
          coda = None,
          factGuardrails = (localFactGuardrails ++ support.toList).distinct,
          paragraphPlan =
            if support.nonEmpty then List("p1=claim", "p2=support_chain")
            else List("p1=claim"),
          sourceKind = MoveReviewPolishSlots.Source.BasicMoveExplanation,
          moveReviewExplanation = Some(explanation),
          factFragments = explanation.factFragments
        )
      }
    }

  private def reviewTagValue(explanation: MoveReviewExplanation, key: String): Option[String] =
    val prefix = s"$key:"
    explanation.reasonTags.collectFirst {
      case tag if tag.startsWith(prefix) => tag.stripPrefix(prefix)
    }

  private def exactFactualFallbackResult(
      ctx: NarrativeContext,
      plannerRuntime: PlannerRuntime,
      refs: Option[MoveReviewRefs],
      strategyPack: Option[StrategyPack]
  ): Option[ExactFactualFallbackResult] =
    MoveReviewLocalFactualFallback.build(ctx, refs)
      .orElse(
        QuietMoveIntentBuilder.exactFactualSentence(ctx).map { factual =>
          MoveReviewLocalFactualFallback.Result(
            claim = factual,
            support = None,
            factGuardrails = Nil,
            sourceTag = "san_literal_fallback"
          )
        }
      )
      .flatMap { fallback =>
        cleanSentence(fallback.claim, ctx).map { factual =>
          val fallbackGuardrails =
            (fallback.factGuardrails :+ s"MoveReview exact factual fallback source: ${fallback.sourceTag}").distinct
          val claimOnly =
            MoveReviewPolishSlots(
              lens = StrategicLens.Decision,
              claim = prefixMoveHeader(ctx, factual),
              supportPrimary = None,
              supportSecondary = None,
              tension = None,
              evidenceHook = None,
              coda = None,
              factGuardrails = fallbackGuardrails,
              paragraphPlan = List("p1=claim"),
              sourceKind = MoveReviewPolishSlots.Source.ExactFactualFallback
            )
          val cleanLocalSupport =
            if fallback.sourceTag == "legal_local_factual" then cleanLocalFactualSupport(_, ctx)
            else cleanSentence(_, ctx)
          val localSupportCandidate =
            fallback.support
              .flatMap(cleanLocalSupport)
              .filter(text => !sameSentence(text, factual))
              .map { support =>
                claimOnly.copy(
                  supportPrimary = Some(support),
                  factGuardrails = (fallbackGuardrails ++ List(support)).distinct,
                  paragraphPlan = List("p1=claim", "p2=support_chain")
                )
              }
          val localSupportAccepted =
            localSupportCandidate.filter(moveReviewContractSafe)
          val baseSlots =
            localSupportAccepted.getOrElse(claimOnly)
          val composerTrace =
            QuietStrategicSupportComposer.diagnose(
              ctx,
              plannerRuntime.inputs,
              plannerRuntime.rankedPlans,
              strategyPack
            )
          val supportCandidate =
            Option.when(baseSlots.supportPrimary.isEmpty)(composerTrace.line).flatten
              .flatMap { line =>
                Option.when(!sameSentence(line.text, factual)) {
                  claimOnly.copy(
                    supportPrimary = Some(line.text),
                    factGuardrails = (fallbackGuardrails ++ List(line.text)).distinct,
                    paragraphPlan = List("p1=claim", "p2=support_chain")
                  )
                }
              }
          val liftApplied =
            supportCandidate.exists(moveReviewContractSafe)
          val rejectReasons =
            composerTrace.rejectReasons ++
              Option.when(localSupportCandidate.nonEmpty && localSupportAccepted.isEmpty)("local_factual_support_contract_rejected") ++
              Option.when(composerTrace.line.nonEmpty && baseSlots.supportPrimary.isEmpty && supportCandidate.isEmpty)("support_same_sentence_as_claim") ++
              Option.when(supportCandidate.nonEmpty && !liftApplied)("support_contract_rejected")
          ExactFactualFallbackResult(
            finalSlots =
              if baseSlots.supportPrimary.nonEmpty then baseSlots
              else supportCandidate.filter(moveReviewContractSafe).getOrElse(baseSlots),
            trace =
              ExactFactualQuietSupportTrace(
                factualSentence = Some(factual),
                composerTrace = composerTrace,
                liftApplied = liftApplied,
                rejectReasons = rejectReasons.distinct
              )
          )
        }
      }

  private def omittedSlots: MoveReviewPolishSlots =
    MoveReviewPolishSlots(
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
      .map(MoveReviewSlotSanitizer.sanitizeUserText)
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

  private def cleanLocalFactualSupport(raw: String, ctx: NarrativeContext): Option[String] =
    normalized(raw)
      .map(MoveReviewSlotSanitizer.sanitizeUserText)
      .map(LiveNarrativeCompressionCore.rewritePlayerLanguage)
      .flatMap(normalized)
      .map(LiveNarrativeCompressionCore.trimLeadScaffold)
      .flatMap(normalized)
      .filter(text => systemLanguageHits(text).isEmpty)
      .filter(text => LiveNarrativeCompressionCore.playerLanguageHits(text).isEmpty)
      .filter(LiveNarrativeCompressionCore.keepPlayerFacingSentence)
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

  private def variationGuardrail(refs: Option[MoveReviewRefs]): Option[String] =
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
