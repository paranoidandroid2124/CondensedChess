package lila.chessjudgment.analysis.qc

import lila.chessjudgment.analysis.assembly.MoveReviewJudgmentResult

final case class MoveReviewQualitySample(
    sampleId: String,
    result: Option[MoveReviewJudgmentResult]
)

final case class LayerGapAggregate(
    layer: JudgmentGraphLayer,
    sampleCount: Int,
    totalSlots: Int,
    missingSlots: Int,
    gapPercent: Double,
    missingBySlot: Map[JudgmentGraphSlot, Int],
    missingByOwner: Map[JudgmentGraphOwner, Int]
)

final case class CorpusIssueAggregate(
    kind: ChessQualityIssueKind,
    count: Int
)

final case class SemanticCoverageAggregate(
    sampleCount: Int,
    tacticalIdeaSamples: Int,
    strategicIdeaSamples: Int,
    pawnStructureIdeaSamples: Int,
    openingIdeaSamples: Int,
    defensiveIdeaSamples: Int,
    evaluationIdeaSamples: Int,
    conversionIdeaSamples: Int,
    relativeAssessmentSamples: Int,
    candidateSetComparisonSamples: Int,
    onlyMoveSignalSamples: Int,
    forcedLineThemeSamples: Int
)

final case class ClaimPromotionAggregate(
    sampleCount: Int,
    totalIdeas: Int,
    totalClaims: Int,
    totalClaimsWithEngineComparison: Int,
    claimPromotionRate: Double
)

final case class MoveReviewCorpusQualityReport(
    sampleCount: Int,
    builtCount: Int,
    failedBuildCount: Int,
    overallLayerGapPercent: Double,
    layerGaps: List[LayerGapAggregate],
    issues: List[CorpusIssueAggregate],
    semanticCoverage: SemanticCoverageAggregate,
    claimPromotion: ClaimPromotionAggregate
)

object MoveReviewCorpusQualityAggregate:

  def fromSamples(samples: List[MoveReviewQualitySample]): MoveReviewCorpusQualityReport =
    val built = samples.flatMap(_.result)
    val layerGaps = aggregateLayerGaps(built)
    val totalSlots = layerGaps.map(_.totalSlots).sum
    val missingSlots = layerGaps.map(_.missingSlots).sum
    MoveReviewCorpusQualityReport(
      sampleCount = samples.size,
      builtCount = built.size,
      failedBuildCount = samples.size - built.size,
      overallLayerGapPercent = percent(missingSlots, totalSlots),
      layerGaps = layerGaps,
      issues = aggregateIssues(built),
      semanticCoverage = aggregateSemanticCoverage(built),
      claimPromotion = aggregateClaimPromotion(built)
    )

  private def aggregateLayerGaps(results: List[MoveReviewJudgmentResult]): List[LayerGapAggregate] =
    val byLayer =
      results.flatMap(_.quality.layerGaps.layers).groupBy(_.layer)
    JudgmentGraphLayer.values.toList.flatMap { layer =>
      byLayer.get(layer).map { metrics =>
        val totalSlots = metrics.map(_.totalSlots).sum
        val missing = metrics.flatMap(_.missingSlots)
        LayerGapAggregate(
          layer = layer,
          sampleCount = metrics.size,
          totalSlots = totalSlots,
          missingSlots = missing.size,
          gapPercent = percent(missing.size, totalSlots),
          missingBySlot = countBy(missing)(_.slot),
          missingByOwner = countBy(missing)(_.owner)
        )
      }
    }

  private def aggregateIssues(results: List[MoveReviewJudgmentResult]): List[CorpusIssueAggregate] =
    countBy(results.flatMap(_.quality.audit.issues))(_.kind)
      .toList
      .sortBy((kind, _) => kind.ordinal)
      .map(CorpusIssueAggregate.apply)

  private def aggregateSemanticCoverage(results: List[MoveReviewJudgmentResult]): SemanticCoverageAggregate =
    val coverage = results.map(_.quality.semanticCoverage)
    SemanticCoverageAggregate(
      sampleCount = results.size,
      tacticalIdeaSamples = coverage.count(_.tacticalIdeas > 0),
      strategicIdeaSamples = coverage.count(_.strategicIdeas > 0),
      pawnStructureIdeaSamples = coverage.count(_.pawnStructureIdeas > 0),
      openingIdeaSamples = coverage.count(_.openingIdeas > 0),
      defensiveIdeaSamples = coverage.count(_.defensiveIdeas > 0),
      evaluationIdeaSamples = coverage.count(_.evaluationIdeas > 0),
      conversionIdeaSamples = coverage.count(_.conversionIdeas > 0),
      relativeAssessmentSamples = coverage.count(_.hasRelativeAssessment),
      candidateSetComparisonSamples = coverage.count(_.hasCandidateSetComparison),
      onlyMoveSignalSamples = coverage.count(_.hasOnlyMoveSignal),
      forcedLineThemeSamples = coverage.count(_.hasForcedLineTheme)
    )

  private def aggregateClaimPromotion(results: List[MoveReviewJudgmentResult]): ClaimPromotionAggregate =
    val promotions = results.map(_.quality.claimPromotion)
    val totalIdeas = promotions.map(_.ideas).sum
    val totalClaims = promotions.map(_.claims).sum
    ClaimPromotionAggregate(
      sampleCount = results.size,
      totalIdeas = totalIdeas,
      totalClaims = totalClaims,
      totalClaimsWithEngineComparison = promotions.map(_.claimsWithEngineComparison).sum,
      claimPromotionRate = percent(totalClaims, totalIdeas) / 100d
    )

  private def countBy[A, K](items: List[A])(key: A => K): Map[K, Int] =
    items.groupMapReduce(key)(_ => 1)(_ + _)

  private def percent(numerator: Int, denominator: Int): Double =
    if denominator == 0 then 0d else numerator.toDouble / denominator.toDouble * 100d
