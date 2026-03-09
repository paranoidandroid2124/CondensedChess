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
  val MaxProbeRequestsPerMoment = 1

  def eligible(
      momentType: String,
      ctx: NarrativeContext,
      outline: NarrativeOutline
  ): Boolean =
    val thesis = StrategicThesisBuilder.build(ctx)
    val hasOpeningBranch =
      ctx.openingEvent.exists {
        case OpeningEvent.BranchPoint(_, _, _) | OpeningEvent.OutOfBook(_, _, _) |
            OpeningEvent.TheoryEnds(_, _) | OpeningEvent.Novelty(_, _, _, _) =>
          true
        case _ => false
      }
    val hasDeferredAlternative = AlternativeNarrativeSupport.build(ctx).isDefined
    val hasStructureDeferred =
      thesis.exists(_.lens == StrategicLens.Structure) && hasDeferredAlternative
    val hasBlunderWhyNot =
      Set("Blunder", "MissedWin", "Mistake").contains(Option(momentType).getOrElse("").trim) && hasDeferredAlternative
    val hasEndgameContinuation =
      Option(ctx.phase.current).exists(_.trim.equalsIgnoreCase("endgame")) &&
        (hasDeferredAlternative || ctx.latentPlans.nonEmpty)
    val hasNarrativeEvidence =
      ctx.probeRequests.nonEmpty || ctx.authorQuestions.nonEmpty || ctx.authorEvidence.nonEmpty
    val hasFocusedSupport =
      outline.beats.exists(_.questionIds.nonEmpty) || outline.beats.exists(_.questionKinds.nonEmpty) || hasDeferredAlternative

    hasNarrativeEvidence &&
    hasFocusedSupport &&
    (hasOpeningBranch || hasStructureDeferred || hasBlunderWhyNot || hasEndgameContinuation)

  def payload(
      eligible: Boolean,
      probeRequests: List[ProbeRequest],
      authorQuestions: List[AuthorQuestionSummary],
      authorEvidence: List[AuthorEvidenceSummary]
  ): FullGameEvidencePayload =
    if !eligible then FullGameEvidencePayload(Nil, Nil, Nil)
    else
      FullGameEvidencePayload(
        probeRequests = probeRequests.take(MaxProbeRequestsPerMoment),
        authorQuestions = authorQuestions.take(2),
        authorEvidence = authorEvidence.take(2)
      )
