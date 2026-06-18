package lila.commentary.analysis.practical

import lila.commentary.analysis.*
import lila.commentary.model.*
import lila.commentary.model.authoring.AuthorQuestionKind

private[commentary] object ContrastiveSupportAdmissibility:

  object SourceKind:
    val ExplicitAlternativeCollapse = "explicit_alternative_collapse"
    val PreventedResource = "prevented_resource"
    val DelayedOnlyMove = "delayed_only_move"
    val ExplicitReplyLoss = "explicit_reply_loss"
    val TopEngineMoveWithConcreteConsequence = "top_engine_move_with_concrete_consequence"
    val PlayedMoveWithCertifiedLocalConsequence = "played_move_with_certified_local_consequence"
    val RoleAwareLineConsequence = "role_aware_line_consequence"
    val CounterfactualCausalThreat = "counterfactual_causal_threat"

  object RejectReason:
    val QuestionOutsideScope = "question_outside_scope"
    val MissingContrastCandidate = "missing_contrast_candidate"
    val RawCloseCandidate = "raw_close_candidate"
    val VagueEnginePreference = "vague_engine_preference"
    val ExplanationFreeEvalGap = "explanation_free_eval_gap"
    val MissingConcreteConsequence = "missing_concrete_consequence"
    val PreventedResourceWithoutAnchor = "prevented_resource_without_anchor"
    val OnlyMoveWithoutConcreteConsequence = "only_move_without_concrete_consequence"
    val LineConditionedGeneralization = "line_conditioned_generalization"

  final case class ContrastSupportTrace(
      contrast_source_kind: Option[String] = None,
      contrast_anchor: Option[String] = None,
      contrast_consequence: Option[String] = None,
      contrast_admissible: Boolean = false,
      contrast_reject_reason: Option[String] = None,
      contrast_sentence: Option[String] = None,
      contrast_forced_reply: Boolean = false,
      contrast_evidence_refs: List[String] = Nil,
      contrast_guardrails: List[String] = Nil
  ):
    def effectiveSupport(existingContrast: Option[String]): Option[String] =
      if contrast_admissible then contrast_sentence.orElse(existingContrast)
      else existingContrast

  def decide(
      primary: QuestionPlan,
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): ContrastSupportTrace =
    primary.questionKind match
      case AuthorQuestionKind.WhyThis => decideWhyThis(primary, inputs, truthContract)
      case AuthorQuestionKind.WhyNow  => decideWhyNow(primary, inputs, truthContract)
      case AuthorQuestionKind.WhatChanged => decideWhatChanged(primary, inputs, truthContract)
      case AuthorQuestionKind.WhatMustBeStopped => decideWhatMustBeStopped(primary, inputs)
      case _                          => reject(RejectReason.QuestionOutsideScope)

  private def decideWhyThis(
      primary: QuestionPlan,
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): ContrastSupportTrace =
    inputs.decisionComparison
      .map(decideDecisionComparison(primary, inputs, _, allowTiming = false, truthContract))
      .getOrElse {
        truthContract
          .filter(contract =>
            contract.isCriticalBestMove || contract.reasonFamily == DecisiveReasonKind.OnlyMoveDefense
          )
          .flatMap(onlyMoveContrast)
          .map { case (anchor, consequence, sentence) =>
            allow(SourceKind.DelayedOnlyMove, anchor, consequence, sentence)
          }
          .orElse(localFactWhyThisSupport(primary, inputs, comparison = None, allowTiming = false, truthContract))
          .getOrElse(reject(RejectReason.MissingContrastCandidate))
      }

  private def decideWhyNow(
      primary: QuestionPlan,
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): ContrastSupportTrace =
    whyNowContrastForPrimarySource(primary, inputs, truthContract)
      .getOrElse(reject(RejectReason.MissingContrastCandidate))

  private def decideWhatMustBeStopped(
      primary: QuestionPlan,
      inputs: QuestionPlannerInputs
  ): ContrastSupportTrace =
    primary.plannerSource match
      case "threat" =>
        bestImmediateThreat(inputs.opponentThreats)
          .flatMap(threatContrast)
          .getOrElse(reject(RejectReason.MissingContrastCandidate))
      case "prevented_plan" =>
        inputs.preventedPlansNow.iterator
          .map(preventedResourceContrast)
          .collectFirst(Function.unlift(identity))
          .getOrElse(reject(RejectReason.MissingContrastCandidate))
      case _ =>
        reject(RejectReason.MissingContrastCandidate)

  private def decideWhatChanged(
      primary: QuestionPlan,
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): ContrastSupportTrace =
    if primary.plannerSource == "decision_comparison" || primary.sourceKinds.exists(sameText(_, "decision_comparison")) then
      inputs.decisionComparison
        .map(decideDecisionComparison(primary, inputs, _, allowTiming = false, truthContract))
        .getOrElse(reject(RejectReason.MissingContrastCandidate))
    else reject(RejectReason.QuestionOutsideScope)

  private def whyNowContrastForPrimarySource(
      primary: QuestionPlan,
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): Option[ContrastSupportTrace] =
    primary.plannerSource match
      case "threat" =>
        bestImmediateThreat(inputs.opponentThreats).flatMap(threatContrast)
      case "prevented_plan" =>
        inputs.preventedPlansNow.iterator.map(preventedResourceContrast).collectFirst(Function.unlift(identity))
      case "only_move_defense" =>
        truthContract
          .filter(contract =>
            contract.isCriticalBestMove || contract.reasonFamily == DecisiveReasonKind.OnlyMoveDefense
          )
          .flatMap(contract =>
            inputs.decisionComparison
              .flatMap(onlyMoveTimingContrast(primary, inputs, _))
              .orElse(
                onlyMoveContrast(contract).map { case (anchor, consequence, sentence) =>
                  allow(SourceKind.DelayedOnlyMove, anchor, consequence, sentence)
                }
              )
          )
      case "decision_comparison" =>
        inputs.decisionComparison.map(decideDecisionComparison(primary, inputs, _, allowTiming = true, truthContract))
      case _ =>
        bestImmediateThreat(inputs.opponentThreats)
          .flatMap(threatContrast)
          .orElse(inputs.preventedPlansNow.iterator.map(preventedResourceContrast).collectFirst(Function.unlift(identity)))
          .orElse(inputs.decisionComparison.map(decideDecisionComparison(primary, inputs, _, allowTiming = true, truthContract)))

  private def threatContrast(threat: ThreatRow): Option[ContrastSupportTrace] =
    clean(threat.bestDefense)
      .map { rawDefense =>
        val defense = displayDefense(rawDefense)
        val threatSquare = threat.square.map(_.trim.toLowerCase).filter(MoveReviewPlayerPayloadBuilder.ChessSquarePattern.matches)
        val threatKind = clean(threat.kind).map(_.toLowerCase)
        val distinctThreatTarget = threatSquare.filter(_ != defense.trim.toLowerCase)
        val threatTargetText =
          distinctThreatTarget.map(square => s"the ${threatKind.getOrElse("concrete")} threat on $square")
        val forcedReply = threat.defenseCount <= 1 && !coordinateOnly(rawDefense)
        val guardrails =
          (
            List(
              SourceKind.ExplicitReplyLoss,
              if forcedReply then "forced_reply_unique" else "forced_reply_non_unique",
              s"reply_defense_count:${threat.defenseCount.max(0)}",
              s"reply_anchor_kind:${replyAnchorKind(rawDefense)}"
            ) ++
              threatKind.map(kind => s"threat_kind:${normalize(kind)}") ++
              threat.square.map(square => s"threat_square:${square.trim.toLowerCase}")
          ).distinct
        val evidenceRefs =
          (
            List(
              Some(s"reply_anchor:$defense"),
              Option.when(isUciMove(rawDefense))(s"reply_uci:${rawDefense.trim.toLowerCase}"),
              Some(s"reply_defense_count:${threat.defenseCount.max(0)}"),
              Some(s"loss_if_ignored_cp:${threat.lossIfIgnoredCp}"),
              Some(s"turns_to_impact:${threat.turnsToImpact}")
            ).flatten ++
              threatKind.map(kind => s"threat_kind:${normalize(kind)}") ++
              threat.square.map(square => s"threat_square:${square.trim.toLowerCase}")
          ).distinct
        val consequence =
          threatKind.map(kind => s"the opponent's $kind threat lands")
            .getOrElse("the reply becomes forced")
        allow(
          sourceKind = SourceKind.ExplicitReplyLoss,
          anchor = defense,
          consequence = consequence,
          sentence =
            if coordinateOnly(rawDefense) then s"If delayed, the reply has to address $defense."
            else if threat.defenseCount <= 1 && isUciMove(rawDefense) then s"If delayed, the forced reply goes to $defense."
            else if threat.defenseCount > 1 && isUciMove(rawDefense) && threatTargetText.nonEmpty then
              s"If delayed, one defensive reply has to answer ${threatTargetText.get}."
            else if threat.defenseCount > 1 && isUciMove(rawDefense) then s"If delayed, one defensive reply has to address $defense."
            else if threat.defenseCount > 1 then s"If delayed, $defense is one defensive reply."
            else if defense == "the defensive reply" then "If delayed, the reply becomes forced."
            else s"If delayed, $defense is the reply.",
          forcedReply = forcedReply,
          evidenceRefs = evidenceRefs,
          guardrails = guardrails
        )
      }

  private def displayDefense(raw: String): String =
    if isUciMove(raw) then NarrativeUtils.formatUciAsSan(raw) else raw

  private def coordinateOnly(raw: String): Boolean =
    raw.matches("""[a-h][1-8]""")

  private def isUciMove(raw: String): Boolean =
    raw.matches("""[a-h][1-8][a-h][1-8][qrbn]?""")

  private def replyAnchorKind(raw: String): String =
    if isUciMove(raw) then "uci"
    else if coordinateOnly(raw) then "square"
    else "san_or_text"

  private def preventedResourceContrast(plan: PreventedPlanInfo): Option[ContrastSupportTrace] =
    clean(plan.citationLine)
      .filterNot(looksLikeGeneralizedBranch)
      .map(line =>
        allow(
          sourceKind = SourceKind.PreventedResource,
          anchor = line,
          consequence = "that resource becomes available again",
          sentence = s"If delayed, the line $line becomes available again."
        )
      )
      .orElse(
        clean(plan.breakNeutralized).map(file =>
          allow(
            sourceKind = SourceKind.PreventedResource,
            anchor = s"$file-break",
            consequence = "the resource comes back into play",
            sentence = s"If delayed, the $file-break comes back into play."
          )
        )
      )
      .orElse(
        clean(plan.preventedThreatType).map(kind =>
          allow(
            sourceKind = SourceKind.PreventedResource,
            anchor = kind,
            consequence = s"the opponent's ${kind.toLowerCase} becomes live again",
            sentence = s"If delayed, the opponent's ${kind.toLowerCase} becomes concrete again."
          )
        )
      )

  private def onlyMoveContrast(
      contract: DecisiveTruthContract
  ): Option[(String, String, String)] =
    val benchmark = clean(contract.benchmarkMove)
    val anchor =
      benchmark
        .orElse(clean(contract.verifiedBestMove))
        .orElse(clean(contract.playedMove))
    anchor
      .filter(_ => onlyMoveLossMagnitude(contract) >= Thresholds.MISTAKE_CP)
      .map { move =>
        val label = benchmark.getOrElse("the played move")
        (
          move,
          "it is the only move that still holds the position together",
          s"Only $label still keeps the position together now."
        )
      }

  private def onlyMoveLossMagnitude(contract: DecisiveTruthContract): Int =
    math.max(contract.cpLoss, contract.swingSeverity)

  private def onlyMoveTimingContrast(
      primary: QuestionPlan,
      inputs: QuestionPlannerInputs,
      comparison: DecisionComparison
  ): Option[ContrastSupportTrace] =
    resolvedConsequence(primary, inputs, comparison)
      .flatMap { consequence =>
        val anchor =
          clean(comparison.deferredMove)
            .orElse(clean(comparison.engineBestMove))
            .orElse(clean(comparison.chosenMove))
        anchor.map { resolvedAnchor =>
          allow(
            sourceKind = SourceKind.DelayedOnlyMove,
            anchor = resolvedAnchor,
            consequence = consequence,
            sentence = renderOnlyMoveSentence(resolvedAnchor, consequence)
          )
        }
      }

  private def decideDecisionComparison(
      primary: QuestionPlan,
      inputs: QuestionPlannerInputs,
      comparison: DecisionComparison,
      allowTiming: Boolean,
      truthContract: Option[DecisiveTruthContract]
  ): ContrastSupportTrace =
    val directConsequence = decisionComparisonConsequence(comparison)
    val consequence = resolvedConsequence(primary, inputs, comparison, truthContract)
    clean(comparison.deferredSource) match
      case Some(source)
          if sameText(source, "close_candidate") &&
            exactComparativeConsequence(comparison).isEmpty &&
            roleAwareComparativeConsequence(comparison).isEmpty =>
        inputs.alternativeNarrative match
          case Some(alt) if enrichedCloseCandidateSentence(alt.sentence) =>
            allow(
              sourceKind = "enriched_close_candidate",
              anchor = alt.move.getOrElse(""),
              consequence = alt.reason,
              sentence = alt.sentence
            )
          case _ =>
            reject(RejectReason.RawCloseCandidate)
      case Some(source) if isEngineGapLike(source) && directConsequence.isEmpty && consequence.isEmpty =>
        reject(RejectReason.VagueEnginePreference)
      case _ if directConsequence.isEmpty && consequence.isEmpty && comparison.cpLossVsChosen.exists(math.abs(_) >= 60) =>
        reject(RejectReason.ExplanationFreeEvalGap)
      case _ =>
        val anchor =
          comparativeAnchor(comparison)
            .orElse(clean(comparison.deferredMove))
            .orElse(clean(comparison.engineBestMove))
            .orElse(clean(comparison.chosenMove))
        (anchor, consequence) match
          case (Some(move), Some(value)) if roleAwareComparativeConsequence(comparison).nonEmpty =>
            allow(
              sourceKind = SourceKind.RoleAwareLineConsequence,
              anchor = move,
              consequence = value,
              sentence = renderRoleAwareLineConsequenceSentence(value, allowTiming),
              evidenceRefs = roleAwareComparisonEvidenceRefs(comparison),
              guardrails = roleAwareComparisonGuardrails(comparison)
            )
          case (Some(move), Some(value))
              if comparison.chosenMatchesBest && counterfactualCausalThreatConsequence(inputs).exists(sameText(_, value)) =>
            allow(
              sourceKind = SourceKind.CounterfactualCausalThreat,
              anchor = move,
              consequence = value,
              sentence = renderCounterfactualCausalThreatSentence(move, value, allowTiming)
            )
          case (_, Some(value))
              if !comparison.chosenMatchesBest && localFactConsequence(primary, inputs, truthContract).exists(local => sameText(local.consequence, value)) =>
            localFactWhyThisSupport(primary, inputs, Some(comparison), allowTiming, truthContract)
              .getOrElse(reject(RejectReason.MissingConcreteConsequence))
          case (Some(move), Some(value))
              if clean(comparison.deferredSource).exists(isVerifiedOrTopEngineSource) ||
                comparison.chosenMatchesBest ||
                exactComparativeConsequence(comparison).nonEmpty =>
            allow(
              sourceKind = SourceKind.TopEngineMoveWithConcreteConsequence,
              anchor = move,
              consequence = value,
              sentence = renderTopEngineSentence(move, value, allowTiming)
            )
          case (Some(move), Some(value)) =>
            allow(
              sourceKind = SourceKind.ExplicitAlternativeCollapse,
              anchor = move,
              consequence = value,
              sentence = renderAlternativeSentence(move, value)
            )
          case (Some(_), None) if comparison.cpLossVsChosen.exists(math.abs(_) >= 60) =>
            reject(RejectReason.ExplanationFreeEvalGap)
          case (_, Some(_)) =>
            reject(RejectReason.MissingContrastCandidate)
          case _ =>
            reject(RejectReason.MissingConcreteConsequence)

  private[commentary] def enrichedCloseCandidateSentence(sentence: String): Boolean =
    val normalized = normalize(sentence)
    normalized.nonEmpty &&
      (
        normalized.contains("while") ||
          normalized.contains("whereas") ||
          normalized.contains("both")
      )

  private def resolvedConsequence(
      primary: QuestionPlan,
      inputs: QuestionPlannerInputs,
      comparison: DecisionComparison,
      truthContract: Option[DecisiveTruthContract] = None
  ): Option[String] =
    decisionComparisonConsequence(comparison).orElse(fallbackPlannerConsequence(primary, inputs, comparison, truthContract))

  private def decisionComparisonConsequence(comparison: DecisionComparison): Option[String] =
    exactComparativeConsequence(comparison)
      .orElse(roleAwareComparativeConsequence(comparison))
      .orElse(
        clean(comparison.deferredReason)
          .filterNot(looksLikeEngineGapReason)
          .filterNot(looksLikeGeneralizedBranch)
          .filter(hasConcreteAnchorOrAction)
      )

  private def exactComparativeConsequence(comparison: DecisionComparison): Option[String] =
    clean(comparison.comparativeSource)
      .filter(source => sameText(source, DecisionComparisonComparativeSupport.ExactTargetFixationSource))
      .flatMap(_ => clean(comparison.comparativeConsequence))
      .filter(looksLikeExactTargetFixationConsequence)
      .filter(hasConcreteAnchorOrAction)

  private def looksLikeExactTargetFixationConsequence(text: String): Boolean =
    val normalized = normalize(text)
    normalized.contains(" fixes ") &&
      normalized.contains(" as the target") &&
      normalized.contains(" leaves ") &&
      normalized.contains(" compared branch")

  private def roleAwareComparativeConsequence(comparison: DecisionComparison): Option[String] =
    clean(comparison.comparativeSource)
      .filter(source => sameText(source, DecisionComparisonComparativeSupport.RoleAwareLineConsequenceSource))
      .flatMap(_ => clean(comparison.comparativeConsequence))
      .filter(_ => DecisionComparisonComparativeSupport.roleAwareLineConsequenceText(comparison))
      .filter(hasConcreteAnchorOrAction)

  private def comparativeAnchor(comparison: DecisionComparison): Option[String] =
    exactComparativeConsequence(comparison)
      .orElse(roleAwareComparativeConsequence(comparison))
      .flatMap(_ => clean(comparison.engineBestMove).orElse(clean(comparison.chosenMove)))

  private def roleAwareComparisonEvidenceRefs(comparison: DecisionComparison): List[String] =
    (
      List(
        s"comparison_source:${SourceKind.RoleAwareLineConsequence}",
        "branch_role:engine_best",
        "branch_role:played"
      ) ++
        clean(comparison.engineBestMove).map(move => s"engine_best_move:$move") ++
        clean(comparison.chosenMove).map(move => s"played_move:$move") ++
        clean(comparison.comparedMove).map(move => s"compared_move:$move") ++
        comparison.cpLossVsChosen.map(loss => s"cp_loss:${math.abs(loss)}") ++
        comparison.roleAwareBranchEvidence.toList.flatMap(_.evidenceRefs)
    ).map(_.trim).filter(_.nonEmpty).distinct

  private def roleAwareComparisonGuardrails(comparison: DecisionComparison): List[String] =
    (
      List(
        SourceKind.RoleAwareLineConsequence,
        "alternative_role:engine_best_branch",
        "alternative_role:played_branch",
        "checked_branch_comparison"
      ) ++
        Option.when(comparison.cpLossVsChosen.exists(math.abs(_) > 0))("comparison_has_cp_gap") ++
        comparison.roleAwareBranchEvidence.toList.flatMap(_.guardrails)
    ).distinct

  private def fallbackPlannerConsequence(
      primary: QuestionPlan,
      inputs: QuestionPlannerInputs,
      comparison: DecisionComparison,
      truthContract: Option[DecisiveTruthContract]
  ): Option[String] =
    val localConsequence = localFactConsequence(primary, inputs, truthContract).map(_.consequence)
    Option.when(comparison.chosenMatchesBest || localConsequence.nonEmpty) {
      val certifiedPlannerConsequence =
        primary.consequence
        .filter(_.certified)
        .flatMap(consequence => clean(consequence.text))
      val counterfactualConsequence = counterfactualCausalThreatConsequence(inputs)
        .filter(_ => certifiedPlannerConsequence.exists(looksLikeGenericCounterfactualConsequence))
      List(
        counterfactualConsequence,
        certifiedPlannerConsequence,
        localConsequence,
        primaryClaimConsequence(primary, truthContract),
        derivedInputConsequence(inputs)
      ).flatten.find(admissibleCertifiedPlannerConsequence)
    }.flatten

  private def primaryClaimConsequence(
      primary: QuestionPlan,
      truthContract: Option[DecisiveTruthContract]
  ): Option[String] =
    Option.when(localFactConsequenceAllowed(truthContract)) {
      val sourceKeys = (primary.plannerSource :: primary.sourceKinds).map(_.trim.toLowerCase).toSet
      if sourceKeys.contains("relation_witness") && primary.admissibilityReasons.exists(sameText(_, "typed_local_fact")) &&
          primary.admissibilityReasons.exists(reason => sameText(reason, "local_fact_producer:relation_witness"))
      then relationProseConsequenceText(primary.claim)
      else if sourceKeys.contains("iqp_inducement_probe") && normalize(primary.claim).contains("isolated pawn") then
        Some("That leaves an isolated pawn as a local target.")
      else None
    }.flatten

  private final case class LocalFactConsequence(
      detailAnchor: String,
      consequence: String,
      evidenceRefs: List[String],
      guardrails: List[String]
  )

  private def localFactWhyThisSupport(
      primary: QuestionPlan,
      inputs: QuestionPlannerInputs,
      comparison: Option[DecisionComparison],
      allowTiming: Boolean,
      truthContract: Option[DecisiveTruthContract]
  ): Option[ContrastSupportTrace] =
    localFactConsequence(primary, inputs, truthContract).flatMap { local =>
      val moveAnchor =
        comparison
          .flatMap(value => clean(value.chosenMove).map(displayDefense))
          .orElse(playedMoveFromClaim(primary.claim))
          .orElse(Some(local.detailAnchor))
      moveAnchor.map { move =>
        allow(
          sourceKind = SourceKind.PlayedMoveWithCertifiedLocalConsequence,
          anchor = move,
          consequence = local.consequence,
          sentence = renderPlayedMoveLocalConsequenceSentence(move, local.consequence, allowTiming),
          evidenceRefs = local.evidenceRefs,
          guardrails = local.guardrails
        )
      }
    }

  private def localFactConsequence(
      primary: QuestionPlan,
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): Option[LocalFactConsequence] =
    Option.when(primary.questionKind == AuthorQuestionKind.WhyThis && localFactConsequenceAllowed(truthContract)) {
      inputs.localFactResult
        .filter(QuestionFirstCommentaryPlanner.localFactResultWhyThisEligible)
        .filter(result => localFactMatchesPrimary(primary, result))
        .flatMap(result => localFactConsequence(result))
    }.flatten

  private def localFactConsequenceAllowed(truthContract: Option[DecisiveTruthContract]): Boolean =
    truthContract.forall(contract => !contract.isBad)

  private def localFactMatchesPrimary(
      primary: QuestionPlan,
      result: MoveReviewExplanationBuilder.Result
  ): Boolean =
    sameText(primary.claim, result.explanation.prose) ||
      primary.plannerSource == result.explanation.source ||
      primary.sourceKinds.exists(kind => sameText(kind, result.explanation.source) || sameText(kind, "typed_local_fact")) ||
      localFactCarriesPlannerSource(primary, result.localFact)

  private def localFactCarriesPlannerSource(
      primary: QuestionPlan,
      fact: MoveReviewLocalFact.Admission
  ): Boolean =
    val selectedSources =
      (primary.plannerSource :: primary.sourceKinds)
        .flatMap(clean)
        .map(_.trim.toLowerCase)
        .filterNot(source => source == "typed_local_fact" || source == "planner")
        .toSet
    selectedSources.nonEmpty && selectedSources.exists(localFactSourceKeys(fact).contains)

  private def localFactSourceKeys(fact: MoveReviewLocalFact.Admission): Set[String] =
    val prefixes = List("proof_source:", "claim_source:", "typed_local_fact_source:", "evidence_source:", "source:")
    localFactEvidenceTokens(fact).flatMap { token =>
      prefixes.collectFirst {
        case prefix if token.startsWith(prefix) => token.drop(prefix.length).trim
      }
    }.filter(_.nonEmpty).toSet

  private def localFactConsequence(result: MoveReviewExplanationBuilder.Result): Option[LocalFactConsequence] =
    val fact = result.localFact
    for
      anchor <- localFactAnchor(fact).orElse(relationWitnessAnchor(fact))
      consequence <-
        if fact.producer == MoveReviewLocalFact.Producer.RelationWitness then
          relationWitnessConsequenceText(fact).orElse(relationWitnessProseConsequenceText(result.explanation.prose, fact))
        else localFactConsequenceText(fact, anchor)
    yield
      val evidenceRefs =
        (
          List(
            s"typed_local_fact_source:${result.explanation.source}",
            s"typed_local_fact_family:${fact.family.key}",
            s"typed_local_fact_producer:${fact.producer.key}",
            s"local_fact_anchor:$anchor"
          ) ++ fact.evidenceRefs
        ).distinct
      val guardrails =
        (
          List(
            SourceKind.PlayedMoveWithCertifiedLocalConsequence,
            "certified_local_fact_consequence",
            s"local_fact_authority:${fact.authority.key}",
            s"local_fact_line_binding:${fact.lineBinding.key}"
          ) ++ fact.guardrails
        ).distinct
      LocalFactConsequence(anchor, consequence, evidenceRefs, guardrails)

  private def localFactAnchor(fact: MoveReviewLocalFact.Admission): Option[String] =
    val preferredKeys =
      List("target", "played_san", "space_gain_pawn", "plan_anchor_matched_token", "anchor", "focus_square", "focus_file")
    preferredKeys.iterator
      .flatMap(key => fact.anchors.find(anchor => sameText(anchor.key, key)).flatMap(anchor => clean(anchor.value)))
      .toSeq
      .headOption
      .orElse(fact.anchors.iterator.flatMap(anchor => clean(anchor.value)).toSeq.headOption)

  private def localFactConsequenceText(
      fact: MoveReviewLocalFact.Admission,
      anchor: String
  ): Option[String] =
    val markers =
      normalize(
        (
          fact.guardrails ++
            fact.evidenceRefs ++
            fact.anchors.map(anchor => s"${anchor.key}:${anchor.value}")
        ).mkString(" ")
    )
    tacticalLocalFactConsequence(fact).orElse {
      fact.family match
        case MoveReviewLocalFact.Family.Pressure
            if markers.contains("iqp inducement") || isolatedPawnTarget(fact).nonEmpty =>
          isolatedPawnTarget(fact)
            .map(target => s"That leaves $target as an isolated pawn target.")
            .orElse(Some(s"That keeps the isolated-pawn target fixed around $anchor."))
        case MoveReviewLocalFact.Family.Defense if markers.contains("counterplay") =>
          Some(s"That keeps counterplay restrained around $anchor.")
        case MoveReviewLocalFact.Family.Defense =>
          Some(s"That keeps the defensive detail tied to $anchor.")
        case MoveReviewLocalFact.Family.PlanSupport =>
          Some(s"That keeps the plan support connected around $anchor.")
        case MoveReviewLocalFact.Family.Pressure if markers.contains("space") =>
          Some(s"That keeps space pressure anchored around $anchor.")
        case MoveReviewLocalFact.Family.Pressure if markers.contains("outpost") =>
          Some(s"That keeps outpost pressure connected around $anchor.")
        case MoveReviewLocalFact.Family.Pressure if markers.contains("target fixing") || markers.contains("target pressure") =>
          Some(s"That keeps target pressure fixed around $anchor.")
        case MoveReviewLocalFact.Family.Pressure =>
          Some(s"That keeps positional pressure anchored around $anchor.")
        case MoveReviewLocalFact.Family.Threat =>
          Some(s"That keeps the local tactical threat concrete around $anchor.")
        case MoveReviewLocalFact.Family.Attack =>
          Some(s"That keeps the attacking idea connected around $anchor.")
        case MoveReviewLocalFact.Family.LineConsequence =>
          Some(s"That keeps the checked line consequence tied to $anchor.")
        case MoveReviewLocalFact.Family.Timing =>
          Some(s"That keeps the move-order point tied to $anchor.")
        case _ =>
          None
    }

  private def tacticalLocalFactConsequence(fact: MoveReviewLocalFact.Admission): Option[String] =
    Option.when(
      fact.family == MoveReviewLocalFact.Family.Threat &&
        fact.producer == MoveReviewLocalFact.Producer.TacticalMotif
    ) {
      anchorValue(fact, "tactical_kind").flatMap {
        case "pin" =>
          for
            pinned <- pieceAnchorLabel(fact, "pinned_square", "pinned_role")
            behind <- pieceAnchorLabel(fact, "behind_square", "behind_role")
          yield s"That keeps the $pinned pinned to the $behind."
        case "skewer" =>
          for
            front <- pieceAnchorLabel(fact, "front_square", "front_role")
            back <- pieceAnchorLabel(fact, "back_square", "back_role")
          yield s"That keeps the skewer through the $front toward the $back."
        case "fork" =>
          val targets = fact.anchors
            .filter(anchor => anchor.key.startsWith("target_"))
            .sortBy(_.key)
            .map(_.value.trim)
            .filter(_.nonEmpty)
            .distinct
          Option.when(targets.nonEmpty)(s"That keeps the fork targets ${targets.take(3).mkString(" and ")} concrete.")
        case "check" =>
          pieceAnchorLabel(fact, "king_square", "king_role").map(king => s"That keeps the check on the $king concrete.")
        case "discovered_attack" =>
          for
            revealed <- pieceAnchorLabel(fact, "revealed_square", "revealed_role")
            target <- pieceAnchorLabel(fact, "target_square", "target_role")
          yield s"That keeps the discovered attack from the $revealed toward the $target concrete."
        case "trapped_piece" =>
          pieceAnchorLabel(fact, "trapped_square", "trapped_role").map(trapped => s"That keeps the trap on the $trapped concrete.")
        case _ => None
      }
    }.flatten

  private def relationWitnessConsequenceText(fact: MoveReviewLocalFact.Admission): Option[String] =
    Option.when(relationWitnessConsequenceAllowed(fact)) {
      relationKind(fact).flatMap {
        case "pin" =>
          for
            pinned <- relationPieceLabel(fact, "pinned")
            behind <- relationPieceLabel(fact, "behind")
          yield s"That keeps the $pinned pinned to the $behind."
        case "overload" =>
          for
            defender <- relationFactValue(fact, "defender")
            duties = relationFactValues(fact, "duties")
            if duties.size >= 2
          yield s"That keeps the defender on $defender overloaded across ${duties.take(3).mkString(" and ")}."
        case _ => None
      }
    }.flatten

  private def relationWitnessProseConsequenceText(
      prose: String,
      fact: MoveReviewLocalFact.Admission
  ): Option[String] =
    Option.when(relationWitnessConsequenceAllowed(fact))(relationProseConsequenceText(prose)).flatten

  private def relationProseConsequenceText(prose: String): Option[String] =
    prose.trim match
      case RelationOverloadProse(defender, duties) =>
        val targets = duties.split("""\s+and\s+""").toList.map(_.trim).filter(_.nonEmpty)
        Option.when(targets.size >= 2)(
          s"That keeps the defender on ${defender.toLowerCase} overloaded across ${targets.take(3).mkString(" and ")}."
        )
      case RelationPinProse(pinnedRole, pinnedSquare, behindRole, behindSquare) =>
        Some(
          s"That keeps the ${pinnedRole.toLowerCase} on ${pinnedSquare.toLowerCase} pinned to the ${behindRole.toLowerCase} on ${behindSquare.toLowerCase}."
        )
      case _ => None

  private val RelationOverloadProse =
    """(?i).*overloads?\s+the\s+defender\s+on\s+([a-h][1-8])\s+across\s+([a-h][1-8](?:\s+and\s+[a-h][1-8]){1,2}).*""".r

  private val RelationPinProse =
    """(?i).*pins?\s+the\s+([a-z]+)\s+on\s+([a-h][1-8])\s+to\s+the\s+([a-z]+)\s+on\s+([a-h][1-8]).*""".r

  private def relationWitnessConsequenceAllowed(fact: MoveReviewLocalFact.Admission): Boolean =
    val guardrails = fact.normalizedGuardrails
    val tokens = localFactEvidenceTokens(fact)
    fact.producer == MoveReviewLocalFact.Producer.RelationWitness &&
      fact.lineBinding == MoveReviewLocalFact.LineBinding.PvCoupled &&
      tokens.exists(token => token == "typed_local_fact_source:relation_witness" || token == "evidence_source:relation_witness") &&
      guardrails.exists(guardrail =>
        guardrail == "relation_witness_typed_details" ||
          guardrail == "local_fact_guardrail:relation_witness_typed_details"
      )

  private def relationKind(fact: MoveReviewLocalFact.Admission): Option[String] =
    relationFactValue(fact, "relation_kind")

  private def relationWitnessAnchor(fact: MoveReviewLocalFact.Admission): Option[String] =
    relationFactValue(fact, "target")
      .orElse(relationFactValue(fact, "defender"))
      .orElse(relationFactValue(fact, "pinned"))
      .orElse(relationFactValue(fact, "behind"))

  private def relationPieceLabel(fact: MoveReviewLocalFact.Admission, key: String): Option[String] =
    relationFactValue(fact, key).map { square =>
      relationFactValue(fact, s"${key}_role")
        .map(role => s"$role on $square")
        .getOrElse(s"piece on $square")
    }

  private def isolatedPawnTarget(fact: MoveReviewLocalFact.Admission): Option[String] =
    localFactEvidenceTokens(fact).collectFirst {
      case token if token.contains(":isolated_pawn:") =>
        token.split(':').lastOption.getOrElse("").trim
    }.filter(_.nonEmpty)

  private def relationFactValue(fact: MoveReviewLocalFact.Admission, key: String): Option[String] =
    relationFactValues(fact, key).headOption

  private def relationFactValues(fact: MoveReviewLocalFact.Admission, key: String): List[String] =
    val normalizedKey = key.trim.toLowerCase
    val prefixes = List(s"$normalizedKey:", s"relation_fact:$normalizedKey:", s"anchor:$normalizedKey:")
    val tokenValues =
      localFactEvidenceTokens(fact).flatMap { token =>
        prefixes.collectFirst {
          case prefix if token.startsWith(prefix) => token.drop(prefix.length).trim
        }
      }
    val anchorValues =
      fact.anchors
        .filter(anchor => anchor.key.trim.equalsIgnoreCase(normalizedKey))
        .map(_.value.trim.toLowerCase)
    (tokenValues ++ anchorValues)
      .flatMap(_.split("\\|").toList)
      .map(_.trim)
      .filter(_.nonEmpty)
      .distinct

  private def localFactEvidenceTokens(fact: MoveReviewLocalFact.Admission): List[String] =
    (
      fact.normalizedEvidenceRefs ++
        fact.normalizedGuardrails ++
        fact.anchors.map(anchor => s"anchor:${anchor.key.trim.toLowerCase}:${anchor.value.trim.toLowerCase}")
    ).filter(_.nonEmpty).distinct

  private def anchorValue(fact: MoveReviewLocalFact.Admission, key: String): Option[String] =
    fact.anchors.find(anchor => anchor.key == key).flatMap(anchor => clean(anchor.value))

  private def pieceAnchorLabel(
      fact: MoveReviewLocalFact.Admission,
      squareKey: String,
      roleKey: String
  ): Option[String] =
    for
      square <- anchorValue(fact, squareKey)
      role <- anchorValue(fact, roleKey)
    yield s"$square $role"

  private def playedMoveFromClaim(claim: String): Option[String] =
    clean(claim).flatMap(_.split("""\s+""").headOption).flatMap(clean).map(_.stripSuffix(":"))

  private def derivedInputConsequence(inputs: QuestionPlannerInputs): Option[String] =
    immediateThreatConsequence(inputs)
      .orElse(preventedCounterplayConsequence(inputs))
      .orElse(pvDeltaConsequence(inputs))
      .orElse(counterfactualCausalThreatConsequence(inputs))
      .orElse(counterfactualConsequence(inputs))

  private def immediateThreatConsequence(inputs: QuestionPlannerInputs): Option[String] =
    bestImmediateThreat(inputs.opponentThreats).flatMap { threat =>
      clean(threat.kind).map(kind => s"That keeps the immediate ${kind.toLowerCase} pressure from taking over.")
    }

  private def preventedCounterplayConsequence(inputs: QuestionPlannerInputs): Option[String] =
    inputs.preventedPlansNow.collectFirst(Function.unlift { plan =>
      Option.when(plan.counterplayScoreDrop > 0)(
        s"That shuts down roughly ${plan.counterplayScoreDrop}cp of counterplay before it starts."
      )
    })

  private def pvDeltaConsequence(inputs: QuestionPlannerInputs): Option[String] =
    inputs.pvDelta.flatMap { delta =>
      delta.resolvedThreats.headOption.map(threat => s"That removes the immediate problem of $threat.")
        .orElse(
          delta.planAdvancements.headOption.map(step =>
            s"That changes the next phase of the position by making $step available."
          )
        )
        .orElse(
          delta.newOpportunities.headOption.map(opportunity =>
            s"That creates a concrete follow-up around $opportunity."
          )
        )
    }

  private def counterfactualConsequence(inputs: QuestionPlannerInputs): Option[String] =
    inputs.counterfactual
      .filter(_.cpLoss > 0)
      .map { cf =>
        clean(cf.bestMove).filterNot(isUciMove) match
          case Some(bestMove) => s"If the move is missed, $bestMove becomes the cleaner continuation instead."
          case None           => "If the move is missed, the opponent gets a cleaner continuation instead."
      }

  private def counterfactualCausalThreatConsequence(inputs: QuestionPlannerInputs): Option[String] =
    inputs.counterfactual.flatMap(ThreatExtractor.counterfactualCausalThreatConsequence)

  private def renderOnlyMoveSentence(anchor: String, consequence: String): String =
    consequenceVerbPhrase(consequence) match
      case Some(verbPhrase) =>
        s"If delayed, only $anchor still keeps the position together because it $verbPhrase"
      case None =>
        s"If delayed, only $anchor still keeps the position together because ${becauseClause(consequence)}"

  private def renderTopEngineSentence(move: String, consequence: String, allowTiming: Boolean): String =
    if opponentCleanerContinuation(consequence) then
      if allowTiming then s"If delayed, $move avoids giving the opponent a cleaner continuation."
      else s"The move $move stays best because it avoids giving the opponent a cleaner continuation."
    else
      consequenceVerbPhrase(consequence) match
        case Some(verbPhrase) =>
          if allowTiming then s"If delayed, $move is still the move that $verbPhrase"
          else s"The move $move stays best because it $verbPhrase"
        case None =>
          val clause = becauseClause(consequence)
          if allowTiming then s"If delayed, $move is still the move that matters, because $clause"
          else s"The move $move stays best because $clause"

  private def opponentCleanerContinuation(consequence: String): Boolean =
    normalize(consequence).startsWith("if the move is missed the opponent gets a cleaner continuation")

  private def renderAlternativeSentence(move: String, consequence: String): String =
    s"The alternative $move stays secondary because ${becauseClause(consequence)}"

  private def renderRoleAwareLineConsequenceSentence(consequence: String, allowTiming: Boolean): String =
    val sentence = ensureSentence(consequence)
    if allowTiming then s"If delayed, $sentence"
    else sentence

  private def renderCounterfactualCausalThreatSentence(move: String, consequence: String, allowTiming: Boolean): String =
    val clause = becauseClause(consequence)
    if allowTiming then s"If delayed, $clause"
    else s"The move $move stays best because $clause"

  private def renderPlayedMoveLocalConsequenceSentence(move: String, consequence: String, allowTiming: Boolean): String =
    val clause = becauseClause(consequence)
    if allowTiming then s"If delayed, $move is still the move with the checked local point because it $clause"
    else s"The move $move has a checked local point because it $clause"

  private def consequenceVerbPhrase(consequence: String): Option[String] =
    val clause = becauseClause(consequence)
    val normalized = normalize(clause)
    Option.when(
      normalized.startsWith("preserves ") ||
        normalized.startsWith("keeps ") ||
        normalized.startsWith("leaves ") ||
        normalized.startsWith("shuts down ") ||
        normalized.startsWith("changes ") ||
        normalized.startsWith("creates ") ||
        normalized.startsWith("removes ")
    )(clause)

  private def becauseClause(consequence: String): String =
    val trimmed = stripSentencePunctuation(consequence.trim)
    val withoutLead =
      if trimmed.startsWith("That ") || trimmed.startsWith("This ") then trimmed.drop(5)
      else if trimmed.startsWith("If the move is missed,") then
        trimmed.drop("If the move is missed,".length).trim
      else trimmed
    lowerCaseLeading(withoutLead)

  private def looksLikeCertifiedPlannerConsequence(text: String): Boolean =
    val normalized = normalize(text)
      normalized.startsWith("that keeps the immediate ") ||
      (normalized.startsWith("that keeps the ") && normalized.contains(" pinned to the ")) ||
      (normalized.startsWith("that keeps the defender on ") && normalized.contains(" overloaded across ")) ||
      (normalized.startsWith("that leaves ") && normalized.contains(" isolated pawn target")) ||
      (normalized.startsWith("that leaves ") && normalized.contains(" isolated pawn as a local target")) ||
      normalized.startsWith("that keeps the skewer through ") ||
      normalized.startsWith("that keeps the fork targets ") ||
      normalized.startsWith("that keeps the check on ") ||
      normalized.startsWith("that keeps the discovered attack from ") ||
      normalized.startsWith("that keeps the trap on ") ||
      normalized.startsWith("that keeps counterplay restrained around ") ||
      normalized.startsWith("that keeps the defensive detail tied to ") ||
      normalized.startsWith("that keeps the plan support connected around ") ||
      normalized.startsWith("that keeps target pressure fixed around ") ||
      normalized.startsWith("that keeps space pressure anchored around ") ||
      normalized.startsWith("that keeps outpost pressure connected around ") ||
      normalized.startsWith("that keeps positional pressure anchored around ") ||
      normalized.startsWith("that keeps the local tactical threat concrete around ") ||
      normalized.startsWith("that keeps the attacking idea connected around ") ||
      normalized.startsWith("that keeps the checked line consequence tied to ") ||
      normalized.startsWith("that keeps the move order point tied to ") ||
      normalized.startsWith("that keeps roughly ") ||
      normalized.startsWith("that shuts down roughly ") ||
      normalized.startsWith("that preserves roughly ") ||
      normalized.startsWith("that changes the next phase ") ||
      normalized.startsWith("that creates a concrete follow up ") ||
      normalized.startsWith("that removes the immediate problem ") ||
      normalized.startsWith("that removes roughly ") ||
      normalized.startsWith("missing it ") ||
      normalized.startsWith("if the move is missed, ") ||
      normalized.startsWith("if the move is missed ")

  private def admissibleCertifiedPlannerConsequence(text: String): Boolean =
    looksLikeCertifiedPlannerConsequence(text)

  private def looksLikeGenericCounterfactualConsequence(text: String): Boolean =
    val normalized = normalize(text)
    normalized.startsWith("if the move is missed ") &&
      (
        normalized.contains("cleaner continuation") ||
          normalized.contains("opponent gets a cleaner continuation")
      )

  private def bestImmediateThreat(threats: List[ThreatRow]): Option[ThreatRow] =
    threats
      .filter(threat => threat.lossIfIgnoredCp > 0 || clean(threat.kind).exists(_.equalsIgnoreCase("mate")))
      .sortBy(threat => (-threat.lossIfIgnoredCp, threat.turnsToImpact))
      .headOption

  private def allow(
      sourceKind: String,
      anchor: String,
      consequence: String,
      sentence: String,
      forcedReply: Boolean = false,
      evidenceRefs: List[String] = Nil,
      guardrails: List[String] = Nil
  ): ContrastSupportTrace =
    ContrastSupportTrace(
      contrast_source_kind = clean(anchor).flatMap(_ => Some(sourceKind)),
      contrast_anchor = clean(anchor),
      contrast_consequence = clean(consequence),
      contrast_admissible = true,
      contrast_sentence = clean(sentence).map(ensureSentence),
      contrast_forced_reply = forcedReply,
      contrast_evidence_refs = evidenceRefs.map(_.trim).filter(_.nonEmpty).distinct,
      contrast_guardrails = guardrails.map(_.trim).filter(_.nonEmpty).distinct
    )

  private def reject(reason: String): ContrastSupportTrace =
    ContrastSupportTrace(
      contrast_admissible = false,
      contrast_reject_reason = Some(reason)
    )

  private def isEngineGapLike(source: String): Boolean =
    sameText(source, "engine_gap") ||
      sameText(source, "top_engine_move") ||
      sameText(source, "verified_best")

  private def isVerifiedOrTopEngineSource(source: String): Boolean =
    sameText(source, "verified_best") || sameText(source, "top_engine_move")

  private def looksLikeEngineGapReason(reason: String): Boolean =
    val normalized = normalize(reason)
    normalized.startsWith("it trails the engine line by about") ||
      normalized.startsWith("it trails the engine line") ||
      normalized.startsWith("delaying costs about") ||
      normalized.startsWith("it costs about")

  private def looksLikeGeneralizedBranch(text: String): Boolean =
    LineScopedCitation.hasConcreteSanLine(text) ||
      text.matches("""(?i).*\bline:\b.*""") ||
      text.matches("""^[a-z]\)\s+.*""")

  private def hasConcreteAnchorOrAction(text: String): Boolean =
    LiveNarrativeCompressionCore.hasConcreteAnchor(text) ||
      normalize(text).split("""[^a-z0-9]+""").exists(token =>
        token.nonEmpty && Set(
          "break",
          "reply",
          "recapture",
          "threat",
          "counterplay",
          "mate",
          "wins",
          "loses",
          "drops",
          "fork",
          "pin",
          "trade"
        ).contains(token)
      )

  private def clean(raw: Option[String]): Option[String] =
    raw.flatMap(clean)

  private def clean(raw: String): Option[String] =
    Option(raw).map(_.trim.replaceAll("\\s+", " ")).filter(_.nonEmpty)

  private def ensureSentence(raw: String): String =
    if raw.isEmpty then raw
    else if ".!?".contains(raw.last) then raw
    else s"$raw."

  private def stripSentencePunctuation(raw: String): String =
    raw.replaceAll("""[.!?]+$""", "")

  private def lowerCaseLeading(raw: String): String =
    if raw.isEmpty then raw
    else if "KQRBNO".contains(raw.head) then raw
    else if raw.length >= 2 && raw.head.isUpper && !raw.charAt(1).isLower then raw
    else s"${raw.head.toLower}${raw.drop(1)}"

  private def normalize(raw: String): String =
    Option(raw).getOrElse("").toLowerCase.replaceAll("""[^a-z0-9\s]""", " ").replaceAll("\\s+", " ").trim

  private def sameText(left: String, right: String): Boolean =
    normalize(left) == normalize(right)
