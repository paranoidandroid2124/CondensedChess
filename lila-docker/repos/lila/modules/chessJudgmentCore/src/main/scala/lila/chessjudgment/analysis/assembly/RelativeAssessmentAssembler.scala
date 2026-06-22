package lila.chessjudgment.analysis.assembly

import chess.Color
import lila.chessjudgment.analysis.evaluation.{ JudgmentThresholds, PerspectiveMath, VerdictThresholdPolicy }
import lila.chessjudgment.analysis.policy.ClaimTruthPolicy
import lila.chessjudgment.analysis.singlePosition.PawnPlayDriver
import lila.chessjudgment.analysis.transition.TransitionFactNormalizer
import lila.chessjudgment.model.{ Motif, TransitionType }
import lila.chessjudgment.model.structure.AlignmentBand
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

  private final case class RelativeCauseCandidate(
      kind: RelativeCauseKind,
      support: List[EvidenceRecord]
  )

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
          val role = RelativeCauseRole.fromComparisonKind(fact.kind)
          val supportRefs = support.map(_.ref).distinctBy(_.id)
          val sourceSide =
            RelativeCauseSourceSide
              .fromSupportEvidence(kind, fact.referenceLine, fact.candidateLine, supportRefs)
              .getOrElse(RelativeCauseSourceSide.Shared)
          val eventLine =
            RelativeCauseSourceSide.eventLine(sourceSide, role, fact.referenceLine, fact.candidateLine)
          val importance = RelativeCauseImportance.from(role, sourceSide, kind)
          val evidenceLines = evidenceLinesForCause(fact, support, sourceSide)
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
              evidenceLines = evidenceLines,
              role = role,
              eventLine = eventLine,
              sourceSide = sourceSide,
              importance = importance
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
        .filterNot(record => proofIds.contains(record.ref.id))
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
          payload.proofSignalConsequenceKinds.map(kind => LineConsequenceProof(ref, kind))
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

  private def proofSectionRecords(graph: TypedEvidenceGraph, records: List[EvidenceRecord]): List[EvidenceRecord] =
    val mechanismParents =
      records.collect {
        case record @ EvidenceRecord(_, payload: TacticalMechanismEvidence, _) if payload.hasConcreteProof =>
          parentClosure(graph, record).filter(record => proofSourceLayer(record.ref.layer))
      }.flatten
    (records ++ mechanismParents).distinctBy(_.ref.id)

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

  private def mergeCauseCandidates(candidates: List[RelativeCauseCandidate]): List[RelativeCauseCandidate] =
    candidates
      .groupBy(candidate => (candidate.kind, supportSignature(candidate.support)))
      .toList
      .sortBy { case ((kind, signature), _) => (kind.toString, signature) }
      .map { case ((kind, _), grouped) =>
        RelativeCauseCandidate(kind, grouped.headOption.map(_.support).getOrElse(Nil))
      }

  private def supportSignature(records: List[EvidenceRecord]): String =
    records.map(_.ref.id).distinct.sorted.mkString("|")

  private def causeCandidate(
      kind: RelativeCauseKind,
      support: List[EvidenceRecord],
      condition: Boolean
  ): Option[RelativeCauseCandidate] =
    Option.when(condition)(RelativeCauseCandidate(kind, support.distinctBy(_.ref.id)))

  private def comparisonProofRecords(neighborhood: ComparisonEvidenceNeighborhood): List[EvidenceRecord] =
    (neighborhood.referenceEndpoint ++ neighborhood.candidateEndpoint)
      .filter(record => record.ref.layer == EvidenceLayer.Line || record.ref.layer == EvidenceLayer.Eval)
      .distinctBy(_.ref.id)

  private def evidenceLinesForCause(
      fact: CandidateComparisonFact,
      support: List[EvidenceRecord],
      sourceSide: RelativeCauseSourceSide
  ): List[LineNodeRef] =
    val supportLines =
      support.flatMap(_.ref.line).filter(line => line == fact.referenceLine || line == fact.candidateLine)
    val sideLines =
      sourceSide match
        case RelativeCauseSourceSide.Reference =>
          List(fact.referenceLine)
        case RelativeCauseSourceSide.Candidate =>
          List(fact.candidateLine)
        case RelativeCauseSourceSide.Mixed | RelativeCauseSourceSide.Shared =>
          List(fact.referenceLine, fact.candidateLine)
    (supportLines ++ sideLines).distinct

  private def inferCauseCandidates(
      neighborhood: ComparisonEvidenceNeighborhood,
      fact: CandidateComparisonFact
  ): List[RelativeCauseCandidate] =
    val referenceRecords = neighborhood.referenceRecords
    val candidateRecords = neighborhood.candidateRecords
    val comparison = fact.comparison
    val badLoss = comparison.winPercentLossForMover >= JudgmentThresholds.INACCURACY_WP
    val tacticalLoss = comparison.winPercentLossForMover >= JudgmentThresholds.SIGNIFICANT_THREAT_WP
    val majorLoss = comparison.winPercentLossForMover >= JudgmentThresholds.MATERIAL_THREAT_WP
    val candidateBetter =
      comparison.candidateWinPercentDeltaForMover >= JudgmentThresholds.PLAYABLE_LOSS_WP
    val primaryPlayedSignificantLoss =
      fact.kind == CandidateComparisonKind.PlayedVsBest && tacticalLoss
    val primaryPlayedPositive =
      fact.kind == CandidateComparisonKind.PlayedVsBest && candidateBetter
    val referenceOnlyDefense = onlyDefenseRecords(referenceRecords)
    val candidateOnlyDefense = onlyDefenseRecords(candidateRecords)
    val referenceTacticalRisk = tacticalRiskRecords(referenceRecords)
    val referenceConversionWindow = conversionWindowRecords(referenceRecords)
    val candidateConversionWindow = conversionWindowRecords(candidateRecords)
    val referenceRecaptureResource = recaptureResourceRecords(referenceRecords)
    val candidateRecaptureResource = recaptureResourceRecords(candidateRecords)
    val referenceMoveOrderResource = moveOrderResourceRecords(referenceRecords)
    val candidateMoveOrderResource = moveOrderResourceRecords(candidateRecords)
    val referenceDefensiveResource = defensiveResourceRecords(referenceRecords)
    val candidateDefensiveResource = defensiveResourceRecords(candidateRecords)
    val referenceLooseMaterialExploit = looseMaterialExploitRecords(referenceRecords, neighborhood.sharedRecords)
    val candidateLooseMaterialLiability = looseMaterialLiabilityRecords(candidateRecords, neighborhood.sharedRecords)
    val referencePromotionResource = promotionRaceRecords(referenceRecords)
    val candidatePromotionResource = promotionRaceRecords(candidateRecords)
    val referencePassedPawnResource = passedPawnResourceRecords(referenceRecords)
    val candidatePassedPawnResource = passedPawnResourceRecords(candidateRecords)
    val candidatePassedPawnConcession = passedPawnConcessionRecords(candidateRecords)
    val referenceEndgameResource = endgameResourceRecords(referenceRecords)
    val candidateEndgameResource = endgameResourceRecords(candidateRecords)
    val referenceStructuralTargetRelease = structuralTargetReleaseRecords(referenceRecords)
    val candidateStructuralImprovement = structuralImprovementRecords(candidateRecords)
    val candidatePawnStructureImprovement = pawnStructureImprovementRecords(candidateRecords)
    val candidateTargetPressureGain = targetPressureGainRecords(candidateRecords)
    val candidateCenterControlGain = centerControlGainRecords(candidateRecords)
    val candidatePlanCause = planCauseRecords(candidateRecords)
    val candidateStrategicConcession = strategicConcessionRecords(candidateRecords)
    val referenceTacticalMechanism = tacticalMechanismRecords(referenceRecords)
    val candidateTacticalMechanism = tacticalMechanismRecords(candidateRecords)
    val materialSwingSupport = materialSwingSupportRecords(referenceRecords, candidateRecords)
    val materialDeteriorationSupport = materialDeteriorationSupportRecords(referenceRecords, candidateRecords)
    val materialLossSupport = (materialSwingSupport ++ materialDeteriorationSupport).distinctBy(_.ref.id)
    val sacrificeCompensationSupport = sacrificeCompensationSupportRecords(candidateRecords)
    val structuralImprovementSupport =
      referenceStructuralTargetRelease ++ candidateStructuralImprovement ++ candidatePawnStructureImprovement
    val shortTermEvidenceCompetesWithStrategic =
        candidateConcreteTacticalBridgeRecords(candidateRecords).nonEmpty ||
        referenceTacticalRisk.nonEmpty ||
        materialLossSupport.nonEmpty ||
        referenceConversionWindow.nonEmpty ||
        candidateConversionWindow.nonEmpty
    val missedStrategicSupport =
      missedStrategicImprovementSupport(fact, referenceRecords, candidateRecords, neighborhood.sharedRecords)
    val rawCauses =
      List(
        causeCandidate(RelativeCauseKind.OnlyDefenseNecessity, referenceOnlyDefense, referenceOnlyDefense.nonEmpty && badLoss),
        causeCandidate(RelativeCauseKind.OnlyDefenseNecessity, candidateOnlyDefense, candidateOnlyDefense.nonEmpty && candidateBetter),
        causeCandidate(RelativeCauseKind.DefensiveResource, referenceDefensiveResource, referenceDefensiveResource.nonEmpty && badLoss),
        causeCandidate(RelativeCauseKind.DefensiveResource, candidateDefensiveResource, candidateDefensiveResource.nonEmpty && candidateBetter),
        causeCandidate(
          RelativeCauseKind.WrongRecapturer,
          referenceRecaptureResource,
          referenceRecaptureResource.nonEmpty && candidateRecaptureResource.isEmpty && badLoss
        ),
        causeCandidate(
          RelativeCauseKind.RecaptureRecoveryWindow,
          candidateRecaptureResource,
          candidateRecaptureResource.nonEmpty && candidateBetter
        ),
        causeCandidate(
          RelativeCauseKind.WrongMoveOrder,
          referenceMoveOrderResource,
          referenceMoveOrderResource.nonEmpty && candidateMoveOrderResource.isEmpty && badLoss
        ),
        causeCandidate(
          RelativeCauseKind.TempoLoss,
          candidateMoveOrderResource,
          candidateMoveOrderResource.nonEmpty && (badLoss || candidateBetter)
        ),
        causeCandidate(RelativeCauseKind.ConversionMiss, referenceConversionWindow, referenceConversionWindow.nonEmpty && badLoss),
        causeCandidate(
          RelativeCauseKind.ConversionSecured,
          candidateConversionWindow,
          candidateConversionWindow.nonEmpty && candidateBetter
        ),
        causeCandidate(RelativeCauseKind.ConversionMiss, referencePromotionResource, referencePromotionResource.nonEmpty && badLoss),
        causeCandidate(RelativeCauseKind.ConversionSecured, candidatePromotionResource, candidatePromotionResource.nonEmpty && candidateBetter),
        causeCandidate(RelativeCauseKind.MissedTacticalResource, referenceLooseMaterialExploit, referenceLooseMaterialExploit.nonEmpty && badLoss),
        causeCandidate(
          if playedMoveCandidateSideComparison(fact.kind) then RelativeCauseKind.TacticalRefutationOfPlayed
          else RelativeCauseKind.CandidateTacticalLiability,
          candidateLooseMaterialLiability,
          candidateLooseMaterialLiability.nonEmpty && badLoss
        ),
        causeCandidate(
          RelativeCauseKind.StructuralImprovement,
          structuralImprovementSupport ++ candidatePassedPawnResource ++ candidateEndgameResource,
          candidateBetter &&
            (structuralImprovementSupport.nonEmpty || candidatePassedPawnResource.nonEmpty || candidateEndgameResource.nonEmpty)
        ),
        causeCandidate(RelativeCauseKind.TargetPressureGain, candidateTargetPressureGain, primaryPlayedPositive && candidateTargetPressureGain.nonEmpty),
        causeCandidate(RelativeCauseKind.CenterControlGain, candidateCenterControlGain, primaryPlayedPositive && candidateCenterControlGain.nonEmpty),
        causeCandidate(RelativeCauseKind.PlanImprovement, candidatePlanCause, candidatePlanCause.nonEmpty && candidateBetter),
        causeCandidate(RelativeCauseKind.PlanContradiction, candidatePlanCause, candidatePlanCause.nonEmpty && badLoss),
        causeCandidate(
          RelativeCauseKind.StrategicConcession,
          candidateStrategicConcession ++ candidatePassedPawnConcession,
          (badLoss || primaryPlayedSignificantLoss) &&
            !shortTermEvidenceCompetesWithStrategic &&
            (candidateStrategicConcession.nonEmpty || candidatePassedPawnConcession.nonEmpty)
        ),
        causeCandidate(
          RelativeCauseKind.MissedStrategicImprovement,
          missedStrategicSupport ++ referencePassedPawnResource ++ referenceEndgameResource,
          badLoss && (missedStrategicSupport.nonEmpty || referencePassedPawnResource.nonEmpty || referenceEndgameResource.nonEmpty)
        ),
        causeCandidate(
          RelativeCauseKind.MaterialSwing,
          materialLossSupport,
          (majorLoss || primaryPlayedSignificantLoss) &&
            materialLossSupport.nonEmpty
        ),
        causeCandidate(
          RelativeCauseKind.SacrificeCompensation,
          sacrificeCompensationSupport,
          (primaryPlayedPositive || candidateBetter) &&
            sacrificeCompensationSupport.nonEmpty
        )
      ).flatten ++
        mechanismCauses(referenceTacticalMechanism, candidateTacticalMechanism, fact.kind, tacticalLoss, candidateBetter)
    suppressGenericCompanions(rawCauses)

  private def suppressGenericCompanions(
      causes: List[RelativeCauseCandidate]
  ): List[RelativeCauseCandidate] =
    val hasShortTermCause = causes.exists(candidate => shortTermCause(candidate.kind))
    val hasNonConcessionCause = causes.exists(candidate => candidate.kind != RelativeCauseKind.StrategicConcession)
    val hasSpecificMaterialCause = causes.exists(candidate => specificMaterialCause(candidate.kind))
    val hasSpecificStructuralCause = causes.exists(candidate => specificStructuralCause(candidate.kind))
    causes.filterNot {
      case RelativeCauseCandidate(RelativeCauseKind.StrategicConcession, _) =>
        hasShortTermCause || hasNonConcessionCause
      case RelativeCauseCandidate(RelativeCauseKind.MissedStrategicImprovement, _) =>
        hasShortTermCause
      case RelativeCauseCandidate(RelativeCauseKind.MaterialSwing, _) =>
        hasSpecificMaterialCause
      case RelativeCauseCandidate(RelativeCauseKind.StructuralImprovement, _) =>
        hasSpecificStructuralCause
      case candidate @ RelativeCauseCandidate(RelativeCauseKind.RecaptureRecoveryWindow, _) =>
        causes.exists(other =>
          other.kind == RelativeCauseKind.WrongRecapturer &&
            supportOverlaps(candidate.support, other.support)
        )
      case candidate @ RelativeCauseCandidate(RelativeCauseKind.TempoLoss, _) =>
        causes.exists(other =>
          other.kind == RelativeCauseKind.WrongMoveOrder &&
            supportOverlaps(candidate.support, other.support)
        )
      case _ =>
        false
    }

  private def supportOverlaps(left: List[EvidenceRecord], right: List[EvidenceRecord]): Boolean =
    val leftIds = left.map(_.ref.id).toSet
    leftIds.nonEmpty && right.exists(record => leftIds.contains(record.ref.id))

  private def specificMaterialCause(kind: RelativeCauseKind): Boolean =
    kind match
      case RelativeCauseKind.WrongRecapturer | RelativeCauseKind.RecaptureRecoveryWindow |
          RelativeCauseKind.ConversionMiss | RelativeCauseKind.ConversionSecured |
          RelativeCauseKind.MissedTacticalResource | RelativeCauseKind.TacticalRefutationOfPlayed |
          RelativeCauseKind.CandidateTacticalLiability =>
        true
      case _ =>
        false

  private def specificStructuralCause(kind: RelativeCauseKind): Boolean =
    kind match
      case RelativeCauseKind.TargetPressureGain | RelativeCauseKind.CenterControlGain |
          RelativeCauseKind.PlanImprovement | RelativeCauseKind.PlanContradiction |
          RelativeCauseKind.MissedStrategicImprovement | RelativeCauseKind.StrategicConcession |
          RelativeCauseKind.ConversionMiss | RelativeCauseKind.ConversionSecured =>
        true
      case _ =>
        false

  private def shortTermCause(kind: RelativeCauseKind): Boolean =
    kind != RelativeCauseKind.DrawResource && ClaimEventCluster.kindForCause(kind).nonEmpty

  private def mechanismCauses(
      referenceRecords: List[EvidenceRecord],
      candidateRecords: List[EvidenceRecord],
      kind: CandidateComparisonKind,
      tacticalLoss: Boolean,
      candidateBetter: Boolean
  ): List[RelativeCauseCandidate] =
    val referenceCauses =
      Option.when(tacticalLoss)(mechanismCauseKinds(referenceRecords, badLoss = false)).getOrElse(Nil)
    val candidateBadCauses =
      Option
        .when(tacticalLoss)(
          mechanismCauseKinds(candidateRecords, badLoss = true, playedCandidate = playedMoveCandidateSideComparison(kind))
        )
        .getOrElse(Nil)
    val candidateBetterCauses =
      Option.when(candidateBetter)(mechanismCauseKinds(candidateRecords, badLoss = false)).getOrElse(Nil)
    referenceCauses ++ candidateBadCauses ++ candidateBetterCauses

  private def mechanismCauseKinds(
      records: List[EvidenceRecord],
      badLoss: Boolean,
      playedCandidate: Boolean = false
  ): List[RelativeCauseCandidate] =
    records.collect {
      case record @ EvidenceRecord(_, payload: TacticalMechanismEvidence, _) if payload.canAnchorTacticalIdea =>
        RelativeCauseCandidate(
          TacticalMechanismKind.relativeCauseKind(payload.kind, badLoss, playedCandidate),
          List(record)
        )
    }

  private def playedMoveCandidateSideComparison(kind: CandidateComparisonKind): Boolean =
    kind == CandidateComparisonKind.PlayedVsBest || kind == CandidateComparisonKind.PlayedVsAlternative

  private def tacticalMechanismRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, payload: TacticalMechanismEvidence, _) =>
        payload.canAnchorTacticalIdea || payload.canAnchorDefensiveIdea
      case _ =>
        false
    }

  private def tacticalRiskRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, payload: TacticalMechanismEvidence, _) =>
        payload.canAnchorTacticalIdea
      case _ =>
        false
    }

  private def onlyDefenseRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, payload: ThreatEpisodeEvidence, _) =>
        payload.onlyDefense.nonEmpty && payload.isProofSignalDefensivePressure
      case _ => false
    }

  private def defensiveResourceRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, payload: ThreatEpisodeEvidence, _) =>
        !payload.insufficientData &&
          (payload.defenseRequired ||
            payload.prophylaxisNeeded ||
            payload.maxWinPercentLossIfIgnored.exists(_ >= JudgmentThresholds.SIGNIFICANT_THREAT_WP))
      case EvidenceRecord(_, payload: TacticalMechanismEvidence, _) =>
        payload.canAnchorDefensiveIdea
      case _ =>
        false
    }.distinctBy(_.ref.id)

  private def recaptureResourceRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, payload: LineFactEvidence, _) =>
        payload.hasRecaptureRecoveryConsequence ||
          payload.hasMaterialRecaptureChain ||
          payload.hasMaterialRecoveryWindow
      case EvidenceRecord(_, payload: TacticalMechanismEvidence, _) =>
        payload.kind == TacticalMechanismKind.RecaptureChoice && payload.canAnchorTacticalIdea
      case _ =>
        false
    }.distinctBy(_.ref.id)

  private def moveOrderResourceRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, payload: MoveMotifEvidence, _) =>
        payload.motif match
          case _: Motif.Zwischenzug => true
          case _                    => false
      case EvidenceRecord(_, payload: RelationFactEvidence, _) =>
        payload.kind == RelationFactKind.Zwischenzug && payload.hasConcreteRelationProof
      case EvidenceRecord(_, payload: LineFactEvidence, _) =>
        payload.hasProofSignalConsequence(LineConsequenceKind.ImmediateReplyCheck)
      case EvidenceRecord(_, payload: TacticalMechanismEvidence, _) =>
        payload.kind == TacticalMechanismKind.Tempo && payload.canAnchorTacticalIdea
      case _ =>
        false
    }.distinctBy(_.ref.id)

  private def conversionWindowRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, SinglePositionEvidence(assessment), _) =>
        assessment.simplifyBias.shouldSimplify
      case EvidenceRecord(_, payload: RelationFactEvidence, _) if payload.kind == RelationFactKind.BadPieceLiquidation =>
        true
      case _ => false
    }

  private def promotionRaceRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, payload: LineFactEvidence, _) =>
        payload.hasProofSignalConsequence(LineConsequenceKind.PromotionRace) ||
          payload.materialOutcomeProfile.gainSignals.contains(LineMaterialOutcomeSignal.PromotionGain) ||
          payload.materialOutcomeProfile.lossSignals.contains(LineMaterialOutcomeSignal.PromotionLoss)
      case EvidenceRecord(_, payload: TacticalMechanismEvidence, _) =>
        payload.kind == TacticalMechanismKind.PawnPromotion && payload.canAnchorTacticalIdea
      case EvidenceRecord(_, payload: StructuralDeltaEvidence, _) =>
        payload.hasConsequence(TransitionConsequenceKind.PromotionPressureGain)
      case _ =>
        false
    }.distinctBy(_.ref.id)

  private def looseMaterialExploitRecords(
      records: List[EvidenceRecord],
      sharedRecords: List[EvidenceRecord]
  ): List[EvidenceRecord] =
    val material = materialGainRecords(records)
    val relation = looseMaterialRelationRecords(records)
    Option
      .when((material.nonEmpty || relation.nonEmpty) && looseMaterialContextPresent(records ++ sharedRecords))(
        material ++ relation
      )
      .getOrElse(Nil)
      .distinctBy(_.ref.id)

  private def looseMaterialLiabilityRecords(
      records: List[EvidenceRecord],
      sharedRecords: List[EvidenceRecord]
  ): List[EvidenceRecord] =
    val material = materialLossRecords(records)
    val relation = looseMaterialRelationRecords(records)
    Option
      .when((material.nonEmpty || relation.nonEmpty) && looseMaterialContextPresent(records ++ sharedRecords))(
        material ++ relation
      )
      .getOrElse(Nil)
      .distinctBy(_.ref.id)

  private def looseMaterialContextPresent(records: List[EvidenceRecord]): Boolean =
    records.exists {
      case EvidenceRecord(_, payload: BoardFactEvidence, _) =>
        payload.looseMaterialAnchors.nonEmpty
      case EvidenceRecord(_, payload: RelationFactEvidence, _) =>
        looseMaterialRelation(payload)
      case _ =>
        false
    }

  private def looseMaterialRelationRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, payload: RelationFactEvidence, _) =>
        looseMaterialRelation(payload)
      case _ =>
        false
    }

  private def looseMaterialRelation(payload: RelationFactEvidence): Boolean =
    payload.hasConcreteRelationProof &&
      (
        payload.kind == RelationFactKind.HangingPiece ||
          payload.kind == RelationFactKind.TrappedPiece ||
          payload.kind == RelationFactKind.Domination
      )

  private def materialGainRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, payload: LineFactEvidence, _) =>
        payload.materialOutcomeProfile.gainMagnitude != LineMaterialOutcomeMagnitude.None ||
          payload.hasProofSignalConsequence(LineConsequenceKind.MaterialGain)
      case EvidenceRecord(_, payload: TacticalMechanismEvidence, _) =>
        payload.kind == TacticalMechanismKind.MaterialGain && payload.canAnchorTacticalIdea
      case _ =>
        false
    }.distinctBy(_.ref.id)

  private def materialLossRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, payload: LineFactEvidence, _) =>
        payload.materialOutcomeProfile.lossMagnitude != LineMaterialOutcomeMagnitude.None ||
          payload.hasProofSignalConsequence(LineConsequenceKind.MaterialLoss)
      case EvidenceRecord(_, payload: TacticalMechanismEvidence, _) =>
        payload.kind == TacticalMechanismKind.MaterialGain && payload.canAnchorTacticalIdea
      case _ =>
        false
    }.distinctBy(_.ref.id)

  private def passedPawnResourceRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, payload: StructuralDeltaEvidence, _) =>
        payload.hasPassedPawnProgress ||
          payload.hasConsequence(TransitionConsequenceKind.PromotionPressureGain)
      case EvidenceRecord(_, payload: MoveMotifEvidence, _) =>
        payload.motif match
          case _: Motif.PassedPawnPush | _: Motif.PassedPawn | _: Motif.PawnPromotion => true
          case _                                                                      => false
      case _ =>
        false
    }.distinctBy(_.ref.id)

  private def passedPawnConcessionRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, payload: StructuralDeltaEvidence, _) =>
        payload.hasConsequence(TransitionConsequenceKind.PassedPawnConcession) ||
          payload.hasConsequence(TransitionConsequenceKind.PromotionPressureConcession)
      case _ =>
        false
    }.distinctBy(_.ref.id)

  private def endgameResourceRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, payload: BoardFactEvidence, _) =>
        payload.endgameTechniqueAnchors.nonEmpty
      case EvidenceRecord(_, payload @ StrategicFactEvidence(StrategicFactKind.Endgame, _, _, confidence), _) =>
        confidence >= 0.35 && payload.hasTypedSupport
      case EvidenceRecord(_, SinglePositionEvidence(assessment), _) =>
        assessment.gamePhase.isEndgame && assessment.simplifyBias.shouldSimplify
      case _ =>
        false
    }.distinctBy(_.ref.id)

  private def structuralTargetReleaseRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, payload: StructuralDeltaEvidence, _) =>
        payload.hasTargetPressureRelease
      case _ =>
        false
    }

  private def structuralImprovementRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, payload: StructuralDeltaEvidence, _) =>
        payload.hasStructuralAnchor
      case _ =>
        false
    }

  private def targetPressureGainRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, payload: StructuralDeltaEvidence, _) =>
        payload.hasTargetPressureGain
      case _ =>
        false
    }

  private def centerControlGainRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, payload: StructuralDeltaEvidence, _) =>
        payload.hasCenterControlGain
      case _ =>
        false
    }

  private def pawnStructureImprovementRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, payload: PawnStructureFactEvidence, _) =>
        ClaimTruthPolicy.pawnStructureCanAnchorPlan(payload)
      case _ =>
        false
    }

  private def planCauseRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, PlanTransitionEvidence(transition), _) =>
        transition.primaryPlanId.nonEmpty &&
          transition.transitionType != TransitionType.Opening &&
          transition.momentum >= 0.55
      case EvidenceRecord(_, PlanPressureEvidence(scoring, activePlans), _) =>
        ClaimTruthPolicy.planPressureHasDirectEvidence(scoring, activePlans)
      case _ =>
        false
    }

  private def strategicConcessionRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, payload: StructuralDeltaEvidence, _) =>
        payload.hasStrategicConcession
      case _ =>
        false
    }

  private def strategicImprovementSupport(records: List[EvidenceRecord]): List[EvidenceRecord] =
    (
      structuralImprovementRecords(records) ++
        pawnStructureImprovementRecords(records) ++
        planCauseRecords(records)
    ).distinctBy(_.ref.id)

  private def missedStrategicImprovementSupport(
      fact: CandidateComparisonFact,
      referenceRecords: List[EvidenceRecord],
      candidateRecords: List[EvidenceRecord],
      sharedRecords: List[EvidenceRecord]
  ): List[EvidenceRecord] =
    val scoreBased =
      Option
        .when(referenceStrategicImprovementOutperformsCandidate(referenceRecords, candidateRecords))(
          strategicImprovementSupport(referenceRecords)
        )
        .getOrElse(Nil)
    val axisBased =
      List(
        StructuralDeltaEvidence.unmatchedStructuralImprovementAxisRecords(referenceRecords, candidateRecords),
        samePieceDevelopmentChoiceSupport(fact, referenceRecords, candidateRecords, sharedRecords),
        unmatchedAxisSupport(pawnStructureImprovementRecords(referenceRecords), pawnStructureImprovementRecords(candidateRecords)),
        unmatchedAxisSupport(planCauseRecords(referenceRecords), planCauseRecords(candidateRecords))
      ).flatten
    (scoreBased ++ axisBased).distinctBy(_.ref.id)

  private def unmatchedAxisSupport(
      referenceSupport: List[EvidenceRecord],
      candidateSupport: List[EvidenceRecord]
  ): List[EvidenceRecord] =
    Option.when(referenceSupport.nonEmpty && candidateSupport.isEmpty)(referenceSupport).getOrElse(Nil)

  private def samePieceDevelopmentChoiceSupport(
      fact: CandidateComparisonFact,
      referenceRecords: List[EvidenceRecord],
      candidateRecords: List[EvidenceRecord],
      sharedRecords: List[EvidenceRecord]
  ): List[EvidenceRecord] =
    val referenceSupport = developmentChoiceRecords(referenceRecords)
    val candidateSupport = developmentChoiceRecords(candidateRecords)
    val functionProof = referenceOnlyDefenseFunctionRecords(fact, sharedRecords)
    val samePieceChoice =
      referenceSupport.exists(referenceRecord =>
        candidateSupport.exists(candidateRecord => sameDevelopmentPieceChoice(referenceRecord, candidateRecord))
      )
    Option
      .when(
        fact.kind == CandidateComparisonKind.PlayedVsBest &&
          fact.comparison.winPercentLossForMover >= JudgmentThresholds.BLUNDER_WP &&
          samePieceChoice &&
          functionProof.nonEmpty
      )(referenceSupport ++ candidateSupport ++ functionProof)
      .getOrElse(Nil)
      .distinctBy(_.ref.id)

  private def referenceOnlyDefenseFunctionRecords(
      fact: CandidateComparisonFact,
      records: List[EvidenceRecord]
  ): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, payload: ThreatEpisodeEvidence, _) =>
        !payload.insufficientData &&
          payload.onlyDefense.exists(move => normalizeMove(move) == normalizeMove(fact.referenceLine.rootMove)) &&
          (payload.defenseRequired ||
            payload.maxWinPercentLossIfIgnored.exists(_ >= JudgmentThresholds.MATERIAL_THREAT_WP))
      case _ =>
        false
    }

  private def developmentChoiceRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, payload: StructuralDeltaEvidence, _) =>
        payload.hasDevelopmentActivation &&
          payload.developmentChoices.nonEmpty
      case _ =>
        false
    }

  private def sameDevelopmentPieceChoice(left: EvidenceRecord, right: EvidenceRecord): Boolean =
    developmentChoices(left).exists(leftMove =>
      developmentChoices(right).exists(rightMove =>
        leftMove.role == rightMove.role &&
          leftMove.from == rightMove.from &&
          leftMove.to != rightMove.to
      )
    )

  private def developmentChoices(record: EvidenceRecord) =
    record.payload match
      case payload: StructuralDeltaEvidence =>
        payload.developmentChoices
      case _ =>
        Nil

  private def candidateConcreteTacticalBridgeRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, payload: TacticalMechanismEvidence, _) =>
        payload.canAnchorTacticalIdea
      case _ =>
        false
    }.distinctBy(_.ref.id)

  private def materialSwingSupportRecords(
      referenceRecords: List[EvidenceRecord],
      candidateRecords: List[EvidenceRecord]
  ): List[EvidenceRecord] =
    Option
      .when(typedMaterialConsequenceSwing(referenceRecords, candidateRecords))(
        (proofSignalMaterialSummaryRecords(referenceRecords) ++ proofSignalMaterialSummaryRecords(candidateRecords)).distinctBy(_.ref.id)
      )
      .getOrElse(Nil)

  private def materialDeteriorationSupportRecords(
      referenceRecords: List[EvidenceRecord],
      candidateRecords: List[EvidenceRecord]
  ): List[EvidenceRecord] =
    Option
      .when(materialDeteriorates(referenceRecords, candidateRecords))(
        proofSignalMaterialSummaryRecords(referenceRecords) ++ proofSignalMaterialSummaryRecords(candidateRecords)
      )
      .getOrElse(Nil)
      .distinctBy(_.ref.id)

  private def sacrificeCompensationSupportRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    val material = sacrificeMaterialRecords(records)
    val compensation = compensationSupportRecords(records)
    Option
      .when(material.nonEmpty && compensation.nonEmpty)((material ++ compensation).distinctBy(_.ref.id))
      .getOrElse(Nil)

  private def sacrificeMaterialRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, payload: LineFactEvidence, _) =>
        payload.hasSacrificeConsequence
      case _ =>
        false
    }

  private def compensationSupportRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    (
      strategicCompensationRecords(records) ++
        strategicImprovementSupport(records) ++
        compensationAnchorRecords(records)
    ).distinctBy(_.ref.id)

  private def strategicCompensationRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, payload @ StrategicFactEvidence(StrategicFactKind.Compensation, _, _, confidence), _) =>
        confidence >= 0.35 && payload.hasTypedSupport
      case _ =>
        false
    }

  private def compensationAnchorRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, PlanPressureEvidence(scoring, activePlans), _) =>
        ClaimTruthPolicy.planPressureHasDirectEvidence(scoring, activePlans)
      case EvidenceRecord(_, payload: PawnStructureFactEvidence, _) =>
        ClaimTruthPolicy.pawnStructureCanAnchorPlan(payload)
      case EvidenceRecord(_, FeatureAnchorEvidence(anchor), _) =>
        anchor.signal == FeatureAnchorSignal.CompensationObserved && anchor.hasPositiveStrength
      case _ =>
        false
    }

  private def proofSignalMaterialSummaryRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, payload: LineFactEvidence, _) =>
        payload.hasMaterialConsequence || payload.hasRecaptureRecoveryConsequence
      case _ =>
        false
    }

  private def referenceStrategicImprovementOutperformsCandidate(
      referenceRecords: List[EvidenceRecord],
      candidateRecords: List[EvidenceRecord]
  ): Boolean =
    val referenceScore = strategicImprovementScore(referenceRecords)
    val candidateScore = strategicImprovementScore(candidateRecords)
    (referenceScore >= 3 && referenceScore >= candidateScore + 2) ||
      (referenceScore >= 2 && candidateScore <= 1)

  private def strategicImprovementScore(records: List[EvidenceRecord]): Int =
    records.map {
      case EvidenceRecord(_, payload: StructuralDeltaEvidence, _) =>
        payload.structuralImprovementScore
      case EvidenceRecord(_, PlanTransitionEvidence(transition), _) =>
        if transition.primaryPlanId.nonEmpty && transition.transitionType != TransitionType.Opening then 2 else 0
      case EvidenceRecord(_, PlanPressureEvidence(scoring, activePlans), _) =>
        if ClaimTruthPolicy.planPressureHasDirectEvidence(scoring, activePlans) then 2 else 0
      case EvidenceRecord(_, PawnStructureFactEvidence(_, alignment, pawnPlay), _) =>
        alignment.count(alignment => alignment.band == AlignmentBand.OnBook || alignment.band == AlignmentBand.Playable) * 2 +
          pawnPlay.count(_.primaryDriver != PawnPlayDriver.Quiet)
      case _ =>
        0
    }.sum

  private def normalizeMove(raw: String): String =
    Option(raw).getOrElse("").trim.toLowerCase

  private def typedMaterialConsequenceSwing(
      referenceRecords: List[EvidenceRecord],
      candidateRecords: List[EvidenceRecord]
  ): Boolean =
    materialGainMagnitude(referenceRecords).ordinal > materialGainMagnitude(candidateRecords).ordinal ||
      materialLossMagnitude(candidateRecords).ordinal > materialLossMagnitude(referenceRecords).ordinal

  private def materialDeteriorates(
      referenceRecords: List[EvidenceRecord],
      candidateRecords: List[EvidenceRecord]
  ): Boolean =
    typedMaterialConsequenceSwing(referenceRecords, candidateRecords)

  private def materialGainMagnitude(records: List[EvidenceRecord]): LineMaterialOutcomeMagnitude =
    LineFactEvidence.materialOutcomeProfile(records).gainMagnitude

  private def materialLossMagnitude(records: List[EvidenceRecord]): LineMaterialOutcomeMagnitude =
    LineFactEvidence.materialOutcomeProfile(records).lossMagnitude

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
