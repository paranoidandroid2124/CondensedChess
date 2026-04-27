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

final case class RenderVariationMove(
    san: String,
    uci: String
)

object RenderVariationMove:
  def from(move: VariationMove): RenderVariationMove =
    RenderVariationMove(move.san, move.uci)

final case class RenderVariationBoundary(
    depthFloor: Int,
    realizedDepth: Int,
    multiPv: Int,
    freshnessChecked: Boolean,
    legalReplayChecked: Boolean,
    baselineChecked: Boolean
)

object RenderVariationBoundary:
  def from(boundary: PreparedVariationBoundary): RenderVariationBoundary =
    RenderVariationBoundary(
      depthFloor = boundary.depthFloor,
      realizedDepth = boundary.realizedDepth,
      multiPv = boundary.multiPv,
      freshnessChecked = boundary.freshnessChecked,
      legalReplayChecked = boundary.legalReplayChecked,
      baselineChecked = boundary.baselineChecked
    )

final case class RenderVariationEvidence(
    proofId: String,
    boundClaimId: String,
    startFen: String,
    owner: String,
    defender: Option[String],
    anchor: String,
    route: String,
    scope: String,
    role: VariationEvidenceRole,
    moveRole: VariationMoveRole,
    lineSan: Vector[String],
    lineUci: Vector[String],
    playedMove: Option[RenderVariationMove],
    candidateMove: Option[RenderVariationMove],
    defenderResource: Option[RenderVariationMove],
    continuation: Vector[RenderVariationMove],
    testedMove: Option[RenderVariationMove],
    testedLine: Vector[RenderVariationMove],
    replyLine: Vector[RenderVariationMove],
    resourceLine: Vector[RenderVariationMove],
    testResult: VariationTestResult,
    proves: String,
    proofPurpose: VariationProofPurpose,
    provenanceRefs: Vector[RenderEvidenceRef],
    boundary: RenderVariationBoundary,
    wordingCap: WordingStrength,
    surfaceAllowance: VariationSurfaceAllowance
)

object RenderVariationEvidence:
  def from(proof: PreparedVariationEvidence): RenderVariationEvidence =
    RenderVariationEvidence(
      proofId = proof.proofId,
      boundClaimId = proof.boundClaimId,
      startFen = proof.startFen,
      owner = proof.owner,
      defender = proof.defender,
      anchor = proof.anchor,
      route = proof.route,
      scope = proof.scope,
      role = proof.role,
      moveRole = proof.moveRole,
      lineSan = proof.lineSan,
      lineUci = proof.lineUci,
      playedMove = proof.playedMove.map(RenderVariationMove.from),
      candidateMove = proof.candidateMove.map(RenderVariationMove.from),
      defenderResource = proof.defenderResource.map(RenderVariationMove.from),
      continuation = proof.continuation.map(RenderVariationMove.from),
      testedMove = proof.testedMove.map(RenderVariationMove.from),
      testedLine = proof.testedLine.map(RenderVariationMove.from),
      replyLine = proof.replyLine.map(RenderVariationMove.from),
      resourceLine = proof.resourceLine.map(RenderVariationMove.from),
      testResult = proof.testResult,
      proves = proof.proves,
      proofPurpose = proof.proofPurpose,
      provenanceRefs = proof.provenanceRefs.map(RenderEvidenceRef.from),
      boundary = RenderVariationBoundary.from(proof.boundary),
      wordingCap = proof.wordingCap,
      surfaceAllowance = proof.surfaceAllowance
    )

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
    variationEvidenceIds: Vector[String],
    boundaries: Vector[RenderBoundary],
    nonAuthoritative: Boolean
)

final case class CommentaryRender(
    schemaVersion: Int,
    status: RenderStatus,
    blocks: Vector[RenderBlock],
    evidenceRefs: Vector[RenderEvidenceRef],
    variationEvidence: Vector[RenderVariationEvidence],
    boundaries: Vector[RenderBoundary],
    suppressions: Vector[RenderSuppression],
    wording: RenderWording
)

