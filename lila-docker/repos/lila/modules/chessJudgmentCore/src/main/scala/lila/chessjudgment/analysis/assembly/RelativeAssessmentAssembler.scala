package lila.chessjudgment.analysis.assembly

import chess.Color
import lila.chessjudgment.analysis.evaluation.{ JudgmentThresholds, PerspectiveMath, VerdictThresholdPolicy }
import lila.chessjudgment.analysis.transition.TransitionFactNormalizer
import lila.chessjudgment.model.judgment.*

final case class RelativeAssessmentAssembly(
    input: NormalizedMoveReviewInput,
    context: JudgmentAssemblyContext
)

object RelativeAssessmentAssembler:

  private final case class ComparisonEvidenceNeighborhood(
      referenceEndpoint: List[EvidenceRecord],
      candidateEndpoint: List[EvidenceRecord],
      rootContext: List[EvidenceRecord],
      referenceTransition: List[EvidenceRecord],
      candidateTransition: List[EvidenceRecord],
      referenceAfterPosition: List[EvidenceRecord],
      candidateAfterPosition: List[EvidenceRecord],
      parents: List[EvidenceRecord]
  ):
    val referenceRecords: List[EvidenceRecord] =
      (referenceEndpoint ++ referenceTransition ++ referenceAfterPosition).distinctBy(_.ref.id)
    val candidateRecords: List[EvidenceRecord] =
      (candidateEndpoint ++ candidateTransition ++ candidateAfterPosition).distinctBy(_.ref.id)
    val sharedRecords: List[EvidenceRecord] =
      (rootContext ++ parents).distinctBy(_.ref.id)
    val involvedRecords: List[EvidenceRecord] =
      (referenceRecords ++ candidateRecords ++ sharedRecords).distinctBy(_.ref.id)

  private final case class RelativeCauseProofRecords(
      directProof: List[EvidenceRecord],
      contrastProof: List[EvidenceRecord],
      contextSupport: List[EvidenceRecord]
  ):
    def all: List[EvidenceRecord] =
      (directProof ++ contrastProof ++ contextSupport).distinctBy(_.ref.id)

  def assemble(raw: RawMoveReviewInput): Option[RelativeAssessmentAssembly] =
    EvidenceFactAssembler.assemble(raw).map(enrich)

  def enrich(assembly: EvidenceFactAssembly): RelativeAssessmentAssembly =
    val input = assembly.input
    val context = assembly.context
    val relativeAssembly =
      for
        played <- context.playedTransition
        referenceTransition <- context.referenceTransition
        reference <- context.line(LineNodeRole.BestReference)
        candidate <- context.line(LineNodeRole.Played)
        root <- context.position(PositionNodeRole.Before).map(_.ref)
        mover <- root.sideToMove.orElse(input.sideToMove)
      yield {
      val allocator = JudgmentProvenanceAllocator.forInput(input)
      val comparison = compare(mover, reference, candidate, candidateSetComparison(mover, context.lines, reference))
      val primaryConfidence = comparisonConfidence(reference, candidate)
      val comparisonRecords =
        candidateComparisonRecords(
          mover = mover,
          context = context,
          reference = reference,
          candidate = candidate,
          primaryComparison = comparison,
          root = root,
          allocator = allocator
        )
      val counterfactual =
        TransitionFactNormalizer.fromCounterfactual(
          id = allocator.evidenceId(s"counterfactual:played-vs-reference:${candidate.ref.rootMove}"),
          referenceLine = reference.ref,
          candidateLine = candidate.ref,
          comparison = comparison,
          position = root,
          scope = EvidenceScope.Counterfactual,
          confidence = primaryConfidence,
          parents = parentsForLine(context, reference) ++ parentsForLine(context, candidate) ++ List(played.evidence)
        )
      val causeRecords =
        comparisonRecords.flatMap(record => relativeCauseRecords(context, root, allocator, record))
      val playedMoveSet = Set(JudgmentSubjectBinding.normalizeMove(played.moveUci))
      val playedCauseRecords =
        causeRecords.filter(record =>
          JudgmentSubjectBinding.primaryPlayed(JudgmentSubjectBinding.recordBinding(record, playedMoveSet))
        )
      val playedVerdictCauses =
        playedCauseRecords.collect {
          case EvidenceRecord(_, RelativeCauseFactEvidence(cause), _) => cause
        }
      val certification =
        MoveVerdictCertification(
          playedMove = played.moveUci,
          verdict = comparison.verdict,
          primaryComparison = CandidateComparisonFact(
            kind = CandidateComparisonKind.PlayedVsBest,
            referenceLine = reference.ref,
            candidateLine = candidate.ref,
            comparison = comparison
          ),
          causes = playedVerdictCauses
        )
      val certificationRecord =
        TransitionFactNormalizer.fromMoveVerdictCertification(
          id = allocator.evidenceId(s"move-verdict:${candidate.ref.rootMove}"),
          certification = certification,
          position = root,
          scope = EvidenceScope.Counterfactual,
          confidence = primaryConfidence,
          parents = (comparisonRecords
            .filter(record =>
              JudgmentSubjectBinding.primaryPlayed(JudgmentSubjectBinding.recordBinding(record, playedMoveSet))
            )
            .map(_.ref) ++
            playedCauseRecords.map(_.ref) :+ counterfactual.ref).distinctBy(_.id)
        )
      val relativeEvidence =
        allocator.evidenceRef(
          suffix = s"relative-assessment:${candidate.ref.rootMove}",
          producer = EvidenceProducer.RelativeMoveProducer,
          layer = EvidenceLayer.RelativeAssessment,
          position = root,
          line = Some(candidate.ref),
          scope = EvidenceScope.Counterfactual,
          confidence = primaryConfidence
        )
      val assessment =
        RelativeMoveAssessmentBuilder.fromComparison(
          played = played,
          referenceTransition = Some(referenceTransition),
          reference = reference,
          candidate = candidate,
          comparison = comparison,
          collapse = None,
          confidence = primaryConfidence,
          evidence = relativeEvidence,
          counterfactualEvidence = List(counterfactual.ref),
          candidateComparisonEvidence = comparisonRecords.map(_.ref),
          relativeCauseEvidence = playedCauseRecords.map(_.ref),
          verdictCertificationEvidence = Some(certificationRecord.ref)
        )
      val assessmentRecord = TransitionFactNormalizer.fromRelativeAssessment(assessment)
      RelativeAssessmentAssembly(
        input = input,
        context = context
          .withEvidence(comparisonRecords ++ List(counterfactual) ++ causeRecords ++ List(certificationRecord, assessmentRecord))
          .withRelativeAssessment(assessment)
      )
      }
    relativeAssembly.getOrElse(RelativeAssessmentAssembly(input = input, context = context))

  private def compare(
      mover: Color,
      reference: CandidateLineNode,
      candidate: CandidateLineNode,
      candidateSet: Option[CandidateSetComparison]
  ): EvalComparison =
    val delta =
      PerspectiveMath.compareForMover(
        mover = mover,
        reference = PerspectiveMath.EvalPoint(reference.whitePovEvalCp, reference.mate),
        candidate = PerspectiveMath.EvalPoint(candidate.whitePovEvalCp, candidate.mate)
      )
    EvalComparison(
      mover = mover,
      referenceLine = reference.ref,
      candidateLine = candidate.ref,
      rawCandidateDeltaCpForDiagnostics = delta.rawCandidateDeltaCpForMover,
      candidateWinPercentDeltaForMover = delta.candidateWinPercentDeltaForMover,
      rawCpLossForDiagnostics = delta.rawCpLossForMover,
      winPercentLossForMover = delta.winPercentLossForMover,
      verdict = VerdictThresholdPolicy.verdictFromWinPercent(
        delta.candidateWinPercentDeltaForMover,
        delta.winPercentLossForMover
      ),
      candidateSet = candidateSet
    )

  private def candidateSetComparison(
      mover: Color,
      lines: List[CandidateLineNode],
      reference: CandidateLineNode
  ): Option[CandidateSetComparison] =
    val ordered =
      lines
        .filterNot(_.role == LineNodeRole.Threat)
        .sortBy(_.ref.rank)
        .groupBy(_.ref.rootMove)
        .values
        .map(_.minBy(_.ref.rank))
        .toList
        .sortBy(_.ref.rank)
    val second = ordered.find(_.ref.rootMove != reference.ref.rootMove)
    val gap =
      second.map(line =>
        PerspectiveMath.compareForMover(
          mover = mover,
          reference = PerspectiveMath.EvalPoint(reference.whitePovEvalCp, reference.mate),
          candidate = PerspectiveMath.EvalPoint(line.whitePovEvalCp, line.mate)
        )
      )
    Option.when(ordered.nonEmpty)(
      CandidateSetComparison(
        secondLine = second.map(_.ref),
        rawBestToSecondCpGapForDiagnostics = gap.map(_.rawCpLossForMover),
        bestToSecondWinPercentGapForMover = gap.map(_.winPercentLossForMover),
        candidateCount = ordered.size,
        onlyMove = gap.exists(_.winPercentLossForMover >= JudgmentThresholds.ONLY_MOVE_GAP_WP)
      )
    )
  private def candidateComparisonRecords(
      mover: Color,
      context: JudgmentAssemblyContext,
      reference: CandidateLineNode,
      candidate: CandidateLineNode,
      primaryComparison: EvalComparison,
      root: PositionNodeRef,
      allocator: JudgmentProvenanceAllocator
  ): List[EvidenceRecord] =
    val ordered = uniqueLines(context.lines)
    val second = ordered.find(_.ref.rootMove != reference.ref.rootMove)
    val alternatives =
      ordered.filter(line =>
        line.role == LineNodeRole.Alternative &&
          line.ref.rootMove != candidate.ref.rootMove &&
          line.ref.rootMove != reference.ref.rootMove
      )
    val comparisons =
      List(
        Some((CandidateComparisonKind.PlayedVsBest, reference, candidate, primaryComparison)),
        second.map(line =>
          (
            CandidateComparisonKind.BestVsSecond,
            reference,
            line,
            compare(mover, reference, line, None)
          )
        )
      ).flatten ++
        alternatives.flatMap { alternative =>
          List(
            (
              CandidateComparisonKind.PlayedVsAlternative,
              alternative,
              candidate,
              compare(mover, alternative, candidate, None)
            ),
            (
              CandidateComparisonKind.ReferenceVsAlternative,
              reference,
              alternative,
              compare(mover, reference, alternative, None)
            )
          )
        }
    comparisons
      .distinctBy { case (kind, ref, cand, _) => (kind, ref.ref.id, cand.ref.id) }
      .zipWithIndex
      .map { case ((kind, refLine, candLine, cmp), index) =>
        TransitionFactNormalizer.fromCandidateComparison(
          id = allocator.evidenceId(
            s"candidate-comparison:${allocator.key(kind)}:${allocator.key(refLine.ref.rootMove)}:${allocator.key(candLine.ref.rootMove)}:$index"
          ),
          comparison = CandidateComparisonFact(
            kind = kind,
            referenceLine = refLine.ref,
            candidateLine = candLine.ref,
            comparison = cmp
          ),
          position = root,
          scope = EvidenceScope.Counterfactual,
          confidence = comparisonConfidence(refLine, candLine),
          parents = (parentsForLine(context, refLine) ++ parentsForLine(context, candLine)).distinctBy(_.id)
        )
      }

  private def comparisonConfidence(reference: CandidateLineNode, candidate: CandidateLineNode): EvidenceConfidence =
    if lineHasEngineDepth(reference) && lineHasEngineDepth(candidate) then EvidenceConfidence.EngineBacked
    else EvidenceConfidence.Mixed

  private def comparisonConfidence(context: JudgmentAssemblyContext, fact: CandidateComparisonFact): EvidenceConfidence =
    val reference = context.lines.find(_.ref == fact.referenceLine)
    val candidate = context.lines.find(_.ref == fact.candidateLine)
    reference.zip(candidate)
      .map { case (referenceLine, candidateLine) => comparisonConfidence(referenceLine, candidateLine) }
      .getOrElse(EvidenceConfidence.Mixed)

  private def lineHasEngineDepth(line: CandidateLineNode): Boolean =
    JudgmentThresholds.engineBackedByDepth(line.depth, line.mate)

  private def uniqueLines(lines: List[CandidateLineNode]): List[CandidateLineNode] =
    lines
      .filterNot(_.role == LineNodeRole.Threat)
      .sortBy(_.ref.rank)
      .groupBy(_.ref.rootMove)
      .values
      .map(_.minBy(_.ref.rank))
      .toList
      .sortBy(_.ref.rank)

  private def relativeCauseRecords(
      context: JudgmentAssemblyContext,
      root: PositionNodeRef,
      allocator: JudgmentProvenanceAllocator,
      comparisonRecord: EvidenceRecord
  ): List[EvidenceRecord] =
    comparisonRecord.payload match
      case CandidateComparisonEvidence(fact) =>
        val neighborhood = comparisonNeighborhood(context, root, fact)
        val comparisonProof = comparisonProofRecords(neighborhood)
        val causes = mergeCauseCandidates(inferCauseCandidates(neighborhood, fact))
        causes.zipWithIndex.map { case (candidate, index) =>
          val kind = candidate.kind
          val support = candidate.support
          val supportRefs = support.map(_.ref).distinctBy(_.id)
          val binding =
            RelativeCauseFact.binding(
              kind = kind,
              comparisonKind = fact.kind,
              referenceLine = fact.referenceLine,
              candidateLine = fact.candidateLine,
              supportEvidence = supportRefs,
              explicitSourceSide = candidate.sourceSide
            )
          val proofRecords =
            relativeCauseProofRecords(context.evidenceGraph, fact, support, comparisonProof, neighborhood)
          val proof = relativeCauseProof(
            context.evidenceGraph,
            kind,
            proofRecords.directProof,
            proofRecords.contrastProof,
            proofRecords.contextSupport
          )
          val retainedProof = Some(proof).filter(proof => proof.hasTypedDepth || proof.hasContextSupport)
          val cause =
            RelativeCauseFact(
              kind = kind,
              comparisonKind = fact.kind,
              referenceLine = fact.referenceLine,
              candidateLine = fact.candidateLine,
              verdict = fact.comparison.verdict,
              winPercentLossForMover = fact.comparison.winPercentLossForMover,
              candidateWinPercentDeltaForMover = fact.comparison.candidateWinPercentDeltaForMover,
              supportEvidence = supportRefs,
              evidenceLines = binding.evidenceLines,
              role = binding.role,
              eventLine = binding.eventLine,
              sourceSide = binding.sourceSide,
              importance = binding.importance
            )(proof = retainedProof)
          TransitionFactNormalizer.fromRelativeCause(
            id = allocator.evidenceId(
              s"relative-cause:${allocator.key(fact.kind)}:${allocator.key(kind)}:${allocator.key(fact.referenceLine.rootMove)}:${allocator.key(fact.candidateLine.rootMove)}:$index"
            ),
            cause = cause,
            position = root,
            scope = EvidenceScope.Counterfactual,
            confidence = comparisonConfidence(context, fact),
            parents = (comparisonRecord.ref :: proofRecords.all.map(_.ref)).distinctBy(_.id)
          )
        }
      case _ => Nil

  private def relativeCauseProofRecords(
      graph: TypedEvidenceGraph,
      fact: CandidateComparisonFact,
      support: List[EvidenceRecord],
      comparisonProof: List[EvidenceRecord],
      neighborhood: ComparisonEvidenceNeighborhood
  ): RelativeCauseProofRecords =
    val directRecords = proofSectionRecords(graph, support.distinctBy(_.ref.id))
    val supportIds = supportClosureRecordIds(graph, support)
    val directIds = directRecords.map(_.ref.id).toSet
    val contrastRecords =
      comparisonProof
        .filterNot(record => directIds.contains(record.ref.id) || supportIds.contains(record.ref.id))
        .distinctBy(_.ref.id)
    val contrastIds = proofSectionRecords(graph, contrastRecords).map(_.ref.id).toSet
    val proofIds = directIds ++ contrastIds ++ supportIds
    val contextRecords =
      neighborhood.sharedRecords
        .filter(record => contextSupportRecord(fact, record))
        .filterNot(record => proofSectionRecordIds(graph, List(record)).exists(proofIds.contains))
        .distinctBy(_.ref.id)
    RelativeCauseProofRecords(
      directProof = directRecords,
      contrastProof = contrastRecords,
      contextSupport = contextRecords
    )

  private def relativeCauseProof(
      graph: TypedEvidenceGraph,
      kind: RelativeCauseKind,
      directRecords: List[EvidenceRecord],
      contrastRecords: List[EvidenceRecord],
      contextRecords: List[EvidenceRecord]
  ): RelativeCauseProof =
    RelativeCauseProof(
      directProof = relativeCauseProofSection(
        role = RelativeCauseProofRole.DirectProof,
        strength = RelativeCauseProofStrength.Primary,
        kind = kind,
        graph = graph,
        records = directRecords,
        includeContextLayers = false
      ),
      contrastProof = relativeCauseProofSection(
        role = RelativeCauseProofRole.ContrastProof,
        strength = RelativeCauseProofStrength.Supporting,
        kind = kind,
        graph = graph,
        records = contrastRecords,
        includeContextLayers = false
      ),
      contextSupport = relativeCauseProofSection(
        role = RelativeCauseProofRole.ContextSupport,
        strength = RelativeCauseProofStrength.WeakHint,
        kind = kind,
        graph = graph,
        records = contextRecords,
        includeContextLayers = true
      )
    )

  private def relativeCauseProofSection(
      role: RelativeCauseProofRole,
      strength: RelativeCauseProofStrength,
      kind: RelativeCauseKind,
      graph: TypedEvidenceGraph,
      records: List[EvidenceRecord],
      includeContextLayers: Boolean
  ): RelativeCauseProofSection =
    val proofRecords = proofSectionRecords(graph, records)
    RelativeCauseProofSection(
      role = role,
      strength = strength,
      boardAnchors = proofRecords.flatMap {
        case EvidenceRecord(ref, payload: BoardFactEvidence, _) =>
          payload.proofSignalAnchorKinds.map(kind => BoardAnchorProof(ref, kind))
        case _ =>
          Nil
      }.distinct,
      lineEvents = proofRecords.flatMap {
        case EvidenceRecord(ref, payload: LineFactEvidence, _) =>
          payload.lineEventKinds.map(kind => LineEventProof(ref, kind))
        case _ =>
          Nil
      }.distinct,
      lineConsequences = proofRecords.flatMap {
        case EvidenceRecord(ref, payload: LineFactEvidence, _) =>
          lineConsequenceProofKindsForCause(kind, payload).map(kind => LineConsequenceProof(ref, kind))
        case _ =>
          Nil
      }.distinct,
      relationProofs = proofRecords.collect { case EvidenceRecord(ref, payload: RelationFactEvidence, _) if payload.hasConcreteRelationProof =>
        RelationCauseProof(
          source = ref,
          kind = payload.kind,
          proof = payload.witnessProof
        )
      }.distinct,
      tacticalMechanisms = proofRecords.collect {
        case EvidenceRecord(ref, payload: TacticalMechanismEvidence, _) if payload.hasConcreteProof =>
          TacticalMechanismProof(
            source = ref,
            kind = payload.kind,
            signals = payload.signals
          )
      }.distinct,
      threatEpisodes = proofRecords.collect {
        case EvidenceRecord(ref, payload: ThreatEpisodeEvidence, _) if payload.isProofSignalDefensivePressure =>
          ThreatEpisodeCauseProof(
            source = ref,
            driver = payload.episode.driver,
            kind = payload.episode.kind,
            severity = payload.episode.severity
          )
      }.distinct,
      transitionConsequences = proofRecords.flatMap {
        case EvidenceRecord(ref, payload: StructuralDeltaEvidence, _) =>
          structuralConsequencesForCause(kind, payload).map(consequence =>
            TransitionConsequenceProof(
              source = ref,
              transition = payload.transition,
              consequence = consequence
            )
          )
        case _ =>
          Nil
      }.distinct,
      contextLayers = if includeContextLayers then proofRecords.map(_.ref.layer).distinct else Nil
    )

  private def lineConsequenceProofKindsForCause(
      kind: RelativeCauseKind,
      payload: LineFactEvidence
  ): List[LineConsequenceKind] =
    val materialOutcomeKinds =
      if lineMaterialCanProveRelativeCause(kind) then payload.materialOutcomeConsequenceKinds else Nil
    (payload.proofSignalConsequenceKinds ++ materialOutcomeKinds).distinct

  private def lineMaterialCanProveRelativeCause(kind: RelativeCauseKind): Boolean =
    kind match
      case RelativeCauseKind.MissedTacticalResource | RelativeCauseKind.TacticalRefutationOfPlayed |
          RelativeCauseKind.CandidateTacticalLiability | RelativeCauseKind.WrongRecapturer |
          RelativeCauseKind.RecaptureRecoveryWindow | RelativeCauseKind.ConversionMiss |
          RelativeCauseKind.ConversionSecured | RelativeCauseKind.MaterialSwing |
          RelativeCauseKind.SacrificeCompensation =>
        true
      case _ =>
        false

  private def proofSectionRecords(graph: TypedEvidenceGraph, records: List[EvidenceRecord]): List[EvidenceRecord] =
    val mechanismParents =
      records.collect {
        case record @ EvidenceRecord(_, payload: TacticalMechanismEvidence, _) if payload.hasConcreteProof =>
          parentClosure(graph, record).filter(record => proofSourceLayer(record.ref.layer))
      }.flatten
    (records ++ mechanismParents).distinctBy(_.ref.id)

  private def proofSectionRecordIds(graph: TypedEvidenceGraph, records: List[EvidenceRecord]): Set[String] =
    proofSectionRecords(graph, records).map(_.ref.id).toSet

  private def supportClosureRecordIds(graph: TypedEvidenceGraph, records: List[EvidenceRecord]): Set[String] =
    records
      .flatMap(record => record :: parentClosure(graph, record))
      .map(_.ref.id)
      .toSet

  private def contextSupportRecord(
      fact: CandidateComparisonFact,
      record: EvidenceRecord
  ): Boolean =
    val referencesComparedLine =
      record.referencesLine(fact.referenceLine) || record.referencesLine(fact.candidateLine)
    !referencesComparedLine &&
      (
        record.ref.scope == EvidenceScope.BeforePosition ||
          record.ref.scope == EvidenceScope.CurrentPosition ||
          record.ref.line.nonEmpty
      )

  private def proofSourceLayer(layer: EvidenceLayer): Boolean =
    layer match
      case EvidenceLayer.Board | EvidenceLayer.Line | EvidenceLayer.Relation | EvidenceLayer.ThreatPressure |
          EvidenceLayer.StructuralDelta =>
        true
      case _ =>
        false

  private def parentClosure(graph: TypedEvidenceGraph, record: EvidenceRecord): List[EvidenceRecord] =
    def loop(refs: List[EvidenceRef], seen: Set[String]): List[EvidenceRecord] =
      refs.flatMap { ref =>
        if seen.contains(ref.id) then Nil
        else
          graph.byId.get(ref.id).toList.flatMap { parent =>
            parent :: loop(parent.parents, seen + ref.id)
          }
      }
    loop(record.parents, Set.empty).distinctBy(_.ref.id)

  private def structuralConsequencesForCause(
      kind: RelativeCauseKind,
      payload: StructuralDeltaEvidence
  ): List[TransitionConsequence] =
    import TransitionConsequenceKind.*
    kind match
      case RelativeCauseKind.TargetPressureGain =>
        payload.consequencesOf(TargetPressureGain)
      case RelativeCauseKind.CenterControlGain =>
        payload.consequencesOf(CenterControlGain)
      case RelativeCauseKind.KingSafetyConcession =>
        payload.consequencesOf(KingSafetyConcession) ++
          payload.consequencesOf(KingRingPressureConcession)
      case RelativeCauseKind.PawnWeaknessTarget =>
        payload.consequencesOf(WeakPawnTargetCreated)
      case RelativeCauseKind.ActivityLoss =>
        payload.consequencesOf(DevelopmentLagIncreased) ++
          payload.consequencesOf(DevelopmentPieceRetreated) ++
          payload.consequencesOf(DevelopmentMobilityLoss) ++
          payload.consequencesOf(DevelopmentCenterControlLoss) ++
          payload.consequencesOf(DevelopmentUnsafePlacement) ++
          payload.consequencesOf(MobilityLoss) ++
          payload.consequencesOf(FileAccessLoss)
      case RelativeCauseKind.StructuralImprovement | RelativeCauseKind.MissedStrategicImprovement |
          RelativeCauseKind.PlanImprovement | RelativeCauseKind.PlanContradiction =>
        payload.positiveConsequences.filter(consequence =>
          StructuralDeltaEvidence.isStructuralAnchorConsequence(consequence.kind)
        )
      case RelativeCauseKind.ConversionMiss | RelativeCauseKind.ConversionSecured =>
        payload.positiveConsequences.filter(consequence =>
          consequence.kind == PromotionPressureGain ||
            consequence.kind == PassedPawnProgress
        )
      case RelativeCauseKind.StrategicConcession =>
        payload.negativeConsequences.filter(consequence =>
          StructuralDeltaEvidence.isStrategicSupportConsequence(consequence.kind)
        )
      case _ =>
        Nil

  private def mergeCauseCandidates(candidates: List[RelativeCauseDraft]): List[RelativeCauseDraft] =
    candidates
      .groupBy(candidate => (candidate.kind, candidate.sourceSide, supportSignature(candidate.support)))
      .toList
      .sortBy { case ((kind, sourceSide, signature), _) => (kind.toString, sourceSide.map(_.toString).getOrElse("none"), signature) }
      .map { case ((kind, sourceSide, _), grouped) =>
        val first = grouped.headOption
        RelativeCauseDraft(kind, first.map(_.support).getOrElse(Nil), sourceSide)
      }

  private def supportSignature(records: List[EvidenceRecord]): String =
    records.map(_.ref.id).distinct.sorted.mkString("|")

  private def comparisonProofRecords(neighborhood: ComparisonEvidenceNeighborhood): List[EvidenceRecord] =
    (neighborhood.referenceEndpoint ++ neighborhood.candidateEndpoint)
      .filter(record => record.ref.layer == EvidenceLayer.Line || record.ref.layer == EvidenceLayer.Eval)
      .distinctBy(_.ref.id)

  private def inferCauseCandidates(
      neighborhood: ComparisonEvidenceNeighborhood,
      fact: CandidateComparisonFact
  ): List[RelativeCauseDraft] =
    val profile =
      RelativeCauseSignalProfile.from(
        fact = fact,
        referenceRecords = neighborhood.referenceRecords,
        candidateRecords = neighborhood.candidateRecords,
        sharedRecords = neighborhood.sharedRecords
      )
    RelativeCauseDraftPlanner.drafts(profile)

  private def parentsForLine(
      context: JudgmentAssemblyContext,
      line: CandidateLineNode
  ): List[EvidenceRef] =
    context.evidenceGraph.recordsFor(line.ref).map(_.ref)

  private def comparisonNeighborhood(
      context: JudgmentAssemblyContext,
      root: PositionNodeRef,
      fact: CandidateComparisonFact
  ): ComparisonEvidenceNeighborhood =
    val referenceTransition = transitionForLine(context, fact.referenceLine)
    val candidateTransition = transitionForLine(context, fact.candidateLine)
    val baseRecords =
      (
        recordsForLineEndpoint(context, fact.referenceLine) ++
          recordsForLineEndpoint(context, fact.candidateLine) ++
          recordsForRootContext(context, root) ++
          recordsForTransition(context, referenceTransition) ++
          recordsForTransition(context, candidateTransition) ++
          recordsForAfterPosition(context, referenceTransition) ++
          recordsForAfterPosition(context, candidateTransition)
      ).distinctBy(_.ref.id)
    val parentRecords =
      baseRecords.flatMap(record => record.parents.flatMap(parent => context.evidenceGraph.byId.get(parent.id)))
    ComparisonEvidenceNeighborhood(
      referenceEndpoint = recordsForLineEndpoint(context, fact.referenceLine),
      candidateEndpoint = recordsForLineEndpoint(context, fact.candidateLine),
      rootContext = recordsForRootContext(context, root),
      referenceTransition = recordsForTransition(context, referenceTransition),
      candidateTransition = recordsForTransition(context, candidateTransition),
      referenceAfterPosition = recordsForAfterPosition(context, referenceTransition),
      candidateAfterPosition = recordsForAfterPosition(context, candidateTransition),
      parents = parentRecords.distinctBy(_.ref.id)
    )

  private def recordsForLineEndpoint(
      context: JudgmentAssemblyContext,
      line: LineNodeRef
  ): List[EvidenceRecord] =
    context.evidenceGraph.records.filter(record =>
      endpointLayer(record.ref.layer) &&
        record.referencesLine(line)
    )

  private def recordsForRootContext(
      context: JudgmentAssemblyContext,
      root: PositionNodeRef
  ): List[EvidenceRecord] =
    context.evidenceGraph.records.filter(record =>
      rootContextLayer(record.ref.layer) &&
        record.ref.position == root &&
        (record.ref.scope == EvidenceScope.BeforePosition || record.ref.scope == EvidenceScope.CurrentPosition)
    )

  private def recordsForTransition(
      context: JudgmentAssemblyContext,
      transition: Option[MoveTransitionEdge]
  ): List[EvidenceRecord] =
    transition.toList.flatMap { edge =>
      context.evidenceGraph.records.filter(record =>
        transitionLayer(record.ref.layer) &&
          record.ref.position == edge.from &&
          recordMatchesTransition(record, edge)
      )
    }

  private def recordsForAfterPosition(
      context: JudgmentAssemblyContext,
      transition: Option[MoveTransitionEdge]
  ): List[EvidenceRecord] =
    transition.toList.flatMap(edge =>
      context.evidenceGraph.records.filter(record =>
        afterPositionLayer(record.ref.layer) &&
          record.ref.position == edge.to
      )
    )

  private def transitionForLine(
      context: JudgmentAssemblyContext,
      line: LineNodeRef
  ): Option[MoveTransitionEdge] =
    context.transitions.find(edge =>
      edge.moveUci == line.rootMove &&
        line.role == edge.role.lineRole
    )

  private def recordMatchesTransition(record: EvidenceRecord, edge: MoveTransitionEdge): Boolean =
    record.payload match
      case MoveTransitionEvidence(moveUci, from, to) =>
        record.ref.scope == edge.role.scope &&
          moveUci == edge.moveUci &&
          from == edge.from &&
          to == edge.to
      case payload: StructuralDeltaEvidence =>
        payload.role == edge.role &&
          payload.moveUci == edge.moveUci &&
          payload.from == edge.from &&
          payload.to == edge.to
      case _: PlanTransitionEvidence =>
        record.ref.line.exists(line => line.rootMove == edge.moveUci && line.role == edge.role.lineRole) ||
          record.parents.exists(_.id == edge.evidence.id)
      case _ =>
        false

  private def endpointLayer(layer: EvidenceLayer): Boolean =
    layer match
      case EvidenceLayer.Line | EvidenceLayer.Eval | EvidenceLayer.MoveMotif | EvidenceLayer.Relation |
          EvidenceLayer.TacticalMechanism =>
        true
      case _ =>
        false

  private def rootContextLayer(layer: EvidenceLayer): Boolean =
    layer match
      case EvidenceLayer.Board | EvidenceLayer.SinglePosition | EvidenceLayer.PawnStructure |
          EvidenceLayer.ThreatPressure | EvidenceLayer.Strategic | EvidenceLayer.OpeningContext |
          EvidenceLayer.FeatureAnchor | EvidenceLayer.ApplicabilityAssessment | EvidenceLayer.PlanPressure |
          EvidenceLayer.PlanTransition =>
        true
      case _ =>
        false

  private def transitionLayer(layer: EvidenceLayer): Boolean =
    layer match
      case EvidenceLayer.MoveTransition | EvidenceLayer.StructuralDelta | EvidenceLayer.PlanTransition =>
        true
      case _ =>
        false

  private def afterPositionLayer(layer: EvidenceLayer): Boolean =
    layer match
      case EvidenceLayer.Board | EvidenceLayer.SinglePosition | EvidenceLayer.PawnStructure |
          EvidenceLayer.Strategic | EvidenceLayer.ThreatPressure =>
        true
      case _ =>
        false
