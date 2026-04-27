package lila.commentary.line

import lila.commentary.selection.{ VariationSurfaceAllowance, WordingStrength }

opaque type CandidateBranchId = String

object CandidateBranchId:
  def apply(value: String): CandidateBranchId =
    val normalized = value.trim
    require(normalized.nonEmpty, "Candidate branch id must be non-empty")
    require(
      normalized.matches("[a-z][a-z0-9_-]*"),
      "Candidate branch id must be stable lowercase ASCII"
    )
    normalized

  extension (id: CandidateBranchId) def value: String = id

enum CandidateBranchRole(val key: String, val publicEligible: Boolean, val leadEligible: Boolean):
  case RootCandidate extends CandidateBranchRole("root_candidate", true, true)
  case Alternative extends CandidateBranchRole("alternative", true, false)
  case DefenderResource extends CandidateBranchRole("defender_resource", true, false)
  case DefenderReply extends CandidateBranchRole("defender_reply", true, false)
  case FailedCandidate extends CandidateBranchRole("failed_candidate", false, false)
  case Premature extends CandidateBranchRole("premature", false, false)
  case ReleaseRisk extends CandidateBranchRole("release_risk", false, false)

object CandidateBranchRole:
  def fromKey(key: String): Option[CandidateBranchRole] =
    values.find(_.key == key)

enum CandidateLineSide(val key: String):
  case White extends CandidateLineSide("white")
  case Black extends CandidateLineSide("black")

object CandidateLineSide:
  def fromKey(key: String): Option[CandidateLineSide] =
    values.find(_.key == key)

enum CandidateLineProvenanceKind(
    val key: String,
    val proofOwnerAllowed: Boolean,
    val candidateSourceAllowed: Boolean
):
  case EngineRoot extends CandidateLineProvenanceKind("engine_root", true, true)
  case EngineChild extends CandidateLineProvenanceKind("engine_child", true, true)
  case SourceHint extends CandidateLineProvenanceKind("source_hint", false, true)
  case Cache extends CandidateLineProvenanceKind("cache", true, true)

object CandidateLineProvenanceKind:
  def fromKey(key: String): Option[CandidateLineProvenanceKind] =
    values.find(_.key == key)

final case class CandidateLineProvenance(
    kind: CandidateLineProvenanceKind,
    sourceHintRefs: Vector[String] = Vector.empty
):
  require(sourceHintRefs.forall(_.trim.nonEmpty), "Source hint refs must be non-empty when present")

  def proofOwnerAllowed: Boolean = kind.proofOwnerAllowed
  def candidateSourceAllowed: Boolean = kind.candidateSourceAllowed

enum CandidateLineReplayStatus(val key: String, val legal: Boolean):
  case Legal extends CandidateLineReplayStatus("legal", true)
  case Illegal extends CandidateLineReplayStatus("illegal", false)

object CandidateLineReplayStatus:
  def fromKey(key: String): Option[CandidateLineReplayStatus] =
    values.find(_.key == key)

enum CandidateLineRejectionReason(val key: String):
  case MissingSan extends CandidateLineRejectionReason("missing_san")
  case MissingUci extends CandidateLineRejectionReason("missing_uci")
  case LineLengthMismatch extends CandidateLineRejectionReason("line_length_mismatch")
  case InsufficientDepth extends CandidateLineRejectionReason("insufficient_depth")
  case Stale extends CandidateLineRejectionReason("stale")
  case IllegalReplay extends CandidateLineRejectionReason("illegal_replay")
  case SourceHintOnly extends CandidateLineRejectionReason("source_hint_only")
  case RetrievalNonAuthoritative extends CandidateLineRejectionReason("retrieval_non_authoritative")
  case InternalOnlyRole extends CandidateLineRejectionReason("internal_only_role")
  case InvalidRank extends CandidateLineRejectionReason("invalid_rank")
  case InvalidMultiPv extends CandidateLineRejectionReason("invalid_multipv")
  case InvalidNode extends CandidateLineRejectionReason("invalid_node")
  case InvalidFen extends CandidateLineRejectionReason("invalid_fen")
  case MalformedUci extends CandidateLineRejectionReason("malformed_uci")
  case DuplicateRootFirstMove extends CandidateLineRejectionReason("duplicate_root_first_move")
  case InvalidFreshnessWindow extends CandidateLineRejectionReason("invalid_freshness_window")
  case InternalSurfaceOnly extends CandidateLineRejectionReason("internal_surface_only")
  case MissingParentBranch extends CandidateLineRejectionReason("missing_parent_branch")
  case StartFenMismatch extends CandidateLineRejectionReason("start_fen_mismatch")
  case StartPlyMismatch extends CandidateLineRejectionReason("start_ply_mismatch")
  case InvalidChildRole extends CandidateLineRejectionReason("invalid_child_role")

object CandidateLineRejectionReason:
  def fromKey(key: String): Option[CandidateLineRejectionReason] =
    values.find(_.key == key)