object CommentaryRendererContract:

  val SchemaVersion = 2

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
      variationEvidence = if status == RenderStatus.NoCommentary then Vector.empty else renderVariationEvidence(plan, publicBlocks),
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
          variationEvidenceIds = variationEvidenceIdsFor(plan, selected.claim),
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

  private def renderVariationEvidence(
      plan: CommentaryPlan,
      publicBlocks: Vector[RenderBlock]
  ): Vector[RenderVariationEvidence] =
    val publicClaimIds = publicBlocks.map(_.claimId).toSet
    publicVariationEvidence(plan, publicClaimMap(plan, publicClaimIds)).map(RenderVariationEvidence.from)

  private def variationEvidenceIdsFor(plan: CommentaryPlan, claim: CommentaryClaim): Vector[String] =
    publicVariationEvidence(plan, Map(claim.id -> claim)).map(_.proofId)

  private def publicVariationEvidence(
      plan: CommentaryPlan,
      publicClaims: Map[String, CommentaryClaim]
  ): Vector[PreparedVariationEvidence] =
    val blockedClaimIds = plan.blocked.map(_.claim.id).toSet
    plan.variationEvidence
      .map(_.proof)
      .filter(proof => publicClaims.contains(proof.boundClaimId))
      .filter(proof => !blockedClaimIds.contains(proof.boundClaimId))
      .filter(proof => publicClaims.get(proof.boundClaimId).exists(claim => variationProofBoundToClaim(proof, claim)))
      .filter(renderableVariationEvidence)
      .distinct

  private def publicClaimMap(plan: CommentaryPlan, publicClaimIds: Set[String]): Map[String, CommentaryClaim] =
    val blockedClaimIds = plan.blocked.map(_.claim.id).toSet
    publicSections(plan)
      .flatMap(_.claims)
      .map(_.claim)
      .filter(claim => publicClaimIds.contains(claim.id) && !blockedClaimIds.contains(claim.id))
      .map(claim => claim.id -> claim)
      .toMap

  private def variationProofBoundToClaim(
      proof: PreparedVariationEvidence,
      claim: CommentaryClaim
  ): Boolean =
    proof.boundClaimId == claim.id &&
      claim.owner.contains(proof.owner) &&
      claim.anchor.contains(proof.anchor) &&
      claim.route.contains(proof.route) &&
      claim.scope.contains(proof.scope) &&
      proof.defender.forall(defender => claim.defender.contains(defender))

  private def renderableVariationEvidence(proof: PreparedVariationEvidence): Boolean =
    proof.publicSafe &&
      proof.surfaceAllowance == VariationSurfaceAllowance.PublicLine &&
      proof.boundary.publicSafe &&
      proof.lineSan.nonEmpty &&
      proof.lineSan.size == proof.lineUci.size &&
      proof.lineSan.forall(_.trim.nonEmpty) &&
      proof.lineUci.forall(_.trim.nonEmpty) &&
      proof.provenanceRefs.nonEmpty &&
      proof.provenanceRefs.forall(renderableVariationProvenance) &&
      !containsForbiddenVariationProofToken(proof.proves)

  private def renderableVariationProvenance(ref: EvidenceRef): Boolean =
    ref.kind != EvidenceRefKind.RawEngine &&
      ref.kind != EvidenceRefKind.SourceContext &&
      bounded(ref)

  private def containsForbiddenVariationProofToken(value: String): Boolean =
    val normalized = value.toLowerCase.replace('-', '_').replace(':', '_').replace(' ', '_')
    Vector(
      "best",
      "forced",
      "winning",
      "drawing",
      "drawn",
      "result",
      "oracle",
      "engine",
      "raw_pv",
      "eval",
      "theory_truth"
    ).exists(normalized.contains)

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
