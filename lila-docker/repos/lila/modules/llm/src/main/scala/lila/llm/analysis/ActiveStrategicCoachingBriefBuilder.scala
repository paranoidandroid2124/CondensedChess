package lila.llm.analysis

import _root_.chess.{ Bishop, Board, Color, Knight, Pawn, Queen, Rook, Square }
import _root_.chess.format.Fen
import _root_.chess.variant.Standard

import lila.llm.*

private[llm] object ActiveStrategicCoachingBriefBuilder:

  final case class Brief(
      campaignRole: Option[String],
      primaryIdea: Option[String],
      compensationLead: Option[String],
      whyNow: Option[String],
      opponentReply: Option[String],
      executionHint: Option[String],
      longTermObjective: Option[String],
      keyTrigger: Option[String],
      compensationAnchor: Option[String],
      continuationFocus: Option[String]
  ):
    def nonEmptySections: List[(String, String)] =
      List(
        "Campaign role" -> campaignRole,
        "Primary idea" -> primaryIdea,
        "Compensation lead" -> compensationLead,
        "Why now" -> whyNow,
        "Opponent reply to watch" -> opponentReply,
        "Execution hint" -> executionHint,
        "Long-term objective" -> longTermObjective,
        "Key trigger or failure mode" -> keyTrigger,
        "Compensation anchor" -> compensationAnchor,
        "Continuation focus" -> continuationFocus
      ).collect { case (label, Some(value)) if value.trim.nonEmpty => label -> value }

  final case class Coverage(
      hasDominantIdea: Boolean,
      hasForwardPlan: Boolean,
      hasConcreteAnchor: Boolean,
      hasGroundedSignal: Boolean,
      hasOpponentOrTrigger: Boolean,
      hasCampaignOwner: Boolean
  )

  private val PieceNames = Map(
    "P" -> "pawn",
    "N" -> "knight",
    "B" -> "bishop",
    "R" -> "rook",
    "Q" -> "queen",
    "K" -> "king"
  )

  private val ForwardCuePatterns = List(
    """\b(should|must|needs? to|want(?:s)? to|aim(?:s)? to|plan(?:s)? to|prepare(?:s)? to|look(?:s)? to|tries? to)\b""",
    """\b(can then|so that|before [^.!?]{0,48}\bcan\b|if [^.!?]{0,64}\bthen\b|once\b|next\b|follow(?:s)? with\b)\b""",
    """\b(reroute|reroutes|rerouting|expand|expands|expanding|clamp|clamps|clamping|targeting|pressing|challenge|challenges|challenging|consolidate|consolidates|consolidating|switch|switches|switching|convert|converts|converting|prevent|prevents|preventing|build(?:s|ing)? toward|head(?:s|ing)? toward)\b"""
  ).map(_.r)

  def build(
      strategyPack: Option[StrategyPack],
      dossier: Option[ActiveBranchDossier],
      routeRefs: List[ActiveStrategicRouteRef],
      moveRefs: List[ActiveStrategicMoveRef],
      currentFen: Option[String] = None
  ): Brief =
    val surface = StrategyPackSurface.from(strategyPack)
    val digest = strategyPack.flatMap(_.signalDigest)
    val dominantIdea = strategyPack.toList.flatMap(_.strategicIdeas).headOption
    val preferredSide = dominantIdea.map(_.ownerSide).orElse(strategyPack.map(_.sideToMove)).getOrElse("white")
    val currentBoard = currentFen.flatMap(parseBoard)
    val tacticalReality = immediateTacticalReality(currentBoard, preferredSide, moveRefs)
    val compensationNarrationEligible = CompensationDisplayPhrasing.compensationNarrationEligible(surface)
    val primaryIdea =
      surface.dominantIdeaText
        .orElse(
          dominantIdea
        .map(primaryIdeaLabel)
        )
        .map(value => surface.campaignOwnerText.filter(_ => surface.ownerMismatch).map(side => s"$side: $value").getOrElse(value))
        .flatMap(value => contextualizeSignal(Some(value), currentBoard, preferredSide))
    val compensationWhyNow =
      Option.when(compensationNarrationEligible)(CompensationDisplayPhrasing.compensationWhyNowText(surface)).flatten
    val compensationLead =
      Option.when(compensationNarrationEligible)(
        StrategyPackSurface
          .resolvedNormalizedCompensationLead(surface)
          .orElse(canonicalCompensationLead(surface))
      ).flatten
    val whyNow =
      contextualizeSignal(
        dedupe(
          pickFirst(
            tacticalReality,
            compensationWhyNow,
            dossier.flatMap(_.whyChosen),
            digest.flatMap(_.decision),
            digest.flatMap(_.structuralCue),
            digest.flatMap(_.dominantIdeaFocus).map(focus => s"The position is already pointing toward $focus."),
            digest.flatMap(_.practicalVerdict),
            dossier.flatMap(_.evidenceCue)
          ),
          primaryIdea
        ),
        currentBoard,
        preferredSide
      )
    val opponentReply =
      contextualizeSignal(
        pickFirst(
          dossier.flatMap(_.opponentResource),
          digest.flatMap(_.opponentPlan),
          digest.flatMap(_.prophylaxisThreat),
          dossier.flatMap(_.threadOpponentCounterplan)
        ),
        currentBoard,
        preferredSide
      )
    val executionHint =
      Option.when(compensationNarrationEligible)(
        StrategyPackSurface
          .resolvedNormalizedExecutionText(surface)
          .orElse(CompensationDisplayPhrasing.compensationExecutionTail(surface))
      ).flatten
        .orElse(selectExecutionHint(surface, strategyPack, dossier, routeRefs, dominantIdea))
    val longTermObjective =
      Option.when(compensationNarrationEligible)(
        StrategyPackSurface
          .resolvedNormalizedObjectiveText(surface)
          .orElse(CompensationDisplayPhrasing.compensationObjectiveText(surface))
          .orElse(StrategyPackSurface.resolvedNormalizedLongTermFocusText(surface))
      ).flatten
        .orElse(selectLongTermObjective(surface, strategyPack, dossier, dominantIdea, executionHint))
    val continuationFocus =
      contextualizeSignal(
        dedupe(
          pickFirst(
            dossier.flatMap(_.continuationFocus),
            longTermObjective,
            StrategyPackSurface.resolvedNormalizedLongTermFocusText(surface),
            surface.focusText
          ),
          executionHint
        ),
        currentBoard,
        preferredSide
      )
    val compensationAnchor =
      contextualizeSignal(
        selectCompensationAnchor(
          surface = surface,
          strategyPack = strategyPack,
          dossier = dossier,
          routeRefs = routeRefs,
          dominantIdea = dominantIdea,
          executionHint = executionHint,
          longTermObjective = longTermObjective,
          continuationFocus = continuationFocus
        ),
        currentBoard,
        preferredSide
      )
    val keyTrigger =
      contextualizeSignal(
        dedupe(
          pickFirst(
            dossier.flatMap(_.practicalRisk),
            dossier.flatMap(_.whyDeferred),
            digest.flatMap(_.latentReason),
            digest.flatMap(_.counterplayScoreDrop).map(cp => s"If the plan drifts, the counterplay can rise by about ${cp}cp."),
            strategyPack.flatMap(_.pieceMoveRefs.headOption.map(moveRefSummary)),
            moveRefs.headOption.flatMap(_.san.map(san => s"The follow-up still depends on getting ${san.trim} into the right structure."))
          ),
          primaryIdea,
          whyNow,
          opponentReply,
          executionHint,
          longTermObjective
        ),
        currentBoard,
        preferredSide
      )
    Brief(
      campaignRole =
        surface.campaignOwnerText.filter(_ => surface.ownerMismatch).map(side =>
          (List(side + "'s campaign") ++ dossier.flatMap(_.threadStage).flatMap(stageRoleDescription).toList).mkString(": ")
        ).orElse(dossier.flatMap(_.threadStage).flatMap(stageRoleDescription)),
      primaryIdea = primaryIdea,
      compensationLead = compensationLead,
      whyNow = whyNow,
      opponentReply = opponentReply,
      executionHint = executionHint,
      longTermObjective = longTermObjective,
      keyTrigger = keyTrigger,
      compensationAnchor = compensationAnchor,
      continuationFocus = continuationFocus
    )

  def evaluateCoverage(text: String, brief: Brief): Coverage =
    val normalizedText = normalize(text)
    val textTokens = StrategicSignalMatcher.signalTokens(normalizedText)

    def mentioned(signal: Option[String]): Boolean =
      signal.exists(signalMentioned(normalizedText, textTokens, _))

    def explicitlyMentioned(signal: Option[String]): Boolean =
      signal.exists { value =>
        val normalizedSignal = normalize(value)
        normalizedSignal.nonEmpty && (
          StrategicSignalMatcher.phraseMentioned(normalizedText, normalizedSignal) ||
            StrategicSignalMatcher.containsComparablePhrase(normalizedText, value)
        )
      }

    val dominantIdeaMentioned =
      brief.primaryIdea.exists(primary =>
        StrategicSignalMatcher.phraseMentioned(normalizedText, normalize(primary)) ||
          StrategicSignalMatcher.signalTokens(normalize(primary)).intersect(textTokens).size >= 2
      )
    val campaignOwnerMentioned =
      brief.campaignRole.exists(role =>
        (normalize(role).contains("white") && normalizedText.contains("white")) ||
          (normalize(role).contains("black") && normalizedText.contains("black"))
      ) || brief.primaryIdea.exists(primary =>
        (normalize(primary).contains("white") && normalizedText.contains("white")) ||
          (normalize(primary).contains("black") && normalizedText.contains("black"))
      )

    val forwardCue = ForwardCuePatterns.exists(_.findFirstIn(normalizedText).nonEmpty)
    val structuralSequenceCue =
      List("before", "then", "next", "once", "after", "if").exists(word => normalizedText.contains(s" $word ")) ||
        normalizedText.startsWith("if ")
    val objectiveCue =
      explicitlyMentioned(brief.longTermObjective) ||
        explicitlyMentioned(brief.continuationFocus) ||
        brief.longTermObjective.exists(_ => normalizedText.contains("work toward") || normalizedText.contains("making")) ||
        brief.continuationFocus.exists(_ => normalizedText.contains("next step") || normalizedText.contains("work toward"))
    val executionCueMentioned = explicitlyMentioned(brief.executionHint)
    val compensationAnchorMentioned =
      explicitlyMentioned(brief.compensationAnchor) || hasConcreteCompensationAnchor(normalizedText)

    Coverage(
      hasDominantIdea = dominantIdeaMentioned,
      hasForwardPlan =
        forwardCue ||
          (
            (dominantIdeaMentioned || executionCueMentioned || objectiveCue || compensationAnchorMentioned) &&
              (structuralSequenceCue || executionCueMentioned || objectiveCue)
          ),
      hasConcreteAnchor = compensationAnchorMentioned,
      hasGroundedSignal = dominantIdeaMentioned || compensationAnchorMentioned,
      hasOpponentOrTrigger = mentioned(brief.opponentReply) || mentioned(brief.keyTrigger),
      hasCampaignOwner = campaignOwnerMentioned
    )

  def buildDeterministicNote(
      strategyPack: Option[StrategyPack],
      dossier: Option[ActiveBranchDossier],
      routeRefs: List[ActiveStrategicRouteRef],
      moveRefs: List[ActiveStrategicMoveRef],
      currentFen: Option[String] = None
  ): Option[String] =
    renderDeterministicNote(build(strategyPack, dossier, routeRefs, moveRefs, currentFen))

  def buildStrictCompensationFallbackNote(
      strategyPack: Option[StrategyPack],
      dossier: Option[ActiveBranchDossier],
      routeRefs: List[ActiveStrategicRouteRef],
      moveRefs: List[ActiveStrategicMoveRef],
      currentFen: Option[String] = None
  ): Option[String] =
    val surface = StrategyPackSurface.from(strategyPack)
    Option.when(LlmApi.activeCompensationNoteExpected(surface)) {
      renderStrictCompensationFallback(
        brief = build(strategyPack, dossier, routeRefs, moveRefs, currentFen),
        surface = surface
      )
    }.flatten

  def renderDeterministicNote(brief: Brief): Option[String] =
    val roleSentence =
      brief.campaignRole
        .flatMap(cleanStringSignal)
        .filter(role => role.toLowerCase.contains("'s campaign") || brief.whyNow.isEmpty)
        .map(asSentence)

    val planSentence =
      for
        primary <- brief.primaryIdea.flatMap(playerFacingSentence)
      yield
        val reasonSentence =
          brief.whyNow
            .flatMap(playerFacingSentence)
            .map(asSentence)
        val normalizedReason = reasonSentence.map(normalize).getOrElse("")
        val compensationSentence =
          brief.compensationLead
            .flatMap(playerFacingSentence)
            .filterNot(lead => normalize(primary) == normalize(lead))
            .filterNot(lead => normalizedReason.contains(normalize(lead)))
            .map(lead => asSentence(s"The compensation comes from ${stripTrailingPunctuation(lead)}."))
        val planFollowup =
          List(
            compensationSentence,
            playerFacingSentence(s"The key idea is $primary.").map(asSentence),
            brief.executionHint
              .flatMap(playerFacingSentence)
              .filterNot(ex => normalize(ex) == normalize(primary))
              .filterNot(ex => compensationSentence.exists(sentence => normalize(sentence).contains(normalize(ex))))
              .filterNot(ex => normalizedReason.contains(normalize(ex)))
              .map(ex => asSentence(s"A likely follow-up is $ex.")),
            brief.longTermObjective
              .flatMap(playerFacingSentence)
              .filterNot(obj => normalize(obj) == normalize(primary))
              .filterNot(obj => compensationSentence.exists(sentence => normalize(sentence).contains(normalize(obj))))
              .filterNot(obj => normalizedReason.contains(normalize(obj)))
              .map(obj => asSentence(s"A concrete target is ${stripLeadingObjective(stripTrailingPunctuation(obj))}.")),
            brief.continuationFocus
              .flatMap(playerFacingSentence)
              .filterNot(signal => normalize(signal) == normalize(primary))
              .filterNot(signal => compensationSentence.exists(sentence => normalize(sentence).contains(normalize(signal))))
              .filterNot(signal => normalizedReason.contains(normalize(signal)))
              .map(renderCompensationContinuationSentence)
          ).flatten.take(2).mkString(" ")
        reasonSentence.map(sentence => s"$sentence $planFollowup").getOrElse(planFollowup)

    val cautionSentence =
      (brief.opponentReply.flatMap(cleanStringSignal), brief.keyTrigger.flatMap(cleanStringSignal)) match
        case (Some(reply), Some(trigger)) =>
          Some(
            s"${asSentence(capitalizeSentenceStart(stripTrailingPunctuation(reply)))} ${asSentence(capitalizeSentenceStart(stripTrailingPunctuation(trigger)))}"
          )
        case (Some(reply), None) =>
          Some(asSentence(capitalizeSentenceStart(stripTrailingPunctuation(reply))))
        case (None, Some(trigger)) =>
          Some(asSentence(capitalizeSentenceStart(stripTrailingPunctuation(trigger))))
        case _ =>
          brief.longTermObjective
            .flatMap(playerFacingSentence)
            .map(obj => asSentence(s"The long-term objective is to ${stripTrailingPunctuation(obj)}."))
            .orElse(brief.continuationFocus.flatMap(playerFacingSentence).map(renderCompensationContinuationSentence))

    val sentences =
      (roleSentence.toList ++ planSentence.toList ++ cautionSentence.toList)
        .map(_.trim)
        .filter(_.nonEmpty)
        .distinct
        .take(3)

    Option.when(sentences.nonEmpty)(sentences.mkString(" "))

  private def renderStrictCompensationFallback(
      brief: Brief,
      surface: StrategyPackSurface.Snapshot
  ): Option[String] =
    val resolvedExecution = StrategyPackSurface.resolvedNormalizedExecutionText(surface).orElse(surface.executionText)
    val resolvedObjective = StrategyPackSurface.resolvedNormalizedObjectiveText(surface).orElse(surface.objectiveText)
    val resolvedFocus = StrategyPackSurface.resolvedNormalizedLongTermFocusText(surface).orElse(surface.focusText)
    val leadSentence =
      brief.compensationLead
        .flatMap(cleanStringSignal)
        .orElse(StrategyPackSurface.resolvedNormalizedCompensationLead(surface).flatMap(cleanStringSignal))
        .orElse(canonicalCompensationLead(surface))
        .map(lead => asSentence(s"The compensation comes from ${stripTrailingPunctuation(lead)}."))

    val primaryIdea =
      brief.primaryIdea
        .flatMap(cleanStringSignal)
        .orElse(surface.dominantIdeaText.flatMap(cleanStringSignal))
        .orElse(resolvedFocus.flatMap(cleanStringSignal))
    val ideaSentence =
      primaryIdea.map(idea => asSentence(s"The key idea is ${stripTrailingPunctuation(idea)}."))

    val anchorSignal =
      pickFirst(
        brief.compensationAnchor,
        resolvedExecution.filter(hasConcreteCompensationAnchor),
        brief.executionHint.filter(hasConcreteCompensationAnchor),
        resolvedObjective.filter(hasConcreteCompensationAnchor),
        brief.longTermObjective.filter(hasConcreteCompensationAnchor),
        brief.continuationFocus.filter(hasConcreteCompensationAnchor),
        resolvedFocus.filter(hasConcreteCompensationAnchor)
      )
    val anchorSentence =
      anchorSignal
        .flatMap(cleanStringSignal)
        .filter(hasConcreteCompensationAnchor)
        .filterNot(anchor => primaryIdea.exists(idea => normalize(idea) == normalize(anchor)))
        .filterNot(anchor => leadSentence.exists(sentence => normalize(sentence).contains(normalize(anchor))))
        .map(anchor => asSentence(s"That pressure is anchored on ${stripTrailingPunctuation(anchor)}."))

    val continuationSentence =
      selectCompensationContinuationSignal(brief, surface)
        .orElse(
          pickFirst(
            resolvedExecution,
            resolvedObjective,
            resolvedFocus,
            surface.executionText,
            surface.objectiveText,
            surface.focusText
          )
        )
        .flatMap(cleanStringSignal)
        .filterNot(signal => primaryIdea.exists(idea => normalize(idea) == normalize(signal)))
        .filterNot(signal => anchorSignal.exists(anchor => normalize(anchor) == normalize(signal)))
        .filterNot(signal => leadSentence.exists(sentence => normalize(sentence).contains(normalize(signal))))
        .map(renderCompensationContinuationSentence)

    val whyNowSentence =
      brief.whyNow
        .flatMap(cleanStringSignal)
        .filter(text => normalize(text).nonEmpty)
        .filterNot(text => leadSentence.exists(sentence => normalize(sentence).contains(normalize(text))))
        .filterNot(text => anchorSignal.exists(anchor => normalize(anchor) == normalize(text)))
        .map(asSentence)

    val sentences =
      List(leadSentence, ideaSentence.orElse(whyNowSentence), anchorSentence, continuationSentence)
        .flatten
        .map(_.trim)
        .filter(_.nonEmpty)
        .distinct
        .take(4)

    val ideaSatisfied = primaryIdea.isEmpty || ideaSentence.nonEmpty
    Option.when(leadSentence.nonEmpty && anchorSentence.nonEmpty && continuationSentence.nonEmpty && ideaSatisfied) {
      sentences.mkString(" ")
    }

  private def canonicalCompensationLead(surface: StrategyPackSurface.Snapshot): Option[String] =
    CompensationContractMatcher
      .canonicalSubtype(surface)
      .map { subtype =>
        val theater =
          subtype.pressureTheater match
            case "queenside" => "queenside"
            case "center"    => "central"
            case "kingside"  => "kingside"
            case other       => other
        subtype.pressureMode match
          case "line_occupation" =>
            if theater == "central" then "central file pressure" else s"$theater file pressure"
          case "target_fixing" =>
            if theater == "central" then "pressure against fixed central targets"
            else s"$theater pressure against fixed targets"
          case "counterplay_denial" =>
            s"denying $theater counterplay"
          case "defender_tied_down" =>
            s"keeping the defender tied down on the $theater"
          case "conversion_window" =>
            s"$theater conversion pressure"
          case "break_preparation" =>
            s"keeping the $theater break ready"
          case _ =>
            s"$theater compensation"
      }
      .flatMap(cleanStringSignal)

  private def pickFirst(values: Option[String]*): Option[String] =
    values.iterator.flatMap(cleanSignal).toSeq.headOption

  private def dedupe(value: Option[String], others: Option[String]*): Option[String] =
    value.filter { current =>
      val normalized = normalize(current)
      normalized.nonEmpty && !others.exists(other => normalize(other.getOrElse("")) == normalized)
    }

  private def cleanSignal(raw: Option[String]): Option[String] =
    raw.flatMap { value =>
      val sanitized =
        UserFacingSignalSanitizer.sanitize(normalizeExecutionLikeSignal(naturalizeLabel(value)))
      Option(sanitized).map(_.trim).filter(_.nonEmpty)
    }

  private def cleanStringSignal(raw: String): Option[String] =
    cleanSignal(Some(raw))

  private def playerFacingSentence(raw: String): Option[String] =
    cleanStringSignal(raw)
      .map(LiveNarrativeCompressionCore.rewritePlayerLanguage)
      .flatMap(cleanStringSignal)
      .filter(LiveNarrativeCompressionCore.keepPlayerFacingSentence)
      .filterNot(LiveNarrativeCompressionCore.isLowValueNarrativeSentence)

  private def contextualizeSignal(raw: Option[String], board: Option[Board], side: String): Option[String] =
    raw.flatMap { value =>
      cleanSignal(Some(rewriteOccupiedSquareLanguage(value, board, side)))
    }

  private def signalMentioned(normalizedText: String, textTokens: Set[String], signal: String): Boolean =
    val normalizedSignal = normalize(signal)
    if normalizedSignal.isEmpty then false
    else
      StrategicSignalMatcher.phraseMentioned(normalizedText, normalizedSignal) ||
        StrategicSignalMatcher.signalTokens(normalizedSignal).intersect(textTokens).nonEmpty

  private def normalize(raw: String): String =
    Option(raw).getOrElse("").trim.toLowerCase

  private def stripTrailingPunctuation(text: String): String =
    Option(text).getOrElse("").trim.stripSuffix(".").stripSuffix("!").stripSuffix("?").trim

  private def capitalizeSentenceStart(text: String): String =
    val trimmed = Option(text).getOrElse("").trim
    if trimmed.isEmpty then trimmed
    else s"${trimmed.head.toUpper}${trimmed.tail}"

  private def lowercaseSentenceStart(text: String): String =
    val trimmed = Option(text).getOrElse("").trim
    if trimmed.isEmpty then trimmed
    else s"${trimmed.head.toLower}${trimmed.tail}"

  private def asSentence(text: String): String =
    val trimmed = Option(text).getOrElse("").trim
    if trimmed.isEmpty then trimmed
    else if ".!?".contains(trimmed.last) then trimmed else s"$trimmed."

  private def naturalizeLabel(raw: String): String =
    Option(raw)
      .map(_.trim)
      .filter(_.nonEmpty)
      .map(
        _.replace("->", " leading to ")
          .replaceAll("(?i)\\b([a-h])-break\\s+Break\\b", "$1-break")
          .replaceAll("\\s+", " ")
      )
      .getOrElse("")

  private def pieceName(code: String): String =
    val normalized = Option(code).map(_.trim).getOrElse("")
    val upper = normalized.toUpperCase
    PieceNames.get(upper)
      .orElse(
        upper.split("\\s+").toList.reverse.collectFirst(Function.unlift(PieceNames.get))
      )
      .orElse {
        val lowered = normalized.toLowerCase
        List("pawn", "knight", "bishop", "rook", "queen", "king").find(lowered.contains)
      }
      .getOrElse("piece")

  private def primaryIdeaLabel(idea: StrategyIdeaSignal): String =
    val ideaLabel = StrategicIdeaSelector.playerFacingIdeaText(idea)
    cleanSignal(Some(ideaLabel)).getOrElse(ideaLabel)

  private def selectExecutionHint(
      surface: StrategyPackSurface.Snapshot,
      strategyPack: Option[StrategyPack],
      dossier: Option[ActiveBranchDossier],
      routeRefs: List[ActiveStrategicRouteRef],
      dominantIdea: Option[StrategyIdeaSignal]
  ): Option[String] =
    val preferredSide = dominantIdea.map(_.ownerSide).orElse(strategyPack.map(_.sideToMove))
    pickFirst(
      surface.executionText,
      dossier.flatMap(_.routeCue).filter(cue => preferredSide.forall(_ == cue.ownerSide)).map(routeCueSummary),
      routeRefs.find(ref => preferredSide.forall(_ == ref.ownerSide)).map(routeRefSummary),
      strategyPack.flatMap(
        _.pieceRoutes.find(route =>
          route.surfaceMode != RouteSurfaceMode.Hidden && preferredSide.forall(_ == route.ownerSide)
        ).map(routeSummary)
      ),
      strategyPack.flatMap(
        _.pieceMoveRefs.find(moveRef => preferredSide.forall(_ == moveRef.ownerSide)).map(moveRefSummary)
      ),
      strategyPack.flatMap(
        _.directionalTargets.find(target => preferredSide.forall(_ == target.ownerSide)).flatMap(targetSummary)
      )
    )

  private def selectLongTermObjective(
      surface: StrategyPackSurface.Snapshot,
      strategyPack: Option[StrategyPack],
      dossier: Option[ActiveBranchDossier],
      dominantIdea: Option[StrategyIdeaSignal],
      executionHint: Option[String]
  ): Option[String] =
    val preferredSide = dominantIdea.map(_.ownerSide).orElse(strategyPack.map(_.sideToMove))
    val executionDestination = extractLastSquare(executionHint.getOrElse(""))
    pickFirst(
      surface.objectiveText,
      strategyPack.toList
        .flatMap(_.directionalTargets)
        .find(target =>
          preferredSide.forall(_ == target.ownerSide) &&
            !executionDestination.contains(target.targetSquare)
        )
        .flatMap { target =>
          cleanSignal(Some(s"${pieceName(target.piece)} can use ${target.targetSquare}"))
        },
      dossier.flatMap(_.continuationFocus),
      surface.focusText
    )

  private def selectCompensationAnchor(
      surface: StrategyPackSurface.Snapshot,
      strategyPack: Option[StrategyPack],
      dossier: Option[ActiveBranchDossier],
      routeRefs: List[ActiveStrategicRouteRef],
      dominantIdea: Option[StrategyIdeaSignal],
      executionHint: Option[String],
      longTermObjective: Option[String],
      continuationFocus: Option[String]
  ): Option[String] =
    val preferredSide = dominantIdea.map(_.ownerSide).orElse(strategyPack.map(_.sideToMove))
    val canonicalSubtype = CompensationContractMatcher.canonicalSubtype(surface)
    pickFirst(
      canonicalSubtype.flatMap(subtype => StrategyPackSurface.alignedDirectionalTarget(surface, subtype).flatMap(targetSummary)),
      canonicalSubtype.flatMap(subtype => StrategyPackSurface.alignedMoveRef(surface, subtype).map(moveRefSummary)),
      canonicalSubtype.flatMap(subtype => StrategyPackSurface.alignedRoute(surface, subtype).map(routeSummary)),
      dossier.flatMap(_.routeCue).filter(cue => preferredSide.forall(_ == cue.ownerSide)).map(routeCueSummary),
      routeRefs.find(ref => preferredSide.forall(_ == ref.ownerSide)).map(routeRefSummary),
      executionHint.filter(hasConcreteCompensationAnchor),
      longTermObjective.filter(hasConcreteCompensationAnchor),
      continuationFocus.filter(hasConcreteCompensationAnchor),
      surface.executionText.filter(hasConcreteCompensationAnchor),
      surface.objectiveText.filter(hasConcreteCompensationAnchor),
      surface.focusText.filter(hasConcreteCompensationAnchor)
    ).filter(hasConcreteCompensationAnchor)

  private def selectCompensationContinuationSignal(
      brief: Brief,
      surface: StrategyPackSurface.Snapshot
  ): Option[String] =
    pickFirst(
      brief.executionHint,
      brief.longTermObjective,
      brief.continuationFocus,
      surface.executionText,
      surface.objectiveText,
      surface.focusText
    )

  private def routeCueSummary(cue: ActiveBranchRouteCue): String =
    routeLabel(
      ownerSide = None,
      piece = cue.piece,
      route = cue.route,
      purpose = Some(cue.purpose),
      surfaceMode = cue.surfaceMode
    )

  private def routeRefSummary(routeRef: ActiveStrategicRouteRef): String =
    routeLabel(
      ownerSide = None,
      piece = routeRef.piece,
      route = routeRef.route,
      purpose = Some(routeRef.purpose),
      surfaceMode = routeRef.surfaceMode
    )

  private def routeSummary(route: StrategyPieceRoute): String =
    routeLabel(
      ownerSide = None,
      piece = route.piece,
      route = route.route,
      purpose = Some(route.purpose),
      surfaceMode = route.surfaceMode
    )

  private def moveRefSummary(moveRef: StrategyPieceMoveRef): String =
    cleanSignal(Some(s"${pieceName(moveRef.piece)} toward ${moveRef.target} ${purposeClause(moveRef.idea, moveRef.target)}"))
      .orElse(cleanSignal(Some(moveRef.idea)))
      .getOrElse(moveRef.idea)

  private def targetSummary(target: StrategyDirectionalTarget): Option[String] =
    cleanSignal(Some(s"${pieceName(target.piece)} can use ${target.targetSquare}"))

  private def renderCompensationContinuationSentence(raw: String): String =
    val stripped = stripTrailingPunctuation(stripContinuationLead(stripLeadingObjective(raw)))
    val normalized = normalize(stripped)
    val body =
      if startsWithContinuationAction(normalized) then lowercaseSentenceStart(stripped)
      else if hasConcreteCompensationAnchor(normalized) || normalized.contains("pressure") || normalized.contains("target") then
        s"keep ${strippedLeadingArticle(stripped)}"
      else lowercaseSentenceStart(stripped)
    asSentence(s"From there, $body")

  private def routeLabel(
      ownerSide: Option[String],
      piece: String,
      route: List[String],
      purpose: Option[String],
      surfaceMode: String
  ): String =
    val destination =
      route
        .map(_.trim.toLowerCase)
        .filter(_.matches("^[a-h][1-8]$"))
        .lastOption
    val deploymentText =
      if surfaceMode == RouteSurfaceMode.Exact && route.nonEmpty then s"${pieceName(piece)} via ${route.mkString("-")}"
      else
        destination match
          case Some(square) => s"${pieceName(piece)} toward $square"
          case None         => s"${pieceName(piece)} redeployment"
    val sidePrefix = cleanSignal(ownerSide).map(_ + " ").getOrElse("")
    val prefixedDeployment = s"$sidePrefix$deploymentText".trim
    cleanSignal(purpose)
      .map(text => s"$prefixedDeployment ${purposeClause(text, destination.orNull)}")
      .getOrElse(prefixedDeployment)

  private def purposeClause(raw: String, targetSquare: String | Null = null): String =
    val cleaned = stripTrailingPunctuation(cleanSignal(Some(raw)).getOrElse(raw))
    if cleaned.isEmpty then ""
    else
      val normalizedIdea = normalize(cleaned)
      val target = Option(targetSquare).map(_.trim.toLowerCase).filter(_.matches("^[a-h][1-8]$"))
      if normalizedIdea.startsWith("keep the ") then
        target.filter(square => normalizedIdea.endsWith(square))
          .map(_ => "to keep the pressure fixed there")
          .getOrElse(s"to $cleaned")
      else if normalizedIdea.startsWith("contest ") || normalizedIdea.startsWith("attack ") || normalizedIdea.startsWith("build ") ||
        normalizedIdea.startsWith("prepare ") || normalizedIdea.startsWith("improve ") || normalizedIdea.startsWith("activate ") ||
        normalizedIdea.startsWith("reroute ") || normalizedIdea.startsWith("stabilize ") || normalizedIdea.startsWith("consolidate ") ||
        normalizedIdea.startsWith("pressure ") || normalizedIdea.startsWith("occupy ") then
        s"to $cleaned"
      else if normalizedIdea.contains("coordination improvement") then
        "to improve coordination"
      else if normalizedIdea.contains("plan activation lane") then
        "to activate the plan"
      else if normalizedIdea.contains("open file occupation") then
        "to occupy the open file"
      else if normalizedIdea.contains("semi-open file occupation") then
        "to occupy the semi-open file"
      else if normalizedIdea.contains("kingside pressure") then
        "to increase kingside pressure"
      else if normalizedIdea.contains("queenside pressure") then
        "to increase queenside pressure"
      else if normalizedIdea.contains("clamp") then
        s"to reinforce ${withArticle(strippedLeadingArticle(cleaned))}"
      else if normalizedIdea.contains("circuit") then
        s"to improve ${withArticle(strippedLeadingArticle(cleaned))}"
      else if normalizedIdea.contains("contest the ") then
        target.filter(square => normalizedIdea.endsWith(square))
          .map(_ => "to contest the target there")
          .getOrElse(s"to $cleaned")
      else s"to support ${strippedLeadingArticle(cleaned)}"

  private def normalizeExecutionLikeSignal(text: String): String =
    val trimmed = Option(text).getOrElse("").trim
    if trimmed.isEmpty then trimmed
    else
      val executionLike =
        """(?i)^(?:(white|black)\s+)?([pnbrqk]|pawn|knight|bishop|rook|queen|king)\s+toward\s+([a-h][1-8])\s+for\s+(.+)$""".r
      trimmed match
        case executionLike(_, pieceToken, square, purpose) =>
          UserFacingSignalSanitizer
            .sanitize(s"${pieceName(pieceToken)} toward ${square.toLowerCase} ${purposeClause(purpose, square.toLowerCase)}")
            .trim
        case _ => trimmed

  private def stripLeadingObjective(text: String): String =
    text
      .stripPrefix("keep the initiative alive while working toward ")
      .stripPrefix("Keep the initiative alive while working toward ")
      .stripPrefix("keep the initiative alive while ")
      .stripPrefix("Keep the initiative alive while ")
      .stripPrefix("work toward ")
      .stripPrefix("Work toward ")
      .stripPrefix("working toward ")
      .stripPrefix("Working toward ")

  private def stripContinuationLead(text: String): String =
    text
      .stripPrefix("a likely follow-up is ")
      .stripPrefix("A likely follow-up is ")
      .stripPrefix("the next step is ")
      .stripPrefix("The next step is ")
      .stripPrefix("a concrete target is ")
      .stripPrefix("A concrete target is ")
      .trim

  private def strippedLeadingArticle(text: String): String =
    text.replaceFirst("(?i)^(a|an|the)\\s+", "").trim

  private def withArticle(text: String): String =
    val trimmed = text.trim
    if trimmed.isEmpty then trimmed
    else if trimmed.matches("(?i)^(a|an|the)\\s+.*") then trimmed
    else s"the $trimmed"

  def stageRoleDescription(rawStage: String): Option[String] =
    Option(rawStage)
      .map(_.trim.toLowerCase)
      .filter(_.nonEmpty)
      .map {
        case "seed"    => "the plan is only starting to take shape"
        case "build"   => "the plan is being consolidated move by move"
        case "switch"  => "the game is pivoting toward a new sector or target"
        case "convert" => "the accumulated pressure should now turn into something concrete"
        case other     => naturalizeLabel(other)
      }
      .flatMap(text => cleanSignal(Some(text)))

  private def extractLastSquare(text: String): Option[String] =
    """\b([a-h][1-8])\b""".r.findAllMatchIn(Option(text).getOrElse("").toLowerCase).map(_.group(1)).toList.lastOption

  private def hasConcreteCompensationAnchor(text: String): Boolean =
    val normalized = normalize(text)
    val hasSquare = """\b[a-h][1-8]\b""".r.findFirstIn(normalized).nonEmpty
    val hasFile = """\b[a-h]-file\b""".r.findFirstIn(normalized).nonEmpty
    val hasPieceRoute =
      """\b(pawn|knight|bishop|rook|queen|king)\s+(?:toward|via|can use|head(?:s)? for)\b""".r.findFirstIn(normalized).nonEmpty
    hasSquare ||
    hasFile ||
    hasPieceRoute ||
    normalized.contains("central files") ||
    normalized.contains("queenside files") ||
    normalized.contains("open file") ||
    normalized.contains("open files")

  private def startsWithContinuationAction(normalized: String): Boolean =
    List(
      "keep ",
      "bring ",
      "move ",
      "head ",
      "work toward ",
      "prepare ",
      "build ",
      "fix ",
      "occupy ",
      "target ",
      "press ",
      "switch ",
      "consolidate ",
      "improve "
    ).exists(normalized.startsWith) ||
      """^(pawn|knight|bishop|rook|queen|king)\s+(toward|to|via|can use)\b""".r.findFirstIn(normalized).nonEmpty

  private def parseBoard(fen: String): Option[Board] =
    Fen.read(Standard, Fen.Full(fen)).map(_.board)

  private def materialScore(board: Board, color: Color): Int =
    board.byPiece(color, Pawn).count +
      board.byPiece(color, Knight).count * 3 +
      board.byPiece(color, Bishop).count * 3 +
      board.byPiece(color, Rook).count * 5 +
      board.byPiece(color, Queen).count * 9

  private def sideColor(side: String): Color =
    if side == "white" then Color.White else Color.Black

  private def immediateTacticalReality(
      currentBoard: Option[Board],
      side: String,
      moveRefs: List[ActiveStrategicMoveRef]
  ): Option[String] =
    currentBoard.flatMap { before =>
      val activeColor = sideColor(side)
      val currentEdge = materialScore(before, activeColor) - materialScore(before, !activeColor)

      moveRefs
        .flatMap { ref =>
          val san = ref.san.map(_.trim).filter(_.nonEmpty)
          val afterEdge =
            ref.fenAfter.flatMap(parseBoard).map(after =>
              materialScore(after, activeColor) - materialScore(after, !activeColor)
            )
          val gain = afterEdge.map(_ - currentEdge).getOrElse(0)
          san.flatMap { move =>
            if gain > 0 then
              Some(
                (
                  20 + gain,
                  s"$move immediately wins ${materialGainLabel(gain)}${if isForcingSan(move) then " while forcing the issue" else ""}."
                )
              )
            else if isForcingSan(move) then Some(10 -> s"$move forces the issue immediately.")
            else None
          }
        }
        .sortBy { case (priority, _) => -priority }
        .headOption
        .map(_._2)
    }

  private def materialGainLabel(gain: Int): String =
    if gain >= 9 then "a queen"
    else if gain >= 5 then "a rook"
    else if gain >= 3 then "a piece"
    else if gain >= 1 then "a pawn"
    else "material"

  private def isForcingSan(san: String): Boolean =
    val trimmed = Option(san).map(_.trim).getOrElse("")
    trimmed.contains("+") || trimmed.contains("#")

  private def rewriteOccupiedSquareLanguage(
      text: String,
      board: Option[Board],
      side: String
  ): String =
    board.fold(text) { currentBoard =>
      val color = sideColor(side)

      def occupantLabel(squareKey: String): Option[String] =
        Square.all
          .find(_.key == squareKey)
          .flatMap(currentBoard.pieceAt)
          .filter(_.color == color)
          .map(piece => pieceName(roleToken(piece.role)))

      def preserveCapitalization(original: String, replacement: String): String =
        if original.headOption.exists(_.isUpper) then replacement.take(1).toUpperCase + replacement.drop(1)
        else replacement

      val focus = """(?i)\bfocus on ([a-h][1-8])\b""".r
      val pointing = """(?i)\bpointing toward ([a-h][1-8])\b""".r

      val step1 =
        focus.replaceAllIn(text, m =>
          occupantLabel(m.group(1).toLowerCase)
            .map(piece => preserveCapitalization(m.matched, s"keep the $piece anchored on ${m.group(1).toLowerCase}"))
            .getOrElse(m.matched)
        )

      pointing.replaceAllIn(step1, m =>
        occupantLabel(m.group(1).toLowerCase)
          .map(piece => preserveCapitalization(m.matched, s"the $piece already anchored on ${m.group(1).toLowerCase}"))
          .getOrElse(m.matched)
      )
    }

  private def roleToken(role: _root_.chess.Role): String =
    role match
      case Knight => "N"
      case Bishop => "B"
      case Rook   => "R"
      case Queen  => "Q"
      case Pawn   => "P"
      case _      => "K"
