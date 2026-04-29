package lila.commentary.line

import chess.Ply
import chess.format.{ Fen, Uci }
import chess.variant

import scala.util.control.NonFatal

enum CandidateProbeRole(val key: String, val branchRole: CandidateBranchRole):
  case RootCandidate extends CandidateProbeRole("root_candidate", CandidateBranchRole.RootCandidate)
  case DefenderResource extends CandidateProbeRole("defender_resource", CandidateBranchRole.DefenderResource)
  case DefenderReply extends CandidateProbeRole("defender_reply", CandidateBranchRole.DefenderReply)

enum CandidateProbeSource(val key: String):
  case EngineProbe extends CandidateProbeSource("engine_probe")
  case CacheHit extends CandidateProbeSource("cache_hit")

final case class CandidateProbeBudget(
    rootMultiPv: Int,
    childMultiPv: Int,
    targetDepth: Int,
    floorDepth: Int,
    strongCacheTargetDepth: Int,
    childRootRankLimit: Int,
    allowExpandedThirdRootChildProbe: Boolean,
    allowThirdRootChildFromCache: Boolean
):
  require(rootMultiPv == 3, "Candidate root MultiPV is frozen to 3")
  require(childMultiPv == 2, "Candidate child MultiPV is frozen to 2")
  require(targetDepth >= floorDepth, "Target depth must meet floor depth")
  require(strongCacheTargetDepth >= targetDepth, "Strong/cache depth must meet default target depth")
  require(childRootRankLimit >= 1, "Child root rank limit must be positive")

object CandidateProbeBudget:
  val Default: CandidateProbeBudget =
    CandidateProbeBudget(
      rootMultiPv = 3,
      childMultiPv = 2,
      targetDepth = 18,
      floorDepth = 16,
      strongCacheTargetDepth = 20,
      childRootRankLimit = 2,
      allowExpandedThirdRootChildProbe = false,
      allowThirdRootChildFromCache = true
    )

final case class CandidateProbeRequest(
    startFen: String,
    nodeId: String,
    ply: Int,
    variant: String,
    role: CandidateProbeRole,
    targetDepth: Int,
    floorDepth: Int,
    multiPv: Int,
    engineFingerprint: String,
    parentBranchId: Option[CandidateBranchId] = None,
    parentLinePrefix: Vector[String] = Vector.empty,
    parentRootRank: Option[Int] = None,
    probeSource: CandidateProbeSource = CandidateProbeSource.EngineProbe
):
  require(startFen.trim.nonEmpty, "Probe start FEN must be non-empty")
  require(nodeId.trim.nonEmpty, "Probe node id must be non-empty")
  require(ply >= 0, "Probe ply must be non-negative")
  require(variant.trim.nonEmpty, "Probe variant must be non-empty")
  require(targetDepth >= floorDepth, "Probe target depth must meet floor depth")
  require(multiPv > 0, "Probe MultiPV must be positive")
  require(engineFingerprint.trim.nonEmpty, "Probe engine fingerprint must be non-empty")
  require(parentLinePrefix.forall(_.trim.nonEmpty), "Probe parent prefix moves must be non-empty")

final case class CandidateProbePlan(requests: Vector[CandidateProbeRequest])

