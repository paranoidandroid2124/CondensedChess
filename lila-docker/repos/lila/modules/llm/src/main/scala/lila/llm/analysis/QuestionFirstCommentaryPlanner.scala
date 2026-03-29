package lila.llm.analysis

import lila.llm.StrategyPack
import lila.llm.model.*
import lila.llm.model.authoring.*
import lila.llm.model.strategic.CounterfactualMatch
import scala.annotation.unused

private[llm] enum QuestionPlanFallbackMode:
  case PlannerOwned
  case DemotedToWhyThis
  case DemotedToWhatMustBeStopped
  case FactualFallback
  case Suppressed

private[llm] enum QuestionPlanStrengthTier:
  case Strong
  case Moderate
  case Exact

private[llm] enum QuestionPlanConsequenceBeat:
  case WrapUp
  case TeachingPoint

private[llm] final case class QuestionPlanEvidence(
    text: String,
    purposes: List[String],
    sourceKinds: List[String],
    sourceIds: List[String] = Nil,
    branchScoped: Boolean = false
)

private[llm] final case class QuestionPlanConsequence(
    text: String,
    beat: QuestionPlanConsequenceBeat,
    certified: Boolean = true
)

private[llm] enum SceneType:
  case ForcingDefense
  case TacticalFailure
  case QuietImprovement
  case TransitionConversion
  case PlanClash
  case OpeningRelation
  case EndgameTransition

  def wireName: String =
    this match
      case ForcingDefense      => "forcing_defense"
      case TacticalFailure     => "tactical_failure"
      case QuietImprovement    => "quiet_improvement"
      case TransitionConversion => "transition_conversion"
      case PlanClash           => "plan_clash"
      case OpeningRelation     => "opening_relation"
      case EndgameTransition   => "endgame_transition"

private[llm] enum OwnerFamily:
  case TacticalFailure
  case ForcingDefense
  case MoveDelta
  case DecisionTiming
  case PlanRace
  case OpeningRelation
  case EndgameTransition

  def wireName: String =
    this match
      case TacticalFailure   => "TacticalFailure"
      case ForcingDefense    => "ForcingDefense"
      case MoveDelta         => "MoveDelta"
      case DecisionTiming    => "DecisionTiming"
      case PlanRace          => "PlanRace"
      case OpeningRelation   => "OpeningRelation"
      case EndgameTransition => "EndgameTransition"

private[llm] final case class OwnerCandidateTrace(
    family: OwnerFamily,
    source: String,
    sourceKinds: List[String],
    questionKinds: List[AuthorQuestionKind],
    moveLinked: Boolean,
    supportMaterial: Boolean,
    proposedFamilyMapping: String,
    reasons: List[String]
):
  def key: (OwnerFamily, String) = family -> source

  def render: String =
    List(
      family.wireName,
      s"source_kind=$source",
      s"move_linked=$moveLinked",
      s"support_material=$supportMaterial",
      s"mapping=$proposedFamilyMapping",
      s"source_kinds=${sourceKinds.distinct.sorted.mkString("+")}",
      s"questions=${questionKinds.map(_.toString).distinct.sorted.mkString("+")}",
      reasons.distinct.sorted.mkString("+")
    ).filter(_.nonEmpty).mkString(":")

private[llm] final case class DroppedOwnerFamilyTrace(
    family: OwnerFamily,
    source: String,
    reasons: List[String],
    questionKinds: List[AuthorQuestionKind]
):
  def render: String =
    List(
      family.wireName,
      source,
      questionKinds.map(_.toString).distinct.sorted.mkString("+"),
      reasons.distinct.sorted.mkString("+")
    ).filter(_.nonEmpty).mkString(":")

private[llm] final case class PlannerOwnerTrace(
    sceneType: SceneType = SceneType.QuietImprovement,
    ownerCandidates: List[OwnerCandidateTrace] = Nil,
    admittedFamilies: List[OwnerCandidateTrace] = Nil,
    droppedFamilies: List[DroppedOwnerFamilyTrace] = Nil,
    demotionReasons: List[String] = Nil,
    selectedQuestion: Option[AuthorQuestionKind] = None,
    selectedOwnerFamily: Option[OwnerFamily] = None,
    selectedOwnerSource: Option[String] = None
):
  def ownerCandidateLabels: List[String] =
    ownerCandidates.map(_.render)

  def admittedFamilyLabels: List[String] =
    admittedFamilies.map(_.render)

  def droppedFamilyLabels: List[String] =
    droppedFamilies.map(_.render)

  def supportMaterialSeparationLabels: List[String] =
    ownerCandidates.map(candidate =>
      s"${candidate.source}:${if candidate.supportMaterial then "support_material" else "owner_candidate"}"
    )

  def proposedFamilyMappingLabels: List[String] =
    ownerCandidates.map(candidate =>
      s"${candidate.source}->${candidate.family.wireName}:${candidate.proposedFamilyMapping}"
    )

private[llm] final case class QuestionPlan(
    questionId: String,
    questionKind: AuthorQuestionKind,
    priority: Int,
    claim: String,
    evidence: Option[QuestionPlanEvidence],
    contrast: Option[String],
    consequence: Option[QuestionPlanConsequence],
    fallbackMode: QuestionPlanFallbackMode,
    strengthTier: QuestionPlanStrengthTier,
    sourceKinds: List[String],
    admissibilityReasons: List[String],
    ownerFamily: OwnerFamily,
    ownerSource: String,
    demotionReasons: List[String] = Nil
)

private[llm] final case class RejectedQuestionPlan(
    questionId: String,
    questionKind: AuthorQuestionKind,
    fallbackMode: QuestionPlanFallbackMode,
    reasons: List[String],
    demotedTo: Option[AuthorQuestionKind] = None,
    demotionReasons: List[String] = Nil
)

private[llm] final case class RankedQuestionPlans(
    primary: Option[QuestionPlan],
    secondary: Option[QuestionPlan],
    rejected: List[RejectedQuestionPlan],
    ownerTrace: PlannerOwnerTrace = PlannerOwnerTrace()
)

private[llm] final case class QuestionPlannerInputs(
    mainBundle: Option[MainPathClaimBundle],
    quietIntent: Option[QuietMoveIntentClaim],
    decisionFrame: CertifiedDecisionFrame,
    decisionComparison: Option[DecisionComparison],
    alternativeNarrative: Option[AlternativeNarrative],
    truthMode: PlayerFacingTruthMode,
    preventedPlansNow: List[PreventedPlanInfo],
    pvDelta: Option[PVDelta],
    counterfactual: Option[CounterfactualMatch],
    practicalAssessment: Option[PracticalInfo],
    opponentThreats: List[ThreatRow],
    forcingThreats: List[ThreatRow],
    evidenceByQuestionId: Map[String, List[QuestionEvidence]],
    candidateEvidenceLines: List[String],
    evidenceBackedPlans: List[PlanHypothesis],
    opponentPlan: Option[PlanRow],
    factualFallback: Option[String]
)

private[llm] enum WhyNowTimingOwner:
  case Threat(threat: ThreatRow)
  case Prevented(plan: PreventedPlanInfo)
  case OnlyMove(reason: String)
  case DecisionComparisonOwner(comparison: DecisionComparison)

