package lila.llm.analysis

import chess.{ Bishop, Board, Color, Knight, Pawn, Queen, Rook, Role }
import chess.format.{ Fen, Uci }
import lila.llm.{ GameChronicleMoment, NarrativeSignalDigest, StrategyDirectionalTarget, StrategyPack, StrategyPieceMoveRef, StrategyPieceRoute }
import lila.llm.model.*

private[llm] enum DecisiveTruthClass:
  case Best
  case Acceptable
  case Inaccuracy
  case Mistake
  case Blunder
  case MissedWin
  case WinningInvestment
  case CompensatedInvestment

private[llm] enum DecisiveReasonFamily:
  case TacticalRefutation
  case Conversion
  case InvestmentSacrifice
  case OnlyMoveDefense
  case MissedWin
  case QuietTechnicalMove

private[llm] enum InvestmentTruthPhase:
  case FirstInvestmentCommitment
  case CompensationMaintenance
  case ConversionFollowthrough

private[llm] enum TruthOwnershipRole:
  case CommitmentOwner
  case MaintenanceEcho
  case ConversionOwner
  case BlunderOwner
  case NoneRole

private[llm] enum TruthVisibilityRole:
  case PrimaryVisible
  case SupportingVisible
  case Hidden

private[llm] enum TruthSurfaceMode:
  case InvestmentExplain
  case MaintenancePreserve
  case ConversionExplain
  case FailureExplain
  case Neutral

private[llm] enum TruthExemplarRole:
  case VerifiedExemplar
  case ProvisionalExemplar
  case NonExemplar

private[llm] enum CommitmentEvidenceProvenance:
  case CurrentMaterial
  case CurrentSemantic
  case AfterSemantic
  case LegacyShell

private[llm] enum FailureInterpretationMode:
  case TacticalRefutation
  case OnlyMoveFailure
  case QuietPositionalCollapse
  case SpeculativeInvestmentFailed
  case NoClearPlan

private[llm] enum MoveQualityVerdict:
  case Best
  case Acceptable
  case Inaccuracy
  case Mistake
  case Blunder
  case MissedWin

private[llm] final case class MoveQualityFact(
    verdict: MoveQualityVerdict,
    cpLoss: Int,
    swingSeverity: Int,
    winPercentBefore: Double,
    winPercentAfter: Double,
    winPercentLoss: Double,
    severityBand: String
):
  def isBad: Boolean =
    verdict match
      case MoveQualityVerdict.Inaccuracy | MoveQualityVerdict.Mistake | MoveQualityVerdict.Blunder |
          MoveQualityVerdict.MissedWin =>
        true
      case _ => false

  def baselineTruthClass: DecisiveTruthClass =
    verdict match
      case MoveQualityVerdict.Best       => DecisiveTruthClass.Best
      case MoveQualityVerdict.Acceptable => DecisiveTruthClass.Acceptable
      case MoveQualityVerdict.Inaccuracy => DecisiveTruthClass.Inaccuracy
      case MoveQualityVerdict.Mistake    => DecisiveTruthClass.Mistake
      case MoveQualityVerdict.Blunder    => DecisiveTruthClass.Blunder
      case MoveQualityVerdict.MissedWin  => DecisiveTruthClass.MissedWin

private[llm] final case class BenchmarkFact(
    verifiedBestMove: Option[String],
    chosenMatchesBest: Boolean,
    onlyMove: Boolean,
    uniqueGoodMove: Boolean,
    benchmarkNamingAllowed: Boolean,
    alternativeCount: Int,
    verificationTier: String
)

private[llm] final case class TacticalFact(
    immediateRefutation: Boolean,
    forcingLine: Boolean,
    forcedMate: Boolean,
    forcedDrawResource: Boolean,
    motifs: List[String],
    proofLine: List[String]
)

private[llm] final case class MaterialEconomicsFact(
    investedMaterialCp: Option[Int],
    beforeDeficit: Int,
    afterDeficit: Int,
    movingPieceValue: Int,
    capturedPieceValue: Int,
    sacrificeKind: Option[String],
    valueDownCapture: Boolean,
    recoversDeficit: Boolean,
    overinvestment: Boolean,
    uncompensatedLoss: Boolean,
    forcedRecovery: Boolean
):
  def deficitDelta: Int = afterDeficit - beforeDeficit
  def increasesDeficit: Boolean = deficitDelta >= 75

private[llm] final case class FreshCommitmentEvidence(
    moveLocalPayoffAnchor: Option[String],
    verifiedPayoffAnchor: Option[String],
    provenance: Set[CommitmentEvidenceProvenance],
    currentMaterialSeed: Boolean,
    seedEligible: Boolean,
    ownerEligible: Boolean,
    legacyVisibleOnly: Boolean,
    currentConcreteCarrier: Boolean,
    currentSemanticSupport: Boolean,
    afterSemanticSupport: Boolean,
    legacyShellSupport: Boolean
):
  def currentMoveEvidence: Boolean =
    currentMaterialSeed || currentSemanticSupport || currentConcreteCarrier

private[llm] final case class StrategicOwnershipFact(
    truthPhase: Option[InvestmentTruthPhase],
    reasonFamily: DecisiveReasonFamily,
    benchmarkCriticalMove: Boolean,
    verifiedPayoffAnchor: Option[String],
    chainKey: Option[String],
    evidenceProvenance: Set[CommitmentEvidenceProvenance],
    createsFreshInvestment: Boolean,
    maintainsInvestment: Boolean,
    convertsInvestment: Boolean,
    durablePressure: Boolean,
    currentMoveEvidence: Boolean,
    currentConcreteCarrier: Boolean,
    currentSemanticAnchorMatch: Boolean,
    currentCarrierAnchorMatch: Boolean,
    freshCommitmentCandidate: Boolean,
    freshCurrentInvestmentEvidence: Boolean,
    ownerEligible: Boolean,
    legacyVisibleOnly: Boolean,
    maintenancePressureQualified: Boolean,
    criticalMaintenance: Boolean,
    maintenanceExemplarCandidate: Boolean
)

private[llm] final case class PunishConversionFact(
    immediatePunishment: Boolean,
    latentPunishment: Boolean,
    conversionRoute: Option[String],
    concessionSummary: Option[String]
)

private[llm] final case class FailureInterpretationFact(
    failureMode: FailureInterpretationMode,
    intentConfidence: Double,
    intentAnchor: Option[String],
    interpretationAllowed: Boolean
)

private[llm] final case class DifficultyNoveltyFact(
    onlyMoveDefense: Boolean,
    uniqueGoodMove: Boolean,
    depthSensitive: Boolean,
    shallowUnderestimated: Boolean,
    verificationTier: String
)

private[llm] final case class MoveTruthFrame(
    playedMove: Option[String],
    verifiedBestMove: Option[String],
    moveQuality: MoveQualityFact,
    benchmark: BenchmarkFact,
    tactical: TacticalFact,
    materialEconomics: MaterialEconomicsFact,
    strategicOwnership: StrategicOwnershipFact,
    punishConversion: PunishConversionFact,
    failureInterpretation: FailureInterpretationFact,
    difficultyNovelty: DifficultyNoveltyFact,
    truthClass: DecisiveTruthClass,
    ownershipRole: TruthOwnershipRole,
    visibilityRole: TruthVisibilityRole,
    surfaceMode: TruthSurfaceMode,
    exemplarRole: TruthExemplarRole,
    surfacedMoveOwnsTruth: Boolean,
    compensationProseAllowed: Boolean,
    benchmarkProseAllowed: Boolean
)

private[llm] final case class MomentTruthProjection(
    classificationKey: String,
    ownershipRole: TruthOwnershipRole,
    visibilityRole: TruthVisibilityRole,
    surfaceMode: TruthSurfaceMode,
    exemplarRole: TruthExemplarRole,
    surfacedMoveOwnsTruth: Boolean,
    verifiedPayoffAnchor: Option[String],
    benchmarkProseAllowed: Boolean,
    chainKey: Option[String],
    maintenanceExemplarCandidate: Boolean,
    benchmarkCriticalMove: Boolean = false
)

