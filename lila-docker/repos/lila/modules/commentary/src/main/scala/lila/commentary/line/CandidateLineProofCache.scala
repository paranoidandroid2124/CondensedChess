package lila.commentary.line

import scala.collection.mutable

trait CandidateLineProofCache:

  def read(request: CandidateProbeRequest, nowEpochMs: Long): Option[CandidateProbeCacheLookupResult]

  def read(
      key: CandidateProbeCacheKey,
      request: CandidateProbeRequest,
      nowEpochMs: Long
  ): Option[CandidateProbeCacheLookupResult]

  def commit(
      receipt: CandidateProbeControlledAdapter.CacheWriteReceipt,
      nowEpochMs: Long
  ): Vector[CandidateLineProofCache.WriteResult]

object CandidateLineProofCache:

  enum WriteRejectReason(val key: String):
    case NonProbeWriteSource extends WriteRejectReason("non_probe_write_source")
    case IncompletePayload extends WriteRejectReason("incomplete_payload")
    case KeyPayloadMismatch extends WriteRejectReason("key_payload_mismatch")
    case EntryValidationRejected extends WriteRejectReason("entry_validation_rejected")

  final case class WriteResult(stored: Boolean, reasons: Vector[WriteRejectReason])

  object InMemory:
    def empty: InMemory = new InMemory

  final class InMemory private () extends CandidateLineProofCache:
    private val stored =
      mutable.LinkedHashMap.empty[CandidateProbeCacheKey, CandidateProbeCacheWriteCandidate]

    override def read(
        request: CandidateProbeRequest,
        nowEpochMs: Long
    ): Option[CandidateProbeCacheLookupResult] =
      stored.values.toVector
        .flatMap(candidate => lookup(candidate, request, nowEpochMs))
        .sortBy(hit =>
          (
            -hit.entry.key.realizedDepth,
            -hit.entry.key.generatedAtEpochMs,
            stableKey(hit.entry.key)
          )
        )
        .headOption

    override def read(
        key: CandidateProbeCacheKey,
        request: CandidateProbeRequest,
        nowEpochMs: Long
    ): Option[CandidateProbeCacheLookupResult] =
      stored.get(key).flatMap(candidate => lookup(candidate, request, nowEpochMs))

    override def commit(
        receipt: CandidateProbeControlledAdapter.CacheWriteReceipt,
        nowEpochMs: Long
    ): Vector[WriteResult] =
      receipt.candidates.map(writeCandidate(_, nowEpochMs))

    private def writeCandidate(
        candidate: CandidateProbeCacheWriteCandidate,
        nowEpochMs: Long
    ): WriteResult =
      val reasons = writeRejectReasons(candidate, nowEpochMs)
      if reasons.isEmpty then
        stored.update(candidate.key, candidate)
        WriteResult(stored = true, Vector.empty)
      else WriteResult(stored = false, reasons)

    private def lookup(
        candidate: CandidateProbeCacheWriteCandidate,
        request: CandidateProbeRequest,
        nowEpochMs: Long
    ): Option[CandidateProbeCacheLookupResult] =
      val entry = CandidateProbeCacheEntry(candidate.key, candidate.writeSource)
      Option.when(
        candidate.payload.completed &&
          candidate.payload.request == request &&
          keyMatchesPayload(candidate) &&
          entry.revalidateFor(request, nowEpochMs).accepted
      ):
        CandidateProbeCacheLookupResult(
          request = request,
          entry = entry,
          payload = candidate.payload
        )

    private def writeRejectReasons(
        candidate: CandidateProbeCacheWriteCandidate,
        nowEpochMs: Long
    ): Vector[WriteRejectReason] =
      val entry = CandidateProbeCacheEntry(candidate.key, candidate.writeSource)
      Vector(
        Option.when(candidate.writeSource != CandidateProbeCacheWriteSource.EngineProbe)(
          WriteRejectReason.NonProbeWriteSource
        ),
        Option.when(!candidate.payload.completed)(WriteRejectReason.IncompletePayload),
        Option.when(!keyMatchesPayload(candidate))(WriteRejectReason.KeyPayloadMismatch),
        Option.when(!entry.revalidateFor(candidate.payload.request, nowEpochMs).accepted)(
          WriteRejectReason.EntryValidationRejected
        )
      ).flatten.distinct

    private def keyMatchesPayload(candidate: CandidateProbeCacheWriteCandidate): Boolean =
      candidate.key == CandidateProbeCacheKey.fromRequest(
        request = candidate.payload.request,
        realizedDepth = candidate.payload.realizedDepth,
        generatedAtEpochMs = candidate.payload.generatedAtEpochMs,
        maxAgeMs = candidate.payload.maxAgeMs
      )

    private def stableKey(key: CandidateProbeCacheKey): String =
      Vector(
        key.normalizedFen,
        key.variant,
        key.nodeId,
        key.ply.toString,
        key.engineFingerprint,
        key.targetDepth.toString,
        key.floorDepth.toString,
        key.realizedDepth.toString,
        key.multiPv.toString,
        key.role.key,
        key.parentBranchId.map(_.value).getOrElse(""),
        key.parentLinePrefix.mkString(","),
        key.parentRootRank.map(_.toString).getOrElse(""),
        key.normalizedUciFirstMove.getOrElse(""),
        key.generatedAtEpochMs.toString,
        key.maxAgeMs.toString
      ).mkString("|")
