package lila.commentary.selection

enum PlanRole(val key: String):
  case Main extends PlanRole("main")
  case Support extends PlanRole("support")
  case Context extends PlanRole("context")
  case Contrast extends PlanRole("contrast")

final case class PlanBoundary(
    claimId: String,
    reason: SuppressionReason
)

final case class PlanEvidence(
    ref: EvidenceRef
)

final case class PlanVariationEvidence(
    proof: PreparedVariationEvidence
)

final case class PlanSection(
    role: PlanRole,
    claims: Vector[SelectedClaim],
    boundaries: Vector[PlanBoundary] = Vector.empty
)

final case class WordingRules(
    maxStrength: WordingStrength
)

final case class BlockedClaim(
    claim: CommentaryClaim,
    reasons: Vector[SuppressionReason]
)

final case class CommentaryPlan(
    main: Option[PlanSection],
    support: PlanSection,
    context: PlanSection,
    contrast: PlanSection,
    blocked: Vector[BlockedClaim],
    evidence: Vector[PlanEvidence],
    variationEvidence: Vector[PlanVariationEvidence],
    wordingRules: WordingRules
):
  def noCommentary: Boolean =
    main.isEmpty &&
      support.claims.isEmpty &&
      context.claims.isEmpty &&
      contrast.claims.isEmpty

object CommentaryOutlineBuilder:

  def build(outline: CommentaryOutline): CommentaryPlan =
    CommentaryPlan(
      main = outline.lead.map(selected => PlanSection(PlanRole.Main, Vector(selected))),
      support = PlanSection(PlanRole.Support, outline.support),
      context = PlanSection(
        PlanRole.Context,
        outline.context,
        outline.context.flatMap(selected =>
          selected.softReasons.map(reason => PlanBoundary(selected.claim.id, reason))
        )
      ),
      contrast = PlanSection(PlanRole.Contrast, outline.contrast),
      blocked = outline.suppressedClaims.map(suppressed => BlockedClaim(suppressed.claim, suppressed.reasons)),
      evidence = outline.evidenceRefs.map(PlanEvidence.apply),
      variationEvidence = outline.variationEvidence.map(PlanVariationEvidence.apply),
      wordingRules = WordingRules(outline.wordingStrengthCap)
    )
