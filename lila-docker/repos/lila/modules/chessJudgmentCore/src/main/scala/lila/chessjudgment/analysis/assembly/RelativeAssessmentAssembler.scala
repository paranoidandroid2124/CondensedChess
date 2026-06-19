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

  def assemble(raw: RawMoveReviewInput): Option[RelativeAssessmentAssembly] =
    EvidenceFactAssembler.assemble(raw).flatMap(enrich)

  def enrich(assembly: EvidenceFactAssembly): Option[RelativeAssessmentAssembly] =
    val input = assembly.input
    val context = assembly.context
    for
      played <- context.playedTransition
      referenceTransition <- context.referenceTransition
      reference <- context.line(LineNodeRole.BestReference)
      candidate <- context.line(LineNodeRole.Played)
      root <- context.position(PositionNodeRole.Before).map(_.ref)
      mover <- root.sideToMove.orElse(input.sideToMove)
    yield
      val allocator = JudgmentProvenanceAllocator.forInput(input)
      val comparison = compare(mover, reference, candidate, candidateSetComparison(mover, context.lines, reference))
      val counterfactual =
        TransitionFactNormalizer.fromCounterfactual(
          id = allocator.evidenceId(s"counterfactual:played-vs-reference:${candidate.ref.rootMove}"),
          referenceLine = reference.ref,
          candidateLine = candidate.ref,
          comparison = comparison,
          position = root,
          scope = EvidenceScope.Counterfactual,
          parents = parentsForLine(context, reference) ++ parentsForLine(context, candidate) ++ List(played.evidence)
        )
      val relativeEvidence =
        allocator.evidenceRef(
          suffix = s"relative-assessment:${candidate.ref.rootMove}",
          producer = EvidenceProducer.RelativeMoveProducer,
          layer = EvidenceLayer.RelativeAssessment,
          position = root,
          line = Some(candidate.ref),
          scope = EvidenceScope.Counterfactual,
          confidence = EvidenceConfidence.EngineBacked
        )
      val assessment =
        RelativeMoveAssessmentBuilder.fromComparison(
          played = played,
          referenceTransition = Some(referenceTransition),
          reference = reference,
          candidate = candidate,
          comparison = comparison,
          collapse = None,
          confidence = EvidenceConfidence.EngineBacked,
          evidence = relativeEvidence,
          counterfactualEvidence = List(counterfactual.ref)
        )
      val assessmentRecord = TransitionFactNormalizer.fromRelativeAssessment(assessment)
      RelativeAssessmentAssembly(
        input = input,
        context = context
          .withEvidence(List(counterfactual, assessmentRecord))
          .withRelativeAssessment(assessment)
      )

  private def compare(
      mover: Color,
      reference: CandidateLineNode,
      candidate: CandidateLineNode,
      candidateSet: Option[CandidateSetComparison]
  ): EvalComparison =
    val referenceEffective = effectiveWhiteCp(reference)
    val candidateEffective = effectiveWhiteCp(candidate)
    val delta =
      PerspectiveMath.improvementForMover(
        mover = mover,
        defendedWhiteCp = candidateEffective,
        threatWhiteCp = referenceEffective
      )
    val winPercentDelta =
      PerspectiveMath.winPercentImprovementForMover(
        mover = mover,
        defendedWhiteCp = candidateEffective,
        threatWhiteCp = referenceEffective
      )
    val loss =
      PerspectiveMath.cpLossForMover(
        mover = mover,
        bestWhiteCp = referenceEffective,
        playedWhiteCp = candidateEffective
      )
    val winPercentLoss =
      PerspectiveMath.winPercentLossForMover(
        mover = mover,
        bestWhiteCp = referenceEffective,
        playedWhiteCp = candidateEffective
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
          bestWhiteCp = effectiveWhiteCp(reference),
          playedWhiteCp = effectiveWhiteCp(line)
        )
      )
    val winPercentGap =
      second.map(line =>
        PerspectiveMath.winPercentLossForMover(
          mover = mover,
          bestWhiteCp = effectiveWhiteCp(reference),
          playedWhiteCp = effectiveWhiteCp(line)
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

  private def effectiveWhiteCp(line: CandidateLineNode): Int =
    line.mate.map(mate => if mate > 0 then 10000 - mate else -10000 - mate).getOrElse(line.evalCp)

  private def parentsForLine(
      context: JudgmentAssemblyContext,
      line: CandidateLineNode
  ): List[EvidenceRef] =
    context.evidenceGraph.recordsFor(line.ref).map(_.ref)