private[llm] object QuestionPlannerInputsBuilder:

  def build(
      ctx: NarrativeContext,
      strategyPack: Option[StrategyPack],
      truthContract: Option[DecisiveTruthContract],
      candidateEvidenceLines: List[String] = Nil
  ): QuestionPlannerInputs =
    val decisionComparisonRaw = DecisionComparisonBuilder.build(ctx)
    val decisionComparison =
      truthContract
        .flatMap(contract => DecisiveTruth.sanitizeDecisionComparison(decisionComparisonRaw, contract))
        .orElse(decisionComparisonRaw)
    val authorEvidenceLines =
      ctx.authorEvidence.flatMap(_.branches).flatMap(branchDisplayLine)
    val comparisonEvidence = decisionComparison.flatMap(_.evidence).toList
    val cleanedEvidenceLines =
      (candidateEvidenceLines ++ authorEvidenceLines ++ comparisonEvidence)
        .flatMap(cleanLine)
        .distinct
    val mainBundle =
      MainPathMoveDeltaClaimBuilder.build(ctx, strategyPack, truthContract, cleanedEvidenceLines)
    val quietIntent =
      Option.when(mainBundle.isEmpty) {
        QuietMoveIntentBuilder.build(ctx, cleanedEvidenceLines)
      }.flatten
    val decisionFrame =
      CertifiedDecisionFrameBuilder.build(
        ctx = ctx,
        strategyPack = strategyPack,
        truthContract = truthContract,
        mainBundle = mainBundle,
        quietIntent = quietIntent
      )

    QuestionPlannerInputs(
      mainBundle = mainBundle,
      quietIntent = quietIntent,
      decisionFrame = decisionFrame,
      decisionComparison = decisionComparison,
      alternativeNarrative = AlternativeNarrativeSupport.build(ctx),
      truthMode = PlayerFacingTruthModePolicy.classify(ctx, strategyPack, truthContract),
      preventedPlansNow =
        ctx.semantic.toList.flatMap(_.preventedPlans).filter(_.sourceScope == FactScope.Now),
      pvDelta = ctx.decision.map(_.delta),
      counterfactual = ctx.counterfactual,
      practicalAssessment = ctx.semantic.flatMap(_.practicalAssessment),
      opponentThreats = ctx.threats.toUs,
      forcingThreats = ctx.threats.toThem,
      evidenceByQuestionId = ctx.authorEvidence.groupBy(_.questionId),
      candidateEvidenceLines = cleanedEvidenceLines,
      evidenceBackedPlans = StrategicNarrativePlanSupport.evidenceBackedMainPlans(ctx),
      opponentPlan = ctx.opponentPlan,
      factualFallback = QuietMoveIntentBuilder.exactFactualSentence(ctx)
    )

  private def branchDisplayLine(branch: EvidenceBranch): Option[String] =
    LineScopedCitation
      .evidenceBranchDisplayLine(branch)
      .orElse(cleanLine(branch.line))

  private def cleanLine(raw: String): Option[String] =
    Option(raw)
      .map(_.trim.replaceAll("\\s+", " "))
      .filter(_.nonEmpty)

