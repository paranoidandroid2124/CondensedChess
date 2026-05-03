package lila.commentary.selection

import lila.commentary.projection.{
  StrategyProjectionBandId,
  StrategyProjectionEvidenceKind,
  StrategyProjectionPhraseCapability,
  StrategyProjectionScopeContract
}

enum ClaimLayer(val key: String):
  case Root extends ClaimLayer("root")
  case Witness extends ClaimLayer("witness")
  case Object extends ClaimLayer("object")
  case Delta extends ClaimLayer("delta")
  case Certification extends ClaimLayer("certification")
  case Projection extends ClaimLayer("projection")
  case Engine extends ClaimLayer("engine")
  case SourceContext extends ClaimLayer("source_context")
  case Renderer extends ClaimLayer("renderer")

enum ClaimStatus(val key: String):
  case Admitted extends ClaimStatus("admitted")
  case SupportOnly extends ClaimStatus("support_only")
  case Deferred extends ClaimStatus("deferred")
  case Rejected extends ClaimStatus("rejected")
  case AntiCase extends ClaimStatus("anti_case")
  case Context extends ClaimStatus("context")

enum ClaimBucket(val key: String):
  case MustLead extends ClaimBucket("mustLead")
  case ShouldLead extends ClaimBucket("shouldLead")
  case CanLead extends ClaimBucket("canLead")
  case Support extends ClaimBucket("support")
  case ContextOnly extends ClaimBucket("contextOnly")
  case Suppress extends ClaimBucket("suppress")

object ClaimBucket:
  def fromKey(key: String): Option[ClaimBucket] =
    values.find(_.key == key)

enum SuppressionReason(val key: String):
  case SupportOnly extends SuppressionReason("support_only")
  case Deferred extends SuppressionReason("deferred")
  case AntiCase extends SuppressionReason("anti_case")
  case StaleEvidence extends SuppressionReason("stale_evidence")
  case WrongOwner extends SuppressionReason("wrong_owner")
  case WrongAnchor extends SuppressionReason("wrong_anchor")
  case WrongRoute extends SuppressionReason("wrong_route")
  case ScopeMismatch extends SuppressionReason("scope_mismatch")
  case RivalBand extends SuppressionReason("rival_band")
  case ForbiddenShortcut extends SuppressionReason("forbidden_shortcut")
  case DuplicateWeakerClaim extends SuppressionReason("duplicate_weaker_claim")
  case SourceContextOnly extends SuppressionReason("source_context_only")
  case RawEngineOnly extends SuppressionReason("raw_engine_only")
  case NoBoardReason extends SuppressionReason("no_board_reason")
  case AmbiguousTransposition extends SuppressionReason("ambiguous_transposition")
  case RetrievalNonAuthoritative extends SuppressionReason("retrieval_non_authoritative")
  case RendererNotAllowed extends SuppressionReason("renderer_not_allowed")

object SuppressionReason:
  def fromKey(key: String): Option[SuppressionReason] =
    values.find(_.key == key)

enum WordingStrength(val key: String, val rank: Int):
  case Hidden extends WordingStrength("hidden", 0)
  case NegativeOnly extends WordingStrength("negative_only", 1)
  case ContextOnly extends WordingStrength("context_only", 2)
  case QualifiedSupport extends WordingStrength("qualified_support", 3)
  case AssertiveCertified extends WordingStrength("assertive_certified", 4)

object WordingStrength:
  def weaker(left: WordingStrength, right: WordingStrength): WordingStrength =
    if left.rank <= right.rank then left else right

enum EvidenceRefKind(val key: String):
  case ExactBoard extends EvidenceRefKind("exact_board")
  case Root extends EvidenceRefKind("root")
  case Witness extends EvidenceRefKind("witness")
  case Object extends EvidenceRefKind("object")
  case Delta extends EvidenceRefKind("delta")
  case Certification extends EvidenceRefKind("certification")
  case Projection extends EvidenceRefKind("projection")
  case EngineCertification extends EvidenceRefKind("engine_certification")
  case RawEngine extends EvidenceRefKind("raw_engine")
  case SourceContext extends EvidenceRefKind("source_context")

enum SourceContextKind(val key: String):
  case Opening extends SourceContextKind("opening")
  case Motif extends SourceContextKind("motif")
  case EndgameStudy extends SourceContextKind("endgameStudy")
  case Retrieval extends SourceContextKind("retrieval")

enum PlanAnnotationFrameKind(val key: String):
  case Opening extends PlanAnnotationFrameKind("opening")
  case Motif extends PlanAnnotationFrameKind("motif")
  case EndgameStudy extends PlanAnnotationFrameKind("endgameStudy")
  case Retrieval extends PlanAnnotationFrameKind("retrieval")

object PlanAnnotationFrameKind:
  def fromSourceContextKind(kind: SourceContextKind): PlanAnnotationFrameKind =
    kind match
      case SourceContextKind.Opening => Opening
      case SourceContextKind.Motif => Motif
      case SourceContextKind.EndgameStudy => EndgameStudy
      case SourceContextKind.Retrieval => Retrieval

enum PlanAnnotationStrength(val key: String):
  case Strong extends PlanAnnotationStrength("strong")

final case class PlanAnnotationFrame(
    kind: PlanAnnotationFrameKind,
    proofId: String,
    sourceRefIds: Vector[String]
):
  require(proofId.trim.nonEmpty, "Plan annotation frame proof id must be non-empty")
  require(sourceRefIds.nonEmpty, "Plan annotation frame source refs must be non-empty")
  require(sourceRefIds.forall(_.trim.nonEmpty), "Plan annotation frame source refs must be non-empty")

final case class PlanAnnotationSelection(
    claimId: String,
    primaryProofId: String,
    companionProofIds: Vector[String],
    supportProofIds: Vector[String],
    negativeProofIds: Vector[String],
    sourceFrames: Vector[PlanAnnotationFrame],
    strength: PlanAnnotationStrength,
    wordingCap: WordingStrength
):
  require(claimId.trim.nonEmpty, "Plan annotation claim id must be non-empty")
  require(primaryProofId.trim.nonEmpty, "Plan annotation primary proof id must be non-empty")
  require(companionProofIds.forall(_.trim.nonEmpty), "Plan annotation companion proof ids must be non-empty")
  require(supportProofIds.forall(_.trim.nonEmpty), "Plan annotation support proof ids must be non-empty")
  require(negativeProofIds.forall(_.trim.nonEmpty), "Plan annotation negative proof ids must be non-empty")

enum VariationMoveRole(val key: String):
  case GameMove extends VariationMoveRole("game_move")
  case CandidateMove extends VariationMoveRole("candidate_move")
  case DefenderResource extends VariationMoveRole("defender_resource")
  case Continuation extends VariationMoveRole("continuation")

enum VariationProofPurpose(val key: String):
  case Holds extends VariationProofPurpose("holds")
  case Fails extends VariationProofPurpose("fails")
  case ReleasesCounterplay extends VariationProofPurpose("releases_counterplay")
  case Simplifies extends VariationProofPurpose("simplifies")
  case PreservesPressure extends VariationProofPurpose("preserves_pressure")
  case DeniesResource extends VariationProofPurpose("denies_resource")

enum VariationEvidenceRole(val key: String):
  case DefenderResource extends VariationEvidenceRole("defender_resource")
  case FailedTemptingMove extends VariationEvidenceRole("failed_tempting_move")
  case ReleaseRisk extends VariationEvidenceRole("release_risk")
  case Hold extends VariationEvidenceRole("hold")
  case Conversion extends VariationEvidenceRole("conversion")
  case Persistence extends VariationEvidenceRole("persistence")
  case PrematureMove extends VariationEvidenceRole("premature_move")
  case Simplification extends VariationEvidenceRole("simplification")

enum VariationTestResult(val key: String):
  case ResourceWorks extends VariationTestResult("resource_works")
  case ResourceFails extends VariationTestResult("resource_fails")
  case ReleasesCounterplay extends VariationTestResult("releases_counterplay")
  case DoesNotRestoreCounterplay extends VariationTestResult("does_not_restore_counterplay")
  case DefensiveHold extends VariationTestResult("defensive_hold")
  case MovePremature extends VariationTestResult("move_premature")
  case Simplifies extends VariationTestResult("simplifies")
  case Converts extends VariationTestResult("converts")
  case PressurePersists extends VariationTestResult("pressure_persists")

enum VariationSurfaceAllowance(val key: String):
  case PublicLine extends VariationSurfaceAllowance("public_line")
  case BoundaryOnly extends VariationSurfaceAllowance("boundary_only")
  case InternalOnly extends VariationSurfaceAllowance("internal_only")

final case class VariationMove(
    san: String,
    uci: String
):
  require(san.trim.nonEmpty, "VariationMove SAN must be non-empty")
  require(uci.trim.nonEmpty, "VariationMove UCI must be non-empty")

final case class PreparedVariationBoundary(
    depthFloor: Int,
    realizedDepth: Int,
    multiPv: Int,
    freshnessChecked: Boolean,
    legalReplayChecked: Boolean,
    baselineChecked: Boolean
):
  def publicSafe: Boolean =
    depthFloor > 0 &&
      realizedDepth >= depthFloor &&
      multiPv > 0 &&
      freshnessChecked &&
      legalReplayChecked

final case class PreparedVariationDebug(
    variationHash: Option[String] = None,
    engineConfigFingerprint: Option[String] = None,
    rawPacketId: Option[String] = None,
    rawLineIndex: Option[Int] = None
)

final case class PreparedVariationEvidence(
    proofId: String,
    boundClaimId: String,
    startFen: String,
    owner: String,
    defender: Option[String],
    anchor: String,
    route: String,
    scope: String,
    role: VariationEvidenceRole = VariationEvidenceRole.Persistence,
    moveRole: VariationMoveRole,
    lineSan: Vector[String],
    lineUci: Vector[String],
    playedMove: Option[VariationMove] = None,
    candidateMove: Option[VariationMove] = None,
    defenderResource: Option[VariationMove] = None,
    continuation: Vector[VariationMove] = Vector.empty,
    testedMove: Option[VariationMove] = None,
    testedLine: Vector[VariationMove] = Vector.empty,
    replyLine: Vector[VariationMove] = Vector.empty,
    resourceLine: Vector[VariationMove] = Vector.empty,
    testResult: VariationTestResult = VariationTestResult.PressurePersists,
    proves: String,
    proofPurpose: VariationProofPurpose,
    provenanceRefs: Vector[EvidenceRef] = Vector.empty,
    boundary: PreparedVariationBoundary,
    wordingCap: WordingStrength,
    surfaceAllowance: VariationSurfaceAllowance = VariationSurfaceAllowance.PublicLine,
    publicSafe: Boolean,
    debug: Option[PreparedVariationDebug] = None
):
  require(proofId.trim.nonEmpty, "Prepared variation proof id must be non-empty")
  require(boundClaimId.trim.nonEmpty, "Prepared variation bound claim id must be non-empty")
  require(startFen.trim.nonEmpty, "Prepared variation start FEN must be non-empty")
  require(owner.trim.nonEmpty, "Prepared variation owner must be non-empty")
  require(anchor.trim.nonEmpty, "Prepared variation anchor must be non-empty")
  require(route.trim.nonEmpty, "Prepared variation route must be non-empty")
  require(scope.trim.nonEmpty, "Prepared variation scope must be non-empty")
  require(proves.trim.nonEmpty, "Prepared variation proof token must be non-empty")

  def withWordingCap(cap: WordingStrength): PreparedVariationEvidence =
    copy(wordingCap = WordingStrength.weaker(wordingCap, cap))

