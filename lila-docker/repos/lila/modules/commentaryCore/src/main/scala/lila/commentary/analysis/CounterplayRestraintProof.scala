package lila.commentary.analysis

import lila.commentary.analysis.PlanTaxonomy.PlanKind
import lila.commentary.model.{ FactScope, FutureSnapshot, ProbeResult }
import lila.commentary.model.strategic.PreventedPlan

private[commentary] object CounterplayRestraintProof:

  final case class RestrictionEvidence(
      namedAxis: String,
      claimScope: String,
      deniedResourceClass: Option[String],
      counterplayScoreDrop: Int,
      breakNeutralizationStrength: Option[Int],
      defensiveSufficiency: Option[Int],
      restrictionDeltaMeasured: Boolean,
      bestDefenseStable: Boolean,
      futureSnapshotPersistence: Boolean
  )

  final case class RoutePersistence(
      bestDefenseStable: Boolean,
      futureSnapshotPersistent: Boolean,
      axisStillSuppressed: Boolean,
      directBestDefensePresent: Boolean,
      sameDefendedBranch: Boolean
  )

  final case class MoveOrderFragility(
      fragile: Boolean,
      reasons: List[String]
  )

  final case class Contract(
      strategyHypothesis: String,
      claimScope: String,
      squeezeArchetype: String,
      restrictionEvidence: RestrictionEvidence,
      defenderResources: List[String],
      freeingBreaksRemaining: List[String],
      tacticalReleasesRemaining: List[String],
      bestDefenseFound: Option[String],
      bestDefenseBranchKey: Option[String],
      routePersistence: RoutePersistence,
      failsIf: List[String],
      moveOrderFragility: MoveOrderFragility,
      counterplayReinflationRisk: String,
      confidence: Double,
      evidenceSources: List[String]
  ):
    def certified: Boolean = failsIf.isEmpty

  private final case class AxisSignal(
      label: String,
      claimScope: String,
      deniedResourceClass: Option[String],
      counterplayScoreDrop: Int,
      breakNeutralizationStrength: Option[Int],
      defensiveSufficiency: Option[Int]
  )

  private final case class ProbeEvidenceSlices(
      supportResults: List[ProbeResult],
      validationResults: List[ProbeResult],
      directReplyResults: List[ProbeResult]
  )

  private final case class BestDefenseBranch(
      found: Option[String],
      key: Option[String]
  )

  private final case class AxisEvidence(
      relevantPreventedPlans: List[PreventedPlan],
      primaryAxis: Option[AxisSignal],
      alternativeAxes: List[String]
  )

  private final case class CounterplayContinuityFacts(
      directBestDefensePresent: Boolean,
      defenderResources: List[String],
      bestReplyStable: Boolean,
      futureSnapshotPersistence: Boolean,
      sameDefendedBranch: Boolean,
      routePersistence: RoutePersistence
  )

  private val ApplicableSubplans =
    Set(PlanKind.ProphylaxisRestraint.id, PlanKind.BreakPrevention.id, PlanKind.KeySquareDenial.id)
  private val DirectReplyPurposes =
    Set("defense_reply_multipv", "reply_multipv")
  private val ValidationPurposes =
    Set(
      ThemePlanProbePurpose.RouteDenialValidation,
      ThemePlanProbePurpose.LongTermRestraintValidation
    )
  private val ClearlyBetterAdvantageCp = 150
  private val LateMiddlegamePlyFloor = 20
  private val CounterplayCompressionFloor = 100
  private val BreakNeutralizationFloor = 70
  private val RestrictedResourceCap = 2
  private val MaxQueensForPositiveSlice = 1
  private val HighReinflationRisk = "high"
  private val BoundedAxisOnly = "bounded_axis_only"
  private val SurfaceInflationTokens =
    List(
      "no counterplay",
      "completely tied",
      "completely bound",
      "completely shut down",
      "wins by force",
      "winning route",
      "winning plan",
      "totally squeezed"
    )
  private val GenericNamedResourceLabels =
    Set(
      "counterplay",
      "deny counterplay",
      "deny_counterplay",
      "counterplay resource",
      "resource",
      "threat",
      "plan",
      "their plan",
      "the plan"
    )

  def evaluate(
      plan: PlanEvidenceEvaluator.EvaluatedPlan,
      probeResultsById: Map[String, ProbeResult],
      preventedPlans: List[PreventedPlan],
      evalCp: Int,
      isWhiteToMove: Boolean,
      phase: String,
      ply: Int,
      fen: String
  ): Option[Contract] =
    Option.when(isApplicablePlan(plan)) {
      val probeEvidence = probeEvidenceSlices(plan, probeResultsById)
      val validationResults = probeEvidence.validationResults
      val directReplyResults = probeEvidence.directReplyResults
      val defenseBranch =
        bestDefenseBranch(directReplyResults)
      val sameBranchValidationResults =
        sameBranchValidationResultsFor(validationResults, defenseBranch.key)
      val axisEvidence =
        buildAxisEvidence(preventedPlans)
      val primaryAxis = axisEvidence.primaryAxis
      val alternativeAxes = axisEvidence.alternativeAxes
      val tacticalReleasesRemaining =
        directReplyResults.flatMap(tacticalReleaseSignals).distinct
      val restrictionDeltaMeasured =
        primaryAxis.exists(axisBurdenMeasured)
      val continuityFacts =
        counterplayContinuityFacts(
          defenseBranch = defenseBranch,
          directReplyResults = directReplyResults,
          sameBranchValidationResults = sameBranchValidationResults,
          tacticalReleasesRemaining = tacticalReleasesRemaining,
          restrictionDeltaMeasured = restrictionDeltaMeasured
        )
      val lateMiddlegameSlice =
        normalize(phase) == "middlegame" &&
          ply >= LateMiddlegamePlyFloor &&
          queenCount(fen) <= MaxQueensForPositiveSlice
      val clearlyBetter =
        playerAdvantage(evalCp, isWhiteToMove) >= ClearlyBetterAdvantageCp
      val distinctiveEnough =
        plan.claimCertification.attributionGrade == PlayerFacingClaimAttributionGrade.Distinctive &&
          !plan.claimCertification.alternativeDominance
      val ontologyAllowed =
        Set(
          PlayerFacingClaimOntologyKind.RouteDenial,
          PlayerFacingClaimOntologyKind.LongTermRestraint
        ).contains(plan.claimCertification.ontologyFamily)
      val moveOrderFragility =
        buildMoveOrderFragility(
          plan = plan,
          directReplyResults = directReplyResults,
          bestReplyStable = continuityFacts.bestReplyStable,
          futureSnapshotPersistence = continuityFacts.futureSnapshotPersistence
        )
      val claimScope =
        primaryAxis.map(_.claimScope).getOrElse("break_axis")
      val requiresNamedResource =
        plan.subplanId.contains(PlanKind.ProphylaxisRestraint.id)
      val squeezeArchetype =
        if plan.subplanId.contains(PlanKind.KeySquareDenial.id) || claimScope == "entry_axis" then
          "route_denial"
        else "prophylactic_clamp"
      val restrictionEvidence =
        buildRestrictionEvidence(
          primaryAxis = primaryAxis,
          claimScope = claimScope,
          restrictionDeltaMeasured = restrictionDeltaMeasured,
          continuityFacts = continuityFacts
        )
      val counterplayReinflationRisk =
        buildCounterplayReinflationRisk(
          plan = plan,
          primaryAxis = primaryAxis,
          alternativeAxes = alternativeAxes,
          defenderResources = continuityFacts.defenderResources,
          distinctiveEnough = distinctiveEnough,
          ontologyAllowed = ontologyAllowed
        )
      val failsIf =
        buildFailsIf(
          validationResults = validationResults,
          requiresNamedResource = requiresNamedResource,
          claimScope = claimScope,
          lateMiddlegameSlice = lateMiddlegameSlice,
          clearlyBetter = clearlyBetter,
          distinctiveEnough = distinctiveEnough,
          ontologyAllowed = ontologyAllowed,
          restrictionDeltaMeasured = restrictionDeltaMeasured,
          primaryAxis = primaryAxis,
          alternativeAxes = alternativeAxes,
          tacticalReleasesRemaining = tacticalReleasesRemaining,
          continuityFacts = continuityFacts,
          moveOrderFragility = moveOrderFragility,
          counterplayReinflationRisk = counterplayReinflationRisk
        )
      Contract(
        strategyHypothesis = displayHypothesis(plan),
        claimScope = claimScope,
        squeezeArchetype = squeezeArchetype,
        restrictionEvidence = restrictionEvidence,
        defenderResources = continuityFacts.defenderResources,
        freeingBreaksRemaining = alternativeAxes,
        tacticalReleasesRemaining = tacticalReleasesRemaining,
        bestDefenseFound = defenseBranch.found,
        bestDefenseBranchKey = defenseBranch.key,
        routePersistence = continuityFacts.routePersistence,
        failsIf = failsIf,
        moveOrderFragility = moveOrderFragility,
        counterplayReinflationRisk = counterplayReinflationRisk,
        confidence =
          confidenceScore(
            lateMiddlegameSlice = lateMiddlegameSlice,
            clearlyBetter = clearlyBetter,
            restrictionDeltaMeasured = restrictionDeltaMeasured,
            directBestDefensePresent = continuityFacts.directBestDefensePresent,
            bestReplyStable = continuityFacts.bestReplyStable,
            futureSnapshotPersistence = continuityFacts.futureSnapshotPersistence,
            sameDefendedBranch = continuityFacts.sameDefendedBranch,
            defenderResourceCount = continuityFacts.defenderResources.size,
            alternativeAxisCount = alternativeAxes.size,
            tacticalReleaseCount = tacticalReleasesRemaining.size,
            moveOrderFragility = moveOrderFragility,
            distinctiveEnough = distinctiveEnough,
            ontologyAllowed = ontologyAllowed,
            counterplayReinflationRisk = counterplayReinflationRisk
          ),
        evidenceSources =
          (plan.hypothesis.evidenceSources ++
            directReplyResults.flatMap(_.purpose.flatMap(clean)) ++
            sameBranchValidationResults.flatMap(_.purpose.flatMap(clean)) ++
            axisEvidence.relevantPreventedPlans.flatMap(preventedEvidenceSignals)).distinct
      )
    }

  def playerFacingEvidenceTier(
      baseTier: String,
      contract: Option[Contract]
  ): String =
    contract match
      case Some(cert) if normalize(baseTier) == "evidence_backed" && !cert.certified =>
        "deferred"
      case _ => baseTier

  private def isApplicablePlan(
      plan: PlanEvidenceEvaluator.EvaluatedPlan
  ): Boolean =
    normalize(plan.themeL1) == PlanTaxonomy.PlanTheme.RestrictionProphylaxis.id &&
      plan.subplanId.exists(id => ApplicableSubplans.contains(normalize(id)))

  private def probeEvidenceSlices(
      plan: PlanEvidenceEvaluator.EvaluatedPlan,
      probeResultsById: Map[String, ProbeResult]
  ): ProbeEvidenceSlices =
    val supportResults =
      plan.supportProbeIds.flatMap(probeResultsById.get).distinctBy(_.id)
    ProbeEvidenceSlices(
      supportResults = supportResults,
      validationResults =
        supportResults.filter(result =>
          result.purpose.exists(purpose =>
            ValidationPurposes.contains(normalize(purpose))
          )
        ),
      directReplyResults =
        supportResults.filter(result =>
          result.purpose.exists(purpose =>
            DirectReplyPurposes.contains(normalize(purpose))
          )
        )
    )

  private def bestDefenseBranch(
      directReplyResults: List[ProbeResult]
  ): BestDefenseBranch =
    BestDefenseBranch(
      found =
        directReplyResults.iterator
          .flatMap(result => result.bestReplyPv.headOption.flatMap(clean))
          .toList
          .headOption,
      key =
        directReplyResults.iterator
          .flatMap(MoveReviewExchangeAnalyzer.probeFullReplyLineKey)
          .toList
          .headOption
    )

  private def sameBranchValidationResultsFor(
      validationResults: List[ProbeResult],
      bestDefenseBranchKey: Option[String]
  ): List[ProbeResult] =
    bestDefenseBranchKey match
      case Some(branch) =>
        validationResults.filter(result => matchesDefendedBranch(result, branch))
      case None => Nil

  private def buildAxisEvidence(
      preventedPlans: List[PreventedPlan]
  ): AxisEvidence =
    val relevantPreventedPlans =
      preventedPlans.filter(_.sourceScope == FactScope.Now)
    val axisSignals = relevantPreventedPlans.flatMap(axisSignal)
    val primaryAxis =
      axisSignals.sortBy(signal =>
        (-signal.counterplayScoreDrop, -signal.breakNeutralizationStrength.getOrElse(0))
      ).headOption
    val alternativeAxes =
      primaryAxis match
        case Some(primary) =>
          axisSignals.collect {
            case axis if axis.label != primary.label => axis.label
          }.distinct
        case None => Nil
    AxisEvidence(
      relevantPreventedPlans = relevantPreventedPlans,
      primaryAxis = primaryAxis,
      alternativeAxes = alternativeAxes
    )

  private def counterplayContinuityFacts(
      defenseBranch: BestDefenseBranch,
      directReplyResults: List[ProbeResult],
      sameBranchValidationResults: List[ProbeResult],
      tacticalReleasesRemaining: List[String],
      restrictionDeltaMeasured: Boolean
  ): CounterplayContinuityFacts =
    val defenderResources =
      distinctDefenderResources(directReplyResults)
    val directBestDefensePresent =
      directReplyResults.nonEmpty && defenseBranch.found.nonEmpty
    val bestReplyStable =
      directBestDefensePresent &&
        defenderResources.nonEmpty &&
        directReplyResults.forall(hasReplyCoverage) &&
        directReplyResults.forall(result =>
          result.l1Delta.flatMap(_.collapseReason).forall(reason => clean(reason).isEmpty)
        )
    val futureSnapshotPersistence =
      sameBranchValidationResults.exists(result =>
        result.futureSnapshot.exists(snapshot =>
          isPositiveSuppressionSnapshot(snapshot) && tacticalReleaseSignals(result).isEmpty
        )
      )
    val sameDefendedBranch =
      directBestDefensePresent && sameBranchValidationResults.nonEmpty
    val routePersistence =
      RoutePersistence(
        bestDefenseStable = bestReplyStable,
        futureSnapshotPersistent = futureSnapshotPersistence,
        axisStillSuppressed =
          restrictionDeltaMeasured &&
            directBestDefensePresent &&
            defenderResources.nonEmpty &&
            defenderResources.size <= RestrictedResourceCap &&
            tacticalReleasesRemaining.isEmpty &&
            futureSnapshotPersistence &&
            sameDefendedBranch &&
            bestReplyStable,
        directBestDefensePresent = directBestDefensePresent,
        sameDefendedBranch = sameDefendedBranch
      )
    CounterplayContinuityFacts(
      directBestDefensePresent = directBestDefensePresent,
      defenderResources = defenderResources,
      bestReplyStable = bestReplyStable,
      futureSnapshotPersistence = futureSnapshotPersistence,
      sameDefendedBranch = sameDefendedBranch,
      routePersistence = routePersistence
    )

  private def buildMoveOrderFragility(
      plan: PlanEvidenceEvaluator.EvaluatedPlan,
      directReplyResults: List[ProbeResult],
      bestReplyStable: Boolean,
      futureSnapshotPersistence: Boolean
  ): MoveOrderFragility =
    val fragilityReasons =
      List(
        Option.when(plan.status == PlanEvidenceEvaluator.PlanEvidenceStatus.PlayablePvCoupled)(
          "pv_coupled_only"
        ),
        Option.when(plan.pvCoupled && plan.missingSignals.nonEmpty)(
          "missing_signals_under_pv_coupling"
        ),
        Option.when(
          directReplyResults.exists(result =>
            result.l1Delta.flatMap(_.collapseReason).exists(reason => clean(reason).nonEmpty)
          )
        )("collapse_under_best_defense"),
        Option.when(
          directReplyResults.nonEmpty &&
            directReplyResults.exists(hasReplyCoverage) &&
            !bestReplyStable &&
            !futureSnapshotPersistence
        )("reply_order_not_stable")
      ).flatten.distinct
    MoveOrderFragility(
      fragile = fragilityReasons.nonEmpty,
      reasons = fragilityReasons
    )

  private def buildRestrictionEvidence(
      primaryAxis: Option[AxisSignal],
      claimScope: String,
      restrictionDeltaMeasured: Boolean,
      continuityFacts: CounterplayContinuityFacts
  ): RestrictionEvidence =
    RestrictionEvidence(
      namedAxis = primaryAxis.map(_.label).getOrElse("unnamed_axis"),
      claimScope = claimScope,
      deniedResourceClass = primaryAxis.flatMap(_.deniedResourceClass),
      counterplayScoreDrop = primaryAxis.map(_.counterplayScoreDrop).getOrElse(0),
      breakNeutralizationStrength = primaryAxis.flatMap(_.breakNeutralizationStrength),
      defensiveSufficiency = primaryAxis.flatMap(_.defensiveSufficiency),
      restrictionDeltaMeasured = restrictionDeltaMeasured,
      bestDefenseStable = continuityFacts.bestReplyStable,
      futureSnapshotPersistence = continuityFacts.futureSnapshotPersistence
    )

  private def buildCounterplayReinflationRisk(
      plan: PlanEvidenceEvaluator.EvaluatedPlan,
      primaryAxis: Option[AxisSignal],
      alternativeAxes: List[String],
      defenderResources: List[String],
      distinctiveEnough: Boolean,
      ontologyAllowed: Boolean
  ): String =
    if primaryAxis.isEmpty ||
      alternativeAxes.nonEmpty ||
      !distinctiveEnough ||
      !ontologyAllowed ||
      containsInflationShell(plan.hypothesis) ||
      defenderResources.size > RestrictedResourceCap
    then HighReinflationRisk
    else BoundedAxisOnly

  private def buildFailsIf(
      validationResults: List[ProbeResult],
      requiresNamedResource: Boolean,
      claimScope: String,
      lateMiddlegameSlice: Boolean,
      clearlyBetter: Boolean,
      distinctiveEnough: Boolean,
      ontologyAllowed: Boolean,
      restrictionDeltaMeasured: Boolean,
      primaryAxis: Option[AxisSignal],
      alternativeAxes: List[String],
      tacticalReleasesRemaining: List[String],
      continuityFacts: CounterplayContinuityFacts,
      moveOrderFragility: MoveOrderFragility,
      counterplayReinflationRisk: String
  ): List[String] =
    List(
      Option.when(validationResults.isEmpty)("pv_restatement_only"),
      Option.when(requiresNamedResource && claimScope != "named_resource")("named_resource_missing"),
      Option.when(!continuityFacts.directBestDefensePresent)("direct_best_defense_missing"),
      Option.when(!lateMiddlegameSlice || !clearlyBetter || !distinctiveEnough || !ontologyAllowed)(
        "local_to_global_overreach"
      ),
      Option.when(!restrictionDeltaMeasured || primaryAxis.isEmpty)(
        "waiting_move_disguised_as_plan"
      ),
      Option.when(
        primaryAxis.isEmpty ||
          alternativeAxes.nonEmpty ||
          (continuityFacts.directBestDefensePresent &&
            (continuityFacts.defenderResources.isEmpty ||
              continuityFacts.defenderResources.size > RestrictedResourceCap))
      )("hidden_freeing_break"),
      Option.when(tacticalReleasesRemaining.nonEmpty)("hidden_tactical_release"),
      Option.when(
        continuityFacts.directBestDefensePresent &&
          validationResults.nonEmpty &&
          !continuityFacts.sameDefendedBranch
      )("stitched_defended_branch"),
      Option.when(!continuityFacts.bestReplyStable)("cooperative_defense"),
      Option.when(!continuityFacts.routePersistence.axisStillSuppressed)("route_persistence_missing"),
      Option.when(moveOrderFragility.fragile)("move_order_fragility"),
      Option.when(counterplayReinflationRisk == HighReinflationRisk)("surface_reinflation")
    ).flatten.distinct

  private def axisBurdenMeasured(
      axis: AxisSignal
  ): Boolean =
    axis.counterplayScoreDrop >= CounterplayCompressionFloor ||
      axis.breakNeutralizationStrength.exists(_ >= BreakNeutralizationFloor) ||
      axis.defensiveSufficiency.exists(_ >= BreakNeutralizationFloor)

  private def axisSignal(
      plan: PreventedPlan
  ): Option[AxisSignal] =
    plan.breakNeutralized.flatMap(clean).map { axis =>
      AxisSignal(
        label = axis,
        claimScope = "break_axis",
        deniedResourceClass = plan.deniedResourceClass,
        counterplayScoreDrop = plan.counterplayScoreDrop,
        breakNeutralizationStrength = plan.breakNeutralizationStrength,
        defensiveSufficiency = plan.defensiveSufficiency
      )
    }.orElse {
      Option.when(
        plan.deniedResourceClass.exists(resource =>
          normalize(resource) == "entry_square" || normalize(resource) == "entry"
        ) && plan.deniedSquares.nonEmpty
      )(
        AxisSignal(
          label = plan.deniedSquares.map(_.key).distinct.sorted.mkString(","),
          claimScope = "entry_axis",
          deniedResourceClass = plan.deniedResourceClass,
          counterplayScoreDrop = plan.counterplayScoreDrop,
          breakNeutralizationStrength = plan.breakNeutralizationStrength,
          defensiveSufficiency = plan.defensiveSufficiency
        )
      )
    }.orElse {
      namedResourceLabel(plan).map { label =>
        AxisSignal(
          label = label,
          claimScope = "named_resource",
          deniedResourceClass = plan.deniedResourceClass,
          counterplayScoreDrop = plan.counterplayScoreDrop,
          breakNeutralizationStrength = plan.breakNeutralizationStrength,
          defensiveSufficiency = plan.defensiveSufficiency
        )
      }
    }

  private def namedResourceLabel(
      plan: PreventedPlan
  ): Option[String] =
    clean(plan.planId)
      .map(_.replace('_', ' ').replace('-', ' ').replaceAll("\\s+", " ").trim)
      .filter(_.nonEmpty)
      .filterNot(label => GenericNamedResourceLabels.contains(normalize(label)))
      .filter(_ =>
        plan.breakNeutralized.isEmpty &&
          plan.deniedSquares.isEmpty &&
          (
            plan.preventedThreatType.exists(_.trim.nonEmpty) ||
              plan.deniedResourceClass.exists(_.trim.nonEmpty)
          )
      )

  private def confidenceScore(
      lateMiddlegameSlice: Boolean,
      clearlyBetter: Boolean,
      restrictionDeltaMeasured: Boolean,
      directBestDefensePresent: Boolean,
      bestReplyStable: Boolean,
      futureSnapshotPersistence: Boolean,
      sameDefendedBranch: Boolean,
      defenderResourceCount: Int,
      alternativeAxisCount: Int,
      tacticalReleaseCount: Int,
      moveOrderFragility: MoveOrderFragility,
      distinctiveEnough: Boolean,
      ontologyAllowed: Boolean,
      counterplayReinflationRisk: String
  ): Double =
    val base = 0.44
    val phaseBonus = if lateMiddlegameSlice then 0.07 else 0.0
    val evalBonus = if clearlyBetter then 0.08 else 0.0
    val restrictionBonus = if restrictionDeltaMeasured then 0.12 else 0.0
    val directDefenseBonus = if directBestDefensePresent then 0.04 else 0.0
    val replyBonus = if bestReplyStable then 0.07 else 0.0
    val futureBonus = if futureSnapshotPersistence then 0.06 else 0.0
    val branchBundleBonus = if sameDefendedBranch then 0.04 else 0.0
    val resourceBonus =
      if defenderResourceCount > 0 && defenderResourceCount <= RestrictedResourceCap then 0.05
      else 0.0
    val distinctivenessBonus =
      if distinctiveEnough && ontologyAllowed && counterplayReinflationRisk == BoundedAxisOnly then 0.05
      else 0.0
    val alternativeAxisPenalty = math.min(0.16, alternativeAxisCount * 0.08)
    val tacticalReleasePenalty = math.min(0.18, tacticalReleaseCount * 0.08)
    val fragilityPenalty = if moveOrderFragility.fragile then 0.12 else 0.0
    val reinflationPenalty = if counterplayReinflationRisk == HighReinflationRisk then 0.12 else 0.0
    (base + phaseBonus + evalBonus + restrictionBonus + directDefenseBonus + replyBonus + futureBonus +
      branchBundleBonus + resourceBonus + distinctivenessBonus -
      alternativeAxisPenalty - tacticalReleasePenalty - fragilityPenalty - reinflationPenalty)
      .max(0.0)
      .min(0.96)

  private def tacticalReleaseSignals(
      result: ProbeResult
  ): List[String] =
    val collapseSignals =
      result.l1Delta.flatMap(_.collapseReason).flatMap(clean).toList.map(reason => s"collapse:$reason")
    val snapshotSignals =
      result.futureSnapshot.toList.flatMap { snapshot =>
        snapshot.newThreatKinds.flatMap(clean).map(kind => s"new_threat:$kind") ++
          snapshot.targetsDelta.tacticalAdded.flatMap(clean).map(target => s"tactical_target:$target")
      }
    val motifSignals =
      result.motifTags.flatMap(clean).filter(looksLikeTacticalRelease).map(motif => s"motif:$motif")
    (collapseSignals ++ snapshotSignals ++ motifSignals).distinct

  private def isPositiveSuppressionSnapshot(
      snapshot: FutureSnapshot
  ): Boolean =
    snapshot.planBlockersRemoved.exists(mentionsCounterplayAxis) ||
      snapshot.planPrereqsMet.exists(mentionsCounterplayAxis) ||
      snapshot.resolvedThreatKinds.exists(mentionsCounterplayAxis) ||
      snapshot.planBlockersRemoved.nonEmpty ||
      snapshot.planPrereqsMet.nonEmpty

  private def distinctDefenderResources(
      results: List[ProbeResult]
  ): List[String] =
    results
      .flatMap { result =>
        val replyHeads =
          result.replyPvs.toList
            .flatten
            .flatMap(_.headOption.flatMap(clean))
        val bestReply =
          result.bestReplyPv.headOption.flatMap(clean).toList
        (replyHeads ++ bestReply).distinct
      }
      .distinct

  private def matchesDefendedBranch(
      result: ProbeResult,
      expectedBranchKey: String
  ): Boolean =
    MoveReviewExchangeAnalyzer.probeFullReplyLineMatches(result, expectedBranchKey)

  private def displayHypothesis(
      plan: PlanEvidenceEvaluator.EvaluatedPlan
  ): String =
    clean(plan.hypothesis.planName)
      .orElse(clean(plan.hypothesis.planId))
      .getOrElse("counterplay suppression plan")

  private def playerAdvantage(
      evalCp: Int,
      isWhiteToMove: Boolean
  ): Int =
    if isWhiteToMove then evalCp else -evalCp

  private def hasReplyCoverage(
      result: ProbeResult
  ): Boolean =
    result.bestReplyPv.nonEmpty ||
      result.replyPvs.exists(_.exists(_.nonEmpty))

  private def preventedEvidenceSignals(
      plan: PreventedPlan
  ): List[String] =
    List(
      Option.when(plan.counterplayScoreDrop > 0)(
        s"counterplay_drop:${plan.counterplayScoreDrop}"
      ),
      plan.breakNeutralized.flatMap(signal => clean(signal).map(value => s"neutralized_break:$value")),
      plan.deniedResourceClass.flatMap(resourceClass =>
        clean(resourceClass).map(value => s"denied_resource:$value")
      )
    ).flatten

  private def queenCount(
      fen: String
  ): Int =
    Option(fen)
      .map(_.takeWhile(_ != ' '))
      .getOrElse("")
      .count(ch => ch == 'q' || ch == 'Q')

  private def containsInflationShell(
      hypothesis: lila.commentary.model.authoring.PlanHypothesis
  ): Boolean =
    (List(hypothesis.planName) ++
      hypothesis.preconditions ++
      hypothesis.executionSteps ++
      hypothesis.failureModes ++
      hypothesis.refutation.toList)
      .flatMap(clean)
      .exists(text =>
        SurfaceInflationTokens.exists(token => normalize(text).contains(token))
      )

  private def looksLikeTacticalRelease(
      raw: String
  ): Boolean =
    val low = normalize(raw)
    List("sac", "perpet", "forcing", "tactic", "king route", "counter-sac").exists(low.contains)

  private def mentionsCounterplayAxis(
      raw: String
  ): Boolean =
    val low = normalize(raw)
    List("counterplay", "break", "entry", "activity", "freeing", "route").exists(low.contains)

  private def clean(
      raw: String
  ): Option[String] =
    Option(raw).map(_.trim).filter(_.nonEmpty)

  private def normalize(
      raw: String
  ): String =
    clean(raw).map(_.toLowerCase).getOrElse("")
