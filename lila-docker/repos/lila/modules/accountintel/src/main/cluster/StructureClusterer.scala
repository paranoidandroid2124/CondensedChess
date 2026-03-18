package lila.accountintel.cluster

import lila.accountintel.*
import lila.accountintel.AccountIntel.*

object StructureClusterer:

  private case class ExemplarChoice(candidate: DecisionSnapshotCandidate, centrality: Double)

  def cluster(candidates: List[DecisionSnapshotCandidate]): List[SnapshotCluster] =
    val baseClusters =
      candidates
        .groupBy(_.repeatabilityKey)
        .values
        .toList
        .map(buildCluster)

    val ranked =
      applyRedundancyPenalty(baseClusters)
        .sortBy(cluster =>
          (
            -cluster.priorityScore,
            -cluster.support,
            if cluster.side.white then 0 else 1,
            cluster.id
          )
        )

    ranked

  private def buildCluster(candidates: List[DecisionSnapshotCandidate]): SnapshotCluster =
    val uniqueCandidates = candidates.distinctBy(c => s"${c.gameId}:${c.ply}:${c.triggerType}")
    val exemplarChoice = medoid(uniqueCandidates)
    val exemplarCandidate = exemplarChoice.candidate
    val exemplarGame = exemplarCandidate.game
    val first = uniqueCandidates.head
    val distinctGames = uniqueCandidates.map(_.gameId).distinct.size
    val distinctOpenings = uniqueCandidates.map(_.openingFamily).distinct.size
    val averageExplainability = mean(uniqueCandidates.map(_.explainabilityScore))
    val averagePreventability = mean(uniqueCandidates.map(_.preventabilityScore))
    val averageBranching = mean(uniqueCandidates.map(_.branchingScore))
    val resultImpactScore = mean(uniqueCandidates.map(c => resultWeight(c.game.subjectResult)))
    val earliestPreventableRate =
      ratio(uniqueCandidates.count(_.earliestPreventablePly.isDefined), uniqueCandidates.size)
    val collapseBackedRate = ratio(uniqueCandidates.count(_.collapseBacked), uniqueCandidates.size)
    val offPlanRate = ratio(uniqueCandidates.count(_.planAlignmentBand.contains("OffPlan")), uniqueCandidates.size)
    val quietRate = ratio(uniqueCandidates.count(_.quiet), uniqueCandidates.size)
    val triggerDiversity = uniqueCandidates.map(_.triggerType).distinct.size.toDouble
    val snapshotConfidenceMean = mean(uniqueCandidates.map(_.snapshotConfidence))
    val commitmentMean = mean(uniqueCandidates.map(_.commitmentScore))
    val nonContinuationRate =
      ratio(uniqueCandidates.count(_.transitionType.exists(_ != "Continuation")), uniqueCandidates.size)
    val priorityBreakdown =
      clusterPriority(
        distinctGames = distinctGames,
        distinctOpenings = distinctOpenings,
        averageExplainability = averageExplainability,
        averagePreventability = averagePreventability,
        averageBranching = averageBranching,
        resultImpactScore = resultImpactScore,
        collapseBackedRate = collapseBackedRate,
        offPlanRate = offPlanRate,
        snapshotConfidenceMean = snapshotConfidenceMean,
        commitmentMean = commitmentMean,
        triggerDiversity = triggerDiversity,
        nonContinuationRate = nonContinuationRate,
        triggerType = first.triggerType
      )
    SnapshotCluster(
      id = slug(first.repeatabilityKey),
      side = first.side,
      openingFamily = first.openingFamily,
      structureFamily = first.structureFamily,
      triggerType = first.triggerType,
      labels = uniqueCandidates.flatMap(_.labels).distinct.take(4),
      priorityScore = priorityBreakdown.total,
      priorityBreakdown = priorityBreakdown,
      distinctGames = distinctGames,
      distinctOpenings = distinctOpenings,
      collapseBackedRate = collapseBackedRate,
      offPlanRate = offPlanRate,
      quietRate = quietRate,
      triggerDiversity = triggerDiversity,
      snapshotConfidenceMean = snapshotConfidenceMean,
      exemplarCentrality = exemplarChoice.centrality,
      nonContinuationRate = nonContinuationRate,
      averageExplainability = averageExplainability,
      averagePreventability = averagePreventability,
      averageBranching = averageBranching,
      resultImpactScore = resultImpactScore,
      earliestPreventableRate = earliestPreventableRate,
      candidates = uniqueCandidates,
      exemplar = ClusterExemplar(exemplarGame, exemplarCandidate.some)
    )

  private def applyRedundancyPenalty(clusters: List[SnapshotCluster]): List[SnapshotCluster] =
    val bestCentralityBySideStructure =
      clusters
        .groupBy(cluster => (cluster.side, cluster.structureFamily))
        .view
        .mapValues(_.map(_.exemplarCentrality).max)
        .toMap

    clusters.map: cluster =>
      val best = bestCentralityBySideStructure((cluster.side, cluster.structureFamily))
      val redundancyPenalty =
        if best > cluster.exemplarCentrality then 0.22 else 0.0
      val nextBreakdown = cluster.priorityBreakdown.copy(redundancyPenalty = redundancyPenalty)
      cluster.copy(priorityBreakdown = nextBreakdown, priorityScore = nextBreakdown.total)

  private def medoid(candidates: List[DecisionSnapshotCandidate]): ExemplarChoice =
    val ranked =
      candidates.map: candidate =>
        val meanDistance =
          candidates
            .filterNot(other =>
              other.game.external.gameId == candidate.game.external.gameId && other.ply == candidate.ply
            )
            .map(distance(candidate, _))
            .sum / candidates.size.max(1).toDouble
        ExemplarChoice(candidate, centrality = 1.0 / (1.0 + meanDistance))
    ranked.maxBy(choice => (choice.centrality, choice.candidate.snapshotConfidence))

  private def distance(left: DecisionSnapshotCandidate, right: DecisionSnapshotCandidate): Double =
    val resultDistance =
      math.abs(resultRank(left.game.subjectResult) - resultRank(right.game.subjectResult)).toDouble
    val plyDistance = math.abs(left.ply - right.ply).toDouble / 6.0
    val transitionDistance =
      if left.transitionType == right.transitionType then 0d else 0.6
    val planAlignmentDistance =
      if left.planAlignmentBand == right.planAlignmentBand then 0d else 0.4
    val overlap =
      left.labels
        .intersect(right.labels)
        .size
        .toDouble / left.labels.concat(right.labels).distinct.size.max(1).toDouble
    val labelDistance = 1.0 - overlap
    val confidenceDistance = math.abs(left.snapshotConfidence - right.snapshotConfidence) * 0.5
    resultDistance + plyDistance + labelDistance + transitionDistance + planAlignmentDistance + confidenceDistance

  private def resultRank(result: SubjectResult): Int =
    result match
      case SubjectResult.Loss => 0
      case SubjectResult.Draw => 1
      case SubjectResult.Win => 2

  private def resultWeight(result: SubjectResult): Double =
    result match
      case SubjectResult.Loss => 1.0
      case SubjectResult.Draw => 0.6
      case SubjectResult.Win => 0.2

  private def mean(values: List[Double]): Double =
    if values.isEmpty then 0d else values.sum / values.size.toDouble

  private def ratio(part: Int, total: Int): Double =
    if total <= 0 then 0d else part.toDouble / total.toDouble

  private def clusterPriority(
      distinctGames: Int,
      distinctOpenings: Int,
      averageExplainability: Double,
      averagePreventability: Double,
      averageBranching: Double,
      resultImpactScore: Double,
      collapseBackedRate: Double,
      offPlanRate: Double,
      snapshotConfidenceMean: Double,
      commitmentMean: Double,
      triggerDiversity: Double,
      nonContinuationRate: Double,
      triggerType: String
  ): PriorityBreakdown =
    PriorityBreakdown(
      supportScore = math.min(1.5d, distinctGames.toDouble * 0.25d),
      repeatabilityScore = math.min(0.8d, distinctOpenings.toDouble * 0.12d + triggerDiversity * 0.20d),
      snapshotScore = snapshotConfidenceMean * 0.90d + commitmentMean * 0.25d + averageExplainability * 0.08d,
      preventabilityScore = collapseBackedRate * 0.85d + averagePreventability * 0.25d,
      branchingScore = averageBranching * 0.70d,
      repairImpactScore =
        resultImpactScore * 0.55d + offPlanRate * 0.20d + nonContinuationRate * 0.15d + collapseBackedRate * 0.10d,
      readinessBonus = if distinctGames >= 3 then 0.2d else 0d,
      triggerPenalty = if triggerType == "file_commitment" then 0.18d else 0d,
      redundancyPenalty = 0d
    )
