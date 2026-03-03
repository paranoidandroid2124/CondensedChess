package lila.llm

import play.api.libs.json._
import lila.llm.model.CollapseAnalysis

case class DefeatDnaReport(
  userId: String,
  totalGamesAnalyzed: Int,
  rootCauseDistribution: Map[String, Int],
  avgRecoverabilityPlies: Double,
  mostCommonPatchLines: List[String],
  recentCollapses: List[CollapseAnalysis]
)

object DefeatDnaReport {
  import CollapseAnalysis.given
  implicit val writes: OWrites[DefeatDnaReport] = Json.writes[DefeatDnaReport]
}

class DefeatDnaApi {

  /**
   * Aggregates CCA records from the user's last N games.
   */
  def aggregateDna(userId: String, recentAnalyses: List[CollapseAnalysis]): DefeatDnaReport = {
    val total = recentAnalyses.size
    if (total == 0) {
      return DefeatDnaReport(userId, 0, Map.empty, 0.0, Nil, Nil)
    }

    val distribution = recentAnalyses
      .groupBy(_.rootCause)
      .view.mapValues(_.size).toMap

    val avgRecov = recentAnalyses.map(_.recoverabilityPlies).sum.toDouble / total

    val flatPatches = recentAnalyses.map(_.patchLineUci.take(2).mkString(" "))
      .filter(_.nonEmpty)
      .groupBy(identity)
      .view.mapValues(_.size).toList
      .sortBy(-_._2)
      .take(5)
      .map(_._1)

    DefeatDnaReport(userId, total, distribution, avgRecov, flatPatches, recentAnalyses.take(10))
  }

  /**
   * Simple A/B test flag generator for CCA visibility and telemetry.
   * Tracks whether the user is in the treatment group (sees CCA & Defeat DNA).
   */
  def isCcaEnabledForUser(userId: String): Boolean = {
    // Basic sticky hash-based AB test
    val hash = userId.hashCode.abs
    // 50% rollout for CCA
    hash % 100 < 50
  }
}
