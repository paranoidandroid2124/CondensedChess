package lila.chessjudgment.model.judgment

import chess.Color
import lila.chessjudgment.analysis.position.PositionFeatures
import lila.chessjudgment.analysis.singlePosition.{ PawnPlayAnalysis, SinglePositionAssessment, ThreatAnalysis }
import lila.chessjudgment.analysis.structure.StructuralDelta
import lila.chessjudgment.model.{ ActivePlans, Fact, Motif, PlanScoringResult, PlanSequenceSummary }
import lila.chessjudgment.model.structure.{ PlanAlignment, StructureProfile }

final case class EvidenceSquare(key: String)
final case class EvidenceFile(key: String)
final case class EvidencePieceRole(name: String)

enum EvidenceSemanticAnchorKind:
  case StrategicKind
  case Plan
  case BoardAnchor
  case PawnStructure
  case StructurePlan
  case PawnPlay
  case OpeningAnchor
  case OpeningSupported
  case OpeningObserved
  case CandidateComparison
  case PlanPressure
  case PlanTransition
  case LineEvent
  case LineConsequence
  case StructuralDelta

final case class EvidenceSemanticAnchor(
    kind: EvidenceSemanticAnchorKind,
    values: List[String]
):
  def stableKey: String =
    (kind.toString :: values).mkString(":")

object EvidenceSemanticAnchor:
  def of(kind: EvidenceSemanticAnchorKind, values: String*): EvidenceSemanticAnchor =
    EvidenceSemanticAnchor(kind, values.toList)

enum RelationParticipantRole:
  case Attacker
  case Defender
  case Target
  case Blocker
  case Beneficiary
  case King
  case Mover
  case Bait
  case Lured
  case Other

final case class RelationParticipant(
    square: EvidenceSquare,
    role: Option[EvidencePieceRole],
    participantRole: RelationParticipantRole
)

enum RelationFactKind:
  case DefenderTrade
  case BadPieceLiquidation
  case Overload
  case Deflection
  case DiscoveredAttack
  case DoubleCheck
  case BackRankMate
  case MateNet
  case Fork
  case HangingPiece
  case Decoy
  case Interference
  case Clearance
  case XRay
  case Battery
  case Pin
  case Skewer
  case Zwischenzug
  case Domination
  case TrappedPiece
  case GreekGift
  case StalemateTrap
  case PerpetualCheck

object RelationFactKind:
  private val byId: Map[String, RelationFactKind] =
    Map(
      "defender_trade" -> DefenderTrade,
      "bad_piece_liquidation" -> BadPieceLiquidation,
      "overload" -> Overload,
      "deflection" -> Deflection,
      "discovered_attack" -> DiscoveredAttack,
      "double_check" -> DoubleCheck,
      "back_rank_mate" -> BackRankMate,
      "mate_net" -> MateNet,
      "fork" -> Fork,
      "hanging_piece" -> HangingPiece,
      "decoy" -> Decoy,
      "interference" -> Interference,
      "clearance" -> Clearance,
      "xray" -> XRay,
      "battery" -> Battery,
      "pin" -> Pin,
      "skewer" -> Skewer,
      "zwischenzug" -> Zwischenzug,
      "domination" -> Domination,
      "trapped_piece" -> TrappedPiece,
      "greek_gift" -> GreekGift,
      "stalemate_trap" -> StalemateTrap,
      "perpetual_check" -> PerpetualCheck
    )

  def fromId(raw: String): Option[RelationFactKind] =
    byId.get(Option(raw).getOrElse("").trim.toLowerCase)

enum StrategicFactKind:
  case Outpost
  case FileControl
  case Space
  case CounterplayRestraint
  case TargetFixation
  case Structure
  case Endgame
  case Activity
  case Compensation
  case Practicality
  case PlanPressure

sealed trait EvidencePayload:
  def layer: EvidenceLayer

final case class BoardFactEvidence(
    private val facts: List[Fact],
    private val features: Option[PositionFeatures]
)(private val anchors: List[BoardAnchor] = Nil,
    private val attackDefense: List[BoardAttackDefenseEntry] = Nil,
    private val profile: Option[BoardPositionProfile] = None
) extends EvidencePayload:
  val layer: EvidenceLayer = EvidenceLayer.Board
  def boardAnchors: List[BoardAnchor] =
    anchors
  def boardAnchorCount: Int =
    anchors.size
  def hasBoardAnchors: Boolean =
    anchors.nonEmpty
  def anchorsOf(kind: BoardAnchorKind): List[BoardAnchor] =
    anchors.filter(_.kind == kind)
  def anchorsOfAny(kinds: Set[BoardAnchorKind]): List[BoardAnchor] =
    anchors.filter(anchor => kinds.contains(anchor.kind))
  def anchorsOfAtLeast(kind: BoardAnchorKind, minimumMagnitude: Int): List[BoardAnchor] =
    anchors.filter(anchor => anchor.kind == kind && anchor.magnitude >= minimumMagnitude)
  def semanticGroupingAnchors: List[EvidenceSemanticAnchor] =
    anchors.map(_.semanticGroupingAnchor)
  def proofSignalAnchors: List[BoardAnchor] =
    anchors.filter(BoardFactEvidence.isProofSignalAnchor)
  def proofSignalAnchorKinds: List[BoardAnchorKind] =
    proofSignalAnchors.map(_.kind).distinct
  def hasProofSignalAnchor(kind: BoardAnchorKind): Boolean =
    proofSignalAnchors.exists(_.kind == kind)
  def looseMaterialAnchors: List[BoardAnchor] =
    anchorsOf(BoardAnchorKind.LooseMaterial)
  def outpostAnchors: List[BoardAnchor] =
    anchorsOf(BoardAnchorKind.Outpost)
  def fileControlAnchors: List[BoardAnchor] =
    anchorsOf(BoardAnchorKind.FileControl)
  def spaceAnchors: List[BoardAnchor] =
    anchorsOf(BoardAnchorKind.Space)
  def activityAnchors: List[BoardAnchor] =
    anchorsOfAny(Set(BoardAnchorKind.Activity, BoardAnchorKind.CounterplayRestraint))
  def counterplayRestraintAnchors: List[BoardAnchor] =
    anchorsOfAtLeast(BoardAnchorKind.CounterplayRestraint, minimumMagnitude = 3)
  def endgameTechniqueAnchors: List[BoardAnchor] =
    anchorsOf(BoardAnchorKind.EndgameTechnique)
  def openingContextAnchors: List[BoardAnchor] =
    anchorsOfAny(
      Set(
        BoardAnchorKind.CenterControl,
        BoardAnchorKind.Space,
        BoardAnchorKind.Development,
        BoardAnchorKind.Activity,
        BoardAnchorKind.PawnStructure,
        BoardAnchorKind.KingSafety
      )
    )
  def anchorFocusSquares: List[EvidenceSquare] =
    anchors.flatMap(_.focusSquares).distinct
  def boardProfile: Option[BoardPositionProfile] =
    profile
  def factCount: Int =
    facts.size
  def hasBoardProfile: Boolean =
    profile.nonEmpty
  def hasAttackDefenseEntries: Boolean =
    attackDefense.nonEmpty
  def attackDefenseCount: Int =
    attackDefense.size
  def vulnerableAttackDefense: List[BoardAttackDefenseEntry] =
    attackDefense.filter(entry => entry.isLoose || entry.isUnderdefended)
  def targetHintSquares: List[EvidenceSquare] =
    val anchorSquares =
      anchors.flatMap(_.targetHintSquares)
    val materialSquares =
      vulnerableAttackDefense.map(_.square)
    (anchorSquares ++ materialSquares).distinct

