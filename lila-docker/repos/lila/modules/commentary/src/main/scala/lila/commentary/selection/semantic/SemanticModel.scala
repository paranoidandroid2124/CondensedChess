package lila.commentary.selection.semantic

import lila.commentary.api.CommentaryPipelineInput
import lila.commentary.selection.*

object SemanticModelShape:
  val InputSize: Int = 8969
  val ScoreVectorSize: Int = 64
  val TopK: Int = 8

enum SemanticGate(val key: String, val slot: Int):
  case ExactBoardBound extends SemanticGate("exact_board_bound", 0)
  case LegalReplay extends SemanticGate("legal_replay", 1)
  case RootSupport extends SemanticGate("root_support", 2)
  case EvidencePresent extends SemanticGate("evidence_present", 3)
  case OwnerBound extends SemanticGate("owner_bound", 4)
  case AnchorBound extends SemanticGate("anchor_bound", 5)
  case RouteBound extends SemanticGate("route_bound", 6)
  case ScopeBound extends SemanticGate("scope_bound", 7)
  case NoRawEngine extends SemanticGate("no_raw_engine", 8)
  case SourceSafe extends SemanticGate("source_safe", 9)
  case PublicVariationSafe extends SemanticGate("public_variation_safe", 10)
  case DepthFreshnessSafe extends SemanticGate("depth_freshness_safe", 11)
  case SameRootCertification extends SemanticGate("same_root_certification", 12)
  case NoTacticalRelease extends SemanticGate("no_tactical_release", 13)
  case NoForbiddenShortcut extends SemanticGate("no_forbidden_shortcut", 14)
  case RenderContractSafe extends SemanticGate("render_contract_safe", 15)
  case StatusAdmitted extends SemanticGate("status_admitted", 16)

enum SemanticRole(val key: String):
  case Lead extends SemanticRole("lead")
  case Support extends SemanticRole("support")
  case Context extends SemanticRole("context")
  case Suppressed extends SemanticRole("suppressed")
  case Abstain extends SemanticRole("abstain")

final case class CandidateInputVector(values: Vector[Int]):
  require(
    values.size == SemanticModelShape.InputSize,
    s"CandidateInputVector must contain ${SemanticModelShape.InputSize} values"
  )

object CandidateInputVector:
  def zero: CandidateInputVector =
    CandidateInputVector(Vector.fill(SemanticModelShape.InputSize)(0))

final case class CandidateScoreVector(values: Vector[Double]):
  require(
    values.size == SemanticModelShape.ScoreVectorSize,
    s"CandidateScoreVector must contain ${SemanticModelShape.ScoreVectorSize} values"
  )

final case class SemanticCandidate(
    claimId: String,
    layer: ClaimLayer,
    status: ClaimStatus,
    inputVector: CandidateInputVector
):
  require(claimId.trim.nonEmpty, "SemanticCandidate claim id must be non-empty")

final case class SemanticDecision(
    candidate: SemanticCandidate,
    scoreVector: CandidateScoreVector,
    gateFailures: Vector[SemanticGate],
    suppressionReasons: Vector[SuppressionReason],
    leadScore: Double,
    supportScore: Double,
    contextScore: Double,
    confidenceScore: Double,
    salienceScore: Double,
    abstainScore: Double,
    role: SemanticRole,
    rankOrdinal: Int,
    publicEligible: Boolean
)

final case class SemanticModelResult(
    inputSize: Int,
    scoreVectorSize: Int,
    topK: Int,
    candidates: Vector[SemanticCandidate],
    decisions: Vector[SemanticDecision],
    topDecisions: Vector[SemanticDecision],
    legacyLeadClaimId: Option[String],
    selectedPublicClaimIds: Vector[String]
)

final case class SemanticDecisionSummary(
    claimId: String,
    role: SemanticRole,
    publicEligible: Boolean,
    gateFailures: Vector[SemanticGate],
    suppressionReasons: Vector[SuppressionReason],
    leadScore: Double,
    supportScore: Double,
    contextScore: Double,
    confidenceScore: Double,
    abstainScore: Double,
    rankOrdinal: Int
)

final case class SemanticShadowSummary(
    inputSize: Int,
    scoreVectorSize: Int,
    topK: Int,
    candidateCount: Int,
    topDecisions: Vector[SemanticDecisionSummary],
    legacyLeadClaimId: Option[String],
    selectedPublicClaimIds: Vector[String]
)

