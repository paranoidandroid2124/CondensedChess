package lila.chessjudgment.analysis.assembly

import lila.chessjudgment.analysis.evaluation.JudgmentThresholds
import lila.chessjudgment.analysis.policy.ClaimTruthPolicy
import lila.chessjudgment.analysis.singlePosition.PawnPlayDriver
import lila.chessjudgment.analysis.structure.StructuralDelta
import lila.chessjudgment.analysis.tactical.TacticalMotifClassifier
import lila.chessjudgment.model.Motif
import lila.chessjudgment.model.structure.AlignmentBand
import lila.chessjudgment.model.structure.StructureId
import lila.chessjudgment.model.judgment.*

final case class ChessIdeaAssembly(
    input: NormalizedMoveReviewInput,
    context: JudgmentAssemblyContext
)

object ChessIdeaAssembler:

  private enum TacticalIdeaDriver(val id: String):
    case KingForcing extends TacticalIdeaDriver("king-forcing")
    case MaterialGain extends TacticalIdeaDriver("material-gain")
    case RecaptureChoice extends TacticalIdeaDriver("recapture-choice")
    case Tempo extends TacticalIdeaDriver("tempo")
    case RelationMechanism extends TacticalIdeaDriver("relation-mechanism")
    case Conversion extends TacticalIdeaDriver("conversion")
    case Refutation extends TacticalIdeaDriver("refutation")
    case DrawResource extends TacticalIdeaDriver("draw-resource")
    case PawnPromotion extends TacticalIdeaDriver("pawn-promotion")

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
          relativeCauseIdeas(context, allocator),
          evaluationIdeas(context, allocator),
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
    (
      context.lines.flatMap(line => compositeTacticalIdeas(context, allocator, line)) ++
        playedTransitionTacticalIdeas(context, allocator)
    ).distinctBy(_.ref.id)

  private def playedTransitionTacticalIdeas(
      context: JudgmentAssemblyContext,
      allocator: JudgmentProvenanceAllocator
  ): List[ChessIdea] =
    context.playedTransition.toList
      .filterNot(edge => context.line(LineNodeRole.Played).exists(_.ref.rootMove == edge.moveUci))
      .flatMap { edge =>
        val transitionRecords =
          context.evidenceGraph.records.filter(record =>
            record.ref.scope == EvidenceScope.PlayedTransition &&
              record.ref.position == edge.from &&
              transitionRecordMentionsMove(record, edge.moveUci)
          )
        val motifRecords = transitionRecords.collect { case record @ EvidenceRecord(_, _: MoveMotifEvidence, _) => record }
        val relationRecords = transitionRecords.collect { case record @ EvidenceRecord(_, _: RelationFactEvidence, _) => record }
        val motifs =
          motifRecords.collect { case EvidenceRecord(_, MoveMotifEvidence(moveUci, motifs), _) =>
            motifs.filter(TacticalMotifClassifier.isRootMoveMotif(moveUci, _))
          }.flatten
        val drivers =
          (motifs.flatMap(tacticalDriverForMotif) ++
            relationRecords.collect { case EvidenceRecord(_, RelationFactEvidence(kind, _, _, _, _), _) =>
              tacticalDriverForRelation(kind)
            }).distinct
        drivers.flatMap { driver =>
          val evidence =
            transitionTacticalEvidence(
              context = context,
              edge = edge,
              driver = driver,
              motifRecords = motifRecords,
              relationRecords = relationRecords
            )
          Option.when(evidence.nonEmpty) {
            ChessIdeaBuilder.fromEvidence(
              id = allocator.evidenceId(s"idea:tactical:${driver.id}:played-transition:${edge.moveUci}"),
              family = ChessIdeaFamily.Tactical,
              subject = IdeaSubject.PlayedMove,
              primaryPosition = edge.from,
              primaryLine = None,
              moveUci = Some(edge.moveUci),
              evidence = evidence.distinctBy(_.id),
              scope = EvidenceScope.PlayedTransition,
              confidence =
                if relationRecords.nonEmpty then EvidenceConfidence.LegalReplayVerified
                else EvidenceConfidence.Mixed
            )
          }
        }
      }

  private def compositeTacticalIdeas(
      context: JudgmentAssemblyContext,
      allocator: JudgmentProvenanceAllocator,
      line: CandidateLineNode
  ): List[ChessIdea] =
    val lineRecords = context.evidenceGraph.recordsFor(line.ref)
    val lineFactRecords = lineRecords.collect { case record @ EvidenceRecord(_, _: LineFactEvidence, _) => record }
    val evalRecords = lineRecords.collect { case record @ EvidenceRecord(_, _: EvalFactEvidence, _) => record }
    val motifRecords = lineRecords.collect { case record @ EvidenceRecord(_, _: MoveMotifEvidence, _) => record }
    val relationRecords = lineRecords.collect { case record @ EvidenceRecord(_, _: RelationFactEvidence, _) => record }
    val relative = relativeAssessmentsForLine(context, line.ref)
    val motifs =
      motifRecords.collect { case EvidenceRecord(_, MoveMotifEvidence(moveUci, motifs), _) =>
        motifs.filter(TacticalMotifClassifier.isRootMoveMotif(moveUci, _))
      }.flatten
    val primaryPosition =
      (lineFactRecords ++ evalRecords ++ motifRecords ++ relationRecords).headOption
        .map(_.ref.position)
        .orElse(context.root)
    val drivers = tacticalDrivers(
      motifs = motifs,
      lineFacts = lineFactRecords.collect { case EvidenceRecord(_, payload: LineFactEvidence, _) => payload },
      evalFacts = evalRecords.collect { case EvidenceRecord(_, payload: EvalFactEvidence, _) => payload },
      relationRecords = relationRecords,
      relativeAssessments = relative
    )
    primaryPosition.toList.flatMap { position =>
      drivers.flatMap { driver =>
        val evidence = compositeTacticalEvidence(
          context = context,
          position = position,
          driver = driver,
          lineRecords = lineFactRecords,
          evalRecords = evalRecords,
          motifRecords = motifRecords,
          relationRecords = relationRecords,
          relativeAssessments = relative
        )
        Option.when(evidence.nonEmpty) {
          ChessIdeaBuilder.fromEvidence(
            id = allocator.evidenceId(s"idea:tactical:${driver.id}:${allocator.key(line.role)}:${line.ref.rank}:${line.ref.rootMove}"),
            family = ChessIdeaFamily.Tactical,
            subject = line.ref.role.subject,
            primaryPosition = position,
            primaryLine = Some(line.ref),
            moveUci = Some(line.ref.rootMove),
            evidence = evidence.distinctBy(_.id),
            scope = line.ref.role.scope,
            confidence = tacticalIdeaConfidence(driver, relative, evalRecords)
          )
        }
      }
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
        case EvidenceRecord(ref, StructuralDeltaEvidence(delta), _) if pawnStructureDelta(delta) =>
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
            payload.threats.isProofSignalDefensivePressure =>
        val evidence =
          (ref :: parents ++
            ref.line.toList.flatMap(lineLayerRefs(context, _))).distinctBy(_.id)
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
        (assessment.evidence ::
          (assessment.counterfactualEvidence ++
            primaryCandidateComparisonEvidence(context, assessment) ++
            assessment.verdictCertificationEvidence.toList) ++
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

  private def primaryCandidateComparisonEvidence(
      context: JudgmentAssemblyContext,
      assessment: RelativeMoveAssessment
  ): List[EvidenceRef] =
    assessment.candidateComparisonEvidence.filter { ref =>
      context.evidenceGraph.byId.get(ref.id).exists {
        case EvidenceRecord(_, CandidateComparisonEvidence(fact), _) =>
          fact.kind == CandidateComparisonKind.PlayedVsBest &&
            fact.referenceLine == assessment.reference.ref &&
            fact.candidateLine == assessment.candidate.ref
        case _ =>
          false
      }
    }

  private def relativeCauseIdeas(
      context: JudgmentAssemblyContext,
      allocator: JudgmentProvenanceAllocator
  ): List[ChessIdea] =
    context.evidenceGraph.records.flatMap {
      case EvidenceRecord(ref, RelativeCauseFactEvidence(cause), parents) =>
        val subjectLine = cause.eventLine
        familiesForRelativeCause(context, ref, cause, parents).map { family =>
          val familyEvidence =
            if family == ChessIdeaFamily.Conversion then
              (ref :: parents ++ conversionContextEvidence(context, ref.position)).distinctBy(_.id)
            else (ref :: parents).distinctBy(_.id)
          ChessIdeaBuilder.fromEvidence(
            id = allocator.evidenceId(
              s"idea:${allocator.key(family)}:relative-cause:${allocator.key(cause.kind)}:${allocator.key(subjectLine.rootMove)}:${allocator.key(ref.id)}"
            ),
            family = family,
            subject = subjectForRelativeCause(cause, subjectLine),
            primaryPosition = ref.position,
            primaryLine = Some(subjectLine),
            moveUci = Some(subjectLine.rootMove),
            evidence = familyEvidence,
            scope = ref.scope,
            confidence = ref.confidence
          )
        }
      case _ =>
        Nil
    }

  private def familyForRelativeCause(kind: RelativeCauseKind): ChessIdeaFamily =
    ClaimEventCluster.kindForCause(kind) match
      case Some(ClaimEventClusterKind.TacticalEvent)   => ChessIdeaFamily.Tactical
      case Some(ClaimEventClusterKind.DefensiveEvent)  => ChessIdeaFamily.Defensive
      case Some(ClaimEventClusterKind.ConversionEvent) => ChessIdeaFamily.Conversion
      case Some(ClaimEventClusterKind.MaterialEvent)   => ChessIdeaFamily.Material
      case None                                        => ChessIdeaFamily.Strategic

  private def familiesForRelativeCause(
      context: JudgmentAssemblyContext,
      ref: EvidenceRef,
      cause: RelativeCauseFact,
      parents: List[EvidenceRef]
  ): List[ChessIdeaFamily] =
    val parentRecords = recordsForRefs(context, parents)
    cause.kind match
      case RelativeCauseKind.MaterialSwing =>
        val promoted =
          List(
            Some(ChessIdeaFamily.Material),
            Option.when(
              hasConcreteTacticalSupport(parentRecords) ||
                materialSwingHasTacticalProof(cause, parentRecords)
            )(ChessIdeaFamily.Tactical),
            Option.when(hasConversionContext(context, ref.position, parents))(ChessIdeaFamily.Conversion)
          ).flatten
        promoted.distinct
      case RelativeCauseKind.SacrificeCompensation =>
        val promoted =
          List(
            Some(ChessIdeaFamily.Material),
            Option.when(hasConcreteTacticalSupport(parentRecords))(ChessIdeaFamily.Tactical),
            Option.when(hasStrategicCompensationSupport(parentRecords))(ChessIdeaFamily.Strategic)
          ).flatten
        promoted.distinct
      case kind if strategicRelativeCause(kind) =>
        Option.when(hasStrategicRelativeCauseSupport(parentRecords))(ChessIdeaFamily.Strategic).toList
      case _ =>
        val base = familyForRelativeCause(cause.kind)
        val conversionFamily =
          Option.when(
            materialConversionCause(cause.kind) &&
              hasConversionContext(context, ref.position, parents)
          )(ChessIdeaFamily.Conversion)
        (base :: conversionFamily.toList).distinct

  private def materialConversionCause(kind: RelativeCauseKind): Boolean =
    kind == RelativeCauseKind.RecaptureRecoveryWindow || kind == RelativeCauseKind.MaterialSwing

  private def materialSwingHasTacticalProof(
      cause: RelativeCauseFact,
      records: List[EvidenceRecord]
  ): Boolean =
    val engineBackedMaterialSwing =
      cause.winPercentLossForMover >= JudgmentThresholds.INACCURACY_WP ||
        cause.candidateWinPercentDeltaForMover >= JudgmentThresholds.PLAYABLE_LOSS_WP
    engineBackedMaterialSwing &&
      records.exists {
        case EvidenceRecord(_, payload: LineFactEvidence, _) =>
          lineHasConcreteConsequence(payload)
        case _ =>
          false
      }

  private def strategicRelativeCause(kind: RelativeCauseKind): Boolean =
    ClaimEventCluster.kindForCause(kind).isEmpty

  private def recordsForRefs(
      context: JudgmentAssemblyContext,
      refs: List[EvidenceRef]
  ): List[EvidenceRecord] =
    refs.flatMap(ref => context.evidenceGraph.byId.get(ref.id))

  private def hasConcreteTacticalSupport(records: List[EvidenceRecord]): Boolean =
    records.exists {
      case EvidenceRecord(_, _: RelationFactEvidence, _) =>
        true
      case EvidenceRecord(_, MoveMotifEvidence(moveUci, motifs), _) =>
        motifs.exists(motif =>
          TacticalMotifClassifier.isRootMoveMotif(moveUci, motif) &&
            TacticalMotifClassifier.isCauseEligible(motif)
        )
      case EvidenceRecord(_, payload: LineFactEvidence, _) =>
        lineHasConcreteConsequence(payload)
      case EvidenceRecord(_, EvalFactEvidence(_, _, mate, _), _) =>
        mate.nonEmpty
      case _ =>
        false
    }

  private def hasStrategicCompensationSupport(records: List[EvidenceRecord]): Boolean =
    records.exists {
      case EvidenceRecord(_, StrategicFactEvidence(StrategicFactKind.Compensation, facts, relatedPlans, confidence), _) =>
        confidence >= 0.35 && (facts.nonEmpty || relatedPlans.nonEmpty)
      case EvidenceRecord(_, PlanPressureEvidence(scoring, _), _) =>
        scoring.topPlans.exists(plan => plan.evidence.nonEmpty || plan.support.nonEmpty)
      case EvidenceRecord(_, payload: PawnStructureFactEvidence, _) =>
        ClaimTruthPolicy.pawnStructureCanAnchorPlan(payload)
      case _ =>
        false
    }

  private def hasStrategicRelativeCauseSupport(records: List[EvidenceRecord]): Boolean =
    records.exists {
      case EvidenceRecord(_, StrategicFactEvidence(_, facts, relatedPlans, confidence), _) =>
        confidence >= 0.35 && (facts.nonEmpty || relatedPlans.nonEmpty)
      case EvidenceRecord(_, payload: PawnStructureFactEvidence, _) =>
        ClaimTruthPolicy.pawnStructureCanAnchorPlan(payload)
      case EvidenceRecord(_, StructuralDeltaEvidence(delta), _) =>
        delta.hasConsequence
      case EvidenceRecord(_, PlanPressureEvidence(scoring, activePlans), _) =>
        ClaimTruthPolicy.planPressureHasDirectEvidence(scoring, activePlans)
      case _ =>
        false
    }

  private def hasConversionContext(
      context: JudgmentAssemblyContext,
      position: PositionNodeRef,
      parents: List[EvidenceRef]
  ): Boolean =
    conversionContextEvidence(context, position).nonEmpty ||
      parents
        .flatMap(parent => context.evidenceGraph.byId.get(parent.id))
        .exists {
          case EvidenceRecord(_, payload: LineFactEvidence, _) =>
            payload.consequenceProfile.hasConversionConsequence
          case _ =>
            false
        }

  private def conversionContextEvidence(
      context: JudgmentAssemblyContext,
      position: PositionNodeRef
  ): List[EvidenceRef] =
    context.evidenceGraph.recordsFor(position).collect {
      case record @ EvidenceRecord(_, SinglePositionEvidence(assessment), _)
          if assessment.simplifyBias.shouldSimplify || assessment.gamePhase.isEndgame =>
        record.ref
    }

  private def subjectForRelativeCause(cause: RelativeCauseFact, line: LineNodeRef): IdeaSubject =
    cause.kind match
      case kind if ClaimEventCluster.kindForCause(kind).contains(ClaimEventClusterKind.DefensiveEvent) =>
        IdeaSubject.Threat
      case RelativeCauseKind.PlanImprovement | RelativeCauseKind.PlanContradiction =>
        IdeaSubject.Plan
      case _ =>
        line.role.subject

  private def strategicIdeas(
      context: JudgmentAssemblyContext,
      allocator: JudgmentProvenanceAllocator
  ): List[ChessIdea] =
    val strategicFactIdeas =
      context.evidenceGraph.records.collect {
        case EvidenceRecord(ref, payload: StrategicFactEvidence, _) if canSeedStrategicIdea(payload) =>
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
    val castlingMoveIdeas =
      context.evidenceGraph.records.flatMap {
        case EvidenceRecord(ref, MoveMotifEvidence(moveUci, motifs), parents) if rootCastlingMotif(moveUci, motifs) =>
          val transition = transitionForScope(context, ref.scope)
          val afterPositionEvidence =
            transition.toList.flatMap { edge =>
              recordsForPosition(context, EvidenceLayer.Board, edge.to) ++
                recordsForPosition(context, EvidenceLayer.SinglePosition, edge.to) ++
                recordsForPosition(context, EvidenceLayer.PawnStructure, edge.to)
            }
          val evidence =
            (ref :: parents ++
              transition.toList.map(_.evidence) ++
              ref.line.toList.flatMap(lineLayerRefs(context, _)) ++
              afterPositionEvidence).distinctBy(_.id)
          Some(ChessIdeaBuilder.fromEvidence(
            id = allocator.evidenceId(s"idea:strategic-castling:${allocator.key(ref.id)}"),
            family = ChessIdeaFamily.Strategic,
            subject = transition.map(_.role.subject).getOrElse(subjectForLine(ref.line)),
            primaryPosition = ref.position,
            primaryLine = ref.line,
            moveUci = Some(moveUci),
            evidence = evidence,
            scope = ref.scope,
            confidence = ref.confidence
          ))
        case _ =>
          None
      }
    val structuralDeltaIdeas =
      context.evidenceGraph.records.flatMap {
        case EvidenceRecord(ref, StructuralDeltaEvidence(delta), parents) if strategicMoveDelta(delta) =>
          transitionForScope(context, ref.scope).map { transition =>
            val evidence =
              (ref :: transition.evidence :: parents ++
                ref.line.toList.flatMap(lineLayerRefs(context, _)) ++
                recordsForPosition(context, EvidenceLayer.Board, transition.to) ++
                recordsForPosition(context, EvidenceLayer.SinglePosition, transition.to)).distinctBy(_.id)
            ChessIdeaBuilder.fromEvidence(
              id = allocator.evidenceId(s"idea:strategic-delta:${allocator.key(ref.id)}"),
              family = ChessIdeaFamily.Strategic,
              subject = transition.role.subject,
              primaryPosition = ref.position,
              primaryLine = ref.line,
              moveUci = Some(transition.moveUci),
              evidence = evidence,
              scope = ref.scope,
              confidence = ref.confidence
            )
          }
        case _ =>
          None
      }
    val planPressureIdeas =
      context.evidenceGraph.records.flatMap {
        case EvidenceRecord(ref, PlanPressureEvidence(scoring, activePlans), parents) =>
          val evidence =
            (ref :: parents ++
              ref.line.toList.flatMap(lineLayerRefs(context, _)) ++
              recordsForPosition(context, EvidenceLayer.Strategic, ref.position) ++
              recordsForPosition(context, EvidenceLayer.PawnStructure, ref.position) ++
              recordsForPosition(context, EvidenceLayer.PlanTransition, ref.position) ++
              recordsForPosition(context, EvidenceLayer.SinglePosition, ref.position)).distinctBy(_.id)
          Option.when(ClaimTruthPolicy.planPressureCanSeedIdea(scoring, activePlans, evidence, context.evidenceGraph))(
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
          )
        case _ =>
          None
    }
    strategicFactIdeas ++ castlingMoveIdeas ++ structuralDeltaIdeas ++ planPressureIdeas

  private def canSeedStrategicIdea(payload: StrategicFactEvidence): Boolean =
    payload.facts.nonEmpty || payload.relatedPlans.nonEmpty

  private def openingIdeas(
      context: JudgmentAssemblyContext,
      allocator: JudgmentProvenanceAllocator
  ): List[ChessIdea] =
    context.evidenceGraph.records.collect {
      case EvidenceRecord(ref, ApplicabilityAssessmentEvidence(assessment), parents)
          if assessment.canCertifyOpeningClaim =>
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

  private def tacticalDrivers(
      motifs: List[Motif],
      lineFacts: List[LineFactEvidence],
      evalFacts: List[EvalFactEvidence],
      relationRecords: List[EvidenceRecord],
      relativeAssessments: List[RelativeMoveAssessment]
  ): List[TacticalIdeaDriver] =
    val motifDrivers = motifs.flatMap(tacticalDriverForMotif)
    val relationDrivers =
      relationRecords.collect {
        case EvidenceRecord(_, RelationFactEvidence(kind, _, _, _, _), _) =>
          tacticalDriverForRelation(kind)
      }
    val materialDrivers = lineFacts.flatMap(lineConsequenceDrivers)
    val lineDrivers =
      List(
        Option.when(evalFacts.exists(_.mate.nonEmpty))(TacticalIdeaDriver.KingForcing),
        Option.when(relativeAssessments.exists(relativeRefutesCandidate) && (motifDrivers.nonEmpty || relationRecords.nonEmpty))(
          TacticalIdeaDriver.Refutation
        )
      ).flatten
    val drivers = (motifDrivers ++ relationDrivers ++ materialDrivers ++ lineDrivers).distinct
    Option
      .when(tacticalCompositeHasProof(drivers, lineFacts, evalFacts, relationRecords, relativeAssessments))(
        drivers
      )
      .getOrElse(Nil)

  private def tacticalCompositeHasProof(
      drivers: List[TacticalIdeaDriver],
      lineFacts: List[LineFactEvidence],
      evalFacts: List[EvalFactEvidence],
      relationRecords: List[EvidenceRecord],
      relativeAssessments: List[RelativeMoveAssessment]
  ): Boolean =
    val hasTacticalAnchor =
      drivers.nonEmpty || relationRecords.nonEmpty
    val hasLineConsequence =
      lineFacts.exists(lineHasConcreteConsequence) ||
        evalFacts.exists(_.mate.nonEmpty) ||
        relationRecords.exists {
          case EvidenceRecord(_, RelationFactEvidence(_, _, _, lineMoves, _), _) => lineMoves.nonEmpty
          case _                                                                => false
        }
    val hasEngineOrForcingProof =
      evalFacts.exists(_.mate.nonEmpty) ||
        lineFacts.exists(_.hasProofSignalConsequence) ||
        relativeAssessments.exists(relativeSupportsTacticalIdea)
    hasTacticalAnchor && hasLineConsequence && hasEngineOrForcingProof

  private def lineHasConcreteConsequence(line: LineFactEvidence): Boolean =
    line.consequenceProfile.hasConcreteProofSignal

  private def lineConsequenceDrivers(line: LineFactEvidence): List[TacticalIdeaDriver] =
    val typedDrivers =
      line.consequenceProfile.tacticalDriverKinds.flatMap {
          case LineConsequenceKind.MaterialGain | LineConsequenceKind.MaterialLoss =>
            List(TacticalIdeaDriver.MaterialGain)
          case LineConsequenceKind.RecaptureSequence | LineConsequenceKind.RecoveryWindow =>
            List(TacticalIdeaDriver.RecaptureChoice)
          case LineConsequenceKind.ImmediateReplyCheck =>
            List(TacticalIdeaDriver.Tempo)
          case LineConsequenceKind.PromotionRace =>
            List(TacticalIdeaDriver.PawnPromotion)
          case LineConsequenceKind.ForcedTheme | LineConsequenceKind.Sacrifice =>
            Nil
      }
    typedDrivers.distinct

  private def relativeSupportsTacticalIdea(assessment: RelativeMoveAssessment): Boolean =
    assessment.comparison.winPercentLossForMover >= JudgmentThresholds.SIGNIFICANT_THREAT_WP ||
      assessment.comparison.candidateWinPercentDeltaForMover >= JudgmentThresholds.PLAYABLE_LOSS_WP ||
      assessment.comparison.candidateSet.exists(set =>
        set.onlyMove ||
          set.bestToSecondWinPercentGapForMover.exists(_ >= JudgmentThresholds.ONLY_MOVE_GAP_WP)
      )

  private def relativeRefutesCandidate(assessment: RelativeMoveAssessment): Boolean =
    assessment.comparison.winPercentLossForMover >= JudgmentThresholds.INACCURACY_WP

  private def tacticalDriverForMotif(motif: Motif): List[TacticalIdeaDriver] =
    motif match
      case m: Motif.Check =>
        List(TacticalIdeaDriver.KingForcing) ++
          Option.when(m.checkType == Motif.CheckType.Mate || m.checkType == Motif.CheckType.Smothered)(
            TacticalIdeaDriver.Refutation
          ).toList
      case _: Motif.DoubleCheck | _: Motif.BackRankMate | _: Motif.MateNet | _: Motif.SmotheredMate =>
        List(TacticalIdeaDriver.KingForcing)
      case m: Motif.Capture =>
        m.captureType match
          case Motif.CaptureType.Recapture =>
            List(TacticalIdeaDriver.RecaptureChoice)
          case Motif.CaptureType.Exchange | Motif.CaptureType.ExchangeSacrifice =>
            List(TacticalIdeaDriver.MaterialGain, TacticalIdeaDriver.Conversion)
          case Motif.CaptureType.Winning | Motif.CaptureType.Sacrifice =>
            List(TacticalIdeaDriver.MaterialGain)
          case Motif.CaptureType.Normal =>
            Nil
      case _: Motif.Zwischenzug =>
        List(TacticalIdeaDriver.Tempo, TacticalIdeaDriver.RecaptureChoice)
      case _: Motif.Fork | _: Motif.Pin | _: Motif.Skewer | _: Motif.DiscoveredAttack |
          _: Motif.RemovingTheDefender | _: Motif.Deflection | _: Motif.Decoy | _: Motif.XRay |
          _: Motif.Overloading | _: Motif.Interference | _: Motif.Clearance | _: Motif.Battery =>
        List(TacticalIdeaDriver.RelationMechanism)
      case _: Motif.TrappedPiece | _: Motif.Domination =>
        List(TacticalIdeaDriver.MaterialGain)
      case _: Motif.PawnPromotion | _: Motif.PassedPawnPush =>
        List(TacticalIdeaDriver.PawnPromotion)
      case _: Motif.StalemateThreat =>
        List(TacticalIdeaDriver.DrawResource)
      case _: Motif.WeakBackRank =>
        Nil
      case _ =>
        Nil

  private def tacticalDriverForRelation(kind: RelationFactKind): TacticalIdeaDriver =
    kind match
      case RelationFactKind.DoubleCheck | RelationFactKind.BackRankMate | RelationFactKind.MateNet | RelationFactKind.GreekGift =>
        TacticalIdeaDriver.KingForcing
      case RelationFactKind.DefenderTrade =>
        TacticalIdeaDriver.RecaptureChoice
      case RelationFactKind.HangingPiece | RelationFactKind.TrappedPiece | RelationFactKind.Domination =>
        TacticalIdeaDriver.MaterialGain
      case RelationFactKind.Zwischenzug =>
        TacticalIdeaDriver.Tempo
      case RelationFactKind.BadPieceLiquidation =>
        TacticalIdeaDriver.Conversion
      case RelationFactKind.StalemateTrap | RelationFactKind.PerpetualCheck =>
        TacticalIdeaDriver.DrawResource
      case _ =>
        TacticalIdeaDriver.RelationMechanism

  private def compositeTacticalEvidence(
      context: JudgmentAssemblyContext,
      position: PositionNodeRef,
      driver: TacticalIdeaDriver,
      lineRecords: List[EvidenceRecord],
      evalRecords: List[EvidenceRecord],
      motifRecords: List[EvidenceRecord],
      relationRecords: List[EvidenceRecord],
      relativeAssessments: List[RelativeMoveAssessment]
  ): List[EvidenceRef] =
    val driverMotifRefs =
      motifRecords.collect {
        case record @ EvidenceRecord(_, MoveMotifEvidence(moveUci, motifs), _)
            if motifs.exists(motif =>
              TacticalMotifClassifier.isRootMoveMotif(moveUci, motif) &&
                tacticalDriverForMotif(motif).contains(driver)
            ) =>
          record.ref
      }
    val driverRelationRefs =
      relationRecords.collect {
        case record @ EvidenceRecord(_, RelationFactEvidence(kind, _, _, _, _), _) if tacticalDriverForRelation(kind) == driver =>
          record.ref
      }
    val driverLineRefs =
      lineRecords.collect {
        case record @ EvidenceRecord(_, line: LineFactEvidence, _) if lineConsequenceDrivers(line).contains(driver) =>
          record.ref
      }
    val lineRefs = (lineRecords ++ evalRecords).map(_.ref)
    val engineRefs = relativeEvidenceRefs(relativeAssessments)
    val boardRefs =
      context.evidenceGraph.records.collect {
        case record if record.ref.layer == EvidenceLayer.Board && record.ref.position == position =>
          record.ref
      }
    val driverRefs = driver match
      case TacticalIdeaDriver.Refutation =>
        driverMotifRefs ++ relationRecords.map(_.ref) ++ engineRefs
      case _ =>
        driverMotifRefs ++ driverRelationRefs ++ driverLineRefs
    if driverRefs.isEmpty then Nil
    else (driverRefs ++ lineRefs ++ engineRefs ++ boardRefs.take(2)).distinctBy(_.id)

  private def transitionTacticalEvidence(
      context: JudgmentAssemblyContext,
      edge: MoveTransitionEdge,
      driver: TacticalIdeaDriver,
      motifRecords: List[EvidenceRecord],
      relationRecords: List[EvidenceRecord]
  ): List[EvidenceRef] =
    val driverMotifRefs =
      motifRecords.collect {
        case record @ EvidenceRecord(_, MoveMotifEvidence(moveUci, motifs), _)
            if motifs.exists(motif =>
              TacticalMotifClassifier.isRootMoveMotif(moveUci, motif) &&
                tacticalDriverForMotif(motif).contains(driver)
            ) =>
          record.ref
      }
    val driverRelationRefs =
      relationRecords.collect {
        case record @ EvidenceRecord(_, RelationFactEvidence(kind, _, _, _, _), _) if tacticalDriverForRelation(kind) == driver =>
          record.ref
      }
    val transitionRef =
      context.evidenceGraph.byId.get(edge.evidence.id).map(_.ref).toList
    val boardRefs =
      context.evidenceGraph.records.collect {
        case record if record.ref.layer == EvidenceLayer.Board && record.ref.position == edge.from =>
          record.ref
      }
    (driverMotifRefs ++ driverRelationRefs ++ transitionRef ++ boardRefs.take(2)).distinctBy(_.id)

  private def transitionRecordMentionsMove(record: EvidenceRecord, moveUci: String): Boolean =
    record.payload match
      case MoveMotifEvidence(move, _) =>
        move == moveUci
      case MoveTransitionEvidence(move, _, _) =>
        move == moveUci
      case RelationFactEvidence(_, _, _, lineMoves, _) =>
        lineMoves.headOption.contains(moveUci) || record.ref.scope == EvidenceScope.PlayedTransition
      case _ =>
        false

  private def relativeEvidenceRefs(assessments: List[RelativeMoveAssessment]): List[EvidenceRef] =
    assessments.flatMap(assessment => assessment.evidence :: assessment.counterfactualEvidence).distinctBy(_.id)

  private def relativeAssessmentsForLine(
      context: JudgmentAssemblyContext,
      line: LineNodeRef
  ): List[RelativeMoveAssessment] =
    context.relativeAssessments.filter(assessment =>
      assessment.candidate.ref == line ||
        assessment.reference.ref == line ||
        assessment.comparison.candidateLine == line ||
        assessment.comparison.referenceLine == line
    )

  private def tacticalIdeaConfidence(
      driver: TacticalIdeaDriver,
      relativeAssessments: List[RelativeMoveAssessment],
      evalRecords: List[EvidenceRecord]
  ): EvidenceConfidence =
    if evalRecords.exists {
        case EvidenceRecord(_, EvalFactEvidence(_, _, mate, _), _) => mate.nonEmpty
        case _                                                     => false
      } || relativeAssessments.exists(relativeSupportsTacticalIdea)
    then EvidenceConfidence.EngineBacked
    else if driver == TacticalIdeaDriver.RelationMechanism then EvidenceConfidence.LegalReplayVerified
    else EvidenceConfidence.Mixed

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

  private def pawnStructureCarriesTheme(payload: PawnStructureFactEvidence): Boolean =
    payload.profile.primary != StructureId.Unknown && payload.profile.confidence >= 0.65 ||
      payload.pawnPlay.exists(_.primaryDriver != PawnPlayDriver.Quiet) ||
      payload.alignment.exists(alignment =>
        alignment.band == AlignmentBand.OnBook ||
          alignment.band == AlignmentBand.Playable ||
          alignment.band == AlignmentBand.OffPlan
      )

  private def pawnStructureDelta(delta: StructuralDelta): Boolean =
    delta.openedFiles.nonEmpty ||
      delta.semiOpenedFiles.nonEmpty ||
      delta.newWeakPawns.nonEmpty ||
      delta.createdTension.nonEmpty ||
      delta.resolvedTension.nonEmpty ||
      delta.pawnTensionDelta != 0

  private def strategicMoveDelta(delta: StructuralDelta): Boolean =
    delta.developmentMoves.nonEmpty ||
      delta.developmentDelta > 0 ||
      delta.centerControlDelta > 0 ||
      delta.targetPressureGain > 0 ||
      delta.targetPressureDelta > 0 ||
      delta.fileAccessDelta > 0 ||
      delta.kingShelterDelta != 0 ||
      delta.lineUnlockDelta > 0 ||
      delta.fileOccupation.nonEmpty ||
      delta.createdTargetPressure.nonEmpty ||
      delta.mobilityDelta >= 3

  private def rootCastlingMotif(moveUci: String, motifs: List[Motif]): Boolean =
    motifs.exists {
      case Motif.Castling(_, _, plyIndex, move) =>
        move.contains(moveUci) || (move.isEmpty && plyIndex == 0 && castlingMove(moveUci))
      case _ =>
        false
    }

  private def castlingMove(moveUci: String): Boolean =
    moveUci == "e1g1" || moveUci == "e1c1" || moveUci == "e8g8" || moveUci == "e8c8"

  private def subjectForLine(line: Option[LineNodeRef]): IdeaSubject =
    line.map(_.role.subject).getOrElse(IdeaSubject.Position)

  private def transitionForScope(context: JudgmentAssemblyContext, scope: EvidenceScope): Option[MoveTransitionEdge] =
    TransitionEdgeRole.fromScope(scope).flatMap(context.transition)
