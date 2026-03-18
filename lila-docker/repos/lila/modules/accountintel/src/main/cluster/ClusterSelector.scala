package lila.accountintel.cluster

import lila.accountintel.*
import lila.accountintel.AccountIntel.*

object ClusterSelector:

  def select(kind: ProductKind, clusters: List[SnapshotCluster]): List[SnapshotCluster] =
    if clusters.isEmpty then Nil
    else
      val limit = math.min(3, clusters.size)
      val ordered = ranked(kind, clusters)
      val preferredPool =
        val readyClusters = ordered.filter(_.ready)
        if readyClusters.size >= 2 then readyClusters else ordered
      val selected = scala.collection.mutable.ListBuffer.empty[SnapshotCluster]

      kind match
        case ProductKind.MyAccountIntelligenceLite =>
          pickFromBucket(kind, preferredPool.filter(_.side.white), selected, limit)
          pickFromBucket(kind, preferredPool.filter(_.side.black), selected, limit)
          fillRemaining(kind, preferredPool, ordered, selected, limit)
        case ProductKind.OpponentPrep =>
          pickFromBucket(kind, preferredPool.filter(_.side.white), selected, limit)
          pickFromBucket(kind, preferredPool.filter(_.side.black), selected, limit)
          fillRemaining(kind, preferredPool, ordered, selected, limit)

      selected.toList.take(limit)

  private def pickFromBucket(
      kind: ProductKind,
      candidates: List[SnapshotCluster],
      selected: scala.collection.mutable.ListBuffer[SnapshotCluster],
      limit: Int
  ): Unit =
    if selected.size < limit then bestCandidate(kind, candidates, selected.toList).foreach(selected += _)

  private def fillRemaining(
      kind: ProductKind,
      preferredPool: List[SnapshotCluster],
      ordered: List[SnapshotCluster],
      selected: scala.collection.mutable.ListBuffer[SnapshotCluster],
      limit: Int
  ): Unit =
    while selected.size < limit do
      val next =
        bestCandidate(kind, preferredPool, selected.toList)
          .orElse(bestCandidate(kind, ordered, selected.toList))
          .orElse(ordered.find(cluster => !selected.exists(_.id == cluster.id)))
      next match
        case Some(cluster) => selected += cluster
        case None => return

  private def ranked(kind: ProductKind, clusters: List[SnapshotCluster]): List[SnapshotCluster] =
    clusters.sortBy(cluster =>
      (
        -scoreFor(kind, cluster),
        -cluster.priorityScore,
        -cluster.support,
        if cluster.side.white then 0 else 1,
        cluster.id
      )
    )

  private def bestCandidate(
      kind: ProductKind,
      pool: List[SnapshotCluster],
      selected: List[SnapshotCluster]
  ): Option[SnapshotCluster] =
    val eligibleClusters =
      pool.filter(cluster => eligible(cluster, selected))
    val ordered =
      eligibleClusters.sortBy(cluster =>
        (
          -scoreFor(kind, cluster),
          -cluster.priorityScore,
          -cluster.support,
          cluster.id
        )
      )

    maybeDiversify(selected, ordered, kind)

  private def eligible(cluster: SnapshotCluster, selected: List[SnapshotCluster]): Boolean =
    !selected.exists(_.id == cluster.id) &&
      !selected.exists(existing =>
        existing.side == cluster.side && existing.structureFamily == cluster.structureFamily
      ) &&
      !(cluster.triggerType == "file_commitment" && selected.size < 2)

  private def maybeDiversify(
      selected: List[SnapshotCluster],
      ordered: List[SnapshotCluster],
      kind: ProductKind
  ): Option[SnapshotCluster] =
    ordered.headOption.flatMap: top =>
      val repeatedTrigger =
        Option.when(selected.size >= 2 && selected.take(2).map(_.triggerType).distinct.size == 1)(
          selected.head.triggerType
        )
      repeatedTrigger match
        case Some(trigger) if top.triggerType == trigger =>
          ordered
            .find(candidate =>
              candidate.triggerType != trigger &&
                (scoreFor(kind, top) - scoreFor(kind, candidate)) <= 0.15
            )
            .orElse(Some(top))
        case _ => Some(top)

  private def scoreFor(kind: ProductKind, cluster: SnapshotCluster): Double =
    kind match
      case ProductKind.MyAccountIntelligenceLite => cluster.priorityScore + repairBias(cluster)
      case ProductKind.OpponentPrep => steeringScore(cluster)

  private def repairBias(cluster: SnapshotCluster): Double =
    (cluster.resultImpactScore * 0.35d) +
      (cluster.offPlanRate * 0.25d) +
      (cluster.collapseBackedRate * 0.20d) +
      (cluster.quietRate * 0.10d) +
      (cluster.nonContinuationRate * 0.10d)

  private def steeringScore(cluster: SnapshotCluster): Double =
    cluster.priorityScore +
      (cluster.quietRate * 0.15d) +
      (cluster.collapseBackedRate * 0.10d) +
      (cluster.nonContinuationRate * 0.10d)