object CandidateProbePlan:

  def rootStage(
      startFen: String,
      nodeId: String,
      ply: Int,
      variant: String,
      engineFingerprint: String,
      budget: CandidateProbeBudget = CandidateProbeBudget.Default
  ): CandidateProbePlan =
    CandidateProbePlan(
      Vector(
        CandidateProbeRequest(
          startFen = normalizeFen(startFen).getOrElse(startFen),
          nodeId = nodeId,
          ply = ply,
          variant = variant,
          role = CandidateProbeRole.RootCandidate,
          targetDepth = budget.targetDepth,
          floorDepth = budget.floorDepth,
          multiPv = budget.rootMultiPv,
          engineFingerprint = engineFingerprint
        )
      )
    )

  def childStage(
      rootEvidence: Vector[CandidateLineEvidence],
      budget: CandidateProbeBudget = CandidateProbeBudget.Default,
      validCacheEntries: Vector[CandidateProbeCacheEntry] = Vector.empty,
      nowEpochMs: Long = Long.MaxValue
  ): CandidateProbePlan =
    val roots =
      rootEvidence
        .filter(line => line.role == CandidateBranchRole.RootCandidate && line.parentBranchId.isEmpty)
        .filter(rootEligibleForChildPlanning(_, budget, nowEpochMs))
        .sortBy(line => (line.rank, line.multiPvIndex, line.branchId.value))
    val requests =
      roots.flatMap: root =>
        childRequest(root, budget).flatMap: request =>
          val cacheRequest =
            if root.rank == 3 then request.copy(targetDepth = budget.strongCacheTargetDepth)
            else request
          val defaultAllowed = root.rank >= 1 && root.rank <= budget.childRootRankLimit
          val expandedAllowed = root.rank == 3 && budget.allowExpandedThirdRootChildProbe
          val cachedAllowed =
            root.rank == 3 &&
              budget.allowThirdRootChildFromCache &&
              validCacheEntries.exists(_.revalidateFor(cacheRequest, nowEpochMs).accepted)
          Option.when(defaultAllowed || expandedAllowed || cachedAllowed)(
            if cachedAllowed && !defaultAllowed && !expandedAllowed then cacheRequest.copy(probeSource = CandidateProbeSource.CacheHit)
            else request
          )
    CandidateProbePlan(requests)

  private def rootEligibleForChildPlanning(
      root: CandidateLineEvidence,
      budget: CandidateProbeBudget,
      nowEpochMs: Long
  ): Boolean =
    val validationNow =
      if nowEpochMs == Long.MaxValue && root.timing.freshnessValid then root.timing.generatedAtEpochMs
      else nowEpochMs
    root.publicLineEligibleAt(validationNow, budget.floorDepth)

  private def childRequest(root: CandidateLineEvidence, budget: CandidateProbeBudget): Option[CandidateProbeRequest] =
    val prefix = root.lineUci.take(1).map(_.trim.toLowerCase)
    for startFen <- fenAfterPrefix(root.startFen, root.startPly, prefix)
    yield CandidateProbeRequest(
      startFen = startFen,
      nodeId = root.startNodeId,
      ply = root.startPly + prefix.size,
      variant = "standard",
      role = CandidateProbeRole.DefenderResource,
      targetDepth = budget.targetDepth,
      floorDepth = budget.floorDepth,
      multiPv = budget.childMultiPv,
      engineFingerprint = root.engineConfigFingerprint,
      parentBranchId = Some(root.branchId),
      parentLinePrefix = prefix,
      parentRootRank = Some(root.rank)
    )

  private[line] def normalizeFen(fen: String): Option[String] =
    safe((Fen.Full.clean(fen): Fen.Full).value)

  private[line] def fenAfterPrefix(startFen: String, startPly: Int, prefix: Vector[String]): Option[String] =
    for
      position <- readPosition(startFen)
      reached <- replay(position, prefix)
      fen <- safe(Fen.write(reached, Ply(startPly + prefix.size).fullMoveNumber).value)
    yield fen

  private def readPosition(fen: String): Option[chess.Position] =
    safe(Fen.read(variant.Standard, Fen.Full.clean(fen))).flatten

  private def replay(start: chess.Position, uciLine: Vector[String]): Option[chess.Position] =
    uciLine.foldLeft(Option(start)): (position, rawUci) =>
      position.flatMap: current =>
        Uci(rawUci.trim) match
          case Some(move: Uci.Move) => current.move(move).toOption.map(_.after.position)
          case _                    => None

  private def safe[A](body: => A): Option[A] =
    try Some(body)
    catch case NonFatal(_) => None

