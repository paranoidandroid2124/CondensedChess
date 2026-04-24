package lila.commentary.render

import lila.commentary.selection.*

enum RenderRole(val key: String):
  case Primary extends RenderRole("primary")
  case Supporting extends RenderRole("supporting")
  case Context extends RenderRole("context")
  case Contrast extends RenderRole("contrast")

enum RenderStatus(val key: String):
  case Rendered extends RenderStatus("rendered")
  case ContextOnly extends RenderStatus("contextOnly")
  case NoCommentary extends RenderStatus("noCommentary")

final case class RenderText(
    publicText: Option[String],
    forbiddenTerms: Vector[String]
)

final case class RenderEvidenceRef(
    kind: EvidenceRefKind,
    id: String,
    owner: Option[String],
    anchor: Option[String],
    route: Option[String],
    scope: Option[String]
)

object RenderEvidenceRef:
  def from(ref: EvidenceRef): RenderEvidenceRef =
    RenderEvidenceRef(ref.kind, ref.id, ref.owner, ref.anchor, ref.route, ref.scope)

final case class RenderBoundary(
    claimId: String,
    reason: SuppressionReason
)

final case class RenderSuppression(
    claimId: String,
    reasons: Vector[SuppressionReason],
    public: Boolean
)

final case class RenderWording(
    maxStrength: WordingStrength,
    allowedPublicText: Boolean,
    forbiddenTerms: Vector[String]
)

final case class RenderBlock(
    role: RenderRole,
    claimId: String,
    text: RenderText,
    wordingStrength: WordingStrength,
    evidenceIds: Vector[String],
    boundaries: Vector[RenderBoundary],
    nonAuthoritative: Boolean
)

final case class CommentaryRender(
    schemaVersion: Int,
    status: RenderStatus,
    blocks: Vector[RenderBlock],
    evidenceRefs: Vector[RenderEvidenceRef],
    boundaries: Vector[RenderBoundary],
    suppressions: Vector[RenderSuppression],
    wording: RenderWording
)