private[llm] final case class DecisiveTruthContract(
    playedMove: Option[String],
    verifiedBestMove: Option[String],
    truthClass: DecisiveTruthClass,
    cpLoss: Int,
    swingSeverity: Int,
    reasonFamily: DecisiveReasonFamily,
    allowConcreteBenchmark: Boolean,
    chosenMatchesBest: Boolean,
    compensationAllowed: Boolean,
    truthPhase: Option[InvestmentTruthPhase],
    ownershipRole: TruthOwnershipRole,
    visibilityRole: TruthVisibilityRole,
    surfaceMode: TruthSurfaceMode,
    exemplarRole: TruthExemplarRole,
    surfacedMoveOwnsTruth: Boolean,
    verifiedPayoffAnchor: Option[String],
    compensationProseAllowed: Boolean,
    benchmarkProseAllowed: Boolean,
    investmentTruthChainKey: Option[String],
    maintenanceExemplarCandidate: Boolean,
    benchmarkCriticalMove: Boolean = false,
    failureMode: FailureInterpretationMode,
    failureIntentConfidence: Double,
    failureIntentAnchor: Option[String],
    failureInterpretationAllowed: Boolean
):
  def isInvestment: Boolean =
    ownershipRole == TruthOwnershipRole.CommitmentOwner

  def isCompensationMaintenance: Boolean =
    ownershipRole == TruthOwnershipRole.MaintenanceEcho

  def isConversionFollowthrough: Boolean =
    ownershipRole == TruthOwnershipRole.ConversionOwner

  def isPrimaryVisible: Boolean =
    visibilityRole == TruthVisibilityRole.PrimaryVisible

  def isSupportingVisible: Boolean =
    visibilityRole == TruthVisibilityRole.SupportingVisible

  def hasVisibleTruth: Boolean =
    visibilityRole != TruthVisibilityRole.Hidden

  def ownershipRoleKey: String = ownershipRole.toString

  def visibilityRoleKey: String = visibilityRole.toString

  def surfaceModeKey: String = surfaceMode.toString

  def exemplarRoleKey: String = exemplarRole.toString

  def isVerifiedExemplar: Boolean =
    exemplarRole == TruthExemplarRole.VerifiedExemplar

  def isProvisionalExemplar: Boolean =
    exemplarRole == TruthExemplarRole.ProvisionalExemplar

  def isExemplarCandidate: Boolean =
    exemplarRole != TruthExemplarRole.NonExemplar || maintenanceExemplarCandidate

  def hasMaintenanceExemplarCandidate: Boolean =
    maintenanceExemplarCandidate

  def isPromotedBestHold: Boolean =
    benchmarkCriticalMove &&
      truthClass == DecisiveTruthClass.Best &&
      reasonFamily == DecisiveReasonFamily.OnlyMoveDefense

  def isBenchmarkCriticalQuietHold: Boolean =
    benchmarkCriticalMove &&
      truthClass == DecisiveTruthClass.Best &&
      reasonFamily == DecisiveReasonFamily.QuietTechnicalMove

  def isCriticalBestMove: Boolean =
    truthClass == DecisiveTruthClass.Best &&
      (
        reasonFamily == DecisiveReasonFamily.OnlyMoveDefense ||
          isBenchmarkCriticalQuietHold
      )

  def benchmarkMove: Option[String] =
    Option.when(benchmarkProseAllowed || allowConcreteBenchmark) {
      verifiedBestMove.filterNot(best => playedMove.exists(move => DecisiveTruth.sameMoveToken(move, best)))
    }.flatten

  def moveClassificationLabel: Option[String] =
    truthClass match
      case DecisiveTruthClass.Blunder               => Some("Blunder")
      case DecisiveTruthClass.MissedWin             => Some("MissedWin")
      case DecisiveTruthClass.WinningInvestment     => Some("WinningInvestment")
      case DecisiveTruthClass.CompensatedInvestment => Some("CompensatedInvestment")
      case _                                        => None

  def truthClassKey: String =
    truthClass match
      case DecisiveTruthClass.Best                  => "best"
      case DecisiveTruthClass.Acceptable            => "acceptable"
      case DecisiveTruthClass.Inaccuracy            => "inaccuracy"
      case DecisiveTruthClass.Mistake               => "mistake"
      case DecisiveTruthClass.Blunder               => "blunder"
      case DecisiveTruthClass.MissedWin             => "missed_win"
      case DecisiveTruthClass.WinningInvestment     => "winning_investment"
      case DecisiveTruthClass.CompensatedInvestment => "compensated_investment"

  def isBad: Boolean =
    truthClass match
      case DecisiveTruthClass.Inaccuracy | DecisiveTruthClass.Mistake | DecisiveTruthClass.Blunder |
          DecisiveTruthClass.MissedWin =>
        true
      case _ => false

  def prefersDecisivePromotion: Boolean =
    isExemplarCandidate || hasVisibleTruth || (
      truthClass match
        case DecisiveTruthClass.Blunder | DecisiveTruthClass.MissedWin | DecisiveTruthClass.WinningInvestment |
            DecisiveTruthClass.CompensatedInvestment =>
          true
        case _ => false
    )

  def narrativeEvent(rawMomentType: String): String =
    truthClass match
      case DecisiveTruthClass.Blunder | DecisiveTruthClass.MissedWin => "AdvantageSwing"
      case DecisiveTruthClass.WinningInvestment | DecisiveTruthClass.CompensatedInvestment =>
        "InvestmentPivot"
      case _ =>
        Option(rawMomentType).map(_.trim) match
          case Some("Equalization")                          => "Equalization"
          case Some("SustainedPressure")                     => "SustainedPressure"
          case Some("TensionPeak")                           => "TensionPeak"
          case Some("MateFound" | "MateLost" | "MateShift") => "MatePivot"
          case Some(other)                                   => other
          case None                                          => "StrategicBridge"