object BoardFactEvidence:
  def apply(facts: List[Fact], features: Option[PositionFeatures]): BoardFactEvidence =
    new BoardFactEvidence(facts, features)()

  private[chessjudgment] def isProofSignalAnchor(anchor: BoardAnchor): Boolean =
    anchor.kind match
      case BoardAnchorKind.LooseMaterial =>
        anchor.signal == BoardAnchorSignal.HangingPiece ||
          anchor.detail.exists(_.defenderSquares.isEmpty)
      case BoardAnchorKind.PinPressure =>
        anchor.detail.flatMap(_.isAbsolute).contains(true) || anchor.magnitude > 1
      case BoardAnchorKind.SkewerPressure | BoardAnchorKind.ForkPressure | BoardAnchorKind.XRayPressure |
          BoardAnchorKind.Outpost =>
        true
      case BoardAnchorKind.BatteryPressure =>
        anchor.detail.flatMap(_.axis).contains(BoardAnchorAxis.Diagonal)
      case BoardAnchorKind.CenterControl | BoardAnchorKind.Space | BoardAnchorKind.Development |
          BoardAnchorKind.FileControl | BoardAnchorKind.Activity | BoardAnchorKind.CounterplayRestraint |
          BoardAnchorKind.KingSafety | BoardAnchorKind.PawnStructure | BoardAnchorKind.WeakSquare |
          BoardAnchorKind.EndgameTechnique =>
        false

final case class BoardAttackDefenseEntry(
    square: EvidenceSquare,
    occupantColor: Color,
    occupantRole: EvidencePieceRole,
    attackerColor: Color,
    attackerSquares: List[EvidenceSquare],
    defenderSquares: List[EvidenceSquare],
    attackCount: Int,
    defenseCount: Int,
    pressureDelta: Int,
    materialValueCp: Int,
    isLoose: Boolean,
    isUnderdefended: Boolean
)

final case class BoardPositionProfile(
    centerLocked: Boolean,
    centerOpen: Boolean,
    pawnTensionCount: Int,
    whiteCenterControl: Int,
    blackCenterControl: Int,
    whiteCentralPawns: Int,
    blackCentralPawns: Int,
    spaceDiff: Int,
    whiteDevelopmentLag: Int,
    blackDevelopmentLag: Int,
    whiteLowMobilityPieces: Int,
    blackLowMobilityPieces: Int,
    whiteKingExposure: Int,
    blackKingExposure: Int,
    whitePawnWeaknesses: Int,
    blackPawnWeaknesses: Int,
    whitePassedPawns: Int,
    blackPassedPawns: Int,
    whiteEntrenchedPieces: Int,
    blackEntrenchedPieces: Int,
    whiteRookPawnMarchReady: Boolean,
    blackRookPawnMarchReady: Boolean,
    whiteHookCreationChance: Boolean,
    blackHookCreationChance: Boolean,
    whiteColorComplexClamp: Boolean,
    blackColorComplexClamp: Boolean,
    hasStrategicSnapshot: Boolean
):
  def centerControlEdgeFor(side: Color): Int =
    if side.white then whiteCenterControl - blackCenterControl
    else blackCenterControl - whiteCenterControl
  def centralPawnsFor(side: Color): Int =
    if side.white then whiteCentralPawns else blackCentralPawns
  def spaceFor(side: Color): Int =
    if side.white then spaceDiff else -spaceDiff
  def developmentLagFor(side: Color): Int =
    if side.white then whiteDevelopmentLag else blackDevelopmentLag
  def lowMobilityFor(side: Color): Int =
    if side.white then whiteLowMobilityPieces else blackLowMobilityPieces
  def kingExposureFor(side: Color): Int =
    if side.white then whiteKingExposure else blackKingExposure
  def opponentPawnWeaknessFor(side: Color): Int =
    if side.white then blackPawnWeaknesses else whitePawnWeaknesses
  def passedPawnsFor(side: Color): Int =
    if side.white then whitePassedPawns else blackPassedPawns
  def opponentPassedPawnsFor(side: Color): Int =
    if side.white then blackPassedPawns else whitePassedPawns
  def entrenchedPiecesFor(side: Color): Int =
    if side.white then whiteEntrenchedPieces else blackEntrenchedPieces
  def rookPawnReadyFor(side: Color): Boolean =
    if side.white then whiteRookPawnMarchReady else blackRookPawnMarchReady
  def hookChanceFor(side: Color): Boolean =
    if side.white then whiteHookCreationChance else blackHookCreationChance
  def colorComplexClampFor(side: Color): Boolean =
    if side.white then whiteColorComplexClamp else blackColorComplexClamp

enum BoardAnchorKind:
  case CenterControl
  case Space
  case Development
  case FileControl
  case Activity
  case CounterplayRestraint
  case KingSafety
  case PawnStructure
  case LooseMaterial
  case PinPressure
  case SkewerPressure
  case ForkPressure
  case XRayPressure
  case BatteryPressure
  case WeakSquare
  case Outpost
  case EndgameTechnique

