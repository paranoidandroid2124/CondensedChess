package lila.chessjudgment.model.judgment

import chess.Color
import lila.chessjudgment.analysis.position.PositionFeatures
import lila.chessjudgment.analysis.singlePosition.{ PawnPlayAnalysis, SinglePositionAssessment, ThreatAnalysis }
import lila.chessjudgment.analysis.structure.StructuralDelta
import lila.chessjudgment.model.{ ActivePlans, Fact, Motif, PlanScoringResult, PlanSequenceSummary }
import lila.chessjudgment.model.structure.{ PlanAlignment, StructureProfile }

final case class EvidenceSquare(key: String)
final case class EvidencePieceRole(name: String)

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
    facts: List[Fact],
    features: Option[PositionFeatures]
)(val anchors: List[BoardAnchor] = Nil
) extends EvidencePayload:
  val layer: EvidenceLayer = EvidenceLayer.Board

object BoardFactEvidence:
  def apply(facts: List[Fact], features: Option[PositionFeatures]): BoardFactEvidence =
    new BoardFactEvidence(facts, features)()

enum BoardAnchorKind:
  case CenterControl
  case Space
  case Development
  case FileControl
  case Activity
  case CounterplayRestraint
  case KingSafety
  case PawnStructure

enum BoardAnchorSignal:
  case CenterControlEdge
  case SpaceEdge
  case DevelopmentLead
  case SemiOpenFileAccess
  case RookOnSeventh
  case MobilityEdge
  case OpponentLowMobility
  case KingExposure
  case KingPressure
  case PawnStructureShape

final case class BoardAnchor(
    kind: BoardAnchorKind,
    side: Color,
    signal: BoardAnchorSignal,
    magnitude: Int,
    confidence: Double
)

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
) extends EvidencePayload:
  val layer: EvidenceLayer = EvidenceLayer.Strategic

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
)

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
    fenAfter: String
)

enum LineEventKind:
  case Capture
  case Recapture
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
  case MaterialGain
  case MaterialLoss
  case RecaptureSequence
  case RecoveryWindow
  case Sacrifice
  case PromotionRace

final case class LineConsequence(
    kind: LineConsequenceKind,
    lineMoves: List[String],
    claimGrade: Boolean,
    eventMove: Option[String] = None
)

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
    capturesByMover.filter(capture => claimGradeCapturedRole(capture.capturedRole))

  def nonPawnCapturesByOpponent: List[LineMaterialCapture] =
    capturesByOpponent.filter(capture => claimGradeCapturedRole(capture.capturedRole))

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

  def hasClaimGradeMaterialGain: Boolean =
    materialWindowComplete && (nonPawnCapturesByMover.nonEmpty || hasPromotionGainForMover || hasRecoveryWindow)

  def hasClaimGradeMaterialLoss: Boolean =
    materialWindowComplete && (nonPawnCapturesByOpponent.nonEmpty || hasPromotionLossForMover)

  def hasUnrecoveredPawnGainForMover: Boolean =
    materialWindowComplete && pawnCapturesByMover.nonEmpty && !hasRecoveryWindow

  def hasUnrecoveredPawnLossForMover: Boolean =
    materialWindowComplete && pawnCapturesByOpponent.nonEmpty && !hasRecoveryWindow

  def hasClaimGradeMaterialEvent: Boolean =
    hasClaimGradeMaterialGain ||
      hasClaimGradeMaterialLoss ||
      hasUnrecoveredPawnGainForMover ||
      hasUnrecoveredPawnLossForMover ||
      hasResolvedMaterialSequence

  def hasSacrificeMaterialEvent: Boolean =
    materialWindowComplete &&
      capturesByOpponent.exists(capture => !capture.recapture) &&
      !hasRecoveryWindow &&
      !hasClaimGradeMaterialGain

  private def claimGradeCapturedRole(role: EvidencePieceRole): Boolean =
    val normalized = role.name.trim.toLowerCase
    normalized.nonEmpty && normalized != "pawn" && normalized != "king"

  private def pawnCapturedRole(role: EvidencePieceRole): Boolean =
    role.name.trim.equalsIgnoreCase("pawn")

final case class LineFactEvidence(
    line: LineNodeRef,
    firstMove: Option[String],
    replyMove: Option[String],
    continuationMoves: List[String],
    forcedTheme: Option[ForcedLineThemeEvidence] = None,
    material: Option[LineMaterialSummary] = None
)(val replay: List[LineReplayStep] = Nil,
    val events: List[LineMoveEvent] = Nil,
    val consequences: List[LineConsequence] = Nil
) extends EvidencePayload:
  val layer: EvidenceLayer = EvidenceLayer.Line

object LineFactEvidence:
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
    evalCp: Int,
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
)

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
