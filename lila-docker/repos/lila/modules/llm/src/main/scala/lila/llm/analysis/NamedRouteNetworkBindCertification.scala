package lila.llm.analysis

import lila.llm.analysis.ThemeTaxonomy.SubplanId
import lila.llm.model.{ FactScope, FutureSnapshot, NarrativeContext, PreventedPlanInfo, ProbeResult }
import lila.llm.model.authoring.PlanHypothesis
import lila.llm.model.strategic.PreventedPlan

private[llm] object NamedRouteNetworkBindCertification:

  val OwnerSource = "named_route_network_bind"

  final case class RouteNode(
      role: String,
      square: String,
      deniedResourceClass: Option[String],
      deniedEntryScope: Option[String],
      counterplayScoreDrop: Int,
      breakNeutralizationStrength: Option[Int],
      defensiveSufficiency: Option[Int]
  )

  final case class RouteEdge(
      from: String,
      to: String,
      witnessCount: Int,
      sameDefendedBranch: Boolean
  )

  final case class RerouteDenial(
      square: String,
      counterplayScoreDrop: Int,
      witnessedOnBestDefense: Boolean,
      witnessSources: List[String]
  )

  final case class AxisIndependence(
      primaryAxis: Option[String],
      entrySquare: Option[String],
      rerouteSquare: Option[String],
      proven: Boolean,
      reasons: List[String]
  )

  final case class RouteContinuity(
      directBestDefensePresent: Boolean,
      bestDefenseStable: Boolean,
      futureSnapshotPersistent: Boolean,
      convertReplyAligned: Boolean,
      boundedContinuationVisible: Boolean,
      sameDefendedBranch: Boolean,
      routeEdgeVisible: Boolean
  )

  final case class MoveOrderFragility(
      fragile: Boolean,
      reasons: List[String]
  )

  final case class Contract(
      strategyHypothesis: String,
      claimScope: String,
      primaryAxis: Option[String],
      routeNodes: List[RouteNode],
      intermediateNodes: List[RouteNode],
      routeEdges: List[RouteEdge],
      rerouteDenials: List[RerouteDenial],
      axisIndependence: AxisIndependence,
      bestDefenseFound: Option[String],
      bestDefenseBranchKey: Option[String],
      sameDefendedBranch: Boolean,
      pressurePersistence: Boolean,
      routeContinuity: RouteContinuity,
      continuationBound: Boolean,
      releaseRisksRemaining: List[String],
      routeNetworkMirageRisk: String,
      redundantAxisRisk: String,
      postureInflationRisk: String,
      counterplayReinflationRisk: String,
      moveOrderFragility: MoveOrderFragility,
      claimCertification: PlanEvidenceEvaluator.ClaimCertification,
      failsIf: List[String],
      confidence: Double,
      evidenceSources: List[String]
  ):
    def certified: Boolean = failsIf.isEmpty

  final case class SurfaceNetwork(
      file: String,
      entrySquare: String,
      rerouteSquare: String,
      counterplayScoreDrop: Int,
      intermediateSquare: Option[String] = None
  )

  private final case class BranchIdentityResolution(
      selectedKey: Option[String],
      bestDefenseResult: Option[ProbeResult],
      bestDefenseFound: Option[String],
      sameBranchIdentityMissing: Boolean,
      ambiguousDefendedBranch: Boolean
  )

  private final case class FileAxisSignal(
      label: String,
      file: String,
      counterplayScoreDrop: Int,
      breakNeutralizationStrength: Option[Int],
      defensiveSufficiency: Option[Int]
  )

  private final case class EntryAxisSignal(
      square: String,
      counterplayScoreDrop: Int,
      breakNeutralizationStrength: Option[Int],
      defensiveSufficiency: Option[Int]
  )

  private final case class RerouteSignal(
      square: String,
      deniedResourceClass: Option[String],
      deniedEntryScope: Option[String],
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
  private val RestrictedResourceCap = 2
  private val MaxQueensForPositiveSlice = 1
  private val BranchKeyMoveCount = 2
  private val ClaimScope = "named_route_network_bind"
  private val HighRisk = "high"
  private val PlannerWhyThisOnly = "planner_why_this_only"
  private val RouteIntentTokens =
    List("reroute", "route network", "route-net", "detour", "switch wing", "route shell", "dead-end", "dead end")
  private val RouteEdgeTokens =
    List("reroute", "detour", "route", "switch", "entry", "path")
  private val DenialTokens =
    List("closed", "denied", "unavailable", "shut", "sealed", "cut off", "cannot use", "can't use", "stop", "stops", "blocked")
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
  private val ColorEscapeTokens =
    List(
      "opposite color",
      "opposite-color",
      "color complex escape",
      "color-complex escape",
      "dark-square escape",
      "light-square escape"
    )
  private val SectorEscapeTokens =
    List(
      "other wing",
      "other sector",
      "opposite wing",
      "untouched wing",
      "untouched sector",
      "switch wings"
    )

  def evaluate(
      plan: PlanEvidenceEvaluator.EvaluatedPlan,
      probeResultsById: Map[String, ProbeResult],
      preventedPlans: List[PreventedPlan],
      evalCp: Int,
      isWhiteToMove: Boolean,
      phase: String,
      ply: Int,
      fen: String,
      localFileEntryBindCertification: Option[LocalFileEntryBindCertification.Contract]
  ): Option[Contract] =
    val relevantPreventedPlans =
      preventedPlans.filter(_.sourceScope == FactScope.Now)
    val fileAxisSignals = relevantPreventedPlans.flatMap(fileAxisSignal)
    val entryAxisSignals = relevantPreventedPlans.flatMap(entryAxisSignal)
    val routeSignals = relevantPreventedPlans.flatMap(rerouteSignal)
    val primaryFile =
      localFileEntryBindCertification.flatMap(primaryAxisFromContract).orElse(strongestFileAxis(fileAxisSignals))
    val entryAxis =
      localFileEntryBindCertification.flatMap(entryAxisFromContract)
        .orElse(primaryFile.flatMap(primary => strongestIndependentEntry(primary, entryAxisSignals)))
    val routeClaimAttempted =
      mentionsRouteNetworkIntent(plan.hypothesis) ||
        routeSignals.exists(signal => planMentionsSquare(plan.hypothesis, signal.square))
    Option.when(isApplicablePlan(plan) && routeClaimAttempted) {
      val supportResults =
        plan.supportProbeIds.flatMap(probeResultsById.get).distinctBy(_.id)
      val validationResults =
        supportResults.filter(result =>
          result.purpose.exists(purpose => ValidationPurposes.contains(normalize(purpose)))
        )
      val continuityResults =
        supportResults.filter(result =>
          result.purpose.exists(purpose => ContinuityPurposes.contains(normalize(purpose)))
        )
      val directReplyResults =
        supportResults.filter(result =>
          result.purpose.exists(purpose => DirectReplyPurposes.contains(normalize(purpose)))
        )
      val branchIdentity =
        resolveBranchIdentity(directReplyResults, localFileEntryBindCertification)
      val bestDefenseResult = branchIdentity.bestDefenseResult
      val bestDefenseFound = branchIdentity.bestDefenseFound
      val bestDefenseBranchKey = branchIdentity.selectedKey
      val baseRerouteAxis =
        primaryFile.flatMap(primary =>
          entryAxis.flatMap(entry =>
            strongestIndependentReroute(primary.file, entry.square, routeSignals, plan.hypothesis)
          )
        )
      val chainSignalsMentioned =
        routeSignals.filter(signal =>
          baseRerouteAxis.forall(_.square != signal.square) &&
            planMentionsSquare(plan.hypothesis, signal.square)
        )
      val chainClaimAttempted =
        baseRerouteAxis.nonEmpty &&
          chainSignalsMentioned.nonEmpty
      val sameBranchValidationResults =
        validationResults.filter(result => matchesDefendedBranch(result, bestDefenseBranchKey))
      val sameBranchContinuityResults =
        (directReplyResults ++ continuityResults)
          .distinctBy(_.id)
          .filter(result => matchesDefendedBranch(result, bestDefenseBranchKey))
      val sameBranchPersistenceResults =
        (directReplyResults ++ validationResults)
          .distinctBy(_.id)
          .filter(result => matchesDefendedBranch(result, bestDefenseBranchKey))
      val sameBranchEdgeEvidence =
        (sameBranchValidationResults ++ sameBranchContinuityResults ++ sameBranchPersistenceResults)
          .distinctBy(_.id)
      val intermediateAxis =
        Option.when(chainClaimAttempted)(baseRerouteAxis).flatten
      val rerouteAxis =
        if chainClaimAttempted then
          entryAxis.flatMap(entry =>
            intermediateAxis.flatMap(intermediate =>
              strongestDownstreamReroute(
                entrySquare = entry.square,
                intermediateSquare = intermediate.square,
                signals = chainSignalsMentioned,
                sameBranchResults = sameBranchEdgeEvidence,
                hypothesis = plan.hypothesis
              )
            )
          )
        else baseRerouteAxis
      val expectedRouteEdges =
        entryAxis.toList.flatMap(entry =>
          rerouteAxis.toList.flatMap { reroute =>
            intermediateAxis match
              case Some(intermediate) =>
                List(
                  entry.square -> intermediate.square,
                  intermediate.square -> reroute.square
                )
              case None =>
                List(entry.square -> reroute.square)
          }
        )
      val sameBranchRerouteResults =
        rerouteAxis.toList.flatMap(reroute =>
          sameBranchPersistenceResults.filter(result => mentionsRerouteDenial(result, reroute.square))
        )
      val sameBranchEdgeResultsByPair =
        expectedRouteEdges.map { case (from, to) =>
          (from -> to) ->
            sameBranchEdgeEvidence.filter(result => mentionsRouteEdge(result, from, to))
        }.toMap
      val allSameBranchEdgesVisible =
        expectedRouteEdges.nonEmpty &&
          expectedRouteEdges.forall(pair => sameBranchEdgeResultsByPair.getOrElse(pair, Nil).nonEmpty)
      val offBranchRerouteEvidence =
        bestDefenseBranchKey.toList.flatMap(branchKey =>
          rerouteAxis.toList.flatMap(reroute =>
            (validationResults ++ continuityResults ++ directReplyResults)
              .distinctBy(_.id)
              .filter(result =>
                mentionsRerouteDenial(result, reroute.square) &&
                  !matchesDefendedBranch(result, Some(branchKey))
              )
          )
        )
      val offBranchEdgeEvidence =
        bestDefenseBranchKey.toList.flatMap(branchKey =>
          expectedRouteEdges.flatMap { case (from, to) =>
            (validationResults ++ continuityResults ++ directReplyResults)
              .distinctBy(_.id)
              .filter(result =>
                mentionsRouteEdge(result, from, to) &&
                  !matchesDefendedBranch(result, Some(branchKey))
              )
          }
        )
      val directBestDefensePresent =
        bestDefenseFound.nonEmpty &&
          bestDefenseBranchKey.nonEmpty &&
          bestDefenseResult.exists(hasReplyCoverage)
      val defenderResources = distinctDefenderResources(directReplyResults)
      val bestReplyStable =
        directBestDefensePresent &&
          defenderResources.nonEmpty &&
          defenderResources.size <= RestrictedResourceCap &&
          directReplyResults.forall(hasReplyCoverage) &&
          directReplyResults.forall(result =>
            result.l1Delta.flatMap(_.collapseReason).forall(reason => clean(reason).isEmpty)
          ) &&
          localFileEntryBindCertification.forall(_.routeContinuity.bestDefenseStable)
      val futureSnapshotPersistent =
        localFileEntryBindCertification.exists(_.routeContinuity.futureSnapshotPersistent) &&
          sameBranchRerouteResults.nonEmpty &&
          allSameBranchEdgesVisible
      val convertReplyAligned =
        sameBranchContinuityResults.exists(result =>
          normalize(result.purpose.getOrElse("")) == "convert_reply_multipv" &&
            expectedRouteEdges.forall { case (from, to) => mentionsRouteEdge(result, from, to) } &&
            result.futureSnapshot.exists(mentionsBoundedContinuation)
        )
      val boundedContinuationVisible =
        sameBranchContinuityResults.exists(result =>
          result.futureSnapshot.exists(mentionsBoundedContinuation) ||
            result.keyMotifs.exists(mentionsContinuation)
        )
      val sameDefendedBranch =
        directBestDefensePresent &&
          sameBranchRerouteResults.nonEmpty &&
          allSameBranchEdgesVisible &&
          sameBranchContinuityResults.nonEmpty
      val pressurePersistence =
        localFileEntryBindCertification.exists(_.pressurePersistence) &&
          bestReplyStable &&
          futureSnapshotPersistent &&
          sameDefendedBranch
      val routeNodes =
        List(
          entryAxis.map(toRouteNode("entry")),
          intermediateAxis.map(toRouteNode("intermediate")),
          rerouteAxis.map(toRouteNode("reroute"))
        ).flatten
      val intermediateNodes =
        intermediateAxis.toList.map(toRouteNode("intermediate"))
      val rerouteDenials =
        rerouteAxis.toList.map { reroute =>
          RerouteDenial(
            square = reroute.square,
            counterplayScoreDrop = reroute.counterplayScoreDrop,
            witnessedOnBestDefense = sameBranchRerouteResults.nonEmpty,
            witnessSources =
              sameBranchRerouteResults.flatMap(_.purpose.flatMap(clean)).distinct
          )
        }
      val routeEdges =
        expectedRouteEdges.map { case (from, to) =>
          val witnesses = sameBranchEdgeResultsByPair.getOrElse(from -> to, Nil)
          RouteEdge(
            from = from,
            to = to,
            witnessCount = witnesses.size,
            sameDefendedBranch = witnesses.nonEmpty
          )
        }
      val axisIndependence =
        buildAxisIndependence(
          primaryFile = primaryFile,
          entryAxis = entryAxis,
          routeSignals = routeSignals,
          intermediateAxis = intermediateAxis,
          rerouteAxis = rerouteAxis,
          chainClaimAttempted = chainClaimAttempted
        )
      val releaseRisksRemaining =
        remainingReleaseRisks(
          primaryFile = primaryFile,
          entryAxis = entryAxis,
          intermediateAxis = intermediateAxis,
          rerouteAxis = rerouteAxis,
          rerouteSignals = routeSignals,
          supportResults = supportResults
        )
      val routeNetworkMirageRisk =
        if sameBranchRerouteResults.nonEmpty && allSameBranchEdgesVisible then "low" else HighRisk
      val redundantAxisRisk =
        if axisIndependence.proven then "low" else HighRisk
      val routeContinuity =
        RouteContinuity(
          directBestDefensePresent = directBestDefensePresent,
          bestDefenseStable = bestReplyStable,
          futureSnapshotPersistent = futureSnapshotPersistent,
          convertReplyAligned = convertReplyAligned,
          boundedContinuationVisible = boundedContinuationVisible,
          sameDefendedBranch = sameDefendedBranch,
          routeEdgeVisible = allSameBranchEdgesVisible
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
      val moveOrderFragility =
        buildMoveOrderFragility(
          plan = plan,
          directReplyResults = directReplyResults,
          bestReplyStable = bestReplyStable,
          futureSnapshotPersistence = futureSnapshotPersistent,
          inheritedReasons =
            localFileEntryBindCertification.toList.flatMap(_.moveOrderFragility.reasons)
        )
      val heavyPieceShell =
        queenCount(fen) > MaxQueensForPositiveSlice
      val colorComplexEscape =
        supportResults.exists(result => mentionsColorComplexEscape(result)) ||
          routeSignals.exists(signal =>
            signal.deniedResourceClass.exists(resource => normalize(resource) == "color_complex_escape")
          )
      val postureInflation =
        chainClaimAttempted &&
          (!lateMiddlegameSlice || !clearlyBetter)
      val heavyPieceRouteShell =
        chainClaimAttempted && heavyPieceShell
      val untouchedSectorEscape =
        releaseRisksRemaining.exists(_.startsWith("reroute:")) ||
          releaseRisksRemaining.exists(_.startsWith("sector:"))
      val localPrereqMissing =
        localFileEntryBindCertification.forall(!_.certified)
      val enginePvParaphrase =
        validationResults.isEmpty || sameBranchValidationResults.isEmpty
      val staticWithoutProgress =
        sameBranchRerouteResults.nonEmpty &&
          allSameBranchEdgesVisible &&
          !boundedContinuationVisible
      val fakeRouteChain =
        chainClaimAttempted &&
          (rerouteAxis.isEmpty || !allSameBranchEdgesVisible || sameBranchRerouteResults.isEmpty)
      val redundantIntermediateNode =
        chainClaimAttempted &&
          (
            intermediateAxis.isEmpty ||
              intermediateAxis.exists(intermediate => !isIntermediateNodeSignal(intermediate)) ||
              !primaryFile.exists(primary =>
                entryAxis.exists(entry =>
                  intermediateAxis.exists(intermediate =>
                    independentFromFileAndEntry(
                      file = primary.file,
                      entrySquare = entry.square,
                      square = intermediate.square
                    )
                  )
                )
              )
          )
      val chainOnlyOnNonBestBranch =
        chainClaimAttempted &&
          (
            offBranchEdgeEvidence.nonEmpty ||
              (
                bestDefenseBranchKey.nonEmpty &&
                  expectedRouteEdges.nonEmpty &&
                  !sameDefendedBranch
              )
          )
      val crossBranchStitching =
        !chainClaimAttempted &&
          (
            offBranchRerouteEvidence.nonEmpty ||
          (
            bestDefenseBranchKey.nonEmpty &&
              rerouteAxis.nonEmpty &&
              (validationResults ++ continuityResults).nonEmpty &&
              !sameDefendedBranch
          )
          )
      val exactRouteMention =
        if intermediateAxis.nonEmpty then
          planMentionsBroaderRouteChain(plan.hypothesis, primaryFile, entryAxis, intermediateAxis, rerouteAxis)
        else
          planMentionsNamedRouteNetwork(plan.hypothesis, primaryFile, entryAxis, rerouteAxis)
      val exactSurfaceMention =
        if chainClaimAttempted then
          exactSurfaceNetwork(plan.hypothesis, fileAxisSignals, entryAxisSignals, routeSignals)
            .exists(_.intermediateSquare.nonEmpty)
        else exactRouteMention
      val coreFails =
        List(
          Option.when(localPrereqMissing)("file_entry_restatement_only"),
          Option.when(routeSignals.isEmpty)("file_entry_restatement_only"),
          Option.when(branchIdentity.sameBranchIdentityMissing)("same_branch_identity_missing"),
          Option.when(branchIdentity.ambiguousDefendedBranch)("ambiguous_defended_branch"),
          Option.when(!chainClaimAttempted && routeSignals.nonEmpty && !axisIndependence.proven)("redundant_square_counting"),
          Option.when(chainClaimAttempted && fakeRouteChain)("fake_route_chain"),
          Option.when(chainClaimAttempted && redundantIntermediateNode)("redundant_intermediate_node"),
          Option.when(!chainClaimAttempted && rerouteAxis.nonEmpty && (sameBranchRerouteResults.isEmpty || !allSameBranchEdgesVisible))(
            "route_network_mirage"
          ),
          Option.when(!lateMiddlegameSlice)("slice_scope_violation"),
          Option.when(!clearlyBetter)("slight_edge_overclaim"),
          Option.when(postureInflation)("posture_inflation"),
          Option.when(heavyPieceShell)("heavy_piece_release_shell"),
          Option.when(heavyPieceRouteShell)("heavy_piece_route_shell"),
          Option.when(untouchedSectorEscape)("untouched_sector_reroute"),
          Option.when(chainClaimAttempted && untouchedSectorEscape)("untouched_sector_escape"),
          Option.when(colorComplexEscape)("color_complex_escape"),
          Option.when(chainOnlyOnNonBestBranch)("chain_only_on_nonbest_branch"),
          Option.when(crossBranchStitching)("cross_branch_stitching"),
          Option.when(staticWithoutProgress)("static_net_without_progress"),
          Option.when(enginePvParaphrase)("engine_pv_paraphrase"),
          Option.when(moveOrderFragility.fragile)("move_order_fragility")
        ).flatten.distinct
      val counterplayReinflationRisk =
        if coreFails.nonEmpty ||
          !distinctiveEnough ||
          !ontologyAllowed ||
          containsInflationShell(plan.hypothesis) ||
          (!chainClaimAttempted && !exactSurfaceMention) ||
          defenderResources.size > RestrictedResourceCap
        then HighRisk
        else PlannerWhyThisOnly
      val failsIf =
        (coreFails ++
          Option.when(counterplayReinflationRisk == HighRisk)("surface_reinflation") ++
          Option.when(chainClaimAttempted && counterplayReinflationRisk == HighRisk)("replay_reinflation") ++
          Option.when(chainClaimAttempted && counterplayReinflationRisk == HighRisk)("whole_game_wrapper_leak"))
          .distinct
      Contract(
        strategyHypothesis = displayHypothesis(plan),
        claimScope = ClaimScope,
        primaryAxis = primaryFile.map(_.label),
        routeNodes = routeNodes,
        intermediateNodes = intermediateNodes,
        routeEdges = routeEdges,
        rerouteDenials = rerouteDenials,
        axisIndependence = axisIndependence,
        bestDefenseFound = bestDefenseFound,
        bestDefenseBranchKey = bestDefenseBranchKey,
        sameDefendedBranch = sameDefendedBranch,
        pressurePersistence = pressurePersistence,
        routeContinuity = routeContinuity,
        continuationBound = boundedContinuationVisible,
        releaseRisksRemaining = releaseRisksRemaining,
        routeNetworkMirageRisk = routeNetworkMirageRisk,
        redundantAxisRisk = redundantAxisRisk,
        postureInflationRisk = if postureInflation then HighRisk else "low",
        counterplayReinflationRisk = counterplayReinflationRisk,
        moveOrderFragility = moveOrderFragility,
        claimCertification = plan.claimCertification,
        failsIf = failsIf,
        confidence =
          confidenceScore(
            lateMiddlegameSlice = lateMiddlegameSlice,
            clearlyBetter = clearlyBetter,
            localPrereqPresent = !localPrereqMissing,
            directBestDefensePresent = directBestDefensePresent,
            axisIndependence = axisIndependence.proven,
            rerouteDenied = sameBranchRerouteResults.nonEmpty,
            routeEdgeVisible = allSameBranchEdgesVisible,
            bestReplyStable = bestReplyStable,
            boundedContinuationVisible = boundedContinuationVisible,
            sameDefendedBranch = sameDefendedBranch,
            releaseRiskCount = releaseRisksRemaining.size,
            moveOrderFragility = moveOrderFragility,
            distinctiveEnough = distinctiveEnough,
            ontologyAllowed = ontologyAllowed,
            counterplayReinflationRisk = counterplayReinflationRisk
          ),
        evidenceSources =
          (plan.hypothesis.evidenceSources ++
            supportResults.flatMap(_.purpose.flatMap(clean)) ++
            relevantPreventedPlans.flatMap(preventedEvidenceSignals))
            .distinct
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

  def certifiedSurfaceNetwork(
      ctx: NarrativeContext
  ): Option[SurfaceNetwork] =
    certifiedSurfaceNetwork(
      preventedPlans =
        ctx.semantic.toList.flatMap(_.preventedPlans).filter(_.sourceScope == FactScope.Now),
      evidenceBackedPlans = StrategicNarrativePlanSupport.evidenceBackedMainPlans(ctx)
    )

  def certifiedSurfaceNetwork(
      preventedPlans: List[PreventedPlanInfo],
      evidenceBackedPlans: List[PlanHypothesis]
  ): Option[SurfaceNetwork] =
    val scopedPreventedPlans =
      preventedPlans.filter(_.sourceScope == FactScope.Now)
    val fileAxes = scopedPreventedPlans.flatMap(surfaceFileAxis)
    val entryAxes = scopedPreventedPlans.flatMap(surfaceEntryAxis)
    val reroutes = scopedPreventedPlans.flatMap(surfaceRerouteAxis)
    val exactCandidates =
      evidenceBackedPlans
        .filter(isApplicablePlan)
        .flatMap(plan => exactSurfaceNetwork(plan, fileAxes, entryAxes, reroutes))
        .distinctBy(network => (network.file, network.entrySquare, network.intermediateSquare, network.rerouteSquare))
    exactCandidates match
      case network :: Nil => Some(network)
      case _              => None

  private def isApplicablePlan(
      plan: PlanEvidenceEvaluator.EvaluatedPlan
  ): Boolean =
    normalize(plan.themeL1) == ThemeTaxonomy.ThemeL1.RestrictionProphylaxis.id &&
      plan.subplanId.exists(id => ApplicableSubplans.contains(normalize(id)))

  private def isApplicablePlan(
      plan: PlanHypothesis
  ): Boolean =
    normalize(plan.themeL1) == ThemeTaxonomy.ThemeL1.RestrictionProphylaxis.id &&
      plan.subplanId.exists(id => ApplicableSubplans.contains(normalize(id)))

  private def primaryAxisFromContract(
      contract: LocalFileEntryBindCertification.Contract
  ): Option[FileAxisSignal] =
    contract.primaryAxis.map(axis =>
      FileAxisSignal(
        label = axis.label,
        file = axis.file,
        counterplayScoreDrop = axis.counterplayScoreDrop,
        breakNeutralizationStrength = axis.breakNeutralizationStrength,
        defensiveSufficiency = axis.defensiveSufficiency
      )
    )

  private def entryAxisFromContract(
      contract: LocalFileEntryBindCertification.Contract
  ): Option[EntryAxisSignal] =
    contract.corroboratingEntryAxis.map(axis =>
      EntryAxisSignal(
        square = axis.square,
        counterplayScoreDrop = axis.counterplayScoreDrop,
        breakNeutralizationStrength = axis.breakNeutralizationStrength,
        defensiveSufficiency = axis.defensiveSufficiency
      )
    )

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

  private def strongestIndependentReroute(
      file: String,
      entrySquare: String,
      signals: List[RerouteSignal],
      hypothesis: PlanHypothesis
  ): Option[RerouteSignal] =
    signals
      .filter(signal => independentFromFileAndEntry(file, entrySquare, signal.square))
      .sortBy(signal =>
        (
          if planMentionsSquare(hypothesis, signal.square) then 0 else 1,
          -signal.counterplayScoreDrop,
          -signal.breakNeutralizationStrength.getOrElse(0)
        )
      )
      .headOption

  private def strongestDownstreamReroute(
      entrySquare: String,
      intermediateSquare: String,
      signals: List[RerouteSignal],
      sameBranchResults: List[ProbeResult],
      hypothesis: PlanHypothesis
  ): Option[RerouteSignal] =
    signals
      .filter(signal =>
        isFinalRerouteSignal(signal) &&
          independentFromEntryAndIntermediate(entrySquare, intermediateSquare, signal.square) &&
          planMentionsSquare(hypothesis, signal.square)
      )
      .sortBy(signal =>
        (
          -edgeWitnessCount(sameBranchResults, intermediateSquare, signal.square),
          -edgeWitnessCount(sameBranchResults, entrySquare, intermediateSquare),
          -signal.counterplayScoreDrop,
          -signal.breakNeutralizationStrength.getOrElse(0)
        )
      )
      .headOption

  private def exactSurfaceNetwork(
      plan: PlanHypothesis,
      fileAxes: List[FileAxisSignal],
      entryAxes: List[EntryAxisSignal],
      reroutes: List[RerouteSignal]
  ): Option[SurfaceNetwork] =
    val intermediateAxes =
      reroutes.filter(signal =>
        isIntermediateNodeSignal(signal)
      )
    val downstreamReroutes =
      reroutes.filter(signal =>
        isFinalRerouteSignal(signal)
      )
    val exactChainCandidates =
      (
        for
          primary <- fileAxes
          entry <- entryAxes
          if independentFromFile(primary.file, entry.square)
          intermediate <- intermediateAxes
          if independentFromFileAndEntry(primary.file, entry.square, intermediate.square)
          reroute <- downstreamReroutes
          if independentFromEntryAndIntermediate(entry.square, intermediate.square, reroute.square)
          if planMentionsBroaderRouteChain(plan, Some(primary), Some(entry), Some(intermediate), Some(reroute))
        yield SurfaceNetwork(
          file = primary.label,
          entrySquare = entry.square,
          rerouteSquare = reroute.square,
          counterplayScoreDrop =
            List(
              primary.counterplayScoreDrop,
              entry.counterplayScoreDrop,
              intermediate.counterplayScoreDrop,
              reroute.counterplayScoreDrop
            ).max,
          intermediateSquare = Some(intermediate.square)
        )
      ).distinctBy(network => (network.file, network.entrySquare, network.intermediateSquare, network.rerouteSquare))
    exactChainCandidates match
      case network :: Nil => Some(network)
      case _              =>
        val exactCandidates =
          (
            for
              primary <- fileAxes
              entry <- entryAxes
              if independentFromFile(primary.file, entry.square)
              reroute <- reroutes
              if independentFromFileAndEntry(primary.file, entry.square, reroute.square)
              if planMentionsNamedRouteNetwork(plan, Some(primary), Some(entry), Some(reroute))
            yield SurfaceNetwork(
              file = primary.label,
              entrySquare = entry.square,
              rerouteSquare = reroute.square,
              counterplayScoreDrop =
                List(primary.counterplayScoreDrop, entry.counterplayScoreDrop, reroute.counterplayScoreDrop).max
            )
          ).distinctBy(network => (network.file, network.entrySquare, network.rerouteSquare))
        exactCandidates match
          case network :: Nil => Some(network)
          case _              => None

  private def buildAxisIndependence(
      primaryFile: Option[FileAxisSignal],
      entryAxis: Option[EntryAxisSignal],
      routeSignals: List[RerouteSignal],
      intermediateAxis: Option[RerouteSignal],
      rerouteAxis: Option[RerouteSignal],
      chainClaimAttempted: Boolean
  ): AxisIndependence =
    val reasons =
      (primaryFile, entryAxis) match
        case (None, _)          => List("missing_file_axis")
        case (_, None)          => List("missing_entry_axis")
        case (_, _) if routeSignals.isEmpty =>
          List("missing_reroute_axis")
        case (Some(_), Some(_)) if chainClaimAttempted && intermediateAxis.isEmpty =>
          List("missing_independent_intermediate_node")
        case (Some(_), Some(entry)) if chainClaimAttempted && rerouteAxis.isEmpty =>
          intermediateAxis match
            case Some(intermediate) =>
              val redundant =
                routeSignals.exists(signal =>
                  isFinalRerouteSignal(signal) &&
                    !independentFromEntryAndIntermediate(entry.square, intermediate.square, signal.square)
                )
              if redundant then List("downstream_reroute_restates_existing_axis")
              else List("missing_downstream_reroute_axis")
            case None           => List("missing_independent_intermediate_node")
        case (Some(primary), Some(entry)) if !chainClaimAttempted && rerouteAxis.isEmpty =>
          val redundant =
            routeSignals.exists(signal => !independentFromFileAndEntry(primary.file, entry.square, signal.square))
          if redundant then List("reroute_restates_existing_axis")
          else List("no_independent_reroute_axis")
        case _                  => Nil
    AxisIndependence(
      primaryAxis = primaryFile.map(_.label),
      entrySquare = entryAxis.map(_.square),
      rerouteSquare = rerouteAxis.map(_.square),
      proven = reasons.isEmpty,
      reasons = reasons
    )

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
        counterplayScoreDrop = plan.counterplayScoreDrop,
        breakNeutralizationStrength = plan.breakNeutralizationStrength,
        defensiveSufficiency = plan.defensiveSufficiency
      )
    }

  private def rerouteSignal(
      plan: PreventedPlan
  ): Option[RerouteSignal] =
    Option.when(
      plan.deniedResourceClass.exists(resource =>
        List("reroute_square", "reroute", "route_node", "intermediate_node").contains(normalize(resource))
      ) && plan.deniedSquares.nonEmpty
    ) {
      val square = plan.deniedSquares.map(_.key).distinct.sorted.head
      RerouteSignal(
        square = square,
        deniedResourceClass = plan.deniedResourceClass,
        deniedEntryScope = plan.deniedEntryScope,
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
        counterplayScoreDrop = plan.counterplayScoreDrop,
        breakNeutralizationStrength = None,
        defensiveSufficiency = None
      )
    }

  private def surfaceRerouteAxis(
      plan: PreventedPlanInfo
  ): Option[RerouteSignal] =
    Option.when(
      plan.deniedResourceClass.exists(resource =>
        List("reroute_square", "reroute", "route_node", "intermediate_node").contains(normalize(resource))
      ) && plan.deniedSquares.nonEmpty
    ) {
      val square = plan.deniedSquares.distinct.sorted.head
      RerouteSignal(
        square = square,
        deniedResourceClass = plan.deniedResourceClass,
        deniedEntryScope = plan.deniedEntryScope,
        counterplayScoreDrop = plan.counterplayScoreDrop,
        breakNeutralizationStrength = None,
        defensiveSufficiency = None
      )
    }

  private def toRouteNode(
      role: String
  )(
      signal: EntryAxisSignal | RerouteSignal
  ): RouteNode =
    signal match
      case entry: EntryAxisSignal =>
        RouteNode(
          role = role,
          square = entry.square,
          deniedResourceClass = Some("entry_square"),
          deniedEntryScope = Some("single_square"),
          counterplayScoreDrop = entry.counterplayScoreDrop,
          breakNeutralizationStrength = entry.breakNeutralizationStrength,
          defensiveSufficiency = entry.defensiveSufficiency
        )
      case reroute: RerouteSignal =>
        RouteNode(
          role = role,
          square = reroute.square,
          deniedResourceClass = reroute.deniedResourceClass,
          deniedEntryScope = reroute.deniedEntryScope,
          counterplayScoreDrop = reroute.counterplayScoreDrop,
          breakNeutralizationStrength = reroute.breakNeutralizationStrength,
          defensiveSufficiency = reroute.defensiveSufficiency
        )

  private def isIntermediateNodeSignal(
      signal: RerouteSignal
  ): Boolean =
    signal.deniedResourceClass.exists(resource =>
      List("route_node", "intermediate_node").contains(normalize(resource))
    )

  private def isFinalRerouteSignal(
      signal: RerouteSignal
  ): Boolean =
    signal.deniedResourceClass.exists(resource =>
      List("reroute_square", "reroute").contains(normalize(resource))
    )

  private def buildMoveOrderFragility(
      plan: PlanEvidenceEvaluator.EvaluatedPlan,
      directReplyResults: List[ProbeResult],
      bestReplyStable: Boolean,
      futureSnapshotPersistence: Boolean,
      inheritedReasons: List[String]
  ): MoveOrderFragility =
    val reasons =
      (
        inheritedReasons ++ List(
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
        ).flatten
      ).distinct
    MoveOrderFragility(
      fragile = reasons.nonEmpty,
      reasons = reasons
    )

  private def remainingReleaseRisks(
      primaryFile: Option[FileAxisSignal],
      entryAxis: Option[EntryAxisSignal],
      intermediateAxis: Option[RerouteSignal],
      rerouteAxis: Option[RerouteSignal],
      rerouteSignals: List[RerouteSignal],
      supportResults: List[ProbeResult]
  ): List[String] =
    val alternativeReroutes =
      rerouteSignals
        .filter(signal =>
          rerouteAxis.forall(_.square != signal.square) &&
            intermediateAxis.forall(_.square != signal.square)
        )
        .map(signal => s"reroute:${signal.square}")
    val sectorEscapes =
      supportResults.filter(mentionsSectorEscape).map(_ => "sector:untouched")
    val colorEscapes =
      supportResults.filter(mentionsColorComplexEscape).map(_ => "color_complex_escape")
    val alternativeFileOrEntry =
      List(
        Option.when(primaryFile.isEmpty)("file:missing"),
        Option.when(entryAxis.isEmpty)("entry:missing")
      ).flatten
    (alternativeReroutes ++ sectorEscapes ++ colorEscapes ++ alternativeFileOrEntry).distinct

  private def confidenceScore(
      lateMiddlegameSlice: Boolean,
      clearlyBetter: Boolean,
      localPrereqPresent: Boolean,
      directBestDefensePresent: Boolean,
      axisIndependence: Boolean,
      rerouteDenied: Boolean,
      routeEdgeVisible: Boolean,
      bestReplyStable: Boolean,
      boundedContinuationVisible: Boolean,
      sameDefendedBranch: Boolean,
      releaseRiskCount: Int,
      moveOrderFragility: MoveOrderFragility,
      distinctiveEnough: Boolean,
      ontologyAllowed: Boolean,
      counterplayReinflationRisk: String
  ): Double =
    val base = 0.36
    val phaseBonus = if lateMiddlegameSlice then 0.06 else 0.0
    val evalBonus = if clearlyBetter then 0.06 else 0.0
    val prereqBonus = if localPrereqPresent then 0.08 else 0.0
    val defenseBonus = if directBestDefensePresent then 0.06 else 0.0
    val axisBonus = if axisIndependence then 0.08 else 0.0
    val rerouteBonus = if rerouteDenied && routeEdgeVisible then 0.12 else 0.0
    val stabilityBonus = if bestReplyStable then 0.05 else 0.0
    val continuationBonus = if boundedContinuationVisible then 0.06 else 0.0
    val branchBonus = if sameDefendedBranch then 0.05 else 0.0
    val distinctivenessBonus =
      if distinctiveEnough && ontologyAllowed && counterplayReinflationRisk == PlannerWhyThisOnly then 0.04
      else 0.0
    val releasePenalty = math.min(0.20, releaseRiskCount * 0.10)
    val fragilityPenalty = if moveOrderFragility.fragile then 0.12 else 0.0
    val reinflationPenalty = if counterplayReinflationRisk == HighRisk then 0.12 else 0.0
    (base + phaseBonus + evalBonus + prereqBonus + defenseBonus + axisBonus + rerouteBonus + stabilityBonus + continuationBonus + branchBonus + distinctivenessBonus -
      releasePenalty - fragilityPenalty - reinflationPenalty)
      .max(0.0)
      .min(0.96)

  private def mentionsRerouteDenial(
      result: ProbeResult,
      square: String
  ): Boolean =
    resultTexts(result).exists(text => mentionsRerouteDenial(text, square))

  private def mentionsRerouteDenial(
      raw: String,
      square: String
  ): Boolean =
    val low = normalize(raw)
    low.contains(normalize(square)) &&
      DenialTokens.exists(low.contains) &&
      RouteEdgeTokens.exists(low.contains)

  private def mentionsRouteEdge(
      result: ProbeResult,
      entrySquare: String,
      rerouteSquare: String
  ): Boolean =
    resultTexts(result).exists(text => mentionsRouteEdge(text, entrySquare, rerouteSquare))

  private def mentionsRouteEdge(
      raw: String,
      entrySquare: String,
      rerouteSquare: String
  ): Boolean =
    val low = normalize(raw)
    low.contains(normalize(entrySquare)) &&
      low.contains(normalize(rerouteSquare)) &&
      RouteEdgeTokens.exists(low.contains)

  private def edgeWitnessCount(
      results: List[ProbeResult],
      from: String,
      to: String
  ): Int =
    results.count(result => mentionsRouteEdge(result, from, to))

  private def mentionsColorComplexEscape(
      result: ProbeResult
  ): Boolean =
    resultTexts(result).exists(text =>
      ColorEscapeTokens.exists(token => normalize(text).contains(token))
    )

  private def mentionsSectorEscape(
      result: ProbeResult
  ): Boolean =
    resultTexts(result).exists(text =>
      SectorEscapeTokens.exists(token => normalize(text).contains(token))
    )

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

  private def resultTexts(
      result: ProbeResult
  ): List[String] =
    result.futureSnapshot.toList.flatMap(snapshot =>
      snapshot.planPrereqsMet ++
        snapshot.planBlockersRemoved ++
        snapshot.targetsDelta.strategicAdded ++
        snapshot.targetsDelta.strategicRemoved
    ) ++ result.keyMotifs ++ result.l1Delta.flatMap(_.collapseReason).toList

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

  private def resolveBranchIdentity(
      directReplyResults: List[ProbeResult],
      localFileEntryBindCertification: Option[LocalFileEntryBindCertification.Contract]
  ): BranchIdentityResolution =
    val expectedStrongKey =
      localFileEntryBindCertification
        .flatMap(_.bestDefenseBranchKey)
        .flatMap(normalizeExistingBranchKey)
    val groupedByStrongKey =
      directReplyResults
        .flatMap(result => branchKey(result).map(_ -> result))
        .groupBy(_._1)
        .view
        .mapValues(_.map(_._2))
        .toMap
    val strongDirectKeys = groupedByStrongKey.keys.toList.sorted
    val selectedKey =
      expectedStrongKey.filter(groupedByStrongKey.contains)
        .orElse(
          Option.when(expectedStrongKey.isEmpty && strongDirectKeys.size == 1)(strongDirectKeys.head)
        )
    val selectedResults =
      selectedKey.toList.flatMap(key => groupedByStrongKey.getOrElse(key, Nil))
    val bestDefenseResult =
      selectedResults.find(hasReplyCoverage)
    val bestDefenseFound =
      if expectedStrongKey == selectedKey then
        localFileEntryBindCertification.flatMap(_.bestDefenseFound)
          .orElse(bestDefenseResult.flatMap(_.bestReplyPv.headOption.flatMap(clean)))
      else bestDefenseResult.flatMap(_.bestReplyPv.headOption.flatMap(clean))
    val sameBranchIdentityMissing =
      directReplyResults.nonEmpty && strongDirectKeys.isEmpty
    val ambiguousDefendedBranch =
      selectedKey.isEmpty &&
        (
          strongDirectKeys.size > 1 ||
            expectedStrongKey.exists(expected =>
              strongDirectKeys.nonEmpty && !strongDirectKeys.contains(expected)
            )
        )
    BranchIdentityResolution(
      selectedKey = selectedKey,
      bestDefenseResult = bestDefenseResult,
      bestDefenseFound = bestDefenseFound,
      sameBranchIdentityMissing = sameBranchIdentityMissing,
      ambiguousDefendedBranch = ambiguousDefendedBranch
    )

  private def branchKey(
      result: ProbeResult
  ): Option[String] =
    result.variationHash.flatMap(clean).map(normalize)
      .orElse(result.seedId.flatMap(clean).map(normalize))
      .orElse(branchLineKey(result.bestReplyPv))
      .orElse(
        result.replyPvs
          .flatMap(_.headOption)
          .flatMap(branchLineKey)
      )

  private def branchLineKey(
      moves: List[String]
  ): Option[String] =
    val normalizedMoves =
      moves.flatMap(normalizeUciMove).take(BranchKeyMoveCount)
    Option.when(normalizedMoves.size == BranchKeyMoveCount)(
      normalizedMoves.mkString(" ")
    )

  private def normalizeExistingBranchKey(
      raw: String
  ): Option[String] =
    clean(raw).flatMap { value =>
      val normalized = normalize(value)
      if normalized.isEmpty then None
      else if isUciMove(normalized) then None
      else
        branchLineKey(normalized.split("\\s+").toList)
          .orElse(Some(normalized))
    }

  private def planMentionsNamedRouteNetwork(
      plan: PlanHypothesis,
      primaryFile: Option[FileAxisSignal],
      entryAxis: Option[EntryAxisSignal],
      rerouteAxis: Option[RerouteSignal]
  ): Boolean =
    (primaryFile, entryAxis, rerouteAxis) match
      case (Some(primary), Some(entry), Some(reroute)) =>
        affirmativePlanTexts(plan)
          .flatMap(clean)
          .exists { text =>
            val low = normalize(text)
            low.contains(normalize(primary.label)) &&
              low.contains(normalize(entry.square)) &&
              low.contains(normalize(reroute.square)) &&
              (RouteIntentTokens.exists(low.contains) || DenialTokens.exists(low.contains))
          }
      case _ => false

  private def planMentionsNamedRouteChain(
      plan: PlanHypothesis,
      primaryFile: Option[FileAxisSignal],
      entryAxis: Option[EntryAxisSignal],
      intermediateAxis: Option[RerouteSignal],
      rerouteAxis: Option[RerouteSignal]
  ): Boolean =
    (primaryFile, entryAxis, intermediateAxis, rerouteAxis) match
      case (Some(primary), Some(entry), Some(intermediate), Some(reroute)) =>
        affirmativePlanTexts(plan)
          .flatMap(clean)
          .exists { text =>
            val low = normalize(text)
            low.contains(normalize(primary.label)) &&
              low.contains(normalize(entry.square)) &&
              low.contains(normalize(intermediate.square)) &&
              low.contains(normalize(reroute.square)) &&
              RouteIntentTokens.exists(low.contains) &&
              DenialTokens.exists(low.contains)
          }
      case _ => false

  private def planMentionsBroaderRouteChain(
      plan: PlanHypothesis,
      primaryFile: Option[FileAxisSignal],
      entryAxis: Option[EntryAxisSignal],
      intermediateAxis: Option[RerouteSignal],
      rerouteAxis: Option[RerouteSignal]
  ): Boolean =
    planMentionsNamedRouteChain(plan, primaryFile, entryAxis, intermediateAxis, rerouteAxis) ||
      (
        rerouteAxis.exists(reroute =>
          planMentionsNamedRouteNetwork(plan, primaryFile, entryAxis, intermediateAxis) &&
            planMentionsSquare(plan, reroute.square)
        )
      )

  private def mentionsRouteNetworkIntent(
      hypothesis: PlanHypothesis
  ): Boolean =
    affirmativePlanTexts(hypothesis)
      .flatMap(clean)
      .exists(text =>
        RouteIntentTokens.exists(token => normalize(text).contains(token))
      )

  private def planMentionsSquare(
      hypothesis: PlanHypothesis,
      square: String
  ): Boolean =
    affirmativePlanTexts(hypothesis)
      .flatMap(clean)
      .exists(text => normalize(text).contains(normalize(square)))

  private def affirmativePlanTexts(
      plan: PlanHypothesis
  ): List[String] =
    List(plan.planName) ++ plan.executionSteps

  private def containsInflationShell(
      hypothesis: PlanHypothesis
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

  private def preventedEvidenceSignals(
      plan: PreventedPlan
  ): List[String] =
    List(
      Option.when(plan.counterplayScoreDrop > 0)(s"counterplay_drop:${plan.counterplayScoreDrop}"),
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

  private def displayHypothesis(
      plan: PlanEvidenceEvaluator.EvaluatedPlan
  ): String =
    clean(plan.hypothesis.planName)
      .orElse(clean(plan.hypothesis.planId))
      .getOrElse("named route-network bind plan")

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

  private def queenCount(
      fen: String
  ): Int =
    Option(fen)
      .map(_.takeWhile(_ != ' '))
      .getOrElse("")
      .count(ch => ch == 'q' || ch == 'Q')

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

  private def independentFromFileAndEntry(
      file: String,
      entrySquare: String,
      square: String
  ): Boolean =
    independentFromFile(file, square) &&
      normalizeSquareLike(square) != normalizeSquareLike(entrySquare) &&
      normalizeSquareLike(square).nonEmpty

  private def independentFromEntryAndIntermediate(
      entrySquare: String,
      intermediateSquare: String,
      square: String
  ): Boolean =
    normalizeSquareLike(square) != normalizeSquareLike(entrySquare) &&
      normalizeSquareLike(square) != normalizeSquareLike(intermediateSquare) &&
      normalizeSquareLike(square).nonEmpty

  private def squareFileToken(
      raw: String
  ): String =
    "(?i)([a-h])[1-8]".r.findFirstMatchIn(Option(raw).getOrElse("")).map(_.group(1).toLowerCase).getOrElse("")

  private def normalizeUciMove(
      raw: String
  ): Option[String] =
    clean(raw).map(_.toLowerCase).filter(isUciMove)

  private def isUciMove(
      raw: String
  ): Boolean =
    "(?i)^[a-h][1-8][a-h][1-8][qrbn]?$".r.matches(Option(raw).getOrElse(""))

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