enum BoardAnchorSignal:
  case CenterControlEdge
  case SpaceEdge
  case DevelopmentLead
  case OpenFileAccess
  case SemiOpenFileAccess
  case RookOnSeventh
  case MobilityEdge
  case OpponentLowMobility
  case KingExposure
  case KingPressure
  case PawnStructureShape
  case HangingPiece
  case AttackedTarget
  case AbsolutePin
  case RelativePin
  case SkewerLine
  case ForkTargets
  case XRayLine
  case BatteryLine
  case WeakSquareHole
  case OutpostSquare
  case EndgameKingActivity
  case EndgameOpposition
  case EndgameRuleOfSquare
  case EndgameTriangulation
  case EndgameRookPattern
  case EndgameOutcomeHint
  case EndgameZugzwang
  case EndgamePromotion
  case EndgameStalemateResource

enum BoardAnchorAxis:
  case File
  case Rank
  case Diagonal

final case class BoardAnchorDetail(
    subjectColor: Option[Color] = None,
    subjectSquare: Option[EvidenceSquare] = None,
    subjectRole: Option[EvidencePieceRole] = None,
    targetSquare: Option[EvidenceSquare] = None,
    targetRole: Option[EvidencePieceRole] = None,
    attackerColor: Option[Color] = None,
    attackerSquare: Option[EvidenceSquare] = None,
    attackerRole: Option[EvidencePieceRole] = None,
    attackerSquares: List[EvidenceSquare] = Nil,
    defenderSquares: List[EvidenceSquare] = Nil,
    relatedSquares: List[EvidenceSquare] = Nil,
    file: Option[EvidenceFile] = None,
    axis: Option[BoardAnchorAxis] = None,
    isAbsolute: Option[Boolean] = None,
    materialLossCp: Option[Int] = None
):
  def focusSquares: List[EvidenceSquare] =
    (
      subjectSquare.toList ++
        targetSquare.toList ++
        attackerSquare.toList ++
        attackerSquares ++
        defenderSquares ++
        relatedSquares
    ).distinct
  def targetHintSquares: List[EvidenceSquare] =
    (
      targetSquare.toList ++
        subjectSquare.toList ++
        relatedSquares
    ).distinct

final case class BoardAnchor(
    kind: BoardAnchorKind,
    side: Color,
    signal: BoardAnchorSignal,
    magnitude: Int,
    confidence: Double,
    detail: Option[BoardAnchorDetail] = None
):
  def focusSquares: List[EvidenceSquare] =
    detail.toList.flatMap(_.focusSquares).distinct
  def targetHintSquares: List[EvidenceSquare] =
    detail.toList.flatMap(_.targetHintSquares).distinct
  def semanticGroupingAnchor: EvidenceSemanticAnchor =
    val sideKey = if side.white then "white" else "black"
    val detailValues =
      detail.toList.flatMap(detail =>
        List(
          detail.subjectColor.map(color => s"subject-color:${colorKey(color)}"),
          detail.attackerColor.map(color => s"attacker-color:${colorKey(color)}"),
          detail.subjectSquare.map(square => s"subject-square:${square.key}"),
          detail.targetSquare.map(square => s"target-square:${square.key}"),
          Option
            .when(detail.relatedSquares.nonEmpty)(s"related:${detail.relatedSquares.map(_.key).sorted.mkString(",")}"),
          detail.file.map(file => s"file:${file.key}"),
          detail.axis.map(axis => s"axis:$axis")
        ).flatten
      )
    EvidenceSemanticAnchor.of(
      EvidenceSemanticAnchorKind.BoardAnchor,
      (List(sideKey, kind.toString, signal.toString) ++ detailValues)*
    )

  private def colorKey(color: Color): String =
    if color.white then "white" else "black"

final case class SinglePositionEvidence(
    assessment: SinglePositionAssessment
) extends EvidencePayload:
  val layer: EvidenceLayer = EvidenceLayer.SinglePosition

final case class PawnStructureFactEvidence(
    profile: StructureProfile,
    alignment: Option[PlanAlignment],
    pawnPlay: Option[PawnPlayAnalysis]
) extends EvidencePayload:
  val layer: EvidenceLayer = EvidenceLayer.PawnStructure

final case class StrategicFactEvidence(
    kind: StrategicFactKind,
    facts: List[Fact],
    relatedPlans: List[lila.chessjudgment.model.PlanId],
    confidence: Double
)(val boardAnchors: List[BoardAnchor] = Nil
) extends EvidencePayload:
  val layer: EvidenceLayer = EvidenceLayer.Strategic
  def hasTypedSupport: Boolean =
    facts.nonEmpty || relatedPlans.nonEmpty || boardAnchors.nonEmpty
  def semanticGroupingAnchors: List[EvidenceSemanticAnchor] =
    boardAnchors.map(_.semanticGroupingAnchor)

enum OpeningFamily:
  case A
  case B
  case C
  case D
  case E