private[llm] object DecisiveTruth:

  private val WinPercentSlope = 0.00368208
  private val GoodAlternativeGapCp = 40

  extension (frame: MoveTruthFrame)
    def toContract: DecisiveTruthContract =
      DecisiveTruthContract(
        playedMove = frame.playedMove,
        verifiedBestMove = frame.verifiedBestMove.orElse(frame.playedMove.filter(_ => frame.benchmark.chosenMatchesBest)),
        truthClass = frame.truthClass,
        cpLoss = frame.moveQuality.cpLoss,
        swingSeverity = frame.moveQuality.swingSeverity,
        reasonFamily = frame.strategicOwnership.reasonFamily,
        allowConcreteBenchmark = frame.benchmarkProseAllowed,
        chosenMatchesBest = frame.benchmark.chosenMatchesBest,
        compensationAllowed = frame.compensationProseAllowed,
        truthPhase = frame.strategicOwnership.truthPhase,
        ownershipRole = frame.ownershipRole,
        visibilityRole = frame.visibilityRole,
        surfaceMode = frame.surfaceMode,
        exemplarRole = frame.exemplarRole,
        surfacedMoveOwnsTruth = frame.surfacedMoveOwnsTruth,
        verifiedPayoffAnchor = frame.strategicOwnership.verifiedPayoffAnchor,
        compensationProseAllowed = frame.compensationProseAllowed,
        benchmarkProseAllowed = frame.benchmarkProseAllowed,
        investmentTruthChainKey = frame.strategicOwnership.chainKey,
        maintenanceExemplarCandidate = frame.strategicOwnership.maintenanceExemplarCandidate,
        benchmarkCriticalMove = frame.strategicOwnership.benchmarkCriticalMove,
        failureMode = frame.failureInterpretation.failureMode,
        failureIntentConfidence = frame.failureInterpretation.intentConfidence,
        failureIntentAnchor = frame.failureInterpretation.intentAnchor,
        failureInterpretationAllowed = frame.failureInterpretation.interpretationAllowed
      )

  def derive(
      ctx: NarrativeContext,
      momentType: Option[String] = None,
      transitionType: Option[String] = None,
      cpBefore: Option[Int] = None,
      cpAfter: Option[Int] = None,
      strategyPack: Option[StrategyPack] = None,
      comparisonOverride: Option[DecisionComparison] = None
  ): DecisiveTruthContract =
    deriveFrame(
      ctx = ctx,
      momentType = momentType,
      transitionType = transitionType,
      cpBefore = cpBefore,
      cpAfter = cpAfter,
      strategyPack = strategyPack,
      comparisonOverride = comparisonOverride
    ).toContract

  def deriveFrame(
      ctx: NarrativeContext,
      momentType: Option[String] = None,
      transitionType: Option[String] = None,
      cpBefore: Option[Int] = None,
      cpAfter: Option[Int] = None,
      strategyPack: Option[StrategyPack] = None,
      comparisonOverride: Option[DecisionComparison] = None
  ): MoveTruthFrame =
    val comparison = comparisonOverride.orElse(DecisionComparisonBuilder.build(ctx))
    val playedMove = comparison.flatMap(_.chosenMove)
    val playedMoveUci =
      ctx.playedMove.flatMap(normalized).filter(looksLikeUciMove)
        .orElse(playedMove.flatMap(normalized).filter(looksLikeUciMove))
    val verifiedBestMove = comparison.flatMap(_.engineBestMove).flatMap(normalized)
    val chosenMatchesBest =
      comparison.exists(_.chosenMatchesBest) ||
        ((playedMove, verifiedBestMove) match
          case (Some(played), Some(best)) => sameMoveToken(played, best)
          case _                          => false
        )
    val rawCpLoss = comparison.flatMap(_.cpLossVsChosen).getOrElse(0)
    val cpLoss = if chosenMatchesBest then 0 else rawCpLoss.max(0)
    val swingSeverity = math.abs(cpAfter.getOrElse(0) - cpBefore.getOrElse(cpAfter.getOrElse(0)))
    val beforePerspective = cpBefore.map(score => moverPerspectiveCp(ctx.fen, score)).getOrElse(0)
    val afterPerspective =
      cpAfter
        .orElse(comparison.flatMap(_.engineBestScoreCp))
        .map(score => moverPerspectiveCp(ctx.fen, score))
        .getOrElse(0)
    val moveQuality = deriveMoveQualityFact(chosenMatchesBest, cpLoss, swingSeverity, beforePerspective, afterPerspective, momentType)
    val transition = normalized(transitionType.getOrElse("")).map(_.toLowerCase)
    val transitionSignalsConversion =
      transition.exists(text =>
        text.contains("promotion") || text.contains("exchange") || text.contains("convert") || text.contains("simplif")
      )
    val materialShift = playedMoveUci.flatMap(move => materialShiftForMove(ctx.fen, move))
    val currentInvestmentCp =
      strategyPack.flatMap(_.signalDigest.flatMap(_.investedMaterial))
        .orElse(ctx.semantic.flatMap(_.compensation.map(_.investedMaterial)))
        .filter(_ > 0)
    val afterInvestmentCp =
      ctx.semantic.flatMap(_.afterCompensation.map(_.investedMaterial))
        .filter(_ > 0)
    val investmentCp =
      currentInvestmentCp
        .orElse(afterInvestmentCp)
    val strategySurface = StrategyPackSurface.from(strategyPack)
    val currentSemanticDecision = currentAcceptedSemanticDecision(ctx).map(_.decision)
    val afterSemanticDecision = afterAcceptedSemanticDecision(ctx).map(_.decision)
    val rawSurfaceSemanticDecision = CompensationInterpretation.surfaceDecision(ctx, strategySurface)
    val surfaceSemanticDecision = rawSurfaceSemanticDecision.filter(_.accepted)
    val acceptedCompensationEvidence =
      List(currentSemanticDecision, afterSemanticDecision, surfaceSemanticDecision).flatten
    val legacyDurableInvestmentPayoff =
      investmentCp.nonEmpty && strategyPack.exists(hasLegacyDurableInvestmentCarrier)
    val verifiedInvestmentPayoff =
      investmentCp.nonEmpty &&
        (
          (
            rawSurfaceSemanticDecision.forall(_.accepted) &&
              acceptedCompensationEvidence.exists(decision =>
                decision.durableStructuralPressure || decision.persistenceClass == "non_immediate_transition"
              )
          ) ||
            legacyDurableInvestmentPayoff
        )
    val freshCommitmentEvidence =
      collectFreshCommitmentEvidence(
        strategyPack = strategyPack,
        strategySurface = strategySurface,
        materialShift = materialShift,
        currentSemanticDecision = currentSemanticDecision,
        afterSemanticDecision = afterSemanticDecision,
        currentInvestmentCp = currentInvestmentCp,
        transitionSignalsConversion = transitionSignalsConversion,
        moveQuality = moveQuality
      )
    val verifiedPayoffAnchor =
      selectVerifiedPayoffAnchor(
        moveLocalPayoffAnchor = freshCommitmentEvidence.moveLocalPayoffAnchor,
        afterSemanticDecision = afterSemanticDecision
      )
    val materialEconomics =
      deriveMaterialEconomicsFact(
        investmentCp = investmentCp,
        materialShift = materialShift,
        verifiedPayoffAnchor = verifiedPayoffAnchor,
        verifiedInvestmentPayoff = verifiedInvestmentPayoff,
        transitionSignalsConversion = transitionSignalsConversion
      )
    val benchmark =
      deriveBenchmarkFact(
        ctx = ctx,
        comparison = comparison,
        verifiedBestMove = verifiedBestMove,
        chosenMatchesBest = chosenMatchesBest,
        cpLoss = cpLoss,
        moveQuality = moveQuality,
        momentType = momentType
      )
    val tactical =
      deriveTacticalFact(
        ctx = ctx,
        momentType = momentType,
        transition = transition,
        comparison = comparison,
        cpLoss = cpLoss,
        swingSeverity = swingSeverity
      )
    val reasonFamily =
      deriveReasonFamily(
        momentType = momentType,
        transition = transition,
        benchmark = benchmark,
        investmentCp = investmentCp,
        verifiedInvestmentPayoff = verifiedInvestmentPayoff,
        freshCommitmentEvidence = freshCommitmentEvidence,
        tactical = tactical
      )
    val strategicOwnership =
      deriveStrategicOwnershipFact(
        ctx = ctx,
        materialEconomics = materialEconomics,
        freshCommitmentEvidence = freshCommitmentEvidence.copy(verifiedPayoffAnchor = verifiedPayoffAnchor),
        currentInvestmentCp = currentInvestmentCp,
        investmentCp = investmentCp,
        reasonFamily = reasonFamily,
        verifiedInvestmentPayoff = verifiedInvestmentPayoff,
        transitionSignalsConversion = transitionSignalsConversion,
        afterSemanticSupport = afterSemanticDecision.nonEmpty,
        benchmark = benchmark,
        tactical = tactical,
        moveQuality = moveQuality,
        currentSemanticDecision = currentSemanticDecision,
        strategyPack = strategyPack
      )
    val failureInterpretation =
      deriveFailureInterpretationFact(
        moveQuality = moveQuality,
        benchmark = benchmark,
        tactical = tactical,
        materialEconomics = materialEconomics,
        strategicOwnership = strategicOwnership,
        strategyPack = strategyPack
      )
    val difficultyNovelty =
      deriveDifficultyNoveltyFact(comparison, moveQuality, benchmark, tactical, strategicOwnership)
    val truthClass =
      projectTruthClass(moveQuality, strategicOwnership, benchmark, afterPerspective)
    val ownershipRole =
      projectOwnershipRole(truthClass, strategicOwnership)
    val exemplarRole =
      projectExemplarRole(ownershipRole, strategicOwnership, moveQuality)
    val visibilityRole =
      projectVisibilityRole(ownershipRole, exemplarRole, truthClass, strategicOwnership)
    val surfaceMode =
      projectSurfaceMode(ownershipRole, truthClass, strategicOwnership)
    val compensationProseAllowed =
      surfaceMode == TruthSurfaceMode.InvestmentExplain &&
        strategicOwnership.verifiedPayoffAnchor.nonEmpty
    val benchmarkProseAllowed =
      benchmark.benchmarkNamingAllowed &&
        !compensationProseAllowed &&
        truthClass != DecisiveTruthClass.WinningInvestment &&
        truthClass != DecisiveTruthClass.CompensatedInvestment
    val surfacedMoveOwnsTruth =
      ownershipRole == TruthOwnershipRole.BlunderOwner ||
        ownershipRole == TruthOwnershipRole.CommitmentOwner ||
        ownershipRole == TruthOwnershipRole.ConversionOwner
    val punishConversion =
      derivePunishConversionFact(moveQuality, truthClass, strategicOwnership, tactical, afterPerspective)
    MoveTruthFrame(
      playedMove = playedMove,
      verifiedBestMove = verifiedBestMove,
      moveQuality = moveQuality,
      benchmark = benchmark,
      tactical = tactical,
      materialEconomics = materialEconomics,
      strategicOwnership = strategicOwnership,
      punishConversion = punishConversion,
      failureInterpretation = failureInterpretation,
      difficultyNovelty = difficultyNovelty,
      truthClass = truthClass,
      ownershipRole = ownershipRole,
      visibilityRole = visibilityRole,
      surfaceMode = surfaceMode,
      exemplarRole = exemplarRole,
      surfacedMoveOwnsTruth = surfacedMoveOwnsTruth,
      compensationProseAllowed = compensationProseAllowed,
      benchmarkProseAllowed = benchmarkProseAllowed
    )

  def momentProjection(
      moment: GameArcMoment,
      contractOpt: Option[DecisiveTruthContract]
  ): MomentTruthProjection =
    contractOpt.map(contractProjection).getOrElse(
      fallbackMomentProjection(
        classificationKey = normalizedWholeGameText(moment.moveClassification.getOrElse("")).getOrElse(""),
        verifiedPayoffAnchor = moment.verifiedPayoffAnchor
      )
    )

  def momentProjection(
      moment: GameChronicleMoment,
      contractOpt: Option[DecisiveTruthContract]
  ): MomentTruthProjection =
    contractOpt.map(contractProjection).getOrElse(
      fallbackMomentProjection(
        classificationKey = normalizedWholeGameText(moment.moveClassification.getOrElse("")).getOrElse(""),
        verifiedPayoffAnchor = None
      )
    )

  def sanitizeContext(
      ctx: NarrativeContext,
      contract: DecisiveTruthContract
  ): NarrativeContext =
    val compensationAllowed = contract.compensationProseAllowed
    val decisiveInvestmentExplain = contract.surfaceMode == TruthSurfaceMode.InvestmentExplain
    val sanitizedSemantic =
      if compensationAllowed then ctx.semantic
      else ctx.semantic.map(_.copy(compensation = None, afterCompensation = None))
    val sanitizedCounterfactual =
      if contract.chosenMatchesBest || decisiveInvestmentExplain then None else ctx.counterfactual
    val sanitizedCandidates =
      if contract.chosenMatchesBest || decisiveInvestmentExplain then
        ctx.candidates.map { candidate =>
          val isChosen =
            candidate.uci.exists(u => contract.playedMove.exists(move => sameMoveToken(u, move))) ||
              contract.playedMove.exists(move => sameMoveToken(candidate.move, move))
          if isChosen then candidate.copy(whyNot = None) else candidate
        }
      else ctx.candidates
    ctx.copy(
      semantic = sanitizedSemantic,
      counterfactual = sanitizedCounterfactual,
      candidates = sanitizedCandidates
    )

  def sanitizeDecisionComparison(
      comparison: Option[DecisionComparison],
      contract: DecisiveTruthContract
  ): Option[DecisionComparison] =
    comparison.map { existing =>
      val benchmark = contract.benchmarkMove
      val preservedReason =
        if benchmark.nonEmpty && existing.engineBestMove.exists(best => benchmark.exists(move => sameMoveToken(best, move))) then
          existing.deferredReason.flatMap(normalized).map(UserFacingSignalSanitizer.sanitize)
        else None
      existing.copy(
        engineBestMove = contract.verifiedBestMove.orElse(existing.engineBestMove),
        cpLossVsChosen = Option.when(contract.cpLoss > 0)(contract.cpLoss),
        deferredMove = benchmark,
        deferredReason = preservedReason,
        deferredSource = benchmark.map(_ => "verified_best"),
        practicalAlternative = false,
        chosenMatchesBest = contract.chosenMatchesBest
      )
    }

  def sanitizeDigest(
      digest: Option[NarrativeSignalDigest],
      contract: DecisiveTruthContract
  ): Option[NarrativeSignalDigest] =
    digest.map { existing =>
      val benchmark = contract.benchmarkMove
      val sanitizedComparison =
        existing.decisionComparison.map { comparison =>
          val preservedReason =
            if benchmark.nonEmpty && comparison.engineBestMove.exists(best => benchmark.exists(move => sameMoveToken(best, move))) then
              comparison.deferredReason.flatMap(normalized).map(UserFacingSignalSanitizer.sanitize)
            else None
          comparison.copy(
            engineBestMove = contract.verifiedBestMove.orElse(comparison.engineBestMove),
            cpLossVsChosen = Option.when(contract.cpLoss > 0)(contract.cpLoss),
            deferredMove = benchmark,
            deferredReason = preservedReason,
            deferredSource = benchmark.map(_ => "verified_best"),
            practicalAlternative = false,
            chosenMatchesBest = contract.chosenMatchesBest
          )
        }
      existing.copy(
        decisionComparison = sanitizedComparison,
        compensation = Option.when(contract.compensationProseAllowed)(existing.compensation).flatten,
        compensationVectors = Option.when(contract.compensationProseAllowed)(existing.compensationVectors).getOrElse(Nil),
        investedMaterial = Option.when(contract.compensationProseAllowed)(existing.investedMaterial).flatten
      )
    }

  def sanitizeStrategyPack(
      strategyPack: Option[StrategyPack],
      contract: DecisiveTruthContract
  ): Option[StrategyPack] =
    strategyPack.map { pack =>
      pack.copy(
        longTermFocus =
          if !allowsFailureIntentInterpretation(contract) then Nil
          else if contract.compensationProseAllowed then pack.longTermFocus
          else pack.longTermFocus.filterNot(isCompensationCarrierText),
        pieceRoutes =
          if allowsFailureIntentInterpretation(contract) then pack.pieceRoutes else Nil,
        pieceMoveRefs =
          if allowsFailureIntentInterpretation(contract) then pack.pieceMoveRefs else Nil,
        directionalTargets =
          if allowsFailureIntentInterpretation(contract) then pack.directionalTargets else Nil,
        strategicIdeas =
          if allowsFailureIntentInterpretation(contract) then pack.strategicIdeas else Nil,
        evidence =
          if !allowsFailureIntentInterpretation(contract) then Nil
          else if contract.compensationProseAllowed then pack.evidence
          else pack.evidence.filterNot(isCompensationCarrierText),
        signalDigest = sanitizeFailureDigest(sanitizeDigest(pack.signalDigest, contract), contract)
      )
    }

  def sameMoveToken(left: String, right: String): Boolean =
    normalizeMoveToken(left) == normalizeMoveToken(right)

  private def contractProjection(contract: DecisiveTruthContract): MomentTruthProjection =
    MomentTruthProjection(
      classificationKey = contract.truthClassKey.replace("_", ""),
      ownershipRole = contract.ownershipRole,
      visibilityRole = contract.visibilityRole,
      surfaceMode = contract.surfaceMode,
      exemplarRole = contract.exemplarRole,
      surfacedMoveOwnsTruth = contract.surfacedMoveOwnsTruth,
      verifiedPayoffAnchor = contract.verifiedPayoffAnchor,
      benchmarkProseAllowed = contract.benchmarkProseAllowed,
      chainKey = contract.investmentTruthChainKey,
      maintenanceExemplarCandidate = contract.maintenanceExemplarCandidate,
      benchmarkCriticalMove = contract.benchmarkCriticalMove
    )

  private def deriveMoveQualityFact(
      chosenMatchesBest: Boolean,
      cpLoss: Int,
      swingSeverity: Int,
      beforePerspective: Int,
      afterPerspective: Int,
      momentType: Option[String]
  ): MoveQualityFact =
    val winPercentBefore = winPercentFromCp(beforePerspective)
    val winPercentAfter = winPercentFromCp(afterPerspective)
    val winPercentLoss = (winPercentBefore - winPercentAfter).max(0.0)
    val verdict =
      if chosenMatchesBest then
        if cpLoss == 0 then MoveQualityVerdict.Best else MoveQualityVerdict.Acceptable
      else if momentType.exists(_.equalsIgnoreCase("MissedWin")) then MoveQualityVerdict.MissedWin
      else
        Thresholds.classifySeverity(cpLoss) match
          case "blunder"    => MoveQualityVerdict.Blunder
          case "mistake"    => MoveQualityVerdict.Mistake
          case "inaccuracy" => MoveQualityVerdict.Inaccuracy
          case _            => MoveQualityVerdict.Acceptable
    val severityBand =
      if cpLoss >= Thresholds.BLUNDER_CP || winPercentLoss >= 25 then "catastrophic"
      else if cpLoss >= Thresholds.MISTAKE_CP || winPercentLoss >= 12 then "serious"
      else if cpLoss >= Thresholds.INACCURACY_CP || winPercentLoss >= 5 then "moderate"
      else "stable"
    MoveQualityFact(
      verdict = verdict,
      cpLoss = cpLoss,
      swingSeverity = swingSeverity,
      winPercentBefore = winPercentBefore,
      winPercentAfter = winPercentAfter,
      winPercentLoss = winPercentLoss,
      severityBand = severityBand
    )

  private def deriveBenchmarkFact(
      ctx: NarrativeContext,
      comparison: Option[DecisionComparison],
      verifiedBestMove: Option[String],
      chosenMatchesBest: Boolean,
      cpLoss: Int,
      moveQuality: MoveQualityFact,
      momentType: Option[String]
  ): BenchmarkFact =
    val moverBestScore =
      comparison.flatMap(_.engineBestScoreCp).map(score => moverPerspectiveCp(ctx.fen, score))
    val alternativeScores =
      ctx.engineEvidence.toList.flatMap(_.variations.drop(1)).map(line => moverPerspectiveCp(ctx.fen, line.scoreCp))
    val alternativeCount =
      moverBestScore.map { bestScore =>
        alternativeScores.count(score => math.abs(bestScore - score) <= GoodAlternativeGapCp)
      }.orElse(Option.when(comparison.exists(_.practicalAlternative))(1)).getOrElse(0)
    val onlyMove =
      ctx.header.choiceType.equalsIgnoreCase("OnlyMove") ||
        (!chosenMatchesBest && cpLoss >= Thresholds.BLUNDER_CP && alternativeCount == 0)
    val uniqueGoodMove =
      verifiedBestMove.nonEmpty &&
        (alternativeCount == 0 || onlyMove) &&
        !momentType.exists(_.equalsIgnoreCase("MissedWin"))
    val verificationTier =
      if onlyMove || uniqueGoodMove || moveQuality.isBad then "deep_candidate" else "baseline"
    BenchmarkFact(
      verifiedBestMove = verifiedBestMove,
      chosenMatchesBest = chosenMatchesBest,
      onlyMove = onlyMove,
      uniqueGoodMove = uniqueGoodMove,
      benchmarkNamingAllowed = !chosenMatchesBest && verifiedBestMove.nonEmpty,
      alternativeCount = alternativeCount,
      verificationTier = verificationTier
    )

  private def deriveTacticalFact(
      ctx: NarrativeContext,
      momentType: Option[String],
      transition: Option[String],
      comparison: Option[DecisionComparison],
      cpLoss: Int,
      swingSeverity: Int
  ): TacticalFact =
    val motifs =
      (ctx.facts ++ ctx.mainPvFacts ++ ctx.threatLineFacts ++ ctx.counterfactualFacts)
        .flatMap(tacticalMotifId)
        .distinct
    val forcedMate =
      momentType.exists(text => normalizedWholeGameText(text).exists(_.contains("mate"))) ||
        motifs.contains("forced_mate") ||
        ctx.threats.toUs.exists(_.kind.equalsIgnoreCase("Mate")) ||
        ctx.threats.toThem.exists(_.kind.equalsIgnoreCase("Mate"))
    val forcedDrawResource =
      motifs.contains("stalemate_resource") || motifs.contains("perpetual_resource")
    val forcingLine =
      forcedMate ||
        ctx.header.criticality.equalsIgnoreCase("Forced") ||
        motifs.exists(Set("hanging_piece", "fork", "pin", "skewer", "promotion_race", "double_check"))
    val immediateRefutation =
      cpLoss >= Thresholds.MISTAKE_CP &&
        (forcingLine || swingSeverity >= Thresholds.MISTAKE_CP || momentType.exists(_.equalsIgnoreCase("AdvantageSwing")))
    val tacticalMotifs =
      (motifs ++ transition.toList.flatMap(text => if text.contains("promotion") then List("promotion_race") else Nil)).distinct
    TacticalFact(
      immediateRefutation = immediateRefutation,
      forcingLine = forcingLine,
      forcedMate = forcedMate,
      forcedDrawResource = forcedDrawResource,
      motifs = tacticalMotifs,
      proofLine = comparison.map(_.engineBestPv.take(4)).getOrElse(Nil)
    )

  private def deriveMaterialEconomicsFact(
      investmentCp: Option[Int],
      materialShift: Option[MaterialShift],
      verifiedPayoffAnchor: Option[String],
      verifiedInvestmentPayoff: Boolean,
      transitionSignalsConversion: Boolean
  ): MaterialEconomicsFact =
    val shift = materialShift.getOrElse(MaterialShift(0, 0, 0, 0))
    val sacrificeKind = classifySacrificeKind(materialShift)
    MaterialEconomicsFact(
      investedMaterialCp = investmentCp,
      beforeDeficit = shift.beforeDeficit,
      afterDeficit = shift.afterDeficit,
      movingPieceValue = shift.movingPieceValue,
      capturedPieceValue = shift.capturedPieceValue,
      sacrificeKind = sacrificeKind,
      valueDownCapture = materialShift.exists(_.isValueDownCapture),
      recoversDeficit = materialShift.exists(_.recoversDeficit),
      overinvestment = investmentCp.exists(_ >= 300) && verifiedPayoffAnchor.isEmpty,
      uncompensatedLoss = investmentCp.nonEmpty && !verifiedInvestmentPayoff && verifiedPayoffAnchor.isEmpty,
      forcedRecovery = materialShift.exists(_.recoversDeficit) && transitionSignalsConversion
    )

  private def classifySacrificeKind(materialShift: Option[MaterialShift]): Option[String] =
    materialShift.flatMap { shift =>
      if shift.isValueDownCapture && shift.movingPieceValue >= 500 && shift.capturedPieceValue <= 300 then
        Some("exchange_sac")
      else if shift.isValueDownCapture then Some("value_down_capture")
      else if shift.capturedPieceValue == 0 && shift.increasesDeficit && shift.movingPieceValue >= 300 then
        Some("piece_sac")
      else if shift.increasesDeficit && shift.movingPieceValue > shift.capturedPieceValue then
        Some("material_investment")
      else None
    }

  private def collectFreshCommitmentEvidence(
      strategyPack: Option[StrategyPack],
      strategySurface: StrategyPackSurface.Snapshot,
      materialShift: Option[MaterialShift],
      currentSemanticDecision: Option[CompensationInterpretation.Decision],
      afterSemanticDecision: Option[CompensationInterpretation.Decision],
      currentInvestmentCp: Option[Int],
      transitionSignalsConversion: Boolean,
      moveQuality: MoveQualityFact
  ): FreshCommitmentEvidence =
    val currentMaterialSeed =
      materialShift.exists(shift =>
        classifySacrificeKind(Some(shift)).nonEmpty || shift.increasesDeficit || shift.isValueDownCapture
      )
    val moveLocalPayoffAnchor =
      selectMoveLocalPayoffAnchor(
        currentSemanticDecision = currentSemanticDecision,
        strategyPack = strategyPack,
        strategySurface = strategySurface
      )
    val currentConcreteCarrier =
      strategyPack.exists(pack =>
        pack.directionalTargets.nonEmpty || pack.pieceRoutes.nonEmpty || pack.pieceMoveRefs.nonEmpty
      )
    val currentSemanticSupport = currentSemanticDecision.nonEmpty
    val afterSemanticSupport = afterSemanticDecision.nonEmpty
    val legacyShellSupport =
      strategySurface.compensationSummary.nonEmpty ||
        strategySurface.compensationVectors.nonEmpty ||
        strategySurface.investedMaterial.nonEmpty ||
        strategyPack.exists(hasLegacyDurableInvestmentCarrier)
    val blunderGrade =
      moveQuality.verdict == MoveQualityVerdict.Blunder ||
        moveQuality.verdict == MoveQualityVerdict.MissedWin
    val seedEligible =
      currentMaterialSeed &&
        moveLocalPayoffAnchor.nonEmpty &&
        !transitionSignalsConversion &&
        !blunderGrade
    val ownerEligible =
      seedEligible && (currentSemanticSupport || currentInvestmentCp.nonEmpty)
    val legacyVisibleOnly =
      legacyShellSupport &&
        !currentMaterialSeed &&
        !currentSemanticSupport &&
        !afterSemanticSupport &&
        !currentConcreteCarrier
    val provenance =
      Set.newBuilder[CommitmentEvidenceProvenance]
    if currentMaterialSeed then provenance += CommitmentEvidenceProvenance.CurrentMaterial
    if currentSemanticSupport || currentConcreteCarrier then provenance += CommitmentEvidenceProvenance.CurrentSemantic
    if afterSemanticSupport then provenance += CommitmentEvidenceProvenance.AfterSemantic
    if legacyShellSupport then provenance += CommitmentEvidenceProvenance.LegacyShell
    FreshCommitmentEvidence(
      moveLocalPayoffAnchor = moveLocalPayoffAnchor,
      verifiedPayoffAnchor = None,
      provenance = provenance.result(),
      currentMaterialSeed = currentMaterialSeed,
      seedEligible = seedEligible,
      ownerEligible = ownerEligible,
      legacyVisibleOnly = legacyVisibleOnly,
      currentConcreteCarrier = currentConcreteCarrier,
      currentSemanticSupport = currentSemanticSupport,
      afterSemanticSupport = afterSemanticSupport,
      legacyShellSupport = legacyShellSupport
    )

  private def selectMoveLocalPayoffAnchor(
      currentSemanticDecision: Option[CompensationInterpretation.Decision],
      strategyPack: Option[StrategyPack],
      strategySurface: StrategyPackSurface.Snapshot
  ): Option[String] =
    moveLocalPayoffSignals(
      currentSemanticDecision = currentSemanticDecision,
      strategyPack = strategyPack,
      strategySurface = strategySurface
    ).flatMap(normalizeVerifiedPayoffAnchor).headOption

  private def moveLocalPayoffSignals(
      currentSemanticDecision: Option[CompensationInterpretation.Decision],
      strategyPack: Option[StrategyPack],
      strategySurface: StrategyPackSurface.Snapshot
  ): List[String] =
    val semanticSignals =
      currentSemanticDecision.toList.flatMap(decision =>
        decision.signal.summary.toList ++ decision.signal.vectors
      )
    val packSignals =
      strategyPack.toList.flatMap { pack =>
        val digest = pack.signalDigest
        (
          List(
            strategySurface.rawDominantIdeaText,
            strategySurface.rawSecondaryIdeaText,
            strategySurface.rawObjectiveText,
            strategySurface.rawFocusText,
            digest.flatMap(_.strategicFlow),
            digest.flatMap(_.dominantIdeaFocus),
            digest.flatMap(_.secondaryIdeaFocus)
          ).flatten ++
            pack.longTermFocus ++
            pack.pieceRoutes.flatMap(routePurposeSignals) ++
            pack.pieceMoveRefs.flatMap(moveRefPurposeSignals) ++
            pack.directionalTargets.flatMap(directionalTargetSignals) ++
            strategySurface.rawExecutionText.toList
        )
      }
    (semanticSignals ++ packSignals).flatMap(normalized).distinct

  private def semanticAnchorSignals(
      currentSemanticDecision: Option[CompensationInterpretation.Decision]
  ): List[String] =
    currentSemanticDecision.toList
      .flatMap(decision => decision.signal.summary.toList ++ decision.signal.vectors)
      .flatMap(normalized)
      .distinct

  private def strictCurrentCarrierSignals(strategyPack: Option[StrategyPack]): List[String] =
    strategyPack.toList
      .flatMap(pack =>
        pack.pieceRoutes.map(_.purpose) ++
          pack.pieceMoveRefs.map(_.idea) ++
          pack.directionalTargets.flatMap(_.strategicReasons)
      )
      .flatMap(normalized)
      .distinct

  private def matchedAnchorSignal(
      verifiedPayoffAnchor: Option[String],
      rawSignals: List[String]
  ): Option[String] =
    verifiedPayoffAnchor.filter(anchor =>
      rawSignals.exists(signal => anchorSignalsMatch(anchor, signal))
    )

  private def hasMatchedAnchorSignal(
      verifiedPayoffAnchor: Option[String],
      rawSignals: List[String]
  ): Boolean =
    matchedAnchorSignal(verifiedPayoffAnchor, rawSignals).nonEmpty

  private def routePurposeSignals(route: StrategyPieceRoute): List[String] =
    val purpose = StrategyPackSurface.normalizeText(route.purpose)
    val destination = route.route.lastOption.map(StrategyPackSurface.normalizeText).filter(_.nonEmpty)
    val piece = StrategyPackSurface.pieceName(route.piece)
    List(
      Option.when(purpose.nonEmpty)(purpose),
      Option.when(purpose.nonEmpty && destination.nonEmpty)(s"$piece toward ${destination.get} for $purpose")
    ).flatten

  private def moveRefPurposeSignals(moveRef: StrategyPieceMoveRef): List[String] =
    val idea = StrategyPackSurface.normalizeText(moveRef.idea)
    val target = StrategyPackSurface.normalizeText(moveRef.target)
    val piece = StrategyPackSurface.pieceName(moveRef.piece)
    List(
      Option.when(idea.nonEmpty)(idea),
      Option.when(idea.nonEmpty && target.nonEmpty)(s"$piece toward $target for $idea")
    ).flatten

  private def directionalTargetSignals(target: StrategyDirectionalTarget): List[String] =
    val square = StrategyPackSurface.normalizeText(target.targetSquare)
    val piece = StrategyPackSurface.pieceName(target.piece)
    target.strategicReasons.map(StrategyPackSurface.normalizeText).filter(_.nonEmpty).distinct ++
      Option.when(square.nonEmpty)(s"the $piece can head for $square").toList

  private def deriveReasonFamily(
      momentType: Option[String],
      transition: Option[String],
      benchmark: BenchmarkFact,
      investmentCp: Option[Int],
      verifiedInvestmentPayoff: Boolean,
      freshCommitmentEvidence: FreshCommitmentEvidence,
      tactical: TacticalFact
  ): DecisiveReasonFamily =
    val nonTrivialProofLine = tactical.proofLine.lengthCompare(2) >= 0
    val proofBackedBestHold =
      tactical.immediateRefutation ||
        tactical.forcingLine ||
        tactical.forcedMate ||
        tactical.forcedDrawResource ||
        nonTrivialProofLine
    if momentType.exists(_.equalsIgnoreCase("MissedWin")) then DecisiveReasonFamily.MissedWin
    else if (benchmark.onlyMove || benchmark.uniqueGoodMove) && proofBackedBestHold
    then
      DecisiveReasonFamily.OnlyMoveDefense
    else if transition.exists(text =>
        text.contains("promotion") || text.contains("exchange") || text.contains("convert") || text.contains("simplif")
      )
    then DecisiveReasonFamily.Conversion
    else if investmentCp.nonEmpty || verifiedInvestmentPayoff || freshCommitmentEvidence.seedEligible then
      DecisiveReasonFamily.InvestmentSacrifice
    else if proofBackedBestHold then DecisiveReasonFamily.TacticalRefutation
    else DecisiveReasonFamily.QuietTechnicalMove

  private def deriveStrategicOwnershipFact(
      ctx: NarrativeContext,
      materialEconomics: MaterialEconomicsFact,
      freshCommitmentEvidence: FreshCommitmentEvidence,
      currentInvestmentCp: Option[Int],
      investmentCp: Option[Int],
      reasonFamily: DecisiveReasonFamily,
      verifiedInvestmentPayoff: Boolean,
      transitionSignalsConversion: Boolean,
      afterSemanticSupport: Boolean,
      benchmark: BenchmarkFact,
      tactical: TacticalFact,
      moveQuality: MoveQualityFact,
      currentSemanticDecision: Option[CompensationInterpretation.Decision],
      strategyPack: Option[StrategyPack]
  ): StrategicOwnershipFact =
    val verifiedPayoffAnchor = freshCommitmentEvidence.verifiedPayoffAnchor
    val benchmarkCriticalMove = benchmark.onlyMove || benchmark.uniqueGoodMove
    val createsFreshInvestment = freshCommitmentEvidence.seedEligible
    val recoversInvestment =
      verifiedPayoffAnchor.nonEmpty &&
        materialEconomics.recoversDeficit &&
        (investmentCp.nonEmpty || transitionSignalsConversion || afterSemanticSupport)
    val maintainsInvestment =
      verifiedPayoffAnchor.nonEmpty &&
        !createsFreshInvestment &&
        !recoversInvestment &&
        (investmentCp.nonEmpty || afterSemanticSupport || freshCommitmentEvidence.legacyVisibleOnly)
    val truthPhase =
      if createsFreshInvestment then Some(InvestmentTruthPhase.FirstInvestmentCommitment)
      else if recoversInvestment || (transitionSignalsConversion && verifiedPayoffAnchor.nonEmpty) then
        Some(InvestmentTruthPhase.ConversionFollowthrough)
      else if maintainsInvestment then Some(InvestmentTruthPhase.CompensationMaintenance)
      else None
    val chainReasonFamily =
      truthPhase match
        case Some(InvestmentTruthPhase.FirstInvestmentCommitment) | Some(InvestmentTruthPhase.CompensationMaintenance) =>
          DecisiveReasonFamily.InvestmentSacrifice
        case Some(InvestmentTruthPhase.ConversionFollowthrough) =>
          DecisiveReasonFamily.Conversion
        case None => reasonFamily
    val chainKey =
      Option.when(truthPhase.nonEmpty && verifiedPayoffAnchor.nonEmpty) {
        buildInvestmentTruthChainKey(verifiedPayoffAnchor, chainReasonFamily, ctx)
      }.filter(_.nonEmpty)
    val currentSemanticAnchorMatch =
      hasMatchedAnchorSignal(verifiedPayoffAnchor, semanticAnchorSignals(currentSemanticDecision))
    val currentCarrierAnchorMatch =
      hasMatchedAnchorSignal(verifiedPayoffAnchor, strictCurrentCarrierSignals(strategyPack))
    val anchorMatchedCurrentEvidence = currentSemanticAnchorMatch || currentCarrierAnchorMatch
    val tacticalPressureSignal =
      tactical.immediateRefutation ||
        tactical.forcingLine ||
        tactical.forcedMate ||
        tactical.forcedDrawResource ||
        tactical.proofLine.nonEmpty
    val badFollowthroughSignal =
      moveQuality.isBad &&
        !benchmark.chosenMatchesBest &&
        verifiedPayoffAnchor.nonEmpty &&
        anchorMatchedCurrentEvidence
    val maintenancePressureQualified =
      benchmark.onlyMove ||
        benchmark.uniqueGoodMove ||
        tacticalPressureSignal ||
        badFollowthroughSignal ||
        currentSemanticAnchorMatch
    val criticalMaintenance =
      truthPhase.contains(InvestmentTruthPhase.CompensationMaintenance) &&
        verifiedPayoffAnchor.nonEmpty &&
        chainKey.nonEmpty &&
        verifiedInvestmentPayoff &&
        anchorMatchedCurrentEvidence &&
        maintenancePressureQualified &&
        !freshCommitmentEvidence.legacyVisibleOnly

    StrategicOwnershipFact(
      truthPhase = truthPhase,
      reasonFamily = chainReasonFamily,
      benchmarkCriticalMove = benchmarkCriticalMove,
      verifiedPayoffAnchor = verifiedPayoffAnchor,
      chainKey = chainKey,
      evidenceProvenance = freshCommitmentEvidence.provenance,
      createsFreshInvestment = createsFreshInvestment,
      maintainsInvestment = maintainsInvestment,
      convertsInvestment = recoversInvestment || transitionSignalsConversion,
      durablePressure = verifiedInvestmentPayoff,
      currentMoveEvidence = freshCommitmentEvidence.currentMoveEvidence,
      currentConcreteCarrier = freshCommitmentEvidence.currentConcreteCarrier,
      currentSemanticAnchorMatch = currentSemanticAnchorMatch,
      currentCarrierAnchorMatch = currentCarrierAnchorMatch,
      freshCommitmentCandidate = freshCommitmentEvidence.seedEligible,
      freshCurrentInvestmentEvidence =
        freshCommitmentEvidence.currentMaterialSeed || anchorMatchedCurrentEvidence,
      ownerEligible =
        freshCommitmentEvidence.ownerEligible ||
          (
            createsFreshInvestment &&
              currentInvestmentCp.nonEmpty &&
              freshCommitmentEvidence.currentMaterialSeed &&
              freshCommitmentEvidence.verifiedPayoffAnchor.nonEmpty
          ),
      legacyVisibleOnly = freshCommitmentEvidence.legacyVisibleOnly,
      maintenancePressureQualified = maintenancePressureQualified,
      criticalMaintenance = criticalMaintenance,
      maintenanceExemplarCandidate = criticalMaintenance
    )

  private def deriveFailureInterpretationFact(
      moveQuality: MoveQualityFact,
      benchmark: BenchmarkFact,
      tactical: TacticalFact,
      materialEconomics: MaterialEconomicsFact,
      strategicOwnership: StrategicOwnershipFact,
      strategyPack: Option[StrategyPack]
  ): FailureInterpretationFact =
    val routeTargetAnchor =
      selectConcreteCarrierAnchor(strategyPack, strategicOwnership.verifiedPayoffAnchor)
    val semanticIntentAnchor =
      Option.when(strategicOwnership.currentSemanticAnchorMatch)(strategicOwnership.verifiedPayoffAnchor).flatten
    val intentAnchor = routeTargetAnchor.orElse(semanticIntentAnchor)
    val proofBackedCriticalHold =
      tactical.immediateRefutation ||
        tactical.forcingLine ||
        tactical.forcedMate ||
        tactical.forcedDrawResource ||
        tactical.proofLine.lengthCompare(2) >= 0
    val failureMode =
      if !moveQuality.isBad then FailureInterpretationMode.NoClearPlan
      else if materialEconomics.sacrificeKind.nonEmpty &&
        strategicOwnership.freshCurrentInvestmentEvidence &&
        intentAnchor.nonEmpty &&
        !strategicOwnership.legacyVisibleOnly
      then FailureInterpretationMode.SpeculativeInvestmentFailed
      else if strategicOwnership.reasonFamily == DecisiveReasonFamily.OnlyMoveDefense ||
        (benchmark.onlyMove && intentAnchor.nonEmpty) ||
        (strategicOwnership.benchmarkCriticalMove && proofBackedCriticalHold)
      then
        FailureInterpretationMode.OnlyMoveFailure
      else if tactical.immediateRefutation ||
        tactical.forcingLine ||
        tactical.proofLine.lengthCompare(2) >= 0 ||
        strategicOwnership.reasonFamily == DecisiveReasonFamily.TacticalRefutation
      then FailureInterpretationMode.TacticalRefutation
      else if strategicOwnership.verifiedPayoffAnchor.nonEmpty then FailureInterpretationMode.QuietPositionalCollapse
      else FailureInterpretationMode.NoClearPlan
    val intentConfidence =
      if !moveQuality.isBad then 0.0
      else if routeTargetAnchor.nonEmpty then
        if benchmark.onlyMove then 0.92
        else if tactical.immediateRefutation || tactical.forcingLine then 0.84
        else 0.76
      else if semanticIntentAnchor.nonEmpty then
        if tactical.immediateRefutation || tactical.forcingLine then 0.66 else 0.62
      else 0.0
    val interpretationAllowed =
      moveQuality.isBad &&
        failureMode != FailureInterpretationMode.NoClearPlan &&
        intentAnchor.nonEmpty
    FailureInterpretationFact(
      failureMode = failureMode,
      intentConfidence = intentConfidence,
      intentAnchor = Option.when(moveQuality.isBad && intentAnchor.nonEmpty)(intentAnchor).flatten,
      interpretationAllowed = interpretationAllowed
    )

  private def deriveDifficultyNoveltyFact(
      comparison: Option[DecisionComparison],
      moveQuality: MoveQualityFact,
      benchmark: BenchmarkFact,
      tactical: TacticalFact,
      strategicOwnership: StrategicOwnershipFact
  ): DifficultyNoveltyFact =
    val depthSensitive =
      benchmark.onlyMove ||
        benchmark.uniqueGoodMove ||
        (comparison.exists(_.engineBestPv.size >= 3) &&
          (strategicOwnership.verifiedPayoffAnchor.nonEmpty || tactical.motifs.nonEmpty))
    val shallowUnderestimated =
      benchmark.chosenMatchesBest &&
        moveQuality.swingSeverity >= Thresholds.MISTAKE_CP &&
        (strategicOwnership.verifiedPayoffAnchor.nonEmpty || tactical.forcingLine)
    val verificationTier =
      if moveQuality.isBad || strategicOwnership.freshCommitmentCandidate || benchmark.onlyMove || tactical.immediateRefutation
      then "deep_candidate"
      else "baseline"
    DifficultyNoveltyFact(
      onlyMoveDefense = benchmark.onlyMove && !benchmark.chosenMatchesBest,
      uniqueGoodMove = benchmark.uniqueGoodMove,
      depthSensitive = depthSensitive,
      shallowUnderestimated = shallowUnderestimated,
      verificationTier = verificationTier
    )

  private def fallbackMomentProjection(
      classificationKey: String,
      verifiedPayoffAnchor: Option[String]
  ): MomentTruthProjection =
    val ownershipRole =
      classificationKey match
        case "blunder" | "missedwin" => TruthOwnershipRole.BlunderOwner
        case _                       => TruthOwnershipRole.NoneRole
    val surfaceMode =
      ownershipRole match
        case TruthOwnershipRole.BlunderOwner    => TruthSurfaceMode.FailureExplain
        case _                                  => TruthSurfaceMode.Neutral
    val ownsTruth =
      ownershipRole == TruthOwnershipRole.BlunderOwner
    val visibilityRole =
      if ownsTruth then TruthVisibilityRole.PrimaryVisible
      else TruthVisibilityRole.Hidden
    MomentTruthProjection(
      classificationKey = classificationKey,
      ownershipRole = ownershipRole,
      visibilityRole = visibilityRole,
      surfaceMode = surfaceMode,
      exemplarRole =
        TruthExemplarRole.NonExemplar,
      surfacedMoveOwnsTruth = ownsTruth,
      verifiedPayoffAnchor = Option.when(ownsTruth)(verifiedPayoffAnchor).flatten,
      benchmarkProseAllowed = false,
      chainKey = None,
      maintenanceExemplarCandidate = false,
      benchmarkCriticalMove = false
    )

  private def derivePunishConversionFact(
      moveQuality: MoveQualityFact,
      truthClass: DecisiveTruthClass,
      strategicOwnership: StrategicOwnershipFact,
      tactical: TacticalFact,
      afterPerspective: Int
  ): PunishConversionFact =
    val immediatePunishment =
      moveQuality.verdict match
        case MoveQualityVerdict.Blunder | MoveQualityVerdict.MissedWin =>
          tactical.immediateRefutation || moveQuality.swingSeverity >= Thresholds.MISTAKE_CP
        case _ => false
    val latentPunishment =
      moveQuality.verdict match
        case MoveQualityVerdict.Blunder | MoveQualityVerdict.MissedWin =>
          !immediatePunishment && strategicOwnership.verifiedPayoffAnchor.nonEmpty
        case _ => false
    val conversionRoute =
      truthClass match
        case DecisiveTruthClass.WinningInvestment | DecisiveTruthClass.CompensatedInvestment =>
          strategicOwnership.verifiedPayoffAnchor
        case _ =>
          if strategicOwnership.convertsInvestment || afterPerspective >= Thresholds.MISTAKE_CP then
            strategicOwnership.verifiedPayoffAnchor
          else None
    val concessionSummary =
      moveQuality.verdict match
        case MoveQualityVerdict.Blunder | MoveQualityVerdict.MissedWin =>
          Some("hands over the initiative")
        case MoveQualityVerdict.Mistake | MoveQualityVerdict.Inaccuracy
            if strategicOwnership.verifiedPayoffAnchor.nonEmpty =>
          Some("concedes the cleaner route")
        case _ => None
    PunishConversionFact(
      immediatePunishment = immediatePunishment,
      latentPunishment = latentPunishment,
      conversionRoute = conversionRoute,
      concessionSummary = concessionSummary
    )

  private def projectTruthClass(
      moveQuality: MoveQualityFact,
      strategicOwnership: StrategicOwnershipFact,
      benchmark: BenchmarkFact,
      afterPerspective: Int
  ): DecisiveTruthClass =
    if strategicOwnership.truthPhase.contains(InvestmentTruthPhase.FirstInvestmentCommitment) &&
      benchmark.chosenMatchesBest &&
      strategicOwnership.verifiedPayoffAnchor.nonEmpty
    then
      if afterPerspective >= Thresholds.MISTAKE_CP then DecisiveTruthClass.WinningInvestment
      else DecisiveTruthClass.CompensatedInvestment
    else if strategicOwnership.truthPhase.contains(InvestmentTruthPhase.FirstInvestmentCommitment) &&
      !benchmark.chosenMatchesBest &&
      strategicOwnership.verifiedPayoffAnchor.nonEmpty &&
      moveQuality.cpLoss < Thresholds.BLUNDER_CP
    then DecisiveTruthClass.CompensatedInvestment
    else moveQuality.baselineTruthClass

  private def projectOwnershipRole(
      truthClass: DecisiveTruthClass,
      strategicOwnership: StrategicOwnershipFact
  ): TruthOwnershipRole =
    truthClass match
      case DecisiveTruthClass.Blunder | DecisiveTruthClass.MissedWin =>
        TruthOwnershipRole.BlunderOwner
      case _ =>
        strategicOwnership.truthPhase match
          case Some(InvestmentTruthPhase.FirstInvestmentCommitment)
              if strategicOwnership.verifiedPayoffAnchor.nonEmpty &&
                strategicOwnership.freshCommitmentCandidate &&
                strategicOwnership.ownerEligible =>
            TruthOwnershipRole.CommitmentOwner
          case Some(InvestmentTruthPhase.ConversionFollowthrough)
              if strategicOwnership.verifiedPayoffAnchor.nonEmpty || strategicOwnership.convertsInvestment =>
            TruthOwnershipRole.ConversionOwner
          case Some(InvestmentTruthPhase.CompensationMaintenance)
              if strategicOwnership.criticalMaintenance =>
            TruthOwnershipRole.MaintenanceEcho
          case _ =>
            TruthOwnershipRole.NoneRole

  private def projectExemplarRole(
      ownershipRole: TruthOwnershipRole,
      strategicOwnership: StrategicOwnershipFact,
      moveQuality: MoveQualityFact
  ): TruthExemplarRole =
    ownershipRole match
      case TruthOwnershipRole.CommitmentOwner if strategicOwnership.verifiedPayoffAnchor.nonEmpty =>
        TruthExemplarRole.VerifiedExemplar
      case TruthOwnershipRole.NoneRole
          if strategicOwnership.freshCommitmentCandidate &&
            strategicOwnership.verifiedPayoffAnchor.nonEmpty &&
            moveQuality.cpLoss < Thresholds.BLUNDER_CP =>
        TruthExemplarRole.ProvisionalExemplar
      case _ => TruthExemplarRole.NonExemplar

  private def projectVisibilityRole(
      ownershipRole: TruthOwnershipRole,
      exemplarRole: TruthExemplarRole,
      truthClass: DecisiveTruthClass,
      strategicOwnership: StrategicOwnershipFact
  ): TruthVisibilityRole =
    ownershipRole match
      case TruthOwnershipRole.BlunderOwner    => TruthVisibilityRole.PrimaryVisible
      case TruthOwnershipRole.CommitmentOwner => TruthVisibilityRole.PrimaryVisible
      case TruthOwnershipRole.ConversionOwner =>
        if strategicOwnership.verifiedPayoffAnchor.nonEmpty || strategicOwnership.convertsInvestment then
          TruthVisibilityRole.PrimaryVisible
        else TruthVisibilityRole.SupportingVisible
      case TruthOwnershipRole.MaintenanceEcho =>
        if strategicOwnership.criticalMaintenance then
          TruthVisibilityRole.SupportingVisible
        else TruthVisibilityRole.Hidden
      case TruthOwnershipRole.NoneRole =>
        if truthClass == DecisiveTruthClass.Best &&
          strategicOwnership.benchmarkCriticalMove &&
          strategicOwnership.reasonFamily == DecisiveReasonFamily.OnlyMoveDefense
        then TruthVisibilityRole.PrimaryVisible
        else exemplarRole match
          case TruthExemplarRole.ProvisionalExemplar => TruthVisibilityRole.SupportingVisible
          case _                                     => TruthVisibilityRole.Hidden

  private def projectSurfaceMode(
      ownershipRole: TruthOwnershipRole,
      truthClass: DecisiveTruthClass,
      strategicOwnership: StrategicOwnershipFact
  ): TruthSurfaceMode =
    ownershipRole match
      case TruthOwnershipRole.BlunderOwner =>
        TruthSurfaceMode.FailureExplain
      case TruthOwnershipRole.CommitmentOwner
          if strategicOwnership.verifiedPayoffAnchor.nonEmpty &&
            strategicOwnership.durablePressure &&
            (
              truthClass == DecisiveTruthClass.WinningInvestment ||
                truthClass == DecisiveTruthClass.CompensatedInvestment
            ) =>
        TruthSurfaceMode.InvestmentExplain
      case TruthOwnershipRole.MaintenanceEcho if strategicOwnership.criticalMaintenance =>
        TruthSurfaceMode.MaintenancePreserve
      case TruthOwnershipRole.ConversionOwner
          if strategicOwnership.verifiedPayoffAnchor.nonEmpty || strategicOwnership.convertsInvestment =>
        TruthSurfaceMode.ConversionExplain
      case _ =>
        TruthSurfaceMode.Neutral

  private def selectConcreteCarrierAnchor(
      strategyPack: Option[StrategyPack],
      verifiedPayoffAnchor: Option[String]
  ): Option[String] =
    matchedAnchorSignal(verifiedPayoffAnchor, strictCurrentCarrierSignals(strategyPack))

  private def allowsFailureIntentInterpretation(contract: DecisiveTruthContract): Boolean =
    contract.ownershipRole != TruthOwnershipRole.BlunderOwner || contract.failureInterpretationAllowed

  private def sanitizeFailureDigest(
      digest: Option[NarrativeSignalDigest],
      contract: DecisiveTruthContract
  ): Option[NarrativeSignalDigest] =
    if allowsFailureIntentInterpretation(contract) then digest
    else
      digest.map(
        _.copy(
          dominantIdeaKind = None,
          dominantIdeaGroup = None,
          dominantIdeaReadiness = None,
          dominantIdeaFocus = None,
          secondaryIdeaKind = None,
          secondaryIdeaGroup = None,
          secondaryIdeaFocus = None
        )
      )

  private def tacticalMotifId(fact: Fact): Option[String] =
    fact match
      case _: Fact.HangingPiece    => Some("hanging_piece")
      case _: Fact.TargetPiece     => Some("target_piece")
      case _: Fact.Pin             => Some("pin")
      case _: Fact.Skewer          => Some("skewer")
      case _: Fact.Fork            => Some("fork")
      case _: Fact.PawnPromotion   => Some("promotion_race")
      case _: Fact.StalemateThreat => Some("stalemate_resource")
      case _: Fact.DoubleCheck     => Some("double_check")
      case _: Fact.Zugzwang        => Some("zugzwang")
      case _: Fact.Opposition      => Some("opposition")
      case _                       => None

  private def moverPerspectiveCp(fen: String, cp: Int): Int =
    if fenSideToMoveIsWhite(fen) then cp else -cp

  private def fenSideToMoveIsWhite(fen: String): Boolean =
    Option(fen).getOrElse("").trim.split("\\s+").drop(1).headOption.contains("w")

  private def winPercentFromCp(cp: Int): Double =
    50.0 + 50.0 * (2.0 / (1.0 + math.exp(-WinPercentSlope * cp.toDouble)) - 1.0)

  private def normalizeMoveToken(raw: String): String =
    Option(raw).getOrElse("").trim.toLowerCase
      .replaceAll("""^\d+\.(?:\.\.)?\s*""", "")
      .replaceAll("""^\.{2,}\s*""", "")
      .replaceAll("""[+#?!]+$""", "")
      .replaceAll("\\s+", "")

  private def normalized(raw: String): Option[String] =
    Option(raw).map(_.trim).filter(_.nonEmpty)

  private def looksLikeUciMove(raw: String): Boolean =
    Option(raw).exists(_.trim.toLowerCase.matches("^[a-h][1-8][a-h][1-8][nbrq]?$"))

  private def isCompensationCarrierText(text: String): Boolean =
    val normalizedText = Option(text).getOrElse("").trim.toLowerCase
    normalizedText.nonEmpty && (
      normalizedText.contains("gives up material") ||
        normalizedText.contains("compensation carrier") ||
        normalizedText.contains("path to compensation") ||
        normalizedText.contains("recovering material later") ||
        normalizedText.contains("before recovering the material") ||
        normalizedText.contains("keep the material invested") ||
        normalizedText.contains("before winning the material back") ||
        normalizedText.contains("winning the material back") ||
        normalizedText.contains("the material can wait") ||
        normalizedText.contains("material can wait while") ||
        normalizedText.contains("open lines stay active")
    )

  private def hasLegacyDurableInvestmentCarrier(pack: StrategyPack): Boolean =
    val digest = pack.signalDigest
    val compensationSummary = digest.flatMap(_.compensation).flatMap(normalized)
    val investedMaterial = digest.flatMap(_.investedMaterial).filter(_ > 0)
    val vectorTexts =
      digest.toList.flatMap(_.compensationVectors).flatMap(normalized).map(_.toLowerCase)
    val supportingTexts =
      (pack.longTermFocus ++ pack.evidence).flatMap(normalized).map(_.toLowerCase)
    val initiativePressureVectors =
      vectorTexts.exists(_.contains("initiative")) &&
        vectorTexts.exists(_.contains("continuing pressure"))
    val deferredPayoffText =
      supportingTexts.exists(text =>
        text.contains("before winning the material back") ||
          text.contains("before recovering the material") ||
          text.contains("keep the material invested") ||
          text.contains("targets tied down") ||
          text.contains("fixed targets under pressure")
      )
    compensationSummary.nonEmpty && investedMaterial.nonEmpty && initiativePressureVectors && deferredPayoffText

  private final case class MaterialShift(
      beforeDeficit: Int,
      afterDeficit: Int,
      movingPieceValue: Int,
      capturedPieceValue: Int
  ):
    def deficitDelta: Int = afterDeficit - beforeDeficit
    def increasesDeficit: Boolean = deficitDelta >= 75
    def recoversDeficit: Boolean = beforeDeficit > 0 && afterDeficit <= beforeDeficit - 75
    def isValueDownCapture: Boolean = capturedPieceValue > 0 && movingPieceValue - capturedPieceValue >= 150

  private def currentAcceptedSemanticDecision(
      ctx: NarrativeContext
  ): Option[CompensationInterpretation.SemanticDecision] =
    CompensationInterpretation.currentSemanticDecision(ctx).filter(_.decision.accepted)

  private def afterAcceptedSemanticDecision(
      ctx: NarrativeContext
  ): Option[CompensationInterpretation.SemanticDecision] =
    CompensationInterpretation.afterSemanticDecision(ctx).filter(_.decision.accepted)

  private def selectVerifiedPayoffAnchor(
      moveLocalPayoffAnchor: Option[String],
      afterSemanticDecision: Option[CompensationInterpretation.Decision]
  ): Option[String] =
    val afterSignals =
      afterSemanticDecision.toList.flatMap { decision =>
        decision.signal.summary.toList ++ decision.signal.vectors
      }.flatMap(normalizeVerifiedPayoffAnchor).headOption
    moveLocalPayoffAnchor.orElse(afterSignals)

  private def normalizeVerifiedPayoffAnchor(raw: String): Option[String] =
    Option(raw)
      .map(UserFacingSignalSanitizer.sanitize)
      .map(_.trim.stripSuffix("."))
      .map(
        _.replaceFirst("(?i)^a path to compensation through\\s+", "")
          .replaceFirst("(?i)^compensation carrier:\\s*", "")
          .replaceFirst("(?i)^the move gives up material because\\s+", "")
          .replaceFirst("(?i)^the move gives up material to\\s+", "")
          .replaceFirst("(?i)^the compensation comes from\\s+", "")
          .replaceFirst("(?i)^the point is to\\s+", "")
          .replaceAll("\\s+", " ")
          .trim
      )
      .filter(isVerifiedPayoffAnchor)

  private def isVerifiedPayoffAnchor(text: String): Boolean =
    val lower = Option(text).getOrElse("").trim.toLowerCase
    val hasStrategicNoun =
      List(
        "pressure",
        "targets",
        "target",
        "file",
        "files",
        "open line",
        "open lines",
        "initiative",
        "attack",
        "counterplay",
        "conversion",
        "promotion",
        "passed pawn",
        "trade",
        "exchanges",
        "exchange",
        "break",
        "outpost",
        "king"
      ).exists(lower.contains)
    val genericShell =
      lower.isEmpty ||
        lower == "compensation" ||
        lower == "long term compensation" ||
        lower == "return vector" ||
        lower.contains("material can wait") ||
        lower.contains("winning the material back") ||
        lower.contains("recovering the material") ||
        lower.contains("recover material") ||
        lower.contains("open lines stay active") ||
        lower.contains("keep the material invested")
    hasStrategicNoun && !genericShell

  private def anchorSignalsMatch(anchor: String, signal: String): Boolean =
    val anchorTokens = anchorMatchTokens(anchor)
    val signalTokens = anchorMatchTokens(signal)
    anchorTokens.nonEmpty &&
      signalTokens.nonEmpty &&
      anchorTokens.intersect(signalTokens).size >= requiredAnchorOverlap(anchorTokens.size)

  private def anchorMatchTokens(raw: String): Set[String] =
    Option(raw)
      .map(_.toLowerCase.replaceAll("""[^a-z0-9\s]""", " ").replaceAll("""\s+""", " ").trim)
      .toList
      .flatMap(_.split(" ").toList)
      .filter(token => token.length > 1 && !AnchorMatchStopTokens.contains(token))
      .toSet

  private def requiredAnchorOverlap(anchorTokenCount: Int): Int =
    if anchorTokenCount <= 1 then 1 else 2

  private val AnchorMatchStopTokens = Set(
    "the",
    "and",
    "for",
    "with",
    "into",
    "from",
    "that",
    "this",
    "same",
    "more",
    "than",
    "while",
    "before",
    "after",
    "under",
    "over",
    "toward",
    "head",
    "can"
  )

  private def buildInvestmentTruthChainKey(
      verifiedPayoffAnchor: Option[String],
      reasonFamily: DecisiveReasonFamily,
      ctx: NarrativeContext
  ): String =
    val anchorKey =
      verifiedPayoffAnchor
        .map(_.toLowerCase.replaceAll("""[^a-z0-9\s]""", " ").replaceAll("""\s+""", " ").trim)
        .filter(_.nonEmpty)
        .getOrElse("no_anchor")
    val sideKey = if fenSideToMoveIsWhite(ctx.fen) then "white" else "black"
    s"$sideKey:${reasonFamily.toString.toLowerCase}:$anchorKey"

  private def materialShiftForMove(
      fenBefore: String,
      playedMoveUci: String
  ): Option[MaterialShift] =
    Fen.read(chess.variant.Standard, Fen.Full(fenBefore))
      .flatMap { pos =>
        Uci(playedMoveUci)
          .collect { case move: Uci.Move => move }
          .flatMap(pos.move(_).toOption)
          .map { move =>
            val mover = pos.color
            val movingPieceValue =
              pos.board.pieceAt(move.orig).map(piece => pieceValueCp(piece.role)).getOrElse(0)
            val capturedPieceValue =
              move.capture
                .flatMap(pos.board.roleAt)
                .orElse(pos.board.roleAt(move.dest))
                .map(pieceValueCp)
                .getOrElse(0)
            MaterialShift(
              beforeDeficit = sideMaterialDeficitCp(pos.board, mover),
              afterDeficit = sideMaterialDeficitCp(move.after.board, mover),
              movingPieceValue = movingPieceValue,
              capturedPieceValue = capturedPieceValue
            )
          }
      }

  private def sideMaterialDeficitCp(board: Board, side: Color): Int =
    val diff = materialDiffCp(board)
    if side.white then math.max(0, -diff) else math.max(0, diff)

  private def materialDiffCp(board: Board): Int =
    materialCp(board, Color.White) - materialCp(board, Color.Black)

  private def materialCp(board: Board, side: Color): Int =
    board.byPiece(side, Pawn).count * 100 +
      board.byPiece(side, Knight).count * 300 +
      board.byPiece(side, Bishop).count * 300 +
      board.byPiece(side, Rook).count * 500 +
      board.byPiece(side, Queen).count * 900

  private def pieceValueCp(role: Role): Int = role match
    case Pawn   => 100
    case Knight => 300
    case Bishop => 300
    case Rook   => 500
    case Queen  => 900
    case _      => 0

  private def normalizedWholeGameText(raw: String): Option[String] =
    Option(raw).map(_.trim.toLowerCase).filter(_.nonEmpty)
