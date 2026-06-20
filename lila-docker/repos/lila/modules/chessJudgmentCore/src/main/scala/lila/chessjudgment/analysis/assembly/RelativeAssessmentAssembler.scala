package lila.chessjudgment.analysis.assembly

import chess.{ Color, File }
import lila.chessjudgment.analysis.evaluation.{ JudgmentThresholds, PerspectiveMath, VerdictThresholdPolicy }
import lila.chessjudgment.analysis.policy.ClaimTruthPolicy
import lila.chessjudgment.analysis.singlePosition.PawnPlayDriver
import lila.chessjudgment.analysis.tactical.TacticalMotifClassifier
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
      PerspectiveMath.improvementForMover(
        mover = mover,
        defendedWhiteCp = candidate.whitePovEvalCp,
        threatWhiteCp = reference.whitePovEvalCp
      )
    val winPercentDelta =
      PerspectiveMath.winPercentImprovementForMover(
        mover = mover,
        defendedWhiteCp = candidate.whitePovEvalCp,
        defendedMate = candidate.mate,
        threatWhiteCp = reference.whitePovEvalCp,
        threatMate = reference.mate
      )
    val loss =
      PerspectiveMath.cpLossForMover(
        mover = mover,
        bestWhiteCp = reference.whitePovEvalCp,
        playedWhiteCp = candidate.whitePovEvalCp
      )
    val winPercentLoss =
      PerspectiveMath.winPercentLossForMover(
        mover = mover,
        bestWhiteCp = reference.whitePovEvalCp,
        bestMate = reference.mate,
        playedWhiteCp = candidate.whitePovEvalCp,
        playedMate = candidate.mate
      )
    EvalComparison(
      mover = mover,
      referenceLine = reference.ref,
      candidateLine = candidate.ref,
      candidateDeltaForMover = delta,
      candidateWinPercentDeltaForMover = winPercentDelta,
      cpLossForMover = loss,
      winPercentLossForMover = winPercentLoss,
      verdict = VerdictThresholdPolicy.verdictFromWinPercent(winPercentDelta, winPercentLoss),
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
        PerspectiveMath.cpLossForMover(
          mover = mover,
          bestWhiteCp = reference.whitePovEvalCp,
          playedWhiteCp = line.whitePovEvalCp
        )
      )
    val winPercentGap =
      second.map(line =>
        PerspectiveMath.winPercentLossForMover(
          mover = mover,
          bestWhiteCp = reference.whitePovEvalCp,
          bestMate = reference.mate,
          playedWhiteCp = line.whitePovEvalCp,
          playedMate = line.mate
        )
      )
    Option.when(ordered.nonEmpty)(
      CandidateSetComparison(
        secondLine = second.map(_.ref),
        bestToSecondGapForMover = gap,
        bestToSecondWinPercentGapForMover = winPercentGap,
        candidateCount = ordered.size,
        onlyMove = winPercentGap.exists(_ >= JudgmentThresholds.ONLY_MOVE_GAP_WP)
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
          val proofRecords = (comparisonProof ++ support ++ neighborhood.sharedRecords).distinctBy(_.ref.id)
          val proof = relativeCauseProof(proofRecords)
          val cause =
            RelativeCauseFact(
              kind = kind,
              comparisonKind = fact.kind,
              referenceLine = fact.referenceLine,
              candidateLine = fact.candidateLine,
              verdict = fact.comparison.verdict,
              winPercentLossForMover = fact.comparison.winPercentLossForMover,
              candidateWinPercentDeltaForMover = fact.comparison.candidateWinPercentDeltaForMover,
              evidenceLines = List(fact.referenceLine, fact.candidateLine).distinct
            )(proof = Some(proof).filter(_.hasTypedDepth))
          TransitionFactNormalizer.fromRelativeCause(
            id = allocator.evidenceId(
              s"relative-cause:${allocator.key(fact.kind)}:${allocator.key(kind)}:${allocator.key(fact.referenceLine.rootMove)}:${allocator.key(fact.candidateLine.rootMove)}:$index"
            ),
            cause = cause,
            position = root,
            scope = EvidenceScope.Counterfactual,
            confidence = comparisonConfidence(context, fact),
            parents = (comparisonRecord.ref :: proofRecords.map(_.ref)).distinctBy(_.id)
          )
        }
      case _ => Nil

  private def relativeCauseProof(records: List[EvidenceRecord]): RelativeCauseProof =
    RelativeCauseProof(
      boardAnchors = records.flatMap {
        case EvidenceRecord(_, payload: BoardFactEvidence, _) => payload.proofSignalAnchorKinds
        case _                                                => Nil
      }.distinct,
      lineEvents = records.flatMap {
        case EvidenceRecord(_, payload: LineFactEvidence, _) => payload.lineEventKinds
        case _                                               => Nil
      }.distinct,
      lineConsequences = records.flatMap {
        case EvidenceRecord(_, payload: LineFactEvidence, _) => payload.proofSignalConsequenceKinds
        case _                                               => Nil
      }.distinct,
      relationKinds = records.collect { case EvidenceRecord(_, RelationFactEvidence(kind, _, _, _, _), _) =>
        kind
      }.distinct,
      supportLayers = records.map(_.ref.layer).distinct
    )

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
    val playedCandidateSignificantLoss =
      playedMoveCandidateSideComparison(fact.kind) && tacticalLoss
    val primaryPlayedPositive =
      fact.kind == CandidateComparisonKind.PlayedVsBest && candidateBetter
    val referenceOnlyDefense = onlyDefenseRecords(referenceRecords)
    val candidateOnlyDefense = onlyDefenseRecords(candidateRecords)
    val referenceThreatResource = threatResourceRecords(referenceRecords)
    val candidateThreatResource = threatResourceRecords(candidateRecords)
    val referenceTacticalRisk = tacticalRiskRecords(referenceRecords)
    val referenceKingStepResource = kingStepResourceRecords(referenceRecords)
    val candidateKingStepResource = kingStepResourceRecords(candidateRecords)
    val referenceCastlingResource = castlingResourceRecords(referenceRecords)
    val candidateCastlingResource = castlingResourceRecords(candidateRecords)
    val referencePreventivePawnResource = preventivePawnResourceRecords(referenceRecords)
    val candidatePreventivePawnResource = preventivePawnResourceRecords(candidateRecords)
    val referenceConversionWindow = conversionWindowRecords(referenceRecords)
    val candidateConversionWindow = conversionWindowRecords(candidateRecords)
    val referenceStructuralTargetRelease = structuralTargetReleaseRecords(referenceRecords)
    val candidateStructuralImprovement = structuralImprovementRecords(candidateRecords)
    val candidatePawnStructureImprovement = pawnStructureImprovementRecords(candidateRecords)
    val candidateTargetPressureGain = targetPressureGainRecords(candidateRecords)
    val candidateCenterControlGain = centerControlGainRecords(candidateRecords)
    val candidateDevelopmentActivation = developmentActivationRecords(candidateRecords)
    val candidatePieceActivityGain = pieceActivityGainRecords(candidateRecords)
    val candidatePlanCause = planCauseRecords(candidateRecords)
    val candidateStrategicConcession = strategicConcessionRecords(candidateRecords)
    val candidateKingHomeStepConcession = kingHomeStepConcessionRecords(candidateRecords)
    val referenceRecaptureRecovery = recaptureRecoveryResourceRecords(referenceRecords)
    val candidateRecaptureRecovery = recaptureRecoveryResourceRecords(candidateRecords)
    val sameDestinationCaptureSupport = sameDestinationCaptureChoiceRecords(fact, referenceRecords, candidateRecords)
    val materialSwingSupport = materialSwingSupportRecords(referenceRecords, candidateRecords)
    val materialDeteriorationSupport = materialDeteriorationSupportRecords(referenceRecords, candidateRecords)
    val materialLossSupport = (materialSwingSupport ++ materialDeteriorationSupport).distinctBy(_.ref.id)
    val sacrificeCompensationSupport = sacrificeCompensationSupportRecords(candidateRecords)
    val tempoSupport =
      lineTacticSupport(candidateRecords, fact.candidateLine) ++ lineTacticSupport(referenceRecords, fact.referenceLine)
    val structuralImprovementSupport =
      referenceStructuralTargetRelease ++ candidateStructuralImprovement ++ candidatePawnStructureImprovement
    val strategicIdeaRefutedSupport =
      if strategicIdeaRefuted(referenceRecords, candidateRecords) then
        val strategicAnchor = strategicImprovementSupport(candidateRecords)
        val tacticalBridge = tacticalRefutationBridgeRecords(referenceRecords, candidateRecords)
        Option
          .when(strategicAnchor.nonEmpty && tacticalBridge.nonEmpty)(strategicAnchor ++ tacticalBridge)
          .getOrElse(Nil)
          .distinctBy(_.ref.id)
      else Nil
    val shortTermEvidenceCompetesWithStrategic =
      candidateConcreteTacticalBridgeRecords(candidateRecords).nonEmpty ||
        referenceTacticalRisk.nonEmpty ||
        materialLossSupport.nonEmpty ||
        (referenceRecaptureRecovery.nonEmpty && candidateRecaptureRecovery.isEmpty) ||
        sameDestinationCaptureSupport.nonEmpty ||
        referenceConversionWindow.nonEmpty ||
        candidateConversionWindow.nonEmpty ||
        tempoSupport.nonEmpty
    val missedStrategicSupport =
      missedStrategicImprovementSupport(fact, referenceRecords, candidateRecords, neighborhood.sharedRecords)
    val rawCauses =
      List(
        causeCandidate(RelativeCauseKind.OnlyMoveNecessity, Nil, fact.kind == CandidateComparisonKind.BestVsSecond && majorLoss),
        causeCandidate(RelativeCauseKind.OnlyMoveNecessity, Nil, comparison.candidateSet.exists(_.onlyMove)),
        causeCandidate(RelativeCauseKind.OnlyDefenseNecessity, referenceOnlyDefense, referenceOnlyDefense.nonEmpty && badLoss),
        causeCandidate(RelativeCauseKind.OnlyDefenseNecessity, candidateOnlyDefense, candidateOnlyDefense.nonEmpty && candidateBetter),
        causeCandidate(RelativeCauseKind.KingForcing, mateLineRecords(referenceRecords), hasMateLine(referenceRecords) && badLoss),
        causeCandidate(RelativeCauseKind.DefensiveResource, referenceTacticalRisk, referenceTacticalRisk.nonEmpty && candidateBetter),
        causeCandidate(RelativeCauseKind.DefensiveResource, referenceThreatResource, referenceThreatResource.nonEmpty && badLoss),
        causeCandidate(RelativeCauseKind.DefensiveResource, candidateThreatResource, candidateThreatResource.nonEmpty && candidateBetter),
        causeCandidate(
          RelativeCauseKind.DefensiveResource,
          referenceKingStepResource,
          referenceKingStepResource.nonEmpty && badLoss && candidateKingStepResource.isEmpty
        ),
        causeCandidate(
          RelativeCauseKind.DefensiveResource,
          referenceCastlingResource,
          referenceCastlingResource.nonEmpty && badLoss && candidateCastlingResource.isEmpty
        ),
        causeCandidate(
          RelativeCauseKind.DefensiveResource,
          referencePreventivePawnResource,
          referencePreventivePawnResource.nonEmpty && badLoss && candidatePreventivePawnResource.isEmpty
        ),
        causeCandidate(
          RelativeCauseKind.DefensiveResource,
          referencePreventivePawnResource,
          primaryPlayedSignificantLoss &&
            referencePreventivePawnResource.nonEmpty &&
            candidatePreventivePawnResource.isEmpty
        ),
        causeCandidate(
          RelativeCauseKind.DefensiveResource,
          prophylacticResourceRecords(referenceRecords),
          primaryPlayedSignificantLoss && prophylacticResourceRecords(referenceRecords).nonEmpty
        ),
        causeCandidate(RelativeCauseKind.ConversionMiss, referenceConversionWindow, referenceConversionWindow.nonEmpty && badLoss),
        causeCandidate(
          RelativeCauseKind.ConversionSecured,
          candidateConversionWindow,
          candidateConversionWindow.nonEmpty && candidateBetter
        ),
        causeCandidate(
          RelativeCauseKind.TempoLoss,
          tempoSupport,
          playedCandidateSignificantLoss &&
            candidateAllowsImmediateReplyCheck(neighborhood, fact) &&
            !referenceAllowsImmediateReplyCheck(neighborhood, fact)
        ),
        causeCandidate(
          RelativeCauseKind.StructuralImprovement,
          structuralImprovementSupport,
          candidateBetter &&
            structuralImprovementSupport.nonEmpty
        ),
        causeCandidate(RelativeCauseKind.TargetPressureGain, candidateTargetPressureGain, primaryPlayedPositive && candidateTargetPressureGain.nonEmpty),
        causeCandidate(RelativeCauseKind.CenterControlGain, candidateCenterControlGain, primaryPlayedPositive && candidateCenterControlGain.nonEmpty),
        causeCandidate(
          RelativeCauseKind.DevelopmentActivation,
          candidateDevelopmentActivation,
          primaryPlayedPositive && candidateDevelopmentActivation.nonEmpty
        ),
        causeCandidate(RelativeCauseKind.PieceActivityGain, candidatePieceActivityGain, primaryPlayedPositive && candidatePieceActivityGain.nonEmpty),
        causeCandidate(RelativeCauseKind.PlanImprovement, candidatePlanCause, candidatePlanCause.nonEmpty && candidateBetter),
        causeCandidate(RelativeCauseKind.PlanContradiction, candidatePlanCause, candidatePlanCause.nonEmpty && badLoss),
        causeCandidate(
          RelativeCauseKind.StrategicIdeaRefuted,
          strategicIdeaRefutedSupport,
          playedCandidateSignificantLoss && strategicIdeaRefutedSupport.nonEmpty
        ),
        causeCandidate(
          RelativeCauseKind.CastlingRightsConcession,
          candidateKingHomeStepConcession,
          (badLoss || playedCandidateSignificantLoss) && candidateKingHomeStepConcession.nonEmpty
        ),
        causeCandidate(
          RelativeCauseKind.StrategicConcession,
          candidateStrategicConcession,
          (badLoss || primaryPlayedSignificantLoss) &&
            !shortTermEvidenceCompetesWithStrategic &&
            candidateStrategicConcession.nonEmpty
        ),
        causeCandidate(RelativeCauseKind.MissedStrategicImprovement, missedStrategicSupport, badLoss && missedStrategicSupport.nonEmpty),
        causeCandidate(
          RelativeCauseKind.RecaptureRecoveryWindow,
          referenceRecaptureRecovery,
          badLoss && referenceRecaptureRecovery.nonEmpty && candidateRecaptureRecovery.isEmpty
        ),
        causeCandidate(RelativeCauseKind.WrongRecapturer, sameDestinationCaptureSupport, badLoss && sameDestinationCaptureSupport.nonEmpty),
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
        relationCauses(referenceRecords, candidateRecords, fact.kind, tacticalLoss, candidateBetter) ++
        motifCauses(referenceRecords, candidateRecords, fact.kind, tacticalLoss, candidateBetter)
    suppressGenericStrategicCompanions(rawCauses)

  private def suppressGenericStrategicCompanions(
      causes: List[RelativeCauseCandidate]
  ): List[RelativeCauseCandidate] =
    val hasShortTermCause = causes.exists(candidate => shortTermCause(candidate.kind))
    val hasNonConcessionCause = causes.exists(candidate => candidate.kind != RelativeCauseKind.StrategicConcession)
    causes.filterNot {
      case RelativeCauseCandidate(RelativeCauseKind.StrategicConcession, _) =>
        hasShortTermCause || hasNonConcessionCause
      case RelativeCauseCandidate(RelativeCauseKind.MissedStrategicImprovement, _) =>
        hasShortTermCause
      case _ =>
        false
    }

  private def shortTermCause(kind: RelativeCauseKind): Boolean =
    kind != RelativeCauseKind.DrawResource && ClaimEventCluster.kindForCause(kind).nonEmpty

  private def relationCauses(
      referenceRecords: List[EvidenceRecord],
      candidateRecords: List[EvidenceRecord],
      kind: CandidateComparisonKind,
      tacticalLoss: Boolean,
      candidateBetter: Boolean
  ): List[RelativeCauseCandidate] =
    val referenceCauses =
      Option.when(tacticalLoss)(relationCauseKinds(referenceRecords, badLoss = false)).getOrElse(Nil)
    val candidateBadCauses =
      Option
        .when(tacticalLoss)(
          relationCauseKinds(candidateRecords, badLoss = true, playedCandidate = playedMoveCandidateSideComparison(kind))
        )
        .getOrElse(Nil)
    val candidateBetterCauses =
      Option.when(candidateBetter)(relationCauseKinds(candidateRecords, badLoss = false)).getOrElse(Nil)
    referenceCauses ++ candidateBadCauses ++ candidateBetterCauses

  private def relationCauseKinds(
      records: List[EvidenceRecord],
      badLoss: Boolean,
      playedCandidate: Boolean = false
  ): List[RelativeCauseCandidate] =
    records.collect {
      case record @ EvidenceRecord(_, RelationFactEvidence(kind, _, _, _, _), _) =>
        val cause = kind match
          case RelationFactKind.DefenderTrade =>
            RelativeCauseKind.WrongRecapturer
          case RelationFactKind.Zwischenzug =>
            RelativeCauseKind.TempoLoss
          case RelationFactKind.BadPieceLiquidation =>
            if badLoss then RelativeCauseKind.ConversionMiss else RelativeCauseKind.ConversionSecured
          case RelationFactKind.StalemateTrap | RelationFactKind.PerpetualCheck =>
            RelativeCauseKind.DrawResource
          case RelationFactKind.DoubleCheck | RelationFactKind.BackRankMate | RelationFactKind.MateNet | RelationFactKind.GreekGift =>
            RelativeCauseKind.KingForcing
          case RelationFactKind.Fork | RelationFactKind.Pin | RelationFactKind.Skewer |
              RelationFactKind.Overload | RelationFactKind.Deflection | RelationFactKind.DiscoveredAttack |
              RelationFactKind.Decoy | RelationFactKind.Interference | RelationFactKind.Clearance |
              RelationFactKind.XRay | RelationFactKind.Battery | RelationFactKind.HangingPiece |
              RelationFactKind.TrappedPiece | RelationFactKind.Domination =>
            if badLoss then
              if playedCandidate then RelativeCauseKind.TacticalRefutationOfPlayed else RelativeCauseKind.CandidateTacticalLiability
            else RelativeCauseKind.MissedTacticalResource
        RelativeCauseCandidate(cause, List(record))
    }

  private def motifCauses(
      referenceRecords: List[EvidenceRecord],
      candidateRecords: List[EvidenceRecord],
      kind: CandidateComparisonKind,
      tacticalLoss: Boolean,
      candidateBetter: Boolean
  ): List[RelativeCauseCandidate] =
    val referenceForcing = forcingMotifRecords(referenceRecords)
    val candidateForcing = forcingMotifRecords(candidateRecords)
    val referenceTactical = causeEligibleTacticalMotifRecords(referenceRecords)
    val candidateTactical = causeEligibleTacticalMotifRecords(candidateRecords)
    val referenceConcrete = concreteLineConsequenceRecords(referenceRecords)
    val candidateConcrete = concreteLineConsequenceRecords(candidateRecords)
    List(
      causeCandidate(RelativeCauseKind.MissedTacticalResource, referenceForcing, referenceForcing.nonEmpty && tacticalLoss),
      causeCandidate(
        RelativeCauseKind.TacticalRefutationOfPlayed,
        candidateForcing,
        candidateForcing.nonEmpty && tacticalLoss && playedMoveCandidateSideComparison(kind)
      ),
      causeCandidate(
        RelativeCauseKind.CandidateTacticalLiability,
        candidateForcing,
        candidateForcing.nonEmpty && tacticalLoss && !playedMoveCandidateSideComparison(kind)
      ),
      causeCandidate(RelativeCauseKind.MissedTacticalResource, candidateForcing, candidateForcing.nonEmpty && candidateBetter),
      causeCandidate(
        RelativeCauseKind.MissedTacticalResource,
        referenceTactical ++ referenceConcrete,
        referenceTactical.nonEmpty && referenceConcrete.nonEmpty && tacticalLoss
      ),
      causeCandidate(
        RelativeCauseKind.TacticalRefutationOfPlayed,
        candidateTactical ++ candidateConcrete,
        candidateTactical.nonEmpty && candidateConcrete.nonEmpty && tacticalLoss && playedMoveCandidateSideComparison(kind)
      ),
      causeCandidate(
        RelativeCauseKind.CandidateTacticalLiability,
        candidateTactical ++ candidateConcrete,
        candidateTactical.nonEmpty && candidateConcrete.nonEmpty && tacticalLoss && !playedMoveCandidateSideComparison(kind)
      ),
      causeCandidate(
        RelativeCauseKind.MissedTacticalResource,
        candidateTactical ++ candidateConcrete,
        candidateTactical.nonEmpty && candidateConcrete.nonEmpty && candidateBetter
      )
    ).flatten

  private def playedMoveCandidateSideComparison(kind: CandidateComparisonKind): Boolean =
    kind == CandidateComparisonKind.PlayedVsBest || kind == CandidateComparisonKind.PlayedVsAlternative

  private def forcingMotifRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, MoveMotifEvidence(moveUci, motifs), _) =>
        motifs.exists(motif =>
          TacticalMotifClassifier.isRootMoveMotif(moveUci, motif) &&
            TacticalMotifClassifier.isForcing(motif)
        )
      case _ => false
    }

  private def causeEligibleTacticalMotifRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, MoveMotifEvidence(moveUci, motifs), _) =>
        motifs.exists(motif =>
          TacticalMotifClassifier.isRootMoveMotif(moveUci, motif) &&
            TacticalMotifClassifier.isCauseEligible(motif)
        )
      case _ => false
    }

  private def concreteLineConsequenceRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, payload: LineFactEvidence, _) =>
        payload.hasProofSignalConsequence
      case EvidenceRecord(_, EvalFactEvidence(_, _, mate, _), _) =>
        mate.nonEmpty
      case EvidenceRecord(_, RelationFactEvidence(_, _, _, lineMoves, _), _) =>
        lineMoves.nonEmpty
      case _ =>
        false
    }

  private def lineTacticSupport(records: List[EvidenceRecord], line: LineNodeRef): List[EvidenceRecord] =
    records.filter(record =>
      record.referencesLine(line) &&
        (record.ref.layer == EvidenceLayer.Line ||
          record.ref.layer == EvidenceLayer.Eval ||
          record.ref.layer == EvidenceLayer.MoveMotif ||
          record.ref.layer == EvidenceLayer.Relation)
    )

  private def mateLineRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, EvalFactEvidence(_, _, mate, _), _) => mate.nonEmpty
      case _                                                     => false
    }

  private def tacticalRiskRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, EvalFactEvidence(_, _, mate, _), _) =>
        mate.nonEmpty
      case EvidenceRecord(_, RelationFactEvidence(kind, _, _, lineMoves, _), _) =>
        lineMoves.nonEmpty && TacticalMotifClassifier.isRiskRelation(kind)
      case _ =>
        false
    }

  private def onlyDefenseRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, ThreatPressureEvidence(_, threats), _) =>
        !threats.insufficientData && threats.defense.onlyDefense.nonEmpty
      case _ => false
    }

  private def threatResourceRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, ThreatPressureEvidence(_, threats), _) =>
        !threats.insufficientData &&
          (threats.defense.onlyDefense.nonEmpty ||
            threats.defenseRequired ||
            threats.maxWinPercentLossIfIgnored.exists(_ >= JudgmentThresholds.MATERIAL_THREAT_WP))
      case _ => false
    }

  private def prophylacticResourceRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, ThreatPressureEvidence(_, threats), _) =>
        !threats.insufficientData &&
          (threats.prophylaxisNeeded ||
            threats.defense.prophylaxisNeeded ||
            threats.maxWinPercentLossIfIgnored.exists(_ >= JudgmentThresholds.SIGNIFICANT_THREAT_WP))
      case _ => false
    }

  private def conversionWindowRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, SinglePositionEvidence(assessment), _) =>
        assessment.simplifyBias.shouldSimplify
      case EvidenceRecord(_, RelationFactEvidence(RelationFactKind.BadPieceLiquidation, _, _, _, _), _) =>
        true
      case _ => false
    }

  private def structuralTargetReleaseRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, StructuralDeltaEvidence(delta), _) =>
        delta.releasedTargetPressure.nonEmpty && delta.targetPressureRelease > delta.targetPressureGain
      case _ =>
        false
    }

  private def structuralImprovementRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, StructuralDeltaEvidence(delta), _) =>
        hasStructuralAnchor(delta)
      case _ =>
        false
    }

  private def targetPressureGainRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, StructuralDeltaEvidence(delta), _) =>
        delta.createdTargetPressure.nonEmpty && delta.targetPressureGain > delta.targetPressureRelease ||
          delta.targetPressureDelta > 0
      case _ =>
        false
    }

  private def centerControlGainRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, StructuralDeltaEvidence(delta), _) =>
        delta.centerControlDelta > 0
      case _ =>
        false
    }

  private def developmentActivationRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, StructuralDeltaEvidence(delta), _) =>
        delta.developmentDelta > 0
      case _ =>
        false
    }

  private def pieceActivityGainRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, StructuralDeltaEvidence(delta), _) =>
        delta.lineUnlockDelta > 0 || delta.mobilityDelta > 0
      case _ =>
        false
    }

  private def pawnStructureImprovementRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, PawnStructureFactEvidence(_, alignment, pawnPlay), _) =>
        alignment.exists(alignment => alignment.band == AlignmentBand.OnBook || alignment.band == AlignmentBand.Playable) ||
          pawnPlay.exists(_.primaryDriver != PawnPlayDriver.Quiet)
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
        scoring.confidence >= 0.35 &&
          (activePlans.primary.evidence.nonEmpty || scoring.compatibilityEvents.nonEmpty || activePlans.compatibilityEvents.nonEmpty)
      case _ =>
        false
    }

  private def strategicConcessionRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, StructuralDeltaEvidence(delta), _) =>
        delta.releasedTargetPressure.nonEmpty && delta.targetPressureRelease > delta.targetPressureGain ||
          delta.targetPressureDelta < 0 ||
          delta.fileAccessDelta < 0 ||
          delta.kingShelterDelta < 0
      case _ =>
        false
    }

  private def strategicImprovementSupport(records: List[EvidenceRecord]): List[EvidenceRecord] =
    (
      structuralImprovementRecords(records) ++
        targetPressureGainRecords(records) ++
        centerControlGainRecords(records) ++
        developmentActivationRecords(records) ++
        pieceActivityGainRecords(records) ++
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
        unmatchedAxisSupport(targetPressureGainRecords(referenceRecords), targetPressureGainRecords(candidateRecords)),
        unmatchedAxisSupport(centerControlGainRecords(referenceRecords), centerControlGainRecords(candidateRecords)),
        unmatchedAxisSupport(developmentActivationRecords(referenceRecords), developmentActivationRecords(candidateRecords)),
        samePieceDevelopmentChoiceSupport(fact, referenceRecords, candidateRecords, sharedRecords),
        castlingPathDevelopmentSupport(fact, referenceRecords, candidateRecords),
        unmatchedAxisSupport(pieceActivityGainRecords(referenceRecords), pieceActivityGainRecords(candidateRecords)),
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
      case EvidenceRecord(_, ThreatPressureEvidence(_, threats), _) =>
        !threats.insufficientData &&
          threats.defense.onlyDefense.exists(move => normalizeMove(move) == normalizeMove(fact.referenceLine.rootMove)) &&
          (threats.defenseRequired ||
            threats.maxWinPercentLossIfIgnored.exists(_ >= JudgmentThresholds.MATERIAL_THREAT_WP))
      case _ =>
        false
    }

  private def castlingPathDevelopmentSupport(
      fact: CandidateComparisonFact,
      referenceRecords: List[EvidenceRecord],
      candidateRecords: List[EvidenceRecord]
  ): List[EvidenceRecord] =
    val referenceSupport = developmentChoiceRecords(referenceRecords)
    val castlingMove =
      lineEventMoves(referenceRecords, fact.referenceLine, LineEventKind.Castling)
        .map(normalizeMove)
        .find(move => moverCastlingMove(fact.comparison.mover, move))
    val rootMove = normalizeMove(fact.referenceLine.rootMove)
    val clearsPath =
      castlingMove.exists(move => clearsCastlingPath(fact.comparison.mover, rootMove, move))
    val candidateAlsoCastles =
      castlingMove.exists(move =>
        lineEventMoves(candidateRecords, fact.candidateLine, LineEventKind.Castling)
          .map(normalizeMove)
          .contains(move)
      )
    val lineSupport = castlingMove.toList.flatMap(_ => lineSupportRecords(referenceRecords, fact.referenceLine))
    Option
      .when(
        fact.kind == CandidateComparisonKind.PlayedVsBest &&
          fact.comparison.winPercentLossForMover >= JudgmentThresholds.INACCURACY_WP &&
          referenceSupport.nonEmpty &&
          clearsPath &&
          !candidateAlsoCastles &&
          lineSupport.nonEmpty
      )(referenceSupport ++ lineSupport)
      .getOrElse(Nil)
      .distinctBy(_.ref.id)

  private def lineSupportRecords(records: List[EvidenceRecord], line: LineNodeRef): List[EvidenceRecord] =
    records.filter(record =>
      record.referencesLine(line) &&
        (record.ref.layer == EvidenceLayer.Line || record.ref.layer == EvidenceLayer.Eval)
    )

  private def moverCastlingMove(mover: Color, move: String): Boolean =
    if mover.white then move == "e1g1" || move == "e1c1"
    else move == "e8g8" || move == "e8c8"

  private def clearsCastlingPath(mover: Color, rootMove: String, castlingMove: String): Boolean =
    rootMove.length >= 4 &&
      castlingPathBlockers(mover, castlingMove).contains(rootMove.take(2)) &&
      !castlingPathBlockers(mover, castlingMove).contains(rootMove.slice(2, 4))

  private def castlingPathBlockers(mover: Color, castlingMove: String): Set[String] =
    if mover.white then
      if castlingMove == "e1c1" then Set("b1", "c1", "d1")
      else if castlingMove == "e1g1" then Set("f1", "g1")
      else Set.empty
    else
      if castlingMove == "e8c8" then Set("b8", "c8", "d8")
      else if castlingMove == "e8g8" then Set("f8", "g8")
      else Set.empty

  private def developmentChoiceRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, StructuralDeltaEvidence(delta), _) =>
        delta.developmentDelta > 0 &&
          delta.developmentMoves.exists(move => move.fromBackRank && !move.toBackRank)
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
      case StructuralDeltaEvidence(delta) =>
        delta.developmentMoves.filter(move => move.fromBackRank && !move.toBackRank)
      case _ =>
        Nil

  private def tacticalRefutationBridgeRecords(
      referenceRecords: List[EvidenceRecord],
      candidateRecords: List[EvidenceRecord]
  ): List[EvidenceRecord] =
    (
      candidateConcreteTacticalBridgeRecords(candidateRecords) ++
        materialSwingSupportRecords(referenceRecords, candidateRecords)
    ).distinctBy(_.ref.id)

  private def candidateConcreteTacticalBridgeRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    val forcing = forcingMotifRecords(records)
    val tactical = causeEligibleTacticalMotifRecords(records)
    val concrete = concreteLineConsequenceRecords(records)
    val relationRisk = relationRiskRecords(records)
    (
      forcing ++
        Option.when(tactical.nonEmpty && concrete.nonEmpty)(tactical ++ concrete).getOrElse(Nil) ++
        relationRisk
    ).distinctBy(_.ref.id)

  private def relationRiskRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, RelationFactEvidence(kind, _, _, lineMoves, _), _) =>
        lineMoves.nonEmpty && TacticalMotifClassifier.isRiskRelation(kind)
      case _ =>
        false
    }

  private def recaptureRecoveryResourceRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(_, payload: LineFactEvidence, _) =>
        payload.hasRecaptureRecoveryConsequence
      case _ =>
        false
    }

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
        scoring.confidence >= 0.35 &&
          (activePlans.primary.evidence.nonEmpty ||
            activePlans.primary.support.nonEmpty ||
            scoring.topPlans.exists(plan => plan.evidence.nonEmpty || plan.support.nonEmpty))
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

  private def sameDestinationCaptureChoiceRecords(
      fact: CandidateComparisonFact,
      referenceRecords: List[EvidenceRecord],
      candidateRecords: List[EvidenceRecord]
  ): List[EvidenceRecord] =
    val referenceMove = normalizeMove(fact.referenceLine.rootMove)
    val candidateMove = normalizeMove(fact.candidateLine.rootMove)
    if sameDestinationDifferentOrigin(referenceMove, candidateMove) then
      (
        EvidenceRecord.rootCaptureRecords(referenceRecords, referenceMove) ++
          EvidenceRecord.rootCaptureRecords(candidateRecords, candidateMove)
      )
        .distinctBy(_.ref.id)
    else Nil

  private def kingStepResourceRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(ref, MoveMotifEvidence(moveUci, motifs), _) if ref.lineRootMoveMatches(moveUci) =>
        motifs.exists {
          case Motif.KingStep(stepType, _, _, move) if motifMoveBound(move, moveUci) =>
            stepType != Motif.KingStepType.Activation
          case _ =>
            false
        }
      case _ =>
        false
    }

  private def castlingResourceRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(ref, MoveMotifEvidence(moveUci, motifs), _) if ref.lineRootMoveMatches(moveUci) =>
        motifs.exists {
          case Motif.Castling(_, _, _, move) if motifMoveBound(move, moveUci) => true
          case _                                                             => false
        }
      case _ =>
        false
    }

  private def preventivePawnResourceRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(ref, MoveMotifEvidence(moveUci, motifs), _) if ref.lineRootMoveMatches(moveUci) =>
        val hasFlankPawnMove = motifs.exists {
          case Motif.PawnAdvance(file, _, _, color, _, move) if motifMoveBound(move, moveUci) =>
            kingFlankPawnAdvance(records, file, color)
          case _ =>
            false
        }
        val hasKingSafetyCue = motifs.exists {
          case _: Motif.Pin | _: Motif.WeakBackRank | _: Motif.BackRankMate | _: Motif.MateNet => true
          case _                                                                                => false
        }
        hasFlankPawnMove && hasKingSafetyCue
      case _ =>
        false
    }

  private def kingHomeStepConcessionRecords(records: List[EvidenceRecord]): List[EvidenceRecord] =
    records.filter {
      case EvidenceRecord(ref, MoveMotifEvidence(moveUci, motifs), _) if ref.lineRootMoveMatches(moveUci) =>
        motifs.exists {
          case Motif.KingStep(_, color, _, move) if motifMoveBound(move, moveUci) =>
            kingHomeStep(moveUci, color) && hasCastlingRight(records, color)
          case _ =>
            false
        }
      case _ =>
        false
    }

  private def hasForcingMotif(records: List[EvidenceRecord]): Boolean =
    records.exists {
      case EvidenceRecord(_, MoveMotifEvidence(moveUci, motifs), _) =>
        motifs.exists(motif =>
          TacticalMotifClassifier.isRootMoveMotif(moveUci, motif) &&
            TacticalMotifClassifier.isForcing(motif)
        )
      case _ => false
    }

  private def hasCauseEligibleTacticalMotif(records: List[EvidenceRecord]): Boolean =
    records.exists {
      case EvidenceRecord(_, MoveMotifEvidence(moveUci, motifs), _) =>
        motifs.exists(motif =>
          TacticalMotifClassifier.isRootMoveMotif(moveUci, motif) &&
            TacticalMotifClassifier.isCauseEligible(motif)
        )
      case _ => false
    }

  private def candidateAllowsImmediateReplyCheck(
      neighborhood: ComparisonEvidenceNeighborhood,
      fact: CandidateComparisonFact
  ): Boolean =
    lineAllowsImmediateReplyCheck(neighborhood.candidateRecords, fact.candidateLine)

  private def referenceAllowsImmediateReplyCheck(
      neighborhood: ComparisonEvidenceNeighborhood,
      fact: CandidateComparisonFact
  ): Boolean =
    lineAllowsImmediateReplyCheck(neighborhood.referenceRecords, fact.referenceLine)

  private def lineAllowsImmediateReplyCheck(
      records: List[EvidenceRecord],
      line: LineNodeRef
  ): Boolean =
    lineHasTempoAt(records, line, plyOffset = 1)

  private def lineEventMoves(records: List[EvidenceRecord], line: LineNodeRef, kind: LineEventKind): List[String] =
    records.collectFirst {
      case record @ EvidenceRecord(_, payload: LineFactEvidence, _)
          if record.referencesLine(line) =>
        payload.lineEventMoves(kind)
    }.getOrElse(Nil)

  private def lineHasTempoAt(
      records: List[EvidenceRecord],
      line: LineNodeRef,
      plyOffset: Int
  ): Boolean =
    records.exists {
      case record @ EvidenceRecord(_, payload: LineFactEvidence, _)
          if record.referencesLine(line) =>
        payload.hasTempoEventAt(plyOffset)
      case _ =>
        false
    }

  private def hasMateLine(records: List[EvidenceRecord]): Boolean =
    records.exists {
      case EvidenceRecord(_, EvalFactEvidence(_, _, mate, _), _) => mate.nonEmpty
      case _                                                     => false
    }

  private def hasStructuralAnchor(delta: lila.chessjudgment.analysis.structure.StructuralDelta): Boolean =
    delta.fileAccessDelta > 0 ||
      delta.fileOccupation.nonEmpty ||
      delta.openedFiles.nonEmpty ||
      delta.semiOpenedFiles.nonEmpty ||
      delta.newWeakPawns.nonEmpty ||
      delta.newWeakSquares.nonEmpty ||
      delta.createdTension.nonEmpty ||
      delta.pawnTensionDelta > 0

  private def referenceStrategicImprovementOutperformsCandidate(
      referenceRecords: List[EvidenceRecord],
      candidateRecords: List[EvidenceRecord]
  ): Boolean =
    val referenceScore = strategicImprovementScore(referenceRecords)
    val candidateScore = strategicImprovementScore(candidateRecords)
    (referenceScore >= 3 && referenceScore >= candidateScore + 2) ||
      (referenceScore >= 2 && candidateScore <= 1)

  private def strategicIdeaRefuted(
      referenceRecords: List[EvidenceRecord],
      candidateRecords: List[EvidenceRecord]
  ): Boolean =
    val referenceScore = strategicImprovementScore(referenceRecords)
    val candidateScore = strategicImprovementScore(candidateRecords)
    candidateScore >= 3 &&
      candidateScore >= referenceScore + 2 &&
      tacticalRefutationBridge(referenceRecords, candidateRecords)

  private def tacticalRefutationBridge(
      referenceRecords: List[EvidenceRecord],
      candidateRecords: List[EvidenceRecord]
  ): Boolean =
    candidateConcreteTacticalBridge(referenceRecords) ||
      candidateConcreteTacticalBridge(candidateRecords)

  private def candidateConcreteTacticalBridge(records: List[EvidenceRecord]): Boolean =
    hasForcingMotif(records) ||
      (hasCauseEligibleTacticalMotif(records) && EvidenceRecord.hasConcreteLineSignal(records)) ||
      records.exists {
        case EvidenceRecord(_, RelationFactEvidence(kind, _, _, lineMoves, _), _) =>
          lineMoves.nonEmpty && TacticalMotifClassifier.isRiskRelation(kind)
        case _ =>
          false
      }

  private def strategicImprovementScore(records: List[EvidenceRecord]): Int =
    records.map {
      case EvidenceRecord(_, StructuralDeltaEvidence(delta), _) =>
        structuralImprovementScore(delta)
      case EvidenceRecord(_, PlanTransitionEvidence(transition), _) =>
        if transition.primaryPlanId.nonEmpty && transition.transitionType != TransitionType.Opening then 2 else 0
      case EvidenceRecord(_, PlanPressureEvidence(scoring, activePlans), _) =>
        if scoring.confidence >= 0.35 &&
          (activePlans.primary.evidence.nonEmpty || scoring.compatibilityEvents.nonEmpty || activePlans.compatibilityEvents.nonEmpty)
        then 2
        else 0
      case EvidenceRecord(_, PawnStructureFactEvidence(_, alignment, pawnPlay), _) =>
        alignment.count(alignment => alignment.band == AlignmentBand.OnBook || alignment.band == AlignmentBand.Playable) * 2 +
          pawnPlay.count(_.primaryDriver != PawnPlayDriver.Quiet)
      case _ =>
        0
    }.sum

  private def structuralImprovementScore(delta: lila.chessjudgment.analysis.structure.StructuralDelta): Int =
    delta.createdTargetPressure.size * 3 +
      (delta.targetPressureGain - delta.targetPressureRelease).max(0) * 2 +
      delta.targetPressureDelta.max(0) * 2 +
      delta.centerControlDelta.max(0) +
      delta.developmentDelta.max(0) +
      delta.mobilityDelta.max(0) +
      delta.lineUnlockDelta.max(0) +
      delta.fileAccessDelta.max(0) +
      delta.openedFiles.size +
      delta.semiOpenedFiles.size +
      delta.fileOccupation.size * 2 +
      delta.newWeakPawns.size +
      delta.newWeakSquares.size +
      delta.createdTension.size

  private def sameDestinationDifferentOrigin(left: String, right: String): Boolean =
    left.length >= 4 &&
      right.length >= 4 &&
      left.take(2) != right.take(2) &&
      left.slice(2, 4) == right.slice(2, 4)

  private def kingHomeStep(moveUci: String, color: Color): Boolean =
    val move = normalizeMove(moveUci)
    val from = if color.white then "e1" else "e8"
    val castleDestinations = if color.white then Set("g1", "c1") else Set("g8", "c8")
    move.length >= 4 && move.take(2) == from && !castleDestinations.contains(move.slice(2, 4))

  private def hasCastlingRight(records: List[EvidenceRecord], color: Color): Boolean =
    records.exists(record => castlingRights(record.ref.position.fen).exists(rights => colorCastlingRight(rights, color)))

  private def castlingRights(fen: String): Option[String] =
    Option(fen).flatMap(_.split(" ").lift(2)).filter(_ != "-")

  private def colorCastlingRight(rights: String, color: Color): Boolean =
    if color.white then rights.exists(ch => ch == 'K' || ch == 'Q')
    else rights.exists(ch => ch == 'k' || ch == 'q')

  private def motifMoveBound(move: Option[String], moveUci: String): Boolean =
    move.exists(value => normalizeMove(value) == normalizeMove(moveUci))

  private def normalizeMove(raw: String): String =
    Option(raw).getOrElse("").trim.toLowerCase

  private def kingFlankPawnAdvance(records: List[EvidenceRecord], pawnFile: File, color: Color): Boolean =
    kingFile(records, color).exists(file => (fileIndex(file) - fileIndex(pawnFile)).abs <= 1)

  private def kingFile(records: List[EvidenceRecord], color: Color): Option[File] =
    records.view.flatMap(record => kingFileFromFen(record.ref.position.fen, color)).headOption

  private def kingFileFromFen(fen: String, color: Color): Option[File] =
    val king = if color.white then 'K' else 'k'
    Option(fen)
      .map(_.takeWhile(_ != ' '))
      .flatMap: board =>
        var file = 0
        var found = Option.empty[File]
        board.iterator.foreach {
          case '/' =>
            file = 0
          case ch if ch.isDigit =>
            file += ch.asDigit
          case ch =>
            val current = file
            file += 1
            if ch == king && found.isEmpty then found = Some(fileFromIndex(current))
        }
        found

  private def fileFromIndex(index: Int): File =
    List(File.A, File.B, File.C, File.D, File.E, File.F, File.G, File.H)(index.max(0).min(7))

  private def fileIndex(file: File): Int =
    file match
      case File.A => 0
      case File.B => 1
      case File.C => 2
      case File.D => 3
      case File.E => 4
      case File.F => 5
      case File.G => 6
      case File.H => 7

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
      case MoveTransitionEvidence(moveUci, _, _) =>
        moveUci == edge.moveUci
      case _: StructuralDeltaEvidence | _: PlanTransitionEvidence =>
        record.ref.line.exists(_.rootMove == edge.moveUci) ||
          record.parents.exists(_.id == edge.evidence.id)
      case _ =>
        false

  private def endpointLayer(layer: EvidenceLayer): Boolean =
    layer match
      case EvidenceLayer.Line | EvidenceLayer.Eval | EvidenceLayer.MoveMotif | EvidenceLayer.Relation |
          EvidenceLayer.StructuralDelta =>
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
          EvidenceLayer.Strategic | EvidenceLayer.ThreatPressure | EvidenceLayer.StructuralDelta | EvidenceLayer.PlanTransition =>
        true
      case _ =>
        false