object OpeningFamily:
  def fromEco(raw: String): Option[OpeningFamily] =
    Option(raw)
      .map(_.trim.toUpperCase)
      .flatMap(_.headOption)
      .flatMap(ch => fromRaw(ch.toString))

  def fromOpeningName(raw: String): Option[OpeningFamily] =
    val name = Option(raw).map(_.trim.toLowerCase).getOrElse("")
    Option.when(name.nonEmpty)(name).flatMap { value =>
      if value.contains("sicilian") ||
        value.contains("caro-kann") ||
        value.contains("scandinavian") ||
        value.contains("alekhine") ||
        value.contains("pirc") ||
        value.contains("modern defense") ||
        value.contains("nimzowitsch") ||
        value.contains("owen") ||
        value.contains("english defense")
      then Some(OpeningFamily.B)
      else if value.contains("french") ||
        value.contains("king's pawn") ||
        value.contains("italian") ||
        value.contains("ruy lopez") ||
        value.contains("spanish") ||
        value.contains("four knights") ||
        value.contains("vienna") ||
        value.contains("scotch") ||
        value.contains("petrov") ||
        value.contains("philidor") ||
        value.contains("king's gambit")
      then Some(OpeningFamily.C)
      else if value.contains("queen's gambit") ||
        value.contains("slav") ||
        value.contains("semi-slav") ||
        value.contains("tarrasch") ||
        value.contains("queen's pawn")
      then Some(OpeningFamily.D)
      else if value.contains("indian") ||
        value.contains("nimzo-indian") ||
        value.contains("gruenfeld") ||
        value.contains("grünfeld") ||
        value.contains("catalan") ||
        value.contains("bogo-indian") ||
        value.contains("queen's indian")
      then Some(OpeningFamily.E)
      else if value.contains("english opening") ||
        value.contains("reti") ||
        value.contains("réti") ||
        value.contains("dutch") ||
        value.contains("benoni") ||
        value.contains("benko") ||
        value.contains("bird") ||
        value.contains("polish")
      then Some(OpeningFamily.A)
      else None
    }

  def fromRaw(raw: String): Option[OpeningFamily] =
    Option(raw).map(_.trim.toUpperCase).collect:
      case "A" => OpeningFamily.A
      case "B" => OpeningFamily.B
      case "C" => OpeningFamily.C
      case "D" => OpeningFamily.D
      case "E" => OpeningFamily.E

enum OpeningContextSignal:
  case InputIdentity
  case RecognizedIdentity
  case OpeningPhase
  case ThemePrior

enum OpeningTheme:
  case CenterControl
  case Development
  case PawnStructure
  case GambitInitiative
  case KingSafety
  case PlanPressure

enum FeatureAnchorSignal:
  case CenterControlObserved
  case DevelopmentTempoObserved
  case PawnStructureObserved
  case CompensationObserved
  case KingSafetyObserved
  case PlanPressureObserved
  case StructuralDeltaObserved

final case class FeatureAnchor(
    theme: OpeningTheme,
    signal: FeatureAnchorSignal,
    sourceLayer: EvidenceLayer,
    strength: Double
):
  def isBoardObservation: Boolean =
    sourceLayer == EvidenceLayer.Board
  def canCorroborateOpeningPrior: Boolean =
    !isBoardObservation
  def hasPositiveStrength: Boolean =
    strength > 0.0

enum FeatureApplicability:
  case OpeningRelevant
  case MiddlegameRelevant
  case EndgameRelevant
  case ObservedOnly
  case Contraindicated

enum ApplicabilityStatus:
  case InternalOnly
  case Supported
  case PartiallySupported
  case Unverified
  case Ambiguous
  case Contradicted

final case class ApplicabilityAssessment(
    applicability: FeatureApplicability,
    status: ApplicabilityStatus,
    observedThemes: List[OpeningTheme],
    supportedThemes: List[OpeningTheme],
    unverifiedPriorThemes: List[OpeningTheme],
    observedOnlyThemes: List[OpeningTheme]
):
  def hasInternalAnchorAlignment: Boolean =
    applicability == FeatureApplicability.OpeningRelevant &&
      observedThemes.nonEmpty &&
      supportedThemes.nonEmpty &&
      (status == ApplicabilityStatus.Supported || status == ApplicabilityStatus.PartiallySupported)

  def canCertifyOpeningClaim: Boolean =
    hasInternalAnchorAlignment

final case class OpeningIdentity(
    eco: Option[String],
    name: Option[String],
    family: Option[OpeningFamily]
)

final case class OpeningCandidate(
    identity: OpeningIdentity,
    lineage: Option[String],
    frequency: Int,
    sampleCount: Int,
    confidence: Double
)

enum OpeningRecognitionMatchKind:
  case ExactPrefixAndPosition
  case PositionTransposition

final case class OpeningRecognition(
    movePrefixHash: String,
    positionKey: String,
    matchedBy: OpeningRecognitionMatchKind,
    candidates: List[OpeningCandidate],
    matchedPly: Int,
    frequency: Int,
    sampleCount: Int,
    confidence: Double
):
  def bestCandidate: Option[OpeningCandidate] =
    candidates.headOption

  def bestIdentity: Option[OpeningIdentity] =
    bestCandidate.map(_.identity)

  def lineage: Option[String] =
    bestCandidate.flatMap(_.lineage)

final case class OpeningThemePrior(
    lineage: Option[String],
    family: Option[OpeningFamily],
    themes: List[OpeningTheme],
    typicalPawnStructures: List[String],
    centerBreaks: List[String],
    developmentPriorities: List[String],
    gambitCompensation: Boolean,
    strategicPlanPriors: List[String]
)

final case class OpeningContextEvidence(
    identity: Option[OpeningIdentity],
    signals: List[OpeningContextSignal],
    recognition: Option[OpeningRecognition] = None,
    themePrior: Option[OpeningThemePrior] = None
) extends EvidencePayload:
  val layer: EvidenceLayer = EvidenceLayer.OpeningContext

final case class FeatureAnchorEvidence(
    anchor: FeatureAnchor
) extends EvidencePayload:
  val layer: EvidenceLayer = EvidenceLayer.FeatureAnchor

final case class ApplicabilityAssessmentEvidence(
    assessment: ApplicabilityAssessment
) extends EvidencePayload:
  val layer: EvidenceLayer = EvidenceLayer.ApplicabilityAssessment

final case class ThreatPressureEvidence(
    sideUnderPressure: Color,
    threats: ThreatAnalysis
) extends EvidencePayload:
  val layer: EvidenceLayer = EvidenceLayer.ThreatPressure

final case class ForcedLineThemeEvidence(
    id: String,
    lineMoves: List[String]
)

final case class LineReplayStep(
    ply: Int,
    moveUci: String,
    fenBefore: String,
    fenAfter: String
)

enum LineEventKind:
  case Capture
  case Recapture
  case DefenderMove
  case Threat
  case Castling
  case Check
  case Mate
  case Tempo
  case Stalemate
  case Promotion
  case ForcedTheme

final case class LineMoveEvent(
    kind: LineEventKind,
    moveUci: String,
    plyOffset: Int,
    side: Option[Color] = None,
    pieceRole: Option[EvidencePieceRole] = None,
    targetRole: Option[EvidencePieceRole] = None,
    square: Option[EvidenceSquare] = None
)

