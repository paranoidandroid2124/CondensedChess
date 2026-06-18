package lila.chessjudgment.model.judgment

enum PositionNodeRole:
  case Before
  case AfterPlayed
  case AfterReference
  case AfterAlternative
  case AfterThreat

enum LineNodeRole:
  case Played
  case BestReference
  case Alternative
  case Threat

enum TransitionEdgeRole:
  case Played
  case Reference
  case Alternative
  case Threat

enum EvidenceLayer:
  case Board
  case SinglePosition
  case PawnStructure
  case Strategic
  case OpeningRoute
  case ThreatPressure
  case Line
  case Eval
  case MoveMotif
  case MoveTransition
  case Relation
  case StructuralDelta
  case PlanPressure
  case PlanTransition
  case Counterfactual
  case RelativeAssessment
  case ChessIdea
  case Claim

enum ChessIdeaFamily:
  case Tactical
  case Strategic
  case PawnStructure
  case Opening
  case Defensive
  case Conversion
  case Evaluation

enum IdeaSubject:
  case Position
  case PlayedMove
  case ReferenceMove
  case CandidateLine
  case Threat
  case Plan
