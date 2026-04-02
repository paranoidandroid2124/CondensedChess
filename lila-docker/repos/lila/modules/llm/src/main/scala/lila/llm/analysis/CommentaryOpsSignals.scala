package lila.llm.analysis

import lila.llm.{ AuthorEvidenceSummary, DecisionComparisonDigest, NarrativeSignalDigest, StrategyPack }
import lila.llm.model.ProbeRequest

object CommentaryOpsSignals:

  final case class ComparisonObservation(
      available: Boolean,
      consistent: Boolean,
      reasons: List[String]
  )

  final case class ThesisAgreementObservation(
      agreed: Boolean,
      dominantLabels: List[String],
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

  def activeThesisAgreement(
      baseNarrative: String,
      activeNote: String,
      digest: Option[NarrativeSignalDigest]
  ): ThesisAgreementObservation =
    val low = normalize(activeNote)
    val labels = dominantLabels(baseNarrative, digest)
    val labelHits =
      labels.filter(label => significantTokens(label).exists(low.contains))

    val baseClaim = BookmakerProseContract.stripMoveHeader(firstSentence(baseNarrative))
    val claimTokens = significantTokens(baseClaim)
    val claimHit =
      claimTokens.nonEmpty &&
        claimTokens.count(low.contains) >= math.min(3, claimTokens.size.max(1))

    val structureHit =
      digest.exists { d =>
        d.structureProfile.exists(profile => significantTokens(profile).exists(low.contains)) &&
          (
            d.deploymentPurpose.exists(purpose => significantTokens(purpose).exists(low.contains)) ||
              d.deploymentContribution.exists(contrib => significantTokens(contrib).exists(low.contains)) ||
              d.deploymentRoute.exists(square => low.contains(normalize(square)))
          )
      }

    val openingHit =
      digest.exists { d =>
        d.opening.exists(name => significantTokens(name).exists(low.contains)) &&
          (
            low.contains("branch") ||
              d.decisionComparison.flatMap(_.deferredMove).exists(move => low.contains(normalize(move)))
          )
      }

    val decisionHit =
      digest.flatMap(_.decisionComparison).exists { dc =>
        val moveHit =
          List(dc.chosenMove, dc.engineBestMove, dc.deferredMove).flatten
            .map(normalize)
            .filter(_.nonEmpty)
            .exists(low.contains)
        val reasonHit =
          List(dc.deferredReason, dc.evidence).flatten
            .flatMap(significantTokens)
            .exists(low.contains)
        moveHit && reasonHit
      }

    val compensationHit =
      digest.exists { d =>
        d.compensation.exists(value => significantTokens(value).exists(low.contains)) ||
          d.compensationVectors.flatMap(significantTokens).exists(low.contains)
      }

    val prophylaxisHit =
      digest.exists { d =>
        d.prophylaxisPlan.exists(value => significantTokens(value).exists(low.contains)) ||
          d.prophylaxisThreat.exists(value => significantTokens(value).exists(low.contains))
      }

    val agreed =
      claimHit || structureHit || openingHit || decisionHit || compensationHit || prophylaxisHit || labelHits.size >= 2

    val reasons =
      Option.when(!agreed)("dominant_thesis_not_preserved").toList

    ThesisAgreementObservation(
      agreed = agreed,
      dominantLabels = labels,
      reasons = reasons
    )

  private def dominantLabels(
      baseNarrative: String,
      digest: Option[NarrativeSignalDigest]
  ): List[String] =
    digest match
      case Some(d)
          if d.structureProfile.exists(_.trim.nonEmpty) &&
            (d.deploymentPurpose.exists(_.trim.nonEmpty) || d.deploymentContribution.exists(_.trim.nonEmpty)) =>
        List(
          d.structureProfile,
          d.alignmentBand,
          d.deploymentPurpose,
          d.deploymentContribution
        ).flatten.filter(_.trim.nonEmpty)
      case Some(d) if d.opening.exists(_.trim.nonEmpty) =>
        List(
          d.opening,
          d.decisionComparison.flatMap(_.deferredMove),
          d.decisionComparison.flatMap(_.deferredReason)
        ).flatten.filter(_.trim.nonEmpty)
      case Some(d) if d.compensation.exists(_.trim.nonEmpty) =>
        (List(d.compensation).flatten ++ d.compensationVectors).filter(_.trim.nonEmpty)
      case Some(d) if d.prophylaxisPlan.exists(_.trim.nonEmpty) || d.prophylaxisThreat.exists(_.trim.nonEmpty) =>
        List(d.prophylaxisPlan, d.prophylaxisThreat).flatten.filter(_.trim.nonEmpty)
      case Some(d) if d.decisionComparison.isDefined =>
        val dc = d.decisionComparison.get
        List(dc.chosenMove, dc.deferredMove, dc.deferredReason, dc.evidence).flatten.filter(_.trim.nonEmpty)
      case Some(d) if d.decision.exists(_.trim.nonEmpty) =>
        List(d.decision.get.trim)
      case _ =>
        List(BookmakerProseContract.stripMoveHeader(firstSentence(baseNarrative))).filter(_.trim.nonEmpty)

  private def firstSentence(text: String): String =
    Option(text)
      .getOrElse("")
      .split("""(?<=[.!?])\s+""")
      .map(_.trim)
      .find(_.nonEmpty)
      .getOrElse("")

  private def significantTokens(text: String): List[String] =
    normalize(text)
      .split("""[^a-z0-9]+""")
      .toList
      .filter(token => token.nonEmpty && token.length > 3 && !StopWords.contains(token))
      .distinct

  private def normalize(text: String): String =
    Option(text).getOrElse("").toLowerCase.replaceAll("""\s+""", " ").trim

  private val StopWords = Set(
    "this",
    "that",
    "with",
    "from",
    "into",
    "where",
    "because",
    "belongs",
    "calls",
    "move",
    "plan",
    "long",
    "term",
    "structure",
    "keeps",
    "stays",
    "there",
    "their",
    "have",
    "will",
    "after",
    "before",
    "than",
    "then",
    "here",
    "when",
    "line",
    "branch",
    "side",
    "piece",
    "pieces",
    "route",
    "routes",
    "through",
    "around"
  )
