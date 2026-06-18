package lila.chessjudgment.analysis.assembly

import chess.Color
import lila.chessjudgment.analysis.evaluation.{ PerspectiveMath, VerdictThresholdPolicy }
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
      reference <- context.line(LineNodeRole.BestReference)
      candidate <- context.line(LineNodeRole.Played)
      root <- context.position(PositionNodeRole.Before).map(_.ref)
    yield
      val allocator = JudgmentProvenanceAllocator.forInput(input)
      val mover = root.sideToMove.getOrElse(input.sideToMove.getOrElse(Color.White))
      val comparison = compare(mover, reference, candidate)
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
          referenceTransition = context.referenceTransition,
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
      candidate: CandidateLineNode
  ): EvalComparison =
    val referenceEffective = effectiveWhiteCp(reference)
    val candidateEffective = effectiveWhiteCp(candidate)
    val delta =
      PerspectiveMath.improvementForMover(
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
    EvalComparison(
      mover = mover,
      referenceLine = reference.ref,
      candidateLine = candidate.ref,
      candidateDeltaForMover = delta,
      cpLossForMover = loss,
      verdict = VerdictThresholdPolicy.verdictFromDelta(delta, loss)
    )

  private def effectiveWhiteCp(line: CandidateLineNode): Int =
    line.mate.map(mate => if mate > 0 then 10000 - mate else -10000 + mate).getOrElse(line.evalCp)

  private def parentsForLine(
      context: JudgmentAssemblyContext,
      line: CandidateLineNode
  ): List[EvidenceRef] =
    context.evidenceGraph.recordsFor(line.ref).map(_.ref)
