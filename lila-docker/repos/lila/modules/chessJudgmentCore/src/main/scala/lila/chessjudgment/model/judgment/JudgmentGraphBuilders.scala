package lila.chessjudgment.model.judgment

import lila.chessjudgment.analysis.position.PositionFeatures
import lila.chessjudgment.analysis.singlePosition.SinglePositionAssessment
import lila.chessjudgment.model.{ CollapseAnalysis, Fact, PlanSequenceSummary }
import lila.chessjudgment.model.strategic.VariationLine

object PositionNodeBuilder:
  def fromAnalysis(
      role: PositionNodeRole,
      ref: PositionNodeRef,
      facts: List[Fact],
      features: Option[PositionFeatures],
      assessment: Option[SinglePositionAssessment],
      evidence: List[EvidenceRef]
  ): PositionNode =
    PositionNode(
      role = role,
      ref = ref,
      facts = facts,
      features = features,
      assessment = assessment,
      evidence = evidence
    )

object CandidateLineNodeBuilder:
  def fromEngineLine(
      role: LineNodeRole,
      ref: LineNodeRef,
      line: VariationLine,
      evalCp: Int,
      mate: Option[Int],
      depth: Int,
      evidence: EvidenceRef
  ): CandidateLineNode =
    CandidateLineNode(
      role = role,
      ref = ref,
      line = line,
      evalCp = evalCp,
      mate = mate,
      depth = depth,
      evidence = evidence
    )

object MoveTransitionEdgeBuilder:
  def fromMove(
      role: TransitionEdgeRole,
      id: String,
      from: PositionNodeRef,
      moveUci: String,
      to: PositionNodeRef,
      changedFacts: List[Fact],
      planTransition: Option[PlanSequenceSummary],
      evidence: EvidenceRef
  ): MoveTransitionEdge =
    MoveTransitionEdge(
      role = role,
      id = id,
      from = from,
      moveUci = moveUci,
      to = to,
      changedFacts = changedFacts,
      planTransition = planTransition,
      evidence = evidence
    )

object RelativeMoveAssessmentBuilder:
  def fromComparison(
      played: MoveTransitionEdge,
      referenceTransition: Option[MoveTransitionEdge],
      reference: CandidateLineNode,
      candidate: CandidateLineNode,
      comparison: EvalComparison,
      collapse: Option[CollapseAnalysis],
      confidence: EvidenceConfidence,
      evidence: EvidenceRef,
      counterfactualEvidence: List[EvidenceRef]
  ): RelativeMoveAssessment =
    RelativeMoveAssessment(
      played = played,
      referenceTransition = referenceTransition,
      reference = reference,
      candidate = candidate,
      comparison = comparison,
      collapse = collapse,
      confidence = confidence,
      evidence = evidence,
      counterfactualEvidence = counterfactualEvidence
    )

object ChessIdeaBuilder:
  def fromEvidence(
      id: String,
      family: ChessIdeaFamily,
      subject: IdeaSubject,
      primaryPosition: PositionNodeRef,
      primaryLine: Option[LineNodeRef],
      moveUci: Option[String],
      evidence: List[EvidenceRef],
      scope: EvidenceScope,
      confidence: EvidenceConfidence
  ): ChessIdea =
    ChessIdea(
      ref = ChessIdeaRef(id, family),
      subject = subject,
      primaryPosition = primaryPosition,
      primaryLine = primaryLine,
      moveUci = moveUci,
      evidence = evidence,
      requiredLayers = evidence.map(_.layer).distinct,
      scope = scope,
      confidence = confidence
    )

  def evidenceRecord(id: String, idea: ChessIdea): EvidenceRecord =
    val ref =
      EvidenceRef(
        id = id,
        producer = EvidenceProducer.ChessIdeaProducer,
        layer = EvidenceLayer.ChessIdea,
        position = idea.primaryPosition,
        line = idea.primaryLine,
        scope = idea.scope,
        confidence = idea.confidence
      )
    EvidenceRecord(
      ref = ref,
      payload = ChessIdeaEvidence(idea.ref),
      parents = idea.evidence
    )

object ClaimComposer:
  def fromIdea(
      id: String,
      family: ClaimFamily,
      idea: ChessIdea,
      supportingFacts: List[Fact],
      engineComparison: Option[EvalComparison],
      confidence: EvidenceConfidence
  ): ClaimSeed =
    ClaimSeed(
      id = id,
      family = family,
      idea = Some(idea.ref),
      subject = idea.subject,
      primaryPosition = idea.primaryPosition,
      primaryLine = idea.primaryLine,
      subjectMove = idea.moveUci,
      evidence = idea.evidence,
      supportingFacts = supportingFacts,
      engineComparison = engineComparison,
      scope = idea.scope,
      confidence = confidence
    )

  def fromEvidence(
      id: String,
      family: ClaimFamily,
      subject: IdeaSubject,
      primaryPosition: PositionNodeRef,
      primaryLine: Option[LineNodeRef],
      subjectMove: Option[String],
      evidence: List[EvidenceRef],
      supportingFacts: List[Fact],
      engineComparison: Option[EvalComparison],
      scope: EvidenceScope,
      confidence: EvidenceConfidence
  ): ClaimSeed =
    ClaimSeed(
      id = id,
      family = family,
      idea = None,
      subject = subject,
      primaryPosition = primaryPosition,
      primaryLine = primaryLine,
      subjectMove = subjectMove,
      evidence = evidence,
      supportingFacts = supportingFacts,
      engineComparison = engineComparison,
      scope = scope,
      confidence = confidence
    )

  def evidenceRecord(id: String, claim: ClaimSeed): EvidenceRecord =
    val ref =
      EvidenceRef(
        id = id,
        producer = EvidenceProducer.ClaimComposer,
        layer = EvidenceLayer.Claim,
        position = claim.primaryPosition,
        line = claim.primaryLine,
        scope = claim.scope,
        confidence = claim.confidence
      )
    EvidenceRecord(
      ref = ref,
      payload = ClaimEvidence(claim.id),
      parents = claim.evidence
    )

object JudgmentPacketBuilder:
  def fromAssembly(ctx: JudgmentAssemblyContext): Option[EvidenceBackedJudgmentPacket] =
    ctx.root.map { rootRef =>
      val diagnostics = EvidenceLossDiagnostics.fromAssembly(ctx)
      EvidenceBackedJudgmentPacket(
        root = rootRef,
        positions = ctx.positions,
        candidateLines = ctx.lines,
        transitions = ctx.transitions,
        relativeAssessments = ctx.relativeAssessments,
        evidenceGraph = ctx.evidenceGraph,
        ideas = ctx.ideas,
        claims = ctx.claims,
        ideaVerdict = IdeaVerdictSplit.from(ctx.ideas, ctx.claims, ctx.relativeAssessments),
        diagnostics = diagnostics
      )
    }