enum LineConsequenceKind:
  case ForcedTheme
  case ImmediateReplyCheck
  case Mate
  case DrawResource
  case MaterialGain
  case MaterialLoss
  case RecaptureSequence
  case RecoveryWindow
  case Sacrifice
  case PromotionRace

enum LineMaterialOutcomeSignal:
  case MoverCapture
  case OpponentCapture
  case PromotionGain
  case PromotionLoss
  case UnrecoveredPawnGain
  case UnrecoveredPawnLoss
  case RecoveryWindow

enum LineMaterialOutcomeMagnitude:
  case None
  case Pawn
  case Piece

final case class LineConsequence(
    kind: LineConsequenceKind,
    lineMoves: List[String],
    proofSignal: Boolean,
    eventMove: Option[String] = None
)

final case class LineConsequenceProfile(
    proofSignalKinds: List[LineConsequenceKind],
    hasConcreteProofSignal: Boolean,
    hasConversionConsequence: Boolean,
    hasMaterialResult: Boolean,
    hasRecaptureRecovery: Boolean,
    hasSacrifice: Boolean,
    hasPromotionRace: Boolean,
    hasMate: Boolean,
    hasDrawResource: Boolean
):
  def tacticalDriverKinds: List[LineConsequenceKind] =
    proofSignalKinds.filter {
      case LineConsequenceKind.MaterialGain | LineConsequenceKind.MaterialLoss |
          LineConsequenceKind.RecaptureSequence | LineConsequenceKind.RecoveryWindow |
          LineConsequenceKind.ImmediateReplyCheck | LineConsequenceKind.Mate |
          LineConsequenceKind.DrawResource | LineConsequenceKind.PromotionRace =>
        true
      case LineConsequenceKind.ForcedTheme | LineConsequenceKind.Sacrifice =>
        false
    }

final case class LineMaterialOutcomeProfile(
    gainSignals: Set[LineMaterialOutcomeSignal],
    lossSignals: Set[LineMaterialOutcomeSignal]
):
  def merge(other: LineMaterialOutcomeProfile): LineMaterialOutcomeProfile =
    LineMaterialOutcomeProfile(
      gainSignals = gainSignals ++ other.gainSignals,
      lossSignals = lossSignals ++ other.lossSignals
    )

  def gainMagnitude: LineMaterialOutcomeMagnitude =
    if gainSignals.exists(signal =>
        signal == LineMaterialOutcomeSignal.MoverCapture ||
          signal == LineMaterialOutcomeSignal.PromotionGain ||
          signal == LineMaterialOutcomeSignal.RecoveryWindow
      )
    then LineMaterialOutcomeMagnitude.Piece
    else if gainSignals.contains(LineMaterialOutcomeSignal.UnrecoveredPawnGain) then LineMaterialOutcomeMagnitude.Pawn
    else LineMaterialOutcomeMagnitude.None

  def lossMagnitude: LineMaterialOutcomeMagnitude =
    if lossSignals.exists(signal =>
        signal == LineMaterialOutcomeSignal.OpponentCapture ||
          signal == LineMaterialOutcomeSignal.PromotionLoss
      )
    then LineMaterialOutcomeMagnitude.Piece
    else if lossSignals.contains(LineMaterialOutcomeSignal.UnrecoveredPawnLoss) then LineMaterialOutcomeMagnitude.Pawn
    else LineMaterialOutcomeMagnitude.None

object LineMaterialOutcomeProfile:
  val empty: LineMaterialOutcomeProfile =
    LineMaterialOutcomeProfile(Set.empty, Set.empty)

final case class LineMaterialCapture(
    moveUci: String,
    plyOffset: Int,
    side: Color,
    attackerRole: EvidencePieceRole,
    capturedRole: EvidencePieceRole,
    square: EvidenceSquare,
    valueCp: Int,
    recapture: Boolean
)

final case class LineMaterialSummary(
    sideToMove: Color,
    captures: List[LineMaterialCapture],
    netCaptureCpForMover: Int,
    maxGainCpForMover: Int,
    maxLossCpForMover: Int,
    hasRecaptureChain: Boolean,
    hasRecoveryWindow: Boolean,
    promotionGainCpForMover: Int,
    materialWindowComplete: Boolean
):
  def hasPromotion: Boolean = promotionGainCpForMover != 0

  def capturesByMover: List[LineMaterialCapture] =
    captures.filter(_.side == sideToMove)

  def capturesByOpponent: List[LineMaterialCapture] =
    captures.filter(_.side != sideToMove)

  def nonPawnCapturesByMover: List[LineMaterialCapture] =
    capturesByMover.filter(capture => proofSignalCapturedRole(capture.capturedRole))

  def nonPawnCapturesByOpponent: List[LineMaterialCapture] =
    capturesByOpponent.filter(capture => proofSignalCapturedRole(capture.capturedRole))

  def pawnCapturesByMover: List[LineMaterialCapture] =
    capturesByMover.filter(capture => pawnCapturedRole(capture.capturedRole))

  def pawnCapturesByOpponent: List[LineMaterialCapture] =
    capturesByOpponent.filter(capture => pawnCapturedRole(capture.capturedRole))

  def hasPromotionGainForMover: Boolean =
    promotionGainCpForMover > 0

  def hasPromotionLossForMover: Boolean =
    promotionGainCpForMover < 0

  def hasResolvedMaterialSequence: Boolean =
    materialWindowComplete && (hasRecaptureChain || hasRecoveryWindow)

  def hasProofSignalMaterialGain: Boolean =
    materialWindowComplete && (nonPawnCapturesByMover.nonEmpty || hasPromotionGainForMover || hasRecoveryWindow)

  def hasProofSignalMaterialLoss: Boolean =
    materialWindowComplete && (nonPawnCapturesByOpponent.nonEmpty || hasPromotionLossForMover)

  def hasUnrecoveredPawnGainForMover: Boolean =
    materialWindowComplete && pawnCapturesByMover.nonEmpty && !hasRecoveryWindow

  def hasUnrecoveredPawnLossForMover: Boolean =
    materialWindowComplete && pawnCapturesByOpponent.nonEmpty && !hasRecoveryWindow

  def hasProofSignalMaterialEvent: Boolean =
    hasProofSignalMaterialGain ||
      hasProofSignalMaterialLoss ||
      hasUnrecoveredPawnGainForMover ||
      hasUnrecoveredPawnLossForMover ||
      hasResolvedMaterialSequence

  def hasSacrificeMaterialEvent: Boolean =
    materialWindowComplete &&
      capturesByOpponent.exists(capture => !capture.recapture) &&
      !hasRecoveryWindow &&
      !hasProofSignalMaterialGain

  private def proofSignalCapturedRole(role: EvidencePieceRole): Boolean =
    val normalized = role.name.trim.toLowerCase
    normalized.nonEmpty && normalized != "pawn" && normalized != "king"

  private def pawnCapturedRole(role: EvidencePieceRole): Boolean =
    role.name.trim.equalsIgnoreCase("pawn")

