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

  private[commentary] final case class RuntimeResult(
      slots: MoveReviewPolishSlots,
      inputs: QuestionPlannerInputs,
      rankedPlans: RankedQuestionPlans,
      causalTrace: Option[CausalClaimTrace]
  )

  private[commentary] final case class CausalClaimTrace(
      status: String,
      questionKind: String,
      subjectRole: Option[String],
      evidenceKinds: List[String],
      relationKinds: List[String],
      rejectReasons: List[String],
      supportRenderedInClaim: Option[Boolean],
      guardrail: Option[String],
      localFactFamily: Option[String],
      localFactAuthority: Option[String],
      localFactStrictFallbackEligible: Option[Boolean],
      localFactGuardrails: List[String]
  )

  private final case class PlannerRuntime(
      inputs: QuestionPlannerInputs,
      rankedPlans: RankedQuestionPlans
  )

  private final case class ExactFactualFallbackResult(
      finalSlots: MoveReviewPolishSlots,
      trace: ExactFactualQuietSupportTrace
  )

  private val LineIndentPattern = """^[a-z]\)\s+.*""".r
  private val TimingMattersPattern = """the timing matters now\.?""".r
  private val MoveHeaderPrefixPattern = """^\d+\.(?:\.\.)?\s+[^:]+:\s*.*""".r
  private val WhitespacePattern = """\s+""".r

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
  private val PositiveBasicExplanationSources =
    Set("opening_goal", "certified_strategy_support", "basic_move_explanation")

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
    buildSlotsOrFallbackWithRuntime(ctx, outline, refs, strategyPack, truthContract).slots

  private[commentary] def buildSlotsOrFallbackWithRuntime(
      ctx: NarrativeContext,
      @unused outline: NarrativeOutline,
      refs: Option[MoveReviewRefs],
      strategyPack: Option[StrategyPack],
      truthContract: Option[DecisiveTruthContract] = None
  ): RuntimeResult =
    val plannerRuntime =
      plannerInputsRuntime(ctx, refs, strategyPack, truthContract)
    val causalTrace =
      causalClaimTrace(ctx, plannerRuntime.inputs, plannerRuntime.rankedPlans, truthContract, refs)
    val strictLocalFacts =
      causalTrace.exists(_.status == "rejected")
    val slots =
      slotsFromPlanner(ctx, plannerRuntime.inputs, plannerRuntime.rankedPlans, truthContract, refs)
        .orElse(basicMoveExplanationSlots(ctx, refs, truthContract, strategyPack, strictLocalFacts))
        .orElse(exactFactualFallbackSlots(ctx, plannerRuntime, refs, strategyPack))
        .orElse(theme_fallback(ctx, plannerRuntime, refs, strategyPack, truthContract))
        .getOrElse(omittedSlots)
    RuntimeResult(
      slots = slots,
      inputs = plannerRuntime.inputs,
      rankedPlans = plannerRuntime.rankedPlans,
      causalTrace = causalTrace
    )

  private[commentary] def buildSlotsOrFallbackFromPlannerRuntime(
      ctx: NarrativeContext,
      inputs: QuestionPlannerInputs,
      rankedPlans: RankedQuestionPlans,
      strategyPack: Option[StrategyPack],
      truthContract: Option[DecisiveTruthContract] = None,
      refs: Option[MoveReviewRefs] = None
  ): MoveReviewPolishSlots =
    val plannerRuntime =
      PlannerRuntime(
        inputs = inputs,
        rankedPlans = rankedPlans
      )
    slotsFromPlanner(ctx, inputs, rankedPlans, truthContract, refs)
      .orElse(exactFactualFallbackSlots(ctx, plannerRuntime, refs, strategyPack))
      .orElse(theme_fallback(ctx, plannerRuntime, refs, strategyPack, truthContract))
      .getOrElse(omittedSlots)

  private[analysis] def cleanNarrativeSentence(raw: String, ctx: NarrativeContext): Option[String] =
    cleanSentence(raw, ctx)

  private[commentary] def candidateEvidenceLines(
      refs: Option[MoveReviewRefs],
      ctx: NarrativeContext
  ): List[String] =
    variationGuardrail(refs, ctx)
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
    slotsFromPlanner(ctx, plannerRuntime.inputs, plannerRuntime.rankedPlans, truthContract, refs)

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
      causalClaim: Option[MoveReviewCausalClaim.CertifiedClaim]
  )

  private final case class PlannerCausalInputs(
      renderedClaim: String,
      certifiedContrast: Option[String],
      secondarySupport: Option[String],
      primaryEvidence: Option[String],
      lineConsequenceSurface: Option[String],
      primaryConsequence: Option[String],
      timingTension: Option[String],
      decision: MoveReviewCausalClaim.Decision
  )

  private def slotsFromPlanner(
      ctx: NarrativeContext,
      inputs: QuestionPlannerInputs,
      rankedPlans: RankedQuestionPlans,
      truthContract: Option[DecisiveTruthContract],
      refs: Option[MoveReviewRefs]
  ): Option[MoveReviewPolishSlots] =
    renderSelection(inputs, rankedPlans, truthContract)
      .flatMap(selection =>
        plannerDraft(
          ctx,
          selection.primary,
          selection.secondary,
          inputs,
          selection.contrastTrace,
          refs
        )
      )
      .flatMap(draft => finalizePlannerSlots(ctx, draft))

  private[commentary] def causalClaimTrace(
      ctx: NarrativeContext,
      inputs: QuestionPlannerInputs,
      rankedPlans: RankedQuestionPlans,
      truthContract: Option[DecisiveTruthContract],
      refs: Option[MoveReviewRefs]
  ): Option[CausalClaimTrace] =
    renderSelection(inputs, rankedPlans, truthContract).map { selection =>
      val decision =
        plannerCausalInputs(ctx, selection.primary, selection.secondary, inputs, selection.contrastTrace, refs).decision
      val claim = decision.claim
      CausalClaimTrace(
        status =
          if claim.nonEmpty then "accepted"
          else if decision.rejectReasons.nonEmpty then "rejected"
          else "not_applicable",
        questionKind = selection.primary.questionKind.toString,
        subjectRole = claim.map(_.subjectRole.wireName),
        evidenceKinds = claim.map(_.evidenceKinds.map(_.wireName).distinct).getOrElse(Nil),
        relationKinds = claim.map(_.relationKinds.map(_.wireName).distinct).getOrElse(Nil),
        rejectReasons = decision.rejectReasons,
        supportRenderedInClaim = claim.map(_.supportRenderedInClaim),
        guardrail = claim.map(_.guardrail),
        localFactFamily = claim.flatMap(_.localFact.map(_.family.key)),
        localFactAuthority = claim.flatMap(_.localFact.map(_.authority.key)),
        localFactStrictFallbackEligible = claim.flatMap(_.localFact.map(_.strictFallbackEligible)),
        localFactGuardrails = claim.flatMap(_.localFact).map(_.tags).getOrElse(Nil)
      )
    }

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
      contrastTrace: ContrastiveSupportAdmissibility.ContrastSupportTrace,
      refs: Option[MoveReviewRefs]
  ): Option[PlannerSlotDraft] =
    val causalInputs = plannerCausalInputs(ctx, primary, secondary, inputs, contrastTrace, refs)

    def slotDraft(
        questionKind: AuthorQuestionKind,
        lens: StrategicLens,
        supportPrimary: Option[String],
        supportSecondary: Option[String],
        tension: Option[String],
        evidenceHook: Option[String],
        coda: Option[String]
    ): Option[PlannerSlotDraft] =
      Option.when(
        causalInputs.decision.claim.nonEmpty ||
          !MoveReviewCausalClaim.requiresTypedSupport(primary.questionKind)
      ) {
        PlannerSlotDraft(
          questionKind = questionKind,
          lens = lens,
          claim = causalInputs.decision.claim.map(_.surfaceText).getOrElse(causalInputs.renderedClaim),
          supportPrimary = supportPrimary,
          supportSecondary = supportSecondary,
          tension = tension,
          evidenceHook = evidenceHook,
          coda = coda,
          causalClaim = causalInputs.decision.claim
        )
      }

    primary.questionKind match
      case AuthorQuestionKind.WhatMattersHere =>
        slotDraft(
            questionKind = AuthorQuestionKind.WhatMattersHere,
            lens = plannerLens(primary, inputs),
            supportPrimary = primary.contrast,
            supportSecondary = causalInputs.secondarySupport,
            tension = None,
            evidenceHook = causalInputs.primaryEvidence,
            coda = causalInputs.primaryConsequence
        )
      case AuthorQuestionKind.WhyThis =>
        slotDraft(
            questionKind = AuthorQuestionKind.WhyThis,
            lens = plannerLens(primary, inputs),
            supportPrimary = causalInputs.certifiedContrast,
            supportSecondary = causalInputs.secondarySupport,
            tension = None,
            evidenceHook = causalInputs.primaryEvidence,
            coda = causalInputs.primaryConsequence
        )
      case AuthorQuestionKind.WhyNow =>
        slotDraft(
            questionKind = AuthorQuestionKind.WhyNow,
            lens = StrategicLens.Decision,
            supportPrimary = causalInputs.certifiedContrast,
            supportSecondary =
              causalInputs.secondarySupport.filterNot(text => causalInputs.certifiedContrast.exists(sameSentence(_, text))),
            tension = causalInputs.timingTension,
            evidenceHook = causalInputs.primaryEvidence,
            coda = causalInputs.primaryConsequence
        )
      case AuthorQuestionKind.WhatChanged =>
        slotDraft(
            questionKind = AuthorQuestionKind.WhatChanged,
            lens = plannerLens(primary, inputs),
            supportPrimary = primary.contrast,
            supportSecondary = causalInputs.secondarySupport,
            tension = None,
            evidenceHook = causalInputs.primaryEvidence,
            coda = causalInputs.primaryConsequence
        )
      case AuthorQuestionKind.WhatMustBeStopped =>
        slotDraft(
            questionKind = AuthorQuestionKind.WhatMustBeStopped,
            lens =
              if primary.sourceKinds.exists(kind => kind.contains("threat")) &&
                inputs.truthMode == PlayerFacingTruthMode.Tactical
              then StrategicLens.Decision
              else StrategicLens.Prophylaxis,
            supportPrimary = primary.contrast,
            supportSecondary = causalInputs.secondarySupport,
            tension = causalInputs.timingTension,
            evidenceHook = causalInputs.primaryEvidence,
            coda = causalInputs.primaryConsequence
        )
      case AuthorQuestionKind.WhosePlanIsFaster =>
        slotDraft(
            questionKind = AuthorQuestionKind.WhosePlanIsFaster,
            lens = StrategicLens.Decision,
            supportPrimary = inputs.decisionFrame.battlefront.map(_.sentence).orElse(primary.contrast),
            supportSecondary = causalInputs.secondarySupport,
            tension = causalInputs.timingTension,
            evidenceHook = causalInputs.primaryEvidence,
            coda = causalInputs.primaryConsequence
        )

  private def plannerCausalInputs(
      ctx: NarrativeContext,
      primary: QuestionPlan,
      secondary: Option[QuestionPlan],
      inputs: QuestionPlannerInputs,
      contrastTrace: ContrastiveSupportAdmissibility.ContrastSupportTrace,
      refs: Option[MoveReviewRefs]
  ): PlannerCausalInputs =
    val renderedClaim = primary.prefixKind.render(primary.claim)
    val certifiedContrast =
      Option.when(contrastTrace.contrast_admissible)(contrastTrace.effectiveSupport(primary.contrast)).flatten
    val secondarySupport = secondarySupportText(primary, secondary, ctx)
    val primaryEvidence = plannerEvidenceHook(primary.evidence, ctx)
    val lineConsequenceSurface = plannerLineConsequenceSurface(primary, ctx, refs)
    val primaryConsequence = lineConsequenceSurface.orElse(plannerConsequence(primary.consequence, ctx))
    val timingTension = plannerTimingTension(primary.questionKind, inputs, ctx)
    val typedTimingSupport =
      primary.questionKind == AuthorQuestionKind.WhyNow &&
        (
          primary.timingWitness.nonEmpty ||
            contrastTrace.contrast_source_kind.contains(ContrastiveSupportAdmissibility.SourceKind.ExplicitReplyLoss)
        )
    val primaryCausalSupport =
      primary.questionKind match
        case AuthorQuestionKind.WhyThis | AuthorQuestionKind.WhyNow => certifiedContrast
        case _                                                      => primary.contrast
    val decision =
      MoveReviewCausalClaim.decide(
        primary,
        renderedClaim,
        contrastTrace.contrast_admissible,
        primaryCausalSupport,
        secondarySupport,
        timingTension,
        primaryEvidence,
        primaryConsequence,
        lineConsequenceSurface,
        typedTimingSupport
      )
    PlannerCausalInputs(
      renderedClaim = renderedClaim,
      certifiedContrast = certifiedContrast,
      secondarySupport = secondarySupport,
      primaryEvidence = primaryEvidence,
      lineConsequenceSurface = lineConsequenceSurface,
      primaryConsequence = primaryConsequence,
      timingTension = timingTension,
      decision = decision
    )

  private def finalizePlannerSlots(
      ctx: NarrativeContext,
      draft: PlannerSlotDraft
  ): Option[MoveReviewPolishSlots] =
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

      val hasSupport = hasPlannerSupport(supportPrimary, supportSecondary, tension, evidenceHook, coda)
      val supportSatisfiedByClaim = draft.causalClaim.exists(_.supportRenderedInClaim)
      val causalGuardrails = draft.causalClaim.map(_.guardrail).toList
      if draft.causalClaim.exists(_.supportRequired) && !hasSupport && !supportSatisfiedByClaim then None
      else if !hasSupport then
        Some(
          MoveReviewPolishSlots(
            lens = draft.lens,
            claim = prefixMoveHeader(ctx, claim),
            supportPrimary = None,
            supportSecondary = None,
            tension = None,
            evidenceHook = None,
            coda = None,
            factGuardrails = causalGuardrails,
            paragraphPlan = List("p1=claim"),
            localFact = draft.causalClaim.flatMap(_.localFact)
          )
        )
      else {
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
            factGuardrails =
              (supportLines ++
                tension.toList ++
                evidenceHook.toList ++
                coda.toList ++
                causalGuardrails).distinct,
            paragraphPlan = plannerParagraphPlan(supportLines, tension, evidenceHook, coda),
            localFact = draft.causalClaim.flatMap(_.localFact)
          )
        Option.when(moveReviewContractSafe(slots))(slots)
      }
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
          e.text.linesIterator.exists(line => LineIndentPattern.matches(line.trim))
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

  private def plannerLineConsequenceSurface(
      primary: QuestionPlan,
      ctx: NarrativeContext,
      refs: Option[MoveReviewRefs]
  ): Option[String] =
    Option.when(playedMoveLineConsequenceSurfaceAllowed(primary)) {
      LineConsequenceEvaluator
        .surfaceCandidate(ctx, refs)
        .filter(_.kind != LineConsequenceKind.PreviewOnly)
        .flatMap(VariationNarrativeBuilder.build(ctx, _))
        .flatMap(cleanSentence(_, ctx))
    }.flatten

  private def playedMoveLineConsequenceSurfaceAllowed(primary: QuestionPlan): Boolean =
    (primary.questionKind == AuthorQuestionKind.WhyThis || primary.questionKind == AuthorQuestionKind.WhatChanged) &&
      (primary.plannerOwnerKind == PlannerOwnerKind.MoveDelta || primary.plannerOwnerKind == PlannerOwnerKind.TacticalFailure)

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
          LineIndentPattern.matches(text) || LineScopedCitation.hasConcreteSanLine(text)
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
      !TimingMattersPattern.matches(low)

  private[analysis] def relaxedCertifiedRaceSentence(raw: String, ctx: NarrativeContext): Option[String] =
    cleanPlayerFacingSentence(
      raw,
      ctx,
      requirePlayerFacingSentence = false,
      rejectLowValue = true,
      rejectLimitedSupport = false
    )

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

  private def theme_fallback(
      ctx: NarrativeContext,
      plannerRuntime: PlannerRuntime,
      @unused refs: Option[MoveReviewRefs],
      @unused strategyPack: Option[StrategyPack],
      truthContract: Option[DecisiveTruthContract]
  ): Option[MoveReviewPolishSlots] =
    val activeTheme =
      if ctx.plans.top5.nonEmpty then
        PlanTaxonomy.ThemeResolver.fromPlanName(ctx.plans.top5.head.name)
      else if plannerRuntime.inputs.evidenceBackedPlans.nonEmpty then
        PlanTaxonomy.ThemeResolver.fromHypotheses(plannerRuntime.inputs.evidenceBackedPlans)
      else
        PlanTaxonomy.PlanTheme.Unknown

    val thematicFallbackBlocked =
      plannerRuntime.inputs.truthMode == PlayerFacingTruthMode.Tactical ||
        plannerRuntime.inputs.mainBundle.exists(_.primaryClaim.exists(_.mode == PlayerFacingTruthMode.Tactical)) ||
        truthContract.exists { contract =>
          contract.blocksStrategicSupport ||
            contract.reasonFamily == DecisiveReasonKind.TacticalRefutation
        }

    Option.when(activeTheme != PlanTaxonomy.PlanTheme.Unknown && !thematicFallbackBlocked) {
      val claim = theme_fallback_prose(activeTheme)
      val prefixClaim = prefixMoveHeader(ctx, claim)
      MoveReviewPolishSlots(
        lens = StrategicLens.Decision,
        claim = prefixClaim,
        supportPrimary = None,
        supportSecondary = None,
        tension = None,
        evidenceHook = None,
        coda = None,
        factGuardrails = List(s"MoveReview thematic fallback theme: ${activeTheme.id}"),
        paragraphPlan = List("p1=claim"),
        sourceKind = MoveReviewPolishSlots.Source.ThematicFallback
      )
    }

  private def theme_fallback_prose(theme: PlanTaxonomy.PlanTheme): String =
    theme match
      case PlanTaxonomy.PlanTheme.RestrictionProphylaxis =>
        "The move keeps the opponent's activity restrained and aims to consolidate."
      case PlanTaxonomy.PlanTheme.PieceRedeployment =>
        "The move improves piece activity and looks for better squares."
      case PlanTaxonomy.PlanTheme.SpaceClamp =>
        "The move gains space while limiting the opponent's mobility."
      case PlanTaxonomy.PlanTheme.WeaknessFixation =>
        "The move keeps pressure on fixed weaknesses."
      case PlanTaxonomy.PlanTheme.PawnBreakPreparation =>
        "The move coordinates the pieces for a central or wing pawn break."
      case PlanTaxonomy.PlanTheme.FavorableExchange =>
        "The move steers toward favorable exchanges and better piece quality."
      case PlanTaxonomy.PlanTheme.FlankInfrastructure =>
        "The move builds on the flank and prepares attacking chances."
      case PlanTaxonomy.PlanTheme.AdvantageTransformation =>
        "The move steers the advantage toward a simpler winning ending."
      case PlanTaxonomy.PlanTheme.ImmediateTacticalGain | PlanTaxonomy.PlanTheme.OpeningPrinciples | _ =>
        "The move improves piece activity and coordination."

  private def basicMoveExplanationSlots(
      ctx: NarrativeContext,
      refs: Option[MoveReviewRefs],
      truthContract: Option[DecisiveTruthContract],
      strategyPack: Option[StrategyPack],
      strictLocalFacts: Boolean
  ): Option[MoveReviewPolishSlots] =
    MoveReviewExplanationBuilder.buildWithLocalFact(ctx, refs, truthContract, strategyPack, strictLocalFacts).flatMap { result =>
      val explanation = result.explanation
      if truthContract.exists(_.blocksStrategicSupport) &&
          PositiveBasicExplanationSources.contains(explanation.source.trim.toLowerCase)
      then None
      else
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
            factFragments = explanation.factFragments,
            localFact = Some(result.localFact)
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
          val rejectReasons =
            composerTrace.rejectReasons ++
              Option.when(localSupportCandidate.nonEmpty && localSupportAccepted.isEmpty)("local_factual_support_contract_rejected") ++
              Option.when(composerTrace.line.nonEmpty)("quiet_support_diagnostic_only")
          ExactFactualFallbackResult(
            finalSlots = baseSlots,
            trace =
              ExactFactualQuietSupportTrace(
                factualSentence = Some(factual),
                composerTrace = composerTrace,
                liftApplied = false,
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
    cleanPlayerFacingSentence(
      raw,
      ctx,
      requirePlayerFacingSentence = true,
      rejectLowValue = true,
      rejectLimitedSupport = true
    )

  private def cleanLocalFactualSupport(raw: String, ctx: NarrativeContext): Option[String] =
    cleanPlayerFacingSentence(
      raw,
      ctx,
      requirePlayerFacingSentence = true,
      rejectLowValue = false,
      rejectLimitedSupport = true
    )

  private def cleanPlayerFacingSentence(
      raw: String,
      ctx: NarrativeContext,
      requirePlayerFacingSentence: Boolean,
      rejectLowValue: Boolean,
      rejectLimitedSupport: Boolean
  ): Option[String] =
    normalized(raw)
      .map(UserFacingSignalSanitizer.sanitize)
      .map(LiveNarrativeCompressionCore.rewritePlayerLanguage)
      .flatMap(normalized)
      .map(LiveNarrativeCompressionCore.trimLeadScaffold)
      .flatMap(normalized)
      .filter(text => systemLanguageHits(text).isEmpty)
      .filter(text => LiveNarrativeCompressionCore.playerLanguageHits(text).isEmpty)
      .filter(text => !requirePlayerFacingSentence || LiveNarrativeCompressionCore.keepPlayerFacingSentence(text))
      .filter(text => !rejectLowValue || !LiveNarrativeCompressionCore.isLowValueNarrativeSentence(text))
      .filter(text => !containsNonBackedPlanName(text, ctx))
      .filter(text => namedPlanAllowed(text, ctx))
      .filter(text => !rejectLimitedSupport || !text.equalsIgnoreCase("Concrete support is still limited."))

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
    if Option(claim).exists(MoveHeaderPrefixPattern.matches) then claim
    else
      val moveHeader =
        for
          san <- ctx.playedSan.filter(_.trim.nonEmpty)
        yield
          val moveNum = (ctx.ply + 1) / 2
          val prefix = if ctx.ply % 2 == 1 then s"$moveNum." else s"$moveNum..."
          s"$prefix $san:"
      moveHeader.map(h => s"$h $claim").getOrElse(claim)

  private def variationGuardrail(refs: Option[MoveReviewRefs], ctx: NarrativeContext): Option[String] =
    val consequence = LineConsequenceEvaluator.narrativeCandidate(ctx, refs)
    val richNarrative = consequence.flatMap(c => VariationNarrativeBuilder.build(ctx, c))
    richNarrative.orElse {
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
          s"$preview$eval."
        }
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
      .map(s => WhitespacePattern.replaceAllIn(s, " ").trim)

  private def sameSentence(left: String, right: String): Boolean =
    val normalizedLeft = normalized(left).map(_.toLowerCase).getOrElse("")
    val normalizedRight = normalized(right).map(_.toLowerCase).getOrElse("")
    normalizedLeft.nonEmpty && normalizedLeft == normalizedRight
