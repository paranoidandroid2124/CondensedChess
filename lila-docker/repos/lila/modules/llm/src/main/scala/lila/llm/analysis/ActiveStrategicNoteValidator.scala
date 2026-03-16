package lila.llm.analysis

import lila.llm.*

private[llm] object ActiveStrategicNoteValidator:

  private val LeakTokens = List("PA_MATCH", "PRECOND_MISS", "REQ_", "SUP_", "BLK_")

  final case class Result(
      text: String,
      hardReasons: List[String],
      warningReasons: List[String]
  ):
    def isAccepted: Boolean = hardReasons.isEmpty

  def shouldRepair(result: Result): Boolean = result.hardReasons.nonEmpty

  def validate(
      candidateText: String,
      baseNarrative: String,
      dossier: Option[ActiveBranchDossier],
      strategyPack: Option[StrategyPack],
      routeRefs: List[ActiveStrategicRouteRef],
      moveRefs: List[ActiveStrategicMoveRef],
      strategyReasons: List[String]
  ): Result =
    val trimmed = Option(candidateText).map(_.trim).getOrElse("")
    val normalizedText = trimmed.toLowerCase
    val comparison = strategyPack.flatMap(_.signalDigest).flatMap(_.decisionComparison)
    val coachingBrief = ActiveStrategicCoachingBriefBuilder.build(strategyPack, dossier, routeRefs, moveRefs)
    val coachingCoverage = ActiveStrategicCoachingBriefBuilder.evaluateCoverage(trimmed, coachingBrief)
    val surface = StrategyPackSurface.from(strategyPack)

    val baseHardReasons =
      List(
        Option.when(trimmed.isEmpty)("empty_polish"),
        Option.when(trimmed.nonEmpty && looksJsonWrapper(trimmed))("json_wrapper_unparsed"),
        Option.when(trimmed.nonEmpty && looksTruncated(trimmed))("truncated_output"),
        Option.when(trimmed.nonEmpty && LeakTokens.exists(trimmed.contains))("leak_token_detected")
      ).flatten

    val sentenceWarnings =
      if trimmed.isEmpty then Nil
      else
        val count = sentenceCount(trimmed)
        Option.when(count < 2 || count > 4)("active_note_sentence_count").toList

    val independenceHardReasons =
      if trimmed.isEmpty then Nil
      else ActiveNoteIndependenceGuard.reasons(trimmed, baseNarrative)

    val strategyHardReasons =
      strategyReasons.filter(_ == "strategy_coverage_low")

    val coachingHardReasons =
      List(
        Option.when(trimmed.nonEmpty && !coachingCoverage.hasDominantIdea)("dominant_idea_missing"),
        Option.when(trimmed.nonEmpty && !coachingCoverage.hasForwardPlan)("forward_plan_missing"),
        Option.when(trimmed.nonEmpty && !coachingCoverage.hasOpponentOrTrigger)("opponent_or_trigger_missing"),
        Option.when(trimmed.nonEmpty && surface.ownerMismatch && !coachingCoverage.hasCampaignOwner)("campaign_owner_missing")
      ).flatten

    val strategyWarnings =
      strategyReasons.filterNot(_ == "strategy_coverage_low")

    val dossierWarnings =
      if trimmed.isEmpty then Nil
      else
        dossier.toList.flatMap { value =>
          val compareRefs = List(Some(value.chosenBranchLabel), value.engineBranchLabel, value.deferredBranchLabel).flatten
          val comparePresent =
            compareRefs.exists(label => StrategicSignalMatcher.containsComparablePhrase(normalizedText, label))
          List(
            Option.when(!comparePresent && value.evidenceCue.exists(_.trim.nonEmpty))("active_branch_dossier_presence"),
            Option.when(!comparePresent)("active_compare_missing"),
            Option.when(
              value.deferredBranchLabel.exists(_.trim.nonEmpty) &&
                !StrategicSignalMatcher.containsComparablePhrase(normalizedText, value.deferredBranchLabel.get)
            )("active_deferred_branch_missing"),
            Option.when(
              value.opponentResource.exists(_.trim.nonEmpty) &&
                !StrategicSignalMatcher.containsComparablePhrase(normalizedText, value.opponentResource.get)
            )("active_opponent_resource_missing")
          ).flatten
        }

    Result(
      text = trimmed,
      hardReasons = (baseHardReasons ++ independenceHardReasons ++ strategyHardReasons ++ coachingHardReasons).distinct,
      warningReasons = (sentenceWarnings ++ strategyWarnings ++ dossierWarnings).distinct
    )

  private def sentenceCount(text: String): Int =
    Option(text)
      .getOrElse("")
      .split("(?<=[.!?])\\s+")
      .map(_.trim)
      .count(_.nonEmpty)

  private def looksJsonWrapper(text: String): Boolean =
    val trimmed = Option(text).map(_.trim).getOrElse("")
    trimmed.startsWith("{") && trimmed.contains("\"commentary\"")

  private def looksTruncated(text: String): Boolean =
    val trimmed = Option(text).map(_.trim).getOrElse("")
    if trimmed.isEmpty then false
    else
      def balanced(open: Char, close: Char): Boolean =
        trimmed.count(_ == open) == trimmed.count(_ == close)
      val quoteCount = trimmed.count(_ == '"')
      !balanced('{', '}') || !balanced('[', ']') || (quoteCount % 2 != 0) || trimmed.endsWith("\\") ||
        MoveAnchorCodec.hasBrokenAnchorPrefix(trimmed)