final case class EvidenceRef(
    kind: EvidenceRefKind,
    id: String,
    owner: Option[String] = None,
    anchor: Option[String] = None,
    route: Option[String] = None,
    scope: Option[String] = None
):
  require(id.trim.nonEmpty, "EvidenceRef id must be non-empty")

object EvidenceRef:
  def isPublicSafeProvenanceId(id: String): Boolean =
    val trimmed = id.trim
    trimmed.matches("^[A-Za-z0-9][A-Za-z0-9_-]*$") && !containsInternalProvenanceToken(trimmed)

  private def containsInternalProvenanceToken(id: String): Boolean =
    val normalized = id.toLowerCase.replace('-', '_').replace(':', '_').replace(' ', '_')
    Vector(
      "branch_id",
      "branchid",
      "parent_branch",
      "root_candidate",
      "candidate_probe",
      "cache_key",
      "cachekey",
      "probe_payload"
    ).exists(normalized.contains)

final case class ClaimImpact(
    resultMaterialImpact: Int = 0,
    forcedness: Int = 0,
    immediacy: Int = 0,
    persistenceAfterDefense: Int = 0,
    evidenceConfidence: Int = 0,
    evalSwing: Int = 0,
    boardExplainability: Int = 0,
    pedagogicalClarity: Int = 0,
    novelty: Int = 0
):
  private val scores = Vector(
    resultMaterialImpact,
    forcedness,
    immediacy,
    persistenceAfterDefense,
    evidenceConfidence,
    boardExplainability,
    pedagogicalClarity,
    novelty
  )
  require(scores.forall(score => score >= 0 && score <= 100), "ClaimImpact scores must be between 0 and 100")
  require(evalSwing >= 0, "ClaimImpact evalSwing must be non-negative")

  def leadRank(evalSwingAllowed: Boolean): Vector[Int] =
    Vector(
      resultMaterialImpact,
      forcedness,
      immediacy,
      persistenceAfterDefense,
      evidenceConfidence,
      if evalSwingAllowed then math.min(evalSwing, 100) else 0,
      boardExplainability,
      pedagogicalClarity,
      novelty
    )

final case class CommentaryClaim(
    id: String,
    layer: ClaimLayer,
    status: ClaimStatus,
    band: Option[String] = None,
    owner: Option[String] = None,
    beneficiary: Option[String] = None,
    defender: Option[String] = None,
    sideToMove: Option[String] = None,
    anchor: Option[String] = None,
    route: Option[String] = None,
    scope: Option[String] = None,
    impact: ClaimImpact = ClaimImpact(),
    evidenceRefs: Vector[EvidenceRef] = Vector.empty,
    lowerCarrierRefs: Vector[EvidenceRef] = Vector.empty,
    exactBoardBound: Boolean = false,
    wordingStrengthCap: WordingStrength = WordingStrength.QualifiedSupport,
    suppressionHints: Vector[SuppressionReason] = Vector.empty,
    sourceContextKind: Option[SourceContextKind] = None,
    variationEvidence: Vector[PreparedVariationEvidence] = Vector.empty,
    projectionPhraseCapability: Option[StrategyProjectionPhraseCapability] = None,
    projectionRuntimeKId: Option[String] = None,
    publicSurfaceForbiddenTerms: Vector[String] = Vector.empty
):
  require(id.trim.nonEmpty, "CommentaryClaim id must be non-empty")

final case class SelectedClaim(
    claim: CommentaryClaim,
    bucket: ClaimBucket,
    softReasons: Vector[SuppressionReason] = Vector.empty
)

final case class SuppressedClaim(
    claim: CommentaryClaim,
    reasons: Vector[SuppressionReason]
)

final case class CommentaryOutline(
    context: Vector[SelectedClaim],
    lead: Option[SelectedClaim],
    support: Vector[SelectedClaim],
    contrast: Vector[SelectedClaim],
    suppressedClaims: Vector[SuppressedClaim],
    evidenceRefs: Vector[EvidenceRef],
    variationEvidence: Vector[PreparedVariationEvidence],
    wordingStrengthCap: WordingStrength,
    annotationSelections: Vector[PlanAnnotationSelection] = Vector.empty
)

object ClaimSelector:

  def select(
      claims: Vector[CommentaryClaim],
      rendererRequestedCap: Option[WordingStrength] = None
  ): CommentaryOutline =
    LegacyClaimSelector.select(claims, rendererRequestedCap)