final case class LineFactEvidence(
    line: LineNodeRef,
    private val firstMove: Option[String],
    private val replyMove: Option[String],
    private val continuationMoves: List[String],
    private val forcedTheme: Option[ForcedLineThemeEvidence] = None,
    private val material: Option[LineMaterialSummary] = None
)(private val replay: List[LineReplayStep] = Nil,
    private val events: List[LineMoveEvent] = Nil,
    private val consequences: List[LineConsequence] = Nil
) extends EvidencePayload:
  val layer: EvidenceLayer = EvidenceLayer.Line
  def rootMove: Option[String] =
    firstMove
  def reply: Option[String] =
    replyMove
  def continuation: List[String] =
    continuationMoves
  def forcedThemeId: Option[String] =
    forcedTheme.map(_.id)
  def lineReplaySteps: List[LineReplayStep] =
    replay
  def lineReplayMoves: List[String] =
    replay.map(_.moveUci)
  def lineReplayContinuationMoves: List[String] =
    lineReplayMoves.drop(1)
  def lineEvents: List[LineMoveEvent] =
    events
  def lineConsequences: List[LineConsequence] =
    consequences
  def lineReplayCount: Int =
    replay.size
  def hasLineReplay: Boolean =
    replay.nonEmpty
  def lineEventKinds: List[LineEventKind] =
    events.map(_.kind)
  def lineEventsOf(kind: LineEventKind): List[LineMoveEvent] =
    events.filter(_.kind == kind)
  def hasLineEvent(kind: LineEventKind): Boolean =
    lineEventsOf(kind).nonEmpty
  def hasLineEventAt(kind: LineEventKind, plyOffset: Int): Boolean =
    lineEventsOf(kind).exists(_.plyOffset == plyOffset)
  def hasTempoEventAt(plyOffset: Int): Boolean =
    hasLineEventAt(LineEventKind.Tempo, plyOffset)
  def lineEventMoves(kind: LineEventKind): List[String] =
    lineEventsOf(kind).map(_.moveUci)
  def hasRootCaptureEvent(rootMoveUci: String): Boolean =
    val normalizedRoot = normalizeUci(rootMoveUci)
    events.exists(event =>
      (event.kind == LineEventKind.Capture || event.kind == LineEventKind.Recapture) &&
        normalizeUci(event.moveUci) == normalizedRoot
    )
  def lineEventCount: Int =
    events.size
  def hasLineEvents: Boolean =
    events.nonEmpty
  def lineConsequenceCount: Int =
    consequences.size
  def hasLineConsequences: Boolean =
    consequences.nonEmpty
  def hasForcedTheme: Boolean =
    forcedTheme.nonEmpty
  def materialNetCaptureCpForMover: Option[Int] =
    material.map(_.netCaptureCpForMover)
  def materialMaxGainCpForMover: Option[Int] =
    material.map(_.maxGainCpForMover)
  def materialPromotionGainCpForMover: Option[Int] =
    material.map(_.promotionGainCpForMover)
  def hasMaterialRecaptureChain: Boolean =
    material.exists(_.hasRecaptureChain)
  def hasMaterialRecoveryWindow: Boolean =
    material.exists(_.hasRecoveryWindow)
  def hasCompleteMaterialWindow: Boolean =
    material.forall(_.materialWindowComplete)
  def hasProofSignalMaterialEvent: Boolean =
    material.exists(_.hasProofSignalMaterialEvent)
  def hasSacrificeMaterialEvent: Boolean =
    material.exists(_.hasSacrificeMaterialEvent)
  def proofSignalConsequences: List[LineConsequence] =
    consequences.filter(_.proofSignal)
  def hasProofSignalConsequence: Boolean =
    proofSignalConsequences.nonEmpty
  def proofSignalConsequenceKinds: List[LineConsequenceKind] =
    proofSignalConsequences.map(_.kind)
  def hasProofSignalConsequence(kind: LineConsequenceKind): Boolean =
    proofSignalConsequenceKinds.contains(kind)
  def consequenceProfile: LineConsequenceProfile =
    val kinds = proofSignalConsequenceKinds
    LineConsequenceProfile(
      proofSignalKinds = kinds,
      hasConcreteProofSignal = kinds.nonEmpty,
      hasConversionConsequence = kinds.exists {
        case LineConsequenceKind.RecaptureSequence | LineConsequenceKind.RecoveryWindow |
            LineConsequenceKind.MaterialGain | LineConsequenceKind.MaterialLoss |
            LineConsequenceKind.Sacrifice =>
          true
        case _ =>
          false
      },
      hasMaterialResult = kinds.exists {
        case LineConsequenceKind.MaterialGain | LineConsequenceKind.MaterialLoss |
            LineConsequenceKind.Sacrifice | LineConsequenceKind.PromotionRace =>
          true
        case _ =>
          false
      },
      hasRecaptureRecovery = kinds.exists(kind =>
        kind == LineConsequenceKind.RecaptureSequence || kind == LineConsequenceKind.RecoveryWindow
      ),
      hasSacrifice = kinds.contains(LineConsequenceKind.Sacrifice),
      hasPromotionRace = kinds.contains(LineConsequenceKind.PromotionRace),
      hasMate = kinds.contains(LineConsequenceKind.Mate),
      hasDrawResource = kinds.contains(LineConsequenceKind.DrawResource)
    )
  def hasConcreteLineConsequence: Boolean =
    consequenceProfile.hasConcreteProofSignal
  def hasConversionConsequence: Boolean =
    consequenceProfile.hasConversionConsequence
  def hasMaterialConsequence: Boolean =
    consequenceProfile.hasMaterialResult
  def hasRecaptureRecoveryConsequence: Boolean =
    consequenceProfile.hasRecaptureRecovery
  def hasSacrificeConsequence: Boolean =
    consequenceProfile.hasSacrifice
  def tacticalLineConsequenceKinds: List[LineConsequenceKind] =
    consequenceProfile.tacticalDriverKinds
  def hasTacticalLineConsequence: Boolean =
    tacticalLineConsequenceKinds.nonEmpty
  def semanticGroupingAnchors: List[EvidenceSemanticAnchor] =
    Option
      .when(hasLineEvent(LineEventKind.Castling))(
        EvidenceSemanticAnchor.of(EvidenceSemanticAnchorKind.LineEvent, LineEventKind.Castling.toString)
      )
      .toList ++
      proofSignalConsequenceKinds.map(kind =>
        EvidenceSemanticAnchor.of(EvidenceSemanticAnchorKind.LineConsequence, kind.toString)
      )

  private def normalizeUci(raw: String): String =
    Option(raw).getOrElse("").trim.toLowerCase
  def materialOutcomeProfile: LineMaterialOutcomeProfile =
    val consequenceGainSignals =
      consequences.collect {
        case LineConsequence(LineConsequenceKind.MaterialGain, _, true, _) =>
          LineMaterialOutcomeSignal.MoverCapture
        case LineConsequence(LineConsequenceKind.MaterialGain, _, false, _) =>
          LineMaterialOutcomeSignal.UnrecoveredPawnGain
        case LineConsequence(LineConsequenceKind.RecoveryWindow, _, true, _) =>
          LineMaterialOutcomeSignal.RecoveryWindow
      }.toSet
    val consequenceLossSignals =
      consequences.collect {
        case LineConsequence(LineConsequenceKind.MaterialLoss, _, true, _) =>
          LineMaterialOutcomeSignal.OpponentCapture
        case LineConsequence(LineConsequenceKind.MaterialLoss, _, false, _) =>
          LineMaterialOutcomeSignal.UnrecoveredPawnLoss
      }.toSet
    val materialGainSignals =
      material
        .map(summary =>
          Set(
            Option.when(summary.nonPawnCapturesByMover.nonEmpty)(LineMaterialOutcomeSignal.MoverCapture),
            Option.when(summary.hasPromotionGainForMover)(LineMaterialOutcomeSignal.PromotionGain),
            Option.when(summary.hasUnrecoveredPawnGainForMover)(LineMaterialOutcomeSignal.UnrecoveredPawnGain),
            Option.when(summary.hasRecoveryWindow)(LineMaterialOutcomeSignal.RecoveryWindow)
          ).flatten
        )
        .getOrElse(Set.empty)
    val materialLossSignals =
      material
        .map(summary =>
          Set(
            Option.when(summary.nonPawnCapturesByOpponent.nonEmpty)(LineMaterialOutcomeSignal.OpponentCapture),
            Option.when(summary.hasPromotionLossForMover)(LineMaterialOutcomeSignal.PromotionLoss),
            Option.when(summary.hasUnrecoveredPawnLossForMover)(LineMaterialOutcomeSignal.UnrecoveredPawnLoss)
          ).flatten
        )
        .getOrElse(Set.empty)
    LineMaterialOutcomeProfile(
      gainSignals = consequenceGainSignals ++ materialGainSignals,
      lossSignals = consequenceLossSignals ++ materialLossSignals
    )
  def hasMaterialOutcomeSignals: Boolean =
    val profile = materialOutcomeProfile
    profile.gainSignals.nonEmpty || profile.lossSignals.nonEmpty

