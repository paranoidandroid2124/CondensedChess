package lila.chessjudgment.model.judgment

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
) extends EvidencePayload:
  val layer: EvidenceLayer = EvidenceLayer.Board

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

final case class OpeningRouteFactEvidence(
    routeId: String,
    family: String,
    targetSquare: EvidenceSquare,
    pieceRole: EvidencePieceRole,
    path: List[EvidenceSquare],
    targetMode: String
) extends EvidencePayload:
  val layer: EvidenceLayer = EvidenceLayer.OpeningRoute

final case class ThreatPressureEvidence(
    threats: ThreatAnalysis
) extends EvidencePayload:
  val layer: EvidenceLayer = EvidenceLayer.ThreatPressure

final case class ForcedLineThemeEvidence(
    id: String,
    lineMoves: List[String]
)

final case class LineFactEvidence(
    line: LineNodeRef,
    firstMove: Option[String],
    replyMove: Option[String],
    continuationMoves: List[String],
    forcedTheme: Option[ForcedLineThemeEvidence] = None
) extends EvidencePayload:
  val layer: EvidenceLayer = EvidenceLayer.Line

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
