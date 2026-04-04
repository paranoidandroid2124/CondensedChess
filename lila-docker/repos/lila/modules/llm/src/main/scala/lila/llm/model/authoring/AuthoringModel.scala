package lila.llm.model.authoring

import lila.llm.model.ConfidenceLevel

enum AuthorQuestionKind:
  case WhyThis
  case WhyNow
  case WhatChanged
  case WhatMattersHere
  case WhatMustBeStopped
  case WhosePlanIsFaster

case class AuthorQuestion(
  id: String,
  kind: AuthorQuestionKind,
  priority: Int,
  question: String,
  why: Option[String] = None,
  anchors: List[String] = Nil,
  confidence: ConfidenceLevel = ConfidenceLevel.Heuristic,
  evidencePurposes: List[String] = Nil,
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
