package lila.chessjudgment.analysis.assembly

import lila.chessjudgment.analysis.singlePosition.PawnPlayDriver
import lila.chessjudgment.model.structure.AlignmentBand
import lila.chessjudgment.model.structure.StructureId
import lila.chessjudgment.model.judgment.*

final case class ChessIdeaAssembly(
    input: NormalizedMoveReviewInput,
    context: JudgmentAssemblyContext
)

object ChessIdeaAssembler:

  def assemble(raw: RawMoveReviewInput): Option[ChessIdeaAssembly] =
    RelativeAssessmentAssembler.assemble(raw).map(enrich)

  def enrich(assembly: RelativeAssessmentAssembly): ChessIdeaAssembly =
    val allocator = JudgmentProvenanceAllocator.forInput(assembly.input)
    val context = assembly.context
    val ideas =
      List
        .concat(
          tacticalIdeas(context, allocator),
          pawnStructureIdeas(context, allocator),
          openingIdeas(context, allocator),
          defensiveIdeas(context, allocator),
          evaluationIdeas(context, allocator),
          conversionIdeas(context, allocator),
          strategicIdeas(context, allocator)
        )
        .distinctBy(_.ref.id)
    val ideaRecords = ideas.map(idea => ChessIdeaBuilder.evidenceRecord(s"${idea.ref.id}:evidence", idea))
    val withEvidence = context.withEvidence(ideaRecords)
    val withIdeas = ideas.foldLeft(withEvidence)((ctx, idea) => ctx.withIdea(idea))
    ChessIdeaAssembly(assembly.input, withIdeas)

  private def tacticalIdeas(
      context: JudgmentAssemblyContext,
      allocator: JudgmentProvenanceAllocator
  ): List[ChessIdea] =
    context.evidenceGraph.records.collect {
      case EvidenceRecord(ref, payload: RelationFactEvidence, _) =>
        val lineRef = ref.line
        val lineSupport = lineRef.toList.flatMap(line => lineLayerRefs(context, line))
        val evidence = (ref :: lineSupport).distinctBy(_.id)
        ChessIdeaBuilder.fromEvidence(
          id = allocator.evidenceId(s"idea:tactical:${allocator.key(payload.kind)}:${allocator.key(ref.id)}"),
          family = ChessIdeaFamily.Tactical,
          subject = subjectForLine(lineRef),
          primaryPosition = ref.position,
          primaryLine = lineRef,
          moveUci = lineRef.map(_.rootMove),
          evidence = evidence,
          scope = ref.scope,
          confidence = ref.confidence
        )
    }

  private def pawnStructureIdeas(
      context: JudgmentAssemblyContext,
      allocator: JudgmentProvenanceAllocator
  ): List[ChessIdea] =
    val structurePositionIdeas =
      context.evidenceGraph.records.collect {
        case EvidenceRecord(ref, payload: PawnStructureFactEvidence, _)
            if pawnStructureCarriesTheme(payload) =>
          ChessIdeaBuilder.fromEvidence(
            id = allocator.evidenceId(s"idea:pawn-structure:${allocator.key(ref.id)}"),
            family = ChessIdeaFamily.PawnStructure,
            subject = IdeaSubject.Position,
            primaryPosition = ref.position,
            primaryLine = None,
            moveUci = None,
            evidence = (ref :: recordsForPosition(context, EvidenceLayer.Board, ref.position)).distinctBy(_.id),
            scope = ref.scope,
            confidence = ref.confidence
          )
      }
    val structureMoveIdeas =
      context.evidenceGraph.records.flatMap {
        case EvidenceRecord(ref, _: StructuralDeltaEvidence, _) =>
          transitionForScope(context, ref.scope).map { transition =>
            val evidence = (ref :: transition.evidence :: recordsForPosition(context, EvidenceLayer.PawnStructure, transition.to)).distinctBy(_.id)
            ChessIdeaBuilder.fromEvidence(
              id = allocator.evidenceId(s"idea:pawn-structure-delta:${allocator.key(ref.id)}"),
              family = ChessIdeaFamily.PawnStructure,
              subject = if transition.role == TransitionEdgeRole.Played then IdeaSubject.PlayedMove else IdeaSubject.ReferenceMove,
              primaryPosition = ref.position,
              primaryLine = ref.line,
              moveUci = Some(transition.moveUci),
              evidence = evidence,
              scope = ref.scope,
              confidence = ref.confidence
            )
          }
        case _ => None
      }
    structurePositionIdeas ++ structureMoveIdeas

  private def defensiveIdeas(
      context: JudgmentAssemblyContext,
      allocator: JudgmentProvenanceAllocator
  ): List[ChessIdea] =
    context.evidenceGraph.records.collect {
      case EvidenceRecord(ref, payload: ThreatPressureEvidence, parents)
          if ref.position.sideToMove.forall(_ == payload.sideUnderPressure) &&
            (payload.threats.hasThreat || payload.threats.defenseRequired || payload.threats.prophylaxisNeeded) =>
        val evidence =
          (ref :: parents ++
            ref.line.toList.flatMap(lineLayerRefs(context, _)) ++
            relationRefs(context, ref.position, ref.line)).distinctBy(_.id)
        ChessIdeaBuilder.fromEvidence(
          id = allocator.evidenceId(s"idea:defensive:${allocator.key(ref.id)}"),
          family = ChessIdeaFamily.Defensive,
          subject = IdeaSubject.Threat,
          primaryPosition = ref.position,
          primaryLine = ref.line,
          moveUci = payload.threats.defense.onlyDefense.orElse(payload.threats.defense.alternatives.headOption),
          evidence = evidence,
          scope = ref.scope,
          confidence = ref.confidence
        )
    }

  private def evaluationIdeas(
      context: JudgmentAssemblyContext,
      allocator: JudgmentProvenanceAllocator
  ): List[ChessIdea] =
    context.relativeAssessments.map { assessment =>
      val evidence =
        (assessment.evidence :: assessment.counterfactualEvidence ++
          lineLayerRefs(context, assessment.reference.ref) ++
          lineLayerRefs(context, assessment.candidate.ref)).distinctBy(_.id)
      ChessIdeaBuilder.fromEvidence(
        id = allocator.evidenceId(s"idea:evaluation:${allocator.key(assessment.evidence.id)}"),
        family = ChessIdeaFamily.Evaluation,
        subject = IdeaSubject.PlayedMove,
        primaryPosition = assessment.played.from,
        primaryLine = Some(assessment.candidate.ref),
        moveUci = Some(assessment.played.moveUci),
        evidence = evidence,
        scope = assessment.evidence.scope,
        confidence = assessment.confidence
      )
    }

  private def conversionIdeas(
      context: JudgmentAssemblyContext,
      allocator: JudgmentProvenanceAllocator
  ): List[ChessIdea] =
    context.evidenceGraph.records.collect {
      case EvidenceRecord(ref, SinglePositionEvidence(assessment), _)
          if assessment.simplifyBias.shouldSimplify =>
        ChessIdeaBuilder.fromEvidence(
          id = allocator.evidenceId(s"idea:conversion:${allocator.key(ref.id)}"),
          family = ChessIdeaFamily.Conversion,
          subject = IdeaSubject.Position,
          primaryPosition = ref.position,
          primaryLine = None,
          moveUci = None,
          evidence = List(ref),
          scope = ref.scope,
          confidence = ref.confidence
        )
    }

  private def strategicIdeas(
      context: JudgmentAssemblyContext,
      allocator: JudgmentProvenanceAllocator
  ): List[ChessIdea] =
    val strategicFactIdeas =
      context.evidenceGraph.records.collect {
        case EvidenceRecord(ref, _: StrategicFactEvidence, _) =>
          val evidence = (ref :: recordsForPosition(context, EvidenceLayer.Board, ref.position)).distinctBy(_.id)
          ChessIdeaBuilder.fromEvidence(
            id = allocator.evidenceId(s"idea:strategic-fact:${allocator.key(ref.id)}"),
            family = ChessIdeaFamily.Strategic,
            subject = IdeaSubject.Position,
            primaryPosition = ref.position,
            primaryLine = ref.line,
            moveUci = ref.line.map(_.rootMove),
            evidence = evidence,
            scope = ref.scope,
            confidence = ref.confidence
          )
      }
    val planPressureIdeas =
      context.evidenceGraph.records.collect {
        case EvidenceRecord(ref, _: PlanPressureEvidence, parents) =>
          val evidence =
            (ref :: parents ++
              ref.line.toList.flatMap(lineLayerRefs(context, _)) ++
              recordsForPosition(context, EvidenceLayer.Strategic, ref.position) ++
              recordsForPosition(context, EvidenceLayer.PawnStructure, ref.position) ++
              recordsForPosition(context, EvidenceLayer.ThreatPressure, ref.position) ++
              recordsForPosition(context, EvidenceLayer.PlanTransition, ref.position) ++
              relationRefs(context, ref.position, ref.line) ++
              recordsForPosition(context, EvidenceLayer.SinglePosition, ref.position)).distinctBy(_.id)
          ChessIdeaBuilder.fromEvidence(
            id = allocator.evidenceId(s"idea:plan-pressure:${allocator.key(ref.id)}"),
            family = ChessIdeaFamily.Strategic,
            subject = IdeaSubject.Plan,
            primaryPosition = ref.position,
            primaryLine = ref.line,
            moveUci = ref.line.map(_.rootMove),
            evidence = evidence,
            scope = ref.scope,
            confidence = ref.confidence
          )
      }
    strategicFactIdeas ++ planPressureIdeas

  private def openingIdeas(
      context: JudgmentAssemblyContext,
      allocator: JudgmentProvenanceAllocator
  ): List[ChessIdea] =
    context.evidenceGraph.records.collect {
      case EvidenceRecord(ref, ApplicabilityAssessmentEvidence(assessment), parents)
          if assessment.applicability == FeatureApplicability.OpeningRelevant &&
            assessment.status != ApplicabilityStatus.Contradicted &&
            assessment.observedThemes.nonEmpty =>
        val primaryLine = context.line(LineNodeRole.BestReference).map(_.ref)
        val evidence =
          (ref :: parents ++
            primaryLine.toList.flatMap(lineLayerRefs(context, _))).distinctBy(_.id)
        ChessIdeaBuilder.fromEvidence(
          id = allocator.evidenceId(s"idea:opening:${allocator.key(ref.id)}"),
          family = ChessIdeaFamily.Opening,
          subject = IdeaSubject.Position,
          primaryPosition = ref.position,
          primaryLine = primaryLine,
          moveUci = None,
          evidence = evidence,
          scope = ref.scope,
          confidence = ref.confidence
        )
    }

  private def lineLayerRefs(
      context: JudgmentAssemblyContext,
      line: LineNodeRef
  ): List[EvidenceRef] =
    context.evidenceGraph.recordsFor(line).collect {
      case record if record.ref.layer == EvidenceLayer.Line || record.ref.layer == EvidenceLayer.Eval => record.ref
    }

  private def recordsForPosition(
      context: JudgmentAssemblyContext,
      layer: EvidenceLayer,
      position: PositionNodeRef
  ): List[EvidenceRef] =
    context.evidenceGraph.recordsFor(position).collect {
      case record if record.ref.layer == layer => record.ref
    }

  private def relationRefs(
      context: JudgmentAssemblyContext,
      position: PositionNodeRef,
      line: Option[LineNodeRef]
  ): List[EvidenceRef] =
    context.evidenceGraph.records.collect {
      case record
          if record.ref.layer == EvidenceLayer.Relation &&
            record.ref.position == position &&
            line.forall(record.ref.line.contains) =>
        record.ref
    }

  private def pawnStructureCarriesTheme(payload: PawnStructureFactEvidence): Boolean =
    payload.profile.primary != StructureId.Unknown && payload.profile.confidence >= 0.65 ||
      payload.pawnPlay.exists(_.primaryDriver != PawnPlayDriver.Quiet) ||
      payload.alignment.exists(alignment =>
        alignment.band == AlignmentBand.OnBook ||
          alignment.band == AlignmentBand.Playable ||
          alignment.band == AlignmentBand.OffPlan
      )

  private def subjectForLine(line: Option[LineNodeRef]): IdeaSubject =
    line.map(_.role) match
      case Some(LineNodeRole.Played)        => IdeaSubject.PlayedMove
      case Some(LineNodeRole.BestReference) => IdeaSubject.ReferenceMove
      case Some(LineNodeRole.Alternative) | Some(LineNodeRole.Threat) =>
        IdeaSubject.CandidateLine
      case None => IdeaSubject.Position

  private def transitionForScope(context: JudgmentAssemblyContext, scope: EvidenceScope): Option[MoveTransitionEdge] =
    transitionRoleFor(scope).flatMap(context.transition)

  private def transitionRoleFor(scope: EvidenceScope): Option[TransitionEdgeRole] =
    scope match
      case EvidenceScope.PlayedTransition    => Some(TransitionEdgeRole.Played)
      case EvidenceScope.ReferenceTransition => Some(TransitionEdgeRole.Reference)
      case EvidenceScope.AlternativeTransition => Some(TransitionEdgeRole.Alternative)
      case EvidenceScope.ThreatLine          => Some(TransitionEdgeRole.Threat)
      case _                                 => None
