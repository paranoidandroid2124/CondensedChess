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

enum RenderLineRole(val key: String):
  case Resource extends RenderLineRole("resource")
  case Caution extends RenderLineRole("caution")
  case Hold extends RenderLineRole("hold")
  case Conversion extends RenderLineRole("conversion")
  case Pressure extends RenderLineRole("pressure")
  case Simplification extends RenderLineRole("simplification")

object RenderLineRole:
  def from(role: VariationEvidenceRole): RenderLineRole =
    role match
      case VariationEvidenceRole.DefenderResource => RenderLineRole.Resource
      case VariationEvidenceRole.FailedTemptingMove => RenderLineRole.Caution
      case VariationEvidenceRole.PrematureMove => RenderLineRole.Caution
      case VariationEvidenceRole.ReleaseRisk => RenderLineRole.Caution
      case VariationEvidenceRole.Hold => RenderLineRole.Hold
      case VariationEvidenceRole.Conversion => RenderLineRole.Conversion
      case VariationEvidenceRole.Persistence => RenderLineRole.Pressure
      case VariationEvidenceRole.Simplification => RenderLineRole.Simplification

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
    san: String
)

object RenderVariationMove:
  def from(move: VariationMove): RenderVariationMove =
    RenderVariationMove(move.san)

final case class RenderVariationEvidence(
    proofId: String,
    boundClaimId: String,
    owner: String,
    defender: Option[String],
    anchor: String,
    route: String,
    scope: String,
    role: RenderLineRole,
    moveRole: VariationMoveRole,
    lineSan: Vector[String],
    playedMove: Option[RenderVariationMove],
    candidateMove: Option[RenderVariationMove],
    defenderResource: Option[RenderVariationMove],
    continuation: Vector[RenderVariationMove],
    testedMove: Option[RenderVariationMove],
    testedLine: Vector[RenderVariationMove],
    replyLine: Vector[RenderVariationMove],
    resourceLine: Vector[RenderVariationMove],
    testResult: VariationTestResult,
    proofPurpose: VariationProofPurpose,
    wordingCap: WordingStrength,
    surfaceAllowance: VariationSurfaceAllowance
)

object RenderVariationEvidence:
  def from(proof: PreparedVariationEvidence): RenderVariationEvidence =
    RenderVariationEvidence(
      proofId = proof.proofId,
      boundClaimId = proof.boundClaimId,
      owner = proof.owner,
      defender = proof.defender,
      anchor = proof.anchor,
      route = proof.route,
      scope = proof.scope,
      role = RenderLineRole.from(proof.role),
      moveRole = proof.moveRole,
      lineSan = proof.lineSan,
      playedMove = proof.playedMove.map(RenderVariationMove.from),
      candidateMove = proof.candidateMove.map(RenderVariationMove.from),
      defenderResource = proof.defenderResource.map(RenderVariationMove.from),
      continuation = proof.continuation.map(RenderVariationMove.from),
      testedMove = proof.testedMove.map(RenderVariationMove.from),
      testedLine = proof.testedLine.map(RenderVariationMove.from),
      replyLine = proof.replyLine.map(RenderVariationMove.from),
      resourceLine = proof.resourceLine.map(RenderVariationMove.from),
      testResult = proof.testResult,
      proofPurpose = proof.proofPurpose,
      wordingCap = proof.wordingCap,
      surfaceAllowance = proof.surfaceAllowance
    )

