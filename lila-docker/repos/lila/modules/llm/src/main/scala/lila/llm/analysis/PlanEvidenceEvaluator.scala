package lila.llm.analysis

import lila.llm.model.{ ProbeContractValidator, ProbeRequest, ProbeResult }
import lila.llm.model.authoring.{ LatentPlanNarrative, PlanHypothesis }
import lila.llm.analysis.ThemeTaxonomy.{ ThemeL1, ThemeResolver, SubplanCatalog, SubplanId, SubplanSpec }

/**
 * Evaluates strategic plan hypotheses with a fail-closed evidence policy.
 *
 * Plan-first generation is allowed, but promotion to "main strategic plans"
 * is controlled by evidence status partitioning.
 */
object PlanEvidenceEvaluator:

  enum PlanEvidenceStatus:
    case PlayableEvidenceBacked
    case PlayablePvCoupled
    case Refuted
    case Deferred

  case class EvaluatedPlan(
      hypothesis: PlanHypothesis,
      status: PlanEvidenceStatus,
      reason: String,
      supportProbeIds: List[String] = Nil,
      refuteProbeIds: List[String] = Nil,
      missingSignals: List[String] = Nil,
      pvCoupled: Boolean = false,
      themeL1: String = ThemeL1.Unknown.id,
      subplanId: Option[String] = None
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
  private val PvCoupledPlayableThreshold = 0.55
  private val StrictPvCoupledMode = boolEnv("LLM_PLAN_EVIDENCE_STRICT", default = true)
  private val StrictFallbackToPvCoupled = boolEnv("LLM_PLAN_EVIDENCE_STRICT_FALLBACK", default = true)

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
          val themeId = hypothesisThemeId(h)
          val subplanId = hypothesisSubplanId(h)
          val subplanSpec =
            subplanId.flatMap(SubplanId.fromId).flatMap(SubplanCatalog.specs.get)
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
          val structuralEvidenceBacked = hasStructuralEvidence(h)
          val structuralEscalation =
            pvCoupled &&
              structuralEvidenceBacked &&
              h.score >= 0.50 &&
              h.viability.score >= 0.45 &&
              refutations.isEmpty &&
              missingSignals.isEmpty &&
              !subplanSignalsMissing(subplanSpec, linkedRequests)

          val (status, reason) =
            if refutations.nonEmpty then
              val (_, pr) = refutations.maxBy { case (req, res) => moverLoss(res, isWhiteToMove) - req.maxCpLoss.getOrElse(120) }
              val collapse = pr.l1Delta.flatMap(_.collapseReason).getOrElse("forcing tactical concession")
              (PlanEvidenceStatus.Refuted, s"probe refutation: $collapse")
            else if supports.nonEmpty then
              (PlanEvidenceStatus.PlayableEvidenceBacked, "validated by purpose-aligned probe evidence")
            else if structuralEscalation then
              (PlanEvidenceStatus.PlayableEvidenceBacked, "validated by structural evidence with no refutation signal")
            else if pvCoupled && h.score >= PvCoupledPlayableThreshold then
              (PlanEvidenceStatus.PlayablePvCoupled, "supported by engine-coupled continuation (probe evidence pending)")
            else
              val why =
                if linkedRequests.nonEmpty then
                  if missingSignals.nonEmpty then
                    s"evidence pending: missing ${missingSignals.take(3).mkString(", ")}"
                  else if subplanSignalsMissing(subplanSpec, linkedRequests) then
                    s"evidence pending: subplan $subplanId lacks required probe signals"
                  else "evidence pending: probe contract passed but support signal is insufficient"
                else "evidence pending: structurally plausible but probe validation is not yet available"
              (PlanEvidenceStatus.Deferred, why)

          EvaluatedPlan(
            hypothesis =
              h.copy(
                themeL1 = themeId,
                subplanId = subplanId
              ),
            status = status,
            reason = reason,
            supportProbeIds = supports.map(_._2.id).distinct,
            refuteProbeIds = refutations.map(_._2.id).distinct,
            missingSignals = missingSignals,
            pvCoupled = pvCoupled,
            themeL1 = themeId,
            subplanId = subplanId
          )
        }

      val themeLeaders =
        evaluated
          .groupBy(_.themeL1)
          .values
          .toList
          .flatMap(_.sortBy(ep => (-statusRank(ep.status), -rankingScore(ep))).headOption)

      val evidenceBacked =
        themeLeaders.filter(_.status == PlanEvidenceStatus.PlayableEvidenceBacked)
      val pvCoupledPlayable =
        themeLeaders.filter(_.status == PlanEvidenceStatus.PlayablePvCoupled)

      val mainPlanCandidates =
        if StrictPvCoupledMode then
          if evidenceBacked.nonEmpty then evidenceBacked
          else if StrictFallbackToPvCoupled then pvCoupledPlayable.take(2)
          else Nil
        else evidenceBacked ++ pvCoupledPlayable

      val mainPlans =
        mainPlanCandidates
          .sortBy(ep => -rankingScore(ep))
          .take(3)
          .zipWithIndex
          .map { case (ep, idx) => ep.hypothesis.copy(rank = idx + 1) }
      val mainPlanKeys = mainPlans.map(h => planKey(h.planId, h.subplanId)).toSet

      val latentPlans =
        evaluated
          .filter { ep =>
            ep.status == PlanEvidenceStatus.Deferred ||
            (ep.status == PlanEvidenceStatus.PlayablePvCoupled &&
              !mainPlanKeys.contains(planKey(ep.hypothesis.planId, ep.hypothesis.subplanId)))
          }
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
                case PlanEvidenceStatus.PlayablePvCoupled =>
                  if StrictPvCoupledMode then (ep.hypothesis.viability.score * 0.85).max(0.1)
                  else (ep.hypothesis.viability.score * 0.92).max(0.1)
                case _ => (ep.hypothesis.viability.score * 0.95).max(0.1)
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

      evaluated
        .filter(_.status == PlanEvidenceStatus.PlayablePvCoupled)
        .sortBy(ep => -ep.hypothesis.score)
        .take(2)
        .foreach { ep =>
          val label = if StrictPvCoupledMode then "deferred as PlayableByPV under strict evidence mode" else "accepted as PlayableByPV fallback"
          reasons += s"${ep.hypothesis.planName} is $label (${ep.reason})"
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
    val reqTheme = reqThemeId(req)
    val hypTheme = hypothesisThemeId(h)
    val reqSubplan = reqSubplanId(req)
    val hypSubplan = hypothesisSubplanId(h)
    val themeAligned = reqTheme.nonEmpty && reqTheme == hypTheme
    val subplanAligned = reqSubplan.forall(rsp => hypSubplan.contains(rsp))
    val contractCompatible = requestContractCompatible(req, hypSubplan)
    reqPlanId.contains(hypPlanId) ||
      reqSeed.exists(seed => hypSeed.contains(seed)) ||
      reqPlanName.exists { pn =>
        pn.contains(hypPlanName) || hypPlanName.contains(pn) || pn.contains(hypPlanId)
      } ||
      (themeAligned && (subplanAligned || contractCompatible))

  private def seedIdOf(h: PlanHypothesis): Option[String] =
    h.evidenceSources
      .collectFirst { case src if src.startsWith("latent_seed:") => src.stripPrefix("latent_seed:").trim }
      .filter(_.nonEmpty)

  private def normalizeText(raw: String): String =
    raw.toLowerCase.filter(_.isLetterOrDigit)

  private def reqThemeId(req: ProbeRequest): String =
    req.planId
      .map(pid => ThemeResolver.fromPlanId(pid).id)
      .filter(_ != ThemeL1.Unknown.id)
      .orElse(req.planName.map(name => ThemeResolver.fromPlanName(name).id).filter(_ != ThemeL1.Unknown.id))
      .getOrElse("")

  private def reqSubplanId(req: ProbeRequest): Option[String] =
    req.planName
      .flatMap(explicitSubplanFromPlanName)
      .orElse(req.planName.flatMap(name => ThemeResolver.subplanFromPlanName(name).map(_.id)))
      .orElse(req.seedId.flatMap(seed => ThemeResolver.subplanFromSeedId(seed).map(_.id)))
      .orElse(req.planId.flatMap(pid => ThemeResolver.subplanFromPlanId(pid).map(_.id)))
      .filter(_.nonEmpty)

  private def explicitSubplanFromPlanName(raw: String): Option[String] =
    val marker = "subplan:"
    val low = Option(raw).getOrElse("").trim.toLowerCase
    val idx = low.indexOf(marker)
    if idx < 0 then None
    else
      val token =
        low
          .substring(idx + marker.length)
          .takeWhile(ch => ch.isLetterOrDigit || ch == '_' || ch == '-')
          .trim
      SubplanId.fromId(token).map(_.id)

  private def hypothesisThemeId(h: PlanHypothesis): String =
    ThemeResolver.fromHypothesis(h).id

  private def hypothesisSubplanId(h: PlanHypothesis): Option[String] =
    h.subplanId
      .flatMap(SubplanId.fromId)
      .map(_.id)
      .orElse(ThemeResolver.subplanFromHypothesis(h).map(_.id))

  private def subplanSignalsMissing(
      specOpt: Option[SubplanSpec],
      linkedRequests: List[ProbeRequest]
  ): Boolean =
    specOpt.exists { spec =>
      val required = spec.requiredSignals.toSet
      required.nonEmpty &&
      !linkedRequests.exists(req => required.subsetOf(req.requiredSignals.toSet))
    }

  private def requestContractCompatible(req: ProbeRequest, hypSubplan: Option[String]): Boolean =
    if !req.purpose.contains("theme_plan_validation") then false
    else
      val required =
        hypSubplan
          .flatMap(SubplanId.fromId)
          .flatMap(SubplanCatalog.specs.get)
          .map(_.requiredSignals.toSet)
          .getOrElse(Set.empty[String])
      required.nonEmpty && required.subsetOf(req.requiredSignals.toSet)

  private def statusRank(status: PlanEvidenceStatus): Int =
    status match
      case PlanEvidenceStatus.PlayableEvidenceBacked => 4
      case PlanEvidenceStatus.PlayablePvCoupled      => 3
      case PlanEvidenceStatus.Deferred               => 2
      case PlanEvidenceStatus.Refuted                => 1

  private def rankingScore(ep: EvaluatedPlan): Double =
    ep.hypothesis.score + (if isDefaultSubplan(ep.themeL1, ep.subplanId) then 0.0 else 0.03)

  private def isDefaultSubplan(themeId: String, subplanId: Option[String]): Boolean =
    val theme = ThemeL1.fromId(themeId).getOrElse(ThemeL1.Unknown)
    val default = ThemeResolver.defaultSubplanForTheme(theme).map(_.id)
    default.exists(id => subplanId.contains(id))

  private def planKey(planId: String, subplanId: Option[String]): String =
    s"${planId.toLowerCase}|${subplanId.getOrElse("").toLowerCase}"

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
      case "theme_plan_validation" => 95
      case "latent_plan_immediate"  => 80
      case "free_tempo_branches"    => 90
      case "latent_plan_refutation" => 120
      case "reply_multipv" | "defense_reply_multipv" | "convert_reply_multipv" => 100
      case _ => 110

  private def hasStructuralEvidence(h: PlanHypothesis): Boolean =
    val evidenceLow = h.evidenceSources.map(_.toLowerCase)
    evidenceLow.exists(_.startsWith("structural_state:"))

  private def boolEnv(name: String, default: Boolean): Boolean =
    sys.env
      .get(name)
      .map(_.trim.toLowerCase)
      .flatMap {
        case "1" | "true" | "yes" | "on" => Some(true)
        case "0" | "false" | "no" | "off" => Some(false)
        case _ => None
      }
      .getOrElse(default)
