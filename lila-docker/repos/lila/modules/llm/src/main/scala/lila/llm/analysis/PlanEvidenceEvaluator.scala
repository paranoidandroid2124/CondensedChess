package lila.llm.analysis

import lila.llm.model.{ ProbeContractValidator, ProbeRequest, ProbeResult }
import lila.llm.model.authoring.{ LatentPlanNarrative, PlanHypothesis }

/**
 * Evaluates strategic plan hypotheses with a fail-closed evidence policy.
 *
 * Plan-first generation is allowed, but promotion to "main strategic plans"
 * requires either validated probe support or explicit engine coupling.
 */
object PlanEvidenceEvaluator:

  enum PlanEvidenceStatus:
    case Proposed
    case Playable
    case Refuted
    case Deferred

  case class EvaluatedPlan(
      hypothesis: PlanHypothesis,
      status: PlanEvidenceStatus,
      reason: String,
      supportProbeIds: List[String] = Nil,
      refuteProbeIds: List[String] = Nil,
      missingSignals: List[String] = Nil,
      pvCoupled: Boolean = false
  )

  case class ProbeValidation(
      validResults: List[ProbeResult],
      droppedCount: Int,
      droppedReasons: List[String],
      invalidByRequestId: Map[String, List[String]]
  )

  case class PartitionedPlans(
      mainPlans: List[PlanHypothesis],
      latentPlans: List[LatentPlanNarrative],
      whyAbsentFromTopMultiPV: List[String],
      evaluated: List[EvaluatedPlan]
  )

  private val RefutationPurposes = Set("latent_plan_refutation", "refute_aggressive_line")

  def validateProbeResults(
      rawResults: List[ProbeResult],
      probeRequests: List[ProbeRequest]
  ): ProbeValidation =
    val requestById = probeRequests.groupBy(_.id).view.mapValues(_.head).toMap
    val valid = scala.collection.mutable.ListBuffer.empty[ProbeResult]
    val droppedReasons = scala.collection.mutable.ListBuffer.empty[String]
    val invalidByRequest = scala.collection.mutable.Map.empty[String, List[String]]
    var dropped = 0

    rawResults.foreach { pr =>
      val reqOpt = requestById.get(pr.id)
      val validation =
        reqOpt match
          case Some(req) => ProbeContractValidator.validateAgainstRequest(req, pr)
          case None      => ProbeContractValidator.validate(pr)

      if validation.isValid then
        valid += pr
      else
        dropped += 1
        val reason =
          if validation.missingSignals.nonEmpty then
            s"probe ${pr.id} missing required signals: ${validation.missingSignals.mkString(", ")}"
          else if validation.reasonCodes.nonEmpty then
            s"probe ${pr.id} failed contract: ${validation.reasonCodes.mkString(", ")}"
          else s"probe ${pr.id} failed validation"
        droppedReasons += reason
        reqOpt.foreach { req =>
          val details =
            (validation.missingSignals ++ validation.reasonCodes).distinct.filter(_.nonEmpty)
          if details.nonEmpty then
            val existing = invalidByRequest.getOrElse(req.id, Nil)
            invalidByRequest.update(req.id, (existing ++ details).distinct)
        }
    }

    ProbeValidation(
      validResults = valid.toList,
      droppedCount = dropped,
      droppedReasons = droppedReasons.toList.distinct.take(4),
      invalidByRequestId = invalidByRequest.toMap
    )

  def partition(
      hypotheses: List[PlanHypothesis],
      probeRequests: List[ProbeRequest],
      validatedProbeResults: List[ProbeResult],
      rulePlanIds: Set[String],
      isWhiteToMove: Boolean,
      droppedProbeCount: Int,
      droppedProbeReasons: List[String] = Nil,
      invalidByRequestId: Map[String, List[String]] = Map.empty
  ): PartitionedPlans =
    if hypotheses.isEmpty then
      val onlyReasons =
        if droppedProbeCount > 0 then
          (droppedProbeReasons :+ s"$droppedProbeCount probe result(s) were rejected by contract validation").distinct.take(3)
        else droppedProbeReasons.take(3)
      PartitionedPlans(
        mainPlans = Nil,
        latentPlans = Nil,
        whyAbsentFromTopMultiPV = onlyReasons,
        evaluated = Nil
      )
    else
      val resultsById = validatedProbeResults.groupBy(_.id)

      val evaluated =
        hypotheses.map { h =>
          val linkedRequests = probeRequests.filter(req => requestMatchesHypothesis(req, h))
          val linkedResults =
            linkedRequests.flatMap(req => resultsById.getOrElse(req.id, Nil).map(pr => req -> pr))
          val refutations =
            linkedResults.filter { case (req, pr) => isRefuted(req, pr, isWhiteToMove) }
          val supports =
            linkedResults.filter { case (req, pr) => isSupportive(req, pr, isWhiteToMove) }
          val missingSignals =
            linkedRequests.flatMap(req => invalidByRequestId.getOrElse(req.id, Nil)).distinct

          val pvCoupled =
            rulePlanIds.exists(_.equalsIgnoreCase(h.planId)) ||
              h.evidenceSources.exists(_.contains("support:engine_hypothesis"))

          val (status, reason) =
            if refutations.nonEmpty then
              val (_, pr) = refutations.maxBy { case (req, res) => moverLoss(res, isWhiteToMove) - req.maxCpLoss.getOrElse(120) }
              val collapse = pr.l1Delta.flatMap(_.collapseReason).getOrElse("forcing tactical concession")
              (PlanEvidenceStatus.Refuted, s"probe refutation: $collapse")
            else if supports.nonEmpty then
              (PlanEvidenceStatus.Playable, "validated by purpose-aligned probe evidence")
            else if pvCoupled && h.score >= 0.55 then
              (PlanEvidenceStatus.Playable, "supported by engine-coupled continuation")
            else if linkedRequests.nonEmpty then
              val why =
                if missingSignals.nonEmpty then
                  s"evidence pending: missing ${missingSignals.take(3).mkString(", ")}"
                else "evidence pending: probe contract passed but support signal is insufficient"
              (PlanEvidenceStatus.Deferred, why)
            else
              (PlanEvidenceStatus.Proposed, "structurally plausible but not yet evidenced")

          EvaluatedPlan(
            hypothesis = h,
            status = status,
            reason = reason,
            supportProbeIds = supports.map(_._2.id).distinct,
            refuteProbeIds = refutations.map(_._2.id).distinct,
            missingSignals = missingSignals,
            pvCoupled = pvCoupled
          )
        }

      val mainPlans =
        evaluated
          .filter(_.status == PlanEvidenceStatus.Playable)
          .sortBy(ep => -ep.hypothesis.score)
          .take(3)
          .zipWithIndex
          .map { case (ep, idx) => ep.hypothesis.copy(rank = idx + 1) }

      val latentPlans =
        evaluated
          .filter(ep => ep.status == PlanEvidenceStatus.Proposed || ep.status == PlanEvidenceStatus.Deferred)
          .sortBy(ep => -ep.hypothesis.score)
          .take(2)
          .map { ep =>
            val seedId =
              seedIdOf(ep.hypothesis)
                .orElse {
                  probeRequests
                    .find(req => requestMatchesHypothesis(req, ep.hypothesis))
                    .flatMap(_.seedId)
                }
                .getOrElse(ep.hypothesis.planId)
            val adjustedViability =
              ep.status match
                case PlanEvidenceStatus.Deferred => (ep.hypothesis.viability.score * 0.8).max(0.1)
                case _                           => (ep.hypothesis.viability.score * 0.95).max(0.1)
            LatentPlanNarrative(
              seedId = seedId,
              planName = ep.hypothesis.planName,
              viabilityScore = adjustedViability,
              whyAbsentFromTopMultiPv = ep.reason
            )
          }

      val reasons = scala.collection.mutable.ListBuffer.empty[String]
      evaluated
        .filter(_.status == PlanEvidenceStatus.Refuted)
        .sortBy(ep => -ep.hypothesis.score)
        .take(2)
        .foreach { ep =>
          reasons += s"${ep.hypothesis.planName} is held back by refutation evidence (${ep.reason})"
        }

      evaluated
        .filter(_.status == PlanEvidenceStatus.Deferred)
        .sortBy(ep => -ep.hypothesis.score)
        .take(2)
        .foreach { ep =>
          reasons += s"${ep.hypothesis.planName} remains conditional (${ep.reason})"
        }

      if droppedProbeCount > 0 then
        reasons += s"$droppedProbeCount probe result(s) were discarded due to contract validation gaps"

      if reasons.isEmpty && latentPlans.nonEmpty then
        reasons += "strategic ideas are preparatory and need further probe confirmation"
      if reasons.isEmpty && mainPlans.isEmpty then
        reasons += "no plan passed evidence gating in the current search window"

      PartitionedPlans(
        mainPlans = mainPlans,
        latentPlans = latentPlans,
        whyAbsentFromTopMultiPV = (reasons.toList ++ droppedProbeReasons).distinct.take(3),
        evaluated = evaluated
      )

  private def requestMatchesHypothesis(req: ProbeRequest, h: PlanHypothesis): Boolean =
    val reqPlanId = req.planId.map(_.trim.toLowerCase)
    val reqPlanName = req.planName.map(normalizeText)
    val reqSeed = req.seedId.map(_.trim.toLowerCase)
    val hypPlanId = h.planId.trim.toLowerCase
    val hypPlanName = normalizeText(h.planName)
    val hypSeed = seedIdOf(h).map(_.trim.toLowerCase)
    reqPlanId.contains(hypPlanId) ||
      reqSeed.exists(seed => hypSeed.contains(seed)) ||
      reqPlanName.exists { pn =>
        pn.contains(hypPlanName) || hypPlanName.contains(pn) || pn.contains(hypPlanId)
      }

  private def seedIdOf(h: PlanHypothesis): Option[String] =
    h.evidenceSources
      .collectFirst { case src if src.startsWith("latent_seed:") => src.stripPrefix("latent_seed:").trim }
      .filter(_.nonEmpty)

  private def normalizeText(raw: String): String =
    raw.toLowerCase.filter(_.isLetterOrDigit)

  private def isSupportive(req: ProbeRequest, pr: ProbeResult, isWhiteToMove: Boolean): Boolean =
    val purpose = req.purpose.orElse(pr.purpose).getOrElse("")
    if RefutationPurposes.contains(purpose) then
      // Refutation probes can still support a plan when no tactical punishment appears.
      !isRefuted(req, pr, isWhiteToMove)
    else
      val loss = moverLoss(pr, isWhiteToMove)
      val maxLoss = req.maxCpLoss.getOrElse(defaultMaxCpLoss(purpose))
      loss <= maxLoss && !isMateAgainstMover(pr, isWhiteToMove)

  private def isRefuted(req: ProbeRequest, pr: ProbeResult, isWhiteToMove: Boolean): Boolean =
    val purpose = req.purpose.orElse(pr.purpose).getOrElse("")
    val loss = moverLoss(pr, isWhiteToMove)
    val maxLoss = req.maxCpLoss.getOrElse(defaultMaxCpLoss(purpose))
    val hardLoss = loss > maxLoss
    val tacticalCollapse = pr.l1Delta.flatMap(_.collapseReason).exists(_.nonEmpty) && loss >= 90
    val mateAgainstMover = isMateAgainstMover(pr, isWhiteToMove)
    if RefutationPurposes.contains(purpose) then hardLoss || tacticalCollapse || mateAgainstMover
    else hardLoss || mateAgainstMover

  private def moverLoss(pr: ProbeResult, isWhiteToMove: Boolean): Int =
    if isWhiteToMove then -pr.deltaVsBaseline else pr.deltaVsBaseline

  private def isMateAgainstMover(pr: ProbeResult, isWhiteToMove: Boolean): Boolean =
    pr.mate match
      case Some(m) if isWhiteToMove => m < 0
      case Some(m) if !isWhiteToMove => m > 0
      case _ => false

  private def defaultMaxCpLoss(purpose: String): Int =
    purpose match
      case "latent_plan_immediate"  => 80
      case "free_tempo_branches"    => 90
      case "latent_plan_refutation" => 120
      case "reply_multipv" | "defense_reply_multipv" | "convert_reply_multipv" => 100
      case _ => 110
