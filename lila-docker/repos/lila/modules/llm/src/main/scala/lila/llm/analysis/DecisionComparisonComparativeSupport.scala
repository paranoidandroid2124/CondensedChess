package lila.llm.analysis

import lila.llm.StrategyPack
import lila.llm.model.NarrativeContext
import lila.llm.model.strategic.VariationLine

private[analysis] object DecisionComparisonComparativeSupport:

  val ExactTargetFixationSource = "exact_target_fixation_delta"

  private val AlternativeThresholdCp = 35

  def enrich(
      comparison: Option[DecisionComparison],
      ctx: NarrativeContext,
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
      exactTargetFixationLane(existing, ctx, mainBundle).fold(
        existing.copy(
          comparedMove = None,
          comparativeConsequence = None,
          comparativeSource = None
        )
      ) { lane =>
        existing.copy(
          comparedMove = Some(lane.comparedMove),
          comparativeConsequence = Some(lane.consequence),
          comparativeSource = Some(lane.source)
        )
      }
    }

  private final case class ComparativeLane(
      comparedMove: String,
      consequence: String,
      source: String
  )

  private def exactTargetFixationLane(
      comparison: DecisionComparison,
      ctx: NarrativeContext,
      mainBundle: Option[MainPathClaimBundle]
  ): Option[ComparativeLane] =
    for
      packet <- exactTargetFixationPacket(mainBundle)
      square <- exactTargetFixationSquare(packet)
      bestMove <- cleanMove(comparison.engineBestMove.orElse(comparison.chosenMove))
      comparedMove <- comparedMove(comparison, ctx, bestMove)
    yield ComparativeLane(
      comparedMove = comparedMove,
      consequence = s"$bestMove fixes $square as the target; $comparedMove leaves $square unfixed on the compared branch.",
      source = ExactTargetFixationSource
    )

  private def comparedMove(
      comparison: DecisionComparison,
      ctx: NarrativeContext,
      bestMove: String
  ): Option[String] =
    val chosenAlternative =
      Option.unless(comparison.chosenMatchesBest) {
        cleanMove(comparison.chosenMove).filter(move => !sameMoveToken(move, bestMove))
      }.flatten
    chosenAlternative.orElse(topAlternativeMove(ctx, bestMove))

  private def topAlternativeMove(
      ctx: NarrativeContext,
      bestMove: String
  ): Option[String] =
    ctx.engineEvidence
      .flatMap(_.alternatives(AlternativeThresholdCp).headOption)
      .flatMap(leadSan(ctx.fen, _))
      .filter(move => !sameMoveToken(move, bestMove))

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
    line.ourMove.map(_.san).flatMap(cleanMove)
      .orElse {
        line.moves.headOption
          .map(move => NarrativeUtils.uciToSanOrFormat(fen, move))
          .flatMap(cleanMove)
      }

  private def sameMoveToken(left: String, right: String): Boolean =
    normalizeMoveToken(left) == normalizeMoveToken(right)

  private def normalizeMoveToken(raw: String): String =
    normalize(raw).replaceAll("""[+#?!]+$""", "").toLowerCase

  private def cleanMove(raw: Option[String]): Option[String] =
    raw.flatMap(cleanMove)

  private def cleanMove(raw: String): Option[String] =
    cleanText(raw)

  private def cleanText(raw: String): Option[String] =
    Option(raw).map(normalize).filter(_.nonEmpty)

  private def normalize(raw: String): String =
    Option(raw).getOrElse("").replaceAll("\\s+", " ").trim
