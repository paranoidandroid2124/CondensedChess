package lila.commentary.line

import chess.Ply
import chess.format.{ Fen, Uci }
import chess.variant

import scala.util.control.NonFatal

final case class CandidateLinePacket(
    startFen: String,
    nodeId: String,
    ply: Int,
    rawLines: Vector[CandidateLineRawLine],
    requestedDepth: Int,
    realizedDepth: Int,
    multiPv: Int,
    generatedAtEpochMs: Long,
    maxAgeMs: Long,
    engineConfigFingerprint: String,
    requireDistinctRootFirstMoves: Boolean = true
)

final case class CandidateLineChildPacket(
    parentBranchId: CandidateBranchId,
    startFen: String,
    nodeId: String,
    ply: Int,
    role: CandidateBranchRole,
    parentLinePrefix: Vector[String],
    rawLines: Vector[CandidateLineRawLine],
    requestedDepth: Int,
    realizedDepth: Int,
    multiPv: Int,
    generatedAtEpochMs: Long,
    maxAgeMs: Long,
    engineConfigFingerprint: String,
    requireDistinctRootFirstMoves: Boolean = true
)

final case class CandidateLinePacketHandoffInput(
    currentFen: String,
    nodeId: String,
    ply: Int,
    root: CandidateLinePacket,
    children: Vector[CandidateLineChildPacket] = Vector.empty
)

object CandidateLinePacketHandoff:

  def normalize(input: CandidateLinePacketHandoffInput, nowEpochMs: Long): Vector[CandidateLineEvidence] =
    if !rootPacketBound(input) then Vector.empty
    else
      val rootEvidence = normalizeRoot(input.root, nowEpochMs)
      if rootEvidence.isEmpty || rootEvidence.exists(!_.publicLineEligibleAt(nowEpochMs)) || duplicateBranchIds(rootEvidence) then Vector.empty
      else
        val rootByBranchId = rootEvidence.map(line => line.branchId -> line).toMap
        rootEvidence ++ input.children.flatMap(child => normalizeChild(input, child, rootByBranchId, nowEpochMs))

  private def normalizeRoot(root: CandidateLinePacket, nowEpochMs: Long): Vector[CandidateLineEvidence] =
    CandidateLineNormalizer.normalize(
      CandidateLineNormalizationInput(
        startFen = root.startFen,
        startNodeId = root.nodeId,
        startPly = root.ply,
        rawLines = sanitizeRootLines(root.rawLines),
        requestedDepth = root.requestedDepth,
        realizedDepth = root.realizedDepth,
        multiPv = root.multiPv,
        generatedAtEpochMs = root.generatedAtEpochMs,
        maxAgeMs = root.maxAgeMs,
        engineConfigFingerprint = root.engineConfigFingerprint,
        requireDistinctRootFirstMoves = root.requireDistinctRootFirstMoves
      ),
      nowEpochMs = nowEpochMs
    )

  private def normalizeChild(
      input: CandidateLinePacketHandoffInput,
      child: CandidateLineChildPacket,
      rootByBranchId: Map[CandidateBranchId, CandidateLineEvidence],
      nowEpochMs: Long
  ): Vector[CandidateLineEvidence] =
    val base = CandidateLineNormalizer.normalize(
      CandidateLineNormalizationInput(
        startFen = child.startFen,
        startNodeId = child.nodeId,
        startPly = child.ply,
        rawLines = sanitizeChildLines(child),
        requestedDepth = child.requestedDepth,
        realizedDepth = child.realizedDepth,
        multiPv = child.multiPv,
        generatedAtEpochMs = child.generatedAtEpochMs,
        maxAgeMs = child.maxAgeMs,
        engineConfigFingerprint = child.engineConfigFingerprint,
        requireDistinctRootFirstMoves = child.requireDistinctRootFirstMoves
      ),
      nowEpochMs = nowEpochMs
    )
    val parent = rootByBranchId.get(child.parentBranchId)
    val bindingFailures =
      Vector(
        Option.when(parent.isEmpty)(CandidateLineRejectionReason.MissingParentBranch),
        Option.when(!childRoleAllowed(child.role))(CandidateLineRejectionReason.InvalidChildRole),
        Option.when(child.nodeId != input.nodeId)(CandidateLineRejectionReason.InvalidNode),
        Option.when(parent.exists(parentLine => child.ply != input.ply + childPrefix(child, parentLine).size))(
          CandidateLineRejectionReason.StartPlyMismatch
        ),
        Option.when(parent.exists(parentLine => !childStartFenMatches(input, child, parentLine)))(
          CandidateLineRejectionReason.StartFenMismatch
        )
      ).flatten

    base.map(line => line.copy(failureReasons = (line.failureReasons ++ bindingFailures).distinct))

  private def rootPacketBound(input: CandidateLinePacketHandoffInput): Boolean =
    cleanFen(input.root.startFen).contains(cleanFen(input.currentFen).getOrElse("")) &&
      input.root.nodeId == input.nodeId &&
      input.root.ply == input.ply

  private def childStartFenMatches(
      input: CandidateLinePacketHandoffInput,
      child: CandidateLineChildPacket,
      parent: CandidateLineEvidence
  ): Boolean =
    val prefix = childPrefix(child, parent)
    parent.lineUci.startsWith(prefix) &&
      fenAfterPrefix(input.currentFen, input.ply, prefix).exists: reached =>
        cleanFen(child.startFen).contains(reached)

  private def childPrefix(child: CandidateLineChildPacket, parent: CandidateLineEvidence): Vector[String] =
    if child.parentLinePrefix.nonEmpty then child.parentLinePrefix.map(_.trim.toLowerCase)
    else parent.lineUci

  private def sanitizeRootLines(rawLines: Vector[CandidateLineRawLine]): Vector[CandidateLineRawLine] =
    rawLines.map: raw =>
      raw.copy(
        parentBranchId = None,
        role = CandidateBranchRole.RootCandidate,
        provenance = sanitizeProvenance(raw.provenance, CandidateLineProvenanceKind.EngineRoot)
      )

  private def sanitizeChildLines(child: CandidateLineChildPacket): Vector[CandidateLineRawLine] =
    child.rawLines.map: raw =>
      raw.copy(
        parentBranchId = Some(child.parentBranchId),
        role = child.role,
        provenance = sanitizeProvenance(raw.provenance, CandidateLineProvenanceKind.EngineChild)
      )

  private def childRoleAllowed(role: CandidateBranchRole): Boolean =
    role == CandidateBranchRole.DefenderResource || role == CandidateBranchRole.DefenderReply

  private def duplicateBranchIds(lines: Vector[CandidateLineEvidence]): Boolean =
    lines.map(_.branchId).distinct.size != lines.size

  private def sanitizeProvenance(
      provenance: CandidateLineProvenance,
      engineKind: CandidateLineProvenanceKind
  ): CandidateLineProvenance =
    val kind =
      provenance.kind match
        case CandidateLineProvenanceKind.SourceHint => CandidateLineProvenanceKind.SourceHint
        case CandidateLineProvenanceKind.Cache      => CandidateLineProvenanceKind.Cache
        case _                                      => engineKind
    CandidateLineProvenance(kind, provenance.sourceHintRefs)

  private def fenAfterPrefix(startFen: String, startPly: Int, prefix: Vector[String]): Option[String] =
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

  private def cleanFen(fen: String): Option[String] =
    safe((Fen.Full.clean(fen): Fen.Full).value)

  private def safe[A](body: => A): Option[A] =
    try Some(body)
    catch case NonFatal(_) => None
