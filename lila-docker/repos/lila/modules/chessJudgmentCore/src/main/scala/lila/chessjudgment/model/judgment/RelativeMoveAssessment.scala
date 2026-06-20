package lila.chessjudgment.model.judgment

import chess.Color
import lila.chessjudgment.model.CollapseAnalysis

case class CandidateSetComparison(
    secondLine: Option[LineNodeRef],
    bestToSecondGapForMover: Option[Int],
    bestToSecondWinPercentGapForMover: Option[Double],
    candidateCount: Int,
    onlyMove: Boolean
)

enum CandidateComparisonKind:
  case PlayedVsBest
  case BestVsSecond
  case PlayedVsAlternative
  case ReferenceVsAlternative

case class EvalComparison(
    mover: Color,
    referenceLine: LineNodeRef,
    candidateLine: LineNodeRef,
    candidateDeltaForMover: Int,
    candidateWinPercentDeltaForMover: Double,
    cpLossForMover: Int,
    winPercentLossForMover: Double,
    verdict: MoveChoiceVerdict,
    candidateSet: Option[CandidateSetComparison] = None
)

case class CandidateComparisonFact(
    kind: CandidateComparisonKind,
    referenceLine: LineNodeRef,
    candidateLine: LineNodeRef,
    comparison: EvalComparison
)

enum RelativeCauseKind:
  case MissedTacticalResource
  case TacticalRefutationOfPlayed
  case CandidateTacticalLiability
  case WrongRecapturer
  case RecaptureRecoveryWindow
  case WrongMoveOrder
  case OnlyMoveNecessity
  case OnlyDefenseNecessity
  case TempoLoss
  case ConversionMiss
  case ConversionSecured
  case SacrificeCompensation
  case StructuralImprovement
  case TargetPressureGain
  case CenterControlGain
  case DevelopmentActivation
  case PieceActivityGain
  case CastlingRightsConcession
  case StrategicConcession
  case StrategicIdeaRefuted
  case MissedStrategicImprovement
  case PlanImprovement
  case PlanContradiction
  case DefensiveResource
  case DrawResource
  case KingForcing
  case MaterialSwing

case class RelativeCauseFact(
    kind: RelativeCauseKind,
    comparisonKind: CandidateComparisonKind,
    referenceLine: LineNodeRef,
    candidateLine: LineNodeRef,
    verdict: MoveChoiceVerdict,
    winPercentLossForMover: Double,
    candidateWinPercentDeltaForMover: Double,
    evidenceLines: List[LineNodeRef]
):
  def eventLine: LineNodeRef =
    if kind == RelativeCauseKind.CandidateTacticalLiability then candidateLine
    else
      comparisonKind match
        case CandidateComparisonKind.PlayedVsBest | CandidateComparisonKind.PlayedVsAlternative =>
          candidateLine
        case CandidateComparisonKind.BestVsSecond =>
          referenceLine
        case CandidateComparisonKind.ReferenceVsAlternative =>
          candidateLine

  def eventRootMove: String = eventLine.rootMove

case class MoveVerdictCertification(
    playedMove: String,
    verdict: MoveChoiceVerdict,
    primaryComparison: CandidateComparisonFact,
    causes: List[RelativeCauseFact]
)

case class RelativeMoveAssessment(
    played: MoveTransitionEdge,
    referenceTransition: Option[MoveTransitionEdge],
    reference: CandidateLineNode,
    candidate: CandidateLineNode,
    comparison: EvalComparison,
    collapse: Option[CollapseAnalysis],
    confidence: EvidenceConfidence,
    evidence: EvidenceRef,
    counterfactualEvidence: List[EvidenceRef],
    candidateComparisonEvidence: List[EvidenceRef] = Nil,
    relativeCauseEvidence: List[EvidenceRef] = Nil,
    verdictCertificationEvidence: Option[EvidenceRef] = None
)
