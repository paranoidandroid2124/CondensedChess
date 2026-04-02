package lila.llm.analysis

import lila.llm.analysis.ThemeTaxonomy.SubplanId
import lila.llm.model.{ FactScope, FutureSnapshot, NarrativeContext, PreventedPlanInfo, ProbeResult }
import lila.llm.model.authoring.PlanHypothesis
import lila.llm.model.strategic.PreventedPlan

private[llm] object LocalFileEntryBindCertification:

  final case class FileAxis(
      label: String,
      file: String,
      deniedResourceClass: Option[String],
      deniedEntryScope: Option[String],
      counterplayScoreDrop: Int,
      breakNeutralizationStrength: Option[Int],
      defensiveSufficiency: Option[Int]
  )

  final case class EntryAxis(
      square: String,
      deniedResourceClass: Option[String],
      counterplayScoreDrop: Int,
      breakNeutralizationStrength: Option[Int],
      defensiveSufficiency: Option[Int]
  )

  final case class AxisIndependence(
      primaryAxis: Option[String],
      corroboratingEntryAxis: Option[String],
      proven: Boolean,
      reasons: List[String]
  )

  final case class FileUsabilityEvidence(
      file: Option[String],
      deniedEntryScope: Option[String],
      deniedResourceClass: Option[String],
      counterplayScoreDrop: Int,
      breakNeutralizationStrength: Option[Int],
      defensiveSufficiency: Option[Int],
      opponentFacingRouteLoss: Boolean,
      sameBranchFilePersistence: Boolean
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
      primaryAxis: Option[FileAxis],
      corroboratingEntryAxis: Option[EntryAxis],
      axisIndependence: AxisIndependence,
      fileUsabilityEvidence: FileUsabilityEvidence,
      entryAxisPersistence: Boolean,
      pressurePersistence: Boolean,
      bestDefenseFound: Option[String],
      bestDefenseBranchKey: Option[String],
      routeContinuity: RouteContinuity,
      releaseRisksRemaining: List[String],
      fileOccupancyOnlyRisk: Boolean,
      fortressRisk: Boolean,
      counterplayReinflationRisk: String,
      moveOrderFragility: MoveOrderFragility,
      claimCertification: PlanEvidenceEvaluator.ClaimCertification,
      failsIf: List[String],
      confidence: Double,
      evidenceSources: List[String]
  ):
    def certified: Boolean = failsIf.isEmpty

  final case class SurfacePair(
      file: String,
      entrySquare: String,
      counterplayScoreDrop: Int
  )

  private final case class FileAxisSignal(
      label: String,
      file: String,
      deniedResourceClass: Option[String],
      deniedEntryScope: Option[String],
      counterplayScoreDrop: Int,
      breakNeutralizationStrength: Option[Int],
      defensiveSufficiency: Option[Int]
  )

  private final case class EntryAxisSignal(
      square: String,
      deniedResourceClass: Option[String],
      counterplayScoreDrop: Int,
      breakNeutralizationStrength: Option[Int],
      defensiveSufficiency: Option[Int]
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
  private val ClaimScope = "local_file_entry"
  private val HighReinflationRisk = "high"
  private val BoundedFileEntryOnly = "bounded_file_entry_only"
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
      "whole position"
    )
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
  private val TacticalReleaseTokens =
    List("sac", "perpet", "forcing", "tactic", "check chain", "rook lift", "queen infiltration")
  private val FileDenialTokens =
    List(
      "closed",
      "shut",
      "denied",
      "unavailable",
      "sealed",
      "no access",
      "cannot use",
      "can t use",
      "counterplay route",
      "entry route"
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
    val fileAxisSignals = relevantPreventedPlans.flatMap(fileAxisSignal)
    val entryAxisSignals = relevantPreventedPlans.flatMap(entryAxisSignal)
    Option.when(isApplicablePlan(plan) && fileAxisSignals.nonEmpty && entryAxisSignals.nonEmpty) {
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
      val primaryFile =
        strongestFileAxis(fileAxisSignals)
      val corroboratingEntry =
        primaryFile.flatMap(primary =>
          strongestIndependentEntry(primary, entryAxisSignals)
        )
      val axisIndependence =
        buildAxisIndependence(primaryFile, entryAxisSignals, corroboratingEntry)
      val fileUsabilityMeasured =
        primaryFile.exists(axisBurdenMeasured)
      val fileRouteLossVisible =
        primaryFile.exists(primary =>
          sameBranchPersistenceResults.exists(result =>
            mentionsFileDenial(result, primary.file)
          )
        )
      val entryAxisMeasured =
        corroboratingEntry.exists(axisBurdenMeasured)
      val entryAxisPersistence =
        corroboratingEntry.exists(entry =>
          sameBranchPersistenceResults.exists(result =>
            mentionsEntryDenial(result, entry.square)
          )
        )
      val futureSnapshotPersistence =
        sameBranchValidationResults.nonEmpty &&
          fileRouteLossVisible &&
          entryAxisPersistence
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
          sameBranchContinuityResults.nonEmpty
      val pressurePersistence =
        bestReplyStable &&
          futureSnapshotPersistence &&
          sameDefendedBranch
      val releaseRisksRemaining =
        remainingReleaseRisks(
          primaryFile = primaryFile,
          corroboratingEntry = corroboratingEntry,
          fileAxisSignals = fileAxisSignals,
          entryAxisSignals = entryAxisSignals
        )
      val tacticalReleasesRemaining =
        directReplyResults.flatMap(tacticalReleaseSignals).distinct
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
      val fileUsabilityEvidence =
        FileUsabilityEvidence(
          file = primaryFile.map(_.label),
          deniedEntryScope = primaryFile.flatMap(_.deniedEntryScope),
          deniedResourceClass = primaryFile.flatMap(_.deniedResourceClass),
          counterplayScoreDrop = primaryFile.map(_.counterplayScoreDrop).getOrElse(0),
          breakNeutralizationStrength = primaryFile.flatMap(_.breakNeutralizationStrength),
          defensiveSufficiency = primaryFile.flatMap(_.defensiveSufficiency),
          opponentFacingRouteLoss =
            fileUsabilityMeasured &&
              primaryFile.exists(_.deniedEntryScope.contains("file")) &&
              fileRouteLossVisible,
          sameBranchFilePersistence = fileRouteLossVisible
        )
      val fileOccupancyOnlyRisk =
        primaryFile.isEmpty || !fileUsabilityEvidence.opponentFacingRouteLoss
      val fortressRisk =
        fileUsabilityEvidence.opponentFacingRouteLoss &&
          entryAxisPersistence &&
          bestReplyStable &&
          sameDefendedBranch &&
          !boundedContinuationVisible
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
      val coreFails =
        List(
          Option.when(validationResults.isEmpty)("pv_restatement_only"),
          Option.when(!directBestDefensePresent)("direct_best_defense_missing"),
          Option.when(!lateMiddlegameSlice)("slice_scope_violation"),
          Option.when(!clearlyBetter)("slight_edge_overclaim"),
          Option.when(fileOccupancyOnlyRisk || !fileUsabilityMeasured)("file_occupancy_only"),
          Option.when(!entryAxisMeasured)("entry_axis_burden_missing"),
          Option.when(!axisIndependence.proven)("entry_axis_not_independent"),
          Option.when(
            directBestDefensePresent &&
              corroboratingEntry.nonEmpty &&
              !entryAxisPersistence
          )("entry_axis_persistence_missing"),
          Option.when(releaseRisksRemaining.nonEmpty)("hidden_off_file_release"),
          Option.when(tacticalReleasesRemaining.nonEmpty)("hidden_tactical_release"),
          Option.when(
            directBestDefensePresent &&
              validationResults.nonEmpty &&
              !sameDefendedBranch
          )("stitched_defended_branch"),
          Option.when(!bestReplyStable)("cooperative_defense"),
          Option.when(fileUsabilityEvidence.opponentFacingRouteLoss && entryAxisPersistence && !boundedContinuationVisible)(
            "fortress_like_but_not_progressing"
          ),
          Option.when(moveOrderFragility.fragile)("move_order_fragility")
        ).flatten.distinct
      val counterplayReinflationRisk =
        if coreFails.nonEmpty ||
          primaryFile.isEmpty ||
          corroboratingEntry.isEmpty ||
          !axisIndependence.proven ||
          fileOccupancyOnlyRisk ||
          !routeContinuity.directBestDefensePresent ||
          !routeContinuity.sameDefendedBranch ||
          !distinctiveEnough ||
          !ontologyAllowed ||
          !routeContinuity.boundedContinuationVisible ||
          containsInflationShell(plan.hypothesis) ||
          defenderResources.size > RestrictedResourceCap
        then HighReinflationRisk
        else BoundedFileEntryOnly
      val failsIf =
        (coreFails ++
          Option.when(counterplayReinflationRisk == HighReinflationRisk)("surface_reinflation"))
          .distinct
      Contract(
        strategyHypothesis = displayHypothesis(plan),
        claimScope = ClaimScope,
        primaryAxis = primaryFile.map(toFileAxis),
        corroboratingEntryAxis = corroboratingEntry.map(toEntryAxis),
        axisIndependence = axisIndependence,
        fileUsabilityEvidence = fileUsabilityEvidence,
        entryAxisPersistence = entryAxisPersistence,
        pressurePersistence = pressurePersistence,
        bestDefenseFound = bestDefenseFound,
        bestDefenseBranchKey = bestDefenseBranchKey,
        routeContinuity = routeContinuity,
        releaseRisksRemaining = releaseRisksRemaining ++ tacticalReleasesRemaining,
        fileOccupancyOnlyRisk = fileOccupancyOnlyRisk,
        fortressRisk = fortressRisk,
        counterplayReinflationRisk = counterplayReinflationRisk,
        moveOrderFragility = moveOrderFragility,
        claimCertification = plan.claimCertification,
        failsIf = failsIf,
        confidence =
          confidenceScore(
            lateMiddlegameSlice = lateMiddlegameSlice,
            clearlyBetter = clearlyBetter,
            directBestDefensePresent = directBestDefensePresent,
            fileUsabilityMeasured = fileUsabilityMeasured,
            fileRouteLossVisible = fileRouteLossVisible,
            entryAxisMeasured = entryAxisMeasured,
            entryAxisPersistence = entryAxisPersistence,
            axisIndependence = axisIndependence.proven,
            bestReplyStable = bestReplyStable,
            futureSnapshotPersistence = futureSnapshotPersistence,
            boundedContinuationVisible = boundedContinuationVisible,
            sameDefendedBranch = sameDefendedBranch,
            releaseRiskCount = releaseRisksRemaining.size,
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
            sameBranchContinuityResults.flatMap(_.purpose.flatMap(clean)) ++
            relevantPreventedPlans.flatMap(preventedEvidenceSignals)).distinct
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

  def certifiedSurfacePair(
      ctx: NarrativeContext
  ): Option[SurfacePair] =
    certifiedSurfacePair(
      preventedPlans =
        ctx.semantic.toList.flatMap(_.preventedPlans).filter(_.sourceScope == FactScope.Now),
      evidenceBackedPlans = StrategicNarrativePlanSupport.evidenceBackedMainPlans(ctx)
    )

  def certifiedSurfacePair(
      preventedPlans: List[PreventedPlanInfo],
      evidenceBackedPlans: List[PlanHypothesis]
  ): Option[SurfacePair] =
    surfacePair(
      preventedPlans = preventedPlans.filter(_.sourceScope == FactScope.Now),
      evidenceBackedPlans = evidenceBackedPlans.filter(plan =>
        normalize(plan.themeL1) == ThemeTaxonomy.ThemeL1.RestrictionProphylaxis.id &&
          plan.subplanId.exists(id => ApplicableSubplans.contains(normalize(id)))
      )
    )

  private def surfacePair(
      preventedPlans: List[PreventedPlanInfo],
      evidenceBackedPlans: List[PlanHypothesis]
  ): Option[SurfacePair] =
    val fileAxes =
      preventedPlans.flatMap(surfaceFileAxis)
    val entryAxes =
      preventedPlans.flatMap(surfaceEntryAxis)
    strongestFileAxis(fileAxes).flatMap { primary =>
      strongestIndependentEntry(primary, entryAxes)
        .filter(entry => evidenceBackedPlans.exists(plan => planMentionsFileEntryPair(plan, primary.label, entry.square)))
        .map { entry =>
          SurfacePair(
            file = primary.label,
            entrySquare = entry.square,
            counterplayScoreDrop =
              List(primary.counterplayScoreDrop, entry.counterplayScoreDrop).max
          )
        }
    }

  private def isApplicablePlan(
      plan: PlanEvidenceEvaluator.EvaluatedPlan
  ): Boolean =
    normalize(plan.themeL1) == ThemeTaxonomy.ThemeL1.RestrictionProphylaxis.id &&
      plan.subplanId.exists(id => ApplicableSubplans.contains(normalize(id)))

  private def strongestFileAxis(
      signals: List[FileAxisSignal]
  ): Option[FileAxisSignal] =
    signals.sortBy(signal =>
      (-signal.counterplayScoreDrop, -signal.breakNeutralizationStrength.getOrElse(0))
    ).headOption

  private def strongestIndependentEntry(
      primaryFile: FileAxisSignal,
      signals: List[EntryAxisSignal]
  ): Option[EntryAxisSignal] =
    signals
      .filter(signal => independentFromFile(primaryFile.file, signal.square))
      .sortBy(signal =>
        (-signal.counterplayScoreDrop, -signal.breakNeutralizationStrength.getOrElse(0))
      )
      .headOption

  private def buildAxisIndependence(
      primaryFile: Option[FileAxisSignal],
      entrySignals: List[EntryAxisSignal],
      corroboratingEntry: Option[EntryAxisSignal]
  ): AxisIndependence =
    val reasons =
      primaryFile match
        case None => List("missing_file_axis")
        case Some(_) if entrySignals.isEmpty => List("missing_entry_axis")
        case Some(primary) if corroboratingEntry.isEmpty =>
          val overlapping =
            entrySignals.exists(entry => !independentFromFile(primary.file, entry.square))
          if overlapping then List("entry_square_restates_file_axis")
          else List("no_independent_entry_axis")
        case _ => Nil
    AxisIndependence(
      primaryAxis = primaryFile.map(_.label),
      corroboratingEntryAxis = corroboratingEntry.map(_.square),
      proven = reasons.isEmpty,
      reasons = reasons
    )

  private def remainingReleaseRisks(
      primaryFile: Option[FileAxisSignal],
      corroboratingEntry: Option[EntryAxisSignal],
      fileAxisSignals: List[FileAxisSignal],
      entryAxisSignals: List[EntryAxisSignal]
  ): List[String] =
    val usedFile = primaryFile.map(_.file)
    val usedEntry = corroboratingEntry.map(_.square)
    val alternativeFiles =
      fileAxisSignals
        .filter(signal => usedFile.forall(_ != signal.file))
        .map(signal => s"file:${signal.label}")
    val alternativeEntries =
      entryAxisSignals
        .filter(signal => usedEntry.forall(_ != signal.square))
        .map(signal => s"entry:${signal.square}")
    (alternativeFiles ++ alternativeEntries).distinct

  private def fileAxisSignal(
      plan: PreventedPlan
  ): Option[FileAxisSignal] =
    Option.when(
      plan.deniedEntryScope.contains("file") &&
        plan.breakNeutralized.flatMap(clean).exists(raw => breakFileToken(raw).nonEmpty)
    ) {
      val file = plan.breakNeutralized.flatMap(clean).map(breakFileToken).getOrElse("")
      FileAxisSignal(
        label = s"$file-file",
        file = file,
        deniedResourceClass = plan.deniedResourceClass,
        deniedEntryScope = plan.deniedEntryScope,
        counterplayScoreDrop = plan.counterplayScoreDrop,
        breakNeutralizationStrength = plan.breakNeutralizationStrength,
        defensiveSufficiency = plan.defensiveSufficiency
      )
    }

  private def entryAxisSignal(
      plan: PreventedPlan
  ): Option[EntryAxisSignal] =
    Option.when(
      plan.deniedResourceClass.exists(resource =>
        normalize(resource) == "entry_square" || normalize(resource) == "entry"
      ) && plan.deniedSquares.nonEmpty
    ) {
      val square = plan.deniedSquares.map(_.key).distinct.sorted.head
      EntryAxisSignal(
        square = square,
        deniedResourceClass = plan.deniedResourceClass,
        counterplayScoreDrop = plan.counterplayScoreDrop,
        breakNeutralizationStrength = plan.breakNeutralizationStrength,
        defensiveSufficiency = plan.defensiveSufficiency
      )
    }

  private def surfaceFileAxis(
      plan: PreventedPlanInfo
  ): Option[FileAxisSignal] =
    Option.when(
      plan.deniedEntryScope.contains("file") &&
        plan.breakNeutralized.flatMap(clean).exists(raw => breakFileToken(raw).nonEmpty)
    ) {
      val file = plan.breakNeutralized.flatMap(clean).map(breakFileToken).getOrElse("")
      FileAxisSignal(
        label = s"$file-file",
        file = file,
        deniedResourceClass = plan.deniedResourceClass,
        deniedEntryScope = plan.deniedEntryScope,
        counterplayScoreDrop = plan.counterplayScoreDrop,
        breakNeutralizationStrength = None,
        defensiveSufficiency = None
      )
    }

  private def surfaceEntryAxis(
      plan: PreventedPlanInfo
  ): Option[EntryAxisSignal] =
    Option.when(
      plan.deniedResourceClass.exists(resource =>
        normalize(resource) == "entry_square" || normalize(resource) == "entry"
      ) && plan.deniedSquares.nonEmpty
    ) {
      val square = plan.deniedSquares.distinct.sorted.head
      EntryAxisSignal(
        square = square,
        deniedResourceClass = plan.deniedResourceClass,
        counterplayScoreDrop = plan.counterplayScoreDrop,
        breakNeutralizationStrength = None,
        defensiveSufficiency = None
      )
    }

  private def toFileAxis(
      signal: FileAxisSignal
  ): FileAxis =
    FileAxis(
      label = signal.label,
      file = signal.file,
      deniedResourceClass = signal.deniedResourceClass,
      deniedEntryScope = signal.deniedEntryScope,
      counterplayScoreDrop = signal.counterplayScoreDrop,
      breakNeutralizationStrength = signal.breakNeutralizationStrength,
      defensiveSufficiency = signal.defensiveSufficiency
    )

  private def toEntryAxis(
      signal: EntryAxisSignal
  ): EntryAxis =
    EntryAxis(
      square = signal.square,
      deniedResourceClass = signal.deniedResourceClass,
      counterplayScoreDrop = signal.counterplayScoreDrop,
      breakNeutralizationStrength = signal.breakNeutralizationStrength,
      defensiveSufficiency = signal.defensiveSufficiency
    )

  private def axisBurdenMeasured(
      signal: FileAxisSignal
  ): Boolean =
    signal.counterplayScoreDrop >= CounterplayCompressionFloor ||
      signal.breakNeutralizationStrength.exists(_ >= BreakNeutralizationFloor) ||
      signal.defensiveSufficiency.exists(_ >= BreakNeutralizationFloor)

  private def axisBurdenMeasured(
      signal: EntryAxisSignal
  ): Boolean =
    signal.counterplayScoreDrop >= CounterplayCompressionFloor ||
      signal.breakNeutralizationStrength.exists(_ >= BreakNeutralizationFloor) ||
      signal.defensiveSufficiency.exists(_ >= BreakNeutralizationFloor)

  private def confidenceScore(
      lateMiddlegameSlice: Boolean,
      clearlyBetter: Boolean,
      directBestDefensePresent: Boolean,
      fileUsabilityMeasured: Boolean,
      fileRouteLossVisible: Boolean,
      entryAxisMeasured: Boolean,
      entryAxisPersistence: Boolean,
      axisIndependence: Boolean,
      bestReplyStable: Boolean,
      futureSnapshotPersistence: Boolean,
      boundedContinuationVisible: Boolean,
      sameDefendedBranch: Boolean,
      releaseRiskCount: Int,
      tacticalReleaseCount: Int,
      moveOrderFragility: MoveOrderFragility,
      distinctiveEnough: Boolean,
      ontologyAllowed: Boolean,
      counterplayReinflationRisk: String,
      fortressRisk: Boolean
  ): Double =
    val base = 0.40
    val phaseBonus = if lateMiddlegameSlice then 0.07 else 0.0
    val evalBonus = if clearlyBetter then 0.07 else 0.0
    val directBestDefenseBonus = if directBestDefensePresent then 0.06 else 0.0
    val fileBonus = if fileUsabilityMeasured && fileRouteLossVisible then 0.10 else 0.0
    val entryBonus = if entryAxisMeasured && entryAxisPersistence then 0.09 else 0.0
    val independenceBonus = if axisIndependence then 0.08 else 0.0
    val replyBonus = if bestReplyStable then 0.06 else 0.0
    val futureBonus = if futureSnapshotPersistence then 0.05 else 0.0
    val continuityBonus = if boundedContinuationVisible then 0.06 else 0.0
    val branchBonus = if sameDefendedBranch then 0.05 else 0.0
    val distinctivenessBonus =
      if distinctiveEnough && ontologyAllowed && counterplayReinflationRisk == BoundedFileEntryOnly then 0.04
      else 0.0
    val releasePenalty = math.min(0.20, releaseRiskCount * 0.10)
    val tacticalReleasePenalty = math.min(0.18, tacticalReleaseCount * 0.08)
    val fragilityPenalty = if moveOrderFragility.fragile then 0.12 else 0.0
    val reinflationPenalty = if counterplayReinflationRisk == HighReinflationRisk then 0.12 else 0.0
    val fortressPenalty = if fortressRisk then 0.10 else 0.0
    (base + phaseBonus + evalBonus + directBestDefenseBonus + fileBonus + entryBonus + independenceBonus + replyBonus + futureBonus + continuityBonus + branchBonus + distinctivenessBonus -
      releasePenalty - tacticalReleasePenalty - fragilityPenalty - reinflationPenalty - fortressPenalty)
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

  private def mentionsFileDenial(
      result: ProbeResult,
      file: String
  ): Boolean =
    result.futureSnapshot.exists(snapshot => mentionsFileDenial(snapshot, file)) ||
      result.keyMotifs.flatMap(clean).exists(text => mentionsFileDenial(text, file))

  private def mentionsFileDenial(
      snapshot: FutureSnapshot,
      file: String
  ): Boolean =
    (
      snapshot.planPrereqsMet ++
        snapshot.planBlockersRemoved ++
        snapshot.targetsDelta.strategicAdded ++
        snapshot.targetsDelta.strategicRemoved
    ).flatMap(clean).exists(text => mentionsFileDenial(text, file))

  private def mentionsFileDenial(
      raw: String,
      file: String
  ): Boolean =
    val low = normalize(raw)
    fileMention(low, file) && FileDenialTokens.exists(low.contains)

  private def mentionsEntryDenial(
      result: ProbeResult,
      square: String
  ): Boolean =
    result.futureSnapshot.exists(snapshot => mentionsEntryDenial(snapshot, square)) ||
      result.keyMotifs.flatMap(clean).exists(text => mentionsEntryDenial(text, square))

  private def mentionsEntryDenial(
      snapshot: FutureSnapshot,
      square: String
  ): Boolean =
    (
      snapshot.planPrereqsMet ++
        snapshot.planBlockersRemoved ++
        snapshot.targetsDelta.strategicAdded ++
        snapshot.targetsDelta.strategicRemoved
    ).flatMap(clean).exists(text => mentionsEntryDenial(text, square))

  private def mentionsEntryDenial(
      raw: String,
      square: String
  ): Boolean =
    val low = normalize(raw)
    low.contains(square.toLowerCase) &&
      List("closed", "denied", "unavailable", "shut", "entry", "route").exists(low.contains)

  private def mentionsBoundedContinuation(
      snapshot: FutureSnapshot
  ): Boolean =
    (
      snapshot.planPrereqsMet ++
        snapshot.planBlockersRemoved ++
        snapshot.targetsDelta.strategicAdded
    ).flatMap(clean).exists(mentionsContinuation)

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
      .getOrElse("local file-entry bind plan")

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
      ),
      plan.deniedEntryScope.flatMap(scope =>
        clean(scope).map(value => s"denied_entry_scope:$value")
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

  private def planMentionsFileEntryPair(
      plan: PlanHypothesis,
      fileLabel: String,
      entrySquare: String
  ): Boolean =
    affirmativePlanTexts(plan)
      .flatMap(clean)
      .exists { text =>
        val low = normalize(text)
        low.contains(normalize(fileLabel)) && low.contains(normalize(entrySquare))
      }

  private def affirmativePlanTexts(
      plan: PlanHypothesis
  ): List[String] =
    List(plan.planName) ++ plan.executionSteps

  private def looksLikeTacticalRelease(
      raw: String
  ): Boolean =
    val low = normalize(raw)
    TacticalReleaseTokens.exists(low.contains)

  private def fileMention(
      normalizedText: String,
      file: String
  ): Boolean =
    normalizedText.contains(s"$file-file") ||
      normalizedText.contains(s"$file file")

  private def breakFileToken(
      raw: String
  ): String =
    "(?i)([a-h])".r.findFirstMatchIn(Option(raw).getOrElse("")).map(_.group(1).toLowerCase).getOrElse("")

  private def independentFromFile(
      file: String,
      square: String
  ): Boolean =
    normalizeSquareLike(square).nonEmpty &&
      squareFileToken(square).nonEmpty &&
      squareFileToken(square) != file

  private def squareFileToken(
      raw: String
  ): String =
    "(?i)([a-h])[1-8]".r.findFirstMatchIn(Option(raw).getOrElse("")).map(_.group(1).toLowerCase).getOrElse("")

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
