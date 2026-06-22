package lila.chessjudgment.model.judgment

case class EvidenceRef(
    id: String,
    producer: EvidenceProducer,
    layer: EvidenceLayer,
    position: PositionNodeRef,
    line: Option[LineNodeRef],
    scope: EvidenceScope,
    confidence: EvidenceConfidence
):
  def lineRootMoveMatches(moveUci: String): Boolean =
    line.exists(lineRef => EvidenceRef.sameMove(lineRef.rootMove, moveUci))

object EvidenceRef:
  def sameMove(left: String, right: String): Boolean =
    normalizeMove(left) == normalizeMove(right)

  def normalizeMove(raw: String): String =
    Option(raw).getOrElse("").trim.toLowerCase

enum EvidenceProducer:
  case BoardFactProducer
  case SinglePositionProducer
  case LegalLineProducer
  case EngineEvalProducer
  case TacticalRelationProducer
  case PawnStructureProducer
  case StrategicFeatureProducer
  case OpeningContextProducer
  case FeatureAnchorProducer
  case ApplicabilityAssessmentProducer
  case ThreatPressureProducer
  case MoveMotifProducer
  case TacticalMechanismProducer
  case MoveTransitionProducer
  case StructuralDeltaProducer
  case PlanPressureProducer
  case PlanTransitionProducer
  case RelativeMoveProducer
  case ChessIdeaProducer
  case ClaimComposer

enum EvidenceScope:
  case BeforePosition
  case AfterPlayedPosition
  case AfterReferencePosition
  case CurrentPosition
  case PlayedTransition
  case ReferenceTransition
  case AlternativeTransition
  case BestLine
  case PlayedLine
  case CandidateLine
  case ThreatLine
  case Counterfactual

enum EvidenceConfidence:
  case LegalReplayVerified
  case EngineBacked
  case BoardDerived
  case Heuristic
  case Mixed