private[llm] object QuestionFirstCommentaryPlanner:

  private val PlannerLinePurpose = "planner_line_proof"

  def plan(
      ctx: NarrativeContext,
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): RankedQuestionPlans =
    planInternal(
      ctx = Some(ctx),
      ply = ctx.ply,
      authorQuestions = ctx.authorQuestions,
      inputs = inputs,
      truthContract = truthContract
    )

  def plan(
      ply: Int,
      authorQuestions: List[AuthorQuestion],
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): RankedQuestionPlans =
    planInternal(
      ctx = None,
      ply = ply,
      authorQuestions = authorQuestions,
      inputs = inputs,
      truthContract = truthContract
    )

  private def planInternal(
      ctx: Option[NarrativeContext],
      ply: Int,
      authorQuestions: List[AuthorQuestion],
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): RankedQuestionPlans =
    val evaluated = authorQuestions.map(question => evaluateQuestion(question, ply, inputs, truthContract))
    val admitted = evaluated.collect { case Left(value) => value }
    val rejected = evaluated.collect { case Right(value) => value }
    val ranked = admitted.sortBy(planScore).reverse
    val primary = ranked.headOption
    val secondary = ranked.drop(1).find(plan => secondaryAllowed(primary, plan))
    val ownerTrace = buildOwnerTrace(ctx, inputs, truthContract, admitted, rejected, primary)
    RankedQuestionPlans(primary = primary, secondary = secondary, rejected = rejected, ownerTrace = ownerTrace)

  def hasConcreteWhyNowOwner(
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): Boolean =
    concreteWhyNowTimingOwner(inputs, truthContract).nonEmpty

  private def evaluateQuestion(
      question: AuthorQuestion,
      ply: Int,
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): Either[QuestionPlan, RejectedQuestionPlan] =
    question.kind match
      case AuthorQuestionKind.WhyThis =>
        buildWhyThisPlan(question, inputs, truthContract)
      case AuthorQuestionKind.WhyNow =>
        buildWhyNowPlan(question, ply, inputs, truthContract)
      case AuthorQuestionKind.WhatChanged =>
        buildWhatChangedPlan(question, inputs, truthContract)
      case AuthorQuestionKind.WhatMustBeStopped =>
        buildWhatMustBeStoppedPlan(question, ply, inputs, truthContract)
      case AuthorQuestionKind.WhosePlanIsFaster =>
        buildWhosePlanIsFasterPlan(question, ply, inputs, truthContract)

  private def planScore(plan: QuestionPlan): (Int, Int, Int, Int, Int, Int) =
    (
      strengthScore(plan.strengthTier),
      plan.contrast.fold(0)(_ => 1),
      plan.consequence.fold(0)(_ => 1),
      plan.evidence.fold(0)(evidence => evidenceQuality(evidence)),
      plan.priority,
      tacticalSeverity(plan)
    )

  private def strengthScore(tier: QuestionPlanStrengthTier): Int =
    tier match
      case QuestionPlanStrengthTier.Strong   => 3
      case QuestionPlanStrengthTier.Moderate => 2
      case QuestionPlanStrengthTier.Exact    => 1

  private def evidenceQuality(evidence: QuestionPlanEvidence): Int =
    val branchScore = countBranches(evidence.text)
    if branchScore >= 2 then 3
    else if evidence.branchScoped then 2
    else 1

  private def countBranches(text: String): Int =
    """(?m)^[a-z]\)\s+""".r.findAllMatchIn(Option(text).getOrElse("")).size

  private def tacticalSeverity(plan: QuestionPlan): Int =
    if plan.questionKind == AuthorQuestionKind.WhatMustBeStopped then 3
    else if plan.sourceKinds.exists(kind => kind.contains("tactical") || kind.contains("threat")) then 2
    else if plan.sourceKinds.exists(_.contains("prevented_plan")) then 1
    else 0

  private def secondaryAllowed(primary: Option[QuestionPlan], candidate: QuestionPlan): Boolean =
    primary.forall { head =>
      head.questionId != candidate.questionId &&
      head.questionKind != candidate.questionKind &&
      !sameText(head.claim, candidate.claim) &&
      (
        candidate.contrast.exists(text => !head.contrast.exists(sameText(_, text))) ||
          candidate.evidence.exists(e => !head.evidence.exists(existing => sameText(existing.text, e.text)))
      )
    }

  private def buildAuthorEvidence(
      question: AuthorQuestion,
      inputs: QuestionPlannerInputs
  ): Option[QuestionPlanEvidence] =
    val evidence = inputs.evidenceByQuestionId.getOrElse(question.id, Nil)
    val branches = evidence.flatMap(_.branches).distinctBy(_.line).take(3)
    val rendered =
      branches.flatMap(branch => LineScopedCitation.evidenceBranchDisplayLine(branch).map(line => branch -> line))
    Option.when(rendered.nonEmpty) {
      val labels = List("a)", "b)", "c)")
      QuestionPlanEvidence(
        text =
          rendered.zip(labels).map { case ((branch, line), label) =>
            val evalPart = branch.evalCp.map(cp => s" (${formatCp(cp)})").getOrElse("")
            s"$label $line$evalPart"
          }.mkString("\n"),
        purposes = evidence.map(_.purpose).distinct,
        sourceKinds = List("author_evidence"),
        sourceIds = branches.flatMap(_.sourceId).distinct,
        branchScoped = true
      )
    }

  private def buildSingleLineEvidence(
      text: String,
      sourceKinds: List[String]
  ): Option[QuestionPlanEvidence] =
    cleanLine(text).map { line =>
      QuestionPlanEvidence(
        text = s"a) $line",
        purposes = List(PlannerLinePurpose),
        sourceKinds = sourceKinds,
        branchScoped = true
      )
    }

  private def evidenceForQuestion(
      question: AuthorQuestion,
      fallbackLine: Option[String],
      sourceKinds: List[String]
  )(
      using inputs: QuestionPlannerInputs
  ): Option[QuestionPlanEvidence] =
    buildAuthorEvidence(question, inputs)
      .orElse {
        val needsMultiBranch =
          question.evidencePurposes.exists(p => p == "keep_tension_branches" || p == "recapture_branches")
        if needsMultiBranch then None else fallbackLine.flatMap(buildSingleLineEvidence(_, sourceKinds))
      }

  private def mkPlan(
      question: AuthorQuestion,
      kind: AuthorQuestionKind,
      claim: String,
      evidence: Option[QuestionPlanEvidence],
      contrast: Option[String],
      consequence: Option[QuestionPlanConsequence],
      fallbackMode: QuestionPlanFallbackMode,
      strengthTier: QuestionPlanStrengthTier,
      sourceKinds: List[String],
      admissibilityReasons: List[String],
      ownerFamily: OwnerFamily,
      ownerSource: String,
      demotionReasons: List[String] = Nil
  ): Either[QuestionPlan, RejectedQuestionPlan] =
    cleanLine(claim) match
      case Some(cleanClaim) =>
        Left(
          QuestionPlan(
            questionId = question.id,
            questionKind = kind,
            priority = question.priority,
            claim = ensureSentence(cleanClaim),
            evidence = evidence,
            contrast = contrast.flatMap(cleanLine).map(ensureSentence),
            consequence = consequence.map(value => value.copy(text = ensureSentence(value.text))),
            fallbackMode = fallbackMode,
            strengthTier = strengthTier,
            sourceKinds = sourceKinds.distinct,
            admissibilityReasons = admissibilityReasons.distinct,
            ownerFamily = ownerFamily,
            ownerSource = ownerSource,
            demotionReasons = demotionReasons.distinct
          )
        )
      case None =>
        reject(question, QuestionPlanFallbackMode.FactualFallback, "missing_claim")

  private def reject(
      question: AuthorQuestion,
      fallbackMode: QuestionPlanFallbackMode,
      reasons: String*
  ): Either[QuestionPlan, RejectedQuestionPlan] =
    Right(
      RejectedQuestionPlan(
        questionId = question.id,
        questionKind = question.kind,
        fallbackMode = fallbackMode,
        reasons = reasons.toList.distinct
      )
    )

  private def rejectWithDemotion(
      question: AuthorQuestion,
      fallbackMode: QuestionPlanFallbackMode,
      demotedTo: AuthorQuestionKind,
      reasons: String*
  ): RejectedQuestionPlan =
    RejectedQuestionPlan(
      questionId = question.id,
      questionKind = question.kind,
      fallbackMode = fallbackMode,
      reasons = reasons.toList.distinct,
      demotedTo = Some(demotedTo),
      demotionReasons = reasons.toList.distinct
    )

  private def sameText(a: String, b: String): Boolean =
    normalizeText(a) == normalizeText(b)

  private def normalizeText(raw: String): String =
    Option(raw).getOrElse("").toLowerCase.replaceAll("""[^a-z0-9\s]""", " ").replaceAll("\\s+", " ").trim

  private def cleanLine(raw: String): Option[String] =
    Option(raw).map(_.trim.replaceAll("\\s+", " ")).filter(_.nonEmpty)

  private def ensureSentence(raw: String): String =
    val text = Option(raw).map(_.trim).getOrElse("")
    if text.isEmpty then ""
    else if ".!?".contains(text.last) then text
    else s"$text."

  private def formatCp(cp: Int): String =
    f"${cp.toDouble / 100}%.2f"

  private def ownerSideLabel(inputs: QuestionPlannerInputs, ply: Int): String =
    inputs.decisionFrame.ownerSide
      .orElse(Option.when(ply % 2 == 0)("white").orElse(Some("black")))
      .map(_.capitalize)
      .getOrElse("The side to move")

  private def bestImmediateThreat(threats: List[ThreatRow]): Option[ThreatRow] =
    threats
      .filter(threat => threat.lossIfIgnoredCp > 0 || threat.kind.equalsIgnoreCase("mate"))
      .sortBy(threat => (-threat.lossIfIgnoredCp, threat.turnsToImpact))
      .headOption

  private def onlyMovePressure(truthContract: Option[DecisiveTruthContract]): Option[String] =
    truthContract.flatMap { contract =>
      Option.when(contract.isCriticalBestMove || contract.reasonFamily == DecisiveReasonFamily.OnlyMoveDefense) {
        contract.benchmarkMove match
          case Some(best) => s"Other moves allow the position to slip away; the benchmark move is $best"
          case None       => "Other moves allow the position to slip away"
      }
    }

  private def planAdvanceConsequence(delta: PVDelta): Option[String] =
    delta.planAdvancements.headOption.map(step => s"That changes the next phase of the position by making $step available.")

  private def resolvedThreatConsequence(delta: PVDelta): Option[String] =
    delta.resolvedThreats.headOption.map(threat => s"That removes the immediate problem of $threat.")

  private def newOpportunityConsequence(delta: PVDelta): Option[String] =
    delta.newOpportunities.headOption.map(opportunity => s"That creates a concrete follow-up around $opportunity.")

  private def preventedPlanTimingClaim(plan: PreventedPlanInfo): Option[String] =
    plan.breakNeutralized.map(file => s"The timing matters now because otherwise the $file-break becomes available.")
      .orElse(plan.preventedThreatType.map(kind => s"The timing matters now because otherwise the opponent's $kind comes to life."))
      .orElse(
        Option.when(plan.counterplayScoreDrop >= 80)(
          s"The timing matters now because drifting gives the opponent roughly ${plan.counterplayScoreDrop}cp of counterplay."
        )
      )

  private def preventedPlanTimingContrast(plan: PreventedPlanInfo): Option[String] =
    plan.citationLine.map(line => s"If delayed, the line $line becomes available again.")
      .orElse(plan.breakNeutralized.map(file => s"If delayed, the $file-break comes back into play."))
      .orElse(
        Option.when(plan.counterplayScoreDrop >= 80)(
          s"If delayed, that counterplay window reopens immediately."
        )
      )

  private def preventedPlanChangeClaim(plan: PreventedPlanInfo): Option[String] =
    plan.breakNeutralized.map(file => s"This changes the position by taking the $file-break away.")
      .orElse(plan.preventedThreatType.map(kind => s"This changes the position by taking the opponent's $kind off the table."))
      .orElse(
        Option.when(plan.counterplayScoreDrop >= 80)(
          s"This changes the position by shrinking the opponent's counterplay window by about ${plan.counterplayScoreDrop}cp."
        )
      )

  private def preventedPlanChangeContrast(plan: PreventedPlanInfo): Option[String] =
    plan.breakNeutralized.map(file => s"Before the move, the $file-break was still available.")
      .orElse(plan.preventedThreatType.map(kind => s"Before the move, the opponent's $kind was still live."))
      .orElse(
        Option.when(plan.counterplayScoreDrop >= 80)(
          "Before the move, that counterplay window was still there."
        )
      )

  private def preventedPlanChangeConsequence(plan: PreventedPlanInfo): Option[String] =
    Option.when(plan.counterplayScoreDrop > 0) {
      s"That removes roughly ${plan.counterplayScoreDrop}cp of counterplay from the next phase."
    }

  private def decisionComparisonChangeClaim(
      comparison: Option[DecisionComparison]
  ): Option[String] =
    comparison.flatMap { value =>
      value.cpLossVsChosen
        .map(math.abs)
        .filter(_ >= 60)
        .map { loss =>
          value.deferredMove
            .filter(_.trim.nonEmpty)
            .map(move => s"This changes the practical balance by keeping about ${loss}cp that would drift toward $move.")
            .orElse(
              value.deferredReason
                .filter(_.trim.nonEmpty)
                .map(reason => s"This changes the practical balance by preserving about ${loss}cp: $reason.")
            )
            .getOrElse(s"This changes the practical balance by preserving about ${loss}cp.")
        }
    }

  private def decisionComparisonChangeContrast(
      comparison: Option[DecisionComparison]
  ): Option[String] =
    comparison.flatMap { value =>
      value.deferredMove
        .filter(_.trim.nonEmpty)
        .map(move => s"Without the move, the cleaner version of the position runs through $move.")
        .orElse(
          value.deferredReason
            .filter(_.trim.nonEmpty)
            .map(reason => s"Without the move, $reason.")
        )
    }

  private def decisionComparisonChangeConsequence(
      comparison: Option[DecisionComparison]
  ): Option[String] =
    comparison.flatMap { value =>
      value.cpLossVsChosen
        .map(math.abs)
        .filter(_ >= 60)
        .map(loss => s"That keeps roughly ${loss}cp of practical value from slipping away.")
    }

  private def comparisonAlternativeMove(
      comparison: DecisionComparison
  ): Option[String] =
    comparison.deferredMove
      .filter(_.trim.nonEmpty)
      .orElse(
        comparison.engineBestMove
          .filter(_.trim.nonEmpty)
          .filter(move => comparison.chosenMove.forall(chosen => !sameText(chosen, move)))
      )

  private def buildWhyThisPlan(
      question: AuthorQuestion,
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): Either[QuestionPlan, RejectedQuestionPlan] =
    given QuestionPlannerInputs = inputs
    val ownerClaim =
      inputs.mainBundle.flatMap(_.mainClaim).map(_.claimText)
        .orElse(inputs.quietIntent.map(_.claimText))
    ownerClaim match
      case None =>
        reject(question, QuestionPlanFallbackMode.FactualFallback, "missing_move_owner")
      case Some(claim) =>
        val evidence =
          evidenceForQuestion(
            question = question,
            fallbackLine =
              inputs.mainBundle.flatMap(_.lineScopedClaim).map(_.claimText)
                .orElse(inputs.quietIntent.flatMap(_.evidenceLine)),
            sourceKinds = List("main_bundle", "quiet_intent")
          )
        val contrast =
          inputs.decisionComparison.flatMap(_.deferredMove.map(move => s"The practical alternative $move remains secondary here."))
            .orElse(inputs.alternativeNarrative.map(_.sentence))
            .orElse(onlyMovePressure(truthContract))
        val consequence =
          inputs.pvDelta
            .flatMap(delta => planAdvanceConsequence(delta).orElse(newOpportunityConsequence(delta)))
            .map(text => QuestionPlanConsequence(text, QuestionPlanConsequenceBeat.WrapUp))
            .orElse {
              inputs.counterfactual
                .filter(_.cpLoss > 0)
                .map(cf =>
                  QuestionPlanConsequence(
                    s"If the move is missed, ${cf.bestMove} becomes the cleaner continuation instead.",
                    QuestionPlanConsequenceBeat.WrapUp
                  )
                )
            }
        val strength =
          if inputs.mainBundle.flatMap(_.mainClaim).exists(_.mode == PlayerFacingTruthMode.Tactical) then
            QuestionPlanStrengthTier.Strong
          else QuestionPlanStrengthTier.Moderate
        mkPlan(
          question = question,
          kind = AuthorQuestionKind.WhyThis,
          claim = claim,
          evidence = evidence,
          contrast = contrast,
          consequence = consequence,
          fallbackMode = QuestionPlanFallbackMode.PlannerOwned,
          strengthTier = strength,
          sourceKinds =
            List(inputs.mainBundle.flatMap(_.mainClaim).map(_.sourceKind), inputs.quietIntent.map(_.sourceKind)).flatten,
          admissibilityReasons = List("move_owner", "move_local_claim"),
          ownerFamily =
            if inputs.mainBundle.flatMap(_.mainClaim).exists(_.mode == PlayerFacingTruthMode.Tactical) then OwnerFamily.TacticalFailure
            else OwnerFamily.MoveDelta,
          ownerSource =
            inputs.mainBundle.flatMap(_.mainClaim).map(_.sourceKind)
              .orElse(inputs.quietIntent.map(_.sourceKind))
              .getOrElse("move_owner")
        )

  private def buildWhyNowPlan(
      question: AuthorQuestion,
      @unused ply: Int,
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): Either[QuestionPlan, RejectedQuestionPlan] =
    given QuestionPlannerInputs = inputs
    val timingOwner = concreteWhyNowTimingOwner(inputs, truthContract)
    timingOwner match
      case None =>
        val rejected = rejectWithDemotion(
          question,
          QuestionPlanFallbackMode.DemotedToWhyThis,
          AuthorQuestionKind.WhyThis,
          "generic_urgency_only"
        )
        buildWhyThisPlan(question, inputs, truthContract) match
          case Left(plan) =>
            Left(
              plan.copy(
                fallbackMode = QuestionPlanFallbackMode.DemotedToWhyThis,
                demotionReasons = (plan.demotionReasons :+ "generic_urgency_only").distinct
              )
            )
          case Right(_)   => Right(rejected)
      case Some(owner) =>
        val claim =
          owner match
            case WhyNowTimingOwner.Threat(threat) =>
              threat.bestDefense
                .map(defense => s"The move has to happen now because otherwise $defense is demanded immediately.")
                .getOrElse(s"The move has to happen now because the opponent's ${threat.kind.toLowerCase} threat is already live.")
            case WhyNowTimingOwner.Prevented(plan) =>
              preventedPlanTimingClaim(plan).getOrElse("")
            case WhyNowTimingOwner.OnlyMove(reason) =>
              s"The timing matters now because $reason."
            case WhyNowTimingOwner.DecisionComparisonOwner(comparison) =>
              decisionComparisonTimingClaim(Some(comparison)).getOrElse("")
        val evidence =
          evidenceForQuestion(
            question = question,
            fallbackLine =
              inputs.mainBundle.flatMap(_.lineScopedClaim).map(_.claimText)
                .orElse(inputs.decisionComparison.flatMap(_.evidence))
                .orElse(inputs.quietIntent.flatMap(_.evidenceLine)),
            sourceKinds = List("timing_proof", "decision_comparison")
          )
        val contrast =
          owner match
            case WhyNowTimingOwner.Threat(threat) =>
              threat.bestDefense.map(defense => s"If White drifts, $defense is the reply.")
            case WhyNowTimingOwner.Prevented(plan) =>
              preventedPlanTimingContrast(plan)
            case WhyNowTimingOwner.OnlyMove(_) =>
              decisionComparisonTimingContrast(inputs.decisionComparison)
            case WhyNowTimingOwner.DecisionComparisonOwner(comparison) =>
              decisionComparisonTimingContrast(Some(comparison))
        val consequence =
          owner match
            case WhyNowTimingOwner.Threat(threat) =>
              Some(
                QuestionPlanConsequence(
                  s"That keeps the immediate ${threat.kind.toLowerCase} pressure from taking over.",
                  QuestionPlanConsequenceBeat.WrapUp
                )
              )
            case WhyNowTimingOwner.Prevented(plan) =>
              Option.when(plan.counterplayScoreDrop > 0)(
                QuestionPlanConsequence(
                  s"That shuts down roughly ${plan.counterplayScoreDrop}cp of counterplay before it starts.",
                  QuestionPlanConsequenceBeat.WrapUp
                )
              ).orElse(
                inputs.decisionComparison.flatMap { comparison =>
                  comparison.cpLossVsChosen.map(math.abs).filter(_ >= 60).map { loss =>
                    QuestionPlanConsequence(
                      s"That preserves roughly ${loss}cp of practical value that drifting would give back.",
                      QuestionPlanConsequenceBeat.WrapUp
                    )
                  }
                }
              )
            case WhyNowTimingOwner.OnlyMove(_) | WhyNowTimingOwner.DecisionComparisonOwner(_) =>
              inputs.decisionComparison.flatMap { comparison =>
                comparison.cpLossVsChosen.map(math.abs).filter(_ >= 60).map { loss =>
                  QuestionPlanConsequence(
                    s"That preserves roughly ${loss}cp of practical value that drifting would give back.",
                    QuestionPlanConsequenceBeat.WrapUp
                  )
                }
              }
        val sourceKinds =
          List("timing_reason") ++
            (
              owner match
                case WhyNowTimingOwner.Threat(_)                  => List("threat")
                case WhyNowTimingOwner.Prevented(_)               => List("prevented_plan")
                case WhyNowTimingOwner.OnlyMove(_)                => List("truth_contract")
                case WhyNowTimingOwner.DecisionComparisonOwner(_) => List("decision_comparison")
            )
        val strength =
          if sourceKinds.exists(kind =>
              kind == "threat" || kind == "prevented_plan" || kind == "truth_contract" || kind == "decision_comparison"
            )
          then QuestionPlanStrengthTier.Strong
          else QuestionPlanStrengthTier.Moderate
        mkPlan(
          question = question,
          kind = AuthorQuestionKind.WhyNow,
          claim = claim,
          evidence = evidence,
          contrast = contrast,
          consequence = consequence,
          fallbackMode = QuestionPlanFallbackMode.PlannerOwned,
          strengthTier = strength,
          sourceKinds = sourceKinds,
          admissibilityReasons =
            List("timing_owner", "delay_sensitive_proof") ++
              Option.when(sourceKinds.contains("decision_comparison"))("timing_loss").toList,
          ownerFamily =
            owner match
              case WhyNowTimingOwner.DecisionComparisonOwner(_) => OwnerFamily.DecisionTiming
              case _                                            => OwnerFamily.ForcingDefense,
          ownerSource =
            owner match
              case WhyNowTimingOwner.Threat(_)                  => "threat"
              case WhyNowTimingOwner.Prevented(_)               => "prevented_plan"
              case WhyNowTimingOwner.OnlyMove(_)                => "truth_contract"
              case WhyNowTimingOwner.DecisionComparisonOwner(_) => "decision_comparison"
        )

  private def concreteWhyNowTimingOwner(
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): Option[WhyNowTimingOwner] =
    val urgentThreat = bestImmediateThreat(inputs.opponentThreats)
    val preventedNamedOrBreak = inputs.preventedPlansNow.find(plan =>
      plan.breakNeutralized.exists(_.trim.nonEmpty) ||
        plan.preventedThreatType.exists(_.trim.nonEmpty)
    )
    val preventedCounterplayWindow = inputs.preventedPlansNow.find(plan =>
      !plan.breakNeutralized.exists(_.trim.nonEmpty) &&
        !plan.preventedThreatType.exists(_.trim.nonEmpty) &&
        plan.counterplayScoreDrop >= 80
    )
    urgentThreat.map(WhyNowTimingOwner.Threat.apply)
      .orElse(
        preventedNamedOrBreak
          .filter(plan => preventedPlanTimingClaim(plan).nonEmpty)
          .map(WhyNowTimingOwner.Prevented.apply)
      )
      .orElse(onlyMovePressure(truthContract).map(WhyNowTimingOwner.OnlyMove.apply))
      .orElse(
        inputs.decisionComparison
          .filter(comparison => decisionComparisonTimingClaim(Some(comparison)).nonEmpty)
          .map(WhyNowTimingOwner.DecisionComparisonOwner.apply)
      )
      .orElse(
        preventedCounterplayWindow
          .filter(plan => preventedPlanTimingClaim(plan).nonEmpty)
          .map(WhyNowTimingOwner.Prevented.apply)
      )

  private def buildWhatChangedPlan(
      question: AuthorQuestion,
      inputs: QuestionPlannerInputs,
      @unused _truthContract: Option[DecisiveTruthContract]
  ): Either[QuestionPlan, RejectedQuestionPlan] =
    given QuestionPlannerInputs = inputs
    val moveOwner = inputs.mainBundle.flatMap(_.mainClaim)
    val moveLinkedChange =
      inputs.pvDelta.flatMap { delta =>
        resolvedThreatConsequence(delta)
          .orElse(newOpportunityConsequence(delta))
          .orElse(planAdvanceConsequence(delta))
      }.orElse {
        inputs.preventedPlansNow.collectFirst(Function.unlift(preventedPlanChangeClaim))
      }.orElse {
        decisionComparisonChangeClaim(inputs.decisionComparison)
      }
    (moveOwner, moveLinkedChange) match
      case (None, None) =>
        reject(question, QuestionPlanFallbackMode.FactualFallback, "state_truth_only")
      case _ =>
        val claim =
          moveLinkedChange
            .orElse(moveOwner.map(_.claimText))
            .getOrElse("")
        val contrast =
          inputs.pvDelta.flatMap { delta =>
            delta.resolvedThreats.headOption.map(threat => s"Before the move, $threat was still on the board.")
              .orElse(delta.concessions.headOption.map(concession => s"The tradeoff is that $concession."))
          }.orElse {
            inputs.preventedPlansNow.collectFirst(Function.unlift(preventedPlanChangeContrast))
          }.orElse {
            decisionComparisonChangeContrast(inputs.decisionComparison)
          }
        val consequence =
          inputs.pvDelta.flatMap { delta =>
            planAdvanceConsequence(delta)
              .orElse(newOpportunityConsequence(delta))
              .map(text => QuestionPlanConsequence(text, QuestionPlanConsequenceBeat.WrapUp))
          }.orElse {
            inputs.preventedPlansNow.collectFirst(Function.unlift(preventedPlanChangeConsequence))
              .map(text => QuestionPlanConsequence(text, QuestionPlanConsequenceBeat.WrapUp))
          }.orElse {
            decisionComparisonChangeConsequence(inputs.decisionComparison)
              .map(text => QuestionPlanConsequence(text, QuestionPlanConsequenceBeat.WrapUp))
          }
        val sourceKinds =
          moveOwner.toList.map(_.sourceKind) ++
            inputs.pvDelta.toList.map(_ => "pv_delta") ++
            inputs.preventedPlansNow
              .find(plan =>
                preventedPlanChangeClaim(plan).nonEmpty ||
                  preventedPlanChangeContrast(plan).nonEmpty ||
                  preventedPlanChangeConsequence(plan).nonEmpty
              )
              .toList
              .map(_ => "prevented_plan") ++
            inputs.decisionComparison.toList
              .filter(comparison => decisionComparisonChangeClaim(Some(comparison)).nonEmpty)
              .map(_ => "decision_comparison")
        mkPlan(
          question = question,
          kind = AuthorQuestionKind.WhatChanged,
          claim = claim,
          evidence =
            evidenceForQuestion(
              question = question,
              fallbackLine =
                inputs.mainBundle.flatMap(_.lineScopedClaim).map(_.claimText)
                  .orElse(inputs.preventedPlansNow.flatMap(_.citationLine).find(_.trim.nonEmpty)),
              sourceKinds = List("move_delta", "prevented_plan")
            ),
          contrast = contrast,
          consequence = consequence,
          fallbackMode = QuestionPlanFallbackMode.PlannerOwned,
          strengthTier = QuestionPlanStrengthTier.Strong,
          sourceKinds = sourceKinds,
          admissibilityReasons = List("move_attributed_change"),
          ownerFamily =
            if moveOwner.exists(_.mode == PlayerFacingTruthMode.Tactical) then OwnerFamily.TacticalFailure
            else if inputs.decisionComparison.exists(comparison => decisionComparisonChangeClaim(Some(comparison)).nonEmpty) &&
              inputs.pvDelta.isEmpty &&
              inputs.preventedPlansNow.forall(plan => preventedPlanChangeClaim(plan).isEmpty)
            then OwnerFamily.DecisionTiming
            else if inputs.preventedPlansNow.exists(plan => preventedPlanChangeClaim(plan).nonEmpty) &&
              inputs.pvDelta.isEmpty &&
              moveOwner.isEmpty
            then OwnerFamily.ForcingDefense
            else OwnerFamily.MoveDelta,
          ownerSource =
            if moveOwner.exists(_.mode == PlayerFacingTruthMode.Tactical) then
              moveOwner.map(_.sourceKind).getOrElse("move_delta")
            else if inputs.pvDelta.nonEmpty then "pv_delta"
            else if inputs.preventedPlansNow.exists(plan => preventedPlanChangeClaim(plan).nonEmpty) then "prevented_plan"
            else if inputs.decisionComparison.exists(comparison => decisionComparisonChangeClaim(Some(comparison)).nonEmpty) then
              "decision_comparison"
            else moveOwner.map(_.sourceKind).getOrElse("move_delta")
        )

  private def decisionComparisonTimingClaim(
      comparison: Option[DecisionComparison]
  ): Option[String] =
    comparison.flatMap { value =>
      value.cpLossVsChosen
        .map(math.abs)
        .filter(_ >= 60)
        .map { loss =>
          comparisonAlternativeMove(value)
            .map(move => s"The timing matters now because drifting lets $move take over and costs about ${loss}cp.")
            .orElse(
              value.deferredReason
                .filter(_.trim.nonEmpty)
                .map(reason => s"The timing matters now because delaying costs about ${loss}cp: $reason.")
            )
            .getOrElse(s"The timing matters now because delaying costs about ${loss}cp.")
        }
    }

  private def decisionComparisonTimingContrast(
      comparison: Option[DecisionComparison]
  ): Option[String] =
    comparison.flatMap { value =>
      value.deferredReason
        .filter(_.trim.nonEmpty)
        .map(reason => s"If delayed, $reason.")
        .orElse(
          comparisonAlternativeMove(value)
            .map(move => s"If delayed, the cleaner version runs through $move.")
        )
    }

  private def buildWhatMustBeStoppedPlan(
      question: AuthorQuestion,
      @unused ply: Int,
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): Either[QuestionPlan, RejectedQuestionPlan] =
    given QuestionPlannerInputs = inputs
    val urgentThreat = bestImmediateThreat(inputs.opponentThreats)
    val preventedNow = inputs.preventedPlansNow.find(plan =>
      plan.counterplayScoreDrop > 0 ||
        plan.breakNeutralized.exists(_.trim.nonEmpty) ||
        plan.preventedThreatType.exists(_.trim.nonEmpty)
    )
    val claim =
      urgentThreat.map { threat =>
        s"This has to stop the opponent's ${threat.kind.toLowerCase} threat before it lands."
      }.orElse {
        preventedNow.flatMap { plan =>
          plan.breakNeutralized.map(file => s"This has to stop the opponent's $file-break before it starts.")
            .orElse(plan.preventedThreatType.map(kind => s"This has to stop the opponent's $kind before it becomes concrete."))
            .orElse(Option.when(plan.counterplayScoreDrop > 0)("This has to stop the opponent's easiest counterplay before it grows."))
        }
      }
    claim match
      case None =>
        val rejected = rejectWithDemotion(
          question,
          QuestionPlanFallbackMode.DemotedToWhyThis,
          AuthorQuestionKind.WhyThis,
          "generic_opponent_plan_only"
        )
        buildWhyThisPlan(question, inputs, truthContract) match
          case Left(plan) =>
            Left(
              plan.copy(
                fallbackMode = QuestionPlanFallbackMode.DemotedToWhyThis,
                demotionReasons = (plan.demotionReasons :+ "generic_opponent_plan_only").distinct
              )
            )
          case Right(_)   => Right(rejected)
      case Some(text) =>
        val contrast =
          urgentThreat.flatMap(_.bestDefense.map(defense => s"If the move is missed, $defense is forced."))
            .orElse(preventedNow.flatMap(_.citationLine.map(line => s"If the move is missed, the line $line comes back.")))
        val consequence =
          preventedNow.flatMap { plan =>
            Option.when(plan.counterplayScoreDrop > 0)(
              QuestionPlanConsequence(
                s"That keeps roughly ${plan.counterplayScoreDrop}cp of counterplay from appearing.",
                QuestionPlanConsequenceBeat.WrapUp
              )
            )
          }
        mkPlan(
          question = question,
          kind = AuthorQuestionKind.WhatMustBeStopped,
          claim = text,
          evidence =
            evidenceForQuestion(
              question = question,
              fallbackLine =
                preventedNow.flatMap(_.citationLine)
                  .orElse(inputs.mainBundle.flatMap(_.lineScopedClaim).map(_.claimText)),
              sourceKinds = List("threat", "prevented_plan")
            ),
          contrast = contrast,
          consequence = consequence,
          fallbackMode = QuestionPlanFallbackMode.PlannerOwned,
          strengthTier = QuestionPlanStrengthTier.Strong,
          sourceKinds =
            urgentThreat.toList.map(_ => "threat") ++ preventedNow.toList.map(_ => "prevented_plan"),
          admissibilityReasons = List("defensive_owner", "loss_if_ignored"),
          ownerFamily = OwnerFamily.ForcingDefense,
          ownerSource =
            if urgentThreat.nonEmpty then "threat" else "prevented_plan"
        )

  private def buildWhosePlanIsFasterPlan(
      question: AuthorQuestion,
      ply: Int,
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): Either[QuestionPlan, RejectedQuestionPlan] =
    given QuestionPlannerInputs = inputs
    val owner = ownerSideLabel(inputs, ply)
    val intent =
      inputs.decisionFrame.intent.orElse(evidenceBackedRaceIntent(owner, inputs))
    val battlefront = inputs.decisionFrame.battlefront
    val opponentPlan = inputs.opponentPlan.filter(plan => cleanLine(plan.name).nonEmpty)
    val opponentRace =
      opponentPlan.map(_.name)
        .orElse(
          bestImmediateThreat(inputs.opponentThreats).map(threat => s"the ${threat.kind.toLowerCase} threat")
        )
        .orElse(
          inputs.preventedPlansNow.collectFirst {
            case plan if plan.breakNeutralized.exists(_.trim.nonEmpty) =>
              s"the ${plan.breakNeutralized.get}-break"
          }
        )
    val urgencyRaceAnchor =
      opponentRace.flatMap { race =>
        concreteRaceUrgency(inputs).map(anchor => s"$anchor before $race gets fully rolling")
      }
    val raceAnchor =
      bestImmediateThreat(inputs.opponentThreats)
        .map(threat => s"the reply window is short against the ${threat.kind.toLowerCase} threat")
        .orElse(
          inputs.preventedPlansNow.collectFirst {
            case plan if plan.breakNeutralized.exists(_.trim.nonEmpty) =>
              s"the ${plan.breakNeutralized.get}-break is the timing window"
          }
        )
        .orElse(urgencyRaceAnchor)
    if intent.isEmpty || battlefront.isEmpty || opponentRace.isEmpty || raceAnchor.isEmpty then
      val onlyOpponentPressure = opponentRace.nonEmpty && (bestImmediateThreat(inputs.opponentThreats).nonEmpty || inputs.preventedPlansNow.nonEmpty)
      val onlyOurPlan = intent.nonEmpty || battlefront.nonEmpty || inputs.evidenceBackedPlans.nonEmpty
      if onlyOpponentPressure then
        val rejected = rejectWithDemotion(
          question,
          QuestionPlanFallbackMode.DemotedToWhatMustBeStopped,
          AuthorQuestionKind.WhatMustBeStopped,
          "missing_certified_race_pair"
        )
        buildWhatMustBeStoppedPlan(question, ply, inputs, truthContract) match
          case Left(plan) =>
            Left(
              plan.copy(
                fallbackMode = QuestionPlanFallbackMode.DemotedToWhatMustBeStopped,
                demotionReasons = (plan.demotionReasons :+ "missing_certified_race_pair").distinct
              )
            )
          case Right(_)   => Right(rejected)
      else if onlyOurPlan then
        val rejected = rejectWithDemotion(
          question,
          QuestionPlanFallbackMode.DemotedToWhyThis,
          AuthorQuestionKind.WhyThis,
          "single_sided_plan_only"
        )
        buildWhyThisPlan(question, inputs, truthContract) match
          case Left(plan) =>
            Left(
              plan.copy(
                fallbackMode = QuestionPlanFallbackMode.DemotedToWhyThis,
                demotionReasons = (plan.demotionReasons :+ "single_sided_plan_only").distinct
              )
            )
          case Right(_)   => Right(rejected)
      else reject(question, QuestionPlanFallbackMode.FactualFallback, "missing_certified_race_pair")
    else
      val intentReason =
        if inputs.decisionFrame.intent.nonEmpty then "certified_intent"
        else "probe_backed_plan_intent"
      mkPlan(
        question = question,
        kind = AuthorQuestionKind.WhosePlanIsFaster,
        claim = s"$owner keeps the initiative ahead of ${opponentRace.get} because ${raceAnchor.get}.",
        evidence =
          evidenceForQuestion(
            question = question,
            fallbackLine = battlefront.map(_.sentence).orElse(intent.map(_.sentence)),
            sourceKinds = List(intent.get.sourceKind, battlefront.get.sourceKind)
          ),
        contrast = Some(s"${owner}'s plan is racing ${opponentRace.get}, and the battlefront is ${battlefront.get.sentence}"),
        consequence = Some(
          QuestionPlanConsequence(
            s"That preserves ${owner.toLowerCase}'s plan window before ${opponentRace.get} catches up.",
            QuestionPlanConsequenceBeat.WrapUp
          )
        ),
        fallbackMode = QuestionPlanFallbackMode.PlannerOwned,
        strengthTier = QuestionPlanStrengthTier.Moderate,
        sourceKinds = List(intent.get.sourceKind, battlefront.get.sourceKind, if opponentPlan.nonEmpty then "opponent_plan" else "opponent_threat"),
        admissibilityReasons = List(intentReason, "certified_battlefront", "timing_anchor"),
        ownerFamily = OwnerFamily.PlanRace,
        ownerSource = intent.get.sourceKind
      )

  private val EndgameTransitionPattern = raw"(.+)\((.+)\)\s*→\s*(.+)\((.+)\)".r

  private def buildOwnerTrace(
      ctx: Option[NarrativeContext],
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract],
      admitted: List[QuestionPlan],
      rejected: List[RejectedQuestionPlan],
      primary: Option[QuestionPlan]
  ): PlannerOwnerTrace =
    val rawCandidates = rawOwnerCandidates(ctx, inputs, truthContract)
    val admittedFamilies =
      admitted
        .groupBy(plan => plan.ownerFamily -> plan.ownerSource)
        .toList
        .sortBy { case ((family, source), _) => (family.wireName, source) }
        .map { case ((family, source), plans) =>
          OwnerCandidateTrace(
            family = family,
            source = source,
            sourceKinds = plans.flatMap(_.sourceKinds).distinct.sorted,
            questionKinds = plans.map(_.questionKind).distinct.sortBy(_.toString),
            moveLinked = true,
            supportMaterial = false,
            proposedFamilyMapping = s"${family.wireName}/owner_candidate",
            reasons = (plans.flatMap(_.admissibilityReasons) ++ plans.flatMap(_.demotionReasons)).distinct.sorted
          )
        }
    val admittedKeys = admittedFamilies.map(_.key).toSet
    val droppedFamilies =
      rawCandidates
        .filterNot(candidate => admittedKeys.contains(candidate.key))
        .map { candidate =>
          val relatedRejected =
            rejected.filter(rejectedPlan => candidate.questionKinds.contains(rejectedPlan.questionKind))
          val reasons =
            (relatedRejected.flatMap(_.reasons) ++
              relatedRejected.flatMap(_.demotionReasons) ++
              candidate.reasons).distinct.sorted
          DroppedOwnerFamilyTrace(
            family = candidate.family,
            source = candidate.source,
            reasons = if reasons.nonEmpty then reasons else List("not_admitted"),
            questionKinds = relatedRejected.map(_.questionKind).distinct.sortBy(_.toString)
          )
        }
    PlannerOwnerTrace(
      sceneType = classifyScene(inputs, truthContract, rawCandidates),
      ownerCandidates = rawCandidates,
      admittedFamilies = admittedFamilies,
      droppedFamilies = droppedFamilies,
      demotionReasons =
        (admitted.flatMap(_.demotionReasons) ++ rejected.flatMap(_.demotionReasons)).distinct.sorted,
      selectedQuestion = primary.map(_.questionKind),
      selectedOwnerFamily = primary.map(_.ownerFamily),
      selectedOwnerSource = primary.map(_.ownerSource)
    )

  private def rawOwnerCandidates(
      ctx: Option[NarrativeContext],
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): List[OwnerCandidateTrace] =
    val tacticalFailure =
      Option.when(
        truthContract.exists(contract =>
          contract.truthClass == DecisiveTruthClass.Blunder ||
            contract.truthClass == DecisiveTruthClass.MissedWin ||
            contract.reasonFamily == DecisiveReasonFamily.TacticalRefutation ||
            contract.reasonFamily == DecisiveReasonFamily.MissedWin
        ) ||
          inputs.mainBundle.flatMap(_.mainClaim).exists(_.mode == PlayerFacingTruthMode.Tactical)
      ) {
        OwnerCandidateTrace(
          family = OwnerFamily.TacticalFailure,
          source = truthContract.map(_ => "truth_contract").getOrElse("main_bundle"),
          sourceKinds = List(truthContract.map(_ => "truth_contract").getOrElse("main_bundle")),
          questionKinds = List(AuthorQuestionKind.WhyThis, AuthorQuestionKind.WhatChanged),
          moveLinked = true,
          supportMaterial = false,
          proposedFamilyMapping = "TacticalFailure/move_linked",
          reasons = List("tactical_failure_signal")
        )
      }.toList

    val forcingDefense =
      List(
        bestImmediateThreat(inputs.opponentThreats).map { _ =>
          OwnerCandidateTrace(
            family = OwnerFamily.ForcingDefense,
            source = "threat",
            sourceKinds = List("threat"),
            questionKinds = List(AuthorQuestionKind.WhyNow, AuthorQuestionKind.WhatMustBeStopped),
            moveLinked = true,
            supportMaterial = false,
            proposedFamilyMapping = "ForcingDefense/move_linked",
            reasons = List("urgent_threat")
          )
        },
        inputs.preventedPlansNow
          .find(plan =>
            preventedPlanTimingClaim(plan).nonEmpty ||
              preventedPlanChangeClaim(plan).nonEmpty
          )
          .map { _ =>
            OwnerCandidateTrace(
              family = OwnerFamily.ForcingDefense,
              source = "prevented_plan",
              sourceKinds = List("prevented_plan"),
              questionKinds = List(
                AuthorQuestionKind.WhyNow,
                AuthorQuestionKind.WhatChanged,
                AuthorQuestionKind.WhatMustBeStopped
              ),
              moveLinked = true,
              supportMaterial = false,
              proposedFamilyMapping = "ForcingDefense/move_linked",
              reasons = List("prevented_resource")
            )
          },
        onlyMovePressure(truthContract).map { _ =>
          OwnerCandidateTrace(
            family = OwnerFamily.ForcingDefense,
            source = "truth_contract",
            sourceKinds = List("truth_contract"),
            questionKinds = List(AuthorQuestionKind.WhyNow),
            moveLinked = true,
            supportMaterial = false,
            proposedFamilyMapping = "ForcingDefense/move_linked",
            reasons = List("only_move_defense")
          )
        }
      ).flatten

    val moveDelta =
      List(
        inputs.mainBundle.flatMap(_.mainClaim).map { claim =>
          OwnerCandidateTrace(
            family =
              if claim.mode == PlayerFacingTruthMode.Tactical then OwnerFamily.TacticalFailure
              else OwnerFamily.MoveDelta,
            source = claim.sourceKind,
            sourceKinds = List(claim.sourceKind),
            questionKinds = List(AuthorQuestionKind.WhyThis, AuthorQuestionKind.WhatChanged),
            moveLinked = true,
            supportMaterial = false,
            proposedFamilyMapping =
              if claim.mode == PlayerFacingTruthMode.Tactical then "TacticalFailure/move_linked"
              else "MoveDelta/move_linked",
            reasons = List("main_move_claim")
          )
        },
        inputs.quietIntent.map { intent =>
          OwnerCandidateTrace(
            family = OwnerFamily.MoveDelta,
            source = intent.sourceKind,
            sourceKinds = List(intent.sourceKind),
            questionKinds = List(AuthorQuestionKind.WhyThis, AuthorQuestionKind.WhatChanged),
            moveLinked = true,
            supportMaterial = false,
            proposedFamilyMapping = "MoveDelta/move_linked",
            reasons = List("quiet_move_claim")
          )
        },
        inputs.pvDelta.map { _ =>
          OwnerCandidateTrace(
            family = OwnerFamily.MoveDelta,
            source = "pv_delta",
            sourceKinds = List("pv_delta"),
            questionKinds = List(AuthorQuestionKind.WhatChanged),
            moveLinked = true,
            supportMaterial = false,
            proposedFamilyMapping = "MoveDelta/move_linked",
            reasons = List("move_local_delta")
          )
        }
      ).flatten

    val decisionTiming =
      Option.when(
        decisionComparisonTimingClaim(inputs.decisionComparison).nonEmpty ||
          decisionComparisonChangeClaim(inputs.decisionComparison).nonEmpty
      ) {
        OwnerCandidateTrace(
          family = OwnerFamily.DecisionTiming,
          source = "decision_comparison",
          sourceKinds = List("decision_comparison"),
          questionKinds = List(AuthorQuestionKind.WhyNow, AuthorQuestionKind.WhatChanged),
          moveLinked = true,
          supportMaterial = false,
          proposedFamilyMapping = "DecisionTiming/move_linked",
          reasons = List("timing_loss")
        )
      }.toList

    val planRace =
      Option.when(hasPlanRaceCandidate(inputs)) {
        OwnerCandidateTrace(
          family = OwnerFamily.PlanRace,
          source =
            inputs.decisionFrame.intent.map(_.sourceKind)
              .orElse(inputs.decisionFrame.battlefront.map(_.sourceKind))
              .orElse(inputs.evidenceBackedPlans.headOption.map(_ => "evidence_backed_plan"))
              .getOrElse("plan_race"),
          sourceKinds =
            List(
              inputs.decisionFrame.intent.map(_.sourceKind),
              inputs.decisionFrame.battlefront.map(_.sourceKind),
              Option.when(inputs.opponentPlan.nonEmpty)("opponent_plan")
            ).flatten.distinct,
          questionKinds = List(AuthorQuestionKind.WhosePlanIsFaster),
          moveLinked = true,
          supportMaterial = false,
          proposedFamilyMapping = "PlanRace/move_linked",
          reasons = List("certified_plan_race")
        )
      }.toList

    val domainShadowSignals = shadowDomainSignals(ctx, inputs)

    (tacticalFailure ++ forcingDefense ++ moveDelta ++ decisionTiming ++ planRace ++ domainShadowSignals)
      .groupBy(_.key)
      .toList
      .sortBy { case ((family, source), _) => (family.wireName, source) }
      .map { case (_, traces) =>
        traces.reduce { (left, right) =>
          left.copy(
            sourceKinds = (left.sourceKinds ++ right.sourceKinds).distinct.sorted,
            questionKinds = (left.questionKinds ++ right.questionKinds).distinct.sortBy(_.toString),
            supportMaterial = left.supportMaterial && right.supportMaterial,
            reasons = (left.reasons ++ right.reasons).distinct.sorted
          )
        }
      }

  private def shadowDomainSignals(
      ctx: Option[NarrativeContext],
      inputs: QuestionPlannerInputs
  ): List[OwnerCandidateTrace] =
    val decisionComparisonSupport =
      inputs.alternativeNarrative
        .filter(_.source == "close_candidate")
        .map { _ =>
          OwnerCandidateTrace(
            family = OwnerFamily.DecisionTiming,
            source = "close_candidate",
            sourceKinds = List("alternative_narrative", "close_candidate"),
            questionKinds = List(
              AuthorQuestionKind.WhyThis,
              AuthorQuestionKind.WhyNow,
              AuthorQuestionKind.WhatChanged
            ),
            moveLinked = false,
            supportMaterial = true,
            proposedFamilyMapping = "DecisionTiming/support_only",
            reasons = List("raw_close_alternative")
          )
        }
        .toList

    val openingSignals =
      ctx.toList.flatMap { narrativeCtx =>
        val relation =
          OpeningPrecedentBranching.relationSentence(narrativeCtx, narrativeCtx.openingData, requireFocus = false)
        val summary =
          OpeningPrecedentBranching.summarySentence(narrativeCtx, narrativeCtx.openingData, requireFocus = false)
        List(
          relation.map { _ =>
            OwnerCandidateTrace(
              family = OwnerFamily.OpeningRelation,
              source = "opening_relation_translator",
              sourceKinds = List("opening_relation_translator"),
              questionKinds = List(AuthorQuestionKind.WhyThis, AuthorQuestionKind.WhatChanged),
              moveLinked = true,
              supportMaterial = false,
              proposedFamilyMapping = "OpeningRelation/move_linked",
              reasons = List("opening_relation_translated")
            )
          },
          summary.map { _ =>
            OwnerCandidateTrace(
              family = OwnerFamily.OpeningRelation,
              source = "opening_precedent_summary",
              sourceKinds = List("opening_precedent_summary"),
              questionKinds = List(AuthorQuestionKind.WhyThis, AuthorQuestionKind.WhatChanged),
              moveLinked = false,
              supportMaterial = true,
              proposedFamilyMapping = "OpeningRelation/support_only",
              reasons = List("raw_opening_precedent_summary")
            )
          }
        ).flatten
      }

    val endgameSignals =
      ctx.toList.flatMap { narrativeCtx =>
        narrativeCtx.semantic.flatMap(_.endgameFeatures).toList.flatMap { info =>
          List(
            endgameTransitionSentence(info).map { _ =>
              OwnerCandidateTrace(
                family = OwnerFamily.EndgameTransition,
                source = "endgame_transition_translator",
                sourceKinds = List("endgame_transition_translator"),
                questionKinds = List(AuthorQuestionKind.WhyThis, AuthorQuestionKind.WhatChanged),
                moveLinked = true,
                supportMaterial = false,
                proposedFamilyMapping = "EndgameTransition/move_linked",
                reasons = List("endgame_transition_translated")
              )
            },
            rawEndgameHint(info).map { _ =>
              OwnerCandidateTrace(
                family = OwnerFamily.EndgameTransition,
                source = "endgame_theoretical_hint",
                sourceKinds = List("endgame_theoretical_hint", "endgame_oracle"),
                questionKinds = List(AuthorQuestionKind.WhyThis, AuthorQuestionKind.WhatChanged),
                moveLinked = false,
                supportMaterial = true,
                proposedFamilyMapping = "EndgameTransition/support_only",
                reasons = List("raw_endgame_hint")
              )
            }
          ).flatten
        }
      }

    decisionComparisonSupport ++ openingSignals ++ endgameSignals

  private def rawEndgameHint(info: EndgameInfo): Option[String] =
    cleanLine(info.theoreticalOutcomeHint)
      .filterNot(_.equalsIgnoreCase("unclear"))
      .orElse(info.primaryPattern.flatMap(cleanLine))
      .orElse(info.transition.flatMap(cleanLine))

  private def endgameTransitionSentence(info: EndgameInfo): Option[String] =
    info.transition.flatMap {
      case EndgameTransitionPattern(fromRaw, fromHintRaw, toRaw, toHintRaw) =>
        val fromLabel = humanizeEndgamePattern(fromRaw)
        val toLabel = humanizeEndgamePattern(toRaw)
        val fromTask = endgameTaskPhrase(fromHintRaw)
        val toTask = endgameTaskPhrase(toHintRaw)
        Some(
          if fromRaw.equalsIgnoreCase("none") then
            s"A new $toLabel pattern has emerged, giving the position a clearer $toTask."
          else if toRaw.equalsIgnoreCase("none") then
            s"The $fromLabel pattern has dissolved, so the earlier $fromTask no longer holds automatically."
          else
            s"The endgame geometry has shifted from $fromLabel to $toLabel, turning the position from a $fromTask into a $toTask."
        )
      case _ => None
    }.orElse {
      info.primaryPattern.flatMap { pattern =>
        Option.when(info.patternAge >= 2) {
          val label = humanizeEndgamePattern(pattern)
          val task = endgameTaskPhrase(info.theoreticalOutcomeHint)
          s"The $label structure has held, so the same $task remains in force."
        }
      }
    }

  private def humanizeEndgamePattern(raw: String): String =
    val normalized = Option(raw).getOrElse("").trim
    if normalized.isEmpty then "endgame pattern"
    else if normalized.equalsIgnoreCase("none") then "no stable endgame pattern"
    else
      normalized
        .replaceAll("([a-z0-9])([A-Z])", "$1 $2")
        .replace('_', ' ')

  private def endgameTaskPhrase(rawHint: String): String =
    Option(rawHint).getOrElse("").trim.toLowerCase match
      case "win"     => "winning method"
      case "draw"    => "drawing setup"
      case "unclear" => "technical plan"
      case other if other.nonEmpty => s"${other} technical task"
      case _ => "technical plan"

  private def hasPlanRaceCandidate(inputs: QuestionPlannerInputs): Boolean =
    val opponentRaceAvailable =
      inputs.opponentPlan.exists(plan => cleanLine(plan.name).nonEmpty) ||
        bestImmediateThreat(inputs.opponentThreats).nonEmpty ||
        inputs.preventedPlansNow.exists(_.breakNeutralized.exists(_.trim.nonEmpty))
    val ownRaceAvailable =
      inputs.decisionFrame.intent.nonEmpty ||
        (inputs.decisionFrame.battlefront.nonEmpty && inputs.evidenceBackedPlans.nonEmpty)
    val timingAnchorAvailable = concreteRaceUrgency(inputs).nonEmpty || bestImmediateThreat(inputs.opponentThreats).nonEmpty
    ownRaceAvailable && opponentRaceAvailable && timingAnchorAvailable

  private def classifyScene(
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract],
      candidates: List[OwnerCandidateTrace]
  ): SceneType =
    val families = candidates.filterNot(_.supportMaterial).map(_.family).toSet
    if families.contains(OwnerFamily.OpeningRelation) then SceneType.OpeningRelation
    else if families.contains(OwnerFamily.EndgameTransition) then SceneType.EndgameTransition
    else if families.contains(OwnerFamily.TacticalFailure) then SceneType.TacticalFailure
    else if families.contains(OwnerFamily.ForcingDefense) then SceneType.ForcingDefense
    else if families.contains(OwnerFamily.PlanRace) then SceneType.PlanClash
    else if truthContract.exists(_.reasonFamily == DecisiveReasonFamily.Conversion) ||
      inputs.pvDelta.exists(delta =>
        delta.resolvedThreats.nonEmpty || delta.newOpportunities.nonEmpty || delta.planAdvancements.nonEmpty
      )
    then SceneType.TransitionConversion
    else SceneType.QuietImprovement

  private def evidenceBackedRaceIntent(
      owner: String,
      inputs: QuestionPlannerInputs
  ): Option[CertifiedDecisionSupport] =
    inputs.evidenceBackedPlans
      .sortBy(plan => (-plan.score, plan.rank))
      .collectFirst(Function.unlift { plan =>
        cleanLine(plan.planName)
          .filter(isConcreteRacePlanName)
          .map(name =>
            CertifiedDecisionSupport(
              axis = CertifiedDecisionFrameAxis.Intent,
              sentence = s"$owner is playing for ${name.toLowerCase}.",
              priority = 78,
              sourceKind = "evidence_backed_plan"
            )
          )
      })

  private def concreteRaceUrgency(inputs: QuestionPlannerInputs): Option[String] =
    inputs.decisionFrame.urgency
      .filterNot(_.sourceKind == "slow_truth_mode")
      .map { urgency =>
        if urgency.sourceKind == "tactical_truth_mode" then "the tactical window is short"
        else "the timing window is short"
      }
      .orElse {
        inputs.practicalAssessment
          .filter(_.biasFactors.nonEmpty)
          .map(_ => "the practical window is short")
      }

  private def isConcreteRacePlanName(name: String): Boolean =
    val low = name.toLowerCase
    low.length >= 8 &&
    !List(
      "plan",
      "idea",
      "improvement",
      "development",
      "play"
    ).contains(low)