object LegacyClaimSelector:

  def select(
      claims: Vector[CommentaryClaim],
      rendererRequestedCap: Option[WordingStrength] = None
  ): CommentaryOutline =
    val classified = claims.map(claim => claim -> suppressionReasons(claim))
    val baseSuppressed =
      classified.collect { case (claim, reasons) if reasons.nonEmpty =>
        Option.when(!usableSourceContext(claim, reasons))(SuppressedClaim(claim, reasons.distinct))
      }
        .flatten
    val eligible =
      classified.collect { case (claim, reasons) if reasons.isEmpty && canSelectAsBoardClaim(claim) =>
        claim
      }
    val (deduped, duplicateSuppressed) = suppressWeakerDuplicates(eligible)
    val ordered = deduped.sortWith(strongerThan)
    val lead = ordered.headOption.map(claim => SelectedClaim(claim, bucketForLead(claim)))
    val (supportClaims, supportSuppressed) =
      lead match
        case Some(selectedLead) =>
          ordered.drop(1).foldLeft((Vector.empty[CommentaryClaim], Vector.empty[SuppressedClaim])):
            case ((kept, suppressed), claim) =>
              val reasons = supportRelationReasons(selectedLead.claim, claim)
              if reasons.isEmpty then (kept :+ claim, suppressed)
              else (kept, suppressed :+ SuppressedClaim(claim, reasons))
        case None => (Vector.empty, Vector.empty)
    val cap =
      lead
        .map(_.claim.wordingStrengthCap)
        .orElse(
          Option.when(
            classified.exists((claim, reasons) =>
              isContext(claim) &&
                usableSourceContext(claim, reasons) &&
                sourceContextLineTestsSatisfied(claim, lead.toVector.map(_.claim) ++ supportClaims)
            )
          )(WordingStrength.ContextOnly)
        )
        .getOrElse(WordingStrength.Hidden)
    val lineTestSuppressed =
      classified.collect {
        case (claim, reasons)
            if isContext(claim) &&
              usableSourceContext(claim, reasons) &&
              !sourceContextLineTestsSatisfied(claim, lead.toVector.map(_.claim) ++ supportClaims) =>
          SuppressedClaim(claim, Vector(SuppressionReason.ForbiddenShortcut, SuppressionReason.NoBoardReason))
      }
    val contextClaims =
      classified.collect { case (claim, reasons)
          if isContext(claim) &&
            usableSourceContext(claim, reasons) &&
            sourceContextLineTestsSatisfied(claim, lead.toVector.map(_.claim) ++ supportClaims) =>
        SelectedClaim(
          clampSelectedClaim(claim, WordingStrength.ContextOnly),
          if lead.isEmpty then ClaimBucket.ContextOnly else ClaimBucket.Support,
          softSourceReasons(reasons)
        )
      }
    val selectedLead =
      lead.map(selected => SelectedClaim(clampSelectedClaim(selected.claim, cap), selected.bucket))
    val support =
      supportClaims.map(claim => SelectedClaim(clampSelectedClaim(claim, cap), ClaimBucket.Support))
    val rendererSuppressed =
      rendererRequestedCap match
        case Some(requested) if requested.rank > cap.rank =>
          Vector(
            SuppressedClaim(
              CommentaryClaim(
                id = "renderer-wording-upgrade",
                layer = ClaimLayer.Renderer,
                status = ClaimStatus.Rejected,
                wordingStrengthCap = requested,
                suppressionHints = Vector(SuppressionReason.RendererNotAllowed)
              ),
              Vector(SuppressionReason.RendererNotAllowed)
            )
          )
        case _ => Vector.empty
    val outline =
      CommentaryOutline(
      context = contextClaims,
      lead = selectedLead,
      support = support,
      contrast = Vector.empty,
      suppressedClaims = baseSuppressed ++ lineTestSuppressed ++ duplicateSuppressed ++ supportSuppressed ++ rendererSuppressed,
      evidenceRefs = (selectedLead.toVector.flatMap(_.claim.evidenceRefs) ++ support.flatMap(_.claim.evidenceRefs) ++ contextClaims.flatMap(_.claim.evidenceRefs)).distinct,
      variationEvidence = Vector.empty,
      wordingStrengthCap = cap
    )
    val lineSelection = CandidateLineSelection.select(outline)
    outline.copy(
      variationEvidence = CandidateLineSelection.publicVariationEvidenceFor(lineSelection),
      annotationSelections = CandidateLineSelection.annotationSelectionsFor(lineSelection)
    )

  private def clampSelectedClaim(claim: CommentaryClaim, cap: WordingStrength): CommentaryClaim =
    val effectiveCap = WordingStrength.weaker(claim.wordingStrengthCap, cap)
    claim.copy(
      wordingStrengthCap = effectiveCap,
      variationEvidence = claim.variationEvidence.map(_.withWordingCap(effectiveCap))
    )

  private def suppressionReasons(claim: CommentaryClaim): Vector[SuppressionReason] =
    val statusReasons =
      claim.status match
        case ClaimStatus.SupportOnly => Vector(SuppressionReason.SupportOnly)
        case ClaimStatus.Deferred => Vector(SuppressionReason.Deferred)
        case ClaimStatus.AntiCase => Vector(SuppressionReason.AntiCase)
        case ClaimStatus.Rejected => Vector(SuppressionReason.ForbiddenShortcut)
        case _ => Vector.empty
    val layerReasons =
      claim.layer match
        case ClaimLayer.Engine => Vector(SuppressionReason.RawEngineOnly, SuppressionReason.NoBoardReason)
        case ClaimLayer.Renderer => Vector(SuppressionReason.RendererNotAllowed)
        case ClaimLayer.SourceContext =>
          sourceContextReasons(claim)
        case ClaimLayer.Projection =>
          projectionReasons(claim)
        case _ =>
          boardClaimReasons(claim)
    val admissibleHints =
      claim.suppressionHints.filterNot(_ == SuppressionReason.RivalBand)
    (statusReasons ++ layerReasons ++ variationEvidenceReasons(claim) ++ admissibleHints).distinct

  private def variationEvidenceReasons(claim: CommentaryClaim): Vector[SuppressionReason] =
    if claim.variationEvidence.isEmpty then Vector.empty
    else
      claim.layer match
        case ClaimLayer.SourceContext =>
          Vector(SuppressionReason.SourceContextOnly, SuppressionReason.ForbiddenShortcut, SuppressionReason.NoBoardReason)
        case ClaimLayer.Engine =>
          Vector(SuppressionReason.RawEngineOnly, SuppressionReason.NoBoardReason)
        case _ =>
          val unsafe = claim.variationEvidence.exists(proof => !publicSafeVariationEvidenceForClaim(claim, proof))
          if unsafe then Vector(SuppressionReason.RawEngineOnly, SuppressionReason.NoBoardReason)
          else if claim.variationEvidence.forall(negativeOnlyVariationEvidence) then Vector(SuppressionReason.SupportOnly)
          else Vector.empty

  private def publicSafeVariationEvidence(proof: PreparedVariationEvidence): Boolean =
    PublicVariationEvidenceSafety.publicSafe(proof)

  private def publicSafeVariationEvidenceForClaim(
      claim: CommentaryClaim,
      proof: PreparedVariationEvidence
  ): Boolean =
    PublicVariationEvidenceSafety.publicSafeForClaim(claim, proof)

  private def negativeOnlyVariationEvidence(proof: PreparedVariationEvidence): Boolean =
    proof.role == VariationEvidenceRole.FailedTemptingMove ||
      proof.role == VariationEvidenceRole.PrematureMove ||
      proof.role == VariationEvidenceRole.ReleaseRisk ||
      proof.testResult == VariationTestResult.MovePremature ||
      proof.testResult == VariationTestResult.ReleasesCounterplay ||
      proof.wordingCap == WordingStrength.NegativeOnly

  private def sourceContextReasons(claim: CommentaryClaim): Vector[SuppressionReason] =
    val base = Vector(SuppressionReason.SourceContextOnly)
    val familyReasons =
      claim.sourceContextKind match
        case Some(SourceContextKind.Opening) =>
          openingSourceContextReasons(claim)
        case Some(SourceContextKind.Motif) =>
          motifSourceContextReasons(claim)
        case Some(SourceContextKind.EndgameStudy) =>
          endgameStudySourceContextReasons(claim)
        case Some(SourceContextKind.Retrieval) =>
          retrievalSourceContextReasons(claim)
        case None =>
          Vector(SuppressionReason.ForbiddenShortcut)
    (base ++ familyReasons).distinct

  private def motifSourceContextReasons(claim: CommentaryClaim): Vector[SuppressionReason] =
    val forbiddenContext =
      (sourceContextIds(claim) ++ exactBoardIds(claim)).exists(containsForbiddenMotifContextToken)
    if hasSourceDetectorCarrier(claim) && !forbiddenContext then Vector.empty
    else Vector(SuppressionReason.ForbiddenShortcut, SuppressionReason.NoBoardReason)

  private def containsForbiddenMotifContextToken(id: String): Boolean =
    val normalized = normalizedToken(id)
    val forbiddenTokens = Set(
      "discovered_attack",
      "deflection",
      "back_rank",
      "back_rank_mate",
      "clearance",
      "interference",
      "truth",
      "claim",
      "best",
      "forced",
      "result",
      "engine",
      "oracle",
      "winning",
      "draw",
      "loss"
    )
    normalized.matches(".*(^|_)s[0-9]{2}(_|$).*") || forbiddenTokens.exists(normalized.contains)

  private def openingSourceContextReasons(claim: CommentaryClaim): Vector[SuppressionReason] =
    val sourceIds = sourceContextIds(claim)
    val hasCanonicalPosition =
      sourceIds.exists(id => id.startsWith("opening-position:") && id.endsWith(":canonical"))
    val ambiguous =
      sourceIds.exists(id => id.startsWith("opening-position:") && id.contains(":ambiguous"))
    val citationLeak =
      sourceIds.exists(containsOpeningSpecificCitationToken)
    val rankingMerge =
      sourceIds.exists(containsMergedOpeningRankingToken)
    val truthPromotion =
      sourceIds.exists(containsForbiddenOpeningContextToken)
    Vector(
      Option.when(ambiguous)(SuppressionReason.AmbiguousTransposition),
      Option.when(rankingMerge || truthPromotion || citationLeak || !hasCanonicalPosition)(SuppressionReason.ForbiddenShortcut),
      Option.when(rankingMerge || truthPromotion || citationLeak || !hasCanonicalPosition)(SuppressionReason.NoBoardReason)
    ).flatten

  private def containsMergedOpeningRankingToken(id: String): Boolean =
    val normalized = normalizedToken(id)
    normalized.contains("master_reference") && normalized.contains("online_trend")

  private def containsForbiddenOpeningContextToken(id: String): Boolean =
    val normalized = normalizedToken(id)
    Vector(
      "best",
      "recommend",
      "recommendation",
      "theory",
      "truth",
      "current_position_proof",
      "current_position_truth",
      "forced",
      "result",
      "engine",
      "oracle",
      "winning",
      "drawing",
      "wdl",
      "dtz",
      "dtm",
      "pipeline_smoke",
      "taxonomy_reference",
      "draw_offer",
      "repetition",
      "tournament",
      "rating",
      "time_control",
      "game_context",
      "play_environment"
    ).exists(normalized.contains)

  private def containsOpeningSpecificCitationToken(id: String): Boolean =
    val normalized = normalizedToken(id)
    val citationTokens = Vector(
      "game_id",
      "gameid",
      "gameurl",
      "game_url",
      "player",
      "playerurl",
      "player_url",
      "event",
      "eventurl",
      "event_url"
    )
    normalized.contains("http://") ||
      normalized.contains("https://") ||
      citationTokens.exists(normalized.contains)

  private def endgameStudySourceContextReasons(claim: CommentaryClaim): Vector[SuppressionReason] =
    val sourceIds = sourceContextIds(claim)
    val studyIds =
      sourceIds.collect:
        case id if id.startsWith("endgame-study:") && id.endsWith(":applicable") =>
          id.stripPrefix("endgame-study:").stripSuffix(":applicable")
    val applicabilityIds =
      exactBoardRefs(claim).collect:
        case ref if ref.id.startsWith("endgame-study-applicability:") =>
          ref.id.stripPrefix("endgame-study-applicability:") -> ref.route
    val hasApplicability =
      studyIds.exists(studyId =>
        applicabilityIds.exists((id, route) =>
          route.contains(studyId) &&
            exactBoardRefs(claim).exists(ref =>
              ref.id == s"endgame-study-applicability:$id" &&
                ref.scope.contains("exact_endgame_applicability")
            )
        )
      )
    val resultLanguage =
      sourceIds.exists(containsForbiddenEndgameContextToken)
    Vector(
      Option.when(resultLanguage || !hasApplicability)(SuppressionReason.ForbiddenShortcut),
      Option.when(resultLanguage || !hasApplicability)(SuppressionReason.NoBoardReason)
    ).flatten

  private def retrievalSourceContextReasons(claim: CommentaryClaim): Vector[SuppressionReason] =
    val sourceIds = sourceContextIds(claim)
    val hasRetrievalExample =
      sourceIds.exists(_.startsWith("retrieval-example:"))
    val truthPromotion =
      sourceIds.exists(containsForbiddenRetrievalContextToken)
    Vector(
      Some(SuppressionReason.RetrievalNonAuthoritative),
      Option.when(truthPromotion || !hasRetrievalExample)(SuppressionReason.ForbiddenShortcut),
      Option.when(truthPromotion || !hasRetrievalExample)(SuppressionReason.NoBoardReason)
    ).flatten

  private def containsForbiddenEndgameContextToken(id: String): Boolean =
    val normalized = normalizedToken(id)
    Vector(
      "win",
      "draw",
      "loss",
      "result",
      "oracle",
      "wdl",
      "dtz",
      "dtm",
      "forced",
      "conversion",
      "tablebase"
    ).exists(normalized.contains) ||
      Set(
        "outside_passer",
        "fortress_pattern",
        "rook_on_seventh",
        "triangulation",
        "corresponding_squares",
        "shouldering",
        "breakthrough",
        "reserve_tempo"
      ).exists(normalized.contains)

  private def containsForbiddenRetrievalContextToken(id: String): Boolean =
    val normalized = normalizedToken(id)
    Vector(
      "current_position_truth",
      "current_position_proof",
      "truth_promotion",
      "truth",
      "recommendation",
      "recommend",
      "verdict",
      "game_result",
      "result_metadata",
      "display_candidate",
      "display_player",
      "display_event",
      "display_result",
      "famous_player"
    ).exists(normalized.contains)

  private def sourceContextLineTestsSatisfied(
      claim: CommentaryClaim,
      selectedBoardClaims: Vector[CommentaryClaim]
  ): Boolean =
    val lineTestRefs = sourceContextIds(claim).filter(isLineTestRef)
    val linkedProofIds = lineTestRefs.flatMap(ref => lineTestProofId(ref, claim.sourceContextKind))
    lineTestRefs.isEmpty || {
      val publicProofIds =
        selectedBoardClaims
          .flatMap(_.variationEvidence)
          .filter(publicSafeVariationEvidence)
          .map(_.proofId)
          .toSet
      linkedProofIds.size == lineTestRefs.size && linkedProofIds.forall(publicProofIds.contains)
    }

  private def isLineTestRef(ref: String): Boolean =
    PublicVariationEvidenceSafety.isAllowedLineTestRef(ref) ||
      ref.split(":").headOption.exists(_.endsWith("-line-test"))

  private def lineTestProofId(ref: String, kind: Option[SourceContextKind]): Option[String] =
    kind.flatMap(PublicVariationEvidenceSafety.lineTestProofIdForKind(ref, _))

  private def normalizedToken(value: String): String =
    value.toLowerCase.replace('-', '_').replace(':', '_').replace(' ', '_').replace('+', '_')

  private def hasSourceDetectorCarrier(claim: CommentaryClaim): Boolean =
    val motifIds =
      sourceContextIds(claim).collect:
        case id if id.startsWith("motif-example:") =>
          id.stripPrefix("motif-example:")
      .filterNot(_.contains(":"))
    val carrierIds =
      exactBoardIds(claim).collect:
        case id if id.startsWith("motif-detector-carrier:") =>
          id.stripPrefix("motif-detector-carrier:")
    motifIds.exists(motifId => carrierIds.contains(motifId))

  private def sourceContextIds(claim: CommentaryClaim): Vector[String] =
    (claim.evidenceRefs ++ claim.lowerCarrierRefs).collect:
      case ref if ref.kind == EvidenceRefKind.SourceContext => ref.id

  private def exactBoardIds(claim: CommentaryClaim): Vector[String] =
    (claim.evidenceRefs ++ claim.lowerCarrierRefs).collect:
      case ref if ref.kind == EvidenceRefKind.ExactBoard => ref.id

  private def exactBoardRefs(claim: CommentaryClaim): Vector[EvidenceRef] =
    (claim.evidenceRefs ++ claim.lowerCarrierRefs).collect:
      case ref if ref.kind == EvidenceRefKind.ExactBoard => ref

  private def projectionReasons(claim: CommentaryClaim): Vector[SuppressionReason] =
    val exactReasons = boardClaimReasons(claim)
    val lowerReasons =
      if claim.lowerCarrierRefs.nonEmpty then Vector.empty
      else Vector(SuppressionReason.ForbiddenShortcut, SuppressionReason.NoBoardReason)
    val lowerCarrierReasons =
      projectionLowerCarrierReasons(claim)
    val evidenceReasons =
      claim.band match
        case Some(band) =>
          val bindingReasons = projectionEvidenceBindingReasons(claim, band)
          val missingReasons =
            if projectionEvidenceAllowed(claim, band) then Vector.empty
            else Vector(SuppressionReason.ForbiddenShortcut)
          (bindingReasons ++ missingReasons).distinct
        case None => Vector(SuppressionReason.ForbiddenShortcut)
    val bandCarrierReasons =
      claim.band match
        case Some(band) if band == StrategyProjectionScopeContract.S01.value =>
          kingWingStormCarrierReasons(claim)
        case Some(band) if band == StrategyProjectionScopeContract.S02.value =>
          kingRingConcentrationCarrierReasons(claim)
        case Some(band) if band == StrategyProjectionScopeContract.S03.value =>
          diagonalKingAttackCarrierReasons(claim)
        case Some(band) if band == StrategyProjectionScopeContract.S04.value =>
          kingShelterBreachCarrierReasons(claim)
        case Some(band) if band == StrategyProjectionScopeContract.S05.value =>
          centerReleaseCarrierReasons(claim)
        case Some(band) if band == StrategyProjectionScopeContract.S06.value =>
          spaceBindCarrierReasons(claim)
        case Some(band) if band == StrategyProjectionScopeContract.S07.value =>
          initiativeConversionCarrierReasons(claim)
        case Some(band) if band == StrategyProjectionScopeContract.S08.value =>
          counterplayDenialCarrierReasons(claim)
        case Some(band) if band == StrategyProjectionScopeContract.S09.value =>
          filePenetrationCarrierReasons(claim)
        case Some(band) if band == StrategyProjectionScopeContract.S10.value =>
          outpostOccupationCarrierReasons(claim)
        case Some(band) if band == StrategyProjectionScopeContract.S11.value =>
          weakPawnPressureCarrierReasons(claim)
        case Some(band) if band == StrategyProjectionScopeContract.S12.value =>
          localAccessCarrierReasons(claim)
        case Some(band) if band == StrategyProjectionScopeContract.S13.value =>
          wingDamageCarrierReasons(claim)
        case Some(band) if band == StrategyProjectionScopeContract.S14.value =>
          chainBaseCarrierReasons(claim)
        case Some(band) if band == StrategyProjectionScopeContract.S15.value =>
          passerCreationCarrierReasons(claim)
        case Some(band) if band == StrategyProjectionScopeContract.S16.value =>
          passerSuppressionCarrierReasons(claim)
        case Some(band) if band == StrategyProjectionScopeContract.S17.value =>
          liabilityReliefCarrierReasons(claim)
        case Some(band) if band == StrategyProjectionScopeContract.S18.value =>
          bishopPairConversionCarrierReasons(claim)
        case Some(band) if band == StrategyProjectionScopeContract.S19.value =>
          simplificationCarrierReasons(claim)
        case Some(band) if band == StrategyProjectionScopeContract.S20.value =>
          mobilityDominationCarrierReasons(claim)
        case Some(band) if band == StrategyProjectionScopeContract.S21.value =>
          counterplaySurvivalCarrierReasons(claim)
        case Some(band) if band == StrategyProjectionScopeContract.S22.value =>
          holdCarrierReasons(claim)
        case Some(band) if band == StrategyProjectionScopeContract.S23.value =>
          kingActivityCarrierReasons(claim)
        case Some(band) if band == StrategyProjectionScopeContract.S24.value =>
          s24PublicClosedReasons(claim)
        case Some(band) if band == StrategyProjectionScopeContract.S25.value =>
          rankAccessCarrierReasons(claim)
        case Some(_) => Vector(SuppressionReason.ForbiddenShortcut)
        case None => Vector.empty
    (exactReasons ++ lowerReasons ++ lowerCarrierReasons ++ evidenceReasons ++ bandCarrierReasons).distinct

  private def boardClaimReasons(claim: CommentaryClaim): Vector[SuppressionReason] =
    val directReasons = Vector(
      Option.when(!claim.exactBoardBound)(SuppressionReason.StaleEvidence),
      Option.when(claim.owner.exists(_.trim.isEmpty) || claim.owner.isEmpty && requiresOwner(claim))(SuppressionReason.WrongOwner),
      Option.when(claim.beneficiary.exists(_.trim.isEmpty) || claim.beneficiary.isEmpty && requiresBeneficiary(claim))(SuppressionReason.WrongOwner),
      Option.when(claim.sideToMove.exists(_.trim.isEmpty) || claim.sideToMove.isEmpty && requiresSideToMove(claim))(SuppressionReason.ScopeMismatch),
      Option.when(claim.defender.exists(_.trim.isEmpty) || claim.defender.isEmpty && requiresDefender(claim))(SuppressionReason.WrongRoute),
      Option.when(
        claim.band.contains(StrategyProjectionScopeContract.S16.value) &&
          claim.owner.nonEmpty &&
          claim.owner == claim.defender
      )(SuppressionReason.WrongOwner),
      Option.when(
        isKingAttackBand(claim) &&
          claim.owner.nonEmpty &&
          claim.owner == claim.defender
      )(SuppressionReason.WrongOwner),
      Option.when(claim.anchor.exists(_.trim.isEmpty) || claim.anchor.isEmpty && requiresAnchor(claim))(SuppressionReason.WrongAnchor),
      Option.when(claim.route.exists(_.trim.isEmpty) || claim.route.isEmpty && requiresRoute(claim))(SuppressionReason.WrongRoute),
      Option.when(claim.scope.exists(_.trim.isEmpty) || claim.scope.isEmpty && requiresScope(claim))(SuppressionReason.ScopeMismatch),
      Option.when(claim.layer == ClaimLayer.Object && claim.route.contains("tactical_liability"))(SuppressionReason.ForbiddenShortcut),
      Option.when(claim.evidenceRefs.isEmpty)(SuppressionReason.NoBoardReason)
    ).flatten
    (directReasons ++
      evidenceRefReasons(claim, claim.evidenceRefs) ++
      moveLocalLooseCarrierReasons(claim) ++
      boardClaimLowerCarrierShortcutReasons(claim) ++
      engineCertificationBoundaryReasons(claim)).distinct

  private def moveLocalLooseCarrierReasons(claim: CommentaryClaim): Vector[SuppressionReason] =
    if claim.layer == ClaimLayer.Delta && claim.route.contains("moved_piece_left_loose") then
      val hasTransitionEvidence =
        claim.evidenceRefs.exists(ref =>
          ref.kind == EvidenceRefKind.Delta &&
            ref.id == "moved_piece_left_loose_transition" &&
            sameClaimBinding(claim, ref)
        )
      val hasBoundLooseRoot =
        claim.lowerCarrierRefs.exists(ref =>
          ref.kind == EvidenceRefKind.Root &&
            ref.id == "loose_piece" &&
            sameClaimBinding(claim, ref)
        )
      val hasImmediateCaptureCarrier =
        claim.lowerCarrierRefs.exists(ref =>
          ref.kind == EvidenceRefKind.Object &&
            ref.id == "immediate_capture" &&
            sameClaimBinding(claim, ref)
        )
      Vector(
        Option.when(!hasTransitionEvidence)(SuppressionReason.ForbiddenShortcut),
        Option.when(!hasTransitionEvidence)(SuppressionReason.NoBoardReason),
        Option.when(!hasBoundLooseRoot)(SuppressionReason.ForbiddenShortcut),
        Option.when(!hasBoundLooseRoot)(SuppressionReason.NoBoardReason),
        Option.when(!hasImmediateCaptureCarrier)(SuppressionReason.ForbiddenShortcut),
        Option.when(!hasImmediateCaptureCarrier)(SuppressionReason.NoBoardReason)
      ).flatten
    else Vector.empty

  private def boardClaimLowerCarrierShortcutReasons(claim: CommentaryClaim): Vector[SuppressionReason] =
    claim.lowerCarrierRefs.flatMap: ref =>
      Vector(
        Option.when(ref.kind == EvidenceRefKind.RawEngine)(SuppressionReason.RawEngineOnly),
        Option.when(ref.kind == EvidenceRefKind.RawEngine)(SuppressionReason.NoBoardReason),
        Option.when(ref.kind == EvidenceRefKind.SourceContext)(SuppressionReason.SourceContextOnly),
        Option.when(ref.kind == EvidenceRefKind.SourceContext)(SuppressionReason.NoBoardReason),
        Option.when(ref.kind == EvidenceRefKind.EngineCertification)(SuppressionReason.ForbiddenShortcut)
      ).flatten

  private def engineCertificationBoundaryReasons(claim: CommentaryClaim): Vector[SuppressionReason] =
    val engineRefs = claim.evidenceRefs.filter(_.kind == EvidenceRefKind.EngineCertification)
    if engineRefs.isEmpty then Vector.empty
    else
      val hasSameRootCertification =
        claim.evidenceRefs.exists(ref =>
          ref.kind == EvidenceRefKind.Certification &&
            boundedClaimBinding(claim, ref)
        )
      val unboundEngineReasons =
        engineRefs.flatMap: ref =>
          Vector(
            Option.when(ref.owner.isEmpty || claim.owner.isEmpty || ref.owner != claim.owner)(SuppressionReason.WrongOwner),
            Option.when(ref.anchor.isEmpty || claim.anchor.isEmpty || ref.anchor != claim.anchor)(SuppressionReason.WrongAnchor),
            Option.when(ref.route.isEmpty || claim.route.isEmpty || ref.route != claim.route)(SuppressionReason.WrongRoute),
            Option.when(ref.scope.isEmpty || claim.scope.isEmpty || ref.scope != claim.scope)(SuppressionReason.ScopeMismatch)
          ).flatten
      val missingRootReasons =
        if hasSameRootCertification then Vector.empty
        else Vector(SuppressionReason.ForbiddenShortcut, SuppressionReason.NoBoardReason)
      val missingBoardReason =
        if hasEngineCertifiedBoardReason(claim) then Vector.empty
        else Vector(SuppressionReason.NoBoardReason)
      (unboundEngineReasons ++ missingRootReasons ++ missingBoardReason).distinct

  private def hasEngineCertifiedBoardReason(claim: CommentaryClaim): Boolean =
    val boardReasonKinds = Set(
      EvidenceRefKind.Root,
      EvidenceRefKind.Witness,
      EvidenceRefKind.Object,
      EvidenceRefKind.Delta
    )
    (claim.evidenceRefs ++ claim.lowerCarrierRefs).exists(ref =>
      boardReasonKinds.contains(ref.kind) && boundedClaimBinding(claim, ref)
    )

  private def evidenceRefReasons(
      claim: CommentaryClaim,
      refs: Vector[EvidenceRef]
  ): Vector[SuppressionReason] =
    refs.flatMap: ref =>
      Vector(
        Option.when(ref.kind == EvidenceRefKind.RawEngine)(SuppressionReason.RawEngineOnly),
        Option.when(ref.kind == EvidenceRefKind.RawEngine)(SuppressionReason.NoBoardReason),
        Option.when(ref.kind == EvidenceRefKind.EngineCertification && claim.layer != ClaimLayer.Certification)(SuppressionReason.ForbiddenShortcut),
        Option.when(ref.kind == EvidenceRefKind.SourceContext && claim.layer != ClaimLayer.SourceContext)(SuppressionReason.SourceContextOnly),
        Option.when(ref.kind == EvidenceRefKind.SourceContext && claim.layer != ClaimLayer.SourceContext)(SuppressionReason.NoBoardReason),
        Option.when(ref.owner.exists(owner => claim.owner.forall(_ != owner)))(SuppressionReason.WrongOwner),
        Option.when(ref.anchor.exists(anchor => claim.anchor.forall(_ != anchor)))(SuppressionReason.WrongAnchor),
        Option.when(ref.route.exists(route => claim.route.forall(_ != route)))(SuppressionReason.WrongRoute),
        Option.when(ref.scope.exists(scope => claim.scope.forall(_ != scope)))(SuppressionReason.ScopeMismatch)
      ).flatten

  private def projectionLowerCarrierReasons(claim: CommentaryClaim): Vector[SuppressionReason] =
    val allowedLowerKinds = Set(
      EvidenceRefKind.ExactBoard,
      EvidenceRefKind.Root,
      EvidenceRefKind.Witness,
      EvidenceRefKind.Object,
      EvidenceRefKind.Delta,
      EvidenceRefKind.Certification
    )
    claim.lowerCarrierRefs.flatMap: ref =>
      val kindReasons =
        if allowedLowerKinds.contains(ref.kind) then Vector.empty
        else Vector(SuppressionReason.ForbiddenShortcut)
      val rawReasons =
        if ref.kind == EvidenceRefKind.RawEngine then Vector(SuppressionReason.RawEngineOnly, SuppressionReason.NoBoardReason)
        else Vector.empty
      val bindingReasons = Vector(
        Option.when(ref.owner.isEmpty)(SuppressionReason.WrongOwner),
        Option.when(ref.anchor.isEmpty)(SuppressionReason.WrongAnchor),
        Option.when(ref.route.isEmpty)(SuppressionReason.WrongRoute),
        Option.when(ref.scope.isEmpty)(SuppressionReason.ScopeMismatch)
      ).flatten
      val relationReasons =
        if claim.band.contains(StrategyProjectionScopeContract.S16.value) && ref.id == "passed_pawn_entity_state" then
          enemyPasserCarrierReasons(claim, ref)
        else if claim.band.contains(StrategyProjectionScopeContract.S04.value) && ref.id == "KingSafetyShell" then
          defenderOwnedCarrierReasons(claim, ref)
        else evidenceRefReasons(claim, Vector(ref))
      kindReasons ++ rawReasons ++ bindingReasons ++ relationReasons

  private def kingWingStormCarrierReasons(claim: CommentaryClaim): Vector[SuppressionReason] =
    val allowedRoutes = Set("same_wing_contact", "attack_edge_same_king")
    val routeReasons =
      if claim.route.exists(allowedRoutes.contains) then Vector.empty
      else Vector(SuppressionReason.WrongRoute, SuppressionReason.ForbiddenShortcut, SuppressionReason.NoBoardReason)
    val hasLever =
      claim.lowerCarrierRefs.exists(ref =>
        ref.kind == EvidenceRefKind.Witness &&
          ref.id == "available_lever_trigger" &&
          sameClaimBinding(claim, ref)
      )
    val hasContactSource =
      claim.lowerCarrierRefs.exists(ref =>
        ref.kind == EvidenceRefKind.Witness &&
          ref.id == "pawn_push_break_contact_source" &&
          sameClaimBinding(claim, ref)
      )
    val hasAttackScaffold =
      claim.lowerCarrierRefs.exists(ref =>
        ref.kind == EvidenceRefKind.Object &&
          ref.id == "AttackScaffold" &&
          sameClaimBinding(claim, ref)
      )
    val hasSafetyCertification =
      claim.lowerCarrierRefs.exists(ref =>
        ref.kind == EvidenceRefKind.Certification &&
          ref.id == "CertifiedKingSafetyEdge" &&
          sameClaimBinding(claim, ref)
      )
    val carrierReasons =
      if hasLever && hasContactSource && hasAttackScaffold && hasSafetyCertification then Vector.empty
      else Vector(SuppressionReason.ForbiddenShortcut, SuppressionReason.NoBoardReason)
    (routeReasons ++ carrierReasons).distinct

  private def kingRingConcentrationCarrierReasons(claim: CommentaryClaim): Vector[SuppressionReason] =
    val allowedRoutes = Set("direct_piece_concentration", "lane_strengthened_concentration")
    val routeReasons =
      if claim.route.exists(allowedRoutes.contains) then Vector.empty
      else Vector(SuppressionReason.WrongRoute, SuppressionReason.ForbiddenShortcut, SuppressionReason.NoBoardReason)
    val hasAttackScaffold =
      claim.lowerCarrierRefs.exists(ref =>
        ref.kind == EvidenceRefKind.Object &&
          ref.id == "AttackScaffold" &&
          sameClaimBinding(claim, ref)
      )
    val hasSafetyCertification =
      claim.lowerCarrierRefs.exists(ref =>
        ref.kind == EvidenceRefKind.Certification &&
          ref.id == "CertifiedKingSafetyEdge" &&
          sameClaimBinding(claim, ref)
      )
    val carrierReasons =
      if hasAttackScaffold && hasSafetyCertification then Vector.empty
      else Vector(SuppressionReason.ForbiddenShortcut, SuppressionReason.NoBoardReason)
    (routeReasons ++ carrierReasons).distinct

  private def diagonalKingAttackCarrierReasons(claim: CommentaryClaim): Vector[SuppressionReason] =
    val allowedRoutes = Set("king_facing_diagonal_entry", "fragility_linked_diagonal")
    val routeReasons =
      if claim.route.exists(allowedRoutes.contains) then Vector.empty
      else Vector(SuppressionReason.WrongRoute, SuppressionReason.ForbiddenShortcut, SuppressionReason.NoBoardReason)
    val hasDiagonalLane =
      claim.lowerCarrierRefs.exists(ref =>
        ref.kind == EvidenceRefKind.Witness &&
          ref.id == "diagonal_lane_only" &&
          sameClaimBinding(claim, ref)
      )
    val hasAttackScaffold =
      claim.lowerCarrierRefs.exists(ref =>
        ref.kind == EvidenceRefKind.Object &&
          ref.id == "AttackScaffold" &&
          sameClaimBinding(claim, ref)
      )
    val hasComparativeFragility =
      claim.lowerCarrierRefs.exists(ref =>
        ref.kind == EvidenceRefKind.Certification &&
          ref.id == "ComparativeKingFragility" &&
          sameClaimBinding(claim, ref)
      )
    val hasSafetyCertification =
      claim.lowerCarrierRefs.exists(ref =>
        ref.kind == EvidenceRefKind.Certification &&
          ref.id == "CertifiedKingSafetyEdge" &&
          sameClaimBinding(claim, ref)
      )
    val carrierReasons =
      if hasDiagonalLane && hasAttackScaffold && hasComparativeFragility && hasSafetyCertification then Vector.empty
      else Vector(SuppressionReason.ForbiddenShortcut, SuppressionReason.NoBoardReason)
    (routeReasons ++ carrierReasons).distinct

  private def kingShelterBreachCarrierReasons(claim: CommentaryClaim): Vector[SuppressionReason] =
    val allowedRoutes = Set("shell_payload_breach", "support_break_breach")
    val routeReasons =
      if claim.route.exists(allowedRoutes.contains) then Vector.empty
      else Vector(SuppressionReason.WrongRoute, SuppressionReason.ForbiddenShortcut, SuppressionReason.NoBoardReason)
    val hasKingSafetyShell =
      claim.lowerCarrierRefs.exists(ref =>
        ref.kind == EvidenceRefKind.Object &&
          ref.id == "KingSafetyShell" &&
          sameDefenderBinding(claim, ref)
      )
    val hasSafetyCertification =
      claim.lowerCarrierRefs.exists(ref =>
        ref.kind == EvidenceRefKind.Certification &&
          ref.id == "CertifiedKingSafetyEdge" &&
          sameClaimBinding(claim, ref)
      )
    val hasSupportBreakCarrier =
      claim.route match
        case Some("support_break_breach") =>
          claim.lowerCarrierRefs.exists(ref =>
            ref.kind == EvidenceRefKind.Witness &&
              ref.id == "diagonal_lane_only" &&
              sameClaimBinding(claim, ref)
          )
        case _ => true
    val carrierReasons =
      if hasKingSafetyShell && hasSafetyCertification && hasSupportBreakCarrier then Vector.empty
      else Vector(SuppressionReason.ForbiddenShortcut, SuppressionReason.NoBoardReason)
    (routeReasons ++ carrierReasons).distinct

  private def centerReleaseCarrierReasons(claim: CommentaryClaim): Vector[SuppressionReason] =
    requiredCarrierReasons(
      claim,
      allowedRoutes = Set("center_pawn_target", "central_axis_continuation"),
      required = Vector(
        EvidenceRefKind.Witness -> "available_lever_trigger",
        EvidenceRefKind.Witness -> "pawn_push_break_contact_source"
      )
    )

  private def spaceBindCarrierReasons(claim: CommentaryClaim): Vector[SuppressionReason] =
    requiredCarrierReasons(
      claim,
      allowedRoutes = Set("outpost_anchor", "non_outpost_space_bind"),
      required = Vector(
        EvidenceRefKind.Witness -> "structural_space_claim",
        EvidenceRefKind.Certification -> "SpaceBindRestrictionCertification"
      ),
      anyOf = Vector(
        Vector(EvidenceRefKind.Witness -> "knight_on_outpost_square"),
        Vector(EvidenceRefKind.Witness -> "short_run_slider_gate_restriction")
      )
    )

  private def initiativeConversionCarrierReasons(claim: CommentaryClaim): Vector[SuppressionReason] =
    requiredCarrierReasons(
      claim,
      allowedRoutes = Set("development_led_window", "move_right_window"),
      required = Vector(
        EvidenceRefKind.Object -> "OpeningDevelopmentRegime",
        EvidenceRefKind.Certification -> "DevelopmentComparison",
        EvidenceRefKind.Certification -> "InitiativeWindow"
      )
    )

  private def counterplayDenialCarrierReasons(claim: CommentaryClaim): Vector[SuppressionReason] =
    requiredCarrierReasons(
      claim,
      allowedRoutes = Set("rival_break_source_suppressed", "rival_counterplay_source_suppressed"),
      required = Vector(
        EvidenceRefKind.Certification -> "InitiativeWindow",
        EvidenceRefKind.Witness -> "pawn_push_break_contact_source"
      )
    )

  private def filePenetrationCarrierReasons(claim: CommentaryClaim): Vector[SuppressionReason] =
    requiredCarrierReasons(
      claim,
      allowedRoutes = Set("open_file_entry", "semi_open_file_entry", "same_file_penetration"),
      required = Vector(EvidenceRefKind.Witness -> "file_lane_state")
    )

  private def outpostOccupationCarrierReasons(claim: CommentaryClaim): Vector[SuppressionReason] =
    requiredCarrierReasons(
      claim,
      allowedRoutes = Set("knight_only_outpost_occupancy", "same_anchor_eviction_denial"),
      required = Vector(
        EvidenceRefKind.Witness -> "weak_outpost_square_state",
        EvidenceRefKind.Witness -> "knight_on_outpost_square"
      )
    )

  private def weakPawnPressureCarrierReasons(claim: CommentaryClaim): Vector[SuppressionReason] =
    requiredCarrierReasons(
      claim,
      allowedRoutes = Set("same_target_fixation", "same_target_repeated_pressure"),
      required = Vector(EvidenceRefKind.Witness -> "weak_pawn_target_state")
    )

  private def localAccessCarrierReasons(claim: CommentaryClaim): Vector[SuppressionReason] =
    requiredCarrierReasons(
      claim,
      allowedRoutes = Set("weak_square_route", "diagonal_lane_route"),
      required = Vector(EvidenceRefKind.Witness -> "short_run_slider_gate_restriction"),
      anyOf = Vector(
        Vector(EvidenceRefKind.Witness -> "weak_outpost_square_state"),
        Vector(EvidenceRefKind.Witness -> "diagonal_lane_only")
      )
    )

  private def wingDamageCarrierReasons(claim: CommentaryClaim): Vector[SuppressionReason] =
    requiredCarrierReasons(
      claim,
      allowedRoutes = Set("phalanx_edge_target", "structurally_burdened_target"),
      required = Vector(
        EvidenceRefKind.Witness -> "sector_asymmetry_state",
        EvidenceRefKind.Witness -> "available_lever_trigger",
        EvidenceRefKind.Witness -> "pawn_push_break_contact_source"
      )
    )

  private def chainBaseCarrierReasons(claim: CommentaryClaim): Vector[SuppressionReason] =
    requiredCarrierReasons(
      claim,
      allowedRoutes = Set("chain_base_target", "base_contact_continuation"),
      required = Vector(
        EvidenceRefKind.Witness -> "available_lever_trigger",
        EvidenceRefKind.Witness -> "pawn_push_break_contact_source"
      )
    )

  private def passerCreationCarrierReasons(claim: CommentaryClaim): Vector[SuppressionReason] =
    val allowedRoutes = Set("s13_wing_damage", "s14_chain_base")
    val routeReasons =
      if claim.route.exists(allowedRoutes.contains) then Vector.empty
      else Vector(SuppressionReason.WrongRoute, SuppressionReason.ForbiddenShortcut, SuppressionReason.NoBoardReason)
    val hasCandidatePasser =
      claim.lowerCarrierRefs.exists(ref =>
        ref.kind == EvidenceRefKind.Root &&
          ref.id == "candidate_passer" &&
          sameClaimBinding(claim, ref)
      )
    val hasSameCandidateRoute =
      claim.route.exists: route =>
        claim.lowerCarrierRefs.exists(ref =>
          ref.kind == EvidenceRefKind.Witness &&
            ref.id == s"same_candidate_${route}_creation_route" &&
            sameClaimBinding(claim, ref)
        )
    val carrierReasons =
      if hasCandidatePasser && hasSameCandidateRoute then Vector.empty
      else Vector(SuppressionReason.ForbiddenShortcut, SuppressionReason.NoBoardReason)
    (routeReasons ++ carrierReasons).distinct

  private def passerSuppressionCarrierReasons(claim: CommentaryClaim): Vector[SuppressionReason] =
    val allowedRoutes = Set("blockade_hold", "restriction_hold", "non_losing_race")
    val routeReasons =
      if claim.route.exists(allowedRoutes.contains) then Vector.empty
      else Vector(SuppressionReason.WrongRoute, SuppressionReason.ForbiddenShortcut, SuppressionReason.NoBoardReason)
    val hasEnemyPasser =
      claim.lowerCarrierRefs.exists(ref =>
        ref.kind == EvidenceRefKind.Witness &&
          ref.id == "passed_pawn_entity_state" &&
          sameEnemyPasserBinding(claim, ref)
      )
    val routeProofReasons =
      claim.route match
        case Some("blockade_hold") =>
          if hasRouteCertification(claim, "FortressDrawCertification") then Vector.empty
          else Vector(SuppressionReason.ForbiddenShortcut, SuppressionReason.NoBoardReason)
        case Some("restriction_hold") =>
          val hasRestriction =
            claim.lowerCarrierRefs.exists(ref =>
              ref.kind == EvidenceRefKind.Witness &&
                ref.id == "short_run_slider_gate_restriction" &&
                sameClaimBinding(claim, ref)
            )
          if hasRestriction && hasRouteCertification(claim, "PerpetualCheckHolding") then Vector.empty
          else Vector(SuppressionReason.ForbiddenShortcut, SuppressionReason.NoBoardReason)
        case Some("non_losing_race") =>
          if hasRouteCertification(claim, "PromotionRace") then Vector.empty
          else Vector(SuppressionReason.ForbiddenShortcut, SuppressionReason.NoBoardReason)
        case _ => Vector.empty
    val carrierReasons =
      if hasEnemyPasser then Vector.empty
      else Vector(SuppressionReason.ForbiddenShortcut, SuppressionReason.NoBoardReason)
    (routeReasons ++ carrierReasons ++ routeProofReasons).distinct

  private def hasRouteCertification(claim: CommentaryClaim, id: String): Boolean =
    claim.lowerCarrierRefs.exists(ref =>
      ref.kind == EvidenceRefKind.Certification &&
        ref.id == id &&
        sameClaimBinding(claim, ref)
    )

  private def liabilityReliefCarrierReasons(claim: CommentaryClaim): Vector[SuppressionReason] =
    val allowedRoutes = Set("repair_route", "exchange_relief")
    val routeReasons =
      if claim.route.exists(allowedRoutes.contains) then Vector.empty
      else Vector(SuppressionReason.WrongRoute, SuppressionReason.ForbiddenShortcut, SuppressionReason.NoBoardReason)
    val hasAnchorSeed =
      claim.lowerCarrierRefs.exists(ref =>
        ref.kind == EvidenceRefKind.Witness &&
          ref.id == "same_piece_liability_anchor_seed" &&
          sameClaimBinding(claim, ref)
      )
    val hasReliefRoute =
      claim.route.exists: route =>
        val reliefSeed =
          route match
            case "repair_route" => "same_piece_repair_route_seed"
            case "exchange_relief" => "same_piece_exchange_relief_seed"
            case _ => route
        claim.lowerCarrierRefs.exists(ref =>
          ref.kind == EvidenceRefKind.Witness &&
            ref.id == reliefSeed &&
            sameClaimBinding(claim, ref)
        )
    val carrierReasons =
      if hasAnchorSeed && hasReliefRoute then Vector.empty
      else Vector(SuppressionReason.ForbiddenShortcut, SuppressionReason.NoBoardReason)
    (routeReasons ++ carrierReasons).distinct

  private def bishopPairConversionCarrierReasons(claim: CommentaryClaim): Vector[SuppressionReason] =
    val routeSupport = Map(
      "bishop_pair_to_initiative" -> "InitiativeWindow",
      "bishop_pair_to_structure" -> "MobilityComparison",
      "bishop_pair_to_material" -> "MaterialHarvest"
    )
    val routeReasons =
      if claim.route.exists(routeSupport.contains) then Vector.empty
      else Vector(SuppressionReason.WrongRoute, SuppressionReason.ForbiddenShortcut, SuppressionReason.NoBoardReason)
    val hasBishopPair =
      claim.lowerCarrierRefs.exists(ref =>
        ref.kind == EvidenceRefKind.Witness &&
          ref.id == "bishop_pair_state" &&
          sameClaimBinding(claim, ref)
      )
    val hasConversionSupport =
      claim.route.exists(route =>
        routeSupport.get(route).exists(supportId =>
          claim.lowerCarrierRefs.exists(ref =>
            ref.kind == EvidenceRefKind.Certification &&
              ref.id == supportId &&
              sameClaimBinding(claim, ref)
          )
        )
      )
    val carrierReasons =
      if hasBishopPair && hasConversionSupport then Vector.empty
      else Vector(SuppressionReason.ForbiddenShortcut, SuppressionReason.NoBoardReason)
    (routeReasons ++ carrierReasons).distinct

  private def simplificationCarrierReasons(claim: CommentaryClaim): Vector[SuppressionReason] =
    val routeSupport = Map(
      "trade_invariant_to_material" -> "MaterialHarvest",
      "trade_invariant_to_hold" -> "FortressDrawCertification"
    )
    val routeReasons =
      if claim.route.exists(routeSupport.contains) then Vector.empty
      else Vector(SuppressionReason.WrongRoute, SuppressionReason.ForbiddenShortcut, SuppressionReason.NoBoardReason)
    val hasTradeInvariant =
      claim.lowerCarrierRefs.exists(ref =>
        ref.kind == EvidenceRefKind.Delta &&
          ref.id == "TradeInvariant" &&
          sameClaimBinding(claim, ref)
      )
    val hasSimplificationSupport =
      claim.route.exists(route =>
        routeSupport.get(route).exists(supportId =>
          claim.lowerCarrierRefs.exists(ref =>
            ref.kind == EvidenceRefKind.Certification &&
              ref.id == supportId &&
              sameClaimBinding(claim, ref)
          )
        )
      )
    val carrierReasons =
      if hasTradeInvariant && hasSimplificationSupport then Vector.empty
      else Vector(SuppressionReason.ForbiddenShortcut, SuppressionReason.NoBoardReason)
    (routeReasons ++ carrierReasons).distinct

  private def holdCarrierReasons(claim: CommentaryClaim): Vector[SuppressionReason] =
    val routeReasons =
      if claim.route.exists(route => route == "fortress_draw_hold" || route == "perpetual_hold") then Vector.empty
      else Vector(SuppressionReason.WrongRoute, SuppressionReason.ForbiddenShortcut, SuppressionReason.NoBoardReason)
    val hasHoldSupport =
      claim.route match
        case Some("fortress_draw_hold") =>
          claim.lowerCarrierRefs.exists(ref =>
            ref.kind == EvidenceRefKind.Object &&
              ref.id == "FortressHoldingShell" &&
              sameClaimBinding(claim, ref)
          ) &&
            claim.lowerCarrierRefs.exists(ref =>
              ref.kind == EvidenceRefKind.Certification &&
                ref.id == "FortressDrawCertification" &&
                sameClaimBinding(claim, ref)
            )
        case Some("perpetual_hold") =>
          claim.lowerCarrierRefs.exists(ref =>
            ref.kind == EvidenceRefKind.Certification &&
              ref.id == "PerpetualCheckHolding" &&
              sameClaimBinding(claim, ref)
          )
        case _ => false
    val carrierReasons =
      if hasHoldSupport then Vector.empty
      else Vector(SuppressionReason.ForbiddenShortcut, SuppressionReason.NoBoardReason)
    (routeReasons ++ carrierReasons).distinct

  private def mobilityDominationCarrierReasons(claim: CommentaryClaim): Vector[SuppressionReason] =
    requiredCarrierReasons(
      claim,
      allowedRoutes = Set("mobility_plus_restriction", "defender_starvation"),
      required = Vector(EvidenceRefKind.Certification -> "MobilityComparison"),
      anyOf = Vector(
        Vector(EvidenceRefKind.Witness -> "short_run_slider_gate_restriction"),
        Vector(EvidenceRefKind.Witness -> "duty_bound_defender")
      )
    )

  private def counterplaySurvivalCarrierReasons(claim: CommentaryClaim): Vector[SuppressionReason] =
    requiredCarrierReasons(
      claim,
      allowedRoutes = Set("center_source_survives", "far_wing_source_survives"),
      required = Vector(
        EvidenceRefKind.Witness -> "pawn_push_break_contact_source",
        EvidenceRefKind.Certification -> "InitiativeWindow"
      )
    )

  private def kingActivityCarrierReasons(claim: CommentaryClaim): Vector[SuppressionReason] =
    val kBackedReasons = kBackedKingActivityCarrierReasons(claim)
    if kBackedReasons.isEmpty then Vector.empty
    else if claim.route.exists(route => route == "king_entry_conversion" || route == "king_opposition") then kBackedReasons
    else seedKingActivityCarrierReasons(claim)

  private def kBackedKingActivityCarrierReasons(claim: CommentaryClaim): Vector[SuppressionReason] =
    val requiredEvidenceId =
      claim.route match
        case Some("king_entry_conversion") => Some("king_entry_conversion_certified")
        case Some("king_opposition") => Some("king_opposition_certified")
        case _ => None
    val hasKCarrier =
      claim.lowerCarrierRefs.exists(ref =>
        ref.kind == EvidenceRefKind.ExactBoard &&
          ref.id.startsWith("k-s23-") &&
          sameClaimBinding(claim, ref)
      )
    val hasRuntimeK =
      claim.projectionRuntimeKId.exists(id => id.startsWith("k-s23-"))
    val hasProjectionEvidence =
      requiredEvidenceId.exists(id =>
        claim.evidenceRefs.exists(ref =>
          ref.kind == EvidenceRefKind.Projection &&
            ref.id == id &&
            sameClaimBinding(claim, ref)
        )
      )
    Vector(
      Option.when(requiredEvidenceId.isEmpty)(SuppressionReason.WrongRoute),
      Option.when(requiredEvidenceId.isEmpty)(SuppressionReason.ForbiddenShortcut),
      Option.when(!hasRuntimeK)(SuppressionReason.ForbiddenShortcut),
      Option.when(!hasRuntimeK)(SuppressionReason.NoBoardReason),
      Option.when(!hasKCarrier)(SuppressionReason.ForbiddenShortcut),
      Option.when(!hasKCarrier)(SuppressionReason.NoBoardReason),
      Option.when(!hasProjectionEvidence)(SuppressionReason.ForbiddenShortcut)
    ).flatten.distinct

  private def seedKingActivityCarrierReasons(claim: CommentaryClaim): Vector[SuppressionReason] =
    val entryReasons =
      requiredCarrierReasons(
        claim,
        allowedRoutes = Set("king_entry_route"),
        required = Vector(
          EvidenceRefKind.Witness -> "king_entry_square_seed",
          EvidenceRefKind.Witness -> "king_access_route_seed"
        )
      )
    val oppositionReasons =
      requiredCarrierReasons(
        claim,
        allowedRoutes = Set("king_opposition"),
        required = Vector(EvidenceRefKind.Witness -> "king_opposition_contact_seed")
      )
    if entryReasons.isEmpty || oppositionReasons.isEmpty then Vector.empty
    else entryReasons

  private def preparedTargetCarrierReasons(claim: CommentaryClaim): Vector[SuppressionReason] =
    requiredCarrierReasons(
      claim,
      allowedRoutes = Set("same_target_realization"),
      required = Vector(
        EvidenceRefKind.Witness -> "target_resource_dependency_seed",
        EvidenceRefKind.Witness -> "target_attack_convergence_seed"
      )
    )

  private def s24PublicClosedReasons(claim: CommentaryClaim): Vector[SuppressionReason] =
    (preparedTargetCarrierReasons(claim) ++ Vector(SuppressionReason.ForbiddenShortcut, SuppressionReason.NoBoardReason)).distinct

  private def rankAccessCarrierReasons(claim: CommentaryClaim): Vector[SuppressionReason] =
    requiredCarrierReasons(
      claim,
      allowedRoutes = Set("cross_wing_rank_switch"),
      required = Vector(EvidenceRefKind.Witness -> "rank_corridor_state_seed")
    )

  private def requiredCarrierReasons(
      claim: CommentaryClaim,
      allowedRoutes: Set[String],
      required: Vector[(EvidenceRefKind, String)],
      anyOf: Vector[Vector[(EvidenceRefKind, String)]] = Vector.empty
  ): Vector[SuppressionReason] =
    val routeReasons =
      if claim.route.exists(allowedRoutes.contains) then Vector.empty
      else Vector(SuppressionReason.WrongRoute, SuppressionReason.ForbiddenShortcut, SuppressionReason.NoBoardReason)
    def has(kind: EvidenceRefKind, id: String): Boolean =
      claim.lowerCarrierRefs.exists(ref =>
        ref.kind == kind &&
          ref.id == id &&
          sameClaimBinding(claim, ref)
      )
    val hasRequired = required.forall((kind, id) => has(kind, id))
    val hasAlternative =
      anyOf.isEmpty || anyOf.exists(group => group.forall((kind, id) => has(kind, id)))
    val carrierReasons =
      if hasRequired && hasAlternative then Vector.empty
      else Vector(SuppressionReason.ForbiddenShortcut, SuppressionReason.NoBoardReason)
    (routeReasons ++ carrierReasons).distinct

  private def sameClaimBinding(claim: CommentaryClaim, ref: EvidenceRef): Boolean =
    ref.owner == claim.owner &&
      ref.anchor == claim.anchor &&
      ref.route == claim.route &&
      ref.scope == claim.scope

  private def boundedClaimBinding(claim: CommentaryClaim, ref: EvidenceRef): Boolean =
    claim.owner.nonEmpty &&
      claim.anchor.nonEmpty &&
      claim.route.nonEmpty &&
      claim.scope.nonEmpty &&
      sameClaimBinding(claim, ref)

  private def sameEnemyPasserBinding(claim: CommentaryClaim, ref: EvidenceRef): Boolean =
    ref.owner == claim.defender &&
      ref.anchor == claim.anchor &&
      ref.route == claim.route &&
      ref.scope == claim.scope

  private def sameDefenderBinding(claim: CommentaryClaim, ref: EvidenceRef): Boolean =
    ref.owner == claim.defender &&
      ref.anchor == claim.anchor &&
      ref.route == claim.route &&
      ref.scope == claim.scope

  private def enemyPasserCarrierReasons(claim: CommentaryClaim, ref: EvidenceRef): Vector[SuppressionReason] =
    Vector(
      Option.when(ref.owner.exists(owner => claim.defender.forall(_ != owner)))(SuppressionReason.WrongOwner),
      Option.when(ref.anchor.exists(anchor => claim.anchor.forall(_ != anchor)))(SuppressionReason.WrongAnchor),
      Option.when(ref.route.exists(route => claim.route.forall(_ != route)))(SuppressionReason.WrongRoute),
      Option.when(ref.scope.exists(scope => claim.scope.forall(_ != scope)))(SuppressionReason.ScopeMismatch)
    ).flatten

  private def defenderOwnedCarrierReasons(claim: CommentaryClaim, ref: EvidenceRef): Vector[SuppressionReason] =
    Vector(
      Option.when(ref.owner.exists(owner => claim.defender.forall(_ != owner)))(SuppressionReason.WrongOwner),
      Option.when(ref.anchor.exists(anchor => claim.anchor.forall(_ != anchor)))(SuppressionReason.WrongAnchor),
      Option.when(ref.route.exists(route => claim.route.forall(_ != route)))(SuppressionReason.WrongRoute),
      Option.when(ref.scope.exists(scope => claim.scope.forall(_ != scope)))(SuppressionReason.ScopeMismatch)
    ).flatten

  private def supportRelationReasons(
      lead: CommentaryClaim,
      support: CommentaryClaim
  ): Vector[SuppressionReason] =
    if lead.band.contains(StrategyProjectionScopeContract.S15.value) &&
      support.layer == ClaimLayer.Projection &&
      support.band.exists(band => band == StrategyProjectionScopeContract.S13.value || band == StrategyProjectionScopeContract.S14.value)
    then sameCandidateSupportReasons(lead, support)
    else if projectionRivalReasons(lead, support).nonEmpty then projectionRivalReasons(lead, support)
    else if lead.layer == ClaimLayer.Projection &&
      support.layer == ClaimLayer.Projection &&
      support.band.contains(StrategyProjectionScopeContract.S19.value) &&
      lead.band.exists(band =>
        band == StrategyProjectionScopeContract.S17.value ||
          band == StrategyProjectionScopeContract.S18.value ||
          band == StrategyProjectionScopeContract.S22.value
      ) &&
      sameOwnerAnchorScope(lead, support)
    then Vector(SuppressionReason.RivalBand)
    else if lead.layer == ClaimLayer.Projection &&
      support.layer == ClaimLayer.Projection &&
      lead.band.contains(StrategyProjectionScopeContract.S18.value) &&
      support.band.contains(StrategyProjectionScopeContract.S22.value) &&
      sameOwnerAnchorScope(lead, support) &&
      isJustifiedConversionAgainstHold(lead, support)
    then Vector(SuppressionReason.RivalBand)
    else if lead.layer == ClaimLayer.Projection &&
      support.layer == ClaimLayer.Projection &&
      lead.band.contains(StrategyProjectionScopeContract.S22.value) &&
      support.band.contains(StrategyProjectionScopeContract.S18.value) &&
      sameOwnerAnchorScope(lead, support) &&
      !isJustifiedConversionAgainstHold(support, lead)
    then Vector(SuppressionReason.RivalBand)
      else Vector.empty

  private def projectionRivalReasons(
      lead: CommentaryClaim,
      support: CommentaryClaim
  ): Vector[SuppressionReason] =
    val bands = Set(lead.band, support.band).flatten
    val initiativeRival =
      bands == Set(StrategyProjectionScopeContract.S07.value, StrategyProjectionScopeContract.S08.value) &&
        lead.owner == support.owner &&
        lead.scope == support.scope
    val passerRival =
      bands == Set(StrategyProjectionScopeContract.S15.value, StrategyProjectionScopeContract.S16.value) &&
        lead.owner == support.owner &&
        lead.beneficiary.nonEmpty &&
        support.beneficiary.nonEmpty &&
        lead.defender.nonEmpty &&
        support.defender.nonEmpty &&
        lead.sideToMove.nonEmpty &&
        support.sideToMove.nonEmpty
    val passerBindingReasons =
      if bands == Set(StrategyProjectionScopeContract.S15.value, StrategyProjectionScopeContract.S16.value) then
        Vector(
          Option.when(lead.owner != support.owner)(SuppressionReason.WrongOwner),
          Option.when(lead.beneficiary != support.beneficiary)(SuppressionReason.WrongOwner),
          Option.when(lead.defender != support.defender)(SuppressionReason.WrongRoute),
          Option.when(lead.sideToMove != support.sideToMove)(SuppressionReason.ScopeMismatch)
        ).flatten
      else Vector.empty
    if initiativeRival || (passerRival && passerBindingReasons.isEmpty) then Vector(SuppressionReason.RivalBand)
    else passerBindingReasons.distinct

  private def sameCandidateSupportReasons(
      lead: CommentaryClaim,
      support: CommentaryClaim
  ): Vector[SuppressionReason] =
    Vector(
      Option.when(lead.owner != support.owner)(SuppressionReason.WrongOwner),
      Option.when(lead.anchor != support.anchor)(SuppressionReason.WrongAnchor),
      Option.when(lead.scope != support.scope)(SuppressionReason.ScopeMismatch)
    ).flatten

  private def sameOwnerAnchorScope(left: CommentaryClaim, right: CommentaryClaim): Boolean =
    left.owner == right.owner &&
      left.anchor == right.anchor &&
      left.scope == right.scope

  private def isKingAttackBand(claim: CommentaryClaim): Boolean =
    claim.band.exists(band =>
      band == StrategyProjectionScopeContract.S01.value ||
        band == StrategyProjectionScopeContract.S02.value ||
        band == StrategyProjectionScopeContract.S03.value ||
        band == StrategyProjectionScopeContract.S04.value
    )

  private def projectionEvidenceAllowed(claim: CommentaryClaim, band: String): Boolean =
    val projectionEvidenceIds =
      claim.evidenceRefs.collect {
        case ref if ref.kind == EvidenceRefKind.Projection && sameClaimBinding(claim, ref) => ref.id
      }.toSet
    if band == StrategyProjectionScopeContract.S24.value then
      Set(
        StrategyProjectionScopeContract.SameTargetForcingRealization.value,
        StrategyProjectionScopeContract.SameTargetConversionCertified.value
      ).subsetOf(projectionEvidenceIds)
    else
      claim.evidenceRefs.exists: ref =>
        ref.kind == EvidenceRefKind.Projection &&
          sameClaimBinding(claim, ref) &&
          StrategyProjectionScopeContract.isAllowedEvidenceKind(
            StrategyProjectionBandId(band),
            StrategyProjectionEvidenceKind(ref.id)
          )

  private def projectionEvidenceBindingReasons(
      claim: CommentaryClaim,
      band: String
  ): Vector[SuppressionReason] =
    val refs =
      claim.evidenceRefs.filter(ref =>
        ref.kind == EvidenceRefKind.Projection &&
          StrategyProjectionScopeContract.isAllowedEvidenceKind(
            StrategyProjectionBandId(band),
            StrategyProjectionEvidenceKind(ref.id)
          )
      )
    refs.flatMap: ref =>
      Vector(
        Option.when(ref.owner != claim.owner)(SuppressionReason.WrongOwner),
        Option.when(ref.anchor != claim.anchor)(SuppressionReason.WrongAnchor),
        Option.when(ref.route != claim.route)(SuppressionReason.WrongRoute),
        Option.when(ref.scope != claim.scope)(SuppressionReason.ScopeMismatch)
      ).flatten

  private def requiresOwner(claim: CommentaryClaim): Boolean =
    claim.layer == ClaimLayer.Certification || claim.layer == ClaimLayer.Delta || claim.layer == ClaimLayer.Projection

  private def requiresBeneficiary(claim: CommentaryClaim): Boolean =
    claim.layer == ClaimLayer.Certification || claim.layer == ClaimLayer.Delta || claim.layer == ClaimLayer.Projection

  private def requiresDefender(claim: CommentaryClaim): Boolean =
    claim.layer == ClaimLayer.Certification || claim.layer == ClaimLayer.Delta || claim.layer == ClaimLayer.Projection

  private def requiresSideToMove(claim: CommentaryClaim): Boolean =
    claim.layer == ClaimLayer.Certification || claim.layer == ClaimLayer.Delta || claim.layer == ClaimLayer.Projection

  private def requiresAnchor(claim: CommentaryClaim): Boolean =
    claim.layer == ClaimLayer.Certification || claim.layer == ClaimLayer.Delta || claim.layer == ClaimLayer.Projection

  private def requiresRoute(claim: CommentaryClaim): Boolean =
    claim.layer == ClaimLayer.Certification || claim.layer == ClaimLayer.Delta || claim.layer == ClaimLayer.Projection

  private def requiresScope(claim: CommentaryClaim): Boolean =
    claim.layer == ClaimLayer.Certification || claim.layer == ClaimLayer.Delta || claim.layer == ClaimLayer.Projection

  private def isContext(claim: CommentaryClaim): Boolean =
    claim.layer == ClaimLayer.SourceContext

  private def usableSourceContext(claim: CommentaryClaim, reasons: Vector[SuppressionReason]): Boolean =
    (claim.status == ClaimStatus.Context || claim.status == ClaimStatus.Admitted) &&
      claim.sourceContextKind.nonEmpty &&
      reasons.forall(SourceContextSoftReasons.contains)

  private val SourceContextSoftReasons: Set[SuppressionReason] =
    Set(
      SuppressionReason.SourceContextOnly,
      SuppressionReason.RetrievalNonAuthoritative
    )

  private def softSourceReasons(reasons: Vector[SuppressionReason]): Vector[SuppressionReason] =
    reasons.filter(SourceContextSoftReasons.contains).distinct

  private def canSelectAsBoardClaim(claim: CommentaryClaim): Boolean =
    claim.status == ClaimStatus.Admitted && claim.layer != ClaimLayer.SourceContext && claim.layer != ClaimLayer.Engine && claim.layer != ClaimLayer.Renderer

  private def suppressWeakerDuplicates(
      claims: Vector[CommentaryClaim]
  ): (Vector[CommentaryClaim], Vector[SuppressedClaim]) =
    val (projectionClaims, otherClaims) = claims.partition(_.layer == ClaimLayer.Projection)
    val grouped = projectionClaims.groupBy(nonredundancyKey)
    val kept = grouped.values.map(group => group.toVector.sortWith(strongerThan).head).toVector
    val suppressed =
      grouped.values.toVector.flatMap: group =>
        val ordered = group.toVector.sortWith(strongerThan)
        ordered.drop(1).map: claim =>
          val reason =
            if projectionRivalReasons(ordered.head, claim).nonEmpty then SuppressionReason.RivalBand
            else SuppressionReason.DuplicateWeakerClaim
          SuppressedClaim(claim, Vector(reason))
    (otherClaims ++ kept, suppressed)

  private def nonredundancyKey(claim: CommentaryClaim): (Option[String], Option[String], Option[String], Option[String]) =
    (claim.owner, claim.anchor, claim.route, claim.scope)

  private def strongerThan(left: CommentaryClaim, right: CommentaryClaim): Boolean =
    if isS15WithSameCandidateSupport(left, right) then true
    else if isS15WithSameCandidateSupport(right, left) then false
    else if isConversionClusterOwnerWithS19Rival(left, right) then true
    else if isConversionClusterOwnerWithS19Rival(right, left) then false
    else if isJustifiedConversionAgainstHold(left, right) then true
    else if isJustifiedConversionAgainstHold(right, left) then false
    else if isHoldAgainstUnderQualifiedConversion(left, right) then true
    else if isHoldAgainstUnderQualifiedConversion(right, left) then false
    else
      val leftBucketRank = leadBucketPriority(bucketForLead(left))
      val rightBucketRank = leadBucketPriority(bucketForLead(right))
      leftBucketRank > rightBucketRank ||
        (leftBucketRank == rightBucketRank && {
          val leftRank = effectiveLeadRank(left)
          val rightRank = effectiveLeadRank(right)
          compareRanks(leftRank, rightRank) > 0 ||
            (compareRanks(leftRank, rightRank) == 0 && left.id < right.id)
        })

  private def isS15WithSameCandidateSupport(
      s15: CommentaryClaim,
      support: CommentaryClaim
  ): Boolean =
    s15.layer == ClaimLayer.Projection &&
      s15.band.contains(StrategyProjectionScopeContract.S15.value) &&
      support.layer == ClaimLayer.Projection &&
      support.band.exists(band => band == StrategyProjectionScopeContract.S13.value || band == StrategyProjectionScopeContract.S14.value) &&
      sameCandidateSupportReasons(s15, support).isEmpty

  private def isConversionClusterOwnerWithS19Rival(
      ownerClaim: CommentaryClaim,
      s19: CommentaryClaim
  ): Boolean =
    ownerClaim.layer == ClaimLayer.Projection &&
      s19.layer == ClaimLayer.Projection &&
      ownerClaim.band.exists(band =>
        band == StrategyProjectionScopeContract.S17.value ||
          band == StrategyProjectionScopeContract.S18.value ||
          band == StrategyProjectionScopeContract.S22.value
      ) &&
      s19.band.contains(StrategyProjectionScopeContract.S19.value) &&
      sameOwnerAnchorScope(ownerClaim, s19)

  private def isJustifiedConversionAgainstHold(
      conversion: CommentaryClaim,
      hold: CommentaryClaim
  ): Boolean =
    conversion.layer == ClaimLayer.Projection &&
      hold.layer == ClaimLayer.Projection &&
    conversion.band.contains(StrategyProjectionScopeContract.S18.value) &&
      hold.band.contains(StrategyProjectionScopeContract.S22.value) &&
      sameOwnerAnchorScope(conversion, hold) &&
      conversion.route.contains("bishop_pair_to_material") &&
      conversion.impact.resultMaterialImpact >= 80 &&
      conversion.evidenceRefs.exists(ref =>
        ref.kind == EvidenceRefKind.Projection &&
          ref.id == "bishop_pair_material_conversion_certified" &&
          sameClaimBinding(conversion, ref)
      ) &&
      conversion.lowerCarrierRefs.exists(ref =>
        ref.kind == EvidenceRefKind.Certification &&
          ref.id == "MaterialHarvest" &&
          sameClaimBinding(conversion, ref)
      )

  private def isHoldAgainstUnderQualifiedConversion(
      hold: CommentaryClaim,
      conversion: CommentaryClaim
  ): Boolean =
    hold.layer == ClaimLayer.Projection &&
      conversion.layer == ClaimLayer.Projection &&
      hold.band.contains(StrategyProjectionScopeContract.S22.value) &&
      conversion.band.contains(StrategyProjectionScopeContract.S18.value) &&
      sameOwnerAnchorScope(hold, conversion) &&
      !isJustifiedConversionAgainstHold(conversion, hold)

  private def effectiveLeadRank(claim: CommentaryClaim): Vector[Int] =
    claim.impact.leadRank(
      evalSwingAllowed =
        claim.layer == ClaimLayer.Certification &&
          claim.evidenceRefs.exists(_.kind == EvidenceRefKind.EngineCertification) &&
          hasEngineCertifiedBoardReason(claim)
    )

  private def compareRanks(left: Vector[Int], right: Vector[Int]): Int =
    left.zip(right).collectFirst:
      case (l, r) if l != r => l.compare(r)
    .getOrElse(0)

  private def leadBucketPriority(bucket: ClaimBucket): Int =
    bucket match
      case ClaimBucket.MustLead => 3
      case ClaimBucket.ShouldLead => 2
      case ClaimBucket.CanLead => 1
      case ClaimBucket.Support => 0
      case ClaimBucket.ContextOnly => -1
      case ClaimBucket.Suppress => -2

  private def bucketForLead(claim: CommentaryClaim): ClaimBucket =
    if claim.layer == ClaimLayer.Certification &&
      (claim.impact.resultMaterialImpact >= 80 || claim.impact.forcedness >= 80 || claim.impact.immediacy >= 80)
    then ClaimBucket.MustLead
    else if claim.layer == ClaimLayer.Delta && isGenericTransitionClaim(claim) then ClaimBucket.CanLead
    else if claim.layer == ClaimLayer.Delta || claim.layer == ClaimLayer.Projection then ClaimBucket.ShouldLead
    else ClaimBucket.CanLead

  private def isGenericTransitionClaim(claim: CommentaryClaim): Boolean =
    claim.route.exists(route => route == "last_move_transition" || route == "pawn_structure_transition")
