package lila.chessjudgment.analysis.assembly

import lila.chessjudgment.model.Fact
import lila.chessjudgment.model.judgment.*

final case class ClaimSeedAssembly(
    input: NormalizedMoveReviewInput,
    context: JudgmentAssemblyContext
)

object ClaimSeedAssembler:

  def assemble(raw: RawMoveReviewInput): Option[ClaimSeedAssembly] =
    ChessIdeaAssembler.assemble(raw).map(enrich)

  def enrich(assembly: ChessIdeaAssembly): ClaimSeedAssembly =
    val allocator = JudgmentProvenanceAllocator.forInput(assembly.input)
    val context = assembly.context
    val claims =
      context.ideas.map { idea =>
        ClaimComposer.fromIdea(
          id = allocator.evidenceId(s"claim:${allocator.key(idea.ref.family)}:${allocator.key(idea.ref.id)}"),
          family = claimFamily(idea.ref.family),
          idea = idea,
          supportingFacts = supportingFacts(context, idea.evidence),
          engineComparison = engineComparison(context, idea),
          confidence = idea.confidence
        )
      }
    val claimRecords = claims.map(claim => ClaimComposer.evidenceRecord(s"${claim.id}:evidence", claim))
    val withEvidence = context.withEvidence(claimRecords)
    val withClaims = claims.foldLeft(withEvidence)((ctx, claim) => ctx.withClaim(claim))
    ClaimSeedAssembly(assembly.input, withClaims)

  private def claimFamily(family: ChessIdeaFamily): ClaimFamily =
    family match
      case ChessIdeaFamily.Tactical      => ClaimFamily.Tactical
      case ChessIdeaFamily.Strategic     => ClaimFamily.Strategic
      case ChessIdeaFamily.PawnStructure => ClaimFamily.PawnStructure
      case ChessIdeaFamily.Opening       => ClaimFamily.Opening
      case ChessIdeaFamily.Defensive     => ClaimFamily.Defensive
      case ChessIdeaFamily.Conversion    => ClaimFamily.Conversion
      case ChessIdeaFamily.Evaluation    => ClaimFamily.Evaluation

  private def engineComparison(
      context: JudgmentAssemblyContext,
      idea: ChessIdea
  ): Option[EvalComparison] =
    context.relativeAssessments
      .find { assessment =>
        idea.moveUci.contains(assessment.played.moveUci) ||
          idea.primaryLine.contains(assessment.candidate.ref) ||
          idea.ref.family == ChessIdeaFamily.Evaluation
      }
      .map(_.comparison)

  private def supportingFacts(
      context: JudgmentAssemblyContext,
      evidence: List[EvidenceRef]
  ): List[Fact] =
    val ids = evidence.map(_.id).toSet
    context.evidenceGraph.records.collect {
      case record if ids.contains(record.ref.id) =>
        record.payload match
          case BoardFactEvidence(facts, _)             => facts
          case StrategicFactEvidence(_, facts, _, _)   => facts
          case _                                       => Nil
    }.flatten.distinct
