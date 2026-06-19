package lila.chessjudgment.model.judgment

import lila.chessjudgment.model.Fact

enum ClaimFamily:
  case Tactical
  case Strategic
  case PawnStructure
  case Opening
  case Plan
  case Defensive
  case Conversion
  case Evaluation

enum ClaimSalienceDriver:
  case TacticalRelation
  case ForcingLine
  case DefensiveUrgency
  case PlanPressure
  case PawnStructureAlignment
  case StructuralChange
  case StrategicFeature
  case OpeningContext
  case EndgamePattern
  case EngineSwing
  case CandidateConstraint
  case BoardAnchor

case class ClaimSalience(
    score: Int,
    drivers: List[ClaimSalienceDriver]
)

enum ClaimSupportStatus:
  case Certified
  case Deferred

case class ClaimSupportCheck(
    status: ClaimSupportStatus,
    presentLayers: Set[EvidenceLayer],
    missingLayerGroups: List[Set[EvidenceLayer]],
    missingEvidence: List[EvidenceRef]
)

case class ClaimSeed(
    id: String,
    family: ClaimFamily,
    idea: Option[ChessIdeaRef],
    subject: IdeaSubject,
    primaryPosition: PositionNodeRef,
    primaryLine: Option[LineNodeRef],
    subjectMove: Option[String],
    evidence: List[EvidenceRef],
    supportingFacts: List[Fact],
    engineComparison: Option[EvalComparison],
    scope: EvidenceScope,
    confidence: EvidenceConfidence,
    supportStatus: Option[ClaimSupportCheck] = None,
    salience: Option[ClaimSalience] = None
)

case class IdeaVerdictSplit(
    ideas: List[ChessIdeaRef],
    ideaClaims: List[ClaimSeed],
    verdict: Option[EvalComparison],
    bindings: List[IdeaVerdictBinding]
)

object IdeaVerdictSplit:
  def from(
      ideas: List[ChessIdea],
      claims: List[ClaimSeed],
      assessments: List[RelativeMoveAssessment]
  ): Option[IdeaVerdictSplit] =
    val ideaRefs = ideas.map(_.ref)
    val ideaClaims = claims.filter(_.idea.exists(ideaRefs.contains))
    val assessment = assessments.headOption
    val verdict = assessment.map(_.comparison)
    val bindings =
      assessment.toList.flatMap { relative =>
        ideas.filter(ideaBindsToRelative(_, relative)).map { idea =>
          IdeaVerdictBinding(
            idea = idea.ref,
            verdict = relative.comparison,
            relation = relationFor(idea, relative.comparison),
            evidence = (idea.evidence ++ (relative.evidence :: relative.counterfactualEvidence)).distinctBy(_.id)
          )
        }
      }
    Option.when(ideaRefs.nonEmpty || ideaClaims.nonEmpty || verdict.nonEmpty)(
      IdeaVerdictSplit(
        ideas = ideaRefs,
        ideaClaims = ideaClaims,
        verdict = verdict,
        bindings = bindings
      )
    )

  private def relationFor(idea: ChessIdea, comparison: EvalComparison): IdeaVerdictRelation =
    comparison.verdict match
      case MoveChoiceVerdict.ImprovesOnReference | MoveChoiceVerdict.MatchesReference | MoveChoiceVerdict.PlayableLoss =>
        if idea.ref.family == ChessIdeaFamily.Defensive then IdeaVerdictRelation.DefensiveNecessity
        else IdeaVerdictRelation.SupportsVerdict
      case MoveChoiceVerdict.Blunder
          if idea.subject == IdeaSubject.PlayedMove || idea.moveUci.nonEmpty =>
        IdeaVerdictRelation.RefutesIdea
      case MoveChoiceVerdict.Mistake
          if idea.ref.family == ChessIdeaFamily.Tactical && (idea.subject == IdeaSubject.PlayedMove || idea.moveUci.nonEmpty) =>
        IdeaVerdictRelation.RefutesIdea
      case MoveChoiceVerdict.Inaccuracy | MoveChoiceVerdict.Mistake | MoveChoiceVerdict.Blunder =>
        IdeaVerdictRelation.ExplainsIdeaDespiteBadVerdict

  private def ideaBindsToRelative(idea: ChessIdea, relative: RelativeMoveAssessment): Boolean =
    val relativeLines = Set(relative.reference.ref, relative.candidate.ref)
    val relativeEvidence = (relative.evidence :: relative.counterfactualEvidence).map(_.id).toSet
    idea.primaryLine.exists(relativeLines.contains) ||
      idea.moveUci.contains(relative.played.moveUci) ||
      idea.evidence.exists(ref =>
        relativeEvidence.contains(ref.id) ||
          ref.line.exists(relativeLines.contains)
      )

case class EvidenceBackedJudgmentPacket(
    root: PositionNodeRef,
    positions: List[PositionNode],
    candidateLines: List[CandidateLineNode],
    transitions: List[MoveTransitionEdge],
    relativeAssessments: List[RelativeMoveAssessment],
    evidenceGraph: TypedEvidenceGraph,
    ideas: List[ChessIdea],
    claims: List[ClaimSeed],
    ideaVerdict: Option[IdeaVerdictSplit],
    diagnostics: EvidenceLossReport = EvidenceLossReport.empty
):
  def playedTransition: Option[MoveTransitionEdge] =
    transitions.find(_.role == TransitionEdgeRole.Played)

  def referenceTransition: Option[MoveTransitionEdge] =
    transitions.find(_.role == TransitionEdgeRole.Reference)

type LlmJudgmentPacket = EvidenceBackedJudgmentPacket