final case class CandidateLineTiming(
    generatedAtEpochMs: Long,
    maxAgeMs: Long
):
  def freshnessValid: Boolean =
    generatedAtEpochMs >= 0L && maxAgeMs > 0L

  def freshAt(nowEpochMs: Long): Boolean =
    freshnessValid && nowEpochMs >= generatedAtEpochMs && (nowEpochMs - generatedAtEpochMs) <= maxAgeMs

final case class CandidateLineSearch(
    requestedDepth: Int,
    realizedDepth: Int,
    multiPv: Int,
    multiPvIndex: Int
):
  def depthFloorMet(depthFloor: Int): Boolean =
    realizedDepth >= depthFloor

  def validMultiPv: Boolean =
    multiPv > 0 && multiPvIndex > 0 && multiPvIndex <= multiPv

final case class CandidateLinePreparedTarget(
    proofId: String,
    boundClaimId: String
):
  require(proofId.trim.nonEmpty, "Prepared variation target proof id must be non-empty")
  require(boundClaimId.trim.nonEmpty, "Prepared variation target bound claim id must be non-empty")

final case class CandidateLineEvidence(
    branchId: CandidateBranchId,
    parentBranchId: Option[CandidateBranchId],
    role: CandidateBranchRole,
    rank: Int,
    multiPvIndex: Int,
    startFen: String,
    startNodeId: String,
    startPly: Int,
    sideToMove: CandidateLineSide,
    lineUci: Vector[String],
    lineSan: Vector[String],
    requestedDepth: Int,
    realizedDepth: Int,
    multiPv: Int,
    timing: CandidateLineTiming,
    engineConfigFingerprint: String,
    legalReplay: CandidateLineReplayStatus,
    provenance: CandidateLineProvenance,
    surfaceAllowance: VariationSurfaceAllowance,
    wordingCap: WordingStrength,
    failureReasons: Vector[CandidateLineRejectionReason] = Vector.empty,
    preparedVariationTarget: Option[CandidateLinePreparedTarget] = None
):
  require(engineConfigFingerprint.trim.nonEmpty, "Engine fingerprint must be non-empty")
  require(lineSan.forall(_.trim.nonEmpty), "SAN moves must be non-empty when present")
  require(lineUci.forall(_.trim.nonEmpty), "UCI moves must be non-empty when present")
  require(failureReasons.distinct.size == failureReasons.size, "Candidate line failure reasons must be distinct")
  require(startPly >= 0, "Start ply must be non-negative")

  def search: CandidateLineSearch =
    CandidateLineSearch(requestedDepth, realizedDepth, multiPv, multiPvIndex)

  def rejectionReasonsAt(
      nowEpochMs: Long,
      depthFloor: Int = CandidateLineEvidence.DefaultDepthFloor
  ): Vector[CandidateLineRejectionReason] =
    (failureReasons ++ Vector(
      Option.when(startFen.trim.isEmpty)(CandidateLineRejectionReason.InvalidFen),
      Option.when(startNodeId.trim.isEmpty)(CandidateLineRejectionReason.InvalidNode),
      Option.when(rank <= 0)(CandidateLineRejectionReason.InvalidRank),
      Option.when(!search.validMultiPv)(CandidateLineRejectionReason.InvalidMultiPv),
      Option.when(lineSan.isEmpty)(CandidateLineRejectionReason.MissingSan),
      Option.when(lineUci.isEmpty)(CandidateLineRejectionReason.MissingUci),
      Option.when(lineSan.nonEmpty && lineUci.nonEmpty && lineSan.size != lineUci.size)(
        CandidateLineRejectionReason.LineLengthMismatch
      ),
      Option.when(!search.depthFloorMet(depthFloor))(CandidateLineRejectionReason.InsufficientDepth),
      Option.when(!timing.freshAt(nowEpochMs))(CandidateLineRejectionReason.Stale),
      Option.when(!timing.freshnessValid)(CandidateLineRejectionReason.InvalidFreshnessWindow),
      Option.when(!legalReplay.legal)(CandidateLineRejectionReason.IllegalReplay),
      Option.when(!role.publicEligible)(CandidateLineRejectionReason.InternalOnlyRole),
      Option.when(!provenance.proofOwnerAllowed)(CandidateLineRejectionReason.SourceHintOnly),
      Option.when(surfaceAllowance == VariationSurfaceAllowance.InternalOnly)(
        CandidateLineRejectionReason.InternalSurfaceOnly
      )
    ).flatten).distinct

  def publicLineEligibleAt(
      nowEpochMs: Long,
      depthFloor: Int = CandidateLineEvidence.DefaultDepthFloor
  ): Boolean =
    rejectionReasonsAt(nowEpochMs, depthFloor).isEmpty &&
      surfaceAllowance == VariationSurfaceAllowance.PublicLine

  def leadRecommendationEligibleAt(
      nowEpochMs: Long,
      depthFloor: Int = CandidateLineEvidence.DefaultDepthFloor
  ): Boolean =
    publicLineEligibleAt(nowEpochMs, depthFloor) && role.leadEligible

object CandidateLineEvidence:
  val DefaultDepthFloor = 16