final case class CandidateProbeCacheKey(
    normalizedFen: String,
    variant: String,
    nodeId: String,
    ply: Int,
    engineFingerprint: String,
    targetDepth: Int,
    floorDepth: Int,
    realizedDepth: Int,
    multiPv: Int,
    role: CandidateProbeRole,
    parentBranchId: Option[CandidateBranchId],
    parentLinePrefix: Vector[String],
    parentRootRank: Option[Int],
    normalizedUciFirstMove: Option[String],
    generatedAtEpochMs: Long,
    maxAgeMs: Long
):
  require(normalizedFen.trim.nonEmpty, "Cache key FEN must be non-empty")
  require(variant.trim.nonEmpty, "Cache key variant must be non-empty")
  require(nodeId.trim.nonEmpty, "Cache key node id must be non-empty")
  require(ply >= 0, "Cache key ply must be non-negative")
  require(engineFingerprint.trim.nonEmpty, "Cache key engine fingerprint must be non-empty")
  require(targetDepth >= floorDepth, "Cache key target depth must meet floor depth")
  require(realizedDepth >= 0, "Cache key realized depth must be non-negative")
  require(multiPv > 0, "Cache key MultiPV must be positive")
  require(parentLinePrefix.forall(_.trim.nonEmpty), "Cache key parent prefix moves must be non-empty")

object CandidateProbeCacheKey:
  def fromRequest(
      request: CandidateProbeRequest,
      realizedDepth: Int,
      generatedAtEpochMs: Long,
      maxAgeMs: Long
  ): CandidateProbeCacheKey =
    CandidateProbeCacheKey(
      normalizedFen = CandidateProbePlan.normalizeFen(request.startFen).getOrElse(request.startFen.trim),
      variant = request.variant.trim,
      nodeId = request.nodeId.trim,
      ply = request.ply,
      engineFingerprint = request.engineFingerprint.trim,
      targetDepth = request.targetDepth,
      floorDepth = request.floorDepth,
      realizedDepth = realizedDepth,
      multiPv = request.multiPv,
      role = request.role,
      parentBranchId = request.parentBranchId,
      parentLinePrefix = normalizePrefix(request.parentLinePrefix),
      parentRootRank = request.parentRootRank,
      normalizedUciFirstMove = normalizePrefix(request.parentLinePrefix).headOption,
      generatedAtEpochMs = generatedAtEpochMs,
      maxAgeMs = maxAgeMs
    )

  private def normalizePrefix(prefix: Vector[String]): Vector[String] =
    prefix.map(_.trim.toLowerCase)

enum CandidateProbeCacheWriteSource(val proofCacheAllowed: Boolean):
  case EngineProbe extends CandidateProbeCacheWriteSource(true)
  case RevalidatedCache extends CandidateProbeCacheWriteSource(true)
  case SourceContext extends CandidateProbeCacheWriteSource(false)
  case Retrieval extends CandidateProbeCacheWriteSource(false)
  case SourceRow extends CandidateProbeCacheWriteSource(false)

object CandidateProbeCacheWritePolicy:
  def canPopulateProofCache(source: CandidateProbeCacheWriteSource): Boolean =
    source.proofCacheAllowed

enum CandidateProbeCacheRejectReason(val key: String):
  case SourceCannotPopulateProofCache extends CandidateProbeCacheRejectReason("source_cannot_populate_proof_cache")
  case FenMismatch extends CandidateProbeCacheRejectReason("fen_mismatch")
  case VariantMismatch extends CandidateProbeCacheRejectReason("variant_mismatch")
  case BindingMismatch extends CandidateProbeCacheRejectReason("binding_mismatch")
  case EngineFingerprintMismatch extends CandidateProbeCacheRejectReason("engine_fingerprint_mismatch")
  case DepthMismatch extends CandidateProbeCacheRejectReason("depth_mismatch")
  case InsufficientDepth extends CandidateProbeCacheRejectReason("insufficient_depth")
  case MultiPvMismatch extends CandidateProbeCacheRejectReason("multipv_mismatch")
  case RoleMismatch extends CandidateProbeCacheRejectReason("role_mismatch")
  case ParentPrefixMismatch extends CandidateProbeCacheRejectReason("parent_prefix_mismatch")
  case Stale extends CandidateProbeCacheRejectReason("stale")

final case class CandidateProbeCacheValidation(reasons: Vector[CandidateProbeCacheRejectReason]):
  def accepted: Boolean = reasons.isEmpty