object SemanticShadowSummary:

  def from(result: SemanticModelResult): SemanticShadowSummary =
    SemanticShadowSummary(
      inputSize = result.inputSize,
      scoreVectorSize = result.scoreVectorSize,
      topK = result.topK,
      candidateCount = result.candidates.size,
      topDecisions = result.topDecisions.map(fromDecision),
      legacyLeadClaimId = result.legacyLeadClaimId,
      selectedPublicClaimIds = result.selectedPublicClaimIds
    )

  private def fromDecision(decision: SemanticDecision): SemanticDecisionSummary =
    SemanticDecisionSummary(
      claimId = decision.candidate.claimId,
      role = decision.role,
      publicEligible = decision.publicEligible,
      gateFailures = decision.gateFailures,
      suppressionReasons = decision.suppressionReasons,
      leadScore = decision.leadScore,
      supportScore = decision.supportScore,
      contextScore = decision.contextScore,
      confidenceScore = decision.confidenceScore,
      abstainScore = decision.abstainScore,
      rankOrdinal = decision.rankOrdinal
    )

object SemanticClaimSelector:

  def shadow(
      input: CommentaryPipelineInput,
      claims: Vector[CommentaryClaim],
      legacyOutline: CommentaryOutline
  ): SemanticModelResult =
    val decisions =
      claims
        .map(claim => decision(input, claim))
        .sortWith(strongerDecision)
        .zipWithIndex
        .map((decision, index) => decision.copy(rankOrdinal = index + 1))
    SemanticModelResult(
      inputSize = SemanticModelShape.InputSize,
      scoreVectorSize = SemanticModelShape.ScoreVectorSize,
      topK = SemanticModelShape.TopK,
      candidates = decisions.map(_.candidate),
      decisions = decisions,
      topDecisions = decisions.take(SemanticModelShape.TopK),
      legacyLeadClaimId = legacyOutline.lead.map(_.claim.id),
      selectedPublicClaimIds =
        (legacyOutline.lead.toVector ++ legacyOutline.support ++ legacyOutline.context ++ legacyOutline.contrast)
          .map(_.claim.id)
          .distinct
    )

  private def decision(input: CommentaryPipelineInput, claim: CommentaryClaim): SemanticDecision =
    val gates = gateFailures(claim)
    val suppressions = suppressionReasons(claim, gates)
    val publicEligible = gates.isEmpty && suppressions.isEmpty
    val scores = hceScores(claim, publicEligible, suppressions)
    val role =
      if !publicEligible then SemanticRole.Suppressed
      else if claim.layer == ClaimLayer.SourceContext then SemanticRole.Context
      else if scores.lead >= 55 && scores.confidence >= 50 && scores.abstain < 60 then SemanticRole.Lead
      else if scores.support >= 45 then SemanticRole.Support
      else SemanticRole.Abstain
    val candidate =
      SemanticCandidate(
        claimId = claim.id,
        layer = claim.layer,
        status = claim.status,
        inputVector = inputVector(input, claim, gates, suppressions)
      )
    SemanticDecision(
      candidate = candidate,
      scoreVector = scoreVector(gates, suppressions, scores, role, publicEligible),
      gateFailures = gates,
      suppressionReasons = suppressions,
      leadScore = scores.lead,
      supportScore = scores.support,
      contextScore = scores.context,
      confidenceScore = scores.confidence,
      salienceScore = scores.salience,
      abstainScore = scores.abstain,
      role = role,
      rankOrdinal = 0,
      publicEligible = publicEligible
    )

  private def gateFailures(claim: CommentaryClaim): Vector[SemanticGate] =
    val bindingRequired = requiresBoundClaimBinding(claim)
    Vector(
      Option.when(requiresExactBoardBound(claim) && !claim.exactBoardBound)(SemanticGate.ExactBoardBound),
      Option.when(!statusCanEnterSemanticPublicSet(claim))(SemanticGate.StatusAdmitted),
      Option.when(bindingRequired && claim.owner.forall(_.trim.isEmpty))(SemanticGate.OwnerBound),
      Option.when(bindingRequired && claim.anchor.forall(_.trim.isEmpty))(SemanticGate.AnchorBound),
      Option.when(bindingRequired && claim.route.forall(_.trim.isEmpty))(SemanticGate.RouteBound),
      Option.when(bindingRequired && claim.scope.forall(_.trim.isEmpty))(SemanticGate.ScopeBound),
      Option.when(!hasEvidence(claim))(SemanticGate.EvidencePresent),
      Option.when(hasRawEngineOnly(claim))(SemanticGate.NoRawEngine),
      Option.when(isSourceContextOnly(claim))(SemanticGate.SourceSafe),
      Option.when(hasUnsafeVariationEvidence(claim))(SemanticGate.PublicVariationSafe),
      Option.when(claim.suppressionHints.contains(SuppressionReason.StaleEvidence))(
        SemanticGate.DepthFreshnessSafe
      ),
      Option.when(claim.suppressionHints.contains(SuppressionReason.ForbiddenShortcut))(
        SemanticGate.NoForbiddenShortcut
      ),
      Option.when(claim.layer == ClaimLayer.Renderer)(SemanticGate.RenderContractSafe)
    ).flatten.distinct

  private def suppressionReasons(
      claim: CommentaryClaim,
      gates: Vector[SemanticGate]
  ): Vector[SuppressionReason] =
    val statusReasons =
      claim.status match
        case ClaimStatus.SupportOnly => Vector(SuppressionReason.SupportOnly)
        case ClaimStatus.Deferred => Vector(SuppressionReason.Deferred)
        case ClaimStatus.AntiCase => Vector(SuppressionReason.AntiCase)
        case ClaimStatus.Rejected => Vector(SuppressionReason.ForbiddenShortcut)
        case _ => Vector.empty
    val gateReasons =
      gates.flatMap:
        case SemanticGate.ExactBoardBound | SemanticGate.DepthFreshnessSafe =>
          Some(SuppressionReason.StaleEvidence)
        case SemanticGate.EvidencePresent => Some(SuppressionReason.NoBoardReason)
        case SemanticGate.OwnerBound => Some(SuppressionReason.WrongOwner)
        case SemanticGate.AnchorBound => Some(SuppressionReason.WrongAnchor)
        case SemanticGate.RouteBound => Some(SuppressionReason.WrongRoute)
        case SemanticGate.ScopeBound => Some(SuppressionReason.ScopeMismatch)
        case SemanticGate.NoRawEngine => Some(SuppressionReason.RawEngineOnly)
        case SemanticGate.SourceSafe => Some(SuppressionReason.SourceContextOnly)
        case SemanticGate.PublicVariationSafe => Some(SuppressionReason.RawEngineOnly)
        case SemanticGate.NoForbiddenShortcut => Some(SuppressionReason.ForbiddenShortcut)
        case SemanticGate.RenderContractSafe => Some(SuppressionReason.RendererNotAllowed)
        case _ => None
    (statusReasons ++ gateReasons ++ claim.suppressionHints).distinct

  private def inputVector(
      input: CommentaryPipelineInput,
      claim: CommentaryClaim,
      gates: Vector[SemanticGate],
      suppressions: Vector[SuppressionReason]
  ): CandidateInputVector =
    val values = Array.fill(SemanticModelShape.InputSize)(0)
    values(0) = stableBucket(input.node.nodeId)
    values(1) = input.node.ply
    values(2) = stableBucket(input.currentFen.value)
    values(8713) = claim.layer.ordinal
    values(8714) = claim.status.ordinal
    values(8715) = Option.when(claim.exactBoardBound)(1).getOrElse(0)
    values(8716) = claim.impact.resultMaterialImpact
    values(8717) = claim.impact.forcedness
    values(8718) = claim.impact.immediacy
    values(8719) = claim.impact.persistenceAfterDefense
    values(8720) = claim.impact.evidenceConfidence
    values(8721) = math.min(claim.impact.evalSwing, 100)
    values(8722) = claim.impact.boardExplainability
    values(8723) = claim.impact.pedagogicalClarity
    values(8724) = claim.impact.novelty
    values(8725) = claim.evidenceRefs.size
    values(8726) = claim.lowerCarrierRefs.size
    values(8727) = claim.variationEvidence.size
    values(8728) = gates.size
    values(8729) = suppressions.size
    claim.owner.foreach(owner => values(8730) = stableBucket(owner))
    claim.anchor.foreach(anchor => values(8731) = stableBucket(anchor))
    claim.route.foreach(route => values(8732) = stableBucket(route))
    claim.scope.foreach(scope => values(8733) = stableBucket(scope))
    CandidateInputVector(values.toVector)

  private def scoreVector(
      gates: Vector[SemanticGate],
      suppressions: Vector[SuppressionReason],
      scores: HceScores,
      role: SemanticRole,
      publicEligible: Boolean
  ): CandidateScoreVector =
    val values = Array.fill(SemanticModelShape.ScoreVectorSize)(0.0)
    SemanticGate.values
      .filter(_.slot < 16)
      .foreach: gate =>
        values(gate.slot) = if gates.contains(gate) then 0.0 else 1.0
    suppressionSlot.foreach((reason, slot) =>
      values(slot) = if suppressions.contains(reason) then 1.0 else 0.0
    )
    values(32) = scores.lead
    values(33) = scores.support
    values(34) = scores.context
    values(35) = 0.0
    values(36) = if suppressions.contains(SuppressionReason.AntiCase) then 100.0 else 0.0
    values(37) = scores.salience
    values(38) = scores.confidence
    values(39) = scores.specificity
    values(40) = scores.pedagogicalClarity
    values(41) = scores.tacticalUrgency
    values(42) = scores.strategicRelevance
    values(43) = scores.lineSupport
    values(44) = scores.sourceFit
    values(45) = scores.novelty
    values(46) = scores.redundancyPenalty
    values(47) = scores.abstain
    values(48 + frameSlot(role)) = frameScore(role)
    values(60) = role.ordinal.toDouble
    values(61) = scores.wordingCode.toDouble
    values(62) = 0.0
    values(63) = if publicEligible then 1.0 else 0.0
    CandidateScoreVector(values.toVector)

  private def hceScores(
      claim: CommentaryClaim,
      publicEligible: Boolean,
      suppressions: Vector[SuppressionReason]
  ): HceScores =
    val impact = claim.impact
    val evidenceBonus =
      math.min(
        20,
        claim.evidenceRefs.size * 6 + claim.lowerCarrierRefs.size * 4 + claim.variationEvidence.count(
          _.publicSafe
        ) * 8
      )
    val layerBonus =
      claim.layer match
        case ClaimLayer.Certification => 10
        case ClaimLayer.Projection => 8
        case ClaimLayer.Delta => 6
        case ClaimLayer.Object => 4
        case ClaimLayer.Witness | ClaimLayer.Root => 2
        case ClaimLayer.SourceContext => -20
        case ClaimLayer.Engine | ClaimLayer.Renderer => -40
    val baseLead =
      weighted(
        impact.resultMaterialImpact -> 0.25,
        impact.forcedness -> 0.16,
        impact.immediacy -> 0.16,
        impact.persistenceAfterDefense -> 0.12,
        impact.evidenceConfidence -> 0.12,
        impact.boardExplainability -> 0.10,
        impact.pedagogicalClarity -> 0.06,
        impact.novelty -> 0.03
      ) + evidenceBonus + layerBonus
    val confidence = clamp(
      impact.evidenceConfidence * 0.75 + evidenceBonus + Option.when(claim.exactBoardBound)(10).getOrElse(0)
    )
    val support = clamp(baseLead * 0.72 + impact.persistenceAfterDefense * 0.18 + evidenceBonus)
    val context = clamp(
      if claim.layer == ClaimLayer.SourceContext then 55 + evidenceBonus
      else impact.pedagogicalClarity * 0.35 + impact.boardExplainability * 0.25
    )
    val abstain =
      if publicEligible then clamp(45 - confidence * 0.25)
      else clamp(70 + suppressions.size * 5)
    HceScores(
      lead = if publicEligible then clamp(baseLead) else 0.0,
      support = if publicEligible then support else 0.0,
      context = if publicEligible || claim.layer == ClaimLayer.SourceContext then context else 0.0,
      confidence = if publicEligible then confidence else clamp(confidence * 0.5),
      salience = clamp(
        (impact.resultMaterialImpact + impact.forcedness + impact.immediacy + impact.boardExplainability) / 4.0
      ),
      specificity = clamp(bindingCount(claim) * 20.0 + evidenceBonus),
      pedagogicalClarity = impact.pedagogicalClarity.toDouble,
      tacticalUrgency = clamp((impact.forcedness + impact.immediacy) / 2.0),
      strategicRelevance = clamp((impact.persistenceAfterDefense + impact.boardExplainability) / 2.0),
      lineSupport = clamp(claim.variationEvidence.count(_.publicSafe) * 25.0),
      sourceFit = if claim.layer == ClaimLayer.SourceContext then clamp(60 + evidenceBonus) else 0.0,
      novelty = impact.novelty.toDouble,
      redundancyPenalty =
        if claim.suppressionHints.contains(SuppressionReason.DuplicateWeakerClaim) then 60.0 else 0.0,
      abstain = abstain,
      wordingCode = claim.wordingStrengthCap.ordinal
    )

  private def hasEvidence(claim: CommentaryClaim): Boolean =
    claim.evidenceRefs.nonEmpty || claim.lowerCarrierRefs.nonEmpty || claim.variationEvidence.nonEmpty

  private def hasRawEngineOnly(claim: CommentaryClaim): Boolean =
    claim.layer == ClaimLayer.Engine ||
      (claim.evidenceRefs ++ claim.lowerCarrierRefs).exists(_.kind == EvidenceRefKind.RawEngine)

  private def isSourceContextOnly(claim: CommentaryClaim): Boolean =
    claim.layer == ClaimLayer.SourceContext ||
      (claim.evidenceRefs ++ claim.lowerCarrierRefs).exists(_.kind == EvidenceRefKind.SourceContext)

  private def hasUnsafeVariationEvidence(claim: CommentaryClaim): Boolean =
    claim.variationEvidence.exists(proof => !PublicVariationEvidenceSafety.publicSafeForClaim(claim, proof))

  private def requiresExactBoardBound(claim: CommentaryClaim): Boolean =
    claim.layer != ClaimLayer.SourceContext && claim.layer != ClaimLayer.Engine && claim.layer != ClaimLayer.Renderer

  private def requiresBoundClaimBinding(claim: CommentaryClaim): Boolean =
    claim.layer == ClaimLayer.Certification || claim.layer == ClaimLayer.Delta || claim.layer == ClaimLayer.Projection

  private def statusCanEnterSemanticPublicSet(claim: CommentaryClaim): Boolean =
    claim.status == ClaimStatus.Admitted || (claim.layer == ClaimLayer.SourceContext && claim.status == ClaimStatus.Context)

  private def bindingCount(claim: CommentaryClaim): Int =
    Vector(claim.owner, claim.anchor, claim.route, claim.scope).count(_.exists(_.trim.nonEmpty))

  private def weighted(values: (Int, Double)*): Double =
    values.map((score, weight) => score * weight).sum

  private def clamp(value: Double): Double =
    math.max(0.0, math.min(100.0, value))

  private def stableBucket(value: String): Int =
    math.floorMod(value.hashCode, 10_000)

  private def strongerDecision(left: SemanticDecision, right: SemanticDecision): Boolean =
    if left.publicEligible != right.publicEligible then left.publicEligible
    else if left.leadScore != right.leadScore then left.leadScore > right.leadScore
    else left.candidate.claimId < right.candidate.claimId

  private def frameSlot(role: SemanticRole): Int =
    role match
      case SemanticRole.Lead => 0
      case SemanticRole.Support => 6
      case SemanticRole.Context => 10
      case SemanticRole.Suppressed | SemanticRole.Abstain => 11

  private def frameScore(role: SemanticRole): Double =
    role match
      case SemanticRole.Suppressed | SemanticRole.Abstain => 100.0
      case _ => 75.0

  private val suppressionSlot: Map[SuppressionReason, Int] =
    Map(
      SuppressionReason.SupportOnly -> 16,
      SuppressionReason.Deferred -> 17,
      SuppressionReason.ForbiddenShortcut -> 18,
      SuppressionReason.AntiCase -> 19,
      SuppressionReason.StaleEvidence -> 20,
      SuppressionReason.WrongOwner -> 21,
      SuppressionReason.WrongAnchor -> 22,
      SuppressionReason.WrongRoute -> 23,
      SuppressionReason.ScopeMismatch -> 24,
      SuppressionReason.NoBoardReason -> 25,
      SuppressionReason.RawEngineOnly -> 26,
      SuppressionReason.SourceContextOnly -> 27,
      SuppressionReason.AmbiguousTransposition -> 29,
      SuppressionReason.DuplicateWeakerClaim -> 30,
      SuppressionReason.RivalBand -> 30,
      SuppressionReason.RendererNotAllowed -> 31
    )

  private final case class HceScores(
      lead: Double,
      support: Double,
      context: Double,
      confidence: Double,
      salience: Double,
      specificity: Double,
      pedagogicalClarity: Double,
      tacticalUrgency: Double,
      strategicRelevance: Double,
      lineSupport: Double,
      sourceFit: Double,
      novelty: Double,
      redundancyPenalty: Double,
      abstain: Double,
      wordingCode: Int
  )
