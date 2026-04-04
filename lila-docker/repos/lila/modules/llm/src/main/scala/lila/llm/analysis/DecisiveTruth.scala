package lila.llm.analysis

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

  def sameMoveToken(left: String, right: String): Boolean =
    normalizeMoveToken(left) == normalizeMoveToken(right)

  private def normalizeMoveToken(raw: String): String =
    Option(raw).getOrElse("").trim.toLowerCase
      .replaceAll("""^\d+\.(?:\.\.)?\s*""", "")
      .replaceAll("""^\.{2,}\s*""", "")
      .replaceAll("""[+#?!]+$""", "")
      .replaceAll("\\s+", "")
