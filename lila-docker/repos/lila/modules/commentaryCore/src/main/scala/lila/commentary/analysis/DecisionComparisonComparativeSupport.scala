package lila.commentary.analysis

import lila.commentary.{ MoveReviewRefs, StrategyPack }
import lila.commentary.model.NarrativeContext
import lila.commentary.model.strategic.VariationLine

private[analysis] object DecisionComparisonComparativeSupport:

  val ExactTargetFixationSource = "exact_target_fixation_delta"
  val RoleAwareLineConsequenceSource = "role_aware_line_consequence"

  private val AlternativeThresholdCp = 35

  def enrich(
      comparison: Option[DecisionComparison],
      ctx: NarrativeContext,
      refs: Option[MoveReviewRefs] = None,
      strategyPack: Option[StrategyPack],
      truthContract: Option[DecisiveTruthContract],
      candidateEvidenceLines: List[String] = Nil,
      mainBundleOverride: Option[MainPathClaimBundle] = None
  ): Option[DecisionComparison] =
    val mainBundle =
      mainBundleOverride.orElse(
        MainPathMoveDeltaClaimBuilder.build(
          ctx = ctx,
          strategyPack = strategyPack,
          truthContract = truthContract,
          candidateEvidenceLines = candidateEvidenceLines
        )
    )
    comparison.map { existing =>
      exactTargetFixationLane(existing, ctx, mainBundle)
        .orElse(roleAwareLineConsequenceLane(existing, ctx, refs, truthContract))
        .fold(
          existing.copy(
            comparedMove = None,
            comparativeConsequence = None,
            comparativeSource = None,
            roleAwareBranchEvidence = None
          )
        ) { lane =>
          existing.copy(
            comparedMove = Some(lane.comparedMove),
            comparativeConsequence = Some(lane.consequence),
            comparativeSource = Some(lane.source),
            roleAwareBranchEvidence = lane.roleAwareBranchEvidence
          )
        }
    }

  def preservableComparativeSource(source: String): Boolean =
    val normalized = Option(source).getOrElse("").trim
    normalized == ExactTargetFixationSource || normalized == RoleAwareLineConsequenceSource

  def roleAwareLineConsequenceText(text: String): Boolean =
    val raw = Option(text).getOrElse("").trim
    val normalized = raw.toLowerCase
    normalized.contains("engine-best branch") &&
      normalized.contains("played branch") &&
      normalized.contains(" reaches ") &&
      LineScopedCitation.hasConcreteSanLine(raw) &&
      (
        normalized.contains("exchange sequence") ||
          normalized.contains("forcing check sequence") ||
          normalized.contains("material transition") ||
        normalized.contains("central pawn advance") ||
          normalized.contains("central break") ||
          normalized.contains("passed pawn") ||
          normalized.contains("promotion race")
      )

  def roleAwareLineConsequenceAllowed(
      truthContract: Option[DecisiveTruthContract]
  ): Boolean =
    truthContract.exists(contract =>
      contract.blocksStrategicSupport ||
        (
          contract.reasonFamily == DecisiveReasonKind.OnlyMoveDefense &&
            !contract.chosenMatchesBest &&
            contract.verifiedBestMove.exists(_.trim.nonEmpty)
        )
    )

  private final case class ComparativeLane(
      comparedMove: String,
      consequence: String,
      source: String,
      roleAwareBranchEvidence: Option[RoleAwareLineConsequenceEvidence] = None
  )

  private def exactTargetFixationLane(
      comparison: DecisionComparison,
      ctx: NarrativeContext,
      mainBundle: Option[MainPathClaimBundle]
  ): Option[ComparativeLane] =
    for
      packet <- exactTargetFixationPacket(mainBundle)
      square <- exactTargetFixationSquare(packet)
      bestMove <- DecisionComparisonBuilder
        .cleanMoveText(comparison.engineBestMove.orElse(comparison.chosenMove))
      comparedMove <- comparedMove(comparison, ctx, bestMove)
    yield ComparativeLane(
      comparedMove = comparedMove,
      consequence = s"$bestMove fixes $square as the target; $comparedMove leaves $square unfixed on the compared branch.",
      source = ExactTargetFixationSource
    )

  private def roleAwareLineConsequenceLane(
      comparison: DecisionComparison,
      ctx: NarrativeContext,
      refs: Option[MoveReviewRefs],
      truthContract: Option[DecisiveTruthContract]
  ): Option[ComparativeLane] =
    Option.when(roleAwareLineConsequenceAllowed(truthContract))(())
      .flatMap { _ =>
        for
          bestMove <- verifiedBestMove(comparison, truthContract)
          playedMove <- DecisionComparisonBuilder.cleanMoveText(comparison.chosenMove)
          if !sameMove(ctx, bestMove, playedMove)
          evidences = LineConsequenceEvaluator.fromRefsForRoleComparison(ctx, refs)
          bestEvidence <- evidences.find(lineLeadMatches(ctx, _, bestMove))
          playedEvidence <- evidences.find(lineLeadMatches(ctx, _, playedMove))
          if bestEvidence.narrativeReady && playedEvidence.narrativeReady
          if bestEvidence.kind != LineConsequenceKind.PreviewOnly
          if playedBranchComparable(bestEvidence, playedEvidence)
          if !lineContainsMove(ctx, playedEvidence, bestMove)
          consequence <- roleAwareConsequence(ctx, bestMove, playedMove, bestEvidence, playedEvidence, comparison)
        yield ComparativeLane(
          comparedMove = playedMove,
          consequence = consequence,
          source = RoleAwareLineConsequenceSource,
          roleAwareBranchEvidence =
            Some(
              RoleAwareLineConsequenceEvidence(
                engineBest = bestEvidence,
                played = playedEvidence
              )
            )
        )
      }

  private def playedBranchComparable(
      bestEvidence: LineConsequenceEvidence,
      playedEvidence: LineConsequenceEvidence
  ): Boolean =
    playedEvidence.kind == LineConsequenceKind.PreviewOnly ||
      (
        consequenceLabel(playedEvidence.kind).nonEmpty &&
          checkedEvidenceGapCp(Some(bestEvidence), Some(playedEvidence)).exists(_ >= AlternativeThresholdCp)
      )

  private def verifiedBestMove(
      comparison: DecisionComparison,
      truthContract: Option[DecisiveTruthContract]
  ): Option[String] =
    truthContract
      .flatMap(contract => contract.benchmarkMove.orElse(contract.verifiedBestMove))
      .orElse(
        comparison.deferredMove
          .filter(_ => comparison.deferredSource.exists(source => source.trim.equalsIgnoreCase("verified_best")))
      )
      .orElse(comparison.engineBestMove.filter(_ => comparison.deferredSource.exists(_.trim.equalsIgnoreCase("verified_best"))))
      .flatMap(DecisionComparisonBuilder.cleanMoveText)

  private def roleAwareConsequence(
      ctx: NarrativeContext,
      bestMove: String,
      playedMove: String,
      bestEvidence: LineConsequenceEvidence,
      playedEvidence: LineConsequenceEvidence,
      comparison: DecisionComparison
  ): Option[String] =
    val bestLabel = consequenceLabel(bestEvidence.kind)
    val bestMoveLabel = displayMove(ctx, bestMove)
    val playedMoveLabel = displayMove(ctx, playedMove)
    for
      label <- bestLabel
      bestLine <- formattedLine(ctx, bestEvidence)
      playedLine <- formattedLine(ctx, playedEvidence)
    yield consequenceLabel(playedEvidence.kind) match
      case None =>
        s"$bestMoveLabel reaches ${withArticle(label)} on the engine-best branch $bestLine; $playedMoveLabel stays on the played branch $playedLine without that concrete $label."
      case Some(playedLabel) =>
        val gapClause =
          comparisonGapCp(comparison, Some(bestEvidence), Some(playedEvidence))
            .map(gap => s", and the checked comparison favors the engine-best branch by about ${gap}cp")
            .getOrElse("")
        val playedDescription =
          if playedLabel == label then s"a different $playedLabel"
          else withArticle(playedLabel)
        s"$bestMoveLabel reaches ${withArticle(label)} on the engine-best branch $bestLine; $playedMoveLabel reaches $playedDescription on the played branch $playedLine$gapClause."

  private def displayMove(ctx: NarrativeContext, move: String): String =
    val cleaned = DecisionComparisonBuilder.cleanMoveText(Some(move)).getOrElse(Option(move).getOrElse("").trim)
    val normalized = NarrativeUtils.normalizeUciMove(cleaned)
    if normalized.matches("""[a-h][1-8][a-h][1-8][qrbn]?""") then
      NarrativeUtils.uciListToSan(ctx.fen, List(normalized)).headOption.getOrElse(cleaned)
    else cleaned

  private def comparisonGapCp(
      comparison: DecisionComparison,
      bestEvidence: Option[LineConsequenceEvidence],
      playedEvidence: Option[LineConsequenceEvidence]
  ): Option[Int] =
    checkedEvidenceGapCp(bestEvidence, playedEvidence)
      .orElse(comparison.cpLossVsChosen.map(math.abs).filter(_ > 0))

  private def checkedEvidenceGapCp(
      bestEvidence: Option[LineConsequenceEvidence],
      playedEvidence: Option[LineConsequenceEvidence]
  ): Option[Int] =
    for
      best <- bestEvidence.flatMap(_.scoreCp)
      played <- playedEvidence.flatMap(_.scoreCp)
      gap = math.abs(best - played)
      if gap > 0
    yield gap

  private def consequenceLabel(kind: LineConsequenceKind): Option[String] =
    kind match
      case LineConsequenceKind.ExchangeSequence     => Some("exchange sequence")
      case LineConsequenceKind.ForcingCheckSequence => Some("forcing check sequence")
      case LineConsequenceKind.CentralBreakTiming   => Some("central break")
      case LineConsequenceKind.CentralPawnAdvance   => Some("central pawn advance")
      case LineConsequenceKind.MaterialTransition   => Some("material transition")
      case LineConsequenceKind.PassedPawnCreation   => Some("passed pawn")
      case LineConsequenceKind.PromotionRace        => Some("promotion race")
      case LineConsequenceKind.PreviewOnly          => None

  private def withArticle(label: String): String =
    if label.headOption.exists(ch => "aeiou".contains(ch.toLower)) then s"an $label"
    else s"a $label"

  private def formattedLine(ctx: NarrativeContext, evidence: LineConsequenceEvidence): Option[String] =
    val startPly = NarrativeUtils.plyFromFen(ctx.fen).map(_ + 1).getOrElse(ctx.ply.max(1))
    val san = evidence.sanMoves.take(4).map(_.trim).filter(_.nonEmpty)
    Option.when(san.nonEmpty)(NarrativeUtils.formatSanWithMoveNumbers(startPly, san))

  private def lineLeadMatches(
      ctx: NarrativeContext,
      evidence: LineConsequenceEvidence,
      move: String
  ): Boolean =
    evidence.sanMoves.headOption.exists(san => sameMove(ctx, san, move)) ||
      evidence.uciMoves.headOption.exists(uci => sameMove(ctx, uci, move))

  private def lineContainsMove(
      ctx: NarrativeContext,
      evidence: LineConsequenceEvidence,
      move: String
  ): Boolean =
    val targetSan = normalizedSanToken(move)
    val targetUci = NarrativeUtils.sanToUci(ctx.fen, move).getOrElse(NarrativeUtils.normalizeUciMove(move))
    evidence.sanMoves.exists(san => normalizedSanToken(san) == targetSan) ||
      (
        targetUci.nonEmpty &&
          evidence.uciMoves.exists(uci => NarrativeUtils.uciEquivalent(NarrativeUtils.normalizeUciMove(uci), targetUci))
      )

  private def normalizedSanToken(move: String): String =
    DecisionComparisonBuilder
      .cleanMoveText(move)
      .getOrElse(Option(move).getOrElse(""))
      .replaceAll("""[+#?!]""", "")
      .trim
      .toLowerCase

  private def sameMove(ctx: NarrativeContext, left: String, right: String): Boolean =
    val leftUci = NarrativeUtils.sanToUci(ctx.fen, left).getOrElse(NarrativeUtils.normalizeUciMove(left))
    val rightUci = NarrativeUtils.sanToUci(ctx.fen, right).getOrElse(NarrativeUtils.normalizeUciMove(right))
    NarrativeUtils.uciEquivalent(leftUci, rightUci)

  private def comparedMove(
      comparison: DecisionComparison,
      ctx: NarrativeContext,
      bestMove: String
  ): Option[String] =
    val chosenAlternative =
      Option.unless(comparison.chosenMatchesBest) {
        DecisionComparisonBuilder.cleanMoveText(comparison.chosenMove)
          .filter(move => !DecisionComparisonBuilder.equalMoveToken(move, bestMove))
      }.flatten
    chosenAlternative.orElse(topAlternativeMove(ctx, bestMove))

  private def topAlternativeMove(
      ctx: NarrativeContext,
      bestMove: String
  ): Option[String] =
    ctx.engineEvidence
      .flatMap(_.alternatives(AlternativeThresholdCp).headOption)
      .flatMap(leadSan(ctx.fen, _))
      .filter(move => !DecisionComparisonBuilder.equalMoveToken(move, bestMove))

  private def exactTargetFixationPacket(
      mainBundle: Option[MainPathClaimBundle]
  ): Option[PlayerFacingClaimPacket] =
    mainBundle.flatMap(_.mainClaim).flatMap(_.packet)
      .filter(PlayerFacingTruthModePolicy.certifiedExactTargetFixationPacket)

  private def exactTargetFixationSquare(
      packet: PlayerFacingClaimPacket
  ): Option[String] =
    PlayerFacingTruthModePolicy.exactSliceTargetSquare(packet)

  private def leadSan(fen: String, line: VariationLine): Option[String] =
    LineScopedCitation.sanMoves(fen, line).headOption.flatMap(DecisionComparisonBuilder.cleanMoveText)
