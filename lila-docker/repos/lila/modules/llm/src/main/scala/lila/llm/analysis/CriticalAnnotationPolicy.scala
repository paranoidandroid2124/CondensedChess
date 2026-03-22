package lila.llm.analysis

import lila.llm.model.NarrativeContext

private[analysis] object CriticalAnnotationPolicy:

  def shouldPrioritizeClaim(ctx: NarrativeContext): Boolean =
    !StandardCommentaryClaimPolicy.quietStandardPosition(ctx) &&
      (hasTacticalMetaOrMotifs(ctx) || hasTacticalCounterfactual(ctx) || hasSevereBlunder(ctx))

  def shouldUseTacticalEmphasis(
      ctx: NarrativeContext,
      cpLoss: Int,
      missedMotifPresent: Boolean,
      hasForcingReply: Boolean
  ): Boolean =
    cpLoss >= Thresholds.MISTAKE_CP &&
      (missedMotifPresent || hasForcingReply || hasExplicitTacticalMeta(ctx))

  private def hasExplicitTacticalMeta(ctx: NarrativeContext): Boolean =
    ctx.meta.flatMap(_.errorClass).exists(_.isTactical)

  private def hasTacticalMetaOrMotifs(ctx: NarrativeContext): Boolean =
    ctx.meta.flatMap(_.errorClass).exists(ec => ec.isTactical || ec.missedMotifs.nonEmpty)

  private def hasTacticalCounterfactual(ctx: NarrativeContext): Boolean =
    ctx.counterfactual.exists(cf => cf.cpLoss >= Thresholds.MISTAKE_CP && cf.missedMotifs.nonEmpty)

  private def hasSevereBlunder(ctx: NarrativeContext): Boolean =
    ctx.counterfactual.exists(cf => cf.cpLoss >= Thresholds.BLUNDER_CP && cf.severity == "blunder")
