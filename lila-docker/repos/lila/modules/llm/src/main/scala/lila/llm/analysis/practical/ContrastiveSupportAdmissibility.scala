package lila.llm.analysis.practical

import lila.llm.analysis.*
import lila.llm.model.*
import lila.llm.model.authoring.AuthorQuestionKind

private[llm] object ContrastiveSupportAdmissibility:

  object SourceKind:
    val ExplicitAlternativeCollapse = "explicit_alternative_collapse"
    val PreventedResource = "prevented_resource"
    val DelayedOnlyMove = "delayed_only_move"
    val ExplicitReplyLoss = "explicit_reply_loss"
    val TopEngineMoveWithConcreteConsequence = "top_engine_move_with_concrete_consequence"

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
      contrast_sentence: Option[String] = None
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
      case _                          => reject(RejectReason.QuestionOutsideScope)

  private def decideWhyThis(
      primary: QuestionPlan,
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): ContrastSupportTrace =
    inputs.decisionComparison
      .map(decideDecisionComparison(primary, inputs, _, allowTiming = false))
      .getOrElse {
        truthContract
          .filter(contract =>
            contract.isCriticalBestMove || contract.reasonFamily == DecisiveReasonFamily.OnlyMoveDefense
          )
          .flatMap(onlyMoveContrast)
          .map { case (anchor, consequence, sentence) =>
            allow(SourceKind.DelayedOnlyMove, anchor, consequence, sentence)
          }
          .getOrElse(reject(RejectReason.MissingContrastCandidate))
      }

  private def decideWhyNow(
      primary: QuestionPlan,
      inputs: QuestionPlannerInputs,
      truthContract: Option[DecisiveTruthContract]
  ): ContrastSupportTrace =
    bestImmediateThreat(inputs.opponentThreats)
      .flatMap(threatContrast)
      .orElse(inputs.preventedPlansNow.iterator.map(preventedResourceContrast).collectFirst(Function.unlift(identity)))
      .orElse {
        truthContract
          .filter(contract =>
            contract.isCriticalBestMove || contract.reasonFamily == DecisiveReasonFamily.OnlyMoveDefense
          )
          .flatMap(_ => inputs.decisionComparison.flatMap(onlyMoveTimingContrast(primary, inputs, _)))
      }
      .orElse(inputs.decisionComparison.map(decideDecisionComparison(primary, inputs, _, allowTiming = true)))
      .getOrElse(reject(RejectReason.MissingContrastCandidate))

  private def threatContrast(threat: ThreatRow): Option[ContrastSupportTrace] =
    clean(threat.bestDefense)
      .map { defense =>
        val consequence =
          clean(threat.kind).map(kind => s"the opponent's ${kind.toLowerCase} threat lands")
            .getOrElse("the reply becomes forced")
        allow(
          sourceKind = SourceKind.ExplicitReplyLoss,
          anchor = defense,
          consequence = consequence,
          sentence = s"If delayed, $defense is the reply."
        )
      }

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
    clean(contract.benchmarkMove)
      .map { move =>
        (
          move,
          "it is the only move that still holds the position together",
          s"Only $move still keeps the position together if the move is delayed."
        )
      }

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
      allowTiming: Boolean
  ): ContrastSupportTrace =
    clean(comparison.deferredSource) match
      case Some(source) if sameText(source, "close_candidate") =>
        reject(RejectReason.RawCloseCandidate)
      case Some(source) if isEngineGapLike(source) && decisionComparisonConsequence(comparison).isEmpty =>
        reject(RejectReason.VagueEnginePreference)
      case _ if decisionComparisonConsequence(comparison).isEmpty && comparison.cpLossVsChosen.exists(math.abs(_) >= 60) =>
        reject(RejectReason.ExplanationFreeEvalGap)
      case _ =>
        val anchor =
          clean(comparison.deferredMove)
            .orElse(clean(comparison.engineBestMove))
            .orElse(clean(comparison.chosenMove))
        val consequence = resolvedConsequence(primary, inputs, comparison)
        (anchor, consequence) match
          case (Some(move), Some(value))
              if clean(comparison.deferredSource).exists(isVerifiedOrTopEngineSource) || comparison.chosenMatchesBest =>
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

  private def resolvedConsequence(
      primary: QuestionPlan,
      inputs: QuestionPlannerInputs,
      comparison: DecisionComparison
  ): Option[String] =
    decisionComparisonConsequence(comparison).orElse(fallbackPlannerConsequence(primary, inputs, comparison))

  private def decisionComparisonConsequence(comparison: DecisionComparison): Option[String] =
    clean(comparison.deferredReason)
      .filterNot(looksLikeEngineGapReason)
      .filterNot(looksLikeGeneralizedBranch)
      .filter(hasConcreteAnchorOrAction)

  private def fallbackPlannerConsequence(
      primary: QuestionPlan,
      inputs: QuestionPlannerInputs,
      comparison: DecisionComparison
  ): Option[String] =
    Option.when(comparison.chosenMatchesBest) {
      primary.consequence
        .filter(_.certified)
        .flatMap(consequence => clean(consequence.text))
        .orElse(derivedInputConsequence(inputs))
        .filterNot(looksLikeGeneralizedBranch)
        .filter(looksLikeCertifiedPlannerConsequence)
    }.flatten

  private def derivedInputConsequence(inputs: QuestionPlannerInputs): Option[String] =
    immediateThreatConsequence(inputs)
      .orElse(preventedCounterplayConsequence(inputs))
      .orElse(pvDeltaConsequence(inputs))
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
      .flatMap(cf => clean(cf.bestMove).map(bestMove => s"If the move is missed, $bestMove becomes the cleaner continuation instead."))

  private def renderOnlyMoveSentence(anchor: String, consequence: String): String =
    consequenceVerbPhrase(consequence) match
      case Some(verbPhrase) =>
        s"If delayed, only $anchor still keeps the position together because it $verbPhrase"
      case None =>
        s"If delayed, only $anchor still keeps the position together because ${becauseClause(consequence)}"

  private def renderTopEngineSentence(move: String, consequence: String, allowTiming: Boolean): String =
    consequenceVerbPhrase(consequence) match
      case Some(verbPhrase) =>
        if allowTiming then s"If delayed, $move is still the move that $verbPhrase"
        else s"The move $move stays best because it $verbPhrase"
      case None =>
        val clause = becauseClause(consequence)
        if allowTiming then s"If delayed, $move is still the move that matters, because $clause"
        else s"The move $move stays best because $clause"

  private def renderAlternativeSentence(move: String, consequence: String): String =
    s"The alternative $move stays secondary because ${becauseClause(consequence)}"

  private def consequenceVerbPhrase(consequence: String): Option[String] =
    val clause = becauseClause(consequence)
    val normalized = normalize(clause)
    Option.when(
      normalized.startsWith("preserves ") ||
        normalized.startsWith("keeps ") ||
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
      normalized.startsWith("that keeps roughly ") ||
      normalized.startsWith("that shuts down roughly ") ||
      normalized.startsWith("that preserves roughly ") ||
      normalized.startsWith("that changes the next phase ") ||
      normalized.startsWith("that creates a concrete follow up ") ||
      normalized.startsWith("that removes the immediate problem ") ||
      normalized.startsWith("that removes roughly ") ||
      normalized.startsWith("if the move is missed, ") ||
      normalized.startsWith("if the move is missed ")

  private def bestImmediateThreat(threats: List[ThreatRow]): Option[ThreatRow] =
    threats
      .filter(threat => threat.lossIfIgnoredCp > 0 || clean(threat.kind).exists(_.equalsIgnoreCase("mate")))
      .sortBy(threat => (-threat.lossIfIgnoredCp, threat.turnsToImpact))
      .headOption

  private def allow(
      sourceKind: String,
      anchor: String,
      consequence: String,
      sentence: String
  ): ContrastSupportTrace =
    ContrastSupportTrace(
      contrast_source_kind = clean(anchor).flatMap(_ => Some(sourceKind)),
      contrast_anchor = clean(anchor),
      contrast_consequence = clean(consequence),
      contrast_admissible = true,
      contrast_sentence = clean(sentence).map(ensureSentence)
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