object CommentaryRendererContract:

  val SchemaVersion = 1

  def render(plan: CommentaryPlan): CommentaryRender =
    val wording = renderWording(plan.wordingRules.maxStrength)
    val publicBlocks =
      if plan.wordingRules.maxStrength == WordingStrength.Hidden then Vector.empty
      else
        Vector(
          plan.main.toVector.flatMap(section => renderSection(plan, section, RenderRole.Primary)),
          renderSection(plan, plan.support, RenderRole.Supporting),
          renderSection(plan, plan.context, RenderRole.Context),
          renderSection(plan, plan.contrast, RenderRole.Contrast)
        ).flatten
    val blockedClaimIds = plan.blocked.map(_.claim.id).toSet
    val boundaries =
      Vector(plan.support, plan.context, plan.contrast)
        .flatMap(
          _.boundaries
            .filterNot(boundary => blockedClaimIds.contains(boundary.claimId))
            .map(boundary => RenderBoundary(boundary.claimId, boundary.reason))
        )
    val suppressions =
      plan.blocked.map(blocked => RenderSuppression(blocked.claim.id, blocked.reasons, public = false))
    val status = statusFor(plan, publicBlocks)
    val publicBoundaries =
      if status == RenderStatus.NoCommentary then Vector.empty
      else boundaries
    CommentaryRender(
      schemaVersion = SchemaVersion,
      status = status,
      blocks = publicBlocks,
      evidenceRefs = if status == RenderStatus.NoCommentary then Vector.empty else renderEvidence(plan),
      boundaries = publicBoundaries,
      suppressions = suppressions,
      wording = wording
    )

  private def renderSection(
      plan: CommentaryPlan,
      section: PlanSection,
      role: RenderRole
  ): Vector[RenderBlock] =
    val blockedClaimIds = plan.blocked.map(_.claim.id).toSet
    section.claims.flatMap: selected =>
      val strength = effectiveStrength(plan, selected, role)
      Option.when(strength != WordingStrength.Hidden && !blockedClaimIds.contains(selected.claim.id)):
        RenderBlock(
          role = role,
          claimId = selected.claim.id,
          text = RenderText(publicText = None, forbiddenTerms = forbiddenTermsFor(selected.claim, strength)),
          wordingStrength = strength,
          evidenceIds = evidenceIdsFor(plan, selected.claim),
          boundaries = section.boundaries
            .filter(_.claimId == selected.claim.id)
            .map(boundary => RenderBoundary(boundary.claimId, boundary.reason)),
          nonAuthoritative = selected.claim.layer == ClaimLayer.SourceContext || selected.bucket == ClaimBucket.ContextOnly
        )

  private def renderEvidence(plan: CommentaryPlan): Vector[RenderEvidenceRef] =
    val publicRefs = publicEvidenceRefs(plan)
    plan.evidence
      .map(_.ref)
      .filter(publicRefs.contains)
      .map(RenderEvidenceRef.from)

  private def evidenceIdsFor(plan: CommentaryPlan, claim: CommentaryClaim): Vector[String] =
    val publicRefs = publicEvidenceRefs(plan)
    claim.evidenceRefs
      .filter(publicRefs.contains)
      .map(_.id)

  private def publicEvidenceRefs(plan: CommentaryPlan): Set[EvidenceRef] =
    val blockedClaimIds = plan.blocked.map(_.claim.id).toSet
    val publicSelectedClaims = publicSections(plan)
      .flatMap(_.claims)
      .filterNot(selected => blockedClaimIds.contains(selected.claim.id))
    val blockedSelectedRefs = publicSections(plan)
      .flatMap(_.claims)
      .filter(selected => blockedClaimIds.contains(selected.claim.id))
      .flatMap(_.claim.evidenceRefs)
    val blockedRefs = (plan.blocked.flatMap(_.claim.evidenceRefs) ++ blockedSelectedRefs).toSet
    val publicClaimRefs = publicSelectedClaims
      .flatMap(_.claim.evidenceRefs)
      .toSet
    val publicCertificationClaimRefs = publicSelectedClaims
      .filter(_.claim.layer == ClaimLayer.Certification)
      .flatMap(_.claim.evidenceRefs)
      .toSet
    plan.evidence
      .map(_.ref)
      .filter(publicClaimRefs.contains)
      .filter(ref => renderableEvidence(ref, plan, publicCertificationClaimRefs))
      .filter(ref => !blockedRefs.contains(ref) || publicClaimRefs.contains(ref))
      .toSet

  private def publicSections(plan: CommentaryPlan): Vector[PlanSection] =
    plan.main.toVector ++ Vector(plan.support, plan.context, plan.contrast)

  private def renderableEvidence(
      ref: EvidenceRef,
      plan: CommentaryPlan,
      publicCertificationClaimRefs: Set[EvidenceRef]
  ): Boolean =
    ref.kind match
      case EvidenceRefKind.RawEngine => false
      case EvidenceRefKind.Certification => bounded(ref)
      case EvidenceRefKind.EngineCertification =>
        publicCertificationClaimRefs.contains(ref) &&
        bounded(ref) &&
          plan.evidence.exists(evidence =>
            evidence.ref.kind == EvidenceRefKind.Certification &&
              bounded(evidence.ref) &&
              sameBinding(ref, evidence.ref)
          ) &&
          hasSameBindingBoardReason(ref, plan)
      case _ => true

  private def hasSameBindingBoardReason(ref: EvidenceRef, plan: CommentaryPlan): Boolean =
    val boardReasonKinds = Set(
      EvidenceRefKind.Root,
      EvidenceRefKind.Witness,
      EvidenceRefKind.Object,
      EvidenceRefKind.Delta
    )
    plan.evidence.exists(evidence =>
      boardReasonKinds.contains(evidence.ref.kind) &&
        bounded(evidence.ref) &&
        sameBinding(ref, evidence.ref)
    )

  private def bounded(ref: EvidenceRef): Boolean =
    ref.owner.nonEmpty &&
      ref.anchor.nonEmpty &&
      ref.route.nonEmpty &&
      ref.scope.nonEmpty

  private def sameBinding(left: EvidenceRef, right: EvidenceRef): Boolean =
    left.owner == right.owner &&
      left.anchor == right.anchor &&
      left.route == right.route &&
      left.scope == right.scope

  private def effectiveStrength(
      plan: CommentaryPlan,
      selected: SelectedClaim,
      role: RenderRole
  ): WordingStrength =
    val roleCap =
      role match
        case RenderRole.Context => WordingStrength.ContextOnly
        case RenderRole.Supporting | RenderRole.Contrast => WordingStrength.QualifiedSupport
        case RenderRole.Primary => plan.wordingRules.maxStrength
    val sourceCap =
      if selected.claim.layer == ClaimLayer.SourceContext then WordingStrength.ContextOnly
      else selected.claim.wordingStrengthCap
    WordingStrength.weaker(WordingStrength.weaker(plan.wordingRules.maxStrength, roleCap), sourceCap)

  private def statusFor(plan: CommentaryPlan, blocks: Vector[RenderBlock]): RenderStatus =
    if blocks.isEmpty then RenderStatus.NoCommentary
    else if blocks.forall(_.role == RenderRole.Context) && plan.main.isEmpty then RenderStatus.ContextOnly
    else RenderStatus.Rendered

  private def renderWording(maxStrength: WordingStrength): RenderWording =
    RenderWording(
      maxStrength = maxStrength,
      allowedPublicText = maxStrength != WordingStrength.Hidden,
      forbiddenTerms = forbiddenTermsForCap(maxStrength)
    )

  private def forbiddenTermsFor(claim: CommentaryClaim, strength: WordingStrength): Vector[String] =
    val base = forbiddenTermsForCap(strength)
    val sourceTerms =
      if claim.layer == ClaimLayer.SourceContext then
        Vector("best", "theory", "forced", "result", "engine", "oracle", "proof", "winning", "drawing")
      else Vector.empty
    (base ++ sourceTerms).distinct

  private def forbiddenTermsForCap(strength: WordingStrength): Vector[String] =
    val proofTerms = Vector("best", "theory", "forced", "result", "oracle", "engine says")
    strength match
      case WordingStrength.Hidden =>
        proofTerms :+ "commentary"
      case WordingStrength.NegativeOnly =>
        proofTerms ++ Vector("winning", "drawing", "main plan")
      case WordingStrength.ContextOnly =>
        proofTerms ++ Vector("current-position proof", "truth")
      case WordingStrength.QualifiedSupport =>
        Vector("best", "theory", "forced", "result", "winning", "drawing", "engine says")
      case WordingStrength.AssertiveCertified =>
        Vector("best", "theory", "result", "winning", "drawing", "engine says", "raw eval", "raw pv", "forced")
