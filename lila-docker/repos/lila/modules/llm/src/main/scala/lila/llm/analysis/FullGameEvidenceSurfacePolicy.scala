package lila.llm.analysis

import lila.llm.{ AuthorEvidenceSummary, AuthorQuestionSummary }
import lila.llm.model.{ NarrativeContext, OpeningEvent, ProbeRequest }
import lila.llm.model.authoring.NarrativeOutline

private[analysis] final case class FullGameEvidencePayload(
    probeRequests: List[ProbeRequest],
    authorQuestions: List[AuthorQuestionSummary],
    authorEvidence: List[AuthorEvidenceSummary]
):
  def nonEmpty: Boolean =
    probeRequests.nonEmpty || authorQuestions.nonEmpty || authorEvidence.nonEmpty

private[analysis] object FullGameEvidenceSurfacePolicy:

  val MaxMoments = 2
  val InternalProbeFanoutMaxMoments = 3
  val MaxProbeRequestsPerMoment = 1

  final case class InternalProbeCandidate(
      ply: Int,
      selectionKind: String,
      strategicSalienceHigh: Boolean,
      ownerMismatch: Boolean,
      compensation: Boolean,
      hasDeferredAlternative: Boolean,
      hasOpeningBranch: Boolean,
      hasStructureDeferred: Boolean,
      hasBlunderWhyNot: Boolean,
      hasEndgameContinuation: Boolean,
      probeRequests: List[ProbeRequest]
  )

  private def openingBranch(ctx: NarrativeContext): Boolean =
    ctx.openingEvent.exists {
      case OpeningEvent.BranchPoint(_, _, _) | OpeningEvent.OutOfBook(_, _, _) |
          OpeningEvent.TheoryEnds(_, _) | OpeningEvent.Novelty(_, _, _, _) =>
        true
      case _ => false
    }

  private def deferredAlternative(ctx: NarrativeContext): Boolean =
    AlternativeNarrativeSupport.build(ctx).isDefined

  private def structureDeferred(ctx: NarrativeContext, hasDeferredAlternative: Boolean): Boolean =
    hasDeferredAlternative &&
      (
        StructurePlanArcBuilder.build(ctx).nonEmpty ||
          ctx.semantic.flatMap(_.structureProfile).nonEmpty ||
          ctx.semantic
            .flatMap(_.planAlignment)
            .exists(alignment => alignment.reasonCodes.nonEmpty || alignment.narrativeIntent.exists(_.trim.nonEmpty))
      )

  private def blunderWhyNot(momentType: String, hasDeferredAlternative: Boolean): Boolean =
    Set("Blunder", "MissedWin", "Mistake").contains(Option(momentType).getOrElse("").trim) && hasDeferredAlternative

  private def endgameContinuation(ctx: NarrativeContext, hasDeferredAlternative: Boolean): Boolean =
    Option(ctx.phase.current).exists(_.trim.equalsIgnoreCase("endgame")) &&
      hasDeferredAlternative

  private def selectionKindPriority(kind: String): Int =
    Option(kind).map(_.trim.toLowerCase) match
      case Some("key")              => 0
      case Some("thread_bridge")    => 1
      case Some("active-note-only") => 2
      case Some("opening")          => 3
      case _                        => 4

  def eligible(
      momentType: String,
      ctx: NarrativeContext,
      outline: NarrativeOutline
  ): Boolean =
    val hasOpeningBranch = openingBranch(ctx)
    val hasDeferredAlternative = deferredAlternative(ctx)
    val hasStructureDeferred = structureDeferred(ctx, hasDeferredAlternative)
    val hasBlunderWhyNot = blunderWhyNot(momentType, hasDeferredAlternative)
    val hasEndgameContinuation = endgameContinuation(ctx, hasDeferredAlternative)
    val hasNarrativeEvidence =
      ctx.probeRequests.nonEmpty || ctx.authorQuestions.nonEmpty || ctx.authorEvidence.nonEmpty
    val hasFocusedSupport =
      outline.beats.exists(_.questionIds.nonEmpty) || outline.beats.exists(_.questionKinds.nonEmpty) || hasDeferredAlternative

    hasNarrativeEvidence &&
    hasFocusedSupport &&
    (hasOpeningBranch || hasStructureDeferred || hasBlunderWhyNot || hasEndgameContinuation)

  def internalCandidate(
      momentType: String,
      selectionKind: String,
      strategicSalienceHigh: Boolean,
      ctx: NarrativeContext,
      outline: NarrativeOutline,
      strategyPack: Option[lila.llm.StrategyPack]
  ): Option[InternalProbeCandidate] =
    if !eligible(momentType, ctx, outline) || ctx.probeRequests.isEmpty then None
    else
      val hasDeferredAlternative = deferredAlternative(ctx)
      val surface = StrategyPackSurface.from(strategyPack)
      Some(
        InternalProbeCandidate(
          ply = ctx.ply,
          selectionKind = selectionKind,
          strategicSalienceHigh = strategicSalienceHigh,
          ownerMismatch = surface.ownerMismatch,
          compensation =
            surface.compensationPosition ||
              strategyPack.flatMap(_.signalDigest).flatMap(_.compensation).exists(_.trim.nonEmpty),
          hasDeferredAlternative = hasDeferredAlternative,
          hasOpeningBranch = openingBranch(ctx),
          hasStructureDeferred = structureDeferred(ctx, hasDeferredAlternative),
          hasBlunderWhyNot = blunderWhyNot(momentType, hasDeferredAlternative),
          hasEndgameContinuation = endgameContinuation(ctx, hasDeferredAlternative),
          probeRequests = ctx.probeRequests.take(MaxProbeRequestsPerMoment)
        )
      )

  def selectInternalProbeMoments(candidates: List[InternalProbeCandidate]): List[Int] =
    candidates
      .sortBy { candidate =>
        val tier =
          if candidate.strategicSalienceHigh then 0
          else if candidate.ownerMismatch || candidate.compensation || candidate.hasDeferredAlternative then 1
          else if candidate.hasOpeningBranch || candidate.hasStructureDeferred || candidate.hasBlunderWhyNot || candidate.hasEndgameContinuation then
            2
          else 3
        val secondarySignals =
          List(
            candidate.ownerMismatch,
            candidate.compensation,
            candidate.hasDeferredAlternative,
            candidate.hasOpeningBranch,
            candidate.hasStructureDeferred,
            candidate.hasBlunderWhyNot,
            candidate.hasEndgameContinuation
          ).count(identity)
        (tier, -secondarySignals, selectionKindPriority(candidate.selectionKind), candidate.ply)
      }
      .take(InternalProbeFanoutMaxMoments)
      .map(_.ply)

  def payload(
      eligible: Boolean,
      probeRequests: List[ProbeRequest],
      authorQuestions: List[AuthorQuestionSummary],
      authorEvidence: List[AuthorEvidenceSummary]
  ): FullGameEvidencePayload =
    if !eligible then FullGameEvidencePayload(Nil, Nil, Nil)
    else
      val carriedQuestions = carriedQuestionSummaries(authorQuestions, authorEvidence)
      val carriedEvidence = carriedEvidenceSummaries(authorEvidence, carriedQuestions)
      FullGameEvidencePayload(
        probeRequests = probeRequests.take(MaxProbeRequestsPerMoment),
        authorQuestions = carriedQuestions,
        authorEvidence = carriedEvidence
      )

  def runtimePayload(
      allowProbeRequests: Boolean,
      probeRequests: List[ProbeRequest],
      authorQuestions: List[AuthorQuestionSummary],
      authorEvidence: List[AuthorEvidenceSummary]
  ): FullGameEvidencePayload =
    val carriedQuestions = carriedQuestionSummaries(authorQuestions, authorEvidence)
    val carriedEvidence = carriedEvidenceSummaries(authorEvidence, carriedQuestions)
    val carriedProbeRequests =
      Option.when(allowProbeRequests)(probeRequests.take(MaxProbeRequestsPerMoment)).getOrElse(Nil)
    if carriedProbeRequests.isEmpty && carriedQuestions.isEmpty && carriedEvidence.isEmpty then
      FullGameEvidencePayload(Nil, Nil, Nil)
    else
      FullGameEvidencePayload(
        probeRequests = carriedProbeRequests,
        authorQuestions = carriedQuestions,
        authorEvidence = carriedEvidence
      )

  private def carriedQuestionSummaries(
      authorQuestions: List[AuthorQuestionSummary],
      authorEvidence: List[AuthorEvidenceSummary]
  ): List[AuthorQuestionSummary] =
    val prioritizedQuestions = authorQuestions.sortBy(question => (question.priority, question.kind, question.id))
    val evidenceLinkedIds = authorEvidence.iterator.map(_.questionId).toSet
    val evidenceLinkedQuestions =
      prioritizedQuestions.filter(question => evidenceLinkedIds.contains(question.id)).take(2)
    val fillerQuestions =
      prioritizedQuestions.filterNot(question => evidenceLinkedIds.contains(question.id)).take(2 - evidenceLinkedQuestions.size)
    (evidenceLinkedQuestions ++ fillerQuestions).take(2)

  private def carriedEvidenceSummaries(
      authorEvidence: List[AuthorEvidenceSummary],
      carriedQuestions: List[AuthorQuestionSummary]
  ): List[AuthorEvidenceSummary] =
    val carriedIds = carriedQuestions.iterator.map(_.id).toSet
    authorEvidence.filter(summary => carriedIds.contains(summary.questionId)).take(2)