object LineFactEvidence:
  def fromRecords(records: List[EvidenceRecord]): List[LineFactEvidence] =
    records.collect { case EvidenceRecord(_, payload: LineFactEvidence, _) => payload }

  def materialOutcomeProfile(records: List[EvidenceRecord]): LineMaterialOutcomeProfile =
    fromRecords(records).map(_.materialOutcomeProfile).foldLeft(LineMaterialOutcomeProfile.empty)(_.merge(_))

  def maxMaterialNetCaptureCpForMover(records: List[EvidenceRecord]): Int =
    fromRecords(records).flatMap(_.materialNetCaptureCpForMover).maxOption.getOrElse(0)

  def maxMaterialGainCpForMover(records: List[EvidenceRecord]): Int =
    fromRecords(records).flatMap(_.materialMaxGainCpForMover).maxOption.getOrElse(0)

  def maxMaterialPromotionGainCpForMover(records: List[EvidenceRecord]): Int =
    fromRecords(records).flatMap(_.materialPromotionGainCpForMover).maxOption.getOrElse(0)

  def hasMaterialRecaptureChain(records: List[EvidenceRecord]): Boolean =
    fromRecords(records).exists(_.hasMaterialRecaptureChain)

  def hasMaterialRecoveryWindow(records: List[EvidenceRecord]): Boolean =
    fromRecords(records).exists(_.hasMaterialRecoveryWindow)

  def allHaveCompleteMaterialWindow(records: List[EvidenceRecord]): Boolean =
    fromRecords(records).forall(_.hasCompleteMaterialWindow)

  def apply(
      line: LineNodeRef,
      firstMove: Option[String],
      replyMove: Option[String],
      continuationMoves: List[String]
  ): LineFactEvidence =
    new LineFactEvidence(line, firstMove, replyMove, continuationMoves, None, None)()

  def apply(
      line: LineNodeRef,
      firstMove: Option[String],
      replyMove: Option[String],
      continuationMoves: List[String],
      forcedTheme: Option[ForcedLineThemeEvidence]
  ): LineFactEvidence =
    new LineFactEvidence(line, firstMove, replyMove, continuationMoves, forcedTheme, None)()

  def apply(
      line: LineNodeRef,
      firstMove: Option[String],
      replyMove: Option[String],
      continuationMoves: List[String],
      forcedTheme: Option[ForcedLineThemeEvidence],
      material: Option[LineMaterialSummary]
  ): LineFactEvidence =
    new LineFactEvidence(line, firstMove, replyMove, continuationMoves, forcedTheme, material)()

