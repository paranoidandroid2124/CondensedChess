package lila.llm.analysis

import lila.llm.StrategyPack
import lila.llm.analysis.practical.ContrastiveSupportAdmissibility
import lila.llm.analysis.render.QuietStrategicSupportComposer
import lila.llm.model.*
import lila.llm.model.authoring.{ AuthorQuestionKind, OutlineBeat, OutlineBeatKind }

private[llm] object GameChronicleCompressionPolicy:

  private final case class BeatSurface(
      beat: OutlineBeat,
      sentences: List[String],
      citedLine: Option[String]
  )

  final case class ChroniclePlanSurface(
      primary: QuestionPlan,
      secondary: Option[QuestionPlan]
  )

  private[llm] final case class ChronicleRenderSurface(
      primary: QuestionPlan,
      secondary: Option[QuestionPlan],
      contrastTrace: ContrastiveSupportAdmissibility.ContrastSupportTrace,
      quietSupportTrace: QuietStrategicSupportComposer.QuietStrategicSupportTrace
  )

  private[llm] final case class ChronicleQuietSupportTrace(
      applied: Boolean,
      rejectReasons: List[String],
      composerTrace: QuietStrategicSupportComposer.QuietStrategicSupportTrace
  )

  private[llm] final case class ChronicleRenderArtifact(
      narrative: String,
      quietSupportTrace: ChronicleQuietSupportTrace
  )

  private final case class ChronicleQuietSupportDecision(
      line: Option[String],
      trace: ChronicleQuietSupportTrace
  )

  private val ChronicleTokenStopWords =
    Set(
      "this",
      "that",
      "with",
      "from",
      "into",
      "before",
      "after",
      "because",
      "move",
      "line",
      "plan",
      "them",
      "they",
      "your",
      "their",
      "here",
      "there"
    )

  def render(
      ctx: NarrativeContext,
      parts: CommentaryEngine.HybridNarrativeParts,
      strategyPack: Option[StrategyPack] = None,
      truthContract: Option[DecisiveTruthContract] = None
  ): Option[String] =
    renderWithTrace(ctx, parts, strategyPack, truthContract).map(_.narrative)

  private[llm] def renderWithTrace(
      ctx: NarrativeContext,
      parts: CommentaryEngine.HybridNarrativeParts,
      strategyPack: Option[StrategyPack] = None,
      truthContract: Option[DecisiveTruthContract] = None
  ): Option[ChronicleRenderArtifact] =
    val surfaces =
      parts.focusedOutline.beats
        .filterNot(isStrategicDistributionBeat)
        .filterNot(_.kind == OutlineBeatKind.MoveHeader)
        .flatMap(renderSurface(ctx, _))
    val beatEvidence = citedLineCandidates(parts, surfaces)
    val candidateEvidence =
      beatEvidence.flatMap(text => BookmakerLiveCompressionPolicy.cleanNarrativeSentence(text, ctx))
    val plannerInputs =
      QuestionPlannerInputsBuilder.build(ctx, strategyPack, truthContract, candidateEvidence)
    val rankedPlans = QuestionFirstCommentaryPlanner.plan(ctx, plannerInputs, truthContract)

    renderSelection(ctx, rankedPlans, plannerInputs, strategyPack, truthContract)
      .flatMap(surface => renderPlanSurface(ctx, surface, beatEvidence))
      .orElse {
        factualFallback(ctx, plannerInputs).map { text =>
          val composerTrace =
            QuietStrategicSupportComposer.diagnose(
              ctx,
              plannerInputs,
              rankedPlans,
              strategyPack
            )
          factualFallbackRenderArtifact(ctx, text, composerTrace)
        }
      }

  private[llm] def renderSelection(
      ctx: NarrativeContext,
      rankedPlans: RankedQuestionPlans,
      inputs: QuestionPlannerInputs,
      strategyPack: Option[StrategyPack],
      truthContract: Option[DecisiveTruthContract]
  ): Option[ChronicleRenderSurface] =
    selectPlannerSurface(rankedPlans, inputs).map(surface =>
      ChronicleRenderSurface(
        primary = surface.primary,
        secondary = surface.secondary,
        contrastTrace = ContrastiveSupportAdmissibility.decide(surface.primary, inputs, truthContract),
        quietSupportTrace =
          QuietStrategicSupportComposer.diagnose(
            ctx,
            inputs,
            rankedPlans,
            strategyPack
          )
      )
    )

  private def factualFallback(
      ctx: NarrativeContext,
      inputs: QuestionPlannerInputs
  ): Option[String] =
    inputs.factualFallback
      .flatMap(text => cleanChronicleSentence(text, ctx))

  private def factualFallbackRenderArtifact(
      ctx: NarrativeContext,
      claim: String,
      composerTrace: QuietStrategicSupportComposer.QuietStrategicSupportTrace
  ): ChronicleRenderArtifact =
    val cleanedCandidate =
      composerTrace.line.flatMap(line => cleanChronicleSentence(line.text, ctx))
    val duplicateWithClaim =
      cleanedCandidate.exists(text => sameSentence(text, claim))
    val localRejectReasons =
      List(
        Option.when(!composerTrace.emitted)("chronicle_quiet_support_not_emitted"),
        Option.when(composerTrace.line.nonEmpty && cleanedCandidate.isEmpty)("candidate_cleaned_empty"),
        Option.when(duplicateWithClaim)("support_same_sentence_as_claim"),
        Option.when(cleanedCandidate.nonEmpty && !duplicateWithClaim)("chronicle_exact_factual_support_blocked")
      ).flatten
    ChronicleRenderArtifact(
      narrative = FullGameDraftNormalizer.normalize(claim).trim,
      quietSupportTrace =
        ChronicleQuietSupportTrace(
          applied = false,
          rejectReasons = (composerTrace.rejectReasons ++ localRejectReasons).distinct,
          composerTrace = composerTrace
        )
    )

  def selectPlannerSurface(
      rankedPlans: RankedQuestionPlans,
      inputs: QuestionPlannerInputs
  ): Option[ChroniclePlanSurface] =
    rankedPlans.primary.flatMap { primary =>
      val secondary = rankedPlans.secondary
      val eligibleSecondary =
        secondary.filter(candidate =>
          !replayClosedNamedRouteNetwork(candidate) &&
            !preserveThreatStopPrimary(primary, candidate) &&
            chroniclePriority(candidate.questionKind) > chroniclePriority(primary.questionKind) &&
            notWeaker(candidate, primary, inputs)
        )
      if replayClosedNamedRouteNetwork(primary) then
        eligibleSecondary.map(candidate => ChroniclePlanSurface(primary = candidate, secondary = None))
      else
        Some(
          eligibleSecondary
            .map(candidate => ChroniclePlanSurface(primary = candidate, secondary = Some(primary)))
            .getOrElse(ChroniclePlanSurface(primary = primary, secondary = secondary.filterNot(replayClosedNamedRouteNetwork)))
        )
    }

  private def replayClosedNamedRouteNetwork(
      plan: QuestionPlan
  ): Boolean =
    plan.ownerSource == NamedRouteNetworkBindCertification.OwnerSource ||
      plan.sourceKinds.contains(NamedRouteNetworkBindCertification.OwnerSource)

  private def preserveThreatStopPrimary(
      primary: QuestionPlan,
      candidate: QuestionPlan
  ): Boolean =
    primary.questionKind == AuthorQuestionKind.WhatMustBeStopped &&
      candidate.questionKind == AuthorQuestionKind.WhyNow &&
      primary.ownerFamily == OwnerFamily.ForcingDefense &&
      candidate.ownerFamily == OwnerFamily.ForcingDefense &&
      primary.ownerSource == "threat" &&
      candidate.ownerSource == "threat"

  private[llm] def renderPlanSurface(
      ctx: NarrativeContext,
      surface: ChronicleRenderSurface,
      beatEvidence: List[String]
  ): Option[ChronicleRenderArtifact] =
    sanitizePrimaryClaim(surface.primary, ctx).flatMap { claim =>
      val contrast =
        sanitizeDistinctSentence(surface.contrastTrace.effectiveSupport(surface.primary.contrast), ctx, claim)
      val evidence =
        plannerEvidenceLine(surface.primary, ctx)
          .orElse(beatEvidenceLine(surface.primary, beatEvidence, ctx, claim, contrast))
      val primaryConsequence =
        plannerConsequenceLine(surface.primary.consequence, ctx, claim, contrast, evidence)
      val secondarySupport =
        secondarySupportLine(surface.primary, surface.secondary, ctx, claim, contrast, evidence)
      val quietSupport =
        chronicleQuietSupportDecision(
          surface,
          ctx,
          claim,
          contrast,
          evidence,
          primaryConsequence,
          secondarySupport
        )
      val support =
        secondarySupport.orElse(primaryConsequence).orElse(quietSupport.line)

      val sentences =
        distinctChronicleSentences(
          List(Some(claim), contrast, evidence, support).flatten
        )

      Option.when(sentences.nonEmpty) {
        ChronicleRenderArtifact(
          narrative = FullGameDraftNormalizer.normalize(sentences.mkString(" ")).trim,
          quietSupportTrace = quietSupport.trace
        )
      }.filter(artifact => artifact.narrative.nonEmpty && !isLowValueSentence(artifact.narrative))
    }

  private def sanitizePrimaryClaim(
      plan: QuestionPlan,
      ctx: NarrativeContext
  ): Option[String] =
    cleanChronicleSentence(plan.claim, ctx)
      .orElse {
        Option.when(plan.questionKind == AuthorQuestionKind.WhosePlanIsFaster) {
          BookmakerLiveCompressionPolicy.relaxedCertifiedRaceSentence(plan.claim, ctx)
        }.flatten
      }

  private def chronicleQuietSupportDecision(
      surface: ChronicleRenderSurface,
      ctx: NarrativeContext,
      claim: String,
      contrast: Option[String],
      evidence: Option[String],
      primaryConsequence: Option[String],
      secondarySupport: Option[String]
  ): ChronicleQuietSupportDecision =
    val composerTrace = surface.quietSupportTrace
    val surfacePrimaryEligible =
      surface.primary.ownerFamily == OwnerFamily.MoveDelta &&
        surface.primary.ownerSource == "pv_delta"
    val supportAlreadyPresent =
      List(contrast, evidence, primaryConsequence, secondarySupport).flatten.nonEmpty
    val cleanedCandidate =
      composerTrace.line.flatMap(line => cleanChronicleSentence(line.text, ctx))
    val duplicateWithClaim =
      cleanedCandidate.exists(text => sameSentence(text, claim))
    val duplicateWithExistingSupport =
      cleanedCandidate.exists { text =>
        List(contrast, evidence, primaryConsequence, secondarySupport).flatten.exists(existing =>
          sameSentence(existing, text)
        )
      }
    val candidate =
      cleanedCandidate.filter(_ =>
        composerTrace.emitted &&
          surfacePrimaryEligible &&
          !supportAlreadyPresent &&
          !duplicateWithClaim &&
          !duplicateWithExistingSupport
      )
    val localRejectReasons =
      List(
        Option.when(!surfacePrimaryEligible)("surface_primary_not_movedelta_pv_delta"),
        Option.when(supportAlreadyPresent)("surface_support_already_present"),
        Option.when(composerTrace.line.nonEmpty && cleanedCandidate.isEmpty)("candidate_cleaned_empty"),
        Option.when(duplicateWithClaim)("support_same_sentence_as_claim"),
        Option.when(duplicateWithExistingSupport)("support_same_sentence_as_existing_support")
      ).flatten
    ChronicleQuietSupportDecision(
      line = candidate,
      trace =
        ChronicleQuietSupportTrace(
          applied = candidate.nonEmpty,
          rejectReasons = (composerTrace.rejectReasons ++ localRejectReasons).distinct,
          composerTrace = composerTrace
        )
    )

  private def chroniclePriority(kind: AuthorQuestionKind): Int =
    kind match
      case AuthorQuestionKind.WhyNow             => 5
      case AuthorQuestionKind.WhatChanged        => 4
      case AuthorQuestionKind.WhatMustBeStopped  => 3
      case AuthorQuestionKind.WhyThis            => 2
      case AuthorQuestionKind.WhosePlanIsFaster  => 1

  private def notWeaker(
      candidate: QuestionPlan,
      current: QuestionPlan,
      inputs: QuestionPlannerInputs
  ): Boolean =
    strengthScore(candidate.strengthTier) >= strengthScore(current.strengthTier) &&
      fallbackScore(candidate.fallbackMode) >= fallbackScore(current.fallbackMode) &&
      claimOwnershipScore(candidate, inputs) >= claimOwnershipScore(current, inputs) &&
      evidenceScore(candidate.evidence) >= evidenceScore(current.evidence)

  private def strengthScore(tier: QuestionPlanStrengthTier): Int =
    tier match
      case QuestionPlanStrengthTier.Strong   => 3
      case QuestionPlanStrengthTier.Moderate => 2
      case QuestionPlanStrengthTier.Exact    => 1

  private def fallbackScore(mode: QuestionPlanFallbackMode): Int =
    mode match
      case QuestionPlanFallbackMode.PlannerOwned              => 4
      case QuestionPlanFallbackMode.DemotedToWhyThis          => 3
      case QuestionPlanFallbackMode.DemotedToWhatMustBeStopped => 3
      case QuestionPlanFallbackMode.FactualFallback           => 2
      case QuestionPlanFallbackMode.Suppressed                => 1

  private def evidenceScore(evidence: Option[QuestionPlanEvidence]): Int =
    evidence match
      case Some(value) if value.text.linesIterator.count(_.trim.matches("""^[a-z]\)\s+.*""")) >= 2 => 3
      case Some(value) if value.branchScoped => 2
      case Some(_)                           => 1
      case None                              => 0

  private def claimOwnershipScore(
      plan: QuestionPlan,
      inputs: QuestionPlannerInputs
  ): Int =
    if plan.ownerFamily == OwnerFamily.OpeningRelation &&
      plan.ownerSource == "opening_relation_translator"
    then
      if plan.questionKind == AuthorQuestionKind.WhyThis then 5 else 4
    else if plan.ownerFamily == OwnerFamily.EndgameTransition &&
      plan.ownerSource == "endgame_transition_translator"
    then
      if plan.questionKind == AuthorQuestionKind.WhatChanged then 5 else 4
    else if inputs.mainBundle.flatMap(_.mainClaim).exists(claim => sameSentence(claim.claimText, plan.claim)) then 4
    else if plan.questionKind == AuthorQuestionKind.WhatChanged &&
      plan.sourceKinds.exists(kind =>
        kind == "pv_delta" || kind == "move_delta" || kind == "prevented_plan" || kind == "decision_comparison"
      )
    then 4
    else if inputs.quietIntent.exists(intent => sameSentence(intent.claimText, plan.claim)) then 3
    else if plan.questionKind == AuthorQuestionKind.WhatMustBeStopped &&
      plan.sourceKinds.exists(kind => kind == "threat" || kind == "prevented_plan")
    then 3
    else if plan.questionKind == AuthorQuestionKind.WhyNow &&
      plan.sourceKinds.exists(kind =>
        kind == "threat" || kind == "prevented_plan" || kind == "truth_contract" || kind == "decision_comparison"
      )
    then 4
    else if plan.questionKind == AuthorQuestionKind.WhosePlanIsFaster &&
      plan.sourceKinds.exists(kind => kind == "evidence_backed_plan" || kind == "opponent_plan")
    then 2
    else 1

  private def plannerEvidenceLine(
      plan: QuestionPlan,
      ctx: NarrativeContext
  ): Option[String] =
    plan.evidence
      .flatMap { evidence =>
        evidence.text.linesIterator
          .map(_.trim)
          .find(line =>
            line.nonEmpty &&
              (line.matches("""^[a-z]\)\s+.*""") ||
                LineScopedCitation.hasInlineCitation(line) ||
                LineScopedCitation.hasConcreteSanLine(line))
          )
      }
      .flatMap(text => cleanChronicleSentence(text, ctx))

  private def beatEvidenceLine(
      plan: QuestionPlan,
      beatEvidence: List[String],
      ctx: NarrativeContext,
      claim: String,
      contrast: Option[String]
  ): Option[String] =
    beatEvidence
      .filter(line =>
        LineScopedCitation.hasInlineCitation(line) ||
          LineScopedCitation.hasConcreteSanLine(line)
      )
      .find(line => evidenceMatchesPlan(line, plan))
      .flatMap(text => cleanChronicleSentence(text, ctx))
      .filter(text =>
        !sameSentence(text, claim) &&
          !contrast.exists(sameSentence(_, text))
      )

  private def plannerConsequenceLine(
      consequence: Option[QuestionPlanConsequence],
      ctx: NarrativeContext,
      claim: String,
      contrast: Option[String],
      evidence: Option[String]
  ): Option[String] =
    consequence
      .filter(_.certified)
      .map(_.text)
      .flatMap(text => cleanChronicleSentence(text, ctx))
      .filter(text =>
        !sameSentence(text, claim) &&
          !contrast.exists(sameSentence(_, text)) &&
          !evidence.exists(sameSentence(_, text))
      )

  private def secondarySupportLine(
      primary: QuestionPlan,
      secondary: Option[QuestionPlan],
      ctx: NarrativeContext,
      claim: String,
      contrast: Option[String],
      evidence: Option[String]
  ): Option[String] =
    secondary
      .flatMap { plan =>
        plan.contrast
          .orElse(plan.consequence.filter(_.certified).map(_.text))
      }
      .flatMap(text => cleanChronicleSentence(text, ctx))
      .filter(text =>
        !sameSentence(text, claim) &&
          !sameSentence(text, primary.claim) &&
          !contrast.exists(sameSentence(_, text)) &&
          !evidence.exists(sameSentence(_, text))
      )

  private def evidenceMatchesPlan(line: String, plan: QuestionPlan): Boolean =
    val planTokens =
      significantTokens(
        List(
          plan.claim,
          plan.contrast.getOrElse(""),
          plan.consequence.map(_.text).getOrElse("")
        ).mkString(" ")
      )
    val lineTokens = significantTokens(line)
    lineTokens.intersect(planTokens).nonEmpty ||
      (
        plan.questionKind match
          case AuthorQuestionKind.WhyNow =>
            containsAny(normalize(line), List("threat", "window", "delay", "immediate", "counterplay"))
          case AuthorQuestionKind.WhatChanged =>
            containsAny(normalize(line), List("opens", "pressure", "exchange", "counterplay", "improves"))
          case AuthorQuestionKind.WhatMustBeStopped =>
            containsAny(normalize(line), List("threat", "defend", "counterplay", "break", "stop"))
          case AuthorQuestionKind.WhosePlanIsFaster =>
            containsAny(normalize(line), List("window", "before", "initiative", "counterplay"))
          case _ =>
            false
      )

  private def significantTokens(text: String): Set[String] =
    normalize(text)
      .split("""[^a-z0-9]+""")
      .toList
      .filter(token => token.nonEmpty && token.length >= 4 && !ChronicleTokenStopWords.contains(token))
      .toSet

  private def distinctChronicleSentences(sentences: List[String]): List[String] =
    sentences.foldLeft(List.empty[String]) { (acc, sentence) =>
      if acc.exists(existing => sameSentence(existing, sentence)) then acc else acc :+ sentence
    }

  private def sanitizeDistinctSentence(
      textOpt: Option[String],
      ctx: NarrativeContext,
      claim: String
  ): Option[String] =
    textOpt
      .flatMap(text => cleanChronicleSentence(text, ctx))
      .filter(text => !sameSentence(text, claim))

  private def cleanChronicleSentence(
      raw: String,
      ctx: NarrativeContext
  ): Option[String] =
    BookmakerLiveCompressionPolicy.cleanNarrativeSentence(raw, ctx)
      .filterNot(isQuestionLike)
      .filterNot(isLowValueSentence)

  private def containsAny(text: String, needles: List[String]): Boolean =
    needles.exists(text.contains)

  private def sameSentence(a: String, b: String): Boolean =
    NarrativeDedupCore.sameSemanticSentence(a, b)

  private def renderSurface(ctx: NarrativeContext, beat: OutlineBeat): Option[BeatSurface] =
    val rendered = BookStyleRenderer.renderBeatForSelection(beat, ctx)
    val sentences =
      splitSentences(rendered)
        .flatMap(raw => BookmakerLiveCompressionPolicy.cleanNarrativeSentence(raw, ctx))
        .filterNot(isQuestionLike)
        .filterNot(isLowValueSentence)
        .filterNot(s => beat.branchScoped && !LineScopedCitation.hasInlineCitation(s))
    val citedLine =
      citationFromBeat(beat, rendered)
        .flatMap(raw => BookmakerLiveCompressionPolicy.cleanNarrativeSentence(raw, ctx))
        .filterNot(isLowValueSentence)
    Option.when(sentences.nonEmpty || citedLine.nonEmpty)(
      BeatSurface(
        beat = beat,
        sentences = sentences,
        citedLine = citedLine
      )
    )

  private def citedLineCandidates(
      parts: CommentaryEngine.HybridNarrativeParts,
      surfaces: List[BeatSurface]
  ): List[String] =
    val beatLines =
      surfaces
        .filter(surface =>
          surface.beat.kind == OutlineBeatKind.Evidence ||
            surface.beat.kind == OutlineBeatKind.Alternatives ||
            surface.beat.branchScoped
        )
        .flatMap(_.citedLine)
    val critical = parts.criticalBranch.toList.flatMap(normalizeCitationSentence)
    (beatLines ++ critical).distinct

  private def citationFromBeat(beat: OutlineBeat, raw: String): Option[String] =
    beat.kind match
      case OutlineBeatKind.Evidence | OutlineBeatKind.Alternatives =>
        raw.linesIterator
          .map(_.trim)
          .filter(_.nonEmpty)
          .flatMap(normalizeCitationSentence)
          .find(_.nonEmpty)
      case _ =>
        splitSentences(raw)
          .flatMap(normalizeCitationSentence)
          .headOption

  private def normalizeCitationSentence(raw: String): Option[String] =
    val trimmed = Option(raw).map(_.trim).getOrElse("")
    val unlabelled = trimmed.replaceFirst("""^[a-z]\)\s*""", "").trim
    if LineScopedCitation.hasInlineCitation(trimmed) then
      Some(trimmed.stripSuffix(".") + ".")
    else if LineScopedCitation.hasConcreteSanLine(unlabelled) then
      Some(s"Line: a) ${unlabelled.stripSuffix(".")}.")
    else None

  private def splitSentences(text: String): List[String] =
    Option(text)
      .map(_.trim)
      .filter(_.nonEmpty)
      .map(_.split("(?<=[.!?])\\s+").toList)
      .getOrElse(Nil)
      .map(_.trim)
      .filter(_.nonEmpty)

  private def isQuestionLike(text: String): Boolean =
    val trimmed = Option(text).map(_.trim).getOrElse("")
    val low = trimmed.toLowerCase
    trimmed.endsWith("?") ||
      low.startsWith("why ") ||
      low.startsWith("what ") ||
      low.startsWith("how ") ||
      low.startsWith("which ")

  private def isLowValueSentence(text: String): Boolean =
    val low = normalize(text)
    LiveNarrativeCompressionCore.isLowValueNarrativeSentence(text) ||
      low.startsWith("the strategic stack still favors") ||
      low.startsWith("the leading route is") ||
      low.startsWith("the backup strategic stack is") ||
      low.startsWith("the main signals are") ||
      low.startsWith("evidence must show") ||
      low.startsWith("current support centers on") ||
      low.startsWith("initial board read") ||
      low.startsWith("clearest read is that") ||
      low.startsWith("validation evidence specifically") ||
      low.startsWith("another key pillar is that") ||
      low.startsWith("in practical terms the split should appear") ||
      low.contains("ranked stack") ||
      low.contains("latent plan") ||
      low.contains("probe evidence says") ||
      low.contains("refutation hold")

  private def isStrategicDistributionBeat(beat: OutlineBeat): Boolean =
    beat.conceptIds.contains("strategic_distribution_first") ||
      beat.conceptIds.contains("plan_evidence_three_stage")

  private def normalize(text: String): String =
    Option(text)
      .getOrElse("")
      .replace("**", "")
      .replaceAll("""[^\p{L}\p{N}\s]""", " ")
      .replaceAll("""\s+""", " ")
      .trim
      .toLowerCase