object PublicSurfaceTemplate:

  def renderBlock(claim: PublicClaim): RenderBlock =
    renderBlock(claim, None)

  def renderBlock(claim: PublicClaim, phrase: Option[PublicPhrase]): RenderBlock =
    RenderBlock(
      role = claim.role,
      claimId = claim.claimId,
      text = authorizedText(claim, phrase),
      wordingStrength = claim.wordingStrength,
      evidenceIds = claim.evidenceIds,
      variationEvidenceIds = claim.variationEvidenceIds,
      boundaries = claim.boundaries,
      nonAuthoritative = claim.nonAuthoritative,
      phraseCapability = claim.phraseCapability
    )

  private def authorizedText(claim: PublicClaim, phrase: Option[PublicPhrase]): RenderText =
    claim.text.copy(
      publicText = phrase.filter(phraseAllowed(claim, _)).map(_.text.trim)
    )

  private def phraseAllowed(claim: PublicClaim, phrase: PublicPhrase): Boolean =
    val capability = claim.phraseCapability
    phrase.claimId == claim.claimId &&
      phrase.text.trim.nonEmpty &&
      phrase.predicate == PublicClaimPredicate.LineCommentary &&
      claim.role == RenderRole.Primary &&
      claim.wordingStrength.rank >= WordingStrength.QualifiedSupport.rank &&
      phrase.wordingStrength.rank >= WordingStrength.QualifiedSupport.rank &&
      capability.maxStrength.rank >= claim.wordingStrength.rank &&
      capability.maxStrength.rank >= phrase.wordingStrength.rank &&
      capability.allowsLineCommentary &&
      capability.allowedPredicates.contains(PublicClaimPredicate.LineCommentary) &&
      !capability.allowsResultLanguage &&
      !capability.allowsBestForcedLanguage &&
      !capability.allowsEngineLanguage &&
      !containsForbiddenTerm(phrase.text, capability.forbiddenTerms)

  def containsForbiddenTerm(text: String, terms: Vector[String]): Boolean =
    val normalized = text.toLowerCase
    terms.exists(term => normalized.contains(term.toLowerCase))

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

enum PublicClaimPredicate(val key: String):
  case BoardFact extends PublicClaimPredicate("board_fact")
  case Certification extends PublicClaimPredicate("certification")
  case StrategyProjection extends PublicClaimPredicate("strategy_projection")
  case SourceContext extends PublicClaimPredicate("source_context")
  case LineCommentary extends PublicClaimPredicate("line_commentary")
  case ResultMaterial extends PublicClaimPredicate("result_material")

final case class PublicPhrase(
    claimId: String,
    text: String,
    predicate: PublicClaimPredicate,
    wordingStrength: WordingStrength
)

final case class PhraseCapability(
    maxStrength: WordingStrength,
    allowedPredicates: Set[PublicClaimPredicate],
    allowsResultLanguage: Boolean,
    allowsBestForcedLanguage: Boolean,
    allowsEngineLanguage: Boolean,
    allowsLineCommentary: Boolean,
    forbiddenTerms: Vector[String]
)

final case class PublicClaim(
    role: RenderRole,
    claimId: String,
    text: RenderText,
    wordingStrength: WordingStrength,
    evidenceIds: Vector[String],
    variationEvidenceIds: Vector[String],
    boundaries: Vector[RenderBoundary],
    nonAuthoritative: Boolean,
    phraseCapability: PhraseCapability
):
  def toRenderBlock: RenderBlock =
    PublicSurfaceTemplate.renderBlock(this)

