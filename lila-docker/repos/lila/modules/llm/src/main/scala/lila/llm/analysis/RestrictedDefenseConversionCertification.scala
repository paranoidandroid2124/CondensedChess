package lila.llm.analysis

import lila.llm.model.{ FactScope, FutureSnapshot, ProbeResult }
import lila.llm.model.strategic.PreventedPlan

private[llm] object RestrictedDefenseConversionCertification:

  final case class RestrictedDefenseEvidence(
      defenderResourceCount: Int,
      moveQualityCompression: Boolean,
      counterplayScoreDrop: Int,
      preventedResourcePressure: Boolean,
      bestReplyStable: Boolean,
      futureSnapshotPersistence: Boolean
  )

  final case class RoutePersistence(
      bestDefenseStable: Boolean,
      futureSnapshotPersistent: Boolean,
      counterplayStillCompressed: Boolean,
      directBestDefensePresent: Boolean,
      sameDefendedBranch: Boolean
  )

  final case class MoveOrderFragility(
      fragile: Boolean,
      reasons: List[String]
  )

  final case class Contract(
      strategyHypothesis: String,
      restrictedDefenseEvidence: RestrictedDefenseEvidence,
      defenderResources: List[String],
      bestDefenseFound: Option[String],
      bestDefenseBranchKey: Option[String],
      routePersistence: RoutePersistence,
      failsIf: List[String],
      moveOrderFragility: MoveOrderFragility,
      confidence: Double,
      evidenceSources: List[String]
  ):
    def certified: Boolean = failsIf.isEmpty

  private val ConversionReplyPurposes =
    Set("convert_reply_multipv", "defense_reply_multipv")
  private val MinimumWinningAdvantageCp = 200
  private val RestrictedResourceCap = 2
  private val CounterplayCompressionFloor = 80

  def evaluate(
      plan: PlanEvidenceEvaluator.EvaluatedPlan,
      probeResultsById: Map[String, ProbeResult],
      preventedPlans: List[PreventedPlan],
      evalCp: Int,
      isWhiteToMove: Boolean
  ): Option[Contract] =
    Option.when(isConversionPlan(plan)) {
      val supportResults =
        plan.supportProbeIds.flatMap(probeResultsById.get).distinctBy(_.id)
      val directReplyResults =
        supportResults.filter(result =>
          result.purpose.exists(purpose =>
            ConversionReplyPurposes.contains(normalize(purpose))
          )
        )
      val defenderResources = distinctDefenderResources(directReplyResults)
      val bestDefenseFound =
        directReplyResults.iterator
          .flatMap(result => result.bestReplyPv.headOption.flatMap(clean))
          .toList
          .headOption
      val directBestDefensePresent =
        directReplyResults.nonEmpty && bestDefenseFound.nonEmpty
      val bestDefenseBranchKey =
        directReplyResults.iterator.flatMap(branchKey).toList.headOption
      val sameBranchReplyResults =
        bestDefenseBranchKey match
          case Some(branch) =>
            directReplyResults.filter(result => matchesDefendedBranch(result, branch))
          case None => Nil
      val bestReplyStable =
        directBestDefensePresent &&
          directReplyResults.forall(hasReplyCoverage) &&
          directReplyResults.forall(result =>
            result.l1Delta.flatMap(_.collapseReason).forall(reason => clean(reason).isEmpty)
          )
      val futureSnapshotPersistence =
        sameBranchReplyResults.exists(result =>
          result.futureSnapshot.exists(isPositiveFutureSnapshot)
        )
      val sameDefendedBranch =
        directBestDefensePresent && futureSnapshotPersistence
      val relevantPreventedPlans =
        preventedPlans.filter(_.sourceScope == FactScope.Now)
      val counterplayScoreDrop =
        relevantPreventedPlans.map(_.counterplayScoreDrop).maxOption.getOrElse(0)
      val preventedResourcePressure =
        relevantPreventedPlans.exists(plan =>
          plan.counterplayScoreDrop >= CounterplayCompressionFloor ||
            plan.breakNeutralized.exists(signal => clean(signal).nonEmpty) ||
            plan.deniedResourceClass.exists(resourceClass =>
              Set("break", "entry_square", "forcing_threat").contains(normalize(resourceClass))
            ) ||
            plan.defensiveSufficiency.exists(_ >= 60) ||
            plan.breakNeutralizationStrength.exists(_ >= 60)
        )
      val moveQualityCompression =
        defenderResources.nonEmpty &&
          defenderResources.size <= RestrictedResourceCap
      val counterplayStillCompressed =
        preventedResourcePressure ||
          (
            moveQualityCompression &&
              (
                counterplayScoreDrop >= CounterplayCompressionFloor ||
                  sameBranchReplyResults.exists(result =>
                    result.futureSnapshot.exists(snapshot =>
                      snapshot.resolvedThreatKinds.nonEmpty ||
                        snapshot.targetsDelta.strategicRemoved.nonEmpty ||
                        snapshot.planBlockersRemoved.nonEmpty
                    )
                  )
              )
          )
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
      val restrictedDefenseEvidence =
        RestrictedDefenseEvidence(
          defenderResourceCount = defenderResources.size,
          moveQualityCompression = moveQualityCompression,
          counterplayScoreDrop = counterplayScoreDrop,
          preventedResourcePressure = preventedResourcePressure,
          bestReplyStable = bestReplyStable,
          futureSnapshotPersistence = futureSnapshotPersistence
        )
      val routePersistence =
        RoutePersistence(
          bestDefenseStable = bestReplyStable,
          futureSnapshotPersistent = futureSnapshotPersistence,
          counterplayStillCompressed = counterplayStillCompressed,
          directBestDefensePresent = directBestDefensePresent,
          sameDefendedBranch = sameDefendedBranch
        )
      val conversionAdvantageReady =
        playerAdvantage(evalCp, isWhiteToMove) >= MinimumWinningAdvantageCp
      val distinctiveEnough =
        plan.claimCertification.attributionGrade == PlayerFacingClaimAttributionGrade.Distinctive &&
          !plan.claimCertification.alternativeDominance
      val failsIf =
        List(
          Option.when(directReplyResults.isEmpty || bestDefenseFound.isEmpty)("pv_restatement_only"),
          Option.when(
            directBestDefensePresent &&
              directReplyResults.exists(result =>
                result.futureSnapshot.exists(isPositiveFutureSnapshot)
              ) &&
              !sameDefendedBranch
          )("stitched_defended_branch"),
          Option.when(!conversionAdvantageReady)("local_to_global_overreach"),
          Option.when(!bestReplyStable)("cooperative_defense"),
          Option.when(!moveQualityCompression)("hidden_defensive_resource"),
          Option.when(!futureSnapshotPersistence)("route_persistence_missing"),
          Option.when(!counterplayStillCompressed)("insufficient_counterplay_suppression"),
          Option.when(moveOrderFragility.fragile)("move_order_fragility"),
          Option.when(!distinctiveEnough)("local_to_global_overreach")
        ).flatten.distinct
      Contract(
        strategyHypothesis = displayHypothesis(plan),
        restrictedDefenseEvidence = restrictedDefenseEvidence,
        defenderResources = defenderResources,
        bestDefenseFound = bestDefenseFound,
        bestDefenseBranchKey = bestDefenseBranchKey,
        routePersistence = routePersistence,
        failsIf = failsIf,
        moveOrderFragility = moveOrderFragility,
        confidence =
          confidenceScore(
            conversionAdvantageReady = conversionAdvantageReady,
            directBestDefensePresent = directBestDefensePresent,
            bestReplyStable = bestReplyStable,
            futureSnapshotPersistence = futureSnapshotPersistence,
            sameDefendedBranch = sameDefendedBranch,
            moveQualityCompression = moveQualityCompression,
            counterplayStillCompressed = counterplayStillCompressed,
            distinctiveEnough = distinctiveEnough,
            moveOrderFragility = moveOrderFragility,
            directReplyResultCount = directReplyResults.size,
            preventedSignalCount = relevantPreventedPlans.size
          ),
        evidenceSources =
          (plan.hypothesis.evidenceSources ++
            directReplyResults.flatMap(_.purpose.flatMap(clean)) ++
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

  private def isConversionPlan(
      plan: PlanEvidenceEvaluator.EvaluatedPlan
  ): Boolean =
    normalize(plan.themeL1) == ThemeTaxonomy.ThemeL1.AdvantageTransformation.id

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

  private def displayHypothesis(
      plan: PlanEvidenceEvaluator.EvaluatedPlan
  ): String =
    clean(plan.hypothesis.planName)
      .orElse(clean(plan.hypothesis.planId))
      .getOrElse("conversion plan")

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

  private def isPositiveFutureSnapshot(
      snapshot: FutureSnapshot
  ): Boolean =
    snapshot.planPrereqsMet.nonEmpty ||
      snapshot.planBlockersRemoved.nonEmpty ||
      snapshot.targetsDelta.strategicAdded.nonEmpty ||
      snapshot.resolvedThreatKinds.nonEmpty

  private def preventedEvidenceSignals(
      plan: PreventedPlan
  ): List[String] =
    List(
      Option.when(plan.counterplayScoreDrop > 0)(
        s"counterplay_drop:${plan.counterplayScoreDrop}"
      ),
      plan.breakNeutralized.flatMap(signal => clean(signal).map(value => s"neutralized_break:$value")),
      plan.deniedResourceClass.flatMap(resourceClass => clean(resourceClass).map(value => s"denied_resource:$value"))
    ).flatten

  private def confidenceScore(
      conversionAdvantageReady: Boolean,
      directBestDefensePresent: Boolean,
      bestReplyStable: Boolean,
      futureSnapshotPersistence: Boolean,
      sameDefendedBranch: Boolean,
      moveQualityCompression: Boolean,
      counterplayStillCompressed: Boolean,
      distinctiveEnough: Boolean,
      moveOrderFragility: MoveOrderFragility,
      directReplyResultCount: Int,
      preventedSignalCount: Int
  ): Double =
    val signalCount =
      List(
        conversionAdvantageReady,
        directBestDefensePresent,
        bestReplyStable,
        futureSnapshotPersistence,
        sameDefendedBranch,
        moveQualityCompression,
        counterplayStillCompressed,
        distinctiveEnough,
        !moveOrderFragility.fragile
      ).count(identity)
    val base = 0.34 + signalCount * 0.08
    val replyBonus = math.min(0.12, directReplyResultCount * 0.03)
    val preventionBonus = math.min(0.05, preventedSignalCount * 0.02)
    val fragilityPenalty = if moveOrderFragility.fragile then 0.08 else 0.0
    (base + replyBonus + preventionBonus - fragilityPenalty).max(0.05).min(0.97)

  private def branchKey(
      result: ProbeResult
  ): Option[String] =
    branchLineKey(result.bestReplyPv)
      .orElse {
        result.replyPvs.toList
          .flatten
          .flatMap(branchLineKey)
          .headOption
      }

  private def branchLineKey(
      moves: List[String]
  ): Option[String] =
    Option.when(moves.nonEmpty)(moves.flatMap(clean).mkString(" "))

  private def matchesDefendedBranch(
      result: ProbeResult,
      expectedBranchKey: String
  ): Boolean =
    branchKey(result).contains(expectedBranchKey) ||
      result.replyPvs.toList.flatten.exists(line =>
        branchLineKey(line).contains(expectedBranchKey)
      )

  private def clean(raw: String): Option[String] =
    Option(raw).map(_.trim).filter(_.nonEmpty)

  private def normalize(raw: String): String =
    clean(raw).map(_.toLowerCase).getOrElse("")
