package lila.accountintel.service

import lila.accountintel.*
import lila.accountintel.AccountIntel.*
import lila.llm.PgnAnalysisHelper

private[accountintel] case class SelectiveEvalRefinement(
    clusters: List[SnapshotCluster],
    warnings: List[String]
)

private case class EvalWindow(
    clusterId: String,
    anchorGameId: String,
    anchorPly: Int,
    fen: String
)

final class SelectiveEvalRefiner(
    cacheLookup: SelectiveEvalLookup,
    requester: SelectiveEvalLookup
)(using Executor):

  private val dossierFenBudget = 12
  private val maxClusters = 3
  private val maxPrepClusters = 3
  private val maxWindowsPerCluster = 2
  private val targetOffsets = List(-2, 0, 2)

  def refine(
      provider: String,
      kind: ProductKind = ProductKind.MyAccountIntelligenceLite,
      selectedClusters: List[SnapshotCluster],
      featureRows: List[SnapshotFeatureRow]
  ): Fu[SelectiveEvalRefinement] =
    if provider != "chesscom" then fuccess(SelectiveEvalRefinement(selectedClusters, Nil))
    else
      val refinementTargets =
        selectedClusters
          .take(maxClustersFor(kind))
          .filter(needsRefinement)
      val windows = buildWindows(refinementTargets, featureRows).take(dossierFenBudget)
      if windows.isEmpty then fuccess(SelectiveEvalRefinement(selectedClusters, Nil))
      else
        val uniqueFens = windows.map(_.fen).distinct
        uniqueFens.foldLeft(fuccess(Map.empty[String, SelectiveEvalProbe])) { (acc, fen) =>
          acc.flatMap: probesByFen =>
            fetch(fen).map: maybeProbe =>
              maybeProbe.fold(probesByFen)(probe => probesByFen.updated(fen, probe))
        }.map: probesByFen =>
          val hitsByCluster =
            windows.foldLeft(Map.empty[String, List[SelectiveEvalProbe]]) { case (acc, window) =>
              probesByFen.get(window.fen).fold(acc)(probe =>
                acc.updatedWith(window.clusterId)(_.map(_ :+ probe).orElse(Some(List(probe))))
              )
            }
          val warnings =
            Option.when(refinementTargets.nonEmpty && hitsByCluster.isEmpty)("eval refinement unavailable").toList
          SelectiveEvalRefinement(
            clusters =
              selectedClusters.map(cluster =>
                if refinementTargets.exists(_.id == cluster.id) then
                  applyClusterRefinement(cluster, hitsByCluster.getOrElse(cluster.id, Nil))
                else cluster
              ),
            warnings = warnings
          )

  private def maxClustersFor(kind: ProductKind): Int =
    kind match
      case ProductKind.MyAccountIntelligenceLite => maxClusters
      case ProductKind.OpponentPrep => maxPrepClusters

  private def fetch(fen: String): Fu[Option[SelectiveEvalProbe]] =
    cacheLookup.lookup(fen).flatMap:
      case some @ Some(_) => fuccess(some)
      case None => requester.lookup(fen)

  private def needsRefinement(cluster: SnapshotCluster): Boolean =
    cluster.collapseBackedRate < 0.25d || cluster.snapshotConfidenceMean < 0.68d

  private def buildWindows(
      selectedClusters: List[SnapshotCluster],
      featureRows: List[SnapshotFeatureRow]
  ): List[EvalWindow] =
    val rowsByGame = featureRows.groupBy(_.gameId).view.mapValues(_.sortBy(_.ply)).toMap
    val exactFenByGame = exactFenMap(selectedClusters, featureRows)
    selectedClusters.flatMap: cluster =>
      clusterCandidates(cluster)
        .flatMap(candidate =>
          buildCandidateWindow(
            cluster.id,
            candidate,
            rowsByGame.getOrElse(candidate.gameId, Nil),
            exactFenByGame.getOrElse(candidate.gameId, Nil)
          )
        )
    .distinctBy(window => s"${window.clusterId}|${window.anchorGameId}|${window.anchorPly}|${window.fen}")

  private def exactFenMap(
      selectedClusters: List[SnapshotCluster],
      featureRows: List[SnapshotFeatureRow]
  ): Map[String, List[(Int, String)]] =
    val games =
      (selectedClusters.flatMap(_.candidates.map(_.game)) ++ featureRows.map(_.game))
        .distinctBy(_.external.gameId)
    games.map: game =>
      game.external.gameId ->
        PgnAnalysisHelper
          .extractPlyData(game.external.pgn)
          .toOption
          .getOrElse(Nil)
          .map(p => p.ply -> p.fen)
    .toMap

  private def clusterCandidates(cluster: SnapshotCluster): List[DecisionSnapshotCandidate] =
    cluster.candidates
      .sortBy(candidate =>
        (
          if cluster.exemplar.candidate.exists(ex => ex.gameId == candidate.gameId && ex.ply == candidate.ply)
          then 0
          else 1,
          -candidate.snapshotConfidence,
          -candidate.commitmentScore,
          candidate.ply
        )
      )
      .distinctBy(candidate => s"${candidate.gameId}:${candidate.ply}")
      .take(maxWindowsPerCluster)

  private def buildCandidateWindow(
      clusterId: String,
      candidate: DecisionSnapshotCandidate,
      gameRows: List[SnapshotFeatureRow],
      exactFenRows: List[(Int, String)]
  ): List[EvalWindow] =
    val searchRows =
      val anchoredRows =
        gameRows.filter(row => row.ply >= candidate.windowStartPly && row.ply <= candidate.windowEndPly)
      if anchoredRows.nonEmpty then anchoredRows else gameRows

    val targetPlies = targetPliesFor(candidate)
    val fens =
      (candidate.fen :: targetPlies.flatMap(targetPly =>
        exactFen(exactFenRows, targetPly)
          .orElse(closestFen(searchRows, targetPly, candidate))
      )).filter(_.trim.nonEmpty).distinct

    fens
      .take(targetOffsets.size + 2)
      .map(fen =>
        EvalWindow(
          clusterId = clusterId,
          anchorGameId = candidate.gameId,
          anchorPly = candidate.ply,
          fen = fen
        )
      )

  private def targetPliesFor(candidate: DecisionSnapshotCandidate): List[Int] =
    List(
      candidate.earliestPreventablePly.getOrElse(candidate.windowStartPly),
      (candidate.ply - 2).max(1),
      candidate.ply,
      candidate.ply + 2,
      candidate.windowEndPly
    ).distinct

  private def exactFen(rows: List[(Int, String)], targetPly: Int): Option[String] =
    rows.find(_._1 == targetPly).map(_._2).filter(_.trim.nonEmpty)

  private def closestFen(
      rows: List[SnapshotFeatureRow],
      targetPly: Int,
      candidate: DecisionSnapshotCandidate
  ): Option[String] =
    rows
      .filter(row => math.abs(row.ply - targetPly) <= 2)
      .sortBy(row => (math.abs(row.ply - targetPly), math.abs(row.ply - candidate.ply)))
      .headOption
      .map(_.fen)
      .orElse(Option.when(targetPly == candidate.ply && candidate.fen.trim.nonEmpty)(candidate.fen))

  private def applyClusterRefinement(
      cluster: SnapshotCluster,
      probes: List[SelectiveEvalProbe]
  ): SnapshotCluster =
    cluster.exemplar.candidate.fold(cluster): exemplar =>
      if probes.isEmpty then cluster
      else
        val mismatch = probes.exists(probe => probe.bestMove.exists(_ != exemplar.playedUci))
        val confidenceBoost = if mismatch then 0.12d else 0.06d
        val refinedExemplar =
          exemplar.copy(
            preventabilityScore =
              if mismatch then exemplar.preventabilityScore.max(0.82d)
              else exemplar.preventabilityScore.max(0.68d),
            earliestPreventablePly =
              if mismatch then exemplar.earliestPreventablePly.orElse(Some(exemplar.windowStartPly))
              else exemplar.earliestPreventablePly,
            snapshotConfidence = (exemplar.snapshotConfidence + confidenceBoost).min(1.0d),
            collapseBacked = exemplar.collapseBacked || probes.nonEmpty
          )

        val refinedCandidates =
          cluster.candidates.map(existing =>
            if existing.gameId == refinedExemplar.gameId && existing.ply == refinedExemplar.ply then refinedExemplar
            else existing
          )

        cluster.copy(
          averagePreventability = mean(refinedCandidates.map(_.preventabilityScore)),
          earliestPreventableRate =
            ratio(refinedCandidates.count(_.earliestPreventablePly.isDefined), refinedCandidates.size),
          snapshotConfidenceMean = mean(refinedCandidates.map(_.snapshotConfidence)),
          collapseBackedRate = ratio(refinedCandidates.count(_.collapseBacked), refinedCandidates.size),
          candidates = refinedCandidates,
          exemplar = cluster.exemplar.copy(candidate = refinedExemplar.some)
        )

  private def mean(values: List[Double]): Double =
    if values.isEmpty then 0d else values.sum / values.size.toDouble

  private def ratio(part: Int, total: Int): Double =
    if total <= 0 then 0d else part.toDouble / total.toDouble
