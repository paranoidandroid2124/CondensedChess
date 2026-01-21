package lila.llm.model.authoring

import lila.llm.model.ConfidenceLevel

enum AuthorQuestionKind:
  case TensionDecision
  case PlanClash
  case TacticalTest
  case StructuralCommitment
  case ConversionPlan
  case DefensiveTask
  case LatentPlan

case class AuthorQuestion(
  id: String,
  kind: AuthorQuestionKind,
  priority: Int,
  question: String,
  why: Option[String] = None,
  anchors: List[String] = Nil,
  confidence: ConfidenceLevel = ConfidenceLevel.Heuristic,
  latentPlan: Option[LatentPlanInfo] = None
)

case class EvidenceBranch(
  keyMove: String, // e.g. "...exd5"
  line: String,    // e.g. "9... exd5 10. O-O ..."
  evalCp: Option[Int] = None,
  mate: Option[Int] = None,
  depth: Option[Int] = None,
  sourceId: Option[String] = None
)

case class QuestionEvidence(
  questionId: String,
  purpose: String,
  branches: List[EvidenceBranch]
)
