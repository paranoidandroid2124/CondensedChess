package lila.commentary.analysis

import play.api.libs.json.*
import lila.commentary.model.{ ProbeContractValidator, ProbeRequest, ProbeResult }
import lila.commentary.model.authoring.PlanHypothesis
import lila.commentary.model.strategic.PreventedPlan
import lila.commentary.analysis.PlanTaxonomy.{ PlanTheme, ThemeResolver, SubplanCatalog, PlanKind, SubplanSpec }
import lila.commentary.analysis.structure.TranspositionPvAligner

/**
 * Evaluates strategic plan hypotheses with a fail-closed evidence policy.
 *
 * Plan-first generation is allowed, but promotion to "main strategic plans"
 * is controlled by evidence status partitioning.
 */
object PlanEvidenceEvaluator:

  private val DeniedResourcePrefix = "denied_resource:"
  private val ProphylacticResourceClasses =
    Set(
      "break",
      "entry_square",
      "forcing_threat",
      "piece_activity",
      "counterplay_route",
      "route_node",
      "reroute_square",
      "pressure",
      "color_complex_escape"
    )

  enum PlanEvidenceStatus:
    case PlayableEvidenceBacked
    case PlayableTranspositionAligned
    case PlayableStructuralOnly
    case PlayablePvCoupled
    case Refuted
    case Deferred

  enum UserFacingPlanEligibility:
    case ProbeBacked
    case TranspositionAligned
    case StructuralOnly
    case PvCoupledOnly
    case Deferred
    case Refuted

  def preventedPlanSignalTerms(
      plan: PreventedPlan,
      includeDeniedSquares: Boolean = false,
      includeDeniedEntryScope: Boolean = false
  ): List[String] =
    List(
      Option.when(plan.counterplayScoreDrop > 0)(
        s"counterplay_drop:${plan.counterplayScoreDrop}"
      ),
      plan.breakNeutralized.flatMap(signal => cleanText(signal).map(value => s"neutralized_break:$value")),
      Option.when(includeDeniedSquares && plan.deniedSquares.nonEmpty)(
        s"denied_squares:${plan.deniedSquares.map(_.key).distinct.sorted.mkString(",")}"
      ),
      plan.deniedResourceClass.flatMap(resourceClass =>
        deniedResourceTerm(resourceClass)
      ),
      plan.deniedEntryScope
        .filter(_ => includeDeniedEntryScope)
        .flatMap(scope => cleanText(scope).map(value => s"denied_entry_scope:$value"))
    ).flatten

  def deniedResourceTerm(resourceClass: String): Option[String] =
    cleanText(resourceClass).map(value => s"$DeniedResourcePrefix$value")

  def prophylacticResourceClassKey(raw: String): Option[String] =
    val key = normalizeResourceKey(raw)
    Option.when(ProphylacticResourceClasses.contains(key))(key)

  def prophylacticDeniedResourceTerm(raw: String): Option[String] =
    prophylacticResourceClassKey(raw).flatMap(deniedResourceTerm)

  def isProphylacticDeniedResourceTerm(raw: String): Boolean =
    val token = normalizeToken(raw)
    token.startsWith(DeniedResourcePrefix) &&
      prophylacticResourceClassKey(token.stripPrefix(DeniedResourcePrefix)).nonEmpty

  case class EvaluatedPlan(
      hypothesis: PlanHypothesis,
      status: PlanEvidenceStatus,
      userFacingEligibility: UserFacingPlanEligibility,
      reason: String,
      supportProbeIds: List[String] = Nil,
      transpositionProofIds: List[String] = Nil,
      refuteProbeIds: List[String] = Nil,
      missingSignals: List[String] = Nil,
      pvCoupled: Boolean = false,
      themeL1: String = PlanTheme.Unknown.id,
      subplanId: Option[String] = None,
      planProposal: PlanClaimBoundary.PlanProposal = PlanClaimBoundary.PlanProposal.empty,
      claimCertification: ClaimCertification = ClaimCertification()
  )

  def isMainAdmittedPlan(plan: EvaluatedPlan): Boolean =
    plan.userFacingEligibility match
      case UserFacingPlanEligibility.ProbeBacked =>
        plan.supportProbeIds.exists(nonEmptyToken)
      case UserFacingPlanEligibility.TranspositionAligned =>
        plan.transpositionProofIds.exists(nonEmptyToken) &&
          plan.claimCertification.provenanceClass == PlayerFacingClaimProvenanceClass.TranspositionAligned
      case _ => false

  def isBoundedPracticalSupportPlan(plan: EvaluatedPlan): Boolean =
    plan.userFacingEligibility == UserFacingPlanEligibility.StructuralOnly ||
      plan.userFacingEligibility == UserFacingPlanEligibility.PvCoupledOnly

  def claimCertificationTerms(plan: EvaluatedPlan): List[String] =
    claimCertificationTerms(plan.claimCertification)

  def claimCertificationTerms(cert: ClaimCertification): List[String] =
    List(
      Option.when(cert.quantifier == PlayerFacingClaimQuantifier.Universal)("universal"),
      Option.when(cert.quantifier == PlayerFacingClaimQuantifier.BestResponse)("best_response"),
      Option.when(cert.stabilityGrade == PlayerFacingClaimStabilityGrade.Stable)("stable"),
      Option.when(cert.provenanceClass == PlayerFacingClaimProvenanceClass.ProbeBacked)("probe_backed")
    ).flatten

  def planProposal(plan: EvaluatedPlan): PlanClaimBoundary.PlanProposal =
    if plan.planProposal != PlanClaimBoundary.PlanProposal.empty then plan.planProposal
    else
      PlanClaimBoundary.PlanProposal.fromHypothesis(
        plan.hypothesis.copy(
          themeL1 = plan.themeL1,
          subplanId = plan.subplanId
        )
      )

  def planTheme(plan: EvaluatedPlan): PlanTheme =
    val proposal = planProposal(plan)
    if proposal.theme != PlanTheme.Unknown then proposal.theme
    else PlanTheme.fromId(plan.themeL1).getOrElse(PlanTheme.Unknown)

  def planKind(plan: EvaluatedPlan): Option[PlanKind] =
    planProposal(plan).supportKind.orElse(plan.subplanId.flatMap(PlanKind.fromId))

  def planSupport(plan: EvaluatedPlan): PlanClaimBoundary.PlanSupport =
    PlanClaimBoundary.PlanSupport(
      proposal = planProposal(plan),
      supportProbeIds = plan.supportProbeIds,
      transpositionProofIds = plan.transpositionProofIds,
      refuteProbeIds = plan.refuteProbeIds,
      missingSignals = plan.missingSignals
    )

  def admittedPlanClaim(plan: EvaluatedPlan): Option[PlanClaimBoundary.AdmittedPlanClaim] =
    Option.when(isMainAdmittedPlan(plan))(
      PlanClaimBoundary.AdmittedPlanClaim(
        proposal = planProposal(plan),
        supportProbeIds = plan.supportProbeIds,
        transpositionProofIds = plan.transpositionProofIds
      )
    )

  def supportProbeTerm(probeId: String): Option[String] =
    cleanText(probeId).map(id => s"support_probe:$id")

  def supportProbeTerms(probeIds: Iterable[String]): List[String] =
    probeIds.toList.flatMap(supportProbeTerm)

  def supportProbeTerms(plan: EvaluatedPlan): List[String] =
    supportProbeTerms(plan.supportProbeIds)

  case class ClaimCertification(
      certificateStatus: PlayerFacingCertificateStatus = PlayerFacingCertificateStatus.Invalid,
      quantifier: PlayerFacingClaimQuantifier = PlayerFacingClaimQuantifier.Existential,
      modalityTier: PlayerFacingClaimModalityTier = PlayerFacingClaimModalityTier.Available,
      attributionGrade: PlayerFacingClaimAttributionGrade = PlayerFacingClaimAttributionGrade.StateOnly,
      stabilityGrade: PlayerFacingClaimStabilityGrade = PlayerFacingClaimStabilityGrade.Unknown,
      provenanceClass: PlayerFacingClaimProvenanceClass = PlayerFacingClaimProvenanceClass.Deferred,
      taintFlags: List[PlayerFacingClaimTaintFlag] = Nil,
      ontologyFamily: PlayerFacingClaimOntologyKind = PlayerFacingClaimOntologyKind.Unknown,
      alternativeDominance: Boolean = false
  )

  case class ProbeValidation(
      validResults: List[ProbeResult],
      droppedCount: Int,
      droppedReasons: List[String],
      invalidByRequestId: Map[String, List[String]],
      softByRequestId: Map[String, List[String]] = Map.empty
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
      selectedMainEvaluatedPlans: List[EvaluatedPlan] = Nil,
      diagnosticSidecar: DiagnosticPlanSidecar = DiagnosticPlanSidecar()
  )

  case class StrategicPlanEvidenceView(
      selectedPlans: List[EvaluatedPlan] = Nil,
      evaluatedPlans: List[EvaluatedPlan] = Nil
  ):
    def isEmpty: Boolean = selectedPlans.isEmpty && evaluatedPlans.isEmpty
    def nonEmpty: Boolean = !isEmpty

    def mainAdmittedPlans: List[EvaluatedPlan] =
      selectedPlans.filter(PlanEvidenceEvaluator.isMainAdmittedPlan)

    def mainAdmittedClaims: List[PlanClaimBoundary.AdmittedPlanClaim] =
      mainAdmittedPlans.flatMap(PlanEvidenceEvaluator.admittedPlanClaim)

    def probeBackedPlans: List[EvaluatedPlan] =
      mainAdmittedPlans.filter(_.userFacingEligibility == UserFacingPlanEligibility.ProbeBacked)

    def probeBackedClaims: List[PlanClaimBoundary.AdmittedPlanClaim] =
      probeBackedPlans.flatMap(PlanEvidenceEvaluator.admittedPlanClaim)

    def transpositionAlignedMainPlans: List[EvaluatedPlan] =
      mainAdmittedPlans.filter(_.userFacingEligibility == UserFacingPlanEligibility.TranspositionAligned)

    def transpositionAlignedMainClaims: List[PlanClaimBoundary.AdmittedPlanClaim] =
      transpositionAlignedMainPlans.flatMap(PlanEvidenceEvaluator.admittedPlanClaim)

    def pvCoupledPlans: List[EvaluatedPlan] =
      evaluatedPlans.filter(_.userFacingEligibility == UserFacingPlanEligibility.PvCoupledOnly)

    def deferredPlans: List[EvaluatedPlan] =
      evaluatedPlans.filter(_.userFacingEligibility == UserFacingPlanEligibility.Deferred)

    def refutedPlans: List[EvaluatedPlan] =
      evaluatedPlans.filter(_.userFacingEligibility == UserFacingPlanEligibility.Refuted)

    def structuralOnlyPlans: List[EvaluatedPlan] =
      evaluatedPlans.filter(_.userFacingEligibility == UserFacingPlanEligibility.StructuralOnly)

    def boundedPracticalPlans: List[EvaluatedPlan] =
      evaluatedPlans.filter(PlanEvidenceEvaluator.isBoundedPracticalSupportPlan)

    def hasProbeBacked: Boolean = probeBackedPlans.nonEmpty

    def hasMainAdmitted: Boolean = mainAdmittedPlans.nonEmpty

    def bestEvidenceFor(planId: String, subplanId: Option[String]): Option[EvaluatedPlan] =
      (selectedPlans ++ evaluatedPlans)
        .filter(plan =>
          normalizeText(plan.hypothesis.planId) == normalizeText(planId) &&
            subplanId.forall(id => plan.hypothesis.subplanId.exists(normalizeText(_) == normalizeText(id)))
        )
        .sortBy(plan => -rankingScore(plan))
        .headOption

    def probeBackedMainPlans: List[PlanHypothesis] =
      probeBackedPlans.map(_.hypothesis)

    def mainAdmittedPlanHypotheses: List[PlanHypothesis] =
      mainAdmittedPlans.map(_.hypothesis)

    def probeBackedPlanNames: List[String] =
      probeBackedMainPlans.flatMap(plan => cleanText(plan.planName))

    def mainAdmittedPlanNames: List[String] =
      mainAdmittedPlanHypotheses.flatMap(plan => cleanText(plan.planName))

    def leadingProbeBackedPlanName: Option[String] =
      probeBackedPlanNames.headOption

    def leadingMainAdmittedPlanName: Option[String] =
      mainAdmittedPlanNames.headOption

    def leadingProbeBackedCertification: Option[ClaimCertification] =
      probeBackedPlans.headOption.map(_.claimCertification)

  object StrategicPlanEvidenceView:
    val empty: StrategicPlanEvidenceView = StrategicPlanEvidenceView()

    def from(partition: PartitionedPlans): StrategicPlanEvidenceView =
      StrategicPlanEvidenceView(
        selectedPlans = partition.selectedMainEvaluatedPlans,
        evaluatedPlans = partition.evaluated
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
    val softByRequest = scala.collection.mutable.Map.empty[String, List[String]]
    var dropped = 0

    rawResults.foreach { pr =>
      val reqOpt = requestById.get(pr.id)
      val validation =
        reqOpt match
          case Some(req) => ProbeContractValidator.validateAgainstRequest(req, pr)
          case None      => ProbeContractValidator.validate(pr)

      if validation.isValid then
        valid += pr
        reqOpt.foreach { req =>
          val soft = validation.softReasonCodes.distinct.filter(_.nonEmpty)
          if soft.nonEmpty then
            val existing = softByRequest.getOrElse(req.id, Nil)
            softByRequest.update(req.id, (existing ++ soft).distinct)
        }
      else
        dropped += 1
        val reason =
          if validation.missingSignals.nonEmpty then
            s"supporting evidence is still incomplete: missing ${describeSignalList(validation.missingSignals)}"
          else if validation.hardReasonCodes.nonEmpty then
            "supporting evidence is still incomplete after validation"
          else "supporting evidence is still incomplete"
        droppedReasons += reason
        reqOpt.foreach { req =>
          val details =
            (validation.missingSignals ++ validation.hardReasonCodes).distinct.filter(_.nonEmpty)
          if details.nonEmpty then
            val existing = invalidByRequest.getOrElse(req.id, Nil)
            invalidByRequest.update(req.id, (existing ++ details).distinct)
        }
    }

    ProbeValidation(
      validResults = valid.toList,
      droppedCount = dropped,
      droppedReasons = droppedReasons.toList.distinct.take(4),
      invalidByRequestId = invalidByRequest.toMap,
      softByRequestId = softByRequest.toMap
    )

  def partition(
      hypotheses: List[PlanHypothesis],
      probeRequests: List[ProbeRequest],
      validatedProbeResults: List[ProbeResult],
      rulePlanIds: Set[String],
      isWhiteToMove: Boolean,
      droppedProbeCount: Int,
      droppedProbeReasons: List[String] = Nil,
      invalidByRequestId: Map[String, List[String]] = Map.empty,
      softByRequestId: Map[String, List[String]] = Map.empty,
      transpositionProofs: List[TranspositionPvAligner.TranspositionProof] = Nil
  ): PartitionedPlans =
    if hypotheses.isEmpty then
      PartitionedPlans(
        mainPlans = Nil,
        evaluated = Nil,
        selectedMainEvaluatedPlans = Nil,
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
          val planProposal = PlanClaimBoundary.PlanProposal.fromHypothesis(h)
          val themeId = planProposal.theme.id
          val subplanId = planProposal.supportKind.map(_.id)
          val subplanSpec =
            subplanId.flatMap(PlanKind.fromId).flatMap(SubplanCatalog.specs.get)
          val linkedRequests = probeRequests.filter(req => requestMatchesHypothesis(req, h))
          val linkedResults =
            linkedRequests.flatMap(req => resultsById.getOrElse(req.id, Nil).map(pr => req -> pr))
          val refutations =
            linkedResults.filter { case (req, pr) => isRefuted(req, pr, isWhiteToMove) }
          val supports =
            linkedResults.filter { case (req, pr) => isSupportive(req, pr, isWhiteToMove) }
          val transpositionSupports =
            transpositionProofs.filter(proof => transpositionProofMatchesHypothesis(proof, h))
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
            else if supports.nonEmpty then
              (PlanEvidenceStatus.PlayableEvidenceBacked, "backed by concrete supporting lines")
            else if transpositionSupports.nonEmpty then
              (PlanEvidenceStatus.PlayableTranspositionAligned, "backed by a transposition-aligned target line")
            else if structuralEscalation then
              (PlanEvidenceStatus.PlayableStructuralOnly, "backed by the structure with no refutation signal")
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
            if refutations.nonEmpty then UserFacingPlanEligibility.Refuted
            else if supports.nonEmpty then UserFacingPlanEligibility.ProbeBacked
            else if transpositionSupports.nonEmpty then UserFacingPlanEligibility.TranspositionAligned
            else if structuralEscalation then UserFacingPlanEligibility.StructuralOnly
            else if pvCoupled && h.score >= PvCoupledPlayableThreshold then UserFacingPlanEligibility.PvCoupledOnly
            else UserFacingPlanEligibility.Deferred

          val claimCertification =
            certifyPlanClaim(
              hypothesis = h,
              allHypotheses = hypotheses,
              supports = supports,
              transpositionSupports = transpositionSupports,
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
            transpositionProofIds = transpositionSupports.map(_.proofId).distinct,
            refuteProbeIds = refutations.map(_._2.id).distinct,
            missingSignals = missingSignals,
            pvCoupled = pvCoupled,
            themeL1 = themeId,
            subplanId = subplanId,
            planProposal = planProposal,
            claimCertification = claimCertification
          )
        }

      val userFacingThemeLeaders =
        evaluated
          .filter(isMainAdmittedPlan)
          .groupBy(_.themeL1)
          .values
          .toList
          .flatMap(_.sortBy(ep => -mainAdmissionScore(ep)).headOption)

      val mainPlanCandidates =
        userFacingThemeLeaders

      val selectedMainEvaluatedPlans =
        mainPlanCandidates
          .sortBy(ep => -mainAdmissionScore(ep))
          .take(3)
          .zipWithIndex
          .map { case (ep, idx) =>
            val hyp = ep.hypothesis.copy(rank = idx + 1)
            val marked = markMainAdmitted(hyp, ep.userFacingEligibility)
            ep.copy(hypothesis = marked)
          }

      val mainPlans =
        selectedMainEvaluatedPlans.map(_.hypothesis)

      val diagnosticEntries =
        evaluated.map { ep =>
          val linkedRequests = probeRequests.filter(req => requestMatchesHypothesis(req, ep.hypothesis))
          val softReasonCodes =
            linkedRequests.flatMap(req => softByRequestId.getOrElse(req.id, Nil)).distinct
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
                linkedRequests = linkedRequests,
                softReasonCodes = softReasonCodes
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
        selectedMainEvaluatedPlans = selectedMainEvaluatedPlans,
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
    val reqPlanId = req.planId.map(_.trim.toLowerCase).filter(_.nonEmpty)
    val reqPlanName =
      req.planName.map(name => normalizeText(ThemeResolver.stripSubplanAnnotations(name))).filter(_.nonEmpty)
    val reqSeed = req.seedId.map(_.trim.toLowerCase).filter(_.nonEmpty)
    val hypPlanId = h.planId.trim.toLowerCase
    val hypPlanName = normalizeText(h.planName)
    val hypSeed = seedIdOf(h).map(_.trim.toLowerCase)
    val reqTheme = reqThemeId(req)
    val hypProposal = PlanClaimBoundary.PlanProposal.fromHypothesis(h)
    val hypTheme = hypProposal.theme.id
    val reqSubplan = reqPlanKind(req)
    val hypSubplan = hypProposal.supportKind.map(_.id)
    val themeAligned = reqTheme.nonEmpty && reqTheme == hypTheme
    val subplanAligned = reqSubplan.forall(rsp => hypSubplan.contains(rsp))
    val contractCompatible = requestContractCompatible(req, hypSubplan)
    val explicitBinding = reqPlanId.nonEmpty || reqPlanName.nonEmpty || reqSeed.nonEmpty
    val explicitChecks = List(
      reqPlanId.map(_ == hypPlanId),
      reqSeed.map(seed => hypSeed.contains(seed)),
      reqPlanName.map(_ == hypPlanName)
    ).flatten
    val explicitMatch = explicitChecks.nonEmpty && explicitChecks.forall(identity)
    explicitMatch || (!explicitBinding && themeAligned && (subplanAligned || contractCompatible))

  private def transpositionProofMatchesHypothesis(
      proof: TranspositionPvAligner.TranspositionProof,
      hypothesis: PlanHypothesis
  ): Boolean =
    normalizeText(proof.planId) == normalizeText(hypothesis.planId) &&
      proof.subplanId.forall(id => hypothesisPlanKind(hypothesis).contains(id))

  private def seedIdOf(h: PlanHypothesis): Option[String] =
    h.evidenceSources
      .flatMap(ThemeResolver.latentSeedIdFromEvidenceSource)
      .headOption
      .filter(_.nonEmpty)

  private def normalizeText(raw: String): String =
    raw.toLowerCase.filter(_.isLetterOrDigit)

  private def normalizeToken(raw: String): String =
    Option(raw).getOrElse("").trim.toLowerCase

  private def normalizeResourceKey(raw: String): String =
    normalizeToken(raw).replace('-', '_').replaceAll("\\s+", "_")

  private def reqThemeId(req: ProbeRequest): String =
    PlanClaimBoundary.PlanProposal.fromProbeRequest(req).fallbackTheme.map(_.id).getOrElse("")

  private def reqPlanKind(req: ProbeRequest): Option[String] =
    PlanClaimBoundary.PlanProposal.fromProbeRequest(req).supportKind.map(_.id)

  private def hypothesisThemeId(h: PlanHypothesis): String =
    PlanClaimBoundary.PlanProposal.fromHypothesis(h).theme.id

  private def hypothesisPlanKind(h: PlanHypothesis): Option[String] =
    PlanClaimBoundary.PlanProposal.fromHypothesis(h).supportKind.map(_.id)

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
          .flatMap(PlanKind.fromId)
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
        PlayerFacingClaimProof.allowsStrongMainClaim(
          certificateStatus = cert.certificateStatus,
          quantifier = cert.quantifier,
          attribution = cert.attributionGrade,
          stability = cert.stabilityGrade,
          provenance = cert.provenanceClass,
          taintFlags = taint
        )
      val allowsWeak =
        PlayerFacingClaimProof.allowsWeakMainClaim(
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
      transpositionSupports: List[TranspositionPvAligner.TranspositionProof],
      refutations: List[(ProbeRequest, ProbeResult)],
      missingSignals: List[String],
      structuralEscalation: Boolean,
      pvCoupled: Boolean,
      userFacingEligibility: UserFacingPlanEligibility,
      themeId: String,
      alternativeDominance: Boolean
  ): ClaimCertification =
    val supportResults = supports.map(_._2).distinctBy(_.id)
    val hasTranspositionSupport = transpositionSupports.nonEmpty
    val replyCoverage = supportResults.count(MoveReviewExchangeAnalyzer.probeHasReplyCoverage)
    val futureCoverage = supportResults.count(_.futureSnapshot.exists(isPositiveFutureSnapshot))
    val quantifier =
      if supportResults.size >= 2 || (replyCoverage > 0 && futureCoverage > 0) then
        PlayerFacingClaimQuantifier.Universal
      else if replyCoverage > 0 || futureCoverage > 0 then
        PlayerFacingClaimQuantifier.BestResponse
      else if supportResults.nonEmpty then PlayerFacingClaimQuantifier.Existential
      else if hasTranspositionSupport then PlayerFacingClaimQuantifier.BestResponse
      else PlayerFacingClaimQuantifier.LineConditioned
    val modalityTier =
      if supportResults.exists(result => indicatesRemoval(themeId, result)) then
        PlayerFacingClaimModalityTier.Removes
      else if supportResults.exists(result => indicatesForcing(themeId, result)) then
        PlayerFacingClaimModalityTier.Forces
      else if supportResults.exists(result => indicatesAdvancement(themeId, result)) then
        PlayerFacingClaimModalityTier.Advances
      else if supportResults.nonEmpty then PlayerFacingClaimModalityTier.Supports
      else if hasTranspositionSupport then PlayerFacingClaimModalityTier.Supports
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
    val finalStabilityGrade =
      if hasTranspositionSupport && supportResults.isEmpty then PlayerFacingClaimStabilityGrade.Stable
      else stabilityGrade
    val provenanceClass =
      userFacingEligibility match
        case UserFacingPlanEligibility.ProbeBacked   => PlayerFacingClaimProvenanceClass.ProbeBacked
        case UserFacingPlanEligibility.TranspositionAligned => PlayerFacingClaimProvenanceClass.TranspositionAligned
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
        Option.when(pvCoupled && supports.isEmpty && !structuralEscalation && !hasTranspositionSupport)(PlayerFacingClaimTaintFlag.PvCoupled),
        Option.when(missingSignals.nonEmpty && supports.isEmpty)(PlayerFacingClaimTaintFlag.Deferred)
      ).flatten
    val certificateStatus =
      if refutations.nonEmpty then PlayerFacingCertificateStatus.Invalid
      else if supports.nonEmpty && quantifier != PlayerFacingClaimQuantifier.Existential then
        PlayerFacingCertificateStatus.Valid
      else if supports.nonEmpty then PlayerFacingCertificateStatus.WeaklyValid
      else if hasTranspositionSupport then PlayerFacingCertificateStatus.WeaklyValid
      else if missingSignals.nonEmpty || pvCoupled || structuralEscalation then
        PlayerFacingCertificateStatus.WeaklyValid
      else PlayerFacingCertificateStatus.Invalid
    ClaimCertification(
      certificateStatus = certificateStatus,
      quantifier = quantifier,
      modalityTier = modalityTier,
      attributionGrade = attributionGrade,
      stabilityGrade = finalStabilityGrade,
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
    themeId != PlanTheme.Unknown.id && allHypotheses.exists { other =>
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
    targetTheme != PlanTheme.Unknown.id && allHypotheses.exists { other =>
      other.planId != target.planId &&
        hypothesisThemeId(other) == targetTheme &&
        rankingScore(other) >= rankingScore(target) + 0.08 &&
        other.viability.score >= target.viability.score &&
        normalizeText(other.planName) != normalizeText(target.planName)
    }

  private def rankingScore(h: PlanHypothesis): Double =
    h.score + h.viability.score * 0.1

  private def cleanText(raw: String): Option[String] =
    Option(raw).map(_.trim).filter(_.nonEmpty)

  private def indicatesAdvancement(themeId: String, result: ProbeResult): Boolean =
    result.futureSnapshot.exists(snapshot =>
      snapshot.planPrereqsMet.nonEmpty ||
        snapshot.planBlockersRemoved.nonEmpty
    ) || Set(
      PlanTheme.OpeningPrinciples.id,
      PlanTheme.PawnBreakPreparation.id,
      PlanTheme.AdvantageTransformation.id,
      PlanTheme.FlankInfrastructure.id,
      PlanTheme.PieceRedeployment.id
    ).contains(themeId)

  private def indicatesForcing(themeId: String, result: ProbeResult): Boolean =
    (themeId == PlanTheme.FavorableExchange.id &&
      MoveReviewExchangeAnalyzer.probeHasReplyCoverage(result)) ||
      result.motifTags.exists(tag => ForcingMotifTags.contains(normalizeText(tag)))

  private val ForcingMotifTags =
    Set("forcing", "exchange", "trade", "simplification")

  private def indicatesRemoval(themeId: String, result: ProbeResult): Boolean =
    Set(
      PlanTheme.RestrictionProphylaxis.id,
      PlanTheme.WeaknessFixation.id
    ).contains(themeId) &&
      result.futureSnapshot.exists(snapshot =>
        snapshot.resolvedThreatKinds.nonEmpty ||
          snapshot.targetsDelta.strategicRemoved.nonEmpty
      )

  private def ontologyFamily(
      themeId: String,
      hypothesis: PlanHypothesis
  ): PlayerFacingClaimOntologyKind =
    themeId match
      case id if id == PlanTheme.RestrictionProphylaxis.id =>
        val low = normalizeText(hypothesis.planName)
        if containsAny(low, List("route", "entry", "denial")) then
          PlayerFacingClaimOntologyKind.RouteDenial
        else if containsAny(low, List("color", "complex", "bishop")) then
          PlayerFacingClaimOntologyKind.ColorComplexSqueeze
        else PlayerFacingClaimOntologyKind.LongTermRestraint
      case id if id == PlanTheme.FavorableExchange.id     => PlayerFacingClaimOntologyKind.Exchange
      case id if id == PlanTheme.PawnBreakPreparation.id  => PlayerFacingClaimOntologyKind.PlanAdvance
      case id if id == PlanTheme.PieceRedeployment.id     => PlayerFacingClaimOntologyKind.PlanAdvance
      case id if id == PlanTheme.SpaceClamp.id            => PlayerFacingClaimOntologyKind.CounterplayRestraint
      case id if id == PlanTheme.WeaknessFixation.id      => PlayerFacingClaimOntologyKind.ResourceRemoval
      case _                                            => PlayerFacingClaimOntologyKind.Unknown

  private def rankingScore(ep: EvaluatedPlan): Double =
    ep.hypothesis.score + (if isDefaultSubplan(ep.themeL1, ep.subplanId) then 0.0 else 0.03)

  private def mainAdmissionScore(ep: EvaluatedPlan): Double =
    rankingScore(ep) + (ep.userFacingEligibility match
      case UserFacingPlanEligibility.ProbeBacked          => 1.0
      case UserFacingPlanEligibility.TranspositionAligned => 0.5
      case _                                             => 0.0
    )

  private def nonEmptyToken(raw: String): Boolean =
    Option(raw).exists(_.trim.nonEmpty)

  private def isDefaultSubplan(themeId: String, subplanId: Option[String]): Boolean =
    val theme = PlanTheme.fromId(themeId).getOrElse(PlanTheme.Unknown)
    val default = ThemeResolver.defaultSubplanForTheme(theme).map(_.id)
    default.exists(id => subplanId.contains(id))

  private def statusCode(status: PlanEvidenceStatus): String =
    status match
      case PlanEvidenceStatus.PlayableEvidenceBacked => "playable_evidence_backed"
      case PlanEvidenceStatus.PlayableTranspositionAligned => "playable_transposition_aligned"
      case PlanEvidenceStatus.PlayableStructuralOnly => "playable_structural_only"
      case PlanEvidenceStatus.PlayablePvCoupled      => "playable_pv_coupled"
      case PlanEvidenceStatus.Refuted                => "refuted"
      case PlanEvidenceStatus.Deferred               => "deferred"

  private def eligibilityCode(eligibility: UserFacingPlanEligibility): String =
    eligibility match
      case UserFacingPlanEligibility.ProbeBacked   => "probe_backed"
      case UserFacingPlanEligibility.TranspositionAligned => "transposition_aligned"
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
      case PlayerFacingClaimProvenanceClass.TranspositionAligned => "transposition_aligned"
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

  private def ontologyCode(family: PlayerFacingClaimOntologyKind): String =
    family match
      case PlayerFacingClaimOntologyKind.Access              => "access"
      case PlayerFacingClaimOntologyKind.Pressure            => "pressure"
      case PlayerFacingClaimOntologyKind.Exchange            => "exchange"
      case PlayerFacingClaimOntologyKind.CounterplayRestraint => "counterplay_restraint"
      case PlayerFacingClaimOntologyKind.ResourceRemoval     => "resource_removal"
      case PlayerFacingClaimOntologyKind.PlanAdvance         => "plan_advance"
      case PlayerFacingClaimOntologyKind.RouteDenial         => "route_denial"
      case PlayerFacingClaimOntologyKind.ColorComplexSqueeze => "color_complex_squeeze"
      case PlayerFacingClaimOntologyKind.LongTermRestraint   => "long_term_restraint"
      case PlayerFacingClaimOntologyKind.PieceImprovement    => "piece_improvement"
      case PlayerFacingClaimOntologyKind.KingSafety          => "king_safety"
      case PlayerFacingClaimOntologyKind.TechnicalConversion => "technical_conversion"
      case PlayerFacingClaimOntologyKind.Unknown             => "unknown"

  private def diagnosticReasonCodes(
      ep: EvaluatedPlan,
      linkedRequests: List[ProbeRequest],
      softReasonCodes: List[String]
  ): List[String] =
    val base =
      ep.userFacingEligibility match
        case UserFacingPlanEligibility.ProbeBacked   => List("validated_support_probe")
        case UserFacingPlanEligibility.TranspositionAligned => List("transposition_aligned")
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
    val softCodes =
      softReasonCodes.map(normalizeReasonCode)
    (base ++ certificateCodes ++ requestCodes ++ alternativeCodes ++ ep.missingSignals ++ softCodes).distinct

  private def normalizeReasonCode(raw: String): String =
    Option(raw)
      .map(_.trim.toLowerCase.replaceAll("[^a-z0-9]+", "_"))
      .filter(_.nonEmpty)
      .getOrElse("validation_gap")

  private def markMainAdmitted(
      h: PlanHypothesis,
      eligibility: UserFacingPlanEligibility
  ): PlanHypothesis =
    eligibility match
      case UserFacingPlanEligibility.ProbeBacked =>
        h.copy(evidenceSources = (h.evidenceSources :+ "probe_backed:validated_support").distinct)
      case UserFacingPlanEligibility.TranspositionAligned =>
        h.copy(evidenceSources = (h.evidenceSources :+ "transposition_aligned:target_profile").distinct)
      case _ => h

  private def isSupportive(req: ProbeRequest, pr: ProbeResult, isWhiteToMove: Boolean): Boolean =
    val purpose = req.purpose.orElse(pr.purpose).getOrElse("")
    if ProbePurposeClassifier.isRefutationPurpose(purpose) then
      false
    else if !knownSupportPurpose(purpose) then
      false
    else
      val loss = moverLoss(pr, isWhiteToMove)
      val maxLoss = req.maxCpLoss.getOrElse(defaultMaxCpLoss(purpose))
      loss <= maxLoss && !isMateAgainstMover(pr, isWhiteToMove)

  private def isRefuted(req: ProbeRequest, pr: ProbeResult, isWhiteToMove: Boolean): Boolean =
    val purpose = req.purpose.orElse(pr.purpose).getOrElse("")
    if !knownProbePurpose(purpose) then false
    else
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
    ProbePurposeClassifier.defaultMaxCpLossForPurpose(purpose).getOrElse(0)

  private def knownSupportPurpose(purpose: String): Boolean =
    ProbePurposeClassifier.isKnownSupportPurpose(purpose)

  private def knownProbePurpose(purpose: String): Boolean =
    ProbePurposeClassifier.isKnownProbePurpose(purpose)

  private def isPositiveFutureSnapshot(snapshot: lila.commentary.model.FutureSnapshot): Boolean =
    snapshot.planPrereqsMet.nonEmpty ||
      snapshot.planBlockersRemoved.nonEmpty ||
      snapshot.targetsDelta.strategicAdded.nonEmpty ||
      snapshot.resolvedThreatKinds.nonEmpty

  private def hasStructuralEvidence(h: PlanHypothesis): Boolean =
    ThemeResolver.hasStructuralStateEvidence(h.evidenceSources)

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
