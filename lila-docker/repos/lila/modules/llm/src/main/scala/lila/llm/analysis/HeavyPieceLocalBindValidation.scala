package lila.llm.analysis

import chess.*
import chess.format.{ Fen, Uci }

import lila.llm.analysis.ThemeTaxonomy.SubplanId
import lila.llm.model.{ FactScope, FutureSnapshot, NarrativeContext, PreventedPlanInfo, ProbeResult }
import lila.llm.model.authoring.PlanHypothesis
import lila.llm.model.strategic.PreventedPlan

private[llm] object HeavyPieceLocalBindValidation:

  private[analysis] final case class ExactBranchMove(
      uci: String,
      mover: Color,
      piece: Role,
      from: Square,
      to: Square,
      givesCheck: Boolean,
      capturedRole: Option[Role]
  )

  private[analysis] final case class ExactBranchReplay(
      startFen: String,
      requestedMoves: List[String],
      replayedMoves: List[ExactBranchMove],
      complete: Boolean,
      stopReason: Option[String],
      features: List[String]
  ):
    def replayedUci: List[String] = replayedMoves.map(_.uci)

  final case class RouteContinuity(
      directBestDefensePresent: Boolean,
      bestDefenseStable: Boolean,
      futureSnapshotPersistent: Boolean,
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
      axisFamilies: List[String],
      bestDefenseFound: Option[String],
      bestDefenseBranchKey: Option[String],
      sameDefendedBranch: Boolean,
      pressurePersistence: Boolean,
      routeContinuity: RouteContinuity,
      heavyPieceReleaseInventory: List[String],
      perpetualRisk: Boolean,
      bestDefenseReleaseSurvivors: List[String],
      tacticalReleaseDensity: Int,
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
  private val ClaimScope = "heavy_piece_local_bind"
  private val HighReinflationRisk = "high"
  private val NegativeOnlyLane = "negative_only_lane"
  private val LateMiddlegamePlyFloor = 20
  private val ClearlyBetterAdvantageCp = 150
  private val RestrictedResourceCap = 2
  private val HeavyPieceCountFloor = 4
  private val QueenCountFloor = 2
  private val ProofEligibleReplayPlies = 3
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
  private val InflationTokens =
    List(
      "no counterplay",
      "completely tied",
      "completely bound",
      "completely shut down",
      "wins by force",
      "winning route",
      "winning plan",
      "totally squeezed",
      "whole position",
      "bind"
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
    Option.when(
      isApplicablePlan(plan) &&
        heavyPieceSlice(phase, ply, fen) &&
        playerAdvantage(evalCp, isWhiteToMove) >= ClearlyBetterAdvantageCp &&
        fileAxisSignals.nonEmpty &&
        entryAxisSignals.nonEmpty
    ) {
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
            result.bestReplyPv.flatMap(clean).nonEmpty &&
            branchKey(result).nonEmpty
        )
      val bestDefenseReplay =
        bestDefenseResult.flatMap(result =>
          replayBranchLine(
            baseFen = resultBaseFen(result, fen),
            line = result.bestReplyPv
          )
        )
      val bestDefenseFound =
        bestDefenseResult.flatMap(displayBestDefense)
      val bestDefenseBranchKey =
        bestDefenseResult.flatMap(branchKey)
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
      val directBestDefensePresent =
        directReplyResults.nonEmpty &&
          bestDefenseFound.nonEmpty &&
          bestDefenseBranchKey.nonEmpty &&
          bestDefenseReplay.exists(_.complete)
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
        primaryFile.flatMap(primary => strongestIndependentEntry(primary, entryAxisSignals))
      val fileRouteLossVisible =
        primaryFile.exists(primary =>
          sameBranchPersistenceResults.exists(result => mentionsFileDenial(result, primary.file))
        )
      val entryAxisPersistence =
        corroboratingEntry.exists(entry =>
          sameBranchPersistenceResults.exists(result => mentionsEntryDenial(result, entry.square))
        )
      val futureSnapshotPersistence =
        sameBranchValidationResults.nonEmpty && fileRouteLossVisible && entryAxisPersistence
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
          fileRouteLossVisible &&
          entryAxisPersistence &&
          sameDefendedBranch
      val routeContinuity =
        RouteContinuity(
          directBestDefensePresent = directBestDefensePresent,
          bestDefenseStable = bestReplyStable,
          futureSnapshotPersistent = futureSnapshotPersistence,
          boundedContinuationVisible = boundedContinuationVisible,
          sameDefendedBranch = sameDefendedBranch
        )
      val heavyPieceReleaseInventory =
        collectHeavyPieceReleaseInventory(
          results = directReplyResults ++ sameBranchContinuityResults,
          defaultFen = fen
        )
      val bestDefenseReleaseSurvivors =
        collectBestDefenseReleaseSurvivors(
          results = bestDefenseResult.toList ++ sameBranchValidationResults,
          defaultFen = fen
        )
      val perpetualRisk =
        (heavyPieceReleaseInventory ++ bestDefenseReleaseSurvivors).exists(release =>
          release == "perpetual_check" || release == "forcing_checks"
        )
      val tacticalReleaseDensity =
        (heavyPieceReleaseInventory ++ bestDefenseReleaseSurvivors).distinct.size
      val releaseRisksRemaining =
        remainingReleaseRisks(
          primaryFile = primaryFile,
          corroboratingEntry = corroboratingEntry,
          fileAxisSignals = fileAxisSignals,
          entryAxisSignals = entryAxisSignals
        )
      val fileOccupancyOnlyRisk =
        primaryFile.isEmpty || !fileRouteLossVisible
      val fortressRisk =
        pressurePersistence && !boundedContinuationVisible
      val moveOrderFragility =
        buildMoveOrderFragility(plan, directReplyResults, bestReplyStable, futureSnapshotPersistence)
      val distinctiveEnough =
        plan.claimCertification.attributionGrade == PlayerFacingClaimAttributionGrade.Distinctive &&
          !plan.claimCertification.alternativeDominance
      val ontologyAllowed =
        Set(
          PlayerFacingClaimOntologyFamily.RouteDenial,
          PlayerFacingClaimOntologyFamily.LongTermRestraint
        ).contains(plan.claimCertification.ontologyFamily)
      val coreFails =
        List(
          Option.when(!directBestDefensePresent)("direct_best_defense_missing"),
          Option.when(validationResults.isEmpty)("engine_pv_paraphrase"),
          Option.when(fileOccupancyOnlyRisk)("pressure_only_waiting_move"),
          Option.when(releaseRisksRemaining.nonEmpty)("hidden_off_sector_break"),
          Option.when(
            (heavyPieceReleaseInventory ++ bestDefenseReleaseSurvivors).nonEmpty
          )("heavy_piece_release_illusion"),
          Option.when(
            directBestDefensePresent &&
              validationResults.nonEmpty &&
              !sameDefendedBranch
          )("stitched_heavy_piece_bundle"),
          Option.when(moveOrderFragility.fragile)("move_order_fragility"),
          Option.when(fortressRisk)("fortress_like_but_not_progressing")
        ).flatten.distinct
      val counterplayReinflationRisk =
        if coreFails.nonEmpty ||
          !distinctiveEnough ||
          !ontologyAllowed ||
          containsInflationShell(plan.hypothesis) ||
          defenderResources.size > RestrictedResourceCap
        then HighReinflationRisk
        else NegativeOnlyLane
      val failsIf =
        (coreFails ++
          Option.when(counterplayReinflationRisk == HighReinflationRisk)("surface_reinflation"))
          .distinct
      Contract(
        strategyHypothesis = displayHypothesis(plan),
        claimScope = ClaimScope,
        axisFamilies =
          List(
            primaryFile.map(_.label),
            corroboratingEntry.map(_.square)
          ).flatten,
        bestDefenseFound = bestDefenseFound,
        bestDefenseBranchKey = bestDefenseBranchKey,
        sameDefendedBranch = sameDefendedBranch,
        pressurePersistence = pressurePersistence,
        routeContinuity = routeContinuity,
        heavyPieceReleaseInventory = heavyPieceReleaseInventory,
        perpetualRisk = perpetualRisk,
        bestDefenseReleaseSurvivors = bestDefenseReleaseSurvivors,
        tacticalReleaseDensity = tacticalReleaseDensity,
        releaseRisksRemaining = releaseRisksRemaining,
        fileOccupancyOnlyRisk = fileOccupancyOnlyRisk,
        fortressRisk = fortressRisk,
        counterplayReinflationRisk = counterplayReinflationRisk,
        moveOrderFragility = moveOrderFragility,
        claimCertification = plan.claimCertification,
        failsIf = failsIf,
        confidence =
          confidenceScore(
            directBestDefensePresent = directBestDefensePresent,
            fileRouteLossVisible = fileRouteLossVisible,
            entryAxisPersistence = entryAxisPersistence,
            bestReplyStable = bestReplyStable,
            futureSnapshotPersistence = futureSnapshotPersistence,
            boundedContinuationVisible = boundedContinuationVisible,
            sameDefendedBranch = sameDefendedBranch,
            tacticalReleaseDensity = tacticalReleaseDensity,
            releaseRiskCount = releaseRisksRemaining.size,
            moveOrderFragility = moveOrderFragility,
            distinctiveEnough = distinctiveEnough,
            ontologyAllowed = ontologyAllowed,
            counterplayReinflationRisk = counterplayReinflationRisk,
            fortressRisk = fortressRisk,
            fileOccupancyOnlyRisk = fileOccupancyOnlyRisk
          ),
        evidenceSources =
          (plan.hypothesis.evidenceSources ++
            supportResults.flatMap(_.purpose.flatMap(clean)) ++
            relevantPreventedPlans.flatMap(preventedEvidenceSignals) ++
            bestDefenseReplay.toList.flatMap(replayEvidenceSignals) ++
            supportResults.flatMap(result =>
              replayBranchLine(resultBaseFen(result, fen), result.bestReplyPv).toList.flatMap(replayEvidenceSignals)
            ))
            .distinct
      )
    }

  def playerFacingEvidenceTier(
      baseTier: String,
      contract: Option[Contract]
  ): String =
    contract match
      case Some(_) if normalize(baseTier) == "evidence_backed" =>
        "deferred"
      case _ => baseTier

  def blocksPlayerFacingShell(
      ctx: NarrativeContext
  ): Boolean =
    val preventedPlans =
      ctx.semantic.toList.flatMap(_.preventedPlans).filter(_.sourceScope == FactScope.Now)
    blocksPlayerFacingShell(
      fen = ctx.fen,
      phase = ctx.phase.current,
      ply = ctx.ply,
      preventedPlans = preventedPlans,
      mainPlans = ctx.mainStrategicPlans,
      experiments = ctx.strategicPlanExperiments
    )

  private def blocksPlayerFacingShell(
      fen: String,
      phase: String,
      ply: Int,
      preventedPlans: List[PreventedPlanInfo],
      mainPlans: List[PlanHypothesis],
      experiments: List[lila.llm.model.StrategicPlanExperiment]
  ): Boolean =
    heavyPieceSlice(phase, ply, fen) &&
      hasApplicableSurfacePair(preventedPlans) &&
      (
        mainPlans.exists(isApplicablePlan) ||
          experiments.exists(isApplicableExperiment)
      )

  private def hasApplicableSurfacePair(
      preventedPlans: List[PreventedPlanInfo]
  ): Boolean =
    val fileAxes =
      preventedPlans.flatMap(surfaceFileAxis)
    val entryAxes =
      preventedPlans.flatMap(surfaceEntryAxis)
    strongestFileAxis(fileAxes).exists(primary =>
      strongestIndependentEntry(primary, entryAxes).nonEmpty
    )

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

  private def isApplicableExperiment(
      experiment: lila.llm.model.StrategicPlanExperiment
  ): Boolean =
    normalize(experiment.themeL1) == ThemeTaxonomy.ThemeL1.RestrictionProphylaxis.id &&
      experiment.subplanId.exists(id => ApplicableSubplans.contains(normalize(id)))

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

  private def collectHeavyPieceReleaseInventory(
      results: List[ProbeResult],
      defaultFen: String
  ): List[String] =
    collectReplayFeatures(
      results = results,
      defaultFen = defaultFen,
      lines = branchLines
    )

  private def collectBestDefenseReleaseSurvivors(
      results: List[ProbeResult],
      defaultFen: String
  ): List[String] =
    collectReplayFeatures(
      results = results,
      defaultFen = defaultFen,
      lines = bestDefenseLines
    )

  private def collectReplayFeatures(
      results: List[ProbeResult],
      defaultFen: String,
      lines: ProbeResult => List[List[String]]
  ): List[String] =
    val replays =
      results.flatMap(result =>
        replayBranchLines(
          baseFen = resultBaseFen(result, defaultFen),
          lines = lines(result)
        )
      )
    val replayFeatures =
      replays.flatMap(_.features).distinct
    replayFeatures

  private def replayBranchLines(
      baseFen: String,
      lines: List[List[String]]
  ): List[ExactBranchReplay] =
    lines.flatMap(line => replayBranchLine(baseFen, line))

  private[analysis] def replayBranchLine(
      baseFen: String,
      line: List[String]
  ): Option[ExactBranchReplay] =
    clean(baseFen).flatMap(fen =>
      Fen.read(chess.variant.Standard, Fen.Full(fen)).map { start =>
        val requestedMoves =
          line.flatMap(clean)
        val replayed = scala.collection.mutable.ListBuffer.empty[ExactBranchMove]
        var current = start
        var stopReason: Option[String] = None
        val it = requestedMoves.iterator
        while it.hasNext && stopReason.isEmpty do
          val raw = it.next()
          clean(raw).flatMap(Uci.apply).collect { case move: Uci.Move => move } match
            case None =>
              stopReason = Some(s"invalid_uci:${Option(raw).getOrElse("")}")
            case Some(uciMove) =>
              current.move(uciMove).toOption match
                case None =>
                  stopReason = Some(s"illegal_move:${uciMove.uci}")
                case Some(applied) =>
                  val capturedRole =
                    applied.capture
                      .flatMap(current.board.roleAt)
                      .orElse(current.board.roleAt(applied.dest))
                  replayed += ExactBranchMove(
                    uci = uciMove.uci,
                    mover = current.color,
                    piece = applied.piece.role,
                    from = applied.orig,
                    to = applied.dest,
                    givesCheck = applied.after.check.yes,
                    capturedRole = capturedRole
                  )
                  current = applied.after
        val complete =
          stopReason.isEmpty && replayed.size == requestedMoves.size
        val exactFeatures =
          Option.when(complete && replayed.size >= ProofEligibleReplayPlies) {
            inferReleaseFeatures(replayed.toList)
          }.getOrElse(Nil)
        ExactBranchReplay(
          startFen = fen,
          requestedMoves = requestedMoves,
          replayedMoves = replayed.toList,
          complete = complete,
          stopReason = stopReason,
          features = exactFeatures
        )
      }
    )

  private def replayEvidenceSignals(
      replay: ExactBranchReplay
  ): List[String] =
    List(
      Option.when(replay.complete)("exact_branch_replay"),
      replay.stopReason.flatMap(reason => clean(s"branch_replay_stop:$reason"))
    ).flatten ++ replay.features.map(feature => s"branch_feature:$feature")

  private def inferReleaseFeatures(
      replayedMoves: List[ExactBranchMove]
  ): List[String] =
    val queenInfiltration =
      replayedMoves.exists(isQueenInfiltration)
    val rookLift =
      replayedMoves.exists(isRookLiftOrSwitch)
    val heavyPieceCheckingMoves =
      replayedMoves.filter(move =>
        (move.piece == Queen || move.piece == Rook) && move.givesCheck
      )
    val checkBursts =
      heavyPieceCheckingMoves
        .groupBy(_.mover)
        .values
        .map(_.size)
        .maxOption
        .getOrElse(0)
    val forcingChecks =
      checkBursts >= 2
    val perpetualLike =
      checkBursts >= 3
    val exchangeSac =
      replayedMoves.exists(isExchangeSacrificeRelease)
    List(
      Option.when(queenInfiltration)("queen_infiltration"),
      Option.when(rookLift)("rook_lift"),
      Option.when(perpetualLike)("perpetual_check"),
      Option.when(forcingChecks)("forcing_checks"),
      Option.when(exchangeSac)("exchange_sac_release")
    ).flatten.distinct

  private def isQueenInfiltration(
      move: ExactBranchMove
  ): Boolean =
    val enemyTerritory =
      if move.mover.white then move.to.rank.value >= 4
      else move.to.rank.value <= 3
    val deepAdvance =
      (move.from.rank.value - move.to.rank.value).abs >= 2
    move.piece == Queen && enemyTerritory && (deepAdvance || move.givesCheck)

  private def isRookLiftOrSwitch(
      move: ExactBranchMove
  ): Boolean =
    val fromRank = move.from.rank.value
    val toRank = move.to.rank.value
    val fromFile = move.from.file.value
    val toFile = move.to.file.value
    val backRank = if move.mover.white then chess.Rank.First else chess.Rank.Eighth
    val leavesBackRank =
      move.from.rank == backRank && move.to.rank != backRank
    val lateralSwitch =
      fromRank == toRank && fromFile != toFile
    move.piece == Rook && (leavesBackRank || lateralSwitch)

  private def isExchangeSacrificeRelease(
      move: ExactBranchMove
  ): Boolean =
    move.piece == Rook &&
      move.capturedRole.exists(captured => pieceValueCp(captured) > 0 && pieceValueCp(captured) < pieceValueCp(Rook))

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

  private def buildMoveOrderFragility(
      plan: PlanEvidenceEvaluator.EvaluatedPlan,
      directReplyResults: List[ProbeResult],
      bestReplyStable: Boolean,
      futureSnapshotPersistence: Boolean
  ): MoveOrderFragility =
    val reasons =
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
      fragile = reasons.nonEmpty,
      reasons = reasons
    )

  private def confidenceScore(
      directBestDefensePresent: Boolean,
      fileRouteLossVisible: Boolean,
      entryAxisPersistence: Boolean,
      bestReplyStable: Boolean,
      futureSnapshotPersistence: Boolean,
      boundedContinuationVisible: Boolean,
      sameDefendedBranch: Boolean,
      tacticalReleaseDensity: Int,
      releaseRiskCount: Int,
      moveOrderFragility: MoveOrderFragility,
      distinctiveEnough: Boolean,
      ontologyAllowed: Boolean,
      counterplayReinflationRisk: String,
      fortressRisk: Boolean,
      fileOccupancyOnlyRisk: Boolean
  ): Double =
    val base = 0.34
    val bestDefenseBonus = if directBestDefensePresent then 0.08 else 0.0
    val fileBonus = if fileRouteLossVisible then 0.08 else 0.0
    val entryBonus = if entryAxisPersistence then 0.07 else 0.0
    val replyBonus = if bestReplyStable then 0.07 else 0.0
    val futureBonus = if futureSnapshotPersistence then 0.06 else 0.0
    val continuationBonus = if boundedContinuationVisible then 0.06 else 0.0
    val branchBonus = if sameDefendedBranch then 0.05 else 0.0
    val distinctivenessBonus = if distinctiveEnough && ontologyAllowed then 0.04 else 0.0
    val releasePenalty = math.min(0.24, tacticalReleaseDensity * 0.08)
    val offSectorPenalty = math.min(0.18, releaseRiskCount * 0.09)
    val fragilityPenalty = if moveOrderFragility.fragile then 0.12 else 0.0
    val reinflationPenalty = if counterplayReinflationRisk == HighReinflationRisk then 0.10 else 0.0
    val fortressPenalty = if fortressRisk then 0.10 else 0.0
    val pressurePenalty = if fileOccupancyOnlyRisk then 0.10 else 0.0
    (base + bestDefenseBonus + fileBonus + entryBonus + replyBonus + futureBonus + continuationBonus +
      branchBonus + distinctivenessBonus -
      releasePenalty - offSectorPenalty - fragilityPenalty - reinflationPenalty - fortressPenalty - pressurePenalty)
      .max(0.0)
      .min(0.94)

  private def displayBestDefense(
      result: ProbeResult
  ): Option[String] =
    Option.when(result.bestReplyPv.flatMap(clean).nonEmpty)(
      result.bestReplyPv.flatMap(clean).mkString(" ")
    )

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
    fileMention(low, file) &&
      List("closed", "shut", "denied", "unavailable", "sealed", "counterplay route", "entry").exists(low.contains)

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
          result.replyPvs.toList.flatten.flatMap(_.headOption.flatMap(clean))
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

  private def heavyPieceSlice(
      phase: String,
      ply: Int,
      fen: String
  ): Boolean =
    normalize(phase) == "middlegame" &&
      ply >= LateMiddlegamePlyFloor &&
      heavyPieceCount(fen) >= HeavyPieceCountFloor &&
      queenCount(fen) >= QueenCountFloor

  private def heavyPieceCount(
      fen: String
  ): Int =
    boardSection(fen).count(ch => ch == 'q' || ch == 'Q' || ch == 'r' || ch == 'R')

  private def queenCount(
      fen: String
  ): Int =
    boardSection(fen).count(ch => ch == 'q' || ch == 'Q')

  private def boardSection(
      fen: String
  ): String =
    Option(fen)
      .map(_.takeWhile(_ != ' '))
      .getOrElse("")

  private def displayHypothesis(
      plan: PlanEvidenceEvaluator.EvaluatedPlan
  ): String =
    clean(plan.hypothesis.planName)
      .orElse(clean(plan.hypothesis.planId))
      .getOrElse("heavy-piece local bind")

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
      Option.when(plan.counterplayScoreDrop > 0)(s"counterplay_drop:${plan.counterplayScoreDrop}"),
      plan.breakNeutralized.flatMap(signal => clean(signal).map(value => s"neutralized_break:$value")),
      Option.when(plan.deniedSquares.nonEmpty)(
        s"denied_squares:${plan.deniedSquares.map(_.key).distinct.sorted.mkString(",")}"
      ),
      plan.deniedResourceClass.flatMap(resource => clean(resource).map(value => s"denied_resource:$value")),
      plan.deniedEntryScope.flatMap(scope => clean(scope).map(value => s"denied_entry_scope:$value"))
    ).flatten

  private def containsInflationShell(
      hypothesis: PlanHypothesis
  ): Boolean =
    (List(hypothesis.planName) ++ hypothesis.preconditions ++ hypothesis.executionSteps ++
      hypothesis.failureModes ++ hypothesis.refutation.toList)
      .flatMap(clean)
      .exists(text => InflationTokens.exists(token => normalize(text).contains(token)))

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

  private def branchLines(
      result: ProbeResult
  ): List[List[String]] =
    (result.bestReplyPv :: result.replyPvs.toList.flatten).filter(_.nonEmpty).distinct

  private def bestDefenseLines(
      result: ProbeResult
  ): List[List[String]] =
    List(result.bestReplyPv).filter(_.nonEmpty)

  private def resultBaseFen(
      result: ProbeResult,
      defaultFen: String
  ): String =
    clean(result.fen.getOrElse(defaultFen)).getOrElse(defaultFen)

  private def pieceValueCp(
      role: Role
  ): Int = role match
    case Pawn   => 100
    case Knight => 300
    case Bishop => 300
    case Rook   => 500
    case Queen  => 900
    case _      => 0