final case class EvalFactEvidence(
    line: LineNodeRef,
    whitePovEvalCp: Int,
    mate: Option[Int],
    depth: Int
) extends EvidencePayload:
  val layer: EvidenceLayer = EvidenceLayer.Eval

final case class MoveMotifEvidence(
    moveUci: String,
    motifs: List[Motif]
) extends EvidencePayload:
  val layer: EvidenceLayer = EvidenceLayer.MoveMotif

final case class MoveTransitionEvidence(
    moveUci: String,
    from: PositionNodeRef,
    to: PositionNodeRef
) extends EvidencePayload:
  val layer: EvidenceLayer = EvidenceLayer.MoveTransition

final case class RelationFactEvidence(
    kind: RelationFactKind,
    focusSquares: List[EvidenceSquare],
    targetSquare: Option[EvidenceSquare],
    lineMoves: List[String],
    participants: List[RelationParticipant]
) extends EvidencePayload:
  val layer: EvidenceLayer = EvidenceLayer.Relation

final case class StructuralDeltaEvidence(
    delta: StructuralDelta
) extends EvidencePayload:
  val layer: EvidenceLayer = EvidenceLayer.StructuralDelta

final case class PlanTransitionEvidence(
    transition: PlanSequenceSummary
) extends EvidencePayload:
  val layer: EvidenceLayer = EvidenceLayer.PlanTransition

final case class PlanPressureEvidence(
    scoring: PlanScoringResult,
    activePlans: ActivePlans
) extends EvidencePayload:
  val layer: EvidenceLayer = EvidenceLayer.PlanPressure

final case class CandidateComparisonEvidence(
    comparison: CandidateComparisonFact
) extends EvidencePayload:
  val layer: EvidenceLayer = EvidenceLayer.CandidateComparison

final case class CounterfactualFactEvidence(
    referenceLine: LineNodeRef,
    candidateLine: LineNodeRef,
    comparison: EvalComparison
) extends EvidencePayload:
  val layer: EvidenceLayer = EvidenceLayer.Counterfactual

final case class RelativeAssessmentEvidence(
    assessment: RelativeMoveAssessment
) extends EvidencePayload:
  val layer: EvidenceLayer = EvidenceLayer.RelativeAssessment

final case class RelativeCauseFactEvidence(
    cause: RelativeCauseFact
) extends EvidencePayload:
  val layer: EvidenceLayer = EvidenceLayer.RelativeCause

final case class MoveVerdictCertificationEvidence(
    certification: MoveVerdictCertification
) extends EvidencePayload:
  val layer: EvidenceLayer = EvidenceLayer.MoveVerdictCertification

final case class ChessIdeaEvidence(
    idea: ChessIdeaRef
) extends EvidencePayload:
  val layer: EvidenceLayer = EvidenceLayer.ChessIdea

final case class ClaimEvidence(
    claimId: String
) extends EvidencePayload:
  val layer: EvidenceLayer = EvidenceLayer.Claim

final case class EvidenceRecord(
    ref: EvidenceRef,
    payload: EvidencePayload,
    parents: List[EvidenceRef] = Nil
):
  def payloadLineRefs: List[LineNodeRef] =
    payload match
      case lineFact: LineFactEvidence =>
        List(lineFact.line)
      case EvalFactEvidence(payloadLine, _, _, _) =>
        List(payloadLine)
      case CandidateComparisonEvidence(fact) =>
        List(fact.referenceLine, fact.candidateLine)
      case CounterfactualFactEvidence(referenceLine, candidateLine, _) =>
        List(referenceLine, candidateLine)
      case RelativeAssessmentEvidence(assessment) =>
        List(assessment.reference.ref, assessment.candidate.ref)
      case RelativeCauseFactEvidence(cause) =>
        (cause.referenceLine :: cause.candidateLine :: cause.evidenceLines).distinct
      case MoveVerdictCertificationEvidence(certification) =>
        (
          certification.primaryComparison.referenceLine ::
            certification.primaryComparison.candidateLine ::
            certification.causes.flatMap(cause => cause.referenceLine :: cause.candidateLine :: cause.evidenceLines)
        ).distinct
      case _ =>
        Nil
  def referencesLine(line: LineNodeRef): Boolean =
    ref.line.contains(line) || payloadLineRefs.contains(line)
  def carriesLinePayload(line: LineNodeRef, layer: EvidenceLayer): Boolean =
    ref.layer == layer && ref.line.contains(line) && payloadLineRefs.contains(line)
  def hasConcreteLineSignal: Boolean =
    payload match
      case payload: LineFactEvidence =>
        payload.hasConcreteLineConsequence
      case EvalFactEvidence(_, _, mate, _) =>
        mate.nonEmpty
      case RelationFactEvidence(_, _, _, lineMoves, _) =>
        lineMoves.nonEmpty
      case _ =>
        false
  def hasRootCaptureEvent(rootMove: String): Boolean =
    payload match
      case payload: LineFactEvidence =>
        payload.hasRootCaptureEvent(rootMove)
      case _ =>
        false

object EvidenceRecord:
  def hasConcreteLineSignal(records: List[EvidenceRecord]): Boolean =
    records.exists(_.hasConcreteLineSignal)

  def rootCaptureRecords(records: List[EvidenceRecord], rootMove: String): List[EvidenceRecord] =
    records.filter(_.hasRootCaptureEvent(rootMove))

  def hasRootCaptureEvent(records: List[EvidenceRecord], rootMove: String): Boolean =
    rootCaptureRecords(records, rootMove).nonEmpty

final case class TypedEvidenceGraph(
    records: List[EvidenceRecord]
):
  lazy val byId: Map[String, EvidenceRecord] =
    records.map(record => record.ref.id -> record).toMap

  def refs(layer: EvidenceLayer): List[EvidenceRef] =
    records.collect { case record if record.payload.layer == layer => record.ref }

  def recordsFor(position: PositionNodeRef): List[EvidenceRecord] =
    records.filter(_.ref.position == position)

  def recordsFor(line: LineNodeRef): List[EvidenceRecord] =
    records.filter(_.ref.line.contains(line))

  def add(record: EvidenceRecord): TypedEvidenceGraph =
    copy(records = records.filterNot(_.ref.id == record.ref.id) :+ record)

object TypedEvidenceGraph:
  val empty: TypedEvidenceGraph = TypedEvidenceGraph(Nil)