final case class RenderBlock(
    role: RenderRole,
    claimId: String,
    text: RenderText,
    wordingStrength: WordingStrength,
    evidenceIds: Vector[String],
    variationEvidenceIds: Vector[String],
    boundaries: Vector[RenderBoundary],
    nonAuthoritative: Boolean,
    phraseCapability: PhraseCapability
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

final case class PublicCommentaryPlan(
    schemaVersion: Int,
    publicClaims: Vector[PublicClaim],
    evidenceRefs: Vector[RenderEvidenceRef],
    variationEvidence: Vector[RenderVariationEvidence],
    boundaries: Vector[RenderBoundary],
    suppressions: Vector[RenderSuppression],
    wording: RenderWording
)

object CommentaryRendererContract:

  val SchemaVersion = 2

  def publicClaims(plan: CommentaryPlan): Vector[PublicClaim] =
    lowerableSelectedClaims(plan).map(publicClaimFor(plan, _))

  def publicPlan(plan: CommentaryPlan): PublicCommentaryPlan =
    val wording = renderWording(plan.wordingRules.maxStrength)
    val claims = publicClaims(plan)
    val publicBlocks = claims.map(_.toRenderBlock)
    val blockedClaimIds = plan.blocked.map(_.claim.id).toSet
    val boundaries =
      Vector(plan.support, plan.context, plan.contrast)
        .flatMap(
          _.boundaries
            .filterNot(boundary => blockedClaimIds.contains(boundary.claimId))
            .filter(boundary => claims.exists(_.claimId == boundary.claimId))
            .map(boundary => RenderBoundary(boundary.claimId, boundary.reason))
        )
    val suppressions =
      plan.blocked.map(blocked => RenderSuppression(blocked.claim.id, blocked.reasons, public = false))
    val status = statusFor(publicBlocks)
    val publicBoundaries =
      if status == RenderStatus.NoCommentary then Vector.empty
      else boundaries
    PublicCommentaryPlan(
      schemaVersion = SchemaVersion,
      publicClaims = claims,
      evidenceRefs = renderEvidence(plan),
      variationEvidence = renderVariationEvidence(plan, publicBlocks),
      boundaries = publicBoundaries,
      suppressions = suppressions,
      wording = wording
    )

  def render(plan: CommentaryPlan): CommentaryRender =
    render(publicPlan(plan))

  def render(publicPlan: PublicCommentaryPlan): CommentaryRender =
    render(publicPlan, Map.empty)

  def render(publicPlan: PublicCommentaryPlan, phrases: Map[String, PublicPhrase]): CommentaryRender =
    val publicBlocks = publicPlan.publicClaims.map(claim => PublicSurfaceTemplate.renderBlock(claim, phrases.get(claim.claimId)))
    val status = statusFor(publicBlocks)
    CommentaryRender(
      schemaVersion = publicPlan.schemaVersion,
      status = status,
      blocks = publicBlocks,
      evidenceRefs = if status == RenderStatus.NoCommentary then Vector.empty else publicPlan.evidenceRefs,
      variationEvidence = if status == RenderStatus.NoCommentary then Vector.empty else publicPlan.variationEvidence,
      boundaries = if status == RenderStatus.NoCommentary then Vector.empty else publicPlan.boundaries,
      suppressions = publicPlan.suppressions,
      wording = publicPlan.wording
    )

  private final case class LowerableSelected(
      section: PlanSection,
      selected: SelectedClaim,
      role: RenderRole,
      strength: WordingStrength
  )

  private def lowerableSelectedClaims(plan: CommentaryPlan): Vector[LowerableSelected] =
    if plan.wordingRules.maxStrength == WordingStrength.Hidden then Vector.empty
    else
      val blockedClaimIds = plan.blocked.map(_.claim.id).toSet
      publicSectionsWithRoles(plan).flatMap: (section, role) =>
        section.claims.flatMap: selected =>
          val strength = effectiveStrength(plan, selected, role)
          Option.when(
            strength != WordingStrength.Hidden &&
              !blockedClaimIds.contains(selected.claim.id) &&
              lowerableClaimStatus(selected.claim)
          )(LowerableSelected(section, selected, role, strength))

  private def publicSectionsWithRoles(plan: CommentaryPlan): Vector[(PlanSection, RenderRole)] =
    plan.main.toVector.map(_ -> RenderRole.Primary) ++
      Vector(
        plan.support -> RenderRole.Supporting,
        plan.context -> RenderRole.Context,
        plan.contrast -> RenderRole.Contrast
      )

  private def lowerableClaimStatus(claim: CommentaryClaim): Boolean =
    claim.layer match
      case ClaimLayer.SourceContext =>
        claim.status == ClaimStatus.Context || claim.status == ClaimStatus.Admitted
      case ClaimLayer.Engine | ClaimLayer.Renderer =>
        false
      case _ =>
        claim.status == ClaimStatus.Admitted

  private def publicClaimFor(
      plan: CommentaryPlan,
      lowerable: LowerableSelected
  ): PublicClaim =
    val claim = lowerable.selected.claim
    val forbiddenTerms = forbiddenTermsFor(claim, lowerable.strength)
    val variationIds = variationEvidenceIdsFor(plan, claim)
    PublicClaim(
      role = lowerable.role,
      claimId = claim.id,
      text = RenderText(publicText = None, forbiddenTerms = forbiddenTerms),
      wordingStrength = lowerable.strength,
      evidenceIds = evidenceIdsFor(plan, claim),
      variationEvidenceIds = variationIds,
      boundaries = lowerable.section.boundaries
        .filter(_.claimId == claim.id)
        .map(boundary => RenderBoundary(boundary.claimId, boundary.reason)),
      nonAuthoritative = claim.layer == ClaimLayer.SourceContext || lowerable.selected.bucket == ClaimBucket.ContextOnly,
      phraseCapability = phraseCapabilityFor(claim, lowerable.role, lowerable.strength, variationIds, forbiddenTerms)
    )

  private def renderEvidence(plan: CommentaryPlan): Vector[RenderEvidenceRef] =
    val publicRefs = publicEvidenceRefs(plan)
    plan.evidence
      .map(_.ref)
      .filter(publicRefs.contains)
      .map(RenderEvidenceRef.from)

  private def evidenceIdsFor(plan: CommentaryPlan, claim: CommentaryClaim): Vector[String] =
    val publicRefs = publicEvidenceRefs(plan)
    publicRefsForClaim(claim)
      .filter(publicRefs.contains)
      .map(_.id)

  private def publicEvidenceRefs(plan: CommentaryPlan): Set[EvidenceRef] =
    val blockedClaimIds = plan.blocked.map(_.claim.id).toSet
    val publicSelectedClaims = lowerableSelectedClaims(plan).map(_.selected)
    val blockedSelectedRefs = publicSections(plan)
      .flatMap(_.claims)
      .filter(selected => blockedClaimIds.contains(selected.claim.id))
      .flatMap(selected => publicRefsForClaim(selected.claim))
    val blockedRefs = (plan.blocked.flatMap(blocked => publicRefsForClaim(blocked.claim)) ++ blockedSelectedRefs).toSet
    val publicClaimRefs = publicSelectedClaims
      .flatMap(selected => publicRefsForClaim(selected.claim))
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

  private def publicRefsForClaim(claim: CommentaryClaim): Vector[EvidenceRef] =
    claim.evidenceRefs ++ claim.lowerCarrierRefs.filter(isBoardReasonRef)

  private def isBoardReasonRef(ref: EvidenceRef): Boolean =
    Set(
      EvidenceRefKind.Root,
      EvidenceRefKind.Witness,
      EvidenceRefKind.Object,
      EvidenceRefKind.Delta
    ).contains(ref.kind) &&
      bounded(ref)

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
    lowerableSelectedClaims(plan)
      .map(_.selected)
      .map(_.claim)
      .filter(claim => publicClaimIds.contains(claim.id))
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
      EvidenceRef.isPublicSafeProvenanceId(ref.id) &&
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

  private def statusFor(blocks: Vector[RenderBlock]): RenderStatus =
    if blocks.isEmpty then RenderStatus.NoCommentary
    else if blocks.forall(_.role == RenderRole.Context) then RenderStatus.ContextOnly
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
    (base ++ sourceTerms ++ claim.publicSurfaceForbiddenTerms).distinct

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

  private def phraseCapabilityFor(
      claim: CommentaryClaim,
      role: RenderRole,
      strength: WordingStrength,
      variationEvidenceIds: Vector[String],
      forbiddenTerms: Vector[String]
  ): PhraseCapability =
    val layerPredicates =
      claim.layer match
        case ClaimLayer.Projection =>
          Set(PublicClaimPredicate.StrategyProjection)
        case ClaimLayer.Certification =>
          Set(PublicClaimPredicate.BoardFact, PublicClaimPredicate.Certification)
        case ClaimLayer.Root | ClaimLayer.Witness | ClaimLayer.Object | ClaimLayer.Delta =>
          Set(PublicClaimPredicate.BoardFact)
        case ClaimLayer.SourceContext =>
          Set(PublicClaimPredicate.SourceContext)
        case _ =>
          Set.empty[PublicClaimPredicate]
    val basePredicates =
      claim.projectionPhraseCapability match
        case Some(capability) if claim.layer == ClaimLayer.Projection =>
          predicateForKey(capability.allowedPredicateKey).toSet.intersect(layerPredicates)
        case _ => layerPredicates
    val lineAllowed =
      role == RenderRole.Primary &&
        variationEvidenceIds.nonEmpty &&
        strength.rank >= WordingStrength.QualifiedSupport.rank &&
        claim.layer != ClaimLayer.SourceContext &&
        claim.projectionPhraseCapability.forall(capability => !capability.sanOnlyVariationEvidence)
    val predicates =
      if lineAllowed then basePredicates + PublicClaimPredicate.LineCommentary
      else basePredicates
    PhraseCapability(
      maxStrength = strength,
      allowedPredicates = predicates,
      allowsResultLanguage = false,
      allowsBestForcedLanguage = false,
      allowsEngineLanguage = false,
      allowsLineCommentary = lineAllowed,
      forbiddenTerms = forbiddenTerms.distinct
    )

  private def predicateForKey(key: String): Option[PublicClaimPredicate] =
    PublicClaimPredicate.values.find(_.key == key)
