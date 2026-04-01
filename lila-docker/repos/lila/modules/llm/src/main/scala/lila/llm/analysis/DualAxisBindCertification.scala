package lila.llm.analysis

import lila.llm.analysis.ThemeTaxonomy.SubplanId
import lila.llm.model.{ FactScope, FutureSnapshot, ProbeResult }
import lila.llm.model.strategic.PreventedPlan

private[llm] object DualAxisBindCertification:

  final case class AxisDescriptor(
      kind: String,
      label: String,
      deniedResourceClass: Option[String],
      counterplayScoreDrop: Int,
      breakNeutralizationStrength: Option[Int],
      defensiveSufficiency: Option[Int]
  )

  final case class AxisIndependence(
      primaryAxis: Option[String],
      corroboratingAxis: Option[String],
      proven: Boolean,
      reasons: List[String]
  )

  final case class RestrictionEvidence(
      primaryAxis: Option[AxisDescriptor],
      corroboratingAxes: List[AxisDescriptor],
      primaryAxisMeasured: Boolean,
      corroboratingAxisMeasured: Boolean,
      restrictionDeltaMeasured: Boolean,
      bestDefenseStable: Boolean,
      futureSnapshotPersistence: Boolean
  )

  final case class RouteContinuity(
      directBestDefensePresent: Boolean,
      bestDefenseStable: Boolean,
      futureSnapshotPersistent: Boolean,
      convertReplyAligned: Boolean,
      boundedContinuationVisible: Boolean,
      sameDefendedBranch: Boolean
  )

  final case class MoveOrderFragility(
      fragile: Boolean,
      reasons: List[String]
  )

  final case class Contract(
      strategyHypothesis: String,
      claimScope: String,
      primaryAxis: Option[AxisDescriptor],
      corroboratingAxes: List[AxisDescriptor],
      axisIndependence: AxisIndependence,
      bindArchetype: String,
      restrictionEvidence: RestrictionEvidence,
      defenderResources: List[String],
      freeingResourcesRemaining: List[String],
      tacticalReleasesRemaining: List[String],
      bestDefenseFound: Option[String],
      bestDefenseBranchKey: Option[String],
      persistenceAfterBestDefense: Boolean,
      routeContinuity: RouteContinuity,
      fortressRisk: Boolean,
      moveOrderFragility: MoveOrderFragility,
      counterplayReinflationRisk: String,
      claimCertification: PlanEvidenceEvaluator.ClaimCertification,
      failsIf: List[String],
      confidence: Double,
      evidenceSources: List[String]
  ):
    def certified: Boolean = failsIf.isEmpty

  private final case class AxisSignal(
      kind: String,
      label: String,
      deniedResourceClass: Option[String],
      counterplayScoreDrop: Int,
      breakNeutralizationStrength: Option[Int],
      defensiveSufficiency: Option[Int],
      squares: List[String]
  )

  private val ApplicableSubplans =
    Set(SubplanId.BreakPrevention.id, SubplanId.KeySquareDenial.id)
  private val DirectReplyPurposes =
    Set("defense_reply_multipv", "reply_multipv")
  private val ValidationPurposes =
    Set(
      ThemePlanProbePurpose.RouteDenialValidation,
      ThemePlanProbePurpose.LongTermRestraintValidation
    )
  private val ContinuityPurposes =
    Set(
      ThemePlanProbePurpose.RouteDenialValidation,
      ThemePlanProbePurpose.LongTermRestraintValidation,
      "convert_reply_multipv"
    )
  private val ClearlyBetterAdvantageCp = 150
  private val LateMiddlegamePlyFloor = 20
  private val CounterplayCompressionFloor = 100
  private val BreakNeutralizationFloor = 70
  private val RestrictedResourceCap = 2
  private val MaxQueensForPositiveSlice = 1
  private val HighReinflationRisk = "high"
  private val BoundedDualAxisOnly = "bounded_dual_axis_only"
  private val ClaimScope = "dual_axis_local"
  private val BindArchetype = "break_plus_entry"
  private val SurfaceInflationTokens =
    List(
      "no counterplay",
      "completely tied",
      "completely bound",
      "completely shut down",
      "wins by force",
      "winning route",
      "winning plan",
      "totally squeezed",
      "dominates the whole position"
    )
  private val StaticHoldTokens =
    List("hold", "waiting", "stays intact", "just keeps", "sit", "locked")
  private val ContinuityTokens =
    List(
      "convert",
      "conversion",
      "follow-through",
      "follow through",
      "stabiliz",
      "prepare",
      "double",
      "penetrat",
      "invasion",
      "improve",
      "activate"
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
    val relevantPreventedPlans =
      preventedPlans.filter(_.sourceScope == FactScope.Now)
    val axisSignals = relevantPreventedPlans.flatMap(axisSignalsOf)
    val breakSignals = axisSignals.filter(_.kind == "break_axis")
    val entrySignals = axisSignals.filter(_.kind == "entry_axis")
    Option.when(isApplicablePlan(plan) && breakSignals.nonEmpty && entrySignals.nonEmpty) {
      val supportResults =
        plan.supportProbeIds.flatMap(probeResultsById.get).distinctBy(_.id)
      val validationResults =
        supportResults.filter(result =>
          result.purpose.exists(purpose =>
            ValidationPurposes.contains(normalize(purpose))
          )
        )
      val continuityResults =
        supportResults.filter(result =>
          result.purpose.exists(purpose =>
            ContinuityPurposes.contains(normalize(purpose))
          )
        )
      val directReplyResults =
        supportResults.filter(result =>
          result.purpose.exists(purpose =>
            DirectReplyPurposes.contains(normalize(purpose))
          )
        )
      val bestDefenseResult =
        directReplyResults.find(result =>
          hasReplyCoverage(result) &&
            result.bestReplyPv.headOption.flatMap(clean).nonEmpty &&
            branchKey(result).nonEmpty
        )
      val bestDefenseFound =
        bestDefenseResult.flatMap(_.bestReplyPv.headOption.flatMap(clean))
      val bestDefenseBranchKey =
        bestDefenseResult.flatMap(branchKey)
      val sameBranchValidationResults =
        validationResults.filter(result =>
          matchesDefendedBranch(result, bestDefenseBranchKey)
        )
      val sameBranchPersistenceResults =
        (directReplyResults ++ validationResults)
          .distinctBy(_.id)
          .filter(result => matchesDefendedBranch(result, bestDefenseBranchKey))
      val sameBranchContinuityResults =
        (directReplyResults ++ continuityResults)
          .distinctBy(_.id)
          .filter(result => matchesDefendedBranch(result, bestDefenseBranchKey))
      val sameBranchContinuationSupportResults =
        continuityResults.filter(result =>
          matchesDefendedBranch(result, bestDefenseBranchKey)
        )
      val directBestDefensePresent =
        directReplyResults.nonEmpty &&
          bestDefenseFound.nonEmpty &&
          bestDefenseBranchKey.nonEmpty
      val defenderResources = distinctDefenderResources(directReplyResults)
      val bestReplyStable =
        directBestDefensePresent &&
          defenderResources.nonEmpty &&
          defenderResources.size <= RestrictedResourceCap &&
          directReplyResults.forall(hasReplyCoverage) &&
          directReplyResults.forall(result =>
            result.l1Delta.flatMap(_.collapseReason).forall(reason => clean(reason).isEmpty)
          )
      val primaryBreak =
        strongestAxis(breakSignals)
      val corroboratingEntry =
        primaryBreak.flatMap(primary =>
          strongestIndependentEntry(primary, entrySignals)
        )
      val axisIndependence =
        buildAxisIndependence(primaryBreak, entrySignals, corroboratingEntry)
      val freeingResourcesRemaining =
        remainingAxes(primaryBreak, corroboratingEntry, breakSignals, entrySignals)
      val tacticalReleasesRemaining =
        directReplyResults.flatMap(tacticalReleaseSignals).distinct
      val futureSnapshotPersistence =
        sameBranchValidationResults.nonEmpty &&
          sameBranchPersistenceResults.exists(result =>
            result.futureSnapshot.exists(snapshot =>
              isPositiveDualAxisSnapshot(snapshot) && tacticalReleaseSignals(result).isEmpty
            )
          )
      val convertReplyAligned =
        sameBranchContinuityResults.exists(result =>
          normalize(result.purpose.getOrElse("")) == "convert_reply_multipv" &&
            result.futureSnapshot.exists(mentionsBoundedContinuation)
        )
      val boundedContinuationVisible =
        sameBranchContinuityResults.exists(result =>
          result.futureSnapshot.exists(mentionsBoundedContinuation) ||
            result.keyMotifs.exists(mentionsContinuation)
        )
      val sameDefendedBranch =
        directBestDefensePresent &&
          sameBranchValidationResults.nonEmpty &&
          sameBranchContinuationSupportResults.nonEmpty
      val routeContinuity =
        RouteContinuity(
          directBestDefensePresent = directBestDefensePresent,
          bestDefenseStable = bestReplyStable,
          futureSnapshotPersistent = futureSnapshotPersistence,
          convertReplyAligned = convertReplyAligned,
          boundedContinuationVisible = boundedContinuationVisible,
          sameDefendedBranch = sameDefendedBranch
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
          PlayerFacingClaimOntologyFamily.RouteDenial,
          PlayerFacingClaimOntologyFamily.LongTermRestraint
        ).contains(plan.claimCertification.ontologyFamily)
      val primaryAxisMeasured =
        primaryBreak.exists(axisBurdenMeasured)
      val corroboratingAxisMeasured =
        corroboratingEntry.exists(axisBurdenMeasured)
      val restrictionDeltaMeasured =
        primaryAxisMeasured && corroboratingAxisMeasured
      val waitingMoveOnly =
        !primaryAxisMeasured && !corroboratingAxisMeasured
      val staticHoldOnly =
        sameBranchContinuityResults.exists(result =>
          result.futureSnapshot.exists(looksStaticHoldOnly) ||
            result.keyMotifs.exists(looksStaticHoldOnly)
        )
      val fortressRisk =
        restrictionDeltaMeasured &&
          axisIndependence.proven &&
          bestReplyStable &&
          futureSnapshotPersistence &&
          sameDefendedBranch &&
          !boundedContinuationVisible &&
          staticHoldOnly
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
      val moveOrderFragility =
        MoveOrderFragility(
          fragile = fragilityReasons.nonEmpty,
          reasons = fragilityReasons
        )
      val restrictionEvidence =
        RestrictionEvidence(
          primaryAxis = primaryBreak.map(toDescriptor),
          corroboratingAxes = corroboratingEntry.toList.map(toDescriptor),
          primaryAxisMeasured = primaryAxisMeasured,
          corroboratingAxisMeasured = corroboratingAxisMeasured,
          restrictionDeltaMeasured = restrictionDeltaMeasured,
          bestDefenseStable = bestReplyStable,
          futureSnapshotPersistence = futureSnapshotPersistence
        )
      val counterplayReinflationRisk =
        if primaryBreak.isEmpty ||
          corroboratingEntry.isEmpty ||
          !axisIndependence.proven ||
          !routeContinuity.directBestDefensePresent ||
          !routeContinuity.sameDefendedBranch ||
          !distinctiveEnough ||
          !ontologyAllowed ||
          !routeContinuity.boundedContinuationVisible ||
          containsInflationShell(plan.hypothesis) ||
          defenderResources.size > RestrictedResourceCap
        then HighReinflationRisk
        else BoundedDualAxisOnly
      val failsIf =
        List(
          Option.when(validationResults.isEmpty)("pv_restatement_only"),
          Option.when(!routeContinuity.directBestDefensePresent)("direct_best_defense_missing"),
          Option.when(!lateMiddlegameSlice || !clearlyBetter || !distinctiveEnough || !ontologyAllowed)(
            "local_to_global_overreach"
          ),
          Option.when(waitingMoveOnly)(
            "waiting_move_disguised_as_bind"
          ),
          Option.when(!waitingMoveOnly && !restrictionDeltaMeasured)("dual_axis_burden_missing"),
          Option.when(!axisIndependence.proven)("axis_independence_not_proven"),
          Option.when(
            freeingResourcesRemaining.nonEmpty ||
              primaryBreak.isEmpty ||
              corroboratingEntry.isEmpty ||
              (defenderResources.nonEmpty && defenderResources.size > RestrictedResourceCap)
          )("hidden_freeing_break"),
          Option.when(tacticalReleasesRemaining.nonEmpty)("hidden_tactical_release"),
          Option.when(!bestReplyStable)("cooperative_defense"),
          Option.when(!routeContinuity.boundedContinuationVisible)("route_continuity_missing"),
          Option.when(
            routeContinuity.directBestDefensePresent &&
              validationResults.nonEmpty &&
              !routeContinuity.sameDefendedBranch
          )("stitched_defended_branch"),
          Option.when(fortressRisk)("fortress_like_but_not_winning"),
          Option.when(moveOrderFragility.fragile)("move_order_fragility"),
          Option.when(counterplayReinflationRisk == HighReinflationRisk)("surface_reinflation")
        ).flatten.distinct
      Contract(
        strategyHypothesis = displayHypothesis(plan),
        claimScope = ClaimScope,
        primaryAxis = primaryBreak.map(toDescriptor),
        corroboratingAxes = corroboratingEntry.toList.map(toDescriptor),
        axisIndependence = axisIndependence,
        bindArchetype = BindArchetype,
        restrictionEvidence = restrictionEvidence,
        defenderResources = defenderResources,
        freeingResourcesRemaining = freeingResourcesRemaining,
        tacticalReleasesRemaining = tacticalReleasesRemaining,
        bestDefenseFound = bestDefenseFound,
        bestDefenseBranchKey = bestDefenseBranchKey,
        persistenceAfterBestDefense =
          bestReplyStable &&
            futureSnapshotPersistence &&
            axisIndependence.proven &&
            routeContinuity.sameDefendedBranch,
        routeContinuity = routeContinuity,
        fortressRisk = fortressRisk,
        moveOrderFragility = moveOrderFragility,
        counterplayReinflationRisk = counterplayReinflationRisk,
        claimCertification = plan.claimCertification,
        failsIf = failsIf,
        confidence =
          confidenceScore(
            lateMiddlegameSlice = lateMiddlegameSlice,
            clearlyBetter = clearlyBetter,
            directBestDefensePresent = directBestDefensePresent,
            restrictionDeltaMeasured = restrictionDeltaMeasured,
            axisIndependence = axisIndependence.proven,
            bestReplyStable = bestReplyStable,
            futureSnapshotPersistence = futureSnapshotPersistence,
            boundedContinuationVisible = boundedContinuationVisible,
            sameDefendedBranch = routeContinuity.sameDefendedBranch,
            defenderResourceCount = defenderResources.size,
            hiddenResourceCount = freeingResourcesRemaining.size,
            tacticalReleaseCount = tacticalReleasesRemaining.size,
            moveOrderFragility = moveOrderFragility,
            distinctiveEnough = distinctiveEnough,
            ontologyAllowed = ontologyAllowed,
            counterplayReinflationRisk = counterplayReinflationRisk,
            fortressRisk = fortressRisk
          ),
        evidenceSources =
          (plan.hypothesis.evidenceSources ++
            sameBranchPersistenceResults.flatMap(_.purpose.flatMap(clean)) ++
            sameBranchContinuationSupportResults.flatMap(_.purpose.flatMap(clean)) ++
            relevantPreventedPlans.flatMap(preventedEvidenceSignals)).distinct
      )
    }

  private def isApplicablePlan(
      plan: PlanEvidenceEvaluator.EvaluatedPlan
  ): Boolean =
    normalize(plan.themeL1) == ThemeTaxonomy.ThemeL1.RestrictionProphylaxis.id &&
      plan.subplanId.exists(id => ApplicableSubplans.contains(normalize(id)))

  private def strongestAxis(signals: List[AxisSignal]): Option[AxisSignal] =
    signals.sortBy(signal =>
      (-signal.counterplayScoreDrop, -signal.breakNeutralizationStrength.getOrElse(0))
    ).headOption

  private def strongestIndependentEntry(
      primaryBreak: AxisSignal,
      entrySignals: List[AxisSignal]
  ): Option[AxisSignal] =
    entrySignals
      .filter(signal => independentFromBreak(primaryBreak, signal))
      .sortBy(signal =>
        (-signal.counterplayScoreDrop, -signal.breakNeutralizationStrength.getOrElse(0))
      )
      .headOption

  private def buildAxisIndependence(
      primaryBreak: Option[AxisSignal],
      entrySignals: List[AxisSignal],
      corroboratingEntry: Option[AxisSignal]
  ): AxisIndependence =
    val reasons =
      primaryBreak match
        case None => List("missing_primary_break_axis")
        case Some(_) if entrySignals.isEmpty => List("missing_entry_axis")
        case Some(primary) if corroboratingEntry.isEmpty =>
          val overlapping = entrySignals.exists(entry => !independentFromBreak(primary, entry))
          if overlapping then List("entry_axis_restates_break_axis")
          else List("no_independent_entry_axis")
        case _ => Nil
    AxisIndependence(
      primaryAxis = primaryBreak.map(_.label),
      corroboratingAxis = corroboratingEntry.map(_.label),
      proven = reasons.isEmpty,
      reasons = reasons
    )

  private def remainingAxes(
      primaryBreak: Option[AxisSignal],
      corroboratingEntry: Option[AxisSignal],
      breakSignals: List[AxisSignal],
      entrySignals: List[AxisSignal]
  ): List[String] =
    val primaryLabels =
      primaryBreak.toList.map(signal => s"${signal.kind}:${signal.label}") ++
        corroboratingEntry.toList.map(signal => s"${signal.kind}:${signal.label}")
    (breakSignals ++ entrySignals)
      .map(signal => s"${signal.kind}:${signal.label}")
      .distinct
      .filterNot(primaryLabels.contains)

  private def independentFromBreak(
      breakAxis: AxisSignal,
      entryAxis: AxisSignal
  ): Boolean =
    val breakSquare = normalizeSquareLike(breakAxis.label)
    val breakFile = breakFileToken(breakAxis.label)
    entryAxis.squares.nonEmpty &&
      entryAxis.squares.exists { square =>
        val normalizedSquare = normalizeSquareLike(square)
        val file = breakFileToken(square)
        normalizedSquare.nonEmpty &&
        normalizedSquare != breakSquare &&
        file.nonEmpty &&
        file != breakFile
      }

  private def axisSignalsOf(
      plan: PreventedPlan
  ): List[AxisSignal] =
    val breakAxis =
      plan.breakNeutralized.flatMap(clean).map { axis =>
        AxisSignal(
          kind = "break_axis",
          label = axis,
          deniedResourceClass = plan.deniedResourceClass,
          counterplayScoreDrop = plan.counterplayScoreDrop,
          breakNeutralizationStrength = plan.breakNeutralizationStrength,
          defensiveSufficiency = plan.defensiveSufficiency,
          squares = Nil
        )
      }
    val entryAxis =
      Option.when(
        plan.deniedResourceClass.exists(resource =>
          normalize(resource) == "entry_square" || normalize(resource) == "entry"
        ) && plan.deniedSquares.nonEmpty
      )(
        AxisSignal(
          kind = "entry_axis",
          label = plan.deniedSquares.map(_.key).distinct.sorted.mkString(","),
          deniedResourceClass = plan.deniedResourceClass,
          counterplayScoreDrop = plan.counterplayScoreDrop,
          breakNeutralizationStrength = plan.breakNeutralizationStrength,
          defensiveSufficiency = plan.defensiveSufficiency,
          squares = plan.deniedSquares.map(_.key).distinct.sorted
        )
      )
    List(breakAxis, entryAxis).flatten

  private def toDescriptor(
      axis: AxisSignal
  ): AxisDescriptor =
    AxisDescriptor(
      kind = axis.kind,
      label = axis.label,
      deniedResourceClass = axis.deniedResourceClass,
      counterplayScoreDrop = axis.counterplayScoreDrop,
      breakNeutralizationStrength = axis.breakNeutralizationStrength,
      defensiveSufficiency = axis.defensiveSufficiency
    )

  private def axisBurdenMeasured(
      axis: AxisSignal
  ): Boolean =
    axis.counterplayScoreDrop >= CounterplayCompressionFloor ||
      axis.breakNeutralizationStrength.exists(_ >= BreakNeutralizationFloor) ||
      axis.defensiveSufficiency.exists(_ >= BreakNeutralizationFloor)

  private def confidenceScore(
      lateMiddlegameSlice: Boolean,
      clearlyBetter: Boolean,
      directBestDefensePresent: Boolean,
      restrictionDeltaMeasured: Boolean,
      axisIndependence: Boolean,
      bestReplyStable: Boolean,
      futureSnapshotPersistence: Boolean,
      boundedContinuationVisible: Boolean,
      sameDefendedBranch: Boolean,
      defenderResourceCount: Int,
      hiddenResourceCount: Int,
      tacticalReleaseCount: Int,
      moveOrderFragility: MoveOrderFragility,
      distinctiveEnough: Boolean,
      ontologyAllowed: Boolean,
      counterplayReinflationRisk: String,
      fortressRisk: Boolean
  ): Double =
    val base = 0.42
    val phaseBonus = if lateMiddlegameSlice then 0.07 else 0.0
    val evalBonus = if clearlyBetter then 0.07 else 0.0
    val directBestDefenseBonus = if directBestDefensePresent then 0.06 else 0.0
    val restrictionBonus = if restrictionDeltaMeasured then 0.10 else 0.0
    val independenceBonus = if axisIndependence then 0.10 else 0.0
    val replyBonus = if bestReplyStable then 0.06 else 0.0
    val futureBonus = if futureSnapshotPersistence then 0.05 else 0.0
    val continuityBonus = if boundedContinuationVisible then 0.07 else 0.0
    val branchBundleBonus = if sameDefendedBranch then 0.05 else 0.0
    val resourceBonus =
      if defenderResourceCount > 0 && defenderResourceCount <= RestrictedResourceCap then 0.04
      else 0.0
    val distinctivenessBonus =
      if distinctiveEnough && ontologyAllowed && counterplayReinflationRisk == BoundedDualAxisOnly then 0.04
      else 0.0
    val hiddenResourcePenalty = math.min(0.20, hiddenResourceCount * 0.10)
    val tacticalReleasePenalty = math.min(0.18, tacticalReleaseCount * 0.08)
    val fragilityPenalty = if moveOrderFragility.fragile then 0.12 else 0.0
    val reinflationPenalty = if counterplayReinflationRisk == HighReinflationRisk then 0.12 else 0.0
    val fortressPenalty = if fortressRisk then 0.10 else 0.0
    (base + phaseBonus + evalBonus + directBestDefenseBonus + restrictionBonus + independenceBonus + replyBonus + futureBonus + continuityBonus + branchBundleBonus + resourceBonus + distinctivenessBonus -
      hiddenResourcePenalty - tacticalReleasePenalty - fragilityPenalty - reinflationPenalty - fortressPenalty)
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
      result.keyMotifs.flatMap(clean).filter(looksLikeTacticalRelease).map(motif => s"motif:$motif")
    (collapseSignals ++ snapshotSignals ++ motifSignals).distinct

  private def isPositiveDualAxisSnapshot(
      snapshot: FutureSnapshot
  ): Boolean =
    snapshot.planBlockersRemoved.exists(mentionsCounterplayAxis) &&
      snapshot.planPrereqsMet.exists(mentionsCounterplayAxis)

  private def mentionsBoundedContinuation(
      snapshot: FutureSnapshot
  ): Boolean =
    (snapshot.planPrereqsMet ++ snapshot.planBlockersRemoved ++ snapshot.targetsDelta.strategicAdded)
      .flatMap(clean)
      .exists(mentionsContinuation)

  private def looksStaticHoldOnly(
      raw: String
  ): Boolean =
    val low = normalize(raw)
    StaticHoldTokens.exists(low.contains) &&
      !ContinuityTokens.exists(low.contains)

  private def looksStaticHoldOnly(
      snapshot: FutureSnapshot
  ): Boolean =
    val texts =
      snapshot.planPrereqsMet ++ snapshot.planBlockersRemoved ++
        snapshot.targetsDelta.strategicAdded ++ snapshot.targetsDelta.strategicRemoved
    texts.flatMap(clean).exists(looksStaticHoldOnly)

  private def mentionsContinuation(
      raw: String
  ): Boolean =
    ContinuityTokens.exists(token => normalize(raw).contains(token))

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
      expectedBranchKey: Option[String]
  ): Boolean =
    expectedBranchKey.exists(expected =>
      branchKey(result).contains(expected)
    )

  private def branchKey(
      result: ProbeResult
  ): Option[String] =
    result.variationHash.flatMap(clean)
      .orElse(result.seedId.flatMap(clean))
      .orElse(branchLineKey(result.bestReplyPv))
      .orElse(
        result.replyPvs
          .flatMap(_.headOption)
          .flatMap(branchLineKey)
      )
      .orElse(result.probedMove.flatMap(clean))
      .orElse(result.candidateMove.flatMap(clean))

  private def branchLineKey(
      moves: List[String]
  ): Option[String] =
    moves.headOption.flatMap(clean)

  private def displayHypothesis(
      plan: PlanEvidenceEvaluator.EvaluatedPlan
  ): String =
    clean(plan.hypothesis.planName)
      .orElse(clean(plan.hypothesis.planId))
      .getOrElse("dual-axis bind plan")

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
      Option.when(plan.deniedSquares.nonEmpty)(
        s"denied_squares:${plan.deniedSquares.map(_.key).distinct.sorted.mkString(",")}"
      ),
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
      hypothesis: lila.llm.model.authoring.PlanHypothesis
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
    List("counterplay", "break", "entry", "activity", "freeing", "route").exists(low.contains) ||
      (
        normalizeSquareLike(raw).nonEmpty &&
          List("unavailable", "closed", "denied", "shut").exists(low.contains)
      )

  private def breakFileToken(
      raw: String
  ): String =
    "(?i)([a-h])".r.findFirstMatchIn(Option(raw).getOrElse("")).map(_.group(1).toLowerCase).getOrElse("")

  private def normalizeSquareLike(
      raw: String
  ): String =
    "(?i)([a-h][1-8])".r.findFirstMatchIn(Option(raw).getOrElse("")).map(_.group(1).toLowerCase).getOrElse("")

  private def clean(
      raw: String
  ): Option[String] =
    Option(raw).map(_.trim).filter(_.nonEmpty)

  private def normalize(
      raw: String
  ): String =
    clean(raw).map(_.toLowerCase).getOrElse("")
