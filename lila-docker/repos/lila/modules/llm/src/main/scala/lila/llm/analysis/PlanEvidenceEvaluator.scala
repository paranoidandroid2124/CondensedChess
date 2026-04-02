package lila.llm.analysis

import play.api.libs.json.*
import lila.llm.model.{ ProbeContractValidator, ProbeRequest, ProbeResult }
import lila.llm.model.authoring.PlanHypothesis
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

  enum UserFacingPlanEligibility:
    case ProbeBacked
    case StructuralOnly
    case PvCoupledOnly
    case Deferred
    case Refuted

  case class EvaluatedPlan(
      hypothesis: PlanHypothesis,
      status: PlanEvidenceStatus,
      userFacingEligibility: UserFacingPlanEligibility,
      reason: String,
      supportProbeIds: List[String] = Nil,
      refuteProbeIds: List[String] = Nil,
      missingSignals: List[String] = Nil,
      pvCoupled: Boolean = false,
      themeL1: String = ThemeL1.Unknown.id,
      subplanId: Option[String] = None,
      claimCertification: ClaimCertification = ClaimCertification()
  )

  case class ClaimCertification(
      certificateStatus: PlayerFacingCertificateStatus = PlayerFacingCertificateStatus.Invalid,
      quantifier: PlayerFacingClaimQuantifier = PlayerFacingClaimQuantifier.Existential,
      modalityTier: PlayerFacingClaimModalityTier = PlayerFacingClaimModalityTier.Available,
      attributionGrade: PlayerFacingClaimAttributionGrade = PlayerFacingClaimAttributionGrade.StateOnly,
      stabilityGrade: PlayerFacingClaimStabilityGrade = PlayerFacingClaimStabilityGrade.Unknown,
      provenanceClass: PlayerFacingClaimProvenanceClass = PlayerFacingClaimProvenanceClass.Deferred,
      taintFlags: List[PlayerFacingClaimTaintFlag] = Nil,
      ontologyFamily: PlayerFacingClaimOntologyFamily = PlayerFacingClaimOntologyFamily.Unknown,
      alternativeDominance: Boolean = false
  )

  case class ProbeValidation(
      validResults: List[ProbeResult],
      droppedCount: Int,
      droppedReasons: List[String],
      invalidByRequestId: Map[String, List[String]]
  )

  case class DiagnosticPlanEntry(
      planId: String,
      subplanId: Option[String] = None,
      planName: String,
      status: String,
      userFacingEligibility: String,
      certificateStatus: String = "invalid",
      quantifier: String = "existential",
      modalityTier: String = "available",
      attributionGrade: String = "state_only",
      stabilityGrade: String = "unknown",
      provenanceClass: String = "deferred",
      taintFlags: List[String] = Nil,
      ontologyFamily: String = "unknown",
      linkedRequestIds: List[String] = Nil,
      supportProbeIds: List[String] = Nil,
      refuteProbeIds: List[String] = Nil,
      missingSignals: List[String] = Nil,
      reasonCodes: List[String] = Nil,
      maxCpLoss: Option[Int] = None,
      worstMoverLoss: Option[Int] = None,
      alternativeDominance: Boolean = false
  )

  object DiagnosticPlanEntry:
    given OWrites[DiagnosticPlanEntry] = Json.writes[DiagnosticPlanEntry]

  case class DiagnosticPlanSidecar(
      entries: List[DiagnosticPlanEntry] = Nil,
      droppedProbeCount: Int = 0,
      droppedReasonCodes: List[String] = Nil,
      blockedStrongClaims: Int = 0,
      downgradedWeakClaims: Int = 0,
      attributionFailures: Int = 0,
      quantifierFailures: Int = 0,
      stabilityFailures: Int = 0
  )

  object DiagnosticPlanSidecar:
    given OWrites[DiagnosticPlanSidecar] = Json.writes[DiagnosticPlanSidecar]

  case class PartitionedPlans(
      mainPlans: List[PlanHypothesis],
      evaluated: List[EvaluatedPlan],
      diagnosticSidecar: DiagnosticPlanSidecar = DiagnosticPlanSidecar()
  )

  private val PvCoupledPlayableThreshold = 0.55

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
            s"supporting evidence is still incomplete: missing ${describeSignalList(validation.missingSignals)}"
          else if validation.reasonCodes.nonEmpty then
            "supporting evidence is still incomplete after validation"
          else "supporting evidence is still incomplete"
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
      PartitionedPlans(
        mainPlans = Nil,
        evaluated = Nil,
        diagnosticSidecar =
          DiagnosticPlanSidecar(
            entries = Nil,
            droppedProbeCount = droppedProbeCount,
            droppedReasonCodes =
              if droppedProbeCount > 0 then
                (invalidByRequestId.values.flatten.toList :+ "contract_validation_drop").distinct.take(8)
              else invalidByRequestId.values.flatten.toList.distinct.take(8)
          )
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
          val alternativeDominance =
            isAlternativeDominated(
              target = h,
              allHypotheses = hypotheses
            )

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
              (PlanEvidenceStatus.Refuted, s"concrete follow-up refutes it: $collapse")
            else if alternativeDominance then
              (
                PlanEvidenceStatus.Refuted,
                "a stronger sibling plan explains the position more directly, so this wording would be misleading"
              )
            else if supports.nonEmpty then
              (PlanEvidenceStatus.PlayableEvidenceBacked, "backed by concrete supporting lines")
            else if structuralEscalation then
              (PlanEvidenceStatus.PlayableEvidenceBacked, "backed by the structure with no refutation signal")
            else if pvCoupled && h.score >= PvCoupledPlayableThreshold then
              (
                PlanEvidenceStatus.PlayablePvCoupled,
                "the current engine line still keeps this idea alive, but it still needs independent support"
              )
            else
              val why =
                if linkedRequests.nonEmpty then
                  if missingSignals.nonEmpty then
                    s"supporting evidence is still incomplete: missing ${describeSignalList(missingSignals)}"
                  else if subplanSignalsMissing(subplanSpec, linkedRequests) then
                    "supporting evidence is still incomplete for this subplan"
                  else "the idea still looks playable, but the supporting evidence is still thin"
                else "the idea fits the position, but supporting evidence is not ready yet"
              (PlanEvidenceStatus.Deferred, why)

          val userFacingEligibility =
            if refutations.nonEmpty || alternativeDominance then UserFacingPlanEligibility.Refuted
            else if supports.nonEmpty then UserFacingPlanEligibility.ProbeBacked
            else if structuralEscalation then UserFacingPlanEligibility.StructuralOnly
            else if pvCoupled && h.score >= PvCoupledPlayableThreshold then UserFacingPlanEligibility.PvCoupledOnly
            else UserFacingPlanEligibility.Deferred

          val claimCertification =
            certifyPlanClaim(
              hypothesis = h,
              allHypotheses = hypotheses,
              supports = supports,
              refutations = refutations,
              missingSignals = missingSignals,
              structuralEscalation = structuralEscalation,
              pvCoupled = pvCoupled,
              userFacingEligibility = userFacingEligibility,
              themeId = themeId,
              alternativeDominance = alternativeDominance
            )

          EvaluatedPlan(
            hypothesis =
              h.copy(
                themeL1 = themeId,
                subplanId = subplanId
              ),
            status = status,
            userFacingEligibility = userFacingEligibility,
            reason = reason,
            supportProbeIds = supports.map(_._2.id).distinct,
            refuteProbeIds = refutations.map(_._2.id).distinct,
            missingSignals = missingSignals,
            pvCoupled = pvCoupled,
            themeL1 = themeId,
            subplanId = subplanId,
            claimCertification = claimCertification
          )
        }

      val userFacingThemeLeaders =
        evaluated
          .filter(_.userFacingEligibility == UserFacingPlanEligibility.ProbeBacked)
          .groupBy(_.themeL1)
          .values
          .toList
          .flatMap(_.sortBy(ep => -rankingScore(ep)).headOption)

      val mainPlanCandidates =
        userFacingThemeLeaders

      val mainPlans =
        mainPlanCandidates
          .sortBy(ep => -rankingScore(ep))
          .take(3)
          .zipWithIndex
          .map { case (ep, idx) =>
            markProbeBacked(
              ep.hypothesis.copy(rank = idx + 1)
            )
          }

      val diagnosticEntries =
        evaluated.map { ep =>
          val linkedRequests = probeRequests.filter(req => requestMatchesHypothesis(req, ep.hypothesis))
          DiagnosticPlanEntry(
            planId = ep.hypothesis.planId,
            subplanId = ep.hypothesis.subplanId,
            planName = ep.hypothesis.planName,
            status = statusCode(ep.status),
            userFacingEligibility = eligibilityCode(ep.userFacingEligibility),
            certificateStatus = certificateStatusCode(ep.claimCertification.certificateStatus),
            quantifier = quantifierCode(ep.claimCertification.quantifier),
            modalityTier = modalityTierCode(ep.claimCertification.modalityTier),
            attributionGrade = attributionCode(ep.claimCertification.attributionGrade),
            stabilityGrade = stabilityCode(ep.claimCertification.stabilityGrade),
            provenanceClass = provenanceCode(ep.claimCertification.provenanceClass),
            taintFlags = ep.claimCertification.taintFlags.map(taintCode),
            ontologyFamily = ontologyCode(ep.claimCertification.ontologyFamily),
            linkedRequestIds = linkedRequests.map(_.id).distinct,
            supportProbeIds = ep.supportProbeIds,
            refuteProbeIds = ep.refuteProbeIds,
            missingSignals = ep.missingSignals,
            reasonCodes =
              diagnosticReasonCodes(
                ep = ep,
                linkedRequests = linkedRequests
              ),
            maxCpLoss =
              linkedRequests
                .map(req => req.maxCpLoss.getOrElse(defaultMaxCpLoss(req.purpose.orElse(req.planName).getOrElse(""))))
                .maxOption,
            worstMoverLoss =
              linkedRequests.flatMap(req =>
                resultsById.getOrElse(req.id, Nil).map(pr => moverLoss(pr, isWhiteToMove))
              ).maxOption,
            alternativeDominance = ep.claimCertification.alternativeDominance
          )
        }
      val claimAudit = summarizeClaimAudit(evaluated)

      PartitionedPlans(
        mainPlans = mainPlans,
        evaluated = evaluated,
        diagnosticSidecar =
          DiagnosticPlanSidecar(
            entries = diagnosticEntries,
            droppedProbeCount = droppedProbeCount,
            droppedReasonCodes =
              if droppedProbeCount > 0 then
                (invalidByRequestId.values.flatten.toList ++ droppedProbeReasons.map(normalizeReasonCode) :+ "contract_validation_drop").distinct.take(8)
              else (invalidByRequestId.values.flatten.toList ++ droppedProbeReasons.map(normalizeReasonCode)).distinct.take(8),
            blockedStrongClaims = claimAudit.blockedStrongClaims,
            downgradedWeakClaims = claimAudit.downgradedWeakClaims,
            attributionFailures = claimAudit.attributionFailures,
            quantifierFailures = claimAudit.quantifierFailures,
            stabilityFailures = claimAudit.stabilityFailures
          )
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
    if !req.purpose.exists(ThemePlanProbePurpose.isThemeValidationPurpose) then false
    else
      val required =
        hypSubplan
          .flatMap(SubplanId.fromId)
          .flatMap(SubplanCatalog.specs.get)
          .map(_.requiredSignals.toSet)
          .getOrElse(Set.empty[String])
      required.nonEmpty && required.subsetOf(req.requiredSignals.toSet)

  private case class ClaimAuditSummary(
      blockedStrongClaims: Int,
      downgradedWeakClaims: Int,
      attributionFailures: Int,
      quantifierFailures: Int,
      stabilityFailures: Int
  )

  private def summarizeClaimAudit(evaluated: List[EvaluatedPlan]): ClaimAuditSummary =
    evaluated.foldLeft(ClaimAuditSummary(0, 0, 0, 0, 0)) { case (acc, ep) =>
      val cert = ep.claimCertification
      val taint = cert.taintFlags.toSet
      val allowsStrong =
        PlayerFacingClaimCertification.allowsStrongMainClaim(
          certificateStatus = cert.certificateStatus,
          quantifier = cert.quantifier,
          attribution = cert.attributionGrade,
          stability = cert.stabilityGrade,
          provenance = cert.provenanceClass,
          taintFlags = taint
        )
      val allowsWeak =
        PlayerFacingClaimCertification.allowsWeakMainClaim(
          certificateStatus = cert.certificateStatus,
          quantifier = cert.quantifier,
          attribution = cert.attributionGrade,
          stability = cert.stabilityGrade,
          provenance = cert.provenanceClass,
          taintFlags = taint
        )
      val strongModalityRequested =
        cert.modalityTier == PlayerFacingClaimModalityTier.Advances ||
          cert.modalityTier == PlayerFacingClaimModalityTier.Forces ||
          cert.modalityTier == PlayerFacingClaimModalityTier.Removes
      ClaimAuditSummary(
        blockedStrongClaims =
          acc.blockedStrongClaims + Option.when(strongModalityRequested && !allowsStrong && !allowsWeak)(1).getOrElse(0),
        downgradedWeakClaims =
          acc.downgradedWeakClaims + Option.when(strongModalityRequested && !allowsStrong && allowsWeak)(1).getOrElse(0),
        attributionFailures =
          acc.attributionFailures +
            Option.when(cert.attributionGrade != PlayerFacingClaimAttributionGrade.Distinctive)(1).getOrElse(0),
        quantifierFailures =
          acc.quantifierFailures +
            Option.when(
              cert.quantifier == PlayerFacingClaimQuantifier.Existential ||
                cert.quantifier == PlayerFacingClaimQuantifier.LineConditioned
            )(1).getOrElse(0),
        stabilityFailures =
          acc.stabilityFailures +
            Option.when(cert.stabilityGrade == PlayerFacingClaimStabilityGrade.Unstable)(1).getOrElse(0)
      )
    }

  private def certifyPlanClaim(
      hypothesis: PlanHypothesis,
      allHypotheses: List[PlanHypothesis],
      supports: List[(ProbeRequest, ProbeResult)],
      refutations: List[(ProbeRequest, ProbeResult)],
      missingSignals: List[String],
      structuralEscalation: Boolean,
      pvCoupled: Boolean,
      userFacingEligibility: UserFacingPlanEligibility,
      themeId: String,
      alternativeDominance: Boolean
  ): ClaimCertification =
    val supportResults = supports.map(_._2).distinctBy(_.id)
    val replyCoverage = supportResults.count(hasReplyCoverage)
    val futureCoverage = supportResults.count(_.futureSnapshot.exists(isPositiveFutureSnapshot))
    val quantifier =
      if supportResults.size >= 2 || (replyCoverage > 0 && futureCoverage > 0) then
        PlayerFacingClaimQuantifier.Universal
      else if replyCoverage > 0 || futureCoverage > 0 then
        PlayerFacingClaimQuantifier.BestResponse
      else if supportResults.nonEmpty then PlayerFacingClaimQuantifier.Existential
      else PlayerFacingClaimQuantifier.LineConditioned
    val modalityTier =
      if supportResults.exists(result => indicatesRemoval(themeId, result)) then
        PlayerFacingClaimModalityTier.Removes
      else if supportResults.exists(result => indicatesForcing(themeId, result)) then
        PlayerFacingClaimModalityTier.Forces
      else if supportResults.exists(result => indicatesAdvancement(themeId, result)) then
        PlayerFacingClaimModalityTier.Advances
      else if supportResults.nonEmpty then PlayerFacingClaimModalityTier.Supports
      else PlayerFacingClaimModalityTier.Available
    val attributionGrade =
      if alternativeDominance then PlayerFacingClaimAttributionGrade.StateOnly
      else if hasNearbySibling(hypothesis, allHypotheses, themeId) then PlayerFacingClaimAttributionGrade.AnchoredButShared
      else PlayerFacingClaimAttributionGrade.Distinctive
    val stabilityGrade =
      if supportResults.isEmpty then PlayerFacingClaimStabilityGrade.Unknown
      else if supportResults.exists(result =>
          result.l1Delta.flatMap(_.collapseReason).exists(_.trim.nonEmpty)
        ) then PlayerFacingClaimStabilityGrade.Unstable
      else if replyCoverage > 0 || futureCoverage > 0 then PlayerFacingClaimStabilityGrade.Stable
      else PlayerFacingClaimStabilityGrade.Unstable
    val provenanceClass =
      userFacingEligibility match
        case UserFacingPlanEligibility.ProbeBacked   => PlayerFacingClaimProvenanceClass.ProbeBacked
        case UserFacingPlanEligibility.StructuralOnly => PlayerFacingClaimProvenanceClass.StructuralOnly
        case UserFacingPlanEligibility.PvCoupledOnly => PlayerFacingClaimProvenanceClass.PvCoupled
        case UserFacingPlanEligibility.Deferred      => PlayerFacingClaimProvenanceClass.Deferred
        case UserFacingPlanEligibility.Refuted       =>
          if supports.nonEmpty then PlayerFacingClaimProvenanceClass.ProbeBacked
          else if pvCoupled then PlayerFacingClaimProvenanceClass.PvCoupled
          else PlayerFacingClaimProvenanceClass.Deferred
    val taintFlags =
      List(
        Option.when(seedIdOf(hypothesis).nonEmpty)(PlayerFacingClaimTaintFlag.Latent),
        Option.when(structuralEscalation)(PlayerFacingClaimTaintFlag.StructuralOnly),
        Option.when(pvCoupled && supports.isEmpty && !structuralEscalation)(PlayerFacingClaimTaintFlag.PvCoupled),
        Option.when(missingSignals.nonEmpty && supports.isEmpty)(PlayerFacingClaimTaintFlag.Deferred)
      ).flatten
    val certificateStatus =
      if refutations.nonEmpty || alternativeDominance then PlayerFacingCertificateStatus.Invalid
      else if supports.nonEmpty && quantifier != PlayerFacingClaimQuantifier.Existential then
        PlayerFacingCertificateStatus.Valid
      else if supports.nonEmpty then PlayerFacingCertificateStatus.WeaklyValid
      else if missingSignals.nonEmpty || pvCoupled || structuralEscalation then
        PlayerFacingCertificateStatus.WeaklyValid
      else PlayerFacingCertificateStatus.Invalid
    ClaimCertification(
      certificateStatus = certificateStatus,
      quantifier = quantifier,
      modalityTier = modalityTier,
      attributionGrade = attributionGrade,
      stabilityGrade = stabilityGrade,
      provenanceClass = provenanceClass,
      taintFlags = taintFlags.distinct,
      ontologyFamily = ontologyFamily(themeId, hypothesis),
      alternativeDominance = alternativeDominance
    )

  private def hasNearbySibling(
      hypothesis: PlanHypothesis,
      allHypotheses: List[PlanHypothesis],
      themeId: String
  ): Boolean =
    themeId != ThemeL1.Unknown.id && allHypotheses.exists { other =>
      other.planId != hypothesis.planId &&
        hypothesisThemeId(other) == themeId &&
        rankingScore(other) >= rankingScore(hypothesis) - 0.04 &&
        rankingScore(other) <= rankingScore(hypothesis) + 0.08 &&
        normalizeText(other.planName) != normalizeText(hypothesis.planName)
    }

  private def isAlternativeDominated(
      target: PlanHypothesis,
      allHypotheses: List[PlanHypothesis]
  ): Boolean =
    val targetTheme = hypothesisThemeId(target)
    targetTheme != ThemeL1.Unknown.id && allHypotheses.exists { other =>
      other.planId != target.planId &&
        hypothesisThemeId(other) == targetTheme &&
        rankingScore(other) >= rankingScore(target) + 0.08 &&
        other.viability.score >= target.viability.score &&
        normalizeText(other.planName) != normalizeText(target.planName)
    }

  private def rankingScore(h: PlanHypothesis): Double =
    h.score + h.viability.score * 0.1

  private def indicatesAdvancement(themeId: String, result: ProbeResult): Boolean =
    result.futureSnapshot.exists(snapshot =>
      snapshot.planPrereqsMet.nonEmpty ||
        snapshot.planBlockersRemoved.nonEmpty
    ) || Set(
      ThemeL1.OpeningPrinciples.id,
      ThemeL1.PawnBreakPreparation.id,
      ThemeL1.AdvantageTransformation.id,
      ThemeL1.FlankInfrastructure.id,
      ThemeL1.PieceRedeployment.id
    ).contains(themeId)

  private def indicatesForcing(themeId: String, result: ProbeResult): Boolean =
    (themeId == ThemeL1.FavorableExchange.id &&
      hasReplyCoverage(result)) ||
      result.keyMotifs.exists(motif =>
        containsAny(normalizeText(motif), List("forcing", "exchange", "trade", "simplif"))
      )

  private def indicatesRemoval(themeId: String, result: ProbeResult): Boolean =
    Set(
      ThemeL1.RestrictionProphylaxis.id,
      ThemeL1.WeaknessFixation.id
    ).contains(themeId) &&
      result.futureSnapshot.exists(snapshot =>
        snapshot.resolvedThreatKinds.nonEmpty ||
          snapshot.targetsDelta.strategicRemoved.nonEmpty
      )

  private def ontologyFamily(
      themeId: String,
      hypothesis: PlanHypothesis
  ): PlayerFacingClaimOntologyFamily =
    themeId match
      case id if id == ThemeL1.RestrictionProphylaxis.id =>
        val low = normalizeText(hypothesis.planName)
        if containsAny(low, List("route", "entry", "denial")) then
          PlayerFacingClaimOntologyFamily.RouteDenial
        else if containsAny(low, List("color", "complex", "bishop")) then
          PlayerFacingClaimOntologyFamily.ColorComplexSqueeze
        else PlayerFacingClaimOntologyFamily.LongTermRestraint
      case id if id == ThemeL1.FavorableExchange.id     => PlayerFacingClaimOntologyFamily.Exchange
      case id if id == ThemeL1.PawnBreakPreparation.id  => PlayerFacingClaimOntologyFamily.PlanAdvance
      case id if id == ThemeL1.PieceRedeployment.id     => PlayerFacingClaimOntologyFamily.PlanAdvance
      case id if id == ThemeL1.SpaceClamp.id            => PlayerFacingClaimOntologyFamily.CounterplayRestraint
      case id if id == ThemeL1.WeaknessFixation.id      => PlayerFacingClaimOntologyFamily.ResourceRemoval
      case _                                            => PlayerFacingClaimOntologyFamily.Unknown

  private def rankingScore(ep: EvaluatedPlan): Double =
    ep.hypothesis.score + (if isDefaultSubplan(ep.themeL1, ep.subplanId) then 0.0 else 0.03)

  private def isDefaultSubplan(themeId: String, subplanId: Option[String]): Boolean =
    val theme = ThemeL1.fromId(themeId).getOrElse(ThemeL1.Unknown)
    val default = ThemeResolver.defaultSubplanForTheme(theme).map(_.id)
    default.exists(id => subplanId.contains(id))

  private def statusCode(status: PlanEvidenceStatus): String =
    status match
      case PlanEvidenceStatus.PlayableEvidenceBacked => "playable_evidence_backed"
      case PlanEvidenceStatus.PlayablePvCoupled      => "playable_pv_coupled"
      case PlanEvidenceStatus.Refuted                => "refuted"
      case PlanEvidenceStatus.Deferred               => "deferred"

  private def eligibilityCode(eligibility: UserFacingPlanEligibility): String =
    eligibility match
      case UserFacingPlanEligibility.ProbeBacked   => "probe_backed"
      case UserFacingPlanEligibility.StructuralOnly => "structural_only"
      case UserFacingPlanEligibility.PvCoupledOnly => "pv_coupled_only"
      case UserFacingPlanEligibility.Deferred      => "deferred"
      case UserFacingPlanEligibility.Refuted       => "refuted"

  private def certificateStatusCode(status: PlayerFacingCertificateStatus): String =
    status match
      case PlayerFacingCertificateStatus.Valid             => "valid"
      case PlayerFacingCertificateStatus.WeaklyValid       => "weakly_valid"
      case PlayerFacingCertificateStatus.Invalid           => "invalid"
      case PlayerFacingCertificateStatus.StaleOrMismatched => "stale_or_mismatched"

  private def quantifierCode(quantifier: PlayerFacingClaimQuantifier): String =
    quantifier match
      case PlayerFacingClaimQuantifier.Universal      => "universal"
      case PlayerFacingClaimQuantifier.BestResponse   => "best_response"
      case PlayerFacingClaimQuantifier.Existential    => "existential"
      case PlayerFacingClaimQuantifier.LineConditioned => "line_conditioned"

  private def modalityTierCode(modality: PlayerFacingClaimModalityTier): String =
    modality match
      case PlayerFacingClaimModalityTier.Available => "available"
      case PlayerFacingClaimModalityTier.Supports  => "supports"
      case PlayerFacingClaimModalityTier.Advances  => "advances"
      case PlayerFacingClaimModalityTier.Forces    => "forces"
      case PlayerFacingClaimModalityTier.Removes   => "removes"

  private def attributionCode(attribution: PlayerFacingClaimAttributionGrade): String =
    attribution match
      case PlayerFacingClaimAttributionGrade.Distinctive      => "distinctive"
      case PlayerFacingClaimAttributionGrade.AnchoredButShared => "anchored_but_shared"
      case PlayerFacingClaimAttributionGrade.StateOnly        => "state_only"

  private def stabilityCode(stability: PlayerFacingClaimStabilityGrade): String =
    stability match
      case PlayerFacingClaimStabilityGrade.Stable   => "stable"
      case PlayerFacingClaimStabilityGrade.Unstable => "unstable"
      case PlayerFacingClaimStabilityGrade.Unknown  => "unknown"

  private def provenanceCode(provenance: PlayerFacingClaimProvenanceClass): String =
    provenance match
      case PlayerFacingClaimProvenanceClass.ProbeBacked   => "probe_backed"
      case PlayerFacingClaimProvenanceClass.StructuralOnly => "structural_only"
      case PlayerFacingClaimProvenanceClass.PvCoupled     => "pv_coupled"
      case PlayerFacingClaimProvenanceClass.Deferred      => "deferred"

  private def taintCode(flag: PlayerFacingClaimTaintFlag): String =
    flag match
      case PlayerFacingClaimTaintFlag.Latent           => "latent"
      case PlayerFacingClaimTaintFlag.PvCoupled        => "pv_coupled"
      case PlayerFacingClaimTaintFlag.Deferred         => "deferred"
      case PlayerFacingClaimTaintFlag.StructuralOnly   => "structural_only"
      case PlayerFacingClaimTaintFlag.BranchConditioned => "branch_conditioned"

  private def ontologyCode(family: PlayerFacingClaimOntologyFamily): String =
    family match
      case PlayerFacingClaimOntologyFamily.Access              => "access"
      case PlayerFacingClaimOntologyFamily.Pressure            => "pressure"
      case PlayerFacingClaimOntologyFamily.Exchange            => "exchange"
      case PlayerFacingClaimOntologyFamily.CounterplayRestraint => "counterplay_restraint"
      case PlayerFacingClaimOntologyFamily.ResourceRemoval     => "resource_removal"
      case PlayerFacingClaimOntologyFamily.PlanAdvance         => "plan_advance"
      case PlayerFacingClaimOntologyFamily.RouteDenial         => "route_denial"
      case PlayerFacingClaimOntologyFamily.ColorComplexSqueeze => "color_complex_squeeze"
      case PlayerFacingClaimOntologyFamily.LongTermRestraint   => "long_term_restraint"
      case PlayerFacingClaimOntologyFamily.PieceImprovement    => "piece_improvement"
      case PlayerFacingClaimOntologyFamily.KingSafety          => "king_safety"
      case PlayerFacingClaimOntologyFamily.TechnicalConversion => "technical_conversion"
      case PlayerFacingClaimOntologyFamily.Unknown             => "unknown"

  private def diagnosticReasonCodes(
      ep: EvaluatedPlan,
      linkedRequests: List[ProbeRequest]
  ): List[String] =
    val base =
      ep.userFacingEligibility match
        case UserFacingPlanEligibility.ProbeBacked   => List("validated_support_probe")
        case UserFacingPlanEligibility.StructuralOnly => List("structural_escalation_only")
        case UserFacingPlanEligibility.PvCoupledOnly => List("pv_coupled_only")
        case UserFacingPlanEligibility.Deferred      => List("support_not_ready")
        case UserFacingPlanEligibility.Refuted       => List("probe_refuted")
    val certificateCodes =
      List(
        s"certificate_${certificateStatusCode(ep.claimCertification.certificateStatus)}",
        s"quantifier_${quantifierCode(ep.claimCertification.quantifier)}",
        s"modality_${modalityTierCode(ep.claimCertification.modalityTier)}",
        s"attribution_${attributionCode(ep.claimCertification.attributionGrade)}",
        s"stability_${stabilityCode(ep.claimCertification.stabilityGrade)}",
        s"provenance_${provenanceCode(ep.claimCertification.provenanceClass)}",
        s"ontology_${ontologyCode(ep.claimCertification.ontologyFamily)}"
      ) ++ ep.claimCertification.taintFlags.map(flag => s"taint_${taintCode(flag)}")
    val requestCodes =
      Option.when(linkedRequests.isEmpty)("no_linked_probe_request").toList
    val alternativeCodes =
      Option.when(ep.claimCertification.alternativeDominance)("alternative_dominance").toList
    (base ++ certificateCodes ++ requestCodes ++ alternativeCodes ++ ep.missingSignals).distinct

  private def normalizeReasonCode(raw: String): String =
    Option(raw)
      .map(_.trim.toLowerCase.replaceAll("[^a-z0-9]+", "_"))
      .filter(_.nonEmpty)
      .getOrElse("validation_gap")

  private def markProbeBacked(h: PlanHypothesis): PlanHypothesis =
    h.copy(evidenceSources = (h.evidenceSources :+ "probe_backed:validated_support").distinct)

  private def isSupportive(req: ProbeRequest, pr: ProbeResult, isWhiteToMove: Boolean): Boolean =
    val purpose = req.purpose.orElse(pr.purpose).getOrElse("")
    if ProbePurposeClassifier.isRefutationPurpose(purpose) then
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
    if ProbePurposeClassifier.isRefutationPurpose(purpose) then hardLoss || tacticalCollapse || mateAgainstMover
    else hardLoss || mateAgainstMover

  private def moverLoss(pr: ProbeResult, isWhiteToMove: Boolean): Int =
    if isWhiteToMove then -pr.deltaVsBaseline else pr.deltaVsBaseline

  private def isMateAgainstMover(pr: ProbeResult, isWhiteToMove: Boolean): Boolean =
    pr.mate match
      case Some(m) if isWhiteToMove => m < 0
      case Some(m) if !isWhiteToMove => m > 0
      case _ => false

  private def defaultMaxCpLoss(purpose: String): Int =
    if ThemePlanProbePurpose.isThemeValidationPurpose(purpose) then 95
    else purpose match
      case "latent_plan_immediate"  => 80
      case "free_tempo_branches"    => 90
      case "latent_plan_refutation" => 120
      case "reply_multipv" | "defense_reply_multipv" | "convert_reply_multipv" => 100
      case _ => 110

  private def hasReplyCoverage(result: ProbeResult): Boolean =
    result.bestReplyPv.nonEmpty || result.replyPvs.exists(_.exists(_.nonEmpty))

  private def isPositiveFutureSnapshot(snapshot: lila.llm.model.FutureSnapshot): Boolean =
    snapshot.planPrereqsMet.nonEmpty ||
      snapshot.planBlockersRemoved.nonEmpty ||
      snapshot.targetsDelta.strategicAdded.nonEmpty ||
      snapshot.resolvedThreatKinds.nonEmpty

  private def hasStructuralEvidence(h: PlanHypothesis): Boolean =
    val evidenceLow = h.evidenceSources.map(_.toLowerCase)
    evidenceLow.exists(_.startsWith("structural_state:"))

  private def describeSignalList(signals: List[String]): String =
    signals
      .map(humanizeSignalName)
      .filter(_.nonEmpty)
      .distinct
      .take(3)
      .mkString(", ")

  private def humanizeSignalName(raw: String): String =
    raw match
      case "replyPvs"              => "a concrete reply line"
      case "futureSnapshot"        => "a stable follow-up position"
      case "l1Delta"               => "positional follow-through"
      case "keyMotifs"             => "the underlying motif"
      case "resolvedThreatKinds"   => "the resulting threats"
      case "newThreatKinds"        => "new attacking chances"
      case "targetsDelta"          => "target changes"
      case "planBlockersRemoved"   => "removed blockers"
      case "planPrereqsMet"        => "met plan conditions"
      case "bestReplyPv"           => "the best defensive line"
      case "requiredSignals"       => "the requested supporting details"
      case other if Option(other).exists(_.trim.nonEmpty) =>
        other
          .replaceAll("([a-z])([A-Z])", "$1 $2")
          .replace('_', ' ')
          .trim
          .toLowerCase
      case _ => "supporting details"

  private def containsAny(text: String, needles: List[String]): Boolean =
    needles.exists(text.contains)