final case class CandidateProbeCacheEntry(
    key: CandidateProbeCacheKey,
    writeSource: CandidateProbeCacheWriteSource
):
  def revalidateFor(request: CandidateProbeRequest, nowEpochMs: Long): CandidateProbeCacheValidation =
    val expected = CandidateProbeCacheKey.fromRequest(
      request = request,
      realizedDepth = key.realizedDepth,
      generatedAtEpochMs = key.generatedAtEpochMs,
      maxAgeMs = key.maxAgeMs
    )
    val timing = CandidateLineTiming(key.generatedAtEpochMs, key.maxAgeMs)
    CandidateProbeCacheValidation(
      Vector(
        Option.when(!CandidateProbeCacheWritePolicy.canPopulateProofCache(writeSource))(
          CandidateProbeCacheRejectReason.SourceCannotPopulateProofCache
        ),
        Option.when(key.normalizedFen != expected.normalizedFen)(CandidateProbeCacheRejectReason.FenMismatch),
        Option.when(key.variant != expected.variant)(CandidateProbeCacheRejectReason.VariantMismatch),
        Option.when(key.nodeId != expected.nodeId || key.ply != expected.ply)(CandidateProbeCacheRejectReason.BindingMismatch),
        Option.when(key.engineFingerprint != expected.engineFingerprint)(
          CandidateProbeCacheRejectReason.EngineFingerprintMismatch
        ),
        Option.when(key.targetDepth != expected.targetDepth || key.floorDepth != expected.floorDepth)(
          CandidateProbeCacheRejectReason.DepthMismatch
        ),
        Option.when(key.realizedDepth < request.floorDepth)(CandidateProbeCacheRejectReason.InsufficientDepth),
        Option.when(key.multiPv != expected.multiPv)(CandidateProbeCacheRejectReason.MultiPvMismatch),
        Option.when(key.role != expected.role)(CandidateProbeCacheRejectReason.RoleMismatch),
        Option.when(
          key.parentBranchId != expected.parentBranchId ||
            key.parentLinePrefix != expected.parentLinePrefix ||
            key.parentRootRank != expected.parentRootRank ||
            key.normalizedUciFirstMove != expected.normalizedUciFirstMove
        )(CandidateProbeCacheRejectReason.ParentPrefixMismatch),
        Option.when(!timing.freshAt(nowEpochMs))(CandidateProbeCacheRejectReason.Stale)
      ).flatten.distinct
    )

final case class CandidateProbeAssemblyResult(
    evidence: Vector[CandidateLineEvidence],
    rootBindingValid: Boolean
):
  def mainCandidateHasDefenderResource(
      nowEpochMs: Long,
      depthFloor: Int = CandidateLineEvidence.DefaultDepthFloor
  ): Boolean =
    val mainRoot =
      evidence.find(line =>
        line.role == CandidateBranchRole.RootCandidate &&
          line.parentBranchId.isEmpty &&
          line.rank == 1 &&
          line.publicLineEligibleAt(nowEpochMs, depthFloor)
      )
    mainRoot.exists: root =>
      evidence.exists(line =>
        line.parentBranchId.contains(root.branchId) &&
          line.role == CandidateBranchRole.DefenderResource &&
          line.publicLineEligibleAt(nowEpochMs, depthFloor)
      )

object CandidateProbeAssemblyResult:
  def fromPackets(
      currentFen: String,
      nodeId: String,
      ply: Int,
      root: CandidateLinePacket,
      children: Vector[CandidateLineChildPacket],
      nowEpochMs: Long
  ): CandidateProbeAssemblyResult =
    val evidence =
      CandidateLinePacketHandoff.normalize(
        CandidateLinePacketHandoffInput(
          currentFen = currentFen,
          nodeId = nodeId,
          ply = ply,
          root = root,
          children = children
        ),
        nowEpochMs = nowEpochMs
      )
    CandidateProbeAssemblyResult(
      evidence = evidence,
      rootBindingValid = evidence.exists(line =>
        line.role == CandidateBranchRole.RootCandidate &&
          line.parentBranchId.isEmpty &&
          line.publicLineEligibleAt(nowEpochMs)
      )
    )
