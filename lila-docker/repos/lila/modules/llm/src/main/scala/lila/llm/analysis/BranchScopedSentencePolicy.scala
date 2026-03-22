package lila.llm.analysis

import lila.llm.model.NarrativeContext
import lila.llm.model.authoring.{ NarrativeOutline, OutlineBeat }

enum BranchScopedSentenceDecision:
  case KeepWithCitation
  case DowngradeToNeutral
  case Drop

object BranchScopedSentencePolicy:

  def decision(ctx: NarrativeContext, beat: OutlineBeat): BranchScopedSentenceDecision =
    if !beat.branchScoped then BranchScopedSentenceDecision.DowngradeToNeutral
    else if EarlyOpeningNarrationPolicy.collapsedEarlyOpening(ctx) then BranchScopedSentenceDecision.Drop
    else if hasUsableCitation(beat) && !LineScopedCitation.hasSourceLabelOnly(beat.text) then
      BranchScopedSentenceDecision.KeepWithCitation
    else BranchScopedSentenceDecision.Drop

  def keepBeat(ctx: NarrativeContext, beat: OutlineBeat): Boolean =
    decision(ctx, beat) != BranchScopedSentenceDecision.Drop

  def hasBranchScopedProse(outline: NarrativeOutline): Boolean =
    outline.beats.exists(_.branchScoped)

  def hasUsableCitation(beat: OutlineBeat): Boolean =
    LineScopedCitation.hasInlineCitation(beat.text) ||
      (beat.kind == lila.llm.model.authoring.OutlineBeatKind.Evidence && LineScopedCitation.hasConcreteSanLine(beat.text))
