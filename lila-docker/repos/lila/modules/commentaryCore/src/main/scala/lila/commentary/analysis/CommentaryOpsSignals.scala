package lila.commentary.analysis

import lila.commentary.{ AuthorEvidenceSummary, DecisionComparisonDigest, StrategyPack }
import lila.commentary.model.ProbeRequest

object CommentaryOpsSignals:

  final case class ComparisonObservation(
      available: Boolean,
      consistent: Boolean,
      reasons: List[String]
  )

  def decisionComparisonConsistency(
      digest: Option[DecisionComparisonDigest],
      authorEvidence: List[AuthorEvidenceSummary],
      probeRequests: List[ProbeRequest],
      strategyPack: Option[StrategyPack] = None
  ): Option[ComparisonObservation] =
    val packDigest = strategyPack.flatMap(_.signalDigest).flatMap(_.decisionComparison)
    val available =
      digest.nonEmpty || authorEvidence.nonEmpty || probeRequests.nonEmpty || packDigest.nonEmpty

    Option.when(available) {
      val reasons = scala.collection.mutable.ListBuffer.empty[String]

      (digest, packDigest) match
        case (Some(left), Some(right)) if left != right =>
          reasons += "strategy_pack_mismatch"
        case (None, Some(_)) =>
          reasons += "missing_signal_digest"
        case _ =>

      digest match
        case None =>
          if authorEvidence.nonEmpty || probeRequests.nonEmpty then
            reasons += "missing_decision_digest"
        case Some(dc) =>
          if dc.practicalAlternative && dc.deferredMove.forall(_.trim.isEmpty) then
            reasons += "practical_without_deferred"
          if !dc.chosenMatchesBest && dc.engineBestMove.forall(_.trim.isEmpty) then
            reasons += "missing_engine_best"
          if dc.chosenMatchesBest && dc.cpLossVsChosen.exists(_ > 20) then
            reasons += "matching_best_has_cp_loss"

          val hasDeferredSupport =
            dc.deferredReason.exists(_.trim.nonEmpty) || dc.evidence.exists(_.trim.nonEmpty)
          if dc.deferredMove.exists(_.trim.nonEmpty) && !hasDeferredSupport then
            reasons += "deferred_without_support"

          val hasEvidenceSource =
            authorEvidence.nonEmpty || probeRequests.nonEmpty || dc.engineBestPv.nonEmpty
          if dc.evidence.exists(_.trim.nonEmpty) && !hasEvidenceSource then
            reasons += "evidence_without_source"

      ComparisonObservation(
        available = true,
        consistent = reasons.isEmpty,
        reasons = reasons.toList.distinct
      )
    }
